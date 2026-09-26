package com.dddumpling.game;

/** Character controls for the player settings: musical handles and the pear age toggle. */
final class SettingsArt extends Draw {
    static void draw(Painter p,boolean music,float x,float y,float keyR,float volume,boolean muted,float clock) {
        int glyph=music?Kawaii.CAT:Kawaii.BLOB, col=Glyph.COLOR[glyph];
        float r=keyR*.60f;
        float pulse=(float)Math.sin(clock*(4f+volume*10f));
        if(!muted) {
            y-=r*.055f*volume*(1f+pulse);
            if(!music) x+=r*.04f*volume*volume*(float)Math.sin(clock*37f);
        }
        if(music && muted) Kawaii.crying(p,glyph,x,y,r,col,1f,clock,.8f);
        else Kawaii.draw(p,glyph,x,y,r,col,muted?1f:1f+.055f*volume*pulse,muted?0f:volume);
        if(music) {
            if(!muted) {
                guitar(p,x,y,r,volume,clock,col);
                notes(p,x,y,r,volume,clock);
            }
        } else {
            float my=y+r*.32f;
            p.fillEllipse(x,my,r*.27f,r*.17f,col);
            if(muted) {
                // A rounded palm, three fingers and a short arm across the mouth.
                p.line(x+r*.70f,y+r*.61f,x+r*.08f,my,col,r*.22f);
                int hand=0xFF999CA5;
                p.fillEllipse(x,my,r*.26f,r*.15f,hand);
                for(int i=0;i<3;i++) p.fillCircle(x+r*(-.15f+i*.14f),my-r*.06f,r*.075f,hand);
                p.line(x-r*.09f,my+r*.015f,x+r*.13f,my+r*.015f,0xFF686B75,r*.03f);
            } else {
                float h=r*(.025f+.34f*volume*(.8f+.2f*pulse));
                p.fillEllipse(x,my,r*(.04f+.22f*volume),h,INK);
                if(volume>.35f) p.fillEllipse(x,my+h*.5f,r*.14f*volume,h*.3f,ROSE);
                waves(p,x,my,r,volume,clock);
            }
        }
    }
    static void kidsToggle(Painter p,float x,float y,float s,float position,float lift) {
        p.fillPoly(pill(x,y,s*2f,s*.70f,12),Glyph.mix(0xFF555066,0xFF397C69,position));
        float cx=x+(position*2f-1f)*s, r=s*1.05f*(1f+lift*.10f);
        y-=s*lift;
        // Keep the pear opaque while its young/old face and track blend during the hop.
        Shape.draw(p,Collect.POME,cx,y,r,Glyph.mix(0xFFB9C877,0xFFCBE06A,position),0xFF8FD9A0,1f,0f,true);
        if(position<1f) pearFace(new OpacityPainter(p,1f-position),cx,y,r,false);
        if(position>0f) pearFace(new OpacityPainter(p,position),cx,y,r,true);
    }
    /** The same young/old pear without the settings track, for saved Kids Mode readouts. */
    static void kidsPear(Painter p,float x,float y,float r,boolean young) {
        Shape.draw(p,Collect.POME,x,y,r,young?0xFFCBE06A:0xFFB9C877,0xFF8FD9A0,1f,0f,true);
        pearFace(p,x,y,r,young);
    }
    private static void pearFace(Painter p,float cx,float y,float r,boolean young) {
        int face=0xFF3A2E4F;
        float ey=y+r*.13f;
        for(int side=-1;side<=1;side+=2) {
            float ex=cx+side*r*.27f;
            if(young) {
                p.fillEllipse(ex,ey,r*.13f,r*.17f,face);
                p.fillCircle(ex-r*.025f,ey-r*.06f,r*.04f,0xFFFFFAEF);
                p.fillEllipse(cx+side*r*.49f,y+r*.34f,r*.12f,r*.07f,0xDDFF8F9A);
            } else {
                p.strokeCircle(ex,ey,r*.20f,face,r*.055f);
                p.line(ex-r*.07f,ey,ex+r*.07f,ey,face,r*.05f);
                p.line(ex-r*.15f,ey-r*.29f,ex+r*.13f,ey-r*.31f,0xFFF5F0D8,r*.10f);
                p.line(cx+side*r*.48f,ey+r*.24f,cx+side*r*.59f,ey+r*.29f,face,r*.035f);
            }
        }
        p.arc(cx,y+r*.33f,r*.16f,r*.13f,0,180,face,r*.05f);
        if(!young) {
            p.line(cx-r*.07f,ey,cx+r*.07f,ey,face,r*.05f);
            for(int side=-1;side<=1;side+=2)
                p.fillEllipse(cx+side*r*.13f,y+r*.37f,r*.18f,r*.09f,0xFFFFF6DF);
            p.line(cx-r*.12f,y-r*.20f,cx+r*.12f,y-r*.20f,face,r*.035f);
        }
    }
    private static void waves(Painter p,float x,float y,float r,float v,float clock) {
        for(int i=0;i<3;i++) {
            float phase=(clock*(1.2f+v*2f)+i/3f)%1f;
            float radius=r*(.30f+.65f*phase)*(.3f+.7f*v);
            int color=Glyph.withAlpha(Glyph.COLOR[Kawaii.BLOB],Math.round(225f*v*(1f-phase)));
            for(int side=-1;side<=1;side+=2)
                p.arc(x+side*r*.5f,y,radius,radius*.9f,side>0?-50f:130f,100f,color,r*(.025f+.04f*v));
        }
    }
    private static void notes(Painter p,float x,float y,float r,float v,float clock) {
        for(int i=0;i<3;i++) {
            float phase=(clock*(.65f+v*.8f)+i/3f)%1f;
            float side=i%2==0?-1f:1f;
            float nx=x+side*r*(1f+.1f*(float)Math.sin(clock*2f+i));
            float ny=y+r*(.25f-phase*.95f),h=r*(.16f+.55f*v);
            int color=Glyph.withAlpha(i==1?GOLD:Glyph.COLOR[3],Math.round(235f*(1f-phase)*(.4f+.6f*v)));
            p.fillEllipse(nx,ny,h*.23f,h*.16f,color);
            p.line(nx+h*.16f,ny,nx+h*.16f,ny-h,color,h*.10f);
            p.line(nx+h*.16f,ny-h,nx+h*.48f,ny-h*.77f,color,h*.13f);
        }
    }
    static void guitar(Painter p,float x,float y,float r,float v,float clock,int paw) {
        float size=.55f+.3f*v, gx=x-r*.18f,gy=y+r*.57f,w=r*size;
        boolean electric=v>=.8f;
        int wood=electric?0xFF941C35:v<.35f?0xFF287E89:0xFFBB784F;
        p.line(gx,gy,x+r*.77f,y+r*.05f,0xFF714A46,r*.16f);
        if(electric) {
            // Two swept wings and an open notch, aligned with the neck.
            float[] body={.38f,0f,-.68f,-.65f,-.29f,0f,-.68f,.65f};
            for(int i=0;i<body.length;i+=2) {
                float along=body[i]*w,across=body[i+1]*w;
                body[i]=gx+along*.866f+across*.5f;
                body[i+1]=gy-along*.5f+across*.866f;
            }
            p.fillPoly(body,wood);
            p.line(body[0],body[1],body[2],body[3],0xFFCF5363,r*.025f);
            p.line(gx-r*.06f,gy-r*.10f,gx+r*.06f,gy+r*.10f,INK,r*.09f);
        }
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

    }
}
