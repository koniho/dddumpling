package com.sram.hexatype;

/** Alternation, steering, persistence and rewards for the star-path interlude. */
final class TestStars extends Check {
    static void game(Layout L) {
        group("star path minigame");
        StarPath q = new StarPath();
        q.make(new java.util.Random(7));
        q.begin(3, L);
        check("starts with a visual ready beat", q.ready());
        check("course has twenty stars", q.sx.length == 20);
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

        q.begin(3, L);
        q.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.1f;
        float x = q.x;
        q.hold(0, true);
        q.update(0.25f, L);
        check("a held left-hand key accelerates left", q.x < x && q.vx < 0f);
        q.hold(3, true);
        float before = Math.abs(q.vx);
        q.update(0.25f, L);
        check("opposite hands cancel and ease", Math.abs(q.vx) < before);

        q.collected = 0b10101;
        q.begin(3, L);
        check("failed-run stars persist when the course restarts", q.count() == 3);
        float wasAt = q.sx[7];
        q.reroll(new java.util.Random(31L));
        check("and a failed attempt is a new line, not the same one back",
                q.count() == 3 && q.sx[7] != wasAt);
        check("pickup animation resets between attempts", q.burst[0] == 0f);
        check("the steamer prize pilots the course", q.who == 3);

        GameCore c = new GameCore(new Mem(), 19L);
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
        check("the victory tableau takes the screen", c.stars.winning() && c.starFlight());
        check("it knows which star finished the course", c.stars.winStar == last);
        check("completion pays like the steamer", c.score >= score + GameCore.FREE_BONUS);
        check("completion awards a star-exclusive collectible",
                c.prize >= Collect.STAR_FIRST && c.prize < Collect.COUNT);
        check("and the prize is paid before the tableau draws it", c.stars.winning());
        check("a parade is scheduled, as on a won steamer", c.paradeTimer > 0f);

        // Frozen: the course, the flyer and the clock all hold for the length of the tableau.
        float held = c.stars.timer, atX = c.stars.x;
        c.stars.hold(0, true);
        c.update(DT, L);
        check("the course stops dead", c.stars.timer == held && c.stars.x == atX
                && c.stars.vx == 0f && !c.stars.left);
        check("the tableau runs on its own clock", c.stars.winProgress() > 0f);
        check("no parade until the tableau is done", !c.bonusParading());

        int guard = 0;
        for (; guard < 60 * 10 && !c.bonusParading(); guard++) c.update(DT, L);
        check("the tableau hands over to the parade", c.bonusParading()
                && !c.stars.winning() && c.starFlight() == false);
        for (guard = 0; guard < 60 * 20 && c.state == GameCore.BONUS; guard++) c.update(DT, L);
        check("twenty stars complete the game", !c.starNext && c.state == GameCore.PLAY);
        check("a successful course resets its persistent stars", c.stars.count() == 0);

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

        courseIsFlyable(L);
    }

    /**
     * That the course can actually be flown, and by hands rather than by a god.
     *
     * The soak bot cannot steer, so without this the only readings available were "a passenger
     * collects a few by drifting" and nothing else — and the course generator is tuned against the
     * steering, which is exactly the sort of arithmetic that stops being true after a tweak.
     */
    /** Courses measured per figure. See the note on variance where the pilots are flown. */
    private static final int SEEDS = 12;

