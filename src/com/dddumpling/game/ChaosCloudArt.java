package com.dddumpling.game;

/** Small bounded vector swarm; no blur or offscreen layer. */
final class ChaosCloudArt extends Draw {
    private ChaosCloudArt() {}
    static void ellipse(Painter p,float x,float y,float rx,float ry,int color,float width) {
        float[] ring=new float[48];
        for(int i=0;i<24;i++) {float a=i*Softbody.TAU/24;ring[i*2]=x+(float)Math.cos(a)*rx;ring[i*2+1]=y+(float)Math.sin(a)*ry;}
        p.strokePoly(ring,color,width);
    }
    static void portrait(Painter p,float x,float y,float r,float clock,float fade,boolean known) {
        swarm(p,x,y,r,clock,fade,known,0,0);
    }
    static float orbitX(int i,float clock) {
        float angle=i*2.39996f+clock*(i%2==0?.32f:-.24f);
        return (float)Math.cos(angle)*(.22f+.70f*hash(23+i*41))*1.3f;
    }
    static float orbitY(int i,float clock) {
        float angle=i*2.39996f+clock*(i%2==0?.32f:-.24f);
        return (float)Math.sin(angle)*(.22f+.70f*hash(23+i*41))*.74f;
    }
    static void swarm(Painter p,float x,float y,float r,float clock,float fade,boolean known,float hurt,float scatter) {
        swarm(p,x,y,r,clock,fade,known,hurt,scatter,null);
    }
    static void swarm(Painter p,float x,float y,float r,float clock,float fade,boolean known,float hurt,float scatter,Boss owner) {
        boolean bats=Skins.halloween(p);
        Painter particles=new OpacityPainter(p,fade);
        for(int i=0;i<30;i++) {
            float opacity=owner==null?1:owner.cloud.particleOpacity(owner,i);
            if(opacity<.3f)continue;
            float xx=x+orbitX(i,clock)*r*(1+scatter*5);
            float yy=y+orbitY(i,clock)*r*(1+scatter*5);
            float rr=r*(.11f+.08f*hash(i*17+29))*(1+hurt*.15f)*opacity;
            int color=known?Glyph.mix(Skins.keyColor(p,i%Glyph.COUNT),0xFFFFFFFF,hurt*.8f):0xFF827B9C;
            if(bats)bat(particles,xx,yy,rr,clock*9+i,color);
            else {
                p.fillPoly(Glyph.hex(xx,yy,rr*1.18f),Glyph.withAlpha(color,(int)(fade*70)));
                p.strokePoly(Glyph.hex(xx,yy,rr),Glyph.withAlpha(color,(int)(fade*210)),rr*.09f);
                if(known && i%3==0)Kawaii.incognito(particles,i%Glyph.COUNT,xx,yy,rr*.62f,color,.75f);
                else p.fillCircle(xx-rr*.2f,yy-rr*.25f,rr*.19f,Glyph.withAlpha(color,(int)(fade*140)));
            }
        }
        // The swarm has a single expressive face, visible above the little orbiting faces.
        if(scatter<.15f) {
            int ink=Glyph.withAlpha(0xFFF4EEFF,(int)(fade*255));
            for(int side=-1;side<=1;side+=2) {
                p.fillEllipse(x+side*r*.25f,y-r*.04f,r*.105f,r*(hurt>0?.07f:.15f),ink);
                p.fillCircle(x+side*r*.25f+r*.02f,y-r*.03f,r*.05f,0xFF24213F);
            }
            ellipse(p,x,y+r*.23f,r*.15f,r*.07f,ink,r*.035f);
        }
    }
    static void bat(Painter p,float x,float y,float r,float t,int color) {
        float flap=(float)Math.sin(t)*r*.65f;
        int body=Glyph.mix(0xFF29223F,color,.22f),edge=Glyph.mix(color,0xFFC5B0FF,.5f);
        for(int side=-1;side<=1;side+=2) {
            float[] wing={x,y,x+side*r*1.5f,y-r*.5f+flap,x+side*r*1.1f,y+r*.25f,
                    x+side*r*.7f,y+r*.05f,x+side*r*.4f,y+r*.55f};
            p.fillPoly(wing,body);p.strokePoly(wing,edge,r*.065f);
        }
        p.fillEllipse(x,y,r*.43f,r*.55f,body);
        p.fillPoly(new float[]{x-r*.38f,y-r*.28f,x-r*.27f,y-r*.80f,x,y-r*.42f,
                x+r*.27f,y-r*.80f,x+r*.38f,y-r*.28f},body);
        p.fillCircle(x-r*.16f,y-r*.08f,r*.10f,0xFFF3DD91);
        p.fillCircle(x+r*.16f,y-r*.08f,r*.10f,0xFFF3DD91);
    }
    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        float r=Boss.bodyR(L)*1.55f,x=b.bodyX(L),y=b.bodyY(L);
        if(b.body!=null) {x=b.body.centreX();y=b.body.centreY();r*=Math.max(.8f,Math.min(1.2f,b.body.spanX()/(Boss.bodyR(L)*2.4f)));}
        if(b.beaten) {supernova(p,c,L,b,x,y,r,alpha);return;}
        float arrival=b.introProgress(),scale=.12f+.88f*arrival*arrival;
        swarm(p,x,y,r*scale,b.age,alpha*arrival,true,b.hurt,0,b);
        if(b.cloud.vulnerable) {
            float pulse=.6f+.4f*(float)Math.sin(c.clock*6);
            ellipse(p,x,y,r*1.35f,r*.85f,Glyph.withAlpha(0xFFFFE9AB,(int)(alpha*(90+pulse*100))),L.unit*.1f);
            float u=(c.clock%1.4f)/1.4f,xx=x+(u<.5f?u*4-1:3-u*4)*r;
            p.line(x-r,y+r*.95f,x+r,y+r*.95f,Glyph.withAlpha(0xFFEEE7FF,(int)(100*alpha)),L.unit*.08f);
            p.fillCircle(xx,y+r*.95f,L.unit*.20f,Glyph.withAlpha(0xFFFFEC9D,(int)(alpha*220)));
        }
        if(b.cloud.slashFlash>0) {
            float f=b.cloud.slashFlash/.32f;
            p.line(x-r*1.5f,y+r*.2f*b.cloud.lastDirection,x+r*1.5f,y-r*.2f*b.cloud.lastDirection,
                    Glyph.withAlpha(0xFFB4C8FF,(int)(f*150)),r*.22f*f);
            p.line(x-r*1.5f,y+r*.2f*b.cloud.lastDirection,x+r*1.5f,y-r*.2f*b.cloud.lastDirection,
                    Glyph.withAlpha(0xFFFFFFFF,(int)(f*255)),r*.055f*f);
        }
    }
    static void projectile(Painter p,GameCore c,Layout L,Boss b,int i) {
        ChaosCloud a=b.cloud;
        float morph=a.morph(b,i);
        float x=b.boltX(i,L),y=b.boltY(i,L),r=L.enemyR*(.58f+.44f*b.boltAt(i));
        int g=b.bglyph[i],color=Skins.keyColor(p,g);
        if(morph<1) {
            float rr=r*(.6f+.4f*morph),amount=1-morph;
            if(Skins.halloween(p))bat(new OpacityPainter(p,amount),x,y,rr,c.clock*13,color);
            Kawaii.incognito(p,g,x,y,rr*.75f,color,amount);
            p.strokePoly(Glyph.hex(x,y,rr),Glyph.withAlpha(color,(int)(180*morph)),rr*.09f);
            return;
        }
        int other=a.mate[i];
        if(other>i && b.blive[other]) {
            float ox=b.boltX(other,L),oy=b.boltY(other,L);
            p.line(x,y,ox,oy,0x559ADFFF,L.unit*.23f);p.line(x,y,ox,oy,0xDDE1E9FF,L.unit*.065f);
        }
        SpaceEnemyArt.comet(p,x,y,r,color,c.clock+i,b.bfast[i]?1:.55f,255,x-b.bsx[i],y-b.bsy[i]);
        p.fillPoly(Glyph.hex(x,y,r),Glyph.withAlpha(color,a.wait[i]>0?60:150));
        p.strokePoly(Glyph.hex(x,y,r),Glyph.withAlpha(color,240),r*.07f);
        Kawaii.draw(new OpacityPainter(p,a.wait[i]>0?.35f:1),g,x,y,r*.61f,color,1,.2f);
        if(b.bhpMax[i]>1)for(int pip=0;pip<b.bhpMax[i];pip++)
            p.fillCircle(x+(pip-1)*r*.4f,y+r*.85f,r*.12f,pip<b.bhp[i]?0xFFFFF3C6:0x55403B61);
    }
    static void supernova(Painter p,GameCore c,Layout L,Boss b,float x,float y,float r,float alpha) {
        float t=b.leaveProgress(),collapse=Math.max(0,1-t/.22f),blast=Math.max(0,(t-.22f)/.78f);
        if(t<.22f)swarm(p,x,y,r*collapse,c.clock*3,alpha,true,1-collapse,0);
        float flash=Math.max(0,1-Math.abs(t-.25f)/.14f);
        p.fillRect(0,0,L.w,L.deckTop,Glyph.withAlpha(0xFFECDEFF,(int)(flash*alpha*220)));
        if(blast<=0)return;
        for(int k=0;k<7;k++) {
            float radius=r*(.2f+blast*(5+k*.6f));
            p.strokeCircle(x,y,radius,Glyph.withAlpha(Glyph.COLOR[k%Glyph.COUNT],(int)((1-blast)*alpha*150)),r*(.10f+k*.025f)*(1-blast));
        }
        for(int i=0;i<52;i++) {
            float angle=i*2.39996f,reach=r*blast*(4+hash(i*31)*4);
            float xx=x+(float)Math.cos(angle)*reach,yy=y+(float)Math.sin(angle)*reach;
            float size=r*(.05f+hash(i*13)*.06f)*(1-blast*.7f);
            int color=Glyph.withAlpha(Glyph.COLOR[i%Glyph.COUNT],(int)(alpha*255*(1-blast)));
            p.line(xx-(float)Math.cos(angle)*r*blast,yy-(float)Math.sin(angle)*r*blast,xx,yy,color,size*.5f);
            p.fillPoly(star(xx,yy,size,size*.32f,4,angle+c.clock),color);
        }
    }
}
