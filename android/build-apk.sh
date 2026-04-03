#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/output"
IMAGE_NAME="scantoupload-builder"

echo "=== Scan to Upload — Android APK Builder ==="
echo ""

# Check Docker is available
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH."
    exit 1
fi

echo "[1/3] Building Docker image (this may take a few minutes on first run)..."
docker build -t "${IMAGE_NAME}" --target builder "${SCRIPT_DIR}"

echo "[2/3] Extracting APK..."
mkdir -p "${OUTPUT_DIR}"

# Create a temporary container to copy the APK out
CONTAINER_ID=$(docker create "${IMAGE_NAME}")
docker cp "${CONTAINER_ID}:/app/app/build/outputs/apk/release/" "${OUTPUT_DIR}/tmp_apk"
docker rm "${CONTAINER_ID}" > /dev/null

# Move APK to output directory with a clean name
APK_FILE=$(find "${OUTPUT_DIR}/tmp_apk" -name "*.apk" -type f | head -1)
if [ -z "${APK_FILE}" ]; then
    rm -rf "${OUTPUT_DIR}/tmp_apk"
    echo "Error: No APK file found in build output."
    exit 1
fi

mv "${APK_FILE}" "${OUTPUT_DIR}/scan-to-upload.apk"
rm -rf "${OUTPUT_DIR}/tmp_apk"

echo "[3/3] Cleaning up Docker image..."
docker rmi "${IMAGE_NAME}" > /dev/null 2>&1 || true

APK_SIZE=$(du -h "${OUTPUT_DIR}/scan-to-upload.apk" | cut -f1)
echo ""
echo "=== Done ==="
echo "APK: ${OUTPUT_DIR}/scan-to-upload.apk (${APK_SIZE})"
