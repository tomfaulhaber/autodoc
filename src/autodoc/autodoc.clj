(ns autodoc.autodoc
  (:use 
   [clojure.contrib.pprint :only (cl-format)]
   [clojure.contrib.pprint.utilities :only (prlabel)]
   [clojure.contrib.duck-streams :only [make-parents]]
   [clojure.contrib.java-utils :only [file]]
   [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
   [autodoc.params :only (merge-params params params-help process-command-line)]
   [autodoc.load-files :only (load-namespaces)]
   [autodoc.build-html :only (make-all-pages)]
   [autodoc.copy-statics :only (copy-statics)])
  (:gen-class))

(defn make-doc-dir [] (make-parents (file (params :output-path) "foo")))

(defn build-html 
  "Build the documentation (default command)"
  [& _]
  (load-namespaces)
  (make-doc-dir)
  (copy-statics)
  (make-all-pages))

(defn sym-to-var [sym] 
  (find-var (symbol "autodoc.autodoc" (name sym))))

(declare commands)

(defn help
  "Print this help message"
  [& _]
  (cl-format true 
             "Usage: autodoc [params] cmd args~%~%") 
  ;; TODO: We should have a fixed width tab (~20t) between the terms below, but something's funky
  (cl-format true "Available commands:~%~:{   ~a: ~a~%~}~%"
             (for [cmd commands]
               [cmd (:doc (meta (sym-to-var cmd)))]))
    (params-help *out*))

(def commands ['build-html 'help])

(defn directory-name []
  (.replaceFirst (.getParent (.getAbsoluteFile (file "."))) ".*/" ""))

;;; Leiningen likes to make the source path include the absolute path to this directory
;;; for whatever reason, so we clean it up on the way in.
(defn clean-params [params]
  (when (and (params :root) (params :source-path))
    (update-in params [:source-path]
               #(if (.startsWith % (params :root)) 
                  (.substring % (inc (count (params :root))))
                  %))))

(defn autodoc
  ([myparams] (autodoc myparams nil))
  ([myparams cmd & cmd-args]
     (prlabel autodoc myparams)
     (merge-params (clean-params myparams))
     (if (nil? (params :namespaces-to-document))
       (merge-params {:namespaces-to-document
                      (map
                       name
                       (find-namespaces-in-dir
                        (file (params :root)
                              (params :source-path))))}))
     (if-let [cmd-sym ((set commands) (symbol (or cmd 'build-html)))]
       (apply (sym-to-var cmd-sym) cmd-args)
       (do
         (cl-format true "Unknown autodoc command: ~a~%" cmd)
         (help)))))

(defn -main [& args]
  (if-let [[params args] (try 
                          (process-command-line args)
                          (catch RuntimeException e
                            (println (.getMessage e))
                            (prn)
                            (help)))]
    (apply autodoc (merge {:name (directory-name)} params) args)))
