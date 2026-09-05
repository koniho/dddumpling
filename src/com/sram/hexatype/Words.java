package com.sram.hexatype;

import java.util.Random;

/**
 * Word generation. Split out of {@link GameCore} because the press-budget rules that keep a
 * word inside MAX_PRESSES are fiddly enough to want reading on their own.
 */
final class Words {

    private Words() {}

    /**
     * Builds a fresh word for the given stage: a press budget spent on stacks so the total can
     * never exceed {@link Pacing#MAX_PRESSES}, then a glyph per tile.
     *
     * Stacks are chosen before letters, which is the opposite of the order this used to run in. A
     * letter cannot be picked until it is known whether it or its neighbour is a stack, because a
     * stack is not allowed to sit next to its own letter.
     */
    static void fill(GameCore.Enemy e, int len, float stackChance, Random rnd) {
        fill(e, len, stackChance, rnd, true);
    }

    static void fill(GameCore.Enemy e, int len, float stackChance, Random rnd, boolean fullRoster) {
        e.word = new int[len];
        e.need = new int[len];
        e.gone = new boolean[len];
        e.goneT = new float[len];
        e.goneDx = new float[len];
        e.goneDy = new float[len];

        for (int i = 0; i < len; i++) e.need[i] = 1;
        int budget = Pacing.MAX_PRESSES - len;
        for (int i = 0; i < len && budget > 0; i++) {
            if (rnd.nextFloat() >= stackChance) continue;
            int extra = 1 + rnd.nextInt(Math.min(3, budget));
            e.need[i] += extra;
            budget -= extra;
        }

        for (int i = 0; i < len; i++) {
            // A stack beside its own letter is unreadable: it is the same key either way, so
            // nothing on screen tells you where the stack ended and the next tile began, and a
            // miscount stays invisible until the word fails. Either side of a stack has to differ
            // from it — checked in both directions here, since the tile before this one may be the
            // stack rather than this one.
            int avoid = i > 0 && (e.need[i] > 1 || e.need[i - 1] > 1) ? e.word[i - 1] : -1;
            if (avoid < 0) {
                e.word[i] = Roster.random(fullRoster, rnd);
            } else {
                // One draw over the other five, so the letter stays uniform and the RNG is
                // consumed at exactly one call per tile either way.
                e.word[i] = Roster.randomExcept(fullRoster, avoid, rnd);
            }
        }

        e.pos = 0;
        e.done = 0;
    }
}
