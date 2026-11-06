'use strict';
// Generate the same Java vector artwork off the input/render thread.
self.window=self;
self.document={createElement:()=>new OffscreenCanvas(1,1)};
self.ddd={context:new OffscreenCanvas(1,1).getContext('2d'),storage:{get:()=>null,put:()=>true},
  notice(){},audio:{effect(){},music(){},volumes(){},loop(){},speak(){},hush(){}},
  connect(api){
    self.onmessage=async ({data})=>{
      try{
        const base=api.art(data.group,data.art),canvas=data.matrix?ddd.cache.filtered(base,data.matrix):base;
        // Keep a bounded CPU-side source cache for later colour variants, avoiding GPU readback.
        const bitmap=await createImageBitmap(canvas);
        self.postMessage({key:data.key,group:data.group,art:data.art,bitmap},[bitmap]);
      }catch(e){self.postMessage({error:String(e)});}
    };
    self.postMessage({ready:true});
  }};
importScripts('paint.js','game.js');
main();
