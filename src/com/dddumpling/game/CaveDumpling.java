package com.dddumpling.game;

/** A normalized spring body keeps the same bounce at every screen size. */
final class CaveDumpling extends Draw {
    static final int COUNT = 6, CREAM = 0, RAINBOW = 1, GOLDEN = 2, SILVER = 3, SPARKLY = 4, PURPLE = 5;
    static final String[] NAMES = {"CREAM", "RAINBOW", "GOLDEN", "SILVER", "SPARKLY", "PURPLE"};
    static final int[] COLORS = {0xFFFFE5AB,0xFFFFA5BF,0xFFF4C552,0xFFCCD8E4,0xFF83DED1,0xFFB390EB};
    final Softbody body = new Softbody(Softbody.NODES, 731);
    float stride;
    CaveDumpling() { reset(); }
    void reset() { body.reset(0f,0f,1f); stride=0f; }
    void update(float dt, float distance) {
        stride += Math.max(0f,distance);
        if (stride >= .18f) {
            stride %= .18f;
            body.squash(.20f); body.shove(0f,-1f,1.6f);
        }
        body.update(dt);
    }
    float squash() { return Math.max(.88f,Math.min(1.14f,(float)Math.sqrt(body.squashAspect()))); }
    float lift() { return body.centreY(); }
    static int valid(int choice) { return choice >= 0 && choice < COUNT ? choice : -1; }
    static void draw(Painter p,int choice,float x,float y,float r,float squash,float clock) {
        draw(p,choice,x,y,r,squash,clock,false);
    }
    static void draw(Painter p,int choice,float x,float y,float r,float squash,float clock,boolean scared) {
        choice=valid(choice)<0 ? CREAM : choice;
        if (choice==RAINBOW) {
            int[] bands={0xFFFF9FAD,0xFFFFCA85,0xFFFFEA91,0xFFA5E6B8,0xFF9ECDF3,0xFFCEAEF0};
            for(int i=0;i<6;i++) {
                float left=x-r*1.3f+i*r*2.6f/6f;
                p.save();p.clipRect(left,y-r*1.5f,left+r*2.6f/6f,y+r*1.5f);
                character(p,x,y,r,bands[i],squash,clock,scared);p.restore();
            }
        } else character(p,x,y,r,COLORS[choice],squash,clock,scared);
        if(choice==GOLDEN || choice==SILVER) {
            float shine=.55f+.20f*(float)Math.sin(clock*1.8f);
            p.fillEllipse(x-r*.40f,y-r*.23f,r*.085f,r*.22f,Glyph.withAlpha(INK,(int)(shine*200)));
            p.fillEllipse(x+r*.58f,y+r*.24f,r*.06f,r*.13f,Glyph.withAlpha(INK,125));
        }
        if(choice==SPARKLY) for(int i=0;i<7;i++) {
            float angle=i*2.4f, reach=i%2==0 ? .72f : 1.13f;
            float sx=x+(float)Math.cos(angle)*r*reach,sy=y+(float)Math.sin(angle)*r*reach;
            float size=r*(.055f+.045f*(float)Math.sin(clock*3f+i));
            p.fillPoly(new float[]{sx,sy-size*1.8f,sx+size*.45f,sy,sx,sy+size*1.8f,sx-size*.45f,sy},INK);
        }
    }
    private static void character(Painter p,float x,float y,float r,int color,float squash,float clock,boolean scared) {
        Kawaii.draw(p,Kawaii.DUMPLING,x,y,r,color,squash,.7f);
        if(!scared)return;
        int ink=0xFF3A2E4F;
        p.fillEllipse(x,y+r*.15f,r*.70f,r*.47f,color);
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.31f;
            p.fillEllipse(ex,y-r*.02f,r*.21f,r*.26f,ink);
            p.fillEllipse(ex,y-r*.025f,r*.17f,r*.215f,0xFFFFFBEB);
            p.fillEllipse(ex,y+r*.015f,r*.065f,r*.105f,ink);
            p.line(ex-side*r*.17f,y-r*.26f,ex+side*r*.12f,y-r*.37f,ink,r*.06f);
            p.fillEllipse(x+side*r*.68f,y+r*(.10f+.10f*(float)Math.sin(clock*18)),r*.065f,r*.15f,0xFF9BD7FF);
        }
        p.fillEllipse(x,y+r*.39f,r*.20f,r*.24f,ink);
        p.fillEllipse(x,y+r*.51f,r*.13f,r*.065f,0xFFFF93AF);
    }

}
