package com.sram.hexatype;

/**
 * The boss on screen: its body, its health, whatever it is asking for, and the things it has put on
 * the field to be tapped and dragged.
 *
 * The body itself is {@link Slime} over the boss's {@link Softbody}, for all five of them — they are
 * all soft creatures, and a thing that dents where you hit it is worth more than any amount of
 * flashing. What differs per boss is the ornament: a chain letter, three heads, a beat marker, a
 * stolen key, a belt.
 *
 * Everything here reads {@code Boss}'s element arrays for position rather than working them out
 * again, because the hit-test reads the same arrays — two copies of that arithmetic would drift, and
 * the symptom would be taps that miss what they plainly landed on.
 */
final class BossScreen extends Draw {

    private BossScreen() {}

    /** Tint per boss, taken from the letter each one is a giant version of. */
    private static int tint(Boss b) {
        return Glyph.COLOR[Boss.FACE[b.kind]];
    }

    // ---- header geometry ----------------------------------------------------
    // The health bar, the name and the blurb stack, and so does the wanted-letter badge under them,
    // and then the body under that. Four things in a column, which is four chances to put text on
    // top of text — the one fault DOES NOT FIT cannot see, because both lines fit perfectly well.
    // Every offset is therefore a named method and TestBoss asserts the gaps across a sweep of
    // screen sizes, exactly as TestVisuals does for the HUD's own stack. The first pass at this had
    // the name sitting on STAGE 5 and the blurb sitting on the badge, at the one size it was
    // eyeballed at.

    /** Top of the health bar, and how thick it is. */
    static float barY(Layout L) {
        return L.playTop + L.unit * 0.55f;
    }

    static float barH(Layout L) {
        return L.unit * 0.30f;
    }

    static float nameSize(Layout L) {
        return type(L.unit * 0.86f);
    }

    static float blurbSize(Layout L) {
        return type(L.unit * 0.5f);
    }

    /** Baseline of the boss's name, below the bar rather than above it. */
    static float nameY(Layout L) {
        return barY(L) + barH(L) + nameSize(L);
    }

    /** Baseline of the blurb under the name. Scaled with the type, or the two collide as TEXT grows. */
    static float blurbY(Layout L) {
        return nameY(L) + type(L.unit * 0.80f);
    }

    /** Radius of the wanted-letter badge the bosses that want a letter hold above themselves. */
    static float badgeR(Layout L) {
        return L.unit * 0.95f;
    }

    /**
     * Topmost pixel the badge reaches, caret included.
     *
     * The caret is drawn at {@code cy - r * 1.55} and is {@code r * 0.4} tall either side of that,
     * per {@link Draw#caret}; this has to agree with it, so if that mark ever changes shape this
     * number moves with it.
     */
    static float badgeTop(Layout L, float badgeCy) {
        float r = badgeR(L);
        return badgeCy - r * 1.55f - r * 0.4f;
    }

    /**
     * The body and its ornaments, drawn behind the falling words.
     *
     * Behind them deliberately: the boss is the backdrop of its stage and the words are what is
     * about to hurt you, so the words win every overlap.
     */
    static void body(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active() || b.body == null) return;

        // In on the arrival card, out on the burst.
        float fade = b.intro > 0f ? Math.min(1f, b.introProgress() * 1.6f)
                : b.beaten ? Math.max(0f, 1f - b.leaveProgress() * 1.3f) : 1f;
        if (fade <= 0.01f) return;

        int col = tint(b);
        // Rage reddens it; the enrage reddens it further and permanently, so a fight going badly
        // looks like one.
        col = Glyph.mix(col, ROSE, Math.max(b.rage * 0.55f, b.enrage() * 0.45f));
        // A hit whitens it for a moment on top of the dent the body is already taking.
        col = Glyph.mix(col, 0xFFFFFFFF, b.hurt * 0.35f);

        float r = b.body.radius();
        float cx = b.body.centreX(), cy = b.body.centreY();

