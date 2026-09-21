package com.dddumpling.game;

/** Warm, toy-like landmarks placed in the meadow's scrolling world coordinates. */
final class TownAttractions extends Draw {
    private static final int INK = 0xFF294C3C;
    private static final int CREAM = 0xFFFFF5D9, CREAM_HI = 0xFFFFFFEF;
    private static final int GOLD = 0xFFF5B94D, GOLD_HI = 0xFFFFD777, GOLD_DARK = 0xFFD98B3D;
    private static final int WOOD = 0xFF936A48, WOOD_HI = 0xFFC59664;
    private static final int ROCK = 0xFF708776, ROCK_HI = 0xFFA2B39A;
    private static final int PINK = 0xFFFFA9BE, FLOWER = 0xFFFF8FAE;

    private TownAttractions() {}

    static void friend(Painter p, Town t, Layout L, float clock) {
        float at = .20f, x = TownScreen.pathX(at, L);
        float r = L.w * .052f, y = TownScreen.landmarkGround(at,L) - r*1.44f;
        float spring = t.motion(x, y + r * 1.44f, L.w * .28f);
        p.save();
        p.translate(r * (.025f * wave(clock, .83f, .8f) + .11f * spring),
                r * (.025f * wave(clock, 1.55f, .2f) + .15f * spring));
        plinth(p, x, y + r * 1.44f, r * 2.05f, r * .68f);

        if (t.archBuilt) {
            p.save();
            p.translate(r * (.025f * wave(clock, 1.1f, 2.2f) - .05f * spring),
                    -r * .035f * spring);
            flowerArch(p, x, y + r * .32f, r, clock);
            p.restore();
        }
        // A pink friend remains readable in front of the arch's warm cream and flowers.
        p.save();
        p.translate(-r * .018f * wave(clock, .96f, .4f),
                r * (.018f * wave(clock, 1.9f, 1.2f) - .06f * spring));
        p.fillCircle(x, y + r * .10f, r * 1.07f, Glyph.withAlpha(PINK, 42));
        Trinket.draw(p, 1, x, y, r * 1.02f, clock, true, 1f);
        p.restore();
        p.save();
        p.translate(r * (.018f * wave(clock, 1.25f, 2.6f) + .04f * spring),
                r * (.018f * wave(clock, 1.7f, 1.8f) + .07f * spring));
        plaque(p, x, y + r * 2.02f, r * 1.38f, r * .39f, "HELLO!", r * .42f);
        p.restore();

        if (t.targetPoi == Town.FRIEND)
            caret(p, x, y - r * 1.58f, r * .45f, 0xFFFFCE4A);
        p.restore();
    }

