package com.dddumpling.game;

/** Portrait town and slide presentation. Input geometry lives beside what it draws. */
final class TownScreen extends Draw {
    private static final int SKY = 0xFFCEF0D1, FAR = 0xFF9FD59E, MEADOW = 0xFF79C883,
            WOOD = 0xFF936A48,
            TOWN_INK = 0xFF294C3C, CREAM = 0xFFFFF4D7, PANEL = 0xF7FFF9E9;

    private TownScreen() { }

    static void draw(Painter p, GameCore c, Layout L) {
        if (c.town.mode == Town.SLIDE) drawSlide(p, c, L);
        else if (c.town.mode == Town.FIGHT) SlimeFightScreen.draw(p, c.town.fight, L);
        else drawMeadow(p, c, L);
        if (c.town.mode == Town.FIGHT) closeButton(p,L,TOWN_INK);
        float wash=c.townClosing ? c.townExitFade : c.townReveal;
        if(wash>0f) p.fillRect(0,0,L.w,L.h,Glyph.withAlpha(SKY,(int)(255*wash/GameCore.TOWN_FADE)));
    }

    private static void drawMeadow(Painter p, GameCore c, Layout L) {
        Town t = c.town;
        float w = L.w;
        TownScenery.background(p, L, cameraX(t,L), c.clock);
        p.save();
        p.clipRect(0, 0, L.w, L.deckTop);
        p.translate(-cameraX(t, L), 0);
        TownScenery.ground(p, t, L, c.clock);
        TownScenery.treesAndBalloons(p,t,L,c.clock);
        drawPlots(p, L);
        drawPath(p, L);

        TownAttractions.friend(p, t, L, c.clock);
        TownAttractions.slide(p, t, L, c.clock);
        if (t.slimeBossOwned) TownAttractions.slime(p, t, L, c.clock);

        float px = pathX(t.path, L), py = pathY(t.path, L);
        float speed = Math.abs(t.targetPath >= 0 ? t.targetPath - t.path : t.steer);
        float hop = (float) Math.abs(Math.sin(t.bounceClock)) * w * (.008f + .018f * Math.min(1, speed * 3));
        TownPlayer.draw(p, c, L, px, py, hop, speed);
        p.save();
        p.translate(-cameraX(t,L)*(TownScenery.FOREGROUND_SCROLL-1f),0);
        TownScenery.foreground(p, t, L, c.clock);
        p.restore();
        if (t.touchAge < .6f) {
            float age = t.touchAge/.6f;
            p.strokeCircle(t.touchX,t.touchY,L.w*(.015f+.055f*age),
                    Glyph.withAlpha(0xFFFFF7C7,(int)(150*(1f-age))),L.w*.003f);
        }
        p.restore();
        header(p, t, L);
        slider(p, t, L, c.clock);
        if (t.dialogue != Town.TALK_NONE) dialogue(p, t, L);
    }

    private static void drawPath(Painter p, Layout L) {
        float[] path = route(L);
        p.polyline(path, 0x553F754D, L.w * .125f);
        p.polyline(path, 0xFFD2B07B, L.w * .108f);
        p.polyline(path, 0xFFFFE8B2, L.w * .098f);
        p.polyline(path, 0xFFF1D39D, L.w * .080f);
        for (int i=0;i<57;i++) {
            float at=(i+.5f)/57f;
            float x=pathX(at,L),y=pathY(at,L)+L.w*.018f*(hash(i*19+4)-.5f);
            float r=L.w*(.008f+.012f*hash(i*31+9));
            p.fillEllipse(x,y,r,r*.52f,0x42C39760);
            p.arc(x,y-r*.12f,r,r*.52f,195,115,0x60FFF1CE,L.w*.002f);
        }
    }

    private static float[] route(Layout L) {
        float[] out = new float[162];
        for (int i = 0; i <= 80; i++) {
            float at = i / 80f;
            out[i * 2] = pathX(at, L);
            out[i * 2 + 1] = pathY(at, L);
        }
        return out;
    }

    static float pathX(float at, Layout L) { return L.w * (.15f + 2.7f * at); }

    // World coordinates are shared by scenery and taps; the UI stays in screen coordinates.
    static float cameraX(Town t, Layout L) {
        return Math.max(0f, Math.min(L.w * 2f, pathX(t.path, L) - L.w * .5f));
    }

