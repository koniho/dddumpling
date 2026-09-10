package com.sram.hexatype;

import android.app.Application;
import com.google.android.gms.games.PlayGamesSdk;

/** Included only in an explicitly configured production build. */
public final class PlayApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (!BuildFlags.DEVELOPER) {
            try { PlayGamesSdk.initialize(this); }
            catch (RuntimeException e) { android.util.Log.w("DDDUMPLING", "Play Games initialization unavailable"); }
        }
    }
}
