(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [clj-htmltopdf.options :as o])
  (:import
    [java.io OutputStream]
    [com.openhtmltopdf DOMBuilder]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.util XRLog]
    [org.jsoup Jsoup]
    [org.jsoup.nodes Document]))

(defn- read-html-string
  ^String [in]
  (cond
    (string? in)     in
    (sequential? in) (h/html5 {} in)
    :else            (with-open [r (io/reader in)]
                       (slurp r))))

(defn- ->output-stream
  ^OutputStream [out]
  (if (instance? OutputStream out)
    out
    (io/output-stream out)))

(defn configure-logging!
  [options]
  (if (:logging? options)
    (do
      (if-let [logger (:logger options)]
        (XRLog/setLoggerImpl logger))
      (XRLog/setLoggingEnabled true))
    ; NOTE: a bug in how Open HTML to PDF's XRLog class initializes itself will always result
    ; in an initial little bit of logging output regardless of when we set this to false.
    (XRLog/setLoggingEnabled false)))

(defn prepare-html
  [in options]
  (let [html     (read-html-string in)
        html-doc (Jsoup/parse html)]
    (o/inject-options-into-html! html-doc options)
    html-doc))

(defn write-pdf!
  [^Document html-doc ^String base-uri out]
  (let [builder (PdfRendererBuilder.)
        os      (->output-stream out)]
    (.withW3cDocument builder (DOMBuilder/jsoup2DOM html-doc) base-uri)
    (with-open [os os]
      (.toStream builder os)
      (.run builder)
      os)))

(defn ->pdf
  [in out & [options]]
  (let [options  (merge o/default-options options)
        html-doc (prepare-html in options)]
    (configure-logging! options)
    (write-pdf! html-doc (o/->base-uri options) out)))
