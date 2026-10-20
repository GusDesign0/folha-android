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
        String revision=UUID.randomUUID().toString(),originId="";
        boolean loaded=true;
        boolean pdf;
        int count=1,lastPage=0;
        long updated=System.currentTimeMillis();
        final Map<Integer,List<Ink.Stroke>> pages=new HashMap<>();
        final TreeSet<Integer> bookmarks=new TreeSet<>();
        List<Ink.Stroke> strokes(int page){return pages.computeIfAbsent(page,k->new ArrayList<>());}
        void movePage(int from,int to){
            if(pdf||from<0||to<0||from>=count||to>=count||from==to)return;
            Map<Integer,List<Ink.Stroke>> next=new HashMap<>();TreeSet<Integer> marks=new TreeSet<>();
            for(int i=0;i<count;i++){int target=i==from?to:from<to&&i>from&&i<=to?i-1:from>to&&i>=to&&i<from?i+1:i;if(pages.containsKey(i))next.put(target,pages.get(i));if(bookmarks.contains(i))marks.add(target);}
            pages.clear();pages.putAll(next);bookmarks.clear();bookmarks.addAll(marks);lastPage=to;
        }
        void duplicatePage(int index){
            if(pdf||index<0||index>=count||count>=10000)return;Map<Integer,List<Ink.Stroke>> next=new HashMap<>();TreeSet<Integer> marks=new TreeSet<>();
            for(Map.Entry<Integer,List<Ink.Stroke>> e:pages.entrySet())next.put(e.getKey()>index?e.getKey()+1:e.getKey(),e.getValue());
            List<Ink.Stroke> copy=new ArrayList<>();for(Ink.Stroke s:strokes(index))copy.add(s.copy(true));next.put(index+1,copy);
            for(int i:bookmarks)marks.add(i>index?i+1:i);if(bookmarks.contains(index))marks.add(index+1);
            pages.clear();pages.putAll(next);bookmarks.clear();bookmarks.addAll(marks);count++;lastPage=index+1;
        }
        void deletePage(int index){
            if(pdf||count<=1||index<0||index>=count)return;Map<Integer,List<Ink.Stroke>> next=new HashMap<>();TreeSet<Integer> marks=new TreeSet<>();
            for(Map.Entry<Integer,List<Ink.Stroke>> e:pages.entrySet())if(e.getKey()!=index)next.put(e.getKey()>index?e.getKey()-1:e.getKey(),e.getValue());for(int i:bookmarks)if(i!=index)marks.add(i>index?i-1:i);
            pages.clear();pages.putAll(next);bookmarks.clear();bookmarks.addAll(marks);count--;lastPage=Math.min(index,count-1);
        }
        JSONObject json() throws Exception {
            JSONObject o=metadata();
            JSONObject p=new JSONObject();
            for(Map.Entry<Integer,List<Ink.Stroke>> e:pages.entrySet()) {
                JSONArray strokes=new JSONArray();
                for(Ink.Stroke s:e.getValue()) strokes.put(strokeJson(s));
                p.put(String.valueOf(e.getKey()),strokes);
            }
            o.put("pages",p);return o;
        }
        JSONObject metadata() throws Exception {
            JSONObject o=new JSONObject();o.put("version",2);o.put("id",id);o.put("name",name);o.put("folder",folder);o.put("revision",revision);o.put("originId",originId);
            o.put("pdf",pdf);o.put("paper",paper);o.put("count",count);o.put("lastPage",lastPage);o.put("updated",updated);
            o.put("bookmarks",new JSONArray(bookmarks));
            return o;
        }
        static JSONObject strokeJson(Ink.Stroke s) throws Exception {
                    JSONObject item=new JSONObject();item.put("color",s.color);item.put("width",s.width);item.put("marker",s.marker);
                    item.put("id",s.id);item.put("kind",s.kind);item.put("pen",s.pen);item.put("smoothing",s.smoothing);item.put("sensitivity",s.sensitivity);
                    item.put("text",s.text);item.put("asset",s.asset);item.put("x",s.x);item.put("y",s.y);item.put("boxW",s.boxW);item.put("boxH",s.boxH);item.put("fontSize",s.fontSize);
                    JSONArray points=new JSONArray();
                    for(Ink.Point pt:s.points){JSONArray q=new JSONArray();q.put((double)pt.x);q.put((double)pt.y);q.put((double)pt.pressure);points.put(q);}
                    item.put("points",points);return item;
        }
        Doc copy(){Doc d=new Doc();d.id=id;d.name=name;d.folder=folder;d.paper=paper;d.pdf=pdf;d.count=count;d.lastPage=lastPage;d.updated=updated;d.revision=revision;d.originId=originId;d.loaded=loaded;d.bookmarks.addAll(bookmarks);for(Map.Entry<Integer,List<Ink.Stroke>> e:pages.entrySet())d.pages.put(e.getKey(),new ArrayList<>(e.getValue()));return d;}
        Doc light(){Doc d=copy();d.pages.clear();d.loaded=false;return d;}
        static Doc parse(String text) throws Exception {
            JSONObject o=new JSONObject(text);Doc d=new Doc();d.id=o.getString("id");d.name=o.getString("name");
            d.folder=o.optString("folder","");d.paper=o.optString("paper","lines");d.pdf=o.optBoolean("pdf");d.count=o.getInt("count");
            d.lastPage=Math.max(0,Math.min(d.count-1,o.optInt("lastPage",0)));d.updated=o.optLong("updated",0);
            d.revision=o.optString("revision",d.revision);d.originId=o.optString("originId","");
            JSONArray marks=o.optJSONArray("bookmarks");if(marks!=null)for(int i=0;i<marks.length();i++){int value=marks.optInt(i,-1);if(value>=0&&value<d.count)d.bookmarks.add(value);}
            JSONObject p=o.optJSONObject("pages");if(p==null)return d;
            Iterator<String> keys=p.keys();while(keys.hasNext()) {
                String key=keys.next();JSONArray a=p.getJSONArray(key);List<Ink.Stroke> list=d.strokes(Integer.parseInt(key));
                for(int i=0;i<a.length();i++) {
                    JSONObject s=a.getJSONObject(i);Ink.Stroke stroke=new Ink.Stroke(s.getInt("color"),(float)s.getDouble("width"),s.getBoolean("marker"));
                    stroke.id=s.optString("id",stroke.id);stroke.kind=s.optString("kind","ink");stroke.pen=s.optString("pen","fountain");stroke.smoothing=(float)s.optDouble("smoothing",1);stroke.sensitivity=(float)s.optDouble("sensitivity",1);
                    stroke.text=s.optString("text","");stroke.asset=s.optString("asset","");stroke.x=(float)s.optDouble("x",0);stroke.y=(float)s.optDouble("y",0);stroke.boxW=(float)s.optDouble("boxW",200);stroke.boxH=(float)s.optDouble("boxH",100);stroke.fontSize=(float)s.optDouble("fontSize",24);
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
    boolean exists(String id){return new File(new File(root,id),"document.json").exists();}
    File pdf(Doc d){return new File(dir(d.id),"source.pdf");}
    File assets(Doc d){File f=new File(dir(d.id),"assets");f.mkdirs();return f;}
    synchronized Doc read(String id) throws Exception {return Doc.parse(new String(new AtomicFile(new File(dir(id),"document.json")).readFully(),StandardCharsets.UTF_8));}
    synchronized void write(Doc d) throws Exception {atomic(new File(dir(d.id),"document.json"),d.json().toString());atomic(new File(dir(d.id),"metadata.json"),d.metadata().toString());}
    synchronized void write(String id,String json) throws IOException {
        atomic(new File(dir(id),"document.json"),json);
        try{Doc d=Doc.parse(json);atomic(new File(dir(id),"metadata.json"),d.metadata().toString());}catch(Exception e){throw new IOException(e);}
    }
    static void atomic(File file,String json) throws IOException {
        AtomicFile atom=new AtomicFile(file);FileOutputStream out=null;
        try{out=atom.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));atom.finishWrite(out);}catch(IOException e){if(out!=null)atom.failWrite(out);throw e;}
    }
    synchronized List<Doc> load() throws Exception {
        List<Doc> docs=new ArrayList<>();File[] dirs=root.listFiles();if(dirs==null)return docs;
        for(File dir:dirs) {
            if(!dir.isDirectory())continue;
            AtomicFile atom=new AtomicFile(new File(dir,"document.json"));
            if(!atom.getBaseFile().exists()&&!new File(dir,"document.json.bak").exists())continue;
            File meta=new File(dir,"metadata.json");Doc d;
            if(meta.exists())d=Doc.parse(new String(new AtomicFile(meta).readFully(),StandardCharsets.UTF_8));
            else{d=Doc.parse(new String(atom.readFully(),StandardCharsets.UTF_8));atomic(meta,d.metadata().toString());}
            docs.add(d.light());
        }
        docs.sort((a,b)->Long.compare(b.updated,a.updated));return docs;
    }
    static void copy(InputStream in,OutputStream out) throws IOException {byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
}
