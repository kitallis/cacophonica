(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'cacophonica.core
   :output-to "out/cacophonica.js"
   :output-dir "out"})
