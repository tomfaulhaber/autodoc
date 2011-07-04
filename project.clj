(defproject autodoc "0.9.0-SNAPSHOT"
  :description "A tool to build HTML documentation from your Clojure source"
  :url "http://github.com/tomfaulhaber/autodoc"
  :main autodoc.autodoc
  :namespaces [autodoc.autodoc]
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/tools.namespace "0.1.0"]
                 [enlive "1.0.0"]
                 [leiningen "1.6.1-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]])
