package com.dddumpling.game;

/** The eleven independently ranked boards configured in issue #237. */
final class LeaderboardCatalog {
    static final int SURVIVAL_STARTER=0,SURVIVAL_FULL=1,SURVIVAL_KIDS=2,
            TIME_ATTACK_BASE=3,TIME_ATTACK_ALL=9,BOSS_FIRST=3,ALL_BOSSES=9,ADVENTURE=10,COUNT=11;
    private static final String[] APPLE={
        "survival_time_starter","survival_time_full","survival_time_kids",
        "time_attack_slime_full","time_attack_dark_divide_full","time_attack_octopulse_full",
        "time_attack_fly_agaric_full","time_attack_mossy_maw_full","time_attack_rainbow_cat_full",
        "time_attack_all_6_full","adventure_score_normal"
    };
    private static final String[] GOOGLE={
        "CgkIm8qy89IBEAIQgQE","CgkIm8qy89IBEAIQggE","CgkIm8qy89IBEAIQgwE",
        "CgkIm8qy89IBEAIQhAE","CgkIm8qy89IBEAIQhQE","CgkIm8qy89IBEAIQhgE",
        "CgkIm8qy89IBEAIQhwE","CgkIm8qy89IBEAIQiAE","CgkIm8qy89IBEAIQiQE",
        "CgkIm8qy89IBEAIQigE","CgkIm8qy89IBEAIQiwE"
    };
    static boolean valid(int board) {return board>=0 && board<COUNT;}
    static boolean timedClear(int board) {return board>=BOSS_FIRST && board<=ALL_BOSSES;}
    static String appleId(int board) {return valid(board)?"com.dddumpling.game."+APPLE[board]:null;}
    static String googleId(int board) {return valid(board)?GOOGLE[board]:null;}
    static int survival(int profile) {return profile>=0 && profile<3?profile:-1;}
    static int timeAttack(int profile,int challenge) {
        if(profile!=Survival.FULL_PROFILE)return -1;
        if(challenge>=0 && challenge<6)return BOSS_FIRST+challenge;
        return Boss.COUNT==6 && challenge==TimeAttack.ALL?ALL_BOSSES:-1;
    }
    // Match displayed whole seconds/centiseconds without rounding a result into a better rank.
    static long appleScore(int board,long value) {
        return board<3?value/1000:timedClear(board)?value/10+(value%10==0?0:1):value;
    }
    static long googleScore(int board,long value) {return value;}
    static boolean better(int board,long value,long old) {
        return valid(board) && value>0 && (old<=0 || (timedClear(board)?value<old:value>old));
    }
    private LeaderboardCatalog() {}
}
