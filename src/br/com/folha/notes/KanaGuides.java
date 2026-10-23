package br.com.folha.notes;
import android.graphics.*;
/** Build background guide paths only on a paper-cache miss, never in pen input. */
final class KanaGuides {
 static void add(Path[] paths,String type){StudyPaper.Guide[] guides=StudyPaper.GUIDES.get(type);if(guides==null||guides.length==0)return;Matrix matrix=new Matrix();for(StudyPaper.Guide g:guides){for(int i=0;i<g.text.length();i++){String glyph=KanaGlyphs.DATA.get(g.text.substring(i,i+1));if(glyph==null)continue;matrix.setScale(g.size/1000,g.size/1000);matrix.postTranslate(g.x+i*g.size,g.y);paths[g.trace?3:4].addPath(JapaneseGlyph.path(glyph),matrix);}}}
}
