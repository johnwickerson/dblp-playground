# Author: unknown
# Retrieved from http://agdb.informatik.uni-bremen.de/dblp/statistics.php

import xml.sax
import codecs
import sys

xml_path = sys.argv[1]

tl = ['article', 'inproceedings', 'proceedings', 'book', 'incollection', 'phdthesis', 'mastersthesis', 'www']
at = ['title', 'booktitle', 'pages', 'year', 'address', 'journal', 'volume', 'number', 'month', 'url', 'ee', 'cdrom', 'cite', 'publisher', 'note', 'crossref', 'isbn', 'series', 'school', 'chapter']
sl = ['mdate', 'publtype', 'reviewid', 'rating', 'key']
pages = ['begin', 'end', 'numpages']

csvfields = ['etype'] + at + sl + pages
csvlengths = {}
for f in csvfields:
    csvlengths[f] = 0
for f in tl:
    csvlengths[f] = 0
csvlengths['author'] = 0
csvlengths['editor'] = 0

writtenBy = codecs.open("writtenBy.tsv", "w", "utf-8")
papers  = codecs.open("papers.tsv",  "w", "utf-8")
authors = codecs.open("authors.tsv", "w", "utf-8")
editedBy = codecs.open("editedBy.tsv", "w", "utf-8")
lengths = codecs.open("lengths.tsv", "w", "utf-8")

authorFirstNameLength = 0
authorLastNameLength = 0

class DBLPXMLHANDLER(xml.sax.ContentHandler):
    cfields = {}
    distAuthors = {}
    cval = ""
    paperCounter = 0
    authorCounter = 0
    authorID = 0

    def startElement(self, name, attrs):
        if name in tl:
            self.cfields.clear()
            self.cval = ""
            self.cfields['anum'] = 1
            self.cfields['etype'] = name
            for s in tl:
                self.cfields[s] = '\N'
            for s in at:
                self.cfields[s] = '\N'
            for s in sl:
                self.cfields[s] = '\N'
            for s in pages:
                self.cfields[s] = '\N'
            for (k, v) in attrs.items():
                self.cfields[k] = v

        if name in ['author'] + csvfields:
            self.cval = ""
        if name in ['editor'] + csvfields:
            self.cval = ""

    def characters(self, content):
        self.cval = self.cval + content

    def endElement(self, name):
        if name in (tl + csvfields) and not self.cval.isdigit() and csvlengths[name] < len(self.cval):
            csvlengths[name] = len(self.cval)

        #editors and authors share the same tsv, but not the same writtenBy/ editedBy 
        if name == 'author' or name == 'editor':
            global authorFirstNameLength
            global authorLastNameLength
            if self.cval in self.distAuthors:
                authorID = self.distAuthors[self.cval]
            else:
                self.distAuthors[self.cval] = self.authorCounter;
                authorID = self.authorCounter;
                self.authorCounter += 1
                authorName = self.cval.split()
                authorFirstName =""
                for x in xrange(len(authorName) - 1):
                    authorFirstName += authorName[x] 
                    if x<(len(authorName)-1):
                        authorFirstName += " "
                authorLastName = authorName[len(authorName) - 1]
                if authorFirstName is " ":
                    authorFirstName = "\N"
                if len(authorFirstName) > authorFirstNameLength:
                    authorFirstNameLength = len(authorFirstName)
                if len(authorLastName) > authorLastNameLength:
                    authorLastNameLength = len(authorLastName)
                authors.write(str(authorID) + "\t" + self.cval + "\t" + authorFirstName + "\t" + authorLastName + "\n")
            if name == 'author':
                writtenBy.write(str(self.paperCounter) + "\t" + str(authorID) + "\t" + str(self.cfields['anum']).encode("utf-8").decode("utf-8") + "\n")
                self.cfields['anum'] = self.cfields['anum'] + 1
            else: #name == 'editor'
                editedBy.write(str(self.paperCounter) + "\t" + str(authorID) + "\t" + str(self.cfields['anum']).encode("utf-8").decode("utf-8") + "\n")
                self.cfields['anum'] = self.cfields['anum'] + 1

        if name in at:
            if name == 'pages':
                pageArray = self.cval.split('-')
                if len(pageArray) == 2:
                    pageFromArray = pageArray[0].split(':')
                    pageFrom = pageFromArray[len(pageFromArray) - 1]
                    pageToArray = pageArray[1].split(':')
                    pageTo = pageToArray[len(pageToArray) - 1]
                    if pageFrom.isdigit() and pageTo.isdigit():
                        self.cfields['begin'] = pageFrom
                        self.cfields['end'] = pageTo
                    
            self.cfields[name] = self.cval

        if name in tl:
            line = []
            for f in csvfields:
                line.append(self.cfields.get(f, ''))
            papers.write('\t'.join(line))
            papers.write('\t' + str(self.paperCounter))
            self.paperCounter = self.paperCounter + 1
            papers.write('\n')

parser = xml.sax.make_parser()
parser.setContentHandler(DBLPXMLHANDLER())
parser.parse(open(xml_path, "r"))


for key in csvlengths:
    lengths.write(key + "\t" + str(csvlengths.get(key, '')) + "\n")
lengths.write("FirstName\t" + str(authorFirstNameLength) + "\n")
lengths.write("LastName\t" + str(authorLastNameLength) + "\n")

papers.close()
authors.close()
writtenBy.close()
editedBy.close()
lengths.close()
