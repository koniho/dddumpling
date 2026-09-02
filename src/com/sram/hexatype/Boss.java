package com.sram.hexatype;

import java.util.Random;

/**
 * The boss encounter: every fifth stage, in place of that stage's wave.
 *
 * Five of them, one mechanic each, cycling in order so a run meets them in the same sequence it met
 * them last time — the first is the one that teaches the frame, and the frame is what all five
 * share:
 *
 * <ul>
 *   <li>A boss <em>arrives</em> ({@link #INTRO}), announcing itself over an empty field.
 *   <li>It then alternates a shut phase with an <em>open</em> one. It can only be hurt while open,
 *       and while open it claims the letters it is asking for — see {@link #wants}.
 *   <li>Nothing else is on the field. A boss stage releases no words at all: the fight is the
 *       stage, and it gets the screen to itself.
 *   <li>The stage does not end until it is beaten. There is no way past it.
 * </ul>
 *
 * <h2>Where the threat comes from</h2>
 *
 * A boss stage used to run a thin wave underneath the fight, and that wave was quietly carrying two
 * jobs nobody had written down: it was the only thing that could hurt you, and — since enraging
 * worked by speeding the spawns up — it was the only reason to hurry. Taking the words away left
 * every boss but {@link #SUMO} completely harmless, which turns "must be beaten" into "may be poked
 * at indefinitely".
 *
 * Each boss's own mechanic now supplies its pressure. A dragging fight still reddens after
 * {@link #ENRAGE_AT}, but elapsed time alone never costs a life.
 *
 * <h2>Three ways in</h2>
 *
 * No boss can be beaten by typing alone. Each one asks for at least two of the three things a
 * player can do here — press a key, tap something, drag something — because the six keys on their
 * own cannot express five different fights, and because a set piece that is only the ordinary verb
 * at a higher rate is a wave, not a boss.
 *
 * Taps and drags land on {@link #elems} <em>elements</em>: hit-testable things the boss puts on the
 * field, whose positions are computed once per frame in {@link #update} and read by both the
 * hit-test and the renderer, so the two cannot disagree about where they are.
 *
 * Elements live in the <em>upper</em> field, and that is a hard constraint rather than a
 * preference. A drag may not start on a key (see CLAUDE.md: telling a drag from a tap means
 * holding the tap back, and every tap here is a keystroke), and the panic swipe already owns the
 * lower half. The upper field is the one part of the screen where a touch is unambiguous, which is
 * what lets a boss element commit to a drag on the frame the finger lands — exactly as the display
 * case's position bar does, and for the same reason.
 *
 * <h2>Why the window exists</h2>
 *
 * The window is what makes press precedence tractable. Six keys have to address both the boss and
 * the words underneath it, and the answer is that an engaged word always outranks the boss (exactly
 * as it outranks the drifting powerup) while an open boss outranks an <em>unengaged</em> word for
 * the letters it is asking for. Without the window that denial would be permanent and a stage could
 * be unwinnable; with it, it is a couple of seconds of the boss demanding attention.
 */
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
    /**
     * Three heads, asleep until <em>tapped</em> awake, then struck as one chord inside a window.
     * Two thumbs and a spare finger.
     */
    static final int TRIPLETS = 1;
    /**
     * A drum on a fixed beat, alternately wanting a key press and a <em>tap</em> on its skin.
     * Pressing early does not merely miss — it resets the beat, so the beat cannot be mashed.
     */
    static final int DRUM = 2;
    /**
     * It holds one of your six keys hostage. A press on its belly makes it drop the key, which then
     * has to be <em>dragged</em> back down to the deck before it is snatched again.
     */
    static final int MAGPIE = 3;
    /**
     * It sinks toward the danger line and has to be <em>swiped</em> back. A press on its belt banks
     * the swipe and staggers it, and a staggered shove hits twice as hard.
     */
    static final int SUMO = 4;
    static final int COUNT = 5;

    static final String[] NAMES = {"SLIME", "TRIPLETS", "MOCHI DRUM", "MAGPIE", "SUMO BUN"};
    /** One line each, in the mode bar. Held to the width of the longest frenzy blurb. */
    static final String[] BLURB = {"HIT THE MARK, DRAG GLOBS", "TAP THEM AWAKE FIRST",
            "KEY, THEN TAP, ON BEAT", "DRAG YOUR KEY BACK", "SWIPE IT BACK"};
    /**
     * Which of the six characters each boss is a giant version of.
     *
     * Reusing {@link Kawaii} rather than drawing five new creatures is not only cheap: a boss that
     * is plainly an enormous one of the things you have been typing all game is funnier, and it
     * arrives already legible.
     */
    static final int[] FACE = {Kawaii.SQUISHY, Kawaii.GRAPES, Kawaii.DUMPLING, Kawaii.CAT,
            Kawaii.BLOB};

    /** How long the arrival card holds the field before the fight starts. */
    static final float INTRO = 1.6f;
    /** How long the burst takes once it is beaten, before the stage may end. */
    static final float LEAVE = 1.1f;

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

    /**
     * Seconds of one open/shut cycle, and how many of those seconds the window is open for.
     *
     * The ratio is the boss's whole character. SLIME is open most of the time because its mechanic
     * is a long chain that needs room to run; DRUM is barely open at all because its mechanic
     * <em>is</em> the window. SUMO has no press window — see {@link #open()}, which answers a
     * different question for it.
     */
    private static final float[] CYCLE = {5.0f, 3.6f, 1.20f, 3.2f, 0f};
    private static final float[] SHOW = {4.0f, 3.6f, 0.40f, 2.0f, 0f};

    /**
     * How long a {@link #TRIPLETS} chord may take from its first head to its last.
     *
     * This boss is the one exception to the open/shut rhythm: its window above is its whole cycle,
     * so it is permanently open, and this is what stands in for the window instead.
     *
     * That is not a shortcut, it is the fix for the two rules fighting. A chord had to land inside
     * one window, and an engaged word outranks the boss — so whenever a window opened while a minion
     * was part-typed, the chord could not be started at all, and the player watched the window go by.
     * The soak bot got eight windows and two chords out of a whole fight. Timing the chord from its
     * own first press instead means a chord can straddle whatever else is going on, and "all three at
     * once" becomes a rule about the three presses rather than a rule about the clock they happen to
     * fall under.
     */
    static final float CHORD_TIME = 2.0f;

    /**
     * Hits needed to beat each boss on its first visit.
     *
     * Not comparable between bosses: a SLIME hit is a whole glob carried off the screen and a SUMO
     * hit is a swipe paid for with presses, so these are counted in each boss's own currency and
     * balanced against how long its window is and how often one comes round.
     *
     * <p>Every one of them is set against one figure: a competent player should finish the fight
     * inside about fifteen of the {@link #ENRAGE_AT} seconds before it turns nasty. That is what
     * these numbers are, and the first draft of them ignored it — {@code TRIPLETS} wanted six chords
     * at one window per 4.5s, which is 27 seconds of flawless play against a 22-second fuse, so even
     * a perfect run lost it. Health times cycle length is the figure to check, never health alone.
     *
     * <p>{@link #SLIME} is counted in globs carried off, and that is the whole reason its number is
     * the smallest here: one point of its health is {@link #SPLIT_HITS} presses of its chain
     * <em>and</em> a drag to the edge of the screen, where every other boss's is a press or a swipe.
     * Four of those is twenty presses with four drags threaded through them, which the steady soak
     * hand finishes in about eight seconds — see the per-boss timings {@code TestBoss.winning} prints,
     * which are the figures to read this table against.
     */
    private static final float[] HP = {4f, 4f, 7f, 5f, 3f};
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
    /** Accepted, but the boss is not hurt yet — one head of a chord, or a glob picked up. */
    static final int PART = 2;
    /** The boss took the input and refused it: right thing, wrong moment, or a held key. */
    static final int REBUFF = 3;
    /** A bolt the boss threw was swatted out of the air. Scores; does not hurt the boss. */
    static final int PARRY = 4;

    /** The most swipes {@link #SUMO} will bank. Earned by pressing its belt. */
    static final int CHARGE_MAX = 3;
    /**
     * How far down {@link #SUMO} has to have sunk before a swipe can reach it, and how long it takes
     * to sink the whole way. Reaching the bottom costs a life and puts it back at the top.
     */
    static final float SHOVE_REACH = 0.35f, SINK_TIME = 5.5f;
    /** How long a staggered {@link #SUMO} stays staggered, and what a shove is worth then. */
    static final float STAGGER_TIME = 3f;
    static final float STAGGER_BONUS = 2f;

    // ---- elements -----------------------------------------------------------
    /** Most touch elements one boss puts on the field at once. */
    static final int ELEMS = 3;
    /** No element in this slot. */
    static final int E_OFF = 0;
    /** A {@link #SLIME} glob: drag it off the play area or it crawls back. */
    static final int E_GLOB = 1;
    /** A {@link #TRIPLETS} head: tap it awake, then press its letter. */
    static final int E_HEAD = 2;
    /** The {@link #DRUM} skin: tap it on the beats that want a tap. */
    static final int E_SKIN = 3;
    /** A key {@link #MAGPIE} dropped: drag it down to the deck to get it back. */
    static final int E_KEY = 4;

    /** Element type per slot, {@link #E_OFF} when empty. */
    final int[] etype = new int[ELEMS];
    /** Element centre and radius in view pixels, recomputed every frame by {@link #update}. */
    final float[] ex = new float[ELEMS];
    final float[] ey = new float[ELEMS];
    final float[] er = new float[ELEMS];
    /** Per-element life: what a glob has left before it re-merges, or a dropped key before it goes. */
    final float[] elife = new float[ELEMS];
    /** Which element a finger is currently holding, or -1. */
    int held = -1;

    /** How long a shed glob survives untouched, and a dropped key. */
    static final float GLOB_TIME = 5f, KEY_TIME = 4.5f;
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
    float hp, hpMax;
    /** Seconds the fight has been running, and of the arrival card and the exit. */
    float age, intro, leaveT;
    /** Counts up through the open/shut cycle, wrapping at {@link #CYCLE}. */
    float phase;
    /** Decaying flashes: hurt on damage, rage on a rebuff. Separate channels, on purpose. */
    float hurt, rage;
    boolean beaten;

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
    /** The three letters {@link #TRIPLETS} is showing, which are awake, and which this window took. */
    private final int[] head = new int[3];
    private int awake, chord;
    /** Seconds left to finish the chord in progress, or 0 with none started. */
    float chordT;
    /** The single letter {@link #DRUM}, {@link #MAGPIE} and {@link #SUMO} want. */
    private int want = -1;
    /** {@link #DRUM}: true when this beat wants a tap on the skin rather than a key press. */
    boolean tapBeat;
    /** The key {@link #MAGPIE} is holding, or -1. Never equal to {@link #want}. */
    int stolen = -1;
    /** {@link #SUMO}: swipe charges banked, how far it has sunk, and how long it stays staggered. */
    int charges;
    float depth, stagger;
    /** {@link #MAGPIE}: seconds until it takes another key, while it is empty-handed. */
    float stealT;

    /** Which boss stage {@code stage} is, or -1 if it is an ordinary one. */
    static int kindFor(int stage) {
        if (stage <= 0 || stage % EVERY != 0) return -1;
        return (stage / EVERY - 1) % COUNT;
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

    /** 0..1 of how wound up the enrage is; 0 until {@link #ENRAGE_AT}. */
    float enrage() {
        if (!fighting() || age <= ENRAGE_AT) return 0f;
        float t = (age - ENRAGE_AT) / ENRAGE_RAMP;
        return t > 1f ? 1f : t;
    }


    /**
     * Where the last accepted press landed on the boss, in view coordinates.
     *
     * Recorded rather than worked out afterwards, because by the time the caller wants it the thing
     * that was struck may have moved on — a chord's third head is rerolled the instant it completes,
     * so asking "which head shows that letter" a moment later gets a different answer or none. Same
     * shape as the MULTI chain keeping its hop positions.
     */
    float hitX, hitY;

    /** 0..1 through the arrival card. */
    float introProgress() {
        return intro <= 0f ? 1f : 1f - intro / INTRO;
    }

    /** 0..1 through the exit. */
    float leaveProgress() {
        return leaveT <= 0f ? 1f : 1f - leaveT / LEAVE;
    }

    String name() {
        return kind < 0 ? "" : NAMES[kind];
    }

    void begin(int which, int stage, Random rnd) {
        kind = which;
        int visit = Math.max(0, Math.min(TOUGH_MAX, stage / EVERY - 1));
        hpMax = HP[which] + TOUGH * visit;
        hp = hpMax;
        age = 0f;
        intro = INTRO;
        leaveT = 0f;
        // Starts shut, so the first thing a boss does is arrive rather than be vulnerable. The
        // opening breather is also where the blurb gets read.
        phase = 0f;
        hurt = rage = 0f;
        beaten = false;
        awake = chord = 0;
        charges = 0;
        depth = 0f;
        stagger = 0f;
        stealT = 0f;
        tapBeat = false;
        want = -1;
        stolen = -1;
        held = -1;
        chordT = 0f;
        chainAt = 0;
        split = 0;
        promptT = PROMPT_MAX;
        launchT = 0f;
        launched = false;
        followX = followY = 0f;
        clearBolts();
        // Seeded off the kind, so the five bosses do not all breathe on the same phase. Placed on
        // the first update, which is the first time there is a Layout to place it in.
        body = new Softbody(Softbody.NODES, which + 1);
        bodyPlaced = false;
        for (int i = 0; i < ELEMS; i++) {
            etype[i] = E_OFF;
            elife[i] = 0f;
        }
        for (int i = 0; i < chain.length; i++) chain[i] = rnd.nextInt(Glyph.COUNT);
        for (int i = 0; i < head.length; i++) head[i] = rnd.nextInt(Glyph.COUNT);
        if (which == DRUM || which == SUMO) want = rnd.nextInt(Glyph.COUNT);
        if (which == MAGPIE) steal(rnd);
        if (which == TRIPLETS) {
            for (int i = 0; i < head.length; i++) etype[i] = E_HEAD;
        }
        if (which == DRUM) etype[0] = E_SKIN;
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
        hurt = rage = 0f;
        beaten = false;
        awake = chord = 0;
        want = -1;
        stolen = -1;
        charges = 0;
        depth = 0f;
        stagger = 0f;
        stealT = 0f;
        tapBeat = false;
        held = -1;
        chordT = 0f;
        chainAt = 0;
        split = 0;
        promptT = PROMPT_MAX;
        launchT = 0f;
        launched = false;
        followX = followY = 0f;
        clearBolts();
        // The body goes too. It is the largest thing a boss puts on the screen, and the renderer
        // reads exactly this to decide whether there is anything to draw at all.
        body = null;
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
    private static final float[] WIDE = {2f, 1f, 1f, 1f, 1f};

    /**
     * How springy each boss is; see {@link Softbody#jiggle}.
     *
     * The slime is twice everything: a hit dents it twice as deep, it breathes twice as far, and what
     * is set going in it rings for twice as long. It can afford that where the others could not,
     * because its own mechanic no longer hits it every second — five presses work a glob loose and
     * only the drag scores, so the body has time to actually finish a wobble.
     */
    private static final float[] JIGGLE = {2f, 1f, 1f, 1f, 1f};

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

    /**
     * Body centre. {@link #SUMO} is the exception: it sinks down the field, so its height is its
     * threat and {@link #depth} owns it.
     */
    float bodyY(Layout L) {
        if (beaten) return defeatY(L);
        return baseY(L) + followY;
    }

    /** Shared defeated-boss route: soften, then melt down toward the player and off screen. */
    float defeatY(Layout L) {
        float t = leaveProgress();
        float melt = t * t * (3f - 2f * t);
        float from = baseY(L) + followY;
        return from + (L.h + bodyR(L) * 2.2f - from) * melt;
    }

    /** Body centre y, before the drag follow. */
    float baseY(Layout L) {
        float top = L.playTop + L.unit * BODY_DROP + bodyR(L);
        if (kind != SUMO) return top;
        float bottom = L.dangerY - bodyR(L) * 0.7f;
        return top + (bottom - top) * depth;
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

    /** The body's resting height, for anything that has to lay out around it. */
    static float restY(Layout L) {
        return L.playTop + L.unit * BODY_DROP + bodyR(L);
    }

    // ---- the window ---------------------------------------------------------

    /**
     * The window: true while the boss can be hurt.
     *
     * {@link #SUMO} answers a different question here — it has no press window at all, and what
     * makes it vulnerable is having sunk far enough down the field for a swipe to reach it. One
     * predicate either way, because everything that reads this only wants to know whether hurting it
     * is possible right now.
     */
    boolean open() {
        if (!fighting()) return false;
        if (kind == SUMO) return depth >= SHOVE_REACH;
        return phase >= CYCLE[kind] - SHOW[kind];
    }

    /** 0..1 through the current window, or through the breather when it is shut. */
    float phaseProgress() {
        if (kind < 0 || kind == SUMO) return 0f;
        float show = SHOW[kind], cycle = CYCLE[kind];
        if (phase >= cycle - show) return show <= 0f ? 1f : (phase - (cycle - show)) / show;
        return cycle - show <= 0f ? 1f : phase / (cycle - show);
    }

    /**
     * True when pressing {@code g} right now would actually be <em>accepted</em> — so this is what
     * the key hint rings, and what any player, human or bot, should read as "press this".
     *
     * Deliberately narrower than {@link #asksFor}, which is about whether the boss <em>claims</em>
     * the press at all. The two differ for exactly one case and it matters: a {@link #TRIPLETS} head
     * that is awake but already struck in this chord is still claimed — pressing it is plainly aimed
     * at the boss and earns a rebuff rather than falling through to a word — but it is no longer
     * being asked for. Conflating them made this method lie, and the lie was expensive: the soak bot
     * read the struck head's letter as the thing to press, pressed it, was rebuffed, and pressed it
     * again. Ten chords started and ten lost in one fight, none of them for want of skill.
     */
    boolean wants(int g) {
        // Whether or not the window is open: a bolt is in the air and the press that clears it has
        // to be advertised, or the key hint tells the player to ignore the only threat on the field.
        if (boltWants(g)) return true;
        if (!open()) return false;
        if (kind == TRIPLETS) return headIndex(g) >= 0;
        return asksFor(g);
    }

    /**
     * True when {@code g} is the key {@link #MAGPIE} is holding. Refused wherever it is pressed,
     * including into a word that needs it — that denial is the mechanic.
     */
    boolean denies(int g) {
        return fighting() && kind == MAGPIE && g == stolen;
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

    /** The letter on head {@code i} of {@link #TRIPLETS}. */
    int head(int i) {
        return kind == TRIPLETS && i >= 0 && i < head.length ? head[i] : -1;
    }

    boolean headAwake(int i) {
        return (awake & (1 << i)) != 0;
    }

    boolean headStruck(int i) {
        return (chord & (1 << i)) != 0;
    }

    /** The one letter {@link #DRUM}, {@link #MAGPIE} or {@link #SUMO} is showing, or -1. */
    int want() {
        return kind == DRUM || kind == MAGPIE || kind == SUMO ? want : -1;
    }

    /** True when a shove would land right now, for the renderer's hint and the view's gesture. */
    boolean shovable() {
        return fighting() && kind == SUMO && charges > 0 && open();
    }

    /** First awake, unstruck head showing {@code g}, or -1. */
    private int headIndex(int g) {
        for (int i = 0; i < head.length; i++) {
            if (head[i] == g && headAwake(i) && !headStruck(i)) return i;
        }
        return -1;
    }

    /**
     * True when {@code g} is a letter this boss would want if its window were open.
     *
     * {@link #TRIPLETS} asks about awake heads only, and deliberately does not care whether one has
     * already been struck this window: a press on a head that is up but spent is plainly a press at
     * the boss, and it wants the rebuff rather than falling through to a word. A head that is still
     * asleep asks for nothing, which is what makes the tap the first half of that mechanic.
     */
    boolean claims(int g) {
        return boltWants(g) || asksFor(g);
    }

    private boolean asksFor(int g) {
        switch (kind) {
            case SLIME: return g == chainLetter();
            case TRIPLETS:
                for (int i = 0; i < head.length; i++) {
                    if (head[i] == g && headAwake(i)) return true;
                }
                return false;
            case DRUM: return !tapBeat && g == want;
            case MAGPIE:
            case SUMO: return g == want;
            default: return false;
        }
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
        if (denies(g)) {
            // It has that key. Nothing happens with it anywhere — this is the refusal, and it is the
            // reason the stolen key is never the letter the boss itself wants.
            rage = 1f;
            return REBUFF;
        }
        if (!asksFor(g)) return NONE;

        // Where a bullet fired at this press should land. The body by default; overridden below by
        // the one boss whose presses land somewhere more specific than "it".
        if (body != null) {
            hitX = body.centreX();
            hitY = body.centreY();
        }

        if (!open()) {
            // Its letter, at the wrong moment.
            rage = 1f;
            if (kind == DRUM) {
                // The beat resets, which is what stops the window being brute-forced: mashing its
                // letter pushes the window further away instead of catching its opening frame.
                phase = 0f;
            }
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
                split++;
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
                return PART;
            }
            case TRIPLETS: {
                int i = headIndex(g);
                if (i < 0) {
                    // An awake head showing this letter has already been taken this window. Refused
                    // rather than passed on: it is plainly a press at the boss, just a wasted one.
                    rage = 1f;
                    return REBUFF;
                }
                // The first head of a chord starts its clock; the rest have to beat it.
                if (chord == 0) chordT = CHORD_TIME;
                // The head actually struck, before the reroll below can move it.
                hitX = ex[i];
                hitY = ey[i];
                chord |= 1 << i;
                if (chord != (1 << head.length) - 1) return PART;
                chordT = 0f;
                // All three, awake and struck inside one window. New letters, and exactly one head
                // nods off again.
                //
                // One, not all three. Sending them all back to sleep made every single hit cost
                // three taps plus three presses inside one window, which is six actions in 2.4s
                // before a thumb has touched a minion — the soak bot died on this boss at every tier
                // above casual, and it was not close. One sleeper keeps the tap in the loop as a
                // trickle rather than a toll, which is what the mechanic wanted to be: something to
                // keep on top of, not a gate in front of every hit.
                chord = 0;
                awake &= ~(1 << rnd.nextInt(head.length));
                for (int k = 0; k < head.length; k++) head[k] = rnd.nextInt(Glyph.COUNT);
                return damage(1f);
            }
            case DRUM: {
                int r = damage(1f);
                // A landed beat closes the window at once, so one window is worth exactly one hit
                // and the beat stays something played rather than something mashed through.
                phase = 0f;
                nextBeat(rnd);
                return r;
            }
            case MAGPIE: {
                int r = damage(1f);
                // Hit, so it drops what it was holding: the key falls onto the field as an element
                // and has to be dragged home. Losing the drag is how it gets stolen again.
                //
                // Unless that was the last hit, in which case damage() has already handed the key
                // straight back and there is nothing to drop. Dropping anyway put an element on the
                // field carrying key -1, which is an array index waiting to happen — and it was, in
                // the first run of these assertions.
                if (!beaten) dropKey(rnd);
                return r;
            }
            case SUMO: {
                // Does not hurt it. It staggers it — which doubles the next shove — and banks the
                // swipe that shove will be spent on. So the keys pay for the gesture, and the two
                // halves of this fight need each other.
                //
                // Charges used to come from words cleared during the fight, which was a fine rule
                // until boss stages stopped spawning words: this boss then had no way to earn a
                // swipe at all and could not be beaten. If a mechanic is paid for in some other
                // system's currency, it dies when that system does.
                stagger = STAGGER_TIME;
                if (charges < CHARGE_MAX) charges++;
                want = rnd.nextInt(Glyph.COUNT);
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
            float r = er[i] * 1.35f;
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
        return etype[i] == E_GLOB || etype[i] == E_KEY;
    }

    /**
     * A tap on element {@code i}. Only the tapped elements answer this; a draggable one returns
     * {@link #NONE} so a stray tap on a glob is not silently eaten.
     */
    int tap(int i, Random rnd) {
        if (!fighting() || i < 0 || i >= ELEMS || etype[i] == E_OFF) return NONE;
        if (etype[i] == E_HEAD) {
            if (headAwake(i)) {
                // Already up. Not a rebuff — tapping a head twice is a natural thing to do and must
                // not sound like a mistake.
                return NONE;
            }
            awake |= 1 << i;
            return PART;
        }
        if (etype[i] == E_SKIN) {
            if (!tapBeat) {
                // This beat wants a key, not a tap.
                rage = 1f;
                return REBUFF;
            }
            if (!open()) {
                rage = 1f;
                phase = 0f;
                return REBUFF;
            }
            int r = damage(1f);
            phase = 0f;
            nextBeat(rnd);
            return r;
        }
        return NONE;
    }

    /** Picks element {@code i} up. Returns true when the boss took the finger. */
    boolean grab(int i) {
        if (!fighting() || !draggable(i)) return false;
        held = i;
        return true;
    }

    /**
     * A held element following a finger.
     *
     * @return {@link #HIT} when the drag completed the job, {@link #PART} while it is still under
     *     way, {@link #NONE} when nothing is held
     */
    int dragTo(float x, float y, Layout L) {
        if (!fighting() || held < 0 || etype[held] == E_OFF) return NONE;
        ex[held] = x;
        ey[held] = y;
        int t = etype[held];
        if (t == E_GLOB) {
            // Carried to the edge of the play area, which is worth another hit on the boss.
            //
            // The test is the glob's own edge touching the play edge, not its centre reaching it.
            // Centre-on-the-line meant the finger had to arrive within a couple of percent of the
            // physical screen edge, where Android's own edge gestures start stealing the touch — so
            // the drag kept ending in a lifted finger instead of a landed hit, and it read as only
            // counting on release. There is a whole glob's width of slack now, and the glob is
            // visibly against the wall when it lands.
            float slack = er[held];
            if (x <= L.playLeft + slack || x >= L.playRight - slack || y <= L.playTop + slack) {
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
                damage(1f);
                return HIT;
            }
        } else if (t == E_KEY) {
            // Home is the deck. A drag ending on a key is fine — it is a drag *starting* on one
            // that this game cannot afford.
            if (y >= L.deckTop) {
                stolen = -1;
                // Empty-handed for a moment, which is the reward for the fetch: a whole deck to type
                // with. Then it takes another. Timed rather than immediate, because a thief that has
                // stolen the next key before you have straightened up makes the drag feel pointless.
                stealT = STEAL_GAP;
                clearElem(held);
                held = -1;
                return HIT;
            }
        }
        return PART;
    }

    /** The finger lifted without finishing. Whatever was held stays where it was dropped. */
    void release() {
        held = -1;
    }

    /**
     * A swipe at {@link #SUMO}. Spends a charge, shoves it back to the top and hurts it — twice as
     * hard if a press has staggered it.
     *
     * @return true when it landed, so the caller knows the gesture was consumed
     */
    boolean shove() {
        if (!shovable()) return false;
        charges--;
        depth = 0f;
        float n = stagger > 0f ? STAGGER_BONUS : 1f;
        stagger = 0f;
        damage(n);
        return true;
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
            leaveT = LEAVE;
            // Beaten, so it gives the key back and takes its litter with it. Cleared here as well as
            // in leave(): the exit animation still draws the body, and it must not still be holding
            // a key hostage while it bursts.
            stolen = -1;
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
        seed(i);
        // The hit tears the silhouette outward before the wart settles onto it.
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
            // Small enough that two of them inside the body do not become its eyes — at 0.30 a pair
            // sat exactly where a face goes and the creature stopped having one.
            er[i] = br * 0.23f;
            if (held == i || moved[i]) return;
            // Inside the body. A glob is a piece that has come loose, not a thing standing next to
            // the boss — it sits in the goo it split off from, glowing through it, until a finger
            // hauls it out. That is also what makes the stretch read: the skin has to be dragged out
            // around something that started within it.
            //
            // Spread across the width, so a wide boss sheds them out along itself instead of
            // stacking them all down its middle. Vertically it is the plain radius: the body is only
            // ever wide, never tall, so that axis has no room to spare.
            // A wart on the silhouette, overlapping it enough to read as one continuous form.
            ex[i] = bx + globSide[i] * (br * wide() - er[i] * 0.55f);
            ey[i] = by + br * (globLift[i] - 0.6f) * 0.5f;
        } else if (etype[i] == E_KEY) {
            er[i] = L == null ? br * 0.30f : L.keyR * 0.78f;
            if (held == i || moved[i]) return;
            // Below the body: the magpie drops it, so it is out in the open from the start.
            ex[i] = bx + globSide[i] * br * 0.85f;
            ey[i] = by + br * (1.25f + globLift[i] * 0.35f);
        }
    }

    /**
     * Puts a just-created element where the layout pass would, from the body's last known placement
     * — so it is never briefly at the origin. See {@link #lastBX}.
     *
     * No {@link Layout} here: an element is created by a press, which does not have one. Only the
     * dropped key's radius wants it, and one frame at an approximate radius is invisible where a
     * frame at the origin was not.
     */
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
    /** True once a finger has moved this element, so the layout pass stops placing it. */
    private final boolean[] moved = new boolean[ELEMS];

    /**
     * {@link #MAGPIE} drops the key it was holding onto the field.
     *
     * Does nothing if it is not holding one, or if the one it dropped is still lying there. Without
     * that second guard a second hit dropped the same key again — two elements carrying the same
     * letter, one of which could not possibly be returned, since returning either one frees it. It
     * showed up as two identical dumplings under the boss in a preview frame.
     */
    private void dropKey(Random rnd) {
        if (stolen < 0) return;
        for (int i = 0; i < ELEMS; i++) {
            if (etype[i] == E_KEY) return;
        }
        int i = freeElem();
        if (i < 0) {
            // Nowhere to put it, so it simply keeps hold of it and takes a different one. Cannot
            // happen with the slots it has, and silently losing a key would be worse than this.
            steal(rnd);
            return;
        }
        etype[i] = E_KEY;
        elife[i] = KEY_TIME;
        keyOf[i] = stolen;
        globSide[i] = rnd.nextBoolean() ? -1f : 1f;
        globLift[i] = 0.5f;
        moved[i] = false;
        seed(i);
        // It is on the field now, not in its pocket — but it is not yours again until it is home, and
        // the deck stays short until then. Which is exactly why the new letter still has to avoid the
        // key being denied: a bare reroll here could land on it, and then the only press that could
        // hurt the thief would be the one press it was refusing. That is a stage nobody can leave,
        // and with no retreat to fall back on it is a run ended by a coin flip.
        want = pickWant(stolen, rnd);
    }

    /** A wanted letter that is not {@code avoid}, drawn uniformly over the five that qualify. */
    private int pickWant(int avoid, Random rnd) {
        if (avoid < 0 || avoid >= Glyph.COUNT) return rnd.nextInt(Glyph.COUNT);
        int pick = rnd.nextInt(Glyph.COUNT - 1);
        return pick < avoid ? pick : pick + 1;
    }

    /** Which key a dropped-key element is carrying. */
    final int[] keyOf = new int[ELEMS];

    // ---- bolts --------------------------------------------------------------
    /**
     * Letter bolts, thrown at the deck when the slime prompt expires.
     *
     * Not {@code Enemy}: a boss stage releases no words, and these are not words — they carry one
     * letter, fly at the key that clears them, and cost a life at the deck. Press the letter to swat
     * one; there is nothing to engage and no order to type them in.
     */
    static final int BOLTS = 3;
    /** Seconds one takes to reach the deck. Long enough to read three letters and find three keys. */
    static final float BOLT_TIME = 2.4f;
    /**
     * Head start between bolts, in progress. They are launched together, so without this all three
     * land on the same frame and one volley takes three lives at once. 0.18 spaces them 0.43s apart
     * — long enough to press three keys in sequence.
     */
    static final float BOLT_STAGGER = 0.18f;
    /** Live flag, letter, launch point and 0..1 of the way down, per bolt. */
    final boolean[] blive = new boolean[BOLTS];
    final int[] bglyph = new int[BOLTS];
    final float[] bsx = new float[BOLTS];
    final float[] bsy = new float[BOLTS];
    final float[] bt = new float[BOLTS];
    final int[] bhp = new int[BOLTS];
    final int[] bhpMax = new int[BOLTS];

    /** Where bolt {@code i} is now: launch point to its own key, straight. */
    float boltX(int i, Layout L) {
        return bsx[i] + (L.keyX[bglyph[i]] - bsx[i]) * boltAt(i);
    }

    float boltY(int i, Layout L) {
        return bsy[i] + (L.keyY[bglyph[i]] - bsy[i]) * boltAt(i);
    }

    /** 0..1 of the way down, with the negative head start clamped off. */
    float boltAt(int i) {
        return bt[i] < 0f ? 0f : bt[i];
    }

    boolean hasGlob() {
        if (kind != SLIME) return false;
        for (int i = 0; i < ELEMS; i++) if (etype[i] == E_GLOB) return true;
        return false;
    }

    /** Live bolts on the field. */
    int boltCount() {
        int n = 0;
        for (int i = 0; i < BOLTS; i++) {
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
        for (int i = 0; i < BOLTS; i++) {
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
        if (body != null) body.squash(0.75f);
        promptT = promptDelay();
        for (int i = 0; i < BOLTS; i++) {
            blive[i] = true;
            // Spread round the six rather than drawn independently — a repeat would collapse the
            // volley, and the spacing keeps the three keys apart on the deck.
            bglyph[i] = (first + i * 2) % Glyph.COUNT;
            bhp[i] = bhpMax[i] = hits;
            bt[i] = -BOLT_STAGGER * i;
            // Fanned across the body it came out of, so they plainly come from the boss.
            bsx[i] = lastBX + (i - 1) * lastBR * 0.55f;
            bsy[i] = lastBY;
        }
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
        if (bhp[i] <= 0) blive[i] = false;
        return PARRY;
    }

    /** Ticks the volley. Returns the number that reached the deck this frame, each costing a life. */
    private int ageBolts(float dt) {
        int landed = 0;
        for (int i = 0; i < BOLTS; i++) {
            if (!blive[i]) continue;
            bt[i] += dt / BOLT_TIME;
            if (bt[i] < 1f) continue;
            blive[i] = false;
            landed++;
        }
        return landed;
    }

    private void clearBolts() {
        for (int i = 0; i < BOLTS; i++) {
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
    /** False until the body has been placed, which cannot happen until a {@link Layout} is in hand. */
    private boolean bodyPlaced;

    /** Takes a key, never the one it is showing — that pairing would be a deadlock. */
    private void steal(Random rnd) {
        want = rnd.nextInt(Glyph.COUNT);
        stolen = pickWant(want, rnd);
    }

    /** Alternates the drum between wanting a key and wanting a tap, and rerolls its letter. */
    private void nextBeat(Random rnd) {
        tapBeat = !tapBeat;
        want = rnd.nextInt(Glyph.COUNT);
    }

    /** Returns how many visible boss threats reached the deck this frame. */
    int update(float dt, Layout L, Random rnd) {
        if (kind < 0) return 0;
        hurt = Math.max(0f, hurt - dt * 2.6f);
        launchT = Math.max(0f, launchT - dt);
        launched = false;
        rage = Math.max(0f, rage - dt * 2.2f);

        // Above both early returns below, and that is not tidiness. An element has no position until
        // this has run, so laying out after the intro return meant that on the very frame the
        // arrival card ended — the first frame a tap is accepted — every element was still sitting at
        // the origin, and a tap on one hit nothing. It also lets the card draw them, which is how the
        // heads and the drum skin arrive with the body instead of appearing after it.
        // Before the layout, because the layout reads bodyX/bodyY and those include the follow.
        updateFollow(dt, L);
        layoutElems(L);

        // The body follows wherever the layout put the boss, and keeps wobbling on the way out — a
        // burst that starts from a frozen shape reads as two separate animations.
        if (body != null) {
            if (!bodyPlaced) {
                body.reset(bodyX(L), bodyY(L), bodyR(L), wide());
                body.jiggle = JIGGLE[kind];
                bodyPlaced = true;
            } else {
                body.moveTo(bodyX(L), bodyY(L));
            }
            // A glob being hauled out stretches the skin after it, like pulling at something in
            // treacle. Re-aimed every frame at wherever the finger has got to, and let go the
            // instant nothing is held — the spring back is the solver's own and needs no schedule.
            //
            // With the glob's own radius handed over, so the promise the solver keeps is that the
            // whole glob stays inside the body rather than that its centre does. Between that and
            // updateFollow above, a glob never leaves the goo it came out of until it is off the
            // screen.
            if (beaten) {
                // Shared death morph: gravity wins while the body is carried toward the player.
                // Pulling below its travelling centre makes the silhouette neck, sag and melt.
                float melt = leaveProgress();
                body.pull(body.centreX(), L.h + bodyR(L) * 2.5f,
                        0.22f + melt * 0.58f);
                body.jiggle = JIGGLE[kind] * (1f + melt * 2.6f);
            } else if (held >= 0 && etype[held] == E_GLOB) {
                body.pull(ex[held], ey[held], PULL_K, er[held]);
            } else {
                body.letGo();
            }
            if (kind == SLIME && !beaten)
                body.jiggle = JIGGLE[kind] * (1f + (1f - health()) * 1.25f);
            body.update(dt);
        }

        // Above both early returns: a volley already in the air still arrives. Cleared on the frame
        // the boss is beaten, so nothing lands after the burst.
        int hits = ageBolts(dt);

        if (beaten) {
            leaveT = Math.max(0f, leaveT - dt);
            return hits;
        }
        if (intro > 0f) {
            intro = Math.max(0f, intro - dt);
            return hits;
        }

        age += dt;
        if (stagger > 0f) stagger = Math.max(0f, stagger - dt);
        if (stealT > 0f) {
            stealT = Math.max(0f, stealT - dt);
            // The breather is up: it takes another key. Only ever reached with an empty hand, since
            // this clock is only started by a key being fetched home.
            if (stealT <= 0f && kind == MAGPIE && stolen < 0) steal(rnd);
        }
        if (chordT > 0f) {
            chordT = Math.max(0f, chordT - dt);
            // Ran out of time: the part-finished chord is lost, and it is lost visibly, since a
            // combo that resets with no cause reads as a fault.
            if (chordT <= 0f && chord != 0) {
                chord = 0;
                rage = 1f;
            }
        }
        ageElems(dt);

        if (kind == SLIME && boltCount() == 0 && !hasGlob() && open()) {
            promptT -= dt;
            if (promptT <= 0f) volley(rnd);
        }

        if (kind == SUMO) {
            depth += dt / SINK_TIME;
            if (depth >= 1f) {
                depth = 0f;
                // It landed on the line with its whole weight, so it squashes flat and rebounds.
                if (body != null) body.squash(1f);
                hits++;
            }
            return hits;
        }

        float cycle = CYCLE[kind];
        boolean wasOpen = open();
        phase += dt;
        if (phase >= cycle) phase -= cycle;
        if (wasOpen && !open()) {
            // The window shut. A part-finished chord is lost, and that is the only thing a shut
            // window costs anybody.
            //
            // Nothing heals. The chain used to give a press back here, which meant the health bar
            // went up while you watched — and a bar that goes up reads as being cheated rather than
            // as being hard. The wakes survive too, for the same reason: a fumbled chord costing six
            // actions to get back to where it already was is a punishment that compounds, and
            // compounding punishment on a boss with no way past it is how a stage becomes a wall.
            chord = 0;
        }
        return hits;
    }

    /** Where every live element is this frame. Read by both the hit-test and the renderer. */
    private void layoutElems(Layout L) {
        float bx = bodyX(L), by = bodyY(L), br = bodyR(L);
        lastBX = bx;
        lastBY = by;
        lastBR = br;
        for (int i = 0; i < ELEMS; i++) {
            switch (etype[i]) {
                case E_HEAD:
                    // Three heads across the body, evenly spread and a touch above its middle.
                    // Fixed to it: they are the boss, which is also why it is drawn without a face
                    // of its own — see BossScreen.
                    ex[i] = bx + (i - 1) * br * 1.05f;
                    ey[i] = by - br * 0.06f;
                    er[i] = br * 0.40f;
                    break;
                case E_SKIN:
                    ex[i] = bx;
                    ey[i] = by;
                    er[i] = br * 0.72f;
                    break;
                case E_GLOB:
                case E_KEY:
                    place(i, bx, by, br, L);
                    break;
                default:
                    break;
            }
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
            if (etype[i] == E_KEY) {
                // Snatched again, and it is the same key: losing the drag has to cost the thing the
                // drag was for, or the theft would be a formality.
                stolen = keyOf[i];
            }
            clearElem(i);
        }
    }
}
