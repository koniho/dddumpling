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

    void cancelTouch() { armed = ownsTouch = false; }
    void reset() { active = false; clock = 0; cancelTouch(); }

    boolean update(GameCore c, float dt, Layout L) {
        if (c.state != GameCore.PLAY) { reset(); return false; }
        if (!active && !seen && c.onboarding.eligible(Onboarding.SKIPPED)
                && c.lives == 1 && !c.pushUsed && !c.settingsOpen
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
        // Leave the real swipe bar uncovered so the lesson teaches its ordinary appearance.
        p.fillRect(0, 0, L.w, L.dangerY, 0xCC100D20);
        p.fillRect(0, L.deckTop, L.w, L.h, 0xCC100D20);
        p.fillRect(0, L.dangerY, L.playLeft, L.deckTop, 0xCC100D20);
        p.fillRect(L.playRight, L.dangerY, L.w, L.deckTop, 0xCC100D20);
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
