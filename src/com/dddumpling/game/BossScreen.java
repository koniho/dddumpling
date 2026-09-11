package com.dddumpling.game;

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

    private static final int OCTO_SKIN = 0xFFD967DC;
    private static final int OCTO_SHADE = 0xFFAD43B7;
    private static final int OCTO_INK = 0xFF100D16;
    private static final int OCTO_SUCKER = 0xFFF6B5CB;
    private static final int OCTO_GLEAM = 0xFFFFD1F2;

    /** Smooth 0..1 heartbeat shared by vulnerable slime bodies. */
    static float vulnerabilityPulse(float clock) {
        float wave = 0.5f + 0.5f * (float) Math.sin(clock * 6.4f);
        return wave * wave;
    }

    /** Tint per boss, taken from the letter each one is a giant version of. */
    private static int tint(Boss b) {
        if (b.kind == Boss.SLIME) return 0xFF83E51C;
        if (b.kind == Boss.SPLITTER) return DIVIDE_COLOR[0];
        if (b.kind == Boss.OCTOPUS) return OCTO_SKIN;
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
        body(p, c, L, c.boss, 1f);
    }

    /** Draws either the live boss or a retained victory snapshot through the identical renderer. */
    static void body(Painter p, GameCore c, Layout L, Boss b, float alpha) {
        if (!b.active() || b.body == null) return;

        // In on the arrival card, out on the burst.
        float fade = b.intro > 0f ? Math.min(1f, b.introProgress() * 1.6f)
                : b.beaten ? Math.max(0f, 1f - b.defeatMelt() * b.defeatMelt()) : 1f;
        fade *= alpha;
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
        if (b.kind == Boss.MUSHROOM && b.mushroomAngry > 0f) {
            int flare = b.mushroomAngry > Boss.MUSHROOM_REACTION_RELEASE ? 0xFFFFCF83 : 0xFFFF8CBD;
            col = Glyph.mix(col, flare, b.mushroomDamagePulse() * 0.90f);
        }
        // A hit whitens it for a moment on top of the dent the body is already taking.
        col = Glyph.mix(col, 0xFFFFFFFF, b.hurt * 0.35f);
        if (b.kind == Boss.SLIME) {
            float dragRed = Math.min(1f, b.slimeDragPulse / 0.30f);
            float keyRed = Math.min(1f, b.slimeKeyPulse / 0.11f);
            float red = Math.max(dragRed * 0.88f, keyRed * 0.66f);
            col = Glyph.mix(col, 0xFFFF3048, red);
        }

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

        // The active boss supplies its face.
        // giving the body one of its own put a fourth face behind the three and the whole thing read
        // as a blob wearing heads rather than as a creature with three of them.
        int face = Boss.FACE[b.kind];
        float mood = b.beaten ? 1f : wounded ? 0f : b.open() ? 0.15f : 0.5f;
        if (b.kind == Boss.SPLITTER) {
            drawDividePieces(p, c, L, b, fade);
        } else {
            if (b.kind == Boss.MUSHROOM) drawMushroom(p, c, L, b, col, fade);
            else if (b.kind == Boss.OCTOPUS) {
                drawOctopusHead(p, c, b, col, mood, fade);
                drawCapturedKey(p, L, b, fade);
            }
            else if (b.kind == Boss.SLIME) drawSlimeBoss(p, c, b, col, mood, fade);
            else Slime.draw(p, b.body, c.clock, col, face, mood, fade);
            if (b.kind == Boss.SLIME && !b.open() && b.rage > 0f) {
                float[] skin = slimeBossOutline(b.body);
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
            if (b.kind != Boss.SLIME && b.kind != Boss.OCTOPUS && b.kind != Boss.MUSHROOM) {
                Slime.mesh(p, b.body, col,
                        Math.max(b.hurt, b.body.pulled() ? 0.8f : 0f), fade);
            }
        }

        ornament(p, c, L, b, fade * b.defeatPromptFade());
        elements(p, c, L, b, fade, col);
    }

    /** Reference-led first boss: a bright jelly dome settled into a low rippled puddle. */
    private static void drawSlimeBoss(Painter p, GameCore c, Boss b, int col, float mood,
            float fade) {
        float[] skin = slimeBossOutline(b.body);
        float cx = b.body.centreX(), cy = b.body.centreY();
        float rx = b.body.radiusX(), ry = b.body.radiusY();
        int lime = Glyph.mix(col, 0xFFA8F02B, 0.32f);
        int edge = Glyph.mix(0xFF176A24, col, 0.16f);
        boolean vulnerable = b.hasGlob();

        // The puddle sits behind the skirt, with the two darker body layers thickest at its foot.
        p.fillEllipse(cx, cy + ry * 1.34f, rx * 1.12f, ry * 0.30f,
                Glyph.withAlpha(0xFF86C52D, (int) (150 * fade)));
        p.fillPoly(skin, Glyph.withAlpha(Glyph.mix(lime, 0xFF185C23, 0.58f), (int) (245 * fade)));
        float[] middle = slimeInset(skin, cx, cy, rx, ry, 0.965f, 0.97f, -0.025f, -0.035f);
        p.fillPoly(middle,
                Glyph.withAlpha(Glyph.mix(lime, 0xFF35821D, 0.40f), (int) (245 * fade)));
        p.fillPoly(slimeInset(middle, cx, cy, rx, ry, 0.922f, 0.907f, -0.062f, -0.128f),
                Glyph.withAlpha(lime, (int) (245 * fade)));
        p.strokePoly(skin, Glyph.withAlpha(edge, (int) (255 * fade)),
                b.body.radius() * 0.055f);

        // Broad tilted wet highlight, matching the reference's upper-left shoulder.
        float gleam = 0.96f + 0.05f * (float) Math.sin(c.clock * 2.1f);
        float[] reflection = new float[48];
        for (int i = 0; i < 24; i++) {
            float a = Softbody.TAU * i / 24f;
            float dx = (float) Math.cos(a) * rx * 0.14f * gleam;
            float dy = (float) Math.sin(a) * ry * 0.16f / gleam;
            reflection[i * 2] = cx - rx * 0.36f + dx * 0.75f + dy * 0.66f;
            reflection[i * 2 + 1] = cy - ry * 0.94f - dx * 0.66f + dy * 0.75f;
        }
        p.fillPoly(reflection, Glyph.withAlpha(0xFFFFFFCF, (int) (228 * fade)));
        p.fillCircle(cx - rx * 0.53f, cy - ry * 0.58f, ry * 0.078f,
                Glyph.withAlpha(0xFFFFFFCF, (int) (235 * fade)));
        p.fillCircle(cx - rx * 0.13f, cy - ry * 1.22f, ry * 0.043f,
                Glyph.withAlpha(0xFFFFFFDF, (int) (235 * fade)));
        for (int i = 0; i < 4; i++) {
            float px = cx + rx * (-0.66f + i * 0.43f);
            float py = cy + ry * (1.15f + 0.18f * hash(i + 8));
            p.fillEllipse(px, py, rx * (0.065f + 0.035f * hash(i)), ry * 0.065f,
                    Glyph.withAlpha(0xFFD5F566, (int) (100 * fade)));
        }

        // Sparse submerged bubbles: asymmetrical and faint, so they read as depth rather than spots.
        for (int i = 0; i < 9; i++) {
            float bx = cx + rx * (-0.66f + 0.165f * i);
            float by = cy + ry * (0.05f + 0.31f * hash(i * 13 + 5));
            float br = ry * (0.025f + 0.018f * hash(i * 7 + 2));
            p.fillEllipse(bx, by, br * 1.35f, br,
                    Glyph.withAlpha(i % 3 == 0 ? 0xFFFFFFFF : 0xFFBDF14A,
                            (int) ((48 + 42 * hash(i + 19)) * fade)));
        }

        // The reference face: two deep green button eyes with crisp reflected light.
        float eyeY = cy - ry * (vulnerable ? 0.07f : 0.10f), eyeDX = rx * 0.35f;
        float eyeRX = ry * 0.225f, eyeRY = ry * (vulnerable ? 0.29f : 0.265f);
        int ink = Glyph.withAlpha(0xFF154F19, (int) (255 * fade));
        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + side * eyeDX;
            p.fillEllipse(ex, eyeY, eyeRX, eyeRY, ink);
            p.fillCircle(ex - eyeRX * 0.25f, eyeY - eyeRY * 0.34f, eyeRX * 0.25f,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (245 * fade)));
            if (vulnerable) {
                // Pinched, raised inner brows make the face read as worried even at phone scale.
                float innerY = eyeY - eyeRY * 1.18f;
                p.line(ex - side * eyeRX * 0.82f, innerY - eyeRY * 0.24f,
                        ex + side * eyeRX * 0.70f, innerY + eyeRY * 0.05f, ink, ry * 0.050f);
            }
        }
        if (vulnerable) {
            // A trembling open gasp replaces the content smile while the glob can be pulled.
            float gasp = 1f + 0.07f * (float) Math.sin(c.clock * 9f);
            p.fillEllipse(cx, cy + ry * 0.18f, rx * 0.105f * gasp, ry * 0.145f / gasp, ink);
            p.fillEllipse(cx, cy + ry * 0.225f, rx * 0.060f, ry * 0.050f,
                    Glyph.withAlpha(0xFFFF8099, (int) (225 * fade)));
            p.fillEllipse(cx - rx * 0.38f, cy + ry * 0.14f, rx * 0.075f, ry * 0.045f,
                    Glyph.withAlpha(0xFFFF7291, (int) (115 * fade)));
            p.fillEllipse(cx + rx * 0.38f, cy + ry * 0.14f, rx * 0.075f, ry * 0.045f,
                    Glyph.withAlpha(0xFFFF7291, (int) (115 * fade)));
        } else {
            float[] smile = new float[18];
            for (int i = 0; i < 9; i++) {
                float u = -1f + i * 0.25f;
                smile[i * 2] = cx + rx * 0.15f * u;
                smile[i * 2 + 1] = cy + ry * (0.10f + 0.22f * (float) Math.sqrt(Math.max(0f, 1f - u * u)));
            }
            p.polyline(smile, ink, ry * 0.075f);
        }
    }

    private static float[] slimeInset(float[] skin, float cx, float cy, float rx, float ry,
            float sx, float sy, float ox, float oy) {
        float[] layer = new float[skin.length];
        for (int i = 0; i < skin.length; i += 2) {
            float dx = (skin[i] - cx) * sx + rx * ox;
            float dy = (skin[i + 1] - cy) * sy + ry * oy;
            // A deep dent can put an offset inset outside the skin; stop at the first boundary.
            float limit = 1f;
            for (int j = 0; j < skin.length; j += 2) {
                int k = (j + 2) % skin.length;
                float ex = skin[k] - skin[j], ey = skin[k + 1] - skin[j + 1];
                float px = skin[j] - cx, py = skin[j + 1] - cy;
                float cross = dx * ey - dy * ex;
                if (Math.abs(cross) < 1e-6f) continue;
                float reach = (px * ey - py * ex) / cross;
                float along = (px * dy - py * dx) / cross;
                if (reach > 0f && along >= 0f && along <= 1f)
                    limit = Math.min(limit, reach * 0.99f);
            }
            layer[i] = cx + dx * limit;
            layer[i + 1] = cy + dy * limit;
        }
        return layer;
    }

    /** Reference contour, radially warped by the live ring so pulls and dents remain visible. */
    static float[] slimeBossOutline(Softbody body) {
        float[] raw = body.outline(), out = new float[raw.length * 3];
        float cx = body.centreX(), cy = body.centreY();
        float rx = Math.max(1f, body.rest * 2f), ry = Math.max(1f, body.rest);
        for (int i = 0; i < out.length; i += 2) {
            float sample = i / 6f;
            int node = (int) sample;
            float t = sample - node;
            int a = node * 2, next = (a + 2) % raw.length;
            float nx = (raw[a] + (raw[next] - raw[a]) * t - cx) / rx;
            float ny = (raw[a + 1] + (raw[next + 1] - raw[a + 1]) * t - cy) / ry;
            float angle = (float) Math.atan2(ny, nx);
            if (angle < 0f) angle += Softbody.TAU;
            float at = angle / Softbody.TAU * SLIME_RADII.length;
            int k = (int) at % SLIME_RADII.length;
            float mix = at - (int) at;
            float radius = SLIME_RADII[k] + (SLIME_RADII[(k + 1) % SLIME_RADII.length]
                    - SLIME_RADII[k]) * mix;
            out[i] = cx + nx * rx * radius;
            out[i + 1] = cy + ny * ry * radius;
        }
        return out;
    }

    private static final float[] SLIME_RADII = slimeReferenceRadii();

    /** Cubic trace of the reference shoulders and irregular skirt, normalized about the face. */
    private static float[] slimeReferenceRadii() {
        float[] curves = {
            108,1290, 160,1260, 176,1205,
            185,1165, 186,1093, 222,1026,
            266,948, 341,898, 425,891,
            519,878, 597,927, 650,980,
            719,1049, 739,1131, 750,1197,
            756,1233, 764,1249, 784,1271,
            805,1293, 827,1310, 818,1335,
            812,1368, 773,1375, 715,1374,
            684,1372, 681,1378, 670,1399,
            654,1433, 608,1429, 566,1422,
            527,1411, 503,1416, 465,1419,
            401,1429, 363,1433, 309,1419,
            276,1411, 271,1401, 246,1393,
            198,1380, 154,1382, 130,1361,
            120,1350, 119,1340, 119,1329
        };
        int steps = 16;
        float[] trace = new float[curves.length / 6 * steps * 2];
        float x = 119f, y = 1329f;
        int at = 0;
        for (int i = 0; i < curves.length; i += 6) {
            for (int j = 0; j < steps; j++) {
                float t = j / (float) steps, u = 1f - t;
                trace[at++] = (u*u*u*x + 3*u*u*t*curves[i] + 3*u*t*t*curves[i+2]
                        + t*t*t*curves[i+4] - 450f) / 350f;
                trace[at++] = (u*u*u*y + 3*u*u*t*curves[i+1] + 3*u*t*t*curves[i+3]
                        + t*t*t*curves[i+5] - 1150f) / 175f;
            }
            x = curves[i + 4]; y = curves[i + 5];
        }
        float[] radii = new float[720];
        for (int i = 0; i < radii.length; i++) {
            float a = Softbody.TAU * i / radii.length;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            float nearest = Float.MAX_VALUE;
            for (int j = 0; j < trace.length; j += 2) {
                int k = (j + 2) % trace.length;
                float ex = trace[k] - trace[j], ey = trace[k + 1] - trace[j + 1];
                float cross = dx * ey - dy * ex;
                if (Math.abs(cross) < 1e-6f) continue;
                float r = (trace[j] * ey - trace[j + 1] * ex) / cross;
                float t = (trace[j] * dy - trace[j + 1] * dx) / cross;
                if (r > 0f && t >= 0f && t <= 1f) nearest = Math.min(nearest, r);
            }
            radii[i] = nearest == Float.MAX_VALUE ? 1f : nearest;
        }
        return radii;
    }

    /** Keep the pale strip inside each bent cross-section, including the tapered endpoints. */
    static float[] mushroomStemHighlight(float[] stem) {
        float[] glow = new float[stem.length];
        for (int i = 0; i < stem.length; i += 2) {
            float y = stem[i + 1], left = stem[i], right = stem[i];
            for (int j = 0; j < stem.length; j += 2) {
                int k = (j + 2) % stem.length;
                float y1 = stem[j + 1], y2 = stem[k + 1];
                if (y < Math.min(y1, y2) || y > Math.max(y1, y2)) continue;
                if (Math.abs(y2 - y1) < 0.0001f) {
                    left = Math.min(left, Math.min(stem[j], stem[k]));
                    right = Math.max(right, Math.max(stem[j], stem[k]));
                } else {
                    float x = stem[j] + (stem[k] - stem[j]) * (y - y1) / (y2 - y1);
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }
            glow[i] = left + (right - left) * 0.08f + (stem[i] - left) * 0.46f;
            glow[i + 1] = y;
        }
        return glow;
    }

    /** A fly-agaric silhouette built around the same live soft-body ring as every other boss. */
    private static void drawMushroom(Painter p, GameCore c, Layout L, Boss b, int col, float fade) {
        float rootX = b.body.centreX(), cy = b.body.centreY();
        float rx = b.body.radiusX(), ry = b.body.radiusY();
        // The cap may squash violently at every endpoint. The stalk must not inherit that
        // transient height or repeated shakes progressively shorten its resting frame.
        float stemR = Boss.bodyR(L);
        float charge = b.mushroomCharge <= 0f ? 0f
                : 1f - b.mushroomCharge / Boss.MUSHROOM_CHARGE_TIME;
        float squeeze = (float) Math.sin(charge * Math.PI * 0.5f);
        float shakeFlash = Math.max(0f, Math.min(1f,
                b.mushroomSweepFlash / 0.28f));
        float damagePulse = b.mushroomDamagePulse();
        float sy = 1f - squeeze * 0.30f, sx = 1f + squeeze * 0.16f;

        float capX = rootX + b.mushroomCapDX;
        float capY = cy + ry * 0.65f + b.mushroomCapDY;
        float stemBottom = cy + stemR * (3.55f - squeeze * 0.20f);
        float attachX = capX, attachY = capY + stemR * 0.36f * sy;
        float stemHalf = Boss.bodyR(L) * b.wide() * (0.35f + squeeze * 0.035f);
        int cream = Glyph.mix(Glyph.mix(0xFFFFF5ED, col, 0.015f), 0xFFFFFFFF,
                Math.max(shakeFlash * 0.72f, damagePulse * 0.92f));
        int stemEdge = Glyph.withAlpha(Glyph.mix(cream, 0xFFBBA488, 0.32f),
                (int) (220 * fade));
        int stemFill = Glyph.withAlpha(cream, (int) (255 * fade));

        // Warp the stalk's own live pressure-ring along a planted quadratic spine. Its noisy edge,
        // breathing and impact deformation now come from a second Softbody rather than stamped
        // circles; the warp only tapers it and guarantees the top remains sewn to the cap.
        float controlX = rootX + (attachX - rootX) * 0.27f;
        float controlY = (stemBottom + attachY) * 0.5f;
        // A dense fan of living mycelium sits under everything else. Damage wakes it up: pulses
        // race farther down the roots, brighten, and shift from moonlit cream to hostile coral.
        float damage = 1f - b.health();
        float rootPulse = 0.42f + 0.58f * (0.5f + 0.5f
                * (float) Math.sin(c.clock * (3.2f + damage * 10f)));
        int mycelium = Glyph.mix(Glyph.mix(0xFFDDFBEF, 0xFFFF416C, damage * 0.82f),
                0xFFFFFFFF, Math.max(shakeFlash * 0.62f, damagePulse));
        float rootSpan = Math.min(L.w * 0.47f, rx * 3.45f);
        // The body's centroid and measured radius breathe and recoil. Do not derive the buried tips
        // from either: homeY/rest are the planted pose, so only the inner roots flex with the stalk.
        float plantedY = b.body.homeY + b.body.rest * 3.55f;
        for (int branch = 0; branch < 19; branch++) {
            float n = branch / 18f * 2f - 1f;
            float bend = (Draw.hash(branch * 47 + 901) - 0.5f) * ry * 0.42f;
            float endX = rootX + n * rootSpan;
            // Keep the established width, but plant the tips on one broad downward arc. The
            // centre hangs deepest and the small hash variation keeps it alive rather than ruled.
            float arc = 1f - n * n;
            float endY = plantedY + b.body.rest
                    * (0.62f + 0.42f * arc + 0.10f * Draw.hash(branch * 61 + 17));
            float midX = rootX + n * rootSpan * 0.48f + bend;
            float plantedMidY = plantedY + b.body.rest
                    * (0.10f + 0.10f * Draw.hash(branch * 31 + 7));
            // A small share of the stalk motion reaches the junction; none reaches the tips.
            float midY = plantedMidY + (stemBottom - plantedY) * 0.18f;
            int glowA = (int) ((35f + damage * 85f) * rootPulse * fade);
            int coreA = (int) ((105f + damage * 125f) * fade);
            p.line(rootX, stemBottom, midX, midY, Glyph.withAlpha(mycelium, glowA),
                    ry * (0.085f + damage * 0.045f));
            p.line(midX, midY, endX, endY, Glyph.withAlpha(mycelium, glowA),
                    ry * (0.060f + damage * 0.035f));
            p.line(rootX, stemBottom, midX, midY, Glyph.withAlpha(0xFFFFF8DB, coreA),
                    ry * 0.018f);
            p.line(midX, midY, endX, endY, Glyph.withAlpha(0xFFFFF8DB, coreA),
                    ry * 0.013f);
            // Fine forked hyphae turn the radial fan into a tangled underground network.
            float fork = branch % 2 == 0 ? 1f : -1f;
            float forkX = midX + fork * rootSpan * (0.09f + 0.04f * damage);
            float forkY = endY + b.body.rest
                    * (0.08f + 0.05f * Draw.hash(branch * 73 + 5));
            p.line(midX, midY, forkX, forkY, Glyph.withAlpha(mycelium, glowA),
                    ry * 0.040f);
            p.line(midX, midY, forkX, forkY, Glyph.withAlpha(0xFFFFF8DB, coreA),
                    ry * 0.010f);
            float wave = (c.clock * (0.65f + damage * 1.25f)
                    + branch * 0.113f) % 1f;
            float px = midX + (endX - midX) * wave;
            float py = midY + (endY - midY) * wave;
            p.fillCircle(px, py, ry * (0.025f + damage * 0.035f) * rootPulse,
                    Glyph.withAlpha(mycelium, (int) ((125f + damage * 120f) * fade)));
        }
        p.fillEllipse(rootX, stemBottom + stemR * 0.05f, stemHalf * 2.0f, stemR * 0.13f,
                Glyph.withAlpha(0xFFE7C5CE, (int) (70 * fade)));
        float[] stemRaw = b.mushroomStem == null ? null : b.mushroomStem.outline();
        if (stemRaw != null) {
            float scx = b.mushroomStem.centreX(), scy = b.mushroomStem.centreY();
            float sry = Math.max(1f, b.mushroomStem.radiusY());
            float[] stem = new float[stemRaw.length];
            for (int i = 0; i < stem.length; i += 2) {
                float t = Math.max(0f, Math.min(1f, (scy + sry - stemRaw[i + 1]) / (sry * 2f)));
                float u = 1f - t;
                float spineX = u * u * rootX + 2f * u * t * controlX + t * t * attachX;
                float spineY = stemBottom + (attachY - stemBottom) * t;
                float taper = 1.22f - t * 0.32f;
                float side = (stemRaw[i] - scx) / Math.max(1f, b.mushroomStem.radiusX());
                float broaden = (float) Math.pow(Math.abs(side), 0.30f);
                stem[i] = spineX + Math.signum(side) * stemHalf * taper * broaden;
                stem[i + 1] = spineY;
            }
            p.strokePoly(stem, stemEdge, ry * 0.12f);
            p.fillPoly(stem, Glyph.withAlpha(0xFFE5D5C2, (int) (255 * fade)));
            float[] glow = mushroomStemHighlight(stem);
            p.fillPoly(glow, Glyph.withAlpha(0xFFFFF5F0, (int) (245 * fade)));
        }
        float angle = Math.max(-0.62f, Math.min(0.62f,
                (float) Math.atan2(attachX - rootX, Math.max(ry * 0.55f, stemBottom - attachY))));
        float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);

        float[] raw = b.body.outline();
        float[] cap = new float[raw.length];
        for (int i = 0; i < raw.length; i += 2) {
            float rawDy = raw[i + 1] - cy;
            float dx = (raw[i] - rootX) * sx * 1.85f;
            float dy = rawDy * sy * (rawDy < 0f ? 2.25f : 0.48f);
            cap[i] = capX + dx * ca - dy * sa;
            cap[i + 1] = capY + dx * sa + dy * ca;
        }
        int reactionColor = b.mushroomAngry > Boss.MUSHROOM_REACTION_RELEASE ? 0xFFFFCF83 : 0xFFFF8CBD;
        int red = Glyph.mix(Glyph.mix(0xFFEE2928, reactionColor, damagePulse * 0.90f),
                0xFFFFFFFF, shakeFlash * 0.78f);
        p.fillPoly(cap, Glyph.withAlpha(Glyph.mix(0xFFAE2029, reactionColor, damagePulse * 0.60f), (int) (255 * fade)));
        float[] bright = new float[cap.length];
        for (int i = 0; i < cap.length; i += 2) {
            bright[i] = capX + (cap[i] - capX) * 0.86f - rx * 0.16f;
            bright[i + 1] = capY + (cap[i + 1] - capY) * 0.94f - ry * 0.02f;
        }
        p.fillPoly(bright, Glyph.withAlpha(red, (int) (255 * fade)));

        // The cream underside is a shallow ellipse, with radial gills anchored at the stalk.
        float brimW = rx * sx * 1.72f, brimH = ry * sy * 0.29f;
        float[] underside = new float[96];
        for (int i = 0; i < 48; i++) {
            float a = Softbody.TAU * i / 48f;
            float dx = (float) Math.cos(a) * brimW;
            float dy = ry * sy * 0.22f + (float) Math.sin(a) * brimH;
            underside[i * 2] = capX + dx * ca - dy * sa;
            underside[i * 2 + 1] = capY + dx * sa + dy * ca;
        }
        p.fillPoly(underside, Glyph.withAlpha(0xFFD5BDA1, (int) (255 * fade)));
        for (int i = 0; i < 32; i++) {
            float a = Softbody.TAU * i / 32f;
            float dx = (float) Math.cos(a) * brimW * 0.97f;
            float dy = ry * sy * 0.22f + (float) Math.sin(a) * brimH * 0.94f;
            p.line(attachX, attachY, capX + dx * ca - dy * sa, capY + dx * sa + dy * ca,
                    Glyph.withAlpha(0xFFFFF0D5, (int) (245 * fade)), ry * 0.027f);
        }
        p.strokePoly(underside, Glyph.withAlpha(0xFFA6212B, (int) (255 * fade)), ry * 0.045f);

        // A broad salmon reflection follows the crown tilt.
        orientedEllipse(p, capX - rx * 0.65f * ca + ry * 1.12f * sa,
                capY - rx * 0.65f * sa - ry * 1.12f * ca,
                ca * 0.80f + sa * 0.60f, sa * 0.80f - ca * 0.60f,
                rx * 0.43f, ry * 0.24f, Glyph.withAlpha(0xFFFF8780, (int) (205 * fade)));
        // Large crown warts taper into dense, flatter flecks near the rim.
        float[][] spots = {{-.72f,-.56f,.13f},{-.40f,-.82f,.13f},{.04f,-.91f,.14f},
                {.46f,-.74f,.12f},{.77f,-.48f,.13f},{-.45f,-.44f,.13f},
                {0f,-.56f,.18f},{.35f,-.35f,.15f},{-.68f,-.17f,.14f},
                {-.22f,-.21f,.11f},{.67f,-.15f,.14f},{.13f,-.13f,.10f}};
        for (float[] spot : spots) {
            float dx = spot[0] * rx * sx * 1.72f;
            float dy = spot[1] * ry * sy * 2.25f;
            orientedEllipse(p, capX + dx * ca - dy * sa, capY + dx * sa + dy * ca,
                    ca, sa, spot[2] * ry * 1.22f, spot[2] * ry,
                    Glyph.withAlpha(0xFFF4F0F6, (int) (255 * fade)));
        }
        for (int i = 0; i < 17; i++) {
            float u = -0.94f + i * 1.88f / 16f;
            float dx = u * rx * sx * 1.72f;
            float dy = -ry * sy * (0.02f + 0.13f * hash(i + 120));
            orientedEllipse(p, capX + dx * ca - dy * sa, capY + dx * sa + dy * ca,
                    ca, sa, ry * (0.045f + 0.035f * hash(i + 33)), ry * 0.035f,
                    Glyph.withAlpha(0xFFF4F0F6, (int) (255 * fade)));
        }

        for (int i = 0; i < Boss.MUSHROOM_DUST; i++) {
            float life = b.mushroomDustLife[i];
            if (life <= 0f) continue;
            float size = ry * (0.019f + (i % 3) * 0.006f);
            int alpha = (int) (225f * fade * Math.min(1f, life / 0.35f));
            p.fillCircle(b.mushroomDustX[i], b.mushroomDustY[i], size * 1.8f,
                    Glyph.withAlpha(0xFFEBC99C, alpha / 4));
            p.fillCircle(b.mushroomDustX[i], b.mushroomDustY[i], size,
                    Glyph.withAlpha(0xFFFFE9BC, alpha));
        }

        // Ruffled skirt collar bends with the spine beneath the gills.
        float collarT = 0.73f, collarU = 1f - collarT;
        float collarX = collarU * collarU * rootX + 2f * collarU * collarT * controlX
                + collarT * collarT * attachX;
        float collarY = stemBottom + (attachY - stemBottom) * collarT;
        float[] collar = new float[32];
        collar[0] = attachX - stemHalf * 0.87f; collar[1] = attachY;
        collar[2] = attachX + stemHalf * 0.87f; collar[3] = attachY;
        for (int i = 0; i < 14; i++) {
            float u = 1f - i * 2f / 13f;
            collar[4 + i * 2] = collarX + u * stemHalf * 1.55f;
            collar[5 + i * 2] = collarY + stemR * (i % 2 == 0 ? 0.17f : 0.08f);
        }
        p.fillPoly(collar, Glyph.withAlpha(0xFFE8D9C7, (int) (255 * fade)));
        p.polyline(java.util.Arrays.copyOfRange(collar, 4, collar.length),
                Glyph.withAlpha(0xFFB59A74, (int) (245 * fade)), stemR * 0.045f);

        // Face on the stem keeps the boss alive without disguising the mushroom silhouette.
        float faceT = 0.38f, faceU = 1f - faceT;
        float faceX = faceU * faceU * rootX + 2f * faceU * faceT * controlX
                + faceT * faceT * attachX;
        float faceY = faceU * faceU * stemBottom + 2f * faceU * faceT * controlY
                + faceT * faceT * attachY;
        p.fillCircle(faceX - stemHalf * 0.38f, faceY, ry * 0.055f,
                Glyph.withAlpha(0xFF4A2631, (int) (245 * fade)));
        p.fillCircle(faceX + stemHalf * 0.38f, faceY, ry * 0.055f,
                Glyph.withAlpha(0xFF4A2631, (int) (245 * fade)));
        if (b.mushroomAngry > 0f) {
            p.fillEllipse(faceX, faceY + ry * 0.20f, ry * 0.10f, ry * 0.125f,
                    Glyph.withAlpha(0xFF4A2631, (int) (240 * fade)));
        } else p.line(faceX - stemHalf * 0.24f, faceY + ry * 0.16f,
                faceX + stemHalf * 0.24f, faceY + ry * 0.16f,
                Glyph.withAlpha(0xFF4A2631, (int) (220 * fade)), ry * 0.035f);

        if (b.mushroomReject > 0f) {
            float taunt = b.mushroomReject;
            float flex = ry * (0.72f + 0.12f * (float) Math.sin(c.clock * 24f));
            int tauntCol = Glyph.withAlpha(0xFFFF4668, (int) (235 * taunt * fade));
            p.line(capX - rx * 1.35f, capY + ry * 0.25f,
                    capX - rx * 1.75f, capY - flex, tauntCol, ry * 0.11f);
            p.line(capX + rx * 1.35f, capY + ry * 0.25f,
                    capX + rx * 1.75f, capY - flex, tauntCol, ry * 0.11f);
            p.line(faceX - stemHalf * 0.55f, faceY - ry * 0.10f,
                    faceX - stemHalf * 0.08f, faceY, tauntCol, ry * 0.055f);
            p.line(faceX + stemHalf * 0.55f, faceY - ry * 0.10f,
                    faceX + stemHalf * 0.08f, faceY, tauntCol, ry * 0.055f);
        }

        if (b.mushroomMeterAlpha > 0f) {
            float meterFade = fade * b.mushroomMeterAlpha;
            float meterW = L.w * 0.68f, meterH = Math.max(20f, L.unit * 0.42f);
            float meterX = L.w * 0.5f;
            float meterY = L.playTop + L.unit * 2.55f;
            int white = Glyph.withAlpha(0xFFFFFFFF, (int) (250 * meterFade));
            int shadow = Glyph.withAlpha(0xFF27152F, (int) (185 * meterFade));
            p.fillPoly(pill(meterX, meterY, meterW * 0.5f, meterH * 1.28f, 16), shadow);
            p.strokePoly(pill(meterX, meterY, meterW * 0.5f, meterH * 1.28f, 16),
                    white, Math.max(4f, L.unit * 0.085f));
            p.line(meterX, meterY - meterH * 1.02f, meterX, meterY + meterH * 1.02f,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (175 * meterFade)),
                    Math.max(3f, L.unit * 0.055f));
            float travel = meterW * 0.5f - meterH * 1.65f;
            float targetX = meterX + b.mushroomGuideX * travel;
            float pulse = 1f + 0.10f * (float) Math.sin(c.clock * 13f);
            float playerX = meterX + b.mushroomPlayerX * travel;
            boolean aligned = Math.abs(b.mushroomPlayerX - b.mushroomGuideX) < 0.09f
                    && Math.abs(b.mushroomGuideX - b.mushroomGuideTarget) < Boss.MUSHROOM_GUIDE_WINDOW;
            int player = Glyph.withAlpha(aligned ? 0xFF8CFF79 : 0xFF65F5E3,
                    (int) (255 * meterFade));
            p.fillCircle(playerX, meterY, meterH * 1.18f, player);
            p.fillCircle(playerX, meterY, meterH * 0.62f,
                    Glyph.withAlpha(0xFF17333A, (int) (250 * meterFade)));
            p.line(playerX, meterY - meterH * 1.32f, playerX, meterY + meterH * 1.32f,
                    player, Math.max(3f, L.unit * 0.060f));
            float targetHalfW = meterH * 2.34f * pulse;
            float targetHalfH = meterH * 0.70f * pulse;
            p.fillRect(targetX - targetHalfW, meterY - targetHalfH,
                    targetX + targetHalfW, meterY + targetHalfH,
                    Glyph.withAlpha(0xFFFF477E, (int) (245 * meterFade)));
            float pipY = meterY + meterH + L.unit * 0.38f;
            float gap = L.unit * 0.46f;
            float first = L.w * 0.5f - gap * (Boss.MUSHROOM_SHAKES - 1) * 0.5f;
            for (int i = 0; i < Boss.MUSHROOM_SHAKES; i++) {
                float pipR = L.unit * (i < b.mushroomShakes ? 0.155f : 0.112f);
                p.fillCircle(first + i * gap, pipY, pipR,
                        Glyph.withAlpha(i < b.mushroomShakes ? 0xFFFFFFFF : 0xFF76596D,
                                (int) (245 * meterFade)));
                if (i < b.mushroomShakes)
                    p.strokeCircle(first + i * gap, pipY, pipR * 1.28f,
                            Glyph.withAlpha(0xFFFF477E, (int) (210 * meterFade)),
                            Math.max(2f, L.unit * 0.035f));
            }
        }
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
            // Round crown over a shallow lower lobe, with the live mantle's elastic motion.
            float width = sn < 0f ? 0.74f - 0.38f * sn + 0.05f * sn * sn
                    : 0.74f + 0.20f * sn;
            float height = sn < 0f ? 1.45f : 0.82f;
            mantle[i] = cx + cs * rx * width * elastic;
            mantle[i + 1] = cy + sn * ry * height * elastic - ry * 0.11f;
        }
        p.fillPoly(mantle, Glyph.withAlpha(col, (int) (250 * fade)));
        p.strokePoly(mantle, Glyph.withAlpha(OCTO_INK,
                (int) (255 * fade)), b.body.radius() * 0.06f);

        // Wet sticker-like highlight from the reference, kept translucent to match the slime family.
        p.fillEllipse(cx - rx * 0.35f, cy - ry * 0.43f, rx * 0.17f, ry * 0.27f,
                Glyph.withAlpha(OCTO_GLEAM, (int) (235 * fade)));
        p.fillCircle(cx - rx * 0.18f, cy - ry * 0.64f, rx * 0.085f,
                Glyph.withAlpha(OCTO_GLEAM, (int) (245 * fade)));

        float eyeY = cy - ry * 0.06f;
        float eyeR = rx * 0.145f;
        float eyeDx = rx * 0.34f;
        int ink = Glyph.withAlpha(OCTO_INK, (int) (255 * fade));
        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + side * eyeDx;
            if (b.beaten) {
                p.line(ex-eyeR, eyeY-eyeR*.25f, ex+eyeR, eyeY+eyeR*.25f, ink, rx*.065f);
                continue;
            }
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
            smile[k * 2 + 1] = mouthY + ry * (b.beaten ? -0.10f : 0.10f) * (1f - u * u);
        }
        p.polyline(smile, ink, rx * 0.055f);
        if (!b.beaten && b.octoVulnerableArm >= 0) {
            float urgency = Math.min(1f, b.octoDragTime / 2f);
            float wince = 0.5f + 0.5f * (float) Math.sin(
                    c.clock * (8f + urgency * 20f));
            // Squeezed brows, flushed cheeks and a trembling frown during the whole catch window.
            p.line(cx - eyeDx - eyeR, eyeY - eyeR * (1.22f + 0.18f * wince),
                    cx - eyeDx + eyeR, eyeY - eyeR * (0.78f - 0.10f * wince),
                    Glyph.withAlpha(0xFFFFD83D, (int) (245 * fade)), rx * 0.055f);
            p.line(cx + eyeDx - eyeR, eyeY - eyeR * (0.78f - 0.10f * wince),
                    cx + eyeDx + eyeR, eyeY - eyeR * (1.22f + 0.18f * wince),
                    Glyph.withAlpha(0xFFFFD83D, (int) (245 * fade)), rx * 0.055f);
            p.fillCircle(cx - rx * 0.34f, mouthY + ry * 0.04f, rx * 0.105f,
                    Glyph.withAlpha(OCTO_SUCKER, (int) ((105 + 75 * wince) * fade)));
            p.fillCircle(cx + rx * 0.34f, mouthY + ry * 0.04f, rx * 0.105f,
                    Glyph.withAlpha(OCTO_SUCKER, (int) ((105 + 75 * wince) * fade)));
            p.fillEllipse(cx, mouthY + ry * 0.06f, rx * 0.25f, ry * 0.16f,
                    Glyph.withAlpha(col, (int) (245 * fade)));
            float[] frown = new float[14];
            for (int k = 0; k < 7; k++) {
                float u = -1f + 2f * k / 6f;
                frown[k * 2] = cx + rx * 0.19f * u;
                frown[k * 2 + 1] = mouthY + ry * (0.03f - 0.11f * (1f - u * u))
                        + ry * 0.025f * wince;
            }
            p.polyline(frown, ink, rx * 0.060f);
        }
        if (!b.beaten && b.octoEat > 0f) {
            float eaten = 1f - b.octoEat;
            float chew = 0.5f + 0.5f * (float) Math.sin(eaten * Math.PI * 7f);
            float gulp = Math.min(1f, eaten * 5f);
            // Puffed cheeks and a rubbery chomping mouth make the stolen key feel swallowed.
            p.fillCircle(cx - rx * 0.31f, mouthY, rx * (0.16f + 0.035f * chew),
                    Glyph.withAlpha(OCTO_SUCKER, (int) (125 * fade * gulp)));
            p.fillCircle(cx + rx * 0.31f, mouthY, rx * (0.16f + 0.035f * chew),
                    Glyph.withAlpha(OCTO_SUCKER, (int) (125 * fade * gulp)));
            p.fillEllipse(cx, mouthY + ry * 0.015f, rx * (0.16f + 0.035f * chew),
                    ry * (0.055f + 0.11f * chew), ink);
            p.fillEllipse(cx, mouthY + ry * (0.045f + 0.025f * chew), rx * 0.085f,
                    ry * 0.038f, Glyph.withAlpha(OCTO_SUCKER, (int) (235 * fade)));
            float wordBounce = (float) Math.sin(eaten * Math.PI * 7f) * ry * 0.08f;
            p.text(eaten < 0.48f ? "NOM!" : "YUM!", cx, cy - ry * 1.55f + wordBounce,
                    type(rx * 0.29f), Glyph.withAlpha(0xFFFFD83D,
                            (int) (245 * fade * Math.min(1f, b.octoEat * 5f))), Painter.CENTER, true);
            for (int crumb = 0; crumb < 6; crumb++) {
                float hop = (eaten * 1.7f + crumb * 0.29f) % 1f;
                float side = crumb == 1 ? -1f : 1f;
                p.fillCircle(cx + side * rx * (0.18f + hop * 0.26f),
                        mouthY - ry * (0.08f + hop * 0.28f), rx * 0.025f * (1f - hop),
                        Glyph.withAlpha(0xFFFFD83D, (int) (210 * fade * (1f - hop))));
            }
        }
        if (b.octoTaunt > 0f) {
            float appear = Math.min(1f, (1f - b.octoTaunt) * 9f);
            float vanish = Math.min(1f, b.octoTaunt * 4f);
            float taunt = appear * vanish * fade;
            float bounce = (float) Math.sin(c.clock * 11f) * ry * 0.08f;
            p.text("TOO SLOW!", cx, cy - ry * 1.62f + bounce, type(rx * 0.26f),
                    Glyph.withAlpha(0xFFFFD83D, (int) (255 * taunt)), Painter.CENTER, true);
            p.arc(cx, mouthY - ry * 0.02f, rx * 0.22f, ry * 0.15f, 8f, 164f,
                    Glyph.withAlpha(0xFFFFD83D, (int) (235 * taunt)), rx * 0.055f);
        }
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
        // Paint outside-in so the central arms sit in front at every crossing.
        for (int layer = 0; layer < Boss.OCTO_ARMS; layer++) {
            int a = layer % 2 == 0 ? layer / 2 : Boss.OCTO_ARMS - 1 - layer / 2;
            float[] pts = new float[Boss.OCTO_NODES * 2];
            for (int n = 0; n < Boss.OCTO_NODES; n++) { pts[n * 2] = b.octoX[a][n]; pts[n * 2 + 1] = b.octoY[a][n]; }
            boolean dying = !b.beaten && a == b.octoDyingArm && b.octoDeath > 0f;
            if ((b.octoArms & (1 << a)) != 0 || dying || b.beaten) {
                float death = dying ? Math.min(1f, b.octoDeath) : 0f;
                boolean warning = !b.beaten && a == b.octoAttackArm && b.octoTarget >= 0
                        && (b.octoSweep < 1f || b.octoCharge < 1f);
                boolean vulnerable = !b.beaten && a == b.octoVulnerableArm;
                boolean escaping = !b.beaten && a == b.octoEscapeArm && b.octoEscape > 0f;
                float tug = vulnerable ? Math.min(1f, b.octoDragTime / 2f) : 0f;
                // The warning heartbeat accelerates continuously toward the two-second escape.
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * (8f + 24f * tug));
                int armCol = dying
                        ? Glyph.mix(col, death < 0.38f ? 0xFFFFFF8A : 0xFFFF6A86,
                                0.82f - death * 0.30f)
                        : escaping ? Glyph.mix(OCTO_SKIN, 0xFFFFFFFF, 0.38f + 0.52f * b.octoEscape)
                        : vulnerable ? Glyph.mix(OCTO_SKIN, YELLOW,
                                0.54f + 0.34f * pulse + 0.10f * tug)
                        : warning ? Glyph.mix(OCTO_SKIN, 0xFFFF355F,
                                0.35f + 0.65f * pulse * (0.45f + 0.55f * b.octoCharge)) : col;
                float[] curve = smoothTentacle(pts);
                float variety = 0.88f + 0.16f * (float) Math.sin(a * 2.17f);
                int steps = curve.length / 2 - 1;
                // Complete each coat before the next, so dark segment caps cannot stripe the arm.
                for (int coat = 0; coat < 4; coat++) {
                    for (int s = 0; s < steps; s++) {
                        float u = s / (float) steps;
                        float width = thick * variety * (1.18f - 0.76f * u)
                                * (dying ? 1f - death * 0.72f : 1f);
                        float x1 = curve[s * 2], y1 = curve[s * 2 + 1];
                        float x2 = curve[s * 2 + 2], y2 = curve[s * 2 + 3];
                        if (coat == 0) p.line(x1, y1, x2, y2,
                                Glyph.withAlpha(OCTO_INK, (int) (255 * fade)),
                                width * 1.22f);
                        if (coat == 1) p.line(x1, y1, x2, y2, Glyph.withAlpha(armCol, (int) (255 * fade)), width);
                        if (coat == 2) p.line(x1 + width * 0.16f, y1 + width * 0.16f,
                                x2 + width * 0.16f, y2 + width * 0.16f,
                                Glyph.withAlpha(Glyph.mix(armCol, OCTO_SHADE, 0.72f),
                                        (int) (255 * fade)), width * 0.48f);
                        if (coat == 3) p.line(x1 - width * 0.10f, y1 - width * 0.10f,
                                x2 - width * 0.10f, y2 - width * 0.10f,
                                Glyph.withAlpha(Glyph.mix(armCol, OCTO_GLEAM, 0.30f),
                                        (int) (255 * fade)), width * 0.14f);
                    }
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
                            Glyph.withAlpha(OCTO_SHADE, (int) (245 * fade)));
                    // A smaller inset cup gives every sucker a visible recessed centre.
                    orientedEllipse(p, suckerX - nx * suckerR * 0.10f,
                            suckerY - ny * suckerR * 0.10f, dx / len, dy / len,
                            suckerR * 1.18f, suckerR * 0.56f,
                            Glyph.withAlpha(OCTO_SUCKER, (int) (238 * fade)));
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
                        Glyph.withAlpha(armCol, (int) (255 * fade)));
                if (a == b.octoAttackArm) drawOctoCharge(p, b, curve, thick, fade);
                if (vulnerable && b.octoCoil >= 0.72f) {
                    float grabPulse = 0.86f + 0.14f * (float) Math.sin(c.clock * 8f);
                    float grabR = thick * 0.82f * grabPulse;
                    p.fillCircle(tipX, tipY, grabR * 1.72f,
                            Glyph.withAlpha(YELLOW, (int) (62 * fade)));
                    p.strokeCircle(tipX, tipY, grabR * 1.30f,
                            Glyph.withAlpha(0xFFFFD83D, (int) (175 * fade)), grabR * 0.13f);
                    p.fillCircle(tipX, tipY, grabR * 0.98f,
                            Glyph.withAlpha(0xFF5A102B, (int) (238 * fade)));
                    p.strokeCircle(tipX, tipY, grabR,
                            Glyph.withAlpha(0xFFFFFFFF, (int) (255 * fade)), grabR * 0.20f);
                    p.fillCircle(tipX, tipY, grabR * 0.58f,
                            Glyph.withAlpha(0xFFFFD83D, (int) (255 * fade)));
                    caret(p, tipX, tipY, grabR * 1.12f,
                            Glyph.withAlpha(0xFFFFFFFF, (int) (255 * fade)));
                    p.text("DRAG!", tipX, tipY + grabR * 1.72f, type(grabR * 0.50f),
                            Glyph.withAlpha(0xFFFFFFFF, (int) (250 * fade)), Painter.CENTER, true);
                }

            } else {
                float gx = pts[2], gy = pts[3];
                p.fillEllipse(gx, gy, thick * 0.58f, thick * 0.43f, Glyph.withAlpha(col, (int) (190 * fade)));
                p.fillCircle(gx - thick * 0.14f, gy - thick * 0.13f, thick * 0.12f, Glyph.withAlpha(0xFFFFFFFF, (int) (120 * fade)));
            }
        }
    }

    /** Foreground pass: a stolen character must remain readable over arms and mantle. */
    /** The settled-arm charge owns the pulse; its endpoint is the strike's first frame. */
    static float octoPulseProgress(Boss b) {
        if (b.beaten || b.octoTarget < 0 || b.octoAttackArm < 0 || b.octoSweep < 1f
                || b.octoReach > 0.15f) return -1f;
        return Math.max(0f, Math.min(1f, b.octoCharge));
    }

    /** Distance along the drawn curve, independent of unequal node spacing. */
    static float[] octoPulsePoint(float[] curve, float progress) {
        float total = 0f;
        for (int i = 2; i < curve.length; i += 2)
            total += (float) Math.hypot(curve[i] - curve[i - 2], curve[i + 1] - curve[i - 1]);
        float remaining = total * Math.max(0f, Math.min(1f, progress));
        for (int i = 2; i < curve.length; i += 2) {
            float dx = curve[i] - curve[i - 2], dy = curve[i + 1] - curve[i - 1];
            float length = (float) Math.hypot(dx, dy);
            if (remaining <= length || i == curve.length - 2) {
                float t = length < 0.001f ? 0f : Math.min(1f, remaining / length);
                return new float[] {curve[i - 2] + dx * t, curve[i - 1] + dy * t};
            }
            remaining -= length;
        }
        return new float[] {curve[0], curve[1]};
    }

    private static void drawOctoCharge(Painter p, Boss b, float[] curve, float thick, float fade) {
        float progress = octoPulseProgress(b);
        if (progress < 0f) return;
        float alpha = fade * (b.octoCharge < 1f ? 1f : Math.max(0f, 1f - b.octoReach / 0.15f));
        int color = Glyph.COLOR[b.octoTarget];
        // A short bright wake makes the direction readable even on the quick charge.
        for (int i = 0; i < 6; i++) {
            float at = progress - i * 0.025f;
            if (at < 0f) break;
            float[] point = octoPulsePoint(curve, at);
            p.fillCircle(point[0], point[1], thick * (0.38f - i * 0.04f),
                    Glyph.withAlpha(color, (int) ((145 - i * 20) * alpha)));
        }
        float[] point = octoPulsePoint(curve, progress);
        float r = thick * 0.37f;
        p.fillCircle(point[0], point[1], r * 1.6f, Glyph.withAlpha(color, (int) (85 * alpha)));
        p.fillCircle(point[0], point[1], r, Glyph.withAlpha(color, (int) (255 * alpha)));
        p.strokeCircle(point[0], point[1], r, Glyph.withAlpha(INK, (int) (255 * alpha)), r * 0.22f);
        p.fillCircle(point[0], point[1], r * 0.36f, Glyph.withAlpha(INK, (int) (255 * alpha)));
    }

    private static void drawCapturedKey(Painter p, Layout L, Boss b, float fade) {
        if (b.octoAttackArm < 0 || b.octoCaptured < 0) return;
        int tip = Boss.OCTO_NODES - 1;
        float tipX = b.octoX[b.octoAttackArm][tip];
        float tipY = b.octoY[b.octoAttackArm][tip];
        int g = b.octoCaptured, keyCol = Glyph.COLOR[g];
        float swallowed = Math.max(0f, Math.min(1f, (b.octoReturn - 0.68f) / 0.32f));
        float keyR = L.keyR * (1f - swallowed * 0.72f);
        p.fillCircle(tipX, tipY, keyR * 1.18f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (72 * fade * (1f - swallowed))));
        p.fillPoly(Glyph.hex(tipX, tipY, keyR), Glyph.withAlpha(keyCol, (int) (150 * fade)));
        p.strokePoly(Glyph.hex(tipX, tipY, keyR),
                Glyph.withAlpha(0xFFFFFFFF, (int) (255 * fade)), keyR * 0.13f);
        Kawaii.draw(p, g, tipX, tipY, keyR * 0.60f,
                Glyph.withAlpha(keyCol, (int) (255 * fade)), 1f, 0.1f);
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
            Slime.cube(p, piece, c.clock + i * 0.31f, halfCol,
                    b.pieceNodeIndex(i) == b.pinchNode, fade);
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
            Slime.cube(p, remnant, c.clock + n * 0.31f, remnantCol,
                    false, fade * DIVIDE_REMNANT_ALPHA);
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
        float[] patch = globPath(body, Slime.cubeOutline(body), x, y, 0.82f);
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
                letterBadge(p, c, L, b.chainLetter(), cx, slimeBadgeY(L, b),
                        slimeBoltR(c, L, urgency), fade,
                        b.open());
            }
            // And how far the chain has got: the press does not move the health bar, so without
            // this a run of four presses looks like four presses that did nothing. Hung off the
            // resting height rather than the live one so it does not get swallowed by the body every
            // time a squash flattens it.
            splitGauge(p, c, L, b, cx, slimeBadgeY(L, b) + badgeR(L) * 1.65f, fade);
        } else if (b.kind == Boss.SPLITTER) {
            for (int i = 0; i < b.pieceCount(); i++) {
                float x = b.pieceX(i, L), y = b.pieceY(i, L), pr = b.pieceR(i, L);
                int charge = b.pieceCharge(i);
                if (charge < Boss.DIVIDE_HITS) {
                    letterBadge(p, c, L, b.pieceWant(i), x, y - pr * 1.35f,
                            standardBoltR(L, 1f), fade, true);
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
        }
    }

    /** Centre of the slime's charged bolt character, below its live underside. */
    static float slimeBadgeY(Layout L, Boss b) {
        float ry = b.body == null ? Boss.bodyR(L) : Math.min(b.body.radiusY(), Boss.bodyR(L));
        float bottom = ornamentY(L, b) + ry * 1.65f;
        if (b.body != null) {
            float[] skin = slimeBossOutline(b.body);
            for (int i = 1; i < skin.length; i += 2) bottom = Math.max(bottom, skin[i]);
        }
        return bottom + L.unit * 0.35f + badgeR(L);
    }

    /** Radius of the slime's charging glob, carried unchanged into its launched bolts. */
    static float standardBoltR(Layout L, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        return L.keyR * (0.42f + 0.30f * progress);
    }

    static float slimeBoltR(GameCore c, Layout L, float urgency) {
        urgency = Math.max(0f, Math.min(1f, urgency));
        float pop = 1f + urgency * 0.32f
                + 0.08f * urgency * (float) Math.sin(c.clock * (8f + urgency * 10f));
        return L.unit * 1.08f * pop;
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

            if (t == Boss.E_GLOB) {
                glob(p, c, L, b, i, x, y, rr, life, held, fade, bodyCol);
            }
        }
    }

    /**
     * The loose place in the slime skin: a local colour change, not a separate object.
     *
     * Its outer edge is a literal arc copied from the live soft-body outline. The inward copy closes
     * the colour region, so it is one marked piece of skin rather than a shape laid over the body.
     */
    private static void glob(Painter p, GameCore c, Layout L, Boss b, int i, float x, float y, float rr,
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
            float pull = Math.max(0f, Math.min(1f, (drag - 0.20f) / 0.65f));
            pull = pull * pull * (3f - 2f * pull);
            float tipX = x + side * rr * pull * 2.4f;
            tipX = Math.max(L.playLeft + rr, Math.min(L.playRight - rr, tipX));
            float tipY = y - rr * 0.10f * (float) Math.sin(drag * Math.PI);
            float handAngle = side > 0f ? 2.45f : 0.69f;
            Renderer.touchHint(p, tipX, tipY, rr * 0.85f, handAngle,
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
        return globPath(softbody, slimeBossOutline(softbody), x, y, grow);
    }

    private static float[] globPath(Softbody softbody, float[] body, float x, float y, float grow) {
        int n = body.length / 2, nearest = 0;
        float best = Float.MAX_VALUE;
        for (int q = 0; q < n; q++) {
            float dx = body[q * 2] - x, dy = body[q * 2 + 1] - y;
            float d = dx * dx + dy * dy;
            if (d < best) { best = d; nearest = q; }
        }
        int span = Math.max(1, Math.round(5f * grow * n / (Softbody.NODES * Softbody.SMOOTH)));
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

    /** A countdown as pips round a circle, emptying clockwise. */

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
                : b.beaten ? b.defeatPromptFade() : 1f;
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
        for (int i = 0; i < Boss.MAX_BOLTS; i++) {
            if (!b.blive[i]) continue;
            int g = b.bglyph[i];
            float x = b.boltX(i, L), y = b.boltY(i, L);
            float at = b.boltAt(i);
            // A slime bolt keeps the fully charged glob's size after launch. Other boss projectiles
            // grow as they approach to make their final half-second read more loudly.
            float rr = b.kind == Boss.SLIME
                    ? slimeBoltR(c, L, 1f)
                    : standardBoltR(L, at);
            int col = Glyph.COLOR[g];
            float sporeMorph = b.kind == Boss.MUSHROOM ? Math.min(1f, at / 0.30f) : 1f;
            if (b.kind == Boss.MUSHROOM) {
                float sporeR = rr * (0.34f + 0.66f * sporeMorph);
                int spore = Glyph.mix(0xFFFFF1C8, col, sporeMorph);
                p.fillCircle(x, y, sporeR, Glyph.withAlpha(spore, 220));
                for (int k = 0; k < 3; k++) {
                    float a = c.clock * 4f + k * Softbody.TAU / 3f;
                    p.fillCircle(x + (float) Math.cos(a) * rr * 0.28f * (1f - sporeMorph),
                            y + (float) Math.sin(a) * rr * 0.28f * (1f - sporeMorph),
                            rr * 0.11f, Glyph.withAlpha(0xFFFFF8DF,
                                    (int) (170 * (1f - sporeMorph))));
                }
                // Twenty-five fine motes travel as a loose cloud around the letter itself.
                // Stable per-particle radii prevent the cloud from collapsing into a regular ring.
                for (int k = 0; k < 25; k++) {
                    float seed = Draw.hash(i * 97 + k * 43 + 1701);
                    float orbit = rr * (0.58f + seed * 1.08f);
                    float a = c.clock * (0.75f + Draw.hash(k * 71 + 9) * 0.72f)
                            + k * Softbody.TAU / 25f + i * 0.63f;
                    float dustX = x + (float) Math.cos(a) * orbit;
                    float dustY = y + (float) Math.sin(a) * orbit * 0.72f
                            + (float) Math.sin(c.clock * 2.1f + k) * rr * 0.10f;
                    float dustR = rr * (0.035f + Draw.hash(k * 59 + i * 13) * 0.026f);
                    int dustA = (int) ((105f + seed * 80f) * Math.min(1f, at * 12f));
                    p.fillCircle(dustX, dustY, dustR,
                            Glyph.withAlpha(k % 3 == 0 ? 0xFFFFDCA5 : 0xFFFFF5D8, dustA));
                }
                col = spore;
            }

            // A tail back toward the launch point, so the direction reads in one frame.
            float tx = b.bsx[i], ty = b.bsy[i];
            if (b.kind != Boss.MUSHROOM) {
                for (int k = 1; k <= 3; k++) {
                    float f = 1f - 0.10f * k;
                    p.fillCircle(tx + (x - tx) * f, ty + (y - ty) * f,
                            rr * (0.55f - 0.12f * k), Glyph.withAlpha(col, 60 / k));
                }
            }
            // Halo, then the hexagon and its face.
            for (int k = 2; k >= 1; k--) {
                p.fillCircle(x, y, rr * (1.15f + 0.28f * k), Glyph.withAlpha(col, 40 / k));
            }
            p.fillPoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, (int) (96 * sporeMorph)));
            p.strokePoly(Glyph.hex(x, y, rr), Glyph.withAlpha(col, (int) (255 * sporeMorph)),
                    rr * 0.13f);
            Kawaii.draw(p, g, x, y, rr * 0.58f,
                    Glyph.withAlpha(col, (int) (255 * sporeMorph)), sporeMorph, 0.1f);
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
