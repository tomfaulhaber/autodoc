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
      [(! :project "algo.generic" :name "Generic versions of common functions")
       (! :project "algo.monads" :name "Monad Macros and Definitions"
          :branches [{:name "master"
                      :version :from-pom
                      :status "in development"
                      :params {:dependencies [['org.clojure/clojure "1.3.0"]
                                              ['org.clojure/tools.macro "0.1.1"]]}},
                     ]
          )
       (! :project "core.incubator" :name "Functions proposed for core inclusion")
       (! :project "core.match" :name "Pattern match and predicate dispatch")
       (! :project "core.unify" :name "Unification library")
       (! :project "data.codec" :name "Native codec implementations")
       (! :project "data.csv" :name "Reading and writing CSV files")
       (! :project "data.finger-tree" :name "Finger trees")
       (! :project "data.json" :name "JSON Utilities") 
       (! :project "data.priority-map" :name "Priority maps")
       (! :project "data.xml" :name "Lazy XML parsing")
       (! :project "data.zip" :name "Manipulating zippers")
       (! :project "java.classpath" :name "Classpath Utilities")
       (! :project "java.data" :name "Work with Java Beans"
          :branches [{:name "master"
                      :version :from-pom
                      :status "in development"
                      :params {:dependencies [['org.clojure/clojure "1.3.0"]
                                              ['org.clojure/tools.logging "0.2.3"]]}},
                     ])
       (! :project "java.jdbc" :name "JDBC-based SQL Interface")
       (! :project "java.jmx" :name "JMX Interface")
       (! :project "math.combinatorics" :name "Lazy sequences for common combinatorial functions")
       (! :project "math.numeric-tower" :name "Math functions")
       (! :project "test.benchmark" :name "Benchmark suite")
       (! :project "tools.cli" :name "Command-line processor")
       (! :project "tools.logging" :name "Logging Tools")
       (! :project "tools.macros" :name "Tools for Macro Writers")
       (! :project "tools.namespace" :name "Extract namespace declarations"
          :branches [{:name "master"
                      :version :from-pom
                      :status "in development"
                      :params {:dependencies [['org.clojure/clojure "1.3.0"]
                                              ['org.clojure/java.classpath "0.1.1"]]}},
                     ])
       (! :project "tools.trace" :name "Execution tracing tool")
       (! :project "tools.nrepl" :name "Network REPL")])
