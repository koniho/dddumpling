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

    /**
     * One turn of the loop: arrive, three presses, fly apart, a beat of nothing, repeat.
     *
     * The whole sequence has to finish inside this. The last press lands at 0.70 of it, so the
     * bullet and the fly-apart after it — {@link #SHOT} plus {@link GameCore#DESTROY_TIME} — have
     * the remaining 1.26s to play out in, and leave a beat over.
     */
    static final float LOOP = 4.2f;

    /** Letters in the demo word. Three is enough to show "left to right" and fits the band. */
    static final int LEN = 3;

    /** When each letter is struck, as a fraction of the loop. */
    private static final float[] PRESS = {0.42f, 0.56f, 0.70f};
    /**
     * How long a key stays lit after its letter is struck. Longer than {@link #SHOT}, so the key is
     * still lit when its bullet lands — in play {@code keyPress} decays slower than a shot flies,
     * and a key that goes dark mid-flight breaks the very link the demo exists to draw.
     */
    private static final float LIT = 0.30f;

    /**
     * How long the demo's bullet is in the air: twice as long as a real one.
     *
     * Deliberately not {@link GameCore#SHOT_TIME}. In play you fired the shot, so you know where
     * it went and 0.13s is plenty; on the title screen you are watching somebody else play, and at
     * the real speed the bullet is a flicker between a key lighting and a letter going. Slowed
     * down, the deck and the word are visibly connected, which is the whole point of the demo.
     */
    private static final float SHOT = GameCore.SHOT_TIME * 2f;

    /**
     * How long the word takes to arrive.
     *
     * A real word's entrance is position-driven, off how far it has cleared the top edge — but this
     * one is not falling from anywhere, because the band it types in sits under the title and the
     * BEST line and a word dropping in from above would fall through both. So it fades up where it
     * is, on the loop clock, and everything the real entrance does — the alpha ramp and the swell —
     * follows from {@code enterT} exactly as it does in play. Time-driven here is not the trap it
     * would be on the field: the demo's position is itself a function of this same clock.
     */
    private static final float ENTER = 0.55f;

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
     * Seconds since the last letter was struck, so negative while the word is still being typed.
     * The whole tail of the loop is measured off this: the finishing bullet's flight, the fly-apart
     * it triggers when it lands, and the empty beat after.
     */
    private static float sinceLast(GameCore c) {
        return (phase(c) - PRESS[LEN - 1]) * LOOP;
    }

    /**
     * Draws the word, the bullets it is being shot with, and — once the last one lands — the word
     * coming apart.
     *
     * Every beat of it is the field's own drawing, driven by the same {@link GameCore.Enemy} fields
     * play sets: {@code enterT} fades and swells it in, {@code pos} dims each letter as it is
     * cleared, {@code dying} rings the finishing press, and {@code destroyed} with {@code flyDir}
     * throws the tiles apart. Nothing here is a title-screen imitation of any of it, which is the
     * point — an imitation is the thing that eventually stops matching.
     *
     * @param fade 0..1, so it leaves with the rest of the title screen
     */
    static void draw(Painter p, GameCore c, Layout L, float fade) {
        if (fade <= 0.004f) return;
        float u = phase(c);
        int done = struck(c);
        float last = sinceLast(c);
        // Gone for the last part of the loop: the band sits empty for a beat, which is what makes
        // it read as a round rather than a treadmill.
        if (last > SHOT + GameCore.DESTROY_TIME) return;

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
        e.enterT = Math.min(1f, u * LOOP / ENTER);

        if (last >= 0f) {
            if (last < SHOT) {
                // Typed out, with the finishing bullet still in the air: the tiles stand there
                // dimmed and the ring flashes over them, exactly as they do in play.
                e.dying = true;
                e.deathT = last;
            } else {
                e.destroyed = true;
                e.destroyT = last - SHOT;
                c.computeFlyDirs(e, L);
            }
        }

        // Renderer.enemy reads target for the lock ring and the caret; the demo is always
        // locked on, because a word being typed is the thing being demonstrated.
        GameCore.Enemy was = c.target;
        c.target = e;
        Renderer.enemy(p, c, L, e);
        c.target = was;

        // The bullet each press put in the air, drawn over the deck it came off. Aimed at the tile
        // rather than at where the tile was when the key was struck: a real shot homes in on a word
        // that is still falling, and the demo borrows the same drawing to do the same thing.
        for (int i = 0; i < LEN; i++) {
            float since = (u - PRESS[i]) * LOOP;
            if (since < 0f || since > SHOT) continue;
            int g = letter(c, i);
            Renderer.bullet(p, c, L, L.keyX[g], L.keyY[g], c.tileX(e, i, L), e.y,
                    since / SHOT, g, fade);
        }
    }
}
