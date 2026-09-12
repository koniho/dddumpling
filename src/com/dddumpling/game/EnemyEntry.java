package com.dddumpling.game;

/** Side entrances and room for their full rows, including future crossings. */
final class EnemyEntry {
    private EnemyEntry() {}

    static final float ARC_FRACTION = 0.15f;

    static float progress(GameCore.Enemy e, float y, Layout L) {
        float span = Math.max(1f, L.dangerY - L.enemyR - e.pathStartY);
        return Math.max(0f, Math.min(1f, (y - e.pathStartY) / (span * ARC_FRACTION)));
    }

    static float xAt(GameCore.Enemy e, float y, Layout L) {
        if (!e.sideEntry) return e.baseX;
        float u = 1f - progress(e, y, L);
        // Cubic ease-out: fast inward motion, with zero horizontal velocity and acceleration
        // at the join to the vertical lane. Fall speed stays continuous through the turn.
        return e.pathEndX + (e.pathStartX - e.pathEndX) * u * u * u;
    }

    static boolean clear(GameCore.Enemy incoming, GameCore c, Layout L) {
        for (GameCore.Enemy other : c.enemies) {
            if (other == incoming || other.dying || other.destroyed) continue;
            if (!incoming.sideEntry && !other.sideEntry) continue;
            if (conflict(incoming, other, L)) return false;
        }
        return true;
    }

    private static boolean conflict(GameCore.Enemy a, GameCore.Enemy b, Layout L) {
        float gapY = L.enemyR * 2.7f;
        // Both velocities receive the same frenzy multiplier, so nominal fall time suffices.
        float end = Math.min((L.dangerY - a.y) / Math.max(1f, a.speed),
                (L.dangerY - b.y) / Math.max(1f, b.speed));
        float lo = 0f, hi = Math.max(0f, end);
        float dy = a.y - b.y, dv = a.speed - b.speed;
        if (Math.abs(dv) < 1e-4f) {
            if (Math.abs(dy) >= gapY) return false;
        } else {
            float t1 = (-gapY - dy) / dv, t2 = (gapY - dy) / dv;
            lo = Math.max(lo, Math.min(t1, t2));
            hi = Math.min(hi, Math.max(t1, t2));
            if (lo > hi) return false;
        }
        // Monotone arcs let endpoint bounds cover every position between samples. Sway and
        // a little character padding are reserved too; a top spawn must respect the same lane.
        float ax0 = xAt(a, a.y + a.speed * lo, L), ax1 = xAt(a, a.y + a.speed * hi, L);
        float bx0 = xAt(b, b.y + b.speed * lo, L), bx1 = xAt(b, b.y + b.speed * hi, L);
        float gapX = (L.wordWidth(a.word.length) + L.wordWidth(b.word.length)) / 2f
                + a.sway + b.sway + L.enemyR * 0.35f;
        return Math.min(ax0, ax1) < Math.max(bx0, bx1) + gapX
                && Math.max(ax0, ax1) > Math.min(bx0, bx1) - gapX;
    }
}
