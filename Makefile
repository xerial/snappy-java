
include Makefile.common

MVN:=mvn

all: snappy

SNAPPY_OUT:=$(TARGET)/$(snappy)-$(os_arch)
SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz 
SNAPPY_CC:=snappy-sinksource.cc snappy-stubs-internal.cc snappy.cc
SNAPPY_SRC_DIR:=$(TARGET)/snappy-$(VERSION)
SNAPPY_SRC:=$(addprefix $(SNAPPY_SRC_DIR)/,$(SNAPPY_CC))
SNAPPY_OBJ:=$(addprefix $(SNAPPY_OUT)/,$(patsubst %.cc,%.o,$(SNAPPY_CC)) SnappyNative.o)

SNAPPY_UNPACKED:=$(TARGET)/snappy-extracted.log

CXXFLAGS:=$(CXXFLAGS) -I$(SNAPPY_SRC_DIR)

$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://snappy.googlecode.com/files/snappy-$(VERSION).tar.gz

$(SNAPPY_UNPACKED): $(SNAPPY_ARCHIVE)
	tar xvfz $< -C $(TARGET)	
	touch $@

jni-header: $(SRC)/org/xerial/snappy/SnappyNative.h


$(SRC)/org/xerial/snappy/SnappyNative.h: $(SRC)/org/xerial/snappy/SnappyNative.java
	$(JAVAH) -classpath $(TARGET)/classes -o $@ org.xerial.snappy.SnappyNative

bytecode: src/main/resources/org/xerial/snappy/SnappyNativeLoader.bytecode

src/main/resources/org/xerial/snappy/SnappyNativeLoader.bytecode: src/main/resources/org/xerial/snappy/SnappyNativeLoader.java
	@mkdir -p $(TARGET)/temp
	$(JAVAC) -source 1.5 -target 1.5 -d $(TARGET)/temp $<
	cp $(TARGET)/temp/org/xerial/snappy/SnappyNativeLoader.class $@

$(SNAPPY_SRC): $(SNAPPY_UNPACKED)

$(SNAPPY_OUT)/%.o : $(SNAPPY_SRC_DIR)/%.cc
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c $< -o $@ 

$(SNAPPY_OUT)/SnappyNative.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp $(SRC)/org/xerial/snappy/SnappyNative.h  
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

snappy-jar-version:=snappy-java-$(shell $(JAVA) -jar lib/silk-weaver.jar find 'project(artifactId, version)' pom.xml | grep snappy-java | awk '{ print $$2; }')

native: $(SNAPPY_UNPACKED) $(NATIVE_DLL) 
snappy: native $(TARGET)/$(snappy-jar-version).jar

$(NATIVE_DLL): $(SNAPPY_OUT)/$(LIBNAME) 
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)


$(TARGET)/$(snappy-jar-version).jar: native $(NATIVE_DLL)
	$(MVN) package -Dmaven.test.skip=true

test: $(NATIVE_DLL)
	$(MVN) test

win32: 
	$(MAKE) native CXX=mingw32-g++ OS_NAME=Windows OS_ARCH=x86  

mac32: 
	$(MAKE) native OS_NAME=Mac OS_ARCH=i386

linux32:
	$(MAKE) native OS_NAME=Linux OS_ARCH=i386

clean-native-linux32:
	$(MAKE) clean-native OS_NAME=Linux OS_ARCH=i386

clean-native-win32:
	$(MAKE) clean-native OS_NAME=Windows OS_ARCH=x86

javadoc:
	$(MVN) javadoc:javadoc -DreportOutputDirectory=wiki/apidocs
