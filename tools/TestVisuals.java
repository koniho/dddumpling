package com.dddumpling.game;

/** Sky and clouds, the lock indicator, the edge glow and the settings panel. */
final class TestVisuals extends Check {

    private static void releaseWrap(Layout L) {
        int[] original=ReleaseChange.ITEMS[0];
        int columns=ReleaseNotes.columns(L);
        float oldNext=ReleaseNotes.groupY(L,1)-ReleaseNotes.listTop(L);
        try {
            int[] many=new int[columns*9+2];
            for(int i=0;i<many.length;i++) many[i]=original[i%original.length];
            ReleaseChange.ITEMS[0]=many;
            GameCore c=new GameCore(new Mem(),7201L);ReleaseNotes n=c.releaseNotes;
            n.show(c,L);n.update(ReleaseTransition.DURATION,L);
            check("overflow icons wrap to the same left edge",ReleaseNotes.iconX(L,0,columns)==ReleaseNotes.iconX(L,0,0)
                    && ReleaseNotes.itemY(L,0,columns)>ReleaseNotes.itemY(L,0,0));
            check("wrapped rows push the next release down",ReleaseNotes.groupY(L,1)-ReleaseNotes.listTop(L)>oldNext
                    && ReleaseNotes.itemY(L,0,many.length-1)+ReleaseNotes.size(L)*1.5f<ReleaseNotes.groupY(L,1));
            boolean fits=true;
            for(int i=0;i<many.length;i++) fits &= ReleaseNotes.iconX(L,0,i)+ReleaseNotes.size(L)*1.4f<=L.w*.91f+.01f;
            check("wrapped icons all fit the panel width",fits);
            float ix=ReleaseNotes.iconX(L,0,0),iy=ReleaseNotes.rowY(L,0);
            n.handleTouch(c,L,0,ix,iy);n.handleTouch(c,L,2,ix,iy-L.h*.4f);n.handleTouch(c,L,1,ix,iy-L.h*.4f);
            check("wrapped content scrolls without selecting",n.listing && n.listScroll>0f);
            for(int[] target:new int[][]{{0,columns},{0,many.length-1},{1,0},{2,ReleaseChange.ITEMS[2].length-1}}) {
                float row=ReleaseNotes.itemY(L,target[0],target[1]);
                n.listScroll=Math.max(0f,Math.min(ReleaseNotes.maxScroll(L),row-(ReleaseNotes.listTop(L)+ReleaseNotes.listBottom(L))*.5f));
                float scroll=n.listScroll,x=ReleaseNotes.iconX(L,target[0],target[1]),y=row-scroll;
                n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,1,x,y);
                check("wrapped icon opens exact feature "+target[0]+"/"+target[1],!n.listing && n.page==target[0] && n.feature==target[1]);
                n.update(ReleaseNotes.PAGE_TIME,L);n.back();n.update(ReleaseNotes.PAGE_TIME,L);
                check("wrapped list keeps scroll after back "+target[0]+"/"+target[1],n.listing && n.listScroll==scroll);
            }
        } finally { ReleaseChange.ITEMS[0]=original; }
    }

    private static void releaseBook(Layout L) {
        releaseHistory();
        releaseOutside(L);
        releaseFeedback(L);
        try(ReleaseExamples examples=new ReleaseExamples()) { releaseBookExamples(L); }
    }
    private static void releaseFeedback(Layout L) {
        GameCore c=new GameCore(new Mem(),7205L);Ear ear=new Ear();c.sound=ear;ReleaseNotes n=c.releaseNotes;
        n.close();check("closed book has no feedback",ear.uiBloops==0 && n.takeFeedback()==0);
        n.show(c,L);n.show(c,L);
        check("opening book gives one bloop and haptic",ear.uiBloops==1 && n.takeFeedback()==1 && n.takeFeedback()==0);
        n.handleTouch(c,L,0,0,0);n.handleTouch(c,L,1,0,0);
        check("opening animation ignores feedback",ear.uiBloops==1 && n.takeFeedback()==0);
        n.update(ReleaseTransition.DURATION,L);
        float x=ReleaseNotes.iconX(L,0,0),y=ReleaseNotes.rowY(L,0);
        n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,2,x,y-20f*ReleaseNotes.size(L));
        n.handleTouch(c,L,1,x,y-20f*ReleaseNotes.size(L));
        check("list scrolling is silent",ear.uiBloops==1 && n.takeFeedback()==0);
        n.listScroll=0f;n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,1,x,y);
        check("entering item gives one bloop and haptic",ear.uiBloops==2 && n.takeFeedback()==1);
        n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,1,x,y);
        check("item animation ignores feedback",ear.uiBloops==2 && n.takeFeedback()==0);
        n.update(ReleaseNotes.PAGE_TIME,L);n.back();n.update(ReleaseNotes.PAGE_TIME,L);
        n.handleTouch(c,L,0,0,0);n.close();
        check("outside exit gives exactly one bloop and haptic",ear.uiBloops==3 && n.takeFeedback()==1);
        n.update(ReleaseTransition.DURATION,L);n.close();
        check("exit completion stays silent",ear.uiBloops==3 && n.takeFeedback()==0);
        n.show(c,L);n.update(ReleaseTransition.DURATION,L);n.takeFeedback();n.back();
        check("back exit also gives feedback",ear.uiBloops==5 && n.takeFeedback()==1);
    }
    private static void releaseOutside(Layout L) {
        float[][] outside={{L.w*.02f,(ReleaseNotes.listTop(L)+ReleaseNotes.listBottom(L))*.5f},
                {L.w*.98f,ReleaseNotes.rowY(L,0)}, {L.w*.5f,ReleaseNotes.top(L)-1f},
                {L.w*.5f,ReleaseNotes.bottom(L)+1f}};
        for(float[] point:outside) {
            GameCore c=new GameCore(new Mem(),7203L);ReleaseNotes n=c.releaseNotes;
            n.show(c,L);n.update(ReleaseTransition.DURATION,L);
            check("outside list press is consumed",n.handleTouch(c,L,0,point[0],point[1]));
            check("outside list starts existing exit",n.open && n.transition.closing);
            n.update(ReleaseTransition.DURATION,L);
            check("held closing gesture still owns input",n.handleTouch(c,L,2,L.keyX[0],L.keyY[0]));
            check("closing lift is consumed",n.handleTouch(c,L,1,L.keyX[0],L.keyY[0]));
            check("fresh title input is released",!n.handleTouch(c,L,0,L.keyX[0],L.keyY[0]));
        }
        GameCore c=new GameCore(new Mem(),7204L);ReleaseNotes n=c.releaseNotes;
        n.show(c,L);n.update(ReleaseTransition.DURATION,L);
        n.handleTouch(c,L,0,L.w*.5f,ReleaseNotes.top(L)+ReleaseNotes.size(L));
        n.handleTouch(c,L,1,L.w*.5f,ReleaseNotes.top(L)+ReleaseNotes.size(L));
        check("inside list header stays open",n.open && !n.transition.closing);
        float x=ReleaseNotes.iconX(L,0,0),y=ReleaseNotes.rowY(L,0);
        n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,2,x,ReleaseNotes.top(L)-5f);
        n.handleTouch(c,L,1,x,ReleaseNotes.top(L)-5f);
        check("scroll ending outside does not dismiss",n.listing && !n.transition.closing);
        n.listScroll=0f;n.handleTouch(c,L,0,x,y);n.handleTouch(c,L,1,x,y);
        n.update(ReleaseNotes.PAGE_TIME,L);
        n.handleTouch(c,L,0,0,0);n.handleTouch(c,L,1,0,0);
        check("feature outside taps retain existing behavior",!n.listing && !n.transition.closing);
    }
    private static void releaseHistory() {
        for(int[] dimensions:new int[][]{{320,568},{393,852},{640,1400},{1080,2400},{852,393}}) {
            Layout bounds=new Layout();bounds.compute(dimensions[0],dimensions[1],0,0,0,0);
            check("release list clears dotted line "+dimensions[0],ReleaseNotes.bottom(bounds)<bounds.dangerY
                    && ReleaseNotes.top(bounds)>=bounds.topSafe);
            int last=ReleaseNotes.VERSIONS.length-1;
            float lastBottom=ReleaseNotes.itemY(bounds,last,ReleaseChange.ITEMS[last].length-1)+ReleaseNotes.size(bounds)*1.4f;
            check("oldest release fits after scroll "+dimensions[0],lastBottom-ReleaseNotes.maxScroll(bounds)<=ReleaseNotes.listBottom(bounds));
        }
        Layout L=new Layout();L.compute(852,393,0,0,0,0);
        GameCore history=new GameCore(new Mem(),7202L);ReleaseNotes n=history.releaseNotes;
        n.show(history,L);n.update(ReleaseTransition.DURATION,L);
        int oldest=ReleaseNotes.VERSIONS.length-1;
        float x=ReleaseNotes.iconX(L,0,0),y=ReleaseNotes.rowY(L,0);
        n.handleTouch(history,L,0,x,y);
        n.handleTouch(history,L,2,x,y-L.h*ReleaseNotes.VERSIONS.length);
        n.handleTouch(history,L,1,x,y-L.h*ReleaseNotes.VERSIONS.length);
        check("full history scroll reaches the oldest release",oldest>=3 && n.listing && n.listScroll>0f && n.listScroll==ReleaseNotes.maxScroll(L));
        float scroll=n.listScroll;
        x=ReleaseNotes.iconX(L,oldest,0);y=ReleaseNotes.itemY(L,oldest,0)-scroll;
        n.handleTouch(history,L,0,x,y);n.handleTouch(history,L,1,x,y);
        check("old release remains selectable below the first three",!n.listing && n.page==oldest);
        n.update(ReleaseNotes.PAGE_TIME,L);n.back();n.update(ReleaseNotes.PAGE_TIME,L);
        check("old release back preserves history position",n.listing && n.listScroll==scroll);
    }
    private static void releaseBookExamples(Layout L) {
        Mem seen=new Mem();seen.releaseSeen="older-build";
        GameCore news=new GameCore(seen,71L);
        news.settingsOpen=true;news.releaseMascot.update(news,.1f);
        check("news waits for visible title",!news.releaseMascot.unread && seen.releaseSeen.equals("older-build"));
        news.settingsOpen=false;news.releaseMascot.update(news,.1f);
        check("attention waits for an actual read before saving",news.releaseMascot.unread && seen.releaseSeen.equals("older-build"));
        check("attention leaves outside taps alone",!news.releaseNotes.handleTouch(news,L,0,L.w*.95f,L.h*.7f)
                && news.releaseMascot.unread && !Pause.handlesBack(news));
        news.screenKey(0);
        check("unread notes do not block starting a game",news.starting() && news.releaseMascot.unread);
        GameCore again=new GameCore(seen,72L);again.releaseMascot.update(again,.1f);
        check("unread build still attracts attention after restart",again.releaseMascot.unread);
        again.clock=.4f;float lid=again.releaseMascot.attentionLift(again.clock);
        check("unread lid pops repeatedly",lid>.4f && again.releaseMascot.attentionLift(2.6f)>.4f);
        again.releaseNotes.handleTouch(again,L,0,again.releaseMascot.x(L),again.releaseMascot.y(L));
        check("steamer opens release list and saves read status",again.releaseNotes.open && again.releaseNotes.listing
                && !again.releaseMascot.unread && seen.releaseSeen.equals(BuildFlags.BUILD_ID));
        check("attention enters without a position or lid jump",Math.abs(again.releaseNotes.transition.x(L)-again.releaseMascot.x(L))<.01f
                && Math.abs(again.releaseNotes.transition.y(L)-again.releaseMascot.y(L))<.01f
                && Math.abs(again.releaseNotes.transition.lidLift()-lid)<.01f);
        GameCore read=new GameCore(seen,73L);read.releaseMascot.update(read,.1f);
        check("read build stays quiet after restart",!read.releaseMascot.unread && read.releaseMascot.attentionLift(.4f)==0f);
        read.releaseMascot.reset(read);read.releaseMascot.update(read,.1f);
        check("reset restores attention for current build",seen.releaseSeen.equals("") && read.releaseMascot.unread);
        SettingsUi newsUi=new SettingsUi();newsUi.compute(L,SettingsUi.PROGRESS);
        check("reset news chip target",newsUi.hit(L.w*.5f,newsUi.difficultyY+newsUi.testH*.5f)==SettingsUi.HIT_RESET_NEWS);
        group("interactive release notes");
        releaseWrap(L);
        Mem save=new Mem();GameCore c=new GameCore(save,7100L),control=new GameCore(new Mem(),7100L);
        ReleaseNotes n=c.releaseNotes;
        check("book catalog preserves historical releases",ReleaseNotes.VERSIONS.length>=4
                && ReleaseNotes.VERSIONS.length==ReleaseChange.ITEMS.length
                && !ReleaseNotes.VERSIONS[0].equals(ReleaseNotes.VERSIONS[2]));
        n.show(c,L);
        check("release entrance starts at the steamer",n.transition.progress==0f && n.transition.listX(L)==L.w);
        n.handleTouch(c,L,0,ReleaseNotes.iconX(L,0,0),ReleaseNotes.rowY(L,0));
        n.handleTouch(c,L,1,ReleaseNotes.iconX(L,0,0),ReleaseNotes.rowY(L,0));
        check("moving release icons ignore taps",n.listing && n.demo==null);
        n.update(.15f,L);
        check("steamer tap lifts the lid",n.transition.lidLift()>.25f);
        n.update(ReleaseTransition.CENTRE*ReleaseTransition.DURATION-.15f,L);
        check("steamer reaches centre before the list moves",Math.abs(n.transition.x(L)-L.w*.5f)<.01f
                && Math.abs(n.transition.y(L)-L.h*.5f)<.01f && n.transition.listX(L)==L.w);
        n.update(ReleaseTransition.DURATION,L);
        check("steamer lid settles after its tap",Math.abs(n.transition.lidLift())<.001f);
        check("idle steam keeps rising",ReleaseMascot.steamPhase(.2f,0)<ReleaseMascot.steamPhase(.6f,0));
        float time=c.time,clock=c.clock;
        for(int release=0;release<ReleaseNotes.VERSIONS.length;release++) {
            int bugs=0;
            for(int id:ReleaseChange.ITEMS[release]) if(ReleaseContent.ICONS[id]==ReleaseChange.BUGS) bugs++;
            check("at most one bug group per release "+release,bugs<=1);
            check("release icons share a left edge "+release,ReleaseNotes.iconX(L,release,0)==ReleaseNotes.iconX(L,0,0));
        }
        check("book opens on the release list",n.open && n.listing && n.demo==null);
        float row=ReleaseNotes.rowY(L,0),icon=ReleaseNotes.iconX(L,0,0);
        n.handleTouch(c,L,0,icon,row);
        n.handleTouch(c,L,2,icon,row-L.h*.3f);
        n.handleTouch(c,L,1,icon,row-L.h*.3f);
        check("release swipe never opens an icon",n.listing && n.listScroll==ReleaseNotes.maxScroll(L));
        n.listScroll=0f;
        n.handleTouch(c,L,0,icon,row);
        n.handleTouch(c,L,5,icon,row);
        n.handleTouch(c,L,1,icon,row);
        check("an extra finger cancels icon selection",n.listing);
        n.handleTouch(c,L,0,L.w*.5f,ReleaseNotes.groupY(L,0)+ReleaseNotes.size(L)*.5f);
        n.handleTouch(c,L,1,L.w*.5f,ReleaseNotes.groupY(L,0)+ReleaseNotes.size(L)*.5f);
        check("release header is not clickable",n.listing);
        for(int release=0;release<ReleaseNotes.VERSIONS.length;release++)
            for(int feature=0;feature<ReleaseChange.ITEMS[release].length;feature++) {
                float ix=ReleaseNotes.iconX(L,release,feature),iy=ReleaseNotes.itemY(L,release,feature);
                n.listScroll=Math.max(0f,Math.min(ReleaseNotes.maxScroll(L),iy-(ReleaseNotes.listTop(L)+ReleaseNotes.listBottom(L))*.5f));
                iy-=n.listScroll;
                n.handleTouch(c,L,0,ix,iy);
                check("feature icon waits for finger lift "+release+"/"+feature,n.listing);
                n.handleTouch(c,L,1,ix,iy);
                check("feature icon opens its own page "+release+"/"+feature,!n.listing && n.page==release && n.feature==feature);
                check("feature selection starts a horizontal slide "+release+"/"+feature,n.pageMoving() && n.pageSlide==0f);
                n.update(ReleaseNotes.PAGE_TIME,L);
                check("feature slide settles before interaction "+release+"/"+feature,!n.pageMoving() && n.pageSlide==1f);
                n.back();n.update(ReleaseNotes.PAGE_TIME,L);
            }
        Layout shortL=new Layout();shortL.compute(852,393,0,0,0,0);
        float shortRow=ReleaseNotes.rowY(shortL,0),sx=ReleaseNotes.iconX(shortL,0,0);
        n.handleTouch(c,shortL,0,sx,shortRow);
        n.handleTouch(c,shortL,2,sx,shortRow-ReleaseNotes.maxScroll(shortL)-shortL.h);
        n.handleTouch(c,shortL,1,sx,shortRow-ReleaseNotes.maxScroll(shortL)-shortL.h);
        check("short screens scroll the compact release groups",n.listing && n.listScroll>0f && n.listScroll==ReleaseNotes.maxScroll(shortL));
        float savedScroll=n.listScroll;
        n.select(2,shortL);n.update(ReleaseNotes.PAGE_TIME,shortL);n.back();n.update(ReleaseNotes.PAGE_TIME,shortL);
        check("feature back preserves release scroll",n.listScroll==savedScroll);
        n.listScroll=0f;
        for(int[] dimensions:new int[][]{{393,852},{918,2048},{1080,2400},{852,393}}) {
            Layout box=new Layout();box.compute(dimensions[0],dimensions[1],0,0,0,0);
            n.select(0,1,box);float small=n.windowHeight(box);
            n.select(2,box);
            check("feature window follows content "+dimensions[0],n.windowHeight(box)>small
                    && n.windowTop(box)>=box.topSafe && n.windowBottom(box)<=box.h-box.padB
                    && n.demoBottom(box)<n.windowBottom(box)-ReleaseNotes.size(box)*3f);
        }
        n.select(0,L);n.update(ReleaseNotes.PAGE_TIME,L);
        c.screenKey(0);c.update(0.2f,L);
        check("book keeps the title animating while swallowing keys",!c.starting() && c.time>time && c.clock>clock && c.state==GameCore.TITLE);
        n.touch(c,L,L.w*0.75f,(n.demoTop(L)+n.demoBottom(L))*.5f);
        check("land illustration starts a real journey",n.demo.landTravelFrom>=0);
        GameCore travelDemo=n.demo;int destination=n.demo.landChoice;
        n.update(ReleaseNotes.RESTART_DELAY+.1f,L);
        check("land illustration keeps its destination after two seconds",n.demo==travelDemo
                && n.demo.landChoice==destination && n.demo.landTravelFrom<0);
        n.update(3f,L);
        check("land illustration does not reset while idle",n.demo==travelDemo && n.demo.landChoice==destination);
        n.select(1,L);n.update(ReleaseNotes.PAGE_TIME,L);n.touch(c,L,L.w*0.5f,(n.demoTop(L)+n.demoBottom(L))*.5f);
        check("mystery illustration begins its shuffle",n.demo.power.hit);
        n.update(Power.SELECT_TIME+0.1f,L);
        check("mystery illustration reveals its choice",n.demo.power.shownEffect()==n.demo.power.effect);
        n.update(ReleaseNotes.RESTART_DELAY-Power.SELECT_TIME-.1f+.001f,L);
        check("shuffle restarts as an uncollected pickup",!n.demo.power.hit);
        int shuffleItem=n.item();boolean resetChoice=ReleaseContent.AUTO_RESET[shuffleItem];
        try {
            ReleaseContent.AUTO_RESET[shuffleItem]=false;
            n.touch(c,L,L.w*.5f,(n.demoTop(L)+n.demoBottom(L))*.5f);
            GameCore heldDemo=n.demo;n.update(ReleaseNotes.RESTART_DELAY+.1f,L);
            check("entry can opt out of shuffle reset",n.demo==heldDemo && n.demo.power.hit);
        } finally { ReleaseContent.AUTO_RESET[shuffleItem]=resetChoice; }

        n.select(2,L);n.update(ReleaseNotes.PAGE_TIME,L);n.select(ReleaseNotes.VERSIONS.length,L);check("invalid release cannot replace the selected page",n.page==2);
        GameCore.Enemy a=n.demo.enemies.get(0),b=a.link;
        float ax=L.w*0.08f+n.demo.enemyCentreX(a),bx=L.w*0.08f+n.demo.enemyCentreX(b);
        float y=n.demoTop(L)+a.y;
        n.touch(c,L,ax,y);n.update(0.1f,L);
        check("one illustrated key flexes the bond",a.linkWaiting && a.linkStrain>0f);
        n.touch(c,L,bx,y);check("illustrated pair accepts the real chord",a.destroyed && b.destroyed);
        n.update(1.99f,L);
        check("pair destruction remains visible before restart",n.demo.enemies.get(0)==a && a.destroyed);
        n.update(.011f,L);
        check("pair automatically returns intact",n.demo.enemies.get(0)!=a && !n.demo.enemies.get(0).destroyed
                && n.demo.enemies.get(0).link!=null);
        check("demo state never changes saved progress",c.score==0 && c.collected==control.collected
                && save.saves==0 && save.collectedSaves==0 && c.rnd.nextLong()==control.rnd.nextLong());
        check("detail back starts a reverse slide",Pause.handlesBack(c) && Pause.back(c) && n.open && n.pageReturning && n.demo!=null);
        n.update(ReleaseNotes.PAGE_TIME,L);
        check("detail back restores the release list",n.listing && n.demo==null);
        n.select(0,L);n.update(ReleaseNotes.PAGE_TIME*.4f,L);
        float pageTravel=n.pageTravel();
        n.touch(c,L,L.w*.75f,(n.demoTop(L)+n.demoBottom(L))*.5f);
        check("moving feature demo ignores taps",n.demo.landTravelFrom<0);
        n.back();
        check("feature back reverses from its current position",n.pageTravel()==pageTravel && n.pageReturning);
        n.update(ReleaseNotes.PAGE_TIME,L);
        check("interrupted feature returns to list",n.listing && n.open && !n.transition.closing);
        check("list back begins the reverse transition",Pause.back(c) && n.open && n.transition.closing);
        float lastListX=-1f,lastSteamerX=-L.w;
        for(int step=0;step<=4;step++) {
            if(step>0) n.update(ReleaseTransition.DURATION/10f,L);
            check("closing steamer stays corner-sized and level "+step,
                    Math.abs(n.transition.radius(L)-c.releaseMascot.radius(L))<.001f
                    && Math.abs(n.transition.y(L)-c.releaseMascot.y(L))<.001f);
            check("closing list slides right and steamer travels home "+step,
                    n.transition.listX(L)>lastListX && n.transition.x(L)>lastSteamerX
                    && n.transition.x(L)<=c.releaseMascot.x(L));
            lastListX=n.transition.listX(L);lastSteamerX=n.transition.x(L);
        }
        c.update(ReleaseTransition.DURATION*.1f+.00001f,L);
        check("half-duration exit returns steamer to corner",!n.open && !c.releaseMascot.unread && c.releaseMascot.x(L)==L.unit*2.5f);
        n.show(c,L);n.update(ReleaseTransition.DURATION,L);
        n.handleTouch(c,L,0,L.w*0.9f,n.closeY(L));
        check("close consumes the rest of its touch gesture",n.open && n.transition.closing && n.handleTouch(c,L,5,L.w*.5f,L.h*.95f)
                && n.handleTouch(c,L,1,L.w*.5f,L.h*.95f) && !c.starting());
        c.update(ReleaseTransition.DURATION,L);
        n.show(c,L);n.update(ReleaseTransition.DURATION*.7f,L);
        float beforeX=n.transition.x(L),beforeListX=n.transition.listX(L);
        n.back();
        check("back during entrance reverses without a snap",n.transition.closing
                && n.transition.x(L)==beforeX && n.transition.listX(L)==beforeListX);
        c.update(ReleaseTransition.DURATION,L);
        check("interrupted entrance finishes closed",!n.open);
        c.state=GameCore.PLAY;n.show(c,L);
        check("book cannot open over an active run",!n.open);
    }

    private static void discoveryTrip(Layout L) {
        Mem store=new Mem();GameCore c=new GameCore(store,723L);Ear ear=new Ear();c.sound=ear;
        for(int land=1;land<Lands.COUNT;land++) c.collected=Collect.add(c.collected,Collect.BOSS_FIRST+land-1);
        store.collected=c.collected;
        LandPicker.updateDiscovery(c,0f);
        check("discovery uses the swipe traveler size",LandPicker.travelerRadius(c,L)==L.keyR*c.keyScale()*.55f);
        check("tour begins from the current land",c.landDiscovery==1 && c.landDiscoveryFrom==0 && c.landPickerSlide==1f);
        check("tour owns its land motion",!LandPicker.down(c,L,L.w*.7f,LandPicker.cardY(L)));
        for(int land=1;land<Lands.COUNT;land++) {
            check("tour visits each new land in order "+land,c.landDiscovery==land && c.landDiscoveryFrom==land-1);
            check("no glow or seen flag before arrival "+land,LandDiscovery.glow(c,land)==0f && (c.landSeen&(1<<land))==0);
            LandPicker.updateDiscovery(c,LandDiscovery.arrival(c)*.5f);
            check("discovery follows the trail meander "+land,Math.abs(LandDiscovery.arc(c,L)
                    -LandPicker.walkingDip(LandDiscovery.walk(c))*LandPicker.iconRadius(c,L))<.001f);
            LandPicker.updateDiscovery(c,LandDiscovery.arrival(c)*.5f+.001f);
            check("arrival persists its newly seen land "+land,(store.landState&(1<<land))!=0 && c.landPickerSlide==0f);
            LandPicker.updateDiscovery(c,LandDiscovery.HOLD*.35f);
            check("arrival glows only on its own land "+land,LandDiscovery.glow(c,land)>.5f && LandDiscovery.glow(c,0)==0f);
            float age=c.landDiscoveryT;c.caseOpen=true;LandPicker.updateDiscovery(c,1f);
            check("covered tour pauses its arrival glow "+land,c.landDiscoveryT==age);c.caseOpen=false;
            float x=LandDiscovery.x(c,L),ground=LandDiscovery.ground(c,L);
            LandPicker.updateDiscovery(c,LandDiscovery.HOLD);
            if(land<Lands.COUNT-1) check("next journey joins without a position jump "+land,
                    c.landDiscoveryChained && Math.abs(LandDiscovery.x(c,L)-x)<.01f
                    && Math.abs(LandDiscovery.ground(c,L)-ground)<.01f);
        }
        check("tour ends on the final land",c.landDiscovery==-1 && c.landChoice==Lands.COUNT-1 && c.landSeen==(1<<Lands.COUNT)-2 && ear.landShuffles==Lands.COUNT-1);
        LandPicker.step(c,-1);LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME);LandPicker.updateDiscovery(c,1f);
        check("revisiting a discovered land does not glow again",c.landDiscovery==-1 && LandDiscovery.glow(c,2)==0f);
        GameCore reload=new GameCore(store,724L);LandPicker.updateDiscovery(reload,1f);
        check("all discovered lands stay seen after restart",reload.landSeen==(1<<Lands.COUNT)-2 && reload.landDiscovery==-1);
        GameCore gap=new GameCore(new Mem(),725L);gap.collected=c.collected;gap.landSeen=4;
        LandPicker.updateDiscovery(gap,0f);LandPicker.updateDiscovery(gap,LandDiscovery.arrival(gap)+LandDiscovery.HOLD);
        check("tour traverses an already-seen intermediate land",gap.landDiscovery==2 && !gap.landDiscoveryFresh);
        LandPicker.updateDiscovery(gap,LandDiscovery.arrival(gap)+LandDiscovery.HOLD*.5f);
        check("intermediate land does not repeat its discovery glow",LandDiscovery.glow(gap,2)==0f);
        LandPicker.updateDiscovery(gap,LandDiscovery.HOLD);
        check("tour continues to the next unseen land",gap.landDiscovery==3 && gap.landDiscoveryChained);

    }

    private static void caseScoreFade() {
        Layout phone = new Layout();
        phone.compute(1290, 2796, 0, 0, 0, 0);
        GameCore c = new GameCore(new Mem(), 728L);
        c.best = 1840;
        final int[] alpha = new int[2];
        Painter p = (Painter) java.lang.reflect.Proxy.newProxyInstance(
                Painter.class.getClassLoader(), new Class<?>[] {Painter.class}, (proxy, method, args) -> {
                    if (method.getName().equals("text")) {
                        int a = (Integer) args[4] >>> 24;
                        if (args[0].equals("BEST 1840")) alpha[0] = Math.max(alpha[0], a);
                        if (args[0].equals("DISPLAY CASE")) alpha[1] = Math.max(alpha[1], a);
                    }
                    return null;
                });
        for (boolean opening : new boolean[] {true, false}) {
            c.caseOpen = opening;
            for (int step = 0; step <= 20; step++) {
                c.caseFade = (opening ? step : 20 - step) / 20f;
                alpha[0] = alpha[1] = 0;
                Screens.title(p, c, phone);
                if (c.caseFade == 0f) check("closed case restores Best Score", alpha[0] == 255);
                if (c.caseFade == .25f) check("Best Score fades with the closing content",
                        alpha[0] > 0 && alpha[0] < 255);
                if (c.caseFade >= .5f) check("Best Score is gone before the open-case heading",
                        alpha[0] == 0);
                if (c.caseFade == 1f) check("open display case retains its heading", alpha[1] > 0);
            }
        }
    }

    static void persistentExplorer(Layout L) {
        GameCore c=new GameCore(new Mem(),725L);
        c.collected=Collect.MASK;c.landSeen=LandPicker.stateMask();
        LandPicker.updateTravel(c,1.3f);
        float x=LandPicker.explorerBodyX(c,L),y=LandPicker.explorerBodyY(c,L);
        check("resident explores within the emblem",Math.abs(x-L.w*.5f)>.1f && LandPicker.explorerScale(c)==.42f);
        LandPicker.select(c,3);
        check("departing preserves resident position",Math.abs(x-LandPicker.explorerBodyX(c,L))<.001f
                && Math.abs(y-LandPicker.explorerBodyY(c,L))<.001f && LandPicker.explorerScale(c)==.42f);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.99f);
        check("distant destination does not shrink at intermediate land",LandPicker.explorerScale(c)==1f);
        float before=LandPicker.explorerBodyX(c,L);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.01f+.00001f);
        check("queued leg joins without position or scale jump",c.landTravelFrom==1 && LandPicker.explorerScale(c)==1f
                && Math.abs(before-LandPicker.explorerBodyX(c,L))<L.w*.01f);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.5f);
        check("intermediate journey keeps moving",LandPicker.travelWalk(c)>.49f && LandPicker.travelWalk(c)<.51f);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*1.5f);
        check("final arrival becomes a small resident",c.landChoice==3 && c.landTravelFrom<0 && LandPicker.explorerScale(c)==.42f);
        x=LandPicker.explorerBodyX(c,L);y=LandPicker.explorerBodyY(c,L);
        LandPicker.updateTravel(c,.001f);
        check("arrival joins idle wandering continuously",Math.abs(x-LandPicker.explorerBodyX(c,L))<.01f
                && Math.abs(y-LandPicker.explorerBodyY(c,L))<.01f);
        boolean inside=true;
        for(int i=0;i<600;i++) {
            LandPicker.updateTravel(c,DT);
            inside &= Math.abs(LandPicker.explorerBodyX(c,L)-LandPicker.cardX(c,L,3))<LandPicker.iconRadius(c,L)*.5f;
            inside &= Math.abs(LandPicker.explorerBodyY(c,L)-LandPicker.cardY(c,L,3))<LandPicker.iconRadius(c,L)*.6f;
        }
        check("wandering remains inside the selected land",inside);
        LandPicker.select(c,0);LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.4f);LandPicker.select(c,2);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*6f);
        check("retargeting completes the queued route",c.landChoice==2 && c.landTravelFrom<0 && c.landTravelQueue.isEmpty());
        LandPicker.select(c,0);c.caseOpen=true;LandPicker.updateTravel(c,DT);c.caseOpen=false;
        check("covered journey returns to a visible resident",c.landTravelFrom<0 && LandPicker.explorerScale(c)==.42f);
        c.landChoice=0;LandPicker.select(c,1);LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.9f);
        float scale=LandPicker.explorerScale(c);LandPicker.select(c,3);
        check("late retarget preserves the current size",Math.abs(scale-LandPicker.explorerScale(c))<.0001f);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.1f+.00001f);
        check("late retarget grows into the next uninterrupted leg",LandPicker.explorerScale(c)==1f);

    }

    static void titleScreen(Layout L) {
        caseScoreFade();
        discoveryTrip(L);
        persistentExplorer(L);
        GameCore trail=new GameCore(new Mem(),724L);
        float r=LandPicker.iconRadius(trail,L);
        trail.collected=Collect.MASK;trail.landSeen=14;
        for(int direction:new int[]{1,-1}) {
            trail.landTravelFrom=direction>0 ? 0 : 1;
            trail.landChoice=direction>0 ? 1 : 0;
            for(float time:new float[]{0f,.30f,.5f,.70f,1f}) {
                trail.landTravelT=time*LandPicker.TRAVEL_TIME;
                float from=LandPicker.cardX(trail,L,trail.landTravelFrom);
                float to=LandPicker.cardX(trail,L,trail.landChoice);
                check("explorer crosses between land centres "+direction+" "+time,
                        Math.abs(LandPicker.travelX(trail,L)-(from+(to-from)*time))<.001f);
                check("explorer stays fully visible in travel "+direction+" "+time,
                        LandPicker.explorerScale(trail)>=.42f && LandPicker.explorerScale(trail)<=1f);
            }
        }
        trail.landChoice=1;trail.landTravelFrom=-1;trail.landDiscovery=-1;
        float fixedX=LandPicker.trailX(trail,L,1,.35f),fixedY=LandPicker.trailY(trail,L,1,.35f);
        trail.landTravelFrom=2;
        check("leftward travel cannot reshape the trail",LandPicker.trailX(trail,L,1,.35f)==fixedX
                && LandPicker.trailY(trail,L,1,.35f)==fixedY);
        trail.landTravelFrom=-1;trail.landDiscoveryFrom=1;trail.landDiscovery=2;trail.landDiscoveryChained=true;
        check("discovery cannot reshape the trail",LandPicker.trailX(trail,L,1,.35f)==fixedX
                && LandPicker.trailY(trail,L,1,.35f)==fixedY);
        for(int land=0;land<Lands.COUNT;land++) {
            float lx=LandPicker.cardX(trail,L,land),ly=LandPicker.cardY(trail,L,land);
            check("trail excludes the land silhouette "+land,LandPicker.trailCovered(trail,L,land,lx,ly+r*.8f));
            check("trail is visible outside the land edge "+land,!LandPicker.trailCovered(trail,L,land,lx+r*2f,ly));
        }

        releaseBook(L);
        group("title choreography");
        Mem landStore = new Mem();
        landStore.best = 123;
        GameCore lands = new GameCore(landStore, 717L);
        check("new players have no land picker", !LandPicker.visible(lands));
        check("locked land cannot be selected", !LandPicker.unlocked(lands, 1));
        landStore.collected = lands.collected = Collect.add(lands.collected, Collect.BOSS_FIRST);
        check("a boss friend unlocks its next land", LandPicker.visible(lands) && LandPicker.count(lands) == 2);
        LandPicker.select(lands, 2);
        check("later locked lands remain unavailable", lands.landChoice == 0);
        int titleColor = Lands.background(lands);
        LandPicker.select(lands, 1);
        for (int frame = 0; frame < 180; frame++) lands.update(DT, L);
        check("land selection leaves the title palette unchanged", Lands.background(lands) == titleColor);
        for (int layer = 0; layer < GameCore.CLOUD_LAYERS; layer++)
            check("selection preserves intro clouds " + layer, Lands.cloudTint(lands, layer) == Sky.CLOUD_TINT[layer]);
        check("the removed play button has no hit target", !LandPicker.down(lands, L, L.w * 0.5f, L.h * 0.62f));
        lands.screenKey(Kawaii.DUMPLING);
        check("a character key starts the chosen land", lands.starting());
        for (int frame = 0; frame < 180 && lands.state == GameCore.TITLE; frame++) lands.update(DT, L);
        check("land starts are fresh runs at their actual stage", lands.stage == 6 && lands.score == 0
                && lands.lives == GameCore.START_LIVES && !lands.boss.active() && lands.runStartLand == 1);
        check("the chosen land fades in only after play starts", lands.landBlend == 0f
                && Lands.background(lands) == Draw.BG);
        check("land difficulty uses the selected stage", lands.travelSeconds() == Pacing.travelSeconds(6));
        lands.score = 456;
        LandPicker.recordBest(lands);
        check("land scores stay out of the full-run record", landStore.best == 123 && landStore.landBests[1] == 456);
        GameCore fresh = new GameCore(landStore, 718L);
        check("app launch defaults to full run while retaining unlocks and records", fresh.landChoice == 0
                && fresh.best == 123 && fresh.landBests[1] == 456 && LandPicker.visible(fresh));
        LandPicker.down(fresh, L, L.w * 0.6f, LandPicker.cardY(L));
        LandPicker.move(fresh, L, L.w * 0.3f);
        LandPicker.up(fresh, L, L.w * 0.3f, LandPicker.cardY(L));
        check("swiping selects only an unlocked postcard", fresh.landChoice == 1 && !fresh.landPickerDragging);
        LandPicker.updateTravel(fresh,LandPicker.TRAVEL_TIME);
        float firstX = LandPicker.cardX(fresh, L, 0);
        LandPicker.down(fresh, L, firstX, LandPicker.cardY(L));
        LandPicker.up(fresh, L, firstX, LandPicker.cardY(L));
        check("separated icons select the nearest centre", fresh.landChoice == 0);
        LandPicker.updateTravel(fresh,LandPicker.TRAVEL_TIME);
        GameCore travel=new GameCore(new Mem(),721L);
        for(int i=0;i<Lands.COUNT-1;i++) travel.collected=Collect.add(travel.collected,Collect.BOSS_FIRST+i);
        travel.landSeen=(1<<Lands.COUNT)-2;
        Ear travelEar=new Ear();travel.sound=travelEar;
        float row=LandPicker.cardY(L),start=L.w*0.8f;
        LandPicker.down(travel,L,start,row);
        LandPicker.move(travel,L,L.w*0.1f);
        LandPicker.move(travel,L,0f);
        LandPicker.up(travel,L,0f,row);
        check("one long swipe moves exactly one land",travel.landChoice==1 && travel.landTravelQueue.isEmpty() && travelEar.landShuffles==1);
        check("rightward travel starts inside the previous land",Math.abs(LandPicker.travelX(travel,L)-LandPicker.cardX(travel,L,0))<.001f);
        LandPicker.step(travel,1);
        LandPicker.step(travel,-1);
        check("rapid swipes queue distinct journeys",travel.landChoice==1 && travel.landTravelQueue.size()==2 && travelEar.landShuffles==1);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        check("each queued land gets a fresh travel animation",travel.landChoice==2 && travel.landTravelFrom==1 && travelEar.landShuffles==2);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        check("leftward travel starts on the fixed trail at the previous land",travel.landChoice==1
                && Math.abs(LandPicker.travelX(travel,L)-LandPicker.cardX(travel,L,2))<.001f);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        check("travel settles precisely on the selected land",travel.landTravelFrom<0 && travel.landPickerSlide==0f && travelEar.landShuffles==3);
        LandPicker.select(travel,0);LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        LandPicker.step(travel,-1);
        check("land boundary does not play a false journey",travel.landChoice==0 && travel.landTravelFrom<0 && travelEar.landShuffles==4);
        check("land icon centres leave room between full sized icons",LandPicker.spacing(travel,L)>LandPicker.iconRadius(travel,L)*2f);
        float high=LandPicker.cardY(travel,L,0),low=LandPicker.cardY(travel,L,1);
        check("land heights alternate with a thirty percent step", Math.abs(low-high-LandPicker.iconRadius(travel,L)*0.6f)<0.01f
                && LandPicker.cardY(travel,L,2)==high && LandPicker.cardY(travel,L,3)==low);
        travel.landWanderT=0f;LandPicker.step(travel,1);
        check("travel starts at the previous land height",LandPicker.travelGround(travel,L)==high && LandPicker.travelArc(travel,L)==0f);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME*0.5f);
        check("travel dips below the midpoint between land heights",Math.abs(LandPicker.travelGround(travel,L)-(high+low)*0.5f)<0.01f
                && LandPicker.travelArc(travel,L)>LandPicker.iconRadius(travel,L)*0.4f);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME*0.5f);
        check("travel lands at the destination height",Math.abs(LandPicker.travelGround(travel,L)-low)<0.01f && LandPicker.travelArc(travel,L)==0f);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        travel.landWanderT=0f;LandPicker.step(travel,-1);
        check("reverse travel starts at the lower land",LandPicker.travelGround(travel,L)==low);
        LandPicker.updateTravel(travel,LandPicker.TRAVEL_TIME);
        check("reverse travel reaches the upper land along its fixed trail",
                Math.abs(LandPicker.travelX(travel,L)-LandPicker.cardX(travel,L,0))<.01f);
        LandPicker.step(travel,1);LandPicker.step(travel,1);
        travel.caseOpen=true;LandPicker.updateTravel(travel,DT);
        check("leaving the picker clears pending journeys",travel.landTravelFrom<0 && travel.landTravelQueue.isEmpty() && travel.landPickerSlide==0f);
        fresh.caseOpen = true;
        check("the open display case hides the picker", !LandPicker.visible(fresh));

        fresh.caseOpen = false;fresh.landSeen=0;landStore.landState=0;
        LandPicker.updateDiscovery(fresh, 0.1f);
        check("new land begins a title discovery", fresh.landDiscovery == 1);
        float earlyReveal = LandPicker.discoveryReveal(fresh, L, 1);
        fresh.landDiscoveryT = LandDiscovery.arrival(fresh);
        LandPicker.updateDiscovery(fresh,0f);
        check("new icon fades in as the explorer approaches", earlyReveal < 0.5f
                && LandPicker.discoveryReveal(fresh, L, 1) > 0.99f);
        check("explorer arrives at the focused discovered land", Math.abs(LandPicker.explorerX(fresh,L)-L.w*.5f)<.01f);
        fresh.caseOpen = true;
        float discoveryAge = fresh.landDiscoveryT;
        LandPicker.updateDiscovery(fresh, 2f);
        check("covered discoveries wait for the title", fresh.landDiscoveryT == discoveryAge);
        fresh.caseOpen = false;
        LandPicker.updateDiscovery(fresh, 4.5f);
        GameCore seen = new GameCore(landStore, 719L);
        LandPicker.updateDiscovery(seen, 0.1f);
        check("completed discoveries survive app restarts", seen.landDiscovery == -1 && seen.landSeen == 2);
        long collection = seen.collected;
        LandPicker.reset(seen);
        GameCore reset = new GameCore(landStore, 720L);
        check("reset lands persists without clearing collection or scores", !LandPicker.visible(reset)
                && reset.collected == collection && reset.best == 123 && reset.landBests[1] == 456);
        LandPicker.reward(reset, Collect.BOSS_FIRST);
        LandPicker.updateDiscovery(reset, 0.1f);
        check("a repeat boss reward unlocks and rediscovers its land", LandPicker.unlocked(reset, 1)
                && reset.landDiscovery == 1);
        GameCore allLands=new GameCore(new Mem(),725L);
        long beforeUnlock=allLands.collected;
        LandPicker.enableAll(allLands);
        check("all lands chip enables every land without granting collectibles",
                LandPicker.count(allLands)==Lands.COUNT && allLands.collected==beforeUnlock);
        LandPicker.reset(allLands);
        check("reset lands clears the developer unlock",LandPicker.count(allLands)==1);
        check("trail leaves to the right before dipping toward the next land",
                LandPicker.walkingDip(.2f)<0f && LandPicker.walkingDip(.7f)>.6f);
        SettingsUi resetUi = new SettingsUi();
        resetUi.compute(L, SettingsUi.PROGRESS);
        check("all lands chip has its own hit target", resetUi.hit(
                (resetUi.testChipL(0,2)+resetUi.testChipR(0,2))*.5f,
                resetUi.debuffY+resetUi.testH*.5f)==SettingsUi.HIT_ALL_LANDS);
        check("reset lands chip has its own hit target", resetUi.hit(
                (resetUi.testChipL(1, 2) + resetUi.testChipR(1, 2)) / 2f,
                resetUi.debuffY + resetUi.testH / 2f) == SettingsUi.HIT_RESET_LANDS);

        boolean ordered = true;
        for (int i = 0; i < Demo.LEN; i++) {
            ordered &= Demo.ACQUIRE[i] < Demo.PRESS[i]
                    && Demo.PRESS[i] < Demo.fireAt(i)
                    && Demo.fireAt(i) < Demo.impactAt(i);
            if (i + 1 < Demo.LEN) ordered &= Demo.impactAt(i) < Demo.ACQUIRE[i + 1];
        }
        check("each lesson separates acquire, press, fire, impact and advance", ordered);
        check("the loop leaves a readable rest after the final impact",
                Demo.LOOP - Demo.impactAt(Demo.LEN - 1) > GameCore.DESTROY_TIME + 0.8f);
        check("the open case keeps its margin above the play-field floor",
                Showcase.panelBot(L) <= L.dangerY - Showcase.FIELD_MARGIN * L.unit + 0.5f);

        GameCore c = new GameCore(new Mem(), 73L);
        check("the powerup appears after the enemy has finished breaking apart",
                Demo.POWER_START > Demo.impactAt(Demo.LEN - 1) + GameCore.DESTROY_TIME);
        check("the collection burst finishes before the loop restarts",
                Demo.LOOP > Demo.POWER_START + Demo.POWER_TOUCH + Power.POP_TIME + 0.5f);
        c.clock = Demo.POWER_START - 0.01f;
        check("the powerup waits for its lesson", Demo.lessonPower(c, L) == null);
        c.clock = Demo.POWER_START + Demo.POWER_APPROACH;
        Power drifting = Demo.lessonPower(c, L);
        c.clock = Demo.POWER_START + Demo.POWER_TOUCH + 0.05f;
        Power caught = Demo.lessonPower(c, L);
        check("the powerup drifts across before the touch collects it",
                !drifting.hit && caught.hit && caught.x > drifting.x);
        check("the title powerup is only a demonstration", c.power == null && !c.powerActive());
        GameCore.Enemy demo = new GameCore.Enemy();
        demo.word = new int[Demo.LEN];
        demo.baseX = (L.playLeft + L.playRight) / 2f;
        demo.sway = 0f;
        c.clock = Demo.impactAt(0);
        float from = Demo.caretX(c, demo, L);
        c.clock += 0.08f;
        float moving = Demo.caretX(c, demo, L);
        float to = c.tileX(demo, 1, L);
        check("the demo chevron eases toward the next letter after impact",
                from < moving && moving < to);

        float ax = Screens.titleAnchorX(2, L), ay = Screens.titleAnchorY(2, L);
        c.titleTouchDown = true;
        c.titleTouchX = ax;
        c.titleTouchY = ay;
        advance(c, L, 0.18f);
        float kicked = Math.abs(c.titleSpringX[2]) + Math.abs(c.titleSpringY[2]);
        check("touching the logo kicks a spring body", kicked > L.unit * 0.12f);
        c.titleTouchDown = false;
        advance(c, L, 6f);
        boolean bounded = true, shaped = true;
        for (int i = 0; i < GameCore.TITLE_LETTERS; i++) {
            bounded &= Math.abs(c.titleSpringX[i]) <= L.unit * 2.41f
                    && Math.abs(c.titleSpringY[i]) <= L.unit * 2.41f;
            float shape = c.titleSpringShape(i, L);
            shaped &= shape >= 0.84f && shape <= 1.18f;
        }
        check("title springs stay tethered", bounded);
        check("spring squash and stretch stays drawable", shaped);
    }

    private static void backgroundShake(Layout L) {
        GameCore c = new GameCore(new Mem(), 8801L); c.startGame();
        c.clock = .137f; c.shake = 1.2f;
        final float[] offset = new float[2], backdrop = new float[2];
        final java.util.ArrayList<float[]> stack = new java.util.ArrayList<float[]>();
        final boolean[] seen = new boolean[4];
        Painter p = (Painter) java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[] {Painter.class}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("save")) stack.add(offset.clone());
            else if (name.equals("restore")) {
                float[] saved = stack.remove(stack.size() - 1);
                offset[0] = saved[0]; offset[1] = saved[1];
            } else if (name.equals("translate")) {
                offset[0] += (Float) args[0]; offset[1] += (Float) args[1];
            } else if (name.equals("fillRect") && !seen[0]) {
                seen[0] = true; backdrop[0] = offset[0]; backdrop[1] = offset[1];
                check("shaken background covers every screen edge", (Float) args[0] + offset[0] <= 0f
                        && (Float) args[1] + offset[1] <= 0f && (Float) args[2] + offset[0] >= L.w
                        && (Float) args[3] + offset[1] >= L.h);
            } else if (name.equals("clipRect") && (Float) args[1] <= 0f
                    && (Float) args[3] == L.deckTop && !seen[1]) {
                seen[1] = true;
                check("cloud clipping leaves no stationary edge strip", (Float) args[0] + offset[0] <= 0f
                        && (Float) args[2] + offset[0] >= L.w);
                check("background clouds move with screen shake", Math.abs(offset[0]) > 1f
                        && offset[0] == backdrop[0] && offset[1] == backdrop[1]);
            } else if (name.equals("line") && (Float) args[1] == L.dangerY && !seen[2]) {
                seen[2] = true;
                check("playfield shares the background shake", offset[0] == backdrop[0] && offset[1] == backdrop[1]);
            } else if (name.equals("text") && "SCORE".equals(args[0])) {
                seen[3] = true;
                check("score remains steady above the shaking scene", offset[0] == 0f && offset[1] == 0f);
            }
            return null;
        });
        Renderer.draw(p, c, L);
        check("shake regression inspected background, clouds, field and HUD", seen[0] && seen[1] && seen[2] && seen[3]);
        check("screen shake restores its drawing transform", stack.isEmpty() && offset[0] == 0f && offset[1] == 0f);
    }

    static void sky(Layout L) {
        backgroundShake(L);
        group("cloud sky");
        for (int land = 0; land < Lands.COUNT; land++) {
            int first = land * Boss.EVERY + 1, bossStage = first + Boss.EVERY - 1;
            for (int stage = first; stage <= bossStage; stage++)
                check("land stays consistent through stage " + stage, Lands.forStage(stage) == land);
            check("land previews its upcoming boss " + land, Boss.kindFor(bossStage) == (land < Boss.COUNT ? land : -1));
            check("each land has three skits " + land,
                    Lands.skitFor(first) != Lands.skitFor(first + 1)
                    && Lands.skitFor(first + 1) != Lands.skitFor(first + 2)
                    && Lands.skitFor(first) != Lands.skitFor(first + 2));
        }
        check("endless stages reuse scenery", Lands.forStage(26) == Lands.forStage(1));
        check("title uses the original palette", Lands.background(new GameCore(new Mem(), 1L)) == Draw.BG);
        GameCore c = new GameCore(new Mem(), 111L);
        c.startGame();

        GameCore fade = new GameCore(new Mem(), 119L);
        fade.startGame();
        int initial = Lands.background(fade);
        fade.jumpToStage(6, L);
        check("new land begins in the previous palette", Lands.background(fade) == initial);
        fade.update(Lands.FADE_TIME / 2f, L);
        int midway = Lands.background(fade);
        check("land colors interpolate", midway != initial && midway != Lands.BG[1]);
        float beforeSameLand = fade.landBlend;
        fade.jumpToStage(7, L);
        check("same-land stages do not restart the fade", fade.landBlend == beforeSameLand);
        fade.jumpToStage(11, L);
        check("interrupted color fade remains continuous", Lands.background(fade) == midway);
        for (int frame = 0; frame < 180; frame++) fade.update(1f / 60f, L);
        check("land fade reaches its destination", fade.landBlend == 1f
                && Lands.background(fade) == Lands.BG[2] && Lands.tint(fade) == Lands.TINT[2]);
        fade.startGame();
        check("new runs fade from the intro palette", Lands.background(fade) == Draw.BG
                && fade.landFrom == -1 && fade.landBlend == 0f);
        for (int layer = 0; layer < GameCore.CLOUD_LAYERS; layer++)
            check("intro cloud tint stays continuous for layer " + layer,
                    Lands.cloudTint(fade, layer) == Sky.CLOUD_TINT[layer]);
        for (int frame = 0; frame < 72; frame++) fade.update(1f / 60f, L);
        check("intro palette visibly blends into the first land",
                Lands.background(fade) != Draw.BG && Lands.background(fade) != Lands.BG[0]);
        for (int frame = 0; frame < 90; frame++) fade.update(1f / 60f, L);
        check("intro fade completes in the first land", Lands.background(fade) == Lands.BG[0]);

        check("three cloud layers", GameCore.CLOUD_LAYERS == 3);
        check("one layer in front, two behind", Sky.CLOUD_FRONT_LAYER == 2);

        boolean speedsRise = true;
        for (int l = 1; l < GameCore.CLOUD_LAYERS; l++) {
            if (GameCore.CLOUD_SPEED[l] <= GameCore.CLOUD_SPEED[l - 1]) speedsRise = false;
        }
        check("each layer drifts faster than the one behind it", speedsRise);
        check("all layers actually move", GameCore.CLOUD_SPEED[0] > 0f);

        // Phases must stay in 0..1 and wrap, never run away.
        boolean inRange = true, moved = true, wrapped = false;
        float[][] before = new float[GameCore.CLOUD_LAYERS][GameCore.CLOUDS_PER_LAYER];
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                before[l][i] = c.cloudPhase(l, i);
                if (before[l][i] < 0f || before[l][i] >= 1f) inRange = false;
            }
        }
        advance(c, L, 3f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float now = c.cloudPhase(l, i);
                if (now < 0f || now >= 1f) inRange = false;
                if (now == before[l][i]) moved = false;
            }
        }
        check("cloud phases stay inside 0..1", inRange);
        check("clouds drift with the clock", moved);

        // Long run: the front layer must wrap many times and stay bounded.
        advance(c, L, 400f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float v = c.cloudPhase(l, i);
                if (v < 0f || v >= 1f) inRange = false;
            }
        }
        // 400s at the front-layer speed is well over one full traversal.
        wrapped = 400f * GameCore.CLOUD_SPEED[Sky.CLOUD_FRONT_LAYER] > 1f;
        check("phases stay bounded over a long run", inRange);
        check("the front layer wraps repeatedly", wrapped);

        // Drift is a pure function of the clock, so two cores at the same time agree.
        GameCore d = new GameCore(new Mem(), 222L);
        // skyClock, not clock: the sky runs on its own accumulator so a frenzy can speed it
        // up without the drift jumping.
        d.skyClock = c.skyClock;
        boolean deterministic = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (Math.abs(d.cloudPhase(l, i) - c.cloudPhase(l, i)) > 1e-5f) {
                    deterministic = false;
                }
            }
        }
        check("cloud drift is identical for any core at the same clock", deterministic);

        // A cloud must be entirely out of the sky before its phase wraps, or it pops out
        // of existence mid-screen.
        boolean exitsCleanly = true, entersCleanly = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            float margin = Sky.cloudMargin(L, l);
            GameCore z = new GameCore(new Mem(), 333L);
            z.clock = 0f;
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                z.cloudY[l][i] = 0f;                 // phase 0: just entering
                if (Sky.cloudY(z, L, l, i) + margin > 0.5f) entersCleanly = false;
                z.cloudY[l][i] = 0.99999f;           // phase ~1: just leaving
                if (Sky.cloudY(z, L, l, i) - margin < L.deckTop - 0.5f) {
                    exitsCleanly = false;
                }
            }
        }
        check("clouds start fully above the sky", entersCleanly);
        check("clouds leave fully below the sky before wrapping", exitsCleanly);
        boolean marginsCover = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            if (Sky.cloudMargin(L, l) <= Sky.cloudHeight(L, l)) marginsCover = false;
        }
        check("the exit margin exceeds the cloud height", marginsCover);

        // Sky glow: a landed press tints with that letter, a cleared word floods yellow.
        GameCore g = new GameCore(new Mem(), 444L);
        g.startGame();
        check("sky starts unglowed", g.skyGlow == 0f);
        g.enemies.clear();
        g.target = null;
        GameCore.Enemy e = add(g, L, new int[] {3, 5}, L.playTop + 200);

        g.tapKey(3, L);
        check("a landed press glows the sky", g.skyGlow == GameCore.GLOW_HIT);
        check("the glow takes the struck letter's colour", g.skyGlowColor == Glyph.COLOR[3]);
        check("a single press is a faint glow", GameCore.GLOW_HIT < 1f);
        check("no screen flash for a mere press", g.flash == 0f);

        advance(g, L, 1.0f);
        check("the press glow fades out", g.skyGlow == 0f);

        g.tapKey(5, L);                      // completes the word
        advance(g, L, 0.2f);                 // let the killing shot land
        // Already decaying by now, so check it outranks a single press rather than == 1.
        check("clearing a word floods the sky", g.skyGlow > GameCore.GLOW_HIT);
        check("the flood is yellow", g.skyGlowColor == GameCore.FLASH_CLEAR);
        check("clearing a word flashes the screen", g.flash > 0f);
        check("the clear flash is warm, not red", g.flashColor == GameCore.FLASH_CLEAR);
        advance(g, L, 1.2f);
        check("the clear glow is brief", g.skyGlow == 0f && g.flash == 0f);

        // Damage keeps its own colour, and outranks a celebration.
        GameCore d2 = new GameCore(new Mem(), 445L);
        d2.startGame();
        d2.enemies.clear();
        add(d2, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d2, L, GameCore.ATTACK_TIME + 2 * DT);
        check("damage flashes red", d2.flash > 0f && d2.flashColor == GameCore.FLASH_DAMAGE);

        boolean spread = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (c.cloudX[l][i] < 0f || c.cloudX[l][i] > 1f) spread = false;
                if (c.cloudW[l][i] <= 0f) spread = false;
            }
        }
        check("cloud placement is on screen and sized", spread);

        // The clip is what lets clouds run past the sky's edge without touching the key
        // deck, so it is worth checking directly rather than trusting it.
        RasterPainter rp = new RasterPainter(40, 40, 1);
        rp.clear(0xFF000000);
        rp.save();
        rp.clipRect(0, 0, 40, 20);
        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rp.restore();
        int[] px = rp.resolve();
        check("clip keeps painting inside the region", (px[5 * 40 + 5] & 0xFF) > 200);
        check("clip blocks painting outside the region", (px[30 * 40 + 5] & 0xFF) < 40);

        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        px = rp.resolve();
        check("restore lifts the clip again", (px[30 * 40 + 5] & 0xFF) > 200);

        // Clips must intersect, never widen.
        RasterPainter rq = new RasterPainter(40, 40, 1);
        rq.clear(0xFF000000);
        rq.save();
        rq.clipRect(0, 0, 20, 20);
        rq.clipRect(0, 0, 40, 40);
        rq.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rq.restore();
        px = rq.resolve();
        check("a second clip cannot widen the first", (px[30 * 40 + 30] & 0xFF) < 40);
    }

    static void indicatorsAndGlow(Layout L) {
        group("indicator and glow");

        // The red edge glow must not survive the run that caused it.
        GameCore c = new GameCore(new Mem(), 93L);
        c.startGame();
        c.lives = 1;
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME * 0.5f);
        check("a lunging word lights the edge glow", c.warnLevel > 0f);
        advance(c, L, GameCore.ATTACK_TIME + 4 * DT);
        check("reached game over", c.state == GameCore.OVER);
        check("the edge glow clears on game over", c.warnLevel == 0f);
        advance(c, L, 1.0f);
        check("and stays clear", c.warnLevel == 0f);

        // Every member of the key cast has its own crying render; none is replaced by the
        // generic mood dumpling used by the accuracy readout.
        RasterPainter cries = new RasterPainter(360, 80, 1);
        cries.clear(0xFF000000);
        for (int g = 0; g < Glyph.COUNT; g++) {
            Kawaii.crying(cries, g, 30 + g * 60, 40, 22, Glyph.COLOR[g], 1f, g * 0.7f, 1f);
        }
        int[] cryingPixels = cries.resolve();
        boolean everyCryVisible = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            boolean visible = false;
            for (int y = 8; y < 72 && !visible; y++) {
                for (int x = g * 60 + 5; x < g * 60 + 55; x++) {
                    if (cryingPixels[y * 360 + x] != 0xFF000000) visible = true;
                }
            }
            if (!visible) everyCryVisible = false;
        }
        check("every key character has a crying render", everyCryVisible);

        // Non-fatal damage must also clear it.
        GameCore d = new GameCore(new Mem(), 94L);
        d.startGame();
        d.enemies.clear();
        add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d, L, GameCore.ATTACK_TIME + 4 * DT);
        d.enemies.clear();
        d.update(DT, L);
        check("the glow clears after surviving a hit", d.warnLevel == 0f);

        // The lock indicator eases between letters rather than jumping.
        GameCore k = new GameCore(new Mem(), 95L);
        k.startGame();
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy e = add(k, L, new int[] {0, 1, 2}, L.playTop + 150);
        k.tapKey(0, L);
        k.update(DT, L);
        float atFirst = k.caretXFor(e, L);
        check("indicator starts on the locked word", k.caretOwner == e);
        k.tapKey(1, L);
        k.update(DT, L);
        float justAfter = k.caretXFor(e, L);
        float destination = k.tileX(e, e.pos, L);
        check("indicator has begun moving", justAfter != atFirst);
        check("indicator has not jumped straight there",
                Math.abs(justAfter - destination) > 1f);
        check("indicator is heading the right way",
                Math.abs(justAfter - destination) < Math.abs(atFirst - destination));
        advance(k, L, 0.5f);
        check("indicator arrives", Math.abs(k.caretXFor(e, L) - k.tileX(e, e.pos, L)) < 1f);

        // Switching words snaps instead of gliding across the screen.
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy other = add(k, L, new int[] {3, 4}, L.playTop + 400);
        other.baseX = L.playLeft + L.enemyR * 3f;
        k.tapKey(3, L);
        k.update(DT, L);
        check("a fresh lock snaps into place",
                Math.abs(k.caretXFor(other, L) - k.tileX(other, other.pos, L)) < 1f);
    }

    /**
     * Stacked HUD text clears itself, at every text scale.
     *
     * The score label sits one line above the number, and the number's caps reach
     * {@code RasterPainter.CAP} of its size above its own baseline. Both sizes go through
     * {@link Draw#type}, so the gap between them has to as well — at a plain unit multiple the
     * digits came up three pixels through SCORE's baseline once TEXT reached 1.34. Checked against
     * a sweep of scales rather than the current one, since that is what went wrong: the layout was
     * right when it was written and wrong when the knob moved.
     */
    static void hudStacking(Layout L) {
        group("HUD stacking");
        float clear = L.hudY - RasterPainter.CAP * Hud.scoreSize(L) - Hud.labelY(L);
        System.out.printf("    the score digits clear the label by %.1fpx at TEXT=%.2f%n",
                clear, Draw.TEXT);
        check("the score number clears its own label", clear > 0f);
        check("with room to spare, not by a pixel", clear > L.unit * 0.1f);

        // The label must also stay under the safe top edge, since raising it is how this was fixed.
        float labelTop = Hud.labelY(L) - RasterPainter.CAP * Draw.type(L.unit * 0.52f);
        check("and the label stays below the safe top edge", labelTop >= L.topSafe);

        // The relationship has to hold however the knob is turned, which is the whole point of
        // scaling the gap: both sides move together.
        boolean holds = true;
        for (int px = 640; px <= 1600; px += 240) {
            Layout t = new Layout();
            t.compute(px, px * 20 / 9, 0, 0, 0, 0);
            if (t.hudY - RasterPainter.CAP * Hud.scoreSize(t) <= Hud.labelY(t)) holds = false;
        }
        check("at every screen width too", holds);
    }

    /**
     * The star screen stacks READY under the checkpoint counter, so it needs the same clearance
     * check the HUD does — and for the same reason. The counter and the prompt arrived with a
     * plain unit gap between them and READY's caps sat on the counter's baseline at TEXT 1.34.
     */
    static void starStacking(Layout L) {
        group("star screen stacking");
        float clear = StarScreen.readyY(L) - RasterPainter.CAP * StarScreen.readySize(L)
                - StarScreen.countY(L);
        System.out.printf("    READY clears the counter by %.1fpx at TEXT=%.2f%n",
                clear, Draw.TEXT);
        check("READY clears the counter above it", clear > 0f);
        check("with room to spare, not by a pixel", clear > L.unit * 0.1f);

        boolean holds = true;
        for (int px = 640; px <= 1600; px += 240) {
            Layout t = new Layout();
            t.compute(px, px * 20 / 9, 0, 0, 0, 0);
            if (StarScreen.readyY(t) - RasterPainter.CAP * StarScreen.readySize(t)
                    <= StarScreen.countY(t)) holds = false;
        }
        check("at every screen width too", holds);

        // Both lines live in the play field, above the deck: the prompt must not reach the keys.
        check("and READY stays clear of the deck", StarScreen.readyY(L) < L.deckTop);
    }

    /**
     * That a run really does end on a green screen, sampled off a rendered frame rather than
     * reasoned about.
     *
     * The world drains green as the last life goes, and it is meant to stay that way until the title
     * screen takes the screen back. It did not: the summary laid the ordinary violet scrim over the
     * sky, so the green survived only on the key deck below the scrim's reach, which read as the
     * deck being tinted rather than the world dying. Nothing in the geometry could catch that — only
     * the pixels can, so this asserts on them.
     */
    static void deathIsGreen(Layout L) {
        group("the screen a run ends on");

        GameCore c = new GameCore(new Mem(), 63L);
        c.startGame();
        c.lives = 0;
        c.state = GameCore.OVER;
        c.deathT = 0f;
        // Past the hold and the fade, which is the settled summary.
        c.time = GameCore.DEATH_TIME + GameCore.OVER_FADE + 0.5f;
        check("the world is fully drained", c.drained() == 1f && c.overFade() >= 1f);

        int[] sky = sample(c, L, 0.5f, 0.16f);
        int[] deck = sample(c, L, 0.5f, 0.965f);
        System.out.printf("    summary sky rgb %d,%d,%d and deck rgb %d,%d,%d%n",
                sky[0], sky[1], sky[2], deck[0], deck[1], deck[2]);
        // Green has to be the strongest channel, and by a margin: the violet scrim it replaced was
        // blue-dominant, so this is exactly the swap that went wrong.
        check("the summary's sky is green", sky[1] > sky[0] + 6 && sky[1] > sky[2] + 6);
        check("and so is the deck under it", deck[1] > deck[0] + 6 && deck[1] > deck[2] + 6);
        check("the sky is dark enough to read text off",
                sky[0] + sky[1] + sky[2] < 3 * 70);

        // And it lets go the moment the title arrives, which is what "until the title screen" means.
        c.toTitle();
        // Sample clear sky, not the large translucent title lettering itself.
        int[] title = sample(c, L, 0.05f, 0.05f);
        System.out.printf("    title sky rgb %d,%d,%d%n", title[0], title[1], title[2]);
        check("the title screen is not death green", title[0] > title[1]);
    }

    /** Red, green and blue at a fraction of the way across and down a rendered frame. */
    private static int[] sample(GameCore c, Layout L, float fx, float fy) {
        int w = (int) L.w, h = (int) L.h;
        RasterPainter p = new RasterPainter(w, h, 1);
        p.clear(0xFF000000);
        Renderer.draw(p, c, L);
        int px = p.resolve()[(int) (h * fy) * w + (int) (w * fx)];
        return new int[] {(px >> 16) & 0xFF, (px >> 8) & 0xFF, px & 0xFF};
    }

    static void settings(Layout L) {
        group("settings");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 81L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        check("settings start closed", !c.settingsOpen);

        // Opening freezes the simulation.
        c.enemies.clear();
        GameCore.Enemy e = add(c, L, new int[] {0, 1}, L.playTop + 100);
        e.speed = 200f;
        c.update(DT, L);
        float movedY = e.y;
        c.openSettings();
        check("opening settings pauses", c.settingsOpen);
        advance(c, L, 1.0f);
        check("nothing moves while paused", e.y == movedY);
        check("the clock still runs so the panel animates", c.clock > 0f);
        c.closeSettings();
        c.update(DT, L);
        check("closing resumes the simulation", e.y > movedY);

        // Every synth style must produce a clean, correctly sized loop.
        boolean stylesOk = true;
        for (int style = 0; style < Music.NAMES.length; style++) {
            if (!Music.isSynth(style)) continue;
            short[] loop = Music.loop(style);
            if (loop.length != Music.loopFrames(style)) stylesOk = false;
            int max = 0;
            for (int i = 0; i < loop.length; i++) max = Math.max(max, Math.abs(loop[i]));
            if (max >= 32767 || max < 2000) stylesOk = false;
        }
        check("every music style renders cleanly", stylesOk);
        check("an unknown style still returns audio", Music.loop(99).length > 0);

        // Panel hit-testing.
        SettingsUi ui = new SettingsUi();
        ui.compute(L);
        check("panel fits on screen",
                ui.panelT >= L.topSafe && ui.panelB <= L.h && ui.panelL > 0);
        check("a tap outside closes",
                ui.hit(L.w / 2f, ui.panelB + 20f) == SettingsUi.HIT_OUTSIDE);
        check("the close button is hit", ui.hit(ui.closeCx, ui.closeCy) == SettingsUi.HIT_CLOSE);
        ui.compute(L,SettingsUi.MINIGAMES);
        check("the difficulty-reset chip is hit",
                ui.hit((ui.optionL() * 3f + ui.optionR()) / 4f,
                        ui.difficultyY + ui.difficultyH / 2f)
                        == SettingsUi.HIT_RESET_DIFFICULTY);
        ui.compute(L,SettingsUi.PROGRESS);
        check("the collection-reset chip remains hit",
                ui.hit((ui.optionL() + ui.optionR() * 3f) / 4f,
                        ui.clearY + ui.clearH / 2f) == SettingsUi.HIT_CLEAR);
        ui.compute(L,SettingsUi.GENERAL);
        check("stage controls lead the run panel", ui.stageY<ui.runLabelY);
        check("former music rows contain no track targets", ui.hit(L.w*.5f,ui.panelT+PlayerSettings.unit(L)*23f)==SettingsUi.HIT_NONE);
        // The stage readout is the settings button, and must not swallow key taps.
        check("the stage readout opens settings", L.inStageTap(L.w / 2f, L.hudY));
        boolean keysClear = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (L.inStageTap(L.keyX[g], L.keyY[g])) keysClear = false;
        }
        check("the settings region does not cover any key", keysClear);
        check("mid-field taps do not open settings", !L.inStageTap(L.w / 2f, L.h * 0.5f));

        check("minigames tab is hittable", ui.hit((ui.tabL(1) + ui.tabR(1)) / 2f,
                ui.tabY + ui.tabH / 2f) == SettingsUi.HIT_MINIGAMES);
        ui.compute(L, SettingsUi.MINIGAMES);
        check("minigames panel fits", ui.panelT >= L.topSafe && ui.panelB <= L.h);
        check("difficulty decreases and increases have distinct targets",
                ui.hit((ui.testChipL(0, 3) + ui.testChipR(0, 3)) / 2f, ui.difficultyStepY + ui.testH / 2f)
                        == SettingsUi.HIT_EASIER
                && ui.hit((ui.testChipL(2, 3) + ui.testChipR(2, 3)) / 2f, ui.difficultyStepY + ui.testH / 2f)
                        == SettingsUi.HIT_HARDER);
        c.stars.collected = 7;
        c.steamer.opens = 4;
        float oldBend = c.stars.sx[7];
        c.setStarDifficulty(2);
        check("difficulty edits preserve the current course and other progression",
                c.stars.collected == 7 && c.stars.sx[7] == oldBend && c.steamer.opens == 4);
        c.startGame();
        check("chosen difficulty survives a new run", c.stars.wins == 2);
        check("chosen difficulty survives relaunch", new GameCore(store, 83L).stars.wins == 2);
        c.stars.recordWin();
        check("wins advance one level from the chosen level", c.stars.wins == 3);
        c.setStarDifficulty(99);
        check("difficulty control clamps high", c.stars.wins == StarPath.MAX_DIFFICULTY);
        c.setStarDifficulty(-1);
        check("difficulty control clamps low and saves", c.stars.wins == 0 && store.starWins == 0);
    }

}
