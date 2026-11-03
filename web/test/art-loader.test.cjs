const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function env(){
 const sent=[],cache=new Map();let worker,timeout;
 const win={Worker:class{constructor(){worker=this;}postMessage(m){sent.push(m);}terminate(){this.stopped=true;}},OffscreenCanvas:class{},ddd:{cache:{get:k=>cache.get(k),put:(k,v)=>cache.set(k,v)}},setTimeout:f=>{timeout=f;},clearTimeout(){},console:{warn(){}}};
 vm.runInNewContext(fs.readFileSync('web/public/art-loader.js','utf8'),win);return {win,worker,sent,cache,timeout};
}
test('art requests queue until ready, deduplicate and accept transferred artwork',()=>{
 const {win,worker,sent,cache}=env();assert.equal(win.ddd.cache.requestArt('crystal',4),true);win.ddd.cache.requestArt('crystal',4);assert.equal(sent.length,0);
 worker.onmessage({data:{ready:true}});assert.equal(sent.length,1);
 const bitmap={};worker.onmessage({data:{key:'crystal4',bitmap}});assert.equal(cache.get('crystal4'),bitmap);
 win.ddd.cache.requestArt('crystal',4);assert.equal(sent.length,1);assert.equal(win.ddd.artState().pending,0);
});
test('worker failure/timeout permits synchronous rendering instead of permanently missing art',()=>{
 for(const mode of ['error','timeout']){const {win,worker,timeout}=env();win.ddd.cache.requestArt('vista',22);if(mode==='error')worker.onerror({message:'unsupported'});else timeout();assert.equal(win.ddd.cache.requestArt('vista',22),false);assert.equal(worker.stopped,true);assert.equal(win.ddd.artState().pending,0);}
});
