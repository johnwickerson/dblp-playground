# DBLP Playground

[DBLP](https://dblp.uni-trier.de/) is an online database of academic publications in computer science and related fields. Handily, all of its data is available as an XML file, which can be converted into an SQL database and queried. Here, I share a few fun facts I discovered while exploring the data that DBLP holds about some of the main conferences on programming languages: POPL, PLDI, ICFP, and OOPSLA.

_Note: In what follows, I define a paper to be any entry in the proceedings that is at least four pages long. This slightly coarse rule means that some very short but legitimate papers may not get counted. There may also be a small number of entries that are not legitimate papers, such as extended tributes or announcements, but which get counted anyway._

Last update: 26-Jan-2023 (up to and including POPL 2024)

* [Most papers](mostpapers.md)

* [Longest vacations](longestvacations.md)

* [Longest careers](longestcareers.md)

* [Longest streaks](longeststreaks.md)

* [Most papers in a single conference](mostpapersperconf.md)

## Running queries yourself

This repo contains some code for converting the DBLP XML dump into a PostgreSQL database, and some sample queries that can be run on this database, including those that generated the tables linked above.

_Note: I've only tested this on my Mac. I think PostgreSQL tends to be configured a little differently on Linux._

0. System requirements: PostgreSQL (`brew install postgresql`), wget (`brew install wget`), GNU's sed (`brew install gnu-sed`).

1. Start your PostgreSQL server using `pg_ctl -D /opt/homebrew/var/postgres start`. There are further instructions about getting started with PostgreSQL on [this webpage](https://www.robinwieruch.de/postgres-sql-macos-setup).

2. Run `make`. This downloads the DBLP XML dump from `http://dblp.org/xml/dblp.xml.gz`, unzips it into `dblp.xml`, and then converts it into a PostgreSQL database. Caution: the DBLP XML dump is quite a large file, and converting it into a database took me about half an hour.

3. Run `make myqueries`. See the Makefile for the gory details.

## Credits

My code is adapted from some code I found on [this website](http://agdb.informatik.uni-bremen.de/dblp/statistics.php). Unfortunately I can't find any names of people to credit. My SQL queries are hodge-podge of snippets pasted from StackOverflow.
