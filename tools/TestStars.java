package com.dddumpling.game;

/** Alternation, steering, persistence and rewards for the star-path interlude. */
final class TestStars extends Check {
    private static void interruptedWin(Layout L, boolean duringParade) {
        Mem store = new Mem();
        GameCore c = new GameCore(store, 19L);
        c.startGame();
        c.starNext = true;
        Interlude.enterBonus(c, L);
        int last = StarPath.COUNT - 1;
        c.stars.collected = (1 << last) - 1;
        for (int i = 0; i < 60 * 12 && !c.stars.won; i++) {
            c.stars.x = c.stars.starX(last, L);
            c.update(DT, L);
        }
        check("interruption fixture earns its final star", c.stars.won && store.starWins == 1);
        if (duringParade)
            for (int i = 0; i < 60 * 10 && !c.bonusParading(); i++) c.update(DT, L);
        check("quit starts during the intended celebration",
                duringParade ? c.bonusParading() : c.stars.winning());
        int prizes = c.collectTotal;
        Pause.open(c);
        Pause.action(c, 2);
        Pause.action(c, 2);
        c.startGame();
        check("quitting a completed course keeps its prize and difficulty",
                c.collectTotal == prizes && c.stars.wins == 1 && store.starWinSaves == 1);
        check("the next run retires the completed course",
                !c.starNext && c.stars.count() == 0);
        Interlude.enterBonus(c, L);
        check("the next minigame returns to the steamer", !c.starBonus);
        c.starNext = true;
        Interlude.enterBonus(c, L);
        check("the next star course has twenty stars to collect",
                c.starBonus && c.stars.count() == 0 && !c.stars.won);
        check("entering the recovered course never pays the old reward twice",
                c.collectTotal == prizes && c.stars.wins == 1 && store.starWinSaves == 1);
    }

    private static void selectedPilot(Layout L) {
        Mem store = new Mem();
        store.collected = (1L << 2) | (1L << 7);
        GameCore c = new GameCore(store, 173L);
        c.caseIndex = 7;
        c.beginStart();
        advance(c, L, Launch.TIME + GameCore.START_FADE + DT);
        check("pilot fixture starts through the title send-off", c.state == GameCore.PLAY);
        c.starNext = true;
        Interlude.enterBonus(c, L);
        check("Star Path uses the title selection without a prize", c.prize == -1 && c.stars.who == 7);
        c.stars.collected = 7;
        Interlude.awardBossPrize(c, 0);
        c.bossPrizePending = false;
        Interlude.enterBonus(c, L);
        check("a new boss collectible changes the case but not the pilot",
                c.caseIndex == Collect.BOSS_FIRST && c.stars.who == 7 && c.stars.count() == 3);
        Interlude.awardPrize(c);
        Interlude.enterBonus(c, L);
        check("a steamer award never replaces the run's pilot", c.stars.who == 7);
        Interlude.awardStarPrize(c);
        check("the victory tableau keeps its pilot when the star prize arrives", c.stars.who == 7);
        Interlude.enterBonus(c, L);
        check("the next course keeps the title selection after a star award", c.stars.who == 7);
        c.state = GameCore.TITLE;
        c.caseIndex = 2;
        c.beginStart();
        advance(c, L, Launch.TIME + GameCore.START_FADE + DT);
        Interlude.enterBonus(c, L);
        check("unfinished stars resume with the next run's selected character",
                c.starBonus && c.prize == -1 && c.stars.who == 2 && c.stars.count() == 3);
        if (BuildFlags.DEVELOPER) {
            c.state = GameCore.PLAY;
            c.playtestStars(L);
            check("the Star Path test chip uses the same pilot without inventing a prize",
                    c.stars.who == 2 && c.prize == -1);
        }
        GameCore fresh = new GameCore(new Mem(), 175L);
        fresh.startGame();
        fresh.starNext = true;
        Interlude.enterBonus(fresh, L);
        check("an empty collection still flies a dumpling instead of a blank circle", fresh.stars.who == 0);
    }

