package com.sram.hexatype;

import java.io.File;

/**
 * Drives {@link GameCore} headlessly to interesting states and renders each one to a PNG
 * through {@link RasterPainter}. This is how the game gets looked at during development
 * without a build/install cycle: same Layout, same Renderer, same state machine as the APK.
 */
final class Preview {

    private static final float DT = 1f / 60f;

    private static final class Mem implements GameCore.Store {
        int best;
        float speed = 1f;
        int bgm;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; }
        public float loadSpeed() { return speed; }
        public void saveSpeed(float v) { speed = v; }
        public int loadBgm() { return bgm; }
        public void saveBgm(int v) { bgm = v; }
    }

    public static void main(String[] args) throws Exception {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 640;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : 1400;
        int ss = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        File dir = new File(args.length > 3 ? args[3] : "out");
        dir.mkdirs();

        Layout L = new Layout();
        L.compute(w, h, 0, 0, 0, 0);
        System.out.printf("layout %dx%d  keyR=%.1f  keyTop=%.0f  dangerY=%.0f  enemyR=%.1f%n",
                w, h, L.keyR, L.keyTop, L.dangerY, L.enemyR);

        characterSheet(dir, w, h, ss);
        sounds(dir);

        Mem store = new Mem();
        store.best = 1840;

        // Title screen.
        GameCore c = new GameCore(store, 7L);
        step(c, L, 0.55f);
        shot(dir, "1-title", c, L, w, h, ss);

        // A wave in flight, nothing typed yet.
        c.startGame();
        step(c, L, 7.0f);
        shot(dir, "2-wave", c, L, w, h, ss);

        // Locked on, first letter struck: colour strobe and scale pop, shot mid-flight.
        GameCore.Enemy e = lowest(c);
        if (e != null) {
            c.tapKey(e.word[e.pos], L);
            step(c, L, 3 * DT);
        }
        shot(dir, "3-hit", c, L, w, h, ss);

        // Perfect play into a later stage.
        GameCore c2 = new GameCore(store, 11L);
        c2.startGame();
        autoplay(c2, L, 60f);
        System.out.printf("autoplay 60s: stage=%d score=%d kills=%d lives=%d combo=%d%n",
                c2.stage, c2.score, c2.kills, c2.lives, c2.combo);
        shot(dir, "4-stage" + c2.stage, c2, L, w, h, ss);

        // Stage-up banner.
        int before = c2.stage;
        float guard = 0;
        while (c2.stage == before && c2.state == GameCore.PLAY && guard < 40f) {
            autoplay(c2, L, DT * 2);
            guard += DT * 2;
        }
        step(c2, L, 0.2f);
        shot(dir, "5-stage-up", c2, L, w, h, ss);

        // Word closing in, on the last life: red tint, alarm line, agitated letters.
        GameCore c4 = new GameCore(store, 17L);
        c4.startGame();
        c4.score = 1310;
        c4.kills = 14;
        c4.stage = 2;
        c4.lives = 1;
        step(c4, L, 1.8f);   // past the opening stage banner
        c4.enemies.clear();
        GameCore.Enemy near = new GameCore.Enemy();
        near.word = new int[] {2, 5, 0};
        near.need = new int[] {1, 2, 1};
        near.baseX = (L.playLeft + L.playRight) / 2f;
        near.y = L.dangerY - (L.dangerY - L.playTop) * 0.06f;
        near.speed = 0;
        c4.enemies.add(near);
        step(c4, L, 2 * DT);
        System.out.printf("danger frame: warn=%.2f harm=%.2f%n", c4.warnLevel, c4.harm());
        shot(dir, "6-danger", c4, L, w, h, ss);

        // Mid-lunge attack.
        near.y = L.dangerY - L.enemyR + 1;
        step(c4, L, GameCore.ATTACK_TIME * 0.55f);
        System.out.printf("attack frame: attacking=%s flash=%.2f%n", near.attacking, c4.flash);
        shot(dir, "7-attack", c4, L, w, h, ss);

        // Game over.
        GameCore c3 = new GameCore(store, 5L);
        c3.startGame();
        c3.score = 2450;
        c3.kills = 26;
        c3.stage = 4;
        c3.maxCombo = 19;
        float t = 0;
        while (c3.state == GameCore.PLAY && t < 120f) {
            c3.update(DT, L);
            t += DT;
        }
        c3.hits = 184;
        c3.misses = 61;   // 75% -> mid mood
        step(c3, L, 1.0f);
        System.out.printf("gameover: accuracy=%d%% mood=%.2f%n",
                c3.accuracyPercent(), c3.accuracyMood());
        shot(dir, "8-gameover", c3, L, w, h, ss);

        // Same screen at both mood extremes.
        c3.hits = 92;
        c3.misses = 84;   // 52% -> saddest
        shot(dir, "9-gameover-sad", c3, L, w, h, ss);
        c3.hits = 240;
        c3.misses = 9;    // 96% -> happiest
        shot(dir, "10-gameover-happy", c3, L, w, h, ss);

        // Flawless wave celebration.
        GameCore c5 = new GameCore(store, 23L);
        c5.startGame();
        c5.score = 980;
        c5.spawnedThisStage = c5.stageQuota();
        c5.enemies.clear();
        c5.shots.clear();
        c5.update(DT, L);
        step(c5, L, 0.42f);   // into the bounce, past the fade-in
        System.out.printf("perfect wave: banner=%.2f stage=%d%n", c5.perfectBanner, c5.stage);
        shot(dir, "11-perfect", c5, L, w, h, ss);

        // Mid-destruction: a cleared word flying apart.
        GameCore c7 = new GameCore(store, 31L);
        c7.startGame();
        c7.score = 640;
        step(c7, L, 1.8f);
        c7.enemies.clear();
        c7.target = null;
        GameCore.Enemy boom = new GameCore.Enemy();
        boom.word = new int[] {1, 3, 0, 4};
        boom.need = new int[] {1, 1, 1, 1};
        boom.baseX = (L.playLeft + L.playRight) / 2f;
        boom.y = L.playTop + (L.dangerY - L.playTop) * 0.45f;
        boom.enterT = 1f;
        c7.enemies.add(boom);
        for (int i = 0; i < boom.word.length; i++) c7.tapKey(boom.word[i], L);
        step(c7, L, 0.16f);          // shot has landed, tiles are on their way out
        step(c7, L, GameCore.DESTROY_TIME * 0.45f);
        System.out.printf("destruction: destroyed=%s t=%.2f dirs=%s shake=%.2f%n",
                boom.destroyed, boom.destroyT, java.util.Arrays.toString(boom.flyDir), c7.shake);
        shot(dir, "13-destroy", c7, L, w, h, ss);

        // Settings panel, opened mid-game.
        GameCore c6 = new GameCore(store, 29L);
        c6.startGame();
        c6.score = 1420;
        c6.stage = 3;
        step(c6, L, 6f);
        c6.setSpeed(1.2f);
        c6.setBgm(Music.DRIFT);
        c6.openSettings();
        step(c6, L, 0.3f);
        shot(dir, "12-settings", c6, L, w, h, ss);
    }

    // ---- driving ------------------------------------------------------------

    private static void step(GameCore c, Layout L, float seconds) {
        for (float t = 0; t < seconds; t += DT) c.update(DT, L);
    }

    private static GameCore.Enemy lowest(GameCore c) {
        GameCore.Enemy best = null;
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.typeable()) continue;
            if (best == null || e.y > best.y) best = e;
        }
        return best;
    }

    /** Kept across calls so a cadence limit survives short autoplay slices. */
    private static int autoBudget;

    /** Plays perfectly: always types the next glyph of the most urgent word. */
    private static void autoplay(GameCore c, Layout L, float seconds) {
        float t = 0;
        while (t < seconds && c.state == GameCore.PLAY) {
            c.update(DT, L);
            t += DT;
            // Human-ish cadence: at most one keypress every other frame.
            if (++autoBudget % 2 != 0) continue;
            GameCore.Enemy e =
                    c.target != null && c.enemies.contains(c.target) && c.target.typeable()
                            ? c.target : lowest(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }
    }

    private static void shot(File dir, String name, GameCore c, Layout L, int w, int h, int ss)
            throws Exception {
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        Renderer.draw(p, c, L);
        File f = new File(dir, name + ".png");
        Png.write(f, p.resolve(), w, h);
        System.out.printf("  wrote %-18s state=%d enemies=%d shots=%d particles=%d score=%d%n",
                f.getName(), c.state, c.enemies.size(), c.shots.size(), c.particles.size(),
                c.score);
    }

    /** Writes every effect and the music loop to WAV so they can be auditioned. */
    private static void sounds(File dir) throws Exception {
        File sfxDir = new File(dir, "sfx");
        sfxDir.mkdirs();
        String[] names = {"squish-dumpling", "squish-strawberry", "squish-cat", "squish-grapes",
                "squish-squishy", "squish-blob", "damage-drip", "clear-word", "wrong",
                "achievement"};
        int peak = 0;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            peak = Math.max(peak, max);
            Wav.write(new File(sfxDir, names[id] + ".wav"), pcm, Sfx.RATE);
        }
        for (int style = 0; style < Music.NAMES.length; style++) {
            if (!Music.isSynth(style)) continue;
            short[] loop = Music.loop(style);
            String slug = Music.NAMES[style].toLowerCase().replace(' ', '-');
            Wav.write(new File(sfxDir, "bgm-" + slug + ".wav"), loop, Sfx.RATE);
            int lmax = 0;
            for (int i = 0; i < loop.length; i++) lmax = Math.max(lmax, Math.abs(loop[i]));
            System.out.printf("  wrote bgm-%-12s %.2fs peak=%d%n", slug,
                    (float) loop.length / Sfx.RATE, lmax);
        }
        System.out.printf("  wrote %d sfx, peak=%d%n", Sfx.COUNT, peak);
    }

    /** Harness-only sheet: every character large, for checking the faces read clearly. */
    private static void characterSheet(File dir, int w, int h, int ss) throws Exception {
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        p.fillRect(0, 0, w, h, Renderer.BG);
        float unit = 0.042f * w;
        p.text("THE SIX LETTERS", w / 2f, unit * 2.6f, unit * 1.1f, Renderer.INK,
                Painter.CENTER, true);

        float r = w / 7.6f;
        for (int g = 0; g < Glyph.COUNT; g++) {
            float cx = w * (0.22f + 0.28f * (g % 3));
            float cy = h * (0.24f + 0.22f * (g / 3));
            int col = Glyph.COLOR[g];
            p.fillPoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, 46));
            p.strokePoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, 200), r * 0.07f);
            Kawaii.draw(p, g, cx, cy, r * 0.60f, col, 1f, 0.35f);
            p.text(Glyph.NAME[g].toUpperCase(), cx, cy + r * 1.40f, unit * 0.56f,
                    Renderer.INK_DIM, Painter.CENTER, true);
        }

        // Same characters at real tile size, plus a struck-and-strobing variant.
        float tr = 0.052f * w * Layout.HEAD_SCALE;
        p.text("AT TILE SIZE", w / 2f, h * 0.755f, unit * 0.66f, Renderer.INK_DIM,
                Painter.CENTER, true);
        for (int g = 0; g < Glyph.COUNT; g++) {
            float cx = w * (0.135f + 0.146f * g);
            int col = Glyph.COLOR[g];
            p.fillPoly(Glyph.hex(cx, h * 0.815f, tr), Glyph.withAlpha(col, 52));
            p.strokePoly(Glyph.hex(cx, h * 0.815f, tr), Glyph.withAlpha(col, 165), tr * 0.075f);
            Kawaii.draw(p, g, cx, h * 0.815f, tr * 0.60f, col, 1f, 0.4f);

            int hot = Glyph.mix(col, Glyph.cycle(g / 6f), 0.9f);
            p.fillPoly(Glyph.hex(cx, h * 0.90f, tr), Glyph.withAlpha(hot, 52));
            p.strokePoly(Glyph.hex(cx, h * 0.90f, tr), Glyph.withAlpha(hot, 165), tr * 0.075f);
            Kawaii.draw(p, g, cx, h * 0.90f, tr * 0.60f * 1.34f, hot, 1.16f, 0.4f);
        }
        p.text("STRUCK", w / 2f, h * 0.955f, unit * 0.56f, Renderer.INK_DIM, Painter.CENTER, true);

        File f = new File(dir, "0-characters.png");
        Png.write(f, p.resolve(), w, h);
        System.out.println("  wrote " + f.getName());
    }
}
