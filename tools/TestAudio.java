package com.dddumpling.game;

/** Effect normalisation and which sound fires on which event. */
final class TestAudio extends Check {

    /** What the two frenzy squish sounds are, and that they are the right shape for the job. */
    static void frenzySounds(Layout L) {
        group("frenzy sounds");
        check("mining cheer is a short voiced phrase",Sfx.build(Sfx.MINING_CHEER).length<Sfx.RATE*.6f && crossRate(Sfx.build(Sfx.MINING_CHEER))<3000);
        for(int id=Sfx.CAVE_RUMBLE;id<=Sfx.CAVE_SINK;id++){
            short[] effect=Sfx.build(id);int head=0,tail=0;
            for(int i=0;i<effect.length/2;i++)head=Math.max(head,Math.abs(effect[i]));
            for(int i=effect.length*3/4;i<effect.length;i++)tail=Math.max(tail,Math.abs(effect[i]));
            check("cave cue fits between impacts "+id,effect.length<Sfx.RATE*CaveTraps.GAP && head>1000);
            check("cave cue fades before next impact "+id,tail<head/4);
        }
        for(int id=Sfx.CAVE_RUMBLE;id<=Sfx.CART_TUMBLE;id++){
            if(id==Sfx.MINING_CHEER)continue;
            short[] effect=Sfx.build(id);
            double total=0,lowEnergy=0,phoneEnergy=0,lo=0,hi=0;
            int peak=0;
            for(short value:effect){
                lo+=.025*(value-lo);hi+=.16*(value-hi);
                total+=(double)value*value;lowEnergy+=lo*lo;phoneEnergy+=(hi-lo)*(hi-lo);
                peak=Math.max(peak,Math.abs(value));
            }
            System.out.printf("    cave audio %d: %.0f ms, low %.2f, phone %.2f%n",id,
                    effect.length*1000f/Sfx.RATE,lowEnergy/total,phoneEnergy/total);
            check("cave sound has low body "+id,lowEnergy/total>.12);
            check("cave sound survives small speakers "+id,phoneEnergy/total>.075);
            check("cave sound leaves mixing headroom "+id,peak<30000);
            check("ride cue finishes before repeat "+id,id<Sfx.CART_ROLL || effect.length<=Sfx.RATE*.35f);
        }
        short[] bloop=Sfx.build(Sfx.UI_BLOOP);
        check("UI bloop stays brief",bloop.length>Sfx.RATE*.07f && bloop.length<Sfx.RATE*.15f);
        check("UI bloop is tonal",crossRate(bloop)>300f && crossRate(bloop)<1200f);
        int bloopHead=0,bloopTail=0;
        for(int i=0;i<bloop.length/2;i++) bloopHead=Math.max(bloopHead,Math.abs(bloop[i]));
        for(int i=bloop.length*3/4;i<bloop.length;i++) bloopTail=Math.max(bloopTail,Math.abs(bloop[i]));
        check("UI bloop fades softly",bloopTail<bloopHead/4);

        // The chop fires several times per swipe, so anything with a tail would smear.
        short[] chop = Sfx.build(Sfx.CHOP);
        float chopLen = (float) chop.length / Sfx.RATE;
        System.out.printf("    chop is %.0fms, squish is %.0fms%n", chopLen * 1000f,
                1000f * Sfx.build(Sfx.SQUISH_0).length / Sfx.RATE);
        check("the chop is short enough to repeat", chopLen < 0.09f);
        check("and shorter than a squish",
                chop.length < Sfx.build(Sfx.SQUISH_0).length);
        // It has to actually decay, or a run of them builds into a wash.
        int head = 0, tail = 0;
        for (int i = 0; i < chop.length / 4; i++) head = Math.max(head, Math.abs(chop[i]));
        for (int i = chop.length * 3 / 4; i < chop.length; i++) {
            tail = Math.max(tail, Math.abs(chop[i]));
        }
        check("the chop dies away", tail < head / 4);

        // Body, not hiss. A noise burst crosses zero constantly; a sound with a tone under it
        // does not, so the crossing rate is the cheapest measure that tells them apart. The
        // first chop was noise alone and audibly a hiss.
        System.out.printf("    zero-crossings per second: chop %.0f, squish %.0f, wrong %.0f%n",
                crossRate(chop), crossRate(Sfx.build(Sfx.SQUISH_0)),
                crossRate(Sfx.build(Sfx.WRONG)));
        check("the chop has body under the air", crossRate(chop) < 3500f);
        check("but is still brighter than a squish",
                crossRate(chop) > crossRate(Sfx.build(Sfx.SQUISH_0)));

        short[] bolt = Sfx.build(Sfx.BOLT_POP);
        check("the bolt explosion stays compact",
                bolt.length >= Sfx.RATE * 0.18f && bolt.length <= Sfx.RATE * 0.25f);
        check("the bolt explosion has brighter debris than a rounded squish",
                crossRate(bolt) > crossRate(Sfx.build(Sfx.SQUISH_0)));

        // The chain crack: it has to hit hard and immediately, which is the whole difference
        // between a bolt and a fizz.
        short[] zap = Sfx.build(Sfx.ZAP);
        int peakAt = 0, peak = 0;
        for (int i = 0; i < zap.length; i++) {
            if (Math.abs(zap[i]) > peak) {
                peak = Math.abs(zap[i]);
                peakAt = i;
            }
        }
        float toPeak = 1000f * peakAt / Sfx.RATE;
        System.out.printf("    zap is %.0fms, peaks at %.1fms, %.0f crossings/s%n",
                1000f * zap.length / Sfx.RATE, toPeak, crossRate(zap));
        check("the zap hits immediately", toPeak < 12f);
        check("it is the bigger event of the two", zap.length > chop.length);
        check("it has low-end punch, not just crackle", crossRate(zap) < 3000f);
        int zTail = 0;
        for (int i = zap.length * 3 / 4; i < zap.length; i++) {
            zTail = Math.max(zTail, Math.abs(zap[i]));
        }
        check("and it decays", zTail < peak / 6);

        // MULTI: one crack per hop, climbing, and never a squish.
        GameCore m = new GameCore(new Mem(), 417L);
        Ear earM = new Ear();
        m.sound = earM;
        m.startGame();
        m.enemies.clear();
        m.target = null;
        m.startFrenzy(Power.MULTI, L);
        add(m, L, new int[] {1, 1}, L.playTop + 200f);
        add(m, L, new int[] {1, 1}, L.playTop + 320f);
        m.tapKey(1, L);
        check("the chain took four", m.chainLen == 4);
        check("silent until it plays back", earM.zaps == 0);
        advance(m, L, GameCore.CHAIN_TIME);
        check("one crack per hop", earM.zaps == 4);
        check("the last one is the highest", earM.lastZapHop == 4);
        check("no squishes in a chain", earM.squishes == 0);

        // FLING: one chop per letter the blade cuts, and no fanfare.
        GameCore c = new GameCore(new Mem(), 411L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.enemies.clear();
        c.target = null;
        c.startFrenzy(Power.FLING, L);
        GameCore.Enemy e = add(c, L, new int[] {1, 2, 3, 4}, L.playTop + 300f);
        int chops = ear.chops, cheers = ear.achievements;
        c.beginStroke(c.tileX(e, 0, L) - L.enemyR * 2f, e.y);
        int cut = c.sliceTo(c.tileX(e, 3, L) + L.enemyR * 2f, e.y, L);
        check("the blade cut the word", cut == 4);
        check("one chop per letter cut", ear.chops - chops == 4);
        check("and no fanfare for a single word", ear.achievements == cheers);
        // The chops are the word's sound. A clear tone on top lands on the last one.
        check("a word cut by the blade rings no clear tone", ear.clears == 0);
        c.endStroke();

        // Typed, it still does — the tone is what tells you a word is finished when there is no
        // chop to say so.
        GameCore t2 = new GameCore(new Mem(), 415L);
        Ear ear3 = new Ear();
        t2.sound = ear3;
        t2.startGame();
        t2.enemies.clear();
        GameCore.Enemy typed = add(t2, L, new int[] {1, 2}, L.playTop + 200f);
        t2.tapKey(1, L);
        t2.tapKey(2, L);
        advance(t2, L, 0.4f);
        check("a typed word still rings", typed.destroyed && ear3.clears == 1);
        check("and rang no chop", ear3.chops == 0);

        // TEAM SQUISH: a squish per word, not the achievement flourish — it fires far too often
        // for that, which is what it used to do.
        Mem store = new Mem();
        store.collected = (1L << 7) | (1L << 18);
        GameCore d = new GameCore(store, 413L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.startGame();
        d.enemies.clear();
        d.target = null;
        d.playtestMode(Power.TEAM, L);
        GameCore.Enemy prey = add(d, L, new int[] {2, 2}, d.buddy.y);
        prey.baseX = d.buddy.x;
        int sq = ear2.squishes, fan = ear2.achievements;
        d.update(DT, L);
        check("the squishy took the word", prey.destroyed);
        check("it squishes", ear2.squishes == sq + 1);
        check("and does not blow the achievement fanfare", ear2.achievements == fan);
        check("the squish is pitched by its size", ear2.lastDepth >= 1);
        check("on a letter of the word it took", ear2.lastGlyph == 2);

        // Bigger squishy, deeper squish: depth climbs, and depth is what lowers the pitch.
        d.buddy.squishes = 8;
        GameCore.Enemy again = add(d, L, new int[] {5, 5}, d.buddy.y);
        again.baseX = d.buddy.x;
        int wasDepth = ear2.lastDepth;
        d.update(DT, L);
        check("a grown squishy sounds deeper", ear2.lastDepth > wasDepth);
    }

    /** Zero crossings per second, as a rough stand-in for how tonal a buffer is. */
    private static float crossRate(short[] pcm) {
        int crossings = 0;
        for (int i = 1; i < pcm.length; i++) {
            if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++;
        }
        return crossings * (float) Sfx.RATE / pcm.length;
    }

    static void musicChoice(Layout L) {
        group("automatic music");
        GameCore c = new GameCore(new Mem(),301L);
        Ear ear = new Ear(); c.sound=ear;
        check("music waits for backend attachment",ear.musicCalls==0);
        c.startMusic();
        check("title automatically uses MOOG SWING",ear.music==Music.SWING_STYLE && ear.musicCalls==1);
        new GameCore(new Mem(),302L).startMusic();
        c.state=GameCore.PLAY; c.jumpToStage(21,L);
        check("cave entry selects LOFI DRIFT",ear.music==Music.DRIFT);
        c.preferences.music=.35f;c.preferences.musicMuted=true;c.preferences.save(c);
        c.jumpToStage(24,L);c.startMusic();
        check("cave resume preserves mute and automatic track",ear.music==Music.DRIFT && ear.musicVolume==0f);
        c.preferences.musicMuted=false;c.preferences.save(c);
        check("unmute restores volume without restarting music",ear.musicVolume==.35f && ear.music==Music.DRIFT);
        c.jumpToStage(19,L);
        check("leaving caves restores MOOG SWING",ear.music==Music.SWING_STYLE);
        c.jumpToStage(20,L);check("ordinary bosses retain boss music",ear.bossMusic);
        c.jumpToStage(21,L);
        check("boss to cave switches to LOFI DRIFT",!ear.bossMusic && ear.music==Music.DRIFT);
        c.toTitle();check("title restores MOOG SWING",ear.music==Music.SWING_STYLE);
        c.allLandsEnabled=true;c.landChoice=Cave.LAND;c.startGame();
        check("starting directly in caves selects LOFI DRIFT",ear.music==Music.DRIFT);
    }

    static void audio(Layout L) {
        group("audio");
        int peak = (int) (Sfx.PEAK * 32767f);
        boolean allNormalised = true, allClean = true, allSane = true;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            // Peak-normalised: every effect tops out at the same level.
            if (Math.abs(max - peak) > 2) allNormalised = false;
            if (max >= 32767) allClean = false;
            // Roulette ticks intentionally fit between fast icon changes.
            int minimum = id == Sfx.SHUFFLE_BLIP ? Sfx.RATE / 40 : Sfx.RATE / 20;
            if (pcm.length < minimum || pcm.length > Sfx.RATE * 2) allSane = false;
        }
        check("every effect is normalised to the same peak", allNormalised);
        check("no effect clips", allClean);
        check("effect lengths are sane", allSane);

        short[] divideHit = Sfx.build(Sfx.DIVIDE_DAMAGE);
        short[] divideSplit = Sfx.build(Sfx.DIVIDE_SPLIT);
        check("Dark Divide damage is shorter than an ordinary boss hit",
                divideHit.length < Sfx.build(Sfx.BOSS_DAMAGE).length);
        check("its split is the larger, lingering event",
                divideSplit.length > Sfx.build(Sfx.BOSS_SPLIT).length
                        && divideSplit.length > divideHit.length * 2);
        check("its damage cue starts with an immediate crack",
                peakAt(divideHit) < Sfx.RATE / 100);

        short[] heavyBoing = Sfx.build(Sfx.DIVIDE_BOING_HEAVY);
        short[] mediumBoing = Sfx.build(Sfx.DIVIDE_BOING_MEDIUM);
        short[] lightBoing = Sfx.build(Sfx.DIVIDE_BOING_LIGHT);
        check("Dark Divide has three collision voices",
                heavyBoing != mediumBoing && mediumBoing != lightBoing);
        check("large pieces have a longer, heavier boing",
                heavyBoing.length > mediumBoing.length && mediumBoing.length > lightBoing.length);
        check("small pieces have the springiest, highest boing",
                crossings(lightBoing) > crossings(mediumBoing)
                        && crossings(mediumBoing) > crossings(heavyBoing));

        short[] loop = Music.loop(Music.SWING_STYLE);
        check("music loop is the expected length", loop.length == Music.loopFrames(Music.SWING_STYLE));
        check("music loop is several seconds", loop.length > Sfx.RATE * 5);
        int lmax = 0;
        for (int i = 0; i < loop.length; i++) lmax = Math.max(lmax, Math.abs(loop[i]));
        check("music sits below the effects", lmax < peak);
        check("music is audible", lmax > peak / 4);

        short[] bossLoop = Music.bossLoop(Music.SWING_STYLE);
        int bossMax = 0;
        for (int i = 0; i < bossLoop.length; i++)
            bossMax = Math.max(bossMax, Math.abs(bossLoop[i]));
        for(int style=0;style<Music.NAMES.length;style++) {
            if(!Music.isSynth(style)) continue;
            short[] normal=Music.loop(style,false), battle=Music.bossLoop(style);
            double normalEnergy=0,bossEnergy=0;
            for(short sample:normal) normalEnergy+=(double)sample*sample;
            int maxBoss=0;
            for(short sample:battle) {bossEnergy+=(double)sample*sample;maxBoss=Math.max(maxBoss,Math.abs(sample));}
            double ratio=Math.sqrt(bossEnergy/battle.length/(normalEnergy/normal.length))*Music.BOSS_GAIN;
            check("boss music sits slightly above stage music " + style,ratio>1.13 && ratio<1.17);
            check("boss mix retains effect headroom " + style,maxBoss*Music.BOSS_GAIN+peak*0.72f<32767);
        }
        check("boss progression spans thirty-two bars", Music.bossBars() == 32);
        check("boss melody is quantized to eighth notes", Music.bossMelodyOnEighths());
        check("boss loop is at least eight old four-bar loops",
                bossLoop.length > Sfx.RATE * 37);
        check("and keeps the same audible instrument bed", bossMax > peak / 4);
        check("boss music leaves mix headroom for a peak effect",
                bossMax * Music.BOSS_GAIN + peak * 0.72f < 32767f);
        check("the boss loop joins without a click",
                Math.abs(bossLoop[0] - bossLoop[bossLoop.length - 1]) < peak / 5);

        // Effects must fire on the right events.
        Ear ear = new Ear();
        GameCore c = new GameCore(new Mem(), 71L);
        c.sound = ear;
        c.startGame();
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy e = add(c, L, new int[] {3, 1}, new int[] {2, 1}, L.playTop + 80);

        c.tapKey(3, L);
        check("a correct press squishes", ear.squishes == 1);
        check("the squish knows the letter", ear.lastGlyph == 3);
        check("the squish knows the stack depth", ear.lastDepth == 2);
        c.tapKey(0, L);
        check("a wrong press thunks", ear.wrongs == 1);
        check("a wrong press does not squish", ear.squishes == 1);

        c.tapKey(3, L);
        c.tapKey(3, L);
        c.tapKey(1, L);
        check("no clear sound before the shot lands", ear.clears == 0);
        advance(c, L, 0.3f);
        check("clearing a word plays the clear sound", ear.clears == 1);

        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("taking damage plays the drip", ear.damages == 1);

        Ear ear2 = new Ear();
        GameCore g = new GameCore(new Mem(), 72L);
        g.sound = ear2;
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        advancePastBonus(g, L);
        check("a flawless wave plays the achievement", ear2.achievements == 1);

        Ear ear3 = new Ear();
        GameCore m = new GameCore(new Mem(), 73L);
        m.sound = ear3;
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);                      // a miss forfeits the reward
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        check("a flawed wave plays no achievement", ear3.achievements == 0);

        check("a null sound seam is safe", silentRunSurvives(L));
    }

