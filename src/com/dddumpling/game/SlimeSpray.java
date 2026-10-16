package com.dddumpling.game;

import java.util.Random;

/** Slime thrown by a hit: a ballistic arc arrests, hangs briefly, then drips away. */
final class SlimeSpray {
    static final int HIT_DROPS=24, DRAG_DROPS=64;
    static final float FLIGHT=.48f, HANG=.12f;
    private SlimeSpray() {}

    static final class Drop {
        float x,y,phase,gravity;
        final float[] shape=new float[48];
    }

    static void emit(GameCore c,float x,float y,float dx,float dy,float scale,boolean drag) {
        // Cosmetic variation must not change later enemy or prompt generation.
        Random random=new Random(((long)Float.floatToIntBits(c.clock)<<32)
                ^ Float.floatToIntBits(x) ^ (long)c.hits*193L ^ Float.floatToIntBits(y));
        float heading=(float)Math.atan2(dy,dx);
        int count=drag?DRAG_DROPS:HIT_DROPS;
        for(int i=0;i<count;i++) {
            GameCore.Particle p=new GameCore.Particle();
            Drop d=p.slime=new Drop();
            d.x=p.x=x;d.y=p.y=y;d.phase=random.nextFloat()*6.283f;
            d.gravity=scale*9f;
            float angle=heading+(random.nextFloat()-.5f)*(drag?2.9f:1.15f);
            float speed=scale*(6f+random.nextFloat()*10f)*(drag?1.3f:1f);
            p.vx=(float)Math.cos(angle)*speed;p.vy=(float)Math.sin(angle)*speed;
            p.size=scale*(.16f+random.nextFloat()*.26f);
            p.life=p.max=1.15f+random.nextFloat()*.5f;
            p.color=i%3==0?0xFFB9EF43:i%3==1?0xFF80DB2B:0xFF55B941;
            c.particles.add(p);
        }
    }

    static void update(GameCore.Particle p) {
        Drop d=p.slime;
        float age=p.max-p.life,t=Math.min(FLIGHT,age);
        // Ease time to a standstill without losing the curved flight path.
        float travel=t-t*t/(2f*FLIGHT);
        float drip=Math.max(0f,age-FLIGHT-HANG);
        p.x=d.x+p.vx*travel;
        p.y=d.y+p.vy*travel+.5f*d.gravity*travel*travel+drip*drip*d.gravity*.85f;
    }

    static void draw(Painter p,GameCore.Particle q) {
        Drop d=q.slime;
        float age=q.max-q.life,drip=Math.min(1f,Math.max(0f,age-FLIGHT-HANG)*2f);
        float fade=Math.min(1f,q.life/.5f),r=q.size*(.8f+.2f*fade);
        float stretch=1f+drip*.9f,rotation=d.phase+age*2f*(1f-drip);
        for(int layer=0;layer<2;layer++) {
            float inset=layer==0?1f:.78f;
            for(int i=0;i<24;i++) {
                float a=i*6.283185f/24f;
                float lobe=1f+.16f*(float)Math.sin(a*3f+rotation)
                        +.075f*(float)Math.sin(a*5f-rotation*1.3f);
                float sin=(float)Math.sin(a);
                // A wider foot and a narrowing neck make the suspended goo turn into a drip.
                d.shape[i*2]=q.x+(float)Math.cos(a)*r*lobe*inset*(1f+sin*drip*.22f);
                d.shape[i*2+1]=q.y+sin*r*lobe*stretch*inset-(layer==1?r*.14f:0);
            }
            p.fillPoly(d.shape,Glyph.withAlpha(layer==0?0xFF347D29:q.color,(int)((layer==0?70:110)*fade)));
        }
        p.fillEllipse(q.x-r*.27f,q.y-r*.34f*stretch,r*.24f,r*.13f,
                Glyph.withAlpha(0xFFE8FFAE,(int)(125*fade)));
    }
}
