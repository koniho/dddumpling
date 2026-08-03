package com.sram.hexatype;

/**
 * Draws the whole game against a {@link Painter}. Pure Java and stateless, so the device
 * and the preview harness produce the same picture from the same state.
 */
final class Renderer {

    static final int BG = 0xFF1B1730;
    static final int BG_HI = 0xFF251F42;
    static final int BG_HURT = 0xFF4A0F22;
    static final int INK = 0xFFF6F1FF;
    static final int INK_DIM = 0xFFA79DCC;
    static final int ROSE = 0xFFFF7C9E;

    private Renderer() {}

    static void draw(Painter p, GameCore c, Layout L) {
        float harm = c.harm();
        // Low health drags the whole palette toward red, and the pulse quickens with it.
        float hurtPulse = 0.5f + 0.5f * (float) Math.sin(c.clock * (2.6f + 5.5f * harm));
        float hurt = harm * (0.55f + 0.45f * hurtPulse);

        // Kept moderate: the red reads as a pulse at the edges, not a wash over the
        // characters, which have to stay legible at exactly the moment you are panicking.
        p.fillRect(0, 0, L.w, L.h, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
        p.fillRect(0, L.keyTop - 0.02f * L.h, L.w, L.h, Glyph.mix(BG_HI, BG_HURT, hurt * 0.35f));
        stars(p, c, L);

        p.save();
        if (c.shake > 0) {
            float m = c.shake * 0.016f * L.w;
            p.translate(m * (float) Math.sin(c.clock * 57f),
                    m * 0.6f * (float) Math.cos(c.clock * 71f));
        }

        dangerLine(p, c, L);
        for (int i = 0; i < c.enemies.size(); i++) enemy(p, c, L, c.enemies.get(i));
        shots(p, c, L);
        particles(p, c);
        keys(p, c, L);
        p.restore();

        // Red closing-in glow: from low health, and from a word about to land.
        vignette(p, L, ROSE, Math.max(hurt, c.warnLevel * (0.45f + 0.55f * hurtPulse)));

        // The title and game-over screens carry their own numbers; a second copy is clutter.
        if (c.state == GameCore.PLAY) {
            hudBacking(p, L, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
            hud(p, c, L);
        }

        if (c.flash > 0) p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(ROSE, (int) (c.flash * 52)));

        if (c.state == GameCore.TITLE) title(p, c, L);
        else if (c.state == GameCore.OVER) gameOver(p, c, L);
        else if (c.stageBanner > 0) stageBanner(p, c, L);
    }

    // ---- background ---------------------------------------------------------

    private static void stars(Painter p, GameCore c, Layout L) {
        for (int i = 0; i < c.starX.length; i++) {
            float y = c.starY[i] * L.keyTop;
            float tw = 0.55f + 0.45f * (float) Math.sin(c.clock * 1.4f + i);
            int a = (int) (26 + 46 * c.starS[i] * tw);
            p.fillCircle(c.starX[i] * L.w, y, c.starS[i] * 0.004f * L.w, Glyph.withAlpha(INK, a));
        }
    }

    /**
     * Edge glow from overlapping strips rather than discrete rings: each layer reaches
     * from an edge inward by a shrinking amount, all at the same low alpha, so the
     * build-up is a smooth ramp instead of visible bands. Corners get both a horizontal
     * and a vertical layer, which is what a vignette wants anyway.
     */
    private static void vignette(Painter p, Layout L, int color, float strength) {
        if (strength <= 0.01f) return;
        // Many thin layers, not few thick ones: the innermost layer's own edge is the only
        // hard boundary, so its alpha has to be small enough to be invisible.
        int layers = 32;
        float depthY = 0.16f * L.h, depthX = 0.16f * L.w;
        int col = Glyph.withAlpha(color, Math.max(1, (int) (strength * 3.5f)));
        for (int i = 0; i < layers; i++) {
            float k = 1f - (float) i / layers;
            p.fillRect(0, 0, L.w, depthY * k, col);
            p.fillRect(0, L.h - depthY * k, L.w, L.h, col);
            p.fillRect(0, 0, depthX * k, L.h, col);
            p.fillRect(L.w - depthX * k, 0, L.w, L.h, col);
        }
    }

    /**
     * Soft band over the strip above the play area. Words now spawn off-screen and slide
     * down through it, so without this they would track across the score and stage
     * readouts; with it they read as emerging from behind the HUD.
     */
    private static void hudBacking(Painter p, Layout L, int bg) {
        int layers = 14;
        int col = Glyph.withAlpha(bg, 26);
        for (int i = 0; i < layers; i++) {
            p.fillRect(0, 0, L.w, L.playTop * (1f - (float) i / layers), col);
        }
    }

    private static void dangerLine(Painter p, GameCore c, Layout L) {
        float alarm = c.warnLevel;
        int bands = 5;
        for (int i = 0; i < bands; i++) {
            float t0 = L.dangerY + i * 0.010f * L.h;
            int a = (int) ((16 + 54 * alarm) * (1f - (float) i / bands));
            p.fillRect(0, t0, L.w, t0 + 0.010f * L.h, Glyph.withAlpha(ROSE, a));
        }
        float pulse = 0.65f + 0.35f * (float) Math.sin(c.clock * (2.2f + 6f * alarm));
        int col = Glyph.withAlpha(ROSE, (int) ((110 + 145 * alarm) * pulse));
        float dash = 0.030f * L.w, gap = 0.022f * L.w;
        for (float x = L.playLeft; x < L.playRight; x += dash + gap) {
            float x2 = Math.min(x + dash, L.playRight);
            p.line(x, L.dangerY, x2, L.dangerY, col, Math.max(1.5f, 0.004f * L.w) * (1 + alarm));
        }
    }

    // ---- enemies ------------------------------------------------------------

    private static void enemy(Painter p, GameCore c, Layout L, GameCore.Enemy e) {
        if (e.dying) {
            float t = Math.min(1f, e.deathT / 0.13f);
            float r = L.enemyR * (1.1f + t * 1.6f);
            p.strokePoly(Glyph.hex(c.enemyCentreX(e), e.y, r),
                    Glyph.withAlpha(INK, (int) (235 * (1f - t))), L.enemyR * 0.16f);
            return;
        }

        float attack = e.attacking ? Math.min(1f, e.attackT / GameCore.ATTACK_TIME) : 0f;

        // Agitation as it closes on the line, becoming a full-body lunge on attack.
        float jx = 0, jy = 0;
        float agitate = Math.max(Math.max(e.warn, attack), e.failPulse * 0.85f);
        if (agitate > 0) {
            float m = agitate * L.enemyR * (0.14f + 0.30f * attack);
            jx = m * (float) Math.sin(c.clock * 47f + e.phase);
            jy = m * 0.55f * (float) Math.cos(c.clock * 61f + e.phase);
        }

        if (attack > 0) {
            // Radiating rings telegraph the strike.
            for (int k = 0; k < 3; k++) {
                float rr = L.enemyR * (1.4f + attack * (2.2f + k * 0.9f));
                p.strokePoly(Glyph.hex(c.enemyCentreX(e) + jx, e.y + jy, rr),
                        Glyph.withAlpha(ROSE, (int) (215 * (1f - attack) / (k + 1))),
                        L.enemyR * 0.09f);
            }
        }

        boolean locked = c.target == e;
        // Entrance: eases in as the word clears the top edge.
        float enter = 0.62f + 0.38f * e.enterT;
        float swell = (1f + 0.26f * attack + 0.08f * e.warn) * enter;
        int fade = (int) (90 + 165 * e.enterT);

        for (int i = 0; i < e.word.length; i++) {
            int g = e.word[i];
            boolean head = i == e.pos;
            boolean cleared = i < e.pos;
            float x = c.tileX(e, i, L) + jx;
            float y = e.y + jy;

            float wobble = c.clock * 3.1f + e.phase + i * 0.7f;
            float scale = head ? Layout.HEAD_SCALE : cleared ? Layout.TILE_SCALE * 0.84f
                    : Layout.TILE_SCALE;
            float cellR = L.enemyR * scale * swell;

            int col = Glyph.COLOR[g];
            // Only the tile actually struck takes the full colour strobe and pop.
            float pop = (e.hitIndex == i) ? e.hitPulse : 0f;
            if (pop > 0) {
                col = Glyph.mix(col, Glyph.cycle(c.clock * 7f + i * 0.17f), pop * 0.62f);
            }
            if (e.failPulse > 0) col = Glyph.mix(col, ROSE, e.failPulse * 0.75f);
            if (attack > 0) col = Glyph.mix(col, ROSE, attack * 0.35f);

            // Cleared letters stay put — the word only leaves once it is fully typed — but
            // recede so the remaining letters are what the eye lands on.
            int fillA = cleared ? 26 : head ? 52 : 30;
            int edgeA = cleared ? 58 : head ? 165 : 88;
            p.fillPoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, fillA * fade / 255));
            p.strokePoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, edgeA * fade / 255),
                    cellR * 0.075f);

            float charR = cellR * 0.60f * (1f + 0.045f * (float) Math.sin(wobble))
                    * (1f + 0.34f * pop);
            float squash = 1f + 0.16f * pop - 0.05f * (float) Math.sin(wobble);
            int charCol = cleared ? Glyph.withAlpha(Glyph.mix(col, INK_DIM, 0.42f), 180) : col;
            Kawaii.draw(p, g, x, y, charR, charCol, squash,
                    cleared ? 1f : head ? 0.4f : 0.1f);

            if (head && locked) {
                // Thicker white outline plus a caret: unmistakable without growing the cell.
                float pulse = 0.6f + 0.4f * (float) Math.sin(c.clock * 7f);
                p.strokePoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(INK, (int) (215 * pulse)),
                        cellR * 0.13f);
                float cy = y - cellR * 1.55f, cw = cellR * 0.40f;
                p.fillPoly(new float[] {x - cw, cy - cw, x + cw, cy - cw, x, cy + cw * 0.75f},
                        Glyph.withAlpha(INK, 225));
            }
        }
    }

    private static void shots(Painter p, GameCore c, Layout L) {
        for (int i = 0; i < c.shots.size(); i++) {
            GameCore.Shot s = c.shots.get(i);
            float t = Math.min(1f, s.t);
            float x = s.sx + (s.tx - s.sx) * t;
            float y = s.sy + (s.ty - s.sy) * t;
            float t0 = Math.max(0f, t - 0.30f);
            float x0 = s.sx + (s.tx - s.sx) * t0;
            float y0 = s.sy + (s.ty - s.sy) * t0;
            int col = Glyph.cycle(c.clock * 8f + s.glyph * 0.15f);
            p.line(x0, y0, x, y, Glyph.withAlpha(col, 130), L.enemyR * 0.26f);
            p.fillCircle(x, y, L.enemyR * 0.46f, Glyph.withAlpha(col, 80));
            p.fillCircle(x, y, L.enemyR * 0.21f, Glyph.withAlpha(INK, 245));
        }
    }

    private static void particles(Painter p, GameCore c) {
        for (int i = 0; i < c.particles.size(); i++) {
            GameCore.Particle q = c.particles.get(i);
            float k = q.life / q.max;
            p.fillCircle(q.x, q.y, q.size * (0.4f + k * 0.6f),
                    Glyph.withAlpha(q.color, (int) (235 * k)));
        }
    }

    // ---- key deck -----------------------------------------------------------

    private static void keys(Painter p, GameCore c, Layout L) {
        int hint = c.hintGlyph();
        for (int g = 0; g < Glyph.COUNT; g++) {
            float press = c.keyPress[g], bad = c.keyBad[g];
            float r = L.keyR * (1f - 0.05f * press);
            float cx = L.keyX[g], cy = L.keyY[g];

            // Activated keys strobe through the palette rather than merely brightening.
            int col = Glyph.COLOR[g];
            if (press > 0.02f) {
                col = Glyph.mix(col, Glyph.cycle(c.clock * 9f + g * 0.13f), press * 0.9f);
            }
            if (bad > 0) col = Glyph.mix(col, ROSE, bad);

            if (hint == g) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 6f);
                p.strokePoly(Glyph.hex(cx, cy, r * 1.12f),
                        Glyph.withAlpha(col, (int) (60 + 145 * pulse)), r * 0.06f);
            }
            if (press > 0.02f) {
                // Ripple outward as the press decays.
                p.strokePoly(Glyph.hex(cx, cy, r * (1.05f + 0.42f * (1f - press))),
                        Glyph.withAlpha(col, (int) (210 * press)), r * 0.07f);
            }

            p.fillPoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, (int) (36 + 150 * press)));
            p.strokePoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, (int) (190 + 65 * press)),
                    r * 0.085f);

            Kawaii.draw(p, g, cx, cy, r * 0.60f * (1f + 0.12f * press), col,
                    1f + 0.20f * press, 0.25f + 0.6f * press);
        }
    }

    // ---- HUD ----------------------------------------------------------------

    private static void hud(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        p.text("SCORE", L.playLeft, L.hudY - s * 0.95f, s * 0.52f, INK_DIM, Painter.LEFT, false);
        p.text(String.valueOf(c.score), L.playLeft, L.hudY, s * 1.05f, INK, Painter.LEFT, true);

        p.text("STAGE " + c.stage, L.w / 2f, L.hudY - s * 0.95f, s * 0.58f, INK_DIM,
                Painter.CENTER, true);
        int done = c.killsIntoStage();
        float pr = s * 0.13f, gap = s * 0.46f;
        float x0 = L.w / 2f - gap * 3.5f;
        for (int i = 0; i < 8; i++) {
            p.fillCircle(x0 + i * gap, L.hudY - s * 0.30f, pr,
                    i < done ? INK : Glyph.withAlpha(INK, 55));
        }

        float lr = s * 0.44f;
        float step = lr * 2.25f;
        for (int i = 0; i < GameCore.START_LIVES; i++) {
            float cx = L.playRight - lr - (GameCore.START_LIVES - 1 - i) * step;
            boolean alive = i < c.lives;
            // The last life throbs, so you feel it without reading the HUD.
            float rr = alive && c.lives == 1
                    ? lr * (1f + 0.18f * (0.5f + 0.5f * (float) Math.sin(c.clock * 7f)))
                    : lr;
            p.fillPoly(Glyph.hex(cx, L.hudY - s * 0.34f, rr),
                    alive ? Glyph.withAlpha(ROSE, 220) : Glyph.withAlpha(INK, 38));
            if (!alive) {
                p.strokePoly(Glyph.hex(cx, L.hudY - s * 0.34f, lr), Glyph.withAlpha(INK, 70),
                        lr * 0.11f);
            }
        }
    }

    // ---- screens ------------------------------------------------------------

    /** Dims everything above the key deck, so the real keys stay lit as the tutorial. */
    private static float scrim(Painter p, Layout L, int a) {
        float bottom = L.keyTop - 0.02f * L.h;
        p.fillRect(0, 0, L.w, bottom, Glyph.withAlpha(0xFF120E22, a));
        return bottom;
    }

    private static void handLabels(Painter p, Layout L, float baseline) {
        p.text("LEFT HAND", L.keyX[1], baseline, L.unit * 0.5f, INK_DIM, Painter.CENTER, true);
        p.text("RIGHT HAND", L.keyX[4], baseline, L.unit * 0.5f, INK_DIM, Painter.CENTER, true);
    }

    private static void title(Painter p, GameCore c, Layout L) {
        float bottom = scrim(p, L, 210);
        float s = L.unit;
        p.text("HEXATYPE", L.w / 2f, L.h * 0.22f, s * 2.15f, INK, Painter.CENTER, true);
        p.text("SIX LETTERS. THREE PER THUMB.", L.w / 2f, L.h * 0.22f + s * 1.5f, s * 0.62f,
                INK_DIM, Painter.CENTER, false);

        p.text("CUTE WORDS FALL FROM THE SKY.", L.w / 2f, L.h * 0.40f, s * 0.66f, INK,
                Painter.CENTER, false);
        p.text("TAP THE MATCHING HEX IN ORDER,", L.w / 2f, L.h * 0.40f + s * 1.0f, s * 0.66f,
                INK, Painter.CENTER, false);
        p.text("LEFT TO RIGHT, BEFORE THEY LAND.", L.w / 2f, L.h * 0.40f + s * 2.0f, s * 0.66f,
                INK, Painter.CENTER, false);

        if (c.best > 0) {
            p.text("BEST " + c.best, L.w / 2f, L.h * 0.55f, s * 0.78f, ROSE, Painter.CENTER, true);
        }

        float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 3.2f);
        p.text("TAP TO START", L.w / 2f, L.h * 0.65f, s * 0.95f,
                Glyph.withAlpha(INK, (int) (255 * pulse)), Painter.CENTER, true);

        handLabels(p, L, bottom - s * 0.45f);
    }

    private static void gameOver(Painter p, GameCore c, Layout L) {
        float bottom = scrim(p, L, 220);
        float s = L.unit;
        p.text("GAME OVER", L.w / 2f, L.h * 0.24f, s * 1.85f, ROSE, Painter.CENTER, true);

        p.text("SCORE", L.w / 2f, L.h * 0.36f, s * 0.6f, INK_DIM, Painter.CENTER, false);
        p.text(String.valueOf(c.score), L.w / 2f, L.h * 0.36f + s * 1.9f, s * 1.9f, INK,
                Painter.CENTER, true);

        p.text("STAGE " + c.stage + "   KILLS " + c.kills, L.w / 2f, L.h * 0.49f, s * 0.6f,
                INK_DIM, Painter.CENTER, false);
        p.text("BEST COMBO " + c.maxCombo, L.w / 2f, L.h * 0.49f + s * 0.9f, s * 0.6f, INK_DIM,
                Painter.CENTER, false);
        p.text(c.score >= c.best ? "NEW BEST!" : "BEST " + c.best, L.w / 2f, L.h * 0.58f,
                s * 0.78f, c.score >= c.best ? Glyph.COLOR[0] : INK_DIM, Painter.CENTER, true);

        if (c.time > 0.6f) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 3.2f);
            p.text("TAP TO RESTART", L.w / 2f, L.h * 0.67f, s * 0.95f,
                    Glyph.withAlpha(INK, (int) (255 * pulse)), Painter.CENTER, true);
        }

        handLabels(p, L, bottom - s * 0.45f);
    }

    private static void stageBanner(Painter p, GameCore c, Layout L) {
        float k = Math.min(1f, c.stageBanner / 0.4f);
        int a = (int) (235 * k);
        p.text("STAGE " + c.stage, L.w / 2f, L.h * 0.38f, L.unit * 1.7f,
                Glyph.withAlpha(INK, a), Painter.CENTER, true);
        p.text("FASTER NOW", L.w / 2f, L.h * 0.38f + L.unit * 1.15f, L.unit * 0.6f,
                Glyph.withAlpha(ROSE, a), Painter.CENTER, false);
    }
}