    private static void courseIsFlyable(Layout L) {
        group("star course is flyable");

        // The peak demand ratio, which is printed and no longer asserted on, because it turned out
        // to measure almost nothing. A sine only exceeds the steering speed over a short arc either
        // side of its steepest point, so the course that "asked 1.2 times the steering" for a long
        // time cost a tracker a hundredth of a width — a twelfth of the catch band — and every pilot
        // the harness can write took all twenty at every reaction time it was given. What is worth
        // holding is what that overspeed integrates to.
        float demand = StarPath.SWEEP * StarPath.TAU / StarPath.SWEEP_TIME
                + StarPath.RIPPLE * StarPath.TAU / StarPath.RIPPLE_TIME;
        float band = StarPath.pickupR(L) / (L.playRight - L.playLeft);
        float lag = trackerLag(L);
        System.out.printf("    sweep asks %.2f widths a second, steering gives %.2f (%.2fx)%n",
                demand, StarPath.MAX_VX, demand / StarPath.MAX_VX);
        System.out.printf("    following the line costs %.3f widths, the catch band is %.3f "
                + "(%.0f%% of it)%n", lag, band, 100f * lag / band);
        // Following the line has to be a losing strategy, or the course is not asking for anything:
        // a lag worth a good part of the band is what makes it necessary to cut across instead.
        check("following the line is not enough to fly a course", lag >= band * 0.45f);
        // And the far side of it is still a cliff rather than a dial — a flyer that can never catch
        // up is not late, it is somewhere else, and which stars it gets turns to luck.
        check("but the line is not hopeless to follow either", lag <= band * 1.1f);

        // What a perfect-speed pilot who knows the whole course in advance is left to thread. Empty
        // anywhere and nobody can complete it; as wide as the catch band everywhere and it demands
        // no precision at all. This is the reachability bound the peak ratio was standing in for.
        float win = tightestWindow(L);
        System.out.printf("    the tightest window through a course is %.3f widths, %.0f%% of "
                + "the band%n", win, 100f * win / (2f * band));
        check("a course leaves a line through it", win > band * 0.20f);
        check("and asks the player to find it", win < 2f * band);

        // Reachable: the flyer's centre stops a radius inside the play area, and the sweep goes no
        // wider than that. As a fraction of the play area, that radius is the floor for EDGE.
        float stop = StarPath.flyerR(L) / (L.playRight - L.playLeft);
        check("and it never puts a checkpoint out of reach", StarPath.EDGE >= stop - 0.001f);

        // How long a checkpoint is level with the flyer. Nothing a player does moves this — the
        // climb and the scroll are both functions of the clock — so it is not difficulty, it is how
        // much of the difficulty is luck. PICKUP_TALL exists to hold it roughly where it was before
        // the stars were spaced out and the flight shortened; see the note on it.
        float window = levelWindow(L);
        System.out.printf("    a checkpoint is level with the flyer for %.0fms at the tightest%n",
                window * 1000f);
        check("a star stays level long enough to be caught deliberately", window >= 0.095f);

        // Twelve seeds, not six, and the reason is worth stating: which sweep period, direction and
        // ripple phase a course draws moves the outcome enormously — six seeds put slow thumbs at
        // 18.3 of 20 and the next six at 9.6 — and the pilot's fixed decision quantum aliases
        // against the arrival rhythm on top of that, so its score is not monotonic in reaction time
        // at a single value. Read the ends against each other over a wide sample; never one value of
        // the curve against another nearby one.
        // How much warning a player actually gets: the seconds between the course line entering the
        // top of the screen and reaching the flyer. Not something the harness pilots can speak to —
        // they know the whole course in advance — but it is the number the spacing knob moves fastest
        // and the one a hand feels, so it is printed on every run.
        float look = lookahead(L);
        System.out.printf("    the line is visible %.0fms ahead of the flyer, and a checkpoint "
                + "%.0fms after the one before it%n", look * 1000f,
                1000f * (StarPath.encounterTime(StarPath.COUNT - 1)
                        - StarPath.encounterTime(StarPath.COUNT - 2)));
        check("a player can see the course coming", look >= 0.12f);

        int sharp = 0, laggy = 0, drift = 0;
        float longest = 0f;
        for (long seed = 1L; seed <= SEEDS; seed++) {
            int got = Integer.bitCount(flown(seed, 0.05f, L, true));
            sharp += got;
            // Taken from a won run only: a course that runs its clock out instead of ending on a
            // win says nothing about what the grab beats cost, and quick thumbs no longer win
            // every course.
            if (got == StarPath.COUNT) longest = Math.max(longest, realSeconds);
            laggy += Integer.bitCount(flown(seed, 0.25f, L, true));
            drift += Integer.bitCount(flown(seed, 0.25f, L, false));
        }
        float quick = sharp / (float) SEEDS, slow = laggy / (float) SEEDS;
        System.out.printf("    quick thumbs %.1f of %d a course, slow thumbs %.1f, "
                + "a passenger %.1f%n", quick, StarPath.COUNT, slow, drift / (float) SEEDS);
        // A course has to be completable, or the prize at the end of it is decoration. It is no
        // longer the certainty it was, which is the point of the retune: this pilot brakes for
        // nothing and looks one checkpoint ahead, and it used to take all twenty at every reaction
        // time it was given.
        check("quick thumbs take nearly the whole course", quick >= StarPath.COUNT - 1.5f);

        // And they get to the end of one. Counted in attempts rather than as "wins one course in
        // one go", because that is the promise the game actually makes: the same course comes back
        // with the stars already taken still taken, so what matters is how many interludes a prize
        // costs. As a single-attempt rate it was one course in six for these seeds and five in
        // twelve for the next six — a six-sample coin flip standing where a design promise should
        // be. Note also that a repeated attempt is not a repeat: the stars in hand change which
        // checkpoint the pilot chases next, so it flies the same course on a different line.
        float quickTries = 0f, slowTries = 0f;
        for (long seed = 1L; seed <= SEEDS; seed++) {
            quickTries += attemptsToFinish(seed, 0.05f, L);
            slowTries += attemptsToFinish(seed, 0.25f, L);
        }
        System.out.printf("    a prize costs quick thumbs %.1f attempts, slow thumbs %.1f%n",
                quickTries / SEEDS, slowTries / SEEDS);
        check("quick thumbs finish a course in a couple of attempts", quickTries / SEEDS <= 2.5f);
        check("and slow thumbs get there over a few stages", slowTries / SEEDS <= 4f);
        // And it has to be worth flying badly: the stars carry over between attempts, so a slow
        // pair of thumbs is meant to get there over several stages rather than never.
        check("slow thumbs still take most of it", slow >= StarPath.COUNT * 0.55f);
        // The one that has already gone wrong once. The sweep has to leave the middle of the screen
        // far enough behind that a flyer nobody is steering cannot collect its way to a prize: the
        // stars carry over between courses, so anything it can reach it eventually completes.
        check("and a passenger cannot fly one at all",
                drift / (float) SEEDS <= StarPath.COUNT * 0.6f);
        // Reaction time has to be worth something, or the whole thing is a cutscene. This is the
        // check that would have caught the course being trackable: it used to be 20.0 against 20.0.
        // Stated as a margin over the whole sample, because at one reaction value against the next
        // the aliasing above is bigger than the effect.
        check("and thumbs a quarter-second slow pay for it", slow <= quick - 2f);

        // Every grab spends a slow-motion beat, and the course clock is inside it, so a clean run
        // is longer in real time than the flight is in game time. A second of stutter is the point;
        // three would be a different minigame.
        System.out.printf("    a clean course takes %.1fs of real time, flying %.1fs of it%n",
                longest, StarPath.FLY);
        check("the grab beats do not stretch a course out of shape",
                longest > 0f && longest <= StarPath.READY + StarPath.FLY + 1.5f);
    }

