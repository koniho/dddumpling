package com.dddumpling.game;

/** The corner release-book host calls attention to unread builds. */
final class ReleaseMascot extends Draw {
    boolean unread;
    private boolean checked;
    void update(GameCore c,float dt) {
        if(!checked && ReleaseNotes.available(c)) {
            checked=true;
            unread=c.store!=null && !BuildFlags.BUILD_ID.equals(c.store.loadReleaseSeen());
        }
    }
    void reset(GameCore c) {
        if(c.store!=null) c.store.saveReleaseSeen("");
        checked=false;unread=true;
    }
    void read(GameCore c) {
        unread=false;checked=true;
        if(c.store!=null) c.store.saveReleaseSeen(BuildFlags.BUILD_ID);
    }
    float x(Layout L) { return L.unit*2.5f; }
    float y(Layout L) { return L.dangerY-L.unit*2.1f; }
    float radius(Layout L) { return L.unit*.8f; }
    float attentionLift(float time) {
        float beat=(time%2.2f)/.8f;
        return unread && beat<1f ? .5f*(float)Math.sin(beat*Math.PI) : 0f;
    }
    boolean hit(Layout L,float x,float y) {
        return Math.abs(x-x(L))<radius(L)*1.7f && Math.abs(y-y(L))<radius(L)*2.2f;
    }
    void draw(Painter p,GameCore c,Layout L) {
        p=new OpacityPainter(p,.96f);
        float x=x(L),y=y(L),r=radius(L);
        if(unread) {
            float pulse=1f+.09f*(float)Math.sin(c.clock*3f);
            powerHalo(p,x,y,r*1.2f*pulse,c.clock,Glyph.cycle(c.clock*.7f),1f);
        }
        steamer(p,x,y,r,c.clock,attentionLift(c.clock),unread?1f:0f);
    }
    static float steamPhase(float time,int curl) { return (time*.55f+(curl+1)*.31f)%1f; }
    static void steamer(Painter p,float x,float y,float r,float time) { steamer(p,x,y,r,time,0f); }
    static void steamer(Painter p,float x,float y,float r,float time,float lift) {
        steamer(p,x,y,r,time,lift,0f);
    }
    private static void steamer(Painter p,float x,float y,float r,float time,float lift,float extraSteam) {
        int bamboo=0xFFEBC38A,ink=0xFF4B3642;
        p.fillEllipse(x,y+r*.79f,r*.85f,r*.12f,0x44352B42);
        for(int side=-1;side<=1;side+=2)
            p.fillEllipse(x+side*r*.52f,y+r*.66f,r*.20f,r*.12f,0xFFC99166);
        Basket.back(p,x,y-r*.42f,y+r*.45f,r,r*.28f,r*.91f,bamboo,0f,1f);
        Basket.front(p,x,y-r*.42f,y+r*.45f,r,r*.28f,r*.91f,bamboo,0f,1f);
        Basket.lid(p,x,y-r*(.48f+lift),r*1.05f,r*.31f,0xFFF3D4A2,1f);
        p.fillEllipse(x,y+r*.19f,r*.67f,r*.28f,0xFFE5BA83);
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.31f;
            p.fillEllipse(ex,y+r*.12f,r*.075f,r*.105f,ink);
            p.fillCircle(ex-r*.018f,y+r*.082f,r*.024f,0xFFFFFAEF);
            p.fillEllipse(x+side*r*.50f,y+r*.29f,r*.15f,r*.075f,0xBFF08D91);
        }
        p.arc(x,y+r*.25f,r*.15f,r*.12f,0,180,ink,r*.042f);
        int curls=extraSteam>0f ? 3 : 1;
        for(int curl=-curls;curl<=curls;curl++) {
            float phase=extraSteam>0f ? (time*.8f+(curl+curls)*.21f)%1f : steamPhase(time,curl);
            float[] points=new float[26];
            for(int i=0;i<13;i++) {
                float t=i/12f;
                points[i*2]=x+curl*r*(extraSteam>0f ? .24f : .38f)+(float)Math.sin(t*6f-time*1.8f+curl)*r*.065f;
                points[i*2+1]=y-r*(.90f+lift+phase*(.48f+extraSteam*.28f)+t*(.44f+extraSteam*.25f));
            }
            p.polyline(points,Glyph.withAlpha(0xFFFFF0DB,(int)(210*Math.sin(phase*Math.PI))),r*.06f);
        }
    }
}
