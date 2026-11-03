// Google's live Test Suite + actual SDK. No SDK interception or simulated messages.
import {createRequire} from 'node:module';
import {mkdir,writeFile} from 'node:fs/promises';
const require=createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const {chromium}=require('playwright'),out='build/official-review';await mkdir(out,{recursive:true});
const origin=process.env.PLAYABLES_ORIGIN||'https://localhost:8767/';
// Permit Google's public test page to access this runner's loopback server only.
const browser=await chromium.launch({headless:true,args:['--disable-features=LocalNetworkAccessChecks']});
const context=await browser.newContext({ignoreHTTPSErrors:true,viewport:{width:1440,height:1100}}),page=await context.newPage();
const errors=[],security=[],results={suite:'https://developers.google.com/youtube/gaming/playables/test_suite',origin,checks:[]};
let game;
page.on('pageerror',e=>errors.push(String(e)));
page.on('console',m=>{if(m.type()==='error')errors.push(m.text());});
await context.addInitScript(()=>{window.cspViolations=[];document.addEventListener('securitypolicyviolation',e=>cspViolations.push({directive:e.effectiveDirective,blocked:e.blockedURI}));});
async function frame(){
 const handle=await page.locator('#playable-iframe').elementHandle();return handle.contentFrame();
}
async function ready(){
 game=await frame();await game.waitForFunction(()=>window.ddd?.ready||window.ddd?.error,null,{timeout:60000});
 const error=await game.evaluate(()=>ddd.error);if(error)throw Error(error);
 if(await game.evaluate(()=>ddd.keyboardGuide?.open))await game.locator('.keyboard-ready').click();
}
const requirements=()=>page.locator('.requirement-section').evaluateAll(sections=>sections.map(s=>({category:s.querySelector('.requirement-header')?.textContent.trim(),entries:[...s.querySelectorAll('.requirement-entry')].map(e=>({name:e.querySelector('.requirement-text')?.textContent.trim(),state:e.querySelector('.material-icons')?.className}))})));
try {
 await page.goto(results.suite,{waitUntil:'domcontentloaded'});
 await page.locator('#url-input').waitFor({timeout:60000});await page.locator('#url-input').fill(origin);await ready();
 results.checks.push('Official SDK loaded and game became interactive');
 await game.evaluate(()=>ddd.storage.put('OfficialSuiteCheck','restore-me'));
 await game.waitForFunction(()=>!ddd.platform.saveState.pending);
 await page.waitForTimeout(1500);results.requirements=await requirements();
 const must=results.requirements.find(s=>s.category==='MUST');
 if(!must?.entries.length||must.entries.some(e=>!e.state?.split(' ').includes('pass')))throw Error('Official MUST checks not all passed: '+JSON.stringify(must));
 results.checks.push('All official automated MUST checks passed');
 await page.locator('#pause-button').click();await game.waitForFunction(()=>ddd.platform.paused);
 const frozen=await game.evaluate(()=>({frames:ddd.stats().frames,state:ddd.api.status()}));await page.waitForTimeout(350);
 const paused=await game.evaluate(()=>({frames:ddd.stats().frames,state:ddd.api.status(),audio:ddd.audio.state,art:ddd.artState?.()}));
 if(paused.frames!==frozen.frames||paused.state!==frozen.state||!paused.audio.hidden||paused.art?.ready)throw Error('Official pause failed');
 await page.locator('#pause-button').click();await game.waitForFunction(()=>!ddd.platform.paused);
 await page.locator('#audio-button').click();await game.waitForFunction(()=>!ddd.platform.audioAllowed);
 await game.evaluate(()=>{ddd.audio.volumes(1,1);ddd.audio.unlock();});
 if(await game.evaluate(()=>ddd.audio.state.allowed))throw Error('Game volume bypassed SDK mute');
 await page.locator('#audio-button').click();await game.waitForFunction(()=>ddd.platform.audioAllowed);
 results.checks.push('Official pause/resume and mute controls honored');
 await page.locator('#refresh-button').click();await ready();
 if(await game.evaluate(()=>ddd.storage.get('OfficialSuiteCheck'))!=='restore-me')throw Error('Official save did not restore');
 results.checks.push('Official save survived iframe restart');
 // Fail an SDK save, preserve it locally, then recover without losing the newer edit.
 await page.locator('#error-response-button').check();
 await game.evaluate(()=>ddd.storage.put('OfficialSuiteCheck','retry-me'));
 await game.waitForFunction(()=>!document.getElementById('notice').hidden);
 if(!await game.evaluate(()=>ddd.platform.saveState.pending))throw Error('Failed save was incorrectly acknowledged');
 await page.locator('#error-response-button').uncheck();await game.evaluate(()=>ddd.platform.flush());
 await game.waitForFunction(()=>!ddd.platform.saveState.pending);
 await page.locator('#refresh-button').click();await ready();
 if(await game.evaluate(()=>ddd.storage.get('OfficialSuiteCheck'))!=='retry-me')throw Error('Retried save did not restore');
 results.checks.push('Official save error retained progress and recovered');
 security.push(...await game.evaluate(()=>window.cspViolations));
 if(security.length)throw Error('Game CSP violations: '+JSON.stringify(security));
 results.checks.push('No game CSP violations under documented YouTube policy');
 results.status='passed';
} catch(e) {results.status='failed';results.failure=String(e);process.exitCode=1;}
finally {
 results.errors=errors;results.security=security;
 try{results.finalRequirements=await requirements();results.events=await page.locator('.log-content').innerText();await page.screenshot({path:`${out}/official-suite.png`,fullPage:true});if(game)await game.locator('#game').screenshot({path:`${out}/game.png`});}catch(e){results.captureError=String(e);}
 await writeFile(`${out}/results.json`,JSON.stringify(results,null,2));console.log(JSON.stringify(results,null,2));await browser.close();
}
