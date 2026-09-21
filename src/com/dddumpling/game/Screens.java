package com.dddumpling.game;

/**
 * Full-screen states: title, game over, the between-stages minigame and settings.
 */
final class Screens extends Draw {

    private Screens() {}

    // ---- screens ------------------------------------------------------------

    /** What a scrim is made of: near-black, faintly violet, to sit under the ink. */
    static final int SCRIM = 0xFF120E22;

    /** Dims everything above the key deck, so the real keys stay lit as the tutorial. */
    static float scrim(Painter p, Layout L, int a) {
        return scrim(p, L, a, SCRIM);
    }

    /**
     * The same, in another colour.
     *
     * A scrim covers the sky down to the deck, so whatever the sky is painted is <em>its</em>
     * business, not the field's. The game-over screen learned that the hard way: the world drains
     * green as a run ends, and the ordinary violet scrim went over the top of it and left the green
     * showing only on the key deck below, which read as the deck being tinted rather than the
     * world dying.
     */
    static float scrim(Painter p, Layout L, int a, int color) {
        float bottom = L.deckTop;
        p.fillRect(0, 0, L.w, bottom, Glyph.withAlpha(color, a));
        return bottom;
    }

    static void title(Painter p, GameCore c, Layout L) {
        // Dissolves once a start key is pressed, revealing the field it was sitting over. Every
        // element takes the same factor, so the screen leaves as one thing rather than in parts.
        // Gated on starting() rather than on the timer: the send-off holds the title state open
        // after the fade is spent, and reading the timer alone snapped the screen back to full.
        float fade = c.starting() ? c.startFade / GameCore.START_FADE : 1f;
        scrim(p, L, (int) (210 * fade), Lands.background(c));
        float s = L.unit;
        float cx = L.w / 2f;
        bubblyTitle(p, c, L, cx, L.h * 0.100f, fade);
        HighScoreScreen.titleGlow(p,c,L,fade*caseOut(c));
        p.text("BEST " + c.best, cx, L.h * 0.292f, type(s * 0.74f)*HighScoreScreen.titleTextScale(c),
                fadeBy(HighScoreScreen.titleTextColor(c), fade * caseOut(c)),
                Painter.CENTER, true);

        // Where the two lines explaining the game used to be: the game, played. A word falls and
        // types itself while the matching keys light under it. Suppressed with the case open —
        // there is one lesson on screen at a time.
        Demo.draw(p, c, L, fade * caseOut(c));

        // The badge and the case swap in the same place, and in series rather than on top of
        // each other: crossing them over on the raw fade drew both at half strength for a
        // moment, and two labelled panels through each other is illegible, not a dissolve.
        float shut = fade * caseOut(c);
        float open = fade * caseIn(c);
        Showcase.icon(p, c, L, shut);
        Showcase.draw(p, c, L, open);
        PrivacyUi.draw(p, c, L);
        LandPicker.draw(p, c, L);

        // Anchored above the danger line rather than off the deck: the dashed line shows
        // faintly through the scrim, and text sitting on it looks struck through. The lines swap
        // with the case, because with it open every key only puts it away again. Nothing here
        // points at the badge — it carries its own TAP TO OPEN, and saying it twice on one screen
        // made the case look like the thing to do rather than something off to the side.
        // Nothing says "press a key to start" any more. The demo says it, by pressing one: a key
        // lights, a bullet leaves it, and a letter goes — which points at the thing you have to
        // touch and shows what touching it does. Six keys glowing at once said the same thing
        // louder and read as an alarm. See Renderer.keys and Demo.
        // The case explains itself: arrows either side of the shelf, and the focused entry throbs
        // when it first comes up if there is a story behind it. See Showcase.

    }

    /** The title logo: candy-coloured letters with a soft cream rim and individual bounce. */
    private static final float[] TITLE_X = {-0.85f, 0.34f, -0.42f, 0.92f, -0.58f,
            0.68f, -0.95f, 0.52f, -0.44f, 0.88f};
    private static final float[] TITLE_Y = {-1.05f, 0.72f, -0.38f, 1.02f, -0.82f,
            0.88f, -0.70f, 1.05f, -0.92f, 0.48f};
    private static final float[] TITLE_SCALE = {1.04f, 0.95f, 1.01f, 0.93f, 1.06f,
            0.97f, 1.05f, 0.92f, 1.02f, 0.96f};

