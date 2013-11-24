(ns autodoc.git-tools
  (:use [clojure.java.shell :only [sh with-sh-dir]]
        [clojure.pprint :only [cl-format]]
        [autodoc.build-html :only [branch-subdir]])
  (:import [java.io File]))

(defn offset-path
  "Returns a new file that is specified as an offset from root (assumes that
root is an ancestor of file)"
  [root file]
  (let [root-path (.getCanonicalPath root)
        file-path (.getCanonicalPath file)]
    (if (= root-path file-path)
      (File. ".")
      (File. (.substring file-path (inc (.length root-path)))))))

(defn git-dir? [dir]
  "Return true if dir (a java.io.File) has a .git subdirectory (i.e. it's the
root of a git repo)"
  (.exists (File. dir ".git")))

(defn current-branch
  "Return the name of currently checked out branch in dir"
  [dir]
  (with-sh-dir dir
    (when-let [branch-str (first
                           (filter #(.startsWith % "*")
                                   (.split (:out (sh "git" "branch")) "\n")))]
      (.substring branch-str 2))))

(defn has-remote?
  "return true if there is a remote called origin that we could push back to"
  [dir]
  (with-sh-dir dir
    (some #(= % "origin") (.split (:out (sh "git" "remote")) "\n"))))

(defn stage-new-doc-files
  "Add any new supplementary documents to the git staging area"
  [dir branches]
  (let [dirs (filter #(.exists (File. dir %))
                     (map #(.getPath (File. (File. %) "doc"))
                          (cons "." (map branch-subdir (next branches)))))]
    (with-sh-dir dir
      (println (:out (apply sh "git" "add" "-v" dirs))))))

(defn stage-new-api-files
  "Add any new API namespace files to the git staging area"
  [dir]
  (when-let [files (map (comp #(.getPath %) (partial offset-path dir))
                        (filter #(.endsWith (.getPath %) "-api.html")
                                (file-seq dir)))]
    (with-sh-dir dir
      (println (:out (apply sh "git" "add" "-v" files))))))

(defn stage-new-index-files
  "Add any new index-XXX.clj files to the git staging area"
  [dir]
  (when-let [files (map (comp #(.getPath %) (partial offset-path dir))
                        (filter #(re-matches #"index-.*\.clj" (.getName %))
                                (file-seq dir)))]
    (with-sh-dir dir
      (println (:out (apply sh "git" "add" "-v" files))))))

(defn stage-modified-files
  "Add any changed files to the git staging area"
  [dir]
  (with-sh-dir dir
    (println (:out (sh "git" "add" "-u" "-v" ".")))))

(defn git-hash
  "Get the git hash for the head of the given branch (or tag)"
  [dir head len]
  (with-sh-dir dir
    (.substring (.trim (:out (sh "git" "rev-parse" head))) 0 len)))

(defn comment-for
  "Construct a git comment for all the appropriate branches"
  [dir branches]
  (cl-format nil "Autodoc commit for ~{~{~@[~a/~]~a~}~^, ~}"
             (if branches
               (for [name branches] [name (git-hash dir name 8)])
               [[nil (git-hash dir "HEAD" 8)]])))

(defn git-commit
  "Commit the staged files in dir (a java.io.File)."
  [dir comment]
  (with-sh-dir dir
    (println (:out (sh "git" "commit" "-m" comment)))))

(defn git-push
  "Push the commit to a remote, if defined"
  [dir]
  (with-sh-dir dir
    (println (:out (sh "git" "push" "origin" (current-branch dir))))))

(defn git-reset
  "Reset the directory back to the gh-pages branch at the origin"
  [dir]
  (with-sh-dir dir
    (println (:out (sh "git" "reset" "--hard" "origin/gh-pages")))))

(defn git-pull
  "Pull the latest checked in version of the code from the origin"
  [dir]
  (with-sh-dir dir
    (println (:out (sh "git" "pull")))))

(defn autodoc-refresh-dir
  "Reset the autodoc directory to the latest directory at the origin.
WARNING: this will kill any uncommitted changes."
  [doc-dir]
  (git-reset doc-dir)
  (git-pull doc-dir))

(defn autodoc-commit
  "Stage and commit all new and changed files in the autodoc tree"
  [src-dir doc-dir branches]
  (stage-new-doc-files doc-dir branches)
  (stage-new-api-files doc-dir)
  (stage-new-index-files doc-dir)
  (stage-modified-files doc-dir)
  (git-commit doc-dir (comment-for src-dir branches))
  (when (has-remote? doc-dir)
    (git-push doc-dir)))
