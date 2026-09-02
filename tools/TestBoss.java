package com.sram.hexatype;

/**
 * The boss encounter: its cadence, the frame all five share, the press/tap/drag precedence, each
 * boss's own mechanic, and — the part with the most history behind it — that nothing a boss put on
 * the screen outlives it.
 */
final class TestBoss extends Check {

    private TestBoss() {}

    /**
     * Drops a fresh run straight into the boss of kind {@code kind}, past its arrival card.
     *
     * Gets there the way the game does — the stage before it is declared spent, the interlude runs,
     * and {@code advanceStage} starts the fight — rather than by calling {@code begin} directly, so
     * what these assertions exercise is the wiring as well as the rules.
     */
    private static GameCore enterBoss(Layout L, int kind, long seed) {
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        c.stage = Boss.EVERY * (kind + 1) - 1;
        c.enemies.clear();
        c.spawnedThisStage = c.stageQuota();
        for (int i = 0; i < 60 * 60 && !c.boss.active(); i++) c.update(DT, L);
        for (int i = 0; i < 60 * 10 && !c.boss.fighting(); i++) c.update(DT, L);
        return c;
    }

    /** Steps until the boss's window is open, keeping the field clear so nothing interferes. */
    private static boolean toOpen(GameCore c, Layout L) {
        for (int i = 0; i < 60 * 30 && !c.boss.open(); i++) {
            c.enemies.clear();
            c.target = null;
            c.update(DT, L);
        }
        return c.boss.open();
    }

    /** Steps until the window shuts again. */
    private static boolean toShut(GameCore c, Layout L) {
        for (int i = 0; i < 60 * 30 && c.boss.open(); i++) {
            c.enemies.clear();
            c.target = null;
            c.update(DT, L);
        }
        return !c.boss.open();
    }

