#!/bin/bash

# Device information script
# Usage: ./devices.sh

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Connected Android Devices${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Check if adb exists
if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}⚠ ADB not found${NC}"
    exit 1
fi

# Get all devices
devices=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$devices" ]; then
    echo "❌ No devices connected"
    echo ""
    echo "To connect a device:"
    echo "  • USB: Enable USB debugging and connect cable"
    echo "  • WiFi: adb connect <IP>:5555"
    echo "  • Emulator: Start from Android Studio"
    exit 1
fi

count=1
while IFS= read -r device; do
    echo -e "${GREEN}Device $count:${NC} $device"

    # Get device properties
    model=$(adb -s "$device" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    manufacturer=$(adb -s "$device" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r')
    android_version=$(adb -s "$device" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
    api_level=$(adb -s "$device" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')

    # Detect type
    if [[ "$device" == *":"* ]]; then
        type="WiFi"
    elif [[ $(adb -s "$device" shell getprop ro.kernel.qemu 2>/dev/null) == "1" ]]; then
        type="Emulator"
    else
        type="USB Cable"
    fi

    echo "  📱 Model: $manufacturer $model"
    echo "  🤖 Android: $android_version (API $api_level)"
    echo "  🔌 Connection: $type"

    # Check if Mentra is installed
    if adb -s "$device" shell pm list packages | grep -q "com.example.mentra"; then
        version=$(adb -s "$device" shell dumpsys package com.example.mentra | grep versionName | head -1 | awk '{print $1}' | cut -d'=' -f2)
        echo -e "  ✅ Mentra: Installed (v$version)"
    else
        echo "  ⚪ Mentra: Not installed"
    fi

    echo ""
    count=$((count + 1))
done <<< "$devices"

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo "Total devices: $(echo "$devices" | wc -l | xargs)"
echo ""

# Offer to launch app
PACKAGE_NAME="com.example.mentra"
ACTIVITY_NAME="$PACKAGE_NAME/.MainActivity"

# Check if any device has Mentra installed
has_mentra=false
while IFS= read -r device; do
    if adb -s "$device" shell pm list packages | grep -q "$PACKAGE_NAME"; then
        has_mentra=true
        break
    fi
done <<< "$devices"

if [ "$has_mentra" = true ]; then
    echo -e "${YELLOW}Launch Mentra on a device? (y/n)${NC}"
    read -r response

    if [[ "$response" =~ ^[Yy]$ ]]; then
        # If only one device, use it; otherwise ask which one
        device_count=$(echo "$devices" | wc -l | xargs)

        if [ "$device_count" -eq 1 ]; then
            selected_device=$(echo "$devices" | head -1)
        else
            echo "Select device number (1-$device_count):"
            read -r device_num
            selected_device=$(echo "$devices" | sed -n "${device_num}p")
        fi

        if [ ! -z "$selected_device" ]; then
            echo -e "${BLUE}🚀 Launching Mentra on $selected_device...${NC}"
            adb -s "$selected_device" shell am force-stop "$PACKAGE_NAME" 2>/dev/null
            sleep 1
            adb -s "$selected_device" shell am start -n "$ACTIVITY_NAME"

            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ App launched!${NC}"
            else
                echo "⚠️  Failed to launch app"
            fi
        fi
    fi
fi

