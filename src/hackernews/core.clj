(ns hackernews.core
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


(defn encode-url 
  " Encodes the URL for the thrifdb API call with the given url and params "
  [url & params]
  ; TEST (prn (encode-url hn-url :start 0 :limit 100))
  ; TEST (prn (encode-url hn-url :start 0 :limit 100 :sortby "points desc" :username "pg"))
  ; TEST (prn (encode-url hn-url :start 0 :limit 100 :date-interval "[2010-08-01T00:00:00Z TO 2010-08-01T23:59:59Z]"))
  (let [kwp (apply hash-map (flatten params))]
    (str url "sortby=" (URLEncoder/encode (get kwp :sortby "score desc"))
         "&filter[field][type][]=submission&start=" (URLEncoder/encode 
                                                      (str (get kwp :start 0)))
         "&limit=" (URLEncoder/encode (str (get kwp :limit 100)))
         (if (get kwp :username) 
           (str "&filter[fields][username][]=" (get kwp :username))
           "")
         (if (get kwp :date-interval)
           (str "&filter[fields][create_ts][]=" (get kwp :date-interval))
           ""))))

(defn clean-text
  " Removes \n \t and html fields that passed through tika (#{header}...) "
  [text]
  (s/replace (s/replace 
               (s/replace text #"\n" " ") #"\t" " ") #"#\{[^}]*\}" " "))

(defn remove-markup
  " Remove HTML markup "
  [text]
  (s/replace text #"<[^>]*>" " "))

(defn extract-article
  " Calls tika, hopefully with the right MIME (from headers), on the url "
  [article-json]
  (try 
  (let [url (:url (:item article-json)),
        domain (:domain (:item article-json))]
    (if (= nil url)
      {:url (str "http://news.ycombinator.com/item?id=" 
                 (first (s/split (:_id (:item article-json)) #"-"))),
       :domain domain,
       :text (remove-markup (:text (:item article-json)))}
      {:url url, 
       :domain domain,
       :text (clean-text (:text (tika/parse url)))}))
    (catch Exception e (prn "Exception: " (.getMessage e)))))
      ;{:url url, :text (clean-text (:text (try (tika/parse url) 
      ;                                     (catch Exception e (str "caught exception: " e)))))})))

(defn write-down
  [article]
  (try
  (with-open [wrtr (writer (str folder-prefix (:domain article)
                                (hash (:url article)) ".txt"))]
    (if (= false textonly)
      (.write wrtr (str (:url article) "\n")))
    (.write wrtr (:text article)))
    (catch Exception e (prn "Exception: " (.getMessage e)))))

(defn clean-article
  [article]
  (let [url (:url article),
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
      {:url url,
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

(defn fetch-all []
  " Currently takes the top 1000 by points and by num_comments 
    + the top 100 according to thriftdb score for each day since Oct 9 2006 "
  (do
    (map fetch-1000 sortby-list)
    (map (partial fetch-articles 0 :date-interval) 
         (for [d (iterate #(t/plus % (t/days 1)) (t/date-time 2006 10 9)) :while (t/before? d (t/date-time 2012 11 18))] (str "[" d "+TO+" (t/plus d (t/days 1)) "]" ))
         )))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

(defn -main [& args]
   (fetch-all)
  )
