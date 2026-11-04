package com.dddumpling.game;

/** Perspective scenery and the authored last wave of each space stage. */
final class SpaceLand extends Draw {
    static final float APPROACH=.48f;
    private SpaceLand() {}
    static boolean active(GameCore c) {return !c.survival.active && Lands.forStage(c.stage)==Lands.SPACE;}
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float t=Lands.blend(c);
        return (c.landFrom==Lands.SPACE?1-t:0)+(active(c)?t:0);
    }
    static float rushSpeed(GameCore c) {return FastEnemies.SPEED*(active(c)?3:1);}
    static void approach(GameCore.Enemy e,Layout L) {
        e.spaceArrival=APPROACH;e.spaceLane=e.baseX;
        e.spaceOriginX=L.w*.5f;e.spaceOriginY=L.hudBottom()+L.enemyR;
        e.baseX=e.spaceOriginX;e.y=e.spaceOriginY;
        e.headerRush=false;e.rushSpan=0;e.sideEntry=false;e.sway=0;
        e.enterT=0;
    }
    static boolean approach(GameCore.Enemy e,float dt,Layout L) {
        if(e.spaceArrival<=0 || e.destroyed || e.dying || e.slideT>0)return false;
        e.spaceArrival=Math.max(0,e.spaceArrival-dt);
        float t=Math.max(0,1-e.spaceArrival/APPROACH),u=t*t;
        e.enterT=t;e.baseX=e.spaceOriginX+(e.spaceLane-e.spaceOriginX)*u;
        e.y=e.spaceOriginY+L.enemyR*3*u;
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
            e.fastSingle=lane!=1;e.swampKind=lane==1?SwampEnemies.DIVE:SwampEnemies.NONE;
            e.speed=((L.dangerY+L.enemyR*2.2f)/c.travelSeconds())*(lane==1?.62f:rushSpeed(c));
            approach(e,L);
            // Rows enter in succession; everyone shares the same distant origin.
            e.spaceArrival+=row*.85f;
            c.enemies.add(e);
        }
        return true;
    }
    static void backdrop(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        float x=L.w*.5f,y=L.playTop+(L.deckTop-L.playTop)*.28f;
        float speed=c.boss.active()?.23f:.14f;
        for(int layer=0;layer<3;layer++)for(int i=0;i<40;i++) {
            int seed=1201+i*37+layer*743;
            float t=(hash(seed)+c.skyClock*speed*(.65f+layer*.3f))%1f;
            float angle=hash(seed+9)*Softbody.TAU;
            float reach=t*t*L.h*(.65f+hash(seed+4)*.40f);
            float xx=x+(float)Math.cos(angle)*reach,yy=y+(float)Math.sin(angle)*reach;
            float turn=c.spaceTurn;
            float upX=L.w*(hash(seed+17)*1.2f-.1f),upY=L.deckTop*(1.15f-t*1.4f);
            xx+=(upX-xx)*turn;yy+=(upY-yy)*turn;
            float tail=reach*(.015f+t*.07f),r=L.unit*(.018f+.045f*t)*(1+layer*.22f);
            int color=layer==0?0xFF887CC9:layer==1?0xFFA9CCFF:0xFFFFE9CB;
            int a=(int)(fade*Math.min(1,t*6)*(95+layer*65));
            float dx=(float)Math.cos(angle)*tail*(1-turn),dy=(float)Math.sin(angle)*tail*(1-turn)-tail*turn;
            p.line(xx-dx,yy-dy,xx,yy,
                    Glyph.withAlpha(color,a/3),r*3);
            p.line(xx-dx*.65f,yy-dy*.65f,xx,yy,
                    Glyph.withAlpha(color,a),Math.max(.6f,r));
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
