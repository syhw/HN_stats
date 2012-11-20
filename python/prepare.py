import sys
from gensim import corpora, models, similarities
import logging
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

# globals:
directory = ''
fnamelist = []
dictionary = {}
stoplist = ['.', ',', ';', ':', '-', '!', '?', '(', ')', '"', "'"]
init = False

class OnlineCorpus(object):
    def __init__(self, dictionary):
        self.dictionary = dictionary
    def __iter__(self):
        for f in fnamelist:
            with open(f, 'r') as doc:
                yield self.dictionary.doc2bow(doc.read().lower().split())

def init_dict(fnamelist):
    dictionary = corpora.Dictionary(open(f, 'r').read().lower().split() for f in fnamelist)
    stop_ids = [dictionary.token2id[stopword] for stopword in stoplist if stopword in dictionary.token2id]
    once_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq == 1]
    dictionary.filter_tokens(stop_ids + once_ids)
    dictionary.compactify()
    dictionary.save(dict_path)
    print "saved: ", dict_path
    return dictionary

def init_corpus(dictionary):
    corpus = OnlineCorpus(dictionary)
    corpora.MmCorpus.serialize(directory + 'corpus.mm', corpus)
    print "saved: ", corpus_path
    return corpora.MmCorpus(corpus_path)

if __name__ == "__main__":
    # init
    if '-i' in sys.argv:
        init = True
    if '-d' in sys.argv:
        directory = sys.argv[sys.argv.index('-d') + 1] + '/'
        import glob
        for g in glob.iglob(directory + '*.txt'):
            fnamelist.append(g)
    else:
        add = 0
        if '-i' in sys.argv:
            add += 1
        fnamelist = [fnam for fnam in sys.argv[1+add:]]
        directory = '/'.join(fnamelist[0].split('/')[:-1]) + '/'
    print "working in: ", directory

    dict_path = directory + 'dictionary.dict'
    if not init:
        try:
            with open(dict_path) as f:
                pass
            dictionary = corpora.Dictionary.load(dict_path)
            print "opened: ", dict_path
        except IOError as e:
            dictionary = init_dict(fnamelist)
    else:
        dictionary = init_dict(fnamelist)
    
    corpus_path = directory + 'corpus.mm'
    if not init:
        try:
            with open(corpus_path) as f:
                corpus = corpora.MmCorpus(corpus_path)
                print "opened: ", corpus_path
        except IOError as e:
            corpus = init_corpus(dictionary)
    else:
        corpus = init_corpus(dictionary)

    # transformations
    print corpus
    #tfidf = models.TfidfModel(corpus)

    #hdp = models.hdpmodel.HdpModel(corpus, id2word=dictionary)
    #hdp.print_topics(20)
    #print hdp

    lda = models.ldamodel.LdaModel(corpus, id2word=dictionary, num_topics=20, update_every=1, chunksize=5000, passes=5)
    lda.print_topics(20)


