(defproject autodoc "1.1.1-SNAPSHOT"
  :description "A tool to build HTML documentation from your Clojure source"
  :url "http://github.com/tomfaulhaber/autodoc"
  :main autodoc.autodoc
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [enlive "1.0.0" :exclusions [org.clojure/clojure]]
                 [leiningen-core "2.5.2"]
                 [autodoc/autodoc-collect "1.1.3"]]
  :profiles {:uberjar {:aot :all}})
