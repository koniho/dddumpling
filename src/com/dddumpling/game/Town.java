package com.dddumpling.game;

/**
 * Persistent rules and small simulation for DDDUMPLING Town.
 *
 * The town deliberately owns no {@link GameCore} state.  A run hands it an opaque serial and a
 * score, the collection is sampled when the gate opens, and the host persists {@link #save()} when
 * {@link #dirty} is true.  That narrow seam keeps town-only work out of the main game loop.
 */
final class Town {
    static final int MEADOW = 0, SLIDE = 1, FIGHT = 2;
    static final int NONE = -1, FRIEND = 0, SLIDE_POI = 1, SLIME_POI = 2;
    static final int TALK_NONE = 0, TALK_WELCOME = 1, TALK_FRIEND = 2,
            TALK_BUILD_ARCH = 3, TALK_ARCH_BUILT = 4, TALK_SLIME = 5;
    static final int ARCH_COST = 5, WELCOME_TICKETS = 5, MAX_FLOWERS = 24;

    // Persisted state. Package visibility is useful to the pure preview and focused rules checks.
    int tickets, visits, flowerCount, rider, slideBest, fightBest;
    boolean welcomed, archBuilt;
    long runSerial, lastClaimedRun;
    float flowerTime;

    // Session state.
    int mode = MEADOW, dialogue, targetPoi = NONE;
    float touchX, touchY, touchAge = 3f;
    float path = .08f, targetPath = -1f, steer, bounceClock, idleClock;
    boolean dragging, slideHolding, slimeBossOwned, dirty;
    int pointer = -1;
    long owned;
    final SlimeFight fight = new SlimeFight();

    // Slide session, all in 0..1 scene coordinates.
    float slideCharge, slideX, slideY, slideVX, slideVY, slideSpin, slideCheer, slideRamp;
    boolean slideFlying, slideAirborne, slideLanded;

    Town() { }

    /** Allocates the persisted ID the next main-game run will use for its town reward. */
    long beginRun() {
        if (runSerial < Long.MAX_VALUE) runSerial++;
        dirty = true;
        return runSerial;
    }

    /** Credits one run once. Reopening a summary with the same ID cannot mint more tickets. */
    int grantRunReward(long runId, int score) {
        if (runId <= 0 || runId <= lastClaimedRun) return 0;
        lastClaimedRun = runId;
        if (runId > runSerial) runSerial = runId;
        int reward = score <= 0 ? 0 : Math.max(1, Math.min(12,
                1 + (int) Math.sqrt(score / 300f)));
        tickets = Math.min(9999, tickets + reward);
        dirty = true;
        return reward;
    }

    /** Opens a visit using a read-only snapshot of collection ownership. */
    void enter(int[] collectionCounts, boolean slimeOwned) {
        owned = 0;
        if (collectionCounts != null) {
            for (int i = 0; i < Math.min(Collect.COUNT, collectionCounts.length); i++)
                if (collectionCounts[i] > 0) owned |= 1L << i;
        }
        slimeBossOwned = slimeOwned;
        if (rider != 0 && !owns(rider)) rider = firstOwned();
        mode = MEADOW;
        touchAge = 3f;
        targetPoi = NONE;
        targetPath = -1f;
        steer = 0;
        dragging = false;
        pointer = -1;
        dialogue = welcomed ? TALK_NONE : TALK_WELCOME;
        visits++;
        flowerCount = Math.min(MAX_FLOWERS, Math.max(flowerCount, 2 + visits / 2));
        if (!welcomed) {
            welcomed = true;
            tickets = Math.min(9999, tickets + WELCOME_TICKETS);
        }
        dirty = true;
    }

    void leave() {
        mode = MEADOW;
        dialogue = TALK_NONE;
        targetPoi = NONE;
        targetPath = -1f;
        steer = 0;
        dragging = slideHolding = false;
        pointer = -1;
        fight.cancelInput();
    }

    void markSaved() { dirty = false; }

    boolean owns(int i) { return i >= 0 && i < Collect.COUNT && (owned & 1L << i) != 0; }

    int firstOwned() {
        for (int i = 0; i < Collect.COUNT; i++) if (owns(i)) return i;
        return 0; // Cream Bao is the friendly town guide when the collection is empty.
    }

    void nextRider() {
        if (owned == 0) { rider = 0; return; }
        for (int n = 1; n <= Collect.COUNT; n++) {
            int i = (rider + n) % Collect.COUNT;
            if (i == 0 || owns(i)) { rider = i; dirty = true; return; }
        }
    }

