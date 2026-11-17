if(Project.uuid!=='70428e16-87b5-2c3d-1b9b-7e3c89902eb3')throw Error('Wrong project');
const V=(a)=>new THREE.Vector3(...a);
const mix=(a,b,u)=>a.clone().lerp(b,u);
const smooth=(u)=>{u=Math.max(0,Math.min(1,u));return u*u*(3-2*u);};
const down=V([0,-1,0]);
const axis=V([Math.cos(Math.PI/9),0,Math.sin(Math.PI/9)]);
const mouth=V([3,17,-5]);
const startR=mouth.clone().addScaledVector(axis,-2.75);
const clearR=startR.clone().addScaledVector(axis,-8);
const clearL=mouth.clone().addScaledVector(axis,8);
const endR=V([-9.5,18,-4.5]);
const lowR=V([-9,16,-4]);
const lowL=V([4,16,-3]);
const bones={Body:[],Head:[],ArmRight:[],ForearmRight:[],SwordHand:[],ArmLeft:[],ForearmLeft:[],Scabbard:[],LegRight:[],LegLeft:[]};
const prior={};
let maxReach=0;
function euler(name,q){const e=new THREE.Euler().setFromQuaternion(q,'ZYX');const v=[e.x,e.y,e.z].map(x=>x*180/Math.PI);if(prior[name])for(let k=0;k<3;k++){while(v[k]-prior[name][k]>180)v[k]-=360;while(v[k]-prior[name][k]<-180)v[k]+=360;}prior[name]=v;return v.map(v=>Math.round(v*100000)/100000);}
function bladeQ(d,roll){const y=d.clone().normalize();let x=V([0,1,0]);x.addScaledVector(y,-x.dot(y)).normalize();const z=new THREE.Vector3().crossVectors(x,y).normalize();const q=new THREE.Quaternion().setFromRotationMatrix(new THREE.Matrix4().makeBasis(x,y,z));q.multiply(new THREE.Quaternion().setFromAxisAngle(V([0,1,0]),roll));return q;}
function arm(side,h,weaponQ,time){const sign=side==='Right'?-1:1;const s=V([sign*6,24,0]);const delta=h.clone().sub(s);const length=delta.length();maxReach=Math.max(maxReach,length);if(length>=12)throw Error('Unreachable hand at '+time+' '+side+': '+length);const dir=delta.normalize();const pole=V([sign,-.15,-.2]);pole.addScaledVector(dir,-pole.dot(dir)).normalize();const elbow=s.clone().addScaledVector(dir,length/2).addScaledVector(pole,Math.sqrt(36-length*length/4));const upperQ=new THREE.Quaternion().setFromUnitVectors(down,elbow.clone().sub(s).normalize());const foreQ=new THREE.Quaternion().setFromUnitVectors(down,h.clone().sub(elbow).normalize());const relFore=upperQ.clone().invert().multiply(foreQ);const relWeapon=foreQ.clone().invert().multiply(weaponQ);const upper='Arm'+side,fore='Forearm'+side,weapon=side==='Right'?'SwordHand':'Scabbard';bones[upper].push({time,rotation:euler(upper,upperQ)});bones[fore].push({time,rotation:euler(fore,relFore)});bones[weapon].push({time,rotation:euler(weapon,relWeapon)});}
const expected=[];
for(let i=0;i<=160;i++){
 const t=Number((i*.01).toFixed(2));
 const wind=smooth(t/.20),draw=smooth((t-.20)/.28),cut=smooth((t-.48)/.12),recover=smooth((t-.85)/.75);
 const u=Math.max(0,Math.min(1,(t-.20)/.28));
 const drawR=2*u*u-u*u*u;
 let hr=mix(startR,clearR,drawR),hl=mix(mouth,clearL,draw);
 let dir=axis.clone(),roll=0;
 if(t>=.48){const q=Math.max(0,Math.min(1,(t-.48)/.12)),v=1-q;const p1=clearR.clone().addScaledVector(axis,-(8/.28)*(.12/3));hr=clearR.clone().multiplyScalar(v*v*v).addScaledVector(p1,3*v*v*q).addScaledVector(endR,3*v*q*q+q*q*q);const a=smooth(q);const theta=(20-140*a)*Math.PI/180;dir=V([Math.cos(theta),0,Math.sin(theta)]);roll=a*Math.PI/2;}
 if(t>=.85){hr=mix(endR,lowR,recover);hl=mix(clearL,lowL,recover);dir=mix(V([Math.cos(-2*Math.PI/3),0,Math.sin(-2*Math.PI/3)]),V([-.45,-.65,-.62]).normalize(),recover).normalize();roll=(1-recover*.6)*Math.PI/2;}
 const sheathDir=mix(axis,V([.94,-.12,.32]).normalize(),recover).normalize();
 arm('Right',hr,bladeQ(dir,roll),t);arm('Left',hl,bladeQ(sheathDir,0),t);
 const bx=-3-5*wind+2*cut+4*recover,by=-12*wind+32*cut-9*recover,bz=-2*cut+2*recover;
 bones.Body.push({time:t,rotation:[bx,by,bz],position:[0,-.45*wind+.25*cut+.2*recover,0]});
 bones.Head.push({time:t,rotation:[-bx*.6,-by*.75,0]});
 bones.LegRight.push({time:t,rotation:[-4,0,-7],position:[-.25,0,.6]});
 bones.LegLeft.push({time:t,rotation:[7,0,7],position:[.25,0,-.6]});
 expected.push({time:t,right:hr.toArray(),left:hl.toArray(),blade:dir.toArray(),sheath:sheathDir.toArray()});
}
globalThis.iaidoExpected=expected;
({bones,diagnostics:{maxArmReach:maxReach,armLength:12,frames:161,duration:1.6,oneSword:true}});
