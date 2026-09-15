package com.dddumpling.game;

/** Session-only starting-land selection; boss friends are the persistent unlocks. */
final class LandPicker extends Draw {
    private LandPicker() {}
    static final float TRAVEL_TIME = 0.85f;
    static boolean unlocked(GameCore c, int land) {
        return land == 0 || land > 0 && land < Lands.COUNT
                && (BuildFlags.DEVELOPER && c.allLandsEnabled || (c.landSuppressed & (1 << land)) == 0
                && Collect.has(c.collected, Collect.BOSS_FIRST + land - 1));
    }
    static int count(GameCore c) {
        int n = 0;
        for (int land = 0; land < Lands.COUNT; land++) if (unlocked(c, land)) n++;
        return n;
    }
    static boolean visible(GameCore c) {
        return c.state == GameCore.TITLE && count(c) > 1 && !c.caseOpen && c.caseFade < 0.01f
                && !c.storyOpen() && !c.starting() && !c.settingsOpen && c.rosterSceneT <= 0f;
    }
    static float cardY(Layout L) { return L.h * 0.705f; }
    // Adjacent centres differ by 30% of the full icon height.
    static float cardY(GameCore c, Layout L, int land) {
        return cardY(L) + (slot(c,land)%2==0 ? -0.3f : 0.3f)*iconRadius(c,L);
    }
    static int slot(GameCore c, int land) {
        int n = 0;
        for (int i = 0; i < land; i++) if (unlocked(c, i)) n++;
        return n;
    }
    static float spacing(GameCore c, Layout L) { return L.keyR * c.keyScale() * 2.40f; }
    static float iconRadius(GameCore c, Layout L) { return L.keyR * c.keyScale() * 0.72f; }
    static float cardX(GameCore c, Layout L, int land) {
        return L.w * 0.5f + (slot(c, land) - slot(c, c.landChoice) + c.landPickerSlide) * spacing(c, L);
    }
    private static int pendingLand(GameCore c) {
        return c.landTravelQueue.isEmpty() ? c.landChoice : c.landTravelQueue.get(c.landTravelQueue.size()-1);
    }
    static void select(GameCore c, int land) {
        if (!visible(c) || !unlocked(c,land) || c.landDiscovery>=0) return;
        int from=pendingLand(c);
        if(from==land) return;
        int direction=land>from ? 1 : -1;
        for(int next=from+direction;next!=land+direction;next+=direction)
            if(unlocked(c,next)) c.landTravelQueue.add(next);
        if(c.landTravelFrom<0) beginTravel(c);
    }
    private static void beginTravel(GameCore c) {
        if(c.landTravelQueue.isEmpty()) return;
        c.landTravelFrom=c.landChoice;
        c.landChoice=c.landTravelQueue.remove(0);
        c.landTravelT=0f;
        c.landPickerSlide=slot(c,c.landChoice)-slot(c,c.landTravelFrom);
        c.best=c.landBests[c.landChoice];
        if(c.sound!=null) c.sound.landShuffle();
    }
    static void updateTravel(GameCore c,float dt) {
        if(!visible(c)) {
            c.landTravelFrom=-1;c.landTravelQueue.clear();c.landPickerSlide=0f;
            c.landPickerDragging=false;
            return;
        }
        if(c.landTravelFrom<0) return;
        c.landTravelT=Math.min(TRAVEL_TIME,c.landTravelT+dt);
        float walk=ease((c.landTravelT/TRAVEL_TIME-0.16f)/0.68f);
        c.landPickerSlide=(slot(c,c.landChoice)-slot(c,c.landTravelFrom))*(1f-walk);
        if(c.landTravelT>=TRAVEL_TIME) {
            c.landTravelFrom=-1;c.landPickerSlide=0f;
            beginTravel(c);
        }
    }
    static void step(GameCore c, int direction) {
        if(direction==0) return;
        direction=direction<0 ? -1 : 1;
        for (int land = pendingLand(c) + direction; land >= 0 && land < Lands.COUNT; land += direction)
            if (unlocked(c, land)) { select(c, land); return; }
    }
    static boolean down(GameCore c, Layout L, float x, float y) {
        if (!visible(c) || c.returnFade > 0f || c.landDiscovery>=0) return false;
        if (Math.abs(y - cardY(L)) > L.h * 0.05f) return false;
        c.landPickerDragging = true; c.landPickerMoved = false; c.landPickerX = x;
        return true;
    }
    static void move(GameCore c, Layout L, float x) {
        if (!c.landPickerDragging || c.landPickerMoved || !visible(c)) return;
        float dx = x - c.landPickerX;
        if (Math.abs(dx) < spacing(c, L) * 0.65f) return;
        step(c, dx < 0 ? 1 : -1);
        c.landPickerX = x; c.landPickerMoved = true;
    }
    static void up(GameCore c, Layout L, float x, float y) {
        if (c.landPickerDragging && !c.landPickerMoved && visible(c)
                && Math.abs(y - cardY(L)) < L.h * 0.05f) {
            int nearest = -1;
            float distance = spacing(c, L) * 0.65f;
            for (int land = 0; land < Lands.COUNT; land++) {
                float dx = Math.abs(x - cardX(c, L, land));
                if (unlocked(c, land) && dx < distance) { nearest = land; distance = dx; }
            }
            if (nearest >= 0) select(c, nearest);
        }
        c.landPickerDragging = false;
    }
    static void recordBest(GameCore c) {
        int land = c.runStartLand;
        c.best = Math.max(c.best, c.score);
        c.landBests[land] = Math.max(c.landBests[land], c.best);
        if (c.store != null) c.store.saveLandBest(land, c.landBests[land]);
    }
    static void save(GameCore c) {
        if (c.store != null) c.store.saveLandState(c.landSeen | (c.landSuppressed << 4));
    }
    static void enableAll(GameCore c) {
        if (BuildFlags.DEVELOPER) c.allLandsEnabled=true;
    }
    static void reset(GameCore c) {
        if (!BuildFlags.DEVELOPER) return;
        c.allLandsEnabled = false;
        c.landSuppressed = 14;
        c.landSeen = 0;
        c.landDiscovery = -1;
        c.landDiscoveryT = c.landPickerSlide = 0f;
        c.landPickerDragging = false;
        c.landTravelFrom=-1;c.landTravelQueue.clear();c.landTravelT=0f;
        c.landChoice = 0;
        c.best = c.landBests[0];
        save(c);
    }
    static void reward(GameCore c, int character) {
        int land = character - Collect.BOSS_FIRST + 1;
        if (land <= 0 || land >= Lands.COUNT) return;
        c.landSuppressed &= ~(1 << land);
        save(c);
    }
    static void updateDiscovery(GameCore c,float dt) { LandDiscovery.update(c,dt); }
    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
    static float explorerX(GameCore c,Layout L) { return LandDiscovery.x(c,L); }
    static float discoveryReveal(GameCore c,Layout L,int land) { return LandDiscovery.reveal(c,L,land); }

