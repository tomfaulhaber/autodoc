(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/data.json"))
      root (str file-prefix "/src/")]
  {:name "JSON Utilities",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/data.json/blob/",

   :web-home "http://clojure.github.com/data.json/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.data.json"],
   :trim-prefix "clojure.data.",

   :branches [{:name "master"
               :version "0.1.1"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-1.3/clojure.jar"}},
              ]

   :load-except-list [#"/classes/"],
   })

