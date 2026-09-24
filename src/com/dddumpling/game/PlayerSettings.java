package com.dddumpling.game;

/** Player preferences are device-local; mute keeps each channel's chosen volume. */
final class PlayerSettings extends Draw {
    static final int DEFAULT = 100 | (100 << 7);
    static final int MUSIC = 1, EFFECTS = 2, MUSIC_MUTE = 3, EFFECTS_MUTE = 4,
            KIDS = 5, PRIVACY = 6, CLOSE = 7, PLAYER = 8, DEVELOPER = 9, SHARE = 10, RATE = 11;
    static final String PUBLIC_ANDROID_URL="https://play.google.com/store/apps/details?id=com.dddumpling.game";
    static String invitation(String url) { return "Come play DDDUMPLING with me! " + url; }
    int externalAction;
    boolean externalBusy;
    float externalAt=-10f, externalDone=-10f;
    int animatedAction;
    void requestExternal(GameCore c,int action) {
        if(externalBusy || c.clock-externalDone<.5f) return;
        externalBusy=true;externalAction=animatedAction=action;externalAt=c.clock;
        if(c.sound!=null) c.sound.uiBloop();
    }
    int takeExternal(GameCore c) {
        if(externalAction==0 || c.clock-externalAt<.18f) return 0;
        int action=externalAction;externalAction=0;return action;
    }
    void externalFinished(GameCore c) { externalBusy=false;externalDone=c.clock; }
    static final float PANEL_TIME=PANEL_SLIDE_TIME;
    float panelEntrance=1f;
    boolean panelClosing;
    void enterPanel() { panelEntrance=0f;panelClosing=false; }
    boolean panelMoving() { return panelClosing || panelEntrance<1f; }
    float panelOffset(Layout L) {
        return L.w*(1f-panelTravel(panelEntrance));
    }
    void updatePanel(GameCore c,float elapsed) {
        panelEntrance=Math.max(0f,Math.min(1f,panelEntrance+(panelClosing?-elapsed:elapsed)/PANEL_TIME));
        if(panelClosing && panelEntrance==0f) c.closeSettings();
    }
    static void close(GameCore c) { if(c.settingsOpen) c.preferences.panelClosing=true; }
    float music = 1f, effects = 1f;
    boolean musicMuted, effectsMuted, kids;

    static final float KIDS_TOGGLE_TIME=.36f;
    private float kidsToggleAt=-10f, kidsFrom, kidsLiftFrom;
    private float kidsProgress(float clock) {
        return Math.max(0f,Math.min(1f,(clock-kidsToggleAt)/KIDS_TOGGLE_TIME));
    }
    float kidsPosition(float clock) {
        float t=kidsProgress(clock),ease=t*t*(3f-2f*t);
        return kidsFrom+((kids?1f:0f)-kidsFrom)*ease;
    }
    float kidsLift(float clock) {
        float t=kidsProgress(clock),ease=t*t*(3f-2f*t);
        return t>=1f?0f:kidsLiftFrom*(1f-ease)+(float)Math.sin(t*Math.PI)*.28f;
    }
    void toggleKids(GameCore c) {
        // Retarget from the visible pose, including a quick second tap mid-hop.
        kidsFrom=kidsPosition(c.clock);kidsLiftFrom=kidsLift(c.clock);kidsToggleAt=c.clock;
        kids=!kids;save(c);
    }

