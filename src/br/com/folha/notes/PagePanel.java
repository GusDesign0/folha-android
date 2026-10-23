package br.com.folha.notes;

import android.app.*;
import android.graphics.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;

/** Recycled grid cells and at most ten cached thumbnails, rendered only on demand. */
final class PagePanel {
    final MainActivity a;
    final Documents.Doc doc,snapshot;
    final ExecutorService worker=MainActivity.background("folha-thumbnails");
    final LruCache<Integer,Bitmap> cache=new LruCache<>(10);
    final Set<Integer> pending=new HashSet<>();
    final Set<Integer> failed=new HashSet<>();
    final List<Integer> visible=new ArrayList<>();
    final GridView grid;
    final LinearLayout body;
    final TextView allFilter,markedFilter;
    final BaseAdapter adapter;
    AlertDialog dialog;
    boolean markedOnly;
    volatile boolean closed;
    PagePanel(MainActivity activity){
        a=activity;a.canvas.finishInput();a.saveCurrent();doc=a.current;snapshot=doc.copy();
        body=a.column();body.setMinimumHeight((int)(a.getResources().getDisplayMetrics().heightPixels*0.88f)-a.dp(128));body.setPadding(a.dp(16),a.dp(4),a.dp(16),a.dp(8));LinearLayout actions=a.row();
        allFilter=a.button("Todas",true,()->filter(false));markedFilter=a.button("Marcadas",false,()->filter(true));actions.addView(allFilter);a.gap(actions,8);actions.addView(markedFilter);a.gap(actions,8);actions.addView(a.button("Ir ao número",false,()->{dialog.dismiss();a.jump();}));body.addView(actions);
        TextView hint=a.text(doc.pdf?"Toque para abrir · segure para marcar uma página do PDF":"Toque para abrir · segure para marcar, mover, duplicar ou excluir",12,MainActivity.MUTED,false);hint.setPadding(0,a.dp(8),0,a.dp(8));body.addView(hint);
        grid=new GridView(a);grid.setNumColumns(Math.max(2,Math.min(4,(a.getResources().getConfiguration().screenWidthDp-80)/175)));grid.setHorizontalSpacing(a.dp(12));grid.setVerticalSpacing(a.dp(12));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        adapter=new BaseAdapter(){
            public int getCount(){return visible.size();}public Object getItem(int pos){return visible.get(pos);}public long getItemId(int pos){return visible.get(pos);}
            public View getView(int pos,View reuse,android.view.ViewGroup parent){
                LinearLayout cell;if(reuse instanceof LinearLayout)cell=(LinearLayout)reuse;else{cell=a.column();cell.setPadding(a.dp(6),a.dp(6),a.dp(6),a.dp(6));ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);cell.addView(image,new LinearLayout.LayoutParams(-1,a.dp(188)));TextView label=a.text("",13,MainActivity.INK,true);label.setGravity(Gravity.CENTER);cell.addView(label,new LinearLayout.LayoutParams(-1,a.dp(32)));}
                int index=visible.get(pos);cell.setTag(index);cell.setBackground(a.shape(index==a.page?0xFFDCE8DC:0xFFF1F3EE,10));((TextView)cell.getChildAt(1)).setText((doc.bookmarks.contains(index)?"★  ":"")+"Página "+(index+1));cell.setContentDescription("Página "+(index+1)+(doc.bookmarks.contains(index)?", marcada":""));ImageView image=(ImageView)cell.getChildAt(0);Bitmap b=cache.get(index);image.setImageBitmap(b);if(b==null)request(index);return cell;
            }
        };grid.setAdapter(adapter);grid.setOnItemClickListener((p,v,pos,id)->{int index=visible.get(pos);dialog.dismiss();a.changePage(index-a.page);});grid.setOnItemLongClickListener((p,v,pos,id)->{menu(visible.get(pos));return true;});
        TextView empty=a.text("Nenhuma página marcada. Segure uma miniatura para marcar.",15,MainActivity.MUTED,false);empty.setGravity(Gravity.CENTER);body.addView(empty,new LinearLayout.LayoutParams(-1,0,1));grid.setEmptyView(empty);body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        dialog=new AlertDialog.Builder(a).setTitle("Páginas · "+doc.count).setView(body).setNegativeButton("Fechar",null).create();dialog.setOnDismissListener(d->close());filter(false);
    }
    void show(){dialog.show();resize();}
    void resize(){if(closed)return;int anchor=grid.getFirstVisiblePosition();grid.setNumColumns(Math.max(2,Math.min(4,(a.getResources().getConfiguration().screenWidthDp-80)/175)));body.setMinimumHeight((int)(a.getResources().getDisplayMetrics().heightPixels*0.88f)-a.dp(128));Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*0.94f),(int)(a.getResources().getDisplayMetrics().heightPixels*0.88f));grid.post(()->{if(!closed)grid.setSelection(anchor);});}
    void filter(boolean marked){markedOnly=marked;allFilter.setBackground(a.shape(marked?0xFFE8EDE7:MainActivity.GREEN,12));allFilter.setTextColor(marked?MainActivity.INK:Color.WHITE);markedFilter.setBackground(a.shape(marked?MainActivity.GREEN:0xFFE8EDE7,12));markedFilter.setTextColor(marked?Color.WHITE:MainActivity.INK);visible.clear();for(int i=0;i<doc.count;i++)if(!marked||doc.bookmarks.contains(i))visible.add(i);adapter.notifyDataSetChanged();}
    void menu(int index){String label=doc.bookmarks.contains(index)?"Remover marcador":"Marcar página";String[] actions=doc.pdf?new String[]{"Abrir página",label}:new String[]{"Abrir página",label,"Mover para…","Duplicar página","Excluir página"};
        new AlertDialog.Builder(a).setTitle("Página "+(index+1)).setItems(actions,(d,which)->{
            if(which==0){dialog.dismiss();a.changePage(index-a.page);}if(which==1){if(!doc.bookmarks.remove(index))doc.bookmarks.add(index);a.saveCurrent();filter(markedOnly);}
            if(which>=2){dialog.dismiss();a.managePage(index,which-2);}
        }).show();
    }
    void request(int index){if(closed||failed.contains(index)||pending.size()>=12||!pending.add(index))return;worker.execute(()->{Bitmap b=null;Exception failure=null;try{if(!closed&&!Thread.currentThread().isInterrupted())b=thumbnail(snapshot,a.store.dir(snapshot.id),index);}catch(Exception e){failure=e;}catch(OutOfMemoryError e){android.util.Log.w("Folha","Miniatura sem memória",e);}final Bitmap result=b;final Exception error=failure;a.main.post(()->{pending.remove(index);if(closed){if(result!=null)result.recycle();return;}if(error!=null)android.util.Log.w("Folha","Não foi possível desenhar a miniatura",error);if(result!=null)cache.put(index,result);else failed.add(index);for(int i=0;i<grid.getChildCount();i++){LinearLayout cell=(LinearLayout)grid.getChildAt(i);int bound=(Integer)cell.getTag();ImageView image=(ImageView)cell.getChildAt(0);if(bound==index&&result!=null)image.setImageBitmap(result);if(image.getDrawable()==null)request(bound);}});});}
    static Bitmap thumbnail(Documents.Doc doc,File dir,int index) throws Exception {
        Bitmap bitmap;float w=Paper.width(doc.paper),h=Paper.height(doc.paper);if(doc.pdf){PdfFiles.Rendered rendered=PdfFiles.render(new File(dir,"source.pdf"),index,256);bitmap=rendered.bitmap;w=rendered.width;h=rendered.height;}else{float scale=Math.min(256/w,256/h);bitmap=Bitmap.createBitmap(Math.round(w*scale),Math.round(h*scale),Bitmap.Config.ARGB_8888);Canvas background=new Canvas(bitmap);background.scale(bitmap.getWidth()/w,bitmap.getHeight()/h);PageArt.paper(background,w,h,doc.paper);}
        Map<String,Bitmap> images=Collections.emptyMap();try{List<Ink.Stroke> strokes=doc.pages.get(index);if(strokes==null)strokes=Collections.emptyList();images=MediaFiles.page(dir,strokes,256);Canvas c=new Canvas(bitmap);c.scale(bitmap.getWidth()/w,bitmap.getHeight()/h);c.clipRect(0,0,w,h);for(Ink.Stroke s:strokes)PageArt.stroke(c,s.copy(false),images);return bitmap;}catch(Exception|OutOfMemoryError e){bitmap.recycle();throw e;}finally{MediaFiles.recycle(images);}
    }
    void close(){if(closed)return;closed=true;worker.shutdownNow();grid.setAdapter(null);cache.evictAll();if(a.pagePanel==this)a.pagePanel=null;}
}
