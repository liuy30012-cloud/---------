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
};
