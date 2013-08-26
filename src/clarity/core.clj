(ns clarity.core
  (:refer-clojure :exclude [peek])
  (use [clarity.reader utils macros]
        clarity.syntax
        clarity.utils))

(defn seq'
  ([form]
    (if (= (count form) 1)
      (first form)
      (apply list form)))
  ([form meta]
    (let [form (seq' form)]
      (if (instance? clojure.lang.IMeta form)
        (with-meta form meta)
        form))))

(defn read-whitespace [reader]
  (loop [count 0
         newline false]
    (let [c (read-1 reader)]
      (condp = c
        \space   (recur (inc count) newline)
        \tab     (recur (+ count 4) newline)
        \newline (recur 0 true)
        \;       (do (discard-line reader) (recur 0 true))
        nil      [count newline]
        (do (unread reader c) [count newline])))))

(defn read-clarity
  ([reader] (first (read-clarity reader (first (read-whitespace reader)))))
  ([reader indent]
    (let [[l c] (reader-position reader)]
      (loop [form [(read-next reader)]
             [next-i newline] (read-whitespace reader)]
        (if (peek reader)
          (if newline
            (if (> next-i indent)
              (let [[next-form next-i] (read-clarity reader next-i)]
                (recur (conj form next-form) [next-i true]))
              [(seq' form {:line l :column c}) next-i])
            (recur (conj form (read-next reader)) (read-whitespace reader)))
          [(seq' form {:line l :column c}) 0])))))

;; Catch EOF here
(defn read-to-bracket [reader]
  (read-whitespace reader)
  (if (= (str (peek reader)) ")")
    (do (read-1 reader) ())
    (let [next (read-clarity reader)]
      (conj (read-to-bracket reader) next))))

(defsyntax ^:stream clarity [reader]
  (-> reader read-to-bracket (conj 'do)))

(defn use-clarity
  ([] (use-clarity true))
  ([read-macros]
    (use-syntax clarity)
    (when read-macros
      (use-reader-macros colon raw-string))))
