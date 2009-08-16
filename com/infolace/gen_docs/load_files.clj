(ns com.infolace.gen-docs.load-files
  (:import [java.util.jar JarFile])
  (:use com.infolace.gen-docs.params))

;;; Load all the files from contrib. This is a little hacked up 
;;; because we can't just grab them out of the jar, but rather need 
;;; to load the files because of bug in namespace metadata

(def *jar-file* (str *file-prefix* "/clojure-contrib/clojure-contrib.jar"))

;(add-classpath (str "file:" *file-prefix* "/clojure-contrib-slim.jar"))
;(add-classpath (str "file:" *file-prefix* "/classes/"))

;(load (str *file-prefix* "/src/" "clojure/contrib/pprint/utilities"))
(use 'clojure.contrib.pprint.utilities)
(use 'clojure.contrib.pprint)

(defn get-elements [iterable]
  (loop [acc []]
    (if-not (.hasMoreElements iterable)
      acc
      (recur (conj acc (.nextElement iterable))))))

(defn read-jar []
  (with-open [jar
              (JarFile. *jar-file*)]
    (filter 
     #(re-find #".clj$" %) 
     (map #(.getName %) (get-elements (.entries jar))))))

(def except-list 
     [
      #"/test_contrib"
      #"/test_clojure"
      #"/load_all"
      #"/datalog/tests/"
      #"/datalog/example"
      #"/javadoc"
      #"/jmx/Bean"
      ])

(defn not-in [str regex-seq] 
  (loop [regex-seq regex-seq]
    (cond
      (nil? (seq regex-seq)) true
      (re-find (first regex-seq) str) false
      :else (recur (next regex-seq)))))

(defn file-to-ns [file]
  (.replaceAll (.replaceFirst file ".clj$" "") "/" "."))

(defn ns-to-file [ns-name]
  (.replaceAll (.replaceAll ns-name "\\." "/") "-" "_"))

(defn basename
  "Strip the .clj extension so we can pass the filename to load"
  [filename]
  (.substring filename 0 (- (.length filename) 4)))

(defn load-files [filelist]
  (doseq [file (filter #(not-in % except-list) filelist)]
    (cl-format true "~a: " file)
    (try 
     (load (basename file))
     (cl-format true "done.~%")
     (catch Exception e 
       (cl-format true "failed.~%")))))

(defn load-contrib []
  (load-files (read-jar)))
