package com.sram.hexatype;

/**
 * All screen geometry, derived once per size change. Pure Java so the offline preview
 * harness lays out identically to the device.
 *
 * The six keys sit in two honeycomb chevrons — three under the left thumb, three under
 * the right. Within a chevron the middle hex is raised by sqrt(3)/2*r and shifted by
 * 1.5r, which is exactly hex-grid adjacency, so the three tiles interlock with no gaps
 * while tracing the arc a thumb naturally sweeps.
 */
final class Layout {
    float w, h;
    float padT, padB, padL, padR;

    /** Key hex radius (half-width). Hex height is sqrt(3)*r. */
    float keyR;
    final float[] keyX = new float[Glyph.COUNT];
    final float[] keyY = new float[Glyph.COUNT];
    /** Topmost pixel of the key clusters. */
    float keyTop;
    /** Where the key deck's background band starts, and the bottom edge of the sky. */
    float deckTop;

    /** Enemies that cross this line cost a life. */
    float dangerY;

    float playTop, playLeft, playRight;
    /** Radius of one glyph tile in an attacking word. */
    float enemyR;
    /** Horizontal centre-to-centre spacing of tiles within a word. */
    float enemyStep;

    /** The next-to-type tile is drawn larger; the rest are slightly smaller. */
    static final float HEAD_SCALE = 1.08f;
    static final float TILE_SCALE = 0.94f;

    float hudY;
    /** Top edge that content must stay below, even when insets report zero. */
    float topSafe;
    /** Base text size; all type is a multiple of this. */
    float unit;

    static final float SQ3_2 = 0.8660254f;

    void compute(float w, float h, float padL, float padT, float padR, float padB) {
        this.w = w; this.h = h;
        this.padL = padL; this.padT = padT; this.padR = padR; this.padB = padB;

        float sideMargin = 0.035f * w;
        float centreGap = 0.07f * w;
        float span = w - padL - padR - 2 * sideMargin - centreGap;
        keyR = Math.min(span / 10f, 0.085f * h);

        float bottomMargin = Math.min(0.035f * h, 0.10f * w);
        float yLow = h - padB - bottomMargin - SQ3_2 * keyR;

        float leftEdge = padL + sideMargin;
        float rightEdge = w - padR - sideMargin;

        // Left chevron: low, high, low walking inward.
        keyX[0] = leftEdge + keyR;
        keyX[1] = leftEdge + 2.5f * keyR;
        keyX[2] = leftEdge + 4f * keyR;
        keyY[0] = yLow;
        keyY[1] = yLow - SQ3_2 * keyR;
        keyY[2] = yLow;

        // Right chevron: mirrored, so both thumbs get the same shape.
        keyX[5] = rightEdge - keyR;
        keyX[4] = rightEdge - 2.5f * keyR;
        keyX[3] = rightEdge - 4f * keyR;
        keyY[3] = yLow;
        keyY[4] = yLow - SQ3_2 * keyR;
        keyY[5] = yLow;

        keyTop = yLow - 2f * SQ3_2 * keyR;
        deckTop = keyTop - 0.02f * h;
        dangerY = keyTop - 0.05f * h;

        unit = 0.042f * w;
        // Immersive mode reports no system-bar inset, and some devices report no cutout
        // inset either, so padT can be 0 — which would put the centred HUD directly under
        // a punch-hole camera. Keep a floor regardless of what the insets claim.
        topSafe = Math.max(padT, 0.045f * h);
        hudY = topSafe + 0.024f * h + unit;
        playTop = topSafe + 0.058f * h;
        playLeft = padL + 0.02f * w;
        playRight = w - padR - 0.02f * w;

        enemyR = 0.052f * w;
        // Wide enough that the enlarged head tile clears its neighbour instead of overlapping it.
        enemyStep = (HEAD_SCALE + TILE_SCALE + 0.22f) * enemyR;
    }

    /**
     * Total width of a word of {@code n} tiles. Sized for a head tile at either end,
     * because the head advances through the word while the row stays put.
     */
    float wordWidth(int n) {
        return (n - 1) * enemyStep + 2f * HEAD_SCALE * enemyR;
    }

    /**
     * True inside the stage readout in the HUD, which opens settings. Sized generously —
     * the text itself is small, and a mis-tap here costs nothing.
     */
    boolean inStageTap(float x, float y) {
        if (!BuildFlags.DEVELOPER) return false;
        return Math.abs(x - w / 2f) <= 0.24f * w
                && y >= topSafe - 0.01f * h
                && y <= hudY + 0.6f * unit;
    }

    /**
     * True in the band an upward panic swipe may start in: from the middle of the screen down to
     * the top of the key deck.
     *
     * Far larger than the lit strip that advertises it, deliberately. The strip alone — the sliver
     * between the danger line and the deck — was about a thirtieth of the screen and a thumb coming
     * up off a key overshot it constantly, so a gesture meant for the worst moment in the game was
     * the hardest one to land. The strip stays where it is as the *target*; this is the catchment
     * around it. See {@code Renderer.pushHint}, which is not widened to match: a hint the size of
     * half the screen is not a hint.
     *
     * Nothing else in play claims a touch in here — the keys are all below {@link #deckTop} and the
     * settings tap is up at the HUD — so widening it takes nothing away. The one gesture it shares
     * the field with is the FLING blade, and {@code GameView} runs the blade first for that reason.
     */
    boolean inPushZone(float x, float y) {
        return y >= h / 2f && y <= deckTop;
    }

    /** Index of the key hex containing x,y, or -1. */
    int keyAt(float x, float y) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < Glyph.COUNT; i++) {
            float dx = x - keyX[i], dy = y - keyY[i];
            float d = dx * dx + dy * dy;
            // Cheap inclusive test: the circumscribed circle, nearest centre wins. Slightly
            // generous at the hex corners, which is the forgiving direction for thumbs.
            if (d <= keyR * keyR * 1.10f && d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
