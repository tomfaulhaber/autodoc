(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/clojure-fork"))
      root (str file-prefix "/src/")]
  {
   :name "Clojure (Tom's Fork)",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/clj",
   :web-src-dir "http://github.com/tomfaulhaber/clojure/blob/",

   :web-home "http://tomfaulhaber.github.com/clojure/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
   :jar-file (str root "clojure-slim.jar"),

   :built-clojure-jar nil,

   :namespaces-to-document ["clojure.core" "clojure.inspector" "clojure.main" "clojure.pprint" 
                            "clojure.set" "clojure.stacktrace" "clojure.template"
                            "clojure.test" "clojure.walk" "clojure.xml"
                            "clojure.zip"],

   :branches [["pprint" 
               {:built-clojure-jar
                "/home/tom/src/clj/autodoc-work-area/clojure-fork/src/clojure-slim.jar"}],
              ]
   :load-except-list 
   [ 
    #"clojure/core.clj"
    #"clojure/parallel.clj"
    ],

   :page-title "Clojure Core API Reference",
   :copyright "Copyright 2007-2009 by Rich Hickey",
   })
