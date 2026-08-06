package com.sram.hexatype;

/**
 * The title screen playing itself: one word falls, and its letters clear one at a time while the
 * matching keys light up under them. It replaced the two lines that used to explain the game in
 * words — a sentence about tapping the matching hex is a worse teacher than three seconds of
 * watching it happen.
 *
 * It builds a throwaway {@link GameCore.Enemy} and hands it to {@link Renderer#enemy}, so what is
 * on screen is drawn by exactly the code that draws a real word: head tile, cleared letters,
 * entrance, caret and all. A second, simplified word-drawer would have been the thing that
 * eventually disagreed with the real one.
 *
 * Nothing here touches game state. Everything is a function of the clock, so the loop is the same
 * every time round and a frozen frame is reproducible.
 */
final class Demo extends Draw {

    private Demo() {}

    /** One turn of the loop: fall, three presses, a beat of nothing, repeat. */
    static final float LOOP = 4.2f;

    /** Letters in the demo word. Three is enough to show "left to right" and fits the band. */
    static final int LEN = 3;

    /** When each letter is struck, as a fraction of the loop. */
    private static final float[] PRESS = {0.42f, 0.56f, 0.70f};
    /** How long a key stays lit after its letter is struck. */
    private static final float LIT = 0.22f;

    /** Where the word falls between, as fractions of view height. */
    private static final float FROM = 0.215f, TO = 0.335f;

    /** 0..1 through the current loop. */
    private static float phase(GameCore c) {
        float t = c.clock % LOOP;
        return t / LOOP;
    }

    /** Which turn of the loop this is, so successive words differ. */
    private static int round(GameCore c) {
        return (int) (c.clock / LOOP);
    }

    /**
     * The demo word's letters. Walked off the round number rather than drawn from the RNG: the
     * title screen must not consume the generator that the first wave's words come out of.
     */
    private static int letter(GameCore c, int i) {
        return (int) (hash(round(c) * 31 + i * 7) * Glyph.COUNT) % Glyph.COUNT;
    }

    /** How many of its letters have been struck by now. */
    private static int struck(GameCore c) {
        float u = phase(c);
        int n = 0;
        for (int i = 0; i < LEN; i++) if (u >= PRESS[i]) n++;
        return n;
    }

    /**
     * The key the demo is pressing right now, or -1. {@link Renderer#keys} lights it, which is the
     * half of the lesson that says the deck is what you touch.
     */
    static int litKey(GameCore c) {
        float u = phase(c);
        for (int i = LEN - 1; i >= 0; i--) {
            if (u >= PRESS[i] && u < PRESS[i] + LIT / LOOP) return letter(c, i);
        }
        return -1;
    }

    /** 1 right as that key is struck, decaying, so it flashes rather than switching on. */
    static float litAmount(GameCore c) {
        float u = phase(c);
        for (int i = LEN - 1; i >= 0; i--) {
            float since = u - PRESS[i];
            if (since >= 0f && since < LIT / LOOP) return 1f - since * LOOP / LIT;
        }
        return 0f;
    }

    /**
     * Draws the falling word. Gone for the last part of the loop: the word is cleared and the band
     * sits empty for a beat, which is what makes it read as a round rather than a treadmill.
     *
     * @param fade 0..1, so it leaves with the rest of the title screen
     */
    static void draw(Painter p, GameCore c, Layout L, float fade) {
        if (fade <= 0.004f) return;
        float u = phase(c);
        int done = struck(c);
        if (done >= LEN) return;

        GameCore.Enemy e = new GameCore.Enemy();
        e.word = new int[LEN];
        e.need = new int[LEN];
        e.gone = new boolean[LEN];
        e.goneT = new float[LEN];
        e.goneDx = new float[LEN];
        e.goneDy = new float[LEN];
        for (int i = 0; i < LEN; i++) {
            e.word[i] = letter(c, i);
            e.need[i] = 1;
        }
        e.pos = done;
        e.baseX = (L.playLeft + L.playRight) / 2f;
        e.sway = 0f;
        // Falls over the whole loop rather than stopping when the last letter lands, so the word
        // is still drifting down as it is cleared — which is what really happens.
        e.y = L.h * (FROM + (TO - FROM) * Math.min(1f, u / PRESS[LEN - 1]));
        e.enterT = 1f;

        // Renderer.enemy reads target for the lock ring and the caret; the demo is always locked
        // on, because a word being typed is the thing being demonstrated.
        GameCore.Enemy was = c.target;
        c.target = e;
        Renderer.enemy(p, c, L, e);
        c.target = was;
    }
}
