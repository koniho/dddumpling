package com.dddumpling.game;

/** Sparse surface bubbles with a clipped emergence, followed by either an early pop or lift-off. */
final class SlimeBubbles extends Draw {
    static final int LIMIT=3;
    static final float POP_TIME=.55f;
    static final class Bubble {
        boolean live,early;
        int seed;
        float age,x,surfaceY,groundY,r,riseTime,popAt;
        float dropTime(int drop) {return .70f+drop*.18f+hash(seed+41)*.10f;}
        float landingX(int drop) {return centreX()+r*(drop==0?-.42f:.46f);}
        float landingY(int drop) {return groundY+r*(drop==0?-.05f:.10f);}
        float emergence() {return Math.min(1,Math.min(age,popAt)/riseTime);}
        float freeTime() {return Math.max(0,Math.min(age,popAt)-riseTime);}
        float centreY() {
            float t=emergence(),f=freeTime();
            return surfaceY+r-r*2*t*t*(3-2*t)-r*(f*f*.45f+f*f*f*.06f);
        }
        float centreX() {
            float f=freeTime();
            return x+r*.23f*((float)Math.sin(seed+f*f/(1+f)*1.8f)-(float)Math.sin(seed));
        }
    }
    final Bubble[] bubbles=new Bubble[LIMIT];
    private final float[] shell=new float[96],gleam=new float[52],dropShape=new float[66];
    private final float[][] rim={new float[96],new float[96]};
    float untilNext=.6f;
    int serial;
    SlimeBubbles() {for(int i=0;i<LIMIT;i++)bubbles[i]=new Bubble();}
    static boolean enabled(GameCore c) {
        return c.state==GameCore.PLAY && !c.survival.active && !c.boss.active() && Lands.forStage(c.stage)==0;
    }
    void reset() {for(Bubble b:bubbles)b.live=false;untilNext=.6f;serial=0;}

    static boolean earlyPop(int serial) {
        // Shuffle each pair's order while retaining an even split of the two outcomes.
        return ((serial&1)==0)==(hash(1499+serial/2*61)>.5f);
    }
    void spawn(Bubble b,int id,Layout L) {
        int seed=1391+id*83;
        b.live=true;b.age=0;b.seed=seed;b.early=earlyPop(id);
        b.r=L.enemyR*(1.43f+hash(seed+1)*.14f);
        b.x=L.w*(.30f+hash(seed+3)*.40f);
        b.surfaceY=L.playTop+(L.deckTop-L.playTop)*(.20f+hash(seed+7)*.56f);
        b.groundY=b.surfaceY;
        b.riseTime=2.3f+hash(seed+11)*.8f;
        b.popAt=b.early?b.riseTime*(.43f+hash(seed+13)*.30f):b.riseTime+1.6f+hash(seed+17)*1.1f;
    }
    void update(GameCore c,Layout L,float dt,float skyDelta) {
        if(!enabled(c)){reset();return;}
        for(Bubble b:bubbles) {
            if(!b.live)continue;
            float previousAge=b.age;
            b.groundY+=skyDelta*L.h*SlimeHills.SCROLL_RATE;
            float attached=Math.max(0,Math.min(dt,Math.min(b.riseTime,b.popAt)-b.age));
            // Stop carrying the bubble with the water exactly when it lifts free (or pops).
            if(dt>0)b.surfaceY+=skyDelta*L.h*SlimeHills.SCROLL_RATE*(attached/dt);
            boolean popping=b.early && b.age<b.popAt && b.age+dt>=b.popAt;
            b.age+=dt;
            if(popping)SlimeHills.cleared(c,b.x,b.surfaceY,SlimeHills.MINT,L);
            if(!b.early)for(int drop=0;drop<2;drop++) {
                float impact=b.popAt+b.dropTime(drop);
                if(previousAge<impact && b.age>=impact)
                    SlimeHills.surfaceRipple(c,b.landingX(drop),b.landingY(drop),SlimeHills.MINT,b.r*.30f);
            }
            float aftermath=b.early?POP_TIME:b.dropTime(1)+.15f;
            if(b.age>=b.popAt+aftermath)b.live=false;
        }
        untilNext-=dt;
        if(untilNext<=0)for(Bubble b:bubbles)if(!b.live) {
            spawn(b,serial++,L);
            untilNext=1.8f+hash(1793+serial*97)*1.7f;
            break;
        }
    }

