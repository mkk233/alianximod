import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const root = 'D:/alianximod/docs/开发记录/制作表资源状态通用化-20261028';
const mode = process.argv[2];
const inputPath = process.argv[3];
if (!mode || !inputPath) {
  console.error('usage: migration-render.mjs <before|after> <xlsx>');
  process.exit(1);
}
const shots = mode === 'before'
  ? [
      ['before-ids', '已完成', 'A1:B14'],
      ['before-old-domain', '已完成', 'DC1:DM2'],
      ['before-catalog', '数据表', 'A1:G8'],
      ['before-pending', '待制作', 'A1:B5'],
    ]
  : [
      ['after-group-header', '已完成', 'DV1:EN2'],
      ['after-blade', '已完成', 'B13:B13'],
      ['after-blade-group', '已完成', 'DW13:EN13'],
      ['after-domain-rows', '已完成', 'B26:B28'],
      ['after-domain-group', '已完成', 'DW26:EN28'],
      ['after-tide', '已完成', 'DW38:EN38'],
      ['after-fire', '待制作', 'DW1:EN5'],
      ['after-dictionary', '数据表', 'AW1:BH9'],
      ['after-lists', '数据表', 'BJ1:BV12'],
      ['after-detail', '资源状态明细', 'A1:T6'],
    ];
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
for (const [name, sheetName, range] of shots) {
  const preview = await workbook.render({ sheetName, range, scale: 1.25, format: 'png' });
  await fs.writeFile(`${root}/${name}.png`, new Uint8Array(await preview.arrayBuffer()));
  console.log(name);
}
console.log('rendered', mode);
