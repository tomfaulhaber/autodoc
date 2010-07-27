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

   :clojure-contrib-jar (str root "target/clojure-contrib-1.2.0-SNAPSHOT.jar"),
   :clojure-contrib-classes (str root "target/classes/"),

   :namespaces-to-document ["clojure.contrib"],
   :trim-prefix "clojure.contrib.",

   :branches [{:name "1.2.x"
               :version "v1.2"
               :status "beta 1"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-master/clojure-slim.jar"
                        :source-path "src/main/clojure"
                        :load-classpath [(str root "/target/classes")]}},
              {:name "master"
               :version "next"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-master/clojure-slim.jar"
                        :source-path "src/main/clojure"
                        :load-classpath [(str root "/target/classes")]}},
              {:name "1.1.x"
               :version "v1.1"
               :status "stable"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure/clojure-slim.jar"
                        :load-classpath [(str root "/classes")]}}]

   :load-except-list 
   [ 
    #"/test_contrib"
    #"/test_clojure"
    #"/load_all"
    #"/datalog/tests/"
    #"/datalog/example"
    #"/javadoc"
    #"/jmx/Bean"
    #"/test/clojure/clojure/contrib/"
    ],

   :build-json-index true,
   :page-title "Clojure-contrib API Reference"
   :copyright "Copyright 2007-2009 by Rich Hickey and the various contributors"})

