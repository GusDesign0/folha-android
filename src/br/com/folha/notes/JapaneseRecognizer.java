package br.com.folha.notes;
import com.google.mlkit.vision.digitalink.recognition.*;
import com.google.mlkit.common.model.*;
import java.util.*;
import java.util.concurrent.Executor;

/** Optional local recognizer. The language model is only requested when the user enables Regular. */
final class JapaneseRecognizer implements AutoCloseable {
 interface Ready {void done(Exception error);}
 interface Result {void done(List<String> candidates,Exception error);}
 final Executor executor;final DigitalInkRecognitionModel model;final RemoteModelManager manager;
 volatile DigitalInkRecognizer recognizer;volatile boolean closed;
 JapaneseRecognizer(Executor executor)throws Exception{this.executor=executor;DigitalInkRecognitionModelIdentifier id=DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja");if(id==null)throw new IllegalStateException("Modelo japonês indisponível");model=DigitalInkRecognitionModel.builder(id).build();manager=RemoteModelManager.getInstance();}
 void prepare(boolean download,Ready callback){manager.isModelDownloaded(model).addOnSuccessListener(executor,available->{if(closed)return;if(available){start();callback.done(null);}else if(download)manager.download(model,new DownloadConditions.Builder().build()).addOnSuccessListener(executor,v->{if(!closed){start();callback.done(null);}}).addOnFailureListener(executor,callback::done);else callback.done(new IllegalStateException("Baixe o modelo japonês para usar Regular."));}).addOnFailureListener(executor,callback::done);}
 synchronized void start(){if(!closed&&recognizer==null)recognizer=DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).setExecutor(executor).setMaxResultCount(5).build());}
 void recognize(List<br.com.folha.notes.Ink.Stroke> strokes,Result callback){try{DigitalInkRecognizer client=recognizer;if(closed||client==null){callback.done(Collections.emptyList(),new IllegalStateException("Modelo ainda não está pronto."));return;}float[] b=JapaneseGeometry.bounds(strokes);com.google.mlkit.vision.digitalink.recognition.Ink.Builder input=com.google.mlkit.vision.digitalink.recognition.Ink.builder();for(br.com.folha.notes.Ink.Stroke s:strokes){com.google.mlkit.vision.digitalink.recognition.Ink.Stroke.Builder stroke=com.google.mlkit.vision.digitalink.recognition.Ink.Stroke.builder();int step=Math.max(1,(s.points.size()+254)/255);for(int i=0;i<s.points.size();i+=step){br.com.folha.notes.Ink.Point p=s.points.get(i);stroke.addPoint(com.google.mlkit.vision.digitalink.recognition.Ink.Point.create(p.x-b[0]+10,p.y-b[1]+10));}br.com.folha.notes.Ink.Point end=s.points.get(s.points.size()-1);stroke.addPoint(com.google.mlkit.vision.digitalink.recognition.Ink.Point.create(end.x-b[0]+10,end.y-b[1]+10));input.addStroke(stroke.build());}
   RecognitionContext context=RecognitionContext.builder().setPreContext("").setWritingArea(new WritingArea(Math.max(40,b[2]-b[0]+20),Math.max(30,b[3]-b[1]+20))).build();client.recognize(input.build(),context).addOnSuccessListener(executor,result->{List<String> list=new ArrayList<>();if(result.getCandidates().isEmpty()||!JapaneseGeometry.japanese(result.getCandidates().get(0).getText().trim())){callback.done(list,null);return;}for(com.google.mlkit.vision.digitalink.common.RecognitionCandidate c:result.getCandidates()){String text=c.getText().trim();if(JapaneseGeometry.japanese(text)&&!list.contains(text))list.add(text);if(list.size()>=5)break;}callback.done(list,null);}).addOnFailureListener(executor,e->callback.done(Collections.emptyList(),e));
  }catch(Exception e){callback.done(Collections.emptyList(),e);}}
 @Override public synchronized void close(){closed=true;if(recognizer!=null){DigitalInkRecognizer previous=recognizer;recognizer=null;try{executor.execute(previous::close);}catch(java.util.concurrent.RejectedExecutionException e){previous.close();}}}
}
