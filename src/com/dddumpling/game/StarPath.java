package com.dddumpling.game;

import java.util.Random;

/** Persistent course and flight state for the alternating star-path interlude. */
final class StarPath {
    static final int COUNT = 20;
    static final int MAX_DIFFICULTY = 10;
    static final int WIN_STEP = 1;
    // Continue the same +0.6 bend rate per level through ten successful courses.
    /** Saved difficulty level; keeps the historical wins storage key. */
    int wins;

    float bendRate() { return 1f + 0.6f * Math.max(0, Math.min(MAX_DIFFICULTY, wins)); }

    void recordWin() { wins = Math.min(MAX_DIFFICULTY, Math.max(0, wins) + WIN_STEP); }

    void resetDifficulty() { wins = 0; }

    /**
     * The flight is down from five seconds to four and now to 3.6: the same twenty checkpoints
     * arrive in ever less time, which together with the wider spacing below puts the scroll up
     * by nearly half again on where it started.
     */
    static final float READY = 1.5f, FLY = 3.6f, EXIT = 0.75f, REPORT = 1.5f;
    /**
     * Course lengths visible at once; larger means wider gaps between its stars.
     *
     * This is also the spacing knob: the gap between two checkpoints is
     * {@code span * COURSE_SCREENS / COUNT}, and since the whole course still passes in {@link #FLY}
     * seconds, raising it spreads them out <em>and</em> speeds the scroll up in the same move.
     *
     * Note what it does <em>not</em> change: when each checkpoint arrives. The gaps between arrivals
     * come out of {@link #FLY} and {@link #RUSH} through {@link #encounterTime}, which touches this
     * only through a half-screen lead term — so spacing them out and speeding the scroll up by the
     * same factor leaves the rhythm of a course exactly where it was. That is the knob being asked
     * for whenever the stars want to be further apart without the course getting busier.
     *
     * It went 3.2 down to 2.8 to get more of a sweep on screen — at six checkpoints the course read
     * as a straight diagonal however swoopy it was over its five seconds — then back up to 3.4 and
     * 4.2 because the stars wanted to be further apart, then to 10.5, and back to here.
     *
     * 10.5 is where the ceiling was found, and it is not the checkpoints touching or the line
     * looking straight: it is the <em>lookahead</em>. Spacing them out at a fixed rhythm speeds the
     * scroll up by the same factor, so it divides the seconds of course visible ahead of the flyer
     * by that factor too — 162ms at 10.5, which is inside a hand's own reaction time, so the line
     * arrives already too close and the course reads as unfair rather than fast. At 5.6 it is 295ms,
     * comfortably the far side of a reaction, with the gap a quarter of a screen and three or four
     * checkpoints in shot. That is the number to dial this against; {@code TestStars} prints it.
     *
     * Nothing else has to move when it does, and that took two goes to arrange. The catch's vertical
     * tolerance is stated in seconds rather than pixels ({@link #GRACE}), the trail's pulse is in
     * screen space ({@code StarScreen}), and the course's own shape is held still by freezing the
     * lead term this used to feed — see {@link #LEAD}, which is the one that made this a difficulty
     * knob in disguise.
     */
    static final float COURSE_SCREENS = 5.6f;
    /**
     * How sharply the scroll accelerates over the flight. 1 would be a constant crawl.
     *
     * Nearly flat now, down from 1.45, because {@link #EASE_IN} took over the job it was really
     * doing. A course used to crawl at the off and make its pace up later, and that curve was the
     * only thing keeping the opening gentle — but it also meant the last stars arrived three times
     * as fast as the first, and it is the closing speed that decides how long a checkpoint is level
     * with the flyer. At 1.45, with the stars spaced further apart and the flight shortened, the
     * end of a course went past faster than a hand can be in place for: the naive harness pilot went
     * from taking nineteen to taking ten, and every one of the ten it lost was in the last half.
     * Held a little above 1 so a course still gathers itself rather than running at one flat pace.
     */
    static final float RUSH = 1.15f;
    /**
     * Seconds of soft start: the course leaves from a standstill and reaches its nominal pace over
     * this long, rather than being at speed on the first frame of the flight.
     *
     * Applied through {@link #launch}, which is normalised so the course still passes in exactly
     * {@link #FLY} — the seconds given away at the off are taken back over the rest of the flight.
     * {@link #encounterTime} inverts it, so the generator and the scroll agree about when a star
     * arrives; there is a round-trip assertion on that pair.
     */
    static final float EASE_IN = 1f;
    static final float TAU = 6.2831853f;

