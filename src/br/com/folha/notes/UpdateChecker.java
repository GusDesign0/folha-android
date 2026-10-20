package br.com.folha.notes;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class UpdateChecker {
    static final String RELEASES="https://github.com/GusDesign0/folha-android/releases/latest";
    static final class Result {String version,url,notes;boolean newer;}
    static Result check(String current) throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL("https://api.github.com/repos/GusDesign0/folha-android/releases/latest").openConnection();conn.setConnectTimeout(8000);conn.setReadTimeout(10000);conn.setRequestProperty("Accept","application/vnd.github+json");conn.setRequestProperty("User-Agent","Folha-Android/"+current);
        try{if(conn.getResponseCode()!=200)throw new IOException("Não foi possível consultar o GitHub");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(bytes.size()+n>1048576)throw new IOException("Resposta inválida");bytes.write(b,0,n);}}
            JSONObject json=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));Result r=new Result();r.version=json.getString("tag_name").replaceFirst("^v","");r.newer=compare(r.version,current)>0;r.url=RELEASES;r.notes=json.optString("body","");JSONArray assets=json.optJSONArray("assets");
            if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);String url=a.optString("browser_download_url","");if(a.optString("name").equals("Folha.apk")&&url.startsWith("https://github.com/GusDesign0/folha-android/releases/download/"))r.url=url;}
            return r;
        }finally{conn.disconnect();}
    }
    static int compare(String a,String b){String[] aa=a.split("[.-]"),bb=b.split("[.-]");for(int i=0;i<3;i++){int x=i<aa.length?Integer.parseInt(aa[i]):0,y=i<bb.length?Integer.parseInt(bb[i]):0;if(x!=y)return Integer.compare(x,y);}return 0;}
}
