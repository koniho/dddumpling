package com.dddumpling.game;

/** A rounded carrier becomes a rocket; victory waits until its rainbow exhaust has cleared. */
final class RainbowCatRocket extends Draw {
    static final float FETCH=1f,PULL=2.5f,TIP=3.25f,RELEASE=4f,DROP=4.5f,CLOSE=5.05f,
            LAUNCH=5.7f,DEPART=7.1f,DURATION=8f,SHAKE_STEP=.22f;
    static final int SHAKES=3;
    static final float FINAL_BURST=1.05f;
    boolean active,finalImpact;
    float age;
    int feedback,shakeBeats,launchBeats;
    final float[] shell=new float[82],ray=new float[8],trail=new float[100];
    void reset() {active=finalImpact=false;age=0;feedback=shakeBeats=launchBeats=0;}
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
        if(!finalImpact) {
            finalImpact=true;c.bossDeathHaptic=2;c.shake=Math.max(c.shake,1.05f);
            c.flash=Math.max(c.flash,.38f);c.flashColor=0xFFFFEFFF;
        }
        while(shakeBeats<SHAKES && age>=TIP+SHAKE_STEP*(shakeBeats+.5f)) {
            shakeBeats++;c.bossDeathHaptic=Math.max(c.bossDeathHaptic,1);
            c.shake=Math.max(c.shake,.12f);
            if(c.sound!=null)c.sound.ninjaSwish(shakeBeats%3);
        }
        if(age>=DROP && feedback<1) {
            feedback=1;if(c.sound!=null)c.sound.squish(Kawaii.CAT,2);
            c.bossDeathHaptic=2;c.shake=Math.max(c.shake,.35f);
        }
        if(age>=CLOSE && feedback<2) {
            feedback=2;if(c.sound!=null)c.sound.uiBloop();c.bossDeathHaptic=Math.max(c.bossDeathHaptic,1);
        }
        if(age>=LAUNCH && feedback<3) {
            feedback=3;if(c.sound!=null)c.sound.courseFinish();
            c.shake=Math.max(c.shake,.9f);c.flash=Math.max(c.flash,.45f);
            c.flashColor=0xFFF0DFFF;c.bossDeathHaptic=2;
        }
        while(launchBeats<4 && age>=LAUNCH+launchBeats*.13f) {
            launchBeats++;c.bossDeathHaptic=Math.max(c.bossDeathHaptic,launchBeats==1?2:1);
        }
        if(age>=CLOSE && c.sound!=null)c.sound.rocket(thrust());
    }
    float thrust() {
        if(age<CLOSE || age>=DEPART+.3f)return 0;
        float ignition=.15f+.85f*ease((age-CLOSE)/(LAUNCH-CLOSE));
        return ignition*(1-ease((age-DEPART)/.3f));
    }
    float shakeMotion() {
        float t=(age-TIP)/SHAKE_STEP;
        if(t<0 || t>=SHAKES)return 0;
        return (float)Math.sin((t-(float)Math.floor(t))*Math.PI);
    }
    float netAngle() {
        float shake=age>=TIP && age<RELEASE?.10f*(float)Math.sin((age-TIP)/SHAKE_STEP*Softbody.TAU):0;
        return -(float)Math.PI*ease((age-PULL)/(TIP-PULL))+shake;
    }
    float catAngle() {return age<RELEASE?netAngle():-(float)Math.PI*(1+ease((age-DROP)/.35f));}
    float lift() {return ease((age-PULL)/(TIP-PULL))*(1-ease((age-DROP)/.45f));}

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
        x-=L.w*.055f*lift();
        return mix(x,RunCompanion.x(L),ease((age-CLOSE)/1.1f));
    }
    float companionY(RainbowCat a,Layout L) {
        float walk=ground(L)+radius(L)*.9f;
        if(age<FETCH)return mix(a.catchY+a.r*2.15f,walk,ease(age/FETCH));
        return mix(walk,RunCompanion.y(L),ease((age-CLOSE)/1.1f))
                -radius(L)*1.2f*lift()+radius(L)*.11f*shakeMotion()
                -(age<PULL?Math.abs((float)Math.sin(age*17))*L.w*.016f:0);
    }
    float netX(RainbowCat a,Layout L) {
        float held=companionX(a,L)-L.w*.11f;
        if(age<FETCH)return mix(a.x,held,ease(age/FETCH));
        return mix(held,rocketX(L)+radius(L)*.12f,ease((age-PULL)/(TIP-PULL)))
                +radius(L)*.6f*ease((age-RELEASE)/.55f);
    }
    float netY(RainbowCat a,Layout L) {
        float held=ground(L)-radius(L)*1.45f;
        if(age<FETCH)return mix(a.y,held,ease(age/FETCH));
        return held-radius(L)*.22f*ease((age-PULL)/(TIP-PULL))
                +radius(L)*.14f*shakeMotion()-radius(L)*.3f*ease((age-RELEASE)/.55f);
    }
    float catX(RainbowCat a,Layout L) {
        return age<RELEASE?netX(a,L):rocketX(L)+radius(L)*.12f;
    }
    float catY(RainbowCat a,Layout L) {
        if(age<RELEASE)return netY(a,L);
        float fall=unit((age-RELEASE)/(DROP-RELEASE));
        return mix(ground(L)-radius(L)*1.67f,rocketY(L)+radius(L)*.02f,fall*fall);
    }
    private Painter fitTurn(Painter p,RainbowCat a,float x,float y,float scale,float angle) {
        return new BossScreen.PortraitFit(new LinkedPairArt.TurnPainter(p,x,y,angle),a.x,a.y,0,0,scale);
    }
    private void finalBurst(Painter p,RainbowCat a,Layout L) {
        if(age>=FINAL_BURST)return;
        float t=age/FINAL_BURST,fade=(1-t)*(1-t),x=a.catchX,y=a.catchY;
        // Fixed ray/star counts and reusable geometry keep the victory flash cheap to draw.
        for(int i=0;i<18;i++) {
            float angle=i*Softbody.TAU/18+.12f*t,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float near=a.r*(.65f+t*2),far=L.w*(.34f+.70f*ease(t*2))*(.75f+.12f*(i%3));
            float width=a.r*(.25f+.14f*(i%3))*fade;
            ray[0]=x+dx*near;ray[1]=y+dy*near;
            ray[2]=x+dx*far-dy*width;ray[3]=y+dy*far+dx*width;
            ray[4]=x+dx*(far+a.r*.5f);ray[5]=y+dy*(far+a.r*.5f);
            ray[6]=x+dx*far+dy*width;ray[7]=y+dy*far-dx*width;
            int color=RainbowLand.CLEAR_COLORS[i%6];
            p.fillPoly(ray,Glyph.withAlpha(color,(int)(170*fade)));
            p.line(ray[0],ray[1],ray[4],ray[5],Glyph.withAlpha(0xFFFFFFFF,(int)(115*fade)),L.w*.006f);
        }
        for(int i=0;i<6;i++) {
            float u=(age-i*.055f)/.75f;if(u<0 || u>=1)continue;
            float radius=a.r*.65f+L.w*.85f*(1-(1-u)*(1-u));
            int alpha=(int)(235*(1-u)*(1-u)),color=RainbowLand.CLEAR_COLORS[i];
            p.strokeCircle(x,y,radius,Glyph.withAlpha(color,alpha/3),L.w*.065f);
            p.strokeCircle(x,y,radius,Glyph.withAlpha(color,alpha),L.w*.032f);
            p.strokeCircle(x,y,radius,Glyph.withAlpha(0xFFFFFFFF,alpha/2),L.w*.007f);
        }
        for(int i=0;i<30;i++) {
            float angle=i*2.399963f,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float distance=a.r+(L.w*(.26f+.055f*(i%5)))*ease(t);
            float sx=x+dx*distance,sy=y+dy*distance+L.w*.14f*t*t;
            float r=L.w*(.013f+.004f*(i%3))*(1-t),thin=r*.22f;
            int color=Glyph.withAlpha(RainbowLand.CLEAR_COLORS[i%6],(int)(255*(1-t)));
            ray[0]=sx;ray[1]=sy-r;ray[2]=sx+thin;ray[3]=sy;
            ray[4]=sx;ray[5]=sy+r;ray[6]=sx-thin;ray[7]=sy;p.fillPoly(ray,color);
            ray[0]=sx-r;ray[1]=sy;ray[2]=sx;ray[3]=sy-thin;
            ray[4]=sx+r;ray[5]=sy;ray[6]=sx;ray[7]=sy+thin;p.fillPoly(ray,color);
            p.fillCircle(sx,sy,r*.2f,Glyph.withAlpha(0xFFFFFFFF,(int)(255*fade)));
        }
        if(age<.18f)p.fillCircle(x,y,a.r*(1+age*7),Glyph.withAlpha(0xFFFFFFFF,(int)(220*(1-age/.18f))));
    }
    void draw(Painter p,GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;
        RainbowCatArrival.stars(new OpacityPainter(p,1-ease((age-DEPART)/(DURATION-DEPART))),a,c.boss,L);
        finalBurst(p,a,L);
        float r=radius(L),x=rocketX(L),y=rocketY(L),cx=companionX(a,L),cy=companionY(a,L);
        exhaust(p,L,x,y,r);
        pod(p,x,y,r,false);
        float catScale=mix(1,.62f,ease(age/FETCH));
        float nx=netX(a,L),ny=netY(a,L),angle=netAngle();
        Painter meshFit=fitTurn(p,a,nx,ny,catScale,angle);
        float netFade=1-ease((age-RELEASE)/.55f);
        if(netFade>0) {
            RainbowCatArt.net(new OpacityPainter(meshFit,netFade),c,L,false,false);
            // Transform the hoop's grip with the overturned mesh; keep the pole in the companion's hands.
            float gripX=a.r*1.9f*catScale,gripY=-a.r*1.25f*catScale;
            float gx=nx+gripX*(float)Math.cos(angle)-gripY*(float)Math.sin(angle);
            float gy=ny+gripX*(float)Math.sin(angle)+gripY*(float)Math.cos(angle);
            Painter pole=new OpacityPainter(p,netFade);
            pole.line(cx,cy,gx,gy,0xFF485777,L.w*.018f);
            pole.line(cx-L.w*.003f,cy,gx-L.w*.003f,gy,0xFFC4F9EC,L.w*.008f);
        }
        Painter cat=fitTurn(p,a,catX(a,L),catY(a,L),catScale,catAngle());
        RainbowCatArt.cat(new ColorFadePainter(cat,0xFFFFF5FF,.8f*(1-ease(age/.45f)),1),a.x,a.y,a.r,a.clock,a.turn,a.pastry,a.head,true,1);
        if(netFade>0)RainbowCatArt.net(new OpacityPainter(meshFit,netFade),c,L,true,false);
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
            float turn=-.72f*lift()+.22f*shakeMotion();
            Painter actor=new LinkedPairArt.TurnPainter(p,cx,cy,turn);
            float cr=RunCompanion.radius(c,L)*mix(1.55f,1,ease((age-CLOSE)/1.1f));
            Trinket.drawReacting(actor,c.companion.who,0,0,cr,c.companion.clock,1,
                    age<RELEASE?Trinket.FIGHT:0,lean,c.ninja());
            if(age>=PULL && age<RELEASE) {
                int color=Collect.BODY[c.companion.who];
                actor.fillEllipse(-cr*.22f,-cr*.08f,cr*.24f,cr*.18f,color);
                actor.fillEllipse(cr*.22f,-cr*.08f,cr*.24f,cr*.18f,color);
            }
        }
    }
    private void contour(float x,float y,float r,float scale) {
        // Soft squared carrier: broad base, round shoulders and an even inset window rim.
        for(int i=0;i<=40;i++) {
            float t=Softbody.TAU*i/40,sy=(float)Math.sin(t),sx=(float)Math.cos(t);
            shell[i*2]=x+Math.copySign((float)Math.pow(Math.abs(sx),.82f),sx)*r*(.85f+.07f*sy)*scale;
            shell[i*2+1]=y+Math.copySign((float)Math.pow(Math.abs(sy),.85f),sy)*r*1.05f*scale;
        }
    }
    private void shellGlint(Painter p,float x,float y,float r,float scale,int from,int to,int color,float width) {
        contour(x,y,r,scale);
        int count=to-from+1;float[] glint=new float[count*4];
        // One filled ribbon keeps translucent highlights smooth where line segments would overlap.
        for(int i=0;i<count;i++) {
            int at=from+i,previous=Math.max(0,at-1),next=Math.min(40,at+1);
            float dx=shell[next*2]-shell[previous*2],dy=shell[next*2+1]-shell[previous*2+1];
            float length=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy));
            float half=width*.5f*(.35f+.65f*(float)Math.sin(Math.PI*i/(count-1)));
            int back=(count*2-1-i)*2;
            glint[i*2]=shell[at*2]-dy/length*half;glint[i*2+1]=shell[at*2+1]+dx/length*half;
            glint[back]=shell[at*2]+dy/length*half;glint[back+1]=shell[at*2+1]-dx/length*half;
        }
        p.fillPoly(glint,color);
    }
    private void fin(Painter p,float x,float y,float r,int side) {
        float[] edge={.65f,.26f,.78f,.32f,.99f,.54f,1.10f,.78f,1.12f,1.03f,
                1.07f,1.13f,.95f,1.12f,.69f,.98f,.59f,.77f};
        for(int i=0;i<edge.length;i+=2) {edge[i]=x+edge[i]*r*side;edge[i+1]=y+edge[i+1]*r;}
        p.fillPoly(edge,0xFF376A76);p.strokePoly(edge,0xFF294656,r*.035f);
        p.line(x+side*r*.91f,y+r*.63f,x+side*r*1.02f,y+r*.95f,0xFF8CE4C1,r*.10f);
        p.line(x+side*r*.89f,y+r*.63f,x+side*r*.99f,y+r*.92f,0xFFC3F8D9,r*.035f);
    }
    private void pod(Painter p,float x,float y,float r,boolean front) {
        float lid=ease((age-DROP)/(CLOSE-DROP));
        if(!front) {
            for(int side=-1;side<=1;side+=2) {
                fin(p,x,y,r,side);
                p.fillEllipse(x+side*r*.43f,y+r*1.01f,r*.23f,r*.16f,0xFF2D4E60);
                p.fillEllipse(x+side*r*.43f-r*.015f,y+r*.99f,r*.20f,r*.13f,0xFF64999E);
                p.fillEllipse(x+side*r*.43f,y+r*1.08f,r*.16f,r*.055f,0xFFB0FFF1);
            }
            // Nested offset silhouettes shade the solid shell without piling up bright outlines.
            contour(x,y,r,1);p.fillPoly(shell,0xFF2F515C);
            for(int layer=0;layer<5;layer++) {
                float u=layer/4f;
                contour(x-r*.035f*u,y-r*.035f*u,r,.975f-layer*.018f);
                p.fillPoly(shell,Glyph.mix(0xFF519B8C,0xFFBAEACC,u));
            }
            contour(x,y-r*.015f,r,.81f);p.fillPoly(shell,0xFF264A5C);
            contour(x,y-r*.015f,r,.775f);p.fillPoly(shell,0xFF457F8D);
            contour(x-r*.02f,y-r*.045f,r,.72f);p.fillPoly(shell,0xFF538E99);
            shellGlint(p,x,y,r,.99f,21,32,0xBBD5FFE0,r*.027f);
            if(lid<1)p.fillEllipse(x,y-r*.88f,r*.54f,r*.12f*(1-lid),0xFF23394F);
        } else {
            if(lid>0) {
                // A recessed glass door seals down over the occupant; a narrow reflection gives depth.
                p.save();p.clipRect(x-r,y-r*1.1f,x+r,y-r*1.1f+r*2.2f*lid);
                contour(x,y-r*.015f,r,.775f);p.fillPoly(shell,0x283CDBE7);
                shellGlint(p,x,y-r*.015f,r,.71f,23,30,0x79E4FFF8,r*.045f);
                shellGlint(p,x,y-r*.015f,r,.68f,24,29,0x35E4FFF8,r*.09f);
                p.restore();
            }
            contour(x,y-r*.015f,r,.805f);p.strokePoly(shell,0xFF3C736F,r*.065f);
            shellGlint(p,x,y-r*.015f,r,.825f,21,32,0xFFC6F7D8,r*.035f);
            shellGlint(p,x,y-r*.015f,r,.825f,0,17,0x80598980,r*.025f);
            // A single smooth cap lifts with its carry loop, then settles flush onto the shoulders.
            float hatchY=y-r*.30f*(1-lid);
            contour(x,hatchY,r,.96f);
            float[] cap=new float[30];System.arraycopy(shell,24*2,cap,0,26);
            cap[26]=x+r*.55f;cap[27]=hatchY-r*.87f;
            cap[28]=x-r*.55f;cap[29]=hatchY-r*.87f;
            p.fillPoly(cap,0xFF7FC8A5);
            shellGlint(p,x-r*.01f,hatchY-r*.015f,r,.955f,24,36,0xFFCAF6D7,r*.055f);
            p.arc(x,hatchY-r*1.03f,r*.21f,r*.20f,180,180,0xFF31545E,r*.115f);
            p.arc(x,hatchY-r*1.045f,r*.21f,r*.20f,185,170,0xFF9DDEC0,r*.065f);
            for(int i=-2;i<=2;i++) {
                float xx=x+i*r*.17f,yy=y+r*.91f;
                p.line(xx,yy-r*.024f,xx,yy+r*.024f,0xFF43756F,r*.048f);
                p.line(xx-r*.006f,yy+r*.025f,xx+r*.012f,yy+r*.025f,0xFFCBF6D9,r*.015f);
            }
            p.fillCircle(x+r*.72f,y+r*.12f,r*.075f,0xFF39686C);
            p.fillCircle(x+r*.72f,y+r*.12f,r*.043f,age>=CLOSE?0xFFFFE887:0xFFB7FFE1);
            float charge=unit((age-CLOSE)/(LAUNCH-CLOSE));
            if(age>=CLOSE && age<LAUNCH) {
                float pulse=.5f+.5f*(float)Math.sin(age*33);
                contour(x,y,r,1.02f);p.strokePoly(shell,Glyph.withAlpha(0xFFDDFFEF,(int)(charge*(45+90*pulse))),r*.035f);
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
