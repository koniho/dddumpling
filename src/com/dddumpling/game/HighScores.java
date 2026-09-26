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
    final int[] effects=new int[Power.NAMES.length];
    final ArrayList<Integer> prizes=new ArrayList<>();
    private boolean recording;
    private int character=-1;
    boolean unread;

    static final class Run {
        final long id;
        final int score, stage, stages, dumplings, powers, swipes, bosses;
        final int hits, misses, squishes, combo, best, land;
        final boolean kids;
        final int ending, character;
        final int[] effects=new int[Power.NAMES.length];
        final int[] prizes;
        Run(long id,int[] v,int[] prizes) {
            this.id=id;score=v[0];stage=v[1];stages=v[2];dumplings=v[3];powers=v[4];
            swipes=v[5];bosses=v[6];hits=v[7];misses=v[8];squishes=v[9];combo=v[10];
            best=v[11];land=v[12];kids=v[13]!=0;ending=v[14];character=v.length>15?v[15]:-1;
            for(int i=0;i<effects.length && 16+i<v.length;i++) effects[i]=v[16+i];
            this.prizes=prizes;
        }
        String blurb() { return BLURBS[ending]; }
        int accuracy() { long n=(long)hits+misses;return n==0?0:(int)Math.round(100.0*hits/n); }
        int[] values() {
            int[] v={score,stage,stages,dumplings,powers,swipes,bosses,hits,misses,squishes,
                    combo,best,land,kids?1:0,ending,character,0,0,0,0,0,0};
            System.arraycopy(effects,0,v,16,effects.length);
            return v;
        }
    }
    void clear() {
        runs.clear();latest=0;latestRun=null;unread=false;recording=false;
        stages=dumplings=powers=swipes=bosses=0;character=-1;
        java.util.Arrays.fill(effects,0);
        prizes.clear();
    }
    void start(int selection) {
        character=selection;stages=dumplings=powers=swipes=bosses=0;
        java.util.Arrays.fill(effects,0);prizes.clear();recording=true;unread=false;
    }
    void effect(int effect) {
        if(effect>=0 && effect<effects.length) effects[effect]++;
    }
    void prize(int entry) {
        if(entry<0 || entry>=Collect.COUNT) return;
        dumplings++;prizes.add(entry);
    }
    void finish(GameCore c) {
        if(!recording) return;
        recording=false;
        if(latest==Long.MAX_VALUE) return;
        int boss=Boss.kindFor(c.stage);
        int ending=c.lives>0?0:boss>=0?1+boss:5+Lands.forStage(c.stage);
        int[] values={Math.max(0,c.score),c.stage,stages,dumplings,powers,swipes,bosses,c.hits,
                c.misses,c.squishes,c.maxCombo,Math.max(c.best,c.score),c.runStartLand,
                c.kidsRun?1:0,ending,character,0,0,0,0,0,0};
        System.arraycopy(effects,0,values,16,effects.length);
        int[] won=new int[prizes.size()];
        for(int i=0;i<won.length;i++) won[i]=prizes.get(i);
        Run run=new Run(++latest,values,won);
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
        StringBuilder s=new StringBuilder("5:").append(latest);
        for(int i=0;i<displayCount();i++) {
            Run run=displayRun(i);
            s.append(';').append(run.id);
            for(int value:run.values()) s.append(',').append(value);
            s.append(',').append(run.prizes.length);
            for(int prize:run.prizes) s.append(',').append(prize);
        }
        return s.toString();
    }
    void load(String data) {
        runs.clear();latest=0;latestRun=null;
        if(data==null || data.isEmpty() || data.length()>16000) return;
        try {
            String[] rows=data.split(";",-1);
            boolean prizeHistory=rows[0].startsWith("5:");
            boolean effectHistory=prizeHistory || rows[0].startsWith("4:");
            boolean portraits=effectHistory || rows[0].startsWith("3:");
            boolean current=portraits || rows[0].startsWith("2:");
            if((!current && !rows[0].startsWith("1:")) || rows.length>LIMIT+(current?2:1)) return;
            long sequence=Long.parseLong(rows[0].substring(2));
            if(sequence<0) return;
            ArrayList<Run> parsed=new ArrayList<>();
            for(int row=1;row<rows.length;row++) {
                String[] fields=rows[row].split(",",-1);
                int valueCount=effectHistory?22:portraits?16:15;
                if(!prizeHistory && fields.length!=valueCount+1) return;
                if(prizeHistory && fields.length<valueCount+2) return;
                long id=Long.parseLong(fields[0]);
                if(id<=0 || id>sequence) return;
                for(Run run:parsed) if(run.id==id) return;
                int[] v=new int[valueCount];
                for(int i=0;i<v.length;i++) { v[i]=Integer.parseInt(fields[i+1]);if(v[i]<(i==15?-1:0)) return; }
                int[] prizes=new int[0];
                if(prizeHistory) {
                    int count=Integer.parseInt(fields[valueCount+1]);
                    if(count<0 || count>1024 || fields.length!=valueCount+2+count || count!=v[3]) return;
                    prizes=new int[count];
                    for(int i=0;i<count;i++) {
                        prizes[i]=Integer.parseInt(fields[valueCount+2+i]);
                        if(prizes[i]<0 || prizes[i]>=Collect.COUNT) return;
                    }
                }
                if(portraits && v[15]>=Collect.COUNT) return;
                if(v[1]<1 || v[6]>15 || v[11]<v[0] || v[12]>=Lands.COUNT || v[13]>1 || v[14]>=BLURBS.length) return;
                parsed.add(new Run(id,v,prizes));
            }
            latest=sequence;
            for(Run run:parsed) {
                if(run.id==latest) latestRun=run;
                insert(run);
            }
        } catch(NumberFormatException ignored) { runs.clear();latest=0;latestRun=null; }
    }
}
