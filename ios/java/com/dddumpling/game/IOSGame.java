package com.dddumpling.game;

/** UIKit point-coordinate input and frame adapter. Gesture routing mirrors GameView. */
public final class IOSGame {
    public interface Host {
        void tick();
        void openPrivacy(String url);
    }

    private final GameCore core;
    private final Layout layout = new Layout();
    private final SettingsUi settingsUi = new SettingsUi();

    private Host host;
    private float elapsedClock, delayedHaptic;
    private boolean background;
    private int pausePress;
    public void setHost(Host host) { this.host = host; }
    public boolean handlesBack() { return Pause.handlesBack(core); }
    public boolean paused() { return core.paused; }
    private void cancelPointers() {
        landPointer = starDragPointer = bonusSwipePointer = bossDragPointer = -1;
        bossDragging = bossPinching = pushArmed = false;
        caseGesture = CASE_IDLE; pausePress = 0;
        delayedHaptic = 0f;
        core.endBossPinch();
        Pause.release(core);
    }
    public boolean back() {
        if (!handlesBack()) return false;
        cancelPointers();
        boolean handled = Pause.back(core);
        return handled;
    }
    public void background(boolean hidden) {
        background = hidden;
        if (hidden) { cancelPointers(); Pause.open(core); }
    }


    public IOSGame(GameCore.Store store, GameCore.Sound sound, long seed) {
        core = new GameCore(store, seed);
        core.sound = sound;
        // Has to be after the sound is attached, and before the Activity resumes: the loaded
        // choice is otherwise never announced and the backend picks its own fallback.
        core.startMusic();
    }

    GameCore core() { return core; }
    Layout geometry() { return layout; }

