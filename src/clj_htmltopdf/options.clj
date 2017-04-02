(ns clj-htmltopdf.options
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clj-htmltopdf.css :as css]
    [clj-htmltopdf.utils :as utils])
  (:import
    [org.jsoup.nodes Document Element]
    [org.jsoup.parser Tag]))

(defn append-style-tag!
  ^Element [^Element parent css-styles]
  (let [element (.appendElement parent "style")]
    (.attr element "type" "text/css")
    (.text element
           (if (string? css-styles)
             css-styles
             (css/css->str css-styles)))))

(defn append-css-link-tag!
  ^Element [^Element parent href]
  (let [element (.appendElement parent "link")]
    (.attr element "type" "text/css")
    (.attr element "rel" "stylesheet")
    (.attr element "href" (str href))))

(def default-options
  {:logging? false
   :base-uri ""
   :styles   true
   :page     {:size        :letter
              :orientation :portrait
              :margin      "1.0in"}})

(defn get-final-options
  [options]
  (let [final-options (merge default-options options)]
    (if (get-in final-options [:debug :display-options?])
      (clojure.pprint/pprint final-options))
    final-options))

(defn ->base-uri
  [options]
  (str (:base-uri options)))

(defn ->page-size-css
  [{:keys [size orientation] :as page-options}]
  (if (or size orientation)
    (string/trim
      (str
        (cond
          (keyword? size)    (name size)
          (sequential? size) (string/join " " size)
          :else              size)
        " "
        (if (keyword? orientation)
          (name orientation)
          orientation)))))

(defn page-options->css
  [page-options]
  (let [styles (->> [[:size (->page-size-css page-options)]]
                    (into [])
                    (remove #(nil? (second %)))
                    (reduce #(assoc %1 (first %2) (second %2)) {}))]
    [["@page" styles]]))

(defn append-stylesheet-link-tags!
  [^Element parent stylesheets]
  (doseq [stylesheet stylesheets]
    (append-css-link-tag! parent stylesheet)))

(defn build-body-css-style
  [styles]
  [[:body
    (merge
      {:font-family      "sans-serif"
       :font-size        "12pt"
       :line-height      "1.3"
       :background-color "#fff"
       :color            "#000"}
      (if (map? styles)
        (dissoc styles :styles :fonts)))]])

(defn build-font-face-styles
  [styles]
  (if-let [fonts (seq (:fonts styles))]
    (mapv
      (fn [{:keys [font-family src]}]
        ["@font-face"
         {:font-family font-family
          :src         (str "url(\"" (utils/string->url-or-file src) "\")")}])
      fonts)))

(defn build-base-css-styles
  [styles]
  (vec
    (concat
      (build-body-css-style styles)
      (build-font-face-styles styles))))

(defn build-and-append-base-css-styles!
  [^Element parent styles]
  (append-style-tag! parent (build-base-css-styles styles))
  (if-let [additional-styles (:styles styles)]
    (cond
      (sequential? additional-styles) (append-stylesheet-link-tags! parent additional-styles)
      (string? additional-styles)     (append-stylesheet-link-tags! parent [additional-styles]))))

(defn inject-options-into-html!
  [^Document doc options]
  (let [head-tag (-> doc (.select "head") (.first))
        styles   (:styles options)]
    (if (:page options) (append-style-tag! head-tag (page-options->css (:page options))))
    (cond
      (sequential? styles)              (append-stylesheet-link-tags! head-tag styles)
      (string? styles)                  (append-stylesheet-link-tags! head-tag [styles])
      (or (true? styles) (map? styles)) (build-and-append-base-css-styles! head-tag styles))
    doc))
