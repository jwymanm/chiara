(ns chiara
  (:refer-clojure :exclude [peek])
  (use [chiara.reader utils macros]
        chiara.syntax))

;; -------------
;; Chiara Reader
;; -------------

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

(defn read-chiara
  ([reader] (first (read-chiara reader (first (read-whitespace reader)))))
  ([reader indent]
    (let [[l c] (reader-position reader)]
      (loop [form [(read-next reader)]
             [next-i newline] (read-whitespace reader)]
        (if (peek reader)
          (if newline
            (if (> next-i indent)
              (let [[next-form next-i] (read-chiara reader next-i)]
                (recur (conj form next-form) [next-i true]))
              [(seq' form {:line l :column c}) next-i])
            (recur (conj form (read-next reader)) (read-whitespace reader)))
          [(seq' form {:line l :column c}) 0])))))

;; Catch EOF here
(defn read-to-bracket [reader]
  (read-whitespace reader)
  (if (= (str (peek reader)) ")")
    (do (read-1 reader) ())
    (let [next (read-chiara reader)]
      (conj (read-to-bracket reader) next))))

(defsyntax ^:stream chiara [reader]
  (-> reader read-to-bracket (conj 'do)))

;; ------------
;; Chiara Utils
;; ------------

;; -------------
;; Reader macros
;; -------------

(import clojure.lang.Util)

(defn ^:private colon-reader [reader c]
  (let [next (read-1 reader)]
    (if-not (= next \space)
      ; Keyword
      (do
        (unread reader next)
        (-> reader read-next keyword))
      ; Implicit bracket
      (loop [list []
             c    (read-1 reader)]
        (cond
          (#{\space \,} (char c)) (recur list (read-1 reader))
          (#{"\n" ")" "]" "}" nil} (str (char c))) (do (unread reader c) (seq list))
          :else                  (do
                                   (unread reader c)
                                   (recur (conj list (read reader))
                                          (read-1 reader))))))))

(defn ^:private read-literal [reader end]
  (loop [s ""]
    (if (.endsWith s end)
      (.substring s 0 (- (.length s) (.length end)))
      (if (peek reader)
        (recur (str s (read-1 reader)))
        (Util/runtimeException "EOF while reading literal string.")))))

(defn ^:private literal-string-reader [reader _]
  (let [string-reader (get-default-macro \")]
    (if-not (= (peek reader) \")
      (string-reader reader (int \"))
      (do
        (read-1 reader)
        (if-not (= (peek reader) \")
          ""
          (do
            (read-1 reader)
            (read-literal reader "\"\"\"")))))))

(def colon
  "Reader macro. Wraps forms to the right of it
  in brackets e.g.
    (map inc : range 10) => (map inc (range 10))
  Will insert the right bracket either at the
  next unmatched bracket or at the end of the
  line, whichever makes sense."
  {:char \: :reader colon-reader})

(def raw-string
  "Reader macro for triple-quoted raw strings."
  {:char \" :reader literal-string-reader})

;; ------
;; Lambda
;; ------

(defmacro λ
  "A smart replacement for both fn and #(),
  avoiding common pitfalls of the reader macro.
  If no parameter list if given:
    * A body which is a single form will not
      be treated as a function call.
    * The fn won't complain about arity.

  A parameter list is interpreted if there is
  more than one form and the first is a vector.
  e.g.
  ((λ println %) :hi)   => :hi
  ((λ (println %)) :hi) => :hi
  ((λ [% %2]) 1 2 3)    => [1 2]
  ((λ 1) :arg)          => 1
  ((λ [x y] y) 1 3)     => 3"
  [& exprs]
  (let [[args body] (if (and (> (count exprs) 1)
                             (= clojure.lang.PersistentVector (-> exprs first class)))
                      [(first exprs) (rest exprs)]
                      [nil exprs])
        body        (if (or args (= (count body) 1))
                      (conj body 'do)
                      body)
        args        (or args '[& [% %2 %3 %4 %5 :as %s]])]
    `(fn ~args ~body)))

;; -----------
;; Better cond
;; -----------

(defmacro cond' [& forms]
  `(cond ~@(apply concat forms)))

;; -------------
;; Usage Utility
;; -------------

(defn use-chiara
  ([] (use-chiara true))
  ([read-macros]
    (use-syntax chiara)
    (when read-macros
      (use-reader-macros colon raw-string))))
