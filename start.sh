#!/bin/bash

# Start Mentra app via ADB intent
# Usage: ./start.sh

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PACKAGE_NAME="com.example.mentra"
ACTIVITY_NAME="$PACKAGE_NAME/.MainActivity"

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - App Launcher${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗ ADB not found${NC}"
    exit 1
fi

# Get the first available device
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo -e "${RED}✗ No device connected${NC}"
    echo ""
    echo "Please connect a device or start an emulator"
    exit 1
fi

# Get device info
DEVICE_NAME=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VERSION=$(adb -s "$DEVICE" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')

echo -e "${BLUE}📱 Device: ${NC}$DEVICE_NAME"
echo -e "${BLUE}🤖 Android: ${NC}$ANDROID_VERSION"
echo ""

# Check if app is installed
if ! adb -s "$DEVICE" shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo -e "${RED}✗ Mentra is not installed${NC}"
    echo ""
    echo "Install it first:"
    echo "  ./run.sh          # Build and install"
    echo "  or"
    echo "  ./quick-install.sh  # Clean build and install"
    exit 1
fi

# Stop app if running
IS_RUNNING=$(adb -s "$DEVICE" shell "pidof $PACKAGE_NAME" 2>/dev/null)

if [ ! -z "$IS_RUNNING" ]; then
    echo -e "${YELLOW}⚠ App is running (PID: $IS_RUNNING). Restarting...${NC}"
    adb -s "$DEVICE" shell am force-stop "$PACKAGE_NAME"
    sleep 1
else
    echo -e "${BLUE}ℹ Starting app...${NC}"
fi

# Launch the app using intent
adb -s "$DEVICE" shell am start -n "$ACTIVITY_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Failed to start app${NC}"
    exit 1
fi

sleep 2

# Verify app is running
IS_RUNNING=$(adb -s "$DEVICE" shell "pidof $PACKAGE_NAME" 2>/dev/null)

if [ ! -z "$IS_RUNNING" ]; then
    echo -e "${GREEN}✓ App started successfully!${NC}"
    echo -e "${BLUE}ℹ PID: ${NC}$IS_RUNNING"
    echo ""
    echo "View logs with: ./logs.sh"
else
    echo -e "${YELLOW}⚠ App may not have started${NC}"
    echo "Check device screen or run: adb logcat"
fi

