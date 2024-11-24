(defproject clj-htmltopdf "0.2.3-SNAPSHOT"
  :description    "Simple Clojure wrapper for Open HTML to PDF"
  :url            "https://github.com/gered/clj-htmltopdf"
  :license        {:name "GNU Lesser General Public License v3.0"
                   :url  "https://www.gnu.org/licenses/lgpl.html"}

  :dependencies   [[io.github.openhtmltopdf/openhtmltopdf-core "1.1.22"]
                   [io.github.openhtmltopdf/openhtmltopdf-pdfbox "1.1.22"]
                   [io.github.openhtmltopdf/openhtmltopdf-rtl-support "1.1.22"]
                   [io.github.openhtmltopdf/openhtmltopdf-svg-support "1.1.22"]
                   [org.jsoup/jsoup "1.15.3"]
                   [commons-io/commons-io "2.11.0"]
                   [hiccup "1.0.5"]]

  :resource-paths ["resources"]

  :profiles       {:provided
                   {:dependencies [[org.clojure/clojure "1.9.0"]]}

                   :dev
                   {:dependencies   [[pjstadig/humane-test-output "0.9.0"]]
                    :resource-paths ["test-resources"]
                    :injections     [(require 'pjstadig.humane-test-output)
                                     (pjstadig.humane-test-output/activate!)]}})
