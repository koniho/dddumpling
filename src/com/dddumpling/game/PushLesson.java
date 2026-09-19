package com.dddumpling.game;

/** The first last-life threat waits for a real desperation swipe. */
final class PushLesson extends Draw {
    boolean seen, active, ownsTouch, armed;
    float clock, startX, startY;

    // Stop just before the tile reaches the line; the lunge remains a fallback for crowded hits.
    static float triggerY(Layout L) { return L.dangerY - L.enemyR * 1.4f; }
    private static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }
    static float swipeProgress(float clock) {
        float t = clamp01((clock % 1.8f - .25f) / .9f);
        return t * t * (3f - 2f * t);
    }
    static int barColor(float clock) {
        float pulse = .5f - .5f * (float) Math.cos(clock * Math.PI * 2 / 1.2f);
        return Glyph.mix(0xFF805626, GOLD, pulse);
    }

    void cancelTouch() { armed = ownsTouch = false; }
    void reset() { active = false; clock = 0; cancelTouch(); }

    boolean update(GameCore c, float dt, Layout L) {
        if (c.state != GameCore.PLAY) { reset(); return false; }
        if (!active && !seen && c.lives == 1 && !c.pushUsed && !c.settingsOpen
                && !c.pendingBonus && !c.boss.active() && !Cave.active(c)) {
            for (GameCore.Enemy e : c.enemies) {
                if (e.destroyed || e.dying || e.linkWaiting || e.slideT > 0) continue;
                if (e.attacking || e.y >= triggerY(L)) {
                    Pause.release(c);
                    active = true;
                    c.warnLevel = Math.max(.5f, c.warnLevel);
                    break;
                }
            }
        }
        if (active) clock += dt;
        return active;
    }

    // Native action values are shared by Android and IOSTouch. A second finger cancels.
    boolean touch(GameCore c, Layout L, int action, float x, float y) {
        if (action == 0) {
            ownsTouch = true;
            armed = active && x >= L.playLeft && x <= L.playRight
                    && y >= L.dangerY - L.enemyR * .35f && y <= L.deckTop;
            startX = x; startY = y;
        } else if (action == 5 || action == 6 || action == 3) {
            cancelTouch();
        } else if ((action == 2 || action == 1) && armed) {
            float rise = startY - y;
            if (rise >= L.enemyR * 1.6f && rise > Math.abs(x - startX)) {
                armed = false;
                boolean fired = c.swipeUp(L);
                if (action == 1) cancelTouch();
                return fired;
            }
        }
        if (action == 1) cancelTouch();
        return false;
    }

    static void draw(Painter p, GameCore c, Layout L) {
        PushLesson lesson = c.pushLesson;
        if (!lesson.active) return;
        float s = Pause.scale(L), x = L.w * .5f;
        float y = L.playTop + (L.dangerY - L.playTop) * .36f;
        p.fillRect(0, 0, L.w, L.h, 0xCC100D20);
        p.text("LAST LIFE!", x, y, type(s * .95f), GOLD, Painter.CENTER, true);
        p.text("Swipe up from the bar", x, y + type(s * 1.5f),
                type(s * .58f), INK, Painter.CENTER, true);
        p.text("to push danger back.", x, y + type(s * 2.5f),
                type(s * .54f), INK, Painter.CENTER, false);
        p.text("Once per stage", x, y + type(s * 3.6f),
                type(s * .44f), INK_DIM, Painter.CENTER, false);
        p.fillRect(L.playLeft, L.dangerY, L.playRight, L.deckTop, barColor(lesson.clock));
        float base = (L.dangerY + L.deckTop) * .5f;
        float tip = base - L.enemyR * 2.7f;
        p.polyline(new float[] {x, base, x, tip}, GOLD, s * .15f);
        p.polyline(new float[] {x-s*.6f, tip+s*.65f, x, tip, x+s*.6f, tip+s*.65f},
                GOLD, s*.15f);
        float phase = lesson.clock % 1.8f;
        float fade = Math.min(clamp01(phase / .15f),
                clamp01((1.65f - phase) / .3f));
        Renderer.touchHint(p, x, base - L.enemyR * 2.7f * swipeProgress(lesson.clock),
                L.enemyR * 1.05f, (float) Math.PI * .5f, fade, lesson.clock);
    }
}
