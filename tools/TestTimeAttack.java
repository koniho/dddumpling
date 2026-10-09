package com.dddumpling.game;

final class TestTimeAttack extends Check {
    static Mem store() {
        Mem m=TestSurvival.store();m.collected=Collect.encode(Collect.MASK);return m;
    }
    static GameCore start(int choice) {
        GameCore c=new GameCore(store(),150,true);c.landSeen=LandPicker.stateMask();
        c.fullRoster=false;c.modes.restore(ModeSelector.TIME_ATTACK);c.timeAttack.selected=choice;c.startGame();return c;
    }
    static void beat(GameCore c,Layout L) {
        c.boss.beaten=true;c.boss.leaveT=0;
        BossPlay.endBoss(c,L);
    }
    private static void selectorSwipes(Layout L) {
        GameCore c=start(Boss.SLIME);c.toTitle();c.returnFade=0;
        Ear ear=new Ear();c.sound=ear;
        float x=L.w*.6f,y=L.h*.677f,distance=L.w*.25f;
        check("battle skit accepts a swipe start",c.modes.touch(c,L,0,42,x,y));
        c.modes.touch(c,L,2,42,x-distance,y+L.unit*1.2f);
        c.modes.touch(c,L,2,42,x-distance*1.5f,y);
        c.modes.touch(c,L,1,42,x-distance*1.5f,y);
        check("diagonal left swipe advances exactly one boss with feedback",c.timeAttack.selected==1
                && c.modes.selected==ModeSelector.TIME_ATTACK && ear.uiBloops==1 && c.modes.pointer==-1);
        c.modes.touch(c,L,0,42,x,y);c.modes.touch(c,L,1,42,x+distance,y);
        check("quick right swipe works without move events",c.timeAttack.selected==0 && ear.uiBloops==2);
        c.modes.touch(c,L,0,42,x,y);c.modes.touch(c,L,1,42,x+distance,y);
        check("right swipe wraps to all bosses",c.timeAttack.selected==TimeAttack.ALL);
        float label=TimeAttack.selectY(L);
        c.modes.touch(c,L,0,42,x,label);c.modes.touch(c,L,1,42,x-distance,label);
        check("boss name swipe wraps forward to slime",c.timeAttack.selected==0);
        for(float row:new float[]{y,label})for(int direction:new int[]{-1,1}) {
            c.modes.touch(c,L,0,42,L.w*.5f,row);
            c.modes.touch(c,L,2,42,L.w*.5f+direction*L.w*.19f,row);
            c.modes.touch(c,L,1,42,L.w*.5f+direction*L.w*.19f,row);
            check("short swipe does not switch or become a tap "+row+"/"+direction,c.timeAttack.selected==0);
        }
        c.modes.touch(c,L,0,42,x,label);c.modes.touch(c,L,2,42,x-L.unit*2,label);
        c.modes.touch(c,L,1,42,x,label);
        check("short drag returning to its start is not a tap",c.timeAttack.selected==0);
        c.modes.touch(c,L,0,42,x,y);c.modes.touch(c,L,1,42,x,y);
        check("tapping the skit starts the selected boss",c.timeAttack.selected==0 && c.starting());
        c.cancelStart();
        c.modes.touch(c,L,0,42,x,y);c.modes.touch(c,L,2,42,x,y+L.unit*3);
        c.modes.touch(c,L,1,42,x-L.unit*4,y+L.unit*3);
        check("vertical skit drag cannot become boss selection",c.timeAttack.selected==0);
        for(int action:new int[]{3,5}) {
            c.modes.touch(c,L,0,42,x,y);c.modes.touch(c,L,action,action==5?43:42,x,y);
            c.modes.touch(c,L,1,42,x-L.unit*4,y);
            check("cancel or extra finger cancels boss swipe "+action,c.timeAttack.selected==0);
        }
        c.modes.touch(c,L,0,42,x,y);c.caseOpen=true;
        c.modes.touch(c,L,1,42,x-L.unit*4,y);
        check("opening another panel cancels boss swipe",c.timeAttack.selected==0 && c.modes.pointer==-1);
    }
    private static int skitFrame(GameCore c,Layout L) {
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);
        TimeAttackDemo.draw(p,c,L);
        return java.util.Arrays.hashCode(p.resolve());
    }
    private static int companionFrame(GameCore c,Layout L) {
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);
        TimeAttackDemo.drawCompanion(p,c,L);
        return java.util.Arrays.hashCode(p.resolve());
    }
    private static void titleSkit() {
        Layout L=new Layout();L.compute(320,700,0,0,0,0);
        GameCore c=start(Boss.SLIME),control=start(Boss.SLIME);
        c.toTitle();control.toTitle();c.returnFade=0;c.clock=.5f;c.caseIndex=7;
        int first=skitFrame(c,L);
        check("title battle rendering is deterministic",first==skitFrame(c,L));
        c.clock=1.6f;check("title battle advances through dodge and counter",first!=skitFrame(c,L));
        c.clock=.5f;c.caseIndex=8;
        check("title battle uses current display case companion",first!=skitFrame(c,L));
        c.caseIndex=7;
        for(int boss=0;boss<Boss.COUNT;boss++) {
            c.timeAttack.selected=boss;
            if(boss>0)check("bosses have distinct skit frames "+boss,first!=skitFrame(c,L));
        }
        c.timeAttack.selected=TimeAttack.ALL;
        int group=skitFrame(c,L);
        check("all bosses uses a distinct group battle",group!=first);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            c.collected &= ~(1L << (Collect.BOSS_FIRST+boss));
            check("group battle includes boss "+boss,group!=skitFrame(c,L));
            c.collected=control.collected;
        }
        check("title skits leave combat and gameplay randomness alone",c.state==GameCore.TITLE
                && c.boss.active()==control.boss.active() && !c.timeAttack.active && c.shots.isEmpty() && c.enemies.isEmpty()
                && c.collected==control.collected && c.rnd.nextLong()==control.rnd.nextLong());
        c.timeAttack.selected=Boss.SLIME;c.timeAttack.slide=1;c.clock=.5f;
        int actor=companionFrame(c,L);
        c.timeAttack.choose(c,-1);
        check("boss selection retains departing boss and scroll direction",c.timeAttack.selected==TimeAttack.ALL
                && c.timeAttack.previousSelected==Boss.SLIME && c.timeAttack.slideDirection==-1 && c.timeAttack.slide==0);
        for(float slide:new float[]{0,.25f,.5f,.75f,1}) {
            c.timeAttack.slide=slide;
            check("companion stays anchored through group transition "+slide,companionFrame(c,L)==actor);
        }
        c.timeAttack.choose(c,1);c.modes.update(c,TimeAttack.SLIDE_TIME*.5f);
        check("boss scroll advances over time",c.timeAttack.slide>.4f && c.timeAttack.slide<.6f);
        c.modes.update(c,TimeAttack.SLIDE_TIME);
        check("boss scroll settles",c.timeAttack.slide==1);
        check("solo pair anchors are equally spaced around center",
                Math.abs(TimeAttackDemo.companionAnchor(L)+TimeAttackDemo.bossAnchor(L)-L.w)<.001f);
        for(int turn=0;turn<Boss.COUNT;turn++) {
            float clock=(turn+.5f)*TimeAttackDemo.GROUP_TURN;
            float front=TimeAttackDemo.groupX(L,clock,turn);
            check("each boss takes the front spot "+turn,Math.abs(front-TimeAttackDemo.bossAnchor(L))<.001f);
            for(int boss=0;boss<Boss.COUNT;boss++) {
                float x=TimeAttackDemo.groupX(L,clock,boss);
                check("waiting bosses form a compact queue "+turn+"/"+boss,x>=front && x<=front+L.w*.241f);
                float boundary=(turn+1)*TimeAttackDemo.GROUP_TURN;
                check("queue shuffle does not teleport at turn boundary "+turn+"/"+boss,
                        Math.abs(TimeAttackDemo.groupX(L,boundary-.0001f,boss)
                                -TimeAttackDemo.groupX(L,boundary+.0001f,boss))<L.w*.001f);
            }
        }
    }
    static void all(Layout L) {
        encounterStatistics(L);
        statistics(L);
        titleSkit();
        selectorSwipes(L);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore played=start(boss);
            Bot.Result run=new Bot(6f,.20f,.04f,false,150+boss).play(played,L,180);
            check("bounded player completes or loses real encounter "+boss,
                    played.state==GameCore.OVER && played.timeAttack.finished && played.timeAttack.seconds>0);
            check("only successful bounded clears set records "+boss,
                    (played.timeAttack.best[0][boss]>0)==played.timeAttack.won);
            System.out.println("    Time Attack "+Boss.NAMES[boss]+": "+TimeAttack.time(played.timeAttack.millis())
                    +(played.timeAttack.won?" clear":" loss")+", "+run.presses+" presses");
        }
        GameCore c=start(Boss.SLIME);Mem m=(Mem)c.store;
        check("Time Attack starts real selected boss",c.state==GameCore.PLAY && c.timeAttack.active
                && c.boss.kind==Boss.SLIME && c.stage==5 && c.lives==GameCore.START_LIVES);
        check("Time Attack skips wave, banner and minigame",c.enemies.isEmpty() && !c.pendingBonus
                && c.stageBanner==0 && !c.stageCleared() && !Cave.active(c));
        c.update(.01f,.25f,L);
        check("arrival card is not timed",c.timeAttack.seconds==0);
        c.boss.intro=0;c.update(.01f,.25f,L);
        check("playable combat uses real elapsed time",Math.abs(c.timeAttack.seconds-.25)<.001);
        Pause.open(c);c.update(1,1,L);Pause.resume(c);
        check("paused time excluded",Math.abs(c.timeAttack.seconds-.25)<.001);
        PlayerSettings.open(c);c.update(1,1,L);c.closeSettings();
        check("settings time excluded",Math.abs(c.timeAttack.seconds-.25)<.001);
        c.boss.beaten=true;c.boss.leaveT=Boss.LEAVE;c.update(.01f,.25f,L);
        check("defeat animation not timed",Math.abs(c.timeAttack.seconds-.25)<.001);
        c.timeAttack.seconds=12.345;long owned=c.collected;String adventure=m.highScores;
        beat(c,L);
        check("successful boss ends at result with local best",c.state==GameCore.OVER && c.timeAttack.won
                && c.timeAttack.newBest && c.timeAttack.best[c.timeAttack.profile][Boss.SLIME]==12345);
        check("replay does not award Adventure boss or change records",c.collected==owned
                && c.roundPrizes==0 && m.highScores.equals(adventure) && m.best==789
                && c.progress.maximum("highest_stage")==0);
        String saved=m.timeAttack;c.timeAttack.finish(c,true);
        check("finishing twice is inert",saved.equals(m.timeAttack));
        c.toTitle();check("title clears session only",!c.timeAttack.active && c.timeAttack.seconds==0
                && c.modes.selected==ModeSelector.TIME_ATTACK && c.timeAttack.best[0][0]==12345);
        c.startGame();c.timeAttack.seconds=15;beat(c,L);
        check("slower retry keeps personal best",!c.timeAttack.newBest && c.timeAttack.best[0][0]==12345);
        c.toTitle();c.startGame();c.timeAttack.seconds=10;beat(c,L);
        check("faster retry updates personal best",c.timeAttack.newBest && c.timeAttack.best[0][0]==10000);
        GameCore reloaded=new GameCore(m,151);
        check("records survive restart",reloaded.timeAttack.best[0][0]==10000);
        c.toTitle();c.startGame();c.boss.intro=0;c.timeAttack.seconds=2;c.lives=1;c.takeHit(L.w*.5f,L);
        check("failed attempt never replaces clear record",c.state==GameCore.OVER && c.timeAttack.finished
                && !c.timeAttack.won && c.timeAttack.best[0][0]==10000);
        c.toTitle();c.startGame();Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("pause exit clears boss and timer without recording",c.state==GameCore.TITLE
                && !c.timeAttack.active && !c.boss.active() && c.timeAttack.best[0][0]==10000);
        c.modes.restore(ModeSelector.ADVENTURE);c.startGame();
        check("mode switch starts ordinary Adventure",!c.timeAttack.active && c.stage==1 && !c.boss.active());
        c=start(TimeAttack.ALL);c.lives=1;
        for(int boss=0;boss<Boss.COUNT;boss++) {
            check("all-boss order is fixed "+boss,c.boss.kind==boss && c.boss.intro>0);
            c.update(.01f,10,L);
            check("between-boss introduction excluded "+boss,c.timeAttack.seconds==boss*5);
            c.boss.intro=0;c.timeAttack.tick(c,5);c.pushUsed=true;beat(c,L);
            if(boss+1<Boss.COUNT)check("boss clear carries lives and reloads rescue "+boss,
                    c.lives==Math.min(GameCore.START_LIVES,boss+2) && !c.pushUsed && c.state==GameCore.PLAY);
        }
        check("all-boss saves only full challenge record",c.timeAttack.won && c.timeAttack.cleared==4
                && c.timeAttack.best[0][TimeAttack.ALL]==20000 && c.timeAttack.best[0][0]==0);
        for(int profile=0;profile<3;profile++) {
            c=new GameCore(store(),152);c.preferences.kids=profile==2;c.fullRoster=profile==1;
            c.modes.restore(ModeSelector.TIME_ATTACK);c.timeAttack.selected=Boss.OCTOPUS;c.startGame();
            c.timeAttack.seconds=8+profile;beat(c,L);
            for(int other=0;other<3;other++)check("profile records separated "+profile+"/"+other,
                    c.timeAttack.best[other][Boss.OCTOPUS]==(other==profile?(8+profile)*1000:0));
        }
        c=new GameCore(TestSurvival.store(),154);c.modes.restore(ModeSelector.TIME_ATTACK);
        c.timeAttack.selected=Boss.MUSHROOM;c.startGame();
        check("locked selected boss cannot start",c.state==GameCore.TITLE && !c.timeAttack.active);
        c.timeAttack.selected=TimeAttack.ALL;c.startGame();
        check("all-boss requires every boss",c.state==GameCore.TITLE && !c.timeAttack.active);
        c.timeAttack.selected=0;float y=TimeAttack.selectY(L);
        c.modes.touch(c,L,0,42,L.w*.9f,y);c.modes.touch(c,L,1,42,L.w*.9f,y);
        check("boss selector shares native touch routing",c.timeAttack.selected==1 && c.modes.selected==ModeSelector.TIME_ATTACK);
        c.modes.touch(c,L,0,42,L.w*.9f,y);Pause.release(c);c.modes.touch(c,L,1,42,L.w*.9f,y);
        check("cancelled boss touch does not move selection",c.timeAttack.selected==1);
        c.timeAttack.best[0][0]=999;c.timeAttack.load("1;garbage");
        check("malformed records leave valid records intact",c.timeAttack.best[0][0]==999);
        c.timeAttack.load(new TimeAttack().encode().replaceFirst(";0",";-1"));
        check("negative times rejected atomically",c.timeAttack.best[0][0]==999);
        check("record reset includes Time Attack",c.resetHighScores() && c.timeAttack.best[0][0]==0
                && ((Mem)c.store).timeAttack.isEmpty());
    }
    private static void defend(GameCore c,Layout L,int hp) {
        c.boss.intro=0;int g=Roster.at(c.runFullRoster,0);
        c.boss.blive[0]=true;c.boss.bglyph[0]=g;c.boss.bt[0]=.5f;
        c.boss.bhp[0]=c.boss.bhpMax[0]=hp;
        c.tapKey(g,L);
    }
    private static void encounterStatistics(Layout L) {
        GameCore c=start(TimeAttack.ALL);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            c.boss.intro=0;
            c.timeAttack.tick(c,.25f);
            if(boss==1)c.takeHit(L.w*.5f,L);
            c.timeAttack.tick(c,boss+1);
            c.hits+=boss+2;c.misses+=boss;
            for(int i=0;i<=boss;i++)c.timeAttack.defended();
            beat(c,L);
        }
        TimeAttackHistory.Run run=c.timeAttack.result;
        check("all boss run stores each encounter",run.encounters.length==Boss.COUNT);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            TimeAttackHistory.Encounter e=run.encounters[boss];
            check("encounter counters are local "+boss,e.boss==boss && e.cleared && e.duration==1250+boss*1000
                    && e.hits==boss+2 && e.misses==boss && e.defended==boss+1 && e.damage==(boss==1?1:0));
            check("first damage restarts its clock per boss "+boss,e.firstDamage==(boss==1?250:-1));
        }
        TimeAttack loaded=new TimeAttack();loaded.load(c.timeAttack.encode());
        check("encounter breakdown survives reopening",loaded.histories[0][TimeAttack.ALL].latest.encounters.length==Boss.COUNT
                && loaded.histories[0][TimeAttack.ALL].latest.encounters[1].firstDamage==250);
        String saved=loaded.encode();
        loaded.load(saved.replace("~0,1250,","~0,1251,"));
        check("inconsistent encounter totals rejected atomically",loaded.encode().equals(saved));
        String old=c.timeAttack.encode().replaceAll("~[^;|~]+", "").replaceFirst("^3;","2;");
        TimeAttack migrated=new TimeAttack();migrated.load(old);
        check("version two records retain totals without inventing encounters",migrated.histories[0][TimeAttack.ALL].latest!=null
                && migrated.histories[0][TimeAttack.ALL].latest.encounters.length==0
                && migrated.histories[0][TimeAttack.ALL].latest.hits==run.hits);
        c.toTitle();c.returnFade=0;c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        c.highScoreScreen.action(c,HighScoreScreen.ROW);
        HighScoreScreen ui=c.highScoreScreen;SettingsInput input=new SettingsInput();
        float x=L.w*.5f,y=(HighScoreScreen.listTop(L)+HighScoreScreen.listBottom(L))*.5f;
        check("per-boss summary has scrollable content",ui.maxScroll(c,L)>0);
        input.touch(c,L,0,5,x,y);input.touch(c,L,2,9,x,y-100);
        check("another finger cannot scroll summary",ui.scroll==0);
        input.touch(c,L,2,5,x,y-100);input.touch(c,L,1,5,x,y-100);
        check("vertical drag reviews details without leaving summary",ui.scroll==100 && ui.selected==0 && ui.open);
        input.touch(c,L,0,5,x,y);input.touch(c,L,1,5,x,y-L.h*10);
        check("scroll clamps at final boss",ui.scroll==ui.maxScroll(c,L));
        ui.scrollTo(c,L,0);input.touch(c,L,0,5,x,y);input.touch(c,L,3,5,x,y);
        input.touch(c,L,2,5,x,y-100);input.touch(c,L,1,5,x,y-100);
        check("cancelled scrolling is inert",ui.scroll==0);
        ui.scrollTo(c,L,100);ui.back(c);ui.action(c,HighScoreScreen.ROW);
        check("reopening summary resets scroll",ui.scroll==0 && ui.selected==0);
        c=start(TimeAttack.ALL);c.boss.intro=0;c.timeAttack.tick(c,.1235f);beat(c,L);
        c.boss.intro=0;c.timeAttack.tick(c,.4565f);
        c.takeHit(L.w*.5f,L);c.takeHit(L.w*.5f,L);c.takeHit(L.w*.5f,L);
        run=c.timeAttack.result;
        check("failed rush keeps completed boss and fatal encounter",run.encounters.length==2
                && run.encounters[0].cleared && !run.encounters[1].cleared && run.encounters[1].damage==3
                && run.encounters[1].firstDamage==run.encounters[1].duration);
        loaded=new TimeAttack();loaded.load(c.timeAttack.encode());
        check("rounded encounter times and fatal damage persist",loaded.histories[0][TimeAttack.ALL].latest!=null
                && loaded.histories[0][TimeAttack.ALL].latest.encounters.length==2);
        c=start(TimeAttack.ALL);c.timeAttack.finish(c,false);loaded=new TimeAttack();loaded.load(c.timeAttack.encode());
        check("immediate exit still saves a valid partial encounter",loaded.histories[0][TimeAttack.ALL].latest!=null
                && loaded.histories[0][TimeAttack.ALL].latest.encounters[0].duration==1);
    }
    private static void statistics(Layout L) {
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore c=start(boss);
            defend(c,L,2);
            check("partial projectile hits are not defended projectiles "+boss,c.timeAttack.defended==0);
            c.tapKey(Roster.at(c.runFullRoster,0),L);
            check("destroying a projectile counts once "+boss,c.timeAttack.defended==1 && !c.boss.blive[0]);
            c.timeAttack.tick(c,1.25f);c.takeHit(L.w*.5f,L);
            c.timeAttack.tick(c,2f);c.takeHit(L.w*.5f,L);
            c.hits=3;c.misses=1;beat(c,L);
            TimeAttackHistory.Run r=c.timeAttack.result;
            check("each boss snapshots complete run stats "+boss,r.won && r.duration==3250 && r.damage==2
                    && r.firstDamage==1250 && r.defended==1 && r.accuracy().equals("75%"));
            GameCore loaded=new GameCore((Mem)c.store,1500+boss);
            TimeAttackHistory h=loaded.timeAttack.histories[0][boss];
            check("boss stats persist in the matching challenge "+boss,h.runs.size()==1 && h.latest.damage==2
                    && h.latest.firstDamage==1250 && h.latest.defended==1 && h.latest.accuracy().equals("75%"));
        }
        GameCore c=start(TimeAttack.ALL);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            defend(c,L,1);c.timeAttack.tick(c,2f);
            if(boss==1)c.takeHit(L.w*.5f,L);
            beat(c,L);
        }
        check("all-boss summary aggregates without resetting at encounters",c.timeAttack.result.cleared==4
                && c.timeAttack.result.hits==4 && c.timeAttack.result.defended==4 && c.timeAttack.result.damage==1
                && c.timeAttack.result.firstDamage==4000 && c.timeAttack.result.duration==8000);
        check("all-boss history stays separate from individual bosses",c.timeAttack.histories[0][TimeAttack.ALL].runs.size()==1
                && c.timeAttack.histories[0][0].displayCount()==0 && c.timeAttack.histories[1][TimeAttack.ALL].displayCount()==0);
        c.toTitle();c.returnFade=0;
        check("Time Attack best time opens shared score panel",HighScoreScreen.entryHit(c,L,L.w*.5f,HighScoreScreen.titleY(L)));
        c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        c.highScoreScreen.action(c,HighScoreScreen.ROW);
        check("Time Attack history row opens summary",c.highScoreScreen.open && c.highScoreScreen.selected==0
                && !c.timeAttack.titleHistory(c).unread);
        c.highScoreScreen.back(c);
        check("summary back returns to time list",c.highScoreScreen.open && c.highScoreScreen.selected==-1);
        c=start(Boss.SLIME);c.boss.intro=0;c.timeAttack.tick(c,.5f);Pause.open(c);c.update(10,10,L);Pause.resume(c);
        c.takeHit(L.w*.5f,L);c.takeHit(L.w*.5f,L);c.takeHit(L.w*.5f,L);
        check("fatal damage is included and pause is excluded",c.timeAttack.result.damage==3 && c.timeAttack.result.firstDamage==500);
        check("failed attempt is summarized but not ranked",c.timeAttack.histories[0][0].runs.isEmpty()
                && c.timeAttack.histories[0][0].displayCount()==1 && !c.timeAttack.histories[0][0].latest.won);
        c=start(Boss.SLIME);c.timeAttack.seconds=2;beat(c,L);
        check("no-hit summary distinguishes no damage",c.timeAttack.result.firstDamage==-1
                && c.timeAttack.result.firstDamageText().equals("NO DAMAGE"));
        TimeAttackHistory history=new TimeAttackHistory();
        for(int i=1;i<=12;i++)history.add(new TimeAttackHistory.Run(i,1000+i,0,1,true,i,0,i,0,-1));
        history.add(new TimeAttackHistory.Run(13,500,0,0,false,2,1,0,1,400));
        check("history retains fastest ten plus latest failed run",history.runs.size()==10 && history.runs.get(0).duration==1001
                && history.runs.get(9).duration==1010 && history.displayCount()==11 && history.displayRun(10).id==13);
        TimeAttackHistory restored=new TimeAttackHistory();
        check("ranked history and latest attempt round trip",restored.load(history.encode(),0)
                && restored.runs.size()==10 && restored.latest.id==13 && restored.latest.firstDamage==400);
        StringBuilder old=new StringBuilder("1");
        for(int i=0;i<3*TimeAttack.CHOICES;i++)old.append(';').append(i==0?1234:0);
        TimeAttack attack=new TimeAttack();attack.load(old.toString());
        check("old personal best migrates without invented stats",attack.best[0][0]==1234
                && attack.histories[0][0].runs.get(0).legacy() && attack.histories[0][0].runs.get(0).accuracy().equals("--"));
        String before=attack.encode();attack.load(before.replace(",1234,",",-1,"));
        check("bad detailed history is rejected atomically",attack.encode().equals(before));
        TimeAttack roundTrip=new TimeAttack();roundTrip.load(before);
        check("migrated best saves in detailed format",roundTrip.encode().equals(before));
        check("score reset also clears Time Attack summaries",c.resetHighScores() && c.timeAttack.histories[0][0].displayCount()==0);
    }
}
