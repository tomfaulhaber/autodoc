(import (java.io File))

(def *file-prefix* (.getAbsolutePath (File. "../autodoc-work-area/clojure")))
(def *src-dir* (str *file-prefix* "/src/"))
(def *src-root* "src/clj")
(def *web-src-dir* "http://github.com/richhickey/clojure/blob/")

(def *web-home* "http://richhickey.github.com/clojure-contrib/")
(def *output-directory* (str *file-prefix* "/autodoc/"))
(def *external-doc-tmpdir* "/tmp/autodoc/doc")
(def *jar-file* (str *src-dir* "clojure-slim.jar"))

(def *built-clojure-jar* (str *src-dir* "/clojure-slim.jar"))

(def *namespaces-to-document* ["clojure.core" "clojure.inspector" "clojure.main" 
                               "clojure.parallel" "clojure.set" "clojure.stacktrace"
                               "clojure.test" "clojure.xml" "clojure.zip"])

(def *load-except-list* 
     [ 
      #"clojure/core.clj"
      ])

(def *page-title* "Clojure")
(def *copyright* "Copyright 2007-2009 by Rich Hickey")
