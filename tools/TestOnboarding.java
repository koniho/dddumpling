package com.dddumpling.game;

/** Real controls must finish lessons without advancing the waiting run or writing rewards. */
final class TestOnboarding extends Check {
    static GameCore fresh(Layout L,Mem store) {
        store.tutorials=0;
        GameCore c=new GameCore(store,114);c.startGame();c.update(DT,L);return c;
    }
    static void touch(GameCore c,Layout L,int action,float x,float y) {
        c.onboarding.touch(c,L,action,7,x,y);
    }
    static void key(GameCore c,Layout L,int key) {
        GameCore q=c.onboarding.practice;
        touch(c,L,0,q.keyX(L,key),q.keyY(L,key));
        touch(c,L,1,q.keyX(L,key),q.keyY(L,key));
        c.update(DT,L);
    }
    static void finish(GameCore c,Layout L) { for(int i=0;i<120;i++)c.update(DT,L); }
    static void all(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);
        check("fresh run offers core practice",c.onboarding.lesson==Onboarding.CORE);
        for(int i=0;i<1200;i++)c.update(DT,L);
        check("incoming practice word waits safely for interaction",c.onboarding.step==0
                && c.onboarding.word.pos==0 && c.onboarding.word.y<L.dangerY && c.onboarding.practice.lives==GameCore.START_LIVES);
        float spawn=c.spawnTimer;long town=c.townRunId;
        int writes=store.saves+store.collectedSaves+store.collectTotalSaves+store.townSaves;
        byte[] progress=store.progress==null?null:store.progress.clone();
        key(c,L,1);
        check("wrong key does not advance practice",c.onboarding.step==0);
        key(c,L,0);
        for(int i=0;i<60 && c.onboarding.step==0;i++)c.update(DT,L);
        check("real key advances to a multi-character word",c.onboarding.step==1);
        key(c,L,1);key(c,L,4);key(c,L,0);finish(c,L);
        check("core completion persists independently",(store.tutorials&Onboarding.CORE)!=0 && c.onboarding.eligible(Onboarding.STEAMER));
        check("practice awards no score, lives, progress or collection",c.score==0 && c.hits==0 && c.lives==GameCore.START_LIVES
                && town==c.townRunId && writes==store.saves+store.collectedSaves+store.collectTotalSaves+store.townSaves
                && java.util.Arrays.equals(progress,store.progress));
        check("normal play resumes",c.onboarding.practice==null && c.spawnTimer<spawn);
        GameCore restart=new GameCore(store,115);restart.startGame();restart.update(DT,L);
        check("completed intro stays complete after restart",restart.onboarding.practice==null);

        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.STARS,Onboarding.CART,Onboarding.MINE,Onboarding.SLIME}) {
            c.onboarding.begin(c,lesson,L);
            GameCore q=c.onboarding.practice;
            int score=c.score,cart=store.cartTrack,mine=store.mineCarts;
            float hp=c.boss.hp,clock=c.clock;
            if(lesson==Onboarding.SLIME) {
                int at=q.boss.chainAt;
                key(c,L,q.boss.chainLetter());
                check("closed Slime rejects chain input",q.boss.chainAt==at && !q.boss.open());
            }
            for(int i=0;i<60*35 && c.onboarding.success==0;i++) {
                if(lesson==Onboarding.STEAMER) {
                    if(q.bonusSwipeReady()) {
                        float y=Screens.steamerLidY(q,L);
                        touch(c,L,0,L.w*.5f,y);touch(c,L,2,L.w*.5f,y-L.unit*3);touch(c,L,1,L.w*.5f,y-L.unit*3);
                    } else if(q.bonusMashing())key(c,L,q.steamer.wanted());
                } else if(lesson==Onboarding.MINE) {
                    if(q.mining.digging())key(c,L,q.mining.sequence[q.mining.pos]);
                    if(q.mining.swipeReady()) {
                        float x=q.mining.cartX*L.w,y=CaveMiningScreen.cartY(L);
                        touch(c,L,0,x,y);touch(c,L,2,x+L.w*.25f,y);touch(c,L,1,x+L.w*.25f,y);
                    }
                } else if(lesson==Onboarding.CART) {
                    float x=StarScreen.sliderLeft(L)+(q.cart.turn+1)*.5f*(StarScreen.sliderRight(L)-StarScreen.sliderLeft(L));
                    if(q.cart.input.pointer<0)touch(c,L,0,x,StarScreen.sliderY(L));
                    else touch(c,L,2,x,StarScreen.sliderY(L));
                } else if(lesson==Onboarding.STARS) {
                    float x=q.stars.starX(0,L);
                    if(c.onboarding.pointer<0)touch(c,L,0,x,StarScreen.sliderY(L));
                    else touch(c,L,2,x,StarScreen.sliderY(L));
                } else if(q.boss.hasGlob()) {
                    for(int g=0;g<Boss.ELEMS;g++)if(q.boss.etype[g]==Boss.E_GLOB) {
                        float y=q.boss.ey[g];
                        touch(c,L,0,q.boss.ex[g],y);
                        touch(c,L,2,L.w*.5f,y);touch(c,L,2,-L.w*.2f,y);touch(c,L,1,-L.w*.2f,y);break;
                    }
                } else if(q.boss.open())key(c,L,q.boss.chainLetter());
                c.update(DT,L);
            }
            check("real interaction completes lesson "+lesson,c.onboarding.success>0);
            check("practice freezes waiting run and durable progress "+lesson,c.score==score && c.boss.hp==hp
                    && c.clock==clock && store.cartTrack==cart && store.mineCarts==mine && store.steamerOpens==0 && store.starWins==0);
            finish(c,L);
            check("encounter completion survives reload "+lesson,!new GameCore(store,4).onboarding.eligible(lesson));
        }
        skipResetAndHints(L);
        encounters(L);
    }
    private static void encounters(Layout L) {
        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.STARS,Onboarding.CART,Onboarding.MINE,Onboarding.SLIME}) {
            Mem store=new Mem();store.tutorials=Onboarding.CORE;
            GameCore c=new GameCore(store,82);c.startGame();
            if(lesson==Onboarding.SLIME) {
                c.stage=5;c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;
            } else if(lesson==Onboarding.CART || lesson==Onboarding.MINE) {
                c.state=GameCore.BONUS;c.bonusTimer=1;
                if(lesson==Onboarding.CART)c.cart.begin(c);else c.mining.begin(c);
            } else { c.starNext=lesson==Onboarding.STARS;Interlude.enterBonus(c,L); }
            float timer=c.bonusTimer,hp=c.boss.hp;
            int expectedRandom=new java.util.Random(95).nextInt();
            c.rnd.setSeed(95);c.update(DT,L);
            check("first encounter starts correct practice "+lesson,c.onboarding.lesson==lesson);
            check("practice does not consume waiting RNG "+lesson,c.rnd.nextInt()==expectedRandom);
            c.onboarding.skip(c);c.update(0,L);
            check("skip preserves actual encounter "+lesson,c.bonusTimer==timer && c.boss.hp==hp
                    && c.onboarding.practice==null && c.score==0);
            c.update(DT,L);check("global Skip prevents encounter restart "+lesson,c.onboarding.practice==null);
        }
        Mem store=new Mem();store.tutorials=Onboarding.CORE;
        GameCore c=new GameCore(store,5);c.startGame();c.starNext=true;Interlude.enterBonus(c,L);c.update(DT,L);
        c.onboarding.success=DT;c.update(DT,L);
        check("completed Star Path practice reuses ready lesson once",!c.stars.ready() && c.stars.collected==0 && c.stars.flying());
    }
    private static void skipResetAndHints(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);
        GameCore q=c.onboarding.practice;
        Pause.open(c);float age=c.onboarding.age;c.update(20,L);
        check("pause freezes practice",age==c.onboarding.age);
        Pause.resume(c);
        touch(c,L,0,L.w*.84f,Onboarding.skipY(L));
        touch(c,L,1,L.w*.84f,Onboarding.skipY(L));
        check("skip consumes gesture and returns to normal play",c.onboarding.practice==null && !c.onboarding.ownsTouch && q!=null);
        GameCore loaded=new GameCore(store,4);loaded.startGame();loaded.update(DT,L);
        check("global skip survives restart and suppresses every encounter",loaded.onboarding.practice==null
                && !loaded.onboarding.eligible(Onboarding.MINE) && !loaded.onboarding.eligible(Onboarding.SLIME));
        loaded.lives=1;loaded.pushLesson.seen=false;
        add(loaded,L,new int[]{0},PushLesson.triggerY(L));loaded.update(DT,L);
        check("global skip suppresses rescue-swipe lesson",!loaded.pushLesson.active);
        store.best=721;store.collected=4;store.steamerOpens=3;store.starWins=4;store.cartTrack=8;store.mineCarts=2;
        loaded.preferences.kids=true;
        SettingsInput.action(loaded,L,1000+PlayerSettings.TUTORIALS);
        GameCore reset=new GameCore(store,9);
        check("reset clears all onboarding and swipe flags",store.tutorials==0 && !store.pushLessonSeen
                && reset.onboarding.eligible(Onboarding.CORE) && reset.onboarding.eligible(Onboarding.MINE));
        check("reset leaves non-tutorial progress and preferences alone",store.best==721 && store.collected==4
                && store.steamerOpens==3 && store.starWins==4 && store.cartTrack==8 && store.mineCarts==2 && loaded.preferences.kids);
        reset.startGame();reset.update(DT,L);check("reset restores intro next run",reset.onboarding.lesson==Onboarding.CORE);
        c=new GameCore(new Mem(),55);c.startGame();c.onboarding.saved=Onboarding.CORE;
        add(c,L,new int[]{0},L.playTop+L.enemyR*3);c.update(DT,L);
        check("word hint appears only at relevant threat",c.onboarding.hintLeft>0 && (c.onboarding.saved&Onboarding.WORD_HINT)!=0);
        for(int i=0;i<240;i++)c.update(DT,L);
        check("hint expires without repeating",c.onboarding.hintLeft==0);
        c.misses=1;c.update(DT,L);check("wrong-key hint appears contextually",(c.onboarding.saved&Onboarding.WRONG_HINT)!=0);
        c.onboarding.hintLeft=0;c.enemies.clear();add(c,L,new int[]{0},new int[]{2},L.playTop+L.enemyR*3);c.update(DT,L);
        check("stack hint appears contextually",(c.onboarding.saved&Onboarding.STACK_HINT)!=0);
    }
}
