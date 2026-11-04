package com.dddumpling.game;

final class TestChaosCloud extends Check {
    static GameCore fight(Layout L) {
        GameCore c=new GameCore(new Mem(),234L);c.startGame();c.stage=WorldCatalog.bossStage(Boss.CHAOS_CLOUD);
        c.onboarding.saved=-1;c.onboarding.savedPowers=-1;c.enemies.clear();c.boss.begin(Boss.CHAOS_CLOUD,c.stage,c.rnd,true);
        c.boss.update(.01f,L,c.rnd);c.boss.intro=0;c.stageBanner=c.stageGap=c.buddy.entryLeft=0;c.landBlend=1;c.devInfiniteLives=true;
        return c;
    }
    static void stroke(GameCore c,Layout L) {
        Boss b=c.boss;int d=b.cloud.lastDirection==1?-1:1;float r=Boss.bodyR(L),x=b.bodyX(L),y=b.bodyY(L);
        b.cloud.touch(c,L,0,234,x-d*r,y);b.cloud.touch(c,L,1,234,x+d*r,y);
    }
    static void all(Layout L) {
        group("space progression and finale");
        check("space follows rainbow",Lands.forStage(31)==Lands.SPACE && Boss.kindFor(35)==Boss.CHAOS_CLOUD
                && Lands.unlockBoss(Lands.SPACE)==Boss.RAINBOW_CAT && Lands.forStage(36)==0);
        GameCore c=fight(L);Boss b=c.boss;
        check("twelve slash hits",b.hpMax==12);
        check("space rushes triple existing rush speed",SpaceLand.rushSpeed(c)==FastEnemies.SPEED*3);
        c.boss.leave();c.stage=31;c.spaceFormationStage=-1;c.enemies.clear();
        check("final formation starts once",SpaceLand.formation(c,L) && !SpaceLand.formation(c,L) && c.enemies.size()==6);
        check("slower middle word flanked by runners",c.enemies.get(1).swampKind==SwampEnemies.DIVE
                && c.enemies.get(0).fastSingle && c.enemies.get(2).fastSingle
                && c.enemies.get(0).speed>c.enemies.get(1).speed*5);
        for(GameCore.Enemy e:c.enemies) {
            for(int k=0;k<100;k++)SpaceLand.approach(e,.02f,L);
            check("arrival completes at its own lane",e.spaceArrival==0 && Math.abs(e.baseX-e.spaceLane)<.01f && e.enterT==1);
        }
        c.state=GameCore.BONUS;c.bonusTimer=10;c.update(.1f,L);
        float turned=c.spaceTurn;
        check("stage break turns stars smoothly upward",turned>0 && turned<1);
        c.state=GameCore.PLAY;c.pendingBonus=false;c.stageGap=0;c.update(.1f,L);
        check("play turns stars back smoothly",c.spaceTurn>0 && c.spaceTurn<turned);
        c.startGame();check("new run resets direction",c.spaceTurn==0);
        long old=Collect.SAVE_TAG|((1L<<62)-1);
        check("old collection does not acquire the new friend",!Collect.has(Collect.decode(old),62));
        check("new friend saves with old IDs intact",Collect.decode(Collect.encode(Collect.MASK))==Collect.MASK);
        group("fast finite boss phases");
        for(int fps:new int[]{30,60,120}) {
            c=fight(L);b=c.boss;int[] emitted=new int[3];int previous=0,damage=0;
            for(int frame=0;frame<fps*90 && !b.beaten;frame++) {
                int phase=b.cloud.phase;damage+=b.update(1f/fps,L,c.rnd);
                emitted[phase]+=b.cloud.total-previous;previous=b.cloud.total;
                for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i] && b.cloud.targetable(b,i)) {
                    check("phase projectile hp "+phase,b.bhpMax[i]==(phase==1?3:1));
                    if(phase==2)check("linked projectiles use opposite hands",b.cloud.mate[i]>=0
                            && Roster.ordinal(b.rosterFull,b.bglyph[i])/3!=Roster.ordinal(b.rosterFull,b.bglyph[b.cloud.mate[i]])/3);
                    b.press(b.bglyph[i],c.rnd,L);
                }
                if(b.cloud.vulnerable) {
                    float hp=b.hp,x=b.bodyX(L),y=b.bodyY(L),r=Boss.bodyR(L);
                    check("tiny swipe cannot damage",!b.cloud.slash(b,L,x,y,x+r*.1f,y) && hp==b.hp);
                    stroke(c,L);
                    if(b.cloud.vulnerable)check("must reverse next slash",!b.cloud.slash(b,L,x-r*b.cloud.lastDirection,y,x+r*b.cloud.lastDirection,y));
                }
            }
            check("exactly fifty projectiles, three phases, no leak "+fps,b.beaten && damage==0
                    && b.cloud.total==50 && emitted[0]==16 && emitted[1]==16 && emitted[2]==18 && b.boltCount()==0);
            b.leave();check("leave releases touch and phases",b.cloud.pointer<0 && b.cloud.total==0);
        }
        group("linked deadline and pause");
        c=fight(L);b=c.boss;b.cloud.phase=2;b.cloud.next=0;b.cloud.update(b,.01f,L,c.rnd);
        int emerging=-1;for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i]){emerging=i;break;}
        check("projectile starts at its source particle",Math.abs(b.boltX(emerging,L)-b.cloud.sourceX[emerging])<.01f
                && !b.cloud.targetable(b,emerging));
        b.cloud.age(b,ChaosCloud.MORPH+.02f);int first=-1;for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i]){first=i;break;}
        int other=b.cloud.mate[first];b.press(b.bglyph[first],c.rnd,L);
        check("one half waits rather than clears",b.blive[first] && b.cloud.wait[first]>0);
        b.cloud.age(b,ChaosCloud.LINK_WINDOW+.01f);
        check("late partner resets first half",b.bhp[first]==1 && b.cloud.wait[first]<=0);
        b.press(b.bglyph[first],c.rnd,L);b.press(b.bglyph[other],c.rnd,L);
        check("paired presses clear both",!b.blive[first] && !b.blive[other]);
        b.cloud.vulnerable=true;b.cloud.touch(c,L,0,234,0,b.bodyY(L));Pause.release(c);
        check("pause releases swipe",b.cloud.pointer<0);
    }
}
