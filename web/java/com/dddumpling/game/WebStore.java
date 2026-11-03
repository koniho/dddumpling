package com.dddumpling.game;

import org.teavm.jso.JSBody;

/** Synchronous game Store; the host selects local storage or the loaded Playables save. */
final class WebStore implements GameCore.Store {
    @JSBody(params="key",script="return ddd.storage.get(key);") private static native String get(String key);
    @JSBody(params={"key","value"},script="return ddd.storage.put(key,value);") private static native boolean put(String key,String value);
    private void save(String key,String value) {if(!put(key,value))throw new IllegalStateException("Browser storage unavailable");}
    private int number(String key,int fallback) {String s=get(key);try{return s==null?fallback:Integer.parseInt(s);}catch(NumberFormatException e){return fallback;}}
    public String progressReplica() {String id=get("replica");if(id==null){id="web-"+Long.toHexString(System.currentTimeMillis());save("replica",id);}return id;}
    public byte[] loadProgress() {String s=get("progress");if(s==null)return null;byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    public void saveProgress(byte[] b) {StringBuilder s=new StringBuilder();for(byte v:b){int n=v&255;s.append(Character.forDigit(n>>>4,16)).append(Character.forDigit(n&15,16));}save("progress",s.toString());}
    public String loadPlaythroughLog() {String s=get("PlaythroughLog");return s==null?null:s;}
    public boolean savePlaythroughLog(String value) {save("PlaythroughLog",value);return true;}
    public String loadTown() {String s=get("Town");return s==null?"":s;}
    public boolean saveTown(String value) {save("Town",value);return true;}
    public String loadReviewPrompt() {String s=get("ReviewPrompt");return s==null?"":s;}
    public void saveReviewPrompt(String value) {save("ReviewPrompt",value);}
    public String loadStoreUpdate() {String s=get("StoreUpdate");return s==null?"":s;}
    public void saveStoreUpdate(String value) {save("StoreUpdate",value);}
    public String loadUpdateState() {String s=get("UpdateState");return s==null?"":s;}
    public void saveUpdateState(String value) {save("UpdateState",value);}
    public String loadReleaseSeen() {String s=get("ReleaseSeen");return s==null?BuildFlags.BUILD_ID:s;}
    public void saveReleaseSeen(String value) {save("ReleaseSeen",value);}
    public int loadCaseIndex() {return number("CaseIndex",0);}
    public void saveCaseIndex(int value) {save("CaseIndex",Integer.toString(value));}
    public String loadSurvivalAward() {String s=get("SurvivalAward");return s==null?"":s;}
    public void saveSurvivalAward(String value) {save("SurvivalAward",value);}
    public String loadTimeAttack() {String s=get("TimeAttack");return s==null?"":s;}
    public void saveTimeAttack(String value) {save("TimeAttack",value);}
    public String loadSurvival() {String s=get("Survival");return s==null?"":s;}
    public void saveSurvival(String value) {save("Survival",value);}
    public String loadHighScores() {String s=get("HighScores");return s==null?"":s;}
    public void saveHighScores(String value) {save("HighScores",value);}
    public int loadBest() {return number("Best",0);}
    public void saveBest(int value) {save("Best",Integer.toString(value));}
    public int loadCaveChoice() {return number("CaveChoice",-1);}
    public void saveCaveChoice(int value) {save("CaveChoice",Integer.toString(value));}
    public int loadLandState() {return number("LandState",0);}
    public void saveLandState(int value) {save("LandState",Integer.toString(value));}
    public int loadPlayerSettings() {return number("PlayerSettings",PlayerSettings.DEFAULT);}
    public void savePlayerSettings(int value) {save("PlayerSettings",Integer.toString(value));}
    public boolean loadCloudIntro() {return "true".equals(get("CloudIntro"));}
    public void saveCloudIntro(boolean value) {save("CloudIntro",Boolean.toString(value));}
    public int loadTutorials() {return number("Tutorials",0);}
    public void saveTutorials(int value) {save("Tutorials",Integer.toString(value));}
    public int loadPowerTutorials() {return number("PowerTutorials",0);}
    public void savePowerTutorials(int value) {save("PowerTutorials",Integer.toString(value));}
    public long loadCollected() {String s=get("Collected");return s==null?0:Long.parseLong(s);}
    public void saveCollected(long value) {save("Collected",Long.toString(value));}
    public int[] loadCollectionCounts() {String s=get("CollectionCounts");if(s==null)return null;String[] a=s.split(",");int[] b=new int[a.length];for(int i=0;i<b.length;i++)b[i]=Integer.parseInt(a[i]);return b;}
    public void saveCollectionCounts(int[] a) {StringBuilder s=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)s.append(",");s.append(a[i]);}save("CollectionCounts",s.toString());}
    public int loadCollectTotal() {return number("CollectTotal",0);}
    public void saveCollectTotal(int value) {save("CollectTotal",Integer.toString(value));}
    public int loadSteamerOpens() {return number("SteamerOpens",0);}
    public void saveSteamerOpens(int value) {save("SteamerOpens",Integer.toString(value));}
    public boolean loadCaveMiningNext() {return "true".equals(get("CaveMiningNext"));}
    public void saveCaveMiningNext(boolean value) {save("CaveMiningNext",Boolean.toString(value));}
    public int loadCartTrack() {return number("CartTrack",0);}
    public void saveCartTrack(int value) {save("CartTrack",Integer.toString(value));}
    public int loadMineCarts() {return number("MineCarts",0);}
    public void saveMineCarts(int value) {save("MineCarts",Integer.toString(value));}
    public int loadStarWins() {return number("StarWins",0);}
    public void saveStarWins(int value) {save("StarWins",Integer.toString(value));}
    public int loadRosterState() {return number("RosterState",0);}
    public void saveRosterState(int value) {save("RosterState",Integer.toString(value));}
    public int loadLandBest(int land) {return number("land-"+land,0);}
    public void saveLandBest(int land,int value) {save("land-"+land,Integer.toString(value));}
}
