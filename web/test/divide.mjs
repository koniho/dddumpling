// Real RAF frames around actual pinch effects; never infer FPS from batched test.step().
import {createRequire} from 'node:module';
import {mkdir,writeFile} from 'node:fs/promises';
const require=createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const engine=process.env.WEB_ENGINE||'chromium',browser=await require('playwright')[engine].launch({headless:true});
const out='build/web-review';await mkdir(out,{recursive:true});
const errors=[],results=[];
const stats=rows=>{const a=rows.map(r=>r.interval).sort((a,b)=>a-b),cpu=rows.map(r=>r.cpu).sort((a,b)=>a-b);
 const pct=(v,p)=>v[Math.min(v.length-1,Math.floor(v.length*p))]||0;
 return {frames:rows.length,frameMs:{p50:pct(a,.5),p95:pct(a,.95),p99:pct(a,.99)},cpuMs:{p50:pct(cpu,.5),p95:pct(cpu,.95),p99:pct(cpu,.99)}};};
try {
 const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2,hasTouch:true,isMobile:true});
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.goto((process.env.WEB_ORIGIN||'http://127.0.0.1:8766')+'/?scene=profile:divide&debug=1');
 await page.waitForFunction(()=>ddd.ready);await page.evaluate(()=>ddd.test.stop());
 for(const scale of [1,2]) {
  await page.evaluate(s=>{ddd.test.renderScale(s);ddd.api.scene('profile:divide');ddd.api.advanceReplay(550);ddd.test.step(1);},scale);
  await page.waitForFunction(()=>!ddd.artState || ddd.artState().pending===0);
  await page.evaluate(()=>{
   const frame=ddd.api.frame;let previous=0,split=-100,phase=-1;
   ddd.divideSamples=[];
   ddd.api.frame=dt=>{
    const start=performance.now();frame(dt);const cpu=performance.now()-start;
    const state=ddd.api.status(),tick=Number(state.match(/tick=(\d+)/)[1]),next=Number(state.match(/phase=(\d+)/)[1]);
    if(next!==phase){if(phase>=0)split=tick;phase=next;}
    if(previous)ddd.divideSamples.push({tick,phase,cpu,interval:start-previous,sinceSplit:tick-split,state});
    previous=start;
   };
   ddd.restoreDivide=()=>{ddd.api.frame=frame;};ddd.test.resume();
  });
  await page.waitForFunction(()=>/tick=1[6-9]\d\d;/.test(ddd.api.status()),{},{timeout:60000});
  const {samples,summary,art}=await page.evaluate(()=>{ddd.test.stop();ddd.restoreDivide();return {samples:ddd.divideSamples,summary:ddd.stats(),art:ddd.artState?.()};});
  if(!samples.some(s=>/pieces=8(?:;|$)/.test(s.state)) || new Set(samples.map(s=>s.phase)).size<6)throw Error('Split replay missed late phases');
  if(art?.failed)throw Error('Artwork worker failed');
  const row={engine,scale,environment:'CI normal RAF, not device FPS',all:stats(samples),split:stats(samples.filter(s=>s.sinceSplit<=48)),settled:stats(samples.filter(s=>s.sinceSplit>48)),summary,samples};
  results.push(row);console.log(engine,'Divide',scale,JSON.stringify({split:row.split,settled:row.settled}));
  await page.locator('#game').screenshot({path:`${out}/${engine}-divide-splits-${scale}x.png`});
  await writeFile(`${out}/${engine}-divide-splits.json`,JSON.stringify(results,null,2));
 }
 if(errors.length)throw Error(errors.join('\n'));await context.close();
} finally {await browser.close();}
