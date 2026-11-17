var oldUnits = app.preferences.rulerUnits;
app.preferences.rulerUnits = Units.PIXELS;
try {
 var folder = new Folder('D:/alianximod/docs/资源库/过程归档/术原胚-20261024'); if(!folder.exists) folder.create();
 var doc=app.documents.add(16,16,72,'术原胚_MC_16px_六属性',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);
 var initial=doc.activeLayer;
 var pixels=[
 '................',
 '.......oo.......',
 '.....ooLLoo.....',
 '....oLHLLLLo....',
 '...oLLLLMMMo....',
 '..oLLLMMMMMMo...',
 '..oLLMMMMLMMo...',
 '.oLMMMMMMLMSSo..',
 '.oMMMMMMMMSSSo..',
 '.oMMMMMLMSSSSo..',
 '..oMMMLMMSSSo...',
 '..oMMMMSSSSSo...',
 '...oMMSSSSSo....',
 '....ooSSSoo.....',
 '......ooo.......',
 '................'
 ];
 var sets=[
 ['none','基础_未刻印',['282533','494456','82798F','B6ACBC','E7E0E6']],
 ['wood','木_苔绿',['203025','354D31','65834A','9DB56A','D8DCA0']],
 ['fire','火_赤橙',['402528','77382E','BA6039','E69A55','F3D598']],
 ['water','水_湛蓝',['202D45','304D77','4A86B0','83BED2','CBE6E9']],
 ['earth','土_赭褐',['382B24','655039','A17B4D','C6A777','E5CEA2']],
 ['metal','金_暗金',['3B3027','77603B','B89A4F','DDC47B','F3E4B2']]
 ];
 var codes=['o','S','M','L','H']; var layers=[];var outputs=[];
 for(var i=0;i<sets.length;i++) {
  var layer=doc.artLayers.add(); layer.name=sets[i][1]; layers.push(layer);doc.activeLayer=layer;
  for(var y=0;y<16;y++) {
   var row=pixels[y]; var x=0;
   while(x<16) {
    var ch=row.charAt(x); if(ch=='.'){x++;continue;}
    var end=x+1;while(end<16 && row.charAt(end)==ch)end++;
    var idx=0;for(var k=0;k<codes.length;k++){if(codes[k]==ch){idx=k;break;}}
    var color=new SolidColor();color.rgb.hexValue=sets[i][2][idx];
    doc.selection.select([[x,y],[end,y],[end,y+1],[x,y+1]],SelectionType.REPLACE,0,false);
    doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);x=end;
   }
  }
  doc.selection.deselect();
  for(var j=0;j<layers.length;j++)layers[j].visible=(j==i);
  var path=folder.fsName+'/shu_embryo_'+sets[i][0]+'_16.png';
  doc.saveAs(new File(path),new PNGSaveOptions(),true,Extension.LOWERCASE);outputs.push(path);
 }
 initial.remove();
 for(var j=0;j<layers.length;j++)layers[j].visible=(j==0);doc.activeLayer=layers[0];
 var psd=folder.fsName+'/shu_embryo_mc_16.psd';doc.saveAs(new File(psd),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
 var sheet=app.documents.add(112,24,72,'术原胚_MC_预览',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);var blank=sheet.activeLayer;
 for(var i=0;i<layers.length;i++) {
  app.activeDocument=doc;
  var dup=layers[i].duplicate(sheet,ElementPlacement.PLACEATBEGINNING);
  app.activeDocument=sheet;dup.visible=true;dup.translate(4+i*18,4);
 }
 blank.remove();sheet.resizeImage(UnitValue(896,'px'),UnitValue(192,'px'),72,ResampleMethod.NEARESTNEIGHBOR);
 var preview=folder.fsName+'/shu_embryo_mc_preview_8x.png';sheet.saveAs(new File(preview),new PNGSaveOptions(),true,Extension.LOWERCASE);
 sheet.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
 return {ok:true,document_id:doc.id,psd:psd,pngs:outputs,preview:preview,width:16,height:16,layers:6};
} finally {app.preferences.rulerUnits=oldUnits;}

