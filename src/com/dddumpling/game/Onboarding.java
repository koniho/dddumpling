package com.dddumpling.game;

/** Disposable real-game practice; the waiting run and its random stream never advance. */
final class Onboarding extends Draw {
    static final int CORE=1, STEAMER=2, STARS=4, CART=8, MINE=16, SLIME=32,
            SKIPPED=64, WORD_HINT=128, WRONG_HINT=256, STACK_HINT=512;
    int saved, lesson, step, pointer=-1;
    GameCore practice;
    GameCore.Enemy word;
    boolean corePending, ownsTouch, lid, starDrag;
    final int[] starPointers=new int[Glyph.COUNT];
    float age, success, startY, offsetX, hintLeft, globAge;
    String hint="";

    boolean eligible(int bit) { return (saved & (bit|SKIPPED))==0; }
    void save(GameCore c) { if(c.store!=null)c.store.saveTutorials(saved); }
    void reset(GameCore c) {
        clear();saved=0;corePending=false;hintLeft=0;
        c.pushLesson.seen=false;c.pushLesson.reset();
        if(c.store!=null)c.store.savePushLessonSeen(false);
        save(c);
    }
    void clear() { cancelTouch();practice=null;word=null;lesson=step=0;success=0; }
    void cancelTouch() {
        pointer=-1;ownsTouch=lid=starDrag=false;
        java.util.Arrays.fill(starPointers,-1);
        if(practice!=null)Pause.release(practice);
    }
    void skip(GameCore c) {
        clear();saved|=SKIPPED;corePending=false;hintLeft=0;
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
        if(c.state!=GameCore.PLAY && c.state!=GameCore.BONUS) { clear();hintLeft=0;return false; }
        if(c.settingsOpen || c.paused || c.townOpen)return false;
        if(practice==null) {
            int next=0;
            if(corePending && eligible(CORE))next=CORE;
            else if(c.state==GameCore.PLAY && c.stage==5 && c.boss.kind==Boss.SLIME
                    && c.boss.fighting() && eligible(SLIME))next=SLIME;
            else if(c.state==GameCore.BONUS && !c.bossReward) {
                if(c.cart.active)next=CART;
                else if(c.mining.active)next=MINE;
                else if(c.starBonus)next=STARS;
                else if(!c.band.active && c.bonusRolling())next=STEAMER;
                if(!eligible(next))next=0;
            }
            if(next!=0)begin(c,next,L);
        }
        if(practice==null) { hints(c,elapsed,L);return false; }
        GameCore q=practice;age+=elapsed;
        if(success>0) {
            success-=elapsed;q.clock+=dt;
            if(success<=0) {
                // The ready demonstration already ran in practice; launch the waiting course.
                if(lesson==STARS && c.starBonus)c.stars.timer=Math.min(c.stars.timer,StarPath.FLY+StarPath.EXIT+StarPath.REPORT);
                saved|=lesson;save(c);corePending=false;
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
        if(lesson==CORE && !q.enemies.contains(word)) {
            if(step==0) { step=1;makeWord(L,new int[]{1,4,0}); }
            else complete();
        } else if(lesson==STEAMER && q.bonusPrizeWon())complete();
        else if(lesson==MINE && q.mining.carts>0)complete();
        else if(lesson==CART && q.cart.progress>0)complete();
        else if(lesson==STARS) {
            if(step>0 && q.stars.count()>0)complete();
            else if(q.stars.reporting())q.stars.begin(q.runWho,L);
        } else if(lesson==SLIME && q.boss.hp<q.boss.hpMax)complete();
        return true;
    }
    private void complete() { success=1f;boolean touch=ownsTouch;cancelTouch();ownsTouch=touch; }
    private void hints(GameCore c,float dt,Layout L) {
        hintLeft=Math.max(0,hintLeft-dt);
        if(hintLeft>0 || c.pushLesson.active || c.state!=GameCore.PLAY || c.stage>4
                || c.boss.active() || !eligible(SKIPPED))return;
        int bit=0;
        if(c.misses>0 && eligible(WRONG_HINT)) { bit=WRONG_HINT;hint="WRONG KEY? START THE WORD AGAIN"; }
        else for(GameCore.Enemy e:c.enemies) {
            if(!e.typeable())continue;
            if(eligible(STACK_HINT) && e.stacked(e.pos)) { bit=STACK_HINT;hint="STACKED LETTER? TAP IT AGAIN";break; }
            if(e.y>L.playTop+L.enemyR*2 && eligible(WORD_HINT)) { bit=WORD_HINT;hint="CLEAR WORDS BEFORE THE RED LINE";break; }
        }
        if(bit!=0) { saved|=bit;save(c);hintLeft=3.5f; }
    }
    static float skipY(Layout L) { return L.topSafe+L.unit*1.2f; }
    static boolean skipHit(Layout L,float x,float y) {
        return x>=L.w*.70f && x<=L.w*.98f && Math.abs(y-skipY(L))<L.unit*1.2f;
    }
    // Both native hosts pass stable pointer IDs, including historical move samples.
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(c.paused || c.settingsOpen)return false;
        boolean visible=practice!=null || c.pushLesson.active;
        if(action==0 && visible && skipHit(L,x,y)) { skip(c);ownsTouch=true;return true; }
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
    String prompt() {
        GameCore q=practice;
        if(success>0)return "NICE! LET'S PLAY";
        if(lesson==CORE)return step==0?"TAP THE MATCHING CHARACTER KEY":"CLEAR THE WORD FROM LEFT TO RIGHT";
        if(lesson==STEAMER)return q.bonusSwipeReady()?"SWIPE THE LID UP TO FREE A DUMPLING":"ALTERNATE LIT KEYS TO OPEN THE LID";
        if(lesson==STARS)return q.stars.ready()?"STEER TO COLLECT THE STARS":"DRAG THE SLIDER TOWARD A STAR";
        if(lesson==CART)return "DRAG TO LEAN INTO THE CURVE";
        if(lesson==MINE)return q.mining.swipeReady()?"SWIPE THE FULL CART LEFT OR RIGHT":"TYPE THE LETTERS TO FILL THE CART";
        if(q.boss.hasGlob())return "DRAG THE GLOB OFF EITHER SIDE";
        return q.boss.open()?"TYPE THE CHAIN TO GROW A GLOB":"CLOSED! WAIT FOR THE LETTER";
    }
    String title() {
        switch(lesson) {
            case STEAMER:return "STEAMER PRACTICE";
            case STARS:return "STAR PATH PRACTICE";
            case CART:return "CART RUSH PRACTICE";
            case MINE:return "DUMPLING MINE PRACTICE";
            case SLIME:return "SLIME PRACTICE";
            default:return "LET'S PRACTICE";
        }
    }
    static void draw(Painter p,GameCore c,Layout L) {
        Onboarding o=c.onboarding;
        if(o.practice!=null) {
            Renderer.draw(p,o.practice,L);
            float s=L.unit,y=L.topSafe+s*3.6f;
            float bottom=o.lesson==SLIME?Math.max(y+s*1.3f,BossScreen.blurbY(L)+s*.3f):y+s*1.3f;
            p.fillRect(0,L.topSafe,L.w,bottom,0xFF171426);
            p.text(o.title(),L.w*.05f,skipY(L)+s*.25f,type(s*.42f),GOLD,Painter.LEFT,true);
            p.text(o.prompt(),L.w*.5f,y,type(s*.44f),INK,Painter.CENTER,true);
            if(o.lesson==CART)p.text("KEEP GREEN TO REACH CHECKPOINTS",L.w*.5f,y+s,type(s*.36f),INK_DIM,Painter.CENTER,true);
            int key=-1;GameCore q=o.practice;
            if(o.lesson==CORE && o.word.pos<o.word.word.length)key=o.word.word[o.word.pos];
            if(o.lesson==STEAMER && q.bonusMashing() && !q.bonusSwipeReady())key=q.steamer.wanted();
            if(o.lesson==MINE && q.mining.digging())key=q.mining.sequence[q.mining.pos];
            if(o.lesson==SLIME && q.boss.open() && q.boss.slimePromptCover()<.3f && !q.boss.hasGlob())key=q.boss.chainLetter();
            if(key>=0 && o.success<=0)Renderer.touchHint(p,q.keyX(L,key),q.keyY(L,key),L.keyR*.65f,1f,.85f,o.age);
            if(o.success<=0)o.gesture(p,L);
        } else if(o.hintLeft>0 && !c.pushLesson.active && !c.settingsOpen) {
            float y=L.playTop+L.unit*1.4f;
            p.fillRect(0,y-L.unit,L.w,y+L.unit*.5f,0xCC171426);
            p.text(o.hint,L.w*.5f,y,type(L.unit*.42f),INK,Painter.CENTER,true);
        }
        if(o.practice!=null || c.pushLesson.active)
            p.text("SKIP ALL",L.w*.84f,skipY(L)+L.unit*.25f,type(L.unit*.46f),GOLD,Painter.CENTER,true);
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
