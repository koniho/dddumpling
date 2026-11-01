package com.dddumpling.game;

import java.util.Random;

final class TestRoster extends Check {
    static void adaptive(Layout L) {
        group("adaptive key roster");
        Mem fresh = new Mem(); fresh.rosterState = 0;
        GameCore c = new GameCore(fresh, 44);
        check("a first run has four keys", !c.fullRoster && Roster.count(c.fullRoster) == 4);
        check("starter cast is active", c.keyActive(0) && c.keyActive(1) && c.keyActive(4) && c.keyActive(5));
        check("cat and grapes begin off the deck", !c.keyActive(2) && !c.keyActive(3));
        check("hidden key cannot be hit", c.keyAt(L.keyX[2], L.keyY[2], L) == -1);
        check("four-key deck is enlarged", c.keyScale() > 1.1f);
        for (int n = 0; n < 500; n++) {
            GameCore.Enemy e = new GameCore.Enemy();
            Words.fill(e, 5, 0.5f, new Random(n), false);
            for (int g : e.word) check("starter words never request a hidden key", Roster.active(false, g));
        }
        c.startGame(); c.stage = Boss.EVERY;
        c.boss.begin(Boss.SLIME, c.stage, c.rnd, false); c.boss.beaten = true;
        BossPlay.endBoss(c, L);
        Interlude.beginStageEnd(c);
        check("first boss alone does not add keys", !c.fullRoster);
        strongStage(c,L,6,9,1,0,false);
        strongStage(c,L,7,9,1,0,false);
        check("two strong stages still use four keys",!c.runFullRoster && c.rosterStrongStages==2);
        strongStage(c,L,8,9,1,0,false);
        check("three strong stages remove one level of help",c.runDifficultyTier==Pacing.LIGHT_HELP && !c.fullRoster);
        strongStage(c,L,9,9,1,0,false);strongStage(c,L,11,9,1,0,false);strongStage(c,L,12,9,1,0,false);
        check("sustained strong play reaches normal and adds cat and grapes", c.fullRoster && c.rosterScene == GameCore.ROSTER_JOIN);
        check("join is saved immediately", (fresh.rosterState & 1) != 0);
        advance(c, L, GameCore.ROSTER_SCENE_TIME + 0.1f);
        check("six-key deck settles at normal size", c.rosterScene == 0 && Math.abs(c.keyScale() - 1f) < 0.01f);

        Mem losses = new Mem(); losses.rosterState = 1;
        GameCore loss = new GameCore(losses, 80); loss.startGame(); loss.lives = 1;
        loss.takeHit(L.w / 2f, L);
        check("one early loss removes advanced keys", (losses.rosterState & 1) == 0);
        check("loss keeps finished run roster for its summary",loss.runFullRoster);
        check("farewell survives until title", (losses.rosterState & 8) != 0);
        GameCore farewell = new GameCore(losses, 90);
        check("pending farewell starts on title", farewell.rosterScene == GameCore.ROSTER_LEAVE && farewell.rosterMix() > 0.99f);
        advance(farewell, L, GameCore.ROSTER_SCENE_TIME + 0.1f);
        check("farewell leaves the four-key deck", farewell.rosterScene == 0 && !farewell.fullRoster);
        check("farewell is one shot", (losses.rosterState & 8) == 0);

        Mem safe = new Mem(); safe.rosterState = 1 | (2 << 1);
        GameCore reached = new GameCore(safe, 100); reached.startGame(); reached.enterStage(3);
        reached.lives=1;reached.takeHit(L.w/2,L);
        check("later loss does not remove six keys or read legacy loss bits",reached.fullRoster && (safe.rosterState&1)!=0);
        GameCore later=new GameCore(new Mem(),102);later.fullRoster=true;
        later.collected=Collect.add(later.collected,Collect.bossPrize(Lands.unlockBoss(1)));later.landChoice=1;later.startGame();
        check("later-land fallback is measured from selected start",later.stage==Lands.firstStage(1) && later.rosterStartStage==later.stage);
        later.lives=1;later.takeHit(L.w/2,L);check("early later-land loss also falls back",!later.fullRoster);
        GameCore newSix=fourKeyGame(103);newSix.startGame();newSix.enterStage(8);newSix.unlockRoster();
        newSix.enterStage(9);newSix.finishDefeat();check("early ordinary stage after promotion still has fallback",!newSix.fullRoster);
        promotionRules(L);
        assistanceRules(L);

        GameCore choice = new GameCore(new Mem(), 101); choice.startGame();
        boolean current = choice.runFullRoster;
        choice.setNextRoster(!current);
        check("settings choice changes only the next run", choice.runFullRoster == current
                && choice.fullRoster != current);
        choice.toTitle(); choice.startGame();
        check("next run adopts the chosen roster", choice.runFullRoster == choice.fullRoster);

        SettingsUi ui = new SettingsUi(); ui.compute(L);
        float mid = (ui.optionL() + ui.optionR()) / 2f;
        float left = (ui.optionL() + mid) / 2f, right = (mid + ui.optionR()) / 2f;
        check("next-roster chip is hittable", ui.hit(left, ui.runY + ui.runH / 2f)
                == SettingsUi.HIT_ROSTER);
        check("end-run button is hittable", ui.hit(right, ui.runY + ui.runH / 2f)
                == SettingsUi.HIT_GAMEOVER);
        choice.openSettings(); choice.endCurrentRun();
        check("end-run uses game over and closes settings", choice.state == GameCore.OVER
                && !choice.settingsOpen && choice.deathT == GameCore.DEATH_TIME);
    }
    private static GameCore fourKeyGame(int seed) {
        Mem save=new Mem();save.rosterState=0;return new GameCore(save,seed);
    }
    private static void strongStage(GameCore c,Layout L,int stage,int correct,int misses,int damage,boolean rescue) {
        c.pendingBonus=false;c.enterStage(stage);c.hits+=correct;
        c.missesThisStage=misses;c.hurtThisStage=damage;c.pushUsed=rescue;
        Interlude.beginStageEnd(c);
    }
    private static void promotionRules(Layout L) {
        GameCore c=fourKeyGame(104);c.startGame();
        for(int n=1;n<=3;n++)strongStage(c,L,n,10,0,0,false);
        check("strong play cannot skip first boss milestone",c.runDifficultyTier==Pacing.LIGHT_HELP && !c.fullRoster);
        c.collected=Collect.add(c.collected,Collect.bossPrize(Boss.SLIME));
        c.rosterStrongStages=1;
        strongStage(c,L,4,8,1,0,false);
        check("borderline accuracy holds confidence",c.rosterStrongStages==1 && !c.fullRoster);
        strongStage(c,L,6,10,0,0,false);strongStage(c,L,7,10,0,1,false);
        check("damage subtracts one point at stage end",c.rosterStrongStages==1 && !c.fullRoster);
        strongStage(c,L,8,10,0,0,true);
        check("rescue does not penalize strong play",c.rosterStrongStages==2 && !c.fullRoster);
        strongStage(c,L,9,0,0,0,false);
        check("empty stage holds confidence",c.rosterStrongStages==2);
        c.enterStage(11);c.startFrenzy(Power.NINJA,L);c.mode=-1;c.modeLeft=0;c.hits+=20;
        c.hurtThisStage=1;c.assessRosterStage();
        check("power usage holds even after power ends and damage",c.rosterStrongStages==2 && c.runDifficultyTier==Pacing.LIGHT_HELP);
        c.enterStage(12);c.hits+=20;c.hurtThisStage=0;c.missesThisStage=0;c.assessRosterStage();
        check("confidence accumulates across held stages",c.fullRoster && c.runDifficultyTier==Pacing.NORMAL);
        c.rosterStrongStages=2;c.enterStage(15);c.takeHit(L.w/2,L);
        check("boss damage leaves ordinary confidence alone",c.rosterStrongStages==2);
        c.assessRosterStage();check("boss evaluation also holds confidence",c.rosterStrongStages==2);
        c.finishDefeat();check("boss defeat does not lower ordinary difficulty",c.difficultyTier==Pacing.NORMAL);
        GameCore control=fourKeyGame(107);control.startGame();control.enterStage(6);
        GameCore.Enemy observed=new GameCore.Enemy();observed.y=L.playTop+(L.dangerY-L.playTop)*.3f;
        control.observeRosterEnemy(observed,L);control.hits+=17;control.missesThisStage=2;control.assessRosterStage();
        check("comfortable field control qualifies below ninety percent without telemetry",control.rosterStrongStages==1 && !control.playthrough.enabled);
        control.enterStage(7);observed.sideEntry=true;observed.y=L.dangerY;
        control.observeRosterEnemy(observed,L);control.assessRosterStage();
        check("side entries alone do not establish field control",control.rosterStrongStages==1);
        control.enterStage(8);observed.sideEntry=false;control.observeRosterEnemy(observed,L);
        observed.y=L.playTop;control.observeRosterEnemy(observed,L);control.assessRosterStage();
        check("earlier halfway crossing persists when enemies return upward",control.rosterStrongStages==1);
        control.runDifficultyTier=control.difficultyTier=Pacing.LIGHT_HELP;
        control.enterStage(9);control.startFrenzy(Power.NINJA,L);control.finishDefeat();
        check("power stage defeat also holds assistance",control.difficultyTier==Pacing.LIGHT_HELP);
        c.rosterStrongStages=2;c.toTitle();c.startGame();
        check("new run needs fresh sustained performance",c.rosterStrongStages==0);
        c=fourKeyGame(108);c.preferences.kids=true;c.startGame();
        for(int n=1;n<=4;n++)strongStage(c,L,n,10,0,0,false);
        check("kids mode remains four keys after strong play",!c.runFullRoster && !c.fullRoster);
        for(int mode=0;mode<2;mode++) {
            GameCore other=fourKeyGame(105+mode);other.startGame();
            other.collected=Collect.add(other.collected,Collect.bossPrize(Boss.SLIME));
            other.survival.active=mode==0;other.timeAttack.active=mode==1;
            other.rosterStrongStages=2;
            strongStage(other,L,1,10,0,0,false);
            check("other mode cannot promote Adventure "+mode,!other.fullRoster);
            other.runFullRoster=other.fullRoster=true;other.finishDefeat();
            check("other mode cannot demote Adventure "+mode,other.fullRoster);
        }
    }

