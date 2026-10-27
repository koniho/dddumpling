package com.dddumpling.game;

/** Installation history and prompt acknowledgement are independent of the release book's read state. */
final class UpdatePrompt extends Draw {
    boolean pending, highlight, open, closing;
    float slide;
    private String identity="", catalog="";
    boolean storeOffer;
    private boolean readAfterClose, gesture;
    private int pointer=-1, pressed;

    void launch(GameCore c) {
        if(c.store==null)return;
        identity=c.store.appVersion();catalog=ReleaseNotes.VERSIONS[0];
        String[] saved=c.store.loadUpdateState().split("\n",-1);
        if(saved.length==4 && saved[0].equals("1")) {
            String[] flags=saved[3].split(",",-1);
            pending=flags.length==2 && flags[0].equals("1");
            highlight=flags.length==2 && flags[1].equals("1");
            if(!identity.equals(saved[1]) && !catalog.equals(saved[2]))pending=true;
        }
        // Missing history is ambiguous (fresh install or an older save): establish a silent baseline.
        save(c);
    }
    private void save(GameCore c) {
        if(c.store!=null)c.store.saveUpdateState("1\n"+identity+"\n"+catalog+"\n"+(pending?"1":"0")+","+(highlight?"1":"0"));
    }
    void rearm(GameCore c) {
        pending=highlight=true;c.releaseMascot.unread=true;save(c);
    }
    void read(GameCore c) {
        pending=highlight=false;save(c);
    }
    static boolean suitable(GameCore c) {
        return ReleaseNotes.available(c) && !c.releaseNotes.open && !c.paused && !c.townOpen
                && !Starter.hideCase(c) && !c.starter.open && !c.onboarding.titleGuide
                && !c.onboarding.briefing && c.onboarding.practice==null && !c.onboarding.rescueGuide
                && !c.onboarding.collectionPending(c) && c.homeT<=0f && c.townReturnFade<=0f
                && c.landDiscovery<0 && LandDiscovery.next(c)<0 && !c.titleTouchDown
                && c.modes.transition>=1f && c.modes.pointer<0 && c.modes.swipeOffset==0f;
    }
    boolean update(GameCore c,float elapsed,Layout L) {
        if(!open) {
            if(!suitable(c))return false;
            storeOffer=!pending && c.storeUpdate.ready(c);
            if(!pending && !storeOffer)return false;
            // Never stack two unsolicited notices in the same session.
            c.storeUpdate.show();
            open=true;closing=false;slide=0f;Pause.release(c);c.titleTouchDown=false;
            if(c.sound!=null)c.sound.uiBloop();
        }
        slide=Math.max(0f,Math.min(1f,slide+(closing?-elapsed:elapsed)/PANEL_SLIDE_TIME));
        if(closing && slide==0f) {
            open=closing=false;
            if(readAfterClose) { c.releaseNotes.show(c,L);c.releaseNotes.select(0,L); }
        }
        return true;
    }
    void choose(GameCore c,boolean read) {
        if(!open || closing || slide<1f)return;
        if(storeOffer)c.storeUpdate.choose(c,read);
        else { pending=false;save(c); }
        closing=true;readAfterClose=!storeOffer && read;
        if(c.sound!=null)c.sound.uiBloop();
    }
    void cancelTouch() { pointer=-1;pressed=0;gesture=false; }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(!open && !gesture)return false;
        if(action==0) {
            gesture=true;pointer=id;pressed=closing || slide<1f?0:hit(L,x,y);
        } else if(action==2 && id==pointer && hit(L,x,y)!=pressed)pressed=0;
        else if(action==5 || action==3) {pressed=0;pointer=-1;}
        else if(action==1) {
            int choice=id==pointer && hit(L,x,y)==pressed?pressed:0;
            cancelTouch();if(choice!=0)choose(c,choice==1);
        }
        if(action==3)cancelTouch();
        return true;
    }
    static float size(Layout L) { return ReleaseNotes.size(L); }
    static float top(Layout L) { return L.topSafe+(L.dangerY-L.topSafe-12f*size(L))*.5f; }
    static float buttonY(Layout L,int choice) { return top(L)+size(L)*(choice==1?7.8f:10.5f); }
    static int hit(Layout L,float x,float y) {
        if(x<L.w*.18f || x>L.w*.82f)return 0;
        for(int choice=1;choice<=2;choice++)if(Math.abs(y-buttonY(L,choice))<=size(L)*1.1f)return choice;
        return 0;
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!open)return;
        float s=size(L),x=L.w*.5f,t=top(L);
        p.save();p.translate(L.w*(1f-panelTravel(slide)),0);
        Painter glass=new OpacityPainter(p,PANEL_OPACITY);
        glassPanel(glass,L.w*.10f,t,L.w*.90f,t+s*12.5f,s);
        ReleaseMascot.steamer(glass,x,t+s*3f,s*1.25f,c.clock);
        glass.text(storeOffer?"UPDATE AVAILABLE":"WHAT'S NEW?",x,t+s*5.6f,type(s*.8f),GOLD,Painter.CENTER,true);
        for(int choice=1;choice<=2;choice++) {
            float y=buttonY(L,choice);
            glass.fillRect(L.w*.18f,y-s*1.1f,L.w*.82f,y+s*1.1f,
                    pressed==choice?0x9971618A:choice==1?0x664DCEAA:0x334DCEAA);
            glass.text(storeOffer?(choice==1?"UPDATE":"LATER"):(choice==1?"SEE WHAT'S NEW":"SKIP"),x,y+s*.25f,type(s*.60f),INK,Painter.CENTER,true);
        }
        p.restore();
    }
}
