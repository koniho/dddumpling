package com.sram.hexatype;

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

    private SharedPreferences prefs;
    private Audio audio;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Crash.install(this);
        try {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            audio = new Audio(this);
            // setContentView first: it installs the decor view, and
            // Window.getInsetsController() dereferences that decor view, so going
            // fullscreen any earlier throws inside the framework.
            setContentView(new GameView(this, this, audio));
            goFullscreen();
        } catch (Throwable t) {
            Crash.show(this, t);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (audio != null) {
            audio.startMusic();
            audio.resumeMusic();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (audio != null) audio.pauseMusic();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (audio != null) audio.release();
    }

    @Override public int loadBest() {
        return prefs.getInt(KEY_BEST, 0);
    }

    @Override public void saveBest(int best) {
        prefs.edit().putInt(KEY_BEST, best).apply();
    }

    @Override public float loadSpeed() {
        return prefs.getFloat(KEY_SPEED, 1f);
    }

    @Override public void saveSpeed(float speed) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply();
    }

    @Override public int loadBgm() {
        // If a personal track was dropped into res/raw, default to it on first run rather
        // than making the player go and find the option.
        int fallback = getResources().getIdentifier("bgm", "raw", getPackageName()) != 0
                ? Music.CUSTOM : Music.SWING_STYLE;
        return prefs.getInt(KEY_BGM, fallback);
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
