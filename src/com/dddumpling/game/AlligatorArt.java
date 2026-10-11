package com.dddumpling.game;

/** Forest-green jelly skin, a broad snout, little teeth and a tummy-sized arena. */
final class AlligatorArt extends Draw {
    static final int SKIN=0xFF398F77, LIGHT=0xFF9DDEC0, DARK=0xFF164F4C, BELLY=0xFFBCD6A1;
    private AlligatorArt() {}
    static void draw(Painter painter,GameCore c,Layout L,Boss b,float alpha) {
        Alligator a=b.alligator;
        float arrival=Math.min(1,b.introProgress()*1.6f);
        float end=b.beaten?b.leaveProgress():0;
        Painter p=new OpacityPainter(painter,alpha*(1-end*end));
        p.save();p.translate(0,-L.h*.5f*(1-arrival)+L.h*end*end);
        if(a.tummy!=null) {
            float x=a.tummy.centreX(),y=a.tummy.centreY();
            float[] skin=a.tummy.outline();
            p.fillPoly(skin,DARK);
            p.fillPoly(inset(skin,x,y,.96f,.98f),SKIN);
            p.fillPoly(inset(skin,x,y,.79f,.91f),BELLY);
            p.fillPoly(inset(skin,x-L.w*.07f,y,.65f,.88f),0xFFCCE5B6);
            for(int n=0;n<7;n++) {
                float yy=L.playTop+(L.dangerY-L.playTop)*(.28f+n*.10f);
                float w=L.w*(.27f-.07f*Math.abs(n-3)/3f);
                p.arc(L.w*.5f,yy,w,L.unit*.85f,10,160,0x50517960,L.unit*.075f);
            }
        }
        for(int i=0;i<3;i++) {
            float tx=a.limb==i?a.tipX:a.restX(L,i),ty=a.limb==i?a.tipY:a.restY(L,i);
            float rx=a.rootX(L,i),ry=a.rootY(L,i);
            boolean live=a.limb==i && !a.blocked && !b.beaten;
            limb(p,rx,ry,tx,ty,L.w*(i==2?.055f:.07f),i==2,
                    live?Glyph.mix(SKIN,LIGHT,.4f):SKIN,b.age);
            if(live) {
                float r=L.w*.09f*(1+.08f*(float)Math.sin(b.age*12));
                p.strokeCircle(tx,ty,r,0xC3E5FFD5,L.unit*.12f);
                float d=L.unit*.9f;
                p.line(tx-d,ty,tx+d,ty,0xDDFFFFFF,L.unit*.12f);
                p.line(tx-d,ty,tx-d*.4f,ty-d*.42f,0xDDFFFFFF,L.unit*.10f);
                p.line(tx+d,ty,tx+d*.4f,ty+d*.42f,0xDDFFFFFF,L.unit*.10f);
            }
        }
        if(b.body!=null)head(p,b.body.outline(),b.body.centreX(),b.body.centreY(),b.body.radiusX(),b.body.radiusY(),b.age,a.daze>0 || b.beaten,b.hurt);
        float y=b.bodyY(L)+Boss.bodyR(L)*1.35f;
        if(!b.beaten && b.intro<=0) {
            for(int n=0;n<Alligator.BLOCKS;n++) {
                float x=L.w*.5f+(n-4)*L.unit*.85f;
                p.fillEllipse(x,y,L.unit*.29f,L.unit*.18f,n<a.blocks?LIGHT:0x88406B56);
                if(n<a.blocks)p.line(x,y,x+L.unit*.18f,y-L.unit*.1f,0xFFE9FFCA,L.unit*.055f);
            }
            p.text(a.daze>0?"DAZED! TAP ANY KEY":"SWIPE ACROSS THE GLOWING LIMB",L.w*.5f,y+L.unit*1.5f,
                    type(L.unit*.57f),DARK,Painter.CENTER,true);
            if(a.daze>0) {
                p.line(L.w*.3f,y+L.unit*2.1f,L.w*(.3f+.4f*a.daze/Alligator.DAZE),y+L.unit*2.1f,LIGHT,L.unit*.17f);
            }
        }
        if(a.strokeFade>0)p.line(a.strokeX,a.strokeY,a.strokeEndX,a.strokeEndY,
                fadeBy(0xFFE1FFF0,a.strokeFade/.3f),L.unit*.4f);
        p.restore();
        if(b.beaten)for(int i=0;i<12;i++) {
            float angle=i*Softbody.TAU/12,travel=L.w*end;
            p.strokeCircle(L.w*.5f+(float)Math.cos(angle)*travel,L.h*.55f+(float)Math.sin(angle)*travel,
                    L.unit*(.6f+end),fadeBy(LIGHT,(1-end)*.7f),L.unit*.10f);
        }
    }
    private static float[] inset(float[] skin,float x,float y,float sx,float sy) {
        float[] out=new float[skin.length];
        for(int i=0;i<skin.length;i+=2){out[i]=x+(skin[i]-x)*sx;out[i+1]=y+(skin[i+1]-y)*sy;}
        return out;
    }
    static void limb(Painter p,float x,float y,float tx,float ty,float width,boolean tail,int color,float clock) {
        float dx=tx-x,dy=ty-y,d=Math.max(1,(float)Math.hypot(dx,dy)),nx=-dy/d,ny=dx/d;
        float[] shape=new float[52];
        for(int i=0;i<13;i++) {
            float u=i/12f,bulge=(float)Math.sin(u*Math.PI),bend=width*bulge*(tail?1.6f:.5f);
            float w=width*(tail?1-u*.8f:.9f+.18f*bulge);
            float cx=x+dx*u+nx*bend,cy=y+dy*u+ny*bend;
            shape[i*2]=cx+nx*w;shape[i*2+1]=cy+ny*w;
            shape[(25-i)*2]=cx-nx*w;shape[(25-i)*2+1]=cy-ny*w;
        }
        p.fillPoly(shape,DARK);p.fillPoly(inset(shape,(x+tx)*.5f,(y+ty)*.5f,.93f,.97f),color);
        p.line(x+nx*width*.32f,y+ny*width*.32f,tx,ty,Glyph.mix(color,LIGHT,.35f),width*.35f);
        p.fillCircle(tx,ty,width*(tail?.29f:.94f),color);
        if(!tail)for(int i=-1;i<=1;i++)p.fillEllipse(tx+nx*width*i*.55f+dx/d*width*.65f,
                ty+ny*width*i*.55f+dy/d*width*.65f,width*.17f,width*.13f,BELLY);
        else for(int i=1;i<5;i++) {
            float u=i/5f;float cx=x+dx*u,cy=y+dy*u;
            p.fillPoly(new float[]{cx+nx*width*.6f,cy+ny*width*.6f,cx+nx*width*1.2f,cy+ny*width*1.2f,
                    cx+dx/d*width*.7f,cy+dy/d*width*.7f},LIGHT);
        }
    }
    static void head(Painter p,float[] skin,float x,float y,float w,float h,float clock,boolean dazed,float hurt) {
        int col=Glyph.mix(SKIN,0xFFEDFFF2,hurt*.45f);
        p.fillPoly(skin,DARK);p.fillPoly(inset(skin,x,y,.98f,.95f),col);
        p.fillPoly(inset(skin,x-w*.12f,y-h*.18f,.89f,.78f),Glyph.mix(col,LIGHT,.24f));
        // A wide flattened muzzle joins the cheeks, with a soft underside and rounded teeth.
        p.fillEllipse(x,y+h*.38f,w*.92f,h*.57f,DARK);
        p.fillEllipse(x,y+h*.28f,w*.92f,h*.52f,col);
        p.fillEllipse(x-w*.12f,y+h*.14f,w*.69f,h*.28f,fadeBy(LIGHT,.42f));
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*w*.48f,ey=y-h*.61f;
            p.fillEllipse(ex,ey,w*.21f,h*.49f,col);
            p.fillEllipse(ex,ey-h*.01f,w*.14f,h*.32f,0xFFE9F8CE);
            if(dazed)p.arc(ex,ey,w*.07f,h*.14f,10,160,DARK,w*.035f);
            else {
                p.fillEllipse(ex+side*w*.01f,ey,w*.063f,h*.19f,DARK);
                p.fillCircle(ex-w*.015f,ey-h*.09f,w*.024f,0xFFFFFFFF);
            }
            p.fillEllipse(x+side*w*.39f,y+h*.20f,w*.068f,h*.062f,DARK);
            p.fillEllipse(x+side*w*.69f,y+h*.46f,w*.11f,h*.10f,0x709FE5BD);
            p.fillPoly(new float[]{x+side*w*.48f,y+h*.63f,x+side*w*.60f,y+h*.61f,
                    x+side*w*.54f,y+h*.85f},0xFFEAF6D8);
        }
        p.arc(x,y+h*.22f,w*.73f,h*.42f,12,156,DARK,w*.027f);
        for(int i=-1;i<=1;i++)p.fillEllipse(x+i*w*.18f,y-h*.68f,w*.06f,h*.12f,LIGHT);
        if(dazed)for(int i=0;i<3;i++) {
            float a=clock*3+i*Softbody.TAU/3;
            float sx=x+(float)Math.cos(a)*w*.7f,sy=y-h*1.25f+(float)Math.sin(a)*h*.18f;
            p.line(sx-w*.06f,sy,sx+w*.06f,sy,0xFFFFED9C,w*.025f);
            p.line(sx,sy-h*.10f,sx,sy+h*.10f,0xFFFFED9C,w*.025f);
        }
    }
    static void portrait(Painter p,float x,float y,float r,float clock,boolean known,float fade) {
        Painter q=new OpacityPainter(p,fade);
        if(!known)q=new MonochromePainter(q,1f);
        q.fillEllipse(x,y+r*.33f,r*.57f,r*.60f,SKIN);
        q.fillEllipse(x,y+r*.41f,r*.40f,r*.46f,BELLY);
        for(int side=-1;side<=1;side+=2)limb(q,x+side*r*.43f,y+r*.18f,x+side*r*.78f,y+r*.46f,r*.13f,false,SKIN,clock);
        float[] skin=new float[96];
        float hh=r*.44f*(1+.035f*(float)Math.sin(clock*2));
        for(int i=0;i<48;i++) {float a=i*Softbody.TAU/48;skin[i*2]=x+(float)Math.cos(a)*r;skin[i*2+1]=y-r*.20f+(float)Math.sin(a)*hh;}
        head(q,skin,x,y-r*.20f,r,hh,clock,false,0);
        if(!known)q.text("?",x,y+r*.25f,type(r*.55f),INK,Painter.CENTER,true);
    }
}
