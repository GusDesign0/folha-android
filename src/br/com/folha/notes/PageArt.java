package br.com.folha.notes;

import android.graphics.*;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;

final class PageArt {
    private static final Map<Ink.Stroke,Outline> outlines=new WeakHashMap<>();
    private static final class Outline {List<Ink.Point> samples;Path path;}
    private static final android.util.LruCache<String,Path[]> papers=new android.util.LruCache<>(12);
    static void paper(Canvas c,float w,float h,String type){c.drawColor(Color.WHITE);String key=w+":"+h+":"+type;Path[] paths=papers.get(key);if(paths==null){paths=new Path[]{new Path(),new Path(),new Path()};for(float[] l:Paper.lines(w,h,type)){Path p=paths[(int)l[4]];p.moveTo(l[0],l[1]);p.lineTo(l[2],l[3]);}if(type.equals("dots"))for(float y=32;y<h;y+=18)for(float x=32;x<w;x+=18)paths[0].addCircle(x,y,.7f,Path.Direction.CW);StudyPaper.Label[] labels=StudyPaper.LABELS.get(type);if(labels!=null){Paint font=new Paint(Paint.ANTI_ALIAS_FLAG);font.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));for(StudyPaper.Label label:labels){font.setTextSize(label.size);Path glyph=new Path();font.getTextPath(label.text,0,label.text.length(),label.x,label.y,glyph);paths[2].addPath(glyph);}}papers.put(key,paths);}Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Paper.japanese(type)?0xFFAAC0AD:0xFFDEE5E2);p.setStrokeWidth(.65f);p.setStyle(type.equals("dots")?Paint.Style.FILL:Paint.Style.STROKE);c.drawPath(paths[0],p);p.setColor(0xFFE4E9E1);p.setStrokeWidth(.4f);p.setPathEffect(new DashPathEffect(new float[]{2,3},0));c.drawPath(paths[1],p);p.setPathEffect(null);p.setStyle(Paint.Style.FILL);p.setColor(0xFF56685F);c.drawPath(paths[2],p);}
    static void ink(Canvas c,List<Ink.Stroke> strokes) {ink(c,strokes,Collections.emptyMap());}
    static void ink(Canvas c,List<Ink.Stroke> strokes,Map<String,Bitmap> images){for(Ink.Stroke s:strokes)stroke(c,s,images);}
    static void stroke(Canvas c,Ink.Stroke s) {
        stroke(c,s,Collections.emptyMap());
    }
    static void stroke(Canvas c,Ink.Stroke s,Map<String,Bitmap> images){
        if(!s.glyph.isEmpty()){JapaneseGlyph.draw(c,s);return;}
        if(s.kind.equals("text")){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(s.color);p.setTextSize(s.fontSize);p.setTypeface(Typeface.DEFAULT);int saved=c.save();c.clipRect(s.x,s.y,s.x+s.boxW,s.y+s.boxH);float baseline=s.y-p.ascent(),line=p.getFontSpacing()*1.1f;
            for(String paragraph:s.text.split("\\n",-1)){String remaining=paragraph;if(remaining.isEmpty()){baseline+=line;continue;}while(!remaining.isEmpty()&&baseline<s.y+s.boxH){int n=p.breakText(remaining,true,Math.max(1,s.boxW),null);if(n==0)n=1;String part=remaining.substring(0,n);float offset=s.align.equals("center")?(s.boxW-p.measureText(part))/2:s.align.equals("right")?s.boxW-p.measureText(part):0;c.drawText(part,s.x+Math.max(0,offset),baseline,p);remaining=remaining.substring(n);baseline+=line;}}
            c.restoreToCount(saved);return;
        }
        if(s.kind.equals("image")){Bitmap image=images.get(s.asset);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);if(image!=null&&!image.isRecycled())c.drawBitmap(image,null,new RectF(s.x,s.y,s.x+s.boxW,s.y+s.boxH),p);else{p.setColor(0xFFE3EAE4);c.drawRect(s.x,s.y,s.x+s.boxW,s.y+s.boxH,p);}return;}
        List<Ink.Point> points=s.samples();if(points.isEmpty())return;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(s.color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(s.marker){
            p.setAlpha(76);p.setStrokeWidth(s.width);p.setStyle(Paint.Style.STROKE);Path path=new Path();Ink.Point a=points.get(0);path.moveTo(a.x,a.y);
            for(int i=1;i<points.size();i++){Ink.Point b=points.get(i);path.lineTo(b.x,b.y);}
            if(points.size()==1){p.setStyle(Paint.Style.FILL);c.drawCircle(a.x,a.y,s.width/2,p);}else c.drawPath(path,p);
        }else {
            // One filled silhouette keeps pressure transitions continuous and exports as PDF paths.
            p.setStyle(Paint.Style.FILL);c.drawPath(outline(s,points),p);
        }
    }
    private static synchronized Path outline(Ink.Stroke stroke,List<Ink.Point> points){
        Outline cached=outlines.get(stroke);if(cached!=null&&cached.samples==points)return cached.path;
        Path path=new Path();path.setFillType(Path.FillType.WINDING);int n=points.size();
        if(n==1){Ink.Point a=points.get(0);path.addCircle(a.x,a.y,width(stroke,a)/2,Path.Direction.CW);}
        else {
            float[] nx=new float[n],ny=new float[n];
            for(int i=0;i<n;i++){
                Ink.Point a=points.get(Math.max(0,i-1)),b=points.get(Math.min(n-1,i+1));
                float dx=b.x-a.x,dy=b.y-a.y,len=(float)Math.hypot(dx,dy);
                if(len>0.00001f){nx[i]=dy/len;ny[i]=-dx/len;}else if(i>0){nx[i]=nx[i-1];ny[i]=ny[i-1];}else{nx[i]=0;ny[i]=-1;}
                Ink.Point q=points.get(i);float r=width(stroke,q)/2;
                if(i==0)path.moveTo(q.x+nx[i]*r,q.y+ny[i]*r);else path.lineTo(q.x+nx[i]*r,q.y+ny[i]*r);
            }
            for(int i=n-1;i>=0;i--){Ink.Point q=points.get(i);float r=width(stroke,q)/2;path.lineTo(q.x-nx[i]*r,q.y-ny[i]*r);}path.close();
            Ink.Point a=points.get(0),b=points.get(n-1);path.addCircle(a.x,a.y,width(stroke,a)/2,Path.Direction.CW);path.addCircle(b.x,b.y,width(stroke,b)/2,Path.Direction.CW);
            // Fill tight turns, including a pen reversing direction, without pinched joins.
            for(int i=1;i<n-1;i++)if(nx[i-1]*nx[i]+ny[i-1]*ny[i]<0.6f){Ink.Point q=points.get(i);path.addCircle(q.x,q.y,width(stroke,q)/2,Path.Direction.CW);}
        }
        Outline fresh=new Outline();fresh.samples=points;fresh.path=path;outlines.put(stroke,fresh);return path;
    }
    static float width(Ink.Stroke s,Ink.Point p){return Ink.penWidth(s,p);}
}