    static void slide(Painter p, Town t, Layout L, float clock) {
        float at = .66f, x = TownScreen.pathX(at, L);
        float ground = TownScreen.landmarkGround(at,L), r = L.w * .060f;
        float spring = t.motion(x, ground, L.w * .28f);
        p.save();
        p.translate(r * (.018f * wave(clock, .72f, 2.1f) + .10f * spring),
                r * (.018f * wave(clock, 1.35f, .6f) + .14f * spring));
        plinth(p, x + r * .06f, ground + r * .42f, r * 2.12f, r * .64f);

        float ladderX = x - r * .64f, top = ground - r * 2.75f;
        // Shadow, rails and rungs make the ladder feel built rather than painted onto the meadow.
        p.line(ladderX - r * .31f, top + r * .34f, ladderX - r * .31f, ground + r * .12f,
                Glyph.mix(WOOD, INK, .24f), r * .18f);
        p.line(ladderX + r * .31f, top + r * .34f, ladderX + r * .31f, ground + r * .12f,
                Glyph.mix(WOOD, INK, .24f), r * .18f);
        p.line(ladderX - r * .27f, top + r * .30f, ladderX - r * .27f, ground + r * .08f,
                WOOD_HI, r * .12f);
        p.line(ladderX + r * .27f, top + r * .30f, ladderX + r * .27f, ground + r * .08f,
                WOOD_HI, r * .12f);
        for (int i = 0; i < 6; i++) {
            float yy = top + r * (.68f + i * .43f);
            p.line(ladderX - r * .28f, yy, ladderX + r * .28f, yy, WOOD, r * .10f);
            p.line(ladderX - r * .23f, yy - r * .025f, ladderX + r * .23f, yy - r * .025f,
                    0x99E4BC78, r * .035f);
        }

        // Chunky platform with the same friendly star used throughout the game.
        p.fillPoly(pageShape(ladderX - r * .55f, top - r * .24f,
                ladderX + r * .55f, top + r * .62f, r * .16f), GOLD_DARK);
        p.fillPoly(pageShape(ladderX - r * .49f, top - r * .19f,
                ladderX + r * .49f, top + r * .52f, r * .13f), GOLD_HI);
        p.fillPoly(star(ladderX, top + r * .16f, r * .27f, r * .12f, 5, -.18f), CREAM_HI);

        // A broad rounded chute: dark underside, gold body, then a slim sunlit inner edge.
        float[] chute = {ladderX + r * .46f, top + r * .45f,
                x + r * .36f, top + r * .68f,
                x + r * .54f, top + r * 1.25f,
                x + r * .58f, top + r * 1.83f,
                x + r * .92f, top + r * 2.31f,
                x + r * 1.47f, ground + r * .03f};
        p.polyline(chute, GOLD_DARK, r * .84f);
        roundedJoints(p, chute, r * .42f, GOLD_DARK);
        p.polyline(chute, Glyph.mix(GOLD, GOLD_DARK, .20f), r * .72f);
        roundedJoints(p, chute, r * .36f, Glyph.mix(GOLD, GOLD_DARK, .20f));
        p.polyline(chute, GOLD, r * .62f);
        roundedJoints(p, chute, r * .305f, GOLD);
        float[] shine = {ladderX + r * .39f, top + r * .38f,
                x + r * .29f, top + r * .61f,
                x + r * .46f, top + r * 1.23f,
                x + r * .49f, top + r * 1.72f};
        p.polyline(shine, 0xBFFFF1AE, r * .10f);

        // Little support feet settle the attraction onto its cream toy base.
        p.line(ladderX - r * .32f, ground - r * .03f, ladderX - r * .48f, ground + r * .30f,
                WOOD, r * .12f);
        p.line(x + r * 1.33f, ground - r * .10f, x + r * 1.45f, ground + r * .27f,
                WOOD, r * .12f);
        p.save();
        p.translate(r * (.014f * wave(clock, 1.16f, 1.4f) - .035f * spring),
                r * (.015f * wave(clock, 1.7f, 2.4f) + .07f * spring));
        plaque(p, x + r * .18f, ground + r * .98f, r * 2.40f, r * .39f,
                "LAUNCH SLIDE", r * .33f);
        p.restore();

        if (t.targetPoi == Town.SLIDE_POI)
            caret(p, ladderX, top - r * .62f, r * .45f, 0xFFFFCE4A);
        p.restore();
    }

    static void slime(Painter p, Town t, Layout L, float clock) {
        float at = .88f, x = TownScreen.pathX(at, L);
        float ground = TownScreen.landmarkGround(at,L), r = L.w * .058f;
        float spring = t.motion(x, ground, L.w * .28f);
        p.save();
        p.translate(r * (.022f * wave(clock, .78f, 1.7f) + .11f * spring),
                r * (.021f * wave(clock, 1.48f, .9f) + .15f * spring));
        plinth(p, x, ground + r * .36f, r * 1.92f, r * .61f);

        p.save();
        p.translate(-r * .025f * spring, r * .035f * spring);
        rock(p, x - r * 1.26f, ground + r * .05f, r * .43f, .88f);
        rock(p, x + r * 1.18f, ground + r * .12f, r * .52f, 1.02f);
        rock(p, x + r * .72f, ground - r * .20f, r * .27f, .75f);
        p.restore();
        p.fillEllipse(x, ground + r * .10f, r * 1.05f, r * .25f,
                Glyph.withAlpha(0xFF3D7958, 58));
        // The collectible Slime supplies the canonical mascot; two highlights add meadow gloss.
        p.save();
        p.translate(r * .018f * wave(clock, .92f, 2.5f),
                r * (.025f * wave(clock, 1.82f, .4f) - .07f * spring));
        Trinket.draw(p, Collect.BOSS_FIRST, x, ground - r * .82f, r * .93f, clock, true, 1f);
        p.fillEllipse(x - r * .34f, ground - r * 1.25f, r * .18f, r * .10f, 0xAFFFFFFF);
        p.fillCircle(x - r * .50f, ground - r * 1.07f, r * .065f, 0xD9FFFFFF);
        p.restore();
        p.save();
        p.translate(r * (.015f * wave(clock, 1.18f, 1.9f) + .04f * spring),
                r * (.016f * wave(clock, 1.6f, 2.8f) + .07f * spring));
        plaque(p, x, ground + r * .93f, r * 1.80f, r * .39f,
                "SLIME FIGHT", r * .34f);
        p.restore();

        if (t.targetPoi == Town.SLIME_POI)
            caret(p, x, ground - r * 2.23f, r * .45f, 0xFFFFCE4A);
        p.restore();
    }