    static float screenPathX(Town t, float at, Layout L) {
        return pathX(at, L) - cameraX(t, L);
    }

    static float balloonAnchorY(int index,Layout L) {
        float x=L.w*(.13f+index*.195f);
        return meadowTop(x,L)+L.w*(.025f+.095f*hash(index*37+717));
    }

    static void balloon(Painter p, Town t, Layout L, float clock, int i) {
        String letters = "DDDUMPLING TOWN";
        float size = L.w * .26f;
        char ch = letters.charAt(i);
        if (ch == ' ') return;
        float anchor = L.w * (.13f + i * .195f);
        float phase = clock * 1.2f + i * .73f;
        float x = anchor + L.w * .006f * (float) Math.sin(phase);
        float y = L.playTop + (L.deckTop - L.playTop) * .24f
                + L.w * .035f * (float) Math.sin(phase * .8f);
        float ground = balloonAnchorY(i,L);
        float kick = t.motion(anchor, y - size*.4f, L.w*.23f)
                + .7f*t.motion(anchor,(y+ground)*.5f,L.w*.12f)
                + .6f*t.motion(anchor,ground,L.w*.12f);
        kick = Math.max(-.8f,Math.min(.8f,kick));
        y -= kick * size * .72f;
        x += kick * size * .22f;
        p.polyline(new float[]{x,y,x + size*.10f,y+(ground-y)*.35f,
                anchor-size*.08f,y+(ground-y)*.72f,anchor,ground}, 0xB36D7858, L.w*.002f);
        p.line(anchor, ground - size*.06f, anchor, ground + size*.08f, WOOD, L.w*.005f);
        int color = Glyph.COLOR[i % Glyph.COUNT];
        p.fillPoly(new float[]{x,y-size*.02f,x-size*.06f,y+size*.065f,
                x+size*.06f,y+size*.065f},color);
        TitleBubbleFont.draw(p,ch,x,y,size,color,1f,phase,1f+kick*.15f);
    }

    static float meadowTop(float worldX, Layout L) {
        float phase=worldX/L.w;
        return L.playTop + (L.deckTop-L.playTop)*(.515f
                + .025f*(float)Math.sin(phase*3.6f+.35f)
                + .012f*(float)Math.sin(phase*7.1f+1.2f));
    }

    static float landmarkGround(float at, Layout L) {
        float height=at<.4f?.67f:at<.78f?.82f:.68f;
        return L.playTop+(L.deckTop-L.playTop)*height;
    }

    static boolean inPlot(float x,float y,float margin,Layout L) {
        for(float at:new float[]{.20f,.66f,.88f}) {
            float dx=(x-pathX(at,L))/(L.w*.19f+margin);
            float dy=(y-landmarkGround(at,L)-L.w*.025f)/(L.w*.12f+margin);
            if(dx*dx+dy*dy<1f)return true;
        }
        return false;
    }

    // Broad alternating bends leave sheltered plots on both sides of the walking route.
    private static final float[] ROUTE_AT={0f,.08f,.20f,.34f,.48f,.66f,.77f,.88f,1f};
    private static final float[] ROUTE_HEIGHT={.70f,.75f,.86f,.69f,.84f,.64f,.70f,.87f,.76f};

    static float pathY(float at, Layout L) {
        at=Math.max(0f,Math.min(1f,at));
        int segment=0;
        while(segment<ROUTE_AT.length-2&&at>ROUTE_AT[segment+1])segment++;
        float t=(at-ROUTE_AT[segment])/(ROUTE_AT[segment+1]-ROUTE_AT[segment]);
        float eased=t*t*(3f-2f*t);
        float height=ROUTE_HEIGHT[segment]+(ROUTE_HEIGHT[segment+1]-ROUTE_HEIGHT[segment])*eased;
        return L.playTop+(L.deckTop-L.playTop)*height;
    }

