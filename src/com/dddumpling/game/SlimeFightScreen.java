package com.dddumpling.game;

/** The meadow arena, slime bodies, direct-control bar, and playful FUN report. */
final class SlimeFightScreen extends Draw {
    private static final int SKY = 0xFFDDF5CE, MEADOW = 0xFFAEDB83;
    private static final int LEAF = 0xFF589F66, LEAF_LIGHT = 0xFF82C977;
    private static final int PLAYER_GOO = 0xFF54D9A3, RIVAL_GOO = 0xFFA0D95F;

    private SlimeFightScreen() {}

    static void draw(Painter p, SlimeFight f, Layout L) {
        float w = L.w, top = L.playTop, bottom = L.deckTop, h = bottom - top, s = L.unit;
        p.fillRect(0, 0, w, L.h, SKY);
        p.fillRect(0, top + h * 0.20f, w, L.h, MEADOW);
        hills(p, L, top, h);
        backgroundPlants(p, L, top, h);

        for (int i = 0; i < SlimeFight.MAX_SPLATS; i++) {
            if (f.splatAge[i] <= 0f) continue;
            float fade = Math.min(1f, f.splatAge[i] * 2.4f);
            splat(p, f.splatX[i], f.splatY[i], SlimeFight.globR(L) * 1.25f,
                    f.splatPlayer[i] ? PLAYER_GOO : RIVAL_GOO, fade, i);
        }

        telegraph(p, f, L);
        for (int i = 0; i < SlimeFight.MAX_GLOBS; i++) {
            if (!f.globLive[i]) continue;
            glob(p, f.globX[i], f.globY[i], SlimeFight.globR(L),
                    f.globPlayer[i] ? PLAYER_GOO : RIVAL_GOO);
        }

        Slime.draw(p, f.rivalBody, f.clock + 1.7f, RIVAL_GOO, Kawaii.DUMPLING, 0.65f, 1f);
        Slime.draw(p, f.playerBody, f.clock, PLAYER_GOO, Kawaii.DUMPLING, 0.88f, 1f);

        if (f.flingGesture) {
            p.line(f.touchStartX, f.touchStartY, f.touchX, f.touchY,
                    Glyph.withAlpha(0xFFFFFFFF, 150), s * 0.20f);
            float r = s * (0.42f + 0.08f * (float) Math.sin(f.clock * 10f));
            p.fillPoly(star(f.touchX, f.touchY, r, r * 0.45f, 5, -f.clock),
                    Glyph.withAlpha(GOLD, 210));
        }

        foregroundPlants(p, L, top, h);
        hud(p, f, L);
        p.fillRect(0, L.deckTop, w, L.h, 0xFFD4EDB3);
        p.line(0, L.deckTop, w, L.deckTop, 0xFF70AF68, Math.max(2f, s * 0.12f));
        if (f.phase != SlimeFight.RESULT)
            StarScreen.slider(p, f.sliderKnob(L), L, f.phase == SlimeFight.PLAY ? 1f : 0.45f, f.clock);
        else result(p, f, L);
    }

    private static void hud(Painter p, SlimeFight f, Layout L) {
        float s = L.unit, y = L.playTop + s * 0.78f;
        p.text("FUN " + f.fun, L.playLeft + s * 0.2f, y, type(s * 0.72f), 0xFF315D45,
                Painter.LEFT, true);
        int seconds = (int) Math.ceil(f.timeLeft);
        p.text(f.phase == SlimeFight.READY ? "READY" : seconds + "s", L.playRight - s * 0.2f, y,
                type(s * 0.72f), 0xFF315D45, Painter.RIGHT, true);
        if (f.feedbackTime > 0f && f.phase != SlimeFight.RESULT) {
            float fade = Math.min(1f, f.feedbackTime * 3f);
            p.text(f.feedback, L.w * 0.5f, L.playTop + (L.deckTop - L.playTop) * 0.51f,
                    type(s * 0.65f), fadeBy(0xFF315D45, fade), Painter.CENTER, true);
        }
        if (f.phase == SlimeFight.READY)
            p.text("SLIDE TO BOUNCE", L.w * 0.5f, L.playTop + (L.deckTop - L.playTop) * 0.60f,
                    type(s * 0.58f), 0xFF315D45, Painter.CENTER, true);
    }

    private static void result(Painter p, SlimeFight f, Layout L) {
        float w = L.w, h = L.h, s = L.unit, cx = w * 0.5f;
        p.fillRect(0, 0, w, h, 0x76315D45);
        float top = L.playTop + s * 1.15f, bottom = L.deckTop + s * 1.9f;
        glassPanel(p, w * 0.09f, top, w * 0.91f, bottom, s);
        p.text(f.feedback, cx, top + s * 1.55f, type(s * 0.94f), GOLD, Painter.CENTER, true);
        p.text("FUN " + f.fun, cx, top + s * 3.0f, type(s * 1.15f), INK, Painter.CENTER, true);
        p.text(f.throwsMade + " TOSSES   " + f.hits + " SPLATS", cx, top + s * 4.05f,
                type(s * 0.54f), INK_DIM, Painter.CENTER, true);
        p.text(f.dodges + " DODGES   " + f.splashed + " GIGGLE SPLASHES", cx, top + s * 4.85f,
                type(s * 0.48f), INK_DIM, Painter.CENTER, true);
        p.text(f.achievementCount() + " PLAY BADGES", cx, top + s * 5.65f,
                type(s * 0.54f), GOLD, Painter.CENTER, true);
        button(p, cx, againY(L), w * 0.31f, s * 0.82f, "AGAIN", PLAYER_GOO, s);
        button(p, cx, exitY(L), w * 0.31f, s * 0.82f, "TOWN", 0xFF6E72C8, s);
    }

