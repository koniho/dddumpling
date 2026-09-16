package com.dddumpling.game;

/** Title entry for player settings; the policy link lives inside. */
final class PrivacyUi extends Draw {
    static final String URL = "https://koniho.github.io/dddumpling-privacy/";
    static boolean visible(GameCore c) {
        return c.state == GameCore.TITLE && !c.starting() && !c.caseOpen && !c.storyOpen() && !c.settingsOpen && !c.releaseNotes.open && c.returnFade<=0f;
    }
    static boolean hit(GameCore c, Layout L, float x, float y) {
        return visible(c) && x >= L.w - 7f * L.unit && x <= L.w
                && y >= L.dangerY - 3f * L.unit && y <= L.dangerY - L.unit;
    }
    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        p.text("SETTINGS", L.w - L.unit, L.dangerY - 1.7f * L.unit,
                type(L.unit * 0.5f), INK_DIM, Painter.RIGHT, false);
    }
}
