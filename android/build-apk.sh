#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/output"
KEYSTORE_DIR="${SCRIPT_DIR}/keystore"
IMAGE_NAME="scantoupload-builder"
LEGACY_VOLUME_NAME="scantoupload-keystore"

echo "=== Scan to Upload — Android APK Builder ==="
echo ""

# Check Docker is available
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH."
    exit 1
fi

echo "[1/3] Building Docker image..."
docker build -t "${IMAGE_NAME}" "${SCRIPT_DIR}"

echo "[2/3] Building APK (keystore persisted in '${KEYSTORE_DIR}')..."
mkdir -p "${OUTPUT_DIR}"
mkdir -p "${KEYSTORE_DIR}"

# One-time migration from legacy Docker volume storage
if [ ! -f "${KEYSTORE_DIR}/release.jks" ] && docker volume inspect "${LEGACY_VOLUME_NAME}" &> /dev/null; then
    echo "Migrating existing keystore from legacy Docker volume '${LEGACY_VOLUME_NAME}'..."
    docker run --rm \
        -v "${LEGACY_VOLUME_NAME}:/legacy:ro" \
        -v "${KEYSTORE_DIR}:/keystore" \
        alpine:3.20 \
        sh -c "cp /legacy/release.jks /keystore/release.jks 2>/dev/null || true; cp /legacy/keystore.properties /keystore/keystore.properties 2>/dev/null || true"
fi

BUILD_START_EPOCH="$(date +%s)"

docker run --rm \
    -v "${KEYSTORE_DIR}:/keystore" \
    -v "${OUTPUT_DIR}:/output" \
    "${IMAGE_NAME}"

# Rename APK to a unique, timestamped filename
LATEST_APK=""
LATEST_MTIME=0

shopt -s nullglob
for CANDIDATE in "${OUTPUT_DIR}"/*.apk; do
    MTIME="$(stat -c %Y "${CANDIDATE}" 2>/dev/null || printf '0')"
    if [ "${MTIME}" -lt "${BUILD_START_EPOCH}" ]; then
        continue
    fi
    if [ "${MTIME}" -ge "${LATEST_MTIME}" ]; then
        LATEST_MTIME="${MTIME}"
        LATEST_APK="${CANDIDATE}"
    fi
done
shopt -u nullglob

if [ -z "${LATEST_APK}" ]; then
    echo "Error: No APK file found for this build run in '${OUTPUT_DIR}'."
    exit 1
fi

BUILD_TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
FINAL_APK="${OUTPUT_DIR}/scan-to-upload-${BUILD_TIMESTAMP}.apk"
if [ -e "${FINAL_APK}" ]; then
    SUFFIX=1
    while [ -e "${OUTPUT_DIR}/scan-to-upload-${BUILD_TIMESTAMP}-${SUFFIX}.apk" ]; do
        SUFFIX=$((SUFFIX + 1))
    done
    FINAL_APK="${OUTPUT_DIR}/scan-to-upload-${BUILD_TIMESTAMP}-${SUFFIX}.apk"
fi

if [ "${LATEST_APK}" != "${FINAL_APK}" ]; then
    mv "${LATEST_APK}" "${FINAL_APK}"
fi

echo "[3/3] Cleaning up Docker image..."
docker rmi "${IMAGE_NAME}" > /dev/null 2>&1 || true

APK_SIZE=$(du -h "${FINAL_APK}" | cut -f1)
echo ""
echo "=== Done ==="
echo "APK: ${FINAL_APK} (${APK_SIZE})"
echo "Keystore dir: ${KEYSTORE_DIR} (persistent across builds)"
