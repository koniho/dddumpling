package com.dddumpling.game;

/** A bouncy invitation until the first friend and tutorial unlock the regular title controls. */
final class TitleStart extends Draw {
    static final float SQUEEZE=.32f, BURST=.60f, TRANSITION=SQUEEZE+BURST;
    private TitleStart() {}
    static boolean transitioning(GameCore c) {
        return c.state==GameCore.TITLE && c.starter.open && !c.starter.exiting && c.starter.age<TRANSITION;
    }
    private static float ease(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}

    static boolean visible(GameCore c) {
        return c.modes.entryAvailable(c) && Starter.eligible(c) && !c.starter.open;
    }
    static float y(Layout L) { return L.h*.49f; }
    static boolean hit(GameCore c,Layout L,float x,float y) {
        return visible(c) && Math.abs(x-L.w*.5f)<=L.w*.43f && Math.abs(y-y(L))<=L.w*.16f;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        boolean leaving=transitioning(c);
        if(!visible(c) && !leaving)return;
        float h=L.w*.115f,cy=y(L);
        float burst=leaving?Math.max(0,(c.starter.age-SQUEEZE)/BURST):0;
        if(burst>0) {
            float width=L.w*(.009f-.003f*burst),r=width*7f+L.w*.72f*burst;
            float fade=Math.min(1f,burst/.075f)*(1-burst)*(1-burst);
            for(int side=-1;side<=1;side+=2)
                Renderer.rainbowRing(p,L.w*(.5f+side*.095f),cy,r,width,fade);
        }
        word(p,c,"LET'S",L.w*.5f,cy-h*.25f,h,0,0,L);
        word(p,c,"SQUISH",L.w*.5f,cy+h*1.05f,h,3,5,L);
        for(int i=0;i<6;i++) {
            float phase=c.clock*1.5f+i*1.7f;
            float x=L.w*.5f+(i%2==0?-1:1)*L.w*(.36f+.025f*(float)Math.sin(phase));
            float y=cy+(i/2-1)*h*.9f+h*.12f*(float)Math.cos(phase);
            float r=L.unit*(.10f+.08f*(.5f+.5f*(float)Math.sin(phase)));
            int color=fadeBy(Glyph.COLOR[i%Glyph.COUNT],.7f*(leaving?1-ease(c.starter.age/SQUEEZE):1));
            p.line(x-r,y,x+r,y,color,L.unit*.05f);
            p.line(x,y-r,x,y+r,color,L.unit*.05f);
        }
    }
    private static void word(Painter p,GameCore c,String text,float cx,float y,float h,int palette,int first,Layout L) {
        boolean leaving=transitioning(c);
        float squeeze=leaving?ease(c.starter.age/SQUEEZE):0;
        float burst=leaving?Math.max(0,(c.starter.age-SQUEEZE)/BURST):0;
        float clock=c.clock-(leaving?c.starter.age:0);
        float width=0;
        for(int i=0;i<text.length();i++)width+=h*(text.charAt(i)=='\''?.30f:1.03f);
        float left=cx-width*.5f;
        for(int i=0;i<text.length();i++) {
            float phase=clock*2.8f-i*.6f,hop=Math.max(0,(float)Math.sin(phase));
            float advance=h*(text.charAt(i)=='\''?.30f:1.03f);
            float x=left+advance*.5f,baseline=y-h*.10f*hop;
            left+=advance;
            float shape=1f+.06f*(float)Math.sin(phase),size=h,fade=1f;
            int color=Glyph.COLOR[(i+palette)%Glyph.COUNT];
            if(leaving) {
                float center=y(L);
                x=cx+(x-cx)*(1-.86f*squeeze);
                float letterCenter=baseline-h*.4f;
                letterCenter=center+(letterCenter-center)*(1-.80f*squeeze);
                size=h*(1-.20f*squeeze);
                shape+=(.45f*squeeze);
                if(burst>0) {
                    int index=first+i;
                    float angle=index*6.283185f/11f+.23f;
                    float travel=1-(1-burst)*(1-burst);
                    float distance=L.h*(.85f+.06f*(index%3));
                    float dx=(float)Math.cos(angle)*distance,dy=(float)Math.sin(angle)*distance;
                    x+=dx*travel;letterCenter+=dy*travel+L.h*.10f*burst*burst;
                    fade=1-ease((burst-.45f)/.55f);
                    size*=1+.16f*(float)Math.sin(burst*8+index);
                    shape=1+.25f*(float)Math.sin(burst*10+index);
                    p.line(x-dx*.045f*(1-burst),letterCenter-dy*.045f*(1-burst),x,letterCenter,
                            fadeBy(color,fade*.35f),size*.08f);
                }
                baseline=letterCenter+size*.4f;
            }
            if(text.charAt(i)=='\'') {
                p.fillEllipse(x,baseline-size*.72f,size*.055f,size*.11f,fadeBy(color,fade));
            } else TitleBubbleFont.draw(p,text.charAt(i),x,baseline,size,color,fade,phase,shape);
        }
    }
}