    /**
     * Top steering speed and the acceleration up to it, as fractions of the view width per second.
     * Named because the course is generated against them: what a sweep may ask for is exactly what
     * these can deliver, and the two used to be literals inside {@link #update} where nothing
     * generating a course could see them.
     *
     * Raised from 0.48 and 2.1 to buy the sweep its shorter period — the flyer has to be able to
     * keep up with a course that crosses the whole play area twice in five seconds, and this is the
     * side of that trade the player feels as the thing answering the key rather than lagging it.
     */
    static final float MAX_VX = 0.60f, ACCEL = 2.6f;
    /** Maximum rocket vibration, as a fraction of the text unit. */
    static final float FLIGHT_SHAKE = 0.22f;

    /**
     * The sweep: how far either side of the middle a course reaches, and the seconds it takes to
     * come back. {@code 0.40} spans the play area — the flyer's own clamp keeps its centre a little
     * inside the edges, and {@link #EDGE} is set to exactly what it can reach.
     *
     * The period is shorter than the sweep can be <em>tracked</em> at, so following the line is not
     * enough and the course has to be anticipated and cut across instead. How much shorter is not
     * usefully measured as a ratio of peak speeds, which is how it was stated for a long time: a
     * sine only exceeds {@link #MAX_VX} over a short arc either side of its steepest point, so the
     * old "asks 1.2 times the steering" course cost a tracker one hundredth of a width — a twelfth
     * of the catch band — and every pilot the harness can write collected all twenty at any reaction
     * time. What the player feels is the <em>lag</em> that overspeed integrates to, against the width
     * of the band; {@code TestStars} measures both, and the peak ratio is only printed now.
     *
     * The period is therefore shorter than it was, and no longer scaled to leave the peak ratio
     * near 1.2. The far bound is still a cliff rather than a preference — a flyer that can never
     * catch up stops being late and ends up somewhere else entirely, and which stars it gets turns
     * to luck — but the thing to hold it by is the feasible window at each checkpoint, which is what
     * that suite now asserts.
     */
    static final float SWEEP = 0.40f, SWEEP_TIME = 3.42f;
    /**
     * The ripple on top: what makes a course ask for direction changes rather than one long lean.
     *
     * Twice what it was, at two thirds the period, and this is the reactive half of the difficulty —
     * the sweep alone is a line to be read once at the start of a course, while the ripple has to be
     * answered as it arrives. It spends the same budget as the sweep, so it is still the smaller of
     * the two: the sweep is the shape a player is meant to be able to see coming.
     */
    static final float RIPPLE = 0.04f, RIPPLE_TIME = 1.6f;
    /** Closest a checkpoint comes to the edge of the play area. */
    static final float EDGE = 0.5f - SWEEP;
    /**
     * How long the victory tableau holds once the last star is taken: the course stops dead, the
     * prize climbs out of that star, and then the parade takes the screen. Long enough to read the
     * name under it, short enough that it is not standing between a win and the parade.
     */
    static final float WIN_HOLD = 2.7f;
    /**
     * Flyer radius as a multiple of an enemy tile's, and star radii as multiples of the text
     * unit. One copy of each: the pickup test in here and the drawing in {@link StarScreen} have
     * to agree about how big these are, and they were two separate literals to begin with.
     *
     * All three are half again what they first were: at the original sizes the flyer and its
     * checkpoints were the smallest things on a screen that has nothing else on it. What did
     * <em>not</em> grow with them is the catch tolerance — see {@link #pickupR}.
     */
    static final float FLYER_R = 1.575f, STAR_OUT = 2.34f, STAR_IN = 1.035f;
    /**
     * The heart of a checkpoint, as a fraction of its outer radius: the pearl the drawing paints at
     * the centre of the petals, and what the pickup test below aims at. Was the glow disc around
     * that pearl, at 0.30 — the pearl is the tighter of the two things drawn there, and taking the
     * tighter one is a little less forgiving for the same story.
     */
    static final float HEART = 0.23f;

