package com.dddumpling.game;

/**
 * The send-off when a run starts: the squishy the display case was showing swells out of its
 * badge in a pip of stars, shows its name with a wave, then travels into its run-companion
 * home above the keyboard. The home fades in around it before Stage 1 begins.
 *
 * Only when the case is parked on something collected — an uncollected entry is a silhouette
 * with a question mark on it, and sending that off would be sending off nothing. So the title
 * screen uses a quick random picker when no owned entry is selected.
 *
 * Position is a pure function of {@link GameCore#launchT}, and the badge's own drift is read off
 * the clock frozen at the press, so the squishy leaves from exactly where it was sitting.
 */
final class Launch extends Draw {

    private Launch() {}

    /** Total length of the send-off. The title screen dissolves under the first half of it. */
    static final float TIME = 1.85f;
    static final float PICK_TIME = .85f;
    static final float CENTER_TIME = .34f;
    static final float NAME_START = CENTER_TIME + .06f;

    /** Grown and pipped by here. */
    static final float POP = CENTER_TIME / TIME;
    /** Greeting ends and the leap starts. */
    static final float LAND = 0.64f;
    /** Arrival at the run-companion home, and the second pip. */
    static final float HOME = 0.84f;

    /** How long a pip's stars last. */
    private static final float PIP = 0.20f;

    /** Size it swells to, as a multiple of the size it sat at in the badge. */
    private static final float GROWN = 2.1f;

    /** 0..1 through the send-off. */
    static float progress(GameCore c) {
        return 1f - c.launchT / TIME;
    }

    /** Arrive gently and stop before the name and voice begin. */
    static float slide(float elapsed) {
        float u=Math.max(0f,Math.min(1f,elapsed/CENTER_TIME));
        return u*u*(3f-2f*u);
    }

    static void draw(Painter p, GameCore c, Layout L) {
        if (c.launchT <= 0f || c.launchWho < 0) return;
        float u = progress(c);
        float x0 = Showcase.iconCx(L, c.launchClock);
        float y0 = Showcase.iconCy(L, c.launchClock);
        float r0 = Showcase.iconR(L) * 0.62f;
        float homeX=RunCompanion.x(L),homeY=RunCompanion.y(L);

        float across = slide(TIME-c.launchT);
        float x = x0 + (L.w / 2f - x0) * across;
        float centerY=L.h*.5f;
        float y = y0 + (centerY-y0)*across;
        if (u >= POP && u < LAND) {
            // Two little hello bobs, settling back at the launch point before the leap.
            float v = (u - POP) / (LAND - POP);
            float hello = (float)Math.sin(v*Math.PI);
            y = centerY - r0*.28f*hello*(1f+(float)Math.sin(v*Math.PI*4f));
        } else if (u >= LAND && u < HOME) {
            // Carry the same visible squishy into the place it will occupy during the run.
            float v = ease((u - LAND) / (HOME - LAND));
            x=L.w*.5f+(homeX-L.w*.5f)*v;
            y=centerY+(homeY-centerY)*v;
        } else if (u >= HOME) {
            x=homeX;y=homeY;
        }

        // Swelling out of the badge with an overshoot, then squashed by each impact.
        float grow = u < POP ? GROWN * over(u / POP) : GROWN;
        float launchR=r0*grow*(1f-.26f*hit(u,LAND));
        float homeSize=u<=LAND?0f:ease((u-LAND)/(HOME-LAND));
        float r=launchR+(RunCompanion.radius(c,L)-launchR)*homeSize;
        float fade=1f;

        // The character arrives first; only then does its normal home resolve around it.
        float bubble=bubbleFade(u);
        if(bubble>0f) RunCompanion.drawHomeOnly(p,c,L,c.launchWho,bubble);

        // Reaches chosen to clear the body: the stars go behind it, so a burst tucked inside the
        // silhouette is a burst nobody sees.
        stars(p, c, L, x0, y0, u - POP * 0.55f, r0 * 3.6f, fade);
        stars(p, c, L, x, y, u - LAND, r * 1.9f, fade);
        stars(p, c, L, x, y, u - HOME, r * 2.4f, fade);
        if(u>=POP && u<LAND) wave(p,c.launchWho,x,y,r,(u-POP)/(LAND-POP),fade);
        Trinket.draw(p, c.launchWho, x, y, r, c.clock, true, fade);
        float nameFade=Math.min(1f,Math.max(0f,(TIME-c.launchT-NAME_START)/.10f))
                *Math.min(1f,Math.max(0f,(LAND+.08f-u)/.10f));
        if(nameFade>0f) {
            float nameY=centerY+r0*GROWN*1.8f;
            name(p,c,L,L.w*.5f,nameY,nameFade);
            p.fillCircle(L.w*.5f-L.unit*.4f,nameY+L.unit*.45f,L.unit*.08f,
                    fadeBy(Collect.BODY[c.launchWho],nameFade));
            p.fillCircle(L.w*.5f+L.unit*.4f,nameY+L.unit*.45f,L.unit*.08f,
                    fadeBy(Collect.ACCENT[c.launchWho],nameFade));
        }
    }

