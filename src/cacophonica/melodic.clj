(ns cacophonica.melodic)

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

(defn note->freq
  [note]
  (* 440.0 (js/Math.pow 2.0 (/ (- note 69.0) 12.0))))

(defn freq->note
  [hz]
  (js/Math.round (+ 69
                    (* 12
                       (/ (js/Math.log (/ hz 440.0)) (js/Math.log 2))))))
