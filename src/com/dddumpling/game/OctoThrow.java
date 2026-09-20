package com.dddumpling.game;

import java.util.Random;

/** Arm-tear retaliation: wind up, release from the hand, then follow through. */
final class OctoThrow {
    static final float WIND = .50f, RELEASE = .68f, END = .94f;

    static void reset(Boss b) {
        b.octoThrowsLeft = 0;
        b.octoThrowArm = b.octoThrowGlyph = -1;
        b.octoThrowArm2 = b.octoThrowGlyph2 = -1;
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
        float rate = Integer.bitCount(b.octoArms) == 1 && b.octoThrowT >= 0f ? 2f : 1f;
        b.octoThrowT += dt * rate;
        if (b.octoThrowArm >= 0 && b.octoThrowT >= END && b.octoThrowReleased) {
            b.octoThrowArm = b.octoThrowGlyph = -1;
            b.octoThrowArm2 = b.octoThrowGlyph2 = -1;
            b.octoThrowT = 0f;
        }
        if (b.octoThrowArm < 0 && b.octoThrowsLeft > 0 && b.octoThrowT >= 0f) {
            int pick = rnd.nextInt(Integer.bitCount(b.octoArms));
            for (int a = 0; a < Boss.OCTO_ARMS; a++)
                if ((b.octoArms & (1 << a)) != 0 && pick-- == 0) { b.octoThrowArm = a; break; }
            b.octoThrowGlyph = glyph(b, rnd, -1);
            if (b.octoThrowsLeft == 2 && Integer.bitCount(b.octoArms) > 1) {
                for (int step = 1; step < Boss.OCTO_ARMS; step++) {
                    int arm = (b.octoThrowArm + step) % Boss.OCTO_ARMS;
                    if ((b.octoArms & (1 << arm)) != 0) { b.octoThrowArm2 = arm; break; }
                }
                b.octoThrowGlyph2 = glyph(b, rnd, b.octoThrowGlyph);
            }
            b.octoThrowReleased = false;
            b.octoThrowT = 0f;
        }
    }

    static boolean usesArm(Boss b, int arm) {
        return arm == b.octoThrowArm || arm == b.octoThrowArm2;
    }

    private static int glyph(Boss b, Random rnd, int avoid) {
        int first = rnd.nextInt(Glyph.COUNT), fallback = -1;
        for (int i = 0; i < Glyph.COUNT; i++) {
            int g = (first + i) % Glyph.COUNT;
            if (Roster.active(b.rosterFull, g) && !b.keyDisabled(g)) {
                fallback = g;
                if (g != avoid && !b.boltWants(g)) return g;
            }
        }
        return fallback;
    }

    static float blend(Boss b) {
        return Math.min(1f, b.octoThrowT / .15f)
                * (1f - smooth((b.octoThrowT - RELEASE) / (END - RELEASE)));
    }

    static float handX(Boss b, Layout L, int arm) {
        float side = b.octoThrowArm < 4 ? -1f : 1f;
        if (arm == b.octoThrowArm2) side = -side;
        float wind = smooth(b.octoThrowT / WIND);
        float swing = smooth((b.octoThrowT - WIND) / (RELEASE - WIND));
        float back = b.body.centreX() + side * Boss.bodyR(L) * (1.8f + .5f * wind);
        float front = Roster.keyX(L, arm == b.octoThrowArm2 ? b.octoThrowGlyph2 : b.octoThrowGlyph, b.rosterFull ? 1f : 0f);
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
        int count = b.octoThrowArm2 >= 0 ? 2 : 1;
        if (Boss.MAX_BOLTS - b.boltCount() < count) return;
        launch(b, b.octoThrowArm, b.octoThrowGlyph);
        if (count == 2) launch(b, b.octoThrowArm2, b.octoThrowGlyph2);
        b.launched = true;
        b.launchT = Boss.LAUNCH_TIME;
        b.octoThrowReleased = true;
        b.octoThrowsLeft -= count;
    }

    private static void launch(Boss b, int arm, int glyph) {
        for (int i = 0; i < Boss.MAX_BOLTS; i++) if (!b.blive[i]) {
            int tip = Boss.OCTO_NODES - 1;
            b.blive[i] = true;
            b.bfast[i] = true;
            b.bglyph[i] = glyph;
            b.bhp[i] = b.bhpMax[i] = 1;
            b.bt[i] = 0f;
            b.bsx[i] = b.octoX[arm][tip];
            b.bsy[i] = b.octoY[arm][tip];
            return;
        }
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
