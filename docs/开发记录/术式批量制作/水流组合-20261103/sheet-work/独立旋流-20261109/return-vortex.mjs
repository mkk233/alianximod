import fs from "node:fs/promises";
import crypto from "node:crypto";
import { execFileSync } from "node:child_process";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/水流组合-20261103/sheet-work/独立旋流-20261109";
const live = "D:/alianximod/docs/术式制作表.xlsx";
const priorPath = "D:/alianximod/docs/开发记录/术式批量制作/水流组合-20261103/sheet-work/实测三项-20261104待验收快照.json";
const baseline = "314BD5EF7367DF1192093467F51F5BCBA2070F2FBC32BCC99F18300950CFC3C5";
const python = "C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe";
const hash = (payload) => crypto.createHash("sha256").update(payload).digest("hex").toUpperCase();
if (process.argv.includes("--deliver")) {
  const renderer = "D:/alianximod/src/main/java/com/bslx/XueXiMod/client/DomainFlowRenderer.java";
  const texture = "D:/alianximod/src/main/resources/assets/xueximod/textures/domain/water_flow_v3_1.png";
  const rendererHash = hash(await fs.readFile(renderer));
  const textureHash = hash(await fs.readFile(texture));
  if (rendererHash !== "610D26DBF60F63804E7D06EA2CCF96DD3050015F4CF03FC003E8D36876FAB324") throw new Error("Renderer hash changed");
  if (textureHash !== "D91EA9441D1CF55B048D370E9C45A0349DAD5F8CA41AAB35931418054DAB1CCB") throw new Error("Texture hash changed");
  const bytes = await fs.readFile(live);
  const sourceHash = hash(bytes);
  const stat = await fs.stat(live);
  const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(live));
  const statusNames = ["已完成", "待制作", "制作中", "待验收"];
  const locate = (sheet) => sheet.getRange("B3:B80").values.map((row, index) => ({ row: index + 3, id: row[0] })).filter((row) => row.id);
  const pages = Object.fromEntries(statusNames.map((name) => [name, locate(wb.worksheets.getItem(name))]));
  const hits = statusNames.flatMap((name) => pages[name].filter((row) => row.id === "water_vortex").map((row) => ({ name, row: row.row })));
  if (JSON.stringify(hits) !== JSON.stringify([{ name: "制作中", row: pages["制作中"].find((row) => row.id === "water_vortex").row }])) throw new Error("Vortex location " + JSON.stringify(hits));
  const fromRow = hits[0].row;
  const acceptanceRows = pages["待验收"];
  const toRow = Math.max(...acceptanceRows.map((row) => row.row)) + 1;
  const making = wb.worksheets.getItem("制作中");
  const acceptance = wb.worksheets.getItem("待验收");
  const current = making.getRange(`A${fromRow}:EN${fromRow}`).values[0];
  if (current[1] !== "water_vortex") throw new Error("Source id mismatch");
  const gameN = "干地水平牵引。入卷收取水势；与上升气泡和真实水组成龙卷后才按湿度扣返";
  const gameO = "assets/xueximod/textures/domain/water_flow_v3_1.png；同名水纹用于独立 v4 三层立体水带，组合水龙卷仍为 v3.1。挂载后的气泡膜 assets/xueximod/textures/domain/water_bubble_v3_1.png";
  const note = [
    "2026-11-09 用户确认独立旋流 v4：「可以就按这版接入」。正式归档 docs/资源库/特效/水流/独立旋流_v4/。",
    "接入报告 docs/开发记录/术式批量制作/水流组合-20261103/实测返修-20261109-独立旋流/接入报告.md。",
    "本次 compileJava --offline --no-daemon 于 2026-11-09 18:52:17.665 至 18:52:42.033 退出码 0，见 docs/开发记录/术式批量制作/水流组合-20261103/实测返修-20261109-独立旋流/编译报告.md。该次覆盖当时最终 src，DomainFlowRenderer 为 610D26DBF60F63804E7D06EA2CCF96DD3050015F4CF03FC003E8D36876FAB324。不把其他批次之后的改动记成已经编译。",
    "仍待游戏复测。未追加正式复审。",
    "待验收 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md",
  ].join("\n");
  const after = [...current];
  after[13] = gameN;
  after[14] = gameO;
  after[125] = String(current[125] ?? "") + "\n" + note;
  const beforeCounts = Object.fromEntries(statusNames.map((name) => [name, pages[name].length]));
  const details = wb.worksheets.getItem("资源状态明细").getRange("A3:B30").values
    .map((row, index) => ({ row: index + 3, id: row[0], sequence: row[1] }))
    .filter((row) => row.id === "water_vortex");
  const snapshot = {
    writtenBeforeCandidate: true,
    capturedAt: new Date().toISOString(),
    sourceHash,
    sourceMtimeMs: stat.mtimeMs,
    rendererHash,
    textureHash,
    compile: "2026-11-09 18:52:17.665—18:52:42.033 exit 0；1470 个 src 文件未变。不是游戏验收，也不覆盖其他批次后来的改动。",
    from: `制作中第${fromRow}行`,
    to: `待验收第${toRow}行`,
    countsBefore: beforeCounts,
    currentNODV: { N: current[13], O: current[14], DV: current[125] },
    afterNODV: { N: gameN, O: gameO, DV: after[125] },
    current,
    after,
    resourceDetails: details,
  };
  await fs.writeFile(root + "/待验收转换快照.json", JSON.stringify(snapshot, null, 2));
  await fs.writeFile(root + "/authored-rows.json", JSON.stringify({
    sourceHash, fromSheet: "制作中", toSheet: "待验收", fromRow, toRow, id: "water_vortex",
    n: gameN, o: gameO, dv: after[125], after,
    backup: "original-backup-deliver.xlsx", candidate: "candidate-deliver.xlsx",
    artifact: "artifact-authored-deliver.xlsx", preservation: "native-preservation-deliver.json",
  }, null, 2));
  await fs.writeFile(root + "/original-backup-deliver.xlsx", bytes);
  acceptance.getRange(`A${toRow}:EN${toRow}`).copyFrom(making.getRange(`A${fromRow}:EN${fromRow}`), "all");
  acceptance.getRange(`N${toRow}:O${toRow}`).values = [[gameN, gameO]];
  acceptance.getRange(`DV${toRow}`).values = [[after[125]]];
  making.getRange(`A${fromRow}:EN${fromRow}`).clear({ applyTo: "all" });
  wb.recalculate();
  await (await SpreadsheetFile.exportXlsx(wb)).save(root + "/artifact-authored-deliver.xlsx");
  console.log(execFileSync(python, [root + "/preserve-one.py"], { encoding: "utf8" }));
  const candidatePath = root + "/candidate-deliver.xlsx";
  const check = await SpreadsheetFile.importXlsx(await FileBlob.load(candidatePath));
  const afterPages = Object.fromEntries(statusNames.map((name) => [name, locate(check.worksheets.getItem(name))]));
  const afterCounts = Object.fromEntries(statusNames.map((name) => [name, afterPages[name].length]));
  const all = statusNames.flatMap((name) => afterPages[name].map((row) => row.id));
  const total = Object.values(beforeCounts).reduce((sum, value) => sum + value, 0);
  if (all.length !== total || new Set(all).size !== total) throw new Error("Uniqueness or total changed");
  if (afterCounts["制作中"] !== beforeCounts["制作中"] - 1 || afterCounts["待验收"] !== beforeCounts["待验收"] + 1) throw new Error("Counts " + JSON.stringify(afterCounts));
  if (afterCounts["已完成"] !== beforeCounts["已完成"] || afterCounts["待制作"] !== beforeCounts["待制作"]) throw new Error("Other page counts changed");
  const moved = check.worksheets.getItem("待验收").getRange(`A${toRow}:EN${toRow}`).values[0];
  if (JSON.stringify(moved) !== JSON.stringify(after)) throw new Error("Candidate row differs");
  if (afterPages["制作中"].some((row) => row.id === "water_vortex")) throw new Error("Source row remains");
  check.recalculate();
  const scan = await check.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!|#NULL!|#SPILL!|#CALC!",
    options: { useRegex: true, maxResults: 20 },
    maxChars: 1500,
  });
  console.log(scan.ndjson);
  const picture = await check.render({ sheetName: "待验收", range: `A${toRow}:O${toRow}`, scale: 1, format: "png" });
  await fs.writeFile(root + `/after-acceptance-A${toRow}-O${toRow}.png`, new Uint8Array(await picture.arrayBuffer()));
  if (hash(await fs.readFile(live)) !== sourceHash) throw new Error("Source changed before replace");
  await fs.copyFile(candidatePath, live);
  const reopened = await SpreadsheetFile.importXlsx(await FileBlob.load(live));
  const liveN = reopened.worksheets.getItem("待验收").getRange(`N${toRow}`).values[0][0];
  if (liveN !== gameN || hash(await fs.readFile(live)) !== hash(await fs.readFile(candidatePath))) throw new Error("Reopen mismatch");
  console.log(JSON.stringify({ status: "replaced", hash: hash(await fs.readFile(live)), beforeCounts, afterCounts, fromRow, toRow }));
  process.exit(0);
}
const bytes = await fs.readFile(live);
if (hash(bytes) !== baseline) throw new Error("Source hash changed");
const prior = JSON.parse(await fs.readFile(priorPath, "utf8"));
const approved = prior.rows.find((item) => item.id === "water_vortex");
const visual = [
  "2026-11-09 用户原话：「有一些问题，我测试发现旋流如果直接释放，特性是完全平布的地上的这很不好，在单独释放时特效应该和水龙卷底座外层效果差不多。」",
  "单独释放应呈低矮立体回旋水流，接近水龙卷底座外层，不再完全平铺地面。这是本次视觉要求。具体稿仍待真实 Blockbench 预览确认，新外观尚未实现。",
  "组合水龙卷的下窄上宽、连续漏斗螺旋和水下汇入不变。独立玩法、半径 8 格、扣费和组合机制不变。",
].join("\n");
const restoredN = approved.before[13];
const restoredO = approved.before[14] + "\n" + visual;
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(live));
const acceptance = wb.worksheets.getItem("待验收");
const making = wb.worksheets.getItem("制作中");
const current = acceptance.getRange("A10:EN10").values[0];
if (current[1] !== "water_vortex" || current[13] !== approved.after[13] || current[125] !== approved.after[125]) {
  throw new Error("Live vortex row no longer matches the approved acceptance snapshot");
}
if (making.getRange("B18").values[0][0] !== null || acceptance.getRange("B11").values[0][0] !== "water_bubble_column" || acceptance.getRange("B12").values[0][0] !== "water_deep_pressure") {
  throw new Error("Neighbor rows moved");
}
const details = wb.worksheets.getItem("资源状态明细").getRange("A3:B30").values
  .map((row, index) => ({ row: index + 3, id: row[0], sequence: row[1] }))
  .filter((row) => row.id === "water_vortex");
