(comment
  (do 
    (refer 'com.infolace.gen-docs.collect-info)
    (refer 'com.infolace.gen-docs.build-html)
    (refer 'clojure.contrib.pprint))

  (do
    (load "com/infolace/gen_docs/build_html")
    (make-overview (contrib-info))
    )
)
