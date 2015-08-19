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

;; working

(def a-size 2048)

;; (defmacro go-loop [& body]
;;   `(cljs.core.async.macros/go
;;      (while true
;;        ~@body)))

(defn current-time
  [ctx]
  (.currentTime ctx))

(defn init-file-handling
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
  [buffer]
  (go
    (let [ source (doto (.createBufferSource audio-context)
                    (aset "buffer" buffer))]
      (.connect source (.-destination audio-context))
      (.start source 0))))

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

(let [files-chan (init-file-handling)]
  (go
    (while true
      (let [files (<! files-chan)
            file (aget files 0)
            audio (<! (file->chan file))]
        (prn audio)
        (play-audio audio)))))

;; (go
;;   (let [r (<! (get-audio "https://upload.wikimedia.org/wikipedia/commons/e/e7/Bobbypfeife.ogg"))
;;         b (<! (get-buffer (hum/create-context) "https://upload.wikimedia.org/wikipedia/commons/e/e7/Bobbypfeife.ogg"))]
;;     (println r)
;;     (println b)))

;; experimental

(def audio-context
  (let [constructor (or js/window.AudioContext
                        js/window.mozAudioContext
                        js/window.msAudioContext
                        js/window.oAudioContext
                        js/window.webkitAudioContext)]
    (constructor.)))

(def get-user-media
  (let [function (or js/navigator.getUserMedia
                     js/navigator.mozGetUserMedia
                     js/navigator.webkitGetUserMedia
                     js/navigator.msGetUserMedia
                     js/navigator.oGetUserMedia)]
    function))

(defn may-work? []
  (and (audio-context) (get-user-media)))

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

(def ctx (hum/create-context))

(def vco (hum/create-osc ctx :sawtooth))

(def vcf (hum/create-biquad-filter ctx))

(def source-node (hum/create-buffer-source ctx))

(def output (hum/create-gain ctx))

(hum/connect vco vcf output)

(hum/start-osc vco)

(hum/connect-output output)

(hum/note-on output vco 440)
