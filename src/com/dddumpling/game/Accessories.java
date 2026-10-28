package com.dddumpling.game;

/** Append-only wardrobe IDs and reward mapping. No new platform achievements required. */
final class Accessories {
    static final int HAT=0, GLASSES=1, BOOTS=2, MASK=3, SLOTS=4, COUNT=12;
    static final String[] SLOT={"HATS","SHADES","BOOTS","MASKS"};
    static final String[] ID={"sunhat","sprout_cap","star_crown","rose_shades","sea_shades","star_shades",
        "puddle_boots","cloud_boots","comet_boots","petal_mask","moon_mask","party_mask"};
    static final String[] NAME={"Sunny Scout","Sprout Cap","Star Crown","Rosy Outlook","Sea Glass","Stargazer",
        "Puddle Hoppers","Cloud Steps","Comet Kicks","Petal Party","Moonbeam","Confetti"};
    static final String[] GOAL={"Collect your first squishy","Reach stage 5","Win Star Path",
        "Collect 10 squishies","Reach stage 10","Survive 3 minutes",
        "Win the steamer game","Survive 1 minute","Complete All Bosses",
        "Clear 5 perfect stages","Reach stage 20","Collect 25 squishies"};
    static final String[] ACHIEVEMENT={"collect_1","stage_5","starpath_first_win",
        "collect_10","stage_10","survival_minutes_3","steamer_first_win","survival_minutes_1",
        "gauntlet_complete","perfect_stages_5","stage_20","collect_25"};
    static final int[] COLOR={0xFFFFCC72,0xFF86D99C,0xFFFFE688,0xFFFF9FBD,0xFF79E2E3,0xFFBC9EFF,
        0xFFFFC76B,0xFF99D9FF,0xFFC5A3FF,0xFFFFB1CA,0xFFAAB6FF,0xFF86EDCF};
    static int slot(int item) {return item/3;}
    static int achievement(int item) {
        for(int i=0;i<Achievements.size();i++)if(Achievements.id(i).equals(ACHIEVEMENT[item]))return i;
        throw new IllegalStateException("Missing accessory achievement: "+ID[item]);
    }
    static String key(String field,int item) {return "accessory_"+field+"_"+ID[item];}
}
