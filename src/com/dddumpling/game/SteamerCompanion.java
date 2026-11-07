package com.dddumpling.game;

/** A cosmetic helper at the stove; only scored pips feed its recent-effort meter. */
final class SteamerCompanion extends Draw {
    static final float TRAVEL=.72f,RETURN=.55f,IDLE_SPEED=.45f;
    private static final int[] FAN_COLORS={0xFF457AE6,0xFFFFA54D,0xFFEE6480,0xFF8E55D9,0xFF42CAB0};
    float phase,effort,gust;

    void clear() {phase=effort=gust=0;}
    void scored() {effort=Math.min(1,effort+.28f);gust=1;}
    float sweep() {return .12f+.76f*ease(gust);}
    float speed() {return IDLE_SPEED+3.8f*effort;}
    void update(GameCore c,float dt) {
        if(!scene(c)) {clear();return;}
        phase=(phase+dt*speed()*Softbody.TAU)%Softbody.TAU;
        effort*=(float)Math.exp(-dt*2.2f);
        gust=Math.max(0,gust-dt/.65f);
        if(effort<.005f)effort=0;
    }
    static boolean scene(GameCore c) {
        return c.state==GameCore.BONUS && !c.starBonus && !c.bossReward && !CaveInterlude.active(c);
    }
    private static float ease(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    static float travel(GameCore c) {
        if(!scene(c) || c.bonusStatus() || c.bonusParading())return 0;
        float exit=c.bonusPrizeWon()?(Steamer.FREE_TIME-c.steamer.freedT)/RETURN
                :(GameCore.MASH_END-c.bonusTimer)/RETURN;
        exit=Math.max(0,Math.min(1,exit));
        // Clear the reward captions promptly, then ease into the home bubble.
        float back=1-(1-exit)*(1-exit)*(1-exit);
        if(c.cloudUi.reduceMotion)return back>0?0:1;
        return ease(c.time/TRAVEL)*(1-back);
    }
    static boolean pointing(GameCore c) {return scene(c) && c.bonusSwipeReady();}
    private static float sway(GameCore c) {
        if(c.cloudUi.reduceMotion)return 0;
        if(pointing(c))return .10f*(float)Math.sin(c.companion.clock*9);
        float energy=ease(c.companion.steamer.gust);
        return (.18f+.12f*energy)*(float)Math.sin(c.companion.steamer.phase)+.06f*energy;
    }
    private static float bob(GameCore c) {
        if(c.cloudUi.reduceMotion)return 0;
        if(pointing(c))return -.30f*Math.abs((float)Math.sin(c.companion.clock*9));
        float swing=(float)Math.sin(c.companion.steamer.phase),energy=ease(c.companion.steamer.gust);
        return .09f*swing*(1-energy)-energy*(.12f+.22f*Math.abs(swing));
    }
    static float x(GameCore c,Layout L) {
        float t=travel(c);
        return RunCompanion.x(L)+(L.w*.20f-RunCompanion.x(L)+StarPath.flyerR(L)*sway(c))*t;
    }
    static float workY(Layout L) {return Screens.steamerBaseY(L)+StarPath.flyerR(L)*3.1f;}
    static float y(GameCore c,Layout L) {
        float t=travel(c),target=workY(L)+StarPath.flyerR(L)*bob(c);
        return RunCompanion.y(L)+(target-RunCompanion.y(L))*t;
    }
    static float radius(GameCore c,Layout L) {
        float home=RunCompanion.radius(c,L);
        return home+(StarPath.flyerR(L)-home)*travel(c);
    }
    private static int fanColor(int body) {
        int chosen=FAN_COLORS[0],best=-1;
        for(int color:FAN_COLORS) {
            int r=((body>>16)&255)-((color>>16)&255);
            int g=((body>>8)&255)-((color>>8)&255),b=(body&255)-(color&255);
            int contrast=r*r+g*g+b*b;
            if(contrast>best) {best=contrast;chosen=color;}
        }
        return chosen;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float travel=travel(c);
        if(travel<=0 || c.companion.who<0 || !c.buddy.out() || c.onboarding.companionAway(c))return;
        SteamerCompanion f=c.companion.steamer;
        boolean still=c.cloudUi.reduceMotion,ready=pointing(c);
        float x=x(c,L),y=y(c,L),r=radius(c,L),phase=still?0:f.phase;
        float swing=(float)Math.sin(phase),prop=travel*travel;
        float energy=ready?0:ease(f.gust);
        float motion=still?0:energy;
        int body=Collect.BODY[c.companion.who],paper=fanColor(body);
        float gripX=x+r*(.85f+(.025f+.20f*motion)*swing),gripY=y+r*.14f;
        if(!still)gripY-=r*(.02f+.18f*motion)*(float)Math.cos(phase);
        float fanR=r*1.16f,angle=-1.15f+f.sweep()*swing;
        float groundY=y-StarPath.flyerR(L)*bob(c)*travel;
        p.fillEllipse(x,groundY+r*.88f,r*.75f,r*.11f,Glyph.withAlpha(0xFF302045,65));
        if(!c.bonusPrizeWon()) {
            float glow=(ready?.70f:energy*(.55f+.45f*f.effort))*travel;
            glow(p,x,y,r,glow,still?0:phase,paper,ready);
        }
        if(travel>.95f && !c.bonusPrizeWon()) {
            int heat=Glyph.withAlpha(0xFFFFAC62,(int)(18+65*f.effort));
            p.fillEllipse(L.w*.49f,Screens.steamerBaseY(L)+L.unit*.1f,L.w*.16f,L.unit*.24f,heat);
            if(!still && !ready && c.bonusMashing() && f.gust>0) {
                float sx=gripX+(float)Math.cos(angle)*fanR,sy=gripY+(float)Math.sin(angle)*fanR;
                rays(p,L,sx,sy,r,phase,energy,paper);
            }
        }

        if(ready)pointArm(p,c,L,x,y,r,body,prop,false);
        float squash=still?1:1+travel*(ready?.08f*(float)Math.cos(c.companion.clock*9)
                :(.035f+.085f*motion)*swing);
        Painter sprite=new RunCompanion.Squash(p,x,y,squash);
        int mood=c.bonusPrizeWon() || ready?4:energy>.08f?Trinket.WORKING:0;
        Trinket.drawReacting(sprite,c.companion.who,x,y,r,
                ready || c.bonusPrizeWon() || energy<=.08f?c.companion.clock:phase/9f,1f,
                mood,ready?.12f:(.035f+.185f*motion)*swing*travel);
        if(ready) {
            pointArm(p,c,L,x,y,r,body,prop,true);
        } else {
            fan(p,gripX,gripY,fanR,angle,prop,paper);
            p.fillEllipse(gripX-r*.1f,gripY+r*.08f,r*.16f*prop,r*.12f*prop,body);
        }
    }
    private static void glow(Painter p,float x,float y,float r,float strength,float phase,int paper,boolean ready) {
        if(strength<=0)return;
        int color=Glyph.mix(paper,GOLD,ready?.85f:.60f);
        float pulse=.85f+.15f*(float)Math.sin(phase);
        for(int i=3;i>=1;i--)
            p.fillEllipse(x,y,r*(1+i*.23f),r*(.84f+i*.20f),
                    Glyph.withAlpha(color,(int)((15+12*(3-i))*strength*pulse)));
        for(int i=0;i<3;i++) {
            float a=phase*.35f+i*Softbody.TAU/3;
            float px=x+(float)Math.cos(a)*r*1.35f,py=y+(float)Math.sin(a)*r*1.10f;
            p.fillPoly(star(px,py,r*.12f,r*.035f,4,0),Glyph.withAlpha(GOLD,(int)(200*strength)));
        }
    }
    /** An extended index finger aims at the live lid, with the arm behind the body. */
    private static void pointArm(Painter p,GameCore c,Layout L,float x,float y,float r,int body,float fade,boolean hand) {
        float hx=x+r*.85f,hy=y-r*.85f;
        float dx=L.w*.5f-hx,dy=Screens.steamerLidY(c,L)-hy;
        float length=(float)Math.hypot(dx,dy);dx/=length;dy/=length;
        int edge=fadeBy(Glyph.mix(body,0xFF302440,.45f),fade),fill=fadeBy(body,fade);
        if(!hand) {
            float[] arm={x+r*.45f,y+r*.12f,x+r*.80f,y-r*.25f,hx,hy};
            p.polyline(arm,edge,r*.23f);p.polyline(arm,fill,r*.16f);
            return;
        }
        p.line(hx,hy,hx+dx*r*.60f,hy+dy*r*.60f,edge,r*.18f);
        p.line(hx,hy,hx+dx*r*.60f,hy+dy*r*.60f,fill,r*.12f);
        p.fillCircle(hx,hy,r*.23f,edge);p.fillCircle(hx,hy,r*.19f,fill);
        p.fillCircle(hx-dy*r*.18f,hy+dx*r*.18f,r*.10f,fill);
        p.line(hx-dx*r*.05f,hy-dy*r*.05f,hx-dx*r*.13f-dy*r*.10f,
                hy-dy*r*.13f+dx*r*.10f,edge,r*.035f);
    }

    /** Long air trails follow the fan tip up to the basket, fading at both ends. */
    private static void rays(Painter p,Layout L,float sx,float sy,float r,float phase,float strength,int paper) {
        int air=Glyph.mix(paper,INK,.80f);
        for(int ray=0;ray<5;ray++) {
            float ex=L.w*(.34f+(ray-2)*.032f),ey=Screens.steamerBaseY(L)+r*.30f;
            float prevX=sx,prevY=sy;
            for(int j=1;j<=18;j++) {
                float t=j/18f;
                float envelope=(float)Math.sin(Math.PI*t);
                float wiggle=(float)Math.sin(t*Softbody.TAU*1.4f-phase*2+ray*.8f)*r*.13f*envelope;
                float px=sx+(ex-sx)*t+wiggle,py=sy+(ey-sy)*t;
                int alpha=(int)(195*strength*Math.sin(Math.PI*(t-.5f/18f)));
                p.line(prevX,prevY,px,py,Glyph.withAlpha(air,alpha/3),r*.18f);
                p.line(prevX,prevY,px,py,Glyph.withAlpha(air,alpha),r*.065f);
                prevX=px;prevY=py;
            }
        }
    }
    private static void fan(Painter p,float x,float y,float r,float angle,float fade,int color) {
        if(fade<=0)return;
        float start=angle-.95f,step=1.9f/8f;
        for(int i=0;i<8;i++) {
            float a=start+i*step,b=a+step;
            float ax=x+(float)Math.cos(a)*r,ay=y+(float)Math.sin(a)*r;
            float bx=x+(float)Math.cos(b)*r,by=y+(float)Math.sin(b)*r;
            int paper=Glyph.mix(color,INK,i%2==0?.06f:.24f);
            p.fillPoly(new float[]{x,y,ax,ay,bx,by},fadeBy(paper,fade));
            p.line(ax,ay,bx,by,fadeBy(Glyph.mix(color,0xFF302440,.40f),fade),r*.075f);
            p.line(x,y,ax,ay,fadeBy(0xFF8A603E,fade*.75f),r*.025f);
        }
        p.line(x,y,x-r*.24f,y+r*.28f,fadeBy(0xFFDBAF72,fade),r*.10f);
        p.fillCircle(x,y,r*.07f,fadeBy(GOLD,fade));
    }
}
