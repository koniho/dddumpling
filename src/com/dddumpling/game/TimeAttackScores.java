package com.dddumpling.game;

/** Time Attack content inside the shared score panel and the run-end screen. */
final class TimeAttackScores extends Draw {
    private TimeAttackScores() {}
    static void content(Painter p,GameCore c,Layout L,int selected) {
        TimeAttackHistory history=c.timeAttack.titleHistory(c);
        int profile=Survival.profile(c.preferences.kids,c.fullRoster),challenge=c.timeAttack.selected;
        float top=HighScoreScreen.listTop(L),bottom=HighScoreScreen.listBottom(L);
        if(selected>=0 && selected<history.displayCount()) {
            p.fillRect(L.w*.065f,top,L.w*.935f,bottom,0xE02A2542);
            TimeAttackHistory.Run run=history.displayRun(selected);
            if(challenge==TimeAttack.ALL && run.encounters.length>0) {
                float s=HighScoreScreen.size(L),offset=Math.min(c.highScoreScreen.scroll,maxScroll(L,run,challenge));
                summary(p,c,L,run,challenge,profile,top-offset,top-offset+s*18);
                for(int boss=0;boss<Boss.COUNT;boss++)bossSummary(p,c,L,run,boss,top-offset+s*(18+boss*13),s);
                float total=contentHeight(L),view=bottom-top,thumb=view*view/total;
                float sy=top+(view-thumb)*offset/Math.max(1,maxScroll(L,run,challenge));
                p.line(L.w*.918f,top,L.w*.918f,bottom,0x20FFFFFF,s*.06f);
                p.line(L.w*.918f,sy,L.w*.918f,sy+thumb,INK_DIM,s*.10f);
            } else summary(p,c,L,run,challenge,profile,top,bottom);
            return;
        }
        if(history.displayCount()==0) {
            Trinket.drawReacting(p,SurvivalDemo.companion(c),L.w*.5f,top+(bottom-top)*.25f,L.unit*2,c.clock,1,8,0);
            p.text("Your first clear starts the list.",L.w*.5f,top+(bottom-top)*.43f,type(L.unit*.58f),INK_DIM,Painter.CENTER,false);
            return;
        }
        float height=HighScoreScreen.rowHeight(c,L);
        for(int i=0;i<history.displayCount();i++) {
            TimeAttackHistory.Run run=history.displayRun(i);
            float y=top+height*(i+.5f),s=Math.min(HighScoreScreen.size(L),height/3.2f),font=type(s*.54f);
            boolean latest=history.latest!=null && history.latest.id==run.id;
            if(latest)p.fillRect(L.w*.08f,y-height*.5f,L.w*.92f,y+height*.5f,0x25FFCF6A);
            Trinket.draw(p,Math.max(0,run.character),L.w*.14f,y,s*.85f,c.clock,run.character>=0,1);
            p.text(TimeAttack.time(run.duration),L.w*.24f,y+font*.36f,font,INK,Painter.LEFT,true);
            p.text(run.won?"CLEAR":"ATTEMPT",L.w*.55f,y+font*.36f,font*.85f,run.won?GOLD:INK_DIM,Painter.LEFT,true);
            p.text(latest?"LATEST":run.legacy()?"EARLIER":Integer.toString(i+1),L.w*.85f,y+font*.36f,font*.75f,INK_DIM,Painter.RIGHT,false);
            p.line(L.w*.10f,y+height*.48f,L.w*.90f,y+height*.48f,0x25FFFFFF,s*.05f);
        }
    }
    private static float contentHeight(Layout L) {return HighScoreScreen.size(L)*(19+Boss.COUNT*13);}
    static float maxScroll(Layout L,TimeAttackHistory.Run run,int challenge) {
        return challenge==TimeAttack.ALL && run.encounters.length>0?
                Math.max(0,contentHeight(L)-(HighScoreScreen.listBottom(L)-HighScoreScreen.listTop(L))):0;
    }
    private static void bossSummary(Painter p,GameCore c,Layout L,TimeAttackHistory.Run run,int boss,float top,float s) {
        TimeAttackHistory.Encounter e=null;
        for(TimeAttackHistory.Encounter encounter:run.encounters)if(encounter.boss==boss)e=encounter;
        BossCollect.draw(p,boss,L.w*.16f,top+s*1.5f,s*.9f,c.clock,true,1);
        String name=Boss.NAMES[boss];
        p.text(name,L.w*.24f,top+s*1.8f,Math.min(type(s*.8f),L.w*.62f/(name.length()*.73f)),GOLD,Painter.LEFT,true);
        float font=Math.min(type(s*.5f),L.w*.026f);
        p.text(e==null?"NOT REACHED":e.cleared?"CLEARED":"ATTEMPT",L.w*.24f,top+s*3f,font,INK_DIM,Painter.LEFT,true);
        String[] labels={"COMBAT TIME","ACCURACY","PROJECTILES DEFENDED","DAMAGE TAKEN","TIME TO FIRST DAMAGE"};
        String[] values=e==null?new String[]{"--","--","--","--","--"}:new String[]{TimeAttack.time(Math.max(1,e.duration)),
                e.accuracy(),Integer.toString(e.defended),Integer.toString(e.damage),e.firstDamageText()};
        for(int i=0;i<labels.length;i++)HighScoreScreen.stat(p,L.w*.13f,L.w*.73f,top+s*(4.5f+i*1.6f),font,labels[i],values[i]);
        p.line(L.w*.12f,top+s*12f,L.w*.88f,top+s*12f,0x35FFFFFF,s*.04f);
    }
    static void summary(Painter p,GameCore c,Layout L,TimeAttackHistory.Run run,int challenge,int profile,float top,float bottom) {
        float s=(bottom-top)/18f,x=L.w*.5f,font=Math.min(type(s*.5f),L.w*.026f);
        String name=TimeAttack.name(challenge);
        p.text(name,x,top+s*1.2f,Math.min(type(s*.85f),L.w*.8f/(name.length()*.73f)),GOLD,Painter.CENTER,true);
        p.text(Survival.profileName(profile),x,top+s*2.3f,font,INK_DIM,Painter.CENTER,true);
        if(run.character>=0)Trinket.drawReacting(p,run.character,L.w*.25f,top+s*4.5f,s*1.25f,c.clock,1,run.won?8:5,0);
        else Trinket.draw(p,0,L.w*.25f,top+s*4.5f,s*1.25f,c.clock,false,1);
        String duration=TimeAttack.time(run.duration);
        p.text(duration,L.w*.63f,top+s*4.7f,Math.min(type(s*1.25f),L.w*.53f/(duration.length()*.73f)),INK,Painter.CENTER,true);
        p.text(run.won?"CLEAR TIME":"ATTEMPT TIME",L.w*.63f,top+s*6f,font,INK_DIM,Painter.CENTER,true);
        p.line(L.w*.12f,top+s*7f,L.w*.88f,top+s*7f,0x35FFFFFF,s*.04f);
        String[] labels={"ACCURACY","PROJECTILES DEFENDED","DAMAGE TAKEN","TIME TO FIRST DAMAGE","BOSSES CLEARED"};
        String[] values={run.accuracy(),run.legacy()?"--":Integer.toString(run.defended),run.legacy()?"--":Integer.toString(run.damage),
                run.firstDamageText(),run.cleared+" / "+(challenge==TimeAttack.ALL?Boss.COUNT:1)};
        for(int i=0;i<labels.length;i++)HighScoreScreen.stat(p,L.w*.13f,L.w*.73f,top+s*(8.5f+i*1.6f),font,labels[i],values[i]);
        String note=run.legacy()?"Earlier record: detailed stats unavailable.":challenge==TimeAttack.ALL?(run.encounters.length==0?"Earlier record: per-boss stats unavailable.":"OVERALL RUN STATS"):"";
        if(!note.isEmpty())p.text(note,x,top+s*17f,Math.min(font,L.w*.78f/(note.length()*.73f)),INK_DIM,Painter.CENTER,false);
    }
}
