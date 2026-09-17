package com.dddumpling.game;

/**
 * Geometry and hit-testing for the settings panel, in one pure-Java place so the renderer
 * and the touch handler cannot drift apart about where the controls are.
 */
final class SettingsUi {

    static final int HIT_NONE = 0, HIT_SLIDER = 1, HIT_CLOSE = 2, HIT_OUTSIDE = 3,
            HIT_CLEAR = 4, HIT_ROSTER = 5, HIT_GAMEOVER = 6, HIT_RESET_DIFFICULTY = 7, HIT_RESET_LANDS = 8,
            HIT_GENERAL = 9, HIT_MINIGAMES = 10, HIT_EASIER = 11, HIT_HARDER = 12, HIT_RESET_NEWS = 13, HIT_ALL_LANDS = 14;
    static final int GENERAL = 0, MINIGAMES = 1, POWERS = 2, PROGRESS = 3;
    static final int HIT_POWERS = 15, HIT_PROGRESS = 16;
    static final String[] TABS = {"RUN", "MINIGAMES", "POWERS", "PROGRESS"};
    /** Power shortcuts and minigame shortcuts share action IDs, not a row. */
    static final int HIT_TEST = 200;
    static final int HIT_DEBUFF = 250;
    /** All playtest actions: frenzy modes and all four minigames. */
    static final int TEST_CHIPS = Power.OFFERED.length + 4;
    /** Star Path action offset. */
    static final int TEST_STARS = Power.OFFERED.length;
    /** Steamer action offset. */
    static final int TEST_STEAMER = Power.OFFERED.length + 1;
    static final int TEST_BAND = Power.OFFERED.length + 2, TEST_MINE = Power.OFFERED.length + 3;
    /** Stage-jump steppers are HIT_STAGE + index into {@link #STAGE_STEP}. */
    static final int HIT_STAGE = 300;

    /**
     * What each stage-jump chip moves by.
     *
     * Five as well as one because a boss lands on every fifth stage, so a jump of five is a jump to
     * the next boss of the next kind — which is the reason anybody wants this control. The chips are
     * labelled with these numbers, so the array is the label too and the two cannot disagree.
     */
    static final int[] STAGE_STEP = {-5, -1, 1, 5};

    float panelL, panelT, panelR, panelB;
    float titleY;
    float tabY, tabH;
    float speedLabelY;
    float sliderL, sliderR, sliderY, sliderH;
    float speedValueY;
    float sliderHitH;
    float closeCx, closeCy, closeR;
    /** Playtest row: one chip per powerup mode. */
    float testLabelY, testY, testH, caveY, debuffY;
    /** Stage-jump row: one chip per {@link #STAGE_STEP}. */
    float stageLabelY, stageY, stageH;
    /** Next-run roster toggle and end-current-run button. */
    float runLabelY, runY, runH;
    /** Reset for persistent difficulty progression. */
    float difficultyLabelY, difficultyY, difficultyH;
    /** Empty-the-display-case button, at the foot of the panel. */
    float clearLabelY, clearY, clearH;

    private int tab;

    void compute(Layout L) {
        compute(L, GENERAL);
    }

    void compute(Layout L, int selectedTab) {
        tab=selectedTab;
        float s=PlayerSettings.unit(L);
        panelL=PlayerSettings.left(L); panelR=PlayerSettings.right(L);
        panelT=PlayerSettings.top(L); panelB=PlayerSettings.bottom(L);
        titleY=panelT+s*6.7f; tabY=panelT+s*5.5f; tabH=s*1.8f;
        closeR=s; closeCx=panelR-s*1.3f; closeCy=panelT+s*1.5f;
        sliderL=optionL(); sliderR=optionR(); sliderH=s*.5f;
        sliderHitH=s*1.65f; testH=stageH=runH=clearH=difficultyH=s*2f;
        speedLabelY=panelT+s*9f; sliderY=panelT+s*10.3f; speedValueY=panelT+s*12f;
        stageLabelY=panelT+s*15f; stageY=stageLabelY+s*.5f;
        runLabelY=panelT+s*19f; runY=runLabelY+s*.5f;
        testLabelY=panelT+s*9f; testY=testLabelY+s*.5f;
        debuffY=panelT+s*14f;
        difficultyLabelY=panelT+s*20f; difficultyY=difficultyLabelY+s*.5f;
        clearLabelY=panelT+s*24f; clearY=clearLabelY+s*.5f;
        if(tab==MINIGAMES) {
            testY=panelT+s*16f; testLabelY=testY-s*.6f; caveY=testY+s*3f;
            difficultyLabelY=panelT+s*24f; difficultyY=difficultyLabelY+s*.5f;
            clearLabelY=panelT+s*28f;
        }
    }

