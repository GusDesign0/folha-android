package br.com.folha.notes;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.widget.*;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

final class CloudSync {
    static final int AUTH=611;
    static final String DRIVE="https://www.googleapis.com/auth/drive.appdata";
    final MainActivity app;final SharedPreferences prefs;final ExecutorService executor=MainActivity.background("folha-sync");
    volatile boolean busy,closed;boolean authorizing;long edited;String message="Os cadernos continuam salvos no tablet.";TextView status;
    final java.util.concurrent.ConcurrentHashMap<String,String> hashes=new java.util.concurrent.ConcurrentHashMap<>();final Map<String,TextView> badges=new HashMap<>();
    String badge(Documents.Doc d){if(d.name.contains("(conflito "))return "Conflito · cópia preservada";if(!prefs.getBoolean("enabled",false))return "Salvo no tablet";boolean clean=!d.localHash.isEmpty()&&d.localHash.equals(hashes.get(d.id));return clean?"Sincronizado":busy?"Enviando / verificando…":"Salvo · envio pendente";}
    void receipts() throws Exception {String sub=prefs.getString("sub","");if(sub.isEmpty())return;File f=new File(new File(new File(app.getFilesDir(),"cloud"),DriveSync.hash(sub.getBytes(StandardCharsets.UTF_8))),"state.json");if(!f.exists())return;JSONObject state=new JSONObject(DriveSync.readText(f));hashes.clear();Iterator<String> it=state.keys();while(it.hasNext()){String id=it.next();hashes.put(id,state.getJSONObject(id).optString("hash"));}}
    final Runnable periodic=this::tick;
    void tick(){if(!closed){if(prefs.getBoolean("enabled",false)&&System.currentTimeMillis()-edited>20000&&(app.canvas==null||!app.canvas.writing()))authorize(false);schedule();}}
    CloudSync(MainActivity app){this.app=app;prefs=app.getSharedPreferences("cloud",0);executor.execute(()->{try{receipts();refresh();}catch(Exception ignored){}});schedule();}
    void schedule(){app.main.postDelayed(periodic,45000);}
    void edited(){edited=System.currentTimeMillis();}
    String summary(){return (prefs.getBoolean("enabled",false)?prefs.getString("email","")+"\n\n":"")+message;}
    void refresh(){app.main.post(()->{if(status!=null)status.setText(summary());for(Documents.Doc d:app.library){TextView v=badges.get(d.id);if(v!=null)v.setText(badge(d));}if(app.syncLabel!=null&&app.current!=null)app.syncLabel.setText(badge(app.current));});}
    void show(){LinearLayout box=app.column();box.setPadding(app.dp(24),app.dp(8),app.dp(24),app.dp(8));status=app.text(summary(),14,MainActivity.INK,false);box.addView(status);app.gap(box,12);TextView note=app.text("Dados privados do Folha no seu Google Drive. PDFs e imagens são enviados uma vez; as alterações seguem em segundo plano. Feche o caderno para receber edições de outro aparelho.\n\nEdições simultâneas geram cópias de conflito. A sincronização funciona enquanto o app está aberto.",12,MainActivity.MUTED,false);box.addView(note);boolean enabled=prefs.getBoolean("enabled",false);AlertDialog.Builder builder=app.dialog().setTitle("Conta e sincronização").setView(box).setNegativeButton("Fechar",null).setPositiveButton(enabled?"Sincronizar agora":"Conectar Google",(d,w)->{app.saveCurrent();authorize(true);});if(enabled)builder.setNeutralButton("Desconectar",(d,w)->{if(busy){app.toast("Aguarde a sincronização terminar.");return;}prefs.edit().putBoolean("enabled",false).apply();message="Conta desconectada. Os cadernos continuam salvos no tablet.";app.toast(message);});AlertDialog dialog=builder.create();dialog.setOnDismissListener(d->status=null);dialog.show();}
    void authorize(boolean interactive){if(closed||busy||authorizing)return;try{app.getPackageManager().getPackageInfo("com.google.android.gms",0);}catch(PackageManager.NameNotFoundException e){message="O login Google requer Google Play Services. A biblioteca e a escrita offline continuam disponíveis.";if(interactive)app.toast(message);refresh();return;}
        authorizing=true;AuthorizationRequest.Builder request=AuthorizationRequest.builder().setRequestedScopes(Arrays.asList(new Scope(DRIVE),new Scope("openid"),new Scope("email")));String email=prefs.getString("email","");if(prefs.getBoolean("enabled",false)&&!email.isEmpty())request.setAccount(new Account(email,"com.google"));
        Identity.getAuthorizationClient(app).authorize(request.build()).addOnSuccessListener(result->{authorizing=false;if(closed)return;if(result.hasResolution()){if(interactive)try{app.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),AUTH,null,0,0,0);}catch(Exception e){failed("Não foi possível abrir o login Google.",e);}else{message="Toque em Conectar Google para renovar a permissão.";refresh();}}else accepted(result);}).addOnFailureListener(e->{authorizing=false;failed("Não foi possível conectar ao Google. Seus cadernos locais estão preservados.",e);});
    }
    void activityResult(int result,Intent data){if(result!=android.app.Activity.RESULT_OK){message="Login cancelado. Você pode continuar usando o Folha offline.";refresh();return;}try{accepted(Identity.getAuthorizationClient(app).getAuthorizationResultFromIntent(data));}catch(Exception e){failed("Não foi possível concluir o login.",e);}}
    void accepted(AuthorizationResult auth){if(app.canvas!=null&&app.canvas.writing()){message="Salvo localmente. A sincronização aguardará a escrita terminar.";refresh();return;}String token=auth.getAccessToken();if(token==null||token.isEmpty()){message="O Google não concedeu acesso ao Drive.";refresh();return;}busy=true;message="Sincronizando em segundo plano…";refresh();executor.execute(()->{try{
        DriveSync.GoogleRemote remote=new DriveSync.GoogleRemote(token);JSONObject user=new JSONObject(DriveSync.GoogleRemote.text(remote.request("https://www.googleapis.com/oauth2/v3/userinfo","GET")));String sub=user.getString("sub"),email=user.optString("email"),owner=prefs.getString("owner","");if(!owner.isEmpty()&&!owner.equals(sub))throw new IOException("Esta biblioteca já está vinculada a outra conta. Reconecte a conta original para preservar a privacidade dos arquivos.");prefs.edit().putString("owner",sub).putString("sub",sub).putString("email",email).putBoolean("enabled",true).commit();
        File root=new File(app.getFilesDir(),"cloud");root.mkdirs();DriveSync.Local local=new DriveSync.Local(){
            public List<String> ids() throws Exception {return app.worker.submit(()->{ArrayList<String> ids=new ArrayList<>();for(Documents.Doc d:app.store.load())ids.add(d.id);return ids;}).get();}
            public JSONObject read(String id) throws Exception {return app.worker.submit(()->app.store.read(id).json()).get();}
            public File file(String id,String rel) throws Exception {if(!DriveSync.validId(id))throw new IOException("Caderno inválido");DriveSync.relative(rel);return new File(app.store.dir(id),rel);}
            public boolean exists(String id){return DriveSync.validId(id)&&app.store.exists(id);}
            public boolean opened(String id){return id.equals(app.editingId);}
            public boolean install(JSONObject json,File stage,String expectedHash) throws Exception {final String id=json.getString("id");return app.worker.submit(()->{if(closed||id.equals(app.editingId)||app.pendingWrites.containsKey(id))return false;boolean exists=app.store.exists(id);if(expectedHash==null&&exists)return false;if(expectedHash!=null&&(!exists||!DriveSync.fingerprint(app.store.read(id).json()).equals(expectedHash)))return false;Documents.Doc d=Documents.Doc.parse(json.toString());File[] files=stage.listFiles();if(files!=null)copyStage(stage,app.store.dir(id));app.store.write(d);return true;}).get();}
        };
        message=new DriveSync(local,remote,root,sub).run();receipts();prefs.edit().putLong("lastSync",System.currentTimeMillis()).apply();app.worker.execute(()->{try{List<Documents.Doc> docs=app.store.load();app.main.post(()->{if(closed)return;app.library.clear();app.library.addAll(docs);if(app.current==null&&!app.loading)app.home();});}catch(Exception ignored){}});
    }catch(Exception e){if(e.getMessage()!=null&&e.getMessage().contains("sessão expirou"))Identity.getAuthorizationClient(app).clearToken(ClearTokenRequest.builder().setToken(token).build());failed("Não foi possível sincronizar. Seus arquivos locais estão preservados.",e);}finally{busy=false;refresh();}});}
    static void copyStage(File source,File target) throws Exception {File[] files=source.listFiles();if(files==null)return;for(File f:files){File to=new File(target,f.getName());if(f.isDirectory()){to.mkdirs();copyStage(f,to);}else{to.getParentFile().mkdirs();Files.copy(f.toPath(),to.toPath(),StandardCopyOption.REPLACE_EXISTING);}}}
    void failed(String fallback,Exception e){String detail=e.getMessage();message=detail!=null&&detail.matches("^(Esta|Sua|Acesso|Arquivo|Versão|Caderno|Drive|Formato|Página|Objeto|Ponto|Caminho|Assinatura|Não).*" )?detail:fallback;android.util.Log.w("FolhaSync",fallback);refresh();}
    void close(){closed=true;app.main.removeCallbacks(periodic);status=null;executor.shutdown();}
}
