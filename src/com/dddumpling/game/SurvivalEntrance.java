package com.dddumpling.game;

/** Title send-off and rainbow share one launch clock; combat starts after the sweep. */
final class SurvivalEntrance extends Draw {
    static final float SWEEP=Survival.ENTRANCE_SECONDS;
    private static final String TITLE="SURVIVAL";
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
    static float height(Layout L) {return (L.playRight-L.playLeft)*.92f/(TITLE.length()*1.1f);}
    static float baseline(GameCore c,Layout L) {
        float top=Math.max(L.topSafe+height(L)*1.2f,L.h*.16f);
        return top+(c.cloudUi.reduceMotion?0:L.h*1.15f*progress(c));
    }
    static void scenery(Painter p,GameCore c,Layout L) {
        float entry=progress(c);
        if(entry<=0)return;
        if(c.cloudUi.reduceMotion)c.survival.entranceScenery(new OpacityPainter(p,entry),c,L,1);
        else c.survival.entranceScenery(p,c,L,entry);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!active(c) || c.startFade>0)return;
        float elapsed=Launch.elapsed(c),entry=progress(c),h=height(L);
        float fade=Math.min(1,Math.max(0,(elapsed-GameCore.START_FADE)/.18f))*(c.cloudUi.reduceMotion?1-entry:1);
        float y=baseline(c,L),step=h*1.1f,x=L.w*.5f-step*(TITLE.length()-1)*.5f;
        boolean still=c.cloudUi.reduceMotion;
        p.save();p.clipRect(0,0,L.w,L.h);
        for(int i=0;i<TITLE.length();i++) {
            float phase=still?0:elapsed*4+i*.6f;
            float bob=still?0:(float)Math.sin(phase)*h*.07f*(1-entry);
            float stretch=still?1:1f+.035f*(float)Math.sin(phase+1)*(1-entry);
            TitleBubbleFont.draw(p,TITLE.charAt(i),x+i*step,y+bob,h,Glyph.COLOR[i%Glyph.COUNT],
                    fade,phase,stretch);
        }
        p.restore();
    }
}
