import sys, bz2, glob, pickle, multiprocessing
from gensim import utils, models
from gensim.corpora.dictionary import Dictionary
from gensim.corpora.textcorpus import TextCorpus
from gensim.corpora.mmcorpus import MmCorpus
from gensim.corpora import wikicorpus
import logging
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

NO_BELOW = 32 # no word used less 32 times
NO_ABOVE = 0.08 # no word which is in above 8% of the corpus
VOCAB_SIZE = 200000 # 200k
WIKI_ARTICLE_MIN_WORDS = 50
HN_ARTICLE_MIN_WORDS = 42
outputname = 'wiki_hn'

def tokenize(text):
    return [token.encode('utf8') for token in utils.tokenize(text, lower=True, errors='ignore') if 2 <= len(token) <= 20 and not token.startswith('_')]


class WikiHNCorpus(TextCorpus):
    def __init__(self, wiki_file, hn_folder, dictionary=None, processes=None, 
            lemmatize=utils.HAS_PATTERN):
        """
        Takes the wikipedia *articles.xml.bz2 and the HN folder of articles 
        as input and builds the dictionary and corpus
        """
        global outputname
        self.lemmatize = lemmatize
        if self.lemmatize:
            print "We will lemmatize ('you were'->'be/VB')"
            self.outputname = outputname + "_lemmatized"
        else:
            print "We will only tokenize ('you were'->'you','were')"

        self.wiki_file = wiki_file
        self.hn_folder = hn_folder

        if processes is None:
            processes = max(1, multiprocessing.cpu_count() - 1)
        self.processes = processes

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
        positions, positions_all = 0, 0

        # ************ Wikipedia ************
        texts = ((text, self.lemmatize) for _, text in wikicorpus._extract_pages(bz2.BZ2File(self.wiki_file)))
        pool = multiprocessing.Pool(self.processes)
        for group in utils.chunkize(texts, chunksize=10 * pool._processes, maxsize=1): # otherwise imap puts all the corpus into memory
            for tokens in pool.imap(wikicorpus.process_article, group):
                articles_all += 1
                positions_all += len(tokens)
                if len(tokens) > WIKI_ARTICLE_MIN_WORDS:
                    wiki_articles += 1
                    positions += len(tokens)
                    yield tokens
        pool.terminate()

        print (">>> finished iterating over Wikipedia corpus of %i documents with %i positions (total %i articles, %i positions before pruning articles shorter than %i words)" % (wiki_articles, positions, articles_all, positions_all, WIKI_ARTICLE_MIN_WORDS))

        # ************ HN articles ************
        positions_after_wiki = positions
        fnamelist = []
        for g in glob.iglob(self.hn_folder + '/*.txt'):
            fnamelist.append(g)
        for fileno, fname in enumerate(fnamelist): # TODO parallelize as Wiki
            hn_text = open(fname).read()
            if self.lemmatize:
                result = utils.lemmatize(hn_text) # text into lemmas here
            else:
                result = tokenize(hn_text) # text into tokens here
            articles_all += 1
            positions_all += len(result)
            if len(result) > HN_ARTICLE_MIN_WORDS:
                hn_articles += 1
                positions += len(result)
                yield result

        print (">>> finished iterating over HN corpus of %i documents with %i positions" % (hn_articles, positions - positions_after_wiki))
        # ************ /HN articles ************

        self.length = wiki_articles + hn_articles # cache corpus length


if __name__ == '__main__':
    if len(sys.argv) > 1:
        print "Usage, see __name__ == '__main__' ==> TODO"
        sys.exit(-1)

    try:
        id2token = Dictionary.load_from_text(outputname + '_wordids.txt')
        mm = MmCorpus(outputname + '_bow.mm')
        print ">>> Loaded corpus from serialized files"
    except:
        print ">>> Extracting articles..."
        corpus = WikiHNCorpus(
            '/Volumes/Photos/wikipedia/enwiki-latest-pages-articles.xml.bz2',
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
            num_topics=2000, update_every=1, chunksize=50000, passes=10)

    f = open(outputname + '.ldamodel', 'w')
    pickle.dump(lda, f)
    lda.print_topics(30)


