# Planjure

[![Build Status](https://travis-ci.org/elben/planjure.svg?branch=master)](https://travis-ci.org/elben/planjure)

Path-planning algorithms and demo in ClojureScript, using Om and core.async.

Or, a self-study in Clojure, ClojureScript, Om and core.async.

Go here for a live version (possibly out-of-date): [http://elbenshira.com/projects/planjure](http://elbenshira.com/projects/planjure).

![Planjure screenshot](https://raw.githubusercontent.com/elben/planjure/master/resources/screenshot.png "Planjure Screenshot")

# Development

```bash
# Build .cljx files into target/generated-src
lein cljx auto

# Build cljs files from src/cljs and target/generated-src
lein cljsbuild auto dev
```

Then, modify `index.html`. Remove this line:

```html
<script src="planjure.min.js" type="text/javascript"></script>
```

And add:

```html
<script src="http://fb.me/react-0.11.1.js"></script>
<script src="out/goog/base.js" type="text/javascript"></script>
<script src="planjure.js" type="text/javascript"></script>
<script type="text/javascript">goog.require("planjure.demo");</script>
```

Running tests:

```bash
lein test

# cljs tests (not working yet)
lein do cljsbuild once test, cljsbuild test
```

# Deploying

```bash
lein cljx
lein cljsbuild once release
```

Then, modify `index.html`. Remove these lines:

```html
<script src="http://fb.me/react-0.11.1.js"></script>
<script src="out/goog/base.js" type="text/javascript"></script>
<script src="planjure.js" type="text/javascript"></script>
<script type="text/javascript">goog.require("planjure.demo");</script>
```

And add this line:

```html
<script src="planjure.min.js" type="text/javascript"></script>
```

To deploy statically somewhere, make sure to include these files:

```
index.html
planjure.min.js
resources/
```
