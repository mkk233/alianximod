import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const livePath = "D:/alianximod/docs/术式制作表.xlsx";
const workDir = "D:/alianximod/docs/开发记录/让锋断潮名称整合-20261103/表格工作";
const sentence = "2026-11-03 用户最新决定：仅改名，原停用计划取消；保留功能及水幻形可用，不迁待验收，保持当前子表。";
const expected = {
  tide_sever: { oldName: "断潮", newName: "薄水切", n: "一道薄而长的水切", o: "TideSeverEffect" },
  water_yielding_ribbon: { oldName: "让锋", newName: "断潮", n: "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", o: "WaterYieldingEffect" },
};
const statusSheets = ["已完成", "待制作", "制作中", "待验收"];

function fail(message) {
  throw new Error(message);
}

function sha256(bytes) {
  return crypto.createHash("sha256").update(bytes).digest("hex").toUpperCase();
}

function stamp() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, "0");
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
}

function textOf(value) {
  if (value == null) return "";
  return String(value);
}

async function main() {
  const liveBytes = await fs.readFile(livePath);
  const baseline = sha256(liveBytes);
  const liveStat = await fs.stat(livePath);
  const mark = stamp();
  const backupDir = path.join(workDir, "备份");
  const candidateDir = path.join(workDir, "候选");
  const snapDir = path.join(workDir, "快照");
  await fs.mkdir(backupDir, { recursive: true });
  await fs.mkdir(candidateDir, { recursive: true });
  await fs.mkdir(snapDir, { recursive: true });
  const backupPath = path.join(backupDir, `术式制作表-原位-${mark}-1.xlsx`);
  const candidatePath = path.join(candidateDir, `术式制作表-候选-原位-${mark}-1.xlsx`);
  const snapPath = path.join(snapDir, `原位改名前-${mark}-1.json`);
  await fs.writeFile(backupPath, liveBytes);

  const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(livePath));
  const located = {};
  const counts = {};
  for (const title of statusSheets) {
    const sheet = workbook.worksheets.getItem(title);
    const values = sheet.getRange("A3:EN80").values;
    const ids = [];
    values.forEach((row, index) => {
      const code = textOf(row[1]).trim();
      if (!code) return;
      const excelRow = index + 3;
      ids.push(code);
      if (expected[code]) {
        located[code] = located[code] || [];
        located[code].push({ sheet: title, row: excelRow, values: row });
      }
    });
    counts[title] = ids.length;
  }
  for (const code of Object.keys(expected)) {
    const hits = located[code] || [];
    if (hits.length !== 1 || hits[0].sheet !== "制作中") {
      fail(`${code} 不在唯一的制作中行`);
    }
  }
  if (counts["已完成"] !== 38 || counts["待制作"] !== 0 || counts["制作中"] !== 15 || counts["待验收"] !== 7) {
    fail(`四页计数异常 ${JSON.stringify(counts)}`);
  }

  const before = {};
  for (const [code, spec] of Object.entries(expected)) {
    const hit = located[code][0];
    const row = hit.values;
    const current = {
      sheet: hit.sheet,
      row: hit.row,
      A: textOf(row[0]),
      B: textOf(row[1]),
      N: textOf(row[13]),
      O: textOf(row[14]),
      DV: textOf(row[125]),
      cells: row.map((value) => (value == null ? null : value)),
    };
    if (current.B !== code || current.N !== spec.n || current.O !== spec.o) {
      fail(`${code} 的编号、功能或资源标识已变`);
    }
    if (current.A !== spec.oldName && current.A !== spec.newName) {
      fail(`${code} 的现名不是${spec.oldName}`);
    }
    if (!current.DV.startsWith("2026-11-03 用户原话：")) {
      fail(`${code} 的历史备注丢失`);
    }
    before[code] = current;
  }

  const sheet = workbook.worksheets.getItem("制作中");
  const edited = {};
  for (const [code, spec] of Object.entries(expected)) {
    const current = before[code];
    const nameCell = sheet.getRange(`A${current.row}`);
    if (current.A !== spec.newName) nameCell.values = [[spec.newName]];
    const noteCell = sheet.getRange(`DV${current.row}`);
    const nextNote = current.DV.includes(sentence) ? current.DV : `${current.DV}\n${sentence}`;
    if (nextNote !== current.DV) noteCell.values = [[nextNote]];
    edited[code] = { row: current.row, name: spec.newName, note: nextNote };
  }

  workbook.recalculate();
  for (const [code, spec] of Object.entries(expected)) {
    const row = sheet.getRange(`A${edited[code].row}:EN${edited[code].row}`).values[0];
    if (textOf(row[0]) !== spec.newName || textOf(row[1]) !== code || textOf(row[13]) !== spec.n || textOf(row[14]) !== spec.o) {
      fail(`${code} 写后核对失败`);
    }
    if (textOf(row[125]) !== edited[code].note || !textOf(row[125]).startsWith(before[code].DV)) {
      fail(`${code} 备注没有保留原文`);
    }
    for (let index = 0; index < row.length; index += 1) {
      if (index === 0 || index === 125) continue;
      if (textOf(row[index]) !== textOf(before[code].cells[index])) fail(`${code} 第 ${index + 1} 列变化`);
    }
  }
  const check = await workbook.inspect({
    kind: "region",
    sheetId: "制作中",
    range: "A16:B17",
    include: "values",
    tableMaxRows: 4,
    tableMaxCols: 2,
    maxChars: 2000,
  });

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(candidatePath);
  const candidateBytes = await fs.readFile(candidatePath);
  const snapshot = {
    baselineSha256: baseline,
    baselineMtime: liveStat.mtime.toISOString(),
    baselineSize: liveStat.size,
    rows: before,
    edited,
  };
  await fs.writeFile(snapPath, JSON.stringify(snapshot, null, 2), "utf8");
  const preview = {
    ready: true,
    baselineSha256: baseline,
    baselineMtime: liveStat.mtime.toISOString(),
    baselineSize: liveStat.size,
    backup: backupPath,
    candidate: candidatePath,
    candidateSha256: sha256(candidateBytes),
    snapshot: snapPath,
    live: livePath,
    moves: edited,
    counts,
    inspect: check.ndjson,
  };
  await fs.writeFile(path.join(workDir, "prepare-inplace.json"), JSON.stringify(preview, null, 2), "utf8");
  console.log(JSON.stringify({
    baseline,
    candidate: preview.candidateSha256,
    tide: { row: edited.tide_sever.row, name: edited.tide_sever.name },
    ribbon: { row: edited.water_yielding_ribbon.row, name: edited.water_yielding_ribbon.name },
  }));
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exit(1);
});
