package com.dddumpling.game;

final class TestHighScores extends Check {
    static void all(Layout L) {
        portraits();
        group("high-score history");
        Mem mem=new Mem();mem.best=9000;
        GameCore c=new GameCore(mem,101L);
        check("legacy best retained without invented runs",c.best==9000 && c.highScores.runs.isEmpty());
        for(int i=0;i<12;i++) {
            c.startGame();c.score=(i+1)*100;c.stage=4;c.hits=2;c.misses=1;c.maxCombo=7;c.squishes=9;
            c.highScores.stages=3;c.highScores.prize(0);c.highScores.prize(0);c.highScores.finish(c);
        }
        check("only ten highest runs retained",c.highScores.runs.size()==10 && c.highScores.runs.get(9).score==300);
        check("score descending",c.highScores.runs.get(0).score==1200);
        HighScores.Run original=c.highScores.runs.get(0);
        c.startGame();c.score=1200;c.highScores.finish(c);
        check("newer tied run sorts first",c.highScores.runs.get(0).id==13 && c.highScores.runs.get(1)==original);
        c.startGame();c.score=1;c.highScores.finish(c);c.highScores.finish(c);
        check("nonqualifying latest still recorded exactly once",c.highScores.latest==14 && c.highScores.runs.get(0).id!=14);
        GameCore loaded=new GameCore(mem,102L);
        check("history round trips",loaded.highScores.encode().equals(c.highScores.encode()));
        check("latest outside top ten is appended and saved",loaded.highScores.displayCount()==11
                && loaded.highScores.displayRun(10).id==14 && loaded.highScores.displayRun(10).score==1);
        c.startGame();c.score=2;c.highScores.finish(c);
        check("another low run replaces the extra row",c.highScores.displayCount()==11 && c.highScores.displayRun(10).score==2);
        c.startGame();c.score=2000;c.highScores.finish(c);
        check("qualifying latest appears only once",c.highScores.displayCount()==10 && !c.highScores.latestOutsideTopTen());
        HighScores legacy=new HighScores();legacy.load(legacy(c.highScores.encode(),1));
        check("old top-ten saves retain qualifying latest",legacy.runs.size()==10 && legacy.latestRun.id==c.highScores.latest);
        check("historical summary remains frozen",original.score==1200 && original.stage==4 && original.stages==3
                && original.dumplings==2 && original.squishes==9 && original.combo==7 && original.accuracy()==67 && original.best==9000);
        for(String bad:new String[]{"junk","6:1","1:-1","1:1;1,0","1:1;999999999999999999999999999999999"}) {
            HighScores history=new HighScores();history.load(bad);
            check("bad history safely ignored "+bad,history.runs.isEmpty() && history.latest==0);
        }
        c.startGame();c.score=2500;c.lives=1;c.takeHit(L.w*.5f,L);
        check("fatal hit saves the completed run",c.highScores.runs.get(0).score==2500);
        c.startGame();
        check("new run resets counters",c.highScores.stages==0 && c.highScores.dumplings==0
                && c.highScores.bosses==0 && c.highScores.powers==0 && c.highScores.swipes==0
                && c.highScores.effects[Power.FLURRY]==0 && c.highScores.effects[Power.INCOGNITO]==0
                && c.highScores.prizes.isEmpty());
        c.startFrenzy(-1,L);c.startFrenzy(Power.TEAM,L);
        check("rejected powers do not count",c.highScores.powers==0);
        c.startFrenzy(Power.MULTI,L);
        check("activated power counts",c.highScores.powers==1 && c.highScores.effects[Power.MULTI]==1);
        c.startDebuff(Power.INCOGNITO);
        check("activated debuff counts",c.highScores.effects[Power.INCOGNITO]==1);
        c.startGame();c.stageGap=0;c.enemies.clear();
        check("unsuccessful swipe does not count",!c.pushBack(L) && c.highScores.swipes==0);
        add(c,L,new int[]{0,1},L.dangerY-L.enemyR*2f);c.warnLevel=1f;
        check("successful rescue counts once",c.pushBack(L) && c.highScores.swipes==1);
        check("spent rescue does not count again",!c.pushBack(L) && c.highScores.swipes==1);
        Interlude.beginStageEnd(c);
        check("stage completion uses event rather than stage number",c.highScores.stages==1);
        c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.beaten=true;BossPlay.endBoss(c,L);
        check("boss defeat tracked",c.highScores.bosses==1);
        int rewards=c.highScores.dumplings;
        int prize=c.highScores.prizes.get(c.highScores.prizes.size()-1);
        Interlude.awardBossPrize(c,Boss.SLIME);
        check("duplicate rewards preserve exact run haul",c.highScores.dumplings==rewards+1
                && c.highScores.prizes.size()==2 && c.highScores.prizes.get(0)==prize
                && c.highScores.prizes.get(1)==prize);
        c.state=GameCore.TITLE;c.pendingBonus=false;c.startFade=0;c.launchT=0;c.caseOpen=false;c.caseFade=0;
        entrance(L);
        titleAttention(L);
        onePage(L);
        navigation(c,L);
        blurbs(L);
    }
    private static String legacy(String data,int version) {
        String[] rows=data.split(";");
        StringBuilder result=new StringBuilder(version+rows[0].substring(1));
        int fields=version>=4?23:version>=3?17:16;
        for(int i=1;i<rows.length;i++) {
            String[] values=rows[i].split(",");result.append(';');
            for(int j=0;j<fields;j++) result.append(j==0?"":",").append(values[j]);
        }
        return result.toString();
    }
    private static void portraits() {
        Mem mem=new Mem();mem.collected=3L;
        GameCore c=new GameCore(mem,900L);c.openCase();CaseUi.select(c,1);c.closeCase();
        c.startGame();c.caseIndex=0;c.highScores.finish(c);
        check("run portrait snapshots start rather than finish",c.highScores.latestRun.character==1);
        GameCore loaded=new GameCore(mem,901L);
        check("selection and saved portrait survive restart",loaded.caseIndex==1 && loaded.highScores.latestRun.character==1);
        loaded.openCase();CaseUi.select(loaded,2);loaded.closeCase(); // Unknown entry explicitly chooses no character.
        GameCore empty=new GameCore(mem,902L);empty.startGame();empty.highScores.finish(empty);
        check("empty selection survives restart and snapshots resolved squishy",empty.caseIndex==2 && empty.highScores.latestRun.character==empty.runWho);
        check("new choice cannot rewrite old portrait",empty.highScores.runs.get(1).character==1);
        for(int version=1;version<=2;version++) {
            HighScores old=new HighScores();old.load(legacy(empty.highScores.encode(),version));
            check("legacy format has unknown portraits "+version,old.runs.size()==2
                    && old.runs.get(0).character==-1 && old.runs.get(1).character==-1);
        }
        HighScores portraits=new HighScores();portraits.load(legacy(empty.highScores.encode(),3));
        check("portrait-only history gains empty effect counts",portraits.runs.size()==2
                && portraits.runs.get(0).character==empty.highScores.runs.get(0).character
                && portraits.runs.get(0).effects[Power.FLURRY]==0);
        HighScores effects=new HighScores();effects.load(legacy(empty.highScores.encode(),4));
        check("effect-only history gains empty prize lists",effects.runs.size()==2
                && effects.runs.get(0).prizes.length==0);
        empty.startGame();Interlude.awardBossPrize(empty,Boss.SLIME);int repeated=empty.prize;
        Interlude.awardBossPrize(empty,Boss.SLIME);
        check("reward-selected character survives restart",new GameCore(mem,904L).caseIndex==empty.prize);
        int runWho=empty.runWho;empty.highScores.finish(empty);
        HighScores.Run rewarded=empty.highScores.latestRun;
        check("repeated rewards persist in encounter order",rewarded.prizes.length==2
                && rewarded.prizes[0]==repeated && rewarded.prizes[1]==repeated);
        empty.startGame();Interlude.awardBossPrize(empty,Boss.SPLITTER);
        check("later rewards cannot rewrite saved haul",rewarded.prizes.length==2
                && rewarded.prizes[0]==repeated);
        check("reward cannot change the active run portrait",rewarded.character==runWho);
        mem.caseIndex=Integer.MAX_VALUE;
        check("invalid saved selection is bounded",new GameCore(mem,903L).caseIndex==0);
    }
    private static void entrance(Layout L) {
        GameCore c=new GameCore(new Mem(),106L);HighScoreScreen ui=c.highScoreScreen;
        ui.show(c);
        check("score panel starts above screen",ui.offsetY(L)==-HighScoreScreen.bottom(L));
        float y=HighScoreScreen.listTop(L)+L.unit;
        check("entrance ignores touches",ui.hit(c,L,L.w*.5f,y)==0);
        c.update(HighScoreScreen.ENTRY_TIME*.5f,L);
        check("panel slides down without starting play",ui.offsetY(L)<0 && ui.offsetY(L)>-HighScoreScreen.bottom(L)
                && c.state==GameCore.TITLE && !c.starting());
        c.update(HighScoreScreen.ENTRY_TIME*.5f,L);
        check("panel settles at normal layout",!ui.moving() && ui.offsetY(L)==0f);
        ui.back(c);c.update(HighScoreScreen.ENTRY_TIME*.5f,L);
        check("exit slides upward while remaining modal",ui.open && ui.closing && ui.offsetY(L)<0f
                && ui.hit(c,L,L.w*.5f,y)==0);
        c.screenKey(0);check("exit cannot start the next run",!c.starting());
        c.update(HighScoreScreen.ENTRY_TIME*.5f,L);
        check("exit finishes above screen",!ui.open && ui.offsetY(L)==-HighScoreScreen.bottom(L));
        ui.show(c);c.update(HighScoreScreen.ENTRY_TIME*.2f,L);ui.back(c);c.update(HighScoreScreen.ENTRY_TIME,L);
        check("back during entrance closes panel",!ui.open);
    }
    private static void titleAttention(Layout L) {
        GameCore c=new GameCore(new Mem(),105L);
        check("fresh title has no score attention",!c.highScores.unread);
        c.startGame();c.score=500;c.lives=1;c.takeHit(L.w*.5f,L);
        c.toTitle();
        c.time=0f;float low=HighScoreScreen.titleTextScale(c);int lowColor=HighScoreScreen.titleTextColor(c);
        c.time=(float)Math.PI/3.5f;
        check("title text pulses in scale and color",HighScoreScreen.titleTextScale(c)>low && HighScoreScreen.titleTextColor(c)!=lowColor);
        check("completed run calls attention on returning to title",c.highScores.unread && c.state==GameCore.TITLE);
        c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        check("viewing scores acknowledges attention",c.highScoreScreen.open && !c.highScores.unread);
        c.highScoreScreen.back(c);c.startGame();c.score=1;c.highScores.finish(c);c.toTitle();
        check("lower scoring run also calls attention",c.highScores.unread);
        c.startGame();check("starting next run clears attention",!c.highScores.unread);
    }
    private static void onePage(Layout L) {
        GameCore c=new GameCore(new Mem(),104L);
        for(int i=0;i<11;i++) { c.startGame();c.score=1100-i*100;c.highScores.finish(c); }
        c.toTitle();c.returnFade=0;c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        for(int[] dims:new int[][]{{320,568},{393,852},{1080,2400},{768,1024}}) {
            Layout l=new Layout();l.compute(dims[0],dims[1],0,24,0,24);
            float rh=HighScoreScreen.rowHeight(c,l),top=HighScoreScreen.listTop(l);
            check("eleven rows fit on one page "+dims[0],top+11*rh<=HighScoreScreen.listBottom(l)+.01f);
            check("last row is clickable "+dims[0],c.highScoreScreen.hit(c,l,l.w*.5f,top+10.5f*rh)==HighScoreScreen.ROW+10);
        }
        c.highScoreScreen.action(c,HighScoreScreen.ROW+10);
        check("extra row opens its saved summary",c.highScoreScreen.selected==10 && c.highScores.displayRun(10).score==100);
        HighScores old=new HighScores();String data=c.highScores.encode();
        old.load(data.substring(0,data.lastIndexOf(';')).replaceFirst("2:","1:"));
        check("legacy nonqualifying run is not fabricated",old.latestRun==null && old.displayCount()==10);
    }
    private static void blurbs(Layout L) {
        group("saved death blurbs");
        GameCore c=new GameCore(new Mem(),103L);
        for(int kind=0;kind<Boss.COUNT;kind++) {
            c.startGame();c.stage=(kind+1)*Boss.EVERY;c.lives=1;c.takeHit(L.w*.5f,L);
            HighScores.Run run=c.highScores.runs.get(0);
            check("boss death names its form "+kind,run.ending==kind+1 && run.blurb().equals(HighScores.BLURBS[kind+1]));
        }
        for(int land=0;land<Lands.playableCount();land++) {
            c.startGame();c.stage=land*Boss.EVERY+1;c.lives=1;c.takeHit(L.w*.5f,L);
            check("land death keeps cute scenery detail "+land,c.highScores.runs.get(0).ending==land+5);
        }
        c.startGame();c.stage=25;c.lives=1;c.takeHit(L.w*.5f,L);
        check("later fifth stages without bosses use land blurb",c.highScores.runs.get(0).ending>=5);
        HighScores saved=new HighScores();saved.load(c.highScores.encode());
        check("death context survives reload",saved.runs.get(0).blurb().equals(c.highScores.runs.get(0).blurb()));
        c.startGame();Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("voluntary ending has no invented death",c.highScores.runs.get(0).ending==0);
    }
    private static void navigation(GameCore c,Layout L) {
        group("high-score navigation");
        Ear ear=new Ear();c.sound=ear;
        HighScoreScreen ui=c.highScoreScreen;ui.show(c);ui.update(HighScoreScreen.ENTRY_TIME);
        check("opens with existing sound",ui.open && ear.uiBloops==1);
        check("blocks starting game under modal",!ReleaseNotes.available(c));
        c.screenKey(0);check("screen keys cannot start run",c.state==GameCore.TITLE && !c.starting());
        SettingsInput input=new SettingsInput();float x=L.w*.5f,y=HighScoreScreen.listTop(L)+L.unit;
        input.touch(c,L,0,5,x,y);input.touch(c,L,1,9,x,y);
        check("other pointer cannot select row",ui.selected==-1);
        input.touch(c,L,1,5,x,y);
        check("settings tap selects saved run",ui.selected==0);
        check("back returns to list",Pause.back(c) && ui.open && ui.selected==-1);
        input.touch(c,L,0,5,x,y);input.touch(c,L,2,5,x,y-L.unit*3);input.touch(c,L,1,5,x,y-L.unit*3);
        check("drag cannot open a row",ui.selected==-1);
        input.touch(c,L,0,5,x,y);input.touch(c,L,3,5,x,y);input.touch(c,L,1,5,x,y);
        check("cancelled gesture cannot select",ui.selected==-1);
        check("back starts list exit",Pause.back(c) && ui.closing);
        ui.update(HighScoreScreen.ENTRY_TIME);
        check("list closes after exit",!ui.open);
        c.preferences.effectsMuted=true;c.preferences.apply(c);ui.show(c);ui.update(HighScoreScreen.ENTRY_TIME);
        check("opening uses muted sound backend",ear.effectsVolume==0f && ui.open);
        ui.back(c);
        for(int[] dimensions:new int[][]{{320,568},{393,852},{1080,2400},{768,1024}}) {
            Layout l=new Layout();l.compute(dimensions[0],dimensions[1],0,24,0,24);
            check("summary fits "+dimensions[0]+"x"+dimensions[1],
                    HighScoreScreen.listTop(l)+HighScoreScreen.size(l)*20f<HighScoreScreen.listBottom(l));
        }
    }
}