    public void layout(float width, float height, float left, float top, float right, float bottom) {
        if (width > 0 && height > 0) layout.compute(width, height, left, top, right, bottom);
    }
    public boolean touch(IOSTouch ev) {
        if (background) return true;
        if (ev.getActionMasked() == IOSTouch.ACTION_CANCEL) { cancelPointers(); return true; }
        if (core.paused) {
            int action = ev.getActionMasked();
            int hit = Pause.hit(layout, ev.getX(), ev.getY());
            if (action == IOSTouch.ACTION_DOWN) pausePress = hit;
            else if (action == IOSTouch.ACTION_MOVE && hit != pausePress) pausePress = 0;
            else if (action == IOSTouch.ACTION_UP) {
                int selected = pausePress; pausePress = 0;
                if (selected != 0 && selected == hit) { Pause.action(core, hit); tick(); }
            } else if (action == IOSTouch.ACTION_CANCEL || action == IOSTouch.ACTION_POINTER_DOWN)
                pausePress = 0;
            return true;
        }
        int action = ev.getActionMasked();
        if (action == IOSTouch.ACTION_DOWN && PrivacyUi.hit(core, layout, ev.getX(), ev.getY())) {
            if (host != null) host.openPrivacy(PrivacyUi.URL);
            return true;
        }

        if (handleLandPicker(ev, action)) return true;

        if (core.state == GameCore.TITLE) {
            if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN
                    || action == IOSTouch.ACTION_MOVE) {
                int titlePointer = action == IOSTouch.ACTION_MOVE ? 0 : ev.getActionIndex();
                core.titleTouchDown = true;
                core.titleTouchX = ev.getX(titlePointer);
                core.titleTouchY = ev.getY(titlePointer);
            } else if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_CANCEL) {
                core.titleTouchDown = false;
            }
        }

        if (core.state == GameCore.BONUS && core.starBonus) {
            handleStarDrag(ev, action);
            return true;
        }

        if (handleBonusSwipe(ev, action)) return true;

        // The settings panel needs drags, for the speed slider.
        if (BuildFlags.DEVELOPER && core.settingsOpen) {
            if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN
                    || action == IOSTouch.ACTION_MOVE) {
                int i = ev.getActionIndex();
                handleSettings(ev.getX(i), ev.getY(i), action == IOSTouch.ACTION_MOVE);
            }
            return true;
        }

        // The display case browses by touch. Needs MOVE events, so it comes before the down-only
        // filter, and it holds on to a gesture that outlives the case being closed.
        if (core.state == GameCore.TITLE && (core.caseOpen || caseGesture != CASE_IDLE)
                && handleCase(ev, action)) {
            return true;
        }

        // A visible powerup owns a direct down on its icon before field gestures.
        if ((action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN)
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

        if (action != IOSTouch.ACTION_DOWN && action != IOSTouch.ACTION_POINTER_DOWN) {
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
    private boolean handleLandPicker(IOSTouch ev, int action) {
        if (action == IOSTouch.ACTION_DOWN) {
            landPointer = -1;
            if (!LandPicker.down(core, layout, ev.getX(), ev.getY())) return false;
            if (core.landPickerDragging) landPointer = ev.getPointerId(0);
            tick(); return true;
        }
        if (landPointer < 0) return false;
        if (!LandPicker.visible(core) || action == IOSTouch.ACTION_CANCEL) {
            core.landPickerDragging = false; landPointer = -1; return true;
        }
        int index = ev.findPointerIndex(landPointer);
        if (index < 0) { core.landPickerDragging = false; landPointer = -1; return true; }
        int before = core.landChoice;
        if (action == IOSTouch.ACTION_MOVE) LandPicker.move(core, layout, ev.getX(index));
        else if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_POINTER_UP
                && ev.getPointerId(ev.getActionIndex()) == landPointer) {
            LandPicker.up(core, layout, ev.getX(index), ev.getY(index)); landPointer = -1;
        }
        if (before != core.landChoice) tick();
        return true;
    }

    private int starDragPointer = -1;
    private float starDragOffsetX;

    /** Grab the Starpath flyer and move it directly left or right. */
    private boolean handleStarDrag(IOSTouch ev, int action) {
        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
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
        } else if (action == IOSTouch.ACTION_MOVE && starDragPointer >= 0) {
            int i = ev.findPointerIndex(starDragPointer);
            if (i >= 0) core.stars.dragTo(ev.getX(i) + starDragOffsetX, layout);
            return true;
        } else if (action == IOSTouch.ACTION_CANCEL) {
            // Let the caller also release every held arrow key.
            starDragPointer = -1;
            core.stars.endDrag();
        } else if ((action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_POINTER_UP)
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
    private boolean handleBonusSwipe(IOSTouch ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);
        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
            if (!core.bonusSwipeReady() || !Screens.inSteamerLid(core, layout, x, y)) return false;
            bonusSwipePointer = ev.getPointerId(i);
            bonusSwipeStartY = y;
            return true;
        }
        if (bonusSwipePointer < 0) return false;
        if (action == IOSTouch.ACTION_MOVE) {
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
        if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_POINTER_UP
                || action == IOSTouch.ACTION_CANCEL) {
            if (action != IOSTouch.ACTION_CANCEL
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
    private boolean handleCase(IOSTouch ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
            // A story is modal: the caller dismisses it and nothing else acts on that touch.
            if (core.storyOpen() || core.keyAt(x, y, layout) >= 0) return false;
            caseHit = Showcase.hit(core, layout, x, y);
            caseDownX = x;
            caseDownY = y;
            caseGesture = CASE_TAP;
            return true;
        }
        if (caseGesture == CASE_IDLE) return false;

        if (action == IOSTouch.ACTION_MOVE) {
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
                    tick();
            }
            return true;
        }
        if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_CANCEL
                || action == IOSTouch.ACTION_POINTER_UP) {
            if (caseGesture == CASE_TAP && action != IOSTouch.ACTION_CANCEL) tapCase(caseHit);
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
    /** Stable pointer that owns the drag; other fingers remain free to tap the key deck. */
    private int bossDragPointer = -1;
    private boolean bossPinching;
    private float lastBossDragHaptic = -1f;
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
    private boolean handleBoss(IOSTouch ev, int action) {
        if (core.state != GameCore.PLAY) return false;
        // A second finger during a carry is not a new boss gesture. In particular, let a pointer
        // landing on the deck fall through to ordinary key routing without replacing the cap owner.
        if (bossDragging && action == IOSTouch.ACTION_POINTER_DOWN) return false;
        if (action == IOSTouch.ACTION_POINTER_DOWN && ev.getPointerCount() == 2) {
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
            if (action == IOSTouch.ACTION_MOVE && ev.getPointerCount() >= 2) {
                float dx = ev.getX(0) - ev.getX(1), dy = ev.getY(0) - ev.getY(1);
                if (core.pinchBoss((float) Math.sqrt(dx * dx + dy * dy),
                        ev.getX(0), ev.getY(0), ev.getX(1), ev.getY(1), layout)) tick();
                return true;
            }
            if (action == IOSTouch.ACTION_POINTER_UP || action == IOSTouch.ACTION_UP
                    || action == IOSTouch.ACTION_CANCEL) {
                core.endBossPinch();
                bossPinching = false;
                return true;
            }
        }
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
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

        if (action == IOSTouch.ACTION_MOVE) {
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
        boolean ownerUp = action == IOSTouch.ACTION_UP
                || action == IOSTouch.ACTION_POINTER_UP
                && ev.getPointerId(ev.getActionIndex()) == bossDragPointer;
        if (ownerUp || action == IOSTouch.ACTION_CANCEL) {
            // Let go part-way: whatever was held stays where it was dropped and carries on
            // counting down. Giving up on a drag is a decision, not a mistake.
            core.releaseBoss();
            bossDragging = false;
            bossDragPointer = -1;
            return true;
        }
        // A non-owner POINTER_UP belongs to its own key tap, not to this continuing drag.
        return action != IOSTouch.ACTION_POINTER_UP;
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
    private boolean handlePush(IOSTouch ev, int action) {
        int i = ev.getActionIndex();
        float y = ev.getY(i);

        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
            if (core.state != GameCore.PLAY || !layout.inPushZone(ev.getX(i), y)) {
                return false;
            }
            pushArmed = true;
            pushStartY = y;
            return true;
        }
        if (!pushArmed) return false;

        if (action == IOSTouch.ACTION_MOVE) {
            // A clear upward flick, not a twitch.
            if (pushStartY - y >= layout.enemyR * 1.6f) {
                // One entry point for the upward panic swipe.
                if (core.swipeUp(layout)) tick();
                pushArmed = false;
            }
            return true;
        }
        if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_CANCEL
                || action == IOSTouch.ACTION_POINTER_UP) {
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
    private boolean handleFling(IOSTouch ev, int action) {
        int i = ev.getActionIndex();
        float x = ev.getX(i), y = ev.getY(i);

        if (action == IOSTouch.ACTION_DOWN || action == IOSTouch.ACTION_POINTER_DOWN) {
            if (y > layout.deckTop) return false;          // that is the key deck
            core.beginStroke(x, y);
            return true;
        }

        if (action == IOSTouch.ACTION_MOVE) {
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

        if (action == IOSTouch.ACTION_UP || action == IOSTouch.ACTION_CANCEL
                || action == IOSTouch.ACTION_POINTER_UP) {
            boolean had = core.touchDown;
            core.endStroke();
            return had;
        }
        return core.touchDown;
    }

    private void handleSettings(float x, float y, boolean dragging) {
        if (!BuildFlags.DEVELOPER) return;
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
        if (elapsedClock - lastBossDragHaptic < .055f) return;
        lastBossDragHaptic = elapsedClock;
        tick();
    }

    private void completedBossDragHaptic() {
        if (core.boss.kind != Boss.OCTOPUS || core.boss.octoDeath <= 0f) {
            tick();
            return;
        }
        tick();
        delayedHaptic = .072f;
    }

    private void tick() {
        if (host != null) host.tick();
    }

    /** Native display-link elapsed seconds, with physics capped after an interrupted frame. */
    public void update(float elapsed) {
        if (background || layout.w <= 0 || elapsed < 0f || Float.isNaN(elapsed)
                || Float.isInfinite(elapsed)) return;
        elapsedClock += elapsed;
        if (delayedHaptic > 0f) {
            delayedHaptic -= elapsed;
            if (delayedHaptic <= 0f) tick();
        }
        boolean playing = core.state == GameCore.PLAY && !core.paused;
        core.update(Math.min(elapsed, .05f), elapsed, layout);
        if (playing && core.boss.octoImpact) tick();
        boolean beaten = core.boss.active() && core.boss.beaten;
        if (beaten && !bossWasBeaten) tick();
        bossWasBeaten = beaten;
    }

    public void draw(Painter painter) {
        if (layout.w > 0) Renderer.draw(painter, core, layout);
    }

    /** Simulator scenes use the same setup methods as the developer panel. */
    public String debugStatus() {
        if (!BuildFlags.DEVELOPER) return "";
        return "state=" + core.state + ";stage=" + core.stage + ";paused=" + core.paused
                + ";score=" + core.score + ";lives=" + core.lives + ";case=" + core.caseOpen
                + ";stars=" + core.starBonus + ";mode=" + core.mode;
    }

    public void debugScene(String scene) {
        if (!BuildFlags.DEVELOPER || scene == null || layout.w <= 0) return;
        int stage = 0;
        if (scene.startsWith("stage:")) {
            try { stage = Integer.parseInt(scene.substring(6)); }
            catch (NumberFormatException invalid) { return; }
        } else if (!scene.equals("title") && !scene.equals("case") && !scene.equals("play")
                && !scene.equals("stars") && !scene.equals("steamer") && !scene.equals("fling")
                && !scene.equals("pause")) return;
        cancelPointers();
        Pause.resume(core);
        if (scene.equals("title")) { core.toTitle(); return; }
        if (scene.equals("case")) { core.toTitle(); core.openCase(); return; }
        core.startGame();
        if (scene.equals("stars")) core.playtestStars(layout);
        else if (scene.equals("steamer")) core.playtestSteamer(layout);
        else if (scene.equals("fling")) core.playtestMode(Power.FLING, layout);
        else if (scene.startsWith("stage:")) core.jumpToStage(stage, layout);
        else if (scene.equals("pause")) Pause.open(core);
    }
}