    static float flyerR(Layout L) { return L.enemyR * FLYER_R; }
    static float starOuter(Layout L) { return L.unit * STAR_OUT; }
    static float starInner(Layout L) { return L.unit * STAR_IN; }

    /**
     * How close a centre has to come to a checkpoint's centre to take it: the flyer has to cover
     * the star's heart, not merely brush a petal.
     *
     * The old rule was two flyer radii, which at the old sizes came to almost exactly this number —
     * and that is the point of writing it this way. Growing the flyer and the stars by half again is
     * a cosmetic change, and two flyer radii would have turned it into a difficulty change: the
     * catch band went from a ninth of the width to a sixth, which is wider than the course's own
     * wander, so a flyer sitting dead centre with nobody touching the keys completed courses on its
     * own. The soak bot found that, having no way to steer at all.
     */
    static float pickupR(Layout L) { return flyerR(L) + starOuter(L) * HEART; }

    /**
     * How late, in seconds, a flyer may still be and take the checkpoint. The catch is an ellipse
     * rather than a circle, and this is its half-height — stated in <em>time</em> rather than in
     * radii, which is the whole point of it.
     *
     * Because only one of the two axes is a skill. The flyer's height and the course's scroll are
     * both functions of the clock and no key touches either: whether a star is level is not
     * something a player can be good at. What a vertical tolerance actually buys is forgiveness for
     * arriving *late*, which is a real human error and is measured in milliseconds — so a distance
     * is the wrong unit for it, and the bug that unit causes is silent. As a distance it was 1.7
     * pickup radii, which was 117ms of grace at one scroll speed and 47ms at two and a half times
     * that; the course would have got harder in the way that feels like the game cheating, with the
     * sideways demand unchanged. Held in seconds it survives any future change to spacing or pace,
     * which is exactly the sort of arithmetic this file keeps getting wrong.
     *
     * 0.065 gives a window of about 130ms — a little over a hand's own reaction quantum, which is
     * what it is for. {@code TestStars} measures the window and holds it against this.
     */
    static final float GRACE = 0.065f;

    /**
     * Vertical half-height of the catch: {@link #GRACE} seconds of whatever the course is closing on
     * the flyer at, with a floor so the launch — where the scroll is barely moving and a star is
     * level for the best part of a second anyway — still has a sensible shape.
     */
    float pickupY(Layout L) {
        return Math.max(pickupR(L) * 0.5f, closingSpeed(L) * GRACE);
    }

    /**
     * How fast the gap between the flyer and a checkpoint is closing, in pixels a second.
     *
     * Every checkpoint closes at the same rate — they all ride one scroll — so this is the scroll
     * plus the climb, and it is taken as a difference over one frame rather than differentiated by
     * hand. Two curves feed it ({@link #RUSH} and {@link #launch}) and a third would not announce
     * itself; a finite difference is right whatever they are.
     */
    float closingSpeed(Layout L) {
        float held = timer;
        float now = starY(0, L) - characterY(L);
        timer = held - DT;
        float next = starY(0, L) - characterY(L);
        timer = held;
        return Math.abs(next - now) / DT;
    }

    /** One frame, for the finite difference in {@link #closingSpeed}. */
    private static final float DT = 1f / 60f;

