package com.dddumpling.game;

/** Back navigation and a modal pause panel, shared by Android and the preview harness. */
final class Pause extends Draw {
    private Pause() {}
    static boolean handlesBack(GameCore c) {
        return c.releaseNotes.open || c.returnFade > 0f || c.paused || c.settingsOpen || c.storyOpen() || c.caseOpen
                || c.starting() || c.state != GameCore.TITLE;
    }
    static boolean back(GameCore c) {
        if(c.releaseNotes.open) { c.releaseNotes.back();return true; }
        if (c.returnFade > 0f) return true;
        if (c.confirmEnd) { c.confirmEnd = false; return true; }
        if (c.paused) { resume(c); return true; }
        if (c.settingsOpen) { c.closeSettings(); return true; }
        if (c.storyOpen()) { c.closeStory(); return true; }
        if (c.caseOpen) { c.closeCase(); return true; }
        if (c.starting()) {
            c.cancelStart();
            return true;
        }
        if (c.state == GameCore.OVER) { c.dismissGameOver(); return true; }
        if (c.state == GameCore.PLAY || c.state == GameCore.BONUS) { open(c); return true; }
        return false;
    }
    static void release(GameCore c) {
        c.endStroke(); c.boss.release(); c.stars.endDrag(); c.cave.input.release(); c.cave.effects.takeFeedback(); c.mining.input.release();
        c.stars.left = c.stars.right = false;
        c.steamer.lidDrag = 0;
        c.caseDragging = c.titleTouchDown = c.landPickerDragging = false;
        if (c.sound != null) { c.sound.bossCharge(0); c.sound.rocket(0); }
    }
    static void open(GameCore c) {
        if (c.state != GameCore.PLAY && c.state != GameCore.BONUS) return;
        if (c.paused) return;
        if (c.band.active && c.sound != null) c.sound.bandPause(true);
        release(c); c.paused = true; c.confirmEnd = false;
    }
    static void resume(GameCore c) {
        c.paused = c.confirmEnd = false;
        if (c.band.active && c.sound != null) c.sound.bandPause(false);
    }
    static void action(GameCore c, int hit) {
        if (!c.paused) return;
        if (hit == 1) {
            resume(c);
        } else if (hit == 2) {
            if (!c.confirmEnd) c.confirmEnd = true;
            else end(c);
        }
    }
    private static void end(GameCore c) {
        LandPicker.recordBest(c);
        release(c); resume(c); c.closeSettings();
        c.boss.leave(); c.buddy.leave(); c.power = null;
        c.mode = -1; c.modeLeft = 0; c.particles.clear();
        c.pendingBonus = c.bossReward = c.bossPrizePending = false;
        c.perfectBanner = c.paradeTimer = c.bonusTimer = c.stageBanner = 0;
        c.pushT = c.pushSlowT = c.shake = c.flash = c.skyGlow = 0;
        c.slowdown = c.sliceCall = 0;
        if (c.sound != null) { c.sound.frenzy(false); c.sound.bossMusic(false); }
        // Quitting is not a loss and must not change the adaptive roster's loss counters.
        c.toTitle();
    }
    static float scale(Layout L) { return Math.min(L.w / 20f, L.h / 28f); }
    static float buttonY(Layout L, int hit) { return L.h * .5f + scale(L) * (hit == 1 ? 1.6f : 4.5f); }
    static int hit(Layout L, float x, float y) {
        float s = scale(L);
        for (int i = 1; i <= 2; i++)
            if (Math.abs(x - L.w * .5f) <= s * 6.8f && Math.abs(y - buttonY(L, i)) <= s * 1.1f) return i;
        return 0;
    }
    static void draw(Painter p, GameCore c, Layout L) {
        if (!c.paused) return;
        float s = scale(L), x = L.w * .5f, y = L.h * .5f;
        p.fillRect(0, 0, L.w, L.h, 0xDA100D20);
        p.fillPoly(pill(x, y - 5.3f*s, 1.4f*s, 1.4f*s, 18), BG_HI);
        p.fillRect(x-.45f*s, y-5.95f*s, x-.13f*s, y-4.65f*s, GOLD);
        p.fillRect(x+.13f*s, y-5.95f*s, x+.45f*s, y-4.65f*s, GOLD);
        p.text(c.confirmEnd ? "END THIS RUN?" : "PAUSED", x, y-2.65f*s,
                type(s*1.03f), INK, Painter.CENTER, true);
        p.text(c.confirmEnd ? "Your collected friends are safe." : "Take a little breather.",
                x, y-1.25f*s, type(s*.44f), INK_DIM, Painter.CENTER, false);
        for (int i = 1; i <= 2; i++) {
            float by = buttonY(L, i);
            int color = i == 1 ? GOLD : c.confirmEnd ? ROSE : BG_HI;
            p.fillPoly(pill(x, by, s*6.8f, s*1.1f, 18), color);
            String label = i == 1 ? (c.confirmEnd ? "KEEP PLAYING" : "RESUME") : "END RUN";
            p.text(label, x, by+s*.27f, type(s*.62f), i == 1 || c.confirmEnd ? BG : INK,
                    Painter.CENTER, true);
        }
    }
}
