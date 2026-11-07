package com.dddumpling.game;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/** Local system alerts after a service-confirmed score; never an upload source. */
final class LeaderboardNotifications {
    private static final int PERMISSION=237;
    private static final String CHANNEL="leaderboard_results",BOARD="leaderboard_board",OWNER="leaderboard_owner";
    private final MainActivity activity;
    private final GameCore core;
    private final SharedPreferences prefs;
    private int board=-1;
    private long rank;
    private String owner;
    private boolean requesting;
    LeaderboardNotifications(MainActivity activity,GameCore core,SharedPreferences prefs) {
        this.activity=activity;this.core=core;this.prefs=prefs;
    }
    void open(Intent intent) {
        if(intent!=null && intent.hasExtra(BOARD))
            core.leaderboardUi.notification(intent.getIntExtra(BOARD,-1),intent.getStringExtra(OWNER));
    }
    void update() {
        if(board<0) {
            board=core.leaderboardNotice.takeSystem(core);if(board<0)return;
            rank=core.leaderboardNotice.rank;owner=core.leaderboardNotice.owner;
        }
        if(core.leaderboards.owner().isEmpty())return;
        if(!owner.equals(core.leaderboards.owner())) {complete(false);return;}
        if(requesting || !LeaderboardNotice.safe(core))return;
        if(Build.VERSION.SDK_INT>=33 && activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED) {
            if(prefs.getBoolean("leaderboard_notifications_asked",false)) {complete(false);return;}
            prefs.edit().putBoolean("leaderboard_notifications_asked",true).apply();requesting=true;
            try {activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},PERMISSION);}
            catch(RuntimeException unavailable) {requesting=false;complete(false);}
            return;
        }
        try {
            NotificationManager manager=(NotificationManager)activity.getSystemService(MainActivity.NOTIFICATION_SERVICE);
            if(manager==null || !manager.areNotificationsEnabled()) {complete(false);return;}
            if(Build.VERSION.SDK_INT>=26) {
                NotificationChannel channel=new NotificationChannel(CHANNEL,"Leaderboard results",NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Your confirmed global rank after a new best score");
                manager.createNotificationChannel(channel);
                if(manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE) {complete(false);return;}
            }
            Intent intent=new Intent(activity,MainActivity.class).setAction("leaderboard."+board)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BOARD,board).putExtra(OWNER,owner);
            PendingIntent tap=PendingIntent.getActivity(activity,board,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder notice=Build.VERSION.SDK_INT>=26?new Notification.Builder(activity,CHANNEL):new Notification.Builder(activity);
            notice.setSmallIcon(R.drawable.leaderboard_notification).setColor(0xFFFFD56C)
                    .setContentTitle("You're on the leaderboard!")
                    .setContentText("#"+rank+" global · "+LeaderboardCatalog.title(board))
                    .setContentIntent(tap).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setCategory(Notification.CATEGORY_SOCIAL).setPriority(Notification.PRIORITY_HIGH);
            if(Build.VERSION.SDK_INT<26)notice.setDefaults(Notification.DEFAULT_SOUND);
            manager.notify(2370+board,notice.build());complete(true);
        }catch(RuntimeException unavailable){complete(false);}
    }
    void permissionResult(int request) {
        if(request==PERMISSION) {requesting=false;update();}
    }
    private void complete(boolean shown) {
        core.leaderboardNotice.systemResult(core,owner,board,rank,shown);board=-1;
    }
}
