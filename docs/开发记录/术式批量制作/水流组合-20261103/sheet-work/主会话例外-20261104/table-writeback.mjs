import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const root = 'D:/alianximod/docs/开发记录/术式批量制作/水流组合-20261103/sheet-work/主会话例外-20261104';
const batch = 'D:/alianximod/docs/开发记录/术式批量制作/水流组合-20261103';
const live = 'D:/alianximod/docs/术式制作表.xlsx';
const hash = b => crypto.createHash('sha256').update(b).digest('hex').toUpperCase();
const bytes = await fs.readFile(live);
const baseline = hash(bytes);
if(process.argv.includes('--verify-final')) {
  const expected=JSON.parse(await fs.readFile(root+'/candidate-check.json','utf8'));
  if(baseline!==expected.candidateHash)throw Error('Written source hash differs');
  const finalBook=await SpreadsheetFile.importXlsx(await FileBlob.load(live));
  const authored=JSON.parse(await fs.readFile(root+'/authored-rows.json','utf8'));
  for(const row of authored.rows){if(JSON.stringify(finalBook.worksheets.getItem('待验收').getRange('A'+row.toRow+':EN'+row.toRow).values[0])!==JSON.stringify(row.after))throw Error('Final live row differs');}
  const counts={};const all=[];
  for(const name of ['已完成','待制作','制作中','待验收']){const ids=finalBook.worksheets.getItem(name).getRange('B3:B80').values.flat().filter(x=>x!==null&&x!=='');counts[name]=ids.length;all.push(...ids);}
  if(all.length!==62||new Set(all).size!==62||JSON.stringify(counts)!==JSON.stringify(expected.counts))throw Error('Final live counts differ');
  const result={verifiedAt:new Date().toISOString(),hash:baseline,counts,uniqueIds:62,rows:expected.rows,sourceReopened:true,gameVerified:false,additionalReview:false};
  await fs.writeFile(root+'/final-live-check.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));process.exit(0);
}
if(baseline !== '882F639DB28818BDFA56D3419A49C4AB54B4C6EFA6C6B8FE429A2E45B9FAA352') throw Error('Source changed; rebase needed');
const proof=JSON.parse(await fs.readFile(root+'/compile-clearance.json','utf8'));
if(!proof.passed || !proof.resourcesPassed) throw Error('Final compilation not successful');
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(live));
const src=wb.worksheets.getItem('制作中'), dst=wb.worksheets.getItem('待验收');
const ids=['water_vortex','water_bubble_column','water_deep_pressure'];
const n=['干地水平牵引。入卷收取水势；与上升气泡和真实水组成龙卷后才按湿度扣返','真实水里的上升水柱。底部与旋流重合时挂上去，父旋流关闭后不再独立续挂','低矮的重压领域。范围内暂时积起不流动的水，里面的人变慢、变潮湿，氧气消耗加快'];
const o=['assets/xueximod/textures/domain/water_flow_v3_1.png；挂载后的气泡膜 assets/xueximod/textures/domain/water_bubble_v3_1.png','assets/xueximod/textures/domain/water_bubble_v3_1.png','assets/xueximod/textures/domain/deep_pressure_bands.png；assets/xueximod/textures/domain/deep_pressure_arcs.png；原版 minecraft:block/water_still、minecraft:block/water_flow、minecraft:block/water_overlay'];
const feedback='[设计反馈 2026-11-04] 本轮实测返修完成：干地五条连续内旋水带、组合六条连续漏斗螺旋及水下汇入；深海冲掉草蕨火把等水可冲毁附着物并正常掉落一次，保留地形建筑楼梯半砖容器自然水与玩家后改；聚怪排除施法者及其骑乘组，组合气泡不重新带入本人；保留区块加载无重入修复。最终统一compileJava和processResources已实际通过，未游戏复测，未追加正式复审。待验收。用户仅本次授权主会话写回三项，其他项目不变。详见 docs/开发记录/术式批量制作/水流组合-20261103/实测返修-20261104-水流/编译证据引用.md。';
const rows=ids.map((id,i)=>{const old=src.getRange('A'+(18+i)+':EN'+(18+i)).values[0];if(old[1]!==id)throw Error('Source row mismatch');const values=[...old];values[13]=n[i];values[14]=o[i];values[125]=String(old[125]??'')+'\n'+feedback;return {id,fromRow:18+i,toRow:10+i,before:old,formulas:src.getRange('A'+(18+i)+':EN'+(18+i)).formulas[0],after:values};});
const snapshot={capturedAt:new Date().toISOString(),source:live,sourceHash:baseline,approval:'用户明确允许，仅本次这三项制作表写回；统一编译成功后执行',visualVersion:'已确认v3.1，不重绘',compile:proof,headers:src.getRange('A2:EN2').values[0],rows};
await fs.writeFile(batch+'/sheet-work/实测三项-20261104待验收快照.json',JSON.stringify(snapshot,null,2));
await fs.writeFile(root+'/authored-rows.json',JSON.stringify({sourceHash:baseline,rows},null,2));
await fs.writeFile(root+'/original-backup.xlsx',bytes);
for(const row of rows){dst.getRange('A'+row.toRow+':EN'+row.toRow).copyFrom(src.getRange('A'+row.fromRow+':EN'+row.fromRow),'all');dst.getRange('N'+row.toRow+':O'+row.toRow).values=[[row.after[13],row.after[14]]];dst.getRange('DV'+row.toRow).values=[[row.after[125]]];src.getRange('A'+row.fromRow+':EN'+row.fromRow).clear({applyTo:'all'});}
wb.recalculate();
await (await SpreadsheetFile.exportXlsx(wb)).save(root+'/artifact-authored.xlsx');
console.log(execFileSync('C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe',[root+'/preserve-package.py'],{encoding:'utf8'}));
const candidate=root+'/candidate.xlsx';
const check=await SpreadsheetFile.importXlsx(await FileBlob.load(candidate));
const all=[];
const counts={};
for(const name of ['已完成','待制作','制作中','待验收']){const values=check.worksheets.getItem(name).getRange('B3:B80').values.flat().filter(v=>v!==null&&v!=='');counts[name]=values.length;all.push(...values);}
if(all.length!==62 || new Set(all).size!==62 || counts['制作中']!==15 || counts['待验收']!==10)throw Error('Counts or uniqueness mismatch');
for(const row of rows){const vals=check.worksheets.getItem('待验收').getRange('A'+row.toRow+':EN'+row.toRow).values[0];if(JSON.stringify(vals)!==JSON.stringify(row.after))throw Error('Candidate row mismatch '+row.id);}
check.recalculate();
for(const [name,range] of [['待验收','A10:F12'],['待验收','N10:O12'],['制作中','A15:F17']]){const pic=await check.render({sheetName:name,range,scale:1,format:'png'});await fs.writeFile(root+'/after-'+range.replace(':','-')+'.png',new Uint8Array(await pic.arrayBuffer()));}
const scan=await check.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!|#NULL!|#SPILL!|#CALC!',options:{useRegex:true,maxResults:20},maxChars:2000});
console.log(scan.ndjson);
await fs.writeFile(root+'/candidate-check.json',JSON.stringify({time:new Date().toISOString(),sourceHash:baseline,candidateHash:hash(await fs.readFile(candidate)),counts,unique:all.length,rows:rows.map(r=>({id:r.id,row:r.toRow})),native:JSON.parse(await fs.readFile(root+'/native-preservation.json','utf8'))},null,2));
console.log(JSON.stringify({stage:'candidate_ready_not_written_back',counts,candidate,hash:hash(await fs.readFile(candidate))}));

