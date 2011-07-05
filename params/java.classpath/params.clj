(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/java.classpath"))
      root (str file-prefix "/src/")]
  {:name "Classpath Utilities",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/java.classpath/blob/",

   :web-home "http://clojure.github.com/java.classpath/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.java.classpath"],
   :trim-prefix "clojure.tools.",

   :branches [{:name "master"
               :version "0.1.2"
               :status "in development"
               :params {:dependencies [['org.clojure/clojure "1.3.0-beta1"]]}},
              ]

   :load-except-list [#"/classes/"],
   })

