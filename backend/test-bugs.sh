#!/bin/bash

# ========================================
# Bug修复验证测试脚本
# ========================================

set -e

echo "=========================================="
echo "Bug修复验证测试"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080"
TOKEN=""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

# 测试1: Bug #3 - 并发借阅数量限制
test_bug3_concurrent_borrow() {
    echo "测试Bug #3: 并发借阅数量限制"
    echo "----------------------------"

    # 登录获取token
    print_info "登录获取token..."
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"studentId": "2021001", "password": "Test123456"}')

    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$TOKEN" ]; then
        print_error "登录失败，无法获取token"
        return 1
    fi

    print_success "登录成功"

    # 并发申请借阅
    print_info "发起10个并发借阅申请..."

    for i in {1..10}; do
        curl -s -X POST "$BASE_URL/api/borrow/apply" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"bookId\": $i, \"notes\": \"并发测试$i\"}" &
    done

    wait

    # 检查当前借阅数量
    print_info "检查当前借阅数量..."
    CURRENT_BORROWS=$(curl -s -X GET "$BASE_URL/api/borrow/current" \
        -H "Authorization: Bearer $TOKEN" | grep -o '"data":\[.*\]' | grep -o '{' | wc -l)

    if [ "$CURRENT_BORROWS" -le 5 ]; then
        print_success "Bug #3修复验证通过：借阅数量=$CURRENT_BORROWS (≤5)"
    else
        print_error "Bug #3修复验证失败：借阅数量=$CURRENT_BORROWS (>5)"
    fi

    echo ""
}

# 测试2: Bug #7 - 登录失败计数器
test_bug7_login_failure() {
    echo "测试Bug #7: 登录失败计数器"
    echo "----------------------------"

    print_info "发起10个并发登录失败请求..."

    for i in {1..10}; do
        curl -s -X POST "$BASE_URL/api/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"studentId": "test_user", "password": "wrong_password"}' &
    done

    wait

    # 尝试再次登录，应该被锁定
    print_info "尝试再次登录（应该被锁定）..."
    LOCK_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"studentId": "test_user", "password": "wrong_password"}')

    if echo "$LOCK_RESPONSE" | grep -q "已被锁定"; then
        print_success "Bug #7修复验证通过：账号已被锁定"
    else
        print_error "Bug #7修复验证失败：账号未被锁定"
    fi

    echo ""
}

# 测试3: Bug #13 - 定时任务重复执行
test_bug13_scheduled_tasks() {
    echo "测试Bug #13: 定时任务重复执行"
    echo "----------------------------"

    print_info "检查ShedLock表是否存在..."

    # 这里需要数据库连接，暂时跳过
    print_info "需要手动验证："
    echo "  1. 启动多个应用实例"
    echo "  2. 等待定时任务执行"
    echo "  3. 检查数据库shedlock表"
    echo "  4. 验证日志中只有一个实例执行了任务"

    echo ""
}

# 测试4: Bug #1 - 逾期天数计算
test_bug1_overdue_calculation() {
    echo "测试Bug #1: 逾期天数计算"
    echo "----------------------------"

    print_info "需要手动验证："
    echo "  1. 创建一个到期日为今天的借阅记录"
    echo "  2. 在今天23:59归还"
    echo "  3. 验证不会被收取罚款"

    echo ""
}

# 测试5: Bug #4 - 续借逾期检查
test_bug4_renew_overdue() {
    echo "测试Bug #4: 续借逾期检查"
    echo "----------------------------"

    print_info "需要手动验证："
    echo "  1. 创建一个已逾期的借阅记录"
    echo "  2. 尝试续借"
    echo "  3. 验证被拒绝"

    echo ""
}

# 测试6: 健康检查
test_health_check() {
    echo "测试: 应用健康检查"
    echo "----------------------------"

    HEALTH_RESPONSE=$(curl -s "$BASE_URL/actuator/health")

    if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
        print_success "应用健康检查通过"
    else
        print_error "应用健康检查失败"
    fi

    echo ""
}

# 测试7: Prometheus指标
test_prometheus_metrics() {
    echo "测试: Prometheus指标"
    echo "----------------------------"

    METRICS_RESPONSE=$(curl -s "$BASE_URL/actuator/prometheus")

    if [ -n "$METRICS_RESPONSE" ]; then
        print_success "Prometheus指标可访问"

        # 统计指标数量
        METRIC_COUNT=$(echo "$METRICS_RESPONSE" | grep -c "^[a-z]")
        print_info "指标数量: $METRIC_COUNT"
    else
        print_error "Prometheus指标不可访问"
    fi

    echo ""
}

# 主测试流程
main() {
    # 检查应用是否运行
    if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_error "应用未运行，请先启动应用"
        exit 1
    fi

    print_success "应用正在运行"
    echo ""

    # 执行测试
    test_health_check
    test_prometheus_metrics
    test_bug3_concurrent_borrow
    test_bug7_login_failure
    test_bug13_scheduled_tasks
    test_bug1_overdue_calculation
    test_bug4_renew_overdue

    echo "=========================================="
    echo "测试完成"
    echo "=========================================="
}

# 执行主流程
main
