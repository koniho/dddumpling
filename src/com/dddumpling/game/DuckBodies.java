package com.dddumpling.game;

/** Owned simulation, advanced only by update; rendering never mutates a duck's springs. */
final class DuckBodies {
    final Softbody[][] skin=new Softbody[Collect.DUCK_COUNT][4];
    private int reaction=-1;
    private float reactionAge;
    DuckBodies() {
        for(int duck=0;duck<skin.length;duck++)for(int layer=0;layer<4;layer++) {
            Softbody body=new Softbody(12,156+duck*4+layer);
            body.reset(0,0,1);body.jiggle=layer==1?.8f:1.1f;
            skin[duck][layer]=body;
        }
    }
    void react(int who,float amount) {
        int duck=who-Collect.DUCK_FIRST;
        if(duck<0 || duck>=skin.length)return;
        for(int layer=0;layer<4;layer++) {
            skin[duck][layer].squash(amount*(1f-layer*.15f));
            skin[duck][layer].impulse(.6f,-.5f,amount*.16f);
        }
    }
    void update(GameCore c,float dt) {
        if(c.paused || c.settingsOpen)return;
        if(c.companion.reaction!=reaction || c.companion.age<reactionAge)
            react(c.companion.who,c.companion.strength*.5f);
        reaction=c.companion.reaction;reactionAge=c.companion.age;
        for(int i=0;i<skin.length;i++) {
            int who=Collect.DUCK_FIRST+i;
            if(who!=c.runWho && who!=c.survival.reward.who && who!=c.story
                    && !(c.state==GameCore.TITLE && Collect.has(c.collected,who)))continue;
            for(Softbody layer:skin[i])layer.update(dt);
        }
    }
}
