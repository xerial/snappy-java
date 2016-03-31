
include Makefile.common

MVN:=mvn
SBT:=./sbt

all: snappy

SNAPPY_OUT:=$(TARGET)/$(snappy)-$(os_arch)
SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz
SNAPPY_CC:=snappy-sinksource.cc snappy-stubs-internal.cc snappy.cc
SNAPPY_SRC_DIR:=$(TARGET)/snappy-$(VERSION)
SNAPPY_SRC:=$(addprefix $(SNAPPY_SRC_DIR)/,$(SNAPPY_CC))
SNAPPY_GIT_REPO_URL:=https://github.com/google/snappy
SNAPPY_GIT_REV:=2b9152d9c5bed71dffb7f7f6c7a3ec48b058ff2d # 1.1.3 with autogen.sh fix
SNAPPY_UNPACKED:=$(TARGET)/snappy-extracted.log
SNAPPY_GIT_UNPACKED:=$(TARGET)/snappy-git-extracted.log

# TODO: Upgrade to a stable release
BITSHUFFLE_VERSION:=55f9b4caec73fa21d13947cacea1295926781440
BITSHUFFLE_ARCHIVE:=$(TARGET)/bitshuffle-$(BITSHUFFLE_VERSION).tar.gz
BITSHUFFLE_C:=bitshuffle_core.c iochain.c
BITSHUFFLE_SRC_DIR:=$(TARGET)/bitshuffle-$(BITSHUFFLE_VERSION)/src
BITSHUFFLE_SRC:=$(addprefix $(BITSHUFFLE_SRC_DIR)/,$(BITSHUFFLE_C))
BITSHUFFLE_UNPACKED:=$(TARGET)/bitshuffle-extracted.log

ifdef USE_GIT
  ifndef GIT_REPO_URL
    $(warning GIT_REPO_URL is not set when using git)
  endif
  ifndef GIT_SNAPPY_BRANCH
    $(warning GIT_SNAPPY_BRANCH is not set when using git)
  endif
endif

$(BITSHUFFLE_ARCHIVE):
	@mkdir -p $(@D)
	curl -L -o$@ https://github.com/kiyo-masui/bitshuffle/archive/$(BITSHUFFLE_VERSION).tar.gz

$(BITSHUFFLE_UNPACKED): $(BITSHUFFLE_ARCHIVE)
	$(TAR) xvfz $< -C $(TARGET)
	touch $@

$(BITSHUFFLE_SRC): $(BITSHUFFLE_UNPACKED)

$(SNAPPY_OUT)/%.o : $(BITSHUFFLE_SRC_DIR)/%.c
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c $< -o $@

SNAPPY_OBJ:=$(addprefix $(SNAPPY_OUT)/,$(patsubst %.cc,%.o,$(SNAPPY_CC)) $(patsubst %.c,%.o,$(BITSHUFFLE_C)) SnappyNative.o)

ifndef UNIVERSAL_BITSHUFFLE
  # Undefined macros to generate a platform-independent binary
  CXXFLAGS:=$(CXXFLAGS) -U__AVX2__ -U__SSE2__  -I$(SNAPPY_SRC_DIR) -I$(BITSHUFFLE_SRC_DIR)
else
  CXXFLAGS:=$(CXXFLAGS) -I$(SNAPPY_SRC_DIR) -I$(BITSHUFFLE_SRC_DIR)
endif

ifeq ($(OS_NAME),SunOS)
	TAR:= gtar
else
	TAR:= tar
endif

$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -L -o$@ https://github.com/google/snappy/releases/download/$(VERSION)/snappy-$(VERSION).tar.gz

$(SNAPPY_UNPACKED): $(SNAPPY_ARCHIVE)
	$(TAR) xvfz $< -C $(TARGET)
	touch $@
	cd  $(SNAPPY_SRC_DIR) && ./configure

$(SNAPPY_GIT_UNPACKED):
	rm -rf $(SNAPPY_SRC_DIR)
	@mkdir -p $(SNAPPY_SRC_DIR)
	git clone $(SNAPPY_GIT_REPO_URL) $(SNAPPY_SRC_DIR)
	git --git-dir=$(SNAPPY_SRC_DIR)/.git --work-tree=$(SNAPPY_SRC_DIR) checkout -b local/snappy-$(VERSION) $(SNAPPY_GIT_REV)
	cd $(SNAPPY_SRC_DIR) && ./autogen.sh && ./configure
	touch $@

jni-header: $(SRC)/org/xerial/snappy/SnappyNative.h $(SRC)/org/xerial/snappy/BitShuffleNative.h

$(TARGET)/jni-classes/org/xerial/snappy/SnappyNative.class : $(SRC)/org/xerial/snappy/SnappyNative.java
	@mkdir -p $(TARGET)/jni-classes
	$(JAVAC) -source 1.6 -target 1.6 -d $(TARGET)/jni-classes -sourcepath $(SRC) $<