    static void game(Layout L) {
        group("star path minigame");
        selectedPilot(L);
        interruptedWin(L, false);
        interruptedWin(L, true);
        StarPath full = new StarPath();
        full.collected = (1 << StarPath.COUNT) - 1;
        full.reroll(new java.util.Random(91L));
        check("a stale completed course never rerolls without stars", full.count() == 0);
        Mem staleStore = new Mem();
        staleStore.starWins = 3;
        GameCore stale = new GameCore(staleStore, 93L);
        stale.starNext = true;
        stale.stars.collected = (1 << StarPath.COUNT) - 1;
        stale.stars.won = false;
        int prizesBefore = stale.collectTotal;
        stale.startGame();
        check("a stale full hand recovers even after its win flag was lost",
                stale.stars.count() == 0 && !stale.starNext);
        check("recovery neither repays nor advances a saved difficulty",
                stale.collectTotal == prizesBefore && stale.stars.wins == 3
                        && staleStore.starWinSaves == 0);
        stale.starNext = true;
        stale.stars.collected = 7;
        stale.startGame();
        check("new runs keep unfinished stars and their pending Star Path turn",
                stale.stars.count() == 3 && stale.starNext);
        StarPath q = new StarPath();
        q.make(new java.util.Random(7));
        q.begin(3, L);
        check("starts with a visual ready beat", q.ready());
        check("course has twenty stars", q.sx.length == 20);
        check("the Starpath slider spans the deck and is directly hittable",
                StarScreen.sliderLeft(L) < L.w * 0.25f
                        && StarScreen.sliderRight(L) > L.w * 0.75f
                        && StarScreen.inSlider(L, L.w * 0.5f, StarScreen.sliderY(L))
                        && !StarScreen.inSlider(L, L.w * 0.5f, L.dangerY));
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - StarPath.FLY * 0.25f;
        float early = q.traversalProgress();
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - StarPath.FLY * 0.75f;
        float lateStep = q.traversalProgress() - early;
        check("the course accelerates over the flight", early < 0.25f && lateStep > 0.50f);

        // The soft start, in both directions. A flight that began at speed made the whole course
        // appear to be shoved down the screen on its first frame; the ramp is normalised so it
        // still finishes exactly on time, and encounterTime inverts it so the sweep is still
        // sampled at the moment each star actually arrives.
        check("the course leaves from a standstill", StarPath.launch(0f) == 0f
                && StarPath.launch(0.004f) < 0.004f * 0.1f);
        check("and is up to pace by the end of the ease-in",
                StarPath.launch(StarPath.EASE_IN / StarPath.FLY) > 0f
                        && StarPath.launch(StarPath.EASE_IN / StarPath.FLY)
                                < StarPath.EASE_IN / StarPath.FLY);
        check("and still covers the whole course in the flight",
                Math.abs(StarPath.launch(1f) - 1f) < 1e-4f);
        boolean roundTrip = true;
        for (int k = 0; k <= 20; k++) {
            float p = k / 20f;
            if (Math.abs(StarPath.unlaunch(StarPath.launch(p)) - p) > 0.002f) roundTrip = false;
        }
        check("the ease-in inverts, so the generator and the scroll agree", roundTrip);
        // Rate rising the whole way through the ramp, so it reads as a launch rather than a lurch.
        boolean rising = true;
        float step = StarPath.EASE_IN / StarPath.FLY / 8f;
        float slower = 0f;
        for (int k = 1; k <= 8; k++) {
            float rate = StarPath.launch(k * step) - StarPath.launch((k - 1) * step);
            if (rate <= slower) rising = false;
            slower = rate;
        }
        check("the ease-in accelerates all the way through", rising);

        // The ready lesson has to come to a stop on the spot the flight leaves from. It used to be
        // a bare sine on the drawn position, so the first frame of the flight snapped the flyer
        // back to the middle from wherever the swing had reached.
        q.begin(3, L);
        check("the lesson leans during the ready beat",
                Math.abs(q.lessonLean(0.46f)) > 0.02f && q.lessonFade() == 1f);
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT + 0.02f;
        check("and has settled to a stop by the time the course moves",
                Math.abs(q.lessonLean(0.46f)) < 0.01f && q.lessonFade() < 0.01f);
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.02f;
        check("with nothing left of it once the flight starts",
                q.lessonLean(0.46f) == 0f && q.lessonFade() == 0f);
        float shakeX = q.flightShakeX(L), shakeY = q.flightShakeY(L);
        check("flight vibration moves the drawn flyer and its hitbox together",
                q.flyerX(L) == q.x + shakeX
                        && q.flyerY(L) == q.characterY(L) + shakeY);

        q.collected = 0b10101;
        q.begin(3, L);
        float readyTarget = L.playRight - StarPath.flyerR(L);
        q.beginDrag();
        q.dragTo(readyTarget, L);
        check("a drag begun during READY positions the flyer for launch",
                q.dragging && Math.abs(q.x - readyTarget) < 0.01f);
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.01f;
        q.dragTo(L.playLeft, L);
        check("the same drag continues once flight begins",
                q.dragging && q.x == L.playLeft + StarPath.flyerR(L));
        q.endDrag();
        check("the drag glow state ends on release", !q.dragging);
        q.timer = StarPath.REPORT + StarPath.EXIT * 0.5f;
        check("the blast-off phase exposes increasing exit progress",
                q.exiting() && q.exitProgress() > 0.49f && q.exitProgress() < 0.51f);

        q.begin(3, L);
        check("failed-run stars persist when the course restarts", q.count() == 3);
        float wasAt = q.sx[7];
        q.reroll(new java.util.Random(31L));
        check("and a failed attempt is a new line, not the same one back",
                q.count() == 3 && q.sx[7] != wasAt);
        check("pickup animation resets between attempts", q.burst[0] == 0f);
        check("the requested character pilots the course", q.who == 3);

        Mem progress = new Mem();
        GameCore c = new GameCore(progress, 19L);
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.starNext = true;
        c.stars.begin(-1, L);
        // Nineteen already in hand, flying, and parked on the twentieth: the last pickup is what
        // ends a course, so the win has to be earned here rather than declared.
        c.stars.collected = (1 << (StarPath.COUNT - 1)) - 1;
        c.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.5f;
        int last = StarPath.COUNT - 1;
        c.stars.x = c.stars.starX(last, L);
        int score = c.score;
        // Walked to the last star rather than teleported onto it: only starY knows where the
        // course has scrolled to, and it moves with the clock.
        boolean took = false;
        for (int i = 0; i < 60 * 6 && !took; i++) {
            c.stars.x = c.stars.starX(last, L);
            c.update(DT, L);
            took = c.stars.won;
        }
        check("taking the last star completes the course", took && c.stars.count() == StarPath.COUNT);
        check("the winning pickup emits feedback", c.starPickups == 1);
        check("success immediately saves one difficulty step",
                c.stars.wins == 1 && progress.starWins == 1 && progress.starWinSaves == 1);
        check("the victory tableau takes the screen", c.stars.winning() && c.starFlight());
        check("it knows which star finished the course", c.stars.winStar == last);
        check("completion pays like the steamer", c.score >= score + GameCore.FREE_BONUS);
        check("completion awards a star-exclusive collectible",
                c.prize >= Collect.STAR_FIRST && c.prize < Collect.COUNT);
        check("and the prize is paid before the tableau draws it", c.stars.winning());
        check("a parade is scheduled, as on a won steamer", c.paradeTimer > 0f);

        // Frozen: the course, the flyer and the clock all hold for the length of the tableau.
        float held = c.stars.timer, atX = c.stars.x;
        c.stars.dragTo(L.playLeft, L);
        c.update(DT, L);
        check("the tableau never repeats pickup feedback", c.starPickups == 0);
        check("the course stops dead", c.stars.timer == held && c.stars.x == atX
                && c.stars.vx == 0f);
        check("the tableau runs on its own clock", c.stars.winProgress() > 0f);
        check("no parade until the tableau is done", !c.bonusParading());

        int guard = 0;
        for (; guard < 60 * 10 && !c.bonusParading(); guard++) c.update(DT, L);
        check("the tableau hands over to the parade", c.bonusParading()
                && !c.stars.winning() && c.starFlight() == false);
        for (guard = 0; guard < 60 * 20 && c.state == GameCore.BONUS; guard++) c.update(DT, L);
        check("twenty stars complete the game", !c.starNext && c.state == GameCore.PLAY);
        check("a successful course resets its persistent stars", c.stars.count() == 0);
        check("the tableau and parade do not count a win twice", progress.starWinSaves == 1);
        c.startGame();
        GameCore restored = new GameCore(progress, 20L);
        check("Starpath difficulty survives new playthroughs and reloads",
                c.stars.wins == 1 && restored.stars.wins == 1);
        restored.stars.collected = 7;
        restored.resetDifficultyScaling();
        check("settings reset the stored Starpath ladder while keeping earned stars",
                restored.stars.wins == 0 && restored.stars.collected == 7
                        && new GameCore(progress, 21L).stars.wins == 0);


        // Exactly one phase names the screen on every frame of a star course, the same guarantee
        // the steamer path has: none of the steamer's phases may come true underneath one.
        GameCore f = new GameCore(new Mem(), 23L);
        f.startGame();
        f.state = GameCore.BONUS;
        f.starBonus = true;
        f.stars.make(new java.util.Random(23L));
        f.stars.begin(2, L);
        f.bonusTimer = f.stars.timer;
        boolean single = true;
        for (int i = 0; i < 60 * 15 && f.state == GameCore.BONUS; i++) {
            int on = 0;
            if (f.bonusRolling()) on++;
            if (f.bonusMashing()) on++;
            if (f.bonusHolding()) on++;
            if (f.bonusStatus()) on++;
            if (f.bonusEscape()) on++;
            if (f.bonusParading()) on++;
            if (f.starFlight()) on++;
            if (on != 1) {
                System.out.printf("    %d phases at once, timer=%.2f%n", on, f.bonusTimer);
                single = false;
                break;
            }
            f.update(DT, L);
        }
        check("one phase holds on every frame of a course", single);

        // The playtest chip: one tap from play into a course, which is how this game gets tuned.
        GameCore t = new GameCore(new Mem(), 29L);
        t.startGame();
        for (int i = 0; i < 60 * 6 && t.enemies.isEmpty(); i++) t.update(DT, L);
        boolean hadWords = !t.enemies.isEmpty();
        t.playtestStars(L);
        check("the playtest chip opens a course from play",
                t.state == GameCore.BONUS && t.starBonus && t.stars.ready());
        check("and retires the wave rather than leaving it falling",
                hadWords && t.enemies.isEmpty() && t.target == null);
        check("with a real collectible flying it", t.stars.who >= 0);
        // And it plays out into the next stage like any other interlude.
        int wasStage = t.stage;
        for (int i = 0; i < 60 * 30 && t.state == GameCore.BONUS; i++) t.update(DT, L);
        check("and hands back to play on the next stage",
                t.state == GameCore.PLAY && t.stage == wasStage + 1);
        // A course that was not finished stays queued, so the chip keeps handing them out.
        check("a lost course leaves the next interlude a course too", t.starNext);
        check("only from play, never over a screen that owns the keys",
                new GameCore(new Mem(), 31L).state == GameCore.TITLE);

        incompleteExit();
        courseIsFlyable(L);
        difficulty(L);
    }