    private static float wave(float clock, float speed, float phase) {
        return (float)Math.sin(clock * speed + phase);
    }

    private static void flowerArch(Painter p, float x, float y, float r, float clock) {
        p.arc(x, y, r * 1.45f, r * 2.0f, 180f, 180f,
                Glyph.mix(WOOD, INK, .16f), r * .24f);
        p.arc(x, y - r * .02f, r * 1.43f, r * 1.98f, 180f, 180f, WOOD_HI, r * .13f);
        for (int i = 0; i < 11; i++) {
            double a = Math.PI + Math.PI * i / 10.0;
            float fx = x + (float) Math.cos(a) * r * 1.45f;
            float fy = y + (float) Math.sin(a) * r * 2.0f;
            float bob = r * .025f * (float) Math.sin(clock * 1.7f + i);
            flower(p, fx, fy + bob, r * (i % 3 == 0 ? .20f : .16f), i);
        }
        p.fillEllipse(x - r * 1.40f, y + r * .03f, r * .30f, r * .20f, 0xFF77AF67);
        p.fillEllipse(x + r * 1.40f, y + r * .03f, r * .30f, r * .20f, 0xFF77AF67);
    }

    private static void flower(Painter p, float x, float y, float r, int seed) {
        int color = seed % 3 == 0 ? FLOWER : seed % 3 == 1 ? 0xFFFFD77D : 0xFFFFB8CB;
        for (int i = 0; i < 5; i++) {
            double a = i * Softbody.TAU / 5f - Math.PI * .5;
            p.fillCircle(x + (float) Math.cos(a) * r * .56f,
                    y + (float) Math.sin(a) * r * .56f, r * .48f, color);
        }
        p.fillCircle(x, y, r * .36f, CREAM_HI);
    }

    private static void plinth(Painter p, float x, float y, float rx, float ry) {
        p.fillEllipse(x + rx * .05f, y + ry * .30f, rx * 1.02f, ry * .83f,
                Glyph.withAlpha(0xFF315E48, 48));
        p.fillEllipse(x, y, rx, ry, 0xFFEED9AF);
        p.fillEllipse(x, y - ry * .15f, rx * .94f, ry * .73f, CREAM);
        p.fillEllipse(x - rx * .27f, y - ry * .40f, rx * .28f, ry * .12f,
                Glyph.withAlpha(CREAM_HI, 185));
    }

    private static void plaque(Painter p, float x, float y, float rx, float ry,
            String label, float size) {
        p.fillPoly(pill(x + ry * .09f, y + ry * .16f, rx, ry, 14),
                Glyph.withAlpha(0xFF3B634D, 48));
        p.fillPoly(pill(x, y, rx, ry, 14), CREAM_HI);
        p.strokePoly(pill(x, y, rx, ry, 14), 0xFFDCC89D, Math.max(1f, ry * .10f));
        p.text(label, x, y + size * .34f, type(size), INK, Painter.CENTER, true);
    }

    private static void rock(Painter p, float x, float y, float r, float squash) {
        p.fillEllipse(x + r * .08f, y + r * .22f, r * 1.05f, r * .40f,
                Glyph.withAlpha(0xFF315E48, 44));
        p.fillPoly(new float[] {x-r*squash,y+r*.42f,x-r*.77f*squash,y-r*.28f,
                x-r*.26f*squash,y-r*.77f,x+r*.43f*squash,y-r*.62f,
                x+r*squash,y+r*.42f}, ROCK);
        p.fillPoly(new float[] {x-r*.66f*squash,y-r*.24f,x-r*.22f*squash,y-r*.66f,
                x+r*.28f*squash,y-r*.50f,x+r*.02f*squash,y-r*.08f}, ROCK_HI);
    }

    private static void roundedJoints(Painter p, float[] points, float radius, int color) {
        for (int i = 0; i < points.length; i += 2)
            p.fillCircle(points[i], points[i + 1], radius, color);
    }
}