    private static void drawPlots(Painter p,Layout L) {
        for(float at:new float[]{.20f,.66f,.88f}) {
            float x=pathX(at,L),y=landmarkGround(at,L)+L.w*.025f;
            p.fillEllipse(x,y,L.w*.20f,L.w*.125f,0xFF499C62);
            p.fillEllipse(x,y-L.w*.009f,L.w*.19f,L.w*.115f,0xFF83BD75);
            p.fillEllipse(x-L.w*.025f,y-L.w*.026f,L.w*.15f,L.w*.077f,0xFF92C67D);
            // A short entry connects each plot to its nearest point on the meandering path.
            float start=pathY(at,L),end=y+(start>y?1:-1)*L.w*.08f;
            float[] spur={x,start,x-L.w*.025f,(start+end)*.5f,x,end};
            p.polyline(spur,0xFFD2B07B,L.w*.049f);
            p.polyline(spur,0xFFF1D39D,L.w*.036f);
        }
    }

    private static void header(Painter p, Town t, Layout L) {
        float s = L.unit;
        p.fillPoly(pill(L.w * .5f, L.topSafe + s * 1.25f, L.w * .20f, s * .55f, 12),
                0xD9FFF9E9);
        p.fillPoly(star(L.w * .39f, L.topSafe + s * 1.22f, s * .31f, s * .14f, 5, 0),
                0xFFFFCE4A);
        p.text(t.tickets + " TICKETS", L.w * .53f, L.topSafe + s * 1.38f,
                type(s * .43f), 0xFF6D7137, Painter.CENTER, true);
        closeButton(p, L, TOWN_INK);
        float gy=flowerChipY(L);
        p.fillPoly(pill(L.w*.5f,gy,L.w*.29f,s*.55f,12),0xEFFFF7DB);
        p.text("FLOWERS: "+t.flowerGrowthName(),L.w*.5f,gy+s*.14f,
                type(s*.38f),TOWN_INK,Painter.CENTER,true);
    }

    static float flowerChipY(Layout L) { return L.topSafe+L.unit*2.65f; }

    private static void closeButton(Painter p, Layout L, int color) {
        float x = L.w - L.padR - L.unit * 1.25f, y = L.topSafe + L.unit * 1.15f, r = L.unit * .62f;
        p.fillCircle(x, y, r, 0xBFFFFFF0);
        p.line(x - r * .34f, y - r * .34f, x + r * .34f, y + r * .34f, color, r * .14f);
        p.line(x + r * .34f, y - r * .34f, x - r * .34f, y + r * .34f, color, r * .14f);
    }

    private static void slider(Painter p, Town t, Layout L, float clock) {
        p.fillRect(0, L.deckTop, L.w, L.h, 0xD8E6F0D3);
        float knob = StarScreen.sliderLeft(L) + (t.steer + 1f) * .5f
                * (StarScreen.sliderRight(L) - StarScreen.sliderLeft(L));
        StarScreen.slider(p, knob, L, 1f, clock);
        p.text("BOUNCE AROUND", L.w * .5f, L.deckTop + L.unit * .60f, type(L.unit * .43f),
                TOWN_INK, Painter.CENTER, true);
    }

    private static void dialogue(Painter p,Town t,Layout L){
        float s=L.unit,l=L.w*.07f,r=L.w*.93f,b=L.deckTop-s*.75f,top=b-s*7.2f;
        p.fillPoly(pageShape(l,top,r,b,s*.8f),PANEL);
        p.strokePoly(pageShape(l,top,r,b,s*.8f),0x805D8B62,s*.10f);
        String title,one,two,button;
        if(t.dialogue==Town.TALK_WELCOME){title="WELCOME TO TOWN!";one="Bounce along the path or tap a place.";two="The slide is open. These tickets are for decorating!";button="LET'S EXPLORE";}
        else if(t.dialogue==Town.TALK_FRIEND){title="A LITTLE SECRET";one="Flowers bloom while you spend time here.";two="Come back and the meadow will keep growing!";button="GOT IT";}
        else if(t.dialogue==Town.TALK_BUILD_ARCH){title="A FLOWERY WELCOME";one="Build an arch for this friendly corner?";two="It costs " + Town.ARCH_COST + " tickets.";button=t.tickets>=Town.ARCH_COST?"BUILD - "+Town.ARCH_COST+" TICKETS":"NEED "+(Town.ARCH_COST-t.tickets)+" MORE";}
        else if(t.dialogue==Town.TALK_ARCH_BUILT){title="SO MANY FLOWERS!";one="The new arch makes a lovely meeting place.";two="More flowers will bloom as you visit.";button="HOORAY";}
        else {title="SLIME FIGHT!";one="Throw slime, dodge slime, and be silly.";two="Every splat adds to the FUN.";button="PLAY!";}
        p.text(title,L.w*.5f,top+s*1.35f,type(s*.66f),TOWN_INK,Painter.CENTER,true);
        p.text(one,L.w*.5f,top+s*2.75f,type(s*.48f),TOWN_INK,Painter.CENTER,false);
        p.text(two,L.w*.5f,top+s*3.78f,type(s*.48f),TOWN_INK,Painter.CENTER,false);
        float by=b-s*1.12f;
        p.fillPoly(pill(L.w*.5f,by,L.w*.29f,s*.80f,12),0xFF69B978);
        p.text(button,L.w*.5f,by+s*.23f,type(s*.54f),CREAM,Painter.CENTER,true);
    }

