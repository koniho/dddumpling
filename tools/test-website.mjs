import {createRequire} from 'node:module';
import {mkdir} from 'node:fs/promises';
const require = createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const {chromium} = require('playwright');
const {default: AxeBuilder} = require('@axe-core/playwright');
const browser = await chromium.launch();
const issues=[];
await mkdir('build/site-preview', {recursive:true});
try {
  for (const width of [320,390,768,1440]) {
    const context = await browser.newContext({viewport:{width,height:width === 1440 ? 1600 : 1000},reducedMotion:'reduce'});
    const page = await context.newPage();
    const errors=[];
    page.on('pageerror', e=>errors.push(e.message));
    await page.goto('http://127.0.0.1:8765', {waitUntil:'networkidle'});
    await page.locator('footer').scrollIntoViewIfNeeded();
    await page.evaluate(async()=>Promise.all([...document.images].map(i=>i.decode())));
    await page.evaluate(()=>scrollTo(0,0));
    const geometry=await page.evaluate(()=>({viewport:innerWidth,content:document.documentElement.scrollWidth}));
    if(geometry.content>geometry.viewport) {
      await page.screenshot({path:`build/site-preview/overflow-${width}.png`,fullPage:true});
      const elements=await page.locator('body *').evaluateAll(nodes=>nodes.filter(n=>n.getBoundingClientRect().right>innerWidth+1).map(n=>({tag:n.tagName,css:n.className,right:n.getBoundingClientRect().right,width:n.getBoundingClientRect().width})));
      issues.push(`Horizontal overflow at ${width}: ${JSON.stringify({geometry,elements})}`);
    }
    const badImages=await page.locator('img').evaluateAll(imgs=>imgs.filter(i=>!i.complete||!i.naturalWidth).map(i=>i.src));
    if(badImages.length||errors.length) throw Error(JSON.stringify({badImages,errors}));
    const audit=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze();
    if(audit.violations.length) issues.push(`${width}px accessibility: ${JSON.stringify(audit.violations.map(v=>({id:v.id,nodes:v.nodes.map(n=>n.target)})))}`);
    for(const id of ['modes','friends','download']){
      const link=page.locator(`a[href="#${id}"]`).first();
      if(!await link.isVisible()) continue;
      await link.click();
      if(new URL(page.url()).hash!==`#${id}`) throw Error(`Broken navigation: ${id}`);
    }
    await page.evaluate(()=>scrollTo(0,0));
    await page.screenshot({path:`build/site-preview/site-${width}.png`,fullPage:true});
    if(width===390||width===1440){
      await page.locator('.hero').screenshot({path:`build/site-preview/hero-${width}.png`});
      await page.locator('.friends-section').screenshot({path:`build/site-preview/friends-${width}.png`});
    }
    console.log(`${width}px: browser review completed`);
    await context.close();
  }
  if(issues.length) throw Error(issues.join("\n"));
} finally {await browser.close();}
