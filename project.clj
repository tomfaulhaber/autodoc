(defproject autodoc "0.7.0"
  :description "A tool to build HTML documentation from your Clojure source"
  :url "http://github.com/tomfaulhaber/autodoc"
  :main autodoc.autodoc
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [ant/ant "1.6.2"]
                 [ant/ant-launcher "1.6.2"]]
  :dev-dependencies [[leiningen/lein-swank "1.0.0-SNAPSHOT"]
                     [lein-clojars "0.5.0-SNAPSHOT"]])
