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

        if (core.state == GameCore.BONUS && core.starBonus) {
            if (handleStarDrag(ev, action)) return true;
            if (action == MotionEvent.ACTION_CANCEL) {
                for (int g = 0; g < Glyph.COUNT; g++) core.holdBonusKey(g, false);
            } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                int i = ev.getActionIndex();
                int key = layout.keyAt(ev.getX(i), ev.getY(i));
                if (key >= 0) core.holdBonusKey(key,
                        action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN);
            }
            return true;
        }

        if (handleBonusSwipe(ev, action)) return true;

        // The settings panel needs drags, for the speed slider.
        if (core.settingsOpen) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                int i = ev.getActionIndex();
                handleSettings(ev.getX(i), ev.getY(i), action == MotionEvent.ACTION_MOVE);
            }
            return true;
        }

        // The display case browses by touch. Needs MOVE events, so it comes before the down-only
        // filter, and it holds on to a gesture that outlives the case being closed.
        if (core.state == GameCore.TITLE && (core.caseOpen || caseGesture != CASE_IDLE)
                && handleCase(ev, action)) {
            return true;
        }

        // FLING: grab a letter and throw it. Handled before the key routing so a drag that
        // starts on a letter is never mistaken for a key press.
        if (core.flinging() && handleFling(ev, action)) return true;

        // Panic swipe: an upward drag starting anywhere in the lower half of the field.
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
            // Otherwise the keys act, and on the title a tap on the badge opens the display
            // case. A tap anywhere else does nothing.
            int screen = layout.keyAt(x, y);
            if (screen >= 0) {
                core.screenKey(screen);
                tick();
            } else if (core.state == GameCore.TITLE && !core.caseOpen
                    && Showcase.inIcon(layout, core.clock, x, y)) {
                core.openCase();
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

    private int starDragPointer = -1;
    private float starDragOffsetX;

    /** Grab the Starpath flyer and move it directly left or right. */
    private boolean handleStarDrag(MotionEvent ev, int action) {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int i = ev.getActionIndex();
            float dx = ev.getX(i) - core.stars.x;
            float dy = ev.getY(i) - core.stars.characterY(layout);
            float grab = StarPath.flyerR(layout) * 1.45f;
            if ((core.stars.ready() || core.stars.flying())
                    && dx * dx + dy * dy <= grab * grab) {
                starDragPointer = ev.getPointerId(i);
                core.stars.beginDrag();
                starDragOffsetX = core.stars.x - ev.getX(i);
                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE && starDragPointer >= 0) {
            int i = ev.findPointerIndex(starDragPointer);
            if (i >= 0) core.stars.dragTo(ev.getX(i) + starDragOffsetX, layout);
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // Let the caller also release every held arrow key.
            starDragPointer = -1;
            core.stars.endDrag();
        } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                && ev.getPointerId(ev.getActionIndex()) == starDragPointer) {
            starDragPointer = -1;
            core.stars.endDrag();
            return true;
        }
        return false;
    }

    private int bonusSwipePointer = -1;
    private float bonusSwipeStartY;

    /** Upward drag beginning on the armed steamer lid. */
    private boolean handleBonusSwipe(MotionEvent ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (!core.bonusSwipeReady() || !Screens.inSteamerLid(core, layout, x, y)) return false;
            bonusSwipePointer = ev.getPointerId(i);
            bonusSwipeStartY = y;
            return true;
        }
        if (bonusSwipePointer < 0) return false;
        if (action == MotionEvent.ACTION_MOVE) {
            i = ev.findPointerIndex(bonusSwipePointer);
            if (i < 0) return true;
            y = ev.getY(i);
            core.dragBonusLid(bonusSwipeStartY - y);
            if (Screens.steamerLidY(core, layout) <= Screens.steamerReleaseY(layout)) {
                core.swipeBonus();
                bonusSwipePointer = -1;
                tick();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_CANCEL) {
            if (action != MotionEvent.ACTION_CANCEL
                    && ev.getPointerId(ev.getActionIndex()) != bonusSwipePointer) return true;
            bonusSwipePointer = -1;
            core.dragBonusLid(0f);
            return true;
        }
        return true;
    }

    private static final int CASE_IDLE = 0, CASE_TAP = 1, CASE_SHELF = 2, CASE_BAR = 3;
    private int caseGesture;
    private float caseDownX, caseDownY;
    private int caseHit;

    /**
     * The display case: taps step the shelf, a sideways drag scrolls it, and the position bar
     * drags straight to an entry.
     *
     * Which one it is only becomes clear as the finger moves, so the tap is held until it lifts.
     * That is affordable here and nowhere else in this game: on the title screen a touch is
     * browsing, not a keystroke. Key taps are handed straight back to the caller instead of
     * being swallowed as "outside" — a key still lights up and still puts the case away, so the
     * deck behaves the same whether the case is up or not.
     */
    private boolean handleCase(MotionEvent ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            // A story is modal: the caller dismisses it and nothing else acts on that touch.
            if (core.storyOpen() || layout.keyAt(x, y) >= 0) return false;
            caseHit = Showcase.hit(layout, x, y);
            caseDownX = x;
            caseDownY = y;
            if (caseHit == Showcase.HIT_BAR) {
                caseGesture = CASE_BAR;
                core.caseTo(Showcase.barIndexAt(layout, x));
                tick();
            } else {
                caseGesture = CASE_TAP;
            }
            return true;
        }
        if (caseGesture == CASE_IDLE) return false;

        if (action == MotionEvent.ACTION_MOVE) {
            if (caseGesture == CASE_BAR) {
                // Tracked past the ends of the bar on purpose: a thumb sliding along a bar this
                // thin drifts off it, and stopping dead there feels like a fault.
                core.caseTo(Showcase.barIndexAt(layout, x));
                return true;
            }
            // A tap becomes a scroll once the finger has clearly gone sideways. Nothing on this
            // screen scrolls vertically, so a drag that is mostly up or down is not one.
            float dx = x - caseDownX;
            if (caseGesture == CASE_TAP && Math.abs(dx) > layout.unit * 0.7f
                    && Math.abs(dx) > Math.abs(y - caseDownY)) {
                caseGesture = CASE_SHELF;
                core.beginCaseDrag(caseDownX);
            }
            if (caseGesture == CASE_SHELF) core.caseDragTo(x, layout);
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_POINTER_UP) {
            if (caseGesture == CASE_TAP && action != MotionEvent.ACTION_CANCEL) tapCase(caseHit);
            core.endCaseDrag();
            caseGesture = CASE_IDLE;
        }
        return true;
    }

    /** A touch that lifted without becoming a drag. */
    private void tapCase(int hit) {
        if (hit == Showcase.HIT_FOCUS) {
            core.openStory();
        } else if (hit == Showcase.HIT_PREV) {
            core.scrollCase(-1);
        } else if (hit == Showcase.HIT_NEXT) {
            core.scrollCase(1);
        } else if (hit == Showcase.HIT_CLOSE || hit == Showcase.HIT_OUTSIDE) {
            core.closeCase();
        } else {
            return;
        }
        tick();
    }

    private boolean pushArmed;
    private float pushStartY;

    /**
     * The push-back gesture: start anywhere in the lower half of the field above the keys, then
     * drag up.
     *
     * The catchment is {@link Layout#inPushZone}, which is much wider than the strip the renderer
     * lights — see that method for why. What has not changed is the floor: it stops at
     * {@link Layout#deckTop}, because a swipe starting on a key would mean holding every tap back
     * until a drag is ruled out, and that is latency this game cannot spend when every press is a
     * keystroke.
     */
    private boolean handlePush(MotionEvent ev, int action) {
        int i = ev.getActionIndex();
        float y = ev.getY(i);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (core.state != GameCore.PLAY || !layout.inPushZone(ev.getX(i), y)) {
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
     *
     * One touch may hold several strokes: the core rests a stroke that stops moving and wakes a
     * new one on the next move, so there is nothing to send from here for that — every MOVE sample
     * already goes to {@link GameCore#sliceTo}, which is where both decisions are made. What this
     * tracks is only whether the finger is on the glass at all, hence {@code touchDown}.
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
            boolean had = core.touchDown;
            core.endStroke();
            return had;
        }
        return core.touchDown;
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
            int chip = hit - SettingsUi.HIT_TEST;
            if (chip == SettingsUi.TEST_STARS) core.playtestStars(layout);
            else if (chip == SettingsUi.TEST_STEAMER) core.playtestSteamer(layout);
            else core.playtestMode(chip, layout);
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
