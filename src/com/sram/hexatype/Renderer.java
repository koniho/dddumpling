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
    static final int GOLD = 0xFFFFCE4A;

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
        cloudBand(p, c, L, 0, CLOUD_FRONT_LAYER, hurt);

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

        // ...and the nearest one in front of them, so words pass behind it. Kept the most
        // translucent of the three: it drifts over the play area and must never hide a letter.
        cloudBand(p, c, L, CLOUD_FRONT_LAYER, GameCore.CLOUD_LAYERS, hurt);

        keys(p, c, L);
        p.restore();

        // Red closing-in glow: from low health, and from a word about to land.
        vignette(p, L, ROSE, Math.max(hurt, c.warnLevel * (0.45f + 0.55f * hurtPulse)));

        // The title and game-over screens carry their own numbers; a second copy is clutter.
        if (c.state == GameCore.PLAY) {
            hudBacking(p, L, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
            hud(p, c, L);
        }

        if (c.flash > 0) {
            p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(c.flashColor, (int) (c.flash * 52)));
        }

        if (c.state == GameCore.TITLE) title(p, c, L);
        else if (c.state == GameCore.OVER) gameOver(p, c, L);
        else if (c.state == GameCore.BONUS) bonus(p, c, L);
        else if (c.stageBanner > 0) stageBanner(p, c, L);

        if (c.state == GameCore.PLAY && c.perfectBanner > 0) perfectStage(p, c, L);
        if (c.settingsOpen) settings(p, c, L);
    }

    /** Settings panel: pacing multiplier and music choice. Freezes the game behind it. */
    private static void settings(Painter p, GameCore c, Layout L) {
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
    }

    /** One decimal place without String.format, which is not worth the cost per frame. */
    private static String fmtSpeed(float v) {
        int tenths = Math.round(v * 10f);
        return (tenths / 10) + "." + (tenths % 10);
    }

    /**
     * Reward for clearing a whole wave without a single wrong press: a gold dumpling that
     * fades in and bounces.
     */
    private static void perfectStage(Painter p, GameCore c, Layout L) {
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
        p.text("PERFECT WAVE", cx, cy + r * 1.85f, L.unit * 0.86f,
                Glyph.withAlpha(GOLD, (int) (255 * a)), Painter.CENTER, true);
    }

    // ---- background ---------------------------------------------------------

    /** Per-layer cloud tint and opacity, back to front. */
    private static final int[] CLOUD_TINT = {0xFF5C5490, 0xFF8478BE, 0xFFC3B7EE};
    // Low, because the soft layers accumulate: the visible density is several times these.
    private static final int[] CLOUD_ALPHA = {30, 22, 14};
    /** The one layer drawn over the enemies; the rest go behind. */
    static final int CLOUD_FRONT_LAYER = GameCore.CLOUD_LAYERS - 1;

    /**
     * One parallax layer of clouds, drifting downward. Nearer layers move faster, sit
     * lighter and are drawn wider, which is what sells the depth.
     */
    /** Depth scale of a cloud layer; nearer layers are bigger. */
    private static float cloudScale(int layer) {
        return 0.62f + 0.30f * layer;
    }

    /** Nominal cloud height for a layer. */
    static float cloudHeight(Layout L, int layer) {
        return L.h * 0.055f * cloudScale(layer) * 1.15f;
    }

    /**
     * How far past an edge a cloud's centre must be for the whole shape to be out of sight.
     * The tallest puff reaches about 1.2 heights from the centre once its outer soft ring
     * and vertical jitter are counted; rounded up.
     */
    static float cloudMargin(Layout L, int layer) {
        return cloudHeight(L, layer) * 1.35f;
    }

    /**
     * Vertical centre of a cloud right now. The travel spans a full margin beyond each end
     * of the sky, so a cloud has completely left the visible area before its phase wraps —
     * otherwise it vanishes mid-screen.
     */
    static float cloudY(GameCore c, Layout L, int layer, int i) {
        float margin = cloudMargin(L, layer);
        return -margin + c.cloudPhase(layer, i) * (L.deckTop + 2f * margin);
    }

    private static void clouds(Painter p, GameCore c, Layout L, int layer, float hurt) {
        int tint = Glyph.mix(CLOUD_TINT[layer], BG_HURT, hurt * 0.55f);
        int alpha = CLOUD_ALPHA[layer];

        // A landed press washes the sky with that letter's colour; a cleared word floods it
        // yellow. Brightening the alpha as well as the hue is what makes it read as a glow
        // rather than as a recolour.
        if (c.skyGlow > 0f) {
            tint = Glyph.mix(tint, c.skyGlowColor, c.skyGlow * 0.60f);
            alpha += (int) (alpha * c.skyGlow * 0.75f);
        }
        float scale = cloudScale(layer);
        float h = cloudHeight(L, layer);

        for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
            float w = L.w * scale * c.cloudW[layer][i];
            cloud(p, c.cloudX[layer][i] * L.w, cloudY(c, L, layer, i), w, h, tint, alpha,
                    c.cloudSeed[layer][i]);
        }
    }

    /** Draws the given layers clipped to the sky, so they slide away behind the key deck. */
    private static void cloudBand(Painter p, GameCore c, Layout L, int from, int to,
            float hurt) {
        p.save();
        p.clipRect(0, 0, L.w, L.deckTop);
        for (int l = from; l < to; l++) clouds(p, c, L, l, hurt);
        p.restore();
    }

    /**
     * A single wide, soft cloud: a fully rounded rectangle base with a row of rounded-
     * rectangle puffs along it. The flat tops and bottoms give the cloud a banded, drifting
     * feel that stacked ellipses did not have.
     *
     * Two things make it read as cloud rather than as a row of separate shapes. The puffs
     * are much wider than the gap between them, so they merge into one mass; and each is
     * drawn three times at increasing size with the outermost barely visible, so the
     * accumulated alpha falls off gradually instead of ending at a hard edge.
     */
    private static void cloud(Painter p, float cx, float cy, float w, float h, int tint,
            int alpha, int seed) {
        p.fillPoly(pill(cx, cy, w * 0.5f, h * 0.40f, 10),
                Glyph.withAlpha(tint, alpha * 40 / 100));

        int puffs = 4;
        for (int k = 0; k < puffs; k++) {
            float t = (float) k / (puffs - 1);
            // Taller through the middle, tapering at both ends.
            float bump = 0.45f + 0.55f * (float) Math.sin(Math.PI * t);
            // Spread kept well under the puff width, so neighbours overlap heavily.
            float px = cx + (t - 0.5f) * w * 0.48f;
            float ry = h * bump * (0.52f + 0.20f * hash(seed + k * 31));
            float rx = ry * (2.3f + 0.8f * hash(seed + k * 57));
            float py = cy - h * 0.12f * bump + h * 0.10f * (hash(seed + k * 91) - 0.5f);

            // Outermost first, faintest: the overlap builds the falloff.
            p.fillPoly(pill(px, py, rx * 1.42f, ry * 1.42f, 8),
                    Glyph.withAlpha(tint, alpha / 5));
            p.fillPoly(pill(px, py, rx * 1.20f, ry * 1.20f, 8),
                    Glyph.withAlpha(tint, alpha / 3));
            p.fillPoly(pill(px, py, rx, ry, 8), Glyph.withAlpha(tint, alpha));
        }
    }

    /** Deterministic 0..1 from an int, so cloud shapes are stable across frames. */
    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    /**
     * Edge glow from overlapping strips rather than discrete rings: each layer reaches
     * from an edge inward by a shrinking amount, all at the same low alpha, so the
     * build-up is a smooth ramp instead of visible bands. Corners get both a horizontal
     * and a vertical layer, which is what a vignette wants anyway.
     */
    /**
     * Fully rounded rectangle — a stadium: straight top and bottom edges with semicircular
     * caps, cap radius equal to the half-height. Emitted as one polygon rather than a rect
     * plus two circles, because translucent fills would double-blend where those overlap.
     *
     * @param rx half the total length, caps included
     * @param ry half the height, which is also the cap radius
     */
    private static float[] pill(float cx, float cy, float rx, float ry, int segs) {
        float straight = Math.max(0f, rx - ry);
        float[] pts = new float[(segs + 1) * 4];
        int i = 0;
        // Right cap: top, round the outside, to bottom.
        for (int k = 0; k <= segs; k++) {
            double a = -Math.PI / 2 + Math.PI * k / segs;
            pts[i++] = cx + straight + ry * (float) Math.cos(a);
            pts[i++] = cy + ry * (float) Math.sin(a);
        }
        // Left cap: bottom, round the outside, back to top.
        for (int k = 0; k <= segs; k++) {
            double a = Math.PI / 2 + Math.PI * k / segs;
            pts[i++] = cx - straight + ry * (float) Math.cos(a);
            pts[i++] = cy + ry * (float) Math.sin(a);
        }
        return pts;
    }

    /** Star polygon with {@code points} spikes, rotated by {@code rot} radians. */
    private static float[] star(float cx, float cy, float outer, float inner, int points,
            float rot) {
        float[] pts = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = rot + Math.PI * i / points;
            float rr = (i % 2 == 0) ? outer : inner;
            pts[i * 2] = cx + rr * (float) Math.cos(a);
            pts[i * 2 + 1] = cy + rr * (float) Math.sin(a);
        }
        return pts;
    }

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
        // Small hex-and-dot to the right: this readout is the settings button.
        float gx = L.w / 2f + s * 2.5f, gy = L.hudY - s * 1.15f;
        p.strokePoly(Glyph.hex(gx, gy, s * 0.34f), Glyph.withAlpha(INK, 95), s * 0.05f);
        p.fillCircle(gx, gy, s * 0.10f, Glyph.withAlpha(INK, 120));
        // One pip per word in this stage's wave, filling as each is dealt with.
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
        float bottom = L.deckTop;
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
    private static void accuracy(Painter p, GameCore c, Layout L, float cy) {
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

    private static final int BAMBOO = 0xFFD9AE6E;
    private static final int BAMBOO_DARK = 0xFF8E6B3A;

    /**
     * Between-stages minigame: mash any key to lever the lid off a dim sum steamer and free
     * the rainbow dumpling inside. Progress carries across interludes, so the lid creeps up
     * over several stages.
     */
    private static void bonus(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        // Dim only the sky: the keys are the instrument here and must stay lit.
        scrim(p, L, 195);

        float open = c.lidOpen();
        boolean freed = c.freedT > 0f;

        float cx = L.w / 2f;
        float cy = L.h * 0.46f;
        // Half-extents. A steamer basket is wide but not a bar: roughly 3:1.
        float bw = Math.min(L.w * 0.30f, s * 7.0f);
        float bh = s * 3.4f;

        p.text(freed ? "FREE!" : "FREE THE DUMPLING", cx, L.h * 0.235f,
                s * (freed ? 1.5f : 0.95f), freed ? GOLD : INK, Painter.CENTER, true);
        if (!freed) {
            p.text("MASH ANY KEY", cx, L.h * 0.235f + s * 1.2f, s * 0.62f, INK_DIM,
                    Painter.CENTER, false);
        }

        // The dumpling: rainbow, and cheerier the closer it is to getting out.
        float dumpR = bh * 0.72f;
        float dumpY = cy - bh * 0.10f;
        if (freed) {
            // Escaping: rises and grows away as the celebration plays.
            float t = 1f - c.freedT / 1.7f;
            dumpY -= t * t * L.h * 0.30f;
            dumpR *= 1f + 0.35f * t;
        }
        int rainbow = Glyph.cycle(c.clock * 0.5f);
        // Rays only once it is out: behind a closed lid they just show through the gap.
        if (freed) {
            for (int k = 3; k >= 1; k--) {
                p.fillPoly(star(cx, dumpY, dumpR * (1.4f + 0.7f * k), dumpR * 0.5f, 8,
                        c.clock * 0.6f), Glyph.withAlpha(rainbow, 40 / k));
            }
        }
        Kawaii.moodDumpling(p, cx, dumpY, dumpR, rainbow,
                freed ? 1f : 0.15f + 0.55f * open, 1f + 0.06f * (float) Math.sin(c.clock * 4f));

        if (!freed) {
            // Basket body, over the dumpling's lower half so it reads as contained.
            int body = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f), c.steamerFlash * 0.85f);
            float bodyCy = cy + bh * 0.42f, bodyH = bh * 0.60f;
            p.fillPoly(pill(cx, bodyCy, bw, bodyH, 10), body);
            p.fillPoly(pill(cx, bodyCy, bw, bodyH, 10),
                    Glyph.withAlpha(0xFF000000, (int) (30 * (1f - c.steamerFlash))));
            // Woven slats.
            for (int k = -1; k <= 1; k++) {
                p.fillPoly(pill(cx, bodyCy + k * bh * 0.28f, bw * 0.92f, bh * 0.045f, 6),
                        Glyph.withAlpha(BAMBOO_DARK, 120));
            }

            // Lid: lifts with progress, and kicks up further on each press. Capped so that
            // at full open it just clears the rim rather than floating away from it.
            float lift = open * bh * 1.0f + c.lidPulse * bh * 0.28f;
            float lidY = cy - bh * 0.52f - lift;
            int lidCol = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f + 0.3f),
                    c.steamerFlash * 0.85f);
            p.fillPoly(pill(cx, lidY, bw * 1.05f, bh * 0.26f, 10), lidCol);
            p.fillPoly(pill(cx, lidY - bh * 0.20f, bw * 0.20f, bh * 0.09f, 8), lidCol);
            for (int k = -1; k <= 1; k += 2) {
                p.fillPoly(pill(cx + k * bw * 0.58f, lidY, bw * 0.24f, bh * 0.07f, 6),
                        Glyph.withAlpha(BAMBOO_DARK, 110));
            }

            // Steam escaping through the widening gap.
            if (open > 0.05f) {
                for (int k = 0; k < 4; k++) {
                    float wob = (float) Math.sin(c.clock * 2.2f + k * 1.7f);
                    float sx2 = cx + (k - 1.5f) * bw * 0.34f + wob * s * 0.25f;
                    float sy2 = lidY - bh * 0.4f - open * s * (0.6f + 0.5f * k);
                    p.fillPoly(pill(sx2, sy2, s * 0.34f * open, s * 0.11f * open, 6),
                            Glyph.withAlpha(INK, (int) (70 * open)));
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
                        i < c.steamerHits ? rainbow : Glyph.withAlpha(INK, 45));
            }
            p.text(c.steamerHits + " / " + GameCore.STEAMER_HITS, cx,
                    rowY + gap * 1.15f + s * 1.5f, s * 0.62f, INK_DIM, Painter.CENTER, true);
        } else {
            p.text("+" + GameCore.FREE_BONUS, cx, L.h * 0.63f, s * 1.1f, GOLD,
                    Painter.CENTER, true);
        }

        handLabels(p, L, L.deckTop - s * 0.45f);
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
