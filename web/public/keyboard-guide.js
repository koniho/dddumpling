'use strict';
(() => {
  let api,gameCanvas,dialog,keys=[],cards=new Map(),pressed=new Set(),wasPaused=false;
  const seen='KeyboardGuideVersion';
  function draw(key,happy){const card=cards.get(key.letter),canvas=card.querySelector('canvas'),c=canvas.getContext('2d');c.clearRect(0,0,96,96);api.guideKey(c,key.glyph,happy);}
  function practice(letter){
    if(ddd.platform?.paused)return false;
    const key=keys.find(k=>k.letter===letter && k.active);if(!key)return false;
    pressed.add(letter);ddd.audio.unlock();draw(key,true);const card=cards.get(letter);card.classList.add('tried');
    if(!matchMedia('(prefers-reduced-motion: reduce)').matches)card.animate([{transform:'scale(1)'},{transform:'scale(1.16,.78) rotate(-5deg)'},{transform:'scale(.94,1.10)'},{transform:'scale(1)'}],{duration:400});
    const total=keys.filter(k=>k.active).length;
    dialog.querySelector('[data-progress]').textContent=pressed.size===total?'Ready to squish!':`Try your keys: ${pressed.size} / ${total}`;
    return true;
  }
  function close(){
    if(!dialog?.open||ddd.platform?.paused)return;dialog.close();ddd.storage.put(seen,'1');
    if(ddd.platform?.paused)return;api.hidden(false);if(!wasPaused && api.paused())api.back();gameCanvas.focus({preventScroll:true});
  }
  function show(){
    if(!api || dialog?.open || ddd.platform?.paused)return;
    if(!dialog){
      dialog=document.createElement('dialog');dialog.id='keyboard-guide';dialog.setAttribute('aria-labelledby','keyboard-guide-title');
      dialog.innerHTML='<h1 id="keyboard-guide-title">Your keyboard crew</h1><p>Match the squishy. Press its key.</p><div class="keyboard-crew"></div><p data-progress aria-live="polite"></p><p data-extra class="keyboard-extra"></p><p class="keyboard-extra">Use your mouse to click, swipe and trace.<br>Drag a ready Dark Divide glob to stretch it. A mirrored finger helps you split it.<br><kbd>Esc</kbd> pauses. <kbd>?</kbd> brings this guide back.</p><button class="keyboard-ready">Got it!</button>';
      document.body.append(dialog);dialog.querySelector('.keyboard-ready').onclick=close;dialog.addEventListener('cancel',close);
    }
    keys=api.keyboardGuide().split(';').filter(Boolean).map(s=>{const [letter,glyph,active]=s.split(',');return {letter,glyph:Number(glyph),active:active==='1'};});
    pressed.clear();cards.clear();const row=dialog.querySelector('.keyboard-crew');row.replaceChildren();
    for(const key of keys){
      const card=document.createElement('button');card.className='keyboard-squishy';card.disabled=!key.active;card.setAttribute('aria-label',`Practise ${key.letter.toUpperCase()}`);
      const icon=document.createElement('canvas');icon.width=icon.height=96;icon.setAttribute('aria-hidden','true');
      const label=document.createElement('kbd');label.textContent=key.letter.toUpperCase();card.append(icon,label);row.append(card);cards.set(key.letter,card);draw(key,false);card.onclick=()=>practice(key.letter);
    }
    const extra=keys.filter(k=>!k.active).map(k=>k.letter.toUpperCase());
    dialog.querySelector('[data-extra]').textContent=extra.length?`${extra.join(' + ')} join when all six squishies play.`:'';
    dialog.querySelector('[data-progress]').textContent=`Try your keys: 0 / ${keys.filter(k=>k.active).length}`;
    wasPaused=api.paused();api.hidden(true);dialog.showModal();dialog.querySelector('.keyboard-ready').focus();
  }
  window.addEventListener('keydown',e=>{
    if(ddd.platform?.paused||e.ctrlKey||e.metaKey||e.altKey)return;
    if(dialog?.open){
      const letter=e.key.toLowerCase();
      if(keys.some(k=>k.letter===letter)){e.preventDefault();e.stopImmediatePropagation();if(!e.repeat)practice(letter);}
      else if(e.key==='Escape'){e.dddGuideHandled=true;close();}
      return;
    }
    if(e.key==='?' && !e.target.closest('input,textarea,select,[contenteditable="true"]')){e.preventDefault();e.stopImmediatePropagation();show();}
  },true);
  window.addEventListener('keyup',e=>{if(dialog?.open && keys.some(k=>k.letter===e.key.toLowerCase())){e.preventDefault();e.stopImmediatePropagation();}},true);
  ddd.keyboardGuide={show,close,get open(){return !!dialog?.open;},init(game,canvas,automatic){api=game;gameCanvas=canvas;if(automatic && matchMedia('(any-pointer: fine) and (any-hover: hover)').matches && ddd.storage.get(seen)!=='1')show();}};
})();
