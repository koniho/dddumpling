package com.sram.hexatype;

/**
 * The boss slime's soft body: that pressure holds its area, that it settles, that it cannot be
 * blown up, and that it renders the same way twice.
 *
 * The last one is why this suite exists at all. Everything else here could be taken to the device
 * and looked at; determinism cannot, and a sim that drifts by a bit between runs turns every PNG
 * hash comparison in the harness into noise and costs the eyes described in CLAUDE.md.
 */
final class TestSoftbody extends Check {

    static void physics(Layout L) {
        shapeAndSettling(L);
        pressureAndHits(L);
        carrying(L);
        stability(L);
        determinism(L);
    }

    private static void shapeAndSettling(Layout L) {
        group("soft body shape and settling");
        float r = L.unit * 6f;
        float cx = L.w * 0.5f, cy = L.h * 0.35f;

        Softbody b = new Softbody();
        b.reset(cx, cy, r);
        float[] o = b.outline();
        check("the outline is an even-length flat point array",
                o.length % 2 == 0 && o.length == b.n * Softbody.SMOOTH * 2);
        check("and every coordinate is a real number", finite(o));
        check("a fresh body is centred where it was put",
                near(b.centreX(), cx, 0.01f) && near(b.centreY(), cy, 0.01f));
        check("and is a circle of the radius asked for", near(b.radius(), r, r * 0.001f)
                && b.deform() < 0.001f);
        // The rest area is the polygon's, not the circle's. If it were pi r squared the pressure
        // term would be pushing outward on a body that is already at rest, and the thing would hum
        // for as long as it existed.
        check("a settled ring is exactly the area pressure is holding it at",
                near(b.area(), b.restArea(), b.restArea() * 0.002f));

        // The idle breath is decoration and it never stops, so a settling assertion has to be able
        // to switch it off — that is what the field is for.
        b.idle = 0f;
        b.reset(cx, cy, r);
        for (int i = 0; i < 60 * 3; i++) b.update(DT);
        check("released undisturbed it does not move at all", b.motion() < r * 0.001f
                && b.deform() < 0.001f);

        Softbody alive = new Softbody();
        alive.reset(cx, cy, r);
        float most = 0f;
        for (int i = 0; i < 60 * 6; i++) {
            alive.update(DT);
            most = Math.max(most, alive.deform());
        }
        // Tuned against how a still boss reads. The breath has to be visible and must never be
        // mistaken for a hit: the lightest hit worth drawing lands around 0.07 of deform, so the
        // ceiling here is a third of that. The first attempt breathed at 0.33 — three times what a
        // full-strength hit does — which is why this is measured rather than eyeballed.
        System.out.printf("    the idle breath reaches %.3f of deform, the lightest hit 0.07%n",
                most);
        check("the idle breath is alive but is not a hit", most > 0.010f && most < 0.045f);
        check("and it stays where it was put", near(alive.centreX(), cx, r * 0.05f)
                && near(alive.centreY(), cy, r * 0.05f));

        Softbody few = new Softbody(10, 0);
        few.reset(cx, cy, r);
        check("the node count is configurable", few.n == 10
                && few.outline().length == 10 * Softbody.SMOOTH * 2);
    }

