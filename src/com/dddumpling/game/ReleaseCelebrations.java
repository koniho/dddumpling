package com.dddumpling.game;

/** Release-book rank climbs and Survival cheers; no gameplay or score side effects. */
final class ReleaseCelebrations extends Draw {
    static boolean active(int id) {
        return id==ReleaseChange.LEADERBOARD || id==ReleaseChange.SURVIVAL_CHEER;
    }
    private static float ease(float t) {
        t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);
    }
    static void leaderboard(Painter p,float x,float y,float r,float time) {
        float t=time%4f,climb=ease((t-.3f)/1.15f);
        float rowY=y+r*(.58f-1.16f*climb);
        // The overtaken rows slide down as the highlighted score climbs past them.
        for(int row=0;row<2;row++) {
            float moved=ease((climb-(row==0?.50f:0f))/.5f);
            float yy=y+r*(-.58f+row*.58f+moved*.58f);
            p.line(x-r*.70f,yy,x+r*.60f,yy,0xFF645B83,r*.20f);
            p.fillCircle(x-r*.70f,yy,r*.10f,INK_DIM);
        }
        float arrival=ease((t-1.35f)/.25f)*(1-ease((t-1.8f)/.6f));
        p.line(x-r*.75f,rowY,x+r*.67f,rowY,Glyph.withAlpha(GOLD,45),r*(.34f+arrival*.14f));
        p.line(x-r*.70f,rowY,x+r*.62f,rowY,GOLD,r*.21f);
        p.fillCircle(x-r*.70f,rowY,r*.19f,BG);
        p.text(climb<.25f?"3":climb<.75f?"2":"1",x-r*.70f,rowY+r*.13f,r*.36f,GOLD,Painter.CENTER,true);
        p.fillPoly(star(x+r*.64f,rowY,r*(.24f+arrival*.09f),r*.115f,5,0),GOLD);
        for(int side=-1;side<=1;side+=2) {
            float sx=x+side*r*.57f,sy=rowY-r*(.30f+arrival*.20f);
            p.fillPoly(star(sx,sy,r*.11f*arrival,r*.035f*arrival,4,0),GOLD);
        }
    }
    static void cheer(Painter p,float x,float y,float r,float time) {
        float t=time%4f,raise=ease((t-.22f)/.23f)*(1-.65f*ease((t-1.8f)/.6f));
        float jump=(float)Math.sin(Math.PI*ease((t-.24f)/.8f));
        float second=(float)Math.sin(Math.PI*ease((t-1.18f)/.60f))*.55f;
        float hop=Math.max(jump,second),cy=y+r*(.33f-hop*.34f);
        float squash=1+.17f*(1-ease(t/.25f))-.13f*hop;
        int body=0xFFFFCF61;
        p.fillEllipse(x,y+r*.88f,r*(.45f-hop*.12f),r*.07f,0x40302544);
        for(int side=-1;side<=1;side+=2) {
            float handX=x+side*r*(.65f+.10f*raise);
            float handY=cy+r*(.18f-.73f*raise);
            p.line(x+side*r*.35f,cy+r*.04f,handX,handY,0xFFC58A41,r*.12f);
            p.line(x+side*r*.35f,cy+r*.02f,handX,handY,body,r*.08f);
            p.fillCircle(handX,handY,r*.13f,body);
        }
        Painter squish=new RunCompanion.Squash(p,x,cy,squash);
        Kawaii.dumplingBody(squish,x,cy,r*.49f,r*.49f,body);
        // Closed happy eyes and an open mouth make the cheer readable without words.
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.17f;
            squish.arc(ex,cy-r*.025f,r*.085f,r*.07f,190,160,0xFF65434F,r*.035f);
            squish.fillEllipse(x+side*r*.30f,cy+r*.12f,r*.09f,r*.05f,0xB3F5859D);
        }
        squish.fillEllipse(x,cy+r*.16f,r*.11f,r*(.07f+.08f*raise),0xFF65434F);
        squish.fillEllipse(x,cy+r*.21f,r*.07f,r*.035f,0xFFFF8FAD);
        float burst=ease((t-.3f)/.75f),fade=1-ease((t-1.2f)/.9f);
        for(int i=0;i<10;i++) {
            float a=i*.6283185f,dist=r*(.48f+burst*.52f);
            float sx=x+(float)Math.cos(a)*dist,sy=y+(float)Math.sin(a)*dist*.74f;
            p.fillPoly(star(sx,sy,r*.075f*fade,r*.03f*fade,4,a+t),
                    Glyph.withAlpha(Glyph.COLOR[i%6],(int)(210*fade*burst)));
        }
        float pop=(float)Math.sin(Math.PI*ease((t-.22f)/.5f));
        p.text("0:30",x,y-r*.73f,r*(.46f+.10f*pop),GOLD,Painter.CENTER,true);
    }
}
