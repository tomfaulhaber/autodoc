(ns autodoc.params
  (require [clojure.string :as str])
  (import [java.io File]))

;;;
;;; Description and default values for settable parameters. These are overridden in the
;;; per project parameters file, or from leiningen, or on the command line.
;;;

(def available-params
     ;; name, default val, description (for help)
     [[:name nil "The name of this project"],
      [:project-home nil "The home page of this project on the web"]
      [:description nil "A description of this project"],
      [:param-dir "autodoc-params" "A directory from which to load custom project data"],
      [:param-file nil "A multi-project file from which to load custom project data"],
      [:param-key nil "The project in a multi-project file that should be built (only used\n     when param-file is specified.)"],
      [:template-dir "templates" "The directory in which project-specific templates are stored"]
      [:root "." "The directory in which to find the project"],
      [:source-path ["src"] "The relative path within the project directory where we find the source. Multiple source paths can be entered separated by \":\""],
      [:web-src-dir nil "The web address for source files (e.g., https://github.com/clojure/clojure/blob/)"],

      [:web-home nil "Where these autodoc pages will be stored on the web (for gh-pages, https://<user>.github.io/<project>/)"],
      [:output-path "autodoc" "Where to create the output html tree."],
      [:external-doc-tmpdir "/tmp/autodoc/doc" "The place to store temporary doc files during conversion (i.e., when converting markdown)."],
      [:load-classpath [] "Extra items on the classpath needed to load (e.g., gen-classed items)."]
      [:load-jar-dirs nil "Directories with jars to add to classpath when doing loads"]

      [:built-clojure-jar nil nil],
      [:namespaces-to-document nil "The list of namespaces to include in the documentation, separated by commas"],
      [:trim-prefix nil "The prefix to trim off namespaces in page names and references (e.g. \"clojure.contrib\")"],
      [:collect-prefix-forms nil "Forms to invoke in the autodoc-collect process before starting collection"]
      [:dependency-exceptions nil "Dependencies to ignore even if they're mentioned in the POM"]
      [:branches [[nil {}]] nil]
      [:build-file nil nil]
      [:load-except-list [] "A list of regexps that describe files that shouldn't be loaded"],
      [:external-autodoc-dirs [] "A list of directories in the target autodoc area that are maintained externally. Autodoc will avoid stepping on these."]
      [:build-json-index false "Set to true if you want to create an index file in JSON (currently slow)"],
      [:build-raw-index false "Set to true if you want to create an index that contains all of the namespace metadata used to generate the docs (suitable for feeding to autodoc's internal functions)"]

      [:page-title nil "A title to put on each page"],
      [:copyright "No copyright info " "Copyright (or other page footer data) to put at the bottom of each page"]
      [:scm-tool "git" "Source control management tool: git or hg"]
      [:commit? false "Commit and push the documentation when complete, if doc dir is a git repo"]
      ])

(def param-set (set (for [[kw _ _] available-params] kw)))


(defmacro defdyn [name & stuff]
  `(if (and (= 1 (:major *clojure-version*))
            (> 2 (:minor *clojure-version*)))
     (def ~name ~@stuff)
     (def ~(with-meta name {:dynamic true}) ~@stuff)))

(defdyn params (into {} (for [[kw val _] available-params] [kw val])))

(defn params-help
  ([writer]
     (binding [*out* writer]
       (println "Parameters:")
       (doseq [[kw _ desc] available-params :when desc]
         (println (str "   --" (name kw) ": " desc))))))

(defn check-params
  "Check the param map to make sure its values are legal"
  [param-map]
  (do
    (when-let [tool (param-map :scm-tool)]
      (when-not (contains? #{"git" "hg"} tool)
        (throw (IllegalArgumentException. "Parameter :scm-tool can only be git or hg"))))
    nil))

(defn merge-params
  "Merge the param map supplied into the params defined in the params var"
  [param-map]
  (check-params param-map)
  (alter-var-root #'params merge param-map))

(defn params-from-dir
  "Read param.clj from the specified directory and set the params accordingly"
  [param-dir]
  (merge-params (merge {:param-dir param-dir} (load-file (str param-dir "/params.clj")))))

(defn params-from-file
  "Read the specified file which should return a map of parameter entries, dereference
the supplied key and set params accordingly"
  [param-file key]
  (merge-params (get (load-file param-file) key)))

(defn extract-arg [arglist]
  (let [param (first arglist)]
    (if (and param (.startsWith param "--"))
      (let [param (.substring param 2)
            parts (.split param "=" 2)
            one-arg (= (count parts) 2)
            [param val] (if one-arg parts [param (second arglist)])
            param (keyword param)
            remainder (if one-arg (next arglist) (next (next arglist)))]
        (if (param-set param)
          [[param val] remainder]
          (throw (RuntimeException. (str "No such parameter --" (name param))))))
      [nil arglist])))

(defn consume [func initial-context]
  (loop [context initial-context
         acc []]
    (let [[result new-context] (apply func [context])]
      (if (not result)
        [acc new-context]
      (recur new-context (conj acc result))))))

(defmulti convert-val (fn [x _] (class x)))
(defmethod convert-val :default [default-val arg-str] arg-str)
(defmethod convert-val java.lang.Integer [default-val arg-str] (java.lang.Integer/valueOf arg-str))
(defmethod convert-val java.lang.Boolean [default-val arg-str]
  (condp = arg-str
    "true" true
    "false" false
    (throw (IllegalArgumentException. "Boolean argument doesn't have boolean value (true or false)"))))
(defmethod convert-val clojure.lang.PersistentVector [default-val arg-str]
  (vec (str/split arg-str #":")))

(defn convert-arg [param arg-str]
  (let [default-val (params param)]
    (convert-val default-val arg-str)))

(defn process-command-line
  "Process the command line arguments returning [ map-of-params [ remaining-args ]]"
  [args]
  (let [[params args] (consume extract-arg args)]
    [(into {} (for [[p v] params] [p (convert-arg p v)])) (into [] args)]))

(defn expand-wildcards
  "Find all the files under root that match re. Not truly wildcard expansion, but..."
  [root re]
  (if (instance? java.util.regex.Pattern re)
    (for [f (file-seq (File. root)) :when (re-find re (.getAbsolutePath f))]
      (.getAbsolutePath f))
    (list re)))

;;; Expand any regexp patterns in the classpath to the set of
;;; available files. This function is memoized so the filesystem work will
;;; only be done of the first call. The classpath is expanded relative to
;;; the argument root, branch is included to force the memoization to a
;;; single branch

(def expand-classpath
     (memoize
      (fn [branch root cp]
        (mapcat (partial expand-wildcards root) cp))))
