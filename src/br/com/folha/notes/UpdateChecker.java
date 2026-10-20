package br.com.folha.notes;

import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.json.*;

/** Network, hashing and APK parsing run only on the updater thread. */
final class UpdateChecker {
    static final String RELEASES="https://github.com/GusDesign0/folha-android/releases/latest";
    static final String DOWNLOADS="https://github.com/GusDesign0/folha-android/releases/download/";
    static final long MAX_APK=100L*1024*1024;
    static final class Result {String version,url,digest;long size;boolean newer;}
    interface Progress {void changed(long bytes,long total);}
    static final class Session {
        volatile boolean canceled;
        private HttpURLConnection connection;
        synchronized void attach(HttpURLConnection c) throws InterruptedIOException {if(canceled){c.disconnect();throw new InterruptedIOException();}connection=c;}
        synchronized void detach(HttpURLConnection c){if(connection==c)connection=null;c.disconnect();}
        synchronized void cancel(){canceled=true;if(connection!=null)connection.disconnect();}
        void check() throws InterruptedIOException {if(canceled||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelado");}
    }
    static HttpURLConnection connect(String url,Session session) throws Exception {
        for(int redirects=0;redirects<6;redirects++){
            session.check();URL target=new URL(url);if(!target.getProtocol().equals("https"))throw new IOException("Endereço de download inválido");
            HttpURLConnection c=(HttpURLConnection)target.openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Folha-Android");c.setRequestProperty("Accept-Encoding","identity");session.attach(c);
            int code;try{code=c.getResponseCode();}catch(Exception e){session.detach(c);throw e;}if(code==200)return c;
            String location=c.getHeaderField("Location");session.detach(c);
            if((code==301||code==302||code==303||code==307||code==308)&&location!=null){url=new URL(target,location).toString();continue;}
            throw new IOException(code==403||code==429?"O GitHub limitou as consultas. Tente novamente mais tarde.":"O GitHub não respondeu ao pedido ("+code+").");
        }
        throw new IOException("Redirecionamentos demais no download");
    }
    static Result check(String current) throws Exception {return check(current,new Session());}
    static Result check(String current,Session session) throws Exception {
        HttpURLConnection c=connect("https://api.github.com/repos/GusDesign0/folha-android/releases/latest",session);
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){session.check();if(bytes.size()+n>1048576)throw new IOException("Resposta inválida");bytes.write(b,0,n);}}
            return parse(new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8)),current);
        }finally{session.detach(c);}
    }
    static Result parse(JSONObject json,String current) throws Exception {
        Result r=new Result();r.version=json.getString("tag_name").replaceFirst("^v","");r.newer=compare(r.version,current)>0;
        if(json.optBoolean("draft")||json.optBoolean("prerelease"))throw new IOException("A versão ainda não está publicada");
        JSONArray assets=json.optJSONArray("assets");
        if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);String url=a.optString("browser_download_url","");
            if(a.optString("name").equals("Folha.apk")&&url.startsWith(DOWNLOADS)&&a.optString("state").equals("uploaded")){r.url=url;r.size=a.optLong("size",0);r.digest=a.optString("digest","");}
        }
        if(r.newer&&(r.url==null||r.size<=0||r.size>MAX_APK||r.digest==null||!r.digest.matches("sha256:[a-fA-F0-9]{64}")))throw new IOException("O instalador desta versão ainda está sendo preparado. Tente novamente em alguns minutos.");
        return r;
    }
    static File download(Context context,Result r,Session session,Progress progress) throws Exception {
        File dir=new File(context.getFilesDir(),"updates");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Não foi possível preparar o download");
        File part=new File(dir,"Folha.part"),ready=UpdateProvider.apk(context);part.delete();
        if(r.url==null||!r.url.startsWith(DOWNLOADS)||r.size<=0||r.size>MAX_APK||r.digest==null||!r.digest.matches("sha256:[a-fA-F0-9]{64}"))throw new IOException("Instalador indisponível");
        HttpURLConnection c=null;boolean complete=false;
        try{
            c=connect(r.url,session);long declared=c.getContentLengthLong();if(declared>0&&declared!=r.size)throw new IOException("Tamanho de download incorreto");
            try(InputStream in=c.getInputStream()){copyVerified(in,part,r.size,r.digest,session,progress);}
            verifyApk(context,part,r.version);session.check();
            if(ready.exists()&&!ready.delete())throw new IOException("Não foi possível substituir o instalador anterior");
            if(!part.renameTo(ready))throw new IOException("Não foi possível guardar o instalador");complete=true;return ready;
        }finally{if(c!=null)session.detach(c);if(!complete)part.delete();}
    }
    static void copyVerified(InputStream in,File target,long size,String digest,Session session,Progress progress) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long bytes=0,last=0;byte[] b=new byte[32768];
        try(FileOutputStream out=new FileOutputStream(target)){int n;while((n=in.read(b))!=-1){session.check();bytes+=n;if(bytes>size||bytes>MAX_APK)throw new IOException("Download maior que o esperado");out.write(b,0,n);hash.update(b,0,n);long now=System.nanoTime();if(now-last>=150000000L){progress.changed(bytes,size);last=now;}}out.getFD().sync();}
        session.check();if(bytes!=size)throw new IOException("Download incompleto. Tente novamente.");
        StringBuilder hex=new StringBuilder();for(byte v:hash.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
        if(!("sha256:"+hex).equalsIgnoreCase(digest))throw new IOException("A verificação do download falhou. Tente novamente.");progress.changed(bytes,size);
    }
    static void verifyApk(Context context,File file,String expectedVersion) throws Exception {
        PackageManager pm=context.getPackageManager();PackageInfo installed=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo apk=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(apk==null||!installed.packageName.equals(apk.packageName)||!expectedVersion.equals(apk.versionName)||apk.getLongVersionCode()<=installed.getLongVersionCode())throw new IOException("O APK não é uma atualização válida do Folha");
        if(apk.signingInfo==null||installed.signingInfo==null||!sameSigners(installed.signingInfo.getApkContentsSigners(),apk.signingInfo.getApkContentsSigners()))throw new IOException("A assinatura do APK não corresponde à versão instalada");
    }
    static boolean sameSigners(Signature[] a,Signature[] b){if(a==null||b==null||a.length==0||a.length!=b.length)return false;for(Signature x:a){boolean found=false;for(Signature y:b)if(Arrays.equals(x.toByteArray(),y.toByteArray()))found=true;if(!found)return false;}return true;}
    static int compare(String a,String b){if(!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!b.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IllegalArgumentException("Versão inválida");String[] aa=a.split("\\."),bb=b.split("\\.");for(int i=0;i<3;i++){int x=Integer.parseInt(aa[i]),y=Integer.parseInt(bb[i]);if(x!=y)return Integer.compare(x,y);}return 0;}
}
