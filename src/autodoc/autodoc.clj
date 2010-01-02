(ns autodoc.autodoc
  (:use 
   [clojure.contrib.pprint :only (cl-format)]
   [clojure.contrib.pprint.utilities :only (prlabel)]
   [clojure.contrib.duck-streams :only [make-parents]]
   [clojure.contrib.java-utils :only [file]]
   [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
   [autodoc.params :only (merge-params params)]
   [autodoc.load-files :only (load-namespaces)]
   [autodoc.build-html :only (make-all-pages)])
  (:gen-class))

(defn make-doc-dir [] (make-parents (file (params :output-directory) "foo")))

(defn build-html 
  "Build the documentation (default command)"
  [& _]
  (when (params :do-load)
    (load-namespaces))
  (make-doc-dir)
  (make-all-pages))

(defn sym-to-var [sym] 
  (find-var (symbol "autodoc.autodoc" (name sym))))

(declare commands)

(defn help
  "Print this help message"
  [& _]
  (prlabel help *out*)
  ;; TODO: We should have a fixed width tab (~20t) between the terms below, but something's funky
  (cl-format true 
             "Usage: autodoc [params] cmd args~%~%~
Available commands:~%~:{~a: ~a~%~}"
             (for [cmd commands]
               [cmd (:doc (meta (sym-to-var cmd)))])))

(def commands ['build-html 'help])

(defn autodoc
  ([myparams] (autodoc params nil))
  ([myparams cmd & cmd-args]
     (merge-params myparams)
     (if (nil? (params :namespaces-to-document))
       (merge-params {:namespaces-to-document
                      (find-namespaces-in-dir (file (params :src-dir) (params :src-root)))}))
     (if-let [cmd-sym ((set commands) (symbol (or cmd 'build-html)))]
       (apply (sym-to-var cmd-sym) cmd-args)
       (do
         (cl-format true "Unknown autodoc command: ~a~%" cmd)
         (help)))))

(defn -main [& args]
  (apply autodoc {} args))
