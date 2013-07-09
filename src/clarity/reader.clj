(ns clarity.reader
  (:refer-clojure :exclude [peek])
  (import [clojure.lang Util LispReader LineNumberingPushbackReader]
          [java.io StringReader PushbackReader]))

;; -------------------------
;; Queue (copied from utils)
;; -------------------------

(defn ^:private queue [] (atom []))

(defmacro ^:private queued
  [q & exprs]
 `(let [id# (last (swap! ~q #(conj % (inc (or (last %) 0)))))]
    (while (not= id# (first @~q))
      (Thread/sleep 10))
    (let [result# (do ~@exprs)]
      (swap! ~q subvec 1)
      result#)))

;; -----------------
;; Low level hacking
;; -----------------

(defn ^:private get-field [class field]
  (-> (doto (.getDeclaredField LispReader (name field))
            (.setAccessible true))
      (.get nil)))

(defn ^:private array->map [array]
  (->> array
       (map-indexed #(do [%1 %2]))
       (filter second)
       (into {})))

(defonce ^:private macros (get-field LispReader :macros))
(defonce ^:private default-macros (array->map macros))

(defn reset-macros
  "Resets clojure's reader macros to their
  defaults. Useful backup function."
  []
  (doseq [i (range 256)]
    (aset macros i (default-macros i))))

(defn set-reader-macro
  "*Globally* set a character (0-255) to
  dispatch to the given reading function.

  (Avoid unless you know what you're doing)"
  [char fn]
  (aset macros (int char) fn)
  nil)

(defn get-reader-macro
  "Get the current reader macro associated
  with `char`, e.g.
    (get-reader-macro \\()
  ;=> #<ListReader>"
  [char]
  (aget macros (int char)))

(defn get-default-macro
  "Get the default reader macro associated
  with `char`, e.g.
    (get-default-macro \\()
  ;=> #<ListReader>"
  [char]
  (default-macros (int char)))

;; ----------------
;; Reader utilities
;; ----------------

(defn string-reader [s] (-> s StringReader. LineNumberingPushbackReader.))

(defn read-delimited-list
  "Read items into a list until delim
  is reached."
  ([delim reader] (read-delimited-list delim reader true))
  ([delim reader recursive]
    (apply list (LispReader/readDelimitedList delim reader recursive))))

(defn read-1
  "Remove and return a single char
  from the reader, or `nil`."
  [reader]
  (let [c (.read reader)]
    (if (= -1 c) nil (char c))))

(defn unread
  "Push a char back onto the reader."
  [reader char] (.unread reader (int char)))

(defn peek
  "Return the next char without removing
  it."
  [reader]
  (when-let [char (read-1 reader)]
    (unread reader char)
    char))

(defn read-next [reader] (read reader))

;; ------------------------
;; Namespaced reader macros
;; ------------------------

(def reader-queue (queue))

(defmacro with-reader-macro
  "Temporarily enables the given macro.
  Useful for testing. Thread-safe as it
  will block until other instances have
  finished.
  e.g.
    (with-reader-macro \\( my-list-reader
      (read-string ...))"
  [char macro & forms]
 `(queued reader-queue
    (let [old# (get-reader-macro ~char)]
      (set-reader-macro ~char ~macro)
      (let [result# (do ~@forms)]
        (set-reader-macro ~char old#)
        result#))))

(defn current-ns
  "Return the name of the current
  ns as a symbol."
  [] (ns-name *ns*))

(defonce macro-table (atom {}))

(defn macro-dispatch
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
