(ns hackernews.stalker
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [tika]
            [hackernews.core :refer :all])
  (:import [java.net URLEncoder])
  (:use [cheshire.core]))

; http://api.thriftdb.com/api.hnsearch.com/items/_search?start=0&limit=100&filter[fields][username][]="pg"

(defn stalk
  [username]
  (defn from-coms [a]
    (let [submission (first (:results (parse-string (:body
                (http/get (encode-url hn-url :start 0 :limit 2
                :q (:title (:discussion (:item a))) :type "submission"))) true)))]
      (prn submission)
      (prn a)
      (prn (:id (:discussion a)))
     ; ((comp write-down clean-article extract-article) submission) TODO (try/catch url 404 and other failures)
      (:id (:item submission))))
  (defn from-subs [a]
    (do
     ; ((comp write-down clean-article extract-article) a) TODO
      (:id (:item a))))
  (loop [start 0
         coms '()
         submissions '()]
    (let [foldername (str "data/" username)
          results-coms (:results (parse-string (:body
                                                 (http/get (format (encode-url hn-url :start start :limit 100 
                                                                               :username username :type "comment")))) true))
          results-subs (:results (parse-string (:body
                                                 (http/get (format (encode-url hn-url :start start :limit 100 
                                                                               :username username :type "submission")))) true))
          essays (if (= username "pg")
                   (map (fn [a] [(str "http://paulgraham.com/" a) "paulgraham.com"]) pgarticles)
                   ["" ""])]
      (if (< start 900)
        (recur (+ start 100)
               (concat coms (map from-coms results-coms))
               (concat submissions (map from-subs results-subs) (map first essays)))
        (let [interesting (set (concat coms submissions))]
          (.mkdir (file foldername))
          (with-open [wrtr (writer (str foldername "/interesting_articles.txt"))]
            (.write wrtr (s/join " " interesting))))))))
       

(stalk "pg")
(stalk "kn0thing")
(stalk "pb") ; NOTHING
(stalk "sama")
(stalk "kirsty") ; FEW
(stalk "jl")
(stalk "rtm")
(stalk "garry")
(stalk "tlb")
(stalk "justin")
(stalk "emmit") ; NOTHING
(stalk "harj")
(stalk "aaroniba")
(stalk "emillon")
(stalk "snippyhollow")
