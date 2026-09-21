package com.dddumpling.game;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Scripted capture of the real game renderer; no runtime game rules are changed. */
final class Trailer {
    static final int W = 480, H = 1050, FPS = 30;
    static final float DT = 1f / 60f;
    static final Layout L = new Layout();
    static final float[] LENGTHS = {1.5f, 6f, 4f, 5f, 5f, 4f, 4f, 4f, 2f};
    static final String[] NAMES = {"hello", "tap", "power", "octopulse", "divide",
            "mushroom", "stars", "collection", "end"};
    static final Mixer MIX = new Mixer();
    static int frame;

    public static void main(String[] args) throws Exception {
        L.compute(W, H, 0, 0, 0, 0);
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                "-f", "rawvideo", "-pixel_format", "rgb24", "-video_size", W + "x" + H,
                "-framerate", "" + FPS, "-i", "pipe:0", "-an", "-c:v", "libx264",
                "-preset", "veryfast", "-crf", "17", "-pix_fmt", "yuv420p",
                "build/trailer/gameplay-silent.mp4");
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process encoder = pb.start();
        BufferedOutputStream out = new BufferedOutputStream(encoder.getOutputStream(), W * H * 3);
        RasterPainter p = new RasterPainter(W, H, 1);
        byte[] rgb = new byte[W * H * 3];
        for (int scene = 0; scene < NAMES.length; scene++) {
            GameCore c = setup(scene);
            Bot bot = new Bot(scene == 2 ? 3f : 6f, 0.18f, 0f, true, 91L + scene);
            c.sound = (GameCore.Sound) Proxy.newProxyInstance(Trailer.class.getClassLoader(),
                    new Class<?>[]{GameCore.Sound.class}, MIX);
            int count = Math.round(LENGTHS[scene] * FPS);
            System.out.println("Rendering " + NAMES[scene] + ": " + count + " frames");
            for (int f = 0; f < count; f++, frame++) {
                for (int sub = 0; sub < 2; sub++) {
                    MIX.at = Math.round((frame / (float) FPS + sub * DT) * Sfx.RATE);
                    drive(scene, c, bot, f, sub);
                    c.update(DT, L);
                }
                p.clear(Draw.BG);
                if (scene == 8) endCard(p, f / (float) FPS);
                else Renderer.draw(p, c, L);
                int[] pixels = p.resolve();
                for (int i = 0, j = 0; i < pixels.length; i++) {
                    rgb[j++] = (byte) (pixels[i] >> 16);
                    rgb[j++] = (byte) (pixels[i] >> 8);
                    rgb[j++] = (byte) pixels[i];
                }
                out.write(rgb);
                if (f == count / 2)
                    Png.write(new File("build/trailer/" + NAMES[scene] + ".png"), pixels, W, H);
            }
            System.out.printf("  stage=%d state=%d score=%d hp=%.0f stars=%d%n",
                    c.stage, c.state, c.score, c.boss.hp, c.stars.count());
        }
        out.close();
        if (encoder.waitFor() != 0) throw new IOException("Video encoder failed");
        MIX.write(frame / (float) FPS);
        System.out.println("Captured " + frame + " frames.");
    }

    static GameCore setup(int scene) {
        Check.Mem m = new Check.Mem();
        m.collected = Collect.MASK;
        m.collectTotal = Collect.COUNT;
        GameCore c = new GameCore(m, 310L + scene);
        if (scene == 0) { warm(c, 2f); return c; }
        if (scene == 7) {
            c.openCase(); c.caseTo(Collect.BOSS_FIRST + 2); warm(c, 0.6f); return c;
        }
        c.startGame();
        if (scene == 1 || scene == 2) {
            c.stage = scene == 1 ? 2 : 4;
            warm(c, 7f);
            if (scene == 2) c.playtestMode(Power.FLURRY, L);
        } else if (scene >= 3 && scene <= 5) {
            c.stage = Boss.EVERY; c.enemies.clear();
            int kind = scene == 3 ? Boss.OCTOPUS : scene == 4 ? Boss.SPLITTER : Boss.MUSHROOM;
            c.boss.begin(kind, c.stage, c.rnd);
            for (int i = 0; i < 600 && !c.boss.fighting(); i++) c.update(DT, L);
            warm(c, 0.2f);
        } else if (scene == 6) {
            c.prize = 0;
            c.playtestStars(L);
            warm(c, StarPath.READY - 0.15f);
            c.stars.beginDrag();
        }
        return c;
    }

    static void warm(GameCore c, float seconds) {
        for (int i = 0; i < seconds * 60; i++) c.update(DT, L);
    }

    static void drive(int scene, GameCore c, Bot bot, int f, int sub) {
        if (scene == 1 || scene == 2) {
            GameCore.Enemy lowest = Check.urgent(c);
            boolean engaged = c.target != null && c.target.pos > 0;
            if (engaged || (lowest != null && lowest.y > L.playTop
                    + (L.dangerY - L.playTop) * (scene == 1 ? 0.35f : 0.25f)))
                bot.step(c, L, DT);
        } else if (scene >= 3 && scene <= 5) bot.step(c, L, DT);
        if (scene == 6 && c.stars.flying()) {
            int nearest = -1; float distance = Float.MAX_VALUE;
            for (int i = 0; i < StarPath.COUNT; i++) {
                if ((c.stars.collected & (1 << i)) != 0) continue;
                float dy = c.stars.starY(i, L) - c.stars.flyerY(L);
                if (Math.abs(dy) < distance) { nearest = i; distance = Math.abs(dy); }
            }
            if (nearest >= 0) c.stars.dragTo(c.stars.starX(nearest, L), L);
        }
        if (scene == 7 && sub == 0) {
            if (f == 35) CaseUi.select(c, Collect.BOSS_FIRST + 3);
            if (f == 80) CaseUi.select(c, Collect.BOSS_FIRST + 1);
        }
    }

    static void endCard(Painter p, float t) {
        String title = "DDDUMPLING";
        for (int i = 0; i < 10; i++)
            TitleBubbleFont.draw(p, title.charAt(i), 58 + i % 5 * 90,
                    i < 5 ? 300 : 406, 91, Glyph.COLOR[i % 6], 1f, t + i, 1f);
        Kawaii.draw(p, Kawaii.DUMPLING, W / 2f, 625, 120, 0xFF9EE65B, 1f, 0.35f);
        p.text("TAP. BATTLE. COLLECT.", W / 2f, 839, 20, Draw.INK, Painter.CENTER, true);
        p.text("DDDUMPLING", W / 2f, 902, 14, Draw.INK_DIM, Painter.CENTER, true);
    }

    static final class Mixer implements InvocationHandler {
        final float[] audio = new float[Sfx.RATE * 45];
        int at;
        final Map<String,Integer> sounds = new HashMap<String,Integer>();
        Mixer() {
            String[] names = {"clearWord","wrong","damage","achievement","bossLaugh","bossDamage",
                    "slimeDamage","bossSplit","divideDamage","divideSplit","divideDeactivate",
                    "boltPop","boltDeath","shieldBounce","octoWave","octoCue","octoLock","mushroomShake",
                    "mushroomSpore","chop","zap","collect","star","courseStart","tally",
                    "paradeJoin","rosterJoin","gameOver","gameStart","stageClear","powerClear"};
            int[] ids = {Sfx.CLEAR,Sfx.WRONG,Sfx.DRIP,Sfx.ACHIEVEMENT,Sfx.BOSS_LAUGH,Sfx.BOSS_DAMAGE,
                    Sfx.SLIME_DAMAGE,Sfx.BOSS_SPLIT,Sfx.DIVIDE_DAMAGE,Sfx.DIVIDE_SPLIT,Sfx.DIVIDE_DEACTIVATE,
                    Sfx.BOLT_POP,Sfx.BOLT_DEATH,Sfx.SHIELD_BOUNCE,Sfx.OCTO_WAVE,Sfx.OCTO_CUE,Sfx.OCTO_LOCK,Sfx.MUSHROOM_SHAKE,
                    Sfx.MUSHROOM_SPORE,Sfx.CHOP,Sfx.ZAP,Sfx.COLLECT,Sfx.STAR,Sfx.COURSE,Sfx.TALLY,
                    Sfx.JOIN,Sfx.ROSTER_JOIN,Sfx.OVER,Sfx.START,Sfx.STAGE_CLEAR,Sfx.POWER_CLEAR};
            for (int i = 0; i < names.length; i++) sounds.put(names[i], ids[i]);
        }
        public Object invoke(Object proxy, Method method, Object[] args) {
            Integer id = sounds.get(method.getName());
            if (method.getName().equals("squish")) id = Sfx.SQUISH_0 + (Integer) args[0];
            if (id != null) {
                short[] pcm = Sfx.build(id);
                for (int i = 0; i < pcm.length && at + i < audio.length; i++)
                    audio[at + i] += pcm[i] * 0.32f;
            }
            return null;
        }
        void write(float duration) throws Exception {
            short[] music = Music.loop(Music.SWING_STYLE);
            short[] result = new short[Math.round(duration * Sfx.RATE)];
            for (int i = 0; i < result.length; i++) {
                float fade = Math.min(1f, Math.min(i / (Sfx.RATE * 0.3f),
                        (result.length - i) / (Sfx.RATE * 0.7f)));
                float value = (audio[i] + music[i % music.length] * 0.28f) * fade;
                result[i] = (short) Math.max(-30000, Math.min(30000, value));
            }
            Wav.write(new File("build/trailer/soundtrack.wav"), result, Sfx.RATE);
        }
    }
}
