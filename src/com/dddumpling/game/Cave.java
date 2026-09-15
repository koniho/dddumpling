package com.dddumpling.game;

/** Route distance is in view widths; encounters stop travel, never the camera's position. */
final class Cave {
    static final int LAND = 4, WALK = 0, FORK = 1, SHADOW = 2, FIGHT = 3,
            ROCKS = 4, SAND = 5, EXIT = 6, CHOOSE = 7;
    static final float LENGTH = 10.6f, WALK_SPEED = .36f, FORK_WAIT = 3f,
            LESSON = 2.4f, REVEAL = .25f, APPROACH = 4.5f;
    static final float[] FORKS = CaveRoute.FORKS;
    final int[] routes = new int[3];
    final boolean[] met = new boolean[3], hearts = new boolean[3];
    final int[] response = new int[5];
    final CaveDumpling walker = new CaveDumpling();
    final CaveSelection selection = new CaveSelection();
    final CaveInput input = new CaveInput();
    final CaveTraps traps = new CaveTraps();
    int phase, fork, responsePos, responseSize, seed;
    float z, cameraZ, aim, timer, reveal, enemyX, enemyZ, enemyStartZ, pulse, exitTime, returnX, returnTime;
    boolean running, lessonSeen, openingMet;

    static boolean stage(int n) { return BuildFlags.DEVELOPER && Lands.forStage(n) == LAND; }
    static boolean active(GameCore c) { return BuildFlags.DEVELOPER && c.state == GameCore.PLAY && c.cave.running; }

    void begin(GameCore c) {
        running = stage(c.stage); phase = WALK; z = cameraZ = aim = timer = reveal = pulse = 0f;
        fork = responsePos = 0; openingMet = false; lessonSeen = c.stage != 21;
        seed = c.stage; exitTime = returnTime = 0f;
        for (int i = 0; i < routes.length; i++) { routes[i] = 0; met[i] = hearts[i] = false; }
        input.release(); traps.reset(); walker.reset(); selection.reset();
        if (running && c.caveChoice < 0) phase = CHOOSE;
    }

    void leave() { running = false; input.release(); traps.reset(); }
    static float centre(float at) { return CaveRoute.centre(at); }
    static float branchX(int junction, int side, float at) {
        return CaveRoute.x(junction, side, at);
    }
    float pathX(float at) {
        for (int i = 0; i < 3; i++)
            if (at >= FORKS[i] && at <= FORKS[i] + 2f) return branchX(i, routes[i], at);
        return centre(at);
    }
    float playerX() {
        if (phase == ROCKS) return traps.x;
        float t = Math.min(1f, returnTime / .45f);
        return pathX(z) + (returnX - pathX(z)) * t * t * (3f - 2f * t);
    }
    static float scale(Layout L) { return L.w * .78f; }
    static float anchor(Layout L) { return L.playTop + (L.deckTop - L.playTop) * .73f; }
    float screenY(float at, Layout L) { return anchor(L) + (cameraZ - at) * scale(L); }
    float playerY(Layout L) { return screenY(z, L); }
    int nearestBranch() { return aim < 0f ? -1 : 1; }
    int event(int junction, int side) {
        return CaveRoute.event(junction, side);
    }
    boolean hasHeart(int junction, int side) { return CaveRoute.heart(junction, side); }

