package com.dddumpling.game;

/** Saved title choice; unfinished modes can be browsed but never start a run. */
final class ModeSelector extends Draw {
    static final int ADVENTURE=0, SURVIVAL=1, TIME_ATTACK=2;
    static final String[] NAMES={"ADVENTURE","SURVIVAL","BOSS TIME ATTACK"};
    static final float CHANGE=.32f;
    int selected=ADVENTURE, previous=ADVENTURE, direction=1;
    float transition=1f, adventureFade=1f, confirmation, unavailable;
    int pointer=-1;
    private int pressed;
    private float downX,downY;
    private boolean moved;

    static boolean implemented(int mode) { return mode==ADVENTURE || mode==SURVIVAL; } // #150 adds Boss Time Attack.
    static boolean unlocked(GameCore c,int mode) {
        return mode==ADVENTURE || mode==SURVIVAL && bossUnlocked(c,Boss.SLIME)
                || mode==TIME_ATTACK && anyBoss(c);
    }
    static boolean bossUnlocked(GameCore c,int boss) {
        return boss>=0 && boss<Boss.COUNT && Collect.has(c.collected,Collect.BOSS_FIRST+boss);
    }
    private static boolean anyBoss(GameCore c) {
        for(int boss=0;boss<Boss.COUNT;boss++)if(bossUnlocked(c,boss))return true;
        return false;
    }
    static boolean allBossesUnlocked(GameCore c) {
        for(int boss=0;boss<Boss.COUNT;boss++)if(!bossUnlocked(c,boss))return false;
        return true;
    }
    boolean playable(GameCore c) { return implemented(selected) && unlocked(c,selected); }
    boolean adventure() { return selected==ADVENTURE; }
    void restore(int mode) {
        selected=previous=mode>=0 && mode<NAMES.length?mode:ADVENTURE;
        transition=1;adventureFade=adventure()?1:0;
    }
    boolean visible(GameCore c) {
        return c.state==GameCore.TITLE && !c.starting() && !Starter.hideCase(c)
                && !c.townOpen && !c.caseOpen && c.caseFade<.01f && !c.storyOpen()
                && !c.settingsOpen && !c.releaseNotes.open && !c.highScoreScreen.open
                && c.returnFade<=0 && c.rosterSceneT<=0 && c.landDiscovery<0
                && !c.onboarding.titleGuide && !c.onboarding.briefing;
    }
    static float y(Layout L) { return L.h*.60f; }
    static float arrowX(Layout L,int direction) { return L.w*(direction<0?.12f:.88f); }
    int hit(GameCore c,Layout L,float x,float y) {
        if(!visible(c) || Math.abs(y-y(L))>L.unit*1.75f || x<L.padL || x>L.w-L.padR)return 0;
        return x<L.w*.23f?1:x>L.w*.77f?3:2;
    }
    void select(GameCore c,int next,int direction) {
        if(!visible(c) || c.landTravelFrom>=0 || next<0 || next>=NAMES.length || next==selected)return;
        change(c,next,direction);
    }
    private void change(GameCore c,int next,int direction) {
        previous=selected;selected=next;this.direction=direction;
        c.preferences.save(c);
        transition=0;confirmation=unavailable=0;c.titleKeyHint=0;
        c.landPickerDragging=false;
        if(c.sound!=null)c.sound.uiBloop();
    }
    boolean confirm(GameCore c) {
        if(!playable(c)) { reject(c);return false; }
        confirmation=1;
        if(c.sound!=null)c.sound.collect(1);
        return true;
    }
    private void reject(GameCore c) {
        unavailable=1;
        if(c.sound!=null)c.sound.uiBloop();
    }
    boolean allowStart(GameCore c) {
        if(playable(c))return true;
        reject(c);return false;
    }
    boolean back(GameCore c) {
        if(c.state!=GameCore.TITLE || adventure())return false;
        cancelTouch();change(c,ADVENTURE,-1);return true;
    }
    void cancelTouch() { pointer=-1;pressed=0;moved=false; }
    void update(GameCore c,float dt) {
        if(Starter.hideCase(c)) {selected=previous=ADVENTURE;transition=adventureFade=1;}
        transition=Math.min(1,transition+dt/CHANGE);
        adventureFade+=Math.max(-dt/CHANGE,Math.min(dt/CHANGE,(adventure()?1:0)-adventureFade));
        confirmation=Math.max(0,confirmation-dt/0.65f);
        unavailable=Math.max(0,unavailable-dt/0.65f);
        if(!visible(c))cancelTouch();
    }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(action==3) {boolean owned=pointer>=0;cancelTouch();return owned;}
        if(action==0) {
            cancelTouch();int target=hit(c,L,x,y);if(target==0)return false;
            pointer=id;pressed=target;downX=x;downY=y;return true;
        }
        if(pointer<0)return false;
        if(!visible(c)) {cancelTouch();return true;}
        if(action==5) {pressed=0;moved=true;return true;}
        if(id!=pointer)return true;
        if(action==2 && !moved) {
            float dx=x-downX,dy=y-downY;
            if(Math.abs(dy)>L.unit) {pressed=0;moved=true;}
            else if(Math.abs(dx)>L.unit*2) {
                int direction=dx<0?1:-1;
                select(c,(selected+direction+NAMES.length)%NAMES.length,direction);
                moved=true;
            }
        } else if(action==1 || action==6) {
            int target=pressed;boolean tap=!moved && hit(c,L,x,y)==target;
            cancelTouch();
            if(tap && target!=0) {
                if(target==2)confirm(c);
                else {int direction=target==1?-1:1;select(c,(selected+direction+NAMES.length)%NAMES.length,direction);}
            }
        }
        return true;
    }
    private static void name(Painter p,int mode,float x,float y,float s,float scale,int color) {
        float font=type(s*1.16f)*scale;
        if(mode==TIME_ATTACK) {
            p.text("BOSS",x,y-s*.62f,font,color,Painter.CENTER,true);
            p.text("TIME ATTACK",x,y+s*.72f,font,color,Painter.CENTER,true);
        } else p.text(NAMES[mode],x,y,font,color,Painter.CENTER,true);
    }
    private static void survivalAccent(Painter p,float x,float y,float s,float clock,float alpha) {
        for(int side=-1;side<=1;side+=2)for(int i=0;i<4;i++) {
            float phase=(clock*.65f+i*.27f+(side+1)*.13f)%1f;
            float cx=x+side*s*(4.5f+i*.43f),cy=y+s*(-1.6f+phase*2.1f);
            float r=s*.09f,length=s*(.3f+.12f*(i%2));
            int color=fadeBy(Glyph.cycle(i*.17f+clock*.1f),alpha*(float)Math.sin(phase*Math.PI)*.75f);
            p.line(cx,cy,cx,cy+length,color,r*2);
            p.fillCircle(cx,cy,r,color);p.fillCircle(cx,cy+length,r,color);
            p.line(cx+r*.35f,cy,cx+r*.35f,cy+length,fadeBy(INK,alpha*.25f*(float)Math.sin(phase*Math.PI)),r*.45f);
        }
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!visible(c))return;
        float s=L.unit,y=y(L),t=panelTravel(transition);
        float bump=1f+.09f*(float)Math.sin(Math.PI*confirmation);
        p.text("MODE",L.w*.5f,y-s*2.05f,type(s*.35f),INK_DIM,Painter.CENTER,false);
        for(int d=-1;d<=1;d+=2) {
            float x=arrowX(L,d),cy=y-s*.25f;
            p.line(x-d*s*.18f,cy-s*.25f,x+d*s*.18f,cy,GOLD,s*.09f);
            p.line(x+d*s*.18f,cy,x-d*s*.18f,cy+s*.25f,GOLD,s*.09f);
        }
        p.save();p.clipRect(L.w*.20f,y-s*1.85f,L.w*.80f,y+s*.9f);
        if(transition<1)name(p,previous,L.w*.5f-direction*L.w*.6f*t,y,s,1,fadeBy(INK,1-t));
        float selectedX=L.w*.5f+direction*L.w*.6f*(1-t),selectedY=y;
        int selectedColor=confirmation>0?GOLD:INK;
        if(selected==SURVIVAL) {
            survivalAccent(p,selectedX,y,s,c.clock,t);
            selectedY-=s*.12f*t*(.5f+.5f*(float)Math.sin(c.clock*3.5f));
            bump*=1f+.018f*t*(float)Math.sin(c.clock*3.5f);
            if(confirmation<=0)selectedColor=Glyph.mix(INK,Glyph.cycle(c.clock*.10f),.25f*t);
        }
        name(p,selected,selectedX,selectedY,s,bump,selectedColor);
        p.restore();
        String action=playable(c)?(confirmation>0?"SELECTED":"TAP TO CONFIRM"):implemented(selected)?"LOCKED":"COMING SOON";
        p.text(action,L.w*.5f,y+s*1.65f,type(s*.36f),unavailable>0?GOLD:INK_DIM,Painter.CENTER,false);
        if(!adventure()) {
            float alpha=Math.max(0,1-2*adventureFade);
            float x=L.w*.5f+(float)Math.sin(unavailable*20)*s*.15f*unavailable;
            p.text(selected==SURVIVAL?"ENDLESS WAVES":"TIMED BOSS CHALLENGES",x,LandPicker.cardY(L),
                    type(s*.52f),fadeBy(INK,alpha),Painter.CENTER,true);
            p.text(unlocked(c,selected)?(selected==SURVIVAL?Survival.profileName(c.survival.titleProfile(c))+" / TIME + SCORE":"SINGLE BOSS + BOSS RUSH"):selected==SURVIVAL?
                    "DEFEAT SLIME TO UNLOCK":"DEFEAT A BOSS TO UNLOCK",x,LandPicker.cardY(L)+s*1.15f,
                    type(s*.38f),fadeBy(INK_DIM,alpha),Painter.CENTER,false);
        }
    }
}
