(ns autodoc.copy-statics
  (:import [java.io File])
  (:use [clojure.contrib.java-utils :only (file)]
        [clojure.contrib.duck-streams :only (copy)]
        [autodoc.params :only (params)]))

(def static-file-list
     ["clojure.css",
      "internal.css",
      "wiki.css",
      "space/content-background.gif",
      "space/left-nav-background.gif",
      "space/left-nav-bottom.gif",
      "space/left-nav-divider.gif",
      "space/resources-background.gif",
      "space/toc-background.gif"])


(defn copy-statics []
  (doseq [f static-file-list]
    (let [source (file "autodoc/static" f)
          target (file (params :output-path) "static" f)]
      (-> target .getParent File. .mkdirs)
      (copy (.getResourceAsStream (clojure.lang.RT/baseLoader) (.getPath source))
            target))))
