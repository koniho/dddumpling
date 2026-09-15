package com.dddumpling.game;

/** First entrance is a quiet choice; only a real selection is persisted. */
final class CaveSelection extends Draw {
    static final float INTRO = .6f, DEPART = .8f;
    float age;
    int selected = -1;
    void reset() { age=0f;selected=-1; }
    static float x(Layout L,int i) { return L.w*(.20f+(i%3)*.30f); }
    static float y(Layout L,int i) { return L.playTop+L.w*(.52f+(i/3)*.35f); }
    static float radius(Layout L) { return L.w*.081f; }
    boolean pick(GameCore c,int choice) {
        if(age<INTRO || selected>=0 || CaveDumpling.valid(choice)<0) return false;
        selected=c.caveChoice=choice;age=0f;
        if(c.store!=null)c.store.saveCaveChoice(choice);
        if(c.sound!=null)c.sound.collect(0);
        return true;
    }
    boolean tap(GameCore c,Layout L,float px,float py) {
        for(int i=0;i<CaveDumpling.COUNT;i++) {
            float dx=px-x(L,i),dy=py-y(L,i);
            if(dx*dx+dy*dy<radius(L)*radius(L)*1.7f) return pick(c,i);
        }
        return false;
    }
    void update(GameCore c,float dt) {
        age+=dt;
        if(selected>=0 && age>=DEPART) { c.cave.phase=Cave.WALK;c.stageBanner=0f; }
    }
    void draw(Painter p,GameCore c,Layout L) {
        float cy=L.playTop+L.w*.14f;
        CaveArt.entrance(p,L.w*.5f,cy,L.w*.14f,255,false);
        p.text("CHOOSE YOUR EXPLORER",L.w*.5f,L.playTop+L.w*.32f,type(L.unit*.65f),CaveArt.LAMP,Painter.CENTER,true);
        for(int i=0;i<CaveDumpling.COUNT;i++) {
            float px=x(L,i),py=y(L,i),r=radius(L);
            if(selected>=0 && i==selected) {
                float t=Math.min(1f,age/DEPART),ease=t*t*(3f-2f*t);
                px+=(L.w*.5f-px)*ease;py+=(cy-py)*ease;
                py-=Math.sin(t*Math.PI)*r*.6f;r*=1f-ease*.42f;
            }
            p.fillEllipse(x(L,i),y(L,i)+radius(L)*.86f,radius(L)*1.2f,radius(L)*.22f,CaveArt.ROCK);
            if(selected<0 || selected==i) CaveDumpling.draw(p,i,px,py,r,
                    1f+(float)Math.sin(c.clock*2f+i)*.025f,c.clock);
            p.text(CaveDumpling.NAMES[i],x(L,i),y(L,i)+radius(L)*1.55f,type(L.unit*.43f),
                    selected==i ? INK : CaveArt.LIGHT,Painter.CENTER,true);
        }
    }
}
