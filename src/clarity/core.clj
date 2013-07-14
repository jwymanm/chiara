(ns clarity.core
  (:refer-clojure :exclude [peek])
  (use clarity.reader.utils
       clarity.syntax))

(defn seq' [form]
  (if (= (count form) 1)
    (first form)
    (apply list form)))

(defn discard-line [reader]
  (while (not= (read-1 reader) \newline)))

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
    (loop [form [(read-next reader)]
           [next-i newline] (read-whitespace reader)]
      (if (peek reader)
        (if newline
          (if (> next-i indent)
            (let [[next-form next-i] (read-clarity reader next-i)]
              (recur (conj form next-form) [next-i true]))
            [(seq' form) next-i])
          (recur (conj form (read-next reader)) (read-whitespace reader)))
        [(seq' form) 0]))))

(defn read-all [reader]
  (loop [form []]
    (read-whitespace reader)
    (if (peek reader)
      (recur (conj form (read-clarity reader)))
      form)))

(defn clarity-reader [reader char]
  (if (= (read-1 reader) \:)
    (conj (seq (read-all reader)) 'do)
    (read-clarity reader)))

(defsyntax clarity [s]
  (-> s string-reader read-all seq (conj 'do)))

; (defn use-clarity
;   ([] (use-clarity true))
;   ([read-macros]
;     (use-syntax clarity)
;     (when read-macros
;       (use-reader-macros colon raw-string))))
