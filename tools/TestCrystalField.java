package com.dddumpling.game;

final class TestCrystalField extends Check {
    static void all(Layout L) {
        arena(L);
        arrival(L);

        GatorReplay replay=new GatorReplay(Boss.SPLITTER),chunked=new GatorReplay(Boss.SPLITTER);
        for(int i=0;i<1440;i++){replay.step(L);if(i%2==1)chunked.update(GatorReplay.STEP*2,L);}
        check("Divide profiling reproduces the busy split state without a playthrough",replay.core.boss.pieceCount()==Boss.DIVIDE_PIECES);
        check("Divide profiling is frame chunk independent",replay.tick==chunked.tick
                && replay.core.boss.divideSplits==chunked.core.boss.divideSplits);

        GameCore cacheScene=chunked.core;
        boolean burstSeen=false;
        for(int i=1440;i<GatorReplay.LENGTH;i++) {
            replay.step(L);burstSeen|=replay.core.boss.beaten && DivideDeath.bursting(replay.core.boss);
        }
        check("Divide profiling reaches the final arena shatter",burstSeen);

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
    static GameCore arenaScene(Layout L,int events) {
        GameCore c=TestBoss.enterBoss(L,Boss.SPLITTER,248);c.stage=10;c.landBlend=1;c.stageBanner=0;
        for(int event=0;event<events;event++) {
            for(int node=0;node<Boss.DIVIDE_NODES;node++)if(c.boss.nodeActive(node)) {
                c.boss.pieceHits[node]=Boss.DIVIDE_HITS;break;
            }
            c.boss.beginPinch(100);BossPlay.pinch(c,100*(Boss.DIVIDE_SCALE+.02f),L);
            if(!c.boss.beaten)c.boss.update(.02f,L,c.rnd);
        }
        if(!c.boss.beaten)for(int frame=0;frame<45;frame++)c.boss.update(DT,L,c.rnd);
        return c;
    }
    private static void arena(Layout L) {
        boolean symmetric=true,growing=true,seamless=true,covers=true;
        int[] previous=new int[3];
        for(int phase=0;phase<=CrystalArena.MAX_PHASE;phase++)for(int layer=0;layer<3;layer++) {
            CrystalField.Cell[] cells=CrystalArena.make(L.w,layer,phase);
            growing&=cells.length>previous[layer];previous[layer]=cells.length;
            for(CrystalField.Cell cell:cells)for(int j=0;j<cell.rim.length;j+=2) {
                int next=(j+2)%cell.rim.length;
                seamless&=!(Math.abs(cell.rim[j]-L.w*.5f)<.001f && Math.abs(cell.rim[next]-L.w*.5f)<.001f);
                boolean match=false;
                for(CrystalField.Cell other:cells)if(other.phase==cell.phase)
                    for(int k=0;k<other.rim.length;k+=2)
                        match|=Math.abs(cell.rim[j]+other.rim[k]-L.w)<.001f
                                && Math.abs(cell.rim[j+1]-other.rim[k+1])<.001f;
                symmetric&=match;
            }
            for(int row=0;row<31;row++)for(int col=0;col<23;col++) {
                float x=L.w*(col+.317f)/23,y=L.w*2*(row+.219f)/31;
                int owners=0;
                for(CrystalField.Cell cell:cells)if(inside(cell.rim,x,y))owners++;
                covers&=owners==1;
            }
        }
        check("all arena layers stay exactly mirrored through every fracture",symmetric);
        check("each pinch adds arena fragments",growing);
        check("center-spanning arena tiles have no vertical mirror seam",seamless);
        check("joined arena tiles cover every phase without gaps or overlaps",covers);
        GameCore c=arenaScene(L,7);
        check("seven actual splits advance the crystal arena",CrystalArena.phase(c.boss)==7 && c.shake>=1);
        ProfileCrystalField.Counter retainedDraw=new ProfileCrystalField.Counter(true);
        c.crystalField.draw(retainedDraw,c,L,1);
        CrystalField.Cell[][] beforeLoss=c.crystalArena.cells;
        c.lives=1;c.takeHit(L.w*.5f,L);
        c.crystalField.draw(retainedDraw,c,L,1);
        check("crystal defeat keeps the exact fractured arena geometry",CrystalArena.active(c)
                && c.crystalArena.cells==beforeLoss && CrystalArena.phase(BossVictory.sceneBoss(c))==7);
        c=arenaScene(L,15);
        check("eight fragment clears finish all fifteen fracture phases",c.boss.beaten && CrystalArena.phase(c.boss)==15);
        c=arenaScene(L,0);
        int key=c.boss.pieceWant(0);c.tapKey(key,L);
        check("arena does not light before projectile contact",!c.crystalField.hasLights(c));
        for(int step=0;step<120 && !c.crystalField.hasLights(c);step++)c.update(DT,L);
        check("projectile contact lights the crystal arena",c.crystalField.hasLights(c));
        boolean mirrored=true;
        for(int layer=0;layer<3;layer++)for(int x=0;x<8;x++) {
            float xx=L.w*(x+.25f)/8,yy=L.h*.35f;
            mirrored&=Math.abs(c.crystalField.lightAt(c,L,layer,xx,yy)-c.crystalField.lightAt(c,L,layer,L.w-xx,yy))<.001f;
        }
        check("arena impact lighting stays mirrored",mirrored);
    }
    private static void arrival(Layout L) {
        GameCore c=arenaScene(L,0);c.boss.intro=Boss.INTRO;c.clock=c.skyClock=5;
        ProfileCrystalField.Counter painter=new ProfileCrystalField.Counter(true);
        c.crystalField.draw(painter,c,L,1);
        CrystalArrival.Edge[] plan=c.crystalField.arrival.edges;
        boolean clipped=plan!=null,matched=false,unique=true;
        for(int i=0;i<plan.length;i++) {
            CrystalArrival.Edge e=plan[i];matched|=e.matched;
            clipped&=e.ax>=-.001f && e.bx>=-.001f && e.ax<=L.w+.001f && e.bx<=L.w+.001f
                    && e.ay>=-.001f && e.by>=-.001f && e.ay<=L.deckTop+.001f && e.by<=L.deckTop+.001f;
            if(!e.departing)for(int j=0;j<i;j++) {
                CrystalArrival.Edge prev=plan[j];
                if(prev.layer==e.layer && !prev.departing)
                    unique&=!(Math.abs(e.tx-prev.tx)<.001f && Math.abs(e.ty-prev.ty)<.001f
                            && Math.abs(e.ux-prev.ux)<.001f && Math.abs(e.uy-prev.uy)<.001f);
            }
        }
        check("arena arrival matches edges within a bounded visible plan",clipped && matched && plan.length<=CrystalArrival.PER_LAYER*2);
        check("arena arrival draws each destination edge once",unique);
        c.boss.intro=Boss.INTRO*.5f;c.clock+=.8f;c.skyClock+=.8f;
        painter=new ProfileCrystalField.Counter(true);c.crystalField.draw(painter,c,L,1);
        check("arena arrival reuses its edge plan as the sky scrolls",plan==c.crystalField.arrival.edges);
        check("cached arrival keeps drawing bounded",painter.calls<250);
        check("arena arrival crossfade begins and ends at the exact backdrops",CrystalArrival.blend(0)==0 && CrystalArrival.blend(1)==1);
        c.boss.intro=0;c.crystalField.draw(painter,c,L,1);
        check("arena arrival releases temporary edges when settled",c.crystalField.arrival.edges==null);
        c.boss.intro=Boss.INTRO;c.crystalField.draw(painter,c,L,1);c.crystalField.reset();
        check("arena arrival clears on game reset",c.crystalField.arrival.edges==null);
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
