const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function env(guarded=false){
 const entries=guarded?[{}, {dddWebNavigation:'base'}, {dddWebNavigation:'guard'}]:[{}, {keep:'existing state'}];let at=entries.length-1,listener,backCalls=0;
 const history={get state(){return entries[at];},replaceState(s){entries[at]=s;},pushState(s){entries.splice(at+1);entries.push(s);at++;},back(){backCalls++;if(at){at--;listener?.({state:entries[at]});}}};
 const win={history,ddd:{},addEventListener:(name,f)=>{listener=f;}};win.window=win;
 vm.runInNewContext(fs.readFileSync('web/public/navigation.js','utf8'),win);
 return {win,history,entries,get index(){return at;},get backCalls(){return backCalls;}};
}
test('browser Back routes into game without growing history on repeated presses',()=>{
 const e=env();let handled=0;e.win.ddd.navigation.install({canBack:()=>true,back(){handled++;}});
 for(let i=0;i<20;i++)e.history.back();
 assert.equal(handled,20);assert.equal(e.entries.length,3);assert.equal(e.history.state.dddWebNavigation,'guard');assert.equal(e.history.state.keep,'existing state');
});
test('title Back skips the guard and really leaves; reload does not add another guard',()=>{
 const e=env(true);e.win.ddd.navigation.install({canBack:()=>false});assert.equal(e.entries.length,3);e.history.back();assert.equal(e.index,0);assert.equal(e.backCalls,2);
});
test('keyboard guide consumes Back before gameplay or browser navigation',()=>{
 const e=env();let closed=0;e.win.ddd.keyboardGuide={open:true,close(){closed++;}};e.win.ddd.navigation.install({canBack(){throw Error('guide should own Back');}});
 e.history.back();assert.equal(closed,1);assert.equal(e.history.state.dddWebNavigation,'guard');
});
