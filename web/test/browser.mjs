import {createRequire} from 'node:module';
import {mkdir,writeFile} from 'node:fs/promises';
const require=createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const {chromium,webkit}=require('playwright');
const origin=process.env.WEB_ORIGIN||'http://127.0.0.1:8766';
const out='build/web-review';await mkdir(out,{recursive:true});
const results=[];
for(const [engine,type] of [['chromium',chromium],['webkit',webkit]]){
 const browser=await type.launch({headless:true});
 try{
  const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2,hasTouch:true});
  const page=await context.newPage(),errors=[];
  page.on('pageerror',e=>errors.push(e.stack||e.message));
  await page.goto(origin+'/?debug=1');await page.waitForFunction(()=>window.ddd?.ready||window.ddd?.error,{},{timeout:90000});
  if(await page.evaluate(()=>ddd.error))throw Error(await page.evaluate(()=>ddd.error));
  await page.waitForTimeout(500);
  await page.locator('#game').screenshot({path:`${out}/${engine}-title.png`});
  // Real DOM keyboard input follows the same router as the touch deck.
  await page.evaluate(()=>{ddd.api.scene('keyboard:6');ddd.test.stop();});
  await page.keyboard.press('s');
  if(!/keyboardPresses=1/.test(await page.evaluate(()=>ddd.api.status())))throw Error('Keyboard did not dispatch');
  await page.keyboard.down('d');await page.keyboard.down('d');await page.keyboard.up('d');
  if(!/keyboardPresses=2/.test(await page.evaluate(()=>ddd.api.status())))throw Error('Repeat was not suppressed');
  // Blur releases held keys and opens the existing pause flow.
  await page.evaluate(()=>window.dispatchEvent(new Event('blur')));
  if(!await page.evaluate(()=>ddd.api.paused()))throw Error('Blur did not pause');
  await page.evaluate(()=>window.dispatchEvent(new Event('focus')));
  await page.keyboard.press('Escape');
  // Durable native progress through the browser Store, then reload.
  await page.evaluate(()=>{ddd.api.scene('progress:12345:0:2');ddd.test.stop();});
  await page.reload();await page.waitForFunction(()=>window.ddd?.ready,{},{timeout:90000});
  if(!/best=12345/.test(await page.evaluate(()=>ddd.api.status())))throw Error('Best score did not survive reload');
  // Resize preserves state and does not stretch the portrait field.
  const before=await page.evaluate(()=>ddd.api.status().split(';').slice(0,2).join(';'));
  await page.setViewportSize({width:768,height:1024});
  const after=await page.evaluate(()=>ddd.api.status().split(';').slice(0,2).join(';'));
  if(before!==after)throw Error('Resize reset gameplay');
  await page.setViewportSize({width:390,height:844});
  for(const scene of ['play','stage:5','stage:10','stage:15','stage:20','stage:25','stage:30','survival','ninja','stars','steamer']){
    await page.evaluate(s=>{ddd.api.scene(s);ddd.test.stop();ddd.test.clearMetrics();},scene);
    // Fixed input/frame sequence for comparable CPU measurements; not physical-device FPS.
    for(let i=0;i<16;i++)await page.evaluate(()=>ddd.test.step(30));
    await page.locator('#game').screenshot({path:`${out}/${engine}-${scene.replace(':','-')}.png`});
    results.push({engine,scene,environment:'CI headless, scripted 60 Hz simulation (not device FPS)',...await page.evaluate(()=>ddd.stats())});
    if(await page.evaluate(()=>ddd.error))throw Error(await page.evaluate(()=>ddd.error));
  }
  // Exercise audio unlock; a browser may start suspended, but should accept the interaction.
  await page.evaluate(()=>ddd.api.scene('keyboard:6'));await page.locator('#game').click({position:{x:200,y:700}});
  await page.waitForTimeout(500);
  const audio=await page.evaluate(()=>ddd.audio.state);if(audio.context==='locked')throw Error('Audio never unlocked');
  if(errors.length)throw Error(errors.join('\n'));
  await context.close();
 }finally{await browser.close();}
}
await writeFile(`${out}/metrics.json`,JSON.stringify(results,null,2));
console.log(`Passed both engines; ${results.length} scripted scene samples. Metrics are CPU timings, not phone FPS.`);
