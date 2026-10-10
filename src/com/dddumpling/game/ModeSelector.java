package com.dddumpling.game;

/** Saved title choice; locked modes explain their Adventure unlock requirement. */
final class ModeSelector extends Draw {
    static final int ADVENTURE=0, SURVIVAL=1, TIME_ATTACK=2;
    static final String[] NAMES={"ADVENTURE","SURVIVAL","BOSS TIME ATTACK"};
    static final float CHANGE=.32f;
    private static final int SCENE=8, FIRST_START=9;
    static final float SWIPE_DISTANCE=.18f, SWIPE_RETURN=.22f;
    final AdventureTitle adventureTitle=new AdventureTitle();
    int selected=ADVENTURE, previous=ADVENTURE, direction=1;
    float transition=1f, adventureFade=1f, confirmation, unavailable;
    float swipeOffset;
    private float returnFrom,returnTime,transitionFrom;
    int pointer=-1;
    private int pressed;
    private float downX,downY;
    private boolean moved,dragged;

    static boolean implemented(int mode) { return mode>=ADVENTURE && mode<=TIME_ATTACK; }
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
    boolean playable(GameCore c) { return implemented(selected) && unlocked(c,selected)
            && (selected!=TIME_ATTACK || c.timeAttack.playable(c)); }
    boolean adventure() { return selected==ADVENTURE; }
    void restore(int mode) {
        selected=previous=mode>=0 && mode<NAMES.length?mode:ADVENTURE;
        transition=1;adventureFade=adventure()?1:0;
        swipeOffset=returnTime=transitionFrom=0;
    }
    boolean visible(GameCore c) { return entryAvailable(c) && !Starter.hideCase(c); }
    boolean entryAvailable(GameCore c) {
        return c.state==GameCore.TITLE && !c.starting()
                && !c.townOpen && !c.caseOpen && c.caseFade<.01f && !c.storyOpen()
                && !c.settingsOpen && !c.releaseNotes.open && !c.highScoreScreen.open
                && c.returnFade<=0 && c.rosterSceneT<=0 && c.landDiscovery<0
                && !c.onboarding.titleGuide && !c.onboarding.briefing;
    }
    static float y(Layout L) { return L.h*.60f; }
    float titleTop(Layout L) {
        boolean peeking=swipeOffset!=0;
        int next=peeking?(selected+(swipeOffset<0?1:-1)+NAMES.length)%NAMES.length:selected;
        int from=peeking?selected:previous;
        float t=peeking?Math.abs(swipeOffset):transitionFrom+(1-transitionFrom)*panelTravel(transition);
        float boss=(from==TIME_ATTACK?1f-t:0)+(next==TIME_ATTACK?t:0);
        return y(L)-type(L.unit*1.74f)*.75f-L.unit*.9f*boss;
    }
    static float arrowX(Layout L,int direction) { return L.w*(direction<0?.065f:.935f); }
    int hit(GameCore c,Layout L,float x,float y) {
        if(TitleStart.hit(c,L,x,y))return FIRST_START;
        int bossHit=c.timeAttack.hit(c,L,x,y);if(bossHit!=0)return bossHit;
        if(!visible(c) || x<L.padL || x>L.w-L.padR)return 0;
        if(Math.abs(y-y(L))<=L.unit*(selected==TIME_ATTACK?2.7f:2f))
            return x<L.w*.11f?1:x>L.w*.89f?3:2;
        if((selected==SURVIVAL || !unlocked(c,selected)) && Math.abs(y-(LandPicker.cardY(L)+L.unit*1.3f))<L.h*.055f
                && x>L.w*.08f && x<L.w*.92f)return SCENE;
        return 0;
    }
    void select(GameCore c,int next,int direction) {
        if(!visible(c) || c.landTravelFrom>=0 || next<0 || next>=NAMES.length || next==selected)return;
        change(c,next,direction);
    }
    private void change(GameCore c,int next,int direction) {
        previous=selected;selected=next;this.direction=direction;
        if(next==TIME_ATTACK)c.timeAttack.focusUnlocked(c);
        c.preferences.save(c);
        transition=transitionFrom=swipeOffset=returnTime=0;confirmation=unavailable=0;c.titleKeyHint=0;
        c.landPickerDragging=false;
        if(c.sound!=null)c.sound.uiBloop();
    }
    boolean confirm(GameCore c) {
        if(!playable(c)) { reject(c);return false; }
        confirmation=1;
        if(c.sound!=null)c.sound.collect(1);
        return true;
    }
    void start(GameCore c) {
        if(c.landTravelFrom>=0)return;
        if(TitleStart.visible(c))c.beginStart();
        else if(visible(c) && confirm(c))c.beginStart();
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
    private void returnSwipe() {
        if(swipeOffset!=0 && returnTime==0) {returnFrom=swipeOffset;returnTime=SWIPE_RETURN;}
    }
    void cancelTouch() { pointer=-1;pressed=0;moved=dragged=false;returnSwipe(); }
    void update(GameCore c,float dt) {
        adventureTitle.update(c,dt);
        c.timeAttack.slide=Math.min(1,c.timeAttack.slide+dt/TimeAttack.SLIDE_TIME);
        if(returnTime>0) {
            returnTime=Math.max(0,returnTime-dt);
            swipeOffset=returnFrom*panelTravel(returnTime/SWIPE_RETURN);
        }
        if(Starter.hideCase(c)) {selected=previous=ADVENTURE;transition=adventureFade=1;}
        transition=Math.min(1,transition+dt/CHANGE);
        adventureFade+=Math.max(-dt/CHANGE,Math.min(dt/CHANGE,(adventure()?1:0)-adventureFade));
        confirmation=Math.max(0,confirmation-dt/0.65f);
        unavailable=Math.max(0,unavailable-dt/0.65f);
        if(!visible(c) && !TitleStart.visible(c)) {cancelTouch();swipeOffset=returnTime=0;}
    }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(action==3) {boolean owned=pointer>=0;cancelTouch();return owned;}
        if(action==0) {
            cancelTouch();int target=hit(c,L,x,y);if(target==0)return false;
            if(target<4) {swipeOffset=returnTime=0;}
            pointer=id;pressed=target;downX=x;downY=y;return true;
        }
        if(pointer<0)return false;
        if(!visible(c) && !TitleStart.visible(c)) {cancelTouch();return true;}
        if(action==5) {pressed=0;moved=true;returnSwipe();return true;}
        if(id!=pointer)return true;
        if(!moved && (action==2 || action==1 || action==6)) {
            float dx=x-downX,dy=y-downY;
            boolean boss=pressed>=4 && pressed<=TimeAttack.SCENE;
            if(Math.max(Math.abs(dx),Math.abs(dy))>L.unit*.7f)dragged=true;
            if(Math.abs(dy)>L.unit*2 && Math.abs(dy)>Math.abs(dx)) {
                pressed=0;moved=true;returnSwipe();
            } else if(boss && Math.abs(dx)>=L.w*.20f && Math.abs(dx)>Math.abs(dy)*1.25f) {
                int direction=dx<0?1:-1;
                c.timeAttack.choose(c,direction);
                moved=true;
            } else if(pressed>0 && pressed<4 && dragged) {
                swipeOffset=Math.max(-1,Math.min(1,dx/L.w));
                if((action==1 || action==6) && Math.abs(dx)>=L.w*SWIPE_DISTANCE
                        && Math.abs(dx)>Math.abs(dy)*1.25f && c.landTravelFrom<0) {
                    int direction=dx<0?1:-1;
                    float from=Math.abs(swipeOffset);
                    change(c,(selected+direction+NAMES.length)%NAMES.length,direction);
                    transitionFrom=from;moved=true;
                }
            }
        }
        if(action==1 || action==6) {
            int target=pressed;boolean tap=!moved && !dragged && hit(c,L,x,y)==target;
            cancelTouch();
            if(tap && target!=0) {
                if(target==4 || target==6)c.timeAttack.choose(c,target==4?-1:1);
                else if(target==2 || target==5 || target==TimeAttack.SCENE || target==SCENE || target==FIRST_START)start(c);
                else {int direction=target==1?-1:1;select(c,(selected+direction+NAMES.length)%NAMES.length,direction);}
            }
        }
        return true;
    }
    private static void name(Painter p,int mode,float x,float y,float s,float scale,int color) {
        float font=type(s*1.74f)*scale;
        p.text(NAMES[mode],x,y,font,color,Painter.CENTER,true);
    }
    private static void survivalAccent(Painter p,float x,float y,float s,float halfSpan,float clock,float alpha) {
        for(int i=0;i<21;i++) {
            float phase=(clock*.5f+i*.273f)%1f;
            float cx=x+(i-10)*halfSpan/10f,cy=y+s*(-2.6f+phase*3.3f);
            float r=s*.19f,length=s*(.7f+.2f*(i%3));
            int color=fadeBy(Glyph.cycle(i*.13f+clock*.1f),alpha*(float)Math.sin(phase*Math.PI)*.9f);
            p.line(cx,cy,cx,cy+length,color,r*2);
            p.fillCircle(cx,cy,r,color);p.fillCircle(cx,cy+length,r,color);
            p.line(cx+r*.35f,cy,cx+r*.35f,cy+length,fadeBy(INK,alpha*.25f*(float)Math.sin(phase*Math.PI)),r*.45f);
        }
    }
    private static void titleLabel(Painter p,GameCore c,Layout L,int mode,float x,float y,float bump,int color,float fade) {
        float s=L.unit;
        if(mode==TIME_ATTACK) {TimeAttackTitle.draw(p,c,x,y,s,bump,color,fade);return;}
        if(mode==SURVIVAL) {
            float clock=SurvivalDemo.animationClock(c);
            survivalAccent(p,x,y,s,L.w*.4f,clock,fade);
            y-=s*.12f*fade*(.5f+.5f*(float)Math.sin(clock*3.5f));
            bump*=1f+.018f*fade*(float)Math.sin(clock*3.5f);
            if(color==INK)color=Glyph.mix(INK,Glyph.cycle(clock*.10f),.25f*fade);
        }
        if(mode==ADVENTURE)c.modes.adventureTitle.draw(p,c,L,x,y,bump,fade,false);
        name(p,mode,x,y,s,bump,fadeBy(color,fade));
        if(mode==ADVENTURE)c.modes.adventureTitle.draw(p,c,L,x,y,bump,fade,true);
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!visible(c))return;
        boolean peeking=swipeOffset!=0;
        int slideDirection=peeking?(swipeOffset<0?1:-1):direction;
        int arriving=peeking?(selected+slideDirection+NAMES.length)%NAMES.length:selected;
        int departing=peeking?selected:previous;
        float s=L.unit,y=y(L),t=peeking?Math.abs(swipeOffset):transitionFrom+(1-transitionFrom)*panelTravel(transition);
        float bump=1f+.09f*(float)Math.sin(Math.PI*confirmation);
        for(int d=-1;d<=1;d+=2) {
            float x=arrowX(L,d),cy=y-s*.25f;
            p.line(x-d*s*.18f,cy-s*.25f,x+d*s*.18f,cy,GOLD,s*.09f);
            p.line(x+d*s*.18f,cy,x-d*s*.18f,cy+s*.25f,GOLD,s*.09f);
        }
        p.save();p.clipRect(L.w*.09f,y-s*2.8f,L.w*.91f,y+s*1.4f);
        if(peeking || transition<1)titleLabel(p,c,L,departing,L.w*.5f-slideDirection*L.w*t,y,1,INK,1-t);
        float selectedX=L.w*.5f+slideDirection*L.w*(1-t);
        selectedX+=(float)Math.sin(unavailable*20)*s*.15f*unavailable;
        int selectedColor=confirmation>0 || unavailable>0?GOLD:INK;
        titleLabel(p,c,L,arriving,selectedX,y,bump,selectedColor,t);
        p.restore();
        if(!unlocked(c,selected)) {drawLocked(p,c,L);return;}
        c.timeAttack.drawSelector(p,c,L);
        if(selected==SURVIVAL)SurvivalDemo.draw(p,c,L,Math.max(0,1-2*adventureFade));
    }
    private void drawLocked(Painter p,GameCore c,Layout L) {
        float s=L.unit,x=L.w*.5f,y=L.h*.682f;
        int color=unavailable>0?GOLD:INK;
        p.arc(x,y-s*.25f,s*.4f,s*.45f,180,180,color,s*.14f);
        p.fillPoly(new float[]{x-s*.6f,y-s*.25f,x+s*.6f,y-s*.25f,
                x+s*.6f,y+s*.6f,x-s*.6f,y+s*.6f},color);
        p.fillCircle(x,y+s*.12f,s*.10f,BG);
        p.line(x,y+s*.12f,x,y+s*.35f,BG,s*.10f);
        p.text("LOCKED",x,y+s*1.7f,type(s*.7f),color,Painter.CENTER,true);
        p.text(selected==SURVIVAL?"BEAT THE STAGE 5 BOSS":"BEAT YOUR FIRST BOSS",
                x,y+s*3f,type(s*.57f),INK,Painter.CENTER,true);
        p.text("IN ADVENTURE TO UNLOCK",x,y+s*4.05f,type(s*.5f),INK_DIM,Painter.CENTER,true);
    }

}
