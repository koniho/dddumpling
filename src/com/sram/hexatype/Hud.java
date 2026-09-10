package com.sram.hexatype;

/**
 * The in-play readouts: score, stage, lives, the frenzy bar, the stage banner and the
 * flawless-wave celebration.
 */
final class Hud extends Draw {

    private Hud() {}

    // ---- HUD ----------------------------------------------------------------

    /**
     * Baseline of the HUD's small labels, one line above the readouts they title. Exposed so the
     * clearance below the number can be asserted rather than eyeballed.
     */
    static float labelY(Layout L) {
        return L.hudY - type(L.unit * 0.95f);
    }

    /** Type size of the score readout, whose caps have to clear {@link #labelY}. */
    static float scoreSize(Layout L) {
        return type(L.unit * 1.05f);
    }

    static void hud(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        // The labels sit one line above the number, so the gap between them has to be scaled by
        // type() along with the sizes it separates. At a plain 0.95 units the score's digits stood
        // 1.01 units tall once TEXT reached 1.34 and their caps came up through SCORE's baseline —
        // three pixels of collision at 1080 wide, and the exact trap CLAUDE.md records for stacked
        // text. Screens was fixed for it at the time; this line was missed.
        float labelY = labelY(L);
        p.text("SCORE", L.playLeft, labelY, type(s * 0.52f), INK_DIM, Painter.LEFT, false);
        p.text(String.valueOf(c.score), L.playLeft, L.hudY, scoreSize(L), INK, Painter.LEFT, true);

        p.text("STAGE " + c.stage, L.w / 2f, labelY, type(s * 0.58f), INK_DIM,
                Painter.CENTER, true);
        // Small hex-and-dot to the right: this readout is the settings button. Hung off the label's
        // own baseline rather than the HUD line, so it travels with the text it belongs to.
        if (BuildFlags.DEVELOPER) {
            float gx = L.w / 2f + s * 2.5f, gy = labelY - type(s * 0.20f);
            p.strokePoly(Glyph.hex(gx, gy, s * 0.34f), Glyph.withAlpha(INK, 95), s * 0.05f);
            p.fillCircle(gx, gy, s * 0.10f, Glyph.withAlpha(INK, 120));
        }
        // One pip per word in this stage's wave, filling as each is dealt with.
        //
        // Left out entirely on a boss stage: there is no wave there, so the quota is never counted
        // up and the row would sit empty for the whole fight — which reads as broken rather than as
        // "not applicable". The boss's own health bar is that stage's progress readout.
        if (!c.bossActive()) {
            int quota = c.stageQuota();
            int done = Math.min(quota, c.resolvedThisStage);
            float span = Math.min(s * 0.46f * (quota - 1), L.w * 0.38f);
            float gap = quota > 1 ? span / (quota - 1) : 0f;
            float x0 = L.w / 2f - span / 2f;
            float pr = Math.min(s * 0.13f, gap * 0.36f);
            for (int i = 0; i < quota; i++) {
                p.fillCircle(x0 + i * gap, L.hudY - s * 0.30f, pr,
                        i < done ? INK : Glyph.withAlpha(INK, 55));
            }
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

    /** Active frenzy: the mode name, what it does, and how long is left. */
    static void modeBar(Painter p, GameCore c, Layout L) {
        if (!c.powerActive()) return;
        float s = L.unit;
        float y = L.playTop + s * 0.9f;
        float left = L.playLeft, right = L.playRight;
        int hue = Glyph.cycle(c.clock * 0.85f);

        // Countdown bar: the whole width at the start, empty as it expires.
        float frac = Math.min(1f, c.modeLeft / Power.DURATION);
        p.fillRect(left, y, right, y + s * 0.22f, Glyph.withAlpha(INK, 40));
        p.fillRect(left, y, left + (right - left) * frac, y + s * 0.22f,
                Glyph.withAlpha(hue, 235));

        p.text(Power.NAMES[c.mode], L.w / 2f, y - s * 0.35f, type(s * 0.86f), hue,
                Painter.CENTER, true);
        p.text(Power.BLURB[c.mode], L.w / 2f, y + s * 1.05f, type(s * 0.5f), INK_DIM,
                Painter.CENTER, false);
    }

    /**
     * Stage title, with one of ten character vignettes under it in place of the old
     * "FASTER NOW" line. They cycle by stage, so a run sees all ten.
     */
    /**
     * The payoff for a stroke that took several words at once. Outlives the slow-motion beat
     * it arrived with, so the number is still readable once normal speed is back.
     */
    static void sliceCall(Painter p, GameCore c, Layout L) {
        // The stroke's own counts are reset by the next stroke, which can start under the same
        // finger now; these are the frozen ones the announcement was made with.
        if (c.sliceCall <= 0f || c.callKills < Blade.SLOW_KILLS) return;
        callOut(p, L, c.sliceCall / Blade.SLICE_CALL_TIME, c.callKills + " IN ONE!",
                c.callCuts + " LETTERS", GOLD);
    }

    /** The same payoff for a long MULTI chain, which is the other thing worth shouting about. */
    static void chainCall(Painter p, GameCore c, Layout L) {
        if (c.chainT <= 0f || c.chainLen < CHAIN_CALL) return;
        callOut(p, L, c.chainT / GameCore.CHAIN_TIME, c.chainShown + " CHAINED!",
                "+" + c.chainScore, Glyph.COLOR[c.chainGlyph]);
    }

    /** The push-back payoff, in the same place as the other two so they read alike. */
    static void pushCall(Painter p, GameCore c, Layout L) {
        if (c.pushT <= 0f) return;
        callOut(p, L, c.pushT / GameCore.PUSH_TIME, "PUSHED BACK!",
                c.pushCount + (c.pushCount == 1 ? " WORD" : " WORDS"), GOLD);
    }

    /** Hops a chain needs before it is worth announcing. Two is just a pair. */
    static final int CHAIN_CALL = 3;

    /**
     * One shared readout for both, so the two payoffs land in the same place at the same size
     * and read as the same kind of event.
     *
     * @param t 0..1 of its own remaining time; it swells as it arrives and fades as it goes
     */
    private static void callOut(Painter p, Layout L, float t, String big, String small,
            int tint) {
        float s = L.unit;
        float pop = 1f + 0.35f * t * t;
        int a = (int) (255 * Math.min(1f, t * 2.2f));
        // High, just under the mode bar. It used to sit at mid-height, which was fine for the
        // blade — that happens under your finger — but a chain threads down the whole field and
        // the readout landed straight on top of it.
        float y = L.h * 0.205f;
        p.text(big, L.w / 2f, y, type(s * 1.6f * pop), Glyph.withAlpha(tint, a), Painter.CENTER, true);
        p.text(small, L.w / 2f, y + s * 1.15f, type(s * 0.70f), Glyph.withAlpha(INK, a),
                Painter.CENTER, true);
    }

    static void stageBanner(Painter p, GameCore c, Layout L) {
        float k = Math.min(1f, c.stageBanner / 0.4f);
        int a = (int) (235 * k);
        p.text("STAGE " + c.stage, L.w / 2f, L.h * 0.34f, type(L.unit * 1.7f),
                Glyph.withAlpha(INK, a), Painter.CENTER, true);

        int skit = Skits.forStage(c.stage);
        // stageBanner counts down from BANNER_TIME, so invert it into 0..1 progress.
        float t = 1f - Math.min(1f, Math.max(0f, c.stageBanner / GameCore.BANNER_TIME));
        Skits.draw(p, L, skit, L.w / 2f, L.h * 0.46f, L.unit * 2.1f, t, a, c.clock);
    }

    /**
     * Reward for clearing a whole wave without a single wrong press: a gold dumpling that
     * fades in and bounces.
     */
    static void perfectStage(Painter p, GameCore c, Layout L) {
        float t = 1f - c.perfectBanner / GameCore.PERFECT_TIME;   // 0 at the start
        float in = Math.min(1f, t / 0.22f);                       // fade/scale in
        float out = Math.min(1f, c.perfectBanner / 0.35f);        // and back out
        float a = in * out;
        if (a <= 0.01f) return;

        float r = L.unit * 1.85f * (0.55f + 0.45f * in);
        float cx = L.w / 2f;
        // Bouncy: settles as the celebration plays out.
        float bounce = (float) Math.abs(Math.sin(t * 9.5f)) * (1f - t) * r * 0.42f;
        // Sits well above the stage banner at 0.38h, which shows at the same moment.
        float cy = L.h * 0.205f - bounce;
        float squash = 1f + 0.14f * (float) Math.sin(t * 19f) * (1f - t);

        // Glowing star behind it: stacked translucent copies, largest and faintest first,
        // turning slowly so the glow shimmers rather than sitting still.
        float spin = c.clock * 0.5f;
        float grow = 1f + 0.06f * (float) Math.sin(c.clock * 3.5f);
        for (int k = 4; k >= 1; k--) {
            float rr = r * (1.5f + 0.62f * k) * grow;
            p.fillPoly(star(cx, cy, rr, rr * 0.40f, 8, spin),
                    Glyph.withAlpha(GOLD, (int) (a * 26 / k)));
        }
        p.fillPoly(star(cx, cy, r * 2.05f * grow, r * 0.72f, 4, spin + 0.4f),
                Glyph.withAlpha(0xFFFFF3C4, (int) (a * 105)));

        for (int k = 3; k >= 1; k--) {
            p.strokePoly(Glyph.hex(cx, cy, r * (1.25f + 0.30f * k)),
                    Glyph.withAlpha(GOLD, (int) (a * 60 / k)), r * 0.05f);
        }
        Kawaii.moodDumpling(p, cx, cy, r, Glyph.withAlpha(GOLD, (int) (255 * a)), 1f, squash);
        p.text("PERFECT WAVE", cx, cy + r * 1.85f, type(L.unit * 0.86f),
                Glyph.withAlpha(GOLD, (int) (255 * a)), Painter.CENTER, true);
    }
}
