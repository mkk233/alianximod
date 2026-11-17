import fs from "node:fs/promises";
import crypto from "node:crypto";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/炉心三式入待制作-20261029";
const sourcePath = "D:/alianximod/docs/术式制作表.xlsx";
const artifactPath = `${root}/candidate-artifact.xlsx`;
const preservedPath = `${root}/candidate-preserved.xlsx`;
const snapshotPath = `${root}/input-snapshot.json`;

const lifecycle = "牛奶不能移除炉心。登出保存累计F，登录不补离线时间。死亡清除炉心，不喷蒸汽，不改升温、燃烧、冷却三式的技能冷却。其它非主动移除不喷蒸汽。普通升到过载时保留F，属性修饰不叠两份。失败的状态切换不扣魔力、不进入冷却、不改变炉心。";

const sharedTail = [
  "用户原话：“把三个技能写入待制作表中”。",
  "主设计是 docs/火属性二级玩法设计.md。原始三式数值为显式魔力20/20/0、技能冷却500/500/0 tick、装配消耗各1、基础伤害各0。本次只写入待制作。炉心底层服务已经存在，这三个技能尚未注册；底层存在不等于这三式已经实现。本记录不包含游戏验证。",
  "记录目录：docs/开发记录/炉心三式入待制作-20261029/。",
  lifecycle,
  "等级上限20沿用制作表已有数据行的统一取值。复杂度、登记弹速、可接受附魔、自身能量倍率、挂到后续的能量倍率没有统一取值，留空，不沿用燎原、燎里、焚天的复杂度20和装配3。弹道、命中体尺寸、外形、弧带、场地、护体、屏障、领域、附魔累积、旧档和火种列留空。登记魔力按表头批注由基础伤害0得到0，实际消耗只看显式魔力，不另扣一次。蓄力tick为0。双重释放再拉段数为0。释放种类“自身状态”是本次在数据表追加的名称，代码列没有已确认的程序常量，留空。",
  "本式只在主表配置一条炉心状态，不写入资源状态明细。",
].join("\n");

const noChain = "本式没有弹道和命中体，不产生普通攻击实例，不调用原版着火，不生产或消费火种。链上角色用表内既有“动作”，不新增链上角色，也不做成攻击命中体。拒绝与其它段放在同一条链，不接受索敌、落点、追踪、垂落、一分为三、双重释放、聚能、三维交错。以后按通用自身状态接入，不为这个术式编号单独写链路分支。";

