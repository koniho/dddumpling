package com.dddumpling.game;

/** Admission checks cover full rows and both directions, including future top/side collisions. */
final class TestSideEntry extends Check {
    private TestSideEntry() {}

    static void all(Layout L) {
        group("side entry spacing");
        GameCore c = new GameCore(new Mem(), 314L);
        c.startGame();
        c.enemies.clear();
        GameCore.Enemy side = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        side.sideEntry = true;
        side.sway = 0;
        side.pathStartY = side.y;
        side.pathStartX = -L.wordWidth(2);
        side.pathEndX = L.w * 0.3f;
        side.baseX = side.pathStartX;
        side.speed = 80f;
        GameCore.Enemy other = add(c, L, new int[] {3, 4}, side.y);
        other.baseX = side.pathEndX;
        other.sway = 0;
        other.speed = side.speed;
        check("blocked landing is rejected before the rows touch", !EnemyEntry.clear(side, c, L));
        check("top entries also respect a reserved side lane", !EnemyEntry.clear(other, c, L));
        other.baseX = L.w * 0.85f;
        check("a separate lane remains available", EnemyEntry.clear(side, c, L));
        other.baseX = side.pathEndX;
        other.y = side.y - L.enemyR * 5f;
        other.speed = side.speed * 2f;
        check("faster trailing words cannot catch the side row", !EnemyEntry.clear(other, c, L));
        other.speed = side.speed;
        check("vertically spaced rows may share a lane", EnemyEntry.clear(other, c, L));
        other.y = side.y;
        other.destroyed = true;
        check("cleared words release their lane", EnemyEntry.clear(side, c, L));

        GameCore blocked = new GameCore(new Mem(), 315L);
        blocked.startGame();
        blocked.enemies.clear();
        GameCore.Enemy barrier = add(blocked, L, new int[] {1, 2, 3, 4, 5, 0, 1, 2},
                -L.enemyR * 2.2f);
        barrier.sideEntry = true;
        barrier.sway = 0f;
        barrier.pathStartY = barrier.y;
        barrier.pathStartX = barrier.pathEndX = barrier.baseX = L.w / 2f;
        barrier.speed = 0f;
        int quota = blocked.spawnedThisStage;
        blocked.spawnTimer = 0f;
        blocked.update(DT, L);
        check("blocked entrance waits without consuming the wave quota",
                blocked.enemies.size() == 1 && blocked.spawnedThisStage == quota
                && blocked.spawnTimer > 0f);
        blocked.enemies.clear();
        blocked.update(0.12f, L);
        check("the deferred word spawns once its entrance clears",
                blocked.enemies.size() == 1 && blocked.spawnedThisStage == quota + 1);

        // Exercise actual mixed spawning and motion at several device shapes. The check uses
        // character extents, independently of the admission predictor's larger safety padding.
        boolean noOverlap = true;
        int sideCount = 0, topCount = 0;
        for (int shape = 0; shape < 3; shape++) {
            Layout size = new Layout();
            size.compute(shape == 0 ? 360 : 1080, shape == 2 ? 1920 : 2340, 0, 60, 0, 90);
            GameCore run = new GameCore(new Mem(), 170L + shape);
            run.startGame();
            run.enemies.clear();
            run.playtestMode(Power.FLURRY, size);
            for (int frame = 0; frame < 240; frame++) {
                int before = run.enemies.size();
                run.update(DT, size);
                if (run.enemies.size() > before) {
                    if (run.enemies.get(run.enemies.size() - 1).sideEntry) sideCount++; else topCount++;
                }
                for (int i = 0; i < run.enemies.size(); i++) {
                    GameCore.Enemy a = run.enemies.get(i);
                    for (int j = i + 1; j < run.enemies.size(); j++) {
                        GameCore.Enemy b = run.enemies.get(j);
                        if ((!a.sideEntry && !b.sideEntry) || a.dying || b.dying
                                || a.destroyed || b.destroyed) continue;
                        float dx = Math.abs(run.enemyCentreX(a) - run.enemyCentreX(b));
                        float dy = Math.abs(a.y - b.y);
                        float rowGap = (size.wordWidth(a.word.length) + size.wordWidth(b.word.length)) / 2f;
                        if (dx < rowGap && dy < size.enemyR * 2.2f) noOverlap = false;
                    }
                }
            }
        }
        check("mixed frenzy traffic includes both entrance types", sideCount > 0 && topCount > 0);
        check("side rows do not overlap mixed traffic during entry or descent", noOverlap);
    }
}
