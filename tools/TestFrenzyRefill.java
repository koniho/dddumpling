package com.dddumpling.game;

/** Keep targets arriving after fast clears without increasing pressure on an occupied field. */
final class TestFrenzyRefill extends Check {
    private TestFrenzyRefill() {}

    static void all(Layout L) {
        group("frenzy replenishment");
        GameCore c = new GameCore(new Mem(), 827L);
        c.startGame();
        c.enemies.clear();
        c.spawnTimer = 2f;
        float calm = c.spawnInterval();
        check("ordinary waves keep their normal spawn delay", Power.spawnDelay(c, L) == calm);
        c.startFrenzy(Power.FLURRY, L);
        float regular = calm / Power.spawnRate(c.ramp());
        check("activation shortens the old wave timer on an empty field", c.spawnTimer < regular);
        check("empty field gets a faster replacement", Power.spawnDelay(c, L) < regular);
        float emptyDelay=Power.spawnDelay(c,L);
        boolean noPauses=true;
        for(int count=1;count<=24;count++) {
            c.powerSpawnedEnemies=count;
            noPauses &= Power.spawnDelay(c,L)==emptyDelay;
        }
        check("formation boundaries never delay a cleared field",noPauses);
        c.powerSpawnedEnemies=0;
        GameCore.Enemy visible = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        visible.baseX = L.w / 2f;
        visible.sway = 0;
        check("visible unfinished work restores the stage's normal frenzy pace",
                Math.abs(Power.spawnDelay(c, L) - regular) < 1e-5f);
        visible.need[0] = 4;
        check("an unfinished stack never triggers extra pressure", Power.spawnDelay(c, L) == regular);
        visible.pos = visible.word.length;
        check("last hits in flight do not delay the next target", Power.spawnDelay(c, L) < regular);
        visible.y = L.dangerY - L.enemyR * 2f;
        check("a threat near the danger line suppresses replenishment", Power.spawnDelay(c, L) == regular);

        c.enemies.clear();
        for (int i = 0; i < 2; i++) add(c, L, new int[] {1, 2}, -L.enemyR * 3f);
        check("two incoming rows stop an unseen backlog", Power.spawnDelay(c, L) == regular);

        c.enemies.clear();
        GameCore.Enemy first = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        GameCore.Enemy second = add(c, L, new int[] {3, 4}, L.playTop + L.enemyR * 8f);
        c.destroyWord(first, first.baseX, first.y, L);
        check("one clear does not bank a burst", c.powerRefillBurst == 0);
        c.clock += 0.1f;
        c.destroyWord(second, second.baseX, second.y, L);
        check("a rapid multi-clear banks only two arrivals", c.powerRefillBurst == 2);
        GameCore.Enemy work = add(c, L, new int[] {1, 2}, L.playTop + L.enemyR * 4f);
        work.baseX = L.w / 2f;
        work.sway = 0;
        check("the short multi-clear burst keeps fresh targets coming", Power.spawnDelay(c, L) < regular);
        c.clock += 0.9f;
        check("unspent bursts expire instead of surprising the player later", Power.spawnDelay(c, L) == regular);
        c.startFrenzy(Power.FLURRY, L);
        check("a new powerup starts without stale burst credit", c.powerRefillBurst == 0);

        boolean scaled = true;
        for (int stage : new int[] {1, 7, 13, 19}) {
            c.stage = stage;
            c.enemies.clear();
            float base = c.spawnInterval() / Power.spawnRate(c.ramp());
            float refill = Power.spawnDelay(c, L);
            scaled &= refill < base && refill >= base * 0.19f;
        }
        check("refill timing follows the stage curve", scaled);
        c.modeLeft = 0f;
        check("ending a powerup restores ordinary spawn timing", Power.spawnDelay(c, L) == c.spawnInterval());
        c.startFrenzy(Power.NINJA,L);c.enemies.clear();
        float ninjaDelay=Power.spawnDelay(c,L);
        add(c,L,new int[]{1,2,3},L.playTop+L.enemyR*4);
        check("Ninja keeps its fast refill while a row is still on screen",Power.spawnDelay(c,L)==ninjaDelay
                && ninjaDelay<=.06f);
        c.enemies.get(0).y=L.dangerY-L.enemyR;
        check("Ninja relaxes its extra refill near danger",Power.spawnDelay(c,L)>ninjaDelay);
        clearSchedulesReplacement(L);
        oppositeEntrances(L);
        rapidClears(L);
    }

