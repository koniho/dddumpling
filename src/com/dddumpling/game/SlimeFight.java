package com.dddumpling.game;

/** A short, consequence-free slime toss played from the town meadow. */
final class SlimeFight {
    static final int READY = 0, PLAY = 1, RESULT = 2;
    static final int IGNORE = 0, HANDLED = 1, AGAIN = 2, EXIT = 3;
    static final int ACH_TOSSER = 1, ACH_DODGER = 2, ACH_SPLAT_PALS = 4, ACH_BOUNCER = 8;
    static final float READY_TIME = 1.35f, PLAY_TIME = 24f;
    static final int MAX_GLOBS = 14, MAX_SPLATS = 16;

    final Softbody playerBody = new Softbody(Softbody.NODES, 1081);
    final Softbody rivalBody = new Softbody(Softbody.NODES, 2081);
    final boolean[] globLive = new boolean[MAX_GLOBS];
    final boolean[] globPlayer = new boolean[MAX_GLOBS];
    final float[] globX = new float[MAX_GLOBS], globY = new float[MAX_GLOBS];
    final float[] globVX = new float[MAX_GLOBS], globVY = new float[MAX_GLOBS];
    final float[] splatX = new float[MAX_SPLATS], splatY = new float[MAX_SPLATS];
    final float[] splatAge = new float[MAX_SPLATS];
    final boolean[] splatPlayer = new boolean[MAX_SPLATS];

    int phase, fun, throwsMade, movementFun, dodges, hits, splashed, achievements;
    float clock, phaseTime, timeLeft, playerX, rivalX, rivalDir, rivalSpeed, intent;
    float playerDistance, rivalThrow, feedbackTime;
    String feedback = "";

    int steerPointer = -1, flingPointer = -1;
    boolean sliderGesture, flingGesture, engaged;
    float touchStartX, touchStartY, touchX, touchY, touchStartTime;
    private int randomState, nextGlob, nextSplat;

    SlimeFight() { randomState = 0x51F17E; }

    /** Start a repeatable session. The attraction owns its RNG and never touches the run's RNG. */
    void begin(Layout L) {
        begin(L, 0x51F17E);
    }

    void begin(Layout L, int seed) {
        phase = READY;
        fun = throwsMade = movementFun = dodges = hits = splashed = achievements = 0;
        engaged = false;
        clock = phaseTime = 0f;
        timeLeft = PLAY_TIME;
        intent = playerDistance = 0f;
        playerX = L.w * 0.5f;
        rivalX = L.w * 0.5f;
        rivalDir = 1f;
        rivalThrow = 0.85f;
        feedback = "FLING UP TO TOSS!";
        feedbackTime = READY_TIME;
        randomState = seed == 0 ? 0x51F17E : seed;
        rivalSpeed = L.w * (0.16f + randomUnit() * 0.015f);
        nextGlob = nextSplat = 0;
        for (int i = 0; i < MAX_GLOBS; i++) globLive[i] = false;
        for (int i = 0; i < MAX_SPLATS; i++) splatAge[i] = 0f;
        playerBody.jiggle = rivalBody.jiggle = 1.65f;
        playerBody.reset(playerX, playerY(L), bodyR(L), 1.24f);
        rivalBody.reset(rivalX, rivalY(L), bodyR(L), 1.35f);
        cancelInput();
    }

    void update(float dt, Layout L) {
        if (!(dt > 0f)) return;
        dt = Math.min(dt, 0.05f);
        clock += dt;
        phaseTime += dt;
        feedbackTime = Math.max(0f, feedbackTime - dt);
        for (int i = 0; i < MAX_SPLATS; i++) splatAge[i] = Math.max(0f, splatAge[i] - dt);

        if (phase == READY && phaseTime >= READY_TIME) {
            phase = PLAY;
            phaseTime = 0f;
            feedback = "MAKE SOME FUN!";
            feedbackTime = 0.8f;
        }

        if (phase == PLAY) {
            rivalX += rivalDir * rivalSpeed * dt;
            float rivalEdge = bodyR(L) * 1.65f;
            if (rivalX <= rivalEdge || rivalX >= L.w - rivalEdge) {
                rivalX = Math.max(rivalEdge, Math.min(L.w - rivalEdge, rivalX));
                rivalDir = rivalX <= rivalEdge ? 1f : -1f;
                rivalSpeed = L.w * (0.15f + randomUnit() * 0.035f);
                rivalBody.squash(0.24f);
            }
            rivalThrow -= dt;
            if (rivalThrow <= 0f) {
                enemyThrow(L);
                rivalThrow = 0.82f + randomUnit() * 0.72f;
            }
            updateGlobs(dt, L);
            timeLeft = Math.max(0f, timeLeft - dt);
            updateAchievements();
            if (timeLeft <= 0f) finish();
        }

        playerBody.moveTo(playerX, playerY(L));
        rivalBody.moveTo(rivalX, rivalY(L));
        playerBody.update(dt);
        rivalBody.update(dt);
    }