    private static void adventure(Painter p,float x,float y,float r,int a,float reveal) {
        Skits.face(p, Kawaii.DUMPLING, x, y, r * 0.60f, a, 1f, reveal);
        // Soft felt crown, pinched top and a wide brim: an adventure hat.
        int felt = Glyph.withAlpha(0xFFC59A62, a), band = Glyph.withAlpha(0xFF795840, a);
        p.fillPoly(new float[]{x-r*.48f,y-r*.42f,x-r*.34f,y-r*.92f,
                x-r*.08f,y-r*.84f,x+r*.25f,y-r*.96f,x+r*.43f,y-r*.42f}, felt);
        p.fillEllipse(x, y-r*.47f, r*.46f, r*.10f, band);
        p.fillEllipse(x, y-r*.39f, r*.76f, r*.12f, felt);
    }
    static float travelX(GameCore c,Layout L) {
        float t=c.landTravelT/TRAVEL_TIME;
        float walk=ease((t-0.16f)/0.68f);
        if(c.landChoice<c.landTravelFrom)
            return trailX(c,L,slot(c,c.landChoice),reverseTrailProgress(c,L,walk));
        float direction=c.landChoice>c.landTravelFrom ? 1f : -1f;
        float start=cardX(c,L,c.landTravelFrom)-direction*iconRadius(c,L)*0.95f;
        return start+(cardX(c,L,c.landChoice)-start)*walk;
    }
    static float travelGround(GameCore c,Layout L) {
        float walk=ease((c.landTravelT/TRAVEL_TIME-0.16f)/0.68f);
        if(c.landChoice<c.landTravelFrom) {
            float t=reverseTrailProgress(c,L,walk);
            float left=cardY(c,L,c.landChoice),right=cardY(c,L,c.landTravelFrom);
            return left+(right-left)*t;
        }
        float from=cardY(c,L,c.landTravelFrom),to=cardY(c,L,c.landChoice);
        return from+(to-from)*walk;
    }
    static float travelArc(GameCore c,Layout L) {
        float walk=ease((c.landTravelT/TRAVEL_TIME-0.16f)/0.68f);
        if(c.landChoice<c.landTravelFrom) walk=reverseTrailProgress(c,L,walk);
        return walkingDip(walk)*iconRadius(c,L);
    }
    static float travelerRadius(GameCore c,Layout L) { return L.keyR*c.keyScale()*.55f; }
    static void drawJourney(Painter p,GameCore c,Layout L,float x,float base,float arc,
            float walk,float pop,float fade,float time) {
        float r=travelerRadius(c,L);
        float stride=(float)Math.sin(time*Softbody.TAU*5f)*(walk>0f && walk<1f ? 1f : 0f);
        float ground=base+iconRadius(c,L)*1.10f+r*.07f+arc;
        float y=ground-r*.70f*pop-Math.abs(stride)*r*.08f;
        int alpha=(int)(255*pop*fade);
        p.fillEllipse(x,ground,r*.60f,r*.10f,Glyph.withAlpha(INK,(int)(alpha*.18f)));
        for(int side=-1;side<=1;side+=2)
            p.fillEllipse(x+side*r*.25f+stride*side*r*.14f,y+r*.53f,
                    r*.19f,r*.10f,Glyph.withAlpha(0xFF795840,alpha));
        adventure(p,x,y,r,alpha,.7f);
    }
    private static void drawTravel(Painter p,GameCore c,Layout L) {
        if(c.landTravelFrom<0) return;
        float t=c.landTravelT/TRAVEL_TIME;
        drawJourney(p,c,L,travelX(c,L),travelGround(c,L),travelArc(c,L),
                ease((t-.16f)/.68f),ease(t/.16f),Math.min(1f,(1f-t)/.14f),t);
    }

