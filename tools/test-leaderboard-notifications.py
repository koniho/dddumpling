#!/usr/bin/env python3
"""Exercise the real Android notification adapter with permission/channel test doubles.
Run check.sh first to compile the shared game model into build/harness.
"""
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
stubs={
'android/os/Build.java': 'package android.os; public class Build {public static class VERSION {public static int SDK_INT=33;}}',
'android/content/pm/PackageManager.java': 'package android.content.pm; public class PackageManager {public static final int PERMISSION_GRANTED=0;}',
'android/content/SharedPreferences.java': '''package android.content;
public class SharedPreferences {
 boolean asked; public boolean getBoolean(String key,boolean fallback){return asked;}
 public Editor edit(){return new Editor();} public class Editor {
 public Editor putBoolean(String key,boolean value){asked=value;return this;} public void apply(){} }
}''',
'android/content/Intent.java': '''package android.content;
public class Intent {
 public static final int FLAG_ACTIVITY_SINGLE_TOP=1,FLAG_ACTIVITY_CLEAR_TOP=2;
 public java.util.HashMap<String,Object> extras=new java.util.HashMap<>(); public String action;
 public Intent(Object context,Class<?> cls){} public Intent setAction(String value){action=value;return this;}
 public Intent addFlags(int f){return this;} public Intent putExtra(String key,int value){extras.put(key,value);return this;}
 public Intent putExtra(String key,String value){extras.put(key,value);return this;}
 public boolean hasExtra(String key){return extras.containsKey(key);}
 public int getIntExtra(String key,int fallback){return extras.containsKey(key)?(Integer)extras.get(key):fallback;}
 public String getStringExtra(String key){return (String)extras.get(key);}
}''',
'android/app/PendingIntent.java': '''package android.app;
public class PendingIntent {
 public static final int FLAG_UPDATE_CURRENT=1,FLAG_IMMUTABLE=2;
 public static android.content.Intent last; public static int flags;
 public static PendingIntent getActivity(Object a,int id,android.content.Intent intent,int f){last=intent;flags=f;return new PendingIntent();}
}''',
'android/app/NotificationChannel.java': '''package android.app;
public class NotificationChannel {public int importance;public NotificationChannel(String id,String name,int i){importance=i;}
 public void setDescription(String d){} public int getImportance(){return importance;} }''',
'android/app/Notification.java': '''package android.app;
public class Notification {
 public static final int VISIBILITY_PRIVATE=0,PRIORITY_HIGH=1,DEFAULT_SOUND=1;public static final String CATEGORY_SOCIAL="social";
 public String title,body; public static class Builder {
 Notification n=new Notification();public Builder(Object a){}public Builder(Object a,String channel){}
 public Builder setSmallIcon(int i){return this;}public Builder setColor(int c){return this;}
 public Builder setContentTitle(String s){n.title=s;return this;}public Builder setContentText(String s){n.body=s;return this;}
 public Builder setContentIntent(PendingIntent p){return this;}public Builder setAutoCancel(boolean b){return this;}
 public Builder setVisibility(int i){return this;}public Builder setCategory(String s){return this;}
 public Builder setPriority(int i){return this;}public Builder setDefaults(int i){return this;}public Notification build(){return n;}
} }''',
'android/app/NotificationManager.java': '''package android.app;
public class NotificationManager {
 public static final int IMPORTANCE_HIGH=4,IMPORTANCE_NONE=0;
 public boolean enabled=true,fail; public int posts;public Notification posted;public NotificationChannel channel;
 public boolean areNotificationsEnabled(){return enabled;}
 public void createNotificationChannel(NotificationChannel c){if(channel==null)channel=c;}
 public NotificationChannel getNotificationChannel(String id){return channel;}
 public void notify(int id,Notification n){if(fail)throw new SecurityException();posts++;posted=n;}
}''',
'com/dddumpling/game/R.java': 'package com.dddumpling.game; final class R {static class drawable {static final int leaderboard_notification=1;}}',
'com/dddumpling/game/MainActivity.java': '''package com.dddumpling.game;
public class MainActivity {
 public static final String NOTIFICATION_SERVICE="notification";
 public int permission=-1,requests;public android.app.NotificationManager manager=new android.app.NotificationManager();
 public int checkSelfPermission(String p){return permission;}public void requestPermissions(String[] p,int r){requests++;}
 public Object getSystemService(String key){return manager;}
}''',
'com/dddumpling/game/NotificationCheck.java': '''package com.dddumpling.game;
public class NotificationCheck extends Check {
 static GameCore ready() {
  GameCore c=TestLeaderboardSubmissions.game(new Mem(),"a");TestLeaderboardSubmissions.adventure(c,100);
  c.leaderboards.acknowledge("a",10,100);c.leaderboards.confirmRank("a",10,100,100,12,false);
  c.toTitle();c.returnFade=0;c.rosterSceneT=0;c.landSeen=-1;c.onboarding.clear();c.leaderboardNotice.update(c,.1f);return c;
 }
 public static void main(String[] args) {
  GameCore c=ready();MainActivity a=new MainActivity();android.content.SharedPreferences prefs=new android.content.SharedPreferences();
  LeaderboardNotifications n=new LeaderboardNotifications(a,c,prefs);n.update();n.update();
  check("requests notification permission once",a.requests==1 && a.manager.posts==0);
  n.permissionResult(237);
  check("denial falls back without posting",a.manager.posts==0 && c.leaderboardNotice.board==10 && !c.leaderboardNotice.systemWaiting);
  c=ready();n=new LeaderboardNotifications(a,c,prefs);n.update();
  check("denied permission is not repeatedly requested",a.requests==1 && !c.leaderboardNotice.systemWaiting);
  a.permission=0;c=ready();n=new LeaderboardNotifications(a,c,prefs);n.update();n.update();
  check("granted permission posts exactly once",a.manager.posts==1 && c.leaderboardNotice.board==-1);
  check("notification contains confirmed rank and board",a.manager.posted.body.equals("#12 global · ADVENTURE"));
  check("tap carries captured board and owner",android.app.PendingIntent.last.getIntExtra("leaderboard_board",-1)==10
    && android.app.PendingIntent.last.getStringExtra("leaderboard_owner").equals("a"));
  check("tap intent is immutable",(android.app.PendingIntent.flags & android.app.PendingIntent.FLAG_IMMUTABLE)!=0);
  a.manager.channel.importance=0;c=ready();n=new LeaderboardNotifications(a,c,prefs);n.update();
  check("blocked channel falls back",a.manager.posts==1 && c.leaderboardNotice.board==10);
  a.manager.channel.importance=4;a.manager.fail=true;c=ready();n=new LeaderboardNotifications(a,c,prefs);n.update();
  check("system posting failure falls back",a.manager.posts==1 && !c.leaderboardNotice.systemWaiting);
  a=new MainActivity();c=ready();n=new LeaderboardNotifications(a,c,new android.content.SharedPreferences());n.update();
  c.leaderboards.account("b",true);a.permission=0;n.permissionResult(237);
  check("account changed during permission cannot post old score",a.manager.posts==0);
  a=new MainActivity();c=ready();n=new LeaderboardNotifications(a,c,new android.content.SharedPreferences());n.update();
  c.leaderboards.account(null,true);a.permission=0;n.permissionResult(237);c.leaderboardNotice.update(c,.1f);
  check("permission return retains pending alert during reauthentication",a.manager.posts==0 && c.leaderboardNotice.board==10);
  c.leaderboards.account("a",true);n.update();
  check("same account resumes pending system notification",a.manager.posts==1);
  android.os.Build.VERSION.SDK_INT=25;a=new MainActivity();c=ready();n=new LeaderboardNotifications(a,c,new android.content.SharedPreferences());n.update();
  check("pre-channel Android posts without runtime permission",a.manager.posts==1 && a.requests==0);
  System.out.println("Android leaderboard notifications: "+pass+" passed, "+fail+" failed");
  if(fail>0)throw new AssertionError("notification regression");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='leaderboard-notifications-',dir=root/'build') as tmp:
    out=Path(tmp)
    files=[]
    for name,content in stubs.items():
        path=out/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content);files.append(str(path))
    subprocess.run(['javac','-nowarn','-cp',str(root/'build/harness'),'-d',str(out/'classes'),*files,
                    str(root/'src/com/dddumpling/game/LeaderboardNotifications.java')],check=True)
    subprocess.run(['java','-cp',str(out/'classes')+':'+str(root/'build/harness'),'com.dddumpling.game.NotificationCheck'],check=True)