    private static void incompleteExit() {
        Layout l=new Layout();l.compute(240,520,0,0,0,0);
        GameCore c=new GameCore(new Mem(),84L);c.startGame();c.state=GameCore.BONUS;c.starBonus=true;
        c.stars.make(c.rnd);c.stars.begin(0,l);
        for(float remaining:new float[]{StarPath.REPORT-0.01f,0.8f,0.1f}) {
            c.stars.timer=remaining;
            RasterPainter a=new RasterPainter(240,520,1),b=new RasterPainter(240,520,1);
            c.stars.x=l.playLeft;StarScreen.draw(a,c,l);
            c.stars.x=l.playRight;StarScreen.draw(b,c,l);
            check("departed flyer cannot reappear during incomplete report " + remaining,
                    java.util.Arrays.equals(a.resolve(),b.resolve()));
        }
    }

    private static void difficulty(Layout L) {
        group("star path difficulty");
        StarPath baseline = new StarPath(), harder = new StarPath();
        float previous = baseline.bendRate();
        check("the first course stays approachable", previous == 1f);
        for (int level = 1; level < StarPath.MAX_DIFFICULTY; level++) {
            harder.wins = level;
            check("the ramp rises faster at level " + level,
                    harder.bendRate() > 1f + level * 0.04f && harder.bendRate() > previous);
            previous = harder.bendRate();
        }
        harder.resetDifficulty();
        for(int win=1;win<=10;win++) {
            harder.recordWin();
            check("win advances exactly one difficulty level " + win,harder.wins==win);
        }
        for (int i = 0; i < 100; i++) harder.recordWin();
        check("further wins keep the difficulty capped", harder.wins == StarPath.MAX_DIFFICULTY
                && Math.abs(harder.bendRate() - 7f) < 0.0001f);
        baseline.make(new java.util.Random(77L));
        harder.make(new java.util.Random(77L));
        boolean changed = false;
        for (int i = 0; i < StarPath.COUNT; i++)
            changed |= Math.abs(baseline.sx[i] - harder.sx[i]) > 0.01f;
        check("wins change the actual course bends", changed);
        baseline.begin(-1, L);
        harder.begin(-1, L);
        baseline.collected = harder.collected = (1 << StarPath.COUNT) - 1;
        boolean sameTiming = true;
        for (int i = 0; i < 360; i++) {
            baseline.update(DT, L);
            harder.update(DT, L);
            sameTiming &= baseline.timer == harder.timer
                    && baseline.starY(10, L) == harder.starY(10, L);
        }
        check("harder curvature preserves flight and phase timing", sameTiming);
        Mem failedSave = new Mem();
        failedSave.starWins = 2;
        GameCore failed = new GameCore(failedSave, 78L);
        failed.startGame();
        failed.state = GameCore.BONUS;
        failed.starBonus = true;
        failed.stars.make(failed.rnd);
        failed.stars.begin(-1, L);
        failed.stars.collected = 7;
        failed.stars.timer = 0.001f;
        failed.update(DT, L);
        check("failure preserves progress without increasing difficulty",
                failed.stars.wins == 2 && failedSave.starWinSaves == 0
                        && failed.stars.collected == 7);

        float dragBase=0f,dragCap=0f,dragQuick=0f,dragTries=0f;
        for(long seed=1;seed<=32;seed++) {
            dragBase+=Integer.bitCount(dragFlown(seed,L,0,0,0.18f,1.8f));
            dragCap+=Integer.bitCount(dragFlown(seed,L,StarPath.MAX_DIFFICULTY,0,0.18f,1.8f));
            dragQuick+=Integer.bitCount(dragFlown(seed,L,StarPath.MAX_DIFFICULTY,0,0.08f,3f));
            int held=0,attempt=0;
            while(Integer.bitCount(held)<StarPath.COUNT && attempt<12)
                held=dragFlown(seed*31+attempt++,L,StarPath.MAX_DIFFICULTY,held,0.18f,1.8f);
            dragTries+=attempt;
        }
        System.out.printf("    drag: base %.1f, cap %.1f, quick cap %.1f stars; capped prize %.1f attempts%n",
                dragBase/32,dragCap/32,dragQuick/32,dragTries/32);
        check("capped bends challenge a bounded drag player",dragCap<dragBase*0.85f);
        check("faster drag reactions reward skill",dragQuick>dragCap);
        check("drag carry-over still yields prizes",dragTries/32<=6f);
        for (int level = 0; level <= StarPath.MAX_DIFFICULTY; level++) {
            float window = tightestWindow(L, level, 3f);
            System.out.printf("    bend level %d: feasible window %.3f widths%n", level, window);
            check("every difficulty is reachable with drag steering at level " + level, window > 0f);
        }
    }

