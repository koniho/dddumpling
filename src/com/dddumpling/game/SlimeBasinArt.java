package com.dddumpling.game;

/** Immutable scenery and soft light masks, rasterized once by each native host. */
final class SlimeBasinArt extends Draw {
    static final int SCENE=0,LIGHT=1,SHADOW=2,LEFT_BANK=3,RIGHT_BANK=4,COUNT=5;
    static final float BANK_FRACTION=.70f;
    static int width(int art) {return art==SCENE?480:art>=LEFT_BANK?336:256;}
    static int height(int art) {return art==SCENE || art>=LEFT_BANK?960:256;}
    private SlimeBasinArt() {}
    static void paint(Painter p,int art,float w,float h) {
        if(art>=LEFT_BANK) {
            int side=art-LEFT_BANK;float fullWidth=w/BANK_FRACTION;
            p.save();p.clipRect(0,0,w,h);
            if(side==1)p.translate(-(fullWidth-w),0);
            banks(p,side,fullWidth,h);p.restore();return;
        }
        if(art!=SCENE) {
            // Dense enough for a smooth cached texture; never rebuilt for lighting changes.
            int color=art==LIGHT?0xFF99E58D:0xFF0D3029;
            float[][] ring={new float[98],new float[98]};
            for(int i=0;i<64;i++) {
                float outer=(i+1)/64f,inner=i/64f;
                float strength=1-inner*inner;strength*=strength;
                for(int j=0;j<49;j++) {
                    float angle=Softbody.TAU*j/49f;
                    float dx=(float)Math.cos(angle)*w*.49f,dy=(float)Math.sin(angle)*h*.49f;
                    ring[0][j*2]=w*.5f+dx*outer;ring[0][j*2+1]=h*.5f+dy*outer;
                    ring[1][j*2]=w*.5f+dx*inner;ring[1][j*2+1]=h*.5f+dy*inner;
                }
                p.fillContours(ring,Glyph.withAlpha(color,(int)(strength*190)));
            }
            return;
        }
        p.fillRect(0,0,w,h,0xFF153D34);
        for(int i=47;i>=0;i--) {
            float r=i/47f;
            p.fillEllipse(w*.5f,h*.34f,w*(.20f+r*.66f),h*(.10f+r*.57f),0x046FBC73);
        }
        rearBank(p,w,h);
    }

