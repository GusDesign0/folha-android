// Catalog metadata and copies are supplied by the native host; no network is used.
export function setupContents({rpc,open,home,fail}){
 const $=id=>document.getElementById(id);let busy=false,epoch=0;
 function selected(value){$('contentButton').classList.toggle('selected',value);$('libraryButton').classList.toggle('selected',!value);$('contentButton').setAttribute('aria-current',value?'page':'false');$('libraryButton').setAttribute('aria-current',value?'false':'page');}
 async function show(){
  await home();const token=++epoch;$('library').hidden=true;$('contents').hidden=false;selected(true);
  $('contentStatus').textContent='Carregando materiais…';$('contentCards').replaceChildren();
  try{const items=await rpc('contentList');if(token!==epoch)return;
   for(const item of items){
    const card=document.createElement('article');card.className='contentCard';const image=document.createElement('img');image.src='./content/'+item.cover;image.alt='Capa: '+item.title;image.loading='lazy';image.width=160;image.height=226;
    const info=document.createElement('div'),category=document.createElement('span'),title=document.createElement('h2'),description=document.createElement('p'),meta=document.createElement('p'),button=document.createElement('button');
    category.className='eyebrow';category.textContent=item.category;title.textContent=item.title;description.textContent=item.description;meta.className='contentMeta';meta.textContent=`${item.pages} páginas · ${(item.bytes/1e6).toLocaleString('pt-BR',{maximumFractionDigits:1})} MB · ${item.license}`;
    button.className='primary';button.textContent=item.state==='added'?'Abrir na biblioteca':item.state==='deleted'?'Restaurar e abrir':'Adicionar e abrir';button.onclick=async()=>{
     if(busy)return;busy=true;const label=button.textContent;button.disabled=true;button.textContent='Preparando…';
     try{const d=await rpc('contentAdd',{id:item.id});if(token!==epoch)return;hide();await open(d.id);}catch(e){if(token===epoch)fail(e);}finally{busy=false;button.disabled=false;button.textContent=label;}
    };info.append(category,title,description,meta,button);card.append(image,info);$('contentCards').append(card);
   }$('contentStatus').textContent='Incluídos no app · disponíveis offline';
  }catch(e){$('contentStatus').textContent='Não foi possível carregar os materiais.';fail(e);}
 }
 function hide(){epoch++;$('contents').hidden=true;selected(false);}
 $('contentButton').onclick=()=>show().catch(fail);
 return {show,hide};
}
