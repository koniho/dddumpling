package com.dddumpling.game;

/** The incoming rail bends and the passengers' lean share the ride's signed turn. */
final class CaveCartScreen extends Draw {
    static final float CART_Z=.684f;
    static float field(Layout L){return L.deckTop-L.playTop;}
    static float railX(CaveCart m,float z){
        float ahead=(CART_Z-z)*2.2f;
        float total=0,at=m.progress+m.segment/CaveCart.SEGMENT;
        // Integrate curvature into heading, then position: a bend stays rounded even at its apex.
        for(int i=0;i<32;i++){float u=ahead*(i+.5f)/32;total+=CaveCart.curve(Math.max(0,at+u))*(ahead-u);}
        return .5f+total/32*ahead*.20f;
    }
    static float bank(CaveCart m){return m.turn*.28f+m.balance*.08f-m.lean*.04f;}
    static void draw(Painter p,GameCore c,Layout L){
        CaveCart m=c.cart;float w=L.w,top=L.playTop,h=field(L),s=L.unit,t=m.scene.clock;
        p.fillRect(0,0,w,L.h,0xFF100F1D);
        p.save();p.clipRect(0,top,w,L.deckTop);
        p.translate((float)Math.sin(t*91)*w*.008f*m.scene.rumble,(float)Math.sin(t*113)*w*.006f*m.scene.rumble);
        float vy=top+h*.18f;
        p.fillEllipse(w*.5f,vy,w*.42f,h*.72f,0xFF262536);
        for(int i=12;i>=0;i--){
            float z=(i/13f+t*.32f)%1;z=z*z;
            float cx=railX(m,z)*w,y=vy+h*.79f*z,rx=w*(.10f+z*.64f),ry=h*(.1f+z*.60f);
            p.polyline(new float[]{cx-rx,y+ry*.35f,cx-rx*.93f,y-ry*.48f,cx-rx*.48f,y-ry,cx+rx*.22f,y-ry*1.12f,cx+rx*.82f,y-ry*.65f,cx+rx,y+ry*.35f},Glyph.mix(0xFF302B3E,0xFF786353,z),w*(.009f+.016f*z));
            if(i%3==0){float x=cx-rx*.81f,yy=y-ry*.39f;
                p.fillCircle(x,yy,w*(.012f+.023f*z),0xFFBDEAD8);
                p.fillCircle(x,yy,w*(.029f+.045f*z),0x226DDDC6);
            }
        }
        p.fillPoly(new float[]{w*.35f,vy,w*.65f,vy,w*1.25f,top+h,w*-.25f,top+h},0xFF3A3038);
        for(int i=0;i<26;i++){
            float z=(i/26f+t*.56f)%1;z=z*z;
            float x=railX(m,z)*w,y=vy+h*.79f*z,r=w*(.018f+z*.32f);
            p.line(x-r,y,x+r,y,0xFF8B6C5B,w*(.003f+z*.02f));
        }
        for(int side=-1;side<=1;side+=2){
            float[] points=new float[162];
            for(int j=0;j<=80;j++){float z=j/80f;points[j*2]=railX(m,z)*w+side*w*(.012f+.245f*z);points[j*2+1]=vy+h*.79f*z;}
            p.polyline(points,0xFFB7B9CF,w*.012f);p.polyline(points,0xFFECE1C2,w*.003f);
        }
        for(int i=0;i<18;i++){
            float z=(hash(i*43)+t*(.6f+hash(i)*.3f))%1,side=i%2==0?-1:1;
            float x=w*(.5f+side*(.28f+z*.28f)),y=top+h*(.2f+z*.8f);
            p.line(x,y,x+side*w*.06f*z,y+h*.12f*z,Glyph.withAlpha(0xFFE9C394,(int)(z*110)),w*.004f);
        }
        if(m.progress>=CaveCart.TRACK-2 || m.won){
            float z=m.won?.7f:Math.max(.06f,(m.progress+m.segment/CaveCart.SEGMENT-(CaveCart.TRACK-2))*.35f);
            float gx=railX(m,z)*w,gy=vy+h*.79f*z,gr=w*(.06f+.27f*z);
            p.line(gx-gr,gy,gx-gr,gy-gr*1.5f,GOLD,w*.014f);
            p.line(gx+gr,gy,gx+gr,gy-gr*1.5f,GOLD,w*.014f);
            for(int i=0;i<10;i++)for(int row=0;row<2;row++)
                p.fillRect(gx-gr+i*gr*.2f,gy-gr*1.5f+row*gr*.2f,gx-gr+(i+1)*gr*.2f,gy-gr*1.5f+(row+1)*gr*.2f,(i+row)%2==0?INK:0xFF30283B);
        }
        float x=railX(m,CART_Z)*w,y=top+h*.72f+(float)Math.sin(t*39)*w*.004f,tilt=bank(m),r=w*.235f;
        boolean panic=Math.abs(m.balance)>.52f;
        // Far axle and tub opening sit behind the riders; rear wheel treads have no face-on spokes.
        for(int side=-1;side<=1;side+=2)wheel(p,x+side*r*.72f,y-r*.12f+side*r*.72f*tilt,r*.65f,tilt,t);
        p.fillPoly(new float[]{x-r*.78f,y-r*.55f-r*.78f*tilt,x+r*.78f,y-r*.55f+r*.78f*tilt,
                x+r,y-r*.04f+r*tilt,x-r,y-r*.04f-r*tilt},0xFF392B42);
        p.line(x-r*.78f,y-r*.55f-r*.78f*tilt,x+r*.78f,y-r*.55f+r*.78f*tilt,0xFFC09A89,w*.013f);
        for(int i=0;i<3;i++){
            float dx=(i-1)*r*.57f+m.lean*r*.24f-m.balance*r*.19f,dy=-r*.53f+dx*tilt;
            if(m.spilled){float a=Math.max(0,m.scene.spillAge-i*.055f);dx-=Math.signum(m.balance)*w*a*(.65f+i*.14f);dy+=w*(-a*1.65f+a*a*2.3f);}
            float rr=r*.32f;
            for(int side=-1;side<=1;side+=2)p.line(x+dx+side*rr*.65f,y+dy,x+dx+side*rr*1.3f,y+dy-rr*(panic||m.spilled?1.1f:.2f),0xFFFFDDA8,w*.013f);
            int who=i==1?c.caveChoice:i+2;
            if(m.spilled)CaveDumpling.draw(p,who,x+dx,y+dy,rr,1,t,true);
            else passengerBack(p,who,x+dx,y+dy,rr,t+i,panic);
        }
        for(int side=-1;side<=1;side+=2){float wx=x+side*r*.83f,wy=y+r*.46f+side*r*.83f*tilt;
            wheel(p,wx,wy,r,tilt,t);
            if(panic)for(int i=0;i<8;i++){float a=(t*4+i*.125f)%1;
                float sx=wx-side*w*a*.13f,sy=wy+w*a*a*.17f;
                p.line(sx,sy,sx+side*w*.02f,sy-w*.025f,GOLD,w*.004f*(1-a));}
        }
        p.fillPoly(new float[]{x-r,y-r*.08f-r*tilt,x+r,y-r*.08f+r*tilt,
                x+r*.88f,y+r*.43f+r*.88f*tilt,x-r*.88f,y+r*.43f-r*.88f*tilt},0xFF956B80);
        p.line(x-r,y-r*.08f-r*tilt,x+r,y-r*.08f+r*tilt,0xFFF1C79B,w*.018f);
        p.line(x-r*.84f,y+r*.28f-r*.84f*tilt,x+r*.84f,y+r*.28f+r*.84f*tilt,0xFFBC8D94,w*.008f);
        for(int side=-1;side<=1;side+=2)p.fillCircle(x+side*r*.75f,y+r*.10f+side*r*.75f*tilt,w*.008f,0xFFF1C79B);
        if(panic)p.polyline(new float[]{w*.012f,top+w*.012f,w*.012f,L.deckTop-w*.012f,w*.988f,L.deckTop-w*.012f,w*.988f,top+w*.012f},Glyph.withAlpha(ROSE,(int)(100+70*Math.sin(t*15))),w*.015f);
        p.restore();
        p.text("CART RUSH",w*.5f,top+s*.5f,type(s*.8f),INK,Painter.CENTER,true);
        float barY=top+h*.105f;
        p.line(w*.15f,barY,w*.85f,barY,0xFF4A415A,w*.015f);
        float done=m.won?1:m.progress/(float)CaveCart.TRACK;
        p.line(w*.15f,barY,w*(.15f+.70f*done),barY,GOLD,w*.015f);
        p.fillCircle(w*(.15f+.70f*done),barY,w*.018f,GOLD);
        if(m.phase==CaveCart.REPORT){
            p.text(m.won?"TRACK COMPLETE!":"PROGRESS SAVED",w*.5f,top+h*.30f,type(s*.72f),GOLD,Painter.CENTER,true);
            if(m.won)Trinket.draw(p,c.prize,w*.5f,top+h*.46f,w*.075f,t,true,1);
        }else{
            float next=CaveCart.bend(m.progress);
            arrow(p,w*.5f,top+h*.29f,w*.065f,next<0?-1:1,GOLD);
            p.text(m.ready>0?"LEAN INTO THE TURN":"HOLD TO LEAN",w*.5f,top+h*.91f,type(s*.55f),INK,Painter.CENTER,true);
            for(int side=-1;side<=1;side+=2){
                int col=m.intent*side>.2f?GOLD:0xFF8D829F;
                arrow(p,w*(.5f+side*.33f),top+h*.81f,w*.052f,side,col);
            }
        }
        p.fillRect(0,L.deckTop,w,L.h,0xFF221E31);
        Renderer.keys(m.phase==CaveCart.RIDE?p:new OpacityPainter(p,.2f),c,L);
        if(c.bonusParading())Parade.draw(p,c,L,Math.min(1,c.paradeTimer/.35f));
    }
    private static void wheel(Painter p,float x,float y,float r,float tilt,float t){
        float half=r*.075f,height=r*.26f;
        p.fillPoly(new float[]{x-half,y-height-half*tilt,x+half,y-height+half*tilt,
                x+half,y+height+half*tilt,x-half,y+height-half*tilt},0xFF181C2B);
        p.line(x-half,y-height,x-half,y+height,0xFF9CA9B7,r*.028f);
        for(int i=0;i<3;i++){float yy=y-height+((i/3f+t*3)%1)*height*2;
            p.line(x-half*.65f,yy-half*tilt,x+half*.65f,yy+half*tilt,0xFF515466,r*.025f);}
    }
    private static void passengerBack(Painter p,int who,float x,float y,float r,float t,boolean panic){
        int col=CaveDumpling.COLORS[Math.max(0,Math.min(CaveDumpling.COUNT-1,who))];
        float[] outline=new float[80];
        for(int i=0;i<40;i++){double angle=i*Math.PI/20;float sy=(float)Math.sin(angle);
            outline[i*2]=x+r*(float)Math.cos(angle);
            outline[i*2+1]=y+r*(.03f+.77f*sy-.34f*(float)Math.pow(Math.max(0,-sy),18));}
        p.fillPoly(outline,col);
        for(int i=-2;i<=2;i++)p.polyline(new float[]{x+i*r*.08f,y-r*.89f,x+i*r*.21f,y-r*.48f,x+i*r*.24f,y-r*.16f},Glyph.mix(col,0xFF766074,.28f),r*.04f);
        p.arc(x,y+r*.1f,r*.72f,r*.36f,15,150,Glyph.mix(col,INK,.22f),r*.035f);
        if(panic)for(int side=-1;side<=1;side+=2){float yy=y-r*(.7f+.10f*(float)Math.sin(t*18));
            p.line(x+side*r*.95f,yy,x+side*r*1.16f,yy-r*.2f,GOLD,r*.07f);}
    }
    private static void arrow(Painter p,float x,float y,float r,int side,int color){
        p.polyline(new float[]{x-side*r*.35f,y-r*.7f,x+side*r*.5f,y,x-side*r*.35f,y+r*.7f},color,r*.22f);
    }
}
