---
author: ajchemist
---


# Runner


[![clojars badge](https://img.shields.io/clojars/v/io.github.ajchemist/runner.svg?style=flat-square)](https://clojars.org/io.github.ajchemist/runner)


## Dev


- <http://localhost:9631/dashboard> - shadow-cljs dashboard
- <http://localhost:5000> - example web app entrypoint


## Heroku


``` shell
heroku container:login
heroku created
heroku container:push web
heroku container:release web
heroku open
```
