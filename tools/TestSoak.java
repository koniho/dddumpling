package com.sram.hexatype;

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
            GameCore.Enemy e = c.target != null && c.enemies.contains(c.target)
                    && c.target.typeable() ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }
        System.out.printf("    120s of perfect play: stage=%d score=%d kills=%d lives=%d%n",
                c.stage, c.score, c.kills, c.lives);
        check("perfect play keeps all lives", c.lives == GameCore.START_LIVES);
        check("perfect play survives 120s",
                c.state == GameCore.PLAY || c.state == GameCore.BONUS);
        check("perfect play reaches a late stage", c.stage >= 6);
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
            if (r.nextInt(4000) == 0) c.anyTap();
            if (c.state == GameCore.OVER) {
                overs++;
                for (int k = 0; k < 45; k++) c.update(DT, L);
                c.anyTap();
            }
            // liveEnemies(), not size(): destroyed words linger while they fly apart, and a
            // frenzy spawns fast enough for several to be in flight at once.
            if (c.lives < 0 || c.score < 0 || c.liveEnemies() > c.crowdCap() + 1
                    || c.combo < 0) {
                sane = false;
                break;
            }
        }
        check("10 minutes of random mashing stays consistent", sane);
        check("random play does reach game over", overs > 0);
        System.out.printf("    fuzz: %d game-overs, %d particles live, %d shots live%n",
                overs, c.particles.size(), c.shots.size());
    }

    // ---- helpers ------------------------------------------------------------


}
