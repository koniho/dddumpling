package com.dddumpling.game;

import java.io.File;

/** Bathtub and Bubble Rider art studies; deliberately outside shipped gameplay. */
final class DuckLandPreview extends Draw {
    private static final int WATER=0xFF479FAD;
    private static float[] rounded(float x,float y,float rx,float ry) {
        float[] shape=new float[128];
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32;float c=(float)Math.cos(a),s=(float)Math.sin(a);
            shape[2*i]=x+rx*Math.copySign((float)Math.pow(Math.abs(c),.45),c);
            shape[2*i+1]=y+ry*Math.copySign((float)Math.pow(Math.abs(s),.45),s);
        }
        return shape;
    }
    private static void soap(Painter p,float x,float y,float r,int color,float t) {
        y+=(float)Math.sin(t)*r*.15f;
        p.fillEllipse(x,y+r*.55f,r*1.2f,r*.45f,0x22323B67);
        p.fillPoly(pill(x,y+r*.12f,r,r*.52f,14),Glyph.mix(color,0xFF615279,.35f));
        p.fillPoly(pill(x,y-r*.10f,r,r*.48f,14),color);
        p.fillPoly(pill(x-r*.1f,y-r*.24f,r*.72f,r*.13f,12),0x77FFF7E8);
        p.strokePoly(pill(x,y-r*.05f,r*.43f,r*.17f,12),0x557B587A,r*.045f);
    }
    private static void bubble(Painter p,float x,float y,float rx,float ry,float t) {
        p.fillEllipse(x,y,rx,ry,0x1BD9FFFA);
        p.fillEllipse(x+rx*.08f,y+ry*.1f,rx*.84f,ry*.82f,0x145DC7CB);
        p.arc(x,y,rx,ry,0,360,0x94DAF9E9,rx*.025f);
        p.arc(x,y,rx*.97f,ry*.97f,195,83,0xBAFFBFDF,rx*.045f);
        p.arc(x,y,rx*.96f,ry*.96f,15,73,0xAEE9EB96,rx*.04f);
        p.arc(x,y,rx*.86f,ry*.86f,212,42,0xE6FFFFFF,rx*.065f);
        p.fillEllipse(x-rx*.38f,y-ry*.58f,rx*.08f,ry*.055f,0xDFFFFFFF);
        p.arc(x,y,rx*.72f,ry*.72f,48,28,0x60FFFFFF,rx*.025f);
    }
    private static void scenery(Painter p,Layout L,float t) {
        float w=L.w,h=L.h;
        p.fillRect(0,0,w,h,0xFF625B7D);
        for(int row=0;row<6;row++)for(int col=0;col<5;col++) {
            float x=(col+.5f)*w/5,y=(row+.5f)*h*.045f;
            int tile=(row+col)%3==0?0xFFB1BDCE:0xFFCADACF;
            p.fillPoly(rounded(x,y,w*.097f,h*.021f),tile);
            p.line(x-w*.077f,y-h*.012f,x+w*.06f,y-h*.012f,0x57FFFFFF,w*.003f);
        }
        p.fillPoly(rounded(w*.5f,h*.508f,w*.493f,h*.343f),0x445A405B);
        p.fillPoly(rounded(w*.5f,h*.493f,w*.49f,h*.342f),0xFF9392B3);
        p.fillPoly(rounded(w*.5f,h*.477f,w*.48f,h*.332f),0xFFFFF1DF);
        p.fillPoly(rounded(w*.5f,h*.469f,w*.466f,h*.318f),0xBFFFFFF4);
        p.fillPoly(rounded(w*.5f,h*.476f,w*.443f,h*.305f),0xFFBED8D0);
        p.fillPoly(rounded(w*.5f,h*.476f,w*.433f,h*.299f),0xFF397381);
        p.fillPoly(rounded(w*.5f,h*.465f,w*.423f,h*.285f),WATER);
        p.fillPoly(rounded(w*.5f,h*.446f,w*.402f,h*.26f),0x2859CFBD);
        // Slow shifting light under the water, independent of the anchored tub frame.
        for(int i=0;i<5;i++) {
            float[] ribbon=new float[42];
            for(int n=0;n<=20;n++) {
                float u=n/20f;
                ribbon[n*2]=w*(.19f+u*.62f);
                ribbon[n*2+1]=h*(.25f+i*.09f+.012f*(float)Math.sin(u*8+t*.7f+i));
            }
            p.polyline(ribbon,0x1068EEC1,w*.016f);
        }
        for(int i=0;i<15;i++) {
            float y=h*(.22f+(i*.035f+t*.012f)%.50f),x=w*(.3f+.36f*hash(i*21));
            p.arc(x,y,w*(.12f+.08f*hash(i*31)),h*.013f,12,153,
                    i%2==0?0x377AF0D0:0x2262D6DE,w*.004f);
        }
        // Foam stays at the porcelain banks, keeping the prompt lane clear.
        for(int side=-1;side<=1;side+=2)for(int i=0;i<13;i++) {
            float y=h*(.21f+i*.041f),x=w*(side<0?.10f:.90f)+w*.017f*(float)Math.sin(i*2.1f+t*.5f);
            float r=w*(.022f+.025f*hash(i*91+side));
            p.fillCircle(x,y+r*.15f,r*1.15f,0x557ACBCB);
            p.fillCircle(x,y,r,0xB8DBEEE8);
            p.fillCircle(x-r*.16f,y-r*.2f,r*.7f,0xB3FFFFED);
            p.fillCircle(x+side*r*.7f,y+r*.65f,r*.54f,0xCBF1F8E8);
            p.fillCircle(x-side*r*.12f,y-r*.38f,r*.23f,0xC9FFFFFF);
        }
        soap(p,w*.17f,h*(.23f+(.37f+t*.010f)%.46f),w*.075f,0xFFE6A4C7,t);
        soap(p,w*.83f,h*(.23f+(.20f+t*.014f)%.46f),w*.07f,0xFFC5B0DD,t+2);
        // Rounded faucet silhouette with a broad shaded metal face.
        float x=w*.81f,y=h*.17f;
        p.polyline(new float[]{x,y-h*.08f,x,y-h*.025f,x-w*.11f,y-h*.025f,x-w*.11f,y},0xFF6C7892,w*.042f);
        p.polyline(new float[]{x-w*.008f,y-h*.08f,x-w*.008f,y-h*.03f,x-w*.118f,y-h*.03f},0xFFE1E7DF,w*.023f);
        p.line(x-w*.005f,y-h*.071f,x-w*.005f,y-h*.036f,0x99FFFFFF,w*.007f);
        p.fillCircle(x,y-h*.080f,w*.033f,0xFF7789A1);
        p.fillCircle(x-w*.003f,y-h*.083f,w*.027f,0xFFC7DBDC);
        p.fillCircle(x-w*.005f,y-h*.086f,w*.014f,0xFF8FB8D1);
        p.line(x-w*.044f,y-h*.080f,x+w*.044f,y-h*.080f,0xFFDAE4E6,w*.013f);
        p.fillEllipse(x-w*.11f,y,w*.027f,h*.009f,0xFFDEE8E0);
        p.fillEllipse(x-w*.11f,y+h*.002f,w*.019f,h*.004f,0xFF465F79);
        float drop=(t*.45f)%1;
        p.fillEllipse(x-w*.11f,y+h*.035f*drop,w*.007f,h*.006f,0xAADEFFF0);
        for(int i=0;i<4;i++)bubble(p,w*(.15f+.23f*i),h*(.22f+(.08f*i+t*.023f)%.48f),w*.025f,w*.028f,t+i);
    }
    private static void rider(Painter p,float x,float y,float r,float t,boolean popped) {
        for(int i=0;i<2;i++) {
            int key=i==0?1:4;float kx=x+(i-.5f)*r*.95f,kr=r*.39f;
            p.fillPoly(Glyph.hex(kx,y,kr),Glyph.COLOR[key]);
            Kawaii.draw(p,key,kx,y,kr*.79f,Glyph.COLOR[key],1,.3f);
        }
        if(!popped)bubble(p,x,y,r*1.3f,r,t);
        else for(int i=0;i<14;i++) {
            float a=i*(float)Math.PI/7,travel=r*(1.35f+.35f*hash(i*61));
            float dx=(float)Math.cos(a),dy=(float)Math.sin(a);
            p.fillEllipse(x+dx*travel,y+dy*travel,r*(.035f+.035f*hash(i*13)),r*.07f,
                    i%2==0?0xB8FFF2BF:0xAADDFFED);
            if(i%3==0)p.arc(x,y,r*1.45f,r*1.15f,i*360f/14,13,0x79FFDCED,r*.035f);
        }
    }
    public static void main(String[] args) throws Exception {
        int w=args.length>0?Integer.parseInt(args[0]):393,h=args.length>1?Integer.parseInt(args[1]):852;
        File dir=new File(args.length>2?args[2]:"out/duck-land");dir.mkdirs();
        Layout L=new Layout();L.compute(w,h,0,0,0,0);
        GameCore c=new GameCore(new Check.Mem(),218);c.startGame();c.onboarding.skip(c);
        for(int scene=0;scene<4;scene++) {
            RasterPainter p=new RasterPainter(w,h,2);RasterPainter.clearFit();scenery(p,L,scene==3?9f:1.7f);
            p.text(scene==2?"SIR WADDLES":"SUDS SPRINGS",w*.5f,h*.10f,type(L.unit*1.05f),0xFF39495F,Painter.CENTER,true);
            p.text(scene==2?"BOSS COMPOSITION STUDY":"BATHTUB LAND STUDY",w*.5f,h*.128f,type(L.unit*.44f),0xFF39495F,Painter.CENTER,true);
            if(scene==2) {
                p.fillEllipse(w*.50f,h*.54f,w*.29f,h*.055f,0x4485EBCA);
                Duck.draw(p,Collect.DUCK_FIRST,w*.5f,h*.44f,w*.30f,2,true,1,0,0,false);
                float cx=w*.5f,cy=h*.265f,r=w*.065f;
                p.fillPoly(new float[]{cx-r,cy,cx-r*1.2f,cy-r,cx-r*.35f,cy-r*.5f,cx,cy-r*1.35f,
                        cx+r*.35f,cy-r*.5f,cx+r*1.2f,cy-r,cx+r,cy},0xC7F3C25D);
                p.line(cx-r,cy,cx+r,cy,0xAFFFF2B5,r*.18f);
                p.text("ROCK / FLIP / SQUEAK",w*.5f,h*.70f,type(L.unit*.57f),0xFFFFF6DB,Painter.CENTER,true);
            } else {
                rider(p,w*.48f,h*.35f,w*.14f,1.7f,scene==1);
                rider(p,w*.60f,h*.56f,w*.14f,3.1f,false);
                p.text(scene==1?"THEN CLEAR THE KEYS":"TAP THE BUBBLE",w*.5f,h*.71f,type(L.unit*.65f),0xFFFFF6DB,Painter.CENTER,true);
            }
            Renderer.keys(p,c,L);
            p.text("VISUAL PROTOTYPE",w*.5f,h*.975f,type(L.unit*.45f),0xFFE8DFED,Painter.CENTER,true);
            File output=new File(dir,scene==0?"suds-springs.png":scene==1?"bubble-rider-pop.png":scene==2?"sir-waddles-study.png":"suds-springs-current.png");
            Png.write(output,p.resolve(),w,h);
            if(!RasterPainter.unfit.isEmpty())throw new IllegalStateException(RasterPainter.unfit.toString());
            System.out.println(output);
        }
    }
}
