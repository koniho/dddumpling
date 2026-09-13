package com.dddumpling.game;

final class TestMystery extends Check {
    private static GameCore scene(Layout L,int stage) {
        GameCore c = new GameCore(new Mem(),42L);
        c.startGame(); c.jumpToStage(stage,L);
        c.stageGap=0f; c.spawnTimer=100f; c.powerTimer=0f;
        c.update(DT,L);
        return c;
    }
    static void all(Layout L) {
        GameCore early=scene(L,9), c=scene(L,11);
        SettingsUi ui=new SettingsUi();ui.compute(L,Music.NAMES.length);
        for(int i=0;i<2;i++) {
            float x=(ui.testChipL(i,2)+ui.testChipR(i,2))*0.5f;
            check("debuff chip hit target " + i,ui.hit(x,ui.debuffY+ui.testH*0.5f)==SettingsUi.HIT_DEBUFF+i);
            c.settingsOpen=true;c.playtestDebuff(Power.INCOGNITO+i);
            check("debuff chip closes settings and starts effect " + i,!c.settingsOpen && c.debuff==Power.INCOGNITO+i && c.debuffLeft==Power.DEBUFF_TIME && !c.powerActive());
        }
        c=scene(L,11);
        check("mystery introduction eases the difficulty ramp",Pacing.lessonRelief(11)==1.15f
                && Pacing.lessonRelief(12)==1.075f && Pacing.lessonRelief(13)==1f);
        check("early pickups keep their fixed outcome",!early.power.mystery && early.power.effect>=0);
        check("stage 11 pickup is undecided before collection",c.power.mystery && c.power.effect == -1);
        for(boolean team : new boolean[]{false,true}) {
            java.util.Set<Integer> options=new java.util.HashSet<Integer>();
            for(int i=0;i<Power.mysteryCount(team);i++) options.add(Power.mysteryAt(team,i));
            check("mystery pool contains both debuffs",options.contains(Power.INCOGNITO) && options.contains(Power.MONOCHROME));
            check("mystery eligibility excludes retired and unavailable modes",!options.contains(Power.MULTI) && options.contains(Power.TEAM)==team);
            Power w=new Power();w.mystery=true;w.teamAvailable=team;
            java.util.Set<Integer> shown=new java.util.HashSet<Integer>();
            for(int i=0;i<80;i++) {w.t=i*0.05f;shown.add(w.shownEffect());}
            check("floating mystery cycles through every eligible icon",shown.equals(options));
        }
        c.power.x=L.w/2f; c.power.y=L.playTop+L.enemyR*3f;
        int score=c.score;
        check("mystery accepts one collection tap",c.tapPower(c.power.x,c.power.y,L));
        check("selection is deferred and cannot be tapped again",!c.powerActive() && c.debuffLeft==0f
                && !c.tapPower(c.power.x,c.power.y,L) && c.score==score+Power.SCORE);
        c.power.effect=Power.MONOCHROME;
        c.paused=true; c.update(0.3f,L);
        check("pause freezes the roulette",c.power.hitT==0f);
        c.paused=false;
        advance(c,L,Power.SELECT_TIME+0.05f);
        check("roulette resolves to one debuff without frenzy acceleration",c.debuff==Power.MONOCHROME
                && c.debuffLeft>0f && !c.powerActive());
        check("monochrome starts with a partial fade",c.monochromeFade>0f && c.monochromeFade<1f);
        advance(c,L,0.7f);
        check("monochrome reaches full strength",c.monochromeFade==1f);
        MonochromePainter mono=new MonochromePainter(new RasterPainter(1,1,1),1f);
        int gray=mono.color(0x8073DA91);
        check("grayscale preserves opacity and removes all hue",(gray>>>24)==128
                && ((gray>>>16)&255)==((gray>>>8)&255) && ((gray>>>8)&255)==(gray&255));
        c.debuffLeft=0.05f; advance(c,L,0.1f);
        check("expiration fades color back instead of snapping",c.debuffLeft==0f && c.monochromeFade>0f && c.monochromeFade<1f);
        advance(c,L,0.7f);
        check("color returns fully",c.monochromeFade==0f);
        c.startDebuff(Power.INCOGNITO);
        check("incognito hides identifying artwork during play",c.incognito() && !c.powerActive());
        advance(c,L,0.4f);
        check("incognito transforms gradually",c.incognitoMorph>0f && c.incognitoMorph<1f);
        advance(c,L,0.5f);
        check("incognito reaches the shared disguise",c.incognitoMorph==1f);
        int[] disguise=null;
        for(int glyph=0;glyph<Glyph.COUNT;glyph++) {
            RasterPainter art=new RasterPainter(100,100,1);
            art.clear(0xFF202030);
            Kawaii.incognito(art,glyph,50f,50f,30f,0xFFFFBE85,1f);
            int[] pixels=art.resolve();
            if(disguise!=null) check("all disguised silhouettes and faces match " + glyph,java.util.Arrays.equals(disguise,pixels));
            disguise=pixels;
        }
        c.debuffLeft=0.01f; advance(c,L,0.1f);
        check("disguise reverses smoothly on expiry",c.incognito() && c.incognitoMorph>0f && c.incognitoMorph<1f);
        advance(c,L,0.8f);
        check("original characters return",!c.incognito() && c.incognitoMorph==0f);
        c.jumpToStage(12,L);
        check("stage transition clears debuffs",!c.incognito() && c.debuffLeft==0f && c.monochromeFade==0f);
        c.startDebuff(Power.MONOCHROME); c.startFrenzy(Power.FLURRY,L);
        check("beneficial powers replace debuffs cleanly",c.flurry() && c.debuffLeft==0f);
        c.startGame();
        check("new run has no debuff",c.debuffLeft==0f && c.monochromeFade==0f);
    }
}
