package com.dddumpling.game;

/** Forced travel and loading leave the lantern clock to the player. */
final class CaveMiningScene extends Draw {
    static final float STRIDE=.64f;
    final float[] age=new float[32],startX=new float[32],height=new float[32];
    int cursor,feedback;
    float clock,distance,shake,hitAge=2,hitFraction,idle,cheer=1.2f,cheerPose;
    CaveMiningScene(){reset();}
    void reset(){clock=distance=shake=idle=cheerPose=0;hitAge=2;cheer=1.2f;cursor=feedback=0;for(int i=0;i<32;i++)age[i]=2;}
    int takeFeedback(){int n=feedback;feedback=0;return n;}
    static float ground(Layout L){return L.playTop+CaveMiningScreen.field(L)*.70f;}
    static float progress(CaveMining m){return m.phase==CaveMining.ADVANCE?Math.min(1,m.travel/CaveMining.WALK_TIME):0;}
    static float walk(CaveMining m){float t=progress(m);return t*t*(3-2*t);}
    static float wallX(CaveMining m){return .79f;}
    static float promptY(CaveMining m,Layout L,float fraction){return ground(L)-L.w*(.48f-.36f*fraction);}
    void hit(GameCore c,float fraction){
        hitFraction=fraction;hitAge=idle=0;shake=.85f;feedback=1;
        for(int j=0;j<4;j++){int i=cursor++%32;age[i]=0;startX[i]=distance+.79f;height[i]=fraction;}
        if(c.sound!=null)c.sound.caveEvent(Sfx.LINKED_THUD);
    }
    void update(GameCore c,float dt){
        clock+=dt;hitAge+=dt;idle+=dt;shake=Math.max(0,shake-dt*5);cheerPose=Math.max(0,cheerPose-dt);
        for(int i=0;i<32;i++)age[i]+=dt;
        CaveMining m=c.mining;
        if(m.phase==CaveMining.REPORT)return;
        cheer-=dt;
        if(cheer<=0 && idle>.6f && (m.phase==CaveMining.DIG||m.phase==CaveMining.FULL)){
            cheer=2.4f;cheerPose=.65f;if(c.sound!=null)c.sound.caveEvent(Sfx.MINING_CHEER);
        }
    }
    void draw(Painter p,GameCore c,Layout L){
        CaveMining m=c.mining;float w=L.w,gy=ground(L),scroll=distance+walk(m)*STRIDE;
        p.fillRect(0,L.playTop,w,L.deckTop,0xFF27212B);
        p.fillRect(0,gy,w,L.deckTop,0xFF594236);
        // Moving ribs and floor sleepers make a completed wall read as forward travel.
        for(int i=-1;i<5;i++){
            float x=(i*.34f-(scroll*.55f% .34f))*w;
            p.fillPoly(new float[]{x,L.playTop,x+w*.17f,L.playTop,x+w*.12f,gy-w*.55f,x+w*.02f,gy-w*.46f},0xFF403039);
            p.line(x,gy+w*.12f,x+w*.13f,L.deckTop,0xFF795845,w*.016f);
        }
        p.line(0,gy,w,gy,0xFFB58B62,w*.012f);
        for(int i=0;i<13;i++){
            float x=(i*.095f-(scroll% .095f))*w;
            CaveArt.tumbling(p,x,L.playTop+w*(.04f+.03f*hash(i*17)),w*(.07f+.045f*hash(i*29)),i*71,255,i);
        }
        float light=CaveMiningScreen.light(m);
        for(int i=4;i>=0;i--)p.fillEllipse(w*.44f,gy-w*.28f,w*(.30f+light*.5f)*(1+i*.1f),w*(.22f+light*.3f),
                Glyph.withAlpha(CaveArt.LAMP,(int)(7+light*5)));
        if(m.phase==CaveMining.ADVANCE){
            wall(p,m,L,(.79f+STRIDE-walk(m)*STRIDE)*w);
            for(int i=0;i<6;i++)CaveArt.tumbling(p,(.79f-walk(m)*STRIDE)*w+i*w*.045f,gy+w*.02f,w*.024f,i*19,180,clock*4+i);
        }else wall(p,m,L,.79f*w);
        p.fillPoly(new float[]{0,gy-w*.16f,w*.36f,gy-w*.16f,w*.33f,gy-w*.11f,0,gy-w*.10f},0xFF795845);
        pile(p,c,L);
        team(p,c,L);
        float px=w*(.48f+.13f*(float)Math.sin(progress(m)*Math.PI));
        float r=w*.095f,py=gy-r*.85f;
        if(m.phase==CaveMining.ADVANCE)py-=Math.abs((float)Math.sin(progress(m)*Math.PI*3))*w*.035f;
        p.fillEllipse(px,gy+r*.12f,r*1.1f,r*.20f,0xFF292027);
        CaveDumpling.draw(p,c.caveChoice,px,py,r,1-m.strike*.07f,clock);
        float fraction=hitAge<.20f?hitFraction:m.pos/(float)Math.max(1,m.length-1);
        float tx=.79f*w,ty=promptY(m,L,fraction),swing=Math.max(0,1-hitAge/.20f);
        float ax=px+r*.65f,ay=py+r*.20f,bx=ax+(tx-ax)*(.45f+.55f*swing),by=ay+(ty-ay)*(.45f+.55f*swing)-w*.08f*(1-swing);
        if(m.phase==CaveMining.DIG || hitAge<.20f){
            p.line(ax,ay,bx,by,0xFFC99A6A,w*.012f);
            p.line(bx-w*.025f,by-w*.035f,bx+w*.035f,by+w*.03f,0xFFD2DEE4,w*.019f);
            p.fillCircle(ax,ay,w*.018f,CaveDumpling.COLORS[Math.max(0,c.caveChoice)]);
        }
        for(int i=0;i<32;i++)if(age[i]<.65f){
            float t=age[i]/.65f,from=(startX[i]-scroll)*w,target=.25f*w;
            float x=from+(target-from)*t,y=promptY(m,L,height[i])+(gy-w*.06f-promptY(m,L,height[i]))*t*t-w*.18f*(float)Math.sin(t*Math.PI);
            x+=(i%4-1.5f)*w*.018f*t;
            CaveArt.tumbling(p,x,y,w*(.015f+(i%3)*.006f),i*77,255,clock*(i%2==0?9:-11));
            if(t<.35f)p.fillCircle(x+w*.035f,y,w*.008f,Glyph.withAlpha(CaveArt.LAMP,100));
        }
    }
    private void wall(Painter p,CaveMining m,Layout L,float x){
        float w=L.w,gy=ground(L);
        p.fillPoly(new float[]{x-w*.025f,gy-w*.57f,x+w*.35f,gy-w*.64f,x+w*.35f,gy+w*.03f,x,gy},0xFF4C3740);
        for(int row=0;row<5;row++)for(int col=0;col<4;col++){
            float y=gy-w*(.055f+row*.11f),rx=x+w*(.035f+col*.10f)+w*.018f*(row%2);
            CaveArt.tumbling(p,rx,y,w*(.063f+.014f*hash(row*11+col)),row*31+col,255,row*.5f+col);
        }
        if(m.phase==CaveMining.DIG)for(int i=0;i<m.pos;i++){
            float y=promptY(m,L,i/(float)(m.length-1));
            p.fillEllipse(x,y,w*.051f,w*.039f,0xFF241C26);
            p.polyline(new float[]{x,y-w*.015f,x+w*.075f,y+w*.026f,x+w*.11f,y-w*.01f},0xFF241C26,w*.006f);
        }
    }
    private void pile(Painter p,GameCore c,Layout L){
        CaveMining m=c.mining;float w=L.w,gy=ground(L),loading=m.phase==CaveMining.PUSH?Math.min(1,m.travel/CaveMining.LOAD_TIME):0;
        for(int i=0;i<m.loads*3;i++){
            float px=m.cartX*w+w*((i%4)-1.5f)*.042f,py=gy-w*(.025f+(i/4)*.045f),lift=0;
            if(m.phase==CaveMining.PUSH){
                float delay=i/(float)Math.max(1,m.loads*3-1)*.22f;
                lift=Math.max(0,Math.min(1,(m.travel-delay)/.30f));
                if(lift>=1)continue;
                float targetX=m.cartX*w+w*((i%3)-1)*.045f,targetY=gy+w*(.055f-(i/3)*.018f);
                px+=(targetX-px)*lift;py+=(targetY-py)*lift-w*.17f*(float)Math.sin(lift*Math.PI);
            }
            CaveArt.tumbling(p,px,py,w*.035f,i*41,255,i+lift*5);
        }
        if(m.phase==CaveMining.PUSH){
            float enter=Math.min(1,m.travel/.20f),cx=(m.cartX-.45f*(1-enter))*w;
            CaveMiningScreen.cart(p,cx,gy+w*.10f,w*.14f,(int)(m.loads*loading),null,clock,m.travel*2);
            p.line(0,gy+w*.21f,w,gy+w*.21f,0xFFB5B3BD,w*.008f);
        }
    }
    private void team(Painter p,GameCore c,Layout L){
        CaveMining m=c.mining;float w=L.w,gy=ground(L),t=m.phase==CaveMining.PUSH?Math.min(1,m.travel/CaveMining.LOAD_TIME):0;
        for(int i=0;i<3;i++){
            float x=w*(.075f+i*.105f),y=gy-w*.20f,r=w*.043f;
            if(m.phase==CaveMining.PUSH){x+=(m.cartX*w+m.direction*w*(.20f+i*.095f)-x)*t;y=gy+w*.10f-w*.016f*(float)Math.sin(clock*24+i);}
            float cheer=cheerPose>0?(float)Math.sin(clock*18+i)*r*.22f:0;
            CaveDumpling.draw(p,i+1,x,y-cheer,r,1,clock);
            for(int side=-1;side<=1;side+=2)p.line(x+side*r*.65f,y,x+side*r*1.25f,y-r*(cheerPose>0?1.0f:.12f),CaveDumpling.COLORS[i+1],r*.15f);
            if(m.phase==CaveMining.PUSH){
                if(m.travel<CaveMining.LOAD_TIME){
                    float lift=(float)Math.sin(Math.min(1,m.travel/CaveMining.LOAD_TIME)*Math.PI);
                    CaveArt.tumbling(p,x+r*.9f,y-r*(.2f+lift),r*.48f,i*23,255,lift*3);
                    p.line(x+r*.5f,y,x+r*.9f,y-r*lift,CaveDumpling.COLORS[i+1],r*.18f);
                }else p.line(x,y+r*.25f,m.cartX*w,gy+w*.08f,0xFFBD9874,w*.003f);
            }
            else if(cheerPose>0)p.arc(x,y-r*.6f,r*1.4f,r*1.4f,-55,35,Glyph.withAlpha(CaveArt.LAMP,160),w*.003f);
        }
    }
}
