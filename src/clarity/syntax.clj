(ns clarity.syntax
  (:refer-clojure :exclude [peek])
  (use [clarity.reader [hacking :exclude [queued queue]] utils]
       [clarity.utils]))

(defn ^:private current-ns
  "Return the name of the current
  ns as a symbol."
  [] (ns-name *ns*))

(defn ^:private read-literal
  "Reads chars into a string (verbatim) until
  the next unmatched ')'."
  [reader]
  (loop [chars []
         brackets 0]
    (let [char (read-1 reader)]
      (cond
        (not char)
          (read reader)
        (= "(" (str char))
          (recur (conj chars char) (inc brackets))
        (= ")" (str char))
          (if (zero? brackets)
            (apply str chars)
            (recur (conj chars char) (dec brackets)))
        :else
          (recur (conj chars char) brackets)))))

;; Currently only works when macros are unqualified
(defn ^:private get-syntax-macros []
  (->> (merge (ns-refers *ns*) (ns-interns *ns*))
       (filter #(-> % val meta :syntax-macro))
       (into {})))

(defn get-macro [sym]
  (if-let [m-var (sym (get-syntax-macros))]
    (-> m-var deref .reader)))

(defn ^:private symbol-dispatch
  "Called by the reader on encountering a
  list - decides whether to read as a macro
  or as a regular list."
  [reader _]
  (if (= ")" (str (peek reader)))
    (do (read-1 reader) ())
    (let [first (read-next reader)]
      (if-let [f (get-macro first)]
        (do
          (if (#{\space \newline} (peek reader)) (read-1 reader))
          (f (read-literal reader)))
        (conj (read-delimited-list \) reader) first)))))

(defn reader-macro-hook []
  (set-reader-macro \( symbol-dispatch))

(defnrecord SyntaxMacro [symbol reader]
  (fn [this & []]
    (throw
      (Exception.
        (str (.symbol this) " is a syntax macro "
             "and can't be called as a function. ")))))

(defmacro defsyntax
  "Creates a macro which operates on a string of
  it contents. Must be explicitly enabled, per-namespace,
  with `use-syntax`.
  e.g.
    (defsyntax r [s] `(def ~'my-string ~(.toUpperCase s)))
    (r hi there)
    (println my-string)
  ;=> HI THERE

  Note: Parentheses '()' MUST be balanced within the body
  of the macro, although other brackets '{}[]' don't
  matter."
  [symbol & rest]
 `(def ~(vary-meta symbol assoc :syntax-macro true)
        (SyntaxMacro. '~symbol (fn ~@rest))))

; (reader-macro-hook)
