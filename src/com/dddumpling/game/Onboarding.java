package com.dddumpling.game;

/** Guided practice freezes the waiting run; a completed Steamer claims its real win. */
final class Onboarding extends Draw {
    static final int CORE=1, STEAMER=2, STARS=4, CART=8, MINE=16, SLIME=32,
            SKIPPED=64, WORD_HINT=128, WRONG_HINT=256, STACK_HINT=512;
    int saved, lesson, step, pointer=-1, speech, introduced, hintKind, speechPointer=-1;
    boolean bossHelp, bossGuide, helpArmed;
    int helpPointer=-1;
    GameCore practice, teacher, narrator;
    GameCore.Enemy word;
    boolean ownsTouch, lid, starDrag, briefing, continueArmed;
    final int[] starPointers=new int[Glyph.COUNT];
    float age, success, startY, offsetX, globAge, companionTravel;

    // Preserve the original ten lesson bits; successful actions have independent durable bits.
    boolean learned(int message) { return !eligible(1<<(message+9)); }
    void learn(GameCore c,int message) {
        if(teacher!=null) { teacher.onboarding.learn(teacher,message);return; }
        if(learned(message))return;
        saved|=1<<(message+9);
        if(message==TutorialSpeech.DANGER)saved|=WORD_HINT;
        if(message==TutorialSpeech.RETRY)saved|=WRONG_HINT;
        if(message==TutorialSpeech.STACK)saved|=STACK_HINT;
        save(c);
        if(message==speech)hintKind=0;
    }
    void bossDamaged(GameCore c) {
        if(c.boss.kind==Boss.SLIME)learn(c,TutorialSpeech.GLOB);
        else for(int message:bossSteps(c.boss.kind))learn(c,message);
    }
    private static int[] bossSteps(int kind) {
        if(kind==Boss.SLIME)return new int[]{TutorialSpeech.CLOSED,TutorialSpeech.CHAIN,TutorialSpeech.GLOB};
        if(kind==Boss.SPLITTER)return new int[]{TutorialSpeech.PINCH};
        if(kind==Boss.OCTOPUS)return new int[]{TutorialSpeech.DEFEND,TutorialSpeech.TEAR};
        return new int[]{TutorialSpeech.SHAKE};
    }
    boolean offersBossHelp(GameCore c) {
        if(c.state!=GameCore.PLAY || !c.boss.fighting() || c.boss.hp<c.boss.hpMax
                || practice!=null || briefing || hintKind!=0 || bossHelp || bossGuide
                || companionTravel>0 || !eligible(SKIPPED))return false;
        for(int message:bossSteps(c.boss.kind))if(!learned(message))return true;
        return false;
    }
    private boolean nextBossHelp(GameCore c) {
        for(int message:bossSteps(c.boss.kind))if(!learned(message) && (introduced&(1<<message))==0) {
            speech=message;introduced|=1<<message;briefing=true;age=0;
            Pause.release(c);narrate(c);return true;
        }
        return false;
    }
    boolean moreBossHelp(GameCore c) {
        if(!bossHelp)return false;
        for(int message:bossSteps(c.boss.kind))if(!learned(message) && (introduced&(1<<message))==0)return true;
        return false;
    }
    private void narrate(GameCore c) {
        narrator=c;
        if(c.sound!=null)c.sound.explain(TutorialSpeech.spoken(speech));
    }
    boolean promptHit(GameCore c,Layout L,float x,float y) {
        return offersBossHelp(c) && TutorialSpeech.helpHit(L,x,y);
    }
    boolean wantsTouch(GameCore c,Layout L,int action,float x,float y) {
        return practice!=null || briefing || ownsTouch || action==0 && (promptHit(c,L,x,y)
                || (hintKind!=0 || bossGuide || c.pushLesson.active) && skipHit(L,x,y));
    }
    boolean speaking(GameCore c) {
        return briefing || (practice!=null || hintKind!=0 || bossGuide) && success<=0 && !learned(speech)
                || c.pushLesson.active;
    }
    boolean movingCompanion(GameCore c) { return companionTravel>0 || speaking(c); }
    boolean companionAway(GameCore c) {
        return teacher==null?movingCompanion(c):teacher.onboarding.movingCompanion(teacher);
    }
    private void moveCompanion(GameCore c,float dt) {
        float target=speaking(c)?1:0;
        companionTravel+=Math.max(-dt/.45f,Math.min(dt/.45f,target-companionTravel));
    }

