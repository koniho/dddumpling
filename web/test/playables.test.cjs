const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};};
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function env({load=async()=>'',save=async()=>{},available=true}={}) {
 const calls=[],callbacks={},timers=new Map();let serial=0;
 const sdk={IN_PLAYABLES_ENV:available,game:{firstFrameReady:()=>calls.push('first'),gameReady:()=>calls.push('ready'),loadData:()=>{calls.push('load');return load();},saveData:data=>{calls.push(['save',JSON.parse(data)]);return save(data);}},system:{isAudioEnabled:()=>false,onPause:f=>callbacks.pause=f,onResume:f=>callbacks.resume=f,onAudioEnabledChange:f=>callbacks.audio=f},health:{logWarning:()=>calls.push('warning'),logError:()=>calls.push('error')}};
 const win={ytgame:sdk,TextEncoder,ddd:{notice:m=>calls.push(['notice',m])},setTimeout:f=>{timers.set(++serial,f);return serial;},clearTimeout:id=>timers.delete(id)};win.window=win;
 Object.defineProperty(win,'localStorage',{get(){throw Error('Playables must never use localStorage');}});
 vm.runInNewContext(fs.readFileSync('web/public/playables.js','utf8'),win);
 return {win,calls,callbacks,timers,platform:win.ddd.platform,store:win.ddd.storage,runTimers(){const queued=[...timers.values()];timers.clear();for(const f of queued)f();}};
}
test('cloud load precedes game initialization; readiness is once and ordered',async()=>{
 const loading=deferred(),e=env({load:()=>loading.promise});const boot=e.platform.init();
 assert.deepEqual(e.calls,['first','load']);assert.throws(()=>e.store.get('Best'));assert.equal(e.store.put('Best','0'),false);
 loading.resolve(JSON.stringify({version:1,values:{Best:'42',Collected:'8'}}));await boot;
 assert.equal(e.store.get('Best'),'42');assert.equal(e.store.get('Collected'),'8');assert.equal(e.calls.some(Array.isArray),false);
 e.platform.gameReady();e.platform.gameReady();assert.deepEqual(e.calls,['first','load','ready']);
});
test('failed or malformed cloud loads cannot be overwritten; retry can recover',async()=>{
 let fail=true;const e=env({load:async()=>{if(fail)throw Error('offline');return '';}});
 await assert.rejects(e.platform.init());assert.equal(e.store.put('Best','0'),false);await e.platform.flush();assert.equal(e.calls.some(Array.isArray),false);
 fail=false;await e.platform.init();e.platform.gameReady();assert.equal(e.store.put('Best','1'),true);await e.platform.flush();assert.equal(e.calls.at(-1)[1].values.Best,'1');
 for(const raw of ['broken','{"version":2,"values":{}}','{"version":1,"values":{"Best":4}}']){
  const bad=env({load:async()=>raw});await assert.rejects(bad.platform.init());assert.equal(bad.store.put('Best','0'),false);await bad.platform.flush();assert.equal(bad.calls.some(Array.isArray),false);
 }
});
test('writes coalesce and serialize while retaining edits made during a pending save',async()=>{
 const first=deferred();let count=0;const e=env({save:()=>++count===1?first.promise:Promise.resolve()});await e.platform.init();e.platform.gameReady();
 e.store.put('Best','10');e.store.put('Collected','31');e.store.put('PlaythroughLog','diagnostic data');assert.equal(e.timers.size,1);
 e.runTimers();e.store.put('Best','20');e.runTimers();assert.equal(count,1);
 first.resolve();await tick();e.runTimers();await tick();
 assert.equal(count,2);const saved=e.calls.filter(Array.isArray).at(-1)[1];assert.equal(saved.values.Best,'20');assert.equal(saved.values.Collected,'31');assert.equal(saved.values.PlaythroughLog,undefined);assert.equal(e.platform.saveState.pending,false);
});
test('failed saves remain dirty and retry; pause flushes once and resume schedules retries',async()=>{
 let failures=true;const e=env({save:async()=>{if(failures)throw Error('offline');}});await e.platform.init();e.platform.gameReady();e.store.put('Best','10');e.callbacks.pause();await tick();
 assert.equal(e.platform.saveState.pending,true);assert.equal(e.timers.size,0);assert.equal(e.calls.filter(Array.isArray).length,1);
 failures=false;e.callbacks.resume();e.runTimers();await tick();assert.equal(e.platform.saveState.pending,false);
 assert.equal(e.calls.filter(c=>Array.isArray(c)&&c[0]==='save').length,2);
});
test('pause while saving flushes the newer snapshot after the older save completes',async()=>{
 const first=deferred();let count=0;const e=env({save:()=>++count===1?first.promise:Promise.resolve()});await e.platform.init();e.platform.gameReady();e.store.put('Best','10');e.runTimers();
 e.store.put('Best','20');e.callbacks.pause();first.resolve();await tick();await tick();
 assert.equal(count,2);assert.equal(e.calls.filter(Array.isArray).at(-1)[1].values.Best,'20');assert.equal(e.timers.size,0);
});
test('SDK pause before load defers boot and readiness; audio events do not resume it',async()=>{
 const e=env();e.callbacks.pause();const boot=e.platform.init();await tick();e.platform.gameReady();
 assert.deepEqual(e.calls,['first']);e.callbacks.audio(true);assert.equal(e.platform.paused,true);assert.equal(e.platform.audioAllowed,true);
 e.callbacks.resume();await boot;e.platform.gameReady();assert.deepEqual(e.calls,['first','load','ready']);
});
test('missing Playables environment fails closed rather than silently saving locally',async()=>{
 const e=env({available:false});await assert.rejects(e.platform.init(),/test suite/);assert.deepEqual(e.calls,[]);
});
test('oversized data is retained in session but never sent to SDK',async()=>{
 const e=env();await e.platform.init();e.platform.gameReady();e.store.put('Huge','x'.repeat(1600000));await e.platform.flush();
 assert.equal(e.calls.filter(c=>Array.isArray(c)&&c[0]==='save').length,0);assert.equal(e.platform.saveState.pending,true);
});

test('construction failures before gameReady cannot save partially initialized defaults',async()=>{
 const e=env();await e.platform.init();e.store.put('replica','new');e.runTimers();await e.platform.flush();
 assert.equal(e.calls.filter(c=>Array.isArray(c)&&c[0]==='save').length,0);
 e.platform.failed();assert.equal(e.store.put('Best','0'),false);e.callbacks.pause();await tick();
 assert.equal(e.calls.filter(c=>Array.isArray(c)&&c[0]==='save').length,0);
});
