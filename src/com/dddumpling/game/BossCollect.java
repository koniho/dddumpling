package com.dddumpling.game;

/** Friendly pocket-sized boss portraits and their victory welcome. */
final class BossCollect extends Draw {
    static final float REVEAL_TIME = 6.8f;

    private BossCollect() {}

    static void draw(Painter p, int boss, float x, float y, float r, float clock,
            boolean known, float fade) {
        draw(p,boss,x,y,r,clock,known,fade,-1,0f);
    }
    static void draw(Painter p,int boss,float x,float y,float r,float clock,boolean known,float fade,int mood,float look) {
        draw(p,boss,x,y,r,clock,known,fade,mood,look,false);
    }
    static void draw(Painter p,int boss,float x,float y,float r,float clock,boolean known,float fade,
            int mood,float look,boolean ninja) {
        int i = Collect.BOSS_FIRST + boss;
        int body = fadeBy(known ? Collect.BODY[i] : 0xFF393054, fade);
        int cream = fadeBy(known ? Collect.ACCENT[i] : 0xFF393054, fade);
        int ink = fadeBy(0xFF382841, fade);
        float faceY = y, faceR = r * 0.6f;
        if (boss == Boss.OCTOPUS) {
            for (int layer = 0; layer < Boss.OCTO_ARMS; layer++) {
                int arm = layer % 2 == 0 ? layer / 2 : Boss.OCTO_ARMS - 1 - layer / 2;
                float endX = x + (arm - 3.5f) * r * 0.28f;
                float endY = y + r * (0.64f + 0.14f * (float) Math.sin(clock * 2f + arm));
                float startX = x + (arm - 3.5f) * r * 0.10f;
                p.line(startX, y + r * 0.15f, endX, endY, ink, r * 0.23f);
                p.line(startX, y + r * 0.15f, endX, endY, body, r * 0.18f);
                p.fillCircle(endX, endY, r * 0.095f, body);
                p.fillCircle(endX, endY - r * 0.035f, r * 0.042f, cream);
            }
            p.fillEllipse(x, y - r * 0.34f, r * 0.61f, r * 0.66f, body);
            p.fillEllipse(x, y + r * 0.05f, r * 0.46f, r * 0.40f, body);
            faceY -= r * 0.25f;
            faceR = r * 0.45f;
        } else if (boss == Boss.MUSHROOM) {
            p.fillEllipse(x, y + r * 0.35f, r * 0.29f, r * 0.57f, cream);
            float[] cap = new float[66];
            for (int k = 0; k <= 32; k++) {
                float a = (float) Math.PI + (float) Math.PI * k / 32f;
                cap[k * 2] = x + (float) Math.cos(a) * r;
                cap[k * 2 + 1] = y - r * 0.08f + (float) Math.sin(a) * r * 0.90f;
            }
            p.fillPoly(cap, body);
            p.fillEllipse(x, y - r * 0.05f, r, r * 0.18f, cream);
            if (known) {
                for (int k = 0; k < 5; k++) {
                    float xx = x + (k - 2) * r * 0.31f;
                    float yy = y - r * (0.38f + (k % 2) * 0.25f);
                    p.fillEllipse(xx, yy, r * 0.105f, r * 0.075f, fadeBy(INK, fade));
                }
                p.fillPoly(new float[] {x-r*.3f,y+r*.1f,x+r*.3f,y+r*.1f,
                        x+r*.4f,y+r*.26f,x-r*.4f,y+r*.26f}, cream);
            }
            faceY += r * 0.48f;
            faceR = r * 0.27f;
        } else if (boss == Boss.SPLITTER) {
            // Match Dark Divide's pale rim so the purple body clears dark backgrounds.
            int rim = known ? fadeBy(Glyph.mix(Collect.BODY[i], 0xFFFFFFFF, .52f), fade) : 0;
            Shape.gelCube(p, x, y, r * 0.95f, body, cream,
                    known ? fade : 0f, clock, known, rim);
            faceR = r * 0.56f;
        } else {
            p.fillEllipse(x, y + r * 0.05f, r * 0.86f, r * 0.82f, body);
            if (boss == Boss.SLIME) {
                for (int k = 0; k < 5; k++) p.fillCircle(x+(k-2)*r*.34f,y+r*.60f,r*.22f,body);
                p.fillEllipse(x-r*.30f,y-r*.42f,r*.22f,r*.13f,cream);
            }
        }
        if (known) {
            if(ninja) Trinket.ninjaMask(p,x,faceY,faceR,fade);
            if(mood<0) face(p, x, faceY, faceR, clock, fade);
            else Trinket.reactionFace(p,x,faceY,faceR,clock,fade,mood,look);
        }
        if (!known) p.text("?",x,y+r*.25f,type(r*.65f),fadeBy(INK_DIM,fade),Painter.CENTER,true);
    }

