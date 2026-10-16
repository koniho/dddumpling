package com.dddumpling.game;

/** Spreads the actual slime skin, face and emblem into one dripping sheet of jelly. */
final class SlimeDeath extends OpacityPainter {
    static final float SPLAT_AT=Boss.LEAVE*.38f;
    static final class Pose {
        static final int ROWS=128;
        final float[] skin;
        final float[] leftEdge=new float[ROWS+1],rightEdge=new float[ROWS+1];
        final float x,y,rx,ry,radius,mx,my,halfW,halfH;
        Pose(Softbody body) {
            skin=BossScreen.slimeBossOutline(body);
            x=body.centreX();y=body.centreY();rx=body.radiusX();ry=body.radiusY();radius=body.radius();
            float left=Float.MAX_VALUE,right=-left,top=left,bottom=-left;
            for(int i=0;i<skin.length;i+=2) {
                left=Math.min(left,skin[i]);right=Math.max(right,skin[i]);
                top=Math.min(top,skin[i+1]);bottom=Math.max(bottom,skin[i+1]);
            }
            mx=(left+right)*.5f;my=(top+bottom)*.5f;
            halfW=Math.max(1f,(right-left)*.5f);halfH=Math.max(1f,(bottom-top)*.5f);
            // Normalize each row, so a stretched neck cannot survive as a skinny final splat.
            for(int row=0;row<=ROWS;row++) {
                float scanY=top+(bottom-top)*Math.max(.0001f,Math.min(.9999f,row/(float)ROWS));
                float lo=Float.MAX_VALUE,hi=-lo;
                for(int i=0;i<skin.length;i+=2) {
                    int j=(i+2)%skin.length;
                    if((skin[i+1]>scanY)==(skin[j+1]>scanY))continue;
                    float px=skin[i]+(skin[j]-skin[i])*(scanY-skin[i+1])/(skin[j+1]-skin[i+1]);
                    lo=Math.min(lo,px);hi=Math.max(hi,px);
                }
                leftEdge[row]=lo==Float.MAX_VALUE?mx:lo;
                rightEdge[row]=hi==-Float.MAX_VALUE?mx:hi;
            }
        }
        float across(float x,float y) {
            float at=Math.max(0f,Math.min(ROWS,(y-my+halfH)/(2f*halfH)*ROWS));
            int row=Math.min(ROWS-1,(int)at);float t=at-row;
            float left=leftEdge[row]+(leftEdge[row+1]-leftEdge[row])*t;
            float right=rightEdge[row]+(rightEdge[row+1]-rightEdge[row])*t;
            return Math.max(-1f,Math.min(1f,(x-(left+right)*.5f)/Math.max(.001f,(right-left)*.5f)));
        }
    }

    final Pose pose;
    final Layout layout;
    final float spread,drip,time;
    SlimeDeath(Painter p,Boss b,Layout L,float alpha) {
        super(p,alpha*opacity(b.leaveProgress()*Boss.LEAVE));
        pose=b.slimeDeath==null?new Pose(b.body):b.slimeDeath;
        layout=L;time=b.leaveProgress()*Boss.LEAVE;
        float u=Math.max(0f,Math.min(1f,(time-.15f)/(SPLAT_AT-.15f)));
        spread=1f-(1f-u)*(1f-u)*(1f-u);
        drip=MushroomDeath.ease(time,SPLAT_AT,Boss.LEAVE);
    }

    static float opacity(float time) {return 1f-MushroomDeath.ease(time,2.25f,Boss.LEAVE);}

    float x(float x,float y) {
        float u=pose.across(x,y),v=Math.max(-1f,Math.min(1f,(y-pose.my)/pose.halfH));
        float round=(float)Math.pow(Math.max(0f,1f-v*v*v*v),.25f);
        float target=layout.w*.5f+u*layout.w*.30f
                *round*(1f-drip*.02f);
        return x+(target-x)*spread;
    }

    float y(float x,float y) {
        float u=pose.across(x,y),v=(y-pose.my)/pose.halfH;
        float height=layout.dangerY-layout.playTop;
        float lower=Math.max(0f,Math.min(1f,(v+.1f)/1.1f));
        float finger=(float)Math.pow(.5f+.5f*Math.sin(u*10f+1f),3);
        float target=layout.playTop+height*.36f+v*layout.h*.185f
                +layout.h*.005f*(float)Math.sin(u*9f)*lower
                +layout.h*drip*(.006f+.032f*finger)*lower*lower+layout.h*.025f*drip*drip;
        return y+(target-y)*spread;
    }

    static void draw(Painter p,GameCore c,Layout L,Boss b,float alpha) {
        SlimeDeath warp=new SlimeDeath(p,b,L,alpha);
        if(warp.amount<=.001f)return;
        Pose s=warp.pose;
        BossScreen.drawSlimeSkin(warp,s.skin,s.x,s.y,s.rx,s.ry,s.radius,
                Glyph.mix(0xFF83E51C,0xFFB5ED62,warp.drip*.5f),c.clock,false,1f,true);
        // The face settles within the frame rather than inheriting a stretched neck's shear.
        float center=L.playTop+(L.dangerY-L.playTop)*.36f;
        BossScreen.slimeDizzyFace(p,s.x+(L.w*.5f-s.x)*warp.spread,
                s.y+(center-s.y)*warp.spread+L.h*.025f*warp.drip*warp.drip,
                s.rx+(L.w*.30f-s.rx)*warp.spread,s.ry+(L.h*.10f-s.ry)*warp.spread,c.clock,warp.amount);
    }

    float[] points(float[] raw) {
        // Sample between vertices before bending, so the drip tips stay round at phone size.
        int steps=spread==0f?1:4,segments=raw.length/2-1;
        float[] out=new float[(segments*steps+1)*2];
        for(int i=0;i<=segments*steps;i++) {
            int a=Math.min(segments,i/steps)*2,b=Math.min(raw.length-2,a+2);
            float t=(i%steps)/(float)steps;
            float px=raw[a]+(raw[b]-raw[a])*t,py=raw[a+1]+(raw[b+1]-raw[a+1])*t;
            out[i*2]=x(px,py);out[i*2+1]=y(px,py);
        }
        return out;
    }
    public void fillPoly(float[] xy,int c) {p.fillPoly(points(xy),color(c));}
    public void strokePoly(float[] xy,int c,float w) {p.strokePoly(points(xy),color(c),w*(1f-spread*.35f));}
    public void polyline(float[] xy,int c,float w) {p.polyline(points(xy),color(c),w*(1f-spread*.25f));}
    public void fillCircle(float x,float y,float r,int c) {fillEllipse(x,y,r,r,c);}
    public void fillEllipse(float x,float y,float rx,float ry,int c) {
        float[] points=new float[64];
        for(int i=0;i<32;i++) {
            float a=Softbody.TAU*i/32f;
            points[i*2]=x+(float)Math.cos(a)*rx;points[i*2+1]=y+(float)Math.sin(a)*ry;
        }
        fillPoly(points,c);
    }
    public void line(float x1,float y1,float x2,float y2,int c,float w) {
        polyline(new float[]{x1,y1,x2,y2},c,w);
    }
}
