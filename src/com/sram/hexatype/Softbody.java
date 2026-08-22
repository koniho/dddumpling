package com.sram.hexatype;

/**
 * A pressurised soft body: a ring of mass points held out by an internal pressure, which is what
 * a boss slime is made of.
 *
 * Four forces, one of each per node. Skin springs between neighbours hold the perimeter;
 * <b>pressure</b> along each edge's outward normal holds the area, and is the term that makes a
 * dent on one side come out as a bulge on the other — which is the whole reading of "squishy", and
 * the reason this is not simply a mesh of springs. Springs alone have no opinion about the space
 * inside themselves and fold flat the first time anything pushes them.
 *
 * The third force is a <b>weak pull toward being round</b>, each node toward {@link #rest} from the
 * centroid, and it has to be both there and weak. Stiff, it is a hub and spokes, and a
 * hub-and-spokes blob is a wheel: it answers a dent by rotating rather than by bulging elsewhere.
 * Missing, the body has no <em>bending</em> stiffness at all — a node pushed straight out along its
 * own radius barely stretches its two nearly-tangential neighbours, so an area-preserving
 * flattening costs almost nothing and relaxes only at third order. Measured: without it a single
 * squash was still 6% flat twenty seconds later, decaying like one over root t rather than
 * exponentially, so a boss taking hits for thirty seconds went quietly lopsided and stayed that
 * way. The fourth is a <b>centring pull</b> on the centroid toward {@link #homeX}/{@link #homeY},
 * applied equally to every node — a uniform body force can only move the centre of mass, so it
 * cannot touch the wobble, which is what lets a hit shove the whole blob and still have it come
 * home.
 *
 * <p><b>Deterministic, and it has to be.</b> The preview harness hash-compares every rendered
 * frame between runs, so an RNG or a wall clock in here would make every check differ for no
 * reason. There is none: the idle breath is two harmonics of the ring at rates and phases hashed
 * off a seed the caller names, run off a clock accumulated from the {@code dt} it is handed — the
 * {@code Cabinet.shimmer} shape. See the note on "random" motion in CLAUDE.md.
 *
 * <p><b>Stability.</b> Semi-implicit Euler, substepped: {@link #update} clamps {@code dt} to
 * {@link #MAX_DT} and splits what is left into steps of at most {@link #STEP}. Both guards are
 * load-bearing and they do different jobs — the clamp makes a long frame (a collection, a resumed
 * app, a test handing it a minute) a <em>slow</em> frame rather than an exploded one; the substep
 * is what keeps the stiffness usable. Every stiffness here is an acceleration per unit of
 * displacement, i.e. an angular frequency squared, which makes the sim scale-invariant: the same
 * constants wobble at the same rate whether the blob is a thumbnail or half the screen.
 */
final class Softbody {

    /**
     * Nodes in the ring. Eighteen because of both ends: below about twelve the silhouette reads as
     * a polygon however hard {@link #outline} smooths it, and a dent from {@link #impulse} lands on
     * two nodes and looks like a corner rather than a press. Above about twenty-four nothing
     * changes on screen and it is all cost — this runs every frame under a whole field of words.
     */
    static final int NODES = 18;
    /** Spline samples per node gap in {@link #outline}. Eighteen at four is 72 points. */
    static final int SMOOTH = 4;
    static final float TAU = 6.2831853f;

    /** Longest frame the sim will believe. See the stability note above. */
    static final float MAX_DT = 0.05f;
    /** Longest internal step. Half a frame at 60Hz, so an ordinary frame is two substeps. */
    static final float STEP = 1f / 120f;
    /** Hard cap on substeps, so nothing can turn one call into an unbounded loop. */
    private static final int MAX_STEPS = 8;

