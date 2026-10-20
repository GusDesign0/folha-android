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
    void ui(Runnable r){runOnMainSync(r);waitForIdleSync();}
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
                int preserved=0;for(Documents.Doc d:activity.library)if(d.name.equals("Caderno de teste")&&d.strokes(0).size()>=2)preserved++;
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
            List<Documents.Doc> docs=activity.store.load();Documents.Doc loaded=null;for(Documents.Doc d:docs)if(d.id.equals(notebook.id))loaded=d;
            check(loaded!=null&&loaded.strokes(0).size()==2,"Persisted strokes survive reopening");results.add("Atomic local save and JSON reload");
            File pdf=new File(getTargetContext().getCacheDir(),"integration.pdf");
            try(OutputStream out=new FileOutputStream(pdf)){PdfFiles.export(loaded,activity.store.pdf(loaded),out);}
            check(PdfFiles.count(pdf)==1,"Notebook PDF export has one page");PdfFiles.Rendered rendered=PdfFiles.render(pdf,0,1200);check(rendered.bitmap.getWidth()>0,"Exported PDF renders");rendered.bitmap.recycle();results.add("Notebook PDF export and render");
            Documents.Doc imported=new Documents.Doc();imported.name="PDF de teste";imported.pdf=true;imported.count=1;
            try(InputStream in=new FileInputStream(pdf);OutputStream out=new FileOutputStream(activity.store.pdf(imported))){Documents.copy(in,out);}
            ui(()->{activity.library.add(imported);activity.open(imported);});activity.worker.submit(()->{}).get();waitForIdleSync();
            ui(()->check(activity.canvas.ready&&activity.canvas.pdf!=null,"Imported PDF opens in editor"));
            File annotated=new File(getTargetContext().getCacheDir(),"annotated.pdf");
            Ink.Stroke mark=new Ink.Stroke(0xFF3562B7,3,false);mark.points.add(new Ink.Point(40,40,1));mark.points.add(new Ink.Point(180,90,1));imported.strokes(0).add(mark);
            try(OutputStream out=new FileOutputStream(annotated)){PdfFiles.export(imported,activity.store.pdf(imported),out);}check(PdfFiles.count(annotated)==1,"Annotated imported PDF exports");results.add("PDF import, annotation and re-export");
            qualityArtifacts();results.add("Smooth ink and PDF-path example generated");
            ui(()->{activity.leaveEditor();activity.home();});report.putString("stream","\nPASS: "+String.join("; ",results)+"\n");finish(-1,report);
        }catch(Throwable e){report.putString("stream","\nFAIL: "+e+"\n"+android.util.Log.getStackTraceString(e));finish(0,report);}
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
}
