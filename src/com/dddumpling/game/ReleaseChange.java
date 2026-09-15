package com.dddumpling.game;

/** Shipped highlights and their shared list/popup illustrations. */
final class ReleaseChange extends Draw {
    static final int TRAVEL=0, STARS=1, BUGS=2, SHUFFLE=3, DISGUISE=4, SLIME=5,
            PAIR=6, FLEX=7, TEAM=8, NEWS=9, FLURRY=10, MISC=11;
    static final int[][] ITEMS=ReleaseContent.ITEMS;
    static boolean playable(int id) { return id==TRAVEL || id==SHUFFLE || id==PAIR; }
    static float artUnits(int id) { return id==TRAVEL ? 8f : id==PAIR ? 9f : id==SHUFFLE ? 9f : 5f; }
    static void icon(Painter p,int id,float x,float y,float r,float time) {
        if(id==MISC) {
            Skits.face(p,Kawaii.DUMPLING,x-r*.12f,y+r*.15f,r*.65f,255,1f,1f);
            for(int i=0;i<3;i++) {
                float sx=x+r*(i==0 ? -.72f : i==1 ? .08f : .72f);
                float sy=y-r*(i==1 ? .82f : .38f);
                float sr=r*(.20f+.025f*(float)Math.sin(time*2f+i));
                p.fillPoly(star(sx,sy,sr,sr*.28f,4,0f),i==1 ? GOLD : 0xFFBCE9DA);
            }
        } else if(id==NEWS) {
            powerHalo(p,x,y,r*.6f,time,Glyph.cycle(time*.7f),1f);
            ReleaseMascot.steamer(p,x,y,r*.85f,time);
        } else if(id==FLURRY) {
            for(int band=0;band<7;band++)
                p.strokeCircle(x,y,r*(.95f-band*.09f),Glyph.cycle(band*.85f),r*.08f);
        } else if(id==TRAVEL) {
            Lands.logo(p,0,x,y-r*.4f,r,255,time);
        } else if(id==STARS) {
            p.polyline(new float[]{x-r*.9f,y+r*.8f,x-r*.4f,y+r*.35f,x+r*.15f,y+r*.6f,x+r*.6f,y-r*.6f},0xFFAE9ADA,r*.14f);
            p.fillPoly(star(x+r*.35f,y-r*.35f,r*.65f,r*.32f,5,.1f),GOLD);
        } else if(id==BUGS) {
            int ink=0xFF574563;
            for(int side=-1;side<=1;side+=2) {
                for(int leg=0;leg<3;leg++)
                    p.line(x+side*r*.48f,y+r*(leg*.27f-.12f),x+side*r*.81f,y+r*(leg*.33f-.19f),ink,r*.08f);
                p.line(x+side*r*.23f,y-r*.53f,x+side*r*.42f,y-r*.87f,ink,r*.065f);
                p.fillCircle(x+side*r*.42f,y-r*.87f,r*.095f,0xFFECAAAD);
            }
            p.fillEllipse(x,y+r*.18f,r*.65f,r*.64f,0xFFED8A9E);
            p.arc(x,y+r*.18f,r*.08f,r*.58f,90,180,0xFFBD6581,r*.06f);
            for(int side=-1;side<=1;side+=2) {
                p.fillCircle(x+side*r*.32f,y+r*.16f,r*.115f,ink);
                p.fillCircle(x+side*r*.26f,y+r*.49f,r*.08f,ink);
            }
            p.fillEllipse(x,y-r*.36f,r*.49f,r*.36f,0xFFC5ABD9);
            for(int side=-1;side<=1;side+=2) {
                p.fillCircle(x+side*r*.17f,y-r*.40f,r*.067f,ink);
                p.fillEllipse(x+side*r*.30f,y-r*.27f,r*.08f,r*.04f,0xFFFFB5B9);
            }
            p.arc(x,y-r*.30f,r*.10f,r*.08f,0,180,ink,r*.035f);
        } else if(id==SHUFFLE) {
            p.fillPoly(Glyph.hex(x,y,r*.94f),0xFFAE9ADA);
            p.text("?",x,y+r*.48f,r*1.5f,GOLD,Painter.CENTER,true);
        } else if(id==DISGUISE) {
            Kawaii.incognito(p,Kawaii.STRAWBERRY,x-r*.42f,y,r*.63f,Glyph.COLOR[1],1f);
            Kawaii.incognito(p,Kawaii.CAT,x+r*.42f,y,r*.63f,Glyph.COLOR[2],1f);
        } else if(id==SLIME) {
            Kawaii.draw(p,Kawaii.BLOB,x,y,r*.8f,0xFF99C48E,1.1f,.7f);
            p.fillPoly(Glyph.hex(x+r*.45f,y+r*.42f,r*.42f),Glyph.COLOR[0]);
            Skits.face(p,Kawaii.DUMPLING,x+r*.45f,y+r*.42f,r*.26f,255,1f,0f);
        } else if(id==FLEX) {
            float[] arm={x+r*.7f,y+r*.5f,x-r*.45f,y+r*.5f,x-r*.65f,y+r*.05f,x-r*.12f,y-r*.6f};
            p.polyline(arm,0xFFE3AB4D,r*.40f);
            p.fillEllipse(x+r*.1f,y+r*.35f,r*.38f,r*.32f,GOLD);
            p.fillCircle(x-r*.12f,y-r*.6f,r*.27f,GOLD);
            p.line(x-r*.20f,y-r*.64f,x+r*.01f,y-r*.51f,0xFFE3AB4D,r*.055f);
        } else if(id==TEAM) {
            Skits.face(p,Kawaii.SQUISHY,x,y,r*.78f,255,1f,1f);
            for(int side=-1;side<=1;side+=2)
                p.polyline(new float[]{x+side*r*.8f,y-r*.7f,x+side*r*.63f,y-r*.28f,
                        x+side*r*.93f,y-r*.31f,x+side*r*.8f,y+r*.13f},GOLD,r*.10f);
        } else {
            p.line(x-r*.53f,y,x+r*.53f,y,GOLD,r*.14f);
            Skits.face(p,Kawaii.DUMPLING,x-r*.62f,y,r*.49f,255,1f,1f);
            Skits.face(p,Kawaii.CAT,x+r*.62f,y,r*.49f,255,1f,1f);
        }
    }
}
