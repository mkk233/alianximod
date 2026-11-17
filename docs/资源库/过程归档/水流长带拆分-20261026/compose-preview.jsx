function compose(f,mode){
 var up=part('upper',f),mid=part('middle',f),low=part('lower',f),g=[];
 for(var y=0;y<96;y++){g[y]=[];for(var x=0;x<128;x++)g[y][x]=-1;}
 for(var x=0;x<128;x++){
  var gap=mode=='taper'?Math.round(48*x/127):Math.round(48*Math.min(f/11,1));
  var oy=Math.floor((96-34-gap)/2);
  for(var y=0;y<22;y++)g[oy+y][x]=up[y][x];
  for(var y=0;y<gap;y++)g[oy+22+y][x]=mid[y%16][x];
  for(var y=0;y<12;y++)g[oy+22+gap+y][x]=low[y][x];
 }return g;
}
