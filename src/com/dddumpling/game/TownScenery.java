package com.dddumpling.game;

/** Lush, deterministic meadow layers for the town's three-screen-wide walking path. */
final class TownScenery extends Draw {
    static final float SKY_SCROLL = .06f, FAR_SCROLL = .18f, MID_SCROLL = .34f,
            NEAR_SCROLL = .52f, FOREGROUND_SCROLL = 1.35f;
    private static final int SKY_TOP = 0xFFBDEDEA, SKY_LOW = 0xFFF4EBC8;
    private static final int HILL_FAR = 0xFF9FD4A1, HILL_MID = 0xFF75BE7B;
    private static final int GRASS = 0xFF56A96A, GRASS_DARK = 0xFF327653;
    private static final int LEAF = 0xFF3E8D5D, LEAF_LIGHT = 0xFF71BE72;
    private static final int TRUNK = 0xFF78583E, CREAM = 0xFFFFF4D5;

    private TownScenery() { }

    /** Sky and distant parallax hills. Draw before entering the translated town world. */
    static void background(Painter p, Layout L, float camera, float clock) {
        float w = L.w, h = L.h, top = L.playTop, field = L.deckTop - top;
        p.fillRect(0,0,w,h,SKY_TOP);
        p.fillRect(0,L.deckTop,w,h,0xFF79C883);
        // Narrow bands give the turquoise sky a warm cream haze near the horizon.
        for (int i = 0; i < 18; i++) {
            float at = i / 17f;
            p.fillRect(0, top + field * .035f * i, w, top + field * .035f * (i + 1),
                    Glyph.mix(SKY_TOP, SKY_LOW, at * .62f));
        }
        p.fillRect(0, top + field * .63f, w, L.deckTop, HILL_MID);

        float sunX = w * .79f - camera * SKY_SCROLL * .5f, sunY = top + field * .17f;
        p.fillCircle(sunX, sunY, w * .115f, 0x30FFFBE0);
        p.fillCircle(sunX, sunY, w * .074f, 0x66FFF7C3);
        p.fillCircle(sunX, sunY, w * .047f, 0xFFFFF2B5);

        float drift = (float) Math.sin(clock * .10f) * w * .018f;
        cloud(p, w * .18f - camera * SKY_SCROLL + drift, top + field * .17f, w * .12f, .88f);
        cloud(p, w * .76f - camera * SKY_SCROLL * .75f - drift * .6f, top + field * .29f, w * .095f, .66f);
        cloud(p, w * 1.22f - camera * SKY_SCROLL + drift * .4f, top + field * .12f, w * .11f, .55f);

        distantTrees(p,L,camera,clock,FAR_SCROLL,.44f,.25f,31);
        hill(p, L, camera * FAR_SCROLL, top + field * .44f, field * .25f, HILL_FAR, 31);
        distantTrees(p,L,camera,clock,MID_SCROLL,.55f,.20f,57);
        hill(p, L, camera * MID_SCROLL, top + field * .55f, field * .20f, 0xFF82C486, 57);
        distantTrees(p,L,camera,clock,NEAR_SCROLL,.65f,.16f,83);
        hill(p,L,camera*NEAR_SCROLL,top+field*.65f,field*.16f,HILL_MID,83);
        distantLine(p,L,camera,top+field*.50f);

        // Fine haze softens the seam where the rolling hills meet the playable meadow.
        for (int i = 0; i < 7; i++) {
            float y = top + field * (.53f + i * .025f);
            p.fillRect(0, y, w, y + field * .028f, Glyph.withAlpha(CREAM, 18 - i * 2));
        }
    }

