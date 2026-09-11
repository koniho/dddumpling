package com.dddumpling.game;

/**
 * Boss celebrations during the player's death transition. Gameplay receives a clean Boss when the
 * run ends, while this renderer draws the retained combat object as a visual-only snapshot.
 */
final class BossVictory extends Draw {
    private BossVictory() {}

    static void draw(Painter p, GameCore c, Layout L) {
        int kind = c.bossVictoryKind;
        if (kind < 0 || c.bossVictory == null || !c.dying()) return;
        float at = c.deathProgress();
        float fade = Math.min(1f, at * 7f) * Math.min(1f, (1f - at) * 6f);
        Boss b = c.bossVictory;
        float r = Boss.bodyR(L);
        float cx = b.body.centreX();
        float cy = b.body.centreY();
        float beat = c.clock * 7f;

        confetti(p, cx, cy, r, beat, fade, kind, false);
        if (kind == Boss.MUSHROOM) sporeStorm(p, c, L, b, fade, false);
        p.save();
        p.translate(tauntX(kind, beat, r), tauntY(kind, beat, r));
        BossScreen.body(p, c, L, b, fade);
        p.restore();
        if (kind == Boss.MUSHROOM) sporeStorm(p, c, L, b, fade, true);
        confetti(p, cx, cy, r, beat, fade, kind, true);
    }

    /** Side-to-side swagger; each silhouette gets a slightly different taunting rhythm. */
    static float tauntX(int kind, float beat, float r) {
        float rate = kind == Boss.OCTOPUS ? 0.82f
                : 0.94f;
        float reach = kind == Boss.OCTOPUS ? 0.24f : 0.10f;
        return (float) Math.sin(beat * rate + kind * 0.73f) * r * reach;
    }

    /** A cocky hop/stomp layered with the swagger, without resizing the retained combat body. */
    static float tauntY(int kind, float beat, float r) {
        float rate = kind == Boss.SLIME ? 0.72f : 1.05f;
        float lift = kind == Boss.SLIME ? 0.24f : 0.11f;
        return -Math.abs((float) Math.sin(beat * rate + kind * 0.41f)) * r * lift;
    }

    /** Draw-only arm poses keep the victory snapshot and combat simulation untouched. */
    static void waveArm(float[] points, Boss b, Layout L, int arm, float clock, float progress) {
        float r = Boss.bodyR(L), side = arm < Boss.OCTO_ARMS / 2 ? -1f : 1f;
        float rootX = b.octoPlaced ? b.octoX[arm][0] : b.body.centreX() + side * r * 0.25f;
        float rootY = b.octoPlaced ? b.octoY[arm][0] : b.body.centreY() + r * 0.22f;
        float ease = Math.min(1f, progress * 7f);
        ease = ease * ease * (3f - 2f * ease);
        if (!b.octoPlaced) ease = 1f;
        for (int n = 0; n < Boss.OCTO_NODES; n++) {
            float u = n / (float)(Boss.OCTO_NODES - 1);
            float beat = clock * 5.5f + arm * 0.8f;
            float wave = (float)Math.sin(beat + u * 3f);
            float reach = 1.65f + Math.abs(arm - 3.5f) * 0.20f;
            float x = rootX + side * r * (reach * u + (float)Math.sin(u * Math.PI) * 0.45f)
                    + wave * r * 0.36f * u * u;
            float y = rootY + r * (0.8f * u - (1.65f + 0.65f * (float)Math.sin(beat)) * u * u)
                    + (float)Math.sin(beat + u * 5f) * r * 0.20f * u;
            points[n * 2] += (x - points[n * 2]) * ease;
            points[n * 2 + 1] += (y - points[n * 2 + 1]) * ease;
        }
    }

    static float mushroomShake(float elapsed, float r) {
        return (float)Math.sin(elapsed * Softbody.TAU / 0.70f) * r * 0.72f
                * Math.min(1f, elapsed * 6f);
    }

    /** Reversal bursts are decorative: they never enter the projectile simulation. */
    private static void sporeStorm(Painter p, GameCore c, Layout L, Boss b, float fade, boolean front) {
        float elapsed = c.deathDuration() - c.deathT, r = Boss.bodyR(L);
        int latest = (int)Math.floor((elapsed - 0.175f) / 0.35f);
        for (int burst = Math.max(0, latest - 4); burst <= latest; burst++) {
            float born = 0.175f + burst * 0.35f, age = elapsed - born;
            if (age < 0f || age > 1.5f) continue;
            float beat = (c.clock - age) * 7f;
            float x = b.body.centreX() + b.mushroomCapDX + mushroomShake(born, r)
                    + tauntX(Boss.MUSHROOM, beat, r);
            float y = b.body.centreY() + b.body.radiusY() * 0.87f + b.mushroomCapDY
                    + tauntY(Boss.MUSHROOM, beat, r);
            for (int i = 0; i < 24; i++) {
                if ((i % 3 != 0) != front) continue;
                float seed = i * 1.91f + burst * 2.37f;
                float spread = (float)Math.sin(seed);
                float xx = x + spread * r * (1.25f + age * 2.6f);
                float yy = y + r * (age * (0.3f + (i % 5) * 0.23f) + age * age * 2f)
                        + (float)Math.sin(seed + age * 4f) * r * 0.12f;
                int alpha = (int)(fade * Math.min(1f, (1.5f - age) * 3f) * (front ? 225 : 130));
                float size = r * (0.025f + (i % 4) * 0.012f);
                p.fillCircle(xx, yy, size * 2.2f, Glyph.withAlpha(0xFFFFD9BA, alpha / 7));
                p.fillCircle(xx, yy, size, Glyph.withAlpha(i % 2 == 0 ? 0xFFFFEBD0 : 0xFFF3B7A9, alpha));
            }
        }
    }

    private static void confetti(Painter p, float x, float y, float r, float t, float fade,
            int kind, boolean front) {
        for (int i = 0; i < 36; i++) {
            if ((i % 3 != 0) != front) continue;
            float phase = t * (0.16f + (i % 3) * 0.025f) + i * 1.91f + kind;
            float xx = x + (float) Math.sin(phase) * r * (1.90f + (i % 4) * 0.25f);
            float fall = (t * 0.10f + i * 0.137f) % 1f;
            float yy = y - r * 1.80f + fall * r * 3.50f;
            int color = i % 3 == 0 ? GOLD : i % 3 == 1 ? ROSE : 0xFF8FD9A0;
            p.fillPoly(Glyph.hex(xx, yy, r * (front ? 0.09f : 0.07f)),
                    Glyph.withAlpha(color, (int) ((front ? 205 : 140) * fade)));
        }
    }
}
