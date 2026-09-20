package com.dddumpling.game;

import java.util.Random;

final class Boss {

    /** Stages between bosses. Stage 5, 10, 15... */
    static final int EVERY = 5;

    /**
     * A soft-body slime. Keys whittle its chain down, and every hit sheds a glob that is worth
     * another hit again if it is <em>dragged</em> off the play area.
     *
     * The globs used to crawl back and <em>heal</em> it, which was the wrong shape of pressure: a
     * fight where the bar goes back up reads as being cheated rather than as being hard, and it
     * punished a player for having two thumbs and one spare finger. They are a bonus now, not a
     * tax — ignore them and the fight is simply slower.
     */
    static final int SLIME = 0;
    static final int SPLITTER = 1;
    static final int OCTOPUS = 2;
    static final int MUSHROOM = 3;
    static final int COUNT = 4;

    static final String[] NAMES = {"SLIME", "DARK DIVIDE", "OCTOPULSE", "FLY AGARIC"};
    /** One line each, in the mode bar. Held to the width of the longest frenzy blurb. */
    static final String[] BLURB = {"HIT THE MARK, DRAG GLOBS", "HIT THE MARK, THEN PINCH OUT", "BEAT THE REACH", "DRAG BACK AND FORTH"};
    /**
     * Which of the six characters each boss is a giant version of.
     *
     * Reusing {@link Kawaii} rather than drawing five new creatures is not only cheap: a boss that
     * is plainly an enormous one of the things you have been typing all game is funnier, and it
     * arrives already legible.
     */
    static final int[] FACE = {Kawaii.SQUISHY, Kawaii.SQUISHY, Kawaii.BLOB, Kawaii.DUMPLING};

    /** How long the arrival card holds the field before the fight starts. */
    static final float INTRO = 1.6f;
    /** Original burst length, retained as the readable basis of the extended victory sequence. */
    static final float LEAVE_BASE = 2.4f;
    /** How long the burst takes once it is beaten, before the stage may end. */
    static final float LEAVE = LEAVE_BASE * 1.5f;
    /** Boss instructions vanish promptly once they are no longer actionable. */
    static final float DEFEAT_PROMPT_FADE = 0.25f;

    /**
     * Seconds before it enrages.
     *
     * There used to be a retreat here: the encounter timed out, the boss left, and the stage carried
     * on without its reward. That was a safety net for exactly one thing — a player who cannot work
     * a boss out never hitting a wall — and it was removed deliberately, so a boss now has to be
     * beaten to advance. Enrage is now visual urgency only; it does not damage the player.
     */
    static final float ENRAGE_AT = 26f;
    static final float ENRAGE_RAMP = 8f;

    private static final float[] CYCLE = {5.0f, 1f, 1f, 1f};
    private static final float[] SHOW = {4.0f, 1f, 1f, 1f};

    private static final float[] HP = {4f, 8f, 8f, 3f};
    /**
     * Extra health per later visit, capped by {@link #TOUGH_MAX}. A boss met at stage 30 should be
     * more than the same boss at stage 5 — but the cap matters far more than the slope now that
     * there is no retreat: health that outgrows what the windows can deliver is not difficulty, it
     * is a stage nobody leaves. Two, on a base of three to eight, is a boss that is noticeably
     * tougher the second time and still inside the fifteen seconds above.
     */
    private static final float TOUGH = 1f;
    private static final int TOUGH_MAX = 2;

    /** What a press, tap or drag did, for {@link GameCore} to score and sound. */
    static final int NONE = 0;
    /** Damage landed. */
    static final int HIT = 1;

    static final int PART = 2;
    /** The boss took the input and refused it: right thing, wrong moment, or a held key. */
    static final int REBUFF = 3;
    /** A bolt the boss threw was swatted out of the air. Scores; does not hurt the boss. */
    static final int PARRY = 4;
    /** The slime chain completed and tore a glob free. */
    static final int SPLIT = 5;
    /** An Octopulse reach was answered with the wrong key and lashed the player. */
    static final int PLAYER_HIT = 6;

    // ---- elements -----------------------------------------------------------
    /** Most touch elements one boss puts on the field at once. */
    static final int ELEMS = 3;
    /** No element in this slot. */
    static final int E_OFF = 0;
    /** A {@link #SLIME} glob: drag it off the play area or it crawls back. */
    static final int E_GLOB = 1;

    /** Element type per slot, {@link #E_OFF} when empty. */
    final int[] etype = new int[ELEMS];
    /** Element centre and radius in view pixels, recomputed every frame by {@link #update}. */
    final float[] ex = new float[ELEMS];
    final float[] ey = new float[ELEMS];
    final float[] er = new float[ELEMS];

    final float[] elife = new float[ELEMS];
    /** Which element a finger is currently holding, or -1. */
    int held = -1;
    /** Edge-started glob drags must visit the safe interior before an edge crossing can damage. */
    boolean globDragStarted, globDragCanDamage;

    static final float GLOB_TIME = 5f;
    /**
     * Presses of {@link #SLIME}'s chain that split one glob off it.
     *
     * The chain used to shed a glob per press, which made the press the interesting half and the drag
     * a chore repeated every second or so — five globs in the air at once with three slots to hold
     * them. Five presses to work one loose makes the chain something to run and the drag the payoff at
     * the end of it, and it is the only thing that scores: see {@link #press}.
     */
    static final int SPLIT_HITS = 5;
    /** Deadline for the slime's shown letter, from full to empty health. */
    static final float PROMPT_MAX = 2f, PROMPT_MIN = 1f;
    /** Visible recoil left after launching a volley. */
    static final float LAUNCH_TIME = 0.8f;
    /**
     * How hard a dragged glob stretches the skin it is being hauled out of, and how hard the skin
     * snaps back when it comes free.
     *
     * The tug is well short of full strength on purpose: the skin should trail the glob and lose,
     * which is what makes the glob feel like it is being torn out rather than towed. The snap is the
     * emphatic one — it is the payoff frame of the whole mechanic.
     */
    private static final float PULL_K = 0.85f, SNAP_BACK = 0.9f;
    /** How wide the forgiving damage strip is, in glob radii. */
    private static final float GLOB_EDGE = 2f;
    /** Invisible pickup radius around the body-path patch, in logical glob radii. */
    static final float GLOB_TOUCH = 3.3f;
    /** How quickly a released glob catches the slime as its body walks home. */
    private static final float GLOB_HOME = 8f;
    /** Resting wart centre and skin reach beyond the plain silhouette, in glob radii. */
    private static final float WART_OUT = 0.15f, WART_SKIN = 0.65f, WART_PULL = 0.35f;
    /**
     * What one landed press does to the body.
     *
     * Up from 0.3, which was a polite little dent on a boss being hit once every few seconds. The
     * body is the health bar as far as the eye is concerned, so a hit wants to be felt in it.
     */
    private static final float HIT_PUNCH = 0.65f;
    /** The breather with a whole deck that fetching a key back buys, before another is taken. */
    static final float STEAL_GAP = 2.5f;

    /** -1 when there is no boss. Also the index into every table above. */
    int kind = -1;
    boolean rosterFull = true;
    float hp, hpMax;
    /** Seconds the fight has been running, and of the arrival card and the exit. */
    float age, intro, leaveT;
    int defeatBeat;
    boolean defeatChime;
    private float defeatStartW;
    /** Counts up through the open/shut cycle, wrapping at {@link #CYCLE}. */
    float phase;
    /** Decaying flashes: hurt on damage, rage on a rebuff. Separate channels, on purpose. */
    float hurt, rage;
    float slimeDragPulse, slimeKeyPulse, slimeKeyLock;
    final float[] slimeBlobPulse = new float[Glyph.COUNT];
    boolean beaten;
    /** A damaged slime stays open while returning home, then answers with a three-bolt volley. */
    boolean slimeRetaliating;
    boolean slimeCoverLearned;
    int slimePromptHits;
    /** Set by swat() for the press frame so audio can distinguish a hit from a destroyed bolt. */
    boolean boltDestroyed;

    static final int MUSHROOM_SHAKES = 6;
    static final float MUSHROOM_SHAKE_WIDTH = 0.35f;
    /** Three times the old endpoint tolerance, giving fast reversals a readable grace window. */
    static final float MUSHROOM_GUIDE_WINDOW = 0.18f;
    static final float MUSHROOM_ATTACK_GAP = 5f, MUSHROOM_CHARGE_TIME = 0.72f,
            MUSHROOM_ANGER_TIME = 1.60f, MUSHROOM_REACTION_RELEASE = 0.65f;
    static final int MUSHROOM_DUST = 64;
    final float[] mushroomDustX = new float[MUSHROOM_DUST], mushroomDustY = new float[MUSHROOM_DUST],
            mushroomDustVX = new float[MUSHROOM_DUST], mushroomDustVY = new float[MUSHROOM_DUST],
            mushroomDustLife = new float[MUSHROOM_DUST];
    int mushroomDustNext;
    float mushroomDustTravel;
    int mushroomShakes, mushroomDirection;
    float mushroomLastX, mushroomShakeWindow, mushroomAttackT, mushroomCharge, mushroomAngry;
    float mushroomSweepFlash;
    float mushroomMeterAlpha, mushroomGuideX, mushroomPlayerX, mushroomReject;
    int mushroomGuideTarget;
    /** Visual offset of the separately dragged cap; it springs home after release. */
    float mushroomCapDX, mushroomCapDY;
    boolean mushroomReaction;
    /** One-frame event consumed by GameCore audio after an accepted shake endpoint. */
    boolean mushroomShakeCue, mushroomSporeCue;

    /**
     * The chain {@link #SLIME} wants, in order. Indexed by {@link #chainAt}, wrapping, so its length
     * is how long the pattern is rather than a bound on how long the fight can run.
     */
    private final int[] chain = new int[32];
    /**
     * Presses of {@link #SLIME}'s chain that have landed: where in the chain it is, and how far
     * through working the current glob loose.
     *
     * Counted rather than derived from health, which is what it used to be. Health no longer moves on
     * a press at all — the only thing that hurts a slime is a glob carried off the screen — so a chain
     * indexed by damage taken would have sat on the same letter for five presses running.
     */
    int chainAt, split;
    float promptT, launchT;
    boolean launched;

    /** Stage-10 split slime: charge, pinch state, per-body prompts and neglect clocks. */
    static final int DIVIDE_HITS = 6, DIVIDE_LEVELS = 3, DIVIDE_PIECES = 1 << DIVIDE_LEVELS;
    static final int DIVIDE_NODES = (DIVIDE_PIECES << 1) - 1;
    static final float DIVIDE_SCALE = 1.55f, DIVIDE_BOLT_TIME = 3f;
    int divideHits, divideLevel, divideAlive, divideSplits;
    boolean divided;
    boolean divideDeactivated;
    final int[] halfWant = new int[DIVIDE_NODES];
    final float[] halfIdle = new float[DIVIDE_NODES];
    /** Short, visual-only memories: the split flare, and which body was just struck. */
    final float[] halfHurt = new float[DIVIDE_NODES];
    final int[] pieceHits = new int[DIVIDE_NODES];
    final float[] divideX = new float[DIVIDE_NODES], divideY = new float[DIVIDE_NODES];
    final float[] divideVX = new float[DIVIDE_NODES], divideVY = new float[DIVIDE_NODES];
    final Softbody[] divideBody = new Softbody[DIVIDE_NODES];
    int divideActive, divideDead, pinchNode = -1;
    boolean dividePlaced;
    float boingWeight = -1f;
    float pinchX1, pinchY1, pinchX2, pinchY2;
    float pinchStart, divideBurst;

    static final int OCTO_ARMS = 8, OCTO_NODES = 7;
    final float[][] octoX = new float[OCTO_ARMS][OCTO_NODES];
    final float[][] octoY = new float[OCTO_ARMS][OCTO_NODES];
    final float[][] octoVX = new float[OCTO_ARMS][OCTO_NODES];
    final float[][] octoVY = new float[OCTO_ARMS][OCTO_NODES];
    int octoArms, octoTarget = -1, octoAttackArm = -1, octoCaptured = -1, disabledKeys;
    int octoLashArm = -1, octoDyingArm = -1, octoVulnerableArm = -1, octoEscapeArm = -1;
    int octoFlurryLeft;
    int octoThrowsLeft, octoThrowArm = -1, octoThrowGlyph = -1;
    float octoThrowT;
    boolean octoThrowReleased;
    float octoReach, octoReturn, octoPause, octoLash, octoDeath;
    float octoSweep, octoCharge, octoCoil, octoDragX, octoDragY, octoTaunt, octoEat, octoLean;
    float octoDragTime, octoEscape, octoFlurryT;
    float octoLashX, octoLashY;
    boolean octoPlaced, octoWave, octoCue, octoLock, octoImpact, octoPlayerHit, octoLashLanded;
    boolean octoWrongLash, octoDragStarted, octoDragCanDamage;

    /** Stage 5 teaches boss play; stage 10 adds the first two-finger fight. */
    static int kindFor(int stage) {
        if (stage == EVERY) return SLIME;
        if (stage == EVERY * 2) return SPLITTER;
        if (stage == EVERY * 3) return OCTOPUS;
        if (stage == EVERY * 4) return MUSHROOM;
        return -1;
    }

    static boolean isBossStage(int stage) {
        return kindFor(stage) >= 0;
    }

    /** True from the arrival card until the exit animation has finished. */
    boolean active() {
        return kind >= 0;
    }

    /** True while the fight is actually running: past the card, and not yet beaten. */
    boolean fighting() {
        return kind >= 0 && intro <= 0f && !beaten;
    }

    /** True once the exit is spent and the stage may end. */
    boolean gone() {
        return kind >= 0 && beaten && leaveT <= 0f;
    }

    /** 0..1 health remaining, for the bar. */
    float health() {
        return hpMax <= 0f ? 0f : hp / hpMax;
    }

    float promptDelay() {
        if (kind != SLIME) return PROMPT_MAX;
        return PROMPT_MIN + (PROMPT_MAX - PROMPT_MIN) * health();
    }

    float promptProgress() {
        float d = promptDelay();
        return kind != SLIME || d <= 0f ? 0f : Math.max(0f, Math.min(1f, 1f - promptT / d));
    }

    int boltHits() {
        int n = 1 + (int) ((1f - health()) * 3f);
        return n > 3 ? 3 : n;
    }

    static float enrageAt(int kind) {
        // Eleven authored throws add recovery time to the eight-arm fight.
        return kind == OCTOPUS ? 40f : ENRAGE_AT;
    }

    /** 0..1 of the visual enrage warning after this fight's time allowance. */
    float enrage() {
        if (!fighting() || age <= enrageAt(kind)) return 0f;
        float t = (age - enrageAt(kind)) / ENRAGE_RAMP;
        return t > 1f ? 1f : t;
    }

    float hitX, hitY;

    /** 0..1 through the arrival card. */
    float introProgress() {
        return intro <= 0f ? 1f : 1f - intro / INTRO;
    }

