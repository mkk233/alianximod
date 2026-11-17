import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const root = 'D:/alianximod/docs/开发记录/制作表资源状态通用化-20261028';
const spec = JSON.parse(await fs.readFile(`${root}/migration-spec.json`, 'utf8'));
const source = 'D:/alianximod/docs/术式制作表.xlsx';
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(source));

function writeValue(sheet, address, value) {
  const range = sheet.getRange(address);
  range.values = [[value]];
}

for (const sheetName of spec.statusSheets) {
  const sheet = workbook.worksheets.getItem(sheetName);
  writeValue(sheet, 'DW1', spec.groupTitle);
  spec.newColumns.forEach((column, index) => {
    writeValue(sheet, `${column}2`, spec.newHeaders[index]);
    sheet.getRange(`${column}1`).format.columnWidth = spec.columnWidths[column];
    sheet.getRange(`${column}1`).format.wrapText = true;
  });
  for (const column of spec.prefixColumns) {
    const current = sheet.getRange(`${column}2`).values[0][0];
    if (typeof current !== 'string' || current.startsWith('旧档·')) {
      throw new Error(`Unexpected header ${sheetName}!${column}2=${current}`);
    }
    writeValue(sheet, `${column}2`, `旧档·${current}`);
  }
  for (const entry of spec.validations) {
    try {
      sheet.getRange(`${entry.col}3:${entry.col}1000`).dataValidation = {
        rule: { type: 'list', formula1: entry.formula },
      };
    } catch (error) {
      console.log(`validation-skip ${sheetName} ${entry.col} ${error.message}`);
    }
  }
}

for (const entry of spec.mainRows) {
  const sheet = workbook.worksheets.getItem(entry.sheet);
  const actualId = sheet.getRange(`B${entry.row}`).values[0][0];
  if (actualId !== entry.id) throw new Error(`Row mismatch ${entry.sheet} ${entry.row} ${actualId}`);
  for (const [column, value] of Object.entries(entry.values)) {
    writeValue(sheet, `${column}${entry.row}`, value);
  }
}

const detail = workbook.worksheets.add('资源状态明细');
writeValue(detail, 'A1', '资源状态明细');
spec.detailColumns.forEach((column, index) => {
  writeValue(detail, `${column}2`, spec.detailHeaders[index]);
  detail.getRange(`${column}1`).format.columnWidth = spec.detailWidths[column];
  detail.getRange(`${column}1`).format.wrapText = true;
});
for (const entry of spec.detailRows) {
  for (const [column, value] of Object.entries(entry.values)) {
    writeValue(detail, `${column}${entry.row}`, value);
  }
}
for (const entry of spec.detailValidations) {
  try {
    detail.getRange(`${entry.col}3:${entry.col}1000`).dataValidation = {
      rule: { type: 'list', formula1: entry.formula },
    };
  } catch (error) {
    console.log(`validation-skip detail ${entry.col} ${error.message}`);
  }
}

const catalog = workbook.worksheets.getItem('数据表');
writeValue(catalog, 'AW1', '资源与状态字典');
writeValue(catalog, 'AW2', '本体字典');
spec.dictionaryColumns.forEach((column, index) => {
  writeValue(catalog, `${column}3`, spec.dictionaryHeaders[index]);
  catalog.getRange(`${column}1`).format.columnWidth = spec.dictionaryWidths[column];
});
spec.dictionaryRows.forEach((row, rowIndex) => {
  row.forEach((value, columnIndex) => {
    if (value === null || value === undefined) return;
    writeValue(catalog, `${spec.dictionaryColumns[columnIndex]}${rowIndex + 4}`, value);
  });
});
for (const list of spec.lists) {
  writeValue(catalog, `${list.col}2`, list.title);
  writeValue(catalog, `${list.col}3`, '名称');
  catalog.getRange(`${list.col}1`).format.columnWidth = list.width;
  list.values.forEach((value, index) => writeValue(catalog, `${list.col}${index + 4}`, value));
}

workbook.recalculate();
const checks = [
  ['已完成!DW1:EN2', 2, 18],
  ['已完成!DW13:EN13', 1, 18],
  ['已完成!DW26:EN28', 3, 18],
  ['待制作!DW3:EN5', 3, 18],
  ['数据表!AW1:BH9', 9, 12],
  ['数据表!BJ2:BV11', 10, 12],
  ['资源状态明细!A1:T6', 6, 20],
];
for (const [range, tableMaxRows, tableMaxCols] of checks) {
  const inspected = await workbook.inspect({
    kind: 'table',
    range,
    include: 'values',
    tableMaxRows,
    tableMaxCols,
    maxChars: 5000,
  });
  console.log(inspected.ndjson);
}
const errors = await workbook.inspect({
  kind: 'match',
  searchTerm: '#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!',
  options: { useRegex: true, maxResults: 50 },
  summary: 'final formula error scan',
  maxChars: 2000,
});
console.log(errors.ndjson);
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(`${root}/candidate-artifact.xlsx`);
console.log('candidate-artifact exported');
