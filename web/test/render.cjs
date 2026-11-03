// Optional local Skia smoke test. This is NOT a browser or a device FPS benchmark.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const {createCanvas,GlobalFonts}=require('../../build/web-test-deps/node_modules/@napi-rs/canvas');
GlobalFonts.registerFromPath('assets/fonts/Bungee-Regular.ttf','Bungee');
const canvas=createCanvas(390,844),storage=new Map(),sounds=[];
const sandbox={console,performance,TextDecoder,TextEncoder,Date,Math,WebAssembly,setTimeout,clearTimeout,document:{createElement:()=>createCanvas(1,1)},navigator:{},window:{open(){}},crypto:require('node:crypto').webcrypto};sandbox.window=sandbox;sandbox.self=sandbox;
sandbox.ddd={context:canvas.getContext('2d'),storage:{get:k=>storage.get(k)??null,put:(k,v)=>{storage.set(k,v);return true;}},notice:console.log,audio:{effect:(...a)=>sounds.push(a),music(){},volumes(){},loop(){},speak(){},hush(){}},connect:api=>sandbox.api=api};
vm.createContext(sandbox);
for(const f of ['web/public/paint.js','build/web/game.js'])vm.runInContext(fs.readFileSync(f,'utf8'),sandbox,{filename:f});
sandbox.main();const api=sandbox.api;assert.ok(api);api.resize(390,844);
const out='build/web-local-review';fs.mkdirSync(out,{recursive:true});const timings=[];
for(const scene of ['title','keyboard:6','stage:5','stage:10','stage:15','stage:20','stage:25','stage:30','ninja']){
 api.scene(scene);const start=performance.now();for(let i=0;i<120;i++){const c=sandbox.ddd.context;c.save();api.frame(1/60);c.restore();}
 fs.writeFileSync(`${out}/${scene.replace(':','-')}.png`,canvas.toBuffer('image/png'));timings.push({scene,cpuMeanMs:(performance.now()-start)/120,status:api.status(),cacheMiB:sandbox.ddd.cache.bytes/1048576});
}
api.scene('keyboard:6');api.key(115,true,false);api.key(115,false,false);assert.match(api.status(),/keyboardPresses=1/);assert.ok(sounds.length);
api.scene('progress:12345:0:2');assert.ok(storage.get('progress'));assert.match(api.status(),/best=12345/);
api.scene('keyboard:6');api.hidden(true);assert.equal(api.paused(),true);api.hidden(false);
fs.writeFileSync(`${out}/metrics.json`,JSON.stringify({environment:'Node/Skia, local CPU smoke only, not browser FPS',samples:timings},null,2));
console.log('Compiled JavaScript gameplay, native audio events, saves, input and scene rendering passed.');
