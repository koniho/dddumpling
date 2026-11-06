// Fixed-resolution normal RAF comparison; CPU submission time alone misses raster stalls.
import {createRequire} from 'node:module';
import {mkdir,writeFile} from 'node:fs/promises';
const require=createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const engine=process.env.WEB_ENGINE||'chromium',browser=await require('playwright')[engine].launch({headless:true});
const out='build/web-review';await mkdir(out,{recursive:true});
const rows=[],errors=[];
try {
 const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2,hasTouch:true,isMobile:true});
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.goto((process.env.WEB_ORIGIN||'http://127.0.0.1:8766')+'/?scene=profile:octopulse&debug=1');
 await page.waitForFunction(()=>ddd.ready);await page.evaluate(()=>{ddd.test.stop();ddd.test.renderScale(1);});
 for(const scale of [1,2])for(const part of ['direct','full','no-arms','no-cave','no-haze','no-lighting','no-kelp','full','direct']) {
  await page.evaluate(s=>ddd.test.renderScale(s),scale);
  const scene='profile:octopulse'+(part==='full'?'':'-'+part);
  await page.evaluate(s=>{ddd.api.scene(s);ddd.api.advanceReplay(600);ddd.test.step(1);},scene);
  await page.waitForFunction(()=>!ddd.artState || ddd.artState().pending===0);
  await page.evaluate(()=>ddd.test.resume());await page.waitForTimeout(2000);
  await page.evaluate(s=>{ddd.test.stop();ddd.api.scene(s);ddd.api.advanceReplay(600);ddd.test.clearMetrics();ddd.test.resume();},scene);
  await page.waitForTimeout(4000);
  const stats=await page.evaluate(()=>{ddd.test.stop();return ddd.stats();});
  if(stats.renderScale!==scale || stats.frames<10)throw Error('Invalid fixed-scale replay');
  if(await page.evaluate(()=>ddd.artState?.().failed))throw Error('Artwork worker failed');
  rows.push({engine,part,environment:`CI normal RAF at fixed ${scale}x, not device FPS`,...stats});
  await page.locator('#game').screenshot({path:`${out}/${engine}-octo-isolate-${scale}x-${part}.png`});
  await writeFile(`${out}/${engine}-octo-isolate.json`,JSON.stringify(rows,null,2));
  console.log(engine,part,JSON.stringify({frameMs:stats.frameMs,cpuMs:stats.cpuMs}));
 }
 if(errors.length)throw Error(errors.join('\n'));
 await context.close();
} finally {await browser.close();}
