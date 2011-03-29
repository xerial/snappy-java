
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

snappy: $(SNAPPY_OUT)/$(LIBNAME)


$(SNAPPY_OUT)/%.o : $(SNAPPY_SRC)/%.cc
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/%.o : $(SRC)/org/xerial/snappy/SnappyNative.cpp
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(SNAPPY_OUT)/$(LIBNAME): $(SNAPPY_OBJ)
	$(CXX) $(CXXFLAGS) $(LINKFLAGS) -o $@ $* 
	$(STRIP) $@

clean-native: 
	rm -rf $(SNAPPY_OBJ) $(SNAPPY_OUT)/$(LIBNAME)


