(ns autodoc.branches
  (:use [clojure.java.io :only [file reader]]
        [clojure.java.shell :only [with-sh-dir sh]]
        [clojure.pprint :only [cl-format pprint]]
        [leiningen.deps :only [find-jars]]
        [autodoc.params :only (params expand-classpath)]
        [autodoc.build-html :only (branch-subdir)]
        [autodoc.doc-files :only (xform-tree)]
        [autodoc.pom-tools :only (get-version)])

  (import [java.io File]
          [java.util.regex Pattern]))

;;; This was dropped from contrib in 1.3, I think
(defn re-split [#^Pattern pattern string]
  (seq (.split pattern string)))

;;; stolen from lancet
(defn env [val]
  (System/getenv (name val)))

(defn- build-sh-args [args]
  (concat (re-split #"\s+" (first args)) (rest args)))

(defn system [& args]
  (pprint args)
  (println (:out (apply sh (build-sh-args args)))))

(defn switch-branches
  "Switch to the specified branch"
  [branch]
  (with-sh-dir (params :root)
    (system "git fetch")
    (system (str "git checkout " branch))
    (system (str "git merge origin/" branch))))

(defn path-str [path-seq]
  (apply str (interpose (System/getProperty "path.separator")
                        (map #(.getAbsolutePath (file %)) path-seq))))

(defn exec-clojure [class-path & args]
  (apply system (concat [ "java" "-cp"]
                        [(path-str class-path)]
                        ["clojure.main" "-e"]
                        args)))

(defn expand-jar-path [jar-dirs]
  (apply concat
         (for [jar-dir jar-dirs]
           (filter #(.endsWith (.getName %) ".jar")
                   (file-seq (java.io.File. jar-dir))))))

(defn do-collect
  "Collect the namespace and var info for the checked out branch"
  [branch-name]
  (let [src-path (.getPath (File. (params :root) (params :source-path)))
        class-path (concat
                    (filter
                     identity
                     [(params :built-clojure-jar)
                      "src"
                      src-path
                      "."])
                    (when-let [deps (params :dependencies)]
                      (find-jars {:local-repo-classpath true,
                                  :dependencies deps,
                                  :root src-path
                                  :name (str "Autodoc for " (params :name))}))
                    (expand-classpath branch-name (params :root) (params :load-classpath))
                    (expand-jar-path (params :load-jar-dirs)))
        tmp-file (File/createTempFile "collect-" ".clj")]
    (exec-clojure class-path
                  (cl-format
                   nil
                   "(use 'autodoc.collect-info) (collect-info-to-file \"~a\" \"~a\" \"~a\" \"~a\" \"~a\")"
                   (params :param-file)
                   (params :param-key)
                   (params :param-dir)
                   (.getAbsolutePath tmp-file)
                   branch-name))
    (try
     (with-open [f (java.io.PushbackReader. (reader tmp-file))]
       (binding [*in* f] (read)))
     (finally
      (.delete tmp-file)))))

(defn do-build
  "Execute an ant build in the given directory, if there's a build.xml"
  [dir branch]
  (when-let [build-file (first
                         (filter
                          #(.exists (file dir %))
                          [(str "build-" branch ".xml") "build.xml"]))]
    (with-sh-dir dir
      (system "ant"
              (str "-Dsrc-dir=" (params :root))
              (str "-Dclojure-jar=" (params :built-clojure-jar))
              "-buildfile" build-file))))

(defn with-first [s]
  (map #(assoc %1 :first? %2) s (conj (repeat false) true)))

(defn load-branch-data
  "Collects the doc data from all the branches specified in the params and
   executes the function f for each branch with the collected data. When f is executed,
   the correct branch will be checked out and any branch-specific parameters
   will be bound. Takes an array of maps, one for each branch that will be
   documented. Each map has the keys :name, :version, :status and :params.
   It calls f as (f branch-info all-branch-info ns-info)."
  [branch-spec f]
  (let [branch-spec (with-first branch-spec)]
    (doseq [branch-info branch-spec]
      (binding [params (merge params (:params branch-info))]
        (when (:name branch-info) (switch-branches (:name branch-info)))
        (do-build (params :param-dir) (:name branch-info))
        (xform-tree (str (params :root) "/doc")
                    (str (params :output-path) "/"
                         (when-not (:first? branch-info)
                           (str (branch-subdir (:name branch-info)) "/"))
                         "doc"))
        (let [branch-info (if (= (:version branch-info) :from-pom)
                            (assoc branch-info :version (first (get-version)))
                            branch-info)]
          (f branch-info branch-spec (doall (do-collect (:name branch-info)))))))))
