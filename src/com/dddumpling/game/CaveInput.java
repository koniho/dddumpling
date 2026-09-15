package com.dddumpling.game;

/** Platform-free pointer ownership; a key finger can never become a steering drag. */
final class CaveInput {
    int pointer = -1;
    float offset;
    void release() { pointer = -1; }
    boolean down(GameCore c, Layout L, int id, float x, float y) {
        if (!Cave.active(c) || c.paused || c.settingsOpen || c.pendingBonus) return false;
        if (c.keyAt(x,y,L) >= 0 || y < L.playTop || y >= L.deckTop) return false;
        if (c.cave.phase != Cave.ROCKS) return c.cave.tap(c,L,x,y);
        if (pointer < 0) { pointer=id; offset=c.cave.traps.x*L.w-x; }
        return true;
    }
    boolean move(GameCore c, Layout L, int id, float x) {
        if (id != pointer || pointer < 0) return false;
        if (!Cave.active(c) || c.paused || c.settingsOpen || c.cave.phase != Cave.ROCKS) {
            release(); return false;
        }
        c.cave.traps.drag((x+offset)/L.w);
        return true;
    }
    void up(int id) { if (id == pointer) release(); }
}
