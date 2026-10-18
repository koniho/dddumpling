package com.dddumpling.game;

/** Append-only world identities. One row owns each boss, its chapter, friend and presentation. */
final class WorldCatalog {
    static final int STAGES_PER_CHAPTER=5;
    static final class Land {
        final int background,tint,ending;
        final String death;
        Land(int background,int tint,int ending,String death) {
            this.background=background;this.tint=tint;this.ending=ending;this.death=death;
        }
    }
    // Land array indices are saved IDs. Cave's reserved row remains in place.
    static final Land[] LANDS={
        new Land(Glyph.mix(Draw.BG,0xFF142A29,.5f),0xFF79C99D,5,"Slime hills hugged back."),
        new Land(Glyph.mix(Draw.BG,0xFF21182F,.5f),0xFFB18CDB,6,"Crystal sparkle nap. Very shiny."),
        new Land(Glyph.mix(Draw.BG,0xFF102735,.5f),0xFF62BEC5,7,"Kelp tucked you in. Bubble blanket!"),
        new Land(Glyph.mix(Draw.BG,0xFF241F2A,.5f),0xFFE4A19C,8,"Mushroom cap became your pillow."),
        new Land(CaveArt.DARK,CaveArt.LIGHT,9,"Pebble adopted its own dumpling."),
        new Land(0xFF182F32,0xFF74B99C,10,"Lily pad booked you a swamp nap."),
        new Land(0xFF282341,0xFFDEA9EC,13,"A rainbow cloud borrowed you.")
    };
    static final class BossEntry {
        final int land,friend,face,ending;
        final String name,halloween,analytics,blurb,death;
        final float hp,wide,jiggle,cycle,show,taunt;
        final float[] voice;
        BossEntry(int land,int friend,int face,int ending,String name,String halloween,String analytics,
                String blurb,String death,float hp,float wide,float jiggle,float cycle,float show,float taunt,float[] voice) {
            this.land=land;this.friend=friend;this.face=face;this.ending=ending;this.name=name;
            this.halloween=halloween;this.analytics=analytics;this.blurb=blurb;this.death=death;
            this.hp=hp;this.wide=wide;this.jiggle=jiggle;this.cycle=cycle;this.show=show;this.taunt=taunt;this.voice=voice;
        }
    }
    // Boss index is its durable ID and chapter order. Friend/death IDs are explicitly stable.
    static final BossEntry[] BOSSES={
        new BossEntry(0,45,Kawaii.SQUISHY,1,"SUPER SLIME","SUPER GHOST","slime","HIT THE MARK, DRAG GLOBS"," gave you a goo hug.",4,2,2,5,4,330,new float[]{1,1.26f,.92f,1.38f}),
        new BossEntry(1,46,Kawaii.SQUISHY,2,"DARK DIVIDE","BUBBLE GUM","dark_divide","HIT THE MARK, THEN PINCH OUT"," cubed your cuteness.",8,1.65f,1.7f,1,1,275,new float[]{1,.71f,1.41f,.59f}),
        new BossEntry(2,47,Kawaii.BLOB,3,"OCTOPULSE","GHASTLY SQUID","octopulse","BEAT THE REACH",": eight hugs too many.",8,1.35f,2.2f,1,1,150,new float[]{1,1.06f,.89f,1.12f}),
        new BossEntry(3,48,Kawaii.DUMPLING,4,"FLY AGARIC","POISON CAP","fly_agaric","DRAG BACK AND FORTH"," made you a spore snack.",3,1.45f,1.9f,1,1,205,new float[]{1,.76f,1.34f,.63f}),
        new BossEntry(5,60,Kawaii.SQUISHY,11,"GUMMY GATOR","COSTUMED CROC","mossy_maw","PARRY, TAP, THEN STOP"," gave you a tummy hug.",72,2.7f,1.6f,1,1,180,new float[]{1,1.33f,1.12f,.84f}),
        new BossEntry(6,61,Kawaii.CAT,12,"RAINBOW CAT",null,"rainbow_cat","DRAG YARN, CATCH THE CAT"," tangled you in rainbow yarn.",3,1.15f,1.6f,1,1,420,new float[]{1,1.5f,1.25f,1.8f})
    };
    static int bossStage(int boss) {return (boss+1)*STAGES_PER_CHAPTER;}
    static int bossForLand(int land) {
        for(int boss=0;boss<BOSSES.length;boss++)if(BOSSES[boss].land==land)return boss;
        return -1;
    }
    static int bossForEnding(int ending) {
        for(int boss=0;boss<BOSSES.length;boss++)if(BOSSES[boss].ending==ending)return boss;
        return -1;
    }
    static int bossForFriend(int friend) {
        for(int boss=0;boss<BOSSES.length;boss++)if(BOSSES[boss].friend==friend)return boss;
        return -1;
    }
    private WorldCatalog() {}
}
