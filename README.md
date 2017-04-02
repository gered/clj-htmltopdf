# clj-htmltopdf

A Clojure wrapper for the [Open HTML to PDF](https://github.com/danfickle/openhtmltopdf)
library. Includes some extra niceties to make generating PDF content such as reports
and other basic "print" content simple.

> **This is currently beta-quality! Use at your own risk. API and options are still subject to change.**  
> Currently not deployed to Clojars, so you will need to clone and install via `lein install` manually.

## Leiningen

```
[clj-htmltopdf "0.1"]
```

## Usage

```clojure
(use 'clj-htmltopdf.core)

(->pdf
  [:div
    [:h1 "HTML to PDF"]
    [:p "Glorious!"]]
  "output.pdf")
```

`clj-htmltopdf.core/->pdf` can take a variety of different input and output argument types,
such as strings, `java.io.File` objects, `java.net.URL` objects, streams, etc.

As shown in the above example, a special case is made when the input argument is
[Hiccup](https://github.com/weavejester/hiccup)-style HTML where it will be automatically 
converted to an HTML string before being rendered as a PDF.

### A More Complex Example

For now, until I write documentation ...

```clojure
(->pdf
  '([:div#margin-box-top-left "this is my custom header"]
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
     [:img {:src "https://clojure.org/images/clojure-logo-120b.png"}])
  "./test.pdf"
  {:doc {:title "My Awesome PDF"
         :author "Gered"
         :subject "Only Testing"
         :keywords "clojure,html,to,pdf"}
   :page {:margin "1.0in"
          :size :letter
          :orientation :portrait
          :margin-box
          {:top-left {:element true}
           :top-right {:paging [:page " of " :pages]}
           :bottom-center {:element true}
           :bottom-right-corner {:text "corner!"}}}
   :styles {:font-size "12pt"
            :color "#000"}
   :debug {:display-html? true
           :display-options? true}})
```

## License

Copyright © 2017 Gered King

Distributed under the LGPL3 license.