    static float bubbleFade(float progress) {
        return ease((progress-HOME)/(1f-HOME));
    }

    private static float ease(float u) {
        u=Math.max(0f,Math.min(1f,u));return u*u*(3f-2f*u);
    }

    private static float nameAdvance(char ch) { return ch==' ' ? .55f : ch=='I' ? .72f : 1.25f; }

    static float nameUnits(String name) {
        float units=0f;
        for(int i=0;i<name.length();i++) units+=nameAdvance(name.charAt(i));
        return units;
    }

    static float nameHeight(Layout L,String name) {
        return Math.min(type(L.unit*1.05f),L.w*.84f/nameUnits(name));
    }

    private static void name(Painter p,GameCore c,Layout L,float cx,float baseline,float fade) {
        String name=Collect.NAME[c.launchWho];
        float h=nameHeight(L,name),width=h*nameUnits(name);
        cx=Math.max(L.w*.08f+width*.5f,Math.min(L.w*.92f-width*.5f,cx));
        float x=cx-width*.5f,elapsed=TIME-c.launchT;
        int letter=0;
        for(int i=0;i<name.length();i++) {
            char ch=name.charAt(i);
            float advance=nameAdvance(ch)*h;
            if(ch!=' ') {
                float age=elapsed-NAME_START-letter*.024f;
                if(age>0f) {
                    float land=age-.12f;
                    float shape=land<0f ? 1.20f : 1f-.28f*(float)Math.exp(-8f*land)*(float)Math.cos(29f*land);
                    float drop=land<0f ? -h*.65f*(1f-age/.12f)*(1f-age/.12f)
                            : -h*.12f*(float)Math.exp(-9f*land)*(float)Math.sin(29f*land);
                    float appear=Math.min(1f,age/.07f);
                    int color=Glyph.COLOR[(letter+c.launchWho)%Glyph.COUNT];
                    TitleBubbleFont.draw(p,ch,x+advance*.5f,baseline+drop,h,color,
                            fade*appear,c.launchClock+letter*.8f+age,shape);
                }
                letter++;
            }
            x+=advance;
        }
    }

    /** The roulette slows down before holding its already-resolved selection. */
    static int shuffleStep(float remaining) {
        float u=Math.max(0f,Math.min(1f,(PICK_TIME-remaining)/(PICK_TIME*.65f)));
        return (int)(8f*(1f-(1f-u)*(1f-u)));
    }

