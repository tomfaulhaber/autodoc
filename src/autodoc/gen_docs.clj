(ns autodoc.gen-docs
  (:use [clojure.contrib.java-utils :only (delete-file)]
        [clojure.contrib.pprint :only (pprint)]
        [autodoc.load-files :only (load-namespaces)]
        [autodoc.build-html :only (make-all-pages)]
        [autodoc.params :only (params params-from-dir)]
        [autodoc.branches :only (load-branch-data)]
        [autodoc.git-tools :only [git-dir? autodoc-commit]])
  (:import [java.io File]))

(defn clean-html-files
  "Remove all the -api.html files before starting a build cycle"
  [dir]
  (doseq [f (filter #(.endsWith (.getPath %) "-api.html")
                    (file-seq (java.io.File. dir)))]
    (delete-file f)))

(defn gen-docs 
  ([param-dir commit?]
     (params-from-dir param-dir)
     (clean-html-files (params :output-path))
     (let [branch-spec (params :branches)]
       (load-branch-data branch-spec make-all-pages))
     (when (git-dir? (File. (params :output-path)))
       (autodoc-commit (File. (params :root)) (File. (params :output-path))
                       (map first (params :branches))
                       commit?))))
