(ns autodoc.doc-files
  "Add any files in the projects doc/ directory to the output directory, transforming
as necessary."
  (:use [autodoc.params :only (params)])
  (:require [clojure.contrib.duck-streams :as io])
  (:import [java.io File]))

(defn- get-extension
  "Return the string extension for a file, nil if none and :dir for a directory"
  [file]
  (if (.isDirectory file)
    :dir
    (if-let [m (re-find #"\.([^./]+)$" (.getPath file))]
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
  Fill in here)

(defn xform-tree 
  "Takes source and destination directories and copies the source to the destination,
transforming files as appropriate."
  [src dst]
  (let [path-offset (+ (.length src) (if (.endsWith src "/" 0 1)))]
    (doall [src-file (file-seq (java.io.File. src))]
           (let [relative-path (.substring (.getPath src-file) path-offset)]
             (xform-file src-file dst relative-path)))))
