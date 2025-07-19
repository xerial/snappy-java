# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

snappy-java is a Java port of Google's Snappy compression library, providing fast compression/decompression with JNI bindings for native performance across multiple platforms.

## Build Commands

### Using sbt (primary build tool)

```bash
# Enter sbt console
./sbt

# Run tests
./sbt test

# Run tests matching a pattern
./sbt "testOnly *BitShuffleTest"

# Create jar file
./sbt package

# Publish to local Maven repository
./sbt publishM2

# Run tests continuously on code changes
./sbt "~test"
```

### Using make (for native library compilation)

```bash
# Build native libraries
make

# Clean build artifacts
make clean

# Platform-specific builds (when cross-compiling)
make native-all
```

## Architecture

### Core Components

1. **Java API Layer** (`src/main/java/org/xerial/snappy/`)
   - `Snappy.java`: Main API facade providing high-level compression/decompression methods
   - `SnappyNative.java`: JNI interface to native Snappy library
   - `SnappyLoader.java`: Handles platform-specific native library loading

2. **Native Layer** (`src/main/java/org/xerial/snappy/`)
   - `SnappyNative.cpp`: JNI implementation bridging Java and C++ Snappy
   - `BitShuffleNative.cpp`: JNI implementation for BitShuffle algorithm

3. **Stream API** (`src/main/java/org/xerial/snappy/`)
   - `SnappyOutputStream`/`SnappyInputStream`: Block-based compression streams
   - `SnappyFramedOutputStream`/`SnappyFramedInputStream`: Framing format implementation
   - `SnappyHadoopCompatibleOutputStream`: Hadoop-compatible format

4. **Memory Management** (`src/main/java/org/xerial/snappy/buffer/` and `/pool/`)
   - Buffer allocation and pooling for efficient memory usage
   - Direct ByteBuffer management for zero-copy operations

### Platform Support

The project includes pre-built native libraries for multiple platforms in `src/main/resources/org/xerial/snappy/native/`:
- Windows (x86, x86_64, aarch64)
- macOS (x86, x86_64, aarch64)
- Linux (x86, x86_64, aarch64, arm, armv6, armv7, ppc64, ppc64le, s390x, riscv64, loongarch64)
- AIX (ppc, ppc64)
- SunOS (x86, x86_64, sparc)
- Android (arm, aarch64)

### Cross-compilation

The project uses Docker-based cross-compilation toolchains (see `docker/` directory) for building native libraries across different architectures.

## Testing

```bash
# Run all tests with debug logging
./sbt "testOnly * -- -l debug"

# Run specific test class
./sbt "testOnly org.xerial.snappy.SnappyTest"

# The project uses AirSpec (for Scala tests) and JUnit (for Java tests)
# Tests are located in src/test/java/org/xerial/snappy/
```

## Important Notes

1. **Native Library Loading**: The project automatically detects the platform and loads the appropriate native library from resources
2. **Memory Safety**: Uses direct ByteBuffers for efficient memory operations - be aware of buffer boundaries
3. **Thread Safety**: Snappy compression/decompression methods are thread-safe as they don't maintain state
4. **OSGi Support**: The project includes OSGi bundle configuration in build.sbt
5. **Compatibility**: Multiple stream formats are supported - ensure you use matching read/write formats (see compatibility matrix in README.md)