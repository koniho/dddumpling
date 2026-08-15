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

        courseIsFlyable(L);
    }

    /**
     * That the course can actually be flown, and by hands rather than by a god.
     *
     * The soak bot cannot steer, so without this the only readings available were "a passenger
     * collects a few by drifting" and nothing else — and the course generator is tuned against the
     * steering, which is exactly the sort of arithmetic that stops being true after a tweak.
     */
    private static void courseIsFlyable(Layout L) {
        group("star course is flyable");

        // The sweep is generated against the steering, and deliberately asks for a little more than
        // it: a course that can be tracked is a course every pilot completes with any reaction time,
        // so the difficulty here is having to anticipate the line rather than follow it. Both terms
        // peak together in the worst case.
        //
        // The bound is a cliff rather than a preference. Measured against the pilot below, 1.2 times
        // the steering speed still completes a course and 1.5 times drops it to a third of one — a
        // flyer that can never catch up is not late, it is somewhere else — so past about 1.25 the
        // game gets worse instead of harder.
        float demand = StarPath.SWEEP * StarPath.TAU / StarPath.SWEEP_TIME
                + StarPath.RIPPLE * StarPath.TAU / StarPath.RIPPLE_TIME;
        System.out.printf("    sweep asks %.2f widths a second, steering gives %.2f (%.2fx)%n",
                demand, StarPath.MAX_VX, demand / StarPath.MAX_VX);
        check("a course asks more of the player than following the line",
                demand > StarPath.MAX_VX);
        check("but stays the near side of uncatchable", demand <= StarPath.MAX_VX * 1.25f);

        // Reachable: the flyer's centre stops a radius inside the play area, and the sweep goes no
        // wider than that. As a fraction of the play area, that radius is the floor for EDGE.
        float stop = StarPath.flyerR(L) / (L.playRight - L.playLeft);
        check("and it never puts a checkpoint out of reach", StarPath.EDGE >= stop - 0.001f);

        int sharp = 0, laggy = 0, drift = 0;
        float longest = 0f;
        for (long seed = 1L; seed <= 6L; seed++) {
            sharp += flown(seed, 0.05f, L, true);
            // Taken from the clean run only: a course nobody steers runs its clock out instead of
            // ending on a win, so its real length says nothing about what the beats cost.
            longest = Math.max(longest, realSeconds);
            laggy += flown(seed, 0.25f, L, true);
            drift += flown(seed, 0.25f, L, false);
        }
        System.out.printf("    quick thumbs %.1f of %d a course, slow thumbs %.1f, "
                + "a passenger %.1f%n", sharp / 6f, StarPath.COUNT, laggy / 6f, drift / 6f);
        // A course has to be completable, or the prize at the end of it is decoration.
        check("quick thumbs take nearly the whole course", sharp / 6f >= StarPath.COUNT - 1.5f);
        // And it has to be worth flying badly: the stars carry over between attempts, so a slow
        // pair of thumbs is meant to get there over several stages rather than never.
        check("slow thumbs still take most of it", laggy / 6f >= StarPath.COUNT * 0.55f);
        // The one that has already gone wrong once. The sweep has to leave the middle of the screen
        // far enough behind that a flyer nobody is steering cannot collect its way to a prize: the
        // stars carry over between courses, so anything it can reach it eventually completes.
        check("and a passenger cannot fly one at all", drift / 6f <= StarPath.COUNT * 0.6f);

        // Every grab spends a slow-motion beat, and the course clock is inside it, so a clean run
        // is longer in real time than the flight is in game time. A second of stutter is the point;
        // three would be a different minigame.
        System.out.printf("    a clean course takes %.1fs of real time, flying %.1fs of it%n",
                longest, StarPath.FLY);
        check("the grab beats do not stretch a course out of shape",
                longest <= StarPath.READY + StarPath.FLY + 1.5f);
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
        GameCore c = new GameCore(new Mem(), seed);
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.stars.make(new java.util.Random(seed));
        c.stars.begin(-1, L);
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
        return c.stars.count();
    }

    /** The next checkpoint still worth chasing: not taken, and not yet past the flyer. */
    private static int nextStar(StarPath q, Layout L) {
        for (int i = 0; i < StarPath.COUNT; i++) {
            if ((q.collected & (1 << i)) != 0) continue;
            if (q.starY(i, L) > q.characterY(L) + StarPath.pickupR(L)) continue;
            return i;
        }
        return -1;
    }
}
