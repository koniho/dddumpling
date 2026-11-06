'use strict';
(() => {
  const canvas=document.getElementById('game'),loading=document.getElementById('loading'),params=new URLSearchParams(location.search);
  const platform=ddd.platform,playables=!!platform?.playables;
  ddd.context=canvas.getContext('2d',{alpha:false});
  let api,last=0,raf=0,hidden=playables?platform.paused:document.hidden,focused=true,wasPaused=false,pendingResize=false,noticeTimer,width=0,height=0,scale=1;
  let ceiling=1,qualityWait=0,fastFrames=0,fixedScale=false;const quality=[];
  let warm=0,warmTime=0,debugTime=0;
  const timings=[],renders=[],pointerIDs=new Set(),prefix='dddumpling.web.v1.';
  ddd.haptics=false;
  ddd.notice=text=>{const n=document.getElementById('notice');n.textContent=text;n.hidden=false;clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>{n.hidden=true;},5000);};
  ddd.storage=ddd.storage||{
    get(key){try{return localStorage.getItem(prefix+key);}catch{ddd.notice('Browser storage is unavailable. Progress cannot be saved.');return null;}},
    put(key,value){try{localStorage.setItem(prefix+key,value);return true;}catch{ddd.notice('Progress could not be saved. Check browser storage space and permissions.');return false;}}
  };
  function fail(error){
    cancelAnimationFrame(raf);ddd.audio.suspend(true);ddd.artPause?.(true);platform?.failed();
    loading.hidden=false;loading.textContent=playables?(api?'The game stopped. Please reload. ':'Could not start the game. Your cloud save has not been overwritten. ')+String(error?.message||error):'The browser preview hit a problem. Reload to try again.';
    console.error(error);ddd.error=String(error);
    if(!api){const retry=document.createElement('button');retry.textContent='Retry';retry.onclick=()=>{retry.disabled=true;ddd.boot();};loading.append(retry);}
  }
  function resize(){if(playables&&hidden){pendingResize=true;return;}pendingResize=false;const r=document.querySelector('main').getBoundingClientRect(),css=getComputedStyle(document.querySelector('main'));
    height=Math.max(1,r.height-parseFloat(css.paddingTop)-parseFloat(css.paddingBottom));
    width=Math.max(1,Math.min(r.width-parseFloat(css.paddingLeft)-parseFloat(css.paddingRight),height*.56));
    ceiling=Math.min(window.devicePixelRatio||1,2,1080/width);setScale(ceiling);quality.length=0;qualityWait=fastFrames=0;
    canvas.style.width=width+'px';canvas.style.height=height+'px';
    pointerIDs.clear();api.resize(width,height);
  }
  function setScale(next){scale=next;canvas.width=Math.round(width*scale);canvas.height=Math.round(height*scale);}
  // Change raster density only: never relayout the game or cancel an active tap/pinch.
  function tune(interval){
    if(fixedScale)return;
    if(api.paused()||interval<=0){quality.length=0;fastFrames=0;return;}
    if(qualityWait>0){qualityWait--;return;}
    quality.push(Math.min(120,Math.max(interval,renders.at(-1)||0)));if(quality.length<30)return;
    const cost=pct(quality,.75);quality.length=0;
    if(cost>23 && scale>Math.min(1,ceiling)){setScale(Math.max(Math.min(1,ceiling),scale-(cost>35?.5:.25)));qualityWait=30;fastFrames=0;}
    else if(cost<17.5){fastFrames+=30;if(fastFrames>=600 && scale<ceiling){setScale(Math.min(ceiling,scale+.125));qualityWait=60;fastFrames=0;}}
    else fastFrames=0;
  }
  function draw(dt){const start=performance.now(),c=ddd.context;c.setTransform(scale,0,0,scale,0,0);c.save();try{api.frame(dt);}finally{c.restore();}renders.push(performance.now()-start);if(renders.length>900)renders.shift();}
  function frame(now){
    if(hidden||!focused)return;
    if(ddd.keyboardGuide?.open){last=0;raf=requestAnimationFrame(frame);return;}
    try{
      const dt=last?Math.min(.1,(now-last)/1000):0,interval=last?now-last:0;last=now;
      if(interval){timings.push(interval);if(timings.length>900)timings.shift();}
      tune(interval);draw(dt);ddd.audio.suspend(api.paused());
      warmTime+=dt;if(warm<5&&warmTime>.2){api.warm(warm++);warmTime=0;}
      if(!playables&&params.has('debug')&&now-debugTime>1000){document.getElementById('metrics').textContent=JSON.stringify(ddd.stats(),null,1);debugTime=now;}
      raf=requestAnimationFrame(frame);
    }catch(e){fail(e);}
  }
  function showGame(){
    if(ddd.ready||hidden||!focused)return;
    draw(0);loading.hidden=true;ddd.ready=true;
    ddd.keyboardGuide?.init(api,canvas,playables||!params.has('debug')&&!params.has('scene'));
    platform?.gameReady();
  }
  function activity(){
    const stopped=hidden||!focused;
    cancelAnimationFrame(raf);last=0;quality.length=0;fastFrames=0;pointerIDs.clear();
    clearTimeout(noticeTimer);
    if(!stopped&&!document.getElementById('notice').hidden)noticeTimer=setTimeout(()=>{document.getElementById('notice').hidden=true;},5000);
    ddd.audio.suspend(stopped||!!api?.paused());ddd.artPause?.(stopped);
    if(!api)return;
    if(playables){
      if(stopped&&!apiHostPaused){wasPaused=api.paused();api.hidden(true);apiHostPaused=true;}
      else if(!stopped&&apiHostPaused){api.hidden(false);apiHostPaused=false;if(!wasPaused&&api.paused()&&!ddd.keyboardGuide?.open)api.back();}
    }else api.hidden(stopped);
    if(!stopped){if(pendingResize)resize();showGame();ddd.audio.suspend(api.paused());raf=requestAnimationFrame(frame);}
  }
  let apiHostPaused=false;
  if(playables)platform.subscribe(()=>{
    ddd.audio.setAllowed(platform.audioAllowed);
    if(hidden!==platform.paused){hidden=platform.paused;activity();}
    else if(!api){ddd.audio.suspend(hidden);ddd.artPause?.(hidden);}
  });
  function event(e,action){if(!api||hidden||!focused||e.pointerType==='mouse'&&e.button>0&&action===0)return;
    if(action===0){canvas.focus({preventScroll:true});ddd.audio.unlock();pointerIDs.add(e.pointerId);canvas.setPointerCapture(e.pointerId);}
    if(!pointerIDs.has(e.pointerId))return;
    const r=canvas.getBoundingClientRect(),x=(e.clientX-r.left)*width/r.width,y=(e.clientY-r.top)*height/r.height;
    if(e.pointerType==='mouse'&&api.mouse)api.mouse(action,x,y);else api.pointer(action,e.pointerId,x,y);if(action===1)pointerIDs.delete(e.pointerId);if(action===3)pointerIDs.clear();e.preventDefault();
  }
  ddd.connect=game=>{
    api=game;ddd.api=api;resize();
    for(const [name,action] of [['pointerdown',0],['pointermove',2],['pointerup',1],['pointercancel',3]])canvas.addEventListener(name,e=>event(e,action));
    canvas.addEventListener('lostpointercapture',e=>{if(pointerIDs.has(e.pointerId))event(e,3);});
    canvas.addEventListener('contextmenu',e=>e.preventDefault());
    window.addEventListener('keydown',e=>{if(hidden||!focused||e.dddGuideHandled)return;if(e.ctrlKey||e.metaKey||e.altKey||e.target.closest('select,input,button'))return;
      if(e.key==='Escape'){api.back();ddd.audio.suspend(api.paused());return;}
      if(e.key.length===1){ddd.audio.unlock();if(api.key(e.key.charCodeAt(0),true,e.repeat))e.preventDefault();}});
    window.addEventListener('keyup',e=>{if(hidden||!focused)return;if(e.key.length===1)api.key(e.key.charCodeAt(0),false,false);});
    if(!playables){
      window.addEventListener('blur',()=>{focused=false;activity();});window.addEventListener('focus',()=>{focused=true;activity();});
      document.addEventListener('visibilitychange',()=>{hidden=document.hidden;activity();});
      document.getElementById('scene').onchange=e=>api.scene(e.target.value);
      document.getElementById('shake').onclick=()=>{ddd.audio.unlock();api.shake();};document.getElementById('haptics').onchange=e=>{ddd.haptics=e.target.checked;};
      if(params.has('debug'))document.getElementById('tools').hidden=false;
      if(params.has('scene'))api.scene(params.get('scene'));
      ddd.navigation?.install(api);
    }
    window.addEventListener('resize',resize);canvas.focus({preventScroll:true});
    showGame();if(hidden)activity();else {ddd.audio.suspend(api.paused());raf=requestAnimationFrame(frame);}

  };
  const pct=(a,p)=>a.length?[...a].sort((x,y)=>x-y)[Math.min(a.length-1,Math.floor(a.length*p))]:0;
  ddd.stats=()=>({frames:timings.length,frameMs:{p50:pct(timings,.5),p95:pct(timings,.95),p99:pct(timings,.99)},cpuMs:{p50:pct(renders,.5),p95:pct(renders,.95),p99:pct(renders,.99)},cacheMiB:ddd.cache.bytes/1048576,cacheEntries:ddd.cache.count,heapMiB:performance.memory?performance.memory.usedJSHeapSize/1048576:null,renderScale:scale,resolution:[canvas.width,canvas.height],audio:ddd.audio.state,state:api?.status()});
  // Explicit test controls never alter ordinary frame timing or user progress.
  ddd.test={renderScale(value){fixedScale=Number.isFinite(value);quality.length=0;qualityWait=fastFrames=0;if(fixedScale)setScale(Math.max(.5,Math.min(ceiling,value)));},stop(){cancelAnimationFrame(raf);},resume(){cancelAnimationFrame(raf);last=0;if(!hidden&&focused)raf=requestAnimationFrame(frame);},step(n,dt=1/60){cancelAnimationFrame(raf);for(let i=0;i<n;i++)draw(dt);},clearMetrics(){timings.length=renders.length=0;}};
  ddd.boot=async()=>{try{await platform?.init();await platform?.running();await document.fonts.load('16px Bungee');await document.fonts.ready;await platform?.running();ddd.error=null;ddd.artPause?.(false);ddd.audio.suspend(false);if(playables){ddd.audio.setAllowed(platform.audioAllowed);if(platform.audioAllowed)ddd.audio.unlock();}ddd.audio.preload();main();}catch(e){fail(e);}};
})();
