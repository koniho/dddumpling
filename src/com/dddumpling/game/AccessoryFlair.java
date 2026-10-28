package com.dddumpling.game;

/** Bounded, clock-driven vector accents: no blur surfaces, particle pool or gameplay RNG. */
final class AccessoryFlair {
    private static final float[] ELLIPSE=new float[80];
    static {for(int i=0;i<40;i++){float a=i*Softbody.TAU/40;ELLIPSE[i*2]=(float)Math.cos(a);ELLIPSE[i*2+1]=(float)Math.sin(a);}}
    private static final int[] STARS={0xFFFFDF80,0xFFFFC4DC,0xFFCDBAFF};
    private static final int[] WINGS={0xFFDAA9FF,0xFF98EACF,0xFFFFB9D6,0xFFFFD990};
    static void draw(Painter p,int variant,float x,float y,float r,float clock) {
        if(r<=0)return;
        float d=AccessoryArt.detail(p,r);
        if(variant==0)stars(p,x,y,r,clock,d);
        else if(variant==1)butterflies(p,x,y,r,clock,d);
        else glow(p,x,y,r,clock,d);
    }
    private static void stars(Painter p,float x,float y,float r,float t,float d) {
        Painter trim=d>0?new OpacityPainter(p,d):p;
        for(int i=0;i<6;i++) {
            float a=i*Softbody.TAU/6+t*.38f;
            float pulse=.65f+.35f*(float)Math.sin(t*3+i*2.1f);
            float xx=x+(float)Math.cos(a)*r*(1.25f+.07f*pulse);
            float yy=y+(float)Math.sin(a)*r*1.12f;
            float size=r*(.09f+.065f*pulse);int color=STARS[i%STARS.length];
            for(int layer=d>0?3:1;layer>0;layer--)(layer>1?trim:p).fillCircle(xx,yy,size*(1+layer*.65f),
                    Glyph.withAlpha(color,(int)((10+layer*2)*pulse)));
            AccessoryArt.star(p,xx,yy,size,color);
            if(d>0)AccessoryArt.star(trim,xx,yy,size*.44f,0xFFFFF6DE);
        }
    }
    private static void butterflies(Painter p,float x,float y,float r,float t,float d) {
        Painter trim=d>0?new OpacityPainter(p,d):p;
        for(int i=0;i<4;i++) {
            float a=i*Softbody.TAU/4+t*.29f+.35f;
            float xx=x+(float)Math.cos(a)*r*(1.30f+.10f*(float)Math.sin(t*1.3f+i));
            float yy=y+(float)Math.sin(a)*r*1.05f+r*.07f*(float)Math.sin(t*2.8f+i);
            float flap=.28f+.72f*Math.abs((float)Math.sin(t*7+i*1.8f));
            float size=r*(.17f+.025f*(i%2));int color=WINGS[i];
            p.fillCircle(xx,yy,size*1.7f,Glyph.withAlpha(color,16));
            for(int side=-1;side<=1;side+=2) {
                p.fillEllipse(xx+side*size*.66f*flap,yy-size*.30f,size*.76f*flap,size*.88f,color);
                p.fillEllipse(xx+side*size*.48f*flap,yy+size*.60f,size*.57f*flap,size*.60f,
                        Glyph.mix(color,0xFF967EB6,.20f));
                if(d>0)trim.fillEllipse(xx+side*size*.70f*flap,yy-size*.47f,size*.32f*flap,size*.39f,
                        Glyph.mix(color,0xFFFFFFFF,.60f));
            }
            p.fillEllipse(xx,yy,size*.12f,size*.70f,0xFF77658B);
            p.fillCircle(xx,yy-size*.70f,size*.17f,0xFF77658B);
        }
    }
    private static float[] ellipse(float x,float y,float rx,float ry,int step) {
        float[] out=new float[80/step];
        for(int i=0;i<out.length;i+=2) {out[i]=x+rx*ELLIPSE[i*step];out[i+1]=y+ry*ELLIPSE[i*step+1];}
        return out;
    }
    private static void glow(Painter p,float x,float y,float r,float t,float d) {
        Painter trim=d>0?new OpacityPainter(p,d):p;
        float breath=.5f+.5f*(float)Math.sin(t*1.7f);
        int mint=Glyph.mix(0xFF86EBD8,0xFFC6B1FF,.5f+.5f*(float)Math.sin(t*.7f));
        float radius=r*(1.18f+breath*.05f);
        for(int layer=d>0?2:0;layer>=0;layer--) {
            float width=r*(.045f+layer*.075f);
            (layer>0?trim:p).fillContours(new float[][]{ellipse(x,y,radius+width,(radius+width)*.88f,d>0?1:2),
                    ellipse(x,y,radius-width,(radius-width)*.88f,d>0?1:2)},Glyph.withAlpha(mint,12+(2-layer)*11));
        }
        for(int i=0;i<5;i++) {
            float a=i*Softbody.TAU/5-t*.33f;
            float xx=x+(float)Math.cos(a)*r*1.24f,yy=y+(float)Math.sin(a)*r*1.08f;
            float size=r*(.055f+.025f*(float)Math.sin(t*2+i));
            if(d>0)trim.fillCircle(xx,yy,size*2.5f,Glyph.withAlpha(mint,20));
            if(d>0)trim.fillCircle(xx,yy,size*1.5f,Glyph.withAlpha(mint,65));
            p.fillCircle(xx,yy,size,0xC8E9FFF6);
        }
    }
}