    /** 0..1 through the exit. */
    float leaveProgress() {
        return leaveT <= 0f ? 1f : 1f - leaveT / LEAVE;
    }

    /** 1 at the killing blow, reaching 0 after the short prompt-dismissal interval. */
    float defeatPromptFade() {
        if (!beaten) return 1f;
        return Math.max(0f, 1f - leaveProgress() * LEAVE / DEFEAT_PROMPT_FADE);
    }

    String name() {
        return kind < 0 ? "" : NAMES[kind];
    }

    void begin(int which, int stage, Random rnd) { begin(which, stage, rnd, true); }

    void begin(int which, int stage, Random rnd, boolean fullRoster) {
        rosterFull = fullRoster;
        kind = which;
        int visit = Math.max(0, Math.min(TOUGH_MAX, stage / EVERY - 1));
        hpMax = HP[which] + (which == OCTOPUS ? 0f : TOUGH * visit);
        hp = hpMax;
        age = 0f;
        intro = INTRO;
        leaveT = 0f;
        // Starts shut, so the first thing a boss does is arrive rather than be vulnerable. The
        // opening breather is also where the blurb gets read.
        phase = 0f;
        hurt = rage = slimeDragPulse = slimeKeyPulse = slimeKeyLock = 0f;
        for (int g = 0; g < slimeBlobPulse.length; g++) slimeBlobPulse[g] = 0f;
        beaten = false;
        slimeRetaliating = boltDestroyed = false;
        slimeCoverLearned = false;
        slimePromptHits = 0;
        mushroomShakes = mushroomDirection = 0;
        mushroomLastX = mushroomShakeWindow = mushroomCharge = mushroomAngry = mushroomSweepFlash = 0f;
        mushroomMeterAlpha = mushroomGuideX = mushroomPlayerX = mushroomReject = 0f;
        mushroomGuideTarget = 1;
        mushroomCapDX = mushroomCapDY = 0f;
        mushroomDustNext = 0;
        mushroomDustTravel = 0f;
        java.util.Arrays.fill(mushroomDustLife, 0f);
        mushroomAttackT = MUSHROOM_ATTACK_GAP;
        mushroomReaction = mushroomShakeCue = mushroomSporeCue = false;

        held = -1;
        globDragStarted = globDragCanDamage = false;

        chainAt = 0;
        split = 0;
        promptT = PROMPT_MAX;
        launchT = 0f;
        launched = false;
        divideHits = divideLevel = divideSplits = 0;
        divideAlive = divideActive = 1;
        divideDead = 0;
        divided = dividePlaced = divideDeactivated = false;
        pinchNode = -1;
        pinchX1 = pinchY1 = pinchX2 = pinchY2 = Float.NaN;
        boingWeight = -1f;
        pinchStart = divideBurst = 0f;
        octoArms = (1 << OCTO_ARMS) - 1;
        octoTarget = octoAttackArm = octoCaptured = -1;
        octoLashArm = octoDyingArm = octoVulnerableArm = octoEscapeArm = -1;
        disabledKeys = 0;
        octoReach = -1f; octoReturn = octoLash = octoDeath = 0f; octoPause = 0.75f;
        octoSweep = octoCharge = octoCoil = octoDragX = octoDragY = octoTaunt = octoEat = octoLean = 0f;
        octoDragTime = octoEscape = octoFlurryT = 0f; octoFlurryLeft = 0;
        OctoThrow.reset(this);
        octoPlaced = octoWave = octoCue = octoLock = octoImpact = octoPlayerHit = octoLashLanded = octoWrongLash = false;
        octoDragStarted = octoDragCanDamage = false;
        resetDividePieces(rnd);
        if (which != SPLITTER) { randomGlyph(rnd); randomGlyph(rnd); }
        followX = followY = 0f;
        clearBolts();
        // Seeded off the kind, so the six bosses do not all breathe on the same phase. Placed on
        // the first update, which is the first time there is a Layout to place it in.
        body = new Softbody(Softbody.NODES, which + 1);
        mushroomStem = which == MUSHROOM ? new Softbody(Softbody.NODES, 91) : null;
        mushroomStemPlaced = false;
        bodyPlaced = false;
        for (int i = 0; i < ELEMS; i++) {
            etype[i] = E_OFF;
            elife[i] = 0f;
        }
        for (int i = 0; i < chain.length; i++) chain[i] = randomGlyph(rnd);

    }

    private int randomGlyph(Random rnd) { return Roster.random(rosterFull, rnd); }
    private int randomExcept(int avoid, Random rnd) {
        return Roster.randomExcept(rosterFull, avoid, rnd);
    }

    /**
     * Sends it away and takes everything it put on the field with it.
     *
     * Called from two places, and the second is the one that matters: the fight ending, and the
     * player dying. A death never reaches the PLAY half of {@link GameCore#update}, so anything a
     * boss owns has to be cleared where the death happens or it stays on screen over the swirl, the
     * summary and the title screen behind them — which is exactly what the TEAM SQUISH squishy did.
     * Every field a boss writes is reset here, not just the ones that happen to be drawn today.
     */
    void leave() {
        kind = -1;
        hp = hpMax = 0f;
        age = intro = leaveT = 0f;
        phase = 0f;
        hurt = rage = slimeDragPulse = slimeKeyPulse = slimeKeyLock = 0f;
        for (int g = 0; g < slimeBlobPulse.length; g++) slimeBlobPulse[g] = 0f;
        beaten = false;
        slimeRetaliating = boltDestroyed = false;
        slimeCoverLearned = false;
        slimePromptHits = 0;
        mushroomShakes = mushroomDirection = 0;
        mushroomLastX = mushroomShakeWindow = mushroomAttackT = mushroomCharge = mushroomAngry = mushroomSweepFlash = 0f;
        mushroomMeterAlpha = mushroomGuideX = mushroomPlayerX = mushroomReject = 0f;
        mushroomGuideTarget = 1;
        mushroomCapDX = mushroomCapDY = 0f;
        mushroomDustNext = 0;
        mushroomDustTravel = 0f;
        java.util.Arrays.fill(mushroomDustLife, 0f);
        mushroomReaction = mushroomShakeCue = mushroomSporeCue = false;

        held = -1;
        globDragStarted = globDragCanDamage = false;

        chainAt = 0;
        split = 0;
        promptT = PROMPT_MAX;
        launchT = 0f;
        launched = false;
        divideHits = divideLevel = divideAlive = divideActive = divideDead = divideSplits = 0;
        divided = dividePlaced = divideDeactivated = false;
        pinchNode = -1;
        pinchX1 = pinchY1 = pinchX2 = pinchY2 = Float.NaN;
        boingWeight = -1f;
        pinchStart = divideBurst = 0f;
        octoArms = disabledKeys = 0; octoTarget = octoAttackArm = octoCaptured = -1;
        octoLashArm = octoDyingArm = octoVulnerableArm = octoEscapeArm = -1;
        octoReach = -1f; octoReturn = octoPause = octoLash = octoDeath = 0f;
        octoSweep = octoCharge = octoCoil = octoDragX = octoDragY = octoTaunt = octoEat = octoLean = 0f;
        octoDragTime = octoEscape = octoFlurryT = 0f; octoFlurryLeft = 0;
        OctoThrow.reset(this);
        octoPlaced = octoWave = octoCue = octoLock = octoImpact = octoPlayerHit = octoLashLanded = octoWrongLash = false;
        octoDragStarted = octoDragCanDamage = false;
        for (int i = 0; i < DIVIDE_NODES; i++) {
            halfWant[i] = -1;
            halfIdle[i] = halfHurt[i] = 0f;
            pieceHits[i] = 0;
            divideBody[i] = null;
            divideX[i] = divideY[i] = divideVX[i] = divideVY[i] = 0f;
        }
        followX = followY = 0f;
        clearBolts();
        // The body goes too. It is the largest thing a boss puts on the screen, and the renderer
        // reads exactly this to decide whether there is anything to draw at all.
        body = null;
        mushroomStem = null;
        mushroomStemPlaced = false;
        bodyPlaced = false;
        for (int i = 0; i < ELEMS; i++) {
            etype[i] = E_OFF;
            elife[i] = 0f;
            ex[i] = ey[i] = er[i] = 0f;
        }
    }

    // ---- geometry -----------------------------------------------------------

    /**
     * Where the body sits and how big it is. One copy, because the hit-test, the drawing and the
     * element layout all have to agree — the star course learnt that the hard way with its flyer.
     */
    static float bodyR(Layout L) {
        return L.enemyR * 2.5f;
    }

    /**
     * How much wider than tall each boss is at rest.
     *
     * Only the slime, and it is its whole silhouette: a wide low mass of goo sprawled across the top
     * of the field rather than another ball. The vertical radius is {@link #bodyR} for every boss,
     * which is what keeps the header column above it — see {@link #BODY_DROP} — a single derivation
     * instead of one per boss.
     */
    private static final float[] WIDE = {2f, 1.65f, 1.35f, 1.45f};

    /**
     * How springy each boss is; see {@link Softbody#jiggle}.
     *
     * The slime is twice everything: a hit dents it twice as deep, it breathes twice as far, and what
     * is set going in it rings for twice as long. It can afford that where the others could not,
     * because its own mechanic no longer hits it every second — five presses work a glob loose and
     * only the drag scores, so the body has time to actually finish a wobble.
     */
    private static final float[] JIGGLE = {2f, 1.7f, 2.2f, 1.9f};

    /** Rest width over rest height for this boss. */
    float wide() {
        return kind < 0 ? 1f : WIDE[kind];
    }

    /** Half the body's resting width. */
    float bodyW(Layout L) {
        return bodyR(L) * wide();
    }

    /**
     * Body centre x, before the drag follow. Drifts, so it is plainly alive and so its elements are
     * not always in the same place.
     *
     * Driven by {@link #age} rather than by {@code GameCore.clock}, and an instance method rather
     * than a static one taking a clock, so that there is exactly one answer to where the body is.
     * A renderer passing a different clock than the layout pass used would put every element a
     * little to one side of the thing it belongs to — and it would only show up as taps missing.
     *
     * The amplitude is what fits rather than a fixed fraction: a body twice as wide as the others has
     * half as much room to wander in, and a slime that drifted the same distance would put a third of
     * itself off the side of the play area. The {@code min} means nothing changed for the four that
     * are round, which have room to spare.
     */
    float baseX(Layout L) {
        float span = L.playRight - L.playLeft;
        float room = Math.max(0f, span * 0.5f - bodyW(L));
        float amp = Math.min(span * 0.16f, room);
        return L.w * 0.5f + (float) Math.sin(age * 0.55f) * amp;
    }

    /** Body centre x, drag follow included. What everything reads. */
    float bodyX(Layout L) {
        return baseX(L) + followX;
    }

    /**
     * How far below {@link Layout#playTop} the body sits, in text units.
     *
     * Derived, not chosen. Above the body sits a column of four things — the health bar, the name,
     * the blurb, and the wanted-letter badge the body holds over itself — and the badge is pinned to
     * the body, so this offset is what decides whether the badge lands on the blurb. Working it
     * through {@code BossScreen}'s header geometry puts the floor at 6.04 units, and both sides of
     * that inequality scale with the view width (a body radius is 3.1 units at every size), so the
     * margin here holds at every screen size rather than at the one it was looked at. There is an
     * assertion on it across a sweep; the first pass used 3.4 and the blurb sat on the badge.
     */
    private static final float BODY_DROP = 6.5f;

    float bodyY(Layout L) {
        if (beaten) return defeatY(L);
        return baseY(L) + followY;
    }

    /** 0..1 through the dramatic widening that precedes the fall. */
    float defeatStretch() {
        float t = leaveProgress() / 0.38f;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return 1f - (float) Math.pow(1f - t, 3);
    }

    float deathImpactTime() {
        return kind == MUSHROOM ? MushroomDeath.FLATTEN_END
                : kind == SPLITTER ? DivideDeath.BURST_AT : LEAVE * (kind == OCTOPUS ? .70f : .38f);
    }

    /** Slow at first and continuously accelerating until it clears the bottom. */
    float defeatMelt() {
        float start = deathImpactTime() / LEAVE;
        float t = (leaveProgress() - start) / (1f - start);
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t * t * t;
    }

    /** Shared defeated-boss route: three soft bounces, then melt toward the player. */
    float defeatY(Layout L) {
        if (kind == MUSHROOM) return baseY(L) + followY;
        float t = leaveProgress();
        float bounceT = Math.min(1f, t / 0.38f);
        float bounce = (float) Math.pow(Math.sin(bounceT * Math.PI * 3f), 2)
                * bodyR(L) * 0.05f * (1f - bounceT);
        float melt = defeatMelt();
        float from = baseY(L) + followY;
        return from + bounce + (L.h + bodyR(L) * 2.2f - from) * melt;
    }

    /** Body centre y, before the drag follow. */
    float baseY(Layout L) {
        float top = L.playTop + L.unit * BODY_DROP + bodyR(L);
        return top;

    }

    /**
     * How far the body has been walked toward a glob being dragged out of it, and how far it will go.
     *
     * The stretch alone cannot keep a glob inside the body all the way to the edge of the screen —
     * that is a spike two-thirds of the way across the field, not a creature. So the two share the
     * work: the body comes a quarter of the way to the glob for free, and further than that only as
     * much as it has to for the stretch to still reach, which is what {@link #DRAG_REACH} bounds.
     * Dragged to the far wall, the whole slime is hauled along after its own glob and squashed against
     * it — which is a better read of what the mechanic is than a boss that sits still while a piece of
     * it is stolen.
     *
     * Capped short of 1, or the body would simply follow the finger and the drag would be a
     * repositioning rather than a tug of war.
     */
    private float followX, followY;
    private static final float DRAG_FOLLOW = 0.25f, DRAG_REACH = 1.55f, DRAG_FOLLOW_MAX = 0.8f;
    /** How fast the follow decays once nothing is held. Roughly a sixth of a second. */
    private static final float FOLLOW_HOME = 6f;

