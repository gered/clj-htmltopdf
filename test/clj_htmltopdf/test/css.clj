(ns clj-htmltopdf.test.css
  (:use
    clojure.test
    clj-htmltopdf.css)
  (:import
    [java.lang StringBuilder]))

(deftest rule-name-parsing
  (is (= ""
         (css-rule-name-str nil)))
  (is (= ""
         (css-rule-name-str [])))
  (is (= "p"
         (css-rule-name-str ["p"])))
  (is (= "p"
         (css-rule-name-str [:p])))
  (is (= "p, li"
         (css-rule-name-str ["p" :li])))
  (is (= "h1, h2, h3, h4, h5, h6"
         (css-rule-name-str [:h1 :h2 :h3 :h4 :h5 :h6])))
  (is (= "ul>li, ol>li"
         (css-rule-name-str [:ul>li :ol>li])))
  (is (= "ul>li, ol>li"
         (css-rule-name-str ["ul>li" "ol>li"]))))

(deftest rule-attribute-parsing
  (is (= "color: black;"
         (css-attr-str :color "black")))
  (is (= "color: #ff0000;"
         (css-attr-str "color" "#ff0000")))
  (is (= nil
         (css-attr-str nil "foo")))
  (is (= nil
         (css-attr-str :color nil)))
  (is (= nil
         (css-attr-str "" "bar")))
  (is (= nil
         (css-attr-str :color "")))
  (is (= nil
         (css-attr-str nil nil))))

(deftest rule-parsing
  (is (= ""
         (str (css-rule->str (StringBuilder.) nil))))
  (is (= ""
         (str (css-rule->str (StringBuilder.) []))))
  (is (= "p {\n}\n"
         (str (css-rule->str (StringBuilder.) [:p]))))
  (is (= "p {\n}\n"
         (str (css-rule->str (StringBuilder.) [:p {}]))))
  (is (= "p {\n  font-weight: bold;\n}\n"
         (str (css-rule->str (StringBuilder.) [:p {:font-weight "bold"}]))))
  (is (= "p {\n  color: #000;\n  font-weight: bold;\n}\n"
         (str (css-rule->str (StringBuilder.) [:p {:font-weight "bold" :color "#000"}]))))
  (is (= "@page {\n  margin: 10%;\n  size: 8.5in 11in;\n  @top-right {\n    content: \"Page \" counter(page);\n  }\n  @top-left {\n    border: solid red;\n    content: \"Foobar\";\n  }\n}\n"
         (str (css-rule->str
                (StringBuilder.)
                ["@page"
                 {:size "8.5in 11in" :margin "10%"}
                 ["@top-right"
                  {:content "\"Page \" counter(page)"}]
                 ["@top-left"
                  {:content "\"Foobar\""
                   :border  "solid red"}]])))))

(deftest sheet-parsing
  (is (= ""
         (css->str nil)))
  (is (= ""
         (css->str [])))
  (is (thrown?
        java.lang.IllegalArgumentException
        (css->str [:body {:color "red"}])))
  (is (= "body {\n}\n"
         (css->str [[:body]])))
  (is (= "body {\n}\n"
         (css->str [[:body {}]])))
  (is (= "body {\n}\np {\n  font-size: 12pt;\n}\n"
         (css->str
           [[:body {}]
            [:p {:font-size "12pt"}]])))
  (is (thrown?
        java.lang.IllegalArgumentException
        (css->str
          [[:body {}]
           :p])))
  (is (= "body {\n  background-color: white;\n  color: black;\n}\np {\n  font-size: 12pt;\n}\n"
         (css->str
           [[:body
             {:background-color "white"
              :color "black"}]
            [:p
             {:font-size "12pt"}]])))
  (is (= "body {\n  background-color: white;\n  color: black;\n}\n@page {\n  margin: 10%;\n  size: 8.5in 11in;\n  @top-right {\n    content: \"Page \" counter(page);\n  }\n  @top-left {\n    border: solid red;\n    content: \"Foobar\";\n  }\n}\n"
         (css->str
           [[:body
             {:background-color "white"
              :color "black"}]
            ["@page"
             {:size "8.5in 11in" :margin "10%"}
             ["@top-right"
              {:content "\"Page \" counter(page)"}]
             ["@top-left"
              {:content "\"Foobar\""
               :border  "solid red"}]]]))))

#_(run-tests)
