package com.dddumpling.game;

final class TestCompanion extends Check {
    static void all(Layout L) {
        group("run companion");
        Mem save=new Mem();save.collected=(1L<<11)|(1L<<4);save.caseIndex=11;
        GameCore c=new GameCore(save,118L);c.startGame();
        check("companion uses the frozen run selection",c.companion.who==11);
        c.caseIndex=4;
        check("later case changes do not replace the companion",c.companion.who==11);
        GameCore.Enemy e=add(c,L,new int[]{0,1},L.playTop+L.enemyR*3);
        c.destroyWord(e,0,0,L);
        check("word destruction celebrates",c.companion.reaction==RunCompanion.WORD);
        c.takeHit(L.w*.5f,L);
        check("damage interrupts celebration",c.companion.reaction==RunCompanion.DAMAGE && c.companion.mood()==3);
        c.companion.age=.24f;
        check("damage pushes companion and home back three quarters of their height",
                Math.abs(c.companion.damagePush(L)-RunCompanion.halfHeight(L)*1.5f)<.001f);
        float left=c.companion.left;
        for(int i=0;i<50;i++) c.companion.react(RunCompanion.WORD,1f);
        check("event bursts cannot queue or prolong damage",c.companion.left==left && c.companion.reaction==RunCompanion.DAMAGE);
        float clock=c.companion.clock;float[] skin=c.companion.outline(L);
        c.paused=true;c.update(.2f,L);
        check("pause freezes character and home",clock==c.companion.clock && java.util.Arrays.equals(skin,c.companion.outline(L)));
        c.paused=false;c.openSettings();c.update(.2f,L);
        check("settings freeze the companion",clock==c.companion.clock && java.util.Arrays.equals(skin,c.companion.outline(L)));
        c.closeSettings();
        for(int i=0;i<60;i++) c.companion.update(c,DT);
        check("reaction settles without a queue",c.companion.reaction==RunCompanion.IDLE);
        check("home has genuine soft-body motion",!java.util.Arrays.equals(skin,c.companion.outline(L)) && c.companion.home.finite());
        float[] hex=c.companion.outline(L);
        check("home has a broad rounded hexagonal top",
                Math.abs(hex[109]-hex[115])<RunCompanion.halfHeight(L)*.12f
                        && Math.abs(hex[115]-hex[121])<RunCompanion.halfHeight(L)*.12f);
        check("home uses an even regular-hexagon aspect",
                Math.abs(RunCompanion.halfHeight(L)/RunCompanion.halfWidth(L)-.8660254f)<.0001f);
        c.startFrenzy(Power.FLING,L);
        RasterPainter masked=new RasterPainter((int)L.w,(int)L.h,1);masked.clear(0xFF010203);
        RunCompanion.draw(masked,c,L);
        boolean cloth=false,slit=false;
        for(int pixel:masked.resolve()) { cloth|=pixel==0xFF211B35;slit|=pixel==0xFF615370; }
        check("FLING gives the companion a ninja cap and mouth wrap",cloth && slit);
        RasterPainter maskShape=new RasterPainter(200,200,1);maskShape.clear(0xFF010203);
        Trinket.ninjaMask(maskShape,100,100,30,1f);int[] maskPixels=maskShape.resolve();
        check("ninja wrap leaves both eyes uncovered",
                maskPixels[100*200+90]==0xFF010203 && maskPixels[100*200+110]==0xFF010203
                        && maskPixels[82*200+100]==0xFF211B35
                        && maskPixels[114*200+100]==0xFF211B35);
        check("ninja hood covers both sides of the eye opening",
                maskPixels[100*200+75]==0xFF211B35 && maskPixels[100*200+125]==0xFF211B35);
        c.modeLeft=DT*.5f;c.update(DT,L);
        c.pushUsed=true;c.pushT=GameCore.PUSH_TIME*.5f;c.companion.rescue();
        c.companion.rescueT=RunCompanion.RESCUE_TIME-GameCore.PUSH_TIME*.5f;
        check("rescue swipe carries the companion upward with an intense expression",
                c.companion.rescueLift(L)<0f && c.companion.displayMood(c)==6);
        c.pushT=0f;c.companion.rescueT=RunCompanion.RESCUE_TIME-GameCore.PUSH_TIME
                -RunCompanion.RESCUE_HOLD*.5f;
        check("rescue companion holds at the push-back height for half a second",
                Math.abs(c.companion.rescueLift(L)
                        +(L.dangerY-L.playTop)*GameCore.PUSH_LIFT)<.001f
                        && c.companion.displayMood(c)==6);
        c.companion.rescueT=RunCompanion.RESCUE_RETURN;
        check("energy bar leaves the screen before the companion returns",
                c.companion.rescueBarFade()==0f && c.companion.rescueLift(L)<0f);
        c.companion.rescueT=0f;
        c.companion.reaction=RunCompanion.IDLE;
        check("companion returns home tired after the rescue swipe",
                c.companion.rescueLift(L)==0f && c.companion.displayMood(c)==7);
        c.pushSlowT=GameCore.PUSH_SLOW;c.companion.react(RunCompanion.DAMAGE,1f);
        check("damage recovery cannot replay a spent rescue animation",
                c.companion.rescueT==0f && c.companion.rescueLift(L)==0f);
        c.companion.left=0f;c.companion.reaction=RunCompanion.IDLE;
        c.pushUsed=false;c.pushSlowT=0f;
        c.startFrenzy(Power.TEAM,L);
        check("TEAM grows the companion from its home",c.companion.who==11 && c.buddy.who==11
                && c.buddy.entryLeft>0 && c.buddy.x==RunCompanion.x(L)
                && c.buddy.y==RunCompanion.y(L) && c.companion.reaction==RunCompanion.POWER);
        c.modeLeft=DT*.5f;c.update(DT,L);
        check("power ending sends the fighter back to the companion home",
                c.companion.who==11 && !c.powerActive() && c.buddy.returning());
        advance(c,L,Buddy.RETURN_TIME+.1f);
        check("home companion resumes after TEAM returns",c.buddy.out() && c.companion.who==11);

        Layout small=new Layout();small.compute(320,700,0,0,0,0);
        GameCore interlude=new GameCore(save,121L);interlude.startGame();interlude.playtestSteamer(small);
        RasterPainter visible=new RasterPainter(320,700,1);visible.clear(0xFF010203);
        RunCompanion.draw(visible,interlude,small);
        boolean painted=false;for(int pixel:visible.resolve()) painted|=pixel!=0xFF010203;
        check("companion remains visible throughout the steamer game",painted);

        interlude=new GameCore(save,122L);interlude.startGame();interlude.starNext=true;
        Interlude.enterBonus(interlude,small);
        float targetX=interlude.stars.flyerX(small),targetY=interlude.stars.flyerY(small);
        check("Star Path starts the companion at home",
                StarScreen.companionX(interlude,small,targetX)==RunCompanion.x(small)
                        && StarScreen.companionY(interlude,small,targetY)==RunCompanion.y(small));
        interlude.time=StarScreen.COMPANION_TRAVEL;
        check("Star Path moves the companion into the flyer position",
                StarScreen.companionX(interlude,small,targetX)==targetX
                        && StarScreen.companionY(interlude,small,targetY)==targetY
                        && StarScreen.companionR(interlude,small)==StarPath.flyerR(small));
        interlude.stars.timer=StarPath.REPORT-.01f;
        check("incomplete Star Path returns the companion from below the screen",
                StarScreen.companionX(interlude,small,targetX)==RunCompanion.x(small)
                        && StarScreen.companionY(interlude,small,targetY)>small.h);
        interlude.stars.timer=StarPath.REPORT-StarScreen.COMPANION_RETURN;
        check("Star Path return settles in the companion home",
                StarScreen.companionY(interlude,small,targetY)==RunCompanion.y(small));
        c.toTitle();check("title clears all reaction state",c.companion.who<0 && c.companion.left==0f);
        c.startGame();check("new run starts from idle",c.companion.reaction==0 && c.companion.clock==0);
        c.pushT=GameCore.PUSH_TIME;c.pushSlowT=GameCore.PUSH_SLOW;c.lives=1;c.takeHit(0,L);
        check("game over keeps the run companion crying",c.state==GameCore.OVER
                && c.companion.who==c.runWho && c.companion.reaction==RunCompanion.CRY
                && c.companion.mood()==5 && c.companion.rescueLift(L)==0f);
        float cryClock=c.companion.clock;c.update(.1f,L);
        check("crying animation continues during game over",c.companion.clock>cryClock
                && c.companion.reaction==RunCompanion.CRY);
        c.returnFade=GameCore.RETURN_FADE;
        c.update(GameCore.RETURN_FADE*.25f,L);
        check("companion remains through the summary exit fade",c.state==GameCore.OVER
                && c.companion.who==c.runWho);
        c.update(GameCore.RETURN_FADE*.25f+DT,L);
        check("midpoint cover clears the game-over companion",c.state==GameCore.TITLE
                && c.companion.who<0 && c.companion.left==0f);
        c=new GameCore(new Mem(),119L);c.beginStart();advance(c,L,Launch.PICK_TIME+Launch.TIME+.1f);
        check("shuffled run uses its resolved character",c.companion.who==c.runWho && c.companion.who>=0);
        c.boss.begin(Boss.SLIME,5,c.rnd);c.companion.update(c,DT);
        check("boss arrival gets anticipation",c.companion.reaction==RunCompanion.BOSS);
        c.boss.intro=0;c.boss.blive[0]=true;c.companion.update(c,DT);
        check("live boss attack gets concern",c.companion.reaction==RunCompanion.DANGER);
        c.boss.beaten=true;c.companion.update(c,DT);
        check("boss defeat gets celebration",c.companion.reaction==RunCompanion.VICTORY);
        GameCore twin=new GameCore(new Mem(),120L);c=new GameCore(new Mem(),120L);c.startGame();twin.startGame();
        for(int i=0;i<100;i++) { c.companion.react(RunCompanion.WORD,.6f);c.companion.update(c,DT); }
        check("companion never advances gameplay randomness",c.rnd.nextLong()==twin.rnd.nextLong());
        bounds();
    }
    private static void bounds() {
        for(int[] size:new int[][]{{320,568},{393,852},{640,1400},{1080,2400}}) for(boolean full:new boolean[]{false,true}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,size[0]*.04f,0,size[0]*.04f);
            GameCore c=new GameCore(new Mem(),118L);c.startGame();c.fullRoster=c.runFullRoster=full;
            boolean clear=true;
            for(int event=0;event<=RunCompanion.CRY;event++) {
                c.companion.begin(c.runWho);c.companion.react(event,1f);
                for(int frame=0;frame<60;frame++) {
                    c.companion.update(c,DT);
                    float[] ring=c.companion.outline(l);
                    for(int i=0;i<ring.length;i+=2) {
                        clear &= ring[i]>=0 && ring[i]<=l.w && ring[i+1]>=l.deckTop && ring[i+1]<=l.h-l.padB;
                        for(int key=0;key<Glyph.COUNT;key++) if(c.keyActive(key)) {
                            float dx=ring[i]-l.keyX[key],dy=ring[i+1]-l.keyY[key];
                            float keyR=l.keyR*c.keyScale();
                            clear &= dx*dx+dy*dy>keyR*keyR*1.1f;
                        }
                    }
                }
            }
            check("soft home clears key hit areas "+size[0]+" full="+full,clear);
            check("companion uses the title adventurer as its size reference "+size[0]+" full="+full,
                    RunCompanion.radius(c,l)==LandPicker.travelerRadius(c,l)*.84f);
        }
        Layout l=new Layout();l.compute(320,700,0,0,0,0);
        GameCore c=new GameCore(new Mem(),118L);c.startGame();
        boolean contained=true;
        for(int who=0;who<Collect.COUNT;who++) for(int event:new int[]{RunCompanion.IDLE,
                RunCompanion.VICTORY,RunCompanion.CRY}) {
            c.companion.begin(who);c.companion.react(event,1);c.companion.update(c,.15f);
            RasterPainter p=new RasterPainter(320,700,1);p.clear(0xFF010203);
            RunCompanion.draw(p,c,l);int[] pixels=p.resolve();
            for(int y=0;y<700;y++) for(int x=0;x<320;x++) if(pixels[y*320+x]!=0xFF010203) {
                boolean inside=y>=0 && y<l.h-l.padB;
                for(int key=0;key<Glyph.COUNT;key++) if(c.keyActive(key)) {
                    float dx=x-l.keyX[key],dy=y-l.keyY[key];
                    inside &= dx*dx+dy*dy>l.keyR*l.keyR;
                }
                contained &= inside;
            }
        }
        check("all collectible silhouettes and expressions stay in the reserved key gap",contained);
        c.companion.begin(c.runWho);c.companion.react(RunCompanion.DAMAGE,1f);c.companion.age=.24f;
        RasterPainter hurt=new RasterPainter(320,700,1);hurt.clear(0xFF010203);
        RunCompanion.draw(hurt,c,l);boolean damageOnscreen=true;
        int[] hurtPixels=hurt.resolve();
        for(int y=0;y<700;y++)for(int x=0;x<320;x++)if(hurtPixels[y*320+x]!=0xFF010203)
            damageOnscreen &= y>=0 && y<l.h-l.padB;
        check("damage knockback remains on screen",damageOnscreen);
        c.companion.begin(Collect.BOSS_FIRST+1);c.startFrenzy(Power.FLING,l);
        RasterPainter masked=new RasterPainter(320,700,1);masked.clear(0xFF010203);
        RunCompanion.draw(masked,c,l);
        boolean bossMask=false;for(int pixel:masked.resolve()) bossMask|=pixel==0xFF211B35;
        check("ninja mask follows special-family face placement",bossMask);
    }
}
