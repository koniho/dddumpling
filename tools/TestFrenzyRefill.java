package com.dddumpling.game;

/** Keep targets arriving after fast clears without increasing pressure on an occupied field. */
final class TestFrenzyRefill extends Check {
    private TestFrenzyRefill() {}

    static void all(Layout L) {
        group("frenzy replenishment");
        GameCore c = new GameCore(new Mem(), 827L);
        c.startGame();
        c.enemies.clear();
        c.spawnTimer = 2f;
        float calm = c.spawnInterval();
        check("ordinary waves keep their normal spawn delay", Power.spawnDelay(c, L) == calm);
        c.startFrenzy(Power.FLING, L);
        float regular = calm / Power.spawnRate(c.ramp());
        check("activation shortens the old wave timer on an empty field", c.spawnTimer < regular);
        check("empty field gets a faster replacement", Power.spawnDelay(c, L) < regular);
        GameCore.Enemy visible = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        visible.baseX = L.w / 2f;
        visible.sway = 0;
        check("visible unfinished work restores the stage's normal frenzy pace",
                Math.abs(Power.spawnDelay(c, L) - regular) < 1e-5f);
        visible.need[0] = 4;
        check("an unfinished stack never triggers extra pressure", Power.spawnDelay(c, L) == regular);
        visible.pos = visible.word.length;
        check("last hits in flight do not delay the next target", Power.spawnDelay(c, L) < regular);
        visible.y = L.dangerY - L.enemyR * 2f;
        check("a threat near the danger line suppresses replenishment", Power.spawnDelay(c, L) == regular);

        c.enemies.clear();
        for (int i = 0; i < 2; i++) add(c, L, new int[] {1, 2}, -L.enemyR * 3f);
        check("two incoming rows stop an unseen backlog", Power.spawnDelay(c, L) == regular);

        c.enemies.clear();
        GameCore.Enemy first = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        GameCore.Enemy second = add(c, L, new int[] {3, 4}, L.playTop + L.enemyR * 8f);
        c.destroyWord(first, first.baseX, first.y, L);
        check("one clear does not bank a burst", c.powerRefillBurst == 0);
        c.clock += 0.1f;
        c.destroyWord(second, second.baseX, second.y, L);
        check("a rapid multi-clear banks only two arrivals", c.powerRefillBurst == 2);
        GameCore.Enemy work = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        work.baseX = L.w / 2f;
        work.sway = 0;
        check("the short multi-clear burst keeps fresh targets coming", Power.spawnDelay(c, L) < regular);
        c.clock += 0.9f;
        check("unspent bursts expire instead of surprising the player later", Power.spawnDelay(c, L) == regular);
        c.startFrenzy(Power.FLURRY, L);
        check("a new powerup starts without stale burst credit", c.powerRefillBurst == 0);

        boolean scaled = true;
        for (int stage : new int[] {1, 7, 13, 19}) {
            c.stage = stage;
            c.enemies.clear();
            c.speed = 1f;
            float base = c.spawnInterval() / Power.spawnRate(c.ramp());
            float refill = Power.spawnDelay(c, L);
            scaled &= refill < base && refill >= base * 0.19f;
            c.speed = 2f;
            scaled &= Math.abs(Power.spawnDelay(c, L) * 2f - refill) < 1e-5f;
        }
        check("refill timing follows the stage curve and speed setting", scaled);
        c.modeLeft = 0f;
        check("ending a powerup restores ordinary spawn timing", Power.spawnDelay(c, L) == c.spawnInterval());
        rapidClears(L);
    }

    private static void rapidClears(Layout L) {
        for (int stage : new int[] {13, 19}) {
            int cleared = 0;
            float longestGap = 0f;
            // A repeatable mass-clear workload, separate from the human-limited Bot playthroughs.
            for (int seed = 1; seed <= 12; seed++) {
                GameCore c = new GameCore(new Mem(), 700L + seed);
                c.startGame();
                c.stage = stage;
                c.enemies.clear();
                c.startFrenzy(Power.FLING, L);
                c.lives = 99;
                float gap = 0f;
                for (int frame = 0; frame < 12 * 60; frame++) {
                    c.update(DT, L);
                    int visible = 0;
                    for (GameCore.Enemy e : c.enemies) {
                        float half = L.wordWidth(e.word.length) / 2f;
                        float x = c.enemyCentreX(e);
                        if (!e.typeable() || e.y - L.enemyR < L.playTop
                                || x - half < L.playLeft || x + half > L.playRight) continue;
                        visible++;
                        if (frame % 42 == 41) {
                            c.destroyWord(e, x, e.y, L);
                            cleared++;
                        }
                    }
                    if (frame >= 60) {
                        gap = visible == 0 ? gap + DT : 0f;
                        longestGap = Math.max(longestGap, gap);
                    }
                }
            }
            System.out.printf("    stage %d rapid clears: %.1f words/12s, longest gap %.2fs%n",
                    stage, cleared / 12f, longestGap);
            // Before replenishment these averaged 9.8/9.3 words and gaps exceeded 2.1 seconds.
            check("stage " + stage + " supplies targets after repeated mass clears", cleared / 12f >= 12f);
            check("stage " + stage + " avoids the old two-second gaps", longestGap < 1.9f);
        }
    }
}
