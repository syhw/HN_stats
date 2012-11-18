(ns hackernews.core
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [tika]
            [hackernews.name_recog :refer :all])
  (:import [java.net URLEncoder])
  (:use [cheshire.core])
  (:gen-class))

(def hn-url "http://api.thriftdb.com/api.hnsearch.com/items/_search?")
(def folder-prefix "data/")
(def textonly true)
(def sortby "num_comments desc") ; points or num_comments
(def stopwords (set (s/split (slurp "models/common-english-words.txt") #",")))

(defn encode-url 
  [url & params]
  ; TEST (prn (encode-url hn-url :start 0 :limit 100))
  (let [kwp (apply hash-map params)]
    (str url "sortby=" (URLEncoder/encode (get kwp :sortby "points desc")) 
         "&filter[field][type][]=submission&start=" (URLEncoder/encode 
                                                      (str (get kwp :start 0)))
         "&limit=" (URLEncoder/encode (str (get kwp :limit 100))))))

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

(defn fetch-articles []
  (loop [start 0]
    (prn (str "pulling links from: " start " to " (+ start 100)))
    (let [articles (:results (parse-string (:body 
       (http/get (format (encode-url hn-url :start start :limit 100 :sortby sortby)))) true))]
          ;_ (prn articles)]
      (pmap (comp write-down clean-article extract-article) articles) ; TODO test with map to remove timeouts/bugs
;      (prn (map extract-article articles))
;      (prn (map (comp clean-article extract-article) articles))
      (prn (str "processed: " (count articles) " articles")))
    (if (< start 900)
      (recur (+ start 100))
      ())))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

(defn -main [& args]
  (do
   (fetch-articles)
    ))
