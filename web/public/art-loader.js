'use strict';
(() => {
  if(typeof Worker==='undefined'||typeof OffscreenCanvas==='undefined')return;
  let worker,ready=false,failed=false,paused=false,timeout=0;const pending=new Map();
  function fallback(error){
    if(failed)return;failed=true;clearTimeout(timeout);worker?.terminate();worker=null;pending.clear();
    console.warn('Background artwork worker unavailable; using synchronous artwork.',error);
  }
  function start(){
    if(worker||paused||failed)return;
    let current;try{current=worker=new Worker('art-worker.js');}catch(e){fallback(e);return;}
    timeout=setTimeout(()=>fallback('startup timeout'),15000);
    current.onerror=e=>{if(worker===current)fallback(e.message);};
    current.onmessage=({data})=>{
      if(worker!==current){data.bitmap?.close();return;}
      if(data.error){fallback(data.error);return;}
      if(data.ready){ready=true;clearTimeout(timeout);for(const request of pending.values())current.postMessage(request);return;}
      pending.delete(data.key);data.bitmap.artGroup=data.group;data.bitmap.artId=data.art;ddd.cache.put(data.key,data.bitmap);
    };
  }
  function request(value){pending.set(value.key,value);start();if(ready)worker.postMessage(value);return true;}
  ddd.cache.requestArt=(group,art)=>{
    if(failed)return false;
    const key=group+art;if(ddd.cache.get(key)||pending.has(key))return true;
    return request({key,group,art});
  };
  ddd.cache.requestPalette=(group,art,key,matrix)=>{
    if(failed)return false;if(pending.has(key)||pending.size>=32)return true;
    return request({key,group,art,matrix});
  };
  ddd.artPause=value=>{
    if(paused===value)return;paused=value;
    if(value){clearTimeout(timeout);worker?.terminate();worker=null;ready=false;}
    else if(pending.size)start();
  };
  ddd.artState=()=>({ready,failed,pending:pending.size});
})();
