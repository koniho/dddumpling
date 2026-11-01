package com.dddumpling.game;

final class TestPlaythroughLog extends Check {
    static void all(Layout L) {
        group("local difficulty playthrough recording");
        Mem save=new Mem();save.playLog="";save.rosterState=0;
        GameCore c=new GameCore(save,220);c.startGame();
        check("developer Adventure starts a structured run and stage",c.playthrough.active && save.playLog.contains("\"event\":\"run_start\"") && save.playLog.contains("\"event\":\"stage_start\""));
        GameCore.Enemy enemy=new GameCore.Enemy();enemy.word=new int[]{0,1};enemy.need=new int[]{1,1};
        enemy.y=L.playTop+(L.dangerY-L.playTop)*.4f;c.enemies.add(enemy);
        int writes=save.playLogSaves;
        c.playthrough.tick(c,2,L);check("frames do not write the log",save.playLogSaves==writes);
        enemy.y=L.playTop+(L.dangerY-L.playTop)*.6f;c.playthrough.tick(c,2,L);
        c.playthrough.cleared(c,enemy,L);c.hits=2;c.assessRosterStage();c.playthrough.endStage(c,"cleared");
        check("captures held-stage evidence",save.playLog.contains("\"decision\":\"hold\"") && save.playLog.contains("\"correct\":2"));
        check("captures halfway pressure and clear depth",save.playLog.contains("\"halfway_s\":2.0") && save.playLog.contains("\"deepest_enemy\":0.6") && save.playLog.contains("\"mean_clear_depth\":0.6"));
        c.enterStage(2);c.paused=true;writes=save.playLogSaves;
        c.playthrough.tick(c,100,L);check("pause does not advance checkpoints",save.playLogSaves==writes);
        c.paused=false;c.playthrough.tick(c,PlaythroughLog.CHECKPOINT,L);
        check("periodic checkpoint persists partial stage",save.playLog.contains("\"event\":\"checkpoint\""));
        c.hits+=4;c.hurtThisStage=1;c.devInfiniteLives=true;c.playthrough.event(c,"damage");
        c.playthrough.requestExport(c);String exported=c.playthrough.takeExport();
        check("export includes metadata partial stage and developer flags",exported.contains("\"schema\":1") && exported.contains("export_snapshot") && exported.contains("\"infinite_lives\":true"));
        check("export request is consumed once",c.playthrough.takeExport()==null);
        c.toTitle();check("return to title closes run as abandoned",!c.playthrough.active && save.playLog.contains("\"event\":\"run_abandoned\""));
        int size=save.playLog.length();c.toTitle();check("closing twice does not duplicate endings",save.playLog.length()==size);
        GameCore restored=new GameCore(save,221);restored.startGame();
        check("history survives restart and run IDs increase",save.playLog.contains("\"run\":"+(save.updateTime+1)+","));
        restored.finishDefeat();check("defeat has a distinct terminal event",save.playLog.contains("\"event\":\"run_defeat\""));
        Mem none=new Mem();GameCore unsupported=new GameCore(none,222);unsupported.startGame();
        check("unsupported harness stores do not collect",!unsupported.playthrough.enabled && none.playLogSaves==0);
        Mem other=new Mem();other.playLog="";GameCore survival=new GameCore(other,223);
        survival.collected=Collect.add(0,Collect.bossPrize(Boss.SLIME));survival.modes.selected=ModeSelector.SURVIVAL;survival.startGame();
        check("recording scope excludes Survival",!survival.playthrough.active && other.playLogSaves==0);
        StringBuilder large=new StringBuilder();while(large.length()<PlaythroughLog.LIMIT+100)large.append("{\"event\":\"old\"}\n");
        String bounded=PlaythroughLog.bounded(large.toString());
        check("retention keeps complete bounded lines",bounded.length()<=PlaythroughLog.LIMIT && bounded.startsWith("{") && bounded.endsWith("}\n"));
        check("JSON strings escape quotes and controls",PlaythroughLog.quote("x\"\n\\").equals("\"x\\\" \\\\\""));
        SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.GENERAL);
        check("export chip has a distinct hit",ui.hit(L.w*.5f,ui.playthroughY+ui.testH*.5f)==SettingsUi.HIT_EXPORT_PLAYTHROUGHS);
        try {java.nio.file.Files.write(java.nio.file.Paths.get("build/playthrough-test.jsonl"),save.playLog.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        catch(java.io.IOException failed){check("analysis fixture writes",false);}
    }
}
