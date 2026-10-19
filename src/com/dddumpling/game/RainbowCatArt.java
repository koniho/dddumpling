package com.dddumpling.game;

/** Frosting, paws and pointed ears traced over moving spring outlines; no sprite textures. */
final class RainbowCatArt extends Draw {
    static final int[] COLORS={0xFFFF7B9B,0xFFFFB36B,0xFFFFE488,0xFF91E2BA,0xFF8ED9F5,0xFFBBA4EE};
    private static final int[] HALLOWEEN_TRAIL={0xFFA86BE4,0xFFCD9AFF,0xFFFF974B,0xFFFFCB79,0xFF9DDC62,0xFFD2F69A};
    static int trailColor(Painter p,int band,int normal) {return Skins.halloween(p)?HALLOWEEN_TRAIL[band%HALLOWEEN_TRAIL.length]:normal;}
    private static final float[] FACE={-1.02f,.02f,-.99f,-.28f,-.88f,-.48f,-.86f,-.98f,-.72f,-1.06f,
                -.34f,-.66f,0,-.60f,.34f,-.66f,.72f,-1.06f,.86f,-.98f,.88f,-.48f,
                .99f,-.28f,1.02f,.02f,.99f,.35f,.87f,.60f,.66f,.79f,.34f,.89f,
                0,.92f,-.34f,.89f,-.66f,.79f,-.87f,.60f,-.99f,.35f};
    static final int FUR=0xFFA9A7BE, INK=0xFF49425F;
    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        RainbowCat a=b.cat;if(!a.placed)return;
        if(a.rocket.active) {a.rocket.draw(new OpacityPainter(p,alpha),c,L);return;}
        float fade=alpha*(b.beaten?1-b.leaveProgress():1);
        Painter q=new OpacityPainter(p,fade);
        RainbowCatArrival.stars(q,a,b,L);
        if(b.intro>0) {RainbowCatArrival.draw(q,a,b,L);return;}
        motionTrail(q,a);
        q.save();
        secondTapRays(q,a,L);
        shapeRings(q,a);
        escapeBurst(q,a);
        hitCones(q,a,L);
        boolean caught=a.phase==RainbowCat.OPEN || (a.phase==RainbowCat.CATCH && a.netAge>=.34f);
        float struggle=caught?a.struggleX():a.shakeOffset();
        if(a.carries(c))net(q,c,L,false);
        Painter sprite=q;
        if(a.tapFlash>0) {
            int color=tapColor(a);float end=Math.min(1,a.tapFlash/.10f);
            float[] body=pastry(a.pastry,a.x,a.y,a.r);
            float[] face=face(p,a.head.centreX(),a.head.centreY(),a.head.radiusY()*1.13f);
            for(int layer=3;layer>=1;layer--) {
                int glow=Glyph.withAlpha(color,(int)(end*(layer==1?180:45)));
                q.strokePoly(body,glow,a.r*layer*.19f);q.strokePoly(face,glow,a.r*layer*.19f);
            }
            sprite=new ColorFadePainter(q,color,.78f*end,1);
        }
        if(a.keyFlash>0)sprite=new ColorFadePainter(sprite,Glyph.mix(a.hitColor,0xFFFFFFFF,.8f),.90f*a.keyFlash/.22f,1);
        sprite.save();
        // The cat rides inside the same swinging pouch as the mesh, including its spring shapes.
        float pouch=a.pouchSway()*.43f;
        sprite.translate(a.hoopShift()+a.r*1.17f*(float)Math.sin(pouch),
                a.r*1.17f*((float)Math.cos(pouch)-1));
        cat(sprite,a.x+a.traceX+struggle,a.y+a.traceY+a.struggleY(),a.r,a.clock*(caught?1:2),a.turn,a.pastry,a.head,caught,1,caught?0:a.scared,a.angry(),0,a.shotWind,a.shotReach);
        sprite.restore();
        if(a.phase==RainbowCat.CIRCLE)circleGuide(q,a);
        if(a.carries(c))net(q,c,L,true);
        if(a.flash>0)for(int band=0;band<3;band++)
            q.strokeCircle(a.x,a.y,a.r*(1.1f+(1-a.flash)*(1.5f+band*.3f)),
                    Glyph.withAlpha(COLORS[(a.taps+band)%COLORS.length],(int)(a.flash*170)),a.r*.12f);
        q.restore();
        if(a.phase==RainbowCat.OPEN && !b.beaten) {
            float s=L.enemyR*1.05f,yy=a.promptY();
            for(int side=0;side<2;side++) {
                int g=side==0?a.leftKey:a.rightKey;boolean active=g==a.promptKey;
                float xx=a.promptX(L,g);
                float pulse=active?1+.10f*a.keyFlash/.22f:1;
                if(active)FastEnemies.glow(q,xx,yy,s*1.1f,Skins.keyColor(p,g),220,1);
                FastEnemies.body(q,xx,yy,s*pulse,Skins.keyColor(p,g),active?255:190,false);
                Kawaii.draw(q,g,xx,yy,s*.70f*pulse,Skins.keyColor(p,g),active?1:.78f,0);
                if(active)q.strokeCircle(xx,yy,s*1.35f,0xFFFFF1BD,s*.12f);
            }
            float fraction=Math.max(0,a.timer/a.promptTime()),width=L.w*.60f;
            for(int band=0;band<COLORS.length;band++) {
                float bandX=L.w*.5f-width*.5f+width*band/COLORS.length;
                float fill=Math.max(0,Math.min(1,fraction*COLORS.length-band));
                if(fill>0)q.line(bandX,yy+s*1.8f,bandX+width/COLORS.length*fill,yy+s*1.8f,COLORS[band],s*.16f);
            }
        }
    }
    static void hitCones(Painter p,RainbowCat a,Layout L) {
        float sway=a.pouchSway()*.43f;
        float tx=a.x+a.traceX+a.struggleX()+a.hoopShift()+a.r*1.17f*(float)Math.sin(sway);
        float ty=a.y+a.traceY+a.struggleY()+a.r*1.17f*((float)Math.cos(sway)-1);
        for(int i=0;i<a.hitConeLife.length;i++) {
            if(a.hitConeLife[i]<=0)continue;
            float age=1-a.hitConeLife[i]/RainbowCat.HIT_CONE_TIME;
            float fade=(1-age)*(1-age),reach=Math.min(1,.35f+age*5);
            float sx=a.hitConeX[i],sy=a.hitConeY[i],dx=tx-sx,dy=ty-sy;
            float len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1)continue;
            float nx=-dy/len,ny=dx/len,width=a.r*(.72f+age*.28f);
            int color=Skins.keyColor(p,a.hitConeKey[i]),light=Glyph.mix(color,0xFFFFFFFF,.72f);
            // Small reusable filled quads: no blur, dynamic strokes or per-frame allocations.
            for(int layer=0;layer<3;layer++) {
                float scale=1-layer*.27f;
                coneSlice(p,a.hitConeWork,sx,sy,dx,dy,nx,ny,0,reach,L.enemyR*.10f,width*scale,
                        Glyph.withAlpha(layer==2?light:color,(int)((layer==0?35:layer==1?50:65)*fade)));
            }
            for(int band=0;band<3;band++) {
                float along=(age*1.8f+band*.31f)%1;
                if(along>reach)continue;
                coneSlice(p,a.hitConeWork,sx,sy,dx,dy,nx,ny,Math.max(0,along-.045f),along,L.enemyR*.10f,width*.86f,
                        Glyph.withAlpha(light,(int)(155*fade*(.5f+along*.5f))));
            }
            for(int spark=0;spark<7;spark++) {
                float along=(age*1.9f+spark*.137f)%1;if(along>reach)continue;
                float cross=(float)Math.sin(spark*2.4f)*width*along*.85f;
                float xx=sx+dx*along+nx*cross,yy=sy+dy*along+ny*cross;
                float size=L.enemyR*(.045f+.065f*(float)Math.pow(Math.sin(age*15+spark),2));
                int glint=Glyph.withAlpha(light,(int)(235*fade));
                p.line(xx-size,yy,xx+size,yy,glint,size*.65f);
                p.line(xx,yy-size*1.6f,xx,yy+size*1.6f,glint,size*.65f);
            }
        }
    }
    private static void coneSlice(Painter p,float[] xy,float x,float y,float dx,float dy,float nx,float ny,
                                  float from,float to,float startWidth,float endWidth,int color) {
        float lo=startWidth+(endWidth-startWidth)*from,hi=startWidth+(endWidth-startWidth)*to;
        xy[0]=x+dx*from-nx*lo;xy[1]=y+dy*from-ny*lo;
        xy[2]=x+dx*to-nx*hi;xy[3]=y+dy*to-ny*hi;
        xy[4]=x+dx*to+nx*hi;xy[5]=y+dy*to+ny*hi;
        xy[6]=x+dx*from+nx*lo;xy[7]=y+dy*from+ny*lo;
        p.fillPoly(xy,color);
    }
    private static void escapeBurst(Painter p,RainbowCat a) {
        if(a.escapeLeft<=0)return;
        float age=RainbowCat.ESCAPE_TIME-a.escapeLeft;
        for(int ring=0;ring<6;ring++) {
            float u=(age-ring*.035f)/.65f;if(u<0 || u>=1)continue;
            int color=RainbowLand.CLEAR_COLORS[ring];float fade=(1-u)*(1-u);
            float radius=a.r*(.5f+7*(1-(1-u)*(1-u)));
            p.strokeCircle(a.escapeX,a.escapeY,radius,Glyph.withAlpha(color,(int)(75*fade)),a.r*.55f);
            p.strokeCircle(a.escapeX,a.escapeY,radius,Glyph.withAlpha(color,(int)(240*fade)),a.r*.24f);
        }
        for(int i=0;i<42;i++) {
            float u=age/(.55f+(i%5)*.1f);if(u>=1)continue;
            float angle=i*2.399963f,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float distance=a.r*(.6f+(3+i%7)*u),xx=a.escapeX+dx*distance,yy=a.escapeY+dy*distance+a.r*u*u;
            int color=RainbowLand.CLEAR_COLORS[i%6],alpha=(int)(255*(1-u));
            p.line(xx-dx*a.r*.65f*(1-u),yy-dy*a.r*.65f*(1-u),xx,yy,Glyph.withAlpha(color,alpha/2),a.r*.13f);
            p.fillCircle(xx,yy,a.r*.24f*(1-u),Glyph.withAlpha(color,alpha/4));
            p.fillPoly(star(xx,yy,a.r*(.12f+i%3*.055f)*(1-u),a.r*.045f*(1-u),4,angle+u*5),Glyph.withAlpha(color,alpha));
        }
        if(age<.18f)p.fillCircle(a.escapeX,a.escapeY,a.r*(1+age*9),Glyph.withAlpha(0xFFFFFFFF,(int)(210*(1-age/.18f))));
    }
    private static void secondTapRays(Painter p,RainbowCat a,Layout L) {
        if(a.taps!=2 || a.tapFlash<=0)return;
        float age=1-a.tapFlash/RainbowCat.TAP_FLASH;
        float grow=(float)Math.sin(Math.min(1,age*3)*Math.PI*.5);
        float fade=Math.min(1,(1-age)*2.5f),reach=Math.max(L.w,L.h)*.72f*grow;
        for(int i=0;i<18;i++) {
            float angle=Softbody.TAU*i/18+age*.22f,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float near=a.r*.6f,far=reach*(.72f+.28f*(float)Math.sin(i*2.4f)* (float)Math.sin(i*2.4f));
            float width=a.r*(.19f+.13f*(i%3));
            int color=RainbowLand.CLEAR_COLORS[i%RainbowLand.CLEAR_COLORS.length];
            p.fillPoly(new float[]{a.x+dx*near,a.y+dy*near,
                    a.x+dx*far-dy*width,a.y+dy*far+dx*width,
                    a.x+dx*(far+a.r),a.y+dy*(far+a.r),
                    a.x+dx*far+dy*width,a.y+dy*far-dx*width},Glyph.withAlpha(color,(int)(155*fade)));
            p.line(a.x+dx*a.r,a.y+dy*a.r,a.x+dx*far,a.y+dy*far,
                    Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.7f),(int)(190*fade)),a.r*.055f);
        }
    }
    static void shapeRings(Painter p,RainbowCat a) {
        if(a.shapeBurst<=0 || a.burstShape==null)return;
        float age=1.25f-a.shapeBurst;
        for(int ring=0;ring<6;ring++) {
            float u=(age-ring*.065f)/.92f;if(u<0 || u>=1)continue;
            float radius=a.burstRadius*(.65f+3.3f*(1-(1-u)*(1-u)));
            float fade=(1-u)*(1-u)*Math.min(1,u/.05f);
            if(fade<.015f)continue;
            int color=RainbowLand.CLEAR_COLORS[ring%RainbowLand.CLEAR_COLORS.length];
            int rim=Glyph.withAlpha(color,(int)(235*fade));
            int light=Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.65f),(int)(155*fade));
            if(a.burstKind==0) {
                p.strokeCircle(a.burstX,a.burstY,radius,rim,a.r*.19f);
                p.strokeCircle(a.burstX,a.burstY,radius,light,a.r*.055f);
            } else {
                // Reuse geometry and avoid large dynamically stroked Paths on Android.
                for(int i=0;i<a.burstWork.length;i+=2) {
                    a.burstWork[i]=a.burstX+a.burstShape[i*a.burstStep]*radius;
                    a.burstWork[i+1]=a.burstY+a.burstShape[i*a.burstStep+1]*radius;
                }
                p.fastPolyline(a.burstWork,rim,a.r*.19f);
                p.fastPolyline(a.burstWork,light,a.r*.055f);
            }
        }
    }
    static int tapColor(RainbowCat a) {
        int band=Math.min(RainbowLand.CLEAR_COLORS.length-1,(int)((1-a.tapFlash/RainbowCat.TAP_FLASH)*RainbowLand.CLEAR_COLORS.length));
        return RainbowLand.CLEAR_COLORS[Math.max(0,band)];
    }
    private static int traceColor(RainbowCat a,int band) {
        return a.traceLost>0?0xFFFF3864:RainbowLand.CLEAR_COLORS[Math.floorMod(band,RainbowLand.CLEAR_COLORS.length)];
    }
    private static void circleGuide(Painter p,RainbowCat a) {
        float radius=a.circleRadius(),pulse=.7f+.3f*(float)Math.sin(a.clock*3);
        if(a.captures>0) {
            float[] points=TraceGesture.shape(a.captures);int edges=points.length/2-1,step=(edges+5)/6;
            for(int first=0;first<edges;first+=step) {
                int last=Math.min(edges,first+step);float[] band=new float[(last-first+1)*2];
                for(int i=first;i<=last;i++) {band[(i-first)*2]=a.x+points[i*2]*radius;band[(i-first)*2+1]=a.y+points[i*2+1]*radius;}
                p.polyline(band,Glyph.withAlpha(traceColor(a,first*COLORS.length/edges),(int)(215+40*pulse)),a.r*.19f);
            }
        } else for(int i=0;i<12;i++)p.arc(a.circleX(),a.circleY(),radius,radius,i*30,17,
                Glyph.withAlpha(traceColor(a,i),(int)(205+50*pulse)),a.r*.16f);
        if(a.starShape()) {
            float edge=a.gesture.length/5;
            int next=!a.circling?0:a.gesture.travel<0?(int)Math.ceil(a.gesture.position/edge)-1:(int)Math.floor(a.gesture.position/edge)+1;
            float tx=a.x+a.gesture.coordinate(next*edge,0)*radius,ty=a.y+a.gesture.coordinate(next*edge,1)*radius;
            p.fillCircle(tx,ty,a.r*(.24f+.05f*pulse),Glyph.withAlpha(0xFFFFE6A3,(int)(65+45*pulse)));
            p.fillCircle(tx,ty,a.r*.10f,0xFFFFF5CB);
        }
        tracedPath(p,a,a.circleX(),a.circleY(),radius,a.r);
        float angle=a.clock*1.7f;
        p.fillPoly(star(a.circleX()+a.gesture.coordinate(angle,0)*radius,a.circleY()+a.gesture.coordinate(angle,1)*radius,
                a.r*.20f,a.r*.08f,4,angle),0xFFFFF5CB);
    }
    private static void tracedPath(Painter p,RainbowCat a,float x,float y,float radius,float stroke) {
        boolean lost=a.traceLost>0;
        float travel=lost?a.lostTravel:a.gesture.travel,start=lost?a.lostStart:a.gesture.start;
        if((!a.circling && !lost) || Math.abs(travel)<.01f)return;
        float fade=lost?Math.min(1,a.traceLost/.25f):1;
        float beat=.5f+.5f*(float)Math.sin(a.clock*(lost?24:7));
        for(int section=0;section<8;section++) {
            float[] band=new float[18];
            for(int i=0;i<=8;i++) {
                float at=start+travel*(section*8+i)/64;
                band[i*2]=x+a.gesture.coordinate(at,0)*radius;
                band[i*2+1]=y+a.gesture.coordinate(at,1)*radius;
            }
            int color=traceColor(a,section);
            p.polyline(band,Glyph.withAlpha(color,(int)((45+30*beat)*fade)),stroke*(.67f+.12f*beat));
            p.polyline(band,Glyph.withAlpha(color,(int)(145*fade)),stroke*.40f);
            p.polyline(band,Glyph.withAlpha(color,(int)(250*fade)),stroke*.22f);
            float light=.5f+.5f*(float)Math.sin(section*.9f-a.clock*9);
            p.polyline(band,Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.65f),(int)((115+110*light)*fade)),stroke*.075f);
        }
        // Seeded phases make twinkles drift off the completed stroke without consuming gameplay RNG.
        for(int i=0;i<24;i++) {
            float age=(a.clock*1.7f+i*.618034f)%1;
            float at=start+travel*(i+.5f)/24,angle=i*2.39996f;
            float xx=x+a.gesture.coordinate(at,0)*radius+(float)Math.cos(angle)*stroke*age*.85f;
            float yy=y+a.gesture.coordinate(at,1)*radius-stroke*age*(.4f+(i%3)*.25f);
            float size=stroke*(.05f+.07f*(1-age));int alpha=(int)(255*(1-age)*fade);
            int color=traceColor(a,i);
            p.fillCircle(xx,yy,size*2.6f,Glyph.withAlpha(color,alpha/4));
            p.fillPoly(star(xx,yy,size,size*.28f,4,angle+a.clock),Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.4f),alpha));
        }
    }
    private static void net(Painter p,GameCore c,Layout L,boolean front) {net(p,c,L,front,true);}
    static void net(Painter p,GameCore c,Layout L,boolean front,boolean holder) {
        RainbowCat a=c.boss.cat;float r=a.r,u=a.escapeLeft>0?1:a.netAmount();
        float escape=a.escapeLeft>0?1-a.escapeLeft/RainbowCat.ESCAPE_TIME:0;
        boolean winding=a.phase==RainbowCat.CIRCLE;
        float wind=winding?a.windUp:a.phase==RainbowCat.CATCH?a.releaseWind:1;
        float homeX=RunCompanion.x(L),homeY=RunCompanion.y(L);
        float targetX=a.catchX+r*2.45f,targetY=a.catchY+r*2.15f;
        float rise=winding?wind*.80f:a.phase==RainbowCat.CATCH?wind*.80f+(1-wind*.80f)*u:u;
        float cx=homeX+(targetX-homeX)*rise,cy=homeY+(targetY-homeY)*rise;
        if(a.escapeLeft>0) {
            float kick=1-(float)Math.pow(1-Math.min(1,escape/.18f),3);
            float back=RainbowCat.clamp((escape-.22f)/.78f,0,1);back=back*back*(3-2*back);
            float kickedX=Math.min(L.w-r*.55f,targetX+r*.85f*kick),kickedY=targetY+r*1.65f*kick;
            cx=kickedX+(homeX-kickedX)*back;
            cy=kickedY+(homeY-kickedY)*back-r*.45f*(float)Math.sin(back*Math.PI*2)*(1-back);
            rise=1-back;
        }
        // Repeat the player's shape in a small stage above the companion's head.
        float mimicRadius=r*.92f;
        float loadedCompanionX=homeX+(targetX-homeX)*wind*.80f;
        float loadedCompanionY=homeY+(targetY-homeY)*wind*.80f;
        float mimicX=loadedCompanionX-r*.45f,mimicY=loadedCompanionY-r*2.6f;
        float loadedX=mimicX+a.netAimX*mimicRadius;
        float loadedY=mimicY+a.netAimY*mimicRadius;
        float waveClock=a.phase==RainbowCat.CATCH?a.clock-a.netAge:a.clock;
        float waveTilt=-.25f+a.netAimX*.55f+.16f*(float)Math.sin(waveClock*8);
        float nx,ny,tilt,size;
        if(winding) {
            nx=loadedX;ny=loadedY;tilt=waveTilt;size=.32f;
        } else {
            nx=loadedX+(a.catchX-loadedX)*u;
            ny=loadedY+(a.catchY-r*1.05f-loadedY)*u-r*.9f*(float)Math.sin(u*Math.PI);
            tilt=waveTilt*(1-u)-.13f*u;size=.32f+.68f*u;
            if(a.phase==RainbowCat.CATCH) {
                // Scoop around the cat's left side, turning the entire hoop and pouch through the swing.
                float sweep=(float)Math.sin(u*Math.PI);
                nx-=r*1.7f*sweep;ny-=r*.45f*sweep;tilt-=1.8f*sweep;
                float settle=RainbowCat.clamp((a.netAge-.34f)/(RainbowCat.NET_TIME-.34f),0,1);
                tilt+=.16f*(float)Math.sin(settle*Softbody.TAU*1.2f)*(1-settle);
            }
        }
        if(a.escapeLeft>0) {
            float snap=1-(float)Math.pow(1-Math.min(1,escape/.24f),3);
            nx+=(cx-targetX)*.85f;ny+=(cy-targetY)*.85f;
            tilt+=snap*1.25f;size*=1-.20f*snap;
        }
        float struggle=a.caughtStrength();
        nx+=a.hoopShift();
        cx-=a.hoopShift()*.45f;cy+=r*.04f*struggle*(float)Math.cos(a.clock*12);
        float rx=r*2.15f*size,ry=r*.52f*size,depth=r*2.72f*size;
        // Rock the hoop in depth as it is waved; the loose mesh lags behind the frame.
        if(winding)ry*=.95f+.4f*(float)Math.sin(a.clock*8+a.netAimY);
        float sway=winding?-.28f*a.netAimX-.22f*(float)Math.sin(a.clock*8-.7f):a.pouchSway();
        if(a.phase==RainbowCat.CATCH) {
            if(a.netAge<.34f)sway=-.42f*(float)Math.sin(u*Softbody.TAU)*(1-u*.25f);
            else {
                float settle=RainbowCat.clamp((a.netAge-.34f)/(RainbowCat.NET_TIME-.34f),0,1);
                sway+=-.22f*(float)Math.sin(settle*Softbody.TAU*1.5f)*(1-settle)*(1-settle);
            }
        }
        float fade=a.escapeLeft>0?RainbowCat.clamp(1-escape/.82f,0,1):winding?Math.min(1,wind*6):a.netReturn>0?u:1;
        Painter q=new OpacityPainter(p,fade);
        float[] grip=new float[2];netPoint(grip,0,1,0,nx,ny,rx,ry,depth,tilt,a.clock,0,sway,true);
        if(!front) {
            if(winding)tracedPath(q,a,mimicX,mimicY,mimicRadius,r*.55f);
            // Deep hanging pouch and tilted hoop follow the fishing-net reference.
            float[] outline=new float[130];
            for(int i=0;i<=32;i++)netPoint(outline,i*2,-1,i/32f,nx,ny,rx,ry,depth,tilt,a.clock,struggle,sway,true);
            for(int i=0;i<32;i++)netPoint(outline,(33+i)*2,1,1-i/31f,nx,ny,rx,ry,depth,tilt,a.clock,struggle,sway,true);
            if(a.escapeLeft==0) {q.fillPoly(outline,0x208CCAE8);q.strokePoly(outline,0x996995BF,r*.045f);}
            if(holder) {
                q.line(grip[0],grip[1],cx+r*.27f,cy+r*.30f,0xFF3F4578,r*.20f);
                q.line(grip[0],grip[1],cx+r*.27f,cy+r*.30f,0xFFD8EEFF,r*.12f);
                q.line(grip[0]-r*.025f,grip[1]-r*.025f,cx+r*.24f,cy+r*.28f,0xFFFFFFFF,r*.035f);
            }
        }
        // Actual diagonal strands share a deformation field, so every mesh junction stays tied.
        for(int direction=-1;direction<=1;direction+=2)for(int i=-7;i<=7;i++) {
            float k=i*.25f,va=(k+direction-1)/(2*direction),vb=(k+direction+1)/(2*direction);
            float from=Math.max(0,Math.min(va,vb)),to=Math.min(1,Math.max(va,vb));
            if(to<=from)continue;
            float[] strand=new float[34];
            for(int j=0;j<=16;j++) {
                float v=from+(to-from)*j/16;
                netPoint(strand,j*2,k-direction*(2*v-1),v,nx,ny,rx,ry,depth,tilt,a.clock,struggle,sway,front);
            }
            if(a.escapeLeft>0) {
                // Each half pulls away from the torn seam; loose ends whip and fall.
                for(int half=0;half<2;half++) {
                    float[] torn=new float[16];float side=half==0?-1:1;
                    for(int j=0;j<8;j++) {
                        int at=half*9+j;float loose=half==0?j/7f:1-j/7f;
                        torn[j*2]=strand[at*2]+side*r*escape*(1.6f+loose*2);
                        torn[j*2+1]=strand[at*2+1]+r*(escape*escape*3+loose*(float)Math.sin(escape*18+i)*escape);
                    }
                    q.polyline(torn,front?0xEEDAF4FF:0x887DA6D6,r*(front?.045f:.03f));
                }
            } else q.polyline(strand,front?0xBCDAF4FF:0x447DA6D6,r*(front?.038f:.025f));
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
        if(!front || !holder)return;
        if(a.phase==RainbowCat.CATCH && a.netAge<.34f) {
            float burst=(float)Math.sin(u*Math.PI);
            for(int band=0;band<3;band++)q.arc(cx,cy,r*(1.5f+band*.30f),r*(1.5f+band*.30f),
                    175+u*170,110,fadeBy(COLORS[band+2],burst*.6f),r*.10f);
        }
        int who=c.companion.who;
        if(who>=0) {
            float cr=RunCompanion.radius(c,L)*(1+rise*.55f);
            float rock=winding?a.netAimX*.22f:a.phase==RainbowCat.CATCH?.34f*(float)Math.sin(u*Math.PI):-.20f*(float)Math.sin(a.clock*6);
            if(a.escapeLeft>0)rock=.85f*(float)Math.sin(escape*Math.PI)* (float)Math.cos(escape*8);
            Trinket.drawReacting(p,who,cx,cy,cr,c.companion.clock,1,a.escapeLeft>0?3:struggle>0?3:Trinket.FIGHT,rock,c.ninja());
            if(struggle>0) {
                float sweat=(a.clock*1.8f)%1;
                p.fillEllipse(cx-cr*.85f,cy-cr*.4f+cr*sweat*.65f,cr*.08f,cr*.14f,
                        fadeBy(0xFF8ED9F5,struggle*(1-sweat)));
            }
            // Both hands hold the pole; reaction props would obscure the grip.
            int color=Collect.BODY[who];
            p.fillEllipse(cx-r*.05f,cy-r*.08f,cr*.20f,cr*.17f,color);
            p.fillEllipse(cx+r*.22f,cy+r*.15f,cr*.20f,cr*.17f,color);
            if(a.phase==RainbowCat.OPEN || (a.escapeSequence && a.doubleFlurry))TutorialSpeech.companionFeedback(p,L,cx,cy,cr,
                    a.companionHint(),.5f,Collect.ACCENT[who]);
        }
    }
    private static void netPoint(float[] out,int at,float u,float v,float nx,float ny,float rx,float ry,float depth,
            float tilt,float clock,float struggle,float sway,boolean front) {
        float cross=(float)Math.sqrt(Math.max(0,1-u*u)),width=(float)Math.sqrt(Math.max(0,1-v));
        float slack=(float)Math.sin(v*Math.PI)*(1-u*u);
        // The mouth stays fixed to the hoop; hanging mesh trails, reverses and settles below it.
        float bend=sway*v;
        float px=u*rx*width+depth*v*(float)Math.sin(bend)+rx*slack*.12f*struggle*(float)Math.sin(clock*18+v*2);
        float py=depth*v*(float)Math.cos(bend)+(front?1:-1)*ry*cross*(1-v)
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
        int[] buckets=new int[COLORS.length];
        for(int start=0;start<n-1;) {
            for(int band=0;band<COLORS.length;band++)buckets[band]=trailOpacity(progress[start+1],band);
            int end=start+1;
            while(end<n-1) {
                boolean same=true;
                for(int band=0;band<COLORS.length;band++)same&=trailOpacity(progress[end+1],band)==buckets[band];
                if(!same)break;
                end++;
            }
            for(int band=0;band<COLORS.length;band++) {
                int bucket=buckets[band];if(bucket<=0)continue;
                int count=end-start+1;float[] strip=new float[count*4];
                float low=(band-3)*a.r*.23f,high=low+a.r*.24f;
                for(int j=0;j<count;j++) {
                    int at=start+j,back=count*2-1-j;
                    strip[j*2]=x[at]+nx[at]*low;strip[j*2+1]=y[at]+ny[at]*low;
                    strip[back*2]=x[at]+nx[at]*high;strip[back*2+1]=y[at]+ny[at]*high;
                }
                p.fillPoly(strip,Glyph.withAlpha(trailColor(p,band,COLORS[band]),160*bucket/16));
            }
            start=end;
        }
    }
    private static int trailOpacity(float progress,int band) {
        // Outer colors disappear first; the two middle bands form the rounded end of the ribbon.
        float edge=(Math.abs(band-(COLORS.length-1)*.5f)-.5f)/2;
        return (int)(RainbowCat.trailFade(progress+.16f*edge*edge)*16);
    }
    static void rushProjectile(Painter p,GameCore c,Layout L,Boss b,int i,float x,float y,float r,int color) {
        if(b.cat.escapeSequence && b.bt[i]<0)return;
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
        cat(p,x,y,r,t,facing,body,skull,caught,known,0,0);
    }
    static void arrivalCat(Painter p,RainbowCat a,float cry) {
        cat(p,a.x,a.y,a.r,a.clock*2,a.turn,a.pastry,a.head,false,1,0,0,cry);
    }
    private static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known,float scared,float angry) {
        cat(p,x,y,r,t,facing,body,skull,caught,known,scared,angry,0);
    }
    private static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known,float scared,float angry,float cry) {
        cat(p,x,y,r,t,facing,body,skull,caught,known,scared,angry,cry,0,0);
    }
    static void laughingCat(Painter p,RainbowCat a,float age,float laugh) {
        cat(p,a.x,a.y,a.r,a.clock+age*2,a.turn,a.pastry,a.head,false,1,0,0,0,0,0,laugh);
    }
    private static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known,float scared,float angry,float cry,float wind,float reach) {
        cat(p,x,y,r,t,facing,body,skull,caught,known,scared,angry,cry,wind,reach,0);
    }
    private static void cat(Painter p,float x,float y,float r,float t,float facing,Softbody body,Softbody skull,boolean caught,float known,float scared,float angry,float cry,float wind,float reach,float laugh) {
        boolean vampire=Skins.halloween(p);
        int crust=known>0?(vampire?0xFF805294:0xFFFFD49A):0xFF49425F;
        int fur=known>0?(vampire?0xFFB58BC7:FUR):0xFF49425F;
        float step=(float)Math.sin(t*(caught?15:7));
        // Static portraits retain their decorative ribbon; battle uses recorded motion.
        if(body==null)for(int band=0;band<COLORS.length;band++) {
            float[] trail=new float[36];
            for(int i=0;i<9;i++){float u=i/8f;trail[i*2]=x-facing*r*(.8f+u*2.9f);trail[i*2+1]=y+r*((band-2.5f)*.24f+(float)Math.sin(t*4-u*3)*u*.30f)-r*.12f;trail[(17-i)*2]=trail[i*2];trail[(17-i)*2+1]=trail[i*2+1]+r*.24f;}
            p.fillPoly(trail,fadeBy(trailColor(p,band,COLORS[band]),.48f*known));
        }
        float[] tail={x-facing*r*.85f,y+r*.4f,x-facing*r*1.5f,y+r*.25f,x-facing*r*1.65f,y-r*(.10f+step*.16f)};
        p.polyline(tail,INK,r*.30f);p.polyline(tail,fur,r*.24f);
        for(int foot=0;foot<4;foot++) {
            float fx=x+r*(-.85f+foot*.51f),fy=y+r*(.9f+.09f*(foot%2==0?step:-step)-.30f*wind+.58f*reach);
            fx+=facing*r*.18f*reach;
            p.line(x+r*(-.85f+foot*.51f),y+r*.60f,fx,fy,INK,r*.35f);
            p.line(x+r*(-.85f+foot*.51f),y+r*.60f,fx,fy,fur,r*.27f);
            p.fillEllipse(fx,fy,r*.22f,r*.26f,INK);p.fillEllipse(fx,fy-r*.04f,r*.17f,r*.21f,fur);
        }
        float[] outline=pastry(body,x,y,r);
        p.fillPoly(outline,INK);p.fillPoly(inset(outline,x,y,.96f,.95f),crust);
        p.fillPoly(inset(outline,x-r*.02f,y-r*.04f,.81f,.83f),known>0?(vampire?0xFFA574BC:0xFFFF92CC):fur);
        p.fillPoly(inset(outline,x-r*.09f,y-r*.13f,.70f,.66f),known>0?(vampire?0xFFC8A0D9:0xFFFFB1DD):fur);
        if(known>0 && !vampire)for(int i=0;i<10;i++) {
            float px=x+r*(-.75f+(i%4)*.43f),py=y+r*(-.61f+(i/4)*.53f);
            p.line(px,py,px+r*.06f,py+r*.10f,COLORS[(i+1)%COLORS.length],r*.07f);
        }
        if(vampire)cape(p,outline,x,y,r,t,facing,known);
        float hx=skull==null?x+facing*r*.96f:skull.centreX();
        float hy=skull==null?y+r*.1f:skull.centreY();
        float hr=(skull==null?r*.66f:skull.radiusY())*1.13f;
        // Plush cheeks, small pointed ears and a wide round chin share one silhouette.

        float[] shape=face(p,hx,hy,hr);
        p.fillPoly(shape,INK);p.fillPoly(inset(shape,hx,hy,.93f,.92f),fur);
        p.fillPoly(inset(shape,hx-hr*.08f,hy-hr*.1f,.80f,.72f),known>0?(vampire?0xFFD4B4E0:0xFFC4C2D3):fur);
        if(known<=0)return;
        p.fillEllipse(hx,hy+hr*.33f,hr*.49f,hr*.32f,0x55FFF6EE);
        for(int side=-1;side<=1;side+=2) {
            p.fillPoly(points(new float[]{side*.45f,-.62f,side*(vampire?.78f:.73f),vampire?-1.13f:-.92f,side*.77f,-.43f},hx,hy,hr,hr),0xFFFFB4CB);
            float ex=hx+side*hr*.40f,ey=hy+hr*.02f;
            boolean blink=scared==0 && angry==0 && Math.sin(t*1.7f)>.985;
            if(laugh>0) {
                p.arc(ex,ey+hr*.10f,hr*.19f,hr*.17f,195,150,INK,hr*.075f);
                p.line(ex-side*hr*.18f,ey,ex-side*hr*.26f,ey-hr*.08f,INK,hr*.045f);
            } else if(caught) {
                float squeeze=.05f*(float)Math.sin(t*18);
                p.polyline(points(new float[]{side*.62f,-.08f-squeeze,side*.28f,.04f,side*.61f,.17f+squeeze},
                        hx,hy,hr,hr),INK,hr*.075f);
                p.polyline(points(new float[]{side*.27f,-.28f,side*.43f,-.34f,side*.62f,-.30f},hx,hy,hr,hr),INK,hr*.055f);
            } else {
                p.fillEllipse(ex,ey,hr*(.18f+.045f*scared),hr*(blink?.045f:.235f+.025f*scared-.09f*angry),INK);
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
            if(angry>0)p.polyline(points(new float[]{side*.19f,-.12f,side*.40f,-.25f,side*.65f,-.30f},hx,hy,hr,hr),fadeBy(INK,angry),hr*.09f);
            p.fillEllipse(hx+side*hr*.71f,hy+hr*.31f,hr*.19f,hr*.105f,0xAFFF83BD);
            p.line(hx+side*hr*.71f,hy+hr*.38f,hx+side*hr*.93f,hy+hr*.35f,0x88766891,hr*.035f);
        }
        p.fillEllipse(hx,hy+hr*.23f,hr*.095f,hr*.065f,0xFFBE7FA7);
        if(laugh>0) {
            p.fillEllipse(hx,hy+hr*.49f,hr*.27f,hr*(.12f+.13f*laugh),INK);
            p.fillEllipse(hx,hy+hr*.57f,hr*.17f,hr*.085f,0xFFFFA8CB);
        } else if(cry>0) {
            float mouth=cry*(.85f+.15f*(float)Math.sin(t*9));
            p.fillEllipse(hx,hy+hr*.49f,hr*.23f,hr*(.06f+.22f*mouth),INK);
            p.fillEllipse(hx,hy+hr*.60f,hr*.13f,hr*.065f*mouth,0xFFFFA8CB);
        } else if(caught) {
            p.fillEllipse(hx,hy+hr*.48f,hr*.13f,hr*(.12f+.035f*(float)Math.sin(t*18)),INK);
            p.fillEllipse(hx,hy+hr*.54f,hr*.085f,hr*.045f,0xFFFFB4CB);
            p.fillEllipse(hx+hr*.89f,hy-hr*.25f,hr*.07f,hr*.13f,0xBBBEF6FF);
        } else {
            if(scared>0) {
                p.fillEllipse(hx,hy+hr*.45f,hr*(.08f+.03f*scared),hr*(.08f+.09f*scared),fadeBy(INK,scared));
                p.fillEllipse(hx+hr*.91f,hy-hr*.25f,hr*.06f*scared,hr*.13f*scared,0xCCB6F5FF);
            }
            p.polyline(points(new float[]{-.25f,.34f,-.19f,.43f,-.10f,.45f,0,.34f,.10f,.45f,.19f,.43f,.25f,.34f},hx,hy,hr,hr),fadeBy(INK,(1-scared)*(1-angry)),hr*.055f);
            if(angry>0)p.polyline(points(new float[]{-.23f,.47f,0,.37f,.23f,.47f},hx,hy,hr,hr),fadeBy(INK,angry),hr*.075f);
        }
        if(vampire)for(int side=-1;side<=1;side+=2) {
            float fx=hx+side*hr*.19f,fy=hy+hr*(cry>0?.48f:.43f);
            p.fillPoly(new float[]{fx-hr*.075f,fy,fx+hr*.075f,fy,fx+hr*.025f,fy+hr*.18f,fx-hr*.025f,fy+hr*.18f},0xFFFFF9E8);
            p.fillCircle(fx,fy+hr*.16f,hr*.035f,0xFFFFF9E8);
        }
    }
    private static float[] face(Painter p,float x,float y,float r) {
        float[] shape=points(FACE,x,y,r,r);
        if(Skins.halloween(p))for(int i=0;i<FACE.length;i+=2)if(FACE[i+1]<-.5f) {
            shape[i]=x+FACE[i]*r*1.06f;
            shape[i+1]=y+r*(-.5f+(FACE[i+1]+.5f)*1.45f);
        }
        return shape;
    }
    private static void cape(Painter p,float[] back,float x,float y,float r,float t,float facing,float known) {
        final int columns=28,rows=8;
        float direction=facing<0?-1:1;
        float[] mesh=new float[(columns+1)*(rows+1)*2];
        for(int col=0;col<=columns;col++) {
            float u=col/(float)columns,local=.97f-2.90f*u,xx=x+direction*r*local;
            float top=Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
            // The shoulder and back follow the actual spring silhouette, not a floating ribbon.
            float sample=x+direction*r*Math.max(-1.07f,local);
            for(int i=0;i<back.length;i+=2) {
                int next=(i+2)%back.length;
                float ax=back[i],bx=back[next];
                if(Math.abs(bx-ax)<.0001f || sample<Math.min(ax,bx) || sample>Math.max(ax,bx))continue;
                float yy=back[i+1]+(back[next+1]-back[i+1])*(sample-ax)/(bx-ax);
                top=Math.min(top,yy);bottom=Math.max(bottom,yy);
            }
            if(top==Float.MAX_VALUE) {top=y-r*.82f;bottom=y+r*.85f;}
            float free=RainbowCat.clamp((-local-1.02f)/.91f,0,1);
            top-=r*.045f;
            bottom-=r*(.18f*(1-u)+.08f);
            // Two travelling waves: the neck stays pinned while the free hem curls and flaps.
            float wave=(float)Math.sin(t*7.5f-u*8.5f),ripple=(float)Math.sin(t*11-u*13);
            float lift=free*free*r*(.10f+.30f*wave+.09f*ripple);
            top+=lift-free*r*.12f;bottom+=lift-free*r*.22f;
            for(int row=0;row<=rows;row++) {
                float v=row/(float)rows;
                int at=(col*(rows+1)+row)*2;
                float curl=free*free*r*(.11f*(float)Math.sin(t*7.5f-u*8.5f+v*3.8f)+.09f*(float)Math.sin(v*Math.PI));
                mesh[at]=xx+direction*curl;
                mesh[at+1]=top+(bottom-top)*v
                        +r*.055f*u*(float)Math.sin(v*Math.PI)*(float)Math.sin(t*7.5f-u*8.5f+v*2);
            }
        }
        float[] cloth=new float[(columns*2+rows*2)*2];int at=0;
        for(int col=0;col<=columns;col++)at=capeVertex(cloth,at,mesh,col,0,rows);
        for(int row=1;row<=rows;row++)at=capeVertex(cloth,at,mesh,columns,row,rows);
        for(int col=columns-1;col>=0;col--)at=capeVertex(cloth,at,mesh,col,rows,rows);
        for(int row=rows-1;row>0;row--)at=capeVertex(cloth,at,mesh,0,row,rows);
        p.fillPoly(cloth,known>0?0xFF37393F:INK);
        if(known>0) {
            int[] shades={0xFF656872,0xFF595C65,0xFF4B4E57,0xFF40434B,0xFF393C43,0xFF34373E,0xFF303239,0xFF393C44};
            float[] strip=new float[(columns+1)*4];
            for(int row=0;row<rows;row++) {
                at=0;
                for(int col=0;col<=columns;col++)at=capeVertex(strip,at,mesh,col,row,rows);
                for(int col=columns;col>=0;col--)at=capeVertex(strip,at,mesh,col,row+1,rows);
                p.fillPoly(strip,shades[row]);
            }
            // Low-contrast folds travel through the fabric with the flutter.
            float[] fold=new float[(columns+1)*2];
            for(int row:new int[]{2,5}) {
                for(int col=0;col<=columns;col++)capeVertex(fold,col*2,mesh,col,row,rows);
                p.polyline(fold,row==2?0x307C818B:0x40303239,r*.025f);
            }
        }
        p.strokePoly(cloth,known>0?0xFF292B32:INK,r*.055f);
        if(known>0) {
            // A soft raised collar and tiny clasp sit underneath the head.
            p.fillEllipse(x+direction*r*.78f,y-r*.26f,r*.24f,r*.43f,0xFF454850);
            p.fillEllipse(x+direction*r*.77f,y-r*.29f,r*.17f,r*.34f,0xFF626670);
            p.fillEllipse(x+direction*r*.78f,y+r*.48f,r*.12f,r*.10f,0xFFC9EE91);
            p.fillCircle(x+direction*r*.75f,y+r*.45f,r*.035f,0xFFF7FFDB);
        }
    }
    private static int capeVertex(float[] out,int at,float[] mesh,int col,int row,int rows) {
        int point=(col*(rows+1)+row)*2;
        out[at]=mesh[point];out[at+1]=mesh[point+1];return at+2;
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
