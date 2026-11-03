const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function env(){
 class Path {constructor(){this.points=[];}moveTo(...p){this.points.push(p);}lineTo(...p){this.points.push(p);}closePath(){this.closed=true;}}
 const win={Path2D:Path,Float32Array};win.window=win;vm.runInNewContext(fs.readFileSync('web/public/paint.js','utf8'),win);return win;
}
test('cached paths reuse unchanged coordinates and notice mutable spring-body buffers',()=>{
 const {ddd}=env(),drawn=[],ctx={fill:p=>drawn.push(p),stroke:p=>drawn.push(p)},p=new Float32Array([0,0,1,0,0,1]);
 ddd.paint.poly(ctx,p,0xffffffff,0,true);ddd.paint.poly(ctx,p,0xff000000,2,true);assert.equal(drawn[0],drawn[1]);
 p[2]=4;ddd.paint.poly(ctx,p,0xffffffff,0,true);assert.notEqual(drawn[1],drawn[2]);assert.equal(drawn[2].points[1][0],4);
 ddd.paint.poly(ctx,p,0xffffffff,1,false);assert.equal(drawn[3].closed,undefined);
});
test('transparent paths do not submit raster work',()=>{
 const {ddd}=env();ddd.paint.poly({fill(){throw Error('invisible path drawn');}},new Float32Array([0,0,1,1]),0,0,true);
});
test('texture eviction releases transferred bitmap memory and keeps accounting bounded',()=>{
 const {ddd}=env();let closed=0;const texture=()=>({width:4096,height:2048,close(){closed++;}});
 ddd.cache.put('a',texture());ddd.cache.put('b',texture());ddd.cache.get('a');ddd.cache.put('c',texture());
 assert.equal(ddd.cache.get('b'),null);assert.equal(closed,1);assert.equal(ddd.cache.bytes,64*1024*1024);
 ddd.cache.remove('a');assert.equal(closed,2);assert.equal(ddd.cache.bytes,32*1024*1024);
});
test('worker-backed tint requests never read pixels on the input thread',()=>{
 const {ddd}=env(),image={artGroup:'vista',artId:22,width:256,height:512};let request;
 ddd.cache.requestPalette=(...args)=>{request=args;return true;};
 const matrix=Array(20).fill(0);matrix[0]=matrix[6]=matrix[12]=.5;matrix[18]=1;
 assert.equal(ddd.cache.filtered(image,matrix),image);assert.equal(request[0],'vista');assert.equal(request[1],22);
 assert.deepEqual(Array.from(request[3]),matrix);
});

test('water haze maps each segment to one unclipped parallelogram',()=>{
 const {ddd}=env(),transforms=[],stack=[];let draws=0;
 const ctx={globalAlpha:.8,save(){stack.push(this.globalAlpha);},restore(){this.globalAlpha=stack.pop();},transform(...v){transforms.push(v);},drawImage(){draws++;}};
 ddd.cache.band(ctx,{width:2,height:64},[10,30,50,40,90,25],12,128,0);
 assert.equal(draws,2);assert.deepEqual(transforms,[[20,5,0,24/64,10,18],[20,-7.5,0,24/64,50,28]]);
 assert.equal(ctx.globalAlpha,.8);assert.equal(stack.length,0);
});