    /**
     * Skin spring: acceleration per pixel of extension, so 1/s², and its square root is the
     * frequency. 1600 is 40 rad/s for one spring and twice that for the shortest wavelength the
     * ring can carry, against the 240 rad/s an explicit step of {@link #STEP} can hold — a factor
     * of three of headroom, which is what a phone dropping frames needs.
     */
    private static final float KS = 1600f;
    /**
     * Damping along a spring, on the neighbours' relative speed. Separate from {@link #DAMP}
     * because they kill different things: this takes out the shortest-wavelength ring modes, which
     * are the ones that look like noise rather than wobble, without slowing the whole-body sloshing
     * that is the point of the thing.
     */
    private static final float SPRING_DAMP = 7f;
    /**
     * Pressure. Scaled by {@code n/TAU} where it is used, which is what makes it independent of the
     * node count: an edge is {@code TAU*r/n} long and the force on it is proportional to its length,
     * so without that factor a blob with more nodes would be a stiffer blob.
     */
    private static final float KP = 900f;
    /**
     * The pull toward being round; see the class note for why it exists. 7.8 rad/s against the
     * skin's 40, so it is the only restoring force the low floppy modes have and nowhere near
     * enough to stiffen the body against a hit. With {@link #DAMP} it is underdamped, so a squash
     * rings about twice before it settles — that ringing is the jiggle, and it is why this is a
     * spring rather than a decay applied to the shape.
     */
    private static final float KR = 60f;
    /** Centring pull on the centroid, and its damping. About 8 rad/s — a lazy half second home. */
    private static final float KC = 70f, CENTRE_DAMP = 6f;
    /**
     * Whole-body velocity damping. Tuned against the ringing: at 2 a hit is still visibly swinging
     * three seconds later, which on a boss being hit several times a second is a body that never
     * has a shape. At 4.2 a hit is spent in about three quarters of a second, roughly one press.
     */
    private static final float DAMP = 4.2f;

    /**
     * Idle breath, as an acceleration per unit of {@link #rest}. Divided by {@link #KR} — not by
     * the skin's much higher frequency, since the harmonics it drives are exactly the floppy modes
     * {@link #KR} is the only restoring force for — it comes out at 2.7% of the radius, a third of
     * the lightest hit worth drawing.
     *
     * Worth stating, because reading it against the spring frequency instead is how this was first
     * mistuned: the number then looked ten times too small, and the first value tried breathed at a
     * third of the radius — three times what a full hit does, on a body that was standing still.
     */
    private static final float WOBBLE = 2.4f;
    /**
     * Which harmonics of the ring the breath drives, and their mix. Not per-node noise, which was
     * the first attempt: a force with an independent phase per node drives the shortest wavelength
     * the ring can carry, and that looks like a crinkled outline rather than a breath. Two low
     * harmonics travelling opposite ways give the slow lopsided undulation of something alive.
     */
    private static final float BREATH_M1 = 2f, BREATH_M2 = 3f, BREATH_MIX = 0.62f;
    /** How far a hit reaches, as a multiple of {@link #rest}. */
    private static final float REACH = 1.1f;
    /**
     * Speed a full-strength hit gives the nodes it lands on, per unit of {@link #rest}. A strength
     * of 1 lands at 0.19 of deform and is spent in about a second; 0.3 lands at 0.07. Callers are
     * expected to pass a fraction for an ordinary press and save 1 for something that matters.
     */
    private static final float PUNCH = 14f;
    /**
     * Vertical squash speed per unit of offset, and how much of it goes sideways. A strength of 1
     * flattens the body to half again as wide as it is tall a sixth of a second in, and the rebound
     * overshoots to 12% <em>taller</em> than round before it settles. That overshoot is not
     * scripted, which is the point of giving this as a velocity rather than a displacement: the
     * springs and the pressure answer it, so two squashes in quick succession add up.
     */
    private static final float SQUASH_V = 2.4f, SPREAD = 0.5f;
    /**
     * How much further the leading side travels than the trailing side during a {@link #moveTo} —
     * the only thing that makes a carried blob look carried rather than repositioned. The smear is
     * a steady state, not a one-off, since the lean is injected every frame the body moves and
     * {@link #KR} pulls it back the whole time: a boss drifting at a radius a second sits about a
     * tenth wider than tall for as long as it moves, and is round again a second after stopping.
     */
    private static final float LEAN = 0.30f;
    /**
     * Closest a node may come to the centroid, and furthest from home, as multiples of
     * {@link #rest}. Neither is physics — they are the guard rails. The inner one stops a hard hit
     * pushing a node through the middle and out the far side, which tangles the ring and inverts its
     * winding, at which point pressure pulls instead of pushing and the body is inside out for
     * good. The outer one bounds the coordinates whatever a caller feeds in.
     */
    private static final float INNER = 0.22f, FAR = 3f;

