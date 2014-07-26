# Planjure

[![Build Status](https://travis-ci.org/elben/planjure.svg?branch=master)](https://travis-ci.org/elben/planjure)

Path-planning algorithms and demo in ClojureScript, using Om and core.async.

Or, a self-study in Clojure, ClojureScript, Om and core.async.

Go here for a live version (possibly out-of-date): [http://elbenshira.com/projects/planjure](http://elbenshira.com/projects/planjure).

# Development

```bash
# Build .cljx files into target/generated-src
lein cljx auto

# Build cljs files from src/cljs and target/generated-src
lein cljsbuild auto planjure
```

Running tests:

```bash
lein test

# cljs tests (not working yet)
lein do cljsbuild once planjure-test, cljsbuild test
```
