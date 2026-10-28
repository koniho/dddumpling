package com.dddumpling.game;

/** Append-only wardrobe IDs and reward mapping. No new platform achievements required. */
final class Accessories {
    static final int HAT=0, GLASSES=1, BOOTS=2, MASK=3, FLAIR=4, SLOTS=5, COUNT=16;
    static final String[] SLOT={"HATS","SHADES","BOOTS","MASKS","FLAIR"};
    static final String[] ID={"sunhat","sprout_cap","star_crown","rose_shades","sea_shades","star_shades",
        "puddle_boots","cloud_boots","comet_boots","petal_mask","moon_mask","party_mask","sky_hero_mask","starstruck_flair","flutter_flair","soft_glow_flair"};
    static final String[] NAME={"Sunny Scout","Sprout Cap","Star Crown","Rosy Outlook","Sea Glass","Stargazer",
        "Cloud Clogs","Star Steps","Party Heels","Ninja","Duck","Samurai","Sky Hero","Starstruck","Flutter","Soft Glow"};
    static final String[] GOAL={"Collect your first squishy","Reach stage 5","Win Star Path",
        "Collect 10 squishies","Reach stage 10","Survive 3 minutes",
        "Win the steamer game","Survive 1 minute","Complete All Bosses",
        "Clear 5 perfect stages","Reach stage 20","Collect 25 squishies","Survive 2 minutes","Beat Super Slime","Visit 2 lands in one run","Survive 5 minutes"};
    static final String[] ACHIEVEMENT={"collect_1","stage_5","starpath_first_win",
        "collect_10","stage_10","survival_minutes_3","steamer_first_win","survival_minutes_1",
        "gauntlet_complete","perfect_stages_5","stage_20","collect_25","survival_minutes_2","boss_slime_first_win","run_lands_2","survival_minutes_5"};
    static final int[] COLOR={0xFFFFCC72,0xFF86D99C,0xFFFFE688,0xFFFF9FBD,0xFF79E2E3,0xFFBC9EFF,
        0xFF7BDBB9,0xFFC9A8FF,0xFFFF8CB6,0xFF9E96BE,0xFFFFD66E,0xFFF38583,0xFF79E0E2,0xFFFFDC85,0xFFF1A2E4,0xFF9BEAD6};
    // IDs (including the original mask IDs) stay stable when artwork is revised.
    private static final int[] CATEGORY={HAT,HAT,HAT,GLASSES,GLASSES,GLASSES,BOOTS,BOOTS,BOOTS,MASK,MASK,MASK,MASK,FLAIR,FLAIR,FLAIR};
    static int slot(int item) {return CATEGORY[item];}
    static int count(int slot) {int n=0;for(int category:CATEGORY)if(category==slot)n++;return n;}
    static int item(int slot,int variant) {
        for(int i=0;i<COUNT;i++)if(CATEGORY[i]==slot && variant--==0)return i;
        throw new IllegalArgumentException("Accessory variant");
    }
    static int variant(int item) {
        int n=0;for(int i=0;i<item;i++)if(CATEGORY[i]==CATEGORY[item])n++;return n;
    }
    static int achievement(int item) {
        for(int i=0;i<Achievements.size();i++)if(Achievements.id(i).equals(ACHIEVEMENT[item]))return i;
        throw new IllegalStateException("Missing accessory achievement: "+ID[item]);
    }
    static String key(String field,int item) {return "accessory_"+field+"_"+ID[item];}
}
