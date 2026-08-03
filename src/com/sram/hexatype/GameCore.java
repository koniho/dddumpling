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
        float loadSpeed();
        void saveSpeed(float speed);
        int loadBgm();
        void saveBgm(int choice);
    }

    /**
     * Audio seam. Kept as an interface so the rules stay Android-free and the harness can
     * assert which effect fires on which event.
     */
    interface Sound {
        /** @param depth presses that were still owed on the tile before this press */
        void squish(int glyph, int depth);
        void clearWord();
        void wrong();
        void damage();
        void achievement();
        /** Switch the looping background track to {@link Music#NAMES}[choice]. */
        void selectMusic(int choice);
    }

    /** Optional; null in the harness unless a test is watching for effects. */
    Sound sound;

    static final class Enemy {
        int[] word;
        /** Presses each tile needs: 1 for a plain letter, 2..4 for a stacked one. */
        int[] need;
        int pos;
        /** Presses landed on the current tile so far. */
        int done;
        float baseX, y, speed, phase, sway;
        boolean dying;
        float deathT;
        /** 1 right after a correct hit, decaying: drives the colour flash and scale pop. */
        float hitPulse;
        /** Which tile was just struck, so only that one takes the full pop. */
        int hitIndex = -1;
        /** 1 right after a wrong key reset this word, decaying. */
        float failPulse;
        /** 0..1 as the word slides in from above the top edge. */
        float enterT;
        /** 0..1 as the word closes on the danger line. */
        float warn;
        /** Final lunge at the player, just before a life is lost. */
        boolean attacking;
        float attackT;
        /** Cleared and flying apart; the enemy stays listed until this finishes. */
        boolean destroyed;
        float destroyT;
        /** Per-tile fly-off direction, -1 left or +1 right. */
        float[] flyDir;

        int remaining() { return word.length - pos; }

        boolean typeable() {
            return !dying && !attacking && !destroyed && pos < word.length;
        }

        /** Presses needed to clear this whole word from scratch. */
        int totalPresses() {
            int n = 0;
            for (int i = 0; i < need.length; i++) n += need[i];
            return n;
        }

        boolean stacked(int i) { return need[i] > 1; }
    }

    static final class Shot {
        float sx, sy, tx, ty, t, dur;
        int glyph;
        Enemy target;
        boolean kill;
        /** Tile this shot is flying at; stored so a word restart cannot invalidate it. */
        int tileIndex;
    }

    static final class Particle {
        float x, y, vx, vy, life, max, size;
        int color;
    }

    // ---- persistent-ish state ----------------------------------------------
    int state = TITLE;
    int score, best, lives, kills, stage, combo, maxCombo;
    /** Words released so far in the current stage; capped at {@link #stageQuota()}. */
    int spawnedThisStage;
    /** Words of the current stage that are done with, whether cleared or breached. */
    int resolvedThisStage;
    /** Correct and incorrect presses over the whole run, for the accuracy readout. */
    int hits, misses;
    /** Incorrect presses in the current stage; zero at stage end earns the gold dumpling. */
    int missesThisStage;

    // ---- settings (persisted) ----------------------------------------------
    static final float SPEED_MIN = 0.5f, SPEED_MAX = 1.5f;
    /** Pacing multiplier: >1 makes words fall and arrive faster. */
    float speed = 1f;
    int bgmChoice;
    /** While true the simulation is frozen and the settings panel is showing. */
    boolean settingsOpen;

    // ---- transient ----------------------------------------------------------
    float time;              // seconds since entering the current state
    float clock;             // never resets; drives idle animation
    final List<Enemy> enemies = new ArrayList<Enemy>();
    final List<Shot> shots = new ArrayList<Shot>();
    final List<Particle> particles = new ArrayList<Particle>();
    Enemy target;
    float spawnTimer;
    /** Breather between stages; nothing spawns while this is running. */
    float stageGap;
    /** Counts down while the flawless-stage gold dumpling is on screen. */
    float perfectBanner;
    float shake, flash, stageBanner;
    /** Highest proximity-to-danger across the field, 0..1. Drives the red screen pulse. */
    float warnLevel;

    /**
     * Smoothed x of the lock indicator, so it slides from letter to letter as you type
     * rather than teleporting. Owner is tracked separately: switching to a different word
     * should snap, not glide across the screen.
     */
    float caretX;
    Enemy caretOwner;
    final float[] keyPress = new float[Glyph.COUNT];
    final float[] keyBad = new float[Glyph.COUNT];

    /** Background starfield, in 0..1 view coordinates. Fixed seed: identical everywhere. */
    /**
     * Parallax cloud layers, back to front. Positions are fixed offsets in 0..1 view
     * coordinates; the drifting is a pure function of {@link #clock}, so no per-frame state
     * is needed and the harness matches the device exactly.
     */
    static final int CLOUD_LAYERS = 3;
    static final int CLOUDS_PER_LAYER = 3;
    /** Downward drift per layer, in view heights per second. Back layer moves least. */
    static final float[] CLOUD_SPEED = {0.010f, 0.024f, 0.052f};

    final float[][] cloudX = new float[CLOUD_LAYERS][CLOUDS_PER_LAYER];
    final float[][] cloudY = new float[CLOUD_LAYERS][CLOUDS_PER_LAYER];
    final float[][] cloudW = new float[CLOUD_LAYERS][CLOUDS_PER_LAYER];
    final int[][] cloudSeed = new int[CLOUD_LAYERS][CLOUDS_PER_LAYER];

    /** Vertical position of a cloud right now, as a 0..1 fraction that wraps. */
    float cloudPhase(int layer, int i) {
        float v = cloudY[layer][i] + clock * CLOUD_SPEED[layer];
        return v - (float) Math.floor(v);
    }

    private final Random rnd;
    private final Store store;

    static final int START_LIVES = 3;
    /** Pause after a stage is cleared, before the next wave starts arriving. */
    static final float STAGE_GAP = 1.3f;
    /** How long the flawless-stage gold dumpling stays on screen. */
    static final float PERFECT_TIME = 2.1f;
    /** How long a cleared word takes to fly apart before it stops existing. */
    static final float DESTROY_TIME = 0.40f;

    /** Length of the lunge animation between crossing the line and losing a life. */
    static final float ATTACK_TIME = 0.42f;
    /** Fraction of the descent over which a word counts as "closing in". */
    private static final float WARN_BAND = 0.20f;

    GameCore(Store store, long seed) {
        this.store = store;
        this.rnd = new Random(seed);
        Random sr = new Random(20260803L);
        for (int l = 0; l < CLOUD_LAYERS; l++) {
            for (int i = 0; i < CLOUDS_PER_LAYER; i++) {
                cloudX[l][i] = 0.1f + sr.nextFloat() * 0.8f;
                // Spread evenly down the layer, then jitter, so gaps stay irregular.
                cloudY[l][i] = (i + sr.nextFloat() * 0.7f) / CLOUDS_PER_LAYER;
                cloudW[l][i] = 0.85f + sr.nextFloat() * 0.5f;
                cloudSeed[l][i] = sr.nextInt(1 << 20);
            }
        }
        if (store != null) {
            best = store.loadBest();
            speed = clampSpeed(store.loadSpeed());
            bgmChoice = Math.max(0, Math.min(Music.NAMES.length - 1, store.loadBgm()));
        }
    }

    static float clampSpeed(float v) {
        if (v != v) return 1f;                       // NaN from a corrupt store
        return v < SPEED_MIN ? SPEED_MIN : v > SPEED_MAX ? SPEED_MAX : v;
    }

    // ---- settings -----------------------------------------------------------

    void openSettings() {
        settingsOpen = true;
    }

    void closeSettings() {
        settingsOpen = false;
    }

    void setSpeed(float v) {
        speed = clampSpeed(v);
        if (store != null) store.saveSpeed(speed);
    }

    void setBgm(int choice) {
        if (choice < 0 || choice >= Music.NAMES.length) return;
        bgmChoice = choice;
        if (store != null) store.saveBgm(choice);
        if (sound != null) sound.selectMusic(choice);
    }

    // ---- stage pacing -------------------------------------------------------
    // One knob per dial so new stages are a numbers change, not a rewrite.

    /**
     * Seconds an enemy takes to fall from spawn to the danger line. The player's speed
     * setting divides this, so 1.5 means everything arrives half again as fast.
     */
    float travelSeconds() { return Math.max(4.2f, 15f - (stage - 1) * 1.05f) / speed; }

    float spawnInterval() { return Math.max(0.80f, 2.5f - (stage - 1) * 0.13f) / speed; }

    int maxEnemies() { return Math.min(7, 3 + stage / 2); }

    int maxWordLen() { return Math.min(5, 2 + stage / 2); }

    int minWordLen() { return Math.max(2, maxWordLen() - 2); }

    /** How many words this stage releases in total. */
    int stageQuota() { return Math.min(10, 5 + stage / 2); }

    /** Hard ceiling on the presses any single word can demand. */
    static final int MAX_PRESSES = 8;

    /** Odds that a given tile becomes a stack. Stacks stay out of the opening stage. */
    float stackChance() {
        return stage < 2 ? 0f : Math.min(0.55f, 0.13f * (stage - 1));
    }

    /** True once every word of this stage has been released and dealt with. */
    boolean stageCleared() {
        return spawnedThisStage >= stageQuota() && enemies.isEmpty() && shots.isEmpty();
    }

    // ---- lifecycle ----------------------------------------------------------

    void startGame() {
        state = PLAY;
        time = 0;
        score = 0;
        kills = 0;
        stage = 1;
        spawnedThisStage = 0;
        resolvedThisStage = 0;
        stageGap = 0;
        perfectBanner = 0;
        hits = 0;
        misses = 0;
        missesThisStage = 0;
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
            // Engage the most urgent match: the word lowest on screen, i.e. closest to
            // reaching the player, whose next-needed letter is g.
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
            // Engaged word, wrong letter: the whole word has to be retyped from scratch,
            // which also resets every stack count. The lock is dropped rather than held —
            // re-engaging is a deliberate press, so the next press is free to pick whatever
            // word is now most urgent instead of being stuck on this one.
            Enemy missed = target;
            missed.pos = 0;
            missed.done = 0;
            missed.hitPulse = 0f;
            missed.hitIndex = -1;
            missed.failPulse = 1f;
            target = null;
            caretOwner = null;
            miss(g);
            return false;
        }

        Enemy e = target;
        int struck = e.pos;
        float hx = tileX(e, struck, L);
        float hy = e.y;

        // A stacked tile absorbs several presses of the same letter before it clears.
        if (sound != null) sound.squish(g, pressesLeft(e, struck));
        e.done++;
        if (e.done >= e.need[struck]) {
            e.pos++;
            e.done = 0;
        }
        e.hitPulse = 1f;
        e.hitIndex = struck;
        hits++;
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
        s.tileIndex = struck;
        s.dur = 0.13f;
        shots.add(s);
        return true;
    }

    private void miss(int g) {
        keyBad[g] = 1f;
        misses++;
        if (sound != null) sound.wrong();
        missesThisStage++;
        combo = 0;
        shake = Math.max(shake, 0.25f);
    }

    /** Correct presses as a fraction of all presses; 1 before anything is pressed. */
    float accuracy() {
        int total = hits + misses;
        return total == 0 ? 1f : (float) hits / total;
    }

    int accuracyPercent() {
        return Math.round(accuracy() * 100f);
    }

    /**
     * Expression for the accuracy dumpling: 0 (saddest) at or below 60%, 1 (happiest) at
     * or above 90%.
     */
    float accuracyMood() {
        return clamp01((accuracyPercent() - 60f) / 30f);
    }

    /** Eases the lock indicator toward the tile the player must press next. */
    private void updateCaret(float dt, Layout L) {
        if (target == null || !target.typeable() || !enemies.contains(target)) {
            caretOwner = null;
            return;
        }
        float tx = tileX(target, target.pos, L);
        if (caretOwner != target) {
            caretOwner = target;
            caretX = tx;          // a fresh lock snaps into place
        } else {
            caretX += (tx - caretX) * Math.min(1f, dt * 17f);
        }
    }

    /** Where the lock indicator should be drawn for {@code e}, in view coordinates. */
    float caretXFor(Enemy e, Layout L) {
        return caretOwner == e ? caretX : tileX(e, e.pos, L);
    }

    /** Presses still owed on tile {@code i}; 0 once it is cleared. */
    int pressesLeft(Enemy e, int i) {
        if (i < e.pos) return 0;
        return e.need[i] - (i == e.pos ? e.done : 0);
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

    /**
     * Centre x of tile {@code i} of enemy {@code e}. Indexed from the start of the whole
     * word: cleared letters stay on screen until the word is finished, so the row never
     * reflows under the player's thumbs mid-word.
     */
    float tileX(Enemy e, int i, Layout L) {
        float rowW = L.wordWidth(e.word.length);
        float left = enemyCentreX(e) - rowW / 2f + Layout.HEAD_SCALE * L.enemyR;
        return left + i * L.enemyStep;
    }

    // ---- simulation ---------------------------------------------------------

    void update(float dt, Layout L) {
        // The clock keeps running so the panel itself can animate, but nothing else moves.
        clock += dt;
        if (settingsOpen) return;
        time += dt;

        for (int i = 0; i < Glyph.COUNT; i++) {
            keyPress[i] = decay(keyPress[i], dt * 5.5f);
            keyBad[i] = decay(keyBad[i], dt * 3.2f);
        }
        shake = decay(shake, dt * 2.6f);
        flash = decay(flash, dt * 2.2f);
        stageBanner = decay(stageBanner, dt);
        perfectBanner = decay(perfectBanner, dt);

        // Cleared here, above the early return, and raised again by the enemy loop below.
        // Resetting it after the return left the red edge glow stuck on at whatever the
        // last lunging word set it to, all the way through the game-over screen.
        warnLevel = 0f;

        updateParticles(dt);
        updateShots(dt, L);

        if (state != PLAY) return;

        // Stages are discrete waves: a stage releases exactly stageQuota() words, and the
        // next stage cannot start arriving until the field is completely clear.
        if (stageGap > 0) {
            stageGap -= dt;
        } else if (spawnedThisStage < stageQuota()) {
            spawnTimer -= dt;
            // Counted against live words only: a word already flying apart is no longer
            // occupying the field as far as pacing is concerned.
            if (spawnTimer <= 0 && liveEnemies() < maxEnemies()) {
                spawn(L);
                spawnedThisStage++;
                spawnTimer = spawnInterval();
            }
        } else if (stageCleared()) {
            advanceStage();
        }

        updateCaret(dt, L);

        float band = Math.max(1f, (L.dangerY - L.playTop) * WARN_BAND);

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.hitPulse = decay(e.hitPulse, dt * 6.5f);
            e.failPulse = decay(e.failPulse, dt * 2.8f);
            // Driven by position, not time: a timed ramp would finish while the word was
            // still above the top edge, so nobody would ever see it.
            e.enterT = clamp01((e.y + L.enemyR) / (L.enemyR * 3f));

            if (e.destroyed) {
                e.destroyT += dt;
                if (e.destroyT >= DESTROY_TIME) enemies.remove(i);
                continue;
            }

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
                s.tx = s.kill ? enemyCentreX(s.target) : tileX(s.target, s.tileIndex, L);
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
        if (s.kill && e != null && enemies.contains(e) && !e.destroyed) {
            // The word is credited now but stays listed until it has flown apart, so
            // anything gated on the field being clear waits for the animation.
            e.destroyed = true;
            e.destroyT = 0f;
            e.dying = false;
            computeFlyDirs(e, L);

            explode(enemyCentreX(e), e.y, L.enemyR * 1.5f, e.word.length + 8, 0xFFFFFFFF);
            for (int i = 0; i < e.word.length; i++) {
                explode(s.tx, s.ty, L.enemyR, 4, Glyph.COLOR[e.word[i]]);
            }
            shake = Math.max(shake, 0.30f);
            kills++;
            resolvedThisStage++;
            // Scored per press, so a stacked word is worth what it cost to clear.
            score += 25 * e.totalPresses();
            if (sound != null) sound.clearWord();
        } else {
            explode(s.tx, s.ty, L.enemyR * 0.7f, 7, Glyph.COLOR[s.glyph]);
        }
    }

    /**
     * Picks which way each tile of a cleared word flies. Tiles head for whichever screen
     * edge is nearer, except that the first and last tile always go left and right
     * respectively, so a word visibly splits apart rather than sliding off as a block.
     */
    private void computeFlyDirs(Enemy e, Layout L) {
        int n = e.word.length;
        e.flyDir = new float[n];
        float mid = L.w / 2f;
        for (int i = 0; i < n; i++) {
            float dir = tileX(e, i, L) < mid ? -1f : 1f;
            if (n > 1) {
                if (i == 0) dir = -1f;
                else if (i == n - 1) dir = 1f;
            }
            e.flyDir[i] = dir;
        }
    }

    /** Enemies that are still a threat, i.e. excluding ones already flying apart. */
    int liveEnemies() {
        int n = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (!enemies.get(i).destroyed) n++;
        }
        return n;
    }

    /** Called once a stage's whole wave has been dealt with. */
    private void advanceStage() {
        // A wave cleared without a single wrong press earns the gold dumpling.
        if (missesThisStage == 0) {
            perfectBanner = PERFECT_TIME;
            if (sound != null) sound.achievement();
        }
        missesThisStage = 0;
        stage++;
        spawnedThisStage = 0;
        resolvedThisStage = 0;
        stageBanner = 1.6f;
        stageGap = STAGE_GAP;
        spawnTimer = 0.35f;
    }

    private void breach(Enemy e, Layout L) {
        if (target == e) target = null;
        resolvedThisStage++;
        lives--;
        combo = 0;
        shake = 1f;
        if (sound != null) sound.damage();
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
        e.need = new int[len];
        for (int i = 0; i < len; i++) {
            e.word[i] = rnd.nextInt(Glyph.COUNT);
            e.need[i] = 1;
        }
        // Spend a press budget on stacks, so a word never demands more than MAX_PRESSES
        // in total no matter how the extras land.
        int budget = MAX_PRESSES - len;
        float chance = stackChance();
        for (int i = 0; i < len && budget > 0; i++) {
            if (rnd.nextFloat() >= chance) continue;
            int extra = 1 + rnd.nextInt(Math.min(3, budget));
            e.need[i] += extra;
            budget -= extra;
        }
        e.pos = 0;
        e.done = 0;

        float half = L.wordWidth(len) / 2f;
        e.sway = Math.min(0.035f * L.w, Math.max(0f, (L.playRight - L.playLeft) / 2f - half - 4f));
        float lo = L.playLeft + half + e.sway;
        float hi = L.playRight - half - e.sway;
        e.baseX = hi > lo ? lo + rnd.nextFloat() * (hi - lo) : (L.playLeft + L.playRight) / 2f;
        e.phase = rnd.nextFloat() * 6.283f;
        // Start fully above the top edge so words visibly fly in rather than popping
        // into existence. travelSeconds still measures spawn -> danger line.
        e.y = -L.enemyR * 2.2f;
        e.enterT = 0f;
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
