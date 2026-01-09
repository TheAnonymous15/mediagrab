#!/bin/bash

# Mentra App - Build, Install, and Launch Script
# Automatically detects devices and prioritizes: Cable > WiFi > Emulator

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# App package name
PACKAGE_NAME="com.example.mentra"
ACTIVITY_NAME="$PACKAGE_NAME/.MainActivity"

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Build & Launch Script${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check if adb is installed
if ! command -v adb &> /dev/null; then
    print_error "ADB is not installed or not in PATH"
    echo "Please install Android SDK Platform Tools"
    exit 1
fi

# Function to detect device type
get_device_type() {
    local device_id=$1
    local device_info=$(adb -s "$device_id" shell getprop ro.kernel.qemu 2>/dev/null)

    if [[ "$device_id" == *":"* ]]; then
        echo "wifi"
    elif [[ "$device_info" == "1" ]]; then
        echo "emulator"
    else
        echo "cable"
    fi
}

# Get all connected devices
print_info "Checking for connected devices..."
devices=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$devices" ]; then
    print_error "No devices connected"
    echo "Please connect a device or start an emulator"
    exit 1
fi

# Count devices
device_count=$(echo "$devices" | wc -l | xargs)
print_success "Found $device_count device(s)"

# Categorize devices
declare -a cable_devices
declare -a wifi_devices
declare -a emulator_devices

while IFS= read -r device; do
    device_type=$(get_device_type "$device")
    case $device_type in
        cable)
            cable_devices+=("$device")
            ;;
        wifi)
            wifi_devices+=("$device")
            ;;
        emulator)
            emulator_devices+=("$device")
            ;;
    esac
done <<< "$devices"

# Display found devices
if [ ${#cable_devices[@]} -gt 0 ]; then
    print_info "Cable devices: ${cable_devices[*]}"
fi
if [ ${#wifi_devices[@]} -gt 0 ]; then
    print_info "WiFi devices: ${wifi_devices[*]}"
fi
if [ ${#emulator_devices[@]} -gt 0 ]; then
    print_info "Emulator devices: ${emulator_devices[*]}"
fi

# Select device with priority: Cable > WiFi > Emulator
if [ ${#cable_devices[@]} -gt 0 ]; then
    SELECTED_DEVICE="${cable_devices[0]}"
    DEVICE_TYPE="Cable"
elif [ ${#wifi_devices[@]} -gt 0 ]; then
    SELECTED_DEVICE="${wifi_devices[0]}"
    DEVICE_TYPE="WiFi"
elif [ ${#emulator_devices[@]} -gt 0 ]; then
    SELECTED_DEVICE="${emulator_devices[0]}"
    DEVICE_TYPE="Emulator"
else
    print_error "No suitable device found"
    exit 1
fi

# Get device name
DEVICE_NAME=$(adb -s "$SELECTED_DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VERSION=$(adb -s "$SELECTED_DEVICE" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')

echo ""
print_success "Selected device: $DEVICE_NAME ($DEVICE_TYPE)"
print_info "Android version: $ANDROID_VERSION"
print_info "Device ID: $SELECTED_DEVICE"
echo ""

# Build the app
print_info "Building debug APK..."
./gradlew assembleDebug --no-daemon

if [ $? -ne 0 ]; then
    print_error "Build failed"
    exit 1
fi

print_success "Build successful"
echo ""

# Find the APK
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    print_error "APK not found at $APK_PATH"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
print_info "APK size: $APK_SIZE"

# Install the app
print_info "Installing app on $DEVICE_NAME..."
adb -s "$SELECTED_DEVICE" install -r "$APK_PATH"

if [ $? -ne 0 ]; then
    print_error "Installation failed"
    exit 1
fi

print_success "Installation successful"
echo ""

# Check if app is already running and stop it
print_info "Checking if app is running..."
IS_RUNNING=$(adb -s "$SELECTED_DEVICE" shell "pidof $PACKAGE_NAME" 2>/dev/null)

if [ ! -z "$IS_RUNNING" ]; then
    print_warning "App is already running. Stopping..."
    adb -s "$SELECTED_DEVICE" shell am force-stop "$PACKAGE_NAME"
    sleep 1
fi

# Clear app data (optional - comment out if you want to preserve data)
# print_info "Clearing app data..."
# adb -s "$SELECTED_DEVICE" shell pm clear "$PACKAGE_NAME"

# Launch the app
print_info "Launching Mentra..."
adb -s "$SELECTED_DEVICE" shell am start -n "$ACTIVITY_NAME"

if [ $? -ne 0 ]; then
    print_error "Failed to launch app"
    exit 1
fi

sleep 2

# Verify app is running
IS_RUNNING=$(adb -s "$SELECTED_DEVICE" shell "pidof $PACKAGE_NAME" 2>/dev/null)

if [ ! -z "$IS_RUNNING" ]; then
    print_success "App launched successfully!"
    echo ""
    print_info "PID: $IS_RUNNING"

    # Open logcat for the app
    echo ""
    print_info "Opening logcat (Press Ctrl+C to stop)..."
    echo -e "${YELLOW}─────────────────────────────────────────${NC}"
    adb -s "$SELECTED_DEVICE" logcat -s "Mentra:*" "AndroidRuntime:E" "*:F"
else
    print_warning "App may not have launched successfully"
    echo "Check the device screen or run: adb logcat"
fi

