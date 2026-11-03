'use strict';
(() => {
  const marker='dddWebNavigation';let installed=false;
  ddd.navigation={install(api){
    if(installed)return;installed=true;
    const state=kind=>({...history.state,[marker]:kind});
    const guard=()=>history.pushState(state('guard'),'');
    // Reuse the existing guard on reload rather than growing the history stack.
    if(history.state?.[marker]!=='guard'){history.replaceState(state('base'),'');guard();}
    window.addEventListener('popstate',event=>{
      if(event.state?.[marker]!=='base')return;
      if(ddd.keyboardGuide?.open){ddd.keyboardGuide.close();guard();}
      else if(api.canBack()){api.back();guard();}
      else history.back(); // Skip our base entry: title-screen Back may leave the game.
    });
  }};
})();
