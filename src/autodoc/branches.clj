(ns autodoc.branches
  (:use [clojure.contrib.pprint :only [cl-format]]
        [clojure.contrib.pprint.utilities :only [prlabel]]
        [clojure.contrib.str-utils :only (re-split)]
        [clojure.contrib.shell-out :only [with-sh-dir sh]]
        [autodoc.params :only (params)])
  (import [java.io File]))

;;; stolen from lancet
(defn env [val]
  (System/getenv (name val)))

(defn- build-sh-args [args]
  (concat (re-split #"\s+" (first args)) (rest args)))

(defn system [& args]
  (println (apply sh (build-sh-args args))))

(defn switch-branches 
  "Switch to the specified branch"
  [branch]
  (with-sh-dir (params :root)
    (system (str "git checkout " branch))))

(defn path-str [path-seq] 
  (apply str (interpose (System/getProperty "path.separator") path-seq)))

(defn exec-clojure [class-path & args]
  (system (concat [ "java" "-cp"] 
                  (path-str class-path)
                  ["clojure.main" "-e"]
                  args)))

(defn do-collect 
  "Collect the namespace and var info for the checked out branch"
  []
  (let [class-path [(or (params :built-clojure-jar)
                        (str (env "HOME") "/src/clj/clojure/clojure.jar"))
                    (or (params :clojure-contrib-jar)
                        (str (env "HOME") "/src/clj/clojure-contrib/clojure-contrib.jar"))
                    "src"]
        out-file ()]
    (with-sh-dir (params :root)
      (exec-clojure class-path 
                    (cl-format 
                     nil 
                     "(use 'autodoc.collect-info) (collect-info-to-file \"~a\" \"~a\")"
                     (params :param-dir)
                     (.getAbsolutePath (File/createTempFile "collect-" "clj")))))))
