import sys, glob, pickle
from gensim import utils, models
from gensim.corpora.dictionary import Dictionary
from gensim.corpora.textcorpus import TextCorpus
from gensim.corpora.mmcorpus import MmCorpus
import logging
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

NO_BELOW = 10 # no word used less than 10 times
NO_ABOVE = 0.1 # no word which is in above 10% of the corpus
VOCAB_SIZE = 100000 # 100k, more? TODO

LEMMATIZE = utils.HAS_PATTERN
outputname = 'hn'
if LEMMATIZE:
    print "you have pattern: we will lemmatize ('you were'->'be/VB')"
    outputname = 'hn_lemmatized'
else:
    print "you don't have pattern: we will tokenize ('you were'->'you','were')"

def tokenize(text):
    return [token.encode('utf8') for token in utils.tokenize(text, lower=True, errors='ignore') if 2 <= len(token) <= 20 and not token.startswith('_')]

class HNCorpus(TextCorpus):
    def __init__(self, hn_folder, dictionary=None):
        """
        Takes the HN folder of articles 
        as input and builds the dictionary and corpus
        """
        self.hn_folder = hn_folder
        if dictionary is None:
            self.dictionary = Dictionary(self.get_texts())
            self.dictionary.filter_extremes(no_below=NO_BELOW, 
                    no_above=NO_ABOVE, keep_n=VOCAB_SIZE)
        else:
            self.dictionary = dictionary


    def get_texts(self):
        """
        Iterate over the HN articles returning text
        """
        positions, hn_articles = 0, 0

        # ************ HN articles ************
        fnamelist = []
        for g in glob.iglob(self.hn_folder + '/*.txt'):
            fnamelist.append(g)
        for fileno, fname in enumerate(fnamelist):
            hn_text = open(fname).read()
            hn_articles += 1
            if LEMMATIZE:
                result = utils.lemmatize(hn_text)
                positions += len(result)
                yield result
            else:
                result = tokenize(hn_text) # text into tokens here
                positions += len(result)
                yield result

        print (">>> finished iterating over HN corpus of %i documents with %i positions" % (hn_articles, positions))

        self.length = hn_articles # cache corpus length


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
        corpus = HNCorpus('/Users/gabrielsynnaeve/labs/clojure/hackernews/data')
        corpus.dictionary.save_as_text(outputname + '_wordids.txt')
        print ">>> Saved dictionary as " + outputname + "_wordids.txt"
        MmCorpus.serialize(outputname + '_bow.mm', corpus, progress_cnt=10000)
        print ">>> Saved MM corpus as " + outputname + "_bow.mm"
        id2token = Dictionary.load_from_text(outputname + '_wordids.txt')
        mm = MmCorpus(outputname + '_bow.mm')
        # tfidf = models.TfidfModel(mm, id2word=id2token, normalize=True)
        del corpus

    lda = models.ldamodel.LdaModel(corpus=mm, id2word=id2token, 
            num_topics=40, update_every=1, chunksize=10000, passes=5)


    f = open(outputname + '.ldamodel', 'w')
    pickle.dump(lda, f)
    lda.print_topics(-1)
    f.close()

    alpha = [i*0.1/40 for i in range(1,101)] # enforcing sparsity on topics
    # with the first topic 40 less probable than the 40th
    div = sum(alpha)
    alpha = [x/div for x in alpha]
    lda_sparse = models.ldamodel.LdaModel(corpus=mm, id2word=id2token, 
            num_topics=40, update_every=1, chunksize=10000, passes=5,
            alpha=alpha)

    f = open(outputname + '.ldasparsemodel', 'w')
    pickle.dump(lda_sparse, f)
    lda_sparse.print_topics(-1)


