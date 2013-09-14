(ns chiara.reader.utils
  (:refer-clojure :exclude [peek])
  (import [clojure.lang Util LispReader LineNumberingPushbackReader]
          [java.io StringReader PushbackReader]))

(defn string-reader [s] (-> s StringReader. LineNumberingPushbackReader.))

(defn read-delimited-list
  "Read items into a list until delim
  is reached."
  ([delim ^PushbackReader reader] (read-delimited-list delim reader true))
  ([delim ^PushbackReader reader recursive]
    (apply list (LispReader/readDelimitedList delim reader recursive))))

(defn read-1
  "Remove and return a single char
  from the reader, or `nil`."
  [^PushbackReader reader]
  (let [c (.read reader)]
    (if (= -1 c) nil (char c))))

(defn unread
  "Push a char back onto the reader."
  [^PushbackReader reader char] (.unread reader (int char)))

(defn peek
  "Return the next char without removing
  it."
  [^PushbackReader reader]
  (when-let [char (read-1 reader)]
    (unread reader char)
    char))

(defn read-next [reader] (read reader))

(defn discard-line [^PushbackReader reader]
  (while (not= (read-1 reader) \newline)))

(defn reader-position [^LineNumberingPushbackReader reader]
  [(-> reader .getLineNumber int)
   (-> reader .getColumnNumber dec int)])