    void load(int bits) {
        if(bits<0 || bits>0x1FFFF) bits=DEFAULT;
        music = Math.min(100, bits & 127) / 100f;
        effects = Math.min(100, (bits >>> 7) & 127) / 100f;
        musicMuted = (bits & (1 << 14)) != 0;
        effectsMuted = (bits & (1 << 15)) != 0;
        kids = (bits & (1 << 16)) != 0;
        kidsToggleAt=-10f;
    }
    void save(GameCore c) {
        if (c.store != null) c.store.savePlayerSettings(Math.round(music * 100) | (Math.round(effects * 100) << 7)
                | (musicMuted ? 1 << 14 : 0) | (effectsMuted ? 1 << 15 : 0) | (kids ? 1 << 16 : 0));
        apply(c);
    }
    void apply(GameCore c) {
        if (c.sound != null) c.sound.volumes(musicMuted ? 0f : music, effectsMuted ? 0f : effects);
    }
    static float unit(Layout L) { return Math.min(L.unit, L.h / 43f); }
    static float left(Layout L) { return L.w * .06f; }
    static float right(Layout L) { return L.w * .94f; }
    static float height(Layout L) { return unit(L)*(BuildFlags.DEVELOPER?36f:33f); }
    static float top(Layout L) { return Math.max(L.topSafe, (L.h - height(L))*.5f); }
    static float bottom(Layout L) { return Math.min(L.h-L.padB, top(L)+height(L)); }
    static float row(Layout L, int row) { return top(L) + unit(L)*(new float[]{7f,14.5f,22f,33f}[row]-(BuildFlags.DEVELOPER?0f:3f)); }
    static float trackL(Layout L) { return left(L)+L.keyR*Roster.STARTER_SCALE+unit(L)*.4f; }
    static float trackR(Layout L) { return right(L)-L.keyR*Roster.STARTER_SCALE-unit(L)*.4f; }
    static float volumeAt(Layout L, float x) {
        return Math.round(Math.max(0f, Math.min(1f, (x-trackL(L))/(trackR(L)-trackL(L))))*20f)/20f;
    }
    static int hit(GameCore c, Layout L, float x, float y) {
        float s=unit(L), t=top(L);
        if (x<left(L) || x>right(L) || y<t || y>bottom(L)) return CLOSE;
        if (x>right(L)-s*2.5f && y<t+s*2.7f) return CLOSE;
        if (BuildFlags.DEVELOPER && y>=t+s*3f && y<=t+s*5f) {
            if (x<L.w*.5f) return PLAYER;
            return DEVELOPER;
        }
        if (c.settingsPage==1 && BuildFlags.DEVELOPER) return 0;
        for (int i=0;i<2;i++) {
            if (Math.abs(y-row(L,i))<s && x>right(L)-s*5f) return i==0?MUSIC_MUTE:EFFECTS_MUTE;
            if (Math.abs(y-(row(L,i)+s*3f))<L.keyR*c.keyScale() && x>=trackL(L)-L.keyR && x<=trackR(L)+L.keyR)
                return i==0?MUSIC:EFFECTS;
        }
        if (Math.abs(y-row(L,2))<s*1.1f) return KIDS;
        if (Math.abs(y-socialY(L))<s*2.4f) return x<L.w*.5f?SHARE:RATE;
        if (Math.abs(y-row(L,3))<s*1.1f) return PRIVACY;
        return 0;
    }
    static float socialY(Layout L) { return top(L)+unit(L)*(BuildFlags.DEVELOPER?27.5f:24.5f); }
    static void open(GameCore c) {
        c.openSettings();c.settingsPage=0;
    }
    static void draw(Painter p, GameCore c, Layout L) {
        p.save();p.clipRect(0,0,L.w,L.h);
        p.translate(c.preferences.panelOffset(L),0);
        panel(p,c,L);
        p.restore();
    }
    private static void panel(Painter p, GameCore c, Layout L) {
        float s=unit(L), t=top(L), l=left(L), r=right(L);
        p=new OpacityPainter(p,PANEL_OPACITY);
        glassPanel(p,l,t,r,bottom(L),s);
        p.text("SETTINGS",l+s*1.5f,t+s*2f,type(s*.85f),INK,Painter.LEFT,true);
        float cx=r-s*1.3f,cy=t+s*1.5f;
        p.line(cx-s*.4f,cy-s*.4f,cx+s*.4f,cy+s*.4f,INK,s*.1f);
        p.line(cx+s*.4f,cy-s*.4f,cx-s*.4f,cy+s*.4f,INK,s*.1f);
        int tabs=BuildFlags.DEVELOPER?2:0;
        for(int i=0;i<tabs;i++) {
            float a=l+(r-l)*i/tabs,b=l+(r-l)*(i+1)/tabs;
            p.fillRect(a,t+s*3f,b,t+s*5f,i==(BuildFlags.DEVELOPER?c.settingsPage:0)?0x554DCEAA:0x18FFFFFF);
            p.text(i==0?"PLAYER":"DEVELOPER",(a+b)*.5f,t+s*4.3f,type(s*.56f),INK,Painter.CENTER,true);
        }
        if(c.settingsPage==1 && BuildFlags.DEVELOPER) { DevSettings.draw(p,c,L); return; }
        PlayerSettings a=c.preferences;
        for(int i=0;i<2;i++) {
            float y=row(L,i),v=i==0?a.music:a.effects;
            boolean muted=i==0?a.musicMuted:a.effectsMuted;
            p.text(i==0?"MUSIC":"SOUND EFFECTS",trackL(L),y+s*.25f,type(s*.57f),INK,Painter.LEFT,true);
            p.fillRect(r-s*5f,y-s*.8f,r-s,y+s*.8f,muted?0x55444066:0x554DCEAA);
            p.text(muted?"MUTED":"ON",r-s*3f,y+s*.25f,type(s*.48f),INK,Painter.CENTER,true);
            float sy=y+s*3f,x=trackL(L)+(trackR(L)-trackL(L))*v;
            p.fillRect(trackL(L),sy-s*.12f,trackR(L),sy+s*.12f,0x55FFFFFF);
            p.fillRect(trackL(L),sy-s*.12f,x,sy+s*.12f,GOLD);
            SettingsArt.draw(p,i==0,x,sy,L.keyR*c.keyScale(),v,muted||v==0f,c.clock);
            p.text(Math.round(v*100)+"%",r-s*1.5f,sy+L.keyR+s*.8f,type(s*.43f),INK_DIM,Painter.RIGHT,false);
        }
        p.text("KIDS MODE",trackL(L),row(L,2)+s*.25f,type(s*.57f),INK,Painter.LEFT,true);
        SettingsArt.kidsToggle(p,r-s*3.2f,row(L,2),s,a.kidsPosition(c.clock),a.kidsLift(c.clock));
        social(p,c,L,true);social(p,c,L,false);
        p.text("PRIVACY POLICY",L.w*.5f,row(L,3)+s*.25f,type(s*.58f),GOLD,Painter.CENTER,true);
    }
    private static void social(Painter p,GameCore c,Layout L,boolean share) {
        float s=unit(L),x=L.w*(share?.28f:.72f),y=socialY(L),r=s*1.05f;
        float age=c.clock-c.preferences.externalAt;
        float tap=c.preferences.animatedAction==(share?SHARE:RATE) && age<.7f
                ?(float)Math.sin(Math.max(0f,age)/.7f*Math.PI):0f;
        float l=share?left(L)+s*.5f:L.w*.5f+s*.3f;
        float right=share?L.w*.5f-s*.3f:right(L)-s*.5f;
        p.fillRect(l,y-s*2.4f,right,y+s*2.4f,share?0x334DCEAA:0x33F4C552);
        if(share) {
            // Ease the handoff there and back so the four-second loop has no reset snap.
            float give=.5f-.5f*(float)Math.cos(c.clock*Softbody.TAU/4f);
            Skits.share(p,x,y-s*(.35f+tap*.25f),s*1.4f,give,255,Kawaii.CAT);
        } else {
            for(int i=0;i<5;i++) {
                // Stagger each hop from left to right, then rest before the next wave.
                float phase=(c.clock+2.6f-i*.18f)%2.6f;
                float hop=phase<.65f?(float)Math.sin(phase/.65f*Math.PI):0f;
                float radius=i==2?r:r*.50f;
                float sx=x+(i-2)*s*1.25f;
                float sy=y-s*(i==2?.55f:.20f)-s*(hop*.5f+tap*.35f);
                Trinket.draw(p,Collect.STAR_FIRST,sx,sy,radius,c.clock,true,1f);
            }
        }
        p.text(share?"Share App":"Rate your app",x,y+s*1.65f,type(s*.47f),INK,Painter.CENTER,true);
    }

}