    /**
     * The haul's flight home: one chime per dumpling, as it is taken into the case.
     *
     * The flight is drawn from {@link RoundEnd#arrival}, and the sounds are fired off the same
     * function, so this suite is really asserting that the two halves cannot drift: every landing
     * announced exactly once, in order, and all of them inside the flight.
     */
    static void haulLanding(Layout L) {
        group("shelving the haul");

        // Arrivals have to be spread out, or the chimes stack into one chord. They used to.
        for (int count = 1; count <= 8; count++) {
            boolean rising = true, inside = true;
            for (int n = 0; n < count; n++) {
                float a = RoundEnd.arrival(n, count);
                if (n > 0 && a <= RoundEnd.arrival(n - 1, count)) rising = false;
                if (a <= 0f || a > 1.0001f) inside = false;
                if (RoundEnd.trip(a, n, count) < 0.999f) inside = false;
            }
            check("a haul of " + count + " lands one at a time", rising);
            check("a haul of " + count + " lands inside the flight", inside);
        }
        check("the last one lands as the flight ends",
                Math.abs(RoundEnd.arrival(4, 5) - 1f) < 1e-4f);
        System.out.printf("    a haul of 5 lands at %.0f, %.0f, %.0f, %.0f, %.0fms%n",
                RoundEnd.arrival(0, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(1, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(2, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(3, 5) * GameCore.HOME_TIME * 1000f,
                RoundEnd.arrival(4, 5) * GameCore.HOME_TIME * 1000f);

        // A run that ended with three freed dumplings, sent to the title screen.
        Mem store = new Mem();
        GameCore c = new GameCore(store, 811L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.state = GameCore.OVER;
        c.roundPrizes = Collect.add(Collect.add(Collect.add(0L, 2), 9), 21);
        c.toTitle();
        check("the flight is on", c.homing() && RoundEnd.hauled(c) == 3);
        check("and nothing has been shelved yet", ear.collects == 0 && c.homeLanded == 0);

        // Nothing may land early, and the first one must land before the flight is over.
        advance(c, L, GameCore.HOME_TIME * RoundEnd.lead(0, 3) + DT);
        check("no chime while they are still in the air", ear.collects == 0);
        advance(c, L, GameCore.HOME_TIME);
        check("every one of them is announced", ear.collects == 3);
        check("once each, in the order they fly",
                ear.shelved.equals(java.util.Arrays.asList(0, 1, 2)));
        check("and the flight is over", !c.homing());
        advance(c, L, 2f);
        check("nothing chimes again afterwards", ear.collects == 3);

        // An empty-handed run has nothing to shelve and no flight to do it in.
        Ear quiet = new Ear();
        GameCore d = new GameCore(store, 813L);
        d.sound = quiet;
        d.startGame();
        d.state = GameCore.OVER;
        d.toTitle();
        advance(d, L, GameCore.HOME_TIME + 0.5f);
        check("an empty-handed run shelves nothing", quiet.collects == 0 && !d.homing());

        // Short and decaying, like the chop and the crack: several land a tenth of a second apart
        // and Audio gives every effect one track, so a tail would smear into the next landing.
        short[] chime = Sfx.build(Sfx.COLLECT);
        float len = (float) chime.length / Sfx.RATE;
        int head = 0, tail = 0;
        for (int i = 0; i < chime.length / 4; i++) head = Math.max(head, Math.abs(chime[i]));
        for (int i = chime.length * 3 / 4; i < chime.length; i++) {
            tail = Math.max(tail, Math.abs(chime[i]));
        }
        System.out.printf("    collect is %.0fms, crossings %.0f/s, tail %d%% of head%n",
                len * 1000f, crossRate(chime), tail * 100 / Math.max(1, head));
        check("the chime is short enough to repeat", len < 0.20f);
        check("and shorter than the gap between two landings",
                len < GameCore.HOME_TIME * (RoundEnd.arrival(1, 5) - RoundEnd.arrival(0, 5)) * 2f);
        check("the chime dies away", tail < head / 4);
        // Tone, not noise: it must ring like glass rather than tick like a click.
        check("it has a tone in it", crossRate(chime) > 1200f && crossRate(chime) < 6000f);
    }

    /**
     * The star pickup: the most repeated effect in the game, and one note of a ladder.
     *
     * Held to the same three properties as the chop and the shelving chime — short, decaying, and
     * tonal — plus the one that is specific to it: twenty of these go off inside a five-second
     * course, the last of them a fifth of a second apart, so it has to be shorter than that gap.
     */
    static void starPickup(Layout L) {
        group("star pickup sound");

        short[] ting = Sfx.build(Sfx.STAR);
        float len = (float) ting.length / Sfx.RATE;
        int head = 0, tail = 0;
        for (int i = 0; i < ting.length / 4; i++) head = Math.max(head, Math.abs(ting[i]));
        for (int i = ting.length * 3 / 4; i < ting.length; i++) {
            tail = Math.max(tail, Math.abs(ting[i]));
        }
        System.out.printf("    star is %.0fms, crossings %.0f/s, tail %d%% of head%n",
                len * 1000f, crossRate(ting), tail * 100 / Math.max(1, head));
        check("the ting is short enough to repeat", len < 0.13f);
        check("and shorter than the shelving chime", ting.length < Sfx.build(Sfx.COLLECT).length);
        // The gap between the last two stars of a course, which is the tightest it ever has to fit.
        float gap = StarPath.encounterTime(StarPath.COUNT - 1)
                - StarPath.encounterTime(StarPath.COUNT - 2);
        System.out.printf("    the last two stars are %.0fms apart%n", gap * 1000f);
        check("and shorter than the gap between the last two stars", len < gap);
        check("the ting dies away", tail < head / 4);
        check("it is a note, not a click", crossRate(ting) > 1800f && crossRate(ting) < 7000f);

        // One note per star, climbing, and the twentieth deliberately silent: the tableau's fanfare
        // is what that one sounds like, and a note under it would be lost anyway.
        GameCore c = new GameCore(new Mem(), 331L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.stars.make(new java.util.Random(331L));
        c.stars.begin(-1, L);
        int last = StarPath.COUNT - 1;
        c.stars.collected = (1 << last) - 1;
        c.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.5f;
        check("nineteen already taken makes no sound", ear.stars == 0);
        for (int i = 0; i < 60 * 6 && !c.stars.won; i++) {
            c.stars.x = c.stars.starX(last, L);
            c.update(DT, L);
        }
        check("the course completed", c.stars.won);
        check("the last star rings the fanfare instead of a note",
                ear.stars == 0 && ear.achievements == 1);

        // And a course flown from the start announces every star it takes, with the count.
        GameCore d = new GameCore(new Mem(), 332L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.startGame();
        d.state = GameCore.BONUS;
        d.starBonus = true;
        d.stars.make(new java.util.Random(332L));
        d.stars.begin(-1, L);
        for (int i = 0; i < 60 * 12 && d.state == GameCore.BONUS; i++) d.update(DT, L);
        System.out.printf("    a drifting course took %d and announced %d%n",
                d.stars.count(), ear2.stars);
        check("every star taken is announced once", ear2.stars == d.stars.count());
        check("and the note climbs with the count", ear2.lastStar == d.stars.count());
    }

    /**
     * The four effects added for the moments that used to pass in silence: a course leaving, a
     * count being read out at the end of one nobody won, the new collectible joining the parade,
     * and the end of a run.
     */
    static void interludeSounds(Layout L) {
        group("interlude and cut-scene sounds");

        short[] blast = Sfx.build(Sfx.BLAST_OFF);
        check("blast-off ends before the climb-out", blast.length < Sfx.RATE * StarPath.EXIT);
        int blastHead = 0, blastTail = 0;
        for (int i = 0; i < blast.length / 2; i++) blastHead = Math.max(blastHead, Math.abs(blast[i]));
        for (int i = blast.length * 3 / 4; i < blast.length; i++) blastTail = Math.max(blastTail, Math.abs(blast[i]));
        check("blast-off exhaust fades away", blastTail < blastHead / 4);
        short[] go = Sfx.build(Sfx.COURSE);
        int firstQuarter = 0, back = 0;
        for (int i = 0; i < go.length / 4; i++) firstQuarter = Math.max(firstQuarter,
                Math.abs(go[i]));
        for (int i = go.length / 2; i < go.length; i++) back = Math.max(back, Math.abs(go[i]));
        System.out.printf("    the course whoosh is %.0fms, opening quarter %d%% of the back half%n",
                1000f * go.length / Sfx.RATE, firstQuarter * 100 / Math.max(1, back));
        // The one effect here that grows. Everything else in this file decays, and a whoosh that
        // decays reads as something stopping rather than as something leaving.
        check("the course whoosh swells rather than decaying", firstQuarter < back / 2);
        // And it is over before the first checkpoints arrive, or it plays under their notes.
        check("and is done before the course is up to pace",
                (float) go.length / Sfx.RATE <= StarPath.EASE_IN * 0.75f);

        short[] tally = Sfx.build(Sfx.TALLY);
        short[] fanfare = Sfx.build(Sfx.ACHIEVEMENT);
        System.out.printf("    the tally is %.0fms against the fanfare's %.0fms, crossings %.0f/s%n",
                1000f * tally.length / Sfx.RATE, 1000f * fanfare.length / Sfx.RATE,
                crossRate(tally));
        // Plainly the smaller of the two: this is a number arriving, not a prize.
        check("the tally is smaller than the fanfare it is not", tally.length < fanfare.length / 2);
        check("and it is a note, not a click",
                crossRate(tally) > 900f && crossRate(tally) < 4000f);
        // It has to fit inside the report it announces, both games' worth.
        check("and fits inside the report window",
                (float) tally.length / Sfx.RATE < Math.min(StarPath.REPORT, GameCore.BONUS_STATUS));

        short[] join = Sfx.build(Sfx.JOIN);
        // Long enough to be a chord and short enough to be inside the beat it marks: the join runs
        // from Parade.IN_END to JOIN_END of a PARADE_TIME parade.
        float joinBeat = GameCore.PARADE_TIME * (Parade.JOIN_END - Parade.IN_END);
        System.out.printf("    the join chord is %.0fms inside a %.0fms beat%n",
                1000f * join.length / Sfx.RATE, joinBeat * 1000f);
        check("the join chord fits the beat it lands on",
                (float) join.length / Sfx.RATE < joinBeat);

        short[] over = Sfx.build(Sfx.OVER);
        // Descending, which nothing else here is: the first note's pitch has to be above the last.
        float head = crossRate(java.util.Arrays.copyOfRange(over, 0, over.length / 4));
        float tail = crossRate(java.util.Arrays.copyOfRange(over,
                over.length / 2, over.length * 3 / 4));
        System.out.printf("    the game-over sting is %.0fms, %.0f/s falling to %.0f/s%n",
                1000f * over.length / Sfx.RATE, head, tail);
        check("the game-over sting falls", tail < head * 0.85f);
        boolean descending = true;
        int chromaticSteps = 0;
        for (int i = 1; i < Sfx.OVER_NOTES.length; i++) {
            float ratio = Sfx.OVER_NOTES[i - 1] / Sfx.OVER_NOTES[i];
            if (ratio <= 1f) descending = false;
            if (ratio < 1.07f) chromaticSteps++;
        }
        check("its melody descends on every note", descending);
        check("most of that melody falls by chromatic-sized steps", chromaticSteps >= 6);
        check("and it is the longest thing here, but not longer than the swirl and the summary",
                over.length > join.length
                        && (float) over.length / Sfx.RATE < GameCore.DEATH_TIME + 1f);

        // Every one of them fires exactly once, where it should. A star course flown by nobody:
        // it launches, it reports, and it never rings the fanfare.
        GameCore c = new GameCore(new Mem(), 515L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        c.state = GameCore.BONUS;
        c.starBonus = true;
        c.stars.make(new java.util.Random(515L));
        c.stars.begin(-1, L);
        c.bonusTimer = c.stars.timer;
        int launchesInLesson = 0;
        boolean silentFinish = true;
        for (int i = 0; i < 60 * 12 && c.state == GameCore.BONUS; i++) {
            if (c.stars.ready()) launchesInLesson = ear.courseStarts;
            c.update(DT, L);
            if (c.stars.exiting() || c.stars.reporting())
                silentFinish &= ear.rocketThrust == 0f && ear.courseFinishes == 1;
        }
        check("climb-out stops the loop and blasts off exactly once", silentFinish && ear.courseFinishes == 1);
        System.out.printf("    a lost course: %d launches, %d tallies at %d stars, %d fanfares%n",
                ear.courseStarts, ear.tallies, ear.lastTally, ear.achievements);
        check("a course announces its launch once", ear.courseStarts == 1);
        check("and not before the lesson is over", launchesInLesson == 0);
        check("a lost course reads its count out once", ear.tallies == 1);
        check("with the count it actually took", ear.lastTally == c.stars.count());
        check("and it does not ring the prize fanfare", ear.achievements == 0);
        check("the light rocket runs through the flight",
                ear.rocketCalls > 0 && ear.firstRocket > 0f);
        check("rocket thrust rises with course progress", ear.maxRocket > ear.firstRocket * 2f);
        check("the rocket stops outside the flight", ear.rocketStops > 0 && ear.rocketThrust == 0f);

        // And a won one: the fanfare, no tally, and one join chord in the parade after it.
        GameCore w = new GameCore(new Mem(), 517L);
        Ear won = new Ear();
        w.sound = won;
        w.startGame();
        w.state = GameCore.BONUS;
        w.starBonus = true;
        w.starNext = true;
        w.stars.begin(-1, L);
        w.stars.collected = (1 << (StarPath.COUNT - 1)) - 1;
        w.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 0.5f;
        int last = StarPath.COUNT - 1;
        boolean silentWin = true;
        for (int i = 0; i < 60 * 25 && w.state == GameCore.BONUS; i++) {
            if (!w.stars.won) w.stars.x = w.stars.starX(last, L);
            w.update(DT, L);
            if (w.stars.winning()) silentWin &= won.rocketThrust == 0f && won.courseFinishes == 1;
        }
        check("victory stops the loop and blasts off exactly once", silentWin && won.courseFinishes == 1);
        System.out.printf("    a won course: %d fanfares, %d tallies, %d join chords%n",
                won.achievements, won.tallies, won.joins);
        check("a won course rings the fanfare and no tally",
                won.achievements >= 1 && won.tallies == 0);
        check("and the parade's new arrival is announced once", won.joins == 1);

        // The run's full stop, at the end of the swirl rather than on the fatal breach.
        GameCore d = new GameCore(new Mem(), 519L);
        Ear died = new Ear();
        d.sound = died;
        d.startGame();
        d.lives = 1;
        d.state = GameCore.PLAY;
        int guard = 0;
        for (; guard < 60 * 200 && d.state != GameCore.OVER; guard++) d.update(DT, L);
        boolean quietAtDeath = died.gameOvers == 0;
        for (int i = 0; i < 60 * 4; i++) d.update(DT, L);
        System.out.printf("    the run ended: %d stings, held for %.1fs first%n",
                died.gameOvers, GameCore.DEATH_TIME);
        check("a run gets one full stop", died.gameOvers == 1);
        check("and it waits for the swirl rather than landing on the drip", quietAtDeath);
    }

    private static int peakAt(short[] pcm) {
        int at = 0, peak = 0;
        for (int i = 0; i < pcm.length; i++) {
            int v = Math.abs(pcm[i]);
            if (v > peak) { peak = v; at = i; }
        }
        return at;
    }

    /** Positive-going zero crossings in the shared opening window: a robust pitch ordering test. */
    private static int crossings(short[] pcm) {
        int n = Math.min(pcm.length, Sfx.RATE / 8), count = 0;
        for (int i = 1; i < n; i++) if (pcm[i - 1] <= 0 && pcm[i] > 0) count++;
        return count;
    }

    static boolean silentRunSurvives(Layout L) {
        GameCore c = new GameCore(new Mem(), 74L);
        c.sound = null;
        c.startGame();
        for (int i = 0; i < 600; i++) {
            c.update(DT, L);
            c.tapKey(i % Glyph.COUNT, L);
        }
        return true;
    }

}
