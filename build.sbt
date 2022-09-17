Global / onChangedBuildSource := ReloadOnSourceChanges

name := "snappy-java"
organization := "org.xerial.snappy"
organizationName := "xerial.org"
description := "snappy-java: A fast compression/decompression library"

sonatypeProfileName := "org.xerial"
ThisBuild / publishTo := sonatypePublishToBundle.value
licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
homepage := Some(url("https://github.com/xerial/snappy-java"))
scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/xerial/snappy-java"),
    connection = "scm:git@github.com:xerial/snappy-java.git"
  )
)
developers := List(
  Developer(id = "leo", name = "Taro L. Saito", email = "leo@xerial.org", url = url("http://xerial.org/leo"))
)

ThisBuild / scalaVersion := "2.12.11"

// For building jars for JDK8
ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
Compile / compile / javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation")

doc / javacOptions := {
  val opts = Seq("-source", "1.8")
  if (scala.util.Properties.isJavaAtLeast("1.8"))
    opts ++ Seq("-Xdoclint:none")
  else
    opts
}

// Configuration for SnappyHadoopCompatibleOutputStream testing
Test / fork := true

val libTemp = {
  val path = s"${System.getProperty("java.io.tmpdir")}/snappy_test_${System.currentTimeMillis()}"
  // certain older Linux systems (debian/trusty in Travis CI) requires the libsnappy.so, loaded by
  // libhadoop.so, be copied to the temp path before the child JVM is forked.
  // because of that, cannot define as an additional task in Test scope
  IO.copyFile(file("src/test/resources/lib/Linux/libsnappy.so"), file(s"$path/libsnappy.so"))
  IO.copyFile(file("src/test/resources/lib/Linux/libsnappy.so"), file(s"$path/libsnappy.so.1"))
  path
}

val macOSXLibPath = s"$libTemp:${System.getenv("DYLD_LIBRARY_PATH")}"
val linuxLibPath  = s"$libTemp:${System.getenv("LD_LIBRARY_PATH")}"

// have to add to system dynamic library path since hadoop native library indirectly load libsnappy.1
// can't use javaOptions in Test because it causes the expression to eval twice yielding different temp path values
Test / envVars := Map("XERIAL_SNAPPY_LIB" -> libTemp, "DYLD_LIBRARY_PATH" -> macOSXLibPath, "LD_LIBRARY_PATH" -> linuxLibPath)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
Test / parallelExecution := false

autoScalaLibrary := false
crossPaths := false

libraryDependencies ++= Seq(
  "junit"               % "junit"              % "4.13.2" % "test",
  "org.codehaus.plexus" % "plexus-classworlds" % "2.4"    % "test",
  "org.xerial.java"     % "xerial-core"        % "2.1"    % "test",
  "org.wvlet.airframe" %% "airframe-log"       % "22.9.0" % "test",
  "org.osgi"            % "org.osgi.core"      % "4.3.0"  % "provided",
  "com.github.sbt"      % "junit-interface"    % "0.13.3" % "test",
  "org.apache.hadoop"   % "hadoop-common"      % "2.7.3"  % "test" exclude ("org.xerial.snappy", "snappy-java")
)

enablePlugins(SbtOsgi)

osgiSettings

OsgiKeys.exportPackage := Seq("org.xerial.snappy", "org.xerial.snappy.buffer", "org.xerial.snappy.pool", "org.xerial.snappy.pure")
OsgiKeys.bundleSymbolicName := "org.xerial.snappy.snappy-java"
OsgiKeys.bundleActivator := Option("org.xerial.snappy.SnappyBundleActivator")
OsgiKeys.importPackage := Seq("""org.osgi.framework;version="[1.5,2)"""")
OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))""""

OsgiKeys.additionalHeaders := Map(
  "Bundle-NativeCode" -> Seq(
    "org/xerial/snappy/native/Windows/x86_64/snappyjava.dll;osname=win32;processor=x86-64",
    "org/xerial/snappy/native/Windows/x86_64/snappyjava.dll;osname=win32;processor=x64",
    "org/xerial/snappy/native/Windows/x86_64/snappyjava.dll;osname=win32;processor=amd64",
    "org/xerial/snappy/native/Windows/x86/snappyjava.dll;osname=win32;processor=x86",
    "org/xerial/snappy/native/Mac/x86/libsnappyjava.jnilib;osname=macosx;processor=x86",
    "org/xerial/snappy/native/Mac/x86_64/libsnappyjava.dylib;osname=macosx;processor=x86-64",
    "org/xerial/snappy/native/Mac/aarch64/libsnappyjava.dylib;osname=macosx;processor=aarch64",
    "org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so;osname=linux;processor=x86-64",
    "org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so;osname=linux;processor=x64",
    "org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so;osname=linux;processor=amd64",
    "org/xerial/snappy/native/Linux/x86/libsnappyjava.so;osname=linux;processor=x86",
    "org/xerial/snappy/native/Linux/aarch64/libsnappyjava.so;osname=linux;processor=aarch64",
    "org/xerial/snappy/native/Linux/arm/libsnappyjava.so;osname=linux;processor=arm",
    "org/xerial/snappy/native/Linux/armv7/libsnappyjava.so;osname=linux;processor=arm_le",
    "org/xerial/snappy/native/Linux/ppc64/libsnappyjava.so;osname=linux;processor=ppc64le",
    "org/xerial/snappy/native/Linux/s390x/libsnappyjava.so;osname=linux;processor=s390x",
    "org/xerial/snappy/native/AIX/ppc/libsnappyjava.a;osname=aix;processor=ppc",
    "org/xerial/snappy/native/AIX/ppc64/libsnappyjava.a;osname=aix;processor=ppc64",
    "org/xerial/snappy/native/SunOS/x86/libsnappyjava.so;osname=sunos;processor=x86",
    "org/xerial/snappy/native/SunOS/x86_64/libsnappyjava.so;osname=sunos;processor=x86-64",
    "org/xerial/snappy/native/SunOS/sparc/libsnappyjava.so;osname=sunos;processor=sparc"
  ).mkString(","),
  "Bundle-DocURL"           -> "http://www.xerial.org/",
  "Bundle-License"          -> "http://www.apache.org/licenses/LICENSE-2.0.txt",
  "Bundle-ActivationPolicy" -> "lazy",
  "Bundle-Name"             -> "snappy-java: A fast compression/decompression library"
)

import ReleaseTransformations._

releaseTagName := { (ThisBuild / version).value }

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
