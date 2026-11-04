package com.dddumpling.game;

/** Perspective scenery and the authored last wave of each space stage. */
final class SpaceLand extends Draw {
    static final float APPROACH=.48f;
    private SpaceLand() {}
    static boolean active(GameCore c) {return !c.survival.active && Lands.forStage(c.stage)==Lands.SPACE;}
    static boolean normalWave(GameCore c) {return active(c) && !c.powerActive();}
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float t=Lands.blend(c);
        return (c.landFrom==Lands.SPACE?1-t:0)+(active(c)?t:0);
    }
    static float rushSpeed(GameCore c) {return FastEnemies.SPEED*(normalWave(c)?3:1);}
    static float originY(Layout L) {return L.playTop+(L.deckTop-L.playTop)*.28f;}
    static float arrivalY(Layout L) {return Math.max(L.playTop,L.hudBottom())+L.enemyR*1.65f;}
    static void approach(GameCore.Enemy e,Layout L) {
        e.spaceArrival=APPROACH;
        e.spaceLane=e.sideEntry?e.pathEndX:e.baseX;
        e.spaceOriginX=L.w*.5f;e.spaceOriginY=originY(L);
        e.baseX=e.spaceOriginX;e.y=e.spaceOriginY;
        e.headerRush=false;e.rushSpan=0;e.sideEntry=false;e.sway=0;
        e.enterT=0;
    }
    static boolean approach(GameCore.Enemy e,float dt,Layout L) {
        if(e.spaceArrival<=0 || e.destroyed || e.dying || e.slideT>0)return false;
        e.spaceArrival=Math.max(0,e.spaceArrival-dt);
        float t=Math.max(0,1-e.spaceArrival/APPROACH),u=t*t*(3-2*t);
        e.enterT=t;e.baseX=e.spaceOriginX+(e.spaceLane-e.spaceOriginX)*u;
        e.y=e.spaceOriginY+(arrivalY(L)-e.spaceOriginY)*u;
        return true;
    }
    static boolean formation(GameCore c,Layout L) {
        if(!active(c) || Boss.isBossStage(c.stage) || c.spaceFormationStage==c.stage)return false;
        c.spaceFormationStage=c.stage;
        int rows=2;
        for(int row=0;row<rows;row++)for(int lane=0;lane<3;lane++) {
            GameCore.Enemy e=new GameCore.Enemy();
            Words.fill(e,lane==1?2:1,0,c.rnd,c.playRosterFull());
            e.phase=row*2+lane;e.baseX=L.w*(lane==0?.12f:lane==1?.5f:.88f);
            if(lane!=1) {
                boolean full=c.playRosterFull();int half=Roster.count(full)/2;
                int key=Roster.ordinal(full,e.word[0])%half+(lane==2?half:0);
                e.word[0]=Roster.at(full,key);
            }
            e.fastSingle=lane!=1;e.swampKind=lane==1?(row==0?SwampEnemies.DIVE:SwampEnemies.FOG):SwampEnemies.NONE;
            e.speed=((L.dangerY+L.enemyR*2.2f)/c.travelSeconds())*(lane==1?.62f:rushSpeed(c)*.75f);
            approach(e,L);
            // Rows enter in succession; everyone shares the same distant origin.
            e.spaceArrival+=row*.85f;
            c.enemies.add(e);
        }
        return true;
    }
    /** Stars keep their positions as the flow turns; only offscreen stars are recycled. */
    static final class Starfield {
        static final int PER_LAYER=120,COUNT=PER_LAYER*3;
        final float[] x=new float[COUNT],y=new float[COUNT],depth=new float[COUNT],
                vx=new float[COUNT],vy=new float[COUNT],age=new float[COUNT];
        final int[] cycle=new int[COUNT];
        boolean ready;
        void reset() {ready=false;}
        static int seed(int i) {return 1201+(i%PER_LAYER)*37+(i/PER_LAYER)*743;}
        void prepare(Layout L) {
            if(ready)return;
            ready=true;
            float horizon=originY(L)/L.h;
            for(int i=0;i<COUNT;i++) {
                int seed=seed(i);float t=hash(seed),angle=hash(seed+9)*Softbody.TAU;
                float reach=t*t*(.65f+hash(seed+4)*.40f);
                x[i]=.5f+(float)Math.cos(angle)*reach*L.h/L.w;
                y[i]=horizon+(float)Math.sin(angle)*reach;
                depth[i]=t;age[i]=1;cycle[i]=0;vx[i]=vy[i]=0;
            }
        }
        void update(GameCore c,Layout L,float dt) {
            if(presence(c)<=0) {reset();return;}
            prepare(L);
            if(dt<=0)return;
            float horizon=originY(L);
            float rate=c.boss.active()?.23f:.14f,turn=c.spaceTurn;
            for(int i=0;i<COUNT;i++) {
                int seed=seed(i);float layer=.65f+(i/PER_LAYER)*.3f;
                float dx=(x[i]-.5f)*L.w,dy=y[i]*L.h-horizon;
                float length=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy));
                float speed=2*Math.max(.07f,depth[i])*L.h*(.65f+hash(seed+4)*.40f)*rate*layer;
                // Blend velocity, never coordinates. The stars bend into the new flow.
                vx[i]=dx/length*(1-turn)*speed;
                vy[i]=(dy/length*(1-turn)-turn)*speed;
                x[i]+=vx[i]*dt/L.w;y[i]+=vy[i]*dt/L.h;
                depth[i]=Math.min(1,depth[i]+dt*rate*layer);age[i]+=dt;
                float margin=L.h*.23f/L.w;
                if(x[i]<-margin || x[i]>1+margin || y[i]<-.23f || y[i]>L.deckTop/L.h+.23f) {
                    int next=seed+(++cycle[i])*157;
                    if(turn>.5f) {
                        x[i]=hash(next+17);y[i]=L.deckTop/L.h+.22f;
                        depth[i]=.45f+hash(next+21)*.45f;
                    } else {
                        // Replenish at varied depths, fading in. A center-only emitter leaves
                        // the lower field empty when the upward population turns outward.
                        float angle=hash(next+9)*Softbody.TAU,ax=(float)Math.cos(angle),ay=(float)Math.sin(angle);
                        float t=.12f+hash(next+21)*.80f;
                        float edge=Math.min(L.w*.5f/Math.max(.001f,Math.abs(ax)),
                                (ay>0?L.deckTop-horizon:horizon)/Math.max(.001f,Math.abs(ay)));
                        float reach=edge*t*t;
                        x[i]=.5f+ax*reach/L.w;y[i]=(horizon+ay*reach)/L.h;depth[i]=t;
                    }
                    age[i]=0;vx[i]=vy[i]=0;
                }
            }
        }
    }
    static void backdrop(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        Starfield stars=c.spaceStars;stars.prepare(L);
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int i=0;i<Starfield.COUNT;i++) {
            int layer=i/Starfield.PER_LAYER;
            float t=stars.depth[i],xx=stars.x[i]*L.w,yy=stars.y[i]*L.h;
            float r=2*L.unit*(.018f+.045f*t)*(1+layer*.22f);
            int color=layer==0?0xFF887CC9:layer==1?0xFFA9CCFF:0xFFFFE9CB;
            int a=(int)(fade*Math.min(1,stars.age[i]*4)*Math.min(1,t*6)*(95+layer*65));
            p.fillCircle(xx,yy,Math.max(.7f,r),Glyph.withAlpha(color,a));
        }
        p.restore();
    }
    static void prop(Painter p,float x,float y,float r,int a,float clock,boolean silhouette) {
        int col=Glyph.withAlpha(silhouette?0xFF8A839B:0xFF9193E7,a);
        p.save();p.translate(x,y);
        ChaosCloudArt.ellipse(p,0,0,r*1.32f,r*.40f,Glyph.withAlpha(0xFFBDEFFF,a*2/3),r*.15f);
        p.fillCircle(0,0,r*.76f,col);
        p.fillEllipse(-r*.15f,-r*.17f,r*.49f,r*.42f,Glyph.withAlpha(0xFFDBCDFF,a/3));
        p.line(-r*.65f,r*.38f,r*.78f,r*.10f,Glyph.withAlpha(0xFFCDD8FF,a*3/4),r*.12f);
        p.restore();
        for(int i=0;i<3;i++) {
            float angle=clock*.6f+i*2.1f;
            p.fillPoly(star(x+(float)Math.cos(angle)*r*1.5f,y+(float)Math.sin(angle)*r*.9f,
                    r*.12f,r*.04f,4,angle),Glyph.withAlpha(0xFFFFEDB7,a));
        }
    }
}
