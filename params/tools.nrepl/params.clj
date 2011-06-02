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
   :trim-prefix "clojure.data.",

   :branches [{:name "master"
               :version "0.0.5"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-1.2/clojure.jar"}},
              ]

   :load-except-list [#"/classes/"],
   })

