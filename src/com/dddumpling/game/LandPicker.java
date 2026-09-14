package com.dddumpling.game;

/** Session-only starting-land selection; boss friends are the persistent unlocks. */
final class LandPicker extends Draw {
    private LandPicker() {}
    static final float TRAVEL_TIME = 0.85f;
    static boolean unlocked(GameCore c, int land) {
        return land == 0 || land > 0 && land < Lands.COUNT
                && (c.landSuppressed & (1 << land)) == 0
                && Collect.has(c.collected, Collect.BOSS_FIRST + land - 1);
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
    static int slot(GameCore c, int land) {
        int n = 0;
        for (int i = 0; i < land; i++) if (unlocked(c, i)) n++;
        return n;
    }
    static float spacing(GameCore c, Layout L) { return L.keyR * c.keyScale() * 1.65f; }
    static float iconRadius(GameCore c, Layout L) { return L.keyR * c.keyScale() * 0.72f; }
    static float cardX(GameCore c, Layout L, int land) {
        return L.w * 0.5f + (slot(c, land) - slot(c, c.landChoice) + c.landPickerSlide) * spacing(c, L);
    }
    private static int pendingLand(GameCore c) {
        return c.landTravelQueue.isEmpty() ? c.landChoice : c.landTravelQueue.get(c.landTravelQueue.size()-1);
    }
    static void select(GameCore c, int land) {
        if (!visible(c) || !unlocked(c,land)) return;
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
        if (!visible(c) || c.returnFade > 0f) return false;
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
    private static void save(GameCore c) {
        if (c.store != null) c.store.saveLandState(c.landSeen | (c.landSuppressed << 4));
    }
    static void reset(GameCore c) {
        if (!BuildFlags.DEVELOPER) return;
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
    static void updateDiscovery(GameCore c, float dt) {
        if (!visible(c) || c.returnFade > 0f || c.landTravelFrom>=0) return;
        if (c.landDiscovery < 0) {
            for (int land = 1; land < Lands.COUNT; land++) {
                if (unlocked(c, land) && (c.landSeen & (1 << land)) == 0) {
                    c.landDiscovery = land;
                    c.landDiscoveryT = 0f;
                    break;
                }
            }
        }
        if (c.landDiscovery < 0) return;
        c.landDiscoveryT += dt;
        if (c.landDiscoveryT >= 4.5f) {
            c.landSeen |= 1 << c.landDiscovery;
            c.landDiscovery = -1;
            save(c);
        }
    }
    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
    static float explorerX(GameCore c, Layout L) {
        float r = L.keyR * c.keyScale();
        float target = cardX(c, L, c.landDiscovery) - r * 2.6f;
        return -r * 2f + (target + r * 2f) * ease(c.landDiscoveryT / 1.6f);
    }
    static float discoveryReveal(GameCore c, Layout L, int land) {
        if (land == 0 || (c.landSeen & (1 << land)) != 0) return 1f;
        if (land != c.landDiscovery) return 0f;
        float r = L.keyR * c.keyScale();
        float distance = cardX(c, L, land) - explorerX(c, L);
        return ease((r * 4.5f - distance) / (r * 1.9f));
    }
    private static void drawDiscovery(Painter p, GameCore c, Layout L) {
        if (c.landDiscovery < 0 || c.landTravelFrom>=0) return;
        float t = c.landDiscoveryT, r = L.keyR * c.keyScale();
        float fade = Math.min(1f, t * 5f) * Math.min(1f, (4.5f - t) * 2f);
        int a = (int)(255 * Math.max(0f, fade));
        float reveal = discoveryReveal(c, L, c.landDiscovery);
        float settle = ease((t - 3.2f) / 1f);
        float x = explorerX(c, L);
        float ground = cardY(L);
        float hop = t < 1.6f ? Math.abs((float)Math.sin(t * 12f)) * r * 0.10f
                : (float)Math.sin(ease((t - 1.6f) / 0.7f) * Math.PI) * r * 0.30f;
        float y = ground - hop;
        adventure(p,x,y,r,a,reveal);
        float tx = cardX(c, L, c.landDiscovery), ty = cardY(L);
        for (int i = 0; i < 7; i++) {
            float phase = i * Softbody.TAU / 7f;
            float reach = r * (1.1f + reveal * 0.5f);
            float sx = tx + (float)Math.cos(phase) * reach;
            float sy = ty + (float)Math.sin(phase) * reach;
            int sparkle = Glyph.withAlpha(GOLD, (int)(a * reveal * (1f-settle)));
            p.line(sx-r*.07f, sy, sx+r*.07f, sy, sparkle, r*.025f);
            p.line(sx, sy-r*.10f, sx, sy+r*.10f, sparkle, r*.025f);
        }
    }

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
        float direction=c.landChoice>c.landTravelFrom ? 1f : -1f;
        float start=cardX(c,L,c.landTravelFrom)-direction*iconRadius(c,L)*0.95f;
        return start+(cardX(c,L,c.landChoice)-start)*walk;
    }
    private static void drawTravel(Painter p,GameCore c,Layout L) {
        if(c.landTravelFrom<0) return;
        float t=c.landTravelT/TRAVEL_TIME,r=L.keyR*c.keyScale()*0.55f;
        float pop=ease(t/0.16f),fade=Math.min(1f,(1f-t)/0.14f);
        float walking=t>0.16f && t<0.84f ? 1f : 0f;
        float stride=(float)Math.sin(t*Softbody.TAU*5f)*walking;
        float x=travelX(c,L),ground=cardY(L)-r*0.05f;
        float y=ground-r*0.70f*pop-Math.abs(stride)*r*0.08f;
        int alpha=(int)(255*pop*fade);
        p.fillEllipse(x,ground,r*0.60f,r*0.10f,Glyph.withAlpha(INK,(int)(alpha*0.18f)));
        for(int side=-1;side<=1;side+=2)
            p.fillEllipse(x+side*r*0.25f+stride*side*r*0.14f,y+r*0.53f,
                    r*0.19f,r*0.10f,Glyph.withAlpha(0xFF795840,alpha));
        adventure(p,x,y,r,alpha,0.7f);
    }

    static void draw(Painter p, GameCore c, Layout L) {
        if (!visible(c)) return;
        float cy = cardY(L);
        p.save(); p.clipRect(0, cy - L.h * 0.063f, L.w, cy + L.h * 0.063f);
        // Back to front: the focused emblem covers the inner edges of its neighbours.
        for (int distance = Lands.COUNT - 1; distance >= 0; distance--)
        for (int land = 0; land < Lands.COUNT; land++) {
            if (!unlocked(c, land) || Math.abs(slot(c, land) - slot(c, c.landChoice)) != distance) continue;
            float x = cardX(c, L, land);
            float stepsAway = Math.abs(x - L.w * 0.5f) / spacing(c, L);
            float focus = Math.max(0f, 1f - stepsAway);
            float reveal = discoveryReveal(c, L, land);
            if (reveal <= 0f) continue;
            if (land == c.landDiscovery)
                focus += (1f - focus) * (1f - ease((c.landDiscoveryT - 3.2f) / 1f));
            float r = iconRadius(c, L) * (0.72f + focus * 0.28f);
            float y = cy + r * (land == 2 ? 0.4f : land == 3 ? -0.3f : 0f);
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
        drawDiscovery(p, c, L);
        drawTravel(p,c,L);
    }
}
