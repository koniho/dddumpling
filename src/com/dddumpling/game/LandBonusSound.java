package com.dddumpling.game;

import java.util.Random;

/** Short toy-like foley: bubbles, glass, pearl, chirps, feathers, and a coin flourish. */
final class LandBonusSound {
    static float[] render(int kind) {
        float[] pcm=new float[(int)(Sfx.RATE*.62f)];
        Random random=new Random(244+kind);
        float[][] notes={{460,650,870},{1320,1760,2217},{740,1110,1480},
                {1200,1550,1320},{620,850,720},{1046,1318,1568,2093}};
        float step=kind==3?.105f:kind==5?.075f:.09f;
        float duration=kind==0?.17f:kind==3?.12f:kind==4?.22f:.36f;
        for(int n=0;n<notes[kind].length;n++) {
            float phase=0;int start=(int)(n*step*Sfx.RATE),length=(int)(duration*Sfx.RATE);
            for(int i=0;i<length && start+i<pcm.length;i++) {
                float u=(float)i/length,t=(float)i/Sfx.RATE;
                float pitch=kind==0?2.1f-1.4f*u:kind==2?1+.25f*(float)Math.exp(-u*15)
                        :kind==3?1+.40f*(float)Math.sin(u*Math.PI):kind==4?1+.16f*(float)Math.sin(u*Math.PI):1;
                phase+=Softbody.TAU*notes[kind][n]*pitch/Sfx.RATE;
                float wave=(float)Math.sin(phase);
                if(kind==1 || kind==5)wave+=.35f*(float)Math.sin(phase*2.76f)*(float)Math.exp(-u*7);
                if(kind==2)wave+=.20f*(float)Math.sin(phase*3);
                if(kind==3)wave+=.12f*(float)Math.sin(phase*2);
                if(kind==4)wave=wave*.70f+(random.nextFloat()*2-1)*.20f*(float)Math.pow(Math.sin(t*70),2);
                float envelope=Math.min(1,t/.005f)*(float)Math.exp(-u*(kind==3?2:5))*(1-u)*(1-u);
                pcm[start+i]+=wave*envelope*(1-n*.09f);
            }
        }
        return pcm;
    }
}
