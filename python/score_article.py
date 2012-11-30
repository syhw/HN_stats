import pickle, sys
from gensim import utils

#article_to_score = '../data/paulgraham.com-startupideas.html.txt'
#article_to_score = '../data/paulgraham.com-founder.html.txt'
article_to_score = '../data/paulgraham.com-ycombinator.html.txt'
text = open(article_to_score, 'r').read()

LEMMATIZE = utils.HAS_PATTERN
lda = None
if LEMMATIZE:
    f = open('/Users/gabrielsynnaeve/Dropbox/Public/hn_lemmatized.ldamodel', 'r')
    lda = pickle.load(f)
    a = utils.lemmatize(text)
else:
    print >> sys.stderr, "ERROR: install pattern"
    sys.exit(-1)

user = 'pg'
if len(sys.argv) > 1:
    user = sys.argv[1]

user_params = None
with open(user + '.params') as f:
    user_params = pickle.load(f)

# score \proto P(Like) 
# P(Like=true) \propto \sum_{t \in Topics}[P(TopicsArticle)
#                 * P(\lambda|t,TopicsArticle) * P(t|Like=true) * P(Like=true)]
score = 0.0
for topicid, proba in lda[lda.id2word.doc2bow(a)]:
    score += proba * user_params[topicid]

print score

