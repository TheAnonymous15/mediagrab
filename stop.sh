#!/bin/bash

# Stop Mentra app via ADB
# Usage: ./stop.sh

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PACKAGE_NAME="com.example.mentra"

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Stop App${NC}"
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
    exit 1
fi

# Get device info
DEVICE_NAME=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')

echo -e "${BLUE}📱 Device: ${NC}$DEVICE_NAME"
echo ""

# Check if app is running
IS_RUNNING=$(adb -s "$DEVICE" shell "pidof $PACKAGE_NAME" 2>/dev/null)

if [ -z "$IS_RUNNING" ]; then
    echo -e "${YELLOW}ℹ App is not running${NC}"
    exit 0
fi

echo -e "${BLUE}ℹ Stopping app (PID: $IS_RUNNING)...${NC}"
adb -s "$DEVICE" shell am force-stop "$PACKAGE_NAME"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ App stopped${NC}"
else
    echo -e "${RED}✗ Failed to stop app${NC}"
    exit 1
fi

