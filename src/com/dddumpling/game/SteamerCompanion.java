package com.dddumpling.game;

/** A cosmetic helper at the stove; only scored pips feed its recent-effort meter. */
final class SteamerCompanion extends Draw {
    static final float TRAVEL=.72f,RETURN=.55f,IDLE_SPEED=.45f;
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
    static void draw(Painter p,GameCore c,Layout L) {
        float travel=travel(c);
        if(travel<=0 || c.companion.who<0 || !c.buddy.out() || c.onboarding.companionAway(c))return;
        SteamerCompanion f=c.companion.steamer;
        boolean still=c.cloudUi.reduceMotion;
        float x=x(c,L),y=y(c,L),r=radius(c,L),phase=still?0:f.phase;
        float swing=(float)Math.sin(phase),prop=travel*travel;
        float bob=still?0:r*.035f*swing*travel;
        int body=Collect.BODY[c.companion.who];
        p.fillEllipse(x,y+r*.88f,r*.75f,r*.11f,Glyph.withAlpha(0xFF302045,65));
        if(travel>.95f && !c.bonusPrizeWon()) {
            int heat=Glyph.withAlpha(0xFFFFAC62,(int)(18+65*f.effort));
            p.fillEllipse(L.w*.49f,Screens.steamerBaseY(L)+L.unit*.1f,L.w*.16f,L.unit*.24f,heat);
            if(!still)for(int i=0;i<3;i++) {
                float t=(phase/Softbody.TAU+i/3f)%1f;
                float sx=x+r*(1.1f+.1f*i),sy=y-r*.65f;
                float ex=L.w*(.36f+.025f*i),ey=Screens.steamerBaseY(L)+L.unit*.15f;
                float px=sx+(ex-sx)*t,py=sy+(ey-sy)*t;
                int ink=Glyph.withAlpha(GOLD,(int)(100*Math.sin(Math.PI*t)*(.35f+.65f*f.effort)));
                p.polyline(new float[]{px,py,px+r*.09f,py-r*.15f,px+r*.04f,py-r*.3f},ink,r*.045f);
            }
        }
        float gripX=x+r*.75f,gripY=y+r*.18f+bob;
        fan(p,gripX,gripY,r*1.03f,-1.15f+.38f*swing,prop);
        Trinket.drawReacting(p,c.companion.who,x,y+bob,r,c.companion.clock,1f,
                c.bonusPrizeWon()?4:f.effort>.1f?1:0,.10f*swing*travel);
        p.fillEllipse(gripX-r*.1f,gripY+r*.08f,r*.16f*prop,r*.12f*prop,body);
    }
    private static void fan(Painter p,float x,float y,float r,float angle,float fade) {
        if(fade<=0)return;
        float start=angle-.95f,step=1.9f/8f;
        for(int i=0;i<8;i++) {
            float a=start+i*step,b=a+step;
            float ax=x+(float)Math.cos(a)*r,ay=y+(float)Math.sin(a)*r;
            float bx=x+(float)Math.cos(b)*r,by=y+(float)Math.sin(b)*r;
            int paper=Glyph.mix(0xFF72D8C2,0xFFFFF0C2,i%2==0?.15f:.42f);
            p.fillPoly(new float[]{x,y,ax,ay,bx,by},fadeBy(paper,fade));
            p.line(ax,ay,bx,by,fadeBy(0xFFFF9F7D,fade),r*.075f);
            p.line(x,y,ax,ay,fadeBy(0xFF8A603E,fade*.75f),r*.025f);
        }
        p.line(x,y,x-r*.24f,y+r*.28f,fadeBy(0xFFDBAF72,fade),r*.10f);
        p.fillCircle(x,y,r*.07f,fadeBy(GOLD,fade));
    }
}
