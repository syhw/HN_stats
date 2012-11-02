(ns hackernews.core
  (:require [clojure.java.io :refer :all]
            [clj-http.client :as http]
            [tika])
  (:import [java.net URLEncoder])
  (:use [cheshire.core])
  (:gen-class))

;(def filename "TEST.html")
;(def filename "TEST2.html")
;(def filename "TEST3.pdf")

(def hn-url "http://api.thriftdb.com/api.hnsearch.com/items/_search?")

(defn encode-url 
  [url & params]
  ; TEST (prn (encode-url hn-url :start 0 :limit 100))
  (let [kwp (apply hash-map params)]
    (str url "sortby=" (URLEncoder/encode (get kwp :sortby "points desc")) 
         "&filter[field][type][]=submission&start=" (URLEncoder/encode 
                                                      (str (get kwp :start 0)))
         "&limit=" (URLEncoder/encode (str (get kwp :limit 100))))))
; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?

(defn extract-article
  [article-json]
  (:text (tika/parse (:body (http/get (:url (:item article-json)))))))

(defn -main [& args]
;  (loop [start 0]
;    DO SOMETHING
;    (if (< start 900)
;      (recur (+ start 100))
;      ()))
 ; (map extract-article (:results (parse-string (:body (http/get 
 ;                 (format (encode-url hn-url :start 0 :limit 100)))) true))))
  ;(prn (:text (tika/parse (:body (http/get "http://en.wikipedia.org/wiki/Mercury-Redstone_1"))))))
  (keys (http/get "http://emotion.inrialpes.fr/people/synnaeve/index_files/dataset_army_composition.pdf"))
  (:headers (http/get "http://emotion.inrialpes.fr/people/synnaeve/index_files/dataset_army_composition.pdf"))
  (:text (tika/parse "TEST3.pdf"))
  (type (:body (http/get "http://emotion.inrialpes.fr/people/synnaeve/index_files/dataset_army_composition.pdf")))
  (prn (:text (tika/parse "http://emotion.inrialpes.fr/people/synnaeve/index_files/dataset_army_composition.pdf")))
  )
  ;(prn (:text (tika/parse (as-file (:body (http/get "http://emotion.inrialpes.fr/people/synnaeve/index_files/dataset_army_composition.pdf")))))))

