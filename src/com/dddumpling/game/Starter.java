package com.dddumpling.game;

/** A first friend: arrive, try a squishy, then tap it again for the normal send-off. */
final class Starter extends Draw {
    static final int[] CHOICES={0,1,13,14,22};
    // Separate from learned tutorial bits, so an interrupted introduction can resume.
    static final int INTRO_PENDING=1<<8;
    static final float ENTER=.65f, STAGGER=.16f, HOP=.32f, FOCUS=.48f, EXIT=.85f;
    static final float READY=ENTER+STAGGER*(CHOICES.length-1)+HOP;
    boolean open, exiting;
    int selected=-1;
    float age, previewAge, exitAge;
    final float[] focus=new float[CHOICES.length];
    private int pointer=-1, armed=-1, arrived;
    private static final int HERO=CHOICES.length;

    static boolean eligible(GameCore c) {
        return Collect.owned(c.collected)==0 && c.onboarding.saved==0 && c.onboarding.savedPowers==0;
    }
    static boolean introPending(GameCore c) {
        return (c.onboarding.savedPowers&INTRO_PENDING)!=0 && c.onboarding.eligible(Onboarding.SKIPPED);
    }
    static boolean hideCase(GameCore c) { return eligible(c) || c.starter.open || introPending(c); }
    void begin() { clear();open=true; }
    void clear() {
        open=exiting=false;selected=-1;age=previewAge=exitAge=0f;arrived=0;
        java.util.Arrays.fill(focus,0f);cancelTouch();
    }
    void cancelTouch() { pointer=armed=-1; }
    static float arrival(int choice) { return ENTER+(CHOICES.length-1-choice)*STAGGER; }
    private static float ease(float t) { t=Math.max(0f,Math.min(1f,t));return t*t*(3f-2f*t); }
    private float hop(int choice) {
        float t=(age-arrival(choice))/HOP;
        return t>0f && t<1f?(float)Math.sin(t*Math.PI):0f;
    }
    void update(GameCore c,float dt) {
        age+=dt;
        if(exiting) {
            exitAge+=dt;
            if(exitAge>=EXIT) { clear();c.beginStarterLaunch(); }
            return;
        }
        // Rightmost arrives first, so later friends never pass through a parked friend.
        for(int i=CHOICES.length-1;i>=0;i--) {
            if(age>=arrival(i) && (arrived&(1<<i))==0) {
                arrived|=1<<i;
                if(c.sound!=null)c.sound.squish(CHOICES[i]%Glyph.COUNT,1);
            }
            focus[i]=i==selected?Math.min(1f,focus[i]+dt/FOCUS):Math.max(0f,focus[i]-dt/FOCUS);
        }
        if(selected>=0 && focus[selected]>=1f)previewAge+=dt;
    }
    void choose(GameCore c,int choice) {
        if(!open || exiting || !eligible(c) || choice<0 || choice>=CHOICES.length
                || age<arrival(choice)+HOP || selected==choice)return;
        selected=choice;previewAge=0f;
        if(c.sound!=null)c.sound.squish(CHOICES[choice]%Glyph.COUNT,1);
    }
    boolean canConfirm() {
        if(!open || exiting || selected<0 || age<READY || focus[selected]<1f)return false;
        for(int i=0;i<focus.length;i++)if(i!=selected && focus[i]>0f)return false;
        return true;
    }
    void confirm(GameCore c) {
        if(!canConfirm() || !eligible(c))return;
        int who=CHOICES[selected];
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
        exiting=true;exitAge=0f;cancelTouch();
        if(c.sound!=null)c.sound.collect(1);
    }
    static float x(Layout L,int choice) { return L.w*(.12f+choice*.19f); }
    static float y(Layout L,int choice) { return L.topSafe+(L.h-L.padB-L.topSafe)*.28f; }
    static float rosterRadius(Layout L) { return Math.min(L.w*.059f,L.h*.039f); }
    static float heroRadius(Layout L) { return Math.min(L.w*.15f,L.h*.08f); }
    private float fun() { return exiting?1f-ease(exitAge/.22f):1f; }
    private float dance() {
        return (float)Math.sin(previewAge*4.4f)*Math.max(0f,(float)Math.sin(previewAge*2.2f))*fun();
    }
    float drawX(Layout L,int i) {
        float t=ease((age-(CHOICES.length-1-i)*STAGGER)/ENTER);
        float x=-rosterRadius(L)*2.5f+(x(L,i)+rosterRadius(L)*2.5f)*t;
        if(exiting && i!=selected) x+=(L.w+rosterRadius(L)*3f)*ease((exitAge-i*.045f)/.55f);
        float f=ease(focus[i]);
        return x+(L.w*.5f+heroRadius(L)*.12f*dance()-x)*f;
    }
    float drawY(Layout L,int i) {
        float f=ease(focus[i]),r=heroRadius(L);
        float hero=L.h*.5f-r*.22f*Math.abs(dance());
        return y(L,i)+(hero-y(L,i))*f-rosterRadius(L)*.5f*hop(i)*(1f-f)
                -r*.35f*(float)Math.sin(Math.PI*f);
    }
    float drawRadius(Layout L,int i) {
        float f=ease(focus[i]);
        return (rosterRadius(L)+(heroRadius(L)-rosterRadius(L))*f)
                *(1f+.07f*dance()*f+.10f*hop(i)*(1f-f));
    }
    private int hit(Layout L,float x,float y) {
        if(!open || exiting)return -1;
        if(canConfirm()) {
            float dx=x-drawX(L,selected),dy=y-drawY(L,selected),r=drawRadius(L,selected)*1.25f;
            if(dx*dx+dy*dy<r*r)return HERO;
        }
        for(int i=0;i<CHOICES.length;i++) {
            if(age<arrival(i)+HOP || focus[i]>0f)continue;
            if(Math.abs(x-x(L,i))<L.w*.09f && Math.abs(y-y(L,i))<rosterRadius(L)*1.4f)return i;
        }
        return -1;
    }
    void touch(GameCore c,Layout L,int action,int id,float x,float y) {
        int target=hit(L,x,y);
        if(action==0) { pointer=id;armed=target; }
        else if(action==2 && id==pointer && target!=armed)armed=-1;
        else if(action==3 || action==5 || action==6)cancelTouch();
        else if(action==1) {
            int tapped=id==pointer && target==armed?armed:-1;
            cancelTouch();
            if(tapped==HERO)confirm(c);else choose(c,tapped);
        }
    }
    private void friend(Painter p,GameCore c,Layout L,int i) {
        float x=drawX(L,i),y=drawY(L,i),r=drawRadius(L,i),f=ease(focus[i]);
        float glowAge=f>0f?previewAge+exitAge*Storybook.GLOW_TIME/EXIT:age-arrival(i);
        Storybook.glowRings(p,CHOICES[i],x,y,r,glowAge);
        float look=(float)Math.sin(previewAge*3f)*.4f*f*fun();
        Trinket.drawReacting(p,CHOICES[i],x,y,r,c.clock,1f,(f>.5f && fun()>.1f) || hop(i)>.1f?4:-1,look);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        Starter a=c.starter;if(!a.open)return;
        float s=L.unit,cx=L.w*.5f;
        float fade=a.exiting?1f-ease((a.exitAge-(EXIT-.22f))/.22f):1f;
        p.fillRect(0,0,L.w,L.h,fadeBy(Screens.SCRIM,fade));
        float heading=fade*(a.exiting?1f-ease(a.exitAge/.3f):ease(a.age/.25f));
        p.text("CHOOSE YOUR",cx,L.topSafe+s*2.5f,type(s*1.05f),fadeBy(INK,heading),Painter.CENTER,true);
        p.text("FIRST SQUISHY",cx,L.topSafe+s*4.6f,type(s*1.05f),fadeBy(ROSE,heading),Painter.CENTER,true);
        p.text("A friend to guide you!",cx,L.topSafe+s*6.5f,type(s*.57f),fadeBy(INK_DIM,heading),Painter.CENTER,false);
        float rowFade=a.exiting?1f-ease(a.exitAge/.4f):ease(a.age/.5f);
        p.line(x(L,0),y(L,0)+rosterRadius(L)*1.6f,x(L,4),y(L,4)+rosterRadius(L)*1.6f,
                fadeBy(INK_DIM,rowFade*.3f),s*.045f);
        for(int i=0;i<CHOICES.length;i++) {
            p.fillCircle(x(L,i),y(L,i)+rosterRadius(L)*1.6f,s*.13f,
                    fadeBy(Collect.ACCENT[CHOICES[i]],rowFade));
            if(i!=a.selected)a.friend(p,c,L,i);
        }
        if(a.selected>=0) {
            a.friend(p,c,L,a.selected);
            float textFade=ease(a.focus[a.selected])*heading;
            float nameY=L.h*.5f+heroRadius(L)*1.85f;
            p.text(Collect.NAME[CHOICES[a.selected]],cx,nameY,type(s*.9f),
                    fadeBy(Collect.BODY[CHOICES[a.selected]],textFade),Painter.CENTER,true);
            p.text("TAP YOUR FRIEND TO BEGIN",cx,nameY+s*2f,type(s*.55f),
                    fadeBy(GOLD,textFade),Painter.CENTER,true);
        } else {
            p.text("TAP A FRIEND",cx,L.h*.53f,type(s*.72f),fadeBy(GOLD,heading),Painter.CENTER,true);
        }
    }
}