    private static void clearSchedulesReplacement(Layout L) {
        boolean immediate=true,keepsEarlier=true;
        for(int mode:Power.OFFERED) {
            GameCore c=new GameCore(new Mem(),1340L+mode);c.startGame();c.enemies.clear();
            c.mode=mode;c.modeLeft=Power.DURATION;c.powerSpawnedEnemies=4;c.spawnTimer=2f;
            GameCore.Enemy e=add(c,L,new int[]{1,2},L.playTop+L.enemyR*4f);
            c.destroyWord(e,c.enemyCentreX(e),e.y,L);
            immediate &= c.spawnTimer==Power.spawnDelay(c,L) && c.spawnTimer<.5f;
            c.spawnTimer=.01f;
            GameCore.Enemy next=add(c,L,new int[]{2},L.playTop+L.enemyR*4f);
            c.destroyWord(next,c.enemyCentreX(next),next.y,L);
            keepsEarlier &= c.spawnTimer==.01f;
        }
        check("each power schedules a replacement as soon as an enemy is destroyed",immediate);
        check("further clears never postpone an already pending arrival",keepsEarlier);
        GameCore calm=new GameCore(new Mem(),1349L);calm.startGame();calm.enemies.clear();calm.spawnTimer=2f;
        GameCore.Enemy e=add(calm,L,new int[]{0},L.playTop+L.enemyR*4f);
        calm.destroyWord(e,calm.enemyCentreX(e),e.y,L);
        check("ordinary clears keep their existing spawn schedule",calm.spawnTimer==2f && calm.powerReplacements.isEmpty());
    }

    private static void oppositeEntrances(Layout L) {
        group("opposite power replacement entrances");
        for(int mode:Power.OFFERED) {
            GameCore c=new GameCore(new Mem(),13460L+mode);c.startGame();c.enemies.clear();
            c.mode=mode;c.modeLeft=Power.DURATION;c.stageGap=0;c.powerSpawnedEnemies=4;
            for(boolean side:new boolean[]{true,false,true}) {
                GameCore.Enemy e=add(c,L,new int[]{1},L.playTop+L.enemyR*4);e.sideEntry=side;
                c.destroyWord(e,c.enemyCentreX(e),e.y,L);
                int count=c.powerReplacements.size();c.destroyWord(e,c.enemyCentreX(e),e.y,L);
                check("duplicate clear cannot queue another replacement "+mode,c.powerReplacements.size()==count);
            }
            boolean ordered=true,speed=true;
            for(boolean side:new boolean[]{false,true,false}) {
                c.enemies.clear();c.spawnTimer=0;c.update(DT,L);
                ordered &= c.enemies.size()==1 && c.enemies.get(0).sideEntry==side;
                if(!c.enemies.isEmpty())speed &= Math.abs(c.enemies.get(0).speed
                        -(L.dangerY+L.enemyR*2.2f)/c.travelSeconds())<.001f;
            }
            check("multi-clear replacements arrive from opposite entrances in order "+mode,ordered && c.powerReplacements.isEmpty());
            check("alternating replacements retain full descent speed "+mode,speed);

            c.enemies.clear();c.stage=19;c.powerSpawnedEnemies=0;
            c.powerReplacements.add(false);c.powerReplacements.add(true);
            check("power can still introduce linked pairs "+mode,LinkedPairs.spawn(c,L));
            check("top pairs consume only matching replacement requests "+mode,
                    c.powerReplacements.size()==1 && Boolean.TRUE.equals(c.powerReplacements.peekFirst()));
            c.powerReplacements.clear();
            GameCore.Enemy pair=c.enemies.get(0),mate=pair.link;
            c.destroyWord(pair,c.enemyCentreX(pair),pair.y,L);
            c.destroyWord(mate,c.enemyCentreX(mate),mate.y,L);
            check("clearing a top pair queues two side replacements "+mode,
                    c.powerReplacements.size()==2 && Boolean.TRUE.equals(c.powerReplacements.peekFirst())
                    && Boolean.TRUE.equals(c.powerReplacements.peekLast()));
            c.powerSpawnedEnemies=4;c.enemies.clear();c.spawnTimer=0;c.update(DT,L);
            check("a queued side replacement takes precedence over another top pair "+mode,
                    c.enemies.size()==1 && c.enemies.get(0).sideEntry && c.enemies.get(0).link==null
                    && c.powerReplacements.size()==1);
        }
        GameCore c=new GameCore(new Mem(),13466L);c.startGame();c.startFrenzy(Power.NINJA,L);
        c.enemies.clear();c.stageGap=0;c.spawnTimer=0;c.powerReplacements.add(false);
        GameCore.Enemy barrier=add(c,L,new int[]{0,1,2,3,4,5,0,1},-L.enemyR*2.2f);
        barrier.sideEntry=true;barrier.pathStartY=barrier.y;
        barrier.pathStartX=barrier.pathEndX=barrier.baseX;
        c.update(DT,L);
        check("blocked top replacements stay queued instead of falling back to a side",
                c.enemies.size()==1 && c.powerReplacements.size()==1);
        c.enemies.clear();c.update(.12f,L);
        check("queued top replacement enters once space opens",c.enemies.size()==1
                && !c.enemies.get(0).sideEntry && c.powerReplacements.isEmpty());
        c.enemies.clear();add(c,L,new int[]{1},L.dangerY-L.enemyR);
        c.powerReplacements.add(false);
        GameCore.Enemy entering=new GameCore.Enemy();entering.word=new int[]{0};entering.y=-L.enemyR*2.2f;
        PowerRush.arrange(c,entering,L,L.w*.2f,L.w*.8f);
        check("pressured top replacements still enter promptly near the top",!entering.sideEntry
                && entering.rushSpan>0 && entering.rushEndY<L.playTop+(L.dangerY-L.playTop)*.2f);
        c.paused=true;c.update(.2f,L);
        check("pause holds pending replacements",c.powerReplacements.size()==1);
        c.paused=false;c.startFrenzy(Power.NINJA,L);
        check("a fresh power clears old replacement requests",c.powerReplacements.isEmpty());
        c.powerReplacements.add(true);c.modeLeft=.001f;c.update(DT,L);
        check("power expiry clears replacement requests",c.powerReplacements.isEmpty());
        c.powerReplacements.add(true);c.startGame();
        check("restart clears replacement requests",c.powerReplacements.isEmpty());
        c.powerReplacements.add(true);c.toTitle();
        check("return to title clears replacement requests",c.powerReplacements.isEmpty());
        c.startGame();c.startFrenzy(Power.NINJA,L);c.powerReplacements.add(true);
        c.lives=1;c.takeHit(L.w*.5f,L);
        check("death clears replacement requests",c.powerReplacements.isEmpty());
    }

