(ns chiara.threading)

;; ------
;; Queues
;; ------

(defn queue
  "Create a queue object to pass into
  `queued`."
  [] (atom []))

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
