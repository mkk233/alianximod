var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{var doc=app.activeDocument;var out='D:/alianximod/docs/资源库/特效/水流/水流长带_v1/';
for(var i=0;i<16;i++){var frame=doc.duplicate('水流长带_frame_'+i,true);frame.crop([UnitValue(0,'px'),UnitValue(i*32,'px'),UnitValue(128,'px'),UnitValue((i+1)*32,'px')]);frame.saveAs(new File(out+'单帧/frame_'+('0'+i).slice(-2)+'.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;}
app.runMenuItem(charIDToTypeID('FtOn'));return {ok:true,frames:16};}finally{app.preferences.rulerUnits=old;}
