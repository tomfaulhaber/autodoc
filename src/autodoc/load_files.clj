(ns autodoc.load-files
  (:import [java.util.jar JarFile]
           [java.io File])
  (:use [autodoc.find-namespaces :only [find-clojure-sources-in-dir]]
        [autodoc.params :only (params)]))

;;; Load all the files from the source. This is a little hacked up
;;; because we can't just grab them out of the jar, but rather need
;;; to load the files because of bug in namespace metadata

(defn not-in [str regex-seq]
  (loop [regex-seq regex-seq]
    (cond
      (nil? (seq regex-seq)) true
      (re-find (first regex-seq) str) false
      :else (recur (next regex-seq)))))

(defn file-to-ns [file]
  (find-ns (symbol (-> file
                       (.replaceFirst ".clj$" "")
                       (.replaceAll "/" ".")
                       (.replaceAll "_" "-")))))

(defn ns-to-file [ns]
  (str (-> (name ns)
           (.replaceAll "\\." "/")
           (.replaceAll "-" "_"))
       ".clj"))

(defn basename
  "Strip the .clj extension so we can pass the filename to load"
  [filename]
  (.substring filename 0 (- (.length filename) 4)))

(defn load-files [filelist]
  (doseq [filename (filter #(not-in % (params :load-except-list)) filelist)]
    (print (str filename ": "))
    (try
     (load-file filename)
     (println "done.")
     (catch Exception e
       (println  (str "failed (ex = " (.getMessage e) ")"))))))

(defn load-namespaces []
  (load-files
   (map #(.getPath %)
        (find-clojure-sources-in-dir
         (File. (params :root) (params :source-path))))))
