import {createRequire} from 'node:module';
import {mkdir} from 'node:fs/promises';
const require = createRequire(`${process.env.SITE_TEST_DEPS}/package.json`);
const {chromium} = require('playwright');
const {default: AxeBuilder} = require('@axe-core/playwright');
const browser = await chromium.launch();
await mkdir('build/site-preview', {recursive:true});
try {
  for (const width of [320,390,768,1440]) {
    const page = await browser.newPage({viewport:{width,height:width === 1440 ? 1600 : 1000},reducedMotion:'reduce'});
    const errors=[];
    page.on('pageerror', e=>errors.push(e.message));
    await page.goto('http://127.0.0.1:8765', {waitUntil:'networkidle'});
    await page.locator('footer').scrollIntoViewIfNeeded();
    await page.evaluate(async()=>Promise.all([...document.images].map(i=>i.decode())));
    await page.evaluate(()=>scrollTo(0,0));
    const geometry=await page.evaluate(()=>({viewport:innerWidth,content:document.documentElement.scrollWidth}));
    if(geometry.content>geometry.viewport) throw Error(`Horizontal overflow at ${width}: ${JSON.stringify(geometry)}`);
    const badImages=await page.locator('img').evaluateAll(imgs=>imgs.filter(i=>!i.complete||!i.naturalWidth).map(i=>i.src));
    if(badImages.length||errors.length) throw Error(JSON.stringify({badImages,errors}));
    const audit=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze();
    if(audit.violations.length) throw Error(JSON.stringify(audit.violations.map(v=>({id:v.id,nodes:v.nodes.map(n=>n.target)}))));
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
    console.log(`${width}px: images, navigation, overflow and accessibility passed`);
    await page.close();
  }
} finally {await browser.close();}
