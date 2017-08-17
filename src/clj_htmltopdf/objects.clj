(ns clj-htmltopdf.objects
  (:require
    [clojure.string :as string])
  (:import
    [com.openhtmltopdf.extend FSObjectDrawer FSObjectDrawerFactory OutputDevice OutputDeviceGraphicsDrawer]
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder]
    [org.w3c.dom Element NamedNodeMap]))

(defn element-attrs->map
  [^Element element]
  (let [attributes (.getAttributes element)]
    (reduce
      (fn [m idx]
        (let [node  (.item attributes (int idx))
              name  (.getNodeName node)
              value (.getNodeValue node)]
          (assoc m (keyword name) value)))
      {}
      (range (.getLength attributes)))))

(defn ->object-drawer-by-id
  ^FSObjectDrawer [f]
  (reify FSObjectDrawer
    (drawObject [_ element x y width height output-device rendering-context dots-per-pixel]
      (.drawWithGraphics
        ^OutputDevice output-device
        (float x)
        (float y)
        (float (/ width dots-per-pixel))
        (float (/ height dots-per-pixel))
        (reify OutputDeviceGraphicsDrawer
          (render [_ graphics2d]
            (f (element-attrs->map element) graphics2d)))))))

(defn ->object-drawer-by-id-factory
  ^FSObjectDrawerFactory [options]
  (reify FSObjectDrawerFactory
    (^FSObjectDrawer createDrawer [_ ^Element element]
      (if (.hasAttribute element "id")
        (let [element-id (.getAttribute element "id")]
          (if-let [f (get-in options [:objects :by-id element-id])]
            (->object-drawer-by-id f)))))))

(defn set-object-drawer-factory
  [^PdfRendererBuilder builder options]
  (let [factory (->object-drawer-by-id-factory options)]
    (.useObjectDrawerFactory builder factory)))