const restoredDv = String(current[125] ?? "") + "\n" + visual + "\n制作中 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md";
const after = [...current];
after[13] = restoredN;
after[14] = restoredO;
after[125] = restoredDv;
const snapshot = {
  writtenBeforeCandidate: true,
  capturedAt: new Date().toISOString(),
  sourceHash: baseline,
  sourceMtime: "2026-11-04 03:03:48.244",
  approvedVersion: "最近完整需求是实测三项-20261104待验收快照中旋流的 before。v3.1 组合外观保留。2026-11-09 只追加单独释放的低矮立体视觉要求。",
  from: "待验收第10行",
  to: "制作中第18行",
  headers: acceptance.getRange("A2:EN2").values[0],
  current: current,
  restored: { N: restoredN, O: restoredO, DV: restoredDv },
  resourceDetails: details,
  gameplayUnchanged: "独立玩法、半径 8 格、扣费和组合机制不变。没有气泡时的平面环绕仍是运动规则。",
};
await fs.writeFile(root + "/需求快照.json", JSON.stringify(snapshot, null, 2));
await fs.writeFile(root + "/authored-rows.json", JSON.stringify({
  sourceHash: baseline, fromRow: 10, toRow: 18, id: "water_vortex", n: restoredN, o: restoredO, dv: restoredDv, after,
}, null, 2));
await fs.writeFile(root + "/original-backup.xlsx", bytes);
making.getRange("A18:EN18").copyFrom(acceptance.getRange("A10:EN10"), "all");
making.getRange("N18:O18").values = [[restoredN, restoredO]];
making.getRange("DV18").values = [[restoredDv]];
acceptance.getRange("A10:EN10").clear({ applyTo: "all" });
wb.recalculate();
await (await SpreadsheetFile.exportXlsx(wb)).save(root + "/artifact-authored.xlsx");
console.log(execFileSync(python, [root + "/preserve-one.py"], { encoding: "utf8" }));
const candidatePath = root + "/candidate.xlsx";
const check = await SpreadsheetFile.importXlsx(await FileBlob.load(candidatePath));
const counts = {};
const all = [];
for (const name of ["已完成", "待制作", "制作中", "待验收"]) {
  const ids = check.worksheets.getItem(name).getRange("B3:B80").values.flat().filter((value) => value !== null && value !== "");
  counts[name] = ids.length;
  all.push(...ids);
}
if (all.length !== 62 || new Set(all).size !== 62 || JSON.stringify(counts) !== JSON.stringify({ 已完成: 37, 待制作: 0, 制作中: 16, 待验收: 9 })) {
  throw new Error("Counts or uniqueness mismatch " + JSON.stringify(counts));
}
const moved = check.worksheets.getItem("制作中").getRange("A18:EN18").values[0];
if (JSON.stringify(moved) !== JSON.stringify(after)) throw new Error("Candidate row differs");
if (check.worksheets.getItem("待验收").getRange("B10").values[0][0] !== null) throw new Error("Source row remains");
if (check.worksheets.getItem("待验收").getRange("B11").values[0][0] !== "water_bubble_column" || check.worksheets.getItem("待验收").getRange("B12").values[0][0] !== "water_deep_pressure") {
  throw new Error("Other water rows moved");
}
check.recalculate();
const scan = await check.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 20 },
  maxChars: 1500,
});
console.log(scan.ndjson);
const picture = await check.render({ sheetName: "制作中", range: "A18:O18", scale: 1, format: "png" });
await fs.writeFile(root + "/after-making-A18-O18.png", new Uint8Array(await picture.arrayBuffer()));
if (hash(await fs.readFile(live)) !== baseline) throw new Error("Source changed before replace");
await fs.copyFile(candidatePath, live);
const reopened = await SpreadsheetFile.importXlsx(await FileBlob.load(live));
const liveRow = reopened.worksheets.getItem("制作中").getRange("N18:O18").values[0];
if (liveRow[0] !== restoredN || liveRow[1] !== restoredO || hash(await fs.readFile(live)) !== hash(await fs.readFile(candidatePath))) {
  throw new Error("Reopen mismatch");
}
console.log(JSON.stringify({
  status: "replaced",
  hash: hash(await fs.readFile(live)),
  counts,
  from: "待验收第10行",
  to: "制作中第18行",
}));
