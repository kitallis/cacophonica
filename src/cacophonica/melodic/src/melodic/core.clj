 (ns melodic.core
  (:require [clojure.math.combinatorics :as combo]))

(def breve 2)
(def semibreve 1)
(def minim 1/2)
(def crotchet 1/4)
(def quaver 1/8)
(def semiquaver 1/16)

(def notes
  {0 :C
   1 :C#
   2 :D
   3 :Eb
   4 :E
   5 :F
   6 :F#
   7 :G
   8 :Ab
   9 :A
   10 :Bb
   11 :B})

(def f-major [65 67 69 70 72 74 76 77])

;; (defn note->freq
;;   [note]
;;   (* 440.0 (js/Math.pow 2.0 (/ (- note 69.0) 12.0))))

;; (defn freq->note
;;   [hz]
;;   (js/Math.round (+ 69
;;                     (* 12
;;                        (/ (js/Math.log (/ hz 440.0)) (js/Math.log 2))))))

(defn bpm
  [beats]
  (fn [beat] (-> beat
                 (/ beats)
                 (* 60)
                 (* 1000))))

(defn make-meter-subsets
  [beats]
  "Generate a list of potential meters."
  (let [filtered-subsets (filter #(= beats (reduce + %))
                                 (combo/subsets (range 1 (inc beats))))
        one-note-subset (repeat beats 1)
        meter-subsets (conj filtered-subsets one-note-subset)]
    (if (even? beats)
      (conj meter-subsets (list (/ beats 2) (/ beats 2)))
      meter-subsets)))

(defn decompose-meter
  [meter]
  "Decompose a meter recursively into its most atomic form."
  (let [meter-size (count meter)]
    (if (> meter-size 1)
      (let [decomposed-meter (reduce (fn [m meter-value]
                                       (if (>= meter-value 3)
                                         (conj m (make-meter-subsets meter-value))
                                         (conj m [[meter-value]])))
                                     []
                                     meter)]
        (map flatten (apply combo/cartesian-product decomposed-meter)))
      [meter])))

(defn make-meters
  [beats]
  "Generate a list of meters for a measure.
  Basically, from the number of a beats in a time signature."
  (distinct (apply concat
                   (map decompose-meter (make-meter-subsets beats)))))

(defn segment-measure [measure
                       & {:keys [running-count result note-value sparseness]
                          :or {running-count 0 result '()
                               note-value 16 sparseness 1}}]
  "Segment a measure into beats and further segment the beats,
  essentially breaking the measure down into sixteenth-notes."
  (if (empty? measure)
    result
    (let [result (concat result (map #(+ running-count %)
                                     (segment-beat (first measure)
                                                   note-value
                                                   :sparseness sparseness)))
          running-count (+ running-count (* (/ 16 note-value) (first measure)))]
      (segment-measure (rest measure)
                       :running-count running-count
                       :result result
                       :note-value note-value
                       :sparseness sparseness))))

(defn break-beat
  [beat]
  "Generate all combinations of breaking down a beat.
  The first note is always a 1, the missing numbers in the segment are rests."
  (let [notes (range 1 (inc beat))]
    (filter (fn [n] (= 1 (first n)))
            (combo/subsets notes))))

(defn make-measures
  ([meter note]
   (make-measures meter note note))
  ([meter note decompose-note]
   (let [note-value (int (/ 1 note))
         decompose-note-value (int (/ 1 decompose-note))
         multiplier-fn (fn [m] (* (/ decompose-note-value note-value) m))
         new-meter (map multiplier-fn meter)]
     (prn (break-beat (first new-meter))))))

(comment
  build a meter combination for a time signature)
