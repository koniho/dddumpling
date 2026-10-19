package com.dddumpling.game;

/** A distant weaving approach, then a full-size rainbow battle cry. */
final class RainbowCatArrival extends Draw {
    static final float FLIGHT=3f, BURST=2.3f, DURATION=FLIGHT+BURST;
    private static float clamp(float v,float min,float max) {return Math.max(min,Math.min(max,v));}
    static final int STARS=36, TRAIL_STEPS=24;
    static float elapsed(Boss b) {return Math.max(0,DURATION-b.intro);}
    static float approach(Boss b) {return clamp(elapsed(b)/FLIGHT,0,1);}
    private static float ease(float u) {return u*u*(3-2*u);}
    static float scale(float u) {return .09f+.91f*ease(clamp(u,0,1));}
    static float x(Layout L,float u) {
        float t=ease(clamp(u,0,1));
        return L.w*.5f+(float)Math.sin(t*Math.PI*4)*L.w*.30f*(.22f+.78f*t);
    }
    static float groundY(Layout L) {return L.playTop+(L.dangerY-L.playTop)*.47f;}
    static float y(Layout L,float u) {
        float t=ease(clamp(u,0,1)),far=L.playTop+(L.dangerY-L.playTop)*.12f;
        return far+(groundY(L)-far)*t;
    }
    static float starFade(Boss b) {return b.intro>0?ease(approach(b)):1;}
    static void pose(RainbowCat a,Boss b,Layout L) {
        if(b.intro<=0)return;
        float u=approach(b);
        a.x=L.w*.5f;a.y=groundY(L);
        // Spring geometry stays full-size; only its fitted drawing moves into the foreground.
        a.vx=u>=1?-2:x(L,Math.min(1,u+.005f))-x(L,Math.max(0,u-.005f));a.vy=0;
    }
    static void feedback(GameCore c,float before) {
        Boss b=c.boss;
        if(before<FLIGHT/DURATION && b.introProgress()>=FLIGHT/DURATION) {
            b.cat.meow(c,CatMeowRecording.STRUGGLE,true);
            c.shake=Math.max(c.shake,.5f);c.flash=Math.max(c.flash,.22f);c.flashColor=0xFFFFD5F3;
        }
    }
    static void stars(Painter p,RainbowCat a,Boss b,Layout L) {
        stars(p,a,b,L,a.clock);
    }
    static void stars(Painter p,RainbowCat a,Boss b,Layout L,float clock) {
        float fade=starFade(b);if(fade<=0)return;
        // Draw distant stars first. Near stars move twice as fast; both trail the battle bands.
        for(int layer=0;layer<2;layer++)for(int i=layer;i<STARS;i+=2) {
            float depth=layer==0?.30f+.18f*hash(i*71+19):.70f+.30f*hash(i*71+19);
            float xx=hash(i*977+31)*L.w,brightness=layer==0?.58f:1;
            float span=L.dangerY-L.playTop;
            float travel=hash(i*313+13)+clock*L.h*RainbowLand.BAND_SCROLL_RATE*(layer==0?.5f:1)/span;
            float yy=L.playTop+(travel-(float)Math.floor(travel))*span;
            float pulse=.65f+.35f*(float)Math.sin(clock*(1.2f+depth)+i*2.1f);
            float r=L.w*(.0035f+.0065f*depth)*(.8f+.2f*pulse);
            int color=RainbowLand.CLEAR_COLORS[i%RainbowLand.CLEAR_COLORS.length];
            p.fillCircle(xx,yy,r*2.7f,Glyph.withAlpha(color,(int)(25*fade*pulse*brightness)));
            float[] shape=a.arrivalStar;
            for(int point=0;point<8;point++) {
                float angle=(float)(point*Math.PI/4),size=r*(point%2==0?1:.24f);
                shape[point*2]=xx+(float)Math.cos(angle)*size;
                shape[point*2+1]=yy+(float)Math.sin(angle)*size*1.4f;
            }
            p.fillPoly(shape,Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.68f),(int)(210*fade*pulse*brightness)));
        }
    }
    static void draw(Painter p,RainbowCat a,Boss b,Layout L) {
        float time=elapsed(b),u=approach(b),burst=time-FLIGHT;
        trail(p,a,L,u,burst);
        if(burst>=0)rays(p,a,L,burst);
        float bob=burst>=0?(float)Math.sin(burst*11)*a.r*.06f*(1-burst/BURST):0;
        Painter fitted=new BossScreen.PortraitFit(p,a.x,a.y,x(L,u),y(L,u)+bob,scale(u));
        float cry=burst>=0?clamp(burst/.08f,0,1)*clamp((CatMeowRecording.duration(CatMeowRecording.STRUGGLE)-burst)/.18f,0,1):0;
        RainbowCatArt.arrivalCat(fitted,a,cry);
    }
    private static void trail(Painter p,RainbowCat a,Layout L,float u,float burst) {
        float fade=burst<0?1:clamp(1-burst/.6f,0,1);if(fade==0 || u<=0)return;
        for(int band=0;band<6;band++) {
            float[] ribbon=a.arrivalRibbon;
            for(int point=0;point<=TRAIL_STEPS;point++) {
                float t=point/(float)TRAIL_STEPS,at=Math.max(0,u-(1-t)*.24f);
                float width=a.r*.22f*scale(at)*t,xx=x(L,at),yy=y(L,at);
                int front=point*2,back=(TRAIL_STEPS*2+1-point)*2;
                ribbon[front]=ribbon[back]=xx;
                ribbon[front+1]=yy+(band-3)*width;ribbon[back+1]=yy+(band-2)*width;
            }
            p.fillPoly(ribbon,Glyph.withAlpha(RainbowCatArt.trailColor(p,band,RainbowLand.CLEAR_COLORS[band]),(int)(150*fade)));
        }
    }
    private static void rays(Painter p,RainbowCat a,Layout L,float age) {
        float progress=clamp(age/BURST,0,1),grow=1-(float)Math.pow(1-clamp(age/.22f,0,1),3);
        float fade=clamp((1-progress)*2,0,1),cx=L.w*.5f,cy=groundY(L);
        float far=Math.max(L.w,L.h)*.85f*grow;
        float[] ray=a.arrivalRay;
        for(int i=0;i<24;i++) {
            float angle=Softbody.TAU*i/24+age*.12f,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float reach=far*(.76f+.24f*hash(i*991+4)),width=reach*(.035f+.02f*hash(i*41+7));
            ray[0]=cx+dx*a.r*.25f;ray[1]=cy+dy*a.r*.25f;
            ray[2]=cx+dx*reach-dy*width;ray[3]=cy+dy*reach+dx*width;
            ray[4]=cx+dx*(reach+a.r);ray[5]=cy+dy*(reach+a.r);
            ray[6]=cx+dx*reach+dy*width;ray[7]=cy+dy*reach-dx*width;
            int color=RainbowLand.CLEAR_COLORS[i%6];
            p.fillPoly(ray,Glyph.withAlpha(color,(int)(145*fade)));
            p.line(cx+dx*a.r,cy+dy*a.r,cx+dx*reach,cy+dy*reach,
                    Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.65f),(int)(135*fade)),a.r*.035f);
        }
        for(int ring=0;ring<3;ring++) {
            float t=(age-ring*.10f)/.8f;if(t<0 || t>=1)continue;
            p.strokeCircle(cx,cy,a.r*(1+t*6),Glyph.withAlpha(RainbowLand.CLEAR_COLORS[ring*2],(int)(180*(1-t))),a.r*.13f);
        }
    }
}
