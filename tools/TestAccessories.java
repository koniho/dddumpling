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
        catalog();rewards();sharedOutfits();migration();merge();ui(L);developerCategories(L);flair(L);
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
        int tab=ui.hit(L,L.w*.82f,WardrobeScreen.trayY(L,.20f));ui.choose(c,tab);
        check("fifth wardrobe tab opens flair",tab==10+Accessories.FLAIR && ui.slot==Accessories.FLAIR);
        for(int variant=0;variant<Accessories.count(Accessories.FLAIR);variant++) {
            FlairProbe first=new FlairProbe(),same=new FlairProbe(),later=new FlairProbe();
            AccessoryFlair.draw(first,variant,100,150,40,1f);
            AccessoryFlair.draw(same,variant,100,150,40,1f);
            AccessoryFlair.draw(later,variant,100,150,40,1.3f);
            check("flair animates deterministically "+variant,first.hash==same.hash && first.hash!=later.hash);
            check("flair has bounded draw cost "+variant,first.calls>0 && first.calls<80);
        }
        FlairProbe powered=new FlairProbe() {
            public int accessory(int who,int slot) {return slot==Accessories.FLAIR?item:-1;}
        };
        AccessoryArt.outfit(powered,0,100,150,30,100,100,190,40,1,1,true);
        check("Ninja costume keeps cosmetic flair visible",powered.calls>0);
    }
    private static class FlairProbe extends OpacityPainter {
        int calls;long hash=1;
        FlairProbe() {super(new RasterPainter(1,1,1),1);}
        private void record(int color,float... values) {
            calls++;hash=hash*31+color;
            for(float value:values)hash=hash*31+Float.floatToIntBits(value);
        }
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
    private static void ui(Layout L) {
        GameCore c=ready();grant(c);WardrobeScreen ui=c.wardrobe.screen;
        c.caseOpen=true;c.caseIndex=0;c.openStory();c.storyT=2;
        check("story opens wardrobe inline",ui.open && c.storyOpen() && !ui.reward);
        ui.choose(c,10+Accessories.MASK);
        float fourthX=L.w*.80f;
        int hit=ui.hit(L,fourthX,WardrobeScreen.trayY(L,.50f));ui.choose(c,hit);
        int lastMask=Accessories.item(Accessories.MASK,3);
        check("card immediately equips fourth mask",hit==23 && c.wardrobe.equipped(0,Accessories.MASK)==lastMask);
        check("equip starts flash before dance",ui.danceT==0);
        int priorDance=ui.dance;ui.choose(c,20);
        check("next outfit picks another dance",ui.dance!=priorDance && ui.danceT==0);
        check("inline animation does not pause story",!ui.update(c,.1f));
        GameCore reload=new GameCore(c.store,214,true);
        check("tap outfit persists without confirmation",reload.wardrobe.equipped(0,Accessories.MASK)==Accessories.item(Accessories.MASK,0));
        ui.choose(c,10+Accessories.HAT);
        float cardX=L.w*.50f,cardY=WardrobeScreen.trayY(L,.50f),hx=Storybook.heroX(c,L),hy=Storybook.heroY(c,L);
        ui.touch(c,L,0,7,cardX,cardY);ui.touch(c,L,2,7,hx,hy);
        check("drag carries accessory without saving early",ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)<0);
        ui.touch(c,L,1,8,hx,hy);
        check("different pointer cannot finish drag",ui.dragging);
        ui.touch(c,L,1,7,hx,hy);
        check("drop on squishy equips",!ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)==1);
        ui.touch(c,L,0,7,L.w*.23f,cardY);ui.touch(c,L,2,7,0,0);ui.touch(c,L,1,7,0,0);
        check("drop outside preserves outfit",c.wardrobe.equipped(0,Accessories.HAT)==1 && c.storyOpen());
        ui.touch(c,L,0,7,L.w*.23f,cardY);ui.touch(c,L,2,7,hx,hy);ui.touch(c,L,3,7,hx,hy);
        check("cancelled drag preserves outfit",!ui.dragging && c.wardrobe.equipped(0,Accessories.HAT)==1);
        ui.choose(c,2);check("remove saves immediately",c.wardrobe.equipped(0,Accessories.HAT)<0);
        c.wardrobe.owned[0]=false;ui.choose(c,20);
        check("locked accessory cannot equip",c.wardrobe.equipped(0,Accessories.HAT)<0 && ui.selected==0);
        check("update notice waits for editor",!UpdatePrompt.suitable(c));
        int state=c.state;c.screenKey(0);
        check("game key dismisses inline wardrobe without starting",c.state==state && !c.starting() && !ui.open && !c.storyOpen() && c.caseOpen);
        c.openStory();
        Pause.back(c);check("back closes story and inline wardrobe together",!ui.open && !c.storyOpen());
        c.state=GameCore.PLAY;ui.update(c,.1f);check("reward waits until title",!ui.open);
        c.state=GameCore.TITLE;c.onboarding.titleGuide=true;ui.update(c,.1f);
        check("reward never interrupts tutorial",!ui.open);
        for(int[] size:new int[][]{{320,568},{640,1400},{1080,2340}}) {
            Layout small=new Layout();small.compute(size[0],size[1],0,20,0,24);
            SettingsUi settings=new SettingsUi();settings.compute(small,SettingsUi.PROGRESS);
            check("developer accessory chips stay in panel",settings.accessoryY+settings.testH<settings.panelB);
            check("editor buttons fit portrait",WardrobeScreen.y(small,.89f)<small.h-small.padB);
            check("inline tray stays below story and above inset",WardrobeScreen.trayTop(small)>small.h*.47f+small.unit*6.2f && WardrobeScreen.trayY(small,1)<small.h-small.padB);
        }
    }
}
