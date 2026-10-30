package com.dddumpling.game;

/** Tiny visits to refreshed scenery; the arena tour also hosts its resident bosses. */
final class ReleaseHomes extends Draw {
    static final float PERIOD=4.8f,SLIDE=.65f;
    static final int[] HOSTS={Boss.SLIME,Boss.SPLITTER,Boss.OCTOPUS,Boss.MUSHROOM};
    private final boolean arena;
    private final Room[] rooms=new Room[HOSTS.length];
    ReleaseHomes(boolean arena){this.arena=arena;}
    static int stop(float time){return Math.floorMod((int)Math.floor(time/PERIOD),HOSTS.length);}
    private static final class Icons {
        static final ReleaseHomes LAND=new ReleaseHomes(false),ARENA=new ReleaseHomes(true);
    }
    // A settled snapshot in the list; opened cards keep their animated tours.
    static void icon(Painter p,boolean arena,float x,float y,float r) {
        (arena?Icons.ARENA:Icons.LAND).draw(p,x-r*1.12f,y-r,r*2.24f,r*2,1.8f,false);
    }
    private static final class Room {
        final GameCore c;
        final Layout layout=new Layout();
        final Boss resident;
        float last=-1;
        Room(int kind,boolean arena,float w,float h) {
            c=new GameCore(null,3500+kind);c.state=GameCore.PLAY;
            c.stage=Lands.firstStage(WorldCatalog.BOSSES[kind].land);c.landBlend=1;
            layout.compute(w,h,0,0,0,0);layout.playTop=0;layout.deckTop=h;
            resident=arena?c.boss:null;
            if(resident!=null) {
                resident.begin(kind,WorldCatalog.bossStage(kind),c.rnd);
                resident.update(0,layout,c.rnd);resident.intro=0;
                TimeAttackVersus.animatePortrait(resident,0,layout);
            }
        }
        void pose(float time) {
            float before=last;
            float dt=last<0 || time<last?0:Math.min(.05f,time-last);last=time;
            c.clock=12+time;c.skyClock=12+time*.65f;
            if(resident!=null)TimeAttackVersus.animatePortrait(resident,dt,layout);
            float beat=time%PERIOD;
            c.slimeBubbles.update(c,layout,dt,dt*.65f);
            c.waterRipples.caveCurrent.update(c,layout);
            if(resident==null)return;
            if(before>=0 && time>=before && before%PERIOD<2.1f && beat>=2.1f)
                c.crystalField.arenaFlash(c,layout,layout.w*.65f,layout.h*.5f,3,0xFF93E9F1);
            resident.hurt=Math.max(0,1-Math.abs(beat-2.1f)/.65f)*.65f;
            if(resident.kind==Boss.MUSHROOM)resident.mushroomLeafTime=beat>2.1f?beat-2.1f:-1;
        }
    }
    void draw(Painter p,float x,float y,float w,float h,float time,boolean caption) {
        int current=stop(time);float phase=time%PERIOD,slide=panelTravel(Math.min(1,phase/SLIDE));
        p.save();p.clipRect(x,y,x+w,y+h);p.translate(x,y);
        if(time>=PERIOD && slide<1)room(p,(current+HOSTS.length-1)%HOSTS.length,w,h,time,-slide*w,caption);
        room(p,current,w,h,time,time>=PERIOD?(1-slide)*w:0,caption);
        p.restore();
    }
    private void room(Painter p,int index,float w,float h,float time,float offset,boolean caption) {
        Room room=rooms[index];
        if(room==null || room.layout.w!=w || room.layout.h!=h)rooms[index]=room=new Room(HOSTS[index],arena,w,h);
        room.pose(time);
        GameCore c=room.c;Layout L=room.layout;Boss b=room.resident;int kind=HOSTS[index];
        float t=time%PERIOD,r=Math.min(w*.23f,h*.34f),greet=Math.max(0,(float)Math.sin((t-1.4f)*Math.PI/1.2f));
        p.save();p.translate(offset,0);p.clipRect(0,0,w,h);
        p.fillRect(0,0,w,h,Lands.BG[WorldCatalog.BOSSES[kind].land]);
        // Keep the native cache seam: scenery is drawn at its real destination dimensions.
        Lands.backdrop(p,c,L);Lands.scenery(p,c,L);
        float bx=w*.66f,by=h*.52f-(float)Math.sin(t*3)*r*.06f;
        float walk=panelTravel(Math.min(1,t/1.25f)),cx=-r+w*(arena?.30f:.50f)*walk+r*walk;
        float cy=h*.77f-Math.abs((float)Math.sin(t*8))*r*(t<1.4f?.15f:.04f)-greet*r*.16f;
        if(arena) {
            p.fillEllipse(bx,h*.82f,r*.8f,r*.09f,0x4430223C);
            BossScreen.versusPortrait(p,c,L,b,bx,by,r,time*2);
        }
        Trinket.drawReacting(p,14,cx,cy,r*.48f,time*2,1,greet>.1f?8:1,.7f);
        if(greet>0) {
            int color=Lands.TINT[WorldCatalog.BOSSES[kind].land];
            for(int i=0;i<3;i++) {
                float xx=w*(.39f+i*.09f),yy=h*.34f-greet*h*.12f-(i%2)*r*.14f;
                p.fillPoly(star(xx,yy,r*.10f*greet,r*.035f*greet,4,time+i),fadeBy(color,greet));
            }
        }
        if(kind==Boss.MUSHROOM)MushroomForest.canopy(p,c,L);
        if(caption && arena) {
            p.fillRect(0,h*.88f,w,h,0xAA19162D);
            p.text(Skins.bossName(p.skin(),b.kind),w*.5f,h*.965f,type(Math.min(w*.035f,h*.065f)),INK,Painter.CENTER,true);
        }
        p.restore();
    }
}
