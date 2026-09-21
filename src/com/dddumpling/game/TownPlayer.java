package com.dddumpling.game;

/** The broad, pleated Adventure Dumpling used as the player's town avatar. */
final class TownPlayer {
    private static final int BODY = 0xFFFFE1A0;
    private static final int BODY_SHADE = 0xFFE8BD75;
    private static final int INK = 0xFF3A2E4F;
    private static final int CHEEK = 0xBFFF4D6B;
    private static final int BOOT = 0xFF795840;
    private static final int FELT = 0xFFC59A62;
    private static final int HAT_BAND = 0xFF795840;

    private TownPlayer() {}

    /** Draws at a path ground point; hop lifts the character while its shadow stays planted. */
    static void draw(Painter p, GameCore c, Layout L, float x, float ground, float hop,
            float speed) {
        float r = L.w * .078f;
        float touch = c.town.motion(x, ground - L.w * .07f, L.w * .18f);
        // Rendering only: keep the path/input anchor fixed while a nearby meadow tap gives the
        // traveler a small delighted hop and side-to-side spring.
        hop += Math.max(0f, touch) * L.w * .026f;
        x += touch * L.w * .005f;
        float moving = Math.min(1f, Math.abs(speed) * 3f);
        float stride = (float) Math.sin(c.town.bounceClock) * moving;
        float cy = ground - hop - r * .62f;
        float footY = ground - hop - r * .04f;

        p.fillEllipse(x, ground + r * .04f, r * .70f, r * .15f,
                Glyph.withAlpha(INK, 42));
        for (int side = -1; side <= 1; side += 2) {
            float fx = x + side * r * .34f + stride * side * r * .11f;
            p.fillEllipse(fx, footY, r * .23f, r * .12f, BOOT);
        }

        // A low crescent with a scalloped, pleated crown. This deliberately avoids the round
        // pinched-bun silhouette of Kawaii.DUMPLING, which reads as a bao at town scale.
        p.fillPoly(smooth(new float[] {
                x-r*1.06f,cy+r*.14f,
                x-r*1.01f,cy-r*.05f,
                x-r*.93f,cy-r*.22f,
                x-r*.82f,cy-r*.37f,
                x-r*.68f,cy-r*.49f,
                x-r*.55f,cy-r*.59f,
                x-r*.42f,cy-r*.51f,
                x-r*.28f,cy-r*.66f,
                x-r*.13f,cy-r*.55f,
                x+r*.02f,cy-r*.69f,
                x+r*.18f,cy-r*.55f,
                x+r*.34f,cy-r*.66f,
                x+r*.49f,cy-r*.51f,
                x+r*.64f,cy-r*.58f,
                x+r*.78f,cy-r*.45f,
                x+r*.91f,cy-r*.29f,
                x+r*1.01f,cy-r*.08f,
                x+r*1.06f,cy+r*.14f,
                x+r*.98f,cy+r*.30f,
                x+r*.83f,cy+r*.44f,
                x+r*.60f,cy+r*.55f,
                x+r*.31f,cy+r*.62f,
                x,cy+r*.65f,
                x-r*.31f,cy+r*.62f,
                x-r*.60f,cy+r*.55f,
                x-r*.83f,cy+r*.44f,
                x-r*.98f,cy+r*.30f
        }, 2), BODY);
        p.polyline(new float[] {x-r*.96f,cy+r*.13f,x-r*.72f,cy+r*.42f,
                x-r*.35f,cy+r*.57f,x,cy+r*.61f,x+r*.35f,cy+r*.57f,
                x+r*.72f,cy+r*.42f,x+r*.97f,cy+r*.14f}, BODY_SHADE, r*.055f);

        int fold = Glyph.withAlpha(INK, 55);
        p.polyline(new float[] {x-r*.45f,cy-r*.57f,x-r*.34f,cy-r*.34f,
                x-r*.27f,cy-r*.18f}, fold, r*.045f);
        p.polyline(new float[] {x-r*.14f,cy-r*.64f,x-r*.08f,cy-r*.35f,
                x-r*.05f,cy-r*.18f}, fold, r*.045f);
        p.polyline(new float[] {x+r*.17f,cy-r*.65f,x+r*.12f,cy-r*.36f,
                x+r*.10f,cy-r*.18f}, fold, r*.045f);
        p.polyline(new float[] {x+r*.47f,cy-r*.58f,x+r*.36f,cy-r*.34f,
                x+r*.29f,cy-r*.18f}, fold, r*.045f);
        p.fillEllipse(x-r*.57f, cy-r*.03f, r*.18f, r*.25f, 0x70FFFFFF);

        face(p, x, cy, r);
        hat(p, x, cy, r);
    }

    private static void face(Painter p, float x, float y, float r) {
        for (int side = -1; side <= 1; side += 2) {
            float ex=x+side*r*.35f;
            p.polyline(new float[] {ex-r*.13f,y+r*.05f,ex,y-r*.08f,
                    ex+r*.13f,y+r*.05f}, INK, r*.065f);
            p.fillCircle(x+side*r*.53f,y+r*.25f,r*.11f,CHEEK);
        }
        p.polyline(new float[] {x-r*.20f,y+r*.28f,x-r*.10f,y+r*.38f,
                x,y+r*.28f,x+r*.10f,y+r*.38f,x+r*.20f,y+r*.28f},
                INK,r*.055f);
    }

    private static void hat(Painter p, float x, float y, float r) {
        p.fillPoly(new float[] {x-r*.48f,y-r*.70f,x-r*.34f,y-r*1.24f,
                x-r*.08f,y-r*1.17f,x+r*.25f,y-r*1.30f,
                x+r*.43f,y-r*.70f}, FELT);
        p.fillEllipse(x,y-r*.76f,r*.46f,r*.10f,HAT_BAND);
        p.fillEllipse(x,y-r*.68f,r*.77f,r*.13f,FELT);
    }

    /** Two light Chaikin passes turn the sampled crescent into steamed-dough curves. */
    private static float[] smooth(float[] points, int passes) {
        float[] out = points;
        for (int pass = 0; pass < passes; pass++) {
            int count = out.length / 2;
            float[] next = new float[out.length * 2];
            for (int i = 0; i < count; i++) {
                int a = i * 2, b = ((i + 1) % count) * 2, at = i * 4;
                next[at] = out[a] * .75f + out[b] * .25f;
                next[at + 1] = out[a + 1] * .75f + out[b + 1] * .25f;
                next[at + 2] = out[a] * .25f + out[b] * .75f;
                next[at + 3] = out[a + 1] * .25f + out[b + 1] * .75f;
            }
            out = next;
        }
        return out;
    }
}
