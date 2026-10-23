package br.com.folha.notes;
import java.util.*;
/** Geometry in PDF points; no font, network or bitmap dependency. */
final class Paper {
 static final String[] IDS={"lines","grid","dots","blank","tategaki","genko200","genko400","kanji","kana"};
 static final String[] NAMES={"Pautado","Quadriculado","Pontilhado","Em branco","Japonês · colunas verticais","Genkō yōshi · 200 caracteres","Genkō yōshi · 400 caracteres (paisagem)","Kanji · quadrículas com guias","Kana · quadrículas grandes"};
 static int width(String t){return t.equals("genko400")?842:595;}static int height(String t){return t.equals("genko400")?595:842;}
 static boolean japanese(String t){return t.equals("tategaki")||t.startsWith("genko")||t.equals("kanji")||t.equals("kana");}
 static void line(List<float[]> a,float x,float y,float X,float Y,int guide){a.add(new float[]{x,y,X,Y,guide});}
 static List<float[]> lines(float w,float h,String t){List<float[]> a=new ArrayList<>();
  if(t.equals("lines")){for(float y=66;y<h-24;y+=24)line(a,34,y,w-34,y,0);}
  else if(t.equals("grid")){for(float y=24;y<h;y+=20)line(a,0,y,w,y,0);for(float x=24;x<w;x+=20)line(a,x,0,x,h,0);}
  else if(t.equals("tategaki")){float step=(w-68)/14;for(int i=0;i<=14;i++){float x=34+i*step;line(a,x,50,x,h-50,0);if(i<14)line(a,x+step*.78f,50,x+step*.78f,h-50,1);}line(a,34,50,w-34,50,0);line(a,34,h-50,w-34,h-50,0);}
  else if(t.startsWith("genko")||t.equals("kanji")||t.equals("kana")){
   boolean gen=t.startsWith("genko");int cols=gen?(t.equals("genko400")?20:10):(t.equals("kanji")?9:6),rows=gen?20:(t.equals("kanji")?12:8);float split=t.equals("genko400")?18:0;
   float size=Math.min((w-68-split)/(cols*(gen?1.25f:1)),(h-100)/rows),gap=gen?size*.25f:0,total=cols*(size+gap)+split,x0=(w-total)/2,y0=(h-rows*size)/2;
   for(int i=0;i<cols;i++){float x=x0+i*(size+gap)+(split>0&&i>=10?split:0);line(a,x,y0,x,y0+rows*size,0);line(a,x+size,y0,x+size,y0+rows*size,0);if(gen)line(a,x+size+gap,y0,x+size+gap,y0+rows*size,1);for(int j=0;j<=rows;j++)line(a,x,y0+j*size,x+size,y0+j*size,0);if(!gen){line(a,x+size/2,y0,x+size/2,y0+rows*size,1);for(int j=0;j<rows;j++)line(a,x,y0+(j+.5f)*size,x+size,y0+(j+.5f)*size,1);}}
  }return a;
 }
}
