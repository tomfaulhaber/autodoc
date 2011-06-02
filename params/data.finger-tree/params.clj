(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/data.finger-tree"))
      root (str file-prefix "/src/")]
  {:name "Finger Trees",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/data.finger-tree/blob/",

   :web-home "http://clojure.github.com/data.finger-tree/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.data.finger-tree"],
   :trim-prefix "clojure.data.",

   :branches [{:name "master"
               :version "0.0.2"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-1.3/clojure.jar"}},
              ]

   :load-except-list [#"/classes/"],
   })

