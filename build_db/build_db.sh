#!/bin/sh

# Original author: unknown
# Retrieved from http://agdb.informatik.uni-bremen.de/dblp/statistics.php
# Modified in January 2021 by John Wickerson

DBLP=$1

# Abort if any command fails
set -e

DIR="$( pwd )"

echo "Creating new blank database"
createdb $DBLP encoding='UTF8'

echo "Creating database structure"
psql $DBLP -f create.sql

echo "Converting XML files to TSV files. Beware; this takes AGES."
python dblp-xml-to-tsv.py ../dblp.xml

function escapeQuotes {
    cat $1 | sed -e 's/\"/\\\"/g'
}

function replaceEmpties {
  cat $1 | sed -e 's/^\t/NULL\t/;s/\t$/\tNULL/;:0 s/\t\t/\tNULL\t/;t0'
}

function replaceEscaped {
  cat $1 | sed -e 's/\\0/\0/g;s/\\\././g'
}

echo "Escaping quotes in TSV files"
escapeQuotes papers.tsv > papers2.tsv
escapeQuotes authors.tsv > authors2.tsv
escapeQuotes writtenBy.tsv > writtenBy2.tsv
escapeQuotes editedBy.tsv > editedBy2.tsv

echo "Replacing empty strings with NULL in TSV files"
replaceEmpties papers2.tsv > papers3.tsv
replaceEmpties authors2.tsv > authors3.tsv
replaceEmpties writtenBy2.tsv > writtenBy3.tsv
replaceEmpties editedBy2.tsv > editedBy3.tsv

echo "Avoiding backslash-0 and backslash-dot in TSV files, as these confuse PostgreSQL"
replaceEscaped papers3.tsv > papers4.tsv
replaceEscaped authors3.tsv > authors4.tsv
replaceEscaped writtenBy3.tsv > writtenBy4.tsv
replaceEscaped editedBy3.tsv > editedBy4.tsv

echo "Move TSV files into tsv directory"
mkdir tsv
mv papers4.tsv tsv/papers.tsv
mv authors4.tsv tsv/authors.tsv
mv writtenBy4.tsv tsv/writtenBy.tsv
mv editedBy4.tsv tsv/editedBy.tsv
mv lengths.tsv tsv/lengths.tsv

echo "Load data into the database"
psql $DBLP -c "COPY papersOriginal FROM '$DIR/tsv/papers.tsv' delimiter E'\t'"
psql $DBLP -c "COPY authors FROM '$DIR/tsv/authors.tsv' delimiter E'\t'"
psql $DBLP -c "COPY editedBy FROM '$DIR/tsv/editedBy.tsv' delimiter E'\t'"
psql $DBLP -c "COPY writtenBy FROM '$DIR/tsv/writtenBy.tsv' delimiter E'\t'"

echo "Create auxiliary tables"
psql $DBLP -f tables.sql

echo "Create table of 'CORE' people"
psql $DBLP -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10)); COPY corePeople FROM '$DIR/../core_people.tsv' delimiter E'\t'"
