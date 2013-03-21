(ns hackernews.stalker
  (:require [clojure.java.io :refer :all]
            [clojure.string :as s]
            [clj-http.client :as http]
            [hackernews.fetcher :refer :all])
  (:import [java.net URLEncoder])
  (:use [cheshire.core]))

; http://api.thriftdb.com/api.hnsearch.com/items/_search?start=0&limit=100&filter[fields][username][]="pg"

(defn stalk [uname]
  (prn "NOT IMPLEMENTED (see stalker.clj + comments)"))

(comment (defn stalk
  [username]
  (defn from-coms [a]
    (try 
      (let [dummy 0]
        [submission (first ; should check for second (and so on) if first is not the correct result, i.e. (:id (:item submission)) != (:id (:discussion a)) TODO
                              (:results (parse-string (:body
                                (http/get (encode-url hn-url :start 0 :limit 2
            :q (:title (:discussion (:item a))) :type "submission"))) true)))]
        (if (and true
              (not= submission nil) 
              (= (:id (:item submission)) (:id (:discussion a)))
              (= (:status (http/get (:url (:item submission)))) 200)
                 )
          (do
            (prn (str "extracting article (submission): " (:id (:item submission))))
            (prn (str "extracting article (dicussion): " (:id (:discussion a))))
            ((comp write-down clean-article extract-article) submission) ; TODO DEBUG THIS FUCK
            (:id (:item submission)) ;/////////////
             (:id (:discussion a)))
          "")
      )
      (catch Exception e "")))
  (defn from-subs [a]
    (try 
      (if (= (:status (http/get (:url (:item a)))) 200)
        (do
          (prn (str "extracting article: " (:id (:item a))))
          ((comp write-down clean-article extract-article) a)
          (:id (:item a)))
        "")
      (catch Exception e "")))
  (do 
    (prn (str "stalking: " username))
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
              (.write wrtr (s/join " " interesting))))))))) 
         )
       
