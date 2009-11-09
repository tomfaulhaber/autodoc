(ns com.infolace.gen-docs.ant-wrapper
  (:import
   [java.io File]
   [org.apache.tools.ant Project ProjectHelper AntClassLoader DefaultLogger])
  (:use clojure.contrib.shell-out))

(def param-map
     {'work-root-dir '*file-prefix*,
      'src-dir '*src-dir*,
      'output-dir '*output-directory*,
      'built-clojure-jar '*built-clojure-jar*})

(defn ant-wrapper
  [param-dir build-target force]
  (load (str param-dir "/params"))
  (let [p (Project.)
        helper (ProjectHelper/getProjectHelper)
        acl (AntClassLoader. p nil true)
        build-file (File. "build.xml")]
    (doto p
      (.addBuildListener (doto (DefaultLogger.)
                           (.setOutputPrintStream System/out)
                           (.setErrorPrintStream System/err)
                           (.setMessageOutputLevel Project/MSG_INFO)))
      (.setUserProperty "ant.file" (.getAbsolutePath build-file))
      (.setUserProperty "param-dir" param-dir)
      (.setUserProperty "force" (if force "true" "false"))
      (.init)
      (.addReference "ant.projectHelper" helper))
    (doseq [item param-map] 
      (if-let [param-var (find-var (symbol (name (ns-name *ns*)) (name (second item))))]
        (.setUserProperty 
         p
         (name (first item))
         (var-get param-var))))
    (.parse helper p build-file)
    (.executeTarget p build-target)))


;;; Sample code 

;;;   File buildFile = new File("build.xml")
;;;   Project p = new Project()                                    ;
;;;   DefaultLogger consoleLogger = new DefaultLogger()            ;
;;;   consoleLogger.setErrorPrintStream(System.err)                ;
;;;   consoleLogger.setOutputPrintStream(System.out)               ;
;;;   consoleLogger.setMessageOutputLevel(Project.MSG_INFO)        ;
;;;   p.addBuildListener(consoleLogger)                            ;
;;;   p.setUserProperty("ant.file", buildFile.getAbsolutePath())   ;
;;;   p.init()                                                     ;
;;;   ProjectHelper helper = ProjectHelper.getProjectHelper()      ;
;;;   p.addReference("ant.projectHelper", helper)                  ;
;;;   helper.parse(p, buildFile)                                   ;
;;;   p.executeTarget("test")
;;;   p.executeTarget(p.getDefaultTarget()) ;
