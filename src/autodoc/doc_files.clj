(ns autodoc.doc-files
;  "Add any files in the projects doc/ directory to the output directory, transforming
;as necessary."
  (:use [autodoc.params :only [params]]
        [clojure.contrib.shell-out :only [with-sh-dir sh]]
        [clojure.contrib.pprint.utilities :only [prlabel]])
  (:require [clojure.contrib.duck-streams :as io])
  (:import [java.io File]))

(defn- get-extension
  "Return the string extension for a file, nil if none and :dir for a directory"
  [file _ _]
  (if (.isDirectory file)
    :dir
    (when-let [m (re-find #"\.([^./]+)$" (.getPath file))]
      (second m))))

(defmulti xform-file 
  "Copy a file from the source to the destination, performing any 
transformations along the way."
  get-extension) 

(defmethod xform-file :default 
  [src-file dst relative]
  (io/copy src-file (File. (File. dst) relative)))

(defmethod xform-file :dir
  [_ dst relative]
  (.mkdirs (File. (File. dst) relative)))

(defmethod xform-file "markdown"
  [src-file dst relative]
  (io/spit
   (File. (File. dst) (.replaceFirst relative "\\.markdown$" ".html"))
   (sh "markdown" (.getPath src-file))))

(defn xform-tree 
  "Takes source and destination directories and copies the source to the destination,
transforming files as appropriate."
  [src dst]
  (let [path-offset (+ (.length src) (if (.endsWith src "/") 1 0))]
    (prlabel x-t src path-offset)
    (doseq [src-file (next (file-seq (java.io.File. src)))]
      (let [relative-path (.substring (.getPath src-file) path-offset)]
        (prlabel x-t1 (.getPath src-file) relative-path)
        (xform-file src-file dst relative-path)))))
