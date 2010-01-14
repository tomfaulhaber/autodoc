(ns leiningen.autodoc
  (:use [leiningen.compile :only [eval-in-project]]))

(defn autodoc
  "Build the autodoc for this project. Use lein autodoc help for all the options"
  [project & args]
  (eval-in-project project
                   `(do (try (require '~'autodoc.autodoc)
                             (apply
                              @(ns-resolve '~'autodoc.autodoc
                                           '~'autodoc)
                              ~(merge (select-keys project
                                                   [:name :description :source-path :root])
                                       (:autodoc project))
                              '~args)
                             (catch Exception e#
                               (println e#)
                               (println "Make sure autodoc is added as"
                                        "a dev-dependency in your"
                                        "project.clj."))))))
