package com.dddumpling.game;

/** Stable, permanent milestones shared by both platform services. */
final class Achievements {
    static final int[] COLLECTION = {1, 10, 25};
    static final int[] STAGES = {5, 10, 20, 30};
    static final int[] SURVIVAL_MINUTES = {1, 2, 3, 4, 5};
    static final int[] LANDS = {2, 3, 4, 5};
    static int bossStart() { return COLLECTION.length + STAGES.length + 2; }
    static int extraStart() { return bossStart() + Boss.COUNT; }
    static int size() { return extraStart() + 2 + SURVIVAL_MINUTES.length + LANDS.length; }
    static String id(int index) {
        if (index < COLLECTION.length) return "collect_" + COLLECTION[index];
        index -= COLLECTION.length;
        if (index < STAGES.length) return "stage_" + STAGES[index];
        index -= STAGES.length;
        if (index < 2) return index == 0 ? "steamer_first_win" : "starpath_first_win";
        index -= 2;
        if (index < Boss.COUNT) return "boss_" + WorldCatalog.BOSSES[index].analytics + "_first_win";
        index -= Boss.COUNT;
        if (index < 2) return index == 0 ? "perfect_stages_5" : "gauntlet_complete";
        index -= 2;
        if (index < SURVIVAL_MINUTES.length) return "survival_minutes_" + SURVIVAL_MINUTES[index];
        return "run_lands_" + LANDS[index - SURVIVAL_MINUTES.length];
    }
    static int target(int index) {
        if (index >= extraStart()) {
            int n = index - extraStart();
            if (n < 2) return n == 0 ? 5 : 1;
            n -= 2;
            return n < SURVIVAL_MINUTES.length ? SURVIVAL_MINUTES[n] * 60 : LANDS[n - SURVIVAL_MINUTES.length];
        }
        if (index < COLLECTION.length) return COLLECTION[index];
        index -= COLLECTION.length;
        return index < STAGES.length ? STAGES[index] : 1;
    }
    static int steps(Progress progress, int index) {
        if (!progress.available()) return 0;
        long value;
        if (index >= extraStart()) {
            int n = index - extraStart();
            value = n == 0 ? progress.count("achievement_perfect_stages")
                : n == 1 ? progress.maximum("achievement_gauntlet_complete")
                : n < 2 + SURVIVAL_MINUTES.length ? progress.maximum("achievement_survival_seconds")
                : progress.maximum("achievement_run_lands");
        } else if (index < COLLECTION.length) {
            value = 0;
            for (int i = 0; i < Collect.COUNT; i++)
                if (progress.count(Collect.progressKey(i)) > 0) value++;
        } else if (index < COLLECTION.length + STAGES.length) value = progress.maximum("highest_stage");
        else {
            int n = index - COLLECTION.length - STAGES.length;
            value = progress.count(n < 2 ? (n == 0 ? "steamer_won" : "starpath_won")
                    : "boss_" + WorldCatalog.BOSSES[n - 2].analytics + "_won");
            if(n>=2) value=Math.max(value,progress.maximum("achievement_boss_"+WorldCatalog.BOSSES[n-2].analytics));
        }
        return (int)Math.max(0, Math.min(target(index), value));
    }
}
