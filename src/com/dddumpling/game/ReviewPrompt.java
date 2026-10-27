package com.dddumpling.game;

/** Local review attempts, never an assumption that a player submitted a rating. */
final class ReviewPrompt {
    static final long DAY=86400000L, SESSION_GAP=30L*60*1000, COOLDOWN=90L*DAY;
    int sessions, days;
    long lastDay=-1, lastSession, lastAttempt;
    String attemptedVersion="";
    boolean available, pending, preview, attempted;

    void load(GameCore.Store store) {
        if(store==null)return;
        String[] f=store.loadReviewPrompt().split("\n",-1);
        if(f.length!=8 || !f[0].equals("1"))return;
        try {
            int s=Integer.parseInt(f[1]),d=Integer.parseInt(f[2]);
            long day=Long.parseLong(f[3]),session=Long.parseLong(f[4]),attempt=Long.parseLong(f[5]);
            if(s<0 || s>3 || d<0 || d>2 || day< -1 || session<0 || attempt<0 || !f[7].equals("end"))return;
            sessions=s;days=d;lastDay=day;lastSession=session;lastAttempt=attempt;attemptedVersion=f[6];
        } catch(NumberFormatException ignored) {}
    }
    private void save(GameCore c) {
        if(c.store!=null)c.store.saveReviewPrompt("1\n"+sessions+"\n"+days+"\n"+lastDay+"\n"+lastSession
                +"\n"+lastAttempt+"\n"+attemptedVersion+"\nend");
    }
    void play(GameCore c) {
        pending=false;
        if(c.store==null)return;
        long now=StoreUpdate.now(c),day=now/DAY;
        if(now<=0)return;
        // Quick restarts and short trips out of the app are still the same play session.
        if(lastSession==0 || now-lastSession>=SESSION_GAP)sessions=Math.min(3,sessions+1);
        lastSession=Math.max(lastSession,now);
        if(day>lastDay) {days=Math.min(2,days+1);lastDay=day;}
        save(c);
    }
    boolean eligible(GameCore c) {
        if(c.store==null || sessions<3 || days<2 || !ModeSelector.bossUnlocked(c,Boss.SLIME))return false;
        if(attemptedVersion.equals(version(c)))return false;
        return lastAttempt==0 || StoreUpdate.now(c)-lastAttempt>=COOLDOWN;
    }
    private static String version(GameCore c) {
        String id=c.store.appVersion();
        if(id.startsWith("ios:") || id.startsWith("android:")) {
            String[] parts=id.split(":");
            if(parts.length==3)return parts[0]+":"+parts[1];
        }
        return id;
    }
    void milestone(GameCore c) { if(eligible(c))pending=true; }
    static boolean safe(GameCore c) {
        return UpdatePrompt.suitable(c) && !c.updatePrompt.open && !c.updatePrompt.pending && !c.cloudUi.open
                && !c.preferences.externalBusy && !c.bossRetry.pending;
    }
    int takeRequest(GameCore c) {
        if(!safe(c))return 0;
        if(preview && BuildFlags.DEVELOPER) {preview=false;return 2;}
        if(!available || !pending || attempted || c.storeUpdate.shown || !eligible(c))return 0;
        pending=false;attempted=true;c.storeUpdate.shown=true;
        lastAttempt=Math.max(1,StoreUpdate.now(c));attemptedVersion=version(c);save(c);
        return 1;
    }
    void test() { if(BuildFlags.DEVELOPER)preview=true; }
    void reset(GameCore c) {
        if(!BuildFlags.DEVELOPER)return;
        sessions=days=0;lastDay=-1;lastSession=lastAttempt=0;attemptedVersion="";
        pending=preview=attempted=false;save(c);
    }
}
