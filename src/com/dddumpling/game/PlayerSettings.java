package com.dddumpling.game;

/** Player preferences are device-local; mute keeps each channel's chosen volume. */
final class PlayerSettings extends Draw {
    static final int DEFAULT = 100 | (100 << 7);
    static final int MUSIC = 1, EFFECTS = 2, MUSIC_MUTE = 3, EFFECTS_MUTE = 4,
            KIDS = 5, PRIVACY = 6, CLOSE = 7, PLAYER = 8, DEVELOPER = 9;
    float music = 1f, effects = 1f;
    boolean musicMuted, effectsMuted, kids;

    void load(int bits) {
        if(bits<0 || bits>0x1FFFF) bits=DEFAULT;
        music = Math.min(100, bits & 127) / 100f;
        effects = Math.min(100, (bits >>> 7) & 127) / 100f;
        musicMuted = (bits & (1 << 14)) != 0;
        effectsMuted = (bits & (1 << 15)) != 0;
        kids = (bits & (1 << 16)) != 0;
    }
    void save(GameCore c) {
        if (c.store != null) c.store.savePlayerSettings(Math.round(music * 100) | (Math.round(effects * 100) << 7)
                | (musicMuted ? 1 << 14 : 0) | (effectsMuted ? 1 << 15 : 0) | (kids ? 1 << 16 : 0));
        apply(c);
    }
    void apply(GameCore c) {
        if (c.sound != null) c.sound.volumes(musicMuted ? 0f : music, effectsMuted ? 0f : effects);
    }
    static float unit(Layout L) { return Math.min(L.unit, L.h / 38f); }
    static float left(Layout L) { return L.w * .06f; }
    static float right(Layout L) { return L.w * .94f; }
    static float height(Layout L) { return unit(L)*(BuildFlags.DEVELOPER?31f:28f); }
    static float top(Layout L) { return Math.max(L.topSafe, (L.h - height(L))*.5f); }
    static float bottom(Layout L) { return Math.min(L.h-L.padB, top(L)+height(L)); }
    static float row(Layout L, int row) { return top(L) + unit(L)*(new float[]{7f,14.5f,22f,28f}[row]-(BuildFlags.DEVELOPER?0f:3f)); }
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
        if (Math.abs(y-row(L,3))<s*1.1f) return PRIVACY;
        return 0;
    }
    static void open(GameCore c) {
        Pause.release(c);
        c.settingsOpen=true; c.settingsPage=0; c.clearArmed=false;
    }
    static void draw(Painter p, GameCore c, Layout L) {
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
        p.text(a.kids?"ON":"OFF",r-s*3f,row(L,2)+s*.25f,type(s*.55f),a.kids?GOLD:INK_DIM,Painter.CENTER,true);
        p.text("Slower play, shorter words.",trackL(L),row(L,2)+s*1.65f,type(s*.43f),INK_DIM,Painter.LEFT,false);
        p.text("Applies to your next run.",trackL(L),row(L,2)+s*2.85f,type(s*.43f),INK_DIM,Painter.LEFT,false);
        p.text("PRIVACY POLICY",L.w*.5f,row(L,3)+s*.25f,type(s*.58f),GOLD,Painter.CENTER,true);
    }
}