    /** Rounded shoulders rise at the sides and melt into a low, scalloped waterline. */
    private static void rearBank(Painter p,float w,float h) {
        float[] bank=new float[134],edge=new float[260],blob=new float[98];
        for(int layer=0;layer<3;layer++) {
            for(int j=0;j<=64;j++) {
                float u=j/64f;
                float y=h*(.312f-.025f*(float)Math.sin(u*Math.PI)
                        +.005f*(float)Math.sin(u*18+.6f)-layer*.006f);
                bank[j*2]=edge[j*2]=w*u;bank[j*2+1]=edge[j*2+1]=y;
                edge[(129-j)*2]=w*u;edge[(129-j)*2+1]=y-h*.002f;
            }
            bank[130]=w;bank[131]=0;bank[132]=0;bank[133]=0;
            p.fillPoly(bank,layer==0?0xFF599B70:layer==1?0xFF32694E:0xFF1A4036);
            p.fillPoly(edge,Glyph.withAlpha(SlimeHills.MINT,layer==0?80:22));
        }
        for(int i=0;i<7;i++) {
            float x=w*(.05f+i*.15f),y=h*(.35f-.035f*(float)Math.sin(i/6f*Math.PI));
            SlimeHills.jelly(p,blob,x,y,w*(.028f+hash(i+811)*.035f),h*.0025f,i,0x356EBE88);
        }
    }
    private static void rearShoulder(Painter p,int side,float w,float h) {
        float[] blob=new float[98];
        // Unequal domes and small melting shoulders form one silhouette, not stacked ovals.
        float[] points=side==0?new float[]{
                -.08f,.205f,-.015f,.174f,.055f,.184f,.105f,.218f,.13f,.257f,
                .205f,.267f,.23f,.29f,.29f,.296f,.345f,.315f,.26f,.329f,.11f,.337f,-.08f,.35f}
                :new float[]{
                -.08f,.19f,.015f,.166f,.083f,.188f,.116f,.244f,.172f,.263f,
                .23f,.258f,.264f,.283f,.315f,.299f,.35f,.32f,.235f,.335f,.085f,.338f,-.08f,.35f};
        float[] shape=rearContour(points);
        for(int layer=0;layer<4;layer++) {
            float scale=1-layer*.065f;
            float[] face=new float[shape.length];
            for(int j=0;j<shape.length;j+=2) {
                float x=.08f+(shape[j]-.08f)*scale-layer*.008f;
                float y=.326f+(shape[j+1]-.326f)*(1-layer*.11f)-layer*.008f;
                face[j]=w*(side==0?x:1-x);face[j+1]=h*y;
            }
            p.fillPoly(face,layer==0?0xFF2D674C:layer==1?0xFF4E9C67:layer==2?0xFF70BE82:0xFF468C61);
        }
        // A low, lopsided skirt flows out into the water along each shoulder's foot.
        float[] lip=rearContour(new float[]{-.04f,.319f,.045f,.309f,.12f,.316f,.19f,.309f,
                .25f,.318f,.33f,.321f,.31f,.33f,.22f,.333f,.13f,.329f,.04f,.34f,-.04f,.342f});
        for(int layer=0;layer<3;layer++) {
            float[] face=new float[lip.length];
            for(int j=0;j<lip.length;j+=2) {
                face[j]=w*(side==0?lip[j]:1-lip[j]);
                face[j+1]=h*(lip[j+1]-layer*.0025f);
            }
            p.fillPoly(face,layer==0?0xFF78C38A:layer==1?0xFF4D9A6C:0xFF61AF78);
        }
        for(int i=0;i<4;i++) {
            float x=w*(.035f+i*.057f),y=h*(.205f+i*.030f-i*i*.002f);
            if(side==1)x=w-x;
            SlimeHills.jelly(p,blob,x,y,w*(.012f+hash(i+63)*.013f),h*.0038f,i,0x90B9E8A5);
        }
    }
    /** Smooth a traced closed outline once while producing the cached scenery. */
    private static float[] rearContour(float[] points) {
        int n=points.length/2,steps=8;float[] shape=new float[n*steps*2];
        for(int i=0;i<n;i++)for(int j=0;j<steps;j++) {
            float t=j/(float)steps,t2=t*t,t3=t2*t;
            for(int axis=0;axis<2;axis++) {
                float a=points[((i+n-1)%n)*2+axis],b=points[i*2+axis];
                float c=points[((i+1)%n)*2+axis],d=points[((i+2)%n)*2+axis];
                shape[(i*steps+j)*2+axis]=.5f*(2*b+(-a+c)*t
                        +(2*a-5*b+4*c-d)*t2+(-a+3*b-3*c+d)*t3);
            }
        }
        return shape;
    }
    private static void banks(Painter p,int side,float w,float h) {
        float[] shape=new float[98],bank=new float[134],shine=new float[260];
        for(int layer=0;layer<6;layer++) {
            for(int j=0;j<=64;j++) {
                float u=j/64f,x=inset(u,side,w)-w*layer*.024f;
                x+=w*.007f*(float)Math.sin(u*24+layer*.7f+side);
                if(side==1)x=w-x;
                bank[j*2]=x;bank[j*2+1]=h*u;
                shine[j*2]=x;shine[j*2+1]=h*u;
                float join=Math.max(0,Math.min(1,(u-.88f)/.08f));join=join*join*(3-2*join);
                shine[(129-j)*2]=x+(side==0?-1:1)*w*(layer==1?.009f:.005f)*(1-join);
                shine[(129-j)*2+1]=h*u;
            }
            bank[130]=bank[132]=side==0?-w*.05f:w*1.05f;bank[131]=h;bank[133]=0;
            int color=layer==0?0xFF66B88C:layer==1?0xFF86D99C:layer==2?0xFF68C28C
                    :layer==3?0xFF4A9F79:layer==4?0xFF357C63:0xFF28604F;
            p.fillPoly(bank,Glyph.withAlpha(color,layer==0?115:175));
            p.fillPoly(shine,Glyph.withAlpha(SlimeHills.MINT,layer==1?105:32));
        }
        rearShoulder(p,side,w,h);
        // The foreground banks curl into broad jelly cushions with internal depth.
        for(int i=0;i<2;i++) {
            float u=(side==0?.49f:.43f)+i*.43f;
            float x=i==0?inset(u,side,w)-w*.13f:w*(side==0?.035f:.06f);
            if(side==1)x=w-x;
            mound(p,shape,x,h*u,w*(i==0?.17f:.23f),w*(i==0?.11f:.17f),side*3+i);
        }
        upperWall(p,side,w,h);
        for(int i=0;i<16;i++) {
            if(i%2!=side)continue;
            float u=.10f+hash(i+927)*.87f;
            float x=inset(u,side,w)-w*(.025f+hash(i+64)*.08f);if(side==1)x=w-x;
            SlimeHills.jelly(p,shape,x,h*u,w*(.008f+hash(i+76)*.016f),w*.008f,i,0x608AE7AF);
        }
    }
    /** One hanging slime curtain with rounded lobes, rather than separate striped rods. */
    private static void upperWall(Painter p,int side,float w,float h) {
        float[] wall=new float[198],gleam=new float[34];
        for(int layer=0;layer<5;layer++) {
            for(int j=0;j<=96;j++) {
                float u=j/96f,xx=w*(u*.34f-.035f);
                float y=curtainEdge(u,side)*h-layer*h*.006f;
                wall[j*2]=side==0?xx:w-xx;wall[j*2+1]=y;
            }
            wall[194]=side==0?w*.305f:w*.695f;wall[195]=-h*.1f;
            wall[196]=side==0?-w*.035f:w*1.035f;wall[197]=-h*.1f;
            int color=layer==0?0xFF70B884:layer==1?0xFF4D996A:layer==2?0xFF397C55
                    :layer==3?0xFF2C6147:0xFF234D3D;
            p.fillPoly(wall,color);
        }
        for(int i=0;i<3;i++) {
            float u=i==0?.20f:i==1?.52f:.82f;
            float x=w*(u*.34f-.035f),y=curtainEdge(u,side)*h;
            if(side==1)x=w-x;
            SlimeHills.jelly(p,gleam,x-w*.008f,y-h*.018f,w*(i==0?.018f:.009f),h*.008f,i,0x77B9ECA7);
            SlimeHills.jelly(p,gleam,x,y-h*.045f,w*(i==0?.014f:.006f),h*.018f,i,0x286AC481);
        }
    }
    private static float curtainEdge(float u,int side) {
        float base=.085f*(1-u)+.012f;
        float outer=.115f*roundedLobe(u,.20f+side*.025f,.20f);
        float middle=.075f*roundedLobe(u,.52f-side*.025f,.10f);
        float inner=.105f*roundedLobe(u,.82f+side*.014f,.065f);
        return base+Math.max(outer,Math.max(middle,inner));
    }
    private static float roundedLobe(float u,float centre,float radius) {
        float t=(u-centre)/radius;
        float shoulder=Math.max(0,1-t*t);
        return shoulder*shoulder;
    }

