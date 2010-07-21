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

   :built-clojure-jar nil,

   :namespaces-to-document ["clojure.core" "clojure.core.protocols"
                            "clojure.inspector" "clojure.main" "clojure.pprint"
                            "clojure.repl" 
                            "clojure.set" "clojure.stacktrace" "clojure.string"
                            "clojure.template" "clojure.test" "clojure.walk" 
                            "clojure.xml" "clojure.zip"
                            "clojure.java.browse" "clojure.java.io" "clojure.java.javadoc"
                            "clojure.java.shell"],

   :branches [{:name "master"
               :version "next" 
               :status "in development"
               :params {:built-clojure-jar
                        "/home/tom/src/clj/autodoc-work-area/clojure/src/clojure-slim.jar"}},
              {:name "1.1.x" 
               :version "v1.1"
               :status "stable"
               :params {:built-clojure-jar
                        "/home/tom/src/clj/autodoc-work-area/clojure/src/clojure-slim.jar"}}]
   :load-except-list 
   [ 
    #"clojure/core.clj"
    #"clojure/parallel.clj"
    ],

   :page-title "Clojure Core API Reference",
   :copyright "Copyright 2007-2010 by Rich Hickey",
   })
