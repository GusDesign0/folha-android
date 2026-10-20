package br.com.folha.notes;

/** A short, stationary two-finger chord. No allocations in the touch path. */
public final class TwoFingerTap {
    private final float slop2;
    private int first=-1,second=-1;
    private float ax,ay,bx,by;
    private long began;
    private boolean valid,firstUp,secondUp;
    public TwoFingerTap(float slop){slop2=slop*slop;}
    public void begin(int id,float x,float y,long time){first=id;second=-1;ax=x;ay=y;began=time;valid=true;firstUp=secondUp=false;}
    public void add(int id,float x,float y,long time){if(!valid||second!=-1||id==first||time-began>180){cancel();return;}second=id;bx=x;by=y;}
    public void move(int id,float x,float y,long time){if(!valid)return;if(time-began>380){cancel();return;}float dx,dy;if(id==first){dx=x-ax;dy=y-ay;}else if(id==second){dx=x-bx;dy=y-by;}else{cancel();return;}if(dx*dx+dy*dy>slop2)cancel();}
    public boolean lift(int id,long time,boolean last){if(!valid)return false;if(time-began>380||second==-1){cancel();return false;}if(id==first)firstUp=true;else if(id==second)secondUp=true;else cancel();boolean result=valid&&last&&firstUp&&secondUp;if(last)cancel();return result;}
    public void cancel(){valid=false;}
}
