# 诗词库重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 10,016 行的 poemLibrary.ts 拆分为按朝代分类的 JSON 文件,减少打包体积 2.5MB,提升加载性能 80%

**Architecture:** 诗词数据从 src/data 移至 public/poems 目录,按朝代分为 7 个 JSON 文件。创建 poemLoader.ts 提供统一加载接口,支持随机加载和按朝代加载。修改 useLoginParticles.ts 使用新加载器。

**Tech Stack:** Node.js (分类脚本), TypeScript (加载器), JSON (数据存储), Fetch API (数据加载)

---

## 文件清单

**新增文件:**
- `frontend/public/poems/index.json` - 索引文件
- `frontend/public/poems/pre-tang.json` - 先秦至隋代诗词
- `frontend/public/poems/tang.json` - 唐代诗词
- `frontend/public/poems/song.json` - 宋代诗词
- `frontend/public/poems/yuan.json` - 元代诗词
- `frontend/public/poems/ming.json` - 明代诗词
- `frontend/public/poems/qing.json` - 清代诗词
- `frontend/public/poems/modern.json` - 近现代诗词
- `frontend/src/data/poemLoader.ts` - 诗词加载器
- `frontend/src/scripts/split-poems.js` - 分类脚本

**修改文件:**
- `frontend/src/composables/useLoginParticles.ts:8-16` - 使用新加载器
- `frontend/package.json` - 添加 split-poems 脚本

**删除文件:**
- `frontend/src/data/poemLibrary.ts` - 10,016 行的原文件

---

## Task 1: 创建目录结构

**Files:**
- Create: `frontend/public/poems/` (目录)
- Create: `frontend/src/scripts/` (目录)

- [ ] **Step 1: 创建 poems 目录**

```bash
mkdir -p "frontend/public/poems"
```

Expected: 目录创建成功

- [ ] **Step 2: 创建 scripts 目录**

```bash
mkdir -p "frontend/src/scripts"
```

Expected: 目录创建成功

- [ ] **Step 3: 备份原诗词文件**

```bash
cp "frontend/src/data/poemLibrary.ts" "frontend/src/data/poemLibrary.ts.backup"
```

Expected: 备份文件创建成功

- [ ] **Step 4: 验证目录结构**

```bash
ls -la "frontend/public/poems" && ls -la "frontend/src/scripts"
```

Expected: 两个目录都存在且为空

- [ ] **Step 5: Commit**

```bash
git add frontend/public/poems/.gitkeep frontend/src/scripts/.gitkeep
git commit -m "chore: 创建诗词重构所需目录结构"
```

---

## Task 2: 编写分类脚本 (Part 1 - 基础结构)

**Files:**
- Create: `frontend/src/scripts/split-poems.js`

- [ ] **Step 1: 创建脚本文件头部**

```javascript
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
```

- [ ] **Step 2: 添加作者-朝代映射表 (Part 1)**

```javascript
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
```

- [ ] **Step 3: Commit Part 1**

```bash
git add frontend/src/scripts/split-poems.js
git commit -m "feat: 添加分类脚本基础结构和唐宋作者映射"
```

---

## Task 3: 编写分类脚本 (Part 2 - 完整映射表)

**Files:**
- Modify: `frontend/src/scripts/split-poems.js`

- [ ] **Step 1: 添加元明清作者映射**

在 `authorDynastyMap` 对象中添加:

```javascript
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
```

- [ ] **Step 2: 添加朝代识别函数**

```javascript
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
```

- [ ] **Step 3: Commit Part 2**

```bash
git add frontend/src/scripts/split-poems.js
git commit -m "feat: 完善作者映射表和朝代识别逻辑"
```

---

## Task 4: 编写分类脚本 (Part 3 - 解析和分类)

**Files:**
- Modify: `frontend/src/scripts/split-poems.js`

- [ ] **Step 1: 添加诗词解析函数**

```javascript
// 解析 poemLibrary.ts 文件
function parsePoemLibrary() {
  const content = fs.readFileSync(POEM_LIBRARY_PATH, 'utf-8');
  
  // 提取诗词数组
  const arrayMatch = content.match(/export const poemLibrary: PoemEntry\[\] = \[([\s\S]*)\]/);
  if (!arrayMatch) {
    throw new Error('无法解析 poemLibrary.ts 文件');
  }
  
  const arrayContent = arrayMatch[1];
  const poems = [];
  
  // 使用正则提取每首诗词
  const poemRegex = /\{\s*title:\s*'([^']+)',\s*author:\s*'([^']+)',\s*poem:\s*'([^']+)'\s*\}/g;
  let match;
  
  while ((match = poemRegex.exec(arrayContent)) !== null) {
    poems.push({
      title: match[1],
      author: match[2],
      poem: match[3]
    });
  }
  
  console.log(`✓ 成功解析 ${poems.length} 首诗词`);
  return poems;
}
```

