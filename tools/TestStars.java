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
        c.stars.collected = (1 << StarPath.COUNT) - 1;
        c.stars.won = true;
        c.stars.timer = 0.001f;
        int score = c.score;
        c.update(0.01f, L);
        check("twenty stars complete the game", !c.starNext && c.state == GameCore.PLAY);
        check("completion pays like the steamer", c.score >= score + GameCore.FREE_BONUS);
        check("completion awards a star-exclusive collectible",
                c.prize >= Collect.STAR_FIRST && c.prize < Collect.COUNT);
        check("a successful course resets its persistent stars", c.stars.count() == 0);
    }
}
