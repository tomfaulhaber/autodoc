(defproject autodoc "0.8.0-SNAPSHOT"
  :description "A tool to build HTML documentation from your Clojure source"
  :url "http://github.com/tomfaulhaber/autodoc"
  :main autodoc.autodoc
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [enlive "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[leiningen/lein-swank "1.0.0-SNAPSHOT"]
                     [lein-clojars "0.5.0-SNAPSHOT"]])
