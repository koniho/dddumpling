package com.dddumpling.game;

final class TestGatorRendering extends Check {
    static final class Cache extends OpacityPainter {
        int palms,pebbles,tint;float mix,x,y,angle,phase;
        final float[] palmX=new float[2],palmY=new float[2];
        Cache() {super((Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(p,m,a)->null),1);}
        public ArtStamp stamps(){return this;}
        public boolean greenStamp(int art,float x,float y,float w,float h,float angle,int tint,float mix,float phase) {
            this.phase=phase;return stamp(art,x,y,w,h,angle,tint,mix);
        }
        public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float mix) {
            if(art==ArtStamp.PALM) {if(palms<2){palmX[palms]=x;palmY[palms]=y;}palms++;}
            else pebbles++;
            this.tint=tint;this.mix=mix;this.x=x;this.y=y;this.angle=angle;return true;
        }
    }
    static void all(Layout l) {
        group("cached Gator artwork");
        GameCore c=TestAlligator.fight(l);Alligator a=c.boss.alligator;
        a.next=0;a.timer=0;TestAlligator.advance(c,l,.3f);
        Cache cache=new Cache();AlligatorArt.draw(cache,c,l,c.boss,1);
        check("both palms and skin texture use optional stamps",cache.palms==2 && cache.pebbles==57);
        for(int side=0;side<2;side++)check("cached palm follows its moving wrist "+side,
                Math.abs(cache.palmX[side]-a.drawnX(l,side))<.01f && Math.abs(cache.palmY[side]-a.drawnY(l,side))<.01f);
        cache=new Cache();AlligatorArt.draw(cache,c,l,c.boss,.5f);
        check("translucent transitions retain per-primitive vector fading",cache.palms==0 && cache.pebbles==0);
        cache=new Cache();Painter outer=new ColorFadePainter(cache,0xFFDD3377,.4f,1);
        Painter inner=new ColorFadePainter(outer,0xFFFFA052,.5f,1);
        check("phase and attack tints preserve cache access",inner.stamps()!=null);
        inner.stamps().stamp(ArtStamp.PALM,5,6,7,8,.3f,0,0);
        int expected=((ColorFadePainter)outer).color(((ColorFadePainter)inner).color(AlligatorArt.SKIN));
        int actual=Glyph.mix(AlligatorArt.SKIN,cache.tint,cache.mix);
        boolean match=true;for(int shift=0;shift<=16;shift+=8)match &= Math.abs(((expected>>>shift)&255)-((actual>>>shift)&255))<=2;
        check("cached tint composition follows nested vector palette",match && cache.x==5 && cache.y==6 && cache.angle==.3f);
        check("grayscale and opacity effects fall back safely",new MonochromePainter(cache,1).stamps()==null
                && new ColorFadePainter(cache,0,.5f,.5f).stamps()==null && new OpacityPainter(cache,.5f).stamps()==null);
        palette(l);

        group("repeatable isolated Gator replay");
        GatorReplay reference=new GatorReplay(),chunked=new GatorReplay();
        check("replay starts directly at Gator with no persistent store",reference.core.store==null
                && reference.core.boss.kind==Boss.ALLIGATOR && reference.core.boss.intro>0 && reference.core.state==GameCore.PLAY);
        int demos=0;boolean hit=false,parried=false,damaged=false,longWindow=false;
        for(int frame=0;frame<GatorReplay.LENGTH;frame++) {
            reference.step(l);Boss b=reference.core.boss;
            if(b.alligator.showoff())demos|=1<<b.alligator.showoffCount;
            hit |= reference.core.hurtThisStage>0;parried |= reference.core.timeAttack.defended>0;
            damaged |= b.hurt>0;longWindow |= b.alligator.longWindow;
            if(frame%2==1)chunked.update(GatorReplay.STEP*2,l);
        }
        check("replay covers both combo demos, real parries, both damage effects and long opening",
                demos==12 && hit && parried && damaged && longWindow);
        check("fixed-step replay is independent of render cadence",reference.tick==chunked.tick
                && reference.core.boss.hp==chunked.core.boss.hp && reference.core.score==chunked.core.score
                && reference.core.hurtThisStage==chunked.core.hurtThisStage);
        reference.step(l);GatorReplay fresh=new GatorReplay();fresh.step(l);
        check("replay loops back to identical seeded entry",reference.loops==1 && reference.tick==1
                && reference.core.boss.intro==fresh.core.boss.intro && reference.core.boss.hp==fresh.core.boss.hp);
    }
    private static void palette(Layout l) {
        group("Gator green-to-red palette");
        Cache cache=new Cache();GatorPalette red=new GatorPalette(cache,1);
        check("phase preserves white highlights, dark neutrals and alpha",red.color(0x70FFFFFF)==0x70FFFFFF
                && red.color(0xFF181818)==0xFF181818 && (red.color(0x55418F69)>>>24)==0x55);
        int skin=red.color(AlligatorArt.SKIN);
        check("phase transfers green pigment into red instead of adding a wash",
                ((skin>>>16)&255)==143 && ((skin>>>8)&255)==65 && (skin&255)==70);
        java.util.Set<Integer> inks=new java.util.HashSet<>();
        Painter collector=new OpacityPainter(cache.p,1) {
            int color(int c) {inks.add(c);return c;}
        };
        for(int art=0;art<ArtStamp.COUNT;art++)AlligatorArt.stampArt(collector,art,100);
        boolean valid=true,match=true;
        float[] matrix=new float[20];
        for(int ink:inks) {
            int r=(ink>>>16)&255,g=(ink>>>8)&255,b=ink&255;
            valid &= g>=r && b>=.45f*(g-r);
            for(float phase:new float[]{0,.4f,.75f,1})for(float flash:new float[]{0,.37f,.67f}) {
                GatorPalette palette=new GatorPalette(cache,phase);
                // White attack tint commutes with the pigment transfer; red hurt flash follows it.
                ColorFadePainter hurt=new ColorFadePainter(cache,0xFFFF786E,.2f,1);
                Painter p=new ColorFadePainter(new GatorPalette(hurt,phase),0xFFFFFFFF,flash,1);
                p.stamps().stamp(ArtStamp.PALM,0,0,1,1,0,0,0);
                GatorPalette.matrix(matrix,cache.tint,cache.mix,cache.phase);
                int expected=hurt.color(palette.color(Glyph.mix(ink,0xFFFFFFFF,flash)));
                for(int row=0;row<3;row++) {
                    int actual=Math.round(matrix[row*5]*r+matrix[row*5+1]*g+matrix[row*5+2]*b+matrix[row*5+4]);
                    match &= Math.abs(actual-((expected>>>(16-row*8))&255))<=2;
                }
                match &= matrix[18]==1 && matrix[19]==0;
            }
        }
        check("cached source inks support the linear pigment transfer",valid);
        check("cached and vector palettes agree through attack and damage flashes",match);
        for(float health:new float[]{.4f,.2f}) {
            GameCore c=TestAlligator.fight(l);c.boss.hp=c.boss.hpMax*health;c.boss.hurt=.9f;
            cache=new Cache();AlligatorArt.draw(cache,c,l,c.boss,1);
            check("red phases retain both cached palms and all texture stamps "+health,cache.palms==2 && cache.pebbles==57);
        }
    }
}
