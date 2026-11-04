package com.dddumpling.game;

/** Tumbling faceted shells and a bounded particle wake, with matching rotating prompts. */
final class SpaceEnemyArt extends Draw {
    static float spin(float clock,float speed) {return clock*(.65f+speed*.5f);}
    static Painter promptPainter(Painter p,float x,float y,float clock,float speed) {
        float angle=spin(clock,speed),cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
        // TurnPainter takes local coordinates; offset its origin to pivot world geometry here.
        return new LinkedPairArt.TurnPainter(p,x-x*cos+y*sin,y-x*sin-y*cos,angle);
    }
    static void comet(Painter p,float x,float y,float r,int color,float clock,float speed,int fade,float dx,float dy) {
        float norm=(float)Math.sqrt(dx*dx+dy*dy);
        if(norm<.001f) {dx=0;dy=1;norm=1;}
        dx/=norm;dy/=norm;
        int light=Glyph.mix(color,0xFFFFFFDD,.65f),stone=Glyph.mix(color,0xFF34314C,.55f);
        float length=r*(3+speed*4),spin=spin(clock,speed);
        // Recycled dust glows and drifts across the wake; no solid flame/cone underneath.
        for(int n=0;n<16;n++) {
            float t=(clock*(.85f+speed*.4f)+n*.618034f)%1f;
            float scatter=(float)Math.sin(n*12.4f+clock*1.8f)*r*(.12f+t*.65f);
            float distance=r*.7f+length*t;
            float px=x-dx*distance-dy*scatter,py=y-dy*distance+dx*scatter;
            float size=r*(.06f+hash(n*29+3)*.09f)*(1-t*.55f);
            int a=(int)(fade*(1-t)*(1-t)*.85f);
            p.fillCircle(px,py,size*3.3f,Glyph.withAlpha(color,a/7));
            p.fillEllipse(px,py,size,size*.75f,Glyph.withAlpha(light,a));
        }
        float[] rock=new float[20],inner=new float[20],shadow=new float[20];
        for(int n=0;n<10;n++) {
            float angle=n*Softbody.TAU/10+spin;
            float rr=r*(1.16f+hash(n*43+11)*.22f);
            float xx=(float)Math.cos(angle)*rr,yy=(float)Math.sin(angle)*rr;
            rock[n*2]=x+xx;rock[n*2+1]=y+yy;
            inner[n*2]=x+xx*.81f-r*.04f;inner[n*2+1]=y+yy*.81f-r*.07f;
            shadow[n*2]=x+xx+r*.11f;shadow[n*2+1]=y+yy+r*.16f;
        }
        p.fillPoly(shadow,Glyph.withAlpha(0xFF090B20,fade*110/255));
        p.fillPoly(rock,Glyph.withAlpha(stone,fade*235/255));
        for(int n=0;n<10;n++) {
            int j=(n+1)%10;
            float angle=(n+.5f)*Softbody.TAU/10+spin;
            float lit=.5f-.5f*(float)Math.cos(angle-.75f);
            int facet=Glyph.mix(Glyph.mix(stone,0xFF151329,.55f),light,lit*.78f);
            p.fillPoly(new float[]{rock[n*2],rock[n*2+1],rock[j*2],rock[j*2+1],
                    inner[j*2],inner[j*2+1],inner[n*2],inner[n*2+1]},Glyph.withAlpha(facet,fade*220/255));
        }
        p.fillPoly(inner,Glyph.withAlpha(Glyph.mix(stone,color,.25f),fade*165/255));
        p.strokePoly(rock,Glyph.withAlpha(light,fade*115/255),r*.055f);
        for(int n=0;n<3;n++) {
            float angle=spin+n*2.1f,rr=r*(n==1?1.05f:1.13f);
            float cx=x+(float)Math.cos(angle)*rr,cy=y+(float)Math.sin(angle)*rr;
            p.fillCircle(cx,cy,r*.10f,Glyph.withAlpha(0xFF222039,fade*150/255));
            p.fillCircle(cx+r*.025f,cy+r*.03f,r*.065f,Glyph.withAlpha(light,fade*95/255));
        }
    }
}
