#!/bin/bash

# ========================================
# 回滚脚本 - 图书馆书籍定位系统
# ========================================

set -e

echo "=========================================="
echo "回滚脚本 - 图书馆书籍定位系统"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# 配置变量
DEPLOY_DIR="/opt/library-backend"
BACKUP_DIR="/opt/library-backend/backups"
SERVICE_NAME="library-backend"
DB_NAME="library_prod"
DB_USER="root"

# 检查是否以root运行
if [ "$EUID" -ne 0 ]; then
    print_error "请使用root权限运行此脚本"
    exit 1
fi

# 显示可用的备份
show_backups() {
    echo "可用的备份版本："
    echo "----------------------------"

    if [ -d "$BACKUP_DIR" ]; then
        ls -lht "$BACKUP_DIR" | grep "^d" | awk '{print $9}' | nl
    else
        print_error "备份目录不存在"
        exit 1
    fi

    echo ""
}

# 选择备份版本
select_backup() {
    show_backups

    read -p "请输入要回滚到的备份编号: " backup_num

    BACKUP_VERSION=$(ls -t "$BACKUP_DIR" | sed -n "${backup_num}p")

    if [ -z "$BACKUP_VERSION" ]; then
        print_error "无效的备份编号"
        exit 1
    fi

    BACKUP_PATH="$BACKUP_DIR/$BACKUP_VERSION"

    print_info "选择的备份: $BACKUP_VERSION"
    print_info "备份路径: $BACKUP_PATH"
    echo ""
}

# 确认回滚
confirm_rollback() {
    print_warning "即将回滚到版本: $BACKUP_VERSION"
    print_warning "此操作将："
    echo "  1. 停止当前服务"
    echo "  2. 恢复应用程序"
    echo "  3. 恢复数据库（可选）"
    echo "  4. 重启服务"
    echo ""

    read -p "确认回滚? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        print_info "回滚已取消"
        exit 0
    fi
}

# 停止服务
stop_service() {
    print_info "停止服务..."

    systemctl stop $SERVICE_NAME

    # 等待服务完全停止
    sleep 5

    if systemctl is-active --quiet $SERVICE_NAME; then
        print_error "服务停止失败"
        exit 1
    fi

    print_success "服务已停止"
}

# 备份当前版本
backup_current() {
    print_info "备份当前版本..."

    CURRENT_BACKUP_DIR="$BACKUP_DIR/rollback_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$CURRENT_BACKUP_DIR"

    # 备份应用
    if [ -f "$DEPLOY_DIR/library-backend.jar" ]; then
        cp "$DEPLOY_DIR/library-backend.jar" "$CURRENT_BACKUP_DIR/"
        print_success "当前应用已备份到: $CURRENT_BACKUP_DIR"
    fi

    # 备份数据库
    print_info "备份当前数据库..."
    mysqldump -u $DB_USER -p $DB_NAME > "$CURRENT_BACKUP_DIR/database_backup.sql" 2>/dev/null || {
        print_warning "数据库备份失败（可能需要手动输入密码）"
    }
}

# 恢复应用
restore_application() {
    print_info "恢复应用程序..."

    if [ -f "$BACKUP_PATH/library-backend.jar" ]; then
        cp "$BACKUP_PATH/library-backend.jar" "$DEPLOY_DIR/"
        print_success "应用程序已恢复"
    else
        print_error "备份中未找到应用程序文件"
        exit 1
    fi
}

# 恢复数据库
restore_database() {
    read -p "是否恢复数据库? (yes/no): " restore_db

    if [ "$restore_db" = "yes" ]; then
        print_info "恢复数据库..."

        if [ -f "$BACKUP_PATH/database_backup.sql" ]; then
            print_warning "即将恢复数据库，所有当前数据将被覆盖"
            read -p "确认恢复数据库? (yes/no): " confirm_db

            if [ "$confirm_db" = "yes" ]; then
                mysql -u $DB_USER -p $DB_NAME < "$BACKUP_PATH/database_backup.sql" 2>/dev/null || {
                    print_error "数据库恢复失败"
                    exit 1
                }
                print_success "数据库已恢复"
            else
                print_info "跳过数据库恢复"
            fi
        else
            print_warning "备份中未找到数据库文件"
        fi
    else
        print_info "跳过数据库恢复"
    fi
}

