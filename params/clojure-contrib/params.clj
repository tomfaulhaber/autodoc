(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/clojure-contrib"))
      src-dir (str file-prefix "/src/")]
  {:file-prefix file-prefix,
   :src-dir src-dir,
   :src-root "src",
   :web-src-dir "http://github.com/richhickey/clojure-contrib/blob/",

   :web-home "http://richhickey.github.com/clojure-contrib/",
   :output-directory (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",

   :clojure-contrib-jar (str src-dir "clojure-contrib-slim.jar"),
   :clojure-contrib-classes (str src-dir "classes/"),

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

   :page-title "Clojure-contrib",
   :copyright "Copyright 2007-2009 by Rich Hickey and the various contributors"})

