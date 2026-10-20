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
        System.out.println("14 scenarios passed: eraser, history, rounded curves, raw-input preservation, pressure continuity and live geometry.");
    }
}
