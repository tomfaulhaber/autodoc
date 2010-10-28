(ns autodoc.doc-files
;  "Add any files in the projects doc/ directory to the output directory, transforming
;as necessary."
  (:use [autodoc.params :only [params]]
        [clojure.contrib.shell-out :only [sh]]
        [clojure.java.io :only [delete-file copy]])
  (:import [java.io File]))

;; Brought in from clojure.contrib.java-utils since it's not making the migration to 1.2
(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (if (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-file-recursively child silently)))
  (delete-file f silently))

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
  (copy src-file (File. (File. dst) relative)))

(defmethod xform-file :dir
  [_ dst relative]
  (.mkdirs (File. (File. dst) relative)))

(defmethod xform-file "markdown"
  [src-file dst relative]
  (spit
   (File. (File. dst) (.replaceFirst relative "\\.markdown$" ".html"))
   (sh "markdown" (.getPath src-file))))

(defn xform-tree 
  "Takes source and destination directories and copies the source to the destination,
transforming files as appropriate."
  [src dst]
  ;; first delete any previous version
  (delete-file-recursively (File. dst) true)
  ;; Now walk the source tree copying/transforming each file
  (when (.exists (File. src))
    (.mkdirs (File. dst))
    (let [path-offset (+ (.length src) (if (.endsWith src "/") 1 0))]
      (doseq [src-file (next (file-seq (java.io.File. src)))]
        (let [relative-path (.substring (.getPath src-file) path-offset)]
          (xform-file src-file dst relative-path))))))
