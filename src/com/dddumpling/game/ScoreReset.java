package com.dddumpling.game;

/** Confirmation and a single brief celebration, owned by the Settings panel. */
final class ScoreReset extends Draw {
    static final int OPEN=12, CONFIRM=13, CANCEL=14;
    static final float TIME=1.5f;
    boolean confirming, failed;
    float left;
    boolean active() { return confirming || left>0f; }
    void cancel() { confirming=failed=false;left=0f; }
    void update(float elapsed) { left=Math.max(0f,left-elapsed); }
    void action(GameCore c,int action) {
        if(left>0f) return;
        if(action==OPEN) { confirming=true;failed=false; }
        else if(action==CANCEL) cancel();
        else if(action==CONFIRM && confirming) {
            if(!c.resetHighScores()) { failed=true;return; }
            confirming=failed=false;left=TIME;
            if(c.sound!=null) c.sound.uiBloop();
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
        float hop=done?Math.abs((float)Math.sin(age*Softbody.TAU/ .7f))*(1f-age/TIME):0f;
        ReleaseMascot.steamer(p,x+s*3f,y+s*.6f,s*2.2f,c.clock,hop*.3f);
        Kawaii.draw(p,Kawaii.DUMPLING,x-s*3f,y-s*hop*2.5f,s*1.8f,
                Collect.BODY[0],1f-hop*.18f,1f);
        if(done && age<.85f) {
            float u=age/.85f;
            for(int i=0;i<7;i++) {
                float angle=i*Softbody.TAU/7f;
                float px=x+(float)Math.cos(angle)*s*(1f+u*6f);
                float py=y-s*2f+(float)Math.sin(angle)*s*(1f+u*4f);
                int tint=Glyph.withAlpha(Glyph.COLOR[i%Glyph.COUNT],(int)(255*(1f-u)));
                p.text(i%2==0?"0":"9",px,py,type(s*.65f*(1f-u)),tint,Painter.CENTER,true);
                p.fillCircle(px+s*.4f,py-s*.6f,s*.12f*(1f-u),tint);
            }
        }
        p.text(done?"SCORES RESET!":failed?"COULDN'T SAVE RESET":"RESET HIGH SCORES?",
                x,y-s*6f,type(s*.78f),done?GOLD:INK,Painter.CENTER,true);
        p.text(done?"Ready for a fresh record!":"Your squishies and progress stay.",
                x,y+s*4f,type(s*.52f),INK_DIM,Painter.CENTER,false);
        if(!done) for(int i=0;i<2;i++) {
            float l=i==0?PlayerSettings.left(L)+s:L.w*.5f+s*.4f;
            float r=i==0?L.w*.5f-s*.4f:PlayerSettings.right(L)-s;
            p.fillRect(l,buttonsY(L)-s*1.4f,r,buttonsY(L)+s*1.4f,i==0?0x334DCEAA:0x55F080A8);
            p.text(i==0?"KEEP SCORES":failed?"TRY AGAIN":"RESET SCORES",(l+r)*.5f,
                    buttonsY(L)+s*.25f,type(s*.53f),INK,Painter.CENTER,true);
        }
    }
}
