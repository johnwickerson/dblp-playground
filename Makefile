MMDB=mmdb-2016-12-09.jar

run: dblp.xml dblp.dtd DblpExampleParser.class
	java -cp $(MMDB):. DblpExampleParser dblp.xml

dblp.xml.gz:
	wget http://dblp.org/xml/dblp.xml.gz

dblp.dtd:
	wget http://dblp.org/xml/dblp.dtd

$(MMDB):
	wget http://dblp.org/src/$(MMDB)

dblp.xml: dblp.xml.gz
	gunzip -k dblp.xml.gz

DblpExampleParser.class: $(MMDB) DblpExampleParser.java
	javac -cp $(MMDB) DblpExampleParser.java

.PHONY: run clean deepclean

clean:
	rm -f DblpExampleParser.class
	rm -f dblp.dtd
	rm -f dblp.xml
	rm -f dblp.xml.gz
	rm -f $(MMDB)