    /** Trees, undergrowth, stones and flowers in translated world coordinates. */
    static void ground(Painter p, Town t, Layout L, float clock) {
        float w = L.w, top = L.playTop, field = L.deckTop - top, world = w * 3f;
        float[] meadow=new float[198];
        meadow[0]=0;meadow[1]=L.deckTop;
        for(int i=0;i<=96;i++) {
            float x=world*i/96f;
            meadow[2+i*2]=x;
            meadow[3+i*2]=TownScreen.meadowTop(x,L);
        }
        meadow[196]=world;meadow[197]=L.deckTop;
        p.fillPoly(meadow,GRASS);
        for(int i=0;i<8;i++){
            float x=w*(.12f+i*.40f),y=top+field*(.66f+.055f*(i%3));
            groundPatch(p,x,y,w*(.30f+.07f*(i%2)),field*(.10f+.025f*(i%3)),i);
        }

        // Low rounded shrubs border both sides of the path while leaving its cream surface clear.
        for(int i=0;i<44;i++){
            float at=.015f+.97f*hash(i*59+404),x=TownScreen.pathX(at,L);
            float routeY=TownScreen.pathY(at,L),side=(i&1)==0?-1f:1f;
            float y=routeY+side*w*(.080f+hash(i*31+2)*.045f);
            y=Math.max(y,TownScreen.meadowTop(x,L)+w*.025f);
            float r=w*(.014f+hash(i*17+8)*.010f);
            if(TownScreen.inPlot(x,y,r,L))continue;
            shrub(p,x,y,r,i,t.motion(x,y,r*5f));
        }

        // Short grass is numerous enough to feel lush, but fixed-count and cheap to render.
        for (int i = 0; i < 78; i++) {
            float x = hash(i * 97 + 12) * world;
            float y = top + field * (.57f + hash(i * 53 + 31) * .38f);
            float at = Math.max(0f, Math.min(1f, (x / w - .15f) / 2.7f));
            float routeY = TownScreen.pathY(at, L);
            if (Math.abs(y - routeY) < w * .055f) y += y < routeY ? -w * .06f : w * .06f;
            float r = w * (.007f + hash(i * 29) * .006f);
            if(TownScreen.inPlot(x,y,r,L))continue;
            tuft(p, x, y, r, i % 3 == 0 ? 0xFF2F7F50 : 0xFF438E58, 210,
                    t.motion(x, y, r * 7f));
        }

        // Fixed hashed scatter: clumps of stones nestled in grass, with clear gaps between them.
        for(int i=0;i<25;i++) {
            float x=w*(.04f+2.92f*hash(i*71+300));
            float y=top+field*(.59f+hash(i*37+50)*.35f);
            float at=Math.max(0f,Math.min(1f,(x/w-.15f)/2.7f));
            float route=TownScreen.pathY(at,L);
            if(Math.abs(y-route)<w*.11f)y=route+(y<route?-1:1)*w*.12f;
            y=Math.max(TownScreen.meadowTop(x,L)+w*.06f,Math.min(L.deckTop-w*.06f,y));
            float r=w*(.017f+hash(i*23+6)*.029f);
            if(TownScreen.inPlot(x,y,r*1.5f,L))continue;
            p.fillEllipse(x,y+r*.35f,r*1.9f,r*.55f,0x3843784D);
            for(int k=0;k<3;k++) {
                float gx=x+r*(k-1)*1.25f,gy=y+r*(.35f+.22f*hash(i*13+k));
                tuft(p,gx,gy,r*.45f,k%2==0?0xFF73B96A:0xFF408D56,230,
                        (float)Math.sin(clock*1.1f+i+k)*.32f+t.motion(gx,gy,r*4f));
            }
            rock(p,x-r*.50f,y+r*.12f,r*.58f,i+1,t.motion(x,y,r*4f));
            rock(p,x+r*.32f,y,r,i,t.motion(x,y,r*4f));
            if(i%3==0)rock(p,x+r*1.18f,y+r*.30f,r*.42f,i+2,t.motion(x,y,r*4f));
        }

        int flowers = Math.min(102, 30 + t.flowerCount * 3);
        for (int i = 0; i < flowers; i++) {
            float x = hash(i * 67 + 711) * world;
            float y = top + field * (.58f + hash(i * 41 + 93) * .36f);
            float r = w * (.010f + hash(i * 17) * .008f);
            if(TownScreen.inPlot(x,y,r*2f,L))continue;
            daisy(p, x, y, r, i, (float)Math.sin(clock*1.15f+i)*.55f
                    + t.motion(x, y, r * 8f));
        }

        for (int i = 0; i < 16; i++) {
            float x = hash(i * 101 + 55) * world;
            float y = top + field * (.42f + hash(i * 79 + 20) * .42f);
            float pulse = .55f + .45f * (float) Math.sin(clock * 2.1f + i * 1.73f);
            float react=t.motion(x,y,w*.10f);
            x+=react*w*.018f;y-=Math.abs(react)*w*.012f;
            p.fillCircle(x, y, w * .005f, Glyph.withAlpha(0xFFFFF2A2, (int) (50 + 75 * pulse)));
            p.fillCircle(x, y, w * .0022f, Glyph.withAlpha(0xFFFFFFCE, (int) (150 + 90 * pulse)));
        }
    }

