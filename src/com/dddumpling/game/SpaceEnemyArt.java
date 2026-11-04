package com.dddumpling.game;

/** Asteroid facets sit behind readable prompts; comet tails point away from the flight. */
final class SpaceEnemyArt extends Draw {
    static void comet(Painter p,float x,float y,float r,int color,float clock,float speed,int fade) {
        float length=r*(2+speed*4),w=r*.85f;
        int light=Glyph.mix(color,0xFFFFFFDD,.55f);
        for(int k=0;k<3;k++) {
            float inset=1-k*.23f;
            p.fillPoly(new float[]{x-w*inset,y,x-w*.36f*inset,y-length*.48f,
                    x+r*.18f*(float)Math.sin(clock*4),y-length,x+w*.42f*inset,y-length*.45f,x+w*inset,y},
                    Glyph.withAlpha(k==2?Glyph.mix(color,0xFFFFFFCC,.55f):color,fade*(25+k*24)/255));
        }
        // Small ember fragments break up the taper without hiding adjacent words.
        for(int n=0;n<5;n++) {
            float t=(clock*1.4f+n*.618034f)%1f;
            float px=x+(float)Math.sin(n*12.4f)*r*(.3f+t*.5f),py=y-r-length*t;
            float size=r*(.045f+.055f*(1-t));int a=(int)(fade*(1-t)*.65f);
            p.line(px,py+size*3,px,py-size*3,Glyph.withAlpha(color,a/4),size*4);
            p.fillCircle(px,py,size,Glyph.withAlpha(light,a));
        }
        float[] rock=new float[20];
        for(int n=0;n<10;n++) {
            float a=n*Softbody.TAU/10,rr=r*(1.1f+hash(n*43+11)*.23f);
            rock[n*2]=x+(float)Math.cos(a)*rr;rock[n*2+1]=y+(float)Math.sin(a)*rr;
        }
        p.fillPoly(rock,Glyph.withAlpha(Glyph.mix(color,0xFF24253F,.65f),fade*210/255));
        p.strokePoly(rock,Glyph.withAlpha(Glyph.mix(color,0xFFE6DBFF,.3f),fade*130/255),r*.07f);
        p.fillPoly(new float[]{rock[10],rock[11],rock[12],rock[13],rock[14],rock[15],x-r*.18f,y-r*.62f},
                Glyph.withAlpha(light,fade*65/255));
        p.fillPoly(new float[]{rock[0],rock[1],rock[2],rock[3],rock[4],rock[5],x+r*.58f,y+r*.20f},
                Glyph.withAlpha(0xFF15162B,fade*100/255));
        p.line(x-r*.8f,y-r*.65f,x-r*.2f,y-r*.95f,Glyph.withAlpha(color,fade*170/255),r*.08f);
    }
}
