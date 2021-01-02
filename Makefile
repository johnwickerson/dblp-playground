# Author: John Wickerson
# Date: 02-January-2021

all: dblp.xml dblp.dtd
	./build_db.sh

clean:
	echo "Deleting existing database"
	dropdb --if-exists dblp
	echo "Delete existing TSV files"
	rm -f papers*.tsv
	rm -f authors*.tsv
	rm -f writtenBy*.tsv
	rm -f editedBy*.tsv
	rm -rf tsv

dblp.xml.gz:
	echo "Downloading XML dump from DBLP"
	wget http://dblp.org/xml/dblp.xml.gz

dblp.xml: dblp.xml.gz
	echo "Unzipping XML dump"
	gunzip -k dblp.xml.gz

dblp.dtd:
	wget http://dblp.org/xml/dblp.dtd

test:
	psql dblp -c "SELECT publYear, COUNT(publYear) FROM papers WHERE publYear BETWEEN 1995 AND 2005 GROUP BY publYear ORDER BY publYear"

myqueries:
	echo "Most POPL papers:"
	FIRST=5 CONF=\'POPL\' make countMostPapers
	echo "Most PLDI papers:"
	FIRST=5 CONF=\'PLDI\' make countMostPapers
	echo "Most POPL and PLDI papers:"
	FIRST=5 CONF=\'PLDI\',\'POPL\' make countMostPapers
	echo "Longest POPL holiday:"
	FIRST=5 CONF=POPL make findHolidays
	echo "Longest PLDI holiday:"
	FIRST=5 CONF=PLDI make findHolidays
	echo "Longest POPL career:"
	FIRST=5 CONF=POPL make findLongestCareer
	echo "Longest PLDI career:"
	FIRST=5 CONF=PLDI make findLongestCareer
	echo "Longest POPL streak:"
	FIRST=3 CONF=POPL make findStreaks
	echo "Longest PLDI streak:"
	FIRST=3 CONF=PLDI make findStreaks
	echo "Most POPL papers per conference:"
	FIRST=2 CONF=POPL make findMostPapersPerConf
	echo "Most PLDI papers per conference:"
	FIRST=2 CONF=PLDI make findMostPapersPerConf

firstalpha: #invoke like `CONF=POPL make firstalpha`
	psql dblp -c "SELECT title, publyear FROM fullpapers WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) ORDER BY title ASC"

lastalpha: #invoke like `CONF=POPL make lastalpha`
	psql dblp -c "SELECT title, publyear FROM fullpapers WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) ORDER BY title DESC"

papersFor: #invoke like `CONF=POPL AUTHOR="John Wickerson" make papersFor`
	psql dblp -c "SELECT title, publyear FROM fullpaperswithauthoraliases WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND fullname = '$(AUTHOR)' ORDER BY publYear DESC"

recentPapersFor: #invoke like `CONF=POPL AUTHOR="Alex Aiken" make recentPapersFor`
	psql dblp -c "SELECT title, publyear FROM fullpaperswithauthoraliases WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND fullname = '$(AUTHOR)' AND publYear BETWEEN 2015 AND 2020 ORDER BY publYear DESC"

countCorePapers: #invoke like `CONF=POPL AREA=PLDI make countCorePapers`
	psql dblp -c "DROP TABLE if EXISTS corepeople"
	psql dblp -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql dblp -c "COPY corePeople FROM '$$DIR/core_people.tsv' delimiter E'\t'"
	psql dblp -c "SELECT corepeople.fullname, count(publyear) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)' GROUP BY corepeople.fullname"

countCoreConfs: #invoke like `CONF=POPL AREA=PLDI make countCoreConfs`
	psql dblp -c "DROP TABLE if EXISTS corepeople"
	psql dblp -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql dblp -c "COPY corePeople FROM '$$DIR/core_people.tsv' delimiter E'\t'"
	psql dblp -c "SELECT corepeople.fullname, count(DISTINCT publyear) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)' GROUP BY corepeople.fullname"

countUniqueCorePapers: #invoke like `CONF=ICFP AREA=TOP make countUniqueCorePapers`
	psql dblp -c "DROP TABLE if EXISTS corepeople"
	psql dblp -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql dblp -c "COPY corePeople FROM '$$DIR/core_people.tsv' delimiter E'\t'"
	psql dblp -c "SELECT count(DISTINCT pid) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)'"

countMostPapers: #invoke like `FIRST=5 CONF=\'PLDI\',\'POPL\' make countMostPapers`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(publyear) DESC) as rank, fullname, count(publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle IN ($(CONF))) OR (etype = 'article' AND volnumber IN ($(CONF)))) GROUP BY fullname ORDER BY count(publyear) DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

countMostConfs: #invoke like `FIRST=5 CONF=PLDI make countMostConfs`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(DISTINCT publyear) DESC) as rank, fullname, count(DISTINCT publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY count(publyear) DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findLongestCareer: #invoke like `FIRST=5 CONF=PLDI make findLongestCareer`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY max(publyear) - min(publyear) DESC) as rank, fullname, min(publyear), max(publyear), max(publyear) - min(publyear) as span FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY span DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findMostPapersPerConf: #invoke like `FIRST=2 CONF=PLDI make findMostPapersPerConf`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(publyear) DESC) as rank, fullname, publyear, count(publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname, publyear ORDER BY count(publyear) DESC, fullname ASC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

# following query inspired by: https://dba.stackexchange.com/a/239622
findStreaks: #invoke like `FIRST=3 CONF=PLDI make findStreaks`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY endpoint - publyear + 1 DESC) as rank, fullname, publyear AS first, endpoint AS last, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint IS NOT null ORDER BY streaklength DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findOngoingStreaks: #invoke like `FIRST=3 CONF=PLDI CURRENTYEAR=2020 make findOngoingStreaks`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY endpoint - publyear + 1 DESC) as rank, fullname, publyear AS first, endpoint AS last, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint = $(CURRENTYEAR) ORDER BY streaklength DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findHolidays: #invoke like `FIRST=5 CONF=POPL make findHolidays`
	psql dblp -c "WITH papers_of AS (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY lastyear.publyear - firstyear.publyear DESC) as rank,firstyear.fullname, firstyear.publyear as firstyear, lastyear.publyear as lastyear, (lastyear.publyear - firstyear.publyear) as gap FROM papers_of AS firstyear INNER JOIN papers_of AS lastyear ON firstyear.fullname = lastyear.fullname AND firstyear.publyear < lastyear.publyear LEFT JOIN papers_of AS intermediateyear ON firstyear.fullname = intermediateyear.fullname AND firstyear.publyear < intermediateyear.publyear AND intermediateyear.publyear < lastyear.publyear WHERE intermediateyear.fullname IS null ORDER BY gap DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"
