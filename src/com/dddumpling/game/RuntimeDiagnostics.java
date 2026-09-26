package com.dddumpling.game;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Developer-only, private, bounded crash and navigation breadcrumbs. No network access. */
final class RuntimeDiagnostics {
    private static final int LIMIT=64*1024;
    private static File current, previous;
    private static int activitySerial;

    static synchronized void open(Activity activity, boolean restored) {
        if(!BuildFlags.DEVELOPER)return;
        current=new File(activity.getFilesDir(),"runtime-diagnostics.log");
        previous=new File(activity.getFilesDir(),"runtime-diagnostics.previous.log");
        record("activity-create #"+(++activitySerial)+" restored="+restored+" build="+BuildFlags.BUILD_ID);
        if(Build.VERSION.SDK_INT>=30)exits(activity);
    }
    private static void exits(Activity activity) {
        try {
            ActivityManager manager=(ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
            for(android.app.ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(null,0,3))
                record("historical-exit time="+exit.getTimestamp()+" reason="+exit.getReason()
                        +" status="+exit.getStatus()+" description="+exit.getDescription());
        } catch(RuntimeException unavailable) { record("historical-exit unavailable"); }
    }
    static synchronized void record(String event) {
        if(!BuildFlags.DEVELOPER || current==null)return;
        try {
            if(current.length()>=LIMIT) {
                if(previous.exists() && !previous.delete())return;
                if(!current.renameTo(previous))return;
            }
            String line=System.currentTimeMillis()+" uptime="+android.os.SystemClock.uptimeMillis()
                    +" pid="+android.os.Process.myPid()+" "+event+"\n";
            if(line.length()>LIMIT/2)line=line.substring(0,LIMIT/2)+" [truncated]\n";
            try(FileOutputStream out=new FileOutputStream(current,true)) {
                out.write(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch(Exception unavailable) { /* A full disk must not stop the game. */ }
    }
    static void crash(Throwable error) {
        if(!BuildFlags.DEVELOPER)return;
        StringWriter text=new StringWriter();error.printStackTrace(new PrintWriter(text));
        record("CRASH thread="+Thread.currentThread().getName()+"\n"+text);
    }
    private static synchronized String read() {
        StringBuilder text=new StringBuilder();
        for(File file:new File[]{previous,current}) {
            if(file==null || !file.exists())continue;
            try(FileInputStream in=new FileInputStream(file)) {
                byte[] bytes=new byte[LIMIT*2];int used=0,n;
                while(used<bytes.length && (n=in.read(bytes,used,bytes.length-used))>0)used+=n;
                text.append(new String(bytes,0,used,java.nio.charset.StandardCharsets.UTF_8));
            } catch(Exception unavailable) { text.append("Log unavailable: ").append(unavailable).append('\n'); }
        }
        return text.toString();
    }
    static void show(Activity activity) {
        if(!BuildFlags.DEVELOPER)return;
        final String log=read();
        TextView text=new TextView(activity);text.setText(log);text.setTextIsSelectable(true);
        text.setTextSize(10);text.setPadding(20,20,20,20);
        ScrollView scroll=new ScrollView(activity);scroll.addView(text);
        new AlertDialog.Builder(activity).setTitle("Local diagnostics (not uploaded)").setView(scroll)
                .setPositiveButton("Copy log",(dialog,which)-> {
                    ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("DDDUMPLING diagnostics",log));
                }).setNegativeButton("Close",null).show();
        scroll.post(()->scroll.fullScroll(android.view.View.FOCUS_DOWN));
    }
}
