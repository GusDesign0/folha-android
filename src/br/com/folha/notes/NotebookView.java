package br.com.folha.notes;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

final class NotebookView extends View {
    interface Listener {void changed();void zoom(float value);}
    final Listener listener;
    final List<Ink.Stroke> strokes;
    final Ink.History history=new Ink.History();
    String paper;
    Bitmap pdf;
    float pageW=595,pageH=842;
    float zoom=1,panX=0,panY=0,base=1,left,top;
    int tool=0,color=Color.rgb(36,53,47);
    float penWidth=2.2f;
    String pen="fountain";
    float smoothing=1,sensitivity=1;
    boolean partialEraser=true;
    boolean autoShapes=true,rulerEnabled=false;
    final RulerGuide ruler=new RulerGuide();
    final TwoFingerTap twoTap;
    private int rulerDrag=-1;
    private boolean drawingWithRuler=false;
    private Ink.Stroke heldRaw;
    private float holdX,holdY,tapZoom,tapPanX,tapPanY;
    private long lastStylusTime=-1000;
    private final Runnable shapeHold=()->snapShape();
    private void snapShape(){
        if(activeId<0||pending==null||heldRaw!=null||drawingWithRuler||!autoShapes)return;
        Ink.Stroke adjusted=ShapeRecognizer.recognize(pending);
        if(adjusted!=null){heldRaw=pending;pending=adjusted;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);invalidate();}
    }
    final SelectionEditor selection;
    Map<String,Bitmap> images=new HashMap<>();
    boolean touchInk=false,ready=true;
    private Ink.Stroke pending;
    private int activeId=-1;
    private boolean erasing=false,eraseChanged=false;
    private List<Ink.Stroke> beforeErase;
    private float lastX,lastY;
    private float eraseLastX,eraseLastY;
    private boolean selecting=false;
    private boolean dragging=false,wasScaling=false,activeFinger=false;
    private final ScaleGestureDetector scaler;
    private Bitmap inkCache;
    private float cachedLeft=Float.NaN,cachedTop,cachedScale;
    private boolean lowMemoryCache=false;
    private boolean cacheDirty=true;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private float hoverX=-1,hoverY=-1;

    NotebookView(Context c,List<Ink.Stroke> strokes,String paper,Listener listener) {
        super(c);this.strokes=strokes;this.paper=paper;this.listener=listener;selection=new SelectionEditor(this);
        twoTap=new TwoFingerTap(ViewConfiguration.get(c).getScaledTouchSlop());ruler.reset(pageW,pageH);
        setBackgroundColor(Color.rgb(230,234,231));setContentDescription("Página do caderno. S Pen escreve. Dois dedos ampliam; um toque curto com dois dedos desfaz.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        scaler=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector d){wasScaling=true;return activeId==-1;}
            @Override public boolean onScale(ScaleGestureDetector d){
                geometry();float px=(d.getFocusX()-left)/(base*zoom),py=(d.getFocusY()-top)/(base*zoom);
                zoom=Math.max(1,Math.min(5,zoom*d.getScaleFactor()));
                panX=d.getFocusX()-(getWidth()-pageW*base*zoom)/2-px*base*zoom;
                panY=d.getFocusY()-(getHeight()-pageH*base*zoom)/2-py*base*zoom;
                constrain();invalidate();listener.zoom(zoom);return true;
            }
        });
    }
    private float density(){return getResources().getDisplayMetrics().density;}
    void setPdf(PdfFiles.Rendered image){pdf=image.bitmap;pageW=image.width;pageH=image.height;ruler.reset(pageW,pageH);ready=true;cacheDirty=true;invalidate();}
    void fit(){zoom=1;panX=panY=0;listener.zoom(zoom);invalidate();}
    void dispose(){removeCallbacks(shapeHold);twoTap.cancel();if(pdf!=null){pdf.recycle();pdf=null;}if(inkCache!=null){inkCache.recycle();inkCache=null;}MediaFiles.recycle(images);}
    void setRuler(boolean enabled){finishInput();rulerEnabled=enabled;rulerDrag=-1;twoTap.cancel();invalidate();}
    void setImages(Map<String,Bitmap> next){MediaFiles.recycle(images);images=next;refreshLayer();}
    void refreshLayer(){cacheDirty=true;invalidate();}
    void documentChanged(){changed();}
    void addObject(Ink.Stroke item){history.before(strokes);strokes.add(item);selection.select(item);changed();}
    void undo(){cancelInput();selection.clear();if(history.undo(strokes)){changed();}}
    void redo(){cancelInput();selection.clear();if(history.redo(strokes)){changed();}}
    void finishInput(){if(pending!=null||erasing||selecting)commit();}
    private void changed(){cacheDirty=true;listener.changed();invalidate();}
    private void appendInk(Ink.Stroke stroke){
        geometry();float scale=base*zoom;
        if(inkCache!=null&&!cacheDirty&&cachedLeft==left&&cachedTop==top&&cachedScale==scale){Canvas c=new Canvas(inkCache);c.scale((float)inkCache.getWidth()/Math.max(1,getWidth()),(float)inkCache.getHeight()/Math.max(1,getHeight()));c.translate(left,top);c.scale(scale,scale);c.clipRect(0,0,pageW,pageH);PageArt.stroke(c,stroke,images);}else cacheDirty=true;
        listener.changed();invalidate();
    }
    private void geometry(){
        float margin=18*density();base=Math.max(0.01f,Math.min((getWidth()-margin*2)/pageW,(getHeight()-margin*2)/pageH));
        left=(getWidth()-pageW*base*zoom)/2+panX;top=(getHeight()-pageH*base*zoom)/2+panY;
    }
    private void constrain(){
        geometry();float maxX=Math.max(0,(pageW*base*zoom-getWidth())/2+40*density());float maxY=Math.max(0,(pageH*base*zoom-getHeight())/2+40*density());
        panX=Math.max(-maxX,Math.min(maxX,panX));panY=Math.max(-maxY,Math.min(maxY,panY));geometry();
    }
    private void cache(){
        // Cache the visible area at up to 2x screen resolution, rather than stretching a fixed page image.
        int vw=Math.max(1,getWidth()),vh=Math.max(1,getHeight());
        float aa=lowMemoryCache?1f:Math.max(1f,Math.min(2f,(float)Math.sqrt(8000000d/((double)vw*vh))));
        int w=Math.max(1,Math.round(vw*aa)),h=Math.max(1,Math.round(vh*aa));
        if(inkCache==null||inkCache.getWidth()!=w||inkCache.getHeight()!=h){
            if(inkCache!=null)inkCache.recycle();
            try{inkCache=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);}catch(OutOfMemoryError e){lowMemoryCache=true;inkCache=Bitmap.createBitmap(vw,vh,Bitmap.Config.ARGB_8888);}
            cacheDirty=true;
        }
        float scale=base*zoom;
        if(cacheDirty||cachedLeft!=left||cachedTop!=top||cachedScale!=scale){
            inkCache.eraseColor(Color.TRANSPARENT);Canvas c=new Canvas(inkCache);
            c.scale((float)inkCache.getWidth()/vw,(float)inkCache.getHeight()/vh);c.translate(left,top);c.scale(scale,scale);c.clipRect(0,0,pageW,pageH);
            for(Ink.Stroke s:strokes)if(!selection.transforming||!selection.selected.contains(s))PageArt.stroke(c,s,images);
            cachedLeft=left;cachedTop=top;cachedScale=scale;cacheDirty=false;
        }
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);geometry();float scale=base*zoom;
        paint.setColor(Color.rgb(207,213,209));c.drawRoundRect(left-2*density(),top+3*density(),left+pageW*scale+2*density(),top+pageH*scale+6*density(),3*density(),3*density(),paint);
        int saved=c.save();c.translate(left,top);c.scale(scale,scale);c.clipRect(0,0,pageW,pageH);
        if(pdf!=null)c.drawBitmap(pdf,null,new RectF(0,0,pageW,pageH),paint);else PageArt.paper(c,pageW,pageH,paper);
        c.restoreToCount(saved);
        cache();c.drawBitmap(inkCache,null,new RectF(0,0,getWidth(),getHeight()),paint);
        if(pending!=null){saved=c.save();c.translate(left,top);c.scale(scale,scale);c.clipRect(0,0,pageW,pageH);PageArt.stroke(c,pending);c.restoreToCount(saved);}
        if(tool==4){saved=c.save();c.translate(left,top);c.scale(scale,scale);selection.draw(c,scale,images);c.restoreToCount(saved);}
        if(rulerEnabled){saved=c.save();c.translate(left,top);c.scale(scale,scale);drawRuler(c,scale);c.restoreToCount(saved);}
        if(!ready){paint.setColor(Color.rgb(50,107,86));paint.setTextSize(15*density());paint.setTypeface(Typeface.DEFAULT);c.drawText("Carregando PDF…",24*density(),36*density(),paint);}
        if(hoverX>=0&&tool<3){paint.setColor(0x90608070);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density());float radius=(tool==2?10:tool==1?penWidth*4:penWidth)*scale;c.drawCircle(hoverX,hoverY,Math.max(3*density(),radius),paint);paint.setStyle(Paint.Style.FILL);}
    }
    @Override public boolean onHoverEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT){hoverX=hoverY=-1;}else{hoverX=e.getX();hoverY=e.getY();}invalidate();return true;}
    private boolean stylus(MotionEvent e,int i){int t=e.getToolType(i);return t==MotionEvent.TOOL_TYPE_STYLUS||t==MotionEvent.TOOL_TYPE_ERASER;}
    private boolean tapEvent(MotionEvent e){
        int action=e.getActionMasked(),idx=e.getActionIndex();long time=e.getEventTime();
        for(int i=0;i<e.getPointerCount();i++)if(e.getToolType(i)!=MotionEvent.TOOL_TYPE_FINGER){lastStylusTime=time;twoTap.cancel();return false;}
        if(action==MotionEvent.ACTION_CANCEL||(e.getFlags()&MotionEvent.FLAG_CANCELED)!=0){twoTap.cancel();return false;}
        if(action==MotionEvent.ACTION_DOWN){twoTap.begin(e.getPointerId(0),e.getX(),e.getY(),time);tapZoom=zoom;tapPanX=panX;tapPanY=panY;if(time-lastStylusTime<120)twoTap.cancel();}
        if(action==MotionEvent.ACTION_POINTER_DOWN)twoTap.add(e.getPointerId(idx),e.getX(idx),e.getY(idx),time);
        for(int i=0;i<e.getPointerCount();i++){for(int h=0;h<e.getHistorySize();h++)twoTap.move(e.getPointerId(i),e.getHistoricalX(i,h),e.getHistoricalY(i,h),e.getHistoricalEventTime(h));twoTap.move(e.getPointerId(i),e.getX(i),e.getY(i),time);}
        if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP)return twoTap.lift(e.getPointerId(idx),time,action==MotionEvent.ACTION_UP);
        return false;
    }
    private void hold(MotionEvent e,int i,boolean start){
        float x=e.getX(i),y=e.getY(i),slop=3*density();
        if(start||(x-holdX)*(x-holdX)+(y-holdY)*(y-holdY)>slop*slop){
            if(heldRaw!=null){pending=heldRaw;heldRaw=null;}
            holdX=x;holdY=y;removeCallbacks(shapeHold);if(autoShapes&&pending!=null&&!drawingWithRuler)postDelayed(shapeHold,600);
        }
    }
    private boolean rulerTouch(MotionEvent e){
        if(!rulerEnabled)return false;int a=e.getActionMasked();if(e.getPointerCount()>1){rulerDrag=-1;return false;}
        if(e.getToolType(0)!=MotionEvent.TOOL_TYPE_FINGER)return false;
        float x=Math.max(0,Math.min(pageW,(e.getX()-left)/(base*zoom))),y=Math.max(0,Math.min(pageH,(e.getY()-top)/(base*zoom)));
        if(a==MotionEvent.ACTION_DOWN){float radius=22*density()/(base*zoom);if(Math.hypot(x-ruler.x1,y-ruler.y1)<radius)rulerDrag=0;else if(Math.hypot(x-ruler.x2,y-ruler.y2)<radius)rulerDrag=1;else if(Ink.distanceSquared(x,y,ruler.x1,ruler.y1,ruler.x2,ruler.y2)<radius*radius/4)rulerDrag=2;else rulerDrag=-1;lastX=x;lastY=y;}
        if(rulerDrag<0)return false;twoTap.cancel();
        if(a==MotionEvent.ACTION_MOVE){if(rulerDrag<2)ruler.endpoint(rulerDrag,x,y);else ruler.translate(x-lastX,y-lastY,pageW,pageH);lastX=x;lastY=y;invalidate();}
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)rulerDrag=-1;
        return true;
    }
    private void drawRuler(Canvas c,float scale){
        float radius=9*density()/scale,dx=ruler.x2-ruler.x1,dy=ruler.y2-ruler.y1,len=(float)Math.hypot(dx,dy);int saved=c.save();c.translate(ruler.x1,ruler.y1);c.rotate((float)Math.toDegrees(Math.atan2(dy,dx)));
        paint.setStyle(Paint.Style.FILL);paint.setColor(0x55E6B848);c.drawRect(0,-radius*2,len,0,paint);paint.setColor(0xFF8C742A);paint.setStrokeWidth(density()/scale);c.drawLine(0,0,len,0,paint);paint.setTextSize(9*density()/scale);paint.setTypeface(Typeface.DEFAULT);
        for(int i=0;i*14.1732f<len;i++){float x=i*14.1732f;c.drawLine(x,0,x,-radius*(i%2==0?1.15f:0.6f),paint);if(i%2==0)c.drawText(String.valueOf(i/2),x+radius*0.18f,-radius*1.25f,paint);}c.restoreToCount(saved);
        paint.setColor(0xFFE6B848);c.drawCircle(ruler.x1,ruler.y1,radius,paint);c.drawCircle(ruler.x2,ruler.y2,radius,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF8C742A);c.drawCircle(ruler.x1,ruler.y1,radius,paint);c.drawCircle(ruler.x2,ruler.y2,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(!ready)return true;geometry();int a=e.getActionMasked(),idx=e.getActionIndex();
        if(tapEvent(e)){scaler.onTouchEvent(e);undo();zoom=tapZoom;panX=tapPanX;panY=tapPanY;constrain();listener.zoom(zoom);wasScaling=false;dragging=false;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);return true;}
        if(a==MotionEvent.ACTION_CANCEL){cancelInput();rulerDrag=-1;dragging=false;return true;}
        if(activeId!=-1&&activeFinger&&a==MotionEvent.ACTION_POINTER_DOWN){cancelInput();wasScaling=true;dragging=false;}
        if(activeId!=-1) {
            int p=e.findPointerIndex(activeId);if(p<0){cancelInput();return true;}
            if((e.getFlags()&MotionEvent.FLAG_CANCELED)!=0&&(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_POINTER_UP)&&e.getPointerId(idx)==activeId){cancelInput();return true;}
            if(a==MotionEvent.ACTION_MOVE){hold(e,p,false);sample(e,p);invalidate();}
            if((a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_POINTER_UP)&&e.getPointerId(idx)==activeId){sample(e,p);commit();}
            return true;
        }
        if(rulerTouch(e))return true;
        if((a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_POINTER_DOWN)&&tool!=3&&(stylus(e,idx)||(touchInk&&e.getPointerCount()==1))) {
            float x=(e.getX(idx)-left)/(base*zoom),y=(e.getY(idx)-top)/(base*zoom);
            if(x<0||y<0||x>pageW||y>pageH)return true;
            activeId=e.getPointerId(idx);erasing=tool==2||e.getToolType(idx)==MotionEvent.TOOL_TYPE_ERASER||(e.getButtonState()&MotionEvent.BUTTON_STYLUS_PRIMARY)!=0;
            if(stylus(e,idx))requestUnbufferedDispatch(e);
            activeFinger=!stylus(e,idx);
            selecting=tool==4;
            if(selecting){erasing=false;selection.begin(x,y);}
            else if(erasing){beforeErase=new ArrayList<>(strokes);eraseChanged=false;eraseLastX=x;eraseLastY=y;}
            else{pending=new Ink.Stroke(color,tool==1?penWidth*7:penWidth,tool==1);pending.pen=pen;pending.smoothing=smoothing;pending.sensitivity=sensitivity;drawingWithRuler=rulerEnabled&&tool<=1;if(drawingWithRuler){pending.pen="ballpoint";pending.smoothing=0;}}
            sample(e,idx);hold(e,idx,true);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
        }
        // A second finger cancels optional finger-writing and enters navigation.
        if(!stylus(e,idx)) {
            scaler.onTouchEvent(e);
            if(a==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();dragging=true;wasScaling=false;}
            if(a==MotionEvent.ACTION_POINTER_DOWN){dragging=false;wasScaling=true;}
            if(a==MotionEvent.ACTION_MOVE&&dragging&&!scaler.isInProgress()&&!wasScaling){panX+=e.getX()-lastX;panY+=e.getY()-lastY;lastX=e.getX();lastY=e.getY();constrain();invalidate();}
            if(a==MotionEvent.ACTION_UP){dragging=false;performClick();}
        }else if(tool==3){
            if(a==MotionEvent.ACTION_DOWN){dragging=true;lastX=e.getX();lastY=e.getY();}
            if(a==MotionEvent.ACTION_MOVE&&dragging){panX+=e.getX()-lastX;panY+=e.getY()-lastY;lastX=e.getX();lastY=e.getY();constrain();invalidate();}
            if(a==MotionEvent.ACTION_UP)dragging=false;
        }
        return true;
    }
    private void sample(MotionEvent e,int i) {
        for(int h=0;h<e.getHistorySize();h++)point(e.getHistoricalX(i,h),e.getHistoricalY(i,h),e.getHistoricalPressure(i,h));
        point(e.getX(i),e.getY(i),e.getPressure(i));
    }
    private void point(float sx,float sy,float pressure) {
        if(heldRaw!=null)return;
        float x=(sx-left)/(base*zoom),y=(sy-top)/(base*zoom);
        if(x<0||y<0||x>pageW||y>pageH)return;
        if(selecting){selection.move(x,y);return;}
        if(erasing){boolean removed=false;float radius=11/(base*zoom);
            for(int j=strokes.size()-1;j>=0;j--){Ink.Stroke s=strokes.get(j);if(!s.kind.equals("ink"))continue;float[] b=s.bounds();if(b[2]<Math.min(x,eraseLastX)-radius||b[0]>Math.max(x,eraseLastX)+radius||b[3]<Math.min(y,eraseLastY)-radius||b[1]>Math.max(y,eraseLastY)+radius)continue;
                List<Ink.Stroke> fragments=Ink.cut(s,eraseLastX,eraseLastY,x,y,radius);if(fragments.size()!=1||fragments.get(0)!=s){strokes.remove(j);if(partialEraser)strokes.addAll(j,fragments);removed=true;}}
            eraseLastX=x;eraseLastY=y;
            if(removed){eraseChanged=true;cacheDirty=true;}
        }else if(pending!=null){
            if(drawingWithRuler){Ink.Point p=ruler.project(x,y,pressure);if(pending.points.size()<2)pending.points.add(p);else pending.points.set(1,p);return;}
            int n=pending.points.size();if(n>0){Ink.Point last=pending.points.get(n-1);if(Ink.distanceSquared(x,y,last.x,last.y,last.x,last.y)<0.04f&&Math.abs(last.pressure-pressure)<0.03f)return;}
            pending.points.add(new Ink.Point(x,y,pressure));
        }
    }
    private void commit(){
        removeCallbacks(shapeHold);heldRaw=null;drawingWithRuler=false;
        if(selecting){selection.end();selecting=false;}
        if(pending!=null&&!pending.points.isEmpty()){history.before(strokes);Ink.Stroke finished=pending;strokes.add(finished);pending=null;appendInk(finished);}
        if(erasing&&eraseChanged){history.before(beforeErase);changed();}
        pending=null;erasing=false;eraseChanged=false;beforeErase=null;activeId=-1;dragging=false;invalidate();
    }
    private void cancelInput(){removeCallbacks(shapeHold);heldRaw=null;drawingWithRuler=false;if(selecting){selection.cancel();selecting=false;}if(erasing&&beforeErase!=null){strokes.clear();strokes.addAll(beforeErase);cacheDirty=true;}pending=null;beforeErase=null;erasing=false;activeId=-1;eraseChanged=false;invalidate();}
    @Override public boolean performClick(){super.performClick();return true;}
}