$(SRC)/org/xerial/snappy/SnappyNative.h: $(TARGET)/jni-classes/org/xerial/snappy/SnappyNative.class
	$(JAVAH) -force -classpath $(TARGET)/jni-classes -o $@ org.xerial.snappy.SnappyNative


$(TARGET)/jni-classes/org/xerial/snappy/BitShuffleNative.class : $(SRC)/org/xerial/snappy/BitShuffleNative.java
	@mkdir -p $(TARGET)/jni-classes
	$(JAVAC) -source 1.6 -target 1.6 -d $(TARGET)/jni-classes -sourcepath $(SRC) $<

$(SRC)/org/xerial/snappy/BitShuffleNative.h: $(TARGET)/jni-classes/org/xerial/snappy/BitShuffleNative.class
	$(JAVAH) -force -classpath $(TARGET)/jni-classes -o $@ org.xerial.snappy.BitShuffleNative

$(SNAPPY_SRC): $(SNAPPY_GIT_UNPACKED)

$(SNAPPY_OUT)/%.o : $(SNAPPY_SRC_DIR)/%.cc
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c $< -o $@

#$(SNAPPY_OUT)/SnappyNative.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp $(SRC)/org/xerial/snappy/SnappyNative.h $(SRC)/org/xerial/snappy/BitShufflenative.h
$(SNAPPY_OUT)/SnappyNative.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp jni-header
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(SNAPPY_OUT)/$(LIBNAME): $(SNAPPY_OBJ)
	$(CXX) $(CXXFLAGS) -o $@ $+ $(LINKFLAGS)
	$(STRIP) $@

clean-native:
	rm -rf $(SNAPPY_OUT)

clean:
	rm -rf $(TARGET)

NATIVE_DIR:=src/main/resources/org/xerial/snappy/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/org/xerial/snappy/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

snappy-jar-version:=snappy-java-$(shell perl -npe "s/version in ThisBuild\s+:=\s+\"(.*)\"/\1/" version.sbt | sed -e "/^$$/d")

native: $(SNAPPY_GIT_UNPACKED) $(NATIVE_DLL)
snappy: native $(TARGET)/$(snappy-jar-version).jar

$(NATIVE_DLL): $(SNAPPY_OUT)/$(LIBNAME)
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)


package: $(TARGET)/$(snappy-jar-version).jar

$(TARGET)/$(snappy-jar-version).jar:
	$(SBT) package

test: $(NATIVE_DLL)
	$(SBT) test

win32:
	$(MAKE) native CROSS_PREFIX=i686-w64-mingw32- OS_NAME=Windows OS_ARCH=x86

# for cross-compilation on Ubuntu, install the g++-mingw-w64-x86-64 package
win64:
	$(MAKE) native CROSS_PREFIX=x86_64-w64-mingw32- OS_NAME=Windows OS_ARCH=x86_64

mac32:
	$(MAKE) native OS_NAME=Mac OS_ARCH=x86

linux32:
	$(MAKE) native OS_NAME=Linux OS_ARCH=x86

freebsd64:
	$(MAKE) native OS_NAME=FreeBSD OS_ARCH=x86_64

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabi package
linux-arm:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabi- OS_NAME=Linux OS_ARCH=arm

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabihf package
linux-armhf:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armhf

# for cross-compilation on Ubuntu, install the g++-aarch64-linux-gnu
linux-aarch64:
	$(MAKE) native CROSS_PREFIX=aarch64-linux-gnu- OS_NAME=Linux OS_ARCH=aarch64

clean-native-linux32:
	$(MAKE) clean-native OS_NAME=Linux OS_ARCH=x86

clean-native-win32:
	$(MAKE) clean-native OS_NAME=Windows OS_ARCH=x86

javadoc:
	$(SBT) doc

install-m2:
	$(SBT) publishM2

googlecode-upload: googlecode-lib-upload googlecode-src-upload

googlecode-lib-upload: $(TARGET)/snappy-java-$(VERSION)-lib.upload
googlecode-src-upload: $(TARGET)/snappy-java-$(VERSION)-src.upload

GOOGLECODE_USER:=leo@xerial.org

$(TARGET)/snappy-java-$(VERSION)-lib.upload:
	./googlecode_upload.py -s "library for all platforms" -p snappy-java -l "Type-Executable,Featured,OpSys-All" -u "$(GOOGLECODE_USER)" target/snappy-java-$(VERSION).jar
	touch $@

$(TARGET)/snappy-java-$(VERSION)-src.upload:
	./googlecode_upload.py -s "source code archive" -p snappy-java -l "Type-Source,OpSys-All" -u "$(GOOGLECODE_USER)" target/snappy-java-$(VERSION).tar.gz
	touch $@

