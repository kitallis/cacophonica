(ns cacophonica.core
  (:require [clojure.browser.repl :as repl]
            [goog.net.XhrIo]
            [cljs.core.async :as async :refer [<! >! chan put! close!]]
            [hum.core :as hum])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)
(println "Hi, PitchDetect!")

(def a-size 2048)


(defn current-time
  [ctx]
  (.currentTime ctx))

(defn handle-files
  []
  (let [drop-zone js/document
        files-chan (chan)]
    (.addEventListener drop-zone
                       "dragover"
                       (fn [e]
                         (.stopPropagation e)
                         (.preventDefault e)
                         (set! (.-dropEffect (.-dataTransfer e))
                               "copy"))
                       false)
    (.addEventListener drop-zone
                       "drop"
                       (fn [e]
                         (.stopPropagation e)
                         (.preventDefault e)
                         (put! files-chan
                               (.-files (.-dataTransfer e))))
                       false)
    files-chan))

(defn decode-audio-data
  [context data]
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn get-audio [url]
  (let [ch (chan)]
    (doto (goog.net.XhrIo.)
      (.setResponseType "arraybuffer")
      (.addEventListener goog.net.EventType.COMPLETE
                         (fn [event]
                           (let [res (-> event
                                         .-target
                                         .getResponse)]
                             (go (>! ch res)
                                 (close! ch)))))
      (.send url "GET"))
    ch))

(defn play-audio
  "Play the given audioBuffer using global audio-context"
  [buffer]
  (let [source (.createBufferSource audio-context)]
    (set! (.-buffer source) buffer)
    (.connect source (.-destination audio-context))
    (.start source 0)
    source))

(defn file->chan
  "return a channel which will get populated with decoded audio data"
  [file]
  (let [reader (new window/FileReader)
        resp-c (chan)
        c (chan)]
    (set! (.-onload reader) (fn []
                              (put! resp-c (.-result reader))))
    (.readAsArrayBuffer reader file)
    (go
      (while true
        (let [[resp] (alts! [resp-c])]
          (.decodeAudioData audio-context
                            resp
                            #(put! c %)))))
    c))

(defn get-buffer
  [ctx url]
  (decode-audio-data ctx (get-audio url)))

(defn source->analyzer
  [source-node]
  (let [analyzer (.createAnalyser audio-context)]
    (set! (.-fftSize analyzer) 2048)
    (set! (.-smoothingTimeConstant analyzer) 0.7)
    (.connect source-node analyzer)
    (.connect analyzer (.-destination audio-context))
    analyzer))

;; function findNextPositiveZeroCrossing( start ) {
;; 	var i = Math.ceil( start );
;; 	var last_zero = -1;
;; 	// advance until we're zero or negative
;; 	while (i<buflen && (buf[i] > 128 ) )
;; 		i++;
;; 	if (i>=buflen)
;; 		return -1;

;; 	// advance until we're above MINVAL, keeping track of last zero.
;; 	while (i<buflen && ((t=buf[i]) < MINVAL )) {
;; 		if (t >= 128) {
;; 			if (last_zero == -1)
;; 				last_zero = i;
;; 		} else
;; 			last_zero = -1;
;; 		i++;
;; 	}

;; 	// we may have jumped over MINVAL in one sample.
;; 	if (last_zero == -1)
;; 		last_zero = i;

;; 	if (i==buflen)	// We didn't find any more positive zero crossings
;; 		return -1;

;; 	// The first sample might be a zero.  If so, return it.
;; 	if (last_zero == 0)
;; 		return 0;

;; 	// Otherwise, the zero might be between two values, so we need to scale it.

;; 	var t = ( 128 - buf[last_zero-1] ) / (buf[last_zero] - buf[last_zero-1]);
;; 	return last_zero+t;
;; }

;; (defn find-next-postive-zero-crossing
;;   [start buf]
;;  (let [i (js/Math.ceil start)
;;        last-zero -1]
;;    (while (and (< i buflen) (> (.indexOf buf i) 128)))))

(let [analyzer (atom nil)
      files-chan (handle-files)]

  (go
    (let [files (<! files-chan)
          file  (aget files 0)
          audio (<! (file->chan file))
          ;; _ (.noteOn audio-source (current-time audio-context))
          source-node (play-audio audio)]
      (reset! analyzer (source->analyzer source-node))
      (when @analyzer
        (let [arr (new window/Uint8Array 2048)]
          (.getByteFrequencyData @analyzer arr)
          (println "foo" (aget arr 0))
          (let [audio-data (for [i (range (.-length arr))]
                             (aget arr i))]
            (println "printing arr")
            (println arr)))))))

;; (go
;;   (let [r (<! (get-audio "https://upload.wikimedia.org/wikipedia/commons/e/e7/Bobbypfeife.ogg"))
;;         b (<! (get-buffer (hum/create-context) "https://upload.wikimedia.org/wikipedia/commons/e/e7/Bobbypfeife.ogg"))]
;;     (println r)
;;     (println b)))

;; experimental
