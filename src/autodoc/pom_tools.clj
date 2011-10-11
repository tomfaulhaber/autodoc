(ns autodoc.pom-tools
  (import [java.io File FileInputStream])
  (:require [clojure.zip :as zip]
	    [clojure.data.zip :as zf]
	    [clojure.xml :as xml])
  (:use clojure.data.zip.xml
        [autodoc.params :only [params]]))

(defn get-pom-file
  "Return a java.io.File that represents the pom.xml for this project."
  []
  (File. (File. (params :root)) "pom.xml"))

(defn- get-pom-xml
  "Get the zipper corresponding to the pom.xml file for this project."
  []
  (zip/xml-zip (xml/parse (FileInputStream. (get-pom-file)))))

(defn get-version
  "Returns the 2-vector [version is-snapshot?] where version is a string representing 
the version (with -SNAPSHOT removed) and is-snapshot? is true if we 
removed that suffix and false otherwise"
  []
  (let [full-version (first (:content (first (xml-> (get-pom-xml) :version zip/node))))]
    (if-let [[_ base-version] (re-matches #"(.*)-SNAPSHOT" full-version)]
      [base-version true]
      [full-version false])))