    final float[] sx = new float[COUNT];
    /** 1 on pickup, decaying to zero; drives the shine and burst without spawning objects. */
    final float[] burst = new float[COUNT];
    int collected;
    int who = -1;
    float timer, x, vx;
    boolean left, right, won, dragging;
    /**
     * Seconds of victory tableau left. While this is running every other kind of motion here is
     * frozen — the scroll, the steering and the pickup shines all hold where the win found them.
     */
    float winT;
    /** Which checkpoint completed the course, so the prize can climb out of that one. */
    int winStar = -1;
    /**
     * Set on any frame a checkpoint is taken, for the caller to turn into the slow-motion beat and
     * the climbing note.
     *
     * Not set by the one that completes the course: the victory tableau is a full stop already, so a
     * beat over the top of it only stretches its opening, and the achievement fanfare is what that
     * star sounds like. The note ladder therefore runs to nineteen and the fanfare takes the last.
     */
    boolean grabbed;
    /**
     * Set on the frame the ready lesson hands over to the flight, and on the frame a course that
     * was not won starts reading its count out. Both are for the caller to turn into a sound; both
     * are cleared by it, as {@link #grabbed} is.
     */
    boolean launched, reported;
    /**
     * Set on the frame the course is completed and cleared by {@link GameCore} once it has paid
     * out. The tableau draws what was won, so the prize has to be picked before it is drawn —
     * which means the award happens here, not at the end of the interlude.
     */
    boolean awardPending;

