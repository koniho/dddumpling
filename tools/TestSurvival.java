package com.dddumpling.game;

final class TestSurvival extends Check {
    static Mem store() {
        Mem m=new Mem();m.collected=1L|(1L<<Collect.BOSS_FIRST);m.tutorials|=Onboarding.SKIPPED;
        m.best=789;return m;
    }
    static GameCore start(Mem store,long seed) {
        GameCore c=new GameCore(store,seed,true);c.landSeen=LandPicker.stateMask();
        c.landChoice=1;c.fullRoster=false;c.modes.selected=ModeSelector.SURVIVAL;
        c.startGame();return c;
    }
    static void all(Layout L) {
        Mem m=store();GameCore c=start(m,149);
        check("Survival starts from normal selector",c.state==GameCore.PLAY && c.survival.active && c.stage==1);
        check("Survival preserves Adventure land choice",c.landChoice==1 && c.runStartLand==0 && c.best==0);
        long prizes=c.collected;String history=m.highScores;
        c.update(.01f,.25f,L);double before=c.survival.seconds;
        check("Survival counts active elapsed time",Math.abs(before-.25)<.001);
        c.slowdown=1;c.update(.01f,.25f,L);
        check("slow motion does not slow the record clock",Math.abs(c.survival.seconds-.5)<.001);
        Pause.open(c);before=c.survival.seconds;c.update(1,1,L);
        check("pause excludes elapsed time",c.survival.seconds==before);
        Pause.resume(c);PlayerSettings.open(c);c.update(1,1,L);
        check("settings exclude elapsed time",c.survival.seconds==before);c.closeSettings();
        c.spawnedThisStage=c.stageQuota();c.enemies.clear();c.shots.clear();
        check("clearing Adventure quota cannot end Survival",!c.stageCleared());
        c.spawnTimer=0;c.update(DT,L);
        check("Survival spawns beyond the ordinary quota",c.liveEnemies()>0 && !c.pendingBonus && !c.boss.active());
        GameCore.Enemy e=c.enemies.get(0);c.spawnTimer=10;
        c.destroyWord(e,e.baseX,e.y,L);
        check("ordinary clear requests immediate replacement",c.spawnTimer<=.18f);
        c.startFrenzy(Power.NINJA,L);c.modeLeft=.001f;c.update(DT,L);
        check("ending power keeps Survival continuous",!c.powerActive() && !c.stageByPower && !c.pendingBonus && !c.stageCleared());
        c.companion.left=0;
        c.pushUsed=true;c.survival.rescueLeft=29.9f;c.update(.01f,.2f,L);
        check("rescue recharges without an interlude",!c.pushUsed && c.survival.rescueLeft==0);
        check("rescue ready glow and companion reaction announce recharge",c.survival.rescueReadyFlash==1
                && c.survival.rescueCharge(c)==1 && c.companion.reaction==RunCompanion.WORD);
        c.pushUsed=true;c.survival.rescueLeft=15;c.survival.update(c,.01f);
        check("rescue ring shows partial charge and clears ready glow",Math.abs(c.survival.rescueCharge(c)-.5f)<.001f
                && c.survival.rescueReadyFlash==0);
        Pause.open(c);float charge=c.survival.rescueCharge(c);c.update(10,10,L);
        check("paused rescue indicator cannot advance",c.survival.rescueCharge(c)==charge);Pause.resume(c);
        int calm=c.survival.background();float travel=c.travelSeconds(),spawn=c.spawnInterval();
        c.survival.seconds=300;c.survival.update(c,.01f);
        check("difficulty and distinct sky intensify together",c.travelSeconds()<travel && c.spawnInterval()<spawn
                && c.survival.background()!=calm && c.maxEnemies()==5 && c.stage==16);
        c.survival.seconds=1000;c.survival.update(c,.01f);
        check("difficulty has a bounded ceiling",c.stage==19 && c.travelSeconds()==8f && c.maxEnemies()==5);
        c.score=1234;c.lives=1;c.takeHit(L.w*.5f,L);
        check("death saves only Survival records",c.state==GameCore.OVER && c.survival.finished
                && m.best==789 && m.highScores.equals(history) && (c.collected & Collect.LEGACY_MASK)==prizes && c.survival.reward.pending
                && c.progress.maximum("highest_stage")==0 && c.townRunTickets==0);
        c.dismissGameOver();
        check("Survival death animation cannot be dismissed early",c.state==GameCore.OVER && c.returnFade==0);
        int profile=c.survival.profile;long record=c.survival.bestTime[profile];before=c.survival.seconds;
        c.update(10,10,L);
        check("death animation cannot increase time",c.survival.seconds==before && c.survival.bestTime[profile]==record);
        GameCore loaded=new GameCore(m,150,true);
        check("Survival records persist separately",loaded.survival.bestTime[profile]==record && loaded.survival.bestScore[profile]==1234);
        c.dismissGameOver();
        c.update(2,2,L);c.dismissGameOver();
        check("Survival dismissal begins the return fade without retry",c.returnFade>0 && !c.starting());
        c.update(GameCore.RETURN_FADE,GameCore.RETURN_FADE,L);
        check("return restores Adventure progress and keeps mode choice",c.state==GameCore.TITLE && !c.starting() && c.landChoice==1
                && c.best==c.landBests[1] && !c.survival.active && c.modes.selected==ModeSelector.SURVIVAL);
        c.startGame();check("new run resets time and transient state",c.survival.active && c.survival.seconds==0 && !c.pushUsed && c.stage==1);
        c.toTitle();
        c.modes.back(c);c.startGame();check("Adventure still starts at its selected land",!c.survival.active && c.stage==6);
        ducks(L);records(L);history(L);linkedAfterMinute(L);effects(L);powerRack(L);backgroundEntrance(L);backgroundCoverage();backgroundExit();endingSkits(L);bounded(L);fuzz(L);
    }
    private static java.util.ArrayList<float[]> stripes(GameCore c,Layout L) {
        java.util.ArrayList<float[]> bodies=new java.util.ArrayList<>();
        Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)-> {
                    if(method.getName().equals("fillRect"))bodies.add(new float[]{(Float)args[0],(Float)args[1],
                            (Float)args[2],(Float)args[3],(Integer)args[4]});
                    return null;
                });
        c.survival.scenery(painter,c,L);return bodies;
    }
    private static void backgroundExit() {
        for(int[] size:new int[][]{{320,568},{640,1400}})for(float age:new float[]{.5f,3,150,300}) {
            Layout L=new Layout();L.compute(size[0],size[1],0,0,0,0);
            GameCore c=start(store(),171);c.survival.update(c,age);
            java.util.ArrayList<float[]> original=stripes(c,L);
            c.lives=1;c.takeHit(L.w*.5f,L);
            java.util.ArrayList<float[]> initial=stripes(c,L);
            boolean continuous=initial.size()==original.size();
            for(int i=0;i<initial.size() && i<original.size();i++)continuous &= java.util.Arrays.equals(initial.get(i),original.get(i));
            check("Survival stripes keep position and color at death "+size[0]+"/"+age,continuous);
            float phase=c.survival.skyPhase;double seconds=c.survival.seconds;
            boolean departing=true;
            for(int step=0;step<4;step++) {
                c.update(c.deathDuration()/4,c.deathDuration()/4,L);
                for(float[] stripe:stripes(c,L)) {
                    boolean found=false;
                    for(float[] old:original)if(stripe[0]==old[0] && stripe[2]==old[2] && stripe[4]==old[4]
                            && stripe[1]>old[1] && Math.abs((stripe[3]-stripe[1])-(old[3]-old[1]))<.01f)found=true;
                    departing &= found;
                }
            }
            check("only original stripes slide down with their colors "+size[0]+"/"+age,departing);
            check("all stripes leave before summary "+size[0]+"/"+age,stripes(c,L).isEmpty()
                    && c.survival.skyPhase==phase && c.survival.seconds==seconds);
            c.toTitle();c.startGame();
            check("next Survival run clears exit progress "+size[0]+"/"+age,c.drained()==0 && c.survival.entrance()==0);
        }
    }
    private static void endingSkits(Layout L) {
        check("every rainbow blurb has a skit",SurvivalEnd.COUNT==HighScores.SURVIVAL_BLURBS.length);
        GameCore c=start(store(),172);c.survival.update(c,90);c.lives=1;c.takeHit(L.w*.5f,L);
        int ending=c.survival.ending;long duration=c.survival.history().latestRun.duration;
        String saved=c.survival.encode();
        java.util.HashSet<Integer> scenes=new java.util.HashSet<>();
        final int[] signature={1},calls={0};
        Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)-> {
                    signature[0]=31*signature[0]+method.getName().hashCode();calls[0]++;
                    if(args!=null)for(Object arg:args)signature[0]=31*signature[0]+(arg instanceof float[]?
                            java.util.Arrays.hashCode((float[])arg):arg==null?0:arg.hashCode());
                    return null;
                });
        check("Survival skit owns the companion during game over",SurvivalEnd.ownsCompanion(c));
        float[] home=SurvivalEnd.pose(c,L);
        check("washed-away companion starts at its home",home[0]==RunCompanion.x(L) && home[1]==RunCompanion.y(L));
        for(int scene=0;scene<SurvivalEnd.COUNT;scene++) {
            c.survival.ending=scene;c.deathT=c.deathDuration()*.5f;
            signature[0]=1;SurvivalEnd.draw(painter,c,L);int first=signature[0];scenes.add(first);
            signature[0]=1;SurvivalEnd.draw(painter,c,L);
            check("rainbow skit renders deterministically "+scene,signature[0]==first);
        }
        check("all twenty rainbow skits have distinct drawings",scenes.size()==SurvivalEnd.COUNT);
        c.survival.ending=ending;c.deathT=0;calls[0]=0;SurvivalEnd.draw(painter,c,L);
        float[] departed=SurvivalEnd.pose(c,L);
        check("skit and companion are gone before the summary",calls[0]==0 && departed[1]-departed[2]*3>L.h);
        check("skit drawing preserves the recorded run and ending",saved.equals(c.survival.encode())
                && c.survival.history().latestRun.duration==duration);
        c.toTitle();check("title no longer owns a death skit",!SurvivalEnd.ownsCompanion(c));
    }
    private static void ducks(Layout L) {
        group("Survival duck rewards and migration");
        int[] base={0,2,4,5,6,7,8};
        java.util.Random random=new java.util.Random(156);
        for(int segment=0;segment<7;segment++) {
            int seen=0;
            double end=segment==6?300:(segment+1)*45;
            for(double time:new double[]{segment*45,end-.001})for(int n=0;n<40;n++) {
                int pick=DuckReward.choose(time,random)-Collect.DUCK_FIRST;
                check("duck belongs to final segment "+segment,pick==base[segment] || pick==base[segment]+1);
                seen|=1<<(pick-base[segment]);
            }
            check("both pool ducks can be selected "+segment,seen==3);
        }
        for(double time:new double[]{300,300.001,900})
            check("five-minute champion replaces pool",DuckReward.choose(time,random)==Collect.COUNT-1);
        Mem m=store();GameCore c=start(m,156);c.survival.seconds=300;c.score=50;
        c.lives=1;c.takeHit(L.w*.5f,L);
        int who=c.survival.reward.who;long sequence=c.survival.reward.sequence;
        check("one saved duck without changing score",who==Collect.COUNT-1 && c.collectionCounts[who]==1
                && c.score==50 && c.survival.history().latestRun.prizes.length==1);
        String journal=m.survivalAward;
        c.survival.finish(c);c.dismissGameOver();
        check("early taps and repeated finish cannot reroll",m.survivalAward.equals(journal) && c.collectionCounts[who]==1);
        // Simulate loss of all writes after the atomic journal commit.
        m.collected=store().collected;m.collectionCounts=new int[Collect.COUNT];m.survival="";m.progress=null;
        GameCore recovered=new GameCore(m,157,true);
        check("journal recovers ownership record and reveal",recovered.state==GameCore.OVER
                && recovered.survival.reward.pending && recovered.collectionCounts[who]==1
                && recovered.survival.history().latestRun.prizes[0]==who && recovered.score==50
                && recovered.survival.adventureLand==1);
        GameCore again=new GameCore(m,158,true);
        check("recovery does not duplicate the cloud reward",again.collectionCounts[who]==1
                && again.progress.count(Collect.progressKey(who))==1 && again.survival.reward.sequence==sequence);
        again.dismissGameOver();again.update(2,2,L);again.dismissGameOver();again.update(1,1,L);
        GameCore done=new GameCore(m,159,true);
        check("acknowledged reveal does not reopen",done.state==GameCore.TITLE && !done.survival.reward.pending
                && Collect.has(done.collected,who));
        c=start(m,160);c.survival.seconds=300;c.score=70;c.lives=1;c.takeHit(L.w*.5f,L);
        check("duplicates count once and give no score bonus",c.collectionCounts[who]==2 && !c.survival.reward.fresh && c.score==70);
        Mem quitStore=store();GameCore quit=start(quitStore,161);quit.survival.seconds=350;quit.toTitle();
        check("abandoning Survival earns no duck",quit.survival.history().latestRun.prizes.length==0
                && quitStore.survivalAward.isEmpty());
        Mem legacy=store();legacy.collected=(1L<<59)-1;legacy.collectionCounts=new int[59];
        legacy.collectionCounts[58]=9;legacy.caseIndex=58;
        GameCore migrated=new GameCore(legacy,162,true);
        check("retired cave bits never become ducks",(migrated.collected & ~Collect.LEGACY_MASK)==0
                && migrated.collectionCounts[58]==0 && migrated.caseIndex==0);
        try {
            ProgressData oldCloud=new ProgressData();oldCloud.increment("old","prize_58",7);
            migrated.progress.restore(oldCloud.encode(),migrated);
            check("retired cloud IDs cannot resurrect as ducks",migrated.collectionCounts[58]==0);
            ProgressData ducks=new ProgressData();ducks.increment("other","duck_10",2);
            migrated.progress.restore(ducks.encode(),migrated);
            GameCore cloudReload=new GameCore(legacy,163,true);
            check("cloud ducks survive native mask round trip",cloudReload.collectionCounts[59]==2
                    && Collect.has(cloudReload.collected,59));
        } catch(Exception e) {check("duck cloud migration",false);}
        c.resetHighScores();
        GameCore reset=new GameCore(m,164,true);
        check("score reset keeps ducks and cannot resurrect the pending record",!reset.survival.reward.pending
                && reset.survival.histories[0].latestRun==null && reset.collectionCounts[who]==2);
        DuckBodies bodies=c.ducks;
        float[] before=bodies.skin[10][0].outline().clone();
        bodies.react(who,.8f);bodies.update(c,.05f);
        check("duck touch impulses deform actual spring skin",!java.util.Arrays.equals(before,bodies.skin[10][0].outline()));
        float[] paused=bodies.skin[10][0].outline().clone();c.paused=true;bodies.update(c,1);
        check("duck springs freeze while paused",java.util.Arrays.equals(paused,bodies.skin[10][0].outline()));c.paused=false;
        for(int i=0;i<600;i++)bodies.update(c,DT);
        boolean bounded=true;for(float value:bodies.skin[10][0].outline())bounded&=Float.isFinite(value)&&Math.abs(value)<2;
        check("duck spring skin settles without exploding",bounded);
        float[] frozen=bodies.skin[10][0].outline().clone();Renderer.draw((Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)->null),c,L);Renderer.draw((Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)->null),c,L);
        check("drawing does not advance duck physics",java.util.Arrays.equals(frozen,bodies.skin[10][0].outline()));
    }

    private static void linkedAfterMinute(Layout L) {
        for(int effect:new int[]{-1,Power.FLURRY,Power.NINJA,Power.TEAM}) {
            GameCore c=start(store(),168);
            if(effect>=0)c.startFrenzy(effect,L);
            c.enemies.clear();c.stageGap=0;c.spawnTimer=0;c.spawnedThisStage=6;
            c.survival.seconds=59.99;
            check("Survival holds linked pairs until one minute "+effect,!LinkedPairs.due(c));
            c.survival.seconds=60;
            check("Survival unlocks pairs without waiting for stage 16 "+effect,LinkedPairs.due(c) && c.stage<16);
            c.update(DT,L);
            check("Survival spawns linked friends after one minute "+effect,c.enemies.size()==2
                    && c.enemies.get(0).link==c.enemies.get(1) && c.enemies.get(1).link==c.enemies.get(0));
            check("Survival keeps solo enemies between linked pairs "+effect,!LinkedPairs.due(c));
        }
        GameCore paused=start(store(),170);paused.survival.seconds=59.5;Pause.open(paused);
        paused.update(2,2,L);
        check("paused time cannot unlock Survival linked pairs",!LinkedPairs.due(paused));
    }
    private static void history(Layout L) {
        Mem m=store();GameCore c=start(m,164);
        String adventure=c.highScores.encode();
        c.score=456;c.hits=9;c.misses=1;c.maxCombo=4;c.squishes=12;c.survival.seconds=65.432;
        c.tapPower(Survival.rackX(L),Survival.rackY(L,1),L);
        add(c,L,new int[]{0},L.dangerY-L.enemyR*.4f);
        c.warnLevel=1;
        c.pushBack(L);
        c.lives=1;c.takeHit(L.w*.5f,L);
        HighScores history=c.survival.history();HighScores.Run run=history.latestRun;
        check("Survival snapshot keeps its own score time companion and controls",run!=null && run.survival
                && run.duration==65432 && run.profile==0 && run.score==456 && run.character==c.runWho);
        check("Survival captures combat stats and rack usage",run.accuracy()==90 && run.combo==4
                && run.squishes==12 && run.effects[Power.NINJA]==1 && run.powers==1 && run.swipes==1);
        check("Survival history leaves Adventure records and counters alone",adventure.equals(c.highScores.encode())
                && c.highScores.effects[Power.NINJA]==0 && c.highScores.swipes==0);
        check("Survival has exactly twenty distinct rainbow endings",HighScores.SURVIVAL_BLURBS.length==20
                && new java.util.HashSet<String>(java.util.Arrays.asList(HighScores.SURVIVAL_BLURBS)).size()==20);
        String blurb=run.blurb(),saved=c.survival.encode();
        c.survival.finish(c);
        check("Survival saves a run and its random blurb only once",history.latest==1 && saved.equals(c.survival.encode()));
        GameCore loaded=new GameCore(m,165,true);
        check("Survival snapshots and selected blurb persist",loaded.survival.encode().equals(saved)
                && loaded.survival.histories[0].latestRun.blurb().equals(blurb));
        c.toTitle();c.returnFade=0;
        check("Survival score attention uses its own unread state",HighScoreScreen.titleAttention(c));
        c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        check("shared score panel opens Survival history and clears attention",c.highScoreScreen.open
                && HighScoreScreen.records(c)==history && !history.unread);
        c.highScoreScreen.action(c,HighScoreScreen.ROW);
        check("Survival row opens shared summary",c.highScoreScreen.selected==0);
        java.util.ArrayList<String> labels=new java.util.ArrayList<>();
        Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)-> {
                    if(method.getName().equals("text"))labels.add((String)args[0]);
                    return null;
                });
        c.highScoreScreen.draw(painter,c,L);
        check("Survival summary contains its time and controls",labels.contains("TIME SURVIVED")
                && labels.contains("CONTROLS") && labels.contains("RESCUE SWIPES"));
        check("Survival summary excludes Adventure-only stats",!labels.contains("STAGES COMPLETED")
                && !labels.contains("BOSSES BEATEN") && !labels.contains("DUMPLINGS COLLECTED")
                && !labels.contains(Power.NAMES[Power.INCOGNITO]) && !labels.contains(Power.NAMES[Power.MONOCHROME]));
        c.highScoreScreen.back(c);c.highScoreScreen.back(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        java.util.HashSet<String> endings=new java.util.HashSet<>();
        for(int i=0;i<12;i++) {
            c.startGame();c.score=i*100;c.survival.seconds=i*10;c.lives=0;c.survival.finish(c);
            endings.add(history.latestRun.blurb());c.toTitle();
        }
        check("Survival death phrases vary between runs",endings.size()>1);
        check("Survival reuses top-ten score ranking",history.runs.size()==10 && history.runs.get(0).score==1100);
        c.startGame();c.score=1;c.survival.finish(c);c.toTitle();
        check("latest below top ten remains reviewable",history.displayCount()==11 && history.displayRun(10).score==1);
        check("voluntary exit uses a snack-break blurb",history.latestRun.ending==20);
        c.fullRoster=true;c.startGame();c.score=77;c.survival.finish(c);c.toTitle();
        check("six-key leaderboard is independent",HighScoreScreen.records(c)==c.survival.histories[1]
                && HighScoreScreen.records(c).runs.size()==1 && history.runs.size()==10);
        c.preferences.kids=true;c.startGame();c.score=88;c.survival.finish(c);c.toTitle();
        check("Kids leaderboard is independent",HighScoreScreen.records(c)==c.survival.histories[2]
                && HighScoreScreen.records(c).latestRun.profile==2 && HighScoreScreen.records(c).latestRun.kids);
        c.modes.back(c);
        check("Adventure selects its unchanged list",HighScoreScreen.records(c)==c.highScores
                && adventure.equals(c.highScores.encode()));
        Survival legacy=new Survival();legacy.load("1;12345,500;20000,600;30000,800");
        check("legacy bests migrate without inventing run snapshots",legacy.bestTime[0]==12345
                && legacy.bestScore[2]==800 && legacy.histories[0].runs.isEmpty());
        HighScores rejected=new HighScores();rejected.load(history.encode());
        check("Adventure parser cannot load Survival history",rejected.runs.isEmpty());
        HighScores malformed=new HighScores(true);
        String single=saved.split("\\|")[1];
        String[] fields=single.split(";")[1].split(",");fields[24]="-1";
        malformed.load("7:1:0;"+String.join(",",fields));
        check("negative Survival duration is rejected",malformed.runs.isEmpty());
        fields[24]="65432";fields[23]="3";
        malformed.load("7:1:0;"+String.join(",",fields));
        check("invalid Survival profile is rejected",malformed.runs.isEmpty());
        GameCore first=start(store(),167),second=start(store(),167);first.lives=0;first.survival.finish(first);
        check("rainbow blurb selection preserves gameplay randomness",first.rnd.nextLong()==second.rnd.nextLong());
        c.resetHighScores();GameCore reset=new GameCore(m,166,true);
        check("score reset clears every mode and Survival profile",reset.highScores.runs.isEmpty()
                && reset.survival.histories[0].runs.isEmpty() && reset.survival.histories[1].runs.isEmpty()
                && reset.survival.histories[2].runs.isEmpty());
    }
    private static void powerRack(Layout L) {
        GameCore c=start(store(),158);float x=Survival.rackX(L),y=Survival.rackY(L,0);
        check("Survival starts with all offered powers unused",c.survival.usedPowers==0);
        Pause.open(c);
        check("pause blocks rack activation",!c.tapPower(x,y,L) && c.survival.usedPowers==0);
        Pause.resume(c);c.openSettings();
        check("settings block rack activation",!c.tapPower(x,y,L) && c.survival.usedPowers==0);
        c.closeSettings();c.onboarding.briefing=true;
        check("tutorial briefing blocks rack activation",!c.tapPower(x,y,L));c.onboarding.clear();
        check("rack leaves the rest of the field available",!c.tapPower(L.w*.8f,y,L));
        int score=c.score,hits=c.hits,combo=c.combo;
        for(int slot=0;slot<Power.OFFERED.length;slot++) {
            int effect=Power.offeredAt(slot);y=Survival.rackY(L,slot);
            check("rack activates offered power "+effect,c.tapPower(x,y,L) && c.mode==effect
                    && c.powerActive() && c.survival.powerUsed(effect) && c.power==null);
            check("activation feedback starts at the rack",c.powerBurstX==x && c.powerBurstY==y);
            int spent=c.survival.usedPowers;
            c.tapPower(x,Survival.rackY(L,(slot+1)%Power.OFFERED.length),L);
            check("busy rack preserves unused powers and active duration",c.survival.usedPowers==spent
                    && c.mode==effect && c.modeLeft==Power.DURATION);
            c.modeLeft=.001f;c.update(DT,L);
            check("spent rack button consumes its touch without reactivating",c.tapPower(x,y,L)
                    && !c.powerActive() && c.survival.powerUsed(effect));
        }
        check("rack activation awards no free score or accuracy",c.score==score && c.hits==hits && c.combo==combo);
        c.powerTimer=-1;boolean noPickups=true;
        for(int i=0;i<180;i++) {c.update(DT,L);noPickups &= c.power==null;}
        check("Survival never spawns random powerups or recharges spent powers",noPickups
                && c.powerTimer==-1 && c.survival.usedPowers==((1<<Power.FLURRY)|(1<<Power.NINJA)|(1<<Power.TEAM)));
        c.lives=1;c.takeHit(L.w*.5f,L);
        check("death clears owned rack state",c.survival.usedPowers==0);
        c.toTitle();c.startGame();
        check("new Survival run restores every power",c.survival.usedPowers==0 && c.tapPower(x,Survival.rackY(L,0),L));
    }
    private static void backgroundEntrance(Layout L) {
        GameCore c=start(store(),157);
        float[] bottom={-1f};
        Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)-> {
                    if(method.getName().equals("fillCircle"))bottom[0]=Math.max(bottom[0],(Float)args[1]+(Float)args[2]);
                    return null;
                });
        c.survival.scenery(painter,c,L);
        check("Survival starts on the title sky with all stripe caps above screen",
                c.survival.background()==Draw.BG && bottom[0]<0 && Lands.cloudTint(c,0)==Sky.CLOUD_TINT[0]);
        c.survival.update(c,.5f);c.survival.scenery(painter,c,L);
        float first=bottom[0];
        check("stripe tips enter at the top before covering the bottom",first>0 && first<L.h*.4f);
        Pause.open(c);float entrance=c.survival.entrance();c.update(.5f,.5f,L);
        check("pause holds the stripe entrance",c.survival.entrance()==entrance);
        Pause.resume(c);c.survival.update(c,.5f);bottom[0]=-1;c.survival.scenery(painter,c,L);
        check("stripe tips progress downward through the entrance",bottom[0]>first && bottom[0]<L.deckTop);
        c.survival.update(c,1f);
        check("entrance settles into normal scrolling",c.survival.entrance()==1);
        c.toTitle();c.startGame();
        check("retry starts a fresh stripe entrance",c.survival.entrance()==0 && c.survival.skyPhase==0);
    }
    private static void backgroundCoverage() {
        for(int[] size:new int[][]{{320,568},{640,1400}})for(int age:new int[]{3,150,300}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,0,0,0);
            GameCore c=start(store(),156);c.survival.seconds=age;
            java.util.ArrayList<float[]> bodies=new java.util.ArrayList<>();
            java.util.ArrayList<Integer> colors=new java.util.ArrayList<>();
            float[] clip=new float[4];
            Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                    new Class<?>[]{Painter.class},(proxy,method,args)-> {
                        if(method.getName().equals("fillRect")) {
                            bodies.add(new float[]{(Float)args[0],(Float)args[1],(Float)args[2],(Float)args[3]});
                            colors.add((Integer)args[4]);
                        }
                        if(method.getName().equals("clipRect"))for(int i=0;i<4;i++)clip[i]=(Float)args[i];
                        return null;
                    });
            boolean covered=true,saturated=true,stable=true,refreshed=true,screenLength=true;
            java.util.ArrayList<Float> firstOrder=null;
            for(float phase:new float[]{0,.001f,19.5f,123.4f,901.2f}) {
                bodies.clear();colors.clear();c.survival.skyPhase=phase;c.survival.scenery(painter,c,l);
                java.util.ArrayList<Float> order=new java.util.ArrayList<>();
                for(float[] b:bodies) {
                    screenLength &= Math.abs((b[3]-b[1])+(b[2]-b[0])-l.h)<.01f;
                    float x=(b[0]+b[2])*.5f;
                    if(order.isEmpty() || x!=order.get(order.size()-1))order.add(x);
                }
                if(firstOrder==null)firstOrder=order;
                else if(phase==.001f)stable &= firstOrder.equals(order);
                else refreshed &= !firstOrder.equals(order);
                for(int row=0;row<=20;row++)for(int column=0;column<=40;column++) {
                    float x=l.w*column/40f,y=l.deckTop*row/20f;
                    boolean found=false;
                    for(float[] b:bodies)if(x>=b[0] && x<=b[2] && y>=b[1] && y<=b[3]) {found=true;break;}
                    covered &= found && x>=clip[0] && y>=clip[1] && x<=clip[2] && y<=clip[3];
                }
                for(int color:colors) {
                    int r=color>>16&255,g=color>>8&255,b=color&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    saturated &= Math.abs((max-min)/(float)max-(.50f+.25f*age/300f))<.02f;
                }
            }
            boolean left=false,right=false;
            for(int i=1;i<firstOrder.size();i++) {
                left |= firstOrder.get(i)<firstOrder.get(i-1);
                right |= firstOrder.get(i)>firstOrder.get(i-1);
            }
            check("band draw order crosses both left and right instead of sweeping columns",left && right);
            check("live stripes keep their horizontal position and depth while scrolling",stable);
            check("incoming stripes refresh horizontal positions after scrolling cycles",refreshed);
            check("stripe length including both round caps is one screen height",screenLength);
            float previous=Float.NaN,minGap=Float.MAX_VALUE,maxGap=0;
            for(float x:new java.util.TreeSet<Float>(firstOrder)) {
                if(!Float.isNaN(previous) && x>previous) {
                    minGap=Math.min(minGap,x-previous);maxGap=Math.max(maxGap,x-previous);
                }
                previous=x;
            }
            check("band spacing has visible clusters and wide gaps",maxGap>minGap*2f);
            check("scrolling bands cover from the screen top through the playfield at "+size[0]+" age "+age,covered);
            check("band saturation ramps from 50 to 75 percent",saturated);
        }
    }
    private static void effects(Layout L) {
        GameCore calm=start(store(),155),vivid=start(store(),155);
        vivid.survival.seconds=Survival.RAMP_SECONDS;
        GameCore.Enemy a=add(calm,L,new int[]{0,1,2},L.h*.4f);
        GameCore.Enemy b=add(vivid,L,new int[]{0,1,2},L.h*.4f);
        calm.destroyWord(a,a.baseX,a.y,L);vivid.destroyWord(b,b.baseX,b.y,L);
        check("maximum Survival clears emit ten times the particles",vivid.particles.size()==calm.particles.size()*10);
        check("explosion intensity cannot alter gameplay RNG",vivid.rnd.nextLong()==calm.rnd.nextLong());
        for(int i=0;i<60;i++)vivid.survival.wordBurst(vivid,L.w*.5f,L.h*.5f,L.enemyR,20,Draw.GOLD);
        check("simultaneous clears have a bounded particle budget",vivid.particles.size()<=4200);
        Fx.updateParticles(vivid,1f);
        check("large clear effects expire normally",vivid.particles.isEmpty());
    }
    private static void records(Layout L) {
        Mem m=store();GameCore c=start(m,152);
        c.survival.seconds=12.345;c.score=500;Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("ending early saves time and returns cleanly",c.state==GameCore.TITLE && !c.survival.active && c.survival.bestTime[0]==12345);
        c.preferences.kids=true;c.startGame();c.survival.seconds=30;c.score=800;c.highScores.finish(c);c.toTitle();
        c.preferences.kids=false;c.fullRoster=true;c.startGame();c.survival.seconds=20;c.score=600;c.highScores.finish(c);c.toTitle();
        check("Kids and both normal decks have independent records",c.survival.bestTime[0]==12345
                && c.survival.bestTime[1]==20000 && c.survival.bestTime[2]==30000);
        check("time format is stable",Survival.time(65000).equals("1:05"));
        Survival saved=new Survival();saved.load(c.survival.encode());
        check("all record profiles round trip",saved.encode().equals(c.survival.encode()));
        for(String bad:new String[]{"","future","1;1,2;3,4;-1,8","1;1,2;3,4;5,no"}) {
            saved.load(bad);check("bad Survival record stays empty",saved.bestTime[0]==0 && saved.bestScore[2]==0);
        }
        c.resetHighScores();GameCore reset=new GameCore(m,153,true);
        check("score reset also clears Survival records",reset.survival.bestTime[0]==0 && reset.survival.bestScore[2]==0);
    }
    private static void bounded(Layout L) {
        for(float presses:new float[]{2.5f,5f,8f}) {
            float total=0;int deaths=0;
            for(int seed=0;seed<3;seed++) {
                GameCore c=start(store(),1490+seed);
                Bot bot=new Bot(presses,.18f,.02f,true,1490+seed);
                int frame=0;
                while(c.state==GameCore.PLAY && frame++<60*360) {
                    c.update(DT,L);bot.step(c,L,DT);
                    if(c.state==GameCore.BONUS || c.boss.active() || c.cave.running)break;
                }
                check("bounded Survival has no boss or minigame",c.state!=GameCore.BONUS && !c.boss.active() && !c.cave.running);
                check("bounded Survival keeps rewards isolated",(c.collected & Collect.LEGACY_MASK)==store().collected && c.progress.maximum("highest_stage")==0);
                total+=c.survival.seconds;if(c.state==GameCore.OVER)deaths++;
            }
            System.out.printf("    Survival %.1f presses/s: mean %.1fs, %d/3 deaths%n",presses,total/3,deaths);
            check("bounded player gets a playable opening",total/3>15);
        }
    }
    private static void fuzz(Layout L) {
        GameCore c=start(store(),154);java.util.Random random=new java.util.Random(154);
        int deaths=0;boolean sane=true;
        for(int i=0;i<60*180;i++) {
            c.update(DT,L);
            if(c.state==GameCore.OVER) {c.toTitle();c.startGame();deaths++;}
            if(random.nextInt(5)==0)c.tapKey(random.nextInt(Glyph.COUNT),L);
            if(random.nextInt(800)==0) {Pause.open(c);c.update(.1f,L);Pause.resume(c);}
            sane &= c.state==GameCore.PLAY || c.state==GameCore.OVER;
            sane &= !c.boss.active() && !c.pendingBonus && !Double.isNaN(c.survival.seconds);
        }
        check("Survival fuzz survives repeated deaths and pauses",sane && deaths>0);
    }
}
