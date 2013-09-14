(ns chiara.reader.hacking
  (use chiara.threading)
  (import java.lang.Class))

;; Begin functions of unimaginable evil

(defn get-field [^Class class field]
  (-> (doto (.getDeclaredField class (name field))
            (.setAccessible true))
      (.get nil)))

(defn array->map [array]
  (->> array
       (map-indexed #(do [%1 %2]))
       (filter second)
       (into {})))

(def ArrayOfIFn (Class/forName "[Lclojure.lang.IFn;"))

(defonce ^ArrayOfIFn macros (get-field clojure.lang.LispReader :macros))
(defonce default-macros (array->map macros))

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

;; End functions of unimaginable evil
