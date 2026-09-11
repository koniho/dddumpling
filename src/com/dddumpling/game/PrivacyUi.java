package com.dddumpling.game;

/** Public policy access stays available in both builds, outside the developer panel. */
final class PrivacyUi extends Draw {
    static final String URL = "https://koniho.github.io/dddumpling-privacy/";
    static boolean visible(GameCore c) {
        return c.state == GameCore.TITLE && !c.starting() && !c.caseOpen && !c.storyOpen();
    }
    static boolean hit(GameCore c, Layout L, float x, float y) {
        return visible(c) && x >= L.w - 7f * L.unit && x <= L.w
                && y >= L.dangerY - 3f * L.unit && y <= L.dangerY - L.unit;
    }
    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        p.text("PRIVACY", L.w - L.unit, L.dangerY - 1.7f * L.unit,
                type(L.unit * 0.5f), INK_DIM, Painter.RIGHT, false);
    }
}
