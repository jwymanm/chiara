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
   ;=> Elapsed time: 1009.702535 msecs"
  [q & exprs]
 `(let [id# (last (swap! ~q #(conj % (inc (or (last %) 0)))))]
    (while (not= id# (first @~q))
      (Thread/sleep 1))
    (try
      ~@exprs
      (finally
        (swap! ~q subvec 1)))))

;; -------
;; Spacing
;; -------

(defn system-ms
  "Gives System.nanoTime in rounded milliseconds."
  [] (-> (System/nanoTime) (/ 1e6) Math/round))

(defn spacer
  "Create a new delay object, interval in ms."
  [interval] (atom [interval 0 0]))

(defn space
  "Will block until `interval` ms have passed since
  all other calls finished. Used by `spaced`."
  [spacer] (-> (swap! spacer
                 (fn [[interval last _]]
                   (let [now  (system-ms)
                         next (+ last interval)  ; Time of next call
                         wait (- next now)]      ; Time until next call
                     (if (> wait 0)              ; `next` might be in the past
                       [interval next wait]
                       [interval now     0]))))  ; in which case "next" is really now
               last Thread/sleep))

(defmacro spaced
  "`spaced` code blocks will not execute within `interval`
  milliseconds of each other, so can be used to space out
  API calls in an optimal and thread-aware way."
  [spacer & forms] `(do (space ~spacer) ~@forms))