const rows = [
  {
    row: 6,
    id: "fire_heat_up",
    values: {
      A: "升温",
      B: "fire_heat_up",
      C: "法术",
      D: "火",
      E: 0,
      F: 500,
      G: 20,
      J: "否",
      K: 1,
      L: 0,
      N: [
        "升温是独立的火属性法术，只让施法者进入普通炉心。仅当前没有炉心时可以成功。成功时调用FurnaceService.enterNormal，amplifier为0。已经处于普通或过载时失败，不刷新热量、状态或计时。",
        "普通炉心无限持续。累计火伤F每tick增加0.002，150tick到达0.30并封顶。产火种倍率为1.12+0.48×clamp(F/0.30,0,1)。炉心不被动产生火种。生命上限×0.75，护甲和韧性各×0.50，治疗×0.50，水或冰承伤×1.40。保护附魔仍按原规则计算。施法者自己的抗性提升不再减伤，不移除抗性药水。进入时当前生命只钳到新上限，不回复生命。",
        noChain,
        "服务端先检查没有炉心、魔力、冷却和装配限制，通过后一次提交。失败不扣魔力、不进入冷却、不改变炉心。只有成功才扣显式魔力20，并从成功施放起计算500tick技能冷却。装配消耗1，基础伤害0。升温、燃烧、冷却各占栏位，各自计算魔力和冷却，不互相替换，也不重置另外两式的冷却。",
        lifecycle,
      ].join("\n"),
      O: "施放时，施法者周围出现短促的暖橙火星和轻微热浪。普通炉心保持期间只用低密度暖色提示，不遮挡视线。不点燃地形，不生成攻击体。这里只记录制作意图，不填写资源文件名。",
      P: "自身状态",
      Q: "触发",
      S: "动作",
      T: "否",
      X: 0,
      Y: "否",
      Z: "否",
      AA: "否",
      AB: "否",
      AC: "否",
      AD: "否",
      AE: "否",
      AH: "否",
      AI: "否",
      AJ: "否",
      AK: "否",
      AL: "否",
      AM: "是",
      AP: 20,
      AQ: 0,
      DV: ["本行是升温 / fire_heat_up。", sharedTail].join("\n"),
      DW: "是",
      DY: "炉心",
      DZ: "施加",
      EA: "施术者",
      EB: "成功施放",
      ED: 0,
      EE: "无限",
      EJ: "仅无炉心",
      EK: "成功才调用FurnaceService.enterNormal，进入普通炉心amplifier 0。已有普通或过载则失败，不刷新F、状态或计时。失败不扣魔力、不进冷却、不改变炉心。不产火种。",
      EL: "N/DV",
      EM: "单一炉心配置。不产火种。无附加明细。",
    },
  },
  {
    row: 7,
    id: "fire_burn",
    values: {
      A: "燃烧",
      B: "fire_burn",
      C: "法术",
      D: "火",
      E: 0,
      F: 500,
      G: 20,
      J: "否",
      K: 1,
      L: 0,
      N: [
        "燃烧是独立的火属性法术。没有炉心时可以直接进入过载，amplifier为1。已有普通炉心时升级到amplifier 1，并保留已累计的F，不能先移除再重新进入。已经过载时失败，不刷新计时。成功时调用FurnaceService.enterOverdrive。不能从过载直接降回普通。",
        "过载炉心无限持续。F每tick增加0.003，封顶0.60。从0到封顶为200tick；普通炉心已满热0.30时再升级，还需100tick到顶。产火种倍率固定2.2。炉心不被动产生火种。生命上限×0.60，护甲和韧性各×0.25，治疗×0.30，水或冰承伤×1.70。保护附魔仍按原规则计算。施法者自己的抗性提升不再减伤，不移除抗性药水。升级时当前生命只钳到新上限，不回复生命。属性修饰使用可移除的稳定标识，不叠成两份。",
        noChain,
        "服务端先检查炉心状态、魔力、冷却和装配限制，通过后一次提交。失败不扣魔力、不进入冷却、不改变炉心。只有成功才扣显式魔力20，并从成功施放起计算独立的500tick技能冷却。装配消耗1，基础伤害0。不重置升温或冷却各自的冷却。",
        lifecycle,
      ].join("\n"),
      O: "施放时，施法者周围出现短促、更明亮的橙红火星和热浪。过载期间的提示强于普通炉心。从普通升级到过载只播放一次升级反馈，不重复播放普通入场。不蔓延，不点燃地形，不额外造成伤害。这里只记录制作意图，不填写资源文件名。",
      P: "自身状态",
      Q: "触发",
      S: "动作",
      T: "否",
      X: 0,
      Y: "否",
      Z: "否",
      AA: "否",
      AB: "否",
      AC: "否",
      AD: "否",
      AE: "否",
      AH: "否",
      AI: "否",
      AJ: "否",
      AK: "否",
      AL: "否",
      AM: "是",
      AP: 20,
      AQ: 0,
      DV: [
        "本行是燃烧 / fire_burn。",
        "表内操作“施加”在本行表示进入或升级为过载并保留F，不是先移除再重新进入。已有过载则失败。",
        sharedTail,
      ].join("\n"),
      DW: "是",
      DY: "炉心",
      DZ: "施加",
      EA: "施术者",
      EB: "成功施放",
      ED: 1,
      EE: "无限",
      EJ: "无炉心或普通炉心；已有过载失败",
      EK: "表内“施加”表示FurnaceService.enterOverdrive：无炉心直接进入过载amplifier 1；普通炉心升级为amplifier 1并保留累计F，不能先移除再进入。已有过载失败且不刷新计时。不能从过载降为普通。失败不扣魔力、不进冷却、不改变炉心。",
      EL: "N/DV",
      EM: "单一炉心配置。升级保留F。不产火种。无附加明细。",
    },
  },
  {
    row: 8,
    id: "fire_cool",
    values: {
      A: "冷却",
      B: "fire_cool",
      C: "法术",
      D: "火",
      E: 0,
      F: 0,
      G: 20,
      J: "否",
      K: 1,
      L: 0,
      N: [
        "冷却是独立的火属性法术。仅普通或过载炉心可以成功。成功时调用FurnaceService.cool，主动结束炉心并触发一次蒸汽。没有炉心时失败，不喷蒸汽。不能用普通removeEffect或end passive代替cool，也不能靠通用移除监听产生蒸汽。",
        "成功后恢复生命上限、护甲、韧性等被炉心改变的状态属性，当前生命不增加。蒸汽以身体中心为球心，半径3格，排除自己、队友和关系规则中的友方。每个存活敌对目标结算一次，固定2点基础蒸汽伤害，再经过该目标的正常防御。不读取炉心F、火种、能量或规则，不生产或消费火种，不标记火、水、冰。击退使用普通碰撞和击退抗性；空旷且没有抗性时大约3格，不能穿墙保证距离。这2点是内部蒸汽伤害，不能写成术式基础伤害；本式基础伤害仍是0。",
        noChain,
        "显式魔力0，技能冷却0tick，装配消耗1。不重置升温或燃烧各自的500tick冷却。服务端先检查已有炉心、魔力、冷却和装配限制，通过后一次提交。失败不扣魔力、不进入冷却、不改变炉心、不喷蒸汽。只有成功才结束炉心并结算这一次蒸汽。",
        lifecycle,
      ].join("\n"),
      O: "成功退出炉心时，释放一次向外扩散的白色蒸汽环，并带上随后消散的雾气。没有炉心时不播放。不得留下地形火或持续攻击体。这里只记录制作意图，不填写资源文件名。",
      P: "自身状态",
      Q: "触发",
      S: "动作",
      T: "否",
      X: 0,
      Y: "否",
      Z: "否",
      AA: "否",
      AB: "否",
      AC: "否",
      AD: "否",
      AE: "否",
      AH: "否",
      AI: "否",
      AJ: "否",
      AK: "否",
      AL: "否",
      AM: "是",
      AP: 0,
      AQ: 0,
      DV: [
        "本行是冷却 / fire_cool。",
        "表内操作“移除”必须调用FurnaceService.cool，成功后才产生一次蒸汽。不能用removeEffect、endPassive或通用移除监听代替。内部蒸汽伤害不写入基础伤害。",
        sharedTail,
      ].join("\n"),
      DW: "是",
      DY: "炉心",
      DZ: "移除",
      EA: "施术者",
      EB: "成功施放",
      EE: "不适用",
      EJ: "仅普通或过载炉心；无炉心失败",
      EK: "表内“移除”必须是FurnaceService.cool的成功主动结束，并只在那时触发一次蒸汽。不能用removeEffect、endPassive或通用移除监听产生蒸汽。无炉心不喷蒸汽。蒸汽的2点基础伤害不写入术式基础伤害。",
      EL: "N/DV",
      EM: "单一炉心配置。主动cool才蒸汽。无附加明细。",
    },
  },
];

