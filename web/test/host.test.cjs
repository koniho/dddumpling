const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function environment(){
 const listeners={},elements={};
 const context={setTransform(){},save(){},restore(){}};
 const element=id=>elements[id]??=( {hidden:true,textContent:'',width:0,height:0,style:{},addEventListener(n,f){listeners[id+':'+n]=f;},getContext(){return context;},getBoundingClientRect(){return {left:0,top:0,width:390,height:844};},setPointerCapture(){},focus(){}} );
 const storage=new Map(),frames=new Map();let next=0;
 const win={ddd:{audio:{suspend(){},unlock(){},preload(){},state:{}},cache:{bytes:0,count:0}},addEventListener(n,f){listeners[n]=f;},devicePixelRatio:3};win.window=win;
 Object.assign(win,{document:{hidden:false,getElementById:element,querySelector:()=>element('main'),addEventListener(n,f){listeners[n]=f;},fonts:{load:async()=>{},ready:Promise.resolve()}},location:{search:''},URLSearchParams,performance:{now:()=>100},localStorage:{getItem:k=>storage.get(k)??null,setItem:(k,v)=>storage.set(k,v)},getComputedStyle:()=>({paddingTop:'0',paddingBottom:'0',paddingLeft:'0',paddingRight:'0'}),requestAnimationFrame:f=>{frames.set(++next,f);return next;},cancelAnimationFrame:id=>frames.delete(id),setTimeout(){},clearTimeout(){},setInterval(){},console});
 vm.runInNewContext(fs.readFileSync('web/public/host.js','utf8'),win);
 const calls=[],api={resize:(...a)=>calls.push(['resize',...a]),hidden:v=>calls.push(['hidden',v]),frame:d=>calls.push(['frame',d]),pointer:(...a)=>calls.push(['pointer',...a]),key:(...a)=>{calls.push(['key',...a]);return true;},paused:()=>false,status:()=>'',back:()=>calls.push(['back'])};win.ddd.connect(api);
 return {win,listeners,elements,calls,frames};
}
test('pixel ratio capped; CSS game coordinates remain independent of backing pixels',()=>{const {elements,calls}=environment();assert.equal(elements.game.width,780);assert.deepEqual(calls[0],['resize',390,844]);});
test('pointer-up delivered once; lost capture does not cancel a different active pointer',()=>{const {listeners,calls}=environment();const e=id=>({pointerId:id,pointerType:'touch',clientX:20,clientY:40,preventDefault(){}});listeners['game:pointerdown'](e(7));listeners['game:pointerdown'](e(90));listeners['game:pointerup'](e(7));listeners['game:lostpointercapture'](e(7));assert.equal(calls.filter(c=>c[0]==='pointer').length,3);listeners['game:pointermove'](e(90));assert.equal(calls.at(-1)[2],90);});
test('background cancels frame scheduling and tells game to release held input',()=>{const {win,listeners,calls,frames}=environment();win.document.hidden=true;listeners.visibilitychange();assert.deepEqual(calls.at(-1),['hidden',true]);assert.equal(frames.size,0);win.document.hidden=false;listeners.visibilitychange();assert.deepEqual(calls.at(-1),['hidden',false]);assert.equal(frames.size,1);});
test('system shortcuts are untouched; Escape not preventDefault; mapped keys route',()=>{const {listeners,calls}=environment();let prevented=0;const e={key:'s',ctrlKey:true,target:{closest:()=>null},preventDefault(){prevented++;}};listeners.keydown(e);assert.equal(calls.filter(c=>c[0]==='key').length,0);e.ctrlKey=false;listeners.keydown(e);assert.equal(prevented,1);e.key='Escape';listeners.keydown(e);assert.equal(prevented,1);assert.deepEqual(calls.at(-1),['back']);});
test('storage is namespaced and write failures are surfaced',()=>{const {win}=environment();assert.equal(win.ddd.storage.put('Best','42'),true);assert.equal(win.ddd.storage.get('Best'),'42');win.localStorage.setItem=()=>{throw Error('quota');};assert.equal(win.ddd.storage.put('Best','99'),false);assert.equal(win.ddd.storage.get('Best'),'42');});
test('slow rendering reduces pixels without relayout or dropping an ongoing touch',()=>{
 const {win,listeners,elements,calls,frames}=environment();
 const touch={pointerId:8,pointerType:'touch',clientX:20,clientY:40,preventDefault(){}};
 listeners['game:pointerdown'](touch);
 let now=1;for(let i=0;i<240;i++){const f=[...frames.values()].at(-1);frames.clear();f(now);now+=40;}
 assert.equal(elements.game.width,390);assert.equal(calls.filter(c=>c[0]==='resize').length,1);
 listeners['game:pointerup'](touch);assert.deepEqual(calls.at(-1),['pointer',1,8,20,40]);
 assert.equal(elements.game.style.width,'390px');
 // Recovery needs sustained headroom and never exceeds the original pixel cap.
 for(let i=0;i<6000;i++){const f=[...frames.values()].at(-1);frames.clear();f(now);now+=16;}
 assert.equal(elements.game.width,780);assert.equal(calls.filter(c=>c[0]==='resize').length,1);
});

test('fixed diagnostic scale isolates render cost without changing game coordinates',()=>{
 const {win,elements,calls,frames}=environment();win.ddd.test.renderScale(1.5);
 let now=1;for(let i=0;i<240;i++){const f=[...frames.values()].at(-1);frames.clear();f(now);now+=40;}
 assert.equal(elements.game.width,585);assert.equal(calls.filter(c=>c[0]==='resize').length,1);
 win.ddd.test.renderScale(null);
 for(let i=0;i<240;i++){const f=[...frames.values()].at(-1);frames.clear();f(now);now+=40;}
 assert.equal(elements.game.width,390);
});
