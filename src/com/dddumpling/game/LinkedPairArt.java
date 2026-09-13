package com.dddumpling.game;

/** Key-colored vines, paws and soft hands with a directional cue; shared by Android and iOS renderers. */
final class LinkedPairArt {
    private static final int INK = 0xFF22253C, GLOVE = 0xFFFFF4DD, GOLD = 0xFFFFD56B;
    static final float SPIN_TIME = 0.9f;
    private LinkedPairArt() {}

    static void draw(Painter p, GameCore c, Layout L) {
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy a = c.enemies.get(i), b = a.link;
            if (a.spinMate != null) {
                if (a.linkReleaseDir < 0f) spin(p,c,L,a,a.spinMate);
                continue;
            }
            if (a.destroyed && a.linkReleaseDir != 0f) release(p,c,L,a);
            if (b == null || c.enemies.indexOf(b) <= i || a.destroyed || b.destroyed) continue;
            float r = L.enemyR;
            float ax = c.enemyCentreX(a) + maskRadius(a, r) - r*0.14f, ay = a.y;
            float bx = c.enemyCentreX(b) - maskRadius(b, r) + r*0.14f, by = b.y;
            float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f;
            if (my < -r*2f) continue;
            float wiggle = (float) Math.sin(c.clock * 1.4f) * r * 0.012f;
            int ac = Glyph.COLOR[a.word[0]], bc = Glyph.COLOR[b.word[0]];
            int ag = c.incognito() ? 0 : a.word[0], bg = c.incognito() ? 0 : b.word[0];
            GameCore.Enemy waiting = a.linkWaiting ? a : b.linkWaiting ? b : null;
            float ahx = mx - r * 0.30f, bhx = mx + r * 0.30f;
            float ahy = my + wiggle, bhy = my - wiggle;
            float strain = Math.max(a.linkStrain, b.linkStrain);
            if (waiting != null) strain = Math.max(strain, 0.65f);
            float pose = Math.min(1f, strain/0.18f);
            // Hold a firm flex pose; only the short release tail blends back to rest.
            float limbR = r * (1f + pose*0.12f);
            float bend = 0.025f + pose*0.375f;
            Pulse wave = new Pulse(mx, Math.max(r*0.2f, (bx-ax)*0.5f), strain);
            // The entire limb layer is confined to the gap between the character silhouettes.
            // Its leaves, paws and flex overshoot cannot leak onto or behind either body.
            // Let the shared HUD backing fade the arms and characters together.
            p.save();
            p.clipRect(L.playLeft, 0f, L.playRight, L.dangerY);
            p.clipOutCircle(c.enemyCentreX(a), a.y, maskRadius(a, r));
            p.clipOutCircle(c.enemyCentreX(b), b.y, maskRadius(b, r));
            limb(p, ag, ax, ay, ahx, ahy, limbR, 1f, ac, false, bend, wave);
            limb(p, bg, bx, by, bhx, bhy, limbR, -1f, bc, false, bend, wave);
            if (!fruit(ag)) extremity(p, ag, ahx, ahy, limbR, 1f, wave.tint(ac, ahx), false);
            if (!fruit(bg)) extremity(p, bg, bhx, bhy, limbR, -1f, wave.tint(bc, bhx), false);
            // Short foreground sections pass over the partner's limb; the rest stays behind it.
            if (fruit(ag)) curl(p, ahx, ahy, limbR, 1f, ac, 0.35f, 0.65f, wave);
            if (fruit(bg)) curl(p, bhx, bhy, limbR, -1f, bc, 0.65f, 0.90f, wave);
            p.restore();
            if (waiting != null) {
                GameCore.Enemy next = waiting == a ? b : a;
                float tx = c.tileX(next, Math.min(next.pos, next.word.length - 1), L);
                float ty = next.y;
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 10f);
                p.strokePoly(Glyph.hex(tx, ty, r * (1.10f + pulse * 0.12f)), GOLD, r * 0.09f);

            }
        }
    }

    /** One collision sends both bodies orbiting their still-joined hands offscreen. */
    private static void spin(Painter p, GameCore c, Layout L, GameCore.Enemy a, GameCore.Enemy b) {
        float t = Math.min(1f,a.destroyT/SPIN_TIME), r = L.enemyR;
        float direction = a.linkReleaseX < L.w*0.5f ? -1f : 1f;
        float mx = a.linkReleaseX+direction*t*t*(L.w+r*4f);
        float my = a.linkReleaseY-t*t*L.h*0.45f;
        float angle = t*(float)Math.PI*3.5f*direction;
        float reach = Math.abs(a.baseX-b.baseX)*0.5f;
        Painter q = new TurnPainter(p,mx,my,angle);
        GameCore.Enemy[] pair = {a,b};
        q.save();
        q.clipOutCircle(-reach,0f,r*Layout.HEAD_SCALE*0.965f);
        q.clipOutCircle(reach,0f,r*Layout.HEAD_SCALE*0.965f);
        for (int i = 0; i < 2; i++) {
            float sign = i == 0 ? 1f : -1f, cx = -sign*reach;
            float hx = -sign*r*0.30f;
            int g = pair[i].word[0], col = Glyph.COLOR[g];
            limb(q,g,cx+sign*r*0.9f,0f,hx,0f,r,sign,col,false,0.08f,new Pulse(0f,r,0f));
            if (!fruit(g)) extremity(q,g,hx,0f,r,sign,col,false);
        }
        q.restore();
        for (int i = 0; i < 2; i++) {
            int g = pair[i].word[0], col = Glyph.COLOR[g];
            float x = (i == 0 ? -1f : 1f)*reach;
            float[] hex = Glyph.hex(x,0f,r*Layout.HEAD_SCALE);
            q.fillPoly(hex,Glyph.withAlpha(col,52));
            q.strokePoly(hex,Glyph.withAlpha(col,200),r*0.08f);
            Kawaii.draw(q,g,x,0f,r*Layout.HEAD_SCALE*0.60f,col,1f,1f);
        }
    }

    /** Rotates the complete code-drawn pair, including faces, clasp and clipping masks. */
    private static final class TurnPainter implements Painter {
        final Painter p;
        final float cx,cy,cos,sin;
        TurnPainter(Painter p,float cx,float cy,float angle) {
            this.p=p; this.cx=cx; this.cy=cy;
            cos=(float)Math.cos(angle); sin=(float)Math.sin(angle);
        }
        float x(float x,float y) { return cx+x*cos-y*sin; }
        float y(float x,float y) { return cy+x*sin+y*cos; }
        float[] turn(float[] xy) {
            float[] out=new float[xy.length];
            for(int i=0;i<xy.length;i+=2) { out[i]=x(xy[i],xy[i+1]); out[i+1]=y(xy[i],xy[i+1]); }
            return out;
        }
        public void fillPoly(float[] xy,int c) { p.fillPoly(turn(xy),c); }
        public void strokePoly(float[] xy,int c,float w) { p.strokePoly(turn(xy),c,w); }
        public void polyline(float[] xy,int c,float w) { p.polyline(turn(xy),c,w); }
        public void fillContours(float[][] paths,int c) {
            float[][] out=new float[paths.length][];
            for(int i=0;i<paths.length;i++) out[i]=turn(paths[i]);
            p.fillContours(out,c);
        }
        public void fillCircle(float x,float y,float r,int c) { p.fillCircle(x(x,y),y(x,y),r,c); }
        public void strokeCircle(float x,float y,float r,int c,float w) { p.strokeCircle(x(x,y),y(x,y),r,c,w); }
        float[] ellipse(float x,float y,float rx,float ry,float start,float sweep,int n) {
            float[] out=new float[(n+1)*2];
            for(int i=0;i<=n;i++) {
                float a=(start+sweep*i/n)*(float)Math.PI/180f;
                out[i*2]=x+rx*(float)Math.cos(a); out[i*2+1]=y+ry*(float)Math.sin(a);
            }
            return out;
        }
        public void fillEllipse(float x,float y,float rx,float ry,int c) { fillPoly(ellipse(x,y,rx,ry,0f,360f,40),c); }
        public void arc(float x,float y,float rx,float ry,float a,float sweep,int c,float w) { polyline(ellipse(x,y,rx,ry,a,sweep,32),c,w); }
        public void line(float ax,float ay,float bx,float by,int c,float w) { p.line(x(ax,ay),y(ax,ay),x(bx,by),y(bx,by),c,w); }
        public void fillRect(float l,float t,float r,float b,int c) { fillPoly(new float[]{l,t,r,t,r,b,l,b},c); }
        public void clipOutCircle(float x,float y,float r) { p.clipOutCircle(x(x,y),y(x,y),r); }
        public void save() { p.save(); }
        public void restore() { p.restore(); }
        public void translate(float x,float y) { p.translate(x*cos-y*sin,x*sin+y*cos); }
        public void text(String s,float x,float y,float size,int c,int align,boolean bold) { p.text(s,x(x,y),y(x,y),size,c,align,bold); }
        public void clipRect(float l,float t,float r,float b) {
            float[] xy=turn(new float[]{l,t,r,t,r,b,l,b});
            float minX=xy[0],maxX=xy[0],minY=xy[1],maxY=xy[1];
            for(int i=2;i<xy.length;i+=2) { minX=Math.min(minX,xy[i]); maxX=Math.max(maxX,xy[i]); minY=Math.min(minY,xy[i+1]); maxY=Math.max(maxY,xy[i+1]); }
            p.clipRect(minX,minY,maxX,maxY);
        }
    }

    static float releaseTravel(GameCore.Enemy e, Layout L) {
        float t = Math.min(1f,e.destroyT/GameCore.DESTROY_TIME);
        return e.linkReleaseDir*t*t*L.w*0.75f;
    }

    static float releaseLift(GameCore.Enemy e, Layout L) {
        float t = Math.min(1f,e.destroyT/GameCore.DESTROY_TIME);
        return -(float)Math.sin(t*Math.PI)*L.enemyR*0.65f;
    }

    /** The clasp opens, each hand recoils with its character, and paired swooshes fade. */
    private static void release(Painter p, GameCore c, Layout L, GameCore.Enemy e) {
        float t = Math.min(1f,e.destroyT/0.32f);
        if (t >= 1f) return;
        float r = L.enemyR, dir = e.linkReleaseDir, travel = releaseTravel(e,L);
        float cy = e.y+releaseLift(e,L), cx = c.enemyCentreX(e)+travel;
        float peel = 1f-(1f-t)*(1f-t);
        float hx = e.linkReleaseX+travel+dir*r*(0.30f+0.55f*peel);
        float hy = e.linkReleaseY+releaseLift(e,L)-r*0.22f*peel;
        float size = r*(1f-t*t);
        int col = Glyph.COLOR[e.word[0]];
        int glyph = c.incognito() ? 0 : e.word[0];
        Pulse quiet = new Pulse(hx,r,0f);
        p.save();
        p.clipRect(L.playLeft,0f,L.playRight,L.dangerY);
        p.clipOutCircle(cx,cy,maskRadius(e,r)*(1f-0.30f*e.destroyT/GameCore.DESTROY_TIME));
        limb(p,glyph,cx-dir*r*0.9f,cy,hx,hy,size,-dir,col,false,0.18f,quiet);
        if (!fruit(glyph)) extremity(p,glyph,hx,hy,size,-dir,col,false);
        p.restore();
        if (dir > 0f) return; // One burst for the bond, not one per character.
        int ink = Glyph.withAlpha(GLOVE,(int)(210f*(1f-t)));
        for (int side = -1; side <= 1; side += 2) {
            for (int streak = 0; streak < 3; streak++) {
                float[] path = new float[14];
                for (int k = 0; k < 7; k++) {
                    float u = k/6f;
                    path[k*2] = e.linkReleaseX+side*r*(0.12f+t*0.65f+u*0.60f);
                    path[k*2+1] = e.linkReleaseY+r*((streak-1)*0.24f
                            +(streak-1)*(float)Math.sin(u*Math.PI)*0.16f);
                }
                p.polyline(path,ink,r*0.055f*(1f-t*0.6f));
            }
        }
    }

    /** Include character extremities and late-stage threat jitter in the mask. */
    private static float maskRadius(GameCore.Enemy e, float r) {
        float attack = e.attacking ? Math.min(1f, e.attackT / GameCore.ATTACK_TIME) : 0f;
        // Follow the rendered tile's entrance and threat scale. A small inset puts the
        // shoulder beneath its border instead of leaving a visible gap around the key.
        float enter = 0.62f + 0.38f*e.enterT;
        float tile = Layout.HEAD_SCALE*enter*(1f+0.26f*attack+0.08f*e.warn);
        float jitter = Math.max(e.warn, attack)*(0.14f+0.30f*attack);
        return r*(tile*0.965f+jitter);
    }

    /** One highlight front travels from the clasp outward along both colored limbs. */
    private static final class Pulse {
        final float center, reach, phase, strength;
        Pulse(float center, float reach, float strain) {
            this.center = center;
            this.reach = reach;
            phase = (1f-strain)*1.6f;
            strength = strain > 0f ? 1f : 0f;
        }
        int tint(int color, float x) {
            float distance = Math.abs(x-center)/reach;
            float band = Math.max(0f, 1f-Math.abs(distance-phase)/0.30f);
            return Glyph.mix(color, GLOVE, band*strength*0.85f);
        }
    }

    private static boolean fruit(int glyph) { return glyph == 1 || glyph == 3; }

    private static void limb(Painter p, int glyph, float ax, float ay, float hx, float hy,
            float r, float dir, int color, boolean pointing, float bend, Pulse wave) {
        if (!fruit(glyph)) {
            arm(p, ax, ay, hx, hy, r, color, bend, wave);
            return;
        }
        float cx = hx + dir*r*0.24f;
        float endX = pointing ? hx + dir*r*0.85f : cx - dir*r*0.34f;
        float endY = hy - (pointing ? r*0.12f : 0f);
        float[] stem = new float[26];
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            stem[i*2] = ax + (endX-ax)*t;
            stem[i*2+1] = ay + (endY-ay)*t + (float)Math.sin(t*Math.PI)*r*bend;
        }
        float[] widths = new float[13];
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            widths[i] = r * (0.13f - 0.065f*t + 0.025f*(float)Math.sin(t*Math.PI));
        }
        tube(p, stem, widths, color, wave);
        // A leaf at the bend replaces the glove's mechanical elbow with a growing node.
        float lx = stem[10], ly = stem[11];
        leaf(p, lx, ly, lx-dir*r*0.32f, ly+r*0.38f, r*0.16f, wave.tint(color, lx));
        if (pointing) {
            // The unfurled tip and pointed leaf direct attention at the remaining key.
            leaf(p, endX-dir*r*0.28f, endY, endX+dir*r*0.12f, endY, r*0.13f, color);
        } else {
            curl(p, hx, hy, r, dir, color, 0f, 1f, wave);
        }
    }

    private static void leaf(Painter p, float x, float y, float tx, float ty, float width, int color) {
        float dx = tx-x, dy = ty-y;
        float length = Math.max(0.001f, (float)Math.sqrt(dx*dx+dy*dy));
        float nx = -dy/length*width, ny = dx/length*width;
        float mx = (x+tx)*0.5f, my = (y+ty)*0.5f;
        float[] shape = {x,y, mx+nx,my+ny, tx,ty, mx-nx,my-ny};
        p.fillPoly(shape, color);
        p.strokePoly(shape,Glyph.withAlpha(color,175),width*0.30f);
        p.line(x,y,tx,ty,Glyph.withAlpha(color,100),width*0.20f);
    }

    private static void curl(Painter p, float hx, float hy, float r, float dir, int color,
            float from, float to, Pulse wave) {
        float[] path = new float[34];
        for (int i = 0; i <= 16; i++) {
            float t = from + (to-from)*i/16f;
            float angle = (float)Math.PI + t*(float)Math.PI*1.8f;
            float radius = r*(0.34f-0.08f*t);
            path[i*2] = hx+dir*r*0.24f+dir*(float)Math.cos(angle)*radius;
            path[i*2+1] = hy+(float)Math.sin(angle)*radius;
        }
        float[] widths = new float[17];
        for (int i = 0; i <= 16; i++) {
            float t = from + (to-from)*i/16f;
            widths[i] = r*(0.075f-0.035f*t);
        }
        tube(p, path, widths, color, wave);
    }

    private static void extremity(Painter p, int glyph, float x, float y, float r,
            float dir, int color, boolean pointing) {
        if (glyph != 2 && glyph != 5) {
            hand(p, x, y, r, dir, color, pointing);
            return;
        }
        float cx = x+dir*r*0.14f;
        if (pointing) {
            capsule(p,cx,y-r*0.10f,x+dir*r*0.82f,y-r*0.10f,r*0.10f,color);
        }
        // A broad animal paw, with joined toes and pads inside the silhouette.
        p.fillEllipse(cx,y,r*0.36f,r*0.30f,color);
        for (int i = 0; i < 3; i++) {
            float toeX = cx + (i-1)*r*0.19f;
            float toeY = y-r*(i == 1 ? 0.24f : 0.18f);
            p.fillCircle(toeX,toeY,r*0.13f,color);
        }
        int pad = Glyph.mix(color,0xFFCF718C,0.55f);
        p.fillEllipse(cx,y+r*0.06f,r*0.14f,r*0.10f,pad);
        for (int i = 0; i < 3; i++) {
            p.fillEllipse(cx+(i-1)*r*0.16f,y-r*0.14f,r*0.045f,r*0.06f,pad);
        }
    }

    /** Nearly straight at rest; the elbow drops below the clasp into a held flex on rejection. */
    private static void arm(Painter p, float ax, float ay, float bx, float by, float r, int color, float bend, Pulse wave) {
        float ex = ax + (bx - ax) * 0.60f;
        float ey = Math.max(ay, by) + r * bend;
        // A cubic bend keeps both sides of the elbow rounded in the held flex.
        float[] points = new float[50], widths = new float[25];
        for (int i = 0; i <= 24; i++) {
            float t = i/24f, u = 1f-t;
            points[i*2] = u*u*u*ax+3f*u*t*ex+t*t*t*bx;
            points[i*2+1] = u*u*u*ay+3f*u*t*ey+t*t*t*by;
            widths[i] = r*(0.16f-0.065f*t+0.035f*(float)Math.sin(t*Math.PI));
        }
        tube(p, points, widths, color, wave);
    }

    /** Solid hexagon-colored fill with a separate rounded underside shadow layer. */
    private static void tube(Painter p, float[] points, float[] widths, int color, Pulse wave) {
        int n = widths.length;
        float[] outline = new float[n*4];
        float widest = 0f;
        for (int i = 0; i < n; i++) {
            int before = Math.max(0,i-1), after = Math.min(n-1,i+1);
            float dx = points[after*2]-points[before*2];
            float dy = points[after*2+1]-points[before*2+1];
            float length = Math.max(0.001f,(float)Math.sqrt(dx*dx+dy*dy));
            float nx = -dy/length*widths[i], ny = dx/length*widths[i];
            outline[i*2] = points[i*2]+nx;
            outline[i*2+1] = points[i*2+1]+ny;
            int j = 2*n-1-i;
            outline[j*2] = points[i*2]-nx;
            outline[j*2+1] = points[i*2+1]-ny;
            widest = Math.max(widest,widths[i]);
        }
        p.fillPoly(outline,color);
        // Translucent shadow bands stay inside the silhouette. Lighting comes from
        // above-left, so the shaded side follows each bend and either arm direction.
        int shadow = Glyph.mix(color,INK,0.82f);
        int highlight = Glyph.mix(color,GLOVE,0.80f);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 1; i < n; i++) {
                int a = side > 0 ? i-1 : 2*n-i;
                int b = side > 0 ? i : 2*n-1-i;
                float ax = points[(i-1)*2], ay = points[(i-1)*2+1];
                float bx = points[i*2], by = points[i*2+1];
                float anx = outline[a*2]-ax, any = outline[a*2+1]-ay;
                float bnx = outline[b*2]-bx, bny = outline[b*2+1]-by;
                float light = ((anx*0.35f+any*0.94f)/widths[i-1]
                        +(bnx*0.35f+bny*0.94f)/widths[i])*0.5f;
                for (int band = 0; band < 4; band++) {
                    float inner = band*0.25f, outer = inner+0.25f;
                    int alpha = light >= 0f ? 40+band*40
                            : band == 0 ? 25 : band == 1 ? 80 : band == 2 ? 115 : 65;
                    p.fillPoly(new float[] {ax+anx*inner,ay+any*inner,
                            bx+bnx*inner,by+bny*inner,bx+bnx*outer,by+bny*outer,
                            ax+anx*outer,ay+any*outer},
                            Glyph.withAlpha(light >= 0f ? shadow : highlight,
                                    (int)(alpha*Math.abs(light))));
                }
            }
        }
        float stroke = widest*0.25f;
        // Tint each edge where the pulse passes, from clasp toward character.
        for (int i = 0; i < 2*n; i++) {
            int j = (i+1)%(2*n);
            float x = (outline[i*2]+outline[j*2])*0.5f;
            p.line(outline[i*2],outline[i*2+1],outline[j*2],outline[j*2+1],
                    Glyph.withAlpha(wave.tint(color,x),175),stroke);
        }
    }

    private static void capsule(Painter p, float x, float y, float tx, float ty, float radius, int color) {
        p.line(x, y, tx, ty, color, radius * 2f);
        p.fillCircle(x, y, radius, color);
        p.fillCircle(tx, ty, radius, color);
    }

    /** A smooth mitten silhouette with a cuff, folded fingers and one distinct thumb. */
    private static void hand(Painter p, float x, float y, float r, float dir, int color, boolean pointing) {
        float palmX = x + dir * r * 0.13f;
        // The extended index is part of its owner's silhouette and keeps that key's color.
        if (pointing) {
            capsule(p, palmX, y - r * 0.12f, x + dir * r * 0.85f, y - r * 0.12f,
                    r * 0.10f, color);
        }
        p.fillEllipse(palmX, y, r * 0.34f, r * 0.27f, color);
        // Thumb curves across the lower palm, making a clasp rather than a paw print.
        capsule(p, x - dir*r*0.04f, y+r*0.16f, x+dir*r*0.24f, y+r*0.23f, r*0.10f, color);
        int seam = Glyph.mix(color, INK, 0.48f);
        for (int i = 0; i < 2; i++) {
            float fx = palmX + dir * r * (0.08f + i * 0.10f);
            p.line(fx, y-r*0.16f, fx, y-r*0.03f, seam, r*0.025f);
        }
        // Cuff bridges the forearm and palm; a soft highlight keeps the hand readable small.
        float cuffX = x - dir*r*0.19f;
        capsule(p, cuffX, y-r*0.10f, cuffX, y+r*0.10f, r*0.060f, Glyph.mix(color, GLOVE, 0.35f));
        p.arc(palmX, y, r*0.22f, r*0.15f, 210f, 65f,
                Glyph.mix(color, GLOVE, 0.42f), r*0.035f);
    }
}