# 启动服务
start_service() {
    print_info "启动服务..."

    systemctl start $SERVICE_NAME

    # 等待服务启动
    sleep 10

    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "服务已启动"
    else
        print_error "服务启动失败"
        print_info "查看日志: journalctl -u $SERVICE_NAME -n 50"
        exit 1
    fi
}

# 验证回滚
verify_rollback() {
    print_info "验证回滚..."

    # 检查服务状态
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "服务运行正常"
    else
        print_error "服务未运行"
        return 1
    fi

    # 检查健康端点
    sleep 5
    HEALTH_CHECK=$(curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "failed")

    if echo "$HEALTH_CHECK" | grep -q '"status":"UP"'; then
        print_success "健康检查通过"
    else
        print_error "健康检查失败"
        print_info "响应: $HEALTH_CHECK"
        return 1
    fi

    print_success "回滚验证通过"
}

# 回滚失败处理
rollback_failed() {
    print_error "回滚失败！"
    print_info "尝试恢复到回滚前的状态..."

    # 尝试恢复
    if [ -d "$CURRENT_BACKUP_DIR" ]; then
        cp "$CURRENT_BACKUP_DIR/library-backend.jar" "$DEPLOY_DIR/"
        systemctl start $SERVICE_NAME
        print_info "已尝试恢复到回滚前的状态"
    fi

    print_error "请手动检查系统状态"
    exit 1
}

# 生成回滚报告
generate_report() {
    REPORT_FILE="$DEPLOY_DIR/rollback_report_$(date +%Y%m%d_%H%M%S).txt"

    cat > "$REPORT_FILE" << EOF
========================================
回滚报告
========================================

回滚时间: $(date '+%Y-%m-%d %H:%M:%S')
回滚版本: $BACKUP_VERSION
操作人员: $(whoami)

回滚内容:
- 应用程序: 已恢复
- 数据库: $([ "$restore_db" = "yes" ] && echo "已恢复" || echo "未恢复")

服务状态:
- 服务名称: $SERVICE_NAME
- 运行状态: $(systemctl is-active $SERVICE_NAME)
- 健康检查: $(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -o '"status":"[^"]*"' || echo "失败")

备份位置:
- 回滚前备份: $CURRENT_BACKUP_DIR
- 恢复的备份: $BACKUP_PATH

验证步骤:
1. 检查服务状态: systemctl status $SERVICE_NAME
2. 查看日志: journalctl -u $SERVICE_NAME -n 100
3. 测试API: curl http://localhost:8080/actuator/health
4. 访问前端: https://library.yourdomain.com

注意事项:
- 如果发现问题，请立即联系技术负责人
- 监控系统指标，确保无异常
- 通知相关人员回滚已完成

========================================
EOF

    print_success "回滚报告已生成: $REPORT_FILE"
}

# 主流程
main() {
    echo "开始回滚流程..."
    echo ""

    # 选择备份版本
    select_backup

    # 确认回滚
    confirm_rollback

    # 停止服务
    stop_service

    # 备份当前版本
    backup_current

    # 恢复应用
    restore_application

    # 恢复数据库
    restore_database

    # 启动服务
    start_service

    # 验证回滚
    if verify_rollback; then
        print_success "回滚成功！"

        # 生成报告
        generate_report

        echo ""
        echo "=========================================="
        echo "回滚完成"
        echo "=========================================="
        echo ""
        echo "下一步操作："
        echo "  1. 检查应用日志: journalctl -u $SERVICE_NAME -f"
        echo "  2. 监控系统指标"
        echo "  3. 通知相关人员"
        echo "  4. 查看回滚报告: cat $REPORT_FILE"
        echo ""
    else
        rollback_failed
    fi
}

# 执行主流程
main
