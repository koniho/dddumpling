package com.dddumpling.game;

/**
 * The dim sum steamer of the interlude, drawn as a shallow bamboo basket seen from slightly
 * above — a front three-quarter view, so the rim reads as an ellipse and you can see down
 * into the opening.
 *
 * It is deliberately two passes with the squishy drawn between them: {@link #back} paints the
 * far wall and the shadowed interior, then the caller draws whatever is inside, then
 * {@link #front} paints the near wall over its lower half. That ordering is the whole
 * illusion — one flat basket shape in front of the squishy reads as a lid pasted on, and one
 * behind it reads as the squishy floating in front of a barrel.
 *
 * The geometry is passed in rather than computed here, because the lid, the steam and the
 * squishy's resting height all have to agree with it.
 */
final class Basket extends Draw {

    /** Segments per elliptical arc. Enough that the rim does not read as a polygon. */
    private static final int SEGS = 15;

    private Basket() {}

    /** Split the fixed wisps across the lid so steam wraps around it without covering the face. */
    static void steam(Painter p,float cx,float sourceY,float rx,float rise,float clock,
            float strength,float pulse,float fade,boolean front) {
        if(fade<=0f || strength<=0f)return;
        for(int i=0;i<14;i++) {
            if((i%4>=2)!=front)continue;
            float density=Math.max(0f,Math.min(1f,strength*14f-i));
            if(density<=0f)continue;
            float phase=(clock*(.42f+(i%3)*.035f)+i*.381966f)%1f;
            float side=i%2==0?-1f:1f;
            float vent=cx+side*rx*(.69f+.045f*(i%3));
            float drift=(float)Math.sin(phase*5f+i*1.7f)*rx*(.06f+.06f*strength);
            float y=sourceY+(front?rx*.08f:0f)-phase*rise;
            float r=rx*(.035f+.09f*strength)*(.65f+phase);
            float envelope=(float)Math.sin(phase*Math.PI);
            float opacity=fade*density*envelope*envelope;
            int mist=fadeBy(Glyph.withAlpha(0xFFFFF1DC,(int)(25+40*strength)),opacity);
            p.fillEllipse(vent+drift,y,r*1.4f,r*.85f,mist);
            p.fillEllipse(vent+drift-side*r*.65f,y-r*.48f,r,r*.85f,mist);
            float[] curl=new float[16];
            for(int j=0;j<8;j++) {
                float t=j/7f;
                curl[j*2]=vent+drift+(float)Math.sin(t*5f+phase*4f+i)*r*.50f;
                curl[j*2+1]=y+t*r*2.5f;
            }
            p.polyline(curl,fadeBy(Glyph.withAlpha(0xFFFFFAF0,70),opacity),rx*(.010f+.015f*strength));
        }
        if(pulse>0f)for(int side=-1;side<=1;side+=2) {
            if((side<0)!=front)continue;
            float age=1f-pulse;
            float x=cx+side*rx*(.72f+age*.12f),y=sourceY-age*rise*.45f;
            float r=rx*(.055f+age*.16f)*(.65f+.35f*strength);
            int color=fadeBy(Glyph.withAlpha(0xFFFFF6E5,(int)(90*pulse)),fade);
            p.fillEllipse(x,y,r*1.5f,r,color);
            p.fillEllipse(x+side*r*.8f,y-r*.35f,r,r*.8f,color);
        }
    }

    /** A soft white veil clears while the existing prize animation climbs out. */
    static float releaseWash(float age) {
        if(age<=0f || age>=1.20f)return 0f;
        float v=age<.28f ? age/.28f : age<.42f ? 1f : (1.20f-age)/.78f;
        return .74f*v*v*(3f-2f*v);
    }

    static void releaseSteam(Painter p,float cx,float cy,float rx,float w,float h,float age,float fade) {
        if(age<=0f || age>=1.5f || fade<=0f)return;
        p.fillRect(0,0,w,h,fadeBy(Glyph.withAlpha(0xFFFFFFFF,(int)(255*releaseWash(age))),fade));
        // Stagger broad mist rings; their feathered edges dissolve before the reveal ends.
        for(int i=0;i<5;i++) {
            float t=(age-i*.07f)/1.15f;
            if(t<=0f || t>=1f)continue;
            float ease=1f-(1f-t)*(1f-t);
            float radius=rx*.65f+ease*h*(.58f+i*.045f);
            float alpha=(float)Math.sin(Math.PI*t)*(1f-t)*fade;
            float width=rx*(.18f+i*.018f)*(1f+t);
            int white=Glyph.mix(0xFFFFF3DF,0xFFFFFFFF,i/4f);
            p.strokeCircle(cx,cy,radius,fadeBy(Glyph.withAlpha(white,34),alpha),width*2.1f);
            p.strokeCircle(cx,cy,radius,fadeBy(Glyph.withAlpha(white,115),alpha),width);
            for(int k=0;k<16;k++) {
                double a=k*Math.PI/8+i*.47;
                float x=cx+(float)Math.cos(a)*radius,y=cy+(float)Math.sin(a)*radius;
                p.fillEllipse(x,y,width*(.65f+.15f*(k%3)),width*.65f,
                        fadeBy(Glyph.withAlpha(white,45),alpha));
            }
        }
    }

