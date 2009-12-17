(ns com.infolace.gen-docs.load-files
  (:import [java.util.jar JarFile])
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [clojure.contrib.pprint.utilities :only [prlabel]]
        [com.infolace.gen-docs.params :only (*src-dir* *src-root* *load-except-list*)]))

;;; Load all the files from contrib. This is a little hacked up 
;;; because we can't just grab them out of the jar, but rather need 
;;; to load the files because of bug in namespace metadata

(use 'clojure.contrib.pprint.utilities)
(use 'clojure.contrib.pprint)


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

(defn find-files [dir]
  (map ns-to-file (find-namespaces-in-dir (file dir))))

(defn basename
  "Strip the .clj extension so we can pass the filename to load"
  [filename]
  (.substring filename 0 (- (.length filename) 4)))

(defn load-files [filelist]
  (doseq [filename (filter #(not-in % *load-except-list*) filelist)]
    (cl-format true "~a: " filename)
    (try 
     (load (basename filename))
     (cl-format true "done.~%")
     (catch Exception e 
       (cl-format true "failed (ex = ~a).~%" (.getMessage e))))))

(defn load-namespaces []
  (load-files (find-files (str *src-dir* *src-root*))))
