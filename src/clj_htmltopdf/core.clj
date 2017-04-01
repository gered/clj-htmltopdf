(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [hiccup.page :as h])
  (:import
    [java.io OutputStream]
    [com.openhtmltopdf DOMBuilder]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.util XRLog]
    [org.jsoup Jsoup]))

(defn- read-html
  [in]
  (try
    (cond
      (string? in)     in
      (sequential? in) (h/html5 {} in)
      :else            (with-open [r (io/reader in)]
                         (slurp r)))
    (catch Exception ex
      (throw (Exception. "Error reading HTML from input." ex)))))

(defn- ->output-stream
  [out]
  (try
    (if (instance? OutputStream out)
      out
      (io/output-stream out))
    (catch Exception ex
      (throw (Exception. "Error preparing an OutputStream from output given." ex)))))

(defn- parse-html5
  [^String html]
  (try
    (let [parsed-doc (Jsoup/parse html)]
      (DOMBuilder/jsoup2DOM parsed-doc))
    (catch Exception ex
      (throw (Exception. "Error parsing input as HTML5." ex)))))

(def default-options
  {:logging?          false
   :base-uri          ""
   :include-base-css? true
   :page              {:size        :letter
                       :orientation :portrait}})

(defn ->pdf
  [in out & [options]]
  (let [options  (merge default-options options)
        builder  (PdfRendererBuilder.)
        base-uri (str (:base-uri options))
        html     (read-html in)
        html-doc (parse-html5 html)
        output   (->output-stream out)]
    (if (:logging? options)
      (let [logger (:logger options)]
        (if logger (XRLog/setLoggerImpl logger))
        (XRLog/setLoggingEnabled true))
      (XRLog/setLoggingEnabled false))
    (.withW3cDocument builder html-doc base-uri)
    (with-open [os output]
      (.toStream builder os)
      (try
        (.run builder)
        os
        (catch Exception ex
          (throw (Exception. "Failed to convert to PDF." ex)))))))

