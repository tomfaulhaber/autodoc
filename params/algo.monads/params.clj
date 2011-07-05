(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/algo.monads"))
      root (str file-prefix "/src/")]
  {:name "Monad Macros and Definitions",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/algo.monads/blob/",

   :web-home "http://clojure.github.com/algo.monads/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.algo.monads"],
   :trim-prefix nil,
   :build-json-index nil,

   :branches [{:name "master"
               :version "0.1.1"
               :status "in development"
               :params {:dependencies [['org.clojure/clojure "1.3.0-beta1"]
                                       ['org.clojure/tools.macro "0.1.1"]]}},
              ]

   :load-except-list [#"/classes/"],
   })

