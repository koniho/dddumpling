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
    private final CanvasPainter painter;
    private final SettingsUi settingsUi = new SettingsUi();

    private float padL, padT, padR, padB;
    private long last;

    GameView(Context ctx, GameCore.Store store, GameCore.Sound sound) {
        super(ctx);
        painter = new CanvasPainter(loadFace(ctx));
        core = new GameCore(store, SystemClock.elapsedRealtimeNanos());
        core.sound = sound;
        // Has to be after the sound is attached, and before the Activity resumes: the loaded
        // choice is otherwise never announced and the backend picks its own fallback.
        core.startMusic();
        setKeepScreenOn(true);
        setClickable(true);
    }

    /**
     * The bundled rounded face, or null to let {@link CanvasPainter} fall back to the
     * platform sans-serif. A missing or unreadable font must never stop the game starting.
     */
    private static android.graphics.Typeface loadFace(Context ctx) {
        try {
            return android.graphics.Typeface.createFromAsset(ctx.getAssets(),
                    "fonts/Quicksand.ttf");
        } catch (Throwable e) {
            return null;
        }
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

        // The settings panel needs drags, for the speed slider.
        if (core.settingsOpen) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                int i = ev.getActionIndex();
                handleSettings(ev.getX(i), ev.getY(i), action == MotionEvent.ACTION_MOVE);
            }
            return true;
        }

        // FLING: grab a letter and throw it. Handled before the key routing so a drag that
        // starts on a letter is never mistaken for a key press.
        if (core.flinging() && handleFling(ev, action)) return true;

        // Panic swipe: an upward drag out of the strip between the danger line and the deck.
        // Needs MOVE events, so it is handled before the down-only filter. After the blade,
        // because during a FLING frenzy a stroke through that strip is a cut and should stay one.
        if (handlePush(ev, action)) return true;

        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) {
            return true;
        }
        // Both thumbs are first-class: pointer-down counts, not just the primary pointer.
        int idx = ev.getActionIndex();
        float x = ev.getX(idx), y = ev.getY(idx);

        if (core.state == GameCore.BONUS) {
            // Mash any key to hammer the steamer open.
            int mash = layout.keyAt(x, y);
            if (mash >= 0) {
                core.tapBonus(mash);
                tick();
            }
            return true;
        }

        if (core.state != GameCore.PLAY) {
            // A story on screen is modal: any touch anywhere dismisses it and nothing else
            // acts on that touch.
            if (core.storyOpen()) {
                core.closeStory();
                tick();
                return true;
            }
            // Otherwise the keys act — the inner four start, the outer two work the display
            // case — and a tap on the focused entry opens its story. A tap anywhere else does
            // nothing, which is what stops a stray touch skipping past the collection.
            int screen = layout.keyAt(x, y);
            if (screen >= 0) {
                core.screenKey(screen);
                tick();
            } else if (core.state == GameCore.TITLE && Showcase.inFocus(layout, x, y)) {
                core.openStory();
                tick();
            }
            return true;
        }
        // The stage readout opens settings, so check it before the keys.
        if (layout.inStageTap(x, y)) {
            core.openSettings();
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

    private boolean pushArmed;
    private float pushStartY;

    /**
     * The push-back gesture: start in the strip below the danger line and above the keys, then
     * drag up.
     *
     * That strip is the target on purpose. Starting anywhere lower would mean a swipe beginning
     * on a key, and telling a swipe from a tap needs the tap held back until the drag is ruled
     * out — latency this game cannot spend, since every press is a keystroke.
     */
    private boolean handlePush(MotionEvent ev, int action) {
        int i = ev.getActionIndex();
        float y = ev.getY(i);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (core.state != GameCore.PLAY || y < layout.dangerY || y > layout.deckTop) {
                return false;
            }
            pushArmed = true;
            pushStartY = y;
            return true;
        }
        if (!pushArmed) return false;

        if (action == MotionEvent.ACTION_MOVE) {
            // A clear upward flick, not a twitch.
            if (pushStartY - y >= layout.enemyR * 1.6f) {
                if (core.pushBack(layout)) tick();
                pushArmed = false;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_POINTER_UP) {
            pushArmed = false;
        }
        return true;
    }

    /**
     * FLING is a blade: a stroke cuts every letter it sweeps past. Returns true when the event
     * belonged to the gesture, so the caller leaves it alone.
     *
     * This used to grab a letter and drag it, which is why the mode felt weak — one letter per
     * gesture, and only if the gesture happened to start on one.
     */
    private boolean handleFling(MotionEvent ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (y > layout.deckTop) return false;          // that is the key deck
            core.beginStroke(x, y);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            // Every sample in the batch, not just the newest. A fast swipe arrives as one event
            // carrying several positions, and slicing only the last one leaves gaps in the cut.
            for (int h = 0; h < ev.getHistorySize(); h++) {
                if (core.sliceTo(ev.getHistoricalX(i, h), ev.getHistoricalY(i, h), layout) > 0) {
                    tick();
                }
            }
            if (core.sliceTo(x, y, layout) > 0) tick();
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_POINTER_UP) {
            boolean had = core.fingerDown;
            core.endStroke();
            return had;
        }
        return core.fingerDown;
    }

    private void handleSettings(float x, float y, boolean dragging) {
        settingsUi.compute(layout, Music.NAMES.length);
        int hit = settingsUi.hit(x, y);
        if (hit == SettingsUi.HIT_SLIDER) {
            float v = settingsUi.speedAt(x);
            if (v != core.speed) {
                core.setSpeed(v);
                tick();
            }
            return;
        }
        // A drag that wandered off the slider must not trip the other controls.
        if (dragging) return;

        // Any other tap in the panel stands the clear button back down, so an armed erase
        // cannot sit waiting through a music change for a second tap that meant something else.
        if (hit != SettingsUi.HIT_CLEAR) core.clearArmed = false;

        if (hit == SettingsUi.HIT_CLOSE || hit == SettingsUi.HIT_OUTSIDE) {
            core.closeSettings();
            tick();
        } else if (hit == SettingsUi.HIT_CLEAR) {
            core.tapClearCase();
            tick();
        } else if (hit >= SettingsUi.HIT_TEST) {
            // Closes the panel and drops straight into the mode.
            core.playtestMode(hit - SettingsUi.HIT_TEST, layout);
            tick();
        } else if (hit >= SettingsUi.HIT_OPTION) {
            core.setBgm(hit - SettingsUi.HIT_OPTION);
            tick();
        }
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
