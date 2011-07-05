(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/tools.nrepl"))
      root (str file-prefix "/src/")]
  {:name "Network REPL",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/tools.nrepl/blob/",

   :web-home "http://clojure.github.com/tools.nrepl/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.tools.nrepl"],
   :trim-prefix "clojure.tools.",

   :branches [{:name "master"
               :version "0.0.6"
               :status "in development"
               :params {:dependencies [['org.clojure/clojure "1.3.0-beta1"]]}},
              ]

   :load-except-list [#"/classes/"],
   })

