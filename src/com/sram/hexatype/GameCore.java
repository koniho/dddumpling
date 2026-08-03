package com.sram.hexatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Every rule in the game: state machine, stage pacing, targeting, scoring, and the
 * simulation of enemies/shots/particles. Deliberately free of any Android import so it
 * can be driven headlessly by the test harness and the PNG preview tool.
 */
final class GameCore {

    // ---- states -------------------------------------------------------------
    static final int TITLE = 0, PLAY = 1, OVER = 2;

    /** Persistence seam; the Activity backs this with SharedPreferences. */
    interface Store {
        int loadBest();
        void saveBest(int best);
    }

    static final class Enemy {
        int[] word;
        int pos;
        float baseX, y, speed, phase, sway;
        boolean dying;
        float deathT;
        /** 1 right after a correct hit, decaying: drives the colour flash and scale pop. */
        float hitPulse;
        /** 0..1 as the word closes on the danger line. */
        float warn;
        /** Final lunge at the player, just before a life is lost. */
        boolean attacking;
        float attackT;

        int remaining() { return word.length - pos; }

        boolean typeable() { return !dying && !attacking && pos < word.length; }
    }

    static final class Shot {
        float sx, sy, tx, ty, t, dur;
        int glyph;
        Enemy target;
        boolean kill;
    }

    static final class Particle {
        float x, y, vx, vy, life, max, size;
        int color;
    }

    // ---- persistent-ish state ----------------------------------------------
    int state = TITLE;
    int score, best, lives, kills, stage, combo, maxCombo;

    // ---- transient ----------------------------------------------------------
    float time;              // seconds since entering the current state
    float clock;             // never resets; drives idle animation
    final List<Enemy> enemies = new ArrayList<Enemy>();
    final List<Shot> shots = new ArrayList<Shot>();
    final List<Particle> particles = new ArrayList<Particle>();
    Enemy target;
    float spawnTimer;
    float shake, flash, stageBanner;
    /** Highest proximity-to-danger across the field, 0..1. Drives the red screen pulse. */
    float warnLevel;
    final float[] keyPress = new float[Glyph.COUNT];
    final float[] keyBad = new float[Glyph.COUNT];

    /** Background starfield, in 0..1 view coordinates. Fixed seed: identical everywhere. */
    final float[] starX = new float[70];
    final float[] starY = new float[70];
    final float[] starS = new float[70];

    private final Random rnd;
    private final Store store;

    static final int START_LIVES = 3;
    private static final int KILLS_PER_STAGE = 8;

    /** Length of the lunge animation between crossing the line and losing a life. */
    static final float ATTACK_TIME = 0.42f;
    /** Fraction of the descent over which a word counts as "closing in". */
    private static final float WARN_BAND = 0.20f;

