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
	echo "Most ICFP papers:"
	FIRST=5 CONF=\'ICFP\' make countMostPapers
	echo "Most OOPSLA papers:"
	FIRST=5 CONF=\'OOPSLA\' make countMostPapers
	echo "Most POPL, PLDI, ICFP, and OOPSLA papers:"
	FIRST=5 CONF=\'PLDI\',\'POPL\',\'ICFP\',\'OOPSLA\' make countMostPapers
	echo "Longest POPL holiday:"
	FIRST=5 CONF=POPL make findHolidays
	echo "Longest PLDI holiday:"
	FIRST=5 CONF=PLDI make findHolidays
	echo "Longest ICFP holiday:"
	FIRST=5 CONF=ICFP make findHolidays
	echo "Longest OOPSLA holiday:"
	FIRST=5 CONF=OOPSLA make findHolidays
	echo "Longest POPL career:"
	FIRST=5 CONF=POPL make findLongestCareer
	echo "Longest PLDI career:"
	FIRST=5 CONF=PLDI make findLongestCareer
	echo "Longest ICFP career:"
	FIRST=5 CONF=ICFP make findLongestCareer
	echo "Longest OOPSLA career:"
	FIRST=5 CONF=OOPSLA make findLongestCareer
	echo "Longest POPL streak:"
	FIRST=3 CONF=POPL make findStreaks
	echo "Longest PLDI streak:"
	FIRST=3 CONF=PLDI make findStreaks
	echo "Longest ICFP streak:"
	FIRST=3 CONF=ICFP make findStreaks
	echo "Longest OOPSLA streak:"
	FIRST=3 CONF=OOPSLA make findStreaks
	echo "Most POPL papers per conference:"
	FIRST=2 CONF=POPL make findMostPapersPerConf
	echo "Most PLDI papers per conference:"
	FIRST=2 CONF=PLDI make findMostPapersPerConf
	echo "Most ICFP papers per conference:"
	FIRST=2 CONF=ICFP make findMostPapersPerConf
	echo "Most OOPSLA papers per conference:"
	FIRST=2 CONF=OOPSLA make findMostPapersPerConf

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
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(publyear) DESC) as rank, fullname as name, count(publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle IN ($(CONF))) OR (etype = 'article' AND volnumber IN ($(CONF)))) GROUP BY fullname ORDER BY count(publyear) DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

countMostConfs: #invoke like `FIRST=5 CONF=PLDI make countMostConfs`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(DISTINCT publyear) DESC) as rank, fullname as name, count(DISTINCT publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY count(publyear) DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findLongestCareer: #invoke like `FIRST=5 CONF=PLDI make findLongestCareer`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY max(publyear) - min(publyear) DESC) as rank, fullname as name, min(publyear) as from, max(publyear) as to, max(publyear) - min(publyear) as span FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY span DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findMostPapersPerConf: #invoke like `FIRST=2 CONF=PLDI make findMostPapersPerConf`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY count(publyear) DESC) as rank, fullname as name, publyear as year, count(publyear) as papers FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname, publyear ORDER BY count(publyear) DESC, fullname ASC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

# following query inspired by: https://dba.stackexchange.com/a/239622
findStreaks: #invoke like `FIRST=3 CONF=PLDI make findStreaks`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY endpoint - publyear + 1 DESC) as rank, fullname as name, publyear AS from, endpoint AS to, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint IS NOT null ORDER BY streaklength DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findOngoingStreaks: #invoke like `FIRST=3 CONF=PLDI CURRENTYEAR=2020 make findOngoingStreaks`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY endpoint - publyear + 1 DESC) as rank, fullname as name, publyear AS from, endpoint AS to, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint = $(CURRENTYEAR) ORDER BY streaklength DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findSigplanStreaks: #invoke like `FIRST=5 make findSigplanStreaks`
	psql dblp -c "SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY endpoint - publyear + 1 DESC) as rank, fullname as name, publyear AS from, endpoint AS to, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear * 4 + (CASE WHEN (etype = 'inproceedings' AND booktitle = 'POPL') OR (etype = 'article' AND volnumber = 'POPL') THEN 0 WHEN (etype = 'inproceedings' AND booktitle = 'PLDI') OR (etype = 'article' AND volnumber = 'PLDI') THEN 1 WHEN (etype = 'inproceedings' AND booktitle = 'ICFP') OR (etype = 'article' AND volnumber = 'ICFP') THEN 2 WHEN (etype = 'inproceedings' AND booktitle = 'OOPSLA') OR (etype = 'article' AND volnumber = 'OOPSLA') THEN 3 END) as publyear FROM fullpaperswithauthors WHERE (etype = 'inproceedings' AND booktitle IN ('POPL','PLDI','ICFP','OOPSLA')) OR (etype = 'article' AND volnumber IN ('POPL','PLDI','ICFP','OOPSLA')) ) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint IS NOT null ORDER BY streaklength DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

findHolidays: #invoke like `FIRST=5 CONF=POPL make findHolidays`
	psql dblp -c "WITH papers_of AS (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) SELECT * FROM (SELECT DENSE_RANK() OVER(ORDER BY lastyear.publyear - firstyear.publyear DESC) as rank,firstyear.fullname as name, firstyear.publyear as from, lastyear.publyear as to, (lastyear.publyear - firstyear.publyear) as gap FROM papers_of AS firstyear INNER JOIN papers_of AS lastyear ON firstyear.fullname = lastyear.fullname AND firstyear.publyear < lastyear.publyear LEFT JOIN papers_of AS intermediateyear ON firstyear.fullname = intermediateyear.fullname AND firstyear.publyear < intermediateyear.publyear AND intermediateyear.publyear < lastyear.publyear WHERE intermediateyear.fullname IS null ORDER BY gap DESC) AS x WHERE rank <= $(FIRST) ORDER BY rank ASC"

