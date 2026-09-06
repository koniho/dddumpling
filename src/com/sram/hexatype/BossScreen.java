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

    static final float DIVIDE_REMNANT_ALPHA = 0.38f;
    static final int DIVIDE_SHARDS_PER_PIECE = 12;

    /** Deep plum through hot mulberry: every generation of Divide is angrier than its parent. */
    private static final int[] DIVIDE_COLOR = {
        0xFF46265F, 0xFF60306F, 0xFF823A78, 0xFFAC3F76
    };

    private BossScreen() {}

    /** Smooth 0..1 heartbeat shared by vulnerable slime bodies. */
    static float vulnerabilityPulse(float clock) {
        float wave = 0.5f + 0.5f * (float) Math.sin(clock * 6.4f);
        return wave * wave;
    }

    /** Tint per boss, taken from the letter each one is a giant version of. */
    private static int tint(Boss b) {
        if (b.kind == Boss.SPLITTER) return DIVIDE_COLOR[0];
        if (b.kind == Boss.OCTOPUS) return 0xFF861735;
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
     * The row a boss's ornaments hang off: the body's own centre, but never higher than its resting
     * one.
     *
     * Two things can lift a body above where it rests, and both are the slime's: a glob dragged at
     * the ceiling walks the whole creature up after it, and the stretch that goes with it makes the
     * body taller besides. Either one carries the wanted-letter badge into the blurb it is asserted to
     * clear. Downward is left alone on purpose — that is SUMO sinking, and its badge has to sink with
     * it or the thing it is asking for is no longer on the thing asking.
     *
     * Named and exported so {@code TestBoss} can hold the header stacking against the number the
     * drawing actually uses, rather than against its own second copy of it.
     */
    static float ornamentY(Layout L, Boss b) {
        return b.body == null ? Boss.restY(L) : Math.max(b.body.centreY(), Boss.restY(L));
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
                : b.beaten ? Math.max(0f, 1f - b.defeatMelt() * b.defeatMelt()) : 1f;
        if (fade <= 0.01f) return;

        int col = tint(b);
        // Rage reddens it; the enrage reddens it further and permanently, so a fight going badly
        // looks like one.
        boolean wounded = b.kind == Boss.SLIME && b.hasGlob();
        float damageHeat = b.kind == Boss.SLIME ? 1f - b.health() : 0f;
        float launch = b.launchT / Boss.LAUNCH_TIME;
        float throb = 0.72f + 0.28f * (float) Math.sin(c.clock * (7f + damageHeat * 9f));
        col = Glyph.mix(col, ROSE, Math.max(Math.max(b.rage * 0.55f,
                b.enrage() * 0.45f), damageHeat * 0.62f * throb));
        if (wounded) col = Glyph.mix(col, YELLOW, vulnerabilityPulse(c.clock) * 0.78f);
        col = Glyph.mix(col, 0xFFFFFFFF, launch * 0.35f);
        // A hit whitens it for a moment on top of the dent the body is already taking.
        col = Glyph.mix(col, 0xFFFFFFFF, b.hurt * 0.35f);

        float r = b.body.radius();
        float cx = b.body.centreX(), cy = b.body.centreY();
        // Per axis, because a boss can be wider than it is tall — see Softbody.reset(.., wide).
        float rx = b.body.radiusX(), ry = b.body.radiusY();

        if (b.kind == Boss.OCTOPUS) drawOctopusArms(p, c, L, b, col, fade);

        // An aura, so the body reads as lit rather than as a flat shape on the sky. Widest and
        // faintest first, and only three layers: this sits behind a soft outline that is already
        // translucent, and more than that greys the whole upper field. Elliptical, so it hugs the
        // silhouette instead of drawing a circle around a wide body.
        for (int k = 3; k >= 1; k--) {
            float g = 1.05f + 0.16f * k;
            p.fillEllipse(cx, cy, rx * g, ry * g,
                    Glyph.withAlpha(col, (int) ((22 + 26 * b.hurt) * fade / k)));
        }
        // While the window is open it glows: that is the whole tell for "it can be hurt now", and it
        // has to be readable without reading the bar.
        //
        // Filled and sized off the resting shape, both for the same reason the burst is: a stroked
        // ten-point star is twenty vertices of translucent line that double-blends at every one, and
        // it came out as a spidery scribble — the CLAUDE.md overlap trap. Sizing it off the live
        // radius made that worse the moment a body could be stretched, since a glob hauled halfway
        // across the field doubles the radius and the scribble grew to fill the upper field with it.
        // The window is a property of the fight, not of what the skin happens to be doing.
        if (launch > 0f) {
            float kick = 1f - launch;
            float rr = Boss.bodyR(L) * (1.1f + kick * 0.9f);
            p.strokeCircle(cx, cy, rr, Glyph.withAlpha(ROSE, (int) (220 * launch * fade)),
                    L.unit * (0.16f + 0.18f * launch));
            p.strokeCircle(cx, cy, rr * 0.72f,
                    Glyph.withAlpha(GOLD, (int) (180 * launch * fade)), L.unit * 0.12f);
        }

        if (b.open() && b.kind != Boss.SPLITTER) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 7f);
            float sx = b.bodyW(L), sy = Boss.bodyR(L);
            for (int k = 2; k >= 1; k--) {
                float g = 1.18f + 0.20f * k;
                p.fillPoly(squash(star(cx, cy, sy * g, sy * g * 0.62f, 10, c.clock * 0.6f),
                                cx, sx / sy),
                        Glyph.withAlpha(GOLD, (int) (34 * pulse * fade / k)));
            }
        }

        // The triplets are drawn faceless: their three heads sit on the body and are its face, so
        // giving the body one of its own put a fourth face behind the three and the whole thing read
        // as a blob wearing heads rather than as a creature with three of them.
        int face = b.kind == Boss.TRIPLETS ? -1 : Boss.FACE[b.kind];
        float mood = b.beaten ? 1f : wounded ? 0f : b.open() ? 0.15f : 0.5f;
        if (b.kind == Boss.SPLITTER) {
            drawDividePieces(p, c, L, b, fade);
        } else {
            if (b.kind == Boss.OCTOPUS) drawOctopusHead(p, c, b, col, mood, fade);
            else Slime.draw(p, b.body, c.clock, col, face, mood, fade);
            if (b.kind == Boss.SLIME && !b.open() && b.rage > 0f) {
                float[] skin = b.body.outline();
                float[] shield = new float[skin.length];
                float shieldPulse = 1.08f + 0.035f * (float) Math.sin(c.clock * 15f);
                for (int i = 0; i < skin.length; i += 2) {
                    shield[i] = cx + (skin[i] - cx) * shieldPulse;
                    shield[i + 1] = cy + (skin[i + 1] - cy) * shieldPulse;
                }
                int shieldCol = Glyph.cycle(c.clock * 5.5f);
                p.strokePoly(shield, Glyph.withAlpha(shieldCol,
                        (int) (245f * b.rage * fade)), r * (0.055f + 0.025f * b.rage));
            }
            // The original slime is a wide, amorphous silhouette. The inset node mesh reconstructs
            // a regular ring over it, making an obsolete circular body appear on top of the skin.
            // Other bosses keep the mesh because it helps their rounder bodies read as soft physics.
            if (b.kind != Boss.SLIME && b.kind != Boss.OCTOPUS) {
                Slime.mesh(p, b.body, col,
                        Math.max(b.hurt, b.body.pulled() ? 0.8f : 0f), fade);
            }
        }

        ornament(p, c, L, b, fade);
        elements(p, c, L, b, fade, col);
    }

    private static void drawOctopusHead(Painter p, GameCore c, Boss b, int col, float mood, float fade) {
        float[] raw = b.body.outline();
        float cx = b.body.centreX(), cy = b.body.centreY();
        float rx = Math.max(1f, b.body.radiusX()), ry = Math.max(1f, b.body.radiusY());
        float[] mantle = new float[raw.length];
        for (int i = 0; i < raw.length; i += 2) {
            float dx = raw[i] - cx, dy = raw[i + 1] - cy;
            float angle = (float) Math.atan2(dy / ry, dx / rx);
            float cs = (float) Math.cos(angle), sn = (float) Math.sin(angle);
            float expected = (float) Math.sqrt((rx * cs) * (rx * cs) + (ry * sn) * (ry * sn));
            float live = (float) Math.sqrt(dx * dx + dy * dy);
            float elastic = Math.max(0.88f, Math.min(1.12f, live / Math.max(1f, expected)));
            // Broad crown and cheeks, then a shallow tucked skirt instead of a circular belly.
            float width = sn > 0.15f ? 0.98f - (sn - 0.15f) * 0.20f : 1.00f;
            float height = sn < 0f ? 1.42f : 0.86f;
            mantle[i] = cx + cs * rx * width * elastic;
            mantle[i + 1] = cy + sn * ry * height * elastic - ry * 0.11f;
        }
        p.fillPoly(mantle, Glyph.withAlpha(col, (int) (224 * fade)));
        p.strokePoly(mantle, Glyph.withAlpha(Glyph.mix(col, 0xFFFFFFFF, 0.46f),
                (int) (255 * fade)), b.body.radius() * 0.06f);

        // Wet sticker-like highlight from the reference, kept translucent to match the slime family.
        p.fillEllipse(cx - rx * 0.35f, cy - ry * 0.43f, rx * 0.17f, ry * 0.27f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (108 * fade)));
        p.fillCircle(cx - rx * 0.18f, cy - ry * 0.64f, rx * 0.085f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (178 * fade)));

        float eyeY = cy - ry * 0.06f;
        float eyeR = rx * 0.145f;
        float eyeDx = rx * 0.34f;
        int ink = Glyph.withAlpha(0xFF160B18, (int) (255 * fade));
        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + side * eyeDx;
            p.fillEllipse(ex, eyeY, eyeR, eyeR * 1.08f, ink);
            p.fillCircle(ex - eyeR * 0.27f, eyeY - eyeR * 0.34f, eyeR * 0.25f,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (245 * fade)));
            p.fillCircle(ex + eyeR * 0.22f, eyeY + eyeR * 0.25f, eyeR * 0.11f,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (210 * fade)));
        }
        float mouthY = cy + ry * 0.24f;
        float[] smile = new float[14];
        for (int k = 0; k < 7; k++) {
            float u = -1f + 2f * k / 6f;
            smile[k * 2] = cx + rx * 0.18f * u;
            smile[k * 2 + 1] = mouthY + ry * 0.10f * (1f - u * u);
        }
        p.polyline(smile, ink, rx * 0.055f);
    }

    private static float[] smoothTentacle(float[] src) {
        int segments = (src.length / 2 - 1) * 4;
        float[] out = new float[(segments + 1) * 2];
        int count = src.length / 2;
        for (int s = 0; s <= segments; s++) {
            float at = s / 4f;
            int i = Math.min(count - 2, (int) at);
            float t = at - i;
            if (s == segments) { i = count - 2; t = 1f; }
            int i0 = Math.max(0, i - 1), i1 = i, i2 = i + 1, i3 = Math.min(count - 1, i + 2);
            float t2 = t * t, t3 = t2 * t;
            for (int axis = 0; axis < 2; axis++) {
                float p0 = src[i0 * 2 + axis], p1 = src[i1 * 2 + axis];
                float p2 = src[i2 * 2 + axis], p3 = src[i3 * 2 + axis];
                out[s * 2 + axis] = 0.5f * ((2f * p1) + (-p0 + p2) * t
                        + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                        + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
            }
        }
        return out;
    }

    private static void orientedEllipse(Painter p, float cx, float cy, float tx, float ty,
            float along, float across, int color) {
        final int points = 14;
        float[] oval = new float[points * 2];
        float nx = -ty, ny = tx;
        for (int i = 0; i < points; i++) {
            float angle = Softbody.TAU * i / points;
            float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
            oval[i * 2] = cx + tx * ca * along + nx * sa * across;
            oval[i * 2 + 1] = cy + ty * ca * along + ny * sa * across;
        }
        p.fillPoly(oval, color);
    }

    private static void drawOctopusArms(Painter p, GameCore c, Layout L, Boss b, int col, float fade) {
        float thick = Boss.bodyR(L) * 0.52f;
        for (int a = 0; a < Boss.OCTO_ARMS; a++) {
            float[] pts = new float[Boss.OCTO_NODES * 2];
            for (int n = 0; n < Boss.OCTO_NODES; n++) { pts[n * 2] = b.octoX[a][n]; pts[n * 2 + 1] = b.octoY[a][n]; }
            boolean dying = a == b.octoDyingArm && b.octoDeath > 0f;
            if ((b.octoArms & (1 << a)) != 0 || dying) {
                float death = dying ? Math.min(1f, b.octoDeath) : 0f;
                boolean warning = a == b.octoAttackArm && b.octoTarget >= 0 && b.octoReach < 0f;
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 13f);
                int armCol = dying
                        ? Glyph.mix(col, death < 0.38f ? 0xFFFFFF8A : 0xFFFF6A86,
                                0.82f - death * 0.30f)
                        : warning ? Glyph.mix(0xFF861735, 0xFFFF355F,
                                0.35f + 0.65f * pulse) : col;
                float[] curve = smoothTentacle(pts);
                float variety = 0.88f + 0.16f * (float) Math.sin(a * 2.17f);
                int steps = curve.length / 2 - 1;
                for (int s = 0; s < steps; s++) {
                    float u = s / (float) steps;
                    float width = thick * variety * (1.18f - 0.76f * u)
                            * (dying ? 1f - death * 0.72f : 1f);
                    float x1 = curve[s * 2], y1 = curve[s * 2 + 1];
                    float x2 = curve[s * 2 + 2], y2 = curve[s * 2 + 3];
                    p.line(x1, y1, x2, y2,
                            Glyph.withAlpha(Glyph.mix(armCol, BG, 0.48f), (int) (170 * fade)),
                            width * 1.22f);
                    p.line(x1, y1, x2, y2, Glyph.withAlpha(armCol, (int) (230 * fade)), width);
                    p.line(x1 - width * 0.10f, y1 - width * 0.10f,
                            x2 - width * 0.10f, y2 - width * 0.10f,
                            Glyph.withAlpha(0xFFFFFFFF, (int) (38 * fade)), width * 0.14f);
                }
                // A row of soft pink suckers follows the lower side of each curl.
                for (int s = steps * 5 / 9; s < steps; s += 3) {
                    float x1 = curve[s * 2], y1 = curve[s * 2 + 1];
                    float x2 = curve[s * 2 + 2], y2 = curve[s * 2 + 3];
                    float dx = x2 - x1, dy = y2 - y1;
                    float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
                    float nx = -dy / len, ny = dx / len;
                    if (ny < 0f) { nx = -nx; ny = -ny; }
                    float suckerR = thick * (0.13f - 0.045f * s / steps);
                    float suckerX = x1 + nx * thick * 0.27f;
                    float suckerY = y1 + ny * thick * 0.27f;
                    // Twice the old footprint, stretched along the local tentacle direction.
                    orientedEllipse(p, suckerX, suckerY, dx / len, dy / len,
                            suckerR * 2.0f, suckerR * 1.12f,
                            Glyph.withAlpha(0xFFB83F68, (int) (245 * fade)));
                    // A smaller inset cup gives every sucker a visible recessed centre.
                    orientedEllipse(p, suckerX - nx * suckerR * 0.10f,
                            suckerY - ny * suckerR * 0.10f, dx / len, dy / len,
                            suckerR * 1.18f, suckerR * 0.56f,
                            Glyph.withAlpha(0xFFFFC1CF, (int) (238 * fade)));
                }
                float tipX = curve[curve.length - 2], tipY = curve[curve.length - 1];
                if (dying) {
                    for (int bit = 0; bit < 7; bit++) {
                        float burst = death * (1.1f + bit * 0.055f);
                        float px = tipX + (float) Math.cos(bit * 2.4f) * thick * burst;
                        float py = tipY + (float) Math.sin(bit * 1.9f) * thick * burst
                                + thick * death * death;
                        p.fillCircle(px, py, thick * (0.12f - bit * 0.008f)
                                * (1f - death * 0.65f),
                                Glyph.withAlpha(bit % 2 == 0 ? 0xFFFFD38A : armCol,
                                        (int) (220f * (1f - death) * fade)));
                    }
                }
                p.fillCircle(tipX, tipY, thick * variety * 0.22f
                                * (dying ? 1f - death * 0.72f : 1f),
                        Glyph.withAlpha(armCol, (int) (230 * fade)));
                if (a == b.octoAttackArm && b.octoCaptured >= 0) {
                    int g = b.octoCaptured; int keyCol = Glyph.COLOR[g];
                    p.fillPoly(Glyph.hex(tipX, tipY, L.keyR), Glyph.withAlpha(keyCol, (int) (110 * fade)));
                    p.strokePoly(Glyph.hex(tipX, tipY, L.keyR), Glyph.withAlpha(keyCol, (int) (255 * fade)), L.keyR * 0.10f);
                    Kawaii.draw(p, g, tipX, tipY, L.keyR * 0.60f, Glyph.withAlpha(keyCol, (int) (255 * fade)), 1f, 0.1f);
                }
            } else {
                float gx = pts[2], gy = pts[3];
                p.fillEllipse(gx, gy, thick * 0.58f, thick * 0.43f, Glyph.withAlpha(col, (int) (190 * fade)));
                p.fillCircle(gx - thick * 0.14f, gy - thick * 0.13f, thick * 0.12f, Glyph.withAlpha(0xFFFFFFFF, (int) (120 * fade)));
            }
        }
        if (b.octoLash > 0f) {
            float t = b.octoLash;
            float bend = (float) Math.sin(t * Math.PI) * L.enemyR * 1.8f;
            float[] lash = {
                    b.hitX, b.hitY,
                    (b.hitX + b.octoLashX) * 0.5f + bend,
                    (b.hitY + b.octoLashY) * 0.5f,
                    b.octoLashX, b.octoLashY
            };
            float alpha = fade * (float) Math.sin(Math.PI * b.octoLash);
            p.polyline(lash, Glyph.withAlpha(0xFF310817, (int) (190 * alpha)), thick * 0.82f);
            p.polyline(lash, Glyph.withAlpha(0xFFFF355F, (int) (245 * alpha)), thick * 0.48f);
            p.polyline(lash, Glyph.withAlpha(0xFFFFB0C0, (int) (150 * alpha)), thick * 0.10f);
        }
    }

    static boolean divideVulnerable(Boss b, int piece) {
        return b.pieceCharge(piece) >= Boss.DIVIDE_HITS;
    }

    private static void drawDividePieces(Painter p, GameCore c, Layout L, Boss b, float fade) {
        for (int i = 0; i < b.pieceCount(); i++) {
            Softbody piece = b.pieceBody(i);
            if (piece == null) continue;
            float rr = b.pieceR(i, L);
            float x = piece.centreX(), y = piece.centreY();
            float heat = Math.min(1f, b.pieceIdle(i) / Boss.DIVIDE_BOLT_TIME);
            float hurt = b.pieceHurt(i);
            int depth = Math.max(0, Math.min(DIVIDE_COLOR.length - 1, b.pieceDepth(i)));
            int halfCol = Glyph.mix(DIVIDE_COLOR[depth], ROSE, heat * 0.45f);
            boolean vulnerable = divideVulnerable(b, i);
            if (vulnerable)
                halfCol = Glyph.mix(halfCol, YELLOW, vulnerabilityPulse(c.clock) * 0.78f);
            halfCol = Glyph.mix(halfCol, 0xFFFFFFFF, hurt * 0.65f);
            Slime.draw(p, piece, c.clock + i * 0.31f, halfCol, Boss.FACE[b.kind], heat, fade);
            divideSkin(p, c, L, b, i, piece, x, y, piece.radiusX(), piece.radiusY(),
                    halfCol, fade);
            float dangerR = rr * (1.48f - heat * 0.30f);
            int dangerCol = Glyph.mix(GOLD, ROSE, heat);
            p.strokeCircle(x, y, dangerR, Glyph.withAlpha(dangerCol,
                    (int) ((35 + 180 * heat * heat) * fade)), rr * (0.035f + 0.07f * heat));
            if (hurt > 0f) {
                p.strokeCircle(x, y, rr * (0.72f + (1f - hurt) * 0.55f),
                        Glyph.withAlpha(0xFFFFFFFF, (int) (230 * hurt * fade)), rr * 0.10f);
                float dir = i == 0 ? -1f : 1f;
                p.polyline(new float[] {x - dir * rr * 0.10f, y - rr * 0.52f,
                        x + dir * rr * 0.12f, y - rr * 0.10f,
                        x - dir * rr * 0.04f, y + rr * 0.30f},
                        Glyph.withAlpha(INK, (int) (190 * hurt * fade)), rr * 0.055f);
            }
        }
        // Destroyed leaves stay in the arena as harmless bouncing remnants. They are deliberately
        // drawn without prompts, danger rings, cracks, or hit targets.
        for (int n = 0; n < Boss.DIVIDE_NODES; n++) {
            if (b.nodeActive(n) || !b.nodeVisible(n)) continue;
            Softbody remnant = b.divideBody[n];
            if (remnant == null) continue;
            int depth = Math.max(0, Math.min(DIVIDE_COLOR.length - 1, b.nodeDepth(n)));
            int remnantCol = Glyph.mix(DIVIDE_COLOR[depth], BG, 0.36f);
            Slime.draw(p, remnant, c.clock + n * 0.31f, remnantCol,
                    Boss.FACE[b.kind], 1f, fade * DIVIDE_REMNANT_ALPHA);
        }

        if (b.beaten) divideBreakup(p, c, L, b, fade);

        if (b.divideBurst > 0f) {
            float burst = b.divideBurst;
            float rr = Boss.bodyR(L) * 0.72f;
            float cx = b.bodyX(L), cy = b.halfY(L);
            float gap = rr * (0.32f + (1f - burst) * 0.75f);
            p.fillEllipse(cx, cy, gap, rr * (0.22f + burst * 0.50f),
                    Glyph.withAlpha(0xFFFFFFFF, (int) (210 * burst * fade)));
            p.strokeCircle(cx, cy, rr * (0.45f + (1f - burst) * 1.25f),
                    Glyph.withAlpha(GOLD, (int) (235 * burst * fade)), rr * 0.12f);
        }
    }

    /** Tiny same-colour droplets shed as the defeated fragments break apart and fall. */
    private static void divideBreakup(Painter p, GameCore c, Layout L, Boss b, float fade) {
        float progress = b.leaveProgress();
        if (progress < 0.50f) return;
        float fall = Math.min(1f, (progress - 0.50f) / 0.50f);
        int visible = b.divideActive | b.divideDead;
        int count = Math.max(1, Integer.bitCount(visible));
        float centreX = (L.playLeft + L.playRight) * 0.5f;
        float centreY = (L.playTop + L.dangerY) * 0.5f;
        float orbit = Boss.bodyR(L) * 0.72f;
        for (int n = 0; n < Boss.DIVIDE_NODES; n++) {
            if (!b.nodeVisible(n)) continue;
            int ordinal = Integer.bitCount(visible & ((1 << n) - 1));
            float ring = -Softbody.TAU * 0.25f + Softbody.TAU * ordinal / count;
            float ox = centreX + (float) Math.cos(ring) * orbit;
            float oy = centreY + (float) Math.sin(ring) * orbit;
            int depth = Math.max(0, Math.min(DIVIDE_COLOR.length - 1, b.nodeDepth(n)));
            int col = DIVIDE_COLOR[depth];
            float sourceR = b.divideBody[n] == null ? L.enemyR : b.divideBody[n].radius();
            for (int k = 0; k < DIVIDE_SHARDS_PER_PIECE; k++) {
                int hash = n * 1103515245 + k * 12345 + 0x51A7;
                float jitter = ((hash >>> 8) & 1023) / 1023f;
                float angle = ring + (k / (float) DIVIDE_SHARDS_PER_PIECE - 0.5f) * 2.8f
                        + jitter * 0.55f;
                float speed = sourceR * (1.4f + 2.2f * (((hash >>> 18) & 255) / 255f));
                float x = ox + (float) Math.cos(angle) * speed * fall;
                float y = oy + (float) Math.sin(angle) * speed * fall
                        + fall * fall * L.h * (0.72f + 0.28f * jitter);
                float r = sourceR * (0.075f + 0.075f * (((hash >>> 4) & 15) / 15f));
                int alpha = (int) (220f * Math.min(1f, fall * 5f) * (1f - fall * 0.58f) * fade);
                p.fillEllipse(x, y, r, r * (0.72f + 0.20f * jitter), Glyph.withAlpha(col, alpha));
                p.fillCircle(x - r * 0.22f, y - r * 0.20f, r * 0.22f,
                        Glyph.withAlpha(0xFFFFFFFF, alpha / 3));
            }
        }
    }

    /** Damage cracks the intact Divide inward; a full charge turns that crack into a pull seam. */
    private static void divideSkin(Painter p, GameCore c, Layout L, Boss b, int piece, Softbody body,
            float cx, float cy, float rx, float ry, int bodyCol, float fade) {
        float charge = Math.min(1f, b.pieceCharge(piece) / (float) Boss.DIVIDE_HITS);
        if (charge <= 0f) return;
        float pulse = 0.72f + 0.28f * (float) Math.sin(c.clock * (7f + 5f * charge));
        int seam = charge >= 1f ? GOLD : Glyph.mix(INK, GOLD, charge * 0.65f);
        float crack = ry * (0.20f + charge * 0.62f);
        if (charge < 1f) {
            p.polyline(new float[] {cx, cy - crack, cx - rx * 0.09f, cy - ry * 0.24f,
                    cx + rx * 0.07f, cy, cx - rx * 0.08f, cy + ry * 0.27f, cx, cy + crack},
                    Glyph.withAlpha(seam, (int) ((105 + 125 * charge * pulse) * fade)),
                    L.unit * (0.05f + 0.05f * charge));
        }
        if (b.hurt > 0f) {
            float kick = b.hurt;
            p.strokeCircle(cx, cy, ry * (0.55f + (1f - kick) * 0.75f),
                    Glyph.withAlpha(0xFFFFFFFF, (int) (210 * kick * fade)), L.unit * 0.13f);
        }
        if (charge < 1f) return;
        float rr = ry * (0.27f + 0.035f * pulse);
        divideBlob(p, body, cx - rx, cy, bodyCol, fade * pulse);
        divideBlob(p, body, cx + rx, cy, bodyCol, fade * pulse);
        if (b.pieceDepth(piece) == 0) {
            float travel = (c.clock * 0.85f) % 1f;
            float arrowX = rx * (0.62f + 0.72f * travel);
            float shaft = rx * 0.34f;
            float head = ry * 0.20f;
            int arrow = Glyph.withAlpha(GOLD, (int) (240f * (1f - travel) * fade));
            for (int side = -1; side <= 1; side += 2) {
                float tip = cx + side * arrowX;
                float tail = tip - side * shaft;
                p.line(tail, cy, tip, cy, arrow, L.unit * 0.13f);
                p.fillPoly(new float[] {tip, cy, tip - side * head, cy - head * 0.72f,
                        tip - side * head, cy + head * 0.72f}, arrow);
            }
        }
    }

    /** A mirrored vulnerable marker using the slime glob skin treatment, not a floating badge. */
    private static void divideBlob(Painter p, Softbody body, float x, float y, int bodyCol,
            float fade) {
        float[] patch = globPath(body, x, y, 0.82f);
        fillGlobGradient(p, patch, bodyCol, fade);
        float[] outer = new float[patch.length / 2];
        System.arraycopy(patch, 0, outer, 0, outer.length);
        int rim = Glyph.mix(bodyCol, 0xFFFFFFFF, 0.62f);
        p.polyline(outer, Glyph.withAlpha(rim, (int) (245 * fade)), body.radius() * 0.075f);
    }

    /** Whatever this boss is asking for, drawn on or around the body. */
    private static void ornament(Painter p, GameCore c, Layout L, Boss b, float fade) {
        float r = b.body.radius();
        float cx = b.body.centreX(), cy = b.body.centreY();
        // Vertical half-extent for anything stacked over or under the body, horizontal for anything
        // set beside it. The badge's offset is what the header stacking is derived against, so this
        // has to be the height and not the mean radius: on a body twice as wide as it is tall the
        // mean is half again the height, and the badge would climb into the blurb.
        //
        // And capped at the resting height, which is the figure {@code TestBoss.stacking} derives the
        // whole column from. A body can be stretched taller than it rests — drag a glob at the ceiling
        // and the goo follows it up — and an uncapped badge would ride that straight through the blurb
        // it is asserted to clear. Downward it is free to follow a squash, since that only ever opens
        // the gap up.
        float rx = b.body.radiusX();
        float ry = Math.min(b.body.radiusY(), Boss.bodyR(L));
        float pin = ornamentY(L, b);

        if (b.kind == Boss.SLIME) {
            // The next prompt does not appear until the whole launched volley is gone.
            if (b.boltCount() == 0 && !b.hasGlob()) {
                float urgency = b.promptProgress();
                float pop = 1f + urgency * 0.32f
                        + 0.08f * urgency * (float) Math.sin(
                                c.clock * (8f + urgency * 10f));
                letterBadge(p, c, L, b.chainLetter(), cx, slimeBadgeY(L, b), L.unit * 1.08f * pop, fade,
                        b.open());
            }
            // And how far the chain has got: the press does not move the health bar, so without
            // this a run of four presses looks like four presses that did nothing. Hung off the
            // resting height rather than the live one so it does not get swallowed by the body every
            // time a squash flattens it.
            splitGauge(p, c, L, b, cx, slimeBadgeY(L, b) + badgeR(L) * 1.65f, fade);
        } else if (b.kind == Boss.TRIPLETS) {
            // The heads are elements, so they are drawn with them.
            return;
        } else if (b.kind == Boss.SPLITTER) {
            for (int i = 0; i < b.pieceCount(); i++) {
                float x = b.pieceX(i, L), y = b.pieceY(i, L), pr = b.pieceR(i, L);
                int charge = b.pieceCharge(i);
                if (charge < Boss.DIVIDE_HITS) {
                    letterBadge(p, c, L, b.pieceWant(i), x, y - pr * 1.35f,
                            L.keyR, fade, true);
                } else if (b.pieceDepth(i) < Boss.DIVIDE_LEVELS) {
                    float spread = pr * (0.75f + 0.08f * (float) Math.sin(c.clock * 6f));
                    p.line(x - pr * 0.18f, y, x - spread, y,
                            Glyph.withAlpha(GOLD, (int) (235 * fade)), L.unit * 0.10f);
                    p.line(x + pr * 0.18f, y, x + spread, y,
                            Glyph.withAlpha(GOLD, (int) (235 * fade)), L.unit * 0.10f);
                }
            }
        } else if (b.kind == Boss.OCTOPUS) {
            return;
        } else if (b.kind == Boss.DRUM) {
            // The beat: a ring that closes as the window approaches, and the thing it wants inside.
            beatRing(p, c, b, cx, cy, r, fade);
            if (b.tapBeat) tapMark(p, c, cx, cy, r * 0.5f, fade, b.open());
            else letterBadge(p, c, L, b.want(), cx, pin - ry * 1.28f, L.unit * 0.95f, fade, b.open());
        } else if (b.kind == Boss.MAGPIE) {
            letterBadge(p, c, L, b.want(), cx, pin - ry * 1.28f, L.unit * 0.95f, fade, b.open());
            // The key it is holding, clear of the body's outline rather than over it — a struck-out
            // key drawn across the boss's own face read as damage to the boss instead of as
            // something it had taken. Upper left, away from the wanted letter above it.
            if (b.stolen >= 0) heldKey(p, c, L, b, cx - rx * 1.20f, pin - ry * 0.85f, fade);
        } else if (b.kind == Boss.SUMO) {
            letterBadge(p, c, L, b.want(), cx, pin - ry * 1.28f, L.unit * 0.95f, fade, true);
            charges(p, c, L, b, cx, pin + ry * 1.30f, fade);
            if (b.stagger > 0f) staggerRing(p, c, b, cx, cy, r, fade);
        }
    }

    /** Centre of the slime's charged bolt character, below its live underside. */
    static float slimeBadgeY(Layout L, Boss b) {
        float ry = b.body == null ? Boss.bodyR(L) : Math.min(b.body.radiusY(), Boss.bodyR(L));
        return ornamentY(L, b) + ry * 1.28f + badgeR(L);
    }

    /**
     * Widens a shape about {@code cx} by {@code k}, in place, and hands the same array back.
     *
     * For laying a round mark over a body that is not round. Mutates the buffer it is given, which is
     * what {@link Draw#star} returns anyway — so a caller wanting to keep the original has to copy it
     * first, exactly as with {@link Softbody#outline}.
     */
    private static float[] squash(float[] pts, float cx, float k) {
        for (int i = 0; i < pts.length; i += 2) pts[i] = cx + (pts[i] - cx) * k;
        return pts;
    }

    /**
     * How far the chain has got toward tearing the next glob loose: one pip per press, under the body.
     *
     * The slime is the one boss whose presses do not move the health bar — only a glob carried off the
     * screen does that — so this is the whole feedback for four presses out of every five. Rose,
     * because that is the colour a glob is, and the last pip is what a glob will be.
     */
    private static void splitGauge(Painter p, GameCore c, Layout L, Boss b, float cx, float y,
            float fade) {
        float rr = L.unit * 0.20f, gap = rr * 3.0f;
        float x0 = cx - gap * (Boss.SPLIT_HITS - 1) / 2f;
        for (int i = 0; i < Boss.SPLIT_HITS; i++) {
            boolean done = i < b.split;
            // The next one to fill breathes, so the gauge says "press again" rather than only
            // recording what has happened.
            boolean next = i == b.split && b.open();
            float pulse = next ? 1f + 0.22f * (float) Math.sin(c.clock * 8f) : 1f;
            p.fillCircle(x0 + i * gap, y, rr * pulse,
                    Glyph.withAlpha(done ? ROSE : INK, (int) ((done ? 235 : 40) * fade)));
            if (next) {
                p.strokeCircle(x0 + i * gap, y, rr * 1.5f * pulse,
                        Glyph.withAlpha(ROSE, (int) (120 * fade)), rr * 0.22f);
            }
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
        if (b.open() && b.kind != Boss.SPLITTER) {
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
    private static void elements(Painter p, GameCore c, Layout L, Boss b, float fade, int bodyCol) {
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
                glob(p, c, b, i, x, y, rr, life, held, fade, bodyCol);
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
     * The loose place in the slime skin: a local colour change, not a separate object.
     *
     * Its outer edge is a literal arc copied from the live soft-body outline. The inward copy closes
     * the colour region, so it is one marked piece of skin rather than a shape laid over the body.
     */
    private static void glob(Painter p, GameCore c, Boss b, int i, float x, float y, float rr,
            float life, boolean held, float fade, int bodyCol) {
        float born = Math.min(1f, (Boss.GLOB_TIME - life) / 0.30f);
        float dying = Math.min(1f, life / 0.65f);
        float grow = born * (1.08f - 0.08f * born);
        rr *= grow;
        if (rr <= 0.1f) return;
        float side = x < b.body.centreX() ? -1f : 1f;
        float[] patch = globPath(b, x, y, grow * 1.35f);
        fillGlobGradient(p, patch, bodyCol, dying * fade);
        // No closed outline through the body: only restore the shared exterior rim that the patch
        // fill covers. The inward closing edge remains colour against colour with no border.
        float[] outer = new float[patch.length / 2];
        System.arraycopy(patch, 0, outer, 0, outer.length);
        float hit = Math.min(1f, b.body.deform() * 4.5f);
        int rim = Glyph.mix(bodyCol, 0xFFFFFFFF, 0.42f + 0.38f * hit);
        p.polyline(outer, Glyph.withAlpha(rim, (int) (255 * dying * fade)),
                b.body.radius() * (0.055f + 0.025f * hit));
        if (!held && born >= 0.55f) {
            float drag = (c.clock * 0.65f) % 1f;
            float tipX = x + side * rr * (0.15f + drag * 2.4f);
            float tipY = y - rr * 0.10f * (float) Math.sin(drag * Math.PI);
            float handAngle = side > 0f ? 2.45f : 0.69f;
            Renderer.fingerHint(p, tipX, tipY, rr * 0.58f, handAngle,
                    fade * (1f - drag * 0.35f), c.clock);
        }
    }

    /** Colour bands from the untouched inner skin to rose at the protruding body arc. */
    private static void fillGlobGradient(Painter p, float[] patch, int bodyCol, float fade) {
        int arc = patch.length / 4;
        final int bands = 11;
        for (int band = 0; band < bands; band++) {
            float t0 = (float) band / bands, t1 = (float) (band + 1) / bands;
            float mid = (t0 + t1) * 0.5f;
            int col = Glyph.mix(bodyCol, ROSE, 0.12f + 0.88f * mid);
            // The inner edge starts as the body already underneath it; opacity rises outward so the
            // first band cannot leave a second translucent seam in otherwise continuous goo.
            int a = (int) (220 * t1 * fade);
            for (int q = 0; q < arc - 1; q++) {
                int in0 = arc * 2 + (arc - 1 - q) * 2;
                int in1 = arc * 2 + (arc - 2 - q) * 2;
                float ix0 = patch[in0], iy0 = patch[in0 + 1];
                float ix1 = patch[in1], iy1 = patch[in1 + 1];
                float ox0 = patch[q * 2], oy0 = patch[q * 2 + 1];
                float ox1 = patch[(q + 1) * 2], oy1 = patch[(q + 1) * 2 + 1];
                p.fillPoly(new float[] {
                        ix0 + (ox0 - ix0) * t0, iy0 + (oy0 - iy0) * t0,
                        ix1 + (ox1 - ix1) * t0, iy1 + (oy1 - iy1) * t0,
                        ix1 + (ox1 - ix1) * t1, iy1 + (oy1 - iy1) * t1,
                        ix0 + (ox0 - ix0) * t1, iy0 + (oy0 - iy0) * t1},
                        Glyph.withAlpha(col, a));
            }
        }
    }

    /** A closed skin region whose outside edge is copied directly from the slime outline. */
    static float[] globPath(Boss b, float x, float y, float grow) {
        return globPath(b.body, x, y, grow);
    }

    private static float[] globPath(Softbody softbody, float x, float y, float grow) {
        float[] body = softbody.outline();
        int n = body.length / 2, nearest = 0;
        float best = Float.MAX_VALUE;
        for (int q = 0; q < n; q++) {
            float dx = body[q * 2] - x, dy = body[q * 2 + 1] - y;
            float d = dx * dx + dy * dy;
            if (d < best) { best = d; nearest = q; }
        }
        int span = Math.max(1, Math.round(5f * grow));
        int arc = span * 2 + 1;
        float[] patch = new float[arc * 4];
        float cx = softbody.centreX(), cy = softbody.centreY();
        for (int q = 0; q < arc; q++) {
            int at = (nearest - span + q + n) % n;
            float px = body[at * 2], py = body[at * 2 + 1];
            patch[q * 2] = px;
            patch[q * 2 + 1] = py;
            int back = arc * 2 + (arc - 1 - q) * 2;
            patch[back] = px + (cx - px) * 0.28f;
            patch[back + 1] = py + (cy - py) * 0.28f;
        }
        return patch;
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

    /**
     * The letter bolts a slime throws, drawn in front of everything.
     *
     * In front because they are the only thing on a boss stage that costs a life — the inverse of the
     * body, which is the backdrop. Each is the letter's own hexagon, the same mark the deck and the
     * word tiles use, so what to press needs no explaining.
     */
    static void bolts(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active()) return;
        for (int i = 0; i < Boss.BOLTS; i++) {
            if (!b.blive[i]) continue;
            int g = b.bglyph[i];
            float x = b.boltX(i, L), y = b.boltY(i, L);
            float at = b.boltAt(i);
            // Slime bolts match the deck keys throughout their flight; other boss projectiles
            // grow as they approach to make their final half-second read more loudly.
            float rr = b.kind == Boss.SLIME
                    ? L.keyR
                    : L.keyR * (0.42f + 0.30f * at);
            int col = Glyph.COLOR[g];

            // A tail back toward the launch point, so the direction reads in one frame.
            float tx = b.bsx[i], ty = b.bsy[i];
            for (int k = 1; k <= 3; k++) {
                float f = 1f - 0.10f * k;
                p.fillCircle(tx + (x - tx) * f, ty + (y - ty) * f,
                        rr * (0.55f - 0.12f * k), Glyph.withAlpha(col, 60 / k));
            }
            // Halo, then the hexagon and its face.
            for (int k = 2; k >= 1; k--) {
                p.fillCircle(x, y, rr * (1.15f + 0.28f * k), Glyph.withAlpha(col, 40 / k));
            }
            p.fillPoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, 96));
            p.strokePoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, 255), rr * 0.13f);
            Kawaii.draw(p, g, x, y, rr * 0.58f, Glyph.withAlpha(col, 255), 1f, 0.1f);
            for (int h = 0; h < b.bhp[i]; h++) {
                float pip = rr * 0.13f;
                float px = x + (h - (b.bhp[i] - 1) * 0.5f) * pip * 2.6f;
                p.fillCircle(px, y + rr * 0.82f, pip, Glyph.withAlpha(INK, 235));
            }
        }
    }

    /** The burst a beaten boss goes out on. */
    static void burst(Painter p, GameCore c, Layout L) {
        Boss b = c.boss;
        if (!b.active() || !b.beaten || b.body == null) return;
        float t = b.leaveProgress();
        float cx = b.body.centreX(), cy = b.body.centreY();
        float r = b.body.radius();
        float melt = b.defeatMelt();
        float cute = Math.min(1f, t / 0.38f);
        // Filled, not stroked. A stroked nine-point star is eighteen vertices of translucent line
        // that double-blends at every one of them, and it came out as a spidery dotted scribble
        // rather than a burst — the same trap Slime's opaque rim exists for. Layered fills, widest
        // and faintest first, is how every other halo in this game is built.
        for (int k = 4; k >= 1; k--) {
            float rr = r * (1f + melt * (1.6f + k * 0.8f));
            p.fillPoly(star(cx, cy, rr, rr * 0.42f, 9, c.clock * 0.9f + k),
                    Glyph.withAlpha(k % 2 == 0 ? GOLD : tint(b),
                            (int) (90 * (1f - melt) / k)));
        }
        // Hearts hop out on the three squishy sound beats before gravity takes the boss.
        if (cute < 1f) {
            for (int k = 0; k < 5; k++) {
                float u = cute * 1.45f - k * 0.13f;
                if (u <= 0f || u >= 1f) continue;
                float hx = cx + (k - 2) * r * 0.42f;
                float hy = cy - r * (0.45f + u * 1.15f);
                heart(p, hx, hy, r * 0.14f * (1f - u * 0.35f),
                        Glyph.withAlpha(k % 2 == 0 ? ROSE : GOLD,
                                (int) (230 * (1f - u) * (1f - u))));
            }
        }

        // Below the body, not above it. Above put it straight through the name and the blurb, which
        // are still fading out at that moment — and a payoff drawn over its own header reads as a
        // rendering fault rather than as a reward.
        // The resting height, not the live one: this is the figure the stacking assertion derives the
        // clearance from at both ends, and a body mid-burst is not the shape it was asserted at.
        p.text("BEATEN!", L.w / 2f, beatenY(L, cy, Boss.bodyR(L)),
                type(L.unit * 1.3f * (1f + 0.3f * t)),
                Glyph.withAlpha(GOLD, (int) (255 * (1f - t) * (1f - t))), Painter.CENTER, true);
    }

    private static void heart(Painter p, float cx, float cy, float r, int col) {
        p.fillCircle(cx - r * 0.45f, cy - r * 0.28f, r * 0.55f, col);
        p.fillCircle(cx + r * 0.45f, cy - r * 0.28f, r * 0.55f, col);
        p.fillPoly(new float[] {cx - r * 0.95f, cy - r * 0.10f,
                cx + r * 0.95f, cy - r * 0.10f, cx, cy + r * 1.05f}, col);
    }

    /** Baseline of the BEATEN! payoff, under the body it just came off. */
    static float beatenY(Layout L, float bodyCy, float bodyR) {
        return bodyCy + bodyR * 1.60f;
    }
}
