# clj-htmltopdf

A light-weight wrapper for the [Open HTML to PDF](https://github.com/danfickle/openhtmltopdf)
library to make it a little bit easier to use directly from Clojure.

## Leiningen

```
[clj-htmltopdf "0.1"]
```

## Usage

```clojure
(use 'clj-htmltopdf.core)

(->pdf
  [:div
    [:h1 "HTML to PDF"]
    [:p "Glorious!"]]
  "output.pdf")
```

`clj-htmltopdf.core/->pdf` can take a variety of different input and output argument types,
such as strings, `java.io.File` objects, `java.net.URL` objects, streams, etc.

As shown in the above example, a special case is made when the input argument is
[Hiccup](https://github.com/weavejester/hiccup)-style HTML where it will be automatically 
converted to an HTML string before being rendered as a PDF.

## License

Copyright © 2017 Gered King

Distributed under the LGPL3 license.
