var oldUnits=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{
 var folder=new Folder('D:/alianximod/art/ps-icons-20261024/');
 var doc=app.documents.add(112,24,72,'术原胚_MC_六图标_透明画布',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);
 var blank=doc.activeLayer;
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

 var codes=['o','S','M','L','H'];
 for(var i=0;i<sets.length;i++){
  var layer=doc.artLayers.add();layer.name=sets[i][1];doc.activeLayer=layer;
  for(var c=0;c<codes.length;c++){
   var parts=[];
   for(var y=0;y<16;y++){
    var x=0;
    while(x<16){
     if(pixels[y].charAt(x)!=codes[c]){x++;continue;}
     var e=x+1;while(e<16&&pixels[y].charAt(e)==codes[c])e++;
     var coords=[[x+4+i*18,y+4],[e+4+i*18,y+4],[e+4+i*18,y+5],[x+4+i*18,y+5]];
     var pts=[];
     for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}
     var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;
    }
   }
   if(parts.length){var path=doc.pathItems.add('pixel-mask',parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=sets[i][2][c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}
  }
 }
 blank.remove();doc.activeLayer=doc.artLayers[doc.artLayers.length-1];
 var psd=folder.fsName+'/shu_embryo_mc_sheet.psd';doc.saveAs(new File(psd),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
 var png=folder.fsName+'/shu_embryo_mc_sheet.png';doc.saveAs(new File(png),new PNGSaveOptions(),true,Extension.LOWERCASE);
 var preview=doc.duplicate('preview',false);preview.resizeImage(UnitValue(896,'px'),UnitValue(192,'px'),72,ResampleMethod.NEARESTNEIGHBOR);
 preview.saveAs(new File(folder.fsName+'/shu_embryo_mc_sheet_8x.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);preview.close(SaveOptions.DONOTSAVECHANGES);
 app.activeDocument=doc;
 return {ok:true,document_id:doc.id,psd:psd,png:png,width:112,height:24,layers:6};
}finally{app.preferences.rulerUnits=oldUnits;}