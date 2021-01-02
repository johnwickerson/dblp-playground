-- Author: unknown
-- Retrieved from http://agdb.informatik.uni-bremen.de/dblp/statistics.php
-- Modified by: John Wickerson

DROP TABLE if EXISTS writtenBy;
DROP TABLE if EXISTS editedBy;
DROP VIEW if EXISTS papers;
DROP TABLE if EXISTS papersOriginal;
DROP TABLE if EXISTS authors;
CREATE TABLE papersOriginal (etype varchar(20), title varchar(1642), booktitle varchar(210), pages varchar(100), publYear int, address varchar(9), journal varchar(76), volume varchar(50), volNumber varchar(50), month varchar(26), url varchar(1000), ee varchar(254), cdrom varchar(51), cite varchar(48), publisher varchar(163), note varchar(350), crossref varchar(39), isbn varchar(19), series varchar(136), school varchar(150), chapter int, mdate varchar(100), publType varchar(100), reviewid varchar(100), rating varchar(100), paperKey varchar(100), pagesFrom int, pagesTo int, numPages int, pid int primary key);
CREATE VIEW papers AS (SELECT * FROM papersOriginal WHERE NOT etype='www');
CREATE TABLE authors (aid int primary key, fullName varchar(100), firstName varchar(100), lastName varchar(100));
CREATE TABLE writtenBy(pid int references papersOriginal, aid int references authors, apos int);
CREATE TABLE editedBy (pid int references papersOriginal, eid int references authors, epos int);
