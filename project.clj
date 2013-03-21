(defproject hackernews "1.0.0-SNAPSHOT"
  :description "Pulls top content from HN"
  :dev-dependencies [[vimclojure/server "2.3.6"]]
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [clj-http "0.6.5"]
                 [cheshire "5.0.2"]
                 [clojure-opennlp "0.2.0"]
                 ;[org.apache.mahout/mahout-core "0.7"]
                 ;[org.apache.mahout/mahout-math "0.7"]
                 ;[org.apache.mahout/mahout-utils "0.5"]
                 [incanter "1.4.1"]
                 [clj-tika "1.2.0"]
                 [clj-time "0.4.5"]]
  :main "hackernews.core"
  :jvm-opts ["-Xmx2048m"])