    // Rows contain root/anchor depth, x, canopy y, radius (zero for balloons), and seed/index.
    static float[][] depthItems(Layout L) {
        float w=L.w,field=L.deckTop-L.playTop;
        float[][] items=new float[48][];
        int count=0;
        // Uneven groves with open clearings, small saplings and taller shelter trees.
        float[] groves={.08f,.48f,1.07f,1.49f,2.12f,2.62f,2.94f};
        for(int group=0;group<groves.length;group++) {
            int members=3+group%3;
            for(int j=0;j<members;j++) {
                int seed=group*11+j;
                float x=w*(groves[group]+(hash(seed*43+9)-.5f)*.30f);
                float r=w*(.033f+hash(seed*61+2)*.070f);
                float root=TownScreen.meadowTop(x,L)+field*(.025f+j*.018f)
                        +w*.025f*hash(seed*19+4);
                if(TownScreen.inPlot(x,root,r*.8f,L))continue;
                float y=root-r*1.43f;
                items[count++]=new float[]{root,x,y,r,seed};
            }
        }

        for(int i=0;i<"DDDUMPLING TOWN".length();i++) {
            if("DDDUMPLING TOWN".charAt(i)==' ')continue;
            items[count++]=new float[]{TownScreen.balloonAnchorY(i,L),0f,0f,0f,i};
        }
        // Stable back-to-front ordering uses ground contact, never canopy height or idle sway.
        for(int i=1;i<count;i++) {
            float[] item=items[i];int j=i-1;
            while(j>=0&&items[j][0]>item[0]) {items[j+1]=items[j];j--;}
            items[j+1]=item;
        }
        return java.util.Arrays.copyOf(items,count);
    }

    static void treesAndBalloons(Painter p,Town t,Layout L,float clock) {
        for(float[] item:depthItems(L)) {
            if(item[3]==0f)TownScreen.balloon(p,t,L,clock,(int)item[4]);
            else tree(p,item[1],item[2],item[3],(int)item[4],1f,clock,
                    t.motion(item[1],item[2]-item[3]*.45f,item[3]*2.3f));
        }
    }

    /** Translucent leaves and flowers composited over the walking rider and attractions. */
    static void foreground(Painter p, Town t, Layout L, float clock) {
        float w = L.w, top = L.playTop, field = L.deckTop - top, world = w * (1f + 2f * FOREGROUND_SCROLL);
        // Oversized shelter trees grow from below the screen; only trunks and crowns enter the scene.
        float[] nearTrees={-.08f,1.45f,3.35f};
        for(int i=0;i<nearTrees.length;i++) {
            float x=w*nearTrees[i],r=w*(.28f+.055f*hash(i*41+70));
            float root=L.h+w*(.06f+.04f*hash(i*61+20));
            float y=root-r*1.43f;
            tree(p,x,y,r,i*13+2,.38f,clock,0f);
        }
        for(int i=0;i<10;i++){
            float x=w*(.03f+i*.42f)+(hash(i*89+1)-.5f)*w*.12f;
            float r=w*(.085f+hash(i*13+5)*.035f);
            float y=top+field*(.85f+hash(i*47+7)*.12f);
            bush(p,x,y,r,i,150,(float)Math.sin(clock*.75f+i*.9f)*.08f
                    );
        }
        // Oversized close leaves sweep across the meadow, clipped before the controls.
        for(int screen=0;screen<5;screen+=2){
            float x=screen*w,y=top+field*.96f;
            frond(p,x,y,w*.14f,screen%4==0?1:-1,clock+screen,0f);
        }
        for (int i = 0; i < Math.min(10, t.flowerCount / 2); i++) {
            float x = hash(i * 109 + 901) * world;
            float y = top + field * (.84f + hash(i * 31 + 8) * .13f);
            daisy(p,x,y,w*.018f,i+31,(float)Math.sin(clock*1.3f+i)*.60f
                    );
        }
    }

