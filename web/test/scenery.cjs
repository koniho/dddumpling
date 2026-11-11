// Optional local Skia smoke test. This is NOT a browser or a device FPS benchmark.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const {createCanvas,GlobalFonts,Path2D}=require('../../build/web-test-deps/node_modules/@napi-rs/canvas');
GlobalFonts.registerFromPath('assets/fonts/Bungee-Regular.ttf','Bungee');
const canvas=createCanvas(390,844),storage=new Map(),sounds=[];
const sandbox={Path2D,console,performance,TextDecoder,TextEncoder,Date,Math,WebAssembly,setTimeout,clearTimeout,document:{createElement:()=>createCanvas(1,1)},navigator:{},window:{open(){}},crypto:require('node:crypto').webcrypto};sandbox.window=sandbox;sandbox.self=sandbox;
sandbox.ddd={context:canvas.getContext('2d'),storage:{get:k=>storage.get(k)??null,put:(k,v)=>{storage.set(k,v);return true;}},notice:console.log,audio:{effect:(...a)=>sounds.push(a),music(){},volumes(){},loop(){},speak(){},hush(){}},connect:api=>sandbox.api=api};
vm.createContext(sandbox);
for(const f of ['web/public/paint.js','build/web/game.js'])vm.runInContext(fs.readFileSync(f,'utf8'),sandbox,{filename:f});
sandbox.main();const api=sandbox.api;assert.ok(api);api.resize(390,844);
const out='build/web-scenery-review';fs.mkdirSync(out,{recursive:true});const timings=[];
for(const scene of ['stage:21','stage:11']){
 api.scene(scene);for(let i=0;i<120;i++)api.frame(1/60);
 let polys=0;const poly=sandbox.ddd.paint.poly;sandbox.ddd.paint.poly=(...args)=>{polys++;return poly(...args);};
 const start=performance.now();for(let i=0;i<120;i++)api.frame(1/60);
 sandbox.ddd.paint.poly=poly;
 if(scene==='stage:21'){
  for(let art=0;art<4;art++)assert.ok(sandbox.ddd.cache.get('grass'+art),'missing cached reed patch');
  assert.ok(polys/120<800,'swamp fell back to thousands of vector grass blades');
 }
 fs.writeFileSync(`${out}/${scene.replace(':','-')}.png`,canvas.toBuffer('image/png'));
 timings.push({scene,cpuMeanMs:(performance.now()-start)/120,polysPerFrame:polys/120,cacheMiB:sandbox.ddd.cache.bytes/1048576});
}
console.log(JSON.stringify(timings,null,2));

// Pixel-level check: water tint must preserve translucent edges without a worker.
const source=createCanvas(2,1),sc=source.getContext('2d');sc.fillStyle='rgba(200,100,50,0.5)';sc.fillRect(0,0,1,1);
const target=createCanvas(2,1),tc=target.getContext('2d');let last=-1;
sandbox.ddd.cache.requestPalette=()=>{throw Error('Depth tint queued a worker palette');};
for(let i=0;i<=20;i++){
 tc.clearRect(0,0,2,1);sandbox.ddd.cache.draw(tc,source,0,0,2,1,255,0,i/20,0xff17445c);
 const pixel=tc.getImageData(0,0,2,1).data;
 assert.ok(Math.abs(pixel[3]-128)<=1,'shading changed edge opacity');assert.equal(pixel[7],0,'tint leaked outside artwork');
 assert.ok(pixel[0]>=last,'depth tint jumped backward');last=pixel[0];
}
console.log('Scenery caches and continuous depth shading passed. CPU timings are not browser FPS.');
