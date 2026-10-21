package com.dddumpling.game;

/** First sightings pause on the real enemy, without changing its movement or generation. */
final class EnemyTutorial extends Draw {
    static boolean message(int speech) { return speech>=TutorialSpeech.FOG && speech<=TutorialSpeech.LINKED; }
    static int kind(GameCore.Enemy e) {
        if(e.link!=null)return TutorialSpeech.LINKED;
        if(e.fastSingle)return TutorialSpeech.RUSH;
        if(e.swampKind==SwampEnemies.FOG)return TutorialSpeech.FOG;
        if(e.swampKind==SwampEnemies.DIVE)return TutorialSpeech.DIVEBOMB;
        return 0;
    }
    static float companionY(Layout L) { return bottom(L)-TutorialSpeech.unit(L)*11.5f; }
    static float bottom(Layout L) { return Math.min(L.h*.82f,L.deckTop-TutorialSpeech.unit(L)*.5f); }
    static float buttonY(Layout L) { return bottom(L)-TutorialSpeech.unit(L)*1.4f; }
    static float[] bounds(GameCore c,Layout L,GameCore.Enemy e) {
        float left=c.tileX(e,e.pos,L),right=c.tileX(e,e.word.length-1,L),top=e.y,bottom=e.y;
        if(e.link!=null) {
            left=Math.min(left,c.tileX(e.link,0,L));right=Math.max(right,c.tileX(e.link,0,L));
            top=Math.min(top,e.link.y);bottom=Math.max(bottom,e.link.y);
        }
        float margin=L.enemyR*1.6f;
        return new float[]{left-margin,top-margin,right+margin,bottom+margin};
    }
    static void introduce(GameCore c,Layout L) {
        Onboarding o=c.onboarding;
        if(c.state!=GameCore.PLAY || c.stageBanner>0 || c.dying() || c.boss.active() || Cave.active(c) || c.powerActive()
                || o.briefing || o.practice!=null || o.hintKind!=0 || o.powerGuide || o.bossGuide
                || o.rescueGuide || o.starGuide || !o.eligible(Onboarding.SKIPPED))return;
        for(GameCore.Enemy e:c.enemies) {
            int message=kind(e);
            if(message==0 || o.learned(message) || !e.typeable() || e.attacking || e.slideT>0
                    || e.linkWaiting || e.link!=null && (!e.link.typeable() || e.link.linkWaiting))continue;
            float[] b=bounds(c,L,e);
            // Wait until the whole threat is below the header and clear of the speech bubble.
            if(b[0]<L.playLeft || b[2]>L.playRight || b[1]<L.playTop || b[3]>L.h*.45f)continue;
            o.clear();o.enemyLesson=e;o.speech=message;o.briefing=true;o.age=0;
            Pause.release(c);o.narrate(c);return;
        }
    }
    static void draw(Painter p,GameCore c,Layout L) {
        Onboarding o=c.onboarding;float[] b=bounds(c,L,o.enemyLesson);
        int dim=0xB8101022;
        p.fillRect(0,0,L.w,b[1],dim);p.fillRect(0,b[3],L.w,L.h,dim);
        float radius=(b[3]-b[1])*.5f,cy=(b[1]+b[3])*.5f;
        p.save();p.clipOutCircle(b[0]+radius,cy,radius);p.clipOutCircle(b[2]-radius,cy,radius);
        p.fillRect(0,b[1],b[0]+radius,b[3],dim);p.fillRect(b[2]-radius,b[1],L.w,b[3],dim);p.restore();
        float pulse=.5f+.5f*(float)Math.sin(o.age*4),s=TutorialSpeech.unit(L);
        p.strokePoly(pill((b[0]+b[2])*.5f,(b[1]+b[3])*.5f,(b[2]-b[0])*.5f,(b[3]-b[1])*.5f,16),
                Glyph.mix(GOLD,ROSE,pulse),s*(.10f+.05f*pulse));
        float top=bottom(L)-s*9f,companionY=companionY(L),growth=o.bubbleProgress();
        TutorialSpeech.bubble(p,L.w*.05f,top,L.w*.95f,bottom(L),s*.8f,3,
                new float[]{L.w*.10f,top,L.w*.13f,companionY+s*.8f,L.w*.23f,top},growth,L.w*.13f,companionY,0xFF302440);
        if(growth<1)return;
        String[] lines=TutorialSpeech.pageLines(o.speech,0,c.preferences.skin);
        for(int i=0;i<lines.length;i++)p.text(lines[i],L.w*.5f,top+s*(1.8f+i*1.45f),
                type(s*.8f),0xFFFFF5DD,Painter.CENTER,true);
        p.fillPoly(pill(L.w*.5f,buttonY(L),L.w*.32f,s*1.15f,14),0xFF387358);
        p.text("LET'S TRY!",L.w*.5f,buttonY(L)+s*.38f,type(s*.92f),0xFFFFFFFF,Painter.CENTER,true);
    }
}
