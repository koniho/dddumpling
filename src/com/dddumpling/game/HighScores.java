package com.dddumpling.game;

import java.util.ArrayList;
import java.util.Arrays;

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
    final ArrayList<Integer> bossOrder=new ArrayList<>(), prizes=new ArrayList<>();
    final int[] powerUses=new int[Power.COUNT], debuffUses=new int[2];
    private boolean recording;
    private int character=-1;
    boolean unread;

    static final class Run {
        final long id;
        final int score, stage, stages, dumplings, powers, swipes, bosses;
        final int hits, misses, squishes, combo, best, land;
        final boolean kids;
        final int ending, character;
        final int[] bossOrder, prizes, powerUses, debuffUses;
        Run(long id,int[] v) {
            this(id,v,new int[0],new int[0],new int[0],new int[0]);
        }
        Run(long id,int[] v,int[] bossOrder,int[] prizes,int[] powerUses,int[] debuffUses) {
            this.id=id;score=v[0];stage=v[1];stages=v[2];dumplings=v[3];powers=v[4];
            swipes=v[5];bosses=v[6];hits=v[7];misses=v[8];squishes=v[9];combo=v[10];
            best=v[11];land=v[12];kids=v[13]!=0;ending=v[14];character=v.length>15?v[15]:-1;
            this.bossOrder=bossOrder.clone();this.prizes=prizes.clone();
            this.powerUses=powerUses.clone();this.debuffUses=debuffUses.clone();
        }
        String blurb() { return BLURBS[ending]; }
        int accuracy() { long n=(long)hits+misses;return n==0?0:(int)Math.round(100.0*hits/n); }
        int[] values() { return new int[]{score,stage,stages,dumplings,powers,swipes,bosses,
                hits,misses,squishes,combo,best,land,kids?1:0,ending,character}; }
    }
    void clear() {
        runs.clear();latest=0;latestRun=null;unread=false;recording=false;
        stages=dumplings=powers=swipes=bosses=0;character=-1;
        bossOrder.clear();prizes.clear();Arrays.fill(powerUses,0);Arrays.fill(debuffUses,0);
    }
    void start(int selection) {
        character=selection;stages=dumplings=powers=swipes=bosses=0;recording=true;unread=false;
        bossOrder.clear();prizes.clear();Arrays.fill(powerUses,0);Arrays.fill(debuffUses,0);
    }
    void recordBoss(int kind) { bosses|=1<<kind;bossOrder.add(kind); }
    void recordPrize(int who) { dumplings++;prizes.add(who); }
    void recordPower(int effect) { powers++;powerUses[effect]++; }
    void recordDebuff(int effect) { debuffUses[effect-Power.COUNT]++; }
    void finish(GameCore c) {
        if(!recording) return;
        recording=false;
        if(latest==Long.MAX_VALUE) return;
        int boss=Boss.kindFor(c.stage);
        int ending=c.lives>0?0:boss>=0?1+boss:5+Lands.forStage(c.stage);
        Run run=new Run(++latest,new int[]{Math.max(0,c.score),c.stage,stages,dumplings,powers,
                swipes,bosses,c.hits,c.misses,c.squishes,c.maxCombo,Math.max(c.best,c.score),c.runStartLand,c.kidsRun?1:0,ending,character},
                array(bossOrder),array(prizes),powerUses,debuffUses);
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
    private static int[] array(ArrayList<Integer> values) {
        int[] out=new int[values.size()];for(int i=0;i<out.length;i++) out[i]=values.get(i);return out;
    }
    private static String list(int[] values) {
        if(values.length==0)return "-";
        StringBuilder s=new StringBuilder();for(int value:values) { if(s.length()>0)s.append('.');s.append(value); }
        return s.toString();
    }
    String encode() {
        StringBuilder s=new StringBuilder("4:").append(latest);
        for(int i=0;i<displayCount();i++) {
            Run run=displayRun(i);
            s.append(';').append(run.id);
            for(int value:run.values()) s.append(',').append(value);
            s.append(',').append(list(run.bossOrder)).append(',').append(list(run.prizes))
                    .append(',').append(list(run.powerUses)).append(',').append(list(run.debuffUses));
        }
        return s.toString();
    }
    void load(String data) {
        runs.clear();latest=0;latestRun=null;
        if(data==null || data.isEmpty() || data.length()>16000) return;
        try {
            String[] rows=data.split(";",-1);
            boolean details=rows[0].startsWith("4:");
            boolean portraits=details || rows[0].startsWith("3:");
            boolean current=portraits || rows[0].startsWith("2:");
            if((!current && !rows[0].startsWith("1:")) || rows.length>LIMIT+(current?2:1)) return;
            long sequence=Long.parseLong(rows[0].substring(2));
            if(sequence<0) return;
            ArrayList<Run> parsed=new ArrayList<>();
            for(int row=1;row<rows.length;row++) {
                String[] fields=rows[row].split(",",-1);
                if(fields.length!=(details?21:portraits?17:16)) return;
                long id=Long.parseLong(fields[0]);
                if(id<=0 || id>sequence) return;
                for(Run run:parsed) if(run.id==id) return;
                int[] v=new int[portraits?16:15];
                for(int i=0;i<v.length;i++) { v[i]=Integer.parseInt(fields[i+1]);if(v[i]<(i==15?-1:0)) return; }
                if(portraits && v[15]>=Collect.COUNT) return;
                if(v[1]<1 || v[6]>15 || v[11]<v[0] || v[12]>=Lands.COUNT || v[13]>1 || v[14]>=BLURBS.length) return;
                int[] bossOrder=details?list(fields[17],64,Boss.COUNT):new int[0];
                int[] prizes=details?list(fields[18],128,Collect.COUNT):new int[0];
                int[] powerUses=details?list(fields[19],Power.COUNT,Integer.MAX_VALUE):new int[0];
                int[] debuffUses=details?list(fields[20],2,Integer.MAX_VALUE):new int[0];
                if(details && (powerUses.length!=Power.COUNT || debuffUses.length!=2)) return;
                parsed.add(new Run(id,v,bossOrder,prizes,powerUses,debuffUses));
            }
            latest=sequence;
            for(Run run:parsed) {
                if(run.id==latest) latestRun=run;
                insert(run);
            }
        } catch(NumberFormatException ignored) { runs.clear();latest=0;latestRun=null; }
    }
    private static int[] list(String field,int limit,int exclusiveMax) {
        if(field.equals("-"))return new int[0];
        String[] parts=field.split("\\.",-1);if(parts.length>limit)throw new NumberFormatException();
        int[] out=new int[parts.length];
        for(int i=0;i<out.length;i++) { out[i]=Integer.parseInt(parts[i]);if(out[i]<0 || out[i]>=exclusiveMax)throw new NumberFormatException(); }
        return out;
    }
}
