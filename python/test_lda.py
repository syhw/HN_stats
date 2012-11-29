import pickle, numpy
from gensim import utils, models
from gensim.corpora.dictionary import Dictionary

LEMMATIZE = utils.HAS_PATTERN

def tokenize(text):
    return [token.encode('utf8') for token in utils.tokenize(text, lower=True, errors='ignore') if 2 <= len(token) <= 20 and not token.startswith('_')]

def topic_names(ldaobject):
    " WORSE ALGORITHM DESCRIPTION EVER "
    topn = ldaobject.num_topics # test, should perhaps be less? 10? TODO
    bests = []
    topicnames = []
    for topicid in range(ldaobject.num_topics):
        topic = ldaobject.state.get_lambda()[topicid]
        topic /= topic.sum()
        bests.append(numpy.argsort(topic)[::-1][:topn])
    for topicid in range(ldaobject.num_topics):
        topicnames.append(ldaobject.id2word[bests[topicid][0]] + " " 
                + ldaobject.id2word[bests[topicid][1]])
        ind = 0
        while ind < topn:
            contsearch = False
            # we seek the word bests[topicid][ind] in the bests words
            # for others topic
            for i,wl in enumerate(bests):
                if i == topicid:
                    continue
                if bests[topicid][ind] in wl:
                    # if we found bests[topicid][ind] in other topics' bests
                    # words, we try and find a more discrimining word
                    ind += 1
                    contsearch = True
                    break
            if contsearch:
                continue
            # here we are not "contsearch"ing, so we have found a discriming
            # word as bests[topicid][ind]:
            if ind == 0:
                # if it's the first we keep the default
                break
            candidate = ldaobject.id2word[bests[topicid][ind]]
            if len(candidate) > 2: # TODO here we only take words>2 in length
                topicnames[topicid] = candidate
                break
            else:
                ind += 1
            continue

        best10 = bests[topicid][:10]
        beststrl = [(topic[i], ldaobject.id2word[i]) for i in best10]
        beststr = ' + '.join(['%.3f*%s' % v for v in beststrl])
        print "topic #", topicid, " described by word:", topicnames[topicid]
        print beststr

f = open('hn.ldamodel', 'r')
lda = pickle.load(f)
#for i in range(100):
#    print lda.print_topic(i)
topic_names(lda)
import sys
sys.exit(0)

id2token = Dictionary.load_from_text('/Users/gabrielsynnaeve/Dropbox/Public/hn_wordids.txt')
article = open('/Users/gabrielsynnaeve/labs/clojure/hackernews/data/99985.txt', 'r').read()
#if LEMMATIZE:
#    a = utils.lemmatize(article)
#    print a
#else:
a = tokenize(article)
print a
for topic, proba in lda[id2token.doc2bow(a)]:
    print lda.show_topic(topic)
    print proba

#print lda[article]

