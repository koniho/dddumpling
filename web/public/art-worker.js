'use strict';
// Generate the same Java vector artwork off the input/render thread.
self.window=self;
self.document={createElement:()=>new OffscreenCanvas(1,1)};
self.ddd={context:new OffscreenCanvas(1,1).getContext('2d'),storage:{get:()=>null,put:()=>true},
  notice(){},audio:{effect(){},music(){},volumes(){},loop(){},speak(){},hush(){}},
  connect(api){
    self.onmessage=({data})=>{
      try{
        const canvas=api.art(data.group,data.art),bitmap=canvas.transferToImageBitmap();
        ddd.cache.remove(data.key);self.postMessage({key:data.key,bitmap},[bitmap]);
      }catch(e){self.postMessage({error:String(e)});}
    };
    self.postMessage({ready:true});
  }};
importScripts('paint.js','game.js');
main();
