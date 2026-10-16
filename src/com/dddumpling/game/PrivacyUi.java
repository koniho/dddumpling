package com.dddumpling.game;

/** Title entry for player settings; the policy link lives inside. */
final class PrivacyUi extends Draw {
    static final String URL = "https://koniho.github.io/dddumpling-privacy/";
    static boolean visible(GameCore c) {
        return c.state == GameCore.TITLE && !c.starting() && !c.caseOpen && !c.storyOpen() && !c.settingsOpen && !c.releaseNotes.open && c.returnFade<=0f;
    }
    static boolean hit(GameCore c, Layout L, float x, float y) {
        return visible(c) && x >= L.w - 7f * L.unit && x <= L.w
                && y >= L.dangerY - L.unit && y <= L.dangerY + L.unit;
    }
    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        float x=L.w-L.unit*2.5f,y=L.dangerY,r=L.unit*.95f;
        float beat=c.clock%3.2f;
        float bounce=beat<.85f?(float)Math.sin(beat/.85f*Math.PI):0f;
        float squash=1f+.055f*(float)Math.sin(c.clock*2.4f)-bounce*.10f;
        p.fillEllipse(x,y+r*.95f,r*.8f,r*.13f,0x44352B42);
        if(c.preferences.notificationPending)ReleaseMascot.notificationGlow(p,x,y,r,c.clock);
        y-=r*bounce*.15f;
        float angle=.10f*(float)Math.sin(c.clock*1.7f);
        gear(p,x,y+r*.10f,r*1.04f,squash,angle,0xFF565481);
        gear(p,x,y,r,squash,angle,0xFF9D9ADB);
        gear(p,x,y-r*.06f,r*.88f,squash,angle,0xFFC6C8F5);
        p.fillEllipse(x,y+r*.03f,r*.59f*squash,r*.59f/squash,0xFF7175AA);
        p.fillEllipse(x,y+r*.10f,r*.49f*squash,r*.48f/squash,0xFFB3DCF0);
        p.arc(x,y,r*.79f*squash,r*.79f/squash,205,92,0xCCF3F6FF,r*.075f);
        int ink=0xFF35324F;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.21f*squash;
            p.fillEllipse(ex,y+r*.04f,r*.065f,r*.10f/squash,ink);
            p.fillCircle(ex-r*.016f,y+r*.005f,r*.022f,0xFFFFFAEF);
            p.fillEllipse(x+side*r*.35f*squash,y+r*.22f,r*.105f,r*.055f,0xBFF49FBF);
        }
        p.arc(x,y+r*.16f,r*.13f,r*.10f,0,180,ink,r*.04f);
    }
    private static void gear(Painter p,float x,float y,float r,float squash,float angle,int color) {
        float[] outline=new float[192];
        for(int i=0;i<96;i++) {
            float a=i*Softbody.TAU/96f;
            float tooth=Math.max(0f,Math.min(1f,.5f+1.1f*(float)Math.cos(a*8f)));
            tooth=tooth*tooth*(3f-2f*tooth);
            float radius=r*(.77f+.23f*tooth);
            outline[i*2]=x+radius*(float)Math.cos(a+angle)*squash;
            outline[i*2+1]=y+radius*(float)Math.sin(a+angle)/squash;
        }
        p.fillPoly(outline,color);
    }
}