    /** Left edge of playtest chip {@code i} of {@code n}. */
    float testChipL(int i, int n) {
        float w = (optionR() - optionL()) / n;
        return optionL() + w * i;
    }

    float testChipR(int i, int n) {
        return testChipL(i, n) + (optionR() - optionL()) / n - 6f;
    }

    /** Shared horizontal bounds for controls. */
    float optionL() {
        return panelL + Layout.SQ3_2 * 0f + (panelR - panelL) * 0.06f;
    }

    float optionR() {
        return panelR - (panelR - panelL) * 0.06f;
    }

    /** Where the slider knob sits for a given speed. */
    float knobX(float speed) {
        float t = (speed - GameCore.SPEED_MIN) / (GameCore.SPEED_MAX - GameCore.SPEED_MIN);
        return sliderL + (sliderR - sliderL) * t;
    }

    /** Speed implied by a touch at {@code x}, clamped to the legal range. */
    float speedAt(float x) {
        float t = (x - sliderL) / (sliderR - sliderL);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        // Snap to a twentieth, so the value is reachable and readable.
        float v = GameCore.SPEED_MIN + t * (GameCore.SPEED_MAX - GameCore.SPEED_MIN);
        return Math.round(v * 20f) / 20f;
    }

    int hit(float x, float y) {
        if (!BuildFlags.DEVELOPER) return HIT_NONE;
        if (x<panelL || x>panelR || y<panelT || y>panelB) return HIT_OUTSIDE;
        if (Math.abs(x-closeCx)<closeR && Math.abs(y-closeCy)<closeR) return HIT_CLOSE;
        if(y>=tabY && y<=tabY+tabH) for(int i=0;i<4;i++)
            if(x>=tabL(i) && x<=tabR(i)) return new int[]{HIT_GENERAL,HIT_MINIGAMES,HIT_POWERS,HIT_PROGRESS}[i];
        if(tab==MINIGAMES) {
            if(y>=sliderY && y<=sliderY+testH) {
                if(inChip(x,0,3)) return HIT_EASIER;
                if(inChip(x,2,3)) return HIT_HARDER;
            }
            if(y>=testY && y<=testY+testH) {
                if(inChip(x,0,2)) return HIT_TEST+TEST_STARS;
                if(inChip(x,1,2)) return HIT_TEST+TEST_STEAMER;
            }
            if(y>=caveY && y<=caveY+testH) {
                if(inChip(x,0,2)) return HIT_TEST+TEST_BAND;
                if(inChip(x,1,2)) return HIT_TEST+TEST_MINE;
            }
            if(y>=difficultyY && y<=difficultyY+difficultyH) return HIT_RESET_DIFFICULTY;
        } else if(tab==POWERS) {
            if(y>=testY && y<=testY+testH) for(int i=0;i<Power.OFFERED.length;i++)
                if(inChip(x,i,Power.OFFERED.length)) return HIT_TEST+i;
            if(y>=debuffY && y<=debuffY+testH) for(int i=0;i<2;i++)
                if(inChip(x,i,2)) return HIT_DEBUFF+i;
        } else if(tab==PROGRESS) {
            if(y>=debuffY && y<=debuffY+testH) {
                if(inChip(x,0,2)) return HIT_ALL_LANDS;
                if(inChip(x,1,2)) return HIT_RESET_LANDS;
            }
            if(y>=difficultyY && y<=difficultyY+difficultyH) return HIT_RESET_NEWS;
            if(y>=clearY && y<=clearY+clearH) return HIT_CLEAR;
        } else {
            if(Math.abs(y-sliderY)<=sliderHitH*.55f) return HIT_SLIDER;
            if(y>=stageY && y<=stageY+stageH) for(int i=0;i<STAGE_STEP.length;i++)
                if(inChip(x,i,STAGE_STEP.length)) return HIT_STAGE+i;
            if(y>=runY && y<=runY+runH) {
                if(inChip(x,0,2)) return HIT_ROSTER;
                if(inChip(x,1,2)) return HIT_GAMEOVER;
            }
        }
        return HIT_NONE;
    }
    private boolean inChip(float x,int i,int n) { return x>=testChipL(i,n) && x<=testChipR(i,n); }
    float tabL(int i) { return optionL()+i*(optionR()-optionL())/4f; }
    float tabR(int i) { return tabL(i+1)-4f; }
}
