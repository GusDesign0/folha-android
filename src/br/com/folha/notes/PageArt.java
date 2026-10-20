package br.com.folha.notes;

import android.graphics.*;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class PageArt {
    private static final Map<Ink.Stroke,Outline> outlines=new WeakHashMap<>();
    private static final class Outline {List<Ink.Point> samples;Path path;}
    static void paper(Canvas c,float w,float h,String type) {
        c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.rgb(222,229,226));p.setStrokeWidth(0.65f);
        if(type.equals("lines")){for(float y=66;y<h-24;y+=24)c.drawLine(34,y,w-34,y,p);}
        else if(type.equals("grid")){for(float y=24;y<h;y+=20)c.drawLine(0,y,w,y,p);for(float x=24;x<w;x+=20)c.drawLine(x,0,x,h,p);}
        else if(type.equals("dots")){for(float y=32;y<h;y+=18)for(float x=32;x<w;x+=18)c.drawCircle(x,y,0.7f,p);}
    }
    static void ink(Canvas c,List<Ink.Stroke> strokes) {for(Ink.Stroke s:strokes)stroke(c,s);}
    static void stroke(Canvas c,Ink.Stroke s) {
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
    static float width(Ink.Stroke s,Ink.Point p){return s.width*(0.45f+0.65f*p.pressure);}
}
