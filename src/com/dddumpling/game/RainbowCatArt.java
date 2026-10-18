package com.dddumpling.game;

/** Frosting, paws and pointed ears traced over moving spring outlines; no sprite textures. */
final class RainbowCatArt extends Draw {
    static final int[] COLORS={0xFFFF7B9B,0xFFFFB36B,0xFFFFE488,0xFF91E2BA,0xFF8ED9F5,0xFFBBA4EE};
    static final int FUR=0xFFA9A7BE, INK=0xFF49425F;
    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        RainbowCat a=b.cat;if(!a.placed)return;
        float fade=alpha*(b.beaten?1-b.leaveProgress():1),enter=b.intro>0?b.introProgress():1;
        Painter q=new OpacityPainter(p,fade);
        dashTrail(q,a);
        q.save();q.translate((1-enter)*(1-enter)*-L.w,0);
        cat(q,a.x,a.y,a.r,a.clock*(a.phase==RainbowCat.OPEN?1:2),a.turn,a.pastry,a.head,a.phase==RainbowCat.OPEN,1);
        if(a.flash>0)for(int band=0;band<3;band++)
            q.strokeCircle(a.x,a.y,a.r*(1.1f+(1-a.flash)*(1.5f+band*.3f)),
                    Glyph.withAlpha(COLORS[(a.taps+band)%COLORS.length],(int)(a.flash*170)),a.r*.12f);
        q.restore();
        if(a.phase==RainbowCat.OPEN && !b.beaten) {
            float s=L.enemyR*.72f,yy=a.y+a.r*1.8f;
            for(int i=0;i<a.count;i++) {
                int g=a.prompt[i];float xx=L.w*.5f+(i-(a.count-1)*.5f)*s*2.25f;
                float done=i<a.at?.25f:1;
                FastEnemies.body(q,xx,yy,s,Skins.keyColor(p,g),(int)(255*done),false);
                Kawaii.draw(q,g,xx,yy,s*.70f,Skins.keyColor(p,g),done,0);
                if(i==a.at)q.strokeCircle(xx,yy,s*1.35f,0xFFFFF1BD,s*.12f);
            }
            float fraction=Math.max(0,a.timer/a.promptTime()),width=L.w*.60f;
            for(int band=0;band<COLORS.length;band++) {
                float xx=L.w*.5f-width*.5f+width*band/COLORS.length;
                float fill=Math.max(0,Math.min(1,fraction*COLORS.length-band));
                if(fill>0)q.line(xx,yy+s*1.8f,xx+width/COLORS.length*fill,yy+s*1.8f,COLORS[band],s*.16f);
            }
        }
    }
    private static void dashTrail(Painter p,RainbowCat a) {
        for(int i=a.trailCount-1;i>0;i--) {
            float dx=a.trailX[i-1]-a.trailX[i],dy=a.trailY[i-1]-a.trailY[i];
            float d=(float)Math.hypot(dx,dy);if(d<.01f)continue;
            float nx=-dy/d,ny=dx/d,fade=1-a.trailAge[i]/RainbowCat.TRAIL_TIME;
            float width=a.r*(.16f+.07f*fade);
            for(int band=0;band<COLORS.length;band++) {
                float low=(band-3)*width,high=low+width*1.1f;
                p.fillPoly(new float[]{a.trailX[i]+nx*low,a.trailY[i]+ny*low,
                        a.trailX[i-1]+nx*low,a.trailY[i-1]+ny*low,
                        a.trailX[i-1]+nx*high,a.trailY[i-1]+ny*high,
                        a.trailX[i]+nx*high,a.trailY[i]+ny*high},Glyph.withAlpha(COLORS[band],(int)(160*fade*fade)));
            }
            if(i%6==0)p.fillPoly(star(a.trailX[i],a.trailY[i],a.r*.17f*fade,a.r*.07f*fade,4,a.clock*3+i),
                    Glyph.withAlpha(0xFFFFFFFF,(int)(210*fade)));
        }
    }
    static void rushProjectile(Painter p,GameCore c,Layout L,Boss b,int i,float x,float y,float r,int color) {
        float dx=x-b.bsx[i],dy=y-b.bsy[i],d=Math.max(1,(float)Math.hypot(dx,dy));
        float ux=dx/d,uy=dy/d,length=Math.min(d,r*5);
        for(int lane=-1;lane<=1;lane++) {
            int tint=COLORS[Math.floorMod(i+lane+1,COLORS.length)];
            p.line(x-ux*length-uy*r*lane*.2f,y-uy*length+ux*r*lane*.2f,x,y,
                    Glyph.withAlpha(tint,80),r*(lane==0?.22f:.12f));
        }
        for(int j=0;j<10;j++) {
            float u=(c.clock*1.8f+j*.618f)%1f,spread=(float)Math.sin(j*17)*r*u*.6f;
            float xx=x-ux*length*u-uy*spread,yy=y-uy*length*u+ux*spread;
            int tint=COLORS[(i+j)%COLORS.length];float rr=r*(.12f+.07f*(j%3));
            p.fillCircle(xx,yy,rr*2.5f,Glyph.withAlpha(tint,(int)(35*(1-u))));
            p.fillPoly(star(xx,yy,rr,rr*.4f,4,c.clock+j),Glyph.withAlpha(tint,(int)(220*(1-u))));
        }
        FastEnemies.glow(p,x,y,r,color,255,1);
        FastEnemies.body(p,x,y,r,color,255,false);
        Kawaii.draw(p,b.bglyph[i],x,y,r*.63f,color,1,.1f);
    }
    static void portrait(Painter p,float x,float y,float r,float t,boolean known,float fade) {
        portrait(p,x,y,r,t,known,fade,1);
    }
    static void portrait(Painter p,float x,float y,float r,float t,boolean known,float fade,int facing) {
        cat(new OpacityPainter(p,fade),x-facing*r*.12f,y,r*.67f,t,facing,null,null,false,known?1:0);
    }
    static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known) {
        int crust=known>0?0xFFFFD49A:0xFF49425F,fur=known>0?FUR:0xFF49425F;
        float step=(float)Math.sin(t*(caught?15:7));
        // Layered rainbow ribbon tapers behind the pastry and bends with flight.
        for(int band=0;band<COLORS.length;band++) {
            float[] trail=new float[36];
            for(int i=0;i<9;i++){float u=i/8f;trail[i*2]=x-facing*r*(.8f+u*2.9f);trail[i*2+1]=y+r*((band-2.5f)*.24f+(float)Math.sin(t*4-u*3)*u*.30f)-r*.12f;trail[(17-i)*2]=trail[i*2];trail[(17-i)*2+1]=trail[i*2+1]+r*.24f;}
            p.fillPoly(trail,fadeBy(COLORS[band],.48f*known));
        }
        float[] tail={x-facing*r*.85f,y+r*.4f,x-facing*r*1.5f,y+r*.25f,x-facing*r*1.65f,y-r*(.10f+step*.16f)};
        p.polyline(tail,INK,r*.30f);p.polyline(tail,fur,r*.24f);
        for(int foot=0;foot<4;foot++) {
            float fx=x+r*(-.85f+foot*.51f),fy=y+r*(.9f+.09f*(foot%2==0?step:-step));
            p.fillEllipse(fx,fy,r*.22f,r*.26f,INK);p.fillEllipse(fx,fy-r*.04f,r*.17f,r*.21f,fur);
        }
        float[] outline=pastry(body,x,y,r);
        p.fillPoly(outline,INK);p.fillPoly(inset(outline,x,y,.96f,.95f),crust);
        p.fillPoly(inset(outline,x-r*.02f,y-r*.04f,.81f,.83f),known>0?0xFFFF92CC:fur);
        p.fillPoly(inset(outline,x-r*.09f,y-r*.13f,.70f,.66f),known>0?0xFFFFB1DD:fur);
        if(known>0)for(int i=0;i<10;i++) {
            float px=x+r*(-.75f+(i%4)*.43f),py=y+r*(-.61f+(i/4)*.53f);
            p.line(px,py,px+r*.06f,py+r*.10f,COLORS[(i+1)%COLORS.length],r*.07f);
        }
        float hx=skull==null?x+facing*r*.96f:skull.centreX();
        float hy=skull==null?y+r*.1f:skull.centreY();
        float hr=skull==null?r*.66f:skull.radiusY();
        // One ear/head silhouette, with softly beveled cheeks and forehead.
        float[] face={-.98f,.16f,-.9f,-.35f,-.86f,-1.02f,-.64f,-1.10f,-.27f,-.59f,
                .27f,-.59f,.64f,-1.10f,.86f,-1.02f,.9f,-.35f,.98f,.16f,.80f,.64f,.42f,.82f,-.42f,.82f,-.8f,.64f};
        float[] shape=points(face,hx,hy,hr,hr);
        p.fillPoly(shape,INK);p.fillPoly(inset(shape,hx,hy,.93f,.92f),fur);
        p.fillPoly(inset(shape,hx-hr*.08f,hy-hr*.1f,.80f,.72f),known>0?0xFFC4C2D3:fur);
        if(known<=0)return;
        for(int side=-1;side<=1;side+=2) {
            p.fillPoly(points(new float[]{side*.5f,-.63f,side*.73f,-.94f,side*.76f,-.46f},hx,hy,hr,hr),0xFFFFB4CB);
            float ex=hx+side*hr*.38f,ey=hy-hr*.02f;
            boolean blink=Math.sin(t*1.7f)>.985;
            p.fillEllipse(ex,ey,hr*.115f,hr*(blink?.035f:.17f),INK);
            if(!blink)p.fillCircle(ex-hr*.025f,ey-hr*.06f,hr*.04f,0xFFFFFAFF);
            p.fillEllipse(hx+side*hr*.67f,hy+hr*.25f,hr*.15f,hr*.09f,0xBFFF83BD);
        }
        p.fillPoly(points(new float[]{-.08f,.14f,.08f,.14f,0,.23f},hx,hy,hr,hr),0xFFB8709E);
        p.polyline(points(new float[]{-.25f,.30f,-.13f,.39f,0,.31f,.13f,.39f,.25f,.30f},hx,hy,hr,hr),INK,hr*.055f);
        if(caught)p.fillEllipse(hx,hy+hr*.46f,hr*.1f,hr*.12f,0xFFEC88B4);
    }
    static float[] pastry(Softbody b,float x,float y,float r) {
        float[] src=b==null?null:b.outline(),out=new float[src==null?72:src.length];
        for(int i=0;i<out.length;i+=2) {
            float ang=Softbody.TAU*i/out.length;
            float nx=src==null?(float)Math.cos(ang):(src[i]-b.centreX())/(r*1.15f);
            float ny=src==null?(float)Math.sin(ang):(src[i+1]-b.centreY())/r;
            // Rounded rectangle preserves the spring deformation instead of overlaying a rigid box.
            out[i]=x+r*1.15f*(float)Math.copySign(Math.pow(Math.abs(nx),.55),nx);
            out[i+1]=y+r*(float)Math.copySign(Math.pow(Math.abs(ny),.55),ny);
        }return out;
    }
    static float[] points(float[] pts,float x,float y,float sx,float sy) {
        float[] out=new float[pts.length];for(int i=0;i<pts.length;i+=2){out[i]=x+pts[i]*sx;out[i+1]=y+pts[i+1]*sy;}return out;
    }
    static float[] inset(float[] pts,float x,float y,float sx,float sy) {
        float[] out=new float[pts.length];for(int i=0;i<pts.length;i+=2){out[i]=x+(pts[i]-x)*sx;out[i+1]=y+(pts[i+1]-y)*sy;}return out;
    }
    static void arch(Painter p,float x,float y,float rx,float ry,int color,float thick) {
        float[] points=new float[100];
        for(int i=0;i<=24;i++) {
            float angle=(float)Math.PI+(float)Math.PI*i/24;
            float cx=(float)Math.cos(angle),sy=(float)Math.sin(angle);
            points[i*2]=x+cx*rx;points[i*2+1]=y+sy*ry;
            points[(49-i)*2]=x+cx*(rx-thick);points[(49-i)*2+1]=y+sy*(ry-thick);
        }p.fillPoly(points,color);
    }
    static void yarn(Painter p,float x,float y,float r,float t,boolean held) {
        p.fillCircle(x,y,r*1.2f,0x304FC9E8);p.fillCircle(x,y,r,0xFF9281C0);
        for(int i=0;i<RainbowCatArt.COLORS.length;i++)p.arc(x,y,r*(.95f-i*.07f),r*.85f,t*35+i*31,225,COLORS[i],r*.17f);
        p.fillEllipse(x-r*.30f,y-r*.40f,r*.22f,r*.13f,0x99FFFFFF);
        if(held)p.strokeCircle(x,y,r*1.22f,0xFFFFEEB2,r*.07f);
    }
}
