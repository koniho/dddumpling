package com.dddumpling.game;

/** Confirmed service rank, shown between runs without owning input. */
final class LeaderboardNotice extends Draw {
    static final float DURATION=3.6f;
    int board=-1;
    long rank,value;
    float age;
    String owner="";
    boolean systemPending,systemWaiting,recorded;
    static boolean safe(GameCore c) {
        return (c.state==GameCore.TITLE && UpdatePrompt.suitable(c)
                || c.state==GameCore.OVER && c.overFade()>=1f)
                && !c.paused && !c.settingsOpen && !c.cloudUi.open && !c.updatePrompt.open
                && !c.updatePrompt.pending && !c.preferences.externalBusy && !c.bossRetry.pending
                && !c.highScoreScreen.open && !c.caseOpen && !c.storyOpen() && !c.releaseNotes.open
                && c.returnFade<=0 && !c.starting() && !c.onboarding.titleGuide
                && !c.onboarding.briefing && c.onboarding.practice==null;
    }
    void update(GameCore c,float elapsed) {
        if(board>=0 && c.leaderboards.owner().isEmpty())return;
        if(board>=0 && !owner.equals(c.leaderboards.owner())) {board=-1;age=0;}
        if(!safe(c))return;
        if(board>=0) {
            if(systemPending || systemWaiting)return;
            if(!recorded) {
                c.leaderboards.noticeShown(owner,board,value);recorded=true;
                if(c.sound!=null)c.sound.uiBloop();
            }
            age+=Math.max(0,Math.min(elapsed,.1f));
            if(age>=DURATION)board=-1;
            return;
        }
        int next=c.leaderboards.nextNotice();
        if(next<0)return;
        board=next;rank=c.leaderboards.noticeRank(board);owner=c.leaderboards.owner();age=0;
        value=c.leaderboards.rankedValue(board);recorded=false;
        systemPending=true;systemWaiting=false;
    }
    int takeSystem(GameCore c) {
        if(board<0 || !systemPending || !safe(c))return -1;
        systemPending=false;systemWaiting=true;return board;
    }
    void systemResult(GameCore c,String player,int requested,long position,boolean shown) {
        if(board!=requested || rank!=position || !owner.equals(player))return;
        systemPending=systemWaiting=false;
        if(shown) {c.leaderboards.noticeShown(owner,board,value);board=-1;}
        else age=0;
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(board<0 || !owner.equals(c.leaderboards.owner()) || systemPending || systemWaiting || !safe(c))return;
        float u=Math.min(L.unit,L.w/20f),fade=Math.min(1,Math.min(age/.2f,(DURATION-age)/.3f));
        float x=L.w*.5f,y=L.topSafe+u*2.5f-(1-fade)*u;
        Painter ink=new OpacityPainter(p,Math.max(0,fade));
        float[] card=pill(x,y,L.w*.45f,u*2.15f,12);
        ink.fillPoly(card,0xF529203E);ink.strokePoly(card,GOLD,u*.1f);
        ink.text("ON THE LEADERBOARD!",x,y-u*.95f,type(u*.57f),GOLD,Painter.CENTER,true);
        String position="RANK "+rank;
        ink.text(position,x,y+u*.35f,Math.min(type(u*1.12f),L.w*.78f/(position.length()*.73f)),INK,Painter.CENTER,true);
        String title="GLOBAL / "+LeaderboardCatalog.title(board);
        ink.text(title,x,y+u*1.4f,Math.min(type(u*.48f),L.w*.8f/(title.length()*.73f)),INK_DIM,Painter.CENTER,false);
        for(int i=0;i<2;i++) {
            float sx=x+(i==0?-1:1)*L.w*.39f,sy=y-u*.2f;
            float r=u*(.22f+.05f*(float)Math.sin(age*5+i));
            ink.line(sx-r,sy,sx+r,sy,GOLD,u*.09f);ink.line(sx,sy-r,sx,sy+r,GOLD,u*.09f);
        }
    }
}
