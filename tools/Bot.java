package com.sram.hexatype;

/**
 * A player with stated limits, so the difficulty curve can be asserted instead of argued about.
 *
 * The harness already had a bot before this one: {@link TestSoak#perfectPlaySurvives} presses every
 * second frame, which is thirty presses a second, never misses and never has to look for the next
 * word. It proves the game is winnable by a god. What it cannot tell you is whether the game is
 * winnable by *hands*, and that is the question every tuning change actually turns on — the frenzy
 * wall sat in this game through several rounds of tuning precisely because nothing here had a
 * ceiling. This bot has three:
 *
 * <ul>
 *   <li>{@link #pps} — presses a second, sustained. Two thumbs manage somewhere around 5–8.
 *   <li>{@link #reaction} — the beat spent finding the next word and getting a thumb to its key,
 *       paid every time it is not already part-way through a word. FLURRY does not charge it, which
 *       is exactly what that mode buys you and the reason it should show up here as an advantage.
 *   <li>{@link #missRate} — how often a press lands on the wrong key, which in ordinary play costs
 *       the whole word.
 * </ul>
 *
 * It cannot swipe. That is a real limit, not an omission: it means a FLING frenzy reaches this bot
 * as a plain typing frenzy at full wave strength, and the once-a-stage push-back never gets used.
 * Both are the pessimistic reading, which is the useful one for a floor.
 *
 * Its strategy is the obvious human one — take the lowest word on the field and type it out, grab a
 * powerup when one drifts past and nothing is engaged — not an optimal solver. A bot that plays
 * better than a person can would tell us as little as the perfect one does.
 *
 * Deterministic on its seed, like everything else here.
 */
final class Bot {

    /** Presses a second, sustained. */
    final float pps;
    /** Seconds to find the next word once nothing is engaged. */
    final float reaction;
    /** Fraction of presses that go to the wrong key. */
    final float missRate;
    /** Whether it takes powerups, or leaves them to drift past. */
    final boolean takesPowerups;

    private final java.util.Random rnd;
    /** Presses banked but not yet spent. */
    private float budget;
    /** Reaction still owed before the next press. */
    private float think;

    /**
     * At most this many presses may be banked. Without a cap the bot saves up through every quiet
     * moment and spends the lot on the frame a wave arrives, which is a burst no thumb can do and
     * which flattered exactly the crowded moments this exists to measure.
     */
    private static final float BURST = 2f;

    Bot(float pps, float reaction, float missRate, boolean takesPowerups, long seed) {
        this.pps = pps;
        this.reaction = reaction;
        this.missRate = missRate;
        this.takesPowerups = takesPowerups;
        this.rnd = new java.util.Random(seed);
    }

    /** How a run went. */
    static final class Result {
        int stage;
        int score;
        int livesLost;
        int presses;
        int frenzies;
        float seconds;
        boolean died;
        /** Time spent inside a frenzy, and the lives that went while inside one. */
        float frenzySeconds;
        int frenzyLivesLost;

        public String toString() {
            return String.format("stage %-2d  %5ds  score %-6d  lost %d  %d frenzies  %d presses%s",
                    stage, (int) seconds, score, livesLost, frenzies, presses, died ? "  DIED" : "");
        }
    }

    /**
     * Plays one run to its end, or until {@code maxSeconds} of game time has passed.
     *
     * Bounded by the clock rather than by a while-true, so a core that stops progressing fails an
     * assertion instead of hanging the harness.
     */
    Result play(GameCore c, Layout L, float maxSeconds) {
        Result r = new Result();
        boolean wasFrenzied = false;
        while (r.seconds < maxSeconds && c.state != GameCore.OVER) {
            // Both read before the step, because a fatal breach ends the frenzy on its way out —
            // so asking afterwards would book every death that a frenzy caused against calm play,
            // which is precisely backwards for the thing this measures.
            boolean frenzied = c.powerActive();
            int had = c.lives;

            c.update(Check.DT, L);
            step(c, L, Check.DT);

            r.seconds += Check.DT;
            if (frenzied) r.frenzySeconds += Check.DT;
            // Summed from the decrements rather than read off the counter at the end: two words can
            // breach on the same frame, and the second one takes lives past zero, so the counter
            // undercounts exactly the frames this cares about.
            if (c.lives < had) {
                r.livesLost += had - c.lives;
                if (frenzied) r.frenzyLivesLost += had - c.lives;
            }
            if (c.powerActive() && !wasFrenzied) r.frenzies++;
            wasFrenzied = c.powerActive();
            if (c.stage > r.stage) r.stage = c.stage;
            r.score = c.score;
        }
        r.presses = presses;
        r.died = c.state == GameCore.OVER;
        return r;
    }

    private int presses;

    private void step(GameCore c, Layout L, float dt) {
        // The interlude is a mash, and a bounded player mashes no faster than they type. Failing it
        // for want of hands is a legitimate outcome — it costs the prize, not the run.
        if (c.state == GameCore.BONUS) {
            budget = Math.min(BURST, budget + dt * pps);
            if (budget >= 1f) {
                budget -= 1f;
                presses++;
                c.tapBonus(c.steamer.wanted());
            }
            return;
        }
        if (c.state != GameCore.PLAY) return;

        budget = Math.min(BURST, budget + dt * pps);

        // Mid-word, the next key is already known and under a thumb. Otherwise there is a word to
        // find first — unless FLURRY has made every key the right one, which is the whole of what
        // that mode does for you.
        boolean engaged = c.target != null && c.enemies.contains(c.target) && c.target.typeable();
        if (!engaged && !c.flurry()) {
            think -= dt;
            if (think > 0f) return;
        }
        if (budget < 1f) return;

        int want = pick(c);
        if (want < 0) return;
        budget -= 1f;
        presses++;
        // A miss goes to a neighbouring key rather than a uniformly random one: a thumb that slips
        // lands next door, and in this deck next door is a different letter.
        int g = rnd.nextFloat() < missRate ? (want + 1 + rnd.nextInt(Glyph.COUNT - 1))
                % Glyph.COUNT : want;
        c.tapKey(g, L);

        // Whatever that press did, if nothing is engaged now the next one has to be found.
        if (!(c.target != null && c.enemies.contains(c.target) && c.target.typeable())) {
            think = reaction;
        }
    }

    /** The key this player would press next, or -1 with nothing worth pressing. */
    private int pick(GameCore c) {
        boolean engaged = c.target != null && c.enemies.contains(c.target) && c.target.typeable();
        // A drifting powerup is only worth a press when no word is part-way through — engaging one
        // mid-word would throw the word away, and the core would refuse the catch anyway.
        if (!engaged && takesPowerups && c.power != null && c.power.catchable()) {
            return c.power.glyph;
        }
        // FLURRY: any key hits, so there is nothing to work out.
        if (c.flurry()) return rnd.nextInt(Glyph.COUNT);
        if (engaged) return c.target.word[c.target.pos];
        // The lowest typeable word, being the one closest to costing a life.
        GameCore.Enemy best = null;
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.typeable()) continue;
            if (best == null || e.y > best.y) best = e;
        }
        return best == null ? -1 : best.word[best.pos];
    }
}