    final int n;
    private final float[] x, y, vx, vy, fx, fy;
    private final float[] out;
    /** Each node's fixed place round the ring, in radians. The breath is a harmonic of this. */
    private final float[] theta;
    /** Breath rates and phases, hashed off the caller's seed so two bosses do not breathe in step. */
    private final float wr1, wp1, wr2, wp2;

    /** Where the body is being carried to, and the radius it was settled at. */
    float homeX, homeY, rest;
    /** Area of the settled ring. The pressure term measures against this, not against pi r². */
    private float restArea, restLen;
    /** Accumulated from {@code dt}, never from a clock. Drives the idle breath. */
    private float clock;

    /**
     * How much of the idle breath to apply, 0..1. A field rather than a constant because a body
     * that is meant to hold still — a boss frozen for a tableau, or a settling assertion — has to
     * be able to actually hold still.
     */
    float idle = 1f;

    /** Where the skin is being tugged to, and how hard. Zero strength means nothing is pulling. */
    private float pullX, pullY, pullK;
    /** The tug target after clamping, which is where it is actually applied. See {@link #PULL_SPAN}. */
    private float pullTX, pullTY;

    /**
     * How hard a full-strength {@link #pull} tugs, as an acceleration per unit of radius.
     *
     * Well under {@link #KR}'s pull toward round times the reach, so a tug stretches the skin into a
     * teardrop and cannot turn the body inside out however long it is held. That bound is what makes
     * it safe to call every frame for as long as a finger is down.
     */
    private static final float TUG = 90f;
    /**
     * How far from the centroid a tug target is allowed to be, in units of {@link #rest}.
     *
     * The target is clamped to this rather than the pull being switched off out of range, and that
     * distinction is the whole effect. Testing the raw target against {@link #REACH} meant the tug
     * stopped applying the moment the thing being dragged got further than a radius away — so the
     * skin gave a small twitch at the start of a drag and then let go, which is the opposite of the
     * intended read. Clamped, the skin stretches out to about two radii and stays there for as long
     * as the finger holds, and everything past that is the drag having plainly won.
     */
    private static final float PULL_SPAN = 1.4f;

    // Measured once per update, so the getters are free.
    private float cx, cy, sarea, meanR, wobble, minX, maxX, minY, maxY, motion;

    Softbody() {
        this(NODES, 0);
    }

    /**
     * @param seed picks the breath's rates and phases. An int rather than a {@code Random} on
     *     purpose: the caller has to be able to name it, so a body always breathes the same way for
     *     a given boss and the frame hashes hold. Two bodies with the same seed are identical.
     */
    Softbody(int nodes, int seed) {
        n = Math.max(6, nodes);
        x = new float[n];
        y = new float[n];
        vx = new float[n];
        vy = new float[n];
        fx = new float[n];
        fy = new float[n];
        theta = new float[n];
        out = new float[n * SMOOTH * 2];
        for (int i = 0; i < n; i++) theta[i] = TAU * i / n;
        wr1 = 0.9f + 0.7f * Draw.hash(seed * 7 + 11);
        wp1 = Draw.hash(seed * 13 + 3) * TAU;
        wr2 = 1.5f + 1.1f * Draw.hash(seed * 29 + 5);
        wp2 = Draw.hash(seed * 41 + 17) * TAU;
        reset(0f, 0f, 1f);
    }

