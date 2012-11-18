(ns hackernews.name_recog
  (:require [clojure.string :as s])
  (:use [opennlp.nlp]))

(def tokenize (make-tokenizer "models/en-token.bin"))
(def date-find (make-name-finder "models/en-ner-date.bin"))
(def location-find (make-name-finder "models/en-ner-location.bin"))
(def money-find (make-name-finder "models/en-ner-money.bin"))
(def org-find (make-name-finder "models/en-ner-organization.bin"))
(def percentage-find (make-name-finder "models/en-ner-percentage.bin"))
(def person-find (make-name-finder "models/en-ner-person.bin"))
(def time-find (make-name-finder "models/en-ner-time.bin"))

; TEST (s/join " " (tokenize "This is a test."))
; TEST (name-find (tokenize "My name is Lee, not John."))
; TEST (date-find (tokenize "We are Tuesday March 2."))
; TEST (location-find (tokenize "I live 5 rue Primatice 75013 Paris."))
; TEST (let [phrase (tokenize "I will be at 5 rue Primatice 75013 Paris with $300 Monday March 12th.")] (apply hash-set (concat (money-find phrase) (date-find phrase))))
