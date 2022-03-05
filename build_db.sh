#!/bin/sh

# Original author: unknown
# Retrieved from http://agdb.informatik.uni-bremen.de/dblp/statistics.php
# Modified in January 2021 by John Wickerson

# Abort if any command fails
set -e

DIR="$( pwd )"

echo "Load data into the database"
psql dblp -c "COPY papersOriginal FROM '$DIR/tsv/papers.tsv' delimiter E'\t'"
psql dblp -c "COPY authors FROM '$DIR/tsv/authors.tsv' delimiter E'\t'"
psql dblp -c "COPY editedBy FROM '$DIR/tsv/editedBy.tsv' delimiter E'\t'"
psql dblp -c "COPY writtenBy FROM '$DIR/tsv/writtenBy.tsv' delimiter E'\t'"

echo "Creating auxiliary tables"
psql dblp -f tables.sql

echo "Creating table of 'CORE' people"
psql dblp -c "CREATE TABLE corePeople (fullName varchar(100), area varchar(10)); COPY corePeople FROM '$DIR/core_people.tsv' delimiter E'\t'"
