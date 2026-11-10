package com.dddumpling.game;

final class TestGradients extends Check {
    static final int BLACK=0xFF000000,WHITE=0xFFFFFFFF;
    static final float[] ENDS={0,1};
    static int red(int c){return (c>>>16)&255;}
    static void all(Layout L) {
        group("gradient geometry and composition");
        RasterPainter p=new RasterPainter(40,60,3);p.clear(BLACK);
        p.save();p.translate(3,5);p.clipRect(0,0,20,20);
        p.linearGradient(new float[]{0,0,30,0,0,30},0,0,30,0,new int[]{WHITE,0x00FFFFFF},ENDS);
        p.restore();int[] pixels=p.resolve();
        check("directional fade follows translated polygon and rectangular clip",
                red(pixels[8*40+7])>red(pixels[8*40+18]) && red(pixels[8*40+24])==0
                && red(pixels[23*40+21])==0 && red(pixels[3*40+7])==0);
        p.fillRect(30,50,40,60,WHITE);
        check("gradient restores solid paint and caller clip",p.resolve()[55*40+35]==WHITE);
        p.clear(BLACK);p.radialGradient(20,30,16,8,new int[]{WHITE,0x00FFFFFF},ENDS);pixels=p.resolve();
        check("elliptical glow keeps independent radii and transparent edge",
                red(pixels[30*40+20])>220 && Math.abs(red(pixels[30*40+28])-red(pixels[34*40+20]))<15
                && red(pixels[39*40+20])==0 && red(pixels[30*40+38])==0);
        p.clear(BLACK);p.save();p.clipOutCircle(20,30,4);
        p.radialGradient(20,30,16,8,new int[]{WHITE,WHITE},ENDS);p.restore();pixels=p.resolve();
        check("radial glow respects circular cutouts",red(pixels[30*40+20])==0 && red(pixels[30*40+28])==255);
        p.clear(BLACK);p.capsuleGradient(20,15.37f,42.69f,10,new int[]{0x80FFFFFF,0x00FFFFFF},ENDS);pixels=p.resolve();
        boolean continuous=true;int reference=red(pixels[30*40+20]);
        for(int y=14;y<=43;y++)continuous&=Math.abs(red(pixels[y*40+20])-reference)<=10;
        check("capsule has continuous fractional joins without double alpha",continuous && reference>115 && reference<=128);
        check("invalid geometry declines without drawing",!p.radialGradient(0,0,0,2,new int[]{WHITE,WHITE},ENDS)
                && !p.linearGradient(Gradients.rect(0,0,2,2),0,0,0,0,new int[]{WHITE,WHITE},ENDS));
        int[] ramp=Gradients.nested(WHITE,4,48,.19f);
        check("nested profile retains accumulated rather than single-fill opacity",(ramp[0]>>>24)>130 && (ramp[32]>>>24)==0);
        Probe probe=new Probe(true);int[] colors={0x8060A0E0,0x0060A0E0};
        Painter opacity=new OpacityPainter(probe,.5f),mono=new MonochromePainter(probe,1);
        opacity.radialGradient(0,0,2,3,colors,ENDS);
        check("opacity wrapper scales every gradient stop",(probe.colors[0]>>>24)==64 && probe.colors[1]>>>24==0);
        mono.linearGradient(Gradients.rect(0,0,2,2),0,0,2,0,colors,ENDS);
        int gray=probe.colors[0];check("monochrome preserves gradient alpha",(gray>>>24)==128 && red(gray)==((gray>>>8)&255) && red(gray)==(gray&255));
        ColorFadePainter tint=new ColorFadePainter(probe,WHITE,.5f,.75f);
        tint.capsuleGradient(1,0,2,1,colors,ENDS);
        check("tint wrapper preserves its existing color contract",probe.colors[0]==tint.color(colors[0]) && colors[0]==0x8060A0E0);
        Probe fallback=new Probe(false),nativeP=new Probe(true);
        Sky.vignette(fallback,L,WHITE,1);Sky.vignette(nativeP,L,WHITE,1);
        check("vignette reduces 128 fills to four gradient passes",fallback.fills==128 && nativeP.linear==4 && nativeP.fills==0);
        Sky.hudBacking(nativeP,L,BLACK);check("HUD uses one continuous gradient",nativeP.vertical==1);
        GameCore c=new GameCore(new Mem(),250L),twin=new GameCore(new Mem(),250L);c.startGame();twin.startGame();
        fallback=new Probe(false);nativeP=new Probe(true);ForestSunlight.draw(fallback,c,L,1);ForestSunlight.draw(nativeP,c,L,1);
        check("sunlight preserves irregular patches with fewer beam primitives",nativeP.linear>0 && nativeP.fills+nativeP.linear<fallback.fills);
        nativeP=new Probe(true);fallback=new Probe(false);RainbowLand.backdrop(nativeP,c,L,1);RainbowLand.backdrop(fallback,c,L,1);
        check("rainbow retains optional vector fallback",nativeP.capsules>0 && fallback.fills>=nativeP.capsules*8);
        nativeP=new Probe(true);nativeP.declineCapsules=true;RainbowLand.backdrop(nativeP,c,L,1);
        check("rainbow draws continuous vectors when only capsule gradients decline",
                nativeP.capsules>0 && nativeP.fills==nativeP.capsules*8);
        check("gradient rendering leaves gameplay RNG untouched",c.rnd.nextLong()==twin.rnd.nextLong());
    }
    static final class Probe extends OpacityPainter {
        final boolean supported;boolean declineCapsules;int linear,vertical,capsules,fills;int[] colors;
        Probe(boolean supported){super(null,1);this.supported=supported;}
        public boolean gradients(){return supported;}
        public boolean linearGradient(float[] p,float a,float b,float c,float d,int[] colors,float[] stops){linear++;this.colors=colors;return supported;}
        public boolean radialGradient(float x,float y,float rx,float ry,int[] colors,float[] stops){this.colors=colors;return supported;}
        public boolean capsuleGradient(float x,float t,float b,float r,int[] colors,float[] stops){capsules++;this.colors=colors;return supported && !declineCapsules;}
        public void verticalGradient(float l,float t,float r,float b,int[] colors,float[] stops){vertical++;this.colors=colors;}
        public void save(){}
        public void restore(){}
        public void clipRect(float l,float t,float r,float b){}
        public void fillRect(float l,float t,float r,float b,int c){fills++;}
        public void fillPoly(float[] p,int c){fills++;}
    }
}
