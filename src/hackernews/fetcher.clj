(ns hackernews.fetcher
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [tika]
            [hackernews.name_recog :refer :all])
  (:import [java.net URLEncoder])
  (:use [cheshire.core])
  (:gen-class))

; http://api.thriftdb.com/api.hnsearch.com/items/_search?filter[field][type][]=submission&start=0&limit=100&filter[fields][create_ts][]=[2010-08-01T00:00:00Z TO 2010-08-01T23:59:59Z]
(def hn-url "http://api.thriftdb.com/api.hnsearch.com/items/_search?")
(def folder-prefix "data/")
(def textonly true)
(def sortby-list '("num_comments desc" "points desc"))
; "score desc" = default of thriftdb: ponders points and num_comments equally
(def stopwords (set (s/split (slurp "models/common-english-words.txt") #",")))
(def pgarticles (s/split (slurp "list_articles_pg.txt") #"\n"))

(defn encode-url 
  " Encodes the URL for the thrifdb API call with the given url and params "
  [url & params]
  ; TEST (prn (encode-url hn-url :start 0 :limit 100))
  ; TEST (prn (encode-url hn-url :start 0 :limit 100 :sortby "points desc" :username "pg"))
  ; TEST (prn (encode-url hn-url :start 0 :limit 100 :date-interval "[2010-08-01T00:00:00Z TO 2010-08-01T23:59:59Z]"))
  (let [kwp (apply hash-map (flatten params))]
    (str url "sortby=" (URLEncoder/encode (get kwp :sortby "score desc"))
         "&filter[field][type][]=" (get kwp :type "submission")
         "&start=" (URLEncoder/encode (str (get kwp :start 0)))
         "&limit=" (URLEncoder/encode (str (get kwp :limit 100)))
         (if (get kwp :username) 
           (str "&filter[fields][username][]=" (get kwp :username))
           "")
         (if (get kwp :date-interval)
           (str "&filter[fields][create_ts][]=" (get kwp :date-interval))
           "")
         (if (get kwp :username)
           (str "&filter[fields][username][]=" (get kwp :username))
           "")
         (if (get kwp :q)
           (str "&q=" (URLEncoder/encode (str (get kwp :q))))
           "")
         )))

(defn clean-text
  " Removes \n \t and html fields that passed through tika (#{header}...) "
  [text]
  (s/replace (s/replace 
               (s/replace text #"\n" " ") #"\t" " ") #"#\{[^}]*\}" " "))

(defn remove-markup
  " Remove HTML markup "
  [text]
  (s/replace text #"<[^>]*>" " "))

(defn clean-url
  " transforms http://paulgraham.com/fix.html in paulgraham.com-fix.html"
  [url]
  (let [wohttp (s/replace url #"http://" "")]
    (s/replace wohttp #"/" "-")))

(defn extract-url
  " Calls tika, hopefully with the right MIME (from headers), on the url "
  [& [url-domain]]
  (try 
    {:id (clean-url (first url-domain)), 
     :domain (second url-domain),
     :text (clean-text (:text (tika/parse (first url-domain))))}
    (catch Exception e (prn "Exception in extract-url with url: "
                            (first url-domain) " message: " (.getMessage e)))))

(defn extract-article
  " Prepare the json of the article (from thriftdb) for extract-url "
  [article-json]
  (let [url (:url (:item article-json)),
        domain (:domain (:item article-json)),
        id (:id (:item article-json))]
    (try 
      (if (= nil url)
        {:id id,
         :domain domain,
         :text (remove-markup (:text (:item article-json)))}
        {:id id, 
         :domain domain,
         :text (:text (extract-url [url domain]))})
      (catch Exception e (prn "Exception in extract-article with url: "
                              url " message: " (.getMessage e))))))

(defn write-down
  [article]
  (try
  (with-open [wrtr (writer ;(str folder-prefix (:domain article) (hash (:url article)) 
                           (str folder-prefix (:id article) ; HN ID if HN link, url otherwise
                           ".txt"))]
    (if (= false textonly)
      (.write wrtr (str ;(:url article) "\n")))
                        (:id article) "\n")))
    (.write wrtr (:text article)))
    (catch Exception e (prn "Exception in write-down: " (.getMessage e)))))

(defn clean-article
  [article]
  (let [id (:id article),
        domain (:domain article),
        text (:text article)]
    (defn remove-bad-NE ; NE stands for Named Entities
      [tokens]
      (let [bad-NE (set (concat (date-find tokens)
                                (money-find tokens)
                                (percentage-find tokens)
                                (time-find tokens)))]
           ;_ (prn "bad NE: " bad-NE)
           ;_ (prn "tokens: " tokens)] 
        (filter #(not (contains? bad-NE %)) tokens)))
    (defn remove-stopwords
      [tokens]
      (filter #(not (contains? stopwords %)) tokens))
    (let [filtered-text (s/join " " (remove-bad-NE (remove-stopwords (tokenize text))))]
          ;_ (prn filtered-text)]
      {:id id,
       :domain domain,
       :text filtered-text})))

(defn fetch-articles
  " Fetches articles starting at start and using optional-args, 
    to see what is allowed in optional args, look at encode-url "
  [start & optional-args]
  (let [_ (do (prn start) (prn optional-args))
        articles (:results (parse-string (:body 
     (http/get (format (encode-url hn-url :start start :limit 100 
                                  optional-args)))) true))]
        ;_ (prn articles)]
    (pmap (comp write-down clean-article extract-article) articles) 
    ; TODO test with map instead of pmap to remove timeouts/bugs
    (prn (str "processed: " (count articles) " articles"))))

(defn fetch-1000
  " Simplifies fetching the top 1000 according to a sortby "
  [sortby]
  (loop [start 0]
    (prn (str "pulling links from: " start " to " (+ start 100)))
    (fetch-articles start :sortby sortby)
    (if (< start 900)
      (recur (+ start 100))
      ())))

(defn fetch-loop-dates
  [date-interv]
  (loop [start 0]
    (fetch-articles start :date-interval date-interv)
    (if (< start 200)
      (recur (+ start 100))
      ())))

(defn fetch-all []
  " Currently takes PG essays 
  + the top 1000 of HN by points and by num_comments 
  + the top 200 according to thriftdb score for each day since Oct 9 2006 "
  (do
    (map (comp write-down clean-article extract-url) 
         (map (fn [a] [(str "http://paulgraham.com/" a) "paulgraham.com"]) 
              pgarticles))
    (map fetch-1000 sortby-list)
    (map fetch-loop-dates 
         (for [d (iterate #(t/plus % (t/days 1)) (t/date-time 2006 10 9)) :while (t/before? d (t/date-time 2012 11 18))] (str "[" d "+TO+" (t/plus d (t/days 1)) "]" )))))

