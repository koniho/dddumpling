package com.dddumpling.game;

/** Scenery rooted at the lettering baseline; floating slime outlives a land change. */
final class AdventureTitle extends Draw {
    static final float CHANGE=.65f, BUBBLE_LIFE=2.6f, BUBBLE_PERIOD=3.1f;
    final float[] growth=new float[Lands.COUNT];
    final float[] bubbles=new float[8];
    float clock=4f, emission;
    int nextBubble;
    boolean initialized;

    AdventureTitle() {java.util.Arrays.fill(bubbles,-1);}
    void update(GameCore c,float dt) {
        if(c.state!=GameCore.TITLE) {reset();return;}
        int land=land(c);
        if(!initialized) {
            initialized=true;growth[land]=1;
            if(land==0)for(int i=0;i<bubbles.length;i++)
                bubbles[i]=(7-i)*BUBBLE_PERIOD/8f<BUBBLE_LIFE?(7-i)*BUBBLE_PERIOD/8f:-1;
        }
        clock+=dt;
        for(int i=0;i<growth.length;i++)
            growth[i]+=Math.max(-dt/CHANGE,Math.min(dt/CHANGE,(i==land?1:0)-growth[i]));
        for(int i=0;i<bubbles.length;i++)if(bubbles[i]>=0) {
            bubbles[i]+=dt;if(bubbles[i]>=BUBBLE_LIFE)bubbles[i]=-1;
        }
        if(land==0) {
            emission+=Math.min(dt,BUBBLE_LIFE);
            while(emission>=BUBBLE_PERIOD/8f) {
                emission-=BUBBLE_PERIOD/8f;
                bubbles[nextBubble]=emission;nextBubble=(nextBubble+1)%bubbles.length;
            }
        } else emission=0;
    }
    private void reset() {
        if(!initialized)return;
        java.util.Arrays.fill(growth,0);java.util.Arrays.fill(bubbles,-1);
        initialized=false;clock=4;emission=0;nextBubble=0;
    }
    private static int land(GameCore c) {return c.landChoice>=0 && c.landChoice<Lands.COUNT?c.landChoice:0;}
    void draw(Painter p,GameCore c,Layout L,float x,float y,float scale,float fade,boolean front) {
        float s=L.unit*scale,span=Math.min(L.w*.36f,type(s*1.74f)*3.22f);
        float now=initialized?clock:c.clock+4;
        for(int land=0;land<Lands.COUNT;land++) {
            float amount=initialized?growth[land]:land==land(c)?1:0;
            if(amount<=0)continue;
            float rise=amount*amount*(3-2*amount),ground=y+s*(front?.12f:.025f);
            int alpha=(int)(255*fade*(front?.92f:.8f));
            if(!front)p.fillEllipse(x,ground,span,s*.15f,
                    fadeBy(Lands.TINT[land],fade*rise*.25f));
            p.save();p.clipRect(x-span-s*.3f,y-s*2.65f,x+span+s*.3f,ground);
            for(int i=0;i<9;i++) {
                float jitter=(float)Math.sin(i*7.3f+land)*.12f;
                float px=x+span*((i-4)/4.3f+jitter*.25f);
                float size=s*(front?.35f+.16f*(i%3):1.25f+.33f*((i*7)%4));
                // Front plants only brush the feet of the letters.
                float height=land==0?.36f:land==2?1.08f:land==3?1.05f:1.20f;
                float base=ground+(1-rise)*size*height;
                prop(p,land,px,base,size,alpha,now+i*1.7f,front,i);
            }
            p.restore();
        }
        for(int i=0;i<8;i++) {
            if((i%3==0)!=front)continue;
            float age=initialized?bubbles[i]:land(c)==0?(now+i*BUBBLE_PERIOD/8f)%BUBBLE_PERIOD:-1;
            if(age<0 || age>=BUBBLE_LIFE)continue;
            float t=age/BUBBLE_LIFE,r=s*(.12f+.055f*(i%3))*(.55f+t*.45f);
            float bx=x+span*((i-3.5f)/4.2f)+(float)Math.sin(age*2.3f+i)*s*.17f;
            float by=y+s*.03f-t*s*2.35f;
            float alpha=fade*Math.min(1,age*5)*Math.min(1,(BUBBLE_LIFE-age)*3);
            p.fillCircle(bx,by,r,fadeBy(0xFF87DB91,alpha*.32f));
            p.strokeCircle(bx,by,r,fadeBy(0xFFAFEAA4,alpha*.8f),s*.025f);
            p.arc(bx,by,r*.66f,r*.66f,205,80,fadeBy(0xFFF0FFD7,alpha*.9f),s*.035f);
        }
    }
    private static void prop(Painter p,int land,float x,float base,float size,int a,float t,boolean front,int variant) {
        if(land==0) {
            float wobble=1+.09f*(float)Math.sin(t*2);
            p.fillEllipse(x,base,size*.67f*wobble,size*.31f/wobble,Glyph.withAlpha(0xFF76B984,a));
            p.arc(x,base,size*.50f*wobble,size*.23f/wobble,195,110,
                    Glyph.withAlpha(0xFFD2F7AE,a/2),size*.07f);
        } else if(land==1 || land==Cave.LAND) {
            Lands.crystal(p,x,base,size*.24f,size*(front?.82f:1.15f),a,t,false,true);
            if(!front)Lands.crystal(p,x+size*.29f,base,size*.17f,size*.62f,a,t+1.8f,false,true);
        } else if(land==2) {
            for(int blade=0;blade<5;blade++) {
                float h=size*(.50f+.13f*((blade*3)%5)),root=x+(blade-2)*size*.13f;
                float[] ribbon=new float[28];
                for(int n=0;n<7;n++) {
                    float u=n/6f,bend=(float)Math.sin(t*1.6f+u*2.2f+blade*.65f)*size*.20f*u;
                    float cx=root+bend+(blade-2)*size*.09f*u,cy=base-h*u,w=size*.047f*(1-u*.88f);
                    ribbon[n*2]=cx-w;ribbon[n*2+1]=cy;
                    ribbon[(13-n)*2]=cx+w;ribbon[(13-n)*2+1]=cy;
                }
                p.fillPoly(ribbon,Glyph.withAlpha(blade%2==0?0xFF70C6A5:0xFF91D8B5,a));
            }
        } else {
            float sway=(float)Math.sin(t*2.2f)*size*.16f;
            float capY=base-size*.72f,capX=x+sway;
            p.polyline(new float[]{x,base,x+sway*.3f,base-size*.36f,capX,capY},
                    Glyph.withAlpha(0xFFF3D8AE,a),size*.15f);
            p.fillEllipse(capX,capY,size*.48f,size*.27f,Glyph.withAlpha(variant%3==0?0xFFCC839D:variant%3==1?0xFFCBA071:0xFFA78ACD,a));
            p.fillEllipse(capX,capY+size*.06f,size*.45f,size*.09f,Glyph.withAlpha(0xFF87556E,a));
            for(int k=-1;k<=1;k++)p.fillCircle(capX+k*size*.24f,capY-size*(k==0?.13f:.06f),
                    size*(k==0?.064f:.048f),Glyph.withAlpha(0xFFFFE2B7,a));
        }
    }
}
