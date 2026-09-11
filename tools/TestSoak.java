package com.dddumpling.game;

/** Long runs: perfect play, and ten minutes of random input. */
final class TestSoak extends Check {

    static void perfectPlaySurvives(Layout L) {
        group("perfect play");
        GameCore c = new GameCore(new Mem(), 8L);
        c.startGame();
        int frames = 0;
        while (frames < 60 * 120
                && (c.state == GameCore.PLAY || c.state == GameCore.BONUS)) {
            c.update(DT, L);
            frames++;
            if (frames % 2 != 0) continue;
            if (c.state == GameCore.BONUS) {
                c.tapBonus(c.steamer.wanted());
                continue;
            }
            if (bossPlay(c, L)) continue;
            GameCore.Enemy e = c.target != null && c.enemies.contains(c.target)
                    && c.target.typeable() ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }
        System.out.printf("    120s of perfect play: stage=%d score=%d squishes=%d lives=%d%n",
                c.stage, c.score, c.squishes, c.lives);
        check("perfect play keeps all lives", c.lives == GameCore.START_LIVES);
        check("perfect play survives 120s",
                c.state == GameCore.PLAY || c.state == GameCore.BONUS);
        // Was 6, then 5. Every interlude got longer — the spinner, the beat on zero, and the
        // parade after a win — so a fixed two minutes covers fewer stages. What moved is the
        // frame budget, not the difficulty curve: the stage dials are untouched.
        check("perfect play reaches a late stage", c.stage >= 4);
        check("score accumulates", c.score > 1000);
    }


    static void fuzz(Layout L) {
        group("fuzz");
        java.util.Random r = new java.util.Random(1234L);
        GameCore c = new GameCore(new Mem(), 12L);
        c.startGame();
        boolean sane = true;
        int overs = 0;
        for (int i = 0; i < 60 * 600; i++) {
            c.update(DT, L);
            if (r.nextInt(6) == 0) c.tapKey(r.nextInt(Glyph.COUNT), L);
            if (r.nextInt(4000) == 0) c.tapKey(1 + r.nextInt(4), L);
            if (c.state == GameCore.OVER) {
                overs++;
                for (int k = 0; k < 45; k++) c.update(DT, L);
                c.tapKey(1 + r.nextInt(4), L);
            }
            // liveEnemies(), not size(): destroyed words linger while they fly apart, and a
            // frenzy spawns fast enough for several to be in flight at once.
            //
            // The crowd cap is a rule about spawning, so it is only checked during play. Dying
            // inside a frenzy ends the frenzy, which drops the cap fourfold, while the words the
            // frenzy spawned stay standing through the death hold to be swirled away — a held
            // field legitimately holds more than the cap would now allow.
            boolean crowded = c.state == GameCore.PLAY && c.liveEnemies() > c.crowdCap() + 1;
            if (c.lives < 0 || c.score < 0 || crowded || c.combo < 0) {
                sane = false;
                break;
            }
        }
        check("10 minutes of random mashing stays consistent", sane);
        check("random play does reach game over", overs > 0);
        System.out.printf("    fuzz: %d game-overs, %d particles live, %d shots live%n",
                overs, c.particles.size(), c.shots.size());
    }

