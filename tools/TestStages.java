package com.dddumpling.game;

/** Stage waves, the between-stages minigame, accuracy and danger warnings. */
final class TestStages extends Check {

    static void waves(Layout L) {
        group("stage waves");
        GameCore c = new GameCore(new Mem(), 41L);
        c.startGame();
        check("stage starts with nothing released", c.spawnedThisStage == 0);
        check("quota grows with stage", quotaAt(c, 8) > quotaAt(c, 1));
        check("quota is capped", quotaAt(c, 99) <= 10);
        c.stage = 1;

        int frames = 0, stageChanges = 0;
        int prevStage = c.stage;
        boolean overQuota = false, spawnedEarly = false, emptyOnAdvance = true;

        // Play perfectly through several waves, watching the wave invariants every frame.
        // The interlude counts as being in the run, so BONUS must not end the loop.
        while (frames < 60 * 400 && stageChanges < 4
                && (c.state == GameCore.PLAY || c.state == GameCore.BONUS)) {
            c.update(DT, L);
            frames++;

            if (c.spawnedThisStage > c.stageQuota()) overQuota = true;
            if (c.stage != prevStage) {
                stageChanges++;
                // Sampled on the advancing frame itself: the last word of a wave leaves the
                // field during that same frame, so the previous frame can still hold it.
                if (!c.enemies.isEmpty()) emptyOnAdvance = false;
                if (c.spawnedThisStage != 0) spawnedEarly = true;
                prevStage = c.stage;
            }

            if (frames % 2 != 0) continue;
            if (c.state == GameCore.BONUS) {
                c.tapBonus(c.steamer.wanted());
                continue;
            }
            GameCore.Enemy e =
                    c.target != null && c.enemies.contains(c.target) && c.target.typeable()
                            ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }

        System.out.printf("    %d stage transitions in %.0fs, reached stage %d%n",
                stageChanges, frames * DT, c.stage);
        check("stages do advance", stageChanges >= 3);
        check("a stage never releases more than its quota", !overQuota);
        check("field is empty when a stage advances", emptyOnAdvance);
        check("next wave starts from zero released", !spawnedEarly);

        // Once the quota is out and the field is clear, the gap must hold off the next wave.
        GameCore w = new GameCore(new Mem(), 42L);
        w.startGame();
        w.spawnedThisStage = w.stageQuota();
        w.enemies.clear();
        w.shots.clear();
        check("quota exhausted means the stage is cleared", w.stageCleared());
        int before = w.stage;
        w.update(DT, L);
        check("clearing the wave opens the interlude", advanceToBonus(w, L));
        advancePastBonus(w, L);
        check("the stage advances after the interlude", w.stage == before + 1);
        check("a breather follows the interlude", w.stageGap > 0f);
        int released = w.spawnedThisStage;
        advance(w, L, GameCore.STAGE_GAP * 0.6f);
        check("nothing spawns during the breather",
                w.spawnedThisStage == released && w.enemies.isEmpty());
        advance(w, L, GameCore.STAGE_GAP);
        check("the next wave starts after the breather", w.spawnedThisStage > 0);

        // A breached word still counts as resolved, so a stage cannot stall on a miss.
        GameCore b = new GameCore(new Mem(), 43L);
        b.startGame();
        b.spawnedThisStage = b.stageQuota();
        b.enemies.clear();
        add(b, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        // A second word keeps the field occupied, so the stage cannot advance and reset the
        // counter before it can be observed.
        add(b, L, new int[] {3, 3}, L.playTop + 40);
        int resolved = b.resolvedThisStage;
        int stageWas = b.stage;
        advance(b, L, GameCore.ATTACK_TIME + 2 * DT);
        check("a breached word counts as resolved", b.resolvedThisStage == resolved + 1);
        check("a breach alone does not advance the stage", b.stage == stageWas);

        // Clear the survivor: the stage must then advance despite one word being lost.
        b.enemies.clear();
        b.shots.clear();
        b.update(DT, L);
        check("the interlude opens after a breach", advanceToBonus(b, L));
        advancePastBonus(b, L);
        check("stage still advances after a breach", b.stage == stageWas + 1);
    }

    static int quotaAt(GameCore c, int stage) {
        int was = c.stage;
        c.stage = stage;
        int q = c.stageQuota();
        c.stage = was;
        return q;
    }

    /** The interlude opens only after the flawless-wave celebration has finished. */
    static void bonusOrdering(Layout L) {
        group("interlude ordering");
        GameCore c = new GameCore(new Mem(), 131L);
        c.startGame();
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);

        // A flawless wave: the reward shows first and the interlude waits for it.
        check("the perfect indicator is up", c.perfectBanner > 0f);
        check("still in play, holding", c.state == GameCore.PLAY && c.pendingBonus);
        check("the interlude has not opened", c.state != GameCore.BONUS);
        advance(c, L, GameCore.PERFECT_TIME * 0.5f);
        check("still holding part-way through the celebration",
                c.state == GameCore.PLAY && c.perfectBanner > 0f);
        check("nothing spawns while holding", c.enemies.isEmpty());
        advance(c, L, GameCore.PERFECT_TIME);
        check("the interlude opens once it finishes", c.state == GameCore.BONUS);
        check("the hold is released", !c.pendingBonus);

        // A flawed wave has no celebration to wait for, so it goes straight in.
        GameCore d = new GameCore(new Mem(), 132L);
        d.startGame();
        d.enemies.clear();
        d.tapKey(0, L);                       // a miss forfeits the reward
        d.spawnedThisStage = d.stageQuota();
        d.enemies.clear();
        d.shots.clear();
        d.update(DT, L);
        check("no celebration to wait for", d.perfectBanner == 0f);
        d.update(DT, L);
        check("a flawed wave goes straight to the interlude", d.state == GameCore.BONUS);
    }

    /** The interlude heading swells into place over the fade-in. */
    static void bonusIntro(Layout L) {
        group("interlude intro");
        check("starts large", Screens.introScale(0f) > 1.5f);
        check("settles at full size",
                Math.abs(Screens.introScale(Screens.INTRO_TIME) - 1f) < 0.001f);
        check("stays settled afterwards", Screens.introScale(5f) == 1f);

        // Shrinks overall, dipping just under the target before springing back — so it is
        // deliberately not monotone at the tail.
        float smallest = Float.MAX_VALUE, largest = 0f;
        for (float t = 0f; t <= Screens.INTRO_TIME; t += Screens.INTRO_TIME / 60f) {
            float v = Screens.introScale(t);
            smallest = Math.min(smallest, v);
            largest = Math.max(largest, v);
        }
        check("it shrinks overall", largest == Screens.introScale(0f));
        check("it dips just past the target", smallest < 1f && smallest > 0.85f);
        check("never inverted or absurd",
                Screens.introScale(0f) < 3f && Screens.introScale(0.01f) > 0f);
    }

