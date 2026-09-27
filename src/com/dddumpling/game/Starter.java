package com.dddumpling.game;

/** A first friend, chosen before the normal send-off. */
final class Starter extends Draw {
    static final int[] CHOICES={0,1,13,14,22};
    // Separate from learned tutorial bits, so an interrupted introduction can resume.
    static final int INTRO_PENDING=1<<8;
    boolean open;
    private int pointer=-1, armed=-1;

    static boolean eligible(GameCore c) {
        return Collect.owned(c.collected)==0 && c.onboarding.saved==0 && c.onboarding.savedPowers==0;
    }
    static boolean introPending(GameCore c) {
        return (c.onboarding.savedPowers&INTRO_PENDING)!=0 && c.onboarding.eligible(Onboarding.SKIPPED);
    }
    static boolean hideCase(GameCore c) { return eligible(c) || c.starter.open || introPending(c); }
    void clear() { open=false;cancelTouch(); }
    void cancelTouch() { pointer=armed=-1; }
    void choose(GameCore c,int choice) {
        if(!open || !eligible(c) || choice<0 || choice>=CHOICES.length)return;
        int who=CHOICES[choice];
        c.collected=Collect.add(c.collected,who);
        c.collectionCounts[who]=1;
        if(c.collectTotal<Integer.MAX_VALUE)c.collectTotal++;
        CaseUi.highlight(c,who);
        if(c.store!=null) {
            c.store.saveCollected(c.collected);
            c.store.saveCollectionCounts(c.collectionCounts);
            c.store.saveCollectTotal(c.collectTotal);
        }
        c.progress.starter(who);
        c.onboarding.savedPowers|=INTRO_PENDING;c.onboarding.save(c);
        clear();c.beginStart();
    }
    static float x(Layout L,int choice) { return L.w*(choice==4?.5f:choice%2==0?.27f:.73f); }
    static float y(Layout L,int choice) { return L.topSafe+(L.h-L.topSafe)*(.29f+(choice/2)*.22f); }
    static float halfHeight(Layout L) { return Math.min(L.w*.18f,(L.h-L.topSafe)*.095f); }
    static int hit(Layout L,float x,float y) {
        for(int i=0;i<CHOICES.length;i++)
            if(Math.abs(x-x(L,i))<L.w*.205f && Math.abs(y-y(L,i))<halfHeight(L))return i;
        return -1;
    }
    void touch(GameCore c,Layout L,int action,int id,float x,float y) {
        int choice=hit(L,x,y);
        if(action==0) { pointer=id;armed=choice; }
        else if(action==2 && id==pointer && choice!=armed)armed=-1;
        else if(action==3 || action==5 || action==6)cancelTouch();
        else if(action==1) {
            int selected=id==pointer && choice==armed?armed:-1;
            cancelTouch();choose(c,selected);
        }
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!c.starter.open)return;
        float s=L.unit,cx=L.w*.5f;
        p.fillRect(0,0,L.w,L.h,Screens.SCRIM);
        p.text("CHOOSE YOUR",cx,L.topSafe+s*2.5f,type(s*1.05f),INK,Painter.CENTER,true);
        p.text("FIRST SQUISHY",cx,L.topSafe+s*4.6f,type(s*1.05f),ROSE,Painter.CENTER,true);
        p.text("A friend to guide you!",cx,L.topSafe+s*6.5f,type(s*.57f),INK_DIM,Painter.CENTER,false);
        for(int i=0;i<CHOICES.length;i++) {
            float x=x(L,i),y=y(L,i),hh=halfHeight(L);
            p.fillPoly(pill(x,y,L.w*.205f,hh,18),c.starter.armed==i?0xFF514366:BG_HI);
            Trinket.draw(p,CHOICES[i],x,y-hh*.14f,hh*.58f,c.clock,true,1);
            p.text(Collect.NAME[CHOICES[i]],x,y+hh*.78f,type(s*.52f),INK,Painter.CENTER,true);
        }
        p.text("TAP A FRIEND TO BEGIN",cx,L.h-L.padB-s*2.8f,type(s*.61f),GOLD,Painter.CENTER,true);
    }
}
