package br.com.folha.notes;

import android.graphics.*;
import android.graphics.pdf.*;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.util.*;
import android.content.Context;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.multipdf.LayerUtility;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject;
import com.tom_roush.pdfbox.util.Matrix;

final class PdfFiles {
    private static Context context;
    static void init(Context c){context=c.getApplicationContext();}
    static final class Rendered {
        final Bitmap bitmap;final int width,height;
        Rendered(Bitmap b,int w,int h){bitmap=b;width=w;height=h;}
    }
    static int count(File file) throws IOException {
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(fd)){return r.getPageCount();}
    }
    static Rendered render(File file,int index,int maxDimension) throws IOException {
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(fd);PdfRenderer.Page p=r.openPage(index)) {
            float scale=Math.min(3f,(float)maxDimension/Math.max(p.getWidth(),p.getHeight()));
            Bitmap b=Bitmap.createBitmap(Math.max(1,Math.round(p.getWidth()*scale)),Math.max(1,Math.round(p.getHeight()*scale)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);
            try{p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}catch(RuntimeException e){b.recycle();throw e;}
            return new Rendered(b,p.getWidth(),p.getHeight());
        }
    }
    static void export(Documents.Doc doc,File source,OutputStream output) throws IOException {
        if(!doc.pdf){nativeExport(doc,source,output,false);return;}
        boolean annotated=false;for(List<Ink.Stroke> page:doc.pages.values())if(!page.isEmpty())annotated=true;
        if(!annotated){try(InputStream in=new FileInputStream(source)){Documents.copy(in,output);}return;}
        if(context==null)throw new IOException("Exportação não inicializada");PDFBoxResourceLoader.init(context);
        File overlayFile=File.createTempFile("folha-overlay-",".pdf",context.getCacheDir());
        try{
            try(OutputStream out=new FileOutputStream(overlayFile)){nativeExport(doc,source,out,true);}
            MemoryUsageSetting memory=MemoryUsageSetting.setupMixed(8*1024*1024);memory.setTempDir(context.getCacheDir());
            try(PDDocument original=PDDocument.load(source,memory);PDDocument annotations=PDDocument.load(overlayFile,memory)){
                LayerUtility layers=new LayerUtility(original);
                for(int i=0;i<original.getNumberOfPages();i++){
                    if(doc.strokes(i).isEmpty())continue;
                    PDPage page=original.getPage(i);PDRectangle crop=page.getCropBox(),overlay=annotations.getPage(i).getMediaBox();
                    float x=crop.getLowerLeftX(),y=crop.getLowerLeftY(),w=crop.getWidth(),h=crop.getHeight(),aw=overlay.getWidth(),ah=overlay.getHeight();
                    int rotation=((page.getRotation()%360)+360)%360;Matrix transform;
                    if(rotation==90)transform=new Matrix(0,h/aw,-w/ah,0,x+w,y);
                    else if(rotation==180)transform=new Matrix(-w/aw,0,0,-h/ah,x+w,y+h);
                    else if(rotation==270)transform=new Matrix(0,-h/aw,w/ah,0,x,y+h);
                    else transform=new Matrix(w/aw,0,0,h/ah,x,y);
                    PDFormXObject form=layers.importPageAsForm(annotations,i);
                    try(PDPageContentStream content=new PDPageContentStream(original,page,PDPageContentStream.AppendMode.APPEND,true,true)){
                        content.saveGraphicsState();content.transform(transform);content.drawForm(form);content.restoreGraphicsState();
                    }
                }
                original.save(output);
            }
        }finally{overlayFile.delete();}
    }
    private static void nativeExport(Documents.Doc doc,File source,OutputStream output,boolean transparent) throws IOException {
        PdfDocument pdf=new PdfDocument();try {
            ParcelFileDescriptor fd=doc.pdf?ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY):null;PdfRenderer renderer=fd==null?null:new PdfRenderer(fd);
            try {
            for(int i=0;i<doc.count;i++) {
                int w=Paper.width(doc.paper),h=Paper.height(doc.paper);if(renderer!=null){try(PdfRenderer.Page p=renderer.openPage(i)){w=p.getWidth();h=p.getHeight();}}
                PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(w,h,i+1).create());Canvas c=page.getCanvas();
                if(!transparent)PageArt.paper(c,w,h,doc.paper);
                Map<String,Bitmap> images=MediaFiles.page(source.getParentFile(),doc.strokes(i),2400);
                try{PageArt.ink(c,doc.strokes(i),images);pdf.finishPage(page);}finally{MediaFiles.recycle(images);}
            }
            pdf.writeTo(output);
            }finally{if(renderer!=null)renderer.close();if(fd!=null)fd.close();}
        }finally{pdf.close();}
    }
}
