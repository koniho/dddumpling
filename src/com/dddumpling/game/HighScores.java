package com.dddumpling.game;

import java.util.ArrayList;

/** Local run snapshots. The sequence also identifies a latest run outside the top ten. */
final class HighScores {
    static final int LIMIT=10;
    // Persist the context index so a build's scenery cycle cannot rewrite a past death.
    static final String[] BLURBS={
        "Called it a snack break.",
        "Slime gave you a goo hug.",
        "Dark Divide cubed your cuteness.",
        "Octopulse: eight hugs too many.",
        "Fly Agaric made you a spore snack.",
        "Slime hills hugged back.",
        "Crystal sparkle nap. Very shiny.",
        "Kelp tucked you in. Bubble blanket!",
        "Mushroom cap became your pillow.",
        "Pebble adopted its own dumpling."
    };
    final ArrayList<Run> runs=new ArrayList<>();
    long latest;
    Run latestRun;
    int stages, dumplings, powers, swipes, bosses;
    private boolean recording;
    boolean unread;

    static final class Run {
        final long id;
        final int score, stage, stages, dumplings, powers, swipes, bosses;
        final int hits, misses, squishes, combo, best, land;
        final boolean kids;
        final int ending;
        Run(long id,int[] v) {
            this.id=id;score=v[0];stage=v[1];stages=v[2];dumplings=v[3];powers=v[4];
            swipes=v[5];bosses=v[6];hits=v[7];misses=v[8];squishes=v[9];combo=v[10];
            best=v[11];land=v[12];kids=v[13]!=0;ending=v[14];
        }
        String blurb() { return BLURBS[ending]; }
        int accuracy() { long n=(long)hits+misses;return n==0?0:(int)Math.round(100.0*hits/n); }
        int[] values() { return new int[]{score,stage,stages,dumplings,powers,swipes,bosses,
                hits,misses,squishes,combo,best,land,kids?1:0,ending}; }
    }
    void start() { stages=dumplings=powers=swipes=bosses=0;recording=true;unread=false; }
    void finish(GameCore c) {
        if(!recording) return;
        recording=false;
        if(latest==Long.MAX_VALUE) return;
        int boss=Boss.kindFor(c.stage);
        int ending=c.lives>0?0:boss>=0?1+boss:5+Lands.forStage(c.stage);
        Run run=new Run(++latest,new int[]{Math.max(0,c.score),c.stage,stages,dumplings,powers,
                swipes,bosses,c.hits,c.misses,c.squishes,c.maxCombo,Math.max(c.best,c.score),c.runStartLand,c.kidsRun?1:0,ending});
        unread=true;
        latestRun=run;
        insert(run);
        if(c.store!=null) c.store.saveHighScores(encode());
    }
    private void insert(Run run) {
        int at=0;
        while(at<runs.size() && (runs.get(at).score>run.score
                || (runs.get(at).score==run.score && runs.get(at).id>run.id))) at++;
        runs.add(at,run);
        if(runs.size()>LIMIT) runs.remove(LIMIT);
    }
    boolean latestOutsideTopTen() {
        if(latestRun==null) return false;
        for(Run run:runs) if(run.id==latestRun.id) return false;
        return true;
    }
    int displayCount() { return runs.size()+(latestOutsideTopTen()?1:0); }
    Run displayRun(int row) { return row<runs.size()?runs.get(row):latestRun; }
    String encode() {
        StringBuilder s=new StringBuilder("2:").append(latest);
        for(int i=0;i<displayCount();i++) {
            Run run=displayRun(i);
            s.append(';').append(run.id);
            for(int value:run.values()) s.append(',').append(value);
        }
        return s.toString();
    }
    void load(String data) {
        runs.clear();latest=0;latestRun=null;
        if(data==null || data.isEmpty() || data.length()>16000) return;
        try {
            String[] rows=data.split(";",-1);
            boolean current=rows[0].startsWith("2:");
            if((!current && !rows[0].startsWith("1:")) || rows.length>LIMIT+(current?2:1)) return;
            long sequence=Long.parseLong(rows[0].substring(2));
            if(sequence<0) return;
            ArrayList<Run> parsed=new ArrayList<>();
            for(int row=1;row<rows.length;row++) {
                String[] fields=rows[row].split(",",-1);
                if(fields.length!=16) return;
                long id=Long.parseLong(fields[0]);
                if(id<=0 || id>sequence) return;
                for(Run run:parsed) if(run.id==id) return;
                int[] v=new int[15];
                for(int i=0;i<v.length;i++) { v[i]=Integer.parseInt(fields[i+1]);if(v[i]<0) return; }
                if(v[1]<1 || v[6]>15 || v[11]<v[0] || v[12]>=Lands.COUNT || v[13]>1 || v[14]>=BLURBS.length) return;
                parsed.add(new Run(id,v));
            }
            latest=sequence;
            for(Run run:parsed) {
                if(run.id==latest) latestRun=run;
                insert(run);
            }
        } catch(NumberFormatException ignored) { runs.clear();latest=0;latestRun=null; }
    }
}
