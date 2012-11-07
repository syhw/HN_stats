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
(def sortby "num_comments") ; points or num_comments

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
  (let [url (:url (:item article-json)),
        domain (:domain (:item article-json))]
    (if (= nil url)
      {:url (str "http://news.ycombinator.com/item?id=" 
                 (first (s/split (:_id (:item article-json)) #"-"))),
       :domain domain,
       :text (remove-markup (:text (:item article-json)))}
      {:url url, 
       :domain domain,
       :text (clean-text (:text (tika/parse url)))})))
      ;{:url url, :text (clean-text (:text (try (tika/parse url) 
      ;                                     (catch Exception e (str "caught exception: " e)))))})))

(defn write-down
  [article]
  (with-open [wrtr (writer (str folder-prefix (:domain article)
                                (hash (:url article)) ".txt"))]
    (if (= false textonly)
      (.write wrtr (str (:url article) "\n")))
    (.write wrtr (:text article))))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

;(clean-text (:text (tika/parse (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true))))))))
;(extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true))))
;(for [l (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 5)))) true))] (prn (str "item: " l)))
;(map extract-article (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 5)))) true)))
;(map write-down (map extract-article (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 5)))) true))))

(defn fetch-articles []
  (loop [start 0]
    (prn (str "pulling links from: " start " to " (+ start 100)))
    (let [articles (:results (parse-string (:body 
       (http/get (format (encode-url hn-url :start start :limit 100 :sortby sortby)))) true))]
      (pmap (comp write-down extract-article) articles)
      (prn (str "processed: " (count articles) " articles")))
    (if (< start 900)
      (recur (+ start 100))
      ())))

(defn -main [& args]
  (do
    (fetch-articles)
    ))

