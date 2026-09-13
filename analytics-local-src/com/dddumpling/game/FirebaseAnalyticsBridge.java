package com.dddumpling.game;

import android.app.Activity;

/** Compile-time no-op when Firebase is absent or this is a developer build. */
final class FirebaseAnalyticsBridge implements Analytics.Sink {
    static boolean available() { return false; }
    FirebaseAnalyticsBridge(Activity activity) {}
    void enable() {}
    void disable(boolean reset) {}
    @Override public void event(String name, int amount) {}
}
