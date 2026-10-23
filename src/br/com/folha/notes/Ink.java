package br.com.folha.notes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Coordinate and history logic, independent of Android for deterministic tests. */
public final class Ink {
    public static final class Point {
        public final float x, y, pressure;
        public Point(float x, float y, float pressure) {
            this.x=x; this.y=y; this.pressure=Math.max(0.1f,Math.min(1.5f,pressure));
        }
    }
    public static final class Stroke {
        public String id=UUID.randomUUID().toString(),kind="ink",pen="fountain",text="",asset="",align="left";
        public float x,y,boxW=200,boxH=100,fontSize=24,smoothing=1,sensitivity=1;
        public final List<Point> points=new ArrayList<>();
        public int color;
        public float width;
        public boolean marker;
        private List<Point> sampled;
        private int sampledCount=-1;
        private Point sampledLast;
        private float minX,maxX,minY,maxY;
        private float sampledSmoothing=-1;
        public Stroke(int color, float width, boolean marker) { this.color=color; this.width=width; this.marker=marker; }
        /** Raw input stays untouched on disk. The rounded centerline is shared by drawing and erasing. */
        public List<Point> samples() {
            Point last=points.isEmpty()?null:points.get(points.size()-1);
            if(sampledCount==points.size()&&sampledLast==last&&sampledSmoothing==smoothing)return sampled;
            sampled=smooth(points,smoothing);sampledCount=points.size();sampledLast=last;sampledSmoothing=smoothing;
            minX=minY=Float.POSITIVE_INFINITY;maxX=maxY=Float.NEGATIVE_INFINITY;
            for(Point p:sampled){minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);}
            return sampled;
        }
        public boolean hit(float x,float y,float radius) {
            if(!kind.equals("ink"))return x>=this.x-radius&&x<=this.x+boxW+radius&&y>=this.y-radius&&y<=this.y+boxH+radius;
            List<Point> line=samples();float r=radius+width*(marker?0.5f:1f);
            if(x<minX-r||x>maxX+r||y<minY-r||y>maxY+r)return false;
            for(int i=0;i<line.size();i++) {
                Point a=line.get(i), b=line.get(Math.max(0,i-1));
                if(distanceSquared(x,y,a.x,a.y,b.x,b.y)<=r*r) return true;
            }
            return false;
        }
        public Stroke copy(boolean newId){Stroke s=new Stroke(color,width,marker);s.id=newId?UUID.randomUUID().toString():id;s.kind=kind;s.pen=pen;s.text=text;s.asset=asset;s.align=align;s.x=x;s.y=y;s.boxW=boxW;s.boxH=boxH;s.fontSize=fontSize;s.smoothing=smoothing;s.sensitivity=sensitivity;s.points.addAll(points);return s;}
        public float[] bounds(){if(!kind.equals("ink"))return new float[]{x,y,x+boxW,y+boxH};samples();return new float[]{minX-width,minY-width,maxX+width,maxY+width};}
        public Stroke transform(float ox,float oy,float dx,float dy,float scale,boolean newId){Stroke s=copy(newId);s.points.clear();for(Point p:points)s.points.add(new Point(ox+(p.x-ox)*scale+dx,oy+(p.y-oy)*scale+dy,p.pressure));s.x=ox+(x-ox)*scale+dx;s.y=oy+(y-oy)*scale+dy;s.boxW=boxW*scale;s.boxH=boxH*scale;s.fontSize=fontSize*scale;s.width=width*scale;return s;}
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
    static List<Point> smooth(List<Point> raw,float amount){
        ArrayList<Point> out=new ArrayList<>();if(raw.isEmpty())return out;
        if(amount<=0){out.add(raw.get(0));for(int i=1;i<raw.size();i++){Point a=raw.get(i-1),b=raw.get(i);int n=Math.max(1,Math.min(1024,(int)Math.ceil(Math.hypot(b.x-a.x,b.y-a.y)/0.65)));for(int j=1;j<=n;j++){float t=(float)j/n;append(out,new Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.pressure+(b.pressure-a.pressure)*t));}}return out;}
        ArrayList<Point> filtered=new ArrayList<>(raw.size());float pressure=raw.get(0).pressure;
        for(int i=0;i<raw.size();i++){
            Point p=raw.get(i);
            if(i>0){Point prev=raw.get(i-1);float distance=(float)Math.hypot(p.x-prev.x,p.y-prev.y);
                float alpha=Math.max(0.18f,Math.min(0.72f,1f-(float)Math.exp(-distance/2.5f)));alpha=1-amount*(1-alpha);pressure+=(p.pressure-pressure)*alpha;}
            filtered.add(new Point(p.x,p.y,pressure));
        }
        Point start=filtered.get(0);out.add(start);
        for(int i=1;i<filtered.size();i++){
            Point control=filtered.get(i),mid=i==filtered.size()-1?control:midpoint(control,filtered.get(i+1));
            Point end=new Point(control.x+(mid.x-control.x)*amount,control.y+(mid.y-control.y)*amount,control.pressure+(mid.pressure-control.pressure)*amount);
            float length=(float)(Math.hypot(control.x-start.x,control.y-start.y)+Math.hypot(end.x-control.x,end.y-control.y));
            int steps=Math.max(1,Math.min(1024,(int)Math.ceil(length/0.65f)));
            for(int j=1;j<=steps;j++){float t=(float)j/steps,u=1-t;
                append(out,new Point(u*u*start.x+2*u*t*control.x+t*t*end.x,u*u*start.y+2*u*t*control.y+t*t*end.y,u*u*start.pressure+2*u*t*control.pressure+t*t*end.pressure));}
            start=end;
        }
        return out;
    }
    public static float penWidth(Stroke s,Point p){
        float pressure=Math.max(0.1f,Math.min(1.5f,1+(p.pressure-1)*s.sensitivity));
        if(s.pen.equals("ballpoint"))return s.width;
        if(s.pen.equals("brush"))return s.width*(0.18f+1.15f*pressure);
        return s.width*(0.45f+0.65f*pressure);
    }
    /** Cut the visible centerline against a swept eraser capsule, keeping untouched fragments. */
    public static List<Stroke> cut(Stroke s,float ax,float ay,float bx,float by,float radius){
        ArrayList<Stroke> result=new ArrayList<>();if(!s.kind.equals("ink")){result.add(s);return result;}
        List<Point> line=s.samples();Stroke part=null;Point prev=null;boolean previousInside=false,removed=false;
        for(Point p:line){float r=radius+(s.marker?s.width:penWidth(s,p))/2;boolean inside=distanceSquared(p.x,p.y,ax,ay,bx,by)<=r*r;
            if(prev!=null&&inside!=previousInside){Point cross=boundary(s,prev,p,ax,ay,bx,by,radius,previousInside);
                if(!previousInside){if(part!=null)part.points.add(cross);part=null;}else{part=fragment(s);part.points.add(cross);result.add(part);}}
            if(inside){removed=true;part=null;}else{if(part==null){part=fragment(s);result.add(part);}part.points.add(p);}
            prev=p;previousInside=inside;
        }
        if(!removed){result.clear();result.add(s);}return result;
    }
    private static Stroke fragment(Stroke s){Stroke p=s.copy(true);p.points.clear();p.smoothing=0;return p;}
    private static Point boundary(Stroke s,Point a,Point b,float ax,float ay,float bx,float by,float radius,boolean aInside){
        float lo=0,hi=1;for(int i=0;i<14;i++){float t=(lo+hi)/2;Point p=new Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.pressure+(b.pressure-a.pressure)*t);float r=radius+(s.marker?s.width:penWidth(s,p))/2;boolean in=distanceSquared(p.x,p.y,ax,ay,bx,by)<=r*r;if(in==aInside)lo=t;else hi=t;}
        float t=(lo+hi)/2;return new Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.pressure+(b.pressure-a.pressure)*t);
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
