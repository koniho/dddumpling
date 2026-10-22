package com.dddumpling.game;

/** A compact, action-led tour once the first Adventure run has been recorded. */
final class TitleTour extends Draw {
    static boolean active(GameCore c) {
        return c.onboarding.titleGuide && c.onboarding.speech>=TutorialSpeech.SCORES
                && c.onboarding.speech<=TutorialSpeech.MODES;
    }
    static boolean ready(GameCore c) {
        return c.highScores.latest>0 && c.modes.adventure() && !c.caseOpen && c.caseFade<=0
                && c.landDiscovery<0 && c.landTravelFrom<0 && LandDiscovery.next(c)<0
                && !c.onboarding.learned(TutorialSpeech.MODES);
    }
    static void begin(GameCore c,int message) {
        Onboarding o=c.onboarding;o.clear();o.titleGuide=true;o.speech=message;o.age=0;
        Pause.release(c);o.narrate(c);
    }
    static void openedScores(GameCore c) {
        if(active(c) && c.onboarding.speech==TutorialSpeech.SCORES
                && HighScoreScreen.displayCount(c)>0)begin(c,TutorialSpeech.SCORE_DETAILS);
    }
    static void openedDetails(GameCore c) {
        if(!active(c) || c.onboarding.speech!=TutorialSpeech.SCORE_DETAILS)return;
        c.onboarding.learn(c,TutorialSpeech.SCORES);
        c.onboarding.learn(c,TutorialSpeech.SCORE_DETAILS);c.onboarding.clear();
    }
    static void modeChanged(GameCore c) {
        if(!active(c) || c.onboarding.speech!=TutorialSpeech.MODES)return;
        c.onboarding.learn(c,TutorialSpeech.MODES);c.onboarding.clear();
    }
    static float skipY(Layout L) { return Math.min(L.deckTop-L.unit*.75f,L.h*.88f); }
    static float companionY(Layout L) { return L.h*.78f; }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!active(c) || c.state!=GameCore.TITLE || c.settingsOpen || c.releaseNotes.open
                || c.updatePrompt.open || c.storyOpen() || c.caseOpen || c.paused || c.townOpen)return;
        Onboarding o=c.onboarding;
        boolean details=o.speech==TutorialSpeech.SCORE_DETAILS, modes=o.speech==TutorialSpeech.MODES;
        if(details && (!c.highScoreScreen.open || c.highScoreScreen.moving()))return;
        float s=TutorialSpeech.unit(L),x=L.w*.5f,y,halfHeight;
        if(details) {halfHeight=HighScoreScreen.rowHeight(c,L)*.5f;y=HighScoreScreen.listTop(L)+halfHeight;}
        else {halfHeight=L.unit*(modes?2.1f:1.1f);y=modes?ModeSelector.y(L)-L.unit*.65f:HighScoreScreen.titleY(L)-L.unit*.25f;}
        float pulse=.5f+.5f*(float)Math.sin(o.age*4f);
        int color=Glyph.mix(GOLD,ROSE,pulse);
        float[] frame=pill(x,y,L.w*(details?.425f:.46f),halfHeight,16);
        p.strokePoly(frame,fadeBy(color,.12f),s*.45f);
        p.strokePoly(frame,fadeBy(color,.6f+.3f*pulse),s*.10f);
        float handX=modes?x+L.w*(.20f-.40f*TutorialSpeech.swipeProgress(o.age)):x;
        Renderer.touchHint(p,handX,y+halfHeight*.65f,L.keyR*.65f,1f,.85f,o.age);
        float top=L.h*.72f,bottom=Math.min(L.deckTop-s*.4f,L.h*.85f),left=L.w*.25f;
        float cy=companionY(L),growth=o.bubbleProgress();
        TutorialSpeech.bubble(p,left,top,L.w*.97f,bottom,s*.65f,2,
                new float[]{left,cy-s*.4f,L.w*.19f,cy,left,cy+s*.4f},growth,L.w*.13f,cy,0xFF302440);
        if(growth>=1) {
            String[] lines=TutorialSpeech.pageLines(o.speech,0,c.preferences.skin);
            for(int i=0;i<lines.length;i++)p.text(lines[i],L.w*.61f,top+(bottom-top)*(i+1)/(lines.length+1),
                    type(s*.64f),0xFFFFF5DD,Painter.CENTER,true);
        }
        TutorialSpeech.companion(p,c,L);
        p.fillPoly(pill(L.w*.84f,skipY(L),L.w*.135f,L.unit*.95f,12),0xFF493953);
        p.text("SKIP ALL",L.w*.84f,skipY(L)+L.unit*.3f,type(L.unit*.62f),INK,Painter.CENTER,true);
    }
}