    private static void face(Painter p, float x, float y, float r, float clock, float fade) {
        int ink = fadeBy(0xFF382841, fade);
        boolean blink = Math.sin(clock * 1.1f) > 0.992f;
        for (int side = -1; side <= 1; side += 2) {
            float xx = x + side * r * 0.39f;
            p.fillEllipse(xx,y-r*.06f,r*.115f,r*(blink ? .035f : .16f),ink);
            if (!blink) p.fillCircle(xx-r*.025f,y-r*.13f,r*.035f,fadeBy(INK,fade));
            p.fillEllipse(x+side*r*.66f,y+r*.15f,r*.16f,r*.085f,fadeBy(0x88FF8EAD,fade));
        }
        p.polyline(new float[] {x-r*.15f,y+r*.18f,x,y+r*.28f,x+r*.15f,y+r*.18f},ink,r*.065f);
    }

    static void celebration(Painter p, GameCore c, Layout L) {
        float age = REVEAL_TIME - c.bonusTimer;
        float fade = Math.min(1f, Math.min(age / 0.35f, c.bonusTimer / 0.6f));
        float x = L.w / 2f, y = L.h * 0.44f, r = Math.min(L.w * 0.18f, L.h * 0.10f);
        int tint = Collect.BODY[c.prize];
        for (int k = 0; k < 12; k++) {
            float a = k * Softbody.TAU / 12f + c.clock * 0.16f;
            p.fillPoly(new float[] {x,y,x+(float)Math.cos(a)*r*3f,y+(float)Math.sin(a)*r*3f,
                    x+(float)Math.cos(a+.10f)*r*3f,y+(float)Math.sin(a+.10f)*r*3f},
                    fadeBy(Glyph.withAlpha(tint,24),fade));
        }
        for (int k = 0; k < 48; k++) {
            float fall = (age * (0.13f + hash(k+90)*0.12f) + hash(k+17)) % 1f;
            float xx = L.w * hash(k+340) + (float)Math.sin(age*2f+k)*L.unit;
            float yy = L.h * (0.16f + fall * 0.62f);
            int col = k % 3 == 0 ? GOLD : k % 3 == 1 ? tint : ROSE;
            p.fillPoly(star(xx,yy,L.unit*.15f,L.unit*.07f,4,age+k),fadeBy(col,fade*.85f));
        }
        float pop = Math.min(1f, age / 0.85f);
        float scale = pop * (1f + (float)Math.sin(pop*Math.PI)*0.18f);
        p.fillEllipse(x,y+r*1.16f,r*.95f,r*.14f,fadeBy(Glyph.withAlpha(tint,45),fade));
        Trinket.draw(p,c.prize,x,y+(1f-pop)*r*1.5f-(float)Math.sin(age*2f)*r*.035f,
                r*scale,c.clock,true,fade);
        p.text("BOSS BEFRIENDED!",x,L.h*.22f,type(L.unit*1.02f),fadeBy(GOLD,fade),Painter.CENTER,true);
        p.text(Boss.NAMES[c.prize-Collect.BOSS_FIRST],x,L.h*.27f,type(L.unit*.69f),fadeBy(INK,fade),Painter.CENTER,true);
        p.text(Collect.NAME[c.prize],x,L.h*.60f,type(L.unit*1.08f),fadeBy(INK,fade),Painter.CENTER,true);
        p.text(c.prizeNew ? "JOINS YOUR COLLECTION" : "A FRIEND RETURNS",x,L.h*.64f,
                type(L.unit*.62f),fadeBy(GOLD,fade),Painter.CENTER,true);
        int[] mates = new int[5];
        int n = Parade.companions(c,mates);
        for (int k = 0; k < n; k++) Trinket.draw(p,mates[k],x+(k-(n-1)*.5f)*L.w*.13f,
                L.h*.74f-(float)Math.abs(Math.sin(age*4f+k))*L.unit*.35f,
                L.w*.045f,c.clock,true,fade);
    }
}