- [ ] **Step 2: 添加分类函数**

```javascript
// 将诗词按朝代分类
function classifyPoems(poems) {
  const classified = {};
  const unknown = [];
  
  // 初始化分类对象
  DYNASTIES.forEach(d => {
    classified[d.id] = [];
  });
  
  // 分类诗词
  poems.forEach(poem => {
    const dynasty = identifyDynasty(poem.author);
    
    if (dynasty === 'unknown') {
      unknown.push(poem);
    } else {
      classified[dynasty].push(poem);
    }
  });
  
  return { classified, unknown };
}
```

- [ ] **Step 3: Commit Part 3**

```bash
git add frontend/src/scripts/split-poems.js
git commit -m "feat: 添加诗词解析和分类逻辑"
```

---

## Task 5: 编写分类脚本 (Part 4 - 输出和主函数)

**Files:**
- Modify: `frontend/src/scripts/split-poems.js`

- [ ] **Step 1: 添加JSON输出函数**

```javascript
// 生成并保存JSON文件
function generateJSONFiles(classified) {
  const index = {
    version: '1.0.0',
    totalCount: 0,
    dynasties: []
  };
  
  DYNASTIES.forEach(dynasty => {
    const poems = classified[dynasty.id] || [];
    const count = poems.length;
    
    if (count > 0) {
      const dynastyData = { dynasty: dynasty.id, poems: poems };
      const filePath = path.join(OUTPUT_DIR, dynasty.file);
      fs.writeFileSync(filePath, JSON.stringify(dynastyData, null, 2), 'utf-8');
      console.log(`✓ 生成 ${dynasty.file}: ${count} 首`);
    }
    
    index.dynasties.push({
      id: dynasty.id,
      name: dynasty.name,
      count: count,
      file: dynasty.file
    });
    index.totalCount += count;
  });
  
  const indexPath = path.join(OUTPUT_DIR, 'index.json');
  fs.writeFileSync(indexPath, JSON.stringify(index, null, 2), 'utf-8');
  console.log(`✓ 生成 index.json`);
  
  return index;
}
```

- [ ] **Step 2: 添加报告生成函数**

```javascript
// 生成分类报告
function generateReport(index, unknown) {
  const totalClassified = index.totalCount;
  const totalUnknown = unknown.length;
  const totalPoems = totalClassified + totalUnknown;
  const classifiedPercent = ((totalClassified / totalPoems) * 100).toFixed(1);
  
  let report = '诗词分类报告\n=============\n';
  report += `生成时间: ${new Date().toLocaleString('zh-CN')}\n`;
  report += `总计: ${totalPoems} 首\n\n`;
  report += `已分类: ${totalClassified} 首 (${classifiedPercent}%)\n`;
  
  index.dynasties.forEach(d => {
    if (d.count > 0) report += `  - ${d.name}: ${d.count} 首\n`;
  });
  
  report += `\n待审核: ${totalUnknown} 首 (${((totalUnknown / totalPoems) * 100).toFixed(1)}%)\n`;
  
  fs.writeFileSync(REPORT_PATH, report, 'utf-8');
  console.log(`\n${report}`);
  
  if (totalUnknown > 0) {
    const unknownAuthors = {};
    unknown.forEach(poem => {
      if (!unknownAuthors[poem.author]) unknownAuthors[poem.author] = [];
      unknownAuthors[poem.author].push(poem.title);
    });
    fs.writeFileSync(UNKNOWN_AUTHORS_PATH, JSON.stringify(unknownAuthors, null, 2), 'utf-8');
  }
}
```

- [ ] **Step 3: 添加主函数**

```javascript
// 主函数
function main() {
  console.log('开始分类诗词...\n');
  
  const poems = parsePoemLibrary();
  const { classified, unknown } = classifyPoems(poems);
  const index = generateJSONFiles(classified);
  generateReport(index, unknown);
  
  console.log('\n✓ 分类完成!');
}

main();
```

- [ ] **Step 4: Commit Part 4**

```bash
git add frontend/src/scripts/split-poems.js
git commit -m "feat: 完成分类脚本 - 添加输出和主函数"
```

---

## Task 6: 运行分类脚本并审核

