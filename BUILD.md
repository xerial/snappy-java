# Notes for building snappy-java

snappy-java supports Windows, Mac OS X, Linux (x86, x86_64, arm, etc...). If your platform is not supported, you need to build native libraries by yourself.
 
## Requited Tools
 - Java 7 or higher
 - Maven3 (mvn)
 - GNU make, autotools


## Building snappy-java

To build jar file of snappy-java, type:
```
$ make 
```
A native library for your machine environment and a jar package target/snappy-java-(version).jar are produced in the target folder.


### Rebuild the native library for your platform
```
$ make clean-native native
```

## Platform specific tips

After snappy-java 1.1.3, we are using docker images of cross compilers. So no longer need to build native libraries by actually running the target OS.
The following notes are obsolete, but preserved here for future references.

### Windows (32/64-bit)
* GNU make 
* And also tar, curl, cp, rm, grep commands are needed. (I use Cygwin and MinGW for building snappy-java in Windows)

### Windows (32-bit only)
* Install MinGW http://www.mingw.org/
* Set PATH to the following command in MinGW package
 - mingw32-g++
 - strip

To build x86 (32bit) dll under 64-bit Windows, use "make win32" target.

### Windows (64-bit only)
* Download MinGW-w64 http://sourceforge.net/projects/mingw-w64/ 
* Set PATH to the following commands in the downloaded archive: 
 - x86_64-w64-mingw32-g++
 - x86_64-w64-mingw32-strip

NOTICE: Do not use the Cygwin version of MinGW-w64. It fails to build assemblies for 64bit environment.

### Linux (32/64-bit)
* gcc-4.5.x or higher is necessary because snappy-java uses -static-libstdc++ option. It is possible to use gcc-4.3.x but a dependency to libstdc++ remains in the generated jar file; That means if another version of libstdc++ is used, snappy-java might not work correctly.
* You can build 32-bit native library with 64-bit Linux machine (do make linux32)

### Mac
* Install gcc, make, etc. included in Mac OS X install disk. (X Code). And also intall libtool:

```
$ brew install libtool
```

## Building Linux x86\_64 binary 

(obsolete: snappy-java now uses a docker image `xerial/centos5-linux-x86_86-pic` which contains g++ built with `-fPIC` option. )

snappy-java tries to static link libstdc++ to increase the availability for various Linux versions. However, standard distributions of 64-bit Linux OS rarely provide libstdc++ compiled with `-fPIC` option. I currently uses custom g++, compiled as follows:

```
$ cd work
$ wget (gcc-4.8.3 source)
$ tar xvfz (gcc-4.8.3.tar.gz)
$ cd gcc-4.8.3
$ ./contrib/download_prerequisites
$ cd ..
$ mkdir objdir
$ cd objdir
$ ../gcc-4.8.3/configure --prefix=$HOME/local/gcc-4.8.3 CXXFLAGS=-fPIC CFLAGS=-fPIC --enable-languages=c,c++
$ make
$ make install
```

This g++ build enables static linking of libstdc++. For more infomation on building GCC, see GCC's home page.

## Building Linux s390/s390x binaries

Older snapshots of snappy contain a buggy config.h.in that does not work properly on some big-endian platforms like Linux on IBM z (s390/s390x). Building snappy-java on s390/s390x requires fetching the snappy source from GitHub, and processing the source with autoconf to obtain a usable config.h. On a RHEL s390x system, these steps produced a working 64-bit snappy-java build (the process should be similar for other distributions):

	$ sudo yum install java-1.7.1-ibm-devel libstdc++-static-devel
	$ export JAVA_HOME=/usr/lib/jvm/java-1.7.1-ibm-1.7.1.2.10-1jpp.3.el7_0.s390x
	$ make USE_GIT=1 GIT_REPO_URL=https://github.com/google/snappy.git GIT_SNAPPY_BRANCH=master IBM_JDK_7=1

## Activating SSE2/AVX2 instructions in BitShuffle

The most of the native libraries that snappy-java contains disable SSE2/AVX2 instructions in terms of portability (SSE2 is enabled only in Linux/x86_64 platforms). To enable AVX2 instructions, you need to compile as follows:

	$ make CXXFLAGS_BITSHUFFLE=-mavx2  # -msse2 for SSE2 instructions

## Cross-compiling for other platforms (obsolete)
The Makefile contains rules for cross-compiling the native library for other platforms so that the snappy-java JAR can support multiple platforms. For example, to build the native libraries for x86 Linux, x86 and x86-64 Windows, and soft- and hard-float ARM:

    $ make linux32 win32 win64 linux-arm linux-armhf linux-aarch64

If you append `snappy` to the line above, it will also build the native library for the current platform and then build the snappy-java JAR (containing all native libraries built so far).

Of course, you must first have the necessary cross-compilers and development libraries installed for each target CPU and OS. For example, on Ubuntu 12.04 for x86-64, install the following packages for each target:

  * linux32: `sudo apt-get install g++-multilib libc6-dev-i386 lib32stdc++6`
  * win32: `sudo apt-get install g++-mingw-w64-i686`
  * win64: `sudo apt-get install g++-mingw-w64-x86-64`
  * arm: `sudo apt-get install g++-arm-linux-gnueabi`
  * armhf: `sudo apt-get install g++-arm-linux-gnueabihf`
  * aarch64: `sudo apt-get install g++-aarch64-linux`

Unfortunately, cross-compiling for Mac OS X is not currently possible; you must compile within OS X.

If you are using Mac and openjdk7 (or higher), use the following option:

    $ make native LIBNAME=libsnappyjava.dylib


