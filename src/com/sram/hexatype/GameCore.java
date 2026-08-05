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
    /**
     * Opening of the interlude, spent spinning to the pair of keys this round will use. The
     * pair is already chosen when the spinner starts; this is presentation, and mashing is
     * closed for the whole of it.
     */
    static final float BONUS_ROLL = 2.0f;
    /**
     * Beat between the mash timer expiring and anything starting to fade. Without it the
     * scene begins dissolving on the same frame the clock hits zero, and there is nowhere to
     * register that time is what ended the round.
     */
    static final float BONUS_HOLD = 1.0f;
    /** How long a stage title and its vignette stay on screen. */
    static final float BANNER_TIME = 1.9f;
    /** Tail of the interlude spent showing the run status before it fades out. */
    static final float BONUS_STATUS = 1.5f;
    /** Score awarded for freeing the dumpling. */
    static final int FREE_BONUS = 500;
    /**
     * Consolation for freeing a squishy you already own. Well under {@link #FREE_BONUS} so a
     * duplicate still reads as the lesser outcome.
     */
    static final int DUPE_BONUS = 150;
    /** How long the game-over screen ignores presses, so a death is not skipped by reflex. */
    static final float OVER_GRACE = 0.6f;
    /**
     * The parade that closes an interlude something was won in: the collection marches in from
     * the left, the new one joins the end of the line, and they all march off to the right.
     * Play resumes when they are gone, not before.
     *
     * Half again as long as it first was, along with the escape before it: at the original
     * length the whole win read as rushed, and the moment the newcomer actually joins the line
     * is the one thing in the sequence worth lingering on.
     */
    static final float PARADE_TIME = 4.5f;

    /** Persistence seam; the Activity backs this with SharedPreferences. */
    interface Store {
        int loadBest();
        void saveBest(int best);
        float loadSpeed();
        void saveSpeed(float speed);
        int loadBgm();
        void saveBgm(int choice);
        /** The collected-squishy bitmask; see {@link Collect}. */
        long loadCollected();
        void saveCollected(long owned);
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
        /** A letter cut by the FLING blade. Fires several times per swipe, so it is short. */
        void chop();
        /** One hop of a MULTI chain. @param hop 1-based, so the crack can climb with the chain */
        void zap(int hop);
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
    int score, best, lives, stage, combo, maxCombo;
    /** Words squished this run. The game-over screen calls them squishes, so this does too. */
    int squishes;
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
    /** Last character the spinner ticked on, so each step sounds exactly once. */
    private int rollTick = -1;
    /**
     * Seconds of parade left. Set the moment a prize is won but deliberately not ticked until
     * the interlude's own countdown has run out, which is what lets it be both "something was
     * won this round" and "the parade still has to happen" without a second flag.
     */
    float paradeTimer;

    // ---- the collection ----------------------------------------------------
    /**
     * Which squishies have been collected, one bit per {@link Collect} entry. The only state
     * that survives a run other than the best score, so it is written through to the store
     * the moment it changes rather than at the end of a game — a run that is force-quit must
     * not lose the thing it won.
     */
    long collected;
    /** What the last opened steamer handed over, or -1. Reset when a run starts. */
    int prize = -1;
    /** False when {@link #prize} was already in the case. */
    boolean prizeNew;

    /** Which entry the display case is showing, and the slide left over from the last scroll. */
    int caseIndex;
    /** -1..1, decaying to 0: the shelf easing into place after a scroll. */
    float caseSlide;

    /** Which entry's story is on screen, or -1. Title screen only. */
    int story = -1;
    /** Seconds the story has been open; drives the panel opening and its looping scene. */
    float storyT;

    boolean storyOpen() {
        return story >= 0;
    }

    /**
     * Opens the focused entry's story. Only for something collected: an uncollected entry
     * withholds its name on the shelf, so telling you about its family would give it away.
     */
    void openStory() {
        if (state != TITLE || storyOpen() || !Collect.has(collected, caseIndex)) return;
        story = caseIndex;
        storyT = 0f;
        if (sound != null) sound.achievement();
    }

    void closeStory() {
        story = -1;
        storyT = 0f;
    }

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

    boolean team() { return powerActive() && mode == Power.TEAM; }

    /** The squishy that fights during TEAM SQUISH. */
    final Buddy buddy = new Buddy();

    /**
     * Fall-speed multiplier. Applied per frame rather than baked into a word's speed at
     * spawn, so words already on screen speed up too and slow back down when the frenzy
     * ends — otherwise a frenzy would only affect whatever arrived during it.
     */
    float fallRate() {
        return powerActive() ? Power.FALL_RATE : 1f;
    }

    // ---- MULTI chain --------------------------------------------------------
    /**
     * Most hops one chain can have. The field can hold a few dozen tiles at frenzy crowd
     * levels, of which roughly a sixth match any one letter.
     */
    static final int CHAIN_MAX = 32;
    /** What the first hop is worth. The nth hop is worth n times this, so a cluster compounds. */
    static final int CHAIN_STEP = 8;
    /** How long a chain stays on screen. */
    static final float CHAIN_TIME = 0.6f;
    /** Fraction of that spent revealing hops; the rest is the fade. */
    static final float CHAIN_REVEAL = 0.55f;

    /** Where each hop of the last chain struck, in hop order. */
    final float[] chainX = new float[CHAIN_MAX];
    final float[] chainY = new float[CHAIN_MAX];
    /** Hops in the last chain, the letter it ran on, and what it paid. */
    int chainLen, chainGlyph, chainScore;
    /** Hops revealed so far; drives both the drawing and the sound. */
    int chainShown;
    /** Counts down while the chain is on screen. */
    float chainT;

    // ---- FLING blade --------------------------------------------------------
    /** Live finger position while dragging, set by the view. */
    boolean fingerDown;
    float fingerX, fingerY;
    /** Cleared when a frenzy starts; set once the player first touches during FLING. */
    boolean flingUsed;
    /** Where the instructional finger currently sits, for the renderer to follow. */
    float demoX, demoY;
    private float trailAcc;
    /** Where the trail was emitted from last frame, so a fast swipe still leaves a ribbon. */
    private float trailPrevX, trailPrevY;
    /**
     * The stretch the blade covered over the last frame, for the renderer to draw the edge
     * along. Kept separately from {@link #trailPrevX} because that one is brought up to the
     * finger before the frame is drawn, which would leave the edge with no length at all.
     */
    float bladeFromX, bladeFromY;

    /**
     * How wide the blade cuts, in tile radii. Generous: this is a swipe through a moving field
     * of small targets, and the whole point of the mode is that it feels powerful.
     */
    static final float BLADE = 1.15f;
    /** Words a single stroke has destroyed, held until the next stroke begins. */
    int strokeKills;
    /** Tiles a single stroke has cut, same lifetime. */
    int strokeCuts;
    /** Words in one stroke that earn the slow-motion beat. */
    static final int SLOW_KILLS = 2;
    /** How long that beat lasts, in real seconds. Short: it is an impact, not an interlude. */
    static final float SLOW_TIME = 0.28f;
    /** Fraction of normal speed the world runs at during it. */
    static final float SLOW_RATE = 0.32f;
    /** Seconds of slow motion left. */
    float slowdown;
    /**
     * How long the multi-word readout stays up. Deliberately longer than the beat: the
     * slowdown wants to be brief or it drags, and the number wants long enough to read.
     */
    static final float SLICE_CALL_TIME = 1.1f;
    /** Seconds the slice readout has left. */
    float sliceCall;

    /** How much of normal speed the simulation is running at. */
    float timeScale() {
        return slowdown > 0f ? SLOW_RATE : 1f;
    }

    /**
     * The interlude has two paths and exactly one phase is true at any moment.
     *
     * Lost round: spinner, mash, a beat on zero, then the status report whose tail fades out.
     * Won round: the mash is over the instant it is won, so it is the escape animation and then
     * the parade. The first four run off the one countdown — the fade-out then has one value to
     * read, and every boundary is a comparison rather than a transition that could be missed.
     * Only the first boundary varies, since a frenzy buys a longer mash, so it is stored.
     */
    float bonusRollEnd;

    /** Timer value at which the mash gives way to the beat on zero. */
    private static final float MASH_END = BONUS_HOLD + BONUS_STATUS;

    /** Total length of an ordinary interlude: all four phases, without a frenzy's extra. */
    static float bonusLength() {
        return BONUS_ROLL + BONUS_TIME + BONUS_HOLD + BONUS_STATUS;
    }

    /** True while the spinner is still settling on this round's pair. */
    boolean bonusRolling() {
        return state == BONUS && bonusTimer > bonusRollEnd;
    }

    /** True while the interlude accepts presses. */
    boolean bonusMashing() {
        return state == BONUS && !bonusPrizeWon()
                && bonusTimer <= bonusRollEnd && bonusTimer > MASH_END;
    }

    /** True during the beat after the clock runs out, before anything fades. */
    boolean bonusHolding() {
        return state == BONUS && !bonusPrizeWon()
                && bonusTimer <= MASH_END && bonusTimer > BONUS_STATUS;
    }

    /** True while a won prize is climbing out, which is all that is left of a won round. */
    boolean bonusEscape() {
        return state == BONUS && bonusPrizeWon() && bonusTimer > 0f;
    }

    /**
     * True while the interlude is showing its end-of-round status.
     *
     * Never on a winning round: the parade closes those, and it announces the stage itself. The
     * report used to run in between and put a page of numbers between winning something and
     * watching it join the line.
     */
    boolean bonusStatus() {
        return state == BONUS && !bonusPrizeWon() && bonusTimer <= BONUS_STATUS;
    }

    /** True once something has been won this interlude, for the whole rest of it. */
    boolean bonusPrizeWon() {
        return state == BONUS && paradeTimer > 0f;
    }

    /** True while the collection is parading, after every other phase and before play. */
    boolean bonusParading() {
        return state == BONUS && bonusTimer <= 0f && paradeTimer > 0f;
    }

    /** 0..1 through the parade. */
    float paradeProgress() {
        if (!bonusParading()) return 0f;
        return 1f - paradeTimer / PARADE_TIME;
    }

    /** 0..1 through the spinner, reaching 1 the moment it lands. */
    float rollProgress() {
        if (state != BONUS || bonusTimer <= bonusRollEnd) return 1f;
        return 1f - (bonusTimer - bonusRollEnd) / BONUS_ROLL;
    }

    /**
     * Seconds of mashing left, for the countdown. Held at the full length through the spinner
     * so the clock shows what you are about to get rather than counting down behind it.
     */
    float bonusLeft() {
        float mash = bonusRollEnd - MASH_END;
        float left = bonusTimer - MASH_END;
        return left < 0f ? 0f : left > mash ? mash : left;
    }

    /**
     * The pair the interlude is showing right now: spinning while the spinner runs, settled
     * afterwards. The renderer and the key deck both read these, so the deck rings whichever
     * keys the spinner is on and cannot disagree with it.
     */
    int bonusLeftKey() {
        return steamer.shownLeft(rollProgress());
    }

    int bonusRightKey() {
        return steamer.shownRight(rollProgress());
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

        // Captured before the emission below advances trailPrev to the finger.
        bladeFromX = trailPrevX;
        bladeFromY = trailPrevY;

        trailAcc += dt;
        float per = 1f / TRAIL_RATE;
        int emit = 0;
        while (trailAcc >= per) {
            trailAcc -= per;
            emit++;
        }
        // Spread along the path swept since the last frame rather than all dropped at the
        // current point: a quick swipe otherwise leaves a dotted line instead of a blade trail.
        for (int k = 0; k < emit; k++) {
            float f = emit == 1 ? 1f : (float) k / (emit - 1);
            Fx.sparkle(this, rnd, trailPrevX + (sx - trailPrevX) * f,
                    trailPrevY + (sy - trailPrevY) * f, L.enemyR * 0.85f,
                    Glyph.cycle(clock * 1.6f));
        }
        trailPrevX = sx;
        trailPrevY = sy;
    }

    /** Sparkles a second, along the blade. */
    static final float TRAIL_RATE = 100f;

    /**
     * Plays back the last chain, one hop at a time, sounding each as it lands.
     *
     * The hops were all resolved on the press; this is presentation. Each one cracks, and the
     * crack climbs in pitch as the chain runs on.
     */
    private void updateChain(float dt) {
        if (chainT <= 0f) return;
        chainT = Math.max(0f, chainT - dt);
        float done = 1f - chainT / CHAIN_TIME;
        int want = (int) Math.ceil(chainLen * Math.min(1f, done / CHAIN_REVEAL));
        while (chainShown < want) {
            chainShown++;
            if (sound != null) sound.zap(chainShown);
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
        w.effect = rollEffect();
        // Kept in the upper half of the descent, clear of the danger line.
        w.y = L.playTop + (L.dangerY - L.playTop) * (0.15f + rnd.nextFloat() * 0.30f);
        boolean toRight = rnd.nextBoolean();
        float span = L.w + L.enemyR * 5f;
        w.vx = (toRight ? 1f : -1f) * span / Power.CROSS_TIME;
        w.x = toRight ? -L.enemyR * 2.5f : L.w + L.enemyR * 2.5f;
        power = w;
    }

    /**
     * Which frenzy a drifting letter carries.
     *
     * TEAM SQUISH stars one of your own collectibles, so it cannot turn up before there is one.
     * Excluded by rolling over one fewer mode, which works because it is the last index — see
     * {@link Power#TEAM}.
     */
    private int rollEffect() {
        return rnd.nextInt(Collect.owned(collected) > 0 ? Power.COUNT : Power.COUNT - 1);
    }

    /** A random entry from the display case, or -1 when it is empty. */
    int anyCollected() {
        int have = Collect.owned(collected);
        if (have == 0) return -1;
        int nth = rnd.nextInt(have);
        for (int i = 0; i < Collect.COUNT; i++) {
            if (!Collect.has(collected, i)) continue;
            if (nth-- == 0) return i;
        }
        return -1;
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
        // TEAM SQUISH has nobody to field with an empty case. The drifting letter never rolls it
        // then, so only the playtest chips can ask for it, and refusing is clearer than quietly
        // substituting a different mode.
        int entry = effect == Power.TEAM ? anyCollected() : -1;
        if (effect == Power.TEAM && entry < 0) return;
        mode = effect;
        modeLeft = Power.DURATION;
        flingUsed = false;
        fingerDown = false;
        strokeKills = 0;
        strokeCuts = 0;
        if (entry >= 0) buddy.enter(entry, L, rnd);
        else buddy.leave();
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
        buddy.leave();
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
            // Masked: a store that hands back junk in the high bits must not make
            // Collect.owned() report more than there are entries.
            collected = store.loadCollected() & Collect.MASK;
        }
    }

    static float clampSpeed(float v) {
        if (v != v) return 1f;                       // NaN from a corrupt store
        return v < SPEED_MIN ? SPEED_MIN : v > SPEED_MAX ? SPEED_MAX : v;
    }

    // ---- settings -----------------------------------------------------------

    void openSettings() {
        settingsOpen = true;
        clearArmed = false;
    }

    void closeSettings() {
        settingsOpen = false;
        clearArmed = false;
    }

    /**
     * True once the clear-case button has been tapped and is waiting for a second tap. The
     * collection is the one thing here that took several runs to build, so wiping it is
     * behind a confirmation rather than a single stray tap in a panel full of other buttons.
     */
    boolean clearArmed;

    /** Tap on the clear-case button: arms it, then on the second tap empties the case. */
    void tapClearCase() {
        if (!clearArmed) {
            clearArmed = true;
            return;
        }
        clearArmed = false;
        collected = 0L;
        prize = -1;
        closeStory();
        caseIndex = 0;
        caseSlide = 0f;
        if (store != null) store.saveCollected(0L);
        if (sound != null) sound.wrong();
    }

    /**
     * Moves the display case one entry. Wraps, so the strip is a loop and neither outer key
     * ever does nothing.
     */
    void scrollCase(int dir) {
        if (dir == 0 || storyOpen()) return;
        caseIndex = Showcase.wrap(caseIndex + (dir > 0 ? 1 : -1));
        // Full slide, decaying to zero: the shelf glides in from the side it came from.
        caseSlide = dir > 0 ? 1f : -1f;
        if (sound != null) sound.squish(caseIndex % Glyph.COUNT, 1);
    }

    void setSpeed(float v) {
        speed = clampSpeed(v);
        if (store != null) store.saveSpeed(speed);
    }

    /**
     * Announces the loaded music choice to the audio backend.
     *
     * Separate from the constructor because {@link #sound} is attached afterwards, and separate
     * from {@link #setBgm} because nothing here changes — this only tells the backend what was
     * already loaded. Without it the choice was never announced at all: the backend fell back
     * to the first synth track on every launch, so the stored preference and the first-run
     * default both did nothing until the player opened settings and picked something.
     */
    void startMusic() {
        if (sound != null) sound.selectMusic(bgmChoice);
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
        squishes = 0;
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
        closeStory();
        // The collection itself survives; only the "you just won this" banner is per-run.
        prize = -1;
        prizeNew = false;
        paradeTimer = 0f;
        power = null;
        mode = -1;
        modeLeft = 0;
        buddy.leave();
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
        // The shelf must not open part-way through a slide left over from the last visit.
        caseSlide = 0f;
        closeStory();
    }

    /**
     * True for the four inner keys, which are the ones that start a run. The outer two are
     * reserved for the display case, so browsing the collection can never trip a game.
     */
    static boolean startKey(int g) {
        return g > 0 && g < Glyph.COUNT - 1;
    }

    /**
     * A key press on the title or game-over screen. The inner four start; the outer two
     * scroll the display case on the title, and go back to it from game over — which is the
     * only way to reach the case again once a run has begun.
     */
    void screenKey(int g) {
        if (g < 0 || g >= Glyph.COUNT) return;
        if (state == OVER && time <= OVER_GRACE) return;
        keyPress[g] = 1f;
        // A story on screen swallows the first press. Without this an inner key would start a
        // run from behind the panel, which is the one thing a modal must not allow.
        if (storyOpen()) {
            closeStory();
            return;
        }
        if (startKey(g)) {
            startGame();
        } else if (state == OVER) {
            toTitle();
        } else {
            scrollCase(g == 0 ? -1 : 1);
        }
    }

    // ---- input --------------------------------------------------------------

    /** Player pressed key {@code g}. Returns true when it advanced a word. */
    boolean tapKey(int g, Layout L) {
        if (state != PLAY) {
            if (state != BONUS) screenKey(g);
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

        // MULTI: one press chains through every matching letter on the field.
        if (multi()) return multiStrike(g, L);

        // TEAM SQUISH: the press does not type, it aims the squishy.
        if (team()) return teamStrike(g, L);

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
            // Doomed from this press, so it stops reading as a threat now rather than when
            // the shot lands: the update loop skips dying words, freezing whatever warning
            // state they held for the shot's whole flight.
            e.warn = 0f;
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
     * MULTI: a chain through every tile of that letter on the field, hop by hop, each hop worth
     * more than the last. It counts as one hit, so it cannot inflate accuracy.
     *
     * Everything is resolved here on the press and then played back by {@link #updateChain}.
     * Animating the removals themselves would mean holding references to tiles that a fall, a
     * word finishing or the frenzy ending could invalidate underneath the chain — which is the
     * one class of bug this file has been bitten by most. This way nothing is left pending.
     *
     * The escalating value is what gives the mode a decision it did not have: every key used to
     * be equally good, and the answer now depends on which letter has the biggest cluster.
     */
    private boolean multiStrike(int g, Layout L) {
        Enemy[] who = new Enemy[CHAIN_MAX];
        int[] tile = new int[CHAIN_MAX];
        float[] tx = new float[CHAIN_MAX];
        float[] ty = new float[CHAIN_MAX];
        int n = 0;
        for (int k = 0; k < enemies.size() && n < CHAIN_MAX; k++) {
            Enemy e = enemies.get(k);
            if (!e.typeable()) continue;
            for (int i = e.pos; i < e.word.length && n < CHAIN_MAX; i++) {
                if (e.gone[i] || e.word[i] != g) continue;
                who[n] = e;
                tile[n] = i;
                tx[n] = tileX(e, i, L);
                ty[n] = e.y;
                n++;
            }
        }
        if (n == 0) {
            miss(g);
            return false;
        }

        // Hop order: start at the most urgent match, the lowest on screen, then always take the
        // nearest one not yet visited. A chain that criss-crosses the field is unreadable, and
        // this is the cheapest walk that never does.
        int[] order = new int[n];
        boolean[] used = new boolean[n];
        int at = 0;
        for (int i = 1; i < n; i++) if (ty[i] > ty[at]) at = i;
        order[0] = at;
        used[at] = true;
        for (int h = 1; h < n; h++) {
            int best = -1;
            float bestD = Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (used[i]) continue;
                float dx = tx[i] - tx[at], dy = ty[i] - ty[at];
                float d = dx * dx + dy * dy;
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            order[h] = best;
            used[best] = true;
            at = best;
        }

        chainGlyph = g;
        chainLen = 0;
        chainScore = 0;
        for (int h = 0; h < n; h++) {
            int j = order[h];
            Enemy e = who[j];
            // An earlier hop may have finished this word, which takes the rest of it with it.
            if (!e.typeable() || e.gone[tile[j]] || tile[j] < e.pos) continue;
            chainX[chainLen] = tx[j];
            chainY[chainLen] = ty[j];
            chainLen++;
            chainScore += CHAIN_STEP * chainLen;
            removeTile(e, tile[j], 0f, -1f, L);
        }
        score += chainScore;
        chainT = CHAIN_TIME;
        chainShown = 0;

        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        shake = Math.max(shake, 0.18f + 0.05f * chainLen);
        skyGlow = Math.max(skyGlow, GLOW_HIT);
        skyGlowColor = Glyph.COLOR[g];
        // No sound here: updateChain fires one per hop as it is revealed, which is what makes
        // the length audible. A single squish for nine tiles was half of why this felt flat.
        return true;
    }

    /**
     * TEAM SQUISH: sends the squishy at the word this press would have attacked, and it takes
     * the whole word rather than one letter.
     *
     * The pressed key still means something — it picks the most urgent word wanting that letter
     * — but any key works, falling back to whatever is most urgent, so the mode never punishes
     * a press for being the wrong one.
     */
    private boolean teamStrike(int g, Layout L) {
        Enemy pick = null;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.typeable() || e.word[e.pos] != g) continue;
            if (pick == null || e.y > pick.y) pick = e;
        }
        if (pick == null) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (!e.typeable()) continue;
                if (pick == null || e.y > pick.y) pick = e;
            }
        }
        if (pick == null || buddy.out()) {
            miss(g);
            return false;
        }
        buddy.charge(pick);
        // A hit, but no score of its own: the squish it is on its way to pays that.
        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        skyGlow = Math.max(skyGlow, GLOW_HIT);
        skyGlowColor = Glyph.COLOR[g];
        return true;
    }

    /**
     * The squishy landed on a word. Squishes it whole, from where the bubble met it, and shakes
     * the screen a little harder each time as the squishy grows.
     */
    void buddySquish(Enemy e, Layout L) {
        destroyWord(e, buddy.x, buddy.y, L);
        Fx.explode(this, rnd, buddy.x, buddy.y, L.enemyR * 1.6f, 14,
                Collect.BODY[buddy.who]);
        shake = Math.max(shake, 0.30f + 0.03f * buddy.squishes);
        // A squish, pitched by how big the squishy has grown — depth sounds lower and rounder,
        // so it deepens as it fills out. The achievement fanfare was here first and was far too
        // much for something that fires every couple of seconds.
        if (sound != null) {
            sound.squish(e.word[0], Math.min(4, 1 + buddy.squishes / 2));
        }
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
        // Slow motion from a multi-word fling stroke, and the readout it earned. Both ticked
        // on real time and before the scaling below, so neither is slowed by the thing the
        // beat is slowing.
        if (slowdown > 0f) slowdown = Math.max(0f, slowdown - dt);
        if (sliceCall > 0f) sliceCall = Math.max(0f, sliceCall - dt);
        dt *= timeScale();
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
        if (storyOpen()) storyT += dt;
        // Signed, so it eases back to zero from whichever side the scroll came in on.
        if (caseSlide != 0f) {
            float d = dt * Showcase.SLIDE_RATE;
            caseSlide = caseSlide > 0f ? Math.max(0f, caseSlide - d)
                    : Math.min(0f, caseSlide + d);
        }

        // Cleared here, above the early return, and raised again by the enemy loop below.
        // Resetting it after the return left the red edge glow stuck on at whatever the
        // last lunging word set it to, all the way through the game-over screen.
        warnLevel = 0f;

        updateChain(dt);
        Fx.updateParticles(this, dt);
        Fx.updateShots(this, dt, L);

        if (state == BONUS) {
            steamer.update(dt);
            // Once the interlude's own countdown is spent, a won prize gets its parade before
            // play resumes. Handled before bonusTimer is touched again so none of the four
            // phases tick underneath it.
            if (bonusTimer <= 0f) {
                if (paradeTimer > 0f) {
                    paradeTimer -= dt;
                    if (paradeTimer > 0f) return;
                    paradeTimer = 0f;
                }
                advanceStage();
                state = PLAY;
                time = 0;
                return;
            }
            bonusTimer -= dt;
            // One tick per character the spinner steps past, so it sounds like a spin. Only
            // the left slot fires: both would double up on almost every step.
            if (bonusRolling()) {
                int shown = bonusLeftKey();
                if (shown != rollTick) {
                    rollTick = shown;
                    if (sound != null) sound.squish(shown, 1);
                }
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
        if (team()) buddy.update(this, dt, L);
        else if (!buddy.out()) buddy.leave();

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
     * Credits a finished word and starts it flying apart. Shared by the ordinary typed squish,
     * by out-of-order removal (flinging and MULTI), and by the end of a frenzy.
     *
     * @param px,py where the burst originates, usually the last tile struck
     */
    void destroyWord(Enemy e, float px, float py, Layout L) {
        destroyWord(e, px, py, L, true);
    }

    /**
     * @param chime false to leave the word-clear tone off. The blade has already chopped every
     *     letter of the word on the way through, and the tone on top of that crowds them.
     */
    void destroyWord(Enemy e, float px, float py, Layout L, boolean chime) {
        // The word is credited now but stays listed until it has flown apart, so anything
        // gated on the field being clear waits for the animation.
        e.destroyed = true;
        e.destroyT = 0f;
        e.dying = false;
        // A destroyed word is no longer a threat, so it must stop looking like one. The
        // update loop skips destroyed words, so whatever warning state it held would
        // otherwise stay frozen on it for the whole fly-apart: agitated jitter from warn,
        // and rose telegraph rings from attacking if a frenzy ended on one mid-lunge.
        e.warn = 0f;
        e.attacking = false;
        e.attackT = 0f;
        e.failPulse = 0f;
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
        squishes++;
        resolvedThisStage++;
        // Scored per press, so a stacked word is worth what it cost to clear.
        score += 25 * e.totalPresses();
        if (chime && sound != null) sound.clearWord();
    }

    /**
     * Removes tile {@code i} out of order, sending it off along dx,dy. Used by flinging and
     * by MULTI. Finishes the word if that was the last tile left.
     */
    void removeTile(Enemy e, int i, float dx, float dy, Layout L) {
        removeTile(e, i, dx, dy, L, true);
    }

    /** @param chime see {@link #destroyWord(Enemy, float, float, Layout, boolean)} */
    void removeTile(Enemy e, int i, float dx, float dy, Layout L, boolean chime) {
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
        if (e.pos >= e.word.length) destroyWord(e, tx, e.y, L, chime);
    }

    // ---- the blade ----------------------------------------------------------

    /**
     * Starts a blade stroke. Nothing is cut yet: a tap that does not travel cuts nothing, which
     * is what keeps the mode a swipe rather than a poke.
     */
    void beginStroke(float x, float y) {
        fingerDown = true;
        fingerX = x;
        fingerY = y;
        trailPrevX = x;
        trailPrevY = y;
        bladeFromX = x;
        bladeFromY = y;
        flingUsed = true;
        strokeKills = 0;
        strokeCuts = 0;
    }

    /**
     * Extends the stroke to x,y and cuts every tile the blade swept past on the way — not just
     * the ones under the end point, so a fast swipe cuts the whole line it crossed instead of
     * whatever happened to be under the last touch sample.
     *
     * Grabbing a tile and dragging it was the old model, and it was the reason the mode felt
     * weak: it cut one tile per gesture, only if the gesture started exactly on one.
     *
     * @return tiles cut by this segment
     */
    int sliceTo(float x, float y, Layout L) {
        float x0 = fingerX, y0 = fingerY;
        fingerX = x;
        fingerY = y;
        if (!flinging() || !fingerDown) return 0;

        int cut = 0;
        float r = L.enemyR * BLADE;
        // Downward, because destroying a word mutates the list from under the loop.
        for (int n = enemies.size() - 1; n >= 0; n--) {
            if (n >= enemies.size()) continue;
            Enemy e = enemies.get(n);
            if (!e.typeable()) continue;
            for (int i = e.word.length - 1; i >= e.pos; i--) {
                if (e.gone[i]) continue;
                if (segDist2(tileX(e, i, L), e.y, x0, y0, x, y) > r * r) continue;
                boolean alive = !e.destroyed;
                // Sent along the stroke, so the cut piece flies the way the blade went.
                // No clear tone: the chop below is this letter's sound, and a word finished by
                // the blade would otherwise land a chime on top of its own last chop.
                removeTile(e, i, x - x0, y - y0, L, false);
                cut++;
                strokeCuts++;
                if (sound != null) sound.chop();
                if (alive && e.destroyed) {
                    strokeKills++;
                    // Second word and every one after refreshes the beat, so a long sweep
                    // through four words stays slow for the whole of it.
                    if (strokeKills >= SLOW_KILLS) startSlowdown();
                }
                if (!e.typeable()) break;
            }
        }
        return cut;
    }

    /** Ends the stroke. The kill and cut counts survive it, for the readout. */
    void endStroke() {
        fingerDown = false;
    }

    /** The slow-motion beat that lands when one stroke takes several words. */
    private void startSlowdown() {
        slowdown = SLOW_TIME;
        sliceCall = SLICE_CALL_TIME;
        // Restrained on purpose: destroying the words has already fired a screen flash and
        // flooded the sky for each of them, and piling a third wash on top of that whited out
        // the whole field at exactly the moment there was something worth looking at.
        shake = Math.max(shake, 0.35f);
        flash = Math.max(flash, 0.45f);
        flashColor = FLASH_CLEAR;
        if (sound != null) sound.achievement();
    }

    /** Squared distance from a point to the segment a-b. */
    static float segDist2(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float len2 = dx * dx + dy * dy;
        // A stationary finger degenerates to a point, which is still a legitimate test.
        float t = len2 <= 1e-6f ? 0f : ((px - ax) * dx + (py - ay) * dy) / len2;
        if (t < 0f) t = 0f;
        else if (t > 1f) t = 1f;
        float qx = ax + dx * t - px, qy = ay + dy * t - py;
        return qx * qx + qy * qy;
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
        // A stage ended by a frenzy earns a longer go at the steamer. See bonusRollEnd for
        // how the one timer carries all four phases.
        bonusRollEnd = BONUS_TIME + (stageByPower ? Power.BONUS_EXTRA : 0f) + MASH_END;
        bonusTimer = BONUS_ROLL + bonusRollEnd;
        paradeTimer = 0f;
        steamer.lidPulse = 0;
        steamer.flash = 0;
        // Chosen up front, before the spinner has shown anything: the spinner animates toward
        // an answer that already exists rather than deciding when it stops.
        steamer.pick(rnd);
        rollTick = -1;
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
        awardPrize();
        // Set, not extended: the round is over the moment it is won, and what is left of it is
        // exactly the escape animation. Then the parade, immediately. Holding the full beat and
        // status first put nearly five seconds and a page of numbers between the win and the
        // parade, which made the parade look like it was not happening at all.
        bonusTimer = steamer.freedT;
        if (sound != null) sound.achievement();
    }

    /**
     * Opens the blind box the freed dumpling was carrying. A new entry goes into the case
     * and is written through to the store immediately; a duplicate pays out instead.
     *
     * The display case is left showing whatever came out, so the next visit to the title
     * screen opens on the prize rather than wherever the player had scrolled to.
     */
    private void awardPrize() {
        prize = Collect.roll(rnd, collected);
        prizeNew = !Collect.has(collected, prize);
        if (prizeNew) {
            collected = Collect.add(collected, prize);
            if (store != null) store.saveCollected(collected);
        } else {
            score += DUPE_BONUS;
        }
        caseIndex = prize;
        caseSlide = 0f;
        // Scheduled, not started: it runs after the rest of the interlude has played out.
        paradeTimer = PARADE_TIME;
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



    /**
     * 0 at full health, rising to 1 as lives run out. Tints the whole screen red and drives the
     * edge glow.
     *
     * Squared, so it bites late. Linear meant losing one of three lives put a permanent pulsing
     * red border round the screen for the rest of the run: a third of full strength, for the
     * ordinary state of having been hit once. A warning that never goes away stops being a
     * warning. This says "nearly dead", not "not perfect" — one life lost is barely visible, the
     * last life is unmistakable.
     */
    float harm() {
        if (state != PLAY) return 0f;
        float lost = clamp01(1f - (float) lives / START_LIVES);
        return lost * lost;
    }

    private static float decay(float v, float amount) {
        v -= amount;
        return v < 0 ? 0 : v;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
