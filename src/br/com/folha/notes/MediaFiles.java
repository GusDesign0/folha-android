package br.com.folha.notes;

import android.graphics.*;
import java.io.*;
import java.util.*;

final class MediaFiles {
    static File asset(File documentDir,String name) throws IOException {if(!name.matches("[A-Za-z0-9_-]+\\.(png|jpg|jpeg)"))throw new IOException("Nome de imagem inválido");return new File(new File(documentDir,"assets"),name);}
    static Bitmap decode(File file,int edge) throws IOException {
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),o);if(o.outWidth<=0)throw new IOException("Imagem inválida");
        o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>edge*1.4f)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap b=BitmapFactory.decodeFile(file.getAbsolutePath(),o);if(b==null)throw new IOException("Não foi possível ler a imagem");
        if(Math.max(b.getWidth(),b.getHeight())>edge){float scale=(float)edge/Math.max(b.getWidth(),b.getHeight());Bitmap smaller=Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*scale)),Math.max(1,Math.round(b.getHeight()*scale)),true);if(smaller!=b)b.recycle();b=smaller;}
        return b;
    }
    static Map<String,Bitmap> page(File dir,List<Ink.Stroke> objects,int maxEdge) throws IOException {
        HashSet<String> names=new HashSet<>();for(Ink.Stroke s:objects)if(s.kind.equals("image"))names.add(s.asset);
        int edge=Math.max(256,Math.min(maxEdge,(int)Math.sqrt(4000000d/Math.max(1,names.size()))));HashMap<String,Bitmap> result=new HashMap<>();
        try{for(String name:names)result.put(name,decode(asset(dir,name),edge));return result;}catch(IOException|OutOfMemoryError e){recycle(result);throw e;}
    }
    static void recycle(Map<String,Bitmap> images){for(Bitmap b:images.values())if(b!=null&&!b.isRecycled())b.recycle();images.clear();}
}
