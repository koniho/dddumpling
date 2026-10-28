package com.dddumpling.game;

import java.io.IOException;

/** Local progress is authoritative while offline; Play events are best-effort observations. */
final class Progress {
    interface Store {
        byte[] loadProgress();
        void saveProgress(byte[] data);
        String progressReplica();
    }
    interface Sink {
        void event(String name, int amount);
        void changed();
    }
    // Persisted event IDs stay stable when a boss's display name changes.
    static final String[] BOSSES = new String[Boss.COUNT];
    static {for(int i=0;i<BOSSES.length;i++)BOSSES[i]=WorldCatalog.BOSSES[i].analytics;}
    private final Store store;
    private final String replica;
    private final boolean enabled;
    private ProgressData data = new ProgressData();
    private Sink sink;
    private boolean healthy = true, running, stageDone;
    long revision;
    private int stage, boss = -1, startLand;
    private final java.util.Set<Integer> runLands = new java.util.HashSet<>();
    private String minigame;
    private double bossSeconds;
    private boolean firstHit, scoresSuppressed;
    String error;

    Progress(Store store, boolean enabled) {
        this.store = store; this.enabled = enabled;
        String id = "memory";
        if ((enabled || BuildFlags.DEVELOPER) && store != null) {
            try { id = store.progressReplica(); data = ProgressData.decode(store.loadProgress()); }
            catch (Exception e) { fail("Local progress could not be loaded"); }
        }
        replica = id;
    }
    boolean available() {
        if (enabled && healthy && data.size() > ProgressData.MAX_ENTRIES - 128)
            fail("Progress save capacity reached");
        return enabled && healthy;
    }
    void resetForDeveloper() {
        if (!BuildFlags.DEVELOPER) return;
        data=new ProgressData();revision++;
        running=stageDone=firstHit=scoresSuppressed=false;
        stage=startLand=0;boss=-1;minigame=null;bossSeconds=0;
        if (store!=null) store.saveProgress(data.encode());
    }
    void attach(Sink sink) { this.sink = available() ? sink : null; }
    long count(String name) { return data.total(name); }
    long maximum(String name) { return data.maximum(name); }
    byte[] snapshot() { return data.encode(); }
    private void fail(String reason) { healthy = false; error = reason; sink = null; }
    private void changed() {
        revision++;
        if (!available()) return;
        try { if (store != null) store.saveProgress(data.encode()); }
        catch (Exception e) { fail("Local progress could not be saved"); return; }
        if (sink != null) try { sink.changed(); } catch (RuntimeException ignored) { }
    }
    boolean cosmeticsAvailable() {return healthy && (enabled || BuildFlags.DEVELOPER);}
    boolean accessories(String[] keys,long[] values,boolean reset) {
        if(!cosmeticsAvailable())return false;
        try {
            ProgressData next=ProgressData.decode(data.encode());
            if(reset)next.resetAccessories();
            for(int i=0;i<keys.length;i++)next.maximum(keys[i],values[i]);
            if(store!=null)store.saveProgress(next.encode());
            data=next;revision++;
        } catch(Exception e) {error="Accessories could not be saved";return false;}
        if(sink!=null)try {sink.changed();}catch(RuntimeException ignored) { }
        return true;
    }
    private void event(String name) { event(name, 1); }
    private void event(String name, int amount) {
        if (amount <= 0) return;
        data.increment(replica, name, amount);
        if (sink != null) try { sink.event(name, amount); } catch (RuntimeException ignored) { }
    }
    void seed(GameCore c) {
        if (available()) {
            for (long best : c.survival.bestTime) data.maximum("achievement_survival_seconds", best / 1000);
            for (long[] best : c.timeAttack.best)
                for(int i=0;i<best.length;i++) if(best[i]>0) {
                    if(i==TimeAttack.ALL) data.maximum("achievement_gauntlet_complete", 1);
                    else data.maximum("achievement_boss_"+BOSSES[i],1);
                }
        }
        if (!available() || data.maximum("migrated") != 0) return;
        for (int i = 0; i < Collect.COUNT; i++) data.legacy(Collect.progressKey(i), c.collectionCounts[i]);
        data.legacy("rewards_total", c.collectTotal);
        data.legacy("steamer_won", c.steamer.opens);
        data.legacy("starpath_won", c.stars.wins);
        data.maximum("best_score", c.best);
        data.maximum("migrated", 1);
        changed();
    }
    private String scoreKey() { return scoreKey(startLand); }
    private static String scoreKey(int land) { return land == 0 ? "best_score" : "best_score_land_" + land; }
    void startRun() { startRun(0); }
    void startRun(int land) {
        if (!available()) return;
        if (running) finishRun(0, true);
        scoresSuppressed = false;
        startLand = land;
        running = true; stage = 0; boss = -1; minigame = null; runLands.clear();
        event("runs_started"); enterStage(Lands.firstStage(land));
    }
    void enterStage(int next) {
        if (!available() || !running || next == stage) return;
        stage = next; stageDone = false;
        data.maximum("highest_stage", stage);
        event("stage_reached_" + bucket(stage));
        boss = Boss.kindFor(stage);
        bossSeconds = 0; firstHit = false;
        if (boss >= 0) event("boss_" + BOSSES[boss] + "_started");
        changed();
    }
    void completeStage(int score) {
        completeStage(score, false);
    }
    void completeStage(int score, boolean perfect) {
        if (!available() || !running || stageDone) return;
        stageDone = true; recordScore(score);
        if (perfect) data.increment(replica, "achievement_perfect_stages", 1);
        event("stages_completed"); changed();
    }
    void bossTime(double seconds) {
        if (available() && running && boss >= 0 && !firstHit && seconds > 0 && !Double.isNaN(seconds) && !Double.isInfinite(seconds))
            bossSeconds = Math.min(Integer.MAX_VALUE / 1000.0, bossSeconds + seconds);
    }
    void bossDamage(int kind, float before, float after) {
        if (!available() || !running || boss != kind || firstHit || !(after < before)) return;
        firstHit = true;
        String prefix = "boss_" + BOSSES[kind] + "_first_hit_";
        event(prefix + "count");
        event(prefix + "ms_total", Math.max(1, (int) Math.round(bossSeconds * 1000)));
        event(prefix + (bossSeconds < 5 ? "under_5s" : bossSeconds < 15 ? "5_15s"
                : bossSeconds < 30 ? "15_30s" : bossSeconds < 60 ? "30_60s" : "60s_plus"));
        changed();
    }
    void beatBoss(int kind) {
        if (!available() || !running || boss != kind) return;
        event("boss_" + BOSSES[kind] + "_won"); boss = -1;
        runLands.add(kind); data.maximum("achievement_run_lands", runLands.size()); changed();
    }
    void timeAttackBossWon(int kind) {
        if(!available() || kind<0 || kind>=BOSSES.length) return;
        data.maximum("achievement_boss_"+BOSSES[kind],1); changed();
    }
    void survivalTime(long seconds) {
        if (!available() || seconds <= data.maximum("achievement_survival_seconds")) return;
        data.maximum("achievement_survival_seconds", seconds); changed();
    }
    void gauntletComplete() {
        if (!available() || data.maximum("achievement_gauntlet_complete") != 0) return;
        data.maximum("achievement_gauntlet_complete", 1); changed();
    }
    void startMinigame(boolean stars) {
        if (!available() || !running || minigame != null) return;
        minigame = stars ? "starpath" : "steamer";
        event(minigame + "_started"); changed();
    }
    void finishMinigame(boolean won) {
        if (!available() || !running || minigame == null) return;
        event(minigame + (won ? "_won" : "_failed")); minigame = null; changed();
    }
    void reward(int who, boolean fresh, String source, int score) {
        if (!available() || !running || who < 0 || who >= Collect.COUNT) return;
        data.increment(replica, Collect.progressKey(who), 1);
        recordScore(score);
        event("rewards_total"); event(fresh ? "rewards_new" : "rewards_duplicate");
        event("rewards_" + source); changed();
    }
    long duckSequence() { return data.component(replica,"duck_awards"); }
    void duckReward(long sequence,int who,boolean fresh) {
        if(!available() || sequence<=duckSequence())return;
        data.increment(replica,"duck_awards",sequence-duckSequence());
        data.increment(replica,Collect.progressKey(who),1);
        event("rewards_total");event(fresh?"rewards_new":"rewards_duplicate");
        event("rewards_survival");changed();
    }
    void starter(int who) {
        if (!available()) return;
        data.increment(replica,"prize_"+who,1);
        event("rewards_total");event("rewards_new");event("rewards_starter");changed();
    }
    void checkpoint(int score) {
        if (!available()) return;
        recordScore(score); changed();
    }
    void finishRun(int score, boolean abandoned) {
        if (!available() || !running) return;
        recordScore(score);
        event(abandoned ? "runs_abandoned" : "runs_finished");
        if (!abandoned) event("run_end_" + bucket(stage));
        if (boss >= 0) {
            event("boss_" + BOSSES[boss] + (abandoned ? "_abandoned" : "_failed"));
            if (!firstHit) event("boss_" + BOSSES[boss] + "_no_damage");
        }
        if (minigame != null) event(minigame + "_abandoned");
        running = false; boss = -1; minigame = null; changed();
    }
    private void recordScore(int score) {
        if (!scoresSuppressed) data.maximum(scoreKey(), score);
    }
    ProgressData prepareScoreReset() throws IOException {
        if (!enabled) return null;
        if (!available()) throw new IOException("Progress unavailable");
        ProgressData next=ProgressData.decode(data.encode());
        next.resetScores();
        return next;
    }
    void acceptScoreReset(ProgressData next) {
        if (next!=null) {data=next;revision++;}
        scoresSuppressed=true;
        if (sink!=null) try { sink.changed(); } catch (RuntimeException ignored) { }
    }
    /** A newer score reset wins over old maxima; collection counters still merge. */
    void restore(byte[] bytes, GameCore c) throws IOException {
        if (!available()) throw new IOException("Progress unavailable");
        ProgressData merged=ProgressData.decode(data.encode());
        merged.merge(ProgressData.decode(bytes));
        if (merged.scoreEpoch()>data.scoreEpoch()) {
            if (c.store!=null && !c.store.resetHighScores(merged.encode()))
                throw new IOException("Score reset could not be saved");
            acceptScoreReset(merged);
            c.clearScoreRecords();
        } else data=merged;
        if (c.state == GameCore.TITLE) apply(c);
        changed();
    }
    void apply(GameCore c) {
        if (!available()) return;
        boolean modified = false;
        for (int land = 0; land < Lands.COUNT; land++) {
            int best = Math.max(c.landBests[land], ProgressData.integer(data.maximum(scoreKey(land))));
            modified |= c.landBests[land] != best; c.landBests[land] = best;
        }
        c.best = c.landBests[c.state == GameCore.TITLE && c.landChoice < Lands.COUNT ? c.landChoice : c.runStartLand];
        long known = 0;
        for (int i = 0; i < Collect.COUNT; i++) {
            int count = Math.max(c.collectionCounts[i], ProgressData.integer(data.total(Collect.progressKey(i))));
            modified |= c.collectionCounts[i] != count; c.collectionCounts[i] = count;
            if (c.collectionCounts[i] > 0) c.collected = Collect.add(c.collected, i);
            known = ProgressData.add(known, c.collectionCounts[i]);
        }
        int total = Math.max(c.collectTotal, ProgressData.integer(Math.max(known, data.total("rewards_total"))));
        int opens = Math.max(c.steamer.opens, ProgressData.integer(data.total("steamer_won")));
        int wins = Math.max(c.stars.wins, Math.min(StarPath.MAX_DIFFICULTY,
                ProgressData.integer(data.total("starpath_won"))));
        modified |= c.collectTotal != total || c.steamer.opens != opens || c.stars.wins != wins;
        c.collectTotal = total; c.steamer.opens = opens; c.stars.wins = wins;
        if (c.store != null && modified) {
            for (int land = 0; land < Lands.COUNT; land++) c.store.saveLandBest(land, c.landBests[land]);
            c.store.saveCollected(Collect.encode(c.collected));
            c.store.saveCollectionCounts(c.collectionCounts); c.store.saveCollectTotal(c.collectTotal);
            c.store.saveSteamerOpens(c.steamer.opens); c.store.saveStarWins(c.stars.wins);
        }
    }
    private static String bucket(int stage) {
        return stage <= 4 ? "1_4" : stage <= 9 ? "5_9" : stage <= 14 ? "10_14"
                : stage <= 19 ? "15_19" : "20_plus";
    }
}
