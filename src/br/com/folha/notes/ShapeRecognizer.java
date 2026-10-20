package br.com.folha.notes;

import java.util.List;

/** Conservative recognition, called once after a deliberate pen hold, never per sample. */
public final class ShapeRecognizer {
    public static Ink.Stroke recognize(Ink.Stroke source){
        List<Ink.Point> points=source.points;int n=points.size();if(n<3||n>8192)return null;
        Ink.Point first=points.get(0),last=points.get(n-1);float minX=first.x,maxX=first.x,minY=first.y,maxY=first.y;double length=0;
        for(int i=1;i<n;i++){Ink.Point a=points.get(i-1),b=points.get(i);length+=Math.hypot(b.x-a.x,b.y-a.y);minX=Math.min(minX,b.x);maxX=Math.max(maxX,b.x);minY=Math.min(minY,b.y);maxY=Math.max(maxY,b.y);}
        double chord=Math.hypot(last.x-first.x,last.y-first.y);float w=maxX-minX,h=maxY-minY;double diagonal=Math.hypot(w,h);
        if(diagonal<24||length<28)return null;
        Ink.Stroke result=source.copy(false);result.points.clear();result.smoothing=0;result.pen="ballpoint";
        if(chord>=28&&length/chord<1.13){
            double error=0;for(Ink.Point p:points)error=Math.max(error,Math.sqrt(Ink.distanceSquared(p.x,p.y,first.x,first.y,last.x,last.y)));
            if(error<=Math.max(2.4,chord*0.035)){float ex=last.x,ey=last.y;if(Math.abs(ey-first.y)<chord*0.09)ey=first.y;else if(Math.abs(ex-first.x)<chord*0.09)ex=first.x;
                result.points.add(new Ink.Point(first.x,first.y,1));result.points.add(new Ink.Point(ex,ey,1));return result;}
        }
        if(w<22||h<22||chord>diagonal*0.18||length<diagonal*2||length>diagonal*4.7)return null;
        int corners=0;double edges=0,ellipse=0;float cx=(minX+maxX)/2,cy=(minY+maxY)/2;
        for(Ink.Point p:points){float nx=(p.x-minX)/w,ny=(p.y-minY)/h;double edge=Math.min(Math.min(nx,1-nx),Math.min(ny,1-ny));edges+=edge*edge;
            if(Math.hypot(nx,ny)<0.16)corners|=1;if(Math.hypot(1-nx,ny)<0.16)corners|=2;if(Math.hypot(1-nx,1-ny)<0.16)corners|=4;if(Math.hypot(nx,1-ny)<0.16)corners|=8;
            double radial=Math.hypot((p.x-cx)/(w/2),(p.y-cy)/(h/2))-1;ellipse+=radial*radial;}
        if(corners==15&&Math.sqrt(edges/n)<0.055){
            result.points.add(new Ink.Point(minX,minY,1));result.points.add(new Ink.Point(maxX,minY,1));result.points.add(new Ink.Point(maxX,maxY,1));result.points.add(new Ink.Point(minX,maxY,1));result.points.add(new Ink.Point(minX,minY,1));return result;
        }
        if(Math.sqrt(ellipse/n)<0.14){for(int i=0;i<=80;i++){double a=2*Math.PI*i/80;result.points.add(new Ink.Point(cx+w/2*(float)Math.cos(a),cy+h/2*(float)Math.sin(a),1));}return result;}
        return null;
    }
}
