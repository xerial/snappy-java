

SRC:=src/main/java
TARGET:=target


include $(SRC)/org/xerial/snappy/VERSION

SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz 

$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://snappy.googlecode.com/files/snappy-$(VERSION).tar.gz


$(TARGET)/snappy-$(VERSION): $(SNAPPY_ARCHIVE)
	tar xvfz $< -C $(TARGET)



$(SRC)/org/xerial/snappy/SnappyNative.h: $(SRC)/org/xerial/snappy/Snappy.java
	javah -classpath $(TARGET)/classes -o $@ org.xerial.snappy.Snappy


