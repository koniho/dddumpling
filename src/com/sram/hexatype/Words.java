package com.sram.hexatype;

import java.util.Random;

/**
 * Word generation. Split out of {@link GameCore} because the press-budget rules that keep a
 * word inside MAX_PRESSES are fiddly enough to want reading on their own.
 */
final class Words {

    private Words() {}

    /**
     * Builds a fresh word for the given stage: a random glyph per tile, then a press budget
     * spent on stacks so the total can never exceed {@link GameCore#MAX_PRESSES}.
     */
    static void fill(GameCore.Enemy e, int len, float stackChance, Random rnd) {
        e.word = new int[len];
        e.need = new int[len];
        e.gone = new boolean[len];
        e.goneT = new float[len];
        e.goneDx = new float[len];
        e.goneDy = new float[len];
        for (int i = 0; i < len; i++) {
            e.word[i] = rnd.nextInt(Glyph.COUNT);
            e.need[i] = 1;
        }
        int budget = GameCore.MAX_PRESSES - len;
        for (int i = 0; i < len && budget > 0; i++) {
            if (rnd.nextFloat() >= stackChance) continue;
            int extra = 1 + rnd.nextInt(Math.min(3, budget));
            e.need[i] += extra;
            budget -= extra;
        }
        e.pos = 0;
        e.done = 0;
    }
}
