package com.sram.hexatype;

import java.util.Random;

/** The adaptive keyboard cast. Cat and Grapes are the two advanced keys. */
final class Roster {
    static final int[] STARTERS = {0, 1, 4, 5};
    static final float STARTER_SCALE = 1.14f;
    private Roster() {}
    static boolean active(boolean full, int glyph) {
        return glyph >= 0 && glyph < Glyph.COUNT && (full || glyph != 2 && glyph != 3);
    }
    static int count(boolean full) { return full ? Glyph.COUNT : STARTERS.length; }
    static int at(boolean full, int ordinal) { return full ? ordinal : STARTERS[ordinal]; }
    static int ordinal(boolean full, int glyph) {
        if (full) return glyph;
        for (int i = 0; i < STARTERS.length; i++) if (STARTERS[i] == glyph) return i;
        return -1;
    }
    static int random(boolean full, Random rnd) { return at(full, rnd.nextInt(count(full))); }
    static int randomExcept(boolean full, int avoid, Random rnd) {
        int n = count(full) - (active(full, avoid) ? 1 : 0);
        int pick = rnd.nextInt(n);
        for (int i = 0; i < count(full); i++) {
            int glyph = at(full, i);
            if (glyph == avoid) continue;
            if (pick-- == 0) return glyph;
        }
        return at(full, 0);
    }

    /** Key centre while the four-key deck expands and contracts into the six-key deck. */
    static float keyX(Layout L, int glyph, float fullMix) {
        int anchor = glyph == 1 ? 0 : glyph == 4 ? 5 : glyph;
        float expanded = L.keyX[anchor]
                + (L.keyX[glyph] - L.keyX[anchor]) * STARTER_SCALE;
        return expanded + (L.keyX[glyph] - expanded) * fullMix;
    }

    static float keyY(Layout L, int glyph, float fullMix) {
        int anchor = glyph == 1 ? 0 : glyph == 4 ? 5 : glyph;
        float expanded = L.keyY[anchor]
                + (L.keyY[glyph] - L.keyY[anchor]) * STARTER_SCALE;
        return expanded + (L.keyY[glyph] - expanded) * fullMix;
    }
}
