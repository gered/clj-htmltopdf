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
  [^Document html-doc options]
  (let [builder  (PdfRendererBuilder.)
        base-uri (o/->base-uri options)]
    (obj/set-object-drawer-factory builder options)
    (.withW3cDocument builder (DOMBuilder/jsoup2DOM html-doc) base-uri)
    (let [piped-in  (PipedInputStream.)
          piped-out (PipedOutputStream. piped-in)
          renderer  (future
                      (try
                        (with-open [os piped-out]
                          (.toStream builder os)
                          (.run builder))
                        (catch Exception ex
                          (throw (Exception. "Exception while rendering PDF" ex)))))]
      {:pdf      piped-in
       :renderer renderer})))

(defn ->pdf
  [in out & [options]]
  (let [options  (o/get-final-options options)
        html-doc (prepare-html in options)]
    (configure-logging! options)
    (let [{:keys [pdf renderer]} (write-pdf! html-doc options)
          out    (->output-stream out)
          result (if (:watermark options)
                   (w/write-watermark! pdf out options)
                   (with-open [os out]
                     (io/copy pdf os)
                     os))]
      ; this is a little weird, but because of the whole piped stream thing in write-pdf!, we need to render the
      ; PDF in a future. if something throws an exception during rendering, it would otherwise get eaten silently by
      ; the future... except if we deref the future! thus the explicit call to deref it here
      (deref renderer)
      result)))

