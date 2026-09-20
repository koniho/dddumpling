package com.dddumpling.game;

/** Saved runs in the release-notes glass, using the Settings gesture and back routes. */
final class HighScoreScreen extends Draw {
    static final int CLOSE=-1, BACK=-2, ROW=2000;
    boolean open;
    int selected=-1;
    float scroll;
    static float size(Layout L) { return Math.min(ReleaseNotes.size(L),(L.dangerY-L.topSafe)/27f); }
    static float top(Layout L) { return L.topSafe+size(L); }
    static float bottom(Layout L) { return Math.min(L.dangerY-size(L)*.6f,L.h-L.padB-size(L)); }
    static float listTop(Layout L) { return top(L)+size(L)*3.5f; }
    static float listBottom(Layout L) { return bottom(L)-size(L); }
    static float rowHeight(Layout L) { return size(L)*8f; }
    float maxScroll(GameCore c,Layout L) {
        return Math.max(0f,c.highScores.runs.size()*rowHeight(L)-(listBottom(L)-listTop(L)));
    }
    void scrollTo(GameCore c,Layout L,float value) { scroll=Math.max(0f,Math.min(maxScroll(c,L),value)); }
    static boolean entryHit(GameCore c,Layout L,float x,float y) {
        return ReleaseNotes.available(c) && !c.releaseNotes.open
                && Math.abs(x-L.w*.5f)<L.w*.30f && Math.abs(y-L.h*.292f)<L.unit*1.1f;
    }
    void show(GameCore c) {
        if(!ReleaseNotes.available(c) || c.releaseNotes.open) return;
        Pause.release(c);open=true;selected=-1;scroll=0f;feedback(c);
    }
    private void feedback(GameCore c) { if(c.sound!=null) c.sound.uiBloop(); }
    void back(GameCore c) {
        if(selected>=0) selected=-1;else open=false;
        feedback(c);
    }
    int hit(GameCore c,Layout L,float x,float y) {
        float s=size(L),t=top(L);
        if(x<L.w*.04f || x>L.w*.96f || y<t || y>bottom(L)) return CLOSE;
        if(x>L.w*.82f && y<t+2.5f*s) return CLOSE;
        if(selected>=0) return x<L.w*.21f && y<t+2.5f*s?BACK:0;
        if(y<listTop(L) || y>listBottom(L)) return 0;
        int row=(int)((y-listTop(L)+scroll)/rowHeight(L));
        return row<c.highScores.runs.size()?ROW+row:0;
    }
    void action(GameCore c,int hit) {
        if(hit==CLOSE) { open=false;feedback(c); }
        else if(hit==BACK) back(c);
        else if(hit>=ROW && hit<ROW+c.highScores.runs.size()) { selected=hit-ROW;feedback(c); }
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!open) return;
        scrollTo(c,L,scroll);
        float s=size(L),t=top(L),b=bottom(L);
        p.fillRect(0,0,L.w,L.h,0x990F1026);
        glassPanel(p,L.w*.04f,t,L.w*.96f,b,s);
        float x=L.w*.89f,y=t+s*1.25f,r=s*.3f;
        p.line(x-r,y-r,x+r,y+r,INK,s*.12f);
        p.line(x-r,y+r,x+r,y-r,INK,s*.12f);
        if(selected>=0) {
            x=L.w*.13f;
            p.polyline(new float[]{x+r,y-r,x-r,y,x+r,y+r},INK,s*.12f);
        }
        p.text(selected<0?"HIGH SCORES":"RUN SUMMARY",L.w*.5f,t+s*1.7f,type(s*.8f),GOLD,Painter.CENTER,true);
        p.save();p.clipRect(L.w*.07f,listTop(L),L.w*.93f,listBottom(L));
        if(selected>=0 && selected<c.highScores.runs.size()) summary(p,c,L,c.highScores.runs.get(selected));
        else if(c.highScores.runs.isEmpty()) {
            Kawaii.moodDumpling(p,L.w*.5f,t+8f*s,2f*s,Glyph.COLOR[0],.7f,1f);
            p.text("Your next run starts the list.",L.w*.5f,t+12f*s,type(s*.52f),INK_DIM,Painter.CENTER,false);
        } else {
            for(int i=0;i<c.highScores.runs.size();i++) {
                float ry=listTop(L)+i*rowHeight(L)-scroll;
                if(ry+rowHeight(L)<listTop(L) || ry>listBottom(L)) continue;
                row(p,c,L,c.highScores.runs.get(i),i,ry);
            }
        }
        p.restore();
        if(selected<0 && maxScroll(c,L)>0f) {
            float h=listBottom(L)-listTop(L),total=c.highScores.runs.size()*rowHeight(L);
            float thumb=h*h/total,sy=listTop(L)+(h-thumb)*scroll/maxScroll(c,L);
            p.line(L.w*.945f,sy,L.w*.945f,sy+thumb,INK_DIM,s*.10f);
        }
    }
    private void score(Painter p,GameCore c,Layout L,HighScores.Run run,float y) {
        float s=size(L);
        if(run.id==c.highScores.latest) {
            for(int i=4;i>0;i--) p.fillEllipse(L.w*.5f,y-s*.42f,s*(3.2f+i*.25f),s*(.65f+i*.15f),
                    Glyph.withAlpha(GOLD,(int)((13-i*2)*(1f+.15f*Math.sin(c.clock*3f)))));
        }
        p.text(String.valueOf(run.score),L.w*.5f,y,type(s*.95f),INK,Painter.CENTER,true);
    }
    private void bosses(Painter p,GameCore c,Layout L,HighScores.Run run,float y) {
        float s=size(L);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            boolean won=(run.bosses&(1<<boss))!=0;
            BossCollect.draw(p,boss,L.w*.5f+(boss-1.5f)*s*2.8f,y,s*.85f,c.clock,won,won?1f:.32f);
        }
    }
    private void row(Painter p,GameCore c,Layout L,HighScores.Run run,int rank,float y) {
        float s=size(L);
        p.text(String.valueOf(rank+1),L.w*.105f,y+s*1.35f,type(s*.58f),INK_DIM,Painter.LEFT,true);
        score(p,c,L,run,y+s*1.5f);
        if(run.id==c.highScores.latest) p.text("LATEST",L.w*.895f,y+s*1.3f,type(s*.37f),GOLD,Painter.RIGHT,true);
        p.text(run.blurb(),L.w*.5f,y+s*2.7f,type(s*.41f),INK_DIM,Painter.CENTER,false);
        bosses(p,c,L,run,y+s*4.3f);
        float left=L.w*.13f,step=L.w*.205f;
        p.text("STAGES",left,y+s*5.8f,type(s*.35f),INK_DIM,Painter.CENTER,false);
        p.text(String.valueOf(run.stages),left,y+s*7.3f,type(s*.57f),GOLD,Painter.CENTER,true);
        for(int i=0;i<3;i++) {
            float x=left+(i+1)*step;
            icon(p,i,x,y+s*5.85f,s*.5f,c.clock);
            int count=i==0?run.dumplings:i==1?run.powers:run.swipes;
            p.text(String.valueOf(count),x,y+s*7.3f,type(s*.57f),INK,Painter.CENTER,true);
        }
        p.line(L.w*.10f,y+rowHeight(L)-s*.1f,L.w*.90f,y+rowHeight(L)-s*.1f,0x25FFFFFF,s*.05f);
    }
    private static void icon(Painter p,int kind,float x,float y,float r,float clock) {
        if(kind==0) Kawaii.moodDumpling(p,x,y,r,Glyph.COLOR[0],1f,1f);
        else ReleaseChange.icon(p,kind==1?ReleaseChange.SHUFFLE:ReleaseChange.SWIPE,x,y,r,clock);
    }
    private void summary(Painter p,GameCore c,Layout L,HighScores.Run run) {
        float s=size(L),t=listTop(L),cx=L.w*.5f;
        score(p,c,L,run,t+s*1.3f);
        p.text(run.score>=run.best?"NEW BEST!":"BEST "+run.best,cx,t+s*2.8f,type(s*.53f),GOLD,Painter.CENTER,true);
        p.text(run.blurb(),cx,t+s*3.9f,type(s*.41f),INK_DIM,Painter.CENTER,false);
        int pct=run.accuracy();float mood=Math.max(0f,Math.min(1f,(pct-60f)/30f));
        Kawaii.moodDumpling(p,cx-s*3.2f,t+s*5.7f,s*1.25f,Glyph.COLOR[0],mood,1f);
        p.text("ACCURACY "+pct+"%",cx-s*.5f,t+s*6f,type(s*.50f),INK,Painter.LEFT,true);
        p.text("STAGE "+run.stage+"   SQUISHES "+run.squishes,cx,t+s*7.7f,type(s*.48f),INK_DIM,Painter.CENTER,false);
        p.text("BEST COMBO "+run.combo,cx,t+s*8.7f,type(s*.52f),INK_DIM,Painter.CENTER,false);
        p.text("STAGES COMPLETED "+run.stages,cx,t+s*10.1f,type(s*.48f),GOLD,Painter.CENTER,true);
        bosses(p,c,L,run,t+s*12f);
        String[] labels={"DUMPLINGS","POWERUPS USED","RESCUE SWIPES"};
        int[] counts={run.dumplings,run.powers,run.swipes};
        for(int i=0;i<3;i++) {
            float y=t+s*(14.3f+i*1.7f);
            icon(p,i,L.w*.17f,y-s*.2f,s*.55f,c.clock);
            p.text(labels[i],L.w*.23f,y,type(s*.44f),INK_DIM,Painter.LEFT,false);
            p.text(String.valueOf(counts[i]),L.w*.84f,y,type(s*.53f),INK,Painter.RIGHT,true);
        }
        p.text("START STAGE "+(run.land*Boss.EVERY+1)+(run.kids?"   KIDS MODE":""),cx,t+s*20f,type(s*.4f),INK_DIM,Painter.CENTER,false);
    }
}
