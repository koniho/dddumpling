package com.dddumpling.game;

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

    /** Destroy a word, then demonstrate touching a drifting powerup before the next loop. */
    static final float POWER_TOUCH = 2.8f, POWER_APPROACH = 1.4f;
    static final float LOOP = 15.5f;

    /** Letters in the demo word. Three is enough to show "left to right" and fits the band. */
    static final int LEN = 3;

    /** Absolute seconds for acquire, press and projectile launch in each deliberately separate turn. */
    static final float[] ACQUIRE = {1.25f, 4.05f, 6.85f};
    static final float[] PRESS = {1.95f, 4.75f, 7.55f};
    static final float FIRE_DELAY = 0.32f;
    /**
     * How long a key stays lit after its letter is struck. Longer than {@link #SHOT}, so the key is
     * still lit when its bullet lands — in play {@code keyPress} decays slower than a shot flies,
     * and a key that goes dark mid-flight breaks the very link the demo exists to draw.
     */
    private static final float LIT = 0.42f;

    /**
     * How long the demo's bullet is in the air: twice as long as a real one.
     *
     * Deliberately not {@link GameCore#SHOT_TIME}. In play you fired the shot, so you know where
     * it went and 0.13s is plenty; on the title screen you are watching somebody else play, and at
     * the real speed the bullet is a flicker between a key lighting and a letter going. Slowed
     * down, the deck and the word are visibly connected, which is the whole point of the demo.
     */
    static final float SHOT = 0.78f;
    static final float POWER_START = PRESS[LEN - 1] + FIRE_DELAY + SHOT
            + GameCore.DESTROY_TIME + 0.40f;

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
    private static final float ENTER = 0.85f;

    /** Where the word falls between, as fractions of view height. */
    private static final float FROM = 0.320f, TO = 0.440f;

    /** 0..1 through the current loop. */
    private static float phase(GameCore c) {
        float t = c.clock % LOOP;
        return t / LOOP;
    }

    static float loopTime(GameCore c) { return c.clock % LOOP; }

    /** Which turn of the loop this is, so successive words differ. */
    private static int round(GameCore c) {
        return (int) (c.clock / LOOP);
    }

    /**
     * The demo word's letters. Walked off the round number rather than drawn from the RNG: the
     * title screen must not consume the generator that the first wave's words come out of.
     */
    static int letter(GameCore c, int i) {
        int ordinal = (int) (hash(round(c) * 31 + i * 7) * Roster.count(c.fullRoster))
                % Roster.count(c.fullRoster);
        return Roster.at(c.fullRoster, ordinal);
    }

    /** How many of its letters have been struck by now. */
    private static int struck(GameCore c) {
        float t = loopTime(c);
        int n = 0;
        for (int i = 0; i < LEN; i++) if (t >= impactAt(i)) n++;
        return n;
    }

    static float fireAt(int i) { return PRESS[i] + FIRE_DELAY; }
    static float impactAt(int i) { return fireAt(i) + SHOT; }

    /**
     * The field caret normally keeps state in {@link GameCore}. The demo enemy is deliberately
     * throwaway, so reproduce that same 17/s easing from the destroyed tile to the next one as a
     * pure clock function. That keeps frozen preview frames deterministic without making the
     * chevron snap whenever a new temporary Enemy object is built.
     */
    static float caretX(GameCore c, GameCore.Enemy e, Layout L) {
        int done = struck(c);
        if (done <= 0) return c.tileX(e, 0, L);
        if (done >= LEN) return c.tileX(e, LEN - 1, L);
        float since = Math.max(0f, loopTime(c) - impactAt(done - 1));
        float ease = 1f - (float) Math.exp(-17f * since);
        ease = Math.max(0f, Math.min(1f, ease));
        return c.tileX(e, done - 1, L)
                + (c.tileX(e, done, L) - c.tileX(e, done - 1, L)) * ease;
    }

    /** The key being deliberately identified before the visibly separate press. */
    static int hintKey(GameCore c) {
        float t = loopTime(c);
        for (int i = 0; i < LEN; i++)
            if (t >= ACQUIRE[i] && t < PRESS[i]) return letter(c, i);
        return -1;
    }

    static float hintAmount(GameCore c) {
        float t = loopTime(c);
        for (int i = 0; i < LEN; i++) {
            if (t < ACQUIRE[i] || t >= PRESS[i]) continue;
            float u = (t - ACQUIRE[i]) / (PRESS[i] - ACQUIRE[i]);
            return 0.55f + 0.20f * (float) Math.sin(u * Math.PI * 3f);
        }
        return 0f;
    }

    /**
     * The key the demo is pressing right now, or -1. {@link Renderer#keys} lights it, which is the
     * half of the lesson that says the deck is what you touch.
     */
    static int litKey(GameCore c) {
        float t = loopTime(c);
        for (int i = LEN - 1; i >= 0; i--) {
            if (t >= PRESS[i] && t < PRESS[i] + LIT) return letter(c, i);
        }
        return -1;
    }

    /** 1 right as that key is struck, decaying, so it flashes rather than switching on. */
    static float litAmount(GameCore c) {
        float t = loopTime(c);
        for (int i = LEN - 1; i >= 0; i--) {
            float since = t - PRESS[i];
            if (since >= 0f && since < LIT) return 1f - since / LIT;
        }
        return 0f;
    }

    /**
     * Seconds since the last letter was struck, so negative while the word is still being typed.
     * The whole tail of the loop is measured off this: the finishing bullet's flight, the fly-apart
     * it triggers when it lands, and the empty beat after.
     */
    private static float sinceLast(GameCore c) {
        return loopTime(c) - impactAt(LEN - 1);
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
        float t = loopTime(c);
        if (t >= POWER_START) {
            powerLesson(p, c, L, fade);
            return;
        }
        float u = t / LOOP;
        int done = struck(c);
        float last = sinceLast(c);
        // Gone for the last part of the loop: the band sits empty for a beat, which is what makes
        // it read as a round rather than a treadmill.
        if (last > GameCore.DESTROY_TIME + 0.85f) return;

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
        e.y = L.h * (FROM + (TO - FROM) * Math.min(1f, t / impactAt(LEN - 1)));
        e.enterT = Math.min(1f, u * LOOP / ENTER);

        if (last >= 0f) {
            e.destroyed = true;
            e.destroyT = last;
            c.computeFlyDirs(e, L);
        }

        // Renderer.enemy reads target and the stateful field caret. Give its temporary enemy an
        // analytical caret position so it visibly glides onward when each projectile lands.
        GameCore.Enemy was = c.target;
        GameCore.Enemy wasCaretOwner = c.caretOwner;
        float wasCaretX = c.caretX;
        boolean lessonActive = t >= ACQUIRE[0] && t < impactAt(LEN - 1);
        c.target = lessonActive ? e : null;
        if (lessonActive) {
            c.caretOwner = e;
            c.caretX = caretX(c, e, L);
        }
        Renderer.enemy(p, c, L, e);
        c.target = was;
        c.caretOwner = wasCaretOwner;
        c.caretX = wasCaretX;

        // The bullet each press put in the air, drawn over the deck it came off. Aimed at the tile
        // rather than at where the tile was when the key was struck: a real shot homes in on a word
        // that is still falling, and the demo borrows the same drawing to do the same thing.
        for (int i = 0; i < LEN; i++) {
            float since = t - fireAt(i);
            if (since < 0f || since > SHOT) continue;
            int g = letter(c, i);
            Renderer.bullet(p, c, L, c.keyX(L, g), c.keyY(L, g), c.tileX(e, i, L), e.y,
                    since / SHOT, g, fade);
        }
    }
    static Power lessonPower(GameCore c, Layout L) {
        float age = loopTime(c) - POWER_START;
        if (age < 0f || age >= POWER_TOUCH + Power.POP_TIME) return null;
        Power power = new Power();
        power.effect = Power.FLING;
        float travel = Math.min(1f, age / POWER_TOUCH);
        power.x = -L.enemyR * 2f + (L.w * 0.70f + L.enemyR * 2f) * travel;
        power.y = L.h * 0.395f;
        power.t = Math.min(age, POWER_TOUCH);
        power.hit = age >= POWER_TOUCH;
        power.hitT = Math.max(0f, age - POWER_TOUCH);
        return power;
    }

    private static void powerLesson(Painter p, GameCore c, Layout L, float fade) {
        Power power = lessonPower(c, L);
        if (power == null) return;
        Renderer.powerup(p, c, L, power, fade);
        float age = loopTime(c) - POWER_START;
        if (age < POWER_APPROACH) return;
        float approach = Math.min(1f, (age - POWER_APPROACH) / (POWER_TOUCH - POWER_APPROACH));
        float ease = approach * approach * (3f - 2f * approach);
        float tipY = power.y + (float) Math.sin(power.t * 3.2f) * L.enemyR * 0.22f;
        float x = power.x + (1f - ease) * L.unit * 3f;
        float y = tipY + (1f - ease) * L.unit;
        float alpha = fade * Math.min(1f, (age - POWER_APPROACH) / 0.25f);
        if (power.hit) alpha *= 1f - power.hitT / Power.POP_TIME;
        Renderer.touchHint(p, x, y, L.enemyR * 1.05f, 1.10f, alpha, c.clock);
    }

}
