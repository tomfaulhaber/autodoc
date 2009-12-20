(import (java.io File))

(def *file-prefix* (.getAbsolutePath (File. "../autodoc-work-area/clojure-contrib")))
(def *src-dir* (str *file-prefix* "/src/"))
(def *src-root* "src")
(def *web-src-dir* "http://github.com/richhickey/clojure-contrib/blob/")

(def *web-home* "http://richhickey.github.com/clojure-contrib/")
(def *output-directory* (str *file-prefix* "/autodoc/"))
(def *external-doc-tmpdir* "/tmp/autodoc/doc")
(def *jar-file* (str *src-dir* "clojure-contrib-slim.jar"))

(def *clojure-contrib-jar* (str *src-dir* "clojure-contrib-slim.jar"))
(def *clojure-contrib-classes* (str *src-dir* "classes/"))

(def *namespaces-to-document* ["clojure.contrib"])
(def *trim-prefix* "clojure.contrib.")

(def *load-except-list* 
     [ 
      #"/test_contrib"
      #"/test_clojure"
      #"/load_all"
      #"/datalog/tests/"
      #"/datalog/example"
      #"/javadoc"
      #"/jmx/Bean"
      ])

(def *build-json-index* true)

(def *page-title* "Clojure-contrib")
(def *copyright* "Copyright 2007-2009 by Rich Hickey and the various contributors")


