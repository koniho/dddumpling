package com.dddumpling.game;

/** The first last-life threat waits for a real desperation swipe. */
final class PushLesson extends Draw {
    boolean seen, active, ownsTouch, armed;
    float clock, startX, startY;

    void cancelTouch() { armed = ownsTouch = false; }
    void reset() { active = false; clock = 0; cancelTouch(); }

    boolean update(GameCore c, float dt, Layout L) {
        if (c.state != GameCore.PLAY) { reset(); return false; }
        if (!active && !seen && c.lives == 1 && !c.pushUsed && !c.settingsOpen
                && !c.pendingBonus && !c.boss.active() && !Cave.active(c)) {
            float band = (L.dangerY - L.playTop) * GameCore.WARN_BAND;
            for (GameCore.Enemy e : c.enemies) {
                if (e.destroyed || e.dying || e.linkWaiting || e.slideT > 0) continue;
                if (e.attacking || e.y >= L.dangerY - band * .5f) {
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
        p.fillRect(L.playLeft, L.dangerY, L.playRight, L.deckTop, 0xAA806026);
        float base = (L.dangerY + L.deckTop) * .5f;
        float tip = base - L.enemyR * 2.7f;
        p.polyline(new float[] {x, base, x, tip}, GOLD, s * .15f);
        p.polyline(new float[] {x-s*.6f, tip+s*.65f, x, tip, x+s*.6f, tip+s*.65f},
                GOLD, s*.15f);
        float t = (lesson.clock % 1.5f) / 1.5f;
        p.fillCircle(x, base - L.enemyR * 2.7f * t, s * .3f, INK);
    }
}
