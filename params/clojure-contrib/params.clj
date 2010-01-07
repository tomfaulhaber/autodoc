(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/clojure-contrib"))
      root (str file-prefix "/src/")]
  {:name "clojure-contrib",
   :file-prefix file-prefix,
   :root root,
   :web-src-dir "http://github.com/richhickey/clojure-contrib/blob/",

   :web-home "http://richhickey.github.com/clojure-contrib/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",

   :clojure-contrib-jar (str root "clojure-contrib-slim.jar"),
   :clojure-contrib-classes (str root "classes/"),

   :namespaces-to-document ["clojure.contrib"],
   :trim-prefix "clojure.contrib.",

   :load-except-list 
   [ 
    #"/test_contrib"
    #"/test_clojure"
    #"/load_all"
    #"/datalog/tests/"
    #"/datalog/example"
    #"/javadoc"
    #"/jmx/Bean"
    ],

   :build-json-index true,
   :page-title "Clojure-contrib API Reference"
   :copyright "Copyright 2007-2009 by Rich Hickey and the various contributors"})

