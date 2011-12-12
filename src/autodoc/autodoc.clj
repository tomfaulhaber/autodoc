(ns autodoc.autodoc
  (:use 
   [clojure.pprint :only (cl-format)]
   [clojure.java.io :only [file make-parents]]
   [clojure.tools.namespace :only [find-namespaces-in-dir]]
   [autodoc.params :only [merge-params params params-from-dir params-from-file
                          params-help process-command-line]]
   [autodoc.load-files :only (load-namespaces)]
   [autodoc.gen-docs :only (gen-branch-docs)]
   [autodoc.build-html :only (make-all-pages)]
   [autodoc.copy-statics :only (copy-statics)])
  (:import
   [java.io File FileNotFoundException])
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
  (if (and (params :root) (params :source-path))
    (update-in params [:source-path]
               #(if (.startsWith % (params :root)) 
                  (.substring % (inc (count (params :root))))
                  %))
    params))

;;; We really shouldn't be special-casing this!
(defn has-branches? []
  (not (let [branches (params :branches)]
         (and (== (count branches) 1) 
              (nil? (ffirst branches))))))

(defn autodoc
  ([myparams] (autodoc myparams nil))
  ([myparams cmd & cmd-args]
     (merge-params {:name (directory-name)})
     (when-let [dir (myparams :param-dir)]
       (if (.exists (File. (File. dir) "params.clj"))
         (params-from-dir dir)))
     (when-let [f (myparams :param-file)]
       (when-not (.exists (File. f))
         (throw (FileNotFoundException. (str "Parameter file \"" f "\" doesn't exist."))))
       (params-from-file f (myparams :param-key)))
     (merge-params (clean-params myparams))
     (if (has-branches?) 
       (do
         (copy-statics)
         (gen-branch-docs))
       (do 
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
             (help)))))))

(defn -main [& args]
  (if-let [[params args] (try 
                          (process-command-line args)
                          (catch RuntimeException e
                            (println (.getMessage e))
                            (prn)
                            (help)))]
    (apply autodoc params args))
  (shutdown-agents))
