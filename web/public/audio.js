'use strict';
(() => {
  let ctx,master,musicBus,effectsBus,enabled=false,hidden=false,allowed=true,epoch=0,musicVolume=.55,effectsVolume=1;
  let desired='',desiredGain=1,musicNode,musicGeneration=0;
  const loads=new Map(),buffers=new Map(),voices=new Set(),loops=new Map(),requests=new Set();
  const blocked=()=>hidden||!allowed;
  async function buffer(name){
    if(buffers.has(name))return buffers.get(name);
    if(hidden)return null;
    if(!loads.has(name)){
      const controller=new AbortController();requests.add(controller);
      const pending=(async()=>{
        const r=await fetch(`audio/${name}.wav`,{signal:controller.signal});if(!r.ok)throw Error(`Audio ${name}: ${r.status}`);
        const bytes=await r.arrayBuffer();if(!ctx||blocked())return bytes;
        const b=await ctx.decodeAudioData(bytes);buffers.set(name,b);return b;
      })().catch(e=>{if(loads.get(name)===pending)loads.delete(name);if(e.name!=='AbortError')console.warn(e.message);return null;})
        .finally(()=>requests.delete(controller));
      loads.set(name,pending);
    }
    let b=await loads.get(name);
    if(b instanceof ArrayBuffer&&ctx&&!blocked()){b=await ctx.decodeAudioData(b.slice(0));buffers.set(name,b);loads.set(name,Promise.resolve(b));}return b;
  }
  function stop(node){if(!node)return;try{node.stop();}catch{}node.disconnect();voices.delete(node);}
  function node(b,bus,gain,rate=1,loop=false){const s=ctx.createBufferSource(),g=ctx.createGain();s.buffer=b;s.playbackRate.value=rate;s.loop=loop;g.gain.value=gain;s.connect(g);g.connect(bus);s.gainNode=g;s.onended=()=>{s.disconnect();g.disconnect();voices.delete(s);};s.start();return s;}
  async function unlock(){
    if(!ctx){const Audio=window.AudioContext||window.webkitAudioContext;if(!Audio)return;ctx=new Audio();master=ctx.createGain();master.connect(ctx.destination);musicBus=ctx.createGain();musicBus.connect(master);effectsBus=ctx.createGain();effectsBus.connect(master);volumes(musicVolume,effectsVolume);}
    if(blocked())return;enabled=true;master.gain.value=1;try{await ctx.resume();}catch(e){console.warn(e);}syncMusic();
  }
  async function syncMusic(){if(!enabled||blocked()||!desired||musicNode)return;const generation=++musicGeneration,name=desired,b=await buffer(name);if(!b||generation!==musicGeneration||blocked()||!enabled)return;musicNode=node(b,musicBus,desiredGain,1,name!=='versus');const source=musicNode,cleanup=source.onended;source.onended=()=>{cleanup();if(generation===musicGeneration)musicNode=null;};}
  function music(name,gain){if(name===desired)return;desired=name;desiredGain=gain;musicGeneration++;stop(musicNode);musicNode=null;syncMusic();}
  function volumes(m,s){musicVolume=Math.max(0,Math.min(1,m));effectsVolume=Math.max(0,Math.min(1,s));if(ctx){musicBus.gain.value=musicVolume;effectsBus.gain.value=effectsVolume;}if(effectsVolume===0)hush();}
  async function effect(name,rate,gain){if(!enabled||blocked()||!effectsVolume)return;const time=performance.now(),generation=epoch,b=await buffer(name);if(!b||blocked()||generation!==epoch||performance.now()-time>180)return;if(voices.size>=24)stop(voices.values().next().value);const s=node(b,effectsBus,gain,rate);voices.add(s);}
  async function loop(name,rate,gain){let state=loops.get(name);if(!state){state={generation:0,node:null};loops.set(name,state);}state.rate=rate;state.gain=gain;
    if(gain<=0||blocked()){state.generation++;stop(state.node);state.node=null;return;}
    if(state.node){state.node.playbackRate.value=rate;state.node.gainNode.gain.value=gain;return;}if(!enabled||blocked()||state.loading)return;
    const generation=++state.generation;state.loading=true;const b=await buffer(name);state.loading=false;
    if(!b||blocked()||generation!==state.generation||state.gain<=0)return;state.node=node(b,effectsBus,state.gain,state.rate,true);
  }
  function hush(){if(window.speechSynthesis)speechSynthesis.cancel();}
  function silence(){
    epoch++;hush();musicGeneration++;stop(musicNode);musicNode=null;
    for(const s of voices)stop(s);voices.clear();
    for(const s of loops.values()){s.generation++;stop(s.node);s.node=null;}
    if(master)master.gain.value=0;if(ctx)ctx.suspend().catch(()=>{});
  }
  function suspend(value){
    if(hidden===value)return;hidden=value;
    if(value){for(const request of requests)request.abort();requests.clear();loads.clear();silence();}
    else if(enabled&&!blocked())unlock();
  }
  function setAllowed(value){if(allowed===value)return;allowed=value;if(!value)silence();else if(!hidden)unlock();}
  function speak(text,pitch,rate,gain){if(!enabled||blocked()||effectsVolume===0||!window.speechSynthesis)return;hush();const s=new SpeechSynthesisUtterance(text);s.lang='en-US';s.pitch=pitch;s.rate=rate;s.volume=effectsVolume*gain;speechSynthesis.speak(s);}
  ddd.audio={setAllowed,unlock,effect,loop,music,volumes,hush,suspend,speak,
    preload:()=>Promise.all(Array.from({length:21},(_,i)=>buffer('sfx-'+i))),
    get state(){return {allowed,hidden,context:ctx?.state||'locked',voices:voices.size,decoded:buffers.size,music:desired};}};
})();