    private static void drawSlide(Painter p,GameCore c,Layout L){
        Town t=c.town;float w=L.w,h=L.h,ground=L.playTop+(L.deckTop-L.playTop)*.78f;
        p.fillRect(0,0,w,h,SKY);
        p.fillEllipse(w*.12f,L.playTop+h*.24f,w*.62f,h*.25f,FAR);
        p.fillEllipse(w*.90f,L.playTop+h*.28f,w*.72f,h*.24f,0xFFABDCA5);
        p.fillRect(0,ground,w,L.h,MEADOW);
        slideAttraction(p,L,ground);
        closeButton(p,L,TOWN_INK);
        p.text("LAUNCH SLIDE",w*.5f,L.topSafe+L.unit*1.28f,type(L.unit*.76f),TOWN_INK,Painter.CENTER,true);
        float x,y,r=w*.070f;
        if(t.slideFlying||t.slideLanded){x=t.slideX*w;y=L.playTop+t.slideY*(L.deckTop-L.playTop);}
        else {x=w*.17f;y=L.playTop+(L.deckTop-L.playTop)*.20f-r
                -(float)Math.sin(c.clock*3)*w*.006f;}
        p.fillEllipse(x,y+r*.75f,r*.75f,r*.17f,0x25446E43);
        Trinket.draw(p,t.rider,x,y,r,c.clock,t.rider==0||t.owns(t.rider),1f);
        if(t.slideAirborne){
            for(int i=0;i<5;i++){float trail=i*w*.035f;
                p.fillPoly(star(x-trail,y+trail*.28f,w*.016f,w*.007f,5,c.clock+i),Glyph.withAlpha(GOLD,180-i*28));}
        }
        if(t.slideLanded){
            p.text("WHEEEE!",w*.5f,L.playTop+(L.deckTop-L.playTop)*.20f,type(L.unit*.95f),0xFFDB7A48,Painter.CENTER,true);
            p.text("DISTANCE " + Math.max(0, Math.round((t.slideX-.48f)*100f)) + "   BEST " + t.slideBest,
                    w*.5f,L.playTop+(L.deckTop-L.playTop)*.30f,type(L.unit*.48f),TOWN_INK,Painter.CENTER,true);
            actionButton(p,L,"AGAIN",.67f);actionButton(p,L,"TOWN",.86f);
        }else{
            float by=L.deckTop+L.unit*2.2f;
            p.fillPoly(pill(w*.5f,by,w*.31f,L.unit*.95f,14),t.slideHolding?0xFFFFA95F:0xFF69B978);
            p.text(t.slideFlying?"WHEEEE!":t.slideHolding?"LET GO!":"HOLD TO LAUNCH",w*.5f,by+L.unit*.24f,type(L.unit*.57f),CREAM,Painter.CENTER,true);
            p.fillPoly(pill(w*.5f,L.deckTop-L.unit*1.1f,w*.30f,L.unit*.35f,12),0x94FFFFFF);
            p.fillPoly(pill(w*(.20f+.30f*t.slideCharge),L.deckTop-L.unit*1.1f,w*.30f*t.slideCharge,L.unit*.29f,12),0xFFFFCE4A);
        }
        p.text("TAP RIDER TO SWITCH",w*.5f,ground+L.unit*1.05f,type(L.unit*.39f),TOWN_INK,Painter.CENTER,true);
    }

