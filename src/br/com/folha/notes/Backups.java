package br.com.folha.notes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Portable .folha ZIP: plain JSON in PDF-point coordinates and relative asset names. */
final class Backups {
    static void export(Documents.Doc doc,File dir,OutputStream target) throws Exception {
        try(ZipOutputStream zip=new ZipOutputStream(target)){
            zip.putNextEntry(new ZipEntry("document.json"));zip.write(doc.json().toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            if(doc.pdf)add(zip,new File(dir,"source.pdf"),"source.pdf");
            TreeSet<String> assets=new TreeSet<>();for(List<Ink.Stroke> page:doc.pages.values())for(Ink.Stroke s:page)if(s.kind.equals("image"))assets.add(s.asset);
            for(String name:assets)add(zip,MediaFiles.asset(dir,name),"assets/"+name);
        }
    }
    private static void add(ZipOutputStream zip,File file,String path) throws IOException {zip.putNextEntry(new ZipEntry(path));try(InputStream in=new FileInputStream(file)){Documents.copy(in,zip);}zip.closeEntry();}
    static Documents.Doc restore(Documents store,File cache,InputStream input) throws Exception {
        File staging=new File(cache,"restore-"+UUID.randomUUID());if(!staging.mkdirs())throw new IOException("Não foi possível criar a pasta temporária");
        try{
            long total=0;int entries=0;HashSet<String> seen=new HashSet<>();
            try(ZipInputStream zip=new ZipInputStream(input)){
                ZipEntry entry;byte[] bytes=new byte[65536];
                while((entry=zip.getNextEntry())!=null){
                    if(++entries>4096)throw new IOException("Backup com arquivos demais");String name=entry.getName();
                    if(entry.isDirectory()){if(!name.equals("assets/"))throw new IOException("Pasta inválida no backup");continue;}
                    if(!seen.add(name)||!(name.equals("document.json")||name.equals("source.pdf")||name.matches("assets/[A-Za-z0-9_-]+\\.(png|jpg|jpeg)")))throw new IOException("Arquivo inválido no backup");
                    File file=new File(staging,name);File parent=file.getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IOException("Falha ao extrair backup");
                    long size=0;try(OutputStream out=new FileOutputStream(file)){int n;while((n=zip.read(bytes))!=-1){size+=n;total+=n;if(total>536870912L||name.equals("document.json")&&size>67108864L)throw new IOException("Backup acima do limite de importação");out.write(bytes,0,n);}}
                }
            }
            File json=new File(staging,"document.json");if(!json.exists())throw new IOException("Backup sem caderno");
            byte[] bytes=java.nio.file.Files.readAllBytes(json.toPath());String content=new String(bytes,StandardCharsets.UTF_8);if(new org.json.JSONObject(content).optInt("version",1)>2)throw new IOException("Backup de uma versão mais recente do Folha");Documents.Doc doc=Documents.Doc.parse(content);UUID.fromString(doc.id);
            if(doc.count<1||doc.count>10000)throw new IOException("Número de páginas inválido");
            if(doc.pdf&&(!new File(staging,"source.pdf").exists()||PdfFiles.count(new File(staging,"source.pdf"))!=doc.count))throw new IOException("PDF original ausente ou incompleto");
            for(Map.Entry<Integer,List<Ink.Stroke>> page:doc.pages.entrySet()){
                if(page.getKey()<0||page.getKey()>=doc.count)throw new IOException("Página inválida");
                for(Ink.Stroke s:page.getValue())if(s.kind.equals("image")&&!MediaFiles.asset(staging,s.asset).isFile())throw new IOException("Imagem ausente no backup");
            }
            if(store.exists(doc.id)){doc.originId=doc.id;doc.id=UUID.randomUUID().toString();doc.name+=" (restaurado)";}
            File target=store.dir(doc.id);
            if(doc.pdf)copy(new File(staging,"source.pdf"),new File(target,"source.pdf"));
            File[] images=new File(staging,"assets").listFiles();if(images!=null){File assets=store.assets(doc);for(File image:images)copy(image,new File(assets,image.getName()));}
            doc.updated=System.currentTimeMillis();doc.revision=UUID.randomUUID().toString();store.write(doc);return doc;
        }finally{deleteStaging(staging);}
    }
    private static void copy(File source,File target) throws IOException {try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(target)){Documents.copy(in,out);}}
    private static void deleteStaging(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)deleteStaging(child);file.delete();}
}
