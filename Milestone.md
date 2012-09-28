## Features under consideration 
  * `SnappyIndexer` for parallel compression/decompression
  * CUI commands (snap/unsnap)

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