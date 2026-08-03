package com.sram.hexatype;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * On-device crash surface.
 *
 * Termux can only read logcat for its own UID, so a crash in this app is completely
 * invisible from the shell that builds it — and there is no dumpsys or adb here either.
 * The only usable channel is the screen, so render the stack trace instead of dying.
 */
final class Crash {

    private static boolean shown;

    private Crash() {}

    /** Backstop for anything not already wrapped in a try/catch. */
    static void install(final Activity a) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                show(a, e);
                // Re-enter the message loop so the trace stays on screen instead of the
                // process being torn down.
                try {
                    if (Looper.myLooper() != null) Looper.loop();
                } catch (Throwable ignored) {
                    // Loop exited; nothing useful left to do.
                }
            }
        });
    }

    static void show(final Activity a, final Throwable t) {
        if (shown) return;
        shown = true;
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));

            TextView tv = new TextView(a);
            tv.setText("HEXATYPE CRASHED\n\n" + sw);
            tv.setTextColor(0xFFFFD9E2);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            tv.setTextIsSelectable(true);
            tv.setPadding(28, 110, 28, 40);

            ScrollView sv = new ScrollView(a);
            sv.setBackgroundColor(0xFF1C0A13);
            sv.addView(tv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            a.setContentView(sv);
        } catch (Throwable ignored) {
            // The reporter must never become the crash.
        }
    }
}
