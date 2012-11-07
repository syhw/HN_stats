(ns hackernews.name_recog
  (:use [opennlp.nlp]))

(def tokenize (make-tokenizer "models/en-token.bin"))
(def date-find (make-name-finder "models/en-ner-date.bin"))
(def location-find (make-name-finder "models/en-ner-location.bin"))
(def money-find (make-name-finder "models/en-ner-money.bin"))
(def org-find (make-name-finder "models/en-ner-organization.bin"))
(def percentage-find (make-name-finder "models/en-ner-percentage.bin"))
(def person-find (make-name-finder "models/en-ner-person.bin"))
(def time-find (make-name-finder "models/en-ner-time.bin"))

; TEST (name-find (tokenize "My name is Lee, not John."))

