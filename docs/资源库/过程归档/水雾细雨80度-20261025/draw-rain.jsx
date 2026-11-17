var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;
try{
var out="D:/alianximod/docs/资源库/特效/水流/水雾细雨80度_v1/";
var doc=app.documents.add(64,2048,72,"水雾领域_80度细雨_32帧",NewDocumentMode.RGB,DocumentFill.TRANSPARENT);
var blank=doc.activeLayer;
var drops=[[2,4,8,1],[14,11,6,0],[26,2,10,1],[8,23,7,0],[22,29,12,2],[30,18,5,0],[3,43,9,1],[16,48,6,0],[27,55,11,1],[10,61,5,0],[19,36,7,1],[31,40,6,0]];
var pal=["6099B5","86BED3","B9E7ED"],alpha=[48,100,170];
function mod(a,n){return (a%n+n)%n;}
for(var f=0;f<32;f++){
var layer=doc.artLayers.add();layer.name="frame_"+("0"+f).slice(-2)+"_50ms";doc.activeLayer=layer;
var grid=[];for(var yy=0;yy<64;yy++){grid[yy]=[];for(var xx=0;xx<64;xx++)grid[yy][xx]=-1;}
for(var d=0;d<drops.length;d++){var seed=drops[d];
for(var k=0;k<seed[2];k++){
var x=mod(seed[0]+f-Math.round(k/6),32),y=mod(seed[1]+6*f-k,64);
var c=k>=seed[2]-2?0:(seed[3]==0?0:1);if(k<2&&seed[3]==2)c=2;
grid[y][x]=Math.max(grid[y][x],c);grid[y][x+32]=Math.max(grid[y][x+32],c);
}}
for(var c=0;c<3;c++){
var parts=[];for(var y=0;y<64;y++){for(var x=0;x<64;x++){if(grid[y][x]!=c)continue;
var coords=[[x,y+64*f],[x+1,y+64*f],[x+1,y+1+64*f],[x,y+1+64*f]],pts=[];
for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}
var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);
}}
if(parts.length){var path=doc.pathItems.add("rain-pixels",parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,alpha[c]*100/255,false);doc.selection.deselect();path.remove();}
}
}
blank.remove();
doc.saveAs(new File(out+"mist_rain_80.psd"),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
doc.saveAs(new File(out+"mist_rain_80.png"),new PNGSaveOptions(),true,Extension.LOWERCASE);
for(var i=0;i<32;i++){var frame=doc.duplicate("rain_frame_"+i,true);frame.crop([UnitValue(0,"px"),UnitValue(i*64,"px"),UnitValue(64,"px"),UnitValue((i+1)*64,"px")]);frame.saveAs(new File(out+"单帧/frame_"+("0"+i).slice(-2)+".png"),new PNGSaveOptions(),true,Extension.LOWERCASE);frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;}
var preview=doc.duplicate("细雨关键帧预览",true);preview.crop([UnitValue(0,"px"),UnitValue(0,"px"),UnitValue(64,"px"),UnitValue(64,"px")]);preview.resizeImage(UnitValue(512,"px"),UnitValue(512,"px"),72,ResampleMethod.NEARESTNEIGHBOR);preview.saveAs(new File(out+"mist_rain_80_keyframe_8x.png"),new PNGSaveOptions(),true,Extension.LOWERCASE);
app.activeDocument=doc;
return {ok:true,document_id:doc.id,preview_id:preview.id,frames:32,width:64,height:64,angle:80.53767779};
}finally{app.preferences.rulerUnits=old;}
