package com.dddumpling.game;

/** Confirmed service rank, shown between runs without owning input. */
final class LeaderboardNotice extends Draw {
    static final float DURATION=3.6f;
    static final String PREVIEW_OWNER="developer-notification-preview";
    int board=-1;
    long rank,value;
    float age;
    String owner="";
    boolean systemPending,systemWaiting,recorded,preview;
    void test(GameCore c) {
        if(!BuildFlags.DEVELOPER || board>=0)return;
        board=LeaderboardCatalog.ADVENTURE;rank=12;value=0;age=0;owner=PREVIEW_OWNER;
        preview=systemPending=recorded=true;systemWaiting=false;
    }
    boolean current(GameCore c,String player) {
        return player!=null && (preview && BuildFlags.DEVELOPER?PREVIEW_OWNER.equals(player):player.equals(c.leaderboards.owner()));
    }
    boolean ready(GameCore c) {
        return safe(c) || (preview && BuildFlags.DEVELOPER && c.settingsOpen && c.settingsPage==1
                && !c.cloudUi.open && !c.preferences.externalBusy);
    }
    static void openNotification(GameCore c,int board,String player) {
        if(BuildFlags.DEVELOPER && PREVIEW_OWNER.equals(player)) {
            c.openSettings();c.settingsPage=1;c.settingsTab=SettingsUi.PROGRESS;
        } else c.leaderboardUi.notification(board,player);
    }
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
        if(board>=0 && !preview && c.leaderboards.owner().isEmpty())return;
        if(board>=0 && !current(c,owner)) {board=-1;age=0;}
        if(!ready(c))return;
        if(board>=0) {
            if(systemPending || systemWaiting)return;
            if(!recorded) {
                c.leaderboards.noticeShown(owner,board,value);recorded=true;
                if(c.sound!=null)c.sound.uiBloop();
            }
            age+=Math.max(0,Math.min(elapsed,.1f));
            if(age>=DURATION) {board=-1;preview=false;}
            return;
        }
        preview=false;
        int next=c.leaderboards.nextNotice();
        if(next<0)return;
        board=next;rank=c.leaderboards.noticeRank(board);owner=c.leaderboards.owner();age=0;
        value=c.leaderboards.rankedValue(board);recorded=false;
        systemPending=true;systemWaiting=false;
    }
    int takeSystem(GameCore c) {
        if(board<0 || !systemPending || !ready(c))return -1;
        systemPending=false;systemWaiting=true;return board;
    }
    void systemResult(GameCore c,String player,int requested,long position,boolean shown) {
        if(board!=requested || rank!=position || !owner.equals(player))return;
        systemPending=systemWaiting=false;
        if(shown) {if(!preview)c.leaderboards.noticeShown(owner,board,value);board=-1;preview=false;}
        else age=0;
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(board<0 || !current(c,owner) || systemPending || systemWaiting || !ready(c))return;
        float u=Math.min(L.unit,L.w/20f),fade=Math.min(1,Math.min(age/.2f,(DURATION-age)/.3f));
        float x=L.w*.5f,y=(preview && c.settingsOpen?PlayerSettings.bottom(L)-u*2.7f:L.topSafe+u*2.5f)-(1-fade)*u;
        Painter ink=new OpacityPainter(p,Math.max(0,fade));
        float[] card=pill(x,y,L.w*.45f,u*2.15f,12);
        ink.fillPoly(card,0xF529203E);ink.strokePoly(card,GOLD,u*.1f);
        ink.text(preview?"NOTIFICATION PREVIEW":"ON THE LEADERBOARD!",x,y-u*.95f,type(u*.57f),GOLD,Painter.CENTER,true);
        String position="RANK "+rank;
        ink.text(position,x,y+u*.35f,Math.min(type(u*1.12f),L.w*.78f/(position.length()*.73f)),INK,Painter.CENTER,true);
        String title=(preview?"SAMPLE / ":"GLOBAL / ")+LeaderboardCatalog.title(board);
        ink.text(title,x,y+u*1.4f,Math.min(type(u*.48f),L.w*.8f/(title.length()*.73f)),INK_DIM,Painter.CENTER,false);
        for(int i=0;i<2;i++) {
            float sx=x+(i==0?-1:1)*L.w*.39f,sy=y-u*.2f;
            float r=u*(.22f+.05f*(float)Math.sin(age*5+i));
            ink.line(sx-r,sy,sx+r,sy,GOLD,u*.09f);ink.line(sx,sy-r,sx,sy+r,GOLD,u*.09f);
        }
    }
}
