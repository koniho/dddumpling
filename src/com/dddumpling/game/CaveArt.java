package com.dddumpling.game;

/** Warm stone uses the same soft silhouettes and layered fills as the existing lands. */
final class CaveArt extends Draw {
    static final int DARK = 0xFF1C110D, ROCK = 0xFF4C3021, MID = 0xFF765038,
            LIGHT = 0xFFA77B50, FLOOR = 0xFF68472F, LAMP = 0xFFFFD58A;
    private CaveArt() {}
    static void stone(Painter p, float x, float y, float r, int seed, int a) {
        float[] pts = new float[20];
        for (int i = 0; i < 10; i++) {
            float angle = i * 6.283185f / 10f;
            float radius = r * (.82f + .18f * hash(seed + i * 17));
            pts[i * 2] = x + (float)Math.cos(angle) * radius;
            pts[i * 2 + 1] = y + (float)Math.sin(angle) * radius * .75f;
        }
        p.fillPoly(pts, Glyph.withAlpha(DARK, a));
        for (int i = 0; i < 10; i++) {
            pts[i * 2] = x + (pts[i * 2] - x) * .93f;
            pts[i * 2 + 1] = y - r * .07f + (pts[i * 2 + 1] - y) * .88f;
        }
        p.fillPoly(pts, Glyph.withAlpha(MID, a));
        for (int i = 0; i < 10; i++) {
            pts[i * 2] = x - r * .07f + (pts[i * 2] - x) * .87f;
            pts[i * 2 + 1] = y - r * .12f + (pts[i * 2 + 1] - y) * .78f;
        }
        p.fillPoly(pts, Glyph.withAlpha(ROCK, a));
        p.fillEllipse(x - r * .22f, y - r * .41f, r * .31f, r * .045f, Glyph.withAlpha(LIGHT, a / 2));
    }
    static void tumbling(Painter p,float x,float y,float r,int seed,int alpha,float rotation) {
        float[] pts=new float[14];
        for(int i=0;i<7;i++){
            float a=i*6.283185f/7,rr=r*(.72f+.28f*hash(seed+i*17));
            float dx=(float)Math.cos(a)*rr,dy=(float)Math.sin(a)*rr*.8f;
            pts[i*2]=x+dx*(float)Math.cos(rotation)-dy*(float)Math.sin(rotation);
            pts[i*2+1]=y+dx*(float)Math.sin(rotation)+dy*(float)Math.cos(rotation);
        }
        p.fillPoly(pts,Glyph.withAlpha(MID,alpha));
        p.strokePoly(pts,Glyph.withAlpha(DARK,alpha),r*.10f);
        p.fillPoly(new float[]{x,y,pts[0],pts[1],pts[2],pts[3],pts[4],pts[5]},Glyph.withAlpha(LIGHT,alpha));
    }
    static void entrance(Painter p, float x, float y, float r, int a, boolean silhouette) {
        p.fillEllipse(x, y + r * .55f, r * 1.18f, r * .35f, Glyph.withAlpha(DARK, a));
        for (int i = 0; i < 7; i++) {
            float angle = (float)Math.PI * i / 6f;
            float bx = x + (float)Math.cos(angle) * r * .83f;
            float by = y + r * .5f - (float)Math.sin(angle) * r * 1.25f;
            if (silhouette) p.fillEllipse(bx, by, r * .43f, r * .45f, Glyph.withAlpha(0xFF9A8FAF, a));
            else stone(p, bx, by, r * .51f, i * 31, a);
        }
        if (!silhouette) lantern(p, x + r * .80f, y + r * .15f, r * .17f, a);
    }
    static void lantern(Painter p, float x, float y, float r, int a) {
        for (int i = 4; i > 0; i--) p.fillCircle(x, y, r * (1f + i * .7f), Glyph.withAlpha(LAMP, a / (10 + i * 5)));
        p.strokeCircle(x, y - r * 1.12f, r * .35f, Glyph.withAlpha(LIGHT, a), r * .15f);
        p.fillPoly(pill(x, y, r * .70f, r, 8), Glyph.withAlpha(ROCK, a));
        p.fillPoly(pill(x, y, r * .45f, r * .74f, 8), Glyph.withAlpha(LAMP, a));
        p.fillEllipse(x, y - r * .9f, r * .85f, r * .16f, Glyph.withAlpha(LIGHT, a));
    }
}
