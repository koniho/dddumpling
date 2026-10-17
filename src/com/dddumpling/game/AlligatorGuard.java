package com.dddumpling.game;

/** Companion defense feedback. Cosmetic clocks and colors never consume gameplay randomness. */
final class AlligatorGuard extends Draw {
    static final int DEFAULT=0xFF80DDE9;
    static final float SWIPE_TIME=.42f, BLOCK_TIME=.72f;
    static final float TRAVEL=.16f, HOME_AT=1.5f, RETURN=.40f, FLIGHT=HOME_AT+RETURN;
    static final float CONTACT_FLASH=.5f;
    static final float PARRY_SCALE=1.68f;
    static final float LIMB_RETURN=.42f;
    static final int POINTS=32;
    static final int TRAIL_POINTS=20;
    static final float TRAIL_TIME=.40f;
    final float[] moveX=new float[TRAIL_POINTS],moveY=new float[TRAIL_POINTS],moveLife=new float[TRAIL_POINTS];
    private final float[] normalX=new float[TRAIL_POINTS],normalY=new float[TRAIL_POINTS];
    int moves;
    final float[] trailX=new float[POINTS],trailY=new float[POINTS];
    int points;
    float age=BLOCK_TIME, duration=SWIPE_TIME, angle, clock, impactAge=BLOCK_TIME;
    float hitX,hitY,limbX,limbY;
    float contactAge=CONTACT_FLASH;
    float flightAge=FLIGHT,fromX,fromY,toX,toY,impactScale=1,impactDuration=BLOCK_TIME;
    float recoilX,recoilY,swayX,swayY;
    final float[] limbAge={LIMB_RETURN,LIMB_RETURN,LIMB_RETURN};
    final float[] limbFromX=new float[3],limbFromY=new float[3],limbKickX=new float[3],limbKickY=new float[3];
    float growthFrom,returnGrowth=1;
    int impactPrimary=DEFAULT,impactSecondary=0xFFD7FAFF;
    int hitLimb=-1;
    boolean pendingImpact,impactReady,openingCue;
    final GameCore.Particle[] impactParticles=new GameCore.Particle[12];
    boolean dazeImpact;
    int primary=DEFAULT,secondary=0xFFD7FAFF;
    boolean success,newStroke=true;

