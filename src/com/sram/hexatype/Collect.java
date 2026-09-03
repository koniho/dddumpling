package com.sram.hexatype;

import java.util.Random;

/**
 * The thirty collectible squishies — the catalogue, the blind-box odds, and the owned-set
 * bitmask that outlives a run.
 *
 * Three families, modelled on the real blind-box squishy scene: mystery dumplings (the bao
 * buns, with their glitter, holo and golden-ticket chases), squishy fruits, and squeeze
 * globs. Every entry here is described only by shape, finish, tier and colour — no brand
 * names and no artwork — because {@link Trinket} draws all thirty procedurally, and the
 * repo's licensing line depends on there being nothing to license.
 *
 * The table is parallel arrays rather than an object per entry: thirty of anything is a lot
 * of allocation for data that never changes, and a row reads across in one glance. A test
 * asserts every array is exactly {@link #COUNT} long.
 */
final class Collect {

    static final int BLIND_COUNT = 30, STAR_FIRST = 30, STAR_COUNT = 5,
            CUBE_FIRST = 35, CUBE_COUNT = 10, COUNT = 45;

    // ---- families -----------------------------------------------------------
    static final int DUMPLINGS = 0, FRUITS = 1, GLOBS = 2, STARLINGS = 3, GEL_CUBES = 4;
    static final String[] FAMILY_NAME = {"MYSTERY DUMPLINGS", "SQUISHY FRUITS",
            "SQUEEZE GLOBS", "STARLINGS", "GELATINOUS CUBES"};

    // ---- rarity tiers -------------------------------------------------------
    static final int COMMON = 0, UNCOMMON = 1, RARE = 2, CHASE = 3, GRAIL = 4, CUBE_TIER = 5;
    static final String[] TIER_NAME = {"COMMON", "UNCOMMON", "RARE", "CHASE", "GRAIL", "CUBE"};
    /** Frame and label colour per tier, climbing from plain to gold. */
    static final int[] TIER_COLOR = {
        0xFFA79DCC,   // common   - the dim ink
        0xFF9AE9C0,   // uncommon - mint
        0xFF93D6F7,   // rare     - sky
        0xFFC3A8F5,   // chase    - grape
        0xFFFFCE4A,   // grail    - gold
        0xFF75E6B1,   // cube     - slime mint
    };
    /**
     * Relative odds of one entry of that tier. Steep on purpose: a grail is forty times
     * less likely than any single common, which is what makes the case worth filling.
     */
    static final int[] TIER_WEIGHT = {40, 16, 6, 2, 1, 0};

    // ---- shapes -------------------------------------------------------------
    static final int BAO = 0, BUN = 1, SHELL = 2, FIN = 3, CRESCENT = 4, WEDGE = 5,
            CLUSTER = 6, POME = 7, CITRUS = 8, GLOB = 9, CUBE = 10, GUM = 11, RING = 12,
            CONE = 13, DROP = 14, STAR = 15, GEL_CUBE = 16;
    static final int SHAPE_COUNT = 17;

    // ---- finishes -----------------------------------------------------------
    static final int MATTE = 0, GLITTER = 1, HOLO = 2, GALAXY = 3, METALLIC = 4, CLEAR = 5,
            GLOW = 6, TIEDYE = 7, CONFETTI = 8;
    static final int FINISH_COUNT = 9;

    static final String[] NAME = {
        // Mystery dumplings.
        "CREAM BAO", "PEACH BUN", "TIE DYE BAO", "SHERBET BAO", "CONFETTI BUN",
        "SNOW BUN", "PINK GLITTER", "APPLE HOLO", "PURPLE HOLO", "SEASHELL BAO",
        "FIN BAO", "GALAXY BAO", "GOLDEN TICKET",
        // Squishy fruits.
        "NANA", "MELON WEDGE", "CHERRY PAIR", "PEAR DROP", "MOCHI PEACH",
        "ORANGE POP", "GLITTER GRAPE", "LEMON CHROME", "RAINBOW MELON",
        // Squeeze globs.
        "GROOVY GLOB", "NICE CUBE", "GUMDROP", "DOHNUT", "NICE CREAM",
        "MARBLE GLOB", "DREAM DROP", "GLOW GLOB",
        "NOVA NIBBLE", "COMET CUB", "MOONSPARK", "AURORA STAR", "WISHKEEPER",
        "LIME LIMBO", "BERRY BLOCK", "MINT MATRIX", "PEACH PRISM", "COLA CUBIE",
        "GRAPE GLITCH", "AQUA WOBBLE", "SUNSET SLAB", "ROYAL GEL", "JELLO JULEP",
    };

