package com.dddumpling.game;

/** A continuous tour of unseen lands, using the swipe traveler's size and arc. */
final class LandDiscovery extends Draw {
    static final float HOLD=.65f;
    private LandDiscovery() {}
    private static float ease(float t) { t=Math.max(0f,Math.min(1f,t));return t*t*(3f-2f*t); }
    static int next(GameCore c) {
        for(int land=1;land<Lands.COUNT;land++)
            if(LandPicker.unlocked(c,land) && (c.landSeen&(1<<land))==0) return land;
        return -1;
    }
    private static void begin(GameCore c,int destination,boolean chained) {
        int from=c.landChoice;
        if(destination!=from) {
            int direction=destination>from ? 1 : -1;
            for(int land=from+direction;land!=destination;land+=direction)
                if(LandPicker.unlocked(c,land)) { destination=land;break; }
        }
        c.landDiscoveryFrom=from;c.landDiscovery=destination;c.landChoice=destination;
        c.landDiscoveryT=0f;c.landDiscoveryChained=chained;
        c.landDiscoveryFresh=(c.landSeen&(1<<destination))==0;
        c.landPickerSlide=LandPicker.slot(c,destination)-LandPicker.slot(c,from);
        c.best=c.landBests[destination];
        if(c.sound!=null) c.sound.landShuffle();
    }
    static float arrival(GameCore c) { return LandPicker.TRAVEL_TIME*(c.landDiscoveryChained ? 1f : .84f); }
    static float walk(GameCore c) {
        float t=c.landDiscoveryT/LandPicker.TRAVEL_TIME;
        return c.landDiscoveryChained ? ease(t) : ease((t-.16f)/.68f);
    }
    static void update(GameCore c,float dt) {
        if(!LandPicker.visible(c) || c.returnFade>0f || c.landTravelFrom>=0) return;
        if(c.landDiscovery<0) {
            int destination=next(c);if(destination<0) return;
            begin(c,destination,false);
        }
        c.landDiscoveryT+=dt;
        c.landPickerSlide=(LandPicker.slot(c,c.landDiscovery)-LandPicker.slot(c,c.landDiscoveryFrom))*(1f-walk(c));
        if(c.landDiscoveryT>=arrival(c) && (c.landSeen&(1<<c.landDiscovery))==0) {
            c.landSeen|=1<<c.landDiscovery;LandPicker.save(c);
        }
        if(c.landDiscoveryT>=arrival(c)+HOLD) {
            int destination=next(c);
            if(destination>=0) begin(c,destination,true);
            else { c.landDiscovery=-1;c.landPickerSlide=c.landWanderT=0f; }
        }
    }
    static float x(GameCore c,Layout L) {
        float start=LandPicker.cardX(c,L,c.landDiscoveryFrom);
        return start+(LandPicker.cardX(c,L,c.landDiscovery)-start)*walk(c);
    }
    static float ground(GameCore c,Layout L) {
        float from=LandPicker.cardY(c,L,c.landDiscoveryFrom);
        return from+(LandPicker.cardY(c,L,c.landDiscovery)-from)*walk(c);
    }
    static float arc(GameCore c,Layout L) {
        float t=walk(c);return LandPicker.walkingDip(t)*LandPicker.iconRadius(c,L);
    }
    static float reveal(GameCore c,Layout L,int land) {
        if(land==0 || (c.landSeen&(1<<land))!=0) return 1f;
        if(land!=c.landDiscovery) return 0f;
        float distance=Math.abs(LandPicker.cardX(c,L,land)-x(c,L));
        return ease(1f-distance/(LandPicker.spacing(c,L)*1.1f));
    }
    static float glow(GameCore c,int land) {
        if(land!=c.landDiscovery || !c.landDiscoveryFresh) return 0f;
        float t=(c.landDiscoveryT-arrival(c))/HOLD;
        return t>0f && t<1f ? (float)Math.sin(t*Math.PI) : 0f;
    }
    static void drawGlow(Painter p,GameCore c,int land,float x,float y,float r) {
        float pulse=glow(c,land);
        if(pulse<=0f) return;
        for(int layer=4;layer>=1;layer--)
            p.fillCircle(x,y,r*(1f+layer*.13f),Glyph.withAlpha(Lands.TINT[land],(int)(26*pulse)));
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(c.landDiscovery<0 || c.landTravelFrom>=0) return;
        float t=c.landDiscoveryT/LandPicker.TRAVEL_TIME;
        boolean finished=next(c)<0;
        float settle=finished ? ease((c.landDiscoveryT-arrival(c))/(HOLD*.65f)) : 0f;
        float grow=c.landDiscoveryChained ? 1f : ease(t/.16f);
        float scale=.42f+.58f*grow*(1f-settle);
        float px=x(c,L),py=ground(c,L)+arc(c,L)+LandPicker.iconRadius(c,L)*.40f;
        LandPicker.drawExplorer(p,c,L,px,py,scale,c.landDiscoveryT,walk(c)<1f);
    }
}