    /**
     * Settles the body into a circle at {@code cx,cy} of radius {@code r}, at rest.
     *
     * The rest length and rest area are both taken from the regular {@code n}-gon rather than from
     * the circle it stands in for, and that is the difference between a body that sits still and
     * one that hums: with {@code pi r²} as the target, pressure is never zero at rest, so the blob
     * inflates until the springs stop it and then oscillates about wherever that was.
     */
    void reset(float cx, float cy, float r) {
        homeX = cx;
        homeY = cy;
        pullK = 0f;
        rest = Math.max(1e-3f, r);
        restLen = 2f * rest * (float) Math.sin(Math.PI / n);
        restArea = 0.5f * n * rest * rest * (float) Math.sin(TAU / n);
        clock = 0f;
        for (int i = 0; i < n; i++) {
            double a = TAU * i / n;
            x[i] = cx + rest * (float) Math.cos(a);
            y[i] = cy + rest * (float) Math.sin(a);
            vx[i] = 0f;
            vy[i] = 0f;
        }
        measure();
    }

    /**
     * One step. Clamped and substepped; see the stability note on the class.
     *
     * Two rules and no third one. Anything that is not a positive number is refused outright and
     * changes nothing — zero, a negative, a NaN. Anything positive that is too big is
     * <em>clamped</em>, and that includes an infinity: a long frame is a slow frame, and there is
     * no length at which that stops being the answer. {@code TestSoftbody} pins both.
     */
    void update(float dt) {
        // The negated form also catches NaN, since every comparison against it is false.
        if (!(dt > 0f)) return;
        if (dt > MAX_DT) dt = MAX_DT;
        int steps = (int) Math.ceil(dt / STEP);
        if (steps < 1) steps = 1;
        if (steps > MAX_STEPS) steps = MAX_STEPS;
        float h = dt / steps;
        for (int s = 0; s < steps; s++) step(h);
        measure();
        // Belt and braces. A sim that has reached NaN draws nothing, forever, and gives no clue
        // why; a body that snaps back to a circle is one bad frame and then it is playable again.
        if (!finite()) reset(homeX, homeY, rest);
    }

