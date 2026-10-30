package com.dddumpling.game;

/** Inline display-case wardrobe; earned styles equip immediately and save per squishy. */
final class WardrobeScreen extends Draw {
    boolean open, reward, narrationPaused, exiting;
    float exitT;
    static final float EXIT_TIME=.72f;
    int who, slot, selected;
    private Wardrobe wardrobe;
    int count(int category) {
        int n=0;if(wardrobe!=null)for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && wardrobe.owned[i])n++;
        return n;
    }
    int item(int category,int index) {
        if(index<0)return -1;
        if(wardrobe!=null)for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && wardrobe.owned[i] && index--==0)return i;
        return -1;
    }
    int index(int category,int item) {
        int index=0;for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && wardrobe!=null && wardrobe.owned[i]) {if(i==item)return index;index++;}
        return 0;
    }
    final WardrobeFlight flight=new WardrobeFlight();
    private boolean deferCelebration,pendingCelebration,pendingRemoving;
    private int pendingSlot;
    final AccessoryPicking picking=new AccessoryPicking();
    float pulse=2;
    String error="";
    private boolean gesture;
    private int pointer=-1,pressed,dragItem=-1,wornSlot=-1;
    boolean dragging;
    float dragX,dragY,downX,downY,danceT=3;
    int dance,lastSlot=-1,arrivalSounds;
    float trayT=1,scroll,scrollTarget,lastScroll,scrollStart,scrollSpeed,scrollSampleAge,shakeCooldown;
    float swoosh,swooshAge,swipeDirection=1,scrambleT=2;
    private int feedback,scrollDetent;
    private boolean scrollHaptics;
    static final float SCRAMBLE=1.2f;
    boolean scrolling,ribbonGesture;
    private final java.util.Random outfitRandom=new java.util.Random(21431);
    private final java.util.Random celebrations=new java.util.Random(214);
    static final float FLASH=.40f, DANCE=1.65f;
    void show(GameCore c,int target) {
        if(c.state!=GameCore.TITLE || !Collect.has(c.collected,target))return;
        c.wardrobe.refresh(c);wardrobe=c.wardrobe;who=target;slot=0;
        selected=c.wardrobe.equipped(who,slot);if(selected<0)selected=item(slot,0);
        lastSlot=-1;trayT=0;arrivalSounds=0;scroll=scrollTarget=0;shakeCooldown=.7f;swoosh=0;
        revealSelected();scroll=scrollTarget;flight.clear();pendingCelebration=false;
        open=true;reward=false;narrationPaused=false;exiting=false;exitT=0;error="";pulse=0;danceT=3;scrambleT=2;feedback=0;cancelTouch();
    }
    void close() {flight.clear();pendingCelebration=false;open=reward=narrationPaused=exiting=false;exitT=0;lastSlot=-1;trayT=1;swoosh=0;error="";danceT=3;scrambleT=2;feedback=0;cancelTouch();}
    void back(GameCore c) {
        if(reward){close();return;}
        if(exiting)return;
        if(!open || !c.storyOpen() || !c.caseOpen){c.closeStory();return;}
        exiting=true;exitT=0;flight.clear();pendingCelebration=false;cancelTouch();feedback=0;scrambleT=2;swoosh=0;
        pauseNarration(c);
    }
    float exitProgress(float delay,float duration) {
        if(!exiting)return 0;
        float t=Math.max(0,Math.min(1,(exitT-delay)/duration));return t*t*(3-2*t);
    }
    boolean update(GameCore c,float dt) {
        if(open && c.state!=GameCore.TITLE)close();
        if(exiting) {
            exitT+=dt;
            if(exitT>=EXIT_TIME)c.closeStory();
            return false;
        }
        if(open) {
            if(flight.update(dt))land(c);
            float before=scrambleT;scrambleT+=dt;
            if(!c.paused && !c.settingsOpen && !c.cloudUi.open && !c.updatePrompt.open && !c.releaseNotes.open) {
                for(int beat=0;beat<4;beat++) {
                    float at=.18f+beat*.29f;
                    if(before<at && scrambleT>=at)feedback=Math.max(feedback,beat==3?2:1);
                }
            }
            scrollSampleAge+=dt;
            pulse+=dt;danceT+=dt;trayT+=dt;shakeCooldown=Math.max(0,shakeCooldown-dt);swooshAge+=dt;swoosh=Math.max(0,swoosh-dt*2.4f);
            if(!ribbonGesture)setScroll(scroll+(scrollTarget-scroll)*(1-(float)Math.exp(-15*dt)));
            else scrollSpeed*=(float)Math.exp(-5*dt);
            if(Math.abs(scroll-scrollTarget)<.001f && !ribbonGesture) {scroll=scrollTarget;scrollHaptics=false;}
            if(!reward && !gesture && c.sound!=null)for(int i=0;i<Math.min(4,count(slot));i++) {
                if(trayT>=.48f+i*.055f && (arrivalSounds&(1<<i))==0) {arrivalSounds|=1<<i;c.sound.uiBloop();}
            }
            if(reward){c.ducks.update(c,dt);c.gator.update(c,dt);}
        }
        return open && reward;
    }
    int takeFeedback() {int value=feedback;feedback=0;return value;}
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
        p.save();p.clipRect(0,0,L.w,L.h);p.translate(0,-(trayBottom(c,L)+L.unit*3)*exitProgress(.28f,.28f));
        WardrobeToybox.draw(p,c,L,this);
        if(!error.isEmpty())p.text(error,x,trayTop(L)+trayHeight(L)*.88f,type(s*.55f),INK,Painter.CENTER,false);
        if(dragging) {
            float hx=Storybook.heroX(c,L),hy=Storybook.heroY(c,L),r=Storybook.heroRadius(L);
            if(wornSlot<0) {
                Storybook.glowRings(p,who,hx,hy,r,(c.clock*.6f)%Storybook.GLOW_TIME);
                p.strokeCircle(hx,hy,r*1.55f,0xB8FFE29A,s*.10f);
            }
            AccessoryArt.item(p,dragItem,dragX,dragY-(wornSlot<0?s:0),r*.64f,c.clock);
        }
        flight.draw(p,L,c.clock);
        p.restore();
    }
    float trayBottom(GameCore c,Layout L) {return WardrobeToybox.bottom(L,!error.isEmpty());}
    static float trayTop(Layout L) {return top(L);}
    static float trayHeight(Layout L) {return Math.min(height(L)*.25f,L.unit*12f);}
    static float trayY(Layout L,float f) {return trayTop(L)+trayHeight(L)*(f-.09f);}
    Painter heroPainter(Painter p) {
        int flying=flight.hidesIncoming()?flight.category:-1;
        if((!dragging || wornSlot<0) && flying<0)return p;
        final int hidden=dragging && wornSlot>=0?wornSlot:flying,target=who;
        return new OpacityPainter(p,1) {
            public int accessory(int character,int category) {
                return character==target && category==hidden?-1:super.accessory(character,category);
            }
        };
    }
    void hero(Painter p,GameCore c,float x,float y,float r) {
        whirlwind(p,x,y,r,false);
        float t=danceT-FLASH,env=t>=0&&t<DANCE?(float)Math.sin(Math.PI*t/DANCE):0;
        env*=1-exitProgress(.24f,.48f);
        float beat=t*(dance==1?17:12),sx=(float)Math.sin(beat),hop=Math.abs((float)Math.sin(beat*.5f));
        float dx=env*r*sx*(dance==2?.55f:.28f),dy=-env*r*hop*(dance==1?.65f:.30f);
        float scale=1+env*(float)Math.cos(beat)*.12f;
        float angle=env*sx*(dance==3?.60f:.24f);
        if(dance==2 && env>0)angle=Softbody.TAU*(t/DANCE);
        Painter dancer=env>0?new LinkedPairArt.TurnPainter(p,x+dx,y+dy,angle):p;
        Trinket.drawReacting(dancer,who,env>0?0:x,env>0?0:y,r*scale,c.clock,1,env>0?4:-1,env*sx*.42f);
        whirlwind(p,x,y,r,true);
        if(!exiting && danceT<.72f)FastEnemies.starBurst(p,x,y,r*.48f,Accessories.COLOR[selected>=0?selected:Accessories.item(slot,0)],dance*.75f,danceT/.72f);
    }
    private void whirlwind(Painter p,float x,float y,float r,boolean front) {
        if(scrambleT>=SCRAMBLE)return;
        float u=scrambleT/SCRAMBLE,env=(float)Math.sin(Math.PI*u);
        // Six short paths, split at the silhouette so the ribbons wrap around the body.
        for(int ribbon=0;ribbon<3;ribbon++) {
            float[] path=new float[38];
            for(int i=0;i<19;i++) {
                float a=(front?0:(float)Math.PI)+i*(float)Math.PI/18;
                float wave=(float)Math.sin(a*2-scrambleT*15+ribbon*2.1f);
                float radius=r*(1.10f+ribbon*.17f+u*.30f+wave*.06f);
                path[i*2]=x+(float)Math.cos(a)*radius;
                path[i*2+1]=y+r*(.68f-ribbon*.68f)+(float)Math.sin(a)*r*.28f+wave*r*.12f;
            }
            int color=ribbon==0?0xFF9DEFE0:ribbon==1?0xFFC7B5FF:0xFFFFDF91;
            p.polyline(path,Glyph.withAlpha(color,(int)(32*env)),r*.14f);
            p.polyline(path,Glyph.withAlpha(color,(int)(185*env)),r*.042f);
            p.polyline(path,Glyph.withAlpha(0xFFF3F5FF,(int)(125*env)),r*.012f);
        }
        for(int i=0;i<16;i++) {
            float a=i*2.39996f+scrambleT*(i%2==0?11:8),depth=(float)Math.sin(a);
            if((depth>=0)!=front)continue;
            float radius=r*(1.12f+u*.55f+(i%3)*.12f),sx=x+(float)Math.cos(a)*radius;
            float sy=y+r*(.85f-(i%5)*.42f)+depth*r*.25f-u*r*.35f;
            float size=r*(.018f+(i%3)*.008f)*env;
            p.fillCircle(sx,sy,size*2.5f,Glyph.withAlpha(0xFFD7C2FF,(int)(40*env)));
            p.fillPoly(new float[]{sx,sy-size*2,sx+size*.55f,sy-size*.55f,sx+size*2,sy,
                    sx+size*.55f,sy+size*.55f,sx,sy+size*2,sx-size*.55f,sy+size*.55f,
                    sx-size*2,sy,sx-size*.55f,sy-size*.55f},Glyph.withAlpha(0xFFFFEAB5,(int)(230*env)));
        }
    }
    private void drawReward(Painter p,GameCore c,Layout L) {
        float s=unit(L),x=L.w*.5f;
        p.text("EARNED THROUGH PLAY",x,y(L,.14f),type(s*.56f),INK,Painter.CENTER,false);
        for(int i=0;i<Accessories.COUNT;i++) {
            int columns=4,rows=(Accessories.COUNT+columns-1)/columns;float cx=L.w*(.20f+(i%columns)*.20f),cy=y(L,.23f+(i/columns)*(.49f/rows));
            boolean owned=c.wardrobe.owned[i];if(!owned)continue;float iconR=Math.min(s*1.65f,height(L)*(.49f/rows)*.28f);
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
        return WardrobeToybox.hit(this,L,count(slot),x,yy);
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
        if(item<0 || !c.wardrobe.owned[item] || c.wardrobe.equipped(who,slot)==item)return;
        if(c.wardrobe.equip(c,item,who))celebrate(c,false);
        else error="Could not save. Try again.";
    }
    private void remove(GameCore c) {
        if(c.wardrobe.remove(c,who,slot)) {error="";celebrate(c,true);}
        else error="Could not save. Try again.";
    }
    private void celebrate(GameCore c,boolean removing) {celebrate(c,removing,slot);}
    private void celebrate(GameCore c,boolean removing,int soundSlot) {
        if(deferCelebration){pendingCelebration=true;pendingRemoving=removing;pendingSlot=soundSlot;return;}
        dance=(dance+1+celebrations.nextInt(3))%4;danceT=0;pulse=0;feedback=Math.max(feedback,1);
        c.ducks.react(who,.8f);if(who==Collect.ALLIGATOR_FRIEND)c.gator.react(.8f);
        if(c.sound!=null)c.sound.accessoryChange(soundSlot,removing);
    }
    private void setScroll(float next) {
        scroll=next;
        float bounded=Math.max(0,Math.min(WardrobeToybox.maxScroll(count(slot)),next));
        // Hysteresis avoids buzzing when a finger rests on an item boundary.
        if(scrollHaptics && Math.abs(bounded-scrollDetent)>.60f) {
            scrollDetent=Math.round(bounded);feedback=Math.max(feedback,1);
        }
    }
    static float boundedScroll(float raw,int count) {
        float max=WardrobeToybox.maxScroll(count);
        return raw<0?Math.max(-.42f,raw*.22f):raw>max?max+Math.min(.42f,(raw-max)*.22f):raw;
    }
    static float scrollDestination(float scroll,float speed,int count) {
        return Math.max(0,Math.min(WardrobeToybox.maxScroll(count),Math.round(scroll+speed*.12f)));
    }
    private void revealSelected() {
        scrollHaptics=false;
        int v=index(slot,selected),n=count(slot);
        scrollTarget=Math.max(0,Math.min(WardrobeToybox.maxScroll(n),v-(WardrobeToybox.visible(n)-1)/2));
    }
    static boolean canShuffle(GameCore c) {for(boolean earned:c.wardrobe.owned)if(earned)return true;return false;}
    boolean acceptsShake(GameCore c) {
        return open && !reward && !exiting && !flight.active() && c.storyOpen() && c.story==who && c.state==GameCore.TITLE
                && c.storyT>=Storybook.FLIGHT_TIME && !gesture && shakeCooldown<=0
                && !c.paused && !c.settingsOpen && !c.cloudUi.open && !c.updatePrompt.open
                && !c.releaseNotes.open && !c.preferences.externalBusy && canShuffle(c);
    }
    boolean shake(GameCore c) {
        if(!acceptsShake(c))return false;
        shakeCooldown=1.6f;
        return shuffle(c);
    }
    private boolean shuffle(GameCore c) {
        pauseNarration(c);error="";c.wardrobe.refresh(c);
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
            int on=c.wardrobe.equipped(who,slot);selected=on<0?item(slot,0):on;
            revealSelected();celebrate(c,false,Accessories.FLAIR);scrambleT=0;feedback=0;return true;
        } else error="Could not save. Try again.";
        return false;
    }
    void choose(GameCore c,int action) {
        if(exiting)return;
        if(action==1) {back(c);return;}
        if(reward) {
            if(action==2) {
                int target=Collect.has(c.collected,c.caseIndex)?c.caseIndex:-1;
                for(int i=0;target<0 && i<Collect.COUNT;i++)if(Collect.has(c.collected,i))target=i;
                close();if(target>=0){c.caseIndex=target;c.caseOpen=true;c.openStory();}
            }
            return;
        }
        if(action==10+slot) {feedback=Math.max(feedback,1);return;}
        if(action>=10 && action<10+Accessories.SLOTS || action>=20 && action<20+count(slot)) {
            pauseNarration(c);feedback=Math.max(feedback,1);
        }
        if(action>=10 && action<10+Accessories.SLOTS) {
            lastSlot=slot;lastScroll=scroll;slot=action-10;int on=c.wardrobe.equipped(who,slot);selected=on>=0?on:item(slot,0);error="";
            revealSelected();scroll=scrollTarget;trayT=0;arrivalSounds=0;pulse=0;
        } else if(action>=20 && action<20+count(slot)) {
            int item=item(slot,action-20);selected=item;
            if(c.wardrobe.equipped(who,slot)==item)remove(c);else equip(c,item);
            return;
        }
        if(action!=0 && c.sound!=null)c.sound.uiBloop();
    }
    private void land(GameCore c) {
        if(pendingCelebration){pendingCelebration=false;celebrate(c,pendingRemoving,pendingSlot);}
    }
    private void finishFlight(GameCore c) {flight.clear();land(c);}
    private void flyChoice(GameCore c,Layout L,int action,boolean dragged,float x,float y) {
        if(action<20 || action>=20+count(slot)){choose(c,action);return;}
        finishFlight(c);
        int category=slot,oldItem=c.wardrobe.equipped(who,category),newItem=item(category,action-20);
        picking.capture(c,L);
        float[] old=WardrobeFlight.bounds(picking.targets[category],Storybook.heroX(c,L),Storybook.heroY(c,L),Storybook.heroRadius(L)*.6f);
        float size=dragged?Storybook.heroRadius(L)*.64f:WardrobeToybox.itemR(L,count(slot));
        float[] from={x,y,size,size};
        deferCelebration=true;
        try {if(dragged)equip(c,newItem);else choose(c,action);}finally{deferCelebration=false;}
        int equipped=c.wardrobe.equipped(who,category);
        if(equipped==oldItem){pendingCelebration=false;return;}
        danceT=3;
        picking.capture(c,L);
        float[] to=WardrobeFlight.bounds(picking.targets[category],Storybook.heroX(c,L),Storybook.heroY(c,L),Storybook.heroRadius(L)*.6f);
        float boxSize=WardrobeToybox.categoryR(L)*.68f;
        float[] box={WardrobeToybox.categoryX(L,category),WardrobeToybox.categoryY(L),boxSize,boxSize};
        flight.start(category,equipped,oldItem,from,to,old,box,dragged);
    }
    void cancelTouch() {gesture=dragging=scrolling=ribbonGesture=false;pointer=-1;pressed=0;dragItem=-1;wornSlot=-1;scrollHaptics=false;}
    boolean touch(GameCore c,Layout L,int action,int id,float x,float yy) {
        if(exiting)return true;
        if(!open && !gesture)return false;
        if(action==0) {
            if(flight.active())finishFlight(c);
            if(gesture)return true;
            if(!reward && (yy<trayTop(L) || yy>trayBottom(c,L) || x<L.w*.06f || x>L.w*.94f)) {
                if(!c.storyOpen() || c.storyT<Storybook.FLIGHT_TIME)return false;
                int category=picking.hit(c,L,x,yy);
                if(category<0)return false;
                gesture=true;pointer=id;wornSlot=category;dragItem=c.wardrobe.equipped(who,category);
                downX=dragX=x;downY=dragY=yy;pauseNarration(c);return true;
            }
            gesture=true;pointer=id;pressed=hit(L,x,yy);downX=dragX=x;downY=dragY=yy;
            ribbonGesture=!reward && WardrobeToybox.inRibbon(L,x,yy);scrollStart=scroll;scrollSpeed=0;scrollSampleAge=0;
            scrollDetent=Math.round(Math.max(0,Math.min(WardrobeToybox.maxScroll(count(slot)),scroll)));
            if(!reward && pressed>=20 && pressed<20+count(slot)) {
                pauseNarration(c);
                int item=item(slot,pressed-20);if(c.wardrobe.owned[item])dragItem=item;
            }
        } else if(!gesture)return reward;
        else if(action==2 && id==pointer) {
            float dx=x-downX,dy=yy-downY;dragX=x;dragY=yy;
            if(wornSlot>=0) {
                if(dx*dx+dy*dy>L.unit*L.unit*.25f)dragging=true;
                return true;
            }
            if(ribbonGesture && !dragging && !scrolling && Math.abs(dx)>L.unit*.35f && Math.abs(dx)>Math.abs(dy)*1.12f) {
                scrolling=true;scrollHaptics=true;pressed=0;dragItem=-1;pauseNarration(c);
            }
            if(scrolling) {
                float next=boundedScroll(scrollStart-dx/WardrobeToybox.spacing(L,count(slot)),count(slot));
                scrollSpeed=Math.max(-16,Math.min(16,(next-scroll)/Math.max(1f/120,scrollSampleAge)));scrollSampleAge=0;setScroll(next);swoosh=1;swooshAge=0;swipeDirection=dx<0?-1:1;
            } else {
                if(dragItem>=0 && Math.abs(dy)>L.unit*.35f && Math.abs(dy)>=Math.abs(dx)*.85f)dragging=true;
                if(!dragging && hit(L,x,yy)!=pressed)pressed=0;
            }
        } else if(action==1 && id==pointer) {
            if(wornSlot>=0) {
                int category=wornSlot;
                float dx=x-Storybook.heroX(c,L),dy=yy-Storybook.heroY(c,L),r=Storybook.heroRadius(L)*1.5f;
                boolean off=dragging && dx*dx+dy*dy>r*r && !picking.contains(category,x,yy,L.unit*.45f);
                boolean tapped=!dragging;
                cancelTouch();
                if(open && (off || tapped)) {
                    choose(c,10+category);
                    if(off)remove(c);
                }
                return true;
            }
            if(scrolling) {
                scrollTarget=scrollDestination(scroll,scrollSpeed,count(slot));cancelTouch();scrollHaptics=true;
                if(c.sound!=null)c.sound.uiBloop();return true;
            }
            int choice=pressed==hit(L,x,yy)?pressed:0,item=dragItem;boolean drop=dragging;
            cancelTouch();
            if(open) {
                if(drop) {
                    float dx=x-Storybook.heroX(c,L),dy=yy-Storybook.heroY(c,L),r=Storybook.heroRadius(L)*1.8f;
                    if(dx*dx+dy*dy<=r*r)flyChoice(c,L,20+index(slot,item),true,x,yy-unit(L));
                } else if(choice>=20 && choice<20+count(slot)) {
                    int card=choice-20;
                    flyChoice(c,L,choice,false,WardrobeToybox.itemX(this,L,count(slot),card),WardrobeToybox.itemY(this,L,count(slot),card));
                } else choose(c,choice);
            }
        } else if(action==3){scrollTarget=scrollDestination(scroll,0,count(slot));cancelTouch();}
        else if(action==5) {scrollTarget=scrollDestination(scroll,0,count(slot));pressed=0;dragItem=-1;wornSlot=-1;dragging=scrolling=ribbonGesture=scrollHaptics=false;}
        return true;
    }
}
