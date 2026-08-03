package com.sram.hexatype;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

/** Input, insets and the frame loop. All drawing is delegated to {@link Renderer}. */
public class GameView extends View {

    private final GameCore core;
    private final Layout layout = new Layout();
    private final CanvasPainter painter = new CanvasPainter();

    private float padL, padT, padR, padB;
    private long last;

    GameView(Context ctx, GameCore.Store store) {
        super(ctx);
        core = new GameCore(store, SystemClock.elapsedRealtimeNanos());
        setKeepScreenOn(true);
        setClickable(true);
    }

    private void relayout() {
        if (getWidth() > 0 && getHeight() > 0) {
            layout.compute(getWidth(), getHeight(), padL, padT, padR, padB);
        }
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        relayout();
    }

    @Override public WindowInsets onApplyWindowInsets(WindowInsets in) {
        float[] p = Build.VERSION.SDK_INT >= 30 ? modernInsets(in) : legacyInsets(in);
        padL = p[0]; padT = p[1]; padR = p[2]; padB = p[3];
        relayout();
        return super.onApplyWindowInsets(in);
    }

    // Isolated so the API-30 types are never resolved on older devices.
    private static float[] modernInsets(WindowInsets in) {
        android.graphics.Insets i = in.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        return new float[] {i.left, i.top, i.right, i.bottom};
    }

    @SuppressWarnings("deprecation")
    private static float[] legacyInsets(WindowInsets in) {
        return new float[] {in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(),
                in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom()};
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = 0;
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) {
            return true;
        }
        // Both thumbs are first-class: pointer-down counts, not just the primary pointer.
        int idx = ev.getActionIndex();
        float x = ev.getX(idx), y = ev.getY(idx);

        if (core.state != GameCore.PLAY) {
            core.anyTap();
            tick();
            return true;
        }
        int key = layout.keyAt(x, y);
        if (key >= 0) {
            core.tapKey(key, layout);
            tick();
        }
        return true;
    }

    private void tick() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    @Override protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        float dt = last == 0 ? 1f / 60f : (now - last) / 1000f;
        last = now;
        if (dt > 0.05f) dt = 0.05f;   // a backgrounded app must not teleport the wave

        try {
            core.update(dt, layout);
            painter.bind(c);
            Renderer.draw(painter, core, layout);
        } catch (Throwable t) {
            // A throw from inside onDraw would otherwise kill the process with no trace.
            if (getContext() instanceof android.app.Activity) {
                Crash.show((android.app.Activity) getContext(), t);
            }
            return;
        }
        postInvalidateOnAnimation();
    }
}
