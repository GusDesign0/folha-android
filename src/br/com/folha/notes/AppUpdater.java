package br.com.folha.notes;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/** User-initiated updater, with no polling or work on the pen's thread. */
final class AppUpdater {
    static final int INSTALL_PERMISSION=110;
    final MainActivity activity;
    final ExecutorService worker=MainActivity.background("folha-update");
    final Handler main=new Handler(Looper.getMainLooper());
    UpdateChecker.Session session;
    UpdateChecker.Result pending;
    AlertDialog dialog;
    TextView label;
    ProgressBar bar;
    boolean busy,closed;
    AppUpdater(MainActivity a){activity=a;}
    void check(){
        if(busy){activity.toast("Já estamos verificando a atualização.");return;}
        if(pending!=null&&UpdateProvider.apk(activity).isFile()){ready();return;}
        busy=true;session=new UpdateChecker.Session();final UpdateChecker.Session task=session;String version=activity.appVersion();
        progress("Verificando atualizações",false,task);
        worker.execute(()->{try{UpdateChecker.Result r=UpdateChecker.check(version,task);main.post(()->{if(!active(task))return;busy=false;dismiss();if(!r.newer){dialog=new AlertDialog.Builder(activity).setTitle("Você está atualizado").setMessage("Sua versão: "+version+". Última publicada no GitHub: "+r.version+".").setPositiveButton("OK",null).show();return;}
            dialog=new AlertDialog.Builder(activity).setTitle("Folha "+r.version+" disponível").setMessage("Baixar "+String.format(Locale.ROOT,"%.1f",r.size/1048576f)+" MB do GitHub?\n\nSeus cadernos continuam no tablet. Depois do download, o Android pedirá sua confirmação para atualizar.").setNegativeButton("Agora não",null).setPositiveButton("Baixar e atualizar",(d,w)->download(r)).show();});
        }catch(Exception e){failed(task,e);}});
    }
    boolean active(UpdateChecker.Session task){return !closed&&session==task&&!task.canceled&&!activity.isDestroyed();}
    void progress(String title,boolean determinate,UpdateChecker.Session task){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int pad=activity.dp(24);body.setPadding(pad,pad,pad,pad);
        label=activity.text(determinate?"Preparando download…":"Consultando o GitHub…",15,MainActivity.INK,false);body.addView(label);
        bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setIndeterminate(!determinate);bar.setMax(100);body.addView(bar,new LinearLayout.LayoutParams(-1,activity.dp(16)));
        dialog=new AlertDialog.Builder(activity).setTitle(title).setView(body).setNegativeButton("Cancelar",(d,w)->{task.cancel();busy=false;}).create();dialog.setOnCancelListener(d->{task.cancel();busy=false;});dialog.setCanceledOnTouchOutside(false);dialog.show();
    }
    void download(UpdateChecker.Result r){
        if(busy||closed)return;busy=true;pending=null;session=new UpdateChecker.Session();final UpdateChecker.Session task=session;
        progress("Baixando Folha "+r.version,true,task);
        worker.execute(()->{try{UpdateChecker.download(activity,r,task,(bytes,total)->main.post(()->{if(!active(task))return;label.setText(bytes==total?"Verificando instalador…":String.format(Locale.ROOT,"%.1f de %.1f MB",bytes/1048576f,total/1048576f));bar.setProgress((int)(bytes*100/total));}));
            main.post(()->{if(!active(task))return;busy=false;dismiss();pending=r;ready();});
        }catch(Exception e){failed(task,e);}});
    }
    void ready(){if(pending==null||closed)return;dialog=new AlertDialog.Builder(activity).setTitle("Atualização pronta").setMessage("O download foi verificado. Toque em Instalar para abrir a confirmação do Android.\n\nSe for a primeira atualização pelo Folha, permita instalar apps desta fonte na próxima tela e volte ao app.").setNegativeButton("Depois",null).setPositiveButton("Instalar",(d,w)->install()).show();}
    void install(){
        if(pending==null||closed)return;
        if(!activity.getPackageManager().canRequestPackageInstalls()){
            try{activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())),INSTALL_PERMISSION);}catch(ActivityNotFoundException e){activity.toast("Abra Configurações e permita instalar apps pelo Folha.");}return;
        }
        if(busy)return;busy=true;final UpdateChecker.Result r=pending;
        worker.execute(()->{try{UpdateChecker.verifyApk(activity,UpdateProvider.apk(activity),r.version);main.post(()->{busy=false;if(closed)return;try{activity.startActivity(installIntent());}catch(ActivityNotFoundException|SecurityException e){activity.error("Não foi possível abrir o instalador do Android.",e);}});}catch(Exception e){main.post(()->{busy=false;pending=null;if(!closed)activity.error("O instalador não está mais disponível. Baixe a atualização novamente.",e);});}});
    }
    static Intent installIntent(){Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(UpdateProvider.URI,UpdateProvider.TYPE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("Folha.apk",UpdateProvider.URI));return i;}
    void permissionResult(){if(pending==null||closed)return;if(activity.getPackageManager().canRequestPackageInstalls())install();else activity.toast("Instalação não autorizada. Toque em Atualizar app para tentar novamente.");}
    void failed(UpdateChecker.Session task,Exception e){main.post(()->{if(!active(task))return;busy=false;dismiss();activity.error("Não foi possível atualizar. "+(e.getMessage()==null?"Verifique sua conexão e tente novamente.":e.getMessage()),e);});}
    void dismiss(){if(dialog!=null){dialog.dismiss();dialog=null;}}
    void close(){closed=true;if(session!=null)session.cancel();dismiss();main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
