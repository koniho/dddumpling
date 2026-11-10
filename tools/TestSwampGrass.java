package com.dddumpling.game;

final class TestSwampGrass extends Check {
    static void all() {
        layers();
        group("cached tall swamp reeds");
        check("reed coverage is about twenty times the sparse banks",SwampVista.ROWS*2*SwampVista.LANES*SwampGrass.BLADES>=6*2*2*9*20);
        boolean fits=true,tall=true,heads=false,bare=false,depth=true,shaded=true,varied=true;
        for(int art=0;art<SwampGrass.COUNT;art++)for(float sway:new float[]{-.006f,.006f}) {
            Probe p=new Probe(false);SwampGrass.paint(p,art,SwampGrass.ANCHOR_X,SwampGrass.ANCHOR_Y,SwampGrass.UNIT,sway,1);
            fits&=p.left>=0 && p.right<=SwampGrass.WIDTH && p.top>=0 && p.bottom<=SwampGrass.HEIGHT;
            depth&=p.baseMax-p.baseMin>SwampGrass.UNIT*.8f && p.ordered;
            shaded&=p.frontLight>p.backLight*1.12f;
            varied&=p.colors.size()>=12;
            tall&=(SwampGrass.ANCHOR_Y-p.top)/SwampGrass.UNIT>6;
            heads|=p.lines>0;bare|=p.lines==0;
            check("cached patch contains every blade "+art+" sway "+sway,p.polys==SwampGrass.BLADES);
        }
        check("each clump has layered roots instead of a flat base",depth);
        check("rear blades are shaded behind brighter foreground reeds",shaded);
        check("each cached clump contains varied blade colors",varied);
        check("reeds and seed heads fit native cache bounds during sway",fits);
        check("reeds grow long with occasional cattail patches",tall && heads && bare);
        Probe cached=new Probe(true);SwampGrass.draw(cached,100,200,20,17,3,1,1);
        check("a dense patch uses one native image draw",cached.stamps==1 && cached.polys==0 && cached.lines==0);
        Probe vector=new Probe(false);SwampGrass.draw(vector,100,200,20,17,3,1,1);
        check("unsupported hosts retain complete vector reeds",vector.polys==SwampGrass.BLADES && vector.lines>0);
        cached=new Probe(true);SwampGrass.draw(new MonochromePainter(new OpacityPainter(cached,1),.6f),100,200,20,17,3,1,.5f);
        check("palette wrappers preserve caching, fade and grayscale",cached.stamps==1 && Math.abs(cached.mono-.6f)<.001f && cached.alpha>0 && cached.alpha<128);
    }
    private static void layers() {
        group("cached swamp scenery layers");
        Layout l=new Layout();l.compute(393,852,0,0,0,0);
        boolean bounds=true;
        for(int art=0;art<SwampLayers.COUNT;art++) {
            Probe p=new Probe(false);SwampLayers.paint(p,art,l.w,l.h,l.deckTop);
            bounds&=p.left>=SwampLayers.left(art,l.w)-1 && p.right<=SwampLayers.left(art,l.w)+SwampLayers.width(art,l.w)+1
                    && p.top>=SwampLayers.top(art,l.h,l.w)-1 && p.bottom<=SwampLayers.bottom(art,l.h,l.w,l.deckTop)+1;
        }
        check("cropped scenery tiles contain every primitive",bounds);
        LayerProbe cache=new LayerProbe();
        check("opaque scenery dispatches to its native tile",SwampLayers.draw(cache,0,l,0,1) && cache.layers==1);
        check("land fades retain per-primitive alpha",!SwampLayers.draw(cache,0,l,0,.5f) && cache.layers==1);
        check("grayscale and transparent wrappers preserve tile dispatch",SwampLayers.draw(new MonochromePainter(new OpacityPainter(cache,1),.6f),1,l,0,1)
                && cache.layers==2 && Math.abs(cache.mono-.6f)<.001f);
        SwampLayers.draw(new OpacityPainter(cache,.5f),0,l,0,1);
        check("partially faded wrappers keep the vector path",cache.layers==2);
        GatorReplay replay=new GatorReplay(Boss.ALLIGATOR,false,false,true),chunked=new GatorReplay(Boss.ALLIGATOR,false,false,true);
        boolean swamp=true;
        for(int frame=0;frame<GatorReplay.LENGTH;frame++) {
            replay.step(l);if(frame%2==1)chunked.update(GatorReplay.STEP*2,l);
            swamp&=replay.core.stage==21 && !replay.core.boss.active() && replay.core.state==GameCore.PLAY;
        }
        check("swamp benchmark stays in scenery for the full isolated loop",swamp);
        check("swamp benchmark is independent of display frame rate",replay.core.skyClock==chunked.core.skyClock && replay.core.rnd.nextLong()==chunked.core.rnd.nextLong());
        replay.step(l);check("swamp benchmark restarts cleanly",replay.loops==1 && replay.tick==1);
        GameCore c=TestLandBonuses.start(l,21,false,248);c.skyClock=18;
        c.swampGeometry.prepare(c,l);boolean river=true;
        for(int side=-1;side<=1;side+=2)for(int i=0;i<=48;i++) {
            float d=c.swampGeometry.depth[i];
            river&=c.swampGeometry.edge[(side+1)/2][i]==l.w*SwampVista.waterEdgeX(l,c.skyClock,d,side);
        }
        check("shared shoreline samples retain the original river projection",river);
    }
    private static final class LayerProbe extends OpacityPainter {
        int layers;float mono;
        LayerProbe(){super(null,1);}
        public ArtStamp stamps(){return this;}
        public boolean swampLayer(int art,float w,float h,float deck,float shift,int alpha,float mono){layers++;this.mono=mono;return true;}
    }
    private static final class Probe extends OpacityPainter {
        float baseMin=Float.MAX_VALUE,baseMax=-Float.MAX_VALUE,previousBase=-Float.MAX_VALUE;boolean ordered=true;
        final java.util.Set<Integer> colors=new java.util.HashSet<>();float backLight,frontLight;
        final boolean cached;int stamps,polys,lines,alpha;float mono;
        float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
        Probe(boolean cached){super(null,1);this.cached=cached;}
        public ArtStamp stamps(){return cached?this:null;}
        public boolean swampGrass(int art,float x,float y,float r,float sway,int a,float m){stamps++;alpha=a;mono=m;return true;}
        void point(float x,float y,float r){left=Math.min(left,x-r);right=Math.max(right,x+r);top=Math.min(top,y-r);bottom=Math.max(bottom,y+r);}
        public void fillPoly(float[] xy,int c){
            colors.add(c);float light=((c>>16)&255)*.2126f+((c>>8)&255)*.7152f+(c&255)*.0722f;
            if(polys<SwampGrass.BLADES/3)backLight+=light;
            if(polys>=SwampGrass.BLADES*2/3)frontLight+=light;
            polys++;baseMin=Math.min(baseMin,xy[1]);baseMax=Math.max(baseMax,xy[1]);ordered&=xy[1]>=previousBase;previousBase=xy[1];for(int i=0;i<xy.length;i+=2)point(xy[i],xy[i+1],0);}
        public void line(float x,float y,float a,float b,int c,float w){lines++;point(x,y,w*.5f);point(a,b,w*.5f);}
        public void fillRect(float l,float t,float r,float b,int c){point(l,t,0);point(r,b,0);}
        public void fillCircle(float x,float y,float r,int c){point(x,y,r);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){point(x-rx,y-ry,0);point(x+rx,y+ry,0);}
    }
}