    private static void pressureAndHits(Layout L) {
        group("soft body pressure and hits");
        float r = L.unit * 6f;
        float cx = L.w * 0.5f, cy = L.h * 0.35f;

        Softbody b = new Softbody();
        b.idle = 0f;
        b.reset(cx, cy, r);
        // Struck on the left flank, where a thumb would put it.
        b.impulse(cx - r, cy, 1f);
        float lo = b.restArea(), hi = b.restArea(), peak = 0f;
        for (int i = 0; i < 60; i++) {
            b.update(DT);
            lo = Math.min(lo, b.area());
            hi = Math.max(hi, b.area());
            peak = Math.max(peak, b.deform());
        }
        System.out.printf("    a hit dents it to %.3f of deform and its area swings %.0f%%"
                + " to %.0f%% of rest%n", peak, 100f * lo / b.restArea(), 100f * hi / b.restArea());
        check("a hit visibly deforms it", peak > 0.08f);
        // The whole reason for the pressure term. Skin springs alone would let this collapse to
        // nothing, since a folded ring has the same perimeter as a round one.
        check("pressure holds its area through a hit", lo > b.restArea() * 0.75f
                && hi < b.restArea() * 1.25f);

        for (int i = 0; i < 60 * 3; i++) b.update(DT);
        check("and the dent decays away", b.deform() < 0.005f
                && near(b.area(), b.restArea(), b.restArea() * 0.005f));
        check("leaving it back where it was hit from",
                near(b.centreX(), cx, r * 0.02f) && near(b.centreY(), cy, r * 0.02f));

        // Reach: a hit nowhere near it does nothing, or a press anywhere on the field would wobble
        // every boss on screen.
        Softbody miss = new Softbody();
        miss.idle = 0f;
        miss.reset(cx, cy, r);
        miss.impulse(cx + r * 5f, cy, 1f);
        for (int i = 0; i < 20; i++) miss.update(DT);
        check("a hit out of reach does nothing", miss.deform() < 0.001f);

        Softbody sq = new Softbody();
        sq.idle = 0f;
        sq.reset(cx, cy, r);
        sq.squash(1f);
        float flat = 1f, tall = 1f;
        for (int i = 0; i < 40; i++) {
            sq.update(DT);
            flat = Math.max(flat, sq.aspect());
            tall = Math.min(tall, sq.aspect());
        }
        System.out.printf("    a squash flattens it to %.2f wide and rebounds to %.2f%n",
                flat, tall);
        check("a squash flattens the body", flat > 1.15f);
        // The rebound is the springs' own answer rather than a scripted return, so it overshoots
        // into a stretch. If it did not, the squash would read as a scale animation.
        check("and it rebounds past round into a stretch", tall < 0.97f);
        check("a squash never moves the body", near(sq.centreX(), cx, r * 0.01f)
                && near(sq.centreY(), cy, r * 0.01f));

        // The one the pull toward round exists for, and it has to be measured on the shape rather
        // than on the radii: a flattening that keeps the area is nearly free for a pressurised
        // ring, so before that term went in a single squash was still 8% flat four seconds later
        // and 6% flat after twenty, creeping away like one over root t instead of decaying. On a
        // boss taking hits for thirty seconds that is a body that goes quietly lopsided and stays
        // that way, and nothing in a per-frame assertion would have noticed.
        Softbody round = new Softbody();
        round.idle = 0f;
        round.reset(cx, cy, r);
        float wasRound = round.aspect();
        round.squash(0.6f);
        for (int i = 0; i < 60 * 2; i++) round.update(DT);
        System.out.printf("    two seconds after a squash it is back to %.3f of %.3f round%n",
                round.aspect(), wasRound);
        check("a squash leaves no permanent flatten", near(round.aspect(), wasRound, 0.02f)
                && round.deform() < 0.005f);

        // The same guarantee over a whole encounter. A boss lives thirty seconds and is hit several
        // times a second the entire time.
        Softbody fight = new Softbody();
        fight.idle = 0f;
        fight.reset(cx, cy, r);
        for (int i = 0; i < 60 * 30; i++) {
            if (i % 20 == 0) {
                double a = Softbody.TAU * (i / 20) * 0.31f;
                fight.impulse(cx + r * (float) Math.cos(a), cy + r * (float) Math.sin(a), 0.7f);
            }
            fight.update(DT);
        }
        for (int i = 0; i < 60 * 3; i++) fight.update(DT);
        check("and a whole encounter of hits leaves it round again",
                near(fight.aspect(), wasRound, 0.03f) && fight.deform() < 0.01f
                        && near(fight.area(), fight.restArea(), fight.restArea() * 0.01f));
    }

