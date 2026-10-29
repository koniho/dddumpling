package com.dddumpling.game;

final class TestAccessories extends Check {
    static GameCore ready() {
        GameCore c=new GameCore(new Mem(),214,true);
        c.collected=Collect.add(Collect.add(0,0),1);
        return c;
    }
    static void grant(GameCore c) {
        String[] keys=new String[Accessories.COUNT];long[] values=new long[keys.length];
        for(int i=0;i<keys.length;i++){keys[i]=Accessories.key("owned",i);values[i]=1;}
        check("wardrobe grant persisted",c.progress.accessories(keys,values,false));c.wardrobe.refresh(c);
    }
    static void all(Layout L) {
        catalog();earnedPicker(L);exit(L);shake();feedback(L);startup();rewards();sharedOutfits();migration();merge();ui(L);developerCategories(L);flair(L);sounds();picking(L);dragOff(L);detail();glitter(L);googly();narration(L);toybox(L);
    }
    private static void openForShake(GameCore c) {
        c.caseOpen=true;c.caseIndex=0;c.openStory();c.storyT=2;c.wardrobe.screen.update(c,1);
    }
    private static void earnedPicker(Layout L) {
        GameCore c=ready();openForShake(c);WardrobeScreen ui=c.wardrobe.screen;
        check("empty category has no hidden locked targets",ui.count(Accessories.HAT)==0 && ui.item(Accessories.HAT,0)==-1 && ui.selected==-1);
        ui.trayT=1;float x=L.w*.5f,y=WardrobeToybox.settledY(L,x);
        check("empty row cannot be tapped",ui.hit(L,x,y)==0);
        ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,x,y+L.unit*4);ui.touch(c,L,1,7,x,y+L.unit*4);
        check("empty row cannot drag a locked accessory",!ui.dragging && c.wardrobe.equipped(0,0)<0);
        int[] earned={2,18,20,21,12};String[] keys=new String[earned.length];long[] values=new long[earned.length];
        for(int i=0;i<earned.length;i++){keys[i]=Accessories.key("owned",earned[i]);values[i]=1;}
        c.progress.accessories(keys,values,false);c.wardrobe.refresh(c);ui.show(c,0);ui.trayT=1;
        check("earned count excludes gaps in catalog",ui.count(Accessories.HAT)==4 && ui.count(Accessories.BOOTS)==0 && ui.count(Accessories.MASK)==1);
        check("earned indexes map to actual item IDs",ui.item(Accessories.HAT,0)==2 && ui.item(Accessories.HAT,1)==18 && ui.item(Accessories.HAT,3)==21);
        ui.choose(c,21);check("filtered second card equips correct saved item",c.wardrobe.equipped(0,Accessories.HAT)==18);
        ui.scroll=ui.scrollTarget=1;ui.trayT=1.7f;ui.pulse=.8f;ui.lastSlot=Accessories.MASK;
        byte[] saved=c.progress.snapshot();ui.choose(c,10+Accessories.HAT);
        check("open box tap preserves scroll and animation",ui.scroll==1 && ui.scrollTarget==1 && ui.trayT==1.7f && ui.pulse==.8f && ui.lastSlot==Accessories.MASK && java.util.Arrays.equals(saved,c.progress.snapshot()));
        ui.scroll=ui.scrollTarget=0;ui.trayT=1;
        float cx=WardrobeToybox.itemX(ui,L,ui.count(ui.slot),0),cy=WardrobeToybox.itemY(ui,L,ui.count(ui.slot),0);
        ui.touch(c,L,0,7,cx,cy);ui.touch(c,L,2,7,Storybook.heroX(c,L),Storybook.heroY(c,L));ui.touch(c,L,1,7,Storybook.heroX(c,L),Storybook.heroY(c,L));
        check("filtered drag equips the visible earned item",c.wardrobe.equipped(0,Accessories.HAT)==2);
        ui.choose(c,10+Accessories.BOOTS);check("empty category has no selected locked placeholder",ui.selected==-1 && ui.scroll==0);
        ui.choose(c,10+Accessories.MASK);ui.choose(c,20);check("single earned mask uses its real ID",c.wardrobe.equipped(0,Accessories.MASK)==12);
        GameCore reload=new GameCore(c.store,214,true);check("filtered outfit persists by stable item ID",reload.wardrobe.equipped(0,Accessories.HAT)==2 && reload.wardrobe.equipped(0,Accessories.MASK)==12);
    }
    private static void exit(Layout L) {
        GameCore c=ready();grant(c);openForShake(c);WardrobeScreen ui=c.wardrobe.screen;
        c.wardrobe.equip(c,0,0);byte[] outfit=c.progress.snapshot();float t=c.storyT;
        ui.back(c);check("wardrobe dismissal starts a modal exit",ui.exiting && c.storyOpen() && ui.open && c.caseOpen);
        ui.update(c,.10f);float age=ui.exitT;
        ui.back(c);c.screenKey(0);ui.choose(c,21);
        check("repeated exit input cannot restart or change outfit",ui.exitT==age && !c.starting() && java.util.Arrays.equals(outfit,c.progress.snapshot()));
        check("exit swallows touches and shake",ui.touch(c,L,0,7,0,0) && !ui.shake(c));
        check("accessories retreat before boxes",WardrobeToybox.arrival(ui,0)<1 && ui.exitProgress(.28f,.28f)==0);
        advance(c,L,.12f);check("story opening freezes during exit",c.storyT==t);
        advance(c,L,.25f);check("exit is still modal while scene fades",c.storyOpen() && ui.exitProgress(.24f,.48f)>0 && !c.starting());
        advance(c,L,.4f);check("exit returns to case and clears transient state",!c.storyOpen() && !ui.open && !ui.exiting && ui.exitT==0 && c.caseOpen);
        check("exit preserves saved outfit",java.util.Arrays.equals(outfit,c.progress.snapshot()));
        c.openStory();ui.back(c);c.startGame();
        check("forced state transition clears unfinished wardrobe exit",!ui.exiting && !c.storyOpen() && !ui.open);
    }
    private static void shake() {
        OutfitShake detector=new OutfitShake();
        check("tilting phone never shuffles",!detector.sample(0,1,0,0) && !detector.sample(1,0,0,100) && !detector.sample(0,0,1,200));
        check("one impact and sustained acceleration are ignored",!detector.sample(2,0,0,300) && !detector.sample(2,0,0,450));
        detector.sample(0,1,0,470);
        check("two separated shake peaks trigger once",detector.sample(-2,0,0,550) && !detector.sample(-2,0,0,600));
        detector.reset();detector.sample(2,0,0,1000);detector.sample(0,1,0,1100);
        check("old peak expires",!detector.sample(-2,0,0,1900));
        detector.reset();detector.sample(2,0,0,2000);detector.reset();
        check("lifecycle reset drops partial gesture",!detector.sample(-2,0,0,2200));
        GameCore c=ready();grant(c);WardrobeScreen ui=c.wardrobe.screen;
        check("shake outside wardrobe ignored",!ui.shake(c));
        openForShake(c);byte[] before=c.progress.snapshot();
        check("shake changes wardrobe",ui.shake(c) && !java.util.Arrays.equals(before,c.progress.snapshot()));
        before=c.progress.snapshot();
        check("shake cooldown prevents duplicate random outfits",!ui.shake(c) && java.util.Arrays.equals(before,c.progress.snapshot()));
        check("shake starts whirlwind",ui.scrambleT==0);
        ui.update(c,.19f);check("scramble haptic beat consumed once",ui.takeFeedback()==1 && ui.takeFeedback()==0);
        ui.update(c,.88f);check("scramble ends with stronger haptic",ui.takeFeedback()==2);
        ui.update(c,2);check("scramble feedback stops",ui.takeFeedback()==0 && ui.scrambleT>WardrobeScreen.SCRAMBLE);
        c.paused=true;check("paused wardrobe ignores shake",!ui.shake(c));c.paused=false;
        c.settingsOpen=true;check("settings ignores shake",!ui.shake(c));c.settingsOpen=false;
        c.cloudUi.open=true;check("cloud dialog ignores shake",!ui.shake(c));c.cloudUi.open=false;
        ui.close();check("closed wardrobe ignores shake",!ui.shake(c) && ui.takeFeedback()==0 && ui.scrambleT>WardrobeScreen.SCRAMBLE);
    }
    private static void feedback(Layout L) {
        GameCore c=ready();grant(c);openForShake(c);WardrobeScreen ui=c.wardrobe.screen;
        check("wardrobe entry is haptically quiet",ui.takeFeedback()==0);
        for(int category=0;category<Accessories.SLOTS;category++) {
            ui.choose(c,10+category);check("category selection ticks once",ui.takeFeedback()==1 && ui.takeFeedback()==0);
            ui.choose(c,20);check("accessory equip ticks once",ui.takeFeedback()==1 && ui.takeFeedback()==0);
            ui.choose(c,20);check("accessory removal ticks once",ui.takeFeedback()==1 && ui.takeFeedback()==0);
        }
        ui.choose(c,10+Accessories.HAT);ui.takeFeedback();ui.trayT=1;ui.scroll=ui.scrollTarget=0;
        int count=Accessories.count(ui.slot);float x=WardrobeToybox.itemX(ui,L,count,1),y=WardrobeToybox.itemY(ui,L,count,1),step=WardrobeToybox.spacing(L,count);
        ui.touch(c,L,0,7,x,y);
        ui.touch(c,L,2,7,x-step*.25f,y);check("small scroll does not buzz",ui.takeFeedback()==0);
        ui.touch(c,L,2,7,x-step*.7f,y);check("scroll past an accessory ticks",ui.takeFeedback()==1);
        ui.touch(c,L,2,7,x-step*.55f,y);ui.touch(c,L,2,7,x-step*.65f,y);
        check("boundary jitter does not repeat ticks",ui.takeFeedback()==0);
        ui.touch(c,L,2,7,x-step*1.7f,y);check("next accessory adds another tick",ui.takeFeedback()==1);
        ui.touch(c,L,3,7,x,y);ui.update(c,.1f);check("cancel stops scrolling haptics",ui.takeFeedback()==0);
        ui.scroll=ui.scrollTarget=0;ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,x-step*.5f,y);ui.takeFeedback();
        ui.touch(c,L,1,7,x-step*.5f,y);ui.update(c,.1f);
        check("released swoosh continues ticking past accessories",ui.takeFeedback()==1);
        ui.choose(c,10+Accessories.GLASSES);ui.takeFeedback();ui.update(c,2);
        check("category reveal does not generate scroll ticks",ui.takeFeedback()==0);
        ui.choose(c,20);ui.close();check("closing clears queued feedback",ui.takeFeedback()==0);
        GameCore locked=ready();openForShake(locked);locked.wardrobe.screen.choose(locked,20);
        check("empty category has no accessory selection feedback",locked.wardrobe.screen.takeFeedback()==0 && locked.wardrobe.equipped(0,0)<0);
    }
    private static void startup() {
        Mem save=new Mem();GameCore c=new GameCore(save,214,true);grant(c);
        check("startup fixture has unseen accessories and an eligible title",c.wardrobe.pending(c)>=0 && UpdatePrompt.suitable(c));
        check("unseen accessories never interrupt title",!c.wardrobe.screen.update(c,.4f)
                && !c.wardrobe.screen.open && !c.wardrobe.screen.reward);
        GameCore reload=new GameCore(save,215,true);
        check("restart preserves rewards without opening popup",reload.wardrobe.owned[0]
                && !reload.wardrobe.screen.update(reload,.4f) && !reload.wardrobe.screen.open);
        reload.collected=Collect.add(reload.collected,0);reload.caseOpen=true;reload.caseIndex=0;reload.openStory();
        check("earned styles remain available in display case",reload.wardrobe.screen.open
                && !reload.wardrobe.screen.reward && reload.wardrobe.equip(reload,0,0));
    }
    private static void narration(Layout L) {
        GameCore c=ready();grant(c);Ear ear=new Ear();c.sound=ear;c.caseOpen=true;c.caseIndex=0;c.openStory();c.storyT=2;
        WardrobeScreen ui=c.wardrobe.screen;
        check("story starts reading",ear.narrations==1);
        ui.choose(c,20);
        check("accessory selection pauses reading",ear.hushes==1 && ui.narrationPaused && c.storyOpen());
        ui.choose(c,21);check("browsing stays quiet",ear.hushes==1 && ear.narrations==1);
        int worn=c.wardrobe.equipped(0,Accessories.HAT);
        Storybook.tap(c,L,L.w*.5f,Storybook.panelBottom(L)-L.unit*2);
        check("story text tap restarts same narration without changing outfit",ear.narrations==2 && ear.narrated==0 && !ui.narrationPaused && c.storyOpen()
                && c.wardrobe.equipped(0,Accessories.HAT)==worn);
        ui.trayT=1;
        ui.touch(c,L,0,1,WardrobeToybox.itemX(ui,L,Accessories.count(ui.slot),0),WardrobeToybox.itemY(ui,L,Accessories.count(ui.slot),0));
        check("starting accessory drag pauses reading",ear.hushes==2 && ui.narrationPaused);
        ui.cancelTouch();check("cancelled drag does not restart reading",ear.narrations==2);
        c.closeStory();c.openStory();check("new story starts narration again",ear.narrations==3 && !ui.narrationPaused);
    }
    private static void googly() {
        int item=Accessories.item(Accessories.GLASSES,3);
        check("googly eyes append without shifting glitter",item==17 && Accessories.ID[item].equals("googly_eyes") && AccessoryGlitter.ITEM==16);
        FlairProbe a=new FlairProbe(),b=new FlairProbe(),later=new FlairProbe();
        AccessoryArt.item(a,item,100,100,80,1);AccessoryArt.item(b,item,100,100,80,1);
        AccessoryArt.item(later,item,100,100,80,1.3f);
        check("googly pupils animate deterministically",a.hash==b.hash && a.hash!=later.hash);
        GameCore c=ready();grant(c);c.wardrobe.equip(c,item,0);
        check("googly eyewear persists",new GameCore(c.store,214,true).wardrobe.equipped(0,Accessories.GLASSES)==item);
    }
    private static void glitter(Layout L) {
        check("glitter append-only ID",Accessories.ID[16].equals("glitter_flair") && Accessories.variant(16)==3);
        GameCore c=ready();c.wardrobe.refresh(c);
        int rose=AccessoryGlitter.gold(0x99FF7D99),mint=AccessoryGlitter.gold(0xFF8FD9A0);
        check("glitter adds gold without changing opacity",rose>>>24==0x99 && ((rose>>16)&255)>(rose&255) && ((mint>>8)&255)>(mint&255));
        check("glitter preserves dark facial ink",AccessoryGlitter.gold(0xFF302B45)==0xFF302B45);
        float[] cut=AccessoryGlitter.clip(new float[]{-10,-10,10,-10,10,10,-10,10},0,true);
        boolean clipped=cut.length>=6;for(int i=0;i<cut.length;i+=2)clipped &= cut[i]+cut[i+1]*.42f>=-.001f;
        check("golden sweep stays inside its diagonal half plane",clipped);
        FlairProbe still=new FlairProbe(),sweep=new FlairProbe(),same=new FlairProbe();
        AccessoryGlitter g0=new AccessoryGlitter(still,0,0,100,3.5f,1),g1=new AccessoryGlitter(sweep,0,0,100,1.4f,1),g2=new AccessoryGlitter(same,0,0,100,1.4f,1);
        g0.fillEllipse(0,0,80,70,0xFFFFDD88);g1.fillEllipse(0,0,80,70,0xFFFFDD88);g2.fillEllipse(0,0,80,70,0xFFFFDD88);
        check("golden sweep is bounded and deterministic",sweep.calls>still.calls && sweep.calls<=still.calls+3 && sweep.hash==same.hash);
        // Surface sampling must leave holes and empty corners untouched.
        FlairProbe p=new FlairProbe();AccessoryGlitter g=new AccessoryGlitter(p,0,0,100,1,1);
        g.fillEllipse(0,0,70,60,0xFFFFDD88);int before=p.calls;g.finish();
        check("body glitter emits on painted surface",p.calls>before);
        FlairProbe empty=new FlairProbe();new AccessoryGlitter(empty,0,0,100,1,1).finish();
        check("body glitter does not emit off the body",empty.calls==0);
        check("surface polygon excludes outside",AccessoryGlitter.inside(new float[]{0,0,10,0,0,10},2,2)
                && !AccessoryGlitter.inside(new float[]{0,0,10,0,0,10},8,8));
        grant(c);c.wardrobe.equip(c,AccessoryGlitter.ITEM,0);
        GameCore reload=new GameCore(c.store,214,true);
        check("glitter outfit persists",reload.wardrobe.equipped(0,Accessories.FLAIR)==AccessoryGlitter.ITEM);
    }
    private static void detail() {
        for(int item=0;item<Accessories.COUNT;item++) {
            FlairProbe small=new FlairProbe(),large=new FlairProbe();
            AccessoryArt.item(small,item,0,0,24,1);AccessoryArt.item(large,item,0,0,140,1);
            check("small accessory skips close-up decoration "+item,small.calls>0 && small.calls<large.calls);
        }
        for(final float width:new float[]{320,640,1080}) {
            Painter p=new FlairProbe(){public float accessorySize(float r){return r/width;}};
            check("detail follows viewport not pixel density",AccessoryArt.detail(p,width*.06f)==0 && AccessoryArt.detail(p,width*.13f)==1);
            float mid=AccessoryArt.detail(p,width*.09f);
            check("detail blends between view sizes",mid>0 && mid<1);
            Painter fitted=new BossScreen.PortraitFit(p,0,0,0,0,.25f);
            check("portrait uses projected accessory size",AccessoryArt.detail(fitted,width*.20f)==0);
            fitted=new BossScreen.PortraitFit(p,0,0,0,0,3);
            check("enlarged portrait restores decoration",AccessoryArt.detail(fitted,width*.05f)==1);
        }
    }
    private static void picking(Layout L) {
        GameCore c=ready();grant(c);c.caseOpen=true;
        int[] friends=AccessoryPlacementPreview.types();
        for(int skin=0;skin<Skins.COUNT;skin++)for(int who:friends) {
            c.collected=Collect.add(c.collected,who);c.closeStory();c.caseIndex=who;c.openStory();c.storyT=2;c.storyPulse=2;c.preferences.skin=skin;
            WardrobeScreen ui=c.wardrobe.screen;
            for(int slot=0;slot<Accessories.SLOTS;slot++) {
                int item=Accessories.item(slot,0);c.wardrobe.equip(c,item,who);
                for(float age:new float[]{3f,.8f}) {
                    ui.danceT=age;ui.dance=2;ui.picking.capture(c,L);
                    float[] q=ui.picking.targets[slot];
                    check("attachment target exists for family/skin",q!=null);
                    float u=slot==Accessories.FLAIR?.95f:.5f,v=slot==Accessories.MASK?.84f:.5f;
                    float x=q[0]+(q[2]-q[0])*u+(q[6]-q[0])*v,y=q[1]+(q[3]-q[1])*u+(q[7]-q[1])*v;
                    byte[] before=c.progress.snapshot();Storybook.tap(c,L,x,y);
                    check("worn item selects its category through dance",ui.slot==slot && ui.selected==item && c.storyOpen());
                    check("worn item tap does not unequip or edit save",java.util.Arrays.equals(before,c.progress.snapshot()));
                }
                c.wardrobe.remove(c,who,slot);
            }
        }
        c.closeStory();c.caseIndex=0;c.openStory();c.storyT=2;c.storyPulse=2;
        c.wardrobe.equip(c,Accessories.item(Accessories.MASK,0),0);
        c.wardrobe.equip(c,Accessories.item(Accessories.GLASSES,0),0);
        WardrobeScreen ui=c.wardrobe.screen;ui.picking.capture(c,L);
        float[] glasses=ui.picking.targets[Accessories.GLASSES],mask=ui.picking.targets[Accessories.MASK];
        Storybook.tap(c,L,(glasses[0]+glasses[4])*.5f,(glasses[1]+glasses[5])*.5f);
        check("shades win overlapping face target",ui.slot==Accessories.GLASSES);
        Storybook.tap(c,L,(mask[0]+mask[4])*.5f,mask[1]+(mask[5]-mask[1])*.87f);
        check("lower mask remains selectable under shades",ui.slot==Accessories.MASK);
    }
    private static float[] wornPoint(GameCore c,Layout L,int category) {
        c.wardrobe.screen.picking.capture(c,L);
        float[] q=c.wardrobe.screen.picking.targets[category];
        float u=category==Accessories.FLAIR?.95f:.5f,v=category==Accessories.MASK?.84f:.5f;
        return new float[]{q[0]+(q[2]-q[0])*u+(q[6]-q[0])*v,q[1]+(q[3]-q[1])*u+(q[7]-q[1])*v};
    }
    private static void dragOff(Layout L) {
        GameCore c=ready();grant(c);c.caseOpen=true;
        for(int who:AccessoryPlacementPreview.types()) {
            c.collected=Collect.add(c.collected,who);c.closeStory();c.caseIndex=who;c.openStory();c.storyT=2;c.storyPulse=2;
            WardrobeScreen ui=c.wardrobe.screen;
            for(int category=0;category<Accessories.SLOTS;category++) {
                int item=Accessories.item(category,0);c.wardrobe.equip(c,item,who);ui.danceT=3;
                float[] point=wornPoint(c,L,category);float x=point[0],y=point[1];
                check("worn accessory captures pointer",ui.touch(c,L,0,7,x,y));
                ui.touch(c,L,1,7,x,y);
                check("worn tap still selects category without removal",ui.slot==category && c.wardrobe.equipped(who,category)==item);
                byte[] before=c.progress.snapshot();
                ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,L.w*.02f,L.h*.94f);
                check("drag lifts only the worn rendering",ui.dragging && ui.heroPainter(ui.picking).accessory(who,category)<0
                        && java.util.Arrays.equals(before,c.progress.snapshot()));
                ui.touch(c,L,1,8,0,0);check("other finger cannot remove outfit",ui.dragging && c.wardrobe.equipped(who,category)==item);
                ui.touch(c,L,1,7,x,y);
                check("dropping back restores accessory",!ui.dragging && c.wardrobe.equipped(who,category)==item && java.util.Arrays.equals(before,c.progress.snapshot()));
                ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,L.w*.02f,L.h*.94f);ui.touch(c,L,3,7,0,0);
                check("cancelled worn drag keeps outfit",!ui.dragging && c.wardrobe.equipped(who,category)==item);
                ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,L.w*.02f,L.h*.94f);ui.touch(c,L,5,8,0,0);ui.touch(c,L,1,7,0,0);
                check("second touch cancels worn removal",!ui.dragging && c.wardrobe.equipped(who,category)==item);
                ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,L.w*.02f,L.h*.94f);ui.touch(c,L,1,7,L.w*.02f,L.h*.94f);
                check("drop off squishy saves removal and celebrates",c.wardrobe.equipped(who,category)<0 && ui.danceT==0 && c.storyOpen() && c.wardrobe.owned[item]);
                GameCore reload=new GameCore(c.store,214,true);
                check("drag removal survives reopening app",reload.wardrobe.equipped(who,category)<0);
            }
        }
    }
    private static void sounds() {
        GameCore c=ready();grant(c);Ear ear=new Ear();c.sound=ear;
        WardrobeScreen ui=c.wardrobe.screen;ui.show(c,0);
        for(int slot=0;slot<Accessories.SLOTS;slot++) {
            ui.choose(c,10+slot);int before=ear.accessoryChanges;
            ui.choose(c,20);
            check("each slot has its own donning cue",ear.accessoryChanges==before+1 && ear.lastAccessorySlot==slot && !ear.accessoryRemoved);
            ui.choose(c,20);
            check("worn card toggles off with doffing cue",ear.accessoryChanges==before+2 && ear.accessoryRemoved && c.wardrobe.equipped(0,slot)<0);
            GameCore reload=new GameCore(c.store,214,true);
            check("tap removal survives restart",reload.wardrobe.equipped(0,slot)<0);
            ui.choose(c,2);check("empty removal is silent",ear.accessoryChanges==before+2);
        }
        check("accessory audio replaces generic star cue",ear.stars==0);
    }
    private static void developerCategories(Layout L) {
        if(!BuildFlags.DEVELOPER)return;
        for(int slot=0;slot<Accessories.SLOTS;slot++) {
            GameCore c=new GameCore(new Mem(),214,false);c.collected=1;
            c.wardrobe.developer(c,slot+3);
            for(int i=0;i<Accessories.COUNT;i++)check("category grant stays in selected slot "+slot+":"+i,
                    c.wardrobe.owned[i]==(Accessories.slot(i)==slot));
            check("category chip reflects ownership",c.wardrobe.categoryOwned(slot));
            byte[] granted=c.progress.snapshot();c.wardrobe.developer(c,slot+3);
            check("repeat category grant is idempotent",java.util.Arrays.equals(granted,c.progress.snapshot()));
            c.wardrobe.equip(c,Accessories.item(slot,0),0);c.wardrobe.developer(c,1);
            for(boolean owned:c.wardrobe.owned)check("all accessories chip grants every style",owned);
            check("grant all preserves equipped outfit",c.wardrobe.equipped(0,slot)==Accessories.item(slot,0));
            check("developer gifts do not forge achievements",c.progress.maximum("highest_stage")==0
                    && c.progress.count("rewards_total")==0);
            GameCore reload=new GameCore(c.store,214,false);
            check("category gifts persist in developer saves",reload.wardrobe.categoryOwned(slot));
            c.wardrobe.developer(c,2);
            for(boolean owned:c.wardrobe.owned)check("reset clears gifted wardrobe",!owned);
        }
        for(int[] size:new int[][]{{320,568},{640,1400},{1080,2340}}) {
            Layout small=new Layout();small.compute(size[0],size[1],0,20,0,24);
            SettingsUi u=new SettingsUi();u.compute(small,SettingsUi.PROGRESS);
            for(int slot=0;slot<Accessories.SLOTS;slot++) {
                float x=(u.testChipL(slot%2,u.accessoryColumns(slot/2))+u.testChipR(slot%2,u.accessoryColumns(slot/2)))*.5f,y=u.accessoryCategoryY[slot/2]+u.testH*.5f;
                check("category chip routes to matching grant",u.hit(x,y)==SettingsUi.HIT_ACCESSORY_CATEGORY+slot);
            }
            check("category chips fit panel",u.accessoryCategoryY[u.accessoryCategoryY.length-1]+u.testH<u.panelB);
        }
    }
    private static void flair(Layout L) {
        GameCore c=ready();grant(c);int item=Accessories.item(Accessories.FLAIR,0);
        c.wardrobe.equip(c,9,0);c.wardrobe.equip(c,item,0);c.wardrobe.equip(c,item,1);
        check("flair stacks with masks and is reusable",c.wardrobe.equipped(0,Accessories.MASK)==9
                && c.wardrobe.equipped(0,Accessories.FLAIR)==item && c.wardrobe.equipped(1,Accessories.FLAIR)==item);
        GameCore restored=new GameCore(c.store,216,true);
        check("flair saved independently with outfit",restored.wardrobe.equipped(0,Accessories.FLAIR)==item);
        WardrobeScreen ui=c.wardrobe.screen;ui.show(c,0);
        int tab=ui.hit(L,WardrobeToybox.categoryX(L,Accessories.FLAIR),WardrobeToybox.categoryY(L));ui.choose(c,tab);
        check("flair toy opens its category",tab==10+Accessories.FLAIR && ui.slot==Accessories.FLAIR);
        for(int variant=0;variant<Accessories.count(Accessories.FLAIR);variant++) {
            FlairProbe first=new FlairProbe(),same=new FlairProbe(),later=new FlairProbe();
            AccessoryFlair.draw(first,variant,100,150,40,1f);
            AccessoryFlair.draw(same,variant,100,150,40,1f);
            AccessoryFlair.draw(later,variant,100,150,40,1.3f);
            check("flair animates deterministically "+variant,first.hash==same.hash && first.hash!=later.hash);
            check("flair has bounded draw cost "+variant,first.calls>0 && first.calls<80);
        }
        for(int variant=0;variant<3;variant++)for(int skin=0;skin<Skins.COUNT;skin++)for(boolean ninja:new boolean[]{false,true}) {
            final int flair=Accessories.item(Accessories.FLAIR,variant);
            FlairProbe expected=new FlairProbe(){public float accessorySize(float r){return 1;}};
            AccessoryFlair.draw(expected,variant,0,0,100,1.2f);
            for(int who:AccessoryPlacementPreview.types()) {
                final int[] start={-1};
                FlairProbe front=new FlairProbe() {
                    public float accessorySize(float r){return 1;}
                    public boolean accessoryTargets(){return true;}
                    public void accessoryTarget(int slot,float[] corners){if(slot==Accessories.FLAIR)start[0]=calls;}
                    public int accessory(int id,int slot){return slot==Accessories.FLAIR?flair:Accessories.item(slot,0);}
                };
                Trinket.drawReacting(Skins.painter(front,skin),who,200,200,100,1.2f,1,0,0,ninja);
                check("flair is final pass over outfit, body and ninja hood",start[0]>=0 && front.calls-start[0]==expected.calls);
            }
        }
    }
    private static class FlairProbe extends OpacityPainter {
        int calls;long hash=1;
        FlairProbe() {super(new RasterPainter(1,1,1),1);}
        private void record(int color,float... values) {
            calls++;hash=hash*31+color;
            for(float value:values)hash=hash*31+Float.floatToIntBits(value);
        }
        public void line(float x,float y,float xx,float yy,int color,float width) {record(color,x,y,xx,yy,width);}
        public void strokePoly(float[] xy,int color,float width) {record(color,xy);}
        public void fillCircle(float x,float y,float r,int color) {record(color,x,y,r);}
        public void fillEllipse(float x,float y,float rx,float ry,int color) {record(color,x,y,rx,ry);}
        public void fillPoly(float[] xy,int color) {record(color,xy);}
        public void fillContours(float[][] xy,int color) {for(float[] contour:xy)record(color,contour);}
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int color,float width) {record(color,x,y,rx,ry,start,sweep,width);}
    }
    private static void catalog() {
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<Accessories.COUNT;i++) {
            check("unique stable accessory id "+i,ids.add(Accessories.ID[i]));
            check("mapped milestone exists "+i,Achievements.id(Accessories.achievement(i)).equals(Accessories.ACHIEVEMENT[i]));
        }
        for(int slot=0;slot<Accessories.SLOTS;slot++) {
            check("category has at least three designs",Accessories.count(slot)>=3);
            for(int v=0;v<Accessories.count(slot);v++) {
                int item=Accessories.item(slot,v);
                check("catalog category and variant round trip",Accessories.slot(item)==slot && Accessories.variant(item)==v);
            }
        }
        check("expanded accessory categories",Accessories.count(Accessories.HAT)==7 && Accessories.count(Accessories.GLASSES)==5 && Accessories.count(Accessories.BOOTS)==7);
        check("four mask choices",Accessories.count(Accessories.MASK)==4);
        check("redesigned masks retain existing save IDs",Accessories.ID[9].equals("petal_mask")
                && Accessories.ID[10].equals("moon_mask") && Accessories.ID[11].equals("party_mask"));
        check("attachment geometry covers current body shapes",AccessoryAnchors.TOP.length==Collect.SHAPE_COUNT
            && AccessoryAnchors.FEET.length==Collect.SHAPE_COUNT);
        check("persisted style ID has space",Accessories.COUNT<Wardrobe.ITEM_MASK);
        for(int i=0;i<Collect.COUNT;i++)check("collectible attachment family "+i,Collect.bossKind(i)>=0
            || i>=Collect.DUCK_FIRST && i<Collect.DUCK_FIRST+Collect.DUCK_COUNT
            || Collect.SHAPE[i]>=0 && Collect.SHAPE[i]<AccessoryAnchors.TOP.length);
    }
    private static void rewards() {
        try {
            Mem m=new Mem();ProgressData data=new ProgressData();
            data.increment("old",Collect.progressKey(0),1);data.maximum("highest_stage",5);data.maximum("achievement_run_lands",3);data.maximum("achievement_boss_octopulse",1);m.progress=data.encode();
            GameCore c=new GameCore(m,214,true);
            check("existing Octopulse win grants googly eyes",c.wardrobe.owned[Accessories.item(Accessories.GLASSES,3)]);
            check("existing land achievement grants glitter",c.wardrobe.owned[AccessoryGlitter.ITEM]);
            check("retroactive offline rewards",c.wardrobe.owned[0] && c.wardrobe.owned[1] && !c.wardrobe.owned[2]);
            byte[] once=c.progress.snapshot();c.wardrobe.revision=-1;c.wardrobe.refresh(c);
            check("reward grant is idempotent",java.util.Arrays.equals(once,c.progress.snapshot()));
            c.wardrobe.acknowledge(c);
            GameCore reload=new GameCore(m,215,true);
            check("ownership and acknowledgement survive reload",reload.wardrobe.owned[0] && reload.wardrobe.pending(reload)<0);
            ProgressData reset=c.progress.prepareScoreReset();c.progress.acceptScoreReset(reset);c.wardrobe.refresh(c);
            check("score reset retains wardrobe",c.wardrobe.owned[0]);
            c.onboarding.reset(c);c.wardrobe.refresh(c);
            check("tutorial reset retains wardrobe",c.wardrobe.owned[0]);
            ProgressData incoming=new ProgressData();incoming.maximum("achievement_survival_seconds",180);
            c.progress.restore(incoming.encode(),c);c.wardrobe.refresh(c);
            check("cloud milestones backfill rewards",c.wardrobe.owned[5] && c.wardrobe.owned[7]);
            check("unknown target stays undiscovered",!c.wardrobe.equip(c,0,10) && !Collect.has(c.collected,10));
        } catch(Exception e) {throw new AssertionError(e);}
    }
    private static void sharedOutfits() {
        GameCore c=ready();grant(c);Wardrobe w=c.wardrobe;
        check("equip owned accessory",w.equip(c,0,0) && w.equipped(0,0)==0);
        w.screen.show(c,1);w.screen.selected=0;w.screen.choose(c,20);
        check("same accessory equips immediately on another squishy",w.equipped(0,0)==0 && w.equipped(1,0)==0);
        w.equip(c,1,1);
        check("replace only changes chosen squishy",w.equipped(0,0)==0 && w.equipped(1,0)==1 && w.owned[0]);
        w.equip(c,3,1);w.equip(c,6,1);w.equip(c,9,1);
        check("four compatible slots",w.equipped(1,0)==1 && w.equipped(1,1)==3 && w.equipped(1,2)==6 && w.equipped(1,3)==9);
        w.remove(c,1,0);check("remove only affects one outfit",w.equipped(1,0)<0 && w.equipped(0,0)==0 && w.owned[1]);
        GameCore reload=new GameCore(c.store,216,true);
        check("each outfit and removal survive restart",reload.wardrobe.equipped(1,1)==3
                && reload.wardrobe.equipped(1,0)<0 && reload.wardrobe.equipped(0,0)==0);
        w.equip(c,0,1);w.screen.show(c,1);
        check("editor opens actual saved outfit",w.screen.selected==0);
        w.screen.choose(c,21);w.screen.back(c);
        check("leaving preserves immediate outfit edit",w.equipped(0,0)==0 && w.equipped(1,0)==1);
        TestProgress.Storage store=new TestProgress.Storage("broken");
        Progress broken=new Progress(store,true);store.broken=true;
        check("failed write does not create ownership",!broken.accessories(new String[]{Accessories.key("owned",0)},new long[]{1},false)
            && broken.maximum(Accessories.key("owned",0))==0);
    }
    private static void migration() {
        try {
            Mem saved=new Mem();ProgressData old=new ProgressData();
            for(int item:new int[]{0,1,6})old.maximum(Accessories.key("owned",item),1);
            old.maximum(Accessories.key("wearer",0),(4L<<10)|1);
            old.maximum(Accessories.key("wearer",1),(4L<<10)|1);
            old.maximum(Accessories.key("wearer",6),(5L<<10)|2);
            old.increment("old",Collect.progressKey(0),1);old.increment("old",Collect.progressKey(1),1);
            saved.progress=old.encode();GameCore c=new GameCore(saved,214,true);
            check("migration preserves legacy slot winner",c.wardrobe.equipped(0,0)==1);
            check("migration preserves another squishy's footwear",c.wardrobe.equipped(1,Accessories.BOOTS)==6);
            byte[] once=c.progress.snapshot();c.wardrobe.revision=-1;c.wardrobe.refresh(c);
            check("legacy migration is idempotent",java.util.Arrays.equals(once,c.progress.snapshot()));
            c.wardrobe.equip(c,1,1);c.wardrobe.remove(c,0,0);
            old.maximum(Accessories.key("wearer",0),(999L<<10)|1);
            c.progress.restore(old.encode(),c);c.wardrobe.refresh(c);
            check("old cloud claims cannot undo new outfit edits",c.wardrobe.equipped(0,0)<0 && c.wardrobe.equipped(1,0)==1);
            GameCore reload=new GameCore(saved,215,true);
            check("migrated removal survives restart",reload.wardrobe.equipped(0,0)<0 && reload.wardrobe.equipped(1,0)==1);
        } catch(Exception e) {throw new AssertionError(e);}
    }
    private static void merge() {
        try {
            GameCore a=ready(),b=ready();grant(a);grant(b);
            a.wardrobe.equip(a,0,0);b.wardrobe.equip(b,0,1);
            a.wardrobe.equip(a,3,0);b.wardrobe.equip(b,6,0);
            ProgressData ab=ProgressData.decode(a.progress.snapshot()),ba=ProgressData.decode(b.progress.snapshot());
            byte[] aa=ab.encode(),bb=ba.encode();ab.merge(ProgressData.decode(bb));ba.merge(ProgressData.decode(aa));
            check("concurrent outfit edits converge",java.util.Arrays.equals(ab.encode(),ba.encode()));
            a.progress.restore(ab.encode(),a);a.wardrobe.refresh(a);
            b.progress.restore(ba.encode(),b);b.wardrobe.refresh(b);
            check("shared style survives cloud merge on both squishies",a.wardrobe.equipped(0,0)==0 && a.wardrobe.equipped(1,0)==0);
            check("different slot edits both survive",a.wardrobe.equipped(0,1)==3 && a.wardrobe.equipped(0,2)==6);
            a.wardrobe.equip(a,1,0);b.wardrobe.equip(b,2,0);
            ab=ProgressData.decode(a.progress.snapshot());ba=ProgressData.decode(b.progress.snapshot());
            aa=ab.encode();bb=ba.encode();ab.merge(ProgressData.decode(bb));ba.merge(ProgressData.decode(aa));
            check("same-slot conflicts converge",java.util.Arrays.equals(ab.encode(),ba.encode()));
            a.progress.restore(ab.encode(),a);a.wardrobe.refresh(a);
            check("same-slot tie has stable style winner",a.wardrobe.equipped(0,0)==2 && a.wardrobe.equipped(1,0)==0);
            a.wardrobe.remove(a,0,0);a.progress.restore(ba.encode(),a);a.wardrobe.refresh(a);
            check("new removal beats older equipped snapshot",a.wardrobe.equipped(0,0)<0 && a.wardrobe.equipped(1,0)==0);
            ProgressData fresh=ProgressData.decode(a.progress.snapshot());fresh.resetAccessories();fresh.merge(ba);ba.merge(fresh);
            check("developer reset fences old cloud outfits",java.util.Arrays.equals(fresh.encode(),ba.encode())
                && fresh.maximum(Accessories.key("owned",0))==0 && fresh.maximum(Wardrobe.outfitKey(1,0))==0);
        } catch(Exception e) {throw new AssertionError(e);}
    }
    private static void toybox(Layout L) {
        GameCore c=ready();grant(c);c.caseOpen=true;c.caseIndex=0;c.openStory();c.storyT=2;
        WardrobeScreen ui=c.wardrobe.screen;ui.trayT=1;
        float x=WardrobeToybox.itemX(ui,L,Accessories.count(ui.slot),1),y=WardrobeToybox.itemY(ui,L,Accessories.count(ui.slot),1);
        byte[] before=c.progress.snapshot();
        ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,x+L.w*.22f,y);
        check("sideways swoosh scrolls instead of carrying clothing",ui.scrolling && !ui.dragging && ui.swoosh>0);
        ui.touch(c,L,1,8,x,y);check("other pointer cannot release ribbon",ui.scrolling);
        ui.touch(c,L,1,7,x+L.w*.22f,y);ui.update(c,1);
        check("swoosh never changes outfit and springs to edge",java.util.Arrays.equals(before,c.progress.snapshot()) && ui.scroll==0 && !ui.scrolling);
        ui.touch(c,L,0,7,x,y);ui.touch(c,L,2,7,x+L.w*.3f,y);ui.touch(c,L,3,7,x,y);ui.update(c,1);
        check("cancelled swoosh settles without equipping",ui.scroll==0 && java.util.Arrays.equals(before,c.progress.snapshot()));
        ui.choose(c,10+Accessories.MASK);ui.trayT=1;
        x=WardrobeToybox.itemX(ui,L,4,1);y=WardrobeToybox.itemY(ui,L,4,1);
        ui.touch(c,L,0,7,x,y);ui.update(c,.15f);ui.touch(c,L,2,7,x-L.w*.29f,y);ui.touch(c,L,1,7,x-L.w*.29f,y);ui.update(c,1);
        check("swoosh reveals fourth mask without equipping",ui.scroll==1 && c.wardrobe.equipped(0,Accessories.MASK)<0
                && ui.hit(L,WardrobeToybox.itemX(ui,L,4,3),WardrobeToybox.itemY(ui,L,4,3))==23);
        for(int count:new int[]{1,3,4,12,50,500}) {
            float end=WardrobeToybox.maxScroll(count);ui.scroll=end;ui.trayT=1;
            x=WardrobeToybox.itemX(ui,L,count,count-1);y=WardrobeToybox.itemY(ui,L,count,count-1);
            check("last accessory reachable in growing collection "+count,WardrobeToybox.hit(ui,L,count,x,y)==20+count-1 && x<L.w*.94f);
            check("swoosh is bounded with elastic edges "+count,WardrobeScreen.boundedScroll(-100,count)>=-.42f && WardrobeScreen.boundedScroll(end+100,count)<=end+.42f);
            check("fling lands within collection "+count,WardrobeScreen.scrollDestination(end,100,count)==end && WardrobeScreen.scrollDestination(0,-100,count)==0);
        }
        ui.scroll=ui.scrollTarget=0;
        c.wardrobe.equip(c,0,1);ui.shake(c);
        GameCore reload=new GameCore(c.store,214,true);
        for(int slot=0;slot<Accessories.SLOTS;slot++) {
            int item=c.wardrobe.equipped(0,slot);
            check("shuffle equips and saves earned matching style",item>=0 && c.wardrobe.owned[item] && Accessories.slot(item)==slot && reload.wardrobe.equipped(0,slot)==item);
        }
        GameCore untouched=ready(),shuffled=ready();grant(untouched);grant(shuffled);
        openForShake(shuffled);shuffled.wardrobe.screen.shake(shuffled);
        check("shuffle preserves gameplay randomness",untouched.rnd.nextLong()==shuffled.rnd.nextLong());
        check("shuffle leaves other squishy alone",c.wardrobe.equipped(1,0)==0 && c.wardrobe.equipped(1,1)<0);
        GameCore locked=ready();openForShake(locked);locked.wardrobe.screen.shake(locked);
        for(int slot=0;slot<Accessories.SLOTS;slot++)check("shuffle cannot grant locked items",locked.wardrobe.equipped(0,slot)<0);
        int[] outfit={0,3,6,9,13};before=locked.progress.snapshot();
        check("whole outfit rejects unearned styles",!locked.wardrobe.equipOutfit(locked,0,outfit) && java.util.Arrays.equals(before,locked.progress.snapshot()));
        Mem backing=new Mem();boolean[] failWrite={false};
        GameCore.Store store=(GameCore.Store)java.lang.reflect.Proxy.newProxyInstance(GameCore.Store.class.getClassLoader(),new Class<?>[]{GameCore.Store.class},(proxy,method,args)->{
            if(failWrite[0] && method.getName().equals("saveProgress"))throw new IllegalStateException("disk full");
            return method.invoke(backing,args);
        });
        GameCore broken=new GameCore(store,214,true);broken.collected=3;grant(broken);
        before=broken.progress.snapshot();failWrite[0]=true;
        check("failed shuffle write leaves entire outfit untouched",!broken.wardrobe.equipOutfit(broken,0,outfit) && java.util.Arrays.equals(before,broken.progress.snapshot()));
    }
    private static void ui(Layout L) {
        GameCore c=ready();grant(c);WardrobeScreen ui=c.wardrobe.screen;
        c.caseOpen=true;c.caseIndex=0;c.openStory();c.storyT=2;
        check("story opens wardrobe inline",ui.open && c.storyOpen() && !ui.reward);
        ui.choose(c,10+Accessories.MASK);
        ui.scroll=ui.scrollTarget=1;ui.trayT=1;float fourthX=WardrobeToybox.itemX(ui,L,4,3);
        int hit=ui.hit(L,fourthX,WardrobeToybox.itemY(ui,L,4,3));ui.choose(c,hit);
        int lastMask=Accessories.item(Accessories.MASK,3);
        check("card immediately equips fourth mask",hit==23 && c.wardrobe.equipped(0,Accessories.MASK)==lastMask);
        check("equip starts flash before dance",ui.danceT==0);
        int priorDance=ui.dance;ui.choose(c,20);
        check("next outfit picks another dance",ui.dance!=priorDance && ui.danceT==0);
        check("inline animation does not pause story",!ui.update(c,.1f));
        GameCore reload=new GameCore(c.store,214,true);
        check("tap outfit persists without confirmation",reload.wardrobe.equipped(0,Accessories.MASK)==Accessories.item(Accessories.MASK,0));
        ui.choose(c,10+Accessories.HAT);
        ui.trayT=1;float cardX=WardrobeToybox.itemX(ui,L,Accessories.count(ui.slot),1),cardY=WardrobeToybox.itemY(ui,L,Accessories.count(ui.slot),1),hx=Storybook.heroX(c,L),hy=Storybook.heroY(c,L);
        ui.touch(c,L,0,7,cardX,cardY);ui.touch(c,L,2,7,hx,hy);
        check("drag carries accessory without saving early",ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)<0);
        ui.touch(c,L,1,8,hx,hy);
        check("different pointer cannot finish drag",ui.dragging);
        ui.touch(c,L,1,7,hx,hy);
        check("drop on squishy equips",!ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)==1);
        ui.touch(c,L,0,7,cardX,cardY);ui.touch(c,L,2,7,hx,hy);ui.touch(c,L,1,7,hx,hy);
        check("dropping worn item keeps it equipped",c.wardrobe.equipped(0,Accessories.HAT)==1);
        ui.touch(c,L,0,7,WardrobeToybox.itemX(ui,L,Accessories.count(ui.slot),0),WardrobeToybox.itemY(ui,L,Accessories.count(ui.slot),0));ui.touch(c,L,2,7,0,0);ui.touch(c,L,1,7,0,0);
        check("drop outside preserves outfit",c.wardrobe.equipped(0,Accessories.HAT)==1 && c.storyOpen());
        ui.touch(c,L,0,7,WardrobeToybox.itemX(ui,L,Accessories.count(ui.slot),0),WardrobeToybox.itemY(ui,L,Accessories.count(ui.slot),0));ui.touch(c,L,2,7,hx,hy);ui.touch(c,L,3,7,hx,hy);
        check("cancelled drag preserves outfit",!ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)==1);
        check("removed button has no invisible target",ui.hit(L,L.w*.5f,WardrobeScreen.trayY(L,.93f))==0);
        ui.choose(c,21);check("tap removal saves immediately",c.wardrobe.equipped(0,Accessories.HAT)<0);
        c.wardrobe.owned[0]=false;ui.choose(c,20);
        check("hidden locked item is skipped by picker",c.wardrobe.equipped(0,Accessories.HAT)==1 && ui.selected==1);
        check("update notice waits for editor",!UpdatePrompt.suitable(c));
        int state=c.state;c.screenKey(0);advance(c,L,WardrobeScreen.EXIT_TIME+.05f);
        check("game key dismisses inline wardrobe without starting",c.state==state && !c.starting() && !ui.open && !c.storyOpen() && c.caseOpen);
        c.openStory();
        Pause.back(c);advance(c,L,WardrobeScreen.EXIT_TIME+.05f);check("back closes story and inline wardrobe together",!ui.open && !c.storyOpen());
        c.state=GameCore.PLAY;ui.update(c,.1f);check("reward waits until title",!ui.open);
        c.state=GameCore.TITLE;c.onboarding.titleGuide=true;ui.update(c,.1f);
        check("reward never interrupts tutorial",!ui.open);
        for(int[] size:new int[][]{{320,568},{640,1400},{1080,2340}}) {
            Layout small=new Layout();small.compute(size[0],size[1],0,20,0,24);
            SettingsUi settings=new SettingsUi();settings.compute(small,SettingsUi.PROGRESS);
            check("developer accessory chips stay in panel",settings.accessoryY+settings.testH<settings.panelB);
            check("editor buttons fit portrait",WardrobeScreen.y(small,.89f)<small.h-small.padB);
            float topGap=WardrobeScreen.trayY(small,.09f)-WardrobeScreen.safeTop(small);
            float bottomGap=small.h-WardrobeScreen.safeBottom(small)-Storybook.panelBottom(small);
            check("dress-up keeps equal generous outer margins",topGap>=small.unit*1.49f && Math.abs(topGap-bottomGap)<.01f);
            check("picker at top and story anchored at bottom",WardrobeScreen.trayTop(small)>=small.topSafe && WardrobeScreen.trayY(small,1)<Storybook.panelTop(small) && Storybook.panelBottom(small)<small.h-small.padB);
        }
    }
}
