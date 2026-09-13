package com.dddumpling.game;

import android.app.Activity;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.util.EnumMap;
import java.util.Map;

/** Firebase remains uninitialized until the player has allowed analytics. */
final class FirebaseAnalyticsBridge implements Analytics.Sink {
    private final Activity activity;
    private FirebaseAnalytics analytics;

    static boolean available() { return true; }
    FirebaseAnalyticsBridge(Activity activity) { this.activity = activity; }

    void enable() {
        try {
            if (analytics == null) {
                FirebaseApp app = FirebaseApp.initializeApp(activity);
                if (app == null) return;
                analytics = FirebaseAnalytics.getInstance(activity);
            }
            consent(true);
            analytics.setAnalyticsCollectionEnabled(true);
        } catch (RuntimeException ignored) { }
    }

    void disable(boolean reset) {
        if (analytics == null) return;
        try {
            consent(false);
            analytics.setAnalyticsCollectionEnabled(false);
            if (reset) analytics.resetAnalyticsData();
        } catch (RuntimeException ignored) { }
    }

    private void consent(boolean allowed) {
        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> values =
                new EnumMap<>(FirebaseAnalytics.ConsentType.class);
        values.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE,
                allowed ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_STORAGE, FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, FirebaseAnalytics.ConsentStatus.DENIED);
        analytics.setConsent(values);
    }

    @Override public void event(String name, int amount) {
        if (analytics == null || amount <= 0 || name == null || name.length() > 40
                || !name.matches("[A-Za-z][A-Za-z0-9_]*")) return;
        try {
            Bundle values = new Bundle();
            values.putLong("amount", amount);
            analytics.logEvent(name, values);
        } catch (RuntimeException ignored) { }
    }
}
