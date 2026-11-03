// Contract fixture around the real production game. Official Test Suite remains a release gate.
import {createRequire} from 'node:module';
import {mkdir,writeFile} from 'node:fs/promises';
const require=createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const engine=process.env.WEB_ENGINE||'chromium',browser=await require('playwright')[engine].launch({headless:true});
const origin=process.env.WEB_ORIGIN||'http://127.0.0.1:8766',out='build/web-review';await mkdir(out,{recursive:true});
const fixture=`(()=>{
 const callbacks={},calls=[],saved=[];let resolve,reject;
 window.testSDK={calls,saved,finishLoad:value=>resolve(value),failLoad:()=>reject(Error('Unavailable')),
 pause:()=>callbacks.pause(),resume:()=>callbacks.resume(),mute:value=>callbacks.audio(!value)};
 window.ytgame={IN_PLAYABLES_ENV:true,game:{firstFrameReady:()=>calls.push('first'),gameReady:()=>calls.push('ready'),
 loadData:()=>{calls.push('load');return new Promise((a,b)=>{resolve=a;reject=b;});},saveData:async data=>saved.push(data)},
 system:{isAudioEnabled:()=>false,onPause:f=>callbacks.pause=f,onResume:f=>callbacks.resume=f,onAudioEnabledChange:f=>callbacks.audio=f},health:{logError(){},logWarning(){}}};
})();`;
const errors=[];
try {
 const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2,hasTouch:true,isMobile:true});
 await context.addInitScript(()=>{
  Object.defineProperty(window,'localStorage',{get(){throw Error('Unexpected localStorage access in Playables');}});
  window.sdkPageEvents=[];const add=document.addEventListener.bind(document);
  document.addEventListener=(name,...args)=>{window.sdkPageEvents.push(name);return add(name,...args);};
 });
 await context.route('https://www.youtube.com/game_api/v1',route=>route.fulfill({contentType:'application/javascript',body:fixture}));
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.goto(origin+'/playables/');await page.waitForFunction(()=>window.testSDK?.calls.includes('load'));
 if(await page.evaluate(()=>!!ddd.api))throw Error('Game initialized before cloud load');
 await page.evaluate(()=>testSDK.failLoad());await page.locator('#loading button').waitFor();
 if(await page.evaluate(()=>testSDK.saved.length||testSDK.calls.includes('ready')))throw Error('Failed load initialized/overwrote progress');
 await page.locator('#loading button').click();await page.waitForFunction(()=>testSDK.calls.filter(c=>c==='load').length===2);
 await page.evaluate(()=>testSDK.finishLoad(JSON.stringify({version:1,values:{Best:'123',Tutorials:'64'}})));
 await page.waitForFunction(()=>ddd.ready);
 const boot=await page.evaluate(()=>({state:ddd.api.status(),calls:testSDK.calls,events:sdkPageEvents,tools:!!document.getElementById('tools'),audio:ddd.audio.state,history:history.state}));
 if(!boot.state.includes('best=123')||boot.calls.filter(c=>c==='ready').length!==1||boot.calls.filter(c=>c==='first').length!==1)throw Error('Readiness/cloud restore failed: '+JSON.stringify(boot));
 if(boot.tools||boot.events.includes('visibilitychange')||boot.history?.dddWebNavigation||boot.audio.allowed)throw Error('Standalone host leaked into Playables');
 const before=await page.evaluate(()=>ddd.api.status());await page.evaluate(()=>ddd.api.scene('stage:30'));
 if(await page.evaluate(()=>ddd.api.status())!==before)throw Error('Production build accepted developer scene');
 await page.evaluate(()=>{ddd.storage.put('SyncTestMarker','222');ddd.storage.put('KeyboardGuideVersion','1');});
 await page.waitForFunction(()=>testSDK.saved.some(raw=>JSON.parse(raw).values.SyncTestMarker==='222'));
 await page.evaluate(()=>{testSDK.mute(false);testSDK.pause();});
 const frozen=await page.evaluate(()=>({frames:ddd.stats().frames,state:ddd.api.status()}));
 await page.mouse.click(100,300);await page.keyboard.press('s');await page.waitForTimeout(300);
 const paused=await page.evaluate(()=>({frames:ddd.stats().frames,state:ddd.api.status(),audio:ddd.audio.state,art:ddd.artState?.()}));
 if(paused.frames!==frozen.frames||paused.state!==frozen.state||!paused.audio.hidden||paused.art?.ready)throw Error('SDK pause did not freeze game/audio/art');
 await page.evaluate(()=>testSDK.mute(false));await page.waitForTimeout(100);
 if(!await page.evaluate(()=>ddd.audio.state.hidden))throw Error('Audio-enabled callback resumed paused game');
 await page.evaluate(()=>{testSDK.mute(true);testSDK.resume();});await page.waitForFunction(n=>ddd.stats().frames>n,frozen.frames);
 await page.evaluate(()=>{ddd.audio.volumes(1,1);ddd.audio.unlock();ddd.audio.speak('test',1,1,1);});
 if(await page.evaluate(()=>ddd.audio.state.allowed))throw Error('Game volume bypassed YouTube mute');
 await page.locator('#game').screenshot({path:`${out}/${engine}-playables.png`});
 const save=await page.evaluate(()=>testSDK.saved.at(-1));
 await page.reload();await page.waitForFunction(()=>window.testSDK?.calls.includes('load'));await page.evaluate(raw=>testSDK.finishLoad(raw),save);await page.waitForFunction(()=>ddd.ready);
 if(!await page.evaluate(()=>ddd.storage.get('SyncTestMarker')==='222'&&ddd.storage.get('KeyboardGuideVersion')==='1'&&ddd.api.status().includes('best=123')))throw Error('Cloud progress did not survive restart');
 if(errors.length)throw Error(errors.join('\n'));
 await writeFile(`${out}/${engine}-playables.json`,JSON.stringify({engine,boot,paused,reloaded:await page.evaluate(()=>ddd.api.status()),result:'SDK contract checks passed; not official certification'},null,2));
 console.log(engine+': Playables cloud/lifecycle/audio checks passed');await context.close();
}finally{await browser.close();}
