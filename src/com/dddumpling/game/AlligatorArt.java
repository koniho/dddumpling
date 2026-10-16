package com.dddumpling.game;

/** Forest-green jelly skin, a broad snout, little teeth and a tummy-sized arena. */
final class AlligatorArt extends Draw {
    static final int SKIN=0xFF418F69, LIGHT=0xFFADE3B4, DARK=0xFF193F3A, BELLY=0xFFD7DE9C;
    private static final float[] HAND=smooth(new float[]{-.56f,-.73f,-.84f,-.48f,-.94f,-.08f,
            -.89f,.38f,-.69f,.74f,-.47f,.80f,-.27f,.68f,-.20f,.89f,
            .14f,.93f,.32f,.70f,.53f,.80f,.78f,.65f,.91f,.30f,
            .87f,-.18f,.66f,-.61f,.27f,-.84f,-.18f,-.86f});
    private static final float[] HAND_LIGHT=smooth(new float[]{-.61f,-.22f,-.37f,-.50f,
            .10f,-.52f,.51f,-.27f,.48f,.13f,.19f,.28f,-.27f,.26f,-.54f,.09f});
    private static final float[] PEBBLE=smooth(new float[]{-.85f,-.25f,-.30f,-.87f,.45f,-.75f,
            .94f,.02f,.42f,.83f,-.46f,.77f});
    private static final float[][] NAILS=clawShapes();
    private AlligatorArt() {}
    static float damageFlash(float hurt) {
        if(hurt<=0)return 0;
        float t=(1-hurt)/2.6f;
        if(t<.055f)return .68f;
        if(t<.095f)return .20f;
        if(t<.155f)return .58f;
        return .58f*(1-AlligatorArrival.ramp(t,.155f,.24f));
    }
    static int damageColor(float hurt) {
        float t=(1-hurt)/2.6f;
        return t<.095f?0xFFFF786E:0xFFE84064;
    }
    private static float phaseTint(Boss b) {
        float tint=b.hp<b.hpMax*.25f?1:b.hp<b.hpMax*.5f?.75f:0;
        Alligator a=b.alligator;
        if(a.showoff()) {
            float before=a.showoffCount==3?.75f:0;
            tint=before+(tint-before)*AlligatorArrival.ramp(a.showoffAge,0,.45f);
        }
        return tint;
    }
    static void draw(Painter painter,GameCore c,Layout L,Boss b,float alpha) {
        Alligator a=b.alligator;
        boolean victory=c.dying() && c.bossVictory==b;
        float arrival=b.introProgress();
        float seconds=b.beaten?b.leaveProgress()*Boss.LEAVE:0;
        float defeated=b.beaten?AlligatorArrival.ramp(seconds,0,.25f):0;
        float opacity=alpha,entryY=AlligatorArrival.offset(L,arrival)
                +(b.beaten?AlligatorAtmosphere.offset(L,seconds):0);
        float flies=alpha*AlligatorArrival.ramp(arrival,.55f,1)
                *(1-AlligatorArrival.ramp(seconds,.5f,1.6f));
        AlligatorAtmosphere.fireflies(painter,L,c.clock,flies,false);
        if(b.beaten)AlligatorAtmosphere.water(painter,L,seconds,alpha,false);
        Painter scene=opacity==1?painter:new OpacityPainter(painter,opacity);
        AlligatorArrival.particles(scene,L,arrival,false);
        scene.save();
        if(b.beaten)scene.clipRect(0,0,L.w,AlligatorAtmosphere.waterY(L));
        scene.translate(b.beaten?L.unit*.6f*defeated*(float)Math.sin(seconds*7f):0,entryY);
        float cancel=a.cancelFlash(),flash=Math.max(damageFlash(b.hurt),cancel),phase=phaseTint(b);
        Painter creature=flash==0?scene:new ColorFadePainter(scene,cancel>0?0xFFFFB5A0:damageColor(b.hurt),flash,1);
        if(phase>0)creature=new GatorPalette(creature,phase);
        Painter p=creature;
        boolean bodyVisible=L.playTop+(L.dangerY-L.playTop)*.28f+entryY<L.h;
        if(bodyVisible && a.tummy!=null) {
            float x=a.tummy.centreX(),y=a.tummy.centreY();
            float[] skin=a.bodyOutline(L);
            p.fillPoly(skin,DARK);
            p.fillPoly(inset(skin,x,y,.98f,.99f),SKIN);
            p.fillPoly(inset(skin,x-L.w*.035f,y,.88f,.97f),0xFF65AC7C);
            p.fillPoly(inset(skin,x,y,.72f,.92f),0xFF657F50);
            for(int side=-1;side<=1;side+=2)for(int i=0;i<12;i++) {
                float sy=L.playTop+(L.dangerY-L.playTop)*(.31f+i*.095f);
                if(sy-L.w*.04f+entryY>L.h)continue;
                float sx=a.centreAt(L,sy)+side*L.w*(.29f+.014f*(i%2));
                p.fillEllipse(sx,sy,L.w*.024f,L.w*.040f,0x5534604B);
                p.arc(sx,sy-L.w*.007f,L.w*.016f,L.w*.026f,205,115,0x609ADCAA,L.w*.008f);
            }
            for(int i=0;i<Alligator.SEGMENTS;i++) {
                Softbody segment=a.segments[i];if(segment==null)continue;
                float sx=segment.centreX(),sy=segment.centreY(),rx=segment.radiusX(),ry=segment.radiusY()*.62f;
                if(sy-ry*1.5f+entryY>L.h)continue;
                float[] pad=bellyPlate(segment);
                p.fillPoly(inset(pad,sx,sy+ry*.24f,1.015f,1.12f),0x80506943);
                p.fillPoly(pad,0xFF788B54);
                p.fillPoly(inset(pad,sx,sy-ry*.14f,.99f,.90f),0xFFB3BC76);
                p.fillPoly(inset(pad,sx-rx*.035f,sy-ry*.22f,.97f,.78f),BELLY);
                p.fillPoly(inset(pad,sx-rx*.12f,sy-ry*.43f,.83f,.50f),0xFFEDF0BB);
                p.polyline(new float[]{sx-rx*.84f,sy-ry*.48f,sx-rx*.72f,sy-ry*.62f,
                        sx-rx*.42f,sy-ry*.68f,sx+rx*.42f,sy-ry*.68f,sx+rx*.71f,sy-ry*.60f},
                        0xA5FFFFD9,L.unit*.12f);
                p.polyline(new float[]{sx-rx*.82f,sy+ry*.45f,sx-rx*.62f,sy+ry*.60f,
                        sx+rx*.62f,sy+ry*.60f,sx+rx*.82f,sy+ry*.43f},0x6074844D,L.unit*.09f);
            }
        }
        // Hands cross over the face when guarding, striking or celebrating.
        if(b.body!=null && b.body.centreY()-b.body.radiusY()*1.8f+entryY<L.h)head(creature,
                null,b.body.centreX(),b.body.centreY()+Boss.bodyR(L)*.18f*defeated,
                b.body.radiusX(),b.body.radiusY(),victory?c.clock:b.age,
                a.daze>0,0,b.beaten?Alligator.DAZED_EYES:victory?Alligator.CHEEKY_EYES:a.mood(b),
                a.gazeX(),a.gazeY(),b.beaten || victory?null:a.eyes,
                b.beaten?defeated:a.vulnerablePose,victory?.5f+.5f*(float)Math.sin(c.clock*11):a.laugh());
        if(bodyVisible)for(int i=0;i<3;i++) {
            float tx=a.drawnX(L,i),ty=a.drawnY(L,i);
            float rx=a.rootX(L,i),ry=a.rootY(L,i);
            if(i<2 && victory) {
                float blend=AlligatorArrival.ramp(c.deathProgress(),0,.12f);
                tx+=(AlligatorAtmosphere.waveX(L,i,c.clock)-tx)*blend;
                ty+=(AlligatorAtmosphere.waveY(L,i,c.clock)-ty)*blend;
            } else if(i<2 && b.beaten) {
                tx+=(rx+L.w*.06f*(float)Math.sin(seconds*6f+i*2f)-tx)*defeated;
                ty+=(ry+(L.dangerY-L.playTop)*.35f-ty)*defeated;
            }
            if(Math.min(ry,ty)-L.w*.30f+entryY>L.h)continue;
            boolean live=a.retaliation==0 && a.attacking(i) && !b.beaten && !victory;
            float pulse=.35f+.65f*(.5f+.5f*(float)Math.sin(b.age*Softbody.TAU*2.5f));
            float tell=live?pulse:0;
            if(i==2 && !b.beaten && !victory)tell=Math.max(tell,pulse*a.tailFlick());
            if(i==2)limb(p,rx,ry,tx,ty,L.w*.12f,true,SKIN,b.age,tell);
            else {
                float ex=a.elbowX(L,i,tx),ey=a.elbowY(L,i,ty);
                if(victory || b.beaten) {
                    ex=(rx+tx)*.5f+(i==0?-1:1)*L.w*(victory?.10f:.025f);
                    ey=(ry+ty)*.5f;
                }
                arm(p,rx,ry,ex,ey,tx,ty,L.w*.09f,tell);
            }
            if(live && a.showGuide() && !a.showoff()) {
                float r=L.w*.115f*(1+.08f*(float)Math.sin(b.age*12));
                p.strokeCircle(tx,ty,r,0xC3E5FFD5,L.unit*.12f);
                float d=L.unit*.9f;
                p.line(tx-d,ty,tx+d,ty,0xDDFFFFFF,L.unit*.12f);
                p.line(tx-d,ty,tx-d*.4f,ty-d*.42f,0xDDFFFFFF,L.unit*.10f);
                p.line(tx+d,ty,tx+d*.4f,ty+d*.42f,0xDDFFFFFF,L.unit*.10f);
            }
        }
        p=scene;
        float y=b.bodyY(L)+Boss.bodyR(L)*1.70f;
        if(!b.beaten && !victory && b.intro<=0) {
            if(a.daze<=0 && a.closing<=0 && a.showGuide() && a.retaliation==0 && !a.showoff())
                p.text("SWIPE TO PARRY",L.w*.5f,y+L.unit*1.5f,type(L.unit*.65f),DARK,Painter.CENTER,true);
        }
        if(!b.beaten && !victory && a.showoff() && a.showoffLimb()>=0) {
            int beat=a.showoffLimb();float reach=a.showoffReach(beat);
            float x=a.drawnX(L,beat),yy=a.drawnY(L,beat)-L.unit*(2.6f+reach*.5f);
            float size=type(L.unit*(1.1f+.3f*reach));
            p.text(""+(beat+1),x+L.unit*.06f,yy+L.unit*.08f,size,DARK,Painter.CENTER,true);
            p.text(""+(beat+1),x,yy,size,0xFFFFE993,Painter.CENTER,true);
        }
        if(!b.beaten && !victory && a.strokeFade>0)p.line(a.strokeX,a.strokeY,a.strokeEndX,a.strokeEndY,
                fadeBy(a.guard.secondary,a.strokeFade/.3f),L.unit*.4f);
        scene.restore();
        AlligatorArrival.particles(scene,L,arrival,true);
        if(b.beaten)AlligatorAtmosphere.water(painter,L,seconds,alpha,true);
        AlligatorAtmosphere.fireflies(painter,L,c.clock,flies,true);
    }
    private static float[] bellyPlate(Softbody segment) {
        float[] pad=segment.outline();
        float x=segment.centreX(),y=segment.centreY();
        float rx=Math.max(1,segment.radiusX()),ry=Math.max(1,segment.radiusY());
        // Flatten each spring outline into a thin rounded rectangle, retaining its wobble.
        for(int i=0;i<pad.length;i+=2) {
            float dx=(pad[i]-x)/rx,dy=(pad[i+1]-y)/ry;
            pad[i]=x+Math.copySign((float)Math.pow(Math.abs(dx),.60f),dx)*rx;
            pad[i+1]=y+Math.copySign((float)Math.pow(Math.abs(dy),.60f),dy)*ry*.62f;
        }
        return pad;
    }
    private static float[] inset(float[] skin,float x,float y,float sx,float sy) {
        float[] out=new float[skin.length];
        for(int i=0;i<skin.length;i+=2){out[i]=x+(skin[i]-x)*sx;out[i+1]=y+(skin[i+1]-y)*sy;}
        return out;
    }
    private static Painter attackGlow(Painter p,float[] outline,float width,float pulse) {
        if(pulse<=0)return p;
        p.strokePoly(outline,fadeBy(0x26FFFFAA,pulse),width*.30f);
        p.strokePoly(outline,fadeBy(0x65FFFFCD,pulse),width*.12f);
        p.strokePoly(outline,fadeBy(0xB5FFFFFF,pulse),width*.045f);
        return new ColorFadePainter(p,0xFFFFFFFF,.25f+.42f*pulse,1);
    }
    /** Rounded, traced outlines; every layer shares the same bent centreline. */
    static void limb(Painter p,float x,float y,float tx,float ty,float width,boolean tail,int color,float clock) {
        limb(p,x,y,tx,ty,width,tail,color,clock,0);
    }
    private static void limb(Painter p,float x,float y,float tx,float ty,float width,boolean tail,int color,float clock,float tell) {
        float dx=tx-x,dy=ty-y,d=Math.max(1,(float)Math.hypot(dx,dy)),nx=-dy/d,ny=dx/d;
        // Keep the ridge on top on either side, with the full base continuing offscreen.
        if(tail && ny>0) {nx=-nx;ny=-ny;}
        float[] shape=new float[68],shine=new float[26];
        float bend=width*(tail?.55f:.72f)*(1+.08f*(float)Math.sin(clock*4));
        for(int i=0;i<17;i++) {
            float u=i/16f,w=limbWidth(width,u,tail);
            float cx=x+dx*u+nx*bend*(float)Math.sin(u*Math.PI),cy=y+dy*u+ny*bend*(float)Math.sin(u*Math.PI);
            shape[i*2]=cx+nx*w;shape[i*2+1]=cy+ny*w;
            shape[(33-i)*2]=cx-nx*w;shape[(33-i)*2+1]=cy-ny*w;
        }
        float[] contour=smooth(shape);
        if(tail)p=attackGlow(p,contour,width,tell);
        p.fillPoly(contour,DARK);
        p.fillPoly(inset(contour,(x+tx)*.5f,(y+ty)*.5f,.94f,.96f),color);
        for(int i=0;i<13;i++) {
            float u=.06f+i*.071f,w=limbWidth(width,u,tail);
            shine[i*2]=x+dx*u+nx*(bend*(float)Math.sin(u*Math.PI)+w*.38f);
            shine[i*2+1]=y+dy*u+ny*(bend*(float)Math.sin(u*Math.PI)+w*.38f);
        }
        ribbon(p,shine,width*.48f,Glyph.mix(color,LIGHT,.43f));
        ribbon(p,shine,width*.17f,0x559BE2B4);
        if(tail) {
            for(int i=0;i<3;i++) {
                float u=.26f+i*.25f,w=limbWidth(width,u,true),bow=bend*(float)Math.sin(u*Math.PI);
                float cx=x+dx*u+nx*bow,cy=y+dy*u+ny*bow;
                p.line(cx-nx*w*.74f,cy-ny*w*.74f,cx+nx*w*.63f,cy+ny*w*.63f,0x70446143,width*.13f);
                p.line(cx-nx*w*.63f-dx/d*width*.09f,cy-ny*w*.63f-dy/d*width*.09f,
                        cx+nx*w*.59f-dx/d*width*.09f,cy+ny*w*.59f-dy/d*width*.09f,0x6083C88A,width*.10f);
                float[] scale={-.50f,.62f,-.35f,1.00f,-.15f,1.43f,.02f,1.51f,
                        .20f,1.22f,.54f,.71f,.21f,.57f};
                for(int k=0;k<scale.length;k+=2) {
                    float along=scale[k]*w,across=scale[k+1]*w;
                    scale[k]=cx+dx/d*along+nx*across;scale[k+1]=cy+dy/d*along+ny*across;
                }
                float sx=cx+nx*w*.95f,sy=cy+ny*w*.95f;
                float[] scute=smooth(scale);
                p.fillPoly(scute,DARK);
                p.fillPoly(inset(scute,sx,sy,.78f,.76f),0xFF639E70);
                p.fillPoly(inset(scute,sx-dx/d*w*.09f,sy-dy/d*w*.09f,.56f,.57f),
                        Glyph.mix(0xFFB8E2A5,0xFFF1FFD2,u>.55f?tell*.15f:0));
                p.line(cx+nx*w*.78f,cy+ny*w*.78f,cx+nx*w*1.24f,cy+ny*w*1.24f,0x9090C493,width*.035f);
                for(int j=0;j<3;j++) {
                    float offset=(j-1)*w*.28f;
                    float px=cx+dx/d*offset-nx*w*.23f,py=cy+dy/d*offset-ny*w*.23f;
                    skinPebble(p,px,py,width*(.045f+.012f*(j%2)),width*.075f);
                }
            }
            p.fillCircle(tx,ty,width*.40f,Glyph.mix(color,LIGHT,tell*.15f));
            p.fillCircle(tx-width*.08f,ty-width*.12f,width*.16f,0x608BDAA7);
        } else {
            palm(p,tx,ty,dx/d,dy/d,width,color,0);
            for(int i=0;i<3;i++) {
                float u=.25f+i*.15f,px=x+dx*u+nx*bend*(float)Math.sin(u*Math.PI),py=y+dy*u+ny*bend*(float)Math.sin(u*Math.PI);
                p.fillEllipse(px-nx*width*.40f,py-ny*width*.40f,width*.13f,width*.18f,0x50436A47);
                p.arc(px,py,width*.48f,width*.22f,25,100,0x6039674C,width*.07f);
            }
        }
    }
    private static void palm(Painter p,float tx,float ty,float ux,float uy,float width,int color,float tell) {
        p=new HeadTilt(p,tx,ty,(float)Math.atan2(uy,ux)-(float)Math.PI*.5f,true);
        if(tell>0)p=attackGlow(p,scaled(HAND,0,0,width,width),width,tell);
        ArtStamp cache=p.stamps();
        if(color==SKIN && cache!=null && cache.stamp(ArtStamp.PALM,0,0,width,width,0,0,0))return;
        palmArt(p,width,color);
    }
    static void stampArt(Painter p,int art,float unit) {
        if(art==ArtStamp.PALM)palmArt(p,unit,SKIN);
        else if(art==ArtStamp.PEBBLE)pebbleArt(p,0,0,unit,unit);
    }
    private static void palmArt(Painter p,float width,int color) {
        // Soft mitten fingers and broad shading match the tummy and muzzle.
        float[] hand=scaled(HAND,0,0,width,width);
        p.fillPoly(hand,DARK);
        p.fillPoly(inset(hand,0,0,.94f,.94f),color);
        p.fillPoly(scaled(HAND_LIGHT,-width*.12f,-width*.17f,width,width),Glyph.mix(color,LIGHT,.28f));
        for(int i=0;i<NAILS.length;i++) {
            int ink=i%3==0?0xFF527855:i%3==1?BELLY:0xFFF1F2C7;
            p.fillPoly(scaled(NAILS[i],0,0,width,width),ink);
        }
        for(int i=0;i<3;i++) {
            float x=width*(i-1)*.30f,y=width*(-.25f+.08f*(i%2));
            p.fillEllipse(x,y,width*.095f,width*.065f,0x55407859);
            p.fillEllipse(x-width*.012f,y-width*.02f,width*.075f,width*.043f,0x7088CB9C);
        }
    }
    private static float[][] clawShapes() {
        float[] nail=smooth(new float[]{-.17f,.09f,-.20f,.26f,-.13f,.40f,
                .02f,.47f,.13f,.38f,.18f,.23f,.12f,.09f});
        float[][] shapes=new float[9][];
        for(int i=-1;i<=1;i++)for(int layer=0;layer<3;layer++) {
            float[] shape=layer==0?nail.clone():inset(nail,-.025f,.18f,layer==1?.78f:.45f,layer==1?.82f:.55f);
            double angle=-i*16*Math.PI/180;float co=(float)Math.cos(angle),si=(float)Math.sin(angle);
            for(int k=0;k<shape.length;k+=2) {
                float x=shape[k],y=shape[k+1];
                shape[k]=i*.53f+x*co-y*si;shape[k+1]=(i==0?.54f:.43f)+x*si+y*co;
            }
            shapes[(i+1)*3+layer]=shape;
        }
        return shapes;
    }
    private static void skinPebble(Painter p,float x,float y,float rx,float ry) {
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.stamp(ArtStamp.PEBBLE,x,y,rx,ry,0,0,0))return;
        pebbleArt(p,x,y,rx,ry);
    }
    private static void pebbleArt(Painter p,float x,float y,float rx,float ry) {
        float[] scale=scaled(PEBBLE,x,y,rx,ry);
        p.fillPoly(scale,0x35305E48);
        p.fillPoly(inset(scale,x-rx*.12f,y-ry*.22f,.77f,.70f),0x557CC697);
        p.arc(x-rx*.05f,y-ry*.18f,rx*.62f,ry*.45f,200,111,0x659AD9AB,ry*.20f);
    }
    /** Filled ribbons avoid stacked round line caps along translucent skin highlights. */
    private static void ribbon(Painter p,float[] line,float width,int color) {
        int n=line.length/2;float[] shape=new float[n*4];
        for(int i=0;i<n;i++) {
            int before=Math.max(0,i-1)*2,after=Math.min(n-1,i+1)*2;
            float dx=line[after]-line[before],dy=line[after+1]-line[before+1];
            float len=Math.max(.001f,(float)Math.hypot(dx,dy));
            float r=width*.5f*(.18f+.82f*(float)Math.sin(Math.PI*i/(n-1)));
            shape[i*2]=line[i*2]-dy/len*r;shape[i*2+1]=line[i*2+1]+dx/len*r;
            shape[(2*n-1-i)*2]=line[i*2]+dy/len*r;
            shape[(2*n-1-i)*2+1]=line[i*2+1]-dx/len*r;
        }
        p.fillPoly(smooth(shape),color);
    }
    /** One continuous arm contour bends at a soft elbow and blends into the shoulder. */
    private static void arm(Painter p,float x,float y,float ex,float ey,float tx,float ty,float w,float tell) {
        float cx=2*ex-(x+tx)*.5f,cy=2*ey-(y+ty)*.5f;
        final int samples=24;
        float[] outline=new float[(samples+1)*4],shine=new float[(samples+1)*2],shade=new float[(samples+1)*2];
        for(int n=0;n<=samples;n++) {
            float u=n/(float)samples,v=1-u;
            float px=v*v*x+2*v*u*cx+u*u*tx,py=v*v*y+2*v*u*cy+u*u*ty;
            float dx=2*(v*(cx-x)+u*(tx-cx)),dy=2*(v*(cy-y)+u*(ty-cy));
            float len=Math.max(1,(float)Math.hypot(dx,dy)),nx=-dy/len,ny=dx/len;
            float upper=(u-.22f)/.14f,elbow=(u-.52f)/.12f,fore=(u-.76f)/.13f;
            float width=w*(1.22f-.40f*u+.20f*(float)Math.exp(-upper*upper)
                    +.24f*(float)Math.exp(-elbow*elbow)+.20f*(float)Math.exp(-fore*fore));
            float edge=(float)Math.sin(Math.PI*u);
            float outer=width+w*edge*(.070f*(float)Math.sin(u*39+.4f)+.025f*(float)Math.sin(u*67));
            float inner=width+w*edge*(.045f*(float)Math.sin(u*34+1.8f)+.020f*(float)Math.sin(u*57));
            outline[n*2]=px+nx*outer;outline[n*2+1]=py+ny*outer;
            outline[(samples*2+1-n)*2]=px-nx*inner;outline[(samples*2+1-n)*2+1]=py-ny*inner;
            shine[n*2]=px+nx*width*.30f;shine[n*2+1]=py+ny*width*.30f;
            shade[n*2]=px-nx*width*.53f;shade[n*2+1]=py-ny*width*.53f;
        }
        float[] skin=smooth(outline);
        p.fillPoly(skin,DARK);
        p.fillPoly(inset(skin,ex,ey,.95f,.98f),0xFF326D54);
        p.fillPoly(inset(skin,ex-w*.10f,ey-w*.13f,.86f,.97f),SKIN);
        ribbon(p,shade,w*.48f,0x604D6750);
        ribbon(p,shine,w*.90f,0xFF66AE80);
        ribbon(p,shine,w*.45f,0x908FD5A3);
        ribbon(p,shine,w*.12f,0x70C9F1BD);
        // Sloping shoulder blends into the hanging arm, with soft uneven skin folds.
        float[] shoulder=form(x,y,w,w,-1.02f,-.49f,-.64f,-.76f,-.18f,-.71f,
                .32f,-.59f,.78f,-.28f,1.02f,.15f,.96f,.51f,.66f,.83f,
                .22f,.91f,-.16f,.83f,-.60f,.66f,-.89f,.29f);
        p.fillPoly(shoulder,SKIN);
        p.fillPoly(inset(shoulder,x-w*.12f,y-w*.09f,.82f,.79f),0xFF68AB7B);
        p.arc(x-w*.08f,y,w*.77f,w*.60f,198,92,0x70A5DFB0,w*.075f);
        p.arc(x+w*.04f,y+w*.08f,w*.90f,w*.70f,24,100,0x50366148,w*.055f);
        // Curved elbow folds shade the bend without a separate round button on top.
        Painter skinPainter=p;
        p=new HeadTilt(p,ex,ey,(float)Math.atan2(ty-y,tx-x)-(float)Math.PI*.5f,true);
        p.fillPoly(form(0,0,w,w,-.68f,-.14f,-.40f,-.36f,.20f,-.38f,.66f,-.09f,
                .52f,.22f,.06f,.30f,-.46f,.20f),0x407EC28F);
        for(int i=0;i<3;i++) {
            float yy=w*(.04f+i*.18f),rx=w*(.71f-i*.13f);
            p.arc(0,yy,rx,w*.25f,22,131,0x8042654C,w*(.050f-i*.009f));
            p.arc(-w*.025f,yy-w*.055f,rx*.93f,w*.22f,26,123,0x759CD3A2,w*.034f);
        }
        p=skinPainter;
        for(int row=0;row<3;row++)for(int i=0;i<8;i++) {
            float u=.12f+i*.10f+row*.023f,v=1-u;
            float px=v*v*x+2*v*u*cx+u*u*tx,py=v*v*y+2*v*u*cy+u*u*ty;
            float dx=2*(v*(cx-x)+u*(tx-cx)),dy=2*(v*(cy-y)+u*(ty-cy));
            float len=Math.max(1,(float)Math.hypot(dx,dy));
            float offset=w*((row-1)*.42f+.055f*(float)Math.sin(i*2.3f+row));
            px+=dy/len*offset;py-=dx/len*offset;
            float size=w*(.09f+.026f*(float)Math.sin(i*1.8f+row*2));
            skinPebble(new HeadTilt(p,px,py,(float)Math.atan2(dy,dx)-(float)Math.PI*.5f,true),
                    0,0,size*.94f,size*.70f);
        }
        float dx=tx-cx,dy=ty-cy,len=Math.max(1,(float)Math.hypot(dx,dy));
        palm(p,tx,ty,dx/len,dy/len,w,SKIN,tell);
    }
    private static float limbWidth(float w,float u,boolean tail) {
        float taper=tail?1.14f-u*.74f:1.02f+.10f*(float)Math.sin(u*Math.PI)-u*.13f;
        if(tail)taper+=(float)Math.sin(u*Math.PI)*(.085f*(float)Math.cos(u*Math.PI*6)
                +.035f*(float)Math.sin(u*Math.PI*15));
        return w*taper;
    }
    private static float[] smooth(float[] points) {
        int n=points.length/2,steps=3;float[] out=new float[n*steps*2];
        for(int i=0;i<n;i++)for(int j=0;j<steps;j++)for(int axis=0;axis<2;axis++) {
            float a=points[((i+n-1)%n)*2+axis],b=points[i*2+axis],c=points[((i+1)%n)*2+axis],d=points[((i+2)%n)*2+axis];
            float t=j/(float)steps;
            out[(i*steps+j)*2+axis]=.5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
        }
        return out;
    }
    static float[] form(float x,float y,float w,float h,float... points) {
        float[] out=smooth(points);
        for(int i=0;i<out.length;i+=2){out[i]=x+out[i]*w;out[i+1]=y+out[i+1]*h;}
        return out;
    }
    private static float[] scaled(float[] shape,float x,float y,float w,float h) {
        float[] out=new float[shape.length];
        for(int i=0;i<out.length;i+=2){out[i]=x+shape[i]*w;out[i+1]=y+shape[i+1]*h;}
        return out;
    }
    private static float[] skull(float x,float y,float w,float h) {
        return form(x,y,w,h,-.89f,.32f,-.88f,-.27f,-.66f,-.95f,-.37f,-1.11f,-.13f,-.79f,
                .12f,-1.02f,.43f,-.79f,.52f,-.18f,.86f,.10f,1.00f,.52f,.93f,1.02f,.58f,1.35f,
                .08f,1.39f,-.49f,1.06f,-.76f,.74f);
    }
    static float[] headSilhouette(Softbody body,float clock) {
        float x=body.centreX(),y=body.centreY();
        return new HeadTilt(null,x,y,.16f+.018f*(float)Math.sin(clock*2))
                .turn(skull(x,y,body.radiusX(),body.radiusY()));
    }
    static void head(Painter p,float[] skin,float x,float y,float w,float h,float clock,boolean dazed,float hurt) {
        head(p,skin,x,y,w,h,clock,dazed,hurt,-1,0);
    }
    static void head(Painter p,float[] skin,float x,float y,float w,float h,float clock,boolean dazed,float hurt,int mood,float look) {
        head(p,skin,x,y,w,h,clock,dazed,hurt,mood,look,0);
    }
    static void head(Painter p,float[] skin,float x,float y,float w,float h,float clock,boolean dazed,float hurt,int mood,float look,float lookY) {
        head(p,skin,x,y,w,h,clock,dazed,hurt,mood,look,lookY,null,0,0);
    }
    private static void head(Painter p,float[] skin,float x,float y,float w,float h,float clock,boolean dazed,float hurt,int mood,float look,float lookY,Alligator.Eyes eyes,float slump,float laugh) {
        y-=h*.12f*laugh;
        p=new HeadTilt(p,x,y,.16f+.018f*(float)Math.sin(clock*2)
                +slump*(.22f+.055f*(float)Math.sin(clock*5))+laugh*.10f);
        int col=Glyph.mix(SKIN,0xFFEDFFF2,hurt*.45f);
        // Asymmetric brow and descending muzzle: a three-quarter view looking toward the player.
        for(int i=0;i<4;i++) {
            float sx=x-w*(.83f-i*.12f),sy=y+h*(.15f-i*.36f);
            float[] scute=form(sx,sy,w*.18f,h*.28f,-1,.2f,-.55f,-1.0f,.45f,-.28f,.7f,.7f);
            p.fillPoly(scute,DARK);p.fillPoly(inset(scute,sx,sy,.70f,.67f),0xFF8ACF93);
        }
        float[] skull=skull(x,y,w,h);
        p.fillPoly(skull,DARK);p.fillPoly(inset(skull,x,y,.975f,.955f),col);
        p.fillPoly(inset(skull,x-w*.17f,y-h*.23f,.87f,.77f),Glyph.mix(col,LIGHT,.25f));
        float[] chin=form(x,y,w,h,-.70f,.49f,-.24f,.60f,.31f,.81f,.89f,.72f,.75f,1.12f,.26f,1.29f,-.33f,1.06f,-.64f,.81f);
        p.fillPoly(chin,0xFF859953);p.fillPoly(inset(chin,x-w*.06f,y+h*.87f,.94f,.69f),BELLY);
        p.arc(x+w*.10f,y+h*.76f,w*.58f,h*.37f,30,127,0xA4F1E8B7,w*.025f);
        float[] muzzle=form(x,y,w,h,-.52f,.02f,-.19f,-.21f,.24f,-.10f,.46f,.17f,.83f,.17f,.97f,.48f,
                .87f,.89f,.49f,1.01f,.14f,.77f,-.13f,.84f,-.55f,.51f);
        p.fillPoly(muzzle,DARK);p.fillPoly(inset(muzzle,x+w*.18f,y+h*.40f,.98f,.88f),Glyph.mix(col,LIGHT,.17f));
        p.fillPoly(inset(muzzle,x-w*.02f,y+h*.17f,.82f,.57f),Glyph.mix(col,LIGHT,.38f));
        p.arc(x+w*.61f,y+h*.45f,w*.26f,h*.24f,190,114,0x96D2F3CB,w*.026f);
        for(int side=0;side<2;side++) {
            float ex=x+w*(side==0?-.43f:.20f),ey=y+h*(side==0?-.48f:-.58f),scale=side==0?1:.79f;
            p.fillEllipse(ex,ey,w*.225f*scale,h*.59f*scale,col);
            p.fillEllipse(ex-w*.016f,ey-h*.045f,w*.170f*scale,h*.47f*scale*(eyes==null?1:1-eyes.closed()*.60f),0xFFDAF2BF);
            boolean blink=mood==0 && Math.sin(clock*1.7f)>.985f;
            if(eyes!=null)animatedEyes(p,ex,ey,w,h,scale,eyes,clock);
            else if(mood==Trinket.FIGHT)Trinket.fightingEyes(p,ex,ey,w*.85f*scale,side==0?-1:1,clock,1);
            else if(mood>=Alligator.WINDUP_EYES)bossEyes(p,ex,ey,w,h,scale,mood,look,lookY,clock);
            else if(mood==3) p.polyline(new float[]{ex-w*.09f,ey-h*.14f,ex+w*.035f,ey,ex-w*.09f,ey+h*.14f},DARK,w*.030f);
            else if(mood==5 || mood==7) p.arc(ex,ey+h*.10f,w*.105f*scale,h*.19f*scale,194,151,DARK,w*.028f);
            else if(mood==1 || mood==8 || mood==9 || blink) p.arc(ex,ey+h*.08f,w*.105f*scale,h*.20f*scale,188,164,DARK,w*.028f);
            else if(mood==4) p.fillPoly(star(ex,ey,w*.115f,w*.05f,5,clock*.3f),DARK);
            else if(dazed) p.arc(ex,ey,w*.105f*scale,h*.23f*scale,12,160,DARK,w*.025f);
            else {
                p.fillEllipse(ex+w*(.025f+look*.12f),ey+h*.10f,w*.115f*scale,h*(mood==2?.42f:.34f)*scale,DARK);
                p.fillEllipse(ex+w*(.003f+look*.12f),ey-h*.01f,w*.046f*scale,h*.13f*scale,0xFFF5FFED);
                p.fillCircle(ex+w*(.072f+look*.12f),ey+h*.21f,w*.018f*scale,0xFFD2F0CF);
            }
            p.arc(ex-w*.025f,ey-h*.15f,w*.19f*scale,h*.49f*scale,208,96,0x99D3F3C0,w*.025f);
        }
        // Nostrils and a soft cheek dimple follow the descending snout rather than a frontal face.
        for(int i=0;i<2;i++) {
            float nx=x+w*(.45f+i*.33f),ny=y+h*(.50f+i*.06f);
            p.fillEllipse(nx,ny,w*.045f,h*.092f,DARK);
            p.arc(nx,ny-h*.02f,w*.069f,h*.13f,205,120,0x88D2F5BE,w*.013f);
        }
        p.fillEllipse(x-w*.59f,y+h*.35f,w*.115f,h*.12f,0x80BDDC9B);
        for(float[] tooth:new float[][]{{-.28f,.86f,.095f},{.52f,1.02f,.08f},{.76f,.91f,.07f}}) {
            float tx=x+w*tooth[0],ty=y+h*tooth[1],tw=w*tooth[2];
            float[] fang=form(tx,ty,tw,h*.26f,-1,-.2f,1,-.15f,.10f,1f);
            p.fillPoly(fang,DARK);p.fillPoly(inset(fang,tx,ty,.64f,.64f),0xFFF2F5D1);
        }
        if(laugh>0) {
            float mx=x+w*.23f,my=y+h*.87f;
            float[] grin=form(mx,my,w*.39f,h*.35f*laugh,
                    -1,-.25f,-.45f,-.10f,.40f,-.24f,1,-.65f,.70f,.72f,.05f,1,-.65f,.65f);
            p.fillPoly(grin,DARK);
            p.fillEllipse(mx+w*.02f,my+h*.16f*laugh,w*.18f,h*.10f*laugh,0xFFEF8C93);
            p.arc(x-w*.55f,y+h*.40f,w*.13f,h*.14f,20,130,0xFFC5E4A1,w*.025f);
        }
        if(dazed)for(int i=0;i<3;i++) {
            float a=clock*3+i*Softbody.TAU/3;
            float sx=x+(float)Math.cos(a)*w*.7f,sy=y-h*1.34f+(float)Math.sin(a)*h*.18f;
            int starColor=fadeBy(0xFFFFED9C,eyes==null?1:slump);
            p.line(sx-w*.06f,sy,sx+w*.06f,sy,starColor,w*.025f);
            p.line(sx,sy-h*.10f,sx,sy+h*.10f,starColor,w*.025f);
        }
    }
    private static void animatedEyes(Painter p,float x,float y,float w,float h,float scale,Alligator.Eyes eye,float clock) {
        float closed=eye.closed(),pupil=1-Math.max(closed,eye.dizzy);
        float px=x+w*(.012f+eye.x*.075f),py=y+h*(.07f+eye.y*.12f);
        float shrink=1-closed*.90f-eye.dizzy*.55f;
        shrink=Math.max(.05f,shrink);
        p.fillEllipse(px,py,w*eye.rx*scale*(1-eye.dizzy*.6f),h*eye.ry*scale*shrink,fadeBy(DARK,pupil));
        p.fillEllipse(px-w*.025f,py-h*.08f*shrink,w*.030f*scale,h*.09f*scale*shrink,fadeBy(0xFFF5FFED,pupil));
        p.fillCircle(px+w*.033f,py+h*.12f*shrink,w*.025f,fadeBy(0xFFF9E3A1,eye.glint*pupil));
        // The same line grows from a flattened lid, bending into a smile or a wince.
        if(closed>.001f) {
            float pain=eye.squeeze/Math.max(.001f,eye.squeeze+eye.smile);
            float[] lid=new float[50];
            for(int i=0;i<25;i++) {
                float u=i/24f,a=(188+164*u)*(float)Math.PI/180;
                float sx=(float)Math.cos(a)*.105f,sy=.08f+(float)Math.sin(a)*.20f;
                float hx=-.09f+.125f*(1-Math.abs(2*u-1)),hy=-.14f+.28f*u;
                lid[i*2]=x+w*scale*(sx+(hx-sx)*pain)*(.65f+.35f*closed);
                lid[i*2+1]=y+h*scale*(.08f+(sy+(hy-sy)*pain-.08f)*closed);
            }
            p.polyline(lid,fadeBy(DARK,closed),w*.028f);
        }
        if(eye.dizzy>.001f) {
            float[] spiral=new float[50];
            for(int i=0;i<25;i++) {
                float u=i/24f,a=u*Softbody.TAU*1.65f+clock*2;
                spiral[i*2]=x+(float)Math.cos(a)*w*.12f*scale*u*(.3f+.7f*eye.dizzy);
                spiral[i*2+1]=y+(float)Math.sin(a)*h*.28f*scale*u*(.3f+.7f*eye.dizzy);
            }
            p.polyline(spiral,fadeBy(DARK,eye.dizzy),w*.025f);
        }
        p.line(x-w*.10f,y-h*.35f,x+w*.10f,y-h*(.35f-eye.tilt),fadeBy(DARK,eye.brow),w*.024f);
    }
    private static void bossEyes(Painter p,float x,float y,float w,float h,float scale,int mood,float look,float lookY,float clock) {
        if(mood==Alligator.DAZED_EYES) {
            float[] spiral=new float[50];
            for(int i=0;i<25;i++) {
                float u=i/24f,a=u*Softbody.TAU*1.65f+clock*2;
                spiral[i*2]=x+(float)Math.cos(a)*w*.12f*scale*u;
                spiral[i*2+1]=y+(float)Math.sin(a)*h*.28f*scale*u;
            }
            p.polyline(spiral,DARK,w*.025f);
            return;
        }
        if(mood==Alligator.CHEEKY_EYES) {
            p.arc(x,y+h*.09f,w*.10f*scale,h*.20f*scale,190,160,DARK,w*.028f);
            p.arc(x,y-h*.22f,w*.14f*scale,h*.25f*scale,208,110,DARK,w*.022f);
            return;
        }
        boolean tell=mood==Alligator.WINDUP_EYES;
        float px=x+w*(.012f+look*.075f),py=y+h*(.07f+lookY*.12f);
        p.fillEllipse(px,py,w*(tell?.075f:.10f)*scale,h*(tell?.27f:.34f)*scale,DARK);
        p.fillEllipse(px-w*.025f,py-h*.08f,w*.030f*scale,h*.09f*scale,0xFFF5FFED);
        float brow=tell?.35f:.25f;
        p.line(x-w*.10f,y-h*brow,x+w*.10f,y-h*(brow-.12f),DARK,w*.024f);
        if(tell)p.fillCircle(px+w*.033f,py+h*.12f,w*.025f,0xFFF9E3A1);
    }
    /** Local rotations share traced geometry on canvas and the pure-Java preview backend. */
    private static final class HeadTilt extends OpacityPainter {
        final float cx,cy,ox,oy,cos,sin,rotation;
        HeadTilt(Painter p,float x,float y,float angle) {this(p,x,y,angle,false);}
        HeadTilt(Painter p,float x,float y,float angle,boolean local) {
            super(p,1);cx=x;cy=y;ox=local?0:x;oy=local?0:y;rotation=angle;
            cos=(float)Math.cos(angle);sin=(float)Math.sin(angle);
        }
        float x(float x,float y) {return cx+(x-ox)*cos-(y-oy)*sin;}
        float y(float x,float y) {return cy+(x-ox)*sin+(y-oy)*cos;}
        public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float mix) {
            ArtStamp cache=p.stamps();
            return cache!=null && cache.stamp(art,x(x,y),y(x,y),w,h,angle+rotation,tint,mix);
        }
        float[] turn(float[] pts) {
            float[] out=new float[pts.length];
            for(int i=0;i<pts.length;i+=2){out[i]=x(pts[i],pts[i+1]);out[i+1]=y(pts[i],pts[i+1]);}
            return out;
        }
        public void fillPoly(float[] pts,int c) {p.fillPoly(turn(pts),c);}
        public void strokePoly(float[] pts,int c,float width) {p.strokePoly(turn(pts),c,width);}
        public void polyline(float[] pts,int c,float width) {p.polyline(turn(pts),c,width);}
        public void fillCircle(float x,float y,float r,int c) {p.fillCircle(x(x,y),y(x,y),r,c);}
        public void line(float x,float y,float a,float b,int c,float w) {p.line(x(x,y),y(x,y),x(a,b),y(a,b),c,w);}
        float[] ellipse(float x,float y,float rx,float ry,float start,float sweep,int n) {
            float[] out=new float[(n+1)*2];
            double angle=start*Math.PI/180,step=sweep*Math.PI/(180*n);
            double ca=Math.cos(angle),sa=Math.sin(angle),dc=Math.cos(step),ds=Math.sin(step);
            for(int i=0;i<=n;i++) {
                float px=x+(float)ca*rx,py=y+(float)sa*ry;
                out[i*2]=x(px,py);out[i*2+1]=y(px,py);
                double next=ca*dc-sa*ds;sa=sa*dc+ca*ds;ca=next;
            }
            return out;
        }
        // Tiny skin details need fewer segments than the broad face curves.
        int curveSteps(float rx,float ry,float sweep,int min,int max) {
            float radius=Math.max(Math.abs(rx),Math.abs(ry));
            if(radius>=12)return max;
            return Math.max(min,Math.min(max,(int)Math.ceil(6*Math.sqrt(radius)*Math.abs(sweep)/360)));
        }
        public void fillEllipse(float x,float y,float rx,float ry,int c) {
            p.fillPoly(ellipse(x,y,rx,ry,0,360,curveSteps(rx,ry,360,12,40)),c);
        }
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w) {
            p.polyline(ellipse(x,y,rx,ry,start,sweep,curveSteps(rx,ry,sweep,4,24)),c,w);
        }
    }
    static void portrait(Painter p,float x,float y,float r,float clock,boolean known,float fade) {
        Painter q=new OpacityPainter(p,fade);
        if(!known)q=new MonochromePainter(q,1f);
        q.fillEllipse(x,y+r*.33f,r*.57f,r*.60f,SKIN);
        q.fillEllipse(x,y+r*.41f,r*.40f,r*.46f,BELLY);
        for(int side=-1;side<=1;side+=2)limb(q,x+side*r*.43f,y+r*.18f,x+side*r*.78f,y+r*.46f,r*.13f,false,SKIN,clock);
        float hh=r*.44f*(1+.035f*(float)Math.sin(clock*2));
        head(q,null,x,y-r*.20f,r,hh,clock,false,0);
        if(!known)q.text("?",x,y+r*.25f,type(r*.55f),INK,Painter.CENTER,true);
    }
}
