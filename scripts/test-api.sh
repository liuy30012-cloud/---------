# 用户系统API测试脚本

## 测试环境
BASE_URL="http://localhost:8080"

echo "=========================================="
echo "用户系统API测试"
echo "=========================================="
echo ""

## 1. 测试用户注册
echo "1. 测试用户注册"
echo "POST /api/auth/register"
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021002",
    "username": "李四",
    "password": "Test123456!",
    "confirmPassword": "Test123456!",
    "email": "lisi@example.com",
    "phone": "13800138000"
  }'
echo -e "\n"

## 2. 测试用户登录
echo "2. 测试用户登录（使用预置账号）"
echo "POST /api/auth/login"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "Test123456",
    "rememberMe": false
  }')
echo "$LOGIN_RESPONSE"
echo -e "\n"

# 提取token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo "提取的Token: $TOKEN"
echo -e "\n"

## 3. 测试获取当前用户信息
echo "3. 测试获取当前用户信息"
echo "GET /api/auth/me"
curl -X GET "${BASE_URL}/api/auth/me" \
  -H "Authorization: Bearer ${TOKEN}"
echo -e "\n"

## 4. 测试登录失败（错误密码）
echo "4. 测试登录失败（错误密码）"
echo "POST /api/auth/login"
curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "wrongpassword",
    "rememberMe": false
  }'
echo -e "\n"

## 5. 测试注册验证（密码不一致）
echo "5. 测试注册验证（密码不一致）"
echo "POST /api/auth/register"
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021003",
    "username": "王五",
    "password": "Test123456!",
    "confirmPassword": "Wrong123456!",
    "email": "wangwu@example.com"
  }'
echo -e "\n"

## 6. 测试修改密码
echo "6. 测试修改密码"
echo "POST /api/auth/change-password"
curl -X POST "${BASE_URL}/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "oldPassword": "Test123456",
    "newPassword": "Admin123456!"
  }'
echo -e "\n"

## 7. 测试登出
echo "7. 测试登出"
echo "POST /api/auth/logout"
curl -X POST "${BASE_URL}/api/auth/logout" \
  -H "Authorization: Bearer ${TOKEN}"
echo -e "\n"

echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo "默认读者：2021001 / Test123456"
echo "默认管理员：admin001 / Admin123456!"
