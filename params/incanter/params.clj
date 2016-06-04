(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/incanter"))
      root (str file-prefix "/src/")]
  {:name "Incanter",
   :file-prefix file-prefix,
   :root root,
   :source-path ["modules"],
   :web-src-dir "https://github.com/incanter/incanter/blob/",

   :web-home "http://incanter.github.io/incanter/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
   ;;   :clojure-contrib-classes (str root "build"),

   :load-jar-dirs [(str root "lib")],

   :namespaces-to-document ["incanter"],
   :trim-prefix "incanter.",
   :dependencies :from-pom

   :branches [{:name "1.5.5"
               :version "1.5.5"
               :status "stable"
               :params {:load-classpath [#"/modules/[^/]+/src$"]}}
              {:name "1.4.1"
               :version "1.4.1"
               :status "legacy"
               :params {:load-classpath [#"/modules/[^/]+/src$"]}}
              {:name "master"
               :version "2.0"
               :status "in development"
               :params {:load-classpath [#"/modules/[^/]+/src$"]}}]

   :load-except-list [#"/test/" #"/classes/" #"project.clj$"],
   })