    GameCore(Store store, long seed) {
        this.store = store;
        this.rnd = new Random(seed);
        Random sr = new Random(20260803L);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = sr.nextFloat();
            starY[i] = sr.nextFloat();
            starS[i] = 0.35f + sr.nextFloat() * 0.65f;
        }
        best = store != null ? store.loadBest() : 0;
    }

    // ---- stage pacing -------------------------------------------------------
    // One knob per dial so new stages are a numbers change, not a rewrite.

    /** Seconds an enemy takes to fall from the play top to the danger line. */
    float travelSeconds() { return Math.max(4.2f, 15f - (stage - 1) * 1.05f); }

    float spawnInterval() { return Math.max(0.80f, 2.5f - (stage - 1) * 0.13f); }

    int maxEnemies() { return Math.min(7, 3 + stage / 2); }

    int maxWordLen() { return Math.min(5, 2 + stage / 2); }

    int minWordLen() { return Math.max(2, maxWordLen() - 2); }

    int killsIntoStage() { return kills % KILLS_PER_STAGE; }

    // ---- lifecycle ----------------------------------------------------------

    void startGame() {
        state = PLAY;
        time = 0;
        score = 0;
        kills = 0;
        stage = 1;
        combo = 0;
        maxCombo = 0;
        lives = START_LIVES;
        enemies.clear();
        shots.clear();
        particles.clear();
        target = null;
        spawnTimer = 0.7f;
        shake = 0;
        flash = 0;
        stageBanner = 1.5f;
    }

    void toTitle() {
        state = TITLE;
        time = 0;
        enemies.clear();
        shots.clear();
        target = null;
    }

    /** A tap anywhere that is not a key hex. Advances the non-play screens. */
    void anyTap() {
        if (state == TITLE) startGame();
        else if (state == OVER && time > 0.6f) startGame();
    }

    // ---- input --------------------------------------------------------------

    /** Player pressed key {@code g}. Returns true when it advanced a word. */
    boolean tapKey(int g, Layout L) {
        if (state != PLAY) {
            anyTap();
            return false;
        }
        keyPress[g] = 1f;

        if (target != null && (!target.typeable() || !enemies.contains(target))) target = null;

        if (target == null) {
            // Lock onto the most urgent enemy — lowest on screen — that starts with g.
            Enemy pick = null;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (!e.typeable()) continue;
                if (e.word[e.pos] != g) continue;
                if (pick == null || e.y > pick.y) pick = e;
            }
            if (pick == null) {
                miss(g);
                return false;
            }
            target = pick;
        } else if (target.word[target.pos] != g) {
            miss(g);
            return false;
        }

        Enemy e = target;
        float hx = tileX(e, e.pos, L);
        float hy = e.y;
        e.pos++;
        e.hitPulse = 1f;
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        score += 5 + Math.min(combo, 25) / 2;

        boolean kill = e.pos >= e.word.length;
        if (kill) {
            e.dying = true;
            e.deathT = 0;
            target = null;
        }

        Shot s = new Shot();
        s.sx = L.keyX[g];
        s.sy = L.keyY[g];
        s.tx = hx;
        s.ty = hy;
        s.glyph = g;
        s.target = e;
        s.kill = kill;
        s.dur = 0.13f;
        shots.add(s);
        return true;
    }

    private void miss(int g) {
        keyBad[g] = 1f;
        combo = 0;
        shake = Math.max(shake, 0.25f);
    }

    /** The glyph the player must press next, or -1 when nothing is locked. */
    int hintGlyph() {
        if (state != PLAY || target == null || target.pos >= target.word.length) return -1;
        return target.word[target.pos];
    }

    // ---- geometry helpers (shared by renderer and hit feedback) -------------

    float enemyCentreX(Enemy e) {
        return e.baseX + e.sway * (float) Math.sin(clock * 1.1f + e.phase);
    }

    /** Centre x of tile {@code i} of enemy {@code e}, accounting for consumed tiles. */
    float tileX(Enemy e, int i, Layout L) {
        float rowW = L.wordWidth(e.remaining());
        float left = enemyCentreX(e) - rowW / 2f + Layout.HEAD_SCALE * L.enemyR;
        return left + (i - e.pos) * L.enemyStep;
    }

    // ---- simulation ---------------------------------------------------------

    void update(float dt, Layout L) {
        clock += dt;
        time += dt;

        for (int i = 0; i < Glyph.COUNT; i++) {
            keyPress[i] = decay(keyPress[i], dt * 5.5f);
            keyBad[i] = decay(keyBad[i], dt * 3.2f);
        }
        shake = decay(shake, dt * 2.6f);
        flash = decay(flash, dt * 2.2f);
        stageBanner = decay(stageBanner, dt);

        updateParticles(dt);
        updateShots(dt, L);

        if (state != PLAY) return;

        spawnTimer -= dt;
        if (spawnTimer <= 0 && enemies.size() < maxEnemies()) {
            spawn(L);
            spawnTimer = spawnInterval();
        }

        float band = Math.max(1f, (L.dangerY - L.playTop) * WARN_BAND);
        warnLevel = 0f;

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.hitPulse = decay(e.hitPulse, dt * 6.5f);

            if (e.dying) {
                e.deathT += dt;
                continue;
            }

            if (e.attacking) {
                // Committed lunge: it dives at the player, and the screen reacts.
                e.attackT += dt;
                e.y += e.speed * 3.2f * dt;
                e.warn = 1f;
                warnLevel = 1f;
                flash = Math.max(flash, 0.35f + 0.5f * (e.attackT / ATTACK_TIME));
                shake = Math.max(shake, 0.35f * (e.attackT / ATTACK_TIME));
                if (e.attackT >= ATTACK_TIME) {
                    // Unlist first: a fatal breach clears the whole field, which would
                    // invalidate this index.
                    enemies.remove(i);
                    breach(e, L);
                }
                continue;
            }

            e.y += e.speed * dt;
            e.warn = clamp01((e.y - (L.dangerY - band)) / band);
            if (e.warn > warnLevel) warnLevel = e.warn;

            if (e.y + L.enemyR >= L.dangerY) {
                e.attacking = true;
                e.attackT = 0;
                if (target == e) target = null;
            }
        }
    }

    private void updateShots(float dt, Layout L) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            if (s.target != null && enemies.contains(s.target)) {
                // Home in: the word keeps drifting while the shot is in the air.
                s.tx = s.kill ? enemyCentreX(s.target) : tileX(s.target, s.target.pos - 1, L);
                s.ty = s.target.y;
            }
            s.t += dt / s.dur;
            if (s.t >= 1f) {
                impact(s, L);
                shots.remove(i);
            }
        }
    }

    private void impact(Shot s, Layout L) {
        Enemy e = s.target;
        if (s.kill && e != null && enemies.remove(e)) {
            explode(enemyCentreX(e), e.y, L.enemyR * 1.5f, e.word.length + 8, 0xFFFFFFFF);
            for (int i = 0; i < e.word.length; i++) {
                explode(s.tx, s.ty, L.enemyR, 4, Glyph.COLOR[e.word[i]]);
            }
            kills++;
            score += 25 * e.word.length;
            int ns = 1 + kills / KILLS_PER_STAGE;
            if (ns != stage) {
                stage = ns;
                stageBanner = 1.6f;
            }
        } else {
            explode(s.tx, s.ty, L.enemyR * 0.7f, 7, Glyph.COLOR[s.glyph]);
        }
    }

    private void breach(Enemy e, Layout L) {
        if (target == e) target = null;
        lives--;
        combo = 0;
        shake = 1f;
        flash = 1f;
        explode(enemyCentreX(e), L.dangerY, L.enemyR * 2f, 16, 0xFFFF7A9E);
        if (lives <= 0) {
            state = OVER;
            time = 0;
            // Clear the field so the summary screen is readable; particles stay for the bang.
            enemies.clear();
            shots.clear();
            target = null;
            if (score > best) {
                best = score;
                if (store != null) store.saveBest(best);
            }
        }
    }

    private void spawn(Layout L) {
        Enemy e = new Enemy();
        int len = minWordLen() + rnd.nextInt(maxWordLen() - minWordLen() + 1);
        e.word = new int[len];
        for (int i = 0; i < len; i++) e.word[i] = rnd.nextInt(Glyph.COUNT);
        e.pos = 0;

        float half = L.wordWidth(len) / 2f;
        e.sway = Math.min(0.035f * L.w, Math.max(0f, (L.playRight - L.playLeft) / 2f - half - 4f));
        float lo = L.playLeft + half + e.sway;
        float hi = L.playRight - half - e.sway;
        e.baseX = hi > lo ? lo + rnd.nextFloat() * (hi - lo) : (L.playLeft + L.playRight) / 2f;
        e.phase = rnd.nextFloat() * 6.283f;
        e.y = L.playTop - L.enemyR;
        e.speed = (L.dangerY - e.y) / travelSeconds();
        enemies.add(e);
    }

    private void explode(float x, float y, float spread, int n, int color) {
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            double a = rnd.nextFloat() * 6.283f;
            float v = spread * (2.5f + rnd.nextFloat() * 4f);
            p.x = x;
            p.y = y;
            p.vx = v * (float) Math.cos(a);
            p.vy = v * (float) Math.sin(a);
            p.max = 0.28f + rnd.nextFloat() * 0.42f;
            p.life = p.max;
            p.size = spread * (0.10f + rnd.nextFloat() * 0.16f);
            p.color = color;
            particles.add(p);
        }
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.94f;
            p.vy = p.vy * 0.94f + 220f * dt;
        }
    }

    /** 0 at full health, rising to 1 as lives run out. Tints the whole screen red. */
    float harm() {
        if (state != PLAY) return 0f;
        return clamp01(1f - (float) lives / START_LIVES);
    }

    private static float decay(float v, float amount) {
        v -= amount;
        return v < 0 ? 0 : v;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
