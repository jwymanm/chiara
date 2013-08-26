(ns clarity.reader.utils
  (:refer-clojure :exclude [peek])
  (import [clojure.lang Util LispReader LineNumberingPushbackReader]
          [java.io StringReader PushbackReader]))

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

(defn discard-line [reader]
  (while (not= (read-1 reader) \newline)))

(defn reader-position [rdr]
  (if (instance? clojure.lang.LineNumberingPushbackReader rdr)
    [(-> rdr .getLineNumber int) (-> rdr .getColumnNumber dec int)]))
