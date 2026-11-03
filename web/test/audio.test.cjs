const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function env(){
 const pending=[],nodes=[],gains=[],spoken=[];let audio;
 class Audio {
  constructor(){audio=this;this.state='suspended';}createGain(){const g={gain:{value:1},connect(){},disconnect(){}};gains.push(g);return g;}
  createBufferSource(){const s={playbackRate:{value:1},connect(){},disconnect(){},start(){s.started=true;},stop(){s.stopped=true;}};nodes.push(s);return s;}
  resume(){this.state='running';return Promise.resolve();}suspend(){this.state='suspended';return Promise.resolve();}decodeAudioData(){return Promise.resolve({decoded:true});}
 }
 const win={ddd:{},AudioContext:Audio,ArrayBuffer,AbortController,console,performance:{now:()=>0},SpeechSynthesisUtterance:class {constructor(text){this.text=text;}},speechSynthesis:{cancel(){spoken.length=0;},speak:s=>spoken.push(s)},fetch:(url,{signal})=>new Promise((resolve,reject)=>{pending.push({url,resolve:()=>resolve({ok:true,arrayBuffer:async()=>new ArrayBuffer(1)})});signal.addEventListener('abort',()=>{const e=Error('aborted');e.name='AbortError';reject(e);});})};win.window=win;
 vm.runInNewContext(fs.readFileSync('web/public/audio.js','utf8'),win);
 return {api:win.ddd.audio,pending,nodes,gains,spoken,get context(){return audio;}};
}
test('YouTube mute wins over volume controls, gesture unlock, loops and speech',async()=>{
 const e=env();await e.api.unlock();e.api.setAllowed(false);e.api.volumes(1,1);await e.api.unlock();
 await e.api.effect('sfx-1',1,1);await e.api.loop('loop',1,1);e.api.speak('hello',1,1,1);
 assert.equal(e.pending.length,0);assert.equal(e.nodes.length,0);assert.equal(e.spoken.length,0);assert.equal(e.gains[0].gain.value,0);
 e.api.setAllowed(true);await tick();assert.equal(e.context.state,'running');
});
test('a pending effect cannot play after mute then unmute',async()=>{
 const e=env();await e.api.unlock();const effect=e.api.effect('sfx-1',1,1);e.api.setAllowed(false);e.api.setAllowed(true);
 e.pending[0].resolve();await effect;assert.equal(e.nodes.length,0);
});
test('pause aborts audio fetches and remains silent when YouTube unmutes',async()=>{
 const e=env();await e.api.unlock();const effect=e.api.effect('sfx-1',1,1);e.api.suspend(true);await effect;
 e.api.setAllowed(false);e.api.setAllowed(true);assert.equal(e.nodes.length,0);assert.equal(e.context.state,'suspended');
 e.api.suspend(false);await tick();assert.equal(e.context.state,'running');
});
test('mute immediately silences active sound, looping music and narration',async()=>{
 const e=env();await e.api.unlock();e.api.music('title',1);e.pending[0].resolve();await tick();
 const effect=e.api.effect('sfx-1',1,1);e.pending[1].resolve();await effect;e.api.speak('hello',1,1,1);
 assert.equal(e.nodes.length,2);e.api.setAllowed(false);
 assert.ok(e.nodes.every(n=>n.stopped));assert.equal(e.spoken.length,0);assert.equal(e.gains[0].gain.value,0);
});