    void travelTo(int poi) {
        dialogue = TALK_NONE;
        targetPoi = poi;
        targetPath = poi == FRIEND ? .20f : poi == SLIDE_POI ? .66f : .88f;
        steer = 0;
    }

    void steer(float amount) {
        steer = Math.max(-1f, Math.min(1f, amount));
        targetPoi = NONE;
        targetPath = -1f;
        dialogue = TALK_NONE;
    }

    /** Visit-only spring impulse; decorations never spend tickets or alter progression. */
    void cycleFlowerGrowth() {
        flowerCount = flowerCount < 8 ? 12 : flowerCount < 20 ? MAX_FLOWERS : 3;
        flowerTime = 0f;
        dirty = true;
    }

    String flowerGrowthName() {
        return flowerCount < 8 ? "SPROUTS" : flowerCount < 20 ? "BLOOMING" : "FULL BLOOM";
    }

    void react(float worldX, float y) {
        touchX = worldX;
        touchY = y;
        touchAge = 0f;
    }

    float motion(float x, float y, float radius) {
        if (touchAge >= 2f || radius <= 0f) return 0f;
        float dx = x - touchX, dy = y - touchY;
        float proximity = Math.max(0f, 1f - (float)Math.sqrt(dx*dx + dy*dy) / radius);
        return proximity * (float)(Math.sin(touchAge * 18f) * Math.exp(-touchAge * 4.5f));
    }

    void update(float dt, Layout L) {
        dt = Math.max(0f, Math.min(.1f, dt));
        idleClock += dt;
        touchAge = Math.min(3f, touchAge + dt);
        if (mode == SLIDE) { updateSlide(dt, L); return; }
        if (mode == FIGHT) {
            fight.update(dt, L);
            if (fight.phase == SlimeFight.RESULT && fight.fun > fightBest) {
                fightBest = fight.fun;
                dirty = true;
            }
            return;
        }
        if (mode != MEADOW) return;

        float drive = steer;
        if (targetPath >= 0f) {
            float d = targetPath - path;
            if (Math.abs(d) < .012f) {
                path = targetPath;
                targetPath = -1f;
                int arrived = targetPoi;
                targetPoi = NONE;
                arrive(arrived);
            } else drive = Math.signum(d) * Math.min(1f, .28f + Math.abs(d) * 3.5f);
        }
        if (Math.abs(drive) > .02f) {
            path = Math.max(.055f, Math.min(.945f, path + drive * dt * (.12f + .24f * Math.abs(drive))));
            bounceClock += dt * (4.2f + 8.5f * Math.abs(drive));
        } else bounceClock += dt * 2.2f;

        flowerTime += dt;
        if (flowerCount < MAX_FLOWERS && flowerTime >= 14f) {
            flowerTime -= 14f;
            flowerCount++;
            dirty = true;
        }
    }

    private void arrive(int poi) {
        if (poi == FRIEND) dialogue = archBuilt ? TALK_FRIEND : TALK_BUILD_ARCH;
        else if (poi == SLIDE_POI) beginSlide();
        else if (poi == SLIME_POI && slimeBossOwned) dialogue = TALK_SLIME;
    }

    boolean buyArch() {
        if (archBuilt || tickets < ARCH_COST) return false;
        tickets -= ARCH_COST;
        archBuilt = true;
        flowerCount = Math.min(MAX_FLOWERS, flowerCount + 4);
        dialogue = TALK_ARCH_BUILT;
        dirty = true;
        return true;
    }

    void beginSlide() {
        mode = SLIDE;
        dialogue = TALK_NONE;
        slideCharge = 0;
        slideX = .17f;
        slideY = .20f;
        slideVX = slideVY = slideSpin = slideCheer = slideRamp = 0;
        slideFlying = slideAirborne = slideLanded = slideHolding = false;
    }

    void holdSlide() {
        if (mode == SLIDE && !slideFlying && !slideLanded) slideHolding = true;
    }

    void releaseSlide() {
        if (!slideHolding || mode != SLIDE || slideFlying || slideLanded) return;
        slideHolding = false;
        slideFlying = true;
        slideRamp = 0;
    }

    static float slideTrackX(float at) {
        return .17f + .31f * at;
    }

    /** Authored chute: gentle start, quick dip, then a visible launch curl. */
    static float slideTrackY(float at) {
        at = Math.max(0f, Math.min(1f, at));
        float e = at * at * (3f - 2f * at);
        float curl = Math.max(0f, Math.min(1f, (at - .78f) / .22f));
        curl = curl * curl * (3f - 2f * curl);
        return .20f + .55f * e - .12f * curl;
    }

