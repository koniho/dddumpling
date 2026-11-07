package com.dddumpling.game;

/** A cosmetic helper at the stove; only scored pips feed its recent-effort meter. */
final class SteamerCompanion extends Draw {
    static final float TRAVEL=.72f,RETURN=.55f,IDLE_SPEED=.45f;
    private static final int[] FAN_COLORS={0xFF457AE6,0xFFFFA54D,0xFFEE6480,0xFF8E55D9,0xFF42CAB0};
    float phase,effort;

    void clear() {phase=effort=0;}
    void scored() {effort=Math.min(1,effort+.28f);}
    float speed() {return IDLE_SPEED+3.8f*effort;}
    void update(GameCore c,float dt) {
        if(!scene(c)) {clear();return;}
        phase=(phase+dt*speed()*Softbody.TAU)%Softbody.TAU;
        effort*=(float)Math.exp(-dt*2.2f);
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
        return RunCompanion.x(L)+(L.w*.23f-RunCompanion.x(L))*travel(c);
    }
    static float y(GameCore c,Layout L) {
        float t=travel(c),target=Screens.steamerBaseY(L)+StarPath.flyerR(L)*1.55f;
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
        float bob=still?0:r*.075f*swing*travel;
        int body=Collect.BODY[c.companion.who],paper=fanColor(body);
        float gripX=x+r*(.85f+.18f*swing),gripY=y+r*.14f+bob;
        if(!still)gripY-=r*.12f*(float)Math.cos(phase);
        p.fillEllipse(x,y+r*.88f,r*.75f,r*.11f,Glyph.withAlpha(0xFF302045,65));
        if(travel>.95f && !c.bonusPrizeWon()) {
            int heat=Glyph.withAlpha(0xFFFFAC62,(int)(18+65*f.effort));
            p.fillEllipse(L.w*.49f,Screens.steamerBaseY(L)+L.unit*.1f,L.w*.16f,L.unit*.24f,heat);
            if(!still)for(int i=0;i<5;i++) {
                float t=(phase/Softbody.TAU+i/5f)%1f;
                float sx=gripX+r*.35f,sy=gripY+r*.18f;
                float ex=L.w*.36f,ey=Screens.steamerBaseY(L)+r*.22f;
                float px=sx+(ex-sx)*t,py=sy+(ey-sy)*t;
                float rx=r*(.55f+.70f*t),ry=r*(.24f+.40f*t);
                int alpha=(int)(205*Math.sin(Math.PI*t)*(.65f+.35f*f.effort));
                int air=Glyph.mix(paper,INK,.78f);
                p.arc(px,py,rx,ry,200,140,Glyph.withAlpha(air,alpha/3),r*.32f);
                p.arc(px,py,rx,ry,200,140,Glyph.withAlpha(air,alpha),r*.14f);
            }
        }
        Trinket.drawReacting(p,c.companion.who,x,y+bob,r,c.bonusPrizeWon()?c.companion.clock:phase/9f,1f,
                c.bonusPrizeWon()?4:Trinket.WORKING,.22f*swing*travel);
        fan(p,gripX,gripY,r*1.16f,-1.15f+.88f*swing,prop,paper);
        p.fillEllipse(gripX-r*.1f,gripY+r*.08f,r*.16f*prop,r*.12f*prop,body);
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
