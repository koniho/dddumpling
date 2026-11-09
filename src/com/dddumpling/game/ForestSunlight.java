package com.dddumpling.game;

/** Warm canopy openings travel with the ground, below animals, plants, and prompts. */
final class ForestSunlight extends Draw {
    private ForestSunlight() {}
    static void draw(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        float travel=ForestTerrain.travel(c,L),step=.94f;
        int lo=ForestTerrain.firstRow(travel,L,step),hi=ForestTerrain.lastRow(travel,L,step)+1;
        float[] ray=new float[8],patch=new float[24];
        for(int row=lo;row<=hi;row++) {
            int seed=6151+row*239;
            float z=(row+.25f+hash(seed)*.45f)*step,y=ForestTerrain.screenY(z,travel,L);
            if(y<-L.w*.15f || y>L.deckTop+L.w*.68f)continue;
            float x=L.w*(Math.floorMod(row,2)==0?ForestTerrain.pathX(z):ForestTerrain.streamX(z));
            float gust=ForestTerrain.gust(c,L,x,y),shift=gust*L.w*.018f;
            float light=fade*(.82f+hash(seed+7)*.18f)*(1+gust*.18f);
            // Overlapping faint ribbons feather the sides; each ribbon also fades at both ends.
            for(int beam=0;beam<2;beam++) {
                float endX=x+(beam==0?-.035f:.055f)*L.w+shift,endY=y+beam*L.w*.055f;
                float startX=x-L.w*(.27f+hash(seed+11)*.09f)+shift*.25f,startY=y-L.w*.62f;
                float width=L.w*(beam==0?.038f:.024f);
                for(int layer=2;layer>=0;layer--)for(int segment=0;segment<8;segment++) {
                    float a=segment/8f,b=(segment+1)/8f;
                    float envelope=(float)Math.sin((a+b)*.5f*Math.PI);
                    int alpha=(int)(light*envelope*(layer==2?5:layer==1?7:10));
                    if(alpha<=0)continue;
                    float wa=width*(.10f+a)*(1+layer*.40f),wb=width*(.10f+b)*(1+layer*.40f);
                    float ax=startX+(endX-startX)*a,ay=startY+(endY-startY)*a;
                    float bx=startX+(endX-startX)*b,by=startY+(endY-startY)*b;
                    ray[0]=ax-wa;ray[1]=ay;ray[2]=ax+wa;ray[3]=ay;
                    ray[4]=bx+wb;ray[5]=by;ray[6]=bx-wb;ray[7]=by;
                    p.fillPoly(ray,Glyph.withAlpha(0xFFF3DCA0,alpha));
                }
            }
            for(int spot=0;spot<5;spot++) {
                int n=seed+spot*53;
                float xx=x+(hash(n+17)-.5f)*L.w*.17f+shift;
                float yy=y+(hash(n+19)-.5f)*L.w*.13f;
                float size=L.w*(.022f+hash(n+23)*.032f);
                for(int layer=2;layer>=0;layer--) {
                    float radius=size*(1+layer*.24f);
                    for(int j=0;j<12;j++) {
                        float angle=j*Softbody.TAU/12,reach=radius*(.72f+hash(n+j*31)*.28f);
                        patch[j*2]=xx+(float)Math.cos(angle)*reach;
                        patch[j*2+1]=yy+(float)Math.sin(angle)*reach*.58f;
                    }
                    p.fillPoly(patch,Glyph.withAlpha(0xFFEAD79A,(int)(light*(layer==2?4:layer==1?6:9))));
                }
            }
        }
    }
}
