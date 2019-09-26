(defproject clj-htmltopdf "0.1-alpha7"
  :description  "Simple Clojure wrapper for Open HTML to PDF"
  :url          "https://github.com/gered/clj-htmltopdf"
  :license      {:name "GNU Lesser General Public License v3.0"
                 :url  "https://www.gnu.org/licenses/lgpl.html"}

  :dependencies [[com.openhtmltopdf/openhtmltopdf-core "1.0.0"]
                 [com.openhtmltopdf/openhtmltopdf-pdfbox "1.0.0"]
                 [com.openhtmltopdf/openhtmltopdf-rtl-support "1.0.0"]
                 [com.openhtmltopdf/openhtmltopdf-svg-support "1.0.0"]
                 [org.jsoup/jsoup "1.12.1"]
                 [commons-io/commons-io "2.6"]
                 [hiccup "1.0.5"]]

  :profiles     {:provided
                 {:dependencies [[org.clojure/clojure "1.9.0"]]}

                 :dev
                 {:dependencies [[pjstadig/humane-test-output "0.9.0"]]
                  :injections   [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)]}})
