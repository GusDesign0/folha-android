package br.com.folha.notes;
import java.util.*;
public class StudyPaperTest {
 public static void main(String[] args){
  if(Paper.IDS.length!=19||Paper.NAMES.length!=19||StudyPaper.LINES.size()!=10)throw new AssertionError("Template count");
  Set<String> ids=new HashSet<>();StringBuilder json=new StringBuilder("[");
  for(String id:Paper.IDS){if(!ids.add(id)||StudyPaper.name(id).isEmpty()||StudyPaper.icon(id)==null)throw new AssertionError("Missing identity "+id);}
  for(String id:Paper.IDS){if(!StudyPaper.LINES.containsKey(id))continue;if(json.length()>1)json.append(',');json.append("{\"id\":\"").append(id).append("\",\"lines\":[");int i=0;for(float[] l:StudyPaper.LINES.get(id)){if(i++>0)json.append(',');json.append(Arrays.toString(l));for(int j=0;j<4;j++)if(!Float.isFinite(l[j])||l[j]<0||l[j]>(j%2==0?595:842))throw new AssertionError("Bounds "+id);}
   json.append("],\"labels\":[");i=0;for(StudyPaper.Label l:StudyPaper.LABELS.get(id)){if(l.x<0||l.y>842||l.size<8||l.text.isEmpty())throw new AssertionError("Label "+id);if(i++>0)json.append(',');json.append("{\"text\":\"").append(l.text).append("\",\"x\":").append(l.x).append(",\"y\":").append(l.y).append(",\"size\":").append(l.size).append('}');}json.append("]}");}
  float[] cell=JapaneseGeometry.cell("kanji-study",595,842,55,350);if(cell==null||Math.abs(cell[0]-34)>.01||Math.abs(cell[1]-334)>.01)throw new AssertionError("Kanji study grid mapping");if(JapaneseGeometry.cell("kanji-study",595,842,200,300)!=null)throw new AssertionError("Outside practice grid");
  System.out.print(json.append(']'));
 }
}
