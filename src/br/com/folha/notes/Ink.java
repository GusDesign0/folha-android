package br.com.folha.notes;

import java.util.ArrayList;
import java.util.List;

/** Coordinate and history logic, independent of Android for deterministic tests. */
public final class Ink {
    public static final class Point {
        public final float x, y, pressure;
        public Point(float x, float y, float pressure) {
            this.x=x; this.y=y; this.pressure=Math.max(0.1f,Math.min(1.5f,pressure));
        }
    }
    public static final class Stroke {
        public final List<Point> points=new ArrayList<>();
        public int color;
        public float width;
        public boolean marker;
        private List<Point> sampled;
        private int sampledCount=-1;
        private Point sampledLast;
        private float minX,maxX,minY,maxY;
        public Stroke(int color, float width, boolean marker) { this.color=color; this.width=width; this.marker=marker; }
        /** Raw input stays untouched on disk. The rounded centerline is shared by drawing and erasing. */
        public List<Point> samples() {
            Point last=points.isEmpty()?null:points.get(points.size()-1);
            if(sampledCount==points.size()&&sampledLast==last)return sampled;
            sampled=smooth(points);sampledCount=points.size();sampledLast=last;
            minX=minY=Float.POSITIVE_INFINITY;maxX=maxY=Float.NEGATIVE_INFINITY;
            for(Point p:sampled){minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);}
            return sampled;
        }
        public boolean hit(float x,float y,float radius) {
            List<Point> line=samples();float r=radius+width*(marker?0.5f:0.72f);
            if(x<minX-r||x>maxX+r||y<minY-r||y>maxY+r)return false;
            for(int i=0;i<line.size();i++) {
                Point a=line.get(i), b=line.get(Math.max(0,i-1));
                if(distanceSquared(x,y,a.x,a.y,b.x,b.y)<=r*r) return true;
            }
            return false;
        }
    }
    private static Point midpoint(Point a,Point b){return new Point((a.x+b.x)/2,(a.y+b.y)/2,(a.pressure+b.pressure)/2);}
    private static void append(List<Point> result,Point p){
        if(!result.isEmpty()){
            Point last=result.get(result.size()-1);
            if(distanceSquared(p.x,p.y,last.x,last.y,last.x,last.y)<0.000001f){
                if(p.pressure>last.pressure)result.set(result.size()-1,p);return;
            }
        }
        result.add(p);
    }
    static List<Point> smooth(List<Point> raw){
        ArrayList<Point> out=new ArrayList<>();if(raw.isEmpty())return out;
        ArrayList<Point> filtered=new ArrayList<>(raw.size());float pressure=raw.get(0).pressure;
        for(int i=0;i<raw.size();i++){
            Point p=raw.get(i);
            if(i>0){Point prev=raw.get(i-1);float distance=(float)Math.hypot(p.x-prev.x,p.y-prev.y);
                float alpha=Math.max(0.18f,Math.min(0.72f,1f-(float)Math.exp(-distance/2.5f)));pressure+=(p.pressure-pressure)*alpha;}
            filtered.add(new Point(p.x,p.y,pressure));
        }
        Point start=filtered.get(0);out.add(start);
        for(int i=1;i<filtered.size();i++){
            Point control=filtered.get(i),end=i==filtered.size()-1?control:midpoint(control,filtered.get(i+1));
            float length=(float)(Math.hypot(control.x-start.x,control.y-start.y)+Math.hypot(end.x-control.x,end.y-control.y));
            int steps=Math.max(1,Math.min(1024,(int)Math.ceil(length/0.65f)));
            for(int j=1;j<=steps;j++){float t=(float)j/steps,u=1-t;
                append(out,new Point(u*u*start.x+2*u*t*control.x+t*t*end.x,u*u*start.y+2*u*t*control.y+t*t*end.y,u*u*start.pressure+2*u*t*control.pressure+t*t*end.pressure));}
            start=end;
        }
        return out;
    }
    public static float distanceSquared(float px,float py,float ax,float ay,float bx,float by) {
        float dx=bx-ax,dy=by-ay,l=dx*dx+dy*dy;
        float t=l==0?0:Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/l));
        dx=px-(ax+t*dx);dy=py-(ay+t*dy);return dx*dx+dy*dy;
    }
    public static final class History {
        private final List<List<Stroke>> undo=new ArrayList<>(),redo=new ArrayList<>();
        private void push(List<List<Stroke>> stack,List<Stroke> state) {
            stack.add(new ArrayList<>(state));if(stack.size()>50)stack.remove(0);
        }
        public void before(List<Stroke> state) { push(undo,state);redo.clear(); }
        public boolean undo(List<Stroke> state) {
            if(undo.isEmpty())return false;push(redo,state);replace(state,undo.remove(undo.size()-1));return true;
        }
        public boolean redo(List<Stroke> state) {
            if(redo.isEmpty())return false;push(undo,state);replace(state,redo.remove(redo.size()-1));return true;
        }
        private void replace(List<Stroke> target,List<Stroke> source){target.clear();target.addAll(source);}
    }
}
