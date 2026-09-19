package com.dddumpling.game;

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

    /** First steamer target; every successful opening adds {@link Steamer#GOAL_STEP}. */
    static final int STEAMER_START = 10;
    /**
     * Seconds of mashing a round earns, by how it went. The whole point of the spread is that the
     * interlude stops being a thing that happens to you and becomes the round's pay packet.
     *
     * It used to be a flat 2.2s. Short on purpose, that: at four seconds a moderate masher lands
     * all twenty hits in one go and the lid never accumulates across stages. So note what the top
     * of this ladder gives away — a perfect round can now open the steamer in one visit, which is
     * the intended reward and also the reason the tiers below it are worth playing for.
     *
     * <ul>
     *   <li>{@link #MASH_PERFECT} — no damage and no wrong presses.
     *   <li>{@link #MASH_UNHURT} — no damage, but some presses went astray.
     *   <li>{@link #MASH_HURT} — a life went.
     *   <li>{@link #MASH_PANIC} — the push-back was used, which overrides all of the above.
     * </ul>
     */
    static final float MASH_PERFECT = 5f;
    static final float MASH_UNHURT = 4f;
    static final float MASH_HURT = 3f;
    static final float MASH_PANIC = 1f;
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
     * How long the world holds after the last life goes, before the summary arrives. Death used to
     * be a single frame: the field was cleared and the game-over screen was simply there, with no
     * beat between playing and reading your score. The words that killed you swirl away over this,
     * the deck goes red, and the sky drains — then the summary fades up.
     */
    static final float DEATH_TIME = 1.6f;
    /** Boss celebrations get extra room to play; ordinary deaths keep the short transition above. */
    static final float BOSS_DEATH_TIME = DEATH_TIME * 3f - 2f;
    /** How long the summary takes to fade in once the hold is over. */
    static final float OVER_FADE = 0.45f;
    static final float RETURN_FADE = 0.60f;
    float returnFade;

    /** Counts down through the death sequence. Only ever non-zero in {@link #OVER}. */
    float deathT;
    /** Boss celebrating during the death-to-green transition; no live fight state is retained. */
    int bossVictoryKind = -1;

    /**
     * How long the run's dumplings take to carry themselves from the game-over screen to the
     * display case, on the way to the title.
     */
    static final float HOME_TIME = 1.15f;
    /** Counts down through that flight. Only ever non-zero in {@link #TITLE}. */
    float homeT;
    /**
     * How many of the haul have already been shelved, so each one is announced exactly once.
     * Counted in the order {@link RoundEnd} flies them, which is catalogue order.
     */
    int homeLanded;

    /** True while the run's haul is still on its way to the case. */
    boolean homing() {
        return homeT > 0f && roundPrizes != 0L;
    }

    /** 0..1 through the flight home. */
    float homeProgress() {
        return homing() ? 1f - homeT / HOME_TIME : 1f;
    }

    /** True while the world is still on screen after the last life. */
    boolean dying() {
        return deathT > 0f;
    }

    /** 0..1 through the death sequence, and 1 once it is over. */
    float deathProgress() {
        return dying() ? 1f - deathT / deathDuration() : 1f;
    }

    float deathDuration() {
        return bossVictoryKind >= 0 ? BOSS_DEATH_TIME : DEATH_TIME;
    }

    /**
     * 0..1 how far the world has drained: it rises through the death hold and then stays up for the
     * whole summary. Gating this on dying() instead snapped the deck and the sky back to their
     * playing colours on the frame the hold ended, which is exactly when the summary starts fading
     * in — the pop was more noticeable than the drain.
     */
    float drained() {
        return state == OVER ? deathProgress() : 0f;
    }

    /** 0..1 fade of the summary screen, which starts once the hold is spent. */
    float overFade() {
        if (state != OVER) return 0f;
        if (dying()) return 0f;
        return Math.min(1f, (time - deathDuration()) / OVER_FADE);
    }

    /**
     * True once the summary is up and settled, so a press means "done reading" rather than
     * landing on a screen that arrived under the thumb. Covers the whole death sequence, its
     * fade, and then the usual grace.
     */
    boolean overReady() {
        return state == OVER && time > deathDuration() + OVER_FADE + OVER_GRACE;
    }
    /**
     * How long the title screen takes to fade out once a start key is pressed. Play does not
     * begin until it has gone, so the first wave is never already falling behind a screenful of
     * text — which is what a hard cut looked like.
     */
    static final float START_FADE = 0.55f;
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
    interface Store extends Progress.Store {
        default String loadReleaseSeen() { return BuildFlags.BUILD_ID; }
        default void saveReleaseSeen(String value) {}
        int loadBest();
        void saveBest(int best);
        default int loadCaveChoice() { return -1; }
        default void saveCaveChoice(int value) {}
        default int loadLandState() { return 0; }
        default void saveLandState(int value) {}
        default int loadLandBest(int land) { return land == 0 ? loadBest() : 0; }
        default void saveLandBest(int land, int value) { if (land == 0) saveBest(value); }
        default int loadPlayerSettings() { return PlayerSettings.DEFAULT; }
        default void savePlayerSettings(int value) {}
        float loadSpeed();
        void saveSpeed(float speed);
        int loadBgm();
        void saveBgm(int choice);
        /** The collected-squishy bitmask; see {@link Collect}. */
        long loadCollected();
        void saveCollected(long owned);
        int[] loadCollectionCounts();
        void saveCollectionCounts(int[] counts);
        /** All collection rewards, including duplicate minigame and boss prizes. */
        int loadCollectTotal();
        void saveCollectTotal(int total);
        /** Lifetime steamer successes, used to retain its rising target across playthroughs. */
        int loadSteamerOpens();
        void saveSteamerOpens(int opens);
        int loadStarWins();
        void saveStarWins(int wins);
        /** Packed adaptive-roster state; zero is the first-run four-key default. */
        int loadRosterState();
        void saveRosterState(int state);
    }

    /**
     * Audio seam. Kept as an interface so the rules stay Android-free and the harness can
     * assert which effect fires on which event.
     */
    interface Sound {
        default void volumes(float music, float effects) {}
        /** @param depth presses that were still owed on the tile before this press */
        void squish(int glyph, int depth);
        void clearWord();
        void wrong();
        void linkedThud();
        void shuffleBlip();
        void debuffDown();
        void slimeCover(boolean release);
        void landShuffle();
        void uiBloop();
        void damage();
        void achievement();
        /** The slime has turned an unanswered prompt into a volley. */
        void bossLaugh();
        /** 0 stops the slime bubble bed; 0..1 thickens it toward launch. */
        void bossCharge(float charge);
        /** A damaging blow landed on a boss: a single large, wet bubble burst. */
        void bossDamage();
        /** A Slime glob carried free: bright, cute bubbles instead of a heavy body hit. */
        void slimeDamage();
        /** The slime chain tore a glob free: a taut, wet pop distinct from damage. */
        void bossSplit();
        /** Dark Divide was struck: a rounded bloop distinct from every other boss. */
        void divideDamage();
        /** The charged Dark Divide was pulled into two bodies. */
        void divideSplit();
        /** A terminal Dark Divide fragment was pulled apart and deactivated. */
        void divideDeactivate();
        /** The gathered cubes ignite into a supernova. */
        void divideSupernova();
        /** Two Dark Divide bodies, or one body and a wall, rebounded. 1 is largest/heaviest. */
        void divideBoing(float weight);
        /** A charged or flying slime bolt was destroyed: one short, low bloop. */
        void boltPop();
        /** A launched boss bolt breaks apart completely. */
        void boltDeath();
        /** A player projectile ricocheted from the Slime boss shield. */
        void shieldBounce();
        void octoCue();
        void octoLock();
        /** One accepted Fly Agaric shake endpoint. */
        void mushroomShake();
        /** Fly Agaric released a sprinkling volley. */
        void mushroomSpore();
        /** A letter cut by the FLING blade. Fires several times per swipe, so it is short. */
        void chop();
        /** One hop of a MULTI chain. @param hop 1-based, so the crack can climb with the chain */
        void zap(int hop);
        /**
         * One of the run's dumplings being taken into the display case, at the end of its flight
         * home.
         *
         * @param nth 0-based position in the haul, so the chime can climb as the shelf fills
         */
        void collect(int nth);
        /**
         * A star taken on the course. Fires up to twenty times in five seconds, so it is the
         * shortest effect there is.
         *
         * @param nth 1-based count of stars held, so the note can climb as the course fills
         */
        void star(int nth);
        /** The ready lesson ending and the course starting to move. Once per attempt. */
        void courseStart();
        /** One blast as the course enters its finish sequence. */
        void courseFinish();
        /** Continuous light rocket layer; zero stops it, 0..1 raises its thrust. */
        void rocket(float thrust);
        /**
         * The count read out at the end of an interlude nobody won — a star course's report or a
         * steamer's status page. Both used to arrive in silence.
         *
         * @param nth how well it went (stars taken, baskets opened), so the tone can rise with it
         */
        void tally(int nth);
        /** The new collectible taking its place in the parade line. */
        void paradeJoin();
        /** Cat and Grapes joining the keyboard: a bright, triumphant fanfare. */
        void rosterJoin();
        /** The run is over: the swirl has cleared and the summary is coming up. */
        void gameOver();
        /** The boss that ended the run begins its boss-specific victory taunt. */
        void bossTaunt(int kind);
        /** Switch the looping background track to {@link Music#NAMES}[choice]. */
        void selectMusic(int choice);
        /** Switches the selected instruments into or out of their faster boss arrangement. */
        void bossMusic(boolean active);

        void gameStart();
        /** Stage cleared normally. Suppressed when {@link #powerClear()} fires instead. */
        void stageClear();
        /** A frenzy ran to its end and took the stage with it. */
        void powerClear();
        /** Swap the looping track to the faster driven variant, and back. */
        void frenzy(boolean on);

        /**
         * Read collectible {@code entry}'s story aloud, as its popup opens. What is said and how
         * is {@link Narration}'s business; a backend only has to speak it.
         */
        void narrate(int entry);
        /** Stop talking mid-sentence: the panel has gone. */
        void hush();
    }

    /** Optional; null in the harness unless a test is watching for effects. */
    Sound sound;

    static final class Enemy {
        Enemy link;
        // Stage membership survives the visual bond being released or broken.
        Enemy stageMate;
        boolean stageResolved;
        Enemy spinMate;
        boolean linkWaiting;
        boolean linkSliceRejected;
        float linkLeft;
        int linkButton = -1;
        float linkStrain;
        float linkFlex;
        float linkReleaseDir, linkReleaseX, linkReleaseY;
        int[] word;
        /** Presses each tile needs: 1 for a plain letter, 2..4 for a stacked one. */
        int[] need;
        int pos;
        /** Presses landed on the current tile so far. */
        int done;
        float baseX, y, speed, phase, sway;
        /** Frenzy side entrance: an inward arc that settles into a vertical lane. */
        boolean sideEntry;
        float pathStartX, pathEndX, pathStartY;
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
        float[] flyDir, flyY;
        boolean radialFly;

        /**
         * The push-back slide: where the shove found the word, where it lands, and the time left
         * to get there. The whole trip is on {@link #y}, not on a draw-time offset, so targeting
         * and the blade agree with what is on screen while it is moving.
         */
        float slideFrom, slideTo, slideT;

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
        /**
         * Fired at the boss rather than at a word, and where on it — as an offset from the body's
         * centre, not as a point.
         *
         * An offset because the body drifts and wobbles the whole time the shot is in the air, so a
         * fixed point would visibly miss. It is the same homing a word's shot gets, expressed the
         * only way it can be here: keeping the offset means a bullet aimed at a boss surface
         * heads still arrives at that head rather than at the middle of the boss.
         */
        boolean atBoss;
        /** Rejected by the Slime shield: reaches the body, then ricochets back out. */
        boolean shieldBounce;
        float bossDx, bossDy;
    }

    /**
     * How long a shot is in the air, from the key to the tile it was fired at. Named because the
     * title screen's demo fires the same bullet on the same schedule, and a second copy of the
     * number would drift.
     */
    static final float SHOT_TIME = 0.13f;

    static final class Particle {
        float x, y, vx, vy, life, max, size;
        int color;
    }

    static final class PushImpact {
        float x, y, life = 0.62f;
        int color;
    }

    // ---- persistent-ish state ----------------------------------------------
    int state = TITLE;
    /** Live title-screen touch, used only to make the logo letters react under a finger. */
    boolean titleTouchDown;
    float titleTouchX, titleTouchY;
    static final int TITLE_LETTERS = 10;
    final float[] titleSpringX = new float[TITLE_LETTERS];
    final float[] titleSpringY = new float[TITLE_LETTERS];
    private final float[] titleSpringVX = new float[TITLE_LETTERS];
    private final float[] titleSpringVY = new float[TITLE_LETTERS];
    int score, best, lives, stage, combo, maxCombo;
    int landChoice, runStartLand;
    final int[] landBests = new int[Lands.COUNT];
    boolean landPickerDragging, landPickerMoved;
    float landPickerSlide, landPickerX;
    int landTravelFrom = -1;
    float landTravelT, landWanderT, landTravelStartX, landTravelStartY;
    float landTravelRegrowT=-1f, landTravelRegrowScale;
    boolean landTravelChained;
    final ArrayList<Integer> landTravelQueue = new ArrayList<Integer>();
    boolean allLandsEnabled;
    int landSeen, landSuppressed, landDiscovery = -1;
    float landDiscoveryT;
    int landDiscoveryFrom;
    boolean landDiscoveryChained, landDiscoveryFresh;
    /** Words squished this run. The game-over screen calls them squishes, so this does too. */
    int squishes;
    /** Stage enemies released; a linked pair occupies one quota slot. */
    int spawnedThisStage;
    /** Stage enemies resolved, after both halves of a pair clear or breach. */
    int resolvedThisStage;
    /** Correct and incorrect presses over the whole run, for the accuracy readout. */
    int hits, misses;
    /** Incorrect presses in the current stage; zero at stage end earns the gold dumpling. */
    int missesThisStage;
    /** Lives lost in the current stage. Zero is what "no damage" means to the interlude's verdict. */
    int hurtThisStage;
    /**
     * Mash seconds the finished round earned, settled by {@link #beginStageEnd} rather than read
     * when the interlude opens — by then {@link #missesThisStage} has already been zeroed, and the
     * verdict has to be taken while the evidence is still there.
     */
    float earnedMash = MASH_HURT;

    // ---- settings (persisted) ----------------------------------------------
    static final float SPEED_MIN = 0.5f, SPEED_MAX = 1.5f;
    /** Pacing multiplier: >1 makes words fall and arrive faster. */
    float speed = 1f;
    int bgmChoice;
    /** While true the simulation is frozen and the settings panel is showing. */
    boolean settingsOpen;
    int settingsPage;
    final PlayerSettings preferences = new PlayerSettings();
    boolean kidsRun;
    final ReleaseNotes releaseNotes=new ReleaseNotes();
    final ReleaseMascot releaseMascot=new ReleaseMascot();
    int settingsTab;
    /** Newly collected stars in this update, including the winning pickup. */
    int starPickups;
    boolean paused, confirmEnd;

    // ---- transient ----------------------------------------------------------
    float time;              // seconds since entering the current state
    float clock;             // never resets; drives idle animation
    final List<Enemy> enemies = new ArrayList<Enemy>();
    final List<Shot> shots = new ArrayList<Shot>();
    final List<Particle> particles = new ArrayList<Particle>();
    final List<PushImpact> pushImpacts = new ArrayList<PushImpact>();
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
    /** Spent for this stage once the push-back has been used. */
    boolean pushUsed;
    /** Counts down while the push-back shockwave is on screen. */
    float pushT;
    /** Words the last push-back shoved back, for the readout. */
    int pushCount;

    /**
     * True when the push-back is available: once a stage, and only with something already
     * closing on the line. Gating it on the threat is what keeps it a panic button rather than a
     * free tempo reset to be spent the moment a wave starts.
     */
    boolean pushReady() {
        return state == PLAY && !pushUsed && !settingsOpen && !pendingBonus && warnLevel > 0f;
    }

    // ---- between-stages minigame -------------------------------------------
    final Steamer steamer = new Steamer();
    final StarPath stars = new StarPath();
    final Cave cave = new Cave();
    int caveChoice = -1;
    /** Successful games alternate; failures leave the same game queued. */
    boolean starNext, starBonus;
    /** Unlocked for this run after defeating the stage-5 slime. */
    boolean cubeUnlocked;
    /** Cat and Grapes stay until three consecutive runs end before stage 6. */
    boolean fullRoster;
    /** Snapshot used by the current run; settings only changes the next one. */
    boolean runFullRoster;
    int earlyLosses;
    boolean rosterLeavePending;
    static final int ROSTER_JOIN = 1, ROSTER_LEAVE = -1;
    static final float ROSTER_SCENE_TIME = 2.8f;
    int rosterScene;
    float rosterSceneT;
    float bonusTimer;
    /** Last character the spinner ticked on, so each step sounds exactly once. */
    int rollTick = -1;
    /**
     * Seconds of parade left. Set the moment a prize is won but deliberately not ticked until
     * the interlude's own countdown has run out, which is what lets it be both "something was
     * won this round" and "the parade still has to happen" without a second flag.
     */
    float paradeTimer;
    boolean bossPrizePending, bossReward;
    /**
     * One-shot guards for the two interlude sounds that mark a moment rather than a press: the
     * parade's join chord and the steamer's status tally. Both are cleared where their scene starts,
     * not where it ends, so an interrupted one can announce itself again next time.
     */
    boolean joinRung, statusRung;

    // ---- the collection ----------------------------------------------------
    /**
     * Which squishies have been collected, one bit per {@link Collect} entry. The only state
     * that survives a run other than the best score, so it is written through to the store
     * the moment it changes rather than at the end of a game — a run that is force-quit must
     * not lose the thing it won.
     */
    long collected;
    /** Lifetime collection rewards, including duplicates and the unattributed legacy total. */
    int collectTotal;
    final int[] collectionCounts = new int[Collect.COUNT];
    /** What the last opened steamer handed over, or -1. Reset when a run starts. */
    int prize = -1;
    /**
     * Every dumpling freed this run, as a {@link Collect} mask. The run's own haul rather than the
     * whole case: it is what dances on the game-over screen and what flies home to the case on the
     * way to the title.
     */
    long roundPrizes;
    /** False when {@link #prize} was already in the case. */
    boolean prizeNew;

    /** Which entry the display case is showing, and the slide left over from the last scroll. */
    int caseIndex;
    /** -1..1, decaying to 0: the shelf easing into place after a scroll, or held by a drag. */
    float caseSlide, caseSlideY;
    /**
     * True while the case is up. Closed by default: the title screen offers a badge in the
     * middle and this is what a tap on it sets.
     */
    boolean caseOpen;
    /** 0..1 opacity, easing in on the tap and out again on close. */
    float caseFade;
    /** True while a finger is dragging the shelf; the slide is its offset, not a decay. */
    boolean caseDragging;
    /**
     * Seconds the case has been open. Only the first moment of it is used: the focused entry throbs
     * as the case comes up, which is what tells you it can be tapped now that no line says so.
     */
    float caseT;
    /** Time since this tile became highlighted; drives its welcome bounce and sparkles. */
    float caseHighlightAge;
    /** Where the shelf's current entry was grabbed, in view pixels. */
    float caseDragX, caseDragY;
    boolean caseFreePan;
    float casePanMotionX, casePanMotionY;

    /** How quickly the case fades in and out, in screens per second. */
    static final float CASE_FADE_RATE = 4.2f;

    /** True when the case is on screen at all, fades included. */
    boolean caseShown() {
        return state == TITLE && caseFade > 0f;
    }

    /** Seconds left of the title screen fading out. */
    float startFade;

    /** Which squishy is being sent off as the run starts, or -1 for none. */
    int launchWho = -1;
    /** Title selection for this run; awards can move the case without changing the pilot. */
    int runWho;
    /** Seconds left of that send-off. Play waits for it. */
    float launchT;
    /**
     * The clock as the start press landed. The badge is always drifting, and this is what lets
     * the send-off leave from exactly where it was rather than from wherever it has got to.
     */
    float launchClock;
    /** How many of the send-off's impacts have sounded, so each one ticks once. */
    private int launchPips;

    /** True while the title screen is on its way out and play has not begun. */
    boolean starting() {
        return state == TITLE && (startFade > 0f || launchT > 0f);
    }
    /** Set when the start tone has already played, so {@link #startGame} does not repeat it. */
    private boolean startAnnounced;

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
        if (state != TITLE || !caseOpen || storyOpen()
                || !Collect.has(collected, caseIndex)) {
            return;
        }
        story = caseIndex;
        storyT = 0f;
        if (sound != null) {
            sound.achievement();
            sound.narrate(story);
        }
    }

    void closeStory() {
        // Only hushes a reading that was actually under way. This is called on the way into and
        // out of half a dozen states where no story is open, and a backend should not have to
        // work out which of those it is.
        boolean reading = storyOpen();
        story = -1;
        storyT = 0f;
        if (reading && sound != null) sound.hush();
    }

    // ---- powerup ------------------------------------------------------------
    /** At most one glowing letter on screen. */
    Power power;
    float powerTimer;
    /** Active mode, or -1. */
    int mode = -1;
    float modeLeft;
    float powerBurstX, powerBurstY;
    int debuff = -1;
    float debuffLeft, monochromeFade, incognitoMorph;
    /** Rapid clears earn a short, bounded replacement burst. */
    float powerLastClear = -100f;
    int powerRefillBurst;
    int powerSpawnedEnemies;
    /** True between the wave ending and the interlude opening. */
    boolean pendingBonus;
    /** Set when a frenzy ended the stage, so the interlude can run longer. */
    boolean stageByPower;
    /**
     * Drives the clouds. Separate from {@link #clock} because it runs fast during a frenzy;
     * accumulating it keeps the drift continuous rather than jumping when the frenzy ends.
     */
    float skyClock;
    int landFrom, landFromBg = Lands.BG[0], landFromTint = Lands.TINT[0];
    float landBlend = 1f;
    final int[] landCloudFrom = Sky.CLOUD_TINT.clone();

    // ---- boss ---------------------------------------------------------------
    /**
     * The set piece on every fifth stage. Owns its own rules; see {@link Boss}.
     *
     * Note what it shares with the frenzy: both replace the stage's ordinary pacing, both end the
     * stage when they finish, and both have two exits — their own end, and the player dying. The
     * second one never reaches the PLAY half of {@link #update}, which is why {@link #die} sends the
     * boss home alongside the squishy.
     */
    Boss boss = new Boss();
    /** The exact live boss retained only as a visual snapshot while it celebrates a won fight. */
    Boss bossVictory;

    /** True from the boss's arrival card to the end of its exit, defeat or retreat. */
    boolean bossActive() { return boss.active(); }

    /** True while the fight is live: past the card, not yet over. */
    boolean bossFighting() { return state == PLAY && boss.fighting(); }

    /** Score for beating one, and for one landed hit on the way there. */
    static final int BOSS_BONUS = 900, BOSS_HIT = 40;

    boolean incognito() { return state == PLAY && ((debuffLeft > 0f && debuff == Power.INCOGNITO) || incognitoMorph > 0f); }
    void startDebuff(int effect) {
        if (effect != Power.INCOGNITO && effect != Power.MONOCHROME) return;
        debuff = effect;
        debuffLeft = Power.DEBUFF_TIME;
        if (sound != null) sound.debuffDown();
    }

    boolean powerActive() { return modeLeft > 0f; }

    boolean flurry() { return powerActive() && mode == Power.FLURRY; }
    float flurryBurstProgress() {
        float age=Power.DURATION-modeLeft;
        return state==PLAY && flurry() && age<Power.FLURRY_BURST ? age/Power.FLURRY_BURST : -1f;
    }


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
        float rate = powerActive() ? Power.fallRate(ramp()) : 1f;
        // The shove's drag, winding back up. Multiplied into whatever the frenzy is asking for
        // rather than replacing it: a swipe during a frenzy has to be worth the same relief.
        if (pushSlowT > 0f) {
            float back = 1f - pushSlowT / PUSH_SLOW;
            rate *= PUSH_SLOW_RATE + (1f - PUSH_SLOW_RATE) * back;
        }
        return rate;
    }

    /** Counts down through the drag a shove leaves on the field. */
    float pushSlowT;

    /** 0..1 through that drag, for anything that wants to show it. */
    float pushSlowProgress() {
        return pushSlowT > 0f ? 1f - pushSlowT / PUSH_SLOW : 1f;
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
    // The gesture itself is in Blade; these are the fields it works on, kept here because the
    // renderer, the view and the trail all read them.

    /** True while a stroke is live: finger down *and* moving. Everything that draws the blade reads this. */
    boolean fingerDown;
    /**
     * True while the finger is on the glass at all, cutting or not. Split from {@link #fingerDown}
     * because a stroke is a motion, not a touch: the dwell rests a stroke under a stopped finger and
     * the next move wakes a new one with no fresh ACTION_DOWN.
     */
    boolean touchDown;
    /** Live finger position, set by the view. */
    float fingerX, fingerY;
    /** Cleared when a frenzy starts; set once the player first touches during FLING. */
    boolean flingUsed;
    /** Where the instructional finger sits, for the renderer to follow. */
    float demoX, demoY;
    float trailAcc;
    /** Where the trail was emitted from last frame, so a fast swipe still leaves a ribbon. */
    float trailPrevX, trailPrevY;
    /**
     * The stretch the blade covered last frame, for the renderer's edge. Separate from
     * {@link #trailPrevX}, which is brought up to the finger before the frame is drawn and would
     * leave the edge with no length.
     */
    float bladeFromX, bladeFromY;
    /** Words and tiles this stroke has taken, held until the next stroke begins. */
    int strokeKills, strokeCuts;
    /**
     * What the readout is announcing: the counts of the stroke that earned it, frozen as it ended.
     * Separate from {@link #strokeKills} because the readout outlives the stroke and strokes end on
     * their own — without it, starting a fresh stroke inside the window rewrote "4 IN ONE!" down to
     * the new tally and then blanked it. Refreshed on every cut while this stroke owns the readout.
     */
    int callKills, callCuts;
    /** Seconds of slow motion left, and seconds the slice readout has left. */
    float slowdown, sliceCall;
    /** Seconds since the last definite move, and how long this stroke has been live. */
    float strokeIdle, strokeAge;
    /** Where the last definite move landed; a stroke sleeps while the finger stays near it. */
    float strokeAnchorX, strokeAnchorY;
    /** Seconds of dying blade left after a stroke ends. */
    float strokeFade;

    /** How much of normal speed the simulation is running at. */
    float timeScale() {
        return slowdown > 0f ? Blade.SLOW_RATE : 1f;
    }

    /**
     * The interlude has three paths and exactly one phase is true at any moment.
     *
     * Lost round: spinner, mash, a beat on zero, then the status report whose tail fades out.
     * Won round: the mash is over the instant it is won, so it is the escape animation and then
     * the parade. The first four run off the one countdown — the fade-out then has one value to
     * read, and every boundary is a comparison rather than a transition that could be missed.
     * Only the first boundary varies, since the mash is earned per round, so it is stored.
     *
     * Star course: {@link #starFlight} covers all of it — lesson, flight and victory tableau — and
     * then the same parade. None of the steamer's phases may hold during one; see starFlight.
     */
    float bonusRollEnd;

    /** Timer value at which the mash gives way to the beat on zero. */
    static final float MASH_END = BONUS_HOLD + BONUS_STATUS;

    /**
     * Total length of an interlude whose round earned {@code mash} seconds: all four phases, without
     * a frenzy's extra. Takes the mash rather than assuming one, since how long it is is the round's
     * business now.
     */
    static float bonusLength(float mash) {
        return BONUS_ROLL + mash + BONUS_HOLD + BONUS_STATUS;
    }

    /**
     * A perfect round: nothing pressed wrongly and nothing lost.
     *
     * The one definition, read by both things that care — the flawless-wave dumpling in
     * {@link #beginStageEnd} and {@link #mashEarned}. It used to ask only about wrong presses, which
     * meant a word that fell past untouched cost a life and still earned the gold dumpling: no
     * *press* had been wrong, so by that reading nothing was. Two things called perfect in one game
     * had better mean the same thing, so this is it.
     */
    boolean perfectRound() {
        return missesThisStage == 0 && hurtThisStage == 0;
    }

    /**
     * What the round just finished has earned at the steamer.
     *
     * The push-back overrides everything, including an otherwise perfect round: it is a life bought
     * back, and this is the price. Note that it does not cost the dumpling, only the mash — the
     * swipe is not damage, it is what you spent to avoid damage.
     */
    float mashEarned() {
        if (pushUsed) return MASH_PANIC;
        if (hurtThisStage > 0) return MASH_HURT;
        return perfectRound() ? MASH_PERFECT : MASH_UNHURT;
    }

    /** True while the spinner is still settling on this round's pair. */
    boolean bonusRolling() {
        return state == BONUS && !starBonus && !bossReward && bonusTimer > bonusRollEnd;
    }

    /** True while the interlude accepts presses. */
    boolean bonusMashing() {
        return state == BONUS && !starBonus && !bossReward && !bonusPrizeWon()
                && bonusTimer <= bonusRollEnd && bonusTimer > MASH_END;
    }

    /** The final steamer point is in and an upward lid swipe may claim it. */
    boolean bonusSwipeReady() {
        return bonusMashing() && steamer.swipeReady;
    }

    /** True during the beat after the clock runs out, before anything fades. */
    boolean bonusHolding() {
        return state == BONUS && !starBonus && !bossReward && !bonusPrizeWon()
                && bonusTimer <= MASH_END && bonusTimer > BONUS_STATUS;
    }

    /** True while a won prize is climbing out, which is all that is left of a won round. */
    boolean bonusEscape() {
        return state == BONUS && !starBonus && !bossReward && bonusPrizeWon() && bonusTimer > 0f;
    }

    /**
     * True for the whole of a star course — the ready lesson, the flight, and the victory tableau
     * a completed one ends on — up to the parade, which closes both games the same way.
     *
     * The steamer's phases all exclude {@code starBonus} so that this is the one that holds here.
     * They read off the same {@code bonusTimer} and used to come true underneath a star course as
     * it counted down, which drew nothing only because the star branch returns before them.
     */
    boolean starFlight() {
        return state == BONUS && starBonus && !bonusParading();
    }

    /**
     * True while the interlude is showing its end-of-round status.
     *
     * Never on a winning round: the parade closes those, and it announces the stage itself. The
     * report used to run in between and put a page of numbers between winning something and
     * watching it join the line.
     */
    boolean bonusStatus() {
        return state == BONUS && !starBonus && !bossReward && !bonusPrizeWon() && bonusTimer <= BONUS_STATUS;
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
        debuffLeft = Math.max(0f,debuffLeft-dt);
        float mono = debuffLeft > 0f && debuff == Power.MONOCHROME ? 1f : 0f;
        monochromeFade += Math.max(-dt/0.6f,Math.min(dt/0.6f,mono-monochromeFade));
        float disguise = debuffLeft > 0f && debuff == Power.INCOGNITO ? 1f : 0f;
        incognitoMorph += Math.max(-dt/0.75f,Math.min(dt/0.75f,disguise-incognitoMorph));
        if (powerActive()) {
            modeLeft -= dt;
            if (modeLeft <= 0f) endPower(L);
        }

        if (power != null) {
            int previousIcon = power.shownEffect();
            power.update(dt);
            if (power.mystery && power.hit && power.hitT < Power.SELECT_TIME
                    && power.shownEffect() != previousIcon && sound != null) sound.shuffleBlip();
            if (power.mystery && power.hit && !power.activated && power.hitT >= Power.SELECT_TIME) {
                power.activated = true;
                if (power.effect >= Power.COUNT) startDebuff(power.effect);
                else startFrenzy(power.effect,L);
            }
            if (power.spent() || (power.catchable() && power.offScreen(L, L.enemyR))) {
                power = null;
                powerTimer = Power.SPAWN_MIN
                        + rnd.nextFloat() * (Power.SPAWN_MAX - Power.SPAWN_MIN);
            }
            return;
        }

        // One frenzy at a time, and none while the wave is already over.
        //
        // And none at all during a boss. Three of the four modes act on words rather than on the
        // boss, so catching one mid-fight would be a reward that does nothing about the thing
        // actually threatening you; FLURRY, the fourth, wildcards every key and would hand over
        // every boss window for free. One set piece at a time is both the simpler rule and the
        // better one, and it keeps the frenzy taper's arithmetic about what a stage asks intact.
        if (powerActive() || debuffLeft > 0f || boss.active() || stageGap > 0
                || spawnedThisStage >= stageQuota()) {
            return;
        }
        powerTimer -= dt;
        if (powerTimer <= 0f) spawnPower(L);
    }

    /** Releases a powerup letter to drift across the sky. */
    private void spawnPower(Layout L) {
        Power w = new Power();
        w.glyph = randomGlyph();
        w.mystery = stage >= Power.MYSTERY_STAGE;
        w.teamAvailable = Collect.owned(collected) > 0;
        w.effect = w.mystery ? -1 : rollEffect();
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
        int count = Power.offeredCount(Collect.owned(collected) > 0);
        return Power.offeredAt(rnd.nextInt(count));
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
    boolean tapPower(float x, float y, Layout L) {
        if (paused || state != PLAY || power == null || !power.catchable()) return false;
        float bobY = power.y + (float) Math.sin(power.t * 3.2f) * L.enemyR * 0.22f;
        float dx = x - power.x, dy = y - bobY;
        float grab = L.enemyR * 2.05f;
        if (dx * dx + dy * dy > grab * grab) return false;
        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        catchPower(L);
        return true;
    }

    private void catchPower(Layout L) {
        power.hit = true;
        power.hitT = 0f;
        powerBurstX=power.x;
        powerBurstY=power.y+(float)Math.sin(power.t*3.2f)*L.enemyR*0.22f;
        score += Power.SCORE;
        Fx.explode(this, rnd, power.x, power.y, L.enemyR * 2.2f, 26, 0xFFFFFFFF);
        if (power.mystery) {
            if (sound != null) sound.shuffleBlip();
            power.teamAvailable = Collect.owned(collected) > 0;
            power.effect = Power.mysteryAt(power.teamAvailable,rnd.nextInt(Power.mysteryCount(power.teamAvailable)));
        } else {
            power.activated = true;
            startFrenzy(power.effect, L);
        }
    }

    /**
     * Starts a frenzy of the given mode. Shared by catching a powerup and by the playtest
     * buttons, so a playtest exercises exactly what play does.
     */
    void startFrenzy(int effect, Layout L) {
        if (Cave.active(this) || effect < 0 || effect >= Power.COUNT) return;
        // TEAM SQUISH has nobody to field with an empty case. The drifting letter never rolls it
        // then, so only the playtest chips can ask for it, and refusing is clearer than quietly
        // substituting a different mode.
        int entry = effect == Power.TEAM ? anyCollected() : -1;
        if (effect == Power.TEAM && entry < 0) return;
        if (effect != Power.MULTI) LinkedPairs.preparePower(this);
        else LinkedPairs.release(this, L);
        debuffLeft = monochromeFade = incognitoMorph = 0f;
        mode = effect;
        modeLeft = Power.DURATION;
        if(power==null || !power.hit) {
            powerBurstX=L.w*0.5f;powerBurstY=(L.playTop+L.dangerY)*0.5f;
        }
        powerLastClear = -100f;
        powerRefillBurst = 0;
        powerSpawnedEnemies = 0;
        spawnTimer = Math.min(spawnTimer, Power.spawnDelay(this, L));
        flingUsed = false;
        // A finger already resting on the field does not get a free stroke: it has to lift and
        // land again, the same as it would to start a second swipe.
        fingerDown = false;
        touchDown = false;
        strokeFade = 0f;
        strokeKills = 0;
        strokeCuts = 0;
        callKills = 0;
        callCuts = 0;
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
    void playtestDebuff(int effect) {
        if (!BuildFlags.DEVELOPER || state != PLAY
                || (effect != Power.INCOGNITO && effect != Power.MONOCHROME)) return;
        power = null;
        settingsOpen = false;
        modeLeft = 0f;
        buddy.leave();
        fingerDown = touchDown = false;
        strokeFade = 0f;
        debuffLeft = monochromeFade = incognitoMorph = 0f;
        if (sound != null) sound.frenzy(false);
        startDebuff(effect);
    }

    void playtestMode(int effect, Layout L) {
        if (!BuildFlags.DEVELOPER) return;
        if (state != PLAY) return;
        power = null;
        settingsOpen = false;
        startFrenzy(effect, L);
    }

    /**
     * Drops straight into a star course, for the same reason the frenzy chips exist: the scene is
     * otherwise several minutes away. Reaching one in play means clearing a wave, winning a steamer
     * to be handed the flyer, and then clearing another wave — and a course that is not won leaves
     * {@code starNext} set, so from here the chip keeps handing out courses until one is finished,
     * which is what tuning it needs.
     *
     * The wave is retired rather than abandoned: the field is emptied and the stage's quota marked
     * released, so the interlude opens on a cleared board and the stage after it advances normally.
     * Anything less and the interlude plays over words that are still falling behind the scrim.
     */
    void playtestStars(Layout L) {
        if (!BuildFlags.DEVELOPER) return;
        if (state != PLAY) return;
        power = null;
        settingsOpen = false;
        pendingBonus = false;
        enemies.clear();
        target = null;
        caretOwner = null;
        spawnedThisStage = stageQuota();
        resolvedThisStage = stageQuota();
        starNext = true;
        Interlude.enterBonus(this, L);
    }

    /** Drops straight into a full-length steamer round from the playtest panel. */
    void playtestSteamer(Layout L) {
        if (!BuildFlags.DEVELOPER) return;
        if (state != PLAY) return;
        power = null;
        settingsOpen = false;
        pendingBonus = false;
        enemies.clear();
        target = null;
        caretOwner = null;
        spawnedThisStage = stageQuota();
        resolvedThisStage = stageQuota();
        earnedMash = MASH_PERFECT;
        starNext = false;
        Interlude.enterBonus(this, L);
    }

    /**
     * The frenzy ran out. That clears the stage outright: everything still on the field is
     * destroyed and the wave counts as fully released, so the interlude follows.
     */
    private void endPower(Layout L) {
        mode = -1;
        modeLeft = 0f;
        debuffLeft = monochromeFade = incognitoMorph = 0f;
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

    // ---- the boss fight -----------------------------------------------------
    // Delegations; the fight lives in BossPlay. Boss itself owns the rules — this is the wiring to
    // score, sound, shots and lives.

    /** Colour of the burst a tap or a completed drag throws off, which has no letter of its own. */
    static final int INK_SPARK = 0xFFFFF3C4;

    boolean tapBoss(float x, float y, Layout L) { return BossPlay.tap(this, x, y, L); }

    boolean grabBoss(float x, float y) { return BossPlay.grab(this, x, y); }

    boolean dragBoss(float x, float y, Layout L) { return BossPlay.dragTo(this, x, y, L); }

    void releaseBoss() { boss.release(); }

    boolean beginBossPinch(float distance) { return boss.beginPinch(distance); }

    boolean beginBossPinch(float distance, float x1, float y1, float x2, float y2) {
        return boss.beginPinch(distance, x1, y1, x2, y2);
    }

    boolean pinchBoss(float distance, Layout L) { return BossPlay.pinch(this, distance, L); }

    boolean pinchBoss(float distance, float x1, float y1, float x2, float y2, Layout L) {
        return BossPlay.pinch(this, distance, x1, y1, x2, y2, L);
    }

    void endBossPinch() { boss.endPinch(); }

    boolean swipeUp(Layout L) {

        return pushBack(L);
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

    final Random rnd;
    final Store store;
    final Progress progress;

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

    /**
     * How far up a push-back shoves a word, as a fraction of the whole descent. Enough to be a
     * real reprieve rather than a stutter — you spent the stage's only one on it.
     */
    static final float PUSH_LIFT = 0.45f;
    /** How long the push-back shockwave stays on screen. */
    static final float PUSH_TIME = 0.5f;
    /**
     * How long a shoved word takes to travel back. It slides; it does not teleport. A jump reads
     * as a glitch — you cannot see that the swipe moved *these* words and not the ones above the
     * halfway mark unless you watch them go. Slightly under {@link #PUSH_TIME} so the shockwave
     * is still climbing as the last word settles.
     */
    static final float PUSH_SLIDE = 0.4f;
    /**
     * How long the field stays winded after a shove, and how slowly it falls at the start of that.
     *
     * The distance the swipe buys is not the save on its own — the words simply come back down at
     * full speed, and against a late wave that was worth about a second. This is: everything crawls
     * and then winds back up, which is time to actually type rather than time to watch.
     *
     * Ramped rather than switched off at the end, because a field that snaps from a quarter speed to
     * full is the "position-driven animation" trap wearing a different hat — the recovery has to be
     * something you can see coming, or the wave appears to accelerate out of nowhere.
     */
    static final float PUSH_SLOW = 3f;
    static final float PUSH_SLOW_RATE = 0.25f;

    /** Length of the lunge animation between crossing the line and losing a life. */
    static final float ATTACK_TIME = 0.42f;
    /** Fraction of the descent over which a word counts as "closing in". */
    private static final float WARN_BAND = 0.20f;

    GameCore(Store store, long seed) {
        this(store, seed, !BuildFlags.DEVELOPER);
    }

    GameCore(Store store, long seed, boolean trackProgress) {
        this.store = store;
        this.progress = new Progress(store, trackProgress);
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
            LandPicker.restore(this, store.loadLandState());
            caveChoice = CaveDumpling.valid(store.loadCaveChoice());
            best = store.loadBest();
            for (int land = 0; land < Lands.COUNT; land++) landBests[land] = Math.max(0, store.loadLandBest(land));
            landBests[0] = Math.max(landBests[0], best);
            preferences.load(store.loadPlayerSettings());
            speed = BuildFlags.DEVELOPER ? clampSpeed(store.loadSpeed()) : 1f;
            bgmChoice = BuildFlags.DEVELOPER
                    ? Math.max(0, Math.min(Music.NAMES.length - 1, store.loadBgm()))
                    : Music.defaultChoice(false);
            // Masked: a store that hands back junk in the high bits must not make
            // Collect.owned() report more than there are entries.
            collected = store.loadCollected() & Collect.MASK;
            // Floored at what the case holds: a store from before this counter existed has nothing
            // to hand back, and reading zero next to a part-full case would tell the player they
            // had won nothing. Their collection is the floor on how many baskets they opened.
            collectTotal = Math.max(Collect.owned(collected),
                    Math.max(0, store.loadCollectTotal()));
            int[] savedCounts = store.loadCollectionCounts();
            long knownTotal = 0;
            for (int i = 0; i < Collect.COUNT; i++) {
                int saved = savedCounts != null && i < savedCounts.length ? savedCounts[i] : 0;
                // Old saves know ownership, but cannot attribute historical duplicates.
                collectionCounts[i] = Collect.has(collected, i) ? Math.max(1, saved) : 0;
                knownTotal += collectionCounts[i];
            }
            collectTotal = Math.max(collectTotal, (int) Math.min(Integer.MAX_VALUE, knownTotal));
            steamer.opens = Math.max(0, store.loadSteamerOpens());
            stars.wins = Math.max(0, Math.min(StarPath.MAX_DIFFICULTY, store.loadStarWins()));
            int roster = store.loadRosterState();
            fullRoster = (roster & 1) != 0;
            earlyLosses = Math.min(2, (roster >> 1) & 3);
            rosterLeavePending = (roster & 8) != 0;
            if (rosterLeavePending) beginRosterLeave();
        }
        progress.seed(this);
        progress.apply(this);
    }

    boolean playRosterFull() { return state == TITLE ? fullRoster : runFullRoster; }
    boolean keyActive(int glyph) {
        return Roster.active(playRosterFull(), glyph)
                && !boss.keyDisabled(glyph) && !boss.playerLocked();
    }
    int randomGlyph() { return Roster.random(playRosterFull(), rnd); }
    float rosterMix() {
        if (rosterScene == ROSTER_JOIN) return 1f - rosterSceneT / ROSTER_SCENE_TIME;
        if (rosterScene == ROSTER_LEAVE) return rosterSceneT / ROSTER_SCENE_TIME;
        if (rosterLeavePending) return 1f;
        return playRosterFull() ? 1f : 0f;
    }
    float keyScale() { return Roster.STARTER_SCALE + (1f - Roster.STARTER_SCALE) * rosterMix(); }
    float keyX(Layout L, int glyph) { return Roster.keyX(L, glyph, rosterMix()); }
    float keyY(Layout L, int glyph) { return Roster.keyY(L, glyph, rosterMix()); }
    int keyAt(float x, float y, Layout L) {
        int best = -1;
        float bestD = Float.MAX_VALUE, rr = L.keyR * keyScale();
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (!keyActive(g)) continue;
            float dx = x - keyX(L, g), dy = y - keyY(L, g), d = dx * dx + dy * dy;
            if (d <= rr * rr * 1.10f && d < bestD) { best = g; bestD = d; }
        }
        return best;
    }
    private void saveRoster() {
        if (store != null) store.saveRosterState((fullRoster ? 1 : 0)
                | (Math.min(2, earlyLosses) << 1) | (rosterLeavePending ? 8 : 0));
    }
    void unlockRoster() {
        if (fullRoster || kidsRun) return;
        fullRoster = runFullRoster = true; earlyLosses = 0; rosterLeavePending = false;
        rosterScene = ROSTER_JOIN; rosterSceneT = ROSTER_SCENE_TIME;
        if (sound != null) sound.rosterJoin();
        saveRoster();
    }
    private void beginRosterLeave() {
        rosterScene = ROSTER_LEAVE;
        rosterSceneT = ROSTER_SCENE_TIME; saveRoster();
    }

    static float clampSpeed(float v) {
        if (v != v) return 1f;                       // NaN from a corrupt store
        return v < SPEED_MIN ? SPEED_MIN : v > SPEED_MAX ? SPEED_MAX : v;
    }

    // ---- settings -----------------------------------------------------------

    void openSettings() {
        settingsOpen = true; settingsPage = BuildFlags.DEVELOPER ? 1 : 0;
        Pause.release(this);
        clearArmed = false;
    }

    void closeSettings() {
        settingsOpen = false;
        clearArmed = false;
    }

    void setStarDifficulty(int level) {
        if (!BuildFlags.DEVELOPER) return;
        int next = Math.max(0, Math.min(StarPath.MAX_DIFFICULTY, level));
        if (next == stars.wins) return;
        stars.wins = next;
        if (store != null) store.saveStarWins(next);
    }

    /** True once the clear-case button has been tapped and is waiting for a second. */
    boolean clearArmed;

    // ---- display case -------------------------------------------------------
    // Delegations; the browsing lives in CaseUi.

    void tapClearCase() { CaseUi.tapClear(this); }

    void openCase() { CaseUi.open(this); }

    void closeCase() { CaseUi.close(this); }

    void scrollCase(int dir) { CaseUi.scroll(this, dir); }

    void caseTo(int i) { CaseUi.to(this, i); }

    void beginCaseDrag(float x) { CaseUi.beginDrag(this, x, 0f); }

    void caseDragTo(float x, Layout L) { CaseUi.dragTo(this, x, 0f, L); }

    void scrollCaseRow(int dir) { CaseUi.row(this, dir); }

    void beginCaseDrag(float x, float y) { CaseUi.beginDrag(this, x, y); }

    void caseDragTo(float x, float y, Layout L) { CaseUi.dragTo(this, x, y, L); }

    void endCaseDrag() { CaseUi.endDrag(this); }

    /** One bounce of the send-off landing, pitched off the squishy doing the bouncing. */
    private void bounceTick() {
        if (sound != null) sound.squish(launchWho % Glyph.COUNT, 1);
    }

    void setNextRoster(boolean six) {
        if (!BuildFlags.DEVELOPER) return;
        fullRoster = six;
        earlyLosses = 0;
        rosterLeavePending = false;
        saveRoster();
    }

    void endCurrentRun() {
        if (!BuildFlags.DEVELOPER) return;
        if (state != PLAY) return;
        closeSettings();
        lives = 0;
        die();
    }

    void setSpeed(float v) {
        if (!BuildFlags.DEVELOPER) return;
        speed = clampSpeed(v);
        if (store != null) store.saveSpeed(speed);
    }

    /** Restores every persistent difficulty ladder to its first-play values. */
    void resetDifficultyScaling() {
        if (!BuildFlags.DEVELOPER) return;
        steamer.resetDifficulty();
        stars.resetDifficulty();
        if (store != null) {
            store.saveSteamerOpens(0);
            store.saveStarWins(0);
        }
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
        preferences.apply(this);
        if (sound != null) {
            if (boss.active()) sound.bossMusic(true);
            else sound.selectMusic(bgmChoice);
        }
    }

    void setBgm(int choice) {
        if (!BuildFlags.DEVELOPER) return;
        if (choice < 0 || choice >= Music.NAMES.length) return;
        bgmChoice = choice;
        if (store != null) store.saveBgm(choice);
        if (sound != null) {
            if (boss.active()) sound.bossMusic(true);
            else sound.selectMusic(choice);
        }
    }

    // ---- stage pacing -------------------------------------------------------
    // The dials themselves are in Pacing, which is pure. These are the delegations, plus the two
    // that are not pure: they read the boss and the frenzy.

    static final float RAMP = Pacing.RAMP;
    static final int MAX_PRESSES = Pacing.MAX_PRESSES;

    int pacingStage() { return kidsRun ? 1 : stage; }

    float ramp() { return Pacing.ramp(pacingStage()); }

    float travelSeconds() { return Pacing.travelSeconds(pacingStage(), kidsRun ? 1f : speed); }

    float spawnInterval() { return Pacing.spawnInterval(pacingStage(), kidsRun ? 1f : speed); }

    int maxEnemies() { return Pacing.maxEnemies(pacingStage()); }

    int maxWordLen() { return Pacing.maxWordLen(pacingStage()); }

    int minWordLen() { return Pacing.minWordLen(pacingStage()); }

    int stageQuota() { return Pacing.stageQuota(pacingStage()); }

    float stackChance() { return Pacing.stackChance(pacingStage()); }

    /** Concurrent words allowed now. A frenzy lets more pile up, tapering with the ramp. */
    int crowdCap() {
        if (powerActive()) return (int) (maxEnemies() * Power.crowdRate(ramp()));
        // A boss stage releases no words at all.
        if (boss.active()) return 0;
        return maxEnemies();
    }

    /**
     * True once every word of this stage has been released and dealt with. A boss stage is never
     * clear while the boss is on it — only beating it satisfies the quota, see BossPlay.endBoss.
     */
    boolean stageCleared() {
        return !boss.active() && spawnedThisStage >= stageQuota()
                && enemies.isEmpty() && shots.isEmpty()
                && !(power != null && power.mystery && power.hit && !power.activated);
    }

    // ---- lifecycle ----------------------------------------------------------

    /**
     * Begins the title screen's fade out. The tone leads it rather than following, so the press
     * is acknowledged immediately and the triad carries over into the first wave.
     */
    void beginStart() {
        if (state != TITLE || starting() || returnFade > 0f || rosterSceneT > 0f) return;
        startFade = START_FADE;
        // The entry the case was showing comes along, if it is one you own. Set before the
        // fade is under way so the send-off leaves from the badge rather than from a screen
        // that has already gone.
        launchWho = Collect.has(collected, caseIndex) ? caseIndex : -1;
        launchT = launchWho >= 0 ? Launch.TIME : 0f;
        launchClock = clock;
        launchPips = 0;
        closeCase();
        closeStory();
        startAnnounced = true;
        if (sound != null) sound.gameStart();
    }

    void cancelStart() {
        startFade = launchT = 0; launchWho = -1; startAnnounced = false;
    }

    void startGame() {
        runWho = Collect.has(collected, caseIndex) ? caseIndex : 0;
        // A paid win may have been quit before its tableau/parade retired the course.
        if (stars.count() == StarPath.COUNT) {
            stars.make(rnd);
            starNext = false;
        }
        runStartLand = LandPicker.unlocked(this, landChoice) ? landChoice : 0;
        landChoice = runStartLand;
        best = landBests[runStartLand];
        landPickerDragging = false;
        progress.startRun(runStartLand);
        Pause.resume(this);
        state = PLAY;
        kidsRun = preferences.kids;
        runFullRoster = !kidsRun && fullRoster;
        time = 0;
        score = 0;
        squishes = 0;
        stage = runStartLand * Boss.EVERY + 1;
        spawnedThisStage = 0;
        resolvedThisStage = 0;
        stageGap = 0;
        perfectBanner = 0;
        hits = 0;
        misses = 0;
        missesThisStage = 0;
        hurtThisStage = 0;
        earnedMash = MASH_HURT;
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
        // The case is a title-screen thing, and it must not be found still up on the way back.
        caseOpen = false;
        caseFade = 0f;
        caseDragging = false;
        // The send-off has done its job by the time the first word falls.
        launchWho = -1;
        launchT = 0f;
        pushUsed = false;
        pushT = 0f;
        pushSlowT = 0f;
        pushCount = 0;
        // The collection itself survives; only the "you just won this" banner is per-run.
        prize = -1;
        prizeNew = false;
        // This run's haul starts empty, and no death or flight can be left running into it.
        roundPrizes = 0L;
        cubeUnlocked = false;
        deathT = 0f;
        bossVictoryKind = -1;
        bossVictory = null;
        homeT = 0f;
        homeLanded = 0;
        paradeTimer = 0f;
        bossPrizePending = bossReward = false;
        power = null;
        mode = -1;
        modeLeft = 0;
        debuffLeft = monochromeFade = incognitoMorph = 0f;
        buddy.leave();
        // Stage 1 is never a boss stage, so this is only ever clearing one a previous run left
        // standing — but a run must not begin with the last one's boss still on the field.
        boss.leave();
        stageByPower = false;
        powerTimer = Power.SPAWN_MIN;
        pendingBonus = false;
        Lands.fromIntro(this);
        stageBanner = BANNER_TIME;
        startFade = 0f;
        if (sound != null) {
            sound.frenzy(false);
            if (!startAnnounced) sound.gameStart();
        }
        startAnnounced = false;
        cave.begin(this);
    }

    void dismissGameOver() {
        if (overReady()) returnToTitle();
    }

    void returnToTitle() {
        if (returnFade > 0f) return;
        if (state == OVER) returnFade = RETURN_FADE;
        else toTitle();
    }

    void toTitle() {
        cave.leave();
        progress.finishRun(score, true);
        Pause.resume(this);
        boolean hadHaul = state == OVER && roundPrizes != 0L;
        state = TITLE;
        progress.apply(this);
        time = 0;
        deathT = 0f;
        bossVictoryKind = -1;
        bossVictory = null;
        // The run's haul carries itself to the case rather than simply being in it next time the
        // case is opened. Only off the game-over screen: arriving from anywhere else there is no
        // dance for them to be leaving.
        homeT = hadHaul ? HOME_TIME : 0f;
        homeLanded = 0;
        enemies.clear();
        shots.clear();
        target = null;
        // The case starts closed on every visit, and must not open part-way through a slide left
        // over from the last one.
        caseOpen = false;
        caseFade = 0f;
        caseDragging = false;
        caseSlide = caseSlideY = 0f;
        launchWho = -1;
        launchT = 0f;
        closeStory();
        resetTitleSprings();
        if (rosterLeavePending) beginRosterLeave();
    }

    private void resetTitleSprings() {
        for (int i = 0; i < TITLE_LETTERS; i++) {
            titleSpringX[i] = titleSpringY[i] = 0f;
            titleSpringVX[i] = titleSpringVY[i] = 0f;
        }
    }

    /** Ten lightly coupled damped bodies: always alive, but never wandering from the logo. */
    private void updateTitleSprings(float dt, Layout L) {
        if (state != TITLE || dt <= 0f) return;
        dt = Math.min(dt, 1f / 20f);
        for (int i = 0; i < TITLE_LETTERS; i++) {
            float phase = clock * 0.75f + i * 0.91f;
            float restX = (float) Math.sin(phase * 0.73f + 1.1f) * L.unit * 0.07f;
            float restY = ((float) Math.sin(phase) * 0.31f
                    + (float) Math.sin(phase * 1.71f + 1.4f) * 0.11f) * L.unit;
            float ax = (restX - titleSpringX[i]) * 24f - titleSpringVX[i] * 6.8f;
            float ay = (restY - titleSpringY[i]) * 24f - titleSpringVY[i] * 6.8f;

            // A loose elastic thread through each row lets one poked letter tug its neighbours.
            int left = i % 5 == 0 ? -1 : i - 1;
            int right = i % 5 == 4 ? -1 : i + 1;
            if (left >= 0) {
                ax += (titleSpringX[left] - titleSpringX[i]) * 2.2f;
                ay += (titleSpringY[left] - titleSpringY[i]) * 2.2f;
            }
            if (right >= 0) {
                ax += (titleSpringX[right] - titleSpringX[i]) * 2.2f;
                ay += (titleSpringY[right] - titleSpringY[i]) * 2.2f;
            }

            if (titleTouchDown) {
                float x = Screens.titleAnchorX(i, L) + titleSpringX[i];
                float y = Screens.titleAnchorY(i, L) + titleSpringY[i];
                float dx = x - titleTouchX, dy = y - titleTouchY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float reach = L.unit * 7.2f;
                if (distance < reach) {
                    float force = (1f - distance / reach) * L.unit * 105f;
                    if (distance < 1f) { dx = (i & 1) == 0 ? -1f : 1f; dy = -1f; distance = 1.414f; }
                    ax += dx / distance * force;
                    ay += dy / distance * force - L.unit * 18f;
                }
            }
            titleSpringVX[i] += ax * dt;
            titleSpringVY[i] += ay * dt;
            titleSpringX[i] += titleSpringVX[i] * dt;
            titleSpringY[i] += titleSpringVY[i] * dt;
            float limit = L.unit * 2.4f;
            titleSpringX[i] = Math.max(-limit, Math.min(limit, titleSpringX[i]));
            titleSpringY[i] = Math.max(-limit, Math.min(limit, titleSpringY[i]));
        }
    }

    float titleSpringShape(int i, Layout L) {
        float shape = 1f + titleSpringVY[i] / Math.max(1f, L.unit * 28f);
        return Math.max(0.84f, Math.min(1.18f, shape));
    }

    /**
     * A key press on the title or game-over screen.
     *
     * Every key does the same thing on each: start a run from the title, go back to the title
     * from game over. Splitting the deck by role — inner four to start, outer two for the case
     * or a replay — meant the same six letters meant one thing on one screen and something else
     * on the next, and a key that does nothing where it sits looks broken. A replay is two
     * presses of anything now, which also puts the display case back on the way past.
     */
    void screenKey(int g) {
        if (settingsOpen) return;
        if(releaseNotes.open) return;
        if (returnFade > 0f) return;
        if (!keyActive(g) || rosterSceneT > 0f) return;
        // Nothing is dismissable until the summary is up and settled — the death sequence is not
        // something to be pressed through, and a screen that arrives under a thumb reads as a
        // misfire rather than as an answer.
        if (state == OVER && !overReady()) return;
        // Already on the way out: further presses would restart the fade or double the tone.
        if (starting()) return;
        keyPress[g] = 1f;
        // A story on screen swallows the first press. Without this an inner key would start a
        // run from behind the panel, which is the one thing a modal must not allow.
        if (storyOpen()) {
            closeStory();
            return;
        }
        // So does the case. It is somebody reading their collection, and a run starting out from
        // under them is worse than a press that only put the case away.
        if (caseOpen) {
            closeCase();
            return;
        }
        if (state == TITLE) {
            beginStart();
        } else {
            returnToTitle();
        }
    }

    // ---- input --------------------------------------------------------------

    /** Player pressed key {@code g}. Returns true when it advanced a word. */
    boolean tapKey(int g, Layout L) {
        if (paused) return false;
        if (boss.kind == Boss.SLIME && boss.slimeKeyLock > 0f
                && Roster.active(playRosterFull(), g)) {
            boss.slimeBlobPulse[g] = 0.15f;
            keyPress[g] = 1f;
            return false;
        }
        if (!keyActive(g) || rosterSceneT > 0f) return false;
        if (state != PLAY) {
            if (state != BONUS) screenKey(g);
            return false;
        }
        keyPress[g] = 1f;
        if (Cave.active(this)) return cave.press(this, g, L);

        if (target != null && (!target.typeable() || !enemies.contains(target)
                || (powerActive() && mode == Power.FLING && target.link != null))) target = null;

        // The boss, on the same terms the powerup gets: it outranks an *unengaged* word for the
        // letters it is asking for, and never steals a press out of a word already part-typed. The
        // one exception is a key it is holding — that is refused wherever it is pressed, including
        // into an engaged word, because the player does not have that key at all.
        if (boss.fighting() && BossPlay.claims(this, g)) {
            float beforeHp = boss.hp;
            int verdict = boss.press(g, rnd, L);
            progress.bossDamage(boss.kind, beforeHp, boss.hp);
            if (verdict != Boss.NONE) return BossPlay.press(this, g, verdict, L);
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
                if (powerActive() && mode == Power.FLING && e.link != null) continue;
                if (!flurry() && e.word[e.pos] != g) continue;
                boolean guided = e.link != null && (e.link.linkWaiting || e.link.dying);
                boolean pickedGuide = pick != null && pick.link != null
                        && (pick.link.linkWaiting || pick.link.dying);
                if (pick == null || (guided && !pickedGuide)
                        || (guided == pickedGuide && e.y > pick.y)) pick = e;
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
        if (flurry() && e.link != null && e.link.linkWaiting && e.link.linkButton == g) {
            if (sound != null) sound.wrong();
            target = null;
            return false; // Two distinct buttons, never a double-tap of one wildcard.
        }
        if (e.link != null) e.linkButton = g;
        int struck = e.pos;
        float hx = tileX(e, struck, L);
        float hy = e.y;
        // Under FLURRY the pressed key is a wildcard, so the shot and the wash take the
        // letter actually being hit rather than whatever was pressed.
        int lit = e.word[struck];

        // A stacked tile absorbs several presses of the same letter before it clears.
        if (sound != null) {
            // The first half meets the bond's resistance; only the completing key hits.
            if (e.link != null && !e.link.linkWaiting) sound.wrong();
            else sound.squish(lit, pressesLeft(e, struck));
        }
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
        // A chord scores only when both keys land; retries cannot farm points or combo.
        if (e.link == null) {
            combo++;
            if (combo > maxCombo) maxCombo = combo;
            score += 5 + Math.min(combo, 25) / 2;
        }

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
        s.sx = keyX(L, g);
        s.sy = keyY(L, g);
        s.tx = hx;
        s.ty = hy;
        s.glyph = lit;
        s.target = e;
        s.kill = kill;
        s.tileIndex = struck;
        s.dur = SHOT_TIME;
        shots.add(s);
        if (e.link != null) {
            // A 200ms chord resolves at input time. Its shots are visual only, including
            // shots still travelling after a missed window, so they cannot clear a retry.
            s.kill = false;
            destroyWord(e, hx, hy, L);
        }
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
        Enemy partner = e.link;
        if (partner != null) {
            destroyWord(e,buddy.x,buddy.y,L,false);
            destroyWord(partner,buddy.x,buddy.y,L);
            e.spinMate = partner; partner.spinMate = e;
        } else {
            destroyWord(e, buddy.x, buddy.y, L);
            computeImpactFlyDirs(e, buddy.x, buddy.y, L);
        }
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

    /**
     * The push-back: shoves every word in the bottom half of the field back up, calls off any
     * lunge already committed, and is then spent for the stage.
     *
     * A lunge is cancellable on purpose. The gesture exists for exactly the moment a word has
     * crossed the line and is coming for you; a panic button that cannot save you then is not
     * worth the once-a-stage it costs.
     *
     * Resolved here, played back over {@link #PUSH_SLIDE}: the threat is settled the instant the
     * swipe lands — warning cleared, lunge called off, the stage's use spent — and only the
     * travel is spread over the following moment by the update loop.
     *
     * @return true when it fired, so the view can leave the gesture alone
     */
    boolean pushBack(Layout L) {
        if (!pushReady()) return false;
        float mid = (L.playTop + L.dangerY) / 2f;
        float lift = (L.dangerY - L.playTop) * PUSH_LIFT;

        // Shoved words are found lowest-first. If one would land on a higher word, that higher
        // word is smashed instead of joining a tidy cascade: this is a desperation move.
        java.util.List<Enemy> order = new java.util.ArrayList<Enemy>();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.destroyed && !e.dying) order.add(e);
        }
        java.util.Collections.sort(order, new java.util.Comparator<Enemy>() {
            public int compare(Enemy a, Enemy b) {
                return Float.compare(b.y, a.y);
            }
        });

        // A row is two tile radii tall, so that is the distance at which two of them collide.
        float clash = 2f * L.enemyR;
        java.util.List<Float> taken = new java.util.ArrayList<Float>();
        int moved = 0;
        for (int i = 0; i < order.size(); i++) {
            Enemy e = order.get(i);
            boolean shove = e.y >= mid;
            boolean crushed = false;
            for (int k = 0; k < taken.size(); k++) {
                if (Math.abs(e.y - taken.get(k)) < clash) {
                    float ix = enemyCentreX(e), iy = e.y;
                    destroyWord(e, ix, iy, L, false);
                    computeUpwardFlyDirs(e, L);
                    PushImpact hit = new PushImpact();
                    hit.x = ix;
                    hit.y = iy;
                    hit.color = Glyph.COLOR[e.word[Math.min(e.pos, e.word.length - 1)]];
                    pushImpacts.add(hit);
                    Fx.explodeUp(this, rnd, ix, iy, L.enemyR * 1.8f, 24, hit.color);
                    crushed = true;
                    break;
                }
            }
            if (crushed) continue;
            if (!shove) continue;
            e.attacking = false;
            e.attackT = 0f;
            e.warn = 0f;
            e.slideFrom = e.y;
            // Clamped at the top edge, so a field full enough to need the whole board moved can
            // still end with a couple of rows piled against the ceiling. Better than a word shoved
            // off the screen it has to be typed on.
            e.slideTo = Math.max(L.playTop, e.y - lift);
            e.slideT = PUSH_SLIDE;
            taken.add(e.slideTo);
            // At its feet where it stands, not where it is going: this is the shove landing.
            Fx.explode(this, rnd, enemyCentreX(e), e.y + L.enemyR * 1.4f, L.enemyR * 1.2f, 8,
                    Glyph.COLOR[e.word[e.pos]]);
            moved++;
        }
        // pushReady needs warnLevel above zero, which needs a word inside the warning band,
        // which is inside the bottom half — so this cannot happen. It is here so that if the
        // bands are ever retuned apart, the stage's one use is not silently eaten.
        if (moved == 0) return false;

        pushUsed = true;
        pushCount = moved;
        pushT = PUSH_TIME;
        // The field is winded by it. Distance alone was not much of a save: the words came straight
        // back down at full speed, and against a late wave the swipe bought about a second. The drag
        // is where the recovery actually lives.
        pushSlowT = PUSH_SLOW;
        // Cleared here as well as by the loop: the edge glow reads it, and it would otherwise
        // hold last frame's alarm for a field that is no longer in danger.
        warnLevel = 0f;
        shake = Math.max(shake, 0.55f);
        flash = Math.max(flash, 0.5f);
        flashColor = FLASH_CLEAR;
        skyGlow = 1f;
        skyGlowColor = FLASH_CLEAR;
        if (sound != null) sound.achievement();
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

    /** Correct presses as a fraction of all presses; 0 before anything is pressed. */
    float accuracy() {
        int total = hits + misses;
        return total == 0 ? 0f : (float) hits / total;
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
        if (Cave.active(this)) return cave.wanted();
        if (state != PLAY || target == null || target.pos >= target.word.length) return -1;
        return target.word[target.pos];
    }

    // ---- geometry helpers (shared by renderer and hit feedback) -------------

    float enemyCentreX(Enemy e) {
        float x = e.baseX + e.sway * (float) Math.sin(clock * 1.1f + e.phase);
        if (e.link != null) x += (e.link.baseX-e.baseX)*0.035f*e.linkFlex;
        return x;
    }

    /** Ease the side entrance into a vertical lane without changing its fall speed. */
    void updateSidePath(Enemy e, Layout L) {
        if (!e.sideEntry) return;
        e.baseX = EnemyEntry.xAt(e, e.y, L);
        e.enterT = Math.min(1f, EnemyEntry.progress(e, e.y, L) * 3f);
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

    void update(float dt, Layout L) { update(dt, dt, L); }

    void update(float dt, float elapsed, Layout L) {
        starPickups = 0;
        if (paused) return;
        if(releaseNotes.open) {
            releaseNotes.update(elapsed,L);
            clock+=elapsed;time+=elapsed;skyClock+=elapsed;
            updateTitleSprings(elapsed,L);LandPicker.updateTravel(this,elapsed);
            return;
        }
        releaseMascot.update(this,elapsed);
        if (state == PLAY && boss.fighting() && !settingsOpen)
            progress.bossTime(elapsed);
        // Slow motion from a multi-word fling stroke, and the readout it earned. Both ticked
        // on real time and before the scaling below, so neither is slowed by the thing the
        // beat is slowing.
        if (slowdown > 0f) slowdown = Math.max(0f, slowdown - dt);
        if (sliceCall > 0f) sliceCall = Math.max(0f, sliceCall - dt);
        // What ends a blade stroke by itself, on real time and above every early return below.
        Blade.updateStroke(this, dt);
        dt *= timeScale();
        if (kidsRun && (state == PLAY || state == BONUS)) dt *= .45f;
        // The clock keeps running so the panel itself can animate, but nothing else moves.
        clock += dt;
        updateTitleSprings(dt, L);
        if (rosterSceneT > 0f) {
            rosterSceneT = Math.max(0f, rosterSceneT - dt);
            if (rosterSceneT == 0f) {
                if (rosterScene == ROSTER_LEAVE) { rosterLeavePending = false; saveRoster(); }
                rosterScene = 0;
            }
        }
        if (sound != null && (settingsOpen || !boss.fighting() || boss.kind != Boss.SLIME
                || boss.hasGlob() || boss.boltCount() > 0)) sound.bossCharge(0f);
        if (settingsOpen) return;
        time += dt;
        if (returnFade > 0f) {
            returnFade = Math.max(0f, returnFade - dt);
            if (state == OVER && returnFade <= RETURN_FADE * 0.5f) toTitle();
        }
        // Accumulated, not derived from clock, so the frenzy's faster drift does not make the
        // sky jump when it starts or stops.
        skyClock += dt * (powerActive() ? Power.SKY_RATE : 1f);
        landBlend = Math.min(1f, landBlend + dt / Lands.FADE_TIME);
        LandPicker.updateDiscovery(this, dt);
        LandPicker.updateTravel(this, dt);

        for (int i = 0; i < Glyph.COUNT; i++) {
            keyPress[i] = decay(keyPress[i], dt * 5.5f);
            keyBad[i] = decay(keyBad[i], dt * 3.2f);
        }
        shake = decay(shake, dt * 2.6f);
        flash = decay(flash, dt * 2.2f);
        skyGlow = decay(skyGlow, dt * 2.4f);
        float stageIntroLeft = stageBanner;
        stageBanner = decay(stageBanner, dt);
        perfectBanner = decay(perfectBanner, dt);
        pushT = decay(pushT, dt);
        for (int i = pushImpacts.size() - 1; i >= 0; i--) {
            PushImpact hit = pushImpacts.get(i);
            hit.life -= dt;
            if (hit.life <= 0f) pushImpacts.remove(i);
        }
        // Above the PLAY return with the rest of them: a run that ends mid-drag must not leave the
        // next one starting at a quarter speed.
        pushSlowT = decay(pushSlowT, dt);
        // The death hold, and the flight home that follows it a screen later. Both above the PLAY
        // return: neither runs during play, and the states they do run in never reach it.
        if (deathT > 0f) {
            deathT = Math.max(0f, deathT - dt);
            if (deathT == 0f) {
                // The swirl is over, so the words go. Held until now because they are what the
                // swirl is made of; the summary is drawn over an empty field from here.
                enemies.clear();
                target = null;
                // And the run gets its full stop here rather than on the fatal breach, which
                // already has the damage drip on it — two effects on one frame is one of them
                // wasted, and this belongs to the summary coming up, not to the last word.
                if (sound != null) {
                    if (bossVictoryKind >= 0) sound.bossMusic(false);
                    sound.gameOver();
                }
            }
        }
        if (homeT > 0f) {
            homeT = decay(homeT, dt);
            // A chime as each one is taken in. Fired from here rather than from the drawing so it
            // lands on the frame the flyer does and exactly once, and read off the same arrival
            // times the flight is drawn from — the flight owns when they land, and one copy of
            // that is one more than the two it had before.
            int haul = RoundEnd.hauled(this);
            float u = homeT > 0f ? 1f - homeT / HOME_TIME : 1f;
            while (homeLanded < haul && RoundEnd.arrival(homeLanded, haul) <= u) {
                if (sound != null) sound.collect(homeLanded);
                homeLanded++;
            }
        }
        if (storyOpen()) storyT += dt;
        // The title screen dissolving, and the squishy's send-off over the top of it. Play begins
        // the frame the last of them finishes, not on the press.
        if (state == TITLE && starting()) {
            startFade = Math.max(0f, startFade - dt);
            if (launchT > 0f) {
                launchT = Math.max(0f, launchT - dt);
                float u = Launch.progress(this);
                // One tick per bounce, as it happens. The impacts are what the sound is for.
                if (launchPips == 0 && u >= Launch.LAND) {
                    launchPips = 1;
                    bounceTick();
                } else if (launchPips == 1 && u >= Launch.TOP) {
                    launchPips = 2;
                    bounceTick();
                }
            }
            if (startFade == 0f && launchT == 0f) {
                startGame();
                return;
            }
        }
        // The case easing in on a tap and out again on close. Above the PLAY return, since the
        // title screen never reaches it.
        float cf = dt * CASE_FADE_RATE;
        caseFade = caseOpen ? Math.min(1f, caseFade + cf) : Math.max(0f, caseFade - cf);
        if (caseOpen) { caseT += dt; caseHighlightAge += dt; }
        // Signed, so it eases back to zero from whichever side the scroll came in on. Left alone
        // under a finger: there the offset is the drag, not a leftover.
        float caseMotionDecay = Math.max(0f, 1f - dt * 8f);
        casePanMotionX *= caseMotionDecay;
        casePanMotionY *= caseMotionDecay;
        if (caseSlideY != 0f && !caseDragging && !caseFreePan) {
            float d = dt * Showcase.SLIDE_RATE;
            caseSlideY = caseSlideY > 0f ? Math.max(0f, caseSlideY - d)
                    : Math.min(0f, caseSlideY + d);
        }
        if (caseSlide != 0f && !caseDragging && !caseFreePan) {
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
            if (bossReward) {
                bonusTimer = Math.max(0f, bonusTimer - dt);
                if (!joinRung && bonusTimer < BossCollect.REVEAL_TIME - 0.9f) {
                    joinRung = true;
                    if (sound != null) sound.paradeJoin();
                }
                if (bonusTimer == 0f) {
                    bossReward = false;
                    advanceStage();
                    state = PLAY;
                    time = 0f;
                }
                return;
            }
            if (starBonus) {
                int heldStars = stars.collected;
                boolean wasFinishing = stars.won || stars.exiting() || stars.reporting();
                stars.update(dt, L);
                starPickups = Integer.bitCount(stars.collected & ~heldStars);
                boolean finishing = stars.won || stars.exiting() || stars.reporting();
                if (sound != null) {
                    sound.rocket(!finishing && stars.flying()
                            ? 0.15f + 0.85f * stars.flightProgress() : 0f);
                    if (!wasFinishing && finishing) sound.courseFinish();
                }
                bonusTimer = stars.timer;
                if (stars.grabbed) {
                    // The fling stroke's beat, briefly: a taken star lands with the same stutter and
                    // the same gold vignette, so the two read as the same kind of moment.
                    stars.grabbed = false;
                    slowdown = Blade.STAR_BEAT;
                    // Announced with the count, so the note climbs as the course fills.
                    if (sound != null) sound.star(stars.count());
                }
                if (stars.launched) {
                    // The lesson is over and the course is moving. One per attempt, and the sound
                    // of the ease-in rather than of a starting gun: see Sfx.course.
                    stars.launched = false;
                    if (sound != null) sound.courseStart();
                }
                if (stars.reported) {
                    // A course that ran out. The count is about to be read on screen and used to
                    // be read in silence; a won course rings the fanfare instead.
                    stars.reported = false;
                    progress.finishMinigame(false);
                    if (sound != null) sound.tally(stars.count());
                }
                if (stars.awardPending) {
                    // Paid the moment the last star lands, not when the interlude ends: the victory
                    // tableau shows what was won, so the prize has to exist before it is drawn.
                    stars.awardPending = false;
                    progress.finishMinigame(true);
                    stars.recordWin();
                    if (store != null) store.saveStarWins(stars.wins);
                    score += FREE_BONUS;
                    if (lives < START_LIVES) lives++;
                    Interlude.awardStarPrize(this);
                }
                if (stars.timer <= 0f) {
                    stars.finishAttempt();
                    // The parade closes a won course exactly as it closes a won steamer, and for
                    // the same reason: the collection is the point of winning one.
                    if (paradeTimer > 0f) {
                        paradeTimer -= dt;
                        Interlude.joinChord(this);
                        if (paradeTimer > 0f) return;
                        paradeTimer = 0f;
                    }
                    if (stars.won) {
                        starNext = false;
                        stars.make(rnd);
                    }
                    advanceStage();
                    state = PLAY;
                    time = 0f;
                }
                return;
            }
            steamer.update(dt);
            // Once the interlude's own countdown is spent, a won prize gets its parade before
            // play resumes. Handled before bonusTimer is touched again so none of the four
            // phases tick underneath it.
            if (bonusTimer <= 0f) {
                if (paradeTimer > 0f) {
                    paradeTimer -= dt;
                    Interlude.joinChord(this);
                    if (paradeTimer > 0f) return;
                    paradeTimer = 0f;
                }
                advanceStage();
                state = PLAY;
                time = 0;
                return;
            }
            bonusTimer -= dt;
            if (bonusTimer <= MASH_END && steamer.swipeReady) steamer.missSwipe();
            // One tick per character the spinner steps past, so it sounds like a spin. Only
            // the left slot fires: both would double up on almost every step.
            if (bonusRolling()) {
                int shown = bonusLeftKey();
                if (shown != rollTick) {
                    rollTick = shown;
                    if (sound != null) sound.squish(shown, 1);
                }
            }
            // The status page, which is this game's version of the star course's report: the count
            // it opened is read out. The same tally tone as the course's, pitched by what it got —
            // the two interludes end the same way and now sound like it.
            if (!statusRung && bonusStatus()) {
                statusRung = true;
                progress.finishMinigame(false);
                if (sound != null) sound.tally(steamer.opens);
            }
            return;
        }

        if (state != PLAY) return;
        if (rosterScene == ROSTER_JOIN) return;

        // Holding between the wave ending and the interlude opening, so the flawless-wave
        // dumpling gets the screen to itself. Nothing spawns and nothing falls; the field is
        // already empty, which is what let the wave end.
        if (pendingBonus) {
            if (perfectBanner <= 0f) {
                pendingBonus = false;
                Interlude.enterBonus(this, L);
            }
            return;
        }

        if (Cave.active(this)) {
            // The arrival skit owns the field before expedition time can advance.
            float caveDt = cave.phase == Cave.CHOOSE ? dt : Math.max(0f, dt - stageIntroLeft);
            if (caveDt > 0f) {
                stageGap = 0f;
                cave.update(this, caveDt, L);
            }
            return;
        }

        updatePower(dt, L);
        Blade.updateTrail(this, dt, L);
        if (team()) buddy.update(this, dt, L);
        else if (!buddy.out()) buddy.leave();

        if (boss.active()) {
            // Visible projectiles reaching the deck cost lives; elapsed fight time alone does not.
            float beforeHp = boss.hp;
            float priorCover=boss.slimePromptCover();
            boolean priorOpen=boss.open();
            boolean beforeSupernova = boss.kind == Boss.SPLITTER && boss.beaten && !DivideDeath.bursting(boss);
            int bossHits = boss.update(dt, L, rnd);
            if (beforeSupernova && DivideDeath.bursting(boss) && sound != null) sound.divideSupernova();
            float cover=boss.slimePromptCover();
            if(sound!=null && boss.kind==Boss.SLIME && boss.fighting() && boss.slimePromptHits>=2
                    && !boss.hasGlob() && boss.boltCount()==0 && !boss.slimeRetaliating) {
                if(!priorOpen && boss.open()) sound.slimeCover(true);
                else if(priorCover==0f && cover>0f && boss.open()) sound.slimeCover(false);
            }
            progress.bossDamage(boss.kind, beforeHp, boss.hp);
            if (boss.octoPlayerHit && state == PLAY) BossPlay.octoWhipHit(this, L);
            if (boss.octoImpact) {
                shake = Math.max(shake, 1.12f);
                flash = Math.max(flash, 1f);
                flashColor = 0xFFFF355F;
            }
            if (sound != null) {
                float brew = boss.kind == Boss.SLIME && !boss.hasGlob()
                        && boss.boltCount() == 0 && boss.open()
                        ? 0.08f + boss.promptProgress() * 0.92f
                        : boss.kind == Boss.MUSHROOM && boss.mushroomCharge > 0f
                        ? 1f - boss.mushroomCharge / Boss.MUSHROOM_CHARGE_TIME : 0f;
                sound.bossCharge(brew);
                if (boss.launched && boss.kind != Boss.MUSHROOM) sound.bossLaugh();
                if (boss.mushroomSporeCue) sound.mushroomSpore();
                if (boss.boingWeight >= 0f) sound.divideBoing(boss.boingWeight);
                if (boss.octoCue) sound.octoCue();
                if (boss.octoLock) sound.octoLock();
                if (boss.defeatChime) sound.squish(Boss.FACE[boss.kind], boss.defeatBeat);
            }
            for (int k = 0; k < bossHits && state == PLAY; k++) BossPlay.slam(this, L);
            // That may have been the last life, and nothing below here runs after a run ends.
            if (state != PLAY) return;
            if (boss.gone()) {
                BossPlay.endBoss(this, L);
                return;
            }
        }

        // Stages are discrete waves: a stage releases exactly stageQuota() words, and the
        // next stage cannot start arriving until the field is completely clear.
        if (stageGap > 0) {
            stageGap -= dt;
        } else if (powerActive() || (!boss.active() && spawnedThisStage < stageQuota())) {
            if (powerActive()) spawnTimer = Math.min(spawnTimer, Power.spawnDelay(this, L));
            spawnTimer -= dt;
            // Counted against live words only: a word already flying apart is no longer
            // occupying the field as far as pacing is concerned. During a frenzy the quota
            // is ignored: words keep coming until the timer runs out and ends the stage.
            //
            // A boss stage spawns nothing at all. It used to run a thin wave underneath the fight,
            // and the fight is the stage — the words were dividing attention away from the thing
            // the stage is actually about, and they took the screen the boss needs.
            if (spawnTimer <= 0 && liveEnemies() < crowdCap()) {
                if (LinkedPairs.spawn(this, L)) {
                    if (powerActive() && powerRefillBurst > 0) powerRefillBurst--;
                    spawnTimer = Power.spawnDelay(this, L);
                } else if (!LinkedPairs.due(this) && spawn(L)) {
                    if (powerActive() && powerRefillBurst > 0) powerRefillBurst--;
                    if (powerActive()) powerSpawnedEnemies++;
                    else spawnedThisStage++;
                    spawnTimer = Power.spawnDelay(this, L);
                } else {
                    spawnTimer = 0.1f; // No clear entrance yet; keep the wave quota outstanding.
                }
            }
        } else if (stageCleared()) {
            Interlude.beginStageEnd(this);
            return;
        }

        LinkedPairs.update(this, elapsed);
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
                if (e.destroyT >= (e.spinMate != null ? LinkedPairArt.SPIN_TIME : DESTROY_TIME)) enemies.remove(i);
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

            if (e.slideT > 0f) {
                // Travelling back under the push. This replaces the descent rather than fighting
                // it: a word being thrown back is not also falling.
                e.slideT = Math.max(0f, e.slideT - dt);
                float k = 1f - e.slideT / PUSH_SLIDE;
                // Eased out — shoved hard, settling. Quadratic and not cubic: cubic spends most
                // of the distance in the first few frames, which is the teleport again with a
                // tail on it.
                k = 1f - (1f - k) * (1f - k);
                e.y = e.slideFrom + (e.slideTo - e.slideFrom) * k;
                updateSidePath(e, L);
                // No warn recompute and no breach check while it travels, both on purpose. The
                // word is still below the line for these frames, and rearming a lunge on the way
                // up would undo the swipe that just called it off. The threat was resolved when
                // the swipe landed; this is only the playback.
                continue;
            }

            e.y += e.speed * fallRate() * dt;
            updateSidePath(e, L);
            if (e.linkWaiting) {
                e.warn = 0f;
                e.y = Math.min(e.y, L.dangerY - L.enemyR * 1.1f);
                continue;
            }
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
        if (e.destroyed || LinkedPairs.cleared(this, e, L)) return;
        // The word is credited now but stays listed until it has flown apart, so anything
        // gated on the field being clear waits for the animation.
        if (powerActive() && !e.destroyed) {
            if (clock - powerLastClear <= 0.25f) powerRefillBurst = 2;
            powerLastClear = clock;
        }
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
        resolveStageEnemy(e);
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
    // Delegations; the gesture lives in Blade.

    void beginStroke(float x, float y) { Blade.begin(this, x, y); }

    int sliceTo(float x, float y, Layout L) { return Blade.sliceTo(this, x, y, L); }

    void endStroke() { Blade.end(this); }

    /**
     * Picks which way each tile of a cleared word flies. Tiles head for whichever screen
     * edge is nearer, except that the first and last tile always go left and right
     * respectively, so a word visibly splits apart rather than sliding off as a block.
     */
    /** Not private: the title screen's demo fans its word out the same way when it is cleared. */
    void computeFlyDirs(Enemy e, Layout L) {
        e.radialFly = false;
        int n = e.word.length;
        e.flyDir = new float[n];
        e.flyY = new float[n];
        float mid = L.w / 2f;
        for (int i = 0; i < n; i++) {
            float dir = tileX(e, i, L) < mid ? -1f : 1f;
            if (n > 1) {
                if (i == 0) dir = -1f;
                else if (i == n - 1) dir = 1f;
            }
            e.flyDir[i] = dir;
            e.flyY[i] = i % 2 == 0 ? -0.15f : 0.15f;
        }
    }

    /** TEAM SQUISH sends every tile directly away from the buddy collision. */
    void computeImpactFlyDirs(Enemy e, float px, float py, Layout L) {
        int n = e.word.length;
        e.flyDir = new float[n];
        e.flyY = new float[n];
        e.radialFly = true;
        for (int i = 0; i < n; i++) {
            float dx = tileX(e, i, L) - px;
            float dy = e.y - py;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-4f) {
                dx = i < n / 2f ? -1f : 1f;
                dy = -0.35f;
                len = (float) Math.sqrt(dx * dx + dy * dy);
            }
            e.flyDir[i] = dx / len;
            e.flyY[i] = dy / len;
        }
    }

    /** Desperation impacts launch every broken tile upward in a broad celebratory fan. */
    void computeUpwardFlyDirs(Enemy e, Layout L) {
        int n = e.word.length;
        e.flyDir = new float[n];
        e.flyY = new float[n];
        e.radialFly = true;
        for (int i = 0; i < n; i++) {
            float spread = n <= 1 ? 0f : (i / (float) (n - 1) - 0.5f) * 0.9f;
            e.flyDir[i] = spread;
            e.flyY[i] = -1f + 0.18f * Math.abs(spread);
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

    /**
     * The chord for the new collectible taking its place in the line, once per parade.
     *
     * Fired from the countdown rather than from {@link Parade}, which only draws: the join is a
     * moment in the parade's progress, so it is read off the same number the drawing is. Guarded by
     * a flag rather than by an edge test on the progress, because a parade that is interrupted and
     * restarted has to be able to say it again.
     */
    // ---- interlude ----------------------------------------------------------
    // Delegations; the between-stages round lives in Interlude.

    void tapBonus(int g) { if (!paused) Interlude.tapBonus(this, g); }
    /** Claims an armed steamer lid after an upward swipe over it. */
    void swipeBonus() { Interlude.swipeBonus(this); }

    void dragBonusLid(float lift) { Interlude.dragBonusLid(this, lift); }

    void holdBonusKey(int g, boolean down) { Interlude.holdKey(this, g, down); }

    /** Called on the way out of the interlude. */
    private void advanceStage() {
        enterStage(stage + 1);
    }

    /**
     * Begins stage {@code n}: the counters that are per-stage, the banner, the breather, and a boss
     * if that stage has one.
     *
     * Split out of {@link #advanceStage} so the settings panel's stage jump goes through exactly this
     * path rather than a second, slightly different one. A debug jump that sets up a stage a little
     * differently from the way play sets one up is a debug tool that hides the bug you are hunting.
     */
    private void enterStage(int n) {
        boolean hadBoss = boss.active();
        Lands.transition(this, Math.max(1, n));
        stage = Math.max(1, n);
        progress.enterStage(stage);
        if (fullRoster && stage >= 6 && earlyLosses != 0) {
            earlyLosses = 0; saveRoster();
        }
        // One per stage, and this is where a stage begins.
        pushUsed = false;
        spawnedThisStage = 0;
        resolvedThisStage = 0;
        stageBanner = BANNER_TIME;
        stageGap = STAGE_GAP;
        debuffLeft = monochromeFade = incognitoMorph = 0f;
        spawnTimer = 0.35f;
        // Every fifth stage is a boss instead of a wave. Started here rather than on the first frame
        // of play so its arrival card runs over the stage breather it already had.
        int bk = Boss.kindFor(stage);
        if (bk >= 0) boss.begin(bk, stage, rnd, playRosterFull());
        else boss.leave();
        if (sound != null && hadBoss != (bk >= 0)) sound.bossMusic(bk >= 0);
        cave.begin(this);
    }

    /**
     * Playtest hook: jumps straight to a stage, from the settings panel.
     *
     * Clears the field and every set piece on the way, because arriving at a stage is supposed to
     * mean arriving at a clean one — jumping out from under a live frenzy or a half-fought boss
     * leaves the thing you jumped away from still on the screen, which is the same shape of bug as
     * dying with one running.
     *
     * Deliberately does not touch the score, the lives or the run's haul. The point of the control is
     * to reach a stage and see it; a jump that also reset the run would make it useless for looking
     * at how a late stage plays with two lives left.
     */
    void jumpToStage(int n, Layout L) {
        if (!BuildFlags.DEVELOPER) return;
        if (state != PLAY) return;
        enemies.clear();
        shots.clear();
        particles.clear();
        target = null;
        caretOwner = null;
        pendingBonus = false;
        perfectBanner = 0f;
        // Both set pieces go. enterStage starts the boss for the stage being jumped to, so this has
        // to happen first or it would clear the one it just made.
        power = null;
        mode = -1;
        modeLeft = 0f;
        debuffLeft = monochromeFade = incognitoMorph = 0f;
        buddy.leave();
        boolean leftBoss = boss.active();
        boss.leave();
        if (leftBoss && sound != null) sound.bossMusic(false);
        fingerDown = false;
        touchDown = false;
        strokeFade = 0f;
        pushT = 0f;
        pushSlowT = 0f;
        chainT = 0f;
        slowdown = 0f;
        sliceCall = 0f;
        warnLevel = 0f;
        // A fresh stage has a fresh verdict: the interlude reads these, and carrying the old stage's
        // damage across would misreport a stage nobody played.
        missesThisStage = 0;
        hurtThisStage = 0;
        enterStage(n);
        if (sound != null) sound.frenzy(false);
    }

    private void resolveStageEnemy(Enemy e) {
        if(e.stageResolved) return;
        e.stageResolved=true;
        if(e.stageMate==null || e.stageMate.stageResolved) resolvedThisStage++;
    }

    private void breach(Enemy e, Layout L) {
        LinkedPairs.breached(this, e, L);
        if (target == e) target = null;
        resolveStageEnemy(e);
        takeHit(enemyCentreX(e), L);
    }

    void takeHit(float px, Layout L) {
        lives--;
        hurtThisStage++;
        combo = 0;
        shake = 1f;
        if (sound != null) sound.damage();
        flash = 1f;
        flashColor = FLASH_DAMAGE;
        Fx.explode(this, rnd, px, L.dangerY, L.enemyR * 2f, 16, 0xFFFF7A9E);
        if (lives <= 0) die();
    }

    /**
     * The last life is gone.
     *
     * Everything a set piece owns has to be sent home from here, and this is the reason: a death
     * never reaches the PLAY half of {@link #update}, so anything that is only ever cleared down
     * there stays live through the swirl, the summary and the title screen behind them. That has
     * bitten this file three times now — the stuck edge glow, the TEAM SQUISH squishy bouncing over
     * the game-over screen, and a lit blade left over a summary. The rule is: reset state above the
     * early returns, or clear it where the early return is taken. There is no third way.
     */
    private void die() {
        cave.leave();
        progress.finishRun(score, false);
        if (runFullRoster && fullRoster) {
            if (stage >= 6) earlyLosses = 0;
            else if (++earlyLosses >= 3) {
                fullRoster = false; earlyLosses = 0; rosterLeavePending = true;
            }
            saveRoster();
        }
        bossVictoryKind = boss.fighting() ? boss.kind : -1;
        bossVictory = bossVictoryKind >= 0 ? boss : null;
        if (bossVictoryKind >= 0 && sound != null) sound.bossTaunt(bossVictoryKind);
        state = OVER;
        time = 0;
        deathT = deathDuration();
        // The words are deliberately left standing: they swirl away over the death hold, and
        // the field is cleared when it ends, before the summary is drawn over it. Only the
        // shots go now — a kill landing after the run is over would credit a squish.
        shots.clear();
        target = null;
        // Dying mid-frenzy has to end the frenzy here: updatePower only runs during
        // PLAY, so otherwise the mode would stay live and the driven music would carry
        // on into the game-over screen.
        if (powerActive()) {
            mode = -1;
            modeLeft = 0f;
            debuffLeft = monochromeFade = incognitoMorph = 0f;
            fingerDown = false;
            if (sound != null) sound.frenzy(false);
        }
        // And the squishy goes with it.
        buddy.leave();
        // Gameplay gets a clean boss immediately. After a boss-won fight the old object survives
        // only as a frozen visual snapshot, so the victory sequence can retain its exact combat
        // silhouette, scale and attachments without allowing any mechanic to keep updating.
        if (bossVictory != null) boss = new Boss();
        else boss.leave();
        power = null;
        LandPicker.recordBest(this);
    }

    private boolean spawn(Layout L) {
        Enemy e = new Enemy();
        int len = minWordLen() + rnd.nextInt(maxWordLen() - minWordLen() + 1);
        Words.fill(e, len, stackChance(), rnd, playRosterFull());

        float half = L.wordWidth(len) / 2f;
        e.sway = Math.min(0.035f * L.w, Math.max(0f, (L.playRight - L.playLeft) / 2f - half - 4f));
        float lo = L.playLeft + half + e.sway;
        float hi = L.playRight - half - e.sway;
        e.baseX = hi > lo ? lo + rnd.nextFloat() * (hi - lo) : (L.playLeft + L.playRight) / 2f;
        e.phase = rnd.nextFloat() * 6.283f;
        // Mix top rain with quick inward arcs, then keep each side word in its landing lane.
        boolean refill = powerActive() && Power.spawnDelay(this, L)
                < spawnInterval() / Power.spawnRate(ramp());
        if (powerActive()
                && (rnd.nextBoolean() || (refill && rnd.nextFloat() < 0.75f)) && hi > lo) {
            e.sideEntry = true;
            boolean fromLeft = rnd.nextBoolean();
            e.pathStartX = fromLeft ? L.playLeft - half - L.enemyR
                    : L.playRight + half + L.enemyR;
            e.pathEndX = fromLeft ? lo + (hi - lo) * 0.25f : hi - (hi - lo) * 0.25f;
            e.pathStartY = L.playTop + (L.dangerY - L.playTop) *
                    (0.08f + rnd.nextFloat() * 0.24f);
            e.y = e.pathStartY;
            e.baseX = e.pathStartX;
            e.sway = 0f;
        } else {
            // Start fully above the top edge so words visibly fly in rather than popping
            // into existence. travelSeconds still measures spawn -> danger line.
            e.y = -L.enemyR * 2.2f;
        }
        e.enterT = 0f;
        e.speed = (L.dangerY - e.y) / travelSeconds();
        // Try other lanes before deferring. Existing side arcs also reserve space against
        // later top entries, so words cannot be admitted into a future collision.
        float preferredX = e.sideEntry ? e.pathEndX : e.baseX;
        for (int attempt = 0; attempt < 9; attempt++) {
            float lane = attempt == 0 ? preferredX : lo + (hi - lo) * (attempt - 1) / 7f;
            if (e.sideEntry) e.pathEndX = lane; else e.baseX = lane;
            if (EnemyEntry.clear(e, this, L)) {
                enemies.add(e);
                return true;
            }
        }
        return false;
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
