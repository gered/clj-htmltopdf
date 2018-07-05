# clj-htmltopdf

A Clojure wrapper for the [Open HTML to PDF](https://github.com/danfickle/openhtmltopdf) library. Includes some extra 
niceties to make generating PDF content such as reports and other basic "print" content simple.

## Leiningen

[![](https://clojars.org/clj-htmltopdf/latest-version.svg)](https://clojars.org/clj-htmltopdf)

## Usage

```clojure
(use 'clj-htmltopdf.core)

(->pdf
  [:div
    [:h1 "HTML to PDF"]
    [:p "Glorious!"]]
  "output.pdf")
```

`clj-htmltopdf.core/->pdf` can take a variety of different input and output argument types, such as strings, 
`java.io.File` objects, `java.net.URL` objects, streams, etc.

As shown in the above example, a special case is made when the input argument is
[Hiccup](https://github.com/weavejester/hiccup)-style HTML where it will be automatically converted to an HTML string 
before being rendered as a PDF.

Remember, that when rendering HTML to PDFs, you should not just expect that any HTML you throw at it will render
perfectly fine onto the resulting PDF (just the same as when you print some HTML page from your browser, not everything
will be printed exactly the same as you see on your screen).

### Options

The third and optional argument to `clj-htmltopdf.core/->pdf` is a map of options. If you don't include this some basic
defaults will be used that should be suitable for most basic "report-like" PDFs.

The majority of the options supported are implemented by injecting small bits of HTML and/or some extra CSS styling 
into your HTML just before it is rendered as a PDF. However if you wish (or your PDF styling requirements are complex
enough that you've outgrown the simple built-in options available), you are free to ignore these options and have your 
PDF rendered using the exact HTML/CSS you want if you're more comfortable writing things such as @page CSS styles 
yourself. Since Open HTML to PDF is based on [Flying-saucer](https://github.com/flyingsaucerproject/flyingsaucer), it 
supports it's [CSS extensions](https://flyingsaucerproject.github.io/flyingsaucer/r8/guide/users-guide-R8.html#xil_43) 
as well.

A description of the keys supported in the options map follows:

#### `:doc`

Allows you to embed metadata information about the document into resulting PDFs such as title, author, etc.

```clojure
{:title    "My Awesome PDF"
 :author   "Gered"
 :subject  "Only Testing"
 :keywords "clojure,html,to,pdf"}
```

#### `:page`

Allows for control over CSS @page rule styles. This lets you set properties such as headers/footers, page layout and 
orientation, margins and more.

```clojure
{:margin      "1.0in"
 :size        :letter
 :orientation :portrait
 :margin-box  {:top-left            {:element "my-top-left-box"}
               :top-right           {:paging [:page " of " :pages]}
               :bottom-center       {:element "the-bottom-center-box"}
               :bottom-right-corner {:text "this is in the corner!"}}}
```

`:margin` sets page margins and can either a string containing a raw CSS `margin` definition, a map containing `:left`,
`:top`, `:right`, `:bottom` attributes, or a keyword to specify a predefined page margin:

* `:normal` - `"1.0in"`
* `:narrow` - `"0.5in"`
* `:moderate` - `"1.0in 0.75in"`
* `:wide` - `"1.0in 2.0in"`

`:size` and `:orientation` together set the [@page size](https://developer.mozilla.org/en-US/docs/Web/CSS/@page/size) 
property. By default this will be `:size :letter` and `:orientation :portrait`.

`:margin-box` is a map containing definitions for your headers/footers. The keys of the map should correspond to the
margin box names listed [here](https://www.w3.org/TR/css3-page/#margin-boxes) (if you make a typo, you won't get an 
error, but the margin box you typo'd also won't be visible anywhere, so be careful). The values should be maps that 
allow you to specify the content. These can take a few different formats allowing you a good amount of flexibility:

* `{:text "this is in the corner!"}` The simplest format, allows you to place some aribtrary constant text value in a margin box.
* `{:paging [:page " of " :pages]}` Similar to `:text`, it allows you to write out a simple line of text, but the text is specified as a vector of values that are all concatenated together where the values `:page` and `:pages` are replaced with the current page number and total number of pages respectively.
* `{:element "your-element-id-here"}` The string here should correspond to an element located anywhere in your HTML that has the same element ID. This element will be turned into a running element, which is then taken out of the flow of the rest of the HTML and will be rendered on to each page at that margin box location instead. This format is really helpful when you want to include complex elements in your margin boxes, as it allows you to set any arbitrary HTML as the content.
* `{:content "\"Page \" counter(page)"}` Can be used to emit a raw value into the CSS `content` property for that margin box. You'll probably want to use this format the least often, but it can sometimes be useful.

#### `:styles`

> **NOTE** This property is likely to get somewhat of an overhaul in the near future to allow for more flexibility and control over basic styling. Please keep this in mind when upgrading to new versions of clj-htmltopdf.

Allows control over what (non-@page) CSS styles are included in your HTML. Can be a filename or vector of filenames,
a map of CSS styles, or a boolean to toggle on or off default basic style inclusion. The default value is `true`.

When set to `true`, the following basic styles are set in your HTML:

```clojure
{:font-family      "sans-serif"
 :font-size        "12pt"
 :line-height      "1.3"
 :background-color "#fff"
 :color            "#000"}
```

In addition, the CSS stylesheet `htmltopdf-base.css` will be included, which sets up a number of basic styles and helper
CSS class definitions that are mostly intended to make generating simple report-like PDFs a little easier.

When set to a map, the map should include CSS styles for the HTML `<body>` tag only, and it will be merged with the
default styles shown above. Two additional keys can be set in this map:

* `:styles` a single file or vector of filenames pointing to any additional CSS stylesheets to be included.
* `:fonts` a sequence of maps of the form `{:font-family "font-family-name-here" :src "/path/to/custom-font.ttf"}` which allows you to use custom fonts in other CSS style definitions using the `:font-family` name specified here.

If you want to include your own custom CSS styles without the base `htmltopdf-base.css` stylesheet being included nor
any other base styles being injected, then you can specify either a single CSS filename or a vector of multiple CSS
files to be included in your HTML. Alternatively, if you know that your HTML will already include links to the external
CSS files you want to include, then you can just specify `nil` or `false` for the `:styles` option to get the same
behaviour.

#### `:watermark`

Either a map containing various properties for rendering text or image based watermarks onto all pages of the PDF, or a
function that will be called for each page that allows you to implement custom watermark rendering.

For simple text watermarks, the map can use any of the following:

```clojure
{:text      "My Watermark Text"
 :font      "helvetica-bold"
 :font-size 36.0
 :color     [255 0 0]
 :rotation  45.0
 :x         :center
 :y         :center
 :opacity   0.5}
``` 

`:font` is limited at the moment to be one of the built-in PDFbox supported fonts: times-roman, times-bold, times-italic, times-bolditalic, helvetica, helvetica-bold, helvetica-oblique, helvetica-boldoblique, courier, courier-bold, courier-oblique, courier-boldoblique

`:x` and `:y` can either be an X/Y coordinate (relative to the origin 0,0 at the bottom left of the page), or `:center`
which will center the text on that axis.

For simple image watermarks, the map uses most of the same properties:

```clojure
{:image     "/path/to/image-file"
 :rotation  45.0
 :x         :center
 :y         :center
 :scale-x   2.0
 :scale-y   2.0
 :opacity   0.5}
```

Finally, as mentioned, you can specify a function to implement your own watermark rendering.

```clojure
(fn [^org.apache.pdfbox.pdmodel.PDDocument doc
     ^org.apache.pdfbox.pdmodel.PDPage page
     ^org.apache.pdfbox.pdmodel.PDPageContentStream cs]
  ; render your watermark onto 'cs' here !
  )
```

The `cs` instance of a 
[`PDPageContentStream`](https://pdfbox.apache.org/docs/2.0.5/javadocs/org/apache/pdfbox/pdmodel/PDPageContentStream.html) 
object contains a bunch of methods that can be used to render onto the current page.

#### `:objects`

You can include `<object>` elements in your HTML and implement completely custom rendering for them using a `Graphics2D`
object by providing custom functions under the `:objects` key in your options.

```clojure
{; ...
 ; other options
 ; ...
 :objects {:by-id {"my-object" (fn [element-attrs ^java.awt.Graphics2D g]
                                 ; your custom rendering here
                                 )}}}
```

This sets up an object renderer for the `<object>` element with id `my-object`. The first argument is a map containing
all of the `<object>` element's attributes.

Note that with Open HTML to PDF, `<object>` tags have no default dimensions, so you should always make sure to include
a bit of CSS to give them some size so that they will be rendered. For example:

```html
<object id="my-object" style="width: 200px; height: 200px;"></object>
```

#### `:debug`

Two options are currently available under here that can be useful to troubleshoot issues if your PDF is not rendering
as you would expect.

Setting `:display-html?` to true will cause `->pdf` to display the final HTML string (via stdout) that is being
rendered (the final HTML _after_ all of the fancy options `<style>` tag injections and whatnot are completed).

Setting `:display-options?` to true will cause `->pdf` to display the final merged options map (which includes the
default values if not overridden) that is being used.

#### `:logging?`

Setting `:logging?` to true enables Open HTML to PDF's logging output. This is currently disabled by default because I
found it to be a bit too chatty personally. But you may decide to enable this, especially during development as it can
help you see when you've run into problems with your PDF. For example, it will display warnings when you have invalid
URLs to things like images, CSS files, etc.

#### File path / URL Resolving

clj-htmltopdf is now configured (as of `0.1-alpha6` and later) to allow you to use relative URLs for things like `<img>`
tags and CSS files to point to resources that exist on the classpath (e.g. under your project's "resources" directory).

This means you should be able to do something like:

```clojure
(->pdf
  [:div [:img {:src "images/foo.png"}]]
  "relative-image-test.pdf")
```
 
Assuming the image `foo.png` was located in your project under `resources/images/foo.png` you should get a PDF with the
image displayed as you would expect.
 
The URI resolver that clj-htmltopdf uses by default will just use `clojure.java.io/resource` to resolve relative URI by
default. You can also use absolute URIs if you wish, but they **must** include a scheme prefix (e.g. 'file:' or 'http:'
or whatever else) or it will be assumed to be a relative file URI.
 
```clojure
; proper way of specifying an absolute file path URI
[:img {:src "file:/Users/gered/Pictures/foo.png"}]
 
; incorrect way! this will NOT work (it will be assumed to be a relative path)
[:img {:src "/Users/gered/Pictures/foo.png"}]
```

If you wish you can set a "base URI" in the options map provided to `->pdf`:

```clojure
(->pdf
  [:div [:img {:src "foo.png"}]]
  "test.pdf"
  {:base-uri "images/"})
```

The `:base-uri` option does not just apply to `<img>` tags of course, but to any URL that needs to be resolved to render
the HTML as a PDF, so be careful if you use this option.

For more advanced requirements, you can also set a custom URI resolver function:

```clojure
(->pdf
  ...
  "test.pdf"
  {:uri-resolver (fn [base-uri uri]
                   ; return a resolved string uri
                   )})
```

 
### Some More Examples:

```clojure
; <object> renderer support
(->pdf
  [:div
   [:h2 "Object Drawing Test"]
   [:object {:id "the-object" :style "width: 400px; height: 300px;"}]
   [:p "Text after the object"]]
  "object.pdf"
  {:objects
   {:by-id
    {"the-object"
     (fn [object-element-attrs ^java.awt.Graphics2D g]
       (.setColor g java.awt.Color/RED)
       (.drawRect g 50 50 200 200)
       (.setColor g java.awt.Color/BLUE)
       (.drawRect g 75 75 300 200))}}})

; SVG support
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
  "svg.pdf")

; disables automatically injected base styles and @page settings. this allows your html to specify completely custom
; styles and page properties if needed.
(->pdf 
  (clojure.java.io/file "completely-custom.html") 
  "completely-custom.pdf"
  {:styles nil
   :page   nil})

; a more complex example showing some of the styles included in htmltopdf-base.css which is included by default when
; the :styles option is not nil/false or not just a list of CSS external files.
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
  "report.pdf"
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
            :display-options? true}})
```

## License

Copyright © 2017 Gered King

Distributed under the LGPL3 license.
