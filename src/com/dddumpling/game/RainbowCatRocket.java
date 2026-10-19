package com.dddumpling.game;

/** A rounded carrier becomes a rocket; victory waits until its rainbow exhaust has cleared. */
final class RainbowCatRocket extends Draw {
    static final float FETCH=1f,PULL=2.5f,DROP=3.6f,CLOSE=4.15f,LAUNCH=4.8f,DEPART=6.2f,DURATION=7.1f;
    boolean active;
    float age;
    int feedback;
    final float[] shell=new float[82],ray=new float[8],trail=new float[100];
    void reset() {active=false;age=0;feedback=0;}
    void begin(RainbowCat a,Boss b) {
        reset();active=true;b.clearBolts();b.defeatChime=false;
        a.phase=RainbowCat.CATCH;a.netAge=RainbowCat.NET_TIME;
        a.promptKey=a.leftKey=a.rightKey=-1;a.timer=0;a.cue=0;
        a.escapeLeft=0;a.escapeSequence=a.escaping=a.doubleFlurry=false;a.flurryCount=0;
        a.vx=a.vy=0;a.tapFlash=a.keyFlash=a.scared=a.shapeBurst=0;
        a.catchX=a.x;a.catchY=a.y;
    }
    void update(float dt) {age=Math.min(DURATION,age+dt);}
    void feedback(GameCore c) {
        if(!active)return;
        if(age>=DROP && feedback<1) {
            feedback=1;if(c.sound!=null)c.sound.squish(Kawaii.CAT,2);
        }
        if(age>=CLOSE && feedback<2) {
            feedback=2;if(c.sound!=null)c.sound.uiBloop();c.bossDeathHaptic=1;
        }
        if(age>=LAUNCH && feedback<3) {
            feedback=3;if(c.sound!=null)c.sound.courseFinish();
            c.shake=Math.max(c.shake,.9f);c.flash=Math.max(c.flash,.45f);
            c.flashColor=0xFFF0DFFF;c.bossDeathHaptic=2;
        }
    }
    static float unit(float v) {return Math.max(0,Math.min(1,v));}
    static float ease(float v) {v=unit(v);return v*v*(3-2*v);}
    static float mix(float a,float b,float u) {return a+(b-a)*u;}
    float radius(Layout L) {return L.w*.23f;}
    float ground(Layout L) {return L.playTop+(L.dangerY-L.playTop)*.62f;}
    float rocketX(Layout L) {return mix(-L.w*.31f,L.w*.50f,ease((age-FETCH)/(PULL-FETCH)));}
    float rocketY(Layout L) {
        float fly=unit((age-LAUNCH)/(DEPART-LAUNCH));
        return ground(L)-(ground(L)+radius(L)*2)*fly*fly;
    }
    float companionX(RainbowCat a,Layout L) {
        float start=a.catchX+a.r*2.45f;
        if(age<FETCH)return mix(start,-L.w*.03f,ease(age/FETCH));
        float x=rocketX(L)+L.w*.30f;
        return mix(x,RunCompanion.x(L),ease((age-CLOSE)/1.1f));
    }
    float companionY(RainbowCat a,Layout L) {
        float walk=ground(L)+radius(L)*.9f;
        if(age<FETCH)return mix(a.catchY+a.r*2.15f,walk,ease(age/FETCH));
        return mix(walk,RunCompanion.y(L),ease((age-CLOSE)/1.1f))
                -(age<PULL?Math.abs((float)Math.sin(age*17))*L.w*.016f:0);
    }
    float catX(RainbowCat a,Layout L) {
        float held=companionX(a,L)-L.w*.11f;
        if(age<FETCH)return mix(a.x,held,ease(age/FETCH));
        return mix(held,rocketX(L)+radius(L)*.12f,ease((age-PULL)/(DROP-PULL)));
    }
    float catY(RainbowCat a,Layout L) {
        float held=ground(L)-radius(L)*1.45f;
        if(age<FETCH)return mix(a.y,held,ease(age/FETCH));
        float drop=unit((age-PULL)/(DROP-PULL));
        return mix(held,rocketY(L)+radius(L)*.02f,drop*drop)-radius(L)*.4f*(float)Math.sin(drop*Math.PI);
    }
    void draw(Painter p,GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;
        RainbowCatArrival.stars(new OpacityPainter(p,1-ease((age-DEPART)/(DURATION-DEPART))),a,c.boss,L);
        float r=radius(L),x=rocketX(L),y=rocketY(L),cx=companionX(a,L),cy=companionY(a,L);
        exhaust(p,L,x,y,r);
        pod(p,x,y,r,false);
        float catScale=mix(1,.62f,ease(age/FETCH));
        float nx=catX(a,L),ny=catY(a,L);
        Painter fitted=new BossScreen.PortraitFit(p,a.x,a.y,nx,ny,catScale);
        float netFade=1-ease((age-DROP)/.35f);
        if(netFade>0) {
            Painter mesh=new OpacityPainter(fitted,netFade);
            RainbowCatArt.net(mesh,c,L,false,false);
            // The pole stays in the companion's hand while the loaded pouch is tipped into the carrier.
            float gx=nx+a.r*1.9f*catScale,gy=ny-a.r*1.25f*catScale;
            p.line(cx,cy,gx,gy,0xFF485777,L.w*.018f);
            p.line(cx-L.w*.003f,cy,gx-L.w*.003f,gy,0xFFC4F9EC,L.w*.008f);
        }
        RainbowCatArt.cat(fitted,a.x,a.y,a.r,a.clock,a.turn,a.pastry,a.head,true,1);
        if(netFade>0)RainbowCatArt.net(new OpacityPainter(fitted,netFade),c,L,true,false);
        pod(p,x,y,r,true);
        if(age>=FETCH && age<PULL+.1f) {
            // A separate tow strap leaves the other hand free for the net.
            p.line(x+r*.7f,y+r*.68f,cx,cy,0xFF376B6D,L.w*.013f);
            p.line(x+r*.7f,y+r*.68f,cx,cy-L.w*.003f,0xFFAAFFE0,L.w*.005f);
        }
    }
    void drawCompanion(Painter p,GameCore c,Layout L) {
        if(!active || c.state!=GameCore.PLAY || c.boss.kind!=Boss.RAINBOW_CAT)return;
        RainbowCat a=c.boss.cat;
        float cx=companionX(a,L),cy=companionY(a,L);
        if(c.companion.who>=0) {
            float lean=age<PULL?-.22f:.13f*(float)Math.sin(age*9);
            float cr=RunCompanion.radius(c,L)*mix(1.55f,1,ease((age-CLOSE)/1.1f));
            Trinket.drawReacting(p,c.companion.who,cx,cy,cr,c.companion.clock,1,
                    age<PULL?Trinket.FIGHT:0,lean,c.ninja());
        }
    }
    private void contour(float x,float y,float r,float scale) {
        // Broad, round base and curved narrow shoulders, inspired by the reference pet carrier.
        for(int i=0;i<=40;i++) {
            float t=Softbody.TAU*i/40,vertical=(float)Math.sin(t);
            float width=.83f+.16f*vertical;
            shell[i*2]=x+(float)Math.cos(t)*r*width*scale;
            shell[i*2+1]=y+vertical*r*1.10f*scale;
        }
    }
    private void pod(Painter p,float x,float y,float r,boolean front) {
        float lid=ease((age-DROP)/(CLOSE-DROP));
        if(!front) {
            // Low rounded fins and twin engine bells keep the carrier unmistakably rocket-shaped.
            for(int side=-1;side<=1;side+=2) {
                p.fillPoly(new float[]{x+side*r*.66f,y+r*.36f,x+side*r*1.13f,y+r*.72f,
                        x+side*r*1.07f,y+r*1.16f,x+side*r*.59f,y+r*.94f},0xFF466C8A);
                p.line(x+side*r*.86f,y+r*.65f,x+side*r*.97f,y+r*.99f,0xFF9BF8DD,r*.07f);
                p.fillEllipse(x+side*r*.43f,y+r*1.02f,r*.25f,r*.19f,0xFF345A72);
                p.fillEllipse(x+side*r*.43f,y+r*1.10f,r*.17f,r*.07f,0xFF9EFFF5);
            }
            float hatchLift=r*.35f*(1-lid);
            p.arc(x,y-r*1.10f-hatchLift,r*.24f,r*.22f,175,190,0xFF365F6D,r*.13f);
            p.arc(x,y-r*1.12f-hatchLift,r*.24f,r*.22f,185,170,0xFFA8F9BF,r*.065f);
            p.arc(x,y-hatchLift,r*.76f,r*1.08f,220,100,0xFF6EC8A4,r*.16f);
            p.arc(x,y-hatchLift-r*.03f,r*.76f,r*1.08f,220,100,0xFFC1FFDC,r*.055f);
            contour(x,y,r,1);p.fillPoly(shell,0xFF35586A);p.strokePoly(shell,0xFF283E58,r*.055f);
            contour(x-r*.015f,y-r*.02f,r,.95f);p.fillPoly(shell,0xFF74C7A5);
            contour(x,y+r*.01f,r,.84f);p.fillPoly(shell,0xFF27536A);
            contour(x,y-r*.04f,r,.77f);p.fillPoly(shell,0xFF41778D);
            p.fillEllipse(x-r*.23f,y-r*.45f,r*.32f,r*.22f,0x3872E8DA);
            // Open mouth of the pod: the net descends through this hatch.
            if(lid<1)p.fillEllipse(x,y-r*.90f,r*.56f,r*.13f*(1-lid),0xFF23394F);
        } else {
            // Glass slides shut after the cat lands, staying translucent enough to see her expression.
            if(lid>0) {
                p.save();p.clipRect(x-r,y-r*1.1f,x+r,y-r*1.1f+r*2.2f*lid);
                contour(x,y,r,.81f);p.fillPoly(shell,0x353CDBE7);
                p.arc(x,y,r*.65f,r*.85f,205,55,0x88E2FFF6,r*.075f);
                p.line(x-r*.40f,y-r*.55f,x+r*.02f,y-r*.77f,0x55FFFFFF,r*.12f);
                p.restore();
            }
            contour(x,y,r,.86f);p.strokePoly(shell,0xFF406E75,r*.14f);
            p.strokePoly(shell,0xFFA8EFB8,r*.065f);
            p.arc(x-r*.015f,y-r*.025f,r*.75f,r*.98f,210,95,0xFFD1FFDA,r*.07f);
            p.fillEllipse(x,y+r*.83f,r*.55f,r*.14f,0xFF74BA9A);
            for(int i=-2;i<=2;i++)p.fillCircle(x+i*r*.19f,y+r*.85f,r*.04f,0xFF325B69);
            p.fillCircle(x+r*.67f,y+r*.18f,r*.10f,0xFF34586D);
            p.fillCircle(x+r*.67f,y+r*.18f,r*.055f,age>=CLOSE?0xFFFFE887:0xFFB7FFE1);
            float charge=unit((age-CLOSE)/(LAUNCH-CLOSE));
            if(age>=CLOSE && age<LAUNCH) {
                float pulse=.5f+.5f*(float)Math.sin(age*33);
                contour(x,y,r,1.04f);p.strokePoly(shell,Glyph.withAlpha(0xFFDDFFEF,(int)(charge*(60+120*pulse))),r*.06f);
            }
        }
    }
    private void exhaust(Painter p,Layout L,float x,float y,float r) {
        if(age<LAUNCH)return;
        float t=age-LAUNCH,fade=1-ease((age-DEPART)/(DURATION-DEPART));
        if(fade<=0)return;
        // Bounded filled ribbons, no blur or expensive wide dynamic strokes.
        for(int band=0;band<6;band++) {
            for(int point=0;point<25;point++) {
                float u=point/24f,yy=mix(y+r*.92f,L.h*1.12f,u);
                float spread=r*(.85f+u*.5f),wave=(float)Math.sin(u*6-t*9)*r*.08f*u;
                float xx=x+wave+(band-2.5f)*spread/6;
                int back=(49-point)*2;
                trail[point*2]=xx-spread/12;trail[point*2+1]=yy;
                trail[back]=xx+spread/12;trail[back+1]=yy;
            }
            p.fillPoly(trail,Glyph.withAlpha(RainbowLand.CLEAR_COLORS[band],(int)(175*fade*unit(t/.12f))));
        }
        float burst=1-unit(t/.9f);
        for(int i=0;i<18 && burst>0;i++) {
            float angle=i*Softbody.TAU/18,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float length=L.h*.7f*unit(t/.18f),cx=L.w*.5f,cy=ground(L)+r*.8f;
            ray[0]=cx;ray[1]=cy;ray[2]=cx+dx*length-dy*r*.12f;ray[3]=cy+dy*length+dx*r*.12f;
            ray[4]=cx+dx*(length+r);ray[5]=cy+dy*(length+r);
            ray[6]=cx+dx*length+dy*r*.12f;ray[7]=cy+dy*length-dx*r*.12f;
            p.fillPoly(ray,Glyph.withAlpha(RainbowLand.CLEAR_COLORS[i%6],(int)(160*burst)));
        }
        for(int i=0;i<30;i++) {
            float u=(t*.65f+i*.618034f)%1,xx=x+(float)Math.sin(i*2.4f)*r*(.25f+u),yy=mix(y+r,L.h*1.05f,u);
            float size=r*(.018f+.035f*(1-u));int color=Glyph.withAlpha(RainbowLand.CLEAR_COLORS[i%6],(int)(230*fade*(1-u)));
            p.line(xx-size,yy,xx+size,yy,color,size*.7f);p.line(xx,yy-size*1.6f,xx,yy+size*1.6f,color,size*.7f);
        }
        for(int ring=0;ring<4;ring++) {
            float u=unit((t-ring*.09f)/.7f);if(u<=0 || u>=1)continue;
            p.strokeCircle(L.w*.5f,ground(L)+r*.9f,r*(.35f+3*u),
                    Glyph.withAlpha(RainbowLand.CLEAR_COLORS[ring],(int)(210*(1-u))),r*.15f*(1-u));
        }
    }
}
