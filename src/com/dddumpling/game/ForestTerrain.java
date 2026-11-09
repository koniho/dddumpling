package com.dddumpling.game;

/** Continuous woodland ground in world coordinates; detail follows the same scrolling banks. */
final class ForestTerrain extends Draw {
    static final float ROW=.05f,CLUSTER=.34f;
    private ForestTerrain() {}
    static float speed(Layout L) {return L.h*.028f;}
    static float travel(GameCore c,Layout L) {return c.skyClock*speed(L)+MushroomForest.groveProgress(c)*L.h*.72f;}
    static float pathX(float z) {return .205f+.04f*(float)Math.sin(z*1.7f-.5f)+.006f*(float)Math.sin(z*4.1f+.3f);}
    static float pathWidth(float z) {return (.058f+.009f*(float)Math.sin(z*2.1f+1))*(1+.14f*(float)Math.sin(z*8.3f)+.055f*(float)Math.sin(z*19));}
    static float streamX(float z) {return .60f+.105f*(float)Math.sin(z*3.7f+1.4f)+.010f*(float)Math.sin(z*7.1f-.8f);}
    static float streamWidth(float z) {return .040f+.006f*(float)Math.sin(z*3.1f+.7f)+.003f*(float)Math.sin(z*7.7f);}
    static float streamBankWidth(float z) {return streamWidth(z)+.033f;}
    static float screenY(float z,float travel,Layout L) {return z*L.w+travel;}
    static int firstRow(float travel,Layout L,float step) {return (int)Math.floor((-travel/L.w-.4f)/step);}
    static int lastRow(float travel,Layout L,float step) {return (int)Math.ceil(((L.deckTop-travel)/L.w+.4f)/step);}

