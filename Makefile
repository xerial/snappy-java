
include Makefile.common

all: snappy

SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz 

$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://snappy.googlecode.com/files/snappy-$(VERSION).tar.gz


$(SNAPPY_SRC): $(SNAPPY_ARCHIVE)
	tar xvfz $< -C $(TARGET)


$(SRC)/org/xerial/snappy/SnappyNative.h: $(SRC)/org/xerial/snappy/Snappy.java
	javah -classpath $(TARGET)/classes -o $@ org.xerial.snappy.Snappy


SNAPPY_CC:=snappy-sinksource.cc snappy-stubs-internal.cc snappy.cc
SNAPPY_OBJ:=$(addprefix $(SNAPPY_OUT)/,$(patsubst %.cc,%.o,$(SNAPPY_CC)) SnappyNative.o)



$(SNAPPY_OUT)/%.o : $(SNAPPY_SRC)/%.cc $(SNAPPY_SRC)
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/%.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp $(SRC)/org/xerial/snappy/SnappyNative.h  $(SNAPPY_SRC)
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/$(LIBNAME): $(SNAPPY_OBJ)
	$(CXX) $(CXXFLAGS) $(LINKFLAGS) -o $@ $* 
	$(STRIP) $@

clean-native: 
	rm -rf $(SNAPPY_OBJ) $(SNAPPY_OUT)/$(LIBNAME)


NATIVE_DIR:=src/main/resources/org/xerial/snappy/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

snappy: $(NATIVE_DLL)

$(NATIVE_DLL): $(SNAPPY_OUT)/$(LIBNAME)
	@mkdir -p $(@D)
	cp $< $@


