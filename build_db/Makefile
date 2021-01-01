DBLP=dblp

all:
	./build_db.sh $(DBLP)

clean:
	echo "Deleting existing database"
	dropdb --if-exists $(DBLP)
	echo "Delete existing TSV files"
	rm -f *.tsv
	rm -rf tsv

test:
	psql $(DBLP) -c "SELECT publYear, COUNT(publYear) FROM papers WHERE publYear BETWEEN 1995 AND 2005 GROUP BY publYear ORDER BY publYear"

firstalpha: #invoke like `CONF=POPL make firstalpha`
	psql $(DBLP) -c "SELECT title, publyear FROM fullpapers WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) ORDER BY title ASC"

lastalpha: #invoke like `CONF=POPL make lastalpha`
	psql $(DBLP) -c "SELECT title, publyear FROM fullpapers WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) ORDER BY title DESC"

papersFor: #invoke like `CONF=POPL AUTHOR="John Wickerson" make papersFor`
	psql $(DBLP) -c "SELECT title, publyear FROM fullpaperswithauthoraliases WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND fullname = '$(AUTHOR)' ORDER BY publYear DESC"

recentPapersFor: #invoke like `CONF=POPL AUTHOR="Alex Aiken" make recentPapersFor`
	psql $(DBLP) -c "SELECT title, publyear FROM fullpaperswithauthoraliases WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND fullname = '$(AUTHOR)' AND publYear BETWEEN 2015 AND 2020 ORDER BY publYear DESC"

countCorePapers: #invoke like `CONF=POPL AREA=PLDI make countCorePapers`
	psql $(DBLP) -c "DROP TABLE if EXISTS corepeople"
	psql $(DBLP) -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql $(DBLP) -c "COPY corePeople FROM '$$DIR/../core_people.tsv' delimiter E'\t'"
	psql $(DBLP) -c "SELECT corepeople.fullname, count(publyear) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)' GROUP BY corepeople.fullname"

countCoreConfs: #invoke like `CONF=POPL AREA=PLDI make countCoreConfs`
	psql $(DBLP) -c "DROP TABLE if EXISTS corepeople"
	psql $(DBLP) -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql $(DBLP) -c "COPY corePeople FROM '$$DIR/../core_people.tsv' delimiter E'\t'"
	psql $(DBLP) -c "SELECT corepeople.fullname, count(DISTINCT publyear) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)' GROUP BY corepeople.fullname"

countUniqueCorePapers: #invoke like `CONF=ICFP AREA=TOP make countUniqueCorePapers`
	psql $(DBLP) -c "DROP TABLE if EXISTS corepeople"
	psql $(DBLP) -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10))"
	DIR="$$( pwd )"; psql $(DBLP) -c "COPY corePeople FROM '$$DIR/../core_people.tsv' delimiter E'\t'"
	psql $(DBLP) -c "SELECT count(DISTINCT pid) FROM corepeople JOIN fullpaperswithauthoraliases ON corepeople.fullname = fullpaperswithauthoraliases.fullname WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) AND publYear BETWEEN 2015 AND 2020 AND area='$(AREA)'"

countMostPapers: #invoke like `CONF=ICFP make countMostPapers`
	psql $(DBLP) -c "SELECT fullname, count(publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY count(publyear) DESC"

countMostConfs: #invoke like `CONF=PLDI make countMostConfs`
	psql $(DBLP) -c "SELECT fullname, count(DISTINCT publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY count(publyear) DESC"

findLongestCareer: #invoke like `CONF=PLDI make findLongestCareer`
	psql $(DBLP) -c "SELECT fullname, min(publyear), max(publyear), max(publyear) - min(publyear) as span FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname ORDER BY span DESC"

findMostPapersPerConf: #invoke like `CONF=PLDI make findMostPapersPerConf`
	psql $(DBLP) -c "SELECT fullname, publyear, count(publyear) FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)')) GROUP BY fullname, publyear ORDER BY count(publyear) DESC, fullname ASC"

# following query inspired by: https://dba.stackexchange.com/a/239622
findStreaks: #invoke like `CONF=PLDI make findStreaks`
	psql $(DBLP) -c "SELECT fullname, publyear AS first, endpoint AS last, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint IS NOT null ORDER BY streaklength DESC"

findOngoingStreaks: #invoke like `CONF=PLDI CURRENTYEAR=2020 make findOngoingStreaks`
	psql $(DBLP) -c "SELECT fullname, publyear AS first, endpoint AS last, (endpoint - publyear + 1) AS streaklength FROM (SELECT fullname, publyear, CASE WHEN prev <> 1 AND next <> -1 THEN publyear WHEN next = -1 THEN lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING) ELSE null END AS endpoint FROM ( SELECT fullname, publyear, coalesce(publyear - lag(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 0) AS prev, coalesce(publyear - lead(publyear) OVER (PARTITION BY fullname ORDER BY publyear ROWS BETWEEN CURRENT ROW AND 1 FOLLOWING), 0) AS next FROM (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) AS papers_of) AS mark_boundaries WHERE NOT (prev = 1 AND next = -1)) AS raw_ranges WHERE endpoint = $(CURRENTYEAR) ORDER BY streaklength DESC"

findHolidays: #invoke like `CONF=POPL make findHolidays`
	psql $(DBLP) -c "WITH papers_of AS (SELECT DISTINCT fullname, publyear FROM fullpaperswithauthors WHERE ((etype = 'inproceedings' AND booktitle = '$(CONF)') OR (etype = 'article' AND volnumber = '$(CONF)'))) SELECT firstyear.fullname, firstyear.publyear as firstyear, lastyear.publyear as lastyear, (lastyear.publyear - firstyear.publyear) as gap FROM papers_of AS firstyear INNER JOIN papers_of AS lastyear ON firstyear.fullname = lastyear.fullname AND firstyear.publyear < lastyear.publyear LEFT JOIN papers_of AS intermediateyear ON firstyear.fullname = intermediateyear.fullname AND firstyear.publyear < intermediateyear.publyear AND intermediateyear.publyear < lastyear.publyear WHERE intermediateyear.fullname IS null ORDER BY gap DESC"
