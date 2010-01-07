(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/incanter"))
      root (str file-prefix "/src/")]
  {:name "Incanter",
   :file-prefix file-prefix,
   :root root,
   :source-path "modules",
   :web-src-dir "http://github.com/liebke/incanter/blob/",

   :web-home "http://tomfaulhaber.github.com/incanter/",
   :output-path (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
   :clojure-contrib-classes (str root "build"),

   :ext-dir (str root "lib"),

   :namespaces-to-document ["incanter"],
   :trim-prefix "incanter.",

   :load-except-list [#"/test/" #"/classes/"],
   })