    private static void button(Painter p, float x, float y, float rx, float ry, String label,
            int color, float s) {
        p.fillPoly(pill(x, y, rx, ry, 16), color);
        p.strokePoly(pill(x, y, rx, ry, 16), 0xE6FFFFFF, s * 0.09f);
        p.text(label, x, y + s * 0.22f, type(s * 0.65f), 0xFFFFFFFF, Painter.CENTER, true);
    }

    static boolean inAgain(Layout L, float x, float y) {
        return inButton(L, x, y, againY(L));
    }

    static boolean inExit(Layout L, float x, float y) {
        return inButton(L, x, y, exitY(L));
    }

    private static boolean inButton(Layout L, float x, float y, float cy) {
        return Math.abs(x - L.w * 0.5f) <= L.w * 0.37f && Math.abs(y - cy) <= L.unit * 1.15f;
    }

    private static float againY(Layout L) { return L.deckTop - L.unit * 2.1f; }
    private static float exitY(Layout L) { return L.deckTop + L.unit * 0.25f; }

    private static void hills(Painter p, Layout L, float top, float h) {
        p.fillEllipse(L.w * 0.08f, top + h * 0.27f, L.w * 0.52f, h * 0.20f, 0xFFBFE7A1);
        p.fillEllipse(L.w * 0.68f, top + h * 0.29f, L.w * 0.60f, h * 0.23f, 0xFF9FD181);
        p.fillEllipse(L.w * 0.36f, top + h * 0.35f, L.w * 0.65f, h * 0.19f, MEADOW);
    }

    private static void backgroundPlants(Painter p, Layout L, float top, float h) {
        for (int i = 0; i < 7; i++) {
            float x = L.w * (0.05f + i * 0.155f), y = top + h * (0.30f + 0.035f * (i % 2));
            float r = L.w * (0.035f + 0.007f * (i % 3));
            p.line(x, y, x, y + r * 2.4f, 0xFF6A9254, r * 0.20f);
            p.fillPoly(pill(x, y, r, r * 0.78f, 8), i % 2 == 0 ? LEAF_LIGHT : LEAF);
        }
    }

    private static void foregroundPlants(Painter p, Layout L, float top, float h) {
        int dark = Glyph.withAlpha(0xFF316C4D, 92), light = Glyph.withAlpha(0xFF7BCB76, 100);
        float base = top + h;
        for (int side = -1; side <= 1; side += 2) {
            float x = side < 0 ? L.w * 0.015f : L.w * 0.985f;
            p.fillPoly(pill(x, base - h * 0.18f, L.w * 0.12f, h * 0.22f, 10), dark);
            p.fillPoly(pill(x - side * L.w * 0.045f, base - h * 0.08f,
                    L.w * 0.13f, h * 0.13f, 10), light);
        }
        for (int i = 0; i < 9; i++) {
            float x = L.w * (0.08f + 0.105f * i), y = base - L.w * (0.018f + 0.018f * (i % 3));
            int c = i % 3 == 0 ? 0x99FFF2A9 : i % 3 == 1 ? 0x99FFB8C8 : 0x99C8B8FF;
            p.fillCircle(x, y, L.w * 0.011f, c);
        }
    }

    private static void telegraph(Painter p, SlimeFight f, Layout L) {
        if (f.phase != SlimeFight.PLAY || f.rivalThrow > 0.42f) return;
        float t = 1f - f.rivalThrow / 0.42f, r = SlimeFight.globR(L) * (0.55f + t * 0.45f);
        float y = SlimeFight.rivalY(L) + SlimeFight.bodyR(L) * 0.95f;
        p.fillCircle(f.rivalX, y, r, Glyph.withAlpha(RIVAL_GOO, 105 + (int) (100 * t)));
        p.line(f.rivalX, y + r, f.rivalX, y + r + L.unit * (0.7f + t * 0.55f),
                Glyph.withAlpha(0xFF315D45, 100), L.unit * 0.08f);
    }

    private static void glob(Painter p, float x, float y, float r, int color) {
        p.fillCircle(x, y, r * 1.10f, Glyph.withAlpha(color, 105));
        p.fillCircle(x, y, r, Glyph.withAlpha(color, 225));
        p.strokeCircle(x, y, r, Glyph.mix(color, 0xFFFFFFFF, 0.55f), r * 0.13f);
        p.fillCircle(x - r * 0.29f, y - r * 0.33f, r * 0.20f, 0xBFFFFFFF);
    }

    private static void splat(Painter p, float x, float y, float r, int color, float fade, int seed) {
        p.fillPoly(star(x, y, r * 1.55f, r * (0.48f + hash(seed) * 0.18f), 7,
                hash(seed * 31 + 4) * Softbody.TAU), fadeBy(Glyph.withAlpha(color, 145), fade));
        p.fillCircle(x, y, r * 0.72f, fadeBy(Glyph.withAlpha(color, 190), fade));
    }
}
