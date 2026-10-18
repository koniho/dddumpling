package com.dddumpling.game;

/** Frosting, paws and pointed ears traced over moving spring outlines; no sprite textures. */
final class RainbowCatArt extends Draw {
    static final int[] COLORS={0xFFFF7B9B,0xFFFFB36B,0xFFFFE488,0xFF91E2BA,0xFF8ED9F5,0xFFBBA4EE};
    private static final float[] FACE={-1.02f,.02f,-.99f,-.28f,-.88f,-.48f,-.86f,-.98f,-.72f,-1.06f,
                -.34f,-.66f,0,-.60f,.34f,-.66f,.72f,-1.06f,.86f,-.98f,.88f,-.48f,
                .99f,-.28f,1.02f,.02f,.99f,.35f,.87f,.60f,.66f,.79f,.34f,.89f,
                0,.92f,-.34f,.89f,-.66f,.79f,-.87f,.60f,-.99f,.35f};
    static final int FUR=0xFFA9A7BE, INK=0xFF49425F;
    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        RainbowCat a=b.cat;if(!a.placed)return;
        float fade=alpha*(b.beaten?1-b.leaveProgress():1),enter=b.intro>0?b.introProgress():1;
        Painter q=new OpacityPainter(p,fade);
        motionTrail(q,a);
        q.save();q.translate((1-enter)*(1-enter)*-L.w,0);
        boolean caught=a.phase==RainbowCat.OPEN || (a.phase==RainbowCat.CATCH && a.netAge>=.34f);
        float struggle=caught?(float)Math.sin(a.clock*18)*a.r*.065f:0;
        if(a.phase==RainbowCat.CIRCLE)circleGuide(q,a);
        if(a.carries(c))net(q,c,L,false);
        Painter sprite=q;
        if(a.tapFlash>0) {
            int color=tapColor(a);float end=Math.min(1,a.tapFlash/.10f);
            float[] body=pastry(a.pastry,a.x,a.y,a.r);
            float[] face=points(FACE,a.head.centreX(),a.head.centreY(),a.head.radiusY()*1.13f,a.head.radiusY()*1.13f);
            for(int layer=3;layer>=1;layer--) {
                int glow=Glyph.withAlpha(color,(int)(end*(layer==1?180:45)));
                q.strokePoly(body,glow,a.r*layer*.19f);q.strokePoly(face,glow,a.r*layer*.19f);
            }
            sprite=new ColorFadePainter(q,color,.78f*end,1);
        }
        cat(sprite,a.x+struggle,a.y,a.r,a.clock*(caught?1:2),a.turn,a.pastry,a.head,caught,1,caught?0:a.scared);
        if(a.carries(c))net(q,c,L,true);
        if(a.flash>0)for(int band=0;band<3;band++)
            q.strokeCircle(a.x,a.y,a.r*(1.1f+(1-a.flash)*(1.5f+band*.3f)),
                    Glyph.withAlpha(COLORS[(a.taps+band)%COLORS.length],(int)(a.flash*170)),a.r*.12f);
        q.restore();
        if(a.phase==RainbowCat.OPEN && !b.beaten) {
            float s=L.enemyR*.72f,yy=a.y+a.r*3.5f;
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
    static int tapColor(RainbowCat a) {
        int band=Math.min(RainbowLand.CLEAR_COLORS.length-1,(int)((1-a.tapFlash/RainbowCat.TAP_FLASH)*RainbowLand.CLEAR_COLORS.length));
        return RainbowLand.CLEAR_COLORS[Math.max(0,band)];
    }
    private static void circleGuide(Painter p,RainbowCat a) {
        float radius=a.circleRadius(),pulse=.7f+.3f*(float)Math.sin(a.clock*3);
        for(int i=0;i<12;i++)p.arc(a.circleX(),a.circleY(),radius,radius,i*30,17,
                Glyph.withAlpha(COLORS[i%COLORS.length],(int)(100+65*pulse)),a.r*.085f);
        if(a.circling && Math.abs(a.circleSweep)>.01f) {
            float[] traced=new float[130];
            for(int i=0;i<=64;i++) {
                float at=a.gesture.start+a.gesture.travel*i/64;
                traced[i*2]=a.circleX()+a.gesture.coordinate(at,0)*radius;
                traced[i*2+1]=a.circleY()+a.gesture.coordinate(at,1)*radius;
            }
            p.polyline(traced,0xFFFFF2B6,a.r*.16f);
        }
        float angle=a.clock*1.7f;
        p.fillPoly(star(a.circleX()+(float)Math.cos(angle)*radius,a.circleY()+(float)Math.sin(angle)*radius,
                a.r*.20f,a.r*.08f,4,angle),0xFFFFF5CB);
    }
    private static void net(Painter p,GameCore c,Layout L,boolean front) {
        RainbowCat a=c.boss.cat;float r=a.r,u=a.netAmount();
        boolean winding=a.phase==RainbowCat.CIRCLE;
        float wind=winding?a.windUp:a.phase==RainbowCat.CATCH?a.releaseWind:1;
        float homeX=RunCompanion.x(L),homeY=RunCompanion.y(L);
        float targetX=a.catchX+r*2.45f,targetY=a.catchY+r*2.15f;
        float rise=winding?wind*.80f:a.phase==RainbowCat.CATCH?wind*.80f+(1-wind*.80f)*u:u;
        float cx=homeX+(targetX-homeX)*rise,cy=homeY+(targetY-homeY)*rise;
        float loadedX=homeX+(targetX-homeX)*wind*.80f-r*(.4f+.8f*wind);
        float loadedY=homeY+(targetY-homeY)*wind*.80f-r*(2.3f+.8f*wind);
        float nx,ny,tilt,size;
        if(winding) {
            nx=cx-r*(.4f+.8f*wind);ny=cy-r*(2.3f+.8f*wind);tilt=-.25f-.65f*wind;size=.45f+.45f*wind;
        } else {
            nx=loadedX+(a.catchX-loadedX)*u;
            ny=loadedY+(a.catchY-r*1.05f-loadedY)*u-r*.9f*(float)Math.sin(u*Math.PI);
            tilt=(-.25f-.65f*wind)*(1-u)-.13f*u;size=.45f+.55f*u;
        }
        float struggle=a.phase==RainbowCat.OPEN || (a.phase==RainbowCat.CATCH && a.netAge>=.34f)?1:0;
        float rx=r*2.15f*size,ry=r*.52f*size,depth=r*2.72f*size;
        float fade=winding?Math.min(1,wind*6):a.netReturn>0?u:1;
        Painter q=new OpacityPainter(p,fade);
        float[] grip=new float[2];netPoint(grip,0,1,0,nx,ny,rx,ry,depth,tilt,a.clock,0,true);
        if(!front) {
            // Deep hanging pouch and tilted hoop follow the fishing-net reference.
            float[] outline=new float[130];
            for(int i=0;i<=32;i++)netPoint(outline,i*2,-1,i/32f,nx,ny,rx,ry,depth,tilt,a.clock,struggle,true);
            for(int i=0;i<32;i++)netPoint(outline,(33+i)*2,1,1-i/31f,nx,ny,rx,ry,depth,tilt,a.clock,struggle,true);
            q.fillPoly(outline,0x208CCAE8);q.strokePoly(outline,0x996995BF,r*.045f);
            q.line(grip[0],grip[1],cx+r*.27f,cy+r*.30f,0xFF3F4578,r*.20f);
            q.line(grip[0],grip[1],cx+r*.27f,cy+r*.30f,0xFFD8EEFF,r*.12f);
            q.line(grip[0]-r*.025f,grip[1]-r*.025f,cx+r*.24f,cy+r*.28f,0xFFFFFFFF,r*.035f);
        }
        // Actual diagonal strands share a deformation field, so every mesh junction stays tied.
        for(int direction=-1;direction<=1;direction+=2)for(int i=-7;i<=7;i++) {
            float k=i*.25f,va=(k+direction-1)/(2*direction),vb=(k+direction+1)/(2*direction);
            float from=Math.max(0,Math.min(va,vb)),to=Math.min(1,Math.max(va,vb));
            if(to<=from)continue;
            float[] strand=new float[34];
            for(int j=0;j<=16;j++) {
                float v=from+(to-from)*j/16;
                netPoint(strand,j*2,k-direction*(2*v-1),v,nx,ny,rx,ry,depth,tilt,a.clock,struggle,front);
            }
            q.polyline(strand,front?0xBCDAF4FF:0x447DA6D6,r*(front?.038f:.025f));
        }
        float[] rim=new float[130];
        for(int i=0;i<=64;i++) {
            float angle=(front?0:(float)Math.PI)+i*(float)Math.PI/64;
            float px=(float)Math.cos(angle)*rx,py=(float)Math.sin(angle)*ry;
            rim[i*2]=nx+px*(float)Math.cos(tilt)-py*(float)Math.sin(tilt);
            rim[i*2+1]=ny+px*(float)Math.sin(tilt)+py*(float)Math.cos(tilt);
        }
        q.polyline(rim,0xFF343E77,r*.20f);q.polyline(rim,0xFF54CDED,r*.125f);
        q.polyline(rim,0xBBD4F9FF,r*.045f);
        if(!front)return;
        if(a.phase==RainbowCat.CATCH && a.netAge<.34f) {
            float burst=(float)Math.sin(u*Math.PI);
            for(int band=0;band<3;band++)q.arc(cx,cy,r*(1.5f+band*.30f),r*(1.5f+band*.30f),
                    185+u*80,70,fadeBy(COLORS[band+2],burst*.6f),r*.10f);
        }
        int who=c.companion.who;
        if(who>=0) {
            float cr=RunCompanion.radius(c,L)*(1+rise*.55f);
            float rock=winding?-.34f*wind:a.phase==RainbowCat.CATCH?.34f*(float)Math.sin(u*Math.PI):.08f*(float)Math.sin(a.clock*9);
            Trinket.drawReacting(p,who,cx,cy,cr,c.companion.clock,1,Trinket.FIGHT,rock,c.ninja());
            // Both hands hold the pole; reaction props would obscure the grip.
            int color=Collect.BODY[who];
            p.fillEllipse(cx-r*.05f,cy-r*.08f,cr*.20f,cr*.17f,color);
            p.fillEllipse(cx+r*.22f,cy+r*.15f,cr*.20f,cr*.17f,color);
            if(a.phase==RainbowCat.OPEN)TutorialSpeech.companionFeedback(p,L,cx,cy,cr,
                    a.companionHint(),.5f,Collect.ACCENT[who]);
        }
    }
    private static void netPoint(float[] out,int at,float u,float v,float nx,float ny,float rx,float ry,float depth,
            float tilt,float clock,float struggle,boolean front) {
        float cross=(float)Math.sqrt(Math.max(0,1-u*u)),width=(float)Math.sqrt(Math.max(0,1-v));
        float slack=(float)Math.sin(v*Math.PI)*(1-u*u);
        float px=u*rx*width+rx*slack*.12f*struggle*(float)Math.sin(clock*18+v*2);
        float py=depth*v+(front?1:-1)*ry*cross*(1-v)
                +ry*slack*.4f*struggle*(float)Math.sin(clock*15+u*3);
        out[at]=nx+px*(float)Math.cos(tilt)-py*(float)Math.sin(tilt);
        out[at+1]=ny+px*(float)Math.sin(tilt)+py*(float)Math.cos(tilt);
    }
    private static void motionTrail(Painter p,RainbowCat a) {
        int capacity=a.trailCount+1,n=1;
        float[] x=new float[capacity],y=new float[capacity],progress=new float[capacity];
        x[0]=a.x;y[0]=a.y;
        float length=0,limit=a.r*RainbowCat.TRAIL_LENGTH;
        for(int i=0;i<a.trailCount;i++) {
            float dx=a.trailX[i]-x[n-1],dy=a.trailY[i]-y[n-1];
            float d=(float)Math.hypot(dx,dy);if(d<.05f)continue;
            float step=Math.min(d,limit-length);
            x[n]=x[n-1]+dx*step/d;y[n]=y[n-1]+dy*step/d;length+=step;
            progress[n]=Math.max(length/limit,a.trailAge[i]/RainbowCat.TRAIL_TIME);n++;
            if(length>=limit)break;
        }
        if(n<2)return;
        float[] nx=new float[n],ny=new float[n];
        for(int i=0;i<n;i++) {
            int before=Math.max(0,i-1),after=Math.min(n-1,i+1);
            float dx=x[before]-x[after],dy=y[before]-y[after];
            float d=Math.max(.01f,(float)Math.hypot(dx,dy));nx[i]=-dy/d;ny[i]=dx/d;
        }
        // Shared vertex normals keep the ribbon joined through turns. Batch equal-opacity sections.
        for(int start=0;start<n-1;) {
            int bucket=(int)(RainbowCat.trailFade(progress[start+1])*16),end=start+1;
            while(end<n-1 && (int)(RainbowCat.trailFade(progress[end+1])*16)==bucket)end++;
            if(bucket>0)for(int band=0;band<COLORS.length;band++) {
                int count=end-start+1;float[] strip=new float[count*4];
                float low=(band-3)*a.r*.23f,high=low+a.r*.24f;
                for(int j=0;j<count;j++) {
                    int at=start+j,back=count*2-1-j;
                    strip[j*2]=x[at]+nx[at]*low;strip[j*2+1]=y[at]+ny[at]*low;
                    strip[back*2]=x[at]+nx[at]*high;strip[back*2+1]=y[at]+ny[at]*high;
                }
                p.fillPoly(strip,Glyph.withAlpha(COLORS[band],160*bucket/16));
            }
            start=end;
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
        cat(p,x,y,r,t,facing,body,skull,caught,known,0);
    }
    private static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known,float scared) {
        int crust=known>0?0xFFFFD49A:0xFF49425F,fur=known>0?FUR:0xFF49425F;
        float step=(float)Math.sin(t*(caught?15:7));
        // Static portraits retain their decorative ribbon; battle uses recorded motion.
        if(body==null)for(int band=0;band<COLORS.length;band++) {
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
        float hr=(skull==null?r*.66f:skull.radiusY())*1.13f;
        // Plush cheeks, small pointed ears and a wide round chin share one silhouette.

        float[] shape=points(FACE,hx,hy,hr,hr);
        p.fillPoly(shape,INK);p.fillPoly(inset(shape,hx,hy,.93f,.92f),fur);
        p.fillPoly(inset(shape,hx-hr*.08f,hy-hr*.1f,.80f,.72f),known>0?0xFFC4C2D3:fur);
        if(known<=0)return;
        p.fillEllipse(hx,hy+hr*.33f,hr*.49f,hr*.32f,0x55FFF6EE);
        for(int side=-1;side<=1;side+=2) {
            p.fillPoly(points(new float[]{side*.45f,-.62f,side*.73f,-.92f,side*.77f,-.43f},hx,hy,hr,hr),0xFFFFB4CB);
            float ex=hx+side*hr*.40f,ey=hy+hr*.02f;
            boolean blink=scared==0 && Math.sin(t*1.7f)>.985;
            if(caught) {
                float squeeze=.05f*(float)Math.sin(t*18);
                p.polyline(points(new float[]{side*.62f,-.08f-squeeze,side*.28f,.04f,side*.61f,.17f+squeeze},
                        hx,hy,hr,hr),INK,hr*.075f);
                p.polyline(points(new float[]{side*.27f,-.28f,side*.43f,-.34f,side*.62f,-.30f},hx,hy,hr,hr),INK,hr*.055f);
            } else {
                p.fillEllipse(ex,ey,hr*(.18f+.045f*scared),hr*(blink?.045f:.235f+.025f*scared),INK);
                if(scared>0) {
                    p.fillEllipse(ex,ey,hr*.18f*scared,hr*.215f*scared,0xFFFFFAFF);
                    p.fillEllipse(ex,ey-hr*.025f,hr*(.13f-.055f*scared),hr*(.18f-.06f*scared),INK);
                    p.polyline(points(new float[]{side*.21f,-.35f,side*.40f,-.43f,side*.62f,-.32f},hx,hy,hr,hr),
                            fadeBy(INK,scared),hr*.055f);
                }
                if(!blink && scared<.5f) {
                    p.fillEllipse(ex,ey+hr*.09f,hr*.12f,hr*.10f,0xFF7668A1);
                    p.fillCircle(ex-hr*.055f,ey-hr*.08f,hr*.075f,0xFFFFFAFF);
                    p.fillCircle(ex+hr*.065f,ey+hr*.08f,hr*.034f,0xFFFFFAFF);
                }
            }
            p.fillEllipse(hx+side*hr*.71f,hy+hr*.31f,hr*.19f,hr*.105f,0xAFFF83BD);
            p.line(hx+side*hr*.71f,hy+hr*.38f,hx+side*hr*.93f,hy+hr*.35f,0x88766891,hr*.035f);
        }
        p.fillEllipse(hx,hy+hr*.23f,hr*.095f,hr*.065f,0xFFBE7FA7);
        if(caught) {
            p.fillEllipse(hx,hy+hr*.48f,hr*.13f,hr*(.12f+.035f*(float)Math.sin(t*18)),INK);
            p.fillEllipse(hx,hy+hr*.54f,hr*.085f,hr*.045f,0xFFFFB4CB);
            p.fillEllipse(hx+hr*.89f,hy-hr*.25f,hr*.07f,hr*.13f,0xBBBEF6FF);
        } else if(scared>0) {
            p.fillEllipse(hx,hy+hr*.45f,hr*(.08f+.03f*scared),hr*(.08f+.09f*scared),INK);
            p.fillEllipse(hx+hr*.91f,hy-hr*.25f,hr*.06f*scared,hr*.13f*scared,0xCCB6F5FF);
        } else p.polyline(points(new float[]{-.25f,.34f,-.19f,.43f,-.10f,.45f,0,.34f,.10f,.45f,.19f,.43f,.25f,.34f},hx,hy,hr,hr),INK,hr*.055f);
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
