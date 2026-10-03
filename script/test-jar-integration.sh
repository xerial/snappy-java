#!/bin/bash
set -e

echo "=========================================="
echo "Snappy-Java Integration Test"
echo "=========================================="

# sbt 2 requires JDK 17+, so CI may run sbt on a newer JDK and test on TEST_JAVA_HOME
JAVA_BIN=${TEST_JAVA_HOME:+$TEST_JAVA_HOME/bin/}

# Detect Java version
JAVA_VERSION=$(${JAVA_BIN}java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"

# Build the JAR
echo ""
echo "Building JAR..."
./sbt package

# Find the JAR
JAR_FILE=$(ls -t $(find target -name 'snappy-java-*.jar') | grep -v -e sources -e javadoc -e tests | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find snappy-java JAR"
    exit 1
fi
echo "Using JAR: $JAR_FILE"

# Create temp directory
TEMP_DIR=$(mktemp -d)
echo ""
echo "Using temp directory: $TEMP_DIR"

# Copy test source
cp src/test/resources/integration/SnappyIntegrationTest.java "$TEMP_DIR/"

# Compile test
echo ""
echo "Compiling test program..."
${JAVA_BIN}javac -cp "$JAR_FILE" -d "$TEMP_DIR" "$TEMP_DIR/SnappyIntegrationTest.java"

# Run test WITHOUT --enable-native-access flag
echo ""
echo "=========================================="
echo "Running test (WITHOUT --enable-native-access flag)..."
echo "=========================================="

cd "$TEMP_DIR"
${JAVA_BIN}java -cp ".:$OLDPWD/$JAR_FILE" SnappyIntegrationTest 2>&1
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
