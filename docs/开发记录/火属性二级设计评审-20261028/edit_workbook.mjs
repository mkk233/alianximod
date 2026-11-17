import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const root = 'D:/alianximod/docs/开发记录/火属性二级设计评审-20261028';
const inputPath = process.argv.includes('--preview-candidate') ? `${root}/candidate-preserved.xlsx` : 'D:/alianximod/docs/术式制作表.xlsx';
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
if (process.argv.includes('--preview-candidate')) {
  for (const [name,range] of [['final-values','A1:M5'],['final-design','N1:O5'],['final-notes','DV1:DV5']]) {
    const preview = await workbook.render({sheetName:'待制作',range,scale:1.25,format:'png'});
    await fs.writeFile(`${root}/${name}.png`,new Uint8Array(await preview.arrayBuffer()));
  }
  console.log('Preserved candidate rendered without exporting.');
} else if (!process.argv.includes('--apply')) {
  console.log((await workbook.inspect({kind:'sheet', include:'id,name', maxChars:2000})).ndjson);
  const preview = await workbook.render({sheetName:'待制作',range:'A1:O5',scale:1,format:'png'});
  await fs.writeFile(`${root}/before.png`,new Uint8Array(await preview.arrayBuffer()));
  console.log('Read-only preview saved.');
} else {
  const spec = JSON.parse(await fs.readFile(`${root}/fire-techniques.json`, 'utf8'));
  const input = await fs.readFile(spec.source);
  const sha = crypto.createHash('sha256').update(input).digest('hex').toUpperCase();
  if (sha !== spec.baselineSha256) throw new Error('Source changed; do not overwrite.');
  const sheet = workbook.worksheets.getItem(spec.sheet);
  const sourceStyle = workbook.worksheets.getItem('已完成').getRange('A3:DV3');
  function index(col) { return [...col].reduce((a,c) => a*26+c.charCodeAt(0)-64,0)-1; }
  for (const entry of spec.rows) {
    const range = sheet.getRange(`A${entry.row}:DV${entry.row}`);
    range.copyFrom(sourceStyle,'all');
    const values = Array(126).fill(null);
    for (const [col,value] of Object.entries(entry.values)) values[index(col)] = value;
    range.values = [values];
    range.format.rowHeight = 225;
    range.format.verticalAlignment = 'top';
  }
  for (const [col,width] of Object.entries(spec.newColumnWidths)) {
    sheet.getRange(`${col}1:${col}5`).format.columnWidth = width;
    sheet.getRange(`${col}3:${col}5`).format.wrapText = true;
  }
  workbook.recalculate();
  for (const entry of spec.rows) {
    const actual = sheet.getRange(`A${entry.row}:DV${entry.row}`).values[0];
    for (const [col,expected] of Object.entries(entry.values)) {
      if (actual[index(col)] !== expected) throw new Error(`Write mismatch ${col}${entry.row}`);
    }
  }
  console.log((await workbook.inspect({kind:'table',range:'待制作!A3:M5',tableMaxRows:3,tableMaxCols:13,maxChars:3500})).ndjson);
  console.log((await workbook.inspect({kind:'match',range:'待制作!A3:DV5',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!',options:{useRegex:true,maxResults:20},maxChars:1000})).ndjson);
  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(`${root}/candidate-artifact.xlsx`);
  for (const [name,range] of [['after-values','A1:M5'],['after-design','N1:O5'],['after-notes','DV1:DV5']]) {
    const preview = await workbook.render({sheetName:spec.sheet,range,scale:1.25,format:'png'});
    await fs.writeFile(`${root}/${name}.png`,new Uint8Array(await preview.arrayBuffer()));
  }
  console.log('Candidate authored and rendered; original workbook not replaced.');
}
