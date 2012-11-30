(ns hackernews.core
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [tika]
            [hackernews.name_recog :refer :all]
            [hackernews.stalker :refer :all]
            [hackernews.fetcher :refer :all])
  (:import [java.net URLEncoder])
  (:use [cheshire.core])
  (:gen-class))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

(defn -main [& args]
   ;(fetch-all)
    (stalk "jl")
  )