    /**
     * What a pilot who simply follows the line falls behind by at worst, in play widths — the
     * measure that replaced the peak demand ratio. It integrates the overspeed instead of sampling
     * it, so it knows the difference between a course that is briefly steep and one that is gone.
     */
    private static float trackerLag(Layout L) {
        float worst = 0f;
        for (long seed = 1L; seed <= SEEDS; seed++) {
            StarPath q = new StarPath();
            q.make(new java.util.Random(seed));
            float me = 0.5f, step = StarPath.MAX_VX * DT;
            for (float t = 0; t <= StarPath.FLY; t += DT) {
                float want = courseAt(q, t);
                me = want > me ? Math.min(want, me + step) : Math.max(want, me - step);
                worst = Math.max(worst, Math.abs(want - me));
            }
        }
        return worst;
    }

    /**
     * The narrowest place a course can be threaded through, in play widths.
     *
     * The set of positions from which every remaining checkpoint is still catchable, propagated
     * forward at the steering limit and intersected with each checkpoint's catch band in turn. It is
     * the exact reachability statement: if it ever closes, no line completes the course. Stars level
     * with the flyer at the off are skipped — the course begins under it, so they are free.
     */
    private static float tightestWindow(Layout L) {
        float band = StarPath.pickupR(L) / (L.playRight - L.playLeft);
        float tightest = 9f;
        for (long seed = 1L; seed <= SEEDS; seed++) {
            StarPath q = new StarPath();
            q.make(new java.util.Random(seed));
            float lo = 0.5f, hi = 0.5f, prev = 0f;
            for (int i = 0; i < StarPath.COUNT; i++) {
                float t = StarPath.encounterTime(i);
                float reach = StarPath.MAX_VX * (t - prev);
                prev = t;
                lo = Math.max(lo - reach, q.sx[i] - band);
                hi = Math.min(hi + reach, q.sx[i] + band);
                if (t > 0f) tightest = Math.min(tightest, hi - lo);
                if (hi < lo) { lo = hi = q.sx[i]; }
            }
        }
        return tightest;
    }

