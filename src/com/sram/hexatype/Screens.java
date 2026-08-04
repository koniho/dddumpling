package com.sram.hexatype;

/**
 * Full-screen states: title, game over, the between-stages minigame and settings.
 */
final class Screens extends Draw {

    private Screens() {}

    // ---- screens ------------------------------------------------------------

    /** Dims everything above the key deck, so the real keys stay lit as the tutorial. */
    static float scrim(Painter p, Layout L, int a) {
        float bottom = L.deckTop;
        p.fillRect(0, 0, L.w, bottom, Glyph.withAlpha(0xFF120E22, a));
        return bottom;
    }

    static void handLabels(Painter p, Layout L, float baseline) {
        handLabels(p, L, baseline, 1f);
    }

    static void handLabels(Painter p, Layout L, float baseline, float fade) {
        int col = fadeBy(INK_DIM, fade);
        p.text("LEFT HAND", L.keyX[1], baseline, L.unit * 0.5f, col, Painter.CENTER, true);
        p.text("RIGHT HAND", L.keyX[4], baseline, L.unit * 0.5f, col, Painter.CENTER, true);
    }

    static void title(Painter p, GameCore c, Layout L) {
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

    static void gameOver(Painter p, GameCore c, Layout L) {
        float bottom = scrim(p, L, 220);
        float s = L.unit;
        p.text("GAME OVER", L.w / 2f, L.h * 0.24f, s * 1.85f, ROSE, Painter.CENTER, true);

        p.text("SCORE", L.w / 2f, L.h * 0.325f, s * 0.6f, INK_DIM, Painter.CENTER, false);
        p.text(String.valueOf(c.score), L.w / 2f, L.h * 0.325f + s * 1.8f, s * 1.8f, INK,
                Painter.CENTER, true);

        accuracy(p, c, L, L.h * 0.475f);

        p.text("STAGE " + c.stage + "   KILLS " + c.kills, L.w / 2f, L.h * 0.615f, s * 0.6f,
                INK_DIM, Painter.CENTER, false);
        p.text("BEST COMBO " + c.maxCombo, L.w / 2f, L.h * 0.615f + s * 0.85f, s * 0.6f,
                INK_DIM, Painter.CENTER, false);
        p.text(c.score >= c.best ? "NEW BEST!" : "BEST " + c.best, L.w / 2f, L.h * 0.695f,
                s * 0.78f, c.score >= c.best ? GOLD : INK_DIM, Painter.CENTER, true);

        if (c.time > 0.6f) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 3.2f);
            p.text("TAP TO RESTART", L.w / 2f, L.h * 0.765f, s * 0.95f,
                    Glyph.withAlpha(INK, (int) (255 * pulse)), Painter.CENTER, true);
        }

