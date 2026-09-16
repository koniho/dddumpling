package com.dddumpling.game;

import java.util.Arrays;

public final class IOSCloudTest extends Check {
    private static final class Host implements IOSCloud.Host {
        int fetches, writes, token;
        byte[] saved;
        public void fetch(int token) { fetches++; this.token = token; }
        public void save(byte[] data, int token) { writes++; saved = data; this.token = token; }
    }
    private static byte[] remote(int best, int prizes) {
        ProgressData data = new ProgressData();
        data.maximum("best_score", best); data.increment("remote", "prize_0", prizes);
        return data.encode();
    }
    private static void tick(IOSCloud cloud, int seconds) {
        for (int i = 0; i < seconds; i++) cloud.update(1);
    }
    public static void main(String[] args) throws Exception {
        Mem store = new Mem(); GameCore core = new GameCore(store, 9, true);
        Host host = new Host(); IOSCloud cloud = new IOSCloud(core, host, true);
        cloud.session(null, true); cloud.update(1);
        check("signed-out play never contacts cloud", host.fetches == 0);
        cloud.session("player", true); cloud.update(0);
        int token = host.token;
        cloud.fetched(new byte[][] {remote(500, 2), remote(900, 3)}, token);
        check("conflicts merge maxima without double-counting", core.best == 900 && core.collectionCounts[0] == 3);
        check("merged cloud progress is durable before upload", Arrays.equals(store.progress, host.saved));
        cloud.saved(token);
        tick(cloud, 59); check("successful sync waits a minute", host.fetches == 1);
        tick(cloud, 1); check("periodic sync fetches again", host.fetches == 2);
        cloud.fetched(new byte[][] {remote(900, 3)}, host.token); cloud.saved(host.token);
        check("repeated conflict does not award again", core.collectionCounts[0] == 3);
        core.progress.checkpoint(950); tick(cloud, 4);
        check("local checkpoint debounces cloud save", host.fetches == 2);
        tick(cloud, 1); check("dirty save starts after five seconds", host.fetches == 3);
        int stale = host.token;
        cloud.session(null, true);
        cloud.fetched(new byte[][] {remote(9999, 99)}, stale);
        check("signed-out callback cannot merge or upload", core.best == 900 && host.writes == 2);
        cloud.session("other", true); cloud.update(0);
        int next = host.token;
        cloud.failed(stale, true);
        cloud.fetched(new byte[0][], next); cloud.saved(next);
        check("old callback cannot block the new session", host.writes == 3 && cloud.status().equals("Synced"));
        cloud.changed(); tick(cloud, 5);
        byte[] before = store.progress.clone();
        cloud.fetched(new byte[][] {remote(10000, 100), new byte[] {1, 2}}, host.token);
        check("one invalid conflict preserves all local bytes", Arrays.equals(before, store.progress));
        check("invalid conflict blocks writes", host.writes == 3);
        int fetches = host.fetches; tick(cloud, 120);
        check("invalid save is not retried automatically", host.fetches == fetches);

        Host retryHost = new Host(); IOSCloud retry = new IOSCloud(core, retryHost, true);
        retry.session("player", true); retry.update(0); retry.failed(retryHost.token, false);
        tick(retry, 59); check("network failure backs off", retryHost.fetches == 1);
        tick(retry, 1); check("network failure retries", retryHost.fetches == 2);
        retry.fetched(new byte[0][], retryHost.token); retry.saved(retryHost.token);
        core.score = 1234; retry.session("player", false);
        check("background checkpoints and starts pending save", core.progress.maximum("best_score") == 1234 && retryHost.fetches == 3);
        retry.fetched(new byte[0][], retryHost.token); retry.saved(retryHost.token);
        tick(retry, 120); check("background does not poll", retryHost.fetches == 3);

        Host disabledHost = new Host(); IOSCloud disabled = new IOSCloud(core, disabledHost, false);
        disabled.session("player", true); disabled.update(1); disabled.changed();
        disabled.fetched(new byte[][] {remote(100000, 1000)}, 0);
        check("disabled integration has no calls or merges", disabledHost.fetches == 0 && disabledHost.writes == 0
            && core.progress.maximum("best_score") == 1234);
        check("iOS developer game records progress for isolated cloud testing",
            new IOSGame(new Mem(), new Ear(), 9).core().progress.available());
        Host lateHost = new Host(); GameCore lateCore = new GameCore(new Mem(), 9, true);
        IOSCloud late = new IOSCloud(lateCore, lateHost, true);
        late.session("player", true); late.update(0);
        int first = lateHost.token;
        late.fetched(new byte[0][], first);
        lateCore.progress.checkpoint(800);
        late.saved(first); tick(late, 5);
        check("progress earned during upload schedules a follow-up", lateHost.fetches == 2);
        int second = lateHost.token;
        late.saved(first); late.failed(first, true);
        ProgressData other = new ProgressData(); other.increment("another_device", "prize_0", 4);
        late.fetched(new byte[][] {remote(500, 2), other.encode()}, second);
        check("late completion cannot finish or block a newer request", lateHost.writes == 2);
        check("different devices retain all earned prizes", lateCore.collectionCounts[0] == 6);
        check("cloud merge retains local progress earned in flight", lateCore.best == 800);
        late.saved(second); late.changed(); tick(late, 5);
        int writes = lateHost.writes;
        late.fetched(new byte[][] {new byte[0]}, lateHost.token);
        check("existing empty cloud file is rejected instead of overwritten", lateHost.writes == writes);
        System.out.println("iOS cloud: " + pass + " passed, " + fail + " failed");
        if (fail != 0) throw new AssertionError("iOS cloud regressions");
    }
}
