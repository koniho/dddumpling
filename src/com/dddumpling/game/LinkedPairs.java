package com.dddumpling.game;

/** Two linked pairs are scheduled in each ordinary wave from stage 16 onward. */
final class LinkedPairs {
    static final int FIRST_STAGE = 16;
    static final float WINDOW = 0.200f;
    private LinkedPairs() {}

    static boolean due(GameCore c) {
        if (c.stage < FIRST_STAGE || c.boss.active() || Cave.active(c)) return false;
        if (c.powerActive()) return c.mode != Power.MULTI && c.powerSpawnedEnemies%4 == 0;
        return c.spawnedThisStage == 0 || c.spawnedThisStage == 3;
    }

    static boolean spawn(GameCore c, Layout L) {
        if (!due(c) || c.liveEnemies() + 2 > c.crowdCap()) return false;
        GameCore.Enemy a = member(c, L, 0), b = member(c, L, 1);
        boolean full = c.playRosterFull();
        int half = Roster.count(full) / 2;
        a.word[0] = Roster.at(full, c.rnd.nextInt(half));
        b.word[0] = Roster.at(full, half + c.rnd.nextInt(half));
        a.link = b;
        b.link = a;
        a.stageMate=b; b.stageMate=a;
        // Equal velocity keeps the two keys side by side throughout their descent.
        b.speed = a.speed;
        if (!EnemyEntry.clear(a, c, L) || !EnemyEntry.clear(b, c, L)) return false;
        c.enemies.add(a);
        c.enemies.add(b);
        if (c.powerActive()) c.powerSpawnedEnemies += 2;
        else c.spawnedThisStage++; // One stage enemy, with two physical characters.
        return true;
    }

    private static GameCore.Enemy member(GameCore c, Layout L, int row) {
        GameCore.Enemy e = new GameCore.Enemy();
        Words.fill(e, 1, 0f, c.rnd, c.playRosterFull());
        e.baseX = (L.playLeft + L.playRight) / 2f + (row == 0 ? -1 : 1) * L.enemyR * 1.85f;
        e.y = -L.enemyR * 2.2f;
        e.speed = (L.dangerY - e.y) / c.travelSeconds();
        return e;
    }

    /** Intercepts the first clear; only a completed pair awards word-clear credit. */
    static boolean cleared(GameCore c, GameCore.Enemy e, Layout L) {
        GameCore.Enemy other = e.link;
        if (other == null) return false;
        if (e.linkWaiting) return true; // Duplicate/in-flight impacts cannot restart the clock.
        if (other.linkWaiting) {
            c.combo += 2;
            c.maxCombo = Math.max(c.maxCombo, c.combo);
            float ex = c.enemyCentreX(e), ox = c.enemyCentreX(other);
            e.linkReleaseDir = ex < ox ? -1f : 1f;
            other.linkReleaseDir = -e.linkReleaseDir;
            e.linkReleaseX = other.linkReleaseX = (ex+ox)*0.5f;
            e.linkReleaseY = other.linkReleaseY = (e.y+other.y)*0.5f;
            e.baseX = ex; other.baseX = ox;
            unlink(e);
            c.destroyWord(other, c.enemyCentreX(other), other.y, L, false);
            return false; // Caller credits the second word normally.
        }
        e.linkStrain = other.linkStrain = 1f;
        e.linkWaiting = true;
        e.linkLeft = c.kidsRun ? .6f : WINDOW;
        e.pos = e.word.length;
        e.done = 0;
        e.dying = false;
        e.attacking = false;
        e.warn = 0f;
        if (c.target == e) c.target = null;
        return true;
    }

    static void update(GameCore c, float dt) {
        for (GameCore.Enemy e : c.enemies) {
            e.linkStrain = Math.max(0f, e.linkStrain - dt * 2.4f);
            float flex = e.link == null ? 0f : Math.min(1f,e.linkStrain/0.18f);
            e.linkFlex += (flex-e.linkFlex)*(1f-(float)Math.exp(-18f*dt));
            if (!e.linkWaiting) continue;
            e.linkLeft -= dt;
            // Include the 200ms boundary, allowing only float-rounding tolerance.
            if (e.linkLeft >= -0.000001f) continue;
            reset(e);
        }
    }

    private static void reset(GameCore.Enemy e) {
        e.linkWaiting = false;
        e.linkLeft = 0f;
        e.pos = e.done = 0;
        e.dying = false;
        e.linkStrain = 1f;
        if (e.link != null) e.link.linkStrain = 1f;
        for (int i = 0; i < e.word.length; i++) {
            e.gone[i] = false;
            e.goneT[i] = 0f;
        }
    }

    static void unlink(GameCore.Enemy e) {
        GameCore.Enemy other = e.link;
        e.linkStrain = e.linkFlex = 0f;
        e.link = null;
        e.linkWaiting = false;
        e.linkLeft = 0f;
        if (other != null) {
            other.linkStrain = other.linkFlex = 0f;
            other.link = null;
            other.linkWaiting = false;
            other.linkLeft = 0f;
        }
    }

    /** A breached partner cannot leave a cleared word waiting forever. */
    static void breached(GameCore c, GameCore.Enemy e, Layout L) {
        GameCore.Enemy other = e.link;
        boolean waiting = other != null && other.linkWaiting;
        unlink(e);
        if (waiting) reset(other); // An incomplete chord earns no clear, even on a breach.
    }

    /** Keep the bond, but a power transition starts a fresh input window. */
    static void preparePower(GameCore c) {
        for (GameCore.Enemy e : c.enemies) {
            if (e.link == null) continue;
            if (e.linkWaiting) reset(e);
            e.linkButton = -1;
        }
    }

    /** Frenzies release the link, so every existing power remains a clean board-clearing reward. */
    static void release(GameCore c, Layout L) {
        for (GameCore.Enemy e : c.enemies) {
            if (e.link == null) continue;
            GameCore.Enemy other = e.link;
            boolean a = e.linkWaiting, b = other.linkWaiting;
            unlink(e);
            if (a) c.destroyWord(e, c.enemyCentreX(e), e.y, L);
            if (b) c.destroyWord(other, c.enemyCentreX(other), other.y, L);
        }
    }
}
