package com.sram.hexatype;

/**
 * Frame orchestration and the play field itself: the danger line, the words, shots,
 * particles and the key deck.
 *
 * Everything behind or over the field is in {@link Sky}, the readouts are in {@link Hud},
 * and the full-screen states are in {@link Screens}.
 */
final class Renderer extends Draw {

    private Renderer() {}

    static void draw(Painter p, GameCore c, Layout L) {
        float harm = c.harm();
        // Low health drags the whole palette toward red, and the pulse quickens with it.
        float hurtPulse = 0.5f + 0.5f * (float) Math.sin(c.clock * (2.6f + 5.5f * harm));
        float hurt = harm * (0.55f + 0.45f * hurtPulse);

        // Kept moderate: the red reads as a pulse at the edges, not a wash over the
        // characters, which have to stay legible at exactly the moment you are panicking.
        p.fillRect(0, 0, L.w, L.h, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
        p.fillRect(0, L.deckTop, L.w, L.h, Glyph.mix(BG_HI, BG_HURT, hurt * 0.35f));

        // Two cloud layers behind the words...
        Sky.cloudBand(p, c, L, 0, Sky.CLOUD_FRONT_LAYER, hurt);

        p.save();
        if (c.shake > 0) {
            float m = c.shake * 0.016f * L.w;
            p.translate(m * (float) Math.sin(c.clock * 57f),
                    m * 0.6f * (float) Math.cos(c.clock * 71f));
        }

        dangerLine(p, c, L);
        for (int i = 0; i < c.enemies.size(); i++) enemy(p, c, L, c.enemies.get(i));
        powerup(p, c, L);
        shots(p, c, L);
        particles(p, c);
        flingHint(p, c, L);

        // ...and the nearest one in front of them, so words pass behind it. Kept the most
        // translucent of the three: it drifts over the play area and must never hide a letter.
        Sky.cloudBand(p, c, L, Sky.CLOUD_FRONT_LAYER, GameCore.CLOUD_LAYERS, hurt);

        keys(p, c, L);
        p.restore();

        // Red closing-in glow: from low health, and from a word about to land.
        Sky.vignette(p, L, ROSE, Math.max(hurt, c.warnLevel * (0.45f + 0.55f * hurtPulse)));

        // The title and game-over screens carry their own numbers; a second copy is clutter.
        if (c.state == GameCore.PLAY) {
            Sky.hudBacking(p, L, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
            Hud.hud(p, c, L);
            Hud.modeBar(p, c, L);
        }

        if (c.flash > 0) {
            p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(c.flashColor, (int) (c.flash * 52)));
        }

        if (c.state == GameCore.TITLE) Screens.title(p, c, L);
        else if (c.state == GameCore.OVER) Screens.gameOver(p, c, L);
        else if (c.state == GameCore.BONUS) Screens.bonus(p, c, L);
        else if (c.stageBanner > 0) Hud.stageBanner(p, c, L);

        if (c.state == GameCore.PLAY && c.perfectBanner > 0) Hud.perfectStage(p, c, L);
        if (c.settingsOpen) Screens.settings(p, c, L);
    }

    static void dangerLine(Painter p, GameCore c, Layout L) {
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

    static void enemy(Painter p, GameCore c, Layout L, GameCore.Enemy e) {
        float destroy = e.destroyed
                ? Math.min(1f, e.destroyT / GameCore.DESTROY_TIME) : 0f;
        if (e.dying) {
            // Killing shot still in the air: flash a ring, but keep the tiles on screen so
            // there is something for the fly-apart to act on.
            float t = Math.min(1f, e.deathT / 0.13f);
            p.strokePoly(Glyph.hex(c.enemyCentreX(e), e.y, L.enemyR * (1.1f + t * 1.6f)),
                    Glyph.withAlpha(INK, (int) (200 * (1f - t))), L.enemyR * 0.16f);
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

        boolean locked = c.target == e && destroy == 0f;
        // Entrance: eases in as the word clears the top edge.
        float enter = 0.62f + 0.38f * e.enterT;
        float swell = (1f + 0.26f * attack + 0.08f * e.warn) * enter
                * (1f - 0.30f * destroy);
        int fade = (int) ((90 + 165 * e.enterT) * (1f - destroy));

        for (int i = 0; i < e.word.length; i++) {
            int g = e.word[i];
            boolean head = i == e.pos && destroy == 0f;
            boolean cleared = i < e.pos && destroy == 0f;
            float x = c.tileX(e, i, L) + jx;
            float y = e.y + jy;

            // Tiles removed out of order fly off along the direction they were sent.
            if (e.gone[i] && destroy == 0f) {
                float t = Math.min(1f, e.goneT[i]);
                if (t >= 1f) continue;
                float ease = t * t;
                x += e.goneDx[i] * ease * L.w * 0.55f;
                y += e.goneDy[i] * ease * L.h * 0.30f;
                float r = L.enemyR * Layout.TILE_SCALE * (1f - 0.5f * t);
                int col = Glyph.withAlpha(Glyph.COLOR[g], (int) (200 * (1f - t)));
                p.strokePoly(Glyph.hex(x, y, r), col, r * 0.09f);
                Kawaii.draw(p, g, x, y, r * 0.60f, col, 1f, 0.6f);
                continue;
            }

            if (destroy > 0f) {
                // Accelerating away: outer tiles split left and right, the rest take the
                // nearer edge, and they fan slightly so the row does not stay a straight line.
                float ease = destroy * destroy;
                x += e.flyDir[i] * (0.10f + 1.15f * ease) * L.w * 0.60f;
                y += (i % 2 == 0 ? -1f : 1f) * ease * L.h * 0.045f;
            }

            float wobble = c.clock * 3.1f + e.phase + i * 0.7f;
            float scale = head ? Layout.HEAD_SCALE : cleared ? Layout.TILE_SCALE * 0.84f
                    : Layout.TILE_SCALE;
            float cellR = L.enemyR * scale * swell;

            // FLURRY recolours every letter on one upward-travelling rainbow wave.
            int col = c.flurry() ? rainbowAt(y, L, c.clock) : Glyph.COLOR[g];
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

            // Stacked tiles sit on a pile of offset copies, one per press still owed, so the
            // depth is legible before you even count the pips.
            int left = c.pressesLeft(e, i);
            for (int k = left - 1; k >= 1; k--) {
                float off = cellR * 0.15f * k;
                p.fillPoly(Glyph.hex(x + off, y - off, cellR),
                        Glyph.withAlpha(col, (26 - k * 4) * fade / 255));
                p.strokePoly(Glyph.hex(x + off, y - off, cellR),
                        Glyph.withAlpha(col, (95 - k * 18) * fade / 255), cellR * 0.055f);
            }

            p.fillPoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, fillA * fade / 255));
            p.strokePoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, edgeA * fade / 255),
                    cellR * 0.075f);

            float charR = cellR * 0.60f * (1f + 0.045f * (float) Math.sin(wobble))
                    * (1f + 0.34f * pop);
            float squash = 1f + 0.16f * pop - 0.05f * (float) Math.sin(wobble);
            int charCol = cleared ? Glyph.withAlpha(Glyph.mix(col, INK_DIM, 0.42f), 180) : col;
            Kawaii.draw(p, g, x, y, charR, charCol, squash,
                    cleared ? 1f : head ? 0.4f : 0.1f);

            // Exact count of presses still owed, so a 3-stack is never mistaken for a 4.
            if (left > 1) {
                float pr = cellR * 0.085f, gap = cellR * 0.255f;
                float py = y + cellR * 0.60f;
                float px = x - gap * (left - 1) / 2f;
                for (int k = 0; k < left; k++) {
                    p.fillCircle(px + k * gap, py, pr * 1.7f, Glyph.withAlpha(0xFF000000, 90));
                    p.fillCircle(px + k * gap, py, pr, Glyph.withAlpha(INK, 240));
                }
            }

            if (head && locked) {
                // Thicker white outline plus a caret, drawn at the smoothed position so the
                // indicator slides between letters instead of teleporting.
                float ix = c.caretXFor(e, L) + jx;
                float pulse = 0.6f + 0.4f * (float) Math.sin(c.clock * 7f);
                p.strokePoly(Glyph.hex(ix, y, cellR), Glyph.withAlpha(INK, (int) (215 * pulse)),
                        cellR * 0.13f);
                float cy = y - cellR * 1.55f, cw = cellR * 0.40f;
                p.fillPoly(new float[] {ix - cw, cy - cw, ix + cw, cy - cw, ix, cy + cw * 0.75f},
                        Glyph.withAlpha(INK, 225));
            }
        }
    }

    /**
     * The drifting powerup: a single letter with a rotating rainbow halo, labelled with the
     * mode it carries so you know what you are chasing before you commit a press to it.
     */
    static void powerup(Painter p, GameCore c, Layout L) {
        Power w = c.power;
        if (w == null) return;
        float r = L.enemyR * 1.25f;
        float bob = (float) Math.sin(w.t * 3.2f) * L.enemyR * 0.22f;
        float y = w.y + bob;
        int hue = Glyph.cycle(c.clock * 0.7f);

        if (w.hit) {
            // Caught: the halo blows outward and fades.
            float t = Math.min(1f, w.hitT / Power.POP_TIME);
            for (int k = 3; k >= 1; k--) {
                p.strokePoly(star(w.x, y, r * (1f + t * (2f + k)), r * 0.45f, 8, c.clock),
                        Glyph.withAlpha(hue, (int) (200 * (1f - t) / k)), r * 0.10f);
            }
            return;
        }

        // Halo: layered stars turning slowly, brightest at the core.
        for (int k = 4; k >= 1; k--) {
            p.fillPoly(star(w.x, y, r * (1.1f + 0.42f * k), r * 0.40f, 8, c.clock * 0.55f),
                    Glyph.withAlpha(hue, 30 / k));
        }
        float pulse = 0.85f + 0.15f * (float) Math.sin(w.t * 6f);
        p.fillPoly(Glyph.hex(w.x, y, r * pulse), Glyph.withAlpha(hue, 90));
        p.strokePoly(Glyph.hex(w.x, y, r * pulse), Glyph.withAlpha(INK, 235), r * 0.10f);
        Kawaii.draw(p, w.glyph, w.x, y, r * 0.58f, hue, 1f, 0.8f);

        p.text(w.name(), w.x, y - r * 1.7f, L.unit * 0.56f, Glyph.withAlpha(INK, 240),
                Painter.CENTER, true);
    }

    /**
     * Instructional finger for FLING, shown until the player first touches. A hand outline
     * tracing the same arc the sparkle trail follows, so the hint demonstrates the gesture
     * rather than describing it.
     */
    static void flingHint(Painter p, GameCore c, Layout L) {
        if (!c.showFlingHint()) return;
        float x = c.demoX, y = c.demoY;
        float r = L.enemyR * 0.85f;

        // Ripples spreading from the fingertip. Stroked thick: thin arcs come out looking
        // dotted, because the rasterizer draws them as round-capped segments.
        for (int k = 1; k <= 3; k++) {
            float t = ((c.clock * 0.9f) + k * 0.33f) % 1f;
            p.strokeCircle(x, y, r * (0.8f + t * 2.0f),
                    Glyph.withAlpha(INK, (int) (120 * (1f - t))), r * 0.22f);
        }

        // A hand: tapered finger angled down-right from the tip, into a rounded knuckle.
        float a = 0.68f;
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
        p.fillPoly(new float[] {
                x - dy * r * 0.46f, y + dx * r * 0.46f,
                x + dy * r * 0.46f, y - dx * r * 0.46f,
                x + dx * r * 2.0f + dy * r * 0.72f, y + dy * r * 2.0f - dx * r * 0.72f,
                x + dx * r * 2.0f - dy * r * 0.72f, y + dy * r * 2.0f + dx * r * 0.72f,
        }, Glyph.withAlpha(INK, 150));
        p.fillCircle(x + dx * r * 2.1f, y + dy * r * 2.1f, r * 0.80f,
                Glyph.withAlpha(INK, 150));
        // Fingertip, bright, sitting on the letters it is about to drag.
        p.fillCircle(x, y, r * 0.62f, Glyph.withAlpha(INK, 245));
        p.fillCircle(x, y, r * 0.30f, Glyph.withAlpha(0xFF2A2348, 210));
    }

    static void shots(Painter p, GameCore c, Layout L) {
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

    static void particles(Painter p, GameCore c) {
        for (int i = 0; i < c.particles.size(); i++) {
            GameCore.Particle q = c.particles.get(i);
            float k = q.life / q.max;
            p.fillCircle(q.x, q.y, q.size * (0.4f + k * 0.6f),
                    Glyph.withAlpha(q.color, (int) (235 * k)));
        }
    }

    // ---- key deck -----------------------------------------------------------

    static void keys(Painter p, GameCore c, Layout L) {
        int hint = c.hintGlyph();
        for (int g = 0; g < Glyph.COUNT; g++) {
            float press = c.keyPress[g], bad = c.keyBad[g];
            float r = L.keyR * (1f - 0.05f * press);
            float cx = L.keyX[g], cy = L.keyY[g];

            // Activated keys strobe through the palette rather than merely brightening.
            int col = c.flurry() ? rainbowAt(cy, L, c.clock) : Glyph.COLOR[g];
            if (press > 0.02f) {
                col = Glyph.mix(col, Glyph.cycle(c.clock * 9f + g * 0.13f), press * 0.9f);
            }
            if (bad > 0) col = Glyph.mix(col, ROSE, bad);

            // During the interlude only two keys matter; ring them, and mark the next one.
            if (c.bonusMashing() && (g == c.steamer.leftKey || g == c.steamer.rightKey)) {
                boolean wanted = g == c.steamer.wanted();
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 8f);
                p.strokePoly(Glyph.hex(cx, cy, r * 1.16f),
                        Glyph.withAlpha(wanted ? INK : col,
                                wanted ? (int) (110 + 145 * pulse) : 90), r * 0.075f);
            }

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
}
