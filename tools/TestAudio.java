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
