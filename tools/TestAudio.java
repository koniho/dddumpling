package com.sram.hexatype;

/** Effect normalisation and which sound fires on which event. */
final class TestAudio extends Check {

    /** What the two frenzy squish sounds are, and that they are the right shape for the job. */
    static void frenzySounds(Layout L) {
        group("frenzy sounds");
        // The chop fires several times per swipe, so anything with a tail would smear.
        short[] chop = Sfx.build(Sfx.CHOP);
        float chopLen = (float) chop.length / Sfx.RATE;
        System.out.printf("    chop is %.0fms, squish is %.0fms%n", chopLen * 1000f,
                1000f * Sfx.build(Sfx.SQUISH_0).length / Sfx.RATE);
        check("the chop is short enough to repeat", chopLen < 0.09f);
        check("and shorter than a squish",
                chop.length < Sfx.build(Sfx.SQUISH_0).length);
        // It has to actually decay, or a run of them builds into a wash.
        int head = 0, tail = 0;
        for (int i = 0; i < chop.length / 4; i++) head = Math.max(head, Math.abs(chop[i]));
        for (int i = chop.length * 3 / 4; i < chop.length; i++) {
            tail = Math.max(tail, Math.abs(chop[i]));
        }
        check("the chop dies away", tail < head / 4);

        // Body, not hiss. A noise burst crosses zero constantly; a sound with a tone under it
        // does not, so the crossing rate is the cheapest measure that tells them apart. The
        // first chop was noise alone and audibly a hiss.
        System.out.printf("    zero-crossings per second: chop %.0f, squish %.0f, wrong %.0f%n",
                crossRate(chop), crossRate(Sfx.build(Sfx.SQUISH_0)),
                crossRate(Sfx.build(Sfx.WRONG)));
        check("the chop has body under the air", crossRate(chop) < 3500f);
        check("but is still brighter than a squish",
                crossRate(chop) > crossRate(Sfx.build(Sfx.SQUISH_0)));

        // The chain crack: it has to hit hard and immediately, which is the whole difference
        // between a bolt and a fizz.
        short[] zap = Sfx.build(Sfx.ZAP);
        int peakAt = 0, peak = 0;
        for (int i = 0; i < zap.length; i++) {
            if (Math.abs(zap[i]) > peak) {
                peak = Math.abs(zap[i]);
                peakAt = i;
            }
        }
        float toPeak = 1000f * peakAt / Sfx.RATE;
        System.out.printf("    zap is %.0fms, peaks at %.1fms, %.0f crossings/s%n",
                1000f * zap.length / Sfx.RATE, toPeak, crossRate(zap));
        check("the zap hits immediately", toPeak < 12f);
        check("it is the bigger event of the two", zap.length > chop.length);
        check("it has low-end punch, not just crackle", crossRate(zap) < 3000f);
        int zTail = 0;
        for (int i = zap.length * 3 / 4; i < zap.length; i++) {
            zTail = Math.max(zTail, Math.abs(zap[i]));
        }
        check("and it decays", zTail < peak / 6);

        // MULTI: one crack per hop, climbing, and never a squish.
        GameCore m = new GameCore(new Mem(), 417L);
        Ear earM = new Ear();
        m.sound = earM;
        m.startGame();
        m.enemies.clear();
        m.target = null;
        m.startFrenzy(Power.MULTI, L);
        add(m, L, new int[] {1, 1}, L.playTop + 200f);
        add(m, L, new int[] {1, 1}, L.playTop + 320f);
        m.tapKey(1, L);
        check("the chain took four", m.chainLen == 4);
        check("silent until it plays back", earM.zaps == 0);
        advance(m, L, GameCore.CHAIN_TIME);
        check("one crack per hop", earM.zaps == 4);
        check("the last one is the highest", earM.lastZapHop == 4);
        check("no squishes in a chain", earM.squishes == 0);

        // FLING: one chop per letter the blade cuts, and no fanfare.
        GameCore c = new GameCore(new Mem(), 411L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.enemies.clear();
        c.target = null;
        c.startFrenzy(Power.FLING, L);
        GameCore.Enemy e = add(c, L, new int[] {1, 2, 3, 4}, L.playTop + 300f);
        int chops = ear.chops, cheers = ear.achievements;
        c.beginStroke(c.tileX(e, 0, L) - L.enemyR * 2f, e.y);
        int cut = c.sliceTo(c.tileX(e, 3, L) + L.enemyR * 2f, e.y, L);
        check("the blade cut the word", cut == 4);
        check("one chop per letter cut", ear.chops - chops == 4);
        check("and no fanfare for a single word", ear.achievements == cheers);
        // The chops are the word's sound. A clear tone on top lands on the last one.
        check("a word cut by the blade rings no clear tone", ear.clears == 0);
        c.endStroke();

        // Typed, it still does — the tone is what tells you a word is finished when there is no
        // chop to say so.
        GameCore t2 = new GameCore(new Mem(), 415L);
        Ear ear3 = new Ear();
        t2.sound = ear3;
        t2.startGame();
        t2.enemies.clear();
        GameCore.Enemy typed = add(t2, L, new int[] {1, 2}, L.playTop + 200f);
        t2.tapKey(1, L);
        t2.tapKey(2, L);
        advance(t2, L, 0.4f);
        check("a typed word still rings", typed.destroyed && ear3.clears == 1);
        check("and rang no chop", ear3.chops == 0);

        // TEAM SQUISH: a squish per word, not the achievement flourish — it fires far too often
        // for that, which is what it used to do.
        Mem store = new Mem();
        store.collected = (1L << 7) | (1L << 18);
        GameCore d = new GameCore(store, 413L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.startGame();
        d.enemies.clear();
        d.target = null;
        d.playtestMode(Power.TEAM, L);
        GameCore.Enemy prey = add(d, L, new int[] {2, 2}, d.buddy.y);
        prey.baseX = d.buddy.x;
        int sq = ear2.squishes, fan = ear2.achievements;
        d.update(DT, L);
        check("the squishy took the word", prey.destroyed);
        check("it squishes", ear2.squishes == sq + 1);
        check("and does not blow the achievement fanfare", ear2.achievements == fan);
        check("the squish is pitched by its size", ear2.lastDepth >= 1);
        check("on a letter of the word it took", ear2.lastGlyph == 2);

        // Bigger squishy, deeper squish: depth climbs, and depth is what lowers the pitch.
        d.buddy.squishes = 8;
        GameCore.Enemy again = add(d, L, new int[] {5, 5}, d.buddy.y);
        again.baseX = d.buddy.x;
        int wasDepth = ear2.lastDepth;
        d.update(DT, L);
        check("a grown squishy sounds deeper", ear2.lastDepth > wasDepth);
    }

    /** Zero crossings per second, as a rough stand-in for how tonal a buffer is. */
    private static float crossRate(short[] pcm) {
        int crossings = 0;
        for (int i = 1; i < pcm.length; i++) {
            if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++;
        }
        return crossings * (float) Sfx.RATE / pcm.length;
    }

    /** Which track the game starts on, and that the loaded choice actually gets announced. */
    static void musicChoice(Layout L) {
        group("music choice");
        check("a personal track is the default when there is one",
                Music.defaultChoice(true) == Music.CUSTOM);
        check("otherwise the first synth track is",
                Music.defaultChoice(false) == Music.SWING_STYLE);
        check("every name has a constant and vice versa",
                Music.NAMES.length == Music.CUSTOM + 1);
        check("the custom slot is not a synth style", !Music.isSynth(Music.CUSTOM));
        check("and neither is off", !Music.isSynth(Music.OFF));

        // The regression this exists for: the loaded preference was read into bgmChoice and
        // then never announced, so the backend fell back to its own first track on every
        // launch and both the stored choice and the first-run default did nothing.
        Mem store = new Mem();
        store.bgm = Music.CUSTOM;
        GameCore c = new GameCore(store, 301L);
        Ear ear = new Ear();
        c.sound = ear;
        check("the stored choice is loaded", c.bgmChoice == Music.CUSTOM);
        check("nothing is announced before it is asked for", ear.musicCalls == 0);
        c.startMusic();
        check("starting announces the loaded choice",
                ear.musicCalls == 1 && ear.music == Music.CUSTOM);

        // And with no sound attached it must not throw: the harness runs that way throughout.
        GameCore d = new GameCore(new Mem(), 302L);
        d.startMusic();
        check("announcing without a backend is harmless", d.bgmChoice == 0);

        // A deliberate later choice still wins, and is persisted.
        c.setBgm(Music.DRIFT);
        check("a later choice is announced", ear.music == Music.DRIFT && ear.musicCalls == 2);
        check("and saved", store.bgm == Music.DRIFT && store.bgmSaves == 1);
        c.setBgm(99);
        check("an out-of-range choice is refused", c.bgmChoice == Music.DRIFT);
    }

    static void audio(Layout L) {
        group("audio");
        int peak = (int) (Sfx.PEAK * 32767f);
        boolean allNormalised = true, allClean = true, allSane = true;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            // Peak-normalised: every effect tops out at the same level.
            if (Math.abs(max - peak) > 2) allNormalised = false;
            if (max >= 32767) allClean = false;
            if (pcm.length < Sfx.RATE / 20 || pcm.length > Sfx.RATE * 2) allSane = false;
        }
        check("every effect is normalised to the same peak", allNormalised);
        check("no effect clips", allClean);
        check("effect lengths are sane", allSane);

        short[] loop = Music.loop(Music.SWING_STYLE);
        check("music loop is the expected length", loop.length == Music.loopFrames(Music.SWING_STYLE));
        check("music loop is several seconds", loop.length > Sfx.RATE * 5);
        int lmax = 0;
        for (int i = 0; i < loop.length; i++) lmax = Math.max(lmax, Math.abs(loop[i]));
        check("music sits below the effects", lmax < peak);
        check("music is audible", lmax > peak / 4);

        // Effects must fire on the right events.
        Ear ear = new Ear();
        GameCore c = new GameCore(new Mem(), 71L);
        c.sound = ear;
        c.startGame();
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy e = add(c, L, new int[] {3, 1}, new int[] {2, 1}, L.playTop + 80);

        c.tapKey(3, L);
        check("a correct press squishes", ear.squishes == 1);
        check("the squish knows the letter", ear.lastGlyph == 3);
        check("the squish knows the stack depth", ear.lastDepth == 2);
        c.tapKey(0, L);
        check("a wrong press thunks", ear.wrongs == 1);
        check("a wrong press does not squish", ear.squishes == 1);

        c.tapKey(3, L);
        c.tapKey(3, L);
        c.tapKey(1, L);
        check("no clear sound before the shot lands", ear.clears == 0);
        advance(c, L, 0.3f);
        check("clearing a word plays the clear sound", ear.clears == 1);

        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("taking damage plays the drip", ear.damages == 1);

        Ear ear2 = new Ear();
        GameCore g = new GameCore(new Mem(), 72L);
        g.sound = ear2;
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        advancePastBonus(g, L);
        check("a flawless wave plays the achievement", ear2.achievements == 1);

        Ear ear3 = new Ear();
        GameCore m = new GameCore(new Mem(), 73L);
        m.sound = ear3;
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);                      // a miss forfeits the reward
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        check("a flawed wave plays no achievement", ear3.achievements == 0);

        check("a null sound seam is safe", silentRunSurvives(L));
    }