    /** Resting centre of one title body. Shared with the spring simulation. */
    static float titleAnchorX(int letter, Layout L) {
        int column = letter % 5;
        return L.w / 2f + (column - 2) * L.w * 0.176f + TITLE_X[letter] * L.unit;
    }

    static float titleAnchorY(int letter, Layout L) {
        return L.h * (letter < 5 ? 0.140f : 0.235f) + TITLE_Y[letter] * L.unit;
    }

    private static void bubblyTitle(Painter p, GameCore c, Layout L, float cx, float baseline,
            float fade) {
        float size = type(L.unit * 5.7f);
        bubbleTitleRow(p, c, L, "DDDUM", cx, L.h * 0.140f, size, fade, 0);
        bubbleTitleRow(p, c, L, "PLING", cx, L.h * 0.235f, size, fade, 5);
    }

    private static void bubbleTitleRow(Painter p, GameCore c, Layout L, String text, float cx,
            float baseline, float size, float fade, int colorOffset) {
        for (int i = 0; i < text.length(); i++) {
            int letterIndex = colorOffset + i;
            float x = titleAnchorX(letterIndex, L) + c.titleSpringX[letterIndex];
            float y = titleAnchorY(letterIndex, L) + c.titleSpringY[letterIndex];
            float liveSize = size * TITLE_SCALE[letterIndex];
            int goo = Glyph.COLOR[letterIndex % Glyph.COUNT];
            bubbleGlyph(p, text.charAt(i), x, y, liveSize, goo, fade,
                    c.clock + letterIndex * 0.37f, c.titleSpringShape(letterIndex, L));
        }
    }

    /** One translucent vector-font glyph with shadow, outline, body and specular layers. */
    private static void bubbleGlyph(Painter p, char ch, float cx, float baseline, float height,
            int goo, float fade, float phase, float shape) {
        TitleBubbleFont.draw(p, ch, cx, baseline, height, goo, fade, phase, shape);
    }

    /** Opacity of everything the shut case owns: gone by the time the case is half faded in. */
    static float caseOut(GameCore c) {
        return Math.max(0f, 1f - c.caseFade * 2f);
    }

    /** And of everything the open case owns, which starts from there. */
    private static float caseIn(GameCore c) {
        return Math.max(0f, c.caseFade * 2f - 1f);
    }

