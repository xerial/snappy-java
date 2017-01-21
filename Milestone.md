Since vesion 1.1.0.x, Java 6 (1.6) or higher is required.

## snappy-java-1.1.3 (will be available Early 2017)
 * We are waiting contributions of native libraries for varios platforms.
   * Run `make native test` and send the result as a pull requset.
 * We also test the usability of newly added bitshuffle API before this release.

## snappy-java-1.1.3-M1 (2017-01-19)
 * This is a preview release. Appreciate your feedback.
 * Upgraded to snappy 1.1.3 (Minor compression performance improvement)
 * Added support for armv5, armv6, armv7, android-arm, aarch64, ppc64
 * Added BitShuffle class for better float value compression
 * Using docker-based cross compilers for building native libraries
 * AIX, FreeBSD, SunOS, IBM s390x are still using snappy 1.1.2. Your contirbutions of native libraries are welcome. Please send a pull request that contains the changes after runing `make native` in your platform.

## snappy-java-1.1.2.6 (2016-06-02)
 * Fix a bug in SnappyInputStream when reading compressed data that happened to have the same first byte with the stream magic header (#142)
  * Never use 1.1.2.5 (This is a broken build)

## snappy-java-1.1.2.4 (2016-03-30)
 * Improved `SnappyOutputStream.write(byte[], off, len)` performance.

## snappy-java-1.1.2.3 (2016-03-30)
 * Improved `SnappyInputStream.read(byte[], off, len)` performance.

## snappy-java-1.1.2.2 (2016-03-29)
 * Add AArch64 support

## snappy-java-1.1.2.1 (2016-01-22)
 * Fixed #131

## snappy-java-1.1.2 (22 September 2015)
 * This is a backward compatible release for 1.1.x. 
 * Add AIX (32-bit) support. 
    * There is no upgrade for the native libraries of the other platforms.
 * A major change since 1.1.1 is a support for reading concatenated results of SnappyOutputStream(s)

## snappy-java-1.1.2-RC2 (18 May 2015)
 * Fix #107: SnappyOutputStream.close() is not idempotent

## snappy-java-1.1.2-RC1 (13 May 2015)
 * SnappyInputStream now supports reading concatenated compressed results of SnappyOutputStream
 * There has been no compressed format change since 1.0.5.x. So You can read the compressed results interchangeablly between these versions.
 * Fixes a problem when java.io.tmpdir does not exist. 

## snappy-java-1.1.1.7 (14 Apr 2015)
 * Fixes #100

## snappy-java-1.1.1.6 (26 Oct 2014)
 * Fixes #88, #89, #90 and #91
 * Fixed the broken build of 1.1.1.4 and memory leak bug 1.1.1.5 (so never use these versions)

## snappy-java-1.0.5.4 (12 September 2014)
 * Embedded libstdc++ for Linux/amd64 native library (hotfix to 1.0.5.x series)

## snappy-java-1.1.1.3 (19 July 2014)
 * Improved the performance of SnappyOutputStream

## snappy-java-1.1.1 (4 July 2014)
  * Added Snappy framing format support: SnappyFramedInput/OutputStream
  * Added native libraries for PowerPC, IBM-AIX 6.4, SunOS.

# snappy-java-1.1.1-M4 (4 July 2014)
  * Add linux-x86_64 native library, embedding libstdc++ 
  * IBM AIX 6.1 support 
  * A fix for loading in OSGi in Mac OS X (#76)

# snappy-java-1.1.1-M3 (26 June 2014)
  * Add PowerPC support (ppc64, ppc64le)
  * Building with sbt

# snappy-java-1.0.5.2 (25 June 2014)
  * Maintanance release
  * Add PowerPC support ppc64le, ppc64 (big-endian)

# snappy-java-1.1.1-M2 (12 June 2014)
   * Bug fixes and some optimization of SnappyFramedFormat 
   * Added a native library for ppc64le 
   * Preview build (without libstdc++ embedded for Linux/x86_64)
   

## snappy-java-1.1.1-M1
  * Preview build (without libstdc++ embedded for Linux/x86_64)

## snappy-java-1.1.0.1 (8 November 2013)
  * Add SunOS/x86_64 support

## snappy-java-1.1.0  (17 October 2013)
  * Add Snappy framed format support (SnappyFramedInputStream, SnappyFramedOutputStream)
  * Add SunOS support
  * Dropped OpenBSD support
  * OSGi support

## snappy-java-1.1.0-M4 (20 August 2013)
  * New JNI native code loading mechanism, which does not rely on native code injection.
  * Add OpenBSD support
  * Add Framed format support
  * Recovered 32-bit Mac support
  * Fixes several issues
  * Target to Java6 (Java5 will be no longer supported)
  * Add zero-copy compression, decompression and isValidCompressedData for LArray <https://github.com/xerial/larray>

## snappy-java-1.1.0-M3 (28 March 2013)
  * Fix linux amd64 build (embed libstdc++)
  * Fixes #26

## snappy-java-1.1.0-M1 (27 March 2013)
  * Upgrade to snappy-1.1.0
  * Add zero-copy compression (rawCompress, rawUncompress) that can be used with LArray <https://github.com/xerial/larray>
  * Drop 32-bit Mac support

## snappy-java-1.0.5-M2 (27 September 2012)
  * Upgrade release for snappy-1.0.5

## snappy-java-1.0.4.1 (5 September 2011) 
  * Fixes issue 33: Fixes a problem when reading incomplete input stream

## snappy-java-1.0.4 (September 22nd, 2011) 
  * Upgrade to snappy-1.0.4
  * Enhanced the Javadoc 

## snappy-java-1.0.3.3 (September 10th, 2011)
  * Add support for Oracle JRockit JVM. (issue 28)

## snappy-java-1.0.3.2 (August 23rd, 2011)
  * Upgrade from the previous release is optional
  * Add system properites to switch sytem installed native library or bundled
  library (issue 20, issue 26)
  * source code tar ball (issue 25)
  * primitive array read/write support (issue 24)

## snappy-java-1.0.3.1 (August 2nd, 2011) 
  * Maintenance release (no significant change)
  * Refactoring code
  * Rebuild win32 binary

## snappy-java-1.0.3 (July 11st, 2011) 
  * Deprected SnappyException. Instead snappy-java uses IOException to issue
  errors. This change is necessary to support JNI code injection to a parent
  class loader.

## snappy-java-1.0.3-rc4 (June 27th, 2011) 
  * JNI interface injection so that multiple applications can share the native
  code. Issue 21

## snappy-java-1.0.3-rc3 (June 21st, 2011) 
  * Fixes issue 18, issue 19
  * Reduces memory footprint (contribution from Arec Wysoker)

## snappy-java-1.0.3-rc2 (June 7th, 2011) 
  * Fixes issue 17

## snappy-java-1.0.3-rc1 (June 4th, 2011) 
  * Upgrade to snappy-1.0.3 done.
  * libstdc++ embedding (only for Linux version)  done.
  * Minor bug fixes 

## snappy-java-1.0.1-rc4 (April 11th, 2011)
  * Primitive array support (e.g. `int[]`, `float[]`, etc.) issue 10
  * String compression/decompression 

## snappy-java-1.0.1-rc3 (April 4th, 2011) 
  * Running tests under win32/mac32, etc.

## snappy-java-1.0.1-rc2 (April 2nd, 2011) 
  * Adding `SnappyOutputStream` `SnappyInputStream`  issue 3


  * March 29th. Started snappy-java project