const enumCells = {
  I7: "自身状态",
  K7: "自身状态，不生成弹道或命中体",
};

const omitted = [
  "H复杂度",
  "I登记弹速",
  "M可接受附魔",
  "R弹道",
  "U命中体存活tick",
  "V飞行速度",
  "W厚度",
  "AF弧带半径",
  "AG弧带半宽",
  "AN自身能量倍率",
  "AO挂到后续的能量倍率",
  "DX资源",
  "EC数值",
  "EF寿命tick",
  "EG周期tick",
  "EH首次延迟tick",
  "EI计量单位",
  "EN附加明细键",
  "冷却ED状态amplifier",
  "数据表J7释放种类代码",
  "AR至BW以及护体、屏障、领域、附魔累积、旧档列",
];

function columnIndex(column) {
  return [...column].reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0) - 1;
}

function same(actual, expected) {
  if (typeof expected === "number") return Number(actual) === expected;
  return actual === expected;
}

const preview = process.argv.includes("--preview");
const apply = process.argv.includes("--apply");
if (preview === apply) {
  console.error("Use exactly one of --apply or --preview");
  process.exit(2);
}

if (preview) {
  const inputPath = process.argv.includes("--preserved") ? preservedPath : sourcePath;
  const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
  const previewBlob = await workbook.render({
    sheetName: "待制作",
    range: "N6:O8",
    scale: 2,
    format: "png",
  });
  await fs.writeFile(`${root}/preview-notes.png`, new Uint8Array(await previewBlob.arrayBuffer()));
  console.log(JSON.stringify({ preview: `${root}/preview-notes.png`, source: inputPath }));
  process.exit(0);
}