    private void step(float h) {
        clock += h;
        survey();
        float area = Math.abs(sarea);
        if (area < 1e-4f) area = 1e-4f;
        float ratio = restArea / area - 1f;
        // Capped both ways so a degenerate area cannot produce a force big enough to leave the
        // world in one step. Being over-inflated is bounded naturally; being crushed is not.
        if (ratio > 4f) ratio = 4f;
        if (ratio < -1f) ratio = -1f;
        float press = KP * ratio * n / TAU;
        // The winding, so the edge normals point out of the body rather than into it. Taken from
        // the sign of the signed area, which is one test for the whole ring instead of a
        // dot-product per edge.
        float wind = sarea < 0f ? -1f : 1f;

        for (int i = 0; i < n; i++) {
            fx[i] = 0f;
            fy[i] = 0f;
        }
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            float dx = x[j] - x[i], dy = y[j] - y[i];
            // Pressure on this edge is P times its length along its unit outward normal, and
            // (dy,-dx) is already that normal times that length — so no square root is needed
            // here at all. Split half to each end.
            float pnx = wind * dy * press * 0.5f, pny = -wind * dx * press * 0.5f;
            fx[i] += pnx;
            fy[i] += pny;
            fx[j] += pnx;
            fy[j] += pny;

            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-5f) continue;
            float ux = dx / len, uy = dy / len;
            float rel = (vx[j] - vx[i]) * ux + (vy[j] - vy[i]) * uy;
            float f = KS * (len - restLen) + SPRING_DAMP * rel;
            fx[i] += f * ux;
            fy[i] += f * uy;
            fx[j] -= f * ux;
            fy[j] -= f * uy;
        }

        float mvx = 0f, mvy = 0f;
        for (int i = 0; i < n; i++) {
            mvx += vx[i];
            mvy += vy[i];
        }
        mvx /= n;
        mvy /= n;
        // Uniform, so it reaches the centre of mass and nothing else. Damped on the mean velocity
        // rather than each node's, or it would fight the wobble on the way home.
        float hx = KC * (homeX - cx) - CENTRE_DAMP * mvx;
        float hy = KC * (homeY - cy) - CENTRE_DAMP * mvy;

        // Where the tug actually acts: the target, held to PULL_SPAN of the centroid. Computed once
        // per substep rather than per node, and from the centroid rather than from home, so a body
        // that has already been shoved off centre stretches from where it is.
        if (pullK > 0f) {
            float ddx = pullX - cx, ddy = pullY - cy;
            float dd = (float) Math.sqrt(ddx * ddx + ddy * ddy);
            float cap = rest * PULL_SPAN;
            if (dd > cap && dd > 1e-4f) {
                pullTX = cx + ddx / dd * cap;
                pullTY = cy + ddy / dd * cap;
            } else {
                pullTX = pullX;
                pullTY = pullY;
            }
        }

        for (int i = 0; i < n; i++) {
            float ax = fx[i] + hx - DAMP * vx[i];
            float ay = fy[i] + hy - DAMP * vy[i];
            if (pullK > 0f) {
                // Toward the tug point, not outward from the centre: what is wanted is the skin
                // following the finger, which is a direction the body cannot supply on its own.
                float tx = pullTX - x[i], ty = pullTY - y[i];
                float td = (float) Math.sqrt(tx * tx + ty * ty);
                float reach = rest * REACH;
                if (td < reach && td > 1e-4f) {
                    float f = 1f - td / reach;
                    f *= f;
                    // Times rest, like every other force here: the sim is scale-invariant by
                    // construction and a bare px/s² would tug a thumbnail across the screen and
                    // barely dimple a full-size boss.
                    float a = pullK * TUG * rest * f;
                    ax += a * tx / td;
                    ay += a * ty / td;
                }
            }
            float dx = x[i] - cx, dy = y[i] - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1e-4f) {
                // Round, and the breath that rides on it. Both are radial, so they share the one
                // normalisation.
                float radial = KR * (rest - d);
                if (idle > 0f) radial += idle * WOBBLE * rest * breath(i);
                ax += radial * dx / d;
                ay += radial * dy / d;
            }
            vx[i] += ax * h;
            vy[i] += ay * h;
            x[i] += vx[i] * h;
            y[i] += vy[i] * h;
        }
        constrain();
    }

    /**
     * Two harmonics travelling in opposite directions round the ring, at hashed rates and phases:
     * unpredictable to look at, identical every run. The {@code Cabinet.shimmer} shape.
     */
    private float breath(int i) {
        return BREATH_MIX * (float) Math.sin(BREATH_M1 * theta[i] + clock * wr1 + wp1)
                + (1f - BREATH_MIX) * (float) Math.sin(BREATH_M2 * theta[i] - clock * wr2 + wp2);
    }

    private void constrain() {
        float inner = rest * INNER, far = rest * FAR;
        for (int i = 0; i < n; i++) {
            float dx = x[i] - cx, dy = y[i] - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1e-5f && d < inner) {
                float k = inner / d;
                x[i] = cx + dx * k;
                y[i] = cy + dy * k;
                // Only the inward part of the velocity is taken. The tangential part is the wobble
                // travelling round the ring, and killing that turns a hard hit into a dead spot.
                float nx = dx / d, ny = dy / d;
                float rv = vx[i] * nx + vy[i] * ny;
                if (rv < 0f) {
                    vx[i] -= rv * nx;
                    vy[i] -= rv * ny;
                }
            }
            float ox = x[i] - homeX, oy = y[i] - homeY;
            float od = (float) Math.sqrt(ox * ox + oy * oy);
            if (od > far) {
                float k = far / od;
                x[i] = homeX + ox * k;
                y[i] = homeY + oy * k;
                vx[i] *= 0.5f;
                vy[i] *= 0.5f;
            }
        }
    }

    /** Centroid and signed area. Needed inside every substep, so it is its own pass. */
    private void survey() {
        float sx = 0f, sy = 0f, a = 0f;
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            sx += x[i];
            sy += y[i];
            a += x[i] * y[j] - x[j] * y[i];
        }
        cx = sx / n;
        cy = sy / n;
        sarea = a * 0.5f;
    }

    private void measure() {
        survey();
        float sum = 0f, mv = 0f;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float dx = x[i] - cx, dy = y[i] - cy;
            sum += (float) Math.sqrt(dx * dx + dy * dy);
            mv += (float) Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            if (x[i] < minX) minX = x[i];
            if (x[i] > maxX) maxX = x[i];
            if (y[i] < minY) minY = y[i];
            if (y[i] > maxY) maxY = y[i];
        }
        meanR = sum / n;
        motion = mv / n;
        float v = 0f;
        for (int i = 0; i < n; i++) {
            float dx = x[i] - cx, dy = y[i] - cy;
            float e = (float) Math.sqrt(dx * dx + dy * dy) - meanR;
            v += e * e;
        }
        wobble = meanR > 1e-4f ? (float) Math.sqrt(v / n) / meanR : 0f;
    }

    /**
     * A hit at {@code px,py}. Positive {@code strength} dents the surface inward, which is what a
     * press looks like; negative bulges it out, for something bursting from inside.
     *
     * The falloff is squared so the dent lands where the hit did. Linear falloff spreads a press
     * over half the ring and the body just gets smaller for a moment, which reads as the whole
     * boss flinching rather than as a spot being struck.
     */
    void impulse(float px, float py, float strength) {
        if (strength > 3f) strength = 3f;
        if (strength < -3f) strength = -3f;
        float reach = rest * REACH, r2 = reach * reach;
        for (int i = 0; i < n; i++) {
            float dx = x[i] - px, dy = y[i] - py;
            float d2 = dx * dx + dy * dy;
            if (d2 >= r2) continue;
            float f = 1f - (float) Math.sqrt(d2) / reach;
            f *= f;
            float ox = cx - x[i], oy = cy - y[i];
            float d = (float) Math.sqrt(ox * ox + oy * oy);
            if (d < 1e-4f) continue;
            float v = strength * PUNCH * rest * f;
            vx[i] += v * ox / d;
            vy[i] += v * oy / d;
        }
    }

    /**
     * A sustained tug on the nearest part of the ring, toward {@code px,py}.
     *
     * Unlike {@link #impulse}, which is one kick, this is meant to be called every frame for as long
     * as something is pulling — a glob being dragged out of the body — and it stretches the skin
     * toward the finger until the springs and the pressure balance it. Stop calling it and the body
     * springs back and rings on its own, which is exactly the rebound wanted when the glob comes
     * free, so there is nothing to schedule for that.
     *
     * Held as a target and applied inside the solver rather than added to velocity here, because a
     * per-frame velocity kick is a force whose strength depends on the frame rate: at 120fps it would
     * pull twice as hard. {@link #letGo} clears it.
     *
     * The reach is deliberately the same {@link #REACH} the punch uses, and the falloff is squared
     * for the same reason — a tug that grabs half the ring moves the whole body instead of stretching
     * a spot on it, and the centring force would then simply fight it.
     */
    void pull(float px, float py, float strength) {
        pullX = px;
        pullY = py;
        pullK = strength < 0f ? 0f : strength > 3f ? 3f : strength;
    }

    /** Lets the skin go. The spring back is the solver's, not an animation. */
    void letGo() {
        pullK = 0f;
    }

    /** True while something is stretching the skin. */
    boolean pulled() {
        return pullK > 0f;
    }

    /** Nodes in this body's ring, and where node {@code i} currently is. */
    int nodes() {
        return n;
    }

    float nodeX(int i) {
        return x[((i % n) + n) % n];
    }

    float nodeY(int i) {
        return y[((i % n) + n) % n];
    }

    /**
     * A whole-body vertical squash and rebound: a landing, or a slam. Momentum-neutral by
     * construction — the offsets it scales sum to zero over the ring, so a squash never moves the
     * body. See {@link #SQUASH_V} for why this is a velocity and not a displacement.
     */
    void squash(float amount) {
        for (int i = 0; i < n; i++) {
            vy[i] -= amount * SQUASH_V * (y[i] - cy);
            vx[i] += amount * SQUASH_V * SPREAD * (x[i] - cx);
        }
    }

    /**
     * Carries the body to a new centre.
     *
     * Every node gets the whole delta, which is what keeps the wobble: a rigid translation leaves
     * every node's offset from the centre exactly as it was, where re-seeding the ring at the new
     * place would throw away whatever the body was in the middle of doing. The {@link #LEAN} on top
     * of that is what makes it read as carried rather than repositioned.
     *
     * The lean is capped, because this method serves two callers that are indistinguishable from in
     * here: a few pixels of drift a frame, and a one-off reposition of half a screen. Uncapped, the
     * second tears the ring apart along the direction of travel. Past the cap it is a reposition and
     * the body is simply carried.
     */
    void moveTo(float tx, float ty) {
        float dx = tx - homeX, dy = ty - homeY;
        homeX = tx;
        homeY = ty;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6f) return;
        float ux = dx / len, uy = dy / len;
        float lean = LEAN * len;
        if (lean > rest * 0.30f) lean = rest * 0.30f;
        for (int i = 0; i < n; i++) {
            // How far along the travel this node sits, as a fraction of the radius. Sums to about
            // zero over the ring, so the centroid still tracks the delta exactly.
            float u = ((x[i] - cx) * ux + (y[i] - cy) * uy) / rest;
            if (u > 1f) u = 1f;
            if (u < -1f) u = -1f;
            x[i] += dx + ux * lean * u;
            y[i] += dy + uy * lean * u;
        }
        measure();
    }

    /**
     * The body as a smooth closed outline, ready for {@link Painter#fillPoly}.
     *
     * Catmull-Rom through the nodes rather than Chaikin, for one reason: Chaikin cuts corners, so a
     * subdivided ring is smaller than the ring it came from and the body would draw a little inside
     * its own physics. This passes through every node, so what is drawn is where the sim says the
     * surface is.
     *
     * <b>Returns a reused buffer.</b> Copy it if you mean to keep it — a fresh 144-float array per
     * body per frame is garbage a phone does not need.
     */
    float[] outline() {
        int k = 0;
        for (int i = 0; i < n; i++) {
            int i0 = (i + n - 1) % n, i2 = (i + 1) % n, i3 = (i + 2) % n;
            for (int s = 0; s < SMOOTH; s++) {
                float t = (float) s / SMOOTH;
                out[k++] = spline(x[i0], x[i], x[i2], x[i3], t);
                out[k++] = spline(y[i0], y[i], y[i2], y[i3], t);
            }
        }
        return out;
    }

    private static float spline(float a, float b, float c, float d, float t) {
        return b + 0.5f * t * ((c - a)
                + t * ((2f * a - 5f * b + 4f * c - d) + t * (3f * b - 3f * c + d - a)));
    }

    float centreX() { return cx; }
    float centreY() { return cy; }
    /** Mean distance from the centroid to a node: the body's current radius. */
    float radius() { return meanR; }
    /**
     * The spread of the node radii about their mean, as a fraction of that mean: 0 for a settled
     * circle, 0.19 for a full hit. For driving a rim brightness or a wetness, which is all it is
     * meant for — a look, not a measurement.
     */
    float deform() { return wobble; }
    /** Mean node speed, in px/s. Zero when the body has settled. */
    float motion() { return motion; }
    float area() { return Math.abs(sarea); }
    /** The area the pressure term is holding it at. */
    float restArea() { return restArea; }
    float spanX() { return maxX - minX; }
    float spanY() { return maxY - minY; }
    /** Width over height. Above 1 it is squashed flat, below 1 it is stretched tall. */
    float aspect() { return spanY() > 1e-4f ? spanX() / spanY() : 1f; }

    /** True while every coordinate and velocity is a real number. */
    boolean finite() {
        for (int i = 0; i < n; i++) {
            if (!real(x[i]) || !real(y[i]) || !real(vx[i]) || !real(vy[i])) return false;
        }
        return true;
    }

    private static boolean real(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }
}
