'use strict';
(() => {
  if(typeof Worker==='undefined'||typeof OffscreenCanvas==='undefined')return;
  let worker,ready=false,failed=false;const pending=new Map();
  function fallback(error){
    if(failed)return;failed=true;clearTimeout(timeout);worker?.terminate();pending.clear();
    console.warn('Background artwork worker unavailable; using synchronous artwork.',error);
  }
  const timeout=setTimeout(()=>fallback('startup timeout'),15000);
  try{worker=new Worker('art-worker.js');}catch(e){fallback(e);return;}
  worker.onerror=e=>fallback(e.message);
  worker.onmessage=({data})=>{
    if(data.error){fallback(data.error);return;}
    if(data.ready){ready=true;clearTimeout(timeout);for(const request of pending.values())worker.postMessage(request);return;}
    pending.delete(data.key);data.bitmap.artGroup=data.group;data.bitmap.artId=data.art;ddd.cache.put(data.key,data.bitmap);
  };
  ddd.cache.requestArt=(group,art)=>{
    if(failed)return false;
    const key=group+art;if(ddd.cache.get(key)||pending.has(key))return true;
    const request={key,group,art};pending.set(key,request);if(ready)worker.postMessage(request);return true;
  };
  ddd.cache.requestPalette=(group,art,key,matrix)=>{
    if(failed)return false;
    if(pending.has(key)||pending.size>=32)return true;
    const request={key,group,art,matrix};pending.set(key,request);if(ready)worker.postMessage(request);return true;
  };
  ddd.artState=()=>({ready,failed,pending:pending.size});
})();
