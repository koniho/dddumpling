package com.dddumpling.game;

/**
 * Draws a collectible from {@link Collect}: a body from {@link Shape}, a surface from
 * {@link Finish}, and a face from here.
 *
 * Shape and finish are separate classes on purpose. Thirty bespoke drawings would be thirty
 * things to tune; this way a new entry in the catalogue is one row of data, and any fix to
 * the glitter or the holo sheen lands on every entry that wears it.
 *
 * Uncollected entries go through the same shape code with a flat dark fill and a question
 * mark over it, so the silhouette is a real outline of the thing you are missing rather
 * than a generic placeholder.
 */
final class Trinket {

    private static final int INK = 0xFF3A2E4F;        // face lines
    private static final int BLUSH = 0x66FF7C9E;
    /** Fill and outline for something not yet collected. */
    private static final int DARK = 0xFF2C2450, DARK_EDGE = 0xFF564A85;

    /**
     * Where the face sits and how big it is, per {@link Shape}, in units of the nominal
     * radius. A wedge wants its face low on the flesh; a cone wants it up on the scoop.
     */
    private static final float[] FACE_DY = {
        0.04f, 0.06f, 0.18f, 0.06f, 0.40f, 0.16f, 0.34f, 0.12f,
        0.02f, 0.02f, 0.06f, 0.14f, 0.62f, -0.34f, 0.22f, 0.04f, 0.06f,
    };
    private static final float[] FACE_R = {
        0.64f, 0.66f, 0.58f, 0.62f, 0.38f, 0.60f, 0.48f, 0.60f,
        0.64f, 0.70f, 0.64f, 0.60f, 0.38f, 0.52f, 0.62f, 0.52f, 0.64f,
    };

    private Trinket() {}

    /**
     * Draws entry {@code i} centred on cx,cy at nominal radius r.
     *
     * @param known false to draw the unknown silhouette instead
     * @param fade  0..1 master opacity, for dimming the neighbours in the display case
     */
    static void draw(Painter p, int i, float cx, float cy, float r, float clock, boolean known,
            float fade) {
        draw(p,i,cx,cy,r,clock,known,fade,-1,0f);
    }
    static void drawReacting(Painter p,int i,float cx,float cy,float r,float clock,float fade,int mood,float look) {
        draw(p,i,cx,cy,r,clock,true,fade,mood,look,false);
    }
    static void drawReacting(Painter p,int i,float cx,float cy,float r,float clock,float fade,int mood,
            float look,boolean ninja) {
        draw(p,i,cx,cy,r,clock,true,fade,mood,look,ninja);
    }
    private static void draw(Painter p,int i,float cx,float cy,float r,float clock,boolean known,
            float fade,int mood,float look) {
        draw(p,i,cx,cy,r,clock,known,fade,mood,look,false);
    }
    private static void draw(Painter p,int i,float cx,float cy,float r,float clock,boolean known,
            float fade,int mood,float look,boolean ninja) {
        if (fade <= 0.01f || i < 0 || i >= Collect.COUNT) return;
        if (Collect.FAMILY[i]==Collect.MOLES || Collect.FAMILY[i]==Collect.SNAKES) {
            CaveCollect.draw(p,i,cx,cy,r,clock,known,fade,mood,look,ninja);return;
        }
        if (i >= Collect.BOSS_FIRST && i < Collect.BOSS_FIRST + Collect.BOSS_COUNT) {
            BossCollect.draw(p, i - Collect.BOSS_FIRST, cx, cy, r, clock, known, fade,mood,look,ninja);
            return;
        }
        int shape = Collect.SHAPE[i];
        if (!known) {
            // Drawn twice, the outer copy a little larger: that gives any of the fifteen
            // shapes an outline without each having to know how to stroke itself, and
            // without an outline the silhouette is dark-on-dark and reads as nothing.
            Shape.draw(p, shape, cx, cy, r * 1.09f, DARK_EDGE, DARK_EDGE, fade, i * 1.7f, false);
            Shape.draw(p, shape, cx, cy, r, DARK, DARK, fade, i * 1.7f, false);
            // Kept small enough to leave the outline visible around it — the shape is the
            // clue, and a mark that fills the frame throws that away.
            float fr = r * FACE_R[shape];
            p.text("?", cx, cy + r * FACE_DY[shape] + fr * 0.52f, Draw.type(fr * 1.55f),
                    Draw.fadeBy(Glyph.withAlpha(Draw.INK, 210), fade), Painter.CENTER, true);
            return;
        }
        Shape.draw(p, shape, cx, cy, r, Collect.BODY[i], Collect.ACCENT[i], fade, i * 1.7f, true);
        Finish.draw(p, Collect.FINISH[i], cx, cy, r, Collect.ACCENT[i], clock, fade, i);
        float faceY=cy+r*FACE_DY[shape],faceR=r*FACE_R[shape];
        if(mood<0) face(p, i % 3, cx, faceY, faceR, fade);
        else reactionFace(p,cx,faceY,faceR,clock,fade,mood,look);
        if(ninja) ninjaMask(p,cx,faceY,r*.82f,fade);
    }

