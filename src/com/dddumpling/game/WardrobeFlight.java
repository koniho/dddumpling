package com.dddumpling.game;

/** Two bounded paths: the new toy arrives while the replaced toy returns to its box. */
final class WardrobeFlight {
    static final float RETURN_TIME=.44f;
    int incoming=-1,outgoing=-1,category=-1;
    float age,inTime;
    private float[] from,to,old,box;
    boolean active(){return category>=0;}
    boolean hidesIncoming(){return active() && incoming>=0 && age<inTime;}
    void clear(){incoming=outgoing=category=-1;age=0;from=to=old=box=null;}
    void start(int category,int incoming,int outgoing,float[] from,float[] to,float[] old,float[] box,boolean dragged) {
        this.category=category;this.incoming=incoming;this.outgoing=outgoing;
        this.from=from;this.to=to;this.old=old;this.box=box;age=0;
        inTime=incoming<0?RETURN_TIME:dragged?.18f:.46f;
    }
    boolean update(float dt) {
        if(!active())return false;
        float before=age;age+=dt;
        boolean landed=before<inTime && age>=inTime;
        if(age>=Math.max(inTime,RETURN_TIME))clear();
        return landed;
    }
    static float[] bounds(float[] corners,float x,float y,float size) {
        if(corners==null)return new float[]{x,y,size,size};
        float left=corners[0],right=left,top=corners[1],bottom=top;
        for(int i=2;i<corners.length;i+=2){left=Math.min(left,corners[i]);right=Math.max(right,corners[i]);top=Math.min(top,corners[i+1]);bottom=Math.max(bottom,corners[i+1]);}
        return new float[]{(left+right)*.5f,(top+bottom)*.5f,Math.max(1,(right-left)*.5f),Math.max(1,(bottom-top)*.5f)};
    }
    private static float px(float[] a,float[] b,float u,float bend) {
        return a[0]+(b[0]-a[0])*u+4*u*(1-u)*bend;
    }
    private static float py(float[] a,float[] b,float u) {
        return a[1]+(b[1]-a[1])*u;
    }
    void draw(Painter p,Layout L,float clock) {
        if(!active())return;
        if(outgoing>=0 && age<RETURN_TIME)path(p,outgoing,old,box,age/RETURN_TIME,L.w*.12f,L,clock,true);
        if(incoming>=0 && age<inTime)path(p,incoming,from,to,age/inTime,-L.w*.09f,L,clock,false);
    }
    private static void path(Painter p,int item,float[] a,float[] b,float t,float bend,Layout L,float clock,boolean returning) {
        if(a==null || b==null)return;
        float u=t*t*(3-2*t),tail=Math.max(0,u-.36f),fade=Math.min(1,(1-t)*7);
        int color=Accessories.COLOR[item];float[] trail=new float[24];
        for(int i=0;i<12;i++){float s=tail+(u-tail)*i/11;trail[i*2]=px(a,b,s,bend);trail[i*2+1]=py(a,b,s);}
        p.polyline(trail,Glyph.withAlpha(color,(int)(36*fade)),L.unit*.48f);
        p.polyline(trail,Glyph.withAlpha(color,(int)(125*fade)),L.unit*.13f);
        for(int i=0;i<12;i++) {
            float s=u-i*.027f;if(s<0)continue;
            float f=(1-i/12f)*fade,wave=(float)Math.sin(clock*8+i*2.4f);
            float x=px(a,b,s,bend)+wave*L.unit*.28f*(1-f),y=py(a,b,s);
            float r=L.unit*(.08f+(i%3)*.035f)*f;
            p.fillCircle(x,y,r*2.1f,Glyph.withAlpha(color,(int)(35*f)));
            AccessoryArt.star(p,x,y,r,Glyph.withAlpha(Glyph.mix(color,0xFFFFEFBD,.60f),(int)(235*f)));
        }
        float x=px(a,b,u,bend),y=py(a,b,u),w=a[2]+(b[2]-a[2])*u,h=a[3]+(b[3]-a[3])*u;
        float angle=(float)Math.sin(Math.PI*u)*(returning?.45f:-.24f);
        Painter toy=new LinkedPairArt.TurnPainter(new OpacityPainter(p,returning?Math.min(1,(1-t)*10):1),x,y,angle);
        AccessoryArt.thumbnail(toy,item,-w,-h,w,h,clock);
    }
}
