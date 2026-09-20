package com.dddumpling.game;

import java.util.Random;

/** Arm-tear retaliation: wind up, release from the hand, then follow through. */
final class OctoThrow {
    static final float WIND = .50f, RELEASE = .68f, END = .94f;

    static void reset(Boss b) {
        b.octoThrowsLeft = 0;
        b.octoThrowArm = b.octoThrowGlyph = -1;
        b.octoThrowT = 0f;
        b.octoThrowReleased = false;
    }

    static void torn(Boss b) {
        reset(b);
        int left = Integer.bitCount(b.octoArms);
        b.octoThrowsLeft = left == 0 ? 0 : left == 1 ? 3 : left <= 3 ? 2 : 1;
        b.octoThrowT = -.35f; // Let the torn arm snap back before the wind-up.
    }

    static boolean busy(Boss b) { return b.octoThrowsLeft > 0 || b.octoThrowArm >= 0; }

    static void update(Boss b, float dt, Random rnd) {
        if (!busy(b)) return;
        if (b.beaten || b.octoArms == 0) { reset(b); return; }
        b.octoThrowT += dt;
        if (b.octoThrowArm >= 0 && b.octoThrowT >= END && b.octoThrowReleased) {
            b.octoThrowArm = b.octoThrowGlyph = -1;
            b.octoThrowT = 0f;
        }
        if (b.octoThrowArm < 0 && b.octoThrowsLeft > 0 && b.octoThrowT >= 0f) {
            int pick = rnd.nextInt(Integer.bitCount(b.octoArms));
            for (int a = 0; a < Boss.OCTO_ARMS; a++)
                if ((b.octoArms & (1 << a)) != 0 && pick-- == 0) { b.octoThrowArm = a; break; }
            int first = rnd.nextInt(Glyph.COUNT);
            for (int i = 0; i < Glyph.COUNT; i++) {
                int g = (first + i) % Glyph.COUNT;
                if (Roster.active(b.rosterFull, g) && !b.keyDisabled(g)) {
                    b.octoThrowGlyph = g;
                    if (!b.boltWants(g)) break;
                }
            }
            b.octoThrowReleased = false;
            b.octoThrowT = 0f;
        }
    }

    static float blend(Boss b) {
        return Math.min(1f, b.octoThrowT / .15f)
                * (1f - smooth((b.octoThrowT - RELEASE) / (END - RELEASE)));
    }

    static float handX(Boss b, Layout L) {
        float side = b.octoThrowArm < 4 ? -1f : 1f;
        float wind = smooth(b.octoThrowT / WIND);
        float swing = smooth((b.octoThrowT - WIND) / (RELEASE - WIND));
        float back = b.body.centreX() + side * Boss.bodyR(L) * (1.8f + .5f * wind);
        float front = Roster.keyX(L, b.octoThrowGlyph, b.rosterFull ? 1f : 0f);
        front = b.body.centreX() + (front - b.body.centreX()) * .65f;
        return Math.max(L.playLeft + L.keyR, Math.min(L.playRight - L.keyR,
                back + (front - back) * swing));
    }

    static float handY(Boss b, Layout L) {
        float wind = smooth(b.octoThrowT / WIND);
        float swing = smooth((b.octoThrowT - WIND) / (RELEASE - WIND));
        float back = b.body.centreY() + Boss.bodyR(L) * (.8f - 2.1f * wind);
        float front = Math.min(L.deckTop - L.keyR * 2f, b.body.centreY() + Boss.bodyR(L) * 2f);
        return Math.max(L.playTop + L.keyR, back + (front - back) * swing);
    }

    static void release(Boss b) {
        if (b.octoThrowArm < 0 || b.octoThrowReleased || b.octoThrowT < RELEASE) return;
        for (int i = 0; i < Boss.MAX_BOLTS; i++) if (!b.blive[i]) {
            int tip = Boss.OCTO_NODES - 1;
            b.blive[i] = true;
            b.bglyph[i] = b.octoThrowGlyph;
            b.bhp[i] = b.bhpMax[i] = 1;
            b.bt[i] = 0f;
            b.bsx[i] = b.octoX[b.octoThrowArm][tip];
            b.bsy[i] = b.octoY[b.octoThrowArm][tip];
            b.launched = true;
            b.launchT = Boss.LAUNCH_TIME;
            b.octoThrowReleased = true;
            b.octoThrowsLeft--;
            return;
        }
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
