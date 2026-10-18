package com.dddumpling.game;

/** Visual identity travels with the painter, never through global mutable game state. */
final class Skins {
    static final int DEFAULT=0, HALLOWEEN=1, COUNT=2;
    static final String[] NAMES={"DEFAULT","HALLOWEEN"};
    // Preserve familiar key positions: cream, red, orange, purple, green, then black.
    static final int[] HALLOWEEN_COLORS={0xFFF2F2FF,0xFFF26B78,0xFFFFA64F,0xFFBA8BE8,0xFF8DD48B,0xFF51495F};
    static final String[] HALLOWEEN_NAMES={"GHOST","RED BAT","PUMPKIN","WITCH","FRANKENSTEIN","BLACK CAT"};
    static int valid(int skin) { return skin>=0 && skin<COUNT?skin:DEFAULT; }
    static boolean halloween(Painter p) { return p.skin()==HALLOWEEN; }
    static int keyColor(Painter p,int glyph) { return halloween(p)?HALLOWEEN_COLORS[glyph]:Glyph.COLOR[glyph]; }
    static Painter painter(Painter p,int skin) { return new Theme(p,valid(skin)); }
    static String bossName(int skin,int boss) { return skin==HALLOWEEN && boss==Boss.SLIME?"SUPER GHOST":Boss.NAMES[boss]; }
    private static final class Theme extends OpacityPainter {
        final int selected;
        Theme(Painter p,int selected) {super(p,1);this.selected=selected;}
        public int skin() {return selected;}
        public void text(String s,float x,float y,float z,int col,int align,boolean bold) {
            if(selected==HALLOWEEN)s=s.replace("SUPER SLIME","SUPER GHOST").replace("Super Slime","Super Ghost");
            super.text(s,x,y,z,col,align,bold);
        }
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
    }
}
