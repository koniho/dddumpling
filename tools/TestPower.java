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

        // Words arrive four times faster, and the sky runs four times faster.
        c.stage = 3;
        float fast = c.spawnInterval() / Power.SPAWN_RATE;
        check("the spawn interval is quartered", fast < c.spawnInterval());
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
        check("the interlude runs longer", c.bonusTimer > GameCore.bonusLength());
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
        check("a normal interlude is the usual length",
                d.bonusTimer < GameCore.bonusLength() + 0.1f);
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

    /** The blade: what one stroke cuts, and the beat a multi-word stroke earns. */
    static void blade(Layout L) {
        group("FLING blade");
        // Distance from a point to a segment, which is what decides every cut.
        check("a point on the segment is at no distance",
                GameCore.segDist2(5f, 0f, 0f, 0f, 10f, 0f) < 0.001f);
        check("perpendicular offset is measured square",
                Math.abs(GameCore.segDist2(5f, 3f, 0f, 0f, 10f, 0f) - 9f) < 0.001f);
        check("past an end clamps to that end",
                Math.abs(GameCore.segDist2(-4f, 0f, 0f, 0f, 10f, 0f) - 16f) < 0.001f);
        check("a stationary finger degenerates to a point",
                Math.abs(GameCore.segDist2(3f, 4f, 0f, 0f, 0f, 0f) - 25f) < 0.001f);
        check("the blade is wider than a tile", GameCore.BLADE > 1f);

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
                Math.abs(d.timeScale() - GameCore.SLOW_RATE) < 0.001f);
        check("the beat is brief", GameCore.SLOW_TIME <= 0.35f);
        check("the readout outlives it",
                GameCore.SLICE_CALL_TIME > GameCore.SLOW_TIME && d.sliceCall > 0f);
        d.endStroke();

        // It runs on real time: slowing the world must not slow its own expiry.
        float was = d.slowdown;
        d.update(DT, L);
        check("it ticks down by real time, not scaled time",
                Math.abs((was - d.slowdown) - DT) < 0.0005f);
        advance(d, L, GameCore.SLOW_TIME + 0.1f);
        check("it ends by itself", d.slowdown == 0f && d.timeScale() == 1f);
        check("the readout is still up at normal speed", d.sliceCall > 0f);
        advance(d, L, GameCore.SLICE_CALL_TIME);
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
        f.slowdown = GameCore.SLOW_TIME;
        f.update(DT, L);
        float slowed = slow.y - (L.playTop + 100f);
        check("a word falls slower during the beat", slowed < normal * 0.5f);

        // The counts survive the stroke that made them, for the readout, and reset on the next.
        check("the counts outlive the stroke", d.strokeKills == 2);
        d.beginStroke(0f, row);
        check("a new stroke starts them over", d.strokeKills == 0 && d.strokeCuts == 0);
        check("the trail is twice what it was", GameCore.TRAIL_RATE == 100f);
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

        // Touching retires the hint and moves the trail under the finger.
        c.flingUsed = true;
        c.fingerDown = true;
        c.fingerX = L.w * 0.3f;
        c.fingerY = L.h * 0.3f;
        c.particles.clear();
        advance(c, L, 0.2f);
        check("the hint retires after a touch", !c.showFlingHint());
        check("the trail follows the finger", c.particles.size() > 3);
        boolean nearFinger = true;
        for (int i = 0; i < c.particles.size(); i++) {
            GameCore.Particle q = c.particles.get(i);
            if (Math.abs(q.x - c.fingerX) > L.w * 0.25f) nearFinger = false;
        }
        check("sparkles appear at the finger", nearFinger);

        // Lifting the finger stops it; so does the frenzy ending.
        c.fingerDown = false;
        c.particles.clear();
        advance(c, L, 0.3f);
        check("lifting off stops the trail", c.particles.isEmpty());

        c.fingerDown = true;
        c.modeLeft = 0.001f;
        advance(c, L, 0.2f);
        check("the frenzy ending releases the finger", !c.fingerDown);
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

        check("words arrive six times faster during a frenzy", Power.SPAWN_RATE == 6f);
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

        boolean chipsOk = true, chipsDistinct = true;
        for (int i = 0; i < Power.COUNT; i++) {
            float cx = (ui.testChipL(i, Power.COUNT) + ui.testChipR(i, Power.COUNT)) / 2f;
            if (ui.hit(cx, ui.testY + ui.testH / 2f) != SettingsUi.HIT_TEST + i) chipsOk = false;
            if (i > 0 && ui.testChipL(i, Power.COUNT)
                    < ui.testChipR(i - 1, Power.COUNT)) chipsDistinct = false;
        }
        check("every playtest chip is hittable", chipsOk);
        check("the chips do not overlap", chipsDistinct);
        check("chips stay inside the panel",
                ui.testChipL(0, Power.COUNT) >= ui.panelL
                        && ui.testChipR(Power.COUNT - 1, Power.COUNT) <= ui.panelR);

        // Each chip starts the real thing.
        for (int m = 0; m < Power.COUNT; m++) {
            GameCore c = new GameCore(new Mem(), 300L + m);
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

    static void soak(Layout L) {
        group("powerup soak");
        GameCore c = new GameCore(new Mem(), 209L);
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
            GameCore.Enemy e = c.target != null && c.enemies.contains(c.target)
                    && c.target.typeable() ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
            else c.tapKey(frames % Glyph.COUNT, L);
        }
        System.out.printf("    %d frenzies in %.0fs, stage %d, score %d%n",
                frenzies, frames * DT, c.stage, c.score);
        check("frenzies happen over a long run", frenzies >= 2);
        check("state stays consistent throughout", sane);
    }
}
