package com.dddumpling.game;

/** Fresh completed runs only; pending bests stay attached to their original service account. */
final class LeaderboardSubmissions {
    private static final int MAX_ACCOUNTS=8;
    private final GameCore.Store store;
    private final boolean realRuns;
    private final java.util.LinkedHashMap<String,long[][]> accounts=new java.util.LinkedHashMap<>();
    private String owner="", authenticated="",runOwner="";
    private int runBoard=-1,rankCursor;
    private final long[] confirmedRanks=new long[LeaderboardCatalog.COUNT];
    private final long[] confirmedValues=new long[LeaderboardCatalog.COUNT];
    private boolean healthy=true,enabled;

    LeaderboardSubmissions(GameCore.Store store,boolean realRuns) {
        this.store=store;this.realRuns=realRuns;
        if(store!=null)try {load(store.loadLeaderboards());}catch(RuntimeException e){healthy=false;}
    }
    void account(String player,boolean allowed) {
        enabled=allowed;
        String nextOwner=allowed && validOwner(player)?player:"";
        if(!nextOwner.equals(authenticated)) {
            java.util.Arrays.fill(confirmedRanks,0);java.util.Arrays.fill(confirmedValues,0);
        }
        authenticated=nextOwner;
        if(!authenticated.isEmpty() && !authenticated.equals(owner)) {
            owner=authenticated;
            if(!accounts.containsKey(owner) && accounts.size()<MAX_ACCOUNTS)
                accounts.put(owner,new long[3][LeaderboardCatalog.COUNT]);
            save();
        }
    }
    void cancel() {runOwner="";runBoard=-1;}
    void begin(GameCore c) {
        cancel();
        if(!enabled || !healthy || !realRuns || owner.isEmpty() || !accounts.containsKey(owner))return;
        runOwner=owner;
        if(c.modes.selected==ModeSelector.SURVIVAL)
            runBoard=LeaderboardCatalog.survival(Survival.profile(c.kidsRun,c.runFullRoster));
        else if(c.modes.selected==ModeSelector.TIME_ATTACK)
            runBoard=LeaderboardCatalog.timeAttack(Survival.profile(c.kidsRun,c.runFullRoster),c.timeAttack.selected);
        else if(c.modes.adventure() && !c.kidsRun && c.runStartLand==0)runBoard=LeaderboardCatalog.ADVENTURE;
    }
    void finish(GameCore c,int board,long value) {
        String captured=runOwner;int expected=runBoard;
        runOwner="";runBoard=-1;
        if(!enabled || !healthy || !LeaderboardCatalog.valid(board) || expected!=board || c.scoresSuppressed
                || board==LeaderboardCatalog.ADVENTURE && c.highScores.bossRetries>0)return;
        long[][] scores=accounts.get(captured);
        if(scores!=null && LeaderboardCatalog.better(board,value,scores[0][board])) {
            scores[0][board]=value;save();
        }
    }
    int next() {return next(false);}
    int nextApple() {return next(true);}
    private int next(boolean apple) {
        if(!enabled || !healthy || authenticated.isEmpty())return -1;
        long[][] scores=accounts.get(authenticated);
        if(scores==null)return -1;
        for(int board=0;board<LeaderboardCatalog.COUNT;board++)
            if((!apple || LeaderboardCatalog.appleScore(board,scores[0][board])>0)
                    && LeaderboardCatalog.better(board,scores[0][board],scores[1][board]))return board;
        return -1;
    }
    long value(int board) {
        long[][] scores=accounts.get(authenticated);
        return scores!=null && LeaderboardCatalog.valid(board)?scores[0][board]:0;
    }
    String owner() {return authenticated;}
    void acknowledge(String player,int board,long value) {
        if(!enabled || !healthy || !authenticated.equals(player) || !LeaderboardCatalog.valid(board))return;
        long[][] scores=accounts.get(player);
        if(scores==null || value<=0 || LeaderboardCatalog.better(board,value,scores[0][board]))return;
        if(LeaderboardCatalog.better(board,value,scores[1][board])) {scores[1][board]=value;save();}
    }
    // Rank reads are independent of submission: stale/error responses leave this durable work pending.
    int nextRank() {
        if(!enabled || !healthy || authenticated.isEmpty())return -1;
        long[][] scores=accounts.get(authenticated);if(scores==null)return -1;
        for(int i=0;i<LeaderboardCatalog.COUNT;i++) {
            int board=(rankCursor+i)%LeaderboardCatalog.COUNT;
            if(LeaderboardCatalog.better(board,scores[1][board],scores[2][board])
                    && confirmedValues[board]!=scores[1][board]) {
                rankCursor=(board+1)%LeaderboardCatalog.COUNT;return board;
            }
        }
        return -1;
    }
    long rankedValue(int board) {
        long[][] scores=accounts.get(authenticated);
        return scores!=null && LeaderboardCatalog.valid(board)?scores[1][board]:0;
    }
    void confirmRank(String player,int board,long value,long serverScore,long rank,boolean apple) {
        if(!enabled || !healthy || !authenticated.equals(player) || !LeaderboardCatalog.valid(board) || rank<=0)return;
        long[][] scores=accounts.get(player);
        if(scores==null || value!=scores[1][board] || !LeaderboardCatalog.better(board,value,scores[2][board]))return;
        long expected=apple?LeaderboardCatalog.appleScore(board,value):LeaderboardCatalog.googleScore(board,value);
        if(expected<=0 || serverScore<=0 || LeaderboardCatalog.better(board,expected,serverScore))return;
        confirmedRanks[board]=rank;confirmedValues[board]=value;
    }
    int nextNotice() {
        if(!enabled || !healthy || authenticated.isEmpty())return -1;
        long[][] scores=accounts.get(authenticated);if(scores==null)return -1;
        for(int board=0;board<LeaderboardCatalog.COUNT;board++)
            if(confirmedRanks[board]>0 && confirmedValues[board]==scores[1][board]
                    && LeaderboardCatalog.better(board,scores[1][board],scores[2][board]))return board;
        return -1;
    }
    long noticeRank(int board) {return LeaderboardCatalog.valid(board)?confirmedRanks[board]:0;}
    void noticeShown(String player,int board,long value) {
        if(!healthy || !LeaderboardCatalog.valid(board))return;
        long[][] scores=accounts.get(player);
        if(scores==null || scores[1][board]!=value || !LeaderboardCatalog.better(board,value,scores[2][board]))return;
        scores[2][board]=value;
        if(authenticated.equals(player))confirmedRanks[board]=confirmedValues[board]=0;
        save();
    }
    private void save() {
        if(store==null) {healthy=false;return;}
        try {if(!store.saveLeaderboards(encode()))healthy=false;}catch(RuntimeException e){healthy=false;}
    }
    private String encode() {
        StringBuilder text=new StringBuilder("2;").append(hex(owner));
        for(java.util.Map.Entry<String,long[][]> entry:accounts.entrySet()) {
            text.append(';').append(hex(entry.getKey()));
            for(long[] row:entry.getValue())for(long value:row)text.append(',').append(value);
        }
        return text.toString();
    }
    private void load(String text) {
        if(text==null || text.isEmpty())return;
        if(text.length()>32000) {healthy=false;return;}
        try {
            String[] rows=text.split(";",-1);
            if(rows.length<2 || rows.length>MAX_ACCOUNTS+2 || (!rows[0].equals("1") && !rows[0].equals("2")))throw new IllegalArgumentException();
            int scoreRows=rows[0].equals("1")?2:3;
            owner=unhex(rows[1]);
            for(int i=2;i<rows.length;i++) {
                String[] fields=rows[i].split(",",-1);
                if(fields.length!=1+scoreRows*LeaderboardCatalog.COUNT)throw new IllegalArgumentException();
                String player=unhex(fields[0]);
                if(!validOwner(player) || accounts.containsKey(player))throw new IllegalArgumentException();
                long[][] scores=new long[3][LeaderboardCatalog.COUNT];
                for(int j=1;j<fields.length;j++) {
                    long value=Long.parseLong(fields[j]);
                    if(value<0)throw new IllegalArgumentException();
                    scores[(j-1)/LeaderboardCatalog.COUNT][(j-1)%LeaderboardCatalog.COUNT]=value;
                }
                // Existing acknowledged scores predate notifications; do not announce old history.
                if(scoreRows==2)System.arraycopy(scores[1],0,scores[2],0,LeaderboardCatalog.COUNT);
                accounts.put(player,scores);
            }
            if(!owner.isEmpty() && !validOwner(owner))throw new IllegalArgumentException();
        }catch(RuntimeException e){accounts.clear();owner="";healthy=false;}
    }
    private static boolean validOwner(String value) {return value!=null && !value.isEmpty() && value.length()<=256;}
    private static String hex(String value) {
        StringBuilder text=new StringBuilder();
        for(int i=0;i<value.length();i++) {String n=Integer.toHexString(value.charAt(i));for(int j=n.length();j<4;j++)text.append('0');text.append(n);}
        return text.toString();
    }
    private static String unhex(String value) {
        if(value.length()%4!=0 || value.length()>1024)throw new IllegalArgumentException();
        StringBuilder text=new StringBuilder();
        for(int i=0;i<value.length();i+=4)text.append((char)Integer.parseInt(value.substring(i,i+4),16));
        return text.toString();
    }
}
