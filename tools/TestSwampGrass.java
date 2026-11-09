package com.dddumpling.game;

final class TestSwampGrass extends Check {
    static void all() {
        group("cached tall swamp reeds");
        check("reed coverage is about twenty times the sparse banks",SwampVista.ROWS*2*SwampVista.LANES*SwampGrass.BLADES>=6*2*2*9*20);
        boolean fits=true,tall=true,heads=false,bare=false;
        for(int art=0;art<SwampGrass.COUNT;art++)for(float sway:new float[]{-.006f,.006f}) {
            Probe p=new Probe(false);SwampGrass.paint(p,art,SwampGrass.ANCHOR_X,SwampGrass.ANCHOR_Y,SwampGrass.UNIT,sway,1);
            fits&=p.left>=0 && p.right<=SwampGrass.WIDTH && p.top>=0 && p.bottom<=SwampGrass.HEIGHT;
            tall&=(SwampGrass.ANCHOR_Y-p.top)/SwampGrass.UNIT>6;
            heads|=p.lines>0;bare|=p.lines==0;
            check("cached patch contains every blade "+art+" sway "+sway,p.polys==SwampGrass.BLADES);
        }
        check("reeds and seed heads fit native cache bounds during sway",fits);
        check("reeds grow long with occasional cattail patches",tall && heads && bare);
        Probe cached=new Probe(true);SwampGrass.draw(cached,100,200,20,17,3,1,1);
        check("a dense patch uses one native image draw",cached.stamps==1 && cached.polys==0 && cached.lines==0);
        Probe vector=new Probe(false);SwampGrass.draw(vector,100,200,20,17,3,1,1);
        check("unsupported hosts retain complete vector reeds",vector.polys==SwampGrass.BLADES && vector.lines>0);
        cached=new Probe(true);SwampGrass.draw(new MonochromePainter(new OpacityPainter(cached,1),.6f),100,200,20,17,3,1,.5f);
        check("palette wrappers preserve caching, fade and grayscale",cached.stamps==1 && Math.abs(cached.mono-.6f)<.001f && cached.alpha>0 && cached.alpha<128);
    }
    private static final class Probe extends OpacityPainter {
        final boolean cached;int stamps,polys,lines,alpha;float mono;
        float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
        Probe(boolean cached){super(null,1);this.cached=cached;}
        public ArtStamp stamps(){return cached?this:null;}
        public boolean swampGrass(int art,float x,float y,float r,float sway,int a,float m){stamps++;alpha=a;mono=m;return true;}
        void point(float x,float y,float r){left=Math.min(left,x-r);right=Math.max(right,x+r);top=Math.min(top,y-r);bottom=Math.max(bottom,y+r);}
        public void fillPoly(float[] xy,int c){polys++;for(int i=0;i<xy.length;i+=2)point(xy[i],xy[i+1],0);}
        public void line(float x,float y,float a,float b,int c,float w){lines++;point(x,y,w*.5f);point(a,b,w*.5f);}
    }
}
