(import (java.io File))

(def *file-prefix* (.getAbsolutePath (File. "../autodoc-work-area/incanter")))
(def *src-dir* (str *file-prefix* "/src/"))
(def *src-root* "src/main/clojure")
(def *web-src-dir* "http://github.com/liebke/incanter/blob/")

(def *web-home* "http://tomfaulhaber.github.com/incanter/")
(def *output-directory* (str *file-prefix* "/autodoc/"))
(def *external-doc-tmpdir* "/tmp/autodoc/doc")
(def *jar-file* (str *src-dir* *src-root*))
(def *clojure-contrib-classes* (str *src-dir* "build"))

(def *ext-dir* (str *src-dir* "lib"))

(def *namespaces-to-document* ["incanter"])
(def *trim-prefix* "incanter.")


(def *page-title* "Incanter")
; (def *copyright* "Copyright 2007-2009 by Rich Hickey")
