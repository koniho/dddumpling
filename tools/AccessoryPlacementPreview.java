package com.dddumpling.game;

import java.io.File;
import java.util.ArrayList;

/** Reproducible attachment audit: all variants across each body renderer, plus every collectible. */
final class AccessoryPlacementPreview {
    static int[] types() {
        ArrayList<Integer> ids=new ArrayList<>();boolean[] shapes=new boolean[Collect.SHAPE_COUNT];
        for(int who=0;who<Collect.COUNT;who++) {
            if(Collect.bossKind(who)>=0 || who>=Collect.DUCK_FIRST && who<Collect.DUCK_FIRST+Collect.DUCK_COUNT)continue;
            if(!shapes[Collect.SHAPE[who]]){shapes[Collect.SHAPE[who]]=true;ids.add(who);}
        }
        for(int boss=0;boss<Collect.BOSS_COUNT;boss++)ids.add(Collect.bossPrize(boss));
        ids.add(Collect.DUCK_FIRST);
        int[] out=new int[ids.size()];for(int i=0;i<out.length;i++)out[i]=ids.get(i);return out;
    }
    public static void main(String[] args) throws Exception {
        File dir=new File(args.length>0?args[0]:"out/accessory-fit");dir.mkdirs();
        int[] types=types();
        designs(dir);
        for(int item=0;item<Accessories.COUNT;item++)sheet(dir,String.format("item-%02d",item),Accessories.NAME[item],types,item,0,false,1.2f);
        for(int boss=0;boss<Collect.BOSS_COUNT;boss++) {
            int who=Collect.bossPrize(boss);int[] repeated=new int[Accessories.COUNT];java.util.Arrays.fill(repeated,who);
            sheet(dir,"halloween-"+who,Collect.NAME[who]+" / HALLOWEEN",repeated,-2,1,false,1.2f);
        }
        for(int page=0;page*20<Collect.COUNT;page++) {
            int[] ids=new int[Math.min(20,Collect.COUNT-page*20)];for(int i=0;i<ids.length;i++)ids[i]=page*20+i;
            sheet(dir,"outfits-"+page,"OUTFIT FIT / "+(page+1),ids,-1,0,false,1.2f);
        }
        for(float age:new float[]{0f,.6f,1.8f})sheet(dir,"motion-"+age,"MOVING ATTACHMENTS",types,-1,0,true,age);
        System.out.println("Placement sheets: "+types.length+" body types, "+Accessories.COUNT+" styles, "+Collect.COUNT+" collectibles; "+dir);
    }
    private static void designs(File dir) throws Exception {
        int[][] pages={{3,4,5,22},{18,19,20,21},{23,24,25,26}};
        String[] titles={"SHAPED EYEWEAR","HATS WITH PERSONALITY","HAPPY FEET"};
        for(int page=0;page<pages.length;page++) {
            RasterPainter p=new RasterPainter(900,1500,1);p.clear(0xFF171A29);
            p.text(titles[page],450,65,26,0xFFFFDF9A,Painter.CENTER,true);
            for(int row=0;row<4;row++) {
                final int item=pages[page][row];float y=220+row*330;
                Painter detailed=new OpacityPainter(p,1){public float accessorySize(float r){return .20f;}};
                Painter small=new OpacityPainter(p,1){public float accessorySize(float r){return .04f;}};
                AccessoryArt.thumbnail(detailed,item,105,y-125,355,y+125,1.2f);
                AccessoryArt.thumbnail(small,item,412,y-38,488,y+38,1.2f);
                Painter worn=new OpacityPainter(detailed,1){public int accessory(int who,int slot){return Accessories.slot(item)==slot?item:-1;}};
                Trinket.drawReacting(worn,0,690,y+30,87,1.2f,1,-1,0);
                p.text(Accessories.NAME[item],450,y+145,25,Accessories.COLOR[item],Painter.CENTER,true);
            }
            Png.write(new File(dir,"designs-"+page+".png"),p.resolve(),900,1500);
        }
    }
    private static void sheet(File dir,String file,String title,int[] ids,int item,int skin,boolean reaction,float age) throws Exception {
        int w=1080,h=1800,cols=4,rows=(ids.length+cols-1)/cols;float cw=w/4f,ch=1660f/Math.max(6,rows);
        RasterPainter canvas=new RasterPainter(w,h,1);canvas.clear(0xFF171A29);
        canvas.text(title,w*.5f,58,28,0xFFFFDF9A,Painter.CENTER,true);
        for(int i=0;i<ids.length;i++) {
            final int selected=item==-2?i:item;int who=ids[i];
            float x=cw*(i%cols+.5f),y=115+ch*(i/cols+.46f),r=cw*(selected>=18 && selected<=21?.245f:.29f);
            Painter outfit=new OpacityPainter(Skins.painter(canvas,skin),1) {
                public float accessorySize(float radius){return radius/cw;}
                public int accessory(int id,int slot){return selected==-1?Accessories.item(slot,slot==Accessories.HAT?2:0):Accessories.slot(selected)==slot?selected:-1;}
            };
            canvas.save();canvas.clipRect(cw*(i%cols),100+ch*(i/cols),cw*(i%cols+1),100+ch*(i/cols+1));
            Trinket.drawReacting(outfit,who,x,y,r,age,1,reaction?4:-1,reaction?(float)Math.sin(age*5)*.3f:0);
            canvas.restore();
            String label=item==-2?Accessories.NAME[i]:Collect.NAME[who];
            canvas.text(label,x,115+ch*(i/cols+.93f),Math.min(17,230f/(label.length()*.8f)),0xFFEAE5FF,Painter.CENTER,true);
        }
        Png.write(new File(dir,file+".png"),canvas.resolve(),w,h);
    }
}
