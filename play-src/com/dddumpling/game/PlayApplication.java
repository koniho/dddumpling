package com.dddumpling.game;

import android.app.Application;
import com.google.android.gms.games.PlayGamesSdk;

/** Included only in an explicitly configured Play Games build. */
public final class PlayApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (BuildFlags.PLAY_GAMES) {
            try { PlayGamesSdk.initialize(this); }
            catch (RuntimeException e) { android.util.Log.w("DDDUMPLING", "Play Games initialization unavailable"); }
        }
    }
}
