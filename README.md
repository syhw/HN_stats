# hackernews

Seeks the most popuplar (by karma and comments) articles on HN and make a 
text corpus out of them.

## Usage

    lein run
and you get the articles in data, see:
    folder-prefix
    textonly
    sortby
in src/hackernews/core.clj

Temporary workflow: once articles are downloaded (in text format) in data/
    ./mahout-distribution-0.7/bin/mahout seqdirectory --input data --output seqfiles
    ./mahout-distribution-0.7/bin/mahout seq2sparse -i seqfiles -o mahoutvectors -wt tf --minDF 5 --maxDFPercent 90
    ./mahout-distribution-0.5/bin/mahout lda -i mahoutvectors/tf-vectors -o hackernews-lda -k 42 -v 1000 -x 20 -ow

## Development

If you use vimclojure with nailgun, you will need to give it more memory:

    java -Xmx512m -Xms512m -cp "$LEIN_CLASSPATH" ...

## License

Copyright (C) 2012 Gabriel Synnaeve

Distributed under the Eclipse Public License, the same as Clojure.

## TODO

Compare our tika (including boilerpipe) solution to:
- decruft
- unfluff

Use stemming

Hacker News specialized fetcher (self.HN links that require login)


