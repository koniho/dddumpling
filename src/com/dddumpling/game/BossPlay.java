package com.dddumpling.game;

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
        if (c.sound != null) c.sound.bossMusic(false);
        // Everything still on the field goes with it. A boss dying to a field of three words and
        // then handing you a mopping-up job is an anticlimax, and the interlude is the payoff.
        for (int i = c.enemies.size() - 1; i >= 0; i--) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.destroyed) c.destroyWord(e, c.enemyCentreX(e), e.y, L);
        }
        if (won) {
            c.progress.beatBoss(c.boss.kind);
            Interlude.awardBossPrize(c, c.boss.kind);
            if (c.boss.kind == Boss.SLIME && c.stage == Boss.EVERY) c.cubeUnlocked = true;
            if (c.stage == Boss.EVERY) c.unlockRoster();
            c.score += GameCore.BOSS_BONUS;
            // A life back, capped as the steamer's is. A boss costs lives to learn, and a run that
            // beats one should not arrive at the next stage on its last one.
            if (c.lives < GameCore.START_LIVES) c.lives++;
            c.flash = Math.max(c.flash, 1f);
            c.flashColor = GameCore.FLASH_CLEAR;
            c.skyGlow = 1f;
            c.skyGlowColor = GameCore.FLASH_CLEAR;
            c.shake = Math.max(c.shake, 0.8f);
        }
        // Sent home before the quota is filled, so nothing that reads bossActive() can see a beaten
        // boss and a satisfied stage at the same time.
        c.boss.leave();
        c.spawnedThisStage = c.stageQuota();
    }

    static void deathFeedback(GameCore c, float before) {
        Boss b = c.boss;
        if (!b.beaten || before < 0f) return;
        float now = b.leaveProgress() * Boss.LEAVE;
        if (now <= before) return;
        boolean impact = before == 0f || (before < b.deathImpactTime() && now >= b.deathImpactTime());
        if (impact || b.defeatChime) {
            c.bossDeathHaptic = impact ? 2 : 1;
            c.shake = Math.max(c.shake, impact ? 1.2f : .22f + b.defeatBeat * .08f);
        }
    }

    static void slam(GameCore c, Layout L) {
        c.shake = Math.max(c.shake, 1f);
        c.takeHit(c.boss.bodyX(L), L);
    }

    /** The delayed contact frame of an Octopulse wrong-key whip. */
    static void octoWhipHit(GameCore c, Layout L) {
        c.takeHit(c.boss.octoLashX, L);
        c.shake = Math.max(c.shake, 1.25f);
        Fx.explode(c, c.rnd, c.boss.octoLashX, c.boss.octoLashY,
                L.keyR * 1.8f, 18, 0xFFFF355F);
    }

    static boolean claims(GameCore c, int g) {
        // A bolt in the air outranks everything, engaged word included: it is the only thing here
        // that costs a life.
        if (c.boss.boltWants(g)) return true;

        // A shielded Slime rejects every press, including one made during a word. While open it
        // only owns genuinely stray presses, so starting a valid enemy word still works.
        if (c.boss.kind == Boss.SLIME && !c.boss.open()) return true;
        if (c.boss.kind == Boss.SLIME && c.target == null && !anyWordWants(c, g)) return true;
        // Once an Octopulse arm is moving, every available key answers its reaction prompt.
        if (c.boss.kind == Boss.OCTOPUS && c.boss.octoTarget >= 0
                && c.boss.octoReach >= 0f) return true;
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

    static boolean press(GameCore c, int g, int verdict, Layout L) {
        if (verdict == Boss.PLAYER_HIT) {
            c.keyBad[g] = 1f;
            c.misses++;
            c.combo = 0;
            // Damage waits for the visible whip to reach the key.
            return true;
        }
        if (verdict == Boss.REBUFF) {
            boolean slimeShield = c.boss.kind == Boss.SLIME && !c.boss.open();
            if (slimeShield) {
                shot(c, g, L, verdict);
                if (c.sound != null) c.sound.shieldBounce();
            }
            c.keyBad[g] = 1f;
            c.combo = 0;
            c.shake = Math.max(c.shake, 0.22f);
            if (!slimeShield && c.sound != null) c.sound.wrong();
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
        shot(c, g, L, verdict);
        if (verdict == Boss.PARRY) {
            // Swatted, not landed on the boss: the score is the same as a hit but the boss is
            // untouched, so no shake and no flash — those read as damage.
            c.score += GameCore.BOSS_HIT;
            Fx.explode(c, c.rnd, c.boss.hitX, c.boss.hitY, L.enemyR * 1.2f, 10, Glyph.COLOR[g]);
            if (c.sound != null) {
                if (c.boss.kind == Boss.SPLITTER) c.sound.divideDamage();
                else if (c.boss.boltDestroyed) c.sound.boltDeath();
                else c.sound.boltPop();
            }
            return true;
        }
        if (verdict == Boss.HIT) {
            c.score += GameCore.BOSS_HIT;
            c.shake = Math.max(c.shake, 0.30f);
            c.flash = Math.max(c.flash, 0.4f);
            boolean divide = c.boss.kind == Boss.SPLITTER;
            c.flashColor = divide ? 0xFF7D45D6 : GameCore.FLASH_CLEAR;
            Fx.explode(c, c.rnd, divide ? c.boss.hitX : c.boss.bodyX(L),
                    divide ? c.boss.hitY : c.boss.bodyY(L), L.enemyR * 1.4f, 12,
                    Glyph.COLOR[g]);
            if (c.sound != null) {
                if (divide) c.sound.divideDamage();
                else c.sound.bossDamage();
            }
        }
        boolean dividePrompt = c.boss.kind == Boss.SPLITTER && verdict == Boss.PART;
        if (dividePrompt) {
            c.shake = Math.max(c.shake, 0.18f);
            c.flash = Math.max(c.flash, 0.28f);
            c.flashColor = 0xFF7D45D6;
            Fx.explode(c, c.rnd, c.boss.hitX, c.boss.hitY, L.enemyR, 9, 0xFF9B62FF);
            if (c.sound != null) c.sound.divideDamage();
        }
        boolean slimePrompt = c.boss.kind == Boss.SLIME
                && (verdict == Boss.PART || verdict == Boss.SPLIT);
        if (slimePrompt) {
            // Killing the charged character bruises the slime without claiming real boss health.
            c.shake = Math.max(c.shake, 0.12f);
            Fx.explode(c, c.rnd, c.boss.hitX, c.boss.hitY, L.enemyR * 0.72f, 6, Glyph.COLOR[g]);
            if (verdict != Boss.SPLIT && c.sound != null) c.sound.boltPop();
        }
        if (verdict == Boss.SPLIT && c.sound != null) c.sound.bossSplit();
        // Pitched by how much of the boss is left, except the charged bolt which owns its bloop.
        if (!slimePrompt && c.boss.kind != Boss.SPLITTER && c.sound != null)
            c.sound.squish(g, 1 + (int) (3f * (1f - c.boss.health())));
        return true;
    }

    /**
     * Fires the bullet a boss press earns, from key {@code g} to wherever that press landed.
     *
     * Held as an offset from the body's centre rather than as a point, so it homes as the boss drifts
     * — see {@link GameCore.Shot#atBoss}. No {@code target} and no {@code kill}, so {@link #impact} does the
     * one thing wanted at the far end: a burst of that letter's colour where it struck.
     *
     * A normal rebuff fires nothing, just like a wrong word press. The Slime is the exception:
     * its closed-state shield physically returns the shot, making the refusal visible.
     */
    private static void shot(GameCore c, int g, Layout L, int verdict) {
        if (c.boss.body == null) return;
        GameCore.Shot s = new GameCore.Shot();
        s.sx = c.keyX(L, g);
        s.sy = c.keyY(L, g);
        s.tx = c.boss.hitX;
        s.ty = c.boss.hitY;
        // A parry is aimed at where a bolt was, not at the boss, so it must not home: the bolt is
        // already gone and the body's centre is somewhere else entirely.
        s.atBoss = verdict != Boss.PARRY;
        s.shieldBounce = verdict == Boss.REBUFF;
        s.bossDx = c.boss.hitX - c.boss.body.centreX();
        s.bossDy = c.boss.hitY - c.boss.body.centreY();
        s.glyph = g;
        s.dur = s.shieldBounce ? GameCore.SHOT_TIME * 3f : GameCore.SHOT_TIME;
        c.shots.add(s);
    }

    /**
     * A tap on one of the boss's elements. Returns true when the boss took it, so the view knows
     * not to hand the same touch to anything else.
     */
    static boolean tap(GameCore c, float x, float y, Layout L) {
        if (c.state != GameCore.PLAY || !c.boss.fighting() || c.settingsOpen) return false;
        // A tap on a glob stays on the boss, but only a drag can collect it.
        return c.boss.elemAt(x, y) >= 0;
    }

    /** A finger landing on a draggable c.boss element. True when the c.boss has taken the gesture. */
    static boolean grab(GameCore c, float x, float y) {
        if (c.state != GameCore.PLAY || !c.boss.fighting() || c.settingsOpen) return false;
        int i = c.boss.elemAt(x, y);
        if (c.boss.draggable(i)) return c.boss.grab(i);
        return c.boss.grabBody(x, y);
    }

    /** That finger moving. True when the drag finished the job. */
    static boolean dragTo(GameCore c, float x, float y, Layout L) {
        if (c.boss.held < 0 && c.boss.held != -2 && c.boss.held != -3) return false;
        c.boss.mushroomShakeCue = false;
        float beforeHp = c.boss.hp;
        int r = c.boss.dragTo(x, y, L);
        c.progress.bossDamage(c.boss.kind, beforeHp, c.boss.hp);
        if (c.boss.mushroomShakeCue && c.sound != null) c.sound.mushroomShake();
        c.boss.mushroomShakeCue = false;
        if (r != Boss.HIT) return false;
        c.score += GameCore.BOSS_HIT;
        c.hits++;
        c.combo++;
        if (c.combo > c.maxCombo) c.maxCombo = c.combo;
        c.shake = Math.max(c.shake, 0.28f);
        Fx.explode(c, c.rnd, x, y, L.enemyR * 1.5f, 14, GameCore.INK_SPARK);
        if (c.sound != null) {
            if (c.boss.kind == Boss.SLIME) c.sound.slimeDamage();
            else if (c.boss.kind == Boss.OCTOPUS) c.sound.octoDamage();
            else c.sound.bossDamage();
        }
        return true;
    }

    static void release(GameCore c) {
        c.boss.release();
    }

    static boolean pinch(GameCore c, float distance, Layout L) {
        return pinch(c, distance, Float.NaN, Float.NaN, Float.NaN, Float.NaN, L);
    }

    static boolean pinch(GameCore c, float distance, float x1, float y1, float x2, float y2,
            Layout L) {
        float beforeHp = c.boss.hp;
        boolean changed = c.boss.pinch(distance, x1, y1, x2, y2, c.rnd);
        c.progress.bossDamage(c.boss.kind, beforeHp, c.boss.hp);
        if (!changed) return false;
        boolean deactivate = c.boss.divideDeactivated;
        c.shake = Math.max(c.shake, deactivate ? 1.2f : 0.85f);
        c.flashColor = deactivate ? 0xFFFFFFFF : 0xFF7D45D6;
        c.flashColor = 0xFF7D45D6;
        Fx.explode(c, c.rnd, c.boss.hitX, c.boss.hitY, L.enemyR * (deactivate ? 3.5f : 2.8f), deactivate ? 38 : 28,
                0xFF9B62FF);
        if (c.sound != null) {
            if (deactivate) c.sound.divideDeactivate();
            else c.sound.divideSplit();
        }
        return true;
    }

}
