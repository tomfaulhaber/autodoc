(import (java.io File))

(defn ! [& args]
  (let [args (into {} (map vec (partition 2 args)))
        project (:project args)
        file-prefix (.getAbsolutePath (File. (str "../autodoc-work-area/" project)))
        root (str file-prefix "/src/")]
    [(:project args)
     (merge {:file-prefix file-prefix,
             :root root,
             :source-path ["src/main/clojure"],
             :web-src-dir (str "https://github.com/clojure/" project "/blob/"),

             :web-home (str "http://clojure.github.com/" project "/"),
             :project-home (str "http://github.com/clojure/" project "/"),
             :output-path (str file-prefix "/autodoc/"),
             :external-doc-tmpdir "/tmp/autodoc/doc",

             :load-jar-dirs [(str root "lib")],

             :namespaces-to-document [(str "clojure." project)],
             :trim-prefix nil,
             :build-json-index nil,

             :copyright "Copyright 2007-2014 by Rich Hickey and the various contributors",
             :page-title (str project " API Reference"),

             :param-dir "params/contrib",

             :branches [{:name "master"
                         :version :from-pom
                         :status "in development"
                         :params {:dependencies :from-pom}},
                        ],

             :load-except-list [#"/classes/"]}
            args)]))

(into {}
      [(! :project "algo.generic" :name "Generic versions of common functions"
          :description "Generic versions of commonly used functions, implemented as multimethods that can be implemented for any data type.")
       (! :project "algo.graph" :name "Basic graph theory algorithms")
       (! :project "algo.monads" :name "Monad Macros and Definitions")
       (! :project "core.async" :name "Facilities for async programming and communication"
          :build-file "lein.xml")
       (! :project "core.cache" :name "A caching library implementing various cache strategies")
       (! :project "core.contracts" :name "Contracts programming in Clojure")
       (! :project "core.incubator" :name "Functions proposed for core inclusion"
          :namespaces-to-document ["clojure.core.incubator" "clojure.core.strint"]
          :description "Home of functions proposed for core inclusion.")
       (! :project "core.logic" :name "A logic programming library based on miniKanren"
          :build-file "lein.xml")
       (! :project "core.match" :name "Pattern match and predicate dispatch")
       (! :project "core.memoize" :name "A manipulable, pluggable, memoization framework")
       (! :project "core.rrb-vector" :name "RRB-Tree-based Vectors")
       (! :project "core.typed" :name "Gradual Typing"
          :collect-prefix-forms "(require 'clojure.core.typed) (clojure.core.typed/load-if-needed)"
          :source-path ["module-check/src/main/clojure" "module-rt/src/main/clojure"]
          :dependency-exceptions ["core.typed.rt"])
       (! :project "core.unify" :name "Unification library")
       (! :project "data.avl" :name "AVL trees"
          :description "Persistent sorted maps and sets with log-time rank queries")
       (! :project "data.codec" :name "Native codec implementations")
       (! :project "data.csv" :name "Reading and writing CSV files")
       (! :project "data.finger-tree" :name "Finger trees")
       (! :project "data.fressian" :name "Extensible Binary Data Notation")
       (! :project "data.generators" :name "Random Data Generators")
       (! :project "data.json" :name "JSON Utilities")
       (! :project "data.priority-map" :name "Priority maps")
       (! :project "data.xml" :name "Lazy XML parsing")
       (! :project "data.zip" :name "Manipulating zippers")
       (! :project "java.classpath" :name "Classpath Utilities")
       (! :project "java.data" :name "Work with Java Beans")
       (! :project "java.jdbc" :name "JDBC-based SQL Interface")
       (! :project "java.jmx" :name "JMX Interface")
       (! :project "math.combinatorics" :name "Lazy sequences for common combinatorial functions")
       (! :project "math.numeric-tower" :name "Math functions")
       ;; (! :project "test.benchmark" :name "Benchmark suite")
       (! :project "test.generative" :name "Test data generation and execution harness")
       (! :project "tools.analyzer" :name "Analyzer for Clojure code"
                    :external-autodoc-dirs ["spec"])
       (! :project "tools.analyzer.jvm" :name "JVM-specific passes for tools.analyzer"
          :namespaces-to-document ["clojure.tools.analyzer.jvm" "clojure.tools.analyzer.passes.jvm"]
          :external-autodoc-dirs ["spec"]
          :trim-prefix "clojure.tools.analyzer.")
       (! :project "tools.cli" :name "Command-line processor")
       (! :project "tools.emitter.jvm" :name "JVM bytecode generator")
       (! :project "tools.logging" :name "Logging Tools")
       (! :project "tools.macro" :name "Tools for Macro Writers")
       (! :project "tools.namespace" :name "Extract namespace declarations")
       (! :project "tools.nrepl" :name "Network REPL")
       (! :project "tools.reader" :name "Clojure Reader")
       (! :project "tools.trace" :name "Execution tracing tool")])
