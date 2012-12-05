import pickle, numpy, sys
from gensim import utils

user = 'pg'
users = None
if 3 > len(sys.argv) > 1:
    user = sys.argv[1]
elif len(sys.argv) > 2:
    if sys.argv[1] == '-f':
        users = open(sys.argv[2])

LEMMATIZE = utils.HAS_PATTERN
lda = None
if LEMMATIZE:
    #f = open('/Users/gabrielsynnaeve/Dropbox/Public/hn_lemmatized.ldamodel', 'r')
    f = open('/Users/gabrielsynnaeve/Dropbox/Public/hn40_lemmatized.ldamodel', 'r')
    lda = pickle.load(f)
else:
    print >> sys.stderr, "ERROR: install pattern"
    sys.exit(-1)

def extract_user(user):
    with open('../data/' + user + '/interesting_articles.txt') as stalk_f:
        articles = filter(lambda x: x != '',
                stalk_f.read().rstrip('\n').split(' '))

    tastes = numpy.array([0.0 for i in range(lda.num_topics)])
    total = 0.0
    having = 0
    not_having = 0

    for article in articles:
        #print article
        try:
            text = open('../data/' + article + '.txt').read()
            having += 1
        except IOError: # we don't have this article
            not_having += 1
            continue
        if LEMMATIZE:
            a = utils.lemmatize(text)
        else:
            print >> sys.stderr, "ERROR: install pattern"
            sys.exit(-1)
        for topicid, proba in lda[lda.id2word.doc2bow(a)]:
            total += proba
            tastes[topicid] += proba

    tastes /= total

    of = open(user+'.params', 'w')
    pickle.dump(tastes.tolist(), of)

    print "For user:", user, " we had:", having, "and missed:", not_having, "->", having*100.0/(having+not_having+0.000001), "%"

if users == None:
    extract_user(user)
else:
    for line in users:
        user = line.strip().rstrip('\n')
        try:
            with open('../data/' + user + '/interesting_articles.txt') as test_f: pass
        except IOError:
            print >> sys.stderr, "We do not have user:", user
        extract_user(user)
