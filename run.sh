#!/bin/sh

java -cp autodoc-standalone.jar clojure.main -e "(do (use 'autodoc.gen-docs) (gen-docs \"params/$1\" true))"
