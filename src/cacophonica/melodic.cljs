(ns cacophonica.melodic
  (:require [clojure.browser.repl :as repl]
            [clojure.math.combinatorics :as c]))

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

(defn note->freq
  [note]
  (* 440.0 (js/Math.pow 2.0 (/ (- note 69.0) 12.0))))

(defn freq->note
  [hz]
  (js/Math.round (+ 69
                    (* 12
                       (/ (js/Math.log (/ hz 440.0)) (js/Math.log 2))))))

(defn bpm
  [beats]
  (fn [beat] (-> beat
                 (/ beats)
                 (* 60)
                 (* 1000))))

;; ;; (make-meters 11 crotchet)
;; ;; (* (inc (/ 1 note)))
;; (defn make-meters
;;   [beats]
;;   "Generate all combinations of meter representations
;;   of the time signature (beats and note)."
;;   (let [beat-range (range 1 beats)]
;;     (mapv (fn [starting-beat]
;;             (mapv (fn [next-beat]
;;                     (let [partial-beat-range (range next-beat beats)]
;;                       (reduce (fn [meter current-beat]
;;                                 (let [current-meter-count (reduce + meter)
;;                                       beats-left (- beats current-meter-count)
;;                                       next-beat  (- beats current-beat)]
;;                                   (cond
;;                                     (< current-meter-count beats) (conj meter next-beat)
;;                                     ;; (> current-beat beats-left) (conj meter beats-left)
;;                                     (< next-beat beats-left) (conj meter beats-left)
;;                                     :else meter)))
;;                               [starting-beat]
;;                               partial-beat-range)))
;;                   beat-range))
;;           beat-range)))

;; (defn make-meter [beats starting-beat]
;;   (let [beat-range (range 1 beats)]
;;     (mapv (fn [next-beat]
;;             (let [partial-beat-range (range next-beat beats)]
;;               (reduce (fn [meter current-beat]
;;                         (let [current-meter-count (reduce + meter)
;;                               beats-left (- beats current-meter-count)
;;                               next-beat  (- beats current-beat)
;;                               _ (prn current-meter-count " " beats-left " " next-beat " " current-beat)]
;;                           (cond
;;                             (< beats-left 1) meter

;;                             (< beats-left next-beat) (conj meter beats-left)

;;                             (< current-meter-count beats) (conj meter next-beat)

;;                             (>= beats-left current-beat) (conj meter beats-left))))
;;                       [starting-beat]
;;                       partial-beat-range)))
;;           beat-range)))


;; (defn make-meter [beats starting-beat]
;;   (let [beat-range (range 1 beats)]
;;     (mapv (fn [next-beat]
;;             (let [partial-beat-range (range next-beat beats)]
;;               (reduce (fn [meter current-beat]
;;                         (let [current-meter-count (reduce + meter)
;;                               beats-left (- beats current-meter-count)
;;                               next-beat  (- beats current-beat)
;;                               _ (prn current-meter-count " " beats-left " " next-beat " " current-beat)]
;;                           (cond
;;                             (< beats-left 1)
;;                             meter

;;                             (< current-meter-count beats)
;;                             (if (or (>= beats-left current-beat) (< beats-left next-beat))
;;                               (conj meter beats-left)
;;                               (conj meter next-beat)))))
;;                       [starting-beat]
;;                       partial-beat-range)))
;;           beat-range)))

;; (defn make-meters
;;   [beats]
;;   (loop [beat-range (range 1 beats)]
;;     ))

;; (defn break-meters
;;   [meter note])

;; (defn build-beats
;;   [meter])

;; ;; 3
;; ;;
;; (defn generate-meter [beat-count & [sig]]
;;   "Decompose a measure into a random meter of beats,
;;   within the time signature's note count."
;;   (cond
;;     (> beat-count 3)
;;     (let [beat (rand-nth [2 3 4])
;;           sig (conj (or sig '()) beat)]
;;       (generate-meter (- beat-count beat) sig))
;;     (and (<= beat-count 3) (> beat-count 0))
;;     (let [sig (conj (or sig '()) beat-count)]
;;       (generate-meter 0 sig))
;;     :else sig))

;; (comment
;;   build a meter combination for a time signature)
