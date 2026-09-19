package com.dddumpling.game;

/** World-anchored debris survives the encounter; feedback is consumed once by the host. */
final class CaveEffects {
    final float[] age=new float[6], x=new float[6], y=new float[6];
    int cursor,feedback;
    float rumble,clock;
    CaveEffects(){reset();}
    void reset(){for(int i=0;i<6;i++)age[i]=2;cursor=feedback=0;rumble=clock=0;}
    void cue(GameCore c,int sound,float strength){
        rumble=Math.max(rumble,strength);feedback=Math.max(feedback,strength>.7f?2:1);
        if(c.sound!=null)c.sound.caveEvent(sound);
    }
    int takeFeedback(){int n=feedback;feedback=0;return n;}
    void update(float dt){clock+=dt;rumble=Math.max(0,rumble-dt*3.5f);for(int i=0;i<6;i++)age[i]+=dt;}
    void impact(GameCore c,Layout L,float lane){
        Cave v=c.cave;int i=cursor++%6;age[i]=0;
        x[i]=v.pathX(v.z)+(lane-.5f)*L.w/(Cave.scale(L)*v.zoom());y[i]=v.pathY(v.z);
        cue(c,Sfx.CAVE_CRASH,1);
    }
    void draw(Painter p,Cave v,Layout L){
        for(int i=0;i<6;i++){
            float t=age[i];if(t>=.65f)continue;
            float px=v.screenX(x[i],L),py=v.worldScreenY(y[i],L),s=Cave.scale(L)*v.zoom();
            int alpha=(int)(220*(1-t/.65f));
            for(int j=0;j<9;j++){
                float a=j*2.4f+i,dx=(float)Math.cos(a),dy=(float)Math.sin(a);
                p.fillEllipse(px+dx*t*s*.28f,py+dy*t*s*.10f-t*s*.035f,
                        s*(.013f+t*.055f),s*(.009f+t*.035f),Glyph.withAlpha(CaveArt.LIGHT,alpha/3));
                if(t<.48f)CaveArt.tumbling(p,px+dx*t*s*.32f,py+dy*t*s*.13f+s*(-.38f*t+.8f*t*t),
                        s*(.009f+j%3*.004f),i*71+j,alpha,a+t*(j%2==0?14:-17));
            }
        }
    }
}
