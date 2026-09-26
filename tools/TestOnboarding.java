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
        acknowledge(c,L);
        GameCore q=c.onboarding.practice;
        touch(c,L,0,q.keyX(L,key),q.keyY(L,key));
        touch(c,L,1,q.keyX(L,key),q.keyY(L,key));
        c.update(DT,L);
    }
    static void acknowledge(GameCore c,Layout L) {
        if(!c.onboarding.briefing)return;
        touch(c,L,0,L.w*.5f,TutorialSpeech.buttonY(L));
        touch(c,L,1,L.w*.5f,TutorialSpeech.buttonY(L));
    }
    static void finish(GameCore c,Layout L) { for(int i=0;i<120;i++)c.update(DT,L); }
    static void all(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);
        check("fresh run starts stage one without an intro",c.onboarding.practice==null && !c.onboarding.briefing && c.time>0);
        c.onboarding.begin(c,Onboarding.CORE,L);
        float wordY=c.onboarding.word.y;
        for(int i=0;i<1200;i++)c.update(DT,L);
        check("companion explanation freezes practice without a timeout",c.onboarding.briefing
                && c.onboarding.age>19 && c.onboarding.word.y==wordY && c.onboarding.practice.time==0);
        touch(c,L,0,c.onboarding.practice.keyX(L,0),c.onboarding.practice.keyY(L,0));
        touch(c,L,1,c.onboarding.practice.keyX(L,0),c.onboarding.practice.keyY(L,0));
        check("keys cannot dismiss explanation or advance practice",c.onboarding.briefing && c.onboarding.word.pos==0);
        float spawn=c.spawnTimer;long town=c.townRunId;
        int writes=store.saves+store.collectedSaves+store.collectTotalSaves+store.townSaves;
        byte[] progress=store.progress==null?null:store.progress.clone();
        key(c,L,1);
        check("wrong key does not advance practice",c.onboarding.step==0);
        key(c,L,0);
        for(int i=0;i<60 && c.onboarding.step==0;i++)c.update(DT,L);
        check("real key advances to a multi-character word",c.onboarding.step==1);
        check("new word mechanic pauses with its own explanation",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.WORD);
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
                acknowledge(c,L);
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
        briefingInput(L);
        learnedActions(L);
        bossHelp(L);
    }
    private static void encounters(Layout L) {
        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.STARS,Onboarding.CART,Onboarding.MINE}) {
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
        acknowledge(c,L);
        c.onboarding.success=DT;c.update(DT,L);
        check("completed Star Path practice reuses ready lesson once",!c.stars.ready() && c.stars.collected==0 && c.stars.flying());
    }
    private static void skipResetAndHints(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);
        c.onboarding.begin(c,Onboarding.CORE,L);
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
        reset.startGame();reset.update(DT,L);check("reset does not restore removed intro",reset.onboarding.practice==null);
        Mem hintStore=new Mem();hintStore.tutorials=Onboarding.CORE;
        c=new GameCore(hintStore,55);c.startGame();
        add(c,L,new int[]{0},L.playTop+L.enemyR*3);c.update(DT,L);
        float y=c.enemies.get(0).y,clock=c.clock;
        check("word hint pauses at relevant threat",c.onboarding.briefing && c.onboarding.hintKind==Onboarding.WORD_HINT);
        for(int i=0;i<240;i++)c.update(DT,L);
        check("hint freezes actual field and does not auto-dismiss",c.clock==clock && c.enemies.get(0).y==y && c.onboarding.briefing);
        check("unacknowledged hints remain eligible after restart",new GameCore(c.store,4).onboarding.eligible(Onboarding.WORD_HINT));
        acknowledge(c,L);c.update(DT,L);
        check("acknowledging hint resumes play but waits for success",!c.onboarding.briefing && c.clock>clock
                && new GameCore(c.store,4).onboarding.eligible(Onboarding.WORD_HINT));
        c.tapKey(0,L);c.update(DT,L);
        check("successful hinted action persists",!new GameCore(c.store,4).onboarding.eligible(Onboarding.WORD_HINT));
        c.misses=1;c.update(DT,L);check("wrong-key hint pauses contextually",c.onboarding.hintKind==Onboarding.WRONG_HINT);
        acknowledge(c,L);c.enemies.clear();add(c,L,new int[]{0,1},L.playTop+L.enemyR*3);
        c.tapKey(0,L);c.tapKey(1,L);
        c.enemies.clear();add(c,L,new int[]{0},new int[]{2},L.playTop+L.enemyR*3);c.update(DT,L);
        check("stack hint pauses contextually",c.onboarding.hintKind==Onboarding.STACK_HINT);
        acknowledge(c,L);
        c.tapKey(0,L);c.tapKey(0,L);
        check("stack hint completion persists",!new GameCore(c.store,4).onboarding.eligible(Onboarding.STACK_HINT));
    }
    private static void briefingInput(Layout L) {
        GameCore c=fresh(L,new Mem());float x=L.w*.5f,y=TutorialSpeech.buttonY(L);
        c.onboarding.begin(c,Onboarding.CORE,L);
        touch(c,L,0,x,y);Pause.open(c);Pause.resume(c);touch(c,L,1,x,y);
        check("pause cancels pending continue press",c.onboarding.briefing);
        touch(c,L,0,x,y);touch(c,L,2,0,0);touch(c,L,1,x,y);
        check("dragging out cancels continue",c.onboarding.briefing);
        touch(c,L,0,x,y);c.onboarding.touch(c,L,5,99,x,y);touch(c,L,1,x,y);
        check("second finger cancels continue",c.onboarding.briefing);
        acknowledge(c,L);
        check("continue consumes gesture without typing",!c.onboarding.briefing && !c.onboarding.ownsTouch && c.onboarding.word.pos==0);
        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.STARS,Onboarding.CART,Onboarding.MINE,Onboarding.SLIME}) {
            c.onboarding.begin(c,lesson,L);GameCore q=c.onboarding.practice;
            float timer=q.bonusTimer,phase=q.boss.phase,ready=q.cart.ready,mine=q.mining.left;
            for(int i=0;i<300;i++)c.update(DT,L);
            check("new mechanic freezes every gameplay clock "+lesson,c.onboarding.briefing && q.time==0
                    && q.bonusTimer==timer && q.boss.phase==phase && q.cart.ready==ready && q.mining.left==mine);
            if(lesson==Onboarding.STEAMER) {
                acknowledge(c,L);q.bonusTimer=q.bonusRollEnd;
                for(int i=0;i<q.steamer.goal()*2;i++)q.tapBonus(q.steamer.wanted());
                c.update(DT,L);
                check("lifting lid is introduced before drag can proceed",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.LIFT);
            }
        }
        check("speech uses the selected run companion",TutorialSpeech.speaker(c)==c.runWho);
        for(int[] size:new int[][]{{320,568},{360,640},{393,852},{1080,2400},{768,1024}}) {
            Layout p=new Layout();p.compute(size[0],size[1],0,size[1]*.06f,0,size[1]*.04f);
            check("speech and companion stay above controls "+size[0],TutorialSpeech.top(p)>p.topSafe
                    && TutorialSpeech.top(p)+TutorialSpeech.unit(p)*14.2f<p.deckTop);
        }
    }
    private static void learnedActions(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);
        add(c,L,new int[]{0,1},new int[]{2,1},L.playTop);
        c.tapKey(0,L);c.tapKey(0,L);c.tapKey(1,L);
        check("successful normal play retires individual hints",c.onboarding.learned(TutorialSpeech.DANGER)
                && c.onboarding.learned(TutorialSpeech.RETRY) && c.onboarding.learned(TutorialSpeech.STACK));
        GameCore loaded=new GameCore(store,114);
        check("learned actions survive restart without suppressing new mechanics",loaded.onboarding.learned(TutorialSpeech.STACK)
                && !loaded.onboarding.learned(TutorialSpeech.ALTERNATE));
        Interlude.enterBonus(c,L);c.bonusTimer=c.bonusRollEnd;
        c.tapBonus(c.steamer.wanted());c.tapBonus(c.steamer.wanted());c.update(DT,L);
        check("successful alternation before lesson skips it",c.onboarding.practice==null && c.onboarding.learned(TutorialSpeech.ALTERNATE));
        for(int i=0;i<40 && !c.bonusSwipeReady();i++)c.tapBonus(c.steamer.wanted());
        c.update(DT,L);
        check("learned alternation jumps directly to unlearned lid action",c.onboarding.briefing
                && c.onboarding.speech==TutorialSpeech.LIFT && c.onboarding.practice.bonusSwipeReady());
        c.update(.2f,L);float outward=c.onboarding.companionTravel;
        check("companion animates out instead of teleporting",outward>0 && outward<1);
        c.update(.3f,L);acknowledge(c,L);c.update(.1f,L);
        check("companion remains beside unfinished action",c.onboarding.companionTravel==1);
        GameCore q=c.onboarding.practice;q.swipeBonus();c.update(.2f,L);
        check("companion animates back after successful action",c.onboarding.companionTravel<1 && c.onboarding.companionTravel>0);
        c.update(.3f,L);check("companion reaches home",c.onboarding.companionTravel==0);
        c.onboarding.reset(c);
        check("reset makes learned steps eligible again",!c.onboarding.learned(TutorialSpeech.ALTERNATE));
        GameCore stars=fresh(L,new Mem());stars.onboarding.saved=Onboarding.STARS;
        stars.starNext=true;Interlude.enterBonus(stars,L);
        for(int attempt=0;attempt<2;attempt++) {
            stars.stars.collected=0;stars.stars.timer=StarPath.FLY+StarPath.EXIT+StarPath.REPORT;
            if(attempt==1) { stars.stars.hold(0,true);stars.stars.hold(0,false); }
            for(int i=0;i<600 && stars.stars.count()==0;i++) {
                stars.stars.x=stars.stars.starX(0,L);stars.update(DT,L);
            }
            check("star lesson needs a pickup after player steering "+attempt,stars.stars.count()>0
                    && stars.onboarding.learned(TutorialSpeech.STARS)==(attempt==1));
        }
    }
    private static void bossHelp(Layout L) {
        for(int kind=0;kind<Boss.COUNT;kind++) {
            GameCore c=fresh(L,new Mem());Ear ear=new Ear();c.sound=ear;
            c.stage=(kind+1)*5;c.boss.begin(kind,c.stage,c.rnd);c.boss.intro=0;
            c.update(DT,L);
            check("boss offers help without interrupting "+kind,c.onboarding.offersBossHelp(c) && !c.onboarding.briefing);
            float x=TutorialSpeech.helpX(L),y=TutorialSpeech.helpY(L);
            touch(c,L,0,x,y);touch(c,L,2,0,0);touch(c,L,1,x,y);
            check("dragged help press does not open "+kind,!c.onboarding.briefing);
            touch(c,L,0,x,y);touch(c,L,3,x,y);
            check("cancel releases boss help gesture "+kind,!c.onboarding.ownsTouch && c.onboarding.helpPointer<0);
            touch(c,L,0,x,y);touch(c,L,1,x,y);
            float phase=c.boss.phase,time=c.time,hp=c.boss.hp;
            for(int i=0;i<60;i++)c.update(DT,L);
            check("boss help freezes fight and narrates once "+kind,c.onboarding.briefing && c.boss.phase==phase
                    && c.time==time && c.boss.hp==hp && ear.explanations==1 && ear.explanation.length()>10);
            int guard=0;while(c.onboarding.briefing && guard++<5)acknowledge(c,L);
            check("boss explanation resumes same fight "+kind,!c.onboarding.briefing && c.onboarding.practice==null
                    && c.boss.hp==hp && ear.hushes>0);
            c.boss.hp-=1;c.onboarding.bossDamaged(c);c.update(DT,L);
            check("damaged boss retires help "+kind,!c.onboarding.offersBossHelp(c));
        }
    }
}
