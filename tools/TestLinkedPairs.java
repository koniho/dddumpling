package com.dddumpling.game;

final class TestLinkedPairs extends Check {
    private static GameCore wave(Layout L, int stage) {
        GameCore c = new GameCore(new Mem(), 1601L);
        c.startGame();
        c.jumpToStage(stage, L);
        c.stageGap = 0f;
        c.spawnTimer = 0f;
        c.powerTimer = 100f;
        c.update(DT, L);
        return c;
    }

    static void all(Layout L) {
        for (int stage = 16; stage <= 31; stage++) {
            if (stage % Boss.EVERY == 0) continue;
            GameCore wave = wave(L, stage);
            java.util.Set<GameCore.Enemy> seen = new java.util.HashSet<GameCore.Enemy>();
            int pairs = 0;
            for (int frame = 0; frame < 1800 && wave.state == GameCore.PLAY && !wave.pendingBonus; frame++) {
                for (GameCore.Enemy e : wave.enemies) {
                    if (e.link != null && seen.add(e)) {
                        seen.add(e.link);
                        pairs++;
                    }
                }
                for (GameCore.Enemy e : wave.enemies) {
                    while (e.typeable()) {
                        wave.target = e;
                        wave.tapKey(e.word[e.pos], L);
                    }
                }
                wave.update(DT, L);
            }
            check("two pairs spawn across full ordinary stage " + stage, pairs == 2);
            check("two pairs preserve wave quota at stage " + stage,
                    wave.spawnedThisStage == wave.stageQuota() && wave.resolvedThisStage == wave.stageQuota());
        }
        GameCore blocked = wave(L, 16);
        blocked.spawnedThisStage = 4;
        blocked.spawnTimer = 0f;
        blocked.update(DT, L);
        check("blocked pair keeps its quota slots reserved", blocked.spawnedThisStage == 4 && blocked.enemies.size() == 2);
        blocked.enemies.clear();
        blocked.spawnTimer = 0f;
        blocked.update(DT, L);
        check("reserved second pair spawns when entrance clears", blocked.spawnedThisStage == 6
                && blocked.enemies.size() == 2 && blocked.enemies.get(0).link == blocked.enemies.get(1));
        GameCore early = wave(L, 14);
        check("no linked enemies before stage 16", early.enemies.size() == 1 && early.enemies.get(0).link == null);
        check("boss stages do not get pairs", wave(L, 20).enemies.isEmpty());
        GameCore c = wave(L, 16);
        GameCore.Enemy a = c.enemies.get(0), b = c.enemies.get(1);
        check("stage 16 opens with a reciprocal pair", a.link == b && b.link == a);
        check("pair replaces exactly two quota enemies", c.spawnedThisStage == 2);
        check("pair is two single-press keys", a.totalPresses() == 1 && b.totalPresses() == 1);
        check("pair uses opposite thumbs", a.word[0] < 3 && b.word[0] >= 3);
        check("paired keys do not overlap", b.baseX - a.baseX > L.enemyR * 3f && a.speed == b.speed);
        check("second pair is not due immediately after the first", !LinkedPairs.spawn(c, L));
        check("later normal stages keep the pair", wave(L, 21).enemies.get(0).link != null);
        check("stage 16 gives slower falls than stage 14", Pacing.travelSeconds(16, 1f) > Pacing.travelSeconds(14, 1f));
        check("lesson has wider arrival spacing", Pacing.spawnInterval(16, 1f) > Pacing.spawnInterval(15, 1f));
        check("first lesson limits crowd to four", c.maxEnemies() == 4);
        check("stage 17 tapers the lesson relief", Pacing.lessonRelief(16) > Pacing.lessonRelief(17)
                && Pacing.lessonRelief(17) > Pacing.lessonRelief(18));
        c.tapKey(a.word[0], L);
        check("first press immediately waits without credit", a.linkWaiting && !a.destroyed
                && c.squishes == 0 && c.resolvedThisStage == 0 && c.score == 0 && c.combo == 0);
        check("one press strains both sides of the bond", a.linkStrain == 1f && b.linkStrain == 1f);
        check("waiting key cannot take input", !a.typeable());
        float left = a.linkLeft;
        c.destroyWord(a, 0, 0, L);
        check("duplicate clear does not refresh timer", a.linkLeft == left);
        c.paused = true; advance(c, L, 0.5f);
        check("pause freezes link countdown", a.linkLeft == left);
        c.paused = false;
        c.tapKey(b.word[0], L);
        check("second press immediately finishes both", a.destroyed && b.destroyed && a.link == null && b.link == null);
        check("pair awards two clears and combo exactly once", c.squishes == 2 && c.resolvedThisStage == 2 && c.combo == 2);
        int score = c.score;
        advance(c, L, 0.5f);
        c.destroyWord(a, 0, 0, L);
        check("old projectiles and repeated clears cannot score twice", c.score == score && c.resolvedThisStage == 2);

        for (float speed : new float[] {0.75f, 1f, 1.5f}) {
            for (float delay : new float[] {0f, 0.1f, 0.199f, 0.2f, 0.201f, 0.4f}) {
                for (int reverse = 0; reverse < 2; reverse++) {
                    c = wave(L, 16); c.speed = speed;
                    a = c.enemies.get(reverse); b = a.link;
                    c.tapKey(a.word[0], L);
                    c.update(delay, delay, L);
                    c.tapKey(b.word[0], L);
                    check("fixed 200ms input window at speed " + speed + " delay " + delay + " order " + reverse,
                            a.destroyed == (delay <= 0.2f) && b.destroyed == (delay <= 0.2f));
                    if (delay > 0.2f) {
                        check("late second key starts a fresh attempt", a.typeable() && b.linkWaiting && c.score == 0);
                        c.tapKey(a.word[0], L);
                        check("pair recovers in reverse order after timeout", a.destroyed && b.destroyed);
                    }
                }
            }
        }
        c = wave(L, 16); a = c.enemies.get(0); b = a.link;
        c.tapKey(a.word[0], L);
        c.slowdown = 1f;
        c.update(0.01f, 0.201f, L);
        check("200ms is elapsed time, unaffected by slow motion or frame clamp", a.typeable() && !a.linkWaiting);
        check("missed chord keeps a visible resistance reaction", a.linkStrain > 0f && b.linkStrain > 0f);
        advance(c, L, 0.5f);
        check("expired press projectile cannot complete or rearm the key", a.typeable() && c.resolvedThisStage == 0);
        c.tapKey(a.word[0], L);
        GameCore.Enemy distraction = add(c, L, new int[] {b.word[0], b.word[0]}, L.dangerY - L.enemyR * 4f);
        c.tapKey(b.word[0], L);
        check("pointed partner wins matching-target selection", a.destroyed && b.destroyed && distraction.pos == 0);

        c = wave(L, 16); a = c.enemies.get(0); b = a.link;
        c.tapKey(a.word[0], L);
        b.attacking = true; b.attackT = GameCore.ATTACK_TIME;
        c.update(DT, L);
        check("breach restores survivor without awarding an incomplete chord", a.typeable() && a.link == null && c.resolvedThisStage == 1 && c.score == 0);
        check("a partner breach costs only its own life", c.lives == GameCore.START_LIVES - 1);

        for (int mode = 0; mode < Power.COUNT; mode++) {
            c = wave(L, 16); a = c.enemies.get(0); b = a.link;
            c.tapKey(a.word[0], L);
            c.collected = Collect.MASK;
            c.startFrenzy(mode, L);
            check("power releases pair without reviving cleared half " + mode,
                    a.destroyed && a.link == null && b.link == null && b.typeable());
        }
        c = wave(L, 16);
        c.tapKey(c.enemies.get(0).word[0], L);
        c.jumpToStage(17, L);
        check("stage jump drops pending pair", c.enemies.isEmpty());
    }
}
