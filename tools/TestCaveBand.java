package com.dddumpling.game;

final class TestCaveBand extends Check {
    static final class Transport extends Ear {
        float position=Float.NaN;
        int starts,notes,stops;
        boolean paused,muted;
        public void bandStart(int song,boolean off){starts++;muted=off;position=Float.NaN;}
        public float bandTime(){return position;}
        public void bandNote(int song,int note){notes++;}
        public void bandPause(boolean value){paused=value;}
        public void bandStop(){stops++;}
        public void bandMuted(boolean value){muted=value;}
    }
    static GameCore game(Layout L) {
        GameCore c=TestCave.game(L);c.starNext=true;
        Interlude.enterBonus(c,L);c.cart.stop();c.band.begin(c);return c;
    }
    static void all(Layout L) {
        group("cave band");
        for(int song=0;song<CaveSong.COUNT;song++) {
            int total=0;
            for(int b=0;b<CaveSong.BEATS;b++) {
                float t=CaveSong.at(song,b),gap=CaveSong.at(song,b+1)-t;
                check("beat timestamp round trips "+song+"/"+b,Math.abs(CaveSong.beat(song,t)-b)<.001);
                check("chart leaves separate timing windows "+song+"/"+b,gap>CaveBand.WINDOW*2);
                if(CaveSong.cue(song,b))total++;
            }
            check("tempo increases twice "+song,CaveSong.bpm(song,0)<CaveSong.bpm(song,12)
                    && CaveSong.bpm(song,12)<CaveSong.bpm(song,24));
            short[] pcm=CaveSong.backing(song);
            int peak=0;double energy=0;
            for(short sample:pcm){peak=Math.max(peak,Math.abs((int)sample));energy+=(double)sample*sample;}
            check("finite song matches chart duration "+song,Math.abs(pcm.length/(float)Sfx.RATE-CaveSong.duration(song))<.001);
            check("song has audible headroom "+song,peak>4000 && peak<30000 && Math.sqrt(energy/pcm.length)>800);
            GameCore c=game(L);
            c.stage=21+song;c.band.begin(c);
            for(int b=0;b<CaveSong.BEATS;b++)if(c.band.result[b]==0) {
                c.band.position=CaveSong.at(song,b);
                c.tapBonus(c.band.glyph[b]);c.tapBonus(c.band.glyph[b]);
            }
            check("every authored note scores once "+song,c.band.hits==total && c.band.charge==total);
            check("band does not change typing accuracy "+song,c.hits==0 && c.misses==0);
            c.band.position=CaveSong.duration(song);c.update(.01f,L);
            check("first song carries crystal progress "+song,c.band.finished && !c.band.won && c.band.charge==total);
            c.update(CaveBand.REPORT+.1f,L);
            check("report resumes expedition "+song,c.state==GameCore.PLAY && !c.band.active);
        }
        for(float error:new float[]{-.139f,.139f,-.15f,.15f}) {
            GameCore boundary=game(L);
            boundary.band.position=CaveSong.at(0,0)+error;
            boundary.tapBonus(boundary.band.glyph[0]);
            check("timing window is symmetric "+error,(boundary.band.hits==1)==(Math.abs(error)<CaveBand.WINDOW));
        }
        GameCore c=game(L);
        check("cave bypasses both ordinary games",c.band.active && !c.starBonus && c.starNext
                && !c.bonusMashing() && !c.bonusRolling() && !c.bonusHolding() && !c.bonusStatus());
        int stage=c.stage,lives=c.lives;
        c.update(100,L);c.update(CaveBand.REPORT+.1f,L);
        check("idle performance advances with no free reward or damage",c.stage==stage+1 && c.lives==lives && c.collectTotal==0);
        c.jumpToStage(21,L);Interlude.enterBonus(c,L);c.cart.stop();c.band.begin(c);
        c.band.position=CaveSong.at(c.band.song,0);
        c.tapBonus((c.band.glyph[0]+1)%6);c.tapBonus(c.band.glyph[0]);
        check("wrong key consumes cue to prevent all-key mashing",c.band.hits==0 && c.band.result[0]==-2);
        c=game(L);c.band.charge=CaveBand.GOAL-1;c.band.position=CaveSong.at(c.band.song,0);
        c.tapBonus(c.band.glyph[0]);c.band.position=CaveSong.duration(c.band.song);c.update(.01f,L);
        int count=c.collectTotal;c.update(.1f,L);
        check("full crystals award once",c.band.won && count==1 && c.collectTotal==1 && c.paradeTimer>0);
        check("award consumes exactly one goal",c.band.charge==0);
        check("band completion awards a snake",Collect.FAMILY[c.prize]==Collect.SNAKES);
        c.update(CaveBand.REPORT+GameCore.PARADE_TIME+1,L);
        check("winning parade eventually resumes cave",c.state==GameCore.PLAY && !c.band.active);
        c=TestCave.game(L);Transport ear=new Transport();c.sound=ear;Interlude.enterBonus(c,L);c.cart.stop();c.band.begin(c);
        c.update(5,L);check("audio preparation freezes chart",c.band.position==0);
        ear.position=CaveSong.at(c.band.song,0);c.kidsRun=true;c.speed=2f;c.update(.01f,L);
        check("audio clock ignores game speed and kids slow motion",c.band.position==ear.position);
        c.tapBonus(c.band.glyph[0]);check("live input samples transport and plays guitar",ear.notes==1);
        ear.position=CaveSong.at(c.band.song,4);c.update(.01f,L);
        check("frame stall expires skipped notes",c.band.misses>0 && c.band.position==ear.position);
        Pause.open(c);float pos=c.band.position;c.update(5,L);c.tapBonus(0);
        check("pause freezes song and input",ear.paused && c.band.position==pos && ear.notes==1);
        Pause.resume(c);check("resume releases transport",!ear.paused);
        c.openSettings();check("settings pause transport",ear.paused);c.closeSettings();
        c.preferences.musicMuted=true;c.preferences.save(c);
        check("mute silences band without resetting song",ear.musicVolume==0f && ear.starts==1);
        c.preferences.music=.4f;c.preferences.musicMuted=false;c.preferences.save(c);
        check("unmute restores band volume without restarting",ear.musicVolume==.4f && ear.starts==1);
        ear.position=CaveSong.at(c.band.song,5)+.25f;c.update(.01f,L);
        c.tapBonus(c.band.glyph[5],.25f);
        check("queued touch uses original event time",c.band.result[5]==1 && ear.notes==2);
        c.toTitle();check("leaving cancels transport",ear.stops==1 && !c.band.active);
        c.startGame();check("new run resets crystal progress",c.band.charge==0);
        c.jumpToStage(19,L);Interlude.enterBonus(c,L);
        check("other lands keep their interludes",!c.band.active);
    }
}
