package com.sram.hexatype;

import java.util.Random;

/** The adaptive keyboard cast. Cat and Grapes are the two advanced keys. */
final class Roster {
    static final int[] STARTERS = {0, 1, 4, 5};
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
}
