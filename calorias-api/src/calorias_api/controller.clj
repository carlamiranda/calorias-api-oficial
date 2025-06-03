(ns calorias-api.controller
  (:require [calorias-api.db :as db]
            [cheshire.core :as json]
            [calorias-api.services :as svc]
            [ring.util.response :refer [response status]]))

(defn handle-saldo []
  (response {:saldo (db/saldo)}))

(defn handle-transacoes []
  (response (db/transacoes)))

(defn handle-transacoes-global [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (db/transacoes-por-data-global data))})

(defn handle-saldo-data-global [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (db/saldo-por-data-global data))})

(defn handle-saldo-total-global []
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (db/saldo-total-global))})

(defn handle-listar-usuarios []
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (db/listar-usuarios))})

(defn handle-limpar []
  (do
    (db/limpar)
    (response {:mensagem "Transações apagadas."})))

(defn handle-alimento [nome]
  (try
    (let [resultado (svc/buscar-alimento nome)
          transacao (db/registrar {:tipo "alimento"
                                   :nome nome
                                   :valor (:calorias resultado)})]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string transacao)})
    (catch Exception _
      {:status 400 :body "Erro ao buscar alimento"})))

(defn handle-exercicio [nome peso tempo altura idade genero]
  (try
    (let [p (Double/parseDouble peso)
          t (Integer/parseInt tempo)
          h (Integer/parseInt altura)
          i (Integer/parseInt idade)
          resultado (svc/buscar-exercicio nome p t h i genero)
          transacao (db/registrar {:tipo "exercicio"
                                   :nome nome
                                   :tempo t
                                   :valor (:gasto resultado)})]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string transacao)})
    (catch Exception _
      {:status 400 :body "Erro ao buscar exercício"})))

(defn handle-registrar-usuario [request]
  (try
    (let [dados (:body request)
          {:keys [username altura peso idade genero]} dados]
      (if (and username altura peso idade genero)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string
                 (do (db/registrar-usuario dados)
                     {:mensagem "O usuário foi registrado."
                      :usuario dados}))}
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:erro "Os dados estão incompletos"})}))
    (catch Exception _
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:erro "Erro ao registrar usuário"})})))

(defn handle-obter-usuario []
  (try
    (response (db/obter-usuario))
    (catch Exception _
      {:status 500 :body "Erro ao obter usuário"})))

(defn handle-transacoes-intervalo [request]
  (let [params (:query-params request)
        data-inicio (get params "data-inicio")
        data-fim (get params "data-fim")]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string (db/transacoes-por-periodo-global data-inicio data-fim))}))

(defn handle-saldo-intervalo [request]
  (let [params (:query-params request)
        data-inicio (get params "data-inicio")
        data-fim (get params "data-fim")]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string (db/saldo-por-periodo-global data-inicio data-fim))}))
