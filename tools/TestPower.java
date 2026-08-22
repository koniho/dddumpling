package com.sram.hexatype;

/** The powerup letter, the frenzy it starts, and the three modes it can grant. */
final class TestPower extends Check {

    private TestPower() {}

    /** Puts a catchable powerup of a known mode on the field. */
    private static Power place(GameCore c, Layout L, int effect, int glyph) {
        Power w = new Power();
        w.glyph = glyph;
        w.effect = effect;
        w.y = L.playTop + (L.dangerY - L.playTop) * 0.3f;
        w.x = L.w * 0.5f;
        w.vx = 0f;
        c.power = w;
        return w;
    }

    static void drifting(Layout L) {
        group("powerup letter");
        GameCore c = new GameCore(new Mem(), 201L);
        c.startGame();
        check("none at the start", c.power == null);
        check("no mode at the start", !c.powerActive() && c.mode == -1);

        // It appears on its own eventually, and crosses horizontally.
        int frames = 0;
        while (c.power == null && frames < 60 * 60) {
            c.update(DT, L);
            frames++;
        }
        check("one appears unprompted", c.power != null);
        Power w = c.power;
        float x0 = w.x, y0 = w.y;
        check("it starts off the side of the screen", x0 < 0f || x0 > L.w);
        check("it sits above the danger line", y0 < L.dangerY && y0 > L.playTop);
        advance(c, L, 1.0f);
        check("it travels horizontally", Math.abs(w.x - x0) > 1f);
        check("it holds its height", w.y == y0);

        // Left alone, it leaves and is not replaced immediately.
        float t = 0;
        while (c.power == w && t < Power.CROSS_TIME * 2f) {
            c.update(DT, L);
            t += DT;
        }
        check("it drifts away if ignored", c.power == null);
        check("no mode was granted", !c.powerActive());
        check("another is queued up", c.powerTimer > 0f);
    }