    private static void carrying(Layout L) {
        group("soft body carrying");
        float r = L.unit * 6f;
        float cx = L.w * 0.5f, cy = L.h * 0.35f;

        Softbody b = new Softbody();
        b.idle = 0f;
        b.reset(cx, cy, r);
        b.impulse(cx - r, cy, 1f);
        for (int i = 0; i < 8; i++) b.update(DT);
        float was = b.deform(), moving = b.motion();
        check("there is a wobble in progress to carry", was > 0.05f && moving > 0f);

        // The shape, as each node's offset from the centre. A carry must leave these alone.
        float[] before = offsets(b);
        // Taken from where the body actually is rather than from where it was put: a hit carries
        // momentum, so a struck body is already sliding away from its home when the carry lands.
        float fromX = b.centreX(), fromY = b.centreY();
        float step = r * 0.15f;
        b.moveTo(cx + step, cy);
        check("a carry moves the body by exactly the delta",
                near(b.centreX(), fromX + step, r * 0.01f) && near(b.centreY(), fromY, r * 0.01f));
        float[] after = offsets(b);
        float drift = 0f;
        for (int i = 0; i < before.length; i++) drift = Math.max(drift, Math.abs(before[i] - after[i]));
        System.out.printf("    a %.0fpx carry leans the shape by %.1fpx%n", step, drift);
        check("and does not teleport the wobble", near(b.deform(), was, was * 0.15f));
        // The lean is what makes it read as a body being carried rather than a sprite being moved,
        // and it is bounded by what a drifting boss covers in a frame.
        check("it leans into the travel without tearing", drift > 0.01f && drift < step * 0.5f);

        // A whole-screen reposition goes through the same method as a frame of drift, and from in
        // here the two are indistinguishable — so the lean is capped or the big one shreds the ring.
        Softbody jump = new Softbody();
        jump.idle = 0f;
        jump.reset(cx, cy, r);
        jump.moveTo(cx + L.w, cy + L.h);
        check("a reposition of half a screen carries it whole", jump.deform() < 0.10f
                && near(jump.centreX(), cx + L.w, r * 0.05f));

        // Drifted a frame at a time, the body has to keep up with the thing it belongs to rather
        // than trailing off behind it.
        Softbody drifting = new Softbody();
        drifting.idle = 0f;
        drifting.reset(cx, cy, r);
        float at = cx;
        float smear = 0f;
        for (int i = 0; i < 60 * 2; i++) {
            at += r / 60f;                      // a radius a second, which is a brisk drift
            drifting.moveTo(at, cy);
            drifting.update(DT);
            smear = Math.max(smear, drifting.aspect());
        }
        System.out.printf("    drifting a radius a second it smears to %.3f wide, lagging %.2fpx%n",
                smear, at - drifting.centreX());
        // No lag at all is the requirement: the carry is a rigid translation, so a body cannot be
        // left behind however fast its owner moves. The smear is the only thing the speed buys.
        check("a body drifted every frame keeps up with its owner",
                near(drifting.centreX(), at, r * 0.02f));
        check("and smears out along its travel while it moves", smear > 1.05f && smear < 1.30f);
        for (int i = 0; i < 60; i++) drifting.update(DT);
        check("then pulls itself round again once it stops", drifting.aspect() < 1.04f);
    }

    private static void stability(Layout L) {
        group("soft body stability");
        float r = L.unit * 6f;
        float cx = L.w * 0.5f, cy = L.h * 0.35f;

        // Every abuse the game can offer, for longer than any boss lives. A boss is thirty seconds
        // and this is five minutes of being hit five times a second.
        Softbody b = new Softbody();
        b.reset(cx, cy, r);
        java.util.Random rnd = new java.util.Random(41L);
        boolean ok = true;
        float far = 0f;
        for (int i = 0; i < 60 * 300 && ok; i++) {
            if (i % 12 == 0) {
                double a = rnd.nextDouble() * Softbody.TAU;
                b.impulse(cx + r * (float) Math.cos(a), cy + r * (float) Math.sin(a),
                        1.5f * rnd.nextFloat());
            }
            if (i % 90 == 0) b.squash(1f + rnd.nextFloat());
            b.update(DT);
            ok = b.finite();
            float[] o = b.outline();
            for (int k = 0; k < o.length; k += 2) {
                far = Math.max(far, Math.abs(o[k] - cx));
                far = Math.max(far, Math.abs(o[k + 1] - cy));
            }
        }
        System.out.printf("    18000 abused steps stay inside %.1f radii of home%n", far / r);
        check("thousands of steps of abuse leave it finite", ok);
        check("and never let a coordinate run away", far < r * 4f);

        // The dt clamp. A long frame has to be a slow frame, and the cheapest way to state that is
        // that a minute-long frame is bit-identical to a clamped one — there is no third behaviour.
        Softbody huge = new Softbody();
        huge.reset(cx, cy, r);
        huge.impulse(cx - r, cy, 1f);
        Softbody capped = new Softbody();
        capped.reset(cx, cy, r);
        capped.impulse(cx - r, cy, 1f);
        huge.update(60f);
        capped.update(Softbody.MAX_DT);
        check("an absurd frame is clamped, not simulated", same(huge.outline(), capped.outline()));
        check("and leaves it finite and in place", huge.finite()
                && near(huge.centreX(), cx, r * 0.5f));

        // Junk in the clock. Two rules and no third one: anything that is not a positive number
        // changes nothing, and anything positive and too big is the clamp above — including an
        // infinity, which is a long frame like any other.
        Softbody junk = new Softbody();
        junk.reset(cx, cy, r);
        float[] still = copy(junk.outline());
        junk.update(0f);
        junk.update(-1f);
        junk.update(Float.NaN);
        check("a timestep that is not a positive number changes nothing", junk.finite()
                && same(junk.outline(), still));
        Softbody inf = new Softbody();
        inf.reset(cx, cy, r);
        inf.impulse(cx - r, cy, 1f);
        Softbody lim = new Softbody();
        lim.reset(cx, cy, r);
        lim.impulse(cx - r, cy, 1f);
        inf.update(Float.POSITIVE_INFINITY);
        lim.update(Softbody.MAX_DT);
        check("and an infinite one is the same clamp as any long frame",
                inf.finite() && same(inf.outline(), lim.outline()));

        // A body given a hit far past anything the game will ask for still has to come back.
        Softbody hard = new Softbody();
        hard.idle = 0f;
        hard.reset(cx, cy, r);
        for (int k = 0; k < 8; k++) hard.impulse(cx, cy - r, 50f);
        for (int i = 0; i < 60 * 4; i++) hard.update(DT);
        check("an impossible hit is survivable", hard.finite() && hard.deform() < 0.05f
                && near(hard.area(), hard.restArea(), hard.restArea() * 0.05f));
    }

