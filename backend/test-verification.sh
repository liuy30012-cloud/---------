#!/bin/bash

# 测试环境验证脚本
# 用于验证bug修复效果

echo "=========================================="
echo "图书馆书籍定位系统 - 测试环境验证脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 函数：打印测试结果
print_result() {
    local test_name=$1
    local result=$2
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$result" -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $test_name"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗${NC} $test_name"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 检查环境
echo "1. 检查环境依赖..."
command -v java >/dev/null 2>&1
print_result "Java环境" $?

command -v mvn >/dev/null 2>&1
print_result "Maven环境" $?

command -v mysql >/dev/null 2>&1
print_result "MySQL环境" $?

echo ""

# 编译项目
echo "2. 编译项目..."
cd backend
mvn clean compile -DskipTests >/dev/null 2>&1
print_result "项目编译" $?

echo ""

# 运行单元测试
echo "3. 运行单元测试..."
mvn test -Dtest=BorrowServiceTest >/dev/null 2>&1
print_result "BorrowService单元测试" $?

mvn test -Dtest=UserServiceTest >/dev/null 2>&1
print_result "UserService单元测试" $?

echo ""

# 运行集成测试
echo "4. 运行集成测试..."
mvn test -Dtest=BorrowIntegrationTest >/dev/null 2>&1
print_result "借阅流程集成测试" $?

echo ""

# 检查ShedLock配置
echo "5. 检查ShedLock配置..."
if grep -q "shedlock" pom.xml; then
    print_result "ShedLock依赖配置" 0
else
    print_result "ShedLock依赖配置" 1
fi

if [ -f "src/main/java/com/library/config/ShedLockConfig.java" ]; then
    print_result "ShedLock配置类" 0
else
    print_result "ShedLock配置类" 1
fi

echo ""

# 数据库检查
echo "6. 检查数据库配置..."
if [ -f "src/main/resources/application.yml" ]; then
    print_result "应用配置文件" 0
else
    print_result "应用配置文件" 1
fi

if [ -f "src/main/resources/db/migration/V1__Create_ShedLock_Table.sql" ]; then
    print_result "ShedLock数据库脚本" 0
else
    print_result "ShedLock数据库脚本" 1
fi

echo ""
echo "=========================================="
echo "测试结果汇总"
echo "=========================================="
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}所有测试通过！✓${NC}"
    exit 0
else
    echo -e "\n${RED}部分测试失败，请检查日志${NC}"
    exit 1
fi
