The snappy-java is a Java port of the snappy
<http://code.google.com/p/snappy/>, a fast C++ compresser/decompresser developed by Google.

## Features 
  * [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). Free for both commercial and non-commercial use.
  * Fast compression/decompression tailored to 64-bit CPU architecture. 
  * JNI-based implementation to achieve comparable performance to the native C++ version.  
     * Although snappy-java uses JNI, it can be used safely with multiple class loaders (e.g. Tomcat, etc.). 
  * Portable across various operating systems; Snappy-java contains native libraries built for Window/Mac/Linux (32/64-bit). At runtime, snappy-java loads one of these libraries according to your machine environment (It looks system properties, `os.name` and `os.arch`). 
  * Simple usage. Add the snappy-java-(version).jar file to your classpath. Then call compression/decompression methods in org.xerial.snappy.Snappy. 

## Performance 
  * Snappy's main target is very high-speed compression/decompression with reasonable compression size. So the compression ratio of snappy-java is modest and about the same as `LZF` (ranging 20%-100% according to the dataset).

  * Here are some [benchmark results](https://github.com/ning/jvm-compressor-benchmark/wiki), comparing
 snappy-java and the other compressors
 `LZO-java`/`LZF`/`QuickLZ`/`Gzip`/`Bzip2`. Thanks [Tatu Saloranta @cotowncoder](http://twitter.com/#!/cowtowncoder) for providing the benchmark suite. 
 * The benchmark result indicates snappy-java is the fastest compreesor/decompressor in Java:
    * <http://ning.github.com/jvm-compressor-benchmark/results/canterbury-roundtrip-2011-07-28/index.html>
 * The decompression speed is twice as fast as the others:
    * <http://ning.github.com/jvm-compressor-benchmark/results/canterbury-uncompress-2011-07-28/index.html>


## Download 
The current stable version is available from here:
  * Release version: http://code.google.com/p/snappy-java/downloads/list
     * [Release plans](https://github.com/xerial/snappy-java/blob/develop/Milestone.md) 
  * Snapshot version (the latest beta version): https://oss.sonatype.org/content/repositories/snapshots/org/xerial/snappy/snappy-java/
If you are a Maven user, see [pom.xml example](#using-with-maven).

## Usage 
First, import `org.xerial.snapy.Snappy` in your Java code:

     import org.xerial.snappy.Snappy;


Then use `Snappy.compress(byte[])` and `Snappy.uncompress(byte[])`:

     String input = "Hello snappy-java! Snappy-java is a JNI-based wrapper of "
     + "Snappy, a fast compresser/decompresser.";
     byte[] compressed = Snappy.compress(input.getBytes("UTF-8"));
     byte[] uncompressed = Snappy.uncompress(compressed);
     
     String result = new String(uncompressed, "UTF-8");
     System.out.println(result);


In addition, high-level methods (`Snappy.compress(String)`, `Snappy.compress(float[] ..)` etc. ) and low-level ones (e.g. `Snappy.rawCompress(.. )`,  `Snappy.rawUncompress(..)`, etc.), which minimize memory copies, can be used. See also 
[Snappy.java](https://github.com/xerial/snappy-java/blob/master/src/main/java/org/xerial/snappy/Snappy.java)

### Stream-based API
Stream-based compressor/decompressor `SnappyOutputStream`/`SnappyInputStream` are also available for reading/writing large data sets.

### Setting classpath
If you have snappy-java-(VERSION).jar in the current directory, use `-classpath` option as follows:

    $ javac -classpath ".;snappy-java-(VERSION).jar" Sample.java  # in Windows
    or 
    $ javac -classpath ".:snappy-java-(VERSION).jar" Sample.java  # in Mac or Linux


### Using with Maven
  * Snappy-java is available from Maven's central repository:  <http://repo1.maven.org/maven2/org/xerial/snappy/snappy-java>

Add the following dependency to your pom.xml:

    <dependency>
      <groupId>org.xerial.snappy</groupId>
      <artifactId>snappy-java</artifactId>
      <version>(version)</version>
      <type>jar</type>
      <scope>compile</scope>
    </dependency>


## Public discussion group
Post bug reports or feature request to the Issue Tracker: <https://github.com/xerial/snappy-java/issues>

Public discussion forum is here: <http://groups.google.com/group/xerial?hl=en Xerial Public Discussion Group>


## Building from the source code 
See the [installation instruction](https://github.com/xerial/snappy-java/blob/develop/INSTALL). Building from the source code is an option when your OS platform and CPU architecture is not supported. To build snappy-java, you need Git, JDK (1.6 or higher), Maven (3.x or higher is required), g++ compiler (mingw in Windows) etc.

    $ git clone https://github.com/xerial/snappy-java.git
    $ cd snappy-java
    $ make
    

A file `target/snappy-java-$(version).jar` is the product additionally containing the native library built for your platform.

## Cross-compiling for other platforms
The Makefile contains rules for cross-compiling the native library for other platforms so that the snappy-java JAR can support multiple platforms. For example, to build the native libraries for x86 Linux, x86 and x86-64 Windows, and soft- and hard-float ARM:

    $ make linux32 win32 win64 linux-arm linux-armhf

If you append `snappy` to the line above, it will also build the native library for the current platform and then build the snappy-java JAR (containing all native libraries built so far).

Of course, you must first have the necessary cross-compilers and development libraries installed for each target CPU and OS. For example, on Ubuntu 12.04 for x86-64, install the following packages for each target:

  * linux32: `sudo apt-get install g++-multilib libc6-dev-i386 lib32stdc++6`
  * win32: `sudo apt-get install g++-mingw-w64-i686`
  * win64: `sudo apt-get install g++-mingw-w64-x86-64`
  * arm: `sudo apt-get install g++-arm-linux-gnueabi`
  * armhf: `sudo apt-get install g++-arm-linux-gnueabihf`

Unfortunately, cross-compiling for Mac OS X is not currently possible; you must compile within OS X.

If you are using Mac and openjdk7 (or higher), use the following option:

    $ make native LIBNAME=libsnappyjava.dylib

## Miscellaneous Notes
### Using snappy-java with Tomcat 6 (or higher) Web Server

Simply put the snappy-java's jar to WEB-INF/lib folder of your web application. Usual JNI-library specific problem no longer exists since snappy-java version 1.0.3 or higher can be loaded by multiple class loaders in the same JVM by using native code injection to the parent class loader (Issue 21). 

----
Snappy-java is developed by [Taro L. Saito](http://www.xerial.org/leo). Twitter  [@taroleo](http://twitter.com/#!/taroleo)
