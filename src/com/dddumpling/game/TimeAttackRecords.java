package com.dddumpling.game;

/** Version 7 carries its roster size; adding a boss archives the previous all-boss challenge. */
final class TimeAttackRecords {
    static final class Archive {
        final long[] best=new long[Survival.PROFILE_COUNT];
        final TimeAttackHistory[] history=new TimeAttackHistory[Survival.PROFILE_COUNT];
        Archive() {for(int p=0;p<history.length;p++)history[p]=new TimeAttackHistory();}
        void clear() {java.util.Arrays.fill(best,0);for(TimeAttackHistory h:history)h.clear();}
    }
    static Archive archive(java.util.Map<Integer,Archive> all,int bosses) {
        Archive a=all.get(bosses);if(a==null){a=new Archive();all.put(bosses,a);}return a;
    }
    static String encode(TimeAttack t) {
        StringBuilder out=new StringBuilder("7;").append(Boss.COUNT).append(';').append(Survival.PROFILE_COUNT);
        for(long[] row:t.best)for(long value:row)out.append(';').append(value);
        for(TimeAttackHistory[] row:t.histories)for(TimeAttackHistory h:row)out.append('|').append(h.encode());
        for(java.util.Map.Entry<Integer,Archive> e:t.archives.entrySet()) {
            Archive a=e.getValue();
            for(int p=0;p<a.best.length;p++)out.append('|').append(e.getKey()).append(',').append(p)
                    .append(',').append(a.best[p]).append(':').append(a.history[p].encode());
        }
        return out.toString();
    }
    static void load(TimeAttack t,String saved) {
        if(saved==null || saved.length()>500000)return;
        try {
            String[] parts=saved.split("\\|",-1),f=parts[0].split(";",-1);
            int version=Integer.parseInt(f[0]);if(version<1 || version>7)return;
            // Historical schemas describe their original shapes, never today's content counts.
            boolean gator=version==5 || version==4 && f.length==19;
            int bosses=version==7?Integer.parseInt(f[1]):version==6?6:gator?5:4;
            int profiles=version==7?Integer.parseInt(f[2]):3;
            int start=version==7?3:1,width=bosses+1;
            if(bosses<1 || bosses>Boss.COUNT || profiles<1 || profiles>Survival.PROFILE_COUNT
                    || f.length!=start+profiles*width)return;
            int records=profiles*width,base=1+records;
            int oldArchives=version==6?2:gator?1:0;
            if(version==1?parts.length!=1:version==7?parts.length<base:parts.length!=base+oldArchives*profiles)return;
            long[][] values=new long[Survival.PROFILE_COUNT][TimeAttack.CHOICES];
            TimeAttackHistory[][] histories=new TimeAttackHistory[Survival.PROFILE_COUNT][TimeAttack.CHOICES];
            java.util.TreeMap<Integer,Archive> archives=new java.util.TreeMap<>();
            for(int p=0;p<histories.length;p++)for(int i=0;i<TimeAttack.CHOICES;i++)histories[p][i]=new TimeAttackHistory();
            for(int p=0;p<profiles;p++)for(int i=0;i<width;i++) {
                long best=Long.parseLong(f[start+p*width+i]);
                boolean all=i==bosses,archived=all && bosses!=Boss.COUNT;
                int destination=all?TimeAttack.ALL:i,goal=all?bosses:1;
                TimeAttackHistory h=archived?archive(archives,bosses).history[p]:histories[p][destination];
                if(!validBest(best))return;
                if(version==1)h.legacy(best,goal);
                else if(!h.load(parts[1+p*width+i],destination,goal))return;
                if(!matches(h,best))return;
                if(archived)archive(archives,bosses).best[p]=best;else values[p][destination]=best;
            }
            if(version==7) {
                java.util.HashSet<String> seen=new java.util.HashSet<>();
                for(int i=base;i<parts.length;i++) {
                    String[] row=parts[i].split(":",2),key=row[0].split(",",-1);
                    if(row.length!=2 || key.length!=3)return;
                    int count=Integer.parseInt(key[0]),profile=Integer.parseInt(key[1]);
                    if(count<1 || count>=Boss.COUNT || count==bosses || profile<0 || profile>=profiles
                            || !seen.add(count+","+profile))return;
                    long value=Long.parseLong(key[2]);Archive a=archive(archives,count);
                    if(!validBest(value) || !a.history[profile].load(row[1],TimeAttack.ALL,count)
                            || !matches(a.history[profile],value))return;
                    a.best[profile]=value;
                }
            } else for(int era=0;era<oldArchives;era++)for(int p=0;p<profiles;p++) {
                String[] row=parts[base+era*profiles+p].split(":",2);if(row.length!=2)return;
                long value=Long.parseLong(row[0]);Archive a=archive(archives,4+era);
                if(!validBest(value) || !a.history[p].load(row[1],TimeAttack.ALL,4+era) || !matches(a.history[p],value))return;
                a.best[p]=value;
            }
            t.clearRecords();
            for(int p=0;p<Survival.PROFILE_COUNT;p++) {
                System.arraycopy(values[p],0,t.best[p],0,TimeAttack.CHOICES);
                System.arraycopy(histories[p],0,t.histories[p],0,TimeAttack.CHOICES);
            }
            for(java.util.Map.Entry<Integer,Archive> e:archives.entrySet()) {
                Archive to=archive(t.archives,e.getKey()),from=e.getValue();
                System.arraycopy(from.best,0,to.best,0,from.best.length);
                System.arraycopy(from.history,0,to.history,0,from.history.length);
            }
        } catch(NumberFormatException | IndexOutOfBoundsException invalid) { /* Atomic rejection. */ }
    }
    private static boolean validBest(long value) {return value>=0 && value<=86400000L;}
    private static boolean matches(TimeAttackHistory h,long best) {return h.runs.isEmpty() || h.runs.get(0).duration==best;}
    private TimeAttackRecords() {}
}
