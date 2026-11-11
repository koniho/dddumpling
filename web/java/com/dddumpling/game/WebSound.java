package com.dddumpling.game;
import org.teavm.jso.JSBody;

/** Native event/pitch/gain mappings backed by pre-exported PCM and browser audio. */
final class WebSound implements GameCore.Sound {
    private int style;
    private boolean boss,fast;
    private void play(int id,float rate) {play(id,rate,1);}
    @JSBody(params={"id","rate","gain"},script="ddd.audio.effect('sfx-'+id,rate,gain);") private static native void play(int id,float rate,float gain);
    @JSBody(params={"name","rate","gain"},script="ddd.audio.loop(name,rate,gain);") private static native void loop(String name,float rate,float gain);
    @JSBody(params={"name","gain"},script="ddd.audio.music(name,gain);") private static native void music(String name,float gain);
    @JSBody(params={"m","s"},script="ddd.audio.volumes(m,s);") private static native void setVolumes(float m,float s);
    public void volumes(float m,float s) {setVolumes(m,s);}
    private void track() {music((boss?"boss-":fast?"frenzy-":"music-")+style,boss?Music.BOSS_GAIN:1);}
    public void selectMusic(int value) {style=Math.max(0,Math.min(Music.NAMES.length-1,value));track();}
    public void bossMusic(boolean value) {boss=value;track();}
    public void frenzy(boolean value) {fast=value;track();}
    public void versusMusic() {music("versus",1);}
    public void rocket(float amount) {loop("rocket",.82f+.43f*amount,amount<=0?0:.16f+.28f*amount);}
    public void bossCharge(float amount) {loop("bubble",1,Math.max(0,amount)*.10f);}
    public void boltPop() {play(Sfx.BOLT_POP,1,.68f);}
    public void narrate(int entry) {StringBuilder s=new StringBuilder();for(int i=0;i<Narration.CHUNKS;i++)s.append(Narration.lines(entry)[i]).append(" ");explain(s.toString());}
    public void explain(String text) {speak(text,Narration.PITCH,Narration.RATE,1);}
    public void companionExclaim(String text) {speak(text,Narration.PITCH,Narration.RATE,Narration.COMPANION_GAIN);}
    public void announceSquishy(int entry) {speak(Narration.name(entry),Narration.NAME_PITCH,Narration.NAME_RATE,1);}
    public void announceVersus(int entry,int boss,int skin) {speak(Narration.versus(entry,boss,skin),Narration.VERSUS_PITCH,Narration.VERSUS_RATE,1);}
    @JSBody(params={"text","pitch","rate","gain"},script="ddd.audio.speak(text,pitch,rate,gain);") private static native void speak(String text,float pitch,float rate,float gain);
    @JSBody(script="ddd.audio.hush();") private static native void stopSpeech();
    public void hush() {stopSpeech();}
    public void squish(int glyph, int depth) {
        // Deeper remaining stacks sound lower and rounder, so a stack audibly counts down.
        play(Sfx.squishId(glyph), (float) Math.pow(0.92, Math.max(0, depth - 1)));
    }
    public void ninjaCombo() { play(Sfx.NINJA_COMBO,1f,Sfx.NINJA_COMBO_GAIN); }
    public void ninjaSwish(int variant) {
        play(Sfx.ninjaSwishId(variant), 1f);
    }
    public void zap(int hop) {
        // Climbs with the chain, capped so a long one stays a crack rather than turning into a
        // chirp — and well inside what setPlaybackRate will take.
        int step = hop < 1 ? 0 : hop > 9 ? 8 : hop - 1;
        play(Sfx.ZAP, 1f + 0.055f * step);
    }
    public void collect(int nth) {
        // Climbs as the case fills, so a haul of four sounds like a run up the shelf rather than
        // the same chime four times. Capped well inside what setPlaybackRate will take.
        int step = nth < 0 ? 0 : nth > 7 ? 7 : nth;
        play(Sfx.COLLECT, 1f + 0.05f * step);
    }
    public void star(int nth) {
        // Climbs the whole way up a course, so twenty stars are a ladder rather than one note
        // twenty times — that ladder is most of what tells you how the course is going without
        // looking at the counter. Smaller steps than the shelving chime because there are twenty of
        // them and not four: at the chime's 0.05 the last few came out as chirps.
        int step = nth < 1 ? 0 : nth > 16 ? 16 : nth - 1;
        play(Sfx.STAR, 1f + 0.032f * step);
    }
    public void courseStart() {
        play(Sfx.COURSE, 1f);
    }
    public void courseFinish() {
        play(Sfx.BLAST_OFF, 1f);
    }
    public void tally(int nth) {
        // Rises with the count, so how the attempt went is audible before the number is read. A
        // wider step than the pickup ladder and over a longer count: this fires once, so there is
        // nothing for it to blend with, and the two ends want to be plainly different tones.
        int step = nth < 0 ? 0 : nth > 20 ? 20 : nth;
        play(Sfx.TALLY, 0.94f + 0.018f * step);
    }
    public void paradeJoin() {
        play(Sfx.JOIN, 1f);
    }
    public void rosterJoin() {
        play(Sfx.ROSTER_JOIN, 1f);
    }
    public void gameOver() {
        play(Sfx.OVER, 1f);
    }
    public void survivalGameOver() { play(Sfx.SURVIVAL_OVER, 1f); }
    public void clearWord() {
        play(Sfx.CLEAR, 1f);
    }
    public void shuffleBlip() { play(Sfx.SHUFFLE_BLIP, 1f, 0.3f); }
    public void accessoryChange(int slot,boolean removing) { play(Sfx.accessoryId(slot,removing),1f,Sfx.ACCESSORY_GAIN); }
    public void uiBloop() { play(Sfx.UI_BLOOP, 1f, 0.22f); }
    public void scoreResetConfirm() { play(Sfx.SCORE_RESET_CONFIRM,1f,.7f); }
    public void scoreResetBrush(int stroke) {
        play(Sfx.SCORE_RESET_BRUSH,1f+.035f*Math.max(0,Math.min(2,stroke)),.62f);
    }
    public void landShuffle() { play(Sfx.LAND_SHUFFLE, 1f, 0.16f); }
    public void slimeCover(boolean release) { play(release ? Sfx.SLIME_RELEASE : Sfx.SLIME_COVER, 1f, 0.65f); }
    public void debuffDown() { play(Sfx.DEBUFF_DOWN, 1f, 0.8f); }
    public void caveEvent(int sound) { play(sound, 1f, .85f); }
    public void linkedThud() { play(Sfx.LINKED_THUD, 1f, 0.85f); }
    public void wrong() {
        play(Sfx.WRONG, 1f);
    }
    public void damage() {
        play(Sfx.DRIP, 1f);
    }
    public void achievement() {
        play(Sfx.ACHIEVEMENT, 1f);
    }
    public void bossLaugh() {
        play(Sfx.BOSS_LAUGH, 1f);
    }
    public void bossTaunt(int kind) {
        if (kind >= 0 && kind < Boss.COUNT) play(Sfx.bossTauntId(kind), 1f, 0.78f);
    }
    public void bossDamage() {
        play(Sfx.BOSS_DAMAGE, 0.92f, 0.72f);
    }
    public void bossSplit() {
        play(Sfx.BOSS_SPLIT, 1.08f, 0.68f);
    }
    public void divideDamage() { play(Sfx.DIVIDE_DAMAGE, 1f, 0.76f); }
    public void divideSplit() { play(Sfx.DIVIDE_SPLIT, 1f, 0.78f); }
    public void divideDeactivate() { play(Sfx.DIVIDE_DEACTIVATE, 1f, 0.82f); }
    public void divideSupernova() { play(Sfx.DIVIDE_SUPERNOVA, 1f, 0.95f); }
    public void divideBoing(float weight) {
        float w = Math.max(0f, Math.min(1f, weight));
        int id = w >= 0.67f ? Sfx.DIVIDE_BOING_HEAVY
                : w >= 0.34f ? Sfx.DIVIDE_BOING_MEDIUM : Sfx.DIVIDE_BOING_LIGHT;
        play(id, 1f, 0.58f + 0.16f * w);
    }
    public void shieldBounce() { play(Sfx.SHIELD_BOUNCE, 1f, 0.72f); }
    public void gatorParry() { play(Sfx.GATOR_PARRY, 1f); }
    public void catLaser() { play(Sfx.CAT_LASER,1f,Sfx.CAT_LASER_GAIN); }
    public void catFlurry(int note,int count) { play(Sfx.catFlurryId(note,count),1f,Sfx.CAT_FLURRY_GAIN); }
    public void catMeow(int variant) { play(Sfx.catMeowId(variant),1f,Sfx.CAT_MEOW_GAIN); }
    public void gatorTaunt(int variant) { play(Sfx.gatorTauntId(variant), 1f, Sfx.GATOR_TAUNT_GAIN); }
    public void gatorPrompt(int variant) { play(Sfx.gatorPromptId(variant), 1f, Sfx.GATOR_PROMPT_GAIN); }
    public void gatorSplash(boolean entry) { play(Sfx.GATOR_SPLASH, entry?Sfx.GATOR_ENTRY_RATE:1f, Sfx.GATOR_SPLASH_GAIN); }
    public void slimeDragBoip() { play(Sfx.SLIME_DRAG_BOIP,1f,Sfx.SLIME_DRAG_BOIP_GAIN); }
    public void slimeVulnerable() { play(Sfx.SLIME_VULNERABLE,1f,Sfx.SLIME_VULNERABLE_GAIN); }
    public void slimeSquish(int variant) { play(Sfx.slimeSquishId(variant), 1f, Sfx.SLIME_SQUISH_GAIN); }
    public void octoWave() { play(Sfx.OCTO_WAVE, 1f, Sfx.OCTO_WAVE_GAIN); }
    public void octoDamage() { play(Sfx.OCTO_DAMAGE, 1f, .82f); }
    public void octoCue() { play(Sfx.OCTO_CUE, 1f, 0.74f); }
    public void octoLock() { play(Sfx.OCTO_LOCK, 1f, 0.70f); }
    public void mushroomShake() { play(Sfx.MUSHROOM_SHAKE, 1f, 0.78f); }
    public void mushroomSpore() { play(Sfx.MUSHROOM_SPORE, 1f, 0.72f); }
    public void boltDeath() { play(Sfx.BOLT_DEATH, 1f, 0.82f); }
    public void gameStart() {
        play(Sfx.START, 1f, 0.72f);
    }
    public void stageClear() {
        play(Sfx.STAGE_CLEAR, 1f);
    }
    public void powerClear() {
        play(Sfx.POWER_CLEAR, 1f);
    }
}
