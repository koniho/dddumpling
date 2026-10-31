package com.dddumpling.game;

/** Layered bamboo tray and spring-bodied jelly pieces, shared by both native painters. */
final class SquishyFitScreen extends Draw {
    private static final float TAU=6.2831853f;
    private static float[] rim(SquishyFit m,Layout L,float scale,float dy) {
        float[] points=new float[160];
        for(int i=0;i<80;i++) {
            float a=i*TAU/80,r=m.boundary(a);
            points[i*2]=SquishyFit.px(L,(float)Math.cos(a)*r*.38f*scale);
            points[i*2+1]=SquishyFit.py(L,(float)Math.sin(a)*r*.27f*scale)+dy;
        }
        return points;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        SquishyFit m=c.squishyFit;float w=L.w,s=L.unit,cy=SquishyFit.centerY(L),t=m.clock;
        p.fillRect(0,0,w,L.h,0xFF19172C);
        p.fillEllipse(w*.5f,cy,w*.55f,w*.66f,0x142DAFA1);
        for(int i=0;i<9;i++) {
            float x=w*(.08f+(i*.137f)% .88f),y=L.playTop+(i*.193f%1)*(L.dangerY-L.playTop);
            p.fillCircle(x,y+(float)Math.sin(t*.7f+i)*s*.4f,s*(.09f+(i%3)*.04f),0x309DCEBA);
        }
        p.text("SQUISHY FIT",w*.5f,L.playTop+s*.3f,type(s*.95f),GOLD,Painter.CENTER,true);
        p.text(SquishyFit.SHAPES[m.shape()]+" TRAY",w*.5f,L.playTop+s*1.35f,type(s*.45f),INK_DIM,Painter.CENTER,false);
        p.fillPoly(rim(m,L,1.15f,s*.48f),0x55100917);
        p.fillPoly(rim(m,L,1.11f,s*.22f),0xFF624454);
        p.fillPoly(rim(m,L,1.10f,0),0xFFD3A778);
        p.strokePoly(rim(m,L,1.075f,-s*.06f),0xFFFFDCAE,s*.13f);
        p.fillPoly(rim(m,L,1.025f,0),0xFF7E5962);
        p.fillPoly(rim(m,L,1,0),0xFF443B50);
        // Inset bamboo slats end on the same shape boundary as the playable tray.
        for(int row=0;row<17;row++) {
            float y=(row-8)*.029f,begin=0;boolean drawing=false;
            for(int col=0;col<=76;col++) {
                float x=(col-38)*.01f;boolean inside=col<76 && m.contains(x,y);
                if(inside && !drawing){begin=x;drawing=true;}
                if(!inside && drawing){p.line(SquishyFit.px(L,begin),SquishyFit.py(L,y),SquishyFit.px(L,x-.01f),SquishyFit.py(L,y),0xFF595064,s*.06f);drawing=false;}
            }
        }
        for(int pass=0;pass<2;pass++)for(SquishyFit.Piece q:m.pieces)
            if((m.held(q)?1:0)==pass)piece(p,m,q,L);
        if(m.burst>0) {
            float u=1-m.burst,x=SquishyFit.px(L,m.burstX),y=SquishyFit.py(L,m.burstY);
            for(int i=0;i<14;i++) {
                float a=i*TAU/14,rr=w*(.03f+u*.18f);
                p.fillPoly(star(x+(float)Math.cos(a)*rr,y+(float)Math.sin(a)*rr,
                        w*.012f*m.burst,w*.004f*m.burst,4,a),Glyph.withAlpha(SquishyFit.COLORS[i%5],(int)(220*m.burst)));
            }
        }
        float barY=L.dangerY+s*.12f,filled=Math.min(1,m.coverage/m.target());
        p.line(w*.16f,barY,w*.84f,barY,0xFF4A405D,s*.3f);
        if(filled>0)p.line(w*.16f,barY,w*(.16f+.68f*filled),barY,0xFFA0E8BF,s*.3f);
        p.text(Math.round(m.coverage*100)+"% / "+Math.round(m.target()*100)+"%",w*.5f,barY+s*1.4f,type(s*.58f),INK,Painter.CENTER,true);
        float helpY=L.deckTop+s*1.5f;
        String help=m.finished?(m.won?"A PERFECT LITTLE FIT!":"KEPT FOR YOUR NEXT TRY"):
                m.ready?"TAP TO SPLIT":"TAP TO SPLIT";
        if(!m.finished)for(SquishyFit.Piece q:m.pieces)if(m.held(q)) {
            help=q.tooBig?"TOO BIG! SPLIT ME":m.room(q)?"LET GO TO TUCK ME IN":m.fits(q)?"MAKE ROOM FOR ME":"TUCK ME INSIDE";
            break;
        }
        p.text(help,w*.5f,helpY,type(s*.57f),GOLD,Painter.CENTER,true);
        p.text(m.finished?(m.won?"A NEW PAL JOINS YOU":"THIS RUN'S TRAY IS SAVED"):"DRAG THE PIECES INTO THE TRAY",
                w*.5f,helpY+s*1.35f,type(s*.46f),INK_DIM,Painter.CENTER,false);
        if(!m.finished) {
            float y=L.playTop+s*2.7f;
            p.text((int)Math.ceil(m.timer)+"s",w*.88f,y,type(s*.62f),m.timer<3?0xFFFFA4AC:INK,Painter.CENTER,true);
            p.line(w*.14f,y-s*.25f,w*.76f,y-s*.25f,0xFF49415E,s*.12f);
            p.line(w*.14f,y-s*.25f,w*(.14f+.62f*m.timer/m.duration),y-s*.25f,GOLD,s*.12f);
            if(m.ready) {
                SquishyFit.Piece q=m.pieces.get(0);float yy=SquishyFit.py(L,q.y),x=SquishyFit.px(L,q.x);
                float pulse=.5f+.5f*(float)Math.sin(t*4);
                p.strokeCircle(x,yy,w*(.035f+pulse*.018f),Glyph.withAlpha(0xFFFFFFFF,(int)(220-pulse*120)),w*.006f);
            }
        }
        if(m.finished && m.won && !c.bonusParading()) {
            p.fillCircle(w*.5f,cy,w*.13f,0xAA33263F);
            Trinket.draw(p,c.prize,w*.5f,cy,w*.1f,t,true,1);
        }
        if(c.bonusParading())Parade.draw(p,c,L,Math.min(1,c.paradeTimer/Parade.HANDOFF));
    }
    private static void piece(Painter p,SquishyFit m,SquishyFit.Piece q,Layout L) {
        float w=L.w,ox=q.returnX*q.returnAmount(),oy=q.returnY*q.returnAmount();
        float x=SquishyFit.px(L,q.x+ox),y=SquishyFit.py(L,q.y+oy),r=q.r*w;
        boolean hovering=m.held(q),rejected=hovering && !m.room(q);
        if(hovering)p=new OpacityPainter(p,rejected?.72f:.92f);
        float[] source=q.body.outline(),points=new float[source.length];
        for(int i=0;i<points.length;i+=2){points[i]=SquishyFit.px(L,q.x+(source[i]/1000-q.x)*q.drawSqueeze+ox);points[i+1]=SquishyFit.py(L,q.y+(source[i+1]/1000-q.y)/q.drawSqueeze+oy);}
        p.fillEllipse(x,y+r*(hovering?1.05f:.87f),r*(hovering?.95f:.84f),r*.16f,0x44110D22);
        int color=Glyph.mix(q.color,0xFFFF9CA7,q.bad*.4f);
        p.fillPoly(points,Glyph.mix(color,0xFF473653,.36f));
        p.strokePoly(points,q.placed?0xFFA5F0C8:Glyph.mix(color,0xFFFDDFCF,.35f),w*.004f);
        float[] inner=new float[points.length];
        for(int i=0;i<points.length;i+=2){inner[i]=x+(points[i]-x)*.92f;inner[i+1]=y+(points[i+1]-y)*.88f-r*.05f;}
        p.fillPoly(inner,Glyph.withAlpha(color,230));
        p.fillEllipse(x-r*.29f,y-r*.46f,r*.34f,r*.15f,0x80FFFFFF);
        p.fillEllipse(x+r*.34f,y+r*.35f,r*.21f,r*.10f,0x35FFFFFF);
        float ey=y-r*.03f;
        for(int side=-1;side<=1;side+=2) {
            p.fillEllipse(x+side*r*.25f,ey,r*.065f,r*.10f,0xFF3E314E);
            p.fillCircle(x+side*r*.25f+r*.015f,ey-r*.035f,r*.021f,0xFFFFFFFF);
            p.fillEllipse(x+side*r*.40f,ey+r*.17f,r*.11f,r*.06f,0x88F786AA);
        }
        p.arc(x,y+r*.10f,r*.10f,r*.07f,0,180,0xFF3E314E,Math.max(1,w*.0025f));
        if(hovering)p.strokePoly(points,rejected?0xEEFFA7B2:0xFFA5F0C8,w*.008f);
        if(q.bad>0)p.strokePoly(points,Glyph.withAlpha(0xFFFFA4AC,(int)(q.bad*190)),w*.007f);
    }
}
