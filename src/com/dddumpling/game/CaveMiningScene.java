package com.dddumpling.game;

/** Independent visual/feedback clock; a spill continues after ride simulation has stopped. */
final class CaveMiningScene {
    int feedback;
    float clock,rumble,pulse,spillAge=9;
    void reset(){feedback=0;clock=rumble=pulse=0;spillAge=9;}
    int takeFeedback(){int n=feedback;feedback=0;return n;}
    void update(float dt){clock+=dt;rumble=Math.max(0,rumble-dt*3);pulse=Math.max(0,pulse-dt);spillAge+=dt;}
}
