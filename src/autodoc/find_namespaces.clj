;;; find_namespaces.clj: search for ns declarations in dirs, JARs, or CLASSPATH

;; by Stuart Sierra, http://stuartsierra.com/
;; April 19, 2009

;; Copyright (c) Stuart Sierra, 2009. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

;; I lifted this from clojure.contrib and stripped it down for autodoc, so that
;; autodoc wouldn't have to use clojure.contrib during it's first phase
;; (which is problematic owing to version variations in the case of clojure core
;; and contrib)

(ns
  #^{:author "Stuart Sierra",
     :doc "Search for ns declarations in dirs, JARs, or CLASSPATH (stolen from clojure.contrib"}
  autodoc.find-namespaces
  (import (java.io File FileReader BufferedReader PushbackReader
                   InputStreamReader)
          (java.util.jar JarFile)))


;;; Finding namespaces in a directory tree

(defn clojure-source-file?
  "Returns true if file is a normal file with a .clj extension."
  [#^File file]
  (and (.isFile file)
       (.endsWith (.getName file) ".clj")))

(defn find-clojure-sources-in-dir
  "Searches recursively under dir for Clojure source files (.clj).
  Returns a sequence of File objects, in breadth-first sort order."
  [#^File dir]
  ;; Use sort by absolute path to get breadth-first search.
  (sort-by #(.getAbsolutePath %)
           (filter clojure-source-file? (file-seq dir))))