    /**
     * The course: one long swoop out to one edge of the play area, back through the middle and away
     * to the other, with a small ripple riding on top of it.
     *
     * Shaped in <em>seconds</em> rather than in star index, which is the whole trick. The scroll
     * accelerates through the flight, so the last stars arrive about three times as fast as the
     * first; a random walk with a fixed step per star therefore demanded three times the sideways
     * speed at the end that it did at the start, and the steps had to be kept small enough for the
     * fast end — which is why the old course could only wiggle, over the middle two thirds of the
     * width. A sine in seconds asks the same speed of you everywhere, so the sweep can span the
     * whole play area and still sit inside what the steering can deliver.
     *
     * Wins shorten both bend periods without changing the flight clock or catch tolerance.
     * {@code TestStars} checks reachability at every level and flies the capped course with
     * reaction-limited pilots.
     */
    void make(Random rnd) {
        // The side is taken from the second draw, not the first. Java's generator gives nearby
        // seeds the same leading bit, so a coin flipped first sent every course in a freshly
        // seeded harness the same way — the game shares one long-lived generator and would never
        // have shown it.
        float bends = bendRate();
        float period = SWEEP_TIME * (1f + 0.20f * rnd.nextFloat()) / bends;
        float dir = rnd.nextFloat() < 0.5f ? 1f : -1f;
        float ripplePhase = rnd.nextFloat() * TAU;
        for (int i = 0; i < COUNT; i++) {
            float t = encounterTime(i);
            // No sweep phase: every course leaves from the middle, which is where the flyer starts.
            // A random phase put the first star out at an edge and opened with a dash nobody could
            // have known was coming.
            float at = 0.5f + dir * SWEEP * (float) Math.sin(TAU * t / period)
                    + RIPPLE * (float) Math.sin(ripplePhase + TAU * t * bends / RIPPLE_TIME);
            if (at < EDGE) at = EDGE;
            if (at > 1f - EDGE) at = 1f - EDGE;
            sx[i] = at;
        }
        collected = 0;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    /**
     * A fresh line for the next attempt, keeping the checkpoints already in hand.
     *
     * Because a failed attempt used to be handed the identical course back, and once the course got
     * hard enough to fail that turned the carry-over promise inside out. The harness pilot with
     * quarter-second thumbs takes fifteen or so of a course, and on the same line it takes the same
     * fifteen every time: three of eight courses stalled at seventeen or nineteen for as many
     * attempts as it was given, so the prize was not a matter of persistence but simply unreachable.
     * A player is better than that — they learn a corner they keep losing — but the shape of the
     * thing is wrong either way, and re-rolling makes the stars in hand mean what they say: each
     * attempt asks a different question and what has been answered stays answered.
     */
    void reroll(Random rnd) {
        // Recover a completed hand left behind by an interrupted celebration.
        int held = count() == COUNT ? 0 : collected;
        make(rnd);
        collected = held;
    }

    /**
     * Roughly how many seconds into the flight star {@code i} comes level with the flyer.
     *
     * Deliberately approximate: it exists to shape the course against the steering, and a frame
     * either way changes nothing. It takes the flyer's mid-flight height, which is where it spends
     * all but the opening moment, so the first few stars come out at zero — they are already
     * level with it when the course starts, and that keeps the opening stars in the middle.
     *
     * Exact in one respect, though: it inverts {@link #launch} as well as the {@link #RUSH} curve,
     * so the soft start does not slide the whole course out from under the sweep it was shaped
     * against. {@code TestStars} round-trips the pair.
     */
    static float encounterTime(int i) {
        float ahead = (i + 0.5f) / COUNT - LEAD;
        if (ahead <= 0f) return 0f;
        return FLY * unlaunch((float) Math.pow(ahead, 1f / RUSH));
    }

    /**
     * How much of the course sits between a checkpoint appearing and it being level with the flyer,
     * as a fraction of the whole course. Half a screen of it, at the spacing this was tuned at.
     *
     * A constant, and that is the point. It was {@code 0.5 / COURSE_SCREENS} — geometrically honest,
     * since half a screen is exactly what a star has to travel to reach a flyer sitting mid-field —
     * and it made {@link #COURSE_SCREENS} secretly a difficulty knob. Every arrival time is measured
     * from it, so moving the spacing re-sampled the sweep at different moments and handed back a
     * <em>different course</em>: the same dial that was documented as purely cosmetic took the naive
     * pilot from nineteen stars to thirteen when the spacing was dialled back, with nothing else
     * touched. Frozen here, the spacing changes what a course looks like and how fast it comes at
     * you, and nothing about what it asks — which is what it was always claimed to do.
     */
    static final float LEAD = 0.05f;

    /** Where the soft start's ramp meets the nominal pace, as a fraction of the flight. */
    private static final float RAMP = EASE_IN / FLY;
    /** What the ramp gives away at the off, and so what the rest of the flight takes back. */
    private static final float LOST = RAMP * 0.5f;

    /**
     * Flight progress with the soft start applied: a rate of zero on the first frame, climbing
     * linearly to the nominal pace at {@link #EASE_IN} and holding it after that.
     *
     * A quadratic ramp rather than a smoothstep because it has to be invertible in closed form —
     * see {@link #unlaunch} — and because the pace either side of the knee is what is felt, not
     * the third derivative at it. Normalised by {@code 1 - LOST} so {@code launch(1) == 1}: the
     * course still finishes exactly when the flight does, having spent the seconds it gave away at
     * the start slightly faster over the rest.
     */
    static float launch(float p) {
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        return (p < RAMP ? p * p / (2f * RAMP) : p - LOST) / (1f - LOST);
    }

    /** The inverse of {@link #launch}, for turning a place in the course back into a time. */
    static float unlaunch(float z) {
        if (z <= 0f) return 0f;
        if (z >= 1f) return 1f;
        float knee = LOST / (1f - LOST);
        return z <= knee ? (float) Math.sqrt(2f * RAMP * z * (1f - LOST))
                : z * (1f - LOST) + LOST;
    }

    void begin(int entry, Layout L) {
        who = entry;
        timer = READY + FLY + EXIT + REPORT;
        x = L.w * 0.5f;
        vx = 0f;
        left = right = won = dragging = false;
        launched = reported = false;
        winT = 0f;
        winStar = -1;
        awardPending = false;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    boolean ready() { return timer > FLY + EXIT + REPORT; }
    boolean flying() { return timer <= FLY + EXIT + REPORT && timer > EXIT + REPORT; }
    boolean exiting() { return timer <= EXIT + REPORT && timer > REPORT; }
    float exitProgress() {
        if (!exiting()) return 0f;
        float p = (EXIT + REPORT - timer) / EXIT;
        return p < 0f ? 0f : p > 1f ? 1f : p;
    }
    boolean reporting() { return timer <= REPORT; }
    /** True while the victory tableau owns the screen, which is the end of a completed course. */
    boolean winning() { return winT > 0f; }
    /** 0..1 through the victory tableau. */
    float winProgress() {
        if (winT <= 0f) return 0f;
        float p = 1f - winT / WIN_HOLD;
        return p < 0f ? 0f : p > 1f ? 1f : p;
    }
    float flightProgress() {
        float p = (FLY + EXIT + REPORT - timer) / FLY;
        return p < 0 ? 0 : p > 1 ? 1 : p;
    }

    /** Increasing vibration shared by the flyer drawing and its pickup hitbox. */
    float flightShakeX(Layout L) {
        if (!flying()) return 0f;
        float p = flightProgress();
        return L.unit * FLIGHT_SHAKE * p * p
                * ((float) Math.sin(timer * 47f) * 0.68f + (float) Math.sin(timer * 83f) * 0.32f);
    }

    float flightShakeY(Layout L) {
        if (!flying()) return 0f;
        float p = flightProgress();
        return L.unit * FLIGHT_SHAKE * p * p * 0.62f
                * ((float) Math.cos(timer * 53f) * 0.72f + (float) Math.sin(timer * 91f) * 0.28f);
    }

    float flyerX(Layout L) { return x + flightShakeX(L); }
    float flyerY(Layout L) { return characterY(L) + flightShakeY(L); }

    /**
     * Scroll leaves from a standstill, reaches its nominal pace over {@link #EASE_IN}, and keeps
     * accelerating gently from there to the end of the flight.
     */
    float traversalProgress() {
        return (float) Math.pow(launch(flightProgress()), RUSH);
    }
    int count() { return Integer.bitCount(collected); }

    void hold(int key, boolean down) {
        if (key < 0 || key >= Glyph.COUNT || winning()) return;
        if (key < Glyph.COUNT / 2) left = down;
        else right = down;
    }

    void beginDrag() { dragging = true; }
    void endDrag() { dragging = false; }

    /** Places the flyer under a dragging finger while leaving the key steering available. */
    void dragTo(float targetX, Layout L) {
        if ((!ready() && !flying()) || winning()) return;
        float r = flyerR(L);
        x = Math.max(L.playLeft + r, Math.min(L.playRight - r, targetX));
        // Direct manipulation owns the position for this frame. Without clearing this, momentum
        // from a key pressed before the grab makes the flyer slide out from under the finger.
        vx = 0f;
    }

    void update(float dt, Layout L) {
        if (winning()) {
            // The course stops: no scroll, no steering, no further pickups. The tableau is drawn
            // over a still frame rather than over a course still sliding out from under it.
            //
            // The shines are the exception, and deliberately: a pickup burst frozen part-way
            // through leaves a white glint stuck beside the last star for the whole tableau, which
            // reads as a drawing fault. It is the pickup that just landed, so it finishes.
            for (int i = 0; i < COUNT; i++) burst[i] = Math.max(0f, burst[i] - dt * 1.8f);
            winT -= dt;
            if (winT <= 0f) {
                winT = 0f;
                // Spends whatever was left of the flight: the tableau replaces the climb-out and
                // the report, and this is the value GameCore reads to close the interlude.
                timer = 0f;
            }
            return;
        }
        // Both edges are taken as a phase *change* over this frame, which is the only way to catch
        // them: nothing else here knows that the lesson has just ended or that the flight has, and
        // a caller comparing predicates itself would be comparing them one frame late. Cleared by
        // the caller like {@link #grabbed}, and for the same reason.
        boolean wasReady = ready(), wasReporting = reporting();
        timer -= dt;
        launched = wasReady && !ready();
        // Not on a won course: that ends on the tableau's fanfare, and the tally is the sound of
        // a number arriving without a prize behind it.
        reported = !won && !wasReporting && reporting();
        grabbed = false;
        for (int i = 0; i < COUNT; i++) burst[i] = Math.max(0f, burst[i] - dt * 1.8f);
        if (!flying()) return;
        float dir = left == right ? 0f : left ? -1f : 1f;
        float max = L.w * MAX_VX;
        float accel = L.w * ACCEL;
        float aim = dir * max;
        float step = accel * dt;
        if (vx < aim) vx = Math.min(aim, vx + step);
        else vx = Math.max(aim, vx - step);
        x += vx * dt;
        float r = flyerR(L);
        if (x < L.playLeft + r) { x = L.playLeft + r; vx = Math.max(0, vx); }
        if (x > L.playRight - r) { x = L.playRight - r; vx = Math.min(0, vx); }

        float cx = flyerX(L), cy = flyerY(L);
        float rx = pickupR(L), ry = pickupY(L);
        for (int i = 0; i < COUNT; i++) {
            if ((collected & (1 << i)) != 0) continue;
            float dx = (cx - starX(i, L)) / rx, dy = (cy - starY(i, L)) / ry;
            if (dx * dx + dy * dy <= 1f) {
                collected |= 1 << i;
                burst[i] = 1f;
                if (count() == COUNT) {
                    // The last one. The course is over here rather than when the clock runs out,
                    // so the win is announced where it happened.
                    won = true;
                    winStar = i;
                    winT = WIN_HOLD;
                    awardPending = true;
                    // A thumb still down must not carry its lean into the tableau, or into the
                    // next course: the flight is finished either way.
                    left = right = false;
                    vx = 0f;
                    return;
                }
                grabbed = true;
            }
        }
    }

    /**
     * The ready lesson's demo lean, as a fraction of the view width, and how lit the keys under it
     * are. Both die away over the last {@link #SETTLE} of the ready beat.
     *
     * It lives here rather than in the renderer because it is the flyer's position, not a decoration
     * on it: the drawn x used to be {@code q.x} plus a bare sine of the clock, which meant the flight
     * began by teleporting the flyer from wherever in that swing the beat happened to end back to
     * the middle of the screen. Nothing was wrong with the state — it had never left the middle —
     * and it still read as the character jumping the moment the course started. Settling to a stop
     * on the spot it will fly from costs half a second of the ready beat and removes the jump at
     * source, and it also makes the lesson end on something: the keys dim, the flyer holds still,
     * and then the course moves.
     */
    static final float LESSON_SWING = 0.12f, LESSON_RATE = 3.4f, SETTLE = 0.55f;

    /** 1 through the lesson, easing to 0 by the moment the flight starts. */
    float lessonFade() {
        if (!ready()) return 0f;
        float left = timer - (FLY + EXIT + REPORT);
        if (left <= 0f) return 0f;
        if (left >= SETTLE) return 1f;
        float f = left / SETTLE;
        return f * f;
    }

    /** Sideways offset of the flyer during the lesson, in view widths. */
    float lessonLean(float clock) {
        return (float) Math.sin(clock * LESSON_RATE) * LESSON_SWING * lessonFade();
    }

    /**
     * Which way the lesson is leaning and how hard, in -1..1 — the lean's own rate, normalised.
     * The wake trails the steering, and during the lesson there is no steering to read: the swing
     * is drawn onto the position rather than driven through {@code vx}.
     */
    float lessonSway(float clock) {
        return (float) Math.cos(clock * LESSON_RATE) * lessonFade();
    }

    float characterY(Layout L) {
        float p = launch(flightProgress());
        float bottom = L.dangerY - L.enemyR * 1.3f;
        float middle = L.playTop + (L.dangerY - L.playTop) * 0.50f;
        if (exiting()) {
            float q = (EXIT + REPORT - timer) / EXIT;
            return middle + (L.playTop - L.enemyR * 3f - middle) * q * q;
        }
        return bottom + (middle - bottom) * p;
    }

    float starX(int i, Layout L) { return L.playLeft + sx[i] * (L.playRight - L.playLeft); }

    /** The course scrolls down past the climber; only a segment is visible at once. */
    float starY(int i, Layout L) {
        float span = L.dangerY - L.playTop;
        float course = (i + 0.5f) / COUNT;
        return L.dangerY - (course - traversalProgress()) * span * COURSE_SCREENS;
    }

    void finishAttempt() { left = right = false; }
}
