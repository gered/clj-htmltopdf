(ns clj-htmltopdf.test.pdfdoc-manual-tests
  (:require
    [clojure.java.io :as io]
    [clj-htmltopdf.core :refer [->pdf embed-image]]))

; this is a kind of "poor mans" set of tests. it is a bit difficult to put together a set of fully automated
; tests for PDF document generation, so for now this will have to suffice.
;
; how to use: manually run each one in a REPL, verify the PDF looks as expected. yup, very ghetto! :'(


(comment
  (let [filename "test-basic.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:h1 "HTML to PDF"]
       [:p "Glorious!"]]
      filename)))

(comment
  (let [filename "test-images.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:h1 "Using Images"]
       [:div "Local image file" [:img {:src "clojure-logo-120b.png"}]]
       [:div "Remote image via URL" [:img {:src "https://clojure.org/images/clojure-logo-120b.png"}]]
       [:div "Embedded image via CSS data" [:img {:src (embed-image (io/resource "clojure-logo-120b.png"))}]]]
      filename)))

(comment
  (let [filename "test-object-drawing.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:h2 "Object Drawing Test"]
       [:object {:id "the-object" :style "width: 400px; height: 300px;"}]
       [:p "Text after the object"]]
      filename
      {:objects
       {:by-id
        {"the-object"
         (fn [object-element-attrs ^java.awt.Graphics2D g]
           (.setColor g java.awt.Color/RED)
           (.drawRect g 50 50 200 200)
           (.setColor g java.awt.Color/BLUE)
           (.drawRect g 75 75 300 200))}}})))

(comment
  (let [filename "test-svg.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:h2 "SVG test"]
       [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1"}
        [:rect {:x 25 :y 25 :width 200 :height 200 :fill "lime" :stroke-width 4 :stroke "pink"}]
        [:circle {:cx 125 :cy 125 :r 75 :fill "orange"}]
        [:polyline {:points "50,150 50,200 200,200 200,100" :stroke "red" :stroke-width 4 :fill "none"}]
        [:line {:x1 50 :y1 50 :x2 200 :y2 200 :stroke "blue" :stroke-width 4}]]
       [:hr]
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width 100 :height 100}
        [:circle {:cx 50 :cy 50 :r 40 :stroke "green" :stroke-width 4 :fill "yellow"}]]
       [:hr]
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width 300 :height 200}
        [:rect {:width "100%" :height "100%" :fill "red"}]
        [:circle {:cx 150 :cy 100 :r 80 :fill "green"}]
        [:text {:x 150 :y 125 :font-size 60 :text-anchor "middle" :fill "white"} "SVG"]]]
      filename)))

(comment
  (let [filename "test-custom-styles.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      '([:head
         [:style {:type "text/css"}
          ".custom { font-family: monospace; color: #ff0000; }"]]
        [:body
         [:p.custom "this should be custom styled"]
         [:p "this should be normally styled. and the following table should be styled as per htmltopdf-base.css still"]
         [:table {:border 1}
          [:thead
           [:tr
            [:th "Column A"]
            [:th "Column B"]
            [:th "Column C"]]]
          [:tbody
           [:tr
            [:td "1"]
            [:td "2"]
            [:td "3"]]
           [:tr
            [:td "A"]
            [:td "B"]
            [:td "C"]]
           [:tr
            [:td "i"]
            [:td "ii"]
            [:td "iii"]]]]])
      filename
      {:debug {:display-html?    true
               :display-options? true}})))

(comment
  (let [filename "test-report.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:div#margin-box-top-left "this is my custom header"]
       [:div#margin-box-bottom-center
        [:table
         [:tr
          [:td "footer column A"]
          [:td "footer column B"]
          [:td "footer column C"]]]]
       [:h1 "My PDF Title"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
       [:h3 "Sub Title"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
       [:pre "(defn say-hello! [name]\n  (println \"Hello,\" name))"]
       [:p.page-break-after "Going onto page 2 next ... !"]
       [:table
        [:thead
         [:tr
          [:th {:width "6%"} "#"]
          [:th {:width "32%"} "Name"]
          [:th {:width "32%"} "Username"]
          [:th {:width "32%"} "Role"]]]
        [:tbody
         [:tr
          [:th "1"]
          [:th "Gered"]
          [:th "gered"]
          [:th "Developer"]]
         [:tr
          [:th "2"]
          [:th "Bob"]
          [:th "bob"]
          [:th "Sales Associate"]]
         [:tr
          [:th "3"]
          [:th "Sue"]
          [:th "sue"]
          [:th "Designer"]]
         [:tr
          [:th "4"]
          [:th "Joe"]
          [:th "joe"]
          [:th "Manager"]]]]
       [:ul
        [:li "first item"]
        [:li "second item"]
        [:li "third item"]]
       [:ol
        [:li "more items"]
        [:li "for you"]
        [:li "to see!"]]
       [:img {:src "https://clojure.org/images/clojure-logo-120b.png"}]]
      filename
      {:doc    {:title    "My Awesome PDF"
                :author   "Gered"
                :subject  "Only Testing"
                :keywords "clojure,html,to,pdf"}
       :page   {:margin      "1.0in"
                :size        :letter
                :orientation :portrait
                :margin-box  {:top-left            {:element "margin-box-top-left"}
                              :top-right           {:paging [:page " of " :pages]}
                              :bottom-center       {:element "margin-box-bottom-center"}
                              :bottom-right-corner {:text "corner!"}}}
       :styles {:font-size "12pt"
                :color     "#000"}
       :debug  {:display-html?    true
                :display-options? true}})))


(comment
  (let [filename "test-completely-custom.pdf"]
    (println "\n **** " filename " **** \n")
    (->pdf
      (io/resource "hello-world.html")
      filename
      {:styles nil
       :page   nil
       :debug  {:display-html?    true
                :display-options? true}})))

(comment
  (let [filename           "test-custom-font.pdf"
        relative-font-path "FirstTimeWriting-DOy8d.ttf"
        absolute-font-path (str (io/resource relative-font-path))]
    (println "\n **** " filename " **** \n")
    (->pdf
      [:div
       [:h1 "Custom fonts!"]
       [:p {:style "font-family: custom-font-relative"} "This should be styled in a custom font, specified via relative path!"]
       [:p {:style "font-family: custom-font-absolute"} "This should also be styled with a custom font, but specified via an absolute path!"]]
      filename
      {:styles {:fonts [{:font-family "custom-font-relative"
                         :src         relative-font-path}
                        {:font-family "custom-font-absolute"
                         :src         relative-font-path}]}
       :debug  {:display-html?    true
                :display-options? true}})))