    private static void courseIsFlyable(Layout L) {
        group("star course is flyable by dragging");
        float stop=StarPath.flyerR(L)/(L.playRight-L.playLeft);
        check("checkpoints stay inside the drag bounds",StarPath.EDGE>=stop-0.001f);
        float window=levelWindow(L),look=lookahead(L);
        System.out.printf("    pickup window %.0fms, visible warning %.0fms%n",window*1000f,look*1000f);
        check("a star stays level long enough to catch deliberately",window>=0.095f);
        check("a player can see the course coming",look>=0.25f);
        float drift=0f;
        for(long seed=1;seed<=32;seed++) drift+=Integer.bitCount(dragFlown(seed,L,0,0,0.18f,0f));
        check("a stationary passenger cannot finish a course",drift/32<=StarPath.COUNT*0.6f);
    }

    /**
     * The narrowest place a course can be threaded through, in play widths.
     *
     * The set of positions from which every remaining checkpoint is still catchable, propagated
     * forward at the steering limit and intersected with each checkpoint's catch band in turn. It is
     * the exact reachability statement: if it ever closes, no line completes the course. Stars level
     * with the flyer at the off are skipped — the course begins under it, so they are free.
     */
    private static float tightestWindow(Layout L, int wins, float maxSpeed) {
        float band = StarPath.pickupR(L) / (L.playRight - L.playLeft);
        float tightest = 9f;
        for (long seed = 1L; seed <= 256; seed++) {
            StarPath q = new StarPath();
            q.wins = wins;
            q.make(new java.util.Random(seed));
            float lo = 0.5f, hi = 0.5f, prev = 0f;
            for (int i = 0; i < StarPath.COUNT; i++) {
                float t = StarPath.encounterTime(i);
                float reach = maxSpeed * (t - prev);
                prev = t;
                lo = Math.max(lo - reach, q.sx[i] - band);
                hi = Math.min(hi + reach, q.sx[i] + band);
                if (t > 0f) tightest = Math.min(tightest, hi - lo);
                if (hi < lo) { lo = hi = q.sx[i]; }
            }
        }
        return tightest;
    }