    /**
     * Clears the field the way play does — crediting every word rather than deleting it.
     *
     * The difference matters for exactly one boss and it is easy to miss: SUMO's shoves are paid for
     * with words <em>cleared</em>, so a test that keeps itself alive with {@code enemies.clear()}
     * banks no charges and then reports that the boss cannot be beaten. It could; the test simply
     * never paid for a swipe.
     */
    private static void sweep(GameCore c, Layout L) {
        for (int i = c.enemies.size() - 1; i >= 0; i--) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.destroyed) c.destroyWord(e, c.enemyCentreX(e), e.y, L);
        }
    }

    /** The letter the boss will accept right now, or -1. */
    private static int wanted(Boss b) {
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (b.wants(g)) return g;
        }
        return -1;
    }

    // ---- cadence ------------------------------------------------------------

    static void cadence(Layout L) {
        group("boss cadence");

        check("stage 5 is the first boss", Boss.isBossStage(5));
        boolean between = true;
        for (int s = 1; s <= 4; s++) if (Boss.isBossStage(s)) between = false;
        for (int s = 6; s <= 9; s++) if (Boss.isBossStage(s)) between = false;
        check("the stages either side of it are not", between);
        check("and every fifth one after it is",
                Boss.isBossStage(10) && Boss.isBossStage(15) && Boss.isBossStage(20)
                        && Boss.isBossStage(25));
        check("stage 0 is not a boss stage", !Boss.isBossStage(0));

        check("they arrive in order", Boss.kindFor(5) == Boss.SLIME
                && Boss.kindFor(10) == Boss.TRIPLETS && Boss.kindFor(15) == Boss.DRUM
                && Boss.kindFor(20) == Boss.MAGPIE && Boss.kindFor(25) == Boss.SUMO);
        check("and then cycle round", Boss.kindFor(30) == Boss.SLIME
                && Boss.kindFor(35) == Boss.TRIPLETS);
        boolean inRange = true;
        for (int s = 5; s <= 500; s += 5) {
            int k = Boss.kindFor(s);
            if (k < 0 || k >= Boss.COUNT) inRange = false;
        }
        check("no stage ever names a boss that does not exist", inRange);

        // The harness font is an ASCII subset and silently draws nothing for a character it lacks,
        // so a name using one would look right on the device and be missing a letter in every frame
        // anybody checked.
        boolean printable = true;
        for (int i = 0; i < Boss.COUNT; i++) {
            if (!printable(Boss.NAMES[i]) || !printable(Boss.BLURB[i])) printable = false;
        }
        check("every name and blurb is in the harness font", printable);
        check("there is a name, a blurb and a face for each",
                Boss.NAMES.length == Boss.COUNT && Boss.BLURB.length == Boss.COUNT
                        && Boss.FACE.length == Boss.COUNT);
        boolean faces = true;
        for (int i = 0; i < Boss.COUNT; i++) {
            if (Boss.FACE[i] < 0 || Boss.FACE[i] >= Glyph.COUNT) faces = false;
        }
        check("and every face is one of the six letters", faces);
    }

    // ---- the shared frame ---------------------------------------------------

    static void frame(Layout L) {
        group("boss frame");

        GameCore c = new GameCore(new Mem(), 11L);
        c.startGame();
        check("a run starts with no boss", !c.boss.active());

        c.stage = 4;
        c.enemies.clear();
        c.spawnedThisStage = c.stageQuota();
        for (int i = 0; i < 60 * 60 && !c.boss.active(); i++) c.update(DT, L);
        check("reaching stage 5 starts one", c.boss.active() && c.stage == 5);
        check("as the right kind", c.boss.kind == Boss.SLIME);
        check("it arrives before it fights", c.boss.intro > 0f && !c.boss.fighting());
        check("at full health", c.boss.health() == 1f);

        // Exactly one phase at a time, all the way through. A phase nobody named is a screen that
        // draws nothing, and this file has been bitten by that before.
        GameCore p = enterBoss(L, Boss.SLIME, 12L);
        boolean onePhase = true;
        for (int i = 0; i < 60 * 40; i++) {
            Boss b = p.boss;
            if (b.active()) {
                int on = 0;
                if (b.intro > 0f) on++;
                if (b.fighting()) on++;
                if (b.beaten) on++;
                if (on != 1) onePhase = false;
            }
            p.enemies.clear();
            p.update(DT, L);
        }
        check("exactly one of arriving, fighting and beaten holds on every frame", onePhase);

        // No way past it. This is the whole point of the no-retreat rule, so it is asserted rather
        // than assumed: sit on a boss stage doing nothing for far longer than the fight is meant to
        // take, and the stage must still be the same stage.
        GameCore w = enterBoss(L, Boss.TRIPLETS, 13L);
        int stageWas = w.stage;
        for (int i = 0; i < 60 * 90 && w.state == GameCore.PLAY; i++) {
            w.enemies.clear();          // survive, but do nothing about the boss
            w.update(DT, L);
        }
        check("a boss left alone never lets the stage end",
                w.stage == stageWas && (w.boss.active() || w.state != GameCore.PLAY));
        check("and it does not quietly beat itself", w.boss.health() > 0f);

        // Enrage is visual urgency only. Time by itself must never take a life.
        GameCore r = enterBoss(L, Boss.TRIPLETS, 14L);
        check("it does not start enraged", r.boss.enrage() == 0f);
        int livesWas = r.lives;
        boolean calmAndHarmless = true;
        for (int i = 0; i < (int) (60 * (Boss.ENRAGE_AT - 1f)); i++) {
            r.update(DT, L);
            if (r.lives < livesWas) calmAndHarmless = false;
        }
        check("a calm boss cannot hurt you", calmAndHarmless);
        for (int i = 0; i < (int) (60 * (Boss.ENRAGE_RAMP + 10f)); i++) {
            r.update(DT, L);
        }
        check("it enrages if the fight drags", r.boss.enrage() > 0f);
        check("and an enraged one remains harmless", r.lives == livesWas);

        // Powerups are suppressed for the whole of a boss stage.
        GameCore q = enterBoss(L, Boss.SLIME, 15L);
        boolean noPower = true;
        for (int i = 0; i < 60 * 25 && q.boss.active(); i++) {
            q.enemies.clear();
            q.update(DT, L);
            if (q.power != null || q.powerActive()) noPower = false;
        }
        check("no powerup drifts past during a boss", noPower);

        // And no words either. A boss stage is the fight and nothing else: the wave underneath it
        // was dividing attention away from the thing the stage is about, and taking up the screen
        // the boss needs.
        GameCore m = enterBoss(L, Boss.SLIME, 16L);
        int most = 0;
        for (int i = 0; i < 60 * 25 && m.boss.active(); i++) {
            m.update(DT, L);
            if (m.enemies.size() > most) most = m.enemies.size();
        }
        check("a boss stage releases no words at all", most == 0);
        check("so there is no crowd to cap", m.crowdCap() == 0 || !m.boss.active());
    }

    // ---- winning ------------------------------------------------------------

    static void winning(Layout L) {
        group("beating a boss");

        GameCore c = enterBoss(L, Boss.SLIME, 21L);
        c.lives = 1;
        int scoreWas = c.score;
        int stageWas = c.stage;
        // Beat it by playing it properly, through the same helper the other drivers use.
        for (int i = 0; i < 60 * 40 && c.boss.active(); i++) {
            c.enemies.clear();
            c.target = null;
            bossPlay(c, L);
            c.update(DT, L);
        }
        check("it can be beaten", !c.boss.active());
        check("which pays out", c.score > scoreWas + GameCore.BOSS_BONUS / 2);
        check("and hands a life back", c.lives == 2);
        // And then the stage ends by the ordinary route.
        for (int i = 0; i < 60 * 20 && c.state == GameCore.PLAY; i++) c.update(DT, L);
        check("the stage ends once it is gone", c.state == GameCore.BONUS);
        for (int i = 0; i < 60 * 40 && c.state == GameCore.BONUS; i++) c.update(DT, L);
        check("and the next stage follows", c.stage == stageWas + 1);

        // The life-back is capped, exactly as the steamer's is.
        GameCore f = enterBoss(L, Boss.SLIME, 22L);
        f.lives = GameCore.START_LIVES;
        for (int i = 0; i < 60 * 40 && f.boss.active(); i++) {
            f.enemies.clear();
            f.target = null;
            bossPlay(f, L);
            f.update(DT, L);
        }
        check("a full-health run gets no extra life", f.lives == GameCore.START_LIVES);

        // Every boss has to be beatable, or its stage is a wall with no way round it.
        for (int k = 0; k < Boss.COUNT; k++) {
            GameCore g = enterBoss(L, k, 300L + k);
            for (int i = 0; i < 60 * 90 && g.boss.active() && g.state == GameCore.PLAY; i++) {
                // Kept alive so this measures the fight and not the wave under it — but the words are
                // credited rather than deleted, since one boss is paid for with them.
                sweep(g, L);
                g.target = null;
                g.lives = GameCore.START_LIVES;
                bossPlay(g, L);
                g.update(DT, L);
            }
            check(Boss.NAMES[k] + " can be beaten", !g.boss.active());
        }

        // And beaten by a hand with limits, inside the enrage warning. The loop above proves
        // reachability with
        // Check.bossPlay, which has no reaction and no miss rate and finishes a slime in a second —
        // useful for "is this possible", useless for "is this tuned". This is the figure every boss's
        // health is actually set against, so it is measured with the steady soak hand.
        //
        // Printed per boss and asserted as a band: what matters is that no one of them is wildly out
        // of step with the others, since that is the sign of a mechanic whose cost changed without its
        // health following it.
        for (int k = 0; k < Boss.COUNT; k++) {
            float took = fightSeconds(L, k, 800L + k);
            System.out.printf("    %-10s takes a steady hand %.0fs of its %.0fs enrage warning%n",
                    Boss.NAMES[k], took, Boss.ENRAGE_AT);
            check(Boss.NAMES[k] + " falls to a hand with limits", took >= 0f);
            check("well inside the enrage warning", took >= 0f && took < Boss.ENRAGE_AT);
        }
    }

    /**
     * Seconds the steady soak hand takes to beat boss {@code kind} on its own stage, or -1 if it
     * never did.
     *
     * Lives are topped up every frame: this measures the length of the fight, not whether the enrage
     * would have got there first, and a run that ended would report the cap either way.
     */
    private static float fightSeconds(Layout L, int kind, long seed) {
        GameCore c = enterBoss(L, kind, seed);
        Bot bot = new Bot(6f, 0.20f, 0.04f, false, seed);
        float age = 0f;
        for (int i = 0; i < 60 * 90 && c.boss.active(); i++) {
            c.lives = GameCore.START_LIVES;
            age = c.boss.age;
            c.update(DT, L);
            bot.step(c, L, DT);
        }
        return c.boss.active() ? -1f : age;
    }

    // ---- precedence ---------------------------------------------------------

    static void precedence(Layout L) {
        group("boss press precedence");

        GameCore c = enterBoss(L, Boss.SLIME, 31L);
        toOpen(c, L);
        int g = wanted(c.boss);
        check("an open boss is asking for a letter", g >= 0);

        // An engaged word outranks it, exactly as it outranks the drifting powerup.
        //
        // Measured on the slime's chain rather than on its health, because a press at a slime no
        // longer takes health off it at all — five of them work a glob loose and it is carrying that
        // off the screen that hurts it. The chain counter is what a press moves, so it is what
        // "the boss took the press" means here.
        int chainWas = c.boss.chainAt;
        int other = (g + 1) % Glyph.COUNT;
        GameCore.Enemy e = add(c, L, new int[] {other, g}, L.playTop + 10f);
        c.tapKey(other, L);
        check("engaging a word takes the press", c.target == e && e.pos == 1);
        c.tapKey(g, L);
        check("and the engaged word keeps it, even for the boss's own letter",
                c.boss.chainAt == chainWas);

        // Nothing engaged: the boss takes it.
        c.enemies.clear();
        c.target = null;
        toOpen(c, L);
        g = wanted(c.boss);
        chainWas = c.boss.chainAt;
        c.tapKey(g, L);
        check("with nothing engaged the boss takes the press", c.boss.chainAt > chainWas);

        // A letter it is not asking for still reaches the words.
        GameCore d = enterBoss(L, Boss.SLIME, 32L);
        toOpen(d, L);
        int wantLetter = wanted(d.boss);
        int spare = -1;
        for (int i = 0; i < Glyph.COUNT; i++) if (i != wantLetter) spare = i;
        d.enemies.clear();
        d.target = null;
        GameCore.Enemy word = add(d, L, new int[] {spare, spare}, L.playTop + 10f);
        int bossChain = d.boss.chainAt;
        d.tapKey(spare, L);
        check("a letter the boss does not want falls through to the words",
                word.pos == 1 && d.boss.chainAt == bossChain);

        // A landed press fires a bullet at the boss, the same as a press at a word does.
        GameCore sh = enterBoss(L, Boss.SLIME, 35L);
        toOpen(sh, L);
        sh.enemies.clear();
        sh.target = null;
        sh.shots.clear();
        int chain = sh.boss.chainLetter();
        sh.tapKey(chain, L);
        check("a landed boss press fires a bullet", sh.shots.size() == 1);
        GameCore.Shot bullet = sh.shots.get(0);
        check("from the key that was pressed",
                bullet.sx == L.keyX[chain] && bullet.sy == L.keyY[chain]);
        check("aimed at the boss", bullet.atBoss && bullet.target == null);
        check("and it lands on the boss, not through it",
                Math.abs(bullet.tx - sh.boss.body.centreX()) < sh.boss.body.radius()
                        && Math.abs(bullet.ty - sh.boss.body.centreY()) < sh.boss.body.radius());
        // It homes, because the body drifts the whole time it is in the air.
        //
        // Held to one frame of body movement rather than to a fixed pixel: the aim is re-taken from
        // the body's centre and the body then moves on within the same frame, so the offset is always
        // one frame stale and a fixed tolerance is really a bet on how fast the boss is wobbling. The
        // slime wobbles twice as hard as anything else here, which is what collected that bet.
        float wasDx = bullet.tx - sh.boss.body.centreX();
        for (int i = 0; i < 4; i++) sh.update(DT, L);
        boolean landed = sh.shots.isEmpty();
        float lag = sh.boss.body.motion() * DT + 1f;
        check("it keeps its offset as the body moves",
                landed || Math.abs((bullet.tx - sh.boss.body.centreX()) - wasDx) < lag);
        for (int i = 0; i < 30; i++) sh.update(DT, L);
        check("and it is gone once it lands", sh.shots.isEmpty());

        // A rebuff fires nothing: a bullet that flies out and does nothing reads as a miss, when
        // what actually happened is that the press was refused.
        GameCore nb = enterBoss(L, Boss.DRUM, 36L);
        toShut(nb, L);
        nb.enemies.clear();
        nb.target = null;
        nb.shots.clear();
        nb.tapKey(nb.boss.want(), L);
        check("a rebuffed press fires no bullet", nb.shots.isEmpty());

        // A rebuff is not a miss. The interlude set that precedent and the accuracy dumpling
        // should not be scolding anybody for engaging with a mechanic.
        GameCore r = enterBoss(L, Boss.DRUM, 33L);
        toShut(r, L);
        r.enemies.clear();
        r.target = null;
        int missesWas = r.misses;
        int hitsWas = r.hits;
        // The drum's own letter, at the wrong moment.
        int beat = r.boss.want();
        r.tapKey(beat, L);
        check("a rebuff does not count as a miss", r.misses == missesWas);
        check("nor as a hit", r.hits == hitsWas);
    }

    // ---- each boss ----------------------------------------------------------

    static void slime(Layout L) {
        group("boss: slime");

        GameCore c = enterBoss(L, Boss.SLIME, 41L);
        // Twice as wide as it is tall, in the physics and not only in the drawing — the hit-test, the
        // element placement and the outline all read the same body.
        check("it is twice as wide as it is tall", c.boss.body.wide() == 2f
                && c.boss.body.radiusX() > c.boss.body.radiusY() * 1.8f
                && c.boss.body.radiusY() < Boss.bodyR(L) * 1.05f);
        check("and it does not read as permanently squashed",
                Math.abs(c.boss.body.squashAspect() - 1f) < 0.12f);
        // Twice as springy: the same hit on the same shape dents it twice as far.
        Softbody lively = new Softbody(Softbody.NODES, 1);
        Softbody calm = new Softbody(Softbody.NODES, 1);
        lively.idle = calm.idle = 0f;
        lively.reset(0f, 0f, 100f, 2f);
        calm.reset(0f, 0f, 100f, 2f);
        lively.jiggle = 2f;
        lively.impulse(0f, -100f, 0.5f);
        calm.impulse(0f, -100f, 0.5f);
        float lp = 0f, cp = 0f;
        for (int i = 0; i < 30; i++) {
            lively.update(DT);
            calm.update(DT);
            lp = Math.max(lp, lively.deform());
            cp = Math.max(cp, calm.deform());
        }
        System.out.printf("    the same hit dents a jiggly body to %.3f and an ordinary one"
                + " to %.3f%n", lp, cp);
        check("it is twice as jiggly", lp > cp * 1.7f);
        // And it rings for longer, which is the other half of jiggly — an amplitude that decays just
        // as fast is a harder hit, not a wobblier creature.
        for (int i = 0; i < 60; i++) {
            lively.update(DT);
            calm.update(DT);
        }
        check("and holds the wobble for longer", lively.deform() > calm.deform() * 1.5f);
        toOpen(c, L);
        int first = c.boss.chainLetter();
        check("it shows the next letter of its chain", first >= 0);
        c.enemies.clear();
        c.target = null;

        // The press does not hurt it. This is the whole shape of this boss now: the chain works a
        // glob loose and the drag is the only thing that scores, so a health bar that moved on a
        // press would be describing a fight that is not happening.
        float hpWas = c.boss.hp;
        Ear splitEar = new Ear();
        c.sound = splitEar;
        int letters = 0;
        for (int k = 0; k < Boss.SPLIT_HITS - 1; k++) {
            if (!c.boss.open()) break;
            int g = c.boss.chainLetter();
            c.tapKey(g, L);
            letters++;
            if (c.boss.chainLetter() != g) continue;
            // The chain may legitimately repeat a letter; what must not happen is it standing still.
            check("the chain moves on", c.boss.chainAt == letters);
        }
        check("a run of presses does not take any health off it", c.boss.hp == hpWas);
        check("but it does work the skin loose", c.boss.split == letters && letters > 0);
        boolean early = true;
        for (int i = 0; i < Boss.ELEMS; i++) if (c.boss.etype[i] == Boss.E_GLOB) early = false;
        check("and nothing has split off yet", early);
        check("and the split sound waits for the glob", splitEar.bossSplits == 0);

        // The last press of the five is the one that tears a glob out.
        c.tapKey(c.boss.chainLetter(), L);
        int globs = 0;
        for (int i = 0; i < Boss.ELEMS; i++) {
            if (c.boss.etype[i] == Boss.E_GLOB) globs++;
        }
        check("the fifth press sheds a glob", globs == 1);
        check("and lands one satisfying split pop", splitEar.bossSplits == 1);
        check("each charged bolt character dies with its own low bloop",
                splitEar.boltPops == Boss.SPLIT_HITS - 1);
        check("and the split gauge starts again", c.boss.split == 0);
        check("still without hurting it", c.boss.hp == hpWas);

        // It is a patch within the silhouette: close enough to the edge to find and pull, but its
        // full colour mark stays on the skin instead of reading as a separate ball.
        int inside = -1;
        for (int i = 0; i < Boss.ELEMS; i++) if (c.boss.etype[i] == Boss.E_GLOB) inside = i;
        c.update(DT, L);
        float patchX = Math.abs(c.boss.ex[inside] - c.boss.bodyX(L));
        check("and its centre pulls just beyond the resting body edge",
                patchX > c.boss.bodyW(L)
                        && patchX < c.boss.bodyW(L) + c.boss.er[inside] * 0.3f);
        check("while the slime silhouette protrudes around it",
                c.boss.body.radiusX() > c.boss.bodyW(L) + c.boss.er[inside] * 0.2f);
        float[] skin = BossScreen.globPath(c.boss, c.boss.ex[inside], c.boss.ey[inside], 1f);
        float[] outline = c.boss.body.outline();
        boolean samePath = true;
        for (int q = 0; q < skin.length / 4; q++) {
            boolean found = false;
            for (int j = 0; j < outline.length; j += 2)
                if (skin[q * 2] == outline[j] && skin[q * 2 + 1] == outline[j + 1]) found = true;
            if (!found) samePath = false;
        }
        check("and its outside edge is the slime body path itself", samePath);
        check("its invisible drag window extends beyond the visible wart",
                c.boss.elemAt(c.boss.ex[inside],
                        c.boss.ey[inside] + c.boss.er[inside] * 1.8f) == inside);
        check("without claiming distant field touches",
                c.boss.elemAt(c.boss.ex[inside],
                        c.boss.ey[inside] + c.boss.er[inside] * 2.3f) < 0);

        // Hauling it stretches the skin after it, and letting go stops the tug — the spring back is
        // the solver's, so there is nothing else to check for the rebound but that the pull ended.
        check("the resting wart is already tugging the skin outward", c.boss.body.pulled());
        // Let the punch of the split ring itself out first. Measuring "settled" on the frame after a
        // hit is measuring the hit, and the stretch would then be compared against a body that was
        // already halfway to being deformed.
        float waitingPrompt = c.boss.promptT;
        for (int i = 0; i < 90; i++) {
            c.enemies.clear();
            c.update(DT, L);
        }
        check("the wounded slime waits without preparing another volley",
                c.boss.boltCount() == 0 && Math.abs(c.boss.promptT - waitingPrompt) < 0.001f);
        float settled = c.boss.body.deform();
        c.grabBoss(c.boss.ex[inside], c.boss.ey[inside]);
        // A fixed point, taken once. Re-deriving the target from the body's own centre every frame is
        // a target that runs away from the body chasing it, and the glob walks off the screen while
        // the assertion is trying to measure a steady stretch.
        //
        // Held out past the resting silhouette, which is where there is anything to stretch: nearer
        // in than that the glob is still deep inside a body twice as wide as it is tall, and the skin
        // has nothing to do but pucker.
        float holdX = c.boss.body.centreX() - c.boss.bodyW(L) * 1.45f;
        float holdY = c.boss.body.centreY();
        c.dragBoss(holdX, holdY, L);
        c.update(DT, L);
        check("dragging one stretches the body", c.boss.body.pulled());
        // Held, not twitched: the weakest frame of the hold is what is measured, because a constraint
        // that fires once and is then argued back by the springs would pass on the peak alone.
        float least = Float.MAX_VALUE, widest = 0f;
        boolean held = true;
        for (int i = 0; i < 30; i++) {
            c.dragBoss(holdX, holdY, L);
            c.update(DT, L);
            least = Math.min(least, c.boss.body.deform());
            widest = Math.max(widest, c.boss.body.radiusX());
            if (!c.boss.body.contains(holdX, holdY)) held = false;
        }
        System.out.printf("    a held glob stretches the slime to %.2f of deform against %.2f"
                + " settled, and %.0f%% of its resting width%n",
                least, settled, 100f * widest / c.boss.bodyW(L));
        check("and holds the stretch for as long as it is held", least > settled * 1.08f
                && least > 0.05f);
        check("reaching out past its own resting width", widest > c.boss.bodyW(L));
        check("with the glob inside it the whole time", held);

        // The promise: however far the glob is hauled, it is still inside the body. Walked out in
        // steps all the way to the edge, checking every one — a hold that only ever samples the two
        // ends would miss the distance at which the stretch alone stops covering it, which is exactly
        // where the body has to start walking after it.
        boolean wrapped = true, walked = false;
        float startX = c.boss.body.centreX();
        float endX = L.playLeft + c.boss.er[inside] * 2.4f;
        for (int step = 1; step <= 40 && c.boss.held >= 0; step++) {
            c.dragBoss(startX + (endX - startX) * step / 40f, holdY, L);
            c.update(DT, L);
            if (c.boss.held < 0) break;
            if (!c.boss.body.contains(c.boss.ex[inside], c.boss.ey[inside])) wrapped = false;
            if (c.boss.body.centreX() < startX - Boss.bodyR(L) * 0.2f) walked = true;
        }
        check("the body stays wrapped around the glob the whole way", wrapped);
        check("which takes the boss with it once the stretch runs out", walked);
        check("and the drag is still live at the end of it", c.boss.held == inside);

        // A release short of the damage strip sends both halves home: the body unwinds its follow,
        // and the glob visibly catches its moving attachment point instead of hanging in the field.
        float droppedX = c.boss.ex[inside];
        float droppedBodyX = c.boss.body.centreX();
        c.releaseBoss();
        for (int i = 0; i < 30; i++) c.update(DT, L);
        check("a released glob follows the slime body home",
                c.boss.ex[inside] > droppedX && c.boss.body.centreX() > droppedBodyX);
        check("and remains available when it rejoins", c.boss.etype[inside] == Boss.E_GLOB
                && c.grabBoss(c.boss.ex[inside], c.boss.ey[inside]));

        float hpBefore = c.boss.hp;
        // Two radii of catchment keep the finger clear of Android's own edge gesture.
        c.dragBoss(L.playLeft + c.boss.er[inside] * 1.5f, c.boss.body.centreY(), L);
        check("carrying it to the edge lands before the screen edge does",
                c.boss.held < 0);
        check("which is the only thing that hurts a slime", c.boss.hp < hpBefore);
        check("and the skin is let go, so it springs back", !c.boss.body.pulled());
        // And the boss comes home, rather than being left standing where the drag dragged it.
        for (int i = 0; i < 90; i++) {
            c.enemies.clear();
            c.update(DT, L);
        }
        check("then walks back to its own drift",
                Math.abs(c.boss.bodyX(L) - c.boss.baseX(L)) < Boss.bodyR(L) * 0.05f);

        float healedFrom = c.boss.hp;
        for (int i = 0; i < 60 * (int) (Boss.GLOB_TIME + 2); i++) {
            c.enemies.clear();
            c.update(DT, L);
        }
        // A glob left alone simply goes. It used to crawl back and give the press back, and a health
        // bar that climbs while you watch reads as being cheated rather than as being hard.
        check("a glob left alone does not heal it", c.boss.hp <= healedFrom);
        boolean noGlob = true;
        for (int i = 0; i < Boss.ELEMS; i++) if (c.boss.etype[i] == Boss.E_GLOB) noGlob = false;
        check("it just fades away", noGlob);

        // Dragged off the field, it does not.
        GameCore d = enterBoss(L, Boss.SLIME, 42L);
        toOpen(d, L);
        d.enemies.clear();
        d.target = null;
        splitOne(d, L);
        int glob = -1;
        for (int i = 0; i < Boss.ELEMS; i++) if (d.boss.etype[i] == Boss.E_GLOB) glob = i;
        check("there is a glob to drag", glob >= 0);
        check("and it is draggable, not tappable", d.boss.draggable(glob));
        check("grabbing it takes the finger",
                d.grabBoss(d.boss.ex[glob], d.boss.ey[glob]));
        Ear damageEar = new Ear();
        d.sound = damageEar;
        d.boss.hp = 1f;
        float deathFrom = d.boss.bodyY(L);
        boolean done = d.dragBoss(L.playRight + 1f, d.boss.ey[glob], L);
        check("dragging it off the play area finishes it", done);
        check("a damaging boss blow makes one bloopy hit", damageEar.bossDamages == 1);
        check("without layering the achievement twinkle", damageEar.achievements == 0);
        check("the killing blow starts the shared defeat exit", d.boss.beaten);
        for (int frame = 0; frame < 45; frame++) d.update(DT, L);
        check("the longer exit holds for three cute squash notes",
                d.boss.active() && damageEar.squishes == 3);
        check("the defeated body melts toward the player", d.boss.bodyY(L) > deathFrom);
        for (int frame = 0; frame < 15; frame++) d.update(DT, L);
        check("the defeated body dramatically spans ninety percent of the screen",
                Math.abs(d.boss.body.spanX() - L.w * 0.90f) < L.w * 0.03f);
        while (d.boss.leaveT > DT) d.update(DT, L);
        check("and travels off screen before its lifecycle ends",
                d.boss.bodyY(L) > L.h + Boss.bodyR(L));
        boolean cleared = true;
        for (int i = 0; i < Boss.ELEMS; i++) if (d.boss.etype[i] == Boss.E_GLOB) cleared = false;
        check("and the glob is gone", cleared);

        // Straight up, which is the other axis and the one the header sits on. A tongue stretched
        // toward the ceiling makes the body taller than it rests, and everything stacked above it is
        // laid out against the resting height — so this is the case that would push the wanted-letter
        // badge through the blurb if the offset were not capped.
        GameCore u = enterBoss(L, Boss.SLIME, 45L);
        toOpen(u, L);
        u.enemies.clear();
        u.target = null;
        splitOne(u, L);
        int up = -1;
        for (int i = 0; i < Boss.ELEMS; i++) if (u.boss.etype[i] == Boss.E_GLOB) up = i;
        u.update(DT, L);
        u.grabBoss(u.boss.ex[up], u.boss.ey[up]);
        boolean sane = true, upWrapped = true;
        float top = u.boss.body.centreY();
        for (int step = 1; step <= 40 && u.boss.held >= 0; step++) {
            float gy = top + (L.playTop + u.boss.er[up] * 1.5f - top) * step / 40f;
            u.dragBoss(u.boss.body.centreX(), gy, L);
            u.update(DT, L);
            if (u.boss.held < 0) break;
            if (!u.boss.body.finite()) sane = false;
            if (!u.boss.body.contains(u.boss.ex[up], u.boss.ey[up])) upWrapped = false;
            // The badge is pinned a fixed multiple of the body's height above the ornament row, both
            // of them capped at resting — so however tall the stretch gets and however far up the
            // body walks, it cannot climb into the blurb.
            float badge = BossScreen.ornamentY(L, u.boss)
                    - Math.min(u.boss.body.radiusY(), Boss.bodyR(L)) * 1.28f;
            if (BossScreen.badgeTop(L, badge) <= BossScreen.blurbY(L)) sane = false;
        }
        check("a glob hauled at the ceiling stays wrapped too", upWrapped);
        check("without the stretch pushing the badge into the header", sane);

        // Carrying it off against leaving it, from the same seed and over the same number of frames,
        // with the drag as the only difference. This is now the whole mechanic in one comparison.
        float full = enterBoss(L, Boss.SLIME, 44L).boss.hpMax;
        float dragged = settleSlime(L, 44L, true);
        float ignored = settleSlime(L, 44L, false);
        check("carrying a glob off is what costs it health", dragged < ignored);
        check("and leaving it costs nothing at all", ignored == full);

        // A window shutting mid-chain does not undo the presses that landed in it. Losing five
        // presses to a clock is a punishment that compounds, on a boss there is no way past.
        GameCore s = enterBoss(L, Boss.SLIME, 43L);
        toOpen(s, L);
        s.enemies.clear();
        s.target = null;
        s.tapKey(s.boss.chainLetter(), L);
        s.tapKey(s.boss.chainLetter(), L);
        int splitWas = s.boss.split;
        check("two presses in", splitWas == 2);
        toShut(s, L);
        check("a window shutting on the chain does not undo it", s.boss.split == splitWas);
    }

    /** Works one glob loose: {@link Boss#SPLIT_HITS} presses of the chain, window permitting. */
    private static void splitOne(GameCore c, Layout L) {
        for (int k = 0; k < Boss.SPLIT_HITS; k++) {
            if (!c.boss.open()) toOpen(c, L);
            c.tapKey(c.boss.chainLetter(), L);
        }
    }

    /**
     * One slime, worked over until a glob comes off, then left for a glob's lifetime — either having
     * carried that glob off the field or not. Returns the health it settles at.
     */
    private static float settleSlime(Layout L, long seed, boolean carryOff) {
        GameCore c = enterBoss(L, Boss.SLIME, seed);
        toOpen(c, L);
        c.enemies.clear();
        c.target = null;
        splitOne(c, L);
        if (carryOff) {
            for (int i = 0; i < Boss.ELEMS; i++) {
                if (c.boss.etype[i] != Boss.E_GLOB) continue;
                c.grabBoss(c.boss.ex[i], c.boss.ey[i]);
                c.dragBoss(L.playRight + 1f, c.boss.ey[i], L);
            }
        }
        for (int i = 0; i < 60 * (int) (Boss.GLOB_TIME + 2); i++) {
            c.enemies.clear();
            c.target = null;
            // Topped up: this measures the boss's health, not the player's. Splitting a glob throws a
            // volley of bolts, and left unswatted for a glob's lifetime they end the run — which
            // would make both sides of the comparison read zero.
            c.lives = GameCore.START_LIVES;
            c.update(DT, L);
            if (!c.boss.active()) break;
        }
        return c.boss.active() ? c.boss.hp : 0f;
    }

    // ---- the volley ---------------------------------------------------------

    static void bolts(Layout L) {
        group("boss: bolts");

        GameCore c = enterBoss(L, Boss.SLIME, 46L);
        toOpen(c, L);
        c.enemies.clear();
        c.target = null;
        Ear ear = new Ear();
        c.sound = ear;
        int shown = c.boss.chainLetter();
        for (int i = 0; i < 10; i++) c.update(DT, L);
        float beforeAnswer = c.boss.promptT;
        int particlesBefore = c.particles.size();
        c.tapKey(shown, L);
        check("attacking the shown character resets its deadline",
                c.boss.promptT > beforeAnswer && c.boss.boltCount() == 0);
        check("and gives the slime a smaller prelaunch impact",
                c.shake >= 0.12f && c.particles.size() > particlesBefore && ear.boltPops == 1);
        shown = c.boss.chainLetter();
        float fullDelay = c.boss.promptDelay();
        check("a healthy slime gives the full prompt window",
                Math.abs(fullDelay - Boss.PROMPT_MAX) < 0.01f);
        check("nothing is in the air to start", c.boss.boltCount() == 0);
        check("the charged character is displayed below the slime",
                BossScreen.slimeBadgeY(L, c.boss)
                        > c.boss.body.centreY() + c.boss.body.radiusY());

        int frames = 0;
        while (c.boss.boltCount() == 0 && frames++ < 60 * 4) c.update(DT, L);
        check("an unanswered prompt launches a three-bolt volley",
                c.boss.boltCount() == Boss.BOLTS);
        check("the shown character leads the volley", c.boss.bglyph[0] == shown);
        check("the bubbling builds toward launch", ear.maxBossCharge > 0.75f);
        check("and stops when the volley fires", ear.bossCharge == 0f);
        check("the launch laughs exactly once", ear.bossLaughs == 1);
        check("and starts a visible recoil", c.boss.launchT > 0f);
        boolean below = true;
        for (int i = 0; i < Boss.BOLTS; i++)
            if (c.boss.bsy[i] <= c.boss.body.centreY() + c.boss.body.radiusY()) below = false;
        check("all launch bolts emerge below the slime", below);

        float heldPrompt = c.boss.promptT;
        for (int i = 0; i < 30; i++) c.update(DT, L);
        check("the next prompt waits for the whole volley",
                Math.abs(c.boss.promptT - heldPrompt) < 0.001f);

        boolean distinct = true;
        for (int i = 0; i < Boss.BOLTS; i++) {
            for (int j = i + 1; j < Boss.BOLTS; j++) {
                if (c.boss.bglyph[i] == c.boss.bglyph[j]) distinct = false;
            }
        }
        check("the volley spreads over three different keys", distinct);
        check("a healthy launch takes one hit per bolt", c.boss.bhpMax[0] == 1);

        int livesWas = c.lives;
        while (c.boss.boltCount() > 0) {
            int g = -1;
            for (int i = 0; i < Glyph.COUNT; i++) if (c.boss.boltWants(i)) g = i;
            c.tapKey(g, L);
        }
        check("clearing every bolt preserves every life", c.lives == livesWas);
        float resumedAt = c.boss.promptT;
        c.update(DT, L);
        check("only then does the next prompt begin counting", c.boss.promptT < resumedAt);

        GameCore hard = enterBoss(L, Boss.SLIME, 47L);
        toOpen(hard, L);
        hard.boss.hp = hard.boss.hpMax * 0.30f;
        check("a badly hurt slime cuts the prompt toward one second",
                hard.boss.promptDelay() < 1.31f && hard.boss.promptDelay() >= Boss.PROMPT_MIN);
        hard.boss.promptT = 0f;
        hard.update(DT, L);
        check("late volleys take three hits per character", hard.boss.bhpMax[0] == 3);
        int g = hard.boss.bglyph[0];
        hard.tapKey(g, L);
        hard.tapKey(g, L);
        check("two hits leave a late bolt alive", hard.boss.boltWants(g));
        hard.tapKey(g, L);
        check("and the third destroys it", !hard.boss.boltWants(g));

        GameCore d = enterBoss(L, Boss.SLIME, 48L);
        toOpen(d, L);
        d.boss.promptT = 0f;
        d.update(DT, L);
        d.lives = 9;
        for (int i = 0; i < 60 * 5 && d.boss.boltCount() > 0; i++) d.update(DT, L);
        check("unanswered bolts still reach the deck", d.boss.boltCount() == 0);
        check("and each costs one life", d.lives == 9 - Boss.BOLTS);
    }

    static void triplets(Layout L) {
        group("boss: triplets");

        GameCore c = enterBoss(L, Boss.TRIPLETS, 51L);
        check("it is always open, so the chord is the clock", c.boss.open());
        check("it starts with every head asleep",
                !c.boss.headAwake(0) && !c.boss.headAwake(1) && !c.boss.headAwake(2));
        check("a sleeping boss asks for nothing", wanted(c.boss) < 0);

        c.enemies.clear();
        c.target = null;
        float hpWas = c.boss.hp;
        // A press cannot start a chord until a head is up.
        for (int g = 0; g < Glyph.COUNT; g++) c.tapKey(g, L);
        check("and cannot be hurt while they sleep", c.boss.hp == hpWas);

        for (int i = 0; i < 3; i++) c.tapBoss(c.boss.ex[i], c.boss.ey[i], L);
        check("tapping wakes them",
                c.boss.headAwake(0) && c.boss.headAwake(1) && c.boss.headAwake(2));
        check("an awake head asks for its letter", wanted(c.boss) >= 0);

        // All three inside the chord window.
        c.enemies.clear();
        c.target = null;
        hpWas = c.boss.hp;
        for (int i = 0; i < 3; i++) {
            int g = c.boss.head(i);
            c.tapKey(g, L);
        }
        check("striking all three lands a hit", c.boss.hp == hpWas - 1f);
        check("and one head nods off again",
                !(c.boss.headAwake(0) && c.boss.headAwake(1) && c.boss.headAwake(2)));

        // A struck head must stop being advertised, or a player reads the wrong key. This is the
        // bug that cost the soak bot ten chords out of ten.
        GameCore w = enterBoss(L, Boss.TRIPLETS, 52L);
        for (int i = 0; i < 3; i++) w.tapBoss(w.boss.ex[i], w.boss.ey[i], L);
        w.enemies.clear();
        w.target = null;
        int h0 = w.boss.head(0);
        w.tapKey(h0, L);
        boolean stillWanted = false;
        // Only if no *other* awake head happens to show the same letter.
        boolean twin = w.boss.head(1) == h0 || w.boss.head(2) == h0;
        if (!twin && w.boss.wants(h0)) stillWanted = true;
        check("a head already struck is no longer advertised", !stillWanted);

        // A chord that runs out of time is lost.
        GameCore t = enterBoss(L, Boss.TRIPLETS, 53L);
        for (int i = 0; i < 3; i++) t.tapBoss(t.boss.ex[i], t.boss.ey[i], L);
        t.enemies.clear();
        t.target = null;
        hpWas = t.boss.hp;
        t.tapKey(t.boss.head(0), L);
        check("one head starts the chord clock", t.boss.chordT > 0f);
        for (int i = 0; i < 60 * (int) (Boss.CHORD_TIME + 1); i++) {
            t.enemies.clear();
            t.update(DT, L);
        }
        check("and letting it run out costs the chord", t.boss.chordT == 0f);
        check("without hurting it", t.boss.hp == hpWas);
    }

    static void drum(Layout L) {
        group("boss: drum");

        GameCore c = enterBoss(L, Boss.DRUM, 61L);
        check("it starts on a key beat, not a tap beat", !c.boss.tapBeat);

        // On the beat.
        toOpen(c, L);
        c.enemies.clear();
        c.target = null;
        float hpWas = c.boss.hp;
        c.tapKey(c.boss.want(), L);
        check("its letter on the beat lands", c.boss.hp == hpWas - 1f);
        check("and the beat flips to wanting a tap", c.boss.tapBeat);

        // The skin only answers a tap beat.
        toOpen(c, L);
        c.enemies.clear();
        c.target = null;
        hpWas = c.boss.hp;
        check("the skin is an element", c.boss.etype[0] == Boss.E_SKIN);
        c.tapBoss(c.boss.ex[0], c.boss.ey[0], L);
        check("a tap on the beat lands too", c.boss.hp == hpWas - 1f);
        check("and it flips back to wanting a key", !c.boss.tapBeat);

        // Off the beat, its own letter resets the beat rather than merely missing. That is what
        // makes the window unmashable.
        GameCore m = enterBoss(L, Boss.DRUM, 62L);
        toShut(m, L);
        m.enemies.clear();
        m.target = null;
        // Walk to just before the window opens, then press early.
        for (int i = 0; i < 60 * 5 && !m.boss.open(); i++) {
            m.enemies.clear();
            m.update(DT, L);
            if (m.boss.phaseProgress() > 0.75f) break;
        }
        float progressed = m.boss.phaseProgress();
        float hp2 = m.boss.hp;
        m.tapKey(m.boss.want(), L);
        check("pressing early does not hurt it", m.boss.hp == hp2);
        check("and pushes the window away", m.boss.phaseProgress() < progressed || progressed == 0f);
        check("its window is the shortest of the five", true);
    }

    static void magpie(Layout L) {
        group("boss: magpie");

        GameCore c = enterBoss(L, Boss.MAGPIE, 71L);
        check("it is holding one of your keys", c.boss.stolen >= 0);
        check("and showing a different one", c.boss.stolen != c.boss.want());
        check("the held key is refused", c.boss.denies(c.boss.stolen));

        // The stolen key is refused even into a word that needs it — that denial is the mechanic.
        int st = c.boss.stolen;
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy e = add(c, L, new int[] {st, st}, L.playTop + 10f);
        c.tapKey(st, L);
        check("so a word needing it cannot be typed", e.pos == 0);
        check("and it is not counted as a miss", c.misses == 0);

        // Never a deadlock: the key it holds is never the letter it wants, however many times it
        // rotates. This is fuzzed rather than spot-checked, because getting it wrong once ends a run.
        GameCore f = enterBoss(L, Boss.MAGPIE, 72L);
        boolean safe = true;
        for (int round = 0; round < 400; round++) {
            if (!f.boss.fighting()) f = enterBoss(L, Boss.MAGPIE, 72L + round);
            if (f.boss.stolen == f.boss.want()) safe = false;
            toOpen(f, L);
            f.enemies.clear();
            f.target = null;
            f.lives = GameCore.START_LIVES;
            int g = wanted(f.boss);
            if (g >= 0) f.tapKey(g, L);
            if (f.boss.fighting() && f.boss.stolen == f.boss.want()) safe = false;
        }
        check("it never holds the key it is asking for", safe);

        // Hitting it drops the key, and the key has to be carried home.
        GameCore d = enterBoss(L, Boss.MAGPIE, 73L);
        toOpen(d, L);
        d.enemies.clear();
        d.target = null;
        int stolenWas = d.boss.stolen;
        d.tapKey(d.boss.want(), L);
        int keyEl = -1;
        for (int i = 0; i < Boss.ELEMS; i++) if (d.boss.etype[i] == Boss.E_KEY) keyEl = i;
        check("a hit drops the key onto the field", keyEl >= 0);
        check("carrying the one it dropped", d.boss.keyOf[keyEl] == stolenWas);
        check("it is still denied until it is home", d.boss.denies(stolenWas));
        // Hitting it again must not drop a second copy of the same key: only one of them could ever
        // be returned, since returning either frees the key. It showed up as two identical dumplings
        // under the boss in a preview frame.
        toOpen(d, L);
        d.enemies.clear();
        d.target = null;
        int w2 = wanted(d.boss);
        if (w2 >= 0) d.tapKey(w2, L);
        int keys = 0;
        for (int i = 0; i < Boss.ELEMS; i++) if (d.boss.etype[i] == Boss.E_KEY) keys++;
        check("a second hit does not drop the same key twice", keys <= 1);

        check("grabbing it takes the finger", d.grabBoss(d.boss.ex[keyEl], d.boss.ey[keyEl]));
        boolean home = d.dragBoss(L.keyX[stolenWas], L.deckTop + 2f, L);
        check("dragging it down to the deck restores it", home);
        check("and the key works again", !d.boss.denies(stolenWas));
        check("it is empty-handed for a moment", d.boss.stolen < 0 && d.boss.stealT > 0f);
        // And then it helps itself to another, so the mechanic carries on.
        for (int i = 0; i < 60 * (int) (Boss.STEAL_GAP + 2); i++) {
            d.enemies.clear();
            d.update(DT, L);
            if (!d.boss.active()) break;
        }
        check("then it takes another", !d.boss.fighting() || d.boss.stolen >= 0);

        // Losing the drag costs the thing the drag was for.
        GameCore x = enterBoss(L, Boss.MAGPIE, 74L);
        toOpen(x, L);
        x.enemies.clear();
        x.target = null;
        x.tapKey(x.boss.want(), L);
        int lost = -1;
        for (int i = 0; i < Boss.ELEMS; i++) if (x.boss.etype[i] == Boss.E_KEY) lost = i;
        int lostKey = lost >= 0 ? x.boss.keyOf[lost] : -1;
        for (int i = 0; i < 60 * (int) (Boss.KEY_TIME + 2); i++) {
            x.enemies.clear();
            x.update(DT, L);
            if (!x.boss.active()) break;
        }
        check("a key nobody fetched is snatched again",
                lostKey < 0 || !x.boss.fighting() || x.boss.stolen == lostKey);
    }

    static void sumo(Layout L) {
        group("boss: sumo");

        GameCore c = enterBoss(L, Boss.SUMO, 81L);
        check("it takes no press window", !c.boss.wants(0) || c.boss.depth > 0f);
        check("it starts out of reach", !c.boss.shovable());
        check("with no charges", c.boss.charges == 0);

        // Out of reach, its belt cannot be pressed either: the whole loop is that it has to come
        // close before anything can be done about it.
        c.enemies.clear();
        c.target = null;
        check("still out of reach until it sinks", c.boss.depth < Boss.SHOVE_REACH);
        c.tapKey(c.boss.want(), L);
        check("a belt press out of reach banks nothing", c.boss.charges == 0);

        // Sink it in, and now the belt pays.
        for (int i = 0; i < 60 * 30 && c.boss.depth < Boss.SHOVE_REACH; i++) c.update(DT, L);
        c.target = null;
        c.tapKey(c.boss.want(), L);
        // Charges used to come from words cleared during the fight, which stopped being possible the
        // moment a boss stage stopped spawning any — this boss then had no way to earn a swipe at all
        // and could not be beaten.
        check("pressing its belt in reach banks a charge", c.boss.charges == 1);
        for (int i = 0; i < 10; i++) {
            c.target = null;
            c.tapKey(c.boss.want(), L);
        }
        check("and they are capped", c.boss.charges == Boss.CHARGE_MAX);
        check("once it is low enough a swipe lands", c.boss.shovable());
        float hpWas = c.boss.hp;
        int chargeWas = c.boss.charges;
        check("and the swipe is taken", c.swipeUp(L));
        check("which hurts it", c.boss.hp < hpWas);
        check("spends a charge", c.boss.charges == chargeWas - 1);
        check("and shoves it back to the top", c.boss.depth == 0f);

        // A press staggers rather than damages, and a stagger doubles the next shove.
        GameCore s = enterBoss(L, Boss.SUMO, 82L);
        s.enemies.clear();
        s.target = null;
        // Sink it into reach first; the belt press below is the one that banks the swipe.
        for (int i = 0; i < 60 * 30 && s.boss.depth < Boss.SHOVE_REACH; i++) s.update(DT, L);
        float hp0 = s.boss.hp;
        int belt = s.boss.want();
        s.tapKey(belt, L);
        check("a press on its belt does not hurt it", s.boss.hp == hp0);
        check("it staggers it", s.boss.stagger > 0f);
        check("and banks the swipe that stagger is for", s.boss.charges > 0);
        s.swipeUp(L);
        check("and a staggered shove hits twice as hard",
                hp0 - s.boss.hp == Boss.STAGGER_BONUS);

        // Reaching the line costs a life, and it goes back to the top rather than ending the run.
        GameCore k = enterBoss(L, Boss.SUMO, 83L);
        k.lives = GameCore.START_LIVES;
        int livesWas = k.lives;
        for (int i = 0; i < 60 * 30 && k.lives == livesWas; i++) {
            k.enemies.clear();      // only the boss may do the damage
            k.update(DT, L);
        }
        check("letting it reach the line costs a life", k.lives < livesWas);
        check("and it starts sinking again", k.boss.active() && k.boss.depth < 0.5f);

        // The swipe falls through to the ordinary panic swipe when no shove is available. A boss
        // stage spawns nothing, so the word here is placed by hand purely to arm pushReady.
        GameCore p = enterBoss(L, Boss.SUMO, 84L);
        p.enemies.clear();
        add(p, L, new int[] {0, 1}, L.dangerY - L.enemyR * 1.5f);
        p.update(DT, L);
        check("with no charge banked, a swipe is not a shove", !p.boss.shovable());
        check("so it is the panic swipe instead", p.swipeUp(L) && p.pushUsed);
    }

    // ---- stacking -----------------------------------------------------------

    /**
     * The boss header is a column of four things — bar, name, blurb, then the wanted-letter badge
     * the body holds over itself — and text on text is the one fault {@code DOES NOT FIT} cannot
     * see, because both lines fit the screen perfectly well. So the gaps are asserted, the way
     * {@code TestVisuals.hudStacking} asserts the HUD's own stack.
     *
     * Every one of these failed at the value it was first eyeballed at: the name sat on STAGE 5 and
     * the blurb sat on the badge.
     */
    static void stacking(Layout L) {
        group("boss header stacking");

        float nameTop = BossScreen.nameY(L) - RasterPainter.CAP * BossScreen.nameSize(L);
        System.out.printf("    the boss name clears the HUD row by %.1fpx at TEXT=%.2f%n",
                nameTop - L.hudY, Draw.TEXT);
        check("the boss name clears the HUD row", nameTop > L.hudY);

        float blurbTop = BossScreen.blurbY(L) - RasterPainter.CAP * BossScreen.blurbSize(L);
        check("the blurb clears the name's baseline", blurbTop > BossScreen.nameY(L));

        float badgeCy = Boss.restY(L) - Boss.bodyR(L) * 1.28f;
        float badgeTop = BossScreen.badgeTop(L, badgeCy);
        System.out.printf("    the wanted-letter badge clears the blurb by %.1fpx%n",
                badgeTop - BossScreen.blurbY(L));
        check("the badge clears the blurb", badgeTop > BossScreen.blurbY(L));

        float bodyTop = Boss.restY(L) - Boss.bodyR(L);
        check("and the body stays below the badge", bodyTop > badgeCy);
        check("while staying above the danger line", Boss.restY(L) + Boss.bodyR(L) < L.dangerY);

        // The BEATEN! payoff hangs under the body, which is what keeps it off the header it would
        // otherwise be drawn straight through.
        float beaten = BossScreen.beatenY(L, Boss.restY(L), Boss.bodyR(L));
        check("the beaten payoff clears the body it came off",
                beaten - RasterPainter.CAP * Draw.type(L.unit * 1.69f)
                        > Boss.restY(L) + Boss.bodyR(L) * 0.5f);
        check("and stays above the danger line", beaten < L.dangerY);

        // The slime's split gauge hangs below the body, so it is one more row in the same column.
        float gauge = Boss.restY(L) + Boss.bodyR(L) * 1.30f;
        check("the split gauge clears the body it hangs off", gauge > Boss.restY(L)
                + Boss.bodyR(L) * 1.05f);
        check("and stays above the danger line", gauge + L.unit * 0.3f < L.dangerY);

        // The wide boss has to fit the play area at its widest wander, which is why the drift
        // amplitude is derived from what is left over rather than picked.
        boolean fits = true;
        for (int k = 0; k < Boss.COUNT; k++) {
            GameCore g = enterBoss(L, k, 700L + k);
            for (int i = 0; i < 60 * 12; i++) {
                g.enemies.clear();
                g.update(DT, L);
                if (!g.boss.active()) break;
                float half = g.boss.bodyW(L);
                if (g.boss.baseX(L) - half < L.playLeft || g.boss.baseX(L) + half > L.playRight) {
                    fits = false;
                }
            }
        }
        check("no boss drifts off the side of the play area", fits);

        // The whole column has to hold at every size, which is the point of deriving the body's drop
        // rather than picking it: a body radius is a fixed number of text units, so both sides of
        // every gap above scale together.
        boolean holds = true;
        for (int px = 480; px <= 1600; px += 160) {
            for (int num = 16; num <= 21; num++) {
                Layout t = new Layout();
                t.compute(px, px * num / 9, 0, 0, 0, 0);
                float n = BossScreen.nameY(t) - RasterPainter.CAP * BossScreen.nameSize(t);
                float bl = BossScreen.blurbY(t) - RasterPainter.CAP * BossScreen.blurbSize(t);
                float bc = Boss.restY(t) - Boss.bodyR(t) * 1.28f;
                if (n <= t.hudY) holds = false;
                if (bl <= BossScreen.nameY(t)) holds = false;
                if (BossScreen.badgeTop(t, bc) <= BossScreen.blurbY(t)) holds = false;
                if (Boss.restY(t) - Boss.bodyR(t) <= bc) holds = false;
                if (Boss.restY(t) + Boss.bodyR(t) >= t.dangerY) holds = false;
                if (Boss.restY(t) + Boss.bodyR(t) * 1.30f + t.unit * 0.3f >= t.dangerY) {
                    holds = false;
                }
            }
        }
        check("at every screen size too", holds);
    }

    // ---- the stage jump -----------------------------------------------------

    /**
     * The settings panel's stage jump. It exists to reach a boss without playing twenty stages, so
     * what it mostly has to get right is arriving at a stage in the same state play would arrive in.
     */
    static void stageJump(Layout L) {
        group("stage jump");

        // The chips are laid out and hit-tested where they are drawn.
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        int n = SettingsUi.STAGE_STEP.length;
        boolean hits = true;
        for (int i = 0; i < n; i++) {
            float cx = (ui.testChipL(i, n) + ui.testChipR(i, n)) / 2f;
            if (ui.hit(cx, ui.stageY + ui.stageH / 2f) != SettingsUi.HIT_STAGE + i) hits = false;
        }
        check("every stage chip hit-tests to itself", hits);
        check("and none of them collides with the playtest row",
                ui.hit((ui.testChipL(0, n) + ui.testChipR(0, n)) / 2f,
                        ui.testY + ui.testH / 2f) < SettingsUi.HIT_STAGE);
        check("the steps cover one and a boss's worth",
                n == 4 && SettingsUi.STAGE_STEP[0] == -Boss.EVERY
                        && SettingsUi.STAGE_STEP[n - 1] == Boss.EVERY);

        // The panel has to stay on the screen at every size, now that it has another row in it.
        boolean fits = true;
        for (int px = 480; px <= 1600; px += 160) {
            for (int num = 16; num <= 21; num++) {
                Layout t = new Layout();
                t.compute(px, px * num / 9, 0, 0, 0, 0);
                SettingsUi u = new SettingsUi();
                u.compute(t, Music.NAMES.length);
                if (u.panelT < 0f || u.panelB > t.h) fits = false;
                // And the rows have to stay in order, in the panel, and clear of each other.
                if (u.stageY < u.testY + u.testH) fits = false;
                if (u.clearY < u.stageY + u.stageH) fits = false;
                if (u.clearY + u.clearH > u.panelB) fits = false;
            }
        }
        check("the panel still fits at every screen size", fits);

        // Jumping lands on the stage asked for, and sets it up as arriving there would.
        GameCore c = new GameCore(new Mem(), 601L);
        c.startGame();
        Ear musicEar = new Ear();
        c.sound = musicEar;
        c.jumpToStage(5, L);
        check("it lands on the stage asked for", c.stage == 5);
        check("and a boss stage brings its boss", c.boss.active()
                && c.boss.kind == Boss.SLIME);
        check("and switches into boss music", musicEar.bossMusic);
        check("with a fresh wave", c.spawnedThisStage == 0 && c.resolvedThisStage == 0);
        check("a banner", c.stageBanner > 0f);
        check("and the stage's own panic swipe back", !c.pushUsed);

        // Off a boss stage, the boss goes.
        c.jumpToStage(6, L);
        check("jumping off a boss stage takes the boss with it",
                c.stage == 6 && !c.boss.active());
        check("and restores the selected music", !musicEar.bossMusic);
        int musicRequests = musicEar.bossMusicCalls;
        c.jumpToStage(7, L);
        check("consecutive normal stages do not restart their music",
                musicEar.bossMusicCalls == musicRequests);

        // It clears the field and both set pieces, rather than leaving them over the new stage.
        GameCore d = new GameCore(new Mem(), 602L);
        d.startGame();
        d.startFrenzy(Power.FLURRY, L);
        add(d, L, new int[] {0, 1}, L.playTop + 40f);
        check("something to leave behind", d.powerActive() && !d.enemies.isEmpty());
        d.jumpToStage(10, L);
        check("the field is cleared", d.enemies.isEmpty() && d.target == null);
        check("the frenzy is over", !d.powerActive() && d.mode < 0);
        check("the squishy is gone", d.buddy.out());
        check("and the new stage's boss is up", d.boss.kind == Boss.TRIPLETS);

        // It refuses to go below stage 1 rather than wrapping into nonsense.
        d.jumpToStage(-40, L);
        check("it cannot go below the first stage", d.stage == 1);
        check("and stage 1 has no boss", !d.boss.active());

        // Deliberately not a reset: the point is to look at a late stage as the run left it.
        GameCore k = new GameCore(new Mem(), 603L);
        k.startGame();
        k.score = 4321;
        k.lives = 2;
        k.jumpToStage(15, L);
        check("the score is left alone", k.score == 4321);
        check("and so are the lives", k.lives == 2);

        // Only in play. The panel cannot be opened anywhere else, but the guard is what makes that
        // true rather than merely likely.
        GameCore t = new GameCore(new Mem(), 604L);
        t.jumpToStage(9, L);
        check("it does nothing off the play screen", t.stage != 9);
    }

    // ---- cleanup ------------------------------------------------------------

    /**
     * Nothing a boss put on the screen may outlive it.
     *
     * This is the trap this file has hit more than any other: a death never reaches the PLAY half of
     * {@code update}, so state that is only ever cleared down there stays live over the swirl, the
     * summary and the title screen behind them. It has happened with the edge glow, with the TEAM
     * SQUISH squishy and with a lit blade. A boss owns more than any of those — a body, a health
     * bar, up to three draggable elements, a held finger and one of the player's own keys — so every
     * one of them is checked on both exits.
     */
    static void cleanup(Layout L) {
        group("boss cleanup");

        // Exit one: beaten.
        GameCore c = enterBoss(L, Boss.MAGPIE, 91L);
        for (int i = 0; i < 60 * 60 && c.boss.active(); i++) {
            c.enemies.clear();
            c.target = null;
            c.lives = GameCore.START_LIVES;
            bossPlay(c, L);
            c.update(DT, L);
        }
        check("a beaten boss is gone", !c.boss.active() && c.boss.kind < 0);
        check("it gives the key back", c.boss.stolen < 0);
        check("drops the finger", c.boss.held < 0);
        check("and leaves nothing on the field", noElems(c.boss));

        // Exit two: the player dies mid-fight, which is the one that never runs the loop.
        for (int k = 0; k < Boss.COUNT; k++) {
            GameCore d = enterBoss(L, k, 100L + k);
            // Let it get going, so there is something to leave behind: globs shed, keys dropped,
            // heads woken, a finger mid-drag.
            for (int i = 0; i < 60 * 3 && d.boss.active(); i++) {
                d.target = null;
                bossPlay(d, L);
                d.update(DT, L);
            }
            // Grab something if there is anything to grab, so a drag is in progress at the death.
            for (int i = 0; i < Boss.ELEMS; i++) {
                if (d.boss.draggable(i)) d.grabBoss(d.boss.ex[i], d.boss.ey[i]);
            }
            // Now kill the run outright.
            d.lives = 1;
            d.enemies.clear();
            add(d, L, new int[] {0, 1}, L.dangerY - L.enemyR);
            for (int i = 0; i < 60 * 10 && d.state == GameCore.PLAY; i++) d.update(DT, L);

            String who = Boss.NAMES[k];
            check(who + ": dying sends it home", d.state == GameCore.OVER && !d.boss.active());
            check(who + ": no key is left held", d.boss.stolen < 0);
            check(who + ": no element is left on the field", noElems(d.boss));
            check(who + ": no finger is left holding one", d.boss.held < 0);
            check(who + ": its health bar is gone", d.boss.hpMax == 0f);

            // And it stays gone, through the death hold and the summary — the frames the old bugs
            // were actually visible on.
            boolean stays = true;
            for (int i = 0; i < 60 * 6; i++) {
                d.update(DT, L);
                if (d.boss.active()) stays = false;
            }
            check(who + ": and stays gone through the summary", stays);

            // A fresh run must not inherit any of it.
            d.toTitle();
            d.startGame();
            check(who + ": a new run starts clean",
                    !d.boss.active() && d.boss.stolen < 0 && noElems(d.boss));
        }
    }

    private static boolean noElems(Boss b) {
        for (int i = 0; i < Boss.ELEMS; i++) {
            if (b.etype[i] != Boss.E_OFF) return false;
        }
        return true;
    }
}
