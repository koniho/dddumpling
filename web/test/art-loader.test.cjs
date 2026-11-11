const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function env(){
 const sent=[],cache=new Map();let worker,timeout;
 const win={Worker:class{constructor(){worker=this;}postMessage(m){sent.push(m);}terminate(){this.stopped=true;}},OffscreenCanvas:class{},ddd:{cache:{get:k=>cache.get(k),put:(k,v)=>cache.set(k,v)}},setTimeout:f=>{timeout=f;},clearTimeout(){},console:{warn(){}}};
 vm.runInNewContext(fs.readFileSync('web/public/art-loader.js','utf8'),win);return {win,get worker(){return worker;},sent,cache,get timeout(){return timeout;}};
}
test('art requests queue until ready, deduplicate and accept transferred artwork',()=>{
 const e=env(),{win,sent,cache}=e;assert.equal(e.worker,undefined);assert.equal(win.ddd.cache.requestArt('crystal',4),true);const worker=e.worker;win.ddd.cache.requestArt('crystal',4);assert.equal(sent.length,0);
 worker.onmessage({data:{ready:true}});assert.equal(sent.length,1);
 const bitmap={};worker.onmessage({data:{key:'crystal4',bitmap}});assert.equal(cache.get('crystal4'),bitmap);
 win.ddd.cache.requestArt('crystal',4);assert.equal(sent.length,1);assert.equal(win.ddd.artState().pending,0);
});
test('worker failure/timeout permits synchronous rendering instead of permanently missing art',()=>{
 for(const mode of ['error','timeout']){const e=env(),{win}=e;win.ddd.cache.requestArt('vista',22);const {worker,timeout}=e;if(mode==='error')worker.onerror({message:'unsupported'});else timeout();assert.equal(win.ddd.cache.requestArt('vista',22),false);assert.equal(worker.stopped,true);assert.equal(win.ddd.artState().pending,0);}
});
test('rapid palette changes cannot create an unbounded worker queue',()=>{
 const e=env(),{win,sent}=e;
 for(let i=0;i<100;i++)win.ddd.cache.requestPalette('vista',22,'tint'+i,[i]);
 e.worker.onmessage({data:{ready:true}});
 assert.equal(sent.length,32);assert.equal(win.ddd.artState().pending,32);
});

test('SDK pause terminates worker work and resume restores queued artwork',()=>{
 const e=env();e.win.ddd.cache.requestArt('crystal',4);const first=e.worker;
 first.onmessage({data:{ready:true}});e.win.ddd.artPause(true);assert.equal(first.stopped,true);
 let closed=false;first.onmessage({data:{key:'crystal4',bitmap:{close(){closed=true;}}}});assert.equal(closed,true);assert.equal(e.cache.size,0);
 e.win.ddd.cache.requestArt('crystal',5);assert.equal(e.worker,first);
 e.win.ddd.artPause(false);assert.notEqual(e.worker,first);e.worker.onmessage({data:{ready:true}});
 assert.deepEqual(e.sent.map(r=>r.key),['crystal4','crystal4','crystal5']);
});
