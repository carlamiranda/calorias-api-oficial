(ns calorias-api.services
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def app-id "d9ce7ae4")
(def api-key "087692257bca1e1fadc9d02e4dae4f16")

(def headers {"x-app-id" app-id
              "x-app-key" api-key
              "Content-Type" "application/json"})

(defn buscar-alimento [nome]
  (let [body (json/generate-string {:query nome})
        res (http/post "https://trackapi.nutritionix.com/v2/natural/nutrients"
                       {:headers headers :body body})
        dados (json/parse-string (:body res) true)]
    {:alimento nome
     :calorias (-> dados :foods first :nf_calories)}))

(defn buscar-exercicio [nome peso tempo altura idade genero]
  (let [body (json/generate-string {:query (str tempo " minutes of " nome)
                                    :weight_kg peso
                                    :height_cm altura
                                    :age idade
                                    :gender genero})
        res (http/post "https://trackapi.nutritionix.com/v2/natural/exercise"
                       {:headers headers :body body})
        dados (json/parse-string (:body res) true)]
    {:exercicio nome
     :tempo tempo
     :gasto (-> dados :exercises first :nf_calories)}))
