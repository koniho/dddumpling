package com.sram.hexatype;

/**
 * Full-screen states: title, game over, the between-stages minigame and settings.
 */
final class Screens extends Draw {

    private Screens() {}

    // ---- screens ------------------------------------------------------------

    /** Dims everything above the key deck, so the real keys stay lit as the tutorial. */
    static float scrim(Painter p, Layout L, int a) {
        float bottom = L.deckTop;
        p.fillRect(0, 0, L.w, bottom, Glyph.withAlpha(0xFF120E22, a));
        return bottom;
    }

    static void title(Painter p, GameCore c, Layout L) {
        // Dissolves once a start key is pressed, revealing the field it was sitting over. Every
        // element takes the same factor, so the screen leaves as one thing rather than in parts.
        // Gated on starting() rather than on the timer: the send-off holds the title state open
        // after the fade is spent, and reading the timer alone snapped the screen back to full.
        float fade = c.starting() ? c.startFade / GameCore.START_FADE : 1f;
        scrim(p, L, (int) (210 * fade));
        float s = L.unit;
        float cx = L.w / 2f;
        p.text("DDDUMPLING", cx, L.h * 0.100f, type(s * 1.95f), fadeBy(INK, fade),
                Painter.CENTER, true);
        if (c.best > 0) {
            p.text("BEST " + c.best, cx, L.h * 0.170f, type(s * 0.74f), fadeBy(ROSE, fade),
                    Painter.CENTER, true);
        }

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
        scrim(p, L, (int) (220 * fade));
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

        p.text("STAGE " + c.stage + "   SQUISHES " + c.squishes, L.w / 2f, L.h * 0.615f, type(s * 0.6f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
        p.text("BEST COMBO " + c.maxCombo, L.w / 2f, L.h * 0.615f + type(s * 0.85f), type(s * 0.6f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
        p.text(c.score >= c.best ? "NEW BEST!" : "BEST " + c.best, L.w / 2f, L.h * 0.695f,
                type(s * 0.78f), fadeBy(c.score >= c.best ? GOLD : INK_DIM, fade), Painter.CENTER, true);

        // Nothing asks for a press here either: once the summary has settled the deck picks up the
        // same glow the title screen uses. See Renderer.keys.

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
        report(p, L, "STEAMER", c.steamer.hits + " / " + GameCore.STEAMER_HITS,
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
    static void bonus(Painter p, GameCore c, Layout L) {
        float s = L.unit;

        if (c.starBonus) {
            starPath(p, c, L);
            return;
        }

        // The parade closes out a winning interlude and owns the screen for it. Handled before
        // the fade below, which reads the interlude's own countdown — already spent by now, so
        // it would render the whole parade invisible.
        if (c.bonusParading()) {
            float pf = Math.min(1f, c.paradeTimer / 0.35f);
            scrim(p, L, (int) (195 * pf));
            Parade.draw(p, c, L, pf);
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
        if (!freed) {
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
            float lidY = rimY - rimRy * 1.05f - lift;
            int lidCol = Glyph.mix(BAMBOO, Glyph.cycle(c.clock * 6f + 0.3f),
                    c.steamer.flash * 0.45f);
            if (bad > 0f) lidCol = Glyph.mix(lidCol, ROSE, bad * 0.75f);
            Basket.lid(p, cx, lidY, bw * 1.02f, rimRy * 0.95f, lidCol, fade);

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
            int cols = 10;
            float pr = s * 0.14f, gap = s * 0.54f;
            float x0 = cx - gap * (cols - 1) / 2f;
            float rowY = L.h * 0.695f;
            for (int i = 0; i < GameCore.STEAMER_HITS; i++) {
                float px = x0 + (i % cols) * gap;
                float py = rowY + (i / cols) * gap * 1.15f;
                p.fillCircle(px, py, pr,
                        fadeBy(i < c.steamer.hits ? rainbow : Glyph.withAlpha(INK, 45), fade));
            }
            p.text(c.steamer.hits + " / " + GameCore.STEAMER_HITS, cx,
                    rowY + gap * 1.15f + s * 1.5f, type(s * 0.62f), fadeBy(INK_DIM, fade), Painter.CENTER, true);
        } else {
            p.text("+" + GameCore.FREE_BONUS, cx, L.h * 0.665f, type(s * 1.1f), fadeBy(GOLD, fade),
                    Painter.CENTER, true);
        }

    }

    /** Five-second scrolling star course, plus its wordless ready demonstration and exit. */
    /** Baseline of the star screen's checkpoint counter. */
    static float starCountY(Layout L) {
        return L.playTop + L.unit * 1.2f;
    }

    /** Type size of the READY prompt, whose caps have to clear {@link #starCountY}. */
    static float starReadySize(Layout L) {
        return type(L.unit * 1.1f);
    }

    /**
     * Baseline of the READY prompt. The gap below the counter goes through {@code type()} like
     * the sizes do: as a plain unit multiple it was 1.2 units, and at TEXT 1.34 READY's caps
     * came up through the counter's baseline. See the note on the global text scale in CLAUDE.md.
     */
    static float starReadyY(Layout L) {
        return starCountY(L) + type(L.unit * 1.35f);
    }

    private static void starPath(Painter p, GameCore c, Layout L) {
        StarPath q = c.stars;
        float fade = Math.min(1f, c.time / 0.35f)
                * (q.reporting() ? Math.min(1f, q.timer / 0.35f) : 1f);
        scrim(p, L, (int) (175 * fade));
        float s = L.unit;

        // Catmull-Rom samples turn the checkpoints into one soft flight trail. Three strokes
        // supply depth: a broad shadow, a coloured atmosphere, and a bright central filament.
        float[] route = starSpline(q, L);
        // The bright route is as wide as a star's centre pearl (2 * 1.56 * 0.23 units).
        // The two broader passes sit behind it like a soft raised ribbon.
        float pearl = s * 1.56f * 0.23f * 2f;
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF241B50, 145), fade), pearl * 1.55f);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF6E72C8, 105), fade), pearl * 1.25f);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFFBDEBFF, 115), fade), pearl);
        for (int i = 0; i < StarPath.COUNT; i++) {
            float y = q.starY(i, L);
            if (y < L.playTop - s * 2f || y > L.dangerY + s * 2f) continue;
            boolean got = (q.collected & (1 << i)) != 0;
            float pulse = 1f + 0.11f * (float) Math.sin(c.clock * 5.5f + i * 1.31f);
            float turn = c.clock * (i % 2 == 0 ? 0.55f : -0.45f) + i * 0.37f;
            float outer = s * 1.56f * pulse; // exactly three times the original 0.52 unit star
            float inner = s * 0.69f * pulse;
            int col = got ? Glyph.withAlpha(INK_DIM, 62)
                    : Glyph.withAlpha(GOLD, (int) (210 + 45 * Math.sin(c.clock * 6f + i)));
            float xStar = q.starX(i, L);
            p.fillPoly(cuteStar(xStar, y, outer * 1.16f, inner * 1.12f, turn),
                    fadeBy(Glyph.withAlpha(got ? INK_DIM : 0xFFFFA93A, got ? 22 : 45), fade));
            p.fillPoly(cuteStar(xStar, y, outer, inner, turn), fadeBy(col, fade));
            // A pearl-like heart makes each checkpoint feel like a little creature rather than
            // a navigation marker. Two translucent discs give it depth without a gradient API.
            int orb = got ? Glyph.withAlpha(INK_DIM, 70) : Glyph.withAlpha(0xFFFFF6C7, 245);
            p.fillCircle(xStar, y, outer * 0.30f, fadeBy(Glyph.withAlpha(0xFFFFA93A,
                    got ? 18 : 95), fade));
            p.fillCircle(xStar, y, outer * 0.23f, fadeBy(orb, fade));
            p.fillCircle(xStar - outer * 0.065f, y - outer * 0.075f, outer * 0.065f,
                    fadeBy(Glyph.withAlpha(0xFFFFFFFF, got ? 45 : 210), fade));

            float b = q.burst[i];
            if (b > 0f) {
                float out = (1f - b) * s * 2.4f;
                int shine = fadeBy(Glyph.withAlpha(0xFFFFFFFF, (int) (255 * b)), fade);
                // A white glint crosses the star while eight sparks burst away from it.
                p.line(xStar - outer * 0.75f, y, xStar + outer * 0.75f, y, shine,
                        s * 0.12f * b);
                p.line(xStar, y - outer * 0.75f, xStar, y + outer * 0.75f, shine,
                        s * 0.12f * b);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + i * 0.19f;
                    float ux = (float) Math.cos(a), uy = (float) Math.sin(a);
                    float x0 = xStar + ux * outer * 0.65f;
                    float y0 = y + uy * outer * 0.65f;
                    p.line(x0, y0, x0 + ux * out, y0 + uy * out, shine, s * 0.09f * b);
                }
            }
        }

        float x = q.x, y = q.characterY(L);
        if (q.ready()) {
            // Wordless lesson: the three left keys glow and the flyer leans left, then the
            // three right keys and a lean right. The real deck supplies the controls.
            float wave = (float) Math.sin(c.clock * 3.4f);
            x += wave * L.w * 0.12f;
            int side = wave < 0 ? 0 : 1;
            for (int g = side * 3; g < side * 3 + 3; g++) {
                float pulse = 1f + 0.08f * (float) Math.sin(c.clock * 8f + g);
                p.strokePoly(Glyph.hex(L.keyX[g], L.keyY[g], L.keyR * 1.18f * pulse),
                        fadeBy(Glyph.withAlpha(GOLD, 210), fade), L.keyR * 0.08f);
            }
        }

        float rr = L.enemyR * 1.05f;
        p.fillCircle(x, y, rr * 1.18f, fadeBy(Glyph.withAlpha(0xFF93D6F7, 55), fade));
        p.strokePoly(Glyph.hex(x, y, rr * 1.15f), fadeBy(Glyph.withAlpha(0xFF93D6F7, 150), fade),
                rr * 0.07f);
        if (q.who >= 0 && q.who < Collect.COUNT) {
            Trinket.draw(p, q.who, x, y, rr, c.clock, true, fade);
        } else {
            p.fillCircle(x, y, rr * 0.75f, fadeBy(Glyph.withAlpha(INK_DIM, 210), fade));
        }

        String count = q.count() + " / " + StarPath.COUNT;
        p.text(count, L.w / 2f, starCountY(L), type(s * 0.78f), fadeBy(INK, fade),
                Painter.CENTER, true);
        if (q.ready()) {
            p.text("READY", L.w / 2f, starReadyY(L), starReadySize(L),
                    fadeBy(GOLD, fade), Painter.CENTER, true);
        } else if (q.reporting()) {
            p.text(q.won ? "ALL STARS!" : count + " STARS", L.w / 2f, L.h * 0.38f,
                    type(s * 1.15f), fadeBy(q.won ? GOLD : INK, fade), Painter.CENTER, true);
        }
    }

    /** Smooth samples through the scrolling checkpoints, padded at both ends for tangents. */
    private static float[] starSpline(StarPath q, Layout L) {
        final int steps = 7;
        float[] pts = new float[(StarPath.COUNT - 1) * steps * 2 + 2];
        int at = 0;
        for (int i = 0; i < StarPath.COUNT - 1; i++) {
            int a = Math.max(0, i - 1), b = i, cc = i + 1,
                    d = Math.min(StarPath.COUNT - 1, i + 2);
            for (int k = 0; k < steps; k++) {
                float t = k / (float) steps;
                pts[at++] = catmull(q.starX(a, L), q.starX(b, L), q.starX(cc, L),
                        q.starX(d, L), t);
                pts[at++] = catmull(q.starY(a, L), q.starY(b, L), q.starY(cc, L),
                        q.starY(d, L), t);
            }
        }
        pts[at++] = q.starX(StarPath.COUNT - 1, L);
        pts[at] = q.starY(StarPath.COUNT - 1, L);
        return pts;
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    /** Five broad padded points, with paired shoulder vertices instead of needle-like tips. */
    private static float[] cuteStar(float cx, float cy, float outer, float inner, float turn) {
        float[] pts = new float[40];
        int at = 0;
        for (int i = 0; i < 5; i++) {
            float tip = turn + i * 6.283185f / 5f;
            float before = tip - 3.141593f / 5f;
            // Wide valley, two shoulders across a blunt tip, then the next wide valley.
            pts[at++] = cx + inner * (float) Math.cos(before + 0.20f);
            pts[at++] = cy + inner * (float) Math.sin(before + 0.20f);
            pts[at++] = cx + outer * 0.91f * (float) Math.cos(tip - 0.105f);
            pts[at++] = cy + outer * 0.91f * (float) Math.sin(tip - 0.105f);
            pts[at++] = cx + outer * 0.91f * (float) Math.cos(tip + 0.105f);
            pts[at++] = cy + outer * 0.91f * (float) Math.sin(tip + 0.105f);
            pts[at++] = cx + inner * (float) Math.cos(tip + 3.141593f / 5f - 0.20f);
            pts[at++] = cy + inner * (float) Math.sin(tip + 3.141593f / 5f - 0.20f);
        }
        return pts;
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
     */
    private static void prizeLabel(Painter p, GameCore c, Layout L, float cx, float y,
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

    /** Settings panel: pacing multiplier and music choice. Freezes the game behind it. */
    /**
     * The settings panel, and the one screen whose text is <em>not</em> run through
     * {@link Draw#type}. Its rows, chips and slider are all sized from {@code unit} and packed
     * tight; scaled up, the playtest chip labels ran straight out of their boxes and off the
     * panel. It is also the one screen nobody reads at arm's length mid-play.
     */
    static void settings(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);

        p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(0xFF0D0A18, 205));
        p.fillRect(ui.panelL, ui.panelT, ui.panelR, ui.panelB,
                Glyph.withAlpha(0xFF2A2348, 250));
        p.strokePoly(new float[] {ui.panelL, ui.panelT, ui.panelR, ui.panelT, ui.panelR,
                ui.panelB, ui.panelL, ui.panelB}, Glyph.withAlpha(INK, 60), s * 0.06f);

        p.text("SETTINGS", ui.panelL + s * 1.2f, ui.titleY, s * 0.92f, INK, Painter.LEFT, true);
        p.text("PAUSED", ui.panelL + s * 1.2f, ui.titleY + s * 0.8f, s * 0.5f, INK_DIM,
                Painter.LEFT, false);

        // Close button.
        p.fillCircle(ui.closeCx, ui.closeCy, ui.closeR, Glyph.withAlpha(ROSE, 60));
        p.strokeCircle(ui.closeCx, ui.closeCy, ui.closeR, Glyph.withAlpha(ROSE, 220),
                s * 0.06f);
        float k = ui.closeR * 0.42f;
        p.line(ui.closeCx - k, ui.closeCy - k, ui.closeCx + k, ui.closeCy + k, INK, s * 0.09f);
        p.line(ui.closeCx + k, ui.closeCy - k, ui.closeCx - k, ui.closeCy + k, INK, s * 0.09f);

        // Speed slider.
        p.text("SPEED", ui.sliderL, ui.speedLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        p.fillRect(ui.sliderL, ui.sliderY - ui.sliderH / 2f, ui.sliderR,
                ui.sliderY + ui.sliderH / 2f, Glyph.withAlpha(INK, 40));
        float kx = ui.knobX(c.speed);
        p.fillRect(ui.sliderL, ui.sliderY - ui.sliderH / 2f, kx,
                ui.sliderY + ui.sliderH / 2f, Glyph.withAlpha(Glyph.COLOR[4], 210));
        p.fillPoly(Glyph.hex(kx, ui.sliderY, s * 0.62f), Glyph.withAlpha(Glyph.COLOR[4], 255));
        p.strokePoly(Glyph.hex(kx, ui.sliderY, s * 0.62f), Glyph.withAlpha(INK, 200),
                s * 0.055f);

        p.text("0.5X", ui.sliderL, ui.speedValueY, s * 0.48f, INK_DIM, Painter.LEFT, false);
        p.text("1.5X", ui.sliderR, ui.speedValueY, s * 0.48f, INK_DIM, Painter.RIGHT, false);
        p.text(fmtSpeed(c.speed) + "X", (ui.sliderL + ui.sliderR) / 2f, ui.speedValueY,
                s * 0.72f, INK, Painter.CENTER, true);

        // Music options.
        p.text("MUSIC", ui.sliderL, ui.bgmLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        for (int i = 0; i < Music.NAMES.length; i++) {
            float cy = ui.optionCy(i);
            boolean on = i == c.bgmChoice;
            int col = on ? Glyph.COLOR[i % Glyph.COUNT] : INK_DIM;
            if (on) {
                p.fillRect(ui.optionL(), cy - ui.optionH * 0.40f, ui.optionR(),
                        cy + ui.optionH * 0.40f, Glyph.withAlpha(col, 40));
            }
            float bx = ui.optionL() + s * 0.75f;
            p.fillPoly(Glyph.hex(bx, cy, s * 0.34f), Glyph.withAlpha(col, on ? 235 : 45));
            p.strokePoly(Glyph.hex(bx, cy, s * 0.34f), Glyph.withAlpha(col, 190), s * 0.045f);
            p.text(Music.NAMES[i], bx + s * 0.9f, cy + s * 0.22f, s * 0.6f,
                    on ? INK : INK_DIM, Painter.LEFT, on);
        }

        // Playtest: drop straight into a mode instead of waiting for a letter to drift past.
        p.text("PLAYTEST", ui.sliderL, ui.testLabelY, s * 0.58f, INK_DIM, Painter.LEFT, true);
        for (int i = 0; i < Power.COUNT; i++) {
            float l = ui.testChipL(i, Power.COUNT), r = ui.testChipR(i, Power.COUNT);
            int col = Glyph.cycle(i / (float) Power.COUNT);
            p.fillRect(l, ui.testY, r, ui.testY + ui.testH, Glyph.withAlpha(col, 46));
            p.strokePoly(new float[] {l, ui.testY, r, ui.testY, r, ui.testY + ui.testH,
                    l, ui.testY + ui.testH}, Glyph.withAlpha(col, 190), s * 0.05f);
            p.text(Power.NAMES[i], (l + r) / 2f, ui.testY + ui.testH * 0.66f, s * 0.56f,
                    INK, Painter.CENTER, true);
        }

        // Empty the display case. Armed by the first tap and only acted on by the second, so
        // the label itself is the confirmation prompt — there is no dialog in this game.
        p.text("DISPLAY CASE", ui.sliderL, ui.clearLabelY, s * 0.58f, INK_DIM, Painter.LEFT,
                true);
        p.text(Collect.owned(c.collected) + " / " + Collect.COUNT, ui.optionR(),
                ui.clearLabelY, s * 0.58f, INK, Painter.RIGHT, true);
        int col = c.clearArmed ? ROSE : INK_DIM;
        p.fillRect(ui.optionL(), ui.clearY, ui.optionR(), ui.clearY + ui.clearH,
                Glyph.withAlpha(col, c.clearArmed ? 62 : 30));
        p.strokePoly(new float[] {ui.optionL(), ui.clearY, ui.optionR(), ui.clearY,
                ui.optionR(), ui.clearY + ui.clearH, ui.optionL(), ui.clearY + ui.clearH},
                Glyph.withAlpha(col, c.clearArmed ? 235 : 150), s * 0.05f);
        p.text(c.clearArmed ? "TAP AGAIN TO ERASE" : "CLEAR COLLECTION",
                (ui.optionL() + ui.optionR()) / 2f, ui.clearY + ui.clearH * 0.66f, s * 0.58f,
                c.clearArmed ? ROSE : INK, Painter.CENTER, true);
    }

    /** One decimal place without String.format, which is not worth the cost per frame. */
    static String fmtSpeed(float v) {
        int tenths = Math.round(v * 10f);
        return (tenths / 10) + "." + (tenths % 10);
    }
}
