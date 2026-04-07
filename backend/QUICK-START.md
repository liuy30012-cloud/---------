# 🚀 快速启动指南（手动方式）

## 当前状态

✅ 环境配置已完成  
✅ .env 文件已创建并配置  
⚠️ Maven 未在命令行PATH中（需要手动启动）

---

## 📝 启动步骤

### 方式1：使用IDE启动（最简单，推荐）

#### 启动后端

1. **打开IDE**（IntelliJ IDEA / Eclipse / VS Code）

2. **导入后端项目**
   - IntelliJ IDEA: File → Open → 选择 `backend` 文件夹
   - VS Code: File → Open Folder → 选择 `backend` 文件夹

3. **找到主类**
   - 文件路径：`backend/src/main/java/com/library/LibraryApplication.java`

4. **运行主类**
   - IntelliJ IDEA: 右键 `LibraryApplication.java` → Run 'LibraryApplication'
   - VS Code: 点击 Run 按钮或按 F5

5. **等待启动**
   - 看到 "Started LibraryApplication" 表示启动成功
   - 后端地址：http://localhost:8080

#### 启动前端

1. **打开新的终端**（PowerShell 或 CMD）

2. **进入前端目录**
   ```bash
   cd "E:\图书馆书籍定位系统\frontend"
   ```

3. **安装依赖**（首次运行）
   ```bash
   npm install
   ```

4. **启动开发服务器**
   ```bash
   npm run dev
   ```

5. **访问系统**
   - 前端地址：http://localhost:5173
   - 在浏览器中打开即可

---

### 方式2：使用命令行启动

#### 启动后端（需要Maven）

**如果已安装Maven：**

1. 打开 **PowerShell** 或 **CMD**（不是Git Bash）

2. 进入后端目录
   ```powershell
   cd "E:\图书馆书籍定位系统\backend"
   ```

3. 启动服务
   ```powershell
   mvn spring-boot:run
   ```

**如果Maven未安装：**

请使用方式1（IDE启动）

#### 启动前端

1. 打开新的 **PowerShell** 或 **CMD**

2. 进入前端目录
   ```powershell
   cd "E:\图书馆书籍定位系统\frontend"
   ```

3. 安装依赖（首次）
   ```powershell
   npm install
   ```

4. 启动服务
   ```powershell
   npm run dev
   ```

---

## 🌐 访问系统

启动成功后：

1. **打开浏览器**
2. **访问：** http://localhost:5173
3. **注册账号** 或 **登录**

---

## 🔧 常见问题

### 问题1：后端启动失败 - 数据库连接错误

**错误信息：**
```
Communications link failure
```

**解决方法：**

1. **启动MySQL服务**
   - Windows: 按 `Win + R` → 输入 `services.msc` → 找到 MySQL → 启动

2. **创建数据库**
   ```sql
   # 打开MySQL命令行或Workbench
   CREATE DATABASE IF NOT EXISTS library CHARACTER SET utf8mb4;
   ```

3. **检查密码**
   - 确认 `backend/.env` 中的 `DB_PASSWORD=root` 与你的MySQL密码一致

---

### 问题2：前端启动失败 - 端口被占用

**错误信息：**
```
Port 5173 is already in use
```

**解决方法：**

```powershell
# 查找占用端口的进程
netstat -ano | findstr :5173

# 结束进程（替换<PID>为实际进程ID）
taskkill /PID <PID> /F

# 或使用其他端口
npm run dev -- --port 3000
```

---

### 问题3：Maven未安装

**解决方法：**

**选项A：安装Maven**
1. 下载：https://maven.apache.org/download.cgi
2. 解压到 `C:\Program Files\Apache\maven`
3. 添加到PATH：`C:\Program Files\Apache\maven\bin`

**选项B：使用IDE**（推荐）
- IntelliJ IDEA 自带Maven
- 直接用IDE运行即可

---

## 📱 测试功能

系统启动后，你可以：

1. **注册新账号**
   - 学号：任意数字（如：2024001）
   - 姓名：你的名字
   - 密码：至少6位

2. **登录系统**

3. **浏览功能**
   - 📚 书籍搜索
   - 📖 书籍详情
   - 📋 借阅管理
   - 📊 数据分析
   - ⚠️ 库存预警
   - 💡 采购建议

---

## 🎯 推荐启动方式

### 最简单的方式：

1. **用IntelliJ IDEA打开backend文件夹**
2. **运行 LibraryApplication.java**
3. **打开PowerShell，运行：**
   ```powershell
   cd "E:\图书馆书籍定位系统\frontend"
   npm install
   npm run dev
   ```
4. **浏览器访问：** http://localhost:5173

---

## 📞 需要帮助？

如果遇到问题：
1. 检查 MySQL 服务是否运行
2. 检查端口 8080 和 5173 是否被占用
3. 查看控制台错误信息
4. 参考 [HOW-TO-START.md](./HOW-TO-START.md) 详细文档

---

## ✅ 启动成功标志

**后端启动成功：**
```
Started LibraryApplication in X.XXX seconds (JVM running for X.XXX)
```

**前端启动成功：**
```
VITE v5.x.x  ready in XXX ms
➜  Local:   http://localhost:5173/
```

---

祝你使用愉快！🎉
