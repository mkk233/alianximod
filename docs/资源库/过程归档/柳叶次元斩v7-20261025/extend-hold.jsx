var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{var out="D:/alianximod/docs/资源库/特效/次元斩/柳叶单斩_v7/";
var source=app.open(new File("D:/alianximod/docs/资源库/特效/次元斩/柳叶单斩_v6/dimension_willow_v6.psd"));
var doc=app.documents.add(96,960,72,"柳叶次元斩v7_峰值延长200ms",NewDocumentMode.RGB,DocumentFill.TRANSPARENT),blank=doc.activeLayer;
for(var n=0;n<20;n++){var f=n<=4?n:(n<=8?4:n-4);app.activeDocument=source;var original=source.artLayers.getByName("frame_"+("0"+f).slice(-2)+"_50ms");var layer=original.duplicate(doc,ElementPlacement.PLACEATBEGINNING);app.activeDocument=doc;layer.name="frame_"+("0"+n).slice(-2)+"_50ms";if(f<15&&n!=f)layer.translate(0,(n-f)*48);}
blank.remove();doc.saveAs(new File(out+"dimension_willow_v7.psd"),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);doc.saveAs(new File(out+"dimension_willow_v7.png"),new PNGSaveOptions(),true,Extension.LOWERCASE);
for(var i=0;i<20;i++){var frame=doc.duplicate("willow_v7_"+i,true);frame.crop([UnitValue(0,"px"),UnitValue(i*48,"px"),UnitValue(96,"px"),UnitValue((i+1)*48,"px")]);frame.saveAs(new File(out+"单帧/frame_"+("0"+i).slice(-2)+".png"),new PNGSaveOptions(),true,Extension.LOWERCASE);frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;}return {document_id:doc.id,frames:20,addedHoldMs:200};}finally{app.preferences.rulerUnits=old;}
