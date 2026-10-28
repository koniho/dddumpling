package com.dddumpling.game;

/** Shared portrait editor. Preview and transfer confirmation never mutate saved outfits. */
final class WardrobeScreen extends Draw {
    boolean open, reward, confirming, previewing=true;
    int who, slot, selected;
    float pulse=2;
    String error="";
    private boolean gesture;
    private int pointer=-1,pressed;
    void show(GameCore c,int target) {
        if(c.state!=GameCore.TITLE || !Collect.has(c.collected,target))return;
        c.wardrobe.refresh(c);who=target;slot=0;
        selected=Math.max(0,c.wardrobe.equipped(who,slot));
        open=true;previewing=true;reward=confirming=false;error="";pulse=0;Pause.release(c);
        if(c.sound!=null)c.sound.uiBloop();
    }
    void close() {open=reward=confirming=false;error="";}
    void back() {if(confirming)confirming=false;else close();}
    boolean update(GameCore c,float dt) {
        if(open && c.state!=GameCore.TITLE)close();
        if(!open && c.wardrobe.pending(c)>=0 && !c.updatePrompt.open && !c.updatePrompt.pending && !c.cloudUi.open && !c.preferences.externalBusy && !c.bossRetry.pending
                && UpdatePrompt.suitable(c)) {
            open=reward=true;confirming=false;pulse=0;
            if(c.sound!=null)c.sound.uiBloop();
            // One batch, even when many older achievements are backfilled together.
            c.wardrobe.acknowledge(c);
            Pause.release(c);
        }
        if(open) {pulse+=dt;c.ducks.update(c,dt);c.gator.update(c,dt);}
        return open;
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
        p.fillRect(0,0,L.w,L.h,0xEC100F22);
        p.fillRect(L.w*.06f,top(L),L.w*.94f,y(L,1),0xFF27243E);
        p.text(reward?"NEW LOOKS TO LOVE":"DRESS UP",x,y(L,.065f),type(s*1.20f),0xFFFFE29A,Painter.CENTER,true);
        p.text("X",L.w*.88f,y(L,.062f),type(s),INK,Painter.CENTER,true);
        if(reward) {drawReward(p,c,L);return;}
        Painter preview=new OpacityPainter(p,1) {
            public int accessory(int target,int category) {
                return target==who && category==slot && previewing?selected:p.accessory(target,category);
            }
        };
        float r=Math.min(L.w*.16f,height(L)*.11f),cy=y(L,.245f);
        float bounce=Math.max(0,1-pulse/1.1f)*(float)Math.sin(pulse*14);
        Storybook.glowRings(p,who,x,cy,r,pulse);
        Trinket.drawReacting(preview,who,x,cy-r*.10f*bounce,r*(1+.06f*bounce),c.clock,1,4,0);
        p.text(Collect.NAME[who],x,y(L,.39f),type(s*.95f),INK,Painter.CENTER,true);
        for(int k=0;k<Accessories.SLOTS;k++) {
            float l=L.w*(.10f+k*.20f),rr=l+L.w*.19f;
            p.fillRect(l,y(L,.425f),rr,y(L,.48f),k==slot?0xFF596477:0xFF39354F);
            p.text(Accessories.SLOT[k],(l+rr)/2,y(L,.46f),type(s*.67f),INK,Painter.CENTER,true);
        }
        for(int v=0;v<Accessories.count(slot);v++) {
            int item=Accessories.item(slot,v);float step=L.w*.80f/Accessories.count(slot);
            float cx=L.w*.10f+step*(v+.5f),left=cx-step*.46f,right=cx+step*.46f;
            p.fillRect(left,y(L,.505f),right,y(L,.69f),item==selected?0xFF494762:0xFF322F49);
            if(item==selected)p.strokePoly(new float[]{left,y(L,.505f),right,y(L,.505f),right,y(L,.69f),left,y(L,.69f)},Accessories.COLOR[item],s*.085f);
            // Icon positions are neutral thumbnails, not extra previews on a hidden character.
            float iy=y(L,.574f),ir=s*(Accessories.count(slot)>3?1.25f:1.55f);
            if(slot==Accessories.HAT)iy+=ir*.25f;
            AccessoryArt.item(p,item,cx,iy,ir,c.clock);
            p.text(Accessories.NAME[item],cx,y(L,.646f),type(s*.56f),INK,Painter.CENTER,true);
            String status=!c.wardrobe.owned[item]?"LOCKED":c.wardrobe.wearer(item)==who?"ON YOU":c.wardrobe.wearer(item)>=0?"WORN":"OWNED";
            p.text(status,cx,y(L,.675f),type(s*.48f),Accessories.COLOR[item],Painter.CENTER,true);
        }
        int wearer=c.wardrobe.wearer(selected);
        boolean owned=c.wardrobe.owned[selected];
        p.text(confirming?"MOVE FROM "+Collect.NAME[wearer>=0?wearer:who]+"?":Accessories.GOAL[selected],x,y(L,.745f),type(s*.70f),INK,Painter.CENTER,true);
        String detail=!owned?progress(c,selected):wearer>=0 && wearer!=who?"Worn by "+Collect.NAME[wearer]:"Yours to mix, match and move";
        p.text(confirming?"Their other accessories stay put":detail,x,y(L,.78f),type(s*.60f),INK_DIM,Painter.CENTER,false);
        String action=confirming?"MOVE HERE":!owned?"KEEP PLAYING TO UNLOCK":wearer==who?"REMOVE":wearer>=0?"TRANSFER":"WEAR IT";
        button(p,L,.855f,action,owned?Accessories.COLOR[selected]:INK_DIM);
        p.text(error.isEmpty()?(confirming?"CANCEL":"BACK TO STORY"):error,x,y(L,.94f),type(s*.68f),INK,Painter.CENTER,true);
    }
    private String progress(GameCore c,int item) {
        int a=Accessories.achievement(item);
        return Achievements.steps(c.progress,a)+" / "+Achievements.target(a)+"  -  preview only";
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
        float f=(yy-top(L))/height(L);
        if(x>L.w*.82f && f<.10f || f>.90f)return 1;
        if(x<L.w*.10f || x>L.w*.90f)return 0;
        if(f>=.82f && f<=.89f)return 2;
        if(reward || confirming)return 0;
        if(f>=.425f && f<=.48f)return 10+Math.min(3,(int)((x/L.w-.10f)/.20f));
        if(f>=.505f && f<=.69f)return 20+Math.max(0,Math.min(Accessories.count(slot)-1,(int)((x/L.w-.10f)/(.80f/Accessories.count(slot)))));
        return 0;
    }
    void choose(GameCore c,int action) {
        if(action==1) {back();return;}
        if(action>=10 && action<14) {
            previewing=true;slot=action-10;int on=c.wardrobe.equipped(who,slot);selected=on>=0?on:Accessories.item(slot,0);pulse=0;error="";
        } else if(action>=20 && action<20+Accessories.count(slot)) {previewing=true;selected=Accessories.item(slot,action-20);pulse=0;error="";}
        else if(action==2) {
            if(reward) {
                int target=Collect.has(c.collected,c.caseIndex)?c.caseIndex:-1;
                for(int i=0;target<0 && i<Collect.COUNT;i++)if(Collect.has(c.collected,i))target=i;
                close();if(target>=0){c.caseIndex=target;c.caseOpen=true;c.openStory();show(c,target);}
                return;
            }
            if(!c.wardrobe.owned[selected])return;
            int wearer=c.wardrobe.wearer(selected);
            if(wearer>=0 && wearer!=who && !confirming) {confirming=true;return;}
            if(c.wardrobe.equip(c,selected,wearer==who?-1:who)) {
                confirming=false;previewing=wearer!=who;pulse=0;error="";
                c.ducks.react(who,.8f);if(who==Collect.ALLIGATOR_FRIEND)c.gator.react(.8f);
            } else error="Could not save. Try again.";
        }
        if(c.sound!=null)c.sound.uiBloop();
    }
    void cancelTouch() {gesture=false;pointer=-1;pressed=0;}
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(!open && !gesture)return false;
        if(action==0) {gesture=true;pointer=id;pressed=hit(L,x,y);}
        else if(action==2 && id==pointer && hit(L,x,y)!=pressed)pressed=0;
        else if(action==1) {
            int choice=id==pointer && pressed==hit(L,x,y)?pressed:0;
            gesture=false;pointer=-1;pressed=0;if(open)choose(c,choice);
        } else if(action==3) {gesture=false;pointer=-1;pressed=0;}
        else if(action==5)pressed=0;
        return true;
    }
}
