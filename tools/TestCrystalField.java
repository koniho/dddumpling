package com.dddumpling.game;

final class TestCrystalField extends Check {
    static void all(Layout L) {
        GatorReplay replay=new GatorReplay(Boss.SPLITTER),chunked=new GatorReplay(Boss.SPLITTER);
        for(int i=0;i<1800;i++){replay.step(L);if(i%2==1)chunked.update(GatorReplay.STEP*2,L);}
        check("Divide profiling reproduces the busy split state without a playthrough",replay.core.boss.pieceCount()==Boss.DIVIDE_PIECES);
        check("Divide profiling is frame chunk independent",replay.tick==chunked.tick
                && replay.core.boss.divideSplits==chunked.core.boss.divideSplits);

        GameCore cacheScene=replay.core;
        ProfileCrystalField.Counter vector=new ProfileCrystalField.Counter(false),cachedDraw=new ProfileCrystalField.Counter(true);
        cacheScene.crystalField.draw(vector,cacheScene,L,1);
        cacheScene.crystalField.draw(new OpacityPainter(cachedDraw,1),cacheScene,L,1);
        check("native crystal cache removes most backdrop commands through painter wrappers",cachedDraw.calls<vector.calls*.5f);
        check("cached crystal drawing retains live shimmer",cachedDraw.calls>20);
        CrystalField field=new CrystalField();field.prepare(L.w);
        CrystalField.Cell[][] cached=field.layers;
        field.prepare(L.w);
        check("crystal geometry stays cached between frames",field.layers==cached);
        boolean covers=true,finite=true;
        // Sample across the wrapping seam too: every point must have exactly one owner.
        for(int layer=0;layer<CrystalField.LAYERS;layer++)
        for(int row=0;row<61;row++)for(int col=0;col<37;col++) {
            float x=L.w*(col+.317f)/37,y=L.w*CrystalField.PERIOD*(row+.219f)/61;
            int owners=0;
            for(CrystalField.Cell cell:field.layers[layer])for(int tile=-1;tile<=1;tile++)
                if(inside(cell.rim,x,y-tile*CrystalField.PERIOD*L.w))owners++;
            covers&=owners==1;
        }
        int vertices=0;
        for(CrystalField.Cell cell:field.layers[0]) {
            vertices+=cell.rim.length/2;
            for(float v:cell.rim)finite&=Float.isFinite(v);
        }
        check("crystal floor covers the whole tile with no overlapping cells or seam gaps",covers);
        check("crystal geometry stays finite and bounded",finite && vertices<150);
        CrystalField duplicate=new CrystalField();duplicate.prepare(L.w);
        boolean same=true;
        for(int i=0;i<field.layers[0].length;i++)same&=java.util.Arrays.equals(field.layers[0][i].rim,duplicate.layers[0][i].rim);
        check("crystal geometry is deterministic",same);
        field.prepare(L.w*.75f);
        check("crystal cache adapts to a resized viewport",field.layers!=cached
                && Math.abs(field.layers[0][0].rim[0]-cached[0][0].rim[0]*.75f)<.01f);
        for(int layer=0;layer<CrystalField.LAYERS;layer++) {
            check("crystal layer scrolling wraps after a full tile "+layer,
                    Math.abs(CrystalField.scroll(5,L.w,layer)-CrystalField.scroll(5+CrystalField.PERIOD/CrystalField.SPEED[layer],L.w,layer))<.01f);
        }
        check("three crystal depths scroll at increasing rates",CrystalField.LAYERS>=3
                && CrystalField.SPEED[0]<CrystalField.SPEED[1] && CrystalField.SPEED[1]<CrystalField.SPEED[2]);
        check("crystal layers use different outlines",
                !java.util.Arrays.equals(field.layers[0][0].rim,field.layers[1][0].rim)
                && !java.util.Arrays.equals(field.layers[1][0].rim,field.layers[2][0].rim));
        GameCore c=new GameCore(new Mem(),248L);c.startGame();c.stage=6;c.landBlend=1;
        check("crystal stage uses the faceted floor",CrystalField.presence(c)==1);
        clearLights(c,L);
        c.landFrom=1;c.stage=11;c.landBlend=.5f;
        check("crystal floor fades on land departure",Math.abs(CrystalField.presence(c)-.5f)<.001f);
        c.state=GameCore.TITLE;
        check("crystal floor does not replace the title",CrystalField.presence(c)==0);
        c.state=GameCore.PLAY;c.survival.active=true;
        check("crystal floor does not replace Survival",CrystalField.presence(c)==0);
    }
    private static void clearLights(GameCore c,Layout L) {
        float x=L.w*.5f,y=L.h*.45f;
        check("crystal edges begin unlit",c.crystalField.lightAt(c,L,0,x,y)==0);
        GameCore.Enemy enemy=add(c,L,new int[]{0,2,4},y);
        c.destroyWord(enemy,x,y,L);
        for(int layer=0;layer<CrystalField.LAYERS;layer++) {
            check("enemy clear lights crystal depth "+layer,c.crystalField.lightAt(c,L,layer,x,y)>.99f);
            check("clear light stays near the destroyed word "+layer,c.crystalField.lightAt(c,L,layer,x,y+L.w)==0);
        }
        c.clock+=.25f;c.skyClock+=8;
        float expected=(1-.25f/CrystalField.CLEAR_TIME);expected*=expected;
        for(int layer=0;layer<CrystalField.LAYERS;layer++)
            check("clear light follows its own layer speed "+layer,
                    Math.abs(c.crystalField.lightAt(c,L,layer,x,y+8*CrystalField.SPEED[layer]*L.w)-expected)<.001f);
        c.destroyWord(enemy,x,y,L);
        check("duplicate clear does not restart crystal light",c.crystalField.lightAt(c,L,0,x,y)<.9f);
        c.clock+=CrystalField.CLEAR_TIME;
        check("crystal clear light expires",c.crystalField.lightAt(c,L,0,x,y)==0);
        c.crystalField.cleared(c,L,x,y,3,Glyph.COLOR[0]);
        c.paused=true;float clock=c.clock;
        c.update(.2f,L);
        check("pausing freezes crystal clear lights",c.clock==clock && c.crystalField.lightAt(c,L,0,x,y)>.99f);
        c.paused=false;c.state=GameCore.OVER;c.update(0,L);
        check("death clears crystal lights",c.crystalField.lightAt(c,L,0,x,y)==0);
        c.startGame();c.stage=6;c.landBlend=1;
        c.crystalField.cleared(c,L,x,y,3,Glyph.COLOR[0]);c.startGame();
        check("new run clears crystal lights",c.crystalField.lightAt(c,L,0,x,y)==0);
        c.stage=6;c.landBlend=1;
    }
    private static boolean inside(float[] p,float x,float y) {
        boolean hit=false;
        for(int i=0,j=p.length-2;i<p.length;j=i,i+=2)
            if((p[i+1]>y)!=(p[j+1]>y) && x<(p[j]-p[i])*(y-p[i+1])/(p[j+1]-p[i+1])+p[i])hit=!hit;
        return hit;
    }
}
