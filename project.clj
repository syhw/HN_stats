(defproject hackernews "1.0.0-SNAPSHOT"
  :description "Pulls top content from HN"
  :dev-dependencies [[vimclojure/server "2.3.6"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.5.3"]
                 [cheshire "4.0.3"]
                 [org.apache.mahout/mahout-core "0.5"]
                 [org.apache.mahout/mahout-math "0.5"]
                 [org.apache.mahout/mahout-utils "0.5"]
                 [incanter "1.3.0"]
                 [clj-tika "1.1.0"]]
  :main "hackernews.core")
