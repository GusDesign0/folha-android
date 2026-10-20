package br.com.folha.notes;

import android.graphics.*;
import java.util.*;

final class SelectionEditor {
    final NotebookView view;
    final ArrayList<Ink.Stroke> selected=new ArrayList<>();
    final ArrayList<PointF> polygon=new ArrayList<>();
    final RectF box=new RectF();
    private float startX,startY,dx,dy,scale=1;
    boolean transforming=false,resizing=false,drawing=false;
    private static final ArrayList<Ink.Stroke> clipboard=new ArrayList<>();
    private static String clipboardOwner="";
    SelectionEditor(NotebookView v){view=v;}
    void clear(){selected.clear();polygon.clear();drawing=transforming=false;dx=dy=0;scale=1;view.refreshLayer();}
    void begin(float x,float y){
        bounds();float pad=14/(view.base*view.zoom);
        resizing=!selected.isEmpty()&&Math.hypot(x-box.right,y-box.bottom)<pad;
        transforming=!selected.isEmpty()&&(resizing||new RectF(box.left-pad,box.top-pad,box.right+pad,box.bottom+pad).contains(x,y));
        startX=x;startY=y;dx=dy=0;scale=1;
        if(transforming){view.refreshLayer();return;}
        selected.clear();polygon.clear();polygon.add(new PointF(x,y));drawing=true;view.invalidate();
    }
    void move(float x,float y){
        if(transforming){
            if(resizing){float limit=Math.min((view.pageW-box.left)/Math.max(1,box.width()),(view.pageH-box.top)/Math.max(1,box.height()));scale=Math.max(0.1f,Math.min(limit,Math.max(x-box.left,y-box.top)/Math.max(box.width(),box.height())));}
            else{dx=Math.max(-box.left,Math.min(view.pageW-box.right,x-startX));dy=Math.max(-box.top,Math.min(view.pageH-box.bottom,y-startY));}
        }else if(drawing){PointF last=polygon.get(polygon.size()-1);if(Math.hypot(x-last.x,y-last.y)>2){if(polygon.size()>=256){for(int i=polygon.size()-2;i>0;i-=2)polygon.remove(i);}polygon.add(new PointF(x,y));}}
        view.invalidate();
    }
    void end(){
        if(transforming){
            if(Math.abs(dx)+Math.abs(dy)+Math.abs(scale-1)>0.001f){view.history.before(view.strokes);ArrayList<Ink.Stroke> fresh=new ArrayList<>();
                for(Ink.Stroke s:selected){Ink.Stroke t=s.transform(box.left,box.top,dx,dy,scale,false);int i=view.strokes.indexOf(s);if(i>=0)view.strokes.set(i,t);fresh.add(t);}selected.clear();selected.addAll(fresh);view.documentChanged();}
            transforming=false;dx=dy=0;scale=1;bounds();view.refreshLayer();
        }else if(drawing){drawing=false;if(polygon.size()>2){
            RectF area=new RectF(polygon.get(0).x,polygon.get(0).y,polygon.get(0).x,polygon.get(0).y);for(PointF p:polygon)area.union(p.x,p.y);
            for(Ink.Stroke s:view.strokes){float[] b=s.bounds();if(!RectF.intersects(area,new RectF(b[0],b[1],b[2],b[3])))continue;
                boolean hit=inside(b[0],b[1])&&inside(b[2],b[3]);
                if(!hit&&s.kind.equals("ink")){List<Ink.Point> samples=s.samples();for(int i=0;i<samples.size();i+=4){Ink.Point p=samples.get(i);if(inside(p.x,p.y)){hit=true;break;}}}
                else if(!hit)hit=inside((b[0]+b[2])/2,(b[1]+b[3])/2);
                if(hit)selected.add(s);
            }
        }polygon.clear();bounds();view.invalidate();}
    }
    void cancel(){drawing=transforming=false;polygon.clear();dx=dy=0;scale=1;view.refreshLayer();}
    private boolean inside(float x,float y){boolean hit=false;for(int i=0,j=polygon.size()-1;i<polygon.size();j=i++){PointF a=polygon.get(i),b=polygon.get(j);if((a.y>y)!=(b.y>y)&&x<(b.x-a.x)*(y-a.y)/(b.y-a.y)+a.x)hit=!hit;}return hit;}
    void bounds(){box.setEmpty();boolean first=true;for(Ink.Stroke s:selected){float[] b=s.bounds();if(first){box.set(b[0],b[1],b[2],b[3]);first=false;}else box.union(b[0],b[1],b[2],b[3]);}}
    void draw(Canvas c,float screenScale,Map<String,Bitmap> images){
        if(transforming){c.save();c.translate(box.left+dx,box.top+dy);c.scale(scale,scale);c.translate(-box.left,-box.top);PageArt.ink(c,selected,images);c.restore();}
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFF326B56);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f/screenScale);p.setPathEffect(new DashPathEffect(new float[]{6/screenScale,4/screenScale},0));
        if(drawing&&!polygon.isEmpty()){Path path=new Path();path.moveTo(polygon.get(0).x,polygon.get(0).y);for(PointF q:polygon)path.lineTo(q.x,q.y);c.drawPath(path,p);}
        if(!selected.isEmpty()){
            RectF visible=new RectF(box.left+dx,box.top+dy,box.left+dx+box.width()*scale,box.top+dy+box.height()*scale);c.drawRect(visible,p);p.setPathEffect(null);p.setStyle(Paint.Style.FILL);c.drawCircle(visible.right,visible.bottom,6/screenScale,p);
        }
    }
    void recolor(int color){if(selected.isEmpty())return;view.history.before(view.strokes);ArrayList<Ink.Stroke> fresh=new ArrayList<>();for(Ink.Stroke s:selected){Ink.Stroke t=s.copy(false);if(!t.kind.equals("image"))t.color=color;int i=view.strokes.indexOf(s);if(i>=0)view.strokes.set(i,t);fresh.add(t);}selected.clear();selected.addAll(fresh);view.documentChanged();}
    void delete(){if(selected.isEmpty())return;view.history.before(view.strokes);view.strokes.removeAll(selected);selected.clear();view.documentChanged();}
    boolean copy(String owner){if(selected.isEmpty())return false;clipboard.clear();for(Ink.Stroke s:selected)clipboard.add(s.copy(true));clipboardOwner=owner;return true;}
    boolean paste(String owner){if(clipboard.isEmpty()||!clipboardOwner.equals(owner))return false;view.history.before(view.strokes);selected.clear();for(Ink.Stroke s:clipboard){Ink.Stroke t=s.transform(0,0,18,18,1,true);view.strokes.add(t);selected.add(t);}bounds();view.documentChanged();return true;}
    void select(Ink.Stroke s){selected.clear();selected.add(s);bounds();view.invalidate();}
}
