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
    static final int TITLE = 0, PLAY = 1, OVER = 2, BONUS = 3;

    /** Presses needed to lift the steamer lid clear and free the dumpling. */
    static final int STEAMER_HITS = 20;
    /**
     * Length of the between-stages interlude. Short on purpose: at four seconds even a
     * moderate masher lands all twenty hits in one go, and the damage would never actually
     * accumulate across stages. At this length it takes roughly two interludes.
     */
    static final float BONUS_TIME = 2.2f;
    /** How long a stage title and its vignette stay on screen. */
    static final float BANNER_TIME = 1.9f;
    /** Tail of the interlude spent showing the run status before it fades out. */
    static final float BONUS_STATUS = 1.5f;
    /** Score awarded for freeing the dumpling. */
    static final int FREE_BONUS = 500;

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

        void gameStart();
        /** Stage cleared normally. Suppressed when {@link #powerClear()} fires instead. */
        void stageClear();
        /** A frenzy ran to its end and took the stage with it. */
        void powerClear();
        /** Swap the looping track to the faster driven variant, and back. */
        void frenzy(boolean on);
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

        /**
         * Tiles removed out of order — flung away by hand, or cleared en masse by MULTI.
         * Typing still advances {@link #pos} in order; it simply skips anything already gone.
         */
        boolean[] gone;
        /** 0..1 animation of a gone tile leaving, and the direction it left in. */
        float[] goneT;
        float[] goneDx, goneDy;

        int remaining() { return word.length - pos; }

        /** True when tile {@code i} is dealt with, whether typed in order or removed. */
        boolean resolved(int i) {
            return i < pos || gone[i];
        }

        /** Steps {@link #pos} over any tiles already gone. */
        void skipGone() {
            while (pos < word.length && gone[pos]) pos++;
        }

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
    /** Colour of the current full-screen flash. */
    int flashColor = FLASH_DAMAGE;
    /**
     * Sky tinting, 0..1, decaying. A correct press washes the clouds faintly with that
     * letter's colour; clearing a whole word floods them yellow.
     */
    float skyGlow;
    int skyGlowColor = FLASH_CLEAR;
    /** Highest proximity-to-danger across the field, 0..1. Drives the red screen pulse. */
    float warnLevel;

    // ---- between-stages minigame -------------------------------------------
    final Steamer steamer = new Steamer();
    float bonusTimer;

    // ---- powerup ------------------------------------------------------------
    /** At most one glowing letter on screen. */
    Power power;
    float powerTimer;
    /** Active mode, or -1. */
    int mode = -1;
    float modeLeft;
    /** True between the wave ending and the interlude opening. */
    boolean pendingBonus;
    /** Set when a frenzy ended the stage, so the interlude can run longer. */
    boolean stageByPower;
    /**
     * Drives the clouds. Separate from {@link #clock} because it runs fast during a frenzy;
     * accumulating it keeps the drift continuous rather than jumping when the frenzy ends.
     */
    float skyClock;

    boolean powerActive() { return modeLeft > 0f; }

    boolean flurry() { return powerActive() && mode == Power.FLURRY; }

    boolean flinging() { return powerActive() && mode == Power.FLING; }

    boolean multi() { return powerActive() && mode == Power.MULTI; }

    /**
     * Fall-speed multiplier. Applied per frame rather than baked into a word's speed at
     * spawn, so words already on screen speed up too and slow back down when the frenzy
     * ends — otherwise a frenzy would only affect whatever arrived during it.
     */
    float fallRate() {
        return powerActive() ? Power.FALL_RATE : 1f;
    }

    /** Scratch results from {@link #pickTile}. */
    Enemy pickedEnemy;
    int pickedTile = -1;

    // ---- FLING touch trail --------------------------------------------------
    /** Live finger position while dragging, set by the view. */
    boolean fingerDown;
    float fingerX, fingerY;
    /** Cleared when a frenzy starts; set once the player first touches during FLING. */
    boolean flingUsed;
    /** Where the instructional finger currently sits, for the renderer to follow. */
    float demoX, demoY;
    private float trailAcc;

    /** True while the interlude still accepts presses, before the status hold. */
    boolean bonusMashing() {
        return state == BONUS && bonusTimer > BONUS_STATUS;
    }

    /** True while the interlude is showing its end-of-round status. */
    boolean bonusStatus() {
        return state == BONUS && bonusTimer <= BONUS_STATUS;
    }

    /** True while the "drag a letter" demonstration should be on screen. */
    boolean showFlingHint() {
        return flinging() && !flingUsed;
    }

    /**
     * Emits the sparkle trail: under the finger once the player is dragging, and along the
     * demonstration path until they have. One rate for both, so the hint looks like the thing
     * it is teaching.
     */
    private void updateTrail(float dt, Layout L) {
        if (!flinging()) {
            fingerDown = false;
            return;
        }
        // Sweeps across the middle of the screen, briskly enough that the sparkles behind it
        // read as a ribbon rather than piling into a clump.
        demoX = L.w * 0.5f + (float) Math.sin(clock * 2.2f) * L.w * 0.26f;
        demoY = L.h * 0.45f + (float) Math.cos(clock * 1.5f) * L.h * 0.04f;

        float sx, sy;
        if (fingerDown) {
            sx = fingerX;
            sy = fingerY;
        } else if (!flingUsed) {
            sx = demoX;
            sy = demoY;
        } else {
            trailAcc = 0f;
            return;
        }

        trailAcc += dt;
        float per = 1f / 50f;
        while (trailAcc >= per) {
            trailAcc -= per;
            Fx.sparkle(this, rnd, sx, sy, L.enemyR * 0.85f, Glyph.cycle(clock * 1.6f));
        }
    }

    /** Ticks the frenzy timer, and the drifting letter that starts one. */
    private void updatePower(float dt, Layout L) {
        if (powerActive()) {
            modeLeft -= dt;
            if (modeLeft <= 0f) endPower(L);
        }

        if (power != null) {
            power.update(dt);
            if (power.spent() || (power.catchable() && power.offScreen(L, L.enemyR))) {
                power = null;
                powerTimer = Power.SPAWN_MIN
                        + rnd.nextFloat() * (Power.SPAWN_MAX - Power.SPAWN_MIN);
            }
            return;
        }

        // One frenzy at a time, and none while the wave is already over.
        if (powerActive() || stageGap > 0 || spawnedThisStage >= stageQuota()) return;
        powerTimer -= dt;
        if (powerTimer <= 0f) spawnPower(L);
    }

    /** Releases a powerup letter to drift across the sky. */
    private void spawnPower(Layout L) {
        Power w = new Power();
        w.glyph = rnd.nextInt(Glyph.COUNT);
        w.effect = rnd.nextInt(Power.COUNT);
        // Kept in the upper half of the descent, clear of the danger line.
        w.y = L.playTop + (L.dangerY - L.playTop) * (0.15f + rnd.nextFloat() * 0.30f);
        boolean toRight = rnd.nextBoolean();
        float span = L.w + L.enemyR * 5f;
        w.vx = (toRight ? 1f : -1f) * span / Power.CROSS_TIME;
        w.x = toRight ? -L.enemyR * 2.5f : L.w + L.enemyR * 2.5f;
        power = w;
    }

    /** Caught it: scores, then starts the frenzy the letter was carrying. */
    private void catchPower(Layout L) {
        power.hit = true;
        power.hitT = 0f;
        score += Power.SCORE;
        Fx.explode(this, rnd, power.x, power.y, L.enemyR * 2.2f, 26, 0xFFFFFFFF);
        startFrenzy(power.effect, L);
    }

    /**
     * Starts a frenzy of the given mode. Shared by catching a powerup and by the playtest
     * buttons, so a playtest exercises exactly what play does.
     */
    void startFrenzy(int effect, Layout L) {
        if (effect < 0 || effect >= Power.COUNT) return;
        mode = effect;
        modeLeft = Power.DURATION;
        flingUsed = false;
        fingerDown = false;
        shake = Math.max(shake, 0.5f);
        flash = Math.max(flash, 0.8f);
        flashColor = FLASH_CLEAR;
        skyGlow = 1f;
        skyGlowColor = Glyph.cycle(clock);
        if (sound != null) {
            sound.achievement();
            sound.frenzy(true);
        }
    }

    /**
     * Playtest hook: drops straight into a mode without waiting for a letter to drift past.
     * Goes through {@link #startFrenzy} so it is the real thing, not a simulation of it.
     */
    void playtestMode(int effect, Layout L) {
        if (state != PLAY) return;
        power = null;
        settingsOpen = false;
        startFrenzy(effect, L);
    }

    /**
     * The frenzy ran out. That clears the stage outright: everything still on the field is
     * destroyed and the wave counts as fully released, so the interlude follows.
     */
    private void endPower(Layout L) {
        mode = -1;
        modeLeft = 0f;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.destroyed) destroyWord(e, enemyCentreX(e), e.y, L);
        }
        spawnedThisStage = stageQuota();
        stageByPower = true;
        flash = Math.max(flash, 1f);
        flashColor = FLASH_CLEAR;
        skyGlow = 1f;
        skyGlowColor = FLASH_CLEAR;
        shake = Math.max(shake, 0.7f);
        fingerDown = false;
        if (sound != null) sound.frenzy(false);
    }

    /**
     * Smoothed x of the lock indicator, so it slides from letter to letter as you type
     * rather than teleporting. Owner is tracked separately: switching to a different word
     * should snap, not glide across the screen.
     */
    float caretX;
    Enemy caretOwner;
    final float[] keyPress = new float[Glyph.COUNT];
    final float[] keyBad = new float[Glyph.COUNT];

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
        float v = cloudY[layer][i] + skyClock * CLOUD_SPEED[layer];
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

    /** Flash and sky-glow colours. Plain data, so the rules stay Android-free. */
    static final int FLASH_DAMAGE = 0xFFFF7C9E;
    static final int FLASH_CLEAR = 0xFFFFE07A;
    /**
     * How strongly a single correct press tints the sky. Deliberately faint: this fires on
     * every press, so anything stronger reads as strobing rather than as a glow.
     */
    static final float GLOW_HIT = 0.35f;

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

    /** Concurrent words allowed right now; a frenzy lets four times as many pile up. */
    int crowdCap() {
        return powerActive() ? (int) (maxEnemies() * Power.CROWD_RATE) : maxEnemies();
    }

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
        skyGlow = 0;
        steamer.reset();
        bonusTimer = 0;
        power = null;
        mode = -1;
        modeLeft = 0;
        stageByPower = false;
        powerTimer = Power.SPAWN_MIN;
        pendingBonus = false;
        stageBanner = BANNER_TIME;
        if (sound != null) {
            sound.frenzy(false);
            sound.gameStart();
        }
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

        // The drifting powerup outranks the words — but only when nothing is engaged, so it
        // can never steal a press out of a word you are part-way through.
        if (target == null && power != null && power.catchable()
                && (power.glyph == g || flurry())) {
            hits++;
            combo++;
            if (combo > maxCombo) maxCombo = combo;
            catchPower(L);
            return true;
        }

        // MULTI: one press takes every matching letter on the field, engaged or not.
        if (multi()) return multiStrike(g, L);

        if (target == null) {
            // Engage the most urgent match: the word lowest on screen, i.e. closest to
            // reaching the player, whose next-needed letter is g. During FLURRY any letter
            // matches, so this just takes the most urgent word outright.
            Enemy pick = null;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (!e.typeable()) continue;
                if (!flurry() && e.word[e.pos] != g) continue;
                if (pick == null || e.y > pick.y) pick = e;
            }
            if (pick == null) {
                miss(g);
                return false;
            }
            target = pick;
        } else if (!flurry() && target.word[target.pos] != g) {
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
        // Under FLURRY the pressed key is a wildcard, so the shot and the wash take the
        // letter actually being hit rather than whatever was pressed.
        int lit = e.word[struck];

        // A stacked tile absorbs several presses of the same letter before it clears.
        if (sound != null) sound.squish(lit, pressesLeft(e, struck));
        e.done++;
        if (e.done >= e.need[struck]) {
            e.pos++;
            e.skipGone();
            e.done = 0;
        }
        e.hitPulse = 1f;
        e.hitIndex = struck;
        // Faint wash of the struck letter's own colour across the sky.
        skyGlow = Math.max(skyGlow, GLOW_HIT);
        skyGlowColor = Glyph.COLOR[lit];
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
        s.glyph = lit;
        s.target = e;
        s.kill = kill;
        s.tileIndex = struck;
        s.dur = 0.13f;
        shots.add(s);
        return true;
    }

    /**
     * MULTI: one press takes every tile of that letter across every word, engaged or not.
     * Scores per tile and counts as a single hit, so it cannot inflate accuracy.
     */
    private boolean multiStrike(int g, Layout L) {
        int taken = 0;
        // Downward: removing the last tile of a word destroys it and mutates the list.
        for (int n = enemies.size() - 1; n >= 0; n--) {
            Enemy e = enemies.get(n);
            if (!e.typeable()) continue;
            for (int i = e.word.length - 1; i >= e.pos; i--) {
                if (e.gone[i] || e.word[i] != g) continue;
                removeTile(e, i, 0f, -1f, L);
                taken++;
                if (!e.typeable()) break;
            }
        }
        if (taken == 0) {
            miss(g);
            return false;
        }
        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        score += 8 * taken;
        shake = Math.max(shake, 0.2f + 0.04f * taken);
        if (sound != null) sound.squish(g, 1);
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

    /** Presses still owed on tile {@code i}; 0 once it is resolved, however that happened. */
    int pressesLeft(Enemy e, int i) {
        if (e.resolved(i)) return 0;
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
        // Accumulated, not derived from clock, so the frenzy's faster drift does not make the
        // sky jump when it starts or stops.
        skyClock += dt * (powerActive() ? Power.SKY_RATE : 1f);

        for (int i = 0; i < Glyph.COUNT; i++) {
            keyPress[i] = decay(keyPress[i], dt * 5.5f);
            keyBad[i] = decay(keyBad[i], dt * 3.2f);
        }
        shake = decay(shake, dt * 2.6f);
        flash = decay(flash, dt * 2.2f);
        skyGlow = decay(skyGlow, dt * 2.4f);
        stageBanner = decay(stageBanner, dt);
        perfectBanner = decay(perfectBanner, dt);

        // Cleared here, above the early return, and raised again by the enemy loop below.
        // Resetting it after the return left the red edge glow stuck on at whatever the
        // last lunging word set it to, all the way through the game-over screen.
        warnLevel = 0f;

        Fx.updateParticles(this, dt);
        Fx.updateShots(this, dt, L);

        if (state == BONUS) {
            steamer.update(dt);
            bonusTimer -= dt;
            if (bonusTimer <= 0) {
                // The interlude is what sat between the waves; the stage itself turns over
                // on the way out of it.
                advanceStage();
                state = PLAY;
                time = 0;
            }
            return;
        }

        if (state != PLAY) return;

        // Holding between the wave ending and the interlude opening, so the flawless-wave
        // dumpling gets the screen to itself. Nothing spawns and nothing falls; the field is
        // already empty, which is what let the wave end.
        if (pendingBonus) {
            if (perfectBanner <= 0f) {
                pendingBonus = false;
                enterBonus();
            }
            return;
        }

        updatePower(dt, L);
        updateTrail(dt, L);

        // Stages are discrete waves: a stage releases exactly stageQuota() words, and the
        // next stage cannot start arriving until the field is completely clear.
        if (stageGap > 0) {
            stageGap -= dt;
        } else if (powerActive() || spawnedThisStage < stageQuota()) {
            spawnTimer -= dt;
            // Counted against live words only: a word already flying apart is no longer
            // occupying the field as far as pacing is concerned. During a frenzy the quota
            // is ignored: words keep coming until the timer runs out and ends the stage.
            if (spawnTimer <= 0 && liveEnemies() < crowdCap()) {
                spawn(L);
                if (!powerActive()) spawnedThisStage++;
                spawnTimer = spawnInterval() / (powerActive() ? Power.SPAWN_RATE : 1f);
            }
        } else if (stageCleared()) {
            beginStageEnd();
            return;
        }

        updateCaret(dt, L);

        float band = Math.max(1f, (L.dangerY - L.playTop) * WARN_BAND);

        for (int i = enemies.size() - 1; i >= 0; i--) {
            // A fatal breach clears the whole field from under this loop, and walking
            // downward is not enough on its own: the lower indices are gone too.
            if (i >= enemies.size()) continue;
            Enemy e = enemies.get(i);
            e.hitPulse = decay(e.hitPulse, dt * 6.5f);
            e.failPulse = decay(e.failPulse, dt * 2.8f);
            for (int k = 0; k < e.word.length; k++) {
                if (e.gone[k] && e.goneT[k] < 1f) e.goneT[k] = Math.min(1f, e.goneT[k] + dt * 3.2f);
            }
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
                flashColor = FLASH_DAMAGE;
                shake = Math.max(shake, 0.35f * (e.attackT / ATTACK_TIME));
                if (e.attackT >= ATTACK_TIME) {
                    // Unlist first: a fatal breach clears the whole field, which would
                    // invalidate this index.
                    enemies.remove(i);
                    breach(e, L);
                    // Nothing left to simulate once the run is over.
                    if (state != PLAY) return;
                }
                continue;
            }

            e.y += e.speed * fallRate() * dt;
            e.warn = clamp01((e.y - (L.dangerY - band)) / band);
            if (e.warn > warnLevel) warnLevel = e.warn;

            if (e.y + L.enemyR >= L.dangerY) {
                e.attacking = true;
                e.attackT = 0;
                if (target == e) target = null;
            }
        }
    }


    void impact(Shot s, Layout L) {
        Enemy e = s.target;
        if (s.kill && e != null && enemies.contains(e) && !e.destroyed) {
            destroyWord(e, s.tx, s.ty, L);
        } else {
            Fx.explode(this, rnd, s.tx, s.ty, L.enemyR * 0.7f, 7, Glyph.COLOR[s.glyph]);
        }
    }

    /**
     * Credits a finished word and starts it flying apart. Shared by the ordinary typed kill,
     * by out-of-order removal (flinging and MULTI), and by the end of a frenzy.
     *
     * @param px,py where the burst originates, usually the last tile struck
     */
    void destroyWord(Enemy e, float px, float py, Layout L) {
        // The word is credited now but stays listed until it has flown apart, so anything
        // gated on the field being clear waits for the animation.
        e.destroyed = true;
        e.destroyT = 0f;
        e.dying = false;
        if (target == e) target = null;
        computeFlyDirs(e, L);

        Fx.explode(this, rnd, enemyCentreX(e), e.y, L.enemyR * 1.5f, e.word.length + 8,
                0xFFFFFFFF);
        for (int i = 0; i < e.word.length; i++) {
            Fx.explode(this, rnd, px, py, L.enemyR, 4, Glyph.COLOR[e.word[i]]);
        }
        shake = Math.max(shake, 0.30f);
        // Clearing a word flashes the screen and floods the sky yellow.
        flash = Math.max(flash, 0.60f);
        flashColor = FLASH_CLEAR;
        skyGlow = 1f;
        skyGlowColor = FLASH_CLEAR;
        kills++;
        resolvedThisStage++;
        // Scored per press, so a stacked word is worth what it cost to clear.
        score += 25 * e.totalPresses();
        if (sound != null) sound.clearWord();
    }

    /**
     * Removes tile {@code i} out of order, sending it off along dx,dy. Used by flinging and
     * by MULTI. Finishes the word if that was the last tile left.
     */
    void removeTile(Enemy e, int i, float dx, float dy, Layout L) {
        if (e.gone[i] || i < e.pos || !e.typeable()) return;
        e.gone[i] = true;
        e.goneT[i] = 0f;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        e.goneDx[i] = len > 1e-4f ? dx / len : 0f;
        e.goneDy[i] = len > 1e-4f ? dy / len : -1f;
        float tx = tileX(e, i, L);
        Fx.explode(this, rnd, tx, e.y, L.enemyR * 0.9f, 6, Glyph.COLOR[e.word[i]]);
        skyGlow = Math.max(skyGlow, GLOW_HIT);
        skyGlowColor = Glyph.COLOR[e.word[i]];

        e.skipGone();
        if (e.pos >= e.word.length) destroyWord(e, tx, e.y, L);
    }

    /**
     * The topmost tile under a point, for flinging. Results land in {@link #pickedEnemy} and
     * {@link #pickedTile} to avoid allocating on every touch move.
     */
    boolean pickTile(float x, float y, Layout L) {
        pickedEnemy = null;
        pickedTile = -1;
        float best = Float.MAX_VALUE;
        for (int n = 0; n < enemies.size(); n++) {
            Enemy e = enemies.get(n);
            if (!e.typeable()) continue;
            for (int i = e.pos; i < e.word.length; i++) {
                if (e.gone[i]) continue;
                float dx = x - tileX(e, i, L), dy = y - e.y;
                float d = dx * dx + dy * dy;
                float r = L.enemyR * 1.5f;
                if (d <= r * r && d < best) {
                    best = d;
                    pickedEnemy = e;
                    pickedTile = i;
                }
            }
        }
        return pickedEnemy != null;
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

    /** Drops into the between-stages minigame once the wave is clear. */
    private void enterBonus() {
        state = BONUS;
        time = 0;
        // A stage ended by a frenzy earns a longer go at the steamer.
        // One timer for both phases: mashing while it is above BONUS_STATUS, then the status
        // hold below that. Keeping it single means the fade-out has one thing to read.
        bonusTimer = BONUS_TIME + BONUS_STATUS + (stageByPower ? Power.BONUS_EXTRA : 0f);
        steamer.lidPulse = 0;
        steamer.flash = 0;
        steamer.pick(rnd);
        target = null;
        caretOwner = null;
        power = null;
        if (sound != null) {
            // The frenzy tone replaces the ordinary one rather than stacking with it.
            if (stageByPower) sound.powerClear();
            else sound.stageClear();
        }
        stageByPower = false;
    }

    /**
     * A press during the interlude. Any of the six keys counts — this is a mash, not a
     * typing test — so it deliberately leaves hits, misses and combo alone, otherwise
     * mashing would inflate the accuracy readout.
     */
    void tapBonus(int g) {
        if (!bonusMashing()) return;
        keyPress[g] = 1f;

        int r = steamer.press(g);
        if (r == Steamer.WRONG) {
            // Sounds wrong but is not counted as a miss: this is not a typing test, and it
            // must not reach the accuracy readout.
            keyBad[g] = 1f;
            if (sound != null) sound.wrong();
            return;
        }
        if (sound != null) sound.squish(g, 1);
        if (r != Steamer.FREED) return;

        score += FREE_BONUS;
        if (lives < START_LIVES) lives++;
        // Hold the interlude open long enough to watch it escape, plus the status beat.
        bonusTimer = Math.max(bonusTimer, steamer.freedT + BONUS_STATUS + 0.2f);
        if (sound != null) sound.achievement();
    }

    /**
     * The wave is done. Awards the flawless-wave dumpling and holds here until it has
     * finished, so the interlude opens after that celebration rather than on top of it.
     */
    private void beginStageEnd() {
        if (missesThisStage == 0) {
            perfectBanner = PERFECT_TIME;
            if (sound != null) sound.achievement();
        }
        missesThisStage = 0;
        pendingBonus = true;
    }

    /** Called on the way out of the interlude. */
    private void advanceStage() {
        stage++;
        spawnedThisStage = 0;
        resolvedThisStage = 0;
        stageBanner = BANNER_TIME;
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
        flashColor = FLASH_DAMAGE;
        Fx.explode(this, rnd, enemyCentreX(e), L.dangerY, L.enemyR * 2f, 16, 0xFFFF7A9E);
        if (lives <= 0) {
            state = OVER;
            time = 0;
            // Clear the field so the summary screen is readable; particles stay for the bang.
            enemies.clear();
            shots.clear();
            target = null;
            // Dying mid-frenzy has to end the frenzy here: updatePower only runs during
            // PLAY, so otherwise the mode would stay live and the driven music would carry
            // on into the game-over screen.
            if (powerActive()) {
                mode = -1;
                modeLeft = 0f;
                fingerDown = false;
                if (sound != null) sound.frenzy(false);
            }
            power = null;
            if (score > best) {
                best = score;
                if (store != null) store.saveBest(best);
            }
        }
    }

    private void spawn(Layout L) {
        Enemy e = new Enemy();
        int len = minWordLen() + rnd.nextInt(maxWordLen() - minWordLen() + 1);
        Words.fill(e, len, stackChance(), rnd);

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
