package br.com.folha.notes;

import android.graphics.*;
import android.graphics.pdf.*;
import android.os.ParcelFileDescriptor;
import java.io.*;

final class PdfFiles {
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
        PdfDocument pdf=new PdfDocument();try {
            for(int i=0;i<doc.count;i++) {
                Rendered image=doc.pdf?render(source,i,2000):null;
                int w=image==null?595:image.width,h=image==null?842:image.height;
                PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(w,h,i+1).create());Canvas c=page.getCanvas();
                if(image==null)PageArt.paper(c,w,h,doc.paper);else{Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);c.drawBitmap(image.bitmap,null,new Rect(0,0,w,h),p);}
                PageArt.ink(c,doc.strokes(i));pdf.finishPage(page);if(image!=null)image.bitmap.recycle();
            }
            pdf.writeTo(output);
        }finally{pdf.close();}
    }
}