    /**
     * Far wall and interior. Everything the contents should sit in front of.
     *
     * @param rimY  y of the rim line, the widest part
     * @param baseY y of the bottom of the basket
     * @param rx    half-width at the rim
     * @param ry    how deep the rim ellipse looks; the perspective squash
     * @param baseRx half-width at the base, smaller because the basket tapers
     * @param flash 0..1 press flash, which lightens the shading rather than the fill
     */
    static void back(Painter p, float cx, float rimY, float baseY, float rx, float ry,
            float baseRx, int body, float flash, float fade) {
        int fill = fadeBy(body, fade);
        p.fillPoly(barrel(cx, rimY, baseY, rx, baseRx, ry * 0.86f), fill);
        p.fillEllipse(cx, rimY, rx, ry, fill);

        // The opening. Inset by the wall thickness and darkened, so there is a visible lip
        // all the way round and the contents have something to be recessed into.
        int hollow = Glyph.mix(body, 0xFF000000, 0.62f - 0.30f * flash);
        p.fillEllipse(cx, rimY, rx * 0.87f, ry * 0.78f, fadeBy(hollow, fade));
        // Far side of the interior catches a little light, which is what tells the eye the
        // hollow is a bowl rather than a hole.
        p.fillPoly(arc(cx, rimY + ry * 0.06f, rx * 0.80f, ry * 0.62f, true),
                fadeBy(Glyph.withAlpha(Glyph.mix(body, 0xFF000000, 0.30f), 170), fade));
    }

    /**
     * Near wall, drawn over the contents: the front half of the rim lip, the wall below it,
     * and the woven slats on it.
     */
    static void front(Painter p, float cx, float rimY, float baseY, float rx, float ry,
            float baseRx, int body, float flash, float fade) {
        float baseRy = ry * 0.86f;
        int fill = fadeBy(body, fade);
        p.fillPoly(wall(cx, rimY, baseY, rx * 0.87f, ry * 0.78f, baseRx, baseRy), fill);
        // Uniform shade over the whole near wall, lifted by a press. Applied as one fill on
        // one polygon: laying it over the parts separately would double-blend at the seams.
        p.fillPoly(wall(cx, rimY, baseY, rx * 0.87f, ry * 0.78f, baseRx, baseRy),
                fadeBy(Glyph.withAlpha(0xFF000000, (int) (34 * (1f - flash))), fade));

        // Rim lip: the front of the ellipse, brighter than the wall under it.
        p.fillPoly(band(cx, rimY, rx, ry, rx * 0.87f, ry * 0.78f),
                fadeBy(Glyph.mix(body, 0xFFFFFFFF, 0.18f), fade));

        // Woven slats, following the wall's curve. Three, at fractions of the way down, each
        // an arc rather than a straight bar — a straight one gives the perspective away.
        for (int k = 1; k <= 3; k++) {
            float t = k / 4f;
            float y = rimY + (baseY - rimY) * t;
            float wrx = rx * 0.87f + (baseRx - rx * 0.87f) * t;
            float wry = ry * 0.78f + (baseRy - ry * 0.78f) * t;
            p.fillPoly(slat(cx, y, wrx * 0.985f, wry, ry * 0.15f),
                    fadeBy(Glyph.withAlpha(BAMBOO_DARK, 105), fade));
        }
    }

    /**
     * The lid: a flat woven disc with a low edge and a knob, in the rim's perspective.
     *
     * Flat, not domed. A dome shallow enough to sit inside the disc's own ellipse is invisible
     * from this angle, and one tall enough to show made the lid taller than the basket and
     * read as a sun hat. A real bamboo lid is a flat disc anyway.
     */
    static void lid(Painter p, float cx, float lidY, float rx, float ry, int col, float fade) {
        int fill = fadeBy(col, fade);
        // Edge thickness: the same disc dropped a little, so the top face covers its back half.
        float t = ry * 0.34f;
        p.fillEllipse(cx, lidY + t, rx, ry, fadeBy(Glyph.mix(col, 0xFF000000, 0.34f), fade));
        p.fillPoly(slat(cx, lidY, rx, ry, t), fadeBy(Glyph.mix(col, 0xFF000000, 0.34f), fade));
        p.fillEllipse(cx, lidY, rx, ry, fill);

        // Weave, as concentric shades rather than stroked rings: a thin stroked ellipse comes
        // out of the rasterizer looking dotted, because it draws round-capped segments.
        p.fillEllipse(cx, lidY, rx * 0.76f, ry * 0.76f,
                fadeBy(Glyph.mix(col, BAMBOO_DARK, 0.16f), fade));
        p.fillEllipse(cx, lidY, rx * 0.46f, ry * 0.46f, fill);
        // Knob, sitting on the top face just above its centre.
        p.fillEllipse(cx, lidY - ry * 0.12f, rx * 0.17f, ry * 0.26f,
                fadeBy(Glyph.mix(col, 0xFFFFFFFF, 0.14f), fade));
    }

