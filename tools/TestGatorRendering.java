package com.dddumpling.game;

final class TestGatorRendering extends Check {
    static final class Cache extends OpacityPainter {
        int palms,pebbles,tint;float mix,x,y,angle;
        final float[] palmX=new float[2],palmY=new float[2];
        Cache() {super((Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(p,m,a)->null),1);}
        public ArtStamp stamps(){return this;}
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
}
