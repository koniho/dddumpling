package com.dddumpling.game;

/** Immutable scenery and soft light masks, rasterized once by each native host. */
final class SlimeBasinArt extends Draw {
    static final int SCENE=0,LIGHT=1,SHADOW=2,COUNT=3;
    static int width(int art) {return art==SCENE?480:256;}
    static int height(int art) {return art==SCENE?960:256;}
    private SlimeBasinArt() {}
    static void paint(Painter p,int art,float w,float h) {
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
        float[] shape=new float[98],bank=new float[134],shine=new float[260];
        for(int side=0;side<2;side++)for(int layer=0;layer<6;layer++) {
            for(int j=0;j<=64;j++) {
                float u=j/64f,x=inset(u,side,w)-w*layer*.024f;
                x+=w*.007f*(float)Math.sin(u*24+layer*.7f+side);
                if(side==1)x=w-x;
                bank[j*2]=x;bank[j*2+1]=h*u;
                shine[j*2]=x;shine[j*2+1]=h*u;
                shine[(129-j)*2]=x+(side==0?-1:1)*w*(layer==1?.009f:.005f);
                shine[(129-j)*2+1]=h*u;
            }
            bank[130]=bank[132]=side==0?-w*.05f:w*1.05f;bank[131]=h;bank[133]=0;
            int color=layer==0?0xFF66B88C:layer==1?0xFF86D99C:layer==2?0xFF68C28C
                    :layer==3?0xFF4A9F79:layer==4?0xFF357C63:0xFF28604F;
            p.fillPoly(bank,Glyph.withAlpha(color,layer==0?115:175));
            p.fillPoly(shine,Glyph.withAlpha(SlimeHills.MINT,layer==1?105:32));
        }
        // Low, rounded shelves establish the pool's horizon rather than a vertical tunnel.
        for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float x=w*(.025f+i*.054f);if(side==1)x=w-x;
            float y=h*(.29f-i*.016f),r=w*(.15f-i*.028f);
            mound(p,shape,x,y,r,r*(.58f+i*.06f),i+side);
        }
        // The foreground banks curl into broad jelly cushions with internal depth.
        for(int side=0;side<2;side++)for(int i=0;i<2;i++) {
            float u=(side==0?.49f:.43f)+i*.43f,x=inset(u,side,w)-w*.13f;
            if(side==1)x=w-x;
            mound(p,shape,x,h*u,w*.17f,w*(i==0?.11f:.19f),side*3+i);
        }
        for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float x=w*(.025f+i*.050f);if(side==1)x=w-x;
            float len=h*(.16f-i*.035f);
            SlimeHills.jelly(p,shape,x,len*.15f,w*(.045f-i*.007f),len,i+side,0x9957AC7D);
            SlimeHills.jelly(p,shape,x-w*.007f,len*.88f,w*.011f,w*.025f,i,0x85BFF2B5);
        }
        for(int i=0;i<16;i++) {
            int side=i%2;float u=.10f+hash(i+927)*.87f;
            float x=inset(u,side,w)-w*(.025f+hash(i+64)*.08f);if(side==1)x=w-x;
            SlimeHills.jelly(p,shape,x,h*u,w*(.008f+hash(i+76)*.016f),w*.008f,i,0x608AE7AF);
        }
    }
    static float inset(float u,int side,float w) {
        return w*(.045f+.18f*bump(u,.02f,.23f)+.19f*bump(u,side==0?.49f:.43f,.15f)
                +.20f*bump(u,side==0?.94f:.85f,.24f)+.025f*(float)Math.sin(u*19+side));
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
