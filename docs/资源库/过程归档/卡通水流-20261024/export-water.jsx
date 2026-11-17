var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{
var doc=app.activeDocument;var out='D:/alianximod/docs/资源库/特效/水流/卡通水弹_v1/';
for(var i=0;i<16;i++){
var frame=doc.duplicate('frame_'+i,true);frame.crop([UnitValue(0,'px'),UnitValue(i*32,'px'),UnitValue(32,'px'),UnitValue((i+1)*32,'px')]);
frame.saveAs(new File(out+'单帧/frame_'+('0'+i).slice(-2)+'.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
}
var sheet=app.documents.add(128,128,72,'卡通水弹_16帧总览',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);var blank=sheet.activeLayer;
for(var i=0;i<16;i++){
app.activeDocument=doc;var original=doc.artLayers.getByName('frame_'+('0'+i).slice(-2)+'_100ms');var layer=original.duplicate(sheet,ElementPlacement.PLACEATBEGINNING);
app.activeDocument=sheet;layer.translate((i%4)*32,Math.floor(i/4)*32-i*32);
}
blank.remove();sheet.saveAs(new File(out+'water_bolt_contact.psd'),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
sheet.resizeImage(UnitValue(768,'px'),UnitValue(768,'px'),72,ResampleMethod.NEARESTNEIGHBOR);sheet.saveAs(new File(out+'water_bolt_contact.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
sheet.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;app.runMenuItem(charIDToTypeID('FtOn'));return {ok:true,frames:16};
}finally{app.preferences.rulerUnits=old;}