    void draw(Painter p,GameCore c,Layout L,float fade) {
        if(!enabled(c))return;
        for(Bubble b:bubbles) {
            if(!b.live)continue;
            float x=b.centreX(),y=b.centreY(),r=b.r;
            if(b.age>=b.riseTime)shadow(p,b,x,fade);
            if(b.age>=b.popAt && !b.early) {
                fallingSlime(p,b,x,y,fade);
                continue;
            }
            if(b.age>=b.popAt) {
                float t=(b.age-b.popAt)/POP_TIME,a=fade*(1-t)*(1-t);
                // Short curved fragments and a handful of beads; no flash or target-like halo.
                for(int i=0;i<5;i++) {
                    float angle=i*Softbody.TAU/5+b.seed,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                    float visible=b.early?Math.min(0,dy):dy;
                    float py=b.early?Math.min(b.surfaceY,y+visible*r):y+dy*r;
                    float reach=r*(.20f+t*.85f);
                    p.fillEllipse(x+dx*(r+reach),py+dy*reach+r*t*t*.7f,r*.065f*(1-t),r*.09f*(1-t),
                            Glyph.withAlpha(SlimeHills.MINT,(int)(a*100)));
                    if(i<3)p.arc(x,y,r*(1+t*.26f),r*(1+t*.26f),195+i*48,24*(1-t),
                            Glyph.withAlpha(SlimeHills.MINT,(int)(a*65)),r*.035f);
                }
                continue;
            }
            float show=Math.min(1,b.age/.35f)*fade;
            p.save();
            // The waterline clips the soft shell as it emerges, keeping the same material after lift-off.
            if(b.age<b.riseTime)p.clipRect(0,L.playTop,L.w,b.surfaceY);
            bubbleShell(p,b,x,y,show);
            p.restore();
            if(b.age<b.riseTime) {
                float t=b.emergence(),chord=r*(float)Math.sqrt(Math.max(0,1-(1-2*t*t*(3-2*t))*(1-2*t*t*(3-2*t))));
                p.arc(x,b.surfaceY,chord,r*.11f,0,180,
                        Glyph.withAlpha(SlimeHills.MINT,(int)(show*40)),r*.023f);
            }
        }
    }
    private void shadow(Painter p,Bubble b,float x,float fade) {
        float lift=Math.min(1,b.freeTime()*3);
        float height=Math.max(0,(b.groundY-b.centreY())/b.r-1);
        float a=fade*lift/(1+height*.24f);
        if(b.age>=b.popAt)a*=Math.max(0,1-(b.age-b.popAt)/.25f);
        if(a<=.001f)return;
        float r=b.r*.50f/(1+height*.10f);
        for(int layer=0;layer<3;layer++) {
            float scale=1-layer*.20f;
            p.fillEllipse(x,b.groundY,r*scale,r*.27f*scale,
                    Glyph.withAlpha(0xFF102F23,(int)(a*(layer==0?10:16))));
        }
    }