    private static void slideAttraction(Painter p,Layout L,float ground){
        float field=L.deckTop-L.playTop,w=L.w;
        float[] track=new float[66];
        for(int i=0;i<=32;i++){
            float at=i/32f;
            track[i*2]=Town.slideTrackX(at)*w;
            track[i*2+1]=L.playTop+Town.slideTrackY(at)*field;
        }
        float topX=track[0],topY=track[1],exitX=track[64],exitY=track[65];
        int wood=0xFFC88A5A,edge=0xFF8E6045,chute=0xFFFFB56D;
        p.line(topX-w*.045f,ground,topX-w*.045f,topY,wood,w*.020f);
        p.line(topX+w*.045f,ground,topX+w*.045f,topY,wood,w*.020f);
        for(int i=1;i<=5;i++){
            float y=topY+(ground-topY)*i/6f;
            p.line(topX-w*.042f,y,topX+w*.042f,y,edge,w*.011f);
        }
        p.line(w*.31f,ground,w*.31f,L.playTop+Town.slideTrackY(.58f)*field,wood,w*.022f);
        p.line(exitX-w*.02f,ground,exitX-w*.02f,exitY+w*.006f,wood,w*.018f);
        p.polyline(track,edge,w*.065f);
        p.polyline(track,chute,w*.043f);
        p.fillPoly(new float[]{topX-w*.07f,topY+w*.03f,topX-w*.07f,topY-w*.09f,
                topX+w*.07f,topY-w*.09f,topX+w*.07f,topY+w*.03f},0xFFFFE7A8);
        p.fillPoly(star(topX,topY-w*.03f,w*.026f,w*.012f,5,0),GOLD);
        // A bright lip makes the upward curl and its takeoff point readable.
        p.arc(exitX-w*.022f,exitY-w*.012f,w*.045f,w*.035f,15,125,0xFFFFE3A0,w*.010f);
    }

    private static void actionButton(Painter p,Layout L,String text,float y){
        float cy=L.playTop+(L.deckTop-L.playTop)*y;
        p.fillPoly(pill(L.w*.5f,cy,L.w*.27f,L.unit*.78f,12),0xFF69B978);
        p.text(text,L.w*.5f,cy+L.unit*.22f,type(L.unit*.55f),CREAM,Painter.CENTER,true);
    }

    /** Unified native input route. Actions match Android: down 0, up 1, move 2, cancel 3. */
    static boolean touch(GameCore c,Layout L,int action,int id,float x,float y){
        Town t=c.town;
        if(t.mode==Town.FIGHT){
            if(action==0 && hitClose(L,x,y)){t.finishSlimeFight();return true;}
            int result=t.fight.touch(L,action,id,x,y);
            if(result==SlimeFight.AGAIN)t.fight.begin(L);
            else if(result==SlimeFight.EXIT)t.finishSlimeFight();
            return result!=SlimeFight.IGNORE;
        }
        if(action==3){cancel(c,L);return true;}
        if(action==0){
            if(t.pointer>=0)return false;
            boolean handled=down(c,L,x,y);
            if(handled && c.townOpen)t.pointer=id;
            return handled;
        }
        if(id!=t.pointer)return false;
        if(action==2)return move(c,L,x,y);
        if(action==1){boolean handled=up(c,L,x,y);t.pointer=-1;return handled;}
        return false;
    }

    static boolean down(GameCore c,Layout L,float x,float y){
        Town t=c.town;
        if(hitClose(L,x,y)){
            if(t.mode==Town.SLIDE)t.finishSlide(); else c.requestCloseTown();
            return true;
        }
        if(t.mode==Town.SLIDE)return slideDown(t,L,x,y);
        if(t.dialogue!=Town.TALK_NONE){
            float b=L.deckTop-L.unit*.75f,by=b-L.unit*1.12f;
            if(Math.abs(x-L.w*.5f)<=L.w*.35f&&Math.abs(y-by)<=L.unit*1.25f){dialogueAction(t,L);return true;}
            return true;
        }
        if(Math.abs(x-L.w*.5f)<=L.w*.29f&&Math.abs(y-flowerChipY(L))<=L.unit*.60f){
            t.cycleFlowerGrowth();return true;
        }
        if(StarScreen.inSlider(L,x,y)){t.dragging=true;drag(t,L,x);return true;}
        if(y>=L.playTop&&y<L.deckTop)t.react(x+cameraX(t,L),y);
        int poi=poiAt(t,L,x,y);
        if(poi!=Town.NONE){t.travelTo(poi);return true;}
        return true;
    }

