package com.dddumpling.game;

/** Short, directional combo ribbons. Older swipes retain their own direction as the hand turns. */
final class NinjaSlashes extends Draw {
    static final int CAPACITY = 4;
    private static final int[] COLORS = {0xFFFFD8A6,0xFFFFAFCF,0xFFAAF3E4,0xFFD4BEFF,0xFFFFF1BA};
    final float[] age=new float[CAPACITY], life=new float[CAPACITY];
    final float[] x=new float[CAPACITY], y=new float[CAPACITY];
    final float[] dx=new float[CAPACITY], dy=new float[CAPACITY];
    final int[] combo=new int[CAPACITY];
    int next;

    static float strength(int count) { return Math.max(0f,Math.min(1f,(count-1)/9f)); }
    static int ribbons(int count) { return 1+Math.min(4,Math.max(0,(count-1)/2)); }

    void clear() {
        for(int i=0;i<CAPACITY;i++)life[i]=0f;
        next=0;
    }

    void emit(float x0,float y0,float x1,float y1,int count) {
        float vx=x1-x0,vy=y1-y0,len=(float)Math.sqrt(vx*vx+vy*vy);
        if(len<.001f || count<=0)return;
        int at=next;next=(next+1)%CAPACITY;
        x[at]=(x0+x1)*.5f;y[at]=(y0+y1)*.5f;
        dx[at]=vx/len;dy[at]=vy/len;
        combo[at]=count;age[at]=0f;life[at]=.30f+.18f*strength(count);
    }

    void update(float dt) {
        for(int i=0;i<CAPACITY;i++)if(life[i]>0f) {
            age[i]+=dt;
            if(age[i]>=life[i])life[i]=0f;
        }
    }

    void draw(Painter p,Layout L) {
        for(int k=0;k<CAPACITY;k++) {
            int at=(next+k)%CAPACITY;
            if(life[at]<=0f)continue;
            float power=strength(combo[at]),progress=age[at]/life[at];
            float vx=dx[at],vy=dy[at],nx=-vy,ny=vx;
            for(int i=0;i<ribbons(combo[at]);i++) {
                float t=progress-i*.035f;
                if(t<0f || t>=1f)continue;
                float side=i==0?0f:((i+1)/2)*(i%2==1?1f:-1f);
                float spread=L.unit*(1.1f+power*.8f)*side;
                float cx=x[at]+nx*spread,cy=y[at]+ny*spread;
                // Project all four viewport corners so even steep swipes cross the whole screen.
                float base=-cx*vx-cy*vy;
                float start=base+Math.min(0f,L.w*vx)+Math.min(0f,L.h*vy)-L.unit*3f;
                float end=base+Math.max(0f,L.w*vx)+Math.max(0f,L.h*vy)+L.unit*3f;
                float span=end-start;
                float head=start+span*(t*2.1f);
                float tail=head-span*(.72f+.18f*power);
                float fade=Math.min(1f,t/.075f)*(1f-t)*(1f-t);
                float width=L.unit*(.10f+.35f*power)*(1f-i*.10f);
                int hue=COLORS[(Math.min(combo[at],10)+i)%COLORS.length];
                ribbon(p,cx,cy,vx,vy,tail,head,width*4f,fadeBy(Glyph.withAlpha(hue,36),fade));
                ribbon(p,cx,cy,vx,vy,tail,head,width*1.8f,fadeBy(Glyph.withAlpha(hue,105),fade));
                ribbon(p,cx,cy,vx,vy,tail,head,width,fadeBy(Glyph.withAlpha(0xFFFFFAEB,165+(int)(60*power)),fade));
            }
        }
    }

    private static void ribbon(Painter p,float x,float y,float dx,float dy,
            float tail,float head,float width,int color) {
        float shoulder=tail+(head-tail)*.64f,nx=-dy*width,ny=dx*width;
        p.fillPoly(new float[]{x+dx*tail,y+dy*tail,
                x+dx*shoulder+nx,y+dy*shoulder+ny,
                x+dx*head,y+dy*head,
                x+dx*shoulder-nx,y+dy*shoulder-ny},color);
    }
}
