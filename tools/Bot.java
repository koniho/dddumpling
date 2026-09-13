package com.dddumpling.game;

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
 * It does not use the FLING blade and never spends the once-a-stage push-back. Those are real
 * limits, not omissions: a FLING frenzy reaches this bot as a plain typing frenzy at full wave
 * strength, and no panic swipe ever saves it. Both are the pessimistic reading, which is the useful
 * one for a floor.
 *
 * It <em>does</em> tap, drag and shove a boss, and that is not a contradiction of the above — it is
 * the difference between an ability the game offers and one the game requires. A boss has to be
 * beaten for the stage to end, and no boss can be beaten by typing alone, so a bot that could only
 * type would not measure a hard fight: it would measure a stalemate, sitting on stage 5 until the
 * clock ran out. The three touch limits below are stated in the same terms as the typing ones so
 * that what the bot can do with a finger is as bounded as what it can do with a thumb.
 *
 * Its strategy is the obvious human one — clear whatever is closest to the line, otherwise work on
 * the boss, take a powerup when one drifts past and nothing is engaged — not an optimal solver. A
 * bot that plays better than a person can would tell us as little as the perfect one does.
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

    /**
     * How fast a finger drags a boss element, in view widths a second.
     *
     * A real thumb crosses a phone in something like a third of a second, so this is deliberately on
     * the slow side of that: a drag is the slowest thing a player can be asked for, and a glob being
     * carried off the field is time not spent typing. That cost is the whole reason the drag is a
     * mechanic rather than a formality, so the bot must actually pay it.
     */
    private static final float DRAG_SPEED = 2.0f;

    /**
     * How close to the danger line something has to be before the bot drops what it is doing and
     * deals with the field instead of the boss.
     *
     * A boss fight is a divided-attention problem and this is the bot's half of that division. Set
     * low enough that it is not suicidal and high enough that it still loses runs: at 1.0 it would
     * ignore the boss entirely whenever anything was closing, which on a boss stage is most of the
     * time.
     */
    private static final float PANIC_WARN = 0.55f;

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

    /**
     * One frame of this player's attention. Called by {@link #play} after the world has stepped.
     *
     * Visible so a suite can drive a bounded hand over a stretch it owns the loop for — timing a boss
     * fight, for one, which {@code play} cannot answer because it measures a whole run. Callers step
     * the world themselves and must not also let {@code play} do it: this does not update the core.
     */
    void step(GameCore c, Layout L, float dt) {
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
        if (c.bossFighting() && c.boss.kind == Boss.SPLITTER
                && c.boss.vulnerablePiece() >= 0 && budget >= 1f) {
            budget -= 1f;
            presses++;
            think = reaction;
            c.boss.beginPinch(100f);
            c.boss.pinch(100f * (Boss.DIVIDE_SCALE + 0.01f));
            return;
        }

        // A drag already under way is a finger that is busy, so it runs before anything else and
        // costs the frame. Nothing else this player can do happens while carrying something.
        if (c.boss.held >= 0 && dragOn(c, L, dt)) return;

        // Otherwise the boss gets whatever attention the field is not demanding.
        if (c.bossFighting() && c.warnLevel < PANIC_WARN && bossTouch(c, L)) return;

        if (takesPowerups && c.power != null && c.power.catchable() && budget >= 1f) {
            budget -= 1f;
            presses++;
            think = reaction;
            c.tapPower(c.power.x, c.power.y, L);
            return;
        }

        // Mid-word, the next key is already known and under a thumb. Otherwise there is a word to
        // find first — unless FLURRY has made every key the right one, which is the whole of what
        // that mode does for you.
        boolean engaged = c.target != null && c.enemies.contains(c.target) && c.target.typeable();
        if (!engaged && pendingPartner(c) == null && !c.flurry()) {
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

    /**
     * Carries whatever is held toward where it has to go, at {@link #DRAG_SPEED}.
     *
     * @return true while the finger is still busy with it
     */
    private boolean dragOn(GameCore c, Layout L, float dt) {
        int i = c.boss.held;
        if (i < 0) return false;
        float gx = goalX(c, L, i), gy = goalY(c, L, i);
        float x = c.boss.ex[i], y = c.boss.ey[i];
        float dx = gx - x, dy = gy - y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float step = L.w * DRAG_SPEED * dt;
        if (d <= step) {
            // Arrived. Whether that finished the job is the core's call, not the bot's.
            c.dragBoss(gx, gy, L);
            if (c.boss.held >= 0) c.releaseBoss();
            return true;
        }
        c.dragBoss(x + dx / d * step, y + dy / d * step, L);
        return true;
    }

    /** Where element {@code i} has to be taken. Mirrors what {@code Boss.dragTo} accepts. */
    private float goalX(GameCore c, Layout L, int i) {

        // Whichever edge is nearer, so the drag is the short one.
        return c.boss.ex[i] < L.w / 2f ? L.playLeft - 1f : L.playRight + 1f;
    }

    private float goalY(GameCore c, Layout L, int i) {
        return c.boss.ey[i];
    }

    /**
     * One touch at the boss: a shove, a grab, or a tap. Costs a press from the budget and a
     * reaction, exactly as a keystroke does — a finger is not free.
     *
     * @return true when it used the frame
     */
    private boolean bossTouch(GameCore c, Layout L) {
        if (think > 0f || budget < 1f) return false;
        Boss b = c.boss;
        if (b.kind == Boss.OCTOPUS && b.octoVulnerableArm >= 0) {
            if (b.octoCoil < 0.72f) return true;
            budget -= 1f;
            presses++;
            think = reaction;
            if (b.held == -3) {
                if (!b.octoDragStarted || !b.octoDragCanDamage)
                    c.dragBoss(L.w * 0.5f, Math.min(b.octoDragY, L.dangerY - L.keyR), L);
                else c.dragBoss(L.playLeft, b.octoDragY, L);
            } else {
                int tip = Boss.OCTO_NODES - 1;
                c.grabBoss(b.octoX[b.octoVulnerableArm][tip],
                        b.octoY[b.octoVulnerableArm][tip]);
            }
            return true;
        }
        if (b.kind == Boss.MUSHROOM) {
            budget -= 1f;
            presses++;
            think = reaction;
            if (b.held != -2) b.grabBody(b.body.centreX(), b.body.centreY());
            if (b.mushroomMeterAlpha == 0f) {
                c.dragBoss(b.mushroomLastX + L.w * 0.02f, b.body.centreY(), L);
                return true;
            }
            if (Math.abs(b.mushroomGuideX - b.mushroomGuideTarget) <= Boss.MUSHROOM_GUIDE_WINDOW)
                c.dragBoss(b.mushroomLastX + b.mushroomGuideTarget * L.w * 0.36f,
                        b.body.centreY(), L);
            return true;
        }
        // Then anything worth carrying off, oldest first — a glob about to crawl back is the one
        // that matters, and picking the one with least life left is what a player watching them
        // would do.
        int drag = -1;
        for (int i = 0; i < Boss.ELEMS; i++) {
            if (!b.draggable(i)) continue;
            if (drag < 0 || b.elife[i] < b.elife[drag]) drag = i;
        }
        if (drag >= 0) {
            budget -= 1f;
            presses++;
            think = reaction;
            c.grabBoss(b.ex[drag], b.ey[drag]);
            return true;
        }
        return false;
    }

    /** The connected hand already identifies this key; no second search reaction is owed.
     * It still costs a normal press from the budget and can still miss. */
    private static GameCore.Enemy pendingPartner(GameCore c) {
        for (GameCore.Enemy e : c.enemies) {
            if (e.typeable() && e.link != null && e.link.linkWaiting) return e;
        }
        return null;
    }

    /** The key this player would press next, or -1 with nothing worth pressing. */
    private int pick(GameCore c) {
        boolean engaged = c.target != null && c.enemies.contains(c.target) && c.target.typeable();
        // A bolt in the air, ahead of everything including the field: it is aimed at the deck and
        // already committed, and no word is closer to costing a life than that.
        if (c.bossFighting()) {
            for (int g = 0; g < Glyph.COUNT; g++) {
                if (c.boss.boltWants(g)) return g;
            }
        }
        // The boss, when it is asking for something and no word is part-way through. Ahead of the
        // words on purpose: its window is a few seconds long and a word is not going anywhere.
        if (!engaged && c.bossFighting() && c.warnLevel < PANIC_WARN) {
            for (int g = 0; g < Glyph.COUNT; g++) {
                if (c.boss.wants(g)) return g;
            }
        }
        // FLURRY: any key hits, so there is nothing to work out.
        if (c.flurry()) return rnd.nextInt(Glyph.COUNT);
        if (engaged) return c.target.word[c.target.pos];
        GameCore.Enemy partner = pendingPartner(c);
        if (partner != null) return partner.word[partner.pos];
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
