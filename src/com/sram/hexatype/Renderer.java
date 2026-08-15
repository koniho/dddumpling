package com.sram.hexatype;

/**
 * Frame orchestration and the play field itself: the danger line, the words, shots,
 * particles and the key deck.
 *
 * Everything behind or over the field is in {@link Sky}, the readouts are in {@link Hud},
 * and the full-screen states are in {@link Screens}.
 */
final class Renderer extends Draw {

    private Renderer() {}

    static void draw(Painter p, GameCore c, Layout L) {
        float harm = c.harm();
        // Low health drags the whole palette toward red, and the pulse quickens with it.
        float hurtPulse = 0.5f + 0.5f * (float) Math.sin(c.clock * (2.6f + 5.5f * harm));
        float hurt = harm * (0.55f + 0.45f * hurtPulse);

        // The sky drains to a dark green as a run ends. Mixed in after the hurt red rather than
        // instead of it, so the last moments of a run go from panic red to something colder.
        float gone = c.drained();

        // Kept moderate: the red reads as a pulse at the edges, not a wash over the
        // characters, which have to stay legible at exactly the moment you are panicking.
        p.fillRect(0, 0, L.w, L.h,
                Glyph.mix(Glyph.mix(BG, BG_HURT, hurt * 0.45f), BG_DEATH, gone));
        p.fillRect(0, L.deckTop, L.w, L.h,
                Glyph.mix(Glyph.mix(BG_HI, BG_HURT, hurt * 0.35f), BG_DEATH, gone * 0.85f));

        // Two cloud layers behind the words...
        Sky.cloudBand(p, c, L, 0, Sky.CLOUD_FRONT_LAYER, hurt);

        p.save();
        if (c.shake > 0) {
            float m = c.shake * 0.016f * L.w;
            p.translate(m * (float) Math.sin(c.clock * 57f),
                    m * 0.6f * (float) Math.cos(c.clock * 71f));
        }

        dangerLine(p, c, L);
        pushHint(p, c, L);
        // Dying, the words are the swirl instead of standing where they were.
        if (c.dying()) {
            RoundEnd.swirl(p, c, L);
        } else {
            for (int i = 0; i < c.enemies.size(); i++) enemy(p, c, L, c.enemies.get(i));
        }
        pushWave(p, c, L);
        buddy(p, c, L);
        powerup(p, c, L);
        chain(p, c, L);
        shots(p, c, L);
        particles(p, c);
        flingHint(p, c, L);
        blade(p, c, L);

        // ...and the nearest one in front of them, so words pass behind it. Kept the most
        // translucent of the three: it drifts over the play area and must never hide a letter.
        Sky.cloudBand(p, c, L, Sky.CLOUD_FRONT_LAYER, GameCore.CLOUD_LAYERS, hurt);

        keys(p, c, L);
        p.restore();

        // Red closing-in glow: from low health, and from a word about to land.
        Sky.vignette(p, L, ROSE, Math.max(hurt, c.warnLevel * (0.45f + 0.55f * hurtPulse)));
        // Gold rim while the slow-motion beat runs, so the drop in speed reads as deliberate
        // rather than as the game stuttering.
        if (c.slowdown > 0f) {
            Sky.vignette(p, L, GOLD, 0.22f * (c.slowdown / GameCore.SLOW_TIME));
        }

        // The title and game-over screens carry their own numbers; a second copy is clutter.
        if (c.state == GameCore.PLAY) {
            Sky.hudBacking(p, L, Glyph.mix(BG, BG_HURT, hurt * 0.45f));
            Hud.hud(p, c, L);
            Hud.modeBar(p, c, L);
            Hud.sliceCall(p, c, L);
            Hud.chainCall(p, c, L);
            Hud.pushCall(p, c, L);
        }

        if (c.flash > 0) {
            p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(c.flashColor, (int) (c.flash * 52)));
        }