    private void finish() {
        phase = RESULT;
        phaseTime = 0f;
        intent = 0f;
        cancelInput();
        feedback = fun >= 75 ? "SUPER SILLY!" : fun >= 40 ? "SPLAT-TASTIC!" : "GOOD GOOFY FUN!";
        feedbackTime = Float.POSITIVE_INFINITY;
    }

    /** Android/iOS action numbers: down 0, up 1, move 2, cancel 3. */
    int touch(Layout L, int action, int id, float x, float y) {
        if (action == 3) { cancelInput(); return HANDLED; }
        if (phase == RESULT && action == 0) {
            if (SlimeFightScreen.inAgain(L, x, y)) return AGAIN;
            if (SlimeFightScreen.inExit(L, x, y)) return EXIT;
            return HANDLED;
        }
        if (phase == RESULT) return HANDLED;
        if (action == 0) return down(L, id, x, y);
        if (action == 2) return move(L, id, x, y);
        if (action == 1) return up(L, id, x, y);
        return IGNORE;
    }

    private int down(Layout L, int id, float x, float y) {
        if (StarScreen.inSlider(L, x, y)) {
            if (steerPointer < 0) {
                steerPointer = id;
                sliderGesture = true;
                steerFrom(L, x);
            }
            return HANDLED;
        }
        if (y >= L.playTop && y <= L.deckTop) {
            if (flingPointer < 0) {
                flingPointer = id;
                flingGesture = true;
                touchStartX = touchX = x;
                touchStartY = touchY = y;
                touchStartTime = clock;
            }
            return HANDLED;
        }
        return y >= L.deckTop ? HANDLED : IGNORE;
    }

    private int move(Layout L, int id, float x, float y) {
        boolean handled = false;
        if (id == steerPointer) {
            steerFrom(L, x);
            handled = true;
        }
        if (id == flingPointer) {
            touchX = x;
            touchY = y;
            handled = true;
        }
        return handled ? HANDLED : IGNORE;
    }

    private int up(Layout L, int id, float x, float y) {
        boolean handled = false;
        if (id == flingPointer) {
            touchX = x;
            touchY = y;
            float dx = touchX - touchStartX, dy = touchY - touchStartY;
            float elapsed = Math.max(0.07f, clock - touchStartTime);
            if (phase == PLAY && dy < -L.w * 0.075f && -dy > Math.abs(dx) * 0.55f)
                playerThrow(L, dx / elapsed, dy / elapsed);
            flingPointer = -1;
            flingGesture = false;
            handled = true;
        }
        if (id == steerPointer) {
            steerPointer = -1;
            sliderGesture = false;
            handled = true;
        }
        return handled ? HANDLED : IGNORE;
    }

    void cancelInput() {
        steerPointer = flingPointer = -1;
        sliderGesture = flingGesture = false;
    }

    /** Host steering places the player just as dragging the Star Path control does. */
    void steer(float amount, Layout L) {
        float mid = (StarScreen.sliderLeft(L) + StarScreen.sliderRight(L)) * 0.5f;
        float half = (StarScreen.sliderRight(L) - StarScreen.sliderLeft(L)) * 0.5f;
        steerFrom(L, mid + Math.max(-1f, Math.min(1f, amount)) * half);
    }

    private void steerFrom(Layout L, float x) {
        float low = Math.max(StarScreen.sliderLeft(L), bodyR(L) * 1.35f);
        float high = Math.min(StarScreen.sliderRight(L), L.w - bodyR(L) * 1.35f);
        float next = Math.max(low, Math.min(high, x));
        if (phase == PLAY) {
            if (Math.abs(next - playerX) >= L.w * 0.01f) engaged = true;
            playerDistance += Math.abs(next - playerX);
            float step = L.w * 0.44f;
            while (playerDistance >= step) {
                playerDistance -= step;
                movementFun++;
                award(1, "BOUNCE FUN +1");
                playerBody.squash(0.18f);
            }
        }
        playerX = next;
        float mid = (StarScreen.sliderLeft(L) + StarScreen.sliderRight(L)) * 0.5f;
        float half = (StarScreen.sliderRight(L) - StarScreen.sliderLeft(L)) * 0.5f;
        intent = (playerX - mid) / Math.max(1f, half);
    }

    float sliderKnob(Layout L) {
        return Math.max(StarScreen.sliderLeft(L), Math.min(StarScreen.sliderRight(L), playerX));
    }

