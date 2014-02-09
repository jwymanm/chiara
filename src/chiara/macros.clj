(ns chiara.macros)

;; -----
;; Infix
;; -----

(defn ^:private infix-sym [form]
  (cond
    (and (symbol? form) (not (re-find #"[a-zA-Z0-9]" (name form))))
      form
    (and (seq? form) (= (count form) 2) (= (first form) 'quote))
      (second form)))

(defn ^:private infixify* [[first second & rest :as list]]
  (if-not (infix-sym first)
    (if-let [second (infix-sym second)]
      (concat [second first] rest)
      list)
    list))

(defn infixify [form]
  (if (seq? form)
    (map infixify (infixify* form))
    form))

(defmacro infix
  "Walks over its argument forms, infixing them.
  The second element of a function is treated as
  an infix element if it contains no alphnumeric
  characters or is quoted, e.g.
    (infix (1 + (2 * (14 'mod 10)))) ;=> 9

  Currently only the second argument is checked,
  i.e. no (2 * 3 * 4) - this should be improved."
  [form]
  (infixify form))

;; -----
;; Quote
;; -----

;; Implementation of `quote` with unquoting

(defn seqable? [e]
  (if-not (string? e)
    (try (seq e)
      (catch Exception e false))))

(defn empty' [coll]
  (if (= (type coll) clojure.lang.MapEntry)
    []
    (empty coll)))

(defn unquote-form? [expr]
  (and (seq? expr)
       (= (first expr) `unquote)))

(defn unquote-splicing-form? [expr]
  (and (seq? expr)
       (= (first expr) `unquote-splicing)))

(defmacro quote*
  "Like quote, but supports unquoting (`~`, `~@`)."
  [expr]
  (cond
    (unquote-form? expr) (second expr)

    (seq? expr)          (reduce #(if (unquote-splicing-form? %2)
                                    `(concat ~(second %2) ~%1)
                                    `(conj ~%1 (quote* ~%2)))
                                 ()
                                 (reverse expr))

    (seqable? expr)      `(into ~(empty' expr) (quote* ~(seq expr)))
    (symbol?  expr)      `'~expr
    :else                expr))

;; Strings with unquoting.

;; Planned: ability to extract forms e.g. "~(range 10)"

(defn symbol-extract
  "Takes a string with unquoted vars
  e.g. \"a b ~c d\"
  and turns it into a list of string
  and symbols
  e.g. (\"a b \" c \"d\") "
  [s]
  (map #(if-let [n (second (re-find #"\A\~([\w\-]+)\Z" %))]
                  (symbol n)
                  %)
       (re-seq #"[^\~]+|\~[\w\-0-9]*" s)))

(defmacro i-str
  "Like str but interpolates symbols
  preceded by ~ into the string.
  e.g.
    (let [x 'dave]
      (i-str \"hi ~x\"))
  ;=> hi dave"
  [s]
 `(str ~@(symbol-extract s)))

;; --------------
;; Records as fns
;; --------------

(def ^:private fn-sym (atom 'f))

(defn ^:private sym-list [n]
  (->> (range n) (map inc) (map #(str "arg" %)) (map symbol)))

(defn ^:private invoke-fn [n]
  (let [syms (sym-list n)]
    `(~'invoke [~'this ~@syms] (~(deref fn-sym) ~'this ~@syms))))

(defn ^:private invoke-fns []
  (map invoke-fn (range 20)))

(defmacro defnrecord [name vars f & rest]
  (reset! fn-sym (gensym "fn"))
 `(do
    (def ~(deref fn-sym) ~f)
    (defrecord ~name ~vars
      clojure.lang.IFn
        ~@(invoke-fns)
      ~@rest)
    (ns-unmap *ns* '~(deref fn-sym))))

(defmacro defntype [name vars f & rest]
  (reset! fn-sym (gensym "fn"))
 `(do
    (def ~(deref fn-sym) ~f)
    (deftype ~name ~vars
      clojure.lang.IFn
        ~@(invoke-fns)
      ~@rest)
    (ns-unmap *ns* '~(deref fn-sym))))
