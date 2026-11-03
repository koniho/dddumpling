'use strict';
window.ddd=window.ddd||{};
(() => {
  const sdk=window.ytgame,limit=3*1024*1024;
  let values=Object.create(null),loaded=false,paused=false,audioAllowed=false,ready=false,bootError,pendingWarning;
  let revision=0,saved=0,inflight=null,finalRequested=false,timer=0,retry=1000,listener,waiters=[];
  const warn=message=>{if(paused)pendingWarning=message;else ddd.notice?.(message);try{sdk?.health?.logWarning();}catch{}};
  const changed=()=>listener?.();
  const cancelTimer=()=>{clearTimeout(timer);timer=0;};
  function schedule(delay=250){if(paused||timer||!loaded||!ready||saved===revision)return;timer=setTimeout(()=>{timer=0;flush();},delay);}
  async function flush(final=false){
    cancelTimer();if(final)finalRequested=true;
    if(!loaded||!ready||saved===revision||inflight||paused&&!final)return inflight;
    finalRequested=false;
    const at=revision,data=JSON.stringify({version:1,values});
    // Check both encodings conservatively; never replace a large save with an empty one.
    if(Math.max(data.length*2,new TextEncoder().encode(data).length)>=limit){warn('Progress is too large to sync. Your current session is still available.');return;}
    const operation=(async()=>{
      try{await sdk.game.saveData(data);saved=at;retry=1000;}
      catch{warn('Progress has not synced yet. Retrying when the game is active.');retry=Math.min(30000,retry*2);}
    })();
    inflight=operation;await operation;inflight=null;
    if(saved!==revision){if(finalRequested&&saved===at)flush(true);else schedule(saved===at?250:retry);}
  }
  ddd.storage={
    get(key){if(!loaded)throw Error('Progress is not loaded');return values[key]??null;},
    put(key,value){
      if(!loaded||typeof value!=='string')return false;
      // Developer playthrough diagnostics are not player progress and can grow indefinitely.
      if(key==='PlaythroughLog')return true;
      if(values[key]===value)return true;
      values[key]=value;revision++;schedule();return true;
    }
  };
  ddd.platform={playables:true,
    get paused(){return paused;},get audioAllowed(){return audioAllowed;},
    subscribe(fn){listener=fn;changed();},
    async init(){
      if(bootError)throw bootError;if(loaded)return;
      if(paused)await new Promise(resolve=>waiters.push(resolve));
      const raw=await sdk.game.loadData();
      let parsed={version:1,values:{}};
      if(raw)parsed=JSON.parse(raw);
      if(parsed?.version!==1||!parsed.values||typeof parsed.values!=='object'||Array.isArray(parsed.values)
          ||Object.values(parsed.values).some(v=>typeof v!=='string'))throw Error('Saved progress could not be read. It has not been overwritten.');
      values=Object.assign(Object.create(null),parsed.values);delete values.PlaythroughLog;revision=saved=0;loaded=true;
    },
    async running(){if(paused)await new Promise(resolve=>waiters.push(resolve));},
    gameReady(){if(loaded&&!ready&&!paused){sdk.game.gameReady();ready=true;schedule();}},
    flush,
    failed(){if(!ready){loaded=false;cancelTimer();}try{sdk?.health?.logError();}catch{}},
    get saveState(){return {loaded,pending:saved!==revision,saving:!!inflight};}
  };
  try {
    if(!sdk?.IN_PLAYABLES_ENV)throw Error('Open this build in the YouTube Playables test suite. Use the standalone build for local play.');
    sdk.system.onPause(()=>{paused=true;cancelTimer();changed();flush(true);});
    sdk.system.onResume(()=>{paused=false;changed();if(pendingWarning){warn(pendingWarning);pendingWarning=null;}for(const resolve of waiters.splice(0))resolve();schedule(0);});
    sdk.system.onAudioEnabledChange(value=>{audioAllowed=value;changed();});
    audioAllowed=sdk.system.isAudioEnabled();
    // The HTML loading screen is already present before this script runs.
    sdk.game.firstFrameReady();
  }catch(e){bootError=e;}
})();
