@echo off
chcp 65001 >nul
echo ==========================================
echo 用户系统API测试
echo ==========================================
echo.

set BASE_URL=http://localhost:8080

echo 1. 测试用户注册
echo POST /api/auth/register
curl -X POST "%BASE_URL%/api/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021002\",\"username\":\"李四\",\"password\":\"Test123456!\",\"confirmPassword\":\"Test123456!\",\"email\":\"lisi@example.com\",\"phone\":\"13800138000\"}"
echo.
echo.

echo 2. 测试用户登录（使用预置账号）
echo POST /api/auth/login
curl -X POST "%BASE_URL%/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021001\",\"password\":\"Test123456\",\"rememberMe\":false}" > login_response.json
type login_response.json
echo.
echo.

echo 3. 测试获取当前用户信息
echo GET /api/auth/me
echo 请手动复制上面的token，然后运行：
echo curl -X GET "%BASE_URL%/api/auth/me" -H "Authorization: Bearer YOUR_TOKEN"
echo.

echo 4. 测试登录失败（错误密码）
echo POST /api/auth/login
curl -X POST "%BASE_URL%/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021001\",\"password\":\"wrongpassword\",\"rememberMe\":false}"
echo.
echo.

echo 5. 测试注册验证（密码不一致）
echo POST /api/auth/register
curl -X POST "%BASE_URL%/api/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021003\",\"username\":\"王五\",\"password\":\"Test123456!\",\"confirmPassword\":\"Wrong123456!\",\"email\":\"wangwu@example.com\"}"
echo.
echo.

echo 6. 测试重复学号注册
echo POST /api/auth/register
curl -X POST "%BASE_URL%/api/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021001\",\"username\":\"测试\",\"password\":\"Test123456!\",\"confirmPassword\":\"Test123456!\"}"
echo.
echo.

echo ==========================================
echo 测试完成！
echo ==========================================
echo.
echo 提示：
echo 1. 确保后端已启动在 http://localhost:8080
echo 2. 默认读者：学号 2021001，密码 Test123456
echo 3. 默认管理员：学号 admin001，密码 Admin123456!
echo 4. 查看 login_response.json 获取完整的登录响应
echo.
pause
