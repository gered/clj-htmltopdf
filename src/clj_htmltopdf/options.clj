(ns clj-htmltopdf.options
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clj-htmltopdf.css :as css])
  (:import
    [org.jsoup.nodes Document Element]
    [org.jsoup.parser Tag]))

(def default-options
  {:logging?          false
   :base-uri          ""
   :include-base-css? true
   :page              {:size        :letter
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

(defn append-page-options-style-tag!
  ^Element [^Element parent options]
  (let [styles  (-> (:page options)
                    (page-options->css)
                    (css/css->str))
        element (.appendElement parent "style")]
    (.attr element "type" "text/css")
    (.text element styles)))

(defn append-base-css-link-tag!
  ^Element [^Element parent]
  (let [element (.appendElement parent "link")]
    (.attr element "type" "text/css")
    (.attr element "rel" "stylesheet")
    (.attr element "href" (str (io/resource "htmltopdf-base.css")))))

(defn inject-options-into-html!
  [^Document doc options]
  (let [base-uri (->base-uri options)
        head-tag (-> doc (.select "head") (.first))]
    (append-page-options-style-tag! head-tag options)
    (if (:include-base-css? options) (append-base-css-link-tag! head-tag))
    doc))
