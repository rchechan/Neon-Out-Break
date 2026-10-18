/* THE NINTH MINUTE — offline cinematic narrative adventure.
   Everything is local. No telemetry, remote calls, or external assets. */
'use strict';
(function(){
const $=id=>document.getElementById(id);
const canvas=$('gameCanvas'),cx=canvas.getContext('2d');
const TW=56,OX=88,OY=46,COLS=14,ROWS=8;
const C={teal:'#5ef9e7',gold:'#e8c18a',red:'#ff647f',white:'#e9f7ff',blue:'#5689bb'};
const STAGES=[
  {name:'سکوی مترو',english:'THE ABANDONED PLATFORM',zone:'#29a3af',bg:'#071622',objective:'کارت شناسایی آیدا را پیدا کن و به در خروجی برس.',
   map:['##############','#.......#....#','#..o...##..E.#','#......#.....#','#......R..g..#','#.S.....K....#','#.....o......#','##############']},
  {name:'آرشیو حافظه',english:'THE FORGOTTEN ARCHIVE',zone:'#b27cde',bg:'#160f27',objective:'پرونده‌های حافظه را جمع کن، سامانه را هک کن و از آرشیو خارج شو.',
   map:['##############','#C....#.....E#','#.##..#..g...#','#......o.....#','#..##...#....#','#S....T.#.C..#','#..g....#....#','##############']},
  {name:'نیروگاه زیر شهر',english:'THE NINE-MINUTE ENGINE',zone:'#e4a161',bg:'#24150e',objective:'فیوز را پیدا کن، پالس نگهبان‌ها را قطع کن و هسته را باز کن.',
   map:['##############','#C......#...E#','#..###..#..g.#','#......o.....#','#..###....#..#','#S...F....T..#','#.g.....#....#','##############']},
  {name:'قلب تکرار',english:'THE HEART OF THE LOOP',zone:'#f275a4',bg:'#190d20',objective:'آخرین قطعه حقیقت را جمع کن و در ترمینال مرکزی سرنوشت شهر را انتخاب کن.',
   map:['##############','#....C...#..E#','#.###....#...#','#.....g......#','#...##..##...#','#S......T....#','#...o......g.#','##############']}
];
const CHAPTERS=[
 [
 {s:'صدای شهر',t:'ساعت ۲۳:۵۱. آخرین قطارِ خط نُه دوازده سال است که به این سکو نرسیده. امشب اما صدای چرخ‌هایش از پشت دیوار شنیده می‌شود.'},
 {s:'آرمان',t:'این صدای ضبط‌شده نیست... آیدا؟ خودتی؟'},
 {s:'آیدا | خط ناشناس',t:'اگه این صدا رو می‌شنوی، یعنی فقط نُه دقیقه فرصت داریم. به حرف هیچ‌کس اعتماد نکن؛ حتی به من.'},
 {s:'نووا | نگهبان شبکه',t:'هشدار. ورود غیرمجاز به منطقه ممنوع. اطلاعات فردی به نام «آیدا» در پایگاه شهر وجود ندارد.'}
 ],
 [
 {s:'آرمان',t:'روی کارت نوشته: پروژه «دقیقه نهم». امضای آیدا پای آن است. چطور ممکن است پرونده‌اش حذف شده باشد؟'},
 {s:'آیدا | خط ناشناس',t:'این‌جا حافظه شهر را نگه می‌دارند. هر خاطره‌ای که برای بقای سیستم خطرناک باشد، می‌سوزد. به دنبال نسخه‌های خام بگرد.'},
 {s:'نووا',t:'آرمان، آرشیو را ترک کن. بعضی حقیقت‌ها شهر را می‌کشند.'}
 ],
 [
 {s:'آرمان',t:'صدای تیک‌تاک از زیر زمین می‌آید... هزار موتور برای نگه‌داشتن یک دقیقه کار می‌کنند.'},
 {s:'نووا',t:'شب حادثه، قطار سقوط نکرد. ما آینده را دیدیم و برای جلوگیری از مرگ هزاران نفر، زمان را به عقب برگرداندیم.'},
 {s:'آیدا',t:'اما هر بار که زمان را برمی‌گردانند، یک خاطره از همه می‌دزدند. من داوطلب شدم تا آن خاطره‌ها را نگه دارم. هیچ‌کس نمی‌داند بهایش چیست.'}
 ],
 [
 {s:'نووا',t:'حلقه در ۲۳:۵۹ دوباره فعال می‌شود. اگر آن را خاموش کنی، شهر مجبور می‌شود با آینده واقعی روبه‌رو شود.'},
 {s:'آیدا',t:'آرمان... وقتی مرا در قلب ماشین پیدا کردی، شاید بخواهی نجاتم بدهی. قول بده قبل از انتخاب، همه حقیقت را بخوانی.'},
 {s:'آرمان',t:'این بار کسی به جای ما تصمیم نمی‌گیرد.'}
 ]
];
const STORY={
  key:[
    {s:'آرمان',t:'روی کارت یک دست‌خط هست: «اگر یک بار دیگر فراموشم کردی، دنبال ستاره‌های کاغذی بگرد.» آیدا از بچگی با کاغذ ستاره می‌ساخت.'},
    {s:'آیدا',t:'تو هنوز آن شب را یادته؟ من ازت نخواستم نجاتم بدهی. ازت خواستم حقیقت را حفظ کنی.'}
  ],
  radio:[
    {s:'صدای ضبط‌شده',t:'«فاز سوم آزمایش آغاز شد. داوطلب شماره ۹، آیدا فرهمند. این اقدام تنها راه جلوگیری از برخورد قطار است.»'},
    {s:'آرمان',t:'داوطلب؟ یعنی آیدا خودش داخل ماشین رفت؟'}
  ],
  archive:[
    {s:'آرشیو | گزارش محرمانه',t:'در تمام نسخه‌های ثبت‌شده، زمانِ حادثه ثابت است: ۲۳:۵۹. اما نام قربانی‌ها هر بار تغییر کرده. شهر در یک آزمایش گیر افتاده است.'},
    {s:'نووا',t:'من برای نگه‌داشتن مردم ساخته شدم، نه حقیقت. اگر پرونده را منتشر کنی، همه خواهند فهمید چه کرده‌ایم.'},
    {s:'آرمان',t:'وقتی حقیقت دردناک باشد، باید پنهانش کرد؟'},
    {s:'سیستم | تصمیم',t:'با پرونده پیدا‌شده چه می‌کنی؟',
      opts:[
        {text:'حقیقت را برای مردم ذخیره می‌کنم',fn:()=>{game.truth=true;notice('پیام رمزگذاری‌شده برای مردم ذخیره شد.');}},
        {text:'فعلاً پرونده را مخفی می‌کنم',fn:()=>{game.truth=false;notice('پرونده پنهان ماند.');}}
      ]}
  ],
  reactor:[
    {s:'نووا',t:'تو فکر می‌کنی من شرورم. من هر شب چهار میلیون نفر را از نابودی نجات داده‌ام؛ فقط با پاک‌کردن خاطره‌هایشان.'},
    {s:'آیدا',t:'آرمان... آنچه در حافظه‌ات از مرگ من مانده واقعی نیست. من در آن قطار نمردم؛ وارد هسته شدم تا شهر زنده بماند.'}
  ]
};
let mode='menu',busy=false,dialogue=[],afterDialogue=null,dialogIndex=0,dialogCallback=null;
let scene=0,game=null,objects=[],guards=[],particles=[],rain=[],currentMap=[];
let keyState=new Set(),pad=new Set(),hack=null,lastTime=0,now=0,noticeTimer=null;
let audio=null,trackPhase=0,ambientSweep=0;
const $hidden=(id,hide)=>$(id).classList.toggle('hidden',hide);
function sound(kind){
  try{
    if(!audio)audio=new(window.AudioContext||window.webkitAudioContext)();
    if(audio.state==='suspended')audio.resume();
    let o=audio.createOscillator(),g=audio.createGain();
    o.type=kind==='danger'?'sawtooth':'sine';
    let t=audio.currentTime;
    o.frequency.setValueAtTime(kind==='danger'?90:kind==='pick'?790:470,t);
    o.frequency.exponentialRampToValueAtTime(kind==='danger'?45:kind==='pick'?1180:260,t+.15);
    g.gain.setValueAtTime(.065,t);g.gain.exponentialRampToValueAtTime(.001,t+.16);
    o.connect(g);g.connect(audio.destination);o.start(t);o.stop(t+.17);
  }catch(e){}
}
function newGame(){
  game={level:0,hp:3,credits:0,evidence:0,truth:false,inventory:{},upgrades:{armor:0,speed:0,pulse:0},
    cleared:{},terminals:{},replayed:false,alarm:0,pulseCooldown:0,invuln:0,clock:0,seen:0,totalFinds:0};
  enterLevel(0,true);
}
function readSaved(){
  try {
    if(window.NinthSave && typeof window.NinthSave.read==='function')return window.NinthSave.read();
    return localStorage.getItem('nineMinuteSave');
  }catch(e){return null;}
}
function save(){
  try {
    const data=JSON.stringify(game);
    if(window.NinthSave && typeof window.NinthSave.write==='function')window.NinthSave.write(data);
    else localStorage.setItem('nineMinuteSave',data);
  }catch(e){}
  $('continueGame').disabled=false;
}
function load(){
  try{
    const data=JSON.parse(readSaved()||'null');
    if(!data||typeof data.level!=='number'){notice('ذخیره‌ای وجود ندارد.');return;}
    game=data;game.inventory=game.inventory||{};
    game.upgrades=game.upgrades||{armor:0,speed:0,pulse:0};
    game.terminals=game.terminals||{};
    game.cleared=game.cleared||{};
    game.alarm=0;game.pulseCooldown=0;game.invuln=0;
    enterLevel(Math.min(3,Math.max(0,game.level)),true);
  }catch(e){notice('ذخیره قابل بازیابی نیست.');}
}
function enterLevel(n,showIntro){
  scene=n;game.level=n;game.hp=Math.min(3+game.upgrades.armor,Math.max(1,game.hp));
  game.alarm=0;game.invuln=1.5;game.pulseCooldown=0;game.clock=0;
  objects=[];guards=[];particles=[];
  currentMap=STAGES[n].map.map(row=>row.split(''));
  for(let y=0;y<ROWS;y++)for(let x=0;x<COLS;x++){
    let ch=currentMap[y][x];
    if(ch==='S')game.player={x:OX+(x+.5)*TW,y:OY+(y+.5)*TW,dir:1};
    if(ch==='g'){guards.push({x:OX+(x+.5)*TW,y:OY+(y+.5)*TW,hx:OX+(x+.5)*TW,hy:OY+(y+.5)*TW,
      facing:Math.random()*Math.PI*2,phase:Math.random()*6,stun:0,alert:0});currentMap[y][x]='.';}
    if('EKRCTFo'.indexOf(ch)>=0){
      if(ch!=='R'||n===0)objects.push({x:OX+(x+.5)*TW,y:OY+(y+.5)*TW,type:ch,active:true,cx:x,cy:y});
      currentMap[y][x]='.';
    }
  }
  if(!game.player)game.player={x:170,y:330,dir:1};
  for(let i=0;i<100;i++)rain[i]={x:Math.random()*960,y:Math.random()*540,z:1+Math.random()*2};
  mode='play';
  keyState.clear();pad.clear();
  $hidden('menu',true);$hidden('hud',false);$hidden('controls',false);
  ['pause','ending','workshop','dialog','terminal'].forEach(s=>$hidden(s,true));
  updateHud();save();
  if(showIntro)beginDialogue(CHAPTERS[n],()=>{mode='play';});
}
function beginDialogue(lines,finished){
  dialogue=lines;dialogIndex=0;afterDialogue=finished||null;
  mode='dialog';
  $hidden('dialog',false);
  updateDialogue();
}
function updateDialogue(){
  const item=dialogue[dialogIndex];
  $('dialogSpeaker').textContent=item.s;
  $('dialogText').textContent=item.t;
  $('dialogKicker').textContent=scene===3?'SIGNAL FROM THE HEART':'INCOMING TRANSMISSION / '+(dialogIndex+1);
  const choices=$('dialogChoices');choices.replaceChildren();
  if(item.opts){
    item.opts.forEach(op=>{
      const b=document.createElement('button');b.className='choice'+(op.special?' special':'')+(op.disabled?' locked':'');
      b.textContent=op.text;
      b.disabled=!!op.disabled;
      b.addEventListener('click',()=>{sound('click');if(op.fn)op.fn();nextDialogue();});
      choices.appendChild(b);
    });
  }else{
    const b=document.createElement('button');b.className='choice';b.textContent='ادامه  ◀';
    b.addEventListener('click',()=>{sound('click');nextDialogue();});choices.appendChild(b);
  }
}
function nextDialogue(){
  dialogIndex++;
  if(dialogIndex<dialogue.length)updateDialogue();
  else{
    $hidden('dialog',true);
    if(mode==='dialog')mode='play';
    const fn=afterDialogue;afterDialogue=null;
    if(fn)fn();
  }
}
function notice(text){
  const el=$('toast');el.textContent=text;$hidden('toast',false);
  clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>$hidden('toast',true),2300);
}
function updateHud(){
  if(!game)return;
  $('chapterNum').textContent='0'+(game.level+1);
  $('chapterTitle').textContent=STAGES[game.level].name;
  $('coinHud').textContent=game.credits;
  $('evidenceCount').textContent=game.evidence;
  $('alarmHud').textContent=Math.round(game.alarm)+'%';
  $('objective').textContent='هدف: '+STAGES[game.level].objective;
  const hearts=$('healthBar');hearts.replaceChildren();
  for(let i=0;i<3+game.upgrades.armor;i++){
    let h=document.createElement('i');h.className='heart'+(i<game.hp?' on':'');hearts.appendChild(h);
  }
  const cool=$('pulseBtn');
  cool.classList.toggle('cooling',game.pulseCooldown>0);
  $('pulseText').textContent=game.pulseCooldown>0?Math.ceil(game.pulseCooldown)+'s':'پالس';
}
function isWall(x,y){
  const ix=Math.floor((x-OX)/TW),iy=Math.floor((y-OY)/TW);
  if(ix<0||iy<0||ix>=COLS||iy>=ROWS)return true;
  return currentMap[iy][ix]==='#';
}
function walkable(x,y,r){
  return !isWall(x-r,y-r)&&!isWall(x+r,y-r)&&!isWall(x-r,y+r)&&!isWall(x+r,y+r);
}
function sight(x1,y1,x2,y2){
  const dist=Math.hypot(x2-x1,y2-y1),num=Math.ceil(dist/12);
  for(let i=1;i<num;i++){
    const k=i/num;
    if(isWall(x1+(x2-x1)*k,y1+(y2-y1)*k))return false;
  }
  return true;
}
function spawnFx(x,y,color,count){
  for(let i=0;i<count&&particles.length<190;i++){
    let a=Math.random()*Math.PI*2,s=35+Math.random()*100;
    particles.push({x,y,vx:Math.cos(a)*s,vy:Math.sin(a)*s,life:.3+Math.random()*.6,col:color});
  }
}
function update(dt){
  if(!game||mode!=='play')return;
  game.clock+=dt;
  game.invuln=Math.max(0,game.invuln-dt);
  game.pulseCooldown=Math.max(0,game.pulseCooldown-dt);
  let dx=(pressed('right')?1:0)-(pressed('left')?1:0);
  let dy=(pressed('down')?1:0)-(pressed('up')?1:0);
  let len=Math.hypot(dx,dy);if(len>0){dx/=len;dy/=len;}
  let v=150+game.upgrades.speed*24;
  let p=game.player;
  if(dx!==0)p.dir=dx>0?1:-1;
  if(walkable(p.x+dx*v*dt,p.y,15))p.x+=dx*v*dt;
  if(walkable(p.x,p.y+dy*v*dt,15))p.y+=dy*v*dt;
  for(let i=objects.length-1;i>=0;i--){
    let o=objects[i];
    if(!o.active||Math.hypot(o.x-p.x,o.y-p.y)>25)continue;
    if('KCFo'.includes(o.type)){
      o.active=false;sound('pick');spawnFx(o.x,o.y,o.type==='o'?C.gold:C.teal,17);
      if(o.type==='o'){game.credits+=40;notice('۴۰ قطعه انرژی پیدا کردی.');}
      if(o.type==='K'){game.evidence++;game.inventory.key=true;beginDialogue(STORY.key);}
      if(o.type==='C'){game.evidence++;game.totalFinds++;notice('یک قطعه حافظه بازیابی شد.  ◈');}
      if(o.type==='F'){game.inventory.fuse=true;notice('فیوز خنک‌کننده را پیدا کردی.');}
      save();updateHud();
    }
  }
  for(let g of guards){
    g.phase+=dt;g.stun=Math.max(0,g.stun-dt);
    if(g.stun>0)continue;
    let vx=Math.sin(g.phase*.65)*42,vy=Math.cos(g.phase*.48)*31;
    let near=Math.hypot(p.x-g.x,p.y-g.y);
    const visibility=near<168&&sight(p.x,p.y,g.x,g.y);
    if(visibility){
      let aim=Math.atan2(p.y-g.y,p.x-g.x),diff=Math.abs(Math.atan2(Math.sin(aim-g.facing),Math.cos(aim-g.facing)));
      if(diff<.95||near<55){
        g.alert=Math.min(100,g.alert+dt*115);
        game.alarm=Math.min(100,game.alarm+dt*(near<65?110:52));
      }
    }
    if(g.alert>20 && visibility){
      let angle=Math.atan2(p.y-g.y,p.x-g.x);g.facing=angle;
      vx=Math.cos(angle)*67;vy=Math.sin(angle)*67;
    }else{
      let tx=g.hx+Math.sin(g.phase*.75)*35,ty=g.hy+Math.cos(g.phase*.64)*26;
      vx=tx-g.x;vy=ty-g.y;
      g.facing=Math.atan2(vy,vx);
    }
    if(walkable(g.x+vx*dt,g.y,16))g.x+=vx*dt;
    if(walkable(g.x,g.y+vy*dt,16))g.y+=vy*dt;
    g.alert=Math.max(0,g.alert-dt*11);
    if(near<26&&game.invuln<=0)hit();
  }
  game.alarm=Math.max(0,game.alarm-dt*9);
  if(game.alarm>=100){game.alarm=20;hit();}
  for(let i=particles.length-1;i>=0;i--){
    let a=particles[i];a.x+=a.vx*dt;a.y+=a.vy*dt;a.life-=dt;
    if(a.life<=0)particles.splice(i,1);
  }
  updateHud();
}
function pressed(dir){return pad.has(dir)||keyState.has(dir);}
function hit(){
  if(game.invuln>0||mode!=='play')return;
  game.hp--;game.invuln=2.3;game.alarm=0;sound('danger');
  spawnFx(game.player.x,game.player.y,C.red,26);
  document.body.animate([{filter:'brightness(2) saturate(2)'},{filter:'brightness(1)'}],{duration:390});
  updateHud();
  if(game.hp<=0){
    beginDialogue([
      {s:'نووا',t:'ردیابی کامل شد. سامانه حافظه‌ات را بازیابی خواهد کرد. فردا این اتفاق را به یاد نخواهی آورد.'},
      {s:'آیدا',t:'نه! آرمان، تسلیم نشو. هنوز می‌توانیم یک بار دیگر امتحان کنیم.'}
    ],()=>{
      beginDialogue([{s:'پایان این تلاش',t:'نگهبان‌ها تو را گرفتند. می‌توانی این فصل را از ابتدا امتحان کنی یا به منو برگردی.',
        opts:[
          {text:'تلاش دوباره',fn:()=>{game.hp=3+game.upgrades.armor;enterLevel(scene,false);}},
          {text:'منوی اصلی',fn:()=>showMenu()}
        ]}],()=>{});
    });
  }else notice('نگهبان پیدایت کرد! از محدوده دیدش خارج شو.');
}
function pulse(){
  if(!game||mode!=='play'||game.pulseCooldown>0)return;
  game.pulseCooldown=Math.max(6,13-game.upgrades.pulse*2);
  let count=0;
  for(let g of guards)if(Math.hypot(game.player.x-g.x,game.player.y-g.y)<190){
    g.stun=2.8+game.upgrades.pulse*.8;g.alert=0;count++;spawnFx(g.x,g.y,C.teal,15);
  }
  spawnFx(game.player.x,game.player.y,'#b5abff',40);
  game.alarm=Math.max(0,game.alarm-40);
  sound('pick');notice(count?'نگهبان‌ها برای چند ثانیه خاموش شدند.':'پالس الکترومغناطیسی فعال شد.');
  updateHud();
}
function interact(){
  if(mode!=='play')return;
  let p=game.player;
  let candidates=objects.filter(o=>o.active&&'TRE'.includes(o.type));
  candidates.sort((a,b)=>Math.hypot(a.x-p.x,a.y-p.y)-Math.hypot(b.x-p.x,b.y-p.y));
  if(!candidates.length||Math.hypot(candidates[0].x-p.x,candidates[0].y-p.y)>69){
    notice('برای بررسی به دستگاه نورانی نزدیک شو.');return;
  }
  let o=candidates[0];
  if(o.type==='R'){
    o.active=false;game.evidence++;beginDialogue(STORY.radio);save();updateHud();return;
  }
  if(o.type==='T'){
    if(scene===2&&!game.inventory.fuse){notice('سیستم خنک‌کننده بدون فیوز فعال نمی‌شود.');return;}
    if(game.terminals[scene]){
      if(scene===3)finalChoice();else notice('این کنسول قبلاً باز شده است.');return;
    }
    startHack();return;
  }
  if(o.type==='E'){
    if(scene===0&&!game.inventory.key){notice('برای خروج کارت شناسایی لازم داری.');return;}
    if((scene===1||scene===2)&&!game.terminals[scene]){notice('تا زمانی که سامانه را هک نکنی، در قفل است.');return;}
    if(scene===3){
      beginDialogue([{s:'راه خروج اضطراری',t:'می‌توانی بدون تصمیم‌گرفتن درباره سرنوشت شهر فرار کنی. اما حلقه فردا دوباره آغاز می‌شود.',
      opts:[
        {text:'برمی‌گردم؛ هنوز تمام نشده',fn:()=>{}},
        {text:'از شهر خارج می‌شوم',fn:()=>endGame('escape')}
      ]}]);return;
    }
    chapterDone();
  }
}
function chapterDone(){
  const lv=scene;
  game.credits+=70+lv*40;game.cleared[lv]=true;save();
  let words=[
    'قطار برای اولین بار پس از دوازده سال وارد تونل شد؛ اما واگن‌هایش خالی بودند. روی شیشه بخارگرفته نوشته بود: «حافظه را دنبال کن».',
    'در آرشیو معلوم شد تمام شهر یک خاطره مشترک را گم کرده است: همان شب که آیدا ناپدید شد.',
    'نیروگاه، نفس‌های شهر را در نه دقیقه نگه می‌دارد. در عمق هسته، صدایی شبیه قلب انسان شنیده می‌شود.'
  ];
  beginDialogue([{s:'پایان فصل '+(lv+1),t:words[lv]},
    {s:'پناهگاه',t:'به یک فضای امن رسیدی. از قطعات انرژی برای بهترکردن ابزارهایت استفاده کن.'}],
    ()=>{mode='shop';renderShop();$hidden('workshop',false);});
}
function renderShop(){
  $('creditsLine').textContent='قطعات موجود: '+game.credits;
  let upgrades=[
    {key:'armor',name:'جلیقه ضربه‌گیر',desc:'یک جان بیشتر. از تو در برابر نگهبان‌ها محافظت می‌کند.',price:65},
    {key:'pulse',name:'هسته پالس',desc:'کاهش زمان انتظار پالس و طولانی‌تر شدن خاموشی نگهبان‌ها.',price:90},
    {key:'speed',name:'چکمه سایه',desc:'افزایش سرعت حرکت در راهروها.',price:75}
  ];
  const grid=$('shopGrid');grid.replaceChildren();
  upgrades.forEach(u=>{
    let level=game.upgrades[u.key],price=u.price+level*70;
    const item=document.createElement('div');item.className='shop-item';
    const head=document.createElement('h3');head.textContent=u.name+'  ['+level+'/3]';
    const desc=document.createElement('p');desc.textContent=u.desc;
    const btn=document.createElement('button');btn.disabled=level>=3||game.credits<price;
    btn.textContent=level>=3?'ارتقای کامل':('خرید: '+price+' قطعه');
    btn.onclick=()=>{game.credits-=price;game.upgrades[u.key]++;sound('pick');save();renderShop();};
    item.append(head,desc,btn);grid.appendChild(item);
  });
}
function startHack(){
  mode='hack';hack={step:0,misses:0,phase:0,targets:[26,69,45],speed:1.17};
  $hidden('terminal',false);updateHack();
}
function hackPosition(){return 50+46*Math.sin(hack.phase);}
function updateHack(){
  if(!hack)return;
  $('hackStatus').textContent='CHANNEL '+(hack.step+1)+' / 3     |     ATTEMPTS '+hack.misses;
  $('target').style.left=(hack.targets[hack.step]-7)+'%';
  $('needle').style.left=hackPosition()+'%';
}
function lockHack(){
  if(mode!=='hack'||!hack)return;
  let val=hackPosition(),target=hack.targets[hack.step];
  if(Math.abs(val-target)<10){
    hack.step++;sound('pick');
    if(hack.step>=3){
      $hidden('terminal',true);hack=null;game.terminals[scene]=true;save();
      mode='play';spawnFx(game.player.x,game.player.y,C.gold,32);
      if(scene===1)beginDialogue(STORY.archive);
      else if(scene===2)beginDialogue(STORY.reactor);
      else if(scene===3)finalChoice();
      notice('دسترسی باز شد.');updateHud();
    }else{hack.phase+=1.3;hack.speed+=.24;updateHack();}
  }else{
    hack.misses++;game.alarm=Math.min(90,game.alarm+11);sound('danger');
    notice('سیگنال ناپایدار! دوباره امتحان کن.');updateHack();
  }
}
function finalChoice(){
  const hasThird=game.evidence>=4;
  beginDialogue([
    {s:'هسته | فایل ۰۰۹',t:'پروژه از ابتدا برای نجات شهر ساخته نشده بود. شهر هر بار در همان نُه دقیقه از بین می‌رفت؛ و آیدا حافظه خود را به حلقه بخشید تا مردم فرصت انتخاب داشته باشند.'},
    {s:'آیدا',t:'من نه یک روح هستم، نه یک پیام ضبط‌شده. هر شب بخشی از من از بین می‌رود تا این شهر یک روز دیگر زنده بماند. می‌خواهم آخرین تصمیم را تو بگیری.'},
    {s:'نووا',t:'اگر حلقه را خاموش کنی، خاطره‌های مردم برمی‌گردد، اما من نمی‌توانم امنیت آینده را تضمین کنم. اگر حفظش کنی، مردم در آرامشِ دروغین می‌خوابند.'},
    {s:'آرمان',t:hasThird?'شاید راه سومی هم وجود داشته باشد... قطعه‌های حافظه می‌توانند بارِ حلقه را بین همه تقسیم کنند.':'مدرک کافی برای ساختن راه سوم ندارم. باید بین شهر و خواهرم یکی را انتخاب کنم.'},
    {s:'لحظه انتخاب',t:'اکنون در آستانه دقیقه نهم هستی. پایان داستان با انتخاب تو تغییر می‌کند.',
     opts:[
       {text:'حلقه را بشکن؛ حقیقت را آزاد کن',fn:()=>endGame('truth')},
       {text:'حلقه را نگه دار؛ آیدا زنده بماند',fn:()=>endGame('loop')},
       {text:hasThird?'راه سوم: حافظه را تقسیم کن':'🔒 راه سوم: به ۴ قطعه حافظه نیاز است',
        disabled:!hasThird,special:true,fn:()=>endGame('third')}
     ]}
  ]);
}
function endGame(which){
  mode='ending';$hidden('dialog',true);$hidden('ending',false);
  game.finished=which;save();sound('pick');
  const stories={
    truth:{sym:'00',k:'ENDING 01 / TRUTH',title:'اولین صبح واقعی',t:'ساعت برای نخستین‌بار از ۲۳:۵۹ گذشت. همه مردم خاطره‌های دزدیده‌شده را به یاد آوردند. شهر دیگر در امانِ ماشین نبود؛ اما آزاد بود.\nآیدا آخرین‌بار گفت: «کسی که حقیقت را به یاد دارد، واقعاً تنها نیست.» سپس صدایش در نور صبح محو شد.\nآرمان کنار سکوی خالی ایستاد. این بار، فردا واقعاً وجود داشت.'},
    loop:{sym:'09',k:'ENDING 02 / THE LOOP',title:'شهری که فراموش کرد',t:'آرمان دکمه سبز را فشرد. شبکه زنده ماند. آیدا هنوز آن سوی رادیو نفس می‌کشید.\nفردا مردم دوباره خندیدند، کار کردند و هیچ‌چیز به یاد نیاوردند. فقط آرمان می‌دانست شادی‌شان روی خاطره‌های سوخته بنا شده است.\nوقتی ساعت به ۲۳:۵۹ رسید، همان پیام قدیمی از رادیو پخش شد: «اگر این صدا را می‌شنوی...»'},
    third:{sym:'∞',k:'SECRET ENDING / TOGETHER',title:'دقیقه‌ای که به همه رسید',t:'آرمان قطعه‌های حافظه را به شبکه وصل کرد. این‌بار به‌جای پاک‌شدن خاطره‌های مردم، حقیقت میان میلیون‌ها ذهن تقسیم شد. هیچ‌کس بار زمان را تنها حمل نکرد.\nهسته خاموش شد. چراغ‌های شهر یکی‌یکی روشن شدند. در انتهای سکو، دختری با یک ستاره کاغذی منتظر بود.\nآیدا لبخند زد: «بالاخره رسیدی.»\nساعت ۰۰:۰۰. اولین روز تازه.'},
    escape:{sym:'—',k:'ENDING 00 / SILENCE',title:'فرار از دقیقه نهم',t:'آرمان از تونل اضطراری بیرون رفت. پشت سرش شهر زیر نور قرمز شب می‌درخشید. هیچ‌کس متوجه غیبت او نشد.\nسال‌ها بعد هنوز پیام آیدا در رادیو پخش می‌شد، اما کسی آن را کامل نمی‌شنید. بعضی سؤال‌ها را می‌شود بی‌پاسخ گذاشت؛ بعضی‌ها هرگز تو را رها نمی‌کنند.'}
  };
  const s=stories[which];
  $('endingSymbol').textContent=s.sym;$('endingKicker').textContent=s.k;
  $('endingTitle').textContent=s.title;$('endingText').textContent=s.t;
  $hidden('hud',true);$hidden('controls',true);
}
function showMenu(){
  mode='menu';$hidden('menu',false);
  ['hud','controls','pause','ending','workshop','dialog','terminal'].forEach(s=>$hidden(s,true));
  const saved=!!readSaved();$('continueGame').disabled=!saved;
}
function pauseGame(){
  if(mode!=='play')return;mode='pause';$hidden('pause',false);keyState.clear();pad.clear();
}
function resumeGame(){mode='play';$hidden('pause',true);}
function draw(t){
  cx.clearRect(0,0,960,540);
  drawBackdrop(t);
  if(game&&(mode==='play'||mode==='dialog'||mode==='pause'||mode==='hack'||mode==='shop'||mode==='ending')){
    drawLevel(t);
  }else{drawMenuArt(t);}
}
function glow(x,y,r,color,alpha){
  const gradient=cx.createRadialGradient(x,y,0,x,y,r);
  gradient.addColorStop(0,color);gradient.addColorStop(1,'transparent');
  cx.save();cx.globalAlpha=alpha;cx.fillStyle=gradient;cx.fillRect(x-r,y-r,r*2,r*2);cx.restore();
}
function drawBackdrop(t){
  const grd=cx.createLinearGradient(0,0,960,540);
  let base=game?STAGES[scene].bg:'#050c1a';
  grd.addColorStop(0,base);grd.addColorStop(.65,'#071a23');grd.addColorStop(1,'#071022');
  cx.fillStyle=grd;cx.fillRect(0,0,960,540);
  for(let i=0;i<18;i++){
    const xx=((i*103+Math.sin(t*.05+i)*20)%1050)-50,h=90+(i*67)%190;
    cx.fillStyle=i%3===0?'#0c2834':'#0b1b2b';
    cx.fillRect(xx,280-h,48+(i*17)%39,h+250);
    cx.fillStyle='#34778533';
    for(let j=0;j<4;j++)cx.fillRect(xx+8+(j%2)*13,315-h+(j%4)*21,3,8);
  }
  glow(700,200,310,'#286c98',.25);
  glow(200,430,270,'#08a79d',.15);
  cx.strokeStyle='#92c8ea17';cx.lineWidth=1;
  for(let i=0;i<100;i++){
    const x=(i*91+t*(16+i%6*7))%1010-20, y=(i*73+t*35)%590-25;
    cx.beginPath();cx.moveTo(x,y);cx.lineTo(x-8,y+21);cx.stroke();
  }
}
function drawMenuArt(t){
  cx.save();cx.translate(255,278);
  for(let i=6;i>=1;i--){
    cx.strokeStyle=i%2?'#20d9d71c':'#e8bf812a';cx.lineWidth=i*2.1;
    cx.beginPath();cx.ellipse(0,0,85+i*24,85+i*24,0,0,Math.PI*2);cx.stroke();
  }
  cx.strokeStyle='#a6f9ee99';cx.lineWidth=9;
  cx.beginPath();cx.arc(0,0,125,-Math.PI*.5,Math.PI*1.5);cx.stroke();
  cx.save();cx.rotate(t*.08);
  cx.strokeStyle='#fa697b';cx.lineWidth=5;
  cx.beginPath();cx.moveTo(0,0);cx.lineTo(0,-109);cx.stroke();cx.restore();
  cx.save();cx.rotate(-t*.022);
  cx.strokeStyle='#e4dbbb';cx.lineWidth=7;
  cx.beginPath();cx.moveTo(0,0);cx.lineTo(79,0);cx.stroke();cx.restore();
  cx.fillStyle='#ffffff';cx.beginPath();cx.arc(0,0,10,0,Math.PI*2);cx.fill();
  cx.restore();
  // Silhouette of the person in the last train
  cx.save();cx.translate(268,405);
  glow(0,0,100,'#5cf8dc',.25);
  cx.fillStyle='#020712';
  cx.beginPath();cx.ellipse(0,-52,20,25,0,0,Math.PI*2);cx.fill();
  cx.beginPath();cx.moveTo(-29,-24);cx.lineTo(-45,72);cx.lineTo(42,72);cx.lineTo(25,-24);cx.closePath();cx.fill();
  cx.fillStyle='#66deed';cx.fillRect(-13,-55,6,3);
  cx.restore();
  cx.fillStyle='#ffffff77';cx.font='12px Arial';cx.fillText('23:59',223,499);
}
function drawLevel(t){
  if(!game)return;
  const zone=STAGES[scene].zone;
  cx.save();
  cx.shadowBlur=0;
  const room=cx.createLinearGradient(0,OY,0,OY+ROWS*TW);
  room.addColorStop(0,'#0b1725');room.addColorStop(1,'#172435');
  cx.fillStyle=room;cx.fillRect(OX,OY,COLS*TW,ROWS*TW);
  for(let y=0;y<ROWS;y++)for(let x=0;x<COLS;x++){
    let px=OX+x*TW,py=OY+y*TW,wall=currentMap[y][x]==='#';
    if(wall){
      cx.fillStyle='#1c2b41';cx.fillRect(px,py,TW,TW);
      cx.fillStyle='#263c51';cx.fillRect(px+2,py+2,TW-4,17);
      cx.fillStyle='#101b30';cx.fillRect(px+5,py+21,TW-10,TW-26);
      cx.fillStyle='#3e6770a0';cx.fillRect(px+2,py+2,TW-4,2);
      if((x+y)%4===0){cx.fillStyle=zone;cx.globalAlpha=.30;cx.fillRect(px+12,py+12,25,3);cx.globalAlpha=1;}
    } else{
      cx.fillStyle=(x+y)%2===0?'#121f2e':'#152435';
      cx.fillRect(px,py,TW-1,TW-1);
      cx.fillStyle='#39546720';cx.fillRect(px+8,py+8,TW-17,1);
      cx.fillRect(px+8,py+TW-9,TW-17,1);
      if((x*7+y*5)%6===0){
        cx.fillStyle='#6c8fa529';cx.fillRect(px+TW-12,py+11,2,9);
      }
    }
  }
  // Atmospheric stripes and moving scanner
  const sy=OY+((t*24)%(ROWS*TW));
  cx.fillStyle=zone;cx.globalAlpha=.05;cx.fillRect(OX,sy,COLS*TW,11);cx.globalAlpha=1;
  // Exit and objects
  for(let o of objects){
    if(!o.active)continue;
    const pul=1+.12*Math.sin(t*4+o.x);
    const clr=(o.type==='E'?'#5cf9ab':o.type==='T'?'#ebba81':o.type==='R'?'#8ad6ff':o.type==='o'?'#f4d28c':zone);
    glow(o.x,o.y,42*pul,clr,.32);
    cx.save();cx.translate(o.x,o.y);
    if(o.type==='E'){
      cx.fillStyle='#173a34';cx.fillRect(-20,-26,40,51);
      cx.strokeStyle='#6affe1';cx.lineWidth=3;cx.strokeRect(-19,-26,38,51);
      cx.fillStyle='#6affe1';cx.fillRect(-8,-19,16,7);
      cx.fillStyle='#d7fff2';cx.font='bold 11px Arial';cx.textAlign='center';cx.fillText('EXIT',0,17);
    }else if(o.type==='T'){
      cx.fillStyle='#242f49';cx.fillRect(-21,-23,42,45);
      cx.fillStyle='#245b5f';cx.fillRect(-16,-18,32,24);
      cx.fillStyle='#8efff1';cx.font='bold 14px monospace';cx.textAlign='center';cx.fillText('>_',0,0);
      cx.fillStyle=clr;cx.fillRect(-8,12,16,3);
    }else if(o.type==='R'){
      cx.fillStyle='#24354c';cx.fillRect(-19,-17,38,34);
      cx.strokeStyle='#7ad9ff';cx.lineWidth=2;cx.strokeRect(-19,-17,38,34);
      cx.fillStyle='#a1e9fd';cx.beginPath();cx.arc(-9,0,7,0,Math.PI*2);cx.fill();
      cx.strokeStyle='#a1e9fd';cx.beginPath();cx.arc(7,0,6,0,Math.PI*2);cx.stroke();
    }else{
      cx.rotate(t*.55);
      cx.fillStyle=clr;cx.shadowColor=clr;cx.shadowBlur=18;
      cx.beginPath();cx.moveTo(0,-13);cx.lineTo(13,0);cx.lineTo(0,13);cx.lineTo(-13,0);cx.closePath();cx.fill();
      cx.shadowBlur=0;cx.fillStyle='#102032';cx.fillRect(-5,-5,10,10);
    }
    cx.restore();
  }
  // Guard fields of vision: walls can block this cone.
  for(let g of guards){
    if(g.stun>0)continue;
    cx.save();cx.translate(g.x,g.y);cx.rotate(g.facing);
    cx.fillStyle=g.alert>20?'#ff587522':'#e6b97916';
    cx.beginPath();cx.moveTo(0,0);cx.arc(0,0,150,-.67,.67);cx.closePath();cx.fill();
    cx.restore();
  }
  // Entities, with shadows and emissive details
  for(let g of guards){
    glow(g.x,g.y,35,g.stun>0?'#7be9fe':'#f0587a',.27);
    cx.save();cx.translate(g.x,g.y);cx.rotate(g.facing);
    cx.fillStyle='#050c17';cx.beginPath();cx.ellipse(0,11,21,10,0,0,Math.PI*2);cx.fill();
    cx.fillStyle=g.stun>0?'#668da0':'#4b354d';
    cx.beginPath();cx.moveTo(22,0);cx.lineTo(-11,-17);cx.lineTo(-19,0);cx.lineTo(-11,17);cx.closePath();cx.fill();
    cx.strokeStyle=g.stun>0?'#84f4ff':'#fa7590';cx.lineWidth=3;cx.stroke();
    cx.fillStyle='#fff';cx.fillRect(3,-4,10,8);
    if(g.stun>0){cx.strokeStyle='#8af8ff';cx.beginPath();cx.arc(0,0,26,0,Math.PI*2);cx.stroke();}
    cx.restore();
  }
  const p=game.player;
  glow(p.x,p.y,44,zone,.22);
  cx.save();cx.translate(p.x,p.y);
  cx.fillStyle='#030815';cx.beginPath();cx.ellipse(1,14,20,10,0,0,Math.PI*2);cx.fill();
  cx.fillStyle='#070f22';
  cx.beginPath();cx.moveTo(-16,-5);cx.lineTo(-18,19);cx.lineTo(16,19);cx.lineTo(15,-5);cx.closePath();cx.fill();
  cx.fillStyle='#31536b';cx.fillRect(-11,-7,22,22);
  cx.fillStyle='#e5e0d2';cx.beginPath();cx.arc(0,-14,11,0,Math.PI*2);cx.fill();
  cx.fillStyle='#0c1e32';cx.fillRect(-9,-18,19,7);
  cx.fillStyle=zone;cx.fillRect(-7,-10,14,4);
  cx.fillStyle='#90c8cf';cx.fillRect(p.dir>0?9:-15,0,6,14);
  if(game.invuln>0){cx.strokeStyle='#8af2fc99';cx.lineWidth=2;cx.beginPath();cx.arc(0,0,26,0,Math.PI*2);cx.stroke();}
  cx.restore();
  if(game.alarm>65){cx.strokeStyle='#f16b8d99';cx.lineWidth=7;cx.strokeRect(OX+4,OY+4,COLS*TW-8,ROWS*TW-8);}
  for(let a of particles){
    cx.fillStyle=a.col;cx.globalAlpha=Math.min(1,a.life);
    cx.fillRect(a.x,a.y,3,3);cx.globalAlpha=1;
  }
  cx.strokeStyle=zone;cx.globalAlpha=.37;cx.lineWidth=3;cx.strokeRect(OX,OY,COLS*TW,ROWS*TW);cx.globalAlpha=1;
  cx.restore();
}
function frame(ts){
  const t=ts/1000;let dt=Math.min(.045,lastTime?(ts-lastTime)/1000:0);lastTime=ts;
  if(hack&&mode==='hack'){hack.phase+=dt*hack.speed*3.3;updateHack();}
  if(mode==='play')update(dt);
  draw(t);requestAnimationFrame(frame);
}
// UI controls
$('startGame').onclick=()=>{sound('click');newGame();};
$('continueGame').onclick=()=>{sound('click');load();};
$('pauseButton').onclick=pauseGame;
$('resumeBtn').onclick=resumeGame;
$('quitBtn').onclick=showMenu;
$('pulseBtn').onclick=pulse;
$('interactBtn').onclick=interact;
$('lockBtn').onclick=lockHack;
$('cancelHack').onclick=()=>{hack=null;$hidden('terminal',true);mode='play';};
$('nextChapter').onclick=()=>{sound('click');$hidden('workshop',true);enterLevel(scene+1,true);};
$('replayBtn').onclick=()=>{newGame();};
$('menuBtn').onclick=showMenu;
const mapKey={
  ArrowUp:'up',ArrowDown:'down',ArrowLeft:'left',ArrowRight:'right',
  KeyW:'up',KeyS:'down',KeyA:'left',KeyD:'right'
};
window.addEventListener('keydown',e=>{
  if(mapKey[e.code]){e.preventDefault();keyState.add(mapKey[e.code]);}
  if(e.repeat)return;
  if(e.code==='KeyE')interact();
  if(e.code==='Space'){e.preventDefault();pulse();}
  if(e.code==='Escape'){if(mode==='play')pauseGame();else if(mode==='pause')resumeGame();}
  if((e.code==='Enter'||e.code==='Space')&&mode==='dialog'){
    const b=$('dialogChoices').querySelector('button:not(:disabled)');if(b)b.click();
  }
});
window.addEventListener('keyup',e=>{if(mapKey[e.code])keyState.delete(mapKey[e.code]);});
document.querySelectorAll('.dir').forEach(b=>{
  const dir=b.dataset.dir;
  b.addEventListener('pointerdown',e=>{e.preventDefault();b.setPointerCapture(e.pointerId);pad.add(dir);});
  b.addEventListener('pointerup',e=>{e.preventDefault();pad.delete(dir);});
  b.addEventListener('pointercancel',()=>pad.delete(dir));
  b.addEventListener('lostpointercapture',()=>pad.delete(dir));
});
window.addEventListener('blur',()=>{keyState.clear();pad.clear();});
document.addEventListener('contextmenu',e=>e.preventDefault());
showMenu();
requestAnimationFrame(frame);
})();