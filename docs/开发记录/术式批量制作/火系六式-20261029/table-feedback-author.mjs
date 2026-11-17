import crypto from "node:crypto";
import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const livePath = "D:/alianximod/docs/术式制作表.xlsx";
const backupPath = `${root}/备份/table-feedback-术式制作表-20261029-032140-1.xlsx`;
const artifactPath = `${root}/table-feedback-candidate-artifact.xlsx`;
const preservedPath = `${root}/table-feedback-candidate-preserved.xlsx`;
const baselineSha = "053366B785B3C9B25C4E43B75F36F93F68C142655740EFAB1CF5F79FBF43B3B6";
const recordLink = "docs/开发记录/术式批量制作/火系六式-20261029/记录.md";
const processMarker = `制作中 | 火系六式-20261029 | ${recordLink}`;
const feedback = {
  fire_liaoyuan: `[设计反馈 2026-10-29] 用户已确认燎原v2。从原点向外铺火浪，之后维持贴地火海/低矮火焰/临时炭纹。原N数值与不改地形约束保留。确认版归档docs/资源库/特效/火焰/燎原_v2/。详细原话与过程见 ${recordLink}。`,
  fire_liaoli: `[设计反馈 2026-10-29] 用户已确认燎里v6。八剑左右镜像，剑柄向后倾；火从玩家背后逐渐覆盖剑柄与剑身；手持刺击双流朝玩家身后；同时3道环不断增大并后移。透明视觉不新增攻击。确认版归档docs/资源库/特效/火焰/燎里_v6/。详细原话与过程见 ${recordLink}。`,
  fire_fentian: `[设计反馈 2026-10-29] 用户强调“特效是用在敌人身上的”，已确认焚天v2：金白火柱/赤红爆燃/4tick脉冲/余烬，按各敌人实际计划段数播放。确认版归档docs/资源库/特效/火焰/焚天_v2/。详细原话与过程见 ${recordLink}。`,
  fire_heat_up: `[设计反馈 2026-10-29] 用户已确认炉心三式v1。升温是普通炉心低密度暖光。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 ${recordLink}。`,
  fire_burn: `[设计反馈 2026-10-29] 用户已确认炉心三式v1。燃烧是过载较强橙红反馈。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 ${recordLink}。`,
  fire_cool: `[设计反馈 2026-10-29] 用户已确认炉心三式v1。冷却只在成功主动cool时播放一次蒸汽。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 ${recordLink}。`,
};
const rows = [
  ["fire_liaoyuan", 3],
  ["fire_liaoli", 4],
  ["fire_fentian", 5],
  ["fire_heat_up", 6],
  ["fire_burn", 7],
  ["fire_cool", 8],
];

function sha256(bytes) {
  return crypto.createHash("sha256").update(bytes).digest("hex").toUpperCase();
}

const mode = process.argv.includes("--reopen") ? "reopen" : process.argv.includes("--apply") ? "apply" : "";
if (!mode) {
  console.error("Use exactly one of --apply or --reopen");
  process.exit(2);
}

async function loadWorkbook(path) {
  return SpreadsheetFile.importXlsx(await FileBlob.load(path));
}

function sheetCounts(workbook) {
  const counts = {};
  const found = [];
  for (const sheetName of ["已完成", "待制作", "制作中", "待验收"]) {
    const values = workbook.worksheets.getItem(sheetName).getUsedRange(true)?.values ?? [];
    let count = 0;
    for (let rowIndex = 2; rowIndex < values.length; rowIndex += 1) {
      const code = values[rowIndex]?.[1];
      if (code != null && String(code).trim() !== "") {
        count += 1;
        found.push([String(code).trim(), sheetName, rowIndex + 1]);
      }
    }
    counts[sheetName] = count;
  }
  return { counts, found };
}

if (mode === "reopen") {
  const reopenPath = process.argv.includes("--live") ? livePath : preservedPath;
  const workbook = await loadWorkbook(reopenPath);
  const { counts, found } = sheetCounts(workbook);
  const making = workbook.worksheets.getItem("制作中");
  const failed = [];
  const notes = [];
  for (const [code, row] of rows) {
    const actual = String(making.getRange(`B${row}`).values[0][0] ?? "");
    const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
    const name = String(making.getRange(`A${row}`).values[0][0] ?? "");
    notes.push({ code, row, name, endsWithFeedback: note.endsWith(feedback[code]), keepsMarker: note.includes(processMarker) });
    if (actual !== code || !note.endsWith(feedback[code]) || !note.includes(processMarker)) failed.push(code);
  }
  const codes = found.map((item) => item[0]);
  if (codes.length !== new Set(codes).size || counts["制作中"] !== 6 || counts["待制作"] !== 0) failed.push("counts");
  console.log(JSON.stringify({ reopened: reopenPath, counts, total: codes.length, unique: new Set(codes).size, notes, failed }));
  if (failed.length) process.exit(1);
  process.exit(0);
}

const backupBytes = await fs.readFile(backupPath);
if (sha256(backupBytes) !== baselineSha) throw new Error("Backup hash is not the baseline");
const liveHash = sha256(await fs.readFile(livePath));
if (liveHash !== baselineSha) throw new Error(`Live saved hash changed before authoring: ${liveHash}`);

const workbook = await loadWorkbook(backupPath);
const making = workbook.worksheets.getItem("制作中");
const originals = {};
for (const [code, row] of rows) {
  const actual = making.getRange(`B${row}`).values[0][0];
  const note = making.getRange(`DV${row}`).values[0][0];
  const feature = making.getRange(`N${row}`).values[0][0];
  const effect = making.getRange(`O${row}`).values[0][0];
  if (actual !== code) throw new Error(`Id mismatch at row ${row}: ${actual}`);
  if (typeof note !== "string" || !note.includes(processMarker) || note.includes("[设计反馈 2026-10-29]")) {
    throw new Error(`DV${row} is not ready for one feedback append`);
  }
  originals[code] = { note, feature, effect };
  making.getRange(`DV${row}`).values = [[`${note.endsWith("\n") ? note : `${note}\n`}${feedback[code]}`]];
}
for (const [code, row] of rows) {
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  const feature = making.getRange(`N${row}`).values[0][0];
  const effect = making.getRange(`O${row}`).values[0][0];
  if (feature !== originals[code].feature || effect !== originals[code].effect) {
    throw new Error(`N/O changed for ${code}`);
  }
  if (!note.startsWith(originals[code].note) || !note.endsWith(feedback[code])) {
    throw new Error(`DV append mismatch for ${code}`);
  }
}
workbook.recalculate();
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, liveHash, appended: rows.map(([code, row]) => ({ code, row })) }));