    /**
     * The difficulty curve, measured against hands rather than against a god.
     *
     * {@link #perfectPlaySurvives} presses thirty times a second and never has to look for anything,
     * so it can only tell us the game is winnable in principle. These runs put {@link Bot}'s stated
     * ceilings against the same curve, which turns "is this fair?" into a number. Four properties
     * are held, and the last is the one that matters most.
     */
    static void boundedPlay(Layout L) {
        group("bounded play");

        // Somewhere around what two thumbs actually manage. Every figure here is a claim about
        // people, not about the game, which is why they are named rather than inlined.
        Result casual = tier("casual", 4f, 0.30f, 0.08f, L);
        Result steady = tier("steady", 6f, 0.20f, 0.04f, L);
        Result quick = tier("quick", 9f, 0.13f, 0.02f, L);

        // 1. The curve is not brutal early. A steady pair of hands has to get a real way in, or the
        //    game is only for the people who already play it.
        check("steady hands reach at least stage 12", steady.stage >= 12f);
        check("even casual hands get well past the opening stages", casual.stage >= 6f);

        // 2. The relaxed late release curve still defeats casual hands. Steady and quick hands
        //    may survive the simulation cap once they reach the deep game; crowd density is no
        //    longer required to manufacture a loss there.
        //
        //    The quickest tier is allowed one run in four that never ends, and that is not slack: it
        //    presses nine times a second and misses one press in fifty, so a run where the dice stay
        //    kind runs to the cap with all three lives. Over twelve seeds it happens once, at about
        //    twice the stage of a typical run, while the average run still ends at little more than
        //    half the cap. Requiring all four to die made this a four-sample coin flip that any
        //    change to interlude timing could turn over — the star course going from four seconds to
        //    3.6 was enough, because it moves every RNG draw after the first interlude.
        check("casual hands lose while skilled hands reach the deep game",
                casual.deaths == casual.runs && steady.stage >= 20f && quick.stage >= 25f);

        // 3. Monotonic in capability. Faster hands must not do *worse* — if they do, something in
        //    here punishes engagement, and that is a bug rather than a difficulty setting.
        check("better hands get at least as far", quick.stage >= steady.stage - 0.5f
                && steady.stage >= casual.stage - 0.5f);

        // 4. A frenzy must be survivable, repeatedly. This is the property the frenzy wall broke and
        //    the reason this bot exists: with flat multipliers a late frenzy ran at six times the
        //    pace of its own stage and asked 23 presses a second, so catching a powerup past about
        //    stage 8 was a way to die — and a reward indistinguishable from a punishment reads as
        //    the game being unfair rather than hard.
        //
        //    Stated as frenzies entered before the run ended, because the run ends when one finally
        //    gets you. Comparing against a bot that "declines" powerups is not available: with
        //    nothing engaged, any press matching the drifting letter catches it, so avoidance is not
        //    a strategy a player can actually have.
        System.out.printf("    steady hands: %.1f frenzies a run, %.0f%% of the time in one, "
                + "%.0f%% of lives lost in one%n", steady.frenzies, steady.frenzyShare * 100f,
                steady.frenzyDeathShare * 100f);
        check("steady hands live through several frenzies before one gets them",
                steady.frenzies >= 5f);
        check("and casual hands through more than one", casual.frenzies >= 2f);
    }

    // ---- helpers ------------------------------------------------------------

    /** Averaged outcome of one capability tier over several seeds. */
    private static final class Result {
        float stage, seconds, frenzies;
        /** Share of the run spent in a frenzy, and share of lives that went during one. */
        float frenzyShare, frenzyDeathShare;
        int deaths, runs;
    }

    /**
     * How long a run is given to finish. Generous: steady hands take seven to ten minutes of game
     * time to reach their wall, and the point of the exercise is where the wall is, not whether one
     * turns up inside an arbitrary window. Cheap even so — the whole tier is a fraction of a second,
     * because nothing is being drawn.
     */
    private static final float RUN_CAP = 900f;

    /**
     * Runs one tier over a spread of seeds and averages it.
     *
     * Several seeds because one run is noisy: which letters a word draws and when a powerup drifts
     * past both move the outcome by a stage or more, and a monotonicity check over single runs would
     * be failing on noise rather than on difficulty.
     */
    private static Result tier(String name, float pps, float reaction, float miss, Layout L) {
        Result r = new Result();
        float lives = 0f, frenzyLives = 0f;
        for (long seed = 1L; seed <= 4L; seed++) {
            GameCore c = new GameCore(new Mem(), 400L + seed);
            c.startGame();
            Bot bot = new Bot(pps, reaction, miss, true, 900L + seed);
            Bot.Result one = bot.play(c, L, RUN_CAP);
            r.stage += one.stage;
            r.seconds += one.seconds;
            r.frenzies += one.frenzies;
            r.frenzyShare += one.frenzySeconds / Math.max(1e-3f, one.seconds);
            lives += one.livesLost;
            frenzyLives += one.frenzyLivesLost;
            if (one.died) r.deaths++;
            r.runs++;
        }
        r.stage /= r.runs;
        r.seconds /= r.runs;
        r.frenzies /= r.runs;
        r.frenzyShare /= r.runs;
        r.frenzyDeathShare = lives > 0f ? frenzyLives / lives : 0f;
        System.out.printf("    %-7s %.0f/s react %.2fs miss %.0f%%  ->  stage %.1f after %.0fs, "
                + "%d of %d died%n", name, pps, reaction, miss * 100f, r.stage, r.seconds,
                r.deaths, r.runs);
        return r;
    }
}