    boolean eligible(int bit) { return (saved & (bit|SKIPPED))==0; }
    void save(GameCore c) { if(c.store!=null)c.store.saveTutorials(saved); }
    void reset(GameCore c) {
        clear();saved=0;companionTravel=0;
        c.pushLesson.seen=false;c.pushLesson.reset();
        if(c.store!=null)c.store.savePushLessonSeen(false);
        save(c);
    }
    void clear() {
        if(narrator!=null && narrator.sound!=null)narrator.sound.hush();
        narrator=null;
        cancelTouch();practice=null;word=null;lesson=step=speech=introduced=hintKind=0;
        success=0;briefing=bossHelp=bossGuide=false;
    }
    void cancelTouch() {
        pointer=-1;ownsTouch=lid=starDrag=false;
        speechPointer=-1;continueArmed=false;
        helpPointer=-1;helpArmed=false;
        java.util.Arrays.fill(starPointers,-1);
        if(practice!=null)Pause.release(practice);
    }
    void skip(GameCore c) {
        if(c.sound!=null)c.sound.hush();
        clear();saved|=SKIPPED;
        c.pushLesson.reset();save(c);
    }
    void begin(GameCore c,int which,Layout L) {
        Pause.release(c);clear();lesson=which;age=globAge=0;
        GameCore q=practice=new GameCore(null,114L+which);
        q.startGame();q.runFullRoster=c.runFullRoster;q.runWho=c.runWho;
        q.stageBanner=0;q.spawnTimer=9999f;q.companion.begin(q.runWho);
        if(which==CORE) makeWord(L,new int[]{0});
        else if(which==SLIME) {
            q.stage=5;q.boss.begin(Boss.SLIME,5,q.rnd,q.playRosterFull());
            q.boss.intro=0;q.boss.slimeCoverLearned=true;
        } else if(which==CART || which==MINE) {
            q.stage=21;q.state=GameCore.BONUS;q.bonusTimer=1;
            if(which==MINE)q.mining.begin(q);else q.cart.begin(q);
        } else {
            q.starNext=which==STARS;
            q.earnedMash=GameCore.MASH_PERFECT;
            Interlude.enterBonus(q,L);
        }
        q.update(0,L);
        // Recreate only the next unlearned action, never make a player repeat an earlier one.
        if(which==STEAMER && learned(TutorialSpeech.ALTERNATE)) {
            q.bonusTimer=q.bonusRollEnd;q.steamer.hits=q.steamer.goal();q.steamer.swipeReady=true;
        }
        if(which==MINE && learned(TutorialSpeech.DIG)) {
            q.mining.ready=0;q.mining.loads=CaveMining.LOADS;q.mining.phase=CaveMining.FULL;
        }
        if(which==SLIME && learned(TutorialSpeech.CLOSED)) {
            q.boss.phase=1.35f;
            if(learned(TutorialSpeech.CHAIN))
                for(int i=0;i<Boss.SPLIT_HITS;i++)q.tapKey(q.boss.chainLetter(),L);
        }
        q.onboarding.teacher=c;
        introduce(c);
    }
    private void makeWord(Layout L,int[] letters) {
        GameCore q=practice;
        word=new GameCore.Enemy();word.word=letters;word.need=new int[letters.length];
        java.util.Arrays.fill(word.need,1);
        word.gone=new boolean[letters.length];word.goneT=new float[letters.length];
        word.goneDx=new float[letters.length];word.goneDy=new float[letters.length];
        word.baseX=L.w*.5f;word.y=L.playTop+(L.dangerY-L.playTop)*.45f;
        q.enemies.add(word);
    }
    boolean update(GameCore c,float dt,float elapsed,Layout L) {
        if(c.state!=GameCore.PLAY && c.state!=GameCore.BONUS) { clear();companionTravel=0;return false; }
        if(c.settingsOpen || c.paused || c.townOpen)return false;
        if(teacher!=null)return false;
        if(bossGuide && (!c.boss.fighting() || learned(speech)))bossGuide=false;
        moveCompanion(c,elapsed);
        if(briefing) { age+=elapsed;return true; }
        if(practice==null) {
            int next=0;
            if(c.state==GameCore.BONUS && !c.bossReward) {
                if(c.cart.active)next=CART;
                else if(c.mining.active)next=MINE;
                else if(c.starBonus)next=STARS;
                else if(!c.band.active && (c.bonusRolling() || c.bonusMashing()))next=STEAMER;
                if(!eligible(next))next=0;
            }
            if(next!=0 && !learned(mechanic(c,next)))begin(c,next,L);
        }
        if(practice==null) { age+=elapsed;hints(c,L);return briefing; }
        GameCore q=practice;age+=elapsed;
        if(briefing)return true;
        if(success>0) {
            success-=elapsed;q.clock+=dt;
            if(success<=0) {
                // The ready demonstration already ran in practice; launch the waiting course.
                if(lesson==STARS && c.starBonus)c.stars.timer=Math.min(c.stars.timer,StarPath.FLY+StarPath.EXIT+StarPath.REPORT);
                saved|=lesson;save(c);
                boolean touch=ownsTouch;clear();ownsTouch=touch;
            }
            return true;
        }
        // Remove deadlines only inside practice. All inputs and outcomes use ordinary rules.
        q.spawnTimer=9999f;
        if(lesson==CORE)word.speed=word.y<L.dangerY-L.enemyR*2?L.enemyR*1.2f:0;
        if(lesson==STEAMER && !q.bonusRolling() && !q.bonusPrizeWon())q.bonusTimer=q.bonusRollEnd;
        if(lesson==MINE)q.mining.left=CaveMining.TIME;
        if(lesson==CART) { q.cart.elapsed=0;q.cart.danger=0; }
        if(lesson==SLIME) {
            q.boss.promptT=q.boss.promptDelay();
            if(q.boss.hasGlob())globAge+=dt;
            // Keep the birth animation and the existing repeating full-drag gesture alive.
            float shown=globAge<.35f?globAge:.35f+(globAge-.35f)%1.8f;
            for(int i=0;i<Boss.ELEMS;i++)if(q.boss.etype[i]==Boss.E_GLOB)q.boss.elife[i]=Boss.GLOB_TIME-shown;
        }
        q.update(dt,elapsed,L);
        if(lesson==SLIME && q.boss.open() && (introduced&(1<<TutorialSpeech.CLOSED))!=0)
            learn(c,TutorialSpeech.CLOSED);
        if(lesson==CORE && !q.enemies.contains(word)) {
            if(step==0) { step=1;makeWord(L,new int[]{1,4,0}); }
            else complete();
        } else if(lesson==STEAMER && q.bonusPrizeWon()) {
            if(winSteamer(c))return true;
            complete();
        }
        else if(lesson==MINE && q.mining.carts>0)complete();
        else if(lesson==CART && q.cart.progress>0)complete();
        else if(lesson==STARS) {
            if(step>0 && learned(TutorialSpeech.STARS))complete();
            else if(q.stars.reporting())q.stars.begin(q.runWho,L);
        } else if(lesson==SLIME && q.boss.hp<q.boss.hpMax)complete();
        if(success==0)introduce(c);
        return true;
    }
    private boolean winSteamer(GameCore c) {
        if(c.bonusPrizeWon() || !(c.bonusRolling() || c.bonusMashing()))return false;
        // Reuse the real payout once, then show its celebration instead of another attempt.
        c.bonusTimer=c.bonusRollEnd;
        c.steamer.hits=c.steamer.goal();c.steamer.swipeReady=true;
        c.steamer.lidDrag=practice.steamer.freedLidLift;
        c.swipeBonus();
        saved|=STEAMER;save(c);
        boolean touch=ownsTouch;clear();ownsTouch=touch;
        return true;
    }
    private int mechanic() {
        if(lesson==CORE)return step==0?TutorialSpeech.MATCH:TutorialSpeech.WORD;
        return mechanic(practice,lesson);
    }
    private static int mechanic(GameCore q,int lesson) {
        if(lesson==STEAMER)return q.bonusSwipeReady()?TutorialSpeech.LIFT:TutorialSpeech.ALTERNATE;
        if(lesson==STARS)return TutorialSpeech.STARS;
        if(lesson==CART)return TutorialSpeech.LEAN;
        if(lesson==MINE)return q.mining.swipeReady()?TutorialSpeech.CART:TutorialSpeech.DIG;
        if(q.boss.hasGlob())return TutorialSpeech.GLOB;
        return q.boss.open()?TutorialSpeech.CHAIN:TutorialSpeech.CLOSED;
    }
    private void introduce(GameCore c) {
        speech=mechanic();
        int bit=1<<speech;
        if((introduced&bit)!=0 || learned(speech))return;
        introduced|=bit;briefing=true;age=0;
        Pause.release(c);narrate(c);
    }
    private void acknowledge(GameCore c) {
        if(c.sound!=null)c.sound.hush();
        narrator=null;
        if(bossHelp && nextBossHelp(c))return;
        if(bossHelp)bossGuide=true;
        briefing=false;
        bossHelp=false;
        // Acknowledgement resumes play; only a successful action retires the guidance.
    }
    private void complete() { success=1f;boolean touch=ownsTouch;cancelTouch();ownsTouch=touch; }
    private void hints(GameCore c,Layout L) {
        if(hintKind!=0 || c.pushLesson.active || c.state!=GameCore.PLAY || c.stage>4
                || c.boss.active() || !eligible(SKIPPED))return;
        int bit=0;
        if(c.misses>0 && eligible(WRONG_HINT)) { bit=WRONG_HINT;speech=TutorialSpeech.RETRY; }
        else for(GameCore.Enemy e:c.enemies) {
            if(!e.typeable())continue;
            if(eligible(STACK_HINT) && e.stacked(e.pos)) { bit=STACK_HINT;speech=TutorialSpeech.STACK;break; }
            if(e.y>L.playTop+L.enemyR*2 && eligible(WORD_HINT)) {
                bit=WORD_HINT;speech=e.word.length>1?TutorialSpeech.WORD:TutorialSpeech.MATCH;break;
            }
        }
        if(bit!=0) { hintKind=bit;briefing=true;age=0;Pause.release(c);narrate(c); }
    }
    static float skipY(Layout L) { return L.topSafe+L.unit*1.2f; }
    static boolean skipHit(Layout L,float x,float y) {
        return x>=L.w*.70f && x<=L.w*.98f && Math.abs(y-skipY(L))<L.unit*1.2f;
    }
    // Both native hosts pass stable pointer IDs, including historical move samples.
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(c.paused || c.settingsOpen)return false;
        if(action==0 && promptHit(c,L,x,y)) {
            ownsTouch=helpArmed=true;helpPointer=id;return true;
        }
        if(helpPointer>=0) {
            if(action==3) { cancelTouch();return true; }
            if(action==2 && id==helpPointer && !TutorialSpeech.helpHit(L,x,y))helpArmed=false;
            if(action==5 || action==6)helpArmed=false;
            if(action==1) {
                boolean open=helpArmed && id==helpPointer && promptHit(c,L,x,y);
                cancelTouch();
                if(open) { bossHelp=true;introduced=0;nextBossHelp(c); }
            }
            return true;
        }
        boolean visible=practice!=null || briefing || hintKind!=0 || bossGuide || c.pushLesson.active;
        if(action==0 && visible && skipHit(L,x,y)) { skip(c);ownsTouch=true;return true; }
        if(briefing) {
            ownsTouch=true;
            if(action==0) { speechPointer=id;continueArmed=TutorialSpeech.buttonHit(L,x,y); }
            else if(action==2 && id==speechPointer && !TutorialSpeech.buttonHit(L,x,y))continueArmed=false;
            else if(action==3 || action==5 || action==6) { speechPointer=-1;continueArmed=false; }
            else if(action==1) {
                boolean advance=id==speechPointer && continueArmed && TutorialSpeech.buttonHit(L,x,y);
                cancelTouch();
                if(advance)acknowledge(c);
            }
            return true;
        }
        if(practice==null) {
            if(!ownsTouch)return false;
            if(action==1 || action==3)ownsTouch=false;
            return true;
        }
        ownsTouch=true;
        if(action==3) { cancelTouch();return true; }
        GameCore q=practice;
        if(success>0) { if(action==1)ownsTouch=false;return true; }
        if(action==0 || action==5) {
            if(lesson==CART)q.cart.input.down(q,L,id,x,y);
            if(lesson==MINE)q.mining.input.down(q,L,id,x,y);
            if(pointer<0 && lesson==STEAMER && q.bonusSwipeReady() && Screens.inSteamerLid(q,L,x,y)) {
                pointer=id;lid=true;startY=y;
            } else if(pointer<0 && lesson==SLIME && y<L.deckTop && q.grabBoss(x,y))pointer=id;
            else if(pointer<0 && lesson==STARS && (StarScreen.inSlider(L,x,y)
                    || Math.hypot(x-q.stars.x,y-q.stars.characterY(L))<StarPath.flyerR(L)*1.45f)) {
                pointer=id;starDrag=true;q.stars.beginDrag();
                offsetX=StarScreen.inSlider(L,x,y)?0:q.stars.x-x;
                dragStar(q,L,x,y);
            } else {
                int g=q.keyAt(x,y,L);
                if(g>=0) {
                    if(lesson==STARS) { starPointers[g]=id;q.stars.hold(g,true);step=1; }
                    else if(q.state==GameCore.BONUS)q.tapBonus(g);
                    else q.tapKey(g,L);
                }
            }
        } else if(action==2) {
            q.cart.input.move(q,L,id,x,y);q.mining.input.move(q,L,id,x,y);
            if(id==pointer) {
                if(lid) {
                    q.dragBonusLid(startY-y);
                    if(Screens.steamerLidY(q,L)<=Screens.steamerReleaseY(q,L))q.swipeBonus();
                } else if(starDrag)dragStar(q,L,x,y);
                else q.dragBoss(x,y,L);
            }
        } else if(action==1 || action==6) {
            for(int g=0;g<starPointers.length;g++)if(starPointers[g]==id) { starPointers[g]=-1;q.stars.hold(g,false); }
            q.cart.input.up(id);q.mining.input.up(id);
            if(id==pointer) { pointer=-1;lid=starDrag=false;q.dragBonusLid(0);q.releaseBoss();q.stars.endDrag(); }
            if(action==1)ownsTouch=false;
        }
        return true;
    }
    private void dragStar(GameCore q,Layout L,float x,float y) {
        if(Math.abs(x+offsetX-q.stars.x)>L.w*.025f)step=1;
        q.stars.dragTo(x+offsetX,L);
    }
    int demoKey(GameCore c) {
        if(practice==null) {
            if(c.boss.fighting())return c.boss.kind==Boss.SLIME?c.boss.chainLetter():Math.max(0,c.boss.octoTarget);
            for(GameCore.Enemy e:c.enemies)if(e.typeable()
                    && (hintKind!=STACK_HINT || e.stacked(e.pos)))return e.word[e.pos];
            return 0;
        }
        if(lesson==CORE)return word.word[Math.min(word.pos,word.word.length-1)];
        if(lesson==STEAMER)return practice.steamer.wanted();
        if(lesson==MINE)return practice.mining.sequence[practice.mining.pos];
        if(lesson==SLIME)return practice.boss.chainLetter();
        return 0;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        Onboarding o=c.onboarding;
        if(o.teacher!=null)return;
        if(c.settingsOpen && o.practice==null)return;
        if(o.practice!=null) {
            Renderer.draw(p,o.practice,L);
            p.fillRect(0,0,L.w,L.topSafe+L.unit*2.4f,0xFF171426);
            p.text("PRACTICE",L.w*.05f,skipY(L)+L.unit*.3f,type(L.unit*.65f),INK,Painter.LEFT,true);
        }
        if(c.settingsOpen)return;
        if(o.offersBossHelp(c))TutorialSpeech.help(p,c,L);
        if(o.briefing)TutorialSpeech.large(p,c,L,o.speech,true);
        else if(o.practice!=null) {
            if(o.speaking(c))TutorialSpeech.reminder(p,c,L,o.speech);
            int key=-1;GameCore q=o.practice;
            if(o.lesson==CORE && o.word.pos<o.word.word.length)key=o.word.word[o.word.pos];
            if(o.lesson==STEAMER && q.bonusMashing() && !q.bonusSwipeReady())key=q.steamer.wanted();
            if(o.lesson==MINE && q.mining.digging())key=q.mining.sequence[q.mining.pos];
            if(o.lesson==SLIME && q.boss.open() && q.boss.slimePromptCover()<.3f && !q.boss.hasGlob())key=q.boss.chainLetter();
            if(key>=0 && o.success<=0)Renderer.touchHint(p,q.keyX(L,key),q.keyY(L,key),L.keyR*.65f,1f,.85f,o.age);
            if(o.success<=0)o.gesture(p,L);
        } else if(c.pushLesson.active)TutorialSpeech.large(p,c,L,TutorialSpeech.RESCUE,false);
        else if(o.hintKind!=0 || o.bossGuide)TutorialSpeech.reminder(p,c,L,o.speech);
        if(o.movingCompanion(c))TutorialSpeech.companion(p,c,L);
        if(o.practice!=null || o.briefing || o.hintKind!=0 || o.bossGuide || c.pushLesson.active) {
            p.fillPoly(pill(L.w*.84f,skipY(L),L.w*.135f,L.unit*.95f,12),0xFF493953);
            p.text("SKIP ALL",L.w*.84f,skipY(L)+L.unit*.3f,type(L.unit*.62f),INK,Painter.CENTER,true);
        }
    }
    private void gesture(Painter p,Layout L) {
        GameCore q=practice;
        float t=PushLesson.swipeProgress(age),x,y,angle=1f;
        if(lesson==STEAMER && q.bonusSwipeReady() && !lid) {
            x=L.w*.5f;y=Screens.steamerLidY(q,L)-L.unit*2.5f*t;
        } else if(lesson==MINE && q.mining.swipeReady() && q.mining.input.pointer<0) {
            x=q.mining.cartX*L.w+L.w*.24f*t;y=CaveMiningScreen.cartY(L);angle=2.4f;
        } else if(lesson==CART && q.cart.input.pointer<0) {
            x=L.w*.5f+q.cart.turn*L.w*.34f*t;y=StarScreen.sliderY(L);
        } else if(lesson==STARS && !q.stars.ready() && !q.stars.dragging && !q.stars.left && !q.stars.right) {
            x=L.w*.5f+(q.stars.starX(0,L)-L.w*.5f)*t;y=StarScreen.sliderY(L);
        } else return;
        Renderer.touchHint(p,x,y,L.keyR*.65f,angle,.8f,age);
    }
}
