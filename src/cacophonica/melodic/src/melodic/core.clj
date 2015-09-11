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

(defn one-notes
  [times]
  (repeat times 1))

(defn make-meter-subsets
  [beats]
  "Generate a list of potential meters."
  (let [filtered-subsets (filter #(= beats (reduce + %))
                                 (combo/subsets (range 1 (inc beats))))
        one-note-subset (one-notes beats)
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

(defn break-beat
  [beat]
  "Generate all combinations of breaking down a beat.
  The first note is always a 1, the missing numbers in the segment are rests."
  (let [notes (range 1 (inc beat))]
    (filter (fn [n] (= 1 (first n)))
            (combo/subsets notes))))

(defn segment-measure [measure beats
                       & {:keys [running-count result] :or {running-count 0 result '()}}]
  "Segment a measure into beats and further segment the beats,
  essentially breaking the measure down into sixteenth-notes."
  (if (empty? measure)
    result
    (let [result (concat result (map #(+ running-count %) beats))
          running-count (+ running-count (first measure))]
      (segment-measure (rest measure)
                       :running-count running-count
                       :result result))))

(defn make-measures
  ([meter note]
   (make-measures meter note note))
  ([meter note decompose-note]
   (prn meter note)
   (let [note-value (int (/ 1 note))
         decompose-note-value (int (/ 1 decompose-note))
         multiplier-fn (fn [m] (* (/ decompose-note-value note-value) m))
         new-meter (map multiplier-fn meter)
         beats-broken (map break-beat new-meter)
         beats-map (apply combo/cartesian-product beats-broken)
         all-measures (reduce (fn [m v]
                                (conj m
                                      (map-indexed (fn [i x]
                                                     (let [running-count
                                                           (reduce + (subvec meter 0 i))]
                                                       (map #(+ running-count %) x)))
                                                   v)))
                              beats-map)]
     (remove #(= % [1]) (map flatten all-measures)))))

(defn merge-measures [measures beat-count
                      & {:keys [running-string running-count]
                         :or {running-string '() running-count 0}}]
  "String multiple measures together into a single piece."
  (if (empty? measures)
    running-string
    (let [running-string (concat
                          running-string
                          (map #(+ running-count %) (first measures)))
          running-count (+ running-count beat-count)]
      (merge-measures (rest measures)
                      beat-count
                      :running-string running-string
                      :running-count running-count))))

(defn generate-rhythm [measure-count beat-count
                       & {:keys [note-value] :or {note-value quaver}}]
  "Generate a rhythm with measure-count measures,
  each of time signature with beat-count and note-value."
  (let [time-sig (make-meters beat-count)
        _ (prn time-sig)
        segmented (map #(make-measures % note-value) time-sig)
        _ (prn segmented)
        rhythm (repeat measure-count segmented)]
    (merge-measures rhythm beat-count)))

;; (defn map-entity [entity intervals]
;;   "Create a list of hash-maps of positions as keys
;;   and musical notes as values."
;;   (map #(hash-map :pos %1, :note %2) entity intervals))

;; (defn generate-entity-map [construct-melody measure-count beat-count
;;                             & {:keys [note-value sparseness scale]
;;                                :or {note-value 16
;;                                     sparseness 1
;;                                     scale (generate-random-scale)}}]
;;   "Take a function to construct a melody, measure count, note count and note value,
;;   and generate a measure map of the melody and rhythm using map-entity."
;;   (let [rhythm (generate-rhythm measure-count beat-count note-value sparseness)
;;         scale-intervals (intervals->notes
;;                           (generate-intervals construct-melody scale (count rhythm)) scale)]
;;   (map-entity rhythm scale-intervals)))

;; (defn perfect-unison [degree]
;;   "Identical notes. No interval jump."
;;   (+ 0 degree))

;; (defn up-step [degree]
;;   "One step up in the scale (Whole note or half, depending on degree)."
;;   (inc degree))

;; (defn down-step [degree]
;;   "One step down in the scale (Whole note or half, depending on degree)."
;;   (dec degree))

;; (defn up-leap [degree]
;;   "One leap up in the scale (More than 2 semitones)."
;;   (+ (rand-nth [2 3 4 5 6]) degree))

;; (defn down-leap [degree]
;;   "One leap down in the scale (More than 2 semitones)."
;;   (- (rand-nth [2 3 4 5 6]) degree))

;; (defn up-octave [degree]
;;   "One perfect octave up (Example: C3 to C4)."
;;   (+ 7 degree))

;; (defn down-octave [degree]
;;   "One perfect octave down (Example: C4 to C3)."
;;   (- 7 degree))

;; (defn weighted-random-interval-jumps [scale degree weights]
;;   "Generate an interval jump using weighted random selection,
;;   from the current note until another compatible note is reached.
;;   The chances of steps, leaps and octave transitions vary."
;;   (let [movements '(perfect-unison up-step down-step up-leap down-leap up-octave down-octave)
;;         current-move (weighted-choose movements weights)
;;         temp-degree ((resolve current-move) degree)]
;;     (if (and (> temp-degree 0) (>= (count scale) temp-degree))
;;       temp-degree
;;       (weighted-random-interval-jumps scale temp-degree weights))))

;; (defn conjunct-motion [scale degree]
;;   "Melodic motion where steps are more likely to occur than leaps."
;;   (weighted-random-interval-jumps scale degree '(0.06 0.35 0.35 0.08 0.08 0.04 0.04)))

;; (defn disjunct-motion [scale degree]
;;   "Melodic motion where leaps are more likely to occur than steps."
;;   (weighted-random-interval-jumps scale degree '(0.06 0.08 0.08 0.30 0.30 0.09 0.09)))

(comment
  build a meter combination for a time signature)
