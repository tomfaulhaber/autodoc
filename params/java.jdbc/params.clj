(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/java.jdbc"))
      root (str file-prefix "/src/")]
  {:name "JDBC-based SQL Interface",
   :file-prefix file-prefix,
   :root root,
   :source-path "src/main/clojure",
   :web-src-dir "https://github.com/clojure/java.jdbc/blob/",

   :web-home "http://clojure.github.com/java.jdbc/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["clojure.java.jdbc"],
   :trim-prefix nil,
   :build-json-index nil,

   :branches [{:name "master"
               :version "0.0.1"
               :status "in development"
               :params {:built-clojure-jar "/home/tom/src/clj/clojure-1.3/clojure.jar"}},
              ]

   :load-except-list [#"/classes/"],
   })

