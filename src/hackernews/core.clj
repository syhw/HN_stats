(ns hackernews.core
  (:require [hackernews.name_recog :refer :all]
            [hackernews.stalker :refer :all]
            [hackernews.fetcher :refer :all]
            [clj-time.core :as t])
  (:gen-class))

; TEST (prn (:url (:item (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))))
; TEST (prn (:text (tika/parse "test/dataset_army_composition.pdf"))) doesn't work, pdf can't be read TODO ?
; TEST (write-down (extract-article (first (:results (parse-string (:body (http/get (format (encode-url hn-url :start 0 :limit 100)))) true)))))

(defn -main [& args]
  ;(fetch-all)
  (map fetch-loop-dates (for [d (iterate #(t/plus % (t/days 1)) (t/date-time 2009 1 1)) :while (t/before? d (t/date-time 2010 01 01))] (str "[" d "+TO+" (t/plus d (t/days 1)) "]" )))
;  (stalk "pg")
;  (stalk "kn0thing")
;  (stalk "sama")
;  (stalk "kirsty") ; FEW
;  (stalk "jl")
;  (stalk "rtm")
;  (stalk "garry")
;  (stalk "tlb")
;  (stalk "justin")
;  (stalk "harj")
;  (stalk "paul")
;  (stalk "aaroniba")
;  (stalk "emillon")
;  (stalk "snippyhollow")
;  (stalk "tptacek")
;  (stalk "patio11")
;  (stalk "edw519")
;  (stalk "jacquesm")
;  (stalk "fogus")
;  (stalk "cwan")
;  (stalk "llambda")
;  (stalk "jrockway")
;  (stalk "raganwald")
;  (stalk "ssclafani")
;  (stalk "cperciva")
  )
