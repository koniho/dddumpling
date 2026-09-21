package com.dddumpling.game;

/** Gather, charge, then a deterministic radial burst; no falling bodies or gameplay RNG. */
final class DivideDeath extends Draw {
    static final float GATHER = 0.9f, SHAKE = 1f;
    static final float BURST_AT = GATHER + SHAKE;
    static final int SHARDS = 360;
    private static final int[] COLORS = {0xFF6432A0, 0xFF8147C9, 0xFFA264E8,
            0xFFC48BFA, 0xFFE0BAFF, 0xFF9251B5};

    static float elapsed(Boss b) { return b.leaveProgress() * Boss.LEAVE; }
    static float charge(float time) { return Math.max(0f, Math.min(1f, (time - GATHER) / SHAKE)); }
    static float shakeAmplitude(float time, Layout L) {
        float q = charge(time);
        return time >= BURST_AT ? 0f : Boss.bodyR(L) * .22f * q * q;
    }
    static boolean bursting(Boss b) { return elapsed(b) >= BURST_AT; }

    static void pose(Boss b, int node, int ordinal, int count, float dt, Layout L) {
        float t = elapsed(b), angle = -Softbody.TAU * .25f + Softbody.TAU * ordinal / count;
        float radius = Boss.bodyR(L) * 1.35f;
        float x = (L.playLeft + L.playRight) * .5f + (float) Math.cos(angle) * radius;
        float y = (L.playTop + L.dangerY) * .5f + (float) Math.sin(angle) * radius;
        if (t < GATHER) {
            float brake = Math.max(0f, 1f - dt * 12f);
            b.divideVX[node] *= brake; b.divideVY[node] *= brake;
            float blend = Math.min(1f, dt * (6f + 3f / Math.max(.01f, GATHER - t)));
            b.divideX[node] += (x - b.divideX[node]) * blend;
            b.divideY[node] += (y - b.divideY[node]) * blend;
        } else {
            float amplitude = shakeAmplitude(t, L), q = charge(t);
            float phase = (t - GATHER) * (48f + 24f * q) + node * 2.39996f;
            b.divideVX[node] = b.divideVY[node] = 0f;
            b.divideX[node] = x + amplitude * (float) Math.sin(phase);
            b.divideY[node] = y + amplitude * (float) Math.cos(phase * 1.17f);
        }
    }

    static float angle(int i) { return i * 2.3999632f; }
    static float variation(int i) { return ((i * 73 + 19) % SHARDS) / (float) (SHARDS - 1); }
    static float travel(int i, float t, Layout L) {
        return Math.min(L.w, L.h) * (.32f + .80f * variation(i)) * t;
    }

    static void draw(Painter p, Layout L, Boss b, float fade) {
        float t = elapsed(b), q = charge(t);
        float x = (L.playLeft + L.playRight) * .5f, y = (L.playTop + L.dangerY) * .5f;
        if (t < BURST_AT) {
            if (q > 0f) p.fillCircle(x, y, Boss.bodyR(L) * (.3f + q * .55f),
                    Glyph.withAlpha(0xFFE0BAFF, (int) (100 * q * q * fade)));
            return;
        }
        float seconds = t - BURST_AT, progress = seconds / (Boss.LEAVE - BURST_AT);
        float light = Math.max(0f, 1f - seconds / .32f);
        p.fillCircle(x, y, Boss.bodyR(L) * (1f + seconds * 4f),
                Glyph.withAlpha(0xFFEBD5FF, (int) (180 * light * fade)));
        for (int i = 0; i < SHARDS; i++) {
            float a = angle(i), distance = travel(i, seconds, L);
            float radius = L.unit * (.10f + .14f * variation((i + 41) % SHARDS));
            float alpha = fade * Math.min(1f, (1f - progress) * 2.5f);
            cube(p, x + (float) Math.cos(a) * distance, y + (float) Math.sin(a) * distance,
                    radius, a + seconds * (3f + variation(i) * 6f), COLORS[i % COLORS.length], alpha);
        }
    }

    private static void cube(Painter p, float x, float y, float r, float angle, int color, float fade) {
        float cs = (float) Math.cos(angle), sn = (float) Math.sin(angle);
        float[] face = new float[8];
        for (int i = 0; i < 4; i++) {
            float dx = (i == 0 || i == 3 ? -1f : 1f) * r;
            float dy = (i < 2 ? -1f : 1f) * r;
            face[i * 2] = x + dx * cs - dy * sn;
            face[i * 2 + 1] = y + dx * sn + dy * cs;
        }
        float dx = r * .6f, dy = -r * .5f;
        int alpha = (int) (255 * fade);
        p.fillPoly(new float[] {face[0], face[1], face[2], face[3], face[2]+dx, face[3]+dy,
                face[0]+dx, face[1]+dy}, Glyph.withAlpha(Glyph.mix(color, 0xFFFFFFFF, .4f), alpha));
        p.fillPoly(new float[] {face[2], face[3], face[4], face[5], face[4]+dx, face[5]+dy,
                face[2]+dx, face[3]+dy}, Glyph.withAlpha(Glyph.mix(color, INK, .25f), alpha));
        p.fillPoly(face, Glyph.withAlpha(color, alpha));
    }
}
