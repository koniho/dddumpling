package com.dddumpling.game;

/** Visual identity travels with the painter, never through global mutable game state. */
final class Skins {
    static final int DEFAULT=0, HALLOWEEN=1;
    static final String[] NAMES={"DEFAULT","HALLOWEEN"};
    static final int COUNT=NAMES.length;
    // Preserve familiar key positions: cream, red, orange, purple, green, then black.
    static final int[] HALLOWEEN_COLORS={0xFFF2F2FF,0xFFF26B78,0xFFFFA64F,0xFFBA8BE8,0xFF8DD48B,0xFF51495F};
    static final int HALLOWEEN_CAT_KEY=0xFFC4C4CC;
    static final String[] HALLOWEEN_NAMES={"GHOST","RED BAT","PUMPKIN","WITCH","FRANKENSTEIN","BLACK CAT"};
    static int valid(int skin) { return skin>=0 && skin<COUNT?skin:DEFAULT; }
    static boolean halloween(Painter p) { return p.skin()==HALLOWEEN; }
    static int keyColor(Painter p,int glyph) { return halloween(p)?(glyph==Kawaii.BLOB?HALLOWEEN_CAT_KEY:HALLOWEEN_COLORS[glyph]):Glyph.COLOR[glyph]; }
    static Painter painter(Painter p,int skin) { return new Theme(p,valid(skin)); }
    static String bossName(int skin,int boss) {
        if(boss<0 || boss>=Boss.COUNT)return "";
        WorldCatalog.BossEntry b=WorldCatalog.BOSSES[boss];
        return valid(skin)==HALLOWEEN && b.halloween!=null?b.halloween:b.name;
    }
    static String bossTitle(int skin,int boss) {
        StringBuilder title=new StringBuilder();
        for(String word:bossName(skin,boss).toLowerCase(java.util.Locale.ROOT).split(" ")) {
            if(word.isEmpty())continue;
            if(title.length()>0)title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }
    private static final class Theme extends OpacityPainter {
        final int selected;
        Theme(Painter p,int selected) {super(p,1);this.selected=selected;}
        public int skin() {return selected;}
    }
    static Painter ghost(Painter p) { return halloween(p)?new BossPalette(p,0):p; }
    static Painter candy(Painter p) { return halloween(p)?new BossPalette(p,1):p; }
    static Painter squid(Painter p) { return halloween(p)?new BossPalette(p,2):p; }
    static Painter mushroom(Painter p) { return halloween(p)?new BossPalette(p,3):p; }
    private static final class BossPalette extends OpacityPainter {
        final int kind;
        BossPalette(Painter p,int kind) {super(p,1);this.kind=kind;}
        public ArtStamp stamps() {return null;}
        int color(int c) {
            int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            float bright=Math.max(r,Math.max(g,b))/255f;
            int mapped=c;
            if(kind==0 && g>r*1.1f && g>b*1.1f)
                mapped=Glyph.mix(0xFF787692,0xFFF5F4FF,bright);
            else if(kind==1 && b>g*1.15f && r>g*1.1f && (b-g>40 || r-g>45))
                mapped=bright<.30f?Glyph.mix(0xFF291D39,0xFF67355E,bright/.30f):Glyph.mix(0xFFB65392,0xFFFFBADE,bright);
            else if(kind==2 && r>g*1.12f && b>g*1.1f)
                mapped=Glyph.mix(0xFF536B94,0xFFD7F9E9,bright);
            else if(kind==3 && r>g*1.2f && r>b*1.2f)
                mapped=Glyph.mix(0xFF593385,0xFFC487F0,bright);
            return (c&0xFF000000)|(mapped&0xFFFFFF);
        }
    }
    static float squidWidth(float sn) {
        // One mantle widens into the side fins, then tucks back under the face.
        return sn<-.15f?.74f+.85f*(1+sn):.80f+.6625f*Math.max(0,1-(sn+.15f)/.60f);
    }
    static float[] squidOutline(float x,float y,float rx,float ry) {
        float[] outline=new float[96];
        for(int i=0;i<48;i++) {
            float a=i*Softbody.TAU/48f,sn=(float)Math.sin(a),cs=(float)Math.cos(a);
            outline[i*2]=x+rx*cs*squidWidth(sn);
            outline[i*2+1]=y+ry*(sn*(sn<0?1.92f:.82f)-.11f);
        }
        return outline;
    }
    static void squidHead(Painter p,float[] outline,float x,float y,float ca,float sa,int col,float fade) {
        p.fillPoly(outline,Glyph.withAlpha(Glyph.mix(col,0xFF324A65,.22f),(int)(105*fade)));
        // Broad side planes stay inside the same silhouette, with no separate fin outlines.
        float[] face=outline.clone();
        for(int i=0;i<face.length;i+=2) {
            float dx=outline[i]-x,dy=outline[i+1]-y;
            float localX=(dx*ca+dy*sa)*.72f,localY=-dx*sa+dy*ca;
            face[i]=x+localX*ca-localY*sa;
            face[i+1]=y+localX*sa+localY*ca;
        }
        p.fillPoly(face,Glyph.withAlpha(Glyph.mix(col,0xFFFFFFFF,.12f),(int)(90*fade)));
    }
    /** A single tapered ribbon avoids dark overlap seams in translucent tentacles. */
    static void squidArm(Painter p,float[] curve,float width,int col,float fade) {
        int count=curve.length/2;
        for(int layer=0;layer<2;layer++) {
            float[] ribbon=new float[count*4];
            for(int i=0;i<count;i++) {
                int before=Math.max(0,i-1),after=Math.min(count-1,i+1);
                float dx=curve[after*2]-curve[before*2],dy=curve[after*2+1]-curve[before*2+1];
                float len=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy));
                float u=i/(float)(count-1),thick=width*(1-.91f*u);
                float offset=layer==0?0:-thick*.19f,half=thick*(layer==0?.5f:.14f);
                float nx=-dy/len,ny=dx/len,x=curve[i*2],y=curve[i*2+1];
                ribbon[i*2]=x+nx*(offset+half);ribbon[i*2+1]=y+ny*(offset+half);
                int back=(count*2-1-i)*2;
                ribbon[back]=x+nx*(offset-half);ribbon[back+1]=y+ny*(offset-half);
            }
            p.fillPoly(ribbon,Glyph.withAlpha(layer==0?col:Glyph.mix(col,0xFFF1FFF9,.65f),(int)((layer==0?130:65)*fade)));
        }
    }
    /** Ten non-interactive wisps: no arm IDs, prompts, hit regions or gameplay state. */
    static void squidFrill(Painter p,float x,float y,float rx,float ry,float angle,float clock,float fade) {
        if(!halloween(p))return;
        float ca=(float)Math.cos(angle),sa=(float)Math.sin(angle);
        for(int i=0;i<10;i++) {
            float spread=(i-4.5f)/4.5f,phase=clock*1.7f+i*2.4f;
            float length=1.55f+.42f*(float)Math.sin(i*2.1f);
            float[] curve=new float[26];
            for(int n=0;n<13;n++) {
                float u=n/12f;
                float dx=rx*(spread*(.40f+u*.82f)+.15f*u*(float)Math.sin(phase+u*4.5f));
                float dy=ry*(.38f+u*length+.12f*u*(float)Math.cos(phase+u*3));
                curve[n*2]=x+dx*ca-dy*sa;curve[n*2+1]=y+dx*sa+dy*ca;
            }
            squidArm(p,curve,rx*(.14f+.04f*(i%3)),i%2==0?0xFFBFEFDB:0xFFACDDE9,fade*.58f);
        }
    }
    static void candyWrap(Painter p,float x,float y,float rx,float ry,float clock,float alpha) {
        if(!halloween(p))return;
        for(int side=-1;side<=1;side+=2) {
            float flap=.08f*(float)Math.sin(clock*4+side);
            float[] shape=HalloweenKeys.shape(x,y,rx*side,ry,
                    new float[]{.75f,-.18f,1.3f,-.61f,1.47f,-.47f,1.34f,flap,1.48f,.45f,1.23f,.62f,.74f,.18f});
            p.fillPoly(shape,Glyph.withAlpha(0xFFCA79B0,(int)(220*alpha)));
            p.strokePoly(shape,Glyph.withAlpha(0xFFFFD7EF,(int)(220*alpha)),Math.min(rx,ry)*.045f);
            for(int i=-1;i<=1;i++)p.line(x+side*rx*.81f,y,x+side*rx*1.31f,y+ry*(i*.35f+flap),
                    Glyph.withAlpha(0xFFFFE4F5,(int)(150*alpha)),ry*.04f);
        }
    }
    static void crocMask(Painter p,float x,float y,float w,float h,float clock) {
        if(!halloween(p))return;
        // An orange costume domino sits around the eyes, leaving the attack tells visible.
        p.line(x-w*.88f,y-h*.48f,x-w*.68f,y-h*.48f,0xFF73509C,w*.08f);
        p.line(x+w*.40f,y-h*.54f,x+w*.62f,y-h*.46f,0xFF73509C,w*.08f);
        for(int side=0;side<2;side++) {
            float ex=x+w*(side==0?-.43f:.20f),ey=y+h*(side==0?-.48f:-.58f),s=side==0?1:.79f;
            p.arc(ex,ey,w*.23f*s,h*.59f*s,0,360,0xFF713A65,w*.09f);
            p.arc(ex,ey,w*.23f*s,h*.59f*s,0,360,0xFFFFAE56,w*.055f);
            p.arc(ex,ey,w*.23f*s,h*.59f*s,205,90,0xFFFFE6B0,w*.025f);
        }
        // A crooked fabric nose and two floppy rabbit ears make the disguise deliberately silly.
        float bounce=(float)Math.sin(clock*3)*h*.09f;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+w*(-.10f+side*.57f),ey=y-h*.95f;
            float[] ear=HalloweenKeys.shape(ex,ey,w*.23f,h*.46f,new float[]{-.55f,.4f,-.65f,-.45f,-.1f,-.9f,.65f,-.5f,.45f,.4f});
            for(int i=1;i<ear.length;i+=2)ear[i]+=bounce;
            p.fillPoly(ear,0xFFB68AE1);p.strokePoly(ear,0xFF76558D,w*.035f);
            p.line(ex,ey-h*.17f+bounce,ex,ey+h*.10f+bounce,0xFFEBD6F6,w*.075f);
        }
        p.fillEllipse(x-w*.08f,y-h*.21f,w*.14f,h*.18f,0xFFFFB66A);
        p.fillEllipse(x-w*.12f,y-h*.25f,w*.045f,h*.04f,0xFFFFE6BA);
        // The bow rides with the tilted head, just below the chin in every portrait and pose.
        float bx=x-w*.05f,by=y+h*1.28f,bw=w*.36f,bh=h*.31f;
        for(int side=-1;side<=1;side+=2) {
            float flutter=(float)Math.sin(clock*4+side*.6f)*.10f;
            float[] wing=HalloweenKeys.shape(bx,by,bw*side,bh,
                    new float[]{.06f,-.23f,.92f,-.94f+flutter,1.08f,-.58f,1.04f,.65f+flutter,.83f,.86f,.08f,.23f});
            p.fillPoly(wing,0xFF704093);
            p.fillPoly(HalloweenKeys.shape(bx,by-bh*.10f,bw*side*.87f,bh*.72f,
                    new float[]{.12f,-.12f,1,-.84f+flutter,1.05f,.50f,.84f,.68f,.10f,.16f}),0xFFB889DF);
            p.line(bx+side*bw*.26f,by,bx+side*bw*.72f,by-bh*.38f,0xFFDDB8F1,w*.025f);
        }
        p.fillEllipse(bx,by,w*.11f,h*.24f,0xFFDB793F);
        p.fillEllipse(bx-w*.015f,by-h*.035f,w*.085f,h*.18f,0xFFFFBC70);
        p.fillEllipse(bx-w*.03f,by-h*.10f,w*.03f,h*.055f,0xFFFFE5B3);
    }
}