    /**
     * Plays a whole interlude and checks that exactly one phase predicate is true on every
     * frame of it. Two of them true at once is a contradiction; none is a phase nobody named,
     * which is how a screen that draws nothing gets shipped.
     */
    private static boolean onePhaseThroughout(long seed, boolean win, boolean panic, Layout L) {
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        // A panic round earns the shortest mash there is, which makes it the narrowest phase window
        // the interlude ever runs — exactly where a boundary that no longer lines up would hide.
        if (panic) {
            c.enemies.clear();
            add(c, L, new int[] {0, 1}, L.dangerY - L.enemyR * 1.2f);
            c.update(DT, L);
            if (!c.pushBack(L)) return false;
            c.enemies.clear();
        }
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        if (!advanceToBonus(c, L)) return false;
        boolean ok = true, sawWin = false;
        for (int i = 0; i < 60 * 30 && c.state == GameCore.BONUS; i++) {
            if (win && c.bonusMashing() && !sawWin) {
                for (int k = 0; k < c.steamer.goal() * 2 + 4; k++) {
                    c.tapBonus(c.steamer.wanted());
                }
                c.swipeBonus();
                sawWin = c.prize >= 0;
            }
            int on = 0;
            if (c.bonusRolling()) on++;
            if (c.bonusMashing()) on++;
            if (c.bonusHolding()) on++;
            if (c.bonusStatus()) on++;
            if (c.bonusEscape()) on++;
            if (c.bonusParading()) on++;
            if (on != 1) {
                System.out.printf("    %d phases at once, timer=%.2f parade=%.2f%n", on,
                        c.bonusTimer, c.paradeTimer);
                ok = false;
                break;
            }
            c.update(DT, L);
        }
        return ok && (!win || sawWin);
    }

    /** Opens the interlude and steps past the spinner, so presses count. */
    private static boolean toMash(GameCore c, Layout L) {
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        return advanceToMash(c, L);
    }

    /** The push-back: when it is offered, what it moves, and that it is once a stage. */
    static void pushLesson(Layout L) {
        group("desperation swipe lesson");
        Mem mem = new Mem(); mem.pushLessonSeen = false;
        GameCore c = new GameCore(mem, 161L);
        c.startGame(); advance(c,L,2f); c.enemies.clear();
        GameCore.Enemy e = add(c,L,new int[] {1,2},L.dangerY-L.enemyR);
        c.update(DT,L);
        check("healthy player is not interrupted", !c.pushLesson.active);
        c.lives=1; c.settingsOpen=true; c.update(DT,L);
        check("settings do not open lesson", !c.pushLesson.active);
        c.settingsOpen=false; c.pushUsed=true; c.update(DT,L);
        check("spent swipe cannot trap player", !c.pushLesson.active);
        c.pushUsed=false; c.update(DT,L);
        check("last-life threat opens lesson before damage", c.pushLesson.active && c.lives==1);
        float y=e.y, time=c.time, mode=c.modeLeft;
        c.update(10f,L);
        check("lesson freezes words and simulation timers", e.y==y && c.time==time && c.modeLeft==mode);
        check("keys cannot dismiss lesson", !c.tapKey(1,L) && c.pushLesson.active);
        float x=L.w*.5f, bar=(L.dangerY+L.deckTop)*.5f, rise=L.enemyR*2;
        c.pushLesson.touch(c,L,0,x,L.playTop);
        c.pushLesson.touch(c,L,2,x,L.playTop-rise);
        check("swipe must begin at bar", c.pushLesson.active);
        c.pushLesson.touch(c,L,0,x,bar);
        c.pushLesson.touch(c,L,1,x,bar);
        check("tap does not dismiss", c.pushLesson.active);
        c.pushLesson.touch(c,L,0,x,bar);
        c.pushLesson.touch(c,L,2,x,bar+rise);
        c.pushLesson.touch(c,L,2,x+rise*2,bar-rise);
        check("downward and sideways drags do not dismiss", c.pushLesson.active);
        c.pushLesson.touch(c,L,3,x,bar);
        c.pushLesson.touch(c,L,2,x,bar-rise);
        check("cancelled gesture cannot finish", c.pushLesson.active);
        c.pushLesson.touch(c,L,0,x,bar);
        check("upward swipe performs push", c.pushLesson.touch(c,L,2,x,bar-rise) && c.pushUsed);
        check("completion persists and owns remaining touch", !c.pushLesson.active && mem.pushLessonSeen && c.pushLesson.ownsTouch);
        c.pushLesson.touch(c,L,1,x,bar-rise);
        check("lift releases touch", !c.pushLesson.ownsTouch);
        check("reload remembers completion", new GameCore(mem,1L).pushLesson.seen);
        c.startGame();
        check("new run remembers completion", c.pushLesson.seen && !c.pushLesson.active);
        mem.pushLessonSeen=false;
        GameCore boss=new GameCore(mem,2L); boss.startGame(); boss.jumpToStage(5,L);
        boss.lives=1; add(boss,L,new int[] {1,2},L.dangerY);
        boss.update(DT,L);
        check("boss excluded", !boss.pushLesson.active);
        boss.jumpToStage(21,L); boss.lives=1;
        add(boss,L,new int[] {1,2},L.dangerY); boss.update(DT,L);
        check("cave excluded", !boss.pushLesson.active);
    }

    static void pushBack(Layout L) {
        group("push-back");
        GameCore c = new GameCore(new Mem(), 161L);
        c.startGame();
        c.enemies.clear();
        advance(c, L, 2f);
        c.enemies.clear();
        check("not offered with an empty field", !c.pushReady());

        // A word high up is not a threat, so the swipe stays unavailable.
        GameCore.Enemy high = add(c, L, new int[] {1, 2}, L.playTop + 40f);
        c.update(DT, L);
        check("a word far from the line does not arm it", !c.pushReady());
        check("nothing to warn about", c.warnLevel == 0f);

        // One inside the warning band does.
        GameCore.Enemy low = add(c, L, new int[] {3, 4}, L.dangerY - L.enemyR * 1.2f);
        c.update(DT, L);
        check("something closing in arms it", c.warnLevel > 0f && c.pushReady());

        float highWas = high.y, lowWas = low.y;
        check("the swipe fires", c.pushBack(L));
        check("it counted what it moved", c.pushCount == 1);
        check("the alarm is cleared", c.warnLevel == 0f);
        check("and it shows a shockwave", c.pushT > 0f);
        // Resolved now, travelled afterwards. The word has not moved yet; it has been given
        // somewhere to be and PUSH_SLIDE to get there.
        check("the near word does not teleport", low.y == lowWas);
        check("it is aimed well back up the field", low.slideTo < lowWas - L.enemyR);
        check("and given the slide to get there", low.slideT == GameCore.PUSH_SLIDE);
        check("the far word is left alone", high.y == highWas && high.slideT == 0f);

        // It climbs, a frame at a time, and lands on its mark.
        float prev = low.y, first = -1;
        boolean rose = true, quiet = true;
        int frames = 0;
        while (low.slideT > 0f && frames < 200) {
            c.update(DT, L);
            frames++;
            if (frames == 1) first = low.y;
            if (low.y > prev) rose = false;
            // Still below the line for these frames: it must not sound the alarm on the way up,
            // nor rearm the lunge the swipe just called off.
            if (low.warn > 0f || low.attacking) quiet = false;
            prev = low.y;
        }
        check("it moves the first frame without arriving", first > low.slideTo && first < lowWas);
        check("it climbs every frame of the slide", rose);
        check("without warning or lunging on the way", quiet);
        check("the slide takes its own time", Math.abs(frames * DT - GameCore.PUSH_SLIDE) <= DT);
        check("landing where it was aimed", Math.abs(low.y - low.slideTo) < 0.01f);
        // The descent has to come back on its own. Test words are parked with no speed, so it
        // needs one to have anywhere to fall.
        float landed = low.y;
        low.speed = L.enemyR * 4f;
        c.update(DT, L);
        check("and falling again once it lands", low.y > landed);
        low.speed = 0f;

        // Spent for the stage.
        check("no longer offered", !c.pushReady());
        check("and it will not fire again", !c.pushBack(L));
        low.y = L.dangerY - L.enemyR * 1.2f;
        c.update(DT, L);
        check("still spent with a fresh threat", c.warnLevel > 0f && !c.pushReady());

        // A committed lunge is called off — the moment the button exists for.
        GameCore d = new GameCore(new Mem(), 163L);
        d.startGame();
        d.enemies.clear();
        advance(d, L, 2f);
        d.enemies.clear();
        GameCore.Enemy diving = add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1f);
        advance(d, L, GameCore.ATTACK_TIME * 0.4f);
        check("it is lunging", diving.attacking && d.lives == GameCore.START_LIVES);
        check("the swipe is available", d.pushReady());
        check("and it fires", d.pushBack(L));
        check("the lunge is called off", !diving.attacking && diving.attackT == 0f);
        check("no life was lost", d.lives == GameCore.START_LIVES);
        // The called-off word starts the slide from below the line, so the frames before it
        // clears the line are exactly where a rearmed lunge would have cost the life anyway.
        advance(d, L, GameCore.PUSH_SLIDE * 0.5f);
        check("it is still climbing out", diving.slideT > 0f && !diving.attacking);
        check("and has not been charged for it", d.lives == GameCore.START_LIVES);
        advance(d, L, GameCore.ATTACK_TIME + 0.2f);
        check("and none is lost afterwards", d.lives == GameCore.START_LIVES);

