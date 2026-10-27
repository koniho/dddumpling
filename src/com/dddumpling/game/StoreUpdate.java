package com.dddumpling.game;

/** Store availability is ephemeral; only check throttling and the player's reminder choice persist. */
final class StoreUpdate {
    static final long CHECK_INTERVAL=6L*60*60*1000, REMIND_INTERVAL=7L*24*60*60*1000;
    String available="", deferred="", offered="";
    long remindAt, checkedAt;
    boolean shown, requested;

    void load(GameCore.Store store) {
        if(store==null)return;
        String[] data=store.loadStoreUpdate().split("\n",-1);
        if(data.length!=4 || !data[0].equals("1"))return;
        try {
            long reminder=Long.parseLong(data[2]), checked=Long.parseLong(data[3]);
            if(reminder<0 || checked<0)return;
            deferred=data[1];remindAt=reminder;checkedAt=checked;
        } catch(NumberFormatException ignored) {}
    }
    private void save(GameCore c) {
        if(c.store!=null)c.store.saveStoreUpdate("1\n"+deferred+"\n"+remindAt+"\n"+checkedAt);
    }
    static long now(GameCore c) { return c.store==null?System.currentTimeMillis():c.store.updateTimeMillis(); }
    boolean beginCheck(GameCore c) {
        long time=now(c);
        if(checkedAt>0 && time>=checkedAt && time-checkedAt<CHECK_INTERVAL)return false;
        checkedAt=time;available="";save(c);return true;
    }
    void result(String version) { available=version==null?"":version; }
    boolean ready(GameCore c) {
        if(shown || available.isEmpty())return false;
        long time=now(c);
        return (!available.equals(deferred)
                || time>=remindAt || remindAt-time>REMIND_INTERVAL);
    }
    void show() { shown=true;offered=available; }
    void choose(GameCore c,boolean update) {
        deferred=offered;remindAt=now(c)+REMIND_INTERVAL;requested=update;save(c);
    }
    boolean takeRequest() { boolean value=requested;requested=false;return value; }

    /** StoreKit supplies alpha-3; the lookup API takes alpha-2. Unknown storefronts stay silent. */
    static String country(String alpha3) {
        if(alpha3==null)return "";
        for(String country:java.util.Locale.getISOCountries())
            if(new java.util.Locale("",country).getISO3Country().equalsIgnoreCase(alpha3))return country;
        return "";
    }
}
