package com.dddumpling.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

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

    private SharedPreferences prefs;
    private Audio audio;
    private GameView game;
    private PlayBridge play;
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
            game = new GameView(this, this, audio);
            if (Build.VERSION.SDK_INT >= 33) backRegistration = new Api33Back(this, () -> game.back());
            game.navigationChanged(this::navigationChanged);
            setContentView(game);
            play = new PlayBridge(this, game.core());
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

    @Override public String loadReleaseSeen() { return prefs.getString("release_seen", ""); }
    @Override public void saveReleaseSeen(String value) { prefs.edit().putString("release_seen", value).apply(); }
    @Override public boolean loadPushLessonSeen() { return prefs.getBoolean("push_lesson_seen", false); }
    @Override public void savePushLessonSeen(boolean value) { prefs.edit().putBoolean("push_lesson_seen", value).apply(); }
    @Override public int loadCaveChoice() { return prefs.getInt("cave_dumpling", -1); }
    @Override public void saveCaveChoice(int value) { prefs.edit().putInt("cave_dumpling", value).apply(); }
    @Override public int loadLandState() { return prefs.getInt("land_state", 0); }
    @Override public void saveLandState(int value) { prefs.edit().putInt("land_state", value).apply(); }

    @Override public int loadLandBest(int land) {
        return land == 0 ? loadBest() : prefs.getInt("best_land_" + land, 0);
    }
    @Override public void saveLandBest(int land, int value) {
        if (land == 0) saveBest(value);
        else prefs.edit().putInt("best_land_" + land, value).apply();
    }

    @Override public int loadPlayerSettings() { return prefs.getInt("playerSettings", PlayerSettings.DEFAULT); }
    @Override public void savePlayerSettings(int value) { prefs.edit().putInt("playerSettings", value).apply(); }

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
