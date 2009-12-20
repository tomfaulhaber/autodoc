(ns com.infolace.gen-docs.params)

;;; 
;;; Default values for settable parameters. These are overridden in the
;;; per project parameters file.
;;;

(def *param-dir* nil)                   ;set automatically from the first arg when executing

(def *file-prefix* nil)
(def *src-dir* nil)
(def *src-root* nil)
(def *web-src-dir* nil)

(def *web-home* nil)
(def *output-directory* nil)
(def *external-doc-tmpdir* nil)
(def *jar-file* nil)
(def *ext-dir* nil)

(def *clojure-contrib-jar* nil)
(def *clojure-contrib-classes* nil)

(def *built-clojure-jar* nil)

(def *namespaces-to-document* nil)
(def *trim-prefix* nil)

(def *do-load* true)
(def *load-except-list* [])
(def *build-json-index* false)

(def *page-title* "Undefined Title")
(def *copyright* "No copyright info")

