package com.dddumpling.game;

/** Bounded developer-only Adventure measurements. No network, per-frame writes, or gameplay RNG. */
final class PlaythroughLog {
    static final int LIMIT=256*1024;
    static final float CHECKPOINT=15f;
    private final GameCore.Store store;
    boolean enabled,active,stageOpen,exportRequested,saveFailed,cleanup;
    private String history="",decision="not_assessed";
    private long run;
    private int stage,attempt,keys,tier,streak,hits,misses,clears,powerClears,sweepClears,peakEnemies,powerMask;
    private int rush,fog,dive,pairs,sideClears;
    private float seconds,runSeconds,checkpoint,depthSum,maxDepth,firstClear=-1,lastClear=-1;
    private float deepestEnemy,deepestTopEnemy,halfwaySeconds,topHalfwaySeconds;

    PlaythroughLog(GameCore.Store store) {
        this.store=store;
        if(!BuildFlags.DEVELOPER || store==null)return;
        try {
            String saved=store.loadPlaythroughLog();if(saved==null)return;
            history=bounded(saved);enabled=true;
            int at=history.lastIndexOf("\"run\":");
            if(at>=0) {int end=history.indexOf(',',at);run=Long.parseLong(history.substring(at+6,end));}
        } catch(RuntimeException invalid) {history="";}
    }
    static String bounded(String text) {
        if(text.length()<=LIMIT)return text;
        int cut=text.indexOf('\n',text.length()-LIMIT);
        return cut<0?"":text.substring(cut+1);
    }
    private void write(String line) {
        history=bounded(history+line+"\n");
        try {saveFailed=!store.savePlaythroughLog(history);}catch(RuntimeException unavailable){saveFailed=true;}
    }
    void start(GameCore c) {
        if(!enabled || !c.modes.adventure())return;
        run=Math.max(run+1,store.updateTimeMillis());active=true;runSeconds=seconds=checkpoint=0;attempt=0;
        write(base(c,"run_start")+",\"app\":"+quote(store.appVersion())+",\"build\":"+quote(BuildFlags.BUILD_ID)+"}");
        beginStage(c);
    }
    void beginStage(GameCore c) {
        if(!active)return;
        if(stageOpen)endStage(c,"interrupted");
        stage=c.stage;attempt++;stageOpen=true;seconds=checkpoint=0;
        tier=c.runDifficultyTier;keys=Roster.count(c.runFullRoster);streak=c.rosterStrongStages;
        hits=c.hits;misses=c.misses;clears=powerClears=sweepClears=peakEnemies=powerMask=rush=fog=dive=pairs=0;
        depthSum=maxDepth=deepestEnemy=deepestTopEnemy=halfwaySeconds=topHalfwaySeconds=0;sideClears=0;firstClear=lastClear=-1;decision="not_assessed";
        write(base(c,"stage_start")+settings(c)+"}");
    }
    void tick(GameCore c,float elapsed,Layout L) {
        if(!active || !stageOpen || c.state!=GameCore.PLAY || c.paused || c.settingsOpen || c.pendingBonus || !(elapsed>0) || !Float.isFinite(elapsed))return;
        seconds+=elapsed;runSeconds+=elapsed;checkpoint+=elapsed;
        int count=0;boolean below=false,topBelow=false;
        for(GameCore.Enemy e:c.enemies)if(!e.destroyed && !e.dying) {
            count++;float depth=Math.max(0,(e.y-L.playTop)/Math.max(1,L.dangerY-L.playTop));
            deepestEnemy=Math.max(deepestEnemy,depth);below|=depth>=.5f;
            if(!e.sideEntry){deepestTopEnemy=Math.max(deepestTopEnemy,depth);topBelow|=depth>=.5f;}
        }
        if(below)halfwaySeconds+=elapsed;if(topBelow)topHalfwaySeconds+=elapsed;
        peakEnemies=Math.max(peakEnemies,count);
        if(checkpoint>=CHECKPOINT) {checkpoint=0;snapshot(c,"checkpoint");}
    }
    void cleared(GameCore c,GameCore.Enemy e,Layout L) {
        if(!active || !stageOpen)return;
        clears++;if(e.sideEntry)sideClears++;if(c.powerActive())powerClears++;if(cleanup)sweepClears++;
        if(e.fastSingle)rush++;else if(e.swampKind==SwampEnemies.FOG)fog++;
        else if(e.swampKind==SwampEnemies.DIVE)dive++;else if(e.stageMate!=null)pairs++;
        float depth=Math.max(0,Math.min(1.5f,(e.y-L.playTop)/Math.max(1,L.dangerY-L.playTop)));
        depthSum+=depth;maxDepth=Math.max(depth,maxDepth);
        deepestEnemy=Math.max(deepestEnemy,depth);if(!e.sideEntry)deepestTopEnemy=Math.max(deepestTopEnemy,depth);
        if(firstClear<0)firstClear=seconds;lastClear=seconds;
    }
    void event(GameCore c,String event) {if(active && stageOpen)snapshot(c,event);}
    void power(GameCore c,int effect) {if(!active || !stageOpen)return;powerMask|=1<<effect;event(c,"power_start");}
    void assessed(GameCore c,String reason) {if(active && stageOpen)decision=reason;}
    void endStage(GameCore c,String outcome) {
        if(!active || !stageOpen)return;
        snapshot(c,"stage_"+outcome);stageOpen=false;cleanup=false;
    }
    void finish(GameCore c,String outcome) {
        if(!active)return;
        endStage(c,outcome);write(base(c,"run_"+outcome)+"}");active=false;
    }
    void requestExport(GameCore c) {
        if(!enabled)return;
        if(active && stageOpen)snapshot(c,"export_snapshot");exportRequested=true;
    }
    String takeExport() {
        if(!exportRequested)return null;exportRequested=false;
        return "{\"schema\":1,\"event\":\"export\",\"retention_chars\":"+LIMIT+",\"save_failed\":"+saveFailed+"}\n"+history;
    }
    String snapshot() {return history;}
    private void snapshot(GameCore c,String event) {
        write(base(c,event)+settings(c)+",\"correct\":"+Math.max(0,c.hits-hits)+",\"misses\":"+Math.max(0,c.misses-misses)
                +",\"damage\":"+c.hurtThisStage+",\"rescue\":"+c.pushUsed+",\"power_mask\":"+powerMask
                +",\"clears\":"+clears+",\"power_clears\":"+powerClears+",\"cleanup_clears\":"+sweepClears
                +",\"rush\":"+rush+",\"fog\":"+fog+",\"dive\":"+dive+",\"pair_members\":"+pairs
                +",\"deepest_enemy\":"+number(deepestEnemy)+",\"deepest_top_enemy\":"+number(deepestTopEnemy)
                +",\"halfway_s\":"+number(halfwaySeconds)+",\"top_halfway_s\":"+number(topHalfwaySeconds)+",\"side_clears\":"+sideClears
                +",\"peak_enemies\":"+peakEnemies+",\"mean_clear_depth\":"+number(clears==0?0:depthSum/clears)
                +",\"max_clear_depth\":"+number(maxDepth)+",\"first_clear_s\":"+number(firstClear)+",\"last_clear_s\":"+number(lastClear)
                +",\"start_tier\":"+tier+",\"start_keys\":"+keys+",\"start_streak\":"+streak
                +",\"streak\":"+c.rosterStrongStages+",\"decision\":"+quote(decision)+"}");
    }
    private String base(GameCore c,String event) {
        return "{\"schema\":1,\"event\":"+quote(event)+",\"run\":"+run+",\"stage\":"+(stageOpen?stage:c.stage)
                +",\"attempt\":"+attempt+",\"stage_s\":"+number(seconds)+",\"run_s\":"+number(runSeconds)
                +",\"land\":"+Lands.forStage(stageOpen?stage:c.stage)+",\"kids\":"+c.kidsRun+",\"keys\":"+Roster.count(c.runFullRoster)
                +",\"tier\":"+c.runDifficultyTier+",\"saved_tier\":"+c.difficultyTier+",\"score\":"+c.score
                +",\"lives\":"+c.lives+",\"retries\":"+c.highScores.bossRetries+",\"infinite_lives\":"+c.devInfiniteLives;
    }
    private String settings(GameCore c) {
        return ",\"press_cap\":"+c.maxPresses()+",\"distinct_cap\":"+c.distinctEnemyKeys()+",\"speed_factor\":"+number(c.traversalRate())
                +",\"travel_s\":"+number(c.travelSeconds())+",\"spawn_s\":"+number(c.spawnInterval())
                +",\"crowd_cap\":"+c.crowdCap()+",\"quota\":"+c.stageQuota()+",\"power\":"+c.mode;
    }
    private static String number(float n) {return Float.isFinite(n)?Double.toString(Math.round(n*1000.0)/1000.0):"0";}
    static String quote(String s) {
        if(s==null)return "\"\"";
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<s.length();i++) {char ch=s.charAt(i);if(ch=='"' || ch=='\\')out.append('\\').append(ch);
            else if(ch<32)out.append(' ');else out.append(ch);}
        return out.append('"').toString();
    }
}
