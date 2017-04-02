(ns clj-htmltopdf.utils
  (:import
    [java.io File]
    [java.net URL MalformedURLException]))

(defn string->url-or-file
  [^String s]
  (try
    (URL. s)
    (catch MalformedURLException _
      (File. (str "file:" s)))))
