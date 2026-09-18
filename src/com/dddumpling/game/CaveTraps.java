package com.dddumpling.game;

/** Each trap can charge one life; timings are real seconds, steering has a speed ceiling. */
final class CaveTraps {
    static final float WARNING = 0f, DURATION = 2.8f, FALL = .72f, GAP = .40f,
            MAX_VX = 1.3f, ROCK_R = .065f, PLAYER_R = .032f;
    static final int ROCK_COUNT = 6, ESCAPE_PRESSES = 8;
    final float[] lanes = {.50f, .28f, .72f, .40f, .62f, .50f};
    final boolean[] landed = new boolean[ROCK_COUNT];
    float age, x, targetX;
    int kind, hits, left, right;

    void reset() { age = 0f; hits = 0; kind = -1; }
    void begin(GameCore c, int type, float px) {
        reset(); kind = type; x = targetX = px;
        left = Roster.at(c.playRosterFull(), 0);
        right = Roster.at(c.playRosterFull(), Roster.count(c.playRosterFull()) / 2);
        for (int i = 0; i < ROCK_COUNT; i++) landed[i] = false;
    }
    void drag(float px) { targetX = Math.max(.18f, Math.min(.82f, px)); }
    int wanted() { return hits % 2 == 0 ? left : right; }
    boolean press(GameCore c, int g) {
        if (kind != Cave.SAND || age < WARNING) return false;
        if (g != wanted()) {
            c.keyBad[g] = 1f;
            if (c.sound != null) c.sound.wrong();
            return false;
        }
        hits++; c.cave.pulse = 1f;
        if (c.sound != null) c.sound.squish(g, 1);
        if (hits >= ESCAPE_PRESSES) finish(c, false, null);
        return true;
    }
    float rockX(int i,float progress) {
        float t=Math.max(0,Math.min(1,progress));
        return lanes[i]+(i%3-1)*.19f*(1-t);
    }
    float rockProgress(int i) { return (age - WARNING - i * GAP) / FALL; }
    void update(GameCore c, float dt, Layout L) {
        float before=age;age += dt;
        if(kind==Cave.SAND && (int)(before/.65f)!=(int)(age/.65f))c.cave.effects.cue(c,Sfx.CAVE_SINK,.45f);
        if (kind == Cave.ROCKS) {
            x += Math.max(-MAX_VX * dt, Math.min(MAX_VX * dt, targetX - x));
            for (int i = 0; i < ROCK_COUNT; i++) {
                if (!landed[i] && rockProgress(i) >= 1f) {
                    landed[i] = true;
                    c.cave.effects.impact(c,L,lanes[i]);
                    if (Math.abs(x - lanes[i]) < ROCK_R + PLAYER_R) { finish(c, true, L); return; }
                }
            }
        }
        if (age >= WARNING + DURATION) finish(c, kind == Cave.SAND, L);
    }
    private void finish(GameCore c, boolean hurt, Layout L) {
        if (kind == Cave.ROCKS) { c.cave.returnX=x; c.cave.returnTime=.22f; }
        c.cave.input.release();
        c.cave.phase = Cave.WALK;
        if (hurt) c.takeHit(c.cave.playerX() * L.w, L);
        else { c.cave.effects.rumble=Math.max(c.cave.effects.rumble,.35f);c.cave.effects.feedback=Math.max(1,c.cave.effects.feedback);c.score += 30; if (c.sound != null) c.sound.star(1); }
        kind = -1;
    }
}