    void reset() {age=BLOCK_TIME;duration=SWIPE_TIME;impactAge=impactDuration=BLOCK_TIME;success=false;newStroke=true;points=0;
        flightAge=FLIGHT;fromX=fromY=toX=toY=growthFrom=0;recoilX=recoilY=swayX=swayY=0;
        returnGrowth=1;dazeImpact=false;impactScale=1;moves=0;hitLimb=-1;contactAge=CONTACT_FLASH;
        java.util.Arrays.fill(limbAge,LIMB_RETURN);clearPending();}
    private void clearPending() {
        pendingImpact=impactReady=openingCue=false;java.util.Arrays.fill(impactParticles,null);
        for(int i=0;i<3;i++)if(limbAge[i]<0)limbAge[i]=LIMB_RETURN;
    }
    boolean active() {return age<duration;}
    void update(float dt) {age=Math.min(duration,age+dt);impactAge=Math.min(impactDuration,impactAge+dt);
        contactAge=Math.min(CONTACT_FLASH,contactAge+dt);
        for(int i=0;i<3;i++)limbAge[i]=Math.min(LIMB_RETURN,limbAge[i]+dt);
        flightAge=Math.min(FLIGHT,flightAge+dt);clock+=dt;
        if(pendingImpact && flightAge>=TRAVEL) {
            pendingImpact=false;impactReady=true;impactAge=Math.min(impactDuration,flightAge-TRAVEL);
            contactAge=Math.min(CONTACT_FLASH,flightAge-TRAVEL);
        }
        int live=0;
        for(int i=0;i<moves;i++)if(moveLife[i]>dt) {
            moveX[live]=moveX[i];moveY[live]=moveY[i];moveLife[live++]=moveLife[i]-dt;
        }
        moves=live;
        if(flying())trackCompanion();
    }
    private void trackCompanion() {
        float x=offsetX(),y=offsetY();
        if(moves>0 && Math.abs(x-moveX[moves-1])+Math.abs(y-moveY[moves-1])<.002f)return;
        if(moves==TRAIL_POINTS) {
            System.arraycopy(moveX,1,moveX,0,--moves);System.arraycopy(moveY,1,moveY,0,moves);
            System.arraycopy(moveLife,1,moveLife,0,moves);
        }
        moveX[moves]=x;moveY[moves]=y;moveLife[moves++]=TRAIL_TIME;
    }
    boolean flying() {return flightAge<FLIGHT;}
    boolean carries(GameCore c) {return flying() && c.state==GameCore.PLAY && c.boss.kind==Boss.ALLIGATOR
            && c.boss.active() && !c.boss.beaten && c.companion.who>=0 && c.buddy.out();}
    private float offset(float from,float to,float recoil,float sway) {
        if(flightAge<TRAVEL) {float t=flightAge/TRAVEL;return from+(to-from)*t*t;}
        float t=Math.min(flightAge,HOME_AT)-TRAVEL;
        float bounce=1-(float)Math.exp(-t*5)*(float)Math.cos(t*18);
        float bob=AlligatorArrival.ramp(t,0,.25f)*(float)Math.sin(t*10);
        // Recoil away from the strike, then keep a little fighting bounce until returning.
        float held=to+recoil*(bounce+.18f*bob)+sway*bob;
        return held*(1-AlligatorArrival.ramp(flightAge,HOME_AT,FLIGHT));
    }
    private float offsetX() {return offset(fromX,toX,recoilX,swayX);}
    private float offsetY() {return offset(fromY,toY,recoilY,swayY);}
    float companionX(Layout L) {return RunCompanion.x(L)+offsetX()*L.w;}
    float companionY(Layout L) {return RunCompanion.y(L)+offsetY()*L.h;}
    float growth() {
        if(flightAge>=HOME_AT)return returnGrowth*(1-AlligatorArrival.ramp(flightAge,HOME_AT,FLIGHT));
        return growthFrom+(1-growthFrom)*AlligatorArrival.ramp(flightAge,0,TRAVEL);
    }
    float companionScale() {return 1+(PARRY_SCALE-1)*growth();}
    float colorPulse() {return .65f*(1-AlligatorArrival.ramp(contactAge,0,CONTACT_FLASH));}
    private float limbKick(int which) {
        float t=limbAge[which];
        if(t<=0 || t>=LIMB_RETURN)return 0;
        float u=t/.065f;
        return u*(float)Math.exp(1-u)*(1-AlligatorArrival.ramp(t,.25f,LIMB_RETURN));
    }
    float drawnLimbX(Layout L,int which,float normal) {
        float hold=1-AlligatorArrival.ramp(limbAge[which],.08f,.32f);
        return normal+(limbFromX[which]-normal)*hold+limbKickX[which]*L.w*limbKick(which);
    }
    float drawnLimbY(Layout L,int which,float normal) {
        float hold=1-AlligatorArrival.ramp(limbAge[which],.08f,.32f);
        return normal+(limbFromY[which]-normal)*hold+limbKickY[which]*L.h*limbKick(which);
    }
    void returnHome() {
        if(!flying())return;
        toX=offsetX();toY=offsetY();recoilX=recoilY=swayX=swayY=0;
        returnGrowth=growth();flightAge=HOME_AT;
        clearPending();
    }
    void dazeBurst() {
        if(!flying())return;
        dazeImpact=true;impactScale=2.25f;impactDuration=1.05f;impactAge=pendingImpact?impactDuration:0;
    }
    void prepareImpact(GameCore c,Layout L) {
        // Generate at swipe time to preserve gameplay RNG order; release only on contact.
        int first=c.particles.size();
        Fx.explode(c,c.rnd,hitX,hitY,L.enemyR,impactParticles.length,impactPrimary);
        for(int i=impactParticles.length-1;i>=0;i--)impactParticles[i]=c.particles.remove(first+i);
    }
    void feedback(GameCore c) {
        if(!impactReady)return;
        impactReady=false;
        if(c.state!=GameCore.PLAY || c.boss.kind!=Boss.ALLIGATOR || !c.boss.fighting()) {clearPending();return;}
        c.shake=Math.max(c.shake,impactScale>1.5f?.7f:.38f);c.bossDeathHaptic=1;
        Alligator a=c.boss.alligator;a.flash=1;a.jolt(.27f);
        if(c.boss.body!=null)c.boss.body.squash(.35f);
        for(int i=0;i<impactParticles.length;i++)if(impactParticles[i]!=null) {
            c.particles.add(impactParticles[i]);impactParticles[i]=null;
        }
        if(c.sound!=null) {c.sound.gatorParry();if(openingCue)c.sound.uiBloop();}
        openingCue=false;
    }
    void beginGesture() {newStroke=true;}
    void track(float x,float y) {
        if(newStroke || !active())return;
        append(x,y);age=Math.min(age,.12f);
    }
    private void append(float x,float y) {
        if(points>0) {
            float dx=x-trailX[points-1],dy=y-trailY[points-1];
            if(dx*dx+dy*dy<1)return;
            angle=(float)Math.atan2(dy,dx)*180/(float)Math.PI;
        }
        if(points==POINTS) {
            System.arraycopy(trailX,1,trailX,0,POINTS-1);System.arraycopy(trailY,1,trailY,0,POINTS-1);points--;
        }
        trailX[points]=x;trailY[points++]=y;
    }
    void emit(GameCore c,Layout L,float x0,float y0,float x1,float y1,boolean blocked) {
        if(newStroke || !active()) {points=0;success=false;newStroke=false;clock=0;append(x0,y0);}
        append(x1,y1);
        success |= blocked;age=0;duration=success?BLOCK_TIME:SWIPE_TIME;
        int who=c.companion.who>=0?c.companion.who:Math.max(0,Math.min(Collect.COUNT-1,c.runWho));
        primary=success?successColor(Collect.BODY[who],false):DEFAULT;
        secondary=success?successColor(Collect.ACCENT[who],true):0xFFD7FAFF;
        if(blocked) {
            if(pendingImpact && hitLimb>=0)limbAge[hitLimb]=LIMB_RETURN;
            fromX=offsetX();fromY=offsetY();
            Alligator a=c.boss.alligator;
            limbX=a.tipX;limbY=a.tipY;
            toX=(limbX-RunCompanion.x(L))/L.w;toY=(limbY-RunCompanion.y(L))/L.h;
            float dx=(fromX-toX)*L.w,dy=(fromY-toY)*L.h;
            float distance=(float)Math.hypot(dx,dy);
            if(distance<1) {dx=-toX*L.w;dy=-toY*L.h;distance=(float)Math.hypot(dx,dy);}
            if(distance<1) {dx=0;dy=1;distance=1;}
            dx/=distance;dy/=distance;
            // Meet at the two silhouettes' edges, never at the limb's centre.
            float limbRadius=L.w*(a.limb==2?.048f:.09f);
            float companionRadius=RunCompanion.radius(c,L)*PARRY_SCALE;
            hitX=limbX+dx*limbRadius;hitY=limbY+dy*limbRadius;
            toX=(hitX+dx*companionRadius-RunCompanion.x(L))/L.w;
            toY=(hitY+dy*companionRadius-RunCompanion.y(L))/L.h;
            recoilX=dx*.09f;recoilY=dy*.09f*L.w/L.h;
            swayX=-dy*.009f;swayY=dx*.009f*L.w/L.h;
            growthFrom=growth();returnGrowth=1;flightAge=0;
            trackCompanion();
            hitLimb=a.limb;pendingImpact=true;impactReady=false;openingCue=a.dazeCue;
            if(hitLimb>=0) {
                limbAge[hitLimb]=-TRAVEL;limbFromX[hitLimb]=limbX;limbFromY[hitLimb]=limbY;
                // The limb receives the opposite impulse; each combo limb settles independently.
                float kick=hitLimb==2?.15f:.11f;
                limbKickX[hitLimb]=-dx*kick;limbKickY[hitLimb]=-dy*kick*L.w/L.h;
            }
            impactScale=a.dazeCue?(a.roundStreak>=Alligator.ROUNDS_FOR_DAZE-1?1.8f:1.25f):1;
            impactAge=impactDuration=BLOCK_TIME;dazeImpact=false;
            impactPrimary=primary;impactSecondary=secondary;
            c.companion.react(RunCompanion.TOUCH,.8f);
        }
    }
    static int successColor(int color,boolean accent) {
        float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        float max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),chroma=max-min;
        float hue=chroma<.001f?0:max==r?(g-b)/chroma:max==g?2+(b-r)/chroma:4+(r-g)/chroma;
        if(hue<0)hue+=6;
        // Pale neutral, mint, aqua and blue would read as the ordinary shield again.
        boolean similar=chroma<.12f || (hue>=2.15f && hue<=3.85f);
        return similar?(accent?Glyph.mix(YELLOW,0xFFFFFFFF,.35f):YELLOW):color;
    }
    void draw(Painter p,GameCore c,Layout L) {
        if((!active() && impactAge>=impactDuration) || c.state!=GameCore.PLAY || c.boss.kind!=Boss.ALLIGATOR || c.boss.beaten)return;
        float x=flying()?companionX(L):RunCompanion.x(L),y=(flying()?companionY(L):RunCompanion.y(L))+c.companion.damagePush(L);
        float fade=Math.max(0,1-age/duration);
        if(fade>0) {
            // Unsuccessful swipes keep a small circular cue; parries carry the large rings.
            if(!flying()) {
                float r=RunCompanion.halfWidth(L)*(success?1.65f:1.25f)*(1+.12f*(float)Math.sin(age*16));
                p.fillCircle(x,y,r*.85f,fadeBy(secondary,fade*.15f));
                p.strokeCircle(x,y,r,fadeBy(primary,fade*.7f),L.unit*.32f);
                p.strokeCircle(x,y,r*1.2f,fadeBy(secondary,fade*.5f),L.unit*.20f);
            }
            if(points>1) {
                float endX=trailX[points-1],endY=trailY[points-1];
                // The energy comes from the companion and bends to the live fingertip.
                float[] link=new float[42];
                for(int i=0;i<=20;i++) {
                    float t=i/20f,u=1-t;
                    link[i*2]=u*u*x+2*u*t*(x+(endX-x)*.18f)+t*t*endX;
                    link[i*2+1]=u*u*y+2*u*t*(y+(endY-y)*.65f)+t*t*endY;
                }
                float width=L.unit*(success?.48f:.25f);
                p.polyline(link,fadeBy(primary,fade*.16f),width*3.5f);
                p.polyline(link,fadeBy(primary,fade*.62f),width);
                for(int i=1;i<points;i++) {
                    float strength=fade*(.25f+.75f*i/(points-1f));
                    int color=Glyph.mix(primary,secondary,i/(points-1f));
                    p.line(trailX[i-1],trailY[i-1],trailX[i],trailY[i],fadeBy(color,strength*.20f),width*5);
                    p.line(trailX[i-1],trailY[i-1],trailX[i],trailY[i],fadeBy(color,strength),width*1.5f);
                    p.line(trailX[i-1],trailY[i-1],trailX[i],trailY[i],fadeBy(0xFFFFFFFF,strength*.75f),width*.32f);
                }
                int waves=success?5:2;
                for(int i=0;i<waves;i++) {
                    float phase=(clock*2+i/(float)waves)%1;
                    int at=Math.min(19,(int)(phase*20));
                    p.fillCircle(link[at*2],link[at*2+1],L.unit*(success?.27f:.15f),fadeBy(secondary,fade*.8f));
                }
            }
        }
        if(impactAge<impactDuration) {
            float burst=impactAge/impactDuration;
            fade=1-burst;
            int rings=impactScale>1.5f?3:2;
            for(int i=0;i<rings;i++)p.strokeCircle(hitX,hitY,(L.unit+L.w*.14f*burst)*impactScale*(1-i*.18f),
                    fadeBy(i%2==0?impactPrimary:impactSecondary,fade*(1-i*.18f)),L.unit*.22f*fade);
            int sparks=impactScale>1.5f?14:9;
            for(int i=0;i<sparks;i++) {
                float a=i*Softbody.TAU/sparks,rr=L.w*(.03f+.15f*burst)*impactScale;
                float sx=hitX+(float)Math.cos(a)*rr,sy2=hitY+(float)Math.sin(a)*rr;
                float length=L.unit*.50f*fade*impactScale;
                int color=fadeBy(i%2==0?impactPrimary:impactSecondary,fade);
                p.line(sx-length,sy2,sx+length,sy2,color,L.unit*.10f);
                p.line(sx,sy2-length,sx,sy2+length,color,L.unit*.10f);
            }
            if(dazeImpact && c.boss.body!=null) {
                float u=AlligatorArrival.ramp(burst,0,.6f);
                float cx=hitX+(c.boss.body.centreX()-hitX)*u,cy=hitY+(c.boss.body.centreY()-hitY)*u;
                p.strokeCircle(cx,cy,L.unit+L.w*.22f*burst,fadeBy(impactSecondary,fade*.85f),L.unit*.3f*fade);
            }
        }
    }
    void drawCompanion(Painter p,GameCore c,Layout L) {
        if(c.state!=GameCore.PLAY || c.boss.kind!=Boss.ALLIGATOR || !c.boss.active() || c.boss.beaten
                || c.companion.who<0 || !c.buddy.out() || c.onboarding.companionAway(c))return;
        int who=c.companion.who,body=Collect.BODY[who],accent=Collect.ACCENT[who];
        drawTrail(p,c,L,body,accent);
        if(!carries(c))return;
        float x=companionX(L),y=companionY(L),r=RunCompanion.radius(c,L)*companionScale();
        float growth=growth(),pulse=.5f+.5f*(float)Math.sin(c.companion.clock*9);
        p.fillCircle(x,y,r*1.14f,fadeBy(Glyph.mix(body,0xFFFFFFFF,.65f),growth*(.12f+.24f*pulse*pulse)));
        for(int wave=0;wave<2;wave++) {
            float phase=(c.companion.clock*1.6f+wave*.5f)%1;
            for(int band=0;band<4;band++) {
                float radius=r*(1.25f+phase*.85f-band*.11f);
                int color=Glyph.mix(body,accent,band/3f);
                if(band==3)color=Glyph.mix(color,0xFFFFFFFF,.55f);
                p.strokeCircle(x,y,radius,fadeBy(color,growth*(1-phase)*.8f),r*.14f);
            }
        }
        for(int i=0;i<3;i++) {
            float a=c.companion.clock*2+i*Softbody.TAU/3,flash=(float)Math.pow(Math.max(0,Math.sin(c.companion.clock*11+i*2)),2);
            p.fillCircle(x+(float)Math.cos(a)*r*.84f,y+(float)Math.sin(a)*r*.84f,r*(.09f+.08f*flash),
                    fadeBy(0xFFFFFFFF,growth*flash*.85f));
        }
        float rock=.22f*(float)Math.sin(flightAge*12)*Math.min(1,(FLIGHT-flightAge)/RETURN);
        float tint=colorPulse();
        Painter sprite=tint==0?p:new ColorFadePainter(p,Glyph.mix(accent,0xFFFFFFFF,.45f),tint,1);
        Trinket.drawReacting(sprite,who,x,y,r,c.companion.clock,1,
                c.companion.reaction==RunCompanion.DAMAGE?3:Trinket.FIGHT,rock*.35f,c.ninja());
        RunCompanion.drawProps(sprite,c,L,x,y,r);
    }
    private void drawTrail(Painter p,GameCore c,Layout L,int body,int accent) {
        if(moves<2)return;
        float homeX=RunCompanion.x(L),homeY=RunCompanion.y(L);
        for(int i=0;i<moves;i++) {
            int before=Math.max(0,i-1),after=Math.min(moves-1,i+1);
            float dx=(moveX[after]-moveX[before])*L.w,dy=(moveY[after]-moveY[before])*L.h;
            float length=Math.max(.001f,(float)Math.hypot(dx,dy));
            normalX[i]=-dy/length;normalY[i]=dx/length;
        }
        // Adjacent ribbon sections share edges, avoiding overlapping round-cap beads.
        for(int layer=0;layer<3;layer++)for(int i=1;i<moves;i++) {
            float fade=Math.min(moveLife[i-1],moveLife[i])/TRAIL_TIME;fade*=fade;
            int color=layer==0?body:layer==1?accent:Glyph.mix(accent,0xFFFFFFFF,.65f);
            float width=RunCompanion.radius(c,L)*(layer==0?1.8f:layer==1?1.1f:.375f);
            float ax=homeX+moveX[i-1]*L.w,ay=homeY+moveY[i-1]*L.h;
            float bx=homeX+moveX[i]*L.w,by=homeY+moveY[i]*L.h;
            float wa=width*Math.min(1,moveLife[i-1]/.18f),wb=width*Math.min(1,moveLife[i]/.18f);
            p.fillPoly(new float[]{ax+normalX[i-1]*wa,ay+normalY[i-1]*wa,bx+normalX[i]*wb,by+normalY[i]*wb,
                    bx-normalX[i]*wb,by-normalY[i]*wb,ax-normalX[i-1]*wa,ay-normalY[i-1]*wa},
                    fadeBy(color,fade*(layer==0?.22f:layer==1?.45f:.60f)));
        }
    }
}
