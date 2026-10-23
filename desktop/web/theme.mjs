// Device-local preference. Switching chrome never renders, saves or changes a notebook.
export function setupTheme(doc=document,env=window){
 const key='folha.appearance',valid=v=>['system','light','dark'].includes(v),media=env.matchMedia('(prefers-color-scheme: dark)');
 let mode='system';try{const saved=env.localStorage.getItem(key);if(valid(saved))mode=saved;}catch{}
 const controls=['themeLibrary','themeEditor'].map(id=>doc.getElementById(id)).filter(Boolean);
 function apply(){const dark=mode==='dark'||(mode==='system'&&media.matches);doc.documentElement.dataset.theme=dark?'dark':'light';for(const c of controls)c.value=mode;env.chrome?.webview?.postMessage({appearance:dark?'dark':'light'});}
 function set(value){if(!valid(value))return;mode=value;try{env.localStorage.setItem(key,value);}catch{}apply();}
 for(const c of controls)c.onchange=()=>set(c.value);
 media.addEventListener('change',()=>{if(mode==='system')apply();});
 env.addEventListener('storage',e=>{if(e.key===key){mode=valid(e.newValue)?e.newValue:'system';apply();}});
 apply();return{set,get:()=>mode};
}
