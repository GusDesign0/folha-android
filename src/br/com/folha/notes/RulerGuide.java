package br.com.folha.notes;

public final class RulerGuide {
    public float x1,y1,x2,y2;
    public void reset(float w,float h){x1=w*0.16f;x2=w*0.84f;y1=y2=h*0.55f;}
    public Ink.Point project(float x,float y,float pressure){float dx=x2-x1,dy=y2-y1,len=dx*dx+dy*dy;float t=len<1?0:((x-x1)*dx+(y-y1)*dy)/len;return new Ink.Point(x1+t*dx,y1+t*dy,pressure);}
    public void endpoint(int handle,float x,float y){float ox=handle==0?x2:x1,oy=handle==0?y2:y1;if(Math.hypot(x-ox,y-oy)<20)return;if(handle==0){x1=x;y1=y;}else{x2=x;y2=y;}}
    public void translate(float dx,float dy,float w,float h){dx=Math.max(-Math.min(x1,x2),Math.min(w-Math.max(x1,x2),dx));dy=Math.max(-Math.min(y1,y2),Math.min(h-Math.max(y1,y2),dy));x1+=dx;x2+=dx;y1+=dy;y2+=dy;}
}
