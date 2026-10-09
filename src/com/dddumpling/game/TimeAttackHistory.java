package com.dddumpling.game;

import java.util.ArrayList;

/** Fastest completed replays plus the latest attempt, scoped to one boss/control profile. */
final class TimeAttackHistory {
    static final int LIMIT=10;
    final ArrayList<Run> runs=new ArrayList<>();
    Run latest;
    long sequence;
    boolean unread;

    static final class Run {
        final long id,duration,firstDamage;
        final int character,cleared,hits,misses,defended,damage;
        final boolean won;
        Run(long id,long duration,int character,int cleared,boolean won,int hits,int misses,
                int defended,int damage,long firstDamage) {
            this.id=id;this.duration=duration;this.character=character;this.cleared=cleared;this.won=won;
            this.hits=hits;this.misses=misses;this.defended=defended;this.damage=damage;this.firstDamage=firstDamage;
        }
        boolean legacy() {return id==0;}
        String accuracy() {return legacy()?"--":((long)hits+misses==0?"--":Math.round(100.0*hits/((long)hits+misses))+"%");}
        String firstDamageText() {return legacy()?"--":damage==0?"NO DAMAGE":TimeAttack.time(Math.max(1,firstDamage));}
    }
    void clear() {runs.clear();latest=null;sequence=0;unread=false;}
    void legacy(long duration,int cleared) {
        if(duration>0)runs.add(new Run(0,duration,-1,cleared,true,0,0,0,0,-1));
    }
    void add(Run run) {
        if(run.id>0) {latest=run;sequence=run.id;unread=true;}
        if(!run.won)return;
        int at=0;
        while(at<runs.size() && (runs.get(at).duration<run.duration
                || runs.get(at).duration==run.duration && runs.get(at).id>run.id))at++;
        runs.add(at,run);if(runs.size()>LIMIT)runs.remove(LIMIT);
    }
    private boolean extraLatest() {
        if(latest==null)return false;
        for(Run run:runs)if(run.id==latest.id)return false;
        return true;
    }
    int displayCount() {return runs.size()+(extraLatest()?1:0);}
    Run displayRun(int row) {return row<runs.size()?runs.get(row):latest;}
    String encode() {
        StringBuilder s=new StringBuilder(Long.toString(sequence));
        for(int i=0;i<displayCount();i++) {
            Run r=displayRun(i);
            s.append(';').append(r.id).append(',').append(r.duration).append(',').append(r.character)
                    .append(',').append(r.cleared).append(',').append(r.won?1:0).append(',').append(r.hits)
                    .append(',').append(r.misses).append(',').append(r.defended).append(',').append(r.damage)
                    .append(',').append(r.firstDamage);
        }
        return s.toString();
    }
    boolean load(String data,int challenge) {
        try {
            String[] rows=data.split(";",-1);
            if(rows.length>LIMIT+2)return false;
            long seq=Long.parseLong(rows[0]);if(seq<0)return false;
            ArrayList<Long> ids=new ArrayList<>();
            for(int i=1;i<rows.length;i++) {
                String[] f=rows[i].split(",",-1);if(f.length!=10)return false;
                long id=Long.parseLong(f[0]),duration=Long.parseLong(f[1]),first=Long.parseLong(f[9]);
                int character=Integer.parseInt(f[2]),cleared=Integer.parseInt(f[3]),won=Integer.parseInt(f[4]);
                int hits=Integer.parseInt(f[5]),misses=Integer.parseInt(f[6]),defended=Integer.parseInt(f[7]),damage=Integer.parseInt(f[8]);
                int goal=challenge==TimeAttack.ALL?Boss.COUNT:1;
                if(id<0 || id>seq || ids.contains(id) || duration<1 || duration>86400000L || character< -1
                        || character>=Collect.COUNT || cleared<0 || cleared>goal || won<0 || won>1
                        || hits<0 || misses<0 || defended<0 || damage<0 || first< -1 || first>duration
                        || won==1 && cleared!=goal || won==0 && (cleared==goal || id!=seq)
                        || id==0 && (character!=-1 || won!=1 || hits!=0 || misses!=0 || defended!=0 || damage!=0 || first!=-1)
                        || id>0 && (character<0 || (damage==0)!=(first==-1)))return false;
                ids.add(id);
                Run run=new Run(id,duration,character,cleared,won==1,hits,misses,defended,damage,first);
                if(run.won)runs.add(run);
                if(id==seq && id>0)latest=run;
            }
            if(runs.size()>LIMIT || seq>0 && latest==null)return false;
            runs.sort((a,b)->a.duration==b.duration?Long.compare(b.id,a.id):Long.compare(a.duration,b.duration));
            sequence=seq;unread=false;return true;
        } catch(NumberFormatException invalid) {return false;}
    }
}
