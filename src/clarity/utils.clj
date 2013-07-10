(ns clarity.utils)

;; ----------------
;; Inner namespaces
;; ----------------

(defmacro inner-namespace [name & forms]
  (let [outer-ns (ns-name *ns*)
        inner-ns (symbol (str outer-ns "." name))]
   `(do
      (if (find-ns '~inner-ns)
        (in-ns '~inner-ns)
        (ns ~inner-ns))
      ~@forms
      (in-ns '~outer-ns)
      (use '~inner-ns))))

;; -------------
;; Reader macros
;; -------------

(inner-namespace reader-macros
  (use '[clarity.reader [utils :rename {peek rpeek}] macros])
  (import clojure.lang.Util)

  (defn colon-reader [reader c]
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

  (defn read-literal [reader end]
    (loop [s ""]
      (if (.endsWith s end)
        (.substring s 0 (- (.length s) (.length end)))
        (if (rpeek reader)
          (recur (str s (read-1 reader)))
          (Util/runtimeException "EOF while reading literal string.")))))

  (defn literal-string-reader [reader _]
    (let [string-reader (get-default-macro \")]
      (if-not (= (rpeek reader) \")
        (string-reader reader (int \"))
        (do
          (read-1 reader)
          (if-not (= (rpeek reader) \")
            ""
            (do
              (read-1 reader)
              (read-literal reader "\"\"\"")))))))
)

(def colon {:char \: :reader colon-reader})
(def literal-string {:char \" :reader literal-string-reader})

;; ------
;; Lambda
;; ------

(defmacro λ [& exprs]
  (let [[args body] (if (and (> (count exprs) 1)
                             (= clojure.lang.PersistentVector (-> exprs first class)))
                      [(first exprs) (rest exprs)]
                      [nil exprs])
        body        (if (or args (= (count body) 1))
                      (conj body 'do)
                      body)
        args        (or args '[& [% %2 %3 %4 %5 :as %s]])]
    `(fn ~args ~body)))

; (defmacro l [& exprs] `(λ ~@exprs))

;; -----
;; Infix
;; -----

(inner-namespace infix

  (defn list?' [form]
    (or (list? form)
        (= (type form) clojure.lang.LazySeq)
        (= (type form) clojure.lang.Cons)))

  (defn infix-sym [form]
    (cond
      (and (symbol? form) (not (re-find #"[a-zA-Z0-9]" (name form))))
        form
      (and (list?' form) (= (count form) 2) (= (first form) 'quote))
        (second form)))

  (defn infixify* [[first second & rest :as list]]
    (if-not (infix-sym first)
      (if-let [second (infix-sym second)]
        (concat [second first] rest)
        list)
      list))

  (defn infixify [form]
    (if (list?' form)
      (map infixify (infixify* form))
      form))
)

(defmacro infix
  "Walks over its argument forms, infixing them.
  The second element of a function is treated as
  an infix element if it contains no alphnumeric
  characters or is quoted, e.g.
    (infix (1 + (2 * (14 'mod 10)))) ;=> 9

  Currently only the second argument is checked,
  i.e. no (2 * 3 * 4)"
  [form]
  (infixify form))

(defmacro |
  "See `infix`."
  [& form]
  `(infix ~(apply list form)))

;; -----
;; Quote
;; -----

;; Implementation of `quote` with unquoting

(inner-namespace quote

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
)

(defmacro quote*
  "Like quote, but supports unquote (`~`, `~@`)."
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

(defn symbol-extract [s]
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

;; ------
;; Queues
;; ------

(defn queue [] (atom []))

(defmacro queued
  "`queued` code blocks will wait for each other
  to finish before executing.

    (time (dorun (pmap #(do (Thread/sleep 100) %) (range 10))))
  ;=> Elapsed time: 100.221453 msecs

    (def q (queue))
    (time (dorun (pmap #(queued q (Thread/sleep 100) %) (range 10))))
   ;=> Elapsed time: 1090.702535 msecs"
  [q & exprs]
 `(let [id# (last (swap! ~q #(conj % (inc (or (last %) 0)))))]
    (while (not= id# (first @~q))
      (Thread/sleep 10))
    (let [result# (do ~@exprs)]
      (swap! ~q subvec 1)
      result#)))

;; --------------
;; Records as fns
;; --------------

(inner-namespace defnrecord

(def fn-sym (atom 'f))

(defn sym-list [n]
  (->> (range n) (map inc) (map #(str "arg" %)) (map symbol)))

(defn invoke-fn [n]
  (let [syms (sym-list n)]
    `(~'invoke [~'this ~@syms] (~(deref fn-sym) ~'this ~@syms))))

(defn invoke-fns []
  (map invoke-fn (range 20)))
)

(defmacro defnrecord [name vars f & rest]
  (reset! fn-sym (gensym "fn"))
 `(do
    (def ~(deref fn-sym) ~f)
    (defrecord ~name ~vars
      clojure.lang.IFn
        ~@(invoke-fns)
      ~@rest)
    (ns-unmap *ns* '~(deref fn-sym))))
