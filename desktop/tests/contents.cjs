const fs=require('fs'),path=require('path'),http=require('http'),assert=require('assert/strict');
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'../web'),assets=path.resolve(__dirname,'../../assets/content'),out=path.resolve(__dirname,'ui-output/contents'),catalog=JSON.parse(fs.readFileSync(path.join(assets,'catalog.json'),'utf8')).items,docs=[];fs.mkdirSync(out,{recursive:true});
const server=http.createServer(async(req,res)=>{try{
 if(req.url==='/__test/rpc'){
  let raw='';for await(const b of req)raw+=b;const m=JSON.parse(raw),p=m.data;let result;
  switch(m.command){
   case 'init':result={library:docs,cloud:{connected:false,message:'Funciona offline.'}};break;
   case 'list':result=docs;break;case 'close':result=true;break;
   case 'contentList':result=catalog.map(e=>({...e,state:docs.find(d=>d.id===e.documentId)?.deleted?'deleted':docs.some(d=>d.id===e.documentId)?'added':'available'}));break;
   case 'contentAdd':{const e=catalog.find(e=>e.id===p.id);let d=docs.find(d=>d.id===e.documentId);if(!d){d={version:2,id:e.documentId,name:e.title,folder:'Conteúdos · Japonês',pdf:true,count:e.pages,paper:'blank',pages:{},lastPage:0,updated:Date.now()};docs.push(d);}d.deleted=false;result=d;break;}
   case 'open':result=docs.find(d=>d.id===p.id);break;
   case 'patch':{const d=docs.find(d=>d.id===p.id);if(p.op==='meta')Object.assign(d,p);else if(p.op==='append')(d.pages[p.page]??=[]).push(p.stroke);else if(p.op==='page')d.pages[p.page]=p.strokes;result=true;break;}
   default:throw Error('RPC '+m.command);
  }res.setHeader('Content-Type','application/json');res.end(JSON.stringify({request:m.request,result}));return;
 }
 const urlPath=decodeURIComponent(new URL(req.url,'http://localhost').pathname);const file=urlPath.startsWith('/content/')?path.resolve(assets,urlPath.slice(9)):path.resolve(root,'.'+urlPath);if(!file.startsWith(root+path.sep)&&!file.startsWith(assets+path.sep))throw Error('Invalid path');
 res.setHeader('Content-Type',file.endsWith('.mjs')||file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.png')?'image/png':file.endsWith('.svg')?'image/svg+xml':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(fs.readFileSync(file));
}catch(e){res.statusCode=500;res.end(String(e));}});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,...(process.platform==='win32'?{channel:'msedge'}:{})});const page=await browser.newPage({viewport:{width:1280,height:960}}),errors=[];page.on('pageerror',e=>errors.push(String(e)));
 await page.route('https://folha-data.local/data/**',route=>{const id=new URL(route.request().url()).pathname.split('/')[2],entry=catalog.find(e=>e.documentId===id);return route.fulfill({status:200,contentType:'application/pdf',path:path.join(assets,entry.file),headers:{'Access-Control-Allow-Origin':'*'}});});
 await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);await page.locator('#contentButton').click();await page.locator('.contentCard').first().waitFor();assert.equal(await page.locator('.contentCard').count(),2);
 await page.waitForFunction(()=>[...document.querySelectorAll('.contentCard img')].every(i=>i.complete&&i.naturalWidth>0));assert(await page.locator('#contentButton').evaluate(e=>e.classList.contains('selected')));assert(!(await page.locator('#libraryButton').evaluate(e=>e.classList.contains('selected'))));
 await page.selectOption('#themeLibrary','light');await page.screenshot({path:out+'/conteudos-claro.png'});await page.selectOption('#themeLibrary','dark');await page.screenshot({path:out+'/conteudos-escuro.png'});
 await page.setViewportSize({width:880,height:760});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Content overflows at small window');await page.screenshot({path:out+'/conteudos-compacto.png'});
 await page.setViewportSize({width:1280,height:960});await page.locator('.contentCard button').first().click();await page.waitForFunction(()=>document.getElementById('pageLabel').textContent==='Página 1 de 238');await page.waitForFunction(()=>document.getElementById('save').textContent==='Salvo neste PC');
 await page.locator('[data-tool=text]').click();await page.locator('#live').click({position:{x:120,y:120}});await page.locator('#textValue').fill('Minha anotação no material');await page.locator('#textForm button.primary').click();await page.locator('#back').click();
 await page.locator('#contentButton').click();await page.locator('.contentCard button').first().waitFor();assert.equal(await page.locator('.contentCard button').first().textContent(),'Abrir na biblioteca');await page.locator('.contentCard button').first().click();await page.waitForFunction(()=>document.getElementById('pageLabel').textContent==='Página 1 de 238');assert.equal(docs.length,1);assert.equal(docs[0].pages[0][0].text,'Minha anotação no material');
 await page.locator('#contentButton').click();await page.locator('.contentCard').first().waitFor();await page.locator('.contentCard button').nth(1).click();await page.waitForFunction(()=>document.getElementById('pageLabel').textContent==='Página 1 de 53');assert.equal(docs.length,2);
 await page.locator('#libraryButton').click();await page.locator('#library').waitFor({state:'visible'});assert.equal(await page.locator('.card').count(),2);assert.equal(await page.locator('#contents').isVisible(),false);assert.equal(errors.length,0,errors.join('\n'));
 console.log('PASS actual UI: two covers; light/dark/small viewport; real PDF.js 238/53 pages; add/reopen retains text; no duplicate; library navigation; no JS errors.');
}finally{await browser?.close();server.close();}})().catch(e=>{console.error(e);process.exitCode=1;server.close();});
