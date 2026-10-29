package com.dddumpling.game;

/** Inline display-case wardrobe; earned styles equip immediately and save per squishy. */
final class WardrobeScreen extends Draw {
    boolean open, reward, narrationPaused;
    int who, slot, selected;
    final AccessoryPicking picking=new AccessoryPicking();
    float pulse=2;
    String error="";
    private boolean gesture;
    private int pointer=-1,pressed,dragItem=-1;
    boolean dragging;
    float dragX,dragY,downX,downY,danceT=3;
    int dance,lastSlot=-1,arrivalSounds;
    float trayT=1,scroll,scrollTarget,lastScroll,scrollStart,scrollSpeed,scrollSampleAge,diceT=2;
    float swoosh,swooshAge,swipeDirection=1;
    boolean scrolling,ribbonGesture;
    private final java.util.Random outfitRandom=new java.util.Random(21431);
    private final java.util.Random celebrations=new java.util.Random(214);
    static final float FLASH=.40f, DANCE=1.65f;
    void show(GameCore c,int target) {
        if(c.state!=GameCore.TITLE || !Collect.has(c.collected,target))return;
        c.wardrobe.refresh(c);who=target;slot=0;
        selected=Math.max(0,c.wardrobe.equipped(who,slot));
        lastSlot=-1;trayT=0;arrivalSounds=0;scroll=scrollTarget=0;diceT=2;swoosh=0;
        revealSelected();scroll=scrollTarget;
        open=true;reward=false;narrationPaused=false;error="";pulse=0;danceT=3;cancelTouch();
    }
    void close() {open=reward=narrationPaused=false;lastSlot=-1;trayT=1;swoosh=0;error="";danceT=3;cancelTouch();}
    void back(GameCore c) {if(reward)close();else c.closeStory();}
    boolean update(GameCore c,float dt) {
        if(open && c.state!=GameCore.TITLE)close();
        if(open) {
            scrollSampleAge+=dt;
            pulse+=dt;danceT+=dt;trayT+=dt;diceT+=dt;swooshAge+=dt;swoosh=Math.max(0,swoosh-dt*2.4f);
            if(!ribbonGesture)scroll+=(scrollTarget-scroll)*(1-(float)Math.exp(-15*dt));
            else scrollSpeed*=(float)Math.exp(-5*dt);
            if(Math.abs(scroll-scrollTarget)<.001f && !ribbonGesture)scroll=scrollTarget;
            if(!reward && !gesture && c.sound!=null)for(int i=0;i<Math.min(4,Accessories.count(slot));i++) {
                if(trayT>=.48f+i*.055f && (arrivalSounds&(1<<i))==0) {arrivalSounds|=1<<i;c.sound.uiBloop();}
            }
            if(reward){c.ducks.update(c,dt);c.gator.update(c,dt);}
        }
        return open && reward;
    }
    static float safeTop(Layout L) {return Math.max(L.padT,L.h*.07f);}
    static float safeBottom(Layout L) {return Math.max(L.padB,L.h*.025f);}
    static float outerMargin(Layout L) {return Math.max(L.unit*1.5f,(L.h-safeTop(L)-safeBottom(L))*.035f);}
    static float top(Layout L) {return safeTop(L)+outerMargin(L);}
    static float bottom(Layout L) {return L.h-safeBottom(L)-outerMargin(L);}
    static float height(Layout L) {return bottom(L)-top(L);}
    static float y(Layout L,float f) {return top(L)+height(L)*f;}
    static float unit(Layout L) {return Math.min(L.w/23f,height(L)/38f);}
    static void button(Painter p,Layout L,float f,String label,int color) {
        float yy=y(L,f),hh=height(L)*.035f;
        p.fillRect(L.w*.13f,yy-hh,L.w*.87f,yy+hh,0xFF393354);
        p.strokePoly(new float[]{L.w*.13f,yy-hh,L.w*.87f,yy-hh,L.w*.87f,yy+hh,L.w*.13f,yy+hh},color,unit(L)*.09f);
        p.text(label,L.w*.5f,yy+unit(L)*.34f,type(unit(L)*.86f),color,Painter.CENTER,true);
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!open)return;
        float s=unit(L),x=L.w*.5f;
        if(reward) {
            p.fillRect(0,0,L.w,L.h,0xEC100F22);
            p.fillRect(L.w*.06f,top(L),L.w*.94f,y(L,1),0xFF27243E);
            p.text("NEW LOOKS TO LOVE",x,y(L,.065f),type(s*.95f),0xFFFFE29A,Painter.CENTER,true);
            p.text("X",L.w*.89f,y(L,.035f),type(s),INK,Painter.CENTER,true);
            drawReward(p,c,L);return;
        }
        WardrobeToybox.draw(p,c,L,this);
        boolean owned=c.wardrobe.owned[selected];
        if(!error.isEmpty() || !owned)
            p.text(error.isEmpty()?Accessories.GOAL[selected]:error,x,trayTop(L)+trayHeight(L)*.88f,type(s*.55f),INK,Painter.CENTER,false);
        if(!owned)p.text(progress(c,selected),x,trayTop(L)+trayHeight(L)*.98f,type(s*.50f),INK_DIM,Painter.CENTER,false);
        if(dragging) {
            float hx=Storybook.heroX(c,L),hy=Storybook.heroY(c,L),r=Storybook.heroRadius(L);
            Storybook.glowRings(p,who,hx,hy,r,(c.clock*.6f)%Storybook.GLOW_TIME);
            p.strokeCircle(hx,hy,r*1.55f,0xB8FFE29A,s*.10f);
            AccessoryArt.item(p,dragItem,dragX,dragY-s,Storybook.heroRadius(L)*.64f,c.clock);
        }
    }
    float trayBottom(GameCore c,Layout L) {return WardrobeToybox.bottom(L,!c.wardrobe.owned[selected] || !error.isEmpty());}
    static float trayTop(Layout L) {return top(L);}
    static float trayHeight(Layout L) {return Math.min(height(L)*.25f,L.unit*12f);}
    static float trayY(Layout L,float f) {return trayTop(L)+trayHeight(L)*(f-.09f);}
    void hero(Painter p,GameCore c,float x,float y,float r) {
        float t=danceT-FLASH,env=t>=0&&t<DANCE?(float)Math.sin(Math.PI*t/DANCE):0;
        float beat=t*(dance==1?17:12),sx=(float)Math.sin(beat),hop=Math.abs((float)Math.sin(beat*.5f));
        float dx=env*r*sx*(dance==2?.55f:.28f),dy=-env*r*hop*(dance==1?.65f:.30f);
        float scale=1+env*(float)Math.cos(beat)*.12f;
        float angle=env*sx*(dance==3?.60f:.24f);
        if(dance==2 && env>0)angle=Softbody.TAU*(t/DANCE);
        Painter dancer=env>0?new LinkedPairArt.TurnPainter(p,x+dx,y+dy,angle):p;
        Trinket.drawReacting(dancer,who,env>0?0:x,env>0?0:y,r*scale,c.clock,1,env>0?4:-1,env*sx*.42f);
        if(danceT<.72f)FastEnemies.starBurst(p,x,y,r*.48f,Accessories.COLOR[selected],dance*.75f,danceT/.72f);
    }
    private String progress(GameCore c,int item) {
        int a=Accessories.achievement(item);
        return Achievements.steps(c.progress,a)+" / "+Achievements.target(a)+"  to unlock";
    }
    private void drawReward(Painter p,GameCore c,Layout L) {
        float s=unit(L),x=L.w*.5f;
        p.text("EARNED THROUGH PLAY",x,y(L,.14f),type(s*.56f),INK,Painter.CENTER,false);
        for(int i=0;i<Accessories.COUNT;i++) {
            int columns=4,rows=(Accessories.COUNT+columns-1)/columns;float cx=L.w*(.20f+(i%columns)*.20f),cy=y(L,.23f+(i/columns)*(.49f/rows));
            boolean owned=c.wardrobe.owned[i];float iconR=Math.min(s*1.65f,height(L)*(.49f/rows)*.28f);
            p.fillCircle(cx,cy,iconR,owned?0xFF45425B:0xFF302D44);
            AccessoryArt.item(new OpacityPainter(p,owned?1:.22f),i,cx,cy,iconR*.70f,c.clock);
            if(owned) {
                String name=Accessories.NAME[i];int split=name.indexOf(' ');
                String first=split<0?name:name.substring(0,split),last=split<0?"":name.substring(split+1);
                float label=Math.min(type(s*.46f),L.w*.17f/(Math.max(first.length(),last.length())*.80f));
                p.text(first,cx,cy+iconR+s*.62f,label,Accessories.COLOR[i],Painter.CENTER,true);
                if(!last.isEmpty())p.text(last,cx,cy+iconR+s*1.22f,label,Accessories.COLOR[i],Painter.CENTER,true);
            }
        }
        p.text("CHOOSE A SQUISHY TO DRESS UP",x,y(L,.79f),type(s*.52f),INK,Painter.CENTER,false);
        button(p,L,.855f,"TRY THEM ON",0xFFFFDC93);
        p.text("LATER",x,y(L,.94f),type(s*.70f),INK_DIM,Painter.CENTER,true);
    }
    int hit(Layout L,float x,float yy) {
        if(reward) {
            float f=(yy-top(L))/height(L);
            if(x>L.w*.82f && f<.10f || f>.90f)return 1;
            return x>=L.w*.10f && x<=L.w*.90f && f>=.82f && f<=.89f?2:0;
        }
        return WardrobeToybox.hit(this,L,Accessories.count(slot),x,yy);
    }

    void pauseNarration(GameCore c) {
        if(narrationPaused || !c.storyOpen())return;
        narrationPaused=true;if(c.sound!=null)c.sound.hush();
    }
    void replayNarration(GameCore c) {
        if(!c.storyOpen())return;
        narrationPaused=false;if(c.sound!=null)c.sound.narrate(c.story);
    }
    private void equip(GameCore c,int item) {
        pauseNarration(c);
        selected=item;error="";
        if(!c.wardrobe.owned[item] || c.wardrobe.equipped(who,slot)==item)return;
        if(c.wardrobe.equip(c,item,who))celebrate(c,false);
        else error="Could not save. Try again.";
    }
    private void remove(GameCore c) {
        if(c.wardrobe.remove(c,who,slot)) {error="";celebrate(c,true);}
        else error="Could not save. Try again.";
    }
    private void celebrate(GameCore c,boolean removing) {celebrate(c,removing,slot);}
    private void celebrate(GameCore c,boolean removing,int soundSlot) {
        dance=(dance+1+celebrations.nextInt(3))%4;danceT=0;pulse=0;
        c.ducks.react(who,.8f);if(who==Collect.ALLIGATOR_FRIEND)c.gator.react(.8f);
        if(c.sound!=null)c.sound.accessoryChange(soundSlot,removing);
    }
    static float boundedScroll(float raw,int count) {
        float max=WardrobeToybox.maxScroll(count);
        return raw<0?Math.max(-.42f,raw*.22f):raw>max?max+Math.min(.42f,(raw-max)*.22f):raw;
    }
    static float scrollDestination(float scroll,float speed,int count) {
        return Math.max(0,Math.min(WardrobeToybox.maxScroll(count),Math.round(scroll+speed*.12f)));
    }
    private void revealSelected() {
        int v=Accessories.variant(selected),n=Accessories.count(slot);
        scrollTarget=Math.max(0,Math.min(WardrobeToybox.maxScroll(n),v-(WardrobeToybox.visible(n)-1)/2));
    }
    static boolean canShuffle(GameCore c) {for(boolean earned:c.wardrobe.owned)if(earned)return true;return false;}
    private void shuffle(GameCore c) {
        pauseNarration(c);diceT=0;error="";c.wardrobe.refresh(c);
        if(!canShuffle(c)){if(c.sound!=null)c.sound.uiBloop();return;}
        int[] outfit=new int[Accessories.SLOTS];
        for(int category=0;category<outfit.length;category++) {
            int old=c.wardrobe.equipped(who,category),n=0;outfit[category]=old;
            for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && c.wardrobe.owned[i] && i!=old)n++;
            if(n>0) {
                int pick=outfitRandom.nextInt(n);
                for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && c.wardrobe.owned[i] && i!=old && pick--==0){outfit[category]=i;break;}
            }
        }
        if(c.wardrobe.equipOutfit(c,who,outfit)) {
            int on=c.wardrobe.equipped(who,slot);selected=on<0?Accessories.item(slot,0):on;
            revealSelected();celebrate(c,false,Accessories.FLAIR);
        } else error="Could not save. Try again.";
    }
    void choose(GameCore c,int action) {
        if(action==1) {back(c);return;}
        if(reward) {
            if(action==2) {
                int target=Collect.has(c.collected,c.caseIndex)?c.caseIndex:-1;
                for(int i=0;target<0 && i<Collect.COUNT;i++)if(Collect.has(c.collected,i))target=i;
                close();if(target>=0){c.caseIndex=target;c.caseOpen=true;c.openStory();}
            }
            return;
        }
        if(action==WardrobeToybox.SHUFFLE){shuffle(c);return;}
        if(action>=10 && action<10+Accessories.SLOTS || action>=20 && action<20+Accessories.count(slot))pauseNarration(c);
        if(action>=10 && action<10+Accessories.SLOTS) {
            lastSlot=slot;lastScroll=scroll;slot=action-10;int on=c.wardrobe.equipped(who,slot);selected=on>=0?on:Accessories.item(slot,0);error="";
            revealSelected();scroll=scrollTarget;trayT=0;arrivalSounds=0;pulse=0;
        } else if(action>=20 && action<20+Accessories.count(slot)) {
            int item=Accessories.item(slot,action-20);selected=item;
            if(c.wardrobe.equipped(who,slot)==item)remove(c);else equip(c,item);
            return;
        }
        if(action!=0 && c.sound!=null)c.sound.uiBloop();
    }
    void cancelTouch() {gesture=dragging=scrolling=ribbonGesture=false;pointer=-1;pressed=0;dragItem=-1;}
    boolean touch(GameCore c,Layout L,int action,int id,float x,float yy) {
        if(!open && !gesture)return false;
        if(action==0) {
            if(!reward && (yy<trayTop(L) || yy>trayBottom(c,L) || x<L.w*.06f || x>L.w*.94f))return false;
            gesture=true;pointer=id;pressed=hit(L,x,yy);downX=dragX=x;downY=dragY=yy;
            ribbonGesture=!reward && WardrobeToybox.inRibbon(L,x,yy);scrollStart=scroll;scrollSpeed=0;scrollSampleAge=0;
            if(!reward && pressed>=20 && pressed<20+Accessories.count(slot)) {
                pauseNarration(c);
                int item=Accessories.item(slot,pressed-20);if(c.wardrobe.owned[item])dragItem=item;
            }
        } else if(!gesture)return reward;
        else if(action==2 && id==pointer) {
            float dx=x-downX,dy=yy-downY;dragX=x;dragY=yy;
            if(ribbonGesture && !dragging && !scrolling && Math.abs(dx)>L.unit*.35f && Math.abs(dx)>Math.abs(dy)*1.12f) {
                scrolling=true;pressed=0;dragItem=-1;pauseNarration(c);
            }
            if(scrolling) {
                float next=boundedScroll(scrollStart-dx/WardrobeToybox.spacing(L,Accessories.count(slot)),Accessories.count(slot));
                scrollSpeed=Math.max(-16,Math.min(16,(next-scroll)/Math.max(1f/120,scrollSampleAge)));scrollSampleAge=0;scroll=next;swoosh=1;swooshAge=0;swipeDirection=dx<0?-1:1;
            } else {
                if(dragItem>=0 && Math.abs(dy)>L.unit*.35f && Math.abs(dy)>=Math.abs(dx)*.85f)dragging=true;
                if(!dragging && hit(L,x,yy)!=pressed)pressed=0;
            }
        } else if(action==1 && id==pointer) {
            if(scrolling) {
                scrollTarget=scrollDestination(scroll,scrollSpeed,Accessories.count(slot));cancelTouch();
                if(c.sound!=null)c.sound.uiBloop();return true;
            }
            int choice=pressed==hit(L,x,yy)?pressed:0,item=dragItem;boolean drop=dragging;
            cancelTouch();
            if(open) {
                if(drop) {
                    float dx=x-Storybook.heroX(c,L),dy=yy-Storybook.heroY(c,L),r=Storybook.heroRadius(L)*1.8f;
                    if(dx*dx+dy*dy<=r*r)equip(c,item);
                } else choose(c,choice);
            }
        } else if(action==3){scrollTarget=scrollDestination(scroll,0,Accessories.count(slot));cancelTouch();}
        else if(action==5) {scrollTarget=scrollDestination(scroll,0,Accessories.count(slot));pressed=0;dragItem=-1;dragging=scrolling=ribbonGesture=false;}
        return true;
    }
}
