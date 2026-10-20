package br.com.folha.notes;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends Activity {
    static final int BG=0xFFF7F6F2,INK=0xFF263C32,GREEN=0xFF326B56,MUTED=0xFF77857C,BORDER=0xFFDFE4DD;
    static ExecutorService background(String name){return Executors.newSingleThreadExecutor(r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},name));}
    final ExecutorService worker=background("folha-save"),renderWorker=background("folha-render"),fileWorker=background("folha-files");
    final ConcurrentHashMap<String,Documents.Doc> pendingWrites=new ConcurrentHashMap<>();
    final Handler main=new Handler(Looper.getMainLooper());
    final ArrayList<Documents.Doc> library=new ArrayList<>();
    Documents store;
    AppUpdater updater;
    Documents.Doc current;
    Documents.Doc exportSnapshot;
    LinearLayout root;
    NotebookView canvas;
    TextView saveLabel,pageLabel,zoomLabel,documentTitle;
    final ArrayList<Icon> toolIcons=new ArrayList<>();
    String folderFilter="Todas";
    int page=0,tool=0,color=INK,generation=0,saveVersion=0;
    float width=2.2f;
    String pen="fountain",imageTarget="";
    int imagePage=0;
    float smoothing=1,sensitivity=1;
    boolean partialEraser=true;
    boolean touchInk=false,loading=false;
    final Runnable delayedSave=()->saveCurrent();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new Documents(this);updater=new AppUpdater(this);
        PdfFiles.init(this);
        SharedPreferences prefs=getSharedPreferences("pens",0);width=prefs.getFloat("width",2.2f);pen=prefs.getString("pen","fountain");smoothing=prefs.getFloat("smoothing",1);sensitivity=prefs.getFloat("sensitivity",1);partialEraser=prefs.getBoolean("partial",true);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->navigateBack());
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showLoading("Abrindo sua biblioteca…");
        worker.execute(()->{try{List<Documents.Doc> docs=store.load();main.post(()->{library.addAll(docs);
            String restore=state==null?null:state.getString("document");Documents.Doc found=null;for(Documents.Doc d:library)if(d.id.equals(restore))found=d;
            if(found!=null)open(found);else home();
        });}catch(Exception e){main.post(()->{home();error("Não foi possível ler a biblioteca. Os arquivos locais foram preservados.",e);});}});
    }
    @Override protected void onSaveInstanceState(Bundle state){saveCurrent();if(current!=null)state.putString("document",current.id);super.onSaveInstanceState(state);}
    @Override protected void onPause(){super.onPause();if(canvas!=null)canvas.finishInput();saveCurrent();}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(current==null&&!loading)home();}
    @Override protected void onDestroy(){main.removeCallbacks(delayedSave);if(updater!=null)updater.close();if(canvas!=null)canvas.dispose();worker.shutdown();renderWorker.shutdown();fileWorker.shutdown();super.onDestroy();}
    void navigateBack(){if(loading){generation++;loading=false;home();}else if(current!=null){leaveEditor();home();}else finish();}
    @Override public void onBackPressed(){navigateBack();}
    int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    void gap(LinearLayout l,int px){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(dp(px),dp(px)));}
    void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(BORDER);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    void base(){
        root=column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();
    }
    TextView button(String label,boolean primary,Runnable click){TextView b=text(label,14,primary?Color.WHITE:INK,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(18),dp(12),dp(18),dp(12));b.setBackground(shape(primary?GREEN:0xFFE8EDE7,12));b.setOnClickListener(v->click.run());b.setContentDescription(label);b.setMinHeight(dp(48));return b;}
    void showLoading(String message){base();TextView t=text(message,18,GREEN,true);t.setGravity(Gravity.CENTER);root.addView(t,new LinearLayout.LayoutParams(-1,-1));}
    void home(){
        current=null;canvas=null;base();toolIcons.clear();
        LinearLayout header=row();header.setPadding(dp(28),dp(22),dp(28),dp(18));
        Icon logo=new Icon("leaf");logo.setBackground(shape(GREEN,13));header.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));gap(header,12);
        LinearLayout brand=column();brand.addView(text("folha",28,INK,true));brand.addView(text("Espaço para pensar.",12,MUTED,false));header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button("Atualizar app",false,()->checkUpdates()));TextView version=text("  v"+appVersion(),11,MUTED,true);header.addView(version);root.addView(header);line(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout body=column();body.setPadding(dp(28),dp(28),dp(28),dp(28));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(text("Sua biblioteca",30,INK,true));gap(body,6);body.addView(text("Ideias à mão. Tudo no seu tablet.",15,MUTED,false));gap(body,24);
        HorizontalScrollView actionScroll=new HorizontalScrollView(this);actionScroll.setHorizontalScrollBarEnabled(false);LinearLayout actions=row();actions.addView(button("+ Novo caderno",true,()->createNotebook()));gap(actions,10);actions.addView(button("Importar PDF",false,()->importPdf()));gap(actions,10);actions.addView(button("Restaurar backup",false,()->restoreBackup()));actionScroll.addView(actions);body.addView(actionScroll);gap(body,24);
        TreeSet<String> folders=new TreeSet<>();for(Documents.Doc d:library)if(!d.folder.isEmpty())folders.add(d.folder);
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();ArrayList<String> all=new ArrayList<>();all.add("Todas");all.addAll(folders);
        for(String f:all){TextView chip=button(f,folderFilter.equals(f),()->{folderFilter=f;home();});chips.addView(chip);gap(chips,8);}filters.addView(chips);body.addView(filters);gap(body,24);
        library.sort((a,b)->Long.compare(b.updated,a.updated));
        ArrayList<Documents.Doc> visible=new ArrayList<>();for(Documents.Doc d:library)if(folderFilter.equals("Todas")||folderFilter.equals(d.folder))visible.add(d);
        if(visible.isEmpty()) {
            LinearLayout empty=column();empty.setPadding(dp(28),dp(30),dp(28),dp(30));empty.setBackground(shape(0xFFECF0E9,18));
            TextView e=text("Sua próxima ideia começa aqui.",22,INK,true);empty.addView(e);gap(empty,12);
            empty.addView(text("Crie um caderno ou importe uma apostila em PDF.\nUse a S Pen para escrever e os dedos para navegar.",15,MUTED,false));gap(empty,22);
            empty.addView(button("Criar meu primeiro caderno",true,()->createNotebook()));body.addView(empty);
        } else {
            int available=getResources().getConfiguration().screenWidthDp-56;
            int columns=Math.max(1,Math.min(4,available/210));
            for(int start=0;start<visible.size();start+=columns){LinearLayout gridRow=new LinearLayout(this);gridRow.setOrientation(LinearLayout.HORIZONTAL);
                for(int i=0;i<columns;i++){
                    if(i>0)gap(gridRow,14);LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);
                    if(start+i<visible.size())gridRow.addView(card(visible.get(start+i)),cell);else gridRow.addView(new View(this),cell);
                }
                body.addView(gridRow);gap(body,20);
            }
        }
        gap(body,28);body.addView(text("SALVO NO TABLET   ·   SEM CONTA   ·   FUNCIONA OFFLINE",10,MUTED,true));
    }
    View card(Documents.Doc d){
        LinearLayout card=column();card.setPadding(dp(12),dp(12),dp(12),dp(14));card.setBackground(shape(Color.WHITE,16));
        Cover cover=new Cover(d);card.addView(cover,new LinearLayout.LayoutParams(-1,dp(180)));gap(card,12);
        TextView title=text(d.name,17,INK,true);title.setMaxLines(2);card.addView(title);gap(card,4);
        card.addView(text((d.pdf?"PDF":"CADERNO")+"  ·  "+d.count+(d.count==1?" página":" páginas"),11,MUTED,true));
        if(!d.folder.isEmpty()){gap(card,4);card.addView(text(d.folder,12,GREEN,false));}
        card.setOnClickListener(v->open(d));card.setOnLongClickListener(v->{editDetails(d);return true;});card.setContentDescription("Abrir "+d.name);return card;
    }
    void createNotebook(){
        LinearLayout box=column();box.setPadding(dp(24),dp(8),dp(24),dp(8));EditText name=new EditText(this);name.setSingleLine();name.setHint("Nome do caderno");box.addView(name);
        EditText folder=new EditText(this);folder.setSingleLine();folder.setHint("Pasta (opcional)");box.addView(folder);gap(box,12);box.addView(text("Tipo de papel",13,MUTED,true));
        Spinner papers=new Spinner(this);papers.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Pautado","Quadriculado","Pontilhado","Em branco"}));box.addView(papers);
        new AlertDialog.Builder(this).setTitle("Novo caderno").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Criar",(dialog,which)->{
            Documents.Doc d=new Documents.Doc();d.name=name.getText().toString().trim();if(d.name.isEmpty())d.name="Meu caderno";d.folder=folder.getText().toString().trim();d.paper=new String[]{"lines","grid","dots","blank"}[papers.getSelectedItemPosition()];
            library.add(d);open(d);saveCurrent();
        }).show();
    }
    void editDetails(Documents.Doc d){
        LinearLayout box=column();box.setPadding(dp(24),dp(8),dp(24),dp(8));EditText name=new EditText(this);name.setText(d.name);name.setSingleLine();box.addView(name);
        EditText folder=new EditText(this);folder.setText(d.folder);folder.setSingleLine();folder.setHint("Pasta (opcional)");box.addView(folder);
        new AlertDialog.Builder(this).setTitle("Nome e pasta").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(dialog,w)->{
            String title=name.getText().toString().trim();if(!title.isEmpty())d.name=title;d.folder=folder.getText().toString().trim();d.updated=System.currentTimeMillis();persist(d);
            if(current==null)home();else documentTitle.setText(d.name);
        }).show();
    }
    void importPdf(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,100);}
    void open(Documents.Doc d){if(current!=null)leaveEditor();
        if(!d.loaded){loading=true;showLoading("Abrindo caderno…");final int token=++generation;worker.execute(()->{try{Documents.Doc full=store.read(d.id);main.post(()->{if(token!=generation||isDestroyed())return;loading=false;for(int i=0;i<library.size();i++)if(library.get(i).id.equals(full.id)){library.set(i,full);break;}current=full;page=full.lastPage;editor();});}catch(Exception e){main.post(()->{loading=false;home();error("Não foi possível abrir o caderno.",e);});}});return;}
        current=d;page=d.lastPage;editor();}
    void leaveEditor(){if(canvas!=null){canvas.finishInput();saveCurrent();canvas.dispose();canvas=null;}if(current!=null)for(int i=0;i<library.size();i++)if(library.get(i).id.equals(current.id)){library.set(i,current.light());break;}generation++;current=null;}
    void editor(){
        base();toolIcons.clear();LinearLayout top=row();top.setPadding(dp(10),dp(8),dp(14),dp(8));
        top.addView(icon("back","Biblioteca",()->{leaveEditor();home();}));gap(top,6);
        LinearLayout names=column();documentTitle=text(current.name,18,INK,true);documentTitle.setSingleLine();documentTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);names.addView(documentTitle);
        saveLabel=text("Salvo no tablet",11,MUTED,false);names.addView(saveLabel);names.setOnClickListener(v->editDetails(current));top.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(icon("export","Exportar PDF",()->exportPdf()));top.addView(icon("more","Opções e ajuda",()->options()));root.addView(top);line(root);
        HorizontalScrollView toolbar=new HorizontalScrollView(this);toolbar.setHorizontalScrollBarEnabled(false);LinearLayout tools=row();tools.setPadding(dp(12),dp(8),dp(12),dp(8));
        String[] symbols={"pen","marker","eraser","hand","lasso"},labels={"Caneta","Marca-texto","Borracha: segure para escolher o tipo","Mover página","Laço: selecionar, mover e redimensionar"};
        for(int i=0;i<5;i++){final int t=i;Icon b=icon(symbols[i],labels[i],()->selectTool(t));if(i==0)b.setOnLongClickListener(v->{chooseWidth();return true;});if(i==2)b.setOnLongClickListener(v->{chooseEraser();return true;});toolIcons.add(b);tools.addView(b);gap(tools,4);}gap(tools,12);
        tools.addView(icon("undo","Desfazer",()->canvas.undo()));tools.addView(icon("redo","Refazer",()->canvas.redo()));gap(tools,18);
        tools.addView(icon("selectmenu","Copiar, colar e editar seleção",()->selectionOptions()));tools.addView(icon("text","Inserir texto",()->textBox(null)));tools.addView(icon("image","Inserir imagem",()->insertImage()));gap(tools,12);
        int[] colors={INK,0xFF3562B7,0xFFB9564A,0xFF8C63B4,0xFFE6B848,0xFF56A792};
        for(int c:colors){View swatch=new View(this);GradientDrawable drawable=shape(c,20);drawable.setStroke(dp(c==color?3:1),c==color?Color.WHITE:BG);swatch.setBackground(drawable);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(32),dp(32));lp.setMargins(dp(5),0,dp(5),0);tools.addView(swatch,lp);swatch.setContentDescription("Cor "+String.format("#%06X",c&0xFFFFFF));swatch.setOnClickListener(v->{color=c;canvas.color=c;if(tool==4)canvas.selection.recolor(c);for(int k=0;k<tools.getChildCount();k++){View ch=tools.getChildAt(k);if(ch.getTag() instanceof Integer){int cc=(Integer)ch.getTag();GradientDrawable gd=shape(cc,20);gd.setStroke(dp(cc==color?3:1),cc==color?Color.WHITE:BG);ch.setBackground(gd);}}});swatch.setTag(c);
        }
        gap(tools,12);tools.addView(icon("width","Tipo de caneta, espessura e suavização",()->chooseWidth()));toolbar.addView(tools);root.addView(toolbar);line(root);
        LinearLayout.LayoutParams pageParams=new LinearLayout.LayoutParams(-1,0,1);
        canvas=new NotebookView(this,current.strokes(page),current.paper,new NotebookView.Listener(){public void changed(){scheduleSave();}public void zoom(float z){if(zoomLabel!=null)zoomLabel.setText(Math.round(z*100)+"%");}});
        canvas.color=color;canvas.penWidth=width;canvas.pen=pen;canvas.smoothing=smoothing;canvas.sensitivity=sensitivity;canvas.partialEraser=partialEraser;canvas.touchInk=touchInk;canvas.tool=tool;canvas.ready=!current.pdf;root.addView(canvas,pageParams);
        LinearLayout bottom=row();bottom.setPadding(dp(12),dp(5),dp(12),dp(5));
        bottom.addView(icon("prev","Página anterior",()->changePage(-1)));pageLabel=text((page+1)+" / "+current.count,13,INK,true);pageLabel.setPadding(dp(6),0,dp(6),0);pageLabel.setOnClickListener(v->jump());bottom.addView(pageLabel);bottom.addView(icon("next","Próxima página",()->changePage(1)));
        if(!current.pdf)bottom.addView(icon("plus","Adicionar página",()->{canvas.finishInput();saveCurrent();current.count++;page=current.count-1;current.lastPage=page;canvas.dispose();generation++;editor();saveCurrent();}));
        TextView hint=text("S Pen escreve · dedos navegam",11,MUTED,false);hint.setGravity(Gravity.CENTER);bottom.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        zoomLabel=text("100%",12,GREEN,true);zoomLabel.setGravity(Gravity.CENTER);zoomLabel.setMinWidth(dp(50));zoomLabel.setMinHeight(dp(48));zoomLabel.setOnClickListener(v->canvas.fit());bottom.addView(zoomLabel);root.addView(bottom);selectTool(tool);
        if(current.pdf)loadPdfPage();
        loadImages();
    }
    void selectTool(int t){if(canvas!=null){canvas.finishInput();if(t!=4)canvas.selection.clear();canvas.tool=t;}tool=t;for(int i=0;i<toolIcons.size();i++){toolIcons.get(i).selected=i==t;toolIcons.get(i).invalidate();}}
    Icon icon(String symbol,String label,Runnable action){Icon v=new Icon(symbol);v.setContentDescription(label);v.setTooltipText(label);v.setOnClickListener(w->action.run());v.setLayoutParams(new LinearLayout.LayoutParams(dp(46),dp(46)));return v;}
    void chooseWidth(){canvas.finishInput();LinearLayout box=column();box.setPadding(dp(24),dp(10),dp(24),dp(10));Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Esferográfica","Tinteiro","Pincel"}));type.setSelection(pen.equals("ballpoint")?0:pen.equals("brush")?2:1);box.addView(type);
        SeekBar weight=setting(box,"Espessura",70,Math.round((width-0.5f)*10));SeekBar pressure=setting(box,"Sensibilidade à pressão",160,Math.round((sensitivity-0.4f)*100));SeekBar smooth=setting(box,"Suavização",100,Math.round(smoothing*100));
        new AlertDialog.Builder(this).setTitle("Sua caneta").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{pen=new String[]{"ballpoint","fountain","brush"}[type.getSelectedItemPosition()];width=0.5f+weight.getProgress()/10f;sensitivity=0.4f+pressure.getProgress()/100f;smoothing=smooth.getProgress()/100f;canvas.pen=pen;canvas.penWidth=width;canvas.sensitivity=sensitivity;canvas.smoothing=smoothing;getSharedPreferences("pens",0).edit().putString("pen",pen).putFloat("width",width).putFloat("sensitivity",sensitivity).putFloat("smoothing",smoothing).apply();}).show();}
    SeekBar setting(LinearLayout box,String label,int max,int value){box.addView(text(label,13,MUTED,true));SeekBar seek=new SeekBar(this);seek.setMax(max);seek.setProgress(value);box.addView(seek);return seek;}
    void chooseEraser(){new AlertDialog.Builder(this).setTitle("Tipo de borracha").setSingleChoiceItems(new String[]{"Parcial: só onde encostar","Traço inteiro"},partialEraser?0:1,(d,w)->{partialEraser=w==0;canvas.partialEraser=partialEraser;getSharedPreferences("pens",0).edit().putBoolean("partial",partialEraser).apply();selectTool(2);d.dismiss();}).show();}
    void loadPdfPage(){
        final int token=++generation;final Documents.Doc d=current;final int index=page;final NotebookView target=canvas;
        renderWorker.execute(()->{try{PdfFiles.Rendered image=PdfFiles.render(store.pdf(d),index,2400);main.post(()->{if(token!=generation||canvas!=target||isDestroyed()){image.bitmap.recycle();return;}target.setPdf(image);});}
        catch(Exception|OutOfMemoryError e){main.post(()->{if(token==generation)error("Não foi possível abrir esta página do PDF.",e);});}});
    }
    void loadImages(){final NotebookView target=canvas;final File dir=store.dir(current.id);final List<Ink.Stroke> objects=new ArrayList<>(current.strokes(page));boolean has=false;for(Ink.Stroke s:objects)if(s.kind.equals("image"))has=true;if(!has)return;
        renderWorker.execute(()->{try{Map<String,Bitmap> images=MediaFiles.page(dir,objects,1600);main.post(()->{if(canvas!=target||isDestroyed())MediaFiles.recycle(images);else target.setImages(images);});}catch(Exception|OutOfMemoryError e){main.post(()->{if(canvas==target)error("Não foi possível carregar uma imagem desta página.",e);});}});
    }
    void selectionOptions(){
        String[] items={"Copiar seleção","Colar neste caderno","Duplicar seleção","Apagar seleção","Editar texto selecionado"};
        new AlertDialog.Builder(this).setTitle("Seleção").setItems(items,(d,w)->{
            if(w==0){if(canvas.selection.copy(current.id))toast("Copiado. Você pode colar em outra página deste caderno.");else toast("Contorne algo com o laço primeiro.");}
            if(w==1){selectTool(4);if(canvas.selection.paste(current.id))loadImages();else toast("Copie uma seleção deste caderno primeiro.");}
            if(w==2){if(canvas.selection.copy(current.id)){canvas.selection.paste(current.id);loadImages();}else toast("Contorne algo com o laço primeiro.");}
            if(w==3)canvas.selection.delete();
            if(w==4){if(canvas.selection.selected.size()==1&&canvas.selection.selected.get(0).kind.equals("text"))textBox(canvas.selection.selected.get(0));else toast("Selecione uma caixa de texto com o laço.");}
        }).show();
    }
    void textBox(Ink.Stroke existing){
        canvas.finishInput();LinearLayout box=column();box.setPadding(dp(24),dp(8),dp(24),dp(8));EditText input=new EditText(this);input.setHint("Digite seu texto");input.setMinLines(3);input.setMaxLines(7);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(20000)});if(existing!=null)input.setText(existing.text);box.addView(input);
        SeekBar size=setting(box,"Tamanho do texto",52,Math.round((existing==null?24:existing.fontSize)-12));
        new AlertDialog.Builder(this).setTitle(existing==null?"Inserir texto":"Editar texto").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
            String value=input.getText().toString();if(value.trim().isEmpty())return;Ink.Stroke item=existing==null?new Ink.Stroke(color,2,false):existing.copy(false);item.kind="text";item.text=value;item.fontSize=12+size.getProgress();item.color=existing==null?color:existing.color;
            if(existing==null){item.x=45;item.y=55;item.boxW=Math.min(320,canvas.pageW-90);}
            Paint measure=new Paint();measure.setTextSize(item.fontSize);int rows=0;for(String line:value.split("\\n",-1))rows+=Math.max(1,(int)Math.ceil(measure.measureText(line)/Math.max(1,item.boxW)));
            item.boxH=Math.min(canvas.pageH-item.y-20,Math.max(item.fontSize*1.4f,rows*measure.getFontSpacing()*1.1f+8));
            selectTool(4);if(existing==null)canvas.addObject(item);else{canvas.history.before(canvas.strokes);int index=canvas.strokes.indexOf(existing);if(index>=0)canvas.strokes.set(index,item);canvas.selection.select(item);canvas.documentChanged();}
        }).show();
    }
    void insertImage(){canvas.finishInput();imageTarget=current.id;imagePage=page;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,102);}
    void receiveImage(Uri uri){
        if(current==null||!current.id.equals(imageTarget)||page!=imagePage){toast("Volte à página onde iniciou a inserção.");return;}final Documents.Doc doc=current;final int index=page;toast("Preparando imagem…");
        fileWorker.execute(()->{File temp=new File(getCacheDir(),"image-"+UUID.randomUUID());Bitmap bitmap=null;try{
            try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){if(in==null)throw new IOException("Arquivo indisponível");Documents.copy(in,out);}
            bitmap=MediaFiles.decode(temp,2048);
            try{android.media.ExifInterface exif=new android.media.ExifInterface(temp.getAbsolutePath());int orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);Matrix matrix=new Matrix();if(orientation==3)matrix.postRotate(180);if(orientation==6)matrix.postRotate(90);if(orientation==8)matrix.postRotate(270);if(orientation==2)matrix.postScale(-1,1);if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap){bitmap.recycle();bitmap=rotated;}}}catch(IOException ignored){}
            String name=UUID.randomUUID()+(bitmap.hasAlpha()?".png":".jpg");File file=new File(store.assets(doc),name);try(OutputStream out=new FileOutputStream(file)){if(!bitmap.compress(bitmap.hasAlpha()?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,90,out))throw new IOException("Falha ao salvar a imagem");}
            final int imageW=bitmap.getWidth(),imageH=bitmap.getHeight();main.post(()->{if(current!=doc||page!=index||canvas==null)return;Ink.Stroke image=new Ink.Stroke(INK,1,false);image.kind="image";image.asset=name;image.x=45;image.y=60;image.boxW=Math.min(canvas.pageW-90,300);image.boxH=image.boxW*imageH/imageW;if(image.boxH>canvas.pageH-100){image.boxH=canvas.pageH-100;image.boxW=image.boxH*imageW/imageH;}selectTool(4);canvas.addObject(image);loadImages();});
        }catch(Exception|OutOfMemoryError e){main.post(()->error("Não foi possível inserir esta imagem.",e));}finally{if(bitmap!=null)bitmap.recycle();temp.delete();}});
    }
    void backup(){canvas.finishInput();saveCurrent();exportSnapshot=current.copy();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,current.name.replaceAll("[\\\\/:*?\"<>|]","_")+".folha");startActivityForResult(i,103);}
    void restoreBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,104);}
    void receiveBackup(Uri uri){loading=true;showLoading("Restaurando caderno…");fileWorker.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Arquivo indisponível");Documents.Doc doc=Backups.restore(store,getCacheDir(),in);main.post(()->{if(isDestroyed())return;loading=false;library.add(0,doc);open(doc);toast("Backup restaurado com anotações editáveis.");});}catch(Exception|OutOfMemoryError e){main.post(()->{loading=false;home();error("Não foi possível restaurar. Escolha um backup .folha válido.",e);});}});}
    void checkUpdates(){updater.check();}
    String appVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "0.4.0";}}
    void changePage(int delta){int next=page+delta;if(next<0||next>=current.count)return;canvas.finishInput();saveCurrent();canvas.dispose();generation++;page=next;current.lastPage=page;editor();saveCurrent();}
    void jump(){EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setHint("1 a "+current.count);new AlertDialog.Builder(this).setTitle("Ir para a página").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Ir",(d,w)->{try{int n=Integer.parseInt(input.getText().toString());if(n>=1&&n<=current.count)changePage(n-1-page);else toast("Informe uma página entre 1 e "+current.count);}catch(Exception e){toast("Informe o número da página.");}}).show();}
    void scheduleSave(){if(saveLabel!=null)saveLabel.setText("Salvando…");main.removeCallbacks(delayedSave);main.postDelayed(delayedSave,450);}
    void saveCurrent(){main.removeCallbacks(delayedSave);if(current==null)return;current.lastPage=page;current.updated=System.currentTimeMillis();persist(current);}
    void persist(Documents.Doc d){
        d.revision=UUID.randomUUID().toString();final String id=d.id;final Documents.Doc incoming=d.copy();
        pendingWrites.compute(id,(key,previous)->{if(previous!=null&&previous.loaded&&!incoming.loaded){Documents.Doc merged=previous.copy();merged.name=incoming.name;merged.folder=incoming.folder;merged.updated=incoming.updated;merged.revision=incoming.revision;return merged;}return incoming;});
        worker.execute(()->{Documents.Doc snapshot=pendingWrites.remove(id);if(snapshot==null)return;try{
            if(!snapshot.loaded){Documents.Doc full=store.read(id);full.name=snapshot.name;full.folder=snapshot.folder;full.updated=snapshot.updated;full.revision=snapshot.revision;snapshot=full;}
            store.write(snapshot);final String revision=snapshot.revision;main.post(()->{if(current!=null&&current.id.equals(id)&&current.revision.equals(revision)&&saveLabel!=null)saveLabel.setText("Salvo no tablet");});
        }catch(Exception|OutOfMemoryError e){main.post(()->{if(current!=null&&current.id.equals(id)&&saveLabel!=null)saveLabel.setText("Falha ao salvar");error("Não foi possível salvar. Libere espaço no tablet e tente novamente.",e);});}});
    }
    void options(){String[] choices={"Nome e pasta",touchInk?"Desativar escrita com o dedo":"Ativar escrita com o dedo","Backup editável (.folha)","Atualizar app","Como usar","Sobre esta versão"};
        new AlertDialog.Builder(this).setTitle("Seu caderno").setItems(choices,(d,w)->{if(w==0)editDetails(current);if(w==1){touchInk=!touchInk;canvas.touchInk=touchInk;toast(touchInk?"Escrita com o dedo ativada":"Somente a S Pen escreve");}if(w==2)backup();if(w==3)checkUpdates();if(w==4)help();if(w==5)new AlertDialog.Builder(this).setTitle("Folha · versão "+appVersion()).setMessage("App independente de cadernos e anotações.\n\nUse o backup .folha para guardar anotações editáveis, textos, imagens e o PDF original. O PDF exportado preserva o conteúdo original e acrescenta a escrita como caminhos.\n\nOs arquivos ficam neste tablet. Conta, sincronização e aplicativo para Windows estão planejados. OCR e reconhecimento de escrita ainda não estão incluídos.\n\nPDF: PdfBox-Android / Apache License 2.0. Criptografia: Bouncy Castle / licença MIT.").setPositiveButton("Entendi",null).show();}).show();
    }
    void help(){new AlertDialog.Builder(this).setTitle("Feito para escrever").setMessage("• A S Pen escreve. Dedos movem e ampliam.\n\n• Segure o ícone da caneta para escolher esferográfica, tinteiro ou pincel e ajustar a pressão e a suavização.\n\n• Segure a borracha para alternar entre apagar uma parte ou o traço inteiro. O botão lateral da S Pen também apaga.\n\n• Com o laço, contorne o que deseja selecionar. Arraste a seleção para mover e a bolinha do canto para redimensionar. As cores recolorem a seleção.\n\n• Use o ícone de folhas sobrepostas para copiar, colar, duplicar, apagar ou editar uma caixa de texto selecionada. As cópias podem ser coladas em outra página do mesmo caderno.\n\n• Use T para inserir texto e o ícone de paisagem para inserir imagem.\n\n• Exporte um PDF pelo ícone no alto. Para preservar a edição e transferir o caderno, use Backup editável nas opções. Restaure pela biblioteca.").setPositiveButton("Vamos lá",null).show();}
    void exportPdf(){
        if(current==null)return;canvas.finishInput();saveCurrent();
        try{exportSnapshot=current.copy();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,current.name.replaceAll("[\\\\/:*?\"<>|]","_")+" - anotado.pdf");startActivityForResult(intent,101);}catch(Exception e){error("Não foi possível iniciar a exportação.",e);}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==AppUpdater.INSTALL_PERMISSION){updater.permissionResult();return;}if(result!=RESULT_OK||data==null||data.getData()==null){if(request==101||request==103)exportSnapshot=null;return;}Uri uri=data.getData();
        if(request==102){receiveImage(uri);return;}if(request==104){receiveBackup(uri);return;}
        if(request==103&&exportSnapshot!=null){final Documents.Doc snapshot=exportSnapshot;exportSnapshot=null;toast("Criando backup editável…");fileWorker.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Destino indisponível");Backups.export(snapshot,store.dir(snapshot.id),out);main.post(()->toast("Backup salvo. Guarde este arquivo para restaurar em outro aparelho."));}catch(Exception|OutOfMemoryError e){main.post(()->error("Não foi possível criar o backup.",e));}});return;}
        if(request==100){
            if(loading)return;loading=true;String name="PDF importado";
            try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0).replaceFirst("(?i)\\.pdf$","");}catch(Exception ignored){}
            final String title=name;showLoading("Importando PDF…");worker.execute(()->{try{
                Documents.Doc d=new Documents.Doc();d.name=title;d.pdf=true;
                try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(store.pdf(d))){if(in==null)throw new IOException("Arquivo indisponível");Documents.copy(in,out);}
                d.count=PdfFiles.count(store.pdf(d));if(d.count<1)throw new IOException("PDF sem páginas");store.write(d.id,d.json().toString());main.post(()->{loading=false;library.add(0,d);open(d);});
            }catch(Exception|OutOfMemoryError e){main.post(()->{loading=false;home();error("Não foi possível importar. Escolha um PDF válido e sem senha.",e);});}});
        }else if(request==101&&exportSnapshot!=null){
            final Documents.Doc snapshot=exportSnapshot;exportSnapshot=null;toast("Exportando PDF…");
            fileWorker.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Destino indisponível");PdfFiles.export(snapshot,store.pdf(snapshot),out);main.post(()->{if(isDestroyed())return;new AlertDialog.Builder(this).setTitle("PDF exportado").setMessage("Sua cópia com as anotações foi salva no local escolhido.").setPositiveButton("Abrir",(a,b)->{try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("Abra o PDF pelo app Meus Arquivos.");}}).setNegativeButton("Pronto",null).show();});}
            catch(Exception|OutOfMemoryError e){main.post(()->error("Não foi possível exportar o PDF. Verifique o espaço disponível.",e));}});
        }
    }
    void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    void error(String message,Throwable e){android.util.Log.e("Folha",message,e);if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Atenção").setMessage(message).setPositiveButton("OK",null).show();}

    final class Cover extends View {
        final Documents.Doc doc;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Cover(Documents.Doc d){super(MainActivity.this);doc=d;}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(doc.pdf?0xFFEEE9E1:0xFFE4EDE3);c.drawRoundRect(0,0,w,h,dp(10),dp(10),p);
            float cw=Math.min(w*0.65f,h*0.64f),ch=h*0.83f,x=(w-cw)/2,y=(h-ch)/2;
            p.setColor(0x20000000);c.drawRoundRect(x+dp(4),y+dp(5),x+cw+dp(4),y+ch+dp(5),dp(5),dp(5),p);
            p.setColor(doc.pdf?0xFFFAF9F5:GREEN);c.drawRoundRect(x,y,x+cw,y+ch,dp(5),dp(5),p);
            if(doc.pdf){p.setColor(0xFFB9564A);c.drawRoundRect(x+dp(12),y+dp(16),x+dp(49),y+dp(36),dp(4),dp(4),p);p.setColor(Color.WHITE);p.setTextSize(dp(11));p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("PDF",x+dp(19),y+dp(30),p);p.setColor(0xFFDADFD7);p.setStrokeWidth(dp(2));for(int i=0;i<5;i++)c.drawLine(x+dp(13),y+ch*0.43f+i*dp(11),x+cw-dp(13),y+ch*0.43f+i*dp(11),p);
            }else{p.setColor(0xFF24533F);c.drawRect(x+dp(5),y,x+dp(11),y+ch,p);p.setColor(0xFFDEE8D3);p.setStrokeWidth(dp(1));c.drawLine(x+dp(22),y+ch*0.33f,x+cw-dp(15),y+ch*0.33f,p);p.setTextSize(dp(11));p.setTypeface(Typeface.create("sans-serif-medium",0));c.drawText("CADERNO",x+dp(22),y+ch*0.28f,p);p.setTextSize(dp(26));c.drawText("f.",x+dp(22),y+ch*0.77f,p);}
        }
    }
    final class Icon extends View {
        final String symbol;boolean selected;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Icon(String name){super(MainActivity.this);symbol=name;setFocusable(true);setClickable(true);}
        void path(Canvas c,float...xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
        @Override protected void onDraw(Canvas raw){super.onDraw(raw);int save=raw.save();raw.translate((getWidth()-dp(24))/2f,(getHeight()-dp(24))/2f);raw.scale(dp(24)/24f,dp(24)/24f);Canvas c=raw;
            if(selected){p.setColor(0xFFDCE8DC);p.setStyle(Paint.Style.FILL);c.drawRoundRect(-8,-8,32,32,10,10,p);}
            p.setColor(symbol.equals("leaf")?Color.WHITE:selected?GREEN:INK);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            switch(symbol){
                case "back":path(c,14,5,7,12,14,19);break;
                case "prev":path(c,14,7,9,12,14,17);break;
                case "next":path(c,10,7,15,12,10,17);break;
                case "plus":path(c,12,5,12,19);path(c,5,12,19,12);break;
                case "pen":path(c,5,19,7,13,17,3,21,7,11,17,5,19);path(c,15,5,19,9);break;
                case "marker":path(c,5,17,5,14,15,4,20,9,10,19,7,19,5,17);path(c,3,21,13,21);path(c,12,7,17,12);break;
                case "eraser":path(c,4,13,13,4,21,12,12,21,9,21,4,16,4,13);path(c,8,9,16,17);path(c,12,21,22,21);break;
                case "hand":path(c,7,12,7,5,10,5,10,11,10,3,13,3,13,11,13,5,16,5,16,12,16,8,19,8,19,16,16,21,10,21,4,15,4,12,7,14);break;
                case "undo":path(c,8,5,3,10,8,15);path(c,3,10,15,10,19,12,20,15,19,18,17,19);break;
                case "redo":path(c,16,5,21,10,16,15);path(c,21,10,9,10,5,12,4,15,5,18,7,19);break;
                case "export":path(c,7,13,4,13,4,21,20,21,20,13,17,13);path(c,12,15,12,3);path(c,7,8,12,3,17,8);break;
                case "width":p.setStrokeWidth(1);path(c,4,6,20,6);p.setStrokeWidth(2.5f);path(c,4,12,20,12);p.setStrokeWidth(4);path(c,4,19,20,19);break;
                case "lasso":c.drawOval(3,3,21,17,p);path(c,8,16,8,20,12,22,15,20,13,17);break;
                case "selectmenu":c.drawRect(5,4,17,17,p);path(c,9,20,21,20,21,8);break;
                case "text":path(c,4,5,20,5);path(c,12,5,12,21);path(c,8,21,16,21);break;
                case "image":c.drawRect(3,4,21,21,p);c.drawCircle(15,9,2,p);path(c,3,17,9,11,15,17,18,14,21,17);break;
                case "update":c.drawArc(4,4,20,20,35,285,false,p);path(c,20,5,20,12,14,12);break;
                case "more":p.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++)c.drawCircle(12,5+i*7,1.6f,p);break;
                case "leaf":path(c,6,3,16,3,20,7,20,22,5,22,5,3);path(c,9,10,16,10);path(c,9,14,16,14);path(c,9,18,13,18);break;
            }
            p.setStyle(Paint.Style.FILL);raw.restoreToCount(save);
        }
    }
}