        // An aura, so the body reads as lit rather than as a flat shape on the sky. Widest and
        // faintest first, and only three layers: this sits behind a soft outline that is already
        // translucent, and more than that greys the whole upper field.
        for (int k = 3; k >= 1; k--) {
            p.fillCircle(cx, cy, r * (1.05f + 0.16f * k),
                    Glyph.withAlpha(col, (int) ((22 + 26 * b.hurt) * fade / k)));
        }
        // While the window is open it glows: that is the whole tell for "it can be hurt now", and it
        // has to be readable without reading the bar.
        if (b.open()) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 7f);
            for (int k = 2; k >= 1; k--) {
                p.strokePoly(star(cx, cy, r * (1.2f + 0.24f * k), r * 0.9f, 10, c.clock * 0.6f),
                        Glyph.withAlpha(GOLD, (int) (70 * pulse * fade / k)), r * 0.045f);
            }
        }

        // The triplets are drawn faceless: their three heads sit on the body and are its face, so
        // giving the body one of its own put a fourth face behind the three and the whole thing read
        // as a blob wearing heads rather than as a creature with three of them.
        int face = b.kind == Boss.TRIPLETS ? -1 : Boss.FACE[b.kind];
        Slime.draw(p, b.body, c.clock, col, face, b.open() ? 0.15f : 0.5f, fade);
        // Its own structure, faintly, so the wobble reads as physics rather than as a wandering
        // outline. Brightest just after a hit and while the skin is being stretched — the two
        // moments there is something to see.
        Slime.mesh(p, b.body, col, Math.max(b.hurt, b.body.pulled() ? 0.8f : 0f), fade);

        ornament(p, c, L, b, fade);
        elements(p, c, L, b, fade);
    }

    /** Whatever this boss is asking for, drawn on or around the body. */
    private static void ornament(Painter p, GameCore c, Layout L, Boss b, float fade) {
        float r = b.body.radius();
        float cx = b.body.centreX(), cy = b.body.centreY();

        if (b.kind == Boss.SLIME) {
            // The next letter of the chain, held above it, with a caret so it reads as "press this"
            // in the same language the field's head tile uses.
            letterBadge(p, c, L, b.chainLetter(), cx, cy - r * 1.28f, L.unit * 0.95f, fade,
                    b.open());
        } else if (b.kind == Boss.TRIPLETS) {
            // The heads are elements, so they are drawn with them.
            return;
        } else if (b.kind == Boss.DRUM) {
            // The beat: a ring that closes as the window approaches, and the thing it wants inside.
            beatRing(p, c, b, cx, cy, r, fade);
            if (b.tapBeat) tapMark(p, c, cx, cy, r * 0.5f, fade, b.open());
            else letterBadge(p, c, L, b.want(), cx, cy - r * 1.28f, L.unit * 0.95f, fade, b.open());
        } else if (b.kind == Boss.MAGPIE) {
            letterBadge(p, c, L, b.want(), cx, cy - r * 1.28f, L.unit * 0.95f, fade, b.open());
            // The key it is holding, clear of the body's outline rather than over it — a struck-out
            // key drawn across the boss's own face read as damage to the boss instead of as
            // something it had taken. Upper left, away from the wanted letter above it.
            if (b.stolen >= 0) heldKey(p, c, L, b, cx - r * 1.20f, cy - r * 0.85f, fade);
        } else if (b.kind == Boss.SUMO) {
            letterBadge(p, c, L, b.want(), cx, cy - r * 1.28f, L.unit * 0.95f, fade, true);
            charges(p, c, L, b, cx, cy + r * 1.30f, fade);
            if (b.stagger > 0f) staggerRing(p, c, b, cx, cy, r, fade);
        }
    }

    /**
     * A letter the boss wants, on a hexagon, with a caret over it while it will be accepted.
     *
     * The caret is {@link Draw#caret}, the same mark the field puts over a head tile and the
     * interlude puts over its wanted letter. Three places, one mark: a player who has learnt it once
     * should not have to learn it again because this is a boss.
     */
    private static void letterBadge(Painter p, GameCore c, Layout L, int g, float x, float y,
            float rr, float fade, boolean live) {
        if (g < 0) return;
        int col = Glyph.COLOR[g];
        int a = (int) (fade * (live ? 255 : 130));
        float pulse = live ? 1f + 0.07f * (float) Math.sin(c.clock * 7f) : 1f;
        rr *= pulse;
        p.fillPoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, a * 60 / 255));
        p.strokePoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, a * 220 / 255), rr * 0.10f);
        Kawaii.draw(p, g, x, y, rr * 0.60f, Glyph.withAlpha(col, a), 1f, live ? 0.7f : 0.1f);
        if (live) caret(p, x, y, rr, Glyph.withAlpha(INK, (int) (225 * fade)));
    }

    /** The drum's beat: a ring that closes in on the body as the window comes round. */
    private static void beatRing(Painter p, GameCore c, Boss b, float cx, float cy, float r,
            float fade) {
        if (b.open()) {
            // Landed on the beat: a bright flare at the body's own size.
            p.strokeCircle(cx, cy, r * 1.15f, Glyph.withAlpha(GOLD, (int) (235 * fade)),
                    r * 0.09f);
            return;
        }
        // Shrinking toward the body, so the moment to act is when it arrives — the same read as a
        // rhythm game's approach ring, and it needs no explaining.
        float t = b.phaseProgress();
        float rr = r * (2.6f - 1.45f * t);
        p.strokeCircle(cx, cy, rr, Glyph.withAlpha(INK, (int) (40 + 150 * t * fade)), r * 0.05f);
    }

    /** The "tap here" mark: concentric rings and a fingertip dot, on the beats that want a tap. */
    private static void tapMark(Painter p, GameCore c, float x, float y, float rr, float fade,
            boolean live) {
        int a = (int) (fade * (live ? 255 : 120));
        for (int k = 1; k <= 2; k++) {
            float t = ((c.clock * 1.6f) + k * 0.5f) % 1f;
            p.strokeCircle(x, y, rr * (0.5f + t * 0.9f),
                    Glyph.withAlpha(INK, (int) (a * (1f - t) * 0.7f)), rr * 0.13f);
        }
        p.fillCircle(x, y, rr * 0.34f, Glyph.withAlpha(INK, a));
        p.fillCircle(x, y, rr * 0.16f, Glyph.withAlpha(0xFF2A2348, a));
    }

    /** The key the magpie is holding: drawn as a key hexagon with a bar across it. */
    private static void heldKey(Painter p, GameCore c, Layout L, Boss b, float x, float y,
            float fade) {
        int g = b.stolen;
        float rr = L.keyR * 0.62f;
        int col = Glyph.COLOR[g];
        int a = (int) (255 * fade);
        p.fillPoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, a * 55 / 255));
        p.strokePoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, a), rr * 0.11f);
        Kawaii.draw(p, g, x, y, rr * 0.58f, Glyph.withAlpha(col, a), 1f, 0f);
        // Struck through, so it reads as denied rather than as a decoration. A polygon, not a
        // character: the harness font has no such glyph and it would vanish from every frame.
        float k = rr * 0.95f;
        p.line(x - k, y - k * 0.75f, x + k, y + k * 0.75f,
                Glyph.withAlpha(ROSE, a), rr * 0.16f);
    }

    /** The sumo's banked swipes, as pips under it. */
    private static void charges(Painter p, GameCore c, Layout L, Boss b, float cx, float y,
            float fade) {
        float rr = L.unit * 0.26f, gap = rr * 2.6f;
        float x0 = cx - gap * (Boss.CHARGE_MAX - 1) / 2f;
        for (int i = 0; i < Boss.CHARGE_MAX; i++) {
            boolean have = i < b.charges;
            float pulse = have && b.open() ? 1f + 0.16f * (float) Math.sin(c.clock * 8f) : 1f;
            p.fillPoly(star(x0 + i * gap, y, rr * pulse, rr * 0.42f, 5, c.clock * 0.5f),
                    Glyph.withAlpha(have ? GOLD : INK, (int) ((have ? 235 : 45) * fade)));
        }
    }

    /** A staggered sumo, ringed so the doubled shove is visibly available. */
    private static void staggerRing(Painter p, GameCore c, Boss b, float cx, float cy, float r,
            float fade) {
        float k = Math.min(1f, b.stagger / Boss.STAGGER_TIME);
        for (int i = 0; i < 3; i++) {
            float a = c.clock * 3.2f + i * 2.09f;
            p.fillCircle(cx + (float) Math.cos(a) * r * 1.25f,
                    cy + (float) Math.sin(a) * r * 0.7f, r * 0.13f,
                    Glyph.withAlpha(GOLD, (int) (200 * k * fade)));
        }
    }

    /** The tappable and draggable things a boss has put on the field. */
    private static void elements(Painter p, GameCore c, Layout L, Boss b, float fade) {
        for (int i = 0; i < Boss.ELEMS; i++) {
            int t = b.etype[i];
            if (t == Boss.E_OFF) continue;
            float x = b.ex[i], y = b.ey[i], rr = b.er[i];
            boolean held = b.held == i;
            // Everything that expires shows how long it has left, because the whole decision is
            // whether there is time to deal with it.
            float life = b.elife[i] > 0f ? b.elife[i] : 0f;

            if (t == Boss.E_HEAD) {
                head(p, c, L, b, i, x, y, rr, fade);
            } else if (t == Boss.E_SKIN) {
                // Drawn by beatRing/tapMark on the body; nothing extra here, or the body gets a
                // second outline nobody asked for.
                continue;
            } else if (t == Boss.E_GLOB) {
                glob(p, c, b, i, x, y, rr, life, held, fade);
            } else if (t == Boss.E_KEY) {
                loose(p, c, L, b, i, x, y, rr, life, held, fade);
            }
        }
    }

    /** One of the triplets' heads: asleep, awake, or already struck this chord. */
    private static void head(Painter p, GameCore c, Layout L, Boss b, int i, float x, float y,
            float rr, float fade) {
        int g = b.head(i);
        boolean awake = b.headAwake(i);
        boolean struck = b.headStruck(i);
        int col = g >= 0 ? Glyph.COLOR[g] : INK;
        int a = (int) (fade * (awake ? 255 : 150));

        if (!awake) {
            // Asleep: a dim closed shape with the tap mark over it, and no letter — the letter is
            // what tapping it reveals, so showing it would give the tap nothing to do.
            p.fillCircle(x, y, rr, Glyph.withAlpha(INK, (int) (26 * fade)));
            p.strokeCircle(x, y, rr, Glyph.withAlpha(INK, (int) (90 * fade)), rr * 0.09f);
            tapMark(p, c, x, y, rr * 0.55f, fade, true);
            return;
        }
        float pulse = struck ? 1f : 1f + 0.06f * (float) Math.sin(c.clock * 7f + i);
        // A dark core first. A head sits on the body it belongs to, and a pale character on a pale
        // body disappears into it — the same reason the TEAM SQUISH bubble has one behind its
        // squishy. Without it the middle head was barely findable, which matters when the whole
        // mechanic is reading three letters off them.
        p.fillCircle(x, y, rr * pulse, Glyph.withAlpha(BG, (int) (215 * fade)));
        p.fillCircle(x, y, rr * pulse, Glyph.withAlpha(col, a * 90 / 255));
        p.strokeCircle(x, y, rr * pulse, Glyph.withAlpha(struck ? GOLD : col, a), rr * 0.11f);
        Kawaii.draw(p, g, x, y, rr * 0.66f, Glyph.withAlpha(col, a), 1f, struck ? 1f : 0.4f);
        if (struck) {
            // Already taken: ringed gold and no caret, because pressing it again does nothing.
            p.strokeCircle(x, y, rr * 1.22f, Glyph.withAlpha(GOLD, (int) (150 * fade)),
                    rr * 0.06f);
        } else {
            caret(p, x, y, rr, Glyph.withAlpha(INK, (int) (225 * fade)));
        }
    }

    /**
     * A shed glob: the piece that came loose, to be hauled out of the body it split off from.
     *
     * Drawn rose and lit from within rather than in the body's own colour. It starts *inside* the
     * boss, so it has to be the one thing in there that is plainly not the boss — in the slime's own
     * mint it was a slightly different shade of the thing it was sitting in, and invisible.
     */
    private static void glob(Painter p, GameCore c, Boss b, int i, float x, float y, float rr,
            float life, boolean held, float fade) {
        int col = ROSE;
        float k = Math.min(1f, life / Boss.GLOB_TIME);
        // Wobbles on its own sines rather than an RNG, so preview frames still hash the same.
        float wx = 1f + 0.10f * (float) Math.sin(c.clock * 3.1f + hash(i * 17) * 6.283f);
        float wy = 1f + 0.10f * (float) Math.sin(c.clock * 3.7f + hash(i * 29) * 6.283f);
        // A red glow through the goo, pulsing, so it shows while it is still inside the body.
        float beat = 0.72f + 0.28f * (float) Math.sin(c.clock * 5.5f + i);
        for (int q = 3; q >= 1; q--) {
            p.fillCircle(x, y, rr * (1.15f + 0.30f * q),
                    Glyph.withAlpha(col, (int) (46 * beat * fade / q)));
        }
        if (held) {
            for (int q = 2; q >= 1; q--) {
                p.fillCircle(x, y, rr * (1.1f + 0.2f * q), Glyph.withAlpha(GOLD,
                        (int) (40 * fade / q)));
            }
        }
        p.fillEllipse(x, y, rr * wx, rr * wy, Glyph.withAlpha(col, (int) (215 * fade)));
        p.strokeCircle(x, y, rr, Glyph.withAlpha(Glyph.mix(col, 0xFFFFFFFF, 0.4f),
                (int) (235 * fade)), rr * 0.11f);
        p.fillEllipse(x - rr * 0.3f, y - rr * 0.34f, rr * 0.22f, rr * 0.14f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (140 * fade)));
        // How long before it crawls back: an arc of pips, emptying.
        ring(p, x, y, rr * 1.35f, k, ROSE, fade);
    }

    /** A key the magpie dropped, to be dragged home to the deck. */
    private static void loose(Painter p, GameCore c, Layout L, Boss b, int i, float x, float y,
            float rr, float life, boolean held, float fade) {
        int g = b.keyOf[i];
        if (g < 0) return;
        int col = Glyph.COLOR[g];
        float k = Math.min(1f, life / Boss.KEY_TIME);
        if (held) {
            for (int q = 2; q >= 1; q--) {
                p.fillCircle(x, y, rr * (1.2f + 0.25f * q),
                        Glyph.withAlpha(GOLD, (int) (46 * fade / q)));
            }
        }
        float bob = held ? 0f : (float) Math.sin(c.clock * 3.4f) * rr * 0.10f;
        p.fillPoly(Glyph.hex(x, y + bob, rr), Glyph.withAlpha(col, (int) (80 * fade)));
        p.strokePoly(Glyph.hex(x, y + bob, rr), Glyph.withAlpha(col, (int) (250 * fade)),
                rr * 0.11f);
        Kawaii.draw(p, g, x, y + bob, rr * 0.60f, Glyph.withAlpha(col, (int) (255 * fade)), 1f,
                0.5f);
        ring(p, x, y + bob, rr * 1.35f, k, GOLD, fade);
    }

    /** A countdown as pips round a circle, emptying clockwise. */
    private static void ring(Painter p, float x, float y, float rr, float k, int col, float fade) {
        int pips = 10;
        int lit = (int) Math.ceil(pips * k);
        for (int i = 0; i < pips; i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / pips;
            float px = x + rr * (float) Math.cos(a), py = y + rr * (float) Math.sin(a);
            p.fillCircle(px, py, rr * 0.10f,
                    Glyph.withAlpha(i < lit ? col : INK, (int) ((i < lit ? 220 : 40) * fade)));
        }
    }

    // ---- readouts -----------------------------------------------------------

    /**
     * The health bar, the name and the blurb, in the slot the frenzy's mode bar uses.
     *
     * The same slot on purpose, and it cannot collide with it: powerups are suppressed for the whole
     * of a boss stage, so exactly one of the two is ever on screen.
     */
    static void bar(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active()) return;
        float fade = b.intro > 0f ? Math.min(1f, b.introProgress() * 2f)
                : b.beaten ? Math.max(0f, 1f - b.leaveProgress()) : 1f;
        if (fade <= 0.01f) return;

        float y = barY(L), bh = barH(L);
        float left = L.playLeft, right = L.playRight;
        int col = Glyph.mix(tint(b), ROSE, b.enrage() * 0.6f);

        p.fillRect(left, y, right, y + bh, Glyph.withAlpha(INK, (int) (40 * fade)));
        float frac = b.health();
        p.fillRect(left, y, left + (right - left) * frac, y + bh,
                Glyph.withAlpha(col, (int) (240 * fade)));
        // A hit flashes the whole bar, so damage registers even when the number is small.
        if (b.hurt > 0f) {
            p.fillRect(left, y, right, y + bh,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (110 * b.hurt * fade)));
        }

        String name = b.enrage() > 0.35f ? b.name() + "!" : b.name();
        p.text(name, L.w / 2f, nameY(L), nameSize(L),
                Glyph.withAlpha(col, (int) (255 * fade)), Painter.CENTER, true);

        // The blurb retires once it has been read. It sits in the lane words fall down, and unlike
        // the frenzy's blurb — which is only up for fifteen seconds — a boss header stands for the
        // whole fight, so leaving it there means words crossing a line of text for half a minute.
        // It is instructions: it has a job at the start and none afterwards.
        float say = 1f - Math.max(0f, b.age - BLURB_HOLD) / BLURB_FADE;
        if (say > 0.01f) {
            p.text(Boss.BLURB[b.kind], L.w / 2f, blurbY(L), blurbSize(L),
                    Glyph.withAlpha(INK_DIM, (int) (255 * fade * Math.min(1f, say))),
                    Painter.CENTER, false);
        }
    }

    /** How long the blurb stands before it starts going, and how long it takes to go. */
    private static final float BLURB_HOLD = 5f, BLURB_FADE = 1.5f;

    /**
     * The arrival card: the name, big, over the field it is about to take.
     *
     * Its own thing rather than a reuse of the stage banner, because both are on screen at once — the
     * boss starts as the stage begins — and they have to sit apart or they overlap.
     */
    static void intro(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active() || b.intro <= 0f) return;
        float t = b.introProgress();
        // Up quickly, and out over the last third, so the card is gone before the fight starts.
        float a = Math.min(1f, t * 4f) * Math.min(1f, (1f - t) * 3f);
        if (a <= 0.01f) return;

        float s = L.unit;
        float y = L.h * 0.30f;
        int col = tint(b);
        p.text("BOSS", L.w / 2f, y - s * 1.5f, type(s * 0.8f),
                Glyph.withAlpha(ROSE, (int) (235 * a)), Painter.CENTER, true);
        p.text(b.name(), L.w / 2f, y, type(s * 1.8f), Glyph.withAlpha(col, (int) (255 * a)),
                Painter.CENTER, true);
        p.text(Boss.BLURB[b.kind], L.w / 2f, y + type(s * 1.35f), type(s * 0.62f),
                Glyph.withAlpha(INK, (int) (225 * a)), Painter.CENTER, false);
    }

    /** The burst a beaten boss goes out on. */
    static void burst(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active() || !b.beaten || b.body == null) return;
        float t = b.leaveProgress();
        float cx = b.body.centreX(), cy = b.body.centreY();
        float r = b.body.radius();
        // Filled, not stroked. A stroked nine-point star is eighteen vertices of translucent line
        // that double-blends at every one of them, and it came out as a spidery dotted scribble
        // rather than a burst — the same trap Slime's opaque rim exists for. Layered fills, widest
        // and faintest first, is how every other halo in this game is built.
        for (int k = 4; k >= 1; k--) {
            float rr = r * (1f + t * (1.6f + k * 0.8f));
            p.fillPoly(star(cx, cy, rr, rr * 0.42f, 9, c.clock * 0.9f + k),
                    Glyph.withAlpha(k % 2 == 0 ? GOLD : tint(b),
                            (int) (90 * (1f - t) / k)));
        }
        // Below the body, not above it. Above put it straight through the name and the blurb, which
        // are still fading out at that moment — and a payoff drawn over its own header reads as a
        // rendering fault rather than as a reward.
        p.text("BEATEN!", L.w / 2f, beatenY(L, cy, r), type(L.unit * 1.3f * (1f + 0.3f * t)),
                Glyph.withAlpha(GOLD, (int) (255 * (1f - t) * (1f - t))), Painter.CENTER, true);
    }

    /** Baseline of the BEATEN! payoff, under the body it just came off. */
    static float beatenY(Layout L, float bodyCy, float bodyR) {
        return bodyCy + bodyR * 1.60f;
    }
}
