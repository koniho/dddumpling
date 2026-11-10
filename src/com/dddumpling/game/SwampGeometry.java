package com.dddumpling.game;

/** Per-game scratch space; both river-edge passes share one projection per frame. */
final class SwampGeometry {
    final float[] depth=new float[49],core=new float[120],strip=new float[196],quad=new float[8],pad=new float[26];
    final float[][] edge=new float[2][49],bankWidth=new float[2][49],waterWidth=new float[2][49],y=new float[2][49];
    void prepare(GameCore c,Layout l) {
        float horizon=SwampVista.horizon(l),range=l.deckTop-horizon;
        for(int i=0;i<=48;i++) {
            float v=i/48f,d=depth[i]=SwampVista.waterDepth(l,horizon+v*range);
            float material=3*d/(1+2*d)-c.skyClock*SwampVista.SPEED;
            for(int side=-1;side<=1;side+=2) {
                int s=(side+1)/2;float wave=(float)Math.sin(material*19+side);
                float x=edge[s][i]=l.w*SwampVista.waterEdgeX(l,c.skyClock,d,side);
                y[s][i]=SwampVista.bankY(l,d,x);
                bankWidth[s][i]=l.w*(.008f+.024f*v)*(.85f+.20f*wave);
                waterWidth[s][i]=l.w*(.010f+.060f*d)*(.9f+.1f*wave);
            }
        }
    }
}
