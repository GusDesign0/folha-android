using System;using System.IO;using System.Linq;using System.Collections.Generic;
namespace Folha {
static class ContentChecks {
 public static void Run(string root){
  var store=new Store(Path.Combine(root,"content-tests"));var entries=ContentCatalog.Items();if(entries.Length!=2)throw new Exception("Expected two materials");
  foreach(var e in entries){
   var d=ContentCatalog.Add(store,J.S(e,"id"));var id=J.S(d,"id");if(J.I(d,"count")!=J.I(e,"pages"))throw new Exception("Page count");
   var hash=J.HashFile(store.DataFile(id,"source.pdf"));if(hash!=J.S(e,"sha256"))throw new Exception("PDF integrity");
   d["name"]="My annotated material";d["lastPage"]=4;store.Write(d);var before=File.ReadAllText(Path.Combine(store.Dir(id),"document.json"));
   var again=ContentCatalog.Add(store,J.S(e,"id"));if(J.S(again,"id")!=id||before!=File.ReadAllText(Path.Combine(store.Dir(id),"document.json")))throw new Exception("Repeated add overwrites document");
   store.Trash(id,true);var restored=ContentCatalog.Add(store,J.S(e,"id"));if(Store.IsDeleted(restored)||J.S(restored,"name")!="My annotated material"||J.I(restored,"lastPage")!=4||hash!=J.HashFile(store.DataFile(id,"source.pdf")))throw new Exception("Restore must preserve progress");
   var backup=Path.Combine(root,J.S(e,"id")+".folha");store.Backup(id,backup);var other=new Store(Path.Combine(root,"content-copy-"+J.S(e,"id")));other.ImportBackup(backup);var reopened=ContentCatalog.Add(other,J.S(e,"id"));if(J.S(reopened,"name")!="My annotated material"||other.Library().Length!=1)throw new Exception("Backup deduplication");
  }
  if(store.Library().Length!=2)throw new Exception("Repeated adds create duplicates");
  bool rejected=false;try{ContentCatalog.Add(store,"../unexpected");}catch{rejected=true;}if(!rejected)throw new Exception("Unknown catalog ID");
 }
}
}
