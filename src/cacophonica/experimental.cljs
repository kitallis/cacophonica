(ns cacophonica.experimental)

(defmacro go-loop [& body]
  `(cljs.core.async.macros/go
     (while true
       ~@body)))

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


(def ctx (hum/create-context))

(def vco (hum/create-osc ctx :sawtooth))

(def vcf (hum/create-biquad-filter ctx))

(def source-node (hum/create-buffer-source ctx))

(def output (hum/create-gain ctx))

(hum/connect vco vcf output)

(hum/start-osc vco)

(hum/connect-output output)

(hum/note-on output vco 440)
