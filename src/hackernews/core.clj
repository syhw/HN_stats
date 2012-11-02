(ns hackernews.core
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [tika])
  (:import [java.net URLEncoder])
  (:use [cheshire.core])
  (:gen-class))

(def hn-url "http://api.thriftdb.com/api.hnsearch.com/items/_search?")
(def folder-prefix "data/")

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

(defn extract-article
  " Calls tika, hopefully with the right MIME (from headers), on the url "
  [article-json]
  (let [url (:url (:item article-json))]
    {:url url, :text (clean-text (:text (tika/parse url)))}))

(defn write-down 
  [article]
  (with-open [wrtr (writer (str folder-prefix (hash (:url article)) ".txt"))]
    (.write wrtr (str (:url article) "\n"))
    (.write wrtr (:text article))))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

(defn -main [& args]
  (loop [start 0]
    (map write-down (map extract-article (:results (parse-string (:body 
        (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))
    (if (< start 900)
      (recur (+ start 100))
      ()))

