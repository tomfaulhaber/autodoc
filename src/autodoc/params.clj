(ns autodoc.params
  (:use [clojure.contrib.pprint :only (cl-format)]
        [clojure.contrib.pprint.utilities :only (consume)]))

;;; 
;;; Description and default values for settable parameters. These are overridden in the
;;; per project parameters file, or from leiningen, or on the command line.
;;;

(def available-params
     ;; name, default val, description (for help)
     [[:name nil "The name of this project"],
      [:param-dir "autodoc-params" "A directory from which to load custom project data"],
      
      [:file-prefix nil nil], ;; only used with ant-wrapper
      [:root "." "The directory in which to find the project"],
      [:source-path "src" "The relative path within the project directory where we find the source"],
      [:web-src-dir nil "The web address for source files (e.g., http://github.com/richhickey/clojure/blob/)"],
      
      [:web-home nil "Where these autodoc pages will be stored on the web (for gh-pages, http://<user>.github.com/<project>/)"],
      [:output-path "autodoc" "Where to create the output html tree."],
      [:external-doc-tmpdir "/tmp/autodoc/doc" "The place to store temporary doc files during conversion (i.e., when converting markdown)."],
      [:ext-dir nil nil], ;; only used with ant-wrapper
      
      [:clojure-contrib-jar nil nil], ;; only used with ant-wrapper
      [:clojure-contrib-classes nil nil], ;; only used with ant-wrapper
      
      [:built-clojure-jar nil nil],;; only used with ant-wrapper
      [:namespaces-to-document nil "The list of namespaces to include in the documentation, separated by commas"],
      [:trim-prefix nil "The prefix to trim off namespaces in page names and references (e.g. \"clojure.contrib\")"],
      
      [:load-except-list [] "A list of regexps that describe files that shouldn't be loaded"], 
      [:build-json-index false "Set to true if you want to create an index file in JSON (currently slow)"],
      
      [:page-title nil "A title to put on each page"],
      [:copyright "No copyright info " "Copyright (or other page footer data) to put at the bottom of each page"]
      ])

(def param-set (set (for [[kw _ _] available-params] kw)))

(defonce params (into {} (for [[kw val _] available-params] [kw val])))

(defn params-help 
  ([writer]
     (cl-format writer "Parameters:~%~:{   --~a: ~a~%~}"
                (for [[kw _ desc] available-params :when desc] [(name kw) desc]))))

(defn merge-params 
  "Merge the param map supplied into the params defined in the params var"
  [param-map]
  (alter-var-root #'params merge param-map))

(defn params-from-dir 
  "Read param.clj from the specified directory and set the params accordingly"
  [param-dir]
  (merge-params (merge {:param-dir param-dir} (load-file (str param-dir "/params.clj")))))

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

(defn process-command-line 
  "Process the command line arguments returning [ map-of-params [ remaining-args ]]"
  [args]
  (let [[params args] (consume extract-arg args)]
    [(into {} params) (into [] args)]))
