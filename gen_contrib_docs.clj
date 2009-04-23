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
(def *wiki-word-prefix* "")
(def *wiki-word-suffix* "ApiDoc")
(def *wiki-file-suffix* ".wiki")

(add-classpath (str "file:" *file-prefix* "/src/"))
(add-classpath (str "file:" *file-prefix* "/classes/"))

(def overview-name "OverviewOfContrib")
(def index-name "ApiDocIndex")
(def sidebar-name "ApiDocSidebar")

(def header-content "
<wiki:comment>
This document was auto-generated from the clojure.contrib source by contrib-autodoc.
To report errors or ask questions about the overall documentation structure, formatting,
etc., contact Tom Faulhaber (google mail name: tomfaulhaber).
For errors in the documentation of a particular namespace, contact the author of that
namespace.
</wiki:comment>
")

(def overview-intro "The user contributions library, clojure.contrib, is a collection
of namespaces each of which implements features that we believe may be useful to 
a large part of the clojure community. 

This library was created by Rich Hickey but has been populated and is maintained by a 
group of volunteers who are excited about the success of the Clojure language and 
want to do our part to help it along. The current list of contributors is available 
on the [http://code.google.com/p/clojure-contrib/ clojure.contrib home page].

More contributions (and contributors) are welcome. If you wish to contribute, you will need
to sign a contributor agreement (which allows Clojure and clojure.contrib to proceed
without entanglements, see [http://clojure.org/contributing contributing] for more info). 
The best way to start is to share a project you've written with the google group and gauge
the interest in adding it to contrib. (Publishing it in an open source form on google code,
github or some other easy-to-access place in the net will also help.) After general 
discussion, Rich Hickey makes the final determination about what gets added to 
clojure.contrib.

Some parts of clojure.contrib may migrate into clojure.core if they prove to be so 
generally useful that they justify being everywhere. (For example, condp started out
as an extension in contrib, but was moved to core by popular acclamation.)

The exact role of clojure.contrib and the future of the Clojure environment (standard 
libraries, dependency models, packaging systems, etc.)
is the subject of pretty much continuous discussion
in the clojure google group and in #clojure on freenode. Feel free to join that 
discussion and help shape the ways Clojure is extended.

Like Clojure itself, clojure.contrib is made available under the [http://opensource.org/licenses/eclipse-1.0.php Eclipse Public License (EPL)]. 
clojure.contrib is copyright 2008-2009 Rich Hickey and the various contributers.
")

(load "clojure.contrib.pprint.utilities")
(load "clojure.contrib.pprint")
(refer 'clojure.contrib.pprint)

(defn wiki-word-for [reference]
  (str *wiki-word-prefix* 
       (apply str
              (apply str
                     (map #(apply str (concat [(Character/toUpperCase (first %))] (next %)))
                          (.split reference "[.-]"))))
       *wiki-word-suffix*))

(defn wiki-file-for [reference]
  (str *wiki-work-area* (wiki-word-for reference) *wiki-file-suffix*))

(defn wiki-file [basename]
  (str *wiki-work-area* basename *wiki-file-suffix*))

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

(defn base-namespace
  "A nasty function that finds the shortest prefix namespace of this one"
  [ns]
  (first 
   (drop-while 
    (comp not identity) 
    (map #(find-ns (symbol %))
         (let [parts (seq (.split (name (.getName ns)) "\\."))]
           (map #(apply str (interpose "." (take (inc %) parts)))
                (range 2 (count parts))))))))

(defn base-contrib-namespaces []
  (filter #(= % (base-namespace %)) (contrib-namespaces)))

(defn sub-namespaces 
  "Find the list of namespaces that are sub-namespaces of this one. That is they 
have the same prefix followed by a . and then more components"
  [ns]
  (let [pat (re-pattern (str (.replaceAll (name (.getName ns)) "\\." "\\.") "\\..*"))]
    (sort-by
     #(name (.getName %))
     (filter #(re-matches pat (name (.getName %))) (all-ns)))))

(defn ns-short-name [ns]
  (trim-ns-name (name (.getName ns))))

(defn make-api-link [ns]
  (wiki-word-for (ns-short-name (base-namespace ns))))

(defn remove-leading-whitespace 
  "Find out what the minimum leading whitespace is for a doc block and remove it.
We do this because lots of people indent their doc blocks to the indentation of the 
string, which looks nasty when you display it."
  [s]
  (when s
    (let [lines (.split s "\\n") 
          prefix-lens (map #(count (re-find #"^ *" %)) 
                           (filter #(not (= 0 (count %))) 
                                   (next lines)))
          min-prefix (when (seq prefix-lens) (apply min prefix-lens))
          regex (when min-prefix (apply str "^" (repeat min-prefix " ")))]
      (if regex
        (apply str (interpose "\n" (map #(.replaceAll % regex "") lines)))
        s))))

(defn insert-para-space
 "For some reason, the way googlecode deals with <pre> we need an extra space at the
beginning of paragraphs to get them to line up."
 [s]
 (when (and s (pos? (count s)))
   (str " " (.replaceAll s "\\n\\n" "\n\n "))))

(defn escape-asterisks [str] 
  (when str
    (.replaceAll (.matcher #"(\*|\]|\[)" str) "`$1`")))

(defn wrap-pre [s]
  (when s
    (str "<pre>" s "</pre>")))

(defn clean-doc-string [str]
  (wrap-pre (insert-para-space (escape-asterisks (remove-leading-whitespace str)))))

(defn var-headers [v]
  (if-let [arglists (:arglists ^v)]
    (map  
     #(cl-format nil "(_~a_~{ ~a~})" (:name ^v) %)
     arglists)
    [(cl-format nil "_~a_" (:name ^v))]))

(defn var-anchor [v]
  (.replaceAll
   (.replaceAll 
    (.replaceAll (first (var-headers v)) "_? +" "_")
    "^_+" "")
   "_+$" ""))

(defn broken-anchor [anchor]
  (re-find #"\[|]" anchor))

(defn vars-for-ns [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (or (:wiki-doc ^v) (:doc ^v)) (not (:private ^v)))] v))

(defn has-doc? [ns]
  (or (seq (vars-for-ns ns)) (:wiki-doc ^ns) (:doc ^ns)))

(defn gen-shortcuts [writer title namespace include-namespace?]
  (let [vs (vars-for-ns namespace)
        api-link (when include-namespace? (make-api-link namespace))]
    (when (seq vs)
      (if title (cl-format writer title))
      (doseq [v vs]
        (let [anchor (var-anchor v)] 
          (if (broken-anchor anchor) 
            (cl-format writer "~a " (:name ^v))
            (cl-format writer "[~@[~a~]#~a ~a] "
                       api-link
                       (var-anchor v) (:name ^v)))))
      (cl-format writer "~2%"))))

(defn gen-ns-doc [writer title ns]
  (when-let [doc (or (:wiki-doc ^ns) (clean-doc-string (:doc ^ns)))]
    (cl-format writer "~a~a~%" title doc)))

(defn gen-overview [namespaces]
  (with-open [overview (BufferedWriter. (FileWriter. (wiki-file overview-name)))]
    (cl-format overview "#summary An overview of the clojure.contrib library~%")
    (cl-format overview "#sidebar ~a~%" sidebar-name)
    (cl-format overview "~a" header-content)
    (cl-format overview "=The User Contributions Library, clojure.contrib=~%")
    (cl-format overview overview-intro)
    (cl-format overview "=Summary of the Namespaces in clojure.contrib=~%")
    (doseq [namespace namespaces]
      (cl-format overview "===~a===~%API Overview [~a here]"
                 (ns-short-name namespace)
                 (make-api-link namespace))
      (when-let [author (:author ^namespace)]
        (cl-format overview "~%<br>by ~a" author))
      (cl-format overview "~2%")
      (gen-ns-doc overview "" namespace)
      (gen-shortcuts overview "Public Variables and Functions:~%" namespace true)
      (doseq [sub-ns (sub-namespaces namespace)]
        (gen-shortcuts overview 
                       (cl-format nil "Variables and Functions in ~a:~%" (ns-short-name sub-ns))
                       sub-ns true)))))

(defn gen-sidebar [namespaces]
  (with-open [sidebar (BufferedWriter. (FileWriter. (wiki-file sidebar-name)))]
    (cl-format sidebar "#summary The navigational sidebar for the generated API documentation~%")
    (cl-format sidebar "~a" header-content)
    (cl-format sidebar "[~a Overview]<br/>~%" overview-name)
    (cl-format sidebar "[~a Index]~2%" index-name)
    (doseq [namespace namespaces]
      (cl-format sidebar "[~a ~a]<br/>~%"
                 (make-api-link namespace)
                 (ns-short-name namespace)))))

;;; Adapted from rhickey's script for the clojure API
(defn wiki-doc [api-out v]
  ; (cl-format api-out "[[#~a]]~%" (.replace (str (:name ^v)) "!" ""))
  (cl-format api-out "----~%")
  (doseq [header (var-headers v)]
    (cl-format api-out "===~a===~%" (escape-asterisks header)))
  (when (:macro ^v)
    (cl-format api-out "====Macro====~%"))
  (when-let [doc (or (:wiki-doc ^v) (clean-doc-string (:doc ^v)))]
    (cl-format api-out "~a~%" doc)))

(defn gen-var-doc [writer ns]
  (let [vs (vars-for-ns ns)]
    (when (seq vs)
      (doseq [v vs] (wiki-doc writer v)))))

(defn gen-api-page [ns]
  (let [ns-name (ns-short-name ns)]
    (with-open [api-out (BufferedWriter. (FileWriter. (wiki-file-for (ns-short-name ns))))]
      (cl-format api-out "#summary ~a API Reference~%" ns-name)
      (cl-format api-out "#sidebar ~a~%" sidebar-name)
      (cl-format api-out "~a" header-content)
      (cl-format api-out "=API for ~a=" ns-name)
      (when-let [author (:author ^ns)]
        (cl-format api-out "~%<br>by ~a~%" author))
      (cl-format api-out 
                 "~%Usage: ~%{{{~%(ns <your-namespace>~%  (:use clojure.contrib.~a))~%}}}~%"
                 ns-name)
      (gen-ns-doc api-out "==Overview==\n" ns)
      (cl-format api-out "~2%")
      (cl-format api-out "==Public Variables and Functions==~%")
      (gen-shortcuts api-out "Shortcuts:~%" ns false)
      (doseq [sub-ns (sub-namespaces ns)]
        (gen-shortcuts api-out
                       (cl-format nil "Variables and Functions in ~a:~%" (ns-short-name sub-ns))
                       sub-ns true))
      (gen-var-doc api-out ns)
      (doseq [sub-ns (sub-namespaces ns)]
        (when (has-doc? sub-ns)
          (cl-format api-out "==Namespace ~a==~%" (name (.getName sub-ns)))
          (gen-ns-doc api-out "" sub-ns)
          (gen-var-doc api-out sub-ns))))))
  
(defn gen-docs []
  (load-files (read-jar))
  (let [namespaces (base-contrib-namespaces)]
    (gen-overview namespaces)
    (gen-sidebar namespaces)
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
        (sh "sh" :in (str "rm -f " (wiki-wildcard) "\n") :return-map true)]
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