    // ---- geometry -----------------------------------------------------------

    /**
     * Half of an ellipse, as a closed polygon on its chord.
     *
     * @param upper true for the far half (above the centre line), false for the near half
     */
    private static float[] arc(float cx, float cy, float rx, float ry, boolean upper) {
        float[] pts = new float[SEGS * 2];
        for (int k = 0; k < SEGS; k++) {
            // Left to right along the near half; right to left along the far one, so both
            // wind the same way and neither comes out as a bow tie.
            double a = upper ? Math.PI + Math.PI * k / (SEGS - 1.0)
                    : Math.PI - Math.PI * k / (SEGS - 1.0);
            pts[k * 2] = cx + rx * (float) Math.cos(a);
            pts[k * 2 + 1] = cy + ry * (float) Math.sin(a);
        }
        return pts;
    }

    /** Outer silhouette: rim chord, down the tapering sides, round the base. */
    private static float[] barrel(float cx, float rimY, float baseY, float rimRx, float baseRx,
            float baseRy) {
        float[] pts = new float[(SEGS + 2) * 2];
        pts[0] = cx - rimRx;
        pts[1] = rimY;
        for (int k = 0; k < SEGS; k++) {
            double a = Math.PI - Math.PI * k / (SEGS - 1.0);
            pts[(k + 1) * 2] = cx + baseRx * (float) Math.cos(a);
            pts[(k + 1) * 2 + 1] = baseY + baseRy * (float) Math.sin(a);
        }
        pts[(SEGS + 1) * 2] = cx + rimRx;
        pts[(SEGS + 1) * 2 + 1] = rimY;
        return pts;
    }

    /** The near wall: the opening's front arc, down to the base's front arc. */
    private static float[] wall(float cx, float rimY, float baseY, float inRx, float inRy,
            float baseRx, float baseRy) {
        float[] pts = new float[SEGS * 4];
        for (int k = 0; k < SEGS; k++) {
            double a = Math.PI - Math.PI * k / (SEGS - 1.0);
            pts[k * 2] = cx + inRx * (float) Math.cos(a);
            pts[k * 2 + 1] = rimY + inRy * (float) Math.sin(a);
            // Base arc walked back the other way, so the two join into one ring segment.
            double b = Math.PI * k / (SEGS - 1.0);
            pts[(SEGS + k) * 2] = cx + baseRx * (float) Math.cos(b);
            pts[(SEGS + k) * 2 + 1] = baseY + baseRy * (float) Math.sin(b);
        }
        return pts;
    }

    /** One woven slat: the near arc at {@code y}, and the same arc {@code h} lower. */
    private static float[] slat(float cx, float y, float rx, float ry, float h) {
        float[] pts = new float[SEGS * 4];
        for (int k = 0; k < SEGS; k++) {
            double a = Math.PI - Math.PI * k / (SEGS - 1.0);
            pts[k * 2] = cx + rx * (float) Math.cos(a);
            pts[k * 2 + 1] = y + ry * (float) Math.sin(a);
            double b = Math.PI * k / (SEGS - 1.0);
            pts[(SEGS + k) * 2] = cx + rx * (float) Math.cos(b);
            pts[(SEGS + k) * 2 + 1] = y + h + ry * (float) Math.sin(b);
        }
        return pts;
    }

    /** The front of the lip between two concentric ellipses. */
    private static float[] band(float cx, float cy, float rx, float ry, float inRx,
            float inRy) {
        float[] pts = new float[SEGS * 4];
        for (int k = 0; k < SEGS; k++) {
            double a = Math.PI - Math.PI * k / (SEGS - 1.0);
            pts[k * 2] = cx + rx * (float) Math.cos(a);
            pts[k * 2 + 1] = cy + ry * (float) Math.sin(a);
            double b = Math.PI * k / (SEGS - 1.0);
            pts[(SEGS + k) * 2] = cx + inRx * (float) Math.cos(b);
            pts[(SEGS + k) * 2 + 1] = cy + inRy * (float) Math.sin(b);
        }
        return pts;
    }
}