    private static void determinism(Layout L) {
        group("soft body determinism");
        float r = L.unit * 6f;
        float cx = L.w * 0.5f, cy = L.h * 0.35f;

        // Two bodies, the same script, run interleaved so any shared mutable state between them
        // would show up. The idle breath is left on: it is the part that wants to look random and
        // therefore the part that would reach for an RNG if nobody was watching.
        Softbody a = new Softbody();
        Softbody b = new Softbody();
        a.reset(cx, cy, r);
        b.reset(cx, cy, r);
        boolean identical = true;
        for (int i = 0; i < 60 * 20 && identical; i++) {
            if (i % 17 == 0) {
                a.impulse(cx - r, cy + r * 0.3f, 0.8f);
                b.impulse(cx - r, cy + r * 0.3f, 0.8f);
            }
            if (i % 53 == 0) {
                a.squash(0.7f);
                b.squash(0.7f);
            }
            if (i % 7 == 0) {
                a.moveTo(cx + r * 0.01f * (i % 40), cy);
                b.moveTo(cx + r * 0.01f * (i % 40), cy);
            }
            a.update(DT);
            b.update(DT);
            identical = same(a.outline(), b.outline());
        }
        check("two bodies given the same inputs are bit-identical", identical);

        // And the same body twice over, since the harness's guarantee is between runs rather than
        // between instances: nothing may carry over from one life of a body into the next.
        Softbody again = new Softbody();
        again.reset(cx, cy, r);
        for (int i = 0; i < 90; i++) again.update(DT);
        float[] first = copy(again.outline());
        again.reset(cx, cy, r);
        for (int i = 0; i < 90; i++) again.update(DT);
        check("and a reset body repeats its own history exactly",
                same(again.outline(), first));
    }

    // ---- helpers ------------------------------------------------------------

    /** Each node's offset from the centroid, as the shape independent of where it is. */
    private static float[] offsets(Softbody b) {
        float[] o = b.outline();
        float[] d = new float[o.length];
        for (int i = 0; i < o.length; i += 2) {
            d[i] = o[i] - b.centreX();
            d[i + 1] = o[i + 1] - b.centreY();
        }
        return d;
    }

    private static float[] copy(float[] a) {
        float[] c = new float[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    /** Bit-identical, not merely close: this is the assertion the frame hashes depend on. */
    private static boolean same(float[] a, float[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(b[i])) return false;
        }
        return true;
    }

    private static boolean finite(float[] a) {
        for (int i = 0; i < a.length; i++) {
            if (Float.isNaN(a[i]) || Float.isInfinite(a[i])) return false;
        }
        return true;
    }

    private static boolean near(float got, float want, float tol) {
        return Math.abs(got - want) <= tol;
    }
}