    boolean tap(GameCore c, Layout L, float x, float y) {
        if (!active(c) || c.paused || c.settingsOpen || y < L.playTop || y >= L.deckTop) return false;
        if (phase == CHOOSE) return selection.tap(c,L,x,y);
        if (phase == ROCKS) { traps.drag(x / L.w); return true; }
        aim = (float)Math.atan2(x / L.w - playerX(), (playerY(L) - y) / scale(L));
        if (phase == FORK && Math.abs(aim) > .15f && Math.abs(aim) < 1.5f) choose(c, nearestBranch());
        return true;
    }
    void choose(GameCore c, int side) {
        routes[fork] = side < 0 ? -1 : 1; lessonSeen = true; phase = WALK;
        if (c.sound != null) c.sound.landShuffle();
    }
    boolean illuminated(float x, float at) {
        float dx = x - playerX(), dz = at - z;
        float angle = (float)Math.atan2(dx, dz);
        float delta = (float)Math.atan2(Math.sin(angle - aim), Math.cos(angle - aim));
        return dx * dx + dz * dz < 2.6f && Math.abs(delta) < .34f;
    }
    int wanted() {
        if (phase == FIGHT && responsePos < responseSize) return response[responsePos];
        if (phase == SAND && traps.age >= CaveTraps.WARNING) return traps.wanted();
        return -1;
    }
    boolean press(GameCore c, int g, Layout L) {
        if (!active(c)) return false;
        if (phase == CHOOSE) return false;
        if (phase == SAND) return traps.press(c, g);
        if (phase != FIGHT) return false;
        if (g != wanted()) {
            c.keyBad[g] = 1f; c.misses++; c.missesThisStage++; c.combo = 0;
            if (c.sound != null) c.sound.wrong();
            return false;
        }
        c.hits++; c.combo++; c.maxCombo = Math.max(c.maxCombo, c.combo); c.score += 10;
        pulse = 1f; responsePos++;
        if (c.sound != null) c.sound.squish(g, 1);
        if (responsePos == responseSize) {
            c.squishes++; c.score += 40;
            Fx.explode(c, c.rnd, enemyX * L.w, screenY(enemyZ, L), L.enemyR, 12, Glyph.COLOR[g]);
            phase = WALK;
        }
        return true;
    }
    void encounter(GameCore c, int kind) {
        float px = playerX();
        phase = kind; timer = reveal = 0f;
        if (kind == SHADOW) {
            enemyX = pathX(z + .3f); enemyZ = enemyStartZ = z + .85f;
            responsePos = 0; responseSize = 3 + (seed - 21) % 3;
            for (int i = 0; i < responseSize; i++)
                response[i] = Roster.at(c.playRosterFull(), (seed + i * 2 + fork) % Roster.count(c.playRosterFull()));
        } else {
            traps.begin(c, kind, px);
            if (c.sound != null) c.sound.wrong();
        }
    }
    void update(GameCore c, float dt, Layout L) {
        float distance = phase == WALK && returnTime <= 0f && !c.pendingBonus ? dt*WALK_SPEED*c.speed : 0f;
        walker.update(dt,distance);
        pulse = Math.max(0f, pulse - dt * 3f);
        returnTime = Math.max(0f, returnTime - dt);
        cameraZ += (z - cameraZ) * Math.min(1f, dt * 5f);
        if (c.pendingBonus) return;
        switch (phase) {
            case CHOOSE:
                selection.update(c,dt);
                break;
            case FORK:
                timer += dt;
                if (timer >= FORK_WAIT + (lessonSeen ? 0f : LESSON)) choose(c, nearestBranch());
                break;
            case SHADOW:
                reveal = illuminated(enemyX, enemyZ) ? reveal + dt : 0f;
                if (reveal >= REVEAL) {
                    phase = FIGHT; timer = 0f;
                    if (c.sound != null) c.sound.squish(response[0], 1);
                }
                break;
            case FIGHT:
                timer += dt * c.speed;
                enemyZ = z + (enemyStartZ - z) * Math.max(0f, 1f - timer / APPROACH);
                if (timer >= APPROACH) { phase = WALK; c.takeHit(playerX() * L.w, L); }
                break;
            case ROCKS: case SAND:
                traps.update(c, dt, L);
                break;
            case EXIT:
                exitTime += dt;
                if (exitTime >= 1.4f) {
                    c.score += 100; Interlude.beginStageEnd(c);
                }
                break;
            default:
                if (returnTime > 0f) break;
                float next = Math.min(LENGTH, z + dt * WALK_SPEED * c.speed);
                if (!openingMet && next >= .7f) {
                    z = .7f; openingMet = true; encounter(c, SHADOW); break;
                }
                for (int i = 0; i < 3; i++) {
                    if (routes[i] == 0 && next >= FORKS[i]) {
                        z = FORKS[i]; fork = i; phase = FORK; timer = 0f; return;
                    }
                    if (routes[i] != 0 && !met[i] && next >= FORKS[i] + .72f) {
                        z = FORKS[i] + .72f; met[i] = true; fork = i;
                        encounter(c, event(i, routes[i])); return;
                    }
                    if (routes[i] != 0 && !hearts[i] && next >= FORKS[i] + CaveRoute.HEART_OFFSET) {
                        hearts[i] = true;
                        if (hasHeart(i, routes[i])) {
                            c.lives = Math.min(GameCore.START_LIVES, c.lives + 1); pulse = 1f;
                            if (c.sound != null) c.sound.collect(i);
                        }
                    }
                }
                z = next;
                if (z >= LENGTH) { phase = EXIT; if (c.sound != null) c.sound.stageClear(); }
                break;
        }
    }
}