    static final int[] FAMILY = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    };

    static final int[] SHAPE = {
        BAO, BUN, BAO, BAO, BUN, BUN, BAO, BUN, BAO, SHELL, FIN, BAO, BAO,
        CRESCENT, WEDGE, CLUSTER, POME, POME, CITRUS, CLUSTER, CITRUS, WEDGE,
        GLOB, CUBE, GUM, RING, CONE, GLOB, DROP, GLOB,
        STAR, STAR, STAR, STAR, STAR,
        GEL_CUBE, GEL_CUBE, GEL_CUBE, GEL_CUBE, GEL_CUBE,
        GEL_CUBE, GEL_CUBE, GEL_CUBE, GEL_CUBE, GEL_CUBE,
    };

    static final int[] FINISH = {
        MATTE, MATTE, TIEDYE, TIEDYE, CONFETTI, CLEAR, GLITTER, HOLO, HOLO, HOLO,
        MATTE, GALAXY, METALLIC,
        MATTE, MATTE, MATTE, MATTE, MATTE, MATTE, GLITTER, METALLIC, CONFETTI,
        MATTE, MATTE, MATTE, MATTE, MATTE, TIEDYE, GLITTER, GLOW,
        MATTE, GLITTER, HOLO, GALAXY, METALLIC,
        CLEAR, GLITTER, MATTE, HOLO, CLEAR, GALAXY, GLOW, TIEDYE, METALLIC, CONFETTI,
    };

    static final int[] TIER = {
        COMMON, COMMON, COMMON, UNCOMMON, UNCOMMON, UNCOMMON, RARE, RARE, RARE, RARE,
        CHASE, CHASE, GRAIL,
        COMMON, COMMON, COMMON, COMMON, UNCOMMON, UNCOMMON, RARE, RARE, CHASE,
        COMMON, COMMON, COMMON, UNCOMMON, UNCOMMON, RARE, RARE, RARE,
        COMMON, UNCOMMON, RARE, CHASE, CHASE,
        CUBE_TIER, CUBE_TIER, CUBE_TIER, CUBE_TIER, CUBE_TIER,
        CUBE_TIER, CUBE_TIER, CUBE_TIER, CUBE_TIER, CUBE_TIER,
    };

    /** Body fill. */
    static final int[] BODY = {
        0xFFFFE9C4, 0xFFFFC7A8, 0xFFFF9EC4, 0xFFFFB073, 0xFFFDF6EE,
        0xFFDFF3FF, 0xFFFF8FC0, 0xFF8FE0A8, 0xFFB79BF0, 0xFFFFE0EA,
        0xFF9FB6CC, 0xFF6A5AC0, 0xFFFFC93A,
        0xFFFFE066, 0xFFFF7C8E, 0xFFE8455F, 0xFFCBE06A, 0xFFFFB6C8,
        0xFFFFA53A, 0xFFA88AE8, 0xFFF7E24A, 0xFFFFC0D8,
        0xFF9AE9C0, 0xFF93D6F7, 0xFFFF9EB5, 0xFFE0B07A, 0xFFFFF0D0,
        0xFFC3A8F5, 0xFF8FD0F7, 0xFFC8F79A,
        0xFFFFE36E, 0xFFFFA8D8, 0xFF8FE7FF, 0xFFBBA3FF, 0xFFFFCF4A,
        0xFF9EF27B, 0xFFFF7FB3, 0xFF86E8C4, 0xFFFFB07C, 0xFFB87952,
        0xFFA98AF3, 0xFF79DDF2, 0xFFFF8D72, 0xFFE9D45B, 0xFFFFD36E,
    };

    /**
     * Second colour. Its job depends on the shape and finish: the tie-dye partner, the holo
     * sheen, the galaxy void, the rind or leaf on a fruit, the cone under a scoop.
     */
    static final int[] ACCENT = {
        0xFFFFD08A, 0xFFFF9E7A, 0xFF9AD6F7, 0xFFFFF0C0, 0xFFFF7C9E,
        0xFFFFFFFF, 0xFFFFE0F0, 0xFFD8FFE8, 0xFFEADFFF, 0xFFFFF6FA,
        0xFF5D7590, 0xFF1A1436, 0xFFFFF0A8,
        0xFF8A6A2A, 0xFF7ED08A, 0xFF8FD9A0, 0xFF8FD9A0, 0xFF8FD9A0,
        0xFFFFD9A0, 0xFFE8DCFF, 0xFFFFFBD0, 0xFF9AE9C0,
        0xFFD8FFE8, 0xFFD8F2FF, 0xFFFFD8E2, 0xFFFF9EC4, 0xFFD9A86E,
        0xFF9AE9C0, 0xFFE8F8FF, 0xFFF0FFD8,
        0xFFFFF4B0, 0xFFFFE5F4, 0xFFE5FAFF, 0xFF34245E, 0xFFFFFFFF,
        0xFFDFFFF0, 0xFFFFD8EA, 0xFFCFFFF0, 0xFFFFE1CC, 0xFFF1C7A8,
        0xFFE1D8FF, 0xFFD8F8FF, 0xFFFFD65C, 0xFFFFF1A8, 0xFFFFF3C4,
    };

    private Collect() {}

    // ---- the owned set ------------------------------------------------------
    // One bit per entry, so the whole collection is a single long in the store.

    /** Every valid bit. Guards against a store handing back junk in the high bits. */
    static final long MASK = (1L << COUNT) - 1L;

    static boolean has(long owned, int i) {
        return i >= 0 && i < COUNT && (owned & (1L << i)) != 0L;
    }

    static long add(long owned, int i) {
        return i >= 0 && i < COUNT ? owned | (1L << i) : owned;
    }

    static int owned(long owned) {
        return Long.bitCount(owned & MASK);
    }

    /** True once every entry is in the case. */
    static boolean complete(long owned) {
        return (owned & MASK) == MASK;
    }

    // ---- the blind box ------------------------------------------------------

    /**
     * Re-rolls allowed while the result is something already owned. This biases toward
     * entries still missing without touching the tier weights themselves, so early opens
     * nearly always advance the case while a nearly-full case starts handing out
     * duplicates — which is how a blind box is supposed to feel.
     */
    private static final int REROLLS = 3;

    /** Sum of every entry's weight, so {@link #weighted} needs one pass, not two. */
    private static final int TOTAL_WEIGHT = totalWeight();

    private static int totalWeight() {
        int n = 0;
        for (int i = 0; i < BLIND_COUNT; i++) n += TIER_WEIGHT[TIER[i]];
        return n;
    }

    /** One draw at the raw tier odds, ignoring what is already owned. */
    private static int weighted(Random rnd) {
        int r = rnd.nextInt(TOTAL_WEIGHT);
        for (int i = 0; i < BLIND_COUNT; i++) {
            r -= TIER_WEIGHT[TIER[i]];
            if (r < 0) return i;
        }
        return BLIND_COUNT - 1;   // unreachable while the weights are positive
    }

    /** Opens a box: what the player just won, new or duplicate. */
    static int roll(Random rnd, long owned) {
        int pick = weighted(rnd);
        for (int t = 0; t < REROLLS && has(owned, pick); t++) pick = weighted(rnd);
        return pick;
    }

    static int rollStar(Random rnd, long owned) {
        int pick = STAR_FIRST + rnd.nextInt(STAR_COUNT);
        for (int t = 0; t < REROLLS && has(owned, pick); t++)
            pick = STAR_FIRST + rnd.nextInt(STAR_COUNT);
        return pick;
    }

    static int rollCube(Random rnd, long owned) {
        int pick = CUBE_FIRST + rnd.nextInt(CUBE_COUNT);
        for (int t = 0; t < REROLLS && has(owned, pick); t++)
            pick = CUBE_FIRST + rnd.nextInt(CUBE_COUNT);
        return pick;
    }

    // ---- catalogue rules ----------------------------------------------------

    /**
     * True for finishes drawn as bands across the body. {@link Trinket} fits those to an
     * ellipse, so they only sit cleanly on a shape that roughly fills one — there is no way
     * to clip a fill to an arbitrary silhouette through {@link Painter}. A test holds the
     * catalogue to it.
     */
    static boolean banded(int finish) {
        return finish == HOLO || finish == GALAXY || finish == METALLIC || finish == CLEAR
                || finish == TIEDYE;
    }

    /** True for shapes that fill enough of their radius for a banded finish to look right. */
    static boolean roundish(int shape) {
        return shape == BAO || shape == BUN || shape == SHELL || shape == CITRUS
                || shape == GLOB || shape == CUBE || shape == STAR || shape == GEL_CUBE;
    }
}