        handLabels(p, L, bottom - s * 0.45f);
    }

    /**
     * Accuracy readout: the percentage, and a dumpling whose face carries it — miserable
     * at 60% or below, delighted at 90% or above. It idles gently when sad and bounces
     * when pleased, so the mood reads before the number does.
     */
    static void accuracy(Painter p, GameCore c, Layout L, float cy) {
        float s = L.unit;
        float mood = c.accuracyMood();
        int pct = c.accuracyPercent();
        int tint = mood >= 0.999f ? GOLD : Glyph.mix(Glyph.COLOR[0], ROSE, (1f - mood) * 0.55f);

        // Happier moods bounce faster and higher; a sad dumpling just sways.
        float r = s * 1.35f;
        float bob = (float) Math.abs(Math.sin(c.clock * (1.7f + 2.8f * mood)))
                * r * (0.05f + 0.20f * mood);
        float squash = 1f + 0.06f * (float) Math.sin(c.clock * (2.2f + 4f * mood));
        float dx = (1f - mood) * r * 0.12f * (float) Math.sin(c.clock * 1.3f);

        Kawaii.moodDumpling(p, L.w / 2f - s * 3.2f + dx, cy - bob, r, tint, mood, squash);

        p.text("ACCURACY", L.w / 2f + s * 1.5f, cy - s * 0.75f, s * 0.58f, INK_DIM,
                Painter.LEFT, true);
        p.text(pct + "%", L.w / 2f + s * 1.5f, cy + s * 0.95f, s * 1.55f, tint,
                Painter.LEFT, true);
        p.text(c.hits + " HIT   " + c.misses + " MISS", L.w / 2f + s * 1.5f, cy + s * 1.75f,
                s * 0.5f, INK_DIM, Painter.LEFT, false);
    }


    /** How long the interlude heading takes to swell into place. */
    static final float INTRO_TIME = 0.55f;

    /**
     * Size multiplier for the interlude heading: starts large, overshoots small and settles
     * at 1. Scaling down into place reads as the text arriving from the front, which suits a
     * scene that is fading up underneath it.
     */
    static float introScale(float time) {
        if (time >= INTRO_TIME) return 1f;
        float t = time / INTRO_TIME;
        // Ease-out-back: carries the progress slightly past 1 before returning, which on a
        // shrinking size means the text dips just under its final size and springs back.
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        float back = 1f + c3 * u * u * u + c1 * u * u;
        return 1.9f - 0.9f * back;
    }

    /**
     * Between-stages minigame: mash any key to lever the lid off a dim sum steamer and free
     * the rainbow dumpling inside. Progress carries across interludes, so the lid creeps up
     * over several stages.
     */
    static void bonus(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        // Eases in on arrival and back out as the timer expires, so neither edge of the
        // interlude is a hard cut between scenes. Every colour below is scaled by it.
        float fade = Math.min(1f, c.time / 0.40f) * Math.min(1f, c.bonusTimer / 0.40f);
        if (fade <= 0.01f) return;

        // Dim only the sky: the keys are the instrument here and must stay lit.
        scrim(p, L, (int) (195 * fade));

        float open = c.steamer.lidOpen();
        boolean freed = c.steamer.freedT > 0f;

        float cx = L.w / 2f;
        float cy = L.h * 0.46f;
        // Half-extents. A steamer basket is wide but not a bar: roughly 3:1.
        float bw = Math.min(L.w * 0.30f, s * 7.0f);
        float bh = s * 3.4f;

        // Heading swells in over the fade, overshooting and settling, so the interlude
        // announces itself instead of simply appearing.
        float intro = introScale(c.time);
        p.text(freed ? "FREE!" : "FREE THE DUMPLING", cx, L.h * 0.235f,
                s * (freed ? 1.5f : 0.95f) * intro,
                fadeBy(freed ? GOLD : INK, fade), Painter.CENTER, true);
        if (!freed) {
            p.text("MASH ANY KEY", cx, L.h * 0.235f + s * 1.2f, s * 0.62f,
                    fadeBy(INK_DIM, fade), Painter.CENTER, false);
        }

        // The dumpling: rainbow, and cheerier the closer it is to getting out.
        float dumpR = bh * 0.72f;
        float dumpY = cy - bh * 0.10f;
        if (freed) {
            // Escaping: rises and grows away as the celebration plays.
            float t = 1f - c.steamer.freedT / 1.7f;
            dumpY -= t * t * L.h * 0.30f;
            dumpR *= 1f + 0.35f * t;
        }
        int rainbow = Glyph.cycle(c.clock * 0.5f);
        // Rays only once it is out: behind a closed lid they just show through the gap.
        if (freed) {
            for (int k = 3; k >= 1; k--) {
                p.fillPoly(star(cx, dumpY, dumpR * (1.4f + 0.7f * k), dumpR * 0.5f, 8,
                        c.clock * 0.6f), fadeBy(Glyph.withAlpha(rainbow, 40 / k), fade));
            }
        }
        Kawaii.moodDumpling(p, cx, dumpY, dumpR, fadeBy(rainbow, fade),
                freed ? 1f : 0.15f + 0.55f * open, 1f + 0.06f * (float) Math.sin(c.clock * 4f));

        if (!freed) {
            // Basket body, over the dumpling's lower half so it reads as contained.
            int body = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f), c.steamer.flash * 0.85f);
            float bodyCy = cy + bh * 0.42f, bodyH = bh * 0.60f;
            p.fillPoly(pill(cx, bodyCy, bw, bodyH, 10), fadeBy(body, fade));
            p.fillPoly(pill(cx, bodyCy, bw, bodyH, 10),
                    fadeBy(Glyph.withAlpha(0xFF000000, (int) (30 * (1f - c.steamer.flash))), fade));
            // Woven slats.
            for (int k = -1; k <= 1; k++) {
                p.fillPoly(pill(cx, bodyCy + k * bh * 0.28f, bw * 0.92f, bh * 0.045f, 6),
                        fadeBy(Glyph.withAlpha(BAMBOO_DARK, 120), fade));
            }

            // Lid: lifts with progress, and kicks up further on each press. Capped so that
            // at full open it just clears the rim rather than floating away from it.
            float lift = open * bh * 1.0f + c.steamer.lidPulse * bh * 0.28f;
            float lidY = cy - bh * 0.52f - lift;
            int lidCol = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f + 0.3f),
                    c.steamer.flash * 0.85f);
            p.fillPoly(pill(cx, lidY, bw * 1.05f, bh * 0.26f, 10), fadeBy(lidCol, fade));
            p.fillPoly(pill(cx, lidY - bh * 0.20f, bw * 0.20f, bh * 0.09f, 8), fadeBy(lidCol, fade));
            for (int k = -1; k <= 1; k += 2) {
                p.fillPoly(pill(cx + k * bw * 0.58f, lidY, bw * 0.24f, bh * 0.07f, 6),
                        fadeBy(Glyph.withAlpha(BAMBOO_DARK, 110), fade));
            }

            // Steam escaping through the widening gap.
            if (open > 0.05f) {
                for (int k = 0; k < 4; k++) {
                    float wob = (float) Math.sin(c.clock * 2.2f + k * 1.7f);
                    float sx2 = cx + (k - 1.5f) * bw * 0.34f + wob * s * 0.25f;
                    float sy2 = lidY - bh * 0.4f - open * s * (0.6f + 0.5f * k);
                    p.fillPoly(pill(sx2, sy2, s * 0.34f * open, s * 0.11f * open, 6),
                            fadeBy(Glyph.withAlpha(INK, (int) (70 * open)), fade));
                }
            }

            // Progress: one pip per press needed.
            int cols = 10;
            float pr = s * 0.14f, gap = s * 0.54f;
            float x0 = cx - gap * (cols - 1) / 2f;
            float rowY = L.h * 0.63f;
            for (int i = 0; i < GameCore.STEAMER_HITS; i++) {
                float px = x0 + (i % cols) * gap;
                float py = rowY + (i / cols) * gap * 1.15f;
                p.fillCircle(px, py, pr,
                        fadeBy(i < c.steamer.hits ? rainbow : Glyph.withAlpha(INK, 45), fade));
            }
            p.text(c.steamer.hits + " / " + GameCore.STEAMER_HITS, cx,
                    rowY + gap * 1.15f + s * 1.5f, s * 0.62f, fadeBy(INK_DIM, fade), Painter.CENTER, true);
        } else {
            p.text("+" + GameCore.FREE_BONUS, cx, L.h * 0.63f, s * 1.1f, fadeBy(GOLD, fade),
                    Painter.CENTER, true);
        }

        handLabels(p, L, L.deckTop - s * 0.45f, fade);
    }

    /** Settings panel: pacing multiplier and music choice. Freezes the game behind it. */
    static void settings(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);

        p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(0xFF0D0A18, 205));
        p.fillRect(ui.panelL, ui.panelT, ui.panelR, ui.panelB,
                Glyph.withAlpha(0xFF2A2348, 250));
        p.strokePoly(new float[] {ui.panelL, ui.panelT, ui.panelR, ui.panelT, ui.panelR,
                ui.panelB, ui.panelL, ui.panelB}, Glyph.withAlpha(INK, 60), s * 0.06f);

        p.text("SETTINGS", ui.panelL + s * 1.2f, ui.titleY, s * 0.92f, INK, Painter.LEFT, true);
        p.text("PAUSED", ui.panelL + s * 1.2f, ui.titleY + s * 0.8f, s * 0.5f, INK_DIM,
                Painter.LEFT, false);

        // Close button.
        p.fillCircle(ui.closeCx, ui.closeCy, ui.closeR, Glyph.withAlpha(ROSE, 60));
        p.strokeCircle(ui.closeCx, ui.closeCy, ui.closeR, Glyph.withAlpha(ROSE, 220),
                s * 0.06f);
        float k = ui.closeR * 0.42f;
        p.line(ui.closeCx - k, ui.closeCy - k, ui.closeCx + k, ui.closeCy + k, INK, s * 0.09f);
        p.line(ui.closeCx + k, ui.closeCy - k, ui.closeCx - k, ui.closeCy + k, INK, s * 0.09f);

        // Speed slider.
        p.text("SPEED", ui.sliderL, ui.speedLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        p.fillRect(ui.sliderL, ui.sliderY - ui.sliderH / 2f, ui.sliderR,
                ui.sliderY + ui.sliderH / 2f, Glyph.withAlpha(INK, 40));
        float kx = ui.knobX(c.speed);
        p.fillRect(ui.sliderL, ui.sliderY - ui.sliderH / 2f, kx,
                ui.sliderY + ui.sliderH / 2f, Glyph.withAlpha(Glyph.COLOR[4], 210));
        p.fillPoly(Glyph.hex(kx, ui.sliderY, s * 0.62f), Glyph.withAlpha(Glyph.COLOR[4], 255));
        p.strokePoly(Glyph.hex(kx, ui.sliderY, s * 0.62f), Glyph.withAlpha(INK, 200),
                s * 0.055f);

        p.text("0.5X", ui.sliderL, ui.speedValueY, s * 0.48f, INK_DIM, Painter.LEFT, false);
        p.text("1.5X", ui.sliderR, ui.speedValueY, s * 0.48f, INK_DIM, Painter.RIGHT, false);
        p.text(fmtSpeed(c.speed) + "X", (ui.sliderL + ui.sliderR) / 2f, ui.speedValueY,
                s * 0.72f, INK, Painter.CENTER, true);

        // Music options.
        p.text("MUSIC", ui.sliderL, ui.bgmLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        for (int i = 0; i < Music.NAMES.length; i++) {
            float cy = ui.optionCy(i);
            boolean on = i == c.bgmChoice;
            int col = on ? Glyph.COLOR[i % Glyph.COUNT] : INK_DIM;
            if (on) {
                p.fillRect(ui.optionL(), cy - ui.optionH * 0.40f, ui.optionR(),
                        cy + ui.optionH * 0.40f, Glyph.withAlpha(col, 40));
            }
            float bx = ui.optionL() + s * 0.75f;
            p.fillPoly(Glyph.hex(bx, cy, s * 0.34f), Glyph.withAlpha(col, on ? 235 : 45));
            p.strokePoly(Glyph.hex(bx, cy, s * 0.34f), Glyph.withAlpha(col, 190), s * 0.045f);
            p.text(Music.NAMES[i], bx + s * 0.9f, cy + s * 0.22f, s * 0.6f,
                    on ? INK : INK_DIM, Painter.LEFT, on);
        }

        // Playtest: drop straight into a mode instead of waiting for a letter to drift past.
        p.text("PLAYTEST", ui.sliderL, ui.testLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        for (int i = 0; i < Power.COUNT; i++) {
            float l = ui.testChipL(i, Power.COUNT), r = ui.testChipR(i, Power.COUNT);
            int col = Glyph.cycle(i / (float) Power.COUNT);
            p.fillRect(l, ui.testY, r, ui.testY + ui.testH, Glyph.withAlpha(col, 46));
            p.strokePoly(new float[] {l, ui.testY, r, ui.testY, r, ui.testY + ui.testH,
                    l, ui.testY + ui.testH}, Glyph.withAlpha(col, 190), s * 0.05f);
            p.text(Power.NAMES[i], (l + r) / 2f, ui.testY + ui.testH * 0.66f, s * 0.56f,
                    INK, Painter.CENTER, true);
        }
    }

    /** One decimal place without String.format, which is not worth the cost per frame. */
    static String fmtSpeed(float v) {
        int tenths = Math.round(v * 10f);
        return (tenths / 10) + "." + (tenths % 10);
    }
}
