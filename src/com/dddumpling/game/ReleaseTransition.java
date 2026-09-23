package com.dddumpling.game;

/** Steamer gathers at centre, then trades places with the list. Closing slides the small steamer home from the left. */
final class ReleaseTransition {
    // Preserve the steamer lead-in; the list itself uses the shared panel slide duration.
    static final float GATHER_TIME=.529f, DURATION=GATHER_TIME+Draw.PANEL_SLIDE_TIME;
    static final float CENTRE=.437f/DURATION, SLIDE=GATHER_TIME/DURATION, LID_TIME=.75f;
    float progress=1f;
    private float lidAge=LID_TIME;
    boolean closing;
    private float sourceX,sourceY,sourceR,sourceLift;
    private boolean cornerReturn;
    void begin(ReleaseMascot mascot,Layout L,float clock) {
        sourceX=mascot.x(L)/L.w;sourceY=mascot.y(L)/L.h;sourceR=mascot.radius(L)/L.w;
        sourceLift=mascot.attentionLift(clock);progress=0f;lidAge=0f;closing=cornerReturn=false;
    }
    void close() {
        if(closing) return;
        cornerReturn=progress>=1f;closing=true;
    }
    boolean moving() { return closing || progress<1f; }
    boolean update(float dt) {
        lidAge=Math.min(LID_TIME,lidAge+dt);
        progress=Math.max(0f,Math.min(1f,progress+(closing?-dt:dt)/(cornerReturn?Draw.PANEL_SLIDE_TIME:DURATION)));
        return closing && progress<=0f;
    }
    float lidLift() { return .55f*(float)Math.sin(Math.PI*lidAge/LID_TIME)+sourceLift*(1f-lidAge/LID_TIME); }
    private static float ease(float t) { return Draw.panelTravel(t); }
    float slide() { return cornerReturn ? ease(progress) : ease((progress-SLIDE)/(1f-SLIDE)); }
    float listX(Layout L) { return L.w*(1f-slide()); }
    float x(Layout L) {
        if(cornerReturn) return L.unit*2.5f-(L.unit*2.5f+L.unit*1.6f)*ease(progress);
        float start=sourceX*L.w;
        float center=start+(L.w*.5f-start)*ease(progress/CENTRE);
        return center-(L.w*.5f+L.w*.23f)*slide();
    }
    float y(Layout L) {
        if(cornerReturn) return ReleaseMascot.y(L);
        float start=sourceY*L.h;
        return start+(L.h*.5f-start)*ease(progress/CENTRE);
    }
    float radius(Layout L) {
        if(cornerReturn) return L.unit*.8f;
        float start=sourceR*L.w;
        return start+(L.w*.14f-start)*ease(progress/CENTRE);
    }
}
