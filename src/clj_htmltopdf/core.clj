(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [clj-htmltopdf.options :as o])
  (:import
    [java.awt.geom Point2D$Float]
    [java.io InputStream OutputStream PipedInputStream PipedOutputStream]
    [com.openhtmltopdf DOMBuilder]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.util XRLog]
    [org.apache.pdfbox.pdmodel PDDocument PDPage PDPageContentStream PDPageContentStream$AppendMode]
    [org.apache.pdfbox.pdmodel.common PDRectangle]
    [org.apache.pdfbox.pdmodel.font PDType1Font PDFont]
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

(def watermark-fonts
  {"times-roman"           PDType1Font/TIMES_ROMAN
   "times-bold"            PDType1Font/TIMES_BOLD
   "times-italic"          PDType1Font/TIMES_ITALIC
   "times-bolditalic"      PDType1Font/TIMES_BOLD_ITALIC
   "helvetica"             PDType1Font/HELVETICA
   "helvetica-bold"        PDType1Font/HELVETICA_BOLD
   "helvetica-oblique"     PDType1Font/HELVETICA_OBLIQUE
   "helvetica-boldoblique" PDType1Font/HELVETICA_BOLD_OBLIQUE
   "courier"               PDType1Font/COURIER
   "courier-bold"          PDType1Font/COURIER_BOLD
   "courier-oblique"       PDType1Font/COURIER_OBLIQUE
   "courier-boldoblique"   PDType1Font/COURIER_BOLD_OBLIQUE})

(defn render-watermark!
  [^PDPage page ^PDPageContentStream cs options]
  (let [font          (or (get watermark-fonts (:font options))
                          (get watermark-fonts "helvetica-bold"))
        font-size     (float (or (:font-size options) 36.0))
        font-color    (or (:color options) [0 0 0])
        text          (:text options)
        text-width    (/ (* (.getStringWidth font text) font-size) 1000.0)
        text-height   (/ (* (.getHeight (.getFontBoundingBox (.getFontDescriptor ^PDFont font))) font-size) 1000.0)
        rotation      (float (or (:rotation options) 0))
        page-size     (.getMediaBox page)
        page-width    (.getWidth page-size)
        page-height   (.getHeight page-size)
        x             (if (= :center (:x options)) (/ page-width 2) (float (:x options)))
        y             (if (= :center (:y options)) (/ page-height 2) (float (:y options)))
        transform     (doto (Matrix.)
                        (.translate x y)
                        (.rotate (Math/toRadians rotation))
                        (.translate (- (/ text-width 2)) (- (/ text-height 2))))]
    (when (:opacity options)
      (let [r0 (PDExtendedGraphicsState.)]
        (.setNonStrokingAlphaConstant r0 (float (:opacity options)))
        (.setGraphicsStateParameters cs r0)))
    (.beginText cs)
    (.setFont cs font font-size)
    (.setNonStrokingColor cs (int (nth font-color 0)) (int (nth font-color 1)) (int (nth font-color 2)))
    (.setTextMatrix cs transform)
    (.showText cs text)
    (.endText cs)))

(defn write-watermark!
  [^InputStream pdf ^OutputStream out {:keys [watermark] :as options}]
  (with-open [doc (PDDocument/load pdf)]
    (doseq [^PDPage page (.getPages doc)]
      (let [cs (PDPageContentStream. doc page PDPageContentStream$AppendMode/APPEND true true)]
        (with-open [cs cs]
          (if (map? watermark)
            (render-watermark! page cs watermark)
            (watermark page cs)))))
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