    /**
     * The haul's flight home: one chime per dumpling, as it is taken into the case.
     *
     * The flight is drawn from {@link RoundEnd#arrival}, and the sounds are fired off the same
     * function, so this suite is really asserting that the two halves cannot drift: every landing
     * announced exactly once, in order, and all of them inside the flight.
     */
    static void haulLanding(Layout L) {
        group("shelving the haul");

        // Arrivals have to be spread out, or the chimes stack into one chord. They used to.
        for (int count = 1; count <= 8; count++) {
            boolean rising = true, inside = true;
            for (int n = 0; n < count; n++) {
                float a = RoundEnd.arrival(n, count);
                if (n > 0 && a <= RoundEnd.arrival(n - 1, count)) rising = false;
                if (a <= 0f || a > 1.0001f) inside = false;
                if (RoundEnd.trip(a, n, count) < 0.999f) inside = false;
            }
            check("a haul of " + count + " lands one at a time", rising);
            check("a haul of " + count + " lands inside the flight", inside);
        }
        check("the last one lands as the flight ends",
                Math.abs(RoundEnd.arrival(4, 5) - 1f) < 1e-4f);
        System.out.printf("    a haul of 5 lands at %.0f, %.0f, %.0f, %.0f, %.0fms%n",
                RoundEnd.arrival(0, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(1, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(2, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(3, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(4, 5) * GameCore.HOME_TIME * 1000f);

        // A run that ended with three freed dumplings, sent to the title screen.
        Mem store = new Mem();
        GameCore c = new GameCore(store, 811L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.state = GameCore.OVER;
        c.roundPrizes = Collect.add(Collect.add(Collect.add(0L, 2), 9), 21);
        c.toTitle();
        check("the flight is on", c.homing() && RoundEnd.hauled(c) == 3);
        check("and nothing has been shelved yet", ear.collects == 0 && c.homeLanded == 0);

        // Nothing may land early, and the first one must land before the flight is over.
        advance(c, L, GameCore.HOME_TIME * RoundEnd.lead(0, 3) + DT);
        check("no chime while they are still in the air", ear.collects == 0);
        advance(c, L, GameCore.HOME_TIME);
        check("every one of them is announced", ear.collects == 3);
        check("once each, in the order they fly",
                ear.shelved.equals(java.util.Arrays.asList(0, 1, 2)));
        check("and the flight is over", !c.homing());
        advance(c, L, 2f);
        check("nothing chimes again afterwards", ear.collects == 3);

        // An empty-handed run has nothing to shelve and no flight to do it in.
        Ear quiet = new Ear();
        GameCore d = new GameCore(store, 813L);
        d.sound = quiet;
        d.startGame();
        d.state = GameCore.OVER;
        d.toTitle();
        advance(d, L, GameCore.HOME_TIME + 0.5f);
        check("an empty-handed run shelves nothing", quiet.collects == 0 && !d.homing());

        // Short and decaying, like the chop and the crack: several land a tenth of a second apart
        // and Audio gives every effect one track, so a tail would smear into the next landing.
        short[] chime = Sfx.build(Sfx.COLLECT);
        float len = (float) chime.length / Sfx.RATE;
        int head = 0, tail = 0;
        for (int i = 0; i < chime.length / 4; i++) head = Math.max(head, Math.abs(chime[i]));
        for (int i = chime.length * 3 / 4; i < chime.length; i++) {
            tail = Math.max(tail, Math.abs(chime[i]));
        }
        System.out.printf("    collect is %.0fms, crossings %.0f/s, tail %d%% of head%n",
                len * 1000f, crossRate(chime), tail * 100 / Math.max(1, head));
        check("the chime is short enough to repeat", len < 0.20f);
        check("and shorter than the gap between two landings",
                len < GameCore.HOME_TIME * (RoundEnd.arrival(1, 5) - RoundEnd.arrival(0, 5)) * 2f);
        check("the chime dies away", tail < head / 4);
        // Tone, not noise: it must ring like glass rather than tick like a click.
        check("it has a tone in it", crossRate(chime) > 1200f && crossRate(chime) < 6000f);
    }

    /**
     * The star pickup: the most repeated effect in the game, and one note of a ladder.
     *
     * Held to the same three properties as the chop and the shelving chime — short, decaying, and
     * tonal — plus the one that is specific to it: twenty of these go off inside a five-second
     * course, the last of them a fifth of a second apart, so it has to be shorter than that gap.
     */
    static void starPickup(Layout L) {
        group("star pickup sound");

        short[] ting = Sfx.build(Sfx.STAR);
        float len = (float) ting.length / Sfx.RATE;
        int head = 0, tail = 0;
        for (int i = 0; i < ting.length / 4; i++) head = Math.max(head, Math.abs(ting[i]));
        for (int i = ting.length * 3 / 4; i < ting.length; i++) {
            tail = Math.max(tail, Math.abs(ting[i]));
        }
        System.out.printf("    star is %.0fms, crossings %.0f/s, tail %d%% of head%n",
                len * 1000f, crossRate(ting), tail * 100 / Math.max(1, head));
        check("the ting is short enough to repeat", len < 0.13f);
        check("and shorter than the shelving chime", ting.length < Sfx.build(Sfx.COLLECT).length);
        // The gap between the last two stars of a course, which is the tightest it ever has to fit.
        float gap = StarPath.encounterTime(StarPath.COUNT - 1)
                - StarPath.encounterTime(StarPath.COUNT - 2);
        System.out.printf("    the last two stars are %.0fms apart%n", gap * 1000f);
        check("and shorter than the gap between the last two stars", len < gap);
        check("the ting dies away", tail < head / 4);
        check("it is a note, not a click", crossRate(ting) > 1800f && crossRate(ting) < 7000f);

        // One note per star, climbing, and the twentieth deliberately silent: the tableau's fanfare
        // is what that one sounds like, and a note under it would be lost anyway.
        GameCore c = new GameCore(new Mem(), 331L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.stars.make(new java.util.Random(331L));
        c.stars.begin(-1, L);
        int last = StarPath.COUNT - 1;
        c.stars.collected = (1 << last) - 1;
        c.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.5f;
        check("nineteen already taken makes no sound", ear.stars == 0);
        for (int i = 0; i < 60 * 6 && !c.stars.won; i++) {
            c.stars.x = c.stars.starX(last, L);
            c.update(DT, L);
        }
        check("the course completed", c.stars.won);
        check("the last star rings the fanfare instead of a note",
                ear.stars == 0 && ear.achievements == 1);

        // And a course flown from the start announces every star it takes, with the count.
        GameCore d = new GameCore(new Mem(), 332L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.startGame();
        d.state = GameCore.BONUS;
        d.starBonus = true;
        d.stars.make(new java.util.Random(332L));
        d.stars.begin(-1, L);
        for (int i = 0; i < 60 * 12 && d.state == GameCore.BONUS; i++) d.update(DT, L);
        System.out.printf("    a drifting course took %d and announced %d%n",
                d.stars.count(), ear2.stars);
        check("every star taken is announced once", ear2.stars == d.stars.count());
        check("and the note climbs with the count", ear2.lastStar == d.stars.count());
    }

    static boolean silentRunSurvives(Layout L) {
        GameCore c = new GameCore(new Mem(), 74L);
        c.sound = null;
        c.startGame();
        for (int i = 0; i < 600; i++) {
            c.update(DT, L);
            c.tapKey(i % Glyph.COUNT, L);
        }
        return true;
    }

}