    /**
     * Where the body has to be for the skin to still be wrapped around whatever is held, worked out
     * before {@link #layoutElems} so that everything reading {@link #bodyX} this frame — the renderer,
     * the hit-test, the other elements — agrees with the physics.
     */
    private void updateFollow(float dt, Layout L) {
        if (held < 0 || etype[held] != E_GLOB) {
            // Walks home rather than snapping back to it. The body is carried by
            // {@code Softbody.moveTo}, which is a rigid translation, so a follow that went straight
            // to zero would teleport the whole slime back to its drift and the rebound would be over
            // before a frame of it was drawn. Decayed, the walk home and the skin's own spring back
            // are one motion — which is the whole read of the glob coming free.
            float k = 1f - dt * FOLLOW_HOME;
            if (k < 0f) k = 0f;
            followX *= k;
            followY *= k;
            return;
        }
        float bx = baseX(L), by = baseY(L);
        float dx = ex[held] - bx, dy = ey[held] - by;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float f = DRAG_FOLLOW;
        if (d > 1e-3f) {
            // What is left over once the body has walked f of the way must be inside the tongue's
            // reach, so f is at least 1 - reach/d. Below that distance the quarter covers it.
            //
            // The reach is measured along the drag through the body's own rest shape, not in units of
            // its height. A slime is twice as wide as it is tall, so a bound in heights let go of a
            // sideways drag while the glob was still deep inside the goo — the body walked the whole
            // way and there was nothing left for the skin to do, which is the one thing this was
            // built to show.
            float reach = DRAG_REACH * (body != null ? body.restToward(dx, dy) : bodyR(L));
            float need = 1f - reach / d;
            if (need > f) f = need;
            if (f > DRAG_FOLLOW_MAX) f = DRAG_FOLLOW_MAX;
        }
        followX = dx * f;
        followY = dy * f;
    }

    /** Pulls released globs back into the attachment point on the body as it walks home. */
    private void updateReturning(float dt, Layout L) {
        float bx = bodyX(L), by = bodyY(L), br = bodyR(L);
        float k = Math.min(1f, dt * GLOB_HOME);
        for (int i = 0; i < ELEMS; i++) {
            if (!returning[i] || etype[i] != E_GLOB) continue;
            float tx = bx + globSide[i] * (br * wide() + er[i] * WART_OUT);
            float ty = by + br * (globLift[i] - 0.6f) * 0.5f;
            ex[i] += (tx - ex[i]) * k;
            ey[i] += (ty - ey[i]) * k;
            float dx = tx - ex[i], dy = ty - ey[i];
            if (dx * dx + dy * dy < br * br * 0.0009f) {
                ex[i] = tx;
                ey[i] = ty;
                moved[i] = false;
                returning[i] = false;
            }
        }
    }

    /** The body's resting height, for anything that has to lay out around it. */
    static float restY(Layout L) {
        return L.playTop + L.unit * BODY_DROP + bodyR(L);
    }

    // ---- the window ---------------------------------------------------------

    boolean open() {
        if (!fighting()) return false;
        if (kind == SLIME && (!slimeCoverLearned || slimeRetaliating)) return true;
        if (kind == SPLITTER) return true;

        return phase >= CYCLE[kind] - SHOW[kind];
    }

    static final float SLIME_PROMPT_TRANSITION = 0.30f;

    /** The same curve runs backward on release; hit eligibility starts at open(), not its end. */
    float slimePromptCover() {
        if (kind != SLIME || !slimeCoverLearned || !fighting() || hasGlob() || boltCount() > 0 || slimeRetaliating) return 0f;
        if (!open()) return 1f;
        float sinceOpen = phase-(CYCLE[kind]-SHOW[kind]);
        float release = Math.max(0f,1f-sinceOpen/SLIME_PROMPT_TRANSITION);
        float gather = Math.max(0f,1f-(CYCLE[kind]-phase)/SLIME_PROMPT_TRANSITION);
        float t = Math.min(1f,Math.max(release,gather));
        return t*t*(3f-2f*t);
    }

    /** Bubble trails finish just after the skirt closes, before the release starts. */
    float slimeCoverBubbleTime() {
        if(slimePromptCover()<=0f) return -1f;
        float start=CYCLE[SLIME]-SLIME_PROMPT_TRANSITION;
        if(phase>=start) return phase-start;
        return phase<0.35f ? phase+SLIME_PROMPT_TRANSITION : -1f;
    }

    /** 0..1 through the current window, or through the breather when it is shut. */
    float phaseProgress() {
        if (kind < 0) return 0f;
        float show = SHOW[kind], cycle = CYCLE[kind];
        if (phase >= cycle - show) return show <= 0f ? 1f : (phase - (cycle - show)) / show;
        return cycle - show <= 0f ? 1f : phase / (cycle - show);
    }

    boolean wants(int g) {
        // Whether or not the window is open: a bolt is in the air and the press that clears it has
        // to be advertised, or the key hint tells the player to ignore the only threat on the field.
        if (boltWants(g)) return true;
        if (!open()) return false;

        return asksFor(g);
    }

    /** The next letter of {@link #SLIME}'s chain. */
    int chainLetter() {
        if (kind != SLIME) return -1;
        return chain[((chainAt % chain.length) + chain.length) % chain.length];
    }

    /** 0..1 of the way to working the next glob loose off {@link #SLIME}. */
    float splitProgress() {
        return kind != SLIME ? 0f : (float) split / SPLIT_HITS;
    }

    /** True when a shove would land right now, for the renderer's hint and the view's gesture. */

    /** First awake, unstruck head showing {@code g}, or -1. */

    boolean claims(int g) {
        return boltWants(g) || asksFor(g);
    }

    private boolean asksFor(int g) {
        switch (kind) {
            case SLIME: return g == chainLetter();
            case OCTOPUS: return octoReach >= 0f && g == octoTarget;

            case SPLITTER:
                return dividePieceFor(g) >= 0;
            default: return false;
        }
    }

    int pieceCount() {
        return Integer.bitCount(divideActive);
    }

    private int pieceNode(int ordinal) {
        for (int n = 0; n < DIVIDE_NODES; n++) {
            if ((divideActive & (1 << n)) == 0) continue;
            if (ordinal-- == 0) return n;
        }
        return -1;
    }

    boolean pieceAlive(int ordinal) { return pieceNode(ordinal) >= 0; }
    int pieceWant(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? -1 : halfWant[n]; }
    int pieceCharge(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? 0 : pieceHits[n]; }
    int pieceDepth(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? -1 : nodeDepth(n); }
    int pieceNodeIndex(int ordinal) { return pieceNode(ordinal); }
    int vulnerablePiece() {
        for (int i = 0; i < pieceCount(); i++)
            if (pieceCharge(i) >= DIVIDE_HITS) return i;
        return -1;
    }
    float pieceHurt(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? 0f : halfHurt[n]; }
    float pieceIdle(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? 0f : halfIdle[n]; }
    Softbody pieceBody(int ordinal) { int n = pieceNode(ordinal); return n < 0 ? null : divideBody[n]; }

    boolean nodeActive(int n) {
        return n >= 0 && n < DIVIDE_NODES && (divideActive & (1 << n)) != 0;
    }

    boolean nodeVisible(int n) {
        return n >= 0 && n < DIVIDE_NODES && ((divideActive | divideDead) & (1 << n)) != 0;
    }

    int nodeDepth(int n) {
        int d = 0;
        while (n > 0) { n = (n - 1) >> 1; d++; }
        return d;
    }

    private int dividePieceFor(int g) {
        int best = -1;
        for (int n = 0; n < DIVIDE_NODES; n++) {
            if (nodeActive(n) && pieceHits[n] < DIVIDE_HITS
                    && halfWant[n] == g
                    && (best < 0 || halfIdle[n] > halfIdle[best])) best = n;
        }
        return best;
    }

    private void rerollPiece(int node, Random rnd) {
        int next = randomGlyph(rnd);
        for (int guard = 0; guard < Glyph.COUNT; guard++) {
            boolean used = false;
            for (int n = 0; n < DIVIDE_NODES; n++)
                if (n != node && nodeActive(n) && halfWant[n] == next) used = true;
            if (!used) break;
            do { next = (next + 1) % Glyph.COUNT; } while (!Roster.active(rosterFull, next));
        }
        halfWant[node] = next;
    }

    private void resetDividePieces(Random rnd) {
        divideActive = divideAlive = 1;
        divideDead = 0;
        for (int i = 0; i < DIVIDE_NODES; i++) {
            halfWant[i] = -1;
            halfIdle[i] = halfHurt[i] = 0f;
            pieceHits[i] = 0;
            divideBody[i] = null;
            divideX[i] = divideY[i] = divideVX[i] = divideVY[i] = 0f;
        }
        rerollPiece(0, rnd);
    }

    // ---- input --------------------------------------------------------------

    /**
     * A press offered to the boss. Returns {@link #NONE}, {@link #HIT}, {@link #PART} or
     * {@link #REBUFF}; the caller turns that into score, sound and accuracy.
     *
     * Every press in play comes through here first, so the contract that matters is the negative
     * one: anything the boss has no claim on must return {@link #NONE} and fall through to the words
     * underneath it. A boss claims exactly two things — a key it is holding, and its own letter —
     * and it claims that letter whether or not the window is open, because refusing it early is how
     * the beat teaches itself. Everything else is somebody else's press.
     */
    int press(int g, Random rnd, Layout L) {
        // Ahead of everything else: a bolt is the only thing on a boss stage that costs a life, so
        // the press that clears one is never spent on the chain instead. Above the fighting() guard
        // too — a volley outlives the frame the boss is beaten on.
        if (kind >= 0 && L != null) {
            int swatted = swat(g, L);
            if (swatted != NONE) return swatted;
        }
        if (!fighting()) return NONE;
        if (kind == SLIME && (!open() || !asksFor(g))) {
            slimeKeyLock = 1f;
            rage = 1f;
            if (body != null) {
                hitX = body.centreX();
                hitY = body.centreY();
                body.squash(-0.42f);
            }
            return REBUFF;
        }
        if (kind == OCTOPUS && octoTarget >= 0 && octoReach >= 0f && g != octoTarget) {
            int arm = octoAttackArm;
            if (arm < 0) return NONE;
            hitX = octoX[arm][OCTO_NODES - 1];
            hitY = octoY[arm][OCTO_NODES - 1];
            octoLashX = Roster.keyX(L, g, rosterFull ? 1f : 0f);
            octoLashY = Roster.keyY(L, g, rosterFull ? 1f : 0f);
            octoLash = 0.001f;
            octoLashArm = arm;
            octoLashLanded = false;
            octoWrongLash = true;
            octoTaunt = 0f;
            octoTarget = octoAttackArm = -1;
            octoReach = 0f;
            octoPause = 0.48f;
            rage = 1f;
            return PLAYER_HIT;
        }

        if (!asksFor(g)) return NONE;

        // Where a bullet fired at this press should land. The body by default; overridden below by
        // the one boss whose presses land somewhere more specific than "it".
        if (body != null && kind != SPLITTER) {
            hitX = body.centreX();
            hitY = body.centreY();
        }

        if (kind == OCTOPUS) {
            if (g != octoTarget || octoAttackArm < 0) return NONE;
            int arm = octoAttackArm;
            hitX = octoX[arm][OCTO_NODES - 1];
            hitY = octoY[arm][OCTO_NODES - 1];
            // The key defense only forces a recoil. Damage is earned by grabbing the
            // exposed tip and tearing this arm to an edge of the play area.
            octoVulnerableArm = arm;
            octoCoil = 0.001f;
            // The escape clock starts with the successful defense, not when the player finds it.
            octoDragTime = 0.001f;
            octoTarget = octoAttackArm = -1;
            octoReach = -1f;
            octoPause = 0f;
            if (body != null) body.squash(0.38f);
            return PART;
        }

        if (!open()) {
            // Its letter, at the wrong moment.
            rage = 1f;

            return REBUFF;
        }

        switch (kind) {
            case SLIME: {
                // Only ever its own next letter, since asksFor has turned the rest away.
                //
                // A press does not hurt it. It works at the skin: SPLIT_HITS of them tear a glob
                // loose, and it is carrying that glob off the screen that costs the slime health.
                // So the chain is not the fight, it is what earns you something to fight with —
                // which is why every press here is a PART and only dragTo returns a HIT.
                chainAt++;
                slimePromptHits=Math.min(2,slimePromptHits+1);
                split++;
                slimeKeyPulse = 0.11f;
                promptT = promptDelay();
                // Felt where the chain is being worked, even though nothing is being taken off the
                // bar yet. A press with no answer at all reads as a press that missed.
                hurt = Math.max(hurt, 0.55f);
                if (body != null) {
                    // On the top of the goo rather than in the middle of it, so the dent and the
                    // bullet that plays the press back agree about where the press landed. An
                    // impulse at the centroid has no direction to dent in and just shrinks it.
                    hitY = body.centreY() - body.radiusY() * 0.6f;
                    body.impulse(hitX, hitY, HIT_PUNCH * 0.7f);
                }
                if (split < SPLIT_HITS) return PART;
                split = 0;
                shedGlob(rnd);
                return SPLIT;
            }

            case SPLITTER: {
                int part = dividePieceFor(g);
                if (part < 0) return NONE;
                hitX = divideX[part];
                hitY = divideY[part];
                halfIdle[part] = 0f;
                halfHurt[part] = 1f;
                pieceHits[part] = Math.min(DIVIDE_HITS, pieceHits[part] + 1);
                divideHits = pieceHits[part];
                hurt = Math.max(hurt, 0.55f);
                Softbody pb = divideBody[part];
                if (pb != null) pb.impulse(hitX, hitY - pb.radiusY() * 0.45f, HIT_PUNCH * 0.7f);
                if (pieceHits[part] < DIVIDE_HITS) {
                    rerollPiece(part, rnd);
                    return PART;
                }
                return PART;
            }

            default: return NONE;
        }
    }