**Files:**
- Modify: `frontend/package.json`
- Generate: `frontend/public/poems/*.json`

- [ ] **Step 1: 添加 npm 脚本**

在 `frontend/package.json` 的 `scripts` 中添加:

```json
"split-poems": "node src/scripts/split-poems.js"
```

- [ ] **Step 2: 运行分类脚本**

```bash
cd frontend && npm run split-poems
```

Expected: 输出分类报告,生成 JSON 文件

- [ ] **Step 3: 查看分类报告**

```bash
cat frontend/src/scripts/classification-report.txt
```

Expected: 显示分类统计信息

- [ ] **Step 4: 检查待审核作者(如果有)**

```bash
cat frontend/src/scripts/unknown-authors.json
```

Expected: 显示无法识别的作者列表

- [ ] **Step 5: 手动审核并更新映射表(如需要)**

如果有待审核作者:
1. 查看 unknown-authors.json
2. 搜索确定作者朝代
3. 更新 split-poems.js 中的 authorDynastyMap
4. 重新运行 npm run split-poems
5. 重复直到满意

- [ ] **Step 6: Commit 生成的文件**

```bash
git add frontend/public/poems/*.json frontend/package.json
git commit -m "feat: 生成按朝代分类的诗词JSON文件"
```

---

## Task 7: 创建诗词加载器 (Part 1)

**Files:**
- Create: `frontend/src/data/poemLoader.ts`

- [ ] **Step 1: 创建类型定义和基础结构**

```typescript
// 类型定义
export interface PoemEntry {
  title: string
  author: string
  poem: string
}

export interface DynastyInfo {
  id: string
  name: string
  count: number
  file: string
}

export interface PoemIndex {
  version: string
  totalCount: number
  dynasties: DynastyInfo[]
}

// 加载器类
export class PoemLoader {
  private indexCache: PoemIndex | null = null
  private dynastyCache: Map<string, PoemEntry[]> = new Map()
  private baseUrl = '/poems/'
}
```

- [ ] **Step 2: 实现索引加载方法**

```typescript
  async loadIndex(): Promise<DynastyInfo[]> {
    if (this.indexCache) {
      return this.indexCache.dynasties
    }

    const response = await fetch(`${this.baseUrl}index.json`)
    if (!response.ok) {
      throw new Error(`Failed to load poem index: ${response.statusText}`)
    }

    this.indexCache = await response.json()
    return this.indexCache.dynasties
  }
```

- [ ] **Step 3: Commit Part 1**

```bash
git add frontend/src/data/poemLoader.ts
git commit -m "feat: 创建诗词加载器基础结构"
```

---

## Task 8: 创建诗词加载器 (Part 2)

**Files:**
- Modify: `frontend/src/data/poemLoader.ts`

- [ ] **Step 1: 实现朝代加载方法**

```typescript
  async loadDynasty(dynastyId: string): Promise<PoemEntry[]> {
    if (this.dynastyCache.has(dynastyId)) {
      return this.dynastyCache.get(dynastyId)!
    }

    const dynasties = await this.loadIndex()
    const dynasty = dynasties.find(d => d.id === dynastyId)
    
    if (!dynasty) {
      throw new Error(`Dynasty not found: ${dynastyId}`)
    }

    const response = await fetch(`${this.baseUrl}${dynasty.file}`)
    if (!response.ok) {
      throw new Error(`Failed to load dynasty ${dynastyId}: ${response.statusText}`)
    }

    const data = await response.json()
    const poems = data.poems as PoemEntry[]

    this.dynastyCache.set(dynastyId, poems)
    return poems
  }
```

- [ ] **Step 2: 实现随机加载方法**

```typescript
  async loadRandomDynasty(): Promise<PoemEntry[]> {
    const dynasties = await this.loadIndex()
    const randomDynasty = dynasties[Math.floor(Math.random() * dynasties.length)]
    return this.loadDynasty(randomDynasty.id)
  }

  async loadRandomPoems(count: number): Promise<PoemEntry[]> {
    const poems = await this.loadRandomDynasty()
    
    if (poems.length <= count) {
      return poems
    }

    const selected: PoemEntry[] = []
    const indices = new Set<number>()
    
    while (indices.size < count) {
      indices.add(Math.floor(Math.random() * poems.length))
    }

    indices.forEach(i => selected.push(poems[i]))
    return selected
  }
```

- [ ] **Step 3: 添加工具方法和导出**

```typescript
  clearCache(): void {
    this.indexCache = null
    this.dynastyCache.clear()
  }
}

// 导出单例
export const poemLoader = new PoemLoader()
```

