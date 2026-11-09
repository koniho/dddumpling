package com.dddumpling.game;

final class TestSwampFireflies extends Check {
    private static int count(GameCore c){int n=0;for(float age:c.swampFireflies.age)if(age>=0)n++;return n;}
    private static GameCore cleared(Layout L) {
        GameCore c=TestLandBonuses.start(L,21,true,248);
        GameCore.Enemy e=add(c,L,new int[]{0,1,4},L.h*.48f);
        c.destroyWord(e,c.enemyCentreX(e),e.y,L);
        check("word clear scatters four resident fireflies",count(c)==SwampFireflies.PER_CLEAR);
        c.swampFireflies.update(c,.2f);float before=0;for(float age:c.swampFireflies.age)before+=age;
        c.destroyWord(e,c.enemyCentreX(e),e.y,L);float after=0;for(float age:c.swampFireflies.age)after+=age;
        check("duplicate clear does not restart the scatter",before==after);
        return c;
    }
    static void all(Layout L) {
        group("swamp firefly scatter");
        GameCore c=cleared(L);float x=L.w*.5f,y=L.h*.48f;
        c.swampFireflies.reset();c.swampFireflies.cleared(c,L,x,y);c.swampFireflies.update(c,.25f);
        boolean away=true,continuous=true;
        for(int i=0;i<SwampFireflies.COUNT;i++)if(c.swampFireflies.age[i]>=0) {
            float dx=SwampFireflies.baseX(i,c.skyClock)*L.w-x,dy=SwampFireflies.baseY(i,c.skyClock)*L.h-y;
            away&=dx*c.swampFireflies.offset(i,false)*L.w+dy*c.swampFireflies.offset(i,true)*L.h>0;
        }
        check("startled fireflies move away from the cleared enemy",away);
        float[] ox=new float[SwampFireflies.COUNT],oy=new float[SwampFireflies.COUNT];
        for(int i=0;i<ox.length;i++){ox[i]=c.swampFireflies.offset(i,false);oy[i]=c.swampFireflies.offset(i,true);}
        c.swampFireflies.cleared(c,L,x,y);
        for(int i=0;i<ox.length;i++)continuous&=Math.abs(ox[i]-c.swampFireflies.offset(i,false))<.00001f && Math.abs(oy[i]-c.swampFireflies.offset(i,true))<.00001f;
        check("another clear redirects fireflies without a position jump",continuous);
        GameCore twin=TestLandBonuses.start(L,21,true,248),other=TestLandBonuses.start(L,21,true,248);
        twin.swampFireflies.cleared(twin,L,x,y);
        check("scatter does not consume gameplay randomness",twin.rnd.nextLong()==other.rnd.nextLong());
        for(int n=0;n<40;n++){c.swampFireflies.cleared(c,L,x,y);c.swampFireflies.update(c,.05f);}
        check("rapid clears keep the fixed resident population",count(c)>0 && count(c)<=SwampFireflies.COUNT);
        c.swampFireflies.update(c,3);boolean rested=count(c)==0;
        for(int i=0;i<ox.length;i++)rested&=c.swampFireflies.offset(i,false)==0 && c.swampFireflies.offset(i,true)==0;
        check("fireflies settle back into their idle drift",rested);
        c=cleared(L);int active=0;while(c.swampFireflies.age[active]<0)active++;
        float age=c.swampFireflies.age[active];c.paused=true;c.update(.1f,L);
        check("pause holds firefly scatter",age==c.swampFireflies.age[active]);
        c.paused=false;c.settingsOpen=true;c.update(.1f,L);
        check("settings hold firefly scatter",age==c.swampFireflies.age[active]);
        for(int stage:new int[]{1,6,11,16,25,26}) {
            c=TestLandBonuses.start(L,stage,true,248);c.swampFireflies.cleared(c,L,x,y);
            check("no swamp scatter in other lands or boss stage "+stage,count(c)==0);
        }
        c=cleared(L);c.enterStage(25);c.paused=true;c.update(.1f,L);check("boss transition clears scatter before pause",count(c)==0);
        c=cleared(L);c.state=GameCore.BONUS;c.paused=true;c.update(.1f,L);check("interlude clears scatter before early return",count(c)==0);
        c=cleared(L);c.lives=1;c.takeHit(L.w*.5f,L);check("death clears scatter immediately",count(c)==0);
        c=cleared(L);c.toTitle();check("title clears scatter",count(c)==0);
        c=cleared(L);c.startGame();check("new run clears scatter",count(c)==0);
        c=TestLandBonuses.start(L,21,true,248);c.cloudUi.reduceMotion=true;c.swampFireflies.cleared(c,L,x,y);
        check("reduced motion avoids darting fireflies",count(c)==0);
    }
}
