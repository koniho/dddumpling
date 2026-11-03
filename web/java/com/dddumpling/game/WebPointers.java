package com.dddumpling.game;

/** Browser pointer IDs may be sparse. Up packets retain the departing pointer. */
final class WebPointers {
    private final IOSGame game;
    private final int[] ids=new int[16];
    private final float[] xs=new float[16],ys=new float[16];
    private int count;
    WebPointers(IOSGame game) {this.game=game;}
    void event(int action,int id,float x,float y) {
        int at=-1;for(int i=0;i<count;i++)if(ids[i]==id)at=i;
        if(action==0) {
            if(at>=0 || count==ids.length || id<0)return;
            at=count++;ids[at]=id;
        } else if(at<0)return;
        xs[at]=x;ys[at]=y;
        int kind=action==0?(count==1?0:5):action==1?(count==1?1:6):action;
        game.touch(new IOSTouch(kind,at,java.util.Arrays.copyOf(ids,count),
                java.util.Arrays.copyOf(xs,count),java.util.Arrays.copyOf(ys,count)));
        if(action==1) {
            count--;
            for(int i=at;i<count;i++){ids[i]=ids[i+1];xs[i]=xs[i+1];ys[i]=ys[i+1];}
        } else if(action==3)count=0;
    }
    void cancel() {if(count>0)event(3,ids[0],xs[0],ys[0]);game.cancelKeyboard();}
}
