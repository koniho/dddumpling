'use strict';
(() => {
  let ctx,master,musicBus,effectsBus,enabled=false,hidden=false,musicVolume=.55,effectsVolume=1;
  let desired='',desiredGain=1,musicNode,musicGeneration=0;
  const loads=new Map(),buffers=new Map(),voices=new Set(),loops=new Map();
  async function buffer(name){
    if(buffers.has(name))return buffers.get(name);
    if(!loads.has(name))loads.set(name,(async()=>{
      const r=await fetch(`audio/${name}.wav`);if(!r.ok)throw Error(`Audio ${name}: ${r.status}`);
      const bytes=await r.arrayBuffer();if(!ctx)return bytes;
      const b=await ctx.decodeAudioData(bytes);buffers.set(name,b);return b;
    })().catch(e=>{loads.delete(name);console.warn(e.message);return null;}));
    let b=await loads.get(name);
    if(b instanceof ArrayBuffer&&ctx){b=await ctx.decodeAudioData(b.slice(0));buffers.set(name,b);loads.set(name,Promise.resolve(b));}return b;
  }
  function stop(node){if(!node)return;try{node.stop();}catch{}node.disconnect();voices.delete(node);}
  function node(b,bus,gain,rate=1,loop=false){const s=ctx.createBufferSource(),g=ctx.createGain();s.buffer=b;s.playbackRate.value=rate;s.loop=loop;g.gain.value=gain;s.connect(g);g.connect(bus);s.gainNode=g;s.onended=()=>{s.disconnect();g.disconnect();voices.delete(s);};s.start();return s;}
  async function unlock(){
    if(!ctx){const Audio=window.AudioContext||window.webkitAudioContext;if(!Audio)return;ctx=new Audio();master=ctx.createGain();master.connect(ctx.destination);musicBus=ctx.createGain();musicBus.connect(master);effectsBus=ctx.createGain();effectsBus.connect(master);volumes(musicVolume,effectsVolume);}
    if(hidden)return;enabled=true;try{await ctx.resume();}catch(e){console.warn(e);}syncMusic();
  }
  async function syncMusic(){if(!enabled||hidden||!desired||musicNode)return;const generation=++musicGeneration,name=desired,b=await buffer(name);if(!b||generation!==musicGeneration||hidden||!enabled)return;musicNode=node(b,musicBus,desiredGain,1,name!=='versus');const source=musicNode,cleanup=source.onended;source.onended=()=>{cleanup();if(generation===musicGeneration)musicNode=null;};}
  function music(name,gain){if(name===desired)return;desired=name;desiredGain=gain;musicGeneration++;stop(musicNode);musicNode=null;syncMusic();}
  function volumes(m,s){musicVolume=Math.max(0,Math.min(1,m));effectsVolume=Math.max(0,Math.min(1,s));if(ctx){musicBus.gain.value=musicVolume;effectsBus.gain.value=effectsVolume;}if(effectsVolume===0)hush();}
  async function effect(name,rate,gain){if(!enabled||hidden||!effectsVolume)return;const time=performance.now(),b=await buffer(name);if(!b||hidden||performance.now()-time>180)return;if(voices.size>=24)stop(voices.values().next().value);const s=node(b,effectsBus,gain,rate);voices.add(s);}
  async function loop(name,rate,gain){let state=loops.get(name);if(!state){state={generation:0,node:null};loops.set(name,state);}state.rate=rate;state.gain=gain;
    if(gain<=0||hidden){state.generation++;stop(state.node);state.node=null;return;}
    if(state.node){state.node.playbackRate.value=rate;state.node.gainNode.gain.value=gain;return;}if(!enabled||state.loading)return;
    const generation=++state.generation;state.loading=true;const b=await buffer(name);state.loading=false;
    if(!b||hidden||generation!==state.generation||state.gain<=0)return;state.node=node(b,effectsBus,state.gain,state.rate,true);
  }
  function hush(){if(window.speechSynthesis)speechSynthesis.cancel();}
  function suspend(value){if(hidden===value)return;hidden=value;hush();if(value){musicGeneration++;stop(musicNode);musicNode=null;for(const s of voices)stop(s);voices.clear();for(const s of loops.values()){s.generation++;stop(s.node);s.node=null;}if(ctx)ctx.suspend().catch(()=>{});}else if(enabled)unlock();}
  function speak(text,pitch,rate,gain){if(!enabled||hidden||effectsVolume===0||!window.speechSynthesis)return;hush();const s=new SpeechSynthesisUtterance(text);s.lang='en-US';s.pitch=pitch;s.rate=rate;s.volume=effectsVolume*gain;speechSynthesis.speak(s);}
  ddd.audio={unlock,effect,loop,music,volumes,hush,suspend,speak,
    preload:()=>Promise.all(Array.from({length:21},(_,i)=>buffer('sfx-'+i))),
    get state(){return {context:ctx?.state||'locked',voices:voices.size,decoded:buffers.size,music:desired};}};
})();