    /** Draw before each hill so the slope hides roots and the rear grove feels distant. */
    private static void distantTrees(Painter p,Layout L,float camera,float clock,
            float parallax,float base,float height,int seed) {
        float w=L.w,field=L.deckTop-L.playTop;
        for(int i=0;i<22;i++) {
            float wx=w*(-.12f+i*.095f+(hash(i*37+seed)-.5f)*.07f);
            float x=wx-camera*parallax;
            if(x < -w*.08f || x > w*1.08f)continue;
            float wave=(float)Math.sin(wx/w*4.4f+seed*.11f);
            float small=(float)Math.sin(wx/w*9.1f+seed);
            float root=L.playTop+field*(base-height*(.48f+.18f*wave+.05f*small));
            float r=w*(.012f+.016f*hash(i*61+seed));
            float sway=(float)Math.sin(clock*.65f+i)*r*.1f;
            int dark=Glyph.mix(0xFF467F70,SKY_TOP,parallax<.12f?.55f:.30f);
            int light=Glyph.mix(dark,0xFFC1DDA5,.25f);
            p.line(x,root+r*.5f,x+sway*.4f,root-r*1.7f,dark,r*.17f);
            p.fillEllipse(x+sway,root-r*1.65f,r*(i%3==0?.65f:1f),r*1.35f,dark);
            p.fillEllipse(x+sway-r*.24f,root-r*2.05f,r*.52f,r*.68f,light);
        }
    }

    private static void cloud(Painter p, float x, float y, float r, float fade) {
        int shadow = fadeBy(0x55B7D7C5, fade), white = fadeBy(0xD9FFFBE7, fade);
        p.fillEllipse(x, y + r * .14f, r * 1.35f, r * .38f, shadow);
        p.fillCircle(x - r * .56f, y, r * .50f, white);
        p.fillCircle(x, y - r * .24f, r * .70f, white);
        p.fillCircle(x + r * .63f, y + r * .03f, r * .46f, white);
        p.fillEllipse(x, y + r * .16f, r * 1.20f, r * .38f, white);
    }

    private static void hill(Painter p, Layout L, float shift, float base, float height,
            int color, int seed) {
        float[] pts = new float[30];
        pts[0] = 0; pts[1] = L.deckTop;
        for (int i = 0; i <= 12; i++) {
            float x = L.w * i / 12f;
            float wave = (float) Math.sin((x + shift) / L.w * 4.4f + seed * .11f);
            float small = (float) Math.sin((x + shift) / L.w * 9.1f + seed);
            pts[2 + i * 2] = x;
            pts[3 + i * 2] = base - height * (.48f + .18f * wave + .05f * small);
        }
        pts[28] = L.w; pts[29] = L.deckTop;
        p.fillPoly(pts, color);
    }

    private static void distantLine(Painter p,Layout L,float camera,float y){
        float w=L.w,shift=-(camera*MID_SCROLL% (w*.18f));
        p.line(0,y,w,y,0x6664875E,w*.006f);
        for(int i=-1;i<8;i++){
            float x=shift+i*w*.18f,r=w*(.017f+.004f*(i&1));
            p.line(x,y-r*.2f,x,y+r*.50f,0x88745F43,w*.007f);
            p.fillCircle(x-r*.32f,y-r*.60f,r*.62f,0x99659D68);
            p.fillCircle(x+r*.28f,y-r*.67f,r*.55f,0x9974AD71);
        }
    }

    private static void groundPatch(Painter p,float x,float y,float rx,float ry,int seed){
        int color=seed%3==0?0x5059C775:seed%3==1?0x58418E59:0x526FC77A;
        p.fillPoly(new float[]{x-rx,y,x-rx*.72f,y-ry*.70f,x-rx*.12f,y-ry,
                x+rx*.50f,y-ry*.72f,x+rx,y-ry*.08f,x+rx*.72f,y+ry*.52f,
                x,y+ry*.74f,x-rx*.68f,y+ry*.47f},color);
    }

    private static void shrub(Painter p,float x,float y,float r,int seed,float motion){
        x+=motion*r*.25f;y-=Math.abs(motion)*r*.10f;
        int dark=0xFF337B50,mid=seed%2==0?0xFF4D9B5B:0xFF5BA965,hi=0xFF83C978;
        p.fillCircle(x-r*.55f,y,r*.57f,dark);p.fillCircle(x+r*.48f,y,r*.61f,dark);
        p.fillCircle(x,y-r*.32f,r*.72f,mid);
        p.fillEllipse(x-r*.15f,y-r*.55f,r*.25f,r*.11f,hi);
        if(seed%5==0)p.fillCircle(x+r*.32f,y-r*.42f,r*.12f,0xFFFFE39A);
    }

