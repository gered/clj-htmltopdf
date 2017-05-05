(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [clj-htmltopdf.options :as o])
  (:import
    [java.io InputStream OutputStream PipedInputStream PipedOutputStream]
    [com.openhtmltopdf DOMBuilder]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.util XRLog]
    [org.apache.pdfbox.pdmodel PDDocument PDPage PDPageContentStream PDPageContentStream$AppendMode]
    [org.apache.pdfbox.pdmodel.font PDType1Font]
    [org.apache.pdfbox.pdmodel.graphics.state PDExtendedGraphicsState]
    [org.apache.pdfbox.util Matrix]
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
    (if (get-in options [:debug :display-html?])
      (println (str html-doc)))
    html-doc))

(defn write-pdf!
  [^Document html-doc ^String base-uri]
  (let [builder (PdfRendererBuilder.)]
    (.withW3cDocument builder (DOMBuilder/jsoup2DOM html-doc) base-uri)
    (let [piped-in  (PipedInputStream.)
          piped-out (PipedOutputStream. piped-in)]
      (future
        (with-open [os piped-out]
          (.toStream builder os)
          (.run builder)))
      piped-in)))

(defn write-watermark!
  [^InputStream pdf ^OutputStream out {:keys [watermark] :as options}]
  (with-open [doc (PDDocument/load pdf)]
    (doseq [^PDPage page (.getPages doc)]
      (let [cs (PDPageContentStream. doc page PDPageContentStream$AppendMode/APPEND true true)]
        (with-open [cs cs]
          (watermark page cs))))
    (.save doc out)
    out))

(defn ->pdf
  [in out & [options]]
  (let [options  (o/get-final-options options)
        html-doc (prepare-html in options)]
    (configure-logging! options)
    (let [result (write-pdf! html-doc (o/->base-uri options))
          out    (->output-stream out)]
      (if (:watermark options)
        (write-watermark! result out options)
        (with-open [os out]
          (io/copy result os)
          os)))))
