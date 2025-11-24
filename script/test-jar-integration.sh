#!/bin/bash
set -e

echo "=========================================="
echo "Snappy-Java Integration Test"
echo "=========================================="

# Detect Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"

# Build the JAR
echo ""
echo "Building JAR..."
./sbt package

# Find the JAR
JAR_FILE=$(ls -t target/snappy-java-*.jar | grep -v sources | grep -v javadoc | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find snappy-java JAR"
    exit 1
fi
echo "Using JAR: $JAR_FILE"

# Check manifest
echo ""
echo "Checking JAR manifest..."
jar xf "$JAR_FILE" META-INF/MANIFEST.MF
if grep -q "Enable-Native-Access: ALL-UNNAMED" META-INF/MANIFEST.MF; then
    echo "✓ Manifest contains Enable-Native-Access: ALL-UNNAMED"
else
    echo "✗ WARNING: Manifest does NOT contain Enable-Native-Access attribute"
fi
rm -rf META-INF

# Create temp directory
TEMP_DIR=$(mktemp -d)
echo ""
echo "Using temp directory: $TEMP_DIR"

# Copy test source
cp src/test/resources/integration/SnappyIntegrationTest.java "$TEMP_DIR/"

# Compile test
echo ""
echo "Compiling test program..."
javac -cp "$JAR_FILE" -d "$TEMP_DIR" "$TEMP_DIR/SnappyIntegrationTest.java"

# Run test WITHOUT --enable-native-access flag
echo ""
echo "=========================================="
echo "Running test (WITHOUT --enable-native-access flag)..."
echo "=========================================="

cd "$TEMP_DIR"
java -cp ".:$OLDPWD/$JAR_FILE" SnappyIntegrationTest 2>&1
EXIT_CODE=$?
cd - > /dev/null

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ Test PASSED (exit code: $EXIT_CODE)"
else
    echo "✗ Test FAILED (exit code: $EXIT_CODE)"
fi
echo "=========================================="

# Cleanup
rm -rf "$TEMP_DIR"

exit $EXIT_CODE
