package com.dddumpling.game;

final class TestOctoThrow extends Check {
    static GameCore tear(Layout L, int removed) {
        GameCore c = TestBoss.enterBoss(L, Boss.OCTOPUS, 810L + removed);
        Boss b = c.boss;
        b.octoArms = (1 << (9 - removed)) - 1;
        b.hp = 9 - removed;
        b.octoPause = 10f;
        b.octoVulnerableArm = 8 - removed;
        b.held = -3;
        b.octoDragStarted = b.octoDragCanDamage = true;
        b.dragTo(L.playLeft - L.keyR, L.playTop, L);
        return c;
    }

    static void run(Layout L) {
        group("octopulse arm throws");
        for (int removed = 1; removed <= 8; removed++) for (int parry = 0; parry < 2; parry++) {
            GameCore c = tear(L, removed);
            Boss b = c.boss;
            int expected = removed == 8 ? 0 : removed == 7 ? 3 : removed >= 5 ? 2 : 1;
            int launches = 0, hits = 0, cleared = 0, lastLaunch = -1000;
            boolean validArm = true, waiting = true, spaced = true, tipOrigin = true, wound = true;
            float windY = Float.NaN, releaseY = Float.NaN;
            for (int frame = 0; frame < 480; frame++) {
                boolean outstanding = OctoThrow.busy(b) || b.boltCount() > 0;
                hits += b.update(DT, L, c.rnd);
                if (outstanding && (OctoThrow.busy(b) || b.boltCount() > 0))
                    waiting &= b.octoTarget < 0 && !b.octoWave;
                if (b.octoThrowArm >= 0) {
                    validArm &= (b.octoArms & (1 << b.octoThrowArm)) != 0;
                    if (b.octoThrowT > .45f && b.octoThrowT < .50f)
                        windY = b.octoY[b.octoThrowArm][Boss.OCTO_NODES - 1];
                    if (b.octoThrowT < OctoThrow.RELEASE) wound &= !b.launched;
                }
                if (b.launched) {
                    launches++;
                    spaced &= frame - lastLaunch >= 45;
                    lastLaunch = frame;
                    int tip = Boss.OCTO_NODES - 1;
                    releaseY = b.octoY[b.octoThrowArm][tip];
                    boolean found = false;
                    for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i] && b.bt[i] == 0f) {
                        found |= Math.abs(b.bsx[i] - b.octoX[b.octoThrowArm][tip]) < .01f
                                && Math.abs(b.bsy[i] - releaseY) < .01f;
                        validArm &= !b.keyDisabled(b.bglyph[i]);
                    }
                    tipOrigin &= found;
                }
                if (parry != 0) for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i]) {
                    cleared += b.press(b.bglyph[i], c.rnd, L) == Boss.PARRY ? 1 : 0;
                }
                if (!OctoThrow.busy(b) && b.boltCount() == 0 && b.octoTarget >= 0) break;
            }
            check("tear " + removed + " throws " + expected + " enemies, parry=" + parry, launches == expected);
            check("throw uses intact arm and enabled key", validArm);
            check("next attack waits for all throws to resolve", waiting);
            check("each throw has a wind-up and distinct release", spaced && wound && tipOrigin);
            check("thrown enemies resolve normally", parry == 0 ? hits == expected : cleared == expected && hits == 0);
            if (expected > 0) {
                check("wind-up lifts the hand before forward throw", releaseY - windY > Boss.bodyR(L));
                check("next arm wave resumes after the field clears", b.octoTarget >= 0);
            } else check("last tear defeats without retaliation", b.beaten && !OctoThrow.busy(b));
        }
        GameCore reset = tear(L, 7);
        reset.boss.leave();
        check("leaving clears queued throws", !OctoThrow.busy(reset.boss));
    }
}
