import crypto from "node:crypto";
import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const livePath = "D:/alianximod/docs/术式制作表.xlsx";
const backupPath = `${root}/备份/table-result-术式制作表-20261029-043043-1.xlsx`;
const artifactPath = `${root}/table-result-candidate-artifact.xlsx`;
const preservedPath = `${root}/table-result-candidate-preserved.xlsx`;
const baselineSha = "01819BBEA99BB4CD6A9F9E398C9779E39D09962140A372E84FFDD9B8A874DF0C";
const recordLink = "docs/开发记录/术式批量制作/火系六式-20261029/记录.md";
const compileLink = "docs/开发记录/术式批量制作/火系六式-20261029/compile-final-report.md";
const result = `2026-10-29本轮执行结果：已确认素材接入。compile-final报告的确认命令.\\gradlew.bat compileJava --offline --no-daemon于2026-10-29 04:24:17至04:24:31退出0，BUILD SUCCESSFUL，compileJava为UP-TO-DATE。04:17同命令包装进程因日志管道堵住被终止，没有交回退出码，不当成成功。定向检查javac --release 17退出0；SixFireLogicCheck、LiaoliV6LayoutCheck、FireVisualLayoutCheck、KnownLimitsCheck、FurnaceAtomicCheck退出0；FurnaceStructureCheck断言19失败0、FurnaceRepairCheck断言15失败0、FireReleaseStructureCheck断言41失败0，均退出0。参数被拆开的java启动退出1，不是断言失败。未跑全量compileTestJava，未打包，未启动游戏；Mixin、持续伤害、炉心生命周期、视觉、关系过滤和击退都未游戏验证。这些退出码不是正式复审通过。设计/实现额度2/2已用完，本阶段额度已用完，返修结果未追加复审。游戏未实测，保持制作中。详见 ${recordLink} 与 ${compileLink}。`;
const seedNote = {
  fire_liaoyuan: `[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。燎原无火种的地表火伤仍可按现有入口准备，但有火种的一次消费和附着燃烧目前不能按正常玩法准备。该缺口未解决，这里不新增产火种途径或调试命令。详见 ${compileLink}。`,
  fire_liaoli: `[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。燎里无火种的物理刺击仍可按现有入口准备，但火种大于0时的追加火伤和一次消费目前不能按正常玩法准备。该缺口未解决，这里不新增产火种途径或调试命令。详见 ${compileLink}。`,
  fire_fentian: `[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。焚天只处理火种大于0的目标，因此本式点燃和段伤害目前不能按正常玩法准备。该缺口未解决，这里不新增产火种途径或调试命令。详见 ${compileLink}。`,
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

function additionFor(code) {
  return seedNote[code] ? `${result}\n${seedNote[code]}` : result;
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
    const addition = additionFor(code);
    const ok = actual === code && note.endsWith(addition) && note.includes(recordLink) && note.includes(compileLink);
    const seedOk = seedNote[code] ? note.includes(seedNote[code]) : !note.includes("FireSeedState.grant在主源码没有调用方");
    notes.push({ code, row, ok, seedOk });
    if (!ok || !seedOk) failed.push(code);
  }
  const codes = found.map((item) => item[0]);
  if (codes.length !== new Set(codes).size || counts["制作中"] !== 6 || counts["待制作"] !== 0) failed.push("counts");
  console.log(JSON.stringify({ reopened: reopenPath, counts, total: codes.length, unique: new Set(codes).size, notes, failed }));
  if (failed.length) process.exit(1);
  process.exit(0);
}

if (sha256(await fs.readFile(backupPath)) !== baselineSha) throw new Error("Backup hash is not the baseline");
const liveHash = sha256(await fs.readFile(livePath));
if (liveHash !== baselineSha) throw new Error(`Live saved hash changed before authoring: ${liveHash}`);

const workbook = await loadWorkbook(backupPath);
const making = workbook.worksheets.getItem("制作中");
const originals = {};
for (const [code, row] of rows) {
  const actual = making.getRange(`B${row}`).values[0][0];
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  if (actual !== code) throw new Error(`Id mismatch at row ${row}: ${actual}`);
  if (note.includes("2026-10-29本轮执行结果")) throw new Error(`DV${row} already has this execution result`);
  originals[code] = {
    note,
    feature: making.getRange(`N${row}`).values[0][0],
    effect: making.getRange(`O${row}`).values[0][0],
  };
  const addition = additionFor(code);
  making.getRange(`DV${row}`).values = [[`${note.endsWith("\n") ? note : `${note}\n`}${addition}`]];
}
for (const [code, row] of rows) {
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  if (making.getRange(`N${row}`).values[0][0] !== originals[code].feature) throw new Error(`N changed for ${code}`);
  if (making.getRange(`O${row}`).values[0][0] !== originals[code].effect) throw new Error(`O changed for ${code}`);
  if (!note.startsWith(originals[code].note) || !note.endsWith(additionFor(code))) throw new Error(`DV append mismatch for ${code}`);
}
workbook.recalculate();
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, liveHash, appended: rows.map(([code, row]) => ({ code, row })) }));
