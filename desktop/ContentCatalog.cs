using System;using System.IO;using System.Linq;using System.Collections.Generic;
namespace Folha {
static class ContentCatalog {
 static string Root { get { return Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"content"); } }
 static bool Safe(string s){return System.Text.RegularExpressions.Regex.IsMatch(s,@"^[a-z0-9-]+\.(pdf|png)$");}
 public static Dictionary<string,object>[] Items(){
  var catalog=J.Parse(File.ReadAllText(Path.Combine(Root,"catalog.json")));if(J.I(catalog,"version")!=1)throw new Exception("Catálogo incompatível.");
  var items=J.List(catalog["items"]).Select(J.Obj).ToArray();
  foreach(var e in items)if(!J.ValidId(J.S(e,"documentId"))||!Safe(J.S(e,"file"))||!Safe(J.S(e,"cover"))||J.I(e,"pages")<1||!System.Text.RegularExpressions.Regex.IsMatch(J.S(e,"sha256"),"^[a-f0-9]{64}$"))throw new Exception("Conteúdo inválido.");
  return items;
 }
 public static Dictionary<string,object> Find(Store store,string id){
  var matches=store.Library(true).Select(J.Obj).Where(d=>J.S(d,"id")==id||J.S(d,"originId")==id).ToArray();
  return matches.FirstOrDefault(d=>!Store.IsDeleted(d)&&J.S(d,"id")==id)??matches.FirstOrDefault(d=>!Store.IsDeleted(d))??matches.FirstOrDefault();
 }
 public static object[] List(Store store){return Items().Select(e=>{var d=Find(store,J.S(e,"documentId"));e["state"]=d==null?"available":Store.IsDeleted(d)?"deleted":"added";return (object)e;}).ToArray();}
 public static Dictionary<string,object> Add(Store store,string slug){lock(store.Gate){
  var e=Items().FirstOrDefault(x=>J.S(x,"id")==slug);if(e==null)throw new Exception("Material não encontrado.");
  var found=Find(store,J.S(e,"documentId"));if(found!=null){var id=J.S(found,"id");if(Store.IsDeleted(found))store.Trash(id,false);return store.Read(id);}
  var source=Path.Combine(Root,J.S(e,"file"));
  if(new FileInfo(source).Length!=Convert.ToInt64(e["bytes"])||J.HashFile(source)!=J.S(e,"sha256"))throw new Exception("Material incompleto. Extraia novamente o ZIP do app.");
  var d=store.Create(J.S(e,"title"),"blank",false);d["id"]=J.S(e,"documentId");d["pdf"]=true;d["count"]=J.I(e,"pages");d["folder"]="Conteúdos · Japonês";
  var dir=store.Dir(J.S(d,"id"));Directory.CreateDirectory(dir);var target=Path.Combine(dir,"source.pdf");var temp=Path.Combine(dir,"content.tmp");
  try{File.Copy(source,temp,true);if(File.Exists(target))File.Move(target,Path.Combine(dir,"source-interrupted-"+J.Now()+".pdf"));File.Move(temp,target);store.Write(d);return d;}finally{if(File.Exists(temp))File.Delete(temp);}
 }}
}
}