    private void playerThrow(Layout L, float gestureVX, float gestureVY) {
        int i = claimGlob();
        engaged = true;
        float cap = L.w * 1.65f;
        globPlayer[i] = true;
        globX[i] = playerX;
        globY[i] = playerY(L) - bodyR(L) * 0.7f;
        globVX[i] = clamp(gestureVX * 0.42f, -L.w * 0.72f, L.w * 0.72f);
        globVY[i] = clamp(gestureVY * 0.50f, -cap, -L.w * 0.74f);
        throwsMade++;
        award(2, "NICE TOSS +2");
        playerBody.shove(-globVX[i], -globVY[i], 1.8f);
    }

    private void enemyThrow(Layout L) {
        int i = claimGlob();
        globPlayer[i] = false;
        globX[i] = rivalX;
        globY[i] = rivalY(L) + bodyR(L) * 0.72f;
        float lead = (playerX - rivalX) * (0.30f + randomUnit() * 0.22f);
        globVX[i] = lead;
        globVY[i] = L.w * (0.58f + randomUnit() * 0.18f);
        rivalBody.squash(0.18f);
    }

    private int claimGlob() {
        int i = nextGlob++ % MAX_GLOBS;
        globLive[i] = true;
        return i;
    }

    private void updateGlobs(float dt, Layout L) {
        float r = globR(L), playerY = playerY(L), rivalY = rivalY(L);
        for (int i = 0; i < MAX_GLOBS; i++) {
            if (!globLive[i]) continue;
            globX[i] += globVX[i] * dt;
            globY[i] += globVY[i] * dt;
            globVX[i] *= 1f - dt * 0.18f;
            if (globPlayer[i]) {
                if (near(globX[i], globY[i], rivalX, rivalY, r + bodyR(L) * 0.88f)) {
                    globLive[i] = false;
                    hits++;
                    award(8, "SPLAT! +8");
                    rivalBody.impulse(globX[i], globY[i], 0.72f);
                    rivalBody.shove(globVX[i], globVY[i], 1.9f);
                    splat(globX[i], globY[i], true);
                } else if (globY[i] < L.playTop - r || globX[i] < -r || globX[i] > L.w + r) {
                    globLive[i] = false;
                    splat(clamp(globX[i], 0f, L.w), Math.max(L.playTop, globY[i]), true);
                }
            } else {
                if (near(globX[i], globY[i], playerX, playerY, r + bodyR(L) * 0.84f)) {
                    globLive[i] = false;
                    if (engaged) {
                        splashed++;
                        award(1, "GOT SPLASHED +1");
                    }
                    playerBody.impulse(globX[i], globY[i], 0.68f);
                    playerBody.shove(globVX[i], globVY[i], 1.7f);
                    splat(globX[i], globY[i], false);
                } else if (globY[i] > L.deckTop + r || globX[i] < -r || globX[i] > L.w + r) {
                    globLive[i] = false;
                    if (engaged) {
                        dodges++;
                        award(3, "SLIPPERY DODGE +3");
                    }
                    splat(clamp(globX[i], 0f, L.w), Math.min(L.deckTop, globY[i]), false);
                }
            }
        }
    }

    private void splat(float x, float y, boolean fromPlayer) {
        int i = nextSplat++ % MAX_SPLATS;
        splatX[i] = x;
        splatY[i] = y;
        splatPlayer[i] = fromPlayer;
        splatAge[i] = 0.9f;
    }

    private void award(int amount, String message) {
        fun += amount;
        feedback = message;
        feedbackTime = 0.72f;
    }

    /** Session badges are a bitmask which town persistence can merge without coupling saves. */
    int achievements() { return achievements; }

    int achievementCount() { return Integer.bitCount(achievements); }

    private void updateAchievements() {
        if (throwsMade >= 8) achievements |= ACH_TOSSER;
        if (dodges >= 5) achievements |= ACH_DODGER;
        if (hits >= 1 && splashed >= 1) achievements |= ACH_SPLAT_PALS;
        if (movementFun >= 3) achievements |= ACH_BOUNCER;
    }

    private float randomUnit() {
        int x = randomState;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        randomState = x;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    static float bodyR(Layout L) { return Math.min(L.w * 0.102f, (L.deckTop - L.playTop) * 0.095f); }
    static float globR(Layout L) { return bodyR(L) * 0.25f; }
    static float playerY(Layout L) { return L.playTop + (L.deckTop - L.playTop) * 0.76f; }
    static float rivalY(Layout L) { return L.playTop + (L.deckTop - L.playTop) * 0.22f; }

    private static float clamp(float v, float low, float high) { return Math.max(low, Math.min(high, v)); }
    private static boolean near(float ax, float ay, float bx, float by, float radius) {
        float dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy <= radius * radius;
    }
}