    static void ground(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        float travel=travel(c,L);
        int first=firstRow(travel,L,ROW),last=lastRow(travel,L,ROW);
        float[] ribbon=new float[(last-first+1)*4];
        moss(p,L,travel,fade);
        // Feather moss into the trail; a lighter worn middle follows the same bends.
        band(p,L,travel,first,last,ribbon,false,1.50f,0xFF647354,fade*.10f);
        band(p,L,travel,first,last,ribbon,false,1.24f,0xFF69714F,fade*.18f);
        band(p,L,travel,first,last,ribbon,false,1.07f,0xFF554E3D,fade*.46f);
        band(p,L,travel,first,last,ribbon,false,.97f,0xFF766A50,fade*.44f);
        band(p,L,travel,first,last,ribbon,false,.73f,0xFFAA946B,fade*.06f);
        pathTexture(p,L,travel,fade);
        // A raised grassy lip drops into shaded earth, then a dark seam at the waterline.
        streamStrip(p,L,travel,first,last,ribbon,-1,.033f,1,.033f,0,0,0xFF354B32,fade*.28f);
        streamStrip(p,L,travel,first,last,ribbon,-1,.026f,1,.026f,0,0,0xFF162F29,fade*.60f);
        streamStrip(p,L,travel,first,last,ribbon,-1,.028f,-1,.023f,-.025f,-.020f,0xFFA4AA75,fade*.42f);
        streamStrip(p,L,travel,first,last,ribbon,-1,.023f,-1,.002f,-.020f,0,0xFF665B3E,fade*.78f);
        streamStrip(p,L,travel,first,last,ribbon,1,.002f,1,.025f,0,-.020f,0xFF8D8960,fade*.68f);
        streamStrip(p,L,travel,first,last,ribbon,1,.025f,1,.029f,-.020f,-.025f,0xFF819563,fade*.46f);
        band(p,L,travel,first,last,ribbon,true,1,0xFF3E7970,fade*.74f);
        band(p,L,travel,first,last,ribbon,true,.55f,0xFF76A896,fade*.14f);
        streamStrip(p,L,travel,first,last,ribbon,-1,.001f,-.30f,0,0,0,0xFF122F2C,fade*.24f);
        streamStrip(p,L,travel,first,last,ribbon,-1,.001f,-.73f,0,0,0,0xFF122F2C,fade*.30f);
        streamStrip(p,L,travel,first,last,ribbon,.88f,0,1,0,0,0,0xFFABC4A0,fade*.23f);
        water(p,c,L,travel,fade);
        float[] rock=new float[16],leaves=new float[66];
        int lo=firstRow(travel,L,CLUSTER),hi=lastRow(travel,L,CLUSTER);
        for(int row=lo;row<=hi;row++)for(int side=-1;side<=1;side+=2) {
            int seed=7919+row*137+side*41;
            float z=(row+.10f+.78f*hash(seed))*CLUSTER;
            boolean wet=hash(seed+11)>.45f;
            float center=wet?streamX(z):pathX(z),width=wet?streamBankWidth(z):pathWidth(z);
            float bank=center+side*(width+.013f+hash(seed+3)*.016f);
            float y=screenY(z,travel,L);
            if(y<-L.w*.16f || y>L.deckTop+L.w*.16f)continue;
            for(int i=0;i<(wet?3:2);i++) {
                float r=L.w*(.010f+hash(seed+i*31+7)*(wet?.017f:.026f));
                float x=L.w*bank+side*i*r*.95f,yy=y+(hash(seed+i*53+19)-.5f)*L.w*.085f;
                stone(p,rock,x,yy,r,seed+i*17,fade*(wet?.67f:.52f));
            }
            if(!wet)MushroomForest.litter(p,leaves,L.w*(bank-side*.035f),y-L.w*.05f,
                    L.w*(.018f+hash(seed+37)*.015f),Math.floorMod(seed,10000),(int)(fade*58));
        }
    }
    private static void pathTexture(Painter p,Layout L,float travel,float fade) {
        float step=.115f;
        int lo=firstRow(travel,L,step),hi=lastRow(travel,L,step);
        float[] patch=new float[12],leaf=new float[66];
        for(int row=lo;row<=hi;row++) {
            int seed=row*173+4319;
            for(int i=0;i<3;i++) {
                int n=seed+i*47;
                float z=(row+hash(n))*step,half=pathWidth(z)*L.w;
                float x=pathX(z)*L.w+(hash(n+3)-.5f)*half*1.25f,y=screenY(z,travel,L);
                float rx=half*(.14f+hash(n+5)*.18f),ry=L.w*(.007f+hash(n+7)*.021f);
                for(int j=0;j<6;j++) {
                    float angle=j*Softbody.TAU/6,rough=.7f+hash(n+j*19)*.3f;
                    patch[j*2]=x+(float)Math.cos(angle)*rx*rough;
                    patch[j*2+1]=y+(float)Math.sin(angle)*ry*rough;
                }
                p.fillPoly(patch,Glyph.withAlpha(i==0?0xFFC0A47A:0xFF514C39,(int)(fade*(i==0?34:27))));
                if(i==1) {
                    float r=L.w*(.0025f+hash(n+13)*.0035f);
                    p.fillEllipse(x,y,r*1.3f,r*.8f,Glyph.withAlpha(0xFF484C3D,(int)(fade*100)));
                    p.fillEllipse(x-r*.15f,y-r*.25f,r*.8f,r*.45f,Glyph.withAlpha(0xFFB0A58A,(int)(fade*100)));
                }
            }
            if(hash(seed+31)>.68f) {
                float z=(row+hash(seed+37))*step;
                float x=L.w*(pathX(z)+(hash(seed+41)-.5f)*pathWidth(z)*1.35f);
                MushroomForest.litter(p,leaf,x,screenY(z,travel,L),L.w*(.008f+hash(seed+43)*.009f),
                        Math.floorMod(seed,10000),(int)(fade*75));
            }
        }
    }
    private static void moss(Painter p,Layout L,float travel,float fade) {
        int lo=firstRow(travel,L,.53f),hi=lastRow(travel,L,.53f);
        float[] patch=new float[32];
        for(int row=lo;row<=hi;row++)for(int side=-1;side<=1;side+=2) {
            int seed=row*193+side*61+5197;
            float z=(row+hash(seed))*.53f,x=L.w*(side<0?.17f:.83f),y=screenY(z,travel,L);
            float r=L.w*(.15f+hash(seed+9)*.13f);
            for(int i=0;i<16;i++) {
                float angle=i*Softbody.TAU/16,reach=r*(.75f+hash(seed+i*37)*.4f);
                patch[i*2]=x+(float)Math.cos(angle)*reach;patch[i*2+1]=y+(float)Math.sin(angle)*reach*.85f;
            }
            p.fillPoly(patch,Glyph.withAlpha(row%2==0?0xFF526D48:0xFF3F6855,(int)(fade*32)));
        }
    }
    private static void band(Painter p,Layout L,float travel,int first,int last,float[] ribbon,boolean water,float width,int color,float fade) {
        int count=last-first+1;
        for(int i=0;i<count;i++) {
            float z=(first+i)*ROW,cx=water?streamX(z):pathX(z);
            float half=(water?streamWidth(z):pathWidth(z))*width;
            float y=screenY(z,travel,L);
            ribbon[i*2]=L.w*(cx-half);ribbon[i*2+1]=y;
            ribbon[(count*2-1-i)*2]=L.w*(cx+half);ribbon[(count*2-1-i)*2+1]=y;
        }
        p.fillPoly(ribbon,Glyph.withAlpha(color,(int)(255*fade)));
    }
    private static void streamStrip(Painter p,Layout L,float travel,int first,int last,float[] shape,
            float left,float leftExtra,float right,float rightExtra,float leftLift,float rightLift,int color,float fade) {
        int count=last-first+1;
        for(int i=0;i<count;i++) {
            float z=(first+i)*ROW,cx=streamX(z),width=streamWidth(z),y=screenY(z,travel,L);
            float bank=.82f+.12f*(float)Math.sin(z*5.3f)+.06f*(float)Math.sin(z*17);
            shape[i*2]=L.w*(cx+left*width+Math.signum(left)*leftExtra*bank);
            shape[i*2+1]=y+L.w*leftLift;
            int back=(count*2-1-i)*2;
            shape[back]=L.w*(cx+right*width+Math.signum(right)*rightExtra*bank);
            shape[back+1]=y+L.w*rightLift;
        }
        p.fillPoly(shape,Glyph.withAlpha(color,(int)(255*fade)));
    }
    private static void water(Painter p,GameCore c,Layout L,float travel,float fade) {
        float flow=c.cloudUi.reduceMotion?0:c.clock*.045f;
        int lo=firstRow(travel+flow*L.w,L,.29f),hi=lastRow(travel+flow*L.w,L,.29f);
        float[] line=new float[12];
        for(int row=lo;row<=hi;row++) {
            float z=row*.29f+flow;
            float alpha=fade*(.11f+.05f*(float)Math.sin(row*2.7f+(c.cloudUi.reduceMotion?0:c.clock*.8f)));
            for(int i=0;i<6;i++) {
                float zz=z+i*.009f;
                line[i*2]=L.w*(streamX(zz)+streamWidth(zz)*.42f*(float)Math.sin(row*1.7f+i*.4f));
                line[i*2+1]=screenY(zz,travel,L);
            }
            p.polyline(line,Glyph.withAlpha(0xFFC2D8BB,(int)(255*alpha)),L.w*.003f);
        }
    }
    static void foliage(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        float travel=travel(c,L),time=c.cloudUi.reduceMotion?0:c.clock;
        int lo=firstRow(travel,L,CLUSTER),hi=lastRow(travel,L,CLUSTER);
        float[] shape=new float[66];
        for(int row=lo;row<=hi;row++)for(int side=-1;side<=1;side+=2) {
            int seed=7919+row*137+side*41;
            float z=(row+.10f+.78f*hash(seed))*CLUSTER;
            boolean wet=hash(seed+11)>.45f;
            float center=wet?streamX(z):pathX(z),width=wet?streamBankWidth(z):pathWidth(z);
            float bank=center+side*(width+.045f+hash(seed+3)*.025f),y=screenY(z,travel,L);
            if(y<-L.w*.16f || y>L.deckTop+L.w*.16f)continue;
            for(int i=0;i<2;i++) {
                float r=L.w*(.035f+hash(seed+i*23+5)*.035f);
                float x=L.w*bank+side*i*r*.80f,yy=y+i*r*.30f;
                float lean=side*(.48f+.35f*hash(seed+i*37+8))+.04f*(float)Math.sin(time*.65f+seed);
                int color=wet?0xFF729C82:i==0?0xFF81976D:0xFFA1AA7E;
                MushroomForest.sprig(p,shape,x,yy,r,lean,Math.floorMod(row+i,3),(int)(fade*(wet?82:96)),color);
            }
            if(hash(seed+33)>.58f)for(int i=0;i<2;i++) {
                float r=L.w*(.010f+hash(seed+i*11+2)*.006f);
                float x=L.w*(bank+side*.025f)+i*r*1.4f,yy=y+r*(i-1)*1.6f;
                p.fillEllipse(x+r*.15f,yy+r*.25f,r*1.1f,r*.8f,Glyph.withAlpha(0xFF102E25,(int)(fade*60)));
                p.fillEllipse(x,yy,r,r*.88f,Glyph.withAlpha(i==0?0xFFAA8D7A:0xFFB7B597,(int)(fade*110)));
                p.fillEllipse(x-r*.20f,yy-r*.25f,r*.45f,r*.30f,Glyph.withAlpha(0xFFE1D8B6,(int)(fade*40)));
                p.fillCircle(x+r*.3f,yy-r*.07f,r*.12f,Glyph.withAlpha(0xFFDFD8B9,(int)(fade*110)));
            }
        }
    }
    private static void stone(Painter p,float[] shape,float x,float y,float r,int seed,float fade) {
        if(fade<=0)return;
        p.fillEllipse(x+r*.12f,y+r*.25f,r*1.12f,r*.82f,Glyph.withAlpha(0xFF132D26,(int)(fade*100)));
        for(int i=0;i<8;i++) {
            float a=i*Softbody.TAU/8,rough=.86f+.18f*hash(seed+i*29);
            shape[i*2]=x+(float)Math.cos(a)*r*rough;
            shape[i*2+1]=y+(float)Math.sin(a)*r*.79f*rough;
        }
        p.fillPoly(shape,Glyph.withAlpha(0xFF748078,(int)(fade*200)));
        p.fillPoly(new float[]{shape[0],shape[1],shape[10],shape[11],shape[12],shape[13],shape[14],shape[15],x-r*.13f,y-r*.10f},
                Glyph.withAlpha(0xFFB3B49A,(int)(fade*90)));
        p.fillEllipse(x-r*.28f,y+r*.22f,r*.50f,r*.24f,Glyph.withAlpha(0xFF788B60,(int)(fade*130)));
    }
}
