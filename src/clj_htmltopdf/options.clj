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

(defn append-meta-tag!
  ^Element [^Element parent name content]
  (let [element (.appendElement parent "meta")]
    (.attr element "name" (str name))
    (.attr element "content" (str content))))

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
    [[:size
      (string/trim
        (str
          (cond
            (keyword? size)    (name size)
            (sequential? size) (string/join " " size)
            :else              size)
          " "
          (if (keyword? orientation)
            (name orientation)
            orientation)))]]))

(defn ->page-margin-css
  [{:keys [margin] :as page-options}]
  (let [default-margin (get-in default-options [:page :margin])]
    (cond
      (keyword? margin)
      (case margin
        :normal   [[:margin default-margin]]
        :narrow   [[:margin "0.5in"]]
        :moderate [[:margin "1.0in 0.75in"]]
        :wide     [[:margin "1.0in 2.0in"]])

      (map? margin)
      [[:margin-left (or (:left margin) default-margin)]
       [:margin-top (or (:top margin) default-margin)]
       [:margin-right (or (:right margin) default-margin)]
       [:margin-bottom (or (:bottom margin) default-margin)]]

      (sequential? margin)
      [[:margin (string/join " " margin)]]

      :else
      [[:margin (str (or margin default-margin))]])))

(defn sanitize-margin-box-text
  [^String s]
  (if-not (string/blank? s)
    (str \" (string/replace s "\"" "\\\"") \")))

(defn parse-margin-box-paging-content
  [paging-content]
  (->> paging-content
       (map
         #(case %
            :page (str "counter(page)")
            :pages (str "counter(pages)")
            (str (sanitize-margin-box-text %))))
       (string/join " ")))

(defn ->page-margin-boxes-declaration-css
  [{:keys [margin-box] :as page-options}]
  (mapv
    (fn [[box-name {:keys [text element content paging] :as box-properties}]]
      [(str "@" (name box-name))
       (merge
         {:content
          (cond
            content content
            text    (sanitize-margin-box-text text)
            element (str "element(" (name box-name) ")")
            paging  (parse-margin-box-paging-content (if (sequential? paging) paging [paging])))}
         (dissoc box-properties :text :element :content :paging))])
    margin-box))

(defn ->page-margin-boxes-running-element-css
  [{:keys [margin-box] :as page-options}]
  (->> margin-box
       (filter #(:element (second %)))
       (mapv
         (fn [[box-name {:keys [element] :as box-properties}]]
           [(str "#margin-box-" (name box-name)) {:position (str "running(" (name box-name) ")")}]))))

(defn page-options->css
  [page-options]
  (let [page-options (merge (:page default-options) page-options)]
    (vec
      (concat
        [(into
           ["@page"
            (->> (concat
                   (->page-size-css page-options)
                   (->page-margin-css page-options))
                 (remove #(nil? (second %)))
                 (reduce #(assoc %1 (first %2) (second %2)) {})
                 (merge {:font-family "sans-serif"}))]
           (->page-margin-boxes-declaration-css page-options))]
        (->page-margin-boxes-running-element-css page-options)))))

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
  (append-css-link-tag! parent (io/resource "htmltopdf-base.css"))
  (if-let [additional-styles (:styles styles)]
    (cond
      (sequential? additional-styles) (append-stylesheet-link-tags! parent additional-styles)
      (string? additional-styles)     (append-stylesheet-link-tags! parent [additional-styles]))))

(defn append-doc-meta-tags!
  [^Element parent options]
  (let [{:keys [author keywords subject title]} (:doc options)]
    (if author (append-meta-tag! parent "author" author))
    (if keywords (append-meta-tag! parent "keywords" keywords))
    (if subject (append-meta-tag! parent "subject" subject))
    (if title (append-meta-tag! parent "title" title))))

(defn inject-options-into-html!
  [^Document doc options]
  (let [head-tag (-> doc (.select "head") (.first))
        styles   (:styles options)]
    (if (:doc options) (append-doc-meta-tags! head-tag options))
    (if (:page options) (append-style-tag! head-tag (page-options->css (:page options))))
    (cond
      (sequential? styles)              (append-stylesheet-link-tags! head-tag styles)
      (string? styles)                  (append-stylesheet-link-tags! head-tag [styles])
      (or (true? styles) (map? styles)) (build-and-append-base-css-styles! head-tag styles))
    doc))