    static float inset(float u,int side,float w) {
        float edge=.045f+.25f*bump(u,.01f,.30f)+.19f*bump(u,side==0?.49f:.43f,.15f)
                +.20f*bump(u,side==0?.94f:.85f,.24f)+.025f*(float)Math.sin(u*19+side);
        // The foreground curls across the pool, with enough overlap to breathe without a gap.
        float curl=Math.max(0,Math.min(1,(u-.75f)/.20f));curl=curl*curl*curl;
        return w*(edge+(.68f-edge)*curl);
    }
    private static float bump(float u,float centre,float width) {
        float t=(u-centre)/width;float a=Math.max(0,1-t*t);return a*a;
    }
    private static void mound(Painter p,float[] shape,float x,float y,float rx,float ry,int seed) {
        SlimeHills.jelly(p,shape,x,y,rx,ry,seed,0xA731735A);
        SlimeHills.jelly(p,shape,x,y-ry*.13f,rx*.97f,ry*.90f,seed,0x9A72CE90);
        SlimeHills.jelly(p,shape,x-rx*.12f,y-ry*.28f,rx*.76f,ry*.62f,seed,0x405DE6A1);
        SlimeHills.jelly(p,shape,x-rx*.17f,y-ry*.40f,rx*.53f,ry*.33f,seed,0x348BF0B0);
        SlimeHills.jelly(p,shape,x-rx*.30f,y-ry*.58f,rx*.17f,ry*.10f,seed,0xB8D2F8B8);
        SlimeHills.jelly(p,shape,x+rx*.15f,y-ry*.62f,rx*.045f,ry*.055f,seed,0x90D2F8B8);
    }
}
