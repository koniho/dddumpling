package com.dddumpling.game;

/** The run companion demonstrates a control while the real scene waits. */
final class TutorialSpeech extends Draw {
    static final int MATCH=1, WORD=2, ALTERNATE=3, LIFT=4, STARS=5, LEAN=6,
            DIG=7, CART=8, CLOSED=9, CHAIN=10, GLOB=11, DANGER=12, RETRY=13,
            STACK=14, RESCUE=15, SUCCESS=16, PINCH=17, DEFEND=18, TEAR=19, SHAKE=20, WAIT=21,
            POWER_FLURRY=22, POWER_NINJA=23, POWER_TEAM=24, POWER_PICKUP=25,
            MINIGAMES=26, DISPLAY_CASE=27, STORIES=28, COMPANION=29;
    private static final int PAPER=0x4DFFF5DD, TEXT_INK=0xFF302440, ACCENT=0xFF754070;
    private static final String[][] LINES={
        {"", ""}, {"MATCH THE FACE!", "TAP ITS KEY BELOW."},
        {"TAP KEYS IN ORDER!", "START ON THE LEFT."},
        {"GO AS FAST", "AS YOU CAN!"},
        {"GREAT JOB! LIFT IT!", "SWIPE UP TO WIN!"},
        {"STAR PATH!", "SLIDE TO CATCH STARS!"},
        {"CART RUSH!", "STAY IN THE GREEN."},
        {"DUMPLING MINE!", "TAP MATCHING KEYS!"},
        {"THE CART IS FULL!", "SWIPE IT AWAY."},
        {"WRONG OR HIDDEN?", "YOU'LL GET SLIMED!"},
        {"TAP MATCHING KEYS!", "WHEN THE FACE SHOWS!"},
        {"GRAB THE RED AREA!", "DRAG TO A SIDE!"},
        {"QUICK, TAP!", "BEAT THE RED LINE."},
        {"OOPS! TRY AGAIN.", "START ON THE LEFT."},
        {"MORE THAN ONE!", "TAP THE KEY AGAIN."},
        {"NEED SOME SPACE?", "SWIPE THE BAR UP!"},
        {"YOU DID IT!", "LET'S PLAY!"},
        {"MATCH THE KEY!", "SPREAD TWO FINGERS."},
        {"SAVE YOUR KEYS!", "TAP THE SHOWN KEY."},
        {"GRAB THE ARM TIP!", "PULL IT AWAY."},
        {"GRAB THE CAP!", "SHAKE SIDE TO SIDE."},
        {"STEAMER: WAIT!", "WATCH YOUR KEYS SPIN!"},
        {"FLURRY: ANY KEY!", "TAP, TAP, TAP!"},
        {"NINJA: SWIPE!", "SLICE THE SQUISHIES!"},
        {"TEAM SQUISH!", "TAP TO AIM ME!"},
        {"A GLOWING PICKUP!", "TAP IT TO COLLECT!"},
        {"STEAMER TIME!", "COLLECT DUMPLINGS!"},
        {"YOUR DUMPLINGS!", "OPEN THEIR CASE!"},
        {"TAP YOUR DUMPLING!", "DISCOVER ITS STORY!"},
        {"YOUR FIRST FRIEND!", "LET'S PLAY TOGETHER!"}
    };
    static String spoken(int message) {
        if(message==COMPANION)return "I'm your first squishy! I'll stay by your keys. I'll show you how to play as we go. Let's find more friends!";
        if(message==POWER_FLURRY)return "Flurry! Tap any key to clear the falling faces.";
        if(message==POWER_NINJA)return "Ninja! Swipe across squishies to slice them.";
        if(message==POWER_PICKUP)return "Tap the glowing pickup to collect its power!";
        if(message==STACK)return "See the little pips on this Squishy? Tap its matching key once for each pip. Keep tapping until it's cleared!";
        if(message==POWER_TEAM)return "Team Squish! Tap any key to aim me and clear words.";
        if(message==MINIGAMES)return "Steamer is a minigame! Play a minigame after each stage. You can win dumplings! Let's free one from this steamer!";
        if(message==DISPLAY_CASE)return "Your friends live here! Tap the case to visit them!";
        if(message==STORIES)return "Tap the middle dumpling to read or hear its story!";
        if(message==RESCUE)return "An enemy is getting close! Swipe up from the glowing bar to push the Squishies back. You can do this once each stage. Give yourself some room!";
        if(message==STARS)return "Star Path is starting! Slide to steer your dumpling and catch the stars. Collect them all to win a new friend!";
        if(message==LEAN)return "Cart Rush is starting! Slide left and right to steer. Stay in the green and help your cart reach the finish!";
        if(message==DIG)return "Dumpling Mine is starting! Tap the matching keys in order to fill your cart. Let's dig up a new friend!";
        if(message==WAIT)return "Steamer is starting! Wait while two random keys are picked. Watch them spin! Get ready. You can do this!";
        if(message==ALTERNATE)return "Your keys are ready! Tap left, right, left, right. Go as fast as you can! You've got this!";
        if(message==LIFT)return "Great job! The lid is ready. Swipe it up to win. You can do it!";
        if(message==SUCCESS)return "Amazing! You did it! You freed a dumpling!";
        if(message==CLOSED)return "Wait until Slime's face is visible. Tap the key that matches Slime's face. Wrong keys or hidden faces will get you slimed!";
        if(message==CHAIN)return "Tap the matching key while Slime's face is visible. Keep matching to make Slime vulnerable. Watch for the red area!";
        if(message==GLOB)return "Now Slime is vulnerable! Touch and hold the red area. Drag it to either side of the screen. That damages Slime! You've got this!";
        if(message==PINCH)return "Tap the matching key. Put two fingers on the cube. Spread them apart to split it.";
        if(message==DEFEND)return "Octopulse reaches for your keys. Tap the matching key to defend it.";
        if(message==TEAR)return "Wait for an exposed arm tip. Grab it and drag it away from Octopulse.";
        if(message==SHAKE)return "Grab Fly Agaric's cap. Keep holding it and shake it from side to side.";
        return (LINES[message][0]+" "+LINES[message][1]).toLowerCase(java.util.Locale.ROOT);
    }
    private static final int LINE_CHARS=22, PAGE_LINES=3;
    private static final String[][][] PAGES=makePages();
    static String[] pageLines(int message,int page) { return PAGES[message][page]; }
    static int pageCount(int message) { return PAGES[message].length; }
    static String spokenPage(int message,int page) { return joinLines(pageLines(message,page)); }
    private static String joinLines(String[] lines) {
        StringBuilder text=new StringBuilder();
        for(String line:lines) { if(text.length()>0)text.append(' ');text.append(line); }
        return text.toString();
    }
    private static String[] wrap(String text) {
        java.util.ArrayList<String> lines=new java.util.ArrayList<>();String line="";
        for(String word:text.split(" +")) {
            if(!line.isEmpty() && line.length()+1+word.length()>LINE_CHARS) { lines.add(line);line=""; }
            line+=(line.isEmpty()?"":" ")+word;
        }
        if(!line.isEmpty())lines.add(line);
        return lines.toArray(new String[0]);
    }
    private static String[][][] makePages() {
        String[][][] all=new String[COMPANION+1][][];
        for(int message=MATCH;message<=COMPANION;message++) {
            java.util.ArrayList<String[]> pages=new java.util.ArrayList<>();String pending="";
            // Page boundaries are sentence boundaries; copy must fit without splitting a sentence.
            for(String sentence:spoken(message).split("(?<=[.!?]) +")) {
                String joined=pending.isEmpty()?sentence:pending+" "+sentence;
                if(wrap(joined).length<=PAGE_LINES) { pending=joined;continue; }
                if(!pending.isEmpty())pages.add(wrap(pending));
                pending=sentence;
            }
            if(!pending.isEmpty())pages.add(wrap(pending));
            all[message]=pages.toArray(new String[0][]);
        }
        return all;
    }
    static float helpX(Layout L) { return RunCompanion.x(L)+L.unit*2.1f; }
    static float helpY(Layout L) { return RunCompanion.y(L)-L.unit*2.5f; }
    static boolean helpHit(Layout L,float x,float y) {
        return Math.abs(x-helpX(L))<L.unit*1.35f && Math.abs(y-helpY(L))<L.unit*1.35f;
    }
    static float helpPulse(float clock) { return .5f+.5f*(float)Math.sin(clock*(float)Math.PI*2/1.5f); }
    static float helpScale(float clock) { return .88f+.24f*helpPulse(clock); }
    static int helpBorder(float clock) { return Glyph.mix(GOLD,ROSE,helpPulse(clock)); }
    static void help(Painter p,GameCore c,Layout L) {
        float s=L.unit*helpScale(c.clock),x=helpX(L),y=helpY(L);
        bubble(p,x-s,y-s,x+s,y+s,s*.55f,1,
                new float[]{x+s*.3f,y+s,x-s*.7f,y+s*1.6f,x-s*.3f,y+s},1,0,0,helpBorder(c.clock));
        speechText(p,"?",x,y+s*.48f,type(s*1.25f),0xFFFFF5DD);
    }
    static float unit(Layout L) { return Math.min(L.unit,(L.deckTop-L.topSafe)/24f); }
    static float top(Layout L) { return L.topSafe+unit(L)*7f; }
    static float buttonY(Layout L) { return top(L)+unit(L)*12.5f; }
    static boolean buttonHit(Layout L,float x,float y) {
        return Math.abs(x-L.w*.5f)<L.w*.32f && Math.abs(y-buttonY(L))<unit(L)*1.3f;
    }
    static int speaker(GameCore c) { return c.companion.who>=0?c.companion.who:Math.max(0,c.runWho); }
    private static void speechText(Painter p,String text,float x,float y,float size,int color) {
        // Opaque lettering stays readable over the scene showing through the bubble.
        float edge=size*.055f;
        p.text(text,x-edge,y,size,TEXT_INK,Painter.CENTER,true);
        p.text(text,x+edge,y,size,TEXT_INK,Painter.CENTER,true);
        p.text(text,x,y-edge,size,TEXT_INK,Painter.CENTER,true);
        p.text(text,x,y+edge,size,TEXT_INK,Painter.CENTER,true);
        p.text(text,x,y,size,color,Painter.CENTER,true);
    }
    private static void bubble(Painter p,float l,float t,float r,float b,float radius,int tailEdge,float[] tail,
            float growth,float originX,float originY,int border) {
        // One fill keeps the translucent tail from double-blending with the body.
        float[] outline=new float[62];int at=0;
        for(int corner=0;corner<4;corner++) {
            float cx=corner==0 || corner==3?r-radius:l+radius;
            float cy=corner<2?b-radius:t+radius;
            for(int j=0;j<=6;j++) {
                float a=(corner*90f+j*15f)*(float)Math.PI/180f;
                outline[at++]=cx+radius*(float)Math.cos(a);outline[at++]=cy+radius*(float)Math.sin(a);
            }
            if(corner+1==tailEdge)for(float point:tail)outline[at++]=point;
        }
        float scale=1-(1-growth)*(1-growth);
        for(int i=0;i<outline.length;i+=2) {
            outline[i]=originX+(outline[i]-originX)*scale;
            outline[i+1]=originY+(outline[i+1]-originY)*scale;
        }
        p.fillPoly(outline,PAPER);p.strokePoly(outline,border,radius*.14f*scale);
    }
    static void large(Painter p,GameCore c,Layout L,int message,boolean button) {
        float s=unit(L),t=top(L),b=t+s*14.2f,x=L.w*.5f;
        float age=c.onboarding.age;
        if(button)p.fillRect(0,0,L.w,L.h,0xB8101022);
        float growth=c.onboarding.bubbleProgress();
        if(growth<=0)return;
        // The tail points at this run's actual companion.
        float tx=L.w*.13f;
        bubble(p,L.w*.05f,t,L.w*.95f,b,s*1.2f,3,
                new float[]{tx,t,tx+s*.5f,t-s*1.1f,tx+s*2,t},growth,tx,L.topSafe+s*5.3f,TEXT_INK);
        if(growth<1)return;
        String[] lines=pageLines(message,c.onboarding.speechPage);
        for(int i=0;i<lines.length;i++)speechText(p,lines[i],x,t+s*(1.7f+i*1.4f),type(s*.90f),0xFFFFF5DD);
        if(pageCount(message)>1)speechText(p,(c.onboarding.speechPage+1)+" / "+pageCount(message),
                L.w*.88f,t+s*.75f,type(s*.4f),0xFFFFC1E6);
        demonstrate(p,c,L,message,x,t+s*8.4f,s,age);
        if(button) {
            p.fillPoly(pill(x,buttonY(L),L.w*.32f,s*1.15f,14),0xFF387358);
            String buttonText=c.onboarding.moreSpeech() || c.onboarding.moreBossHelp(c) || message==MINIGAMES?"NEXT":message==COMPANION?"LET'S PLAY!":message==WAIT?"LET'S WATCH!":message==ALTERNATE?"LET'S GO!":"LET'S TRY!";
            p.text(buttonText,x,buttonY(L)+s*.38f,type(s*.92f),0xFFFFFFFF,Painter.CENTER,true);
        }
    }
    static void reminder(Painter p,GameCore c,Layout L,int message) {
        float s=unit(L),t=L.topSafe+s*2.6f,b=t+s*4f,l=L.w*.25f;
        float growth=c.onboarding.bubbleProgress();
        if(growth<=0)return;
        bubble(p,l,t,L.w*.97f,b,s*.7f,2,
                new float[]{l,b-s*.8f,l-s*.9f,b-s,l,t+s*1.2f},growth,L.w*.13f,L.topSafe+s*5.3f,TEXT_INK);
        if(growth<1)return;
        float x=(l+L.w*.97f)*.5f;
        speechText(p,LINES[message][0],x,t+s*1.5f,type(s*.70f),0xFFFFF5DD);
        speechText(p,LINES[message][1],x,t+s*3f,type(s*.70f),0xFFFFC1E6);
        if(c.onboarding.powerGuide && message!=POWER_PICKUP) {
            float left=l+s,right=L.w*.97f-s,y=b-s*.45f;
            p.fillRect(left,y,right,y+s*.15f,0x44754070);
            p.fillRect(left,y,left+(right-left)*Math.max(0f,Math.min(1f,c.modeLeft/Power.DURATION)),y+s*.15f,ACCENT);
        }
    }
    static float hopProgress(float travel) { return Math.max(0,(travel-.2f)/.8f); }
    static float hopLift(float travel,float fromY,float toY,float s) {
        // Bounce on the spot, then follow a parabola. Reversing travel retraces it without a snap.
        if(travel<.2f) {
            float bounce=(float)Math.sin(Math.PI*travel/.2f);
            return s*.9f*bounce*bounce;
        }
        float u=hopProgress(travel),height=Math.abs(toY-fromY)*.32f+s*1.5f;
        return 4*height*u*(1-u);
    }
    static void companion(Painter p,GameCore c,Layout L) {
        Onboarding o=c.onboarding;GameCore q=o.practice==null?c:o.practice;
        float u=hopProgress(o.companionTravel);
        float x=RunCompanion.x(L),y=RunCompanion.y(L),r=RunCompanion.radius(c,L),s=unit(L);
        if(q.state==GameCore.BONUS && q.starBonus) {
            x=StarScreen.companionX(q,L,q.stars.flyerX(L));
            y=StarScreen.companionY(q,L,q.stars.flyerY(L));r=StarScreen.companionR(q,L);
        } else if(!q.buddy.out()) {
            x=q.buddy.x;y=q.buddy.y;r=q.buddy.radius(L)*.74f;
        }
        float targetY=L.topSafe+s*4.9f,lift=hopLift(o.companionTravel,y,targetY,s);
        x+=(L.w*.13f-x)*u;y+=(targetY-y)*u-lift;r+=(s*1.6f-r)*u;
        Trinket.drawReacting(p,speaker(c),x,y,r,o.age,1f,1,0);
    }
    private static void face(Painter p,int key,float x,float y,float r) {
        p.fillPoly(Glyph.hex(x,y,r),Glyph.mix(Glyph.COLOR[key],0xFFFFFFFF,.65f));
        p.strokePoly(Glyph.hex(x,y,r),Glyph.COLOR[key],r*.09f);
        Kawaii.draw(p,key,x,y,r*.68f,Glyph.COLOR[key],1f,.6f);
    }
    private static void arrow(Painter p,float x,float y,float dx,float dy,float s) {
        p.line(x,y,x+dx,y+dy,ACCENT,s*.16f);
        float a=(float)Math.atan2(dy,dx),c=(float)Math.cos(a),v=(float)Math.sin(a);
        p.polyline(new float[]{x+dx-s*c+s*v*.6f,y+dy-s*v-s*c*.6f,x+dx,y+dy,
                x+dx-s*c-s*v*.6f,y+dy-s*v+s*c*.6f},ACCENT,s*.16f);
    }
    private static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }
    static float swipeProgress(float clock) {
        float t = clamp01((clock % 1.8f - .25f) / .9f);
        return t * t * (3f - 2f * t);
    }

    static void rescueGesture(Painter p, GameCore c, Layout L) {
        Onboarding lesson = c.onboarding;
        if (!lesson.rescueGuide) return;
        float s = Pause.scale(L), x = L.w * .5f;
        // Leave the real swipe bar uncovered so the lesson teaches its ordinary appearance.
        p.fillRect(0, 0, L.w, L.dangerY, 0xCC100D20);
        p.fillRect(0, L.deckTop, L.w, L.h, 0xCC100D20);
        p.fillRect(0, L.dangerY, L.playLeft, L.deckTop, 0xCC100D20);
        p.fillRect(L.playRight, L.dangerY, L.w, L.deckTop, 0xCC100D20);
        float base = (L.dangerY + L.deckTop) * .5f;
        float tip = base - L.enemyR * 2.7f;
        p.polyline(new float[] {x, base, x, tip}, GOLD, s * .15f);
        p.polyline(new float[] {x-s*.6f, tip+s*.65f, x, tip, x+s*.6f, tip+s*.65f},
                GOLD, s*.15f);
        float phase = lesson.age % 1.8f;
        float fade = Math.min(clamp01(phase / .15f),
                clamp01((1.65f - phase) / .3f));
        Renderer.touchHint(p, x, base - L.enemyR * 2.7f * swipeProgress(lesson.age),
                L.enemyR * 1.05f, (float) Math.PI * .5f, fade, lesson.age);
    }
    private static void demonstrate(Painter p,GameCore c,Layout L,int message,float x,float y,float s,float age) {
        Onboarding o=c.onboarding;GameCore q=o.practice==null?c:o.practice;
        float travel=swipeProgress(age),handX=x,handY=y;
        int key=o.demoKey(c);
        if(message==COMPANION) {
            p.text(Collect.NAME[c.runWho],x,y,type(s*.85f),INK,Painter.CENTER,true);
            p.text("YOUR COMPANION",x,y+s*1.8f,type(s*.6f),ROSE,Painter.CENTER,true);
            return;
        } else if(message==MINIGAMES || message==DISPLAY_CASE || message==STORIES || message==SUCCESS) {
            int who=message==STORIES?c.caseIndex:c.prize>=0?c.prize:0;
            if(message==DISPLAY_CASE)p.fillPoly(pill(x,y,s*3.3f,s*2.2f,16),0xFF493953);
            if(message==STORIES) {
                p.fillPoly(new float[]{x-s*3,y-s*1.8f,x,y-s*1.4f,x+s*3,y-s*1.8f,
                        x+s*3,y+s*1.8f,x,y+s*2.2f,x-s*3,y+s*1.8f},0xFFFFF5DD);
                p.line(x,y-s*1.4f,x,y+s*2.2f,ACCENT,s*.12f);
            }
            Trinket.drawReacting(p,who,x,y,s*1.5f,age,1f,1,0);
        } else if(message==POWER_PICKUP) {
            int effect=c.power==null?Power.FLURRY:c.power.shownEffect();
            Renderer.summaryPowerIcon(p,effect,x,y,s*1.65f,age);
        } else if(message==POWER_FLURRY) {
            int at=(int)(age*2.4f)%3;
            face(p,1,x,y-s*1.1f,s*1.1f);
            for(int i=0;i<3;i++)face(p,Roster.at(c.playRosterFull(),i),x+(i-1)*s*3.4f,y+s*1.5f,s*1.05f);
            handX=x+(at-1)*s*3.4f;handY=y+s*1.5f;
            arrow(p,handX,handY-s*1.2f,x-handX,-s*1.2f,s*.45f);
        } else if(message==POWER_NINJA) {
            handX=x+s*(-5+travel*10);
            for(int i=0;i<3;i++) {
                float fx=x+(i-1)*s*3.4f;
                face(new OpacityPainter(p,handX>fx?.25f:1f),Roster.at(c.playRosterFull(),i),fx,y,s*1.25f);
            }
            p.line(x-s*5,y,handX,y,0xFF387358,s*.2f);
        } else if(message==POWER_TEAM) {
            face(p,1,x+s*2.1f,y-s*.7f,s);
            face(p,4,x+s*4.3f,y-s*.7f,s);
            float bx=x+s*(-3.5f+5.6f*travel);
            Trinket.drawReacting(p,speaker(c),bx,y-s*.7f,s*1.1f,age,1f,1,0);
            face(p,1,x-s*3.5f,y+s*1.8f,s);
            handX=x-s*3.5f;handY=y+s*1.8f;
            arrow(p,x-s*2,y+s*1.4f,s*5,0,s*.5f);
        } else if(message==WAIT) {
            int roll=(int)(age*7f),count=Roster.count(q.playRosterFull()),half=count/2;
            face(p,Roster.at(q.playRosterFull(),roll%half),x-s*3,y,s*1.65f);
            face(p,Roster.at(q.playRosterFull(),half+(roll+1)%half),x+s*3,y,s*1.65f);
            p.fillCircle(x,y,s*.9f,0xFFEAC15C);
            for(int i=-1;i<=1;i+=2)p.line(x+i*s*.25f,y-s*.4f,x+i*s*.25f,y+s*.4f,TEXT_INK,s*.2f);
            p.text("PICKING AT RANDOM",x,y+s*2.2f,type(s*.6f),ACCENT,Painter.CENTER,true);
            return; // No tapping hand until the random selection has settled.
        } else if(message==PINCH) {
            face(p,key,x,y,s*1.7f);
            float spread=s*(1+travel*3);
            arrow(p,x-s,y,-s*3,0,s*.6f);arrow(p,x+s,y,s*3,0,s*.6f);
            Renderer.touchHint(p,x-spread,y,s*1.1f,1f,.95f,age);
            handX=x+spread;
        } else if(message==SHAKE) {
            float dx=(float)Math.sin(age*6f)*s*2.4f;
            p.fillRect(x-s*.45f,y,x+s*.45f,y+s*2,0xFFE8CD9D);
            p.fillEllipse(x+dx,y-s*.5f,s*2.4f,s*1.2f,0xFFCB526A);
            arrow(p,x-s*4,y+s*2,s*8,0,s*.6f);handX=x+dx;handY=y-s*.5f;
        } else if(message==TEAR) {
            float dx=s*(1+travel*4);
            p.line(x-s*3,y+s,x+dx,y,0xFFB87AD2,s*.8f);
            p.fillCircle(x+dx,y,s*.8f,0xFFE9B8E7);handX=x+dx;
        } else if(message==STACK) {
            // The same enemy renderer shows the actual stacked body and remaining-hit pips.
            GameCore.Enemy e=new GameCore.Enemy();
            e.word=new int[]{key};e.need=new int[]{3};e.gone=new boolean[1];
            e.done=(int)(age*1.2f)%3;e.enterT=1;e.baseX=x+s*2.8f;e.y=y-s*.4f;
            p.fillPoly(pill(e.baseX,e.y,s*3.2f,s*3.1f,16),0xFF252038);
            Renderer.enemy(p,c,L,e);
            handX=x-s*3.6f;handY=y+s*.7f;
            face(p,key,handX,handY,s*1.25f);
            arrow(p,handX+s*1.3f,handY,s*1.4f,-s*.7f,s*.4f);
        } else if(message==WORD || message==DIG || message==RETRY) {
            int at=(int)(age*1.3f)%3;
            for(int i=0;i<3;i++) {
                int g=message==DIG?q.mining.sequence[i%q.mining.length]:new int[]{1,4,0}[i];
                face(p,g,x+(i-1)*s*3.4f,y,s*1.25f);
            }
            arrow(p,x-s*4,y+s*1.8f,s*8,0,s*.6f);
            handX=x+(at-1)*s*3.4f;
        } else if(message==ALTERNATE) {
            face(p,q.steamer.leftKey,x-s*3,y,s*1.65f);face(p,q.steamer.rightKey,x+s*3,y,s*1.65f);
            handX=x+((int)(age*3f)%2==0?-3:3)*s;
        } else if(message==STARS || message==LEAN) {
            float dx=(float)Math.sin(age*1.8f)*s*3.2f;
            p.fillPoly(pill(x,y+s,L.w*.29f,s*.15f,12),0xFFC9B8CB);
            if(message==STARS)p.fillPoly(star(x+dx,y-s*1.4f,s, s*.45f,5,0),0xFFD08C12);
            else {
                p.fillPoly(pill(x,y-s*1.2f,s*4,s*.45f,12),0xFFCD5679);
                p.fillPoly(pill(x,y-s*1.2f,s*2,s*.45f,12),0xFF41A781);
                p.fillCircle(x,y-s*1.2f,s*.55f,TEXT_INK);
            }
            p.fillCircle(x+dx,y+s,s*.65f,0xFF387358);handX=x+dx;handY=y+s;
        } else if(message==LIFT || message==RESCUE) {
            if(message==LIFT) {
                p.fillEllipse(x,y+s,s*2.7f,s*.85f,0xFFCFA36B);
                Kawaii.draw(p,Kawaii.DUMPLING,x,y,s*1.15f,0xFFFFCE8B,1f,.7f);
                p.fillEllipse(x,y-s*(.7f+travel*2.2f),s*2.9f,s*.6f,0xFFEAC78E);
                handY=y-s*(.7f+travel*2.2f);
            } else {
                p.fillPoly(pill(x,y+s,s*4,s*.22f,10),0xFFD08C12);handY=y+s-travel*s*3;
            }
            arrow(p,x+s*4,y+s,0,-s*3,s*.7f);
        } else if(message==CART || message==GLOB) {
            float dx=travel*s*6;
            p.line(x+s*4.8f,y-s*2,x+s*4.8f,y+s*2,ACCENT,s*.12f);
            if(message==CART) {
                p.fillPoly(new float[]{x-s*3+dx,y-s*.4f,x+s*1+dx,y-s*.4f,x+s*.5f+dx,y+s,x-s*2.5f+dx,y+s},0xFFA77D92);
                for(int i=0;i<3;i++)p.fillCircle(x-s*2+i*s+dx,y-s*.9f,s*.6f,0xFF90775A);
                p.fillCircle(x-s*2.3f+dx,y+s*1.3f,s*.45f,TEXT_INK);p.fillCircle(x+s*.3f+dx,y+s*1.3f,s*.45f,TEXT_INK);
            } else {
                Kawaii.draw(p,Kawaii.BLOB,x-s*2,y,s*1.7f,0xFF91CA43,1f,.6f);
                p.fillCircle(x-s+dx,y,s*.8f,0xFFEC737A);
            }
            handX=x-s+dx;
        } else if(message==CLOSED) {
            Kawaii.draw(p,Kawaii.BLOB,x,y,s*2,0xFF91CA43,1f,.4f);
            p.fillPoly(pill(x,y+s*2,s*2.6f,s*.35f,12),0xFF77A836);
            return;
        } else {
            face(p,key,x,y,s*1.65f);
            if(message==MATCH) {
                face(p,key,x-s*4.8f,y,s*1.1f);
                arrow(p,x-s*3.4f,y,s*1.2f,0,s*.5f);
            }
            if(message==DANGER)p.line(x-s*5,y+s*2,x+s*5,y+s*2,0xFFCC4C69,s*.22f);
        }
        Renderer.touchHint(p,handX,handY,s*1.3f,1f,.95f,age);
    }
}
