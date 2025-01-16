# Building with musl libc

This project now supports building with musl libc, which is commonly used in Alpine Linux. To build using musl:

1. Use the provided Alpine-based Dockerfile:
```bash
docker build -f docker/Dockerfile.alpine -t snappy-java-alpine .
```

2. The build process will:
- Use musl libc instead of glibc
- Statically link required libraries
- Create a native library compatible with Alpine/musl systems

The resulting library will be placed in:
`src/main/resources/org/xerial/snappy/native/Linux-musl/x86_64/libsnappyjava.so`

## Testing
To test the musl build:
```bash
docker run --rm snappy-java-alpine -cp target/snappy-java-*.jar org.xerial.snappy.SnappyLoader
```