    static void precedence(Layout L) {
        group("powerup precedence");
        GameCore c = new GameCore(new Mem(), 202L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy word = add(c, L, new int[] {3, 4}, L.playTop + 600);
        place(c, L, Power.MULTI, 3);

        // Nothing engaged: the powerup wins even though a word matches the same letter.
        c.tapKey(3, L);
        check("the powerup outranks a matching word", c.powerActive());
        check("the word was left alone", word.pos == 0);
        check("it is no longer catchable", !c.power.catchable());

        // Already engaged: the word keeps the press.
        GameCore d = new GameCore(new Mem(), 203L);
        d.startGame();
        d.enemies.clear();
        d.target = null;
        GameCore.Enemy w2 = add(d, L, new int[] {2, 2}, L.playTop + 600);
        d.tapKey(2, L);
        check("engaged the word first", d.target == w2 && w2.pos == 1);
        place(d, L, Power.MULTI, 2);
        d.tapKey(2, L);
        check("an engaged word keeps the press", w2.pos == 2);
        check("the powerup was not taken", !d.powerActive() && d.power.catchable());
    }

    static void frenzy(Layout L) {
        group("frenzy");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 204L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        int startTone = ear.starts;
        check("a new game plays its tone", startTone == 1);

        place(c, L, Power.FLURRY, 1);
        float sky0 = c.skyClock;
        // startGame already asserts frenzy(false), so count from there rather than from zero.
        int fc0 = ear.frenzyCalls;
        c.tapKey(1, L);
        check("catching it starts a frenzy", c.powerActive() && c.mode == Power.FLURRY);
        check("it runs for the full duration", c.modeLeft > Power.DURATION - 0.01f);
        check("catching it scores", c.score >= Power.SCORE);
        check("the music switches", ear.frenzyCalls == fc0 + 1 && ear.frenzyOn);

        // Words arrive faster, and the sky runs four times faster.
        c.stage = 3;
        float fast = c.spawnInterval() / Power.spawnRate(c.ramp());
        check("the spawn interval shortens", fast < c.spawnInterval());
        advance(c, L, 1.0f);
        check("the sky runs fast", c.skyClock - sky0 > Power.SKY_RATE - 1f);

        // Damage still lands during a frenzy.
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        int lives = c.lives;
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("damage still applies during a frenzy", c.lives == lives - 1);
        check("the frenzy survives the hit", c.powerActive());

        // Running out clears the stage and hands over to the interlude.
        c.enemies.clear();
        add(c, L, new int[] {1, 2}, L.playTop + 200);
        add(c, L, new int[] {3, 4}, L.playTop + 400);
        // Headroom: fifteen idle seconds at 6x spawn and 2x fall would otherwise end the
        // run, and the frenzy timer is what is under test here, not survival.
        c.lives = 50;
        advance(c, L, Power.DURATION);
        check("the frenzy ends", !c.powerActive() && c.mode == -1);
        check("the music switches back", !ear.frenzyOn && ear.frenzyCalls == fc0 + 2);
        check("everything on the field is destroyed", allDestroyed(c));
        check("the wave counts as fully released", c.spawnedThisStage >= c.stageQuota());

        // Step to the interlude rather than over it: bonusTimer starts counting down at once.
        int guard = 0;
        while (c.state != GameCore.BONUS && guard++ < 600) c.update(DT, L);
        check("the interlude follows", c.state == GameCore.BONUS);
        check("the frenzy tone plays", ear.powerClears == 1);
        check("the ordinary stage tone is suppressed", ear.stageClears == 0);
        // A frenzy no longer buys extra interlude: its 2.6s was larger than the whole spread of the
        // earned-mash ladder, so a hurt frenzy round out-paid a perfect calm one. Asserted on
        // bonusRollEnd, which is set once when the interlude opens — bonusTimer has already been
        // counting for as long as it took the loop above to notice, so it cannot be compared to a
        // full length.
        check("a frenzy buys no extra interlude", Math.abs(c.bonusRollEnd
                - (GameCore.bonusLength(c.earnedMash) - GameCore.BONUS_ROLL)) < 0.001f);
        // And the clock says so. This is the assertion the old behaviour failed out loud: a
        // frenzy-cleared perfect round read 5.0 earned plus 2.6 bought, so the countdown opened on
        // "8" — a number the ladder has no tier for, on a clock whose highest tier is 5.
        check("the clock cannot read higher than the top tier",
                c.bonusLeft() <= GameCore.MASH_PERFECT + 0.001f);
        check("the powerup is cleared away", c.power == null);

        // A stage cleared normally plays the ordinary tone instead.
        GameCore d = new GameCore(new Mem(), 205L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.startGame();
        d.spawnedThisStage = d.stageQuota();
        d.enemies.clear();
        d.shots.clear();
        advanceToBonus(d, L);
        check("a normal clear plays the stage tone", ear2.stageClears == 1);
        check("and not the frenzy tone", ear2.powerClears == 0);
        check("a normal interlude is exactly what the round earned",
                d.bonusTimer < GameCore.bonusLength(d.earnedMash) + 0.01f);
    }

    private static boolean allDestroyed(GameCore c) {
        for (int i = 0; i < c.enemies.size(); i++) {
            if (!c.enemies.get(i).destroyed) return false;
        }
        return true;
    }

    static void flurryMode(Layout L) {
        group("FLURRY");
        GameCore c = new GameCore(new Mem(), 206L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.FLURRY, 0);
        c.tapKey(0, L);
        check("flurry is running", c.flurry() && !c.flinging() && !c.multi());

        GameCore.Enemy e = add(c, L, new int[] {2, 5}, L.playTop + 300);
        int missesBefore = c.misses;
        // Nothing matches key 1, but under flurry any key works.
        c.tapKey(1, L);
        check("a non-matching key still engages", c.target == e && e.pos == 1);
        check("and is not a miss", c.misses == missesBefore);
        c.tapKey(1, L);
        check("a non-matching key still advances", e.pos == 2);
        advance(c, L, 0.3f);
        check("the word still finishes", e.destroyed);

        // Once it lapses, wrong keys are wrong again.
        c.modeLeft = 0f;
        c.mode = -1;
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy f = add(c, L, new int[] {2, 5}, L.playTop + 300);
        c.tapKey(1, L);
        check("after flurry a wrong key misses", f.pos == 0 && c.misses > missesBefore);
    }

    static void multiMode(Layout L) {
        group("MULTI");
        GameCore c = new GameCore(new Mem(), 207L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.MULTI, 0);
        c.tapKey(0, L);
        check("multi is running", c.multi());

        // Three words, all containing letter 4 at various positions.
        GameCore.Enemy a = add(c, L, new int[] {4, 1}, L.playTop + 200);
        GameCore.Enemy b = add(c, L, new int[] {1, 4}, L.playTop + 400);
        GameCore.Enemy d = add(c, L, new int[] {1, 1}, L.playTop + 600);
        int scoreBefore = c.score;

        c.tapKey(4, L);
        check("every matching letter goes", a.gone[0] && b.gone[1]);
        check("non-matching letters stay", !a.gone[1] && !b.gone[0]);
        check("a word with no match is untouched", !d.gone[0] && !d.gone[1]);
        check("it scores", c.score > scoreBefore);
        check("it counts as one hit", c.hits >= 1);

        // A word whose every letter is taken is destroyed outright.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy solo = add(c, L, new int[] {3, 3}, L.playTop + 300);
        c.tapKey(3, L);
        check("clearing every letter destroys the word", solo.destroyed);

        // A press matching nothing is still a miss.
        c.enemies.clear();
        int missesBefore = c.misses;
        c.tapKey(5, L);
        check("a press matching nothing misses", c.misses == missesBefore + 1);
    }

    /**
     * Every frenzy mode has to keep turning up. A skew here is close to invisible in play —
     * you only see a handful of frenzies in a run, so three of the same in a row feels like a
     * bug and three different ones proves nothing. Measured rather than eyeballed.
     */
    static void modeSpread(Layout L) {
        group("frenzy mode spread");
        check("a name and a blurb for every mode",
                Power.NAMES.length == Power.COUNT && Power.BLURB.length == Power.COUNT);
        check("TEAM SQUISH is the last mode, which is how it gets gated",
                Power.TEAM == Power.COUNT - 1);

        // The draw itself, made in the same order spawnPower makes it.
        java.util.Random r = new java.util.Random(4242L);
        int[] raw = new int[Power.COUNT];
        for (int i = 0; i < 30000; i++) {
            r.nextInt(Glyph.COUNT);
            raw[r.nextInt(Power.COUNT)]++;
        }
        int lo = Integer.MAX_VALUE, hi = 0;
        for (int i = 0; i < raw.length; i++) {
            lo = Math.min(lo, raw[i]);
            hi = Math.max(hi, raw[i]);
        }
        System.out.print("    30000 draws:");
        for (int i = 0; i < raw.length; i++) System.out.printf(" %d", raw[i]);
        System.out.println();
        check("no mode is favoured in the draw", hi - lo < 30000 / 20);

        // And through the real spawn path, which is the part that got doubted. The wave is held
        // open every frame so the run never ends and the letters keep coming. The case is
        // stocked, or TEAM SQUISH would rightly never appear.
        Mem full = new Mem();
        full.collected = Collect.MASK;
        GameCore c = new GameCore(full, 4243L);
        c.startGame();
        int[] seen = new int[Power.COUNT];
        int last = -1;
        for (int i = 0; i < 60 * 900; i++) {
            c.enemies.clear();
            c.spawnedThisStage = 0;
            // Bosses are kept out of this one on purpose. It is a measurement of the powerup draw,
            // and a boss deliberately suppresses powerups for the whole of its stage — so leaving
            // them in would not make this a harder test, it would make it a test of the four stages
            // before the first boss, silently. The fight is measured in TestBoss and in the soak.
            if (c.boss.active()) c.boss.leave();
            c.update(DT, L);
            if (c.mode >= 0 && c.mode != last) seen[c.mode]++;
            last = c.mode;
            if (c.power != null && c.power.catchable()) c.tapKey(c.power.glyph, L);
        }
        System.out.print("    15 minutes of play:");
        for (int i = 0; i < seen.length; i++) {
            System.out.printf("  %s=%d", Power.NAMES[i], seen[i]);
        }
        System.out.println();
        boolean all = true;
        for (int i = 0; i < seen.length; i++) if (seen[i] == 0) all = false;
        check("every mode turns up in play", all);

        // And with an empty case, TEAM SQUISH must never be offered — it has nobody to field.
        GameCore e = new GameCore(new Mem(), 4245L);
        e.startGame();
        boolean offered = false;
        for (int i = 0; i < 60 * 900; i++) {
            e.enemies.clear();
            e.spawnedThisStage = 0;
            e.update(DT, L);
            if (e.power != null && e.power.effect == Power.TEAM) offered = true;
            if (e.mode == Power.TEAM) offered = true;
            if (e.power != null && e.power.catchable()) e.tapKey(e.power.glyph, L);
        }
        check("an empty case is never offered TEAM SQUISH", !offered);
        check("but the other modes still come", e.score > 0);
    }

    /** TEAM SQUISH: who turns up, how it moves, and what it takes. */
    static void teamMode(Layout L) {
        group("TEAM SQUISH");
        // The gate: nobody collected, nobody to field.
        GameCore empty = new GameCore(new Mem(), 271L);
        empty.startGame();
        empty.playtestMode(Power.TEAM, L);
        check("an empty case cannot start the mode",
                !empty.powerActive() && empty.buddy.out());
        check("and it does not half-start", empty.mode == -1);

        Mem store = new Mem();
        store.collected = (1L << 4) | (1L << 11) | (1L << 23);
        GameCore c = new GameCore(store, 273L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        c.playtestMode(Power.TEAM, L);
        check("the mode starts with a stocked case", c.team() && !c.buddy.out());
        check("the squishy is one of yours", Collect.has(c.collected, c.buddy.who));
        check("it starts inside the field",
                c.buddy.x > L.playLeft && c.buddy.x < L.playRight
                        && c.buddy.y > L.playTop && c.buddy.y < L.dangerY);
        check("it starts moving in both axes", c.buddy.vx != 0f && c.buddy.vy != 0f);
        check("it starts with nothing squished and no target",
                c.buddy.squishes == 0 && c.buddy.chase == null);

        // Frenzy spawns are a mixture: the ordinary top entrance remains, while side entries
        // begin wholly beyond either edge and bend across to a safe landing on the far side.
        GameCore paths = new GameCore(store, 274L);
        paths.startGame();
        paths.enemies.clear();
        paths.playtestMode(Power.TEAM, L);
        int tops = 0, sides = 0, lefts = 0, rights = 0;
        boolean outside = true, crosses = true, curved = true, lands = true;
        for (int i = 0; i < 80; i++) {
            paths.enemies.clear();
            paths.spawnTimer = 0f;
            paths.modeLeft = Power.DURATION;
            paths.update(DT, L);
            GameCore.Enemy word = paths.enemies.get(0);
            if (!word.sideEntry) {
                tops++;
                continue;
            }
            sides++;
            boolean left = word.pathStartX < L.playLeft;
            if (left) lefts++; else rights++;
            outside &= left ? word.pathStartX < L.playLeft : word.pathStartX > L.playRight;
            crosses &= left ? word.pathEndX > L.w / 2f : word.pathEndX < L.w / 2f;
            float linearMid = (word.pathStartX + word.pathEndX) / 2f;
            word.y = (word.pathStartY + L.dangerY - L.enemyR) / 2f;
            paths.updateSidePath(word, L);
            curved &= Math.abs(word.baseX - linearMid) > L.enemyR;
            word.y = L.dangerY - L.enemyR;
            paths.updateSidePath(word, L);
            lands &= Math.abs(word.baseX - word.pathEndX) < 0.01f
                    && word.baseX >= L.playLeft && word.baseX <= L.playRight;
        }
        check("TEAM SQUISH mixes top and side entrances", tops > 0 && sides > 0);
        check("side entrances use both edges", lefts > 0 && rights > 0);
        check("side words begin beyond the play area", outside);
        check("they cross toward the opposite side", crosses);
        check("their crossing is an arc, not a straight diagonal", curved);
        check("they reach the damage line inside the opposite edge", lands);

        // FLING shares the same mixed entrance pool. Its blade benefits from targets crossing
        // the field, but top-down words remain so the frenzy does not become one repeated motion.
        GameCore flingPaths = new GameCore(new Mem(), 276L);
        flingPaths.startGame();
        flingPaths.enemies.clear();
        flingPaths.playtestMode(Power.FLING, L);
        int flingTops = 0, flingSides = 0;
        boolean flingCrosses = true;
        for (int i = 0; i < 80; i++) {
            flingPaths.enemies.clear();
            flingPaths.spawnTimer = 0f;
            flingPaths.modeLeft = Power.DURATION;
            flingPaths.update(DT, L);
            GameCore.Enemy word = flingPaths.enemies.get(0);
            if (!word.sideEntry) {
                flingTops++;
            } else {
                flingSides++;
                flingCrosses &= word.pathStartX < L.playLeft
                        ? word.pathEndX > L.w / 2f : word.pathEndX < L.w / 2f;
            }
        }
        check("FLING mixes top and side entrances", flingTops > 0 && flingSides > 0);
        check("FLING side words cross toward the opposite edge", flingCrosses);
        float small = c.buddy.radius(L), dim = c.buddy.glow();

        // It stays inside the field, however long it bounces around in there.
        boolean inside = true, moved = false;
        float x0 = c.buddy.x, y0 = c.buddy.y;
        for (int i = 0; i < 60 * 30; i++) {
            c.enemies.clear();                    // nothing to hit: pure bouncing
            c.modeLeft = Power.DURATION;           // hold the mode open
            c.update(DT, L);
            float r = c.buddy.radius(L);
            if (c.buddy.x - r < L.playLeft - 1f || c.buddy.x + r > L.playRight + 1f
                    || c.buddy.y - r < L.playTop - 1f || c.buddy.y + r > L.dangerY + 1f) {
                inside = false;
            }
            if (Math.abs(c.buddy.x - x0) > L.enemyR || Math.abs(c.buddy.y - y0) > L.enemyR) {
                moved = true;
            }
        }
        check("it never leaves the play area", inside);
        check("it does move around in there", moved);

        // Bouncing off a word squishes the whole word, and leaves it bigger and brighter.
        GameCore d = new GameCore(store, 275L);
        d.startGame();
        d.enemies.clear();
        d.target = null;
        d.playtestMode(Power.TEAM, L);
        GameCore.Enemy prey = add(d, L, new int[] {1, 2, 3}, d.buddy.y);
        prey.baseX = d.buddy.x;
        int squishesBefore = d.squishes;
        float wasR = d.buddy.radius(L), wasGlow = d.buddy.glow();
        d.update(DT, L);
        check("running into a word squishes the whole word", prey.destroyed);
        check("it is credited as a squish", d.squishes == squishesBefore + 1);
        check("the squishy counts it", d.buddy.squishes == 1);
        check("it gets bigger", d.buddy.radius(L) > wasR);
        check("and brighter", d.buddy.glow() > wasGlow);
        check("growth is the same rule as the readout", small == wasR && dim == wasGlow);
        check("TEAM SQUISH marks the death as impact-driven", prey.radialFly);

        // Pin the vector rule independently of the moving buddy: every tile must have a
        // positive dot product away from the collision, including a real vertical component.
        GameCore.Enemy burst = add(d, L, new int[] {0, 1, 2}, L.playTop + 240f);
        burst.baseX = L.w * 0.55f;
        float impactX = d.enemyCentreX(burst) - L.enemyR;
        float impactY = burst.y + L.enemyR * 2f;
        d.computeImpactFlyDirs(burst, impactX, impactY, L);
        boolean impactAway = true, vertical = true;
        for (int i = 0; i < burst.word.length; i++) {
            float dx = d.tileX(burst, i, L) - impactX;
            float dy = burst.y - impactY;
            impactAway &= dx * burst.flyDir[i] + dy * burst.flyY[i] > 0f;
            vertical &= burst.flyY[i] < -0.1f;
        }
        check("every tile flies away from the TEAM SQUISH impact", impactAway);
        check("impact deaths move vertically as well as sideways", vertical);

        // Growth is capped, or it would fill the field.
        d.buddy.squishes = 500;
        float huge = d.buddy.radius(L);
        d.buddy.squishes = 501;
        check("growth is capped", d.buddy.radius(L) == huge);
        check("and the cap leaves room to move",
                huge * 2f < (L.playRight - L.playLeft) * 0.6f);
        check("glow is capped too", d.buddy.glow() <= 1f);
        d.buddy.squishes = 1;

        // A press aims it. The word it picks is the one that press would have attacked.
        GameCore e = new GameCore(store, 277L);
        e.startGame();
        e.enemies.clear();
        e.target = null;
        e.playtestMode(Power.TEAM, L);
        GameCore.Enemy high = add(e, L, new int[] {1, 1}, L.playTop + 120f);
        GameCore.Enemy low = add(e, L, new int[] {2, 2}, L.playTop + 400f);
        check("the press lands", e.tapKey(1, L));
        check("it targets the word wanting that letter", e.buddy.chase == high);
        check("a press is a hit, not a miss", e.misses == 0 && e.hits > 0);
        check("but pays nothing by itself", e.squishes == 0);
        check("and does not type", high.pos == 0);

        // Any key works: with no match it falls back to the most urgent word.
        e.buddy.chase = null;
        check("a letter nobody wants still lands", e.tapKey(5, L));
        check("and takes the most urgent word instead", e.buddy.chase == low);

        // The charge arrives, and takes the whole word.
        boolean arrived = false;
        for (int i = 0; i < 60 * 10 && !arrived; i++) {
            e.modeLeft = Power.DURATION;
            e.update(DT, L);
            arrived = low.destroyed;
        }
        check("the charge reaches its target", arrived);
        check("and the target is forgotten once taken", e.buddy.chase != low);

        // The charge is a steer, not a snap: the heading swings round at a fixed rate. Set up the
        // worst case — a target dead behind it — and watch it come about.
        GameCore t = new GameCore(store, 279L);
        t.startGame();
        t.enemies.clear();
        t.target = null;
        t.playtestMode(Power.TEAM, L);
        check("a full turn takes about 0.7s", Math.abs(Buddy.TURN_TIME - 0.7f) < 0.001f);
        check("the rate follows from it",
                Math.abs(Buddy.TURN_RATE * Buddy.TURN_TIME - 6.28319f) < 0.01f);

        // Parked in the middle heading right, with a word directly to its left.
        t.buddy.x = (L.playLeft + L.playRight) / 2f;
        t.buddy.y = (L.playTop + L.dangerY) / 2f;
        t.buddy.vx = 1f;
        t.buddy.vy = 0f;
        GameCore.Enemy behind = add(t, L, new int[] {1}, t.buddy.y);
        behind.baseX = L.playLeft + L.enemyR * 3f;
        t.buddy.charge(behind);
        check("it is still pointing the wrong way", t.buddy.vx > 0f);

        // Measured over a short window rather than by timing the whole turn: the bubble travels
        // while it comes about, so the angle it still needs keeps shifting and a wall bounce can
        // take the heading over before it arrives. Sweep rate over a window where it is certainly
        // still turning is the honest measurement — a reversal needs half a full turn, so 0.1s in
        // it is nowhere near aligned.
        float last = (float) Math.atan2(t.buddy.vy, t.buddy.vx);
        float swept = 0f;
        boolean paced = true;
        int frames = 0;
        while (frames < 6) {
            t.modeLeft = Power.DURATION;
            t.update(DT, L);
            frames++;
            float now = (float) Math.atan2(t.buddy.vy, t.buddy.vx);
            float step = Math.abs(now - last);
            if (step > 3.14159f) step = 6.28319f - step;      // across the seam
            // A frame's turn can never exceed the rate. Rounding gets a little slack.
            if (step > Buddy.TURN_RATE * DT + 0.001f) paced = false;
            swept += step;
            last = now;
        }
        check("no frame turns further than the rate allows", paced);
        check("it did turn while it was pointing away", swept > 0.1f);
        check("it is still mid-turn, not arrived", t.buddy.chase == behind);

        // The point of the whole exercise: at the rate it actually swings, how long is a lap?
        float elapsed = frames * DT;
        float lap = 6.28319f / (swept / elapsed);
        check("at that rate a full turn takes 0.7s", Math.abs(lap - 0.7f) < 0.02f);
        System.out.printf("    buddy turn: %.2f rad in %.2fs, so a lap in %.2fs%n",
                swept, elapsed, lap);

        // Speed is driven too. Nothing about it is allowed to step: not the wind-up into a
        // charge, not the wind-down out of one, and a hard turn has to cost speed on the way in.
        float drift = Buddy.SPEED * L.w;
        float charge = drift * Buddy.CHARGE_RATE;
        float accel = drift * (Buddy.CHARGE_RATE - 1f) / Buddy.SPIN_UP;

        GameCore s = new GameCore(store, 281L);
        s.startGame();
        s.enemies.clear();
        s.target = null;
        s.playtestMode(Power.TEAM, L);
        s.buddy.x = (L.playLeft + L.playRight) / 2f;
        s.buddy.y = (L.playTop + L.dangerY) / 2f;
        s.buddy.vx = drift;                       // pointing right, at drift speed
        s.buddy.vy = 0f;
        GameCore.Enemy away = add(s, L, new int[] {1}, s.buddy.y);
        away.baseX = L.playLeft + L.enemyR * 3f;  // dead behind it: the worst turn there is
        s.buddy.charge(away);

        float was = speedOf(s.buddy);
        float slowest = was, fastest = was;
        boolean smooth = true;
        for (int i = 0; i < 6; i++) {
            s.modeLeft = Power.DURATION;
            s.update(DT, L);
            float now = speedOf(s.buddy);
            if (Math.abs(now - was) > accel * DT + 1f) smooth = false;
            slowest = Math.min(slowest, now);
            fastest = Math.max(fastest, now);
            was = now;
        }
        check("speed never steps, it accelerates", smooth);
        check("a hard turn costs speed rather than gaining it", slowest < drift);
        check("and does not stall in the corner", slowest > drift * 0.4f);
        check("nowhere near charge speed while still turning", fastest < charge * 0.5f);
        System.out.printf("    buddy corner: drift %.0f, slowest %.0f, charge %.0f px/s%n",
                drift, slowest, charge);

        // Pointed straight at it, it winds up. Measured as a rate rather than by waiting to hit
        // charge speed: the field is not wide enough to cross in the wind-up time, so it squishes
        // the word first and the wait never completes — timing the arrival tests the geometry, not
        // the acceleration.
        s.buddy.x = L.playLeft + L.enemyR * 2f;
        s.buddy.vx = drift;
        s.buddy.vy = 0f;
        away.baseX = L.playRight - L.enemyR * 3f;   // now dead ahead
        float from = speedOf(s.buddy);
        boolean climbing = true;
        int gained = 0;
        while (gained < 4) {
            s.modeLeft = Power.DURATION;
            s.update(DT, L);
            gained++;
            if (speedOf(s.buddy) <= from) climbing = false;
        }
        float rate = (speedOf(s.buddy) - from) / (gained * DT);
        float windUp = (charge - drift) / rate;
        check("it climbs toward charge speed", climbing);
        check("one frame is nowhere near it", rate * DT < (charge - drift) * 0.2f);
        check("at that rate the wind-up takes SPIN_UP",
                Math.abs(windUp - Buddy.SPIN_UP) < 0.02f);
        System.out.printf("    buddy wind-up: %.0f px/s per s, so drift to charge in %.2fs%n",
                rate, windUp);

        // And it winds back down once there is nothing to chase. Set at charge speed directly:
        // getting there under its own power is what the rate check above already covers.
        s.buddy.chase = null;
        s.enemies.clear();
        s.buddy.vx = charge;
        s.buddy.vy = 0f;
        s.modeLeft = Power.DURATION;
        s.update(DT, L);
        check("dropping the charge eases off rather than cutting",
                speedOf(s.buddy) < charge && speedOf(s.buddy) > drift);
        advance(s, L, Buddy.SPIN_UP * 2f);
        check("and settles back to drift speed",
                Math.abs(speedOf(s.buddy) - drift) < drift * 0.05f);

        // A target squished by something else must not be chased into nothing.
        e.buddy.chase = high;
        e.enemies.remove(high);
        e.update(DT, L);
        check("a target that is gone is dropped", e.buddy.chase == null);

        // The squishy leaves with the frenzy, and never lingers into play.
        e.modeLeft = 0.01f;
        advance(e, L, 0.2f);
        check("the frenzy ended", !e.powerActive());
        check("the squishy has gone with it", e.buddy.out());
        advance(e, L, 1f);
        check("and stays gone", e.buddy.out());

        // Dying mid-mode has to take it too. It is only ever sent home from the PLAY half of
        // update(), which a death puts out of reach — so the bubble used to carry on bouncing
        // through the swirl, the summary and the title screen behind them.
        GameCore k = new GameCore(store, 277L);
        k.startGame();
        k.enemies.clear();
        k.target = null;
        k.playtestMode(Power.TEAM, L);
        check("the mode is running before the death", k.team() && !k.buddy.out());
        k.lives = 1;
        add(k, L, new int[] {0, 0}, L.dangerY - L.enemyR + 1f);
        advance(k, L, GameCore.ATTACK_TIME + 2 * DT);
        check("the last life went", k.state == GameCore.OVER);
        check("the frenzy died with the player", !k.powerActive());
        check("and so did the squishy", k.buddy.out());
        advancePastDeath(k, L);
        check("it is not back on the summary", k.buddy.out());
    }

    /** The MULTI chain: what one press takes, in what order, and what it pays. */
    static void chain(Layout L) {
        group("MULTI chain");
        GameCore c = new GameCore(new Mem(), 251L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.MULTI, 0);
        c.tapKey(0, L);
        check("multi is running", c.multi());
        check("no chain on screen yet", c.chainT == 0f && c.chainLen == 0);

        // Three words of nothing but that letter, so one press takes all six and finishes each
        // of them. A word with a different letter in it would only be part-cleared, which is
        // correct but tests less.
        GameCore.Enemy[] row = new GameCore.Enemy[3];
        for (int k = 0; k < 3; k++) {
            row[k] = add(c, L, new int[] {1, 1}, L.playTop + 200f + k * 120f);
        }
        int before = c.score;
        check("the press lands", c.tapKey(1, L));
        check("every match went", c.chainLen == 6);
        check("all three words are finished",
                row[0].destroyed && row[1].destroyed && row[2].destroyed);
        check("it counts as one hit", c.hits == 2);   // the powerup catch, then this

        // Escalating: the nth hop is worth n steps, so six hops pay 8+16+...+48.
        int expect = 0;
        for (int h = 1; h <= 6; h++) expect += GameCore.CHAIN_STEP * h;
        check("the chain pays an escalating total", c.chainScore == expect);
        check("a long chain beats a flat rate",
                c.chainScore > GameCore.CHAIN_STEP * c.chainLen * 2);
        check("the score went up by at least that", c.score - before >= expect);

        // The path: it starts at the most urgent match and never criss-crosses, so each hop is
        // the nearest one that was left.
        boolean startedLow = true;
        for (int i = 1; i < c.chainLen; i++) {
            if (c.chainY[i] > c.chainY[0] + 0.5f) startedLow = false;
        }
        check("it starts at the lowest match", startedLow);

        // A word with other letters in it is only part-cleared, and keeps its typing position.
        GameCore m = new GameCore(new Mem(), 252L);
        m.startGame();
        m.enemies.clear();
        m.target = null;
        place(m, L, Power.MULTI, 0);
        m.tapKey(0, L);
        GameCore.Enemy mixed = add(m, L, new int[] {1, 2, 1}, L.playTop + 200f);
        m.tapKey(1, L);
        check("both matches in a mixed word go", mixed.gone[0] && mixed.gone[2]);
        check("the letter between them stays", !mixed.gone[1]);
        check("the word is not finished", !mixed.destroyed && mixed.typeable());
        check("typing resumes at the survivor", mixed.pos == 1);

        // Played back, not instant: the hops reveal over time and each one sounds.
        Ear ear = new Ear();
        c.sound = ear;
        check("nothing revealed on the press itself", c.chainShown == 0);
        advance(c, L, GameCore.CHAIN_TIME * GameCore.CHAIN_REVEAL + 2 * DT);
        check("every hop is revealed", c.chainShown == c.chainLen);
        check("and every hop cracked", ear.zaps == c.chainLen);
        check("with none of them a squish", ear.squishes == 0);
        check("the crack climbs with the chain", ear.lastZapHop == c.chainLen);
        check("the chain is still on screen while it fades", c.chainT > 0f);
        advance(c, L, GameCore.CHAIN_TIME);
        check("then it goes", c.chainT == 0f);

        // A press with nothing to chain is still a miss.
        GameCore d = new GameCore(new Mem(), 253L);
        d.startGame();
        d.enemies.clear();
        d.target = null;
        place(d, L, Power.MULTI, 0);
        d.tapKey(0, L);
        add(d, L, new int[] {1}, L.playTop + 200f);
        int missed = d.misses;
        check("a letter that is not there does not land", !d.tapKey(4, L));
        check("and counts as a miss", d.misses == missed + 1);
        check("with no chain drawn", d.chainLen == 0);

        // A single match is a chain of one, worth exactly one step, and not worth announcing.
        check("one match lands", d.tapKey(1, L));
        check("a chain of one", d.chainLen == 1);
        check("worth one step", d.chainScore == GameCore.CHAIN_STEP);
        check("too short to shout about", d.chainLen < Hud.CHAIN_CALL);

        // The gathering is bounded, so a frenzy-sized field cannot overrun the arrays.
        GameCore e = new GameCore(new Mem(), 255L);
        e.startGame();
        e.enemies.clear();
        e.target = null;
        place(e, L, Power.MULTI, 0);
        e.tapKey(0, L);
        for (int k = 0; k < 20; k++) {
            add(e, L, new int[] {1, 1, 1, 1, 1}, L.playTop + 60f + k * 30f);
        }
        e.tapKey(1, L);
        check("the chain is capped, not overrun", e.chainLen <= GameCore.CHAIN_MAX);
        check("and it took what it could", e.chainLen == GameCore.CHAIN_MAX);
    }

    /** The blade: what one stroke cuts, and the beat a multi-word stroke earns. */
    static void blade(Layout L) {
        group("FLING blade");
        // Distance from a point to a segment, which is what decides every cut.
        check("a point on the segment is at no distance",
                Blade.segDist2(5f, 0f, 0f, 0f, 10f, 0f) < 0.001f);
        check("perpendicular offset is measured square",
                Math.abs(Blade.segDist2(5f, 3f, 0f, 0f, 10f, 0f) - 9f) < 0.001f);
        check("past an end clamps to that end",
                Math.abs(Blade.segDist2(-4f, 0f, 0f, 0f, 10f, 0f) - 16f) < 0.001f);
        check("a stationary finger degenerates to a point",
                Math.abs(Blade.segDist2(3f, 4f, 0f, 0f, 0f, 0f) - 25f) < 0.001f);
        check("the blade is wider than a tile", Blade.BLADE > 1f);

        GameCore c = new GameCore(new Mem(), 231L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.FLING, 0);
        c.tapKey(0, L);
        check("fling is running", c.flinging());

        // One stroke straight across a whole word takes every letter in it. The old model cut
        // one tile per gesture, and only if the gesture began on one.
        GameCore.Enemy e = add(c, L, new int[] {1, 2, 3, 4}, L.playTop + 300f);
        float y = e.y;
        c.beginStroke(c.tileX(e, 0, L) - L.enemyR * 2f, y);
        check("nothing is cut before it travels", c.strokeCuts == 0);
        int cut = c.sliceTo(c.tileX(e, 3, L) + L.enemyR * 2f, y, L);
        check("one sweep cuts the whole word", cut == 4 && c.strokeCuts == 4);
        check("and that finished it", e.destroyed);
        check("one word is not enough for the beat",
                c.strokeKills == 1 && c.slowdown == 0f);
        c.endStroke();

        // A tap that does not move cuts nothing, so the mode stays a swipe.
        c.enemies.clear();
        GameCore.Enemy p2 = add(c, L, new int[] {1, 1}, L.playTop + 260f);
        c.beginStroke(c.tileX(p2, 0, L), p2.y);
        check("a tap on a letter cuts nothing on its own", c.strokeCuts == 0);
        c.endStroke();
        check("the letter survived it", !p2.gone[0]);

        // Two words on one stroke earns the slow-motion beat.
        GameCore d = new GameCore(new Mem(), 233L);
        d.startGame();
        d.enemies.clear();
        d.target = null;
        place(d, L, Power.FLING, 0);
        d.tapKey(0, L);
        float row = L.playTop + 320f;
        GameCore.Enemy a = add(d, L, new int[] {1}, row);
        GameCore.Enemy b = add(d, L, new int[] {2}, row);
        a.baseX = L.playLeft + L.enemyR * 2f;
        b.baseX = L.playRight - L.enemyR * 2f;
        check("no slow motion to begin with", d.slowdown == 0f && d.timeScale() == 1f);
        d.beginStroke(L.playLeft, row);
        d.sliceTo(L.playRight, row, L);
        check("both words went in one stroke",
                a.destroyed && b.destroyed && d.strokeKills == 2);
        check("that earns the beat", d.slowdown > 0f);
        check("and the world actually slows",
                Math.abs(d.timeScale() - Blade.SLOW_RATE) < 0.001f);
        check("the beat is brief", Blade.SLOW_TIME <= 0.35f);
        check("the readout outlives it",
                Blade.SLICE_CALL_TIME > Blade.SLOW_TIME && d.sliceCall > 0f);
        d.endStroke();

        // It runs on real time: slowing the world must not slow its own expiry.
        float was = d.slowdown;
        d.update(DT, L);
        check("it ticks down by real time, not scaled time",
                Math.abs((was - d.slowdown) - DT) < 0.0005f);
        advance(d, L, Blade.SLOW_TIME + 0.1f);
        check("it ends by itself", d.slowdown == 0f && d.timeScale() == 1f);
        check("the readout is still up at normal speed", d.sliceCall > 0f);
        advance(d, L, Blade.SLICE_CALL_TIME);
        check("and it clears in its own time", d.sliceCall == 0f);

        // A word falls slower while it lasts, which is the whole point.
        GameCore f = new GameCore(new Mem(), 235L);
        f.startGame();
        f.enemies.clear();
        GameCore.Enemy slow = add(f, L, new int[] {1}, L.playTop + 100f);
        slow.speed = 200f;
        f.update(DT, L);
        float normal = slow.y - (L.playTop + 100f);
        slow.y = L.playTop + 100f;
        f.slowdown = Blade.SLOW_TIME;
        f.update(DT, L);
        float slowed = slow.y - (L.playTop + 100f);
        check("a word falls slower during the beat", slowed < normal * 0.5f);

        // The counts survive the stroke that made them, for the readout, and reset on the next.
        check("the counts outlive the stroke", d.strokeKills == 2);
        d.beginStroke(0f, row);
        check("a new stroke starts them over", d.strokeKills == 0 && d.strokeCuts == 0);
        check("the trail is twice what it was", Blade.TRAIL_RATE == 100f);
    }

    /**
     * A core mid-FLING with an empty, quiet field.
     *
     * Spawning is held off for the length of the test: these are timing tests, and a word arriving
     * of its own accord would wander into a slice and make the counts a matter of luck.
     */
    private static GameCore flingCore(Layout L, long seed) {
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.FLING, 0);
        c.tapKey(0, L);
        c.stageGap = 30f;
        return c;
    }

    /**
     * What ends a stroke: the dwell, the cap, and what a new one starts with.
     *
     * The rule under test is that a stroke is a *motion*, not a touch. Before this, a finger parked
     * on the glass held one combo open for the whole frenzy, so "N IN ONE!" was a number you waited
     * for rather than earned.
     */
    static void strokeEnd(Layout L) {
        group("what ends a fling stroke");
        check("a dwell is a beat, not a pause",
                Blade.STROKE_DWELL >= 0.15f && Blade.STROKE_DWELL <= 0.3f);
        check("a definite move is more than jitter and less than a tile",
                Blade.STROKE_MOVE > 0.2f && Blade.STROKE_MOVE < 1f);
        check("the cap is past any real slice", Blade.STROKE_MAX >= 1.5f);
        check("the blade dies faster than the dwell that killed it",
                Blade.STROKE_FADE < Blade.STROKE_DWELL);
        // The speed a finger has to hold to stay awake. A slice worth calling travels ten times
        // this, which is the margin that keeps a real swipe from ever being cut off mid-motion.
        float keepAwake = L.enemyR * Blade.STROKE_MOVE / Blade.STROKE_DWELL;
        check("staying awake asks for a crawl, not a sprint", keepAwake < L.w * 0.12f);

        // Holding still ends the stroke without the finger going anywhere.
        GameCore c = flingCore(L, 241L);
        float row = L.playTop + 300f;
        GameCore.Enemy e = add(c, L, new int[] {1, 2, 3, 4}, row);
        c.beginStroke(c.tileX(e, 0, L) - L.enemyR * 2f, row);
        c.sliceTo(c.tileX(e, 3, L) + L.enemyR * 2f, row, L);
        check("the sweep took the word", c.strokeKills == 1 && c.fingerDown && c.touchDown);
        advance(c, L, Blade.STROKE_DWELL + 2 * DT);
        check("holding still ends the stroke", !c.fingerDown);
        check("but the finger is still on the glass", c.touchDown);
        check("and the edge is left dying where it stopped", c.strokeFade > 0f);
        check("the counts stand for the readout", c.strokeKills == 1);
        advance(c, L, Blade.STROKE_FADE + 2 * DT);
        check("the edge goes out in its own time", c.strokeFade == 0f);

        // A beat shorter than the dwell is not a stop.
        GameCore b = flingCore(L, 242L);
        b.beginStroke(L.w * 0.5f, row);
        advance(b, L, Blade.STROKE_DWELL - 0.06f);
        check("a shorter hesitation leaves the stroke alone", b.fingerDown);

        // A finger resting on a screen still reports a pixel or two a frame. Summing the path
        // would let a tremble hold a combo open, which is why the test is from an anchor.
        GameCore j = flingCore(L, 243L);
        float jx = L.w * 0.5f;
        j.beginStroke(jx, row);
        for (int i = 0; i < 30; i++) {
            j.sliceTo(jx + (i % 2 == 0 ? 1f : -1f) * L.enemyR * Blade.STROKE_MOVE * 0.4f,
                    row, L);
            j.update(DT, L);
        }
        check("a trembling finger does not hold the stroke open", !j.fingerDown);

        // The thing the mode exists for: a fast sweep through four words, in per-frame samples the
        // way a real swipe arrives, must run to the end of the motion as one stroke.
        GameCore f = flingCore(L, 244L);
        float span = L.playRight - L.playLeft;
        for (int i = 0; i < 4; i++) {
            GameCore.Enemy one = add(f, L, new int[] {i + 1}, row);
            one.baseX = L.playLeft + span * (i + 0.5f) / 4f;
        }
        f.beginStroke(L.playLeft - L.enemyR, row);
        boolean live = true;
        for (int i = 1; i <= 12; i++) {
            f.sliceTo(L.playLeft - L.enemyR + (span + L.enemyR * 2f) * i / 12f, row, L);
            f.update(DT, L);
            if (!f.fingerDown) live = false;
        }
        check("a fast multi-word slice is never cut short", live);
        check("all four went on the one stroke", f.strokeKills == 4);
        check("and the readout says so", f.callKills == 4 && f.sliceCall > 0f);

        // The loophole the cap closes: a finger swinging clear of the anchor every frame is
        // technically always moving, so the dwell alone would let it hold a combo open for ever.
        // It takes a wide, fast wiggle — anything smaller than a definite move reads as the
        // tremble above and rests — which is why this is a backstop and not the main mechanism.
        GameCore g = flingCore(L, 245L);
        float gx = L.w * 0.5f, amp = L.enemyR * Blade.STROKE_MOVE * 1.2f;
        g.beginStroke(gx, row);
        int frames = 0;
        while (g.fingerDown && frames < 60 * 4) {
            g.sliceTo(gx + (frames % 2 == 0 ? amp : -amp), row, L);
            g.update(DT, L);
            frames++;
        }
        float lived = frames * DT;
        check("wobbling in place does keep the dwell at bay", lived > Blade.STROKE_DWELL * 2f);
        check("but the cap ends it anyway", !g.fingerDown);
        check("and only well after any real slice would have finished",
                lived >= Blade.STROKE_MAX - 0.05f);

        // Waking: one touch may hold several strokes, and each counts for itself.
        GameCore h = flingCore(L, 246L);
        GameCore.Enemy w1 = add(h, L, new int[] {1, 2}, row);
        h.beginStroke(h.tileX(w1, 0, L) - L.enemyR * 2f, row);
        h.sliceTo(h.tileX(w1, 1, L) + L.enemyR * 2f, row, L);
        check("the first stroke took its word", h.strokeKills == 1);
        advance(h, L, Blade.STROKE_DWELL + 2 * DT);
        check("it rested under the finger", !h.fingerDown && h.touchDown);
        int nudge = h.sliceTo(h.fingerX + L.enemyR * Blade.STROKE_MOVE * 0.5f, row, L);
        check("a nudge does not wake it", !h.fingerDown && nudge == 0);
        float far = L.playLeft;
        h.sliceTo(far, row, L);
        check("a definite move starts a new stroke", h.fingerDown);
        check("with the combo back at nothing", h.strokeKills == 0 && h.strokeCuts == 0);
        check("and the blade lit again", h.strokeFade == 0f);
        check("the finger never lifted for any of that", h.touchDown);

        // The readout belongs to the stroke that earned it. It outlives that stroke on purpose,
        // and a fresh stroke starting inside its 1.1s must not rewrite the number it is showing.
        GameCore k = flingCore(L, 247L);
        GameCore.Enemy k1 = add(k, L, new int[] {1}, row);
        GameCore.Enemy k2 = add(k, L, new int[] {2}, row);
        k1.baseX = L.playLeft + L.enemyR * 2f;
        k2.baseX = L.playRight - L.enemyR * 2f;
        float low = row + L.enemyR * 6f;
        GameCore.Enemy k3 = add(k, L, new int[] {3}, low);
        k.beginStroke(L.playLeft, row);
        k.sliceTo(L.playRight, row, L);
        check("two words in one stroke earned the call", k.callKills == 2 && k.sliceCall > 0f);
        advance(k, L, Blade.STROKE_DWELL + 2 * DT);
        k.sliceTo(L.playLeft, low, L);              // wakes a second stroke on the lower row
        k.sliceTo(L.playRight, low, L);
        check("the second stroke took the third word", k3.destroyed && k.strokeKills == 1);
        check("the readout still belongs to the stroke that earned it",
                k.callKills == 2 && k.sliceCall > 0f);

        // A stroke has two exits, and dying mid-swipe is the one that never reaches the PLAY half
        // of the loop. The finger has to be let go there too, or a blade hangs over the summary.
        GameCore m = flingCore(L, 248L);
        m.beginStroke(L.w * 0.5f, row);
        m.lives = 1;
        m.enemies.clear();
        m.stageGap = 0f;
        add(m, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(m, L, GameCore.ATTACK_TIME + 2 * DT);
        check("dying ends the run", m.state == GameCore.OVER);
        check("and lets go of the finger", !m.fingerDown && !m.touchDown);

        // A rested stroke stops laying the ribbon too: that dying trail is how the end of a swipe
        // is seen rather than inferred from the readout.
        GameCore t = flingCore(L, 249L);
        t.beginStroke(L.w * 0.4f, L.h * 0.4f);
        advance(t, L, Blade.STROKE_DWELL + 2 * DT);
        t.particles.clear();
        advance(t, L, 0.3f);
        check("a rested stroke lays no trail", t.particles.isEmpty());
    }

    static void flingMode(Layout L) {
        group("FLING");
        GameCore c = new GameCore(new Mem(), 208L);
        c.startGame();
        c.enemies.clear();
        c.target = null;
        place(c, L, Power.FLING, 0);
        c.tapKey(0, L);
        check("fling is running", c.flinging());

        GameCore.Enemy e = add(c, L, new int[] {1, 2, 3}, L.playTop + 300);

        // The blade cuts what a stroke sweeps past, and nothing when it sweeps past nothing.
        c.beginStroke(c.tileX(e, 1, L) - L.enemyR * 4f, e.y);
        check("empty air cuts nothing",
                c.sliceTo(c.tileX(e, 1, L) - L.enemyR * 3f, L.playTop + 10f, L) == 0);
        c.endStroke();

        c.removeTile(e, 1, 1f, -1f, L);
        check("the middle letter is gone out of order", e.gone[1]);
        check("typing position did not move", e.pos == 0);
        check("a gone tile owes no presses", c.pressesLeft(e, 1) == 0);
        check("it is marked resolved", e.resolved(1));
        check("the word is not finished", e.typeable());

        // Typing then skips over it.
        c.tapKey(1, L);
        check("typing steps past the gap", e.pos == 2);
        c.tapKey(3, L);
        advance(c, L, 0.3f);
        check("the word finishes normally", e.destroyed);

        // Flinging the last remaining letters finishes the word directly.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy f = add(c, L, new int[] {5, 5}, L.playTop + 300);
        c.removeTile(f, 1, 1f, 0f, L);
        check("still going with one letter left", !f.destroyed);
        c.removeTile(f, 0, -1f, 0f, L);
        check("flinging the last letter destroys the word", f.destroyed);
    }

    static void trail(Layout L) {
        group("fling trail");
        GameCore c = new GameCore(new Mem(), 210L);
        c.startGame();
        c.enemies.clear();
        c.particles.clear();
        place(c, L, Power.FLING, 0);
        c.tapKey(0, L);
        check("fling is running", c.flinging());
        check("the hint shows before any touch", c.showFlingHint());

        // Untouched, the demonstration emits its own trail along a moving path.
        c.particles.clear();
        float x0 = c.demoX;
        advance(c, L, 0.4f);
        check("the demo path moves", c.demoX != x0);
        check("the demo lays a trail", c.particles.size() > 5);
        check("the demo stays on screen",
                c.demoX > 0f && c.demoX < L.w && c.demoY > 0f && c.demoY < L.deckTop);

        // Touching retires the hint and moves the trail under the finger. The finger is kept
        // creeping along: the trail follows a motion now, and a stroke that stops moving is rested
        // out from under it by the dwell.
        c.beginStroke(L.w * 0.3f, L.h * 0.3f);
        c.particles.clear();
        for (int i = 1; i <= 12; i++) {
            c.sliceTo(L.w * 0.3f + i * L.enemyR * 0.15f, L.h * 0.3f, L);
            c.update(DT, L);
        }
        check("the stroke is still going", c.fingerDown);
        check("the hint retires after a touch", !c.showFlingHint());
        check("the trail follows the finger", c.particles.size() > 3);
        boolean nearFinger = true;
        for (int i = 0; i < c.particles.size(); i++) {
            GameCore.Particle q = c.particles.get(i);
            if (Math.abs(q.x - c.fingerX) > L.w * 0.25f) nearFinger = false;
        }
        check("sparkles appear at the finger", nearFinger);

        // Lifting the finger stops it; so does the frenzy ending.
        c.endStroke();
        c.particles.clear();
        advance(c, L, 0.3f);
        check("lifting off stops the trail", c.particles.isEmpty());
        check("and lets go of the glass", !c.touchDown);

        c.beginStroke(L.w * 0.3f, L.h * 0.3f);
        c.modeLeft = 0.001f;
        advance(c, L, 0.2f);
        check("the frenzy ending releases the finger", !c.fingerDown && !c.touchDown);
        check("no trail once fling is over", !c.flinging());

        // Other modes must not emit a trail or show the hint.
        GameCore d = new GameCore(new Mem(), 211L);
        d.startGame();
        d.enemies.clear();
        place(d, L, Power.MULTI, 0);
        d.tapKey(0, L);
        d.particles.clear();
        advance(d, L, 0.4f);
        check("no fling hint in other modes", !d.showFlingHint());
        check("no trail in other modes", d.particles.isEmpty());

        check("words arrive six times faster during an opening-stage frenzy",
                Power.SPAWN_RATE == 6f && Power.spawnRate(0f) == 6f);
    }

    /**
     * The frenzy taper: what a frenzy is allowed to ask of the player, stage by stage.
     *
     * The thing being pinned is a *ratio*. A frenzy's press demand is exactly its spawn multiplier
     * times the stage's own demand — words cost the same to clear either way, they just turn up
     * more often — so holding the multiplier down is the whole of holding the demand down. Flat
     * multipliers compounded with the ramp: stage 10 asked 23 presses a second and stage 22 asked
     * 43, against a human ceiling of maybe 8.
     */
    static void frenzyTaper(Layout L) {
        group("frenzy taper");

        check("an opening-stage frenzy is untapered", Power.taper(0f) == 1f);
        float floor = (Power.LATE_RATIO - 1f) / (Power.SPAWN_RATE - 1f);
        check("it bottoms out at the ratio the target implies",
                Math.abs(Power.taper(99f) - floor) < 1e-6f);
        check("and a late frenzy asks exactly the target",
                Math.abs(Power.spawnRate(99f) - Power.LATE_RATIO) < 1e-4f);

        // Monotonic, and never slower than the stage it interrupts.
        boolean falling = true, aboveOne = true;
        float last = Power.spawnRate(0f);
        for (float ramp = 0f; ramp <= 20f; ramp += 0.25f) {
            float now = Power.spawnRate(ramp);
            if (now > last + 1e-6f) falling = false;
            if (Power.spawnRate(ramp) < 1f || Power.crowdRate(ramp) < 1f
                    || Power.fallRate(ramp) < 1f) {
                aboveOne = false;
            }
            last = now;
        }
        check("the taper never turns back up", falling);
        check("and a frenzy is never slower than its own stage", aboveOne);

        // The demand itself, measured the way the player feels it: presses a second needed to clear
        // words as fast as a frenzy sends them, off the real word generator rather than an estimate.
        //
        // Two separate things are being held here, and it matters which is which. The taper owns the
        // *ratio* — what a frenzy adds to its own stage. It cannot own the absolute number, because
        // that is the ramp's, and by stage 25 ordinary play already wants 8.8 presses a second all
        // by itself. So the absolute cap is asserted over the stretch where the ramp is still sane,
        // and past that the assertion is only that a frenzy is no worse than twice its stage.
        GameCore c = new GameCore(new Mem(), 941L);
        c.startGame();
        float worst = 0f, worstFlat = 0f, worstEarly = 0f;
        int worstStage = 0;
        boolean withinTarget = true;
        float floorAt = (Power.LATE_RATIO - 1f) / (Power.SPAWN_RATE - 1f) + 1e-6f;
        for (int stage = 1; stage <= 25; stage++) {
            c.stage = stage;
            float calm = pressesPerWord(c, 719L + stage) / c.spawnInterval();
            float demand = calm * Power.spawnRate(c.ramp());
            float flat = calm * Power.SPAWN_RATE;
            // Past the point the taper bottoms out, a frenzy may only ask the target multiple.
            if (Power.taper(c.ramp()) <= floorAt && demand > calm * Power.LATE_RATIO + 1e-3f) {
                withinTarget = false;
            }
            if (demand > worst) {
                worst = demand;
                worstStage = stage;
                worstFlat = flat;
            }
            if (stage <= 19 && demand > worstEarly) worstEarly = demand;
            if (stage % 6 == 1) {
                System.out.printf("    stage %-2d  spawn x%.2f  %5.1f presses/s "
                        + "(flat rates: %.1f, calm stage: %.1f)%n",
                        stage, Power.spawnRate(c.ramp()), demand, flat, calm);
            }
        }
        check("a bottomed-out frenzy asks only the target multiple of its stage", withinTarget);
        System.out.printf("    worst is %.1f presses/s at stage %d, where flat rates asked %.1f%n",
                worst, worstStage, worstFlat);
        // A frenzy is still meant to be a scramble, so this is not comfortable — but it is inside
        // what two thumbs can do in bursts, which 23 and 43 were not.
        check("no frenzy up to stage 19 asks more than 12 presses a second", worstEarly < 12f);
        check("and the worst stage of all is a real improvement on flat rates",
                worst < worstFlat * 0.4f);
    }

    static void frenzyFallSpeed(Layout L) {
        group("frenzy fall speed");
        GameCore c = new GameCore(new Mem(), 212L);
        c.startGame();
        check("normal fall rate outside a frenzy", c.fallRate() == 1f);

        // A word already on screen when the frenzy starts must speed up too.
        c.enemies.clear();
        GameCore.Enemy e = add(c, L, new int[] {1, 2}, L.playTop + 100);
        e.speed = 200f;
        c.update(DT, L);
        float slowStep = e.y - (L.playTop + 100f);

        place(c, L, Power.MULTI, 0);
        c.tapKey(0, L);
        check("frenzy doubles the fall rate", c.fallRate() == Power.FALL_RATE);
        float before = e.y;
        c.update(DT, L);
        float fastStep = e.y - before;
        check("an in-flight word speeds up mid-fall",
                Math.abs(fastStep - slowStep * Power.FALL_RATE) < 0.01f);

        // And slows back down when it lapses.
        c.modeLeft = 0f;
        c.mode = -1;
        before = e.y;
        c.update(DT, L);
        check("it slows back down afterwards",
                Math.abs((e.y - before) - slowStep) < 0.01f);
        check("fall rate is back to normal", c.fallRate() == 1f);

        // The player's own speed setting still compounds with it.
        c.setSpeed(GameCore.SPEED_MAX);
        float fastTravel = c.travelSeconds();
        c.setSpeed(GameCore.SPEED_MIN);
        check("the speed setting is independent of the frenzy",
                c.travelSeconds() > fastTravel);

        // Dying mid-frenzy must end it. updatePower only runs during PLAY, so without an
        // explicit teardown the mode stayed live and the driven music carried on into the
        // game-over screen.
        GameCore d = new GameCore(new Mem(), 213L);
        Ear ear = new Ear();
        d.sound = ear;
        d.startGame();
        place(d, L, Power.FLURRY, 0);
        d.tapKey(0, L);
        check("frenzy is running before the last hit", d.powerActive());
        int fc = ear.frenzyCalls;

        d.lives = 1;
        d.enemies.clear();
        add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d, L, GameCore.ATTACK_TIME + 4 * DT);
        check("the run ended", d.state == GameCore.OVER);
        check("dying ends the frenzy", !d.powerActive() && d.mode == -1);
        check("and restores the music", ear.frenzyCalls == fc + 1 && !ear.frenzyOn);
        check("and drops the powerup letter", d.power == null);
        advance(d, L, 1.0f);
        check("it stays ended", !d.powerActive());
    }

    static void playtest(Layout L) {
        group("playtest hook");
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        check("panel still fits with the playtest row",
                ui.panelB <= L.h && ui.testY + ui.testH < ui.panelB);
        check("the playtest row sits below the music rows",
                ui.testY > ui.optionCy(Music.NAMES.length - 1));

        // Every chip in the row, with shortcuts for both between-stage games after the modes. Written against TEST_CHIPS rather than Power.COUNT so adding another
        // playtest shortcut cannot quietly leave the new chip untestable.
        boolean chipsOk = true, chipsDistinct = true;
        int chips = SettingsUi.TEST_CHIPS;
        for (int i = 0; i < chips; i++) {
            float cx = (ui.testChipL(i, chips) + ui.testChipR(i, chips)) / 2f;
            if (ui.hit(cx, ui.testY + ui.testH / 2f) != SettingsUi.HIT_TEST + i) chipsOk = false;
            if (i > 0 && ui.testChipL(i, chips)
                    < ui.testChipR(i - 1, chips)) chipsDistinct = false;
        }
        check("the row has chips for both between-stage games",
                SettingsUi.TEST_STARS == Power.COUNT
                        && SettingsUi.TEST_STEAMER == Power.COUNT + 1
                        && chips == Power.COUNT + 2);
        // Labels inside their boxes, which nothing was checking: adding the fifth chip put TEAM
        // SQUISH's label across two of its neighbours, and DOES NOT FIT only watches the screen
        // edge. Measured in the harness font, which is wider than the device's.
        float chipType = Draw.type(L.unit * 0.46f);
        boolean labelsFit = true;
        String widest = "";
        float worst = 0f;
        for (int i = 0; i < chips; i++) {
            String label = i == SettingsUi.TEST_STARS ? "PATH"
                    : i == SettingsUi.TEST_STEAMER ? "STEAM" : Power.CHIP[i];
            float box = ui.testChipR(i, chips) - ui.testChipL(i, chips);
            float wide = RasterPainter.textWidth(label, chipType);
            if (wide / box > worst) { worst = wide / box; widest = label; }
            if (wide > box * 0.94f) labelsFit = false;
        }
        System.out.printf("    the widest chip label is %s at %.0f%% of its box%n",
                widest, worst * 100f);
        check("every chip label fits inside its chip", labelsFit);
        check("every playtest chip is hittable", chipsOk);
        check("the chips do not overlap", chipsDistinct);
        check("chips stay inside the panel",
                ui.testChipL(0, chips) >= ui.panelL
                        && ui.testChipR(chips - 1, chips) <= ui.panelR);

        // Each chip starts the real thing. The case needs something in it, because TEAM SQUISH
        // fields one of the collection and refuses to start without one.
        for (int m = 0; m < Power.COUNT; m++) {
            Mem store = new Mem();
            store.collected = 0b1001L;
            GameCore c = new GameCore(store, 300L + m);
            Ear ear = new Ear();
            c.sound = ear;
            c.startGame();
            c.openSettings();
            int fc0 = ear.frenzyCalls;
            c.playtestMode(m, L);
            check("playtest " + Power.NAMES[m] + " starts that mode",
                    c.powerActive() && c.mode == m);
            check("playtest " + Power.NAMES[m] + " runs the full duration",
                    c.modeLeft > Power.DURATION - 0.01f);
            check("playtest " + Power.NAMES[m] + " closes the panel", !c.settingsOpen);
            check("playtest " + Power.NAMES[m] + " swaps the music",
                    ear.frenzyCalls == fc0 + 1 && ear.frenzyOn);
            check("playtest " + Power.NAMES[m] + " awards no score", c.score == 0);

            // And it ends like any other frenzy: stage cleared, interlude follows.
            c.lives = 50;                    // as above: testing the timer, not survival
            advance(c, L, Power.DURATION + 0.1f);
            check("playtest " + Power.NAMES[m] + " ends the stage",
                    c.spawnedThisStage >= c.stageQuota());
        }

        GameCore steam = new GameCore(new Mem(), 319L);
        steam.startGame();
        steam.openSettings();
        steam.playtestSteamer(L);
        check("playtest STEAM starts the steamer game",
                steam.state == GameCore.BONUS && !steam.starBonus);
        check("playtest STEAM closes the panel", !steam.settingsOpen);
        check("playtest STEAM gives a full mash window",
                steam.bonusTimer == GameCore.bonusLength(GameCore.MASH_PERFECT));

        // Only meaningful during play, and only for a real mode.
        GameCore t = new GameCore(new Mem(), 320L);
        check("ignored on the title screen", !titleAccepts(t, L));
        t.startGame();
        t.playtestMode(-1, L);
        check("a bogus mode is ignored", !t.powerActive());
        t.playtestMode(Power.COUNT, L);
        check("an out-of-range mode is ignored", !t.powerActive());
    }

    private static boolean titleAccepts(GameCore c, Layout L) {
        c.playtestMode(Power.FLURRY, L);
        return c.powerActive();
    }

    /**
     * A long run per seed, checking that nothing goes insane and that frenzies keep happening.
     *
     * Three seeds, and the count is averaged, because one seed is not a measurement. It used to be a
     * single run asserting {@code frenzies >= 2} and it passed with four — until boss stages, which
     * release no powerups at all, shifted the RNG enough that the same assertion saw one. Nothing was
     * broken either time: the metric is simply noisy over one run, which is the trap CLAUDE.md
     * records about small-sample yes/no checks.
     */
    static void soak(Layout L) {
        group("powerup soak");
        int totalFrenzies = 0;
        boolean sane = true;
        int seeds = 0;
        for (long seed : new long[] {209L, 977L, 4111L}) {
            seeds++;
            if (!soakRun(L, seed)) sane = false;
            totalFrenzies += lastFrenzies;
            System.out.printf("    seed %-5d %d frenzies in %.0fs, stage %d%n", seed,
                    lastFrenzies, lastFrames * DT, lastStage);
        }
        float mean = totalFrenzies / (float) seeds;
        System.out.printf("    %.1f frenzies a run on average%n", mean);
        check("frenzies happen over a long run", mean >= 2f);
        check("state stays consistent throughout", sane);
    }

    private static int lastFrenzies, lastFrames, lastStage;

    /** One run. Returns false if anything went out of range on the way. */
    private static boolean soakRun(Layout L, long seed) {
        GameCore c = new GameCore(new Mem(), seed);
        c.sound = new Ear();
        c.startGame();
        int frenzies = 0, frames = 0;
        boolean sane = true;
        boolean wasActive = false;
        while (frames < 60 * 400 && c.state != GameCore.OVER) {
            c.update(DT, L);
            frames++;
            if (!wasActive && c.powerActive()) frenzies++;
            wasActive = c.powerActive();

            if (c.modeLeft < 0f || c.mode < -1 || c.mode >= Power.COUNT) sane = false;
            if (c.power != null && c.power.hit && c.power.hitT > Power.POP_TIME + 1f) {
                sane = false;
            }
            if (c.lives < 0 || c.score < 0) sane = false;

            if (frames % 2 != 0) continue;
            if (c.state == GameCore.BONUS) {
                c.tapBonus(c.steamer.wanted());
                continue;
            }
            if (c.state != GameCore.PLAY) continue;
            // A boss stage has to be fought or the run stops here, and a run that stops at stage 5
            // sees no frenzies at all — this is real play, so it plays the boss rather than
            // wishing it away the way modeSpread does.
            if (bossPlay(c, L)) continue;
            GameCore.Enemy e = c.target != null && c.enemies.contains(c.target)
                    && c.target.typeable() ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
            else c.tapKey(frames % Glyph.COUNT, L);
        }
        lastFrenzies = frenzies;
        lastFrames = frames;
        lastStage = c.stage;
        return sane;
    }
}
