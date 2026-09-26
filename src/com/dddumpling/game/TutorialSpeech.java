package com.dddumpling.game;

/** The run companion demonstrates a control while the real scene waits. */
final class TutorialSpeech extends Draw {
    static final int MATCH=1, WORD=2, ALTERNATE=3, LIFT=4, STARS=5, LEAN=6,
            DIG=7, CART=8, CLOSED=9, CHAIN=10, GLOB=11, DANGER=12, RETRY=13,
            STACK=14, RESCUE=15, SUCCESS=16, PINCH=17, DEFEND=18, TEAR=19, SHAKE=20;
    private static final int PAPER=0xFFFFF5DD, TEXT_INK=0xFF302440, ACCENT=0xFF754070;
    private static final String[][] LINES={
        {"", ""}, {"MATCH THE FACE!", "TAP ITS KEY BELOW."},
        {"TAP KEYS IN ORDER!", "START ON THE LEFT."},
        {"LEFT, RIGHT!", "TAKE TURNS TAPPING."},
        {"LIFT THE LID!", "SWIPE UP."},
        {"CATCH THE STARS!", "SLIDE TO STEER."},
        {"SLIDE TO STEER!", "STAY IN THE GREEN."},
        {"TAP MATCHING KEYS!", "FILL THE CART."},
        {"THE CART IS FULL!", "SWIPE IT AWAY."},
        {"NO FACE SHOWING?", "WAIT FOR IT!"},
        {"TAP THE SHOWN KEY!", "GROW A GOOEY GLOB."},
        {"GRAB THE RED GLOB!", "DRAG IT OFFSCREEN."},
        {"QUICK, TAP!", "BEAT THE RED LINE."},
        {"OOPS! TRY AGAIN.", "START ON THE LEFT."},
        {"MORE THAN ONE!", "TAP THE KEY AGAIN."},
        {"NEED SOME SPACE?", "SWIPE THE BAR UP!"},
        {"YOU DID IT!", "LET'S PLAY!"},
        {"MATCH THE KEY!", "SPREAD TWO FINGERS."},
        {"SAVE YOUR KEYS!", "TAP THE SHOWN KEY."},
        {"GRAB THE ARM TIP!", "PULL IT AWAY."},
        {"GRAB THE CAP!", "SHAKE SIDE TO SIDE."}
    };
    static String spoken(int message) {
        if(message==CLOSED)return "If Slime hides its face, wait until it opens. Then you can tap the matching key.";
        if(message==PINCH)return "Tap the matching key. Then put two fingers on the cube and spread them apart to split it.";
        if(message==DEFEND)return "Octopulse reaches for your keys. Tap the matching key to defend it.";
        if(message==TEAR)return "When an arm tip is exposed, grab it and drag it away from Octopulse.";
        if(message==SHAKE)return "Grab Fly Agaric's cap. Keep holding it and shake it from side to side.";
        return (LINES[message][0]+" "+LINES[message][1]).toLowerCase(java.util.Locale.ROOT);
    }
    static float helpX(Layout L) { return RunCompanion.x(L)+L.unit*2.1f; }
    static float helpY(Layout L) { return RunCompanion.y(L)-L.unit*2.5f; }
    static boolean helpHit(Layout L,float x,float y) {
        return Math.abs(x-helpX(L))<L.unit*1.35f && Math.abs(y-helpY(L))<L.unit*1.35f;
    }
    static void help(Painter p,GameCore c,Layout L) {
        float s=L.unit,x=helpX(L),y=helpY(L);
        bubble(p,x-s,y-s,x+s,y+s,s*.55f);
        p.fillPoly(new float[]{x-s*.7f,y+s*.7f,x-s*.2f,y+s*.9f,x-s,y+s*1.6f},PAPER);
        p.text("?",x,y+s*.48f,type(s*1.25f),TEXT_INK,Painter.CENTER,true);
    }
    static float unit(Layout L) { return Math.min(L.unit,(L.deckTop-L.topSafe)/24f); }
    static float top(Layout L) { return L.topSafe+unit(L)*7f; }
    static float buttonY(Layout L) { return top(L)+unit(L)*12.5f; }
    static boolean buttonHit(Layout L,float x,float y) {
        return Math.abs(x-L.w*.5f)<L.w*.32f && Math.abs(y-buttonY(L))<unit(L)*1.3f;
    }
    static int speaker(GameCore c) { return c.companion.who>=0?c.companion.who:Math.max(0,c.runWho); }
    private static void bubble(Painter p,float l,float t,float r,float b,float radius) {
        float[] outline=new float[56];
        for(int corner=0;corner<4;corner++) {
            float cx=corner==0 || corner==3?r-radius:l+radius;
            float cy=corner<2?b-radius:t+radius;
            for(int j=0;j<=6;j++) {
                float a=(corner*90f+j*15f)*(float)Math.PI/180f;
                int i=(corner*7+j)*2;
                outline[i]=cx+radius*(float)Math.cos(a);outline[i+1]=cy+radius*(float)Math.sin(a);
            }
        }
        p.fillPoly(outline,PAPER);p.strokePoly(outline,TEXT_INK,radius*.14f);
    }
    static void large(Painter p,GameCore c,Layout L,int message,boolean button) {
        float s=unit(L),t=top(L),b=t+s*14.2f,x=L.w*.5f;
        float age=message==RESCUE?c.pushLesson.clock:c.onboarding.age;
        if(button)p.fillRect(0,0,L.w,L.h,0xB8101022);
        bubble(p,L.w*.05f,t,L.w*.95f,b,s*1.2f);
        // Opaque tail joins the bubble and points at this run's actual companion.
        float tx=L.w*.13f;
        p.fillPoly(new float[]{tx,t+s*.12f,tx+s*2,t+s*.12f,tx+s*.5f,t-s*1.1f},PAPER);
        p.polyline(new float[]{tx,t,tx+s*.5f,t-s*1.1f,tx+s*2,t},TEXT_INK,s*.17f);
        p.text(LINES[message][0],x,t+s*2.5f,type(s*1.10f),TEXT_INK,Painter.CENTER,true);
        p.text(LINES[message][1],x,t+s*4.6f,type(s*1.00f),ACCENT,Painter.CENTER,true);
        demonstrate(p,c,L,message,x,t+s*8.4f,s,age);
        if(button) {
            p.fillPoly(pill(x,buttonY(L),L.w*.32f,s*1.15f,14),0xFF387358);
            p.text(c.onboarding.moreBossHelp(c)?"NEXT":"LET'S TRY!",x,buttonY(L)+s*.38f,type(s*.92f),0xFFFFFFFF,Painter.CENTER,true);
        }
    }
    static void reminder(Painter p,GameCore c,Layout L,int message) {
        float s=unit(L),t=L.topSafe+s*2.6f,b=t+s*4f,l=L.w*.25f;
        bubble(p,l,t,L.w*.97f,b,s*.7f);
        p.fillPoly(new float[]{l+s*.1f,t+s*1.2f,l-s*.9f,b-s,l+s*.1f,b-s*.5f},PAPER);
        p.polyline(new float[]{l,t+s*1.2f,l-s*.9f,b-s,l,b-s*.5f},TEXT_INK,s*.1f);
        float x=(l+L.w*.97f)*.5f;
        p.text(LINES[message][0],x,t+s*1.5f,type(s*.70f),TEXT_INK,Painter.CENTER,true);
        p.text(LINES[message][1],x,t+s*3f,type(s*.70f),ACCENT,Painter.CENTER,true);
    }
    static void companion(Painter p,GameCore c,Layout L) {
        Onboarding o=c.onboarding;GameCore q=o.practice==null?c:o.practice;
        float u=o.companionTravel;u=u*u*(3-2*u);
        float x=RunCompanion.x(L),y=RunCompanion.y(L),r=RunCompanion.radius(c,L),s=unit(L);
        if(q.state==GameCore.BONUS && q.starBonus) {
            x=StarScreen.companionX(q,L,q.stars.flyerX(L));
            y=StarScreen.companionY(q,L,q.stars.flyerY(L));r=StarScreen.companionR(q,L);
        }
        x+=(L.w*.13f-x)*u;y+=(L.topSafe+s*4.9f-y)*u;r+=(s*1.6f-r)*u;
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
    private static void demonstrate(Painter p,GameCore c,Layout L,int message,float x,float y,float s,float age) {
        Onboarding o=c.onboarding;GameCore q=o.practice==null?c:o.practice;
        float travel=PushLesson.swipeProgress(age),handX=x,handY=y;
        int key=o.demoKey(c);
        if(message==PINCH) {
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
            handX=x+((int)(age*1.5f)%2==0?-3:3)*s;
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
            if(message==STACK)face(p,key,x+s*.45f,y-s*.45f,s*1.65f);
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
