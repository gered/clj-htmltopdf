(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [clj-htmltopdf.objects :as obj]
    [clj-htmltopdf.options :as o]
    [clj-htmltopdf.watermark :as w])
  (:import
    [java.io InputStream OutputStream PipedInputStream PipedOutputStream]
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
    (XRLog/setLoggingEnabled false)))

(defn prepare-html
  [in options]
  (let [html     (read-html-string in)
        html-doc (Jsoup/parse html)]
    (o/inject-options-into-html! html-doc options)
    (if (get-in options [:debug :display-html?])
      (println (str html-doc)))
    html-doc))

(defn write-pdf!
  ^InputStream [^Document html-doc options]
  (let [builder  (PdfRendererBuilder.)
        base-uri (o/->base-uri options)]
    (obj/set-object-drawer-factory builder options)
    (.withW3cDocument builder (DOMBuilder/jsoup2DOM html-doc) base-uri)
    (let [piped-in  (PipedInputStream.)
          piped-out (PipedOutputStream. piped-in)]
      (future
        (try
          (with-open [os piped-out]
            (.toStream builder os)
            (.run builder))
          (catch Exception ex
            (println "Exception while rendering PDF" ex))))
      piped-in)))

(defn ->pdf
  [in out & [options]]
  (let [options  (o/get-final-options options)
        html-doc (prepare-html in options)]
    (configure-logging! options)
    (let [pdf (write-pdf! html-doc options)
          out (->output-stream out)]
      (if (:watermark options)
        (w/write-watermark! pdf out options)
        (with-open [os out]
          (io/copy pdf os)
          os)))))
