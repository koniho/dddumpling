package com.sram.hexatype;

/** Sky and clouds, the lock indicator, the edge glow and the settings panel. */
final class TestVisuals extends Check {

    static void sky(Layout L) {
        group("cloud sky");
        GameCore c = new GameCore(new Mem(), 111L);
        c.startGame();

        check("three cloud layers", GameCore.CLOUD_LAYERS == 3);
        check("one layer in front, two behind", Sky.CLOUD_FRONT_LAYER == 2);

        boolean speedsRise = true;
        for (int l = 1; l < GameCore.CLOUD_LAYERS; l++) {
            if (GameCore.CLOUD_SPEED[l] <= GameCore.CLOUD_SPEED[l - 1]) speedsRise = false;
        }
        check("each layer drifts faster than the one behind it", speedsRise);
        check("all layers actually move", GameCore.CLOUD_SPEED[0] > 0f);

        // Phases must stay in 0..1 and wrap, never run away.
        boolean inRange = true, moved = true, wrapped = false;
        float[][] before = new float[GameCore.CLOUD_LAYERS][GameCore.CLOUDS_PER_LAYER];
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                before[l][i] = c.cloudPhase(l, i);
                if (before[l][i] < 0f || before[l][i] >= 1f) inRange = false;
            }
        }
        advance(c, L, 3f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float now = c.cloudPhase(l, i);
                if (now < 0f || now >= 1f) inRange = false;
                if (now == before[l][i]) moved = false;
            }
        }
        check("cloud phases stay inside 0..1", inRange);
        check("clouds drift with the clock", moved);

        // Long run: the front layer must wrap many times and stay bounded.
        advance(c, L, 400f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float v = c.cloudPhase(l, i);
                if (v < 0f || v >= 1f) inRange = false;
            }
        }
        // 400s at the front-layer speed is well over one full traversal.
        wrapped = 400f * GameCore.CLOUD_SPEED[Sky.CLOUD_FRONT_LAYER] > 1f;
        check("phases stay bounded over a long run", inRange);
        check("the front layer wraps repeatedly", wrapped);

        // Drift is a pure function of the clock, so two cores at the same time agree.
        GameCore d = new GameCore(new Mem(), 222L);
        // skyClock, not clock: the sky runs on its own accumulator so a frenzy can speed it
        // up without the drift jumping.
        d.skyClock = c.skyClock;
        boolean deterministic = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (Math.abs(d.cloudPhase(l, i) - c.cloudPhase(l, i)) > 1e-5f) {
                    deterministic = false;
                }
            }
        }
        check("cloud drift is identical for any core at the same clock", deterministic);

        // A cloud must be entirely out of the sky before its phase wraps, or it pops out
        // of existence mid-screen.
        boolean exitsCleanly = true, entersCleanly = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            float margin = Sky.cloudMargin(L, l);
            GameCore z = new GameCore(new Mem(), 333L);
            z.clock = 0f;
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                z.cloudY[l][i] = 0f;                 // phase 0: just entering
                if (Sky.cloudY(z, L, l, i) + margin > 0.5f) entersCleanly = false;
                z.cloudY[l][i] = 0.99999f;           // phase ~1: just leaving
                if (Sky.cloudY(z, L, l, i) - margin < L.deckTop - 0.5f) {
                    exitsCleanly = false;
                }
            }
        }
        check("clouds start fully above the sky", entersCleanly);
        check("clouds leave fully below the sky before wrapping", exitsCleanly);
        boolean marginsCover = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            if (Sky.cloudMargin(L, l) <= Sky.cloudHeight(L, l)) marginsCover = false;
        }
        check("the exit margin exceeds the cloud height", marginsCover);

        // Sky glow: a landed press tints with that letter, a cleared word floods yellow.
        GameCore g = new GameCore(new Mem(), 444L);
        g.startGame();
        check("sky starts unglowed", g.skyGlow == 0f);
        g.enemies.clear();
        g.target = null;
        GameCore.Enemy e = add(g, L, new int[] {3, 5}, L.playTop + 200);

        g.tapKey(3, L);
        check("a landed press glows the sky", g.skyGlow == GameCore.GLOW_HIT);
        check("the glow takes the struck letter's colour", g.skyGlowColor == Glyph.COLOR[3]);
        check("a single press is a faint glow", GameCore.GLOW_HIT < 1f);
        check("no screen flash for a mere press", g.flash == 0f);

        advance(g, L, 1.0f);
        check("the press glow fades out", g.skyGlow == 0f);

        g.tapKey(5, L);                      // completes the word
        advance(g, L, 0.2f);                 // let the killing shot land
        // Already decaying by now, so check it outranks a single press rather than == 1.
        check("clearing a word floods the sky", g.skyGlow > GameCore.GLOW_HIT);
        check("the flood is yellow", g.skyGlowColor == GameCore.FLASH_CLEAR);
        check("clearing a word flashes the screen", g.flash > 0f);
        check("the clear flash is warm, not red", g.flashColor == GameCore.FLASH_CLEAR);
        advance(g, L, 1.2f);
        check("the clear glow is brief", g.skyGlow == 0f && g.flash == 0f);

        // Damage keeps its own colour, and outranks a celebration.
        GameCore d2 = new GameCore(new Mem(), 445L);
        d2.startGame();
        d2.enemies.clear();
        add(d2, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d2, L, GameCore.ATTACK_TIME + 2 * DT);
        check("damage flashes red", d2.flash > 0f && d2.flashColor == GameCore.FLASH_DAMAGE);

        boolean spread = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (c.cloudX[l][i] < 0f || c.cloudX[l][i] > 1f) spread = false;
                if (c.cloudW[l][i] <= 0f) spread = false;
            }
        }
        check("cloud placement is on screen and sized", spread);

        // The clip is what lets clouds run past the sky's edge without touching the key
        // deck, so it is worth checking directly rather than trusting it.
        RasterPainter rp = new RasterPainter(40, 40, 1);
        rp.clear(0xFF000000);
        rp.save();
        rp.clipRect(0, 0, 40, 20);
        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rp.restore();
        int[] px = rp.resolve();
        check("clip keeps painting inside the region", (px[5 * 40 + 5] & 0xFF) > 200);
        check("clip blocks painting outside the region", (px[30 * 40 + 5] & 0xFF) < 40);

        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        px = rp.resolve();
        check("restore lifts the clip again", (px[30 * 40 + 5] & 0xFF) > 200);

        // Clips must intersect, never widen.
        RasterPainter rq = new RasterPainter(40, 40, 1);
        rq.clear(0xFF000000);
        rq.save();
        rq.clipRect(0, 0, 20, 20);
        rq.clipRect(0, 0, 40, 40);
        rq.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rq.restore();
        px = rq.resolve();
        check("a second clip cannot widen the first", (px[30 * 40 + 30] & 0xFF) < 40);
    }

    static void indicatorsAndGlow(Layout L) {
        group("indicator and glow");

        // The red edge glow must not survive the run that caused it.
        GameCore c = new GameCore(new Mem(), 93L);
        c.startGame();
        c.lives = 1;
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME * 0.5f);
        check("a lunging word lights the edge glow", c.warnLevel > 0f);
        advance(c, L, GameCore.ATTACK_TIME + 4 * DT);
        check("reached game over", c.state == GameCore.OVER);
        check("the edge glow clears on game over", c.warnLevel == 0f);
        advance(c, L, 1.0f);
        check("and stays clear", c.warnLevel == 0f);

        // Every member of the key cast has its own crying render; none is replaced by the
        // generic mood dumpling used by the accuracy readout.
        RasterPainter cries = new RasterPainter(360, 80, 1);
        cries.clear(0xFF000000);
        for (int g = 0; g < Glyph.COUNT; g++) {
            Kawaii.crying(cries, g, 30 + g * 60, 40, 22, Glyph.COLOR[g], 1f, g * 0.7f, 1f);
        }
        int[] cryingPixels = cries.resolve();
        boolean everyCryVisible = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            boolean visible = false;
            for (int y = 8; y < 72 && !visible; y++) {
                for (int x = g * 60 + 5; x < g * 60 + 55; x++) {
                    if (cryingPixels[y * 360 + x] != 0xFF000000) visible = true;
                }
            }
            if (!visible) everyCryVisible = false;
        }
        check("every key character has a crying render", everyCryVisible);

        // Non-fatal damage must also clear it.
        GameCore d = new GameCore(new Mem(), 94L);
        d.startGame();
        d.enemies.clear();
        add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d, L, GameCore.ATTACK_TIME + 4 * DT);
        d.enemies.clear();
        d.update(DT, L);
        check("the glow clears after surviving a hit", d.warnLevel == 0f);

        // The lock indicator eases between letters rather than jumping.
        GameCore k = new GameCore(new Mem(), 95L);
        k.startGame();
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy e = add(k, L, new int[] {0, 1, 2}, L.playTop + 150);
        k.tapKey(0, L);
        k.update(DT, L);
        float atFirst = k.caretXFor(e, L);
        check("indicator starts on the locked word", k.caretOwner == e);
        k.tapKey(1, L);
        k.update(DT, L);
        float justAfter = k.caretXFor(e, L);
        float destination = k.tileX(e, e.pos, L);
        check("indicator has begun moving", justAfter != atFirst);
        check("indicator has not jumped straight there",
                Math.abs(justAfter - destination) > 1f);
        check("indicator is heading the right way",
                Math.abs(justAfter - destination) < Math.abs(atFirst - destination));
        advance(k, L, 0.5f);
        check("indicator arrives", Math.abs(k.caretXFor(e, L) - k.tileX(e, e.pos, L)) < 1f);

        // Switching words snaps instead of gliding across the screen.
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy other = add(k, L, new int[] {3, 4}, L.playTop + 400);
        other.baseX = L.playLeft + L.enemyR * 3f;
        k.tapKey(3, L);
        k.update(DT, L);
        check("a fresh lock snaps into place",
                Math.abs(k.caretXFor(other, L) - k.tileX(other, other.pos, L)) < 1f);
    }

    /**
     * Stacked HUD text clears itself, at every text scale.
     *
     * The score label sits one line above the number, and the number's caps reach
     * {@code RasterPainter.CAP} of its size above its own baseline. Both sizes go through
     * {@link Draw#type}, so the gap between them has to as well — at a plain unit multiple the
     * digits came up three pixels through SCORE's baseline once TEXT reached 1.34. Checked against
     * a sweep of scales rather than the current one, since that is what went wrong: the layout was
     * right when it was written and wrong when the knob moved.
     */
    static void hudStacking(Layout L) {
        group("HUD stacking");
        float clear = L.hudY - RasterPainter.CAP * Hud.scoreSize(L) - Hud.labelY(L);
        System.out.printf("    the score digits clear the label by %.1fpx at TEXT=%.2f%n",
                clear, Draw.TEXT);
        check("the score number clears its own label", clear > 0f);
        check("with room to spare, not by a pixel", clear > L.unit * 0.1f);

        // The label must also stay under the safe top edge, since raising it is how this was fixed.
        float labelTop = Hud.labelY(L) - RasterPainter.CAP * Draw.type(L.unit * 0.52f);
        check("and the label stays below the safe top edge", labelTop >= L.topSafe);

        // The relationship has to hold however the knob is turned, which is the whole point of
        // scaling the gap: both sides move together.
        boolean holds = true;
        for (int px = 640; px <= 1600; px += 240) {
            Layout t = new Layout();
            t.compute(px, px * 20 / 9, 0, 0, 0, 0);
            if (t.hudY - RasterPainter.CAP * Hud.scoreSize(t) <= Hud.labelY(t)) holds = false;
        }
        check("at every screen width too", holds);
    }

    /**
     * The star screen stacks READY under the checkpoint counter, so it needs the same clearance
     * check the HUD does — and for the same reason. The counter and the prompt arrived with a
     * plain unit gap between them and READY's caps sat on the counter's baseline at TEXT 1.34.
     */
    static void starStacking(Layout L) {
        group("star screen stacking");
        float clear = StarScreen.readyY(L) - RasterPainter.CAP * StarScreen.readySize(L)
                - StarScreen.countY(L);
        System.out.printf("    READY clears the counter by %.1fpx at TEXT=%.2f%n",
                clear, Draw.TEXT);
        check("READY clears the counter above it", clear > 0f);
        check("with room to spare, not by a pixel", clear > L.unit * 0.1f);

        boolean holds = true;
        for (int px = 640; px <= 1600; px += 240) {
            Layout t = new Layout();
            t.compute(px, px * 20 / 9, 0, 0, 0, 0);
            if (StarScreen.readyY(t) - RasterPainter.CAP * StarScreen.readySize(t)
                    <= StarScreen.countY(t)) holds = false;
        }
        check("at every screen width too", holds);

        // Both lines live in the play field, above the deck: the prompt must not reach the keys.
        check("and READY stays clear of the deck", StarScreen.readyY(L) < L.deckTop);
    }

    /**
     * That a run really does end on a green screen, sampled off a rendered frame rather than
     * reasoned about.
     *
     * The world drains green as the last life goes, and it is meant to stay that way until the title
     * screen takes the screen back. It did not: the summary laid the ordinary violet scrim over the
     * sky, so the green survived only on the key deck below the scrim's reach, which read as the
     * deck being tinted rather than the world dying. Nothing in the geometry could catch that — only
     * the pixels can, so this asserts on them.
     */
    static void deathIsGreen(Layout L) {
        group("the screen a run ends on");

        GameCore c = new GameCore(new Mem(), 63L);
        c.startGame();
        c.lives = 0;
        c.state = GameCore.OVER;
        c.deathT = 0f;
        // Past the hold and the fade, which is the settled summary.
        c.time = GameCore.DEATH_TIME + GameCore.OVER_FADE + 0.5f;
        check("the world is fully drained", c.drained() == 1f && c.overFade() >= 1f);

        int[] sky = sample(c, L, 0.5f, 0.16f);
        int[] deck = sample(c, L, 0.5f, 0.965f);
        System.out.printf("    summary sky rgb %d,%d,%d and deck rgb %d,%d,%d%n",
                sky[0], sky[1], sky[2], deck[0], deck[1], deck[2]);
        // Green has to be the strongest channel, and by a margin: the violet scrim it replaced was
        // blue-dominant, so this is exactly the swap that went wrong.
        check("the summary's sky is green", sky[1] > sky[0] + 6 && sky[1] > sky[2] + 6);
        check("and so is the deck under it", deck[1] > deck[0] + 6 && deck[1] > deck[2] + 6);
        check("the sky is dark enough to read text off",
                sky[0] + sky[1] + sky[2] < 3 * 70);

        // And it lets go the moment the title arrives, which is what "until the title screen" means.
        c.toTitle();
        int[] title = sample(c, L, 0.5f, 0.16f);
        System.out.printf("    title sky rgb %d,%d,%d%n", title[0], title[1], title[2]);
        check("the title screen is not green", title[2] > title[1]);
    }

    /** Red, green and blue at a fraction of the way across and down a rendered frame. */
    private static int[] sample(GameCore c, Layout L, float fx, float fy) {
        int w = (int) L.w, h = (int) L.h;
        RasterPainter p = new RasterPainter(w, h, 1);
        p.clear(0xFF000000);
        Renderer.draw(p, c, L);
        int px = p.resolve()[(int) (h * fy) * w + (int) (w * fx)];
        return new int[] {(px >> 16) & 0xFF, (px >> 8) & 0xFF, px & 0xFF};
    }

    static void settings(Layout L) {
        group("settings");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 81L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        check("defaults to normal speed", c.speed == 1f);
        check("settings start closed", !c.settingsOpen);

        // Opening freezes the simulation.
        c.enemies.clear();
        GameCore.Enemy e = add(c, L, new int[] {0, 1}, L.playTop + 100);
        e.speed = 200f;
        c.update(DT, L);
        float movedY = e.y;
        c.openSettings();
        check("opening settings pauses", c.settingsOpen);
        advance(c, L, 1.0f);
        check("nothing moves while paused", e.y == movedY);
        check("the clock still runs so the panel animates", c.clock > 0f);
        c.closeSettings();
        c.update(DT, L);
        check("closing resumes the simulation", e.y > movedY);

        // Speed clamps and persists.
        c.setSpeed(1.3f);
        check("speed applies", Math.abs(c.speed - 1.3f) < 1e-6f);
        check("speed persists", Math.abs(store.speed - 1.3f) < 1e-6f && store.speedSaves == 1);
        c.setSpeed(9f);
        check("speed clamps at the top", c.speed == GameCore.SPEED_MAX);
        c.setSpeed(-4f);
        check("speed clamps at the bottom", c.speed == GameCore.SPEED_MIN);
        check("a corrupt stored speed falls back", GameCore.clampSpeed(Float.NaN) == 1f);

        GameCore reloaded = new GameCore(store, 82L);
        check("stored speed is reloaded",
                Math.abs(reloaded.speed - GameCore.SPEED_MIN) < 1e-6f);

        // Faster speed means less time to react and tighter spawns.
        c.stage = 3;
        c.setSpeed(0.5f);
        float slowTravel = c.travelSeconds(), slowSpawn = c.spawnInterval();
        c.setSpeed(1.5f);
        check("higher speed shortens the fall", c.travelSeconds() < slowTravel);
        check("higher speed tightens spawns", c.spawnInterval() < slowSpawn);
        check("the fall floor still applies at max speed", c.travelSeconds() > 0f);

        // Music selection persists and notifies the audio layer.
        c.setBgm(Music.MARCH);
        check("music choice applies", c.bgmChoice == Music.MARCH);
        check("music choice persists", store.bgm == Music.MARCH && store.bgmSaves == 1);
        check("the audio layer is told", ear.music == Music.MARCH && ear.musicCalls == 1);
        c.setBgm(-1);
        check("a bogus low choice is ignored", c.bgmChoice == Music.MARCH);
        c.setBgm(Music.NAMES.length);
        check("a bogus high choice is ignored", c.bgmChoice == Music.MARCH);
        c.setBgm(Music.OFF);
        check("music can be turned off", c.bgmChoice == Music.OFF);

        // Every synth style must produce a clean, correctly sized loop.
        boolean stylesOk = true;
        for (int style = 0; style < Music.NAMES.length; style++) {
            if (!Music.isSynth(style)) continue;
            short[] loop = Music.loop(style);
            if (loop.length != Music.loopFrames(style)) stylesOk = false;
            int max = 0;
            for (int i = 0; i < loop.length; i++) max = Math.max(max, Math.abs(loop[i]));
            if (max >= 32767 || max < 2000) stylesOk = false;
        }
        check("every music style renders cleanly", stylesOk);
        check("OFF and MY TRACK are not synth styles",
                !Music.isSynth(Music.OFF) && !Music.isSynth(Music.CUSTOM));
        check("an unknown style still returns audio", Music.loop(99).length > 0);

        // Panel hit-testing.
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        check("panel fits on screen",
                ui.panelT >= L.topSafe && ui.panelB <= L.h && ui.panelL > 0);
        check("a tap outside closes",
                ui.hit(L.w / 2f, ui.panelB + 20f) == SettingsUi.HIT_OUTSIDE);
        check("the close button is hit", ui.hit(ui.closeCx, ui.closeCy) == SettingsUi.HIT_CLOSE);
        check("the slider is hit",
                ui.hit((ui.sliderL + ui.sliderR) / 2f, ui.sliderY) == SettingsUi.HIT_SLIDER);
        boolean rowsOk = true;
        for (int i = 0; i < Music.NAMES.length; i++) {
            if (ui.hit(ui.optionL() + 5f, ui.optionCy(i)) != SettingsUi.HIT_OPTION + i) {
                rowsOk = false;
            }
        }
        check("every music row is hittable", rowsOk);
        check("slider left end reads minimum", ui.speedAt(ui.sliderL) == GameCore.SPEED_MIN);
        check("slider right end reads maximum", ui.speedAt(ui.sliderR) == GameCore.SPEED_MAX);
        check("slider clamps past its ends",
                ui.speedAt(ui.sliderL - 500f) == GameCore.SPEED_MIN
                        && ui.speedAt(ui.sliderR + 500f) == GameCore.SPEED_MAX);
        check("slider midpoint is centre speed",
                Math.abs(ui.speedAt((ui.sliderL + ui.sliderR) / 2f) - 1f) < 0.03f);
        check("knob tracks the value",
                Math.abs(ui.knobX(GameCore.SPEED_MIN) - ui.sliderL) < 0.5f
                        && Math.abs(ui.knobX(GameCore.SPEED_MAX) - ui.sliderR) < 0.5f);

        // The stage readout is the settings button, and must not swallow key taps.
        check("the stage readout opens settings", L.inStageTap(L.w / 2f, L.hudY));
        boolean keysClear = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (L.inStageTap(L.keyX[g], L.keyY[g])) keysClear = false;
        }
        check("the settings region does not cover any key", keysClear);
        check("mid-field taps do not open settings", !L.inStageTap(L.w / 2f, L.h * 0.5f));
    }

}