    private void fallingSlime(Painter p,Bubble b,float x,float y,float fade) {
        float age=b.age-b.popAt;
        if(age<.30f)for(int i=0;i<3;i++) {
            float t=age/.30f;
            p.arc(x,y,b.r*(1+t*.22f),b.r*(1+t*.22f),195+i*48,24*(1-t),
                    Glyph.withAlpha(SlimeHills.MINT,(int)(fade*(1-t)*55)),b.r*.035f);
        }
        for(int drop=0;drop<2;drop++) {
            float t=age/b.dropTime(drop),tx=b.landingX(drop),ty=b.landingY(drop);
            if(t>=1) {
                float settle=Math.max(0,1-(age-b.dropTime(drop))/.15f);
                if(settle>0)p.fillEllipse(tx,ty,b.r*.13f,b.r*.035f,
                        Glyph.withAlpha(SlimeHills.MINT,(int)(fade*settle*65)));
                continue;
            }
            float startX=x+b.r*(drop==0?-.12f:.12f);
            float dx=startX+(tx-startX)*t;
            float dy=y+(ty-y)*t*t-b.r*.15f*(float)Math.sin(t*Math.PI);
            float r=b.r*(.18f-.08f*t),stretch=1+.85f*(float)Math.sin(t*Math.PI);
            SlimeHills.jelly(p,dropShape,dx,dy,r,r*stretch,b.seed+drop+age*5,
                    Glyph.withAlpha(SlimeHills.GREEN,(int)(fade*130)));
            SlimeHills.jelly(p,dropShape,dx-r*.12f,dy-r*.15f,r*.72f,r*stretch*.78f,b.seed+drop+age*5,
                    Glyph.withAlpha(SlimeHills.MINT,(int)(fade*60)));
            p.fillEllipse(dx-r*.24f,dy-r*.40f,r*.19f,r*.30f,
                    Glyph.withAlpha(INK,(int)(fade*85)));
        }
    }

    /** Soft vector layers avoid native blur passes on either mobile renderer. */
    private void bubbleShell(Painter p,Bubble b,float x,float y,float show) {
        float lift=Math.min(1,b.age*2.5f);
        float pulse=.5f+.5f*(float)Math.sin(b.age*3.8f+b.seed);
        int sheen=Glyph.mix(SlimeHills.MINT,INK,.25f+pulse*.35f);
        for(int layer=0;layer<3;layer++) {
            float scale=1.10f-layer*.05f;
            for(int j=0;j<48;j++)point(shell,j,b,x,y,j*Softbody.TAU/48,scale,lift);
            p.fillPoly(shell,Glyph.withAlpha(SlimeHills.MINT,(int)(show*(layer==0?4:layer==1?6:13))));
        }
        // Wide faint rim with a softer inner edge; no sharp circle outline.
        for(int layer=0;layer<2;layer++) {
            for(int j=0;j<48;j++) {
                float angle=j*Softbody.TAU/48;
                point(rim[0],j,b,x,y,angle,layer==0?1.035f:1f,lift);
                point(rim[1],j,b,x,y,angle,layer==0?.935f:.965f,lift);
            }
            p.fillContours(rim,Glyph.withAlpha(sheen,(int)(show*(layer==0?13:30))));
        }
        for(int band=0;band<2;band++) {
            float start=(band==0?3.55f:.25f)+.65f*(float)Math.sin(b.age*1.7f+b.seed+band*2);
            for(int j=0;j<=12;j++) {
                float u=j/12f,angle=start+u*(band==0?1.05f:.72f);
                float thickness=(.025f+.012f*pulse)*(float)Math.sin(u*Math.PI);
                point(gleam,j,b,x,y,angle,.88f+thickness,lift);
                point(gleam,25-j,b,x,y,angle,.88f-thickness,lift);
            }
            int color=band==0?sheen:Glyph.mix(SlimeHills.GREEN,0xFFE6E4B2,pulse*.45f);
            p.fillPoly(gleam,Glyph.withAlpha(color,(int)(show*(band==0?64+30*pulse:32+26*pulse))));
        }
    }

    private static void point(float[] shape,int index,Bubble b,float x,float y,float angle,float scale,float lift) {
        float wobble=1+lift*(.065f*(float)Math.sin(angle*2+b.age*4.3f+b.seed)
                +.025f*(float)Math.sin(angle*3-b.age*3.1f+b.seed));
        float squash=.045f*lift*(float)Math.sin(b.age*5.4f+b.seed);
        shape[index*2]=x+(float)Math.cos(angle)*b.r*scale*wobble*(1+squash);
        shape[index*2+1]=y+(float)Math.sin(angle)*b.r*scale*wobble*(1-squash);
    }

}
