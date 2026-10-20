package br.com.folha.notes;

public class GesturesShapesTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void start(TwoFingerTap tap){tap.begin(7,100,100,1000);tap.add(42,180,100,1070);}
    static Ink.Stroke rectangle(){Ink.Stroke s=new Ink.Stroke(0,3,false);float[][] xy={{50,50},{250,50},{250,180},{50,180},{50,50}};for(int i=1;i<xy.length;i++)for(int k=0;k<20;k++){float t=k/20f;s.points.add(new Ink.Point(xy[i-1][0]+(xy[i][0]-xy[i-1][0])*t,xy[i-1][1]+(xy[i][1]-xy[i-1][1])*t,0.6f));}s.points.add(new Ink.Point(50,50,0.5f));return s;}
    public static void main(String[] args){
        TwoFingerTap tap=new TwoFingerTap(8);start(tap);check(!tap.lift(42,1140,false)&&tap.lift(7,1180,true),"Two stationary fingers undo exactly once");check(!tap.lift(7,1190,true),"A gesture cannot fire twice");
        start(tap);check(!tap.lift(7,1140,false)&&tap.lift(42,1170,true),"Either finger can lift first");
        start(tap);tap.move(42,205,100,1100);tap.move(42,180,100,1120);tap.lift(42,1140,false);check(!tap.lift(7,1180,true),"Pinch out and back never becomes an undo");
        start(tap);tap.move(7,100,135,1100);tap.lift(7,1140,false);check(!tap.lift(42,1180,true),"Panning is not a two-finger tap");
        start(tap);tap.add(99,240,100,1100);tap.lift(42,1140,false);check(!tap.lift(7,1180,true),"A third finger invalidates undo");
        start(tap);tap.cancel();tap.lift(42,1140,false);check(!tap.lift(7,1180,true),"Palm/cancel cannot undo");
        start(tap);tap.lift(42,1450,false);check(!tap.lift(7,1470,true),"Holding two fingers cannot undo");
        tap.begin(7,100,100,1000);tap.add(42,180,100,1200);tap.lift(42,1240,false);check(!tap.lift(7,1280,true),"Fingers must arrive together");
        tap.begin(7,100,100,1000);check(!tap.lift(7,1100,true),"One finger never undoes");
        Ink.Stroke line=new Ink.Stroke(0xFF123456,2.5f,false);for(int i=0;i<=20;i++)line.points.add(new Ink.Point(10+i*6,50+(i%2==0?0:0.6f),0.7f));Ink.Stroke straight=ShapeRecognizer.recognize(line);
        check(straight!=null&&straight.points.size()==2&&straight.points.get(0).y==straight.points.get(1).y,"Held near-horizontal line snaps straight");check(line.points.size()==21&&straight.id.equals(line.id)&&straight.color==line.color&&straight.width==line.width,"Recognition preserves source samples and style identity");
        Ink.Stroke rectangle=rectangle(),rect=ShapeRecognizer.recognize(rectangle);check(rect!=null&&rect.points.size()==5&&rect.smoothing==0,"Closed rectangle retains sharp corners");
        Ink.Stroke ellipse=new Ink.Stroke(0,2,false);for(int k=0;k<=100;k++){double angle=k*2*Math.PI/100;ellipse.points.add(new Ink.Point(100+70*(float)Math.cos(angle),100+40*(float)Math.sin(angle),1));}Ink.Stroke oval=ShapeRecognizer.recognize(ellipse);check(oval!=null&&oval.points.size()==81,"Oval recognition");
        Ink.Stroke letter=new Ink.Stroke(0,3,false);float[][] scribble={{0,0},{10,30},{20,0},{30,30},{40,0}};for(float[] xy:scribble)letter.points.add(new Ink.Point(xy[0],xy[1],1));check(ShapeRecognizer.recognize(letter)==null,"Handwriting zigzag remains untouched");
        Ink.Stroke tiny=new Ink.Stroke(0,3,false);tiny.points.add(new Ink.Point(0,0,1));tiny.points.add(new Ink.Point(2,2,1));tiny.points.add(new Ink.Point(5,0,1));check(ShapeRecognizer.recognize(tiny)==null,"Small handwriting is not adjusted");
        Ink.Stroke open=rectangle();open.points.remove(open.points.size()-1);while(open.points.size()>65)open.points.remove(open.points.size()-1);check(ShapeRecognizer.recognize(open)==null,"Open box is not silently closed");
        RulerGuide ruler=new RulerGuide();ruler.x1=10;ruler.y1=20;ruler.x2=110;ruler.y2=120;Ink.Point projected=ruler.project(20,80,0.8f);check(Math.abs((projected.x-10)-(projected.y-20))<0.0001f&&projected.pressure==0.8f,"Rotated ruler projection preserves pressure");ruler.endpoint(0,109,119);check(ruler.x1==10,"Ruler cannot collapse");ruler.translate(1000,-1000,595,842);check(ruler.x2<=595&&ruler.y1>=0,"Ruler translation remains within page");
        System.out.println(checks+" scenarios passed: two-finger undo discrimination, held shapes and ruler geometry.");
    }
}