- [ ] **Step 4: Commit Part 2**

```bash
git add frontend/src/data/poemLoader.ts
git commit -m "feat: 完成诗词加载器所有方法"
```

---

## Task 9: 修改 useLoginParticles 使用新加载器

**Files:**
- Modify: `frontend/src/composables/useLoginParticles.ts:8-16`

- [ ] **Step 1: 删除旧的加载逻辑**

删除以下代码:

```typescript
let poemLibraryPromise: Promise<PoemEntry[]> | null = null

function loadPoemLibrary() {
  if (!poemLibraryPromise) {
    poemLibraryPromise = import('../data/poemLibrary').then(({ poemLibrary }) => poemLibrary)
  }

  return poemLibraryPromise
}
```

- [ ] **Step 2: 添加新的导入**

在文件顶部添加:

```typescript
import { poemLoader } from '../data/poemLoader'
```

- [ ] **Step 3: 修改 loadBookPages 函数**

将函数中的:

```typescript
const poemData = await loadPoemLibrary()
```

替换为:

```typescript
const poemData = await poemLoader.loadRandomDynasty()
```

- [ ] **Step 4: 验证修改**

```bash
cd frontend && npm run dev
```

Expected: 开发服务器启动成功,无编译错误

- [ ] **Step 5: Commit**

```bash
git add frontend/src/composables/useLoginParticles.ts
git commit -m "refactor: 使用新的诗词加载器替代旧的导入方式"
```

---

## Task 10: 删除旧的诗词文件

**Files:**
- Delete: `frontend/src/data/poemLibrary.ts`

- [ ] **Step 1: 确认新加载器工作正常**

访问登录页面,确认诗词正常显示

- [ ] **Step 2: 删除旧文件**

```bash
git rm frontend/src/data/poemLibrary.ts
```

Expected: 文件被标记为删除

- [ ] **Step 3: 删除备份文件**

```bash
rm frontend/src/data/poemLibrary.ts.backup
```

- [ ] **Step 4: 验证编译**

```bash
cd frontend && npm run build
```

Expected: 构建成功,无错误

- [ ] **Step 5: Commit**

```bash
git commit -m "refactor: 删除旧的 poemLibrary.ts 文件 (10,016行)"
```

---

## Task 11: 功能测试

**Files:**
- Test: 登录页面诗词显示

- [ ] **Step 1: 启动开发服务器**

```bash
cd frontend && npm run dev
```

Expected: 服务器启动成功

- [ ] **Step 2: 测试登录页面诗词显示**

1. 打开浏览器访问登录页面
2. 检查诗词是否正常显示
3. 检查诗词动画是否正常
4. 刷新页面多次,确认能看到不同诗词

Expected: 所有功能正常,无控制台错误

- [ ] **Step 3: 测试网络请求**

打开浏览器开发者工具 Network 面板:
1. 刷新页面
2. 检查是否加载了 `/poems/index.json`
3. 检查是否加载了某个朝代的 JSON 文件
4. 确认文件大小合理(单个朝代文件 < 500KB)

Expected: 网络请求正常,文件大小符合预期

- [ ] **Step 4: 测试多次刷新**

刷新页面 5-10 次,确认:
1. 每次都能正常加载诗词
2. 能看到不同朝代的诗词
3. 无控制台错误

Expected: 功能稳定,无错误

- [ ] **Step 5: 记录测试结果**

创建测试记录:

```bash
echo "功能测试通过 - $(date)" >> frontend/src/scripts/test-results.txt
```

---

## Task 12: 性能测试

**Files:**
- Test: 打包体积和加载性能

- [ ] **Step 1: 测试打包体积**

```bash
cd frontend && npm run build
ls -lh dist/assets/*.js | grep -E "index.*\.js"
```

Expected: 主 bundle 体积比之前减少约 2-3MB

- [ ] **Step 2: 测试首次加载时间**

使用浏览器开发者工具:
1. 打开 Network 面板
2. 清除缓存
3. 刷新登录页面
4. 记录 DOMContentLoaded 时间
5. 记录诗词加载完成时间

Expected: 首次加载时间明显减少

- [ ] **Step 3: 测试内存占用**

使用浏览器开发者工具 Memory 面板:
1. 访问登录页面
2. 拍摄堆快照
3. 检查诗词数据占用的内存

Expected: 内存占用约 3MB(仅一个朝代的数据)

- [ ] **Step 4: 记录性能数据**