    private static void tree(Painter p, float x, float y, float r, int seed, float fade,
            float clock, float motion) {
        int variant=seed%4;
        int leaf=variant==1?0xFF648D4B:variant==2?0xFF398879:variant==3?0xFF77A45A:LEAF;
        int light=variant==1?0xFFA5C66C:variant==2?0xFF79B99A:variant==3?0xFFBDD884:LEAF_LIGHT;
        int trunk = fadeBy(TRUNK, fade), dark = fadeBy(Glyph.mix(leaf,0xFF173F39,.34f), fade);
        p.fillEllipse(x + r * .05f, y + r * 1.38f, r * .62f, r * .16f,
                fadeBy(0x55305E43, fade));
        p.fillRect(x - r * .14f, y - r * .05f, x + r * .15f, y + r * 1.43f, trunk);
        p.line(x, y + r * .58f, x - r * .66f, y - r * .16f, trunk, r * .16f);
        p.line(x, y + r * .43f, x + r * .70f, y - r * .28f, trunk, r * .14f);
        float sway=r*((float)Math.sin(clock*.92f+seed*1.37f)*.14f+motion*.13f);
        float lift=-Math.abs(motion)*r*.07f,tx=x+sway,ty=y+lift;
        if(variant==1 || variant==2) {
            float wide=variant==1?.78f:1.30f,tall=variant==1?1.33f:.75f;
            p.fillEllipse(tx,ty-r*.48f,r*wide,r*tall,dark);
            p.fillEllipse(tx-r*.13f,ty-r*.64f,r*wide*.84f,r*tall*.80f,fadeBy(leaf,fade));
            p.fillEllipse(tx-r*.24f,ty-r*.96f,r*wide*.52f,r*tall*.37f,fadeBy(light,fade));
            for(int k=0;k<5;k++) {
                float lx=tx+r*wide*(hash(seed*31+k*17)-.5f)*1.3f;
                float ly=ty-r*.45f+r*tall*(hash(seed*47+k*11)-.5f)*1.2f;
                p.fillEllipse(lx,ly,r*.13f,r*.07f,fadeBy(Glyph.mix(leaf,light,.5f),fade));
            }
            return;
        }
        p.fillCircle(tx-r*.55f,ty-r*.17f,r*.70f,dark);
        p.fillCircle(tx+r*.52f,ty-r*.23f,r*.74f,dark);
        p.fillCircle(tx,ty-r*.72f,r*.88f,dark);
        p.fillCircle(tx-r*.17f,ty-r*.50f,r*.86f,fadeBy(leaf,fade));
        p.fillCircle(tx+r*.50f,ty-r*.45f,r*.57f,fadeBy(leaf,fade));
        p.fillCircle(tx-r*.58f,ty-r*.39f,r*.52f,fadeBy(leaf,fade));
        p.fillCircle(tx-r*.26f,ty-r*.88f,r*.45f,fadeBy(light,fade*.82f));
        p.fillCircle(tx+r*.22f,ty-r*.78f,r*.35f,fadeBy(0xFF83CD79,fade*.70f));
        p.fillEllipse(tx-r*.30f,ty-r*.82f,r*.31f,r*.14f,fadeBy(0xAA9CDD83,fade));
        p.fillEllipse(tx+r*.27f,ty-r*.54f,r*.27f,r*.11f,fadeBy(0x887FD079,fade));
        p.fillEllipse(tx-r*.49f,ty-r*.32f,r*.20f,r*.085f,fadeBy(0x777BC778,fade));
        if(seed%3==0){
            p.fillCircle(tx+r*.42f,ty-r*.36f,r*.09f,fadeBy(0xFFFFD477,fade));
            p.fillCircle(tx-r*.48f,ty-r*.28f,r*.08f,fadeBy(0xFFFFB5BC,fade));
        }
    }

    private static void bush(Painter p,float x,float y,float r,int seed,int alpha,float motion){
        int dark=Glyph.withAlpha(0xFF1F6247,alpha),leaf=Glyph.withAlpha(0xFF347B51,alpha);
        x+=motion*r*.12f;y-=Math.abs(motion)*r*.08f;
        p.fillCircle(x - r * .54f, y, r * .60f, dark);
        p.fillCircle(x + r * .52f, y + r * .04f, r * .63f, dark);
        p.fillCircle(x, y - r * .34f, r * .82f, leaf);
        p.fillCircle(x - r * .25f, y - r * .58f, r * .36f,
                Glyph.withAlpha(0xFF62A968, Math.max(0, alpha - 20)));
        if((seed&1)==0)daisy(p,x+r*.32f,y-r*.55f,r*.09f,seed,motion);
    }

    private static void tuft(Painter p,float x,float y,float r,int color,int alpha,float motion){
        int c=Glyph.withAlpha(color,alpha);float bend=motion*r*.85f;
        p.polyline(new float[]{x,y,x-r*.72f+bend,y-r*1.55f},c,r*.22f);
        p.polyline(new float[]{x,y,x+bend,y-r*2.05f},c,r*.24f);
        p.polyline(new float[]{x,y,x+r*.82f+bend,y-r*1.45f},c,r*.22f);
    }

