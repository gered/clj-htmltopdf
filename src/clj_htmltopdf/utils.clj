(ns clj-htmltopdf.utils
  (:import
    [java.io File]
    [java.net URL MalformedURLException]))

(def ^{:private true} bytes-class (Class/forName "[B"))

(defn byte-array?
  [x]
  (boolean (and x (= (.getClass x) bytes-class))))

(defn string->url-or-file
  [^String s]
  (try
    (URL. s)
    (catch MalformedURLException _
      (File. (str "file:" s)))))

