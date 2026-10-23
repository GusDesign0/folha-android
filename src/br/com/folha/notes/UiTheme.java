package br.com.folha.notes;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/** Only application chrome is themed. Paper, ink, images and export never pass through this class. */
final class UiTheme {
    static String preference(MainActivity a){return a.getSharedPreferences("appearance",0).getString("theme","system");}
    static boolean resolve(MainActivity a){String p=preference(a);return p.equals("dark")||(p.equals("system")&&(a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
    static final int[] LIGHT={0xFFF7F6F2,0xFF263C32,0xFF596C60,0xFFDFE4DD,0xFFE8EDE7,0xFFFFFFFF,0xFFECF0E9,0xFFDCE8DC,0xFFF1F3EE,0xFFE4E7E1};
    static final int[] DARK ={0xFF151D1A,0xFFE7EFE9,0xFFA6B8AE,0xFF3B4B42,0xFF2A3830,0xFF1E2923,0xFF24342B,0xFF304F40,0xFF26352D,0xFF101713};
    static int color(boolean dark,int c){for(int i=0;i<LIGHT.length;i++)if(c==LIGHT[i]||c==DARK[i])return (dark?DARK:LIGHT)[i];return c;}
    static int text(MainActivity a,int c){return c==0xFFFFFFFF?c:c==MainActivity.GREEN?(a.dark?0xFF8BD1AF:MainActivity.GREEN):color(a.dark,c);}
    static void apply(MainActivity a,View v){
        if(v==null)return;
        boolean raw=Integer.valueOf(0).equals(v.getTag(R.id.ui_role));
        if(!raw&&v.getBackground() instanceof GradientDrawable){GradientDrawable d=(GradientDrawable)v.getBackground();if(d.getColor()!=null)d.setColor(color(a.dark,d.getColor().getDefaultColor()));}
        if(!raw&&v.getBackground() instanceof android.graphics.drawable.ColorDrawable){android.graphics.drawable.ColorDrawable d=(android.graphics.drawable.ColorDrawable)v.getBackground();d.setColor(color(a.dark,d.getColor()));}
        if(v instanceof TextView){TextView t=(TextView)v;Object role=t.getTag(R.id.ui_role);if(role instanceof Integer){if((Integer)role!=0)t.setTextColor(text(a,(Integer)role));}else{t.setTextColor(color(a.dark,MainActivity.INK));t.setHintTextColor(color(a.dark,MainActivity.MUTED));}}
        if(v instanceof EditText)v.setBackgroundTintList(ColorStateList.valueOf(text(a,MainActivity.GREEN)));
        if(v instanceof CompoundButton)((CompoundButton)v).setButtonTintList(ColorStateList.valueOf(text(a,MainActivity.GREEN)));
        if(v instanceof SeekBar){SeekBar b=(SeekBar)v;ColorStateList c=ColorStateList.valueOf(text(a,MainActivity.GREEN));b.setProgressTintList(c);b.setThumbTintList(c);}
        if(v instanceof NotebookView)v.setBackgroundColor(color(a.dark,0xFFE4E7E1));
        if(v instanceof ListView)v.setBackgroundColor(color(a.dark,0xFFE4E7E1));
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)apply(a,g.getChildAt(i));}
        v.invalidate();
    }
}
