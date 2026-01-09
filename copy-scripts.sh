#!/bin/bash

# Copy development scripts to all project folders
# This ensures every Android project has the useful development scripts

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECTS_DIR="/Users/danielkinyua/Downloads/projects"
SOURCE_DIR="$PROJECTS_DIR/mentra"

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Copy Scripts to All Projects${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Check if source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    echo -e "${YELLOW}⚠ Source directory not found: $SOURCE_DIR${NC}"
    exit 1
fi

# Find all shell scripts in mentra
SCRIPTS=$(find "$SOURCE_DIR" -maxdepth 1 -name "*.sh" -type f)

if [ -z "$SCRIPTS" ]; then
    echo -e "${YELLOW}⚠ No shell scripts found in $SOURCE_DIR${NC}"
    exit 1
fi

SCRIPT_COUNT=$(echo "$SCRIPTS" | wc -l | xargs)
echo -e "${BLUE}ℹ Found $SCRIPT_COUNT script(s) in mentra${NC}"
echo ""

# List the scripts
while IFS= read -r script; do
    basename "$script"
done <<< "$SCRIPTS"

echo ""
echo -e "${BLUE}ℹ Copying to all project folders...${NC}"
echo ""

# Find all project directories (exclude hidden dirs like .idea)
PROJECT_DIRS=$(find "$PROJECTS_DIR" -maxdepth 1 -type d ! -path "$PROJECTS_DIR" ! -name ".*" ! -name "mentra")

if [ -z "$PROJECT_DIRS" ]; then
    echo -e "${YELLOW}⚠ No other project directories found${NC}"
    exit 0
fi

COPIED_COUNT=0

# Copy scripts to each project directory
while IFS= read -r project_dir; do
    if [ -d "$project_dir" ]; then
        PROJECT_NAME=$(basename "$project_dir")

        echo -e "${BLUE}📁 Copying to: ${NC}$PROJECT_NAME"

        # Copy all scripts
        cp "$SOURCE_DIR"/*.sh "$project_dir/" 2>/dev/null

        if [ $? -eq 0 ]; then
            # Make them executable
            chmod +x "$project_dir"/*.sh 2>/dev/null
            echo -e "${GREEN}   ✓ Scripts copied and made executable${NC}"
            COPIED_COUNT=$((COPIED_COUNT + 1))
        else
            echo -e "${YELLOW}   ⚠ Failed to copy scripts${NC}"
        fi
    fi
done <<< "$PROJECT_DIRS"

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Complete!${NC}"
echo -e "${GREEN}Copied scripts to $COPIED_COUNT project(s)${NC}"
echo ""
echo "Scripts available in each project:"
echo "  • run.sh - Build, install, launch, logs"
echo "  • quick-install.sh - Clean build and launch"
echo "  • start.sh - Just launch the app"
echo "  • stop.sh - Stop the app"
echo "  • devices.sh - List devices and launch"
echo "  • logs.sh - View filtered logs"

