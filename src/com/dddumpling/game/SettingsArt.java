package com.dddumpling.game;

/** Existing key characters act as the slider handles; their props show the channel level. */
final class SettingsArt extends Draw {
    static void draw(Painter p,boolean music,float x,float y,float keyR,float volume,boolean muted,float clock) {
        int glyph=music?Kawaii.CAT:Kawaii.BLOB, col=Glyph.COLOR[glyph];
        p.fillPoly(Glyph.hex(x,y,keyR),Glyph.withAlpha(col,35));
        p.strokePoly(Glyph.hex(x,y,keyR),Glyph.withAlpha(col,190),keyR*.025f);
        float r=keyR*.60f;
        if(music && muted) Kawaii.crying(p,glyph,x,y,r,col,1f,clock,.8f);
        else Kawaii.draw(p,glyph,x,y,r,col,1f,muted?0f:volume);
        if(music) {
            if(!muted) guitar(p,x,y,r,volume,clock,col);
        } else {
            float my=y+r*.32f;
            p.fillEllipse(x,my,r*.27f,r*.17f,col);
            if(muted) {
                // A rounded palm, three fingers and a short arm across the mouth.
                p.line(x+r*.70f,y+r*.61f,x+r*.08f,my,col,r*.22f);
                int hand=Glyph.mix(col,INK,.30f);
                p.fillEllipse(x,my,r*.26f,r*.15f,hand);
                for(int i=0;i<3;i++) p.fillCircle(x+r*(-.15f+i*.14f),my-r*.06f,r*.075f,hand);
                p.line(x-r*.09f,my+r*.015f,x+r*.13f,my+r*.015f,col,r*.03f);
            } else {
                float h=r*(.025f+.24f*volume);
                p.fillEllipse(x,my,r*(.045f+.14f*volume),h,INK);
                if(volume>.35f) p.fillEllipse(x,my+h*.5f,r*.10f*volume,h*.3f,ROSE);
                for(int side=-1;side<=1;side+=2) {
                    if(volume<.3f) {
                        for(int i=0;i<3;i++) p.fillCircle(x+side*r*(.35f+i*.16f),my,r*.025f,INK_DIM);
                    } else for(int i=0;i<3;i++) {
                        float a=(i-1)*.5f;
                        p.line(x+side*r*.48f,my+a*r*.4f,x+side*r*(.52f+.25f*volume),my+a*r*.7f,INK,r*.035f);
                    }
                }
            }
        }
    }
    private static void guitar(Painter p,float x,float y,float r,float v,float clock,int paw) {
        float size=.55f+.3f*v, gx=x-r*.18f,gy=y+r*.57f,w=r*size;
        boolean electric=v>=.8f;
        int wood=electric?Glyph.COLOR[1]:v<.35f?0xFFE9B878:0xFFBB784F;
        p.line(gx,gy,x+r*.77f,y+r*.05f,0xFF714A46,r*.16f);
        if(electric) p.fillPoly(new float[]{gx-w*.6f,gy+w*.05f,gx-w*.2f,gy-w*.65f,
                gx+w*.04f,gy-w*.27f,gx+w*.55f,gy-w*.45f,gx+w*.28f,gy+w*.2f,
                gx+w*.5f,gy+w*.55f,gx-w*.5f,gy+w*.55f},wood);
        else {
            p.fillEllipse(gx-w*.12f,gy+w*.17f,w*.48f,w*.42f,wood);
            p.fillEllipse(gx+w*.17f,gy-w*.13f,w*.34f,w*.31f,wood);
            p.fillCircle(gx+w*.05f,gy,w*.14f,INK);
        }
        p.line(x+r*.7f,y+r*.11f,x+r*.82f,y-r*.03f,wood,r*.23f);
        for(int i=-1;i<=1;i++) p.line(gx-w*.2f,gy+r*.035f*i,x+r*.76f,y+r*(.08f+.035f*i),0xFFEEDCA8,r*.016f);
        p.fillCircle(x+r*.52f,y+r*.22f,r*.12f,paw);
        float strum=(float)Math.sin(clock*(4+v*12))*r*.08f*v;
        p.fillEllipse(gx-r*.12f,gy+strum,r*.16f,r*.11f,paw);
        if(electric) for(int side=-1;side<=1;side+=2)
            p.polyline(new float[]{x+side*r*.9f,y-r*.55f,x+side*r*.76f,y-r*.35f,
                    x+side*r*.96f,y-r*.3f,x+side*r*.84f,y-r*.1f},GOLD,r*.055f);
    }
}
