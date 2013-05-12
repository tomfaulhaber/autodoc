(ns autodoc.load-files
  (:import [java.util.jar JarFile]
           [java.io File]))

;;; Load all the files from the source. This is a little hacked up 
;;; because we can't just grab them out of the jar, but rather need 
;;; to load the files because of bug in namespace metadata

;;; The following two functions are taken from find-namespaces which in turn is taken
;;; from contrib code. The there for more details.

(defn clojure-source-file?
  "Returns true if file is a normal file with a .clj extension."
  [#^File file]
  (and (.isFile file)
       (.endsWith (.getName file) ".clj")))

(defn find-clojure-sources-in-dir
  "Searches recursively under dir for Clojure source files (.clj).
  Returns a sequence of File objects, in breadth-first sort order."
  [#^File dir]
  ;; Use sort by absolute path to get breadth-first search.
  (sort-by #(.getAbsolutePath %)
           (filter clojure-source-file? (file-seq dir))))

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

(defn load-files [filelist params]
  (doseq [filename (filter #(not-in % (params :load-except-list)) filelist)]
    (print (str filename ": "))
    (try 
     (load-file filename)
     (println "done.")
     (catch Exception e 
       (println  (str "failed (ex = " (.getMessage e) ")"))))))

(defn load-namespaces [params]
  (load-files
   (map #(.getPath %)
        (mapcat
         #(find-clojure-sources-in-dir
           (File. (params :root) %))
         (params :source-path)))
   params))
