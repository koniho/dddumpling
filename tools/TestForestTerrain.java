package com.dddumpling.game;

final class TestForestTerrain extends Check {
    static void all(Layout L) {
        group("continuous forest terrain");
        boolean dryEdges=true,inside=true,separate=true;
        for(int i=-10000;i<=10000;i++) {
            float z=i*.031f,s=ForestTerrain.streamX(z),sw=ForestTerrain.streamWidth(z);
            float x=ForestTerrain.pathX(z),w=ForestTerrain.pathWidth(z),bank=ForestTerrain.streamBankWidth(z);
            dryEdges&=sw>.03f && s-bank>.40f && s+bank<.81f;
            separate&=s-bank-(x+w*1.5f)>.03f;
            inside&=w>0 && x-w*1.5f>0 && x+w*1.5f<1;
        }
        check("wide channel and recessed banks stay within the forest corridor",dryEdges);
        check("trail remains inside the forest",inside);
        check("stream banks never overlap the trail or its soft edges",separate);
        GameCore c=TestLandBonuses.start(L,16,true,246);
        GameCore twin=TestLandBonuses.start(L,16,true,246);
        c.skyClock=0;Probe first=draw(c,L,false);
        c.skyClock=8;Probe moved=draw(c,L,false);
        float dy=8*ForestTerrain.speed(L);int shared=0;boolean aligned=true;
        for(int i=0;i<first.trail.length/2;i+=2) {
            for(int j=0;j<moved.trail.length/2;j+=2) {
                if(Math.abs(first.trail[i+1]+dy-moved.trail[j+1])<.01f) {
                    shared++;aligned&=Math.abs(first.trail[i]-moved.trail[j])<.01f;
                }
            }
        }
        check("world vertices scroll continuously without rerolling",shared>10 && aligned);
        check("ground extends behind the header and beyond the deck",first.trail[1]<0
                && first.trail[first.trail.length/2-1]>L.deckTop);
        check("log and terrain share scroll speed",LandBonus.scrollSpeed(c,L)==ForestTerrain.speed(L));
        check("drawing leaves gameplay randomness unchanged",c.rnd.nextLong()==twin.rnd.nextLong());
        c.cloudUi.reduceMotion=true;c.clock=0;Probe still=draw(c,L,false);
        c.clock=23;Probe later=draw(c,L,false);
        check("reduced motion freezes water glints and plant sway",still.hash==later.hash);
        c.cloudUi.reduceMotion=false;
        check("normal motion animates water and foliage",draw(c,L,false).hash!=still.hash);
        Probe cached=draw(c,L,true);
        check("native foliage cache replaces vector leaves",cached.stamps>0 && cached.calls<later.calls/2);
        Probe invisible=new Probe(false);
        ForestTerrain.ground(invisible,c,L,0);ForestTerrain.foliage(invisible,c,L,0);
        check("hidden terrain does no drawing",invisible.calls==0);
        c=TestLandBonuses.start(L,20,false,246);c.boss.intro=0;
        Probe grove=new Probe(false);MushroomForest.undergrowth(grove,c,L,1);
        check("trail clears when the boss grove takes over",grove.trail==null);
    }
    private static Probe draw(GameCore c,Layout L,boolean cached) {
        Probe p=new Probe(cached);ForestTerrain.ground(p,c,L,1);ForestTerrain.foliage(p,c,L,1);return p;
    }
    private static final class Probe extends OpacityPainter {
        final boolean cached;int calls,stamps;long hash=1;float[] trail;
        Probe(boolean cached){super(null,1);this.cached=cached;}
        private void record(int color,float... values) {
            calls++;hash=31*hash+color;for(float v:values)hash=31*hash+Float.floatToIntBits(v);
        }
        public ArtStamp stamps(){return cached?this:null;}
        public boolean forest(int art,float x,float y,float r,float lean,int tint,int alpha) {
            stamps++;record(tint,art,x,y,r,lean,alpha);return true;
        }
        public void fillPoly(float[] xy,int color) {
            if((color&0xFFFFFF)==0x766A50)trail=xy.clone();record(color,xy);
        }
        public void polyline(float[] xy,int color,float width){record(color,xy);hash=31*hash+Float.floatToIntBits(width);}
        public void line(float x,float y,float a,float b,int color,float width){record(color,x,y,a,b,width);}
        public void fillEllipse(float x,float y,float rx,float ry,int color){record(color,x,y,rx,ry);}
        public void fillCircle(float x,float y,float r,int color){record(color,x,y,r);}
    }
}
