import sys, bz2, glob, pickle
from gensim import utils, models
from gensim.corpora.dictionary import Dictionary
from gensim.corpora.textcorpus import TextCorpus
from gensim.corpora.mmcorpus import MmCorpus
from gensim.corpora.wikicorpus import filter_wiki
import logging
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

NO_BELOW = 20 # no word used less than 20 times
NO_ABOVE = 0.1 # no word which is in above 10% of the corpus
VOCAB_SIZE = 200000 # 200k
WIKI_ARTICLE_MIN_CHARS = 500
LEMMATIZE = utils.HAS_PATTERN
outputname = 'wiki_hn'

def tokenize(text):
    return [token.encode('utf8') for token in utils.tokenize(text, lower=True, errors='ignore') if 2 <= len(token) <= 20 and not token.startswith('_')]

class WikiHNCorpus(TextCorpus):
    def __init__(self, wiki_file, hn_folder, dictionary=None):
        """
        Takes the wikipedia *articles.xml.bz2 and the HN folder of articles 
        as input and builds the dictionary and corpus
        """
        self.wiki_file = wiki_file
        self.hn_folder = hn_folder
        if dictionary is None:
            self.dictionary = Dictionary(self.get_texts())
            self.dictionary.filter_extremes(no_below=NO_BELOW, 
                    no_above=NO_ABOVE, keep_n=VOCAB_SIZE)
        else:
            self.dictionary = dictionary


    def get_texts(self):
        """
        Iterate over the Wikipedia dump and the HN articles returning text
        """
        wiki_articles, hn_articles, articles_all = 0, 0, 0
        # ************ Wikipedia ************
        intext, positions = False, 0
        if LEMMATIZE:
            lemmatizer = utils.lemmatizer
            yielded = 0
        for lineno, line in enumerate(bz2.BZ2File(self.wiki_file)):
            if line.startswith('      <text'):
                intext = True
                line = line[line.find('>') + 1 : ]
                lines = [line]
            elif intext:
                lines.append(line)
            pos = line.find('</text>') # can be on the same line as <text>
            if pos >= 0:
                articles_all += 1
                intext = False
                if not lines:
                    continue
                lines[-1] = line[:pos]
                text = filter_wiki(''.join(lines))
                if len(text) > WIKI_ARTICLE_MIN_CHARS: # article redirects are pruned here
                    wiki_articles += 1
                    if LEMMATIZE:
                        _ = lemmatizer.feed(text)
                        while lemmatizer.has_results():
                            _, result = lemmatizer.read() # not necessarily the same text as entered above!
                            positions += len(result)
                            yielded += 1
                            yield result
                    else:
                        result = tokenize(text) # text into tokens here
                        positions += len(result)
                        yield result

        if LEMMATIZE:
            print ("all %i wiki articles read; waiting for lemmatizer to finish the %i remaining jobs" % (wiki_articles, wiki_articles - yielded))
            while yielded < wiki_articles:
                _, result = lemmatizer.read()
                positions += len(result)
                yielded += 1
                yield result

        print (">>> finished iterating over Wikipedia corpus of %i documents with %i positions (total %i wiki articles before pruning)" % (wiki_articles, positions, articles_all))
        # ************ /Wikipedia ************

        # ************ HN articles ************
        positions_after_wiki = positions
        fnamelist = []
        for g in glob.iglob(self.hn_folder + '/*.txt'):
            fnamelist.append(g)
        for fileno, fname in enumerate(fnamelist):
            hn_text = open(fname).read()
            articles_all += 1
            hn_articles += 1
            if LEMMATIZE:
                _ = lemmatizer.feed(hn_text)
                while lemmatizer.has_results():
                    _, result = lemmatizer.read() # not necessarily the same text as entered above!
                    positions += len(result)
                    yielded += 1
                    yield result
            else:
                result = tokenize(text) # text into tokens here
                positions += len(result)
                yield result

        if LEMMATIZE:
            print ("all %i hn_articles read; waiting for lemmatizer to finish the %i remaining jobs" % (hn_articles, hn_articles - yielded))
            while yielded < hn_articles:
                _, result = lemmatizer.read()
                positions += len(result)
                yielded += 1
                yield result

        print (">>> finished iterating over HN corpus of %i documents with %i positions" % (wiki_articles, positions - positions_after_wiki))
        # ************ /HN articles ************

        self.length = wiki_articles + hn_articles # cache corpus length


if __name__ == '__main__':
    if len(sys.argv) > 1:
        print "Usage, see __name__ == '__main__' ==> TODO"
        sys.exit(-1)
    print ">>> Extracting articles..."
    corpus = WikiHNCorpus(
        '/Volumes/Photos/wikipedia/10000/enwiki-latest-pages-articles1.xml-p000000010p000010000.bz2',
        '/Users/gabrielsynnaeve/labs/clojure/hackernews/data')

    corpus.dictionary.save_as_text(outputname + '_wordids.txt')
    print ">>> Saved dictionary as " + outputname + "_wordids.txt"

    MmCorpus.serialize(outputname + '_bow.mm', corpus, progress_cnt=10000)
    print ">>> Saved MM corpus as " + outputname + "_bow.mm"

    id2token = Dictionary.load_from_text(outputname + '_wordids.txt')
    mm = MmCorpus(outputname + '_bow.mm')
    # tfidf = models.TfidfModel(mm, id2word=id2token, normalize=True)
    del corpus

    lda = models.ldamodel.LdaModel(corpus=mm, id2word=id2token, 
            num_topics=1000, update_every=1, chunksize=10000, passes=2)

    f = open(outputname + '.ldamodel', 'w')
    pickle.dump(lda, f)
    lda.print_topics(30)


