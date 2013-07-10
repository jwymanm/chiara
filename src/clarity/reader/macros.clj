(ns clarity.reader.macros
  (:refer-clojure :exclude [peek])
  (use clarity.reader.hacking
       clarity.reader.utils))

(defn get-default-macro
  "Get the default reader macro associated
  with `char`, e.g.
    (get-default-macro \\()
  ;=> #<ListReader>"
  [char]
  (default-macros (int char)))

;; ------------------------
;; Namespaced reader macros
;; ------------------------

(defn ^:private current-ns
  "Return the name of the current
  ns as a symbol."
  [] (ns-name *ns*))

(defonce ^:private macro-table (atom {}))

(defn ^:private macro-dispatch
  "When called by the reader, decides either
  to dispatch to a user reader macro or re-read
  without macros."
  [reader char]
  (if-let [f (or (get-in @macro-table [(current-ns) (int char)])
                 (get-default-macro char))]
    (f reader char)
    (do
      (unread reader char)
      (with-reader-macro char nil
        (read reader)))))

(defn use-reader-macro [{:keys [char reader]}]
  (swap! macro-table assoc-in [(current-ns) (int char)] reader)
  (set-reader-macro char macro-dispatch))

(defn use-reader-macros
  "Takes one or more 'reader macro' objects of
  the form `{:char :reader}` and enables them
  in the current namespace"
  [& ms]
  (dorun (map use-reader-macro ms)))

(defn refresh-macros
  "Re-enable user macros if the reader
  is reset, e.g. because of an exception."
  []
  (doseq [i (->> @macro-table vals (map keys) flatten)]
    (set-reader-macro i macro-dispatch)))