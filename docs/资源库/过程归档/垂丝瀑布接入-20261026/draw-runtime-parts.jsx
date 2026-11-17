function runtimePart(kind,f){
 var g=[];
 for(var y=0;y<128;y++){g[y]=[];var q=mod(y-8*f,32),shift=Math.round(.7*Math.sin(2*Math.PI*(y-8*f)/32));
 for(var x=0;x<4;x++){
 var c=2;
 if(kind=='middle'){
 if(x==1&&q<24)c=3;
 if(x==2&&q>=8&&q<26)c=4;
 if(x==2&&q>=20&&q<24)c=5;
 }else{
 var edge=x;if(kind=='right')edge=3-x;
 if(edge==0){c=0;if(shift>0)c=-1;}
 else if(edge==1){c=1;if(q<20)c=3;}
 else if(edge==2){c=2;if(q>=12&&q<18)c=4;}
 }
 g[y][x]=c;
 }}return g;
}
