package com.dddumpling.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameCore.Store {

    private static final String PREFS = "hexatype";
    private static final String KEY_BEST = "best";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_BGM = "bgm";
    private static final String KEY_COLLECTED = "collected";
    private static final String KEY_COLLECT_TOTAL = "collectTotal";
    private static final String KEY_STEAMER_OPENS = "steamerOpens";
    private static final String KEY_STAR_WINS = "starWins";
    private static final String KEY_ROSTER = "roster";
    private static final String KEY_ANALYTICS_CONSENT = "analyticsConsent";
    private static final int ANALYTICS_UNSET = 0, ANALYTICS_ALLOWED = 1, ANALYTICS_DECLINED = 2;

    private SharedPreferences prefs;
    private Audio audio;
    private GameView game;
    private PlayBridge play;
    private FirebaseAnalyticsBridge analytics;
    private FrameLayout analyticsOverlay;
    private TextView analyticsDisclosure;
    private final Button[] analyticsButtons = new Button[5];
    private Button privacyButton;
    private BackRegistration backRegistration;
    private boolean resumed;
    private boolean musicPaused;

    private interface BackRegistration { void enabled(boolean enabled); void close(); }
    private static final class Api33Back implements BackRegistration {
        private final android.window.OnBackInvokedDispatcher dispatcher;
        private final android.window.OnBackInvokedCallback callback;
        private boolean registered;
        Api33Back(Activity activity, Runnable action) {
            dispatcher = activity.getOnBackInvokedDispatcher();
            callback = action::run;
        }
        public void enabled(boolean enabled) {
            if (registered == enabled) return;
            if (enabled) dispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            else dispatcher.unregisterOnBackInvokedCallback(callback);
            registered = enabled;
        }
        public void close() { enabled(false); }
    }
    private void navigationChanged() {
        if (game == null) return;
        updatePrivacyAccess();
        if (backRegistration != null) backRegistration.enabled(game.handlesBack());
        boolean pauseMusic = !resumed || game.paused();
        if (audio != null && pauseMusic != musicPaused) {
            musicPaused = pauseMusic;
            if (pauseMusic) audio.pauseMusic(); else audio.resumeMusic();
        }
    }
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (game == null || !game.back()) super.onBackPressed();
    }


    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Crash.install(this);
        try {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            audio = new Audio(this);
            // setContentView first: it installs the decor view, and
            // Window.getInsetsController() dereferences that decor view, so going
            // fullscreen any earlier throws inside the framework.
            game = new GameView(this, this, audio, this::showPrivacy);
            if (Build.VERSION.SDK_INT >= 33) backRegistration = new Api33Back(this, () -> game.back());
            game.navigationChanged(this::navigationChanged);
            FrameLayout content = new FrameLayout(this);
            content.addView(game, new FrameLayout.LayoutParams(-1, -1));
            installPrivacyAccess(content);
            setContentView(content);
            play = new PlayBridge(this, game.core());
            analytics = new FirebaseAnalyticsBridge(this);
            if (analytics.available()) {
                if (analyticsConsent() == ANALYTICS_ALLOWED) enableAnalytics();
                if (analyticsConsent() == ANALYTICS_UNSET
                        || state != null && state.getBoolean("analyticsPromptVisible"))
                    game.post(this::showPrivacy);
            }
            navigationChanged();
            goFullscreen();
        } catch (Throwable t) {
            Crash.show(this, t);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (play != null) play.resume();
        if (game != null) game.background(false);
        if (audio != null) {
            audio.startMusic();
            if (game == null || !game.paused()) audio.resumeMusic();
        }
    }

    @Override protected void onPause() {
        resumed = false;
        if (game != null) game.core().progress.checkpoint(game.core().score);
        if (play != null) play.pause();
        if (game != null) game.background(true);
        super.onPause();
        if (audio != null) {
            audio.pauseMusic();
            // A story left open stays open, but it stops being read to an empty room. It does
            // not pick up again on resume: half a sentence from nowhere is worse than silence.
            audio.hush();
        }
    }

    @Override protected void onDestroy() {
        if (play != null) play.close();
        if (backRegistration != null) backRegistration.close();
        super.onDestroy();
        if (audio != null) audio.release();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("analyticsPromptVisible", game != null && game.analyticsUi().visible());
        super.onSaveInstanceState(state);
    }

    private int analyticsConsent() { return prefs.getInt(KEY_ANALYTICS_CONSENT, ANALYTICS_UNSET); }
    private void enableAnalytics() {
        if (analytics == null || !analytics.available()) return;
        analytics.enable();
        game.core().progress.attachAnalytics(analytics);
    }
    private void disableAnalytics() {
        if (game != null) game.core().progress.attachAnalytics(null);
        if (analytics != null) analytics.disable(true);
    }
    private void chooseAnalytics(boolean allowed) {
        prefs.edit().putInt(KEY_ANALYTICS_CONSENT,
                allowed ? ANALYTICS_ALLOWED : ANALYTICS_DECLINED).apply();
        if (allowed) enableAnalytics(); else disableAnalytics();
    }
    private void showPrivacy() {
        if (analytics == null || !analytics.available()) { openPrivacyPolicy(); return; }
        if (game.analyticsUi().visible()) return;
        game.showAnalytics(analyticsConsent() == ANALYTICS_ALLOWED);
        AnalyticsUi ui = game.analyticsUi();
        analyticsDisclosure.setText(ui.title() + ". " + ui.message());
        for (int action = AnalyticsUi.ALLOW; action <= AnalyticsUi.CLOSE; action++)
            analyticsButtons[action].setText(action == AnalyticsUi.CLOSE
                    ? "Close analytics choices" : ui.actionTitle(action));
        game.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        analyticsOverlay.setVisibility(View.VISIBLE);
        layoutAnalyticsAccess();
        analyticsDisclosure.requestFocus();
        analyticsDisclosure.sendAccessibilityEvent(
                android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void analyticsAction(int action) {
        if (game == null || !game.analyticsUi().visible()) return;
        if (action == AnalyticsUi.POLICY) { openPrivacyPolicy(); return; }
        if (action == AnalyticsUi.ALLOW) chooseAnalytics(true);
        else if (action == AnalyticsUi.DECLINE
                || action == AnalyticsUi.CLOSE && !game.analyticsUi().enabled()) chooseAnalytics(false);
        else if (action != AnalyticsUi.CLOSE) return;
        analyticsOverlay.setVisibility(View.GONE);
        game.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        game.hideAnalytics();
        if (privacyButton.getVisibility() == View.VISIBLE) privacyButton.requestFocus();
    }

    /** Native semantic controls follow the shared painted controls for TalkBack and keyboard use. */
    private void installPrivacyAccess(FrameLayout content) {
        privacyButton = semanticButton("Privacy and analytics", this::showPrivacy);
        content.addView(privacyButton, new FrameLayout.LayoutParams(1, 1));
        analyticsOverlay = new FrameLayout(this);
        analyticsOverlay.setVisibility(View.GONE);
        analyticsOverlay.setOnTouchListener((view, event) -> true);
        analyticsDisclosure = new TextView(this);
        analyticsDisclosure.setTextColor(android.graphics.Color.TRANSPARENT);
        analyticsDisclosure.setFocusable(true);
        analyticsDisclosure.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        analyticsOverlay.addView(analyticsDisclosure, new FrameLayout.LayoutParams(1, 1));
        for (int action = AnalyticsUi.ALLOW; action <= AnalyticsUi.CLOSE; action++) {
            final int selected = action;
            Button button = semanticButton("", () -> analyticsAction(selected));
            analyticsButtons[action] = button;
            analyticsOverlay.addView(button, new FrameLayout.LayoutParams(1, 1));
        }
        content.addView(analyticsOverlay, new FrameLayout.LayoutParams(-1, -1));
        game.analyticsActions(this::analyticsAction, this::layoutAnalyticsAccess);
    }

    private Button semanticButton(String title, Runnable action) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(android.graphics.Color.TRANSPARENT);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0); button.setMinHeight(0);
        button.setMinimumWidth(0); button.setMinimumHeight(0);
        android.graphics.drawable.GradientDrawable focus = new android.graphics.drawable.GradientDrawable();
        focus.setColor(android.graphics.Color.TRANSPARENT);
        focus.setStroke(Math.max(2, (int) (2 * getResources().getDisplayMetrics().density)), 0xFFFFDB72);
        focus.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
        background.addState(new int[] {android.R.attr.state_focused}, focus);
        background.addState(new int[0], new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        button.setBackground(background);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private static void place(View view, float left, float top, float width, float height) {
        int x = Math.round(left), y = Math.round(top);
        int w = Math.max(1, Math.round(width)), h = Math.max(1, Math.round(height));
        FrameLayout.LayoutParams old = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (old.leftMargin == x && old.topMargin == y && old.width == w && old.height == h) return;
        FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(w, h);
        bounds.leftMargin = x; bounds.topMargin = y;
        view.setLayoutParams(bounds);
    }

    private void updatePrivacyAccess() {
        if (privacyButton == null) return;
        Layout layout = game.gameLayout();
        boolean visible = !game.analyticsUi().visible() && PrivacyUi.visible(game.core());
        privacyButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        place(privacyButton, layout.w - 7f * layout.unit, layout.dangerY - 3f * layout.unit,
                7f * layout.unit, 2f * layout.unit);
    }

    private void layoutAnalyticsAccess() {
        updatePrivacyAccess();
        if (analyticsOverlay == null || !game.analyticsUi().visible()) return;
        AnalyticsUi ui = game.analyticsUi();
        for (int action = AnalyticsUi.ALLOW; action <= AnalyticsUi.CLOSE; action++)
            place(analyticsButtons[action], ui.actionLeft(action), ui.actionTop(action),
                    ui.actionWidth(action), ui.actionHeight(action));
        float left = Math.min(ui.actionLeft(AnalyticsUi.ALLOW), ui.actionLeft(AnalyticsUi.DECLINE));
        float right = Math.max(ui.actionLeft(AnalyticsUi.ALLOW) + ui.actionWidth(AnalyticsUi.ALLOW),
                ui.actionLeft(AnalyticsUi.DECLINE) + ui.actionWidth(AnalyticsUi.DECLINE));
        place(analyticsDisclosure, left, 0, right - left,
                Math.min(ui.actionTop(AnalyticsUi.ALLOW), ui.actionTop(AnalyticsUi.DECLINE)));
    }
    private void openPrivacyPolicy() {
        try {
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(PrivacyUi.URL)));
        } catch (android.content.ActivityNotFoundException unavailable) {
            new android.app.AlertDialog.Builder(this).setTitle("Privacy policy")
                    .setMessage(PrivacyUi.URL + "\nSupport: dddumpling.play@gmail.com")
                    .setPositiveButton("OK", null).show();
        }
    }

    @Override public byte[] loadProgress() {
        String saved = prefs.getString("progress_v1", "");
        return android.util.Base64.decode(saved, android.util.Base64.NO_WRAP);
    }

    @Override public void saveProgress(byte[] data) {
        prefs.edit().putString("progress_v1", android.util.Base64.encodeToString(data,
                android.util.Base64.NO_WRAP)).apply();
    }

    @Override public String progressReplica() {
        // Backup restores must receive a new writer ID, otherwise offline increments collide.
        android.util.AtomicFile file = new android.util.AtomicFile(
                new java.io.File(getNoBackupFilesDir(), "progress-writer"));
        try (java.io.DataInputStream in = new java.io.DataInputStream(file.openRead())) {
            String id = in.readUTF();
            if (!id.matches("[a-zA-Z0-9_-]{1,64}")) throw new java.io.IOException("Invalid writer ID");
            return id;
        } catch (java.io.FileNotFoundException missing) {
            String id = java.util.UUID.randomUUID().toString();
            java.io.FileOutputStream out = null;
            try {
                out = file.startWrite();
                java.io.DataOutputStream data = new java.io.DataOutputStream(out);
                data.writeUTF(id); data.flush(); file.finishWrite(out);
                return id;
            } catch (java.io.IOException e) {
                if (out != null) file.failWrite(out);
                throw new IllegalStateException("Cannot save progress writer", e);
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read progress writer", e); }
    }

    String progressOwner() { return prefs.getString("progress_owner", ""); }
    boolean bindProgressOwner(String player) {
        String owner = progressOwner();
        if (!owner.isEmpty()) return owner.equals(player);
        return prefs.edit().putString("progress_owner", player).commit();
    }

    @Override public int loadBest() {
        return prefs.getInt(KEY_BEST, 0);
    }

    @Override public void saveBest(int best) {
        prefs.edit().putInt(KEY_BEST, best).apply();
    }

    @Override public int loadLandState() { return prefs.getInt("land_state", 0); }
    @Override public void saveLandState(int value) { prefs.edit().putInt("land_state", value).apply(); }

    @Override public int loadLandBest(int land) {
        return land == 0 ? loadBest() : prefs.getInt("best_land_" + land, 0);
    }
    @Override public void saveLandBest(int land, int value) {
        if (land == 0) saveBest(value);
        else prefs.edit().putInt("best_land_" + land, value).apply();
    }

    @Override public float loadSpeed() {
        return prefs.getFloat(KEY_SPEED, 1f);
    }

    @Override public void saveSpeed(float speed) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply();
    }

    @Override public int loadBgm() {
        return prefs.getInt(KEY_BGM, Music.defaultChoice(haveCustomTrack()));
    }

    /**
     * True when a personal track was dropped into {@code res/raw}. Resolved by name so the
     * build does not depend on the file existing — it is gitignored and usually absent.
     */
    private boolean haveCustomTrack() {
        return getResources().getIdentifier("bgm", "raw", getPackageName()) != 0;
    }

    @Override public void saveBgm(int choice) {
        prefs.edit().putInt(KEY_BGM, choice).apply();
    }

    @Override public long loadCollected() {
        return prefs.getLong(KEY_COLLECTED, 0L);
    }

    @Override public void saveCollected(long owned) {
        prefs.edit().putLong(KEY_COLLECTED, owned).apply();
    }

    @Override public int loadCollectTotal() {
        return prefs.getInt(KEY_COLLECT_TOTAL, 0);
    }

    @Override public int[] loadCollectionCounts() {
        int[] counts = new int[Collect.COUNT];
        for (int i = 0; i < counts.length; i++) counts[i] = prefs.getInt("collectedCount_" + i, 0);
        return counts;
    }

    @Override public void saveCollectionCounts(int[] counts) {
        SharedPreferences.Editor edit = prefs.edit();
        for (int i = 0; i < counts.length; i++) edit.putInt("collectedCount_" + i, counts[i]);
        edit.apply();
    }

    @Override public void saveCollectTotal(int total) {
        prefs.edit().putInt(KEY_COLLECT_TOTAL, total).apply();
    }

    @Override public int loadStarWins() {
        return prefs.getInt(KEY_STAR_WINS, 0);
    }

    @Override public void saveStarWins(int wins) {
        prefs.edit().putInt(KEY_STAR_WINS, wins).apply();
    }

    @Override public int loadSteamerOpens() {
        return prefs.getInt(KEY_STEAMER_OPENS, 0);
    }

    @Override public void saveSteamerOpens(int opens) {
        prefs.edit().putInt(KEY_STEAMER_OPENS, opens).apply();
    }

     public int loadRosterState() {
        if (prefs.contains(KEY_ROSTER)) return prefs.getInt(KEY_ROSTER, 0);
        // Existing players keep the deck they already learned; only a genuinely fresh save
        // begins with the four-key teaching roster.
        return prefs.contains(KEY_BEST) || prefs.contains(KEY_COLLECTED)
                || prefs.contains(KEY_COLLECT_TOTAL) ? 1 : 0;
    }

     public void saveRosterState(int state) {
        prefs.edit().putInt(KEY_ROSTER, state).apply();
    }

    private void goFullscreen() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 30) hideBars30(w);
        else hideBarsLegacy(w);
    }

    private static void hideBars30(Window w) {
        w.setDecorFitsSystemWindows(false);
        WindowInsetsController ic = w.getInsetsController();
        if (ic != null) {
            ic.hide(android.view.WindowInsets.Type.systemBars());
            ic.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @SuppressWarnings("deprecation")
    private static void hideBarsLegacy(Window w) {
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        // Immersive mode is dropped whenever the bars are swiped in; re-assert it.
        if (has) {
            try {
                goFullscreen();
            } catch (Throwable t) {
                Crash.show(this, t);
            }
        }
    }
}
