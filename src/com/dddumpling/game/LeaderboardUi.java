package com.dddumpling.game;

/** Browsing native rankings from local records; this never submits a score. */
final class LeaderboardUi extends Draw {
    boolean enabled;
    private int requested=-1,notificationBoard=-1;
    private String notificationOwner="",requestedOwner="";
    private boolean fromNotification;
    void notification(int board,String owner) {
        if(!LeaderboardCatalog.valid(board) || owner==null || owner.isEmpty())return;
        notificationBoard=board;notificationOwner=owner;
    }
    void update(GameCore c) {
        if(notificationBoard<0 || !enabled)return;
        String owner=c.leaderboards.owner();
        if(!owner.isEmpty() && !owner.equals(notificationOwner)) {notificationBoard=-1;return;}
        boolean safe=LeaderboardNotice.safe(c) || (c.state==GameCore.TITLE && c.highScoreScreen.open
                && !c.highScoreScreen.moving() && !c.paused && !c.preferences.externalBusy && !c.cloudUi.open);
        if(!owner.equals(notificationOwner) || !safe)return;
        c.preferences.requestExternal(c,PlayerSettings.LEADERBOARD);
        if(c.preferences.externalAction!=PlayerSettings.LEADERBOARD)return;
        requested=notificationBoard;requestedOwner=owner;fromNotification=true;notificationBoard=-1;
    }
    static int board(GameCore c) {
        if(c.modes.selected==ModeSelector.SURVIVAL)return c.survival.titleProfile(c);
        if(c.modes.selected==ModeSelector.TIME_ATTACK) {
            return LeaderboardCatalog.timeAttack(Survival.profile(c.preferences.kids,c.fullRoster),
                    c.timeAttack.selected);
        }
        return c.modes.adventure() && !c.preferences.kids?LeaderboardCatalog.ADVENTURE:-1;
    }
    boolean canOpen(GameCore c) {
        return enabled && board(c)>=0 && c.state==GameCore.TITLE && c.highScoreScreen.open
                && !c.highScoreScreen.moving() && !c.preferences.externalBusy;
    }
    String hint(GameCore c) {
        if(c.modes.selected==ModeSelector.TIME_ATTACK && board(c)<0)return "6 KEYS / KIDS MODE OFF";
        if(c.modes.adventure() && c.preferences.kids)return "KIDS MODE OFF FOR ONLINE RANKINGS";
        if(!enabled)return "ONLINE RANKINGS UNAVAILABLE";
        return c.preferences.externalBusy && requested>=0?"OPENING LEADERBOARD...":"GLOBAL RANKINGS / ALL TIME";
    }
    void request(GameCore c) {
        if(!canOpen(c))return;
        c.preferences.requestExternal(c,PlayerSettings.LEADERBOARD);
        if(c.preferences.externalAction==PlayerSettings.LEADERBOARD) {requested=board(c);fromNotification=false;}
    }
    int take(GameCore c) {
        int result=requested;requested=-1;
        if(result<0)return -1;
        if(fromNotification) {
            if(!enabled || !requestedOwner.equals(c.leaderboards.owner()) || c.starting() || c.paused
                    || c.settingsOpen || c.cloudUi.open || (c.state!=GameCore.TITLE && c.state!=GameCore.OVER))result=-1;
            fromNotification=false;
        } else if(!enabled || c.state!=GameCore.TITLE || !c.highScoreScreen.open || c.highScoreScreen.closing)result=-1;
        if(result<0)c.preferences.externalFinished(c);
        return result;
    }
    static float y(Layout L) {return HighScoreScreen.bottom(L)-HighScoreScreen.size(L)*2.4f;}
    static boolean hit(Layout L,float x,float y) {
        return Math.abs(x-L.w*.5f)<L.w*.34f && Math.abs(y-y(L))<HighScoreScreen.size(L)*1.05f;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        LeaderboardUi ui=c.leaderboardUi;
        float s=HighScoreScreen.size(L),x=L.w*.5f,y=y(L);
        boolean active=ui.enabled && board(c)>=0;
        int ink=active?GOLD:INK_DIM;
        float[] shape=pill(x,y,L.w*.34f,s*1.05f,12);
        p.fillPoly(shape,active?0xFF3A3156:0xFF29243D);
        p.strokePoly(shape,Glyph.withAlpha(ink,active?220:100),s*.09f);
        float iconX=L.w*.24f,base=y+s*.45f;
        for(int i=0;i<3;i++) {
            float h=s*(i==1?.85f:i==0?.55f:.35f),left=iconX+(i-1)*s*.34f;
            p.fillRect(left,base-h,left+s*.25f,base,ink);
        }
        p.text("LEADERBOARD",x+s*.45f,y+s*.26f,type(s*.68f),ink,Painter.CENTER,true);
        p.text(ui.hint(c),x,HighScoreScreen.bottom(L)-s*.45f,
                Math.min(type(s*.43f),L.w*.82f/(ui.hint(c).length()*.73f)),INK_DIM,Painter.CENTER,false);
    }
}