    private static void rapidClears(Layout L) {
        for (int stage : new int[] {1, 7, 13, 19}) {
            int cleared = 0;
            float longestGap = 0f;
            // A repeatable mass-clear workload, separate from the human-limited Bot playthroughs.
            for (int seed = 1; seed <= 12; seed++) {
                GameCore c = new GameCore(new Mem(), 700L + seed);
                c.startGame();
                c.stage = stage;
                c.enemies.clear();
                c.startFrenzy(Power.NINJA, L);
                c.lives = 99;
                float gap = 0f;
                for (int frame = 0; frame < 12 * 60; frame++) {
                    c.update(DT, L);
                    int visible = 0;
                    for (GameCore.Enemy e : c.enemies) {
                        float half = L.wordWidth(e.word.length) / 2f;
                        float x = c.enemyCentreX(e);
                        if (!e.typeable() || e.y - L.enemyR < L.playTop
                                || x - half < L.playLeft || x + half > L.playRight) continue;
                        visible++;
                        if (frame % 42 == 41) {
                            c.destroyWord(e, x, e.y, L);
                            cleared++;
                        }
                    }
                    if (frame >= 60) {
                        gap = visible == 0 ? gap + DT : 0f;
                        longestGap = Math.max(longestGap, gap);
                    }
                }
            }
            System.out.printf("    stage %d rapid clears: %.1f words/12s, longest gap %.2fs%n",
                    stage, cleared / 12f, longestGap);
            // The first rush prototype fell to 15.6/18.2 late-stage words with 1.15/1.5s gaps.
            float minimum=stage==1?60f:stage==7?45f:stage==13?32f:40f;
            check("stage " + stage + " supplies targets after repeated mass clears", cleared / 12f >= minimum);
            check("stage " + stage + " refills without formation pauses", longestGap < .9f);
        }
    }
}
