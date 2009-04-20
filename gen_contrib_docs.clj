(ns gen-contrib-docs
  (:import [java.util.jar JarFile]
           [java.io FileWriter BufferedWriter]))

;;; The process
;;; 1) Update contrib svn
;;; 2) If it hasn't changed, exit
;;; 3) Build contrib, if failure error exit
;;; 4) Update wiki svn & if merge issue, error exit
;;; 5) Remove all _auto_*
;;; 6) Generate doc files
;;; 7) "svn add" new wiki files & "svn delete" removed files
;;; 8) Commit new and changed wiki files, if error, error exit
;;; 9) Save contrib svn number

;; jar file definition relative to contrib location
(def *clojure-jar-file* "../../clojure/clojure.jar")
(def *file-prefix* "../wiki-work-area/clojure-contrib")
(def *jar-file* (str *file-prefix* "/clojure-contrib.jar"))

(def *wiki-work-area* "../wiki-work-area/wiki-src/")
(def *wiki-word-prefix* "ZZAutoGen")
(def *wiki-file-suffix* ".wiki")

(add-classpath (str "file:" *file-prefix* "/src/"))
(add-classpath (str "file:" *file-prefix* "/classes/"))

(def header-content "
<wiki:comment>
This document was auto-generated from the clojure.contrib source by contrib-autodoc.
To report errors or ask questions about the overall documentation structure, formatting,
etc., contact Tom Faulhaber (google mail name: tomfaulhaber).
For errors in the documentation of a particular namespace, contact the author of that
namespace.
</wiki:comment>
")

(load "clojure.contrib.pprint.utilities")
(load "clojure.contrib.pprint")
(refer 'clojure.contrib.pprint)

(defn wiki-word-for [reference]
  (str *wiki-word-prefix* 
       (apply str (.split (cl-format nil "~:(~a~)" reference) "[.-]"))))

(defn wiki-file-for [reference]
  (str *wiki-work-area* (wiki-word-for reference) *wiki-file-suffix*))

(defn wiki-wildcard [] (wiki-file-for "*"))

(defn class-methods [x] (map #(.getName %) (.getMethods (class x))))

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

(defn contrib-namespaces []
  (map #(find-ns (symbol %)) 
       (filter #(.startsWith % "clojure.contrib.")
               (sort (map #(name (.getName %)) (all-ns))))))

(defn trim-ns-name [s]
  (if (.startsWith s "clojure.contrib.")
    (subs s 16)
    s))

(defn ns-short-name [ns]
  (trim-ns-name (name (.getName ns))))

(defn make-api-link [ns]
  (wiki-word-for (ns-short-name ns)))

(defn remove-leading-whitespace [str]
  (when str
    (.replaceAll (.matcher #"(?m)^[ \t]*" str) "")))

(defn gen-overview [namespaces]
  (with-open [overview (BufferedWriter. (FileWriter. (wiki-file-for "contrib-overview")))]
    (cl-format overview "#summary An overview of the components of clojure.contrib~%")
    (cl-format overview "~a" header-content)
    (cl-format overview "=Clojure.contrib Overview=~%")
    (doseq [namespace namespaces]
      (cl-format overview "*~a* [~a api]" (.getName namespace) (make-api-link namespace))
      (when-let [author (:author ^namespace)]
        (cl-format overview "~%<br>by ~a" author))
      (cl-format overview "~2%")
      (when-let [doc (or (:wiki-doc ^namespace) (remove-leading-whitespace (:doc ^namespace)))]
        (cl-format overview "~a~%"  doc))
      (cl-format overview "~%"))))

;;; Adapted from rhickey's script for the clojure API
(defn wiki-doc [api-out v]
  ; (cl-format api-out "[[#~a]]~%" (.replace (str (:name ^v)) "!" ""))
  (cl-format api-out "----~%")
  (if-let [arglists (:arglists ^v)]
    (doseq [args arglists] 
      (cl-format api-out "===(_~a_~{ ~a~})===~%" (:name ^v) args))
    (cl-format api-out "===_~a_===~%" (:name ^v)))
  (when (:macro ^v)
    (cl-format api-out "====Macro====~%"))
  (when-let [doc (or (:wiki-doc ^v) (remove-leading-whitespace (:doc ^v)))]
    (cl-format api-out "~a~%" doc)))

(defn gen-api-page [ns]
  (let [ns-name (ns-short-name ns)]
    (with-open [api-out (BufferedWriter. (FileWriter. (wiki-file-for (ns-short-name ns))))]
      (cl-format api-out "#summary An api-out of the API of clojure.contrib.~a~%" ns-name)
      (cl-format api-out "~a" header-content)
      (cl-format api-out "=API for clojure.contrib.~a=~%" ns-name)
      (when-let [doc (or (:wiki-doc ^ns) (:doc ^ns))]
        (cl-format api-out "==Overview==~%~a~2%" doc))
      (cl-format api-out "==Public Variables and Functions==~%")
      (let [vs (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
                     :when (and (or (:wiki-doc ^v) (:doc ^v)) (not (:private ^v)))] v)]
        (when vs
          (doseq [v vs] (wiki-doc api-out v)))))))
  
(defn gen-docs []
  (load-files (read-jar))
  (let [namespaces (contrib-namespaces)]
    (gen-overview namespaces)
    (doseq [ns namespaces]
      (gen-api-page ns))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Code for dealing with subversion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(load "clojure.contrib.shell_out")
(refer 'clojure.contrib.shell-out)

(defn svn-update [dir]
  (cl-format true "Updating ~a...~%" dir)
  (let [{result :exit, output :out error :err}
        (sh "svn" "update" :return-map true :dir dir)]
    (print output)
    (newline)
    (when-not (= result 0)
      (throw (Exception. (str "svn update failed:\n" error))))))

(defn get-svn-version [dir]
  (let [{result :exit, output :out error :err}
        (sh "svn" "log" "-q" "--limit" "1" :return-map true :dir dir)]
    (when-not (= result 0)
      (throw (Exception. (str "svn log failed:\n" error))))
    (re-find #"r\d+"
             (first (filter #(not (.startsWith % "-----")) (.split output "\\n"))))))

(defn svn-status [dir]
  (let [{result :exit, output :out error :err}
        (sh "svn" "status" :return-map true :dir dir)]
    (when-not (= result 0)
      (throw (Exception. (str "svn status failed:\n" error))))
    (when (> (count output) 0)
      (apply merge-with concat 
             (for [s (.split output "\\n")]
               (let [[_ k v] (re-find #"(.)\s+(\S+)" s)]
                 {(keyword k) [v]}))))))

(defn svn-add [dir file]
  (let [{result :exit, output :out error :err}
        (sh "svn" "add" file :return-map true :dir dir)]
    (when-not (= result 0)
      (throw (Exception. (str "svn add failed for " file ":\n" error))))))

(defn svn-delete [dir file]
  (let [{result :exit, output :out error :err}
        (sh "svn" "delete" file :return-map true :dir dir)]
    (when-not (= result 0)
      (throw (Exception. (str "svn delete failed for " file ":\n" error))))))

(defn svn-commit [dir comment]
  (cl-format true "Committing ~a...~%" dir)
  (let [{result :exit, output :out error :err}
        (sh "svn" "commit" "-m" comment :return-map true :dir dir)]
    (print output)
    (newline)
    (when-not (= result 0)
      (throw (Exception. (str "svn commit failed:\n" error))))))

(defn update-contrib [] (svn-update *file-prefix*))
(defn get-contrib-version [] (get-svn-version *file-prefix*))

(defn update-wiki [] (svn-update *wiki-work-area*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Code for managing the last seen version file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(load "clojure.contrib.duck_streams")
(refer 'clojure.contrib.duck-streams)

(def last-seen-version-file (str *wiki-work-area* "../last-seen-revision"))

(defn get-last-seen-version []
  (try
   (slurp* last-seen-version-file)
   (catch Exception e)))

(defn put-last-seen-version [version-string]
  (spit last-seen-version-file version-string))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Run ant
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-contrib []
  (cl-format true "Building contrib in ~a...~%" *file-prefix*)
  (let [{result :exit, output :out error :err}
        (sh "ant" "clean" :return-map true :dir *file-prefix*)]
    (print output)
    (newline)
    (when-not (= result 0)
      (throw (Exception. (str "ant clean failed:\n" error)))))
  (let [{result :exit, output :out error :err}
        (sh "ant" (str "-Dclojure.jar=" *clojure-jar-file*) :return-map true :dir *file-prefix*)]
    (print output)
    (newline)
    (when-not (= result 0)
      (throw (Exception. (str "ant failed:\n" error))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Remove autogenerated files
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-auto-doc-files []
  (cl-format true "Removing ~a...~%" (wiki-wildcard))
  (let [{result :exit, output :out error :err}
        (sh "rm" "-f" (wiki-wildcard) :return-map true)]
    (print output)
    (newline)
    (when-not (= result 0)
      (throw (Exception. (str "remove failed:\n" error))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Put all the pieces together
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn contrib-auto-doc
  ([] (contrib-auto-doc false))
  ([force]
     ;; 1) Update contrib svn
     (update-contrib)
     ;; 2) If it hasn't changed, exit
     (let [svn-version (get-contrib-version)
           last-svn-version (get-last-seen-version)]
       (when (or force (not (= svn-version last-svn-version)))
         (cl-format true "Building new contrib doc for contrib version ~a~%" svn-version)
         ;; 3) Build contrib, if failure error exit
         ;(build-contrib)
         ;; 4) Update wiki svn & if merge issue, error exit
         (update-wiki)
         ;; 5) Remove all auto gen files
         (remove-auto-doc-files)
         ;; 6) Generate doc files
         (gen-docs)
         ;; 7) "svn add" new wiki files & svn delete removed files
         (let [status-map (svn-status *wiki-work-area*)]
           (doall (map #(svn-add *wiki-work-area* %) (:? status-map)))
           (doall (map #(svn-delete *wiki-work-area* %) (:! status-map))))
         ;; 8) Commit new and changed wiki files, if error, error exit
         (svn-commit *wiki-work-area*
                     (cl-format nil "Auto-documentation for contrib version ~a~%" svn-version))
         ;; 9) Save contrib svn number
         (put-last-seen-version svn-version)))))
