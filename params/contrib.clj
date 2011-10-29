(import (java.io File))

(defn ! [& args]
  (let [args (into {} (map vec (partition 2 args)))
        project (:project args)
        file-prefix (.getAbsolutePath (File. (str "../autodoc-work-area/" project)))
        root (str file-prefix "/src/")]
    [(:project args)
     (merge {:file-prefix file-prefix,
             :root root,
             :source-path "src/main/clojure",
             :web-src-dir (str "https://github.com/clojure/" project "/blob/"),

             :web-home (str "http://clojure.github.com/" project "/"),
             :output-path (str file-prefix "/autodoc/"),
             :external-doc-tmpdir "/tmp/autodoc/doc",

             :load-jar-dirs [(str root "lib")],

             :namespaces-to-document [(str "clojure." project)],
             :trim-prefix nil,
             :build-json-index nil,

             :copyright "Copyright 2007-2011 by Rich Hickey and the various contributors",
             :page-title (str project " API Reference"),

             :param-dir "params/contrib",

             :branches [{:name "master"
                         :version :from-pom
                         :status "in development"
                         :params {:dependencies [['org.clojure/clojure "1.3.0"]]}},
                        ],

             :load-except-list [#"/classes/"]}
            args)]))

(into {}
      [(! :project "algo.monads" :name "Monad Macros and Definitions"
          :branches [{:name "master"
                      :version :from-pom
                      :status "in development"
                      :params {:dependencies [['org.clojure/clojure "1.3.0"]
                                              ['org.clojure/tools.macro "0.1.1"]]}},
                     ]
          )
       (! :project "data.codec" :name "Native codec implementations")
       (! :project "data.json" :name "JSON Utilities")
       (! :project "java.classpath" :name "Classpath Utilities")
       (! :project "java.jdbc" :name "JDBC-based SQL Interface")
       (! :project "tools.logging" :name "Logging Tools")
       (! :project "tools.macros" :name "Tools for Macro Writers")
       (! :project "tools.nrepl" :name "Network REPL")])
