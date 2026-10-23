package br.com.folha.notes;
import java.util.*;

/** Small deterministic cleanup, not character recognition. Work is done after input has stopped. */
final class JapaneseGeometry {
 static float[] bounds(List<Ink.Stroke> strokes){float l=Float.MAX_VALUE,t=l,r=-l,b=-l;for(Ink.Stroke s:strokes)for(Ink.Point p:s.points){l=Math.min(l,p.x);t=Math.min(t,p.y);r=Math.max(r,p.x);b=Math.max(b,p.y);}return new float[]{l,t,r,b};}
 static float[] cell(String paper,float w,float h,float x,float y){
  if(StudyPaper.CELLS.containsKey(paper))return StudyPaper.cell(paper,x,y);
  boolean gen=paper.startsWith("genko");if(!gen&&!paper.equals("kanji")&&!paper.equals("kana"))return null;
  int cols=gen?(paper.equals("genko400")?20:10):(paper.equals("kanji")?9:6),rows=gen?20:(paper.equals("kanji")?12:8);float split=paper.equals("genko400")?18:0;
  float size=Math.min((w-68-split)/(cols*(gen?1.25f:1)),(h-100)/rows),gap=gen?size*.25f:0,x0=(w-cols*(size+gap)-split)/2,y0=(h-rows*size)/2;int row=(int)Math.floor((y-y0)/size);if(row<0||row>=rows)return null;
  for(int i=0;i<cols;i++){float left=x0+i*(size+gap)+(split>0&&i>=10?split:0);if(x>=left&&x<=left+size)return new float[]{left,y0+row*size,left+size,y0+(row+1)*size};}return null;
 }
 static boolean near(float[] a,float[] b){float size=Math.max(14,Math.max(a[2]-a[0],a[3]-a[1]));return b[0]<=a[2]+size*.35f&&b[2]>=a[0]-size*.35f&&b[1]<=a[3]+size*.35f&&b[3]>=a[1]-size*.35f&&Math.max(Math.max(a[2],b[2])-Math.min(a[0],b[0]),Math.max(a[3],b[3])-Math.min(a[1],b[1]))<200;}
 static boolean hasHook(Ink.Stroke s){int n=s.points.size();if(n<3)return false;Ink.Point a=s.points.get(0),b=s.points.get(n-1),head=s.points.get(Math.min(n-1,Math.max(1,n/5))),tail=s.points.get(Math.max(0,n-1-Math.max(1,n/5)));double dx=b.x-a.x,dy=b.y-a.y,len=Math.hypot(dx,dy),last=Math.hypot(b.x-tail.x,b.y-tail.y),first=Math.hypot(head.x-a.x,head.y-a.y);return(last>1&&((b.x-tail.x)*dx+(b.y-tail.y)*dy)/(last*Math.max(1,len))<.82)||(first>1&&((head.x-a.x)*dx+(head.y-a.y)*dy)/(first*Math.max(1,len))<.82);}
 static List<Ink.Stroke> adjust(List<Ink.Stroke> raw,float[] cell){
  float[] box=bounds(raw);double angle=0,weight=0;for(Ink.Stroke s:raw){if(s.points.size()<2)continue;Ink.Point a=s.points.get(0),b=s.points.get(s.points.size()-1);double dx=b.x-a.x,dy=b.y-a.y,len=Math.hypot(dx,dy),arc=0;for(int i=1;i<s.points.size();i++){Ink.Point p=s.points.get(i-1),q=s.points.get(i);arc+=Math.hypot(p.x-q.x,p.y-q.y);}double theta=Math.atan2(dy,dx),delta=theta-Math.round(theta/(Math.PI/2))*(Math.PI/2);if(len>8&&len/Math.max(1,arc)>.92&&Math.abs(delta)<Math.toRadians(12)){angle+=delta*len;weight+=len;}}
  angle=weight==0?0:Math.max(-Math.toRadians(8),Math.min(Math.toRadians(8),angle/weight));float cx=(box[0]+box[2])/2,cy=(box[1]+box[3])/2,cos=(float)Math.cos(-angle),sin=(float)Math.sin(-angle);List<Ink.Stroke> out=new ArrayList<>();
  for(Ink.Stroke s:raw){Ink.Stroke clean=s.copy(false);clean.points.clear();int n=s.points.size();for(int i=0;i<n;i++){Ink.Point p=s.points.get(i);float x=p.x,y=p.y;if(i>0&&i<n-1){Ink.Point a=s.points.get(i-1),b=s.points.get(i+1);x=x*.7f+(a.x+b.x)*.15f;y=y*.7f+(a.y+b.y)*.15f;}clean.points.add(new Ink.Point(cx+(x-cx)*cos-(y-cy)*sin,cy+(x-cx)*sin+(y-cy)*cos,p.pressure));}
   if(n>=2){Ink.Point a=clean.points.get(0),b=clean.points.get(n-1);double len=Math.hypot(b.x-a.x,b.y-a.y),arc=0;for(int i=1;i<n;i++){Ink.Point p=clean.points.get(i-1),q=clean.points.get(i);arc+=Math.hypot(p.x-q.x,p.y-q.y);}boolean hook=hasHook(s);if(len>8&&len/Math.max(1,arc)>.94&&!hook){boolean horizontal=Math.abs(b.y-a.y)<len*.16,vertical=Math.abs(b.x-a.x)<len*.16;if(horizontal||vertical){float axis=0;for(Ink.Point p:clean.points)axis+=horizontal?p.y:p.x;axis/=n;List<Ink.Point> pts=new ArrayList<>(clean.points);clean.points.clear();for(Ink.Point p:pts)clean.points.add(new Ink.Point(vertical?axis:p.x,horizontal?axis:p.y,p.pressure));}}}out.add(clean);
  }
  if(cell!=null){float[] b=bounds(out);float size=cell[2]-cell[0],scale=Math.min(1,size*.82f/Math.max(1,Math.max(b[2]-b[0],b[3]-b[1]))),dx=(cell[0]+cell[2]-b[0]-b[2])/2,dy=(cell[1]+cell[3]-b[1]-b[3])/2;List<Ink.Stroke> placed=new ArrayList<>();for(Ink.Stroke s:out)placed.add(s.transform((b[0]+b[2])/2,(b[1]+b[3])/2,dx,dy,scale,false));return placed;}return out;
 }
 static boolean japanese(String text){if(text==null||text.codePointCount(0,text.length())!=1)return false;int c=text.codePointAt(0);return c>=0x3041&&c<=0x30FA||c>=0x3400&&c<=0x9FFF||c>=0x20000&&c<=0x3134F||c==0x3005||c==0x3007;}
}