    /** Oval ninja hood with a horizontal eye opening and a tied right side. */
    static void ninjaMask(Painter p,float x,float y,float r,float fade) {
        int cloth=Draw.fadeBy(0xFF211B35,fade),edge=Draw.fadeBy(0xFF615370,fade);
        float[] hood=oval(x,y+r*.10f,r*.98f,r*1.05f,32);
        float[] eyes=oval(x,y-r*.02f,r*.68f,r*.29f,24);
        // Even-odd fill leaves the character and both eyes visible through the opening while the
        // continuous hood covers the forehead, mouth, and both outer sides of the eye line.
        p.fillContours(new float[][]{hood,eyes},cloth);
        p.strokePoly(eyes,edge,r*.055f);
        // Knot and loose ends sit outside the opening, as in the screenshot reference.
        p.fillCircle(x+r*.91f,y-r*.42f,r*.14f,cloth);
        p.fillPoly(new float[]{x+r*.93f,y-r*.48f,x+r*1.38f,y-r*.76f,
                x+r*1.20f,y-r*.38f},cloth);
        p.fillPoly(new float[]{x+r*.94f,y-r*.39f,x+r*1.39f,y-r*.12f,
                x+r*1.12f,y-r*.10f},cloth);
    }

    private static float[] oval(float x,float y,float rx,float ry,int points) {
        float[] out=new float[points*2];
        for(int i=0;i<points;i++) {
            float a=i*Softbody.TAU/points;
            out[i*2]=x+(float)Math.cos(a)*rx;
            out[i*2+1]=y+(float)Math.sin(a)*ry;
        }
        return out;
    }

