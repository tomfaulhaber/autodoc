(ns autodoc.copy-statics
  (:import [java.io File])
  (:use [clojure.java.io :only (file copy)]
        [autodoc.params :only (params)]))

(def static-file-list
     ["clojure.css",
      "internal.css",
      "wiki.css",
      "clojure-icon.gif"
      "space/content-background.gif",
      "space/left-nav-background.gif",
      "space/left-nav-bottom.gif",
      "space/left-nav-divider.gif",
      "space/resources-background.gif",
      "space/toc-background.gif"])


(defn copy-default-statics []
  (doseq [f static-file-list]
    (let [source (file "autodoc/static" f)
          target (file (params :output-path) "static" f)]
      (-> target .getParent File. .mkdirs)
      (copy (.getResourceAsStream (clojure.lang.RT/baseLoader) (.getPath source))
            target))))

(defn copy-project-statics []
  (let [static-dir (file (params :param-dir) "static")
        prefix-len (inc (count (.getPath static-dir)))
        target-dir (file (params :output-path) "static")]
    (when (.exists static-dir)
      (doseq [f (file-seq static-dir)]
        (when (.isFile f)
          (let [target (file target-dir (.substring (.getPath f) prefix-len))]
            (-> target .getParent File. .mkdirs)
            (copy f target)))))))

(defn copy-statics []
  (copy-default-statics)
  (copy-project-statics))
