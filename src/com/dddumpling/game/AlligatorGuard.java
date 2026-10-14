package com.dddumpling.game;

/** Companion defense feedback. Cosmetic clocks and colors never consume gameplay randomness. */
final class AlligatorGuard extends Draw {
    static final int DEFAULT=0xFF80DDE9;
    static final float SWIPE_TIME=.42f, BLOCK_TIME=.72f;
    static final float TRAVEL=.16f, HOME_AT=1.5f, RETURN=.40f, FLIGHT=HOME_AT+RETURN;
    static final int POINTS=32;
    final float[] trailX=new float[POINTS],trailY=new float[POINTS];
    int points;
    float age=BLOCK_TIME, duration=SWIPE_TIME, angle, clock, impactAge=BLOCK_TIME;
    float hitX,hitY;
    float flightAge=FLIGHT,fromX,fromY,toX,toY,impactScale=1,impactDuration=BLOCK_TIME;
    int impactPrimary=DEFAULT,impactSecondary=0xFFD7FAFF;
    boolean dazeImpact;
    int primary=DEFAULT,secondary=0xFFD7FAFF;
    boolean success,newStroke=true;

    void reset() {age=BLOCK_TIME;duration=SWIPE_TIME;impactAge=impactDuration=BLOCK_TIME;success=false;newStroke=true;points=0;
        flightAge=FLIGHT;fromX=fromY=toX=toY=0;dazeImpact=false;impactScale=1;}
    boolean active() {return age<duration;}
    void update(float dt) {age=Math.min(duration,age+dt);impactAge=Math.min(impactDuration,impactAge+dt);
        flightAge=Math.min(FLIGHT,flightAge+dt);clock+=dt;}
    boolean flying() {return flightAge<FLIGHT;}
    boolean carries(GameCore c) {return flying() && c.state==GameCore.PLAY && c.boss.kind==Boss.ALLIGATOR
            && c.boss.active() && !c.boss.beaten && c.companion.who>=0 && c.buddy.out();}
    private float offset(float from,float to) {
        if(flightAge<TRAVEL) {float t=flightAge/TRAVEL;return from+(to-from)*(1-(1-t)*(1-t)*(1-t));}
        return to*(1-AlligatorArrival.ramp(flightAge,HOME_AT,FLIGHT));
    }
    float companionX(Layout L) {return RunCompanion.x(L)+offset(fromX,toX)*L.w;}
    float companionY(Layout L) {return RunCompanion.y(L)+offset(fromY,toY)*L.h;}
    void returnHome() {
        if(!flying())return;
        toX=offset(fromX,toX);toY=offset(fromY,toY);flightAge=HOME_AT;
    }
    void dazeBurst() {
        if(!flying())return;
        dazeImpact=true;impactScale=2.25f;impactAge=0;impactDuration=1.05f;
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
            fromX=offset(fromX,toX);fromY=offset(fromY,toY);
            hitX=c.boss.alligator.tipX;hitY=c.boss.alligator.tipY;
            toX=(hitX-RunCompanion.x(L))/L.w;toY=(hitY-RunCompanion.y(L))/L.h;flightAge=0;
            Alligator a=c.boss.alligator;
            impactScale=a.dazeCue?(a.roundStreak>=Alligator.ROUNDS_FOR_DAZE-1?1.8f:1.25f):1;
            impactAge=0;impactDuration=BLOCK_TIME;dazeImpact=false;
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
            // A little shield sprouts above the companion before its curved wave travels outward.
            float r=RunCompanion.halfWidth(L)*(success?1.65f:1.25f)*(1+.12f*(float)Math.sin(age*16));
            float sy=y-L.unit*.7f;
            float[] shield={x-r,sy-r*.73f,x,sy-r*1.02f,x+r,sy-r*.73f,
                    x+r*.86f,sy+r*.25f,x,sy+r*.94f,x-r*.86f,sy+r*.25f};
            p.fillPoly(shield,fadeBy(primary,fade*.18f));
            p.strokePoly(shield,fadeBy(primary,fade*.65f),L.unit*(success?.43f:.26f));
            p.strokePoly(shield,fadeBy(secondary,fade),L.unit*.10f);
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
                    float rr=L.unit*(success?1.25f:.85f)*(1+i*.40f);
                    int color=Glyph.mix(primary,secondary,i/(float)Math.max(1,waves-1));
                    p.arc(endX,endY,rr,rr,angle-70,140,fadeBy(color,fade*(1-i*.13f)),width*.68f);
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
        if(!carries(c) || c.onboarding.companionAway(c))return;
        float x=companionX(L),y=companionY(L),r=RunCompanion.radius(c,L);
        float pop=(float)Math.sin(Math.min(1,flightAge/.32f)*Math.PI);
        float rock=.22f*(float)Math.sin(flightAge*12)*Math.min(1,(FLIGHT-flightAge)/RETURN);
        Trinket.drawReacting(p,c.companion.who,x,y,r*(1+.20f*pop),c.companion.clock,1,
                c.companion.reaction==RunCompanion.DAMAGE?3:6,rock,c.ninja());
    }
}