    /** Index of the element containing x,y, or -1. Nearest centre wins. */
    int elemAt(float x, float y) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < ELEMS; i++) {
            if (etype[i] == E_OFF) continue;
            float dx = x - ex[i], dy = y - ey[i];
            float d = dx * dx + dy * dy;
            // Generous, like the key hit-test: these are small targets on a moving field.
            float r = er[i] * (etype[i] == E_GLOB ? GLOB_TOUCH : 1.35f);
            if (d <= r * r && d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** True when element {@code i} is dragged rather than tapped. */
    boolean draggable(int i) {
        if (i < 0 || i >= ELEMS) return false;
        return etype[i] == E_GLOB;
    }

    /** Picks element {@code i} up. Returns true when the boss took the finger. */
    boolean grab(int i) {
        if (!fighting() || !draggable(i)) return false;
        held = i;
        globDragStarted = globDragCanDamage = false;
        return true;
    }

    /** The mushroom cap, or an exposed Octopulse arm tip, can own a drag. */
    boolean grabBody(float x, float y) {
        if (!fighting() || body == null) return false;
        if (kind == OCTOPUS && octoVulnerableArm >= 0 && octoCoil >= 0.72f) {
            int tip = OCTO_NODES - 1;
            float dx = x - octoX[octoVulnerableArm][tip];
            float dy = y - octoY[octoVulnerableArm][tip];
            if (dx * dx + dy * dy > body.rest * body.rest * 0.82f) return false;
            held = -3;
            octoDragX = x;
            octoDragY = y;
            octoDragStarted = octoDragCanDamage = false;
            return true;
        }
        if (kind != MUSHROOM) return false;
        float charge = mushroomCharge <= 0f ? 0f : 1f - mushroomCharge / MUSHROOM_CHARGE_TIME;
        float squeeze = (float) Math.sin(charge * Math.PI * 0.5f);
        float sx = 1f + squeeze * 0.16f, sy = 1f - squeeze * 0.30f;
        float capX = body.centreX() + mushroomCapDX;
        float capY = body.centreY() + body.radiusY() * 0.65f + mushroomCapDY;
        float bottom = body.centreY() + body.rest * (3.55f - squeeze * 0.20f);
        float attachY = capY + body.rest * 0.36f * sy;
        float angle = Math.max(-0.62f, Math.min(0.62f,
                (float) Math.atan2(mushroomCapDX, Math.max(body.radiusY() * 0.55f, bottom - attachY))));
        float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
        float localX = (x - capX) * ca + (y - capY) * sa;
        float localY = -(x - capX) * sa + (y - capY) * ca;
        float dx = localX / Math.max(1f, body.radiusX() * sx * 1.90f);
        float dy = localY / Math.max(1f, body.radiusY() * sy * (localY < 0f ? 2.35f : 0.58f));
        if (dx * dx + dy * dy > 1f) return false;
        held = -2;
        mushroomLastX = x;
        mushroomDirection = mushroomShakes = 0;
        mushroomGuideX = 0f;
        mushroomPlayerX = 0f;
        mushroomGuideTarget = 1;
        mushroomMeterAlpha = 0f;
        mushroomShakeWindow = 1.25f / mushroomRate();
        return true;
    }

    private static boolean octoTearZone(float x, float y, Layout L, float edge) {
        return x <= L.playLeft + edge || x >= L.playRight - edge
                || y <= L.playTop + edge || y >= L.dangerY - edge;
    }

    /**
     * A held element following a finger.
     *
     * @return {@link #HIT} when the drag completed the job, {@link #PART} while it is still under
     *     way, {@link #NONE} when nothing is held
     */
    int dragTo(float x, float y, Layout L) {
        if (!fighting()) return NONE;
        if (held == -3 && kind == OCTOPUS && octoVulnerableArm >= 0) {
            float edge = L.keyR * 0.45f;
            if (!octoDragStarted) {
                octoDragCanDamage = !octoTearZone(octoDragX, octoDragY, L, edge);
                octoDragStarted = true;
            }
            octoDragX = x;
            octoDragY = y;
            int arm = octoVulnerableArm;
            hitX = octoX[arm][OCTO_NODES - 1];
            hitY = octoY[arm][OCTO_NODES - 1];
            boolean torn = octoTearZone(x, y, L, edge);
            if (!octoDragCanDamage) {
                if (!torn) octoDragCanDamage = true;
                return PART;
            }
            if (!torn) return PART;
            octoArms &= ~(1 << arm);
            OctoThrow.torn(this);
            octoDyingArm = arm;
            if (body != null) {
                float recoilX = body.centreX() - hitX;
                float recoilY = body.centreY() - hitY;
                body.shove(recoilX, recoilY, 17.25f);
                body.impulse(hitX, hitY, 0.72f);
            }
            octoDeath = 0.001f;
            octoVulnerableArm = -1;
            octoCoil = 0f;
            octoDragStarted = octoDragCanDamage = false;
            octoDragTime = 0f;
            held = -1;
            octoPause = 0.80f;
            hurt = 1f;
            if (body != null) body.squash(0.92f);
            return damage(1f);
        }
        if (held == -2 && kind == MUSHROOM) {
            float cx = body == null ? x : body.centreX();
            float cy = body == null ? y : body.centreY();
            float capMovement = x - cx - mushroomCapDX;
            mushroomCapDX = x - cx;
            mushroomCapDY = Math.max(-bodyR(L) * 0.55f,
                    Math.min(bodyR(L) * 0.55f, y - cy));
            shedMushroomDust(capMovement, L);
            float dx = x - mushroomLastX;
            if (Math.abs(dx) > L.w * 0.015f) mushroomMeterAlpha = Math.max(mushroomMeterAlpha, 0.01f);
            // A twitch is not a shake: every accepted pass must cover 35% of the physical screen.
            float threshold = L.w * MUSHROOM_SHAKE_WIDTH;
            float playerScale = mushroomDirection == 0 ? 1f : 2f;
            mushroomPlayerX = Math.max(-1f, Math.min(1f,
                    mushroomDirection + dx / threshold * playerScale));
            if (Math.abs(dx) < threshold) {
                return PART;
            }
            int direction = dx < 0f ? -1 : 1;
            boolean guideReady = Math.abs(mushroomGuideX - mushroomGuideTarget) < MUSHROOM_GUIDE_WINDOW;
            if (!guideReady || direction != mushroomGuideTarget) {
                held = -1;
                mushroomShakes = mushroomDirection = 0;
                mushroomMeterAlpha = 0f;
                mushroomReject = 1f;
                if (body != null) { body.squash(-0.78f); body.letGo(); }
                if (mushroomStem != null) mushroomStem.squash(-0.65f);
                return PART;
            }
            mushroomLastX = x;
            mushroomSweepFlash = 0.28f;
            mushroomShakeCue = true;
            if (mushroomDirection != 0 && direction != mushroomDirection) mushroomShakes++;
            mushroomDirection = direction;
            mushroomPlayerX = direction;
            mushroomGuideTarget = -direction;
            mushroomShakeWindow = 1.25f / mushroomRate();
            if (body != null) {
                body.letGo();
                body.impulse(body.centreX() - direction * body.radiusX() * 0.62f,
                        body.centreY(), 0.44f);
                body.squash(-0.28f);
            }
            if (mushroomShakes < MUSHROOM_SHAKES) return PART;
            hitX = body == null ? x : body.centreX();
            hitY = body == null ? y : body.centreY();
            held = -1;
            int result = damage(1f);
            if (!beaten) {
                mushroomAngry = MUSHROOM_ANGER_TIME;
                mushroomReaction = true;
                mushroomCharge = 0f;
                mushroomAttackT = MUSHROOM_ATTACK_GAP;
                body.squash(-0.72f);
                if (mushroomStem != null) mushroomStem.squash(0.55f);
                shedMushroomDust(L.w * 0.3f, L);
            }
            return result;
        }
        if (held < 0 || etype[held] == E_OFF) return NONE;
        int t = etype[held];
        if (t == E_GLOB && !globDragStarted) {
            globDragCanDamage = !globDamageZone(ex[held], ey[held], held, L);
            globDragStarted = true;
        }
        ex[held] = x;
        ey[held] = y;
        if (t == E_GLOB) {
            // Carried to the edge of the play area, which is worth another hit on the boss.
            //
            // The test is the glob's own edge touching the play edge, not its centre reaching it.
            // Centre-on-the-line meant the finger had to arrive within a couple of percent of the
            // physical screen edge, where Android's own edge gestures start stealing the touch — so
            // the drag kept ending in a lifted finger instead of a landed hit, and it read as only
            // counting on release. There are two glob radii of slack now, and the glob is
            // visibly against the wall when it lands.
            boolean inDamageZone = globDamageZone(x, y, held, L);
            if (!globDragCanDamage) {
                if (!inDamageZone) globDragCanDamage = true;
                return PART;
            }
            if (inDamageZone) {
                // Free. The skin lets go and snaps back: a dent inward exactly where it had been
                // stretched out to, plus a whole-body wobble, on top of the release the solver gives
                // for nothing. This is the moment the mechanic is built around, so it is the one
                // place the body gets a full-strength answer.
                if (body != null) {
                    body.letGo();
                    body.impulse(ex[held], ey[held], SNAP_BACK);
                    body.squash(0.4f);
                }
                clearElem(held);
                held = -1;
                // Worth a hit of its own, which is what makes carrying one off a decision rather
                // than tidying up. A glob is only ever shed by a press, so this cannot feed itself:
                // damage taken here sheds nothing further.
                slimeDragPulse = 0.30f;
                int result = damage(1f);
                if (!beaten) slimeRetaliating = true;
                return result;
            }
        }
        return PART;
    }

    static float globSideEdge(Layout L, float radius, boolean right) {
        return right ? L.playRight - radius * GLOB_EDGE : L.playLeft + radius * GLOB_EDGE;
    }

    private boolean globDamageZone(float x, float y, int element, Layout L) {
        float slack = er[element] * GLOB_EDGE;
        return x <= globSideEdge(L, er[element], false) || x >= globSideEdge(L, er[element], true)
                || y <= L.playTop + slack;
    }

    /** The finger lifted without finishing. A glob flows back; a stolen key stays dropped. */
    void release() {
        if (held >= 0 && etype[held] == E_GLOB) returning[held] = true;
        held = -1;
        mushroomShakes = mushroomDirection = 0;
        if (mushroomStem != null) mushroomStem.letGo();
        globDragStarted = globDragCanDamage = false;
        octoDragStarted = octoDragCanDamage = false;
    }

    // ---- internals ----------------------------------------------------------

    private int damage(float n) {
        hp -= n;
        hurt = 1f;
        if (kind == SLIME) promptT = Math.min(promptT, promptDelay());
        // Dented where it was struck, from above, and harder for a bigger hit. The last blow gets a
        // full-strength punch, which is what makes the burst look earned.
        if (body != null) {
            float k = hp <= 0f ? 1f : HIT_PUNCH * n;
            body.impulse(body.centreX(), body.centreY() - body.radiusY() * 0.65f, k);
        }
        if (hp <= 0f) {
            hp = 0f;
            beaten = true;
            OctoThrow.reset(this);
            leaveT = LEAVE;
            defeatBeat = 0;
            defeatStartW = body == null ? 0f : body.spanX();
            // Beaten, so it gives the key back and takes its litter with it. Cleared here as well as
            // in leave(): the exit animation still draws the body, and it must not still be holding
            // a key hostage while it bursts.

            held = -1;
            for (int i = 0; i < ELEMS; i++) clearElem(i);
            // Anything still in the air goes too: a bolt landing after the burst takes a life for a
            // fight that is already over.
            clearBolts();
        }
        return HIT;
    }

    private void clearElem(int i) {
        if (i < 0 || i >= ELEMS) return;
        etype[i] = E_OFF;
        elife[i] = 0f;
        moved[i] = false;
        returning[i] = false;
        if (held == i) held = -1;
    }

    /** First free element slot, or -1. */
    private int freeElem() {
        for (int i = 0; i < ELEMS; i++) {
            if (etype[i] == E_OFF) return i;
        }
        return -1;
    }

    /** {@link #SLIME} tears a glob loose every {@link #SPLIT_HITS} presses. Left alone it fades. */
    private void shedGlob(Random rnd) {
        int i = freeElem();
        if (i < 0) return;
        etype[i] = E_GLOB;
        elife[i] = GLOB_TIME;
        // Placed by the layout pass on the frame it appears; these are the offsets it uses. Cleared
        // of any earlier drag in this slot, or a fresh glob would appear wherever the last one in
        // that slot was let go.
        globSide[i] = rnd.nextBoolean() ? -1f : 1f;
        globLift[i] = 0.35f + rnd.nextFloat() * 0.5f;
        moved[i] = false;
        returning[i] = false;
        seed(i);
        // The hit tears the silhouette outward before the loose patch settles into it.
        hurt = 1f;
        if (body != null) {
            float woundX = lastBX + globSide[i] * lastBR * wide();
            body.impulse(woundX, lastBY, HIT_PUNCH * 1.25f);
            body.squash(0.82f);
        }
    }

    /**
     * Puts element {@code i} where it belongs relative to a body at {@code bx,by} of radius
     * {@code br}. Leaves a held or already-moved element where the finger left it.
     *
     * The one copy of this arithmetic, called by the per-frame layout pass and by {@link #seed} on
     * the frame an element is created. Two copies is the trap this file has been bitten by before —
     * and the symptom here would have been a glob that jumped a few pixels on its second frame.
     */
    private void place(int i, float bx, float by, float br, Layout L) {
        if (etype[i] == E_GLOB) {
            // Large enough to read as the fight target at phone scale. It stays on the outside edge,
            // so the stronger mark cannot be mistaken for another eye or part of the face.
            er[i] = br * 0.30f;
            if (held == i || moved[i]) return;
            // Ahead of the resting edge. The soft-body constraint grows the skin around
            // the boss — it sits as a colour change in the goo it split off from, until a finger
            // hauls it out. That is also what makes the stretch read: the skin has to be dragged out
            // around something that started within it.
            //
            // Spread across the width, so a wide boss sheds them out along itself instead of
            // stacking them all down its middle. Vertically it is the plain radius: the body is only
            // ever wide, never tall, so that axis has no room to spare.
            // Its centre sits just outside rest; the live outline protrudes around it as one shape.
            ex[i] = bx + globSide[i] * (br * wide() + er[i] * WART_OUT);
            ey[i] = by + br * (globLift[i] - 0.6f) * 0.5f;
        }
    }

    private void seed(int i) {
        if (lastBR > 0f) place(i, lastBX, lastBY, lastBR, null);
    }

    /**
     * Where the layout pass last put the body.
     *
     * Kept because an element is created by a press, which has no {@link Layout} to hand, and the
     * layout pass does not run again until the next frame — so a glob shed this frame had no position
     * at all until then, and both the renderer and the hit-test read it as sitting at the origin. On
     * screen that was a red blob appearing in the top-left corner for a frame every time the boss was
     * hit. Seeding from here means an element is somewhere sensible from the instant it exists.
     */
    private float lastBX, lastBY, lastBR;

    /** Where a shed glob sits relative to the body, until a finger moves it. */
    private final float[] globSide = new float[ELEMS];
    private final float[] globLift = new float[ELEMS];
    /** A released glob easing back to its attachment point on the moving body. */
    private final boolean[] returning = new boolean[ELEMS];
    /** True once a finger has moved this element, so the layout pass stops placing it. */
    private final boolean[] moved = new boolean[ELEMS];

    /** A wanted letter that is not {@code avoid}, drawn uniformly over the five that qualify. */

    /** Which key a dropped-key element is carrying. */

    boolean keyDisabled(int g) { return kind == OCTOPUS && (disabledKeys & (1 << g)) != 0; }

    /** All player input is locked while a wrong-key retaliation whip is in flight. */
    boolean playerLocked() {
        return kind == OCTOPUS && octoLash > 0f || kind == SLIME && slimeKeyLock > 0f;
    }

    private void updateOctopus(float dt, Layout L, Random rnd) {
        octoImpact = octoPlayerHit = false;
        octoTaunt = Math.max(0f, octoTaunt - dt / 1.20f);
        octoEat = Math.max(0f, octoEat - dt / 0.92f);
        octoEscape = Math.max(0f, octoEscape - dt / 0.72f);
        if (octoVulnerableArm >= 0) {
            octoDragTime += dt;
            if (octoDragTime >= 2f) {
                octoEscapeArm = octoVulnerableArm;
                octoEscape = 1f;
                octoVulnerableArm = -1;
                octoCoil = 0f;
                octoDragStarted = octoDragCanDamage = false;
                octoDragTime = 0f;
                held = -1;
                octoFlurryLeft = 3;
                octoFlurryT = 0f;
                octoPause = 1.25f;
                rage = 1f;
                if (body != null) body.squash(-0.52f);
            }
        }
        if (octoFlurryLeft > 0) {
            octoFlurryT -= dt;
            if (octoFlurryT <= 0f && singleBolt(rnd, body.centreX(), body.centreY()) >= 0) {
                octoFlurryLeft--;
                octoFlurryT = 0.22f;
            }
        }
        if (!octoPlaced) {
            float cx = bodyX(L), cy = bodyY(L);
            for (int a = 0; a < OCTO_ARMS; a++) {
                for (int n = 0; n < OCTO_NODES; n++) {
                    octoX[a][n] = cx;
                    octoY[a][n] = cy;
                    octoVX[a][n] = octoVY[a][n] = 0f;
                }
            }
            octoPlaced = true;
        }

        OctoThrow.update(this, dt, rnd);
        if (octoLash > 0f) {
            octoLash += dt / 0.46f;
            if (!octoLashLanded && octoLash >= 0.64f) {
                octoLashLanded = true;
                octoImpact = octoPlayerHit = true;
            }
            if (octoLash >= 1f) {
                octoLash = 0f;
                octoLashArm = -1;
                if (octoWrongLash) {
                    octoTaunt = 1f;
                    octoPause = 1.18f;
                } else octoPause = 0.42f;
                octoWrongLash = false;
            }
        }
        if (octoDeath > 0f) {
            octoDeath += dt / 1.05f;
            if (octoDeath >= 1f) {
                octoDeath = 0f;
                octoDyingArm = -1;
            }
        }

        if (octoLash <= 0f) {
            if (octoCaptured >= 0) {
                octoReturn += dt / 0.68f;
                if (octoReturn >= 1f) {
                    octoCaptured = octoAttackArm = -1;
                    octoReturn = 0f;
                    octoEat = 1f;
                    octoPause = 0.56f;
                    if (body != null) body.squash(0.34f);
                }
            } else if (octoVulnerableArm >= 0) {
                octoCoil = Math.min(1f, octoCoil + dt / 0.78f);
            } else if (octoTarget < 0) {
                octoPause -= dt;
                if (octoPause <= 0f && octoArms != 0 && octoFlurryLeft == 0 && !OctoThrow.busy(this) && boltCount() == 0)
                    startOctoReach(rnd);
            } else if (octoSweep < 1f) {
                octoSweep = Math.min(1f, octoSweep + dt / 0.68f);
            } else if (octoCharge < 1f) {
                float before = octoCharge;
                octoCharge = Math.min(1f, octoCharge + dt / 0.32f);
                if (before < 1f && octoCharge >= 1f) {
                    octoReach = 0f;
                    octoCue = true;
                }
            } else {
                // The strike keeps the old reaction duration; sweep and charge are added before it.
                float duration = Math.max(0.36f,
                        0.925f - Integer.bitCount(disabledKeys) * 0.125f);
                octoReach += dt / duration;
                if (octoReach >= 1f) {
                    octoTaunt = 0.60f;
                    if (body != null) body.squash(0.18f);
                    if (octoKeysLeft() <= 2) {
                        octoLashX = Roster.keyX(L, octoTarget, rosterFull ? 1f : 0f);
                        octoLashY = Roster.keyY(L, octoTarget, rosterFull ? 1f : 0f);
                        octoLashArm = octoAttackArm;
                        octoLash = 0.64f;
                        octoLashLanded = true;
                        octoTarget = octoAttackArm = -1;
                        octoReach = -1f;
                        octoImpact = octoPlayerHit = true;
                        rage = 1f;
                    } else {
                        octoCaptured = octoTarget;
                        disabledKeys |= 1 << octoTarget;
                        octoTarget = -1;
                        octoReach = octoReturn = 0f;
                        rage = 1f;
                        octoLock = octoImpact = true;
                    }
                }
            }
        }

        poseOctopus(dt, L);
        OctoThrow.release(this);
    }

    private void poseOctopus(float dt, Layout L) {
        float cx = body.centreX(), cy = body.centreY();
        float brace = 0f;
        int reachingArm = octoAttackArm;
        int reachingKey = octoTarget >= 0 ? octoTarget : octoCaptured;
        if (!beaten && reachingArm >= 0 && reachingKey >= 0) {
            // Give the free arms a spring target on the very first reach frame.
            // Complete the recoil during the opening sweep, before the key is locked.
            brace = octoCaptured >= 0 ? Math.max(0f, 1f - octoReturn)
                    : Math.min(1f, 0.20f + Math.max(0f, octoSweep) * 4f);
            brace = brace * brace * (3f - 2f * brace);
        }
        float aimAngle = 0f;
        if (brace > 0f) aimAngle = -(float)Math.atan2(
                Roster.keyX(L, reachingKey, rosterFull ? 1f : 0f) - cx,
                Roster.keyY(L, reachingKey, rosterFull ? 1f : 0f) - cy);
        octoLean = aimAngle * brace;
        float leanCos = (float)Math.cos(octoLean), leanSin = (float)Math.sin(octoLean);
        float resistedDragX = octoDragX, resistedDragY = octoDragY;
        if (held == -3) {
            float resistance = Math.min(1f, octoDragTime / 2f);
            resistance *= resistance;
            float towardX = cx - octoDragX, towardY = cy - octoDragY;
            float towardD = Math.max(1f, (float) Math.sqrt(towardX * towardX + towardY * towardY));
            resistedDragX += towardX / towardD * bodyR(L) * 1.35f * resistance;
            resistedDragY += towardY / towardD * bodyR(L) * 1.35f * resistance;
        }
        for (int a = 0; a < OCTO_ARMS; a++) {
            float angle = -1.18f + 2.36f * a / (OCTO_ARMS - 1);
            float breathe = 1f + 0.045f
                    * (float) Math.sin(age * 0.62f + a * 1.73f);
            float tipLength = bodyR(L) * (3.12f + 0.15f * (a % 3)) * breathe;
            float restTipX = cx + (float) Math.sin(angle) * tipLength;
            float restTipY = cy + (float) Math.cos(angle) * tipLength;
            float tipWave = (float) Math.sin(age * 0.78f + a * 1.37f + 3.4f)
                    * bodyR(L) * 0.18f;
            restTipX += (float) Math.cos(angle) * tipWave;
            restTipY -= (float) Math.sin(angle) * tipWave;
            float tipCurl = bodyR(L) * 0.04f * (2.6f + 0.12f * (a % 3));
            float tipSide = a < 4 ? -1f : 1f;
            restTipX += (float) Math.cos(angle) * tipCurl * tipSide;
            restTipY -= (float) Math.sin(angle) * tipCurl * tipSide;
            for (int n = 0; n < OCTO_NODES; n++) {
                float u = n / (float) (OCTO_NODES - 1);
                // angle and breathe are shared with the resting-tip calculation above.
                float length = bodyR(L) * (0.28f + u * (2.84f + 0.15f * (a % 3)))
                        * breathe;
                float tx = cx + (float) Math.sin(angle) * length;
                float ty = cy + (float) Math.cos(angle) * length;
                float wave = (float) Math.sin(age * 0.78f + a * 1.37f + u * 3.4f)
                        * bodyR(L) * 0.18f * u;
                tx += (float) Math.cos(angle) * wave;
                ty -= (float) Math.sin(angle) * wave;
                float hook = Math.max(0f, u - 0.80f);
                float curl = hook * hook * bodyR(L) * (2.6f + 0.12f * (a % 3));
                float curlSide = a < 4 ? -1f : 1f;
                tx += (float) Math.cos(angle) * curl * curlSide;
                ty -= (float) Math.sin(angle) * curl * curlSide;

                // Spread the attachments around the mantle and fold the free arms over tight arches.
                if (brace > 0f && a != reachingArm && a != octoVulnerableArm
                        && a != octoEscapeArm && a != octoDyingArm) {
                    float r = bodyR(L), outer = Math.abs(a - 3.5f) / 3.5f;
                    float rootX = cx + curlSide * body.radiusX() * (0.20f + outer * 0.60f);
                    float rootY = cy + body.radiusY() * (0.38f - outer * 0.75f);
                    float crestX = cx + curlSide * r * (0.85f + outer * 1.65f);
                    float crestY = cy - r * (0.12f + outer * 0.78f);
                    float sideRoom = curlSide < 0f ? cx - L.playLeft : L.playRight - cx;
                    float fanWidth = Math.max(r * 0.75f, Math.min(r * 4.15f, sideRoom - r * 0.25f));
                    float tipX = cx + curlSide * (r * 0.70f + outer * (fanWidth - r * 0.70f));
                    float tipY = cy + r * (2.90f - outer * 1.85f);
                    // A short shoulder arch turns into a longer falling section at a tight elbow.
                    float q, ax, ay, bx, by, ex, ey;
                    if (u < 0.32f) {
                        q = u / 0.32f;
                        ax = rootX; ay = rootY;
                        bx = rootX + (crestX - rootX) * 0.55f;
                        by = crestY - r * 0.24f;
                        ex = crestX; ey = crestY;
                    } else {
                        q = (u - 0.32f) / 0.68f;
                        ax = crestX; ay = crestY;
                        bx = crestX + curlSide * r * (0.08f + outer * 0.55f);
                        by = cy + r * 1.05f;
                        ex = tipX; ey = tipY;
                    }
                    float v = 1f - q;
                    float archX = v*v*ax + 2f*v*q*bx + q*q*ex;
                    float archY = v*v*ay + 2f*v*q*by + q*q*ey;
                    archX += (float)Math.sin(age * 1.3f + a + u * 4f) * r * 0.07f * u;
                    float ca = (float)Math.cos(aimAngle), sa = (float)Math.sin(aimAngle);
                    float dx = archX - cx, dy = archY - cy;
                    archX = cx + dx * ca - dy * sa;
                    archY = cy + dx * sa + dy * ca;
                    tx += (archX - tx) * brace;
                    ty += (archY - ty) * brace;
                }

                if (!beaten && octoVulnerableArm >= 0 && a != octoVulnerableArm
                        && (octoArms & (1 << a)) != 0 && a != octoDyingArm) {
                    // Stagger the pain ripple across intact arms; leave the grab target alone.
                    float onset = Math.min(1f, octoDragTime / 0.18f);
                    float phase = octoDragTime * 7.5f + a * 1.37f - u * 4.5f;
                    float writhe = (float)Math.sin(phase) * bodyR(L) * 0.72f * u * onset;
                    tx += (float)Math.cos(angle) * writhe;
                    ty -= (float)Math.sin(angle) * writhe;
                    ty -= (0.5f + 0.5f * (float)Math.sin(phase + 1.2f))
                            * bodyR(L) * 0.48f * u * u * onset;
                }

                if (a == octoAttackArm) {
                    float dx = tx - cx, dy = ty - cy;
                    tx = cx + dx * leanCos - dy * leanSin;
                    ty = cy + dx * leanSin + dy * leanCos;
                    int key = octoTarget >= 0 ? octoTarget : octoCaptured;
                    float keyX = Roster.keyX(L, key, rosterFull ? 1f : 0f);
                    float keyY = Roster.keyY(L, key, rosterFull ? 1f : 0f);
                    float frontY = L.deckTop - L.keyR * 0.42f;
                    float aimX = keyX, aimY = frontY;
                    if (octoTarget >= 0 && octoSweep < 1f) {
                        float sweep = Math.max(0f, octoSweep);
                        float waveX = L.w * 0.5f + (float) Math.sin(sweep * Softbody.TAU * 1.5f)
                                * (L.playRight - L.playLeft) * 0.43f;
                        float settle = Math.max(0f, Math.min(1f, (sweep - 0.70f) / 0.30f));
                        settle = settle * settle * (3f - 2f * settle);
                        aimX = waveX + (keyX - waveX) * settle;
                    }
                    float staged = u * u;
                    tx += (aimX - tx) * staged;
                    ty += (aimY - ty) * staged;
                    float q = octoCaptured >= 0 ? 1f - octoReturn
                            : octoCharge >= 1f ? Math.max(0f, octoReach) : 0f;
                    q = q * q * (3f - 2f * q) * u * u;
                    tx += (keyX - tx) * q;
                    ty += (keyY - ty) * q;
                    if (octoCaptured >= 0) {
                        float swallow = Math.max(0f, Math.min(1f, octoReturn));
                        swallow = swallow * swallow * (3f - 2f * swallow) * u;
                        float mouthX = cx;
                        float mouthY = cy + bodyR(L) * 0.20f;
                        tx += (mouthX - tx) * swallow;
                        ty += (mouthY - ty) * swallow;
                    }
                }
                if (a == octoVulnerableArm) {
                    float coil = Math.max(0f, Math.min(1f, octoCoil));
                    coil = coil * coil * (3f - 2f * coil);
                    // Curl the distal half around its idle path. The old coil was positioned from
                    // the body centre, so a successful defense could hide the exposed tip inside
                    // Octopulse. This loop returns to the ordinary resting tip and stays outside.
                    float coilU = Math.max(0f, Math.min(1f, (u - 0.38f) / 0.62f));
                    float loop = coilU * Softbody.TAU;
                    float side = (a < 4 ? -1f : 1f) * (float) Math.sin(loop)
                            * bodyR(L) * 0.58f;
                    float outward = (1f - (float) Math.cos(loop)) * bodyR(L) * 0.25f;
                    float coilX = tx + (float) Math.cos(angle) * side
                            + (float) Math.sin(angle) * outward;
                    float coilY = ty - (float) Math.sin(angle) * side
                            + (float) Math.cos(angle) * outward;
                    tx += (coilX - tx) * coil;
                    ty += (coilY - ty) * coil;
                    if (held != -3) {
                        float urgency = Math.min(1f, octoDragTime / 2f);
                        float safeTop = L.playTop + L.keyR * 1.15f;
                        float safeBottom = L.deckTop - L.keyR * 1.35f;
                        float playCX = (L.playLeft + L.playRight) * 0.5f;
                        float playCY = (safeTop + safeBottom) * 0.5f;
                        // The endpoint sweeps across half the screen while staying catchable.
                        int armsRemoved = OCTO_ARMS - Integer.bitCount(octoArms);
                        float armSpeed = 0.30f + 0.20f * armsRemoved / (OCTO_ARMS - 1f);
                        float dodgePhase = octoDragTime * (5.5f + urgency * 3.0f)
                                * armSpeed + a * 0.71f;
                        float targetX = playCX + (float) Math.sin(dodgePhase) * L.w * 0.25f;
                        float targetY = playCY + (float) Math.cos(dodgePhase * 1.23f)
                                * (safeBottom - safeTop) * 0.22f;
                        targetX = Math.max(L.playLeft + L.keyR,
                                Math.min(L.playRight - L.keyR, targetX));
                        targetY = Math.max(safeTop, Math.min(safeBottom, targetY));
                        tx += (targetX - restTipX) * u;
                        ty += (targetY - restTipY) * u;
                        // A high-amplitude traveling wave makes the whole arm conduct that sweep.
                        float waveEnvelope = (float) Math.sin(Math.PI * u);
                        float armWave = (float) Math.sin(dodgePhase - u * Softbody.TAU * 1.25f)
                                * L.w * (0.075f + urgency * 0.040f) * waveEnvelope;
                        float aimX = targetX - cx, aimY = targetY - cy;
                        float aimD = Math.max(1f, (float) Math.sqrt(aimX * aimX + aimY * aimY));
                        tx += -aimY / aimD * armWave;
                        ty += aimX / aimD * armWave;
                        // Fast distal tremor: the pained hand-shake on top of the whole-arm wave.
                        float wrist = Math.max(0f, (u - 0.42f) / 0.58f);
                        wrist *= wrist;
                        float shake = (float) Math.sin(octoDragTime * (40f + urgency * 24f)
                                + u * 9f + a);
                        tx += -aimY / aimD * shake * L.w * 0.018f * wrist;
                        ty += aimX / aimD * shake * L.w * 0.018f * wrist;
                    }
                    if (held == -3) {
                        // Spread the fingertip displacement down the whole arm. The root remains
                        // fixed while every following segment takes an even share of the stretch.
                        float tipDX = resistedDragX - restTipX;
                        float tipDY = resistedDragY - restTipY;
                        tx += tipDX * u;
                        ty += tipDY * u;
                    }
                }
                if (octoLash > 0f && (octoWrongLash
                        ? (octoArms & (1 << a)) != 0 : a == octoLashArm)) {
                    float p = octoLash;
                    float phase = p < 0.64f ? p / 0.64f : (1f - p) / 0.36f;
                    phase = Math.max(0f, Math.min(1f, phase));
                    float snap = phase * phase * (3f - 2f * phase);
                    float reach = snap * u * u;
                    float lashX = octoLashX, lashY = octoLashY;
                    if (octoWrongLash) {
                        int key = Roster.at(rosterFull, a % Roster.count(rosterFull));
                        lashX = Roster.keyX(L, key, rosterFull ? 1f : 0f);
                        lashY = Roster.keyY(L, key, rosterFull ? 1f : 0f);
                    }
                    tx += (lashX - tx) * reach;
                    ty += (lashY - ty) * reach;
                    float whip = (float) Math.sin(u * Math.PI * 1.35f - p * 8.5f + a * 0.52f)
                            * bodyR(L) * 0.48f * phase * u;
                    tx += (float) Math.cos(angle) * whip;
                    ty -= (float) Math.sin(angle) * whip;
                }
                if (a == octoEscapeArm && octoEscape > 0f) {
                    float escapeWave = (float) Math.sin((1f - octoEscape) * Math.PI * 5f + u * 4f);
                    tx += (float) Math.cos(angle) * escapeWave * bodyR(L) * 0.58f * octoEscape * u;
                    ty -= (1f - octoEscape) * bodyR(L) * 0.32f * u;
                }
                if (octoEat > 0f && (octoArms & (1 << a)) != 0) {
                    float feast = 1f - octoEat;
                    float cheer = (float) Math.sin(feast * Math.PI * 7f + a * 0.82f);
                    float flourish = u * u * bodyR(L) * (0.42f + 0.30f * octoEat);
                    tx += (float) Math.cos(angle) * cheer * flourish;
                    ty -= (0.45f + 0.55f * Math.abs(cheer)) * flourish;
                }
                if (octoTaunt > 0f) {
                    float boast = (float) Math.sin(age * 9f + a * 1.15f)
                            * bodyR(L) * 0.34f * octoTaunt * u * u;
                    tx += boast;
                    ty -= Math.abs(boast) * 0.42f;
                }
                if (beaten) {
                    // A frustrated shrug turns into limp curls, carried with the falling mantle.
                    float t = leaveProgress();
                    float shrug = (float) Math.sin(Math.min(1f, t / 0.45f) * Math.PI);
                    float side = a < OCTO_ARMS / 2 ? -1f : 1f;
                    float flutter = (float) Math.sin(t * 22f + a * 0.8f + u * 4f)
                            * (1f - t) * u * u;
                    tx = cx + (float) Math.sin(angle) * bodyR(L) * (0.28f + 2.15f*u)
                            + side * bodyR(L) * (shrug * 0.55f + flutter * 0.32f) * u;
                    ty = cy + bodyR(L) * (0.22f + 2.0f*u - shrug * 2.2f*u*u
                            + flutter * 0.38f);
                }
                if (a == octoDyingArm && octoDeath > 0f && !beaten) {
                    float death = Math.min(1f, octoDeath);
                    // Release the stored drag tension in a quick elastic snap, then let the
                    // detached arm crumple and fall. The overshoot ripple makes the break read.
                    float snap = Math.min(1f, death / 0.24f);
                    snap = 1f - (1f - snap) * (1f - snap) * (1f - snap);
                    float after = Math.max(0f, (death - 0.24f) / 0.76f);
                    float contract = 1f - snap * 0.58f - after * 0.27f;
                    tx = cx + (tx - cx) * contract;
                    ty = cy + (ty - cy) * contract;
                    float recoil = (float) Math.sin(snap * Math.PI) * bodyR(L) * 0.92f
                            * (0.25f + 0.75f * u) * (a < 4 ? -1f : 1f);
                    tx += (float) Math.cos(angle) * recoil;
                    ty -= (float) Math.sin(angle) * recoil;
                    float thrash = (float) Math.sin(after * Math.PI * 3f + u * 5.5f)
                            * bodyR(L) * 0.42f * (1f - after) * u;
                    tx += (float) Math.cos(angle) * thrash;
                    ty -= (float) Math.sin(angle) * thrash;
                    ty += after * after * bodyR(L) * 0.70f * u;
                }

                if (!beaten && a == octoThrowArm) {
                    float blend = OctoThrow.blend(this);
                    float bend = u * u;
                    tx += (OctoThrow.handX(this, L) - restTipX) * bend * blend;
                    ty += (OctoThrow.handY(this, L) - restTipY) * bend * blend;
                    tx += (a < 4 ? -1f : 1f) * (float)Math.sin(u * Math.PI)
                            * bodyR(L) * .65f * blend;
                }

                tx += (float) Math.sin(age * 0.43f + a * 2.1f + u * 5.2f)
                        * bodyR(L) * 0.018f * u;
                float spring = a == octoDyingArm && octoDeath < 0.30f ? 68f : 26f;
                float damping = a == octoDyingArm && octoDeath < 0.30f ? 0.88f : 0.94f;
                octoVX[a][n] = (octoVX[a][n] + (tx - octoX[a][n]) * dt * spring) * damping;
                octoVY[a][n] = (octoVY[a][n] + (ty - octoY[a][n]) * dt * spring) * damping;
                octoX[a][n] += octoVX[a][n] * dt;
                octoY[a][n] += octoVY[a][n] * dt;
                if (!beaten && (a == octoVulnerableArm && held != -3 || a == octoThrowArm) && n > 0) {
                    // The ordinary tentacle spring deliberately lags idle motion, but that erased
                    // this fast half-screen gesture. Track the authored wave directly, retaining
                    // some elasticity along the arm and none at the catch point.
                    float waveFollow = n == OCTO_NODES - 1 ? 1f : Math.min(1f, dt * 28f);
                    octoX[a][n] += (tx - octoX[a][n]) * waveFollow;
                    octoY[a][n] += (ty - octoY[a][n]) * waveFollow;
                }
                if (!beaten && a == octoVulnerableArm && n == OCTO_NODES - 1) {
                    if (held == -3) {
                        float fingerFollow = Math.min(1f, dt * 46f);
                        octoX[a][n] += (resistedDragX - octoX[a][n]) * fingerFollow;
                        octoY[a][n] += (resistedDragY - octoY[a][n]) * fingerFollow;
                    } else {
                        float margin = L.keyR * 0.72f;
                        octoX[a][n] = Math.max(L.playLeft + margin,
                                Math.min(L.playRight - margin, octoX[a][n]));
                        octoY[a][n] = Math.max(L.playTop + margin,
                                Math.min(L.deckTop - margin, octoY[a][n]));
                    }
                }
                // A living arm is physically rooted in the moving soft body. Do not spring the
                // first node toward it: that produces a visible gap whenever the head rebounds.
                if (n == 0 && ((octoArms & (1 << a)) != 0 || beaten) && (beaten || a != octoDyingArm)) {
                    octoX[a][n] = tx;
                    octoY[a][n] = ty;
                    octoVX[a][n] = octoVY[a][n] = 0f;
                }
            }
        }
    }

    private int octoKeysLeft() {
        int count = 0;
        for (int g = 0; g < Glyph.COUNT; g++)
            if (Roster.active(rosterFull, g) && !keyDisabled(g)) count++;
        return count;
    }

    private void startOctoReach(Random rnd) {
        int[] choice = new int[Glyph.COUNT];
        int count = 0;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (!Roster.active(rosterFull, g) || keyDisabled(g)) continue;
            int side = g < 3 ? 0x07 : 0x38;
            if (Integer.bitCount(disabledKeys & side) < 2) choice[count++] = g;
        }
        // Both sides have lost their quota: keep attacking either survivor, now for damage.
        if (count == 0 && octoKeysLeft() <= 2) {
            for (int g = 0; g < Glyph.COUNT; g++)
                if (Roster.active(rosterFull, g) && !keyDisabled(g)) choice[count++] = g;
        }
        if (count == 0) return;
        octoTarget = choice[rnd.nextInt(count)];
        int pick = rnd.nextInt(Integer.bitCount(octoArms));
        for (int a = 0; a < OCTO_ARMS; a++) {
            if ((octoArms & (1 << a)) != 0 && pick-- == 0) {
                octoAttackArm = a;
                break;
            }
        }
        octoReach = -1f;
        octoSweep = 0.001f;
        octoCharge = 0f;
        octoCoil = 0f;
        octoWave = true;
    }

    // ---- bolts --------------------------------------------------------------
    /**
     * Letter bolts, thrown at the deck when the slime prompt expires.
     *
     * Not {@code Enemy}: a boss stage releases no words, and these are not words — they carry one
     * letter, fly at the key that clears them, and cost a life at the deck. Press the letter to swat
     * one; there is nothing to engage and no order to type them in.
     */
    static final int BOLTS = 3, MAX_BOLTS = 8;
    /** Seconds one takes to reach the deck. Long enough to read three letters and find three keys. */
    static final float BOLT_TIME = 2.4f;
    /**
     * Head start between bolts, in progress. They are launched together, so without this all three
     * land on the same frame and one volley takes three lives at once. 0.18 spaces them 0.43s apart
     * — long enough to press three keys in sequence.
     */
    static final float BOLT_STAGGER = 0.18f;
    /** Live flag, letter, launch point and 0..1 of the way down, per bolt. */
    final boolean[] blive = new boolean[MAX_BOLTS];
    final int[] bglyph = new int[MAX_BOLTS];
    final float[] bsx = new float[MAX_BOLTS];
    final float[] bsy = new float[MAX_BOLTS];
    final float[] bt = new float[MAX_BOLTS];
    final int[] bhp = new int[MAX_BOLTS];
    final int[] bhpMax = new int[MAX_BOLTS];

    /** Where bolt {@code i} is now: launch point to its own key, straight. */
    float boltX(int i, Layout L) {
        return bsx[i] + (Roster.keyX(L, bglyph[i], rosterFull ? 1f : 0f) - bsx[i]) * boltAt(i);
    }

    float boltY(int i, Layout L) {
        return bsy[i] + (Roster.keyY(L, bglyph[i], rosterFull ? 1f : 0f) - bsy[i]) * boltAt(i);
    }

    /** 0..1 of the way down, with the negative head start clamped off. */
    float boltAt(int i) {
        return bt[i] < 0f ? 0f : bt[i];
    }

    private int globIndex() {
        if (kind != SLIME) return -1;
        for (int i = 0; i < ELEMS; i++) if (etype[i] == E_GLOB) return i;
        return -1;
    }

    boolean hasGlob() { return globIndex() >= 0; }

    /** Live bolts on the field. */
    int boltCount() {
        int n = 0;
        for (int i = 0; i < MAX_BOLTS; i++) {
            if (blive[i]) n++;
        }
        return n;
    }

    /** True when a bolt is carrying {@code g}, so pressing it would swat one. */
    boolean boltWants(int g) {
        return boltNearest(g) >= 0;
    }

    /** The live bolt carrying {@code g} that is closest to landing, or -1. */
    private int boltNearest(int g) {
        int best = -1;
        for (int i = 0; i < MAX_BOLTS; i++) {
            if (blive[i] && bglyph[i] == g && (best < 0 || bt[i] > bt[best])) best = i;
        }
        return best;
    }

    /**
     * Throws a volley at the deck. Three distinct letters, so three different keys: two bolts on one
     * key would be cleared by one press, which is a volley of two.
     */
    private void volley(Random rnd) {
        int first = chainLetter();
        int hits = boltHits();
        chainAt++;
        launchT = LAUNCH_TIME;
        launched = true;
        slimeRetaliating = false;
        if (body != null) body.squash(0.75f);
        promptT = promptDelay();
        for (int i = 0; i < BOLTS; i++) {
            blive[i] = true;
            // Spread round the six rather than drawn independently — a repeat would collapse the
            // volley, and the spacing keeps the three keys apart on the deck.
            int offset = rosterFull ? i * 2 : (i == 2 ? 1 : i * 2);
            bglyph[i] = Roster.at(rosterFull,
                    (Roster.ordinal(rosterFull, first) + offset) % Roster.count(rosterFull));
            bhp[i] = bhpMax[i] = hits;
            bt[i] = -BOLT_STAGGER * i;
            // Fanned below the live underside, like drops expelled from the slime rather than
            // projectiles appearing in its face.
            bsx[i] = lastBX + (i - 1) * lastBR * 0.55f;
            bsy[i] = body == null ? lastBY + lastBR
                    : body.centreY() + body.radiusY() * 1.12f;
        }
    }

    /** Damage speeds the rhythm without changing swipe width or the attack warning. */
    float mushroomRate() {
        return 1f + 0.35f * Math.max(0f, Math.min(1f, 1f - health()));
    }

    // Cosmetic dust has its own fixed pool and sequence: it cannot consume attack slots or RNG.
    void shedMushroomDust(float movement, Layout L) {
        if (body == null || Math.abs(movement) < L.w * 0.001f) return;
        float r = bodyR(L);
        mushroomDustTravel += Math.abs(movement);
        int count = Math.min(16, (int) (mushroomDustTravel / (r * 0.09f)));
        if (count == 0) return;
        mushroomDustTravel %= r * 0.09f;
        float charge = mushroomCharge <= 0f ? 0f : 1f - mushroomCharge / MUSHROOM_CHARGE_TIME;
        float squeeze = (float) Math.sin(charge * Math.PI * 0.5f);
        float sx = 1f + squeeze * 0.16f, sy = 1f - squeeze * 0.30f;
        float ry = body.radiusY(), rx = body.radiusX();
        float capX = body.centreX() + mushroomCapDX;
        float capY = body.centreY() + ry * 0.65f + mushroomCapDY;
        float bottom = body.centreY() + r * (3.55f - squeeze * 0.20f);
        float angle = Math.max(-0.62f, Math.min(0.62f, (float) Math.atan2(mushroomCapDX,
                Math.max(ry * 0.55f, bottom - capY - r * 0.36f * sy))));
        float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
        for (int n = 0; n < count; n++) {
            int i = mushroomDustNext;
            mushroomDustNext = (i + 1) % MUSHROOM_DUST;
            float u = ((i * 23) % 61) / 60f * 1.8f - 0.9f;
            float dx = u * rx * sx * 1.72f;
            float dy = ry * sy * (0.22f + 0.29f * (float) Math.sqrt(1f - u * u));
            mushroomDustX[i] = capX + dx * ca - dy * sa;
            mushroomDustY[i] = capY + dx * sa + dy * ca;
            mushroomDustVX[i] = r * (Math.signum(movement) * 0.28f + u * 0.18f);
            mushroomDustVY[i] = r * (0.22f + (i % 5) * 0.06f);
            mushroomDustLife[i] = 0.95f;
        }
    }

    float mushroomDamagePulse() {
        if (mushroomAngry <= 0f) return 0f;
        float t = 1f - mushroomAngry / MUSHROOM_ANGER_TIME;
        float wave = (float) Math.sin(t * Math.PI * 3f);
        return Math.max(Math.max(0f, 1f - t * 8f), wave * wave * (1f - t * 0.55f));
    }

    /** Drops pale spores which become ordinary readable letter projectiles as they descend. */
    private void sporeVolley(int count, Random rnd) {
        int first = randomGlyph(rnd);
        int made = 0;
        for (int slot = 0; slot < MAX_BOLTS && made < count; slot++) {
            if (blive[slot]) continue;
            blive[slot] = true;
            bglyph[slot] = Roster.at(rosterFull,
                    (Roster.ordinal(rosterFull, first) + made) % Roster.count(rosterFull));
            bhp[slot] = bhpMax[slot] = 1;
            bt[slot] = -BOLT_STAGGER * made;
            float spread = count <= 1 ? 0f : (made - (count - 1) * 0.5f) / (count - 1);
            bsx[slot] = lastBX + spread * lastBR * 1.55f;
            bsy[slot] = body == null ? lastBY : body.centreY() + body.radiusY() * 0.72f;
            made++;
        }
        launchT = LAUNCH_TIME;
        launched = made > 0;
        mushroomSporeCue = made > 0;
        if (body != null) body.squash(0.92f);
    }

    boolean beginPinch(float distance) {
        return beginPinch(distance, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
    }

    boolean beginPinch(float distance, float x1, float y1, float x2, float y2) {
        if (!fighting() || kind != SPLITTER || distance <= 0f) return false;
        float mx = Float.isNaN(x1) ? Float.NaN : (x1 + x2) * 0.5f;
        float my = Float.isNaN(y1) ? Float.NaN : (y1 + y2) * 0.5f;
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int n = 0; n < DIVIDE_NODES; n++) {
            if (!nodeActive(n) || pieceHits[n] < DIVIDE_HITS ) continue;
            float dx = Float.isNaN(mx) ? 0f : divideX[n] - mx;
            float dy = Float.isNaN(my) ? 0f : divideY[n] - my;
            float d = dx * dx + dy * dy;
            float reach = pieceRadiusNode(n, null) * 1.45f;
            if (!Float.isNaN(mx) && d > reach * reach) continue;
            if (d < bestD) { bestD = d; best = n; }
        }
        if (best < 0) return false;
        pinchNode = best;
        pinchStart = distance;
        pinchX1 = x1; pinchY1 = y1; pinchX2 = x2; pinchY2 = y2;
        return true;
    }

    boolean pinch(float distance) {
        return pinch(distance, Float.NaN, Float.NaN, Float.NaN, Float.NaN, null);
    }

    boolean pinch(float distance, float x1, float y1, float x2, float y2, Random rnd) {
        if (pinchNode < 0 || pinchStart <= 0f || !nodeActive(pinchNode)) return false;
        Softbody pb = divideBody[pinchNode];
        if (!Float.isNaN(x1)) {
            pinchX1 = x1; pinchY1 = y1; pinchX2 = x2; pinchY2 = y2;
            if (pb != null) {
                divideX[pinchNode] = (x1 + x2) * 0.5f;
                divideY[pinchNode] = (y1 + y2) * 0.5f;
                pb.moveTo(divideX[pinchNode], divideY[pinchNode]);
                pb.encompass(x1, y1, x2, y2);
            }
        }
        if (distance / pinchStart < DIVIDE_SCALE) return false;
        int parent = pinchNode, left = parent * 2 + 1, right = left + 1;
        hitX = divideX[parent]; hitY = divideY[parent];
        divideDeactivated = false;
        if (nodeDepth(parent) >= DIVIDE_LEVELS) {
            divideActive &= ~(1 << parent);
            divideDead |= 1 << parent;
            divideAlive = divideActive;
            halfWant[parent] = -1;
            halfHurt[parent] = 1f;
            divideDeactivated = true;
            int result = damage(1f);
            if (divideActive == 0 && !beaten) result = damage(hp);
            pinchNode = -1; pinchStart = 0f;
            return true;
        }
        if (right >= DIVIDE_NODES) return false;
        float px = divideX[parent], py = divideY[parent], speed = 95f + 35f * (DIVIDE_LEVELS - nodeDepth(parent));
        float ux = 1f, uy = 0f;
        if (!Float.isNaN(x1)) {
            float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 1e-3f) { ux = dx / len; uy = dy / len; }
        }
        divideActive &= ~(1 << parent);
        divideActive |= (1 << left) | (1 << right);
        divideAlive = divideActive;
        divideX[left] = px - ux * pieceRadiusNode(parent, null) * 0.35f;
        divideY[left] = py - uy * pieceRadiusNode(parent, null) * 0.35f;
        divideX[right] = px + ux * pieceRadiusNode(parent, null) * 0.35f;
        divideY[right] = py + uy * pieceRadiusNode(parent, null) * 0.35f;
        divideVX[left] = divideVX[parent] - ux * speed; divideVY[left] = divideVY[parent] - uy * speed;
        divideVX[right] = divideVX[parent] + ux * speed; divideVY[right] = divideVY[parent] + uy * speed;
        Softbody source = divideBody[parent];
        divideBody[left] = new Softbody(Softbody.NODES, left + 37);
        divideBody[right] = new Softbody(Softbody.NODES, right + 37);
        float rr = pieceRadiusNode(left, null);
        divideBody[left].reset(divideX[left], divideY[left], rr, 1f);
        divideBody[right].reset(divideX[right], divideY[right], rr, 1f);
        divideBody[left].jiggle = divideBody[right].jiggle = JIGGLE[SPLITTER] * 1.8f;
        divideBody[left].squash(-0.8f); divideBody[right].squash(-0.8f);
        divideBody[parent] = null; halfWant[parent] = -1;
        pieceHits[left] = pieceHits[right] = 0; halfIdle[left] = halfIdle[right] = 0f;
        if (rnd != null) { rerollPiece(left, rnd); rerollPiece(right, rnd); }
        else { halfWant[left] = (parent + 1) % Glyph.COUNT; halfWant[right] = (parent + 4) % Glyph.COUNT; }
        divideLevel = Math.max(divideLevel, nodeDepth(left));
        divideSplits++;
        halfHurt[left] = halfHurt[right] = 1f;
        divided = true; divideHits = 0; divideBurst = hurt = 1f;
        pinchNode = -1; pinchStart = 0f;
        return true;
    }

    /** Early volleys use shorter waits; later fragments keep their split-rate ramp. */
    float divideBoltInterval() {
        if (divideSplits == 0) return DIVIDE_BOLT_TIME * 0.5f;
        if (divideSplits == 1) return DIVIDE_BOLT_TIME * 0.7f;
        float progress = Math.min(DIVIDE_PIECES - 1, Math.max(0, divideSplits))
                / (float) (DIVIDE_PIECES - 1);
        return DIVIDE_BOLT_TIME / (1f + 0.5f * progress);
    }

    void endPinch() {
        pinchStart = 0f; pinchNode = -1;
        pinchX1 = pinchY1 = pinchX2 = pinchY2 = Float.NaN;
    }

    float pieceX(int ordinal, Layout L) { int n = pieceNode(ordinal); return n < 0 ? bodyX(L) : divideX[n]; }
    float pieceY(int ordinal, Layout L) { int n = pieceNode(ordinal); return n < 0 ? bodyY(L) : divideY[n]; }
    float pieceR(int ordinal, Layout L) { int n = pieceNode(ordinal); return n < 0 ? 0f : pieceRadiusNode(n, L); }
    float halfX(int i, Layout L) { return pieceX(i, L); }
    float halfY(Layout L) { return bodyY(L); }
    float halfR(Layout L) { return bodyR(L) * 0.72f; }

    private float pieceRadiusNode(int n, Layout L) {
        float root = L == null ? (lastBR > 0f ? lastBR : body == null ? 1f : body.radiusY()) : bodyR(L);
        int depth = nodeDepth(n);
        return root * 2f * (depth == 0 ? 1f : 0.62f * (float) Math.pow(0.72f, depth - 1));
    }

    int divideVolleySize() { return divideSplits == 0 ? 3 : divideSplits == 1 ? 2 : 1; }

    private boolean divideVolley(int node, Layout L, Random rnd) {
        int made = 0, count = divideVolleySize(), free = 0;
        for (boolean live : blive) if (!live) free++;
        if (free < count) return false;
        for (int i = 0; i < count; i++) {
            int bolt = singleBolt(rnd, divideX[node], divideY[node] + pieceRadiusNode(node, L));
            if (bolt < 0) break;
            bt[bolt] = -BOLT_STAGGER * i;
            divideRecoil(node, bolt, L);
            made++;
        }
        return made > 0;
    }

    private void divideRecoil(int node, int bolt, Layout L) {
        Softbody piece = divideBody[node];
        if (piece == null) return;
        float dx = Roster.keyX(L, bglyph[bolt], rosterFull ? 1f : 0f) - bsx[bolt];
        float dy = Roster.keyY(L, bglyph[bolt], rosterFull ? 1f : 0f) - bsy[bolt];
        // Rebound toward home without adding speed to the cube's roaming trajectory.
        piece.shove(-dx, -dy, 1.8f / divideVolleySize());
        piece.squash(0.28f / divideVolleySize());
    }

    private int singleBolt(Random rnd, float x, float y) {
        int slot = -1;
        for (int i = 0; i < MAX_BOLTS; i++) if (!blive[i]) { slot = i; break; }
        if (slot < 0) return -1;
        int glyph;
        if (kind == OCTOPUS) {
            int available = octoKeysLeft();
            if (available == 0) return -1;
            int pick = rnd.nextInt(available);
            glyph = -1;
            for (int g = 0; g < Glyph.COUNT; g++) {
                if (Roster.active(rosterFull, g) && !keyDisabled(g) && pick-- == 0) {
                    glyph = g;
                    break;
                }
            }
        } else glyph = randomGlyph(rnd);
        blive[slot] = true;
        bglyph[slot] = glyph;
        bhp[slot] = bhpMax[slot] = 1;
        bt[slot] = 0f;
        bsx[slot] = x;
        bsy[slot] = y;
        launchT = LAUNCH_TIME;
        launched = true;
        return slot;
    }

    /**
     * A press swatting a bolt. Sets {@link #hitX} to where it was, so the bullet the caller fires
     * plays back at the right spot.
     *
     * @return {@link #PARRY} when one was taken, {@link #NONE} otherwise
     */
    private int swat(int g, Layout L) {
        int i = boltNearest(g);
        if (i < 0) return NONE;
        hitX = boltX(i, L);
        hitY = boltY(i, L);
        bhp[i]--;
        boltDestroyed = bhp[i] <= 0;
        if (boltDestroyed) blive[i] = false;
        return PARRY;
    }

    /** Ticks the volley. Returns the number that reached the deck this frame, each costing a life. */
    private int ageBolts(float dt) {
        int landed = 0;
        for (int i = 0; i < MAX_BOLTS; i++) {
            if (!blive[i]) continue;
            bt[i] += dt / BOLT_TIME;
            if (bt[i] < 1f) continue;
            blive[i] = false;
            landed++;
        }
        return landed;
    }

    private void clearBolts() {
        for (int i = 0; i < MAX_BOLTS; i++) {
            blive[i] = false;
            bt[i] = 0f;
            bhp[i] = bhpMax[i] = 0;
        }
    }

    /**
     * The body, as a soft body: a ring of sprung nodes under pressure, so a boss is a squishy thing
     * that wobbles when it moves and dents when it is hit rather than a hexagon that changes colour.
     *
     * Every boss gets one, not just the slime. They are all soft creatures, the sim costs one ring of
     * eighteen nodes a frame, and a hit that visibly deforms the thing you hit is worth more than any
     * amount of flashing. Seeded off {@link #kind} so the five do not breathe in step.
     *
     * Null when there is no boss, which is also how the renderer knows there is nothing to draw.
     */
    Softbody body;
    /** Independent sprung stalk for the fly agaric; the cap remains {@link #body}. */
    Softbody mushroomStem;
    private boolean mushroomStemPlaced;
    /** False until the body has been placed, which cannot happen until a {@link Layout} is in hand. */
    private boolean bodyPlaced;

    /** Takes a key, never the one it is showing — that pairing would be a deadlock. */

    private void updateDivide(float dt, Layout L) {
        boingWeight = -1f;
        if (!dividePlaced) {
            float x = bodyX(L), y = bodyY(L), r = pieceRadiusNode(0, L);
            body.reset(x, y, r, 1f);
            body.jiggle = JIGGLE[SPLITTER];
            divideBody[0] = body;
            divideX[0] = x; divideY[0] = y;
            divideVX[0] = r * 0.72f; divideVY[0] = r * 0.38f;
            dividePlaced = bodyPlaced = true;
        }
        float step = Math.min(dt, 0.05f);
        float top = Boss.restY(L), bottom = L.dangerY;
        int visible = divideActive | divideDead;
        int visibleCount = Integer.bitCount(visible);
        for (int n = 0; n < DIVIDE_NODES; n++) {
            if (!nodeVisible(n)) continue;
            Softbody pb = divideBody[n];
            if (pb == null) continue;
            float r = pieceRadiusNode(n, L);
            if (beaten) {
                int ordinal = Integer.bitCount(visible & ((1 << n) - 1));
                DivideDeath.pose(this, n, ordinal, Math.max(1, visibleCount), dt, L);
                continue;
            }
            if (n != pinchNode) {
                divideX[n] += divideVX[n] * step;
                divideY[n] += divideVY[n] * step;
                boolean bounced = false;
                if (divideX[n] < L.playLeft + r) { divideX[n] = L.playLeft + r; divideVX[n] = Math.abs(divideVX[n]); bounced = true; }
                if (divideX[n] > L.playRight - r * 1.32f) { divideX[n] = L.playRight - r * 1.32f; divideVX[n] = -Math.abs(divideVX[n]); bounced = true; }
                if (divideY[n] < top + r * 0.5f) { divideY[n] = top + r * 0.5f; divideVY[n] = Math.abs(divideVY[n]); bounced = true; }
                if (divideY[n] > bottom - r) { divideY[n] = bottom - r; divideVY[n] = -Math.abs(divideVY[n]); bounced = true; }
                if (bounced) {
                    pb.squash(0.34f);
                    boingWeight = Math.max(boingWeight, 1f - nodeDepth(n) / (float) DIVIDE_LEVELS);
                }
            }
        }
        for (int a = 0; a < DIVIDE_NODES; a++) {
            if (beaten || !nodeVisible(a)) continue;
            for (int b = a + 1; b < DIVIDE_NODES; b++) {
                if (!nodeVisible(b)) continue;
                float dx = divideX[b] - divideX[a], dy = divideY[b] - divideY[a];
                float d2 = dx * dx + dy * dy;
                float reach = pieceRadiusNode(a, L) + pieceRadiusNode(b, L);
                if (d2 >= reach * reach) continue;
                float d = (float) Math.sqrt(Math.max(1f, d2));
                float ux = dx / d, uy = dy / d, overlap = reach - d;
                divideX[a] -= ux * overlap * 0.5f; divideY[a] -= uy * overlap * 0.5f;
                divideX[b] += ux * overlap * 0.5f; divideY[b] += uy * overlap * 0.5f;
                float va = divideVX[a] * ux + divideVY[a] * uy;
                float vb = divideVX[b] * ux + divideVY[b] * uy;
                if (vb < va) {
                    float kick = (va - vb) * 0.88f + overlap * 4f;
                    divideVX[a] -= ux * kick; divideVY[a] -= uy * kick;
                    divideVX[b] += ux * kick; divideVY[b] += uy * kick;
                    divideBody[a].impulse(divideX[a] + ux * pieceRadiusNode(a, L), divideY[a], 0.45f);
                    divideBody[b].impulse(divideX[b] - ux * pieceRadiusNode(b, L), divideY[b], 0.45f);
                    boingWeight = Math.max(boingWeight, 1f - Math.min(nodeDepth(a), nodeDepth(b)) / (float) DIVIDE_LEVELS);
                }
            }
        }
        for (int n = 0; n < DIVIDE_NODES; n++) {
            if (!nodeVisible(n) || divideBody[n] == null) continue;
            Softbody pb = divideBody[n];
            pb.jiggle = JIGGLE[SPLITTER] * (1f + halfHurt[n] * 0.8f);
            if (n == pinchNode && !Float.isNaN(pinchX1)) {
                divideX[n] = (pinchX1 + pinchX2) * 0.5f;
                divideY[n] = (pinchY1 + pinchY2) * 0.5f;
            }
            pb.moveTo(divideX[n], divideY[n]);
            pb.update(dt);
            if (n == pinchNode && !Float.isNaN(pinchX1))
                pb.encompass(pinchX1, pinchY1, pinchX2, pinchY2);
        }
    }

    /** Returns how many visible boss threats reached the deck this frame. */
    int update(float dt, Layout L, Random rnd) {
        if (kind < 0) return 0;
        hurt = Math.max(0f, hurt - dt * 2.6f);
        for (int g = 0; g < slimeBlobPulse.length; g++)
            slimeBlobPulse[g] = Math.max(0f, slimeBlobPulse[g] - dt);
        slimeDragPulse = Math.max(0f, slimeDragPulse - dt);
        slimeKeyPulse = Math.max(0f, slimeKeyPulse - dt);
        slimeKeyLock = Math.max(0f, slimeKeyLock - dt);
        octoWave = octoCue = octoLock = mushroomShakeCue = mushroomSporeCue = false;
        divideBurst = Math.max(0f, divideBurst - dt * 1.35f);
        for (int i = 0; i < halfHurt.length; i++)
            halfHurt[i] = Math.max(0f, halfHurt[i] - dt * 3.4f);
        launchT = Math.max(0f, launchT - dt);
        launched = false;
        rage = Math.max(0f, rage - dt * 2.2f);
        defeatChime = false;

        // Above both early returns below, and that is not tidiness. An element has no position until
        // this has run, so laying out after the intro return meant that on the very frame the
        // arrival card ended — the first frame a tap is accepted — every element was still sitting at
        // the origin, and a tap on one hit nothing. It also lets the card draw them, which is how the
        // heads and the drum skin arrive with the body instead of appearing after it.
        // Before the layout, because the layout reads bodyX/bodyY and those include the follow.
        updateFollow(dt, L);
        updateReturning(dt, L);
        layoutElems(L);

        // The body follows wherever the layout put the boss, and keeps wobbling on the way out — a
        // burst that starts from a frozen shape reads as two separate animations.
        if (kind == SPLITTER) updateDivide(dt, L);
        if (body != null && kind != SPLITTER) {
            if (!bodyPlaced) {
                body.reset(bodyX(L), bodyY(L), bodyR(L), wide());
                body.jiggle = JIGGLE[kind];
                bodyPlaced = true;
            } else {
                float bounce = 0f;
                if (kind == OCTOPUS && octoVulnerableArm >= 0) {
                    float urgency = Math.min(1f, octoDragTime / 2f);
                    // Slow enough for the pressurised body to follow, broad enough to read.
                    bounce = (float) Math.sin(octoDragTime * (4.2f + urgency * 1.8f))
                            * bodyR(L) * (0.34f + urgency * 0.24f);
                }
                body.moveTo(bodyX(L), bodyY(L) + bounce);
            }
            // A glob being hauled out stretches the skin after it, like pulling at something in
            // treacle. Re-aimed every frame at wherever the finger has got to. At rest, the wart keeps a smaller
            // outward constraint so it remains a real protrusion; release only drops the long stretch.
            //
            // With the glob's own radius handed over, so the promise the solver keeps is that the
            // whole glob stays inside the body rather than that its centre does. Between that and
            // updateFollow above, a glob never leaves the goo it came out of until it is off the
            // screen.
            if (beaten) {
                // Shared death morph: gravity wins while the body is carried toward the player.
                // Pulling below its travelling centre makes the silhouette neck, sag and melt.
                float melt = defeatMelt();
                if (kind == OCTOPUS || kind == MUSHROOM) body.letGo();
                else if (melt > 0f) body.pull(body.centreX(), L.h + bodyR(L) * 2.5f,
                        0.11f + melt * 0.29f);
                body.jiggle = kind == MUSHROOM ? JIGGLE[kind] * (1f - leaveProgress())
                        : kind == SLIME ? 0.72f + melt * 0.16f
                        : JIGGLE[kind] * (1f + melt * 1.3f);
            } else if (held >= 0 && etype[held] == E_GLOB) {
                body.pull(ex[held], ey[held], PULL_K, er[held]);
            } else {
                int wart = globIndex();
                if (wart >= 0) body.pull(ex[wart], ey[wart], WART_PULL, er[wart] * WART_SKIN);
                else body.letGo();
            }
            if (kind == SLIME && !beaten)
                body.jiggle = JIGGLE[kind] * (1f + (1f - health()) * 1.25f);
            body.update(dt);
            if (kind == MUSHROOM && mushroomStem != null) {
                float sr = bodyR(L) * 1.08f;
                float sx = bodyX(L), sy = bodyY(L) + bodyR(L) * 0.92f;
                if (!mushroomStemPlaced) {
                    mushroomStem.reset(sx, sy, sr, 0.46f);
                    mushroomStem.jiggle = JIGGLE[kind] * 0.72f;
                    mushroomStemPlaced = true;
                } else mushroomStem.moveTo(sx, sy);
                float capX = body.centreX() + mushroomCapDX;
                float capY = body.centreY() + mushroomCapDY + body.radiusY() * 0.24f;
                if (held == -2) mushroomStem.pull(capX, capY, 0.72f);
                else mushroomStem.letGo();
                mushroomStem.update(dt);
            }
            if (beaten && kind != OCTOPUS && kind != MUSHROOM) {
                float from = defeatStartW > 0f ? defeatStartW : body.spanX();
                body.fitWidth(from + (L.w * 0.90f - from) * defeatStretch());
            }
        }

        // Above both early returns: a volley already in the air still arrives. Cleared on the frame
        // the boss is beaten, so nothing lands after the burst.
        int hits = ageBolts(dt);

        if (beaten) {
            leaveT = Math.max(0f, leaveT - dt);
            if (kind == MUSHROOM) {
                mushroomCapDX *= Math.max(0f, 1f - dt * 8f);
                mushroomCapDY *= Math.max(0f, 1f - dt * 8f);
                mushroomSweepFlash = Math.max(0f, mushroomSweepFlash - dt);
                mushroomAngry = mushroomCharge = mushroomReject = mushroomMeterAlpha = 0f;
                for (int i = 0; i < MUSHROOM_DUST; i++)
                    mushroomDustLife[i] = Math.max(0f, mushroomDustLife[i] - dt);
            }
            if (kind == OCTOPUS) poseOctopus(dt, L);
            float p = leaveProgress();
            int wantBeat = p >= 0.28f ? 3 : p >= 0.16f ? 2 : p >= 0.05f ? 1 : 0;
            if (defeatBeat < wantBeat) {
                defeatBeat++;
                defeatChime = true;
                if (body != null && kind != MUSHROOM) {
                    float kick = defeatBeat % 2 == 0 ? -0.42f : 0.58f;
                    body.squash(kind == SLIME ? kick * 0.38f : kick);
                }
            }
            return hits;
        }
        if (intro > 0f) {
            intro = Math.max(0f, intro - dt);
            return hits;
        }

        age += dt;
        if (kind == MUSHROOM) {
            for (int i = 0; i < MUSHROOM_DUST; i++) {
                if (mushroomDustLife[i] <= 0f) continue;
                mushroomDustLife[i] = Math.max(0f, mushroomDustLife[i] - dt);
                mushroomDustX[i] += mushroomDustVX[i] * dt;
                mushroomDustY[i] += mushroomDustVY[i] * dt;
                mushroomDustVY[i] += bodyR(L) * 1.8f * dt;
            }
            mushroomSweepFlash = Math.max(0f, mushroomSweepFlash - dt);
            mushroomReject = Math.max(0f, mushroomReject - dt * 1.15f);
            if (held == -2 && mushroomMeterAlpha > 0f) {
                mushroomMeterAlpha = Math.min(1f, mushroomMeterAlpha + dt * 5f);
                float step = dt * 3.2f * mushroomRate();
                if (mushroomGuideX < mushroomGuideTarget)
                    mushroomGuideX = Math.min(mushroomGuideTarget, mushroomGuideX + step);
                else mushroomGuideX = Math.max(mushroomGuideTarget, mushroomGuideX - step);
            } else if (held != -2) mushroomMeterAlpha = Math.max(0f, mushroomMeterAlpha - dt * 7f);
            if (held != -2) {
                float settle = Math.max(0f, 1f - dt * 8f);
                mushroomCapDX *= settle;
                mushroomCapDY *= settle;
            }
            if (mushroomShakeWindow > 0f) {
                mushroomShakeWindow = Math.max(0f, mushroomShakeWindow - dt);
                if (mushroomShakeWindow == 0f) {
                    mushroomShakes = mushroomDirection = 0;
                    if (held == -2) mushroomLastX = body == null ? 0f : body.centreX();
                }
            }
            if (mushroomAngry > 0f) {
                mushroomAngry = Math.max(0f, mushroomAngry - dt);
                if (mushroomAngry <= MUSHROOM_REACTION_RELEASE && mushroomReaction) {
                    mushroomReaction = mushroomShakeCue = mushroomSporeCue = false;
                    sporeVolley(3, rnd);
                    shedMushroomDust(L.w * 0.3f, L);
                    shedMushroomDust(-L.w * 0.3f, L);
                }
            } else if (mushroomCharge > 0f) {
                mushroomCharge = Math.max(0f, mushroomCharge - dt);
                if (mushroomCharge == 0f) {
                    sporeVolley(4, rnd);
                    mushroomAttackT = MUSHROOM_ATTACK_GAP;
                }
            } else {
                mushroomAttackT -= dt * mushroomRate();
                if (mushroomAttackT <= 0f) {
                    mushroomCharge = MUSHROOM_CHARGE_TIME;
                    if (body != null) body.squash(1f);
                }
            }
        }

        ageElems(dt);
        if (kind == OCTOPUS) updateOctopus(dt, L, rnd);

        if (kind == SLIME && slimeRetaliating && boltCount() == 0) {
            float home = bodyR(L) * 0.02f;
            if (followX * followX + followY * followY <= home * home) volley(rnd);
        } else if (kind == SLIME && age >= CYCLE[SLIME]-SHOW[SLIME]
                && boltCount() == 0 && !hasGlob() && open()) {
            promptT -= dt;
            if (promptT <= 0f) volley(rnd);
        }

        if (kind == SPLITTER) {
            float interval = divideBoltInterval();
            for (int n = 0; n < DIVIDE_NODES; n++) {
                if (!nodeActive(n)) continue;
                halfIdle[n] += dt;
                if (halfIdle[n] >= interval) {
                    if (divideVolley(n, L, rnd)) halfIdle[n] -= interval;
                }
            }
        }

        float cycle = CYCLE[kind];
        phase += dt;
        if(kind==SLIME && slimePromptHits>=2 && phase>=CYCLE[SLIME]-SLIME_PROMPT_TRANSITION)
            slimeCoverLearned=true;
        if (phase >= cycle) phase -= cycle;

        return hits;
    }

    /** Where every live element is this frame. Read by both the hit-test and the renderer. */
    private void layoutElems(Layout L) {
        float bx = bodyX(L), by = bodyY(L), br = bodyR(L);
        lastBX = bx;
        lastBY = by;
        lastBR = br;
        for (int i = 0; i < ELEMS; i++) {
            if (etype[i] == E_GLOB) place(i, bx, by, br, L);
        }
        // Anything a finger has moved stays moved, so the layout pass does not drag it home again.
        if (held >= 0) moved[held] = true;
    }

    /** Ticks the elements that expire, and applies what expiring costs. */
    private void ageElems(float dt) {
        for (int i = 0; i < ELEMS; i++) {
            if (etype[i] == E_OFF || elife[i] <= 0f) continue;
            // A held element does not tick: a finger on it is holding it still, and a glob that
            // dissolved out from under the drag that was winning would read as the game cheating.
            if (held == i) continue;
            elife[i] -= dt;
            if (elife[i] > 0f) continue;

            clearElem(i);
        }
    }
}
