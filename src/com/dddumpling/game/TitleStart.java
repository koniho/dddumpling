package com.dddumpling.game;

/** A bouncy invitation until the first friend and tutorial unlock the regular title controls. */
final class TitleStart extends Draw {
    private TitleStart() {}
    static boolean visible(GameCore c) {
        return c.modes.entryAvailable(c) && Starter.eligible(c) && !c.starter.open;
    }
    static float y(Layout L) { return L.h*.49f; }
    static boolean hit(GameCore c,Layout L,float x,float y) {
        return visible(c) && Math.abs(x-L.w*.5f)<=L.w*.43f && Math.abs(y-y(L))<=L.w*.16f;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!visible(c))return;
        float h=L.w*.115f,cy=y(L);
        word(p,c,"LET'S",L.w*.5f,cy-h*.25f,h,0);
        word(p,c,"SQUISH",L.w*.5f,cy+h*1.05f,h,3);
        for(int i=0;i<6;i++) {
            float phase=c.clock*1.5f+i*1.7f;
            float x=L.w*.5f+(i%2==0?-1:1)*L.w*(.36f+.025f*(float)Math.sin(phase));
            float y=cy+(i/2-1)*h*.9f+h*.12f*(float)Math.cos(phase);
            float r=L.unit*(.10f+.08f*(.5f+.5f*(float)Math.sin(phase)));
            int color=fadeBy(Glyph.COLOR[i%Glyph.COUNT],.7f);
            p.line(x-r,y,x+r,y,color,L.unit*.05f);
            p.line(x,y-r,x,y+r,color,L.unit*.05f);
        }
    }
    private static void word(Painter p,GameCore c,String text,float cx,float y,float h,int palette) {
        float width=0;
        for(int i=0;i<text.length();i++)width+=h*(text.charAt(i)=='\''?.30f:1.03f);
        float left=cx-width*.5f;
        for(int i=0;i<text.length();i++) {
            float phase=c.clock*2.8f-i*.6f,hop=Math.max(0,(float)Math.sin(phase));
            float advance=h*(text.charAt(i)=='\''?.30f:1.03f);
            float x=left+advance*.5f,baseline=y-h*.10f*hop;
            left+=advance;
            int color=Glyph.COLOR[(i+palette)%Glyph.COUNT];
            if(text.charAt(i)=='\'') {
                p.fillEllipse(x,baseline-h*.72f,h*.055f,h*.11f,color);
            } else TitleBubbleFont.draw(p,text.charAt(i),x,baseline,h,color,1f,phase,1f+.06f*(float)Math.sin(phase));
        }
    }
}
