package br.com.folha.notes;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class Documents {
    static final class Doc {
        String id=UUID.randomUUID().toString(),name="Sem título",folder="",paper="lines";
        boolean pdf;
        int count=1,lastPage=0;
        long updated=System.currentTimeMillis();
        final Map<Integer,List<Ink.Stroke>> pages=new HashMap<>();
        List<Ink.Stroke> strokes(int page){return pages.computeIfAbsent(page,k->new ArrayList<>());}
        JSONObject json() throws Exception {
            JSONObject o=new JSONObject();o.put("version",1);o.put("id",id);o.put("name",name);o.put("folder",folder);
            o.put("pdf",pdf);o.put("paper",paper);o.put("count",count);o.put("lastPage",lastPage);o.put("updated",updated);
            JSONObject p=new JSONObject();
            for(Map.Entry<Integer,List<Ink.Stroke>> e:pages.entrySet()) {
                JSONArray strokes=new JSONArray();
                for(Ink.Stroke s:e.getValue()) {
                    JSONObject item=new JSONObject();item.put("color",s.color);item.put("width",s.width);item.put("marker",s.marker);
                    JSONArray points=new JSONArray();
                    for(Ink.Point pt:s.points){JSONArray q=new JSONArray();q.put((double)pt.x);q.put((double)pt.y);q.put((double)pt.pressure);points.put(q);}
                    item.put("points",points);strokes.put(item);
                }
                p.put(String.valueOf(e.getKey()),strokes);
            }
            o.put("pages",p);return o;
        }
        static Doc parse(String text) throws Exception {
            JSONObject o=new JSONObject(text);Doc d=new Doc();d.id=o.getString("id");d.name=o.getString("name");
            d.folder=o.optString("folder","");d.paper=o.optString("paper","lines");d.pdf=o.optBoolean("pdf");d.count=o.getInt("count");
            d.lastPage=Math.max(0,Math.min(d.count-1,o.optInt("lastPage",0)));d.updated=o.optLong("updated",0);
            JSONObject p=o.optJSONObject("pages");if(p==null)return d;
            Iterator<String> keys=p.keys();while(keys.hasNext()) {
                String key=keys.next();JSONArray a=p.getJSONArray(key);List<Ink.Stroke> list=d.strokes(Integer.parseInt(key));
                for(int i=0;i<a.length();i++) {
                    JSONObject s=a.getJSONObject(i);Ink.Stroke stroke=new Ink.Stroke(s.getInt("color"),(float)s.getDouble("width"),s.getBoolean("marker"));
                    JSONArray points=s.getJSONArray("points");for(int j=0;j<points.length();j++){JSONArray q=points.getJSONArray(j);stroke.points.add(new Ink.Point((float)q.getDouble(0),(float)q.getDouble(1),(float)q.getDouble(2)));}
                    list.add(stroke);
                }
            }
            return d;
        }
    }
    private final File root;
    Documents(Context context){root=new File(context.getFilesDir(),"documents");root.mkdirs();}
    File dir(String id){File f=new File(root,id);f.mkdirs();return f;}
    File pdf(Doc d){return new File(dir(d.id),"source.pdf");}
    void write(String id,String json) throws IOException {
        AtomicFile atom=new AtomicFile(new File(dir(id),"document.json"));FileOutputStream out=null;
        try{out=atom.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));atom.finishWrite(out);}catch(IOException e){if(out!=null)atom.failWrite(out);throw e;}
    }
    List<Doc> load() throws Exception {
        List<Doc> docs=new ArrayList<>();File[] dirs=root.listFiles();if(dirs==null)return docs;
        for(File dir:dirs) {
            if(!dir.isDirectory())continue;
            AtomicFile atom=new AtomicFile(new File(dir,"document.json"));
            if(!atom.getBaseFile().exists()&&!new File(dir,"document.json.bak").exists())continue;
            docs.add(Doc.parse(new String(atom.readFully(),StandardCharsets.UTF_8)));
        }
        docs.sort((a,b)->Long.compare(b.updated,a.updated));return docs;
    }
    static void copy(InputStream in,OutputStream out) throws IOException {byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
}