    static float walkingDip(float t) {
        if(t<=0f || t>=1f) return 0f;
        return (float)(.45f*Math.sin(Math.PI*t)-.35f*Math.sin(2f*Math.PI*t));
    }
    static float trailX(GameCore c,Layout L,int segment,float t) {
        float gap=spacing(c,L),left=cardX(c,L,0)+segment*gap;
        float start=left-iconRadius(c,L)*.95f;
        return start+(left+gap-start)*t;
    }
    static float trailY(GameCore c,Layout L,int segment,float t) {
        float r=iconRadius(c,L);
        float from=cardY(L)+(Math.floorMod(segment,2)==0 ? -.3f : .3f)*r;
        float to=cardY(L)+(Math.floorMod(segment+1,2)==0 ? -.3f : .3f)*r;
        return from+(to-from)*t+r*1.10f-travelerRadius(c,L)*.10f+walkingDip(t)*r;
    }
    static boolean trailCovered(GameCore c,Layout L,int land,float x,float y) {
        float r=iconRadius(c,L),dot=r*.09f;
        float width=land==0 ? 1.6f : land==1 ? 1.15f : land==2 ? .95f : 1.25f;
        float bottom=land==0 ? .9f : land==1 ? 1.08f : land==2 ? 1.12f : 1.38f;
        // Reserve the full land silhouette, including the dot's radius, through focus changes.
        return Math.abs(x-cardX(c,L,land))<=r*width+dot
                && y>=cardY(c,L,land)-r*1.8f-dot
                && y<=cardY(c,L,land)+r*bottom+dot;
    }
    private static float reverseTrailProgress(GameCore c,Layout L,float walk) {
        float lead=iconRadius(c,L)*.95f;
        float end=lead/(spacing(c,L)+lead);
        return 1f+(end-1f)*walk;
    }
    private static void drawTrail(Painter p,GameCore c,Layout L) {
        float gap=spacing(c,L),r=iconRadius(c,L);
        int n=count(c);
        for(int segment=0;segment<n;segment++) {
            for(int dot=1;dot<13;dot++) {
                float t=dot/13f;
                if(segment==n-1 && t>.55f) continue;
                float x=trailX(c,L,segment,t),y=trailY(c,L,segment,t);
                if(x<cardX(c,L,0)) continue;
                float reveal=1f;
                boolean covered=false;
                for(int land=0;land<Lands.COUNT;land++) {
                    if(!unlocked(c,land)) continue;
                    int index=slot(c,land);
                    if(index==segment || index==segment+1)
                        reveal=Math.min(reveal,discoveryReveal(c,L,land));
                    if(trailCovered(c,L,land,x,y)) covered=true;
                }
                if(covered || reveal<=0f) continue;
                float edge=segment==n-1 ? (.6f-t)/.35f : 1f;
                float alpha=105f*reveal*Math.min(1f,edge);
                p.fillCircle(x,y,r*.090f,Glyph.withAlpha(INK_DIM,(int)alpha));
            }
        }
    }

    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        float cy = cardY(L);
        float halfHeight=Math.max(L.h*0.063f,iconRadius(c,L)*2.35f);
        p.save(); p.clipRect(0, cy-halfHeight, L.w, cy+halfHeight);
        drawTrail(p,c,L);
        // Back to front: the focused emblem covers the inner edges of its neighbours.
        for (int distance = Lands.COUNT - 1; distance >= 0; distance--)
        for (int land = 0; land < Lands.COUNT; land++) {
            if (!unlocked(c, land) || Math.abs(slot(c, land) - slot(c, c.landChoice)) != distance) continue;
            float x = cardX(c, L, land);
            float stepsAway = Math.abs(x - L.w * 0.5f) / spacing(c, L);
            float focus = Math.max(0f, 1f - stepsAway);
            float reveal = discoveryReveal(c, L, land);
            if (reveal <= 0f) continue;
            if (land == c.landDiscovery) focus += (1f-focus)*reveal;
            float r = iconRadius(c, L) * (0.72f + focus * 0.28f);
            float y = cardY(c,L,land);
            LandDiscovery.drawGlow(p,c,land,x,y,r);
            // The neighbouring emblems are blurred silhouettes, becoming clear as they centre.
            float falloff = (float)Math.pow(0.45f, Math.max(0f, stepsAway - 1f));
            int haze = (int)(80 * (1f - focus) * falloff * reveal);
            Lands.logo(p, land, x, y, r * 1.20f, haze / 8, c.clock, true);
            Lands.logo(p, land, x, y, r * 1.12f, haze / 5, c.clock, true);
            Lands.logo(p, land, x, y, r * 1.05f, haze / 3, c.clock, true);
            Lands.logo(p, land, x, y, r, haze / 2, c.clock, true);
            if (focus > 0f) Lands.logo(p, land, x, y, r, (int)(235 * focus * reveal), c.clock);
        }
        p.restore();
        LandDiscovery.draw(p,c,L);
        drawTravel(p,c,L);
    }
}
