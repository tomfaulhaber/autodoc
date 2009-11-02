(import (java.io File))

(def *file-prefix* (.getAbsolutePath (File. "../autodoc-work-area/clojure")))
(def *src-dir* (str *file-prefix* "/src/"))
(def *web-src-dir* "http://github.com/richhickey/clojure/blob/")

(def *web-home* "http://richhickey.github.com/clojure-contrib/")
(def *output-directory* (str *file-prefix* "/autodoc/"))
(def *external-doc-tmpdir* "/tmp/autodoc/doc")
(def *jar-file* (str *src-dir* "clojure-slim.jar"))

(def *built-clojure-jar* (str *src-dir* "/clojure-slim.jar"))
