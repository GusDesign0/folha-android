using System;using System.Linq;using System.Collections.Generic;
namespace Folha {
static class KanaPages {
 public const int Count=27;
 public static bool Is(string paper){return paper=="hiragana"||paper=="katakana";}
 public static void Initialize(Dictionary<string,object>d){var paper=J.S(d,"paper");if(!Is(paper))return;d["count"]=Count;d["pagePapers"]=Enumerable.Range(0,Count).ToDictionary(i=>i.ToString(),i=>(object)(paper+"-p"+i));}
 public static void Validate(Dictionary<string,object>d){if(!d.ContainsKey("pagePapers"))return;var map=d["pagePapers"] as Dictionary<string,object>;if(map==null||map.Count>10000)throw new Exception("Modelos de página inválidos.");foreach(var p in map){int i;if(!int.TryParse(p.Key,out i)||i<0||i>=J.I(d,"count")||!(p.Value is string)||((string)p.Value).Length>80)throw new Exception("Modelo de página inválido.");}}
 public static void GuardExisting(Dictionary<string,object>d,Dictionary<string,object>p){if(!p.ContainsKey("pagePapers"))return;var old=d.ContainsKey("pagePapers")?J.Obj(d["pagePapers"]):new Dictionary<string,object>();var next=J.Obj(p["pagePapers"]);for(int i=0;i<J.I(d,"count");i++){var key=i.ToString();if(J.S(old,key)!=J.S(next,key))throw new Exception("O modelo das páginas existentes é preservado.");}}
}
}
