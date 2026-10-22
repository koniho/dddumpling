package com.dddumpling.game;

/** Player preferences are device-local; mute keeps each channel's chosen volume. */
final class PlayerSettings extends Draw {
    static final int DEFAULT = 100 | (100 << 7);
    static final int MUSIC = 1, EFFECTS = 2, MUSIC_MUTE = 3, EFFECTS_MUTE = 4,
            KIDS = 5, PRIVACY = 6, CLOSE = 7, PLAYER = 8, DEVELOPER = 9, SHARE = 10, RATE = 11, TUTORIALS = 15, CLOUD = 20;
    float tutorialResetUntil;
    static final int ACHIEVEMENTS = 21, SERVICES = 22, SERVICES_PAGE = 2;
    boolean achievementsEnabled;
    boolean achievementRequested;
    // Future settings features own setting and clearing this attention flag.
    boolean notificationPending;
    static final String PUBLIC_ANDROID_URL="https://play.google.com/store/apps/details?id=com.dddumpling.game";
    static String invitation(String url) { return "Come play DDDUMPLING with me! " + url; }
    final ScoreReset scoreReset=new ScoreReset();
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
    void enterPanel() { panelEntrance=0f;panelClosing=false;skinsOpen=false;scoreReset.cancel(); }
    boolean panelMoving() { return panelClosing || panelEntrance<1f; }
    float panelOffset(Layout L) {
        return L.w*(1f-panelTravel(panelEntrance));
    }
    void updatePanel(GameCore c,float elapsed) {
        scoreReset.update(c,elapsed);
        panelEntrance=Math.max(0f,Math.min(1f,panelEntrance+(panelClosing?-elapsed:elapsed)/PANEL_TIME));
        if(panelClosing && panelEntrance==0f) c.closeSettings();
    }
    static void close(GameCore c) {
        if(c.preferences.skinsOpen) {c.preferences.skinsOpen=false;return;}
        if(c.preferences.scoreReset.left>0f) return;
        if(c.preferences.scoreReset.active()) { c.preferences.scoreReset.cancel();return; }
        if(c.settingsOpen) c.preferences.panelClosing=true;
    }
    float music = 1f, effects = 1f;
    boolean musicMuted, effectsMuted, kids;
    int mode=ModeSelector.ADVENTURE;
    int skin=Skins.DEFAULT;
    boolean skinsOpen;
    float skinAt=-10f;

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
        if(bits<0 || bits>0x7FFFFF) bits=DEFAULT;
        music = Math.min(100, bits & 127) / 100f;
        effects = Math.min(100, (bits >>> 7) & 127) / 100f;
        musicMuted = (bits & (1 << 14)) != 0;
        effectsMuted = (bits & (1 << 15)) != 0;
        kids = (bits & (1 << 16)) != 0;
        mode=(bits>>>17)&3;
        skin=Skins.valid((bits>>>19)&15);
        if(mode>=ModeSelector.NAMES.length)mode=ModeSelector.ADVENTURE;
        kidsToggleAt=-10f;
    }
    void save(GameCore c) {
        mode=c.modes.selected;
        if (c.store != null) c.store.savePlayerSettings(Math.round(music * 100) | (Math.round(effects * 100) << 7)
                | (musicMuted ? 1 << 14 : 0) | (effectsMuted ? 1 << 15 : 0) | (kids ? 1 << 16 : 0)
                | (mode << 17) | (skin << 19));
        apply(c);
    }
    void apply(GameCore c) {
        if (c.sound != null) c.sound.volumes(musicMuted ? 0f : music*Launch.musicGain(c), effectsMuted ? 0f : effects);
    }
    static float unit(Layout L) { return Math.min(L.unit, L.h / 48f); }
    static float left(Layout L) { return L.w * .06f; }
    static float right(Layout L) { return L.w * .94f; }
    static float contentL(Layout L) {return left(L)+unit(L);}
    static float contentR(Layout L) {return right(L)-unit(L);}
    static float height(Layout L) { return unit(L)*44f; }
    static float top(Layout L) { return Math.max(L.topSafe, (L.h - height(L))*.5f); }
    static float bottom(Layout L) { return Math.min(L.h-L.padB, top(L)+height(L)); }
    static float row(Layout L, int row) { return top(L) + unit(L)*new float[]{7f,13.5f,20.5f,40.5f}[row]; }
    static int tabCount() {return BuildFlags.DEVELOPER?3:2;}
    static int tabPage(int index) {return index==0?0:index==1?SERVICES_PAGE:1;}
    static float tabEdge(Layout L,int index) {
        float fraction=BuildFlags.DEVELOPER?new float[]{0f,.25f,.72f,1f}[index]:new float[]{0f,.4f,1f}[index];
        return contentL(L)+(contentR(L)-contentL(L))*fraction;
    }
    static float serviceY(Layout L,int index) {return top(L)+unit(L)*(12f+index*14f);}
    static float knobR(GameCore c,Layout L) {return Math.min(L.keyR*c.keyScale(),unit(L)*1.65f);}
    static float trackL(Layout L) { return contentL(L)+unit(L)*1.65f; }
    static float trackR(Layout L) { return contentR(L)-unit(L)*1.65f; }
    static float volumeAt(Layout L, float x) {
        return Math.round(Math.max(0f, Math.min(1f, (x-trackL(L))/(trackR(L)-trackL(L))))*20f)/20f;
    }
    static int hit(GameCore c, Layout L, float x, float y) {
        float s=unit(L), t=top(L);
        if(c.preferences.scoreReset.active()) return c.preferences.scoreReset.hit(L,x,y);
        if (x<left(L) || x>right(L) || y<t || y>bottom(L)) return CLOSE;
        if (x>right(L)-s*2.5f && y<t+s*2.7f) return CLOSE;
        if(c.preferences.skinsOpen) return SkinSettings.hit(L,x,y);
        if(x<contentL(L) || x>contentR(L))return 0;
        if (y>=t+s*3f && y<=t+s*5f) {
            for(int i=0;i<tabCount();i++)if(x<=tabEdge(L,i+1))return i==0?PLAYER:i==1?SERVICES:DEVELOPER;
        }
        if (c.settingsPage==1 && BuildFlags.DEVELOPER) return 0;
        if(c.settingsPage==SERVICES_PAGE) {
            if(Math.abs(y-serviceY(L,0))<s*5.5f && c.cloudUi.enabled)return CLOUD;
            if(Math.abs(y-serviceY(L,1))<s*5.5f && c.preferences.achievementsEnabled)return ACHIEVEMENTS;
            return 0;
        }
        for (int i=0;i<2;i++) {
            if (Math.abs(y-row(L,i))<s && x>contentR(L)-s*4f) return i==0?MUSIC_MUTE:EFFECTS_MUTE;
            if (Math.abs(y-(row(L,i)+s*3f))<knobR(c,L) && x>=contentL(L) && x<=contentR(L))
                return i==0?MUSIC:EFFECTS;
        }
        if (Math.abs(y-row(L,2))<s*1.1f) return KIDS;
        if (Math.abs(y-SkinSettings.buttonY(L))<s*2.25f) return SkinSettings.OPEN;
        if (Math.abs(y-socialY(L))<s*2.1f) return x<L.w*.5f?SHARE:RATE;
        if (Math.abs(y-resetY(L))<s*1.2f) return ScoreReset.OPEN;
        if (Math.abs(y-tutorialY(L))<s*1.2f) return TUTORIALS;
        if (Math.abs(y-row(L,3))<s*1.1f) return PRIVACY;
        return 0;
    }
    static float resetY(Layout L) { return top(L)+unit(L)*34f; }
    static float tutorialY(Layout L) { return resetY(L)+unit(L)*3f; }
    static float socialY(Layout L) { return top(L)+unit(L)*29.5f; }
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
        float s=unit(L), t=top(L), l=left(L), r=right(L),cl=contentL(L),cr=contentR(L);
        p=new OpacityPainter(p,PANEL_OPACITY);
        glassPanel(p,l,t,r,bottom(L),s);
        p.text("SETTINGS",cl,t+s*2f,type(s*.85f),INK,Painter.LEFT,true);
        float cx=cr-s*.4f,cy=t+s*1.5f;
        p.line(cx-s*.4f,cy-s*.4f,cx+s*.4f,cy+s*.4f,INK,s*.1f);
        p.line(cx+s*.4f,cy-s*.4f,cx-s*.4f,cy+s*.4f,INK,s*.1f);
        if(c.preferences.scoreReset.active()) { c.preferences.scoreReset.draw(p,c,L);return; }
        if(c.preferences.skinsOpen) {SkinSettings.draw(p,c,L);return;}
        int tabs=tabCount();
        for(int i=0;i<tabs;i++) {
            float a=tabEdge(L,i),b=tabEdge(L,i+1);
            p.fillRect(a,t+s*3f,b-s*.12f,t+s*5f,tabPage(i)==c.settingsPage?0x554DCEAA:0x18FFFFFF);
            p.text(i==0?"PLAYER":i==1?"PROGRESS":"DEVELOPER",(a+b)*.5f,t+s*4.3f,type(s*.48f),INK,Painter.CENTER,true);
        }
        if(c.settingsPage==1 && BuildFlags.DEVELOPER) { DevSettings.draw(p,c,L); return; }
        if(c.settingsPage==SERVICES_PAGE) {services(p,c,L);return;}
        PlayerSettings a=c.preferences;
        for(int i=0;i<2;i++) {
            float y=row(L,i),v=i==0?a.music:a.effects;
            boolean muted=i==0?a.musicMuted:a.effectsMuted;
            p.text(i==0?"MUSIC":"SOUND EFFECTS",cl,y+s*.25f,type(s*.57f),INK,Painter.LEFT,true);
            p.fillRect(cr-s*4f,y-s*.8f,cr,y+s*.8f,muted?0xBBDF4054:0x554DCEAA);
            p.text(muted?"MUTED":"ON",cr-s*2f,y+s*.25f,type(s*.48f),INK,Painter.CENTER,true);
            float sy=y+s*3f,x=trackL(L)+(trackR(L)-trackL(L))*v;
            p.fillRect(cl,sy-s*.12f,cr,sy+s*.12f,0x55FFFFFF);
            p.fillRect(cl,sy-s*.12f,cl+(cr-cl)*v,sy+s*.12f,GOLD);
            SettingsArt.draw(p,i==0,x,sy,knobR(c,L),v,muted||v==0f,c.clock);
        }
        p.text("KIDS MODE",cl,row(L,2)+s*.25f,type(s*.57f),INK,Painter.LEFT,true);
        SettingsArt.kidsToggle(p,cr-s*2.05f,row(L,2),s,a.kidsPosition(c.clock),a.kidsLift(c.clock));
        SkinSettings.chip(p,c,L);
        social(p,c,L,true);social(p,c,L,false);
        p.fillRect(cl,resetY(L)-s*1.2f,cr,resetY(L)+s*1.2f,0x33F080A8);
        p.text("RESET HIGH SCORES",L.w*.5f,resetY(L)+s*.25f,type(s*.58f),INK,Painter.CENTER,true);
        p.fillRect(cl,tutorialY(L)-s*1.2f,cr,tutorialY(L)+s*1.2f,0x334DCEAA);
        p.text(c.clock<a.tutorialResetUntil?"TUTORIALS RESET":"RESET TUTORIALS",L.w*.5f,tutorialY(L)+s*.25f,type(s*.58f),INK,Painter.CENTER,true);
        p.text("PRIVACY POLICY",L.w*.5f,row(L,3)+s*.25f,type(s*.58f),GOLD,Painter.CENTER,true);
    }
    private static void services(Painter p,GameCore c,Layout L) {
        float s=unit(L),x=L.w*.5f;
        for(int i=0;i<2;i++) {
            float y=serviceY(L,i);
            boolean enabled=i==0?c.cloudUi.enabled:c.preferences.achievementsEnabled;
            p.fillRect(contentL(L),y-s*5.5f,contentR(L),y+s*5.5f,enabled?(i==0?0x334DCEAA:0x33F4C552):0x18FFFFFF);
            Trinket.draw(p,i==0?Collect.STAR_FIRST:Collect.STAR_FIRST+1,x,y-s*2.6f,s*1.7f,c.clock,true,1f);
            p.text(i==0?"CLOUD SAVES":"ACHIEVEMENTS",x,y+s*.25f,type(s*.68f),INK,Painter.CENTER,true);
            String status=i==0?c.cloudUi.label():enabled?"YOUR SQUISHY MILESTONES":"NOT AVAILABLE";
            p.text(status,x,y+s*2f,type(s*.43f),enabled?GOLD:INK_DIM,Painter.CENTER,true);
            String hint=enabled?(i==0?"Tap for sync details":"Tap to view achievements"):
                    i==0?"Your progress stays on this device.":"Keep playing and collecting squishies.";
            p.text(hint,x,y+s*3.7f,type(s*.43f),INK_DIM,Painter.CENTER,false);
        }
    }
    private static void social(Painter p,GameCore c,Layout L,boolean share) {
        float s=unit(L),y=socialY(L),r=s*1.05f;
        float age=c.clock-c.preferences.externalAt;
        float tap=c.preferences.animatedAction==(share?SHARE:RATE) && age<.7f
                ?(float)Math.sin(Math.max(0f,age)/.7f*Math.PI):0f;
        float l=share?contentL(L):L.w*.5f+s*.3f;
        float right=share?L.w*.5f-s*.3f:contentR(L);
        float x=(l+right)*.5f;
        p.fillRect(l,y-s*2.1f,right,y+s*2.1f,share?0x334DCEAA:0x33F4C552);
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
