package com.dddumpling.game;

final class TestSwampVista extends Check {
    static void all(Layout L) {
        group("grass bank perspective");
        boolean approaches=true;float previousY=-1,previousScale=0;
        for(int i=0;i<=100;i++) {
            float t=i/100f,y=SwampVista.rootY(L,t),s=SwampVista.scale(t);
            approaches&=y>previousY && s>previousScale;previousY=y;previousScale=s;
        }
        check("scenery grows and approaches from a water-level horizon",approaches && SwampVista.horizon(L)>L.playTop);
        check("grass leaves below the deck before recycling",SwampVista.rootY(L,(SwampVista.DRAW_ROWS-1f)/SwampVista.ROWS)-L.w*SwampGrass.RADIUS*SwampVista.scale((SwampVista.DRAW_ROWS-1f)/SwampVista.ROWS)*1.05f*SwampGrass.TOP>L.deckTop);
        check("horizon forms a gentle symmetrical arc",SwampVista.horizonAt(L,0)>SwampVista.horizonAt(L,L.w*.5f)
                && Math.abs(SwampVista.horizonAt(L,0)-SwampVista.horizonAt(L,L.w))<.01f);
        check("distant ground follows the arc while foreground stays level",SwampVista.rootY(L,.1f,0)>SwampVista.rootY(L,.1f)
                && SwampVista.rootY(L,.9f,0)==SwampVista.rootY(L,.9f));
        boolean continuous=true,rooted=true;
        for(int side=-1;side<=1;side+=2)for(int lane=0;lane<SwampVista.LANES;lane++)for(int row=0;row<SwampVista.DRAW_ROWS-1;row++) {
            float clock=(1-(side>0?.43f:0)-lane*.37f)/(SwampVista.SPEED*SwampVista.ROWS);
            float before=SwampVista.travel(clock-.0001f,row,side,lane),after=SwampVista.travel(clock+.0001f,row+1,side,lane);
            int seed=SwampVista.grassSeed(clock-.0001f,row,side,lane);
            continuous&=seed==SwampVista.grassSeed(clock+.0001f,row+1,side,lane) && Math.abs(before-after)<.0001f;
            continuous&=Math.abs(SwampVista.grassX(L,clock+.0001f,before+.0002f*SwampVista.SPEED,side,lane,seed)-SwampVista.grassX(L,clock+.0001f,after,side,lane,seed))<.001f;
            float first=(SwampVista.grassX(L,clock,before,side,lane,seed)-SwampVista.shoreX(L,clock,SwampVista.depth(before),side))/SwampVista.scale(before);
            float later=before+.10f,elapsed=.10f/SwampVista.SPEED;
            float moved=(SwampVista.grassX(L,clock+elapsed,later,side,lane,seed)-SwampVista.shoreX(L,clock+elapsed,SwampVista.depth(later),side))/SwampVista.scale(later);
            rooted&=Math.abs(first-moved)<.00001f && first*side>0;
        }
        check("row rollover preserves grass identity and projected roots",continuous);
        check("grass keeps its world offset on the same moving bank",rooted);
        boolean halfInWater=true;
        for(int side=-1;side<=1;side+=2)for(int row=1;row<SwampVista.ROWS;row++) {
            float t=SwampVista.travel(18,row,side),d=SwampVista.depth(t);
            int seed=SwampVista.grassSeed(18,row,side,0);
            float edge=SwampVista.waterEdgeX(L,18,d,side),root=SwampVista.grassX(L,18,t,side,0,seed);
            halfInWater&=Math.abs(edge-root)<.007f*SwampVista.scale(t)
                    && side*(edge-SwampVista.shoreX(L,18,d,side))>0;
        }
        check("receded banks pass through the nearest grass footprints",halfInWater);
        float left=0,right=0;
        for(int i=0;i<=60;i++) {float offset=SwampVista.bend(0,i/100f);left=Math.min(left,offset);right=Math.max(right,offset);}
        check("water corridor visibly bends in both directions",left<-.10f && right>.10f);
        GameCore c=TestLandBonuses.start(L,21,true,248),twin=TestLandBonuses.start(L,21,true,248);
        c.landBonus.collectedStages=-1;c.skyClock=18;
        boolean treesOnBanks=true,treesGentle=true;
        float[] treeMin=new float[36],treeMax=new float[36];
        java.util.Arrays.fill(treeMin,Float.MAX_VALUE);java.util.Arrays.fill(treeMax,-Float.MAX_VALUE);
        for(int age=0;age<=90;age+=3) {
            c.skyClock=age;Probe trees=new Probe();trees.treeLayout=L;trees.treeClock=age;
            SwampVista.horizonTrees(trees,c,L,1);treesOnBanks&=trees.rootsOnBanks && trees.calls==36;
            for(int i=0;i<36;i++){treeMin[i]=Math.min(treeMin[i],trees.treeXs[i]);treeMax[i]=Math.max(treeMax[i],trees.treeXs[i]);}
        }
        check("distant tree roots stay on both banks as the river bends",treesOnBanks);
        for(int i=0;i<36;i++)treesGentle&=treeMax[i]-treeMin[i]>L.w*.002f && treeMax[i]-treeMin[i]<L.w*.013f;
        check("horizon tree movement stays subtle throughout travel",treesGentle);
        c.cloudUi.reduceMotion=true;c.skyClock=0;Probe quietTrees=new Probe();SwampVista.horizonTrees(quietTrees,c,L,1);
        c.skyClock=18;Probe quietLater=new Probe();SwampVista.horizonTrees(quietLater,c,L,1);
        check("reduced motion freezes distant tree parallax",quietTrees.hash==quietLater.hash);c.cloudUi.reduceMotion=false;
        check("travelling swamp clears all cloud layers",SwampVista.presence(c)==1);
        Probe clouds=new Probe();for(int layer=0;layer<GameCore.CLOUD_LAYERS;layer++)Sky.clouds(clouds,c,L,layer,0);
        check("clear swamp sky emits no clouds",clouds.calls==0);
        Probe p=new Probe();SwampVista.draw(p,c,L,1);
        check("perspective art preserves gameplay randomness",c.rnd.nextLong()==twin.rnd.nextLong());
        check("scenery stays bounded and extends behind the header ("+p.calls+" draws)",p.calls>100 && p.calls<650 && p.top<=0);
        boolean bounded=true;int peak=0;
        for(int age=0;age<=90;age+=3) {
            c.skyClock=age;Probe frame=new Probe();SwampVista.draw(frame,c,L,1);
            bounded&=frame.calls<650;peak=Math.max(peak,frame.calls);
        }
        check("quiet scenery stays bounded throughout travel ("+peak+" draws)",bounded);c.skyClock=18;
        for(int i=0;i<12;i++) {
            c.swampFireflies.cleared(c,L,L.w*(.2f+(i%3)*.3f),L.h*(.40f+(i%4)*.09f));
            c.swampFireflies.update(c,.03f);
        }
        Probe sparkling=new Probe();SwampVista.draw(sparkling,c,L,1);
        check("rapid clear sparkle and river life stay within the scenery budget",sparkling.calls<650);
        c.swampFireflies.reset();
        boolean filled=true;
        for(float clock:new float[]{0,8,18,32}) {
            c.skyClock=clock;Probe corners=new Probe();corners.width=L.w;corners.cornerTop=SwampVista.horizon(L);corners.cornerBottom=corners.cornerTop+L.h*.12f;
            SwampVista.draw(corners,c,L,1);filled&=corners.leftGrass>0 && corners.rightGrass>0;
        }
        check("moving grass fills both upper corners below the horizon",filled);c.skyClock=18;
        Probe hidden=new Probe();SwampVista.draw(hidden,c,L,0);check("faded scenery does no drawing",hidden.calls==0);
        c.cloudUi.reduceMotion=true;Probe still=new Probe();SwampVista.draw(still,c,L,1);
        c.clock+=20;Probe later=new Probe();SwampVista.draw(later,c,L,1);
        check("reduced motion freezes ambient scenery at fixed travel",still.hash==later.hash);
        c.paused=true;float sky=c.skyClock;c.update(.1f,L);check("pause holds the perspective camera",c.skyClock==sky);
        c.paused=false;c.settingsOpen=true;c.update(.1f,L);check("settings hold the perspective camera",c.skyClock==sky);
        c=TestLandBonuses.start(L,21,true,248);
        float far=LandBonus.size(c,L),farY=LandBonus.y(c,L);c.skyClock+=10;
        check("flamingo approaches and grows with the scenery",LandBonus.size(c,L)>far && LandBonus.y(c,L)>farY);
        check("flamingo feet meet the projected water surface",Math.abs(LandBonus.y(c,L)+LandBonus.size(c,L)*.86f-SwampVista.rootY(L,LandBonus.flamingoTravel(c),LandBonus.x(c,L)))<.01f);
        check("projected flamingo remains collectible",c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L)));
        c=TestLandBonuses.start(L,25,false,248);c.boss.intro=c.boss.introDuration();
        check("boss arrival starts in the travelling swamp",SwampVista.arena(c)==0);
        c.boss.intro-=.45f;check("boss arrival crossfades the scenery",Math.abs(SwampVista.arena(c)-.5f)<.001f);
        c.boss.intro=0;check("boss arena restores its cloud treatment",SwampVista.presence(c)==0);check("established boss arena replaces the perspective scene",SwampVista.arena(c)==1);
    }
    private static final class Probe extends OpacityPainter {
        float width,cornerTop,cornerBottom;int leftGrass,rightGrass;
        Layout treeLayout;float treeClock;boolean rootsOnBanks=true;final float[] treeXs=new float[36];
        long hash=1;int calls;float top=Float.MAX_VALUE;
        Probe(){super(null,1);}
        public ArtStamp stamps(){return this;}
        public boolean swampGrass(int art,float x,float y,float r,float sway,int alpha,float mono){record(alpha,art,x,y,r,sway,mono);if(y>cornerTop && y<cornerBottom){if(x>=0 && x<width*.15f)leftGrass++;if(x>width*.85f && x<=width)rightGrass++;}return true;}

        void record(int color,float... values){calls++;hash=hash*31+color;for(float v:values)hash=hash*31+Float.floatToIntBits(v);}
        public void fillPoly(float[] xy,int c){
            if(treeLayout!=null) {
                treeXs[calls]=xy[0];
                float base=-Float.MAX_VALUE;for(int i=1;i<xy.length;i+=2)base=Math.max(base,xy[i]);
                float d=SwampVista.waterDepth(treeLayout,base);
                float left=treeLayout.w*SwampVista.waterEdgeX(treeLayout,treeClock,d,-1);
                float right=treeLayout.w*SwampVista.waterEdgeX(treeLayout,treeClock,d,1);
                for(int i=0;i<xy.length;i+=2)if(xy[i+1]>=base-.01f)rootsOnBanks&=xy[i]<left || xy[i]>right;
            }
            for(int i=1;i<xy.length;i+=2)top=Math.min(top,xy[i]);record(c,xy);
        }
        public void fillEllipse(float x,float y,float rx,float ry,int c){top=Math.min(top,y-ry);record(c,x,y,rx,ry);}
        public void fillCircle(float x,float y,float r,int c){record(c,x,y,r);}
        public void fillRect(float l,float t,float r,float b,int c){top=Math.min(top,t);record(c,l,t,r,b);}
        public void line(float x,float y,float a,float b,int c,float w){record(c,x,y,a,b,w);}
        public void polyline(float[] xy,int c,float w){record(c,xy);hash=hash*31+Float.floatToIntBits(w);}
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w){record(c,x,y,rx,ry,start,sweep,w);}
    }
}