    private static void assistanceRules(Layout L) {
        GameCore c=fourKeyGame(220);c.startGame();
        check("new players start with extra help",c.runDifficultyTier==Pacing.EXTRA_HELP);
        boolean bounded=true,spawns=true,words=true,rng=true;
        for(int stage:new int[]{1,4,6,16,26,31,42})for(int tier=0;tier<=Pacing.NORMAL;tier++) {
            c.stage=stage;c.runDifficultyTier=tier;
            bounded &= c.maxPresses()<=Pacing.MAX_PRESSES && c.maxWordLen()<=Pacing.maxWordLen(stage)
                    && c.minWordLen()<=c.maxWordLen() && c.traversalRate()<=1;
            spawns &= c.spawnInterval()==Pacing.spawnInterval(stage) && c.maxEnemies()==Pacing.maxEnemies(stage)
                    && c.stageQuota()==Pacing.stageQuota(stage) && c.ramp()==Pacing.ramp(stage);
            for(int seed=0;seed<40;seed++) {
                Random assisted=new Random(seed),normal=new Random(seed);
                GameCore.Enemy e=new GameCore.Enemy(),reference=new GameCore.Enemy();
                Words.fill(e,c.maxWordLen(),c.stackChance(),assisted,false,c.maxPresses(),c.distinctEnemyKeys());
                Words.fill(reference,c.maxWordLen(),c.stackChance(),normal,false,c.maxPresses());
                int mask=0,presses=0;
                for(int i=0;i<e.word.length;i++) {
                    mask|=1<<e.word[i];presses+=e.need[i];
                    words &= Roster.active(false,e.word[i]);
                    if(i>0 && (e.need[i]>1 || e.need[i-1]>1))words &= e.word[i]!=e.word[i-1];
                }
                words &= Integer.bitCount(mask)<=c.distinctEnemyKeys() && presses<=c.maxPresses();
                rng &= assisted.nextLong()==normal.nextLong();
                if(tier==Pacing.NORMAL)words &= java.util.Arrays.equals(e.word,reference.word);
            }
        }
        check("assistance is bounded by current word and speed ramp",bounded);
        check("all tiers preserve spawn intervals caps quotas and power ramp",spawns);
        check("assisted words obey distinct symbols press caps and stack readability",words);
        check("symbol simplification preserves RNG consumption",rng);
        c.runDifficultyTier=Pacing.NORMAL;c.stage=6;c.rosterStrongStages=0;
        strongStage(c,L,6,20,0,1,false);
        check("one damaged wave subtracts confidence without changing tier",c.runDifficultyTier==Pacing.NORMAL && c.rosterStrongStages==-1);
        strongStage(c,L,7,20,0,1,false);strongStage(c,L,8,20,0,1,false);
        check("repeated damage adds one tier of assistance",c.runDifficultyTier==Pacing.LIGHT_HELP && c.rosterStrongStages==0);
        strongStage(c,L,9,20,0,0,true);
        check("rescue earns confidence without reducing spawns",c.runDifficultyTier==Pacing.LIGHT_HELP && c.rosterStrongStages==1 && c.spawnInterval()==Pacing.spawnInterval(9));
        GameCore restored=new GameCore(c.store,221);restored.startGame();
        check("assistance survives restart but confidence does not",restored.runDifficultyTier==Pacing.LIGHT_HELP && restored.rosterStrongStages==0);
        float before=restored.traversalRate();restored.takeHit(L.w/2,L);
        check("damage does not change movement speed mid-stage",restored.traversalRate()==before);
        restored.runDifficultyTier=Pacing.EXTRA_HELP;restored.survival.active=true;
        check("Survival bypasses Adventure assistance",restored.traversalRate()==1 && restored.distinctEnemyKeys()==Glyph.COUNT);
        restored.survival.active=false;restored.timeAttack.active=true;
        check("Time Attack bypasses Adventure assistance",restored.traversalRate()==1);
        restored.timeAttack.active=false;restored.kidsRun=true;
        check("Kids retains its existing traversal and press ceiling",restored.traversalRate()==.45f && restored.maxPresses()==6);
    }

}