    /** Where the course wants the flyer at time {@code t}, lerped between its checkpoints. */
    private static float courseAt(StarPath q, float t) {
        for (int i = 1; i < StarPath.COUNT; i++) {
            float a = StarPath.encounterTime(i - 1), b = StarPath.encounterTime(i);
            if (t <= b && b > a) {
                float u = (t - a) / (b - a);
                return q.sx[i - 1] + (q.sx[i] - q.sx[i - 1]) * Math.max(0f, u);
            }
        }
        return q.sx[StarPath.COUNT - 1];
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

    /** Real seconds the last {@link #flown} call took, beats and all. */
    private static float realSeconds;

    /**
     * Flies one course with stated limits and returns how many of the twenty it took.
     *
     * The pilot only changes its mind every {@code reaction} seconds and holds whichever thumb the
     * next checkpoint is on — no braking and no planning, which is about what a hand does. With
     * {@code steer} false it touches nothing, which is the passenger the soak bot is.
     */
    private static int flown(long seed, float reaction, Layout L, boolean steer) {
        return flown(seed, reaction, L, steer, 0, seed);
    }

    /**
     * How many attempts a pilot needs to finish one course, the stars it has already taken carrying
     * over between them exactly as they do in play.
     */
    private static int attemptsToFinish(long seed, float reaction, Layout L) {
        int held = 0;
        for (int attempt = 1; attempt <= 6; attempt++) {
            // A different line each attempt, which is what the game hands out — see
            // StarPath.reroll. On one repeated line this pilot stalls forever partway up.
            held = flown(seed, reaction, L, true, held, seed * 31L + attempt);
            if (Integer.bitCount(held) == StarPath.COUNT) return attempt;
        }
        return 7;
    }

    /** @return the checkpoints in hand at the end of the attempt, {@code carried} included */
    private static int flown(long seed, float reaction, Layout L, boolean steer, int carried,
            long courseSeed) {
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.stars.make(new java.util.Random(courseSeed));
        c.stars.begin(-1, L);
        c.stars.collected = carried;
        float since = reaction;
        realSeconds = 0f;
        for (int i = 0; i < 60 * 20 && c.state == GameCore.BONUS && !c.stars.won; i++) {
            since += DT;
            if (steer && since >= reaction) {
                since = 0f;
                int aim = nextStar(c.stars, L);
                float dead = StarPath.pickupR(L) * 0.35f;
                float want = aim < 0 ? c.stars.x : c.stars.starX(aim, L);
                c.stars.hold(0, want < c.stars.x - dead);
                c.stars.hold(3, want > c.stars.x + dead);
            }
            c.update(DT, L);
            realSeconds += DT;
        }
        return c.stars.collected;
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
