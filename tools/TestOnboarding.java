package com.dddumpling.game;

/** Real controls finish lessons; only a completed live Steamer lesson claims a reward. */
final class TestOnboarding extends Check {
    static GameCore fresh(Layout L,Mem store) {
        store.tutorials=store.powerTutorials=0;
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
        reveal(c,L);
        int message=c.onboarding.speech;
        for(int i=0;i<12 && c.onboarding.briefing && c.onboarding.speech==message;i++)advancePage(c,L);
    }
    static void advancePage(GameCore c,Layout L) {
        if(!c.onboarding.briefing)return;
        awaitBubble(c,L);
        touch(c,L,0,L.w*.5f,TutorialSpeech.buttonY(L));
        touch(c,L,1,L.w*.5f,TutorialSpeech.buttonY(L));
    }
    static void reveal(GameCore c,Layout L) {
        if(c.onboarding.sceneWait>0)c.update(c.onboarding.sceneWait,L);
        if(c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.MINIGAMES) {
            for(int i=0;i<12 && c.onboarding.speech==TutorialSpeech.MINIGAMES;i++)advancePage(c,L);
        }
    }
    static void awaitBubble(GameCore c,Layout L) {
        for(int i=0;i<40 && c.onboarding.bubbleProgress()<1;i++)c.update(DT,L);
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

        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.CART,Onboarding.MINE,Onboarding.SLIME}) {
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
        companionHop();
        bossHelpPulse(L);
        speechReveal(L);
        speechPages(L);
        bossHelp(L);
        bossHelpPerRun(L);
        slimeVulnerableHelp(L);
        companionHelp(L);
        steamerTutorialWin(L);
        starTutorialRun(L);
        steamerSelection(L);
        steamerArrival(L);
        collectionGuidance(L);
        diagnostics(L);
        TestPowerTutorials.all(L);
    }
    private static void diagnostics(Layout L) {
        GameCore c=fresh(L,new Mem());
        java.util.ArrayList<String> events=new java.util.ArrayList<>();
        c.diagnostics=events::add;
        c.onboarding.begin(c,Onboarding.CORE,L);
        acknowledge(c,L);key(c,L,0);
        check("diagnostics capture lesson explanation and acknowledgement",!BuildFlags.DEVELOPER
                ?events.isEmpty():events.stream().anyMatch(s->s.startsWith("tutorial-explain") && s.contains("briefing=true"))
                && events.stream().anyMatch(s->s.startsWith("tutorial-acknowledge")));
        check("diagnostics record successful first enemy action",!BuildFlags.DEVELOPER
                ?events.isEmpty():events.stream().anyMatch(s->s.startsWith("tutorial-learn "+TutorialSpeech.MATCH+" ")));
        c.toTitle();
        check("title diagnostic retains pre-exit lesson and caller",!BuildFlags.DEVELOPER
                ?events.isEmpty():events.stream().anyMatch(s->s.startsWith("to-title") && s.contains("practice=true")
                && s.contains("TestOnboarding.diagnostics")));
        c.diagnostics=event->{throw new IllegalStateException("storage unavailable");};
        c.startGame();c.toTitle();
        check("unavailable diagnostics cannot interrupt navigation",c.state==GameCore.TITLE);
    }
    private static void steamerArrival(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;
        Interlude.enterBonus(c,L);c.update(DT,L);GameCore q=c.onboarding.practice;
        float timer=q.bonusTimer;int score=c.score;
        check("Steamer scene appears before any speech",c.onboarding.sceneWait>0 && !c.onboarding.briefing
                && ear.explanations==0);
        c.update(.4f,L);
        check("Steamer fades in without spending selection time",q.time>=.4f && q.bonusTimer==timer
                && c.score==score && !c.onboarding.briefing);
        touch(c,L,0,q.keyX(L,0),q.keyY(L,0));touch(c,L,1,q.keyX(L,0),q.keyY(L,0));
        check("arrival touches cannot start minigame",q.steamer.hits==0 && q.bonusTimer==timer);
        c.update(Onboarding.SCENE_REVEAL,L);
        check("minigame purpose precedes control instructions",c.onboarding.briefing
                && c.onboarding.speech==TutorialSpeech.MINIGAMES && ear.explanation.startsWith("Steamer")
                && ear.explanation.equals(TutorialSpeech.spokenPage(TutorialSpeech.MINIGAMES,0)) && q.bonusTimer==timer);
        reveal(c,L);
        check("overview advances to paused Steamer selection step",c.onboarding.briefing
                && c.onboarding.speech==TutorialSpeech.WAIT && ear.explanation.startsWith("Steamer")
                && new GameCore(store,115).onboarding.learned(TutorialSpeech.MINIGAMES));
        for(int message:new int[]{TutorialSpeech.STARS,TutorialSpeech.LEAN,TutorialSpeech.DIG})
            check("minigame narration names its game "+message,TutorialSpeech.spoken(message).startsWith(
                    message==TutorialSpeech.STARS?"Star Path":message==TutorialSpeech.LEAN?"Cart Rush":"Dumpling Mine"));
    }
    private static void collectionGuidance(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;c.toTitle();
        c.update(2,L);c.update(DT,L);
        check("empty collection does not open title guidance",!c.onboarding.titleGuide);
        c.collected=store.collected=1L<<3;c.time=2;c.update(DT,L);
        check("first collected dumpling introduces display case on title",c.onboarding.titleGuide
                && c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.DISPLAY_CASE);
        acknowledge(c,L);c.openCase();
        for(int i=0;i<90 && !c.onboarding.briefing;i++)c.update(DT,L);
        check("case opening learns action and introduces owned dumpling story",c.onboarding.learned(TutorialSpeech.DISPLAY_CASE)
                && c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.STORIES && c.caseIndex==3);
        acknowledge(c,L);c.openStory();c.update(DT,L);
        check("story opens normally and completes collection sequence",c.storyOpen() && ear.narrations==1
                && !c.onboarding.titleGuide && !c.onboarding.briefing
                && new GameCore(store,116).onboarding.learned(TutorialSpeech.STORIES));
        c.closeStory();c.closeCase();c.update(DT,L);
        check("completed collection sequence does not repeat",!c.onboarding.titleGuide);
        c.onboarding.reset(c);c.update(DT,L);
        check("reset makes collection guidance available without losing dumpling",c.onboarding.titleGuide
                && c.collected==(1L<<3));
        c.onboarding.skip(c);c.update(DT,L);
        check("Skip All suppresses collection sequence",!c.onboarding.titleGuide);
        c=fresh(L,new Mem());c.collected=1;c.toTitle();c.openCase();c.openStory();c.closeStory();c.closeCase();
        c.update(2,L);c.update(DT,L);
        check("discovering case and story early skips their instructions",!c.onboarding.titleGuide
                && c.onboarding.learned(TutorialSpeech.DISPLAY_CASE) && c.onboarding.learned(TutorialSpeech.STORIES));
    }
    private static void steamerSelection(Layout L) {
        GameCore c=fresh(L,new Mem());Ear ear=new Ear();c.sound=ear;
        c.onboarding.learn(c,TutorialSpeech.MINIGAMES);
        c.onboarding.begin(c,Onboarding.STEAMER,L);GameCore q=c.onboarding.practice;
        reveal(c,L);
        check("Steamer first explains waiting for random keys",c.onboarding.briefing
                && c.onboarding.speech==TutorialSpeech.WAIT
                && ear.explanation.equals(TutorialSpeech.spokenPage(TutorialSpeech.WAIT,0)));
        check("Steamer practice connects existing audio without replaying setup",q.sound==ear
                && ear.starts==0 && ear.stageClears==0 && ear.musicCalls==0 && ear.squishes==0);
        c.update(1f,L);
        check("paused selection explanation has no spinner sounds",ear.squishes==0);
        acknowledge(c,L);float timer=q.bonusTimer;
        touch(c,L,0,q.keyX(L,q.steamer.wanted()),q.keyY(L,q.steamer.wanted()));
        touch(c,L,1,q.keyX(L,q.steamer.wanted()),q.keyY(L,q.steamer.wanted()));
        c.update(DT,L);
        check("selection runs while waiting and early taps cannot score",q.bonusTimer<timer
                && q.bonusRolling() && q.steamer.hits==0 && !c.onboarding.briefing);
        for(int i=0;i<300 && !c.onboarding.briefing;i++)c.update(DT,L);
        check("Go cue waits for settled selection and pauses for reading",!q.bonusRolling()
                && c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.ALTERNATE
                && ear.explanations==TutorialSpeech.pageCount(TutorialSpeech.WAIT)+1
                && ear.explanation.equals(TutorialSpeech.spokenPage(TutorialSpeech.ALTERNATE,0)));
        check("tutorial random selection plays ordinary spinner effects",ear.squishes>0);
        timer=q.bonusTimer;c.update(2f,L);
        check("Go explanation does not consume tapping time",q.bonusTimer==timer);
        int squishes=ear.squishes;
        key(c,L,q.steamer.wanted());key(c,L,q.steamer.wanted());
        check("ready keys accept real alternating taps",q.steamer.hits==1);
        check("tutorial alternating taps each play their effect",ear.squishes==squishes+2);
        int wrongs=ear.wrongs;
        key(c,L,q.steamer.rightKey);
        check("tutorial wrong-key feedback uses ordinary audio",ear.wrongs==wrongs+1);
        ear.volumes(.5f,0f);key(c,L,q.steamer.wanted());
        check("tutorial effects retain the player's sound volume",ear.squishVolume==0f && ear.musicVolume==.5f);
        Pause.open(c);squishes=ear.squishes;c.update(1f,L);
        touch(c,L,0,q.keyX(L,q.steamer.wanted()),q.keyY(L,q.steamer.wanted()));
        check("pausing tutorial prevents gameplay effects",ear.squishes==squishes);
        Pause.resume(c);
        GameCore restarted=new GameCore(c.store,114);restarted.startGame();
        check("waiting step stays learned across restart",restarted.onboarding.learned(TutorialSpeech.WAIT));
        GameCore ready=fresh(L,new Mem());ready.onboarding.learn(ready,TutorialSpeech.WAIT);
        Interlude.enterBonus(ready,L);ready.bonusTimer=ready.bonusRollEnd;ready.update(DT,L);
        reveal(ready,L);
        check("returning to a learned selection does not spin a second time",ready.onboarding.briefing
                && ready.onboarding.speech==TutorialSpeech.ALTERNATE && ready.onboarding.practice.bonusMashing());
    }
    private static void steamerTutorialWin(Layout L) {
        Mem store=new Mem();store.steamerOpens=3;
        GameCore c=fresh(L,store);c.lives=1;
        Ear ear=new Ear();c.sound=ear;
        Interlude.enterBonus(c,L);c.update(DT,L);
        GameCore q=c.onboarding.practice;
        for(int i=0;i<600 && !q.bonusSwipeReady();i++) {
            acknowledge(c,L);
            if(q.bonusMashing())key(c,L,q.steamer.wanted());
            c.update(DT,L);
        }
        check("filling tutorial basket alone does not award a win",q.bonusSwipeReady()
                && c.collectTotal==0 && store.steamerOpens==3);
        acknowledge(c,L);
        float y=Screens.steamerLidY(q,L);
        touch(c,L,0,L.w*.5f,y);touch(c,L,2,L.w*.5f,y-L.unit*3);
        c.update(DT,L);
        check("tutorial lid swipe wins the actual Steamer immediately",c.onboarding.practice==null
                && c.bonusEscape() && c.prize>=0 && c.score>=GameCore.FREE_BONUS && c.lives==2);
        check("real tutorial win gets an encouraging spoken celebration",ear.explanation.contains("You did it!"));
        awaitBubble(c,L);
        check("spoken win celebration has a visible matching bubble",c.onboarding.celebration>0
                && c.onboarding.bubbleProgress()==1 && c.onboarding.speech==TutorialSpeech.SUCCESS
                && ear.explanation.equals(TutorialSpeech.spokenPage(TutorialSpeech.SUCCESS,0)));
        check("tutorial win plays the real celebration exactly once",ear.achievements==1);
        check("tutorial win persists real reward and existing difficulty",store.steamerOpens==4
                && store.collectTotal==1 && c.collectTotal==1 && c.starNext
                && !new GameCore(store,114).onboarding.eligible(Onboarding.STEAMER));
        touch(c,L,1,L.w*.5f,y-L.unit*3);
        check("winning tutorial consumes the rest of the swipe",!c.onboarding.ownsTouch);
        int score=c.score,prize=c.prize;
        for(int i=0;i<60;i++) { c.swipeBonus();c.update(DT,L); }
        check("tutorial handoff cannot pay twice",store.steamerOpens==4 && store.collectTotal==1
                && c.score==score && c.prize==prize && c.onboarding.practice==null);
        check("repeated winning gestures do not replay celebration",ear.achievements==1);
        int stage=c.stage;
        for(int i=0;i<1200 && c.stage==stage;i++)c.update(DT,L);
        check("tutorial win goes through celebration to next stage",c.stage==stage+1
                && c.state==GameCore.PLAY && c.onboarding.practice==null);
    }
    private static void starTutorialRun(Layout L) {
        Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;c.lives=2;
        c.starNext=true;Interlude.enterBonus(c,L);
        float timer=c.stars.timer,x=c.stars.x;int score=c.score,stage=c.stage;
        c.update(.6f,L);
        check("Star Path explains the real paused course",c.onboarding.starGuide && c.onboarding.briefing
                && c.onboarding.practice==null && c.stars.timer==timer && c.stars.x==x && ear.explanations==1);
        acknowledge(c,L);c.update(.5f,L);
        check("Star Path acknowledgement resumes course and returns flyer",c.stars.timer<timer
                && !c.onboarding.briefing && c.onboarding.practice==null && c.onboarding.companionTravel==0
                && !c.onboarding.learned(TutorialSpeech.STARS));
        c.stars.beginDrag();boolean continuous=true;int count=0;
        for(int frame=0;frame<1200 && c.state==GameCore.BONUS && !c.stars.won;frame++) {
            int target=0;while(target<c.stars.total() && (c.stars.collected&(1<<target))!=0)target++;
            if(target<c.stars.total())c.stars.dragTo(c.stars.starX(target,L),L);
            c.update(DT,L);
            continuous&=c.stars.count()>=count && c.onboarding.practice==null;
            count=c.stars.count();
        }
        check("Star Path tutorial proceeds continuously to real win",continuous && c.stars.won
                && c.stars.complete() && c.onboarding.learned(TutorialSpeech.STARS));
        // The final star uses the prize fanfare instead of another pickup cue.
        check("Star Path tutorial uses normal pickup and win audio",ear.courseStarts==1
                && ear.stars>0 && ear.lastStar==c.stars.total()-1 && ear.achievements==1);
        check("Star Path tutorial awards real score life prize and one saved win",c.score>=score+GameCore.FREE_BONUS
                && c.lives==3 && c.prize>=Collect.STAR_FIRST && store.starWins==1 && store.starWinSaves==1);
        for(int frame=0;frame<1200 && c.state==GameCore.BONUS;frame++)c.update(DT,L);
        check("Star Path win advances without replay or duplicate award",c.state==GameCore.PLAY && c.stage==stage+1
                && store.starWinSaves==1 && !c.onboarding.starGuide && c.onboarding.practice==null);
        check("Star Path successful steering persists lesson",new GameCore(store,115).onboarding.learned(TutorialSpeech.STARS)
                && (store.tutorials&Onboarding.STARS)!=0);
        c=fresh(L,new Mem());c.starNext=true;Interlude.enterBonus(c,L);c.update(DT,L);acknowledge(c,L);
        c.stars.timer=StarPath.REPORT;c.update(DT,L);c.update(DT,L);
        check("incomplete real course reports without restarting tutorial",!c.onboarding.starGuide
                && !c.onboarding.briefing && c.onboarding.practice==null && !c.onboarding.learned(TutorialSpeech.STARS));
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
        awaitBubble(c,L);
        touch(c,L,0,x,y);Pause.open(c);Pause.resume(c);touch(c,L,1,x,y);
        check("pause cancels pending continue press",c.onboarding.briefing);
        touch(c,L,0,x,y);touch(c,L,2,0,0);touch(c,L,1,x,y);
        check("dragging out cancels continue",c.onboarding.briefing);
        touch(c,L,0,x,y);c.onboarding.touch(c,L,5,99,x,y);touch(c,L,1,x,y);
        check("second finger cancels continue",c.onboarding.briefing);
        touch(c,L,0,x,y);touch(c,L,2,x,y+L.unit*3);touch(c,L,1,x,y);
        check("swiping within the play area does not advance",c.onboarding.briefing);
        touch(c,L,0,c.keyX(L,0),c.keyY(L,0));touch(c,L,1,c.keyX(L,0),c.keyY(L,0));
        check("keyboard taps do not advance the explanation",c.onboarding.briefing);
        acknowledge(c,L);
        check("continue consumes gesture without typing",!c.onboarding.briefing && !c.onboarding.ownsTouch && c.onboarding.word.pos==0);
        for(float[] point:new float[][]{{L.playLeft+L.unit,L.playTop+L.unit},
                {L.playRight-L.unit,L.deckTop-L.unit},{L.w*.5f,(L.playTop+L.deckTop)*.5f}}) {
            c.onboarding.begin(c,Onboarding.CORE,L);awaitBubble(c,L);
            touch(c,L,0,point[0],point[1]);touch(c,L,1,point[0],point[1]);
            check("tap anywhere in play area acknowledges without learning",!c.onboarding.briefing
                    && !c.onboarding.ownsTouch && c.onboarding.word.pos==0 && !c.onboarding.learned(TutorialSpeech.MATCH));
        }
        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.CART,Onboarding.MINE,Onboarding.SLIME}) {
            c.onboarding.begin(c,lesson,L);GameCore q=c.onboarding.practice;
            reveal(c,L);float presentationTime=q.time;
            float timer=q.bonusTimer,phase=q.boss.phase,ready=q.cart.ready,mine=q.mining.left;
            for(int i=0;i<300;i++)c.update(DT,L);
            check("new mechanic freezes every gameplay clock "+lesson,c.onboarding.briefing && q.time==presentationTime
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
    private static void speechPages(Layout L) {
        for(int message=TutorialSpeech.MATCH;message<=TutorialSpeech.STORIES;message++) {
            StringBuilder displayed=new StringBuilder();boolean fits=true;
            for(int page=0;page<TutorialSpeech.pageCount(message);page++) {
                String[] lines=TutorialSpeech.pageLines(message,page);
                fits&=lines.length>0 && lines.length<=3;
                for(String line:lines)fits&=!line.isEmpty() && line.length()<=22;
                String spoken=TutorialSpeech.spokenPage(message,page);
                check("page ends on a complete sentence "+message+"/"+page,spoken.matches(".*[.!?]$"));
                check("page narration equals displayed words "+message+"/"+page,spoken.equals(String.join(" ",lines)));
                if(displayed.length()>0)displayed.append(' ');
                displayed.append(spoken);
            }
            check("every spoken word appears in readable bubbles "+message,fits && displayed.toString().equals(TutorialSpeech.spoken(message)));
        }
        GameCore c=TestPowerTutorials.fresh(L,new Mem());Ear ear=new Ear();c.sound=ear;
        c.startFrenzy(Power.TEAM,L);c.update(DT,L);
        int message=TutorialSpeech.POWER_TEAM,pages=TutorialSpeech.pageCount(message);
        float left=c.modeLeft,clock=c.clock;
        check("long explanation uses multiple bubbles",pages>1);
        for(int page=0;page<pages;page++) {
            check("new page speaks only its own displayed text "+page,c.onboarding.speechPage==page
                    && ear.explanation.equals(TutorialSpeech.spokenPage(message,page)));
            advancePage(c,L);
            check("reading pages does not run or learn mechanic "+page,c.modeLeft==left && c.clock==clock
                    && !c.onboarding.learned(message) && c.onboarding.briefing==(page<pages-1));
            if(page<pages-1)check("next page animates from stationary companion",c.onboarding.companionTravel==1
                    && c.onboarding.bubbleProgress()==0);
        }
        check("one narration per page",ear.explanations==pages);
        c.onboarding.clear();check("clear resets page state",c.onboarding.speechPage==0 && c.onboarding.celebration==0);
    }
    private static void speechReveal(Layout L) {
        GameCore c=fresh(L,new Mem());c.onboarding.begin(c,Onboarding.CORE,L);
        Onboarding o=c.onboarding;float x=L.w*.5f,y=TutorialSpeech.buttonY(L),wordY=o.word.y;
        check("speech starts hidden at home",o.bubbleProgress()==0);
        c.update(.2f,L);
        touch(c,L,0,x,y);touch(c,L,1,x,y);
        check("travel hides speech and consumes invisible continue taps",o.bubbleProgress()==0 && o.briefing && !o.ownsTouch);
        c.update(.25f,L);
        check("bubble waits until companion lands",o.companionTravel==1 && o.bubbleProgress()<.001f);
        c.update(Onboarding.BUBBLE_OPEN*.5f,L);
        check("speech expands quickly after landing",o.bubbleProgress()>.49f && o.bubbleProgress()<.51f);
        touch(c,L,0,x,y);c.update(Onboarding.BUBBLE_OPEN,L);touch(c,L,1,x,y);
        check("press started during reveal cannot acknowledge",o.briefing && o.bubbleProgress()==1);
        check("arrival and bubble animation keep game frozen",o.word.y==wordY && o.practice.time==0);
        touch(c,L,0,x,y);touch(c,L,1,x,y);
        check("visible continue works and reminder stays open",!o.briefing && o.bubbleProgress()==1);
        o.begin(c,Onboarding.CART,L);
        check("new explanation restarts bubble even when companion is already there",o.briefing && o.bubbleProgress()==0);
        c.update(Onboarding.BUBBLE_OPEN,L);
        check("stationary companion opens new bubble without another travel delay",o.bubbleProgress()==1);
        o.clear();
        check("clearing guidance clears bubble animation",o.bubbleProgress()==0);
    }
    private static void companionHop() {
        check("hop holds horizontal position through the in-place bounce",TutorialSpeech.hopProgress(0)==0
                && TutorialSpeech.hopProgress(.1f)==0 && TutorialSpeech.hopProgress(.2f)==0);
        check("hop reaches the exact help position",TutorialSpeech.hopProgress(1)==1);
        for(int[] size:new int[][]{{320,568},{360,640},{393,852},{1080,2400},{768,1024}}) {
            Layout L=new Layout();L.compute(size[0],size[1],0,size[1]*.06f,0,size[1]*.04f);
            float s=TutorialSpeech.unit(L),home=RunCompanion.y(L),target=L.topSafe+s*4.9f;
            check("hop endpoints have no lift "+size[0],TutorialSpeech.hopLift(0,home,target,s)==0
                    && TutorialSpeech.hopLift(.2f,home,target,s)==0 && TutorialSpeech.hopLift(1,home,target,s)==0);
            check("companion bounces up before leaving home "+size[0],TutorialSpeech.hopLift(.1f,home,target,s)>s*.8f);
            check("flight curves above the straight path "+size[0],TutorialSpeech.hopLift(.6f,home,target,s)>s*2);
            boolean visible=true,landsDownward=false;
            for(int i=0;i<=100;i++) {
                float t=i/100f,u=TutorialSpeech.hopProgress(t);
                float y=home+(target-home)*u-TutorialSpeech.hopLift(t,home,target,s);
                visible&=y-s*1.6f>=L.topSafe && y+s*1.6f<L.h;
                if(t>.8f && y<target)landsDownward=true;
            }
            check("hop stays onscreen and descends into help position "+size[0],visible && landsDownward);
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
        c.onboarding.learn(c,TutorialSpeech.MINIGAMES);
        Interlude.enterBonus(c,L);c.bonusTimer=c.bonusRollEnd;
        c.tapBonus(c.steamer.wanted());c.tapBonus(c.steamer.wanted());c.update(DT,L);
        check("successful alternation before lesson skips it",c.onboarding.practice==null && c.onboarding.learned(TutorialSpeech.ALTERNATE));
        for(int i=0;i<40 && !c.bonusSwipeReady();i++)c.tapBonus(c.steamer.wanted());
        c.update(DT,L);
        reveal(c,L);
        check("learned alternation jumps directly to unlearned lid action",c.onboarding.briefing
                && c.onboarding.speech==TutorialSpeech.LIFT && c.onboarding.practice.bonusSwipeReady());
        c.update(.2f,L);float outward=c.onboarding.companionTravel;
        check("companion animates out instead of teleporting",outward>0 && outward<1);
        c.update(.3f,L);acknowledge(c,L);c.update(.1f,L);
        check("companion remains beside unfinished action",c.onboarding.companionTravel==1);
        GameCore q=c.onboarding.practice;q.swipeBonus();c.update(.2f,L);
        check("companion stays beside displayed win celebration",c.onboarding.companionTravel==1 && c.onboarding.celebration>0);
        c.update(3.9f,L);c.update(.2f,L);
        check("companion animates back after win celebration",c.onboarding.companionTravel<1 && c.onboarding.companionTravel>0);
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
    private static void bossHelpPerRun(Layout L) {
        for(int kind=0;kind<Boss.COUNT;kind++)for(int used=0;used<2;used++) {
            Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;
            for(int message:new int[]{TutorialSpeech.CLOSED,TutorialSpeech.CHAIN,TutorialSpeech.GLOB,
                    TutorialSpeech.PINCH,TutorialSpeech.DEFEND,TutorialSpeech.TEAR,TutorialSpeech.SHAKE})
                c.onboarding.learn(c,message);
            c.stage=(kind+1)*5;c.boss.begin(kind,c.stage,c.rnd);c.update(DT,L);
            check("learned boss offers help during stage arrival "+kind+"/"+used,c.boss.intro>0
                    && c.onboarding.offersBossHelp(c));
            float x=TutorialSpeech.helpX(L),y=TutorialSpeech.helpY(L);
            touch(c,L,0,x,y);touch(c,L,3,x,y);
            check("cancelled boss help stays available "+kind+"/"+used,c.onboarding.offersBossHelp(c));
            if(used==0) {
                touch(c,L,0,x,y);touch(c,L,1,x,y);
                float intro=c.boss.intro;c.update(.5f,L);
                check("opening help pauses boss arrival "+kind,c.onboarding.briefing && c.boss.intro==intro);
                int guard=0;while(c.onboarding.briefing && guard++<5)acknowledge(c,L);
                check("requested help repeats every learned introductory page "+kind,
                        ear.explanations==(kind==Boss.SLIME?TutorialSpeech.pageCount(TutorialSpeech.CLOSED)+TutorialSpeech.pageCount(TutorialSpeech.CHAIN):
                                kind==Boss.OCTOPUS?TutorialSpeech.pageCount(TutorialSpeech.DEFEND)+TutorialSpeech.pageCount(TutorialSpeech.TEAR):
                                TutorialSpeech.pageCount(kind==Boss.SPLITTER?TutorialSpeech.PINCH:TutorialSpeech.SHAKE)));
            } else {
                touch(c,L,0,x,y);c.boss.hp--;c.onboarding.bossDamaged(c);touch(c,L,1,x,y);
                check("damage cancels pending help activation "+kind,!c.onboarding.briefing);
            }
            c.onboarding.clear();c.onboarding.companionTravel=0;
            c.boss.begin(kind,c.stage,c.rnd);
            check("used or damaged boss stays dismissed for run "+kind+"/"+used,!c.onboarding.offersBossHelp(c));
            int other=(kind+1)%Boss.COUNT;c.boss.begin(other,(other+1)*5,c.rnd);
            check("one boss does not dismiss another boss help "+kind+"/"+used,c.onboarding.offersBossHelp(c));
            c.startGame();c.stage=(kind+1)*5;c.boss.begin(kind,c.stage,c.rnd);
            check("new run restores boss help "+kind+"/"+used,c.onboarding.offersBossHelp(c));
            GameCore loaded=new GameCore(store,115);loaded.startGame();loaded.stage=(kind+1)*5;
            loaded.boss.begin(kind,loaded.stage,loaded.rnd);
            check("saved learning does not suppress new launch boss help "+kind+"/"+used,loaded.onboarding.offersBossHelp(loaded));
            loaded.onboarding.skip(loaded);
            check("Skip All still suppresses optional boss help "+kind+"/"+used,!loaded.onboarding.offersBossHelp(loaded));
        }
    }
    private static void bossHelpPulse(Layout L) {
        float small=TutorialSpeech.helpScale(1.125f),large=TutorialSpeech.helpScale(.375f);
        check("boss help gently grows and shrinks",small<1 && large>1 && large<1.35f);
        check("boss help cycles its opaque border color",TutorialSpeech.helpBorder(.375f)!=TutorialSpeech.helpBorder(1.125f)
                && (TutorialSpeech.helpBorder(.375f)>>>24)==255);
        check("boss help animation loops deterministically",Math.abs(TutorialSpeech.helpScale(.375f)-TutorialSpeech.helpScale(1.875f))<.0001f);
        check("largest boss bubble fits its stationary tap target",TutorialSpeech.helpHit(L,
                TutorialSpeech.helpX(L)+L.unit*large,TutorialSpeech.helpY(L)+L.unit*large));
    }
    private static void companionHelp(Layout L) {
        GameCore c=fresh(L,new Mem());Ear ear=new Ear();c.sound=ear;
        c.stage=5;c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;c.update(DT,L);
        float x=RunCompanion.x(L),y=RunCompanion.y(L);
        check("companion is a help target when question is offered",c.onboarding.wantsTouch(c,L,0,x,y));
        touch(c,L,0,x,y);touch(c,L,2,0,0);touch(c,L,1,x,y);
        check("leaving companion cancels help press",!c.onboarding.briefing);
        touch(c,L,0,x,y);c.onboarding.touch(c,L,5,9,x,y);touch(c,L,1,x,y);
        check("second finger cancels companion help press",!c.onboarding.briefing);
        touch(c,L,0,x,y);touch(c,L,2,x+L.unit*.1f,y);touch(c,L,1,x,y);
        check("small motion inside companion still opens narrated help",c.onboarding.briefing && ear.explanations==1);
        float phase=c.boss.phase;c.update(.5f,L);
        check("companion help freezes same fight and moves companion",c.boss.phase==phase && c.onboarding.companionTravel>0);
        c.onboarding.clear();c.onboarding.companionTravel=0;c.boss.hp--;
        check("companion no longer opens help when question is unavailable",!c.onboarding.wantsTouch(c,L,0,x,y)
                && c.tapCompanion(x,y,L) && !c.onboarding.briefing);
    }
    static int makeSlimeVulnerable(GameCore c,Layout L) {
        for(int i=0;i<Boss.SPLIT_HITS;i++) {
            c.boss.phase=1.35f;c.tapKey(c.boss.chainLetter(),L);
        }
        for(int i=0;i<Boss.ELEMS;i++)if(c.boss.etype[i]==Boss.E_GLOB)return i;
        throw new AssertionError("Slime did not become vulnerable");
    }
    private static void slimeVulnerableHelp(Layout L) {
        for(int help=0;help<2;help++) {
            Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;
            c.stage=5;c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;c.update(DT,L);
            if(help==1) {
                touch(c,L,0,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
                touch(c,L,1,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
                acknowledge(c,L);acknowledge(c,L);
                check("Slime initial help ends before red-area step",!c.onboarding.briefing
                        && (c.onboarding.introduced&(1<<TutorialSpeech.GLOB))==0);
            }
            int g=makeSlimeVulnerable(c,L);int spoken=ear.explanations;
            c.update(.5f,L);
            check("Slime remains playable during vulnerability grace period "+help,!c.onboarding.briefing
                    && ear.explanations==spoken && c.boss.elife[g]<Boss.GLOB_TIME);
            float waiting=c.onboarding.slimeHintAge;Pause.open(c);c.update(2f,L);Pause.resume(c);
            check("pause does not spend Slime tutorial delay "+help,c.onboarding.slimeHintAge==waiting);
            c.update(.99f,L);
            check("Slime drag lesson waits the full 1.5 seconds "+help,!c.onboarding.briefing);
            float life=c.boss.elife[g],hp=c.boss.hp,time=c.time;c.update(.02f,L);
            check("vulnerable Slime introduces separate paused drag step "+help,c.onboarding.briefing
                    && c.onboarding.bossGuide && c.onboarding.speech==TutorialSpeech.GLOB
                    && c.onboarding.practice==null && ear.explanations==spoken+1
                    && c.boss.elife[g]==life && c.boss.hp==hp && c.time==time);
            acknowledge(c,L);c.update(DT,L);
            check("red-area acknowledgement resumes without relearning or reopening "+help,!c.onboarding.briefing
                    && !c.onboarding.learned(TutorialSpeech.GLOB) && c.boss.elife[g]<life);
            c.grabBoss(c.boss.ex[g],c.boss.ey[g]);
            c.dragBoss(L.w*.5f,c.boss.ey[g],L);c.dragBoss(-L.w*.2f,c.boss.ey[g],L);c.update(DT,L);
            check("real red-area drag learns step and returns companion "+help,c.boss.hp<hp
                    && !c.onboarding.bossGuide && new GameCore(store,114).onboarding.learned(TutorialSpeech.GLOB));
            c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;
            makeSlimeVulnerable(c,L);c.update(DT,L);
            check("learned red-area step stays skipped "+help,!c.onboarding.briefing);
        }
        GameCore c=fresh(L,new Mem());c.stage=5;c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;c.update(DT,L);
        c.onboarding.skip(c);makeSlimeVulnerable(c,L);c.update(DT,L);
        check("skip all suppresses contextual Slime drag step",!c.onboarding.briefing);
        c.onboarding.reset(c);c.update(Onboarding.SLIME_HINT_DELAY+.01f,L);acknowledge(c,L);
        for(int i=0;i<Boss.ELEMS;i++)if(c.boss.etype[i]==Boss.E_GLOB)c.boss.elife[i]=.001f;
        c.update(DT,L);c.update(DT,L);
        check("expired red area clears reminder without learning",!c.onboarding.bossGuide
                && !c.onboarding.learned(TutorialSpeech.GLOB));
        c.onboarding.reset(c);c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;c.boss.update(0,L,c.rnd);
        int g=makeSlimeVulnerable(c,L);c.update(.75f,L);c.grabBoss(c.boss.ex[g],c.boss.ey[g]);
        c.dragBoss(L.w*.5f,c.boss.ey[g],L);c.dragBoss(-L.w*.2f,c.boss.ey[g],L);c.update(DT,L);
        c.update(2f,L);
        check("successful drag before prompt learns without interrupting",c.onboarding.learned(TutorialSpeech.GLOB)
                && !c.onboarding.briefing && c.onboarding.slimeHintAge==0
                && new GameCore(c.store,114).onboarding.learned(TutorialSpeech.GLOB));
        c.onboarding.reset(c);c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;
        g=makeSlimeVulnerable(c,L);c.update(.8f,L);c.boss.elife[g]=.001f;c.update(DT,L);c.update(DT,L);
        check("expired vulnerability cancels pending tutorial delay",c.onboarding.slimeHintAge==0 && !c.onboarding.briefing);
        c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;makeSlimeVulnerable(c,L);c.update(.8f,L);
        check("new vulnerability receives a fresh delay",!c.onboarding.briefing && c.onboarding.slimeHintAge<1);
        c.toTitle();
        check("leaving run clears pending Slime delay",c.onboarding.slimeHintAge==0);
    }
}
