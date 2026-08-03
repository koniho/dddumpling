package com.sram.hexatype;

/**
 * Background, parallax clouds, the red edge vignette and the band behind the HUD.\n * Everything that sits behind or over the play field without being part of it.
 */
final class Sky extends Draw {

    private Sky() {}


    /** Per-layer cloud tint and opacity, back to front. */
    static final int[] CLOUD_TINT = {0xFF5C5490, 0xFF8478BE, 0xFFC3B7EE};
    // Low, because the soft layers accumulate: the visible density is several times these.
    static final int[] CLOUD_ALPHA = {30, 22, 14};
    /** The one layer drawn over the enemies; the rest go behind. */
    static final int CLOUD_FRONT_LAYER = GameCore.CLOUD_LAYERS - 1;

    /** Depth scale of a cloud layer; nearer layers are bigger. */
    static float cloudScale(int layer) {
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

    static void clouds(Painter p, GameCore c, Layout L, int layer, float hurt) {
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
    static void cloudBand(Painter p, GameCore c, Layout L, int from, int to,
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
    static void cloud(Painter p, float cx, float cy, float w, float h, int tint,
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


    static void vignette(Painter p, Layout L, int color, float strength) {
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
    static void hudBacking(Painter p, Layout L, int bg) {
        int layers = 14;
        int col = Glyph.withAlpha(bg, 26);
        for (int i = 0; i < layers; i++) {
            p.fillRect(0, 0, L.w, L.playTop * (1f - (float) i / layers), col);
        }
    }
}
