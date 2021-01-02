-- Author: unknown
-- Retrieved from http://agdb.informatik.uni-bremen.de/dblp/statistics.php

DROP TABLE if EXISTS authorPairs;

CREATE index completeWrittenBy ON writtenBy(pid, aid, apos);
CREATE index author ON writtenBy(aid);
CREATE index id ON writtenBy(pid);

CREATE TABLE authorPairs AS
SELECT DISTINCT a.aid, b.aid AS coauthor
FROM writtenBy AS a
  JOIN writtenBy AS b ON a.pid=b.pid
WHERE NOT (a.aid = b.aid);

CREATE VIEW fullpapers AS
SELECT * FROM papers WHERE pagesto - pagesfrom >= 4

CREATE VIEW definitiveAuthorNames AS
SELECT a1.fullname AS definitivefullname, a2.fullname AS fullname
FROM writtenBy AS w1
  JOIN writtenBy AS w2 ON w1.pid = w2.pid
  JOIN papersOriginal AS p ON w1.pid = p.pid
  JOIN authors AS a1 ON a1.aid = w1.aid
  JOIN authors AS a2 ON a2.aid = w2.aid
WHERE etype = 'www'
  AND title = 'Home Page'
  AND w1.apos = 1

CREATE VIEW newWrittenBy AS
SELECT w.pid, a2.aid, w.apos
FROM writtenBy AS w
  JOIN authors AS a ON w.aid = a.aid
  JOIN definitiveAuthorNames AS d ON a.fullname = d.fullname
  JOIN authors AS a2 ON d.definitivefullname = a2.fullname

CREATE VIEW fullpaperswithauthors AS
SELECT fullpapers.*, authors.*
FROM fullpapers
  JOIN newwrittenby ON fullpapers.pid = newwrittenby.pid
  JOIN authors ON authors.aid = newwrittenby.aid

CREATE VIEW fullpaperswithauthoraliases AS
SELECT fullpapers.*, definitiveAuthorNames.fullname
FROM fullpapers
  JOIN newwrittenby ON fullpapers.pid = newwrittenby.pid
  JOIN authors ON authors.aid = newwrittenby.aid
  JOIN definitiveAuthorNames ON definitiveAuthorNames.definitivefullname = authors.fullname


