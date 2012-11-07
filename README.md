# hackernews

Seeks the most popuplar (by karma and comments) articles on HN and make a 
text corpus out of them.

## Usage

    lein run

## Development

If you use vimclojure with nailgun, you will need to give it more memory:

    java -Xmx512m -Xms512m -cp "$LEIN_CLASSPATH" ...

## License

Copyright (C) 2012 Gabriel Synnaeve

Distributed under the Eclipse Public License, the same as Clojure.

## TODO

compare our tika (including boilerpipe) solution to:
- decruft
- unfluff

