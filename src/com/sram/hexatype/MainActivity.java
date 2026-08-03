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

    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Crash.install(this);
        try {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            // setContentView first: it installs the decor view, and
            // Window.getInsetsController() dereferences that decor view, so going
            // fullscreen any earlier throws inside the framework.
            setContentView(new GameView(this, this));
            goFullscreen();
        } catch (Throwable t) {
            Crash.show(this, t);
        }
    }

    @Override public int loadBest() {
        return prefs.getInt(KEY_BEST, 0);
    }

    @Override public void saveBest(int best) {
        prefs.edit().putInt(KEY_BEST, best).apply();
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
