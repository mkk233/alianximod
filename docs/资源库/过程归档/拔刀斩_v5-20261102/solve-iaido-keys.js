if(Project.uuid!=='70054734-4962-3265-be76-9c05cb72a65d')throw Error('Wrong project');
const V=(a)=>new THREE.Vector3(...a);
const mix=(a,b,u)=>a.clone().lerp(b,u);
const smooth=(u)=>{u=Math.max(0,Math.min(1,u));return u*u*(3-2*u);};
const down=V([0,-1,0]),pivot=V([0,12,0]),axis=V([0,0,1]);
const startR=V([5.3,15.5,-3.6]),clearR=V([3.5,18,-9]),endR=V([-9.5,18,-4.5]),lowR=V([-9,16,-4]);
const startL=V([8.8,18,2.4]),clearL=V([8.8,18.4,1.5]),lowL=V([6.5,14.5,1.8]);
const bones={Body:[],Head:[],ArmRight:[],ForearmRight:[],SwordHand:[],ArmLeft:[],ForearmLeft:[],LegRight:[],LegLeft:[]};
const prior={};
let maxReach=0;
function euler(name,q){const e=new THREE.Euler().setFromQuaternion(q,'ZYX');const v=[e.x,e.y,e.z].map(x=>x*180/Math.PI);if(prior[name])for(let k=0;k<3;k++){while(v[k]-prior[name][k]>180)v[k]-=360;while(v[k]-prior[name][k]<-180)v[k]+=360;}prior[name]=v;return v.map(v=>Math.round(v*100000)/100000);}
function bladeQ(d,roll){const y=d.clone().normalize();let x=V([0,1,0]);x.addScaledVector(y,-x.dot(y)).normalize();const z=new THREE.Vector3().crossVectors(x,y).normalize();const q=new THREE.Quaternion().setFromRotationMatrix(new THREE.Matrix4().makeBasis(x,y,z));q.multiply(new THREE.Quaternion().setFromAxisAngle(V([0,1,0]),roll));return q;}
function arm(side,h,weaponQ,time,invBody){const sign=side==='Right'?-1:1;const s=V([sign*6,24,0]);const delta=h.clone().sub(s);const length=delta.length();maxReach=Math.max(maxReach,length);if(length>=12)throw Error('Unreachable hand at '+time+' '+side+': '+length);const dir=delta.normalize();const pole=V(side==='Right'?[0,-.15,-1]:[1,-.15,.3]).applyQuaternion(invBody);pole.addScaledVector(dir,-pole.dot(dir)).normalize();const elbow=s.clone().addScaledVector(dir,length/2).addScaledVector(pole,Math.sqrt(36-length*length/4));const upperQ=new THREE.Quaternion().setFromUnitVectors(down,elbow.clone().sub(s).normalize());const foreQ=new THREE.Quaternion().setFromUnitVectors(down,h.clone().sub(elbow).normalize());const relFore=upperQ.clone().invert().multiply(foreQ);const relWeapon=foreQ.clone().invert().multiply(weaponQ);const upper='Arm'+side,fore='Forearm'+side,weapon='SwordHand';bones[upper].push({time,rotation:euler(upper,upperQ)});bones[fore].push({time,rotation:euler(fore,relFore)});if(side==='Right')bones[weapon].push({time,rotation:euler(weapon,relWeapon)});}

const expected=[];
for(let i=0;i<=160;i++){
 const t=Number((i*.01).toFixed(2)),wind=smooth(t/.20),draw=smooth((t-.20)/.28),cut=smooth((t-.48)/.12),recover=smooth((t-.85)/.75);
 const u=Math.max(0,Math.min(1,(t-.20)/.28)),drawR=2*u*u-u*u*u;
 const bx=-10-4*wind+6*draw+2*cut+4*recover,by=-50-4*wind+19*draw+55*cut-9*recover,bz=-2*cut+2*recover;
 const dy=-.45*wind+.20*draw+.10*cut+.15*recover;
 const bodyQ=new THREE.Quaternion().setFromEuler(new THREE.Euler(bx*Math.PI/180,by*Math.PI/180,bz*Math.PI/180,'ZYX'));
 const invBody=bodyQ.clone().invert();
 let hr=mix(startR,clearR,drawR),hl=mix(startL,clearL,smooth((t-.12)/.36));
 const preAngle=(90-70*smooth((t-.28)/.20))*Math.PI/180;
 let dir=V([Math.cos(preAngle),0,Math.sin(preAngle)]),roll=0;
 if(t>=.48){const q=Math.max(0,Math.min(1,(t-.48)/.12)),v=1-q;const p1=clearR.clone().addScaledVector(clearR.clone().sub(startR),(.12/3)/.28);hr=clearR.clone().multiplyScalar(v*v*v).addScaledVector(p1,3*v*v*q).addScaledVector(endR,3*v*q*q+q*q*q);const a=smooth(q);const theta=(20-140*a)*Math.PI/180;dir=V([Math.cos(theta),0,Math.sin(theta)]);roll=a*Math.PI/2;}
 if(t>=.85){hr=mix(endR,lowR,recover);hl=mix(clearL,lowL,recover);dir=mix(V([Math.cos(-2*Math.PI/3),0,Math.sin(-2*Math.PI/3)]),V([-.45,-.65,-.62]).normalize(),recover).normalize();roll=(1-recover*.6)*Math.PI/2;}
 const local=(h)=>h.clone().sub(pivot).sub(V([0,dy,0])).applyQuaternion(invBody).add(pivot);
 arm('Right',local(hr),invBody.clone().multiply(bladeQ(dir,roll)),t,invBody);
 arm('Left',local(hl),new THREE.Quaternion(),t,invBody);
 bones.Body.push({time:t,rotation:[bx,by,bz],position:[0,dy,0]});
 bones.Head.push({time:t,rotation:euler('Head',invBody.clone().multiply(new THREE.Quaternion().setFromAxisAngle(V([1,0,0]),-.035)))});
 bones.LegRight.push({time:t,rotation:[-4,0,-7],position:[-.25,0,.6]});
 bones.LegLeft.push({time:t,rotation:[7,0,7],position:[.25,0,-.6]});
 expected.push({time:t,right:hr.toArray(),left:hl.toArray(),blade:dir.toArray(),body:[bx,by,bz],bodyPosition:[0,dy,0]});
}
globalThis.iaidoExpected=expected;
({bones,expected,diagnostics:{maxArmReach:maxReach,armLength:12,frames:161,duration:1.6,oneSword:true,scabbard:false,waistStart:startR.toArray(),tipDirectionAtStart:axis.toArray(),mountSide:'left',drawHand:'right',targetSpace:'ActorRoot',upperBodyTwistAtStart:-50}});
