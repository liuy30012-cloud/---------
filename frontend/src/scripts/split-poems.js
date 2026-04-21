#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

// 文件路径配置
const POEM_LIBRARY_PATH = path.join(__dirname, '../data/poemLibrary.ts');
const OUTPUT_DIR = path.join(__dirname, '../../public/poems');
const REPORT_PATH = path.join(__dirname, 'classification-report.txt');
const UNKNOWN_AUTHORS_PATH = path.join(__dirname, 'unknown-authors.json');

// 朝代配置
const DYNASTIES = [
  { id: 'pre-tang', name: '先秦至隋', file: 'pre-tang.json' },
  { id: 'tang', name: '唐代', file: 'tang.json' },
  { id: 'song', name: '宋代', file: 'song.json' },
  { id: 'yuan', name: '元代', file: 'yuan.json' },
  { id: 'ming', name: '明代', file: 'ming.json' },
  { id: 'qing', name: '清代', file: 'qing.json' },
  { id: 'modern', name: '近现代', file: 'modern.json' }
];

// 作者-朝代精确映射表
const authorDynastyMap = {
  // 唐代 (618-907)
  '李白': 'tang', '杜甫': 'tang', '白居易': 'tang', '王维': 'tang',
  '孟浩然': 'tang', '李商隐': 'tang', '杜牧': 'tang', '韩愈': 'tang',
  '柳宗元': 'tang', '刘禹锡': 'tang', '元稹': 'tang', '贾岛': 'tang',
  '温庭筠': 'tang', '李贺': 'tang', '韦应物': 'tang', '岑参': 'tang',
  '高适': 'tang', '王昌龄': 'tang', '王之涣': 'tang', '崔颢': 'tang',
  '太宗皇帝': 'tang', '玄宗皇帝': 'tang', '武则天': 'tang',

  // 宋代 (960-1279)
  '苏轼': 'song', '李清照': 'song', '辛弃疾': 'song', '陆游': 'song',
  '欧阳修': 'song', '王安石': 'song', '苏辙': 'song', '苏洵': 'song',
  '黄庭坚': 'song', '秦观': 'song', '周邦彦': 'song', '柳永': 'song',
  '范仲淹': 'song', '晏殊': 'song', '晏几道': 'song', '姜夔': 'song',
  '杨万里': 'song', '范成大': 'song', '文天祥': 'song', '岳飞': 'song',
  '宋徽宗': 'song', '宋太宗': 'song',

  // 元代 (1271-1368)
  '关汉卿': 'yuan', '马致远': 'yuan', '白朴': 'yuan', '郑光祖': 'yuan',
  '王实甫': 'yuan', '张养浩': 'yuan', '睢景臣': 'yuan',

  // 明代 (1368-1644)
  '唐寅': 'ming', '文征明': 'ming', '徐渭': 'ming', '杨慎': 'ming',
  '于谦': 'ming', '高启': 'ming', '刘基': 'ming', '宋濂': 'ming',

  // 清代 (1644-1912)
  '纳兰性德': 'qing', '曹雪芹': 'qing', '龚自珍': 'qing', '郑燮': 'qing',
  '袁枚': 'qing', '黄景仁': 'qing', '顾炎武': 'qing', '王士祯': 'qing',

  // 近现代 (1912-)
  '鲁迅': 'modern', '毛泽东': 'modern', '郭沫若': 'modern'
};

// 根据作者名识别朝代
function identifyDynasty(author) {
  // 策略1: 精确匹配
  if (authorDynastyMap[author]) {
    return authorDynastyMap[author];
  }

  // 策略2: 模糊匹配 - 皇帝类
  if (author.includes('太宗') || author.includes('高祖')) return 'tang';
  if (author.includes('宋') && author.includes('宗')) return 'song';
  if (author.includes('明') && author.includes('宗')) return 'ming';
  if (author.includes('清') && author.includes('帝')) return 'qing';

  // 策略3: 无法识别
  return 'unknown';
}
