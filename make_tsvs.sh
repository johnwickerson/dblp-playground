function fixup {
    cat $1 \
	| gsed -e 's/\"/\\\"/g'       \ # escape double-quotes
	| gsed -e 's/^\t/NULL\t/g'    \ # replace empty field at start of row with NULL
        | gsed -e 's/\t$/\tNULL/g'    \ # replace empty field at end of row with NULL
	| gsed -e 's/\t\t/\tNULL\t/g' \ # replace empty field in middle of row with NULL
	| gsed -e 's/\\\([^N]\)/|\1/g'  # replace \x with x except when x==N
}

echo "Escaping quotes, replacing empty fields, and removing escaped-characters in TSV files"
fixup papers.tsv > papers2.tsv
fixup authors.tsv > authors2.tsv
fixup writtenBy.tsv > writtenBy2.tsv
fixup editedBy.tsv > editedBy2.tsv

echo "Copying TSV files into tsv directory"
mkdir -p tsv
mv papers2.tsv tsv/papers.tsv
mv authors2.tsv tsv/authors.tsv
mv writtenBy2.tsv tsv/writtenBy.tsv
mv editedBy2.tsv tsv/editedBy.tsv
mv lengths.tsv tsv/lengths.tsv