        // Never pushed above the top of the field.
        GameCore e = new GameCore(new Mem(), 165L);
        e.startGame();
        e.enemies.clear();
        advance(e, L, 2f);
        e.enemies.clear();
        GameCore.Enemy near = add(e, L, new int[] {1}, L.dangerY - L.enemyR);
        e.update(DT, L);
        e.pushBack(L);
        check("it is not aimed out of the field", near.slideTo >= L.playTop);
        advance(e, L, GameCore.PUSH_SLIDE + DT);
        check("it stays inside the field", near.y >= L.playTop);

        // A new stage hands the swipe back.
        GameCore f = new GameCore(new Mem(), 167L);
        f.startGame();
        f.enemies.clear();
        advance(f, L, 2f);
        f.enemies.clear();
        add(f, L, new int[] {1}, L.dangerY - L.enemyR * 1.2f);
        f.update(DT, L);
        f.pushBack(L);
        check("spent this stage", f.pushUsed);
        f.enemies.clear();
        f.shots.clear();
        f.spawnedThisStage = f.stageQuota();
        check("through the interlude", advancePastBonus(f, L));
        check("the next stage hands it back", !f.pushUsed);
        // And so does a fresh run.
        f.pushUsed = true;
        f.startGame();
        check("so does a new game", !f.pushUsed && f.pushT == 0f);

