#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const TANG_JSON = path.join(__dirname, '../../public/poems/tang.json');
const OUTPUT_DIR = path.join(__dirname, '../../public/poems/tang');

console.log('开始处理唐代诗词按作者分类...\n');

// 读取唐代诗词
const tangData = JSON.parse(fs.readFileSync(TANG_JSON, 'utf-8'));
const poems = tangData.poems;

console.log(`总诗词数: ${poems.length}首`);

// 按作者分组
const authorGroups = {};
poems.forEach(poem => {
  if (!authorGroups[poem.author]) {
    authorGroups[poem.author] = [];
  }
  authorGroups[poem.author].push(poem);
});

console.log(`总作者数: ${Object.keys(authorGroups).length}位\n`);

// 创建输出目录
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// 分类统计
const majorAuthors = []; // >=100首
const minorAuthors = []; // 50-99首
const otherPoems = []; // <50首

Object.entries(authorGroups).forEach(([author, poems]) => {
  if (poems.length >= 100) {
    majorAuthors.push({ author, poems, count: poems.length });
  } else if (poems.length >= 50) {
    minorAuthors.push({ author, poems, count: poems.length });
  } else {
    otherPoems.push(...poems);
  }
});

// 按作品数量排序
majorAuthors.sort((a, b) => b.count - a.count);
minorAuthors.sort((a, b) => b.count - a.count);

console.log('=== 分类统计 ===');
console.log(`主要诗人 (>=100首): ${majorAuthors.length}位`);
console.log(`次要诗人 (50-99首): ${minorAuthors.length}位`);
console.log(`其他诗人 (<50首): ${Object.keys(authorGroups).length - majorAuthors.length - minorAuthors.length}位\n`);

// 保存主要诗人(每人一个文件)
console.log('=== 保存主要诗人 ===');
majorAuthors.forEach(({ author, poems, count }) => {
  // 生成安全的文件名
  const filename = author
    .replace(/[<>:"/\\|?*]/g, '_')
    .replace(/\s+/g, '_') + '.json';

  const data = {
    author,
    count: poems.length,
    poems
  };

  fs.writeFileSync(
    path.join(OUTPUT_DIR, filename),
    JSON.stringify(data, null, 2),
    'utf-8'
  );
  console.log(`✓ ${filename}: ${count}首`);
});

// 保存次要诗人(合并文件)
if (minorAuthors.length > 0) {
  console.log('\n=== 保存次要诗人 ===');
  const minorData = {
    description: '次要诗人作品集(50-99首)',
    totalCount: minorAuthors.reduce((sum, a) => sum + a.count, 0),
    authors: minorAuthors.map(a => ({ author: a.author, count: a.count })),
    poems: minorAuthors.flatMap(a => a.poems)
  };

  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'minor-authors.json'),
    JSON.stringify(minorData, null, 2),
    'utf-8'
  );
  console.log(`✓ minor-authors.json: ${minorData.totalCount}首 (${minorAuthors.length}位作者)`);
}

// 保存其他诗人
if (otherPoems.length > 0) {
  console.log('\n=== 保存其他诗人 ===');
  const otherData = {
    description: '其他诗人作品集(<50首)',
    count: otherPoems.length,
    poems: otherPoems
  };

  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'others.json'),
    JSON.stringify(otherData, null, 2),
    'utf-8'
  );
  console.log(`✓ others.json: ${otherPoems.length}首`);
}

// 生成索引
console.log('\n=== 生成索引文件 ===');
const index = {
  version: '1.0.0',
  dynasty: 'tang',
  totalCount: poems.length,
  totalAuthors: Object.keys(authorGroups).length,
  majorAuthors: majorAuthors.map(a => ({
    author: a.author,
    count: a.count,
    file: a.author
      .replace(/[<>:"/\\|?*]/g, '_')
      .replace(/\s+/g, '_') + '.json'
  })),
  minorAuthors: {
    file: 'minor-authors.json',
    count: minorAuthors.reduce((sum, a) => sum + a.count, 0),
    authorCount: minorAuthors.length,
    authors: minorAuthors.map(a => ({ author: a.author, count: a.count }))
  },
  others: {
    file: 'others.json',
    count: otherPoems.length,
    authorCount: Object.keys(authorGroups).length - majorAuthors.length - minorAuthors.length
  }
};

fs.writeFileSync(
  path.join(OUTPUT_DIR, 'index.json'),
  JSON.stringify(index, null, 2),
  'utf-8'
);
console.log('✓ index.json');

// 最终统计
console.log('\n=== 完成统计 ===');
console.log(`总诗词数: ${poems.length}首`);
console.log(`总作者数: ${Object.keys(authorGroups).length}位`);
console.log(`主要诗人: ${majorAuthors.length}位 (${majorAuthors.reduce((sum, a) => sum + a.count, 0)}首)`);
console.log(`次要诗人: ${minorAuthors.length}位 (${minorAuthors.reduce((sum, a) => sum + a.count, 0)}首)`);
console.log(`其他诗人: ${index.others.authorCount}位 (${otherPoems.length}首)`);
console.log('\n✓ 分类完成!');