const baseline = JSON.parse((await fs.readFile(`${root}/baseline.json`, "utf8")).replace(/^\uFEFF/, ""));
const sourceBytes = await fs.readFile(sourcePath);
const sourceHash = crypto.createHash("sha256").update(sourceBytes).digest("hex").toUpperCase();
if (sourceHash !== baseline.sha256) {
  throw new Error(`Source hash changed before authoring: ${sourceHash}`);
}

const snapshot = {
  source: sourcePath,
  baselineSha256: sourceHash,
  sheet: "待制作",
  rows,
  enumSheet: "数据表",
  enumCells,
  omitted,
  namedRange: { name: "释放种类", from: "数据表!$I$4:$I$6", to: "数据表!$I$4:$I$7" },
  filter: { from: "A2:EN5", to: "A2:EN8" },
  dimension: { from: "A1:EN5", to: "A1:EN8" },
};
await fs.writeFile(snapshotPath, JSON.stringify(snapshot, null, 2), "utf8");

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
const sheet = workbook.worksheets.getItem("待制作");
const catalog = workbook.worksheets.getItem("数据表");
for (const entry of rows) {
  for (const [column, value] of Object.entries(entry.values)) {
    sheet.getRange(`${column}${entry.row}`).values = [[value]];
  }
}
for (const [address, value] of Object.entries(enumCells)) {
  catalog.getRange(address).values = [[value]];
}
workbook.recalculate();

const mismatches = [];
for (const entry of rows) {
  const columns = Object.keys(entry.values);
  const last = columns.reduce((best, column) => columnIndex(column) > columnIndex(best) ? column : best);
  const actual = sheet.getRange(`A${entry.row}:${last}${entry.row}`).values[0];
  for (const [column, expected] of Object.entries(entry.values)) {
    if (!same(actual[columnIndex(column)], expected)) {
      mismatches.push({ cell: `${column}${entry.row}`, expected, actual: actual[columnIndex(column)] });
    }
  }
}
for (const [address, expected] of Object.entries(enumCells)) {
  const actual = catalog.getRange(address).values[0][0];
  if (!same(actual, expected)) mismatches.push({ cell: `数据表!${address}`, expected, actual });
}
if (mismatches.length) {
  console.error(JSON.stringify(mismatches.slice(0, 12), null, 2));
  throw new Error(`Author mismatch count ${mismatches.length}`);
}

const inspect = await workbook.inspect({
  kind: "table",
  range: "待制作!A6:S8",
  include: "values",
  tableMaxRows: 4,
  tableMaxCols: 19,
  maxChars: 4000,
});
console.log(inspect.ndjson);
const errors = await workbook.inspect({
  kind: "match",
  range: "待制作!A6:EN8",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 50 },
  summary: "new row formula error scan",
  maxChars: 1500,
});
console.log(errors.ndjson);
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, snapshot: snapshotPath, mismatches: 0 }));