    private void updateSlide(float dt, Layout L) {
        if (slideHolding && !slideFlying && !slideLanded)
            slideCharge = Math.min(1f, slideCharge + dt * .72f);
        if (!slideFlying) {
            if (slideLanded) slideCheer += dt;
            return;
        }
        if (!slideAirborne) {
            slideRamp = Math.min(1f, slideRamp + dt * (.74f + slideCharge * .62f));
            float riderOffset = L.w * .070f / Math.max(1f, L.deckTop - L.playTop);
            slideX = slideTrackX(slideRamp);
            slideY = slideTrackY(slideRamp) - riderOffset;
            slideSpin += dt * 2.2f;
            if (slideRamp < 1f) return;
            slideAirborne = true;
            float power = .36f + .64f * slideCharge;
            slideVX = .20f + power * .18f;
            slideVY = -.42f - power * .34f;
        }
        slideVY += 1.62f * dt;
        slideX = Math.min(.94f, slideX + slideVX * dt);
        slideY += slideVY * dt;
        slideSpin += dt * (3f + slideCharge * 4f);
        if (slideY >= .78f) {
            slideY = .78f;
            slideFlying = slideAirborne = false;
            slideLanded = true;
            slideHolding = false;
            slideCheer = 0;
            int distance = Math.max(0, Math.round((slideX - .48f) * 100f));
            if (distance > slideBest) { slideBest = distance; dirty = true; }
        }
    }

    void finishSlide() {
        mode = MEADOW;
        path = .72f;
        dialogue = TALK_NONE;
        targetPath = -1f;
    }

    void beginSlimeFight(Layout L) {
        if (!slimeBossOwned) return;
        mode = FIGHT;
        dialogue = TALK_NONE;
        pointer = -1;
        fight.begin(L);
    }

    void finishSlimeFight() {
        fight.cancelInput();
        mode = MEADOW;
        path = .88f;
        dialogue = TALK_NONE;
        pointer = -1;
    }

    /** Compact, versioned and deliberately tolerant: unknown keys are ignored. */
    String save() {
        return "v1;t=" + tickets + ";v=" + visits + ";f=" + flowerCount + ";ft="
                + Math.round(flowerTime * 1000f) + ";r=" + rider + ";w=" + bit(welcomed)
                + ";a=" + bit(archBuilt) + ";sb=" + slideBest + ";fb=" + fightBest
                + ";rs=" + runSerial
                + ";lc=" + lastClaimedRun;
    }

    void load(String encoded) {
        if (encoded == null || encoded.length() == 0) return;
        String[] fields = encoded.split(";");
        if (fields.length == 0 || !"v1".equals(fields[0])) return;
        int nt = tickets, nv = visits, nf = flowerCount, nr = rider, nsb = slideBest,
                nfb = fightBest;
        float nft = flowerTime;
        boolean nw = welcomed, na = archBuilt;
        long nrs = runSerial, nlc = lastClaimedRun;
        try {
            for (int i = 1; i < fields.length; i++) {
                int at = fields[i].indexOf('=');
                if (at <= 0) continue;
                String key = fields[i].substring(0, at), value = fields[i].substring(at + 1);
                if ("t".equals(key)) nt = Integer.parseInt(value);
                else if ("v".equals(key)) nv = Integer.parseInt(value);
                else if ("f".equals(key)) nf = Integer.parseInt(value);
                else if ("ft".equals(key)) nft = Integer.parseInt(value) / 1000f;
                else if ("r".equals(key)) nr = Integer.parseInt(value);
                else if ("w".equals(key)) nw = Integer.parseInt(value) != 0;
                else if ("a".equals(key)) na = Integer.parseInt(value) != 0;
                else if ("sb".equals(key)) nsb = Integer.parseInt(value);
                else if ("fb".equals(key)) nfb = Integer.parseInt(value);
                else if ("rs".equals(key)) nrs = Long.parseLong(value);
                else if ("lc".equals(key)) nlc = Long.parseLong(value);
            }
        } catch (NumberFormatException ignored) { return; }
        tickets = clamp(nt, 0, 9999);
        visits = Math.max(0, nv);
        flowerCount = clamp(nf, 0, MAX_FLOWERS);
        flowerTime = Math.max(0f, Math.min(14f, nft));
        rider = clamp(nr, 0, Collect.COUNT - 1);
        welcomed = nw;
        archBuilt = na;
        slideBest = Math.max(0, nsb);
        fightBest = Math.max(0, nfb);
        runSerial = Math.max(0, nrs);
        lastClaimedRun = Math.max(0, Math.min(nlc, runSerial));
        dirty = false;
    }

    private static int bit(boolean value) { return value ? 1 : 0; }
    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
