import br.com.folha.notes.Ink;
import java.util.ArrayList;
import java.util.List;

public class InkTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        Ink.Stroke line=new Ink.Stroke(0,2,false);
        line.points.add(new Ink.Point(0,0,1));line.points.add(new Ink.Point(100,0,1));
        check(line.hit(50,3,3),"Eraser must hit between sparse samples");
        check(!line.hit(50,8,3),"Eraser must preserve distant strokes");
        check(!line.hit(110,0,3),"Hit test must clamp to endpoints");
        Ink.Stroke dot=new Ink.Stroke(0,2,false);dot.points.add(new Ink.Point(20,20,1));
        check(dot.hit(20,20,1),"Single-point pen tap must be erasable");
        check(new Ink.Point(0,0,0).pressure==0.1f,"Pressure zero must remain visible");
        check(new Ink.Point(0,0,5).pressure==1.5f,"Pressure must be bounded");
        List<Ink.Stroke> state=new ArrayList<>();Ink.History history=new Ink.History();
        history.before(state);state.add(line);history.before(state);state.add(dot);
        check(history.undo(state)&&state.size()==1,"Undo stroke");
        check(history.redo(state)&&state.size()==2,"Redo stroke");
        history.before(state);state.clear();check(history.undo(state)&&state.size()==2,"Undo erasure");
        history.before(state);state.remove(0);check(!history.redo(state),"New edit invalidates redo");
        Ink.Stroke curve=new Ink.Stroke(0,2,false);curve.points.add(new Ink.Point(0,100,0.2f));curve.points.add(new Ink.Point(100,100,1.4f));curve.points.add(new Ink.Point(100,0,0.2f));
        check(curve.samples().get(0).x==0&&curve.samples().get(curve.samples().size()-1).y==0,"Smoothing must preserve endpoints");
        check(curve.hit(75,87.5f,0.5f),"Eraser must follow the visible rounded curve rather than the raw polyline");
        check(curve.points.size()==3&&curve.points.get(1).x==100&&curve.points.get(1).pressure==1.4f,"Smoothing must never overwrite saved raw input");
        Ink.Stroke jitter=new Ink.Stroke(0,3,false);for(int k=0;k<30;k++)jitter.points.add(new Ink.Point(k*0.5f,0,k%2==0?0.3f:1.2f));
        float delta=0;List<Ink.Point> samples=jitter.samples();for(int k=1;k<samples.size();k++){check(Float.isFinite(samples.get(k).x)&&Float.isFinite(samples.get(k).pressure),"Finite geometry");delta=Math.max(delta,Math.abs(samples.get(k).pressure-samples.get(k-1).pressure));}
        check(delta<0.5f,"Pressure jitter must not produce abrupt width jumps");
        Ink.Stroke stationary=new Ink.Stroke(0,4,false);stationary.points.add(new Ink.Point(10,10,0.2f));stationary.points.add(new Ink.Point(10,10,1));
        check(stationary.samples().size()==1&&stationary.samples().get(0).pressure>0.2f,"Stationary pressure must produce a finite rounded dot");
        int oldCount=curve.samples().size();curve.points.add(new Ink.Point(200,0,1));check(curve.samples().size()>oldCount,"A growing live stroke must refresh geometry");
        List<Ink.Stroke> fragments=Ink.cut(line,50,-5,50,5,2);check(fragments.size()==2,"Swept partial eraser must preserve both sides of a line");
        check(fragments.get(0).points.get(fragments.get(0).points.size()-1).x<50&&fragments.get(1).points.get(0).x>50,"Partial eraser must open a gap at the contact");
        check(line.points.size()==2,"Partial erasing must preserve the source for undo");
        Ink.Stroke straight=line.copy(false);straight.pen="ballpoint";check(Ink.penWidth(straight,new Ink.Point(0,0,0.1f))==Ink.penWidth(straight,new Ink.Point(0,0,1.5f)),"Ballpoint has stable width");
        straight.pen="brush";check(Ink.penWidth(straight,new Ink.Point(0,0,1.5f))>Ink.penWidth(straight,new Ink.Point(0,0,0.1f))*3,"Brush responds to pressure");
        Ink.Stroke moved=line.transform(0,0,30,40,2,false);check(moved.id.equals(line.id)&&moved.points.get(1).x==230&&line.points.get(1).x==100,"Selection transform preserves identity and does not mutate history");
        check(!line.copy(true).id.equals(line.id),"Copy gets its own persistent identity");
        System.out.println("21 scenarios passed: smooth ink, history, partial erasing, pen styles and immutable transforms.");
    }
}
