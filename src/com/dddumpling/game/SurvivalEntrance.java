package com.dddumpling.game;

/** Title send-off and rainbow share one launch clock; combat starts after the sweep. */
final class SurvivalEntrance extends Draw {
    static final float SWEEP=Survival.ENTRANCE_SECONDS;
    static final float RISE=.70f,SETTLE=.32f;
    private SurvivalEntrance() {}

    static boolean selected(GameCore c) {return c.modes.selected==ModeSelector.SURVIVAL;}
    static boolean active(GameCore c) {
        return selected(c) && c.state==GameCore.TITLE && c.launchWho>=0 && c.launchT>0;
    }
    static float progress(GameCore c) {
        if(!active(c))return 0;
        float t=Math.max(0,Math.min(1,(Launch.elapsed(c)-Launch.TIME)/SWEEP));
        return t*t*(3-2*t);
    }
    static float height(Layout L) {return type(L.unit*1.74f);}
    private static float ease(float t) {
        t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);
    }
    static float scale(GameCore c,Layout L) {
        // Leave a small edge margin for the selector's breathing animation on both native fonts.
        float fit=L.w*.98f/(ModeSelector.NAMES[ModeSelector.SURVIVAL].length()*.73f*height(L));
        return 1+(fit-1)*(c.cloudUi.reduceMotion?1:ease(Launch.elapsed(c)/RISE));
    }
    static float baseline(GameCore c,Layout L) {
        float top=Math.max(L.topSafe+height(L)*scale(c,L)*1.2f,L.h*.16f);
        if(c.cloudUi.reduceMotion)return top;
        float age=Launch.elapsed(c),rise=ease(age/RISE);
        float settle=Math.max(0,Math.min(1,(age-RISE)/SETTLE));
        float bounce=-L.unit*rise*(1-ease(settle))*(float)Math.cos(settle*Math.PI*1.5f);
        return top+(ModeSelector.y(L)-top)*(1-rise)+bounce+L.h*1.15f*progress(c);
    }
    static void scenery(Painter p,GameCore c,Layout L) {
        float entry=progress(c);
        if(entry<=0)return;
        if(c.cloudUi.reduceMotion)c.survival.entranceScenery(new OpacityPainter(p,entry),c,L,1);
        else c.survival.entranceScenery(p,c,L,entry);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!active(c))return;
        float fade=c.cloudUi.reduceMotion?1-progress(c):1;
        float bump=1f+.09f*(float)Math.sin(Math.PI*c.modes.confirmation);
        int color=c.modes.confirmation>0 || c.modes.unavailable>0?GOLD:INK;
        // Continue the selector's own lettering and rainbow accents without fading it out.
        p.save();p.clipRect(0,0,L.w,L.h);
        ModeSelector.titleLabel(p,c,L,ModeSelector.SURVIVAL,L.w*.5f,baseline(c,L),bump*scale(c,L),color,fade);
        p.restore();
    }
}
