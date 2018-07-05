(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [clj-htmltopdf.objects :as obj]
    [clj-htmltopdf.options :as opt]
    [clj-htmltopdf.watermark :as w]
    [clj-htmltopdf.utils :as u])
  (:import
    [java.io InputStream OutputStream PipedInputStream PipedOutputStream]
    [java.net URLConnection]
    [java.util Base64]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.svgsupport BatikSVGDrawer]
    [com.openhtmltopdf.util XRLog]
    [org.apache.commons.io IOUtils]
    [org.jsoup Jsoup]
    [org.jsoup.helper W3CDom]
    [org.jsoup.nodes Document]
    [org.jsoup.parser Parser ParseSettings]))

(defn embed-image
  "Reads an image (provided as a filename, InputStream or byte array) and encodes it as a base64 string suitable for
   use in a data url for displaying inline images in <img> tags or for use in CSS."
  [image]
  (try
    (let [is          (if-not (instance? InputStream image)
                        (io/input-stream image)
                        image)
          mime-type   (URLConnection/guessContentTypeFromStream is)
          image-bytes (if (u/byte-array? image) image (IOUtils/toByteArray ^InputStream is))]
      (let [b64-str (.encodeToString (Base64/getEncoder) image-bytes)]
        (if (nil? mime-type)
          (str "data:" b64-str)
          (str "data:" mime-type ";base64," b64-str))))
    (catch Exception ex
      (throw (ex-info "Exception converting image to inline base64 string: " {:image image} ex)))))

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

(defn- configure-logging!
  [options]
  (if (:logging? options)
    (do
      (if-let [logger (:logger options)]
        (XRLog/setLoggerImpl logger))
      (XRLog/setLoggingEnabled true))
    (XRLog/setLoggingEnabled false)))

(defn- ->jsoup-parser
  ^Parser []
  (doto
    (Parser/htmlParser)
    (.settings ParseSettings/preserveCase)))

(defn- jsoup->w3c
  ^org.w3c.dom.Document [^Document jsoup-doc]
  (let [converter (W3CDom.)]
    (.fromJsoup converter jsoup-doc)))

(defn- prepare-html
  [in options]
  (let [base-uri (opt/->base-uri options)
        html     (read-html-string in)
        parser   (->jsoup-parser)
        html-doc (Jsoup/parse html base-uri parser)]
    (opt/inject-options-into-html! html-doc options)
    (if (get-in options [:debug :display-html?])
      (println (str html-doc)))
    html-doc))

(defn- write-pdf!
  [^Document html-doc options]
  (let [builder  (PdfRendererBuilder.)
        base-uri (opt/->base-uri options)
        w3c-doc  (jsoup->w3c html-doc)]
    (opt/set-uri-resolver! builder options)
    (obj/set-object-drawer-factory! builder options)
    (.useSVGDrawer builder (BatikSVGDrawer.))
    (.withW3cDocument builder w3c-doc base-uri)
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
  "Renders HTML to a PDF document.

    in      - HTML to be rendered, provided as: File or InputStream object for an html file, string containing HTML, or
              Hiccup-style HTML
    out     - where to save the PDF to: PDF filename, or an OutputStream
    options - optional map of options to control PDF document styling and other properties

   Returns the OutputStream that the PDF was rendered to."
  [in out & [options]]
  (let [options  (opt/get-final-options options)
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
