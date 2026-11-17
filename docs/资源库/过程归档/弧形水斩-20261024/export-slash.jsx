var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{var doc=app.activeDocument;var out='D:/alianximod/docs/资源库/特效/水流/弧形水斩_v1/';
for(var i=0;i<16;i++){
var frame;
if(i==15){frame=app.documents.add(64,64,72,'结束透明帧',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);}else{frame=doc.duplicate('弧形水斩_frame_'+i,true);frame.crop([UnitValue(0,'px'),UnitValue(i*64,'px'),UnitValue(64,'px'),UnitValue((i+1)*64,'px')]);}
frame.saveAs(new File(out+'单帧/frame_'+('0'+i).slice(-2)+'.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
}
app.runMenuItem(charIDToTypeID('FtOn'));return {ok:true,frames:16};}finally{app.preferences.rulerUnits=old;}