    /**
     * Seconds of course visible ahead of the flyer at the fastest point of a flight — the top of
     * the screen down to the flyer's own height, at the speed the course is closing.
     *
     * The screen rather than the play field, because the route line is drawn to the top edge and
     * that is what a player reads the coming line off; the checkpoints on it are markers.
     */
    private static float lookahead(Layout L) {
        StarPath q = new StarPath();
        q.make(new java.util.Random(5L));
        q.begin(-1, L);
        float least = 9f;
        for (float t = 0; t <= StarPath.FLY; t += DT) {
            q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - t;
            float speed = q.closingSpeed(L);
            if (speed > 1f) least = Math.min(least, q.characterY(L) / speed);
        }
        return least;
    }

    /**
     * The shortest time any checkpoint spends within catching height of the flyer, in seconds. The
     * scroll accelerates, so this is the last star of a course; it is measured rather than derived
     * because the flyer is climbing while the course comes down at it.
     */
    private static float levelWindow(Layout L) {
        StarPath q = new StarPath();
        q.make(new java.util.Random(5L));
        q.begin(-1, L);
        float[] seen = new float[StarPath.COUNT];
        for (float t = 0; t <= StarPath.FLY; t += DT) {
            q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - t;
            // Asked for per frame, not once: the catch is GRACE seconds of the closing speed, and
            // the closing speed is what the launch ramp and the rush curve are doing right now.
            float ry = q.pickupY(L);
            for (int i = 0; i < StarPath.COUNT; i++) {
                if (Math.abs(q.characterY(L) - q.starY(i, L)) <= ry) seen[i] += DT;
            }
        }
        float least = 9f;
        for (int i = 0; i < StarPath.COUNT; i++) {
            // Stars that never come level at all are the ones still off the top when the flight
            // ends — the course is longer than the screen and only the flown part counts.
            if (seen[i] > 0f) least = Math.min(least, seen[i]);
        }
        return least;
    }

    // A finger reacts to visible checkpoints and has finite sideways speed; no teleporting.
    private static int dragFlown(long seed,Layout L,int wins,int carried,float reaction,float speed) {
        StarPath q=new StarPath();q.wins=wins;q.make(new java.util.Random(seed));q.begin(-1,L);
        q.collected=carried;q.beginDrag();
        float since=0f,target=q.x;
        for(int frame=0;frame<600 && !q.won && !q.reporting();frame++) {
            since+=DT;
            if(since>=reaction) {
                since=0f;int aim=nextStar(q,L);
                if(aim>=0 && q.starY(aim,L)>=L.playTop) target=q.starX(aim,L);
            }
            float step=L.w*speed*DT;
            q.dragTo(q.x+Math.max(-step,Math.min(step,target-q.x)),L);
            q.update(DT,L);
        }
        return q.collected;
    }

    /** The next checkpoint still worth chasing: not taken, and not yet past the flyer. */
    private static int nextStar(StarPath q, Layout L) {
        for (int i = 0; i < StarPath.COUNT; i++) {
            if ((q.collected & (1 << i)) != 0) continue;
            if (q.starY(i, L) > q.characterY(L) + q.pickupY(L)) continue;
            return i;
        }
        return -1;
    }
}
