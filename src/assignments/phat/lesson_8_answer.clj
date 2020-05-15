(ns src.assignments.phat.lesson_8_answer
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]))


(def search-engines {:bing "https://www.bing.com/search?q="
                     :yahoo "https://au.search.yahoo.com/search?p="
                     :yippy "https://www.yippy.com/search?query="})

;; first approach using future
(defn search
  "Returns the first page resulted  from search engines"
  [text]
  (let [result (promise)]
    (doseq [search-engine (vals search-engines)]
      (future (if-let [results-page (slurp (str search-engine text))]
                (deliver result results-page))))
    @result))

(search "dogs")

;; second approach using alts!!
(defn search-url
  [engine term]
  (str (engine search-engines) term))

(defn search-v2
  "Returns the first page resulted  from search engines"
  [text]
  (let [bing (chan)
        yahoo (chan)
        yippy (chan)]

    (go (if-let [result (slurp (search-url :bing text))]
          (>! bing result)))
    (go (if-let [result (slurp (search-url :yahoo text))]
          (>! yahoo result)))
    (go (if-let [result (slurp (search-url :yippy text))]
          (>! yippy result)))

    (let [[result channel] (alts!! [bing yahoo yippy])]
      result)))

(time (search "dogs"))
;=> "Elapsed time: 873.4348 msecs"
(time (search-v2 "dogs"))
;=> "Elapsed time: 443.932 msecs"
;; version 2 solution seems to be faster than version 1 solution

