(import (java.io File))

(let [file-prefix (.getAbsolutePath (File. "../autodoc-work-area/incanter"))
      src-dir (str file-prefix "/src/")]
  {:file-prefix file-prefix,
   :src-dir src-dir,
   :src-root "modules",
   :web-src-dir "http://github.com/liebke/incanter/blob/",

   :web-home "http://tomfaulhaber.github.com/incanter/",
   :output-directory (str file-prefix "/autodoc/"),
   :external-doc-tmpdir "/tmp/autodoc/doc",
   :clojure-contrib-classes (str src-dir "build"),

   :ext-dir (str src-dir "lib"),

   :namespaces-to-document ["incanter"],
   :trim-prefix "incanter.",

   :load-except-list [#"/test/" #"/classes/"],


   :page-title "Incanter"})

