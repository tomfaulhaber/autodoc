(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/clojure"))
      root (str file-prefix "/src/")]
 {
  :name "Clojure",
  :file-prefix file-prefix,
  :root root,
  :source-path "src/clj",
  :web-src-dir "http://github.com/richhickey/clojure/blob/",

  :web-home "http://richhickey.github.com/clojure-contrib/",
  :output-path (str file-prefix "/autodoc/"),
  :external-doc-tmpdir "/tmp/autodoc/doc",
  :jar-file (str root "clojure-slim.jar"),

  :built-clojure-jar (str root "/clojure-slim.jar"),

  :namespaces-to-document ["clojure.core" "clojure.inspector" "clojure.main" 
                            "clojure.set" "clojure.stacktrace" "clojure.template"
                            "clojure.test" "clojure.walk" "clojure.xml"
                            "clojure.zip"],

  :branches [["master" {:built-clojure-jar "/home/tom/src/clj/clojure-master/clojure.jar"}],
             ["1.1.x" {}]]
  :load-except-list 
  [ 
   #"clojure/core.clj"
   #"clojure/parallel.clj"
   ],

  :page-title "Clojure Core API Reference",
  :copyright "Copyright 2007-2009 by Rich Hickey",
  })
