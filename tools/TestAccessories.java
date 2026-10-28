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
        catalog();rewards();transfers();merge();ui(L);
    }
    private static void catalog() {
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<Accessories.COUNT;i++) {
            check("unique stable accessory id "+i,ids.add(Accessories.ID[i]));
            check("mapped milestone exists "+i,Achievements.id(Accessories.achievement(i)).equals(Accessories.ACHIEVEMENT[i]));
        }
        check("three designs per category",Accessories.COUNT==Accessories.SLOTS*3);
        check("attachment geometry covers current body shapes",AccessoryAnchors.TOP.length==Collect.SHAPE_COUNT
            && AccessoryAnchors.FEET.length==Collect.SHAPE_COUNT);
        check("persisted wearer ID has space",Collect.COUNT<Wardrobe.OWNER_MASK);
        for(int i=0;i<Collect.COUNT;i++)check("collectible attachment family "+i,Collect.bossKind(i)>=0
            || i>=Collect.DUCK_FIRST && i<Collect.DUCK_FIRST+Collect.DUCK_COUNT
            || Collect.SHAPE[i]>=0 && Collect.SHAPE[i]<AccessoryAnchors.TOP.length);
    }
    private static void rewards() {
        try {
            Mem m=new Mem();ProgressData data=new ProgressData();
            data.increment("old",Collect.progressKey(0),1);data.maximum("highest_stage",5);m.progress=data.encode();
            GameCore c=new GameCore(m,214,true);
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
    private static void transfers() {
        GameCore c=ready();grant(c);Wardrobe w=c.wardrobe;
        check("equip owned accessory",w.equip(c,0,0) && w.equipped(0,0)==0);
        w.screen.show(c,1);w.screen.selected=0;w.screen.choose(c,2);
        check("transfer asks before changing wearer",w.screen.confirming && w.wearer(0)==0);
        w.screen.back();check("cancel preserves original outfit",w.wearer(0)==0 && !w.screen.confirming);
        w.screen.choose(c,2);w.screen.choose(c,2);
        check("confirmed transfer moves single copy",w.wearer(0)==1 && w.equipped(0,0)<0);
        w.equip(c,1,1);check("replace returns old item to wardrobe",w.wearer(0)<0 && w.owned[0] && w.wearer(1)==1);
        w.equip(c,3,1);w.equip(c,6,1);w.equip(c,9,1);
        check("four compatible slots",w.equipped(1,0)==1 && w.equipped(1,1)==3 && w.equipped(1,2)==6 && w.equipped(1,3)==9);
        w.equip(c,1,-1);check("remove preserves unlock",w.wearer(1)<0 && w.owned[1]);
        GameCore reload=new GameCore(c.store,216,true);
        check("outfits survive restart",reload.wardrobe.equipped(1,1)==3);
        TestProgress.Storage store=new TestProgress.Storage("broken");
        Progress broken=new Progress(store,true);store.broken=true;
        check("failed write does not create ownership",!broken.accessories(new String[]{Accessories.key("owned",0)},new long[]{1},false)
            && broken.maximum(Accessories.key("owned",0))==0);
    }
    private static void merge() {
        try {
            GameCore a=ready(),b=ready();grant(a);grant(b);
            a.wardrobe.equip(a,0,0);b.wardrobe.equip(b,0,1);b.wardrobe.equip(b,1,0);
            ProgressData ab=ProgressData.decode(a.progress.snapshot()),ba=ProgressData.decode(b.progress.snapshot());
            byte[] aa=ab.encode(),bb=ba.encode();ab.merge(ProgressData.decode(bb));ba.merge(ProgressData.decode(aa));
            check("concurrent edits converge",java.util.Arrays.equals(ab.encode(),ba.encode()));
            a.progress.restore(ab.encode(),a);a.wardrobe.refresh(a);
            check("one copy only after merge",a.wardrobe.wearer(0)==1 && a.wardrobe.wearer(1)==0);
            // Equal logical clocks assigned different hats to the same squishy on two devices.
            ProgressData conflict=ProgressData.decode(a.progress.snapshot());
            conflict.maximum(Accessories.key("wearer",0),(10L<<Wardrobe.OWNER_BITS)+1);
            conflict.maximum(Accessories.key("wearer",1),(10L<<Wardrobe.OWNER_BITS)+1);
            a.progress.restore(conflict.encode(),a);a.wardrobe.refresh(a);
            check("same slot conflicts choose stable winner",a.wardrobe.equipped(0,0)==1);
            a.wardrobe.equip(a,2,0);a.wardrobe.equip(a,2,-1);
            check("replaced merge losers never resurface",a.wardrobe.equipped(0,0)<0);
            ProgressData fresh=ProgressData.decode(a.progress.snapshot());fresh.resetAccessories();
            fresh.merge(conflict);conflict.merge(fresh);
            check("developer reset fences old cloud outfits",java.util.Arrays.equals(fresh.encode(),conflict.encode())
                && fresh.maximum(Accessories.key("owned",0))==0);
        } catch(Exception e) {throw new AssertionError(e);}
    }
    private static void ui(Layout L) {
        GameCore c=ready();grant(c);WardrobeScreen ui=c.wardrobe.screen;
        ui.show(c,0);ui.selected=5;
        check("preview does not equip",c.wardrobe.wearer(5)<0);
        int state=c.state;c.screenKey(0);check("keyboard cannot start through wardrobe",c.state==state && !c.starting());
        check("update notice waits for editor",!UpdatePrompt.suitable(c));
        ui.confirming=true;Pause.back(c);check("back cancels transfer before closing",ui.open && !ui.confirming);
        ui.touch(c,L,0,7,L.w*.5f,WardrobeScreen.y(L,.94f));
        ui.touch(c,L,1,7,L.w*.5f,WardrobeScreen.y(L,.94f));check("complete dismiss gesture consumed",!ui.open);
        c.state=GameCore.PLAY;ui.update(c,.1f);check("reward waits until title",!ui.open);
        c.state=GameCore.TITLE;c.onboarding.titleGuide=true;ui.update(c,.1f);
        check("reward never interrupts tutorial",!ui.open);
        for(int[] size:new int[][]{{320,568},{640,1400},{1080,2340}}) {
            Layout small=new Layout();small.compute(size[0],size[1],0,20,0,24);
            SettingsUi settings=new SettingsUi();settings.compute(small,SettingsUi.PROGRESS);
            check("developer accessory chips stay in panel",settings.accessoryY+settings.testH<settings.panelB);
            check("editor buttons fit portrait",WardrobeScreen.y(small,.89f)<small.h-small.padB);
            check("story dress button above deck",Storybook.dressY(small)+small.unit*.8f<small.h-small.padB);
        }
    }
}
