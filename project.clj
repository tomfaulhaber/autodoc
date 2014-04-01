(defproject autodoc "1.0.0"
  :description "A tool to build HTML documentation from your Clojure source"
  :url "http://github.com/tomfaulhaber/autodoc"
  :main autodoc.autodoc
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.namespace "0.1.0"]
                 [enlive "1.0.0"]
                 [lancet "1.0.1"]
                 [org.apache.maven/maven-ant-tasks "2.0.10" :exclusions [ant]]
                 [autodoc-collect "1.0.1"]])