    static void shuffle(Painter p,GameCore c,float x,float y,float hw,float hh,float fade) {
        float elapsed=PICK_TIME-c.pickerT;
        int step=shuffleStep(c.pickerT),who=c.pickerWho();
        int tint=Glyph.mix(Collect.BODY[who],Glyph.COLOR[step%Glyph.COUNT],.45f);
        float beat=.5f+.5f*(float)Math.sin(elapsed*24f);
        for(int k=3;k>=1;k--)
            p.fillEllipse(x,y,hw*(1f+k*.13f),hh*(1f+k*.10f),
                    fadeBy(Glyph.withAlpha(tint,(int)((18f+beat*12f)/k)),fade));
        p.strokePoly(new float[]{x-hw,y-hh,x+hw,y-hh,x+hw,y+hh,x-hw,y+hh},
                fadeBy(Glyph.withAlpha(tint,180),fade),hw*.035f);
        for(int k=0;k<8;k++) {
            float phase=(elapsed*1.8f+k*.137f)%1f;
            double a=k*Math.PI*.25f+elapsed*.8f;
            float sx=x+hw*(1.12f+.18f*phase)*(float)Math.cos(a);
            float sy=y+hh*(1.08f+.12f*phase)*(float)Math.sin(a);
            float twinkle=(float)Math.sin(phase*Math.PI);
            float r=hw*(.035f+.055f*twinkle);
            int color=k%2==0 ? Glyph.mix(tint,INK,.5f) : Glyph.COLOR[(step+k)%Glyph.COUNT];
            p.fillPoly(star(sx,sy,r,r*.28f,4,0f),
                    fadeBy(Glyph.withAlpha(color,(int)(220*twinkle)),fade));
        }
    }

    private static void wave(Painter p,int who,float x,float y,float r,float u,float fade) {
        float envelope=(float)Math.sin(u*Math.PI);
        float swing=(float)Math.sin(u*Math.PI*6f)*envelope;
        float palmX=x+r*(.92f+.16f*swing),palmY=y-r*(.20f+.44f*envelope);
        int outline=fadeBy(0xFF3A2E4F,fade),body=fadeBy(Collect.BODY[who],fade);
        p.line(x+r*.58f,y+r*.12f,palmX,palmY,outline,r*.14f);
        p.line(x+r*.58f,y+r*.12f,palmX,palmY,body,r*.09f);
        for(int finger=0;finger<3;finger++) {
            float fx=palmX+r*(finger-1)*.09f;
            float fy=palmY-r*(.11f+(finger==1?.04f:0f));
            p.fillCircle(fx,fy,r*.067f,outline);
            p.fillCircle(fx,fy,r*.046f,body);
        }
        p.fillCircle(palmX,palmY,r*.16f,outline);
        p.fillCircle(palmX,palmY,r*.135f,body);
    }

    /** Overshoot: past 1 and back, so the swell has a snap in it. */
    private static float over(float v) {
        if (v >= 1f) return 1f;
        return 1f - (1f - v) * (1f - v) * (1f - 2.2f * v);
    }

    /** A brief 1..0 spike as {@code u} passes {@code at}, for the squash of an impact. */
    private static float hit(float u, float at) {
        float d = Math.abs(u - at);
        return d > 0.07f ? 0f : 1f - d / 0.07f;
    }

    /**
     * The pip: a ring of stars flung outward and fading.
     *
     * Deterministic, off the entry's own index rather than the RNG — the preview harness
     * hash-compares its frames between runs, so anything random here would make every check
     * differ. Eight arms at a fixed offset read as a sparkle anyway.
     */
    private static void stars(Painter p, GameCore c, Layout L, float x, float y, float age,
            float reach, float fade) {
        if (age < 0f || age >= PIP) return;
        float v = age / PIP;
        int a = (int) (235 * (1f - v) * fade);
        if (a <= 2) return;
        int tint = Collect.TIER_COLOR[Collect.TIER[c.launchWho]];
        float rot = c.launchWho * 0.7f;
        for (int k = 0; k < 8; k++) {
            double ang = rot + k * 0.7854f;
            float d = reach * (0.35f + 1.15f * v);
            float sx = x + d * (float) Math.cos(ang);
            float sy = y + d * (float) Math.sin(ang);
            float sr = reach * 0.30f * (1f - 0.65f * v);
            // Alternating so the burst has two colours in it rather than one flat spray.
            int col = Glyph.withAlpha(k % 2 == 0 ? GOLD : tint, a);
            p.fillPoly(star(sx, sy, sr, sr * 0.40f, 4, (float) ang), col);
        }
        // A shockwave ring under them, which is what makes the pip read as an impact.
        p.strokeCircle(x, y, reach * (0.3f + 1.1f * v), Glyph.withAlpha(INK, (int) (a * 0.5f)),
                reach * 0.09f * (1f - v));
    }
}
