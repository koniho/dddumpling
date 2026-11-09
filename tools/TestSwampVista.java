package com.dddumpling.game;

final class TestSwampVista extends Check {
    static void all(Layout L) {
        group("mangrove perspective");
        boolean approaches=true;float previousY=-1,previousScale=0;
        for(int i=0;i<=100;i++) {
            float t=i/100f,y=SwampVista.rootY(L,t),s=SwampVista.scale(t);
            approaches&=y>previousY && s>previousScale;previousY=y;previousScale=s;
        }
        check("scenery grows and approaches from a water-level horizon",approaches && SwampVista.horizon(L)>L.playTop);
        float reach=.19f*SwampVista.scale(1)*4.4f;
        check("largest crowns leave the edges before recycling",SwampVista.sideX(1,-1)+reach<0 && SwampVista.sideX(1,1)-reach>1);
        boolean continuous=true;
        for(int side=-1;side<=1;side+=2)for(int row=0;row<SwampVista.ROWS-1;row++) {
            float clock=(1-(side>0?.43f:0))/(SwampVista.SPEED*SwampVista.ROWS);
            float before=SwampVista.travel(clock-.0001f,row,side),after=SwampVista.travel(clock+.0001f,row+1,side);
            continuous&=Math.abs(before-after)<.0001f;
            continuous&=Math.abs(SwampVista.bend(clock-.0001f,SwampVista.depth(before))-SwampVista.bend(clock+.0001f,SwampVista.depth(after)))<.0001f;
        }
        check("row rollover preserves projected positions",continuous);
        float left=0,right=0;
        for(int i=0;i<=60;i++) {float offset=SwampVista.bend(0,i/100f);left=Math.min(left,offset);right=Math.max(right,offset);}
        check("water corridor visibly bends in both directions",left<-.10f && right>.10f);
        GameCore c=TestLandBonuses.start(L,21,true,248),twin=TestLandBonuses.start(L,21,true,248);
        c.landBonus.collectedStages=-1;c.skyClock=18;
        Probe p=new Probe();SwampVista.draw(p,c,L,1);
        check("perspective art preserves gameplay randomness",c.rnd.nextLong()==twin.rnd.nextLong());
        check("scenery stays bounded and extends behind the header ("+p.calls+" draws)",p.calls>100 && p.calls<2000 && p.top<=0);
        boolean bounded=true;int peak=0;
        for(int age=0;age<=90;age+=3) {
            c.skyClock=age;Probe frame=new Probe();SwampVista.draw(frame,c,L,1);
            bounded&=frame.calls<2000;peak=Math.max(peak,frame.calls);
        }
        check("magic scenery stays bounded throughout travel ("+peak+" draws)",bounded);c.skyClock=18;
        Probe hidden=new Probe();SwampVista.draw(hidden,c,L,0);check("faded scenery does no drawing",hidden.calls==0);
        c.cloudUi.reduceMotion=true;Probe still=new Probe();SwampVista.draw(still,c,L,1);
        c.clock+=20;Probe later=new Probe();SwampVista.draw(later,c,L,1);
        check("reduced motion freezes ambient scenery at fixed travel",still.hash==later.hash);
        c.paused=true;float sky=c.skyClock;c.update(.1f,L);check("pause holds the perspective camera",c.skyClock==sky);
        c.paused=false;c.settingsOpen=true;c.update(.1f,L);check("settings hold the perspective camera",c.skyClock==sky);
        c=TestLandBonuses.start(L,21,true,248);
        float far=LandBonus.size(c,L),farY=LandBonus.y(c,L);c.skyClock+=10;
        check("flamingo approaches and grows with the scenery",LandBonus.size(c,L)>far && LandBonus.y(c,L)>farY);
        check("flamingo feet meet the projected water surface",Math.abs(LandBonus.y(c,L)+LandBonus.size(c,L)*.86f-SwampVista.rootY(L,LandBonus.flamingoTravel(c)))<.01f);
        check("projected flamingo remains collectible",c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L)));
        c=TestLandBonuses.start(L,25,false,248);c.boss.intro=c.boss.introDuration();
        check("boss arrival starts in the travelling swamp",SwampVista.arena(c)==0);
        c.boss.intro-=.45f;check("boss arrival crossfades the scenery",Math.abs(SwampVista.arena(c)-.5f)<.001f);
        c.boss.intro=0;check("established boss arena replaces the perspective scene",SwampVista.arena(c)==1);
    }
    private static final class Probe extends OpacityPainter {
        long hash=1;int calls;float top=Float.MAX_VALUE;
        Probe(){super(null,1);}
        void record(int color,float... values){calls++;hash=hash*31+color;for(float v:values)hash=hash*31+Float.floatToIntBits(v);}
        public void fillPoly(float[] xy,int c){for(int i=1;i<xy.length;i+=2)top=Math.min(top,xy[i]);record(c,xy);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){top=Math.min(top,y-ry);record(c,x,y,rx,ry);}
        public void fillCircle(float x,float y,float r,int c){record(c,x,y,r);}
        public void fillRect(float l,float t,float r,float b,int c){top=Math.min(top,t);record(c,l,t,r,b);}
        public void line(float x,float y,float a,float b,int c,float w){record(c,x,y,a,b,w);}
        public void polyline(float[] xy,int c,float w){record(c,xy);hash=hash*31+Float.floatToIntBits(w);}
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w){record(c,x,y,rx,ry,start,sweep,w);}
    }
}
