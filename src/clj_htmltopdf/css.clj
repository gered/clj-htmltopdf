(ns clj-htmltopdf.css
  (:require
    [clojure.string :as string])
  (:import
    [java.lang StringBuilder]))

; everything in this namespace solely exists because garden cannot be used
; to define suitable @page css that we will require.
; and it defines a bunch of its internal extension functionality as private.
; *sigh*

(defn css-rule-name-str
  [names]
  (let [names (map #(if (keyword? %) (name %) %) names)]
    (string/join ", " names)))

(defn css-attr-str
  [attr-name attr-value]
  (let [attr-name  (if (keyword? attr-name) (name attr-name) attr-name)
        attr-value (str attr-value)]
    (if (and (not (string/blank? attr-name)) (not (string/blank? attr-value)))
      (str attr-name ": " attr-value ";"))))

(defn css-rule->str
  [^StringBuilder sb rule & [level]]
  (if (seq rule)
    (let [level       (or level 0)
          indent      (string/join (repeat level "  "))
          attr-indent (str indent "  ")
          names       (take-while #(or (keyword? %) (string? %)) rule)
          rule        (drop (count names) rule)
          attrs       (first rule)
          sub-rules   (rest rule)]
      (.append sb indent)
      (.append sb (css-rule-name-str names))
      (.append sb " {\n")
      (doseq [[attr-name attr-value] (sort-by first attrs)]
        (when-let [attr-str (css-attr-str attr-name attr-value)]
          (.append sb attr-indent)
          (.append sb attr-str)
          (.append sb \newline)))
      (doseq [sub-rule sub-rules]
        (css-rule->str sb sub-rule (inc level)))
      (.append sb indent)
      (.append sb "}\n")))
  sb)

(defn css->str
  [rules]
  (let [sb (StringBuilder.)]
    (doseq [rule rules]
      (css-rule->str sb rule))
    (.toString sb)))
