package br.com.folha.notes;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Bundled PDFs are immutable. Only the user's library copy receives annotations. */
final class ContentCatalog {
    static List<JSONObject> items(Context context) throws Exception {
        String text;
        try(InputStream in=context.getAssets().open("content/catalog.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Documents.copy(in,out);text=new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
        JSONObject catalog=new JSONObject(text);if(catalog.getInt("version")!=1)throw new IOException("Catálogo incompatível");
        ArrayList<JSONObject> list=new ArrayList<>();JSONArray entries=catalog.getJSONArray("items");
        for(int i=0;i<entries.length();i++){
            JSONObject e=entries.getJSONObject(i);if(!DriveSync.validId(e.getString("documentId"))||!safe(e.getString("file"))||!safe(e.getString("cover"))||e.getInt("pages")<1||!e.getString("sha256").matches("[a-f0-9]{64}"))throw new IOException("Conteúdo inválido");list.add(e);
        }return list;
    }
    static boolean safe(String name){return name.matches("[a-z0-9-]+\\.(pdf|png)");}
    static Documents.Doc find(List<Documents.Doc> docs,String id){
        Documents.Doc deleted=null,copy=null;
        for(Documents.Doc d:docs)if(d.id.equals(id)||d.originId.equals(id)){
            if(!d.deleted&&d.id.equals(id))return d;
            if(!d.deleted)copy=d;else deleted=d;
        }return copy!=null?copy:deleted;
    }
    static Documents.Doc add(Context context,Documents store,JSONObject entry) throws Exception {
        synchronized(store){
            Documents.Doc found=find(store.load(),entry.getString("documentId"));
            if(found!=null){Documents.Doc d=store.read(found.id);if(d.deleted){d.deleted=false;d.updated=System.currentTimeMillis();d.revision=UUID.randomUUID().toString();store.write(d);}return d;}
            Documents.Doc d=new Documents.Doc();d.id=entry.getString("documentId");d.name=entry.getString("title");d.pdf=true;d.paper="blank";d.folder="Conteúdos · Japonês";d.count=entry.getInt("pages");
            File target=store.pdf(d),temp=new File(target.getParentFile(),"content.tmp");
            try {
                try(InputStream in=context.getAssets().open("content/"+entry.getString("file"));FileOutputStream out=new FileOutputStream(temp)){Documents.copy(in,out);out.getFD().sync();}
                if(temp.length()!=entry.getLong("bytes")||!DriveSync.hashFile(temp).equals(entry.getString("sha256"))||PdfFiles.count(temp)!=d.count)throw new IOException("Material incompleto. Atualize o app e tente novamente.");
                // A missing metadata file can leave an interrupted import; preserve its source as a backup.
                if(target.exists())Files.move(target.toPath(),new File(target.getParentFile(),"source-interrupted-"+System.currentTimeMillis()+".pdf").toPath());
                Files.move(temp.toPath(),target.toPath());store.write(d);return d;
            } finally {if(temp.exists()&&!temp.delete())temp.deleteOnExit();}
        }
    }
    static void show(MainActivity a){
        ScrollView scroll=new ScrollView(a);LinearLayout body=a.column();body.setPadding(a.dp(24),a.dp(20),a.dp(24),a.dp(24));scroll.addView(body);a.root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(a.text("Conteúdos",30,MainActivity.INK,true));a.gap(body,7);body.addView(a.text("Materiais para aprender no seu ritmo.",15,MainActivity.MUTED,false));a.gap(body,12);
        body.addView(a.text("Incluídos no app · disponíveis offline",12,MainActivity.GREEN,true));a.gap(body,20);
        try {for(JSONObject entry:items(a)){
            LinearLayout card=a.row();card.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));card.setBackground(a.shape(Color.WHITE,16));
            ImageView image=new ImageView(a);try(InputStream in=a.getAssets().open("content/"+entry.getString("cover"))){image.setImageBitmap(BitmapFactory.decodeStream(in));}image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Capa: "+entry.getString("title"));card.addView(image,new LinearLayout.LayoutParams(a.dp(108),a.dp(153)));a.gap(card,18);
            LinearLayout info=a.column();info.addView(a.text(entry.getString("category").toUpperCase(Locale.ROOT),11,MainActivity.GREEN,true));a.gap(info,7);info.addView(a.text(entry.getString("title"),21,MainActivity.INK,true));a.gap(info,8);info.addView(a.text(entry.getString("description"),14,MainActivity.MUTED,false));a.gap(info,8);info.addView(a.text(entry.getInt("pages")+" páginas · "+String.format(Locale.forLanguageTag("pt-BR"),"%.1f MB",entry.getLong("bytes")/1000000.0)+" · "+entry.getString("license"),11,MainActivity.MUTED,false));a.gap(info,14);
            Documents.Doc found=find(a.library,entry.getString("documentId"));String label=found==null?"Adicionar e abrir":found.deleted?"Restaurar e abrir":"Abrir na biblioteca";
            info.addView(a.button(label,true,()->a.openContent(entry)));card.addView(info,new LinearLayout.LayoutParams(0,-2,1));body.addView(card);a.gap(body,18);
        }}catch(Exception e){body.addView(a.text("Não foi possível carregar os materiais. Atualize o app para reparar o catálogo.",15,MainActivity.MUTED,false));android.util.Log.e("Folha","Catálogo",e);}
        a.gap(body,12);body.addView(a.text("Ao adicionar, você cria sua cópia para anotar. Ela aparece na biblioteca e pode sincronizar com sua conta. Reabrir mantém suas anotações. Créditos e fontes estão no final de cada PDF.",13,MainActivity.MUTED,false));
    }
}
