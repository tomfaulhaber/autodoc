(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/tools.logging"))
      root (str file-prefix "/src/")]
  {:name "Logging Tools",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/tools.logging/blob/",

   :web-home "http://clojure.github.com/tools.logging/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.tools.logging"],
   :trim-prefix "clojure.tools.",

   :branches [{:name "master"
               :version "0.1.2"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-1.3/clojure.jar"}},
              ]

   :load-except-list [#"/classes/"],
   })

