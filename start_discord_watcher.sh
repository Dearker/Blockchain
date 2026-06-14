#!/bin/bash
# ============================================================
# start_discord_watcher.sh
# 编译并启动 Discord 通知监听程序
# 前置条件：macOS + Xcode Command Line Tools (xcode-select --install)
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWIFT_SRC="$SCRIPT_DIR/src/main/java/com/blockchain/swift/discord_notification_watcher.swift"
OUTPUT="$SCRIPT_DIR/discord_watcher"

echo "=== Discord 通知监听器 ==="
echo ""

# 检查 swiftc
if ! command -v swiftc &>/dev/null; then
    echo "[ERROR] 未找到 swiftc，请先安装 Xcode Command Line Tools："
    echo "  xcode-select --install"
    exit 1
fi

# 编译
echo "[1/2] 编译 Swift 程序..."
swiftc "$SWIFT_SRC" -o "$OUTPUT" -O
echo "      编译成功: $OUTPUT"

# 检查通知数据库权限
DB_PATH="$HOME/Library/Application Support/com.apple.notificationcenter/db2/db"
if [ ! -f "$DB_PATH" ]; then
    echo ""
    echo "[WARN] 通知数据库不存在: $DB_PATH"
    echo "       请确认 macOS 版本（支持 macOS 12–14）"
    echo "       并在「系统设置 > 隐私与安全性 > 完全磁盘访问权限」"
    echo "       中将「终端」添加进去，然后重新运行此脚本"
    echo ""
else
    echo "      通知数据库: ✅ 已找到"
fi

echo ""
echo "[2/2] 启动监听程序..."
echo "      目标: http://localhost:8000/api/notifications/discord"
echo "      监控页: http://localhost:8000/discord-monitor.html"
echo "      按 Ctrl+C 停止"
echo ""

"$OUTPUT"
