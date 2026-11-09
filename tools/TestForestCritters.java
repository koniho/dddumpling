package com.dddumpling.game;

final class TestForestCritters extends Check {
    static int count(GameCore c){int n=0;for(ForestCritters.Critter a:c.forestCritters.animals)if(a.age>=0)n++;return n;}
    static GameCore spawn(Layout L) {
        GameCore c=TestLandBonuses.start(L,16,true,947);
        GameCore.Enemy e=add(c,L,new int[]{0,1,4},L.h*.43f);
        c.destroyWord(e,c.enemyCentreX(e),e.y,L);
        check("one cleared word startles three animals",count(c)==3);
        c.destroyWord(e,c.enemyCentreX(e),e.y,L);
        check("duplicate clear cannot startle more animals",count(c)==3);
        return c;
    }
    static void all(Layout L) {
        group("forest critter scatter");
        GameCore idle=TestLandBonuses.start(L,16,true,947);idle.forestCritters.update(idle,L,0);
        Positions resting=new Positions();idle.forestCritters.draw(resting,idle,L,1);
        check("animals are visible before any word is cleared",resting.calls>0 && count(idle)==0);
        float[] banksBefore=new float[ForestCritters.RESIDENTS],xBefore=new float[ForestCritters.RESIDENTS];
        for(int i=0;i<banksBefore.length;i++){banksBefore[i]=idle.forestCritters.residents[i].z;xBefore[i]=ForestCritters.x(idle.forestCritters.residents[i],0);}
        idle.forestCritters.cleared(idle,L,L.w*.5f,L.h*.43f);
        int inherited=0;
        for(ForestCritters.Critter a:idle.forestCritters.animals)if(a.age>=0) {
            for(int i=0;i<banksBefore.length;i++)if(a.z==banksBefore[i] && ForestCritters.x(a,0)==xBefore[i] && a.startAlpha>.7f){inherited++;break;}
        }
        check("clear sends existing residents from their exact bank positions",inherited==3);
        int hidden=0;for(ForestCritters.Critter a:idle.forestCritters.residents)if(a.age<0)hidden++;
        check("departing residents are replaced after a quiet delay",hidden==3);
        idle.forestCritters.update(idle,L,3);
        hidden=0;for(ForestCritters.Critter a:idle.forestCritters.residents)if(a.age<0)hidden++;
        check("bank population recovers after a clear",hidden==0);
        idle.cloudUi.reduceMotion=true;Positions still=new Positions();idle.forestCritters.draw(still,idle,L,1);
        idle.forestCritters.update(idle,L,.2f);Positions later=new Positions();idle.forestCritters.draw(later,idle,L,1);
        check("reduced motion freezes idle sniffing and breathing",still.hash==later.hash);
        GameCore c=spawn(L);ForestCritters.Critter first=c.forestCritters.animals[0];
        boolean banks=true,weave=false;int mice=0,frogs=0;
        for(ForestCritters.Critter a:c.forestCritters.animals)if(a.age>=0) {
            if(a.kind==0)mice++;else frogs++;
            float previous=0,dy=0;
            for(int i=0;i<=100;i++) {
                float t=i/100f,z=ForestCritters.z(a,t);
                float distance=a.side*(ForestCritters.x(a,t)-ForestTerrain.streamX(z))-ForestTerrain.streamWidth(z);
                banks&=distance>.03f && distance>=previous-.00001f;previous=distance;
                if(i>0) {
                    float delta=z-ForestCritters.z(a,(i-1)/100f);
                    if(delta*dy<0)weave=true;dy=delta;
                }
            }
        }
        check("mice and a frog scatter together",mice==2 && frogs==1);
        check("winding routes stay on dry banks and move away from water",banks && weave);
        check("animals stagger their starts",first.delay<c.forestCritters.animals[1].delay);
        c.paused=true;c.update(.1f,L);check("pause freezes scatter",first.age==0);
        c.paused=false;c.settingsOpen=true;c.update(.1f,L);check("settings freeze scatter",first.age==0);
        c.settingsOpen=false;c.update(.1f,L);check("gameplay advances scatter",first.age>0);
        GameCore twin=TestLandBonuses.start(L,16,true,947),other=TestLandBonuses.start(L,16,true,947);
        twin.forestCritters.cleared(twin,L,L.w*.5f,L.h*.4f);
        check("cosmetic variation does not consume combat RNG",twin.rnd.nextLong()==other.rnd.nextLong());
        for(int i=0;i<40;i++)c.forestCritters.cleared(c,L,L.w*.5f,L.h*.4f);
        check("rapid clears keep a bounded pool",count(c)==ForestCritters.CAPACITY);
        c.forestCritters.update(c,L,4);check("animals expire after escaping",count(c)==0);
        for(int stage:new int[]{1,6,11,20,21,26}) {
            c=TestLandBonuses.start(L,stage,true,947);c.forestCritters.cleared(c,L,L.w*.5f,L.h*.4f);
            check("no forest animals in other lands or boss arena "+stage,count(c)==0);
        }
        c=spawn(L);c.enterStage(20);c.paused=true;c.update(.1f,L);
        check("boss transition clears animals above pause return",count(c)==0);
        c=spawn(L);c.state=GameCore.BONUS;c.paused=true;c.update(.1f,L);
        check("interlude clears animals above early returns",count(c)==0);
        c=spawn(L);c.lives=1;c.takeHit(L.w*.5f,L);check("death clears animals immediately",count(c)==0);
        c=spawn(L);c.toTitle();check("title clears animals",count(c)==0);
        c=spawn(L);c.startGame();check("new run clears animals",count(c)==0);
        c=spawn(L);c.cloudUi.reduceMotion=true;
        c.forestCritters.update(c,L,.4f);Positions before=new Positions();c.forestCritters.draw(before,c,L,1);
        c.forestCritters.update(c,L,.6f);Positions after=new Positions();c.forestCritters.draw(after,c,L,1);
        check("reduced motion holds animal positions and footfalls",before.calls>0 && before.hash==after.hash);
    }
    private static final class Positions extends OpacityPainter {
        long hash=1;int calls;
        Positions(){super(null,1);}
        void point(float... v){calls++;for(float f:v)hash=31*hash+Float.floatToIntBits(f);}
        public void fillCircle(float x,float y,float r,int color){point(x,y,r);}
        public void fillEllipse(float x,float y,float rx,float ry,int color){point(x,y,rx,ry);}
        public void polyline(float[] xy,int color,float width){point(xy);}
    }
}
