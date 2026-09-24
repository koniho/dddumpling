package com.dddumpling.game;

/** Confirmation and a three-stroke blackboard wipe, owned by the Settings panel. */
final class ScoreReset extends Draw {
    static final int OPEN=12, CONFIRM=13, CANCEL=14;
    static final float LEAD=.14f, STROKE_TIME=.34f, STROKE_GAP=.08f, TIME=1.8f;
    private static final int MELON=14;
    boolean confirming, failed;
    float left;
    int brushed;
    boolean active() { return confirming || left>0f; }
    void cancel() { confirming=failed=false;left=0f;brushed=0; }
    void update(GameCore c,float elapsed) {
        if(left<=0f) return;
        float before=TIME-left;
        left=Math.max(0f,left-elapsed);
        float age=TIME-left;
        for(int stroke=brushed;stroke<3;stroke++) {
            float start=LEAD+stroke*(STROKE_TIME+STROKE_GAP);
            if(before<start && age>=start && c.sound!=null) c.sound.scoreResetBrush(stroke);
            if(age>=start) brushed=stroke+1;
        }
    }
    void action(GameCore c,int action) {
        if(left>0f) return;
        if(action==OPEN) { confirming=true;failed=false; }
        else if(action==CANCEL) cancel();
        else if(action==CONFIRM && confirming) {
            if(!c.resetHighScores()) { failed=true;return; }
            confirming=failed=false;left=TIME;brushed=0;
            if(c.sound!=null) c.sound.scoreResetConfirm();
        }
    }
    static float center(Layout L) { return (PlayerSettings.top(L)+PlayerSettings.bottom(L))*.5f; }
    static float buttonsY(Layout L) { return center(L)+PlayerSettings.unit(L)*7f; }
    int hit(Layout L,float x,float y) {
        if(left>0f) return 0;
        float s=PlayerSettings.unit(L);
        if(x<PlayerSettings.left(L) || x>PlayerSettings.right(L) || y<PlayerSettings.top(L)
                || y>PlayerSettings.bottom(L) || y<PlayerSettings.top(L)+s*3f) return CANCEL;
        if(Math.abs(y-buttonsY(L))<s*1.4f) return x<L.w*.5f?CANCEL:CONFIRM;
        return 0;
    }
    void draw(Painter p,GameCore c,Layout L) {
        float s=PlayerSettings.unit(L),x=L.w*.5f,y=center(L);
        boolean done=left>0f;
        float age=done?TIME-left:0f;
        blackboard(p,c,L,x,y,s,done,age);
        p.text(done?"SCORES RESET!":failed?"COULDN'T SAVE RESET":"RESET HIGH SCORES?",
                x,y-s*6f,type(s*.78f),done?GOLD:INK,Painter.CENTER,true);
        p.text(done?"Ready for a fresh record!":"Your squishies and progress stay.",
                x,y+s*2.8f,type(s*.52f),INK_DIM,Painter.CENTER,false);
        if(!done) for(int i=0;i<2;i++) {
            float l=i==0?PlayerSettings.left(L)+s:L.w*.5f+s*.4f;
            float r=i==0?L.w*.5f-s*.4f:PlayerSettings.right(L)-s;
            p.fillRect(l,buttonsY(L)-s*1.4f,r,buttonsY(L)+s*1.4f,i==0?0x334DCEAA:0x55F080A8);
            p.text(i==0?"KEEP SCORES":failed?"TRY AGAIN":"RESET SCORES",(l+r)*.5f,
                    buttonsY(L)+s*.25f,type(s*.53f),INK,Painter.CENTER,true);
        }
    }

    private static float strokeProgress(float age,int stroke) {
        float start=LEAD+stroke*(STROKE_TIME+STROKE_GAP);
        float u=Math.max(0f,Math.min(1f,((age-start)/STROKE_TIME-.18f)/.62f));
        return u*u*(3f-2f*u);
    }

    private static float ease(float u) {
        u=Math.max(0f,Math.min(1f,u));return u*u*(3f-2f*u);
    }

    private static void blackboard(Painter p,GameCore c,Layout L,float x,float y,float s,
            boolean wiping,float age) {
        // Half the original board size, leaving Melon Wedge clearly beside it rather than
        // underneath it. During each wipe the whole character turns and crosses one chalk line.
        float l=x-s*.85f,r=x+s*4.45f,t=y-s*3.4f,b=y-s*.2f;
        int frame=0xFF9C6946,board=0xFF171C1B,chalk=0xFFDDE8D4;
        p.fillRect(l-s*.24f,t-s*.24f,r+s*.24f,b+s*.24f,frame);
        p.fillRect(l,t,r,b,board);
        for(int stroke=0;stroke<3;stroke++) {
            float lineY=t+s*(.72f+stroke*.88f);
            float wiped=wiping?strokeProgress(age,stroke):0f;
            float start=l+s*.38f+(r-l-s*.76f)*wiped;
            if(wiped<1f) {
                p.line(start,lineY,r-s*.38f,lineY,Glyph.withAlpha(chalk,205),s*.09f);
                p.line(start,lineY+s*.14f,r-s*(.65f+stroke*.18f),lineY+s*.14f,
                        Glyph.withAlpha(chalk,72),s*.04f);
            }
        }
        int stroke=wiping?Math.min(2,Math.max(0,(int)((age-LEAD)/(STROKE_TIME+STROKE_GAP)))):0;
        float wipe=wiping?strokeProgress(age,stroke):0f;
        float homeX=x-s*3.1f,homeY=y-s*1.25f;
        float melonX=homeX,melonY=homeY,angle=0f;
        if(wiping && age>=LEAD) {
            float start=LEAD+stroke*(STROKE_TIME+STROKE_GAP),local=age-start;
            float lineY=t+s*(.72f+stroke*.88f);
            float lineL=l+s*.38f,lineR=r-s*.38f;
            if(local<STROKE_TIME) {
                float u=Math.max(0f,local/STROKE_TIME);
                melonX=lineL+(lineR-lineL)*wipe;melonY=lineY;
                float turn=u<.18f?ease(u/.18f):u<.80f?1f:1f-ease((u-.80f)/.20f);
                angle=turn*(float)Math.PI*.5f;
            } else {
                float back=ease((local-STROKE_TIME)/STROKE_GAP);
                melonX=lineR+(homeX-lineR)*back;
                melonY=lineY+(homeY-lineY)*back;
            }
        }
        Painter melon=new LinkedPairArt.TurnPainter(p,melonX,melonY,angle);
        Trinket.drawReacting(melon,MELON,0f,0f,s*1.65f,c.clock,1f,1,0f);
    }
}
