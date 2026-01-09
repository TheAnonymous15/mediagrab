#!/bin/bash

# Quick clean build and install script
# Usage: ./quick-install.sh

set -e

PACKAGE_NAME="com.example.mentra"
ACTIVITY_NAME="$PACKAGE_NAME/.MainActivity"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🧹 Cleaning build...${NC}"
./gradlew clean --no-daemon

echo -e "${BLUE}🔨 Building debug APK...${NC}"
./gradlew assembleDebug --no-daemon

echo -e "${BLUE}📱 Installing on device...${NC}"
./gradlew installDebug --no-daemon

# Get the device
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')

if [ ! -z "$DEVICE" ]; then
    echo ""
    echo -e "${BLUE}🚀 Launching Mentra...${NC}"

    # Stop app if running
    adb -s "$DEVICE" shell am force-stop "$PACKAGE_NAME" 2>/dev/null
    sleep 1

    # Launch the app
    adb -s "$DEVICE" shell am start -n "$ACTIVITY_NAME"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ App launched successfully!${NC}"
    else
        echo "⚠️  App installed but failed to launch"
    fi
else
    echo -e "${GREEN}✅ Done! No device detected for auto-launch${NC}"
fi


