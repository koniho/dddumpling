package com.dddumpling.game;

/** Body-relative hat brim and sole positions, in Collect.SHAPE order. Faces use Trinket's anchors.
 * Ducks and boss friends supply their moving head/feet directly from their own renderers. */
final class AccessoryAnchors {
    static final float[] TOP={-.62f,-.70f,-.70f,-.68f,-.16f,-.56f,-.37f,-.69f,-.78f,-.76f,-.72f,-.69f,-.82f,-.91f,-.78f,-.82f,-.74f};
    static final float[] FEET={.64f,.71f,.72f,.68f,.72f,.72f,.88f,.80f,.76f,.78f,.75f,.75f,.94f,.90f,.84f,.78f,.76f};
    static float hatTop(int shape,int hat) {
        // Seat the brim into pointed crowns without moving the body's flair bounds.
        float inset=shape==Collect.STAR?.28f:shape==Collect.SHELL?.18f:
                shape==Collect.DROP?.20f:shape==Collect.POME?.10f:0f;
        // The cap's flat band needs more overlap than a curved brim on round heads.
        if(hat>=0 && Accessories.variant(hat)==4 && (shape==Collect.BUN || shape==Collect.FIN))inset+=.08f;
        return TOP[shape]+inset;
    }
    // Face width stays aligned to the eyes; only the cloth's vertical fit changes.
    private static final float[] MASK_HEIGHT={.76f,.80f,.74f,.84f,.52f,.78f,.80f,.90f,.94f,.82f,.86f,.80f,.60f,.90f,.78f,1f,.86f};
    static float maskHeight(int who,int variant) {
        if(who==Collect.ALLIGATOR_FRIEND)return variant==0?.68f:.70f;
        if(who==Collect.RAINBOW_CAT_FRIEND)return variant==0?.74f:.86f;
        if(who>=Collect.DUCK_FIRST && who<Collect.DUCK_FIRST+Collect.DUCK_COUNT)return variant==0?.78f:.80f;
        if(Collect.bossKind(who)>=0)return 1;
        float h=MASK_HEIGHT[Collect.SHAPE[who]];
        return variant==0?h:Math.min(1,h*1.22f);
    }
    static float maskWidth(int who,int variant) {
        if(who>=Collect.DUCK_FIRST && who<Collect.DUCK_FIRST+Collect.DUCK_COUNT)return variant==0?.74f:.88f;
        if(who==Collect.ALLIGATOR_FRIEND)return variant==0?.90f:.96f;
        if(who==Collect.RAINBOW_CAT_FRIEND)return .86f;
        return 1;
    }
}
