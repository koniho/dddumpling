package com.dddumpling.game;

/** Frosting, paws and pointed ears traced over moving spring outlines; no sprite textures. */
final class RainbowCatArt extends Draw {
    static final int[] COLORS={0xFFFF7B9B,0xFFFFB36B,0xFFFFE488,0xFF91E2BA,0xFF8ED9F5,0xFFBBA4EE};
    static final int FUR=0xFFA9A7BE, INK=0xFF49425F;
    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        RainbowCat a=b.cat;if(!a.placed)return;
        float fade=alpha*(b.beaten?1-b.leaveProgress():1),enter=b.intro>0?b.introProgress():1;
        Painter q=new OpacityPainter(p,fade);
        motionTrail(q,a);
        q.save();q.translate((1-enter)*(1-enter)*-L.w,0);
        boolean caught=a.phase==RainbowCat.OPEN;
        float struggle=caught?(float)Math.sin(a.clock*18)*a.r*.065f:0;
        if(a.phase==RainbowCat.CIRCLE)circleGuide(q,a);
        if(a.carries(c))net(q,c,L,false);
        cat(q,a.x+struggle,a.y,a.r,a.clock*(caught?1:2),a.turn,a.pastry,a.head,caught,1);
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
    private static void circleGuide(Painter p,RainbowCat a) {
        float radius=a.circleRadius(),pulse=.7f+.3f*(float)Math.sin(a.clock*3);
        for(int i=0;i<12;i++)p.arc(a.circleX(),a.circleY(),radius,radius,i*30,17,
                Glyph.withAlpha(COLORS[i%COLORS.length],(int)(100+65*pulse)),a.r*.085f);
        if(a.circling && Math.abs(a.circleSweep)>.01f) {
            float start=(a.circleAngle-a.circleSweep)*180/(float)Math.PI;
            p.arc(a.circleX(),a.circleY(),radius,radius,start,a.circleSweep*180/(float)Math.PI,
                    0xFFFFF2B6,a.r*.16f);
        }
        float angle=a.clock*1.7f;
        p.fillPoly(star(a.circleX()+(float)Math.cos(angle)*radius,a.circleY()+(float)Math.sin(angle)*radius,
                a.r*.20f,a.r*.08f,4,angle),0xFFFFF5CB);
    }
    private static void net(Painter p,GameCore c,Layout L,boolean front) {
        RainbowCat a=c.boss.cat;float u=a.netAmount();
        float homeX=RunCompanion.x(L),homeY=RunCompanion.y(L),r=a.r;
        float targetX=a.catchX+r*2.45f,targetY=a.catchY+r*2.15f;
        float cx=homeX+(targetX-homeX)*u,cy=homeY+(targetY-homeY)*u-(float)Math.sin(u*Math.PI)*r*1.2f;
        float shake=a.phase==RainbowCat.OPEN?(float)Math.sin(a.clock*18)*r*.045f:0;
        float nx=homeX+(a.catchX-homeX)*u+shake,ny=homeY-r*2+(a.catchY-homeY+r*2)*u;
        float rx=r*(.55f+1.45f*u),ry=r*(.4f+1.12f*u),fade=Math.min(1,u*3);
        Painter q=new OpacityPainter(p,fade);
        if(!front) {
            q.fillEllipse(nx,ny,rx,ry,0x304BE6CF);
            return;
        }
        q.line(nx+rx*.60f,ny+ry*.80f,cx,cy,0xFF7660A0,r*.21f);
        q.line(nx+rx*.60f,ny+ry*.80f,cx,cy,0xFFFFD89F,r*.14f);
        q.line(nx+rx*.60f-r*.03f,ny+ry*.80f,cx-r*.03f,cy,0xFFFFF1CD,r*.045f);
        // Diagonal mesh clipped analytically to the oval, leaving the cat visible through it.
        for(int direction=-1;direction<=1;direction+=2)for(int i=-5;i<=5;i++) {
            float k=i*.24f,root=(float)Math.sqrt(2-k*k);
            float x0=(direction*k-root)*.5f,x1=(direction*k+root)*.5f;
            float[] strand=new float[26];
            for(int sample=0;sample<=12;sample++) {
                float px=x0+(x1-x0)*sample/12f,py=k-direction*px;
                float slack=Math.max(0,1-px*px-py*py);
                float struggle=a.phase==RainbowCat.OPEN?1:0;
                // One deformation field keeps crossing threads joined while the belly flexes.
                float tug=(float)Math.sin(a.clock*18+py*2)*.13f*struggle;
                strand[sample*2]=nx+rx*(px+slack*tug);
                strand[sample*2+1]=ny+ry*(py+slack*(.14f+.10f*struggle*(float)Math.sin(a.clock*15+px*3)));
            }
            q.polyline(strand,0x99E6FFF7,r*.032f);
        }
        q.arc(nx,ny,rx,ry,0,360,0xFF486D9B,r*.15f);
        q.arc(nx,ny,rx,ry,0,360,0xFFA7F7DD,r*.105f);
        q.arc(nx,ny,rx,ry,205,115,0xFFEEFFF7,r*.045f);
        int who=c.companion.who;
        if(who>=0) {
            float cr=RunCompanion.radius(c,L)*(1+u*.55f);
            Trinket.drawReacting(p,who,cx,cy,cr,c.companion.clock,1,Trinket.FIGHT,
                    .13f*(float)Math.sin(a.clock*9)*u,c.ninja());
            RunCompanion.drawProps(p,c,L,cx,cy,cr);
            if(a.phase==RainbowCat.OPEN)TutorialSpeech.companionFeedback(p,L,cx,cy,cr,
                    a.companionHint(),.5f,Collect.ACCENT[who]);
        }
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
        float[] face={-1.02f,.02f,-.99f,-.28f,-.88f,-.48f,-.86f,-.98f,-.72f,-1.06f,
                -.34f,-.66f,0,-.60f,.34f,-.66f,.72f,-1.06f,.86f,-.98f,.88f,-.48f,
                .99f,-.28f,1.02f,.02f,.99f,.35f,.87f,.60f,.66f,.79f,.34f,.89f,
                0,.92f,-.34f,.89f,-.66f,.79f,-.87f,.60f,-.99f,.35f};
        float[] shape=points(face,hx,hy,hr,hr);
        p.fillPoly(shape,INK);p.fillPoly(inset(shape,hx,hy,.93f,.92f),fur);
        p.fillPoly(inset(shape,hx-hr*.08f,hy-hr*.1f,.80f,.72f),known>0?0xFFC4C2D3:fur);
        if(known<=0)return;
        p.fillEllipse(hx,hy+hr*.33f,hr*.49f,hr*.32f,0x55FFF6EE);
        for(int side=-1;side<=1;side+=2) {
            p.fillPoly(points(new float[]{side*.45f,-.62f,side*.73f,-.92f,side*.77f,-.43f},hx,hy,hr,hr),0xFFFFB4CB);
            float ex=hx+side*hr*.40f,ey=hy+hr*.02f;
            boolean blink=Math.sin(t*1.7f)>.985;
            if(caught) {
                float squeeze=.05f*(float)Math.sin(t*18);
                p.polyline(points(new float[]{side*.62f,-.08f-squeeze,side*.28f,.04f,side*.61f,.17f+squeeze},
                        hx,hy,hr,hr),INK,hr*.075f);
                p.polyline(points(new float[]{side*.27f,-.28f,side*.43f,-.34f,side*.62f,-.30f},hx,hy,hr,hr),INK,hr*.055f);
            } else {
                p.fillEllipse(ex,ey,hr*.18f,hr*(blink?.045f:.235f),INK);
                if(!blink) {
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
