#!/bin/bash
    "*:F"
    "System.err:*" \
    "AndroidRuntime:E" \
    "PermissionManager:*" \
    "MainActivity:*" \
    -s "Mentra:*" \
adb -s "$DEVICE" logcat \
adb -s "$DEVICE" logcat -c  # Clear logcat first
# Show logs with filters

echo ""
echo "════════════════════════════════════════"
echo "Press Ctrl+C to stop"
echo ""
echo "📦 Package: $PACKAGE_NAME"
echo "📱 Device: $DEVICE_NAME"
DEVICE_NAME=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')

fi
    exit 1
    echo "❌ No device connected"
if [ -z "$DEVICE" ]; then

DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')
# Get device

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Logcat Viewer${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

NC='\033[0m'
BLUE='\033[0;34m'
# Colors

PACKAGE_NAME="com.example.mentra"

# Usage: ./logs.sh
# Logcat viewer for Mentra app


