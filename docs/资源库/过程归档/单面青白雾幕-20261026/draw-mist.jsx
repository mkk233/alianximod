var out='D:/alianximod/docs/资源库/特效/水流/单面青白雾幕_v1/';
var doc=app.documents.add(128,2048,72,'单面青白雾幕_32帧',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);
var blank=doc.activeLayer;
var pal=['9BBFC5','9BBFC5','C1DEE0','C1DEE0','E4F3F0','E4F3F0'];
var alpha=[10,18,27,37,48,60];
function wrap(x){return (x%64+64)%64;}
function field(x,y,t){
 var v=0;
 var clouds=[[4,22,15,7,.70],[26,34,19,9,.85],[48,18,13,8,.65],[60,44,22,8,.80],[16,48,12,5,.35],[41,42,12,5,.32]];
 for(var i=0;i<clouds.length;i++){
  var c=clouds[i],dx=wrap(x-c[0]+32)-32;
  var dy=y-c[1]-1.5*Math.sin(t+i*1.7);
  v+=c[4]*Math.exp(-dx*dx/(c[2]*c[2])-dy*dy/(c[3]*c[3]));
 }
 var noise=.80+.16*Math.sin(x*.48+y*.37)+.08*Math.cos(x*.91-y*.49);
 return v*noise*(.94+.06*Math.sin(t+y*.13))*Math.pow(Math.sin(Math.PI*y/64),1.4);
}
for(var f=0;f<32;f++){
 var layer=doc.artLayers.add();layer.name='frame_'+('0'+f).slice(-2)+'_50ms';
 var grid=[];
 for(var y=0;y<64;y+=2){grid[y]=[];for(var x=0;x<128;x+=2){
  var v=field(wrap(x-2*f),y,2*Math.PI*f/32);
  grid[y][x]=v<.13?-1:Math.min(5,Math.floor((v-.13)*5.1));
 }}
 for(var c=0;c<6;c++){
  var parts=[];
  for(var y=0;y<64;y+=2){for(var x=0;x<128;x+=2){
   if(grid[y][x]!=c)continue;
   var end=x+2;while(end<128&&grid[y][end]==c)end+=2;
   var coords=[[x,y+f*64],[end,y+f*64],[end,y+2+f*64],[x,y+2+f*64]],pts=[];
   for(var k=0;k<4;k++){var p=new PathPointInfo();p.kind=PointKind.CORNERPOINT;p.anchor=coords[k];p.leftDirection=coords[k];p.rightDirection=coords[k];pts.push(p);}
   var s=new SubPathInfo();s.closed=true;s.operation=ShapeOperation.SHAPEADD;s.entireSubPath=pts;parts.push(s);x=end-2;
  }}
  if(parts.length){var path=doc.pathItems.add('雾团像素',parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,alpha[c]*100/255,false);doc.selection.deselect();path.remove();}
 }
}
blank.remove();
doc.saveAs(new File(out+'mist_panel.psd'),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
doc.saveAs(new File(out+'mist_panel.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
for(var i=0;i<32;i++){var frame=doc.duplicate('mist_'+i,true);frame.crop([UnitValue(0,'px'),UnitValue(i*64,'px'),UnitValue(128,'px'),UnitValue((i+1)*64,'px')]);frame.saveAs(new File(out+'单帧/frame_'+('0'+i).slice(-2)+'.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);frame.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;}
return {ok:true,document_id:doc.id,frames:32};
