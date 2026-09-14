package com.dddumpling.game;

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
    private boolean background;
    private int pausePress;
    private Runnable navigationChanged;
    void navigationChanged(Runnable listener) { navigationChanged = listener; }
    private void refreshNavigation() { if (navigationChanged != null) navigationChanged.run(); }
    boolean handlesBack() { return Pause.handlesBack(core); }
    boolean paused() { return core.paused; }
    private void cancelPointers() {
        starDragPointer = bonusSwipePointer = bossDragPointer = -1;
        bossDragging = bossPinching = pushArmed = false;
        caseGesture = CASE_IDLE; pausePress = 0;
        Pause.release(core);
    }
    boolean back() {
        if (!handlesBack()) return false;
        cancelPointers();
        boolean handled = Pause.back(core);
        last = 0; refreshNavigation(); invalidate();
        return handled;
    }
    void background(boolean hidden) {
        background = hidden; last = 0;
        if (hidden) { cancelPointers(); Pause.open(core); }
        refreshNavigation(); invalidate();
    }


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
     * The bundled Bungee face, or null to let {@link CanvasPainter} fall back to the
     * platform sans-serif. A missing or unreadable font must never stop the game starting.
     */
    private static android.graphics.Typeface loadFace(Context ctx) {
        try {
            return android.graphics.Typeface.createFromAsset(ctx.getAssets(),
                    "fonts/Bungee-Regular.ttf");
        } catch (Throwable e) {
            return null;
        }
    }

    GameCore core() { return core; }

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
        try { return touch(ev); }
        finally { refreshNavigation(); }
    }
    private boolean touch(MotionEvent ev) {
        if (background) return true;
        if (core.paused) {
            int action = ev.getActionMasked();
            int hit = Pause.hit(layout, ev.getX(), ev.getY());
            if (action == MotionEvent.ACTION_DOWN) pausePress = hit;
            else if (action == MotionEvent.ACTION_MOVE && hit != pausePress) pausePress = 0;
            else if (action == MotionEvent.ACTION_UP) {
                int selected = pausePress; pausePress = 0;
                if (selected != 0 && selected == hit) { Pause.action(core, hit); last = 0; tick(); }
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN)
                pausePress = 0;
            return true;
        }
        int action = ev.getActionMasked();
        if (BuildFlags.DEVELOPER && core.settingsOpen) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                int i = ev.getActionIndex();
                handleSettings(ev.getX(i), ev.getY(i), action == MotionEvent.ACTION_MOVE);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN && PrivacyUi.hit(core, layout, ev.getX(), ev.getY())) {
            try {
                getContext().startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(PrivacyUi.URL)));
            } catch (android.content.ActivityNotFoundException unavailable) {
                new android.app.AlertDialog.Builder(getContext()).setTitle("Privacy policy")
                        .setMessage(PrivacyUi.URL + "\nSupport: dddumpling.play@gmail.com")
                        .setPositiveButton("OK", null).show();
            }
            return true;
        }

        if (handleLandPicker(ev, action)) return true;

        if (core.state == GameCore.TITLE) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                int titlePointer = action == MotionEvent.ACTION_MOVE ? 0 : ev.getActionIndex();
                core.titleTouchDown = true;
                core.titleTouchX = ev.getX(titlePointer);
                core.titleTouchY = ev.getY(titlePointer);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                core.titleTouchDown = false;
            }
        }

        if (core.state == GameCore.BONUS && core.starBonus) {
            handleStarDrag(ev, action);
            return true;
        }

        if (handleBonusSwipe(ev, action)) return true;

        // The display case browses by touch. Needs MOVE events, so it comes before the down-only
        // filter, and it holds on to a gesture that outlives the case being closed.
        if (core.state == GameCore.TITLE && (core.caseOpen || caseGesture != CASE_IDLE)
                && handleCase(ev, action)) {
            return true;
        }

        // A visible powerup owns a direct down on its icon before field gestures.
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
                && core.tapPower(ev.getX(ev.getActionIndex()), ev.getY(ev.getActionIndex()), layout)) {
            tick();
            return true;
        }

        // The boss.s own elements: taps and drags on the things it puts on the field. Needs MOVE
        // events, so it comes before the down-only filter, and before both the blade and the panic
        // swipe — a finger that landed on a glob is carrying that glob, not slicing or shoving.
        if (handleBoss(ev, action)) return true;

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
            int mash = core.keyAt(x, y, layout);
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
            int screen = core.keyAt(x, y, layout);
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
        if (BuildFlags.DEVELOPER && layout.inStageTap(x, y)) {
            core.openSettings();
            tick();
            return true;
        }
        int key = core.keyAt(x, y, layout);
        if (key >= 0) {
            core.tapKey(key, layout);
            tick();
        }
        return true;
    }

    private int landPointer = -1;
    private boolean handleLandPicker(MotionEvent ev, int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            landPointer = -1;
            if (!LandPicker.down(core, layout, ev.getX(), ev.getY())) return false;
            if (core.landPickerDragging) landPointer = ev.getPointerId(0);
            tick(); return true;
        }
        if (landPointer < 0) return false;
        if (!LandPicker.visible(core) || action == MotionEvent.ACTION_CANCEL) {
            core.landPickerDragging = false; landPointer = -1; return true;
        }
        int index = ev.findPointerIndex(landPointer);
        if (index < 0) { core.landPickerDragging = false; landPointer = -1; return true; }
        int before = core.landChoice;
        if (action == MotionEvent.ACTION_MOVE) LandPicker.move(core, layout, ev.getX(index));
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                && ev.getPointerId(ev.getActionIndex()) == landPointer) {
            LandPicker.up(core, layout, ev.getX(index), ev.getY(index)); landPointer = -1;
        }
        if (before != core.landChoice) tick();
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
            boolean flyer = dx * dx + dy * dy <= grab * grab;
            boolean slider = StarScreen.inSlider(layout, ev.getX(i), ev.getY(i));
            if ((core.stars.ready() || core.stars.flying()) && (flyer || slider)) {
                starDragPointer = ev.getPointerId(i);
                core.stars.beginDrag();
                starDragOffsetX = flyer ? core.stars.x - ev.getX(i) : 0f;
                core.stars.dragTo(ev.getX(i) + starDragOffsetX, layout);
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
            if (Screens.steamerLidY(core, layout) <= Screens.steamerReleaseY(core, layout)) {
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

    private static final int CASE_IDLE = 0, CASE_TAP = 1, CASE_SHELF = 2;
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
            if (core.storyOpen() || core.keyAt(x, y, layout) >= 0) return false;
            caseHit = Showcase.hit(core, layout, x, y);
            caseDownX = x;
            caseDownY = y;
            caseGesture = CASE_TAP;
            return true;
        }
        if (caseGesture == CASE_IDLE) return false;

        if (action == MotionEvent.ACTION_MOVE) {
            // A drag can move freely on both axes.
            float dx = x - caseDownX;
            if (caseGesture == CASE_TAP && Math.max(Math.abs(dx), Math.abs(y - caseDownY)) > layout.unit * 0.7f) {
                caseGesture = CASE_SHELF;
                core.beginCaseDrag(caseDownX, caseDownY);
            }
            if (caseGesture == CASE_SHELF) {
                int before = core.caseIndex;
                core.caseDragTo(x, y, layout);
                if (core.caseIndex != before)
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
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
        if (hit >= Showcase.HIT_ENTRY) {
            CaseUi.select(core, hit - Showcase.HIT_ENTRY);
        } else if (hit == Showcase.HIT_FOCUS) {
            core.openStory();
        } else if (hit == Showcase.HIT_PREV) {
            core.scrollCase(-1);
        } else if (hit == Showcase.HIT_NEXT) {
            core.scrollCase(1);
        } else if (hit == Showcase.HIT_UP) {
            core.scrollCaseRow(-1);
        } else if (hit == Showcase.HIT_DOWN) {
            core.scrollCaseRow(1);
        } else if (hit == Showcase.HIT_CLOSE || hit == Showcase.HIT_OUTSIDE) {
            core.closeCase();
        } else {
            return;
        }
        tick();
    }

    /** True while a finger is carrying one of the boss's elements. */
    private boolean bossDragging;
    /** Android pointer that owns the drag; other fingers remain free to tap the key deck. */
    private int bossDragPointer = -1;
    private boolean bossPinching;
    private long lastBossDragHaptic;
    private boolean bossWasBeaten;

    /**
     * The boss's elements: a tap on one acts at once, a drag on one carries it.
     *
     * Which of the two it is needs no waiting, and that is the whole reason this can exist in play at
     * all. A drag may not start on a key, because telling a drag from a tap means holding the tap
     * back and every tap here is a keystroke — but a boss element is not a key and nothing else
     * claims a touch on one, so the element itself does the disambiguating. A touch that lands on a
     * draggable element is a drag from that frame; a touch on a tappable one is a tap on that frame.
     * The display case's position bar works the same way and for the same reason.
     *
     * Elements sit in the upper field, clear of the panic swipe's catchment in the lower half, so the
     * two gestures cannot be confused either.
     */
    private boolean handleBoss(MotionEvent ev, int action) {
        if (core.state != GameCore.PLAY) return false;
        // A second finger during a carry is not a new boss gesture. In particular, let a pointer
        // landing on the deck fall through to ordinary key routing without replacing the cap owner.
        if (bossDragging && action == MotionEvent.ACTION_POINTER_DOWN) return false;
        if (action == MotionEvent.ACTION_POINTER_DOWN && ev.getPointerCount() == 2) {
            float dx = ev.getX(0) - ev.getX(1), dy = ev.getY(0) - ev.getY(1);
            if (core.beginBossPinch((float) Math.sqrt(dx * dx + dy * dy),
                    ev.getX(0), ev.getY(0), ev.getX(1), ev.getY(1))) {
                bossPinching = true;
                bossDragging = false;
                bossDragPointer = -1;
                return true;
            }
        }
        if (bossPinching) {
            if (action == MotionEvent.ACTION_MOVE && ev.getPointerCount() >= 2) {
                float dx = ev.getX(0) - ev.getX(1), dy = ev.getY(0) - ev.getY(1);
                if (core.pinchBoss((float) Math.sqrt(dx * dx + dy * dy),
                        ev.getX(0), ev.getY(0), ev.getX(1), ev.getY(1), layout)) tick();
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                core.endBossPinch();
                bossPinching = false;
                return true;
            }
        }
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (y > layout.deckTop) return false;          // that is the key deck
            if (core.grabBoss(x, y)) {
                bossDragging = true;
                bossDragPointer = ev.getPointerId(i);
                tick();
                return true;
            }
            if (core.tapBoss(x, y, layout)) {
                tick();
                return true;
            }
            return false;
        }
        if (!bossDragging) return false;

        if (action == MotionEvent.ACTION_MOVE) {
            i = ev.findPointerIndex(bossDragPointer);
            if (i < 0) return true;
            x = ev.getX(i);
            y = ev.getY(i);
            // Every sample in the batch, so a quick carry off the edge is not missed between frames.
            for (int h = 0; h < ev.getHistorySize(); h++) {
                if (core.dragBoss(ev.getHistoricalX(i, h), ev.getHistoricalY(i, h), layout)) {
                    bossDragging = false;
                    bossDragPointer = -1;
                    completedBossDragHaptic();
                    return true;
                }
            }
            if (core.dragBoss(x, y, layout)) {
                bossDragging = false;
                bossDragPointer = -1;
                completedBossDragHaptic();
            } else {
                // A rejected mushroom sweep cancels ownership inside Boss.dragTo().
                if (core.boss.held < 0 && core.boss.held != -2 && core.boss.held != -3) {
                    bossDragging = false;
                    bossDragPointer = -1;
                }
                dragHaptic();
            }
            return true;
        }
        boolean ownerUp = action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_POINTER_UP
                && ev.getPointerId(ev.getActionIndex()) == bossDragPointer;
        if (ownerUp || action == MotionEvent.ACTION_CANCEL) {
            // Let go part-way: whatever was held stays where it was dropped and carries on
            // counting down. Giving up on a drag is a decision, not a mistake.
            core.releaseBoss();
            bossDragging = false;
            bossDragPointer = -1;
            return true;
        }
        // A non-owner POINTER_UP belongs to its own key tap, not to this continuing drag.
        return action != MotionEvent.ACTION_POINTER_UP;
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
                // One entry point for the upward panic swipe.
                if (core.swipeUp(layout)) tick();
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
        if (!BuildFlags.DEVELOPER) return;
        settingsUi.compute(layout, Music.NAMES.length, core.settingsTab);
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
        } else if (hit == SettingsUi.HIT_GENERAL || hit == SettingsUi.HIT_MINIGAMES) {
            core.settingsTab = hit == SettingsUi.HIT_GENERAL ? SettingsUi.GENERAL : SettingsUi.MINIGAMES;
            tick();
        } else if (hit == SettingsUi.HIT_EASIER || hit == SettingsUi.HIT_HARDER) {
            core.setStarDifficulty(core.stars.wins + (hit == SettingsUi.HIT_EASIER ? -1 : 1));
            tick();
        } else if (hit == SettingsUi.HIT_CLEAR) {
            core.tapClearCase();
            tick();
        } else if (hit == SettingsUi.HIT_ROSTER) {
            core.setNextRoster(!core.fullRoster);
            tick();
        } else if (hit == SettingsUi.HIT_GAMEOVER) {
            core.endCurrentRun();
            tick();
        } else if (hit == SettingsUi.HIT_RESET_LANDS) {
            LandPicker.reset(core);
            tick();
        } else if (hit == SettingsUi.HIT_RESET_DIFFICULTY) {
            core.resetDifficultyScaling();
            tick();
        } else if (hit >= SettingsUi.HIT_STAGE) {
            // Before the playtest branch, not after: HIT_STAGE is the higher number, so a
            // `hit >= HIT_TEST` test would swallow every stage chip.
            //
            // The panel deliberately stays open, so the steppers can be tapped several times while
            // watching the number. Play resumes at whatever stage it is left on.
            core.jumpToStage(core.stage + SettingsUi.STAGE_STEP[hit - SettingsUi.HIT_STAGE],
                    layout);
            tick();
        } else if (hit >= SettingsUi.HIT_DEBUFF) {
            core.playtestDebuff(Power.INCOGNITO+hit-SettingsUi.HIT_DEBUFF);
            tick();
        } else if (hit >= SettingsUi.HIT_TEST) {
            // Closes the panel and drops straight into the mode.
            int chip = hit - SettingsUi.HIT_TEST;
            if (chip == SettingsUi.TEST_STARS) core.playtestStars(layout);
            else if (chip == SettingsUi.TEST_STEAMER) core.playtestSteamer(layout);
            else core.playtestMode(Power.offeredAt(chip), layout);
            tick();
        } else if (hit >= SettingsUi.HIT_OPTION) {
            core.setBgm(hit - SettingsUi.HIT_OPTION);
            tick();
        }
    }

    private void dragHaptic() {
        long now = SystemClock.uptimeMillis();
        if (now - lastBossDragHaptic < 55) return;
        lastBossDragHaptic = now;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private void completedBossDragHaptic() {
        if (core.boss.kind != Boss.OCTOPUS || core.boss.octoDeath <= 0f) {
            tick();
            return;
        }
        // A hard tear at release, then a second lower beat as the head catches the recoil.
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        postDelayed(() -> performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING), 72L);
    }

    private void bossDeathHaptic() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private void bossImpactHaptic() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private void tick() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    @Override protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        float dt = last == 0 ? 1f / 60f : (now - last) / 1000f;
        last = now;
        float elapsed = !background ? dt : 0f;
        if (dt > 0.05f) dt = 0.05f;   // a backgrounded app must not teleport the wave

        try {
            boolean playingBeforeUpdate = core.state == GameCore.PLAY && !core.paused && !background;
            if (!background) {
                core.update(dt, elapsed, layout);
                for (int i = 0; i < core.starPickups; i++) tick();
            }
            refreshNavigation();
            if (playingBeforeUpdate && core.boss.octoImpact) bossImpactHaptic();
            boolean beaten = core.boss.active() && core.boss.beaten;
            if (beaten && !bossWasBeaten) bossDeathHaptic();
            bossWasBeaten = beaten;
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
