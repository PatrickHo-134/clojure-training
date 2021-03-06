(ns clojure-training.lesson05
  (:require
   [clj-time.core :as time]
   [clojure.pprint :as pprint]
   [clojure.java.io :as io]))

;;;; Questions

;; Getting in trouble with emacs

(defn huh-hoh []
  (loop [x 1]
    (recur (+ 1 x))))

#_ ; bad idea
(huh-hoh)

(defn my-fun []
  (let [[suit rank] (apply map vector ["S4" "S3" "C5" "S2" "H6"])
        my-hand (partition 5 1 ["A" "2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K"])]
    (println my-hand) ;;((A 2 3 4 5) (2 3 4 5 6) (3 4 5 6 7) ...(9 T J Q K))
    (println (sort rank)) ;; (2 3 4 5 6)
    (println (.contains my-hand (lazy-seq (sort rank))))))

(my-fun)

;;;; Misconceptions about futures

(defn count-word-url
  "count number of words in the quote returned from the url"
  [url]
  (let [quote (future (slurp url))]
    (count (clojure.string/split @quote #"\s"))))


;;; More misconceptions:

(defn map-futures
  "Creates a map of futures where each future is slurping a quote from
   Random-quote-url and re-structuring the results to add to quote-entries"
  [amount]
  (reduce
   (fn [futures x]
     (conj futures
           (future (swap! quote-entries conj
                          (get-quote-entry (slurp random-quote-url))))))
   []
   (range amount)))

(defn quote-word-count
  "Returns a map of AMOUNT quote entries implemented as
  {AUTHOR-LAST-NAME QUOTE-WORD-COUNT}"
  [amount]
  (reset! quote-entries {})
  (let [futures (map-futures amount)]
    ;; Concurrently runs all futures
    (doseq [x futures] @x)
    @quote-entries))



;;;; The READER.

;; Lisp is strange; it has an extra step that other language don't have: the READER.
;;
;; Other languages:
;; (source code text)  =>  (compiler)         => (machine executable code)
;;  characters            intermediate AST         Binary code
;; Lisps:
;; (source code text)  =>  (reader)     => (compiler)     => (machine executable code)
;;  characters         =>  lisp objects => lisp objects   =>  Binary code
;;
;; The READER function is what does MACROEXPANSION.
;; The compiler receives LISP STRUCTURES as input.
(macroexpand-1 '(-> foo (bar) (baz)))
;; => (baz (bar foo))
;; When then reader loads your file, and sees (-> foo (bar) (baz)),
;; it sends (baz (bar foo)) to the compiler.
;; That's why there is no performance penalty to using one form over another:
;; by the time they get turned into machine code, the inputs are the SAME.

;;;; READ syntax.
;;
;; You may have noticed a bunch of strange characters, occasionaly

#_ (this form is never seen by the compiler)


(+ 2
   #_ 3
   2)

(+ 2
   #_ (some random stuff which would not even compile)
   2)

;; Regexes:
(class #"foo")

(class #'foo)

(class #(+ 2 %))


;;; Tagged literals

#inst "1985-04-12T23:20:50.52Z"
(class #inst "1985-04-12T23:20:50.52Z")
#uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6" ; Inspect this!

(def run-time
  "This will execute when the jar file load; i.e. when the program starts."
  (time/now))

(def compile-time
  "This will define a var which stores when the file was COMPILED,
  not when the application starts to run"
  ;; Oddly, the usage of this macro is undocumented, even though
  ;; it seems very useful.
  #=(time/now))

;; Reader conditionals
(do
  #?(:clj "Clojure"
     :cljs "ClojureScript"))

[1 2 #?@(:clj [3 4] :cljs [5 6])]


;;;; Java Interop

;; Constructors:

(new java.util.Date)

;; Or, more usually, and succinctly, with trailing dot:
(java.util.Date.)


;; dot (.) special operator

;; Instance methods:

(.toUpperCase "FooBar")

(macroexpand '(.toUpperCase "FooBar"))

;; Another example:
(.getYear (java.util.Date.))

;;; dotdot (..) macro

;; Remember this?
(. "FooBar" toUpperCase)


#_ ; Won't work
(. "FooBar" toUpperCase (startsWith "FOO"))

;; But this neat workaround exists:
(.. "FooBar" toUpperCase (startsWith "FOO"))

(macroexpand '(.. "FooBar" toUpperCase (startsWith "FOO")))
;; => (. (. "FooBar" toUpperCase) (startsWith "FOO"))

;; So .. acts as
;; sort of a "thread first for Java interop" idea.


;;; doto macro

;; More hackishness around OO nature of Java.

;; java.util.Date has a method "setYear", but its signature is:
;;     public void java.util.Date.setYear(int)

;; So this works:
(let [x (java.util.Date.)]
  (. x (setYear 159))
  x)

;; It'd be nice to do this:
(.. (java.util.Date.) (setYear 159))
;; But the void return values breaks the chain.

;; This is the workaround

(doto (java.util.Date.)                 ; Even MORE fuckin' insane... :-(
  (.setYear 137)
  (.setMonth 0)
  (.setDate 1)) ; macroexpand this


;;; Static methods:

(java.util.UUID/randomUUID)

;;; memfn

;; Can't map java methods:
#_
(map .toUpperCase ["foo" "bar" "baz"])

;; You can write this:
(map #(.toUpperCase %) ["foo" "bar" "baz"])

;; Very common, also written as:
(map (memfn toUpperCase) ["foo" "bar" "baz"])
;; memfn could be read as "member function"
;;
;; opinions are divided as to whether this is a good idea,
;; but you'll see it in the wild, so you should at least
;; know what it does.




(deftype Person [name age])             ; Generates a new Java class.

(def bob (Person. "Bob" 57))

(.name bob)

;; Type hints, boxing
(set! *warn-on-reflection* true)

(.name bob)

(.name ^Person bob)

(defprotocol Buzzable
  "Provides the ability to buzz an object."
  (buzz [x] [x y] "Buzzes with an x"))

(extend-protocol Buzzable
  Person
  (buzz
    ([^ Person x]
     (println "buzzing obj of class " (class x)))
    ([^ Person x y]
     (println "buzzing with " [(class x) y]))))

(buzz (->Person "bob" 55))
(buzz (->Person "bob" 55) "foo")


;; When you know everything in advance, you can combine these two.
(deftype Person [name age]
  Buzzable
  (buzz [^ Person x]
    (println "buzzing obj of class " (class x)))
  (buzz [^ Person x y]
    (println "buzzing with " [(class x) y])))




;;; reify

;; Makes a clojure object

;; Use this when you need to call a java interface, making
;; a clojure object which implements some of the interface.
;; Note we get back an INSTANCE of a new temporary class,
;; which, in this case, we don't care about.
(def my-runnable ; (inspect this)
  (reify java.lang.Runnable
    (run [this] (println "We are running this weird runnable."))))

(.run my-runnable)


;;;; Real world example, from clj-time library

;;;; Show better time management libraries
;; clj-time,

(time/now)

;; Based on JODA time, see https://www.joda.org/joda-time/

(let [dt (time/date-time 2000 10 14 4 3 27 456)]
  ;; Much more sane than the java.util.Date stuff we saw above.
  [dt (.minusYears dt 3)])

;; Not very clojury, so clj-time wraps this stuff like this:

(time/minus (time/now) (time/years 3))

;; And we're back in the land of immutable values!  ;-)



;;;; The ns macro


;; (ns some.new.namespace
;;   "With a docstring"
;;   (:gen-class)
;;   (:require [some.lib]
;;             [another.lib])
;;   (:import [some.java.class]))

;;; Namespaces corresponds to java packages, which in turn
;;; correspond to files on the filesystem



;;;; I/O

(def path "/tmp/mystuff")

(spit "/tmp/mystuff" "This clojure class is too hard!\n")

;; There's some options we can give;
(spit "/tmp/mystuff" "This clojure class not so bad after all.\n" :append true)

(slurp "/tmp/mystuff")


;; Readers
(with-open [rdr (io/reader path)]
  (line-seq rdr)) ; Why does this blow up?

(with-open [rdr (io/reader path)]
  (into [] (line-seq rdr)))

(with-open [rdr (io/reader path)]
  (binding [*in* rdr]
    (read-line)))

(with-open [rdr (io/reader path)]
  (read rdr))


(io/resource ".")
(io/resource "my-config")





;; What do you suppose this will return?
(read-string  "(+ 2 2)")

;; How about
(read-string  "{:foo 7, :bar \"99\"}")


;; The writer

(pr {:foo 7, :bar "99"})

(print {:foo 7, :bar "99"})

;; And their -str versions.

;; Different uses cases!


(def paladin {:name "Aragorn"
              :hits-taken 0
              :danger-zone 4
              :max-points 40
              :hit-points 15})
(print paladin)
(pr paladin)
(clojure.pprint/pprint paladin)

clojure.pprint/cl-format ; hardcore!  See the Hyperspec.

(for [i (range 3)]
  ;; One of the many, many, MANY uses: proper pluralization of output
  ;; Also includes, capitalization, conditional output, automatic sequencing...
  (clojure.pprint/cl-format nil "~&~D item~:P found." i))


with-out-str  with-in-str
;; Remember *in* and *out*?

(defn do-stuff []
  (println "Clojure is fun!"))

(with-open [w (io/writer "/tmp/junk.1")]
  (binding [*out* w]
    (do-stuff)))

;; Often, it's nice to capture what do-stuff does in a string, so
(with-out-str (do-stuff)) ; macroexpand!
