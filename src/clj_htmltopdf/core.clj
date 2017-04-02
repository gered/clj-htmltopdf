(ns clj-htmltopdf.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [hiccup.page :as h]
    [clj-htmltopdf.css :as css]
    [clj-htmltopdf.options :as o])
  (:import
    [java.io OutputStream]
    [com.openhtmltopdf DOMBuilder]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [com.openhtmltopdf.util XRLog]
    [org.jsoup Jsoup]
    [org.jsoup.nodes Document Element]
    [org.jsoup.parser Tag]))

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

(defn- parse-jsoup-html
  [^String html]
  (try
    (Jsoup/parse html)
    (catch Exception ex
      (throw (Exception. "Error parsing input HTML to Jsoup Document." ex)))))

(defn- set-jsoup-html-doc
  [^PdfRendererBuilder builder jsoup-doc base-uri]
  (try
    (let [doc (DOMBuilder/jsoup2DOM jsoup-doc)]
      (.withW3cDocument builder doc base-uri))
    (catch Exception ex
      (throw (Exception. "Error setting org.w3c.dom.Document HTML." ex)))))

(defn ->pdf
  [in out & [options]]
  (let [options  (merge o/default-options options)
        builder  (PdfRendererBuilder.)
        html     (read-html in)
        html-doc (parse-jsoup-html html)
        html-doc (o/inject-options-into-html html-doc options)
        output   (->output-stream out)]
    html-doc
    (if (:logging? options)
      (let [logger (:logger options)]
        (if logger (XRLog/setLoggerImpl logger))
        (XRLog/setLoggingEnabled true))
      (XRLog/setLoggingEnabled false))
    (set-jsoup-html-doc builder html-doc (o/->base-uri options))
    (with-open [os output]
      (.toStream builder os)
      (try
        (.run builder)
        os
        (catch Exception ex
          (throw (Exception. "Failed to convert to PDF." ex)))))))

