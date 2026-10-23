package br.com.folha.notes;
import java.util.*;
/** Font-independent filled contours, normalized to 1000 units. No executable SVG or font required. */
final class GlyphData {
 static List<float[]> contours(String data){if(data.length()>120000||!data.matches("[0-9 ,;]+"))throw new IllegalArgumentException("Contorno inválido");List<float[]> result=new ArrayList<>();int total=0;for(String row:data.split(";")){String[] values=row.trim().split("[ ,]+");if(values.length<6||values.length%2!=0)throw new IllegalArgumentException("Contorno incompleto");float[] points=new float[values.length];for(int i=0;i<values.length;i++){points[i]=Integer.parseInt(values[i]);if(points[i]<0||points[i]>1000)throw new IllegalArgumentException("Contorno fora dos limites");}total+=values.length/2;if(total>10000)throw new IllegalArgumentException("Contorno muito grande");result.add(points);}if(result.isEmpty())throw new IllegalArgumentException("Contorno vazio");return result;}
}
