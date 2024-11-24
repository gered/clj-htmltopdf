(ns clj-htmltopdf.watermark
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io InputStream OutputStream]
    [org.apache.pdfbox Loader]
    [org.apache.pdfbox.pdmodel PDDocument PDPage PDPageContentStream PDPageContentStream$AppendMode]
    [org.apache.pdfbox.pdmodel.font PDType1Font PDFont Standard14Fonts$FontName]
    [org.apache.pdfbox.pdmodel.graphics.image PDImageXObject]
    [org.apache.pdfbox.pdmodel.graphics.state PDExtendedGraphicsState]
    [org.apache.pdfbox.util Matrix]))

(declare ^:dynamic *pdf-images*)

(defn create-pdf-image
  "Creates an XObject image from an image file. Caches this XObject for this particular PDF document so that if the
   image is rendered into the PDF multiple times, the PDF will reference the same image instead of creating duplicates
   (which would potentially bump up the file size drastically).
   This function must only be called from within code invoked by write-watermark!."
  ^PDImageXObject [file ^PDDocument doc]
  (if-not *pdf-images*
    (throw (ex-info "Could not create PDF XObject image. Image cache has not been initialized." {:file file})))
  (let [image-file (io/file file)
        path       (.getPath image-file)]
    (if-let [existing-image (get @*pdf-images* path)]
      existing-image
      (let [image (PDImageXObject/createFromFileByContent image-file doc)]
        (swap! *pdf-images* assoc path image)
        image))))

; TODO: this is a temporary measure to allow at least _some_ font customizability for watermarks
;       until something more comprehensive can be implemented such as allowing loading of external
;       TTF fonts via PDTrueTypeFont.loadTTF()
(def watermark-fonts
  {"times-roman"           Standard14Fonts$FontName/TIMES_ROMAN
   "times-bold"            Standard14Fonts$FontName/TIMES_BOLD
   "times-italic"          Standard14Fonts$FontName/TIMES_ITALIC
   "times-bolditalic"      Standard14Fonts$FontName/TIMES_BOLD_ITALIC
   "helvetica"             Standard14Fonts$FontName/HELVETICA
   "helvetica-bold"        Standard14Fonts$FontName/HELVETICA_BOLD
   "helvetica-oblique"     Standard14Fonts$FontName/HELVETICA_OBLIQUE
   "helvetica-boldoblique" Standard14Fonts$FontName/HELVETICA_BOLD_OBLIQUE
   "courier"               Standard14Fonts$FontName/COURIER
   "courier-bold"          Standard14Fonts$FontName/COURIER_BOLD
   "courier-oblique"       Standard14Fonts$FontName/COURIER_OBLIQUE
   "courier-boldoblique"   Standard14Fonts$FontName/COURIER_BOLD_OBLIQUE})

(defn render-text-watermark!
  [^PDDocument doc ^PDPage page ^PDPageContentStream cs options]
  (let [font        (PDType1Font. (or (get watermark-fonts (:font options))
                                      (get watermark-fonts "helvetica-bold")))
        font-size   (float (or (:font-size options) 36.0))
        font-color  (or (:color options) [0 0 0])
        text        (:text options)
        text-width  (/ (* (.getStringWidth font text) font-size) 1000.0)
        text-height (/ (* (.getHeight (.getFontBoundingBox (.getFontDescriptor ^PDFont font))) font-size) 1000.0)
        rotation    (float (or (:rotation options) 0))
        page-size   (.getMediaBox page)
        page-width  (.getWidth page-size)
        page-height (.getHeight page-size)
        x           (if (= :center (:x options)) (/ page-width 2) (float (:x options)))
        y           (if (= :center (:y options)) (/ page-height 2) (float (:y options)))
        transform   (doto (Matrix.)
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

(defn render-image-watermark!
  [^PDDocument doc ^PDPage page ^PDPageContentStream cs options]
  (let [image        (create-pdf-image (:image options) doc)
        image-width  (.getWidth image)
        image-height (.getHeight image)
        rotation     (float (or (:rotation options) 0))
        page-size    (.getMediaBox page)
        page-width   (.getWidth page-size)
        page-height  (.getHeight page-size)
        x            (if (= :center (:x options)) (/ page-width 2) (float (:x options)))
        y            (if (= :center (:y options)) (/ page-height 2) (float (:y options)))
        transform    (doto (Matrix.)
                       (.translate x y)
                       (.scale (or (:scale-x options) 1.0) (or (:scale-y options) 1.0))
                       (.rotate (Math/toRadians rotation))
                       (.translate (- (/ image-width 2)) (- (/ image-height 2))))]
    (when (:opacity options)
      (let [r0 (PDExtendedGraphicsState.)]
        (.setNonStrokingAlphaConstant r0 (float (:opacity options)))
        (.setGraphicsStateParameters cs r0)))
    (.transform cs transform)
    (.drawImage cs image (float 0) (float 0))))

(defn render-watermark!
  [^PDDocument doc ^PDPage page ^PDPageContentStream cs options]
  (cond
    (:text options)  (render-text-watermark! doc page cs options)
    (:image options) (render-image-watermark! doc page cs options)
    :else            (throw (Exception. "Unknown type of watermark. Either :text or :image should be specified."))))

(defn write-watermark!
  [^InputStream pdf ^OutputStream out {:keys [watermark] :as options}]
  (with-open [doc (Loader/loadPDF pdf)]
    (binding [*pdf-images* (atom {})]
      (doseq [^PDPage page (.getPages doc)]
        (let [cs (PDPageContentStream. doc page PDPageContentStream$AppendMode/APPEND true true)]
          (with-open [cs cs]
            (if (map? watermark)
              (render-watermark! doc page cs watermark)
              (watermark doc page cs))))))
    (.save doc out)
    out))