    static boolean move(GameCore c,Layout L,float x,float y){
        Town t=c.town;
        if(t.mode==Town.MEADOW&&t.dragging){drag(t,L,x);return true;}
        return t.mode==Town.SLIDE&&t.slideHolding;
    }

    static boolean up(GameCore c,Layout L,float x,float y){
        Town t=c.town;
        if(t.mode==Town.SLIDE&&t.slideHolding){t.releaseSlide();return true;}
        if(t.dragging){t.dragging=false;t.steer=0;return true;}
        return t.mode==Town.SLIDE;
    }

    static void cancel(GameCore c){cancel(c,null);}
    static void cancel(GameCore c,Layout L){
        Town t=c.town;
        t.dragging=false;t.steer=0;t.slideHolding=false;t.pointer=-1;
        t.fight.cancelInput();
    }

    /** Back peels one town layer at a time before closing the gate. */
    static boolean back(GameCore c){
        Town t=c.town;
        if(t.dialogue!=Town.TALK_NONE){t.dialogue=Town.TALK_NONE;return true;}
        if(t.mode==Town.FIGHT){t.finishSlimeFight();return true;}
        if(t.mode==Town.SLIDE){t.finishSlide();return true;}
        c.requestCloseTown();return true;
    }

    private static boolean slideDown(Town t,Layout L,float x,float y){
        float riderX=t.slideFlying||t.slideLanded?t.slideX*L.w:L.w*.17f;
        float riderY=t.slideFlying||t.slideLanded?L.playTop+t.slideY*(L.deckTop-L.playTop)
                :L.playTop+(L.deckTop-L.playTop)*.20f-L.w*.070f;
        if(!t.slideFlying&&!t.slideLanded&&Math.abs(x-riderX)<L.w*.13f&&Math.abs(y-riderY)<L.w*.16f){t.nextRider();return true;}
        if(t.slideLanded){
            float again=L.playTop+(L.deckTop-L.playTop)*.67f,meadow=L.playTop+(L.deckTop-L.playTop)*.86f;
            if(Math.abs(y-again)<L.unit*1.2f){t.beginSlide();return true;}
            if(Math.abs(y-meadow)<L.unit*1.2f){t.finishSlide();return true;}
        }else if(y>=L.deckTop-L.unit*.2f){t.holdSlide();return true;}
        return true;
    }

    private static void dialogueAction(Town t,Layout L){
        if(t.dialogue==Town.TALK_BUILD_ARCH){t.buyArch();return;}
        if(t.dialogue==Town.TALK_SLIME){t.beginSlimeFight(L);return;}
        t.dialogue=Town.TALK_NONE;
    }

    private static void drag(Town t,Layout L,float x){
        float amount=(x-StarScreen.sliderLeft(L))/(StarScreen.sliderRight(L)-StarScreen.sliderLeft(L))*2f-1f;
        t.steer(amount);
    }

    private static int poiAt(Town t,Layout L,float x,float y){
        int[] pois=t.slimeBossOwned?new int[]{Town.FRIEND,Town.SLIDE_POI,Town.SLIME_POI}:new int[]{Town.FRIEND,Town.SLIDE_POI};
        for(int poi:pois){float at=poi==Town.FRIEND?.20f:poi==Town.SLIDE_POI?.66f:.88f;
            float px=screenPathX(t,at,L),py=landmarkGround(at,L)-L.w*.07f;
            if(Math.abs(x-px)<L.w*.15f&&Math.abs(y-py)<L.w*.22f)return poi;}
        return Town.NONE;
    }

    private static boolean hitClose(Layout L,float x,float y){
        float cx=L.w-L.padR-L.unit*1.25f,cy=L.topSafe+L.unit*1.15f;
        return Math.abs(x-cx)<L.unit*1.2f&&Math.abs(y-cy)<L.unit*1.2f;
    }
}
