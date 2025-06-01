(defproject calorias-api "0.1.0-SNAPSHOT"
  :description "API intermediária para cálculo de calorias"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "2.4.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [clj-http "3.12.3"]
                 [ring/ring-json "0.5.1"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler calorias-api.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