    private static void rock(Painter p,float x,float y,float r,int seed,float motion){
        int stone=seed%2==0?0xFF7C9195:0xFF617C83;
        x+=motion*r*.12f;y-=Math.abs(motion)*r*.18f;
        p.fillEllipse(x, y + r * .25f, r * 1.15f, r * .30f, 0x33305E43);
        p.fillPoly(new float[]{x-r,y,x-r*.62f,y-r*.65f,x+r*.22f,y-r*.82f,
                x+r,y-r*.12f,x+r*.72f,y+r*.18f,x-r*.70f,y+r*.18f},stone);
        p.fillPoly(new float[]{x-r,y,x-r*.62f,y-r*.65f,x-r*.10f,y-r*.36f,x-r*.16f,y+r*.18f},
                Glyph.mix(stone,0xFF344C5A,.28f));
        p.fillPoly(new float[]{x-r*.62f,y-r*.65f,x+r*.22f,y-r*.82f,x+r*.50f,y-r*.39f,x-r*.10f,y-r*.36f},
                Glyph.mix(stone,0xFFD4DFBD,.27f));
        p.line(x-r*.48f,y-r*.48f,x+r*.15f,y-r*.62f,0x99C7D7B8,r*.10f);
    }

    private static void daisy(Painter p,float x,float y,float r,int seed,float motion){
        int kind=Math.floorMod(seed,5);
        int color=kind==0?0xFFFF6BA3:kind==1?0xFFFFC53D:kind==2?0xFFB780F0:
                kind==3?0xFFFFF8D9:0xFFFF8655;
        float headX=x+motion*r*1.15f,root=y+r*2.8f;
        p.polyline(new float[]{x,root,x+motion*r*.4f,y+r*1.4f,headX,y},0xFF307B46,r*.22f);
        p.fillEllipse(x-r*.42f,y+r*1.75f,r*.52f,r*.22f,0xFF65AC50);
        p.fillEllipse(x+r*.39f,y+r*2.1f,r*.45f,r*.20f,0xFF438F43);
        if(kind==4) {
            p.fillEllipse(headX,y+r*.12f,r*.85f,r*.74f,color);
            for(int k=-1;k<=1;k++)p.fillEllipse(headX+k*r*.46f,y-r*.29f,r*.36f,r*.66f,
                    k==0?0xFFFFB46E:color);
        } else {
            int petals=kind==2?5:kind==1?9:6;
            for(int i=0;i<petals;i++) {
                double angle=Math.PI*2*i/petals;
                float px=headX+(float)Math.cos(angle)*r*.69f,py=y+(float)Math.sin(angle)*r*.69f;
                p.fillEllipse(px,py,r*.47f,r*(kind==2?.49f:.37f),Glyph.mix(color,0xFF8A4666,.16f));
                p.fillEllipse(px-r*.06f,py-r*.10f,r*.40f,r*.31f,color);
            }
            p.fillCircle(headX,y,r*.34f,kind==1?0xFF986231:0xFFFFD34F);
            p.fillCircle(headX-r*.09f,y-r*.10f,r*.12f,0xFFFFF4B6);
        }
    }

    private static void frond(Painter p,float x,float y,float r,int side,float clock,float motion){
        float sway=((float)Math.sin(clock*.55f+x*.01f)*.05f+motion*.15f)*r;
        int stem=0xA33A684C,leaf=0x99387A54,light=0x8860A86A;
        float tipX=x+side*r*.28f+sway,tipY=y-r*1.08f;
        p.polyline(new float[]{x,y+r*.12f,x+side*r*.08f+sway*.35f,y-r*.42f,tipX,tipY},
                stem,r*.075f);
        for(int i=0;i<4;i++){
            float at=(i+.22f)/4f,cx=x+side*r*(.04f+.22f*at)+sway*at;
            float cy=y-r*(.16f+.76f*at),rx=r*(.30f-.035f*i),ry=r*(.15f-.014f*i);
            p.fillEllipse(cx+side*rx*.62f,cy,rx,ry,i%2==0?leaf:light);
            p.fillEllipse(cx-side*rx*.48f,cy+r*.07f,rx*.82f,ry*.86f,leaf);
        }
        p.fillCircle(tipX,tipY,r*.16f,light);
    }
}