    /**
     * The summary. Fades up once the death hold is spent rather than replacing the field on the
     * frame the last life went — the world drains first, and this arrives on top of it.
     */
    static void gameOver(Painter p, GameCore c, Layout L) {
        float fade = c.overFade();
        if (fade <= 0.004f) return;
        // Drained, so the green the world died into holds for the whole summary instead of being
        // painted over by the violet one. It stays until the title screen takes the screen back.
        scrim(p, L, (int) (220 * fade), Glyph.mix(SCRIM, DEATH_SCRIM, c.drained()));
        float s = L.unit;
        // Yellow rather than the rose it was: rose is the colour of every warning and every hit
        // in this game, so a rose GAME OVER read as one more of them.
        p.text("GAME OVER", L.w / 2f, L.h * 0.24f, type(s * 1.85f), fadeBy(YELLOW, fade),
                Painter.CENTER, true);

        p.text("SCORE", L.w / 2f, L.h * 0.325f, type(s * 0.6f), fadeBy(INK_DIM, fade),
                Painter.CENTER, false);
        p.text(String.valueOf(c.score), L.w / 2f, L.h * 0.325f + type(s * 1.8f), type(s * 1.8f),
                fadeBy(INK, fade), Painter.CENTER, true);

        accuracy(p, c, L, L.h * 0.475f, fade);
        if (c.townRunTickets > 0)
            p.text("+"+c.townRunTickets+" TOWN TICKETS",L.w*.5f,L.h*.758f,
                    type(s*.58f),fadeBy(GOLD,fade),Painter.CENTER,true);

        p.text("STAGE " + c.stage + "   SQUISHES " + c.squishes, L.w / 2f, L.h * 0.615f, type(s * 0.6f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
        p.text("BEST COMBO " + c.maxCombo, L.w / 2f, L.h * 0.615f + type(s * 0.85f), type(s * 0.6f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
        p.text(c.score >= c.best ? "NEW BEST!" : "BEST " + c.best, L.w / 2f, L.h * 0.695f,
                type(s * 0.78f), fadeBy(c.score >= c.best ? GOLD : INK_DIM, fade), Painter.CENTER, true);

    }

    /**
     * Accuracy readout: the percentage, and a dumpling whose face carries it — miserable
     * at 60% or below, delighted at 90% or above. It idles gently when sad and bounces
     * when pleased, so the mood reads before the number does.
     */
    static void accuracy(Painter p, GameCore c, Layout L, float cy) {
        accuracy(p, c, L, cy, 1f);
    }

    /** @param fade 0..1, for the summary screen fading up after a death */
    static void accuracy(Painter p, GameCore c, Layout L, float cy, float fade) {
        float s = L.unit;
        float mood = c.accuracyMood();
        int pct = c.accuracyPercent();
        int tint = mood >= 0.999f ? GOLD : Glyph.mix(Glyph.COLOR[0], ROSE, (1f - mood) * 0.55f);

        // Happier moods bounce faster and higher; a sad dumpling just sways.
        float r = s * 1.35f;
        float bob = (float) Math.abs(Math.sin(c.clock * (1.7f + 2.8f * mood)))
                * r * (0.05f + 0.20f * mood);
        float squash = 1f + 0.06f * (float) Math.sin(c.clock * (2.2f + 4f * mood));
        float dx = (1f - mood) * r * 0.12f * (float) Math.sin(c.clock * 1.3f);

        Kawaii.moodDumpling(p, L.w / 2f - s * 3.2f + dx, cy - bob, r, fadeBy(tint, fade), mood,
                squash);

        p.text("ACCURACY", L.w / 2f + s * 1.5f, cy - type(s * 0.75f), type(s * 0.58f),
                fadeBy(INK_DIM, fade), Painter.LEFT, true);
        p.text(pct + "%", L.w / 2f + s * 1.5f, cy + type(s * 0.95f), type(s * 1.55f), fadeBy(tint, fade),
                Painter.LEFT, true);
        p.text(c.hits + " HIT   " + c.misses + " MISS", L.w / 2f + s * 1.5f, cy + type(s * 1.75f),
                type(s * 0.5f), fadeBy(INK_DIM, fade), Painter.LEFT, false);
    }


    /**
     * End of the interlude: a beat to read where the run stands before it fades out and the
     * next stage starts, so the transition is not straight from mashing back into play.
     */
    static void bonusReport(Painter p, GameCore c, Layout L, float fade) {
        float s = L.unit;
        float cx = L.w / 2f;
        // Slides up a touch as it settles.
        float rise = Math.min(1f, (GameCore.BONUS_STATUS - c.bonusTimer) / 0.35f);
        float top = L.h * 0.30f + (1f - rise) * s * 1.2f;

        p.text("STAGE " + c.stage + " CLEAR", cx, top, type(s * 1.25f * introScale(
                GameCore.BONUS_STATUS - c.bonusTimer)), fadeBy(GOLD, fade), Painter.CENTER, true);

        float row = top + s * 2.4f;
        report(p, L, "SCORE", String.valueOf(c.score), row, fade);
        report(p, L, "ACCURACY", c.accuracyPercent() + "%", row + s * 1.5f, fade);
        report(p, L, "LIVES", c.lives + " / " + GameCore.START_LIVES, row + s * 3.0f, fade);
        report(p, L, "STEAMER", c.steamer.hits + " / " + c.steamer.goal(),
                row + s * 4.5f, fade);
        if (c.steamer.opens > 0) {
            report(p, L, "DUMPLINGS FREED", String.valueOf(c.steamer.opens),
                    row + s * 6.0f, fade);
        }
        report(p, L, "COLLECTION", Collect.owned(c.collected) + " / " + Collect.COUNT,
                row + s * 7.5f, fade);

        // Whatever the steamer gave up this round waves off from the foot of the report;
        // failing that, the rainbow dumpling does, as it always has.
        float dr = s * 1.3f;
        float bob = (float) Math.abs(Math.sin(c.clock * 4f)) * dr * 0.18f;
        float dy = row + s * 9.2f - bob;
        if (c.prize >= 0) {
            Trinket.draw(p, c.prize, cx, dy, dr, c.clock, true, fade);
        } else {
            Kawaii.moodDumpling(p, cx, dy, dr, fadeBy(Glyph.cycle(c.clock * 0.5f), fade), 1f,
                    1f + 0.06f * (float) Math.sin(c.clock * 5f));
        }
    }

    /** One label/value line of the end-of-interlude report. */
    private static void report(Painter p, Layout L, String label, String value, float y,
            float fade) {
        float s = L.unit;
        p.text(label, L.w * 0.5f - s * 0.4f, y, type(s * 0.62f), fadeBy(INK_DIM, fade),
                Painter.RIGHT, false);
        p.text(value, L.w * 0.5f + s * 0.4f, y, type(s * 0.72f), fadeBy(INK, fade),
                Painter.LEFT, true);
    }

    /**
     * The two keys to alternate, shown side by side with the one that is wanted next lit and
     * pulsing. The arrow between them carries the "then" so no wording is needed.
     */
    private static void alternator(Painter p, GameCore c, Layout L, float cx, float cy,
            float fade) {
        float s = L.unit;
        float r = s * 1.15f;
        // Centres 1.7r either side: enough clear space between them for the arrow.
        float gap = r * 3.4f;
        boolean rolling = c.bonusRolling();
        int[] keys = {c.bonusLeftKey(), c.bonusRightKey()};
        // Both slots are lit while spinning: neither is wanted yet, and dimming one would
        // imply an order the round has not settled on.
        boolean[] next = rolling ? new boolean[] {true, true}
                : new boolean[] {c.steamer.expectLeft, !c.steamer.expectLeft};

        for (int i = 0; i < 2; i++) {
            float x = cx + (i == 0 ? -gap : gap) / 2f;
            int col = Glyph.COLOR[keys[i]];
            float pulse = next[i] ? 1f + 0.10f * (float) Math.sin(c.clock * 8f) : 1f;
            float rr = r * pulse;

            if (next[i]) {
                // The one it wants: a bright ring so the eye lands on it without reading, and the
                // field's own caret above it, which is the mark the player already knows.
                p.strokePoly(Glyph.hex(x, cy, rr * 1.22f),
                        fadeBy(Glyph.withAlpha(INK, 200), fade), rr * 0.09f);
                caret(p, x, cy, rr, fadeBy(Glyph.withAlpha(INK, 225), fade));
            }
            p.fillPoly(Glyph.hex(x, cy, rr),
                    fadeBy(Glyph.withAlpha(col, next[i] ? 95 : 34), fade));
            p.strokePoly(Glyph.hex(x, cy, rr),
                    fadeBy(Glyph.withAlpha(col, next[i] ? 250 : 120), fade), rr * 0.085f);
            Kawaii.draw(p, keys[i], x, cy, rr * 0.58f,
                    fadeBy(Glyph.withAlpha(col, next[i] ? 255 : 130), fade), 1f,
                    next[i] ? 0.9f : 0.2f);
        }

        // Arrow between them, leaning whichever way the sequence is going. Suppressed while
        // spinning: there is no order to point out yet.
        if (!rolling) {
            float dir = c.steamer.expectLeft ? -1f : 1f;
            int arrow = fadeBy(Glyph.withAlpha(INK_DIM, 200), fade);
            float ax = cx + dir * s * 0.18f;
            p.fillPoly(new float[] {ax - dir * s * 0.36f, cy - s * 0.26f,
                    ax + dir * s * 0.36f, cy, ax - dir * s * 0.36f, cy + s * 0.26f}, arrow);
        }

        p.text(rolling ? "PICKING YOUR PAIR" : "+1 PER PAIR", cx, cy + r * 1.9f, type(s * 0.5f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
    }

    /** How long the interlude heading takes to swell into place. */
    static final float INTRO_TIME = 0.55f;

    /**
     * Size multiplier for the interlude heading: starts large, overshoots small and settles
     * at 1. Scaling down into place reads as the text arriving from the front, which suits a
     * scene that is fading up underneath it.
     */
    static float introScale(float time) {
        if (time >= INTRO_TIME) return 1f;
        float t = time / INTRO_TIME;
        // Ease-out-back: carries the progress slightly past 1 before returning, which on a
        // shrinking size means the text dips just under its final size and springs back.
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        float back = 1f + c3 * u * u * u + c1 * u * u;
        return 1.9f - 0.9f * back;
    }

    /**
     * Between-stages minigame: mash any key to lever the lid off a dim sum steamer and free
     * the rainbow dumpling inside. Progress carries across interludes, so the lid creeps up
     * over several stages.
     */
    /** Vertical centre of the visible lid, including a live drag. */
    static float steamerLidY(GameCore c, Layout L) {
        float bw = Math.min(L.w * 0.26f, L.unit * 5.8f), bh = L.unit * 3.4f;
        float rimY = L.h * 0.46f + bh * 0.22f;
        float rimRy = bw * 0.30f;
        float lift = c.steamer.lidOpen() * bh * 0.72f + c.steamer.lidPulse * bh * 0.22f;
        return rimY - rimRy * 1.05f - lift - c.steamer.lidDrag;
    }

    /**
     * The line the armed lid must cross to release. Kept relative to its undragged position so the
     * gesture is equally short on every screen and while the last press pulse is settling.
     */
    static float steamerReleaseY(GameCore c, Layout L) {
        return steamerLidY(c, L) + c.steamer.lidDrag - L.unit * 1.65f;
    }

    static float freedLidY(GameCore c, Layout L) {
        float bw = Math.min(L.w * 0.26f, L.unit * 5.8f), bh = L.unit * 3.4f;
        float rimY = L.h * 0.46f + bh * 0.22f, rimRy = bw * 0.30f;
        float start = rimY - rimRy * 1.05f - bh * 0.72f - c.steamer.freedLidLift;
        float t = 1f - c.steamer.freedT / Steamer.FREE_TIME;
        return start - t * t * (start + rimRy * 2.2f);
    }

    /** Generous touch target around the visible lid while its swipe is armed. */
    static boolean inSteamerLid(GameCore c, Layout L, float x, float y) {
        if (!c.bonusSwipeReady()) return false;
        float s = L.unit, cx = L.w / 2f;
        float bw = Math.min(L.w * 0.26f, s * 5.8f), bh = s * 3.4f;
        cx += (float) Math.sin(c.clock * 52f) * bw * 0.075f * c.steamer.badPulse;
        float rimRy = bw * 0.30f;
        float lidY = steamerLidY(c, L);
        return Math.abs(x - cx) <= bw * 1.55f
                && Math.abs(y - lidY) <= Math.max(s * 1.8f, rimRy * 1.9f);
    }

    static void bonus(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        if (c.bossReward) {
            scrim(p, L, 190);
            BossCollect.celebration(p, c, L);
            return;
        }

        // The parade closes out a winning interlude and owns the screen for it, whichever game
        // won it. Handled before the fade below, which reads the interlude's own countdown —
        // already spent by now, so it would render the whole parade invisible.
        if (c.bonusParading()) {
            float pf = Math.min(1f, c.paradeTimer / 0.35f);
            scrim(p, L, (int) (195 * pf));
            Parade.draw(p, c, L, pf);
            return;
        }

        if (c.starBonus) {
            StarScreen.draw(p, c, L);
            return;
        }
        // Eases in on arrival and back out as the timer expires, so neither edge of the
        // interlude is a hard cut between scenes. Every colour below is scaled by it.
        float fade = Math.min(1f, c.time / 0.40f) * Math.min(1f, c.bonusTimer / 0.40f);
        if (fade <= 0.01f) return;

        // Dim only the sky: the keys are the instrument here and must stay lit.
        scrim(p, L, (int) (195 * fade));

        // The tail of the interlude reports the round instead of showing the steamer.
        if (c.bonusStatus()) {
            bonusReport(p, c, L, fade);
            return;
        }

        float open = c.steamer.lidOpen();
        boolean freed = c.steamer.freedT > 0f;

        float cx = L.w / 2f;
        float cy = L.h * 0.46f;
        // Half-extents. Narrower than the flat version was: in three-quarter view the basket
        // gains height from its rim ellipse, and at the old width it dwarfed the squishy.
        float bw = Math.min(L.w * 0.26f, s * 5.8f);
        float bh = s * 3.4f;

        // Heading swells in over the fade, overshooting and settling, so the interlude
        // announces itself instead of simply appearing.
        float intro = introScale(c.time);
        p.text(freed ? "FREE!" : "FREE THE DUMPLING", cx, L.h * 0.235f,
                type(s * (freed ? 1.5f : 0.95f) * intro),
                fadeBy(freed ? GOLD : INK, fade), Painter.CENTER, true);
        if (!freed && !c.steamer.swipeReady) {
            // No label: the wanted letter wears the same caret the field puts over a head tile,
            // and the arrow between the pair already says which way the sequence is going.
            alternator(p, c, L, cx, L.h * 0.235f + s * 3.0f, fade);
        }

        // Steamer geometry, shared by the back pass, the front pass and the lid so they
        // cannot drift apart. Seen from slightly above: the rim is an ellipse, and the basket
        // tapers a little toward its base the way a real bamboo one does.
        // A wrong press jolts the whole basket sideways and turns it rose. The jolt is what
        // reads as "no" without a word of text; the colour is what reads at a glance.
        float bad = c.steamer.badPulse;
        cx += (float) Math.sin(c.clock * 52f) * bw * 0.075f * bad;
        int body = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f), c.steamer.flash * 0.45f);
        if (bad > 0f) body = Glyph.mix(body, ROSE, bad * 0.75f);
        float rimY = cy + bh * 0.22f;
        float baseY = rimY + bh * 0.78f;
        float rimRy = bw * 0.30f;
        float baseRx = bw * 0.90f;
        Basket.back(p, cx, rimY, baseY, bw, rimRy, baseRx, body, c.steamer.flash, fade);

        // The dumpling: rainbow, and cheerier the closer it is to getting out.
        float dumpR = bh * 0.86f;
        // Sits low enough to be completely hidden under a shut lid, and climbs toward the gap
        // as the lid rises — so you see more of the reward the closer you are to it. At a
        // fixed height its crown poked out over a lid that had not moved yet.
        // Freeing it resets the hit count, so `open` falls back to zero on the very frame the
        // escape starts; treat a freed one as fully open or it drops back into the basket.
        float risen = freed ? 1f : open;
        float dumpY = rimY + dumpR * 0.30f - risen * dumpR * 0.80f;
        if (freed) {
            // Escaping: rises and grows away as the celebration plays.
            float t = 1f - c.steamer.freedT / Steamer.FREE_TIME;
            dumpY -= t * t * L.h * 0.30f;
            dumpR *= 1f + 0.35f * t;
        }
        int rainbow = Glyph.cycle(c.clock * 0.5f);
        // Rays only once it is out: behind a closed lid they just show through the gap.
        if (freed) {
            int rayCol = c.prize >= 0 ? Collect.TIER_COLOR[Collect.TIER[c.prize]] : rainbow;
            for (int k = 3; k >= 1; k--) {
                p.fillPoly(star(cx, dumpY, dumpR * (1.2f + 0.42f * k), dumpR * 0.55f, 8,
                        c.clock * 0.6f), fadeBy(Glyph.withAlpha(rayCol, 30 / k), fade));
            }
        }
        if (freed && c.prize >= 0) {
            // The prize is what climbs out. Until the lid is off it stays the unidentified
            // rainbow dumpling, which is the whole conceit of a mystery box.
            Trinket.draw(p, c.prize, cx, dumpY, dumpR, c.clock, true, fade);
        } else {
            Kawaii.moodDumpling(p, cx, dumpY, dumpR, fadeBy(rainbow, fade),
                    freed ? 1f : 0.15f + 0.55f * open,
                    1f + 0.06f * (float) Math.sin(c.clock * 4f));
        }

        // Near wall, over the squishy's lower half: this is what makes it read as sitting
        // *in* the basket rather than in front of one. Drawn even during the celebration, so
        // the prize is visibly climbing out of something.
        Basket.front(p, cx, rimY, baseY, bw, rimRy, baseRx, body, c.steamer.flash, fade);

        // The caption goes after the front pass, and sits under the basket rather than
        // trailing the prize: drawn before the wall it was hidden behind it, and pinned to a
        // prize that rises off the top of the screen it would have gone with it.
        if (freed && c.prize >= 0) prizeLabel(p, c, L, cx, baseY + s * 2.05f, fade);
        if (!freed && !c.bonusPrizeWon()) countdown(p, c, L, cx, fade);
        // Rings expanding off the rim, so the rebuff carries even at a glance away from it.
        for (int k = 1; k <= 2 && bad > 0.02f; k++) {
            float rr = bw * (1f + (1f - bad) * 0.30f * k);
            p.fillPoly(pill(cx, rimY, rr, rimRy * 0.10f, 8),
                    fadeBy(Glyph.withAlpha(ROSE, (int) (150 * bad / k)), fade));
        }

        if (!freed) {
            // Lid: lifts with progress, and kicks up further on each press. Capped so that
            // at full open it just clears the rim rather than floating away from it.
            float lift = open * bh * 0.72f + c.steamer.lidPulse * bh * 0.22f;
            float lidY = rimY - rimRy * 1.05f - lift - c.steamer.lidDrag;
            int lidCol = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f + 0.3f),
                    c.steamer.flash * 0.45f);
            lidCol = Glyph.mix(lidCol, Glyph.cycle(c.clock * 11f + 0.12f),
                    c.steamer.lidFlash * 0.88f);
            if (bad > 0f) lidCol = Glyph.mix(lidCol, ROSE, bad * 0.75f);
            float lidScale = c.steamer.lidKeyScale();
            Basket.lid(p, cx, lidY, bw * 1.02f * lidScale, rimRy * 0.95f * lidScale,
                    lidCol, fade);

            if (c.bonusSwipeReady()) {
                // A broad luminous arrow bounces over the armed lid. Geometry, not text, so it
                // reads instantly and remains legible in the harness font.
                float bounce = (0.5f + 0.5f * (float) Math.sin(c.clock * 7f)) * s * 0.48f;
                float ay = lidY - s * 1.05f - bounce;
                float aw = Math.min(bw * 0.82f, s * 4.6f), ah = s * 2.0f;
                float[] arrow = {cx, ay - ah, cx + aw * 0.50f, ay - ah * 0.48f,
                        cx + aw * 0.22f, ay - ah * 0.48f, cx + aw * 0.22f, ay,
                        cx - aw * 0.22f, ay, cx - aw * 0.22f, ay - ah * 0.48f,
                        cx - aw * 0.50f, ay - ah * 0.48f};
                p.strokePoly(arrow, fadeBy(Glyph.withAlpha(GOLD, 55), fade), s * 0.48f);
                p.strokePoly(arrow, fadeBy(Glyph.withAlpha(0xFFFFFFFF, 150), fade), s * 0.20f);
                p.fillPoly(arrow, fadeBy(Glyph.withAlpha(GOLD, 220), fade));
            }

            // Steam escaping through the widening gap.
            if (open > 0.05f) {
                for (int k = 0; k < 4; k++) {
                    float wob = (float) Math.sin(c.clock * 2.2f + k * 1.7f);
                    float sx2 = cx + (k - 1.5f) * bw * 0.34f + wob * s * 0.25f;
                    float sy2 = lidY - bh * 0.4f - open * s * (0.6f + 0.5f * k);
                    p.fillPoly(pill(sx2, sy2, s * 0.34f * open, s * 0.11f * open, 6),
                            fadeBy(Glyph.withAlpha(INK, (int) (70 * open)), fade));
                }
            }

            // Progress: one pip per press needed. Below the countdown, which now owns the
            // band directly under the steamer.
            int goal = c.steamer.goal();
            int cols = Math.min(10, goal);
            float pr = s * 0.14f, gap = s * 0.54f;
            float x0 = cx - gap * (cols - 1) / 2f;
            float rowY = L.h * 0.695f;
            for (int i = 0; i < goal; i++) {
                float px = x0 + (i % cols) * gap;
                float py = rowY + (i / cols) * gap * 1.15f;
                p.fillCircle(px, py, pr,
                        fadeBy(i < c.steamer.hits ? rainbow : Glyph.withAlpha(INK, 45), fade));
            }
            int rows = (goal + cols - 1) / cols;
            p.text(c.steamer.hits + " / " + goal, cx,
                    rowY + (rows - 1) * gap * 1.15f + s * 1.5f, type(s * 0.62f),
                    fadeBy(INK_DIM, fade), Painter.CENTER, true);
        } else {
            float fly = 1f - c.steamer.freedT / Steamer.FREE_TIME;
            float lidX = cx + (float) Math.sin(fly * Math.PI) * bw * 0.70f;
            float lidY = freedLidY(c, L);
            float lidFade = Math.min(1f, c.steamer.freedT / 0.28f);
            Basket.lid(p, lidX, lidY, bw * (1.02f - 0.16f * fly),
                    rimRy * (0.95f - 0.12f * fly), BAMBOO, fade * lidFade);
            p.text("+" + GameCore.FREE_BONUS, cx, L.h * 0.665f, type(s * 1.1f), fadeBy(GOLD, fade),
                    Painter.CENTER, true);
        }

    }

    /**
     * The clock, large, in the band under the steamer. Seconds remaining while the mash runs,
     * then TIME! through the beat on zero.
     *
     * Ceiling rather than rounding, so it only reads 0 when the round is actually over — and
     * it swells on each tick, which is the part that makes the last second land.
     */
    private static void countdown(Painter p, GameCore c, Layout L, float cx, float fade) {
        float s = L.unit;
        float left = c.bonusLeft();
        boolean out = c.bonusHolding();
        int col = out || left <= 1f ? ROSE : INK;
        // The fraction runs 1 down to 0 within each second, so this is biggest just after a
        // tick and settled by the time the next one comes.
        float pop = out ? 1f : 1f + 0.16f * (left - (float) Math.floor(left));
        p.text(out ? "TIME!" : String.valueOf((int) Math.ceil(left)), cx, L.h * 0.625f,
                type(s * (out ? 1.7f : 2.6f) * pop), fadeBy(col, fade), Painter.CENTER, true);
    }

    /**
     * Name, tier and whether it is new, under the escaping prize. The tier is what tells you
     * whether to care, so it gets the colour; NEW is what tells you the case grew.
     *
     * Shared with the star course's victory tableau rather than copied into it: a prize announces
     * itself the same way whichever game handed it over, and two copies would drift.
     */
    static void prizeLabel(Painter p, GameCore c, Layout L, float cx, float y,
            float fade) {
        float s = L.unit;
        int tier = Collect.TIER[c.prize];
        int tint = Collect.TIER_COLOR[tier];
        p.text(Collect.NAME[c.prize], cx, y, type(s * 0.92f), fadeBy(INK, fade), Painter.CENTER,
                true);
        p.text(Collect.TIER_NAME[tier], cx, y + s * 0.85f, type(s * 0.58f), fadeBy(tint, fade),
                Painter.CENTER, true);
        if (c.prizeNew) {
            // Pops as it arrives, so a new entry is unmissable next to a duplicate's line.
            float pop = 1f + 0.16f * (float) Math.abs(Math.sin(c.clock * 7f));
            p.text("NEW!", cx, y + s * 2.15f, type(s * 0.92f * pop), fadeBy(GOLD, fade),
                    Painter.CENTER, true);
        } else {
            p.text("ALREADY IN THE CASE   +" + GameCore.DUPE_BONUS, cx, y + s * 2.05f,
                    type(s * 0.56f), fadeBy(INK_DIM, fade), Painter.CENTER, false);
        }
    }

    /** Settings panel: player preferences and developer controls. Freezes the game behind it. */
    /**
     * The settings panel, and the one screen whose text is <em>not</em> run through
     * {@link Draw#type}. Its rows, chips and slider are all sized from {@code unit} and packed
     * tight; scaled up, the playtest chip labels ran straight out of their boxes and off the
     * panel. It is also the one screen nobody reads at arm's length mid-play.
     */
    static void settings(Painter p, GameCore c, Layout L) {
        if (BuildFlags.DEVELOPER) PlayerSettings.draw(p,c,L);
    }

}
