package com.dddumpling.game;

/** Inline display-case wardrobe; earned styles equip immediately and save per squishy. */
final class WardrobeScreen extends Draw {
    boolean open, reward;
    int who, slot, selected;
    final AccessoryPicking picking=new AccessoryPicking();
    float pulse=2;
    String error="";
    private boolean gesture;
    private int pointer=-1,pressed,dragItem=-1;
    boolean dragging;
    float dragX,dragY,downX,downY,danceT=3;
    int dance;
    private final java.util.Random celebrations=new java.util.Random(214);
    static final float FLASH=.40f, DANCE=1.65f;
    void show(GameCore c,int target) {
        if(c.state!=GameCore.TITLE || !Collect.has(c.collected,target))return;
        c.wardrobe.refresh(c);who=target;slot=0;
        selected=Math.max(0,c.wardrobe.equipped(who,slot));
        open=true;reward=false;error="";pulse=0;danceT=3;cancelTouch();
    }
    void close() {open=reward=false;error="";danceT=3;cancelTouch();}
    void back(GameCore c) {if(reward)close();else c.closeStory();}
    boolean update(GameCore c,float dt) {
        if(open && c.state!=GameCore.TITLE)close();
        if(!open && c.wardrobe.pending(c)>=0 && !c.updatePrompt.open && !c.updatePrompt.pending && !c.cloudUi.open && !c.preferences.externalBusy && !c.bossRetry.pending
                && UpdatePrompt.suitable(c)) {
            open=reward=true;pulse=0;
            if(c.sound!=null)c.sound.uiBloop();
            // One batch, even when many older achievements are backfilled together.
            c.wardrobe.acknowledge(c);
            Pause.release(c);
        }
        if(open) {pulse+=dt;danceT+=dt;if(reward){c.ducks.update(c,dt);c.gator.update(c,dt);}}
        return open && reward;
    }
    static float top(Layout L) {return Math.max(L.topSafe,L.h*.055f);}
    static float height(Layout L) {return L.h-L.padB-top(L)-L.h*.045f;}
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
            p.text("NEW LOOKS TO LOVE",x,y(L,.065f),type(s*1.20f),0xFFFFE29A,Painter.CENTER,true);
            p.text("X",L.w*.88f,y(L,.062f),type(s),INK,Painter.CENTER,true);
            drawReward(p,c,L);return;
        }
        p.fillRect(L.w*.06f,trayY(L,0),L.w*.94f,trayY(L,1),0xFF2A2348);
        p.text("MIX, MATCH, MAKE IT YOURS",x,trayY(L,.08f),type(s*.69f),0xFFFFE29A,Painter.CENTER,true);
        for(int k=0;k<Accessories.SLOTS;k++) {
            float step=L.w*.80f/Accessories.SLOTS,l=L.w*.10f+k*step,rr=l+step-L.w*.01f;
            p.fillRect(l,trayY(L,.12f),rr,trayY(L,.27f),k==slot?0xFF596477:0xFF39354F);
            p.text(Accessories.SLOT[k],(l+rr)/2,trayY(L,.22f),type(s*.60f),INK,Painter.CENTER,true);
        }
        for(int v=0;v<Accessories.count(slot);v++) {
            int item=Accessories.item(slot,v);float step=L.w*.80f/Accessories.count(slot);
            float cx=L.w*.10f+step*(v+.5f),left=cx-step*.46f,right=cx+step*.46f;
            boolean wearing=c.wardrobe.equipped(who,slot)==item;
            p.fillRect(left,trayY(L,.30f),right,trayY(L,.72f),wearing?0xFF494762:0xFF322F49);
            if(wearing)p.strokePoly(new float[]{left,trayY(L,.30f),right,trayY(L,.30f),right,trayY(L,.72f),left,trayY(L,.72f)},Accessories.COLOR[item],s*.085f);
            float iy=trayY(L,.46f),ir=s*(Accessories.count(slot)>3?1.05f:1.25f);
            if(slot==Accessories.HAT)iy+=ir*.25f;
            AccessoryArt.item(new OpacityPainter(p,c.wardrobe.owned[item]?1:.38f),item,cx,iy,ir,c.clock);
            p.text(Accessories.NAME[item],cx,trayY(L,.62f),Math.min(type(s*.56f),(right-left)*.90f/(Accessories.NAME[item].length()*.72f)),INK,Painter.CENTER,true);
            String status=!c.wardrobe.owned[item]?"LOCKED":wearing?"ON YOU":"";
            p.text(status,cx,trayY(L,.69f),type(s*.46f),Accessories.COLOR[item],Painter.CENTER,true);
        }
        boolean owned=c.wardrobe.owned[selected];
        p.text(error.isEmpty()?(owned?"Tap or drag onto your squishy":Accessories.GOAL[selected]):error,x,trayY(L,.81f),type(s*.61f),INK,Painter.CENTER,false);
        if(c.wardrobe.equipped(who,slot)>=0) {
            p.fillRect(L.w*.32f,trayY(L,.85f),L.w*.68f,trayY(L,.99f),0xFF494762);
            p.text("REMOVE",x,trayY(L,.95f),type(s*.62f),INK,Painter.CENTER,true);
        } else if(!owned)p.text(progress(c,selected),x,trayY(L,.95f),type(s*.55f),INK_DIM,Painter.CENTER,false);
        if(dragging) {
            float hx=Storybook.heroX(c,L),hy=Storybook.heroY(c,L),r=Storybook.heroRadius(L);
            Storybook.glowRings(p,who,hx,hy,r,(c.clock*.6f)%Storybook.GLOW_TIME);
            p.strokeCircle(hx,hy,r*1.55f,0xB8FFE29A,s*.10f);
            AccessoryArt.item(p,dragItem,dragX,dragY-s,Storybook.heroRadius(L)*.64f,c.clock);
        }
    }
    static float trayTop(Layout L) {return Math.max(L.topSafe,L.unit*.7f);}
    static float trayY(Layout L,float f) {return trayTop(L)+Math.min((L.h-L.padB-trayTop(L))*.27f,L.unit*12f)*f;}
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
        p.text("Your achievements earned new accessories!",x,y(L,.14f),type(s*.67f),INK,Painter.CENTER,false);
        for(int i=0;i<Accessories.COUNT;i++) {
            int columns=4;float cx=L.w*(.20f+(i%columns)*.20f),cy=y(L,.25f+(i/columns)*.125f);
            boolean owned=c.wardrobe.owned[i];
            p.fillCircle(cx,cy,s*1.65f,owned?0xFF45425B:0xFF302D44);
            AccessoryArt.item(new OpacityPainter(p,owned?1:.22f),i,cx,cy,s*1.15f,c.clock);
            if(owned)p.text(Accessories.NAME[i],cx,cy+s*2.15f,type(s*.52f),Accessories.COLOR[i],Painter.CENTER,true);
        }
        p.text("Pick a squishy in your display case to dress up.",x,y(L,.79f),type(s*.64f),INK,Painter.CENTER,false);
        button(p,L,.855f,"TRY THEM ON",0xFFFFDC93);
        p.text("LATER",x,y(L,.94f),type(s*.70f),INK_DIM,Painter.CENTER,true);
    }
    int hit(Layout L,float x,float yy) {
        if(reward) {
            float f=(yy-top(L))/height(L);
            if(x>L.w*.82f && f<.10f || f>.90f)return 1;
            return x>=L.w*.10f && x<=L.w*.90f && f>=.82f && f<=.89f?2:0;
        }
        if(x<L.w*.10f || x>L.w*.90f)return 0;
        float f=(yy-trayTop(L))/(trayY(L,1)-trayTop(L));
        if(f>=.12f && f<=.27f)return 10+Math.min(Accessories.SLOTS-1,(int)((x/L.w-.10f)/(.80f/Accessories.SLOTS)));
        if(f>=.30f && f<=.72f)return 20+Math.max(0,Math.min(Accessories.count(slot)-1,(int)((x/L.w-.10f)/(.80f/Accessories.count(slot)))));
        if(f>=.85f && f<=.99f && x>=L.w*.32f && x<=L.w*.68f)return 2;
        return 0;
    }
    private void equip(GameCore c,int item) {
        selected=item;error="";
        if(!c.wardrobe.owned[item] || c.wardrobe.equipped(who,slot)==item)return;
        if(c.wardrobe.equip(c,item,who))celebrate(c,false);
        else error="Could not save. Try again.";
    }
    private void remove(GameCore c) {
        if(c.wardrobe.remove(c,who,slot)) {error="";celebrate(c,true);}
        else error="Could not save. Try again.";
    }
    private void celebrate(GameCore c,boolean removing) {
        dance=(dance+1+celebrations.nextInt(3))%4;danceT=0;pulse=0;
        c.ducks.react(who,.8f);if(who==Collect.ALLIGATOR_FRIEND)c.gator.react(.8f);
        if(c.sound!=null)c.sound.accessoryChange(slot,removing);
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
        if(action>=10 && action<10+Accessories.SLOTS) {
            slot=action-10;int on=c.wardrobe.equipped(who,slot);selected=on>=0?on:Accessories.item(slot,0);error="";
        } else if(action>=20 && action<20+Accessories.count(slot)) {
            int item=Accessories.item(slot,action-20);selected=item;
            if(c.wardrobe.equipped(who,slot)==item)remove(c);else equip(c,item);
            return;
        } else if(action==2 && c.wardrobe.equipped(who,slot)>=0) {
            remove(c);
            return;
        }
        if(action!=0 && c.sound!=null)c.sound.uiBloop();
    }
    void cancelTouch() {gesture=dragging=false;pointer=-1;pressed=0;dragItem=-1;}
    boolean touch(GameCore c,Layout L,int action,int id,float x,float yy) {
        if(!open && !gesture)return false;
        if(action==0) {
            if(!reward && (yy<trayTop(L) || yy>trayY(L,1) || x<L.w*.06f || x>L.w*.94f))return false;
            gesture=true;pointer=id;pressed=hit(L,x,yy);downX=dragX=x;downY=dragY=yy;
            if(!reward && pressed>=20 && pressed<20+Accessories.count(slot)) {
                int item=Accessories.item(slot,pressed-20);if(c.wardrobe.owned[item])dragItem=item;
            }
        } else if(!gesture)return reward;
        else if(action==2 && id==pointer) {
            dragX=x;dragY=yy;
            if(dragItem>=0 && Math.hypot(x-downX,yy-downY)>L.unit*.35f)dragging=true;
            if(!dragging && hit(L,x,yy)!=pressed)pressed=0;
        } else if(action==1 && id==pointer) {
            int choice=pressed==hit(L,x,yy)?pressed:0,item=dragItem;boolean drop=dragging;
            cancelTouch();
            if(open) {
                if(drop) {
                    float dx=x-Storybook.heroX(c,L),dy=yy-Storybook.heroY(c,L),r=Storybook.heroRadius(L)*1.8f;
                    if(dx*dx+dy*dy<=r*r)equip(c,item);
                } else choose(c,choice);
            }
        } else if(action==3)cancelTouch();
        else if(action==5) {pressed=0;dragItem=-1;dragging=false;}
        return true;
    }
}
