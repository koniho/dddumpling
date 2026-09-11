package com.dddumpling.game;

/** Session-only starting-land selection; boss friends are the persistent unlocks. */
final class LandPicker extends Draw {
    private LandPicker() {}
    static boolean unlocked(GameCore c, int land) {
        return land == 0 || land > 0 && land < Lands.COUNT
                && Collect.has(c.collected, Collect.BOSS_FIRST + land - 1);
    }
    static int count(GameCore c) {
        int n = 0;
        for (int land = 0; land < Lands.COUNT; land++) if (unlocked(c, land)) n++;
        return n;
    }
    static boolean visible(GameCore c) {
        return c.state == GameCore.TITLE && count(c) > 1 && !c.caseOpen && c.caseFade < 0.01f
                && !c.storyOpen() && !c.starting() && !c.settingsOpen && c.rosterSceneT <= 0f;
    }
    static float cardY(Layout L) { return L.h * 0.705f; }
    static int slot(GameCore c, int land) {
        int n = 0;
        for (int i = 0; i < land; i++) if (unlocked(c, i)) n++;
        return n;
    }
    static float spacing(GameCore c, Layout L) { return L.keyR * c.keyScale() * 0.95f; }
    static float iconRadius(GameCore c, Layout L) { return L.keyR * c.keyScale() * 0.72f; }
    static float cardX(GameCore c, Layout L, int land) {
        return L.w * 0.5f + (slot(c, land) - slot(c, c.landChoice) + c.landPickerSlide) * spacing(c, L);
    }
    static void select(GameCore c, int land) {
        if (!visible(c) || !unlocked(c, land) || c.landChoice == land) return;
        c.landPickerSlide += slot(c, land) - slot(c, c.landChoice);
        c.landChoice = land;
        c.best = c.landBests[land];
    }
    static void step(GameCore c, int direction) {
        for (int land = c.landChoice + direction; land >= 0 && land < Lands.COUNT; land += direction)
            if (unlocked(c, land)) { select(c, land); return; }
    }
    static boolean down(GameCore c, Layout L, float x, float y) {
        if (!visible(c)) return false;
        if (Math.abs(y - cardY(L)) > L.h * 0.05f) return false;
        c.landPickerDragging = true; c.landPickerMoved = false; c.landPickerX = x;
        return true;
    }
    static void move(GameCore c, Layout L, float x) {
        if (!c.landPickerDragging || !visible(c)) return;
        float dx = x - c.landPickerX;
        if (Math.abs(dx) < spacing(c, L) * 0.65f) return;
        step(c, dx < 0 ? 1 : -1);
        c.landPickerX = x; c.landPickerMoved = true;
    }
    static void up(GameCore c, Layout L, float x, float y) {
        if (c.landPickerDragging && !c.landPickerMoved && visible(c)
                && Math.abs(y - cardY(L)) < L.h * 0.05f) {
            int nearest = -1;
            float distance = spacing(c, L) * 0.65f;
            for (int land = 0; land < Lands.COUNT; land++) {
                float dx = Math.abs(x - cardX(c, L, land));
                if (unlocked(c, land) && dx < distance) { nearest = land; distance = dx; }
            }
            if (nearest >= 0) select(c, nearest);
        }
        c.landPickerDragging = false;
    }
    static void recordBest(GameCore c) {
        int land = c.runStartLand;
        c.best = Math.max(c.best, c.score);
        c.landBests[land] = Math.max(c.landBests[land], c.best);
        if (c.store != null) c.store.saveLandBest(land, c.landBests[land]);
    }
    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        float cy = cardY(L);
        p.save(); p.clipRect(0, cy - L.h * 0.063f, L.w, cy + L.h * 0.063f);
        // Back to front: the focused emblem covers the inner edges of its neighbours.
        for (int distance = Lands.COUNT - 1; distance >= 0; distance--)
        for (int land = 0; land < Lands.COUNT; land++) {
            if (!unlocked(c, land) || Math.abs(slot(c, land) - slot(c, c.landChoice)) != distance) continue;
            float x = cardX(c, L, land);
            float stepsAway = Math.abs(x - L.w * 0.5f) / spacing(c, L);
            float focus = Math.max(0f, 1f - stepsAway);
            float r = iconRadius(c, L) * (0.72f + focus * 0.28f);
            float y = cy + r * (land == 2 ? 0.4f : land == 3 ? -0.3f : 0f);
            // The neighbouring emblems are blurred silhouettes, becoming clear as they centre.
            float falloff = (float)Math.pow(0.45f, Math.max(0f, stepsAway - 1f));
            int haze = (int)(80 * (1f - focus) * falloff);
            Lands.prop(p, land, x, y, r * 1.20f, haze / 8, c.clock, true);
            Lands.prop(p, land, x, y, r * 1.12f, haze / 5, c.clock, true);
            Lands.prop(p, land, x, y, r * 1.05f, haze / 3, c.clock, true);
            Lands.prop(p, land, x, y, r, haze / 2, c.clock, true);
            if (focus > 0f) Lands.prop(p, land, x, y, r, (int)(235 * focus), c.clock);
        }
        p.restore();
    }
}