        if (c.state == GameCore.TITLE) Screens.title(p, c, L);
        else if (c.state == GameCore.OVER) Screens.gameOver(p, c, L);
        else if (c.state == GameCore.BONUS) Screens.bonus(p, c, L);
        else if (c.stageBanner > 0) Hud.stageBanner(p, c, L);

        // The run's haul: dancing on the summary once it has settled, then carrying itself to the
        // display case over the first moment of the title screen. Both over their screen rather
        // than inside it — the flight starts on one and lands on the other.
        RoundEnd.dance(p, c, L);
        RoundEnd.homeward(p, c, L);

        // The send-off, over the dissolving title screen and the field it is uncovering. Last
        // thing before play: it is the only part of the title screen that outlives the fade.
        Launch.draw(p, c, L);

        if (c.state == GameCore.PLAY && c.perfectBanner > 0) Hud.perfectStage(p, c, L);
        // Over the title screen and its display case, under nothing: the story is modal.
        if (c.storyOpen()) Storybook.draw(p, c, L);
        if (c.settingsOpen) Screens.settings(p, c, L);
    }

    static void dangerLine(Painter p, GameCore c, Layout L) {
        float alarm = c.warnLevel;
        int bands = 5;
        for (int i = 0; i < bands; i++) {
            float t0 = L.dangerY + i * 0.010f * L.h;
            int a = (int) ((16 + 54 * alarm) * (1f - (float) i / bands));
            p.fillRect(0, t0, L.w, t0 + 0.010f * L.h, Glyph.withAlpha(ROSE, a));
        }
        float pulse = 0.65f + 0.35f * (float) Math.sin(c.clock * (2.2f + 6f * alarm));
        int col = Glyph.withAlpha(ROSE, (int) ((110 + 145 * alarm) * pulse));
        float dash = 0.030f * L.w, gap = 0.022f * L.w;
        for (float x = L.playLeft; x < L.playRight; x += dash + gap) {
            float x2 = Math.min(x + dash, L.playRight);
            p.line(x, L.dangerY, x2, L.dangerY, col, Math.max(1.5f, 0.004f * L.w) * (1 + alarm));
        }
    }

    // ---- enemies ------------------------------------------------------------

    static void enemy(Painter p, GameCore c, Layout L, GameCore.Enemy e) {
        float destroy = e.destroyed
                ? Math.min(1f, e.destroyT / GameCore.DESTROY_TIME) : 0f;
        if (e.dying) {
            // The finishing shot is still in the air: flash a ring, but keep the tiles on
            // screen so there is something for the fly-apart to act on. Timed to the flight,
            // because that is what the ring is waiting for.
            float t = Math.min(1f, e.deathT / GameCore.SHOT_TIME);
            p.strokePoly(Glyph.hex(c.enemyCentreX(e), e.y, L.enemyR * (1.1f + t * 1.6f)),
                    Glyph.withAlpha(INK, (int) (200 * (1f - t))), L.enemyR * 0.16f);
        }

        float attack = e.attacking ? Math.min(1f, e.attackT / GameCore.ATTACK_TIME) : 0f;

        // Agitation as it closes on the line, becoming a full-body lunge on attack.
        float jx = 0, jy = 0;
        float agitate = Math.max(Math.max(e.warn, attack), e.failPulse * 0.85f);
        if (agitate > 0) {
            float m = agitate * L.enemyR * (0.14f + 0.30f * attack);
            jx = m * (float) Math.sin(c.clock * 47f + e.phase);
            jy = m * 0.55f * (float) Math.cos(c.clock * 61f + e.phase);
        }

        if (attack > 0) {
            // Radiating rings telegraph the strike.
            for (int k = 0; k < 3; k++) {
                float rr = L.enemyR * (1.4f + attack * (2.2f + k * 0.9f));
                p.strokePoly(Glyph.hex(c.enemyCentreX(e) + jx, e.y + jy, rr),
                        Glyph.withAlpha(ROSE, (int) (215 * (1f - attack) / (k + 1))),
                        L.enemyR * 0.09f);
            }
        }

        boolean locked = c.target == e && destroy == 0f;
        // Entrance: eases in as the word clears the top edge.
        float enter = 0.62f + 0.38f * e.enterT;
        float swell = (1f + 0.26f * attack + 0.08f * e.warn) * enter
                * (1f - 0.30f * destroy);
        int fade = (int) ((90 + 165 * e.enterT) * (1f - destroy));

        for (int i = 0; i < e.word.length; i++) {
            int g = e.word[i];
            boolean head = i == e.pos && destroy == 0f;
            boolean cleared = i < e.pos && destroy == 0f;
            float x = c.tileX(e, i, L) + jx;
            float y = e.y + jy;

            // Tiles removed out of order fly off along the direction they were sent.
            if (e.gone[i] && destroy == 0f) {
                float t = Math.min(1f, e.goneT[i]);
                if (t >= 1f) continue;
                float ease = t * t;
                x += e.goneDx[i] * ease * L.w * 0.55f;
                y += e.goneDy[i] * ease * L.h * 0.30f;
                float r = L.enemyR * Layout.TILE_SCALE * (1f - 0.5f * t);
                int col = Glyph.withAlpha(Glyph.COLOR[g], (int) (200 * (1f - t)));
                p.strokePoly(Glyph.hex(x, y, r), col, r * 0.09f);
                Kawaii.draw(p, g, x, y, r * 0.60f, col, 1f, 0.6f);
                continue;
            }

            if (destroy > 0f) {
                // Accelerating away: outer tiles split left and right, the rest take the
                // nearer edge, and they fan slightly so the row does not stay a straight line.
                float ease = destroy * destroy;
                x += e.flyDir[i] * (0.10f + 1.15f * ease) * L.w * 0.60f;
                y += (i % 2 == 0 ? -1f : 1f) * ease * L.h * 0.045f;
            }

            float wobble = c.clock * 3.1f + e.phase + i * 0.7f;
            float scale = head ? Layout.HEAD_SCALE : cleared ? Layout.TILE_SCALE * 0.84f
                    : Layout.TILE_SCALE;
            float cellR = L.enemyR * scale * swell;

            // FLURRY recolours every letter on one upward-travelling rainbow wave.
            int col = c.flurry() ? rainbowAt(y, L, c.clock) : Glyph.COLOR[g];
            // Only the tile actually struck takes the full colour strobe and pop.
            float pop = (e.hitIndex == i) ? e.hitPulse : 0f;
            if (pop > 0) {
                col = Glyph.mix(col, Glyph.cycle(c.clock * 7f + i * 0.17f), pop * 0.62f);
            }
            if (e.failPulse > 0) col = Glyph.mix(col, ROSE, e.failPulse * 0.75f);
            if (attack > 0) col = Glyph.mix(col, ROSE, attack * 0.35f);

            // Cleared letters stay put — the word only leaves once it is fully typed — but
            // recede so the remaining letters are what the eye lands on.
            int fillA = cleared ? 26 : head ? 52 : 30;
            int edgeA = cleared ? 58 : head ? 165 : 88;

            // Stacked tiles sit on a pile of offset copies, one per press still owed, so the
            // depth is legible before you even count the pips.
            int left = c.pressesLeft(e, i);
            for (int k = left - 1; k >= 1; k--) {
                float off = cellR * 0.15f * k;
                p.fillPoly(Glyph.hex(x + off, y - off, cellR),
                        Glyph.withAlpha(col, (26 - k * 4) * fade / 255));
                p.strokePoly(Glyph.hex(x + off, y - off, cellR),
                        Glyph.withAlpha(col, (95 - k * 18) * fade / 255), cellR * 0.055f);
            }

            p.fillPoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, fillA * fade / 255));
            p.strokePoly(Glyph.hex(x, y, cellR), Glyph.withAlpha(col, edgeA * fade / 255),
                    cellR * 0.075f);

            float charR = cellR * 0.60f * (1f + 0.045f * (float) Math.sin(wobble))
                    * (1f + 0.34f * pop);
            float squash = 1f + 0.16f * pop - 0.05f * (float) Math.sin(wobble);
            int charCol = cleared ? Glyph.withAlpha(Glyph.mix(col, INK_DIM, 0.42f), 180) : col;
            Kawaii.draw(p, g, x, y, charR, charCol, squash,
                    cleared ? 1f : head ? 0.4f : 0.1f);

            // Exact count of presses still owed, so a 3-stack is never mistaken for a 4.
            if (left > 1) {
                float pr = cellR * 0.085f, gap = cellR * 0.255f;
                float py = y + cellR * 0.60f;
                float px = x - gap * (left - 1) / 2f;
                for (int k = 0; k < left; k++) {
                    p.fillCircle(px + k * gap, py, pr * 1.7f, Glyph.withAlpha(0xFF000000, 90));
                    p.fillCircle(px + k * gap, py, pr, Glyph.withAlpha(INK, 240));
                }
            }

            if (head && locked) {
                // Thicker white outline plus a caret, drawn at the smoothed position so the
                // indicator slides between letters instead of teleporting.
                float ix = c.caretXFor(e, L) + jx;
                float pulse = 0.6f + 0.4f * (float) Math.sin(c.clock * 7f);
                p.strokePoly(Glyph.hex(ix, y, cellR), Glyph.withAlpha(INK, (int) (215 * pulse)),
                        cellR * 0.13f);
                caret(p, ix, y, cellR, Glyph.withAlpha(INK, 225));
            }
        }
    }

    /**
     * The push-back affordance: an upward chevron band in the strip between the danger line and
     * the key deck, shown only while the swipe is available and something is closing in.
     *
     * Drawn exactly where the finger has to start, because that strip is narrow and nothing else
     * would tell you it is a target. It disappears the moment the swipe is spent, which is also
     * how you know it is gone for the rest of the stage.
     */
    static void pushHint(Painter p, GameCore c, Layout L) {
        if (!c.pushReady()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 6.5f);
        float top = L.dangerY, bot = L.deckTop, h = bot - top;
        int a = (int) (80 + 100 * pulse);
        p.fillRect(L.playLeft, top, L.playRight, bot, Glyph.withAlpha(GOLD, a / 5));

        // Chevrons marching up with the pulse. Three a side and larger than they were, spread
        // across the middle the SWIPE UP label used to own: with the words gone these are the whole
        // affordance, and two small ones per corner were not enough to be one.
        float rise = h * (0.26f + 0.30f * pulse);
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 3; k++) {
                float cx = L.w / 2f + side * (L.playRight - L.playLeft) * (0.09f + 0.13f * k);
                float y = bot - rise;
                p.polyline(new float[] {cx - h * 0.38f, y + h * 0.32f, cx, y,
                        cx + h * 0.38f, y + h * 0.32f}, Glyph.withAlpha(GOLD, a), h * 0.13f);
            }
        }

    }

    /** The push-back landing: bands sweeping up off the line, fading as they climb. */
    static void pushWave(Painter p, GameCore c, Layout L) {
        if (c.pushT <= 0f) return;
        float t = 1f - c.pushT / GameCore.PUSH_TIME;
        float half = (L.playRight - L.playLeft) / 2f;
        for (int k = 0; k < 3; k++) {
            // Staggered, so it reads as a wave rather than one thick bar.
            float own = t - k * 0.13f;
            if (own <= 0f) continue;
            float y = L.dangerY - own * (L.dangerY - L.playTop) * 1.05f;
            int a = (int) (200 * (1f - own) * (1f - own));
            p.fillPoly(pill(L.w / 2f, y, half * (0.72f + 0.28f * own), L.unit * 0.15f, 8),
                    Glyph.withAlpha(GOLD, a));
        }
    }

    /**
     * The TEAM SQUISH squishy: one of the collection in a glowing bubble, drawn with exactly the
     * graphic the display case and the interlude use, so it is recognisably the one you won.
     *
     * The bubble brightens and the whole thing grows with every word taken, which is the only
     * running score the mode shows.
     */
    static void buddy(Painter p, GameCore c, Layout L) {
        if (c.buddy.out()) return;
        Buddy b = c.buddy;
        float r = b.radius(L);
        float glow = b.glow();
        int tint = Collect.BODY[b.who];

        // A charge leaves a streak behind it, so a fast one is legible as a direction.
        if (b.chase != null) {
            float sp = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
            if (sp > 1f) {
                float back = r * 2.4f;
                p.line(b.x - b.vx / sp * back, b.y - b.vy / sp * back, b.x, b.y,
                        Glyph.withAlpha(tint, 90), r * 0.5f);
            }
        }

        // The bubble: layered halos, brightest at the skin, plus a rotating ring of spikes that
        // spins up as it grows. Mixed toward white rather than left as the body colour, or it
        // reads as a purple object against the frenzy sky instead of as something lit.
        int lit = Glyph.mix(tint, 0xFFFFFFFF, 0.30f + 0.45f * glow);
        int spikes = 6 + b.squishes / 2;
        p.fillPoly(star(b.x, b.y, r * (1.34f + 0.26f * glow), r * 0.94f,
                Math.min(14, spikes), c.clock * (1.1f + 2.2f * glow)),
                Glyph.withAlpha(lit, (int) (55 + 110 * glow)));
        for (int k = 4; k >= 1; k--) {
            p.fillCircle(b.x, b.y, r * (1f + 0.17f * k),
                    Glyph.withAlpha(lit, (int) ((26 + 52 * glow) / k)));
        }
        p.fillCircle(b.x, b.y, r, Glyph.withAlpha(lit, (int) (60 + 90 * glow)));
        p.strokeCircle(b.x, b.y, r, Glyph.withAlpha(INK, (int) (170 + 85 * glow)), r * 0.075f);
        // A darker core behind the squishy. Without it a pale collectible sits on a pale
        // bubble and vanishes into it, whichever one of the thirty turns up.
        p.fillCircle(b.x, b.y, r * 0.82f, Glyph.withAlpha(BG, 130));
        // A highlight on the skin, so it reads as a bubble rather than as a flat disc.
        p.fillEllipse(b.x - r * 0.36f, b.y - r * 0.40f, r * 0.26f, r * 0.16f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (90 + 90 * glow)));

        Trinket.draw(p, b.who, b.x, b.y, r * 0.74f, c.clock, true, 1f);
    }

    /**
     * The drifting powerup: a single letter with a rotating rainbow halo, labelled with the
     * mode it carries so you know what you are chasing before you commit a press to it.
     */
    static void powerup(Painter p, GameCore c, Layout L) {
        Power w = c.power;
        if (w == null) return;
        float r = L.enemyR * 1.25f;
        float bob = (float) Math.sin(w.t * 3.2f) * L.enemyR * 0.22f;
        float y = w.y + bob;
        int hue = Glyph.cycle(c.clock * 0.7f);

        if (w.hit) {
            // Caught: the halo blows outward and fades.
            float t = Math.min(1f, w.hitT / Power.POP_TIME);
            for (int k = 3; k >= 1; k--) {
                p.strokePoly(star(w.x, y, r * (1f + t * (2f + k)), r * 0.45f, 8, c.clock),
                        Glyph.withAlpha(hue, (int) (200 * (1f - t) / k)), r * 0.10f);
            }
            return;
        }

        // Halo: layered stars turning slowly, brightest at the core.
        for (int k = 4; k >= 1; k--) {
            p.fillPoly(star(w.x, y, r * (1.1f + 0.42f * k), r * 0.40f, 8, c.clock * 0.55f),
                    Glyph.withAlpha(hue, 30 / k));
        }
        float pulse = 0.85f + 0.15f * (float) Math.sin(w.t * 6f);
        p.fillPoly(Glyph.hex(w.x, y, r * pulse), Glyph.withAlpha(hue, 90));
        p.strokePoly(Glyph.hex(w.x, y, r * pulse), Glyph.withAlpha(INK, 235), r * 0.10f);
        Kawaii.draw(p, w.glyph, w.x, y, r * 0.58f, hue, 1f, 0.8f);

        p.text(w.name(), w.x, y - r * 1.7f, type(L.unit * 0.56f), Glyph.withAlpha(INK, 240),
                Painter.CENTER, true);
    }

    /**
     * Instructional finger for FLING, shown until the player first touches. A hand outline
     * tracing the same arc the sparkle trail follows, so the hint demonstrates the gesture
     * rather than describing it — and the gesture is a swipe through the letters, not a grab
     * of one, which is why the trail matters more than the hand.
     */
    static void flingHint(Painter p, GameCore c, Layout L) {
        if (!c.showFlingHint()) return;
        float x = c.demoX, y = c.demoY;
        float r = L.enemyR * 0.85f;

        // Ripples spreading from the fingertip. Stroked thick: thin arcs come out looking
        // dotted, because the rasterizer draws them as round-capped segments.
        for (int k = 1; k <= 3; k++) {
            float t = ((c.clock * 0.9f) + k * 0.33f) % 1f;
            p.strokeCircle(x, y, r * (0.8f + t * 2.0f),
                    Glyph.withAlpha(INK, (int) (120 * (1f - t))), r * 0.22f);
        }

        // A hand: tapered finger angled down-right from the tip, into a rounded knuckle.
        float a = 0.68f;
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
        p.fillPoly(new float[] {
                x - dy * r * 0.46f, y + dx * r * 0.46f,
                x + dy * r * 0.46f, y - dx * r * 0.46f,
                x + dx * r * 2.0f + dy * r * 0.72f, y + dy * r * 2.0f - dx * r * 0.72f,
                x + dx * r * 2.0f - dy * r * 0.72f, y + dy * r * 2.0f + dx * r * 0.72f,
        }, Glyph.withAlpha(INK, 150));
        p.fillCircle(x + dx * r * 2.1f, y + dy * r * 2.1f, r * 0.80f,
                Glyph.withAlpha(INK, 150));
        // Fingertip, bright, sitting on the letters it is about to drag.
        p.fillCircle(x, y, r * 0.62f, Glyph.withAlpha(INK, 245));
        p.fillCircle(x, y, r * 0.30f, Glyph.withAlpha(0xFF2A2348, 210));
    }

    /**
     * The blade, while a stroke is in progress: a bright streak along the last stretch of the
     * stroke and a hot tip at the finger. The sparkle ribbon behind it does the length of the
     * trail; this is the edge, and it is what makes the swipe read as a cut rather than as a
     * finger with glitter on it.
     */
    static void blade(Painter p, GameCore c, Layout L) {
        if (!c.flinging() || !c.fingerDown) return;
        float r = L.enemyR * GameCore.BLADE;
        int hue = Glyph.cycle(c.clock * 1.6f);

        // Three passes, widest and faintest first, so the edge has a glow around it.
        for (int k = 3; k >= 1; k--) {
            p.line(c.bladeFromX, c.bladeFromY, c.fingerX, c.fingerY,
                    Glyph.withAlpha(k == 1 ? INK : hue, k == 1 ? 235 : 70 / k),
                    r * (k == 1 ? 0.20f : 0.34f * k));
        }
        p.fillCircle(c.fingerX, c.fingerY, r * 0.60f, Glyph.withAlpha(hue, 110));
        p.fillCircle(c.fingerX, c.fingerY, r * 0.26f, Glyph.withAlpha(INK, 250));
    }

    /**
     * The MULTI chain: a jagged bolt from each hop to the next, revealed in order, with a flare
     * at every point it struck.
     *
     * Jagged rather than straight, and drawn from stored positions rather than from the tiles —
     * the tiles are already gone by the time this runs, which is the whole reason the positions
     * are kept. The kinks are hashed off the hop index so they hold still instead of crawling.
     */
    static void chain(Painter p, GameCore c, Layout L) {
        if (c.chainT <= 0f || c.chainLen < 1) return;
        // Full strength while it is being revealed, then out over the tail.
        float fade = Math.min(1f, c.chainT / (GameCore.CHAIN_TIME * (1f - GameCore.CHAIN_REVEAL)));
        int col = Glyph.COLOR[c.chainGlyph];
        float w = L.enemyR * 0.16f;

        for (int i = 1; i < c.chainShown; i++) {
            float ax = c.chainX[i - 1], ay = c.chainY[i - 1];
            float bx = c.chainX[i], by = c.chainY[i];
            // Three kinks, offset perpendicular to the link by a fixed fraction of its length.
            float dx = bx - ax, dy = by - ay;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1f) continue;
            float nx = -dy / len, ny = dx / len;
            float[] bolt = new float[5 * 2];
            for (int k = 0; k < 5; k++) {
                float t = k / 4f;
                float off = (k == 0 || k == 4) ? 0f
                        : (hash(i * 71 + k) - 0.5f) * len * 0.22f;
                bolt[k * 2] = ax + dx * t + nx * off;
                bolt[k * 2 + 1] = ay + dy * t + ny * off;
            }
            p.polyline(bolt, fadeBy(Glyph.withAlpha(col, 90), fade), w * 2.2f);
            p.polyline(bolt, fadeBy(Glyph.withAlpha(INK, 235), fade), w);
        }

        for (int i = 0; i < c.chainShown; i++) {
            // Newest hop flares brightest, so the eye follows the head of the chain.
            float age = c.chainShown <= 1 ? 1f : (float) i / (c.chainShown - 1);
            float r = L.enemyR * (0.34f + 0.30f * age);
            p.fillPoly(star(c.chainX[i], c.chainY[i], r, r * 0.42f, 6, c.clock * 1.4f + i),
                    fadeBy(Glyph.withAlpha(col, 150), fade));
            p.fillCircle(c.chainX[i], c.chainY[i], r * 0.36f,
                    fadeBy(Glyph.withAlpha(INK, 240), fade));
        }
    }

    static void shots(Painter p, GameCore c, Layout L) {
        for (int i = 0; i < c.shots.size(); i++) {
            GameCore.Shot s = c.shots.get(i);
            bullet(p, c, L, s.sx, s.sy, s.tx, s.ty, s.t, s.glyph, 1f);
        }
    }

    /**
     * One bullet in flight, from the key it was fired from to the tile it is going to hit.
     *
     * Its own method because the title screen's demo fires the same bullet at the word it is
     * pretending to type, and a second shot-drawer would be the thing that eventually disagreed
     * with this one — the same reason the demo hands its word to {@link #enemy}.
     *
     * @param t    0..1 along the flight
     * @param fade multiplies every alpha, so the demo's shots leave with the title screen
     */
    static void bullet(Painter p, GameCore c, Layout L, float sx, float sy, float tx, float ty,
            float t, int glyph, float fade) {
        if (t > 1f) t = 1f;
        float x = sx + (tx - sx) * t;
        float y = sy + (ty - sy) * t;
        float t0 = Math.max(0f, t - 0.30f);
        float x0 = sx + (tx - sx) * t0;
        float y0 = sy + (ty - sy) * t0;
        int col = Glyph.cycle(c.clock * 8f + glyph * 0.15f);
        p.line(x0, y0, x, y, Glyph.withAlpha(col, (int) (130 * fade)), L.enemyR * 0.26f);
        p.fillCircle(x, y, L.enemyR * 0.46f, Glyph.withAlpha(col, (int) (80 * fade)));
        p.fillCircle(x, y, L.enemyR * 0.21f, Glyph.withAlpha(INK, (int) (245 * fade)));
    }

    static void particles(Painter p, GameCore c) {
        for (int i = 0; i < c.particles.size(); i++) {
            GameCore.Particle q = c.particles.get(i);
            float k = q.life / q.max;
            p.fillCircle(q.x, q.y, q.size * (0.4f + k * 0.6f),
                    Glyph.withAlpha(q.color, (int) (235 * k)));
        }
    }

    // ---- key deck -----------------------------------------------------------

    static void keys(Painter p, GameCore c, Layout L) {
        int hint = c.hintGlyph();
        // The deck mourns as one: every key goes red and pulls a sad face over the death hold.
        // Every key wears the same face on purpose — the mood dumpling is already this game's
        // "how are we doing", and six of them at once reads as the deck giving up rather than as
        // six separate characters each having a bad moment.
        float gone = Math.min(1f, c.drained() * 1.6f);

        // The title screen's demo presses the deck for you. Nothing else lights it outside play:
        // six keys glowing and sweeping to say "press anything" read as an alarm rather than an
        // invitation, and the demo already says it by pressing one key at a time. A key answers a
        // press and is otherwise plain, on every screen.
        //
        // Faded on the same crossfade the badge uses, so opening the case puts the demo away with
        // it: while the case is up a key only closes it again.
        float demoLit = 0f;
        if (c.state == GameCore.TITLE) {
            demoLit = Screens.caseOut(c)
                    * (c.starting() ? c.startFade / GameCore.START_FADE : 1f);
        }

        for (int g = 0; g < Glyph.COUNT; g++) {
            float press = c.keyPress[g], bad = c.keyBad[g];
            // The demo's own press, so the deck answers the falling word. Folded into the press
            // itself rather than drawn as a glow beside it: it is the same event, so it should
            // get everything a press gets — the colour strobe, the outward ripple, the pop on
            // the face. Before the radius, because a press squashes the hex a little.
            if (demoLit > 0.004f && Demo.litKey(c) == g) {
                press = Math.max(press, Demo.litAmount(c) * demoLit);
            }
            float r = L.keyR * (1f - 0.05f * press);
            float cx = L.keyX[g], cy = L.keyY[g];

            // Activated keys strobe through the palette rather than merely brightening.
            int col = c.flurry() ? rainbowAt(cy, L, c.clock) : Glyph.COLOR[g];
            if (press > 0.02f) {
                col = Glyph.mix(col, Glyph.cycle(c.clock * 9f + g * 0.13f), press * 0.9f);
            }
            if (bad > 0) col = Glyph.mix(col, ROSE, bad);
            if (gone > 0f) col = Glyph.mix(col, RED, gone);

            // During the interlude only two keys matter; ring them, and mark the next one.
            // The spinner rings too, off the same pair the alternator is showing, so the deck
            // visibly runs through the candidates with it. Nothing is wanted until it lands.
            if ((c.bonusMashing() || c.bonusRolling())
                    && (g == c.bonusLeftKey() || g == c.bonusRightKey())) {
                boolean wanted = !c.bonusRolling() && g == c.steamer.wanted();
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 8f);
                p.strokePoly(Glyph.hex(cx, cy, r * 1.16f),
                        Glyph.withAlpha(wanted ? INK : col,
                                wanted ? (int) (110 + 145 * pulse) : 90), r * 0.075f);
            }

            if (hint == g) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 6f);
                p.strokePoly(Glyph.hex(cx, cy, r * 1.12f),
                        Glyph.withAlpha(col, (int) (60 + 145 * pulse)), r * 0.06f);
            }
            if (press > 0.02f) {
                // Ripple outward as the press decays.
                p.strokePoly(Glyph.hex(cx, cy, r * (1.05f + 0.42f * (1f - press))),
                        Glyph.withAlpha(col, (int) (210 * press)), r * 0.07f);
            }

            p.fillPoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, (int) (36 + 150 * press)));
            p.strokePoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, (int) (190 + 65 * press)),
                    r * 0.085f);

            if (gone > 0.02f) {
                // Sagging a little as it goes, so the deck slumps rather than simply recolouring.
                float sag = r * 0.06f * gone;
                Kawaii.moodDumpling(p, cx, cy + sag, r * 0.60f, col, 0f, 1f + 0.10f * gone);
            } else {
                Kawaii.draw(p, g, cx, cy, r * 0.60f * (1f + 0.12f * press), col,
                        1f + 0.20f * press, 0.25f + 0.6f * press);
            }
        }
    }
}
