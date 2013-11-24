(ns autodoc.gen-docs
  (:use [clojure.java.io :only (delete-file)]
        [clojure.pprint :only (pprint)]
        [autodoc.build-html :only (make-all-pages)]
        [autodoc.params :only (params params-from-dir)]
        [autodoc.branches :only (load-branch-data)]
        [autodoc.git-tools :only [git-dir? autodoc-refresh-dir autodoc-commit]])
  (:import [java.io File]))

(defn file-seq-ignore
  "A tree seq on java.io.Files that ignores directories in the exceptions list.
cf. clojure.core/file-seq"
  [dir exceptions]
  (let [exceptions (set exceptions)]
    (tree-seq
     (fn [#^java.io.File f] (and (. f (isDirectory)) (not (exceptions (.getName f)))))
     (fn [#^java.io.File d] (seq (. d (listFiles))))
     dir)))

(defn clean-html-files
  "Remove all the -api.html files before starting a build cycle"
  [dir]
  (doseq [f (filter #(.endsWith (.getPath %) "-api.html")
                    (file-seq-ignore (java.io.File. dir)
                                     (params :external-autodoc-dirs)))]
    (delete-file f)))

(defn clean-index-files
  "Remove all the index-XXX.clj files before starting a build cycle"
  [dir]
  (doseq [f (filter #(re-matches #"index-.*\.clj" (.getName %))
                    (file-seq-ignore (java.io.File. dir)
                                     (params :external-autodoc-dirs)))]
    (delete-file f)))

(defn gen-branch-docs []
  (when (git-dir? (File. (params :output-path)))
    (autodoc-refresh-dir (File. (params :output-path))))
  (clean-html-files (params :output-path))
  (clean-index-files (params :output-path))
  (let [branch-spec (params :branches)]
    (load-branch-data branch-spec make-all-pages))
  (when (and (params :commit?) (git-dir? (File. (params :output-path))))
    (autodoc-commit (File. (params :root)) (File. (params :output-path))
                    (map :name (params :branches)))))
(defn gen-docs
  [param-dir commit?]
  (params-from-dir param-dir)
  (gen-branch-docs))
