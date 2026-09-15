package com.dddumpling.game;

/** Compact release groups, with an icon opening each isolated feature page. */
final class ReleaseNotes extends Draw {
    static final String[] VERSIONS=ReleaseContent.VERSIONS;
    static final float PAGE_TIME=.38f, RESTART_DELAY=2f;
    boolean open,listing;
    float pageSlide;
    boolean pageReturning;
    final ReleaseTransition transition=new ReleaseTransition();
    int page,feature;
    GameCore demo;
    private final Layout mini=new Layout();
    private boolean gesture;
    private int replay;
    private float restartIn=-1f;
    float listScroll;
    private float listDownY,listStartScroll,age;
    private int pressedRow=-1;
    private boolean listDragging,listMoved;

    static boolean available(GameCore c) {
        return c.state==GameCore.TITLE && !c.starting() && !c.caseOpen && c.caseFade<0.01f
                && !c.storyOpen() && !c.settingsOpen && c.returnFade<=0f && c.rosterSceneT<=0f;
    }
    static boolean entryHit(GameCore c,Layout L,float x,float y) {
        return available(c) && c.releaseMascot.hit(L,x,y);
    }
    static float size(Layout L) { return Math.min(L.unit,(L.h-L.topSafe-L.padB)/24f); }
    static int columns(Layout L) {
        return Math.max(1,1+(int)Math.floor((L.w*.795f/size(L)-2.8f)/3.8f));
    }
    static float groupHeight(Layout L,int release) {
        int rows=Math.max(1,(ReleaseChange.ITEMS[release].length+columns(L)-1)/columns(L));
        return (6.4f+(rows-1)*3.8f)*size(L);
    }
    static float contentHeight(Layout L) {
        float height=0f;
        for(int release=0;release<VERSIONS.length;release++) height+=groupHeight(L,release);
        return height;
    }
    static float panelHeight(Layout L) { return Math.min(contentHeight(L)+4f*size(L),L.h-L.topSafe-L.padB-3f*size(L)); }
    static float top(Layout L) { return L.topSafe+(L.h-L.topSafe-L.padB-panelHeight(L))*.5f; }
    static float bottom(Layout L) { return top(L)+panelHeight(L); }
    static float listTop(Layout L) { return top(L)+3f*size(L); }
    static float listBottom(Layout L) { return bottom(L)-size(L); }
    static float groupY(Layout L,int release) {
        float y=listTop(L);
        for(int i=0;i<release;i++) y+=groupHeight(L,i);
        return y;
    }
    static float rowY(Layout L,int release) { return groupY(L,release)+3.6f*size(L); }
    static float itemY(Layout L,int release,int item) {
        return rowY(L,release)+(item/columns(L))*3.8f*size(L);
    }
    static float iconX(Layout L,int release,int item) {
        return L.w*.115f+size(L)*(1.4f+3.8f*(item%columns(L)));
    }
    static float maxScroll(Layout L) {
        float overflow=contentHeight(L)-(listBottom(L)-listTop(L));
        return overflow>size(L)*.01f ? overflow : 0f;
    }
    int item() { return ReleaseChange.ITEMS[page][feature]; }
    int change() { return ReleaseContent.ICONS[item()]; }
    float copyExtra() {
        float extra=(ReleaseContent.TEXT[item()].length-1)*1.1f;
        for(int line=1;line<ReleaseContent.CONTEXT[item()].length;line++)
            if(ReleaseContent.CONTEXT[item()][line]) extra+=.4f;
        return extra;
    }
    float windowHeight(Layout L) { return (ReleaseChange.artUnits(change())+6f+copyExtra())*size(L); }
    float windowTop(Layout L) { return L.topSafe+(L.h-L.topSafe-L.padB-windowHeight(L))*.5f; }
    float windowBottom(Layout L) { return windowTop(L)+windowHeight(L); }
    float demoTop(Layout L) { return windowTop(L)+3.6f*size(L); }
    float demoBottom(Layout L) { return demoTop(L)+ReleaseChange.artUnits(change())*size(L); }
    float closeY(Layout L) { return (listing?top(L):windowTop(L))+1.25f*size(L); }
    private void geometry(Layout L) {
        mini.compute(L.w*.84f,14f*size(L),0,0,0,0);
        float artH=demoBottom(L)-demoTop(L);
        // Leave room for the pickup's expanding bloom, including its bob.
        mini.enemyR=change()==ReleaseChange.SHUFFLE ? Math.min(mini.enemyR,artH/8.5f)
                : Math.min(mini.enemyR*2f,artH*.30f);
        mini.keyR=Math.min(mini.keyR*1.5f,size(L)*2f);
    }
    void show(GameCore c,Layout L) {
        if(!available(c)) return;
        transition.begin(c.releaseMascot,L,c.clock);
        c.releaseMascot.read(c);
        open=true;listing=true;pageSlide=0f;pageReturning=false;page=feature=replay=0;listScroll=0f;demo=null;c.titleTouchDown=false;
    }
    void close() { transition.close(); }
    void cancelTouch() { gesture=false;listDragging=false;pressedRow=-1; }
    private int rowAt(Layout L,float x,float y) {
        if(y<listTop(L) || y>listBottom(L)) return -1;
        for(int r=0;r<VERSIONS.length;r++) for(int i=0;i<ReleaseChange.ITEMS[r].length;i++)
            if(Math.abs(x-iconX(L,r,i))<size(L)*1.5f
                    && Math.abs(y-(itemY(L,r,i)-listScroll))<size(L)*1.5f) return itemOffset(r)+i;
        return -1;
    }
    private static int itemOffset(int release) {
        int offset=0;
        for(int i=0;i<release;i++) offset+=ReleaseChange.ITEMS[i].length;
        return offset;
    }
    private void selectItem(int index,Layout L) {
        for(int release=0;release<VERSIONS.length;release++) {
            if(index<ReleaseChange.ITEMS[release].length) { select(release,index,L);return; }
            index-=ReleaseChange.ITEMS[release].length;
        }
    }
    void select(int release,Layout L) { select(release,0,L); }
    void select(int release,int item,Layout L) {
        if(release<0 || release>=VERSIONS.length || item<0 || item>=ReleaseChange.ITEMS[release].length) return;
        page=release;feature=item;listing=false;pageSlide=0f;pageReturning=false;reset(L);
    }
    boolean pageMoving() { return !listing && (pageReturning || pageSlide<1f); }
    float pageTravel() { return pageSlide*pageSlide*(3f-2f*pageSlide); }
    void back() {
        if(transition.moving()) { close();return; }
        if(listing) close();else pageReturning=true;
    }
    private void activated() { restartIn=ReleaseContent.AUTO_RESET[item()] ? RESTART_DELAY : -1f; }
    void reset(Layout L) {
        age=0f;restartIn=-1f;
        if(!ReleaseChange.playable(change())) { demo=null;return; }
        geometry(L);
        demo=new GameCore(null,7000L+page+37L*replay++);demo.collected=Collect.MASK;
        demo.landSeen=14;demo.landChoice=1;
        if(change()==ReleaseChange.TRAVEL) return;
        demo.state=GameCore.PLAY;
        if(change()==ReleaseChange.SHUFFLE) {
            Power w=new Power();w.mystery=true;w.teamAvailable=true;
            w.x=mini.w*.5f;w.y=(demoBottom(L)-demoTop(L))*.5f;demo.power=w;
        } else {
            demo.stage=16;LinkedPairs.spawn(demo,mini);
            for(GameCore.Enemy e:demo.enemies) { e.y=(demoBottom(L)-demoTop(L))*.48f;e.enterT=1f; }
        }
    }
    void update(float dt,Layout L) {
        if(!open) return;
        if(transition.update(dt)) { open=false;demo=null;return; }
        if(!listing && !transition.moving()) {
            pageSlide=Math.max(0f,Math.min(1f,pageSlide+(pageReturning?-dt:dt)/PAGE_TIME));
            if(pageReturning && pageSlide==0f) { listing=true;pageReturning=false;demo=null; }
        }
        if(restartIn>=0f && !listing && !transition.moving() && !pageMoving()) {
            restartIn-=dt;
            if(restartIn<=0f) { reset(L);return; }
        }
        age+=dt;listScroll=Math.max(0f,Math.min(maxScroll(L),listScroll));
        if(demo==null) return;
        geometry(L);demo.clock+=dt;
        if(change()==ReleaseChange.TRAVEL) LandPicker.updateTravel(demo,dt);
        else if(change()==ReleaseChange.SHUFFLE) {
            demo.power.update(dt);
            if(demo.power.hit) demo.power.hitT=Math.min(demo.power.hitT,Power.SELECT_TIME+Power.REVEAL_TIME*.5f);
        } else {
            LinkedPairs.update(demo,dt);
            for(GameCore.Enemy e:demo.enemies) if(e.destroyed) e.destroyT+=dt;
        }
    }
    boolean handleTouch(GameCore c,Layout L,int action,float x,float y) {
        if(!open && !gesture && !(action==0 && entryHit(c,L,x,y))) return false;
        if(open && (transition.moving() || pageMoving())) {
            if(action==0 || action==5) gesture=true;
            if(action==1 || action==3) cancelTouch();
            return true;
        }
        if(open && listing && action==0 && x>=L.w*.04f && x<=L.w*.96f && y>=listTop(L) && y<=listBottom(L)) {
            listDragging=true;listMoved=false;gesture=true;listDownY=y;listStartScroll=listScroll;
            pressedRow=rowAt(L,x,y);return true;
        }
        if(open && listing && action==5) { listDragging=false;pressedRow=-1;return true; }
        if(listDragging) {
            if(action==2) {
                float dy=y-listDownY;
                if(Math.abs(dy)>size(L)*.35f) listMoved=true;
                if(listMoved) listScroll=Math.max(0f,Math.min(maxScroll(L),listStartScroll-dy));
            }
            if(action==1 || action==3) {
                int selected=pressedRow;
                boolean tapped=action==1 && !listMoved && selected>=0 && selected==rowAt(L,x,y);
                cancelTouch();
                if(tapped) selectItem(selected,L);
            }
            return true;
        }
        if(action==0 || action==5) {
            if(open || !gesture) touch(c,L,x,y);
            gesture=true;
        }
        if(action==1 || action==3) gesture=false;
        return true;
    }
    boolean touch(GameCore c,Layout L,float x,float y) {
        if(!open) {
            if(!entryHit(c,L,x,y)) return false;
            show(c,L);return true;
        }
        if(transition.moving() || pageMoving()) return true;
        float s=size(L);
        if(x>L.w*.82f && x<L.w*.96f && Math.abs(y-closeY(L))<s) { close();return true; }
        if(listing) {
            if(x<L.w*.04f || x>L.w*.96f || y<top(L) || y>bottom(L)) close();
            return true;
        }
        if(x<L.w*.21f && x>L.w*.04f && Math.abs(y-closeY(L))<s) { back();return true; }
        if(y<demoTop(L) || y>demoBottom(L) || x<L.w*.08f || x>L.w*.92f) return true;
        if(demo==null) { age=0f;activated();return true; }
        float dx=x-L.w*.08f,dy=y-demoTop(L);
        if(change()==ReleaseChange.TRAVEL) {
            int before=demo.landChoice;
            LandPicker.step(demo,dx<mini.w*.5f ? -1 : 1);
            if(before!=demo.landChoice) {
                activated();
                if(c.sound!=null) c.sound.landShuffle();
            }
        } else if(change()==ReleaseChange.SHUFFLE) {
            Power w=demo.power;
            if(!w.hit) {
                w.hit=true;w.hitT=0f;activated();
                w.effect=Power.mysteryAt(true,demo.rnd.nextInt(Power.mysteryCount(true)));
                if(c.sound!=null) c.sound.shuffleBlip();
            }
        } else {
            for(GameCore.Enemy e:demo.enemies) {
                if(e.destroyed || Math.abs(dx-demo.enemyCentreX(e))>mini.enemyR*1.55f
                        || Math.abs(dy-e.y)>mini.enemyR*1.55f) continue;
                demo.target=e;demo.tapKey(e.word[0],mini);activated();
                if(c.sound!=null) {
                    if(e.destroyed) c.sound.clearWord();else c.sound.wrong();
                }
                break;
            }
        }
        return true;
    }
    static void entry(Painter p,GameCore c,Layout L) {
        if(available(c) && !c.releaseNotes.open) c.releaseMascot.draw(p,c,L);
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!open) return;
        p.save();p.clipRect(0,0,L.w,L.h);
        Painter glass=new OpacityPainter(p,.96f);
        if(transition.progress<1f)
            ReleaseMascot.steamer(glass,transition.x(L),transition.y(L),transition.radius(L),c.clock,transition.lidLift());
        p.translate(transition.listX(L),0);
        if(listing) drawPanel(glass,c,L,true);
        else {
            float travel=pageTravel();
            if(travel<1f) {
                p.save();p.translate(-L.w*travel,0);drawPanel(glass,c,L,true);p.restore();
            }
            if(travel>0f) {
                p.save();p.translate(L.w*(1f-travel),0);drawPanel(glass,c,L,false);p.restore();
            }
        }
        p.restore();
    }
    private void drawPanel(Painter p,GameCore c,Layout L,boolean list) {
        float s=size(L),cx=L.w*.5f,t=list?top(L):windowTop(L),b=list?bottom(L):windowBottom(L);
        p.fillPoly(pageShape(L.w*.04f,t,L.w*.96f,b,s*.8f),0xE8302944);
        float closeX=L.w*.89f,closeY=t+1.25f*s,r=s*.3f;
        p.line(closeX-r,closeY-r,closeX+r,closeY+r,INK,s*.12f);
        p.line(closeX-r,closeY+r,closeX+r,closeY-r,INK,s*.12f);
        if(list) { drawList(p,c,L);return; }
        arrow(p,L.w*.13f,closeY,-1,s);
        p.text(VERSIONS[page],cx,t+s*1.5f,type(s*.55f),INK_DIM,Painter.CENTER,false);
        p.text(ReleaseContent.TITLES[item()],cx,t+s*2.9f,type(s*.75f),GOLD,Painter.CENTER,true);
        p.save();p.clipRect(L.w*.08f,demoTop(L),L.w*.92f,demoBottom(L));
        p.translate(L.w*.08f,demoTop(L));
        float artH=demoBottom(L)-demoTop(L);
        if(demo==null) {
            float bounce=(float)Math.sin(Math.min(1f,age/.5f)*Math.PI)*s*.35f;
            ReleaseChange.icon(p,change(),L.w*.42f,artH*.5f-bounce,s*1.7f,c.clock+age);
        } else if(change()==ReleaseChange.TRAVEL) {
            p.translate(0,artH*.5f-LandPicker.cardY(mini));LandPicker.draw(p,demo,mini);
        } else if(change()==ReleaseChange.SHUFFLE) {
            Renderer.powerup(p,demo,mini,demo.power,1f);
            if(demo.power.hit && demo.power.hitT>=Power.SELECT_TIME)
                p.text(Power.NAMES[demo.power.effect],mini.w*.5f,artH-s*.4f,type(s*.5f),GOLD,Painter.CENTER,true);
        } else {
            LinkedPairArt.draw(p,demo,mini);
            for(GameCore.Enemy e:demo.enemies) Renderer.enemy(p,demo,mini,e);
        }
        p.restore();
        float textY=demoBottom(L)+s*1.25f;
        for(int line=0;line<ReleaseContent.TEXT[item()].length;line++) {
            boolean context=ReleaseContent.CONTEXT[item()][line];
            if(context && line>0) textY+=s*.4f;
            p.text(ReleaseContent.TEXT[item()][line],cx,textY,type(s*(context ? .48f : .55f)),
                    context ? 0xFFC9B5EE : INK,Painter.CENTER,false);
            textY+=s*1.1f;
        }

    }
    private void drawList(Painter p,GameCore c,Layout L) {
        float s=size(L);
        p.text("WHAT'S COOKING?",L.w*.46f,top(L)+s*1.8f,type(s*.8f),GOLD,Painter.CENTER,true);
        p.save();p.clipRect(L.w*.09f,listTop(L),L.w*.91f,listBottom(L));
        for(int release=0;release<VERSIONS.length;release++) {
            float headerY=groupY(L,release)-listScroll+s*.75f;
            p.text(VERSIONS[release],L.w*.115f,headerY,type(s*.65f),GOLD,Painter.LEFT,true);
            p.line(L.w*.36f,headerY-s*.3f,L.w*.88f,headerY-s*.3f,0xFF514560,s*.045f);
            for(int i=0;i<ReleaseChange.ITEMS[release].length;i++) {
                int id=ReleaseChange.ITEMS[release][i];float y=itemY(L,release,i)-listScroll,x=iconX(L,release,i);
                int color=listDragging && !listMoved && pressedRow==itemOffset(release)+i ? 0xFF71618A : 0xFF463B5B;
                p.fillPoly(pageShape(x-s*1.4f,y-s*1.4f,x+s*1.4f,y+s*1.4f,s*.4f),color);
                ReleaseChange.icon(p,ReleaseContent.ICONS[id],x,y,s*1.05f,c.clock);
            }
        }
        p.restore();
        float height=listBottom(L)-listTop(L),max=maxScroll(L);
        if(max>0f) {
            float thumb=height*height/(height+max),y=listTop(L)+(height-thumb)*listScroll/max;
            p.line(L.w*.935f,listTop(L),L.w*.935f,listBottom(L),0xFF463B59,s*.1f);
            p.line(L.w*.935f,y,L.w*.935f,y+thumb,0xFFAE9ADA,s*.1f);
        }
    }
    private static float[] pageShape(float l,float t,float r,float b,float radius) {
        float[] points=new float[56];int n=0;
        for(int corner=0;corner<4;corner++) {
            float x=corner==0 || corner==3 ? r-radius : l+radius,y=corner<2 ? b-radius : t+radius;
            for(int step=0;step<=6;step++) {
                float angle=(corner*90f+step*15f)*(float)Math.PI/180f;
                points[n++]=x+(float)Math.cos(angle)*radius;points[n++]=y+(float)Math.sin(angle)*radius;
            }
        }
        return points;
    }
    private static void arrow(Painter p,float x,float y,int dir,float s) {
        p.line(x-dir*s*.3f,y-s*.4f,x+dir*s*.3f,y,INK,s*.14f);
        p.line(x+dir*s*.3f,y,x-dir*s*.3f,y+s*.4f,INK,s*.14f);
    }
}
