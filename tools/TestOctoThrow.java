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
            int launches = 0, events = 0, hits = 0, cleared = 0, lastLaunch = -1000;
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
                    events++;
                    if (lastLaunch >= 0 && removed == 7)
                        spaced &= Math.abs((frame - lastLaunch) * DT - OctoThrow.END * .5f) < DT * 2f;
                    else spaced &= frame - lastLaunch >= 45;
                    lastLaunch = frame;
                    int tip = Boss.OCTO_NODES - 1;
                    releaseY = b.octoY[b.octoThrowArm][tip];
                    boolean found = false;
                    for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i] && b.bt[i] == 0f) {
                        launches++;
                        int arm = b.bglyph[i] == b.octoThrowGlyph2 ? b.octoThrowArm2 : b.octoThrowArm;
                        found |= Math.abs(b.bsx[i] - b.octoX[arm][tip]) < .01f
                                && Math.abs(b.bsy[i] - b.octoY[arm][tip]) < .01f;
                        validArm &= (b.octoArms & (1 << arm)) != 0;
                        validArm &= !b.keyDisabled(b.bglyph[i]);
                    }
                    tipOrigin &= found;
                    if (removed == 5 || removed == 6) {
                        check("pair launches on the same frame", launches == 2 && b.octoThrowArm2 >= 0);
                        check("pair uses different arms and keys", b.octoThrowArm != b.octoThrowArm2
                                && b.octoThrowGlyph != b.octoThrowGlyph2);
                    }
                }
                if (parry != 0) for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i]) {
                    cleared += b.press(b.bglyph[i], c.rnd, L) == Boss.PARRY ? 1 : 0;
                }
                if (!OctoThrow.busy(b) && b.boltCount() == 0 && b.octoTarget >= 0) break;
            }
            check("tear " + removed + " throws " + expected + " enemies, parry=" + parry, launches == expected);
            check("volley uses the requested number of release events", events == (expected == 2 ? 1 : expected));
            check("throw uses intact arm and enabled key", validArm);
            check("next attack waits for all throws to resolve", waiting);
            check("each throw has a wind-up and distinct release", spaced && wound && tipOrigin);
            check("thrown enemies resolve normally", parry == 0 ? hits == expected : cleared == expected && hits == 0);
            if (expected > 0) {
                check("wind-up lifts the hand before forward throw", releaseY - windY > Boss.bodyR(L));
                check("next arm wave resumes after the field clears", b.octoTarget >= 0);
            } else check("last tear defeats without retaliation", b.beaten && !OctoThrow.busy(b));
        }
        GameCore speed = TestBoss.enterBoss(L, Boss.OCTOPUS, 888L);
        Boss fast = speed.boss;
        fast.octoPause = 10f;
        fast.blive[0] = true; fast.bfast[0] = true; fast.bt[0] = 0f;
        for (int i = 0; i < 36; i++) fast.update(DT, L, speed.rnd);
        check("Octopulse projectile is halfway there after 0.6 seconds", Math.abs(fast.bt[0] - .5f) < .001f);
        int arrivals = 0;
        for (int i = 0; i < 37; i++) arrivals += fast.update(DT, L, speed.rnd);
        check("Octopulse projectile lands after 1.2 seconds", arrivals == 1 && !fast.blive[0]);
        GameCore missed = TestBoss.enterBoss(L, Boss.OCTOPUS, 889L);
        Boss slow = missed.boss;
        // Reuse a fast projectile slot for the missed-tear punishment.
        slow.bfast[0] = true;
        slow.octoVulnerableArm = 3;
        slow.octoDragTime = 1.99f;
        slow.update(DT, L, missed.rnd);
        check("missed tear launches at the original speed even in a reused slot", slow.blive[0] && !slow.bfast[0]);
        for (int i = 0; i < 72; i++) slow.update(DT, L, missed.rnd);
        check("missed-tear projectile is only halfway there after 1.2 seconds", slow.blive[0]
                && Math.abs(slow.bt[0] - .5f) < .001f);
        GameCore reset = tear(L, 7);
        reset.boss.leave();
        check("leaving clears queued throws", !OctoThrow.busy(reset.boss));
    }
}
