var oldUnits=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{
var doc=app.activeDocument;var folder=new Folder('D:/alianximod/docs/资源库/过程归档/术原胚-20261024/');
var originals=[];for(var n=0;n<doc.layers.length;n++)originals.push(doc.layers[n]);var archive=doc.layerSets.add();archive.name='旧版_圆矿胚_隐藏';for(var n=0;n<originals.length;n++)originals[n].move(archive,ElementPlacement.INSIDE);archive.visible=false;
var original=["................","................","..........DD....","........DDLLD...",".......DLLHMLD..","......DLLLMMMD..",".....DLLMMMDD...","....DLLMMMMMSD..","...DLLMMMSMSSD..","...DLMMMSMSSD...","..DLLMMSMSSD....","..DLMMSMSSD.....","...DMMSSSD......","....DSSDD.......",".....DD.........","................"];var sets=[["none","01_基础_术原胚",["343343","565574","8588AC","B4BCD8","E4EBF5"],[]],["wood","02_木属性",["26442C","366846","64A458","9CD372","E1F1A6"],[[8,7,"S"],[8,8,"S"],[5,10,"S"],[5,11,"S"],[5,14,"."],[6,14,"."]]],["fire","03_火属性",["662A25","A04329","E87A32","FFB349","FFF0AB"],[[12,7,"."],[11,7,"D"],[10,7,"L"],[10,8,"L"],[10,4,"L"]]],["water","04_水属性",["223F65","286D91","3EA6C4","7BDEDE","DBFFF2"],[[11,9,"."],[10,9,"D"],[9,7,"L"],[6,10,"L"]]],["earth","05_土属性",["493225","785332","B2884A","DAB76E","F4E0A0"],[[2,12,"D"],[3,12,"S"],[8,6,"S"],[9,6,"S"],[5,9,"S"],[6,9,"S"]]],["metal","06_金属性",["624524","987030","D7AA38","F4D666","FFF3B4"],[[10,2,"S"],[11,2,"S"],[10,3,"H"],[11,3,"H"],[10,4,"L"]]]];var codes=['D','S','M','L','H'];
for(var i=0;i<1;i++){
var pixels=original.slice(0);for(var n=0;n<sets[i][3].length;n++){var edit=sets[i][3][n];var row=pixels[edit[1]];pixels[edit[1]]=row.substring(0,edit[0])+edit[2]+row.substring(edit[0]+1);}
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

var psd=folder.fsName+'/shu_embryo_mc_v2.psd';doc.saveAs(new File(psd),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
var png=folder.fsName+'/shu_embryo_mc_v2.png';doc.saveAs(new File(png),new PNGSaveOptions(),true,Extension.LOWERCASE);
var preview=doc.duplicate('v2_预览',false);preview.resizeImage(UnitValue(912,'px'),UnitValue(192,'px'),72,ResampleMethod.NEARESTNEIGHBOR);
preview.saveAs(new File(folder.fsName+'/shu_embryo_mc_v2_8x.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);preview.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;app.runMenuItem(charIDToTypeID('FtOn'));
return {ok:true,document_id:doc.id,psd:psd,png:png};
}finally{app.preferences.rulerUnits=oldUnits;}
