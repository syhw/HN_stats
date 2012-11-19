(ns hackernews.porter-stemmer)

(def vowels #{\a \e \i \o \u})

(defn #^String but-last
  "Returns s without the last n characters.  Returns an empty string
  if n is greater than the length of s."
  [n #^String s]
  (if (< (count s) n)
    ""
    (.substring s 0 (- (count s) n))))

(defn #^String tail
  "Returns the last n characters of s."
  [n #^String s]
  (if (< (count s) n)
    s
    (.substring s (- (count s) n))))

(defn #^String chop
  "Removes the last character of string, does nothing on a zero-length
  string."
  [#^String s]
  (let [size (count s)]
    (if (zero? size)
      s
      (subs s 0 (dec (count s))))))

(defn- ends-with-any [s & args]
  (some true? (map (partial #(.endsWith %1 (str %2)) s) args)))

(defn- get-consonants [s]
  "A in a word is a letter other than A, E, I, O or U, and other
  than Y preceded by a consonant."
  (reduce (fn [v c]
            (cond (vowels c) (conj v false)
                  (= \y c) (conj v (not (peek v)))
                  :else (conj v true))) [] (vec s)))

(defn- measure [s]
  "Measure of any word or part of a word."
  (let [form (vec (partition-by identity (get-consonants s)))
        c (count form)
        vec-from (if (some true? (first form)) 1 0)
        vec-to (if (some false? (peek form)) (dec c) c)]
    (count (partition 2 (subvec form vec-from vec-to)))))

(defn- m? [comp n]
  (partial #(%1 (measure %3) %2) comp n))

(defn- *d? [s]
  "Returns true if the stem ends with a double consonant"
  (let [suffix (tail 2 s)
        types (get-consonants suffix)]
    (and (every? true? types)
         (= (first suffix)
            (second suffix)))))

(defn- *o? [s]
  "Returns true if the stem ends cvc where the second c is not w, x, or y"
  (let [suffix (tail 3 s)
        types (get-consonants suffix)]
    (and (= types [true false true])
         (and ((complement #{"w" "x" "y"}) (tail 1 suffix))))))

(defn- *v*? [s]
  (some false? (get-consonants s)))

(defn apply-rules [s & rules]
  "Applies the rules specified by rules to the string s."
  (let [rules (group-by #(:s1 %) rules)]
    (loop [keys (reverse (sort-by #(.length %) (keys rules)))]
      (if (empty? keys) s
        (let [s1 (first keys)
              stem (but-last (.length s1) s)]
          (if (.endsWith s s1) 
            (loop [r (rules s1)]
              (if (empty? r) s
                (let [{:keys [c? s1 s2 a] :or {c? (fn [_] true) a identity}} (first  r)]
                  (if (c? stem) (a (.concat stem s2))
                    (recur (next r))))))
            (recur (next keys))))))))

(defn step1-a [s]
  (apply-rules s
               {:s1 "sses" :s2 "ss"}
               {:s1 "ies"  :s2 "i" }
               {:s1 "ss"   :s2 "ss"}
               {:s1 "s"    :s2 ""  }))

(defn postprocess1-b [s]
  (letfn [(cond1? [s]
            (and (*d? s)
                 (not (ends-with-any s \l \s \z))))
          (cond2? [s]
            (and (= (measure s) 1)
                 (*o? s)))]
    (apply-rules s
      {:s1 "at" :s2 "ate"}
      {:s1 "bl" :s2 "ble"}
      {:s1 "iz" :s2 "ize"}
      {:c? cond1? :s1 "" :s2 "" :a #(chop %)}
      {:c? cond2? :s1 "" :s2 "e"})))

(defn step1-b [s]
  (apply-rules s
               {:c? (m? > 0) :s1 "eed" :s2 "ee"}
               {:c? *v*?   :s1 "ed"  :s2 "" :a #(postprocess1-b %)}
               {:c? *v*?   :s1 "ing" :s2 "" :a #(postprocess1-b %)}))

(defn step1-c [s]
  (apply-rules s
               {:c? *v*? :s1 "y" :s2 "i"}))

(defn step2 [s]
  (apply-rules s
               {:c? (m? > 0) :s1 "ational" :s2 "ate"}
               {:c? (m? > 0) :s1 "tional"  :s2 "tion"}
               {:c? (m? > 0) :s1 "logi"    :s2 "log"}
               {:c? (m? > 0) :s1 "enci"    :s2 "ence"}
               {:c? (m? > 0) :s1 "anci"    :s2 "ance"}
               {:c? (m? > 0) :s1 "izer"    :s2 "ize"}
               {:c? (m? > 0) :s1 "bli"     :s2 "ble"}
               {:c? (m? > 0) :s1 "alli"    :s2 "al"}
               {:c? (m? > 0) :s1 "entli"   :s2 "ent"}
               {:c? (m? > 0) :s1 "eli"     :s2 "e"}
               {:c? (m? > 0) :s1 "ousli"   :s2 "ous"}
               {:c? (m? > 0) :s1 "ization" :s2 "ize"}
               {:c? (m? > 0) :s1 "ation"   :s2 "ate"}
               {:c? (m? > 0) :s1 "ator"    :s2 "ate"}
               {:c? (m? > 0) :s1 "alism"   :s2 "al"}
               {:c? (m? > 0) :s1 "iveness" :s2 "ive"}
               {:c? (m? > 0) :s1 "fulness" :s2 "ful"}
               {:c? (m? > 0) :s1 "ousness" :s2 "ous"}
               {:c? (m? > 0) :s1 "aliti"   :s2 "al"}
               {:c? (m? > 0) :s1 "iviti"   :s2 "ive"}
               {:c? (m? > 0) :s1 "biliti"   :s2 "ble"}))

(defn step3 [s]
  (apply-rules s
               {:c? (m? > 0) :s1 "icate" :s2 "ic"}
               {:c? (m? > 0) :s1 "ative" :s2 ""}
               {:c? (m? > 0) :s1 "alize" :s2 "al"}
               {:c? (m? > 0) :s1 "iciti" :s2 "ic"}
               {:c? (m? > 0) :s1 "ical"  :s2 "ic"}
               {:c? (m? > 0) :s1 "ful"   :s2 ""}
               {:c? (m? > 0) :s1 "ness"  :s2 ""}))

(defn step4 [s]
  (letfn [(cond? [s]
            (and ((m? > 1) s) (ends-with-any s \s \t)))]
    (apply-rules s
      {:c? (m? > 1) :s1 "al"    :s2 ""}
      {:c? (m? > 1) :s1 "ance"  :s2 ""}
      {:c? (m? > 1) :s1 "ence"  :s2 ""}
      {:c? (m? > 1) :s1 "er"    :s2 ""}
      {:c? (m? > 1) :s1 "ic"    :s2 ""}
      {:c? (m? > 1) :s1 "able"  :s2 ""}
      {:c? (m? > 1) :s1 "ible"  :s2 ""}
      {:c? (m? > 1) :s1 "ant"   :s2 ""}
      {:c? (m? > 1) :s1 "ement" :s2 ""}
      {:c? (m? > 1) :s1 "ment"  :s2 ""}
      {:c? (m? > 1) :s1 "ent"   :s2 ""}
      {:c? cond?    :s1 "ion"    :s2 ""}
      {:c? (m? > 1) :s1 "ou"    :s2 ""}
      {:c? (m? > 1) :s1 "ism"   :s2 ""}
      {:c? (m? > 1) :s1 "ate"   :s2 ""}
      {:c? (m? > 1) :s1 "iti"   :s2 ""}
      {:c? (m? > 1) :s1 "ous"   :s2 ""}
      {:c? (m? > 1) :s1 "ive"   :s2 ""}
      {:c? (m? > 1) :s1 "ize"   :s2 ""})))

(defn step5-a [s]
  (letfn [(cond? [s]
            (and ((m? = 1) s) (not (*o? s))))]
    (apply-rules s
      {:c? (m? > 1) :s1 "e" :s2 ""}
      {:c? cond?     :s1 "e" :s2 ""})))

(defn step5-b [s]
  (letfn [(cond? [s]
            (and ((m? > 1) s)
                 (*d? s)
                 (ends-with-any s \l)))]
    (apply-rules s
      {:c? cond? :s1 "" :s2 "" :a #(chop %)})))

(defn porter-stemmer [s]
  (if (> (.length s) 2)
    (-> s
      step1-a
      step1-b
      step1-c
      step2
      step3
      step4
      step5-a
      step5-b)
    s))

; TEST (porter-stemmer "misses")
