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
    static float x(GameCore c,Layout L) {
        return RunCompanion.x(L)+(L.w*.20f-RunCompanion.x(L))*travel(c);
    }
    static float workY(Layout L) {return Screens.steamerBaseY(L)+StarPath.flyerR(L)*3.1f;}
    static float y(GameCore c,Layout L) {
        float t=travel(c),target=workY(L);
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
        boolean still=c.cloudUi.reduceMotion;
        float x=x(c,L),y=y(c,L),r=radius(c,L),phase=still?0:f.phase;
        float swing=(float)Math.sin(phase),prop=travel*travel;
        float energy=still?0:ease(f.gust);
        float bob=still?0:r*(.015f+.06f*energy)*swing*travel;
        int body=Collect.BODY[c.companion.who],paper=fanColor(body);
        float gripX=x+r*(.85f+(.025f+.155f*energy)*swing),gripY=y+r*.14f+bob;
        if(!still)gripY-=r*(.02f+.10f*energy)*(float)Math.cos(phase);
        float fanR=r*1.16f,angle=-1.15f+f.sweep()*swing;
        p.fillEllipse(x,y+r*.88f,r*.75f,r*.11f,Glyph.withAlpha(0xFF302045,65));
        if(travel>.95f && !c.bonusPrizeWon()) {
            int heat=Glyph.withAlpha(0xFFFFAC62,(int)(18+65*f.effort));
            p.fillEllipse(L.w*.49f,Screens.steamerBaseY(L)+L.unit*.1f,L.w*.16f,L.unit*.24f,heat);
            if(!still && c.bonusMashing() && f.gust>0) {
                float sx=gripX+(float)Math.cos(angle)*fanR,sy=gripY+(float)Math.sin(angle)*fanR;
                rays(p,L,sx,sy,r,phase,energy,paper);
            }
        }

        Trinket.drawReacting(p,c.companion.who,x,y+bob,r,c.bonusPrizeWon()?c.companion.clock:phase/9f,1f,
                c.bonusPrizeWon()?4:Trinket.WORKING,(.035f+.185f*energy)*swing*travel);
        fan(p,gripX,gripY,fanR,angle,prop,paper);
        p.fillEllipse(gripX-r*.1f,gripY+r*.08f,r*.16f*prop,r*.12f*prop,body);
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
