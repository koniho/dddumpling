package com.sram.hexatype;

/**
 * The boss fight's wiring: what a press, tap, drag or swipe at the boss is worth in score, sound,
 * shots and lives, and how the stage ends.
 *
 * {@link Boss} owns the rules and answers what an input <em>means</em>; this turns that answer into
 * the rest of the game. Works on {@code GameCore}'s fields rather than owning them, the {@link Fx}
 * seam.
 */
final class BossPlay {

    private BossPlay() {}

    /**
     * The boss is beaten and its burst is spent: pay out and let the stage end.
     *
     * The stage is satisfied by filling the quota it never spawned, which is the same trick
     * {@link #endPower} uses — {@link #stageCleared} then fires on the next frame through the
     * ordinary path, so a boss stage ends by exactly the route every other stage ends by.
     */
    static void endBoss(GameCore c, Layout L) {
        boolean won = c.boss.beaten;
        // Everything still on the field goes with it. A boss dying to a field of three words and
        // then handing you a mopping-up job is an anticlimax, and the interlude is the payoff.
        for (int i = c.enemies.size() - 1; i >= 0; i--) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.destroyed) c.destroyWord(e, c.enemyCentreX(e), e.y, L);
        }
        if (won) {
            c.score += GameCore.BOSS_BONUS;
            // A life back, capped as the steamer's is. A boss costs lives to learn, and a run that
            // beats one should not arrive at the next stage on its last one.
            if (c.lives < GameCore.START_LIVES) c.lives++;
            c.flash = Math.max(c.flash, 1f);
            c.flashColor = GameCore.FLASH_CLEAR;
            c.skyGlow = 1f;
            c.skyGlowColor = GameCore.FLASH_CLEAR;
            c.shake = Math.max(c.shake, 0.8f);
            if (c.sound != null) c.sound.achievement();
        }
        // Sent home before the quota is filled, so nothing that reads bossActive() can see a beaten
        // boss and a satisfied stage at the same time.
        c.boss.leave();
        c.spawnedThisStage = c.stageQuota();
    }

    /**
     * {@link Boss#SUMO} reached the danger line. Costs a life exactly as a word landing does, and
     * for the same reason: it crossed the line.
     */
    static void slam(GameCore c, Layout L) {
        c.shake = Math.max(c.shake, 1f);
        c.takeHit(c.boss.bodyX(L), L);
    }

    /**
     * Whether the boss gets first refusal on a press of {@code g}.
     *
     * Three cases, and the third one is the subtle one:
     *
     * <ul>
     *   <li>A key it is holding — always, even mid-word, because the player does not have that key.
     *   <li>Its own letter while its window is open, with nothing engaged. This is the boss
     *       outranking an unengaged word, on the same terms the drifting powerup gets.
     *   <li>Its own letter while the window is <em>shut</em>, with nothing engaged <em>and</em>
     *       nothing on the field wanting that letter either.
     * </ul>
     *
     * That last clause is what stops a mistimed-press penalty from taxing ordinary play. Without it
     * the boss claimed its letter whenever nothing was engaged, so starting a word that happened to
     * begin with the drum's letter was read as a fumbled beat — and since a landed rebuff resets the
     * drum's beat, typing normally pushed its window away. It showed up as the soak curve inverting:
     * the quick tier finished <em>below</em> the steady one, because faster hands start more words and
     * so trip over the drum's letter more often. Speed should not be a penalty.
     *
     * A deliberate early press is still punished, because with nothing on the field wanting it there
     * is nothing else the press could have meant.
     */
    static boolean claims(GameCore c, int g) {
        if (c.boss.denies(g)) return true;
        if (c.target != null) return false;
        if (c.boss.wants(g)) return true;
        return c.boss.claims(g) && !anyWordWants(c, g);
    }

    /** True when some typeable word on the field is waiting for {@code g} as its next letter. */
    private static boolean anyWordWants(GameCore c, int g) {
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (e.typeable() && e.word[e.pos] == g) return true;
        }
        return false;
    }

    /**
     * A press the boss claimed. Turns its verdict into score, sound and accuracy.
     *
     * Note what a rebuff does <em>not</em> do: it is not counted as a miss. The interlude set that
     * precedent — a refused input is not a typing mistake, and letting it reach the accuracy readout
     * would mean the dumpling on the game-over screen scolded the player for engaging with the
     * mechanic. The cost of a rebuff is paid where the mechanic lives instead: the drum's beat
     * resets, and the combo goes.
     *
     * @return true when the press did something, for the view's haptic tick
     */
    static boolean press(GameCore c, int g, int verdict, Layout L) {
        if (verdict == Boss.REBUFF) {
            c.keyBad[g] = 1f;
            c.combo = 0;
            c.shake = Math.max(c.shake, 0.22f);
            if (c.sound != null) c.sound.wrong();
            return false;
        }
        c.hits++;
        c.combo++;
        if (c.combo > c.maxCombo) c.maxCombo = c.combo;
        c.skyGlow = Math.max(c.skyGlow, GameCore.GLOW_HIT);
        c.skyGlowColor = Glyph.COLOR[g];
        // A bullet, from the key that was pressed to the part of the boss it landed on — the same
        // shot a press at a word fires, so a press means the same thing wherever it is aimed. The
        // damage is already done: this is playback, exactly as a word's own destruction waits for
        // its shot while the tile pops on the press.
        shot(c, g, L);
        if (verdict == Boss.HIT) {
            c.score += GameCore.BOSS_HIT;
            c.shake = Math.max(c.shake, 0.30f);
            c.flash = Math.max(c.flash, 0.4f);
            c.flashColor = GameCore.FLASH_CLEAR;
            Fx.explode(c, c.rnd, c.boss.bodyX(L), c.boss.bodyY(L), L.enemyR * 1.4f, 12,
                    Glyph.COLOR[g]);
        }
        // Pitched by how much of the boss is left, so a fight is audibly a countdown.
        if (c.sound != null) c.sound.squish(g, 1 + (int) (3f * (1f - c.boss.health())));
        return true;
    }

    /**
     * Fires the bullet a boss press earns, from key {@code g} to wherever that press landed.
     *
     * Held as an offset from the body's centre rather than as a point, so it homes as the boss drifts
     * — see {@link GameCore.Shot#atBoss}. No {@code target} and no {@code kill}, so {@link #impact} does the
     * one thing wanted at the far end: a burst of that letter's colour where it struck.
     *
     * Only fired for a press that landed. A rebuff fires nothing, the same way a wrong press at a
     * word fires nothing — a bullet that flies out and achieves nothing reads as the shot having
     * missed, when what happened is that it was refused.
     */
    private static void shot(GameCore c, int g, Layout L) {
        if (c.boss.body == null) return;
        GameCore.Shot s = new GameCore.Shot();
        s.sx = L.keyX[g];
        s.sy = L.keyY[g];
        s.tx = c.boss.hitX;
        s.ty = c.boss.hitY;
        s.atBoss = true;
        s.bossDx = c.boss.hitX - c.boss.body.centreX();
        s.bossDy = c.boss.hitY - c.boss.body.centreY();
        s.glyph = g;
        s.dur = GameCore.SHOT_TIME;
        c.shots.add(s);
    }

    /**
     * A tap on one of the boss's elements. Returns true when the boss took it, so the view knows
     * not to hand the same touch to anything else.
     */
    static boolean tap(GameCore c, float x, float y, Layout L) {
        if (c.state != GameCore.PLAY || !c.boss.fighting() || c.settingsOpen) return false;
        int i = c.boss.elemAt(x, y);
        if (i < 0) return false;
        int r = c.boss.tap(i, c.rnd);
        if (r == Boss.NONE) {
            // Its element, but nothing to do with it. Still swallowed: a tap that lands on the boss
            // must never fall through and be read as something else.
            return true;
        }
        if (r == Boss.REBUFF) {
            c.combo = 0;
            c.shake = Math.max(c.shake, 0.22f);
            if (c.sound != null) c.sound.wrong();
            return true;
        }
        c.hits++;
        c.combo++;
        if (c.combo > c.maxCombo) c.maxCombo = c.combo;
        if (r == Boss.HIT) {
            c.score += GameCore.BOSS_HIT;
            c.shake = Math.max(c.shake, 0.30f);
            Fx.explode(c, c.rnd, x, y, L.enemyR * 1.4f, 12, GameCore.INK_SPARK);
        }
        if (c.sound != null) c.sound.squish(i % Glyph.COUNT, 1);
        return true;
    }

    /** A finger landing on a draggable c.boss element. True when the c.boss has taken the gesture. */
    static boolean grab(GameCore c, float x, float y) {
        if (c.state != GameCore.PLAY || !c.boss.fighting() || c.settingsOpen) return false;
        int i = c.boss.elemAt(x, y);
        if (!c.boss.draggable(i)) return false;
        return c.boss.grab(i);
    }

    /** That finger moving. True when the drag finished the job. */
    static boolean dragTo(GameCore c, float x, float y, Layout L) {
        if (c.boss.held < 0) return false;
        int r = c.boss.dragTo(x, y, L);
        if (r != Boss.HIT) return false;
        c.score += GameCore.BOSS_HIT;
        c.hits++;
        c.combo++;
        if (c.combo > c.maxCombo) c.maxCombo = c.combo;
        c.shake = Math.max(c.shake, 0.28f);
        Fx.explode(c, c.rnd, x, y, L.enemyR * 1.5f, 14, GameCore.INK_SPARK);
        if (c.sound != null) c.sound.achievement();
        return true;
    }

    static void release(GameCore c) {
        c.boss.release();
    }

    /** True when a swipe up would shove SUMO, for the renderer's hint. */
    static boolean shoveReady(GameCore c) {
        return c.state == GameCore.PLAY && !c.settingsOpen && c.boss.shovable();
    }

    static boolean shove(GameCore c, Layout L) {
        if (!shoveReady(c) || !c.boss.shove()) return false;
        c.pushT = GameCore.PUSH_TIME;
        c.pushCount = 0;
        c.shake = Math.max(c.shake, 0.6f);
        c.flash = Math.max(c.flash, 0.55f);
        c.flashColor = GameCore.FLASH_CLEAR;
        c.skyGlow = 1f;
        c.skyGlowColor = GameCore.FLASH_CLEAR;
        Fx.explode(c, c.rnd, c.boss.bodyX(L), c.boss.bodyY(L), L.enemyR * 2.2f, 20, GameCore.INK_SPARK);
        if (c.sound != null) c.sound.achievement();
        return true;
    }

}
