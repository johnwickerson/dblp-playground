MMDB=mmdb-2016-12-09
DBLP=http://dblp.org

run: dblp.xml dblp.dtd DblpExampleParser.class
	java -Xmx7g -cp $(MMDB).jar:. DblpExampleParser dblp.xml

dblp.xml.gz:
	wget $(DBLP)/xml/dblp.xml.gz

dblp.dtd:
	wget $(DBLP)/xml/dblp.dtd

$(MMDB).jar:
	wget $(DBLP)/src/$(MMDB).jar

$(MMDB)-javadoc.jar:
	wget $(DBLP)/src/$(MMDB)-javadoc.jar

dblp.xml: dblp.xml.gz
	gunzip -k dblp.xml.gz

DblpExampleParser.class: $(MMDB).jar DblpExampleParser.java
	javac -cp $(MMDB).jar DblpExampleParser.java

.PHONY: run clean doc

doc: $(MMDB)-javadoc.jar
	mkdir -p doc
	cd doc; jar -xvf ../$(MMDB)-javadoc.jar
	open doc/index.html

clean:
	rm -f DblpExampleParser.class
	rm -f dblp.dtd
	rm -f dblp.xml
	rm -f dblp.xml.gz
	rm -f $(MMDB).jar
	rm -f $(MMDB)-javadoc.jar
	rm -rf doc
