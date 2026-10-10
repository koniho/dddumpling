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
            if(land==0) {slimePool(p,x,y,s,span,rise,now,fade,front);continue;}
            boolean crystal=land==1 || land==Cave.LAND;
            int alpha=(int)(255*fade*(crystal?(front?.98f:.58f):(front?.92f:.8f)));
            if(!front)p.fillEllipse(x,ground,span,s*.15f,
                    fadeBy(Lands.TINT[land],fade*rise*.25f));
            int count=crystal?(front?3:4):9;
            for(int i=0;i<count;i++) {
                float phase=i*7.3f+land+(front?2.4f:0);
                float jitter=(float)Math.sin(phase)*.12f;
                float px=x+span*((i-4)/4.3f+jitter*.25f);
                float size=s*(1.25f+.33f*((i*7)%4));
                float root=ground+s*(front?.25f:.08f)*(float)Math.sin(phase+1.3f);
                if(crystal) {
                    px=x+span*((i+.5f)*2/count-1+jitter*.45f);
                    size=s*(1.20f+.26f*((i*7)%4));
                    root=y+s*(front?.55f+.36f*(float)Math.sin(phase+1.3f)
                            :-.28f+.20f*(float)Math.sin(phase+1.3f));
                    p.fillEllipse(px,root,size*.32f,s*.065f,
                            fadeBy(Lands.TINT[land],fade*rise*(front?.22f:.10f)));
                }
                float height=land==2?1.08f:land==3?1.05f:1.20f;
                float base=root+(1-rise)*size*height;
                p.save();p.clipRect(x-span-s*.3f,y-s*2.75f,x+span+s*.3f,root);
                prop(p,land,px,base,size,alpha,now+i*1.7f,i);
                p.restore();
            }
        }
        for(int i=0;i<8;i++) {
            if(((i&1)==0)!=front)continue;
            float age=initialized?bubbles[i]:land(c)==0?(now+i*BUBBLE_PERIOD/8f)%BUBBLE_PERIOD:-1;
            if(age<0 || age>=BUBBLE_LIFE)continue;
            float t=age/BUBBLE_LIFE,r=s*(.30f+.135f*(i%3))*(.55f+t*.45f)*(front?1.12f:1f);
            float bx=x+span*((i-3.5f)/4.2f)+(float)Math.sin(age*2.3f+i)*s*.17f;
            float by=y+s*.03f-t*s*2.15f;
            float alpha=fade*Math.min(1,age*5)*Math.min(1,(BUBBLE_LIFE-age)*3);
            p.fillCircle(bx,by,r,fadeBy(0xFF87DB91,alpha*(front?.60f:.26f)));
            p.strokeCircle(bx,by,r,fadeBy(front?0xFF58AB76:0xFFAFEAA4,alpha*.85f),s*(front?.04f:.025f));
            p.arc(bx,by,r*.66f,r*.66f,205,80,fadeBy(0xFFF0FFD7,alpha*.9f),s*.035f);
        }
    }
    private static void slimePool(Painter p,float x,float y,float s,float span,float rise,float clock,float fade,boolean front) {
        float cy=y+s*(.03f+(1-rise)*2f),edge=y-s*.10f;
        // Complementary clips keep a single puddle behind and in front of the letters.
        p.save();p.clipRect(x-span*1.06f,front?edge:y-s*.95f,x+span*1.06f,front?y+s*.8f:edge);
        p.fillPoly(poolOutline(x,cy,span*1.04f,s,clock,1),fadeBy(0xFF478865,fade*.88f));
        p.fillPoly(poolOutline(x,cy-s*.07f,span*1.035f,s,clock,.80f),fadeBy(0xFF87C58A,fade*.80f));
        float[] rim=poolOutline(x,cy-s*.05f,span*1.015f,s,clock,.69f);
        p.polyline(java.util.Arrays.copyOfRange(rim,12,34),fadeBy(0xFFD9F4B7,fade*.58f),s*.06f);
        p.polyline(java.util.Arrays.copyOfRange(rim,62,82),fadeBy(0xFFD9F4B7,fade*.38f),s*.045f);
        p.polyline(java.util.Arrays.copyOfRange(rim,120,146),fadeBy(0xFFB9E59B,fade*.32f),s*.045f);
        p.restore();
    }
    private static float[] poolOutline(float x,float y,float span,float s,float clock,float depth) {
        float[] outline=new float[196];
        for(int side=0;side<2;side++)for(int i=0;i<=48;i++) {
            float u=(side==0?i:48-i)/24f-1;
            float taper=(float)Math.sqrt(Math.max(0,1-u*u));
            float lobe=.48f+.13f*(float)Math.sin(u*8.2f+side*2.1f)
                    +.075f*(float)Math.sin(u*17.4f+side*.8f);
            float swell=.025f*(float)Math.sin(clock*1.35f+u*6+side);
            float drift=.085f*(float)Math.sin(u*5+.7f)*taper;
            int k=(side*49+i)*2;
            outline[k]=x+span*u;
            outline[k+1]=y+s*(drift+(side==0?-1:1)*taper*(lobe+swell)*depth);
        }
        return outline;
    }
    private static void prop(Painter p,int land,float x,float base,float size,int a,float t,int variant) {
        if(land==1 || land==Cave.LAND) {
            int glass=(int)(a*.48f);
            Lands.crystal(p,x,base,size*.24f,size*1.15f,glass,t,false,true);
            if(variant==1)Lands.crystal(p,x+size*.29f,base,size*.17f,size*.62f,glass,t+1.8f,false,true);
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
            int shape=variant%4;
            float width=size*(shape==0?.55f:shape==1?.30f:shape==2?.45f:.22f);
            float height=size*(shape==0?.15f:shape==1?.29f:shape==2?.21f:.43f);
            float sway=(float)Math.sin(t*2.2f)*size*.16f;
            float capY=base-size*(shape==3?.58f:.68f),capX=x+sway;
            p.polyline(new float[]{x,base,x+sway*.3f,base-size*.32f,capX,capY},
                    Glyph.withAlpha(0xFFF3D8AE,a),size*.12f);
            int color=Glyph.withAlpha(variant%3==0?0xFFCC839D:variant%3==1?0xFFCBA071:0xFFA78ACD,a);
            Lands.mushroomCap(p,capX,capY,width,height,t,color,Glyph.withAlpha(0xFFFFE2B7,a));
        }
    }
}
