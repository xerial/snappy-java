

SRC:=src/main/java
TARGET:=target


include $(SRC)/org/xerial/snappy/VERSION

SNAPPY_ARCHIVE:=$(TARGET)/snappy-$(VERSION).tar.gz 

$(SNAPPY_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://snappy.googlecode.com/files/snappy-$(VERSION).tar.gz


$(TARGET)/snappy-$(VERSION): $(SNAPPY_ARCHIVE)
	tar xvfz $< -C $(TARGET)
