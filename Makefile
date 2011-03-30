
include Makefile.common

MVN:=mvn

all: snappy

SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz 
SNAPPY_CC:=snappy-sinksource.cc snappy-stubs-internal.cc snappy.cc
SNAPPY_OBJ:=$(addprefix $(SNAPPY_OUT)/,$(patsubst %.cc,%.o,$(SNAPPY_CC)) SnappyNative.o)


$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://snappy.googlecode.com/files/snappy-$(VERSION).tar.gz


$(TARGET)/snappy-$(VERSION): $(SNAPPY_ARCHIVE)
	tar xvfz $< -C $(TARGET)


jni-header: $(SRC)/org/xerial/snappy/SnappyNative.h

$(SRC)/org/xerial/snappy/SnappyNative.h: $(SRC)/org/xerial/snappy/SnappyNative.java
	$(JAVAH) -classpath $(TARGET)/classes -o $@ org.xerial.snappy.SnappyNative


$(SNAPPY_OUT)/%.o : $(TARGET)/snappy-$(VERSION)/%.cc 
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/%.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp $(SRC)/org/xerial/snappy/SnappyNative.h  
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/$(LIBNAME): $(SNAPPY_OBJ)
	$(CXX) $(CXXFLAGS) $(LINKFLAGS) $+ -o $@ 
	$(STRIP) $@

clean-native: 
	rm -rf $(SNAPPY_OBJ) $(SNAPPY_OUT)/$(LIBNAME)


NATIVE_DIR:=src/main/resources/org/xerial/snappy/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/org/xerial/snappy/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

snappy-jar-version:=snappy-$(shell $(JAVA) -jar lib/silk-weaver.jar find 'project(artifactId, version)' pom.xml | grep snappy-java | awk '{ print $$2; }')

native: $(NATIVE_DLL) 
snappy: $(TARGET)/$(snappy-jar-version).jar

$(NATIVE_DLL): $(SNAPPY_OUT)/$(LIBNAME) $(TARGET)/snappy-$(VERSION)
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)


$(TARGET)/$(snappy-jar-version).jar: $(NATIVE_DLL)
	$(MVN) package



win32: 
	$(MAKE) native CXX=mingw32-g++ OS_NAME=Windows OS_ARCH=x86  
