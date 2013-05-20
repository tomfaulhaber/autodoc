(ns autodoc.branches
  (:require
   [clojure.string :as str])
  
  (:use
   [clojure.java.io :only [file]]
   [clojure.java.shell :only [with-sh-dir sh]]
   [clojure.pprint :only [pprint]]
   [autodoc.params :only (params)]
   [autodoc.build-html :only (branch-subdir)]
   [autodoc.collect-info-wrapper :only (do-collect)]
   [autodoc.doc-files :only (xform-tree)]
   [autodoc.pom-tools :only (get-version)])
  
  (:import [java.util.regex Pattern]))

;;; stolen from lancet
(defn env [val]
  (System/getenv (name val)))

(defn- build-sh-args [args]
  (concat (str/split (first args) #"\s+") (rest args)))

(defn system [& args]
  (pprint args)
  (println (:out (apply sh (build-sh-args args)))))

(defn switch-branches 
  "Switch to the specified branch"
  [branch]
  (with-sh-dir (params :root)
    (system "git fetch")
    (system "git reset --hard HEAD")
    (system (str "git checkout " branch))
    (system (str "git merge origin/" branch))))


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
                            (assoc branch-info :version (first (get-version (params :root))))
                            branch-info)]
          (f branch-info branch-spec (doall (do-collect (:name branch-info)))))))))
