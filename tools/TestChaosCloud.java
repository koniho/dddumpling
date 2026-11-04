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
    static void powerEntries(Layout L) {
        group("Space power-up arrivals stay on the playfield");
        for(int mode:new int[]{Power.FLURRY,Power.NINJA,Power.TEAM})for(boolean side:new boolean[]{false,true})for(int slot:new int[]{1,2}) {
            GameCore c=fight(L);c.boss.leave();c.stage=31;c.collected=1;c.runWho=0;
            c.startFrenzy(mode,L);c.powerSpawnedEnemies=slot;c.powerReplacements.addLast(side);c.spawnTimer=0;
            c.update(.001f,L);
            String tag=mode+"/"+side+"/"+slot;
            check("requested power enemy spawns "+tag,c.enemies.size()==1);
            if(c.enemies.size()!=1)continue;
            GameCore.Enemy e=c.enemies.get(0);
            check("replacement request consumed "+tag,c.powerReplacements.isEmpty());
            for(int n=0;n<35;n++)SpaceLand.approach(e,.02f,L);
            float first=c.tileX(e,0,L),last=c.tileX(e,e.word.length-1,L);
            check("power entry ends visibly inside playfield "+tag,e.spaceArrival==0
                    && first>=L.playLeft && last<=L.playRight && e.y>L.hudBottom() && e.y<L.dangerY);
            c.destroyWord(e,c.enemyCentreX(e),e.y,L);
            check("replacement keeps alternating entry source "+tag,
                    c.powerReplacements.size()==1 && c.powerReplacements.peekFirst()==!side);
        }
    }
    static void starReturnCoverage(Layout L) {
        group("starfield stays populated after a summary");
        for(int fps:new int[]{30,120})for(int hold:new int[]{2,8}) {
            GameCore c=fight(L);c.boss.leave();c.stage=31;
            boolean covered=true;
            for(int frame=0;frame<fps*(10+hold+6);frame++) {
                boolean summary=frame>=fps*10 && frame<fps*(10+hold);
                c.state=summary?GameCore.BONUS:GameCore.PLAY;
                c.spaceTurn+=((summary?1:0)-c.spaceTurn)*(1-(float)Math.exp(-2.4f/fps));
                c.spaceStars.update(c,L,1f/fps);
                if(frame<fps*(10+hold) || frame%(fps/2)!=0)continue;
                int[] bands=new int[6];
                for(int i=0;i<SpaceLand.Starfield.COUNT;i++) {
                    float x=c.spaceStars.x[i],y=c.spaceStars.y[i]*L.h;
                    if(x>=0 && x<=1 && y>=L.playTop && y<L.deckTop && c.spaceStars.age[i]>.15f)
                        bands[Math.min(5,(int)((y-L.playTop)/(L.deckTop-L.playTop)*6))]++;
                }
                for(int count:bands)covered&=count>0;
            }
            check("return keeps stars in every playfield band "+fps+"/"+hold,covered);
        }
    }
    static void all(Layout L) {
        powerEntries(L);
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
        check("fog is present inside a flanked formation",c.enemies.get(4).swampKind==SwampEnemies.FOG);
        for(GameCore.Enemy e:c.enemies) {
            check("enemy enters from starfield origin",e.baseX==L.w*.5f && e.y==SpaceLand.originY(L));
            // Delayed formation rows use the same path once their launch begins.
            e.spaceArrival=SpaceLand.APPROACH;
            SpaceLand.approach(e,SpaceLand.APPROACH*.5f,L);
            check("arrival fans upward from the star origin",e.y<SpaceLand.originY(L) && e.y>SpaceLand.arrivalY(L));
            for(int k=0;k<100;k++)SpaceLand.approach(e,.02f,L);
            check("arrival finishes near top with header clearance",Math.abs(e.y-SpaceLand.arrivalY(L))<.01f
                    && e.y-L.enemyR*1.4f>L.hudBottom());
            check("arrival completes at its own lane",e.spaceArrival==0 && Math.abs(e.baseX-e.spaceLane)<.01f && e.enterT==1);
        }
        for(boolean full:new boolean[]{false,true}) {
            c.fullRoster=c.runFullRoster=full;c.kidsRun=!full;c.spaceFormationStage=-1;c.enemies.clear();
            SpaceLand.formation(c,L);int half=Roster.count(full)/2;
            for(int row=0;row<2;row++) {
                GameCore.Enemy left=c.enemies.get(row*3),right=c.enemies.get(row*3+2);
                check("formation flank prompts match hands "+full+"/"+row,
                        Roster.ordinal(full,left.word[0])<half && Roster.ordinal(full,right.word[0])>=half);
                float ordinary=(L.dangerY+L.enemyR*2.2f)/c.travelSeconds();
                check("formation flanks slowed by one quarter "+full+"/"+row,
                        Math.abs(left.speed-ordinary*SpaceLand.rushSpeed(c)*.75f)<.01f);
            }
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
        group("Space rush frequency and formation-only fog");
        for(int mode:new int[]{-1,Power.FLURRY,Power.NINJA,Power.TEAM}) {
            c=fight(L);c.boss.leave();c.stage=31;c.mode=mode;c.modeLeft=mode<0?0:100;
            int rush=0,fog=0;
            for(int i=0;i<1000;i++) {
                GameCore.Enemy e=new GameCore.Enemy();Words.fill(e,3,0,c.rnd,true);
                e.phase=(i+.5f)*6.283f/1000;FastEnemies.assign(c,e);SwampEnemies.assign(c,e);
                if(e.fastSingle)rush++;if(e.swampKind==SwampEnemies.FOG)fog++;
            }
            check("Space rush share increased in every power mode "+mode,rush==680);
            check("ordinary Space wave has no fog "+mode,fog==0);
        }
        c=fight(L);c.boss.leave();c.stage=31;c.spawnedThisStage=3;c.pairSpawnSlots=0;
        GameCore.Enemy preceding=new GameCore.Enemy();Words.fill(preceding,1,0,c.rnd);preceding.fastSingle=true;c.enemies.add(preceding);
        for(int shot=1;shot<=3;shot++) {
            float delay=FastEnemies.nextDelay(c,L);
            if(shot<3) {
                check("rush burst follows three-quarter-second beat "+shot,c.fastEnemyNext && Math.abs(delay-.75f)<.001f);
                GameCore.Enemy follow=new GameCore.Enemy();Words.fill(follow,3,0,c.rnd);follow.phase=0;
                FastEnemies.assign(c,follow);
                check("scheduled burst target is always rush "+shot,follow.fastSingle);
                c.enemies.add(follow);c.spawnedThisStage++;
            } else check("three rushes leave a short recovery gap",!c.fastEnemyNext && c.spaceRushCount==0 && delay>=.75f);
        }
        c.startGame();check("fresh run clears rush burst",c.spaceRushCount==0 && !c.fastEnemyNext);
        for(int mode:new int[]{Power.FLURRY,Power.NINJA,Power.TEAM}) {
            c=fight(L);c.boss.leave();c.stage=31;c.collected=1;c.runWho=0;c.startFrenzy(mode,L);
            c.powerSpawnedEnemies=1;c.spaceRushCount=1;c.fastEnemyNext=true;c.spawnTimer=.75f;
            advance(c,L,.4f);
            check("power refill respects rush interval "+mode,c.enemies.isEmpty() && c.spawnTimer>.25f);
            advance(c,L,.4f);
            check("power rush arrives after scheduled interval "+mode,!c.enemies.isEmpty());
        }
        starReturnCoverage(L);
        group("starfield flow continuity");
        c=fight(L);c.boss.leave();c.stage=31;c.spaceStars.prepare(L);
        SpaceLand.Starfield stars=c.spaceStars;
        for(int i=0;i<SpaceLand.Starfield.COUNT;i++) {
            stars.x[i]=.2f+(i%10)*.06f;stars.y[i]=.4f+(i%5)*.04f;
        }
        float[] beforeX=stars.x.clone(),beforeY=stars.y.clone();
        c.spaceTurn=1;stars.update(c,L,0);
        boolean stationary=true,upward=true;
        for(int i=0;i<SpaceLand.Starfield.COUNT;i++)stationary&=stars.x[i]==beforeX[i] && stars.y[i]==beforeY[i];
        check("changing flow never relocates existing stars",stationary);
        stars.update(c,L,1f/60);
        for(int i=0;i<SpaceLand.Starfield.COUNT;i++)upward&=stars.x[i]==beforeX[i] && stars.y[i]<beforeY[i];
        check("upward flow moves every existing star upward",upward);
        float oldX=stars.x[0],oldY=stars.y[0];c.spaceTurn=0;stars.update(c,L,1f/60);
        check("return to forward flow preserves continuity",Math.abs(stars.x[0]-oldX)<.03f && Math.abs(stars.y[0]-oldY)<.03f);
        c.startGame();check("new run discards star positions",!stars.ready);
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