        // Not offered outside play, or behind the settings panel.
        GameCore g = new GameCore(new Mem(), 169L);
        g.startGame();
        g.enemies.clear();
        advance(g, L, 2f);
        g.enemies.clear();
        add(g, L, new int[] {1}, L.dangerY - L.enemyR * 1.2f);
        g.update(DT, L);
        check("offered in play", g.pushReady());
        g.openSettings();
        check("not behind the settings panel", !g.pushReady());
        g.closeSettings();
        g.update(DT, L);
        check("offered again once it closes", g.pushReady());
        g.toTitle();
        check("not on the title screen", !g.pushReady() && !g.pushBack(L));
    }

    /** The spinner as a pure function: it must land on its answer, from any pair. */
    static void spinner(Layout L) {
        group("interlude spinner");
        int half = Glyph.COUNT / 2;
        boolean lands = true, startsElsewhere = false, staysInCluster = true, decelerates = true;
        for (int key = 0; key < half; key++) {
            if (Steamer.rolled(key, 0, half, 1f) != key) lands = false;
            // Overshooting the end must still hold the answer, since a frame can land past 1.
            if (Steamer.rolled(key, 0, half, 1.4f) != key) lands = false;
            if (Steamer.rolled(key, 0, half, 0f) != key) startsElsewhere = true;

            int changes = 0, firstHalf = 0, lastHalf = 0, prev = -1;
            for (int i = 0; i <= 240; i++) {
                float t = i / 240f;
                int g = Steamer.rolled(key, 0, half, t);
                if (g < 0 || g >= half) staysInCluster = false;
                if (g != prev) {
                    if (prev >= 0) {
                        changes++;
                        if (t < 0.5f) firstHalf++;
                        else lastHalf++;
                    }
                    prev = g;
                }
            }
            // The whole point of the easing: most of the stepping happens up front.
            if (firstHalf <= lastHalf) decelerates = false;
            if (changes < 4) decelerates = false;
        }
        check("the spinner lands on its answer", lands);
        check("and holds it past the end", lands);
        check("it starts somewhere else, so there is a spin to watch", startsElsewhere);
        check("it only ever offers that thumb's own keys", staysInCluster);
        check("it slows down: more steps in the first half than the second", decelerates);

        // The right-hand cluster is offset, and must never offer a left-hand key.
        boolean rightSide = true;
        for (int key = half; key < Glyph.COUNT; key++) {
            for (int i = 0; i <= 60; i++) {
                int g = Steamer.rolled(key, half, Glyph.COUNT - half, i / 60f);
                if (g < half || g >= Glyph.COUNT) rightSide = false;
            }
            if (Steamer.rolled(key, half, Glyph.COUNT - half, 1f) != key) rightSide = false;
        }
        check("the right slot stays on the right thumb", rightSide);

        // And through the object, which is how the renderer reads it.
        Steamer st = new Steamer();
        st.reset();
        st.leftKey = 1;
        st.rightKey = Glyph.COUNT - 1;
        check("shownLeft settles on the left key", st.shownLeft(1f) == 1);
        check("shownRight settles on the right key",
                st.shownRight(1f) == Glyph.COUNT - 1);
        check("the spinner takes enough steps to read as one", Steamer.ROLL_STEPS >= 8);
    }

    /** The four interlude phases in order, and the crowd cap during a frenzy. */
    static void bonusStatusHold(Layout L) {
        group("interlude phases");
        GameCore c = new GameCore(new Mem(), 141L);
        c.startGame();
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        check("reaches the interlude", advanceToBonus(c, L));

        // 1. Spinner. Presses are refused, and the pair is already decided behind it.
        check("opens on the spinner",
                c.bonusRolling() && !c.bonusMashing() && !c.bonusHolding() && !c.bonusStatus());
        check("the timer covers all four phases",
                c.bonusTimer > GameCore.bonusLength(c.earnedMash) - 0.01f);
        int picked = c.steamer.wanted();
        c.tapBonus(picked);
        c.tapBonus(picked);
        check("presses are refused while it spins", c.steamer.hits == 0);
        check("the clock reads the full earned mash through the spin",
                Math.abs(c.bonusLeft() - c.earnedMash) < 0.01f);

        // The spinner has to land on the pair the round then asks for, every frame of it.
        boolean landed = true, moved = false;
        int seenLeft = c.bonusLeftKey();
        for (int i = 0; i < 60 * 10 && c.bonusRolling(); i++) {
            c.update(DT, L);
            if (c.bonusLeftKey() != seenLeft) {
                moved = true;
                seenLeft = c.bonusLeftKey();
            }
        }
        if (c.bonusLeftKey() != c.steamer.leftKey
                || c.bonusRightKey() != c.steamer.rightKey) landed = false;
        check("the spinner does step through characters", moved);
        check("it settles on exactly the pair the round uses", landed);
        check("the pair survived the spin", c.steamer.wanted() == picked);

        // 2. Mash.
        check("mashing opens once it lands",
                c.bonusMashing() && !c.bonusRolling() && !c.bonusStatus());
        c.tapBonus(c.steamer.wanted());
        c.tapBonus(c.steamer.wanted());
        check("a completed pair lands during the mash phase", c.steamer.hits == 1);
        float wasLeft = c.bonusLeft();
        advance(c, L, 0.3f);
        check("the clock runs down during the mash", c.bonusLeft() < wasLeft);

        // 3. The beat on zero: still on screen, still not fading, no longer taking presses.
        advance(c, L, c.bonusTimer - (GameCore.BONUS_HOLD + GameCore.BONUS_STATUS) + 2 * DT);
        check("the beat on zero starts", c.bonusHolding() && !c.bonusMashing());
        check("the clock has run out", c.bonusLeft() == 0f);
        int held = c.steamer.hits;
        c.tapBonus(c.steamer.wanted());
        check("presses are ignored on zero", c.steamer.hits == held);
        check("the beat is a full second before anything fades",
                Math.abs(GameCore.BONUS_HOLD - 1f) < 0.001f);

        // 4. Status hold.
        advance(c, L, GameCore.BONUS_HOLD + 2 * DT);
        check("the status hold follows the beat", c.bonusStatus() && !c.bonusHolding());
        check("still in the interlude", c.state == GameCore.BONUS);
        int hits = c.steamer.hits;
        c.tapBonus(1);
        check("presses are ignored during the status hold", c.steamer.hits == hits);
        check("the hold is about a second and a half",
                Math.abs(GameCore.BONUS_STATUS - 1.5f) < 0.001f);

        advance(c, L, GameCore.BONUS_STATUS + 0.1f);
        check("play resumes after the hold", c.state == GameCore.PLAY);
        check("the stage turned over", c.stage == 2);

        // Exactly one phase at a time, over both paths through an interlude — a lost round and
        // a won one. An unnamed gap here is what a phase that draws nothing looks like.
        check("a lost round is always in exactly one phase",
                onePhaseThroughout(151L, false, false, L));
        check("and so is a won one", onePhaseThroughout(153L, true, false, L));
        // And the same over the one-second mash a panic round earns, which is the shortest window
        // any of these phases has to hold open.
        check("a panic round holds its phases too", onePhaseThroughout(155L, false, true, L));
        check("even when it is won inside that second", onePhaseThroughout(157L, true, true, L));

        // A frenzy allows more words on screen at once — four times as many on the opening stage,
        // tapering with the ramp, since the cap multiplies a maxEnemies that is itself climbing.
        GameCore d = new GameCore(new Mem(), 142L);
        d.startGame();
        d.stage = 6;
        int calm = d.crowdCap();
        check("the calm cap is the ordinary one", calm == d.maxEnemies());
        d.startFrenzy(Power.FLURRY, L);
        check("a frenzy raises the cap by the tapered rate",
                d.crowdCap() == (int) (calm * Power.crowdRate(d.ramp())));
        check("that is a real increase", d.crowdCap() > calm);
        check("and less than the opening stage would have got",
                Power.crowdRate(d.ramp()) < Power.CROWD_RATE);
        d.mode = -1;
        d.modeLeft = 0f;
        check("and it reverts", d.crowdCap() == calm);
    }

    /** The ten stage vignettes. */
    static void skits(Layout L) {
        group("stage vignettes");
        check("there are ten", Skits.COUNT == 10);
        check("each is named", Skits.NAMES.length == Skits.COUNT);

        boolean allSeen = true;
        boolean[] seen = new boolean[Skits.COUNT];
        for (int stage = 1; stage <= Skits.COUNT; stage++) {
            int i = Skits.forStage(stage);
            if (i < 0 || i >= Skits.COUNT) allSeen = false;
            else seen[i] = true;
        }
        for (int i = 0; i < Skits.COUNT; i++) if (!seen[i]) allSeen = false;
        check("the first ten stages show all ten", allSeen);
        check("they cycle after that", Skits.forStage(11) == Skits.forStage(1));
        check("stage 1 is in range", Skits.forStage(1) >= 0);

        // Every skit must draw at any progress without throwing, at any opacity.
        RasterPainter rp = new RasterPainter(200, 120, 1);
        rp.clear(0xFF000000);
        boolean drew = true;
        try {
            for (int i = 0; i < Skits.COUNT; i++) {
                for (float t = 0f; t <= 1f; t += 0.05f) {
                    Skits.draw(rp, L, i, 100f, 60f, 40f, t, 235, t * 3f);
                }
                Skits.draw(rp, L, i, 100f, 60f, 40f, 0f, 0, 0f);
            }
            // Out-of-range indices fall back rather than crash.
            Skits.draw(rp, L, 99, 100f, 60f, 40f, 0.5f, 235, 0f);
            Skits.draw(rp, L, -1, 100f, 60f, 40f, 0.5f, 235, 0f);
        } catch (Throwable e) {
            drew = false;
            System.out.println("    skit draw threw: " + e);
        }
        check("every skit draws at every progress", drew);

        // And each actually puts ink on the canvas somewhere in its run.
        boolean allDraw = true;
        for (int i = 0; i < Skits.COUNT; i++) {
            RasterPainter one = new RasterPainter(200, 120, 1);
            one.clear(0xFF000000);
            for (float t = 0f; t <= 1f; t += 0.1f) {
                Skits.draw(one, L, i, 100f, 60f, 40f, t, 235, t * 3f);
            }
            int[] px = one.resolve();
            boolean any = false;
            for (int k = 0; k < px.length; k++) if ((px[k] & 0xFFFFFF) != 0) any = true;
            if (!any) {
                allDraw = false;
                System.out.println("    skit " + Skits.NAMES[i] + " drew nothing");
            }
        }
        check("every skit is visible", allDraw);
    }

    /**
     * What the round earns at the steamer. The interlude used to be a flat length whatever you did
     * to get there; it is the round's pay packet now, so how it is earned is a rule and gets held.
     *
     * Each case is built by making the one thing true that the tier turns on and then clearing the
     * wave, so what is being asserted is the ladder and not the arithmetic of {@code mashEarned}.
     */
    /**
     * What the panic swipe is worth, and where it can be started from.
     *
     * It was ineffective and hard to trigger, and those were two separate faults. Hard to trigger:
     * the only place a finger could start it was the sliver between the danger line and the deck,
     * about a thirtieth of the screen, which a thumb coming up off a key overshoots. Ineffective:
     * distance alone bought about a second, because the words came straight back down at full speed
     * and anything above the halfway mark was left standing in the way.
     */
    static void pushBackRelief(Layout L) {
        group("panic swipe relief");

        // 1. Where it can start. The lit strip is the target; the catchment is much bigger.
        float insideStrip = (L.dangerY + L.deckTop) / 2f;
        check("the lit strip still starts it", L.inPushZone(L.w / 2f, insideStrip));
        check("so does the middle of the screen", L.inPushZone(L.w / 2f, L.h / 2f));
        check("and everything between", L.inPushZone(L.w / 2f, (L.h / 2f + L.dangerY) / 2f));
        check("but never on a key", !L.inPushZone(L.keyX[1], L.keyY[1])
                && !L.inPushZone(L.keyX[4], L.keyY[4]));
        check("nor above the middle", !L.inPushZone(L.w / 2f, L.h / 2f - 1f));
        check("nor in the HUD, which has its own tap",
                !L.inPushZone(L.w / 2f, L.hudY) && L.inStageTap(L.w / 2f, L.hudY));
        float wasStrip = L.deckTop - L.dangerY;
        System.out.printf("    swipe catchment is %.0fx the lit strip (%.0fpx of %.0f high)%n",
                (L.deckTop - L.h / 2f) / wasStrip, L.deckTop - L.h / 2f, L.h);
        check("it is a much bigger target than the strip",
                L.deckTop - L.h / 2f > wasStrip * 4f);

        // 2. The drag it leaves on the field.
        GameCore c = new GameCore(new Mem(), 171L);
        c.startGame();
        c.enemies.clear();
        check("no drag before it", c.fallRate() == 1f && c.pushSlowT == 0f);
        GameCore.Enemy e = add(c, L, new int[] {0, 1}, L.dangerY - L.enemyR * 1.2f);
        c.update(DT, L);
        check("the swipe fires", c.pushBack(L));
        check("the field is winded to a quarter speed",
                Math.abs(c.fallRate() - GameCore.PUSH_SLOW_RATE) < 0.02f);

        // Winding back up, every frame, and arriving at exactly full speed.
        boolean rising = true, capped = true;
        float prev = c.fallRate();
        for (float t = 0f; t < GameCore.PUSH_SLOW + 0.2f; t += DT) {
            c.update(DT, L);
            float now = c.fallRate();
            if (now < prev - 1e-4f) rising = false;
            if (now > 1.0001f) capped = false;
            prev = now;
        }
        check("it winds back up rather than snapping", rising);
        check("and never overshoots full speed", capped);
        check("full speed once it has worn off", c.fallRate() == 1f && c.pushSlowT == 0f);

        // A word really does fall slower for it, which is the whole point.
        GameCore f = new GameCore(new Mem(), 172L);
        f.startGame();
        f.enemies.clear();
        GameCore.Enemy fast = add(f, L, new int[] {0}, L.playTop + L.enemyR);
        fast.speed = L.enemyR * 6f;
        float y0 = fast.y;
        f.update(DT, L);
        float fullStep = fast.y - y0;
        // A threat low down to arm the swipe, then the same measurement while winded.
        GameCore.Enemy threat = add(f, L, new int[] {1}, L.dangerY - L.enemyR * 1.2f);
        f.update(DT, L);
        check("armed by the threat", f.pushReady() && threat != null);
        check("the swipe fires", f.pushBack(L));
        // The shoved word is sliding; a fresh one measures the fall rate cleanly.
        f.enemies.clear();
        GameCore.Enemy after = add(f, L, new int[] {2}, L.playTop + L.enemyR);
        after.speed = L.enemyR * 6f;
        float y1 = after.y;
        f.update(DT, L);
        float slowStep = after.y - y1;
        System.out.printf("    a word falls %.0f%% as far per frame right after the swipe%n",
                slowStep / fullStep * 100f);
        check("a word falls a quarter as far while winded",
                Math.abs(slowStep - fullStep * GameCore.PUSH_SLOW_RATE) < fullStep * 0.05f);

        // A run ending mid-drag must not leave the next one crawling.
        f.lives = 1;
        f.enemies.clear();
        add(f, L, new int[] {0, 0}, L.dangerY - L.enemyR + 1f);
        advance(f, L, GameCore.ATTACK_TIME + 2 * DT);
        check("the run ended", f.state == GameCore.OVER);
        f.startGame();
        check("a fresh run starts at full speed", f.fallRate() == 1f && f.pushSlowT == 0f);

        // 3. The collision: a shoved lower word smashes whatever it lands on.
        GameCore g = new GameCore(new Mem(), 173L);
        g.startGame();
        g.enemies.clear();
        float mid = (L.playTop + L.dangerY) / 2f;
        float lift = (L.dangerY - L.playTop) * GameCore.PUSH_LIFT;
        GameCore.Enemy low = add(g, L, new int[] {0}, L.dangerY - L.enemyR * 1.2f);
        GameCore.Enemy inTheWay = add(g, L, new int[] {1},
                Math.min(mid, low.y - lift) - L.enemyR * 0.4f);
        GameCore.Enemy bystander = add(g, L, new int[] {2}, mid - L.enemyR * 3f);
        check("the one in the way is above the halfway mark", inTheWay.y < mid);
        g.update(DT, L);
        check("the swipe fires", g.pushBack(L));
        check("the threat went up", low.slideT > 0f && low.slideTo < low.slideFrom);
        check("the word it would have landed on was smashed", inTheWay.destroyed);
        check("its pieces fly upward", inTheWay.radialFly && inTheWay.flyY[0] < -0.8f);
        check("the collision has a distinct impact animation", !g.pushImpacts.isEmpty());
        check("a word clear of all of them is left alone", bystander.slideT == 0f);
        System.out.printf("    shove moved %d and smashed one of 3%n", g.pushCount);
        check("the count reflects the surviving shove", g.pushCount == 1);

        // A packed ladder becomes a chain of visible break impacts.
        GameCore h = new GameCore(new Mem(), 174L);
        h.startGame();
        h.enemies.clear();
        int rungs = 10;
        for (int k = 0; k < rungs; k++) {
            add(h, L, new int[] {k % Glyph.COUNT}, L.dangerY - L.enemyR * (1.2f + 2f * k));
        }
        h.update(DT, L);
        check("the swipe fires on a packed field", h.pushBack(L));
        int moved = 0, smashed = 0;
        for (int k = 0; k < h.enemies.size(); k++) {
            if (h.enemies.get(k).slideT > 0f) moved++;
            if (h.enemies.get(k).destroyed) smashed++;
        }
        check("a packed field produces upward breakage",
                moved > 0 && smashed > 0 && h.pushImpacts.size() == smashed);
    }

    static void mashEarned(Layout L) {
        group("earning the mash");

        check("a perfect round earns the most", GameCore.MASH_PERFECT > GameCore.MASH_UNHURT);
        check("damage costs more than a stray press", GameCore.MASH_UNHURT > GameCore.MASH_HURT);
        check("and the panic swipe costs the most of all",
                GameCore.MASH_PANIC < GameCore.MASH_HURT);

        // Perfect: nothing pressed wrongly, nothing lost, no swipe.
        GameCore p = clearedRound(151L, L);
        check("a clean round earns the full mash", p.earnedMash == GameCore.MASH_PERFECT);
        check("and the clock shows it", Math.abs(p.bonusLeft() - GameCore.MASH_PERFECT) < 0.01f);

        // A stray press, but nothing lost.
        GameCore u = new GameCore(new Mem(), 152L);
        u.startGame();
        u.enemies.clear();
        u.tapKey(0, L);                        // nothing on the field: a miss
        check("that was a miss", u.missesThisStage == 1 && u.hurtThisStage == 0);
        u.spawnedThisStage = u.stageQuota();
        u.enemies.clear();
        u.shots.clear();
        check("reached the interlude", advanceToBonus(u, L));
        check("a stray press costs a second", u.earnedMash == GameCore.MASH_UNHURT);

        // A life lost outranks a stray press: it is the same round, one tier down.
        GameCore h = new GameCore(new Mem(), 153L);
        h.startGame();
        h.enemies.clear();
        add(h, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(h, L, GameCore.ATTACK_TIME + 2 * DT);
        check("a life went", h.hurtThisStage == 1 && h.missesThisStage == 0);
        // Read before the wave is cleared: beginStageEnd zeroes both counters on its way out, so by
        // the time the interlude is open every round looks perfect again.
        check("a round that lost a life is not perfect", !h.perfectRound());
        h.spawnedThisStage = h.stageQuota();
        h.enemies.clear();
        h.shots.clear();
        check("reached the interlude", advanceToBonus(h, L));
        check("losing a life costs two", h.earnedMash == GameCore.MASH_HURT);
        // And the same round is not perfect for the dumpling either. There is one definition of
        // perfect: this used to award the gold dumpling on a round that lost a life, because no
        // *press* had been wrong.
        check("and it earned no gold dumpling either", h.perfectBanner == 0f);

        // The panic swipe overrides an otherwise perfect round.
        GameCore k = new GameCore(new Mem(), 154L);
        k.startGame();
        k.enemies.clear();
        GameCore.Enemy threat = add(k, L, new int[] {0, 1}, L.dangerY - L.enemyR * 1.2f);
        k.update(DT, L);
        check("the swipe is offered", k.pushReady());
        check("the swipe fires", k.pushBack(L));
        check("it was used", k.pushUsed && threat != null);
        check("and nothing was lost by it",
                k.hurtThisStage == 0 && k.missesThisStage == 0);
        k.enemies.clear();
        k.spawnedThisStage = k.stageQuota();
        k.shots.clear();
        check("reached the interlude", advanceToBonus(k, L));
        check("the panic swipe drops it to the floor", k.earnedMash == GameCore.MASH_PANIC);

        // Every tier has to be spendable: a mash shorter than a pair of presses would be a phase
        // the player cannot act in at all.
        check("even the floor is long enough to press in", GameCore.MASH_PANIC >= 0.5f);
        // The clock can only ever show a number the ladder has a tier for. Swept over real play
        // rather than reasoned about, because the last thing to break this was a modifier added
        // somewhere else entirely — the frenzy's 2.6s, which opened the countdown on "8".
        GameCore w = new GameCore(new Mem(), 159L);
        w.startGame();
        int highest = 0;
        for (int i = 0; i < 60 * 400 && w.state != GameCore.OVER; i++) {
            if (w.state == GameCore.BONUS) {
                highest = Math.max(highest, (int) Math.ceil(w.bonusLeft()));
                if (i % 12 == 0) w.tapBonus(w.steamer.wanted());
            } else if (w.state == GameCore.PLAY && i % 10 == 0) {
                GameCore.Enemy e = urgent(w);
                if (e != null && e.typeable()) w.tapKey(e.word[e.pos], L);
            }
            w.update(DT, L);
        }
        System.out.printf("    over a whole run the clock never read above %d%n", highest);
        check("the clock never shows a number the ladder has no tier for",
                highest <= (int) Math.ceil(GameCore.MASH_PERFECT));
        check("and it does reach the top tier", highest == (int) GameCore.MASH_PERFECT);

        // Nothing adds to the ladder any more: a frenzy used to buy 2.6s on top, which was more
        // than the whole spread and made the tiers unreadable.
        GameCore z = clearedRound(158L, L);
        check("a perfect round's interlude is exactly the ladder",
                Math.abs(z.bonusTimer - GameCore.bonusLength(GameCore.MASH_PERFECT)) < 0.01f);
        System.out.printf("    mash earned: perfect %.0fs, unhurt %.0fs, hurt %.0fs, panic %.0fs%n",
                GameCore.MASH_PERFECT, GameCore.MASH_UNHURT, GameCore.MASH_HURT,
                GameCore.MASH_PANIC);
    }

    /** A wave cleared with nothing pressed wrongly, nothing lost and no swipe used. */
    private static GameCore clearedRound(long seed, Layout L) {
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        advanceToBonus(c, L);
        return c;
    }

    static void steamerBonus(Layout L) {
        group("between-stages minigame");
        Mem persistent = new Mem();
        GameCore c = new GameCore(persistent, 121L);
        c.startGame();
        check("no steamer damage at the start", c.steamer.hits == 0 && c.steamer.opens == 0);
        check("the first steamer target is ten", c.steamer.goal() == 10);
        Steamer capped = new Steamer();
        capped.reset();
        for (int expected : new int[] {10, 12, 14, 16, 18, 20, 20, 20}) {
            check("steamer target rises only to twenty", capped.goal() == expected);
            for (int hit = 0; hit < expected; hit++) {
                capped.press(capped.leftKey); capped.press(capped.rightKey);
            }
            check("the capped target still arms and frees the lid", capped.swipeReady
                    && capped.hits == expected && capped.swipe() == Steamer.FREED);
            capped.update(Steamer.FREE_TIME);
        }
        check("successes keep counting after reaching the cap", capped.opens == 8);
        Mem cappedSave = new Mem(); cappedSave.steamerOpens = Integer.MAX_VALUE;
        Steamer restoredCap = new GameCore(cappedSave, 123L).steamer;
        check("old high-difficulty saves are capped immediately", restoredCap.goal() == 20);
        restoredCap.swipeReady = true; restoredCap.swipe();
        check("large lifetime totals cannot overflow and reset difficulty",
                restoredCap.opens == Integer.MAX_VALUE && restoredCap.goal() == 20);


        // Clearing a wave drops into the minigame, not straight into the next stage.
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);
        check("clearing a wave enters the minigame", advanceToMash(c, L));
        check("the stage has not turned over yet", c.stage == 1);
        check("the lid starts shut", c.steamer.lidOpen() == 0f);

        int hitsBefore = c.hits, missesBefore = c.misses;

        // Only the chosen pair counts, and only in alternation.
        int lk = c.steamer.leftKey, rk = c.steamer.rightKey;
        check("the pair is one key per thumb",
                lk < Glyph.COUNT / 2 && rk >= Glyph.COUNT / 2);
        check("it wants the left one first", c.steamer.expectLeft && c.steamer.wanted() == lk);

        c.tapBonus(rk);
        check("the right key out of turn scores nothing", c.steamer.hits == 0);
        check("and restarts the pair", c.steamer.expectLeft);
        c.tapBonus(lk);
        check("the left key alone scores nothing yet", c.steamer.hits == 0);
        check("but it now wants the right one", !c.steamer.expectLeft);
        c.tapBonus(rk);
        check("a completed pair scores one", c.steamer.hits == 1);
        check("and it wants the left one again", c.steamer.expectLeft);

        int other = (lk + 1) % (Glyph.COUNT / 2);
        c.tapBonus(other);
        check("a key outside the pair scores nothing", c.steamer.hits == 1);
        for (int i = 0; i < 3; i++) { c.tapBonus(lk); c.tapBonus(rk); }
        check("three more pairs score three", c.steamer.hits == 4);
        check("a press pops the lid", c.steamer.lidPulse > 0f);
        check("a press flashes the container", c.steamer.flash > 0f);
        check("the lid is partway open", c.steamer.lidOpen() > 0f && c.steamer.lidOpen() < 1f);
        // The minigame must not pollute the accuracy readout.
        check("the minigame is not counted as typing",
                c.hits == hitsBefore && c.misses == missesBefore);
        for (int g = 0; g < Glyph.COUNT; g++) c.tapBonus(g);
        check("a press pops the lid", c.steamer.lidPulse > 0f);
        check("a press flashes the container", c.steamer.flash > 0f);
        check("the lid is partway open", c.steamer.lidOpen() > 0f && c.steamer.lidOpen() < 1f);
        // Mashing must not pollute the accuracy readout.
        check("mashing is not counted as typing",
                c.hits == hitsBefore && c.misses == missesBefore);

        advance(c, L, 1.0f);
        check("the press animations settle", c.steamer.lidPulse == 0f && c.steamer.flash == 0f);

        // A wrong press must look and sound nothing like a landed one, and above all must not
        // move the lid — the two used to share every channel except the sound.
        GameCore wp = new GameCore(new Mem(), 127L);
        Ear wear = new Ear();
        wp.sound = wear;
        wp.startGame();
        check("in the minigame", toMash(wp, L));
        int wrongKey = -1;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (g != wp.steamer.leftKey && g != wp.steamer.rightKey) wrongKey = g;
        }
        advance(wp, L, 0.6f);                        // let any opening pulse settle
        check("nothing pulsing to start with",
                wp.steamer.lidPulse == 0f && wp.steamer.flash == 0f
                        && wp.steamer.badPulse == 0f);
        int hitsWas = wp.steamer.hits;
        float openWas = wp.steamer.lidOpen();
        int squishWas = wear.squishes, wrongWas = wear.wrongs;

        wp.tapBonus(wrongKey);
        check("a wrong press does not budge the lid",
                wp.steamer.lidPulse == 0f && wp.steamer.lidOpen() == openWas);
        check("and does not flash the basket", wp.steamer.flash == 0f);
        check("it rebuffs instead", wp.steamer.badPulse == 1f);
        check("no progress from it", wp.steamer.hits == hitsWas);
        check("it sounds wrong", wear.wrongs == wrongWas + 1);
        check("and not like a press", wear.squishes == squishWas);
        check("the key itself reddens", wp.keyBad[wrongKey] > 0f);
        check("but it is not a typing miss", wp.misses == 0);

        // The rebuff fades on its own.
        advance(wp, L, 0.5f);
        check("the rebuff fades", wp.steamer.badPulse == 0f);

        // And a right press still does all the things a right press did.
        wp.tapBonus(wp.steamer.wanted());
        check("a right press pulses the lid", wp.steamer.lidPulse == 1f);
        check("and flashes the basket", wp.steamer.flash == 1f);
        check("with no rebuff", wp.steamer.badPulse == 0f);
        check("and sounds like a press", wear.squishes == squishWas + 1);

        // Damage carries over: run the interlude out and check the count survives.
        int carried = c.steamer.hits;
        advancePastBonus(c, L);
        check("the minigame ends by itself", c.state == GameCore.PLAY);
        check("the stage turns over on the way out", c.stage == 2);
        check("steamer damage carries across the interlude", c.steamer.hits == carried);
        check("presses outside the minigame are ignored by it", c.steamer.hits == carried);
        c.tapBonus(0);
        check("tapBonus does nothing during play", c.steamer.hits == carried);

        // Second interlude: finish the job and check the reward. The post-interlude breather
        // has to run out first, or the wave gate is never reached.
        advance(c, L, GameCore.STAGE_GAP + 0.1f);
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);
        check("back in the minigame", advanceToMash(c, L));
        c.lives = GameCore.START_LIVES - 1;
        int scoreBefore = c.score;
        // Bounded: tapBonus is a no-op outside BONUS, so an unbounded loop would hang.
        // Two presses per point, so twice the pairs, and bounded in case it stalls.
        for (int i = 0; i <= c.steamer.goal() * 3 && c.steamer.hits > 0; i++) {
            c.tapBonus(c.steamer.wanted());
        }
        check("the final point arms the lid swipe", c.bonusSwipeReady()
                && c.steamer.hits == c.steamer.goal() && c.steamer.opens == 0);
        c.swipeBonus();
        check("swiping the armed lid frees the dumpling", c.steamer.opens == 1);
        check("a success raises the next target by two", c.steamer.goal() == 12);
        check("the higher target is saved immediately",
                persistent.steamerOpens == 1 && persistent.steamerOpenSaves == 1);
        GameCore reloaded = new GameCore(persistent, 121L);
        reloaded.startGame();
        check("steamer difficulty survives a new playthrough and reload",
                reloaded.steamer.opens == 1 && reloaded.steamer.goal() == 12);
        reloaded.steamer.hits = 5;
        reloaded.resetDifficultyScaling();
        check("settings restore the initial steamer difficulty and progress",
                reloaded.steamer.opens == 0 && reloaded.steamer.hits == 0
                        && reloaded.steamer.goal() == 10);
        check("the difficulty reset is persistent",
                persistent.steamerOpens == 0 && persistent.steamerOpenSaves == 2
                        && new GameCore(persistent, 122L).steamer.goal() == 10);
        check("the counter resets so it can be earned again", c.steamer.hits == 0);
        check("freeing it scores", c.score == scoreBefore + GameCore.FREE_BONUS);
        check("freeing it returns a lost life", c.lives == GameCore.START_LIVES);
        check("the escape animation runs", c.steamer.freedT > 0f);
        check("the interlude is held open for it", c.bonusTimer >= c.steamer.freedT);

        int opensNow = c.steamer.opens;
        c.tapBonus(3);
        check("presses during the escape do not re-trigger", c.steamer.opens == opensNow);

        advance(c, L, GameCore.bonusLength(GameCore.MASH_PERFECT)
                + GameCore.PARADE_TIME + 2f);
        check("play resumes after the celebration and the parade", c.state == GameCore.PLAY);
        check("lives are capped at the starting count", c.lives <= GameCore.START_LIVES);

        // Missing the physical finish does not bank a permanently open lid. It returns one
        // point short, then exactly one fresh pair rearms the swipe on the next visit.
        GameCore miss = new GameCore(new Mem(), 129L);
        miss.startGame();
        check("reached a steamer visit for the missed-swipe case", toMash(miss, L));
        miss.steamer.hits = miss.steamer.goal() - 1;
        miss.tapBonus(miss.steamer.wanted());
        miss.tapBonus(miss.steamer.wanted());
        check("the last point waits for a swipe instead of awarding", miss.bonusSwipeReady()
                && miss.steamer.opens == 0 && miss.prize < 0);
        int armedHits = miss.steamer.hits;
        miss.steamer.flash = miss.steamer.lidFlash = 0f;
        miss.tapBonus(0);
        check("key presses while a drag is wanted flash only the lid",
                miss.steamer.lidFlash == 1f && miss.steamer.flash == 0f
                        && miss.steamer.hits == armedHits && miss.bonusSwipeReady());
        check("the reminder also gives the lid a small scale pop",
                miss.steamer.lidKeyScale() > 1.05f && miss.steamer.lidKeyScale() < 1.08f);
        boolean lidTarget = false;
        for (float y = L.playTop; y < L.dangerY; y += L.unit * 0.25f) {
            if (Screens.inSteamerLid(miss, L, L.w / 2f, y)) lidTarget = true;
        }
        check("the armed lid exposes a wide swipe target", lidTarget
                && !Screens.inSteamerLid(miss, L, L.playLeft, L.dangerY));
        miss.dragBonusLid(L.unit * 0.8f);
        check("the armed lid follows an upward drag",
                Math.abs(miss.steamer.lidDrag - L.unit * 0.8f) < 0.01f);
        miss.dragBonusLid(-L.unit);
        check("a downward drag does not push the lid into the basket", miss.steamer.lidDrag == 0f);
        float lidRestY = Screens.steamerLidY(miss, L);
        miss.dragBonusLid(lidRestY - Screens.steamerReleaseY(miss, L) - L.unit * 0.1f);
        check("a short drag does not release just below the threshold",
                Screens.steamerLidY(miss, L) > Screens.steamerReleaseY(miss, L));
        miss.dragBonusLid(lidRestY - Screens.steamerReleaseY(miss, L) + L.unit * 0.1f);
        check("the longer lid-relative threshold releases it",
                Screens.steamerLidY(miss, L) < Screens.steamerReleaseY(miss, L));
        miss.dragBonusLid(0f);
        advance(miss, L, miss.bonusTimer - (GameCore.BONUS_HOLD + GameCore.BONUS_STATUS)
                + 2f * DT);
        check("time expiring relocks the missed swipe one point short",
                !miss.steamer.swipeReady && miss.steamer.hits == miss.steamer.goal() - 1
                        && miss.steamer.opens == 0);
        advancePastBonus(miss, L);
        advance(miss, L, GameCore.STAGE_GAP + 0.1f);
        miss.spawnedThisStage = miss.stageQuota();
        miss.enemies.clear();
        miss.shots.clear();
        check("the next steamer visit opens", advanceToMash(miss, L));
        check("the missed lid still needs one point",
                miss.steamer.hits == miss.steamer.goal() - 1 && !miss.bonusSwipeReady());
        miss.tapBonus(miss.steamer.wanted());
        miss.tapBonus(miss.steamer.wanted());
        check("one point rearms the swipe on the next visit", miss.bonusSwipeReady());
        miss.dragBonusLid(L.unit * 1.8f);
        miss.swipeBonus();
        check("that swipe frees the dumpling", miss.steamer.opens == 1);
        check("the released lid retains the completed drag", miss.steamer.freedLidLift > L.unit * 1.7f);
        float releasedY = Screens.freedLidY(miss, L);
        miss.steamer.update(Steamer.FREE_TIME * 0.98f);
        check("the released lid accelerates fully off screen",
                Screens.freedLidY(miss, L) < 0f && Screens.freedLidY(miss, L) < releasedY);

        // A full run must be able to reach the minigame repeatedly without wedging.
        GameCore r = new GameCore(new Mem(), 122L);
        r.startGame();
        int bonuses = 0, frames = 0;
        boolean sane = true;
        while (frames < 60 * 240 && r.state != GameCore.OVER) {
            int was = r.state;
            r.update(DT, L);
            frames++;
            if (was != GameCore.BONUS && r.state == GameCore.BONUS) bonuses++;
            if (r.state == GameCore.BONUS) {
                // Human-ish mash rate, so the reported free count means something.
                if (frames % 8 == 0) r.tapBonus(r.steamer.wanted());
                if (r.bonusSwipeReady()) r.swipeBonus();
                continue;
            }
            if (r.state != GameCore.PLAY) continue;
            if (frames % 2 != 0) continue;
            GameCore.Enemy e = r.target != null && r.enemies.contains(r.target)
                    && r.target.typeable() ? r.target : urgent(r);
            if (e != null && e.pos < e.word.length) r.tapKey(e.word[e.pos], L);
            if (r.steamer.hits < 0 || r.steamer.hits > r.steamer.goal()) sane = false;
        }
        System.out.printf("    %d interludes in %.0fs, freed %d, stage %d%n",
                bonuses, frames * DT, r.steamer.opens, r.stage);
        check("interludes recur across a run", bonuses >= 3);
        check("steamer damage stays in range", sane);
        check("the dumpling gets freed during a long run", r.steamer.opens >= 1);
    }

    static void accuracyTracking(Layout L) {
        group("accuracy");
        GameCore c = new GameCore(new Mem(), 61L);
        c.startGame();
        check("accuracy starts at 0%", c.accuracyPercent() == 0);
        check("no presses means the saddest face", c.accuracyMood() == 0f);
        GameCore idle = new GameCore(new Mem(), 62L);
        idle.startGame(); idle.lives = 1; idle.takeHit(L.w * .5f, L);
        check("zero-press run ends with 0% accuracy", idle.state == GameCore.OVER
                && idle.hits == 0 && idle.misses == 0 && idle.accuracyPercent() == 0);
        idle.misses = 5;
        check("miss-only run has 0% accuracy", idle.accuracyPercent() == 0);

        c.enemies.clear();
        c.target = null;
        add(c, L, new int[] {0, 1, 2, 3}, L.playTop + 90);
        c.tapKey(0, L);
        check("a correct press counts as a hit", c.hits == 1 && c.misses == 0);
        check("a hit without misses earns 100% accuracy", c.accuracyPercent() == 100);
        c.tapKey(5, L);
        check("a wrong press counts as a miss", c.hits == 1 && c.misses == 1);
        check("accuracy halves at one for one", c.accuracyPercent() == 50);
        check("50% is the saddest face", c.accuracyMood() == 0f);

        // Push to 75%: three hits, one miss.
        c.tapKey(0, L);
        c.tapKey(1, L);
        check("accuracy climbs with hits", c.accuracyPercent() == 75);
        check("75% sits between the extremes",
                c.accuracyMood() > 0f && c.accuracyMood() < 1f);

        // Threshold checks, driven directly.
        c.hits = 60; c.misses = 40;
        check("60% is the saddest face", c.accuracyPercent() == 60 && c.accuracyMood() == 0f);
        c.hits = 59; c.misses = 41;
        check("below 60% stays saddest", c.accuracyMood() == 0f);
        c.hits = 90; c.misses = 10;
        check("90% is the happiest face", c.accuracyPercent() == 90 && c.accuracyMood() == 1f);
        c.hits = 97; c.misses = 3;
        check("above 90% stays happiest", c.accuracyMood() == 1f);
        c.hits = 75; c.misses = 25;
        check("75% maps to the midpoint", Math.abs(c.accuracyMood() - 0.5f) < 0.001f);

        // A flawless wave earns the gold dumpling; a single miss forfeits it.
        GameCore g = new GameCore(new Mem(), 62L);
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        // The reward now fires the moment the wave clears, ahead of the interlude.
        check("a flawless wave triggers the gold dumpling", g.perfectBanner > 0f);
        check("the celebration expires", g.perfectBanner <= GameCore.PERFECT_TIME);
        advance(g, L, GameCore.PERFECT_TIME + 0.2f);
        check("the celebration ends", g.perfectBanner == 0f);
        check("the celebration ends", g.perfectBanner == 0f);

        GameCore m = new GameCore(new Mem(), 63L);
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);   // nothing to hit: a miss
        check("the miss is recorded against the stage", m.missesThisStage == 1);
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        advancePastBonus(m, L);
        check("a wave with a miss earns no gold dumpling", m.perfectBanner == 0f);
        check("stage misses reset for the next wave", m.missesThisStage == 0);
        check("run totals are not reset by the stage", m.misses == 1);
    }

    static void warningsAndHarm(Layout L) {
        group("warnings and harm");
        GameCore c = new GameCore(new Mem(), 21L);
        c.startGame();
        c.enemies.clear();

        GameCore.Enemy far = add(c, L, new int[] {0, 0}, L.playTop + 20);
        c.update(DT, L);
        check("a distant word raises no alarm", far.warn == 0f && c.warnLevel == 0f);

        c.enemies.clear();
        float band = (L.dangerY - L.playTop) * 0.20f;
        GameCore.Enemy near = add(c, L, new int[] {0, 0}, L.dangerY - band * 0.4f);
        c.update(DT, L);
        check("a closing word raises the alarm", near.warn > 0.5f && near.warn <= 1f);
        check("warnLevel follows the worst offender", c.warnLevel == near.warn);
        check("a closing word is still typeable", near.typeable());

        check("full health means no red tint", c.harm() == 0f);
        c.lives = 2;
        float h2 = c.harm();
        c.lives = 1;
        check("harm rises as lives fall", c.harm() > h2 && h2 > 0f);
        check("harm peaks below or at 1", c.harm() <= 1f);
        // It has to bite late. One life lost out of three is the ordinary state of a run in
        // progress, and at linear strength it painted a permanent red border for the rest of it.
        check("one life lost is barely visible", h2 < 0.15f);
        check("the last life is unmistakable", c.harm() > 0.35f);
        c.lives = 0;
        check("and empty is full strength", Math.abs(c.harm() - 1f) < 0.001f);
        c.lives = 1;
        c.lives = 0;
        c.state = GameCore.OVER;
        check("no tint outside play", c.harm() == 0f);

        c.deathT = 0f;
        check("death tint fills the settled game-over screen", c.drained() == 1f);
        c.toTitle();
        check("death tint clears on the title after game over", c.drained() == 0f);

        for (int i = 0; i < 6; i++) {
            check("cycle colour " + i + " is opaque", (Glyph.cycle(i / 6f) >>> 24) == 0xFF);
        }
        check("cycle wraps", Glyph.cycle(0f) == Glyph.cycle(1f));
    }

}