    /** Compact expressions shared by every collectible family, only in the run companion. */
    static void reactionFace(Painter p,float x,float y,float r,float clock,float fade,int mood,float look) {
        int ink=Draw.fadeBy(INK,fade);
        boolean blink=Math.sin(clock*1.7f)>.985f;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.35f+look*r;
            if(mood==6) {
                p.polyline(new float[]{ex-side*r*.20f,y-r*.26f,ex+side*r*.16f,y-r*.12f},ink,r*.09f);
                p.fillEllipse(ex,y,r*.16f,r*.075f,ink);
                p.fillCircle(ex-side*r*.04f,y-r*.015f,r*.035f,Draw.fadeBy(0xFFFFFFFF,fade));
            } else if(mood==7) {
                p.polyline(new float[]{ex-r*.16f,y-r*.01f,ex,y+r*.08f,ex+r*.16f,y+r*.04f},ink,r*.08f);
                if(side>0)p.fillEllipse(ex+r*.13f,y-r*.20f,r*.065f,r*.13f,
                        Draw.fadeBy(0xFF79DDEB,fade));
            } else if(mood==9) {
                p.polyline(new float[]{ex-side*r*.18f,y-r*.04f,ex,y-r*.15f,
                        ex+side*r*.18f,y-r*.04f},ink,r*.09f);
            } else if(mood==8) {
                float cheer=(float)Math.sin(clock*16f)*r*.025f;
                p.polyline(new float[]{ex-side*r*.20f,y-r*.02f+cheer,ex,y-r*.18f+cheer,
                        ex+side*r*.20f,y-r*.02f+cheer},ink,r*.105f);
                p.fillCircle(ex-side*r*.02f,y-r*.16f+cheer,r*.04f,Draw.fadeBy(0xFFFFFFFF,fade));
            } else if(mood==5) {
                p.polyline(new float[]{ex-r*.15f,y-r*.02f,ex,y+r*.08f,ex+r*.15f,y-r*.02f},ink,r*.085f);
                p.fillEllipse(ex+side*r*.08f,y+r*(.23f+.08f*(float)Math.sin(clock*7f+side)),
                        r*.07f,r*.15f,Draw.fadeBy(0xFF79DDEB,fade));
            } else if(mood==3) {
                p.polyline(new float[]{ex-side*r*.12f,y-r*.17f,ex+side*r*.07f,y,
                        ex-side*r*.12f,y+r*.09f},ink,r*.09f);
            } else if(mood==1 || blink) {
                p.polyline(new float[]{ex-r*.14f,y,ex,y-(blink?0f:r*.15f),ex+r*.14f,y},ink,r*.085f);
            } else if(mood==4) {
                p.fillPoly(Draw.star(ex,y-r*.04f,r*.19f,r*.095f,4,0),ink);
            } else {
                p.fillEllipse(ex,y-r*.03f,r*(mood==2?.15f:.12f),r*(mood==2?.22f:.17f),ink);
                p.fillCircle(ex-r*.035f,y-r*.10f,r*.042f,Draw.fadeBy(0xFFFDF7EA,fade));
            }
            p.fillEllipse(x+side*r*.60f,y+r*.18f,r*.16f,r*.09f,Draw.fadeBy(BLUSH,fade));
        }
        if(mood==9) {
            p.polyline(new float[]{x-r*.22f,y+r*.25f,x-r*.08f,y+r*.34f,
                    x+r*.08f,y+r*.34f,x+r*.25f,y+r*.17f},ink,r*.085f);
        } else if(mood==8) {
            p.fillEllipse(x,y+r*.28f,r*.25f,r*.23f,ink);
            p.fillEllipse(x,y+r*.38f,r*.15f,r*.075f,Draw.fadeBy(0xFFFF8AAB,fade));
        } else if(mood==6) {
            p.fillEllipse(x,y+r*.34f,r*.20f,r*.20f,ink);
            p.fillEllipse(x,y+r*.29f,r*.10f,r*.06f,Draw.fadeBy(0xFFFFD4C8,fade));
        } else if(mood==7) p.polyline(new float[]{x-r*.18f,y+r*.36f,x,y+r*.28f,
                x+r*.18f,y+r*.36f},ink,r*.07f);
        else if(mood==5) p.polyline(new float[]{x-r*.19f,y+r*.38f,x,y+r*.25f,
                x+r*.19f,y+r*.38f},ink,r*.075f);
        else if(mood==2) p.strokeCircle(x,y+r*.33f,r*.11f,ink,r*.07f);
        else if(mood==3) p.polyline(new float[]{x-r*.18f,y+r*.31f,x-r*.06f,y+r*.24f,
                x+r*.06f,y+r*.34f,x+r*.18f,y+r*.27f},ink,r*.075f);
        else if(mood==4) {
            p.fillEllipse(x,y+r*.30f,r*.18f,r*.17f,ink);
            p.fillEllipse(x,y+r*.38f,r*.11f,r*.065f,Draw.fadeBy(0xFFFF8AAB,fade));
        } else p.polyline(new float[]{x-r*.17f,y+r*.24f,x,y+r*.34f,x+r*.17f,y+r*.24f},ink,r*.075f);
    }

    // ---- faces --------------------------------------------------------------

    /**
     * One of three faces, chosen off the entry index. Three is enough that a shelf of
     * thirty does not look stamped from one mould, and few enough that they all stay
     * recognisably the same species.
     */
    private static void face(Painter p, int kind, float cx, float cy, float r, float fade) {
        int ink = Draw.fadeBy(INK, fade);
        float ew = r * 0.30f;
        if (kind == 0) {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.fillEllipse(ex, cy - r * 0.06f, r * 0.13f, r * 0.17f, ink);
                p.fillEllipse(ex - r * 0.04f, cy - r * 0.12f, r * 0.05f, r * 0.05f,
                        Draw.fadeBy(0xE6FFFFFF, fade));
            }
            p.polyline(new float[] {cx - r * 0.16f, cy + r * 0.24f, cx, cy + r * 0.34f,
                    cx + r * 0.16f, cy + r * 0.24f}, ink, r * 0.07f);
        } else if (kind == 1) {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.polyline(new float[] {ex - r * 0.15f, cy + r * 0.02f, ex, cy - r * 0.16f,
                        ex + r * 0.15f, cy + r * 0.02f}, ink, r * 0.08f);
            }
            float w = r * 0.15f;
            p.polyline(new float[] {cx - w * 2, cy + r * 0.24f, cx - w, cy + r * 0.38f,
                    cx, cy + r * 0.24f, cx + w, cy + r * 0.38f, cx + w * 2, cy + r * 0.24f},
                    ink, r * 0.07f);
        } else {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.polyline(new float[] {ex + s * r * 0.15f, cy - r * 0.18f,
                        ex - s * r * 0.13f, cy - r * 0.02f, ex + s * r * 0.15f,
                        cy + r * 0.14f}, ink, r * 0.08f);
            }
            p.fillEllipse(cx, cy + r * 0.30f, r * 0.19f, r * 0.13f, ink);
        }
        for (int s = -1; s <= 1; s += 2) {
            p.fillEllipse(cx + s * r * 0.56f, cy + r * 0.10f, r * 0.14f, r * 0.09f,
                    Draw.fadeBy(BLUSH, fade));
        }
    }
}
