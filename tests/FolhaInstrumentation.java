package br.com.folha.notes;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.*;

/** Device integration tests. No test hooks are compiled into the app. */
public class FolhaInstrumentation extends Instrumentation {
    MainActivity activity;
    Documents.Doc notebook;
    final List<String> results=new ArrayList<>();
    Bundle options;
    void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    @Override public void onCreate(Bundle args){super.onCreate(args);options=args;start();}
    void ui(Runnable r){final Throwable[] failure={null};runOnMainSync(()->{try{r.run();}catch(Throwable e){failure[0]=e;}});waitForIdleSync();if(failure[0]!=null)throw new AssertionError("UI check failed",failure[0]);}
    void event(NotebookView v,int action,int[] types,int[] ids,float[] xs,float[] ys,float[] pressure,int flags){
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=ids[i];props[i].toolType=types[i];coords[i]=new MotionEvent.PointerCoords();coords[i].x=xs[i];coords[i].y=ys[i];coords[i].pressure=pressure[i];coords[i].size=0.1f;}
        long time=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(time-16,time,action,ids.length,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_STYLUS,flags);v.dispatchTouchEvent(e);e.recycle();
    }
    void pen(NotebookView v,int action,float x,float y,float pressure){event(v,action,new int[]{MotionEvent.TOOL_TYPE_STYLUS},new int[]{7},new float[]{x},new float[]{y},new float[]{pressure},0);}
    @Override public void onStart(){
        Bundle report=new Bundle();try{
            Intent i=new Intent(Intent.ACTION_MAIN);i.setClassName(getTargetContext(),"br.com.folha.notes.MainActivity");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity=(MainActivity)startActivitySync(i);waitForIdleSync();
            // Drain initial asynchronous library loading before starting interaction.
            activity.worker.submit(()->{}).get();waitForIdleSync();
            if("true".equals(options.getString("requireLegacy"))){
                int preserved=0;for(Documents.Doc meta:activity.library){Documents.Doc d=activity.store.read(meta.id);if(d.name.equals("Caderno de teste")&&d.strokes(0).size()>=2)preserved++;}
                check(preserved>0,"Updating the APK must preserve notebooks and raw strokes created by version 0.1.0");results.add("In-place 0.1.0 upgrade preserves notebooks");
            }
            notebook=new Documents.Doc();notebook.name="Caderno de teste";notebook.folder="Integração";
            ui(()->{activity.library.add(notebook);activity.open(notebook);});
            ui(()->{
                NotebookView v=activity.canvas;check(v.getWidth()>0&&v.getHeight()>0,"Editor must have a measured drawing area");
                float x=v.getWidth()/2f,y=v.getHeight()/2f;
                pen(v,MotionEvent.ACTION_DOWN,x-60,y,0.25f);pen(v,MotionEvent.ACTION_MOVE,x,y+10,0.85f);pen(v,MotionEvent.ACTION_UP,x+60,y,1f);
                check(v.strokes.size()==1,"Stylus creates exactly one stroke");check(v.strokes.get(0).points.get(0).pressure<0.5f,"Stylus pressure is preserved");
                results.add("Stylus input with pressure");
                event(v,MotionEvent.ACTION_DOWN,new int[]{MotionEvent.TOOL_TYPE_FINGER},new int[]{0},new float[]{x},new float[]{y},new float[]{1},0);
                event(v,MotionEvent.ACTION_MOVE,new int[]{MotionEvent.TOOL_TYPE_FINGER},new int[]{0},new float[]{x+20},new float[]{y},new float[]{1},0);
                event(v,MotionEvent.ACTION_UP,new int[]{MotionEvent.TOOL_TYPE_FINGER},new int[]{0},new float[]{x+20},new float[]{y},new float[]{1},0);
                check(v.strokes.size()==1,"Finger navigation must not create ink");v.fit();results.add("Finger input cannot write by default");
                pen(v,MotionEvent.ACTION_DOWN,x-30,y+60,1);pen(v,MotionEvent.ACTION_MOVE,x+30,y+70,1);pen(v,MotionEvent.ACTION_CANCEL,x+30,y+70,1);
                check(v.strokes.size()==1,"Canceled stroke must not be committed");results.add("ACTION_CANCEL rolls back ink");
                pen(v,MotionEvent.ACTION_DOWN,x-30,y+90,1);
                event(v,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{2,1},new int[]{7,8},new float[]{x-30,x+100},new float[]{y+90,y+100},new float[]{1,1},0);
                event(v,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{2,1},new int[]{7,8},new float[]{x-30,x+100},new float[]{y+90,y+100},new float[]{1,1},MotionEvent.FLAG_CANCELED);
                pen(v,MotionEvent.ACTION_UP,x+30,y+90,1);check(v.strokes.size()==2,"Canceling a palm pointer must preserve active stylus stroke");results.add("Canceled palm pointer preserves stylus");
                v.undo();check(v.strokes.size()==1,"Undo native input");v.redo();check(v.strokes.size()==2,"Redo native input");results.add("Editor undo/redo");
                activity.saveCurrent();
            });
            activity.worker.submit(()->{}).get();
            Documents.Doc loaded=activity.store.read(notebook.id);
            check(loaded!=null&&loaded.strokes(0).size()==2,"Persisted strokes survive reopening");results.add("Atomic local save and JSON reload");
            File pdf=new File(getTargetContext().getCacheDir(),"integration.pdf");
            try(OutputStream out=new FileOutputStream(pdf)){PdfFiles.export(loaded,activity.store.pdf(loaded),out);}
            check(PdfFiles.count(pdf)==1,"Notebook PDF export has one page");PdfFiles.Rendered rendered=PdfFiles.render(pdf,0,1200);check(rendered.bitmap.getWidth()>0,"Exported PDF renders");rendered.bitmap.recycle();results.add("Notebook PDF export and render");
            Documents.Doc imported=new Documents.Doc();imported.name="PDF de teste";imported.pdf=true;imported.count=1;
            try(InputStream in=new FileInputStream(pdf);OutputStream out=new FileOutputStream(activity.store.pdf(imported))){Documents.copy(in,out);}
            ui(()->{activity.library.add(imported);activity.open(imported);});activity.renderWorker.submit(()->{}).get();waitForIdleSync();
            ui(()->check(activity.canvas.ready&&activity.canvas.pdf!=null,"Imported PDF opens in editor"));
            File annotated=new File(getTargetContext().getCacheDir(),"annotated.pdf");
            Ink.Stroke mark=new Ink.Stroke(0xFF3562B7,3,false);mark.points.add(new Ink.Point(40,40,1));mark.points.add(new Ink.Point(180,90,1));imported.strokes(0).add(mark);
            try(OutputStream out=new FileOutputStream(annotated)){PdfFiles.export(imported,activity.store.pdf(imported),out);}check(PdfFiles.count(annotated)==1,"Annotated imported PDF exports");results.add("PDF import, annotation and re-export");
            qualityArtifacts();results.add("Smooth ink and PDF-path example generated");
            featureChecks();
            newToolsChecks();
            rotationChecks();
            trashChecks();
            updaterChecks();
            ui(()->{activity.leaveEditor();activity.home();});report.putString("stream","\nPASS: "+String.join("; ",results)+"\n");finish(-1,report);
        }catch(Throwable e){report.putString("stream","\nFAIL: "+e+"\n"+android.util.Log.getStackTraceString(e));finish(0,report);}
    }
    void chord(NotebookView v,int kind){
        int[] types={MotionEvent.TOOL_TYPE_FINGER,MotionEvent.TOOL_TYPE_FINGER};int[] ids={7,19};float x=v.getWidth()/2f,y=v.getHeight()/2f;
        event(v,MotionEvent.ACTION_DOWN,new int[]{1},new int[]{7},new float[]{x-70},new float[]{y},new float[]{1},0);
        event(v,MotionEvent.ACTION_POINTER_DOWN|(1<<8),types,ids,new float[]{x-70,x+70},new float[]{y,y},new float[]{1,1},0);
        if(kind==1){event(v,MotionEvent.ACTION_MOVE,types,ids,new float[]{x-140,x+140},new float[]{y,y},new float[]{1,1},0);event(v,MotionEvent.ACTION_MOVE,types,ids,new float[]{x-70,x+70},new float[]{y,y},new float[]{1,1},0);}
        if(kind==2){event(v,MotionEvent.ACTION_CANCEL,types,ids,new float[]{x-70,x+70},new float[]{y,y},new float[]{1,1},0);return;}
        if(kind==3){event(v,MotionEvent.ACTION_POINTER_DOWN|(2<<8),new int[]{1,1,1},new int[]{7,19,23},new float[]{x-70,x+70,x},new float[]{y,y,y+90},new float[]{1,1,1},0);event(v,MotionEvent.ACTION_POINTER_UP|(2<<8),new int[]{1,1,1},new int[]{7,19,23},new float[]{x-70,x+70,x},new float[]{y,y,y+90},new float[]{1,1,1},0);}
        event(v,MotionEvent.ACTION_POINTER_UP|(1<<8),types,ids,new float[]{x-70,x+70},new float[]{y,y},new float[]{1,1},kind==4?MotionEvent.FLAG_CANCELED:0);
        event(v,MotionEvent.ACTION_UP,new int[]{1},new int[]{7},new float[]{x-70},new float[]{y},new float[]{1},0);
    }
    void newToolsChecks() throws Exception {
        Documents.Doc doc=new Documents.Doc();doc.name="Formas, régua e páginas";doc.count=3;
        ui(()->{activity.library.add(doc);activity.open(doc);NotebookView v=activity.canvas;for(int k=0;k<3;k++){Ink.Stroke s=new Ink.Stroke(0xFF3562B7,3,false);s.points.add(new Ink.Point(50,60+k*25,1));s.points.add(new Ink.Point(200,60+k*25,1));v.addObject(s);}v.selection.clear();
            chord(v,0);check(v.strokes.size()==2,"Stationary two-finger chord undoes one edit");v.redo();check(v.strokes.size()==3,"Two-finger undo supports redo");
            for(int k=1;k<=4;k++){chord(v,k);check(v.strokes.size()==3,"Pinch, cancel, third finger and canceled palm must never undo: "+k);}v.fit();v.touchInk=true;chord(v,0);check(v.strokes.size()==2,"Two-finger tap cancels optional finger ink before undoing");v.touchInk=false;v.redo();
        });results.add("Native two-finger undo, redo, pinch out/back, third finger, ACTION_CANCEL, palm flag and finger-ink cancellation");
        ui(()->{NotebookView v=activity.canvas;float y=v.getHeight()*0.4f,x=v.getWidth()*0.45f;pen(v,0,x,y,1);for(int k=1;k<=20;k++)pen(v,2,x+k*6,y+(k%2==0?0:0.5f),1);});SystemClock.sleep(680);ui(()->{NotebookView v=activity.canvas;float x=v.getWidth()*0.45f+120,y=v.getHeight()*0.4f;pen(v,1,x,y,1);Ink.Stroke s=v.strokes.get(v.strokes.size()-1);check(s.points.size()==2&&s.pen.equals("ballpoint"),"Holding a native stylus line adjusts it before commit");});
        ui(()->{NotebookView v=activity.canvas;float x=v.getWidth()*0.40f,y=v.getHeight()*0.55f,rx=75,ry=50;pen(v,0,x+rx,y,1);for(int k=1;k<=80;k++){double angle=k*Math.PI*2/80;pen(v,2,x+rx*(float)Math.cos(angle),y+ry*(float)Math.sin(angle),1);}});SystemClock.sleep(680);ui(()->{NotebookView v=activity.canvas;pen(v,1,v.getWidth()*0.40f+75,v.getHeight()*0.55f,1);check(v.strokes.get(v.strokes.size()-1).points.size()==81,"Holding a closed ellipse snaps to a smooth oval");v.setRuler(true);v.ruler.x1=80;v.ruler.y1=100;v.ruler.x2=420;v.ruler.y2=340;float x=v.getWidth()*0.45f,y=v.getHeight()*0.4f;pen(v,0,x,y,0.5f);pen(v,2,x+60,y+80,1);pen(v,1,x+90,y+65,1);Ink.Stroke s=v.strokes.get(v.strokes.size()-1);check(s.points.size()==2,"Ruler keeps only the two endpoints");for(Ink.Point p:s.points)check(Math.abs((p.x-80)*240-(p.y-100)*340)<0.02,"Ink lies on a rotated ruler");v.setRuler(false);activity.saveCurrent();});
        activity.worker.submit(()->{}).get();Documents.Doc disk=activity.store.read(doc.id);check(disk.strokes(0).size()==6,"Adjusted shapes and ruler ink survive persistence");
        Ink.Stroke first=disk.strokes(0).get(0);disk.strokes(1).add(first.copy(true));disk.strokes(2).add(first.copy(true));String originalId=disk.strokes(0).get(0).id;disk.bookmarks.add(0);disk.bookmarks.add(2);disk.movePage(0,2);check(disk.strokes(2).get(0).id.equals(originalId)&&disk.bookmarks.equals(new TreeSet<>(Arrays.asList(1,2))),"Reordering moves ink and markers together");disk.duplicatePage(1);check(disk.count==4&&!disk.strokes(1).get(0).id.equals(disk.strokes(2).get(0).id)&&disk.bookmarks.contains(2),"Duplicated page gets independent object identities and marker");disk.deletePage(2);check(disk.count==3&&disk.strokes(2).get(0).id.equals(originalId),"Deleting a page preserves remaining content order");
        Documents.Doc roundtrip=Documents.Doc.parse(disk.json().toString());check(roundtrip.bookmarks.equals(disk.bookmarks),"Page markers survive JSON");ByteArrayOutputStream bytes=new ByteArrayOutputStream();Backups.export(disk,activity.store.dir(disk.id),bytes);Documents.Doc restored=Backups.restore(activity.store,activity.getCacheDir(),new ByteArrayInputStream(bytes.toByteArray()));check(restored.bookmarks.equals(disk.bookmarks)&&restored.strokes(2).size()==6,"Backup round-trip preserves page order, markers and adjusted shapes");
        Bitmap thumb=PagePanel.thumbnail(disk,activity.store.dir(disk.id),2);check(thumb.getWidth()<=256&&thumb.getHeight()<=256,"Notebook thumbnails use a bounded pixel size");thumb.recycle();
        Documents.Doc pdfDoc=new Documents.Doc();pdfDoc.pdf=true;pdfDoc.count=3;try(InputStream in=new FileInputStream(new File(activity.getExternalFilesDir(null),"fixture-preservar.pdf"));OutputStream out=new FileOutputStream(activity.store.pdf(pdfDoc))){Documents.copy(in,out);}pdfDoc.bookmarks.add(2);thumb=PagePanel.thumbnail(pdfDoc,activity.store.dir(pdfDoc.id),2);check(thumb.getWidth()<=256&&thumb.getHeight()<=256,"Rotated/cropped PDF thumbnail renders");thumb.recycle();pdfDoc.movePage(0,1);pdfDoc.duplicatePage(0);pdfDoc.deletePage(0);check(pdfDoc.count==3,"PDF structural operations leave original order intact");
        ui(()->{activity.leaveEditor();activity.open(restored);activity.showPages();});activity.pagePanel.worker.submit(()->{}).get();waitForIdleSync();ui(()->{check(activity.pagePanel.grid.getChildCount()>0&&activity.pagePanel.grid.getHeight()>100,"Thumbnail grid is visible and measured");check(activity.pagePanel.cache.size()<=10&&activity.pagePanel.pending.size()<=12,"Thumbnail cache and queue are bounded");});
        SystemClock.sleep(250);Bitmap proof=getUiAutomation().takeScreenshot();if(proof!=null){try(OutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"folha-v05-pages.png"))){proof.compress(Bitmap.CompressFormat.PNG,100,out);}proof.recycle();}
        final PagePanel closing=activity.pagePanel;ui(()->{closing.filter(true);check(closing.adapter.getCount()==restored.bookmarks.size(),"Bookmarks filter shows marked pages only");closing.dialog.dismiss();});ui(()->check(closing.worker.isShutdown()&&activity.pagePanel==null,"Closing thumbnails stops the worker and releases the full notebook snapshot"));
        results.add("Held line/oval, rotated ruler, page move/duplicate/delete, persistent bookmarks, backup and bounded lazy thumbnails (PDF included)");
    }
    String shell(String command) throws Exception {try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command));ByteArrayOutputStream bytes=new ByteArrayOutputStream()){Documents.copy(in,bytes);return bytes.toString("UTF-8").trim();}}
    void awaitOrientation(int expected) throws Exception {long until=SystemClock.uptimeMillis()+6000;while(SystemClock.uptimeMillis()<until){if(activity.getResources().getConfiguration().orientation==expected&&((activity.root.getWidth()>activity.root.getHeight())==(expected==android.content.res.Configuration.ORIENTATION_LANDSCAPE))){waitForIdleSync();return;}SystemClock.sleep(100);}throw new AssertionError("Physical display rotation did not resize the app");}
    void rotationChecks() throws Exception {
        // Rotate only the named development emulator; never alter a physical user's settings.
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){results.add("Physical rotation test skipped outside emulator");return;}
        String sensor=shell("settings get system accelerometer_rotation"),rotation=shell("settings get system user_rotation");int original=activity.getWindowManager().getDefaultDisplay().getRotation(),orientation=activity.getResources().getConfiguration().orientation;
        final Documents.Doc doc=activity.current;final NotebookView view=activity.canvas;final int page=activity.page,before=view.strokes.size();
        ui(()->{Ink.Stroke test=new Ink.Stroke(0xFFB9564A,3,false);test.points.add(new Ink.Point(70,70,1));test.points.add(new Ink.Point(220,100,1));view.addObject(test);view.zoom=2;view.panX=60;view.panY=30;view.invalidate();});
        try{shell("settings put system accelerometer_rotation 0");shell("settings put system user_rotation "+((original+1)%4));awaitOrientation(orientation==2?1:2);
            ui(()->{check(activity.current==doc&&activity.canvas==view&&activity.page==page&&view.strokes.size()==before+1,"Rotating keeps the open page and ink without recreating the editor");view.undo();check(view.strokes.size()==before,"Undo history survives rotation");view.redo();check(Float.isFinite(view.panX)&&Float.isFinite(view.panY)&&view.zoom==2,"Viewport remains finite and keeps zoom");activity.showPages();});activity.pagePanel.worker.submit(()->{}).get();waitForIdleSync();SystemClock.sleep(250);
            Bitmap portrait=getUiAutomation().takeScreenshot();if(portrait!=null){try(OutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"folha-v05-retrato.png"))){portrait.compress(Bitmap.CompressFormat.PNG,100,out);}portrait.recycle();}
            final PagePanel panel=activity.pagePanel;shell("settings put system user_rotation "+original);awaitOrientation(orientation);ui(()->{check(activity.pagePanel==panel&&panel.dialog.isShowing(),"Open thumbnails survive rotation");check(panel.grid.getNumColumns()==Math.max(2,Math.min(4,(activity.getResources().getConfiguration().screenWidthDp-80)/175)),"Thumbnail columns adapt to the new width");check(panel.dialog.getWindow().getDecorView().getWidth()<=activity.getResources().getDisplayMetrics().widthPixels,"Thumbnail dialog fits the rotated window");panel.dialog.dismiss();activity.canvas.fit();activity.saveCurrent();});
        }finally{if(activity.pagePanel!=null)ui(()->activity.pagePanel.dialog.dismiss());shell(rotation.matches("[0-3]")?"settings put system user_rotation "+rotation:"settings delete system user_rotation");shell(sensor.matches("[01]")?"settings put system accelerometer_rotation "+sensor:"settings delete system accelerometer_rotation");}
        results.add("Physical emulator rotation: portrait/landscape, same page and ink, undo/redo, zoom and responsive open thumbnails");
    }
    void trashChecks() throws Exception {
        final Documents.Doc light=activity.store.read(notebook.id).light();final int count=activity.store.read(notebook.id).strokes(0).size();
        ui(()->{activity.leaveEditor();light.deleted=true;activity.persist(light);});activity.worker.submit(()->{}).get();
        Documents.Doc trashed=activity.store.read(notebook.id);check(trashed.deleted&&trashed.strokes(0).size()==count,"Trashing light library metadata must preserve all saved strokes");
        check(trashed.copy().deleted&&trashed.light().deleted&&Documents.Doc.parse(trashed.metadata().toString()).deleted,"Deletion state survives copy and metadata reload");
        final Documents.Doc restore=trashed.light();ui(()->{restore.deleted=false;activity.persist(restore);});activity.worker.submit(()->{}).get();
        Documents.Doc restored=activity.store.read(notebook.id);check(!restored.deleted&&restored.strokes(0).size()==count,"Restoring must retain all annotations");results.add("Trash/restore through async metadata saves preserves tablet ink");
    }
    void updaterChecks() throws Exception {
        check(UpdateChecker.compare("0.10.0","0.9.9")>0&&UpdateChecker.compare("1.0.0","0.99.9")>0,"Semantic version ordering");
        String digest="sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        org.json.JSONObject release=new org.json.JSONObject().put("tag_name","v9.0.0").put("assets",new org.json.JSONArray().put(new org.json.JSONObject().put("name","Folha.apk").put("state","uploaded").put("size",3).put("digest",digest).put("browser_download_url",UpdateChecker.DOWNLOADS+"v9.0.0/Folha.apk")));
        check(UpdateChecker.parse(release,"0.4.0").newer,"Published APK with digest is offered");
        release.getJSONArray("assets").getJSONObject(0).remove("digest");boolean rejected=false;try{UpdateChecker.parse(release,"0.4.0");}catch(IOException expected){rejected=true;}check(rejected,"Unfinished release without digest is rejected");
        release.getJSONArray("assets").getJSONObject(0).put("digest",digest).put("browser_download_url","https://example.com/Folha.apk");rejected=false;try{UpdateChecker.parse(release,"0.4.0");}catch(IOException expected){rejected=true;}check(rejected,"APK outside the repository is rejected");
        File sample=new File(activity.getCacheDir(),"update-copy-test");byte[] abc="abc".getBytes("UTF-8");final int[] progress={0};
        UpdateChecker.copyVerified(new ByteArrayInputStream(abc),sample,3,digest,new UpdateChecker.Session(),(n,total)->progress[0]++);check(sample.length()==3&&progress[0]>0,"Verified streaming download reports progress");
        rejected=false;try{UpdateChecker.copyVerified(new ByteArrayInputStream(abc),sample,3,"sha256:"+String.join("",Collections.nCopies(64,"0")),new UpdateChecker.Session(),(n,total)->{});}catch(IOException expected){rejected=true;}check(rejected,"Corrupted download is rejected");
        rejected=false;try{UpdateChecker.copyVerified(new ByteArrayInputStream(abc),sample,4,digest,new UpdateChecker.Session(),(n,total)->{});}catch(IOException expected){rejected=true;}check(rejected,"Truncated download is rejected");
        UpdateChecker.Session canceled=new UpdateChecker.Session();canceled.cancel();rejected=false;try{UpdateChecker.copyVerified(new ByteArrayInputStream(abc),sample,3,digest,canceled,(n,total)->{});}catch(InterruptedIOException expected){rejected=true;}check(rejected,"Cancellation stops download");sample.delete();
        android.content.pm.Signature a=new android.content.pm.Signature(new byte[]{1,2}),b=new android.content.pm.Signature(new byte[]{3,4});
        check(UpdateChecker.sameSigners(new android.content.pm.Signature[]{a},new android.content.pm.Signature[]{a})&&!UpdateChecker.sameSigners(new android.content.pm.Signature[]{a},new android.content.pm.Signature[]{b}),"Mismatched signing certificates are rejected");
        UpdateChecker.Result latest=UpdateChecker.check("0.0.0");boolean shouldDownload=UpdateChecker.compare(latest.version,activity.appVersion())>0;boolean downloaded=false;
        try{UpdateChecker.download(activity,latest,new UpdateChecker.Session(),(n,total)->{});downloaded=true;}catch(IOException e){if(shouldDownload)throw e;check(e.getMessage().contains("atualização válida"),"Old APK reaches native version validation after successful network/hash verification");}
        check(downloaded==shouldDownload,"Real GitHub APK is accepted only when newer than installed");check(!new File(activity.getFilesDir(),"updates/Folha.part").exists(),"Partial APK cleaned after success or failure");
        File ready=UpdateProvider.apk(activity);ready.getParentFile().mkdirs();try(InputStream in=new FileInputStream(activity.getApplicationInfo().sourceDir);OutputStream out=new FileOutputStream(ready)){Documents.copy(in,out);}
        try(InputStream in=activity.getContentResolver().openInputStream(UpdateProvider.URI)){check(in.read()=='P'&&in.read()=='K',"Private APK URI can be read by installer");}
        rejected=false;try{activity.getContentResolver().openFileDescriptor(UpdateProvider.URI,"w");}catch(SecurityException expected){rejected=true;}check(rejected,"Update provider is read-only");
        rejected=false;try{UpdateProvider.requireUri(android.net.Uri.parse("content://br.com.folha.notes.updates/../document.json"));}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Update provider exposes only the single APK");
        Intent install=AppUpdater.installIntent();check(UpdateProvider.URI.equals(install.getData())&&(install.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0&&install.getClipData()!=null,"Installer receives content URI and read grant");ready.delete();
        results.add("Updater: real GitHub APK download and checksum, version/signature checks, cancel, truncated/corrupted downloads, read-only content URI");
    }
    void qualityArtifacts() throws Exception {
        Documents.Doc demo=new Documents.Doc();demo.name="Escrita em caminhos";demo.paper="dots";
        for(int row=0;row<3;row++){
            Ink.Stroke stroke=new Ink.Stroke(row==1?0xFF3562B7:0xFF263C32,row==2?5f:3f,false);
            for(int k=0;k<=52;k++){float t=k/52f;stroke.points.add(new Ink.Point(62+t*460+15*(float)Math.sin(t*12*Math.PI),150+row*145+28*(float)Math.sin(t*12*Math.PI),0.2f+0.95f*(float)Math.pow(Math.sin(t*Math.PI),2)));}
            demo.strokes(0).add(stroke);
        }
        Ink.Stroke marker=new Ink.Stroke(0xFFE6B848,19,true);marker.points.add(new Ink.Point(60,565,1));marker.points.add(new Ink.Point(285,566,1));marker.points.add(new Ink.Point(530,565,1));demo.strokes(0).add(marker);
        Ink.Stroke gradual=new Ink.Stroke(0xFF263C32,7,false);for(int k=0;k<=50;k++){float t=k/50f;gradual.points.add(new Ink.Point(62+t*465,610+8*(float)Math.sin(t*2*Math.PI),0.1f+1.2f*t));}demo.strokes(0).add(gradual);
        File folder=getTargetContext().getExternalFilesDir(null);File output=new File(folder,"folha-caminhos.pdf");try(OutputStream out=new FileOutputStream(output)){PdfFiles.export(demo,activity.store.pdf(demo),out);}
        // Raster QA exercises the same native path renderer used by the editor and PDF export.
        Bitmap preview=Bitmap.createBitmap(1600,1020,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(preview);c.drawColor(0xFFF7F6F2);
        Paint title=new Paint(Paint.ANTI_ALIAS_FLAG);title.setColor(0xFF263C32);title.setTextSize(36);title.setTypeface(Typeface.create("sans-serif-medium",0));
        c.drawText("MESMAS AMOSTRAS DA CANETA",60,65,title);title.setTextSize(25);c.drawText("Antes",60,125,title);c.drawText("Agora",835,125,title);
        for(int panel=0;panel<2;panel++){
            c.save();c.translate(panel==0?25:800,170);c.scale(1.28f,1.28f);
            for(Ink.Stroke s:demo.strokes(0)){
                c.save();c.translate(0,-75);
                if(panel==1)PageArt.stroke(c,s);else legacy(c,s);
                c.restore();
            }
            c.restore();
        }
        try(OutputStream out=new FileOutputStream(new File(folder,"folha-traco-comparacao.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,out);}preview.recycle();
        Bitmap raster=Bitmap.createBitmap(600,240,Bitmap.Config.ARGB_8888);Canvas rc=new Canvas(raster);rc.drawColor(Color.WHITE);
        Ink.Stroke ramp=new Ink.Stroke(Color.BLACK,24,false);for(int k=0;k<100;k++)ramp.points.add(new Ink.Point(20+k*5.5f,120,0.2f+k/100f));PageArt.stroke(rc,ramp);
        for(int x=22;x<560;x++)check(Color.red(raster.getPixel(x,120))<40,"Pressure silhouette must have no seams or holes");raster.recycle();
        ui(()->activity.open(demo));
        ui(()->{
            NotebookView v=activity.canvas;
            for(int z:new int[]{1,3,5}){v.zoom=z;Bitmap screen=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(screen));screen.recycle();}
            v.fit();
        });
    }
    void legacy(Canvas c,Ink.Stroke s){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(s.color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(s.marker){Path path=new Path();Ink.Point a=s.points.get(0);path.moveTo(a.x,a.y);for(int j=1;j<s.points.size();j++)path.lineTo(s.points.get(j).x,s.points.get(j).y);p.setAlpha(76);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s.width);c.drawPath(path,p);}
        else{Ink.Point a=s.points.get(0);c.drawCircle(a.x,a.y,PageArt.width(s,a)/2,p);for(int j=1;j<s.points.size();j++){Ink.Point b=s.points.get(j);p.setStrokeWidth((PageArt.width(s,a)+PageArt.width(s,b))/2);c.drawLine(a.x,a.y,b.x,b.y,p);a=b;}}
    }
    void featureChecks() throws Exception {
        Documents.Doc doc=new Documents.Doc();doc.name="Edição e backup";doc.count=2;
        Ink.Stroke original=new Ink.Stroke(0xFF263C32,3,false);for(int x=90;x<=310;x+=3)original.points.add(new Ink.Point(x,200,0.8f));doc.strokes(0).add(original);
        ui(()->{activity.library.add(doc);activity.open(doc);});
        ui(()->{
            NotebookView v=activity.canvas;v.partialEraser=true;activity.selectTool(2);float sx=v.left+200*v.base,sy=v.top+200*v.base;pen(v,MotionEvent.ACTION_DOWN,sx,sy,1);pen(v,MotionEvent.ACTION_UP,sx,sy,1);
            check(v.strokes.size()==2,"Partial eraser must keep two fragments");v.undo();check(v.strokes.size()==1,"Undo partial erasure restores the original stroke");
            v.partialEraser=false;pen(v,MotionEvent.ACTION_DOWN,sx,sy,1);pen(v,MotionEvent.ACTION_UP,sx,sy,1);check(v.strokes.isEmpty(),"Whole-stroke eraser");v.undo();
            activity.selectTool(4);v.selection.begin(60,170);v.selection.move(340,170);v.selection.move(340,230);v.selection.move(60,230);v.selection.move(60,170);v.selection.end();check(v.selection.selected.size()==1,"Lasso selects visible ink");
            v.selection.begin(160,200);v.selection.move(190,230);check(v.strokes.get(0).points.get(0).x==90,"Dragging must not rewrite raw points per motion event");v.selection.end();check(Math.abs(v.strokes.get(0).points.get(0).x-120)<0.1f,"Move selection");
            float oldWidth=v.strokes.get(0).width;v.selection.bounds();float right=v.selection.box.right,bottom=v.selection.box.bottom;v.selection.begin(right,bottom);v.selection.move(right+40,bottom+10);v.selection.end();check(v.strokes.get(0).width>oldWidth,"Resize selection scales handwriting");v.selection.recolor(0xFF3562B7);check(v.strokes.get(0).color==0xFF3562B7,"Selection recolor");
            check(v.selection.copy(doc.id),"Copy selection");activity.changePage(1);
        });
        ui(()->{check(activity.canvas.selection.paste(doc.id),"Paste selection on another page of the same notebook");check(doc.strokes(1).size()==1,"Cross-page paste adds one object");activity.changePage(-1);});
        File imageFile=new File(activity.store.assets(doc),"qa.png");Bitmap image=Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888);image.eraseColor(0xFF4B85D1);try(OutputStream out=new FileOutputStream(imageFile)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
        ui(()->{Ink.Stroke text=new Ink.Stroke(0xFF263C32,2,false);text.kind="text";text.text="Texto editável no Folha";text.x=60;text.y=300;text.boxW=400;text.boxH=80;text.fontSize=22;activity.canvas.addObject(text);
            Ink.Stroke pic=new Ink.Stroke(0xFF263C32,1,false);pic.kind="image";pic.asset="qa.png";pic.x=60;pic.y=420;pic.boxW=180;pic.boxH=120;activity.canvas.addObject(pic);activity.loadImages();activity.saveCurrent();});
        activity.renderWorker.submit(()->{}).get();activity.worker.submit(()->{}).get();waitForIdleSync();
        ui(()->check(activity.canvas.images.containsKey("qa.png"),"Images load off the UI thread"));
        ui(()->{activity.imageTarget=doc.id;activity.imagePage=0;activity.receiveImage(android.net.Uri.fromFile(imageFile));});activity.fileWorker.submit(()->{}).get();waitForIdleSync();activity.renderWorker.submit(()->{}).get();waitForIdleSync();
        ui(()->{check(activity.canvas.strokes.size()==4,"Image import normalizes a selected image and adds a movable object");activity.saveCurrent();});activity.worker.submit(()->{}).get();
        Documents.Doc race=new Documents.Doc();race.name="Save coalescing";race.strokes(0).add(original.copy(true));java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1);
        activity.worker.execute(()->{try{release.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        ui(()->{activity.persist(race);Documents.Doc metadata=race.light();metadata.name="Renamed safely";activity.persist(metadata);});release.countDown();activity.worker.submit(()->{}).get();check(activity.store.read(race.id).strokes(0).size()==1,"Coalesced metadata edits must never replace a pending full notebook with empty pages");
        Documents.Doc disk=activity.store.read(doc.id);check(disk.strokes(0).size()==4&&disk.strokes(0).get(1).text.contains("editável"),"Texts, image metadata and strokes persist together");
        ByteArrayOutputStream archive=new ByteArrayOutputStream();Backups.export(disk,activity.store.dir(doc.id),archive);Documents.Doc restored=Backups.restore(activity.store,activity.getCacheDir(),new ByteArrayInputStream(archive.toByteArray()));
        check(!restored.id.equals(doc.id)&&restored.originId.equals(doc.id),"Restoring an existing notebook must make a copy");check(restored.strokes(0).size()==4&&MediaFiles.asset(activity.store.dir(restored.id),"qa.png").exists(),"Editable backup preserves all object types and image bytes");
        ByteArrayOutputStream evil=new ByteArrayOutputStream();try(java.util.zip.ZipOutputStream z=new java.util.zip.ZipOutputStream(evil)){z.putNextEntry(new java.util.zip.ZipEntry("../escape.txt"));z.write(1);z.closeEntry();}
        boolean rejected=false;try{Backups.restore(activity.store,activity.getCacheDir(),new ByteArrayInputStream(evil.toByteArray()));}catch(IOException expected){rejected=true;}check(rejected,"Backup extraction rejects path traversal");
        results.add("Partial eraser, lasso, move, resize, copy/paste, recolor, text/image persistence and editable backup");
        Bitmap proof=getUiAutomation().takeScreenshot();if(proof!=null){try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"folha-v03-editor.png"))){proof.compress(Bitmap.CompressFormat.PNG,100,out);}proof.recycle();}
        check(UpdateChecker.compare("0.3.0","0.2.0")>0&&UpdateChecker.compare("0.3.0","0.3.0")==0,"Update version ordering");UpdateChecker.Result latest=UpdateChecker.check("0.0.0");check(latest.newer&&latest.url.startsWith("https://github.com/GusDesign0/folha-android/"),"Manual GitHub update lookup");results.add("GitHub update lookup");
        File folder=getTargetContext().getExternalFilesDir(null);File exported=new File(folder,"folha-objetos.pdf");try(OutputStream out=new FileOutputStream(exported)){PdfFiles.export(disk,activity.store.pdf(disk),out);}check(PdfFiles.count(exported)==2,"Mixed notebook PDF export");
        File fixture=new File(folder,"fixture-preservar.pdf");check(fixture.isFile(),"PDF preservation fixture must be present");Documents.Doc imported=new Documents.Doc();imported.pdf=true;imported.name="PDF preservado";imported.count=PdfFiles.count(fixture);try(InputStream in=new FileInputStream(fixture);OutputStream out=new FileOutputStream(activity.store.pdf(imported))){Documents.copy(in,out);}
        for(int p=0;p<imported.count;p++){Ink.Stroke mark=new Ink.Stroke(0xFFE32E2E,8,false);mark.pen="ballpoint";mark.points.add(new Ink.Point(40,40,1));mark.points.add(new Ink.Point(90,40,1));imported.strokes(p).add(mark);}
        File preserved=new File(folder,"folha-pdf-preservado.pdf");try(OutputStream out=new FileOutputStream(preserved)){PdfFiles.export(imported,activity.store.pdf(imported),out);}
        for(int p=0;p<imported.count;p++){PdfFiles.Rendered rendered=PdfFiles.render(preserved,p,1200);int found=0;float scale=(float)rendered.bitmap.getWidth()/rendered.width;
            for(int y=Math.max(0,(int)(25*scale));y<Math.min(rendered.bitmap.getHeight(),(int)(55*scale));y++)for(int x=Math.max(0,(int)(25*scale));x<Math.min(rendered.bitmap.getWidth(),(int)(105*scale));x++){int c=rendered.bitmap.getPixel(x,y);if(Color.red(c)>160&&Color.green(c)<110&&Color.blue(c)<110)found++;}
            rendered.bitmap.recycle();check(found>10,"Overlay position on rotated/cropped PDF page "+p);}
        results.add("Preserved source PDF with rotated/cropped-page annotation alignment");
        ui(()->{activity.open(doc);});ui(()->{
            NotebookView v=activity.canvas;activity.selectTool(0);v.selection.clear();
            for(int j=0;j<250;j++){Ink.Stroke s=new Ink.Stroke(0xFF263C32,1.5f,false);for(int k=0;k<40;k++)s.points.add(new Ink.Point(25+k*12,60+j%28*24+3*(float)Math.sin(k),0.7f));v.strokes.add(s);}v.refreshLayer();Bitmap screen=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(screen);v.draw(c);
            long[] times=new long[100];float x=v.getWidth()/2f,y=v.getHeight()/2f;pen(v,MotionEvent.ACTION_DOWN,x-100,y,0.7f);
            for(int k=0;k<times.length;k++){long start=System.nanoTime();pen(v,MotionEvent.ACTION_MOVE,x-100+k*2,y+10*(float)Math.sin(k/8f),0.7f);v.draw(c);times[k]=(System.nanoTime()-start)/1000;}
            pen(v,MotionEvent.ACTION_UP,x+100,y,0.7f);v.draw(c);screen.recycle();Arrays.sort(times);results.add("250-stroke page, draw CPU p50="+times[50]+"us p95="+times[95]+"us (emulator, not physical latency)");
        });
    }
}
