package com.dddumpling.game;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class TestProgress extends Check {
    static final class Storage implements Progress.Store {
        byte[] bytes;
        final String id;
        boolean broken;
        Storage(String id) { this.id = id; }
        public byte[] loadProgress() { return bytes; }
        public void saveProgress(byte[] value) {
            if (broken) throw new IllegalStateException("disk full");
            bytes = value.clone();
        }
        public String progressReplica() { return id; }
    }
    static final class Events implements Progress.Sink {
        final Map<String, Long> values = new HashMap<>();
        public void changed() {}
        public void event(String name, int amount) { values.put(name, count(name) + amount); }
        long count(String name) { return values.getOrDefault(name, 0L); }
    }
    static void all(Layout L) {
        scoreResetMerge(); merge(); counters(); firstHit(); failures();
        if (!BuildFlags.DEVELOPER) production(L);
        else {
            Mem m = new Mem(); GameCore c = new GameCore(m, 82);
            c.startGame(); Interlude.awardBossPrize(c, Boss.SLIME); c.toTitle();
            check("developer build does not record production progress", m.progress == null && c.progress.count("runs_started") == 0);
        }
    }
    private static void scoreResetMerge() {
        try {
            ProgressData old=new ProgressData();old.maximum("best_score",9000);
            old.maximum("best_score_land_1",8000);old.increment("one","prize_0",3);
            ProgressData reset=ProgressData.decode(old.encode());reset.resetScores();
            ProgressData a=ProgressData.decode(old.encode());a.merge(reset);
            ProgressData b=ProgressData.decode(reset.encode());b.merge(old);
            check("score resets merge in either order",Arrays.equals(a.encode(),b.encode())
                    && a.maximum("best_score")==0 && a.maximum("best_score_land_1")==0);
            reset.maximum("best_score",120);a.merge(reset);a.merge(old);
            check("post-reset records replace higher old scores",a.maximum("best_score")==120 && a.total("prize_0")==3);
            ProgressData newer=ProgressData.decode(reset.encode());newer.resetScores();newer.maximum("best_score",20);
            b.merge(newer);a.merge(b);b.merge(a);
            check("repeated resets merge idempotently",Arrays.equals(a.encode(),b.encode()) && a.maximum("best_score")==20);
            Mem m=new Mem();GameCore c=new GameCore(m,111,true);
            c.startGame();c.score=9999;LandPicker.recordBest(c);c.highScores.finish(c);c.toTitle();
            c.progress.restore(newer.encode(),c);
            check("remote reset clears local history and projects new records",c.highScores.runs.isEmpty() && c.best==20
                    && new GameCore(m,112,true).best==20);
        } catch(Exception failure) { check("score reset merge succeeds",false); }
    }
    private static void merge() {
        try {
            ProgressData a = new ProgressData(), b = new ProgressData();
            a.legacy("prize_0", 5); b.legacy("prize_0", 5);
            a.increment("a", "prize_0", 2); b.increment("b", "prize_0", 3);
            a.maximum("best_score", 800); b.maximum("best_score", 1200);
            ProgressData c = ProgressData.decode(a.encode()); c.merge(b);
            ProgressData d = ProgressData.decode(b.encode()); d.merge(a);
            check("offline awards on two devices are added to their shared legacy baseline", c.total("prize_0") == 10);
            check("merge is commutative", Arrays.equals(c.encode(), d.encode()));
            byte[] once = c.encode(); c.merge(d); c.merge(a);
            check("repeated restore does not multiply rewards", Arrays.equals(once, c.encode()));
            check("highest score always survives", c.maximum("best_score") == 1200);
            ProgressData e = new ProgressData(); e.increment("third", "prize_0", 4);
            a.merge(b); a.merge(e); b.merge(e); d = ProgressData.decode(once); d.merge(b);
            check("three-way merge is associative", Arrays.equals(a.encode(), d.encode()));
            c.increment("a", "prize_0", 1); d = ProgressData.decode(once); d.increment("b", "prize_0", 1);
            c.merge(d);
            check("new awards after a sync remain distinct", c.total("prize_0") == 12);
            e.increment("third", "overflow", Long.MAX_VALUE); e.increment("other", "overflow", 100);
            check("counter totals saturate instead of overflowing", e.total("overflow") == Long.MAX_VALUE);
            byte[] future = once.clone(); future[7] = 2;
            check("future schema is rejected", rejects(future));
            check("truncated saves are rejected", rejects(Arrays.copyOf(once, once.length - 1)));
            check("trailing bytes are rejected", rejects(Arrays.copyOf(once, once.length + 1)));
            check("oversize saves are rejected", rejects(new byte[ProgressData.MAX_BYTES + 1]));
            check("empty cloud slot is supported", ProgressData.decode(new byte[0]).total("prize_0") == 0);
        } catch (IOException e) { check("save format decodes", false); }
    }
    private static boolean rejects(byte[] bytes) {
        try { ProgressData.decode(bytes); return false; } catch (IOException e) { return true; }
    }
    private static void counters() {
        Storage store = new Storage("one"); Progress p = new Progress(store, true); Events e = new Events(); p.attach(e);
        p.reward(0, true, "boss", 100);
        check("title/demo rewards are not counted", p.count("rewards_total") == 0);
        p.startRun(); p.enterStage(1); p.completeStage(100); p.completeStage(100);
        p.startMinigame(false); p.startMinigame(false); p.finishMinigame(true); p.finishMinigame(false);
        p.enterStage(5); p.beatBoss(Boss.SLIME); p.beatBoss(Boss.SLIME);
        p.reward(45, true, "boss", 250); p.reward(45, false, "boss", 400);
        p.finishRun(900, false); p.finishRun(900, false); p.finishRun(900, true);
        check("run and stage callbacks count once", p.count("runs_started") == 1 && p.count("stages_completed") == 1);
        check("successful minigame cannot later fail", p.count("steamer_started") == 1 && p.count("steamer_won") == 1 && p.count("steamer_failed") == 0);
        check("boss victory cannot later become a failure", p.count("boss_slime_won") == 1 && p.count("boss_slime_failed") == 0);
        check("reward sources share the same duplicate accounting", p.count("rewards_total") == 2
                && p.count("rewards_new") == 1 && p.count("rewards_duplicate") == 1 && p.count("prize_45") == 2);
        check("game over counts once without adding abandonment", p.count("runs_finished") == 1 && p.count("runs_abandoned") == 0);
        Progress loaded = new Progress(store, true); Events second = new Events(); loaded.attach(second);
        check("statistics survive restart without re-emitting events", loaded.count("runs_started") == 1 && second.values.isEmpty());
        p.startRun(); p.enterStage(10); p.startMinigame(true); p.finishRun(50, true);
        check("abandonment is separate from a loss", p.count("boss_dark_divide_abandoned") == 1
                && p.count("starpath_abandoned") == 1 && p.count("boss_dark_divide_failed") == 0);
    }
    private static void firstHit() {
        for (int kind = 0; kind < Boss.COUNT; kind++) {
            Progress p = new Progress(new Storage("timer"), true); Events e = new Events(); p.attach(e);
            p.startRun(); p.enterStage((kind + 1) * 5); p.bossTime(12.345);
            p.bossDamage(kind, 10, 10); p.bossDamage(kind, 10, 11);
            String prefix = "boss_" + Progress.BOSSES[kind] + "_first_hit_";
            check("setup and shield hits do not count for boss " + kind, p.count(prefix + "count") == 0);
            p.bossDamage(kind, 10, 9); p.bossTime(100); p.bossDamage(kind, 9, 8);
            check("first real hit records one timing sample for boss " + kind,
                    p.count(prefix + "count") == 1 && p.count(prefix + "ms_total") == 12345
                    && p.count(prefix + "5_15s") == 1 && e.count(prefix + "count") == 1);
            p.finishRun(0, false);
            check("damaged boss does not enter no-damage count " + kind,
                    p.count("boss_" + Progress.BOSSES[kind] + "_no_damage") == 0);
            p.startRun(); p.enterStage((kind + 1) * 5); p.bossTime(60); p.finishRun(0, false);
            check("failed attempt without damage is counted for boss " + kind,
                    p.count("boss_" + Progress.BOSSES[kind] + "_no_damage") == 1);
        }
        Progress p = new Progress(new Storage("bounds"), true);
        for (double t : new double[] {0, 5, 15, 30, 60}) {
            p.startRun(); p.enterStage(5); p.bossTime(t); p.bossDamage(0, 2, 1); p.finishRun(0, false);
        }
        for (String bucket : new String[] {"under_5s", "5_15s", "15_30s", "30_60s", "60s_plus"})
            check("first-hit bucket boundary " + bucket, p.count("boss_slime_first_hit_" + bucket) == 1);
    }
    private static void failures() {
        Storage full = new Storage("full"); ProgressData fullData = new ProgressData();
        for (int i = 0; i < ProgressData.MAX_ENTRIES - 64; i++) fullData.increment("a", "metric_" + i, 1);
        full.bytes = fullData.encode(); Progress fullTracker = new Progress(full, true);
        fullTracker.startRun();
        check("near-capacity save disables tracking without crashing or overwriting", !fullTracker.available()
                && Arrays.equals(full.bytes, fullData.encode()));
        Storage broken = new Storage("broken"); broken.bytes = new byte[] {1,2,3};
        Progress p = new Progress(broken, true); p.startRun();
        check("corrupt local progress is preserved and disables sync", !p.available() && broken.bytes.length == 3);
        broken = new Storage("broken"); p = new Progress(broken, true); broken.broken = true; p.startRun();
        check("persistence failure does not crash gameplay", !p.available());
        p = new Progress(new Storage("network"), true);
        p.attach(new Progress.Sink() {
            public void event(String name, int amount) { throw new IllegalStateException(); }
            public void changed() { throw new IllegalStateException(); }
        });
        p.startRun(); p.finishRun(10, false);
        check("reporting failure cannot stop local progress", p.available() && p.count("runs_finished") == 1);
    }
    private static void production(Layout L) {
        Mem landStore = new Mem(); landStore.best = 500;
        landStore.collected = Collect.add(0L, Collect.BOSS_FIRST);
        GameCore landRun = new GameCore(landStore, 619L);
        LandPicker.select(landRun, 1);
        landRun.startGame();
        check("land starts report their real starting stage", landRun.progress.count("stage_reached_1_4") == 0
                && landRun.progress.count("stage_reached_5_9") == 1);
        landRun.score = 900; landRun.progress.checkpoint(landRun.score);
        landRun.lives = 1; landRun.takeHit(L.w * 0.5f, L); landRun.toTitle();
        check("production land scores do not contaminate full runs", landRun.progress.maximum("best_score") == 500
                && landRun.progress.maximum("best_score_land_1") == 900 && landStore.best == 500);
        GameCore reopened = new GameCore(landStore, 620L);
        check("production app launch keeps unlocks but resets selection", reopened.landChoice == 0
                && reopened.best == 500 && reopened.landBests[1] == 900 && LandPicker.visible(reopened));
        try {
            ProgressData remoteLand = new ProgressData(); remoteLand.maximum("best_score_land_1", 1200);
            reopened.progress.restore(remoteLand.encode(), reopened);
            check("merged land records remain separate", reopened.best == 500 && reopened.landBests[1] == 1200);
        } catch (IOException e) { check("land score merge succeeds", false); }
        Mem fresh = new Mem(); new GameCore(fresh, 12);
        check("fresh tracking does not write a fake legacy score or unlock the tutorial roster", fresh.saves == 0);
        Mem store = new Mem(); store.collected = 1L; store.collectionCounts[0] = 7; store.collectTotal = 7; store.best = 400;
        GameCore c = new GameCore(store, 117);
        check("legacy collection is seeded without historical events", c.progress.count("prize_0") == 7
                && c.progress.count("rewards_new") == 0 && c.progress.maximum("best_score") == 400);
        c.startGame(); Interlude.awardPrize(c); Interlude.awardStarPrize(c); Interlude.awardBossPrize(c, Boss.SLIME);
        check("all actual award paths emit one source event", c.progress.count("rewards_steamer") == 1
                && c.progress.count("rewards_starpath") == 1 && c.progress.count("rewards_boss") == 1);
        c.lives = 1; c.takeHit(L.w/2, L); c.toTitle();
        check("actual death and return to title produce one run end", c.progress.count("runs_finished") == 1 && c.progress.count("runs_abandoned") == 0);
        try {
            ProgressData remote = new ProgressData(); remote.increment("other", "prize_0", 3);
            remote.increment("other", "prize_48", 2); remote.maximum("best_score", 950);
            long emitted = c.progress.count("rewards_new");
            c.progress.restore(remote.encode(), c); c.progress.restore(remote.encode(), c);
            check("cloud restore unions characters and adds only distinct rewards", c.collectionCounts[0] == 10 && c.collectionCounts[48] == 2);
            check("cloud restore keeps best score without generating rewards", c.best == 950 && c.progress.count("rewards_new") == emitted);
            GameCore loaded = new GameCore(store, 118);
            check("merged progress projects back into existing save fields", loaded.collectionCounts[48] == 2 && loaded.best == 950);
            c.startGame(); remote.increment("other", "prize_48", 1); c.progress.restore(remote.encode(), c);
            check("restoration does not change collection midway through a run", c.collectionCounts[48] == 2);
            c.toTitle(); check("pending restored rewards appear on returning home", c.collectionCounts[48] == 3);
            byte[] before = c.progress.snapshot();
            try { c.progress.restore(new byte[] {9}, c); } catch (IOException expected) { }
            check("bad cloud data cannot overwrite local progress", Arrays.equals(before, c.progress.snapshot()));
        } catch (IOException e) { check("production cloud merge succeeds", false); }
        GameCore boss = new GameCore(new Mem(), 14); boss.startGame(); boss.stage = 5;
        boss.progress.enterStage(5); boss.boss.begin(Boss.SLIME, 5, boss.rnd, boss.playRosterFull());
        boss.update(.1f, .1f, L);
        boss.boss.intro = 0; boss.slowdown = 1;
        Pause.open(boss);
        boss.update(.01f, 45f, L);
        Pause.resume(boss);
        boss.update(.01f, 2f, L);
        boss.progress.bossDamage(0, 10, 9);
        check("timer excludes intro and pause, and counts elapsed time before slow motion",
                boss.progress.count("boss_slime_first_hit_ms_total") == 2000);
        Pause.open(boss); Pause.action(boss, 2); Pause.action(boss, 2);
        check("pause end records one abandonment and no loss", boss.state == GameCore.TITLE
                && boss.progress.count("runs_abandoned") == 1
                && boss.progress.count("boss_slime_abandoned") == 1
                && boss.progress.count("runs_finished") == 0);
        boss.toTitle();
        check("repeated title transition does not double count abandonment",
                boss.progress.count("runs_abandoned") == 1);
    }
}