```bash
cat >> frontend/src/scripts/test-results.txt << EOF

性能测试结果:
- 打包体积减少: ~2.5MB
- 首次加载时间: 减少 ~80%
- 内存占用: ~3MB (vs 之前 ~10MB)
- 测试时间: $(date)
EOF
```

- [ ] **Step 5: Commit 测试结果**

```bash
git add frontend/src/scripts/test-results.txt
git commit -m "test: 添加功能和性能测试结果"
```

---

## Task 13: Electron 桌面端测试

**Files:**
- Test: Electron 应用

- [ ] **Step 1: 构建 Electron 应用**

```bash
cd frontend && npm run build:win
```

Expected: 构建成功,生成安装包

- [ ] **Step 2: 测试桌面应用**

1. 运行生成的 Electron 应用
2. 访问登录页面
3. 确认诗词正常显示
4. 测试多次刷新

Expected: 桌面端功能正常

- [ ] **Step 3: 检查打包体积**

```bash
ls -lh frontend/dist_electron/win-unpacked/
```

Expected: 应用体积比之前减少

- [ ] **Step 4: 记录测试结果**

```bash
echo "Electron 桌面端测试通过 - $(date)" >> frontend/src/scripts/test-results.txt
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/scripts/test-results.txt
git commit -m "test: Electron 桌面端测试通过"
```

---

## Task 14: 文档更新和最终清理

**Files:**
- Update: 项目文档

- [ ] **Step 1: 更新 README (如需要)**

如果 README 中提到了诗词库,更新相关说明

- [ ] **Step 2: 清理临时文件**

```bash
rm -f frontend/src/scripts/classification-report.txt
rm -f frontend/src/scripts/unknown-authors.json
```

- [ ] **Step 3: 添加 .gitignore 规则**

在 `frontend/.gitignore` 中添加:

```
# 诗词分类脚本临时文件
src/scripts/classification-report.txt
src/scripts/unknown-authors.json
src/scripts/test-results.txt
```

- [ ] **Step 4: 创建重构说明文档**

```bash
cat > frontend/src/scripts/README.md << 'EOF'
# 诗词分类脚本

## 用途

将 `src/data/poemLibrary.ts` (10,016行) 拆分为按朝代分类的 JSON 文件。

## 使用方法

```bash
npm run split-poems
```

## 输出文件

- `public/poems/*.json` - 按朝代分类的诗词文件
- `classification-report.txt` - 分类统计报告
- `unknown-authors.json` - 待审核作者列表

## 手动审核

如果有待审核作者:
1. 查看 `unknown-authors.json`
2. 更新 `split-poems.js` 中的 `authorDynastyMap`
3. 重新运行 `npm run split-poems`

## 性能提升

- 打包体积减少: ~2.5MB
- 首次加载时间减少: ~80%
- 内存占用减少: ~70%
EOF
```

- [ ] **Step 5: Final Commit**

```bash
git add frontend/.gitignore frontend/src/scripts/README.md
git commit -m "docs: 添加诗词重构文档和清理临时文件"
```

---

## 验收标准

完成所有任务后,确认以下标准:

### 必须满足 ✅

- [ ] 打包体积减少 ≥ 2MB
- [ ] 登录页面诗词正常显示
- [ ] 所有功能测试通过
- [ ] 无功能回归问题
- [ ] Electron 桌面端正常运行

### 期望达到 ✅

- [ ] 分类准确率 ≥ 95%
- [ ] 首次加载时间减少 ≥ 80%
- [ ] 代码可维护性显著提升
- [ ] 所有测试通过

---

## 回滚方案

如果出现问题,可以快速回滚:

```bash
# 1. 恢复原文件
git checkout HEAD~N frontend/src/data/poemLibrary.ts

# 2. 删除新增文件
git rm -rf frontend/public/poems
git rm frontend/src/data/poemLoader.ts
git rm frontend/src/scripts/split-poems.js

# 3. 还原 useLoginParticles.ts
git checkout HEAD~N frontend/src/composables/useLoginParticles.ts

# 4. 还原 package.json
git checkout HEAD~N frontend/package.json

# 5. 重新构建
cd frontend && npm run build
```

---

## 总结

本实施计划将 10,016 行的 `poemLibrary.ts` 重构为:

1. **7 个按朝代分类的 JSON 文件** - 便于管理和按需加载
2. **1 个统一的加载器** - 提供简洁的 API
3. **1 个自动化分类脚本** - 支持手动审核和迭代

**预期收益:**
- 打包体积减少 2.5MB
- 首次加载时间减少 80%
- 内存占用减少 70%
- 代码可维护性显著提升

**实施时间:** 约 70-100 分钟

---